package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteApprovalRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteDecision;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** V264 商户服务工单存档与审批后删除集成测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MerchantWorkOrderArchiveDeleteJdbcIntegrationTests {

    @Autowired
    private MerchantServiceService service;
    @Autowired
    private MerchantServiceRepository repository;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    private MerchantWorkOrderRecord seed(String id, String status) {
        Instant now = Instant.now();
        return repository.saveWorkOrder(new MerchantWorkOrderRecord(
                id, "merchant_wo_test", null, null, "repair", "medium", status,
                "标题", "描述", "usr_reporter", "web", null, null, null,
                "closed".equals(status) || "cancelled".equals(status) ? now : null,
                null, null, List.of(), false, null, now, now));
    }

    @Test
    void archiveThenApproveDeleteSucceedsWhenTerminal() {
        TestSecurityActors.asPlatformAdmin();
        seed("mswo_del", "closed");
        service.archiveWorkOrder("mswo_del", true, "测试存档理由");
        service.requestDeleteWorkOrder("mswo_del", new MerchantWorkOrderDeleteRequest("过期"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        MerchantWorkOrderDeleteDecision decision =
                service.approveDeleteWorkOrder("mswo_del", new MerchantWorkOrderDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        assertThat(repository.findWorkOrderById("mswo_del")).isEmpty();
    }

    @Test
    void requestDeleteRequiresArchiveFirst() {
        TestSecurityActors.asPlatformAdmin();
        seed("mswo_noarch", "closed");
        assertThatThrownBy(() -> service.requestDeleteWorkOrder("mswo_noarch", new MerchantWorkOrderDeleteRequest("x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("存档");
    }

    @Test
    void approveDeleteRejectedWhenNonTerminal() {
        TestSecurityActors.asPlatformAdmin();
        seed("mswo_use", "processing");
        service.archiveWorkOrder("mswo_use", true, "测试存档理由");
        service.requestDeleteWorkOrder("mswo_use", new MerchantWorkOrderDeleteRequest("试删"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        assertThatThrownBy(() -> service.approveDeleteWorkOrder("mswo_use",
                new MerchantWorkOrderDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("终结");
    }

    @Test
    void selfApproveForbidden() {
        TestSecurityActors.asPlatformAdmin("usr_same");
        seed("mswo_self", "closed");
        service.archiveWorkOrder("mswo_self", true, "测试存档理由");
        service.requestDeleteWorkOrder("mswo_self", new MerchantWorkOrderDeleteRequest("x"));
        // 同一 actor 审批自己的申请 → 禁止。
        assertThatThrownBy(() -> service.approveDeleteWorkOrder("mswo_self",
                new MerchantWorkOrderDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("发起人");
    }
}
