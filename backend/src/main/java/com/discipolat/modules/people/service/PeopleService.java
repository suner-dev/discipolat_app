package com.discipolat.modules.people.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.domain.PermissionVersion;
import com.discipolat.modules.core.repository.PermissionVersionRepository;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.people.domain.EventAssignment;
import com.discipolat.modules.people.domain.Membership;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.domain.RoleAssignment;
import com.discipolat.modules.people.domain.SpaceMembership;
import com.discipolat.modules.people.repository.EventAssignmentRepository;
import com.discipolat.modules.people.repository.MembershipRepository;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.repository.RoleAssignmentRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.spaces.domain.SpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeopleService {

    private final PersonRepository personRepository;
    private final MembershipRepository membershipRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final EventAssignmentRepository eventAssignmentRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceService spaceService;
    private final AuditEventService auditEventService;
    private final OutboxPublisher outboxPublisher;
    private final PermissionVersionRepository permissionVersionRepository;
    private final RoleRepository roleRepository;

    // ========== G3.1 : PEOPLE ENGINE ==========

    /**
     * Inscrit une personne (auto-inscription ou par admin).
     * Création transactionnelle : Person + Membership + événement MemberRegistered.
     * La personne apparaît immédiatement dans le répertoire et liste "Sans espace".
     */
    @Transactional
    public Person registerPerson(UUID tenantId, Person person, String source, UUID actorId) {
        // Normalisation email/phone pour dédoublonnage
        String emailNorm = normalizeEmail(person.getEmailNormalized());
        String phoneNorm = normalizePhone(person.getPhoneNormalized());

        // Vérifier dédoublonnage
        Optional<Person> existingByEmail = emailNorm != null
                ? personRepository.findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(tenantId, emailNorm)
                : Optional.empty();
        Optional<Person> existingByPhone = phoneNorm != null
                ? personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, phoneNorm)
                : Optional.empty();

        Person savedPerson;
        if (existingByEmail.isPresent() || existingByPhone.isPresent()) {
            // Fusion assistée : retourner l'existant pour que l'admin choisisse
            Person existing = existingByEmail.orElse(existingByPhone.get());
            throw new PersonAlreadyExistsException(existing, "Une personne avec cet email/téléphone existe déjà");
        }

        // Créer la personne
        person.setTenantId(tenantId);
        person.setEmailNormalized(emailNorm);
        person.setPhoneNormalized(phoneNorm);
        person.setStatus("ACTIVE");
        savedPerson = personRepository.save(person);

        // Créer membership à l'église
        Membership membership = Membership.builder()
                .tenantId(tenantId)
                .personId(savedPerson.getId())
                .membershipStatus("NOUVEAU_CONVERTI")
                .joinedAt(LocalDate.now())
                .source(source != null ? source : "INSCRIPTION")
                .build();
        membershipRepository.save(membership);

        // Événement MemberRegistered → outbox → répertoire temps réel
        outboxPublisher.publish(
                "PERSON", savedPerson.getId(), "MemberRegistered",
                Map.of(
                        "personId", savedPerson.getId().toString(),
                        "tenantId", tenantId.toString(),
                        "firstName", savedPerson.getFirstName(),
                        "lastName", savedPerson.getLastName(),
                        "email", savedPerson.getEmailNormalized(),
                        "source", source
                )
        );

        // Audit
        auditEventService.log(tenantId, actorId, null, "MEMBER_REGISTERED", "PERSON", savedPerson.getId(),
                Map.of(), Map.of("firstName", savedPerson.getFirstName(), "lastName", savedPerson.getLastName()),
                null, null);

        return savedPerson;
    }

    /**
     * Fusionne deux fiches personne (dédoublonnage assisté).
     * L'admin choisit la fiche maîtresse ; l'autre est archivée (soft delete).
     */
    @Transactional
    public Person mergePersons(UUID tenantId, UUID masterId, UUID duplicateId, UUID actorId) {
        Person master = getPerson(tenantId, masterId);
        Person duplicate = getPerson(tenantId, duplicateId);

        // Transférer memberships
        membershipRepository.findByPersonId(duplicateId).ifPresent(m -> {
            m.setPersonId(masterId);
            membershipRepository.save(m);
        });

        // Transférer space_memberships
        for (SpaceMembership sm : spaceMembershipRepository.findByPersonIdAndStatus(duplicateId, "ACTIVE")) {
            Optional<SpaceMembership> existing = spaceMembershipRepository.findByPersonIdAndSpaceIdAndLeftAtIsNull(masterId, sm.getSpaceId());
            if (existing.isPresent()) {
                // Déjà membre → archiver le duplicate
                sm.setStatus("LEFT");
                sm.setLeftAt(OffsetDateTime.now());
                spaceMembershipRepository.save(sm);
            } else {
                sm.setPersonId(masterId);
                spaceMembershipRepository.save(sm);
            }
        }

        // Transférer role_assignments
        for (RoleAssignment ra : roleAssignmentRepository.findByTenantIdAndPersonIdAndStatus(tenantId, duplicateId, "ACTIVE")) {
            ra.setPersonId(masterId);
            roleAssignmentRepository.save(ra);
        }

        // Archiver le duplicate
        duplicate.setDeletedAt(OffsetDateTime.now());
        duplicate.setStatus("ARCHIVED");
        personRepository.save(duplicate);

        // Audit
        auditEventService.log(tenantId, actorId, null, "PERSON_MERGED", "PERSON", masterId,
                Map.of("duplicateId", duplicateId.toString()), Map.of(),
                null, null);

        outboxPublisher.publish("PERSON", masterId, "MemberRegistered",
                Map.of("action", "MERGED", "duplicateId", duplicateId.toString()));

        return master;
    }

    /**
     * Recherche dans le répertoire (avec filtres : campus, statut, sans espace, sans famille).
     */
    @Transactional(readOnly = true)
    public Page<Person> searchPeople(UUID tenantId, String search, String status, UUID campusId, boolean withoutSpace, boolean withoutFamily, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return personRepository.search(tenantId, search, pageable);
        }
        if (status != null) {
            return personRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable);
        }
        // TODO: Filtres avancés (campus, sans espace, sans famille) nécessitent jointures
        return personRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    /**
     * Liste des personnes "sans espace" (pour affectation par chef de famille/département).
     */
    @Transactional(readOnly = true)
    public List<Person> getPeopleWithoutSpace(UUID tenantId) {
        List<Person> all = personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId);
        return all.stream()
                .filter(p -> spaceMembershipRepository.findByPersonIdAndStatus(p.getId(), "ACTIVE").isEmpty())
                .toList();
    }

    // ========== G3.2 : MEMBERSHIPS & ROLE ASSIGNMENTS ==========

    /**
     * Affecte une personne à un espace (par chef de famille/département/admin).
     * Crée SpaceMembership + notification à la personne.
     */
    @Transactional
    public SpaceMembership assignToSpace(UUID tenantId, UUID actorId, UUID personId, UUID spaceId, String membershipType, String responsibility) {
        // Vérifier droits (spaceService.canCustomize)
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId));
        if (!spaceService.canCustomize(actorId, space)) {
            throw new SecurityException("Vous ne pouvez pas ajouter de membre à cet espace");
        }

        Person person = getPerson(tenantId, personId);

        // Vérifier pas déjà membre actif
        Optional<SpaceMembership> existing = spaceMembershipRepository.findByPersonIdAndSpaceIdAndLeftAtIsNull(personId, spaceId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Cette personne est déjà membre de cet espace");
        }

        SpaceMembership sm = SpaceMembership.builder()
                .tenantId(tenantId)
                .personId(personId)
                .spaceId(spaceId)
                .status("ACTIVE")
                .membershipType(membershipType != null ? membershipType : "MEMBER")
                .responsibility(responsibility)
                .build();
        SpaceMembership saved = spaceMembershipRepository.save(sm);

        // Événement MemberTransferred (ajout à un espace)
        outboxPublisher.publish("SPACE_MEMBERSHIP", saved.getId(), "MemberTransferred",
                Map.of(
                        "personId", personId.toString(),
                        "personName", person.getFullName(),
                        "spaceId", spaceId.toString(),
                        "spaceName", space.getName(),
                        "membershipType", membershipType,
                        "actorId", actorId.toString()
                )
        );

        // Audit
        auditEventService.log(tenantId, actorId, null, "SPACE_MEMBER_ADDED", "SPACE_MEMBERSHIP", saved.getId(),
                Map.of(), Map.of("personId", personId.toString(), "spaceId", spaceId.toString()),
                null, null);

        // G4.4 : propager le changement de permission (rôle vivant)
        notifyPermissionChange(personId, "SPACE_MEMBER_ADDED");

        return saved;
    }

    /**
     * Retire une personne d'un espace (soft leave).
     */
    @Transactional
    public void removeFromSpace(UUID tenantId, UUID actorId, UUID personId, UUID spaceId) {
        SpaceMembership sm = spaceMembershipRepository.findByPersonIdAndSpaceIdAndLeftAtIsNull(personId, spaceId)
                .orElseThrow(() -> new EntityNotFoundException("SpaceMembership", "personId/spaceId", personId + "/" + spaceId));

        sm.setStatus("LEFT");
        sm.setLeftAt(OffsetDateTime.now());
        spaceMembershipRepository.save(sm);

        // Événement + audit
        outboxPublisher.publish("SPACE_MEMBERSHIP", sm.getId(), "MemberTransferred",
                Map.of("personId", personId.toString(), "spaceId", spaceId.toString(), "action", "REMOVED"));
        auditEventService.log(tenantId, actorId, null, "SPACE_MEMBER_REMOVED", "SPACE_MEMBERSHIP", sm.getId(),
                Map.of(), Map.of(), null, null);

        // G4.4 : propager le changement de permission (rôle vivant)
        notifyPermissionChange(personId, "SPACE_MEMBER_REMOVED");
    }

    /**
     * Assigne un rôle permanent (RoleAssignment) — historisé (jamais d'écrasement).
     */
    @Transactional
    public RoleAssignment assignRole(UUID tenantId, UUID actorId, UUID personId, UUID roleId, UUID orgUnitId, UUID spaceId, String reason) {
        // Vérifier droits admin/tenant
        // TODO: authorizationService check

        // Clôturer rôle actif existant sur même org_unit/space + rôle
        List<RoleAssignment> active = roleAssignmentRepository.findByTenantIdAndPersonIdAndStatus(tenantId, personId, "ACTIVE");
        for (RoleAssignment ra : active) {
            if ((orgUnitId != null && orgUnitId.equals(ra.getOrganizationUnitId())) ||
                (spaceId != null && spaceId.equals(ra.getSpaceId()))) {
                ra.setStatus("ENDED");
                ra.setEndedAt(LocalDate.now());
                ra.setReason("Remplacé par nouvelle assignation");
                roleAssignmentRepository.save(ra);
            }
        }

        RoleAssignment assignment = RoleAssignment.builder()
                .tenantId(tenantId)
                .personId(personId)
                .organizationUnitId(orgUnitId)
                .spaceId(spaceId)
                .roleId(roleId)
                .startedAt(LocalDate.now())
                .status("ACTIVE")
                .reason(reason)
                .build();
        RoleAssignment saved = roleAssignmentRepository.save(assignment);

        // Événement RoleAssigned
        outboxPublisher.publish("ROLE_ASSIGNMENT", saved.getId(), "RoleAssigned",
                Map.of("personId", personId.toString(), "roleId", roleId.toString(),
                        "orgUnitId", orgUnitId != null ? orgUnitId.toString() : null,
                        "spaceId", spaceId != null ? spaceId.toString() : null));

        auditEventService.log(tenantId, actorId, null, "ROLE_ASSIGNED", "ROLE_ASSIGNMENT", saved.getId(),
                Map.of(), Map.of("roleId", roleId.toString()), null, null);

        // G4.4 : propager le changement de permission (rôle vivant)
        notifyPermissionChange(personId, "ROLE_ASSIGNED");

        return saved;
    }

    /**
     * Termine un rôle (clôture sans suppression).
     */
    @Transactional
    public void endRole(UUID tenantId, UUID actorId, UUID assignmentId, String reason) {
        RoleAssignment ra = roleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("RoleAssignment", assignmentId));

        if (!ra.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }

        ra.setStatus("ENDED");
        ra.setEndedAt(LocalDate.now());
        ra.setReason(reason);
        roleAssignmentRepository.save(ra);

        outboxPublisher.publish("ROLE_ASSIGNMENT", assignmentId, "RoleEnded",
                Map.of("personId", ra.getPersonId().toString(), "roleId", ra.getRoleId().toString()));
        auditEventService.log(tenantId, actorId, null, "ROLE_ENDED", "ROLE_ASSIGNMENT", assignmentId,
                Map.of(), Map.of(), null, null);

        // G4.4 : propager le changement de permission (rôle vivant)
        notifyPermissionChange(ra.getPersonId(), "ROLE_ENDED");
    }

    /**
     * Transfert de pasteur (G4.3) — change org_unit, clôture ancien rôle, crée nouveau.
     */
    @Transactional
    public RoleAssignment transferPastor(UUID tenantId, UUID actorId, UUID personId, UUID newOrgUnitId, String reason) {
        // Clôturer tous rôles actifs sur ancienne org_unit
        List<RoleAssignment> active = roleAssignmentRepository.findByTenantIdAndPersonIdAndStatus(tenantId, personId, "ACTIVE");
        for (RoleAssignment ra : active) {
            if (ra.getOrganizationUnitId() != null) {
                ra.setStatus("ENDED");
                ra.setEndedAt(LocalDate.now());
                ra.setReason("Transféré vers " + newOrgUnitId + ": " + reason);
                roleAssignmentRepository.save(ra);
            }
        }

        Role pastorCampus = roleRepository.findByTenantIdAndKey(tenantId, "PASTOR_CAMPUS")
                .orElseGet(() -> roleRepository.findGlobalByKey("PASTOR_CAMPUS").orElse(null));
        if (pastorCampus == null) {
            throw new EntityNotFoundException("Role", "key", "PASTOR_CAMPUS");
        }
        String appointmentReason = reason == null ? "Transfert pasteur" : reason;
        RoleAssignment newAssignment = assignRole(tenantId, actorId, personId, pastorCampus.getId(),
                newOrgUnitId, null, appointmentReason);

        outboxPublisher.publish("ROLE_ASSIGNMENT", newAssignment.getId(), "PastorAppointed",
                Map.of("personId", personId.toString(), "newOrgUnitId", newOrgUnitId.toString(), "reason", appointmentReason));

        return newAssignment;
    }

        /**
     * G4.4 — Rôles vivantes : bump la version du cache permissions et publie
     * l'événement outbox PermissionsChanged pour propagation temps réel.
     * Appelé après tout changement de rôle/membre affectant les permissions d'un utilisateur.
     */
    @Transactional
    private void notifyPermissionChange(UUID personId, String changeType) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return;

        // Bump la version du cache permission_version
        permissionVersionRepository.findByTenantIdAndUserId(tenantId, personId)
                .ifPresentOrElse(
                        pv -> {
                            pv.setVersion(pv.getVersion() + 1);
                            pv.setUpdatedAt(OffsetDateTime.now());
                            permissionVersionRepository.save(pv);
                        },
                        () -> {
                            PermissionVersion pv = PermissionVersion.builder()
                                    .tenantId(tenantId)
                                    .userId(personId)
                                    .version(1L)
                                    .permissionsJson(new ArrayList<>())
                                    .updatedAt(OffsetDateTime.now())
                                    .build();
                            permissionVersionRepository.save(pv);
                        });

        // Publie l'événement PermissionsChanged (outbox -> realtime consumer -> WS push)
        outboxPublisher.publish("PERMISSION", personId, "PermissionsChanged",
                Map.of("userId", personId.toString(), "changeType", changeType));
    }

    // ========== HELPERS ==========

    @Transactional(readOnly = true)
    public Person getPerson(UUID tenantId, UUID personId) {
        return personRepository.findById(personId)
                .filter(p -> p.getTenantId().equals(tenantId) && !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Person", personId));
    }

    @Transactional(readOnly = true)
    public List<SpaceMembership> getPersonSpaces(UUID tenantId, UUID personId) {
        return spaceMembershipRepository.findActiveByPersonId(tenantId, personId);
    }

    @Transactional(readOnly = true)
    public List<RoleAssignment> getPersonRoles(UUID tenantId, UUID personId) {
        return roleAssignmentRepository.findActiveByPersonId(tenantId, personId);
    }

    @Transactional(readOnly = true)
    public List<EventAssignment> getPersonEventAssignments(UUID tenantId, UUID personId) {
        return eventAssignmentRepository.findByPersonIdAndStatus(personId, "ASSIGNED");
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        // Normalisation E.164 basique
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (!cleaned.startsWith("+")) cleaned = "+" + cleaned;
        return cleaned;
    }

    public static class PersonAlreadyExistsException extends RuntimeException {
        private final Person existingPerson;

        public PersonAlreadyExistsException(Person existing, String message) {
            super(message);
            this.existingPerson = existing;
        }

        public Person getExistingPerson() { return existingPerson; }
    }
}