// 文件版本：V0.04
package com.complex.platform.compliance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PiiTextRedactor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.compliance.qc.WecomQcPort;
import com.complex.platform.compliance.qc.WecomQcRequest;
import com.complex.platform.compliance.qc.WecomQcResult;
import com.complex.platform.compliance.qc.WecomQcRouter;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * R5 T8 企微会话合规链（PIPL 平台侧合规能力）。
 *
 * <ul>
 *   <li><b>同步入站</b>：受 T2 终端能力开关 {@code integration.wecom.sync} 编程式门控（默认关→拒绝、不落库）；
 *       开关关时不阻塞其它功能。落库时调 {@link PiiTextRedactor} 生成展示用脱敏文本，原文经 {@link PiiCipher}
 *       加密落 content_raw_ref（永久保存），仅审批后的原文调阅解密返回。
 *   <li><b>分级查看</b>：普通展示返回脱敏文本并写 redacted 审计；查看原文需 wecom.view_raw + 审批引用，
 *       返回原文并写 raw 审计 + 事件 wecom.raw_viewed / sensitive.viewed。
 *   <li><b>永久留存</b>：不自动到期处理；手动清理由最高权限本人逐次授权。
 *   <li><b>质检</b>：基础关键词敏感命中（私单/私收款/辱骂/诱导私微等）标记 risk_flags 进复核。
 *       AI 语义质检（V096）按"第三方一律预留接口 + 开关默认关"经 {@link WecomQcPort} 路由：
 *       开关 {@code wecom.ai_qc.enabled} 关（默认）→ 不调 AI、仅关键词质检（行为不变）；
 *       开 → 调 {@link WecomQcPort}（当前无真实厂商=占位 NOT_CONFIGURED，不崩、不阻塞），
 *       AI 结果叠加落 ai_qc_status/ai_qc_result，关键词 risk_flags 始终独立保留。
 * </ul>
 */
@Service
public class WecomConversationService {

    static final String SYNC_CAPABILITY = "integration.wecom.sync";
    private static final int MAX_REDACTED_CHARS = 4000;

    // P0#8 命中状态：open→reviewing→confirmed/dismissed；查询过滤全集 / 复核可达目标态。
    private static final Set<String> HIT_STATUSES = Set.of("open", "reviewing", "confirmed", "dismissed");
    private static final Set<String> REVIEW_TARGET_STATUSES = Set.of("reviewing", "confirmed", "dismissed");

    private static final int MAX_SNIPPET_CHARS = 200;

    // P0#8：企微违规规则码默认配置（兜底）。规则与关键词以 analytics_risk_rules（V106 seed）为准、后台可配；
    // 仅当库内无 compliance 域启用规则时（如纯单测/未迁移环境）回退到此内置默认，保证 detectRiskFlags 行为不退化。
    // 顺序即 risk_flags 输出顺序的稳定基线（私单/私收款/诱导私微/辱骂/违规承诺）。
    private static final List<WecomRiskRuleRecord> DEFAULT_RISK_RULES = List.of(
            new WecomRiskRuleRecord("rule_wecom_staff_private_deal", "STAFF_PRIVATE_DEAL", "high",
                    List.of("私单", "走私单", "线下交易", "私下交易")),
            new WecomRiskRuleRecord("rule_wecom_staff_private_payment", "STAFF_PRIVATE_PAYMENT", "critical",
                    List.of("私收款", "私人收款", "转我", "扫我码", "私户", "转私户")),
            new WecomRiskRuleRecord("rule_wecom_lure_private_wechat", "LURE_PRIVATE_WECHAT", "high",
                    List.of("加我微信", "私微", "私聊", "扫码加我", "加我私", "加个微信")),
            new WecomRiskRuleRecord("rule_wecom_abuse", "ABUSE", "high",
                    List.of("辱骂", "傻", "滚", "骂")),
            new WecomRiskRuleRecord("rule_wecom_illegal_promise", "ILLEGAL_PROMISE", "high",
                    List.of("保证退款", "稳赚", "100%", "包过", "绝对没问题", "违规承诺"))
    );

    // 规则码 → risk_flags 标记码映射（向后兼容既有 wecom_messages.risk_flags / 既有测试断言的标记名）。
    private static final Map<String, String> RULE_CODE_TO_FLAG = Map.of(
            "STAFF_PRIVATE_DEAL", "private_deal",
            "STAFF_PRIVATE_PAYMENT", "private_payment",
            "LURE_PRIVATE_WECHAT", "lure_private_contact",
            "ABUSE", "abuse",
            "ILLEGAL_PROMISE", "illegal_promise"
    );

    private final WecomComplianceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final TerminalSwitchGuard terminalSwitchGuard;
    private final WecomQcPort wecomQcPort;
    private final ObjectMapper objectMapper;
    // P1：evidence 统一异常责任链服务。企微违规命中除写孤岛 analytics_risk_alerts 外，
    // 再接入统一 risk_alerts（source_domain='wecom'），让 V103 死表真实承载企微命中数据。
    // 可为 null（向后兼容/单测无 SecurityContext 环境）；接入为 fail-safe，不阻塞同步主链。
    private final RiskAlertService riskAlertService;
    // M723-F14：调阅人的项目范围取自登录主体（平台管理员全量，D-032）。
    private final AuthorizationService authorizationService;
    // ROOT-05 R4：会话原文以 PII 密钥加密落库，审批后的原文调阅解密返回。
    private final PiiCipher piiCipher;
    private final Clock clock;

    @Autowired
    public WecomConversationService(
            WecomComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard,
            WecomQcPort wecomQcPort,
            ObjectMapper objectMapper,
            RiskAlertService riskAlertService,
            AuthorizationService authorizationService,
            PiiCipher piiCipher
    ) {
        this(repository, idempotencyService, auditEventService, terminalSwitchGuard,
                wecomQcPort, objectMapper, riskAlertService, authorizationService, piiCipher, Clock.systemUTC());
    }

    WecomConversationService(
            WecomComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard,
            WecomQcPort wecomQcPort,
            ObjectMapper objectMapper,
            RiskAlertService riskAlertService,
            AuthorizationService authorizationService,
            PiiCipher piiCipher,
            Clock clock
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.terminalSwitchGuard = terminalSwitchGuard;
        // AI 质检端口：默认 fail-safe 当作关（仅关键词质检）；可为 null（向后兼容/单测）。
        this.wecomQcPort = wecomQcPort == null ? WecomQcRouter.alwaysDisabled() : wecomQcPort;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper.copy().findAndRegisterModules();
        this.riskAlertService = riskAlertService;
        this.authorizationService = authorizationService;
        this.piiCipher = piiCipher;
        this.clock = clock;
    }

    // ----------------------------------------------------------------------------------------- 同步入站

    /** 企微会话同步入站：开关门控 + 脱敏落库 + 基础质检。 */
    public WecomSyncResult syncConversation(WecomConversationSyncRequest request) {
        try {
            // 编程式门控：开关关 → TerminalSwitchGuard.check 抛 TERMINAL_DISABLED，不落库（仍写失败留痕）。
            terminalSwitchGuard.check(SYNC_CAPABILITY);
            validateSyncRequest(request);
            return idempotencyService.execute(
                    "syncWecomConversation",
                    request.idempotencyKey(),
                    request,
                    () -> syncConversationOnce(request),
                    WecomSyncResult.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "wecom.conversation_sync_failed",
                    "wecom.sync",
                    "wecom_conversation",
                    request == null ? "unknown" : blankToUnknown(request.externalConversationId()),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private WecomSyncResult syncConversationOnce(WecomConversationSyncRequest request) {
        Instant now = clock.instant();
        String externalId = request.externalConversationId().trim();
        String projectId = request.projectId().trim();

        WecomConversationRecord conversation = repository.findConversationByExternalId(externalId)
                .orElseGet(() -> repository.saveConversation(new WecomConversationRecord(
                        "wcv_" + UUID.randomUUID(),
                        externalId,
                        projectId,
                        trimToNull(request.ownerStaffId()),
                        trimToNull(request.customerRef()),
                        "active",
                        null,
                        now,
                        now
                )));

        int stored = 0;
        int flagged = 0;
        int hits = 0;
        // P0#8：本次同步生效的企微违规规则码（读 analytics_risk_rules，domain=compliance/enabled；
        //   库内无规则时回退内置默认）。一次解析、整批复用。
        List<WecomRiskRuleRecord> activeRules = resolveActiveRules();
        List<WecomConversationSyncRequest.Message> messages =
                request.messages() == null ? List.of() : request.messages();
        for (WecomConversationSyncRequest.Message message : messages) {
            if (message == null) {
                continue;
            }
            String content = message.content() == null ? "" : message.content();
            // 普通展示脱敏（手机号/身份证/银行卡等正则）。
            String redacted = PiiTextRedactor.redactAndTruncate(content, MAX_REDACTED_CHARS).value();
            // P0#8 规则命中（按可配置规则码 + 关键词）。risk_flags 由命中规则派生（向后兼容既有标记）。
            List<RuleMatch> matches = matchRules(content, activeRules);
            List<String> riskFlags = riskFlagsFromMatches(matches);
            if (!riskFlags.isEmpty()) {
                flagged++;
            }
            // AI 语义质检（第三方预留接口 + 开关默认关）：经路由按 wecom.ai_qc.enabled 选实现——
            //   关→SKIPPED（不调 AI）；开→占位 NOT_CONFIGURED（不崩、不阻塞）。任何状态都不覆盖关键词结果。
            WecomQcResult aiQc = wecomQcPort.analyzeMessage(
                    new WecomQcRequest(conversation.id(), content, riskFlags));
            Instant sentAt = message.sentAt() == null ? now : message.sentAt();
            LocalDate businessDate = LocalDate.ofInstant(sentAt, ZoneOffset.UTC);
            String messageId = "wmsg_" + UUID.randomUUID();
            repository.saveMessage(new WecomMessageRecord(
                    messageId,
                    conversation.id(),
                    trimToNull(message.senderRef()),
                    // 原文加密落库、永久保存（BSM「已拍板：企业微信会话原文永久保存」条）；空内容不落密文。
                    content.isEmpty() ? null : piiCipher.encrypt(content),
                    redacted,
                    riskFlags,
                    false,
                    sentAt,
                    businessDate,
                    now,
                    aiQc.persistenceStatus(),
                    aiQcResultJson(aiQc)
            ));
            stored++;
            // P0#8 命中 → 写 wecom_risk_hits + 入复核队列（analytics_risk_alerts，source_domain=wecom）+ 事件。
            hits += persistRiskHits(conversation, messageId, matches, now);
        }

        auditEventService.recordSuccess(
                "wecom.conversation_synced",
                "wecom.sync",
                "wecom_conversation",
                conversation.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("messages_stored", stored, "flagged_for_review", flagged,
                        "risk_hits", hits, "retention", "permanent")
        );
        return new WecomSyncResult(conversation.id(), stored, flagged);
    }

    // ----------------------------------------------------------------------------------------- 分级查看

    /**
     * 分级查看会话消息。
     *
     * @param mode redacted（脱敏，wecom.view）或 raw（原文，wecom.view_raw + 审批）。
     * @param viewerUserId 调阅人（必填，写审计）。
     * @param approvalRef raw 模式必填审批引用。
     */
    public PageResponse<WecomMessageView> viewMessages(
            String conversationId,
            String mode,
            String viewerUserId,
            String approvalRef
    ) {
        String resolvedMode = mode == null || mode.isBlank() ? "redacted" : mode.trim().toLowerCase(Locale.ROOT);
        if (!"redacted".equals(resolvedMode) && !"raw".equals(resolvedMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "查看模式必须为脱敏（redacted）或原文（raw）");
        }
        String resolvedConversationId = requireText(conversationId, "conversation_id");
        String resolvedViewer = requireText(viewerUserId, "viewer_user_id");
        boolean raw = "raw".equals(resolvedMode);
        String resolvedApprovalRef = trimToNull(approvalRef);
        if (raw && resolvedApprovalRef == null) {
            // 原文调阅最小授权：必须带审批引用，否则拒绝。
            ApiException denied = ApiException.forbidden(
                    "WECOM_RAW_APPROVAL_REQUIRED", "查看企微会话原文必须提供审批依据");
            auditEventService.recordFailure(
                    "wecom.raw_view_failed",
                    "wecom.view_raw",
                    "wecom_conversation",
                    resolvedConversationId,
                    requestId(),
                    null,
                    denied.code()
            );
            throw denied;
        }

        WecomConversationRecord conversation = repository.findConversationById(resolvedConversationId)
                .orElseThrow(() -> ApiException.notFound("WECOM_CONVERSATION_NOT_FOUND", "会话记录不存在或已被删除"));
        // M723-F14（治理地图第 653 行「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」）：脱敏与原文调阅都只能看
        // 可触及项目的会话，平台管理员全量（D-032）；先于读消息与写调阅留痕，拒绝时请求事务回滚、单次来源不算用掉。
        // 第十八批 G 段 82b-R5（PGM 第 659 行）：按入口权限码（脱敏 wecom.view、原文 wecom.view_raw）可触及的项目判，含可跨项目角色勾选的项目。
        if (authorizationService.currentProjectScopeQuietly().isPresent() && !authorizationService.canReachProject(
                authorizationService.currentActor(), conversation.projectId(), raw ? "wecom.view_raw" : "wecom.view")) {
            ApiException denied = ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "企微会话属于其他综合体项目");
            if (raw) {
                auditEventService.recordFailure(
                        "wecom.raw_view_failed",
                        "wecom.view_raw",
                        "wecom_conversation",
                        resolvedConversationId,
                        requestId(),
                        null,
                        denied.code()
                );
            }
            throw denied;
        }
        List<WecomMessageRecord> messages = repository.findMessagesByConversation(conversation.id());

        Instant now = clock.instant();
        // 调阅即写审计（redacted/raw 都留痕；raw 带 approval_ref）。
        repository.saveAccessAudit(new WecomAccessAuditRecord(
                "wacc_" + UUID.randomUUID(),
                conversation.id(),
                resolvedViewer,
                resolvedMode,
                raw ? resolvedApprovalRef : null,
                now
        ));

        List<WecomMessageView> views = new ArrayList<>();
        for (WecomMessageRecord message : messages) {
            views.add(new WecomMessageView(
                    message.id(),
                    message.conversationId(),
                    message.senderRef(),
                    message.contentRedacted(),
                    raw ? piiCipher.decrypt(message.contentRawRef()) : null,
                    message.riskFlags(),
                    message.anonymized(),
                    message.sentAt(),
                    message.businessDate()
            ));
        }

        auditEventService.recordSuccess(
                raw ? "wecom.raw_viewed" : "wecom.redacted_viewed",
                raw ? "wecom.view_raw" : "wecom.view",
                "wecom_conversation",
                conversation.id(),
                requestId(),
                null,
                Map.of("view_type", resolvedMode, "viewer_user_id", resolvedViewer, "count", views.size())
        );
        if (raw) {
            // 敏感原文调阅再记一条统一敏感访问事件，便于横向风控聚合。
            auditEventService.recordSuccess(
                    "sensitive.viewed",
                    "wecom.view_raw",
                    "wecom_conversation",
                    conversation.id(),
                    requestId(),
                    null,
                    Map.of("approval_ref", resolvedApprovalRef, "viewer_user_id", resolvedViewer)
            );
        }
        return new PageResponse<>(views, 1, views.size(), views.size());
    }

    // ----------------------------------------------------------------------------------------- 留存策略

    public PageResponse<WecomRetentionPolicyRecord> listRetentionPolicies() {
        try {
            auditEventService.recordSuccess("wecom.retention_policy_viewed", "wecom.retention.manage",
                    "wecom_retention_policy", "permanent", requestId(), null, Map.of("retention", "permanent"));
            return new PageResponse<>(List.of(new WecomRetentionPolicyRecord(
                    "wrp_permanent", "global", "*", null, "permanent", true, null, null)), 1, 1, 1);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("wecom.retention_policy_view_failed", "wecom.retention.manage",
                    "wecom_retention_policy", "permanent", requestId(), null, errorCode(exception));
            throw exception;
        }
    }

    public WecomRetentionPolicyRecord upsertRetentionPolicy(WecomRetentionPolicyRequest request) {
        auditEventService.recordFailure("wecom.retention_policy_failed", "wecom.retention.manage",
                "wecom_retention_policy", "permanent", requestId(),
                request == null ? null : request.idempotencyKey(), "WECOM_PERMANENT_RETENTION");
        throw ApiException.conflict("WECOM_PERMANENT_RETENTION", "工作会话永久保存，不允许配置自动到期处理");
    }

    /** 旧任务入口也不能归档、删除或匿名化任何工作会话。 */
    public WecomRetentionRunResult applyRetention(LocalDate today) {
        return new WecomRetentionRunResult(0, 0, 0);
    }

    // ----------------------------------------------------------------------------------------- 质检（规则码命中）

    /**
     * 企微违规规则码命中（P0#8）：读 analytics_risk_rules（domain=compliance、enabled）的关键词命中产出 risk_flags。
     *
     * <p>由硬编码 RISK_KEYWORDS 升级为读可配置规则码（库无规则时回退内置默认 {@link #DEFAULT_RISK_RULES}）；
     * 返回值仍为既有 risk_flags 标记码集合（向后兼容 wecom_messages.risk_flags 与既有断言）。AI 语义质检独立并存。
     */
    List<String> detectRiskFlags(String content) {
        return riskFlagsFromMatches(matchRules(content, resolveActiveRules()));
    }

    /** 本次质检生效的企微违规规则码：库内 compliance 启用规则优先；为空则回退内置默认（不退化）。 */
    private List<WecomRiskRuleRecord> resolveActiveRules() {
        List<WecomRiskRuleRecord> rules = repository.findEnabledWecomRiskRules();
        return rules == null || rules.isEmpty() ? DEFAULT_RISK_RULES : rules;
    }

    /** 单条消息按规则集命中（同一规则只记一次，取首个命中关键词作为脱敏片段证据）。 */
    private List<RuleMatch> matchRules(String content, List<WecomRiskRuleRecord> rules) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<RuleMatch> matches = new ArrayList<>();
        for (WecomRiskRuleRecord rule : rules) {
            if (rule.keywords() == null) {
                continue;
            }
            for (String keyword : rule.keywords()) {
                if (keyword != null && !keyword.isBlank() && content.contains(keyword)) {
                    matches.add(new RuleMatch(rule, keyword));
                    break;
                }
            }
        }
        return matches;
    }

    /** 命中规则 → risk_flags 标记码集合（保序去重，映射缺失时回退规则码小写，向后兼容）。 */
    private List<String> riskFlagsFromMatches(List<RuleMatch> matches) {
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        for (RuleMatch match : matches) {
            flags.add(RULE_CODE_TO_FLAG.getOrDefault(
                    match.rule().ruleCode(), match.rule().ruleCode().toLowerCase(Locale.ROOT)));
        }
        return List.copyOf(flags);
    }

    /**
     * P0#8 命中落地：每个命中规则 → 写 wecom_risk_hits（脱敏片段）+ 入复核队列
     * （analytics_risk_alerts，source_domain=wecom，open）+ 事件 wecom.risk.hit_detected。返回命中条数。
     */
    private int persistRiskHits(
            WecomConversationRecord conversation,
            String messageId,
            List<RuleMatch> matches,
            Instant now
    ) {
        int created = 0;
        for (RuleMatch match : matches) {
            WecomRiskRuleRecord rule = match.rule();
            String hitId = "wrh_" + UUID.randomUUID();
            // 命中片段按既有 PiiTextRedactor 脱敏（普通展示口径），不落明文 PII。
            String snippet = PiiTextRedactor.redactAndTruncate(
                    match.keyword(), MAX_SNIPPET_CHARS).value();
            // 命中幂等键（消息×规则）：孤岛表与统一表共用，重复同步不产生重复告警。
            String hitIdempotencyKey = "wecom-hit:" + messageId + ":" + rule.ruleCode();
            // 入复核队列：写孤岛 analytics_risk_alerts，标 source_domain=wecom（看板/命中 alert_id 关联读侧仍依赖此表）。
            String alertId = repository.enqueueRiskAlert(new WecomRiskAlertEnqueue(
                    "ara_" + UUID.randomUUID(),
                    rule.ruleId(),
                    rule.riskLevel(),
                    "wecom_message",
                    messageId,
                    "WeCom compliance hit: " + rule.ruleCode(),
                    hitIdempotencyKey,
                    now));
            // P1：再接入统一异常责任链 risk_alerts（source_domain='wecom'），让 V103 死表真实承载企微命中。
            // 双写：孤岛表保留（读侧依赖未迁移），统一表新增一行 open 预警 + risk_alert_actions。
            reportToUnifiedRiskAlerts(conversation, messageId, rule, hitIdempotencyKey);
            repository.saveRiskHit(new WecomRiskHitRecord(
                    hitId,
                    conversation.id(),
                    messageId,
                    rule.ruleCode(),
                    rule.riskLevel(),
                    snippet,
                    "open",
                    null,
                    null,
                    conversation.projectId(),
                    alertId,
                    now,
                    now));
            auditEventService.recordSuccess(
                    "wecom.risk.hit_detected",
                    "wecom.sync",
                    "wecom_risk_hit",
                    hitId,
                    requestId(),
                    null,
                    Map.of("rule_code", rule.ruleCode(), "risk_level", rule.riskLevel(),
                            "conversation_id", conversation.id(), "alert_id", alertId));
            created++;
        }
        return created;
    }

    /**
     * P1：企微违规命中接入统一异常责任链 risk_alerts（evidence/RiskAlertService.report）。
     *
     * <p>source_domain='wecom'（V103 CHECK 白名单已含），source_ref 带 conversation/message 上下文，
     * summary 透传 rule_code，risk_level 直接复用规则风险级（统一表 RISK_LEVELS 同枚举）。
     * 幂等键复用命中维度键（消息×规则），重复同步不重复上报。
     *
     * <p>fail-safe：未注入服务（单测/无 SecurityContext）或上报异常都不阻塞同步主链——
     * 孤岛 analytics_risk_alerts 与 wecom_risk_hits 仍是命中落地的权威，统一表为汇聚镜像。
     */
    private void reportToUnifiedRiskAlerts(
            WecomConversationRecord conversation,
            String messageId,
            WecomRiskRuleRecord rule,
            String hitIdempotencyKey
    ) {
        if (riskAlertService == null) {
            return;
        }
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    hitIdempotencyKey,
                    "wecom",
                    "wecom_conversation:" + conversation.id() + "/message:" + messageId,
                    "wecom_risk_hit",
                    rule.riskLevel(),
                    "WeCom compliance hit: " + rule.ruleCode(),
                    conversation.projectId(),
                    "rule_code=" + rule.ruleCode() + "; conversation=" + conversation.id()
                            + "; message=" + messageId,
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响同步主链（命中已落孤岛表 + wecom_risk_hits）。
            auditEventService.recordFailure(
                    "wecom.risk.unified_alert_failed",
                    "wecom.sync",
                    "risk_alert",
                    messageId,
                    requestId(),
                    hitIdempotencyKey,
                    errorCode(exception));
        }
    }

    // ----------------------------------------------------------------------------------------- 命中查询 / 复核处置

    /** 查询企微违规命中（可按 status / conversationId 过滤；分页占位单页）。 */
    public PageResponse<WecomRiskHitRecord> listRiskHits(String status, String conversationId) {
        String resolvedStatus = trimToNull(status);
        if (resolvedStatus != null && !HIT_STATUSES.contains(resolvedStatus)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "状态必须为 open/reviewing/confirmed/dismissed 之一");
        }
        List<WecomRiskHitRecord> hits = repository.findRiskHits(resolvedStatus, trimToNull(conversationId));
        // 第十八批 I 段 88-R1（PGM 第 659 行「普通岗位没有跨项目查看权」「逐个勾选的只能查看被选中的项目」）：非平台管理员只列
        // wecom.risk_hit.view 可触及项目内的命中，含可跨项目角色勾选的项目；平台管理员全量（同 viewMessages）。
        if (authorizationService.currentProjectScopeQuietly().isPresent()) {
            AuthorizationService.ProjectReach reach =
                    authorizationService.projectReach(authorizationService.currentActor(), "wecom.risk_hit.view");
            hits = hits.stream().filter(hit -> reach.covers(hit.projectId())).toList();
        }
        return new PageResponse<>(hits, 1, hits.size(), hits.size());
    }

    /**
     * 复核处置命中：open→reviewing（领取）/ reviewing|open→confirmed（确认违规）/ dismissed（误报驳回）。
     *
     * @param targetStatus reviewing / confirmed / dismissed。
     * @param reviewerId   复核处置人（必填，写命中 reviewer_id 与事件）。
     */
    public WecomRiskHitRecord reviewRiskHit(
            String hitId, String targetStatus, String reviewerId, String reviewNote) {
        String resolvedHitId = requireText(hitId, "hit_id");
        String resolvedStatus = requireText(targetStatus, "status").toLowerCase(Locale.ROOT);
        if (!REVIEW_TARGET_STATUSES.contains(resolvedStatus)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "状态必须为 reviewing/confirmed/dismissed 之一");
        }
        String resolvedReviewer = requireText(reviewerId, "reviewer_id");
        WecomRiskHitRecord existing = repository.findRiskHitById(resolvedHitId)
                .orElseThrow(() -> ApiException.notFound("WECOM_RISK_HIT_NOT_FOUND", "风险命中记录不存在或已被删除"));
        // 第十八批 I 段 88-R1：非平台管理员只能复核 wecom.risk_hit.review 可触及项目内的命中，先于终态判断与写库。
        if (authorizationService.currentProjectScopeQuietly().isPresent() && !authorizationService.canReachProject(
                authorizationService.currentActor(), existing.projectId(), "wecom.risk_hit.review")) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该违规命中不在本权限可触及的项目范围内");
        }
        // 终态（confirmed/dismissed）不可再流转，避免复核覆盖既有处置结论。
        if ("confirmed".equals(existing.status()) || "dismissed".equals(existing.status())) {
            throw ApiException.conflict("WECOM_RISK_HIT_ALREADY_RESOLVED",
                    "该风险命中已处理完毕，处理结果：" + existing.status());
        }
        WecomRiskHitRecord updated = repository.updateRiskHitReview(
                resolvedHitId, resolvedStatus, resolvedReviewer, trimToNull(reviewNote));
        auditEventService.recordSuccess(
                "wecom.risk.hit_reviewed",
                "wecom.risk_hit.review",
                "wecom_risk_hit",
                resolvedHitId,
                requestId(),
                null,
                Map.of("from_status", existing.status(), "to_status", resolvedStatus,
                        "reviewer_id", resolvedReviewer, "rule_code", existing.ruleCode()));
        return updated;
    }

    /** 命中规则 + 命中关键词（脱敏片段证据来源）。 */
    private record RuleMatch(WecomRiskRuleRecord rule, String keyword) {
    }

    /**
     * AI 质检结构化结果转 JSON 串落 ai_qc_result（仅 ANALYZED 且有内容时回填；占位/跳过/失败=null）。
     * 序列化异常不阻塞落库——返回 null（结果留空，关键词质检与状态码不受影响）。
     */
    private String aiQcResultJson(WecomQcResult result) {
        if (result == null || !result.hasStructuredResult()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result.toResultMap());
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    // ----------------------------------------------------------------------------------------- 校验/工具

    private void validateSyncRequest(WecomConversationSyncRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.externalConversationId(), "external_conversation_id");
        String projectId = requireText(request.projectId(), "project_id");
        // 堵越权注入：不再裸信客户端 projectId，必须是已登记项目，否则拒绝（防对任意/不存在项目注入会话）。
        if (!repository.projectExists(projectId)) {
            throw ApiException.badRequest("WECOM_PROJECT_NOT_FOUND", "项目编号不是已登记的项目");
        }
    }

    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
