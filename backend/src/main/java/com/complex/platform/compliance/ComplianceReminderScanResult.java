package com.complex.platform.compliance;

import java.util.List;

public record ComplianceReminderScanResult(
        int generatedCount,
        List<ComplianceReminderRecord> reminders
) {
}
