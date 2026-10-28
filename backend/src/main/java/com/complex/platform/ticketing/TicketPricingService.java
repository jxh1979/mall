package com.complex.platform.ticketing;

// 文件版本：V0.03

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketPricingService {

    private static final BigDecimal MAX_DISCOUNT_RATE = BigDecimal.valueOf(100);
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel");
    private static final Set<String> DYNAMIC_ADJUSTMENT_TYPES = Set.of("percent_delta", "fixed_delta");

    private final TicketingRepository repository;

    public TicketPricingService(TicketingRepository repository) {
        this.repository = repository;
    }

    TicketDynamicPricingRuleRecord createDynamicPricingRule(TicketDynamicPricingRuleRequest request) {
        TicketDynamicPricingRuleRequest actual = requireRequest(request);
        String ticketTypeId = requireText(actual.ticketTypeId(), "ticket_type_id");
        repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        LocalDate effectiveFrom = actual.effectiveFrom() == null ? LocalDate.now() : actual.effectiveFrom();
        LocalDate effectiveTo = actual.effectiveTo() == null ? effectiveFrom : actual.effectiveTo();
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效结束日期不能早于开始日期");
        }
        String adjustmentType = requireEnum(actual.adjustmentType(), DYNAMIC_ADJUSTMENT_TYPES, "adjustment_type");
        if ("percent_delta".equals(adjustmentType) && actual.adjustmentRate() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "adjustment_rate is required");
        }
        if ("fixed_delta".equals(adjustmentType) && actual.adjustmentAmountCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "adjustment_amount_cent is required");
        }
        validateNonNegative(actual.minPriceCent(), "min_price_cent");
        validateNonNegative(actual.maxPriceCent(), "max_price_cent");
        if (actual.minPriceCent() != null && actual.maxPriceCent() != null && actual.minPriceCent() > actual.maxPriceCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "最低价不能高于最高价");
        }
        Instant now = Instant.now();
        return repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                "dynamic_price_rule_" + slug(actual.idempotencyKey()),
                ticketTypeId,
                requireText(actual.ruleName(), "rule_name"),
                actual.priority() == null ? 0 : actual.priority(),
                effectiveFrom,
                effectiveTo,
                trimToNull(actual.salesChannel()),
                adjustmentType,
                actual.adjustmentRate(),
                actual.adjustmentAmountCent(),
                actual.minPriceCent(),
                actual.maxPriceCent(),
                "draft",
                actual.conditionRule() == null ? Map.of() : Map.copyOf(actual.conditionRule()),
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    TicketDynamicPricingRuleRecord publishDynamicPricingRule(String ruleId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketDynamicPricingRuleRecord existing = repository.findDynamicPricingRuleById(ruleId)
                .orElseThrow(() -> ApiException.notFound("DYNAMIC_PRICING_RULE_NOT_FOUND", "动态定价规则不存在或已被删除"));
        Instant now = Instant.now();
        return repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                existing.id(),
                existing.ticketTypeId(),
                existing.ruleName(),
                existing.priority(),
                existing.effectiveFrom(),
                existing.effectiveTo(),
                existing.salesChannel(),
                existing.adjustmentType(),
                existing.adjustmentRate(),
                existing.adjustmentAmountCent(),
                existing.minPriceCent(),
                existing.maxPriceCent(),
                "published",
                existing.conditionRule(),
                trimToDefault(existing.operatorNote(), actual.reason()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now
        ));
    }

    /** today：未带到访日时取票种当前营业日（ENUM:187），由下单服务按票种归属算。 */
    TicketDynamicPricingSimulationResponse simulateDynamicPricing(TicketDynamicPricingSimulationRequest request,
            java.util.function.Function<TicketTypeRecord, LocalDate> today) {
        TicketDynamicPricingSimulationRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(actual.ticketTypeId(), "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        LocalDate visitDate = actual.visitDate() == null ? today.apply(ticketType) : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        long basePrice = resolveUnitPrice(ticketType, null, visitDate);
        Map<String, Object> context = actual.context() == null ? Map.of() : actual.context();
        TicketDynamicPricingRuleRecord matched = matchDynamicPricingRule(
                ticketType.id(),
                visitDate,
                salesChannel,
                context,
                Set.of("draft", "published")
        );
        if (matched == null) {
            return new TicketDynamicPricingSimulationResponse(
                    ticketType.id(),
                    visitDate,
                    salesChannel,
                    basePrice,
                    0L,
                    basePrice,
                    null,
                    "no matching dynamic pricing rule"
            );
        }
        long rawAdjustment = dynamicAdjustment(basePrice, matched);
        long finalPrice = clampPrice(basePrice + rawAdjustment, matched.minPriceCent(), matched.maxPriceCent());
        return new TicketDynamicPricingSimulationResponse(
                ticketType.id(),
                visitDate,
                salesChannel,
                basePrice,
                finalPrice - basePrice,
                finalPrice,
                matched.id(),
                "matched " + matched.ruleName()
        );
    }

    long resolveUnitPrice(TicketTypeRecord ticketType, TicketPriceRuleRecord priceRule, LocalDate visitDate) {
        Long base = switch (visitDate.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> firstNonNull(ticketType.weekendPriceCent(), ticketType.weekdayPriceCent(), ticketType.holidayPriceCent());
            default -> firstNonNull(ticketType.weekdayPriceCent(), ticketType.weekendPriceCent(), ticketType.holidayPriceCent());
        };
        if (base == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "票种必须填写基础价格");
        }
        if (priceRule == null) {
            return base;
        }
        Long fixed = switch (visitDate.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> firstNonNull(priceRule.weekendPriceCent(), priceRule.weekdayPriceCent(), priceRule.holidayPriceCent());
            default -> firstNonNull(priceRule.weekdayPriceCent(), priceRule.weekendPriceCent(), priceRule.holidayPriceCent());
        };
        if ("fixed_price".equals(priceRule.priceMode())) {
            if (fixed == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写固定价格");
            }
            return fixed;
        }
        BigDecimal discountRate = priceRule.discountRate() == null ? MAX_DISCOUNT_RATE : priceRule.discountRate();
        return BigDecimal.valueOf(base)
                .multiply(discountRate)
                .divide(MAX_DISCOUNT_RATE, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** ROOT-03-R27 客人价：基础价、动态公开价、命中资格价（每条一行）、最终选中规则（公开价最低时为空）与最终单价。 */
    public record CustomerPrice(long baseUnitCent, DynamicPriceResult publicPrice, List<Map<String, Object>> hits,
            TicketPriceRuleRecord selected, long finalUnitCent) {
    }

    /**
     * ROOT-03-R27（BSM 第 756 行、ENUM 第 58 行「先按日期、时段、场次和容量算动态公开价，再计算会员、新人、学生、团体和渠道等全部符合条件的价格；
     * 多种资格价不叠加，选择最终价格最低的一项」；第 782 行「会员价、渠道价、动态价都在对应基础价上……派生」）：动态公开价与各资格价都从日历基础价派生，
     * 只算客人符合的类型（eligibleVariants，由客人域判定）且在售（未存档、未待删）的规则，取最低。
     */
    CustomerPrice resolveCustomerPrice(TicketTypeRecord ticketType, LocalDate visitDate, String salesChannel,
            Set<String> eligibleVariants) {
        long base = resolveUnitPrice(ticketType, null, visitDate);
        DynamicPriceResult publicPrice = applyPublishedDynamicPricing(ticketType.id(), visitDate, salesChannel, base, Map.of());
        long best = publicPrice.finalUnitPriceCent();
        TicketPriceRuleRecord selected = null;
        List<Map<String, Object>> hits = new java.util.ArrayList<>();
        for (TicketPriceRuleRecord rule : repository.listPriceRules()) {
            if (!ticketType.id().equals(rule.ticketTypeId()) || !"active".equals(rule.status()) || rule.archivedAt() != null
                    || !"none".equals(rule.deleteStatus()) || !eligibleVariants.contains(rule.variantType())) {
                continue;
            }
            long price = resolveUnitPrice(ticketType, rule, visitDate);
            hits.add(Map.of("rule_id", rule.id(), "variant_type", rule.variantType(), "unit_price_cent", price,
                    "rule_updated_at", String.valueOf(rule.updatedAt())));
            if (price < best) {
                best = price;
                selected = rule;
            }
        }
        return new CustomerPrice(base, publicPrice, hits, selected, best);
    }

    /**
     * 第 75 条（ENUM L52「按当前赠送权益预估成本与当前最低实付价组合计算」）：该票种当前可能的最低实付单价，供结算域第一段试算。
     * 基础价取日历价（工作日、周末各一）与每条在售价格规则（会员、新人、学生、团体、渠道等资格价）在这两种日子的价；
     * 再对每个基础价叠加每条已发布、未过期的动态定价规则（不论渠道与条件，含最低价、最高价夹值）——覆盖客人价「动态公开价与资格价取低」
     * 与后台、渠道按价格规则下单再叠动态价两条路径；取全部组合里的最小值。
     */
    public long lowestUnitPriceCent(String ticketTypeId, LocalDate today) {
        TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        List<Long> bases = new java.util.ArrayList<>();
        for (LocalDate day : List.of(today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY)),
                today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY)))) {
            bases.add(resolveUnitPrice(ticketType, null, day));
            for (TicketPriceRuleRecord rule : repository.listPriceRules()) {
                if (ticketTypeId.equals(rule.ticketTypeId()) && "active".equals(rule.status()) && rule.archivedAt() == null
                        && "none".equals(rule.deleteStatus())) {
                    bases.add(resolveUnitPrice(ticketType, rule, day));
                }
            }
        }
        List<TicketDynamicPricingRuleRecord> dynamics = repository.listDynamicPricingRules().stream()
                .filter(rule -> ticketTypeId.equals(rule.ticketTypeId()) && "published".equals(rule.status())
                        && !rule.effectiveTo().isBefore(today))
                .toList();
        long lowest = Long.MAX_VALUE;
        for (long base : bases) {
            lowest = Math.min(lowest, base);
            for (TicketDynamicPricingRuleRecord rule : dynamics) {
                lowest = Math.min(lowest, clampPrice(base + dynamicAdjustment(base, rule), rule.minPriceCent(), rule.maxPriceCent()));
            }
        }
        return lowest;
    }

    DynamicPriceResult applyPublishedDynamicPricing(
            String ticketTypeId,
            LocalDate visitDate,
            String salesChannel,
            long unitPrice,
            Map<String, Object> context
    ) {
        TicketDynamicPricingRuleRecord matched = matchDynamicPricingRule(
                ticketTypeId,
                visitDate,
                salesChannel,
                context == null ? Map.of() : context,
                Set.of("published")
        );
        if (matched == null) {
            return new DynamicPriceResult(unitPrice, 0L, null);
        }
        long finalUnitPrice = clampPrice(unitPrice + dynamicAdjustment(unitPrice, matched), matched.minPriceCent(), matched.maxPriceCent());
        return new DynamicPriceResult(finalUnitPrice, finalUnitPrice - unitPrice, matched.id());
    }

    private TicketDynamicPricingRuleRecord matchDynamicPricingRule(
            String ticketTypeId,
            LocalDate visitDate,
            String salesChannel,
            Map<String, Object> context,
            Set<String> statuses
    ) {
        Map<String, Object> actualContext = context == null ? Map.of() : context;
        return repository.listDynamicPricingRules().stream()
                .filter(rule -> ticketTypeId.equals(rule.ticketTypeId()))
                .filter(rule -> statuses.contains(rule.status()))
                .filter(rule -> !visitDate.isBefore(rule.effectiveFrom()) && !visitDate.isAfter(rule.effectiveTo()))
                .filter(rule -> rule.salesChannel() == null || rule.salesChannel().equals(salesChannel))
                .filter(rule -> matchesCondition(rule.conditionRule(), actualContext))
                .sorted((left, right) -> {
                    int priority = Integer.compare(right.priority(), left.priority());
                    return priority != 0 ? priority : right.updatedAt().compareTo(left.updatedAt());
                })
                .findFirst()
                .orElse(null);
    }

    private boolean matchesCondition(Map<String, Object> conditionRule, Map<String, Object> context) {
        if (conditionRule == null || conditionRule.isEmpty()) {
            return true;
        }
        return conditionRule.entrySet().stream()
                .allMatch(entry -> {
                    Object actual = context.get(entry.getKey());
                    return actual != null && String.valueOf(actual).equals(String.valueOf(entry.getValue()));
                });
    }

    private long dynamicAdjustment(long basePrice, TicketDynamicPricingRuleRecord rule) {
        if ("fixed_delta".equals(rule.adjustmentType())) {
            return rule.adjustmentAmountCent() == null ? 0L : rule.adjustmentAmountCent();
        }
        BigDecimal rate = rule.adjustmentRate() == null ? BigDecimal.ZERO : rule.adjustmentRate();
        return BigDecimal.valueOf(basePrice)
                .multiply(rate)
                .divide(MAX_DISCOUNT_RATE, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private long clampPrice(long value, Long minPriceCent, Long maxPriceCent) {
        long nonNegative = Math.max(0L, value);
        long clamped = minPriceCent == null ? nonNegative : Math.max(nonNegative, minPriceCent);
        return maxPriceCent == null ? clamped : Math.min(clamped, maxPriceCent);
    }

    private Long firstNonNull(Long first, Long second, Long third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String actual = requireText(value, field);
        if (!allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return actual;
    }

    private void validateNonNegative(Long value, String field) {
        if (value != null && value < 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 不能为负数");
        }
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> evidenceOrExisting(List<String> values, List<String> existing) {
        List<String> evidence = evidence(values);
        return evidence.isEmpty() ? List.copyOf(existing == null ? List.of() : existing) : evidence;
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
