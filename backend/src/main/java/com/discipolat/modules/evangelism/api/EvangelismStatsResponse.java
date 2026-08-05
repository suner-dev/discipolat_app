package com.discipolat.modules.evangelism.api;

import java.util.Map;

public record EvangelismStatsResponse(
        long totalAmes,
        Map<String, Long> parEtape
) {
}
