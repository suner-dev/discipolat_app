package com.discipolat.modules.platform.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.api.UpdateChurchSettingsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Gestion du singleton de configuration d'identité & de marque.
 * - Lecture publique (landing page, thème dynamique)
 * - Lecture authentifiée (vue complète)
 * - Mise à jour réservée à l'administrateur, tracée dans le journal d'audit.
 */
@Service
@Transactional
public class ChurchSettingsService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final ChurchSettingsRepository repository;
    private final AuditService auditService;

    public ChurchSettingsService(ChurchSettingsRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /** Retourne la configuration existante, ou crée une configuration par défaut si absente. */
    public ChurchSettings getSettings() {
        return repository.findFirstByOrderByCreatedAtAsc().orElseGet(this::createDefault);
    }

    /** Met à jour partiellement la configuration (seuls les champs non-null sont appliqués). */
    public ChurchSettings update(UpdateChurchSettingsRequest req) {
        ChurchSettings settings = getSettings();
        Map<String, Object> before = snapshot(settings);

        if (req.churchName() != null && !req.churchName().isBlank()) settings.setChurchName(req.churchName().trim());
        if (req.platformName() != null && !req.platformName().isBlank()) settings.setPlatformName(req.platformName().trim());
        if (req.slogan() != null) settings.setSlogan(req.slogan().trim());
        if (req.description() != null) settings.setDescription(req.description().trim());
        if (req.logoUrl() != null) settings.setLogoUrl(req.logoUrl().trim());
        if (req.faviconUrl() != null) settings.setFaviconUrl(req.faviconUrl().trim());
        if (req.bannerUrl() != null) settings.setBannerUrl(req.bannerUrl().trim());
        if (req.primaryColor() != null && HEX_COLOR.matcher(req.primaryColor()).matches()) settings.setPrimaryColor(req.primaryColor());
        if (req.accentColor() != null && HEX_COLOR.matcher(req.accentColor()).matches()) settings.setAccentColor(req.accentColor());
        if (req.buttonColor() != null && HEX_COLOR.matcher(req.buttonColor()).matches()) settings.setButtonColor(req.buttonColor());
        if (req.fontFamily() != null && !req.fontFamily().isBlank()) settings.setFontFamily(req.fontFamily().trim());
        if (req.allowDarkMode() != null) settings.setAllowDarkMode(req.allowDarkMode());
        if (req.address() != null) settings.setAddress(req.address().trim());
        if (req.phone() != null) settings.setPhone(req.phone().trim());
        if (req.email() != null) settings.setEmail(req.email().trim());
        if (req.website() != null) settings.setWebsite(req.website().trim());
        if (req.socialLinks() != null) settings.setSocialLinks(new LinkedHashMap<>(req.socialLinks()));
        if (req.contactNotes() != null) settings.setContactNotes(req.contactNotes().trim());

        repository.save(settings);
        auditService.log("UPDATE_CHURCH_SETTINGS", "CHURCH_SETTINGS", settings.getId(),
                before, snapshot(settings), null);
        return settings;
    }

    /** Réinitialise la configuration vers les valeurs par défaut. */
    public ChurchSettings resetToDefaults() {
        ChurchSettings settings = getSettings();
        settings.setChurchName("Discipolat");
        settings.setPlatformName("Discipolat");
        settings.setSlogan(null);
        settings.setDescription(null);
        settings.setLogoUrl(null);
        settings.setFaviconUrl(null);
        settings.setBannerUrl(null);
        settings.setPrimaryColor(ChurchSettings.DEFAULT_PRIMARY_COLOR);
        settings.setAccentColor(ChurchSettings.DEFAULT_ACCENT_COLOR);
        settings.setButtonColor(ChurchSettings.DEFAULT_BUTTON_COLOR);
        settings.setFontFamily(ChurchSettings.DEFAULT_FONT_FAMILY);
        settings.setAllowDarkMode(true);
        settings.setAddress(null);
        settings.setPhone(null);
        settings.setEmail(null);
        settings.setWebsite(null);
        settings.setSocialLinks(new LinkedHashMap<>());
        settings.setContactNotes(null);
        repository.save(settings);
        auditService.logSimple("RESET_CHURCH_SETTINGS", "CHURCH_SETTINGS", settings.getId());
        return settings;
    }

    private ChurchSettings createDefault() {
        ChurchSettings settings = ChurchSettings.builder().build();
        return repository.save(settings);
    }

    private Map<String, Object> snapshot(ChurchSettings s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("churchName", s.getChurchName());
        map.put("platformName", s.getPlatformName());
        map.put("slogan", s.getSlogan());
        map.put("logoUrl", s.getLogoUrl());
        map.put("faviconUrl", s.getFaviconUrl());
        map.put("bannerUrl", s.getBannerUrl());
        map.put("primaryColor", s.getPrimaryColor());
        map.put("accentColor", s.getAccentColor());
        map.put("buttonColor", s.getButtonColor());
        map.put("fontFamily", s.getFontFamily());
        map.put("allowDarkMode", s.isAllowDarkMode());
        map.put("address", s.getAddress());
        map.put("phone", s.getPhone());
        map.put("email", s.getEmail());
        map.put("website", s.getWebsite());
        map.put("socialLinks", s.getSocialLinks());
        return map;
    }
}
