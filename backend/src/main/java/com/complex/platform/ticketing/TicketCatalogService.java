package com.complex.platform.ticketing;

// 文件版本：V0.04

import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest;
import com.complex.platform.ticketing.dto.TicketTypePublishRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeGiftFloorRequest;
import com.complex.platform.ticketing.dto.TicketTypePricingRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketCatalogService {

    private static final String CURRENCY_CNY = "CNY";
    private static final BigDecimal MIN_DISCOUNT_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_DISCOUNT_RATE = BigDecimal.valueOf(100);
    private static final Set<String> PRICE_VARIANTS = Set.of("group", "new_customer", "student", "channel", "member");
    private static final Set<String> PRICE_MODES = Set.of("derive_from_calendar_price", "fixed_price");
    // 基础日历价模式白名单：业务口径只有「平日/周末/节假日三类基础价」一种
    // （业务系统地图「平日票、周末票、节假日票（基础价，优先级最高）」）。
    // 与上面派生价规则的 PRICE_MODES 是两个不同字段，勿混用。
    private static final Set<String> BASE_CALENDAR_PRICE_MODES = Set.of("weekday_weekend_holiday");
    // 结算重构批4（V230）：票种资格/定价档分类；null/standard=标准（与 DB CHECK 一致）。
    private static final Set<String> TICKET_CATEGORIES = Set.of("standard", "new_person", "student");
    // 结算重构批3-1（V225）：签约价只对虚拟权益取用（实物走吧台商品内部结算价）。
    private static final String KIND_VIRTUAL = "virtual";

    private final TicketingRepository repository;
    // AUD-076：按 id 取酒吧商户（判业态/存档/取名）。可为 null=兼容既有单测（不校验酒吧、不解析名称）。
    private final BarMerchantDirectory barDirectory;
    // 第十八批 AI 段 236-R2：同包无状态解析器，发布与加权益时查提供方内部结算价是否已获批生效（与核销取价同一处）。
    private final InternalSettlementPriceResolver priceResolver;

    // V369：凑整允许误差（后台可配，取自结算配置中心「通票分账 → 套票折扣归属」那一行）。
    // 字段注入免改构造点；生产由 Spring 恒注入 JdbcSettlementBillRepository，端口自身缺配置时 fail-loud。
    // 为 null（旧构造/未注入的单测）= 不做误差校验，与 barDirectory 同范式。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    ComboPriceToleranceConfigPort comboPriceToleranceConfigPort;

    // V369：解析当前登录人的项目公司作用域，用于按项目取允许误差。为 null 时取全局默认作用域。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.access.AuthorizationService authorizationService;

    // 第十八批 AJ 段 236-R3：跨域组合通票票种规则版本（字段注入免改构造点；单测未注入＝不识别跨域、发布与改动行为不变）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    TicketTypeRuleVersionService ruleVersions;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketCatalogService(TicketingRepository repository, BarMerchantDirectory barDirectory) {
        this.repository = repository;
        this.barDirectory = barDirectory;
        this.priceResolver = new InternalSettlementPriceResolver(repository);
    }

    TicketTypeRecord createTicketType(TicketTypeRequest request) {
        TicketTypeRequest actual = requireRequest(request);
        Instant now = Instant.now();
        return repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_" + slug(actual.idempotencyKey()),
                requireText(actual.ticketName(), "ticket_name"),
                requireEnum(actual.baseCalendarPriceMode(), BASE_CALENDAR_PRICE_MODES, "base_calendar_price_mode"),
                actual.weekdayPriceCent(),
                actual.weekendPriceCent(),
                actual.holidayPriceCent(),
                actual.stockRequired() != null && actual.stockRequired(),
                // AUD-015 建票固定 draft：必须先绑权益、再走发布闸（校验≥1个绑定+理由）才可售。
                // 此前允许直接建 active → 客户端公开列表可见「可售但无权益」的票，付款后出票必失败。
                "draft",
                // AUD-004：有效期天数进入建票链路（可空=默认1天；正数校验）。
                requireValidityDays(actual.validityDays()),
                // #172(2026-07-19)：票种级刷新秒数休眠列（全场统一改由 ticket_qr_refresh_config 驱动）；容错校验保留。
                requireRefreshIntervalSeconds(actual.refreshIntervalSeconds()),
                now,
                now,
                // P0 修复（V133）：库存上限取自请求（可空=不限量）；已售从 0 起。
                actual.stockQuantity(),
                0,
                // 结算重构批4（V230）：资格/定价档分类标签（可空=标准；非空校验取值，DB 另有 CHECK 兜底）。
                normalizeTicketCategory(actual.ticketCategory()),
                // 方案2/规则2（V286）：是否可退票（可空=不可退，默认最安全）。
                actual.refundable() != null && actual.refundable(),
                // AUD-076 单店票挂酒吧：校验并挂定酒吧归属（非空必须是业态 bar 且未存档的商户；建后不可变）。
                validateBarBinding(actual.barId()),
                // V369：票种折扣（0~100 百分数，可空=100 不打折）。系统据此自动算出售价。
                requireDiscountRate(actual.comboDiscountRate())
        ));
    }

    /**
     * V369 改售价／折扣（业务 HTML:741「运营可在此基础上填一个近似售价用于凑整……上调下调均可」）。
     *
     * <p><b>为什么改价也要当场卡</b>：让运营先把一个明知超限的价存下去、等到点发布才报错，等于让他白填一遍，
     * 而且中间这段时间票种页上挂着一个错的价，别人看了会以为已经定了。所以保存这一步就比对。
     *
     * <p>只动定价三档与折扣，其余（库存/有效期/权益/已售数）一律原样不动。
     */
    TicketTypeRecord updatePricing(String ticketTypeId, TicketTypePricingRequest request) {
        TicketTypePricingRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        // 第十八批 AJ 段 236-R3：待审期间不许再改（须先撤回提交）。
        requireNoPendingRuleVersion(requireText(ticketTypeId, "ticket_type_id"));
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        if (current.archivedAt() != null || !"none".equals(current.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE", "已存档或待删除的票种不可改售价");
        }
        TicketTypeRecord updated = current.withPricing(
                requireDiscountRate(actual.comboDiscountRate()),
                nonNegativeOrNull(actual.weekdayPriceCent(), "weekday_price_cent"),
                nonNegativeOrNull(actual.weekendPriceCent(), "weekend_price_cent"),
                nonNegativeOrNull(actual.holidayPriceCent(), "holiday_price_cent"),
                Instant.now());
        java.util.List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(current.id());
        // 第十八批 AJ 段 236-R7（第十八批裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」）：已上架跨域组合通票改价
        //   只存进草稿版（同样当场过误差闸，按草稿组成算），现表与售卖不变。
        if (toDraftVersion(current, bindings)) {
            Draft draft = openDraft(current, bindings);
            requirePriceWithinTolerance(updated, draft.bindings());
            ruleVersions.saveOpen(current.id(), snapshot(updated, draft.bindings(), draft.floor()));
            return current;
        }
        requirePriceWithinTolerance(updated, bindings);
        return repository.saveTicketType(updated);
    }

    /** V369 定价核算（前端建票/改票时实时提示用）：算出售价、差额、允许误差、超没超。 */
    TicketTypePriceCheck priceCheck(String ticketTypeId) {
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        long tolerance = comboPriceToleranceConfigPort == null
                ? 0L : comboPriceToleranceConfigPort.findRoundingToleranceCent(currentProjectId());
        return priceCheck(ticketType, repository.listTicketTypeEntitlements(ticketType.id()), tolerance);
    }

    /** V369：票种折扣（百分数）校验，可空=100 不打折；越界拒（DB 另有 CHECK 兜底）。 */
    private BigDecimal requireDiscountRate(BigDecimal rate) {
        if (rate == null) {
            return TicketTypeRecord.NO_COMBO_DISCOUNT;
        }
        if (rate.compareTo(MIN_DISCOUNT_RATE) < 0 || rate.compareTo(MAX_DISCOUNT_RATE) > 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "票种折扣必须在 0~100 之间（100=不打折）");
        }
        return rate;
    }

    TicketPriceRuleRecord createPriceRule(TicketPriceRuleRequest request) {
        TicketPriceRuleRequest actual = requireRequest(request);
        String ticketTypeId = requireText(actual.ticketTypeId(), "ticket_type_id");
        TicketTypeRecord ruleTicketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        // 复核§3(V260)：已存档/待删除票种不可再配派生价规则——否则"存档=软下线"名存实亡，
        // 且新引用会让审批删除永远过不了引用校验（与本范式的硬删闸互相打架）。
        if (ruleTicketType.archivedAt() != null || !"none".equals(ruleTicketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE",
                    "已存档或待删除的票种不可配置派生价规则");
        }
        String variantType = requireEnum(actual.variantType(), PRICE_VARIANTS, "variant_type");
        String priceMode = requireEnum(actual.priceMode(), PRICE_MODES, "price_mode");
        if ("fixed_price".equals(priceMode)
                && actual.weekdayPriceCent() == null
                && actual.weekendPriceCent() == null
                && actual.holidayPriceCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "固定定价至少需要一个价格");
        }
        if ("derive_from_calendar_price".equals(priceMode)
                && (actual.discountRate() == null
                || actual.discountRate().compareTo(MIN_DISCOUNT_RATE) < 0
                || actual.discountRate().compareTo(MAX_DISCOUNT_RATE) > 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "折扣率必须在 0 到 100 之间");
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
        String templateMerchantId = requireMerchant(actual.merchantId());
        String barItemRef = trimToNull(actual.barItemRef());
        // AUD-015：赠饮(gift_drink)模板启用前必须绑定提供方商品(bar_item_ref)。缺映射则核销时
        // 无法生成 0 元出品单/扣吧台库存/进结算（业务图 :727,1044-1050；bar PRD §15.2 第 2 条）。
        if ("gift_drink".equals(entitlementType)
                && "active".equals(trimToDefault(actual.status(), "active"))
                && barItemRef == null) {
            throw ApiException.badRequest("GIFT_TEMPLATE_ITEM_UNMAPPED",
                    "赠饮模板启用前必须配置提供方的商品");
        }
        // S5：bar_item_ref 语义定死 = bar_products.id，且配置当场就把三件事查实，别留到客人站在吧台前才炸：
        //   ① 这个商品真的存在；② 它确实是这家提供方的商品（A 吧的酒不能算作 B 吧请的）；
        //   ③ 若这个商品要扣库存，它必须已绑好库存物料，否则核销时扣不动库。
        if (barItemRef != null) {
            BarProductRefRecord product = repository.findBarProductRef(barItemRef)
                    .orElseThrow(() -> ApiException.badRequest("GIFT_TEMPLATE_ITEM_NOT_FOUND",
                            "所选的提供方商品不存在"));
            if (!templateMerchantId.equals(product.barId())) {
                throw ApiException.badRequest("GIFT_TEMPLATE_ITEM_MERCHANT_MISMATCH",
                        "所选商品不属于该权益的提供方商户");
            }
            if (product.stockDeductionRequired() && trimToNull(product.inventoryItemId()) == null) {
                throw ApiException.badRequest("GIFT_TEMPLATE_ITEM_STOCK_UNMAPPED",
                        "该商品需要扣库存但还没绑定库存物料，不能用作赠送权益");
            }
        }
        // 批10/E(V380)：内部结算价唯一可编辑处 = SKU 目录。直填签约价一律拒绝(静默忽略会骗运营"以为配好了")；
        // 要配价请引用目录条目(catalog_item_code,可就地新建后引用),条目绑定商户必须 = 本模板提供方。
        if (actual.agreementPriceCent() != null) {
            throw ApiException.badRequest("AGREEMENT_PRICE_CATALOG_ONLY",
                    "内部结算价（签约价）已收编进内部结算 SKU 目录：请引用目录条目，改价到目录走草案复核");
        }
        String catalogItemCode = trimToNull(actual.catalogItemCode());
        Long snapshotPriceCent = null;
        if (catalogItemCode != null) {
            String boundMerchant = repository.findCatalogItemMerchant(catalogItemCode)
                    .orElseThrow(() -> ApiException.badRequest("CATALOG_ITEM_NOT_FOUND",
                            "所引用的内部结算 SKU 目录条目不存在"));
            if (!templateMerchantId.equals(boundMerchant)) {
                throw ApiException.badRequest("CATALOG_ITEM_MERCHANT_MISMATCH",
                        "目录条目的绑定商户与本权益的提供方商户不一致，不得跨商户/跨项目引用");
            }
            // 快照 = 当前目录生效价;条目还只有草案(未复核转正)时快照为空,转正传播时由系统补上。
            snapshotPriceCent = repository.findActiveCatalogPrice(catalogItemCode)
                    .map(TicketingRepository.CatalogPriceRef::priceCent).orElse(null);
        }
        return repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_" + slug(actual.idempotencyKey()),
                requireText(actual.name(), "name"),
                entitlementType,
                positiveOrDefault(actual.defaultTotalTimes(), 1, "default_total_times"),
                nonNegativeOrDefault(actual.defaultValueCent(), 0L, "default_value_cent"),
                trimToDefault(actual.status(), "active"),
                // 审计断层①(2026-07-05):模板必须绑商户。不绑则核销结算款因月度成账 SQL
                // (JdbcSettlementBillRepository 的 merchant_id IS NOT NULL)永远进不了账单→孤儿。
                // 从源头强制非空;联合通票各模板可填各自归属(venue/point 仍可选)。
                // S5：该商户即本权益的「提供方」——客人须到这家核销、成本按这家的内部结算价计。
                templateMerchantId,
                trimToNull(actual.venueId()),
                trimToNull(actual.pointId()),
                // S5：提供方商品（bar_item_ref = bar_products.id），上方已校验存在 + 归属 + 扣库前置。
                barItemRef,
                now,
                now,
                // 结算重构批3-1(PRD §6.1)：新建模板结算类别与 V225 回填口径一致——gift_drink=实物(走内部结算价)，
                // 其余=虚拟(走签约单价/内部结算价)。餐饮(dining,用户2026-07-06)属虚拟——内部结算价复用 agreement_price_cent 承载。
                "gift_drink".equals(entitlementType) ? "physical" : "virtual",
                // 批10/E(V380)：agreement_price_cent 自此为目录价快照(只读)——上方已按目录引用取当前生效价;
                //   未挂引用/条目未转正=null,判定层对提供方兑付类 fail-closed、其余回退 value_amount(行为同现状)。
                snapshotPriceCent,
                // 结算重构批3-2(PRD §6.2)：新建模板默认逐次扣(per_use，行为同现状)；入场资格「场次规则」(per_session)
                // 暂无配置入口，待运营后台补 consumption_window 编辑项（判定层对 NULL/per_use 走现状）。
                "per_use",
                // 批3-4/餐饮(用户2026-07-06)：专属商户「不分摊」由请求透传(可空=非专属走池)；餐饮填餐厅商户使核销全额按内部结算价直付餐厅。
                trimToNull(actual.exclusiveMerchantId())
        // 批10/E：挂目录引用(withCatalogRef 同步写快照;code 为空时等价于原构造,快照亦空)。
        ).withCatalogRef(catalogItemCode, snapshotPriceCent, now));
    }

    // 批10/E：原「改虚拟权益签约价」端点（PR#274 updateAgreementPrice）已下线——内部结算价唯一可编辑处收编为
    // SKU 目录（草案 → 他人复核转正 → 快照传播），模板侧不再保留任何改价入口。「历史不受影响」不变且更强：
    // 核销固化（ticket_verifications.settlement_price_cent）照旧，改价只对转正之后的核销/结算生效。

    // ================= V253/V260 数据存档与审批后删除范式（权益模板试点 → 镜像铺到票种/价格规则） =================
    // 三套 archive/requestDelete/approveDelete 逐字同构（原 V260 段注释即自证「镜像上方权益模板」），公共骨架
    // 收拢进下方三个泛型助手；各实体差异（错误码/中文文案/引用计数口径/物理删除）逐字留在各自接线处。

    /** 存档/取消存档权益模板（软标记，可逆；列表默认过滤已存档）。 */
    EntitlementTemplateRecord archiveEntitlementTemplate(String id, boolean archive, String reason, String actorId) {
        return archiveCatalogRecord(requireTemplate(id), id, archive, reason, actorId,
                repository::saveEntitlementTemplate, repository::setEntitlementTemplateArchiveReason);
    }

    /** 发起删除申请：须先存档（防误删在用模板），记 requested_by/at/reason，状态置 pending。 */
    EntitlementTemplateRecord requestDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteRequest request, String actorId) {
        return requestDeleteCatalogRecord(requireTemplate(id), request == null ? null : request.reason(), actorId,
                repository::saveEntitlementTemplate,
                () -> ApiException.badRequest("ENTITLEMENT_TEMPLATE_NOT_ARCHIVED", "模板须先存档才能申请删除"),
                () -> ApiException.conflict("ENTITLEMENT_TEMPLATE_DELETE_ALREADY_PENDING", "该模板已有待审批的删除申请"));
    }

    /**
     * 审批删除申请。approved 且无活引用 → 物理删除；rejected → 回 none。
     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review SUPERVISOR_SELF_REVIEW_FORBIDDEN 原则）。
     */
    EntitlementTemplateDeleteDecision approveDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteApprovalRequest request, String actorId) {
        boolean deleted = approveDeleteCatalogRecord(requireTemplate(id), id,
                request == null ? null : request.decision(), actorId, repository::saveEntitlementTemplate,
                repository::countTemplateReferences, repository::deleteEntitlementTemplate,
                () -> ApiException.badRequest("ENTITLEMENT_TEMPLATE_DELETE_NOT_PENDING", "该模板没有待审批的删除申请"),
                () -> ApiException.forbidden("ENTITLEMENT_TEMPLATE_SELF_APPROVE_FORBIDDEN", "删除申请的发起人不能审批自己的申请"),
                refs -> ApiException.conflict("ENTITLEMENT_TEMPLATE_IN_USE", "该模板仍被 " + refs + " 个票种绑定或已发放权益引用"));
        return new EntitlementTemplateDeleteDecision(id, deleted ? "approved" : "rejected", deleted);
    }

    TicketTypeRecord archiveTicketType(String id, boolean archive, String reason, String actorId) {
        return archiveCatalogRecord(requireTicketTypeRecord(id), id, archive, reason, actorId,
                repository::saveTicketType, repository::setTicketTypeArchiveReason);
    }

    TicketTypeRecord requestDeleteTicketType(String id, TicketCatalogDeleteRequest request, String actorId) {
        return requestDeleteCatalogRecord(requireTicketTypeRecord(id), request == null ? null : request.reason(), actorId,
                repository::saveTicketType,
                () -> ApiException.badRequest("TICKET_TYPE_NOT_ARCHIVED", "票种必须先存档才能申请删除"),
                () -> ApiException.conflict("TICKET_TYPE_DELETE_ALREADY_PENDING", "该票种已有待审批的删除申请"));
    }

    TicketTypeDeleteDecision approveDeleteTicketType(String id, TicketCatalogDeleteApprovalRequest request, String actorId) {
        boolean deleted = approveDeleteCatalogRecord(requireTicketTypeRecord(id), id,
                request == null ? null : request.decision(), actorId, repository::saveTicketType,
                repository::countTicketTypeReferences, repository::deleteTicketType,
                () -> ApiException.badRequest("TICKET_TYPE_DELETE_NOT_PENDING", "该票种没有待审批的删除申请"),
                () -> ApiException.forbidden("TICKET_TYPE_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请"),
                refs -> ApiException.conflict("TICKET_TYPE_IN_USE",
                        "票种仍被 " + refs + " 条业务记录引用（订单/权益/价格规则/结算等），不可删除"));
        return new TicketTypeDeleteDecision(id, deleted ? "approved" : "rejected", deleted);
    }

    TicketPriceRuleRecord archivePriceRule(String id, boolean archive, String reason, String actorId) {
        return archiveCatalogRecord(requirePriceRuleRecord(id), id, archive, reason, actorId,
                repository::savePriceRule, repository::setPriceRuleArchiveReason);
    }

    TicketPriceRuleRecord requestDeletePriceRule(String id, TicketCatalogDeleteRequest request, String actorId) {
        return requestDeleteCatalogRecord(requirePriceRuleRecord(id), request == null ? null : request.reason(), actorId,
                repository::savePriceRule,
                () -> ApiException.badRequest("TICKET_PRICE_RULE_NOT_ARCHIVED", "价格规则必须先存档才能申请删除"),
                () -> ApiException.conflict("TICKET_PRICE_RULE_DELETE_ALREADY_PENDING", "该价格规则已有待审批的删除申请"));
    }

    TicketPriceRuleDeleteDecision approveDeletePriceRule(String id, TicketCatalogDeleteApprovalRequest request, String actorId) {
        boolean deleted = approveDeleteCatalogRecord(requirePriceRuleRecord(id), id,
                request == null ? null : request.decision(), actorId, repository::savePriceRule,
                repository::countPriceRuleReferences, repository::deletePriceRule,
                () -> ApiException.badRequest("TICKET_PRICE_RULE_DELETE_NOT_PENDING", "该价格规则没有待审批的删除申请"),
                () -> ApiException.forbidden("TICKET_PRICE_RULE_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请"),
                refs -> ApiException.conflict("TICKET_PRICE_RULE_IN_USE", "价格规则仍被 " + refs + " 条订单记录引用，不可删除"));
        return new TicketPriceRuleDeleteDecision(id, deleted ? "approved" : "rejected", deleted);
    }

    /**
     * 存档/取消存档公共骨架（{@link ArchivableRecord} 三实现共用）。
     * 存档必填理由（业务 HTML 2026-07-07），行已存在、archive_reason 定向写入，顺序无关；
     * 取消存档清空 archived_at/by + archive_reason，并把 pending 删除申请回退 none（复核§5）。
     */
    private <T extends ArchivableRecord<T>> T archiveCatalogRecord(T rec, String id, boolean archive,
            String reason, String actorId, UnaryOperator<T> save, BiConsumer<String, String> setArchiveReason) {
        Instant now = Instant.now();
        if (archive) {
            String r = requireText(reason, "reason");
            T saved = save.apply(rec.withArchive(now, actorId, now));
            setArchiveReason.accept(id, r);
            return saved;
        }
        T saved = save.apply(rec.withArchive(null, null, now).withDelete("none", null, null, null, now));
        setArchiveReason.accept(id, null);
        return saved;
    }

    /** 发起删除申请公共骨架：须先存档（防误删在用记录），记 requested_by/at/reason，状态置 pending。 */
    private <T extends ArchivableRecord<T>> T requestDeleteCatalogRecord(T rec, String reason, String actorId,
            UnaryOperator<T> save, Supplier<ApiException> notArchived, Supplier<ApiException> alreadyPending) {
        if (rec.archivedAt() == null) {
            throw notArchived.get();
        }
        if ("pending".equals(rec.deleteStatus())) {
            throw alreadyPending.get();
        }
        Instant now = Instant.now();
        return save.apply(rec.withDelete("pending", actorId, now, requireText(reason, "reason"), now));
    }

    /**
     * 审批删除申请公共骨架。approved 且无活引用 → 物理删除（返回 true）；rejected → 回 none（返回 false）。
     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review SUPERVISOR_SELF_REVIEW_FORBIDDEN 原则）。
     */
    private <T extends ArchivableRecord<T>> boolean approveDeleteCatalogRecord(T rec, String id,
            String rawDecision, String actorId, UnaryOperator<T> save,
            ToLongFunction<String> countReferences, Consumer<String> physicalDelete,
            Supplier<ApiException> notPending, Supplier<ApiException> selfApprove,
            LongFunction<ApiException> inUse) {
        if (!"pending".equals(rec.deleteStatus())) {
            throw notPending.get();
        }
        String decision = rawDecision == null ? null : rawDecision.trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权上下文/测试）时跳过该闸。
        if (actorId != null && actorId.equals(rec.deleteRequestedBy())) {
            throw selfApprove.get();
        }
        if ("rejected".equals(decision)) {
            save.apply(rec.withDelete("none", null, null, null, Instant.now()));
            return false;
        }
        // approved：先校验无活引用（票种绑定/已发放权益/订单等），有则拒绝硬删。
        long refs = countReferences.applyAsLong(id);
        if (refs > 0) {
            throw inUse.apply(refs);
        }
        physicalDelete.accept(id);
        return true;
    }

    private TicketTypeRecord requireTicketTypeRecord(String id) {
        return repository.findTicketTypeById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
    }

    private TicketPriceRuleRecord requirePriceRuleRecord(String id) {
        return repository.findPriceRuleById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_PRICE_RULE_NOT_FOUND", "价格规则不存在或已被删除"));
    }

    private EntitlementTemplateRecord requireTemplate(String id) {
        return repository.findEntitlementTemplateById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "权益模板不存在或已被删除"));
    }

    TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        TicketTypeEntitlementRequest actual = requireRequest(request);
        // 第十八批 AJ 段 236-R3：待审期间不许再改（须先撤回提交）。
        requireNoPendingRuleVersion(requireText(ticketTypeId, "ticket_type_id"));
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        // 复核§3(V260)：票种侧同样拒绑——已存档/待删除票种不可再被绑定新权益。
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_BINDABLE",
                    "已存档或待删除的票种不可绑定权益");
        }
        EntitlementTemplateRecord template = repository.findEntitlementTemplateById(requireText(actual.entitlementTemplateId(), "entitlement_template_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "权益模板不存在或已被删除"));
        // 复核§3:已存档或待删除的模板不可再被绑定到票种——否则违背"存档=软下线",且被引用后永远删不掉。
        if (template.archivedAt() != null || !"none".equals(template.deleteStatus())) {
            throw ApiException.conflict("ENTITLEMENT_TEMPLATE_NOT_BINDABLE",
                    "已存档或待删除的权益模板不可绑定到票种");
        }
        // AUD 复核 P2-5：单店票（票种 bar_id 非空）只能绑归属本店的权益模板——出票时权益 merchant 优先取 template.merchantId，
        // 若模板 merchant_id 属他商户 B 而票种 bar_id=A，则出票权益 merchant=B/父票 bar=A 归属分裂（撤场按 A、结算按 B）。
        // 模板 merchant_id 为空时出票回退酒吧归属，天然一致，放行；现无联合票类型故不开例外。
        String bindTypeBarId = trimToNull(ticketType.barId());
        String bindTemplateMerchantId = trimToNull(template.merchantId());
        if (bindTypeBarId != null && bindTemplateMerchantId != null
                && !bindTypeBarId.equals(bindTemplateMerchantId)) {
            throw ApiException.conflict("BAR_TICKET_TEMPLATE_MERCHANT_MISMATCH",
                    "单店票只能绑定归属本店的权益模板");
        }
        Instant now = Instant.now();
        TicketTypeEntitlementRecord binding = new TicketTypeEntitlementRecord(
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
                // S6/V376 配置入口(HTML:737「每个权益必须在票种配置里标明成本承担方」，2026-08-01 拍板改必填)：
                //   赠送类权益（票面原价 0 且提供方兑付类）漏标当场拒绝——不再默认 platform_funded，
                //   否则运营漏标一个商户让利型赠品，平台就替商户多付一笔预估成本。非赠送类无此概念，允许留空。
                requireCostBearer(actual.costBearer(),
                        isGiftClass(nonNegativeOrNull(actual.componentPriceCent(), "component_price_cent"), template),
                        template.name()),
                now,
                now
        );
        boolean published = "published".equals(ticketType.status());
        java.util.List<TicketTypeEntitlementRecord> current = repository.listTicketTypeEntitlements(ticketType.id());
        boolean crossDomain = published && crossDomain(ticketType, current);
        // 第十八批 AJ 段 236-R7（第十八批裁决 62「已上架的非跨域票一律不许加别的经营域收费权益，要做跨域组合通票就另建票种」）：已上架、现表不跨域的票
        //   加上本条后成为跨域组合通票的一律拒绝，绑定不落库、不退回草稿、不改状态；排在 236-R2 内部结算价闸与 V369、215-R1 退回草稿之前。
        //   零元权益不参与域间分配（BSM:749），本条为零元时前后判定相同、照现行；草稿票种照现行。
        if (published && !crossDomain && crossDomain(ticketType, withBinding(current, binding))) {
            throw ApiException.conflict("TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED", "已上架的票不能加别的经营域收费权益；要做跨域组合通票请另建票种"
                    + "（新建票种时勾选「先存为草稿、不立即上架」，配齐组合后在票种行点「发布」提交审批）");
        }
        if (published) {
            // 第十八批 AI 段 236-R2：已上架票种加须按内部结算价结算的权益，提供方价未获批即拒，绑定不落库、票种不退回草稿。
            requireApprovedInternalPrices(java.util.List.of(binding), "不能加到已上架票种");
        }
        // 第十八批 AJ 段 236-R7（第十八批裁决 61）：已上架跨域组合通票加权益只存进该票种唯一一份草稿版，现表与售卖不变，获批后自动换成新版。
        if (ruleVersions != null && crossDomain) {
            Draft draft = openDraft(ticketType, current);
            ruleVersions.saveOpen(ticketType.id(), snapshot(draft.type(), withBinding(draft.bindings(), binding), draft.floor()));
            return binding;
        }
        // V369：已上架的票被改了组成（加权益／改票面原价）后，售价若已经对不上新的业态构成，就把票退回草稿。
        //   直接拒绝会把「先建票、再一次性配组合权益」这条正常流程堵死；继续挂着卖则是让客人按一个
        //   系统自己都算不出来的价买。只在真的对不上时才下架，改完价重新发布即可（重新走一遍误差闸）。
        if (published && !priceWithinTolerance(ticketType, withBinding(current, binding))) {
            repository.saveTicketType(ticketType.withStatus("draft", now));
            published = false;
        }
        TicketTypeEntitlementRecord saved = repository.saveTicketTypeEntitlement(binding);
        // 第十八批 AH 段 215-R1（ENUM:34「低于该下限的票种不得发布上架」）：已上架通票改了组成后跌破最低赠酒杯数
        //   （如新增一家酒吧入场却漏配赠酒），同 V369 退回草稿，补齐后重新发布。
        if (published && barGiftCoverageViolation(ticketType) != null) {
            repository.saveTicketType(ticketType.withStatus("draft", now));
        }
        return saved;
    }

    /**
     * S6/V376：赠送类判定——票面原价 0/未配 且 提供方兑付类。谓词与 S5
     * {@link InternalSettlementPriceResolver#providerRedeemed} / S6 {@code byItemGiftCostCent} 同源不另造：
     * 只有这类权益有「成本承担方」概念（入场/付费组成无内部成本，别误伤）。
     */
    private static boolean isGiftClass(Long componentPriceCent, EntitlementTemplateRecord template) {
        return (componentPriceCent == null || componentPriceCent == 0L)
                && InternalSettlementPriceResolver.providerRedeemed(
                        template.entitlementType(), template.entitlementKind(), template.exclusiveMerchantId());
    }

    /**
     * S6/V376：成本承担方校验（2026-08-01 拍板：必填、无默认）。赠送类漏标 → 中文 400 点名权益；
     * 非赠送类留空合法（存 NULL）；坏值中文 400，不落到 DB CHECK 抛 500。
     */
    private String requireCostBearer(String costBearer, boolean giftClass, String templateName) {
        String value = trimToNull(costBearer);
        if (value == null) {
            if (giftClass) {
                throw ApiException.badRequest("TICKET_COST_BEARER_REQUIRED",
                        "赠送权益「" + templateName + "」还没标成本承担方（商户让利型/平台出资型），不能保存");
            }
            return null;
        }
        if (!TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED.equals(value)
                && !TicketTypeEntitlementRecord.COST_BEARER_MERCHANT_CONCESSION.equals(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "成本承担方仅支持 platform_funded(平台出资型) / merchant_concession(商户让利型)");
        }
        return value;
    }

    /** 现有绑定（现表或草稿版）+ 这条待写入的绑定（同 id 视为改这一条，不重复计）。 */
    private static java.util.List<TicketTypeEntitlementRecord> withBinding(java.util.List<TicketTypeEntitlementRecord> bindings,
            TicketTypeEntitlementRecord binding) {
        java.util.List<TicketTypeEntitlementRecord> merged = new java.util.ArrayList<>();
        for (TicketTypeEntitlementRecord existing : bindings) {
            if (!existing.id().equals(binding.id())) {
                merged.add(existing);
            }
        }
        merged.add(binding);
        return merged;
    }

    TicketTypeRecord publishTicketType(String ticketTypeId, TicketTypePublishRequest request) {
        TicketTypePublishRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        // 第十八批 AJ 段 236-R3：待审期间再发布 409（须先撤回提交）。
        requireNoPendingRuleVersion(requireText(ticketTypeId, "ticket_type_id"));
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        // 第十八批 AJ 段 236-R7（第十八批裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」）：已上架跨域组合通票有草稿版时，
        //   用草稿内容跑全部发布闸后提交平台权限审批，票保持上架、旧版照卖，获批后在审批同一事务里自动换成新版；没有草稿版的照现行。
        TicketTypeRuleVersionView open = ruleVersions == null || !"published".equals(current.status())
                ? null : ruleVersions.findOpen(current.id()).orElse(null);
        if (open != null) {
            Draft draft = draftOf(current, open);
            requirePublishable(draft.type(), draft.bindings(), () -> barGiftCoverageViolation(current.barId(),
                    providerRows(draft.bindings()), draft.floor() == null ? FACTORY_MIN_GIFT_CUPS : draft.floor().minGiftCups()));
            ruleVersions.submit(current, snapshot(draft.type(), draft.bindings(), draft.floor()), actual.reason(),
                    actual.finalApproverId(), actual.idempotencyKey());
            return current;
        }
        java.util.List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(current.id());
        requirePublishable(current, bindings, () -> barGiftCoverageViolation(current));
        // 第十八批 AJ 段 236-R3（第十八批裁决 52 ②；ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754）：
        //   未上架的跨域组合通票闸全过后提交平台权限审批、留一版规则，票仍为草稿（ENUM:30「草稿（规则未发布）」），获批即上架。
        if (ruleVersions != null && !"published".equals(current.status()) && crossDomain(current, bindings)) {
            ruleVersions.submit(current, snapshot(current, bindings, repository.findGiftFloor(current.id()).orElse(null)),
                    actual.reason(), actual.finalApproverId(), actual.idempotencyKey());
            return current;
        }
        // 发布只翻状态，其余（库存/已售/分类/可退/酒吧归属/折扣）一律原样带过，避免被置空/清零。
        return repository.saveTicketType(current.withStatus("published", Instant.now()));
    }

    /**
     * 发布闸（现表与 236-R7 草稿版同一套，次序不变）：至少一个权益、各业态价格、赠送出资方、通票赠酒下限、内部结算价、凑整误差。
     *
     * @param giftShortfall 通票赠酒下限核对（现表读现表；草稿版按草稿组成与草稿最低杯数）
     */
    private void requirePublishable(TicketTypeRecord ticketType, java.util.List<TicketTypeEntitlementRecord> bindings,
            Supplier<ApiException> giftShortfall) {
        // AUD-015 发布闸：至少绑定一个权益模板才可发布——公开可售的票必须能出票，否则形成收钱不出票。
        if (bindings.isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED",
                    "票种发布前必须至少绑定一个权益");
        }
        requireComponentPrices(bindings);
        requireCostBearers(bindings);
        ApiException shortfall = giftShortfall.get();
        if (shortfall != null) {
            throw shortfall;
        }
        requireApprovedInternalPrices(bindings, "不能发布");
        // V369 凑整误差闸：填的售价与系统算出的售价差太多就不许开卖（HTML:741「超出即拒绝发布」）。
        requirePriceWithinTolerance(ticketType, bindings);
    }

    /** S5 K17：入场类权益的类型（这些权益让客人能进这家吧的门）。 */
    private static final java.util.Set<String> ENTRY_ENTITLEMENT_TYPES = java.util.Set.of("entry", "admission");
    /** 酒吧业态（V348 商户业态五值封口）。 */
    private static final String MERCHANT_TYPE_BAR = "bar";
    /** 赠酒类权益。 */
    private static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";
    /** 第十八批 AH 段 215-R2：没配过的票种按出厂最低赠酒杯数（ENUM:34「出厂默认 1 杯」）。 */
    private static final int FACTORY_MIN_GIFT_CUPS = 1;

    /**
     * 第十八批 AH 段 215-R1 通票赠酒下限（ENUM:34「一张酒吧通票或跨域组合通票覆盖的每一家酒吧，赠酒杯数不得低于后台配置的最低赠酒杯数……
     * 低于该下限的票种不得发布上架……该最低赠酒杯数属于票面强制要求提供的权益，一律记为域主管公司出资」；BSM:1033「只有商户主动申请并书面确认
     * 让利的权益才记为商户让利、不结算」）：
     * <ul>
     *   <li>只管通票（票种 bar_id 为空），单店票不套（ENUM:34 只说酒吧通票或跨域组合通票）；</li>
     *   <li>覆盖的酒吧＝该票种实际绑定了入场类权益的酒吧，不按固定家数；</li>
     *   <li>赠酒只计票面价 0 的赠饮，带票面价的是兑换券（ENUM:36「酒水（赠饮/兑换券）」），不计入下限；每人份杯数＝份数×次数；</li>
     *   <li>多次票每完成一次检票开一个场次、附带权益按完整一份配足（ENUM:46）：该吧入场次数 N 时应有 N×最低杯数，与 165-R1 运行期
     *       「一份＝总次数÷入场次数」同口径；</li>
     *   <li>应有杯数以内须平台出资型，超出部分可标商户让利。</li>
     * </ul>
     *
     * @return 不过时的 409（发布闸抛出；改组成、改下限时据此把已上架通票退回草稿），过了返回 null
     */
    private ApiException barGiftCoverageViolation(TicketTypeRecord ticketType) {
        if (trimToNull(ticketType.barId()) != null) {
            return null;
        }
        return barGiftCoverageViolation(ticketType.barId(), repository.listTicketTypeProviderEntitlements(ticketType.id()),
                giftFloor(ticketType.id()).minGiftCups());
    }

    /** 同上，按给定的提供方行与最低杯数核（第十八批 AJ 段 236-R7：草稿版提交审批时传草稿组成与草稿最低杯数）。 */
    private ApiException barGiftCoverageViolation(String barId, java.util.List<TicketTypeProviderEntitlementRow> rows, int floor) {
        if (trimToNull(barId) != null) {
            return null;
        }
        java.util.Map<String, String> barNames = new java.util.HashMap<>();
        java.util.Map<String, Integer> visitsByBar = new java.util.HashMap<>();
        java.util.Map<String, Integer> cupsByBar = new java.util.HashMap<>();
        java.util.Map<String, Integer> platformCupsByBar = new java.util.HashMap<>();
        for (TicketTypeProviderEntitlementRow row : rows) {
            String bar = row.providerMerchantId();
            if (bar == null || !MERCHANT_TYPE_BAR.equals(row.providerMerchantType())) {
                continue;
            }
            barNames.putIfAbsent(bar, row.providerMerchantName() == null ? bar : row.providerMerchantName());
            if (ENTRY_ENTITLEMENT_TYPES.contains(row.entitlementType())) {
                visitsByBar.merge(bar, row.totalTimes(), Math::max);
            }
            if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(row.entitlementType())
                    && (row.componentPriceCent() == null || row.componentPriceCent() == 0L)) {
                int cups = row.quantity() * row.totalTimes();
                cupsByBar.merge(bar, cups, Integer::sum);
                if (TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED.equals(row.costBearer())) {
                    platformCupsByBar.merge(bar, cups, Integer::sum);
                }
            }
        }
        java.util.List<String> shortBars = new java.util.ArrayList<>();
        java.util.List<String> concessionBars = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> visits : visitsByBar.entrySet()) {
            String bar = visits.getKey();
            int required = floor * Math.max(1, visits.getValue());
            int cups = cupsByBar.getOrDefault(bar, 0);
            if (cups < required) {
                shortBars.add(barNames.get(bar) + "（需 " + required + " 杯，现 " + cups + " 杯）");
            }
            int platformCups = platformCupsByBar.getOrDefault(bar, 0);
            if (platformCups < required) {
                concessionBars.add(barNames.get(bar) + "（需 " + required + " 杯，平台出资 " + platformCups + " 杯）");
            }
        }
        if (!shortBars.isEmpty()) {
            return ApiException.conflict("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT", "这张通票覆盖的每家酒吧赠酒不得少于最低 " + floor
                    + " 杯（多次票按每次入场一份计），还差：" + String.join("、", shortBars.stream().sorted().toList()));
        }
        if (!concessionBars.isEmpty()) {
            return ApiException.conflict("PASS_BAR_GIFT_FLOOR_NOT_PLATFORM_FUNDED",
                    "最低赠酒杯数以内的赠酒一律记为域主管公司出资，请把这些酒吧的赠酒成本承担方改为平台出资型："
                            + String.join("、", concessionBars.stream().sorted().toList()));
        }
        return null;
    }

    /** 215-R2：该票种的最低赠酒杯数，没配过的按出厂 1 杯（上下限 HTML 未给出厂数值，留空）。 */
    private TicketTypeGiftFloor giftFloor(String ticketTypeId) {
        return repository.findGiftFloor(ticketTypeId).orElseGet(() -> new TicketTypeGiftFloor(
                ticketTypeId, FACTORY_MIN_GIFT_CUPS, null, null, null, null));
    }

    TicketTypeGiftFloor getGiftFloor(String ticketTypeId) {
        return giftFloor(requireTicketTypeRecord(ticketTypeId).id());
    }

    /**
     * 215-R2 按票种配最低赠酒杯数（ENUM:34「业务参数，按票种可配，出厂默认 1 杯，须同时配置上下限」）：杯数与下限、上限须同时填写，
     * 且 1 ≤ 下限 ≤ 杯数 ≤ 上限。已上架通票改高后跌破即退回草稿（同 V369 改组成退回草稿），补齐赠酒后重新发布。
     */
    TicketTypeGiftFloor configureGiftFloor(String ticketTypeId, TicketTypeGiftFloorRequest request, String actorId) {
        TicketTypeGiftFloorRequest actual = requireRequest(request);
        // 第十八批 AJ 段 236-R3：待审期间不许再改（须先撤回提交）。
        requireNoPendingRuleVersion(requireText(ticketTypeId, "ticket_type_id"));
        TicketTypeRecord ticketType = requireTicketTypeRecord(ticketTypeId);
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE", "已存档或待删除的票种不可改最低赠酒杯数");
        }
        if (actual.minGiftCups() == null || actual.minGiftCupsLower() == null || actual.minGiftCupsUpper() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "最低赠酒杯数须与其下限、上限同时填写");
        }
        int cups = actual.minGiftCups();
        int lower = actual.minGiftCupsLower();
        int upper = actual.minGiftCupsUpper();
        if (lower < 1 || lower > cups || cups > upper) {
            throw ApiException.badRequest("VALIDATION_FAILED", "最低赠酒杯数须满足：1 ≤ 下限 ≤ 杯数 ≤ 上限");
        }
        Instant now = Instant.now();
        TicketTypeGiftFloor floor = new TicketTypeGiftFloor(ticketType.id(), cups, lower, upper, actorId, now);
        java.util.List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketType.id());
        // 第十八批 AJ 段 236-R7（第十八批裁决 61；ENUM:34 最低赠酒杯数「一律记为域主管公司出资」）：已上架跨域组合通票改最低杯数只存进草稿版，
        //   现表与售卖不变；草稿赠酒够不够数在提交审批时由发布闸点名。
        if (toDraftVersion(ticketType, bindings)) {
            Draft draft = openDraft(ticketType, bindings);
            ruleVersions.saveOpen(ticketType.id(), snapshot(draft.type(), draft.bindings(), floor));
            return floor;
        }
        repository.saveGiftFloor(floor);
        if ("published".equals(ticketType.status()) && barGiftCoverageViolation(ticketType) != null) {
            repository.saveTicketType(ticketType.withStatus("draft", now));
        }
        return floor;
    }

    /**
     * S5 票面按酒吧分组的赠酒清单（只读投影）：售票时要写明「各酒吧分别赠几杯」（BSM:1032）；杯数＝份数×次数×每份人数。
     * 与 215-R1 通票赠酒下限发布闸取同一份数据，票面写的和发布时校的必定是同一件事。
     */
    java.util.List<TicketTypeGiftCoverageRow> listGiftCoverage(String ticketTypeId) {
        requireTicketTypeRecord(ticketTypeId);
        java.util.List<TicketTypeProviderEntitlementRow> rows =
                repository.listTicketTypeProviderEntitlements(ticketTypeId);
        java.util.Map<String, Integer> totalByBar = new java.util.LinkedHashMap<>();
        for (TicketTypeProviderEntitlementRow row : rows) {
            if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(row.entitlementType()) && row.providerMerchantId() != null) {
                totalByBar.merge(row.providerMerchantId(), row.units(), Integer::sum);
            }
        }
        return rows.stream()
                .filter(row -> GIFT_DRINK_ENTITLEMENT_TYPE.equals(row.entitlementType()))
                .filter(row -> row.providerMerchantId() != null)
                .map(row -> new TicketTypeGiftCoverageRow(
                        row.providerMerchantId(),
                        row.providerMerchantName() == null ? row.providerMerchantId() : row.providerMerchantName(),
                        row.entitlementTemplateId(),
                        row.templateName(),
                        row.quantity(),
                        row.numPeople(),
                        row.units(),
                        totalByBar.getOrDefault(row.providerMerchantId(), 0)))
                .toList();
    }

    /**
     * S11③ 票面原价必须配全才能发布（原 S6 只卡「横跨多种权益类型」的票，现收紧到<b>每一条权益绑定</b>）。
     *
     * <p><b>为什么必须卡在发布这一步</b>：这张票卖出去以后，客人付的这笔钱要按各权益的<b>票面原价占比</b>
     * 拆给游乐公司、餐厅、零售商户和酒吧。少配一条价，这一条就分不到钱、别人凭空多分——
     * 而且票已经卖了、钱已经收了，事后只能一张张翻单子人工调账。所以票没配全价就不许开卖。
     *
     * <p><b>免费的怎么办</b>：免费权益把票面原价<b>填 0</b>（0 = 免费 = 不参与分钱），
     * 不是留空。留空的意思是「还没想好这份值多少钱」，那就还不能卖。
     */
    /**
     * V369 综合／联合通票<b>售价自动算</b> + <b>凑整误差闸</b>（业务 HTML:741 逐字：
     * 「售价由各业态价格相加后统一打折得出……由系统自动算出售价。运营可在此基础上填一个近似售价用于凑整……
     *   但与系统算出的售价之间的差额不得超过后台配置的允许误差，超出即拒绝发布」）。
     *
     * <p><b>为什么必须有这道闸（业务人员角度）</b>：以前运营在票种上填一个售价，又在每条权益上分别填票面原价，
     * 两处各填各的、系统从不比对。想填 197 手滑填成 179，票照样上架照样卖——少收的那 18 块在月度结算里
     * 被当成「套票折扣」，默认由项目公司吃掉，没有任何人会收到提醒。等财务发现毛利不对，票已经卖出去几百张，
     * 只能一张张翻单子人工调账。现在系统自己把售价算出来，运营填的数一旦离得太远，票根本上不了架。
     *
     * <p><b>为什么只比平日价</b>：算出售价只有一个数（各业态票面原价是单值的），而周末价/节假日价是在同一张票上
     * 叠的<b>日历加价</b>，本来就该比平日价高。若三档都拿这一个数去卡，运营为了让周末价过闸只能把允许误差调到
     * 几十元——那道闸就等于没有了，连 197→179 都能溜过去。所以闸卡在平日价（=这张票的基准售价），
     * 周末/节假日加价另按日历规则走（真要差很多就是 HTML:721 说的另一个「周末票」票种）。
     *
     * @param bindings 参与计价的权益绑定（发布时=库里现有的；加绑时=现有的 + 这条新的）
     */
    private void requirePriceWithinTolerance(TicketTypeRecord ticketType,
            java.util.List<TicketTypeEntitlementRecord> bindings) {
        if (comboPriceToleranceConfigPort == null) {
            return; // 未注入（旧构造/单测）：不校验。生产由 Spring 恒注入，端口自身缺配置时 fail-loud。
        }
        TicketTypePriceCheck check = priceCheck(ticketType, bindings,
                comboPriceToleranceConfigPort.findRoundingToleranceCent(currentProjectId()));
        if (check.withinTolerance()) {
            return;
        }
        if (check.filledPriceCent() == null) {
            throw ApiException.badRequest("TICKET_PRICE_REQUIRED",
                    "这张票还没填平日售价，不能开卖。系统按各业态价格算出的售价是 "
                            + yuan(check.computedPriceCent()) + " 元");
        }
        long over = Math.abs(check.diffCent()) - check.toleranceCent();
        throw ApiException.badRequest("TICKET_PRICE_OUT_OF_TOLERANCE",
                "填的售价和系统算出的售价差太多，票不能开卖：系统算出 " + yuan(check.computedPriceCent())
                        + " 元（各业态价格合计 " + yuan(check.componentTotalCent()) + " 元 × "
                        + check.comboDiscountRate().stripTrailingZeros().toPlainString() + "% 折扣），"
                        + "填的是 " + yuan(check.filledPriceCent()) + " 元，"
                        + (check.diffCent() > 0 ? "上调 " : "下调 ") + yuan(Math.abs(check.diffCent())) + " 元，"
                        + "超出允许误差 " + yuan(check.toleranceCent()) + " 元 达 " + yuan(over) + " 元。"
                        + "凑整可填 " + yuan(check.roundingSuggested()) + " 元");
    }

    /** 同上，但只回答「在不在误差内」、不抛错（供加绑权益时判断要不要把已上架的票退回草稿）。 */
    private boolean priceWithinTolerance(TicketTypeRecord ticketType,
            java.util.List<TicketTypeEntitlementRecord> bindings) {
        if (comboPriceToleranceConfigPort == null) {
            return true;
        }
        return priceCheck(ticketType, bindings,
                comboPriceToleranceConfigPort.findRoundingToleranceCent(currentProjectId())).withinTolerance();
    }

    /**
     * V369 定价核算：算出售价、与填的价差多少、超没超。前端实时提示与发布闸用的是同一份计算。
     *
     * @param toleranceCent 后台可配的允许误差（分）
     */
    TicketTypePriceCheck priceCheck(TicketTypeRecord ticketType,
            java.util.List<TicketTypeEntitlementRecord> bindings, long toleranceCent) {
        // 各业态票面原价相加：每条绑定按【份数】计（一条 3 杯赠酒的绑定，票面上就是 3 份的价）。
        // 免费权益票面原价为 0 → 自然不计入价值（HTML:741「免费的（价格为 0）不计入价值」）；
        // NULL=没配价，发布前已被 requireComponentPrices 拦下，此处按 0 读、绝不回退均摊。
        long componentTotal = 0L;
        for (TicketTypeEntitlementRecord binding : bindings) {
            long unit = binding.componentPriceCent() == null ? 0L : binding.componentPriceCent();
            componentTotal += unit * Math.max(1, binding.quantity());
        }
        BigDecimal rate = ticketType.comboDiscountRate() == null
                ? TicketTypeRecord.NO_COMBO_DISCOUNT : ticketType.comboDiscountRate();
        // 折扣按百分数：算出售价 = 合计 × 折扣 ÷ 100，四舍五入到分（全场金额口径 MoneyRounding）。
        long computed = com.complex.platform.common.money.MoneyRounding.toCent(
                BigDecimal.valueOf(componentTotal).multiply(rate).divide(MAX_DISCOUNT_RATE));
        Long filled = ticketType.weekdayPriceCent();
        Long diff = filled == null ? null : filled - computed;
        boolean within = diff != null && Math.abs(diff) <= toleranceCent;
        return new TicketTypePriceCheck(ticketType.id(), componentTotal, rate, computed,
                filled, diff, toleranceCent, within, roundToNiceEnding(computed));
    }

    /**
     * 凑整建议：离算出售价最近的「整元、且元位尾数是 8/9/0」的价（HTML:741 括号里那句）。
     * 只是给运营的建议数，不强制——真正拒绝发布的只有「超出允许误差」这一条。
     */
    static long roundToNiceEnding(long computedCent) {
        long bestCent = 0L;
        long bestGap = Long.MAX_VALUE;
        long yuan = computedCent / 100;
        for (long candidate = Math.max(0, yuan - 10); candidate <= yuan + 10; candidate++) {
            long lastDigit = candidate % 10;
            if (lastDigit != 8 && lastDigit != 9 && lastDigit != 0) {
                continue;
            }
            long gap = Math.abs(candidate * 100 - computedCent);
            if (gap < bestGap) {
                bestGap = gap;
                bestCent = candidate * 100;
            }
        }
        return bestCent;
    }

    /** 报错文案用：分 → 元（两位小数）。 */
    private static String yuan(long cent) {
        return BigDecimal.valueOf(cent).movePointLeft(2).toPlainString();
    }

    /** 当前登录人的项目公司作用域（未注入授权服务的单测取 null=全局默认作用域）。 */
    private String currentProjectId() {
        return authorizationService == null
                ? null : authorizationService.currentProjectScopeQuietly().orElse(null);
    }

    private void requireComponentPrices(java.util.List<TicketTypeEntitlementRecord> bindings) {
        java.util.List<String> missing = bindings.stream()
                .filter(binding -> binding.componentPriceCent() == null)
                .map(binding -> repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                        .map(EntitlementTemplateRecord::name)
                        .orElse(binding.entitlementTemplateId()))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("TICKET_COMPONENT_PRICE_REQUIRED",
                    "这些权益还没填各业态价格，票不能开卖（免费的请填 0）：" + String.join("、", missing));
        }
    }

    /**
     * S6/V376 发布闸兜底（HTML:737「每个权益必须在票种配置里标明成本承担方」，2026-08-01 拍板：漏标拒发布）。
     * 绑定时已拦，但绑完还能改模板（补专属商户等）让一条旧绑定「变成」赠送类——发布前逐行再查一遍，
     * 漏标的点名报出来让运营当场补，而不是开卖后平台默默替商户垫成本。
     */
    private void requireCostBearers(java.util.List<TicketTypeEntitlementRecord> bindings) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (TicketTypeEntitlementRecord binding : bindings) {
            if (binding.costBearer() != null) {
                continue;
            }
            EntitlementTemplateRecord template =
                    repository.findEntitlementTemplateById(binding.entitlementTemplateId()).orElse(null);
            if (template == null || !isGiftClass(binding.componentPriceCent(), template)) {
                continue; // 非赠送类无成本承担概念，别误伤。
            }
            missing.add(template.name());
        }
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("TICKET_COST_BEARER_REQUIRED",
                    "这些赠送权益还没标成本承担方（商户让利型/平台出资型），票不能发布："
                            + missing.stream().sorted().distinct().collect(java.util.stream.Collectors.joining("、")));
        }
    }

    /**
     * 第十八批 AI 段 236-R2（第十八批裁决 52 ①；ENUM:54「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。」；
     * BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：核销时要按内部结算价结算的权益（判据同
     * {@link TicketVerificationService} 核销取价：提供方兑付类，专属直付且商户让利型除外），其提供方在内部结算价目录里须已有获批生效价
     * （目录草案须他人复核转正）。提供方取法同 {@link InternalSettlementPriceResolver#byItemGiftCostCent}：模板商户，空则专属商户。
     */
    private void requireApprovedInternalPrices(java.util.List<TicketTypeEntitlementRecord> bindings, String action) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template =
                    repository.findEntitlementTemplateById(binding.entitlementTemplateId()).orElse(null);
            if (template == null || !InternalSettlementPriceResolver.providerRedeemed(
                    template.entitlementType(), template.entitlementKind(), template.exclusiveMerchantId())) {
                continue;
            }
            if (template.exclusiveMerchantId() != null
                    && TicketTypeEntitlementRecord.COST_BEARER_MERCHANT_CONCESSION.equals(binding.costBearer())) {
                continue; // 商户让利的专属直付权益不结算（BSM:751），核销不取内部结算价。
            }
            String provider = template.merchantId() != null ? template.merchantId() : template.exclusiveMerchantId();
            if (priceResolver.resolve(template.barItemRef(), provider, template.catalogItemCode()).isEmpty()) {
                missing.add("「" + template.name() + "」");
            }
        }
        if (!missing.isEmpty()) {
            throw ApiException.conflict("TICKET_ENTITLEMENT_PRICE_NOT_APPROVED",
                    "权益" + missing.stream().sorted().distinct().collect(java.util.stream.Collectors.joining("、"))
                            + "的提供方内部结算价还没有获批生效（内部结算价目录草案须他人复核转正），" + action);
        }
    }

    // ================= 第十八批 AJ 段 236-R3 跨域组合通票票种规则版本（第十八批裁决 52 ②、裁决 61） =================

    /** 版本内容快照：三档价、折扣、最低赠酒杯数（三者同空＝没配过）、权益组成、合计与算出售价、凑整差额与各域分摊。 */
    record RuleSnapshot(Long weekdayPriceCent, Long weekendPriceCent, Long holidayPriceCent, BigDecimal comboDiscountRate,
            Integer minGiftCups, Integer minGiftCupsLower, Integer minGiftCupsUpper,
            java.util.List<TicketTypeRuleVersionView.Entitlement> entitlements, long componentTotalCent, long computedPriceCent,
            Long roundingDiffCent, java.util.List<TicketTypeRuleVersionView.DomainAllocation> domainAllocations) {
    }

    /**
     * 业态 → {主管公司键, 经营域名}（ENUM:49「酒吧域 → 娱乐平台公司；游乐域 → 游乐公司；餐饮域与零售域 → 城市项目公司；演出传媒域 → 传媒公司」；
     * ENUM:195；同 TicketDistributionSettlementService 口径）。「其他」业态按商户档案主管公司（空则按商户）自成一键。
     */
    private static final Map<String, String[]> FORMAT_DOMAINS = Map.of(
            "bar", new String[] {"entertainment_platform_company", "酒吧域（娱乐平台公司）"},
            "amusement", new String[] {"amusement_company", "游乐域（游乐公司）"},
            "media", new String[] {"media_company", "演出传媒域（传媒公司）"},
            "catering", new String[] {"city_project_company", "餐饮域（城市项目公司）"},
            "retail", new String[] {"city_project_company", "零售域（城市项目公司）"});

    /**
     * 跨域组合通票：通票（未挂酒吧）里有价权益（各业态价格大于 0）的提供方分属两个及以上域主管公司。零元免费权益不参与域间分配（BSM:749），
     * 提供方业态为空的绑定不计入（只出现在走接口 default 的测试替身里）。
     */
    boolean crossDomain(TicketTypeRecord ticketType, java.util.List<TicketTypeEntitlementRecord> bindings) {
        if (trimToNull(ticketType.barId()) != null) {
            return false;
        }
        Map<String, TicketingRepository.TemplateProvider> providers = providers(bindings);
        return bindings.stream()
                .filter(binding -> binding.componentPriceCent() != null && binding.componentPriceCent() > 0L)
                .map(binding -> providers.get(binding.entitlementTemplateId()))
                .filter(provider -> provider.merchantType() != null)
                .map(provider -> FORMAT_DOMAINS.containsKey(provider.merchantType())
                        ? FORMAT_DOMAINS.get(provider.merchantType())[0] : otherDomain(provider))
                .distinct()
                .count() >= 2;
    }

    /**
     * 版本快照（BSM:754「凑整产生的差额按各业态价格比例，与折扣一并分摊到各域……每次发布时把凑整差额金额及各域分摊数额记入票种规则版本」）：
     * 凑整差额＝填的平日价－算出售价；各域分摊与出票拆分同一算法（{@link TicketOrderService#allocateRevenue} 最大余数补尾），分别按填的平日价与
     * 算出售价拆到每份权益再按域归堆：折扣分摊＝满额－按算出价所得，凑整分摊＝按填价所得－按算出价所得。只按平日价算（同凑整误差闸）；
     * 只列业态价格合计大于 0 的域；平日价未填则不分摊。
     */
    RuleSnapshot snapshot(TicketTypeRecord ticketType, java.util.List<TicketTypeEntitlementRecord> bindings, TicketTypeGiftFloor floor) {
        TicketTypePriceCheck check = priceCheck(ticketType, bindings, 0L);
        Map<String, TicketingRepository.TemplateProvider> providers = providers(bindings);
        Long filled = ticketType.weekdayPriceCent();
        boolean allocate = filled != null && check.componentTotalCent() > 0L;
        long[] byFilled = allocate ? TicketOrderService.allocateRevenue(bindings, filled) : null;
        long[] byComputed = allocate ? TicketOrderService.allocateRevenue(bindings, check.computedPriceCent()) : null;
        java.util.List<TicketTypeRuleVersionView.Entitlement> entitlements = new java.util.ArrayList<>();
        Map<String, long[]> sums = new java.util.LinkedHashMap<>();
        Map<String, String> names = new java.util.HashMap<>();
        int unit = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            TicketingRepository.TemplateProvider provider = providers.get(binding.entitlementTemplateId());
            String format = provider.merchantType();
            String domain = format == null ? null : FORMAT_DOMAINS.containsKey(format) ? format : otherDomain(provider);
            entitlements.add(new TicketTypeRuleVersionView.Entitlement(binding.id(), binding.entitlementTemplateId(),
                    provider.templateName(), provider.entitlementType(), provider.providerMerchantId(), provider.providerMerchantName(),
                    format, domain, binding.quantity(), binding.totalTimes(), binding.numPeople(), binding.valueAmountCent(),
                    binding.effectiveRule(), binding.componentPriceCent(), binding.costBearer(), binding.createdAt()));
            long full = binding.componentPriceCent() == null ? 0L : binding.componentPriceCent();
            for (int i = 0; i < binding.quantity(); i++, unit++) {
                if (!allocate || domain == null || full == 0L) {
                    continue;
                }
                long[] sum = sums.computeIfAbsent(domain, key -> new long[4]);
                names.putIfAbsent(domain, FORMAT_DOMAINS.containsKey(format) ? FORMAT_DOMAINS.get(format)[1]
                        : "其他业态（" + (provider.supervisingCompanyName() != null ? provider.supervisingCompanyName()
                                : provider.providerMerchantName()) + "）");
                sum[0] += full;
                sum[1] += full - byComputed[unit];
                sum[2] += byFilled[unit] - byComputed[unit];
                sum[3] += byFilled[unit];
            }
        }
        java.util.List<TicketTypeRuleVersionView.DomainAllocation> allocations = sums.entrySet().stream()
                .map(entry -> new TicketTypeRuleVersionView.DomainAllocation(entry.getKey(), names.get(entry.getKey()),
                        entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3]))
                .toList();
        return new RuleSnapshot(ticketType.weekdayPriceCent(), ticketType.weekendPriceCent(), ticketType.holidayPriceCent(),
                check.comboDiscountRate(), floor == null ? null : floor.minGiftCups(), floor == null ? null : floor.minGiftCupsLower(),
                floor == null ? null : floor.minGiftCupsUpper(), entitlements, check.componentTotalCent(), check.computedPriceCent(),
                filled == null ? null : filled - check.computedPriceCent(), allocations);
    }

    /** 票种规则版本，新版在前；票种已上架时其未结版本另附定价核算（草稿价、草稿组成与现行允许误差）。 */
    java.util.List<TicketTypeRuleVersionView> listRuleVersions(String ticketTypeId) {
        TicketTypeRecord ticketType = requireTicketTypeRecord(ticketTypeId);
        if (ruleVersions == null) {
            return java.util.List.of();
        }
        java.util.List<TicketTypeRuleVersionView> versions = ruleVersions.list(ticketType.id());
        if (!"published".equals(ticketType.status())) {
            return versions;
        }
        long tolerance = comboPriceToleranceConfigPort == null
                ? 0L : comboPriceToleranceConfigPort.findRoundingToleranceCent(currentProjectId());
        return versions.stream().map(version -> {
            if (!version.open()) {
                return version;
            }
            Draft draft = draftOf(ticketType, version);
            return version.withPriceCheck(priceCheck(draft.type(), draft.bindings(), tolerance));
        }).toList();
    }

    /**
     * 留痕补记（改价、加权益、改最低赠酒杯数、发布四处）：票种已上架且有未结版本，或未结版本已待审时，记下这次落到哪一版、
     * 是存入草稿版（draft）还是已提交审批（pending_review）；否则空。
     */
    Map<String, Object> ruleVersionTrail(String ticketTypeId) {
        if (ruleVersions == null) {
            return Map.of();
        }
        TicketTypeRuleVersionView open = ruleVersions.findOpen(ticketTypeId).orElse(null);
        if (open == null) {
            return Map.of();
        }
        boolean published = repository.findTicketTypeById(ticketTypeId).map(type -> "published".equals(type.status())).orElse(false);
        return published || "pending_review".equals(open.status())
                ? Map.of("rule_version_id", open.id(), "rule_version_status", open.status()) : Map.of();
    }

    /** 锁票种行后看未结版本：已提交审批的，待审期间一律 409（须先撤回提交）。单测未注入版本服务即不查。 */
    private void requireNoPendingRuleVersion(String ticketTypeId) {
        if (ruleVersions == null) {
            return;
        }
        ruleVersions.lockTicketType(ticketTypeId);
        if (ruleVersions.findOpen(ticketTypeId).filter(open -> "pending_review".equals(open.status())).isPresent()) {
            throw TicketTypeRuleVersionService.pendingConflict();
        }
    }

    private Map<String, TicketingRepository.TemplateProvider> providers(java.util.List<TicketTypeEntitlementRecord> bindings) {
        Map<String, TicketingRepository.TemplateProvider> found = repository.findTemplateProviders(
                bindings.stream().map(TicketTypeEntitlementRecord::entitlementTemplateId).distinct().toList());
        Map<String, TicketingRepository.TemplateProvider> all = new java.util.HashMap<>(found);
        for (TicketTypeEntitlementRecord binding : bindings) {
            all.putIfAbsent(binding.entitlementTemplateId(), new TicketingRepository.TemplateProvider(
                    binding.entitlementTemplateId(), null, null, null, null, null, null, null, false));
        }
        return all;
    }

    /** 「其他」业态的域键：商户档案指定的主管公司（ENUM:195「选“其他”的商户，必须在商户档案里指定一家主管公司」），未指定则按商户自成一域。 */
    private static String otherDomain(TicketingRepository.TemplateProvider provider) {
        return "other:" + (provider.supervisingCompanyId() != null ? provider.supervisingCompanyId() : provider.providerMerchantId());
    }

    // ================= 第十八批 AJ 段 236-R7 已上架跨域组合通票的草稿版（第十八批裁决 61） =================

    /** 草稿版内容：草稿价（票种其余字段取现表）、草稿组成、草稿最低赠酒杯数（空＝没配过）。 */
    private record Draft(TicketTypeRecord type, java.util.List<TicketTypeEntitlementRecord> bindings, TicketTypeGiftFloor floor) {
    }

    /** 改价、加权益、改最低赠酒杯数的去向：已上架的跨域组合通票（按现表即当前生效版判）存进草稿版，其余照现行写现表。 */
    private boolean toDraftVersion(TicketTypeRecord ticketType, java.util.List<TicketTypeEntitlementRecord> current) {
        return ruleVersions != null && "published".equals(ticketType.status()) && crossDomain(ticketType, current);
    }

    /** 该票种唯一一份未结版本（草稿版）的内容；没有就由现表起一份。调用方已持票种锁且已排除待审。 */
    private Draft openDraft(TicketTypeRecord ticketType, java.util.List<TicketTypeEntitlementRecord> current) {
        return ruleVersions.findOpen(ticketType.id()).map(open -> draftOf(ticketType, open))
                .orElseGet(() -> new Draft(ticketType, current, repository.findGiftFloor(ticketType.id()).orElse(null)));
    }

    private static Draft draftOf(TicketTypeRecord ticketType, TicketTypeRuleVersionView version) {
        return new Draft(
                ticketType.withPricing(version.comboDiscountRate(), version.weekdayPriceCent(), version.weekendPriceCent(),
                        version.holidayPriceCent(), ticketType.updatedAt()),
                version.entitlements().stream().map(e -> new TicketTypeEntitlementRecord(e.bindingId(), ticketType.id(),
                        e.entitlementTemplateId(), e.quantity(), e.totalTimes(), e.valueAmountCent(), e.effectiveRule(), e.numPeople(),
                        e.componentPriceCent(), e.costBearer(), e.createdAt(), e.createdAt())).toList(),
                version.minGiftCups() == null ? null : new TicketTypeGiftFloor(ticketType.id(), version.minGiftCups(),
                        version.minGiftCupsLower(), version.minGiftCupsUpper(), version.updatedBy(), version.updatedAt()));
    }

    /** 草稿组成的提供方行（同现表 listTicketTypeProviderEntitlements 口径：已存档模板不算票面内容）。 */
    private java.util.List<TicketTypeProviderEntitlementRow> providerRows(java.util.List<TicketTypeEntitlementRecord> bindings) {
        Map<String, TicketingRepository.TemplateProvider> providers = providers(bindings);
        return bindings.stream()
                .filter(binding -> !providers.get(binding.entitlementTemplateId()).archived())
                .map(binding -> {
                    TicketingRepository.TemplateProvider provider = providers.get(binding.entitlementTemplateId());
                    return new TicketTypeProviderEntitlementRow(binding.entitlementTemplateId(), provider.templateName(),
                            provider.entitlementType(), provider.providerMerchantId(), provider.providerMerchantName(),
                            provider.merchantType(), binding.quantity(), binding.numPeople(), binding.totalTimes(), binding.costBearer(),
                            binding.componentPriceCent());
                })
                .toList();
    }

    /** 撤回票种规则草稿版或待审提交（待审的同时作废复核单），现表不变、旧版照卖；撤回后再改由现表另起新草稿。 */
    TicketTypeRuleVersionView withdrawRuleVersion(String ticketTypeId, TicketCatalogArchiveRequest request) {
        String reason = requireText(requireRequest(request).reason(), "reason");
        return ruleVersions.withdraw(requireTicketTypeRecord(ticketTypeId).id(), reason);
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    /**
     * AUD-076 建票校验酒吧归属：null=通票直接放行；非空必须指向存在、业态 bar 且未存档的商户，否则拒 BAR_MERCHANT_INVALID。
     * @return 规范化后的 bar_id（可空）。
     */
    private String validateBarBinding(String barId) {
        String trimmed = trimToNull(barId);
        if (trimmed == null) {
            return null;
        }
        if (barDirectory == null) {
            // 无商户目录：不静默放行，明确拒绝，避免单店票绑不到真实酒吧。
            throw ApiException.badRequest("BAR_MERCHANT_INVALID", "无法校验酒吧商户，无法绑定单店票");
        }
        MerchantRecord bar = barDirectory.findMerchant(trimmed).orElse(null);
        if (bar == null || !"bar".equals(bar.merchantType()) || bar.archivedAt() != null) {
            throw ApiException.badRequest("BAR_MERCHANT_INVALID", "单店票必须绑定业态为酒吧且未存档的商户");
        }
        return trimmed;
    }

    /** AUD-076 展示层实时解析酒吧名（ID 快照、名字不快照）；barId 空/查不到/无目录时返回 null。 */
    String resolveBarName(String barId) {
        String trimmed = trimToNull(barId);
        if (trimmed == null || barDirectory == null) {
            return null;
        }
        return barDirectory.findMerchant(trimmed).map(MerchantRecord::merchantName).orElse(null);
    }

    /** 审计断层①:权益模板必须绑商户,否则核销结算款进不了月度账单(成孤儿)。 */
    private String requireMerchant(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("PARAM_INVALID", "merchant_id is required");
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

    /** 分类可空（null=标准，直接落 null）；非空必须为合法枚举，否则 400（DB CHECK 兜底）。 */
    private String normalizeTicketCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!TICKET_CATEGORIES.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "ticket_category is invalid");
        }
        return trimmed;
    }

    /** AUD-004：有效期天数可空（=默认1天）；非空必须为正（日=1/周=7/月=30/年=365 等业务值均为正整数）。 */
    private Integer requireValidityDays(Integer value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "有效天数必须大于 0");
        }
        return value;
    }

    /** #172(2026-07-19 拍板)：票种级刷新秒数已下线（改为全场统一 ticket_qr_refresh_config），此列休眠；
     * 仍容错校验残留取值——可空（=不落值），非空须能被 5 整除且 5≤x≤60（与全场配置同一式）。 */
    private Integer requireRefreshIntervalSeconds(Integer value) {
        if (value == null) {
            return null;
        }
        if (value % 5 != 0 || value < 5 || value > 60) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "刷新间隔秒数必须为 5 到 60 之间且为 5 的倍数");
        }
        return value;
    }

    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        int resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return resolved;
    }

    private long nonNegativeOrDefault(Long value, long defaultValue, String fieldName) {
        long resolved = value == null ? defaultValue : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return resolved;
    }

    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return value;
    }

    private void validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
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
