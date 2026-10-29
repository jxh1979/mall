package com.complex.platform.evidence;

// 文件版本：V0.01

import java.time.Instant;
import java.util.Map;

/**
 * 第十八批 F 段第 14 条 14-R1：一类可人工补录的对象（BSM 第 1642 行「记录缺失时允许人工补录」）。各对象只实现本接口并注册为 Spring 组件，
 * 公共流程（原因与责任人、凭证、时限、通话录音、主管复核、批准落地、查询、指标）由 {@link ManualBackfillService} 统一办理。
 * type() 须为 manual_backfills.target_type 的三值之一（V570）；policyActionType() 须在主管复核策略表登记（SupervisorReviewService）。
 */
public interface ManualBackfillTarget {

    /** manual_backfills.target_type。 */
    String type();

    /** 提交补录须持的权限码（服务先 requirePermission，再调 {@link #requireScope}）。 */
    String submitPermission();

    /** 查看补录记录须持的权限码（服务先 requirePermission，再调 {@link #requireView}）。 */
    String viewPermission();

    /** 主管复核动作类型（策略表 actionType）。 */
    String policyActionType();

    /** 幂等之前调用：对象存在＋商户／可触及项目范围闸，不满足即抛 403／404（fail-closed）。 */
    void requireScope(String targetId);

    /**
     * 幂等之内调用：核对对象确属缺失（不满足抛 409／400），并给出补录所需的事实。part 为补录部位（交接段名，其余为空）；
     * request 为提交原文（各对象只读自己用到的字段）。
     */
    Resolved resolve(String targetId, String part, ManualBackfillService.Request request);

    /** 查看补录记录的范围闸（对象存在＋商户／可触及项目范围）。 */
    void requireView(String targetId);

    /**
     * 复核通过时在批准事务内调用：锁对象、重核仍可补，条件已变即抛 409 MANUAL_BACKFILL_TARGET_CHANGED（整笔批准回滚）；
     * 否则落业务。原始记录不改写（并列展示、互不覆盖）。
     */
    void apply(ManualBackfillService.Backfill backfill);

    /**
     * 补录事实。businessAt＝业务发生时刻（时限起算）；occurredAt＝被补录事实原时点；arrivalAt／openCardAt＝客人到店／开卡时刻
     * （不适用通话录音规则的对象为空；arrivalAt 为空即不做录音校验）；beneficiaryId＝被影响方（不得复核）；proposed 的键用蛇形；
     * originalSnapshot＝提交时的原始记录；summary＝给复核人看的对象、原记录与拟补录内容。
     */
    record Resolved(String projectId, String merchantId, Instant businessAt, Instant occurredAt, Instant arrivalAt,
                    Instant openCardAt, String beneficiaryId, Map<String, Object> proposed, Object originalSnapshot,
                    String summary) {
    }
}
