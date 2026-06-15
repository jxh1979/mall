package com.complex.platform.analytics;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.DashboardSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import java.util.List;
import java.util.Optional;

public interface AnalyticsRepository {

    List<AiQualityResultRecord> listAiQualityResults();

    List<AiQualityTaskRecord> listAiQualityTasks();

    AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task);

    AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result);

    List<CustomerIntentRecord> listCustomerIntents();

    DashboardSnapshotRecord getP0OverviewDashboard();

    List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots();

    List<MerchantHealthScoreRecord> listMerchantHealthScores();

    List<MerchantSummaryRecord> listAnalyticsMerchants();

    List<MetricAnomalyRecord> listMetricAnomalies();

    List<MetricDefinitionRecord> listMetricDefinitions();

    List<RiskAlertRecord> listRiskAlerts();

    RiskAlertRecord saveRiskAlert(RiskAlertRecord alert);

    Optional<RiskAlertRecord> findRiskAlertById(String id);

    RiskAlertRecord updateRiskAlert(RiskAlertRecord alert);

    List<RiskRuleRecord> listRiskRules();

    List<RobotMetricRecord> listRobotMetrics();

    List<StaffServiceScoreRecord> listStaffServiceScores();
}
