package com.complex.platform.serviceorder;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * 服务点单(G批六口径拍板;H批 2026-07-02 最终拍板将④修订为公司统一账户代收):
 * ① 后台目录固定价;② 服务员代客下单(客人App自点接口预留);③ 下单即指定履约人;
 * ④ 公司代收(推翻直付)——客人扫公司统一收款码付款,全额进公司账户,服务员现场确认收款;
 *   完成履约时分账:管理费归公司,履约人应得写提成台账,月结进薪资并按劳务报酬税表代扣个税;
 * ⑤ 履约前可取消(已收款后取消记退款备注,公司线下退),完成后不可退;
 * ⑥ 不进酒吧桌台账单,仅关联桌台记录来源。
 * TK-23:固定管理费越界保存即拒绝(不静默封顶);归属月按完成时点所在 bar 营业日;管理费变更比照销售提成——草案→差人复核转正→指定生效日形成新版本、下单按生效版本锁定。
 */
@Service
public class ServiceOrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServiceOrderService.class);

    private final JdbcServiceOrderRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.access.staff.JdbcStaffRepository staffRepository;
    private final com.complex.platform.common.idempotency.IdempotencyService idempotencyService;
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;
    private final com.complex.platform.notification.NotificationService notificationService;
    // 第十一批 TK-04-a：收款时识别会员、完成履约时生成商户消费待确认积分与成长值（字段注入免改构造点；单测未注入即不生成）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.customer.MerchantConsumptionRewardService merchantRewards;

    public ServiceOrderService(JdbcServiceOrderRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.access.staff.JdbcStaffRepository staffRepository,
            com.complex.platform.common.idempotency.IdempotencyService idempotencyService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.notification.NotificationService notificationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
        this.idempotencyService = idempotencyService;
        this.businessDayResolver = businessDayResolver;
        this.notificationService = notificationService;
    }

    /** openapi-p0 契约:失败路径落 x-audit-event-failure 事件后再抛(审计闭合,2026-07-03 P1)。 */
    private <T> T withFailureAudit(String failureEvent, String permissionCode, String businessType,
            String businessId, String idempotencyKey, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(failureEvent, permissionCode, businessType, businessId,
                    null, idempotencyKey,
                    exception instanceof ApiException apiException ? apiException.code()
                            : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    public ServiceCatalogItemRecord upsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        return withFailureAudit("service.catalog.save_failed", "service.catalog.manage",
                "service_catalog_item", request == null ? null : request.id(),
                request == null ? null : request.idempotencyKey(),
                () -> doUpsertCatalogItem(request));
    }

    /** ENUM L291 / BSM L1127:按比例 0%..100%(含边界)、固定金额 0..服务售价(含边界);任一越界值在保存时直接拒绝,不得静默截断、封顶或改写。 */
    private static void validateFee(String feeMode, long feeValue, long priceCent) {
        if (!java.util.Set.of("none", "percent", "fixed").contains(feeMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "管理费模式（mgmt_fee_mode）必须为 none/percent/fixed");
        }
        if (feeValue < 0 || (feeMode.equals("percent") && feeValue > 10000)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "管理费数值（mgmt_fee_value）不正确：百分比模式使用基点，不得超过 10000");
        }
        if (feeMode.equals("fixed") && feeValue > priceCent) {
            throw ApiException.badRequest("VALIDATION_FAILED", "固定管理费不得超过服务售价（含边界）");
        }
    }

    private ServiceCatalogItemRecord doUpsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        authorizationService.requirePermission("service.catalog.manage");
        if (request == null || request.name() == null || request.name().isBlank()
                || request.priceCent() == null || request.priceCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写名称与非负的价格（price_cent）");
        }
        var actor = authorizationService.currentActor();
        String id = request.id() == null || request.id().isBlank() ? "svc_" + UUID.randomUUID() : request.id().trim();
        String feeMode = request.mgmtFeeMode() == null ? "none" : request.mgmtFeeMode();
        long feeValue = request.mgmtFeeValue() == null ? 0 : request.mgmtFeeValue();
        validateFee(feeMode, feeValue, request.priceCent());
        // TK-23-R3(ENUM L291「变更版本化与指定生效日……比照销售提成」):已有目录项不再直改管理费,须走草案→差人复核→生效日版本;
        // 首次新建仍可直接写管理费(出厂值=V1)。
        repository.findCatalogItem(id).ifPresent(existing -> {
            if (!existing.mgmtFeeMode().equals(feeMode) || existing.mgmtFeeValue() != feeValue) {
                throw ApiException.badRequest("FEE_CHANGE_REQUIRES_DRAFT",
                        "已有目录项的管理费变更须提交草案并由他人复核转正（指定生效日），不能直接保存生效");
            }
        });
        ServiceCatalogItemRecord saved = repository.upsertCatalogItem(new ServiceCatalogItemRecord(
                id, request.name().trim(), request.priceCent(), request.durationMinutes(),
                request.active() == null || request.active(), feeMode, feeValue,
                actor == null ? null : actor.id(), Instant.now()));
        auditEventService.recordSuccess("service.catalog.saved", "service.catalog.manage",
                "service_catalog_item", saved.id(), null, request.idempotencyKey(),
                Map.of("name", saved.name(), "price_cent", saved.priceCent()));
        return saved;
    }

    /** TK-23-R3:管理费变更草案(拟改模式/数值 + 指定生效日 ≥ 当日 bar 营业日);区间校验同保存;起草人留痕,待他人复核。
     *  复核 authz M4(PGM L303):草案写入走 IdempotencyService——同号同体重放返回第一次结果,不得把已有复核人抹成 NULL。 */
    public ServiceCatalogItemRecord draftFeeChange(String id, com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest request) {
        return withFailureAudit("service.catalog.fee_draft_failed", "service.catalog.manage", "service_catalog_item", id,
                request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.catalog.manage");
            return idempotencyService.execute("draftServiceCatalogFee:" + id, request == null ? null : request.idempotencyKey(),
                    request, () -> doDraftFeeChange(id, request), ServiceCatalogItemRecord.class);
        });
    }

    private ServiceCatalogItemRecord doDraftFeeChange(String id, com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest request) {
        var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "服务目录项不存在或已被删除"));
        if (request == null || request.effectiveFrom() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定生效日（effective_from）");
        }
        String feeMode = request.mgmtFeeMode() == null ? "none" : request.mgmtFeeMode();
        long feeValue = request.mgmtFeeValue() == null ? 0 : request.mgmtFeeValue();
        validateFee(feeMode, feeValue, item.priceCent());
        Instant now = Instant.now();
        // 第十八批第 1 条：「当日营业日」按操作者当前项目（ENUM:187）。
        if (request.effectiveFrom().isBefore(businessDayResolver.businessDate(
                authorizationService.currentProjectScopeQuietly().orElse(null), null, now))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效日不得早于当日营业日");
        }
        var actor = authorizationService.currentActor();
        repository.saveFeeDraft(id, feeMode, feeValue, request.effectiveFrom(), actor == null ? null : actor.id(), now);
        auditEventService.recordSuccess("service.catalog.fee_drafted", "service.catalog.manage",
                "service_catalog_item", id, null, request.idempotencyKey(),
                Map.of("mgmt_fee_mode", feeMode, "mgmt_fee_value", feeValue, "effective_from", request.effectiveFrom().toString()));
        return repository.findCatalogItem(id).orElseThrow();
    }

    /** TK-23-R3:草案差人复核转正(比照 activateDraft 的 requireDifferentActor);只标记复核人,生效日起由下单路径翻为新版本;
     * 转正即向受影响方通告(ENUM L291「变更前向受影响方通告」)。 */
    public ServiceCatalogItemRecord activateFeeDraft(String id, String idempotencyKey) {
        return withFailureAudit("service.catalog.fee_activate_failed", "service.catalog.approve", "service_catalog_item", id,
                idempotencyKey, () -> {
            authorizationService.requirePermission("service.catalog.approve");
            var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "服务目录项不存在或已被删除"));
            if (item.nextRequestedBy() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "该项无待复核的管理费草案");
            }
            if (item.nextApprovedBy() != null) {
                return item;
            }
            var actor = authorizationService.currentActor();
            if (actor != null && actor.id() != null && actor.id().equals(item.nextRequestedBy())) {
                throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "管理费草案须由起草人以外的人复核转正");
            }
            repository.approveFeeDraft(id, actor == null ? null : actor.id());
            auditEventService.recordSuccess("service.catalog.fee_activated", "service.catalog.approve",
                    "service_catalog_item", id, null, idempotencyKey,
                    Map.of("mgmt_fee_mode", item.nextMgmtFeeMode(), "mgmt_fee_value", item.nextMgmtFeeValue(),
                            "effective_from", item.nextEffectiveFrom().toString(), "fee_version_next", item.feeVersion() + 1));
            notifyFeeChange(item);
            return repository.findCatalogItem(id).orElseThrow();
        });
    }

    /** 通告受影响方(复用 notification 现有发送口),留痕 fee_change_notified。操作员裁决 2026-10-20 第三轮第 8 条「可接该服务的履约人
     *  +所属酒吧」、不设 90 天窗口;收件人口径按第十批 blocked_questions 裁决(B10-BRIEF 第 10 条):①本项目在职、持
     *  service.order.perform 权限的员工;②该目录项服务单发生过的酒吧的主理人(该酒吧当前有效分红方案 principal_user_id)。
     *  均按员工档案手机号发,无手机号不发;不发商户档案联系电话。本项目＝转正操作者的项目范围(平台账号无项目范围时不按项目筛)。 */
    private void notifyFeeChange(ServiceCatalogItemRecord item) {
        java.util.Map<String, String> recipients = new java.util.LinkedHashMap<>();
        String project = authorizationService.currentProjectScopeQuietly().orElse(null);
        java.util.List<String> userIds = new java.util.ArrayList<>();
        for (var actor : repository.listActiveStaffActors(project)) {
            if (authorizationService.hasEffectivePermission(actor, "service.order.perform")) {
                userIds.add(actor.id());
            }
        }
        userIds.addAll(repository.listBarPrincipalUserIds(item.id()));
        for (String userId : userIds) {
            staffRepository.findProfileByUserId(userId)
                    .map(com.complex.platform.access.staff.StaffProfileRecord::phone)
                    .map(com.complex.platform.common.Strings::trimToNull)
                    .ifPresent(phone -> recipients.putIfAbsent(userId, phone));
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("item_name", item.name());
        params.put("mgmt_fee_mode", item.nextMgmtFeeMode());
        params.put("mgmt_fee_value", item.nextMgmtFeeValue());
        params.put("effective_from", String.valueOf(item.nextEffectiveFrom()));
        int sent = 0;
        for (var recipient : recipients.entrySet()) {
            String phone = recipient.getValue();
            try {
                notificationService.notify("service", "catalog_fee_changed", phone, "sms.service.catalog_fee_changed", params,
                        "svcfee_" + UUID.nameUUIDFromBytes((item.id() + "|" + (item.feeVersion() + 1) + "|" + recipient.getKey())
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", ""));
                sent++;
            } catch (RuntimeException exception) {
                log.warn("service catalog fee change notification failed: item_id={}", item.id(), exception);
            }
        }
        auditEventService.recordSuccess("service.catalog.fee_change_notified", "service.catalog.approve",
                "service_catalog_item", item.id(), null, null, Map.of("recipients", recipients.size(), "sent", sent));
    }

    public PageResponse<ServiceCatalogItemRecord> listCatalog() {
        authorizationService.requirePermission("service.order.create");
        return page(repository.listCatalog(true, false));
    }

    public PageResponse<ServiceCatalogItemRecord> listCatalogAll() {
        authorizationService.requirePermission("service.catalog.manage");
        return page(repository.listCatalog(false, true));
    }

    public ServiceCatalogItemRecord archiveCatalogItem(String id, boolean archive, String reason) {
        return withFailureAudit("service.catalog.archive_failed", "service.catalog.manage", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.manage");
            repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "服务目录项不存在或已被删除"));
            var actor = authorizationService.currentActor();
            String r = null;
            if (archive) {
                if (reason == null || reason.isBlank()) throw ApiException.badRequest("VALIDATION_FAILED", "存档理由不能为空");
                r = reason.trim();
            }
            repository.archiveCatalogItem(id, archive, r, actor == null ? null : actor.id());
            auditEventService.recordSuccess("service.catalog." + (archive ? "archived" : "unarchived"), "service.catalog.manage",
                    "service_catalog_item", id, null, null, Map.of());
            return repository.findCatalogItem(id).orElseThrow();
        });
    }

    public ServiceCatalogItemRecord requestDeleteCatalogItem(String id, String reason) {
        return withFailureAudit("service.catalog.delete_request_failed", "service.catalog.manage", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.manage");
            var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "服务目录项不存在或已被删除"));
            if (item.archivedAt() == null) throw ApiException.badRequest("VALIDATION_FAILED", "请先存档再申请删除");
            if (reason == null || reason.isBlank()) throw ApiException.badRequest("VALIDATION_FAILED", "删除理由不能为空");
            var actor = authorizationService.currentActor();
            repository.requestDeleteCatalogItem(id, reason.trim(), actor == null ? null : actor.id());
            auditEventService.recordSuccess("service.catalog.delete_requested", "service.catalog.manage", "service_catalog_item", id, null, null, Map.of());
            return repository.findCatalogItem(id).orElseThrow();
        });
    }

    public ServiceCatalogItemRecord approveDeleteCatalogItem(String id, String decision) {
        return withFailureAudit("service.catalog.delete_approve_failed", "service.catalog.approve", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.approve");
            var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "服务目录项不存在或已被删除"));
            if (!"pending".equals(item.deleteStatus())) throw ApiException.badRequest("VALIDATION_FAILED", "该项无待审批删除");
            var actor = authorizationService.currentActor();
            if (actor != null && actor.id() != null && actor.id().equals(item.deleteRequestedBy()))
                throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "不能审批自己发起的删除申请");
            boolean approved = "approved".equals(decision);
            repository.approveDeleteCatalogItem(id, approved);
            auditEventService.recordSuccess("service.catalog.delete_" + (approved ? "approved" : "rejected"), "service.catalog.approve", "service_catalog_item", id, null, null, Map.of());
            return item;
        });
    }

    /** 口径④:履约人录入自己的固定收款码(存二维码内容链接,App 端重绘 QR)。 */
    public void setMyPaymentCode(com.complex.platform.serviceorder.dto.StaffPaymentCodeRequest request) {
        authorizationService.requirePermission("service.order.perform");
        if (request == null || request.qrContent() == null || request.qrContent().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "qr_content is required");
        }
        var actor = authorizationService.currentActor();
        repository.upsertPaymentCode(actor.id(), request.qrContent().trim(), Instant.now());
        auditEventService.recordSuccess("service.payment_code.saved", "service.order.perform",
                "staff_payment_code", actor.id(), null, request.idempotencyKey(), Map.of());
    }

    public Map<String, String> myPaymentCode() {
        authorizationService.requirePermission("service.order.perform");
        var actor = authorizationService.currentActor();
        return Map.of("qr_content", repository.findPaymentCode(actor.id()).orElse(""));
    }

    /** 口径②③:代客下单+指定履约人;价格与名称取目录快照;履约人须已录收款码(否则客人没法付)。
     * P0 真幂等(2026-07-03):幂等键唯一占位+命中重放首次结果,重复提交不再重复建单。 */
    public ServiceOrderRecord createOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        return withFailureAudit("service.order.create_failed", "service.order.create",
                "service_order", null, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("createServiceOrder",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doCreateOrder(request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doCreateOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        if (request == null || request.catalogItemId() == null || request.catalogItemId().isBlank()
                || request.performerUserId() == null || request.performerUserId().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定服务目录项与服务人员");
        }
        // 清单 P1:项目员工只可为本综合体履约人下单(空 scope=平台放行)。守卫先于目录查找。
        requirePerformerInScope(request.performerUserId().trim());
        ServiceCatalogItemRecord item = repository.findCatalogItem(request.catalogItemId().trim())
                .filter(ServiceCatalogItemRecord::active)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ITEM_NOT_FOUND", "服务项不存在或已停用"));
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        // TK-23-R3(BSM L1070「费率变更形成新版本并指定生效日期,只对生效日之后发生的单据有效;每笔提成在下单时锁定当时的费率版本」):
        // 已复核草案到了生效营业日 → 先翻为现行版本(fee_version+1、清空草案)再快照;WHERE 条件保证并发下单只翻一次。
        // 第十八批第 1 条：生效营业日按本单酒吧（未选酒吧按操作者当前项目）。
        String orderBar = trimToNull(request.barId());
        LocalDate orderDay = businessDayResolver.businessDate(
                orderBar == null ? authorizationService.currentProjectScopeQuietly().orElse(null) : null, orderBar, now);
        if (item.nextApprovedBy() != null && item.nextEffectiveFrom() != null
                && !item.nextEffectiveFrom().isAfter(orderDay)) {
            repository.promoteApprovedFeeDraft(item.id(), orderDay);
            item = repository.findCatalogItem(item.id()).orElse(item);
        }
        // H批(2026-07-02 最终拍板):公司统一账户代收——下单即出示公司收款码,服务员现场确认收款。
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                "svo_" + UUID.randomUUID(), item.id(), item.name(), item.priceCent(),
                trimToNull(request.barId()), trimToNull(request.tableId()), request.performerUserId().trim(),
                actor == null ? null : actor.id(), trimToNull(request.customerNote()),
                "created", null, null, null, null, null, 0, 0, now, now, null,
                item.mgmtFeeMode(), item.mgmtFeeValue(), item.feeVersion()));
        auditEventService.recordSuccess("service.order.created", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("item", saved.itemName(), "performer", saved.performerUserId(),
                        "price_cent_company_collect", saved.priceCent(), "snap_fee_version", item.feeVersion()));
        return saved;
    }

    public PageResponse<ServiceOrderRecord> listAssignedToMe() {
        authorizationService.requirePermission("service.order.perform");
        var actor = authorizationService.currentActor();
        return page(repository.listOrdersForPerformer(actor.id()));
    }

    /** 兼容保留(舞者工作时无手机,新流程不经 accept——created→paid 后由服务员直接完成)。 */
    public ServiceOrderRecord accept(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        return withFailureAudit("service.order.accept_failed", "service.order.perform",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.perform");
            return idempotencyService.execute("acceptServiceOrder:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doAccept(id, request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doAccept(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        ServiceOrderRecord current = require(id);
        if (!current.status().equals("pending_accept")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "接单操作仅适用于旧流程的待接单订单");
        }
        return persistStatus(current, "accepted", null, "service.order.accepted",
                request == null ? null : request.idempotencyKey());
    }

    /** H批:服务员现场目视客人扫公司收款码支付成功 → 确认收款(钱进公司账户,财务可对账)。
     * P0 真幂等:资金确认动作,重复提交重放首次结果。 */
    public ServiceOrderRecord markPaid(String id, com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        return withFailureAudit("service.order.pay_confirm_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("markServiceOrderPaid:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doMarkPaid(id, request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doMarkPaid(String id, com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        ServiceOrderRecord current = require(id);
        requirePerformerInScope(current.performerUserId());
        if (!current.status().equals("created")) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "订单当前状态为 " + current.status() + "，只有新建状态可执行此操作");
        }
        // 第十一批 TK-04-R2（BSM L822、PGM L878）：收款时录入会员号（注册手机号）识别本单会员，走客人查询专门通道留痕；查无即拒。
        String memberPhone = request == null ? null : trimToNull(request.memberPhone());
        if (memberPhone != null) {
            merchantRewards.identifyByPhone(com.complex.platform.customer.MerchantConsumptionRewardService.SERVICE_ORDER,
                    current.id(), memberPhone, "service.order.create", request.idempotencyKey());
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "paid", current.cancelReason(),
                now, actor == null ? null : actor.id(),
                request == null || request.payMethod() == null ? "company_qr" : request.payMethod(),
                current.refundNote(), current.mgmtFeeCent(), current.dancerShareCent(),
                current.createdAt(), now, current.completedAt(),
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        // P1 收款对账:落一条公司应收/已收流水到复用的 ar_ap_items(带订单号+金额+时间;确认人=paid_by)。
        // TK-23-R2:应收行 due_date/accounting_period 按收款时点所在 bar 营业日(ENUM L187 钱账统一按营业日归集)。
        // ponytail: 价为 0 的单不生成应收行(ar_ap_items.amount_cent>0 CHECK);正价单幂等一单一条。
        if (saved.priceCent() > 0) {
            repository.insertReceivableForOrder(saved.id(), saved.priceCent(),
                    businessDate(saved, now), now);
        }
        auditEventService.recordSuccess("service.order.paid", "service.order.create",
                "service_order", saved.id(), null,
                request == null ? null : request.idempotencyKey(),
                Map.of("pay_method", saved.payMethod(), "price_cent", saved.priceCent()));
        return saved;
    }

    /** H批:完成履约(服务员现场操作;舞者无手机)。前置=已收款到公司账户;
     * 完成时分账:管理费(目录比例/固定)归公司,余额写提成台账归舞者(月结进薪资并代扣个税)。
     * P0 真幂等:分账+提成入台账为资金动作,重复提交重放首次结果,提成不重复入账。 */
    public ServiceOrderRecord complete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        return withFailureAudit("service.order.complete_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("completeServiceOrder:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doComplete(id, request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doComplete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        ServiceOrderRecord current = require(id);
        requirePerformerInScope(current.performerUserId());
        // P2-#4:仅 paid 可完成(移除 accepted 绕过路径;paid 蕴含 paid_at!=null,无收款不得完成)。
        if (!current.status().equals("paid")) {
            throw ApiException.badRequest("NOT_PAID_YET",
                    "订单须先完成支付（公司账户收款）才能完成，当前状态：" + current.status());
        }
        // P2-#3:分账费率取下单快照(非完成时活目录);存量无快照的历史单回退活目录。
        String feeMode = current.snapMgmtFeeMode();
        Long feeValue = current.snapMgmtFeeValue();
        if (feeMode == null || feeValue == null) {
            var live = repository.findCatalogItem(current.catalogItemId());
            feeMode = live.map(ServiceCatalogItemRecord::mgmtFeeMode).orElse("none");
            feeValue = live.map(ServiceCatalogItemRecord::mgmtFeeValue).orElse(0L);
        }
        final long feeValueFinal = feeValue;
        long mgmtFee = switch (feeMode) {
            // R-7:管理费按比例取额四舍五入到分(旧整除截断,平台恒少收);dancerShare 取补数 → 两侧和恒 = 价,无尾差。
            case "percent" -> MoneyRounding.bpsOf(current.priceCent(), feeValueFinal);
            // TK-23-R1(ENUM L291「不得静默截断、封顶或改写」):固定额直接取快照(保存时已校验 0..售价,不再封顶)。
            case "fixed" -> feeValueFinal;
            default -> 0L;
        };
        long dancerShare = current.priceCent() - mgmtFee;
        Instant now = Instant.now();
        // P1 并发守卫:paid→completed 原子转移作为唯一进入凭据。受影响行数!=1 → 已被其它并发/重放拿走 → 拒。
        // ponytail: 用 DB 行级 CAS 取代应用层 read-then-write,彻底消除多 key 并发多完成/多分账。
        if (repository.markCompletedIfPaid(current.id(), mgmtFee, dancerShare, now, now) != 1) {
            throw ApiException.badRequest("ALREADY_COMPLETED",
                    "服务订单已完成或已不可支付：状态已被并发变更");
        }
        // 赢得 CAS 后再入提成(部分唯一索引 + ON CONFLICT DO NOTHING,双保险幂等);再落审计。
        // TK-23-R2(ENUM L291 / BSM L1138):归属月按完成时点所在 bar 营业日,不按 UTC 自然月。
        String month = java.time.YearMonth.from(businessDate(current, now)).toString();
        staffRepository.insertCommission("cml_" + UUID.randomUUID(), current.performerUserId(), month,
                "service_order", current.id(), dancerShare, current.itemName(), now);
        // 第十一批 TK-04-R3（BSM L823「人员服务取既有实际完成履约记录及完成时点」）：已识别会员的服务单完成即生成待确认积分与成长值。
        if (merchantRewards != null) merchantRewards.tryCreateForServiceOrder(current.id());
        ServiceOrderRecord saved = new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "completed", current.cancelReason(),
                current.paidAt(), current.paidBy(), current.payMethod(), current.refundNote(),
                mgmtFee, dancerShare, current.createdAt(), now, now,
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue(), current.snapFeeVersion());
        auditEventService.recordSuccess("service.order.completed", "service.order.create",
                "service_order", saved.id(), null,
                request == null ? null : request.idempotencyKey(),
                Map.of("mgmt_fee_cent", mgmtFee, "dancer_share_cent", dancerShare, "settlement_month", month));
        return saved;
    }

    /** 服务单时点所属营业日：有酒吧按该酒吧，没有按服务单所属项目（service_orders.project_id）。
     * 份额/管理费归属月取其年月（ENUM L291 / BSM L1138「按服务完成时点所在营业日确定」;DEC-R6-10 禁止服务器默认时区/UTC）。 */
    LocalDate businessDate(ServiceOrderRecord order, Instant instant) {
        String bar = trimToNull(order.barId());
        return businessDayResolver.businessDate(bar == null ? repository.findOrderProjectId(order.id()) : null, bar, instant);
    }

    private ServiceOrderRecord require(String id) {
        return repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "服务订单不存在或已被删除"));
    }

    /** 清单 P1(2026-07-03 审计):按项目 scope 守卫——项目员工只可操作本综合体履约人的服务单;
     * 平台管理员(空 scope)放行。scope 由 token 派生(currentProjectScopeQuietly),不取请求体。 */
    private void requirePerformerInScope(String performerUserId) {
        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
            String performerProject = staffRepository.findUserProjectId(performerUserId).orElse(null);
            if (!scope.equals(performerProject)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                        "服务人员不在当前项目范围内，无权操作");
            }
        });
    }

    private ServiceOrderRecord persistStatus(ServiceOrderRecord current, String toStatus, Instant completedAt,
            String event, String idempotencyKey) {
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), toStatus, current.cancelReason(),
                current.paidAt(), current.paidBy(), current.payMethod(), current.refundNote(),
                current.mgmtFeeCent(), current.dancerShareCent(), current.createdAt(), now,
                completedAt == null ? current.completedAt() : completedAt,
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        auditEventService.recordSuccess(event, "service.order.perform", "service_order", saved.id(), null,
                idempotencyKey, Map.of("status", toStatus));
        return saved;
    }

    /** 口径⑤:履约前可取消(下单者或目录管理岗),留原因;完成后不可退。 */
    public ServiceOrderRecord cancel(String id, com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        return withFailureAudit("service.order.cancel_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(),
                () -> doCancel(id, request));
    }

    private ServiceOrderRecord doCancel(String id, com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        authorizationService.requirePermission("service.order.create");
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "取消订单必须填写理由");
        }
        ServiceOrderRecord current = repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "服务订单不存在或已被删除"));
        requirePerformerInScope(current.performerUserId());
        if (current.status().equals("completed")) {
            throw ApiException.badRequest("ALREADY_COMPLETED",
                    "服务单已完成履约并完成分账,不能取消;如需退款走财务退款流程");
        }
        if (current.status().equals("cancelled")) {
            return current;
        }
        Instant now = Instant.now();
        // H批:已收款(公司账户)后取消 → 记退款备注(公司线下退,财务对账留痕)。
        String refundNote = current.paidAt() == null ? current.refundNote()
                : "已收款待退:" + request.reason().trim();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "cancelled", request.reason().trim(),
                current.paidAt(), current.paidBy(), current.payMethod(), refundNote,
                current.mgmtFeeCent(), current.dancerShareCent(), current.createdAt(), now,
                current.completedAt(),
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        auditEventService.recordSuccess("service.order.cancelled", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("reason", request.reason().trim()));
        return saved;
    }

    /** H批:向客人出示**公司统一收款码**(代收;个人码流程已废弃)。 */
    public Map<String, String> orderPaymentCode(String id) {
        authorizationService.requirePermission("service.order.create");
        ServiceOrderRecord order = require(id);
        requirePerformerInScope(order.performerUserId());
        String qr = repository.companyPayQr();
        if (qr.isBlank()) {
            throw ApiException.badRequest("COMPANY_PAY_QR_NOT_CONFIGURED",
                    "公司收款码未配置，请先在服务支付配置中设置");
        }
        return Map.of("performer_user_id", order.performerUserId(), "qr_content", qr,
                "price_cent", String.valueOf(order.priceCent()));
    }

    /** H批:公司统一收款码配置(经营配置;财务对账基准)。 */
    public void setPayConfig(com.complex.platform.serviceorder.dto.ServicePayConfigRequest request) {
        withFailureAudit("service.pay_config.save_failed", "service.catalog.manage",
                "service_pay_config", "default", request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.catalog.manage");
            if (request == null || request.qrContent() == null || request.qrContent().isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "qr_content is required");
            }
            var actor = authorizationService.currentActor();
            repository.upsertPayConfig(request.qrContent().trim(), actor == null ? null : actor.id(), Instant.now());
            auditEventService.recordSuccess("service.pay_config.saved", "service.catalog.manage",
                    "service_pay_config", "default", null, request.idempotencyKey(), Map.of());
            return null;
        });
    }

    public Map<String, String> payConfig() {
        authorizationService.requirePermission("service.order.create");
        return Map.of("qr_content", repository.companyPayQr());
    }

    public PageResponse<ServiceOrderRecord> listOrders(java.time.LocalDate date, String status) {
        authorizationService.requirePermission("service.catalog.manage");
        // P0 租户隔离(V214):项目员工只见本综合体服务单;平台管理员全量。
        return page(repository.listOrders(date, status,
                authorizationService.currentProjectScopeQuietly().orElse(null)));
    }

    /** H批:履约人当月完成次数+分账应得(公司代收,月结进薪资提成并代扣个税)。TK-23-R2:与薪资月结同源(提成台账 month=营业日年月)。 */
    public Map<String, Object> myStats(String month) {
        authorizationService.requirePermission("service.order.perform");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
        }
        var actor = authorizationService.currentActor();
        long[] stats = repository.performerMonthlyStats(actor.id(), month);
        return Map.of("month", month, "completed_count", stats[0], "reference_income_cent", stats[1]);
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, 500, items.size());
    }
}
