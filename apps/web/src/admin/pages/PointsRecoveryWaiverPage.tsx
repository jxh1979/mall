// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type Line = { rewardId: string; projectId: string; originalPoints: number; remainingPoints: number; status: string };
type Waiver = { id: string; waivedPoints: number; actorId: string; reason: string; createdAt: string; configVersionId: string };
type Detail = { customerId: string; configVersionId: string; threshold: number; pendingPoints: number; lines: Line[]; waivers: Waiver[] };
const labels: Record<string, string> = { pending: "待追回", recovered: "已抵扣", waived: "已豁免", cancelled: "注销作废" };

export function PointsRecoveryWaiverPage({ api, effectivePermissions, setActionResult, setGlobalError }: AdminPageProps) {
  const [customer, setCustomer] = React.useState("");
  const [detail, setDetail] = React.useState<Detail | null>(null);
  const [reason, setReason] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const attempt = React.useRef<{ signature: string; key: string } | null>(null);
  async function load(id: string) {
    const value = await api.get<Detail>(`/api/v1/finance/points-recoveries/${encodeURIComponent(id)}`);
    setDetail(value);
  }
  async function action(run: () => Promise<void>) {
    if (busy) return;
    setBusy(true); setError("");
    try { await run(); }
    catch (e) { const message = e instanceof Error ? e.message : "操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function waive() {
    if (!detail || !reason.trim()) throw new Error("请填写财务豁免原因");
    const body = { configVersionId: detail.configVersionId, expectedPendingPoints: detail.pendingPoints, reason: reason.trim() };
    const signature = JSON.stringify([detail.customerId, body]);
    if (attempt.current?.signature !== signature) attempt.current = { signature, key: crypto.randomUUID() };
    const result = await api.post<Waiver>(`/api/v1/finance/points-recoveries/${encodeURIComponent(detail.customerId)}/waive`,
      { ...body, idempotencyKey: attempt.current.key });
    setActionResult(result); attempt.current = null; setReason("");
    await load(detail.customerId);
  }
  return <section data-testid="points-recovery-waiver-page">
    <AdminPageHeader title="积分追回与财务豁免" description="按会员全部待追回积分判断豁免资格。原追回记录永久保留；豁免不增加积分余额，也不改变积分发行费。" />
    {error && <p role="alert">{error}</p>}
    <div className="admin-form-row">
      <label>会员编号<input aria-label="会员编号" value={customer} disabled={busy} onChange={e => { setCustomer(e.target.value); setDetail(null); setReason(""); }} /></label>
      <PermissionButton permissionsState={effectivePermissions} permissions={["finance.points_recovery.view"]} disabled={busy || !customer.trim()} onClick={() => void action(() => load(customer.trim()))}>查看全部追回记录</PermissionButton>
    </div>
    {detail && <>
      <p>会员 {detail.customerId}：全部待追回 {detail.pendingPoints} 积分；已生效豁免线 {detail.threshold} 积分。</p>
      <table className="admin-table"><thead><tr><th>原奖励记录</th><th>综合体</th><th>原待追回</th><th>剩余</th><th>处理状态</th></tr></thead><tbody>
        {detail.lines.map(line => <tr key={line.rewardId}><td>{line.rewardId}</td><td>{line.projectId}</td><td>{line.originalPoints}</td><td>{line.remainingPoints}</td><td>{labels[line.status] ?? line.status}</td></tr>)}
      </tbody></table>
      <label>财务豁免原因<input aria-label="财务豁免原因" value={reason} disabled={busy} onChange={e => setReason(e.target.value)} /></label>
      <PermissionButton permissionsState={effectivePermissions} permissions={["finance.points_recovery.waive"]} disabled={busy || !reason.trim() || detail.pendingPoints <= 0 || detail.pendingPoints > detail.threshold} onClick={() => void action(waive)}>豁免全部 {detail.pendingPoints} 积分</PermissionButton>
      {detail.pendingPoints > detail.threshold && <p>总额超过豁免线，不允许选择部分记录豁免。</p>}
      <h3>已办理的财务豁免</h3>
      <table className="admin-table"><thead><tr><th>办理时间</th><th>积分</th><th>财务经办人</th><th>原因</th><th>配置版本记录</th></tr></thead><tbody>
        {detail.waivers.map(row => <tr key={row.id}><td>{new Date(row.createdAt).toLocaleString("zh-CN")}</td><td>{row.waivedPoints}</td><td>{row.actorId}</td><td>{row.reason}</td><td>{row.configVersionId}</td></tr>)}
      </tbody></table>
    </>}
  </section>;
}
