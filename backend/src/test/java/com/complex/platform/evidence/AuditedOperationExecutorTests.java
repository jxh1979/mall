package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;

/**
 * 第十八批 N 段第 124 条 124-R3（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」）：控制器写口的失败留痕包装——
 * 失败落声明的失败事件（错误码取 ApiException.code，否则类名）并原样抛出；写失败事件本身出错挂在原异常上、不顶掉原异常。
 */
class AuditedOperationExecutorTests {

    private final AuditEventService audit = mock(AuditEventService.class);
    private final AuditedOperationExecutor executor = new AuditedOperationExecutor(null, audit);

    @Test
    void recordingFailureRecordsApiErrorCodeAndRethrowsSameException() {
        ApiException denied = ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：contract.draft.manage");

        assertThatThrownBy(() -> executor.recordingFailure("contract.submit_failed", "contract.draft.manage", "contract", "ctr_1",
                "k_1", () -> {
                    throw denied;
                })).isSameAs(denied);

        verify(audit).recordFailure("contract.submit_failed", "contract.draft.manage", "contract", "ctr_1", null, "k_1",
                "PERMISSION_DENIED");
    }

    @Test
    void recordingFailureKeepsOriginalExceptionWhenFailureEventWriteFails() {
        IllegalStateException original = new IllegalStateException("业务失败");
        RuntimeException auditDown = new RuntimeException("留痕库不可用");
        when(audit.recordFailure(any(), any(), any(), any(), any(), any(), any())).thenThrow(auditDown);

        assertThatThrownBy(() -> executor.recordingFailure("facility.work_order_start_failed", "facility.work_order.manage",
                "facility_work_order", "wo_1", "k_2", () -> {
                    throw original;
                })).isSameAs(original).satisfies(error -> assertThat(error.getSuppressed()).containsExactly(auditDown));

        verify(audit).recordFailure("facility.work_order_start_failed", "facility.work_order.manage", "facility_work_order", "wo_1",
                null, "k_2", "IllegalStateException");
    }
}
