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
                // R5 审计点②：validity_days 暂无创建入口（DTO 未扩展），保持 null=回退默认 1 天。
                // 后续若需按票种配置有效期，再扩展 TicketTypeRequest + 此处赋值。
                null,
                // P1 清单#19：refresh_interval_seconds 暂无创建入口（DTO 未扩展），保持 null=回退默认 60 秒。
                // 后续若需按票种配置刷新窗口，再扩展 TicketTypeRequest + 此处赋值（取值 {30,60}）。
                null,
                now,
                now,
                // P0 修复（V133）：库存上限取自请求（可空=不限量）；已售从 0 起。
                actual.stockQuantity(),
                0
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
        String entitlementType = requireText(actual.entitlementType(), "entitlement_type");
        return repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_" + slug(actual.idempotencyKey()),
                requireText(actual.name(), "name"),
                entitlementType,
                positiveOrDefault(actual.defaultTotalTimes(), 1, "default_total_times"),
                nonNegativeOrDefault(actual.defaultValueCent(), 0L, "default_value_cent"),
                trimToDefault(actual.status(), "active"),
                // #3 配置入口：模板归属（merchant/venue/point）由请求透传（各可选）。
                // 为空时发放回退出票方商户；联合通票各模板可填各自归属。
                trimToNull(actual.merchantId()),
                trimToNull(actual.venueId()),
                trimToNull(actual.pointId()),
                // #3 配置入口：赠饮→吧台 SKU 映射（bar_item_ref）由请求透传（可选）。
                // 为空时赠饮核销跳过库存扣减并标注。
                trimToNull(actual.barItemRef()),
                now,
                now,
                // 结算重构批3-1(PRD §6.1)：新建模板结算类别与 V225 回填口径一致——gift_drink=实物(走内部结算价)，
                // 其余=虚拟(走签约单价)。签约单价暂无配置入口，留空(判定层回退 value_amount)，待运营后台补录。
                "gift_drink".equals(entitlementType) ? "physical" : "virtual",
                null,
                // 结算重构批3-2(PRD §6.2)：新建模板默认逐次扣(per_use，行为同现状)；入场资格「场次规则」(per_session)
                // 暂无配置入口，待运营后台补 consumption_window 编辑项（判定层对 NULL/per_use 走现状）。
                "per_use",
                // 批3-4：新建模板非专属(exclusive_merchant_id=null，走池分账)；专属由运营后台后补编辑项。
                null,
                // 批3-6(用户2026-07-04)：拍脸照开关由请求透传(可空=false)；游乐模板走 V229 种子回填默认开，新建按请求。
                Boolean.TRUE.equals(actual.requireFacePhoto())
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
                // #3 配置入口：每份核销人数（num_people）由请求透传，缺省 1（单人票，向后兼容）；多人票/联合票配 >1。
                positiveOrDefault(actual.numPeople(), 1, "num_people"),
                // 待确认项#1 配置入口：分项标准单价/手动单价（可选，可空回退）。联合票各分项配单价以按比例拆分订单实付。
                nonNegativeOrNull(actual.componentPriceCent(), "component_price_cent"),
                now,
                now
        ));
    }

    TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        requireComponentPricesForMultiBusinessPass(current.id());
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
                current.validityDays(),
                current.refreshIntervalSeconds(),
                current.createdAt(),
                now,
                // P0 修复（V133）：发布时保留既有库存上限与已售数，避免被置空/清零。
                current.stockQuantity(),
                current.soldQuantity()
        ));
    }

    /**
     * S6(2026-07-02 拍板⑤):联合通票(权益横跨多业态,如酒吧+游乐)按分项单价比例拆分订单实付;
     * 分项单价没填全时发布直接拦(400),不许均摊兜底——逐绑定行校验,不按模板聚合放水。
     */
    private void requireComponentPricesForMultiBusinessPass(String ticketTypeId) {
        java.util.List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        java.util.Set<String> businessTypes = new java.util.HashSet<>();
        for (TicketTypeEntitlementRecord binding : bindings) {
            repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .map(EntitlementTemplateRecord::entitlementType)
                    .ifPresent(businessTypes::add);
        }
        if (businessTypes.size() <= 1) {
            return;
        }
        java.util.List<String> missing = bindings.stream()
                .filter(binding -> binding.componentPriceCent() == null)
                .map(TicketTypeEntitlementRecord::id)
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "multi-business pass requires component_price_cent on every entitlement binding; missing: " + missing);
        }
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

    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return value;
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
