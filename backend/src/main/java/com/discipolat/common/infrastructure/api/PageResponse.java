package com.discipolat.common.infrastructure.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Réponse paginée normalisée renvoyée par tous les endpoints listés.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    /** Convertit une page Spring Data en PageResponse. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, 0, 0L, 0);
    }
}

