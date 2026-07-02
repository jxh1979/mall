-- M4:清商户汇总/健康分/服务分三表的 V017 演示种子(冒真数据)。
-- 商户经营汇总由 ETL 真管道接管(每商户一行最新态);健康分/服务分按 2026-07-02 用户裁决
-- 暂不做打分口径,保持前端未启用,清掉演示行使读端点返回真实空集。
DELETE FROM analytics_merchant_summaries;
DELETE FROM analytics_merchant_health_scores;
DELETE FROM analytics_staff_service_scores;
