package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.common.api.ApiException;

class AmusementBusinessDayLegacyTests {
    @Test
    void globalCutoffCannotBypassComplexVersionApproval() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        OperatingParamsService service = new OperatingParamsService(repository, mock(AuditEventService.class));
        assertThatThrownBy(() -> service.upsertBusinessDayCutoff(new BusinessDayCutoffRequest("tk17-red", "审批旁路复现", "amusement", 10)))
                .isInstanceOf(ApiException.class).hasMessageContaining("综合体");
        verifyNoInteractions(repository);
    }
}
