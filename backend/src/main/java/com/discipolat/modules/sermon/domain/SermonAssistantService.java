package com.discipolat.modules.sermon.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SermonAssistantService {

    public Map<String, Object> generateOutlines(String passage, String theme, String audience, String durationMinutes) {
        return Map.of(
            "outlines", List.of(),
            "status", "stub"
        );
    }
}
