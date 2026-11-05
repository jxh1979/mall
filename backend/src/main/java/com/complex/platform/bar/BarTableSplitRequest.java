package com.complex.platform.bar;

// 文件版本：V0.02

import java.util.List;
import java.util.Map;

/**
 * 第十批 TK-27-R3：拆台（BSM L1079「当前合并台仅含两张原台时，可直接拆回两张原台；包含三张以上原台时，既可以按最近一次并台
 * 记录拆回该次并台前的两侧台组，也可以任选一张原台单独拆出，也支持一次把全部原台同时拆开」）。
 *
 * <ul>
 *   <li>mode：last_merge（按最近一次并台记录拆回两侧）/ single（任选一张原台单独拆出，须填 splitSessionId）/ all（一次全部拆开）。</li>
 *   <li>orderAssignments：并台期间无法自动确认归属的订单的人工归属 [{order_id, session_id}]（BSM L1079「无法确认订单归属时，
 *       须先人工明确归属并留痕，未完成前不得拆台」）。</li>
 *   <li>invitationAssignments [{reservation_id, session_id}]、memberAssignments [{member_id, session_id}]：第十八批 T 段第 167 条
 *       167-R3a，并台期间新发出的邀请及其已接受、已入场成员（开卡人除外）首次拆台前分配到当前合并台的某一张原台（BSM:1079「并台期间新发出的
 *       邀请，以及由这些邀请形成的已接受成员、已入场成员，在首次拆台前由授权人员逐一分配到当前合并台所包含的某一张原台并留痕」）。</li>
 * </ul>
 */
public record BarTableSplitRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String mode,
        String splitSessionId,
        List<Map<String, Object>> orderAssignments,
        List<Map<String, Object>> invitationAssignments,
        List<Map<String, Object>> memberAssignments,
        String reason,
        String requestedByUserId,
        Long primarySessionVersion
) {
}
