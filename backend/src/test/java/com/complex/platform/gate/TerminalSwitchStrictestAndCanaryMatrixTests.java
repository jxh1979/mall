package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.terminal.TerminalCanaryRolloutRecord;
import com.complex.platform.terminal.TerminalCanaryTargetRecord;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalCanaryRepository;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * R-10（2026-07-22 用户裁决）终端开关求值「取最严」+ 灰度层组合矩阵。
 *
 * <p>被证伪的旧口径：{@code resolveEffectiveSwitch} 按 device→…→global 命中第一条显式行即返回，
 * 于是「global=关 + device=开」返回 device 行 → <b>放行</b>，低层打开压过高层关闭，安全方向反了（P0）。
 *
 * <p>本类用<b>真实 JDBC 仓储 + 真实闸口 + 真实库</b>跑满 <b>2^8 = 256</b> 种层级组合
 * （global / project / merchant / business_system / module / version / device / 灰度，每层显式开或显式关），
 * 断言：<b>任一层关闭 ⇔ 最终关闭</b>；八层全开才放行。
 * 旧实现在其中 128 种组合（device 层为"开"但更高层有"关"）上会放行，本矩阵直接把它钉死。
 */
@SpringBootTest(properties = {
        // 遵从 COMPLEX_DATABASE_URL（并发会话各自的隔离测试库）；无 env 时回落仓库默认 complex_test。
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TerminalSwitchStrictestAndCanaryMatrixTests {

    private static final String CAPABILITY = "test.r10.strictest.matrix";
    private static final String TERMINAL_TYPE = "kiosk";

    private static final String PROJECT_ID = "prj_r10";
    private static final String MERCHANT_ID = "mch_r10";
    private static final String BUSINESS_SYSTEM = "bar";
    private static final String MODULE_CODE = "mod_r10";
    private static final String VERSION = "1.0.0-r10";
    private static final String DEVICE_ID = "dev_r10";

    /** 位序 = 由粗到细的层级顺序；bit=1 表示该层显式关闭。bit 7 = 灰度层。 */
    private static final String[] LAYERS = {
            "global", "project", "merchant", "business_system", "module", "version", "device", "canary"
    };
    private static final String[] LAYER_SCOPE_IDS = {
            "global", PROJECT_ID, MERCHANT_ID, BUSINESS_SYSTEM, MODULE_CODE, VERSION, DEVICE_ID, null
    };

    @Autowired
    private TerminalSwitchGuard guard;

    @Autowired
    private TerminalDeviceRepository terminalDeviceRepository;

    @Autowired
    private TerminalCanaryRepository canaryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void bindRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Terminal-Type", TERMINAL_TYPE);
        request.addHeader("X-Device-Id", DEVICE_ID);
        request.addHeader("X-Client-Version", VERSION);
        request.addHeader("X-Module-Code", MODULE_CODE);
        request.addHeader("X-Business-System", BUSINESS_SYSTEM);
        request.addHeader("X-Merchant-Id", MERCHANT_ID);
        request.addHeader("X-Project-Id", PROJECT_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        clearFixtures();
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ── 组合矩阵：8 层 × 开/关 = 256 组 ────────────────────────────────────────
    @ParameterizedTest(name = "mask={0}")
    @ValueSource(ints = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
            64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
            80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
            96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
            112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
            128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
            144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159,
            160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
            176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
            192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
            208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
            224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
            240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255
    })
    void anyClosedLayerClosesTheWholeChain(int mask) {
        seedMatrix(mask);
        List<String> closedLayers = closedLayerNames(mask);
        if (closedLayers.isEmpty()) {
            assertThatCode(() -> guard.check(CAPABILITY))
                    .as("八层全开应放行")
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> guard.check(CAPABILITY))
                    .as("关闭层=%s 应最终关闭（取最严）", closedLayers)
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));
        }
    }

    // ── 定向回归：P0 缺陷本体（低层打开压过高层关闭） ───────────────────────────
    @Test
    void deviceRowEnabledNoLongerOverridesGlobalRowDisabled() {
        insertSwitch("global", "global", false);
        insertSwitch("device", DEVICE_ID, true);

        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .as("global 关 + device 开：旧实现放行(P0)，取最严后必须拒绝")
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));

        // 审计口径：报"最高层"的关闭行，告诉运营是全局把它关了。
        assertThat(terminalDeviceRepository
                .resolveEffectiveSwitch(TERMINAL_TYPE, CAPABILITY, GateRequestContextScopes.of()))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.scopeType()).isEqualTo("global");
                    assertThat(hit.enabled()).isFalse();
                });
    }

    @Test
    void merchantRowDisabledClosesEvenWhenModuleAndDeviceEnabled() {
        insertSwitch("merchant", MERCHANT_ID, false);
        insertSwitch("module", MODULE_CODE, true);
        insertSwitch("device", DEVICE_ID, true);

        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    @Test
    void allLayersEnabledPasses() {
        insertSwitch("global", "global", true);
        insertSwitch("project", PROJECT_ID, true);
        insertSwitch("merchant", MERCHANT_ID, true);
        insertSwitch("business_system", BUSINESS_SYSTEM, true);
        insertSwitch("module", MODULE_CODE, true);
        insertSwitch("version", VERSION, true);
        insertSwitch("device", DEVICE_ID, true);

        assertThatCode(() -> guard.check(CAPABILITY)).doesNotThrowAnyException();
    }

    @Test
    void noExplicitRowRejects() {
        // PGM 第 742 行 opt-in：无显式行即关闭。
        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(terminalDeviceRepository
                .resolveEffectiveSwitch(TERMINAL_TYPE, CAPABILITY, GateRequestContextScopes.of()))
                .isEmpty();
    }

    @Test
    void mostSpecificEnabledRowStillWinsWhenNoLayerIsClosed() {
        insertSwitch("global", "global", true);
        insertSwitch("device", DEVICE_ID, true);

        assertThat(terminalDeviceRepository
                .resolveEffectiveSwitch(TERMINAL_TYPE, CAPABILITY, GateRequestContextScopes.of()))
                .hasValueSatisfying(hit -> assertThat(hit.scopeType()).isEqualTo("device"));
    }

    @Test
    void terminalSpecificRowStillBeatsWildcardRowAtTheSameLayer() {
        // 同一层内「专端行 > 通配行」保持不变（通配/专端是另一坐标轴，不是层级，不纳入取最严）。
        insertSwitch("*", "global", "global", false);
        insertSwitch(TERMINAL_TYPE, "global", "global", true);

        assertThat(terminalDeviceRepository
                .resolveEffectiveSwitch(TERMINAL_TYPE, CAPABILITY, GateRequestContextScopes.of()))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.terminalType()).isEqualTo(TERMINAL_TYPE);
                    assertThat(hit.enabled()).isTrue();
                });
        assertThatCode(() -> guard.check(CAPABILITY)).doesNotThrowAnyException();
    }

    // ── 灰度层专项 ───────────────────────────────────────────────────────────
    @Test
    void canaryAbstainsWhenNoActiveRollout() {
        insertSwitch("global", "global", true); // opt-in：先有显式开启行，灰度层才有放行可收窄
        seedCanary(false, "dev_not_in_pilot");
        assertThatCode(() -> guard.check(CAPABILITY))
                .as("灰度未启用 → 灰度层弃权，行为与改动前一致")
                .doesNotThrowAnyException();
    }

    @Test
    void canaryNarrowsToWhitelistWhenActive() {
        insertSwitch("global", "global", true);
        seedCanary(true, "dev_not_in_pilot");
        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .as("灰度启用且本设备不在白名单 → 关闭")
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));

        seedCanary(true, DEVICE_ID);
        assertThatCode(() -> guard.check(CAPABILITY))
                .as("灰度启用且本设备在白名单 → 放行")
                .doesNotThrowAnyException();
    }

    @Test
    void canaryWhitelistCannotReopenAnUpperLayerThatIsClosed() {
        insertSwitch("merchant", MERCHANT_ID, false);
        seedCanary(true, DEVICE_ID);

        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .as("灰度只能收窄不能放宽：白名单命中也翻不回上层的关闭")
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    @Test
    void archivedWhitelistEntryNoLongerGrantsCanaryAccess() {
        insertSwitch("global", "global", true);
        seedCanary(true, DEVICE_ID);
        assertThatCode(() -> guard.check(CAPABILITY)).doesNotThrowAnyException();

        TerminalCanaryRolloutRecord rollout =
                canaryRepository.findActiveRollout(TERMINAL_TYPE, CAPABILITY).orElseThrow();
        TerminalCanaryTargetRecord target = canaryRepository.listActiveTargets(rollout.id()).get(0);
        canaryRepository.archiveTarget(target.id(), Instant.now(), "试点结束", "tester");

        assertThat(canaryRepository.listActiveTargets(rollout.id())).isEmpty();
        assertThatThrownBy(() -> guard.check(CAPABILITY))
                .as("移出白名单（软删除）后该设备即被灰度关闭；行仍在库里可恢复")
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_capability_canary_targets WHERE id = ?",
                Integer.class, target.id())).isEqualTo(1);
    }

    // ── fixtures ────────────────────────────────────────────────────────────
    private void seedMatrix(int mask) {
        for (int bit = 0; bit < 7; bit++) {
            insertSwitch(LAYERS[bit], LAYER_SCOPE_IDS[bit], (mask & (1 << bit)) == 0);
        }
        boolean canaryClosed = (mask & (1 << 7)) != 0;
        // 灰度层恒启用；bit=1 → 白名单里放的是别的设备（本请求不命中，灰度关）；bit=0 → 放本设备（灰度开）。
        seedCanary(true, canaryClosed ? "dev_not_in_pilot" : DEVICE_ID);
    }

    private List<String> closedLayerNames(int mask) {
        List<String> names = new ArrayList<>();
        for (int bit = 0; bit < LAYERS.length; bit++) {
            if ((mask & (1 << bit)) != 0) {
                names.add(LAYERS[bit]);
            }
        }
        return names;
    }

    private void insertSwitch(String scopeType, String scopeId, boolean enabled) {
        insertSwitch(TERMINAL_TYPE, scopeType, scopeId, enabled);
    }

    private void insertSwitch(String terminalType, String scopeType, String scopeId, boolean enabled) {
        terminalDeviceRepository.saveCapabilitySwitch(new TerminalCapabilitySwitchRecord(
                "tcs_r10_" + terminalType + "_" + scopeType,
                terminalType,
                CAPABILITY,
                scopeType,
                scopeId,
                enabled,
                "R-10 矩阵夹具",
                Instant.now(),
                Instant.now()
        ));
    }

    private void seedCanary(boolean active, String whitelistedDeviceId) {
        jdbcTemplate.update("DELETE FROM terminal_capability_canary_targets t USING "
                + "terminal_capability_canary_rollouts r WHERE t.rollout_id = r.id AND r.capability_code = ?",
                CAPABILITY);
        jdbcTemplate.update("DELETE FROM terminal_capability_canary_rollouts WHERE capability_code = ?", CAPABILITY);
        Instant now = Instant.now();
        TerminalCanaryRolloutRecord rollout = canaryRepository.saveRollout(new TerminalCanaryRolloutRecord(
                "tccr_r10", TERMINAL_TYPE, CAPABILITY, active, "R-10 矩阵夹具", now, now));
        canaryRepository.saveTarget(new TerminalCanaryTargetRecord(
                "tcct_r10", rollout.id(), "device", whitelistedDeviceId, "试点设备",
                now, now, null, null, null));
    }

    private void clearFixtures() {
        jdbcTemplate.update("DELETE FROM terminal_capability_canary_targets t USING "
                + "terminal_capability_canary_rollouts r WHERE t.rollout_id = r.id AND r.capability_code = ?",
                CAPABILITY);
        jdbcTemplate.update("DELETE FROM terminal_capability_canary_rollouts WHERE capability_code = ?", CAPABILITY);
        jdbcTemplate.update("DELETE FROM terminal_capability_switches WHERE capability_code = ?", CAPABILITY);
    }

    /** 复刻 {@link GateRequestContext#orderedScopes()} 的层级顺序，供直接断言仓储返回哪一层。 */
    private static final class GateRequestContextScopes {
        static java.util.Map<String, String> of() {
            java.util.Map<String, String> scopes = new java.util.LinkedHashMap<>();
            scopes.put("device", DEVICE_ID);
            scopes.put("version", VERSION);
            scopes.put("module", MODULE_CODE);
            scopes.put("business_system", BUSINESS_SYSTEM);
            scopes.put("merchant", MERCHANT_ID);
            scopes.put("project", PROJECT_ID);
            scopes.put("global", "global");
            return scopes;
        }
    }
}
