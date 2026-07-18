package com.complex.platform.contract;

import com.complex.platform.common.api.ApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 合同经营范围越界拦截（2026-07-18 拍板：下单侧读合同经营范围并拦超范围经营）。
 *
 * <p>数据形态（零迁移）：contracts.rent_terms(JSONB) 增结构化键 {@code allowedCategories}（字符串数组）。
 * 词表复用既有业务类目，不新建枚举：
 * <ul>
 *   <li>域级：{@code bar/ticketing/amusement} —— V103 chk_risk_alerts_source_domain 既有 CHECK 词表的消费域子集；
 *       {@code catering/retail/service/entertainment} —— merchant_type 既有词表（V005 + 前端 MERCHANT_TYPES）。</li>
 *   <li>品类级（酒吧商品粗类别 product_type）：{@code spirit/wine/beer/cocktail/soft_drink/mixer/tea/snack}
 *       —— 前端 BarProductsPage CATEGORY_LABELS 既有词表。</li>
 * </ul>
 *
 * <p>拦截语义：
 * <ul>
 *   <li>酒吧下单：合同配了 allowedCategories 且不含域级 {@code bar} 时，逐明细比对商品粗类别；任一品类
 *       不在允许集 → 整单拒（CONTRACT_SCOPE_VIOLATION）。含 {@code bar} = 放行全部酒吧品类。</li>
 *   <li>票务出票（单店票挂酒吧商户）：域级比对，允许集须含 {@code ticketing}。</li>
 * </ul>
 *
 * <p>fail-open 口径（有意为之）：拦截是合规增强，不能让主销售路径因合同数据缺失而不可用——
 * 找不到生效合同 / 同商户多份生效合同 / 合同查询抛异常，一律不拦截并记 WARN 日志留痕；
 * 存量合同（rent_terms 无 allowedCategories 键或为空数组）同样不拦截（只有配置了范围的合同才生效）。
 */
@Service
public class ContractScopeGuard {

    private static final Logger log = LoggerFactory.getLogger(ContractScopeGuard.class);

    static final String RENT_TERMS_KEY = "allowedCategories";

    /** 域级：V103 风险告警 source_domain 既有 CHECK 词表中的消费域。 */
    public static final String DOMAIN_BAR = "bar";
    public static final String DOMAIN_TICKETING = "ticketing";
    public static final String DOMAIN_AMUSEMENT = "amusement";

    /**
     * 合法取值全集（禁止发明新枚举，全部取自既有词表）：
     * 域级 3（V103）+ 商户业态 4（merchant_type 词表，声明用，本轮无对应拦截点）+ 酒吧商品粗类别 8。
     */
    public static final Set<String> ALLOWED_CATEGORY_VALUES = Set.of(
            DOMAIN_BAR, DOMAIN_TICKETING, DOMAIN_AMUSEMENT,
            "catering", "retail", "service", "entertainment",
            "spirit", "wine", "beer", "cocktail", "soft_drink", "mixer", "tea", "snack");

    /** 错误消息用中文名（全中文强制）；与前端词表同源。 */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(DOMAIN_BAR, "酒吧"), Map.entry(DOMAIN_TICKETING, "门票销售"),
            Map.entry(DOMAIN_AMUSEMENT, "游乐"), Map.entry("catering", "餐饮"),
            Map.entry("retail", "零售"), Map.entry("service", "服务"),
            Map.entry("entertainment", "娱乐"), Map.entry("spirit", "烈酒"),
            Map.entry("wine", "葡萄酒"), Map.entry("beer", "啤酒"),
            Map.entry("cocktail", "鸡尾酒"), Map.entry("soft_drink", "软饮"),
            Map.entry("mixer", "调饮"), Map.entry("tea", "茶饮"), Map.entry("snack", "小吃"));

    private final ContractRepository repository;

    public ContractScopeGuard(ContractRepository repository) {
        this.repository = repository;
    }

    /**
     * 校验并规范化经营范围取值（合同创建 / business_scope_change 变更载荷共用）。
     * null → null（未配置）；非法值 → VALIDATION_FAILED（中文指明非法项）。
     */
    public static List<String> validateAllowedCategories(Object raw) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Collection<?> values)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "allowed_categories 须为字符串数组");
        }
        List<String> normalized = new ArrayList<>();
        for (Object value : values) {
            String text = value == null ? "" : String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }
            if (!ALLOWED_CATEGORY_VALUES.contains(text)) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "allowed_categories 含系统外取值「" + text + "」，经营范围只能勾选既有业务类目");
            }
            if (!normalized.contains(text)) {
                normalized.add(text);
            }
        }
        return normalized;
    }

    /** 酒吧下单拦截：任一明细品类越界 → 整单拒。categories = 本单全部商品粗类别（去重）。 */
    public void requireBarSaleAllowed(String merchantId, Collection<String> categories) {
        List<String> allowed = effectiveAllowedCategories(merchantId);
        if (allowed == null || allowed.contains(DOMAIN_BAR)) {
            return;
        }
        for (String category : categories) {
            if (category != null && !allowed.contains(category)) {
                throw scopeViolation("品类「" + label(category) + "」不在本商户合同约定的经营范围内，禁止销售");
            }
        }
    }

    /** 票务出票拦截（单店票挂酒吧商户）：域级比对，允许集须含 ticketing。 */
    public void requireTicketSaleAllowed(String merchantId) {
        List<String> allowed = effectiveAllowedCategories(merchantId);
        if (allowed == null || allowed.contains(DOMAIN_TICKETING)) {
            return;
        }
        throw scopeViolation("本商户合同约定的经营范围不含「门票销售」，禁止出票");
    }

    private static ApiException scopeViolation(String message) {
        return ApiException.conflict("CONTRACT_SCOPE_VIOLATION", "超出合同经营范围：" + message);
    }

    private static String label(String code) {
        return LABELS.getOrDefault(code, code);
    }

    /**
     * 读该商户当前唯一生效合同的 allowedCategories；null = 不拦截。
     * 生效谓词抄合同域既有口径（JdbcContractRepository：lifecycle_status='effective' 且今日落在租期窗内）。只读不写。
     */
    private List<String> effectiveAllowedCategories(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return null;
        }
        List<ContractRecord> effective;
        try {
            LocalDate today = LocalDate.now();
            effective = repository.listContractsByMerchant(merchantId).stream()
                    .filter(c -> "effective".equals(c.lifecycleStatus()))
                    .filter(c -> c.effectiveDate() != null && c.expiryDate() != null
                            && !today.isBefore(c.effectiveDate()) && !today.isAfter(c.expiryDate()))
                    .toList();
        } catch (RuntimeException exception) {
            // fail-open（有意口径）：合同域查询故障不能拖垮销售主路径，放行并留痕。
            log.warn("contract scope check fail-open: merchant={} contract query failed", merchantId, exception);
            return null;
        }
        if (effective.size() != 1) {
            // fail-open（有意口径）：无生效合同 / 多份生效合同（数据异常）→ 不拦截，WARN 留痕给合同域治理。
            log.warn("contract scope check fail-open: merchant={} effective contract count={} (expect 1)",
                    merchantId, effective.size());
            return null;
        }
        Map<String, Object> rentTerms = effective.get(0).rentTerms();
        Object raw = rentTerms == null ? null : rentTerms.get(RENT_TERMS_KEY);
        if (!(raw instanceof Collection<?> values) || values.isEmpty()) {
            return null;   // 存量合同未配置经营范围 → 不拦截（兼容口径）。
        }
        return values.stream().map(String::valueOf).toList();
    }
}
