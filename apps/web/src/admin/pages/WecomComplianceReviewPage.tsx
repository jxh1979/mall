// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField, cnDate } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

// 企微合规域内枚举中文（风险等级/复核处置/留存到期动作/scope，页内映射；状态码走全局表兜底）。
const RISK_LEVEL_LABELS: Record<string, string> = { high: "高", medium: "中", low: "低" };
const REVIEW_STATUS_LABELS: Record<string, string> = { reviewing: "领取复核", confirmed: "确认违规", dismissed: "误报驳回" };
const EXPIRY_ACTION_LABELS: Record<string, string> = { permanent: "永久保存，不自动处理" };
const SCOPE_LABELS: Record<string, string> = { global: "全局", project: "综合体", merchant: "商户" };
const wLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? code : "-");
const yn = (v: boolean | undefined) => (v ? "启用" : "停用");

// 后端 compliance 域企微记录映射（接现有 /api/v1/compliance/wecom/* 契约，客户端已 snake→camel）。
type RiskHit = { hitId: string; conversationId?: string; messageId?: string; ruleCode?: string; riskLevel?: string; matchedSnippet?: string; status?: string; reviewerId?: string; reviewNote?: string; projectId?: string };
type RetentionPolicy = { id: string; scopeType?: string; scopeId?: string; retentionDays?: number | null; expiryAction?: string; enabled?: boolean; updatedAt?: string };

type CleanupCandidate = { messageId: string; conversationId: string; projectId: string; sentAt?: string; reason: string; riskHits: { hitId: string; ruleCode: string; status: string }[] };
type CleanupPreview = { id: string; candidates: CleanupCandidate[]; scopeDigest: string; messageCount: number; riskHitCount: number; createdAt: string; executedAt?: string; executedBy?: string; keyFingerprint?: string };
type CleanupChallenge = { challenge: string; expiresAt: string };
type WecomData = { riskHits: RiskHit[]; policies: RetentionPolicy[]; cleanups: CleanupPreview[] };

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
 * ADM-COMP-002 企微会话合规监察：企微违规命中复核处置 + 永久保存与逐次授权清理。
 * 违规命中一条 = 一条消息 × 一个违规规则码（脱敏片段展示，不含明文 PII）；
 * 复核流转 open（待复核）→ reviewing（领取）→ confirmed（确认违规）/ dismissed（误报驳回）。
 * 永久保存只读；手动清理逐条填理由，先预览，再由最高权限本人使用当次 U 盘签名执行。
 * 平台合规口径 fail-closed：后端已拒绝 merchant 越权调阅；某读接口权限不足时 allSettled 局部降级。
 */
export function WecomComplianceReviewPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<WecomData | null>(null);
  const [selectedHitId, setSelectedHitId] = React.useState("");
  const [reviewForm, setReviewForm] = React.useState({ status: "reviewing", reviewNote: "" });
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const isMaster = user.id === "usr_master_authority" && user.accountType === "master_authority";
  const [cleanupItems, setCleanupItems] = React.useState([{ messageId: "", reason: "" }]);
  const [cleanupPreview, setCleanupPreview] = React.useState<CleanupPreview | null>(null);
  const [cleanupChallenge, setCleanupChallenge] = React.useState<CleanupChallenge | null>(null);
  const [signature, setSignature] = React.useState("");
  const [cleanupError, setCleanupError] = React.useState("");
  const [cleanupBusy, setCleanupBusy] = React.useState(false);
  const invalidatePreview = () => { setCleanupPreview(null); setCleanupChallenge(null); setSignature(""); setCleanupError(""); };

  const { loading, error, reload } = useAdminResource(
    async () => {
      const settled = await Promise.allSettled([...wecomApiPaths, ...(isMaster ? ["/api/v1/compliance/wecom/cleanup-previews"] : [])].map((path) => api.get<unknown>(path)));
      const at = (index: number): unknown => {
        const outcome = settled[index];
        return outcome && outcome.status === "fulfilled" ? outcome.value : [];
      };
      setData({ riskHits: asRows<RiskHit>(at(0)), policies: asRows<RetentionPolicy>(at(1)), cleanups: asRows<CleanupPreview>(at(2)) });
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

  const createCleanupPreview = async () => {
    if (cleanupItems.some((item) => !item.messageId.trim() || !item.reason.trim())) {
      setCleanupError("每条消息都必须填写消息编号和清理理由"); return;
    }
    setCleanupBusy(true); setCleanupError(""); setCleanupChallenge(null); setSignature("");
    try {
      setCleanupPreview(await api.post<CleanupPreview>("/api/v1/compliance/wecom/cleanup-previews", {
        items: cleanupItems.map((item) => ({ message_id: item.messageId.trim(), reason: item.reason.trim() })),
      }));
      reload();
    } catch (e) { setCleanupError((e as Error).message); } finally { setCleanupBusy(false); }
  };

  const requestCleanupChallenge = async () => {
    if (!cleanupPreview) return;
    setCleanupBusy(true); setCleanupError(""); setSignature(""); setCleanupChallenge(null);
    try {
      setCleanupChallenge(await api.post<CleanupChallenge>(`/api/v1/compliance/wecom/cleanup-previews/${cleanupPreview.id}/challenge`));
    } catch (e) { setCleanupError((e as Error).message); } finally { setCleanupBusy(false); }
  };

  const executeCleanup = async () => {
    if (!cleanupPreview || !cleanupChallenge || !signature.trim()) return;
    setCleanupBusy(true); setCleanupError("");
    try {
      const result = await api.post<CleanupPreview>(`/api/v1/compliance/wecom/cleanup-previews/${cleanupPreview.id}/execute`, {
        challenge: cleanupChallenge.challenge, signature: signature.trim(),
      });
      setCleanupPreview(result); setCleanupChallenge(null); setSignature("");
      setActionResult({ action: "wecom_cleanup_executed", previewId: result.id, messageCount: result.messageCount });
      reload();
    } catch (e) { setCleanupError((e as Error).message); } finally { setCleanupBusy(false); }
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
        { label: "保留天数", value: "永久保存" },
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
      <AdminPageHeader title="企微会话合规监察" description="企微会话永久保存，违规命中按权限复核；清理须最高权限本人逐次授权。命中片段已脱敏展示，原文调阅另需高敏授权。" />
      <AuditTrailHint event="wecom.risk.hit_reviewed / wecom.cleanup_executed" />
      <TechDetails><div className="admin-generic-evidence" data-testid="wecom-compliance-evidence">/api/v1/compliance/wecom</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="wecom-compliance-summary">
          <div className="admin-kpi-card"><span>违规命中</span><strong data-testid="wecom-risk-hit-count">{riskHits.length}</strong><small>待复核 {openCount}</small></div>
          <div className="admin-kpi-card"><span>会话留存</span><strong>永久保存</strong></div>
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
            <thead><tr><th>选择</th><th>规则码</th><th>风险等级</th><th>命中片段（脱敏）</th><th>状态</th><th>复核人</th><th>详情</th></tr></thead>
            <tbody>
              {riskHits.slice(0, 100).map((row) => (
                <tr key={row.hitId} className="admin-row-clickable" tabIndex={0} data-testid={`wecom-risk-hit-row-${row.hitId}`}
                  onClick={() => openHitDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openHitDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此风险命中 ${row.hitId}`} data-testid={`wecom-risk-hit-select-${row.hitId}`} checked={selectedHitId === row.hitId} onChange={() => setSelectedHitId(row.hitId)} /></td>
                  <td>{row.ruleCode ?? "-"}</td><td>{wLabel(RISK_LEVEL_LABELS, row.riskLevel)}</td><td>{row.matchedSnippet ?? "-"}</td>
                  <td>{formatBusinessStatus(row.status)}</td><td>{row.reviewerId ?? "-"}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看违规命中详情" onClick={(event) => { event.stopPropagation(); openHitDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>会话留存规则</h3>
        <p>永久保存，不自动到期归档、删除或匿名化；此规则不可修改。</p>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="wecom-retention-policies-table">
            <thead><tr><th>适用范围</th><th>范围 ID</th><th>保留天数</th><th>到期动作</th><th>启用</th><th>详情</th></tr></thead>
            <tbody>
              {policies.slice(0, 100).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`wecom-retention-policy-row-${row.id}`}
                  onClick={() => openPolicyDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
                  <td>{wLabel(SCOPE_LABELS, row.scopeType)}</td><td>{row.scopeId ?? "-"}</td><td>永久</td>
                  <td>{wLabel(EXPIRY_ACTION_LABELS, row.expiryAction)}</td><td>{yn(row.enabled)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看留存策略详情" onClick={(event) => { event.stopPropagation(); openPolicyDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
        {isMaster && <section aria-label="企微消息手动清理">
          <h3>手动清理</h3>
          <p>逐条填写消息编号和理由，先核对预览。仅清理所选消息及预览中列出的关联命中，其他业务记录与清理证据永久保留。</p>
          {cleanupItems.map((item, index) => <div className="admin-form-row" key={index}>
            <label>消息编号 {index + 1}<input aria-label={`消息编号 ${index + 1}`} value={item.messageId} disabled={cleanupBusy}
              onChange={(e) => { invalidatePreview(); setCleanupItems(cleanupItems.map((row, i) => i === index ? { ...row, messageId: e.target.value } : row)); }} /></label>
            <label>清理理由 {index + 1}<input aria-label={`清理理由 ${index + 1}`} value={item.reason} disabled={cleanupBusy}
              onChange={(e) => { invalidatePreview(); setCleanupItems(cleanupItems.map((row, i) => i === index ? { ...row, reason: e.target.value } : row)); }} /></label>
            {cleanupItems.length > 1 && <button type="button" disabled={cleanupBusy} onClick={() => { invalidatePreview(); setCleanupItems(cleanupItems.filter((_, i) => i !== index)); }}>移除此条</button>}
          </div>)}
          <button type="button" disabled={cleanupBusy} onClick={() => { invalidatePreview(); setCleanupItems([...cleanupItems, { messageId: "", reason: "" }]); }}>增加一条消息</button>
          <PermissionButton permissionsState={effectivePermissions} permissions={RETENTION_MANAGE} disabled={cleanupBusy}
            onClick={() => void createCleanupPreview()}>预览清理范围</PermissionButton>
          {cleanupError && <p role="alert">{cleanupError}</p>}
          {cleanupPreview && <div data-testid="wecom-cleanup-preview">
            <h4>预览：{cleanupPreview.messageCount} 条消息、{cleanupPreview.riskHitCount} 条关联命中</h4>
            <p>预览编号：{cleanupPreview.id}；生成时间：{cnDate(cleanupPreview.createdAt)}</p>
            <table className="admin-table"><thead><tr><th>消息</th><th>会话</th><th>综合体</th><th>发送时间</th><th>清理理由</th><th>关联命中</th></tr></thead>
              <tbody>{cleanupPreview.candidates.map((row) => <tr key={row.messageId}><td>{row.messageId}</td><td>{row.conversationId}</td><td>{row.projectId}</td><td>{cnDate(row.sentAt)}</td><td>{row.reason}</td>
                <td>{row.riskHits.map((hit) => `${hit.hitId}（${hit.ruleCode}，${formatBusinessStatus(hit.status)}）`).join("；") || "无"}</td></tr>)}</tbody></table>
            {cleanupPreview.executedAt ? <p role="status">已清理：{cnDate(cleanupPreview.executedAt)}；执行人：{cleanupPreview.executedBy}；授权钥匙指纹：{cleanupPreview.keyFingerprint}</p> : <>
              <PermissionButton permissionsState={effectivePermissions} permissions={RETENTION_MANAGE} disabled={cleanupBusy}
                onClick={() => void requestCleanupChallenge()}>已核对范围，获取本次授权码</PermissionButton>
              {cleanupChallenge && <>
                <p>请插入授权 U 盘，用既有本地签名工具签署下方完整授权码，再粘贴签名。私钥和口令留在本地。授权到期：{cnDate(cleanupChallenge.expiresAt)}。</p>
                <label>本次授权码<textarea readOnly value={cleanupChallenge.challenge} /></label>
                <label>本次签名<textarea value={signature} disabled={cleanupBusy} onChange={(e) => setSignature(e.target.value)} /></label>
                <PermissionButton permissionsState={effectivePermissions} permissions={RETENTION_MANAGE} disabled={cleanupBusy || !signature.trim()}
                  onClick={() => void executeCleanup()}>使用本次签名执行清理</PermissionButton>
              </>}
            </>}
          </div>}
          <h4>永久保存的清理记录</h4>
          <ul>{(data?.cleanups ?? []).map((row) => <li key={row.id}><button type="button" disabled={cleanupBusy}
            onClick={() => { setCleanupPreview(row); setCleanupChallenge(null); setSignature(""); setCleanupError(""); }}>
            {cnDate(row.createdAt)}：{row.messageCount} 条消息、{row.riskHitCount} 条命中，{row.executedAt ? "已清理" : "待授权"}</button></li>)}</ul>
        </section>}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
