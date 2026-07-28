package com.complex.platform.terminal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 终端开关灰度层仓储（V342，R-10）。灰度是求值链中【独立的最细粒度级别】，
 * 与 {@code terminal_capability_switches} 的七个层级串联，语义为「取最严」：只能收窄不能放宽。
 */
public interface TerminalCanaryRepository {

    /** 该 端×能力 生效中的灰度试点（terminal_type 匹配请求端或跨端通配 '*'，专端优先）。 */
    Optional<TerminalCanaryRolloutRecord> findActiveRollout(String terminalType, String capabilityCode);

    /** 试点白名单条目（仅未存档）。 */
    List<TerminalCanaryTargetRecord> listActiveTargets(String rolloutId);

    List<TerminalCanaryRolloutRecord> listRollouts();

    Optional<TerminalCanaryRolloutRecord> findRolloutById(String id);

    Optional<TerminalCanaryRolloutRecord> findRollout(String terminalType, String capabilityCode);

    TerminalCanaryRolloutRecord saveRollout(TerminalCanaryRolloutRecord record);

    Optional<TerminalCanaryTargetRecord> findTargetById(String id);

    TerminalCanaryTargetRecord saveTarget(TerminalCanaryTargetRecord record);

    /**
     * 灰度层求值。{@code orderedScopes} 复用终端开关的 scope 上下文
     * （device / version / module / business_system / merchant / project / global）。
     *
     * <p>口径：无 active 试点 → 弃权；有 active 试点且请求上下文任一维度命中白名单 → 放行；
     * 有 active 试点但一个白名单条目都没命中 → <b>关闭</b>（试点期只对白名单开放）。
     * 'global' 维度不参与命中（白名单里放 global 等于全量放开，V342 CHECK 已禁）。
     */
    default TerminalCanaryDecision evaluate(
            String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
        Optional<TerminalCanaryRolloutRecord> rollout = findActiveRollout(terminalType, capabilityCode);
        if (rollout.isEmpty()) {
            return TerminalCanaryDecision.abstain();
        }
        String rolloutId = rollout.get().id();
        Map<String, String> scopes = orderedScopes == null ? Map.of() : orderedScopes;
        for (TerminalCanaryTargetRecord target : listActiveTargets(rolloutId)) {
            if ("global".equals(target.targetType())) {
                continue;
            }
            String requestScopeId = scopes.get(target.targetType());
            if (requestScopeId != null && !requestScopeId.isBlank()
                    && requestScopeId.equals(target.targetId())) {
                return TerminalCanaryDecision.allow(rolloutId, target.targetType(), target.targetId());
            }
        }
        return TerminalCanaryDecision.deny(rolloutId);
    }

    /**
     * 「无任何灰度试点」的等价实现：灰度层恒弃权。
     * 供不关心灰度的单元测试构造闸口用；与生产上灰度表为空时的行为逐字一致
     * （注意：它不是 fail-open 后门——真实仓储读取异常时闸口按 fail-closed 拒绝）。
     */
    static TerminalCanaryRepository noRollout() {
        return new TerminalCanaryRepository() {
            @Override
            public Optional<TerminalCanaryRolloutRecord> findActiveRollout(
                    String terminalType, String capabilityCode) {
                return Optional.empty();
            }

            @Override
            public List<TerminalCanaryTargetRecord> listActiveTargets(String rolloutId) {
                return List.of();
            }

            @Override
            public List<TerminalCanaryRolloutRecord> listRollouts() {
                return List.of();
            }

            @Override
            public Optional<TerminalCanaryRolloutRecord> findRolloutById(String id) {
                return Optional.empty();
            }

            @Override
            public Optional<TerminalCanaryRolloutRecord> findRollout(
                    String terminalType, String capabilityCode) {
                return Optional.empty();
            }

            @Override
            public TerminalCanaryRolloutRecord saveRollout(TerminalCanaryRolloutRecord record) {
                throw new UnsupportedOperationException("no-op canary repository is read-only");
            }

            @Override
            public Optional<TerminalCanaryTargetRecord> findTargetById(String id) {
                return Optional.empty();
            }

            @Override
            public TerminalCanaryTargetRecord saveTarget(TerminalCanaryTargetRecord record) {
                throw new UnsupportedOperationException("no-op canary repository is read-only");
            }

            @Override
            public TerminalCanaryTargetRecord archiveTarget(
                    String id, Instant archivedAt, String archiveReason, String archivedBy) {
                throw new UnsupportedOperationException("no-op canary repository is read-only");
            }
        };
    }

    /** 软删除：移出白名单（行保留，可恢复、可审计）。 */
    TerminalCanaryTargetRecord archiveTarget(
            String id, Instant archivedAt, String archiveReason, String archivedBy);
}
