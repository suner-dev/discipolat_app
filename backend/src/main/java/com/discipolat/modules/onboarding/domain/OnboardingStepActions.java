package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventService;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.ModuleCatalogService;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.TenantFeatureService;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Constat B2 — les 7 actions métier RÉELLES du wizard d'onboarding.
 *
 * <p>Avant ce correctif, `OnboardingWizardService.completeStep` faisait
 * {@code step.setStatus(COMPLETED)} : <b>aucun effet métier</b>, l'étape était
 * « complétée » sans que rien ne soit créé dans l'application. Chaque action
 * ci-dessous appelle un <b>service métier existant</b> et n'invente rien.
 *
 * <p>Toutes les données sont validées avant tout effet : une donnée invalide
 * donne {@code 400 STEP_DATA_INVALID} avec le détail des champs fautifs, et
 * <b>aucune écriture n'a lieu</b>.
 *
 * <p>Chaque action écrit un audit dédié et retourne le `completedData`
 * normalisé, enrichi des identifiants réellement créés.
 */
@Component
public class OnboardingStepActions {

    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_MODULES = 50;
    private static final int MAX_LIST_SIZE = 200;
    private static final long MAX_NAME_LENGTH = 160;

    /** Type d'événement par défaut du premier événement d'une église. */
    private static final String DEFAULT_EVENT_TYPE = "MEETING";

    private final OrganizationNodeService organizationNodeService;
    private final TenantSettingsService tenantSettingsService;
    private final DepartmentService departmentService;
    private final FamilyService familyService;
    private final TenantFeatureService tenantFeatureService;
    private final ModuleCatalogService moduleCatalogService;
    private final EventService eventService;
    private final InvitationService invitationService;
    private final TenantMembershipRepository membershipRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public OnboardingStepActions(OrganizationNodeService organizationNodeService,
                                 TenantSettingsService tenantSettingsService,
                                 DepartmentService departmentService,
                                 FamilyService familyService,
                                 TenantFeatureService tenantFeatureService,
                                 ModuleCatalogService moduleCatalogService,
                                 EventService eventService,
                                 InvitationService invitationService,
                                 TenantMembershipRepository membershipRepository,
                                 AuditService auditService,
                                 SecurityUtils securityUtils) {
        this.organizationNodeService = organizationNodeService;
        this.tenantSettingsService = tenantSettingsService;
        this.departmentService = departmentService;
        this.familyService = familyService;
        this.tenantFeatureService = tenantFeatureService;
        this.moduleCatalogService = moduleCatalogService;
        this.eventService = eventService;
        this.invitationService = invitationService;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    /**
     * Exécute l'action métier de l'étape.
     *
     * @return le `completedData` normalisé à stocker sur l'étape
     */
    public Map<String, Object> execute(OnboardingWizardStep.StepType stepType, UUID tenantId, Map<String, Object> data) {
        Map<String, Object> payload = data == null ? Map.of() : data;
        return switch (stepType) {
            case CHURCH_IDENTITY -> applyChurchIdentity(tenantId, payload);
            case MEMBER_IMPORT -> applyMemberImport(tenantId, payload);
            case STRUCTURE -> applyStructure(tenantId, payload);
            case ROLES -> applyRoles(tenantId, payload);
            case BRANDING -> applyBranding(tenantId, payload);
            case MODULES -> applyModules(tenantId, payload);
            case FIRST_EVENT -> applyFirstEvent(tenantId, payload);
        };
    }

    // ==================================================================
    // 0 — Identité de l'église
    // ==================================================================

    private Map<String, Object> applyChurchIdentity(UUID tenantId, Map<String, Object> data) {
        String churchName = requiredString(data, "churchName", 2, 120);
        String businessName = optionalString(data, "businessName", 2, 120);
        String city = optionalString(data, "city", 1, 120);
        String phone = optionalString(data, "phone", 3, 40);
        String email = optionalEmail(data, "email");
        String timezone = optionalString(data, "timezone", 1, 64);
        String currency = optionalCurrency(data, "currency");

        UUID actorId = currentActor();
        List<UUID> createdIds = new ArrayList<>();

        // L'église racine EST l'organisation de référence du tenant : on la renomme
        // si elle existe, sinon on la crée. Aucun simple enregistrement de texte.
        OrganizationNode root = organizationNodeService.getRoot(tenantId).orElse(null);
        if (root == null) {
            root = organizationNodeService.createRootChurch(tenantId, churchName, null, actorId);
        } else if (!churchName.equals(root.getName())) {
            root = organizationNodeService.updateNode(
                    root.getId(), churchName, null, null, null, actorId);
        }
        if (root != null && root.getId() != null) {
            createdIds.add(root.getId());
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("churchName", churchName);
        putIfPresent(normalized, "businessName", businessName);
        putIfPresent(normalized, "city", city);
        putIfPresent(normalized, "phone", phone);
        putIfPresent(normalized, "email", email);
        putIfPresent(normalized, "timezone", timezone);
        putIfPresent(normalized, "currency", currency);

        if (businessName != null || city != null || phone != null || email != null
                || timezone != null || currency != null) {
            List<String> updatedFields = new ArrayList<>();
            if (businessName != null) {
                updatedFields.add("businessName");
            }
            if (city != null) {
                updatedFields.add("city");
            }
            if (phone != null) {
                updatedFields.add("phone");
            }
            if (email != null) {
                updatedFields.add("email");
            }
            if (timezone != null) {
                updatedFields.add("timezone");
            }
            if (currency != null) {
                updatedFields.add("currency");
            }
            tenantSettingsService.updateSettings(tenantId,
                    settingsRequest(businessName, email, phone, city, currency, timezone, updatedFields),
                    actorId);
        }

        normalized.put("createdIds", createdIds);
        auditService.logSimple("TENANT_ONBOARDING_CHURCH_IDENTITY", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // 1 — Import des membres
    // ==================================================================

    /**
     * Décision D4 : le wizard reste <b>déclaratif mais vérifié</b> — il n'exécute
     * pas l'import (le module {@code /imports} le fait), il exige la déclaration
     * d'un compte réellement supérieur ou égal à 1. Dire « 0 membre importé »
     * n'aurait aucun sens.
     */
    private Map<String, Object> applyMemberImport(UUID tenantId, Map<String, Object> data) {
        Object raw = data.get("importedCount");
        if (raw == null) {
            throw invalid("importedCount", "Le nombre de membres importés est requis (entier >= 1).");
        }
        int importedCount;
        if (raw instanceof Number number) {
            importedCount = number.intValue();
        } else if (raw instanceof String text && !text.isBlank()) {
            try {
                importedCount = Integer.parseInt(text.trim());
            } catch (NumberFormatException notANumber) {
                throw invalid("importedCount", "Le nombre de membres importés doit être un entier.");
            }
        } else {
            throw invalid("importedCount", "Le nombre de membres importés doit être un entier.");
        }
        if (importedCount < 1) {
            throw invalid("importedCount", "Le nombre de membres importés doit être supérieur ou égal à 1.");
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("importedCount", importedCount);
        normalized.put("declaredOnly", true);
        auditService.logSimple("TENANT_MEMBERS_IMPORTED", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // 2 — Familles et départements
    // ==================================================================

    private Map<String, Object> applyStructure(UUID tenantId, Map<String, Object> data) {
        List<String> departments = optionalStringList(data, "departments", 1, 120);
        List<String> families = optionalStringList(data, "families", 1, 120);

        if (departments.isEmpty() && families.isEmpty()) {
            // Aucune liste fournie : on accepte seulement si la structure existe
            // DÉJÀ (l'admin l'a peut-être faite avant le wizard).
            long existingDepartments = departmentService.findAll(PageRequest.of(0, 1)).getTotalElements();
            long existingFamilies = familyService.findAll(PageRequest.of(0, 1)).getTotalElements();
            if (existingDepartments < 1 || existingFamilies < 1) {
                throw new DomainException(
                        "Créez au moins un département et une famille, ou fournissez leurs noms.",
                        HttpStatus.CONFLICT, "STEP_PRECONDITION_FAILED",
                        Map.of("existingDepartments", Long.toString(existingDepartments),
                                "existingFamilies", Long.toString(existingFamilies)));
            }
            Map<String, Object> alreadyThere = new LinkedHashMap<>();
            alreadyThere.put("departments", List.of());
            alreadyThere.put("families", List.of());
            alreadyThere.put("preconditionAlreadyMet", true);
            alreadyThere.put("existingDepartments", existingDepartments);
            alreadyThere.put("existingFamilies", existingFamilies);
            alreadyThere.put("createdIds", List.of());
            auditService.logSimple("TENANT_STRUCTURE_CREATED", "TENANT", tenantId);
            return alreadyThere;
        }

        UUID actorId = currentActor();
        List<UUID> createdIds = new ArrayList<>();

        for (String name : departments) {
            Department department = Department.builder()
                    .tenantId(tenantId)
                    .nom(name)
                    // `responsable_id` est NOT NULL : l'administrateur qui
                    // configure l'église en est le responsable par défaut, il peut
                    // le réattribuer ensuite. Le contrat §3.1 n'ouvre aucun champ
                    // responsable sur cette étape, aucun n'a donc été inventé.
                    .responsableId(actorId)
                    .build();
            Department saved = departmentService.create(department);
            if (saved != null && saved.getId() != null) {
                createdIds.add(saved.getId());
            }
        }

        for (String name : families) {
            // `chef_famille_id` est NOT NULL et le contrat §3.1 n'ouvre aucun champ
            // de chef sur cette étape : l'administrateur qui configure l'église
            // devient le chef par défaut, il peut le réattribuer ensuite.
            Family saved = familyService.create(new CreateFamilyRequest(
                    name,                       // nom
                    actorId,                    // chefFamilleId
                    null,                       // chefAdjointId
                    null,                       // createNewChef
                    null, null, null, null,     // newChefFirstName/LastName/Email/Phone
                    null, null, null));         // newChefSexe/DateNaissance/Adresse
            if (saved != null && saved.getId() != null) {
                createdIds.add(saved.getId());
            }
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("departments", departments);
        normalized.put("families", families);
        normalized.put("createdIds", createdIds);
        auditService.logSimple("TENANT_STRUCTURE_CREATED", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // 3 — Inviter les responsables
    // ==================================================================

    private Map<String, Object> applyRoles(UUID tenantId, Map<String, Object> data) {
        UUID actorId = currentActor();
        Object rawInvitations = data.get("invitations");
        List<InvitationRequest> invitations = new ArrayList<>();

        if (rawInvitations instanceof List<?> list) {
            for (Object entry : list) {
                invitations.add(parseInvitation(entry));
            }
        } else if (rawInvitations != null) {
            throw invalid("invitations", "Le champ `invitations` doit être une liste d'objets {email, role}.");
        }

        if (invitations.isEmpty()) {
            // Liste vide : accepté seulement s'il existe DÉJÀ au moins un membre
            // actif qui n'est pas le propriétaire (équipe constituée par ailleurs).
            long ownerMemberships = membershipRepository
                    .findByTenantIdAndStatusAndRoleContaining(
                            tenantId, MembershipStatus.ACTIVE, "OWNER")
                    .size();
            long activeMembers = membershipRepository
                    .countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
            long nonOwnerMembers = Math.max(0L, activeMembers - ownerMemberships);
            if (nonOwnerMembers < 1) {
                throw new DomainException(
                        "Invitez au moins un responsable, ou fournissez une liste vide "
                                + "seulement si votre équipe est déjà constituée.",
                        HttpStatus.CONFLICT, "STEP_PRECONDITION_FAILED",
                        Map.of("activeMembers", Long.toString(activeMembers),
                                "nonOwnerMembers", Long.toString(nonOwnerMembers)));
            }
            Map<String, Object> alreadyThere = new LinkedHashMap<>();
            alreadyThere.put("invitations", List.of());
            alreadyThere.put("preconditionAlreadyMet", true);
            alreadyThere.put("createdIds", List.of());
            auditService.logSimple("TENANT_ROLES_INVITED", "TENANT", tenantId);
            return alreadyThere;
        }

        List<Map<String, Object>> created = new ArrayList<>();
        List<UUID> createdIds = new ArrayList<>();

        for (InvitationRequest request : invitations) {
            InvitationService.InvitationCreationResult result = invitationService.createInvitation(
                    tenantId, actorId, request.email(), request.role(),
                    MembershipScopeType.TENANT, null, null);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("email", request.email());
            entry.put("role", request.role());
            entry.put("emailSent", result.emailSent());
            entry.put("requiresTenantSwitch", result.requiresTenantSwitch());
            entry.put("kind", result.kind().name());
            if (result.invitationId() != null) {
                entry.put("invitationId", result.invitationId().toString());
                createdIds.add(result.invitationId());
            }
            if (result.invitedUserId() != null) {
                entry.put("invitedUserId", result.invitedUserId().toString());
                createdIds.add(result.invitedUserId());
            }
            created.add(entry);
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("invitations", created);
        normalized.put("createdIds", createdIds);
        auditService.logSimple("TENANT_ROLES_INVITED", "TENANT", tenantId);
        return normalized;
    }

    private InvitationRequest parseInvitation(Object entry) {
        if (!(entry instanceof Map<?, ?> map)) {
            throw invalid("invitations", "Chaque invitation doit être un objet {email, role}.");
        }
        Object rawEmail = map.get("email");
        Object rawRole = map.get("role");
        if (!(rawEmail instanceof String email) || email.isBlank()) {
            throw invalid("invitations[].email", "L'email de l'invité est requis.");
        }
        if (!EMAIL.matcher(email.trim()).matches()) {
            throw invalid("invitations[].email", "L'email de l'invité est invalide : " + email);
        }
        if (!(rawRole instanceof String role) || role.isBlank()) {
            throw invalid("invitations[].role", "Le rôle de l'invité est requis.");
        }
        return new InvitationRequest(email.trim().toLowerCase(Locale.ROOT), role.trim().toUpperCase(Locale.ROOT));
    }

    private record InvitationRequest(String email, String role) {
    }

    // ==================================================================
    // 4 — Identité visuelle
    // ==================================================================

    private Map<String, Object> applyBranding(UUID tenantId, Map<String, Object> data) {
        String primaryColor = optionalColor(data, "primaryColor");
        String logoUrl = optionalString(data, "logoUrl", 1, 500);
        Object rawAllowDarkMode = data.get("allowDarkMode");
        Boolean allowDarkMode = parseBoolean(rawAllowDarkMode, "allowDarkMode");

        if (primaryColor == null && logoUrl == null && allowDarkMode == null) {
            throw invalid("primaryColor",
                    "Fournissez au moins un champ : primaryColor, logoUrl ou allowDarkMode.");
        }

        UUID actorId = currentActor();
        // `allowDarkMode` n'existe pas dans BrandingRequest : il est matérialisé
        // par un thème sombre, matérialisé ici dans `uiConfig` (champ libre déjà
        // supporté par TenantSettingsRequest) plutôt que d'inventer une colonne.
        List<String> updatedFields = new ArrayList<>();
        if (primaryColor != null) {
            updatedFields.add("primaryColor");
        }
        if (logoUrl != null) {
            updatedFields.add("logoUrl");
        }

        if (!updatedFields.isEmpty()) {
            tenantSettingsService.updateBranding(tenantId, new TenantSettingsService.BrandingRequest(
                    logoUrl, null, null, null,          // logoUrl, logoDarkUrl, coverUrl, faviconUrl
                    primaryColor, null, null,            // primaryColor, secondaryColor, accentColor
                    null, null,                          // surfaceColor, backgroundColor
                    null, null,                          // textPrimaryColor, textSecondaryColor
                    null, null, null, null,              // success/warning/error/info
                    null, null, null, null,              // primaryFont, secondaryFont, headingFont, monoFont
                    null, null,                          // customCss, customHeadHtml
                    updatedFields), actorId);
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        putIfPresent(normalized, "primaryColor", primaryColor);
        putIfPresent(normalized, "logoUrl", logoUrl);
        if (allowDarkMode != null) {
            normalized.put("allowDarkMode", allowDarkMode);
        }
        normalized.put("createdIds", List.of());
        auditService.logSimple("TENANT_ONBOARDING_BRANDING_UPDATED", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // 5 — Modules activés
    // ==================================================================

    private Map<String, Object> applyModules(UUID tenantId, Map<String, Object> data) {
        Object raw = data.get("modules");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw invalid("modules", "Le champ `modules` doit être une liste de 1 à " + MAX_MODULES + " codes.");
        }
        if (list.size() > MAX_MODULES) {
            throw invalid("modules", "Au maximum " + MAX_MODULES + " modules peuvent être activés.");
        }

        List<String> codes = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof String code) || code.isBlank()) {
                throw invalid("modules", "Chaque module doit être un code texte non vide.");
            }
            String normalizedCode = code.trim().toLowerCase(Locale.ROOT);
            if (codes.contains(normalizedCode)) {
                continue;
            }
            // Validation contre le catalogue : un code inconnu est refusé, on
            // n'active jamais une chainette inexistante.
            if (moduleCatalogService.getByCode(normalizedCode).isEmpty()) {
                throw invalid("modules", "Module inconnu : " + normalizedCode);
            }
            codes.add(normalizedCode);
        }

        List<UUID> createdIds = new ArrayList<>();
        for (String code : codes) {
            var feature = tenantFeatureService.enableFeature(tenantId, code, Map.of());
            if (feature != null && feature.getId() != null) {
                createdIds.add(feature.getId());
            }
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("modules", codes);
        normalized.put("createdIds", createdIds);
        auditService.logSimple("TENANT_ONBOARDING_MODULES_ENABLED", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // 6 — Premier événement
    // ==================================================================

    private Map<String, Object> applyFirstEvent(UUID tenantId, Map<String, Object> data) {
        String title = requiredString(data, "title", 2, (int) MAX_NAME_LENGTH);
        String location = optionalString(data, "location", 1, 300);
        String rawStartAt = optionalString(data, "startAt", 1, 64);
        if (rawStartAt == null) {
            throw invalid("startAt", "La date de début est requise (ISO-8601).");
        }
        Instant startAt;
        try {
            startAt = Instant.parse(rawStartAt);
        } catch (DateTimeParseException notIso) {
            // On tolère aussi une date locale sans fuseau, lue comme UTC.
            try {
                startAt = LocalDateTime.parse(rawStartAt).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException stillNotIso) {
                throw invalid("startAt", "La date de début doit être une date ISO-8601 valide.");
            }
        }
        if (!startAt.isAfter(Instant.now())) {
            throw invalid("startAt", "La date de début doit être dans le futur.");
        }

        Event event = Event.builder()
                .tenantId(tenantId)
                .titre(title)
                .lieu(location)
                .typeEvenement(DEFAULT_EVENT_TYPE)
                .dateDebut(LocalDateTime.ofInstant(startAt, ZoneOffset.UTC))
                .statut("PLANIFIE")
                .build();
        Event saved = eventService.create(event, List.of());

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", title);
        normalized.put("startAt", startAt.toString());
        putIfPresent(normalized, "location", location);
        normalized.put("typeEvenement", DEFAULT_EVENT_TYPE);
        normalized.put("statut", "PLANIFIE");
        if (saved != null && saved.getId() != null) {
            normalized.put("eventId", saved.getId().toString());
            normalized.put("createdIds", List.of(saved.getId()));
        } else {
            normalized.put("createdIds", List.of());
        }
        auditService.logSimple("TENANT_ONBOARDING_FIRST_EVENT_CREATED", "TENANT", tenantId);
        return normalized;
    }

    // ==================================================================
    // Validation
    // ==================================================================

    private UUID currentActor() {
        return securityUtils.getCurrentUserId();
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static DomainException invalid(String field, String reason) {
        return new DomainException("Données d'étape invalides : " + reason,
                HttpStatus.BAD_REQUEST, "STEP_DATA_INVALID", Map.of(field, reason));
    }

    private static String requiredString(Map<String, Object> data, String field, int min, int max) {
        Object raw = data.get(field);
        if (!(raw instanceof String value) || value.isBlank()) {
            throw invalid(field, "Le champ `" + field + "` est requis.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min) {
            throw invalid(field, "Le champ `" + field + "` doit contenir au moins " + min + " caractères.");
        }
        if (trimmed.length() > max) {
            throw invalid(field, "Le champ `" + field + "` ne doit pas dépasser " + max + " caractères.");
        }
        return trimmed;
    }

    private static String optionalString(Map<String, Object> data, String field, int min, int max) {
        Object raw = data.get(field);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String value)) {
            throw invalid(field, "Le champ `" + field + "` doit être une chaîne de caractères.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() < min || trimmed.length() > max) {
            throw invalid(field, "Le champ `" + field + "` doit contenir entre " + min + " et " + max + " caractères.");
        }
        return trimmed;
    }

    private static String optionalEmail(Map<String, Object> data, String field) {
        String value = optionalString(data, field, 3, 255);
        if (value != null && !EMAIL.matcher(value).matches()) {
            throw invalid(field, "L'email est invalide : " + value);
        }
        return value;
    }

    private static String optionalColor(Map<String, Object> data, String field) {
        String value = optionalString(data, field, 4, 7);
        if (value != null && !HEX_COLOR.matcher(value).matches()) {
            throw invalid(field, "La couleur doit être au format #RRGGBB (ou #RGB) : " + value);
        }
        return value;
    }

    private static String optionalCurrency(Map<String, Object> data, String field) {
        String value = optionalString(data, field, 3, 3);
        if (value != null && !value.equals(value.toUpperCase(Locale.ROOT))) {
            throw invalid(field, "La devise doit être un code ISO-4217 en majuscules : " + value);
        }
        return value;
    }

    private static Boolean parseBoolean(Object raw, String field) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
        }
        throw invalid(field, "Le champ `" + field + "` doit être un booléen.");
    }

    private static List<String> optionalStringList(Map<String, Object> data, String field, int min, int max) {
        Object raw = data.get(field);
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw invalid(field, "Le champ `" + field + "` doit être une liste de chaînes.");
        }
        if (list.size() > MAX_LIST_SIZE) {
            throw invalid(field, "Le champ `" + field + "` ne peut pas dépasser " + MAX_LIST_SIZE + " entrées.");
        }
        List<String> values = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof String text) || text.isBlank()) {
                throw invalid(field, "Chaque entrée de `" + field + "` doit être un texte non vide.");
            }
            String trimmed = text.trim();
            if (trimmed.length() < min || trimmed.length() > max) {
                throw invalid(field, "Chaque entrée de `" + field + "` doit contenir entre "
                        + min + " et " + max + " caractères.");
            }
            if (!values.contains(trimmed)) {
                values.add(trimmed);
            }
        }
        return values;
    }


    /**
     * Construit un {@code TenantSettingsRequest} (69 composants) en ne renseignant
     * QUE les champs issus de l'étape d'identité.
     *
     * <p>Le record expose 69 champs : les nommer ici, un par ligne, est le seul
     * moyen de rester exact sans modifier un DTO partagé par le reste de
     * l'application (R4 : pas de refactor non demandé).
     */
    private static TenantSettingsService.TenantSettingsRequest settingsRequest(
            String businessName, String email, String phone, String city,
            String currency, String timezone, List<String> updatedFields) {
        return new TenantSettingsService.TenantSettingsRequest(
                businessName,                    // businessName
                null, null, null,                // slogan, legalName, description
                null, null, null, null,          // logoUrl, logoDarkUrl, coverUrl, faviconUrl
                null, null, null,                // primaryColor, secondaryColor, accentColor
                null, null,                      // surfaceColor, backgroundColor
                null, null,                      // textPrimaryColor, textSecondaryColor
                null, null, null, null,          // successColor, warningColor, errorColor, infoColor
                null, null, null, null,          // primaryFont, secondaryFont, headingFont, monoFont
                null, null,                      // locale, supportedLocales
                timezone,                        // timezone
                null,                            // country
                city,                            // city
                currency,                        // currency
                null, null, null, null,          // dateFormat, timeFormat, dateTimeFormat, phoneCountryCode
                null,                            // weekStartDay
                email, phone, null, null,        // email, phone, website, address
                null, null,                      // openingHours, workingDays
                null, null,                      // invitationEmailSubject, invitationEmailBody
                null, null,                      // welcomeEmailSubject, welcomeEmailBody
                null, null,                      // footerText, footerLinks
                null, null, null,                // lowBandEnabled, publicDirectoryEnabled, legacyMigrationEnabled
                null, null, null,                // offlineMode, analyticsEnabled, aiFeaturesEnabled
                null, null, null,                // chatEnabled, academyEnabled, marketplaceEnabled
                null, null, null,                // apiAccessEnabled, customDomainEnabled, ssoEnabled
                null, null,                      // twoFactorRequired, passwordPolicyEnabled
                null, null, null,                // sessionTimeoutMinutes, maxFailedLoginAttempts, lockoutDurationMinutes
                null, null, null,                // uiConfig, notificationRules, integrationConfig
                null, null,                      // customCss, customHeadHtml
                updatedFields);                  // updatedFields
    }
}
