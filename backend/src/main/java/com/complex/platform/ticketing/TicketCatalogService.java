package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypePricingRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    // 结算重构批4（V230）：票种资格/定价档分类；null/standard=标准（与 DB CHECK 一致）。
    private static final Set<String> TICKET_CATEGORIES = Set.of("standard", "new_person", "student");
    // 结算重构批3-1（V225）：签约价只对虚拟权益取用（实物走吧台商品内部结算价）。
    private static final String KIND_VIRTUAL = "virtual";

    private final TicketingRepository repository;
    // AUD-076：按 id 取酒吧商户（判业态/存档/取名）。可为 null=兼容既有单测（不校验酒吧、不解析名称）。
    private final BarMerchantDirectory barDirectory;

    // V369：凑整允许误差（后台可配，取自结算配置中心「通票分账 → 套票折扣归属」那一行）。
    // 字段注入免改构造点；生产由 Spring 恒注入 JdbcSettlementBillRepository，端口自身缺配置时 fail-loud。
    // 为 null（旧构造/未注入的单测）= 不做误差校验，与 barDirectory 同范式。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    ComboPriceToleranceConfigPort comboPriceToleranceConfigPort;

    // V369：解析当前登录人的项目公司作用域，用于按项目取允许误差。为 null 时取全局默认作用域。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.access.AuthorizationService authorizationService;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketCatalogService(TicketingRepository repository, BarMerchantDirectory barDirectory) {
        this.repository = repository;
        this.barDirectory = barDirectory;
    }

    // ponytail: 向后兼容构造——既有单测(InMemory 仓库、无商户目录)不建单店票，不校验酒吧归属。
    public TicketCatalogService(TicketingRepository repository) {
        this(repository, null);
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
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        if (current.archivedAt() != null || !"none".equals(current.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE", "已存档或待删除的票种不可改售价");
        }
        TicketTypeRecord updated = current.withPricing(
                requireDiscountRate(actual.comboDiscountRate()),
                nonNegativeOrNull(actual.weekdayPriceCent(), "weekday_price_cent"),
                nonNegativeOrNull(actual.weekendPriceCent(), "weekend_price_cent"),
                nonNegativeOrNull(actual.holidayPriceCent(), "holiday_price_cent"),
                Instant.now());
        requirePriceWithinTolerance(updated, repository.listTicketTypeEntitlements(current.id()));
        return repository.saveTicketType(updated);
    }

    /** V369 定价核算（前端建票/改票时实时提示用）：算出售价、差额、允许误差、超没超。 */
    TicketTypePriceCheck priceCheck(String ticketTypeId) {
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
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
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
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
                trimToNull(actual.exclusiveMerchantId()),
                // 批3-6(用户2026-07-04)：拍脸照开关由请求透传(可空=false)；游乐模板走 V229 种子回填默认开，新建按请求。
                Boolean.TRUE.equals(actual.requireFacePhoto())
        // 批10/E：挂目录引用(withCatalogRef 同步写快照;code 为空时等价于原构造,快照亦空)。
        ).withCatalogRef(catalogItemCode, snapshotPriceCent, now));
    }

    // 批10/E：原「改虚拟权益签约价」端点（PR#274 updateAgreementPrice）已下线——内部结算价唯一可编辑处收编为
    // SKU 目录（草案 → 他人复核转正 → 快照传播），模板侧不再保留任何改价入口。「历史不受影响」不变且更强：
    // 核销固化（ticket_verifications.settlement_price_cent）照旧，改价只对转正之后的核销/结算生效。

    // ================= V253 数据存档与审批后删除范式（试点） =================

    /** 存档/取消存档权益模板（软标记，可逆；列表默认过滤已存档）。 */
    EntitlementTemplateRecord archiveEntitlementTemplate(String id, boolean archive, String reason, String actorId) {
        EntitlementTemplateRecord tpl = requireTemplate(id);
        Instant now = Instant.now();
        if (archive) {
            // 存档必填理由(业务HTML 2026-07-07)。行已存在,archive_reason 定向写入,顺序无关。
            String r = requireText(reason, "reason");
            EntitlementTemplateRecord saved = repository.saveEntitlementTemplate(tpl.withArchive(now, actorId, now));
            repository.setEntitlementTemplateArchiveReason(id, r);
            return saved;
        }
        // 取消存档:清空 archived_at/by + archive_reason,并把 pending 删除申请回退 none(复核§5)。
        EntitlementTemplateRecord saved = repository.saveEntitlementTemplate(
                tpl.withArchive(null, null, now).withDelete("none", null, null, null, now));
        repository.setEntitlementTemplateArchiveReason(id, null);
        return saved;
    }

    /** 发起删除申请：须先存档（防误删在用模板），记 requested_by/at/reason，状态置 pending。 */
    EntitlementTemplateRecord requestDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteRequest request, String actorId) {
        EntitlementTemplateRecord tpl = requireTemplate(id);
        if (tpl.archivedAt() == null) {
            throw ApiException.badRequest("ENTITLEMENT_TEMPLATE_NOT_ARCHIVED",
                    "template must be archived before requesting deletion");
        }
        if ("pending".equals(tpl.deleteStatus())) {
            throw ApiException.conflict("ENTITLEMENT_TEMPLATE_DELETE_ALREADY_PENDING",
                    "a delete request is already pending for this template");
        }
        Instant now = Instant.now();
        String reason = requireText(request == null ? null : request.reason(), "reason");
        return repository.saveEntitlementTemplate(tpl.withDelete("pending", actorId, now, reason, now));
    }

    /**
     * 审批删除申请。approved 且无活引用 → 物理删除；rejected → 回 none。
     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review SUPERVISOR_SELF_REVIEW_FORBIDDEN 原则）。
     */
    EntitlementTemplateDeleteDecision approveDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteApprovalRequest request, String actorId) {
        EntitlementTemplateRecord tpl = requireTemplate(id);
        if (!"pending".equals(tpl.deleteStatus())) {
            throw ApiException.badRequest("ENTITLEMENT_TEMPLATE_DELETE_NOT_PENDING",
                    "no pending delete request for this template");
        }
        String decision = request == null ? null : (request.decision() == null ? null : request.decision().trim().toLowerCase(Locale.ROOT));
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权上下文/测试）时跳过该闸。
        if (actorId != null && actorId.equals(tpl.deleteRequestedBy())) {
            throw ApiException.forbidden("ENTITLEMENT_TEMPLATE_SELF_APPROVE_FORBIDDEN",
                    "the initiator of a delete request cannot approve it");
        }
        if ("rejected".equals(decision)) {
            Instant now = Instant.now();
            repository.saveEntitlementTemplate(tpl.withDelete("none", null, null, null, now));
            return new EntitlementTemplateDeleteDecision(id, "rejected", false);
        }
        // approved：先校验无活引用（票种绑定/已发放权益），有则拒绝硬删。
        long refs = repository.countTemplateReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("ENTITLEMENT_TEMPLATE_IN_USE",
                    "template is still referenced by " + refs + " ticket bindings or issued entitlements");
        }
        repository.deleteEntitlementTemplate(id);
        return new EntitlementTemplateDeleteDecision(id, "approved", true);
    }

    // ================= V260 存档/删除范式铺到票种/价格规则(镜像上方权益模板) =================

    TicketTypeRecord archiveTicketType(String id, boolean archive, String reason, String actorId) {
        TicketTypeRecord tt = requireTicketTypeRecord(id);
        Instant now = Instant.now();
        if (archive) {
            String r = requireText(reason, "reason");
            TicketTypeRecord saved = repository.saveTicketType(tt.withArchive(now, actorId, now));
            repository.setTicketTypeArchiveReason(id, r);
            return saved;
        }
        // 取消存档：清标记 + archive_reason 并把 pending 删除申请回退 none（同权益模板复核§5）。
        TicketTypeRecord saved = repository.saveTicketType(
                tt.withArchive(null, null, now).withDelete("none", null, null, null, now));
        repository.setTicketTypeArchiveReason(id, null);
        return saved;
    }

    TicketTypeRecord requestDeleteTicketType(String id, TicketCatalogDeleteRequest request, String actorId) {
        TicketTypeRecord tt = requireTicketTypeRecord(id);
        if (tt.archivedAt() == null) {
            throw ApiException.badRequest("TICKET_TYPE_NOT_ARCHIVED", "票种必须先存档才能申请删除");
        }
        if ("pending".equals(tt.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_DELETE_ALREADY_PENDING", "该票种已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = requireText(request == null ? null : request.reason(), "reason");
        return repository.saveTicketType(tt.withDelete("pending", actorId, now, reason, now));
    }

    TicketTypeDeleteDecision approveDeleteTicketType(String id, TicketCatalogDeleteApprovalRequest request, String actorId) {
        TicketTypeRecord tt = requireTicketTypeRecord(id);
        if (!"pending".equals(tt.deleteStatus())) {
            throw ApiException.badRequest("TICKET_TYPE_DELETE_NOT_PENDING", "该票种没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        if (actorId != null && actorId.equals(tt.deleteRequestedBy())) {
            throw ApiException.forbidden("TICKET_TYPE_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
        }
        if ("rejected".equals(decision)) {
            repository.saveTicketType(tt.withDelete("none", null, null, null, Instant.now()));
            return new TicketTypeDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countTicketTypeReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("TICKET_TYPE_IN_USE",
                    "票种仍被 " + refs + " 条业务记录引用（订单/权益/价格规则/结算等），不可删除");
        }
        repository.deleteTicketType(id);
        return new TicketTypeDeleteDecision(id, "approved", true);
    }

    TicketPriceRuleRecord archivePriceRule(String id, boolean archive, String reason, String actorId) {
        TicketPriceRuleRecord rule = requirePriceRuleRecord(id);
        Instant now = Instant.now();
        if (archive) {
            String r = requireText(reason, "reason");
            TicketPriceRuleRecord saved = repository.savePriceRule(rule.withArchive(now, actorId, now));
            repository.setPriceRuleArchiveReason(id, r);
            return saved;
        }
        TicketPriceRuleRecord saved = repository.savePriceRule(
                rule.withArchive(null, null, now).withDelete("none", null, null, null, now));
        repository.setPriceRuleArchiveReason(id, null);
        return saved;
    }

    TicketPriceRuleRecord requestDeletePriceRule(String id, TicketCatalogDeleteRequest request, String actorId) {
        TicketPriceRuleRecord rule = requirePriceRuleRecord(id);
        if (rule.archivedAt() == null) {
            throw ApiException.badRequest("TICKET_PRICE_RULE_NOT_ARCHIVED", "价格规则必须先存档才能申请删除");
        }
        if ("pending".equals(rule.deleteStatus())) {
            throw ApiException.conflict("TICKET_PRICE_RULE_DELETE_ALREADY_PENDING", "该价格规则已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = requireText(request == null ? null : request.reason(), "reason");
        return repository.savePriceRule(rule.withDelete("pending", actorId, now, reason, now));
    }

    TicketPriceRuleDeleteDecision approveDeletePriceRule(String id, TicketCatalogDeleteApprovalRequest request, String actorId) {
        TicketPriceRuleRecord rule = requirePriceRuleRecord(id);
        if (!"pending".equals(rule.deleteStatus())) {
            throw ApiException.badRequest("TICKET_PRICE_RULE_DELETE_NOT_PENDING", "该价格规则没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        if (actorId != null && actorId.equals(rule.deleteRequestedBy())) {
            throw ApiException.forbidden("TICKET_PRICE_RULE_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
        }
        if ("rejected".equals(decision)) {
            repository.savePriceRule(rule.withDelete("none", null, null, null, Instant.now()));
            return new TicketPriceRuleDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countPriceRuleReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("TICKET_PRICE_RULE_IN_USE",
                    "价格规则仍被 " + refs + " 条订单记录引用，不可删除");
        }
        repository.deletePriceRule(id);
        return new TicketPriceRuleDeleteDecision(id, "approved", true);
    }

    private TicketTypeRecord requireTicketTypeRecord(String id) {
        return repository.findTicketTypeById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
    }

    private TicketPriceRuleRecord requirePriceRuleRecord(String id) {
        return repository.findPriceRuleById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_PRICE_RULE_NOT_FOUND", "price rule not found"));
    }

    private EntitlementTemplateRecord requireTemplate(String id) {
        return repository.findEntitlementTemplateById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
    }

    TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        TicketTypeEntitlementRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        // 复核§3(V260)：票种侧同样拒绑——已存档/待删除票种不可再被绑定新权益。
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_BINDABLE",
                    "已存档或待删除的票种不可绑定权益");
        }
        EntitlementTemplateRecord template = repository.findEntitlementTemplateById(requireText(actual.entitlementTemplateId(), "entitlement_template_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
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
        // V369：已上架的票被改了组成（加权益／改票面原价）后，售价若已经对不上新的业态构成，就把票退回草稿。
        //   直接拒绝会把「先建票、再一次性配组合权益」这条正常流程堵死；继续挂着卖则是让客人按一个
        //   系统自己都算不出来的价买。只在真的对不上时才下架，改完价重新发布即可（重新走一遍误差闸）。
        if ("published".equals(ticketType.status())
                && !priceWithinTolerance(ticketType, withBinding(ticketType.id(), binding))) {
            repository.saveTicketType(ticketType.withStatus("draft", now));
        }
        return repository.saveTicketTypeEntitlement(binding);
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

    /** 该票种现有绑定 + 这条待写入的绑定（同 id 视为改这一条，不重复计）。 */
    private java.util.List<TicketTypeEntitlementRecord> withBinding(String ticketTypeId,
            TicketTypeEntitlementRecord binding) {
        java.util.List<TicketTypeEntitlementRecord> merged = new java.util.ArrayList<>();
        for (TicketTypeEntitlementRecord existing : repository.listTicketTypeEntitlements(ticketTypeId)) {
            if (!existing.id().equals(binding.id())) {
                merged.add(existing);
            }
        }
        merged.add(binding);
        return merged;
    }

    TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketTypeRecord current = repository.findTicketTypeById(requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        // AUD-015 发布闸：至少绑定一个权益模板才可发布——公开可售的票必须能出票，否则形成收钱不出票。
        if (repository.listTicketTypeEntitlements(current.id()).isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED",
                    "ticket type must have at least one entitlement binding before publish");
        }
        requireComponentPrices(current.id());
        requireCostBearers(current.id());
        requireBarGiftCoverage(current.id());
        // V369 凑整误差闸：填的售价与系统算出的售价差太多就不许开卖（HTML:741「超出即拒绝发布」）。
        requirePriceWithinTolerance(current, repository.listTicketTypeEntitlements(current.id()));
        // 发布只翻状态，其余（库存/已售/分类/可退/酒吧归属/折扣）一律原样带过，避免被置空/清零。
        return repository.saveTicketType(current.withStatus("published", Instant.now()));
    }

    /** S5 K17：入场类权益的类型（这些权益让客人能进这家吧的门）。 */
    private static final java.util.Set<String> ENTRY_ENTITLEMENT_TYPES = java.util.Set.of("entry", "admission");
    /** 酒吧业态（V348 商户业态五值封口）。 */
    private static final String MERCHANT_TYPE_BAR = "bar";
    /** 赠酒类权益。 */
    private static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";

    /**
     * S5 K17「每个酒吧至少赠一杯」发布校验（业务 HTML §通票赠酒按酒吧分配：
     * 「一张通票覆盖的每个酒吧至少赠一杯，各酒吧分别赠几杯在售票时写明票面」）。
     *
     * <p><b>为什么卡在发布这一步</b>：票一旦开卖，客人拿着它进 B 吧却发现没有赠酒，B 吧的店长要么自掏腰包
     * 请这杯、要么让客人白跑一趟——两种都得吵一架，而且事后没法补（票面已经印出去了）。所以在票还没卖
     * 之前就把漏配的吧点名报出来，让运营当场补上。
     *
     * <p><b>什么票才卡</b>：只卡「已经带了赠酒的票」。一张不含任何赠酒的普通门票本来就不该被要求请客，
     * 卡它只会让运营连普通票都发不出去。但只要这张票给某家吧配了赠酒，它覆盖的每家吧就都得有——
     * 有 A 没 B 恰恰是最常见也最伤人的漏配。
     *
     * <p>覆盖的酒吧集合按该票种<b>实际绑定的入场类权益</b>动态算，不按固定家数（可能七八家，也可能二三十家）。
     */
    private void requireBarGiftCoverage(String ticketTypeId) {
        java.util.List<TicketTypeProviderEntitlementRow> rows =
                repository.listTicketTypeProviderEntitlements(ticketTypeId);
        java.util.Map<String, Integer> giftUnitsByBar = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> barNames = new java.util.LinkedHashMap<>();
        java.util.Set<String> barsWithEntry = new java.util.LinkedHashSet<>();
        for (TicketTypeProviderEntitlementRow row : rows) {
            if (row.providerMerchantId() == null || !MERCHANT_TYPE_BAR.equals(row.providerMerchantType())) {
                continue;
            }
            barNames.putIfAbsent(row.providerMerchantId(),
                    row.providerMerchantName() == null ? row.providerMerchantId() : row.providerMerchantName());
            if (ENTRY_ENTITLEMENT_TYPES.contains(row.entitlementType())) {
                barsWithEntry.add(row.providerMerchantId());
            }
            if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(row.entitlementType())) {
                giftUnitsByBar.merge(row.providerMerchantId(), row.units(), Integer::sum);
            }
        }
        if (giftUnitsByBar.values().stream().noneMatch(units -> units >= 1)) {
            return; // 这张票压根不含赠酒（普通门票）→ 本条不适用。
        }
        java.util.List<String> missing = barsWithEntry.stream()
                .filter(barId -> giftUnitsByBar.getOrDefault(barId, 0) < 1)
                .map(barNames::get)
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.conflict("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT",
                    "这张票覆盖的每个酒吧都要至少赠一杯，还差：" + String.join("、", missing));
        }
    }

    /**
     * S5 票面按酒吧分组的赠酒清单（只读投影）：售票时要写明「各酒吧分别赠几杯」。
     * 与 K17 发布校验取同一份数据，票面写的和发布时校的必定是同一件事。
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
                    "这张票还没填平日售价，不能开卖。系统按各业态票面原价算出的售价是 "
                            + yuan(check.computedPriceCent()) + " 元");
        }
        long over = Math.abs(check.diffCent()) - check.toleranceCent();
        throw ApiException.badRequest("TICKET_PRICE_OUT_OF_TOLERANCE",
                "填的售价和系统算出的售价差太多，票不能开卖：系统算出 " + yuan(check.computedPriceCent())
                        + " 元（各业态票面原价合计 " + yuan(check.componentTotalCent()) + " 元 × "
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

    private void requireComponentPrices(String ticketTypeId) {
        java.util.List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        java.util.List<String> missing = bindings.stream()
                .filter(binding -> binding.componentPriceCent() == null)
                .map(binding -> repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                        .map(EntitlementTemplateRecord::name)
                        .orElse(binding.entitlementTemplateId()))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("TICKET_COMPONENT_PRICE_REQUIRED",
                    "这些权益还没填票面原价，票不能开卖（免费的请填 0）：" + String.join("、", missing));
        }
    }

    /**
     * S6/V376 发布闸兜底（HTML:737「每个权益必须在票种配置里标明成本承担方」，2026-08-01 拍板：漏标拒发布）。
     * 绑定时已拦，但绑完还能改模板（补专属商户等）让一条旧绑定「变成」赠送类——发布前逐行再查一遍，
     * 漏标的点名报出来让运营当场补，而不是开卖后平台默默替商户垫成本。
     */
    private void requireCostBearers(String ticketTypeId) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (TicketTypeEntitlementRecord binding : repository.listTicketTypeEntitlements(ticketTypeId)) {
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

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "validity_days must be positive");
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
                    "refresh_interval_seconds must be a multiple of 5 between 5 and 60");
        }
        return value;
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
