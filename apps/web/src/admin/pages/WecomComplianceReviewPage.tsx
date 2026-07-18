import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField, cnDate } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminPageProps } from "../types";

// 企微合规域内枚举中文（风险等级/复核处置/留存到期动作/scope，页内映射；状态码走全局表兜底）。
const RISK_LEVEL_LABELS: Record<string, string> = { high: "高", medium: "中", low: "低" };
const REVIEW_STATUS_LABELS: Record<string, string> = { reviewing: "领取复核", confirmed: "确认违规", dismissed: "误报驳回" };
const EXPIRY_ACTION_LABELS: Record<string, string> = { archive: "归档", delete: "删除", anonymize: "匿名化" };
const SCOPE_LABELS: Record<string, string> = { global: "全局", project: "综合体", merchant: "商户" };
const wLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? code : "-");
const yn = (v: boolean | undefined) => (v ? "启用" : "停用");

// 后端 compliance 域企微记录映射（接现有 /api/v1/compliance/wecom/* 契约，客户端已 snake→camel）。
type RiskHit = { hitId: string; conversationId?: string; messageId?: string; ruleCode?: string; riskLevel?: string; matchedSnippet?: string; status?: string; reviewerId?: string; reviewNote?: string; projectId?: string };
type RetentionPolicy = { id: string; scopeType?: string; scopeId?: string; retentionDays?: number; expiryAction?: string; enabled?: boolean; updatedAt?: string };

type WecomData = { riskHits: RiskHit[]; policies: RetentionPolicy[] };

const RISK_REVIEW = ["wecom.risk_hit.review"];
const RETENTION_MANAGE = ["wecom.retention.manage"];

const wecomApiPaths = [
  "/api/v1/compliance/wecom/risk-hits",
  "/api/v1/compliance/wecom/retention-policies",
];

/** PageResponse / 裸数组 双兼容取行。 */
function asRows<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  const items = (value as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/**
 * ADM-COMP-002 企微会话合规监察：企微违规命中复核处置 + 会话留存策略配置。
 * 违规命中一条 = 一条消息 × 一个违规规则码（脱敏片段展示，不含明文 PII）；
 * 复核流转 open（待复核）→ reviewing（领取）→ confirmed（确认违规）/ dismissed（误报驳回）。
 * 留存策略 = scope + 保留天数 + 到期动作（归档/删除/匿名化）。两表整行可点 → 行明细抽屉。
 * 平台合规口径 fail-closed：后端已拒绝 merchant 越权调阅；某读接口权限不足时 allSettled 局部降级。
 */
export function WecomComplianceReviewPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<WecomData | null>(null);
  const [selectedHitId, setSelectedHitId] = React.useState("");
  const [reviewForm, setReviewForm] = React.useState({ status: "reviewing", reviewNote: "" });
  const [policyForm, setPolicyForm] = React.useState({ scopeType: "global", scopeId: "", retentionDays: "180", expiryAction: "archive", enabled: true });
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式「新增留存策略」弹窗状态（范式：字段留页面持有，弹窗只编排；风险命中复核动作不动）。
  const [policyOpen, setPolicyOpen] = React.useState(false);
  const [policyError, setPolicyError] = React.useState("");
  const [policySaving, setPolicySaving] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const settled = await Promise.allSettled(wecomApiPaths.map((path) => api.get<unknown>(path)));
      const at = (index: number): unknown => {
        const outcome = settled[index];
        return outcome && outcome.status === "fulfilled" ? outcome.value : [];
      };
      setData({ riskHits: asRows<RiskHit>(at(0)), policies: asRows<RetentionPolicy>(at(1)) });
    },
    { onError: setGlobalError, fallbackMessage: "企微合规监察读取失败", onStart: () => setGlobalError(null) },
  );

  const reviewHit = async () => {
    if (!selectedHitId) { setGlobalError("请先选择一条违规命中再提交复核"); return; }
    try {
      await api.post(`/api/v1/compliance/wecom/risk-hits/${selectedHitId}/review`, {
        status: reviewForm.status, review_note: reviewForm.reviewNote || null,
      });
      setActionResult({ action: "wecom_risk_hit_reviewed", hitId: selectedHitId, status: reviewForm.status });
      reload();
    } catch (e) { setGlobalError((e as Error).message); }
  };

  // 提交在确认步触发；接口失败弹窗留开显错（policyError），成功才关。原接口/请求体保留。
  const upsertPolicy = async () => {
    setPolicySaving(true);
    setPolicyError("");
    setGlobalError(null);
    try {
      await api.post("/api/v1/compliance/wecom/retention-policies", {
        idempotency_key: `wrp-${Date.now()}`, scope_type: policyForm.scopeType, scope_id: policyForm.scopeId || null,
        retention_days: Number(policyForm.retentionDays) || 180, expiry_action: policyForm.expiryAction, enabled: policyForm.enabled,
      });
      setActionResult({ action: "wecom_retention_policy_saved", scopeType: policyForm.scopeType });
      setPolicyOpen(false);
      reload();
    } catch (e) { setPolicyError((e as Error).message); } finally { setPolicySaving(false); }
  };

  function openHitDetail(row: RiskHit) {
    setDetail({
      title: `违规命中 · ${row.ruleCode ?? row.hitId}`,
      fields: [
        { label: "违规规则码", value: row.ruleCode ?? "—" },
        { label: "风险等级", value: wLabel(RISK_LEVEL_LABELS, row.riskLevel) },
        { label: "命中片段（脱敏）", value: row.matchedSnippet ?? "—" },
        { label: "会话", value: row.conversationId ?? "—" },
        { label: "消息", value: row.messageId ?? "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "复核人", value: row.reviewerId ?? "—" },
        { label: "复核备注", value: row.reviewNote ?? "—" },
        { label: "所属综合体", value: row.projectId ?? "—" },
      ],
    });
  }

  function openPolicyDetail(row: RetentionPolicy) {
    setDetail({
      title: `留存策略 · ${wLabel(SCOPE_LABELS, row.scopeType)}`,
      fields: [
        { label: "适用范围", value: `${wLabel(SCOPE_LABELS, row.scopeType)}${row.scopeId ? `：${row.scopeId}` : ""}` },
        { label: "保留天数", value: `${row.retentionDays ?? "—"} 天` },
        { label: "到期动作", value: wLabel(EXPIRY_ACTION_LABELS, row.expiryAction) },
        { label: "启用", value: yn(row.enabled) },
        { label: "更新时间", value: cnDate(row.updatedAt) },
        { label: "策略编号", value: row.id },
      ],
    });
  }

  const riskHits = data?.riskHits ?? [];
  const policies = data?.policies ?? [];
  const openCount = riskHits.filter((h) => h.status === "open").length;

  return (
    <section data-testid="wecom-compliance-review-page">
      <AdminPageHeader title="企微会话合规监察" description="企微会话合规：违规命中复核处置（确认违规/误报驳回）与会话留存策略配置。命中片段已脱敏展示，原文调阅另需高敏授权。" />
      <AuditTrailHint event="compliance.wecom.risk_hit_reviewed / compliance.wecom.retention_policy_saved" />
      <TechDetails><div className="admin-generic-evidence" data-testid="wecom-compliance-evidence">/api/v1/compliance/wecom</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="wecom-compliance-summary">
          <div className="admin-kpi-card"><span>违规命中</span><strong data-testid="wecom-risk-hit-count">{riskHits.length}</strong><small>待复核 {openCount}</small></div>
          <div className="admin-kpi-card"><span>留存策略</span><strong>{policies.length}</strong></div>
        </div>

        <h3>违规命中复核</h3>
        <p className="admin-muted-action" data-testid="wecom-review-note">先选中一条命中，再选择处置结论提交复核；确认违规 / 误报驳回为高敏操作。</p>
        <div className="admin-form-grid" data-testid="wecom-review-form">
          <label>处置结论
            <select data-testid="wecom-review-status" value={reviewForm.status} onChange={(e) => setReviewForm({ ...reviewForm, status: e.target.value })}>
              {Object.entries(REVIEW_STATUS_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label>复核备注<input data-testid="wecom-review-note-input" value={reviewForm.reviewNote} onChange={(e) => setReviewForm({ ...reviewForm, reviewNote: e.target.value })} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={RISK_REVIEW} data-testid="wecom-review-submit" onClick={() => void reviewHit()}>提交复核</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="wecom-risk-hits-table">
            <thead><tr><th>选择</th><th>规则码</th><th>风险等级</th><th>命中片段（脱敏）</th><th>状态</th><th>复核人</th></tr></thead>
            <tbody>
              {riskHits.slice(0, 100).map((row) => (
                <tr key={row.hitId} className="admin-row-clickable" tabIndex={0} data-testid={`wecom-risk-hit-row-${row.hitId}`}
                  onClick={() => openHitDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openHitDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此风险命中 ${row.hitId}`} data-testid={`wecom-risk-hit-select-${row.hitId}`} checked={selectedHitId === row.hitId} onChange={() => setSelectedHitId(row.hitId)} /></td>
                  <td>{row.ruleCode ?? "-"}</td><td>{wLabel(RISK_LEVEL_LABELS, row.riskLevel)}</td><td>{row.matchedSnippet ?? "-"}</td>
                  <td>{formatBusinessStatus(row.status)}</td><td>{row.reviewerId ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>会话留存策略</h3>
        <div className="admin-form-row" data-testid="wecom-policy-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={RETENTION_MANAGE} data-testid="wecom-policy-create-open"
            onClick={() => { setPolicyError(""); setPolicyOpen(true); }}>＋ 新增留存策略</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="wecom-retention-policies-table">
            <thead><tr><th>适用范围</th><th>范围 ID</th><th>保留天数</th><th>到期动作</th><th>启用</th></tr></thead>
            <tbody>
              {policies.slice(0, 100).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`wecom-retention-policy-row-${row.id}`}
                  onClick={() => openPolicyDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
                  <td>{wLabel(SCOPE_LABELS, row.scopeType)}</td><td>{row.scopeId ?? "-"}</td><td>{row.retentionDays ?? "-"}</td>
                  <td>{wLabel(EXPIRY_ACTION_LABELS, row.expiryAction)}</td><td>{yn(row.enabled)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={policyOpen}
        title="新增留存策略"
        testId="wecom-policy-guided"
        submitTestId="wecom-policy-save"
        submitLabel="保存留存策略"
        submitting={policySaving}
        error={policyError || undefined}
        onClose={() => setPolicyOpen(false)}
        onSubmit={() => void upsertPolicy()}
        steps={[
          {
            title: "适用范围与天数",
            description: "选择留存策略适用范围（全局/综合体/商户）；非全局须填范围 ID；保留天数为正整数。",
            validate: () => {
              if (policyForm.scopeType !== "global" && !policyForm.scopeId.trim()) return "非全局范围须填写范围 ID";
              const days = Number(policyForm.retentionDays);
              if (!Number.isFinite(days) || days <= 0) return "保留天数须为正整数";
              return null;
            },
            content: (
              <>
                <label>适用范围
                  <select data-testid="wecom-policy-scope" value={policyForm.scopeType} onChange={(e) => setPolicyForm({ ...policyForm, scopeType: e.target.value })}>
                    {Object.entries(SCOPE_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                  </select>
                </label>
                <label>范围 ID（全局留空）<input data-testid="wecom-policy-scope-id" value={policyForm.scopeId} onChange={(e) => setPolicyForm({ ...policyForm, scopeId: e.target.value })} /></label>
                <label>保留天数<input data-testid="wecom-policy-days" value={policyForm.retentionDays} onChange={(e) => setPolicyForm({ ...policyForm, retentionDays: e.target.value })} /></label>
              </>
            ),
          },
          {
            title: "到期动作与启用",
            description: "到期动作：归档/删除/匿名化；可先停用再启用。",
            content: (
              <>
                <label>到期动作
                  <select data-testid="wecom-policy-action" value={policyForm.expiryAction} onChange={(e) => setPolicyForm({ ...policyForm, expiryAction: e.target.value })}>
                    {Object.entries(EXPIRY_ACTION_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                  </select>
                </label>
                <label>启用<input type="checkbox" data-testid="wecom-policy-enabled" checked={policyForm.enabled} onChange={(e) => setPolicyForm({ ...policyForm, enabled: e.target.checked })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "适用范围", value: `${SCOPE_LABELS[policyForm.scopeType] ?? policyForm.scopeType}${policyForm.scopeId.trim() ? `：${policyForm.scopeId.trim()}` : ""}` },
          { label: "保留天数", value: `${policyForm.retentionDays || "—"} 天` },
          { label: "到期动作", value: EXPIRY_ACTION_LABELS[policyForm.expiryAction] ?? policyForm.expiryAction },
          { label: "启用", value: policyForm.enabled ? "启用" : "停用" },
        ]}
      />
    </section>
  );
}
