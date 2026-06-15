package com.complex.platform.ticketing;

import java.util.List;

public record SyncTasksResponse(
        String syncedAt,
        List<SyncTaskResult> results
) {
}
