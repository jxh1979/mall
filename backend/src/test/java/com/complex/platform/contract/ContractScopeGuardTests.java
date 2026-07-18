package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 合同经营范围越界拦截单测（内存，无 DB）：
 * 有范围拦 / 无范围放（存量合同兼容）/ 品类混合整单拒 / 域级放行 / 查询失败 fail-open 放行留痕 / 票务域级比对 / 词表校验。
 */
class ContractScopeGuardTests {

    private final ContractRepository repository = mock(ContractRepository.class);
    private final ContractScopeGuard guard = new ContractScopeGuard(repository);
    private ListAppender<ILoggingEvent> warnings;

    @BeforeEach
    void captureWarnLogs() {
        warnings = new ListAppender<>();
        warnings.start();
        ((Logger) LoggerFactory.getLogger(ContractScopeGuard.class)).addAppender(warnings);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(ContractScopeGuard.class)).detachAppender(warnings);
    }

    private ContractRecord contract(Map<String, Object> rentTerms, String status,
            LocalDate effectiveDate, LocalDate expiryDate) {
        Instant now = Instant.now();
        return new ContractRecord("ctr_scope", "mch_1", "subj_1", null, "subj_1",
                effectiveDate, expiryDate, rentTerms, null, "none", null, Map.of(), List.of(),
                null, null, List.of(), status, null, null, null, null, now, now);
    }

    private ContractRecord effectiveWith(Map<String, Object> rentTerms) {
        return contract(rentTerms, "effective", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
    }

    @Test
    void barSaleBlockedWhenCategoryOutsideAllowedSet() {
        // 有范围拦：合同只许 beer/snack，卖 spirit → 拒，中文消息点名越界品类。
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("allowedCategories", List.of("beer", "snack")))));
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION");
                    assertThat(e.getMessage()).contains("烈酒").contains("经营范围");
                });
    }

    @Test
    void barSaleMixedCategoriesRejectsWholeOrder() {
        // 品类混合整单拒：beer 在范围内、spirit 不在 → 整单仍拒。
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("allowedCategories", List.of("beer")))));
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_1", List.of("beer", "spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
    }

    @Test
    void barSalePassesWhenDomainBarAllowed() {
        // 域级 bar = 放行全部酒吧品类（不逐品类比对）。
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("allowedCategories", List.of("bar")))));
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit", "snack")))
                .doesNotThrowAnyException();
    }

    @Test
    void legacyContractWithoutAllowedCategoriesPasses() {
        // 存量合同兼容：只有 businessScope 文字、无 allowedCategories 键 → 不拦截。
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("businessScope", "酒吧经营"))));
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .doesNotThrowAnyException();
        assertThatCode(() -> guard.requireTicketSaleAllowed("mch_1")).doesNotThrowAnyException();
    }

    @Test
    void failOpenWhenNoEffectiveContractAndWarnLogged() {
        // 无生效合同 → fail-open 放行 + WARN 留痕（拦截是合规增强，不拖垮销售主路径）。
        when(repository.listContractsByMerchant("mch_1")).thenReturn(List.of());
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .doesNotThrowAnyException();
        assertThat(warnings.list).anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("fail-open"));
    }

    @Test
    void failOpenWhenMultipleEffectiveContracts() {
        // 同商户多份生效合同（数据异常）→ fail-open 放行 + WARN。
        Map<String, Object> terms = Map.of("allowedCategories", List.of("beer"));
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(terms), effectiveWith(terms)));
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .doesNotThrowAnyException();
        assertThat(warnings.list).anyMatch(e -> e.getLevel() == Level.WARN);
    }

    @Test
    void failOpenWhenContractQueryThrows() {
        // 合同域查询抛异常 → fail-open 放行 + WARN。
        when(repository.listContractsByMerchant("mch_1")).thenThrow(new RuntimeException("db down"));
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .doesNotThrowAnyException();
        assertThat(warnings.list).anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("fail-open"));
    }

    @Test
    void expiredOrPendingContractsAreNotEnforced() {
        // 生效谓词抄合同域口径：status=effective 且今日在租期窗内；过期/未生效状态不算 → fail-open。
        when(repository.listContractsByMerchant("mch_1")).thenReturn(List.of(
                contract(Map.of("allowedCategories", List.of("beer")), "effective",
                        LocalDate.now().minusYears(2), LocalDate.now().minusDays(1)),
                contract(Map.of("allowedCategories", List.of("beer")), "drafting",
                        LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))));
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_1", List.of("spirit")))
                .doesNotThrowAnyException();
    }

    @Test
    void ticketSaleRequiresTicketingDomain() {
        // 票务域级比对：范围只配了 bar → 出票拒；补上 ticketing → 放行。
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("allowedCategories", List.of("bar")))));
        assertThatThrownBy(() -> guard.requireTicketSaleAllowed("mch_1"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION");
                    assertThat(e.getMessage()).contains("门票销售");
                });
        when(repository.listContractsByMerchant("mch_1"))
                .thenReturn(List.of(effectiveWith(Map.of("allowedCategories", List.of("bar", "ticketing")))));
        assertThatCode(() -> guard.requireTicketSaleAllowed("mch_1")).doesNotThrowAnyException();
    }

    @Test
    void nullMerchantSkipsEnforcement() {
        assertThatCode(() -> guard.requireBarSaleAllowed(null, List.of("spirit")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAllowedCategoriesEnforcesExistingVocabulary() {
        // 词表校验：只认既有业务类目；发明新枚举 → VALIDATION_FAILED；去重去空白；null=未配置。
        assertThat(ContractScopeGuard.validateAllowedCategories(null)).isNull();
        assertThat(ContractScopeGuard.validateAllowedCategories(List.of("beer", " beer ", "ticketing")))
                .containsExactly("beer", "ticketing");
        assertThatThrownBy(() -> ContractScopeGuard.validateAllowedCategories(List.of("metaverse")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).contains("metaverse");
                });
        assertThatThrownBy(() -> ContractScopeGuard.validateAllowedCategories("beer"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }
}
