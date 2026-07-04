import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type AiThresholds = { localLlmMinConfidence?: number | string; benchmarkDeviation?: number | string };

const MANAGE = ["analytics.operating_params.manage"];

/**
 * AI/对标阈值配置页(ADM-ANA-010, V218):维护两阈值——
 * AI建议自动采纳最低置信度(低于转人工复核)、集团对标偏离告警线(低于同类均值该比例记「低于对标」)。
 * 阈值变更改配置不改代码;仅 analytics.operating_params.manage 可改。
 */
export function AiThresholdsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [current, setCurrent] = React.useState<AiThresholds | null>(null);
  const [confEdit, setConfEdit] = React.useState("");
  const [devEdit, setDevEdit] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const cfg = await api.get<AiThresholds>("/api/v1/analytics/ai-thresholds");
      setCurrent(cfg ?? null);
      setConfEdit(cfg?.localLlmMinConfidence === undefined ? "" : String(Number(cfg.localLlmMinConfidence) * 100));
      setDevEdit(cfg?.benchmarkDeviation === undefined ? "" : String(Number(cfg.benchmarkDeviation) * 100));
    },
    { onError: setGlobalError, fallbackMessage: "AI/对标阈值读取失败" },
  );

  async function save() {
    const conf = Number(confEdit) / 100;
    const dev = Number(devEdit) / 100;
    if (!Number.isFinite(conf) || !Number.isFinite(dev) || conf < 0 || conf > 1 || dev < 0 || dev > 1) {
      setGlobalError("两个阈值都按百分比填写(0-100),如置信度 60、对标偏离 20");
      return;
    }
    await api.post("/api/v1/analytics/ai-thresholds", { localLlmMinConfidence: conf, benchmarkDeviation: dev });
    setActionResult({ action: "ai_thresholds_saved", localLlmMinConfidence: conf, benchmarkDeviation: dev });
    reload();
  }

  return (
    <section data-testid="ai-thresholds-page">
      <AdminPageHeader
        title="AI/对标阈值"
        description="维护 AI异常建议自动采纳的最低置信度(低于此转人工复核)与集团下钻对标偏离告警线(低于同类均值该比例记「低于对标」)。均按百分比填写;改配置不改代码。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="ai-thresholds-current">
          <div className="admin-kpi-card"><span>AI采纳最低置信度</span><strong data-testid="ai-threshold-confidence">{current?.localLlmMinConfidence == null ? "—" : (Number(current.localLlmMinConfidence) * 100).toFixed(0) + "%"}</strong></div>
          <div className="admin-kpi-card"><span>对标偏离告警线</span><strong data-testid="ai-threshold-deviation">{current?.benchmarkDeviation == null ? "—" : (Number(current.benchmarkDeviation) * 100).toFixed(0) + "%"}</strong></div>
        </div>
        <div className="admin-inline-form" data-testid="ai-thresholds-form">
          <input aria-label="AI采纳最低置信度(%)" type="number" min="0" max="100" placeholder="置信度(%)如 60" value={confEdit} onChange={(e) => setConfEdit(e.target.value)} />
          <input aria-label="对标偏离告警线(%)" type="number" min="0" max="100" placeholder="对标偏离(%)如 20" value={devEdit} onChange={(e) => setDevEdit(e.target.value)} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ai-thresholds-save" onClick={() => void save()}>
            保存阈值
          </PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
