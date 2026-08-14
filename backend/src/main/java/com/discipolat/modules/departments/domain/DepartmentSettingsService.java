package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Paramétrage du département : seuils configurables des alertes
 * intelligentes. Les règles d'alerte lisent ces valeurs (jamais de
 * constantes hardcodées) et l'administrateur/responsable peut les
 * ajuster depuis l'écran Paramètres.
 * <p>
 * L'accès passe par {@link DepartmentService#findById} : un responsable
 * ne configure que SES départements.
 */
@Service
@Transactional
public class DepartmentSettingsService {

    private final DepartmentService departmentService;
    private final DepartmentSettingRepository settingRepository;

    public DepartmentSettingsService(DepartmentService departmentService,
                                     DepartmentSettingRepository settingRepository) {
        this.departmentService = departmentService;
        this.settingRepository = settingRepository;
    }

    /** Retourne les paramètres du département (ligne créée avec les valeurs par défaut si absente). */
    @Transactional(readOnly = true)
    public Map<String, Object> getSettings(UUID departmentId) {
        departmentService.findById(departmentId);
        DepartmentSetting settings = loadOrCreate(departmentId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("departmentId", settings.getDepartmentId());
        m.put("absenceSeuil", settings.getAbsenceSeuil());
        m.put("absencePeriode", settings.getAbsencePeriode());
        m.put("inactiviteMois", settings.getInactiviteMois());
        m.put("tacheRetardAlerte", settings.isTacheRetardAlerte());
        m.put("eventRappelJours", settings.getEventRappelJours());
        return m;
    }

    /** Met à jour les seuils fournis (champs absents = inchangés). */
    public Map<String, Object> updateSettings(UUID departmentId, Map<String, Object> values) {
        departmentService.findById(departmentId);
        DepartmentSetting settings = loadOrCreate(departmentId);

        Integer absenceSeuil = intValue(values.get("absenceSeuil"));
        if (absenceSeuil != null) {
            if (absenceSeuil < 1 || absenceSeuil > 10) {
                throw new BusinessRuleException(
                        "Le seuil d'absence doit être compris entre 1 et 10", "ABSENCE_SEUIL_OUT_OF_RANGE");
            }
            settings.setAbsenceSeuil(absenceSeuil);
        }
        Integer absencePeriode = intValue(values.get("absencePeriode"));
        if (absencePeriode != null) {
            if (absencePeriode < 1 || absencePeriode > 12) {
                throw new BusinessRuleException(
                        "La période d'absence doit être comprise entre 1 et 12 semaines", "ABSENCE_PERIODE_OUT_OF_RANGE");
            }
            settings.setAbsencePeriode(absencePeriode);
        }
        Integer inactiviteMois = intValue(values.get("inactiviteMois"));
        if (inactiviteMois != null) {
            if (inactiviteMois < 0 || inactiviteMois > 24) {
                throw new BusinessRuleException(
                        "La période d'inactivité doit être comprise entre 0 et 24 mois", "INACTIVITE_MOIS_OUT_OF_RANGE");
            }
            settings.setInactiviteMois(inactiviteMois);
        }
        if (values.containsKey("tacheRetardAlerte") && values.get("tacheRetardAlerte") != null) {
            settings.setTacheRetardAlerte(Boolean.parseBoolean(String.valueOf(values.get("tacheRetardAlerte"))));
        }
        Integer eventRappelJours = intValue(values.get("eventRappelJours"));
        if (eventRappelJours != null) {
            if (eventRappelJours < 0 || eventRappelJours > 30) {
                throw new BusinessRuleException(
                        "Le délai de rappel d'événement doit être compris entre 0 et 30 jours",
                        "EVENT_RAPPEL_JOURS_OUT_OF_RANGE");
            }
            settings.setEventRappelJours(eventRappelJours);
        }

        settingRepository.save(settings);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("departmentId", settings.getDepartmentId());
        m.put("absenceSeuil", settings.getAbsenceSeuil());
        m.put("absencePeriode", settings.getAbsencePeriode());
        m.put("inactiviteMois", settings.getInactiviteMois());
        m.put("tacheRetardAlerte", settings.isTacheRetardAlerte());
        m.put("eventRappelJours", settings.getEventRappelJours());
        return m;
    }

    /** Paramètres effectifs du département pour les règles d'alertes (jamais null). */
    @Transactional(readOnly = true)
    public DepartmentSetting effectiveSettings(UUID departmentId) {
        return loadOrCreate(departmentId);
    }

    private DepartmentSetting loadOrCreate(UUID departmentId) {
        return settingRepository.findById(departmentId).orElseGet(() -> {
            DepartmentSetting settings = DepartmentSetting.builder()
                    .departmentId(departmentId)
                    .build();
            return settingRepository.save(settings);
        });
    }

    private static Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
