package com.foodorder.menuservice.model;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum SizeOption {
    S,
    M,
    L;

    public static SizeOption fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return M;
        }
        try {
            return SizeOption.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size; use S, M, or L");
        }
    }
}
