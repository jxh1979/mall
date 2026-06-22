package com.complex.platform.finance;

import java.time.Instant;
import java.util.List;

public record ArApLedgerExportRecord(
        String id,
        String format,
        int rowCount,
        List<ArApItemRecord> rows,
        String csvText,
        Instant exportedAt
) {
}
