package com.complex.platform.ops.productionreadiness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ProductionReadinessStatus {
    NOT_STARTED("not_started", true),
    PENDING_INFO("pending_info", true),
    IN_PROGRESS("in_progress", true),
    BLOCKED("blocked", true),
    READY_FOR_TEST("ready_for_test", true),
    PASSED("passed", false),
    FAILED("failed", true),
    WAIVED("waived", false),
    NOT_APPLICABLE("not_applicable", false);

    private final String code;
    private final boolean blocking;

    ProductionReadinessStatus(String code, boolean blocking) {
        this.code = code;
        this.blocking = blocking;
    }

    @JsonValue
    public String code() {
        return code;
    }

    public boolean isBlocking() {
        return blocking;
    }

    @JsonCreator
    public static ProductionReadinessStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown production readiness status: " + code));
    }
}
