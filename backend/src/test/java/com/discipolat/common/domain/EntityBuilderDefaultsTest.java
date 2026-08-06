package com.discipolat.common.domain;

import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.enums.StatutValidation;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.appointments.domain.Appointment;
import com.discipolat.modules.badges.domain.Badge;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.members.domain.MemberRequest;
import com.discipolat.modules.objectives.domain.Objective;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowup;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.trainings.domain.Course;
import com.discipolat.modules.trainings.domain.CourseEnrollment;
import com.discipolat.modules.visits.domain.Visit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Non-régression : Lombok @Builder ignore les initialisateurs de champs.
 * Toute entité avec @Builder et un champ enum initialisé DOIT être
 * annotée @Builder.Default pour conserver sa valeur par défaut.
 * (Bug : création de famille -> 500 "null value in column niveau_risque")
 */
class EntityBuilderDefaultsTest {

    @Test
    void familyBuilder_preservesNiveauRisqueAndStatutDefaults() {
        Family family = Family.builder()
                .nom("Famille Test")
                .chefFamilleId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .dateCreation(LocalDate.now())
                .build();

        assertEquals(NiveauRisque.NORMAL, family.getNiveauRisque(),
                "niveauRisque doit être NORMAL par défaut (bug 500 en création de famille)");
        assertEquals(StatutEntite.ACTIVE, family.getStatut(),
                "statut doit être ACTIVE par défaut");
    }

    @Test
    void soulBuilder_preservesStatutDefault() {
        Soul soul = Soul.builder().nom("Test").prenom("A").build();
        assertEquals(StatutAme.EN_INTEGRATION, soul.getStatut());
    }

    @Test
    void departmentBuilder_preservesStatutDefault() {
        Department dept = Department.builder().nom("Département Test").build();
        assertEquals(StatutEntite.ACTIVE, dept.getStatut());
    }

    @Test
    void otherEntitiesBuilder_preserveEnumDefaults() {
        assertEquals(com.discipolat.modules.badges.domain.Badge.Niveau.BRONZE,
                Badge.builder().code("x").nom("x").build().getNiveau());
        assertEquals(MemberRequest.Statut.OUVERT,
                MemberRequest.builder().userId(UUID.randomUUID()).message("m").build().getStatut());
        assertEquals(StatutSuiviParallele.EN_COURS,
                ParallelFollowup.builder().ameId(UUID.randomUUID()).build().getStatut());
        assertEquals(StatutAlerte.ACTIVE,
                Alert.builder().titre("t").build().getStatut());
        assertEquals(CourseEnrollment.Statut.INSCRIT,
                CourseEnrollment.builder().userId(UUID.randomUUID()).courseId(UUID.randomUUID()).build().getStatut());
        assertEquals(Course.Niveau.DEBUTANT,
                Course.builder().titre("c").build().getNiveau());
        assertEquals(Appointment.Statut.EN_ATTENTE,
                Appointment.builder().demandeurId(UUID.randomUUID()).build().getStatut());
        assertEquals(Visit.StatutVisite.PLANIFIEE,
                Visit.builder().soulId(UUID.randomUUID()).build().getStatut());
        assertEquals(Objective.Periode.MENSUEL,
                Objective.builder().role(com.discipolat.common.domain.UserRole.FAISEUR).build().getPeriode());
        assertEquals(StatutValidation.BROUILLON,
                FamilyReport.builder().familleId(UUID.randomUUID()).build().getStatutValidation());
    }
}
