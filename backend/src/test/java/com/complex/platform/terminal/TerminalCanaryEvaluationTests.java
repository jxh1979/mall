package com.complex.platform.terminal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 灰度层（V342，R-10）求值语义单测（不依赖库）。
 *
 * <p>灰度是「取最严」链条上最细的一档，只能收窄不能放宽：
 * 无 active 试点=弃权；有 active 试点=白名单内不关、白名单外关。
 */
class TerminalCanaryEvaluationTests {

    private static final String CAPABILITY = "bar.order.create";

    private static Map<String, String> scopes() {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("device", "dev_1");
        scopes.put("version", "1.2.3");
        scopes.put("module", "bar");
        scopes.put("business_system", "bar");
        scopes.put("merchant", "mch_1");
        scopes.put("project", "prj_1");
        scopes.put("global", "global");
        return scopes;
    }

    @Test
    void abstainsWhenNoActiveRollout() {
        FakeCanaryRepository repository = new FakeCanaryRepository(null);

        TerminalCanaryDecision decision = repository.evaluate("kiosk", CAPABILITY, scopes());

        assertThat(decision.outcome()).isEqualTo(TerminalCanaryDecision.Outcome.ABSTAIN);
        assertThat(decision.denied()).isFalse();
    }

    @Test
    void allowsWhenAnyScopeMatchesWhitelist() {
        FakeCanaryRepository repository = new FakeCanaryRepository(rollout());
        repository.addTarget("merchant", "mch_1");

        TerminalCanaryDecision decision = repository.evaluate("kiosk", CAPABILITY, scopes());

        assertThat(decision.outcome()).isEqualTo(TerminalCanaryDecision.Outcome.ALLOW);
        assertThat(decision.matchedTargetType()).isEqualTo("merchant");
        assertThat(decision.matchedTargetId()).isEqualTo("mch_1");
    }

    @Test
    void deniesWhenActiveRolloutHasNoMatchingTarget() {
        FakeCanaryRepository repository = new FakeCanaryRepository(rollout());
        repository.addTarget("device", "dev_other");
        repository.addTarget("merchant", "mch_other");

        TerminalCanaryDecision decision = repository.evaluate("kiosk", CAPABILITY, scopes());

        assertThat(decision.denied()).isTrue();
        assertThat(decision.rolloutId()).isEqualTo("tccr_1");
    }

    @Test
    void deniesWhenActiveRolloutHasEmptyWhitelist() {
        FakeCanaryRepository repository = new FakeCanaryRepository(rollout());

        assertThat(repository.evaluate("kiosk", CAPABILITY, scopes()).denied())
                .as("试点已开但白名单为空 → 全关（取最严，绝不因为没配白名单就全放）")
                .isTrue();
    }

    @Test
    void globalTargetNeverGrantsCanaryAccess() {
        FakeCanaryRepository repository = new FakeCanaryRepository(rollout());
        repository.addTarget("global", "global");

        assertThat(repository.evaluate("kiosk", CAPABILITY, scopes()).denied())
                .as("global 不是试点维度（放它等于全量放开），求值必须忽略")
                .isTrue();
    }

    @Test
    void blankScopeValueNeverMatchesWhitelist() {
        FakeCanaryRepository repository = new FakeCanaryRepository(rollout());
        repository.addTarget("device", "");
        Map<String, String> scopes = scopes();
        scopes.put("device", "");

        assertThat(repository.evaluate("kiosk", CAPABILITY, scopes).denied())
                .as("空维度值不得被当成命中")
                .isTrue();
    }

    @Test
    void noRolloutHelperAlwaysAbstains() {
        assertThat(TerminalCanaryRepository.noRollout().evaluate("kiosk", CAPABILITY, scopes()).outcome())
                .isEqualTo(TerminalCanaryDecision.Outcome.ABSTAIN);
    }

    private static TerminalCanaryRolloutRecord rollout() {
        return new TerminalCanaryRolloutRecord(
                "tccr_1", "kiosk", CAPABILITY, true, "试点", Instant.now(), Instant.now());
    }

    /** 仅实现求值所需的两个读方法。 */
    private static final class FakeCanaryRepository implements TerminalCanaryRepository {

        private final TerminalCanaryRolloutRecord activeRollout;
        private final List<TerminalCanaryTargetRecord> targets = new ArrayList<>();

        private FakeCanaryRepository(TerminalCanaryRolloutRecord activeRollout) {
            this.activeRollout = activeRollout;
        }

        void addTarget(String targetType, String targetId) {
            targets.add(new TerminalCanaryTargetRecord(
                    "tcct_" + targets.size(), "tccr_1", targetType, targetId, "试点",
                    Instant.now(), Instant.now(), null, null, null));
        }

        @Override
        public Optional<TerminalCanaryRolloutRecord> findActiveRollout(
                String terminalType, String capabilityCode) {
            return Optional.ofNullable(activeRollout);
        }

        @Override
        public List<TerminalCanaryTargetRecord> listActiveTargets(String rolloutId) {
            return List.copyOf(targets);
        }

        @Override
        public List<TerminalCanaryRolloutRecord> listRollouts() {
            return activeRollout == null ? List.of() : List.of(activeRollout);
        }

        @Override
        public Optional<TerminalCanaryRolloutRecord> findRolloutById(String id) {
            return Optional.ofNullable(activeRollout);
        }

        @Override
        public Optional<TerminalCanaryRolloutRecord> findRollout(String terminalType, String capabilityCode) {
            return Optional.ofNullable(activeRollout);
        }

        @Override
        public TerminalCanaryRolloutRecord saveRollout(TerminalCanaryRolloutRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TerminalCanaryTargetRecord> findTargetById(String id) {
            return targets.stream().filter(target -> target.id().equals(id)).findFirst();
        }

        @Override
        public TerminalCanaryTargetRecord saveTarget(TerminalCanaryTargetRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TerminalCanaryTargetRecord archiveTarget(
                String id, Instant archivedAt, String archiveReason, String archivedBy) {
            throw new UnsupportedOperationException();
        }
    }
}
