package com.complex.platform.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditEventServiceTests {

    @Test
    void recordsSuccessEventWithBusinessAndPermissionContext() {
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
        AuditEventService service = new AuditEventService(repository);

        AuditEvent event = service.recordSuccess(
                "production_readiness.viewed",
                "ops.production_readiness.view",
                "production_readiness_item",
                "all",
                "request-1",
                null,
                Map.of("count", 12)
        );

        assertThat(event.eventName()).isEqualTo("production_readiness.viewed");
        assertThat(event.eventResult()).isEqualTo("success");
        assertThat(event.permissionCode()).isEqualTo("ops.production_readiness.view");
        assertThat(event.businessType()).isEqualTo("production_readiness_item");
        assertThat(event.businessId()).isEqualTo("all");
        assertThat(event.metadata()).containsEntry("count", 12);
        assertThat(repository.events).containsExactly(event);
    }

    @Test
    void recordsFailureEventWithErrorMetadata() {
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
        AuditEventService service = new AuditEventService(repository);

        AuditEvent event = service.recordFailure(
                "production_readiness.update_failed",
                "ops.production_readiness.manage",
                "production_readiness_item",
                "mdm-device-enrollment",
                "request-2",
                "update-1",
                "SECRET_VALUE_REJECTED"
        );

        assertThat(event.eventName()).isEqualTo("production_readiness.update_failed");
        assertThat(event.eventResult()).isEqualTo("failure");
        assertThat(event.idempotencyKey()).isEqualTo("update-1");
        assertThat(event.metadata()).containsEntry("error_code", "SECRET_VALUE_REJECTED");
        assertThat(repository.events).containsExactly(event);
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
