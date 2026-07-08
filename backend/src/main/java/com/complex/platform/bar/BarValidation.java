package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class BarValidation {

    private BarValidation() {
    }

    static <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    static String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    static Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    static LocalDate requireLocalDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    static long positive(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved <= 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    static long nonNegative(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved < 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    static int nonNegative(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    static List<Map<String, Object>> requireItems(List<Map<String, Object>> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return items;
    }

    static String requireItemText(Map<String, Object> item, String... names) {
        String value = textFromMap(item, names);
        return requireText(value, String.join("/", names));
    }

    static int requirePositiveItemQuantity(Map<String, Object> item, String name) {
        Object value = item == null ? null : item.get(name);
        int quantity = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
        if (quantity <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", name + " must be positive");
        }
        return quantity;
    }

    static List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static String textFromMap(Map<String, Object> map, String... names) {
        if (map == null) {
            return null;
        }
        for (String name : names) {
            Object value = map.get(name);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    static String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String slug(String value) {
        String source = trimToNull(value);
        if (source == null) {
            source = UUID.randomUUID().toString();
        }
        String normalized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        // 单号防溢出:拼接型输入(账单号+商品号+套餐号…)normalized 会很长,超出 id 字段上限(varchar 64)。
        // >40 时改用定长哈希(酒吧单号前缀最长约17字符,17+32=49<=64),短 slug 保持可读不变(零兼容影响)。
        if (normalized.length() > 40) {
            try {
                return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 32);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("slug hashing unavailable", e);
            }
        }
        return normalized;
    }

    static String jsonText(String value) {
        return trimToDefault(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}