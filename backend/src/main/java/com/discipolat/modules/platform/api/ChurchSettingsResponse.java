package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.ChurchSettings;

import java.util.Map;
import java.util.UUID;

/** Vue complète des paramètres (utilisateurs authentifiés). */
public record ChurchSettingsResponse(
        UUID id,
        String churchName,
        String platformName,
        String slogan,
        String description,
        String logoUrl,
        String faviconUrl,
        String bannerUrl,
        String primaryColor,
        String accentColor,
        String buttonColor,
        String fontFamily,
        boolean allowDarkMode,
        String address,
        String phone,
        String email,
        String website,
        Map<String, String> socialLinks,
        String contactNotes
) {
    public static ChurchSettingsResponse from(ChurchSettings s) {
        return new ChurchSettingsResponse(
                s.getId(), s.getChurchName(), s.getPlatformName(), s.getSlogan(), s.getDescription(),
                s.getLogoUrl(), s.getFaviconUrl(), s.getBannerUrl(),
                s.getPrimaryColor(), s.getAccentColor(), s.getButtonColor(),
                s.getFontFamily(), s.isAllowDarkMode(),
                s.getAddress(), s.getPhone(), s.getEmail(), s.getWebsite(),
                s.getSocialLinks() != null ? s.getSocialLinks() : Map.of(),
                s.getContactNotes());
    }
}
