package com.example.Back.template.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long    total,
        long    totalPages,
        int     page,
        int     size
) {
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(items, total, totalPages, page, size);
    }
}
