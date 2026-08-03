package com.discipolat.modules.members.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberDashboardResponse(
        MemberUserInfo user,
        MemberSoulInfo soul,
        Integer age,
        String statutMembre,
        boolean estFaiseur,
        String dateArriveeEglise,
        MemberFamilyInfo famille,
        PersonneInfo faiseur,
        List<MemberDepartmentInfo> departements
) {
    public record MemberUserInfo(
            UUID id, String firstName, String lastName, String email,
            String phone, String photoUrl, LocalDate dateNaissance,
            String situationFamiliale) {}

    public record MemberSoulInfo(
            UUID id, String profession, String niveauEtude, Integer nbEnfants,
            LocalDate dateIntegration, String statut) {}

    public record MemberFamilyInfo(UUID id, String nom, UUID chefFamilleId, String chefNom) {}

    public record PersonneInfo(UUID id, String nom) {}

    public record MemberDepartmentInfo(
            UUID id, String nom, String description,
            UUID responsableId, String responsableNom) {}
}
