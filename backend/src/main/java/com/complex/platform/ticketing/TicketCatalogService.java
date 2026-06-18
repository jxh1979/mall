package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TicketCatalogService {

    private static final String CURRENCY_CNY = "CNY";
    private static final BigDecimal MIN_DISCOUNT_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_DISCOUNT_RATE = BigDecimal.valueOf(100);
    private static final Set<String> PRICE_VARIANTS = Set.of("group", "new_customer", "student", "channel", "member");
    private static final Set<String> PRICE_MODES = Set.of("derive_from_calendar_price", "fixed_price");

    private final TicketingRepository repository;

    public TicketCatalogService(TicketingRepository repository) {
        this.repository = repository;
    }

    TicketTypeRecord createTicketType(TicketTypeRequest request) {
        TicketTypeRequest actual = requireRequest(request);
        Instant now = Instant.now();
        return repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_" + slug(actual.idempotencyKey()),
                requireText(actual.ticketName(), "ticket_name"),
                requireText(actual.baseCalendarPriceMode(), "base_calendar_price_mode"),
                actual.weekdayPriceCent(),
                actual.weekendPriceCent(),
                actual.holidayPriceCent(),
                actual.stockRequired() != null && actual.stockRequired(),
                trimToDefault(actual.status(), "active"),
                now,
                now
        ));
    }

    TicketPriceRuleRecord createPriceRule(TicketPriceRuleRequest request) {
        TicketPriceRuleRequest actual = requireRequest(request);
        String ticketTypeId = requireText(actual.ticketTypeId(), "ticket_type_id");
        repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        String variantType = requireEnum(actual.variantType(), PRICE_VARIANTS, "variant_type");
        String priceMode = requireEnum(actual.priceMode(), PRICE_MODES, "price_mode");
        if ("fixed_price".equals(priceMode)
                && actual.weekdayPriceCent() == null
                && actual.weekendPriceCent() == null
                && actual.holidayPriceCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "fixed price requires at least one price");
        }
        if ("derive_from_calendar_price".equals(priceMode)
                && (actual.discountRate() == null
                || actual.discountRate().compareTo(MIN_DISCOUNT_RATE) < 0
                || actual.discountRate().compareTo(MAX_DISCOUNT_RATE) > 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "discount_rate must be between 0 and 100");
        }
        validateNonNegative(actual.weekdayPriceCent(), "weekday_price_cent");
        validateNonNegative(actual.weekendPriceCent(), "weekend_price_cent");
        validateNonNegative(actual.holidayPriceCent(), "holiday_price_cent");
        Instant now = Instant.now();
        return repository.savePriceRule(new TicketPriceRuleRecord(
                "price_rule_" + slug(actual.idempotencyKey()),
                ticketTypeId,
                variantType,
                priceMode,
                actual.weekdayPriceCent(),
                actual.weekendPriceCent(),
                actual.holidayPriceCent(),
                actual.discountRate(),
                trimToNull(actual.channelId()),
                CURRENCY_CNY,
                "active",
                actual.eligibilityRule() == null ? Map.of() : Map.copyOf(actual.eligibilityRule()),
                now,
                now
        ));
    }

    EntitlementTemplateRecord createEntitlementTemplate(EntitlementTemplateRequest request) {
        EntitlementTemplateRequest actual = requireRequest(request);
        Instant now = Instant.now();
        return repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_" + slug(actual.idempotencyKey()),
                requireText(actual.name(), "name"),
                requireText(actual.entitlementType(), "entitlement_type"),
                positiveOrDefault(actual.defaultTotalTimes(), 1, "default_total_times"),
                nonNegativeOrDefault(actual.defaultValueCent(), 0L, "default_value_cent"),
                trimToDefault(actual.status(), "active"),
                now,
                now
        ));
    }

    TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        TicketTypeEntitlementRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        EntitlementTemplateRecord template = repository.findEntitlementTemplateById(requireText(actual.entitlementTemplateId(), "entitlement_template_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
        Instant now = Instant.now();
        return repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "ticket_link_" + slug(actual.idempotencyKey()),
                ticketType.id(),
                template.id(),
                positiveOrDefault(actual.quantity(), 1, "quantity"),
                positiveOrDefault(actual.totalTimes(), template.defaultTotalTimes(), "total_times"),
                actual.valueAmountCent() == null ? template.defaultValueCent() : actual.valueAmountCent(),
                trimToNull(actual.effectiveRule()),
                now,
                now
        ));
    }

    TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        Instant now = Instant.now();
        return repository.saveTicketType(new TicketTypeRecord(
                current.id(),
                current.ticketName(),
                current.baseCalendarPriceMode(),
                current.weekdayPriceCent(),
                current.weekendPriceCent(),
                current.holidayPriceCent(),
                current.stockRequired(),
                "published",
                current.createdAt(),
                now
        ));
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = requireText(value, fieldName);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        int resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private long nonNegativeOrDefault(Long value, long defaultValue, String fieldName) {
        long resolved = value == null ? defaultValue : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private void validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
