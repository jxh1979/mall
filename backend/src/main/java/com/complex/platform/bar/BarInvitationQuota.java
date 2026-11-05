package com.complex.platform.bar;

// 文件版本：V0.01

import java.util.List;
import java.util.Optional;

/**
 * 第十八批 S 段第 167 条 167-R1：订卡邀请函名额按当前台位或合并台组计，写法照 {@link BarMinSpend}。依据 BSM:1079「转台：整张台从原台位转到目标台位后，
 * 可邀请人数和最低消费额均按目标台规则执行……按目标台可邀请人数校验“已接受但尚未入场成员数＋已入场成员数”」「合并后的可邀请人数和最低消费额，
 * 分别按两侧所包含全部原台的对应数值相加计算」「已有的已接受成员和已入场成员一并进入新的合并台」「目标台此后再与其他独立台或合并台组并台时，
 * 只按目标台当前生效的可邀请人数……相加」；ENUM:114 同句；BSM:1036「已确认与已入场总人数不超过卡座可进人数」。
 *
 * <p>名额＝当前有效并台链各会话的可邀请人数快照之和（整体转台前的旧链不参与）；已占＝链内（含整体转台前旧链）各订台未移出的成员。
 * 订台还没开台、没有会话时照旧按签发时的可进人数与本订台成员计。
 */
final class BarInvitationQuota {

    private BarInvitationQuota() {
    }

    /** 订台所在台组的名额与已占人数；lockRoot＝有会话时先锁台组根会话行，序列化同一台组内各订台的接受与入场。 */
    static BarCustomerInvitationService.InvitationQuota of(BarRepository repository, BarReservationRecord reservation,
            boolean lockRoot) {
        Optional<String> root = repository.findLiveSessionIdByReservation(reservation.id())
                .map(sessionId -> BarMinSpend.rootSessionId(repository, sessionId));
        if (root.isEmpty()) {
            return quota(reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity(),
                    repository.listInvitationMembers(reservation.id()).stream()
                            .filter(member -> !"revoked".equals(member.status())).toList());
        }
        if (lockRoot) {
            repository.lockTableSession(root.get());
        }
        return quota(capacity(repository, root.get()), members(repository, root.get()));
    }

    /** 台组可邀请人数＝当前有效并台链各会话可邀请人数快照之和（口径同 BarMinSpend.thresholdCent）。 */
    static int capacity(BarRepository repository, String rootSessionId) {
        return repository.familySessionIds(rootSessionId, false).stream()
                .map(repository::findTableSessionById)
                .flatMap(Optional::stream)
                .mapToInt(BarTableSessionRecord::allowedPassFreeGuestCountSnapshot)
                .sum();
    }

    /** 台组已占成员＝链内（含整体转台前旧链）各订台未移出的成员（已接受未入场＋已入场）。 */
    static List<BarReservationInvitationMemberRecord> members(BarRepository repository, String rootSessionId) {
        return repository.listInvitationMembersBySessions(repository.familySessionIds(rootSessionId, true));
    }

    private static BarCustomerInvitationService.InvitationQuota quota(int capacity,
            List<BarReservationInvitationMemberRecord> members) {
        return new BarCustomerInvitationService.InvitationQuota(capacity, members.size(),
                (int) members.stream().filter(member -> "entered".equals(member.status())).count());
    }
}
