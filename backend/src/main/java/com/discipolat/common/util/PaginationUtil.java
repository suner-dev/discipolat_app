package com.discipolat.common.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PaginationUtil {

    private PaginationUtil() {}

    public static Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        return PageRequest.of(Math.max(0, page), Math.min(size, 100), sort);
    }

    public static Pageable createPageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(size, 100));
    }
}
