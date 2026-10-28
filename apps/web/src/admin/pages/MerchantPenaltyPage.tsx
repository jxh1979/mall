// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 第十八批 C 段第 62 条 商户处罚记录（ADM-GOV-010）。BSM 第 1838 行「由城市项目公司的运营人员填写处罚记录，处罚即扣减健康分，
// 扣分规则由后台配置（业务参数，按项目可配，须同时配置上下限）」；第十八批裁决 8「只配单次扣分上下限（按项目可配），
// 运营登记处罚时自己填扣分」。后端端点：
//   GET  /api/v1/merchant-penalties?project_id=        本项目单次扣分上下限＋处罚记录
//   PUT  /api/v1/merchant-penalty-bounds/{project_id}   配置单次扣分上下限
//   POST /api/v1/merchant-penalties                     登记处罚
type Penalty = { id: string; merchantId: string; penaltyPoints: number; reason: string; recordedBy: string; recordedAt: string };
type PenaltyList = { minPenaltyPoints?: number | null; maxPenaltyPoints?: number | null; records?: Penalty[] };

const RECORD = ["merchant.penalty.record"];
const BOUNDS = ["merchant.penalty.bounds.manage"];

function errorText(err: unknown, fallback: string): string {
  return err instanceof Error ? err.message : fallback;
}

function wholeNumber(value: string): number | null {
  const n = Number(value);
  return value.trim() !== "" && Number.isInteger(n) && n >= 1 ? n : null;
}

export function MerchantPenaltyPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [projectId, setProjectId] = React.useState("");
  const [data, setData] = React.useState<PenaltyList | null>(null);
  const [minPoints, setMinPoints] = React.useState("");
  const [maxPoints, setMaxPoints] = React.useState("");
  const [merchantId, setMerchantId] = React.useState("");
  const [points, setPoints] = React.useState("");
  const [reason, setReason] = React.useState("");
  const project = projectId.trim();

  async function load() {
    if (!project) { setGlobalError("请先填写综合体项目 id"); return; }
    try {
      const result = await api.get<PenaltyList>(`/api/v1/merchant-penalties?project_id=${encodeURIComponent(project)}`);
      setData(result ?? {});
      setMinPoints(result?.minPenaltyPoints == null ? "" : String(result.minPenaltyPoints));
      setMaxPoints(result?.maxPenaltyPoints == null ? "" : String(result.maxPenaltyPoints));
    } catch (err) {
      setGlobalError(errorText(err, "商户处罚记录读取失败"));
    }
  }

  async function saveBounds() {
    const min = wholeNumber(minPoints);
    const max = wholeNumber(maxPoints);
    if (!project) { setGlobalError("请先填写综合体项目 id"); return; }
    if (min === null || max === null || min > max) {
      setGlobalError("单次扣分下限与上限须同时填写（整数，下限不小于 1 且不大于上限）");
      return;
    }
    try {
      await api.put(`/api/v1/merchant-penalty-bounds/${encodeURIComponent(project)}`, { minPenaltyPoints: min, maxPenaltyPoints: max });
      setActionResult({ action: "merchant_penalty_bounds_saved", projectId: project, minPenaltyPoints: min, maxPenaltyPoints: max });
      void load();
    } catch (err) {
      setGlobalError(errorText(err, "单次扣分上下限保存失败"));
    }
  }

  async function submit() {
    const value = wholeNumber(points);
    if (!project || !merchantId.trim() || value === null || !reason.trim()) {
      setGlobalError("请填写项目、商户、扣分（正整数）与违规说明");
      return;
    }
    try {
      await api.post("/api/v1/merchant-penalties", { projectId: project, merchantId: merchantId.trim(), penaltyPoints: value, reason: reason.trim() });
      setActionResult({ action: "merchant_penalty_recorded", merchantId: merchantId.trim(), penaltyPoints: value, reason: reason.trim() });
      setPoints("");
      setReason("");
      void load();
    } catch (err) {
      setGlobalError(errorText(err, "商户处罚登记失败"));
    }
  }

  const configured = data?.minPenaltyPoints != null;
  return (
    <section data-testid="merchant-penalty-page">
      <AdminPageHeader
        title="商户处罚记录"
        description="商户违反运营规则的，由项目运营人员登记处罚记录，每条处罚的扣分即对商户健康分的一次扣减。扣分由登记人填写，须在本项目配置的单次扣分上下限之内；本项目未配置上下限前不能登记。健康分具体计分与优惠规则待商场运营规则出台后另定。"
      />
      <div className="admin-form-grid">
        <label>项目 id<input aria-label="处罚记录项目 id" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <button type="button" data-testid="merchant-penalty-query" onClick={() => void load()}>查询</button>
        <span className="admin-muted-action" data-testid="merchant-penalty-bounds-current">
          {data === null ? "未查询" : configured
            ? `当前单次扣分：下限 ${data.minPenaltyPoints}、上限 ${data.maxPenaltyPoints}`
            : "本项目未配置单次扣分上下限，不能登记处罚"}
        </span>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>单次扣分下限<input aria-label="单次扣分下限" type="number" min="1" value={minPoints} onChange={(e) => setMinPoints(e.target.value)} /></label>
        <label>单次扣分上限<input aria-label="单次扣分上限" type="number" min="1" value={maxPoints} onChange={(e) => setMaxPoints(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={BOUNDS}
          data-testid="merchant-penalty-bounds-save" onClick={() => void saveBounds()}>保存上下限</PermissionButton>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>商户 id<input aria-label="处罚商户 id" value={merchantId} onChange={(e) => setMerchantId(e.target.value)} /></label>
        <label>扣分<input aria-label="扣分" type="number" min="1" value={points} onChange={(e) => setPoints(e.target.value)} /></label>
        <label>违规说明<input aria-label="违规说明" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={RECORD}
          data-testid="merchant-penalty-submit" onClick={() => void submit()}>登记处罚</PermissionButton>
      </div>
      <div className="admin-table-scroll" style={{ marginTop: 8 }}>
        <table className="admin-table" data-testid="merchant-penalty-table">
          <thead>
            <tr><th>时间</th><th>商户</th><th>扣分</th><th>违规说明</th><th>填写人</th></tr>
          </thead>
          <tbody>
            {(data?.records ?? []).map((row) => (
              <tr key={row.id} data-testid={`merchant-penalty-${row.id}`}>
                <td>{new Date(row.recordedAt).toLocaleString("zh-CN")}</td><td>{row.merchantId}</td><td>{row.penaltyPoints}</td>
                <td>{row.reason}</td><td>{row.recordedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
