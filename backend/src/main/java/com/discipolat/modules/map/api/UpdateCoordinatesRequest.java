package com.discipolat.modules.map.api;

import jakarta.validation.constraints.NotNull;

public record UpdateCoordinatesRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String zone
) {
}
