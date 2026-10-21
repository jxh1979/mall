// 文件版本：V0.01
import React from "react";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { hasPermissions } from "../security/PermissionGate";
import { cnDate } from "../components/RowDetailDrawer";
import { formatBusinessStatus } from "../i18n/statusLabels";
import type { AdminApiClient, EffectivePermissionsState, TicketRefundRow, TicketSafetyEventRow } from "../types";

// TK-05（BSM:804「退款」、BSM:785「退款原则」、BSM:806「退款授权与留痕」）：普通退款五种情形 + 最高权限逐笔放行，
// 通票安全例外由夜班值班经理及以上批准（多次通票只恢复本次 1 次，其他通票免除该人份票款）。
export const REFUND_REASON_LABELS: Record<string, string> = {
  ticket_type_rule: "票种规则明确允许",
  event_cancelled: "活动或场次取消",
  service_unavailable: "平台或履约方无法提供服务",
  duplicate_charge: "重复扣款",
  legal_requirement: "法律法规要求",
  master_override: "五种情形之外（最高权限逐笔放行）",
  safety_exception: "通票安全例外",
};
const ORDINARY_REASONS = ["ticket_type_rule", "event_cancelled", "service_unavailable", "duplicate_charge", "legal_requirement", "master_override"];
const OUTCOME_LABELS: Record<string, string> = { refund: "免除该人份票款", restore: "恢复本次 1 次完整补偿使用机会" };
const BASIS_LABELS: Record<string, string> = { night_duty: "当班夜班值班经理", superior: "夜班值班经理的上级", master: "最高权限" };

type Props = {
  api: AdminApiClient;
  effectivePermissions: EffectivePermissionsState;
  selectedOrderId?: string;
  refunds: TicketRefundRow[];
  busy: boolean;
  runAction: (action: string, request: () => Promise<unknown>) => Promise<void>;
  setFormError: (message: string) => void;
};

function formatAmount(amountCent: number | undefined) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent ?? 0) / 100);
}

export function TicketRefundCasesPanel({ api, effectivePermissions, selectedOrderId, refunds, busy, runAction, setFormError }: Props) {
  const canRequest = hasPermissions(effectivePermissions, ["ticket.refund.request"]);
  const canApprove = hasPermissions(effectivePermissions, ["ticket.refund.approve"]);
  const canView = hasPermissions(effectivePermissions, ["ticket.refund.view"]);
  const [reasonCode, setReasonCode] = React.useState("");
  const [entityId, setEntityId] = React.useState("");
  const [instanceIds, setInstanceIds] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [decisionNote, setDecisionNote] = React.useState("");
  const [safety, setSafety] = React.useState({ instanceId: "", slot: "1", reason: "", situation: "", entityId: "" });
  const [events, setEvents] = React.useState<TicketSafetyEventRow[]>([]);

  const loadEvents = React.useCallback(async () => {
    if (!canView) return;
    try {
      setEvents(await api.get<TicketSafetyEventRow[]>("/api/v1/ticketing/safety-events"));
    } catch {
      setEvents([]);
    }
  }, [api, canView]);
  React.useEffect(() => { void loadEvents(); }, [loadEvents]);

  async function act(action: string, request: () => Promise<unknown>) {
    await runAction(action, request);
    await loadEvents();
  }

  function requestRefund() {
    if (!selectedOrderId) return setFormError("请先在订单表中选择要退款的订单。");
    if (!reasonCode) return setFormError("请选择退款理由类别（五种情形之一，或最高权限逐笔放行）。");
    if (!entityId.trim()) return setFormError("请填写退款责任主体。");
    if (!reason.trim()) return setFormError("请填写退款说明。");
    const ids = instanceIds.split(/[,，\s]+/).map((id) => id.trim()).filter(Boolean);
    void runAction("ticket_refund_requested", () => api.post("/api/v1/ticketing/refunds", {
      targetId: selectedOrderId,
      reasonCode,
      responsibleEntityId: entityId.trim(),
      ...(ids.length > 0 ? { ticketInstanceIds: ids } : {}),
      reason: reason.trim(),
    }));
  }

  function validateDecision() {
    if (!decisionNote.trim()) {
      setFormError("请先填写审批意见。");
      return false;
    }
    return true;
  }

  function requestSafetyEvent() {
    if (!safety.instanceId.trim() || !safety.reason.trim() || !safety.situation.trim() || !safety.entityId.trim()) {
      return setFormError("请填写票实例编号、原因、当时情形与责任主体。");
    }
    void act("ticket_safety_event_requested", () => api.post("/api/v1/ticketing/safety-events", {
      ticketInstanceId: safety.instanceId.trim(),
      slotIndex: Number(safety.slot) || 1,
      reason: safety.reason.trim(),
      situation: safety.situation.trim(),
      responsibleEntityId: safety.entityId.trim(),
    }));
  }

  const pendingRefunds = refunds.filter((refund) => refund.status === "requested");
  return (
    <div className="admin-section" data-testid="ticket-refund-cases-panel">
      <h3>退款发起与审批</h3>
      <p className="admin-muted-action">门票售出原则上不退款。普通退款只允许在完全未核销的整张票上，按五种情形之一办理；五种情形之外只能由持授权 U 盘密钥的最高权限逐笔放行（仍不解除已开始核销后的退款禁止）；申请人不得审批自己的申请。</p>
      {canRequest ? (
        <div className="admin-form-row" data-testid="ticket-refund-request-form">
          <select data-testid="ticket-refund-reason-code" aria-label="退款理由类别" value={reasonCode} onChange={(event) => setReasonCode(event.target.value)}>
            <option value="">请选择退款理由类别</option>
            {ORDINARY_REASONS.map((code) => <option key={code} value={code}>{REFUND_REASON_LABELS[code]}</option>)}
          </select>
          <input data-testid="ticket-refund-responsible-entity" aria-label="责任主体" placeholder="责任主体（法律主体编号）" value={entityId} onChange={(event) => setEntityId(event.target.value)} />
          <input data-testid="ticket-refund-instance-ids" aria-label="票实例编号" placeholder="票实例编号（多张用逗号分隔；不填＝该订单全部完全未使用的票）" value={instanceIds} onChange={(event) => setInstanceIds(event.target.value)} />
          <input data-testid="ticket-refund-reason" aria-label="退款说明" placeholder="退款说明" value={reason} onChange={(event) => setReason(event.target.value)} />
          <button type="button" data-testid="ticket-refund-request" disabled={busy} onClick={requestRefund}>发起退款</button>
        </div>
      ) : null}
      {canApprove ? (
        <div className="admin-form-row">
          <input data-testid="ticket-refund-decision-note" aria-label="审批意见" placeholder="审批意见（批准、驳回或批准安全事件时必填）" value={decisionNote} onChange={(event) => setDecisionNote(event.target.value)} />
        </div>
      ) : null}
      {pendingRefunds.length > 0 ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-refunds-pending-table">
            <thead><tr><th>退款单号</th><th>原订单</th><th>退款金额</th><th>理由类别</th><th>责任主体</th><th>申请人</th><th>操作</th></tr></thead>
            <tbody>
              {pendingRefunds.map((refund) => (
                <tr key={refund.id} data-testid={`ticket-refund-pending-row-${refund.id}`}>
                  <td>{refund.id}</td>
                  <td>{refund.targetId ?? "-"}</td>
                  <td>{formatAmount(refund.refundAmountCent)}</td>
                  <td>{refund.reasonCode ? REFUND_REASON_LABELS[refund.reasonCode] ?? refund.reasonCode : "—"}</td>
                  <td>{refund.responsibleEntityId ?? "—"}</td>
                  <td>{refund.requestedBy ?? "-"}</td>
                  <td>
                    {canApprove ? (
                      <>
                        <HighRiskConfirm label="批准退款" impact={refund.reasonCode === "master_override"
                          ? "五种情形之外的退款只能由持授权 U 盘密钥的最高权限逐笔放行，只对当前这一笔生效；批准后冻结所退票并发起原路退回。"
                          : "批准后冻结所退票并发起原路退回，钱打出后再确认拨付。"}
                          disabled={busy} precheck={validateDecision}
                          onConfirm={() => void runAction("ticket_refund_approved", () => api.post(
                            `/api/v1/ticketing/refunds/${encodeURIComponent(refund.id)}/approve`, { action: "approve", reason: decisionNote.trim() }))}
                          data-testid={`ticket-refund-approve-${refund.id}`} />
                        <HighRiskConfirm label="驳回退款" impact="驳回后该票解冻恢复可用，不退款。" disabled={busy} precheck={validateDecision}
                          onConfirm={() => void runAction("ticket_refund_rejected", () => api.post(
                            `/api/v1/ticketing/refunds/${encodeURIComponent(refund.id)}/reject`, { action: "reject", reason: decisionNote.trim() }))}
                          data-testid={`ticket-refund-reject-${refund.id}`} />
                      </>
                    ) : "待审批"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      <h3>通票安全例外</h3>
      <p className="admin-muted-action">客人出现重大情绪不稳定、安全问题或其他必要问题时登记，由夜班值班经理及以上批准：多次通票不退现金，只恢复本次入场已消耗的 1 次（作为一次完整补偿使用机会，费用由所填公司承担）；其他通票免除该客人那一人份的票款，即使已核销也退，同票其他人照常使用。</p>
      {canRequest ? (
        <div className="admin-form-row" data-testid="ticket-safety-request-form">
          <input data-testid="ticket-safety-instance" aria-label="票实例编号" placeholder="票实例编号" value={safety.instanceId} onChange={(event) => setSafety({ ...safety, instanceId: event.target.value })} />
          <input data-testid="ticket-safety-slot" aria-label="人份序号" type="number" min={1} value={safety.slot} onChange={(event) => setSafety({ ...safety, slot: event.target.value })} />
          <input data-testid="ticket-safety-reason" aria-label="原因" placeholder="原因" value={safety.reason} onChange={(event) => setSafety({ ...safety, reason: event.target.value })} />
          <input data-testid="ticket-safety-situation" aria-label="当时情形" placeholder="当时情形" value={safety.situation} onChange={(event) => setSafety({ ...safety, situation: event.target.value })} />
          <input data-testid="ticket-safety-entity" aria-label="责任主体" placeholder="责任主体 / 恢复次数的承担公司（法律主体编号）" value={safety.entityId} onChange={(event) => setSafety({ ...safety, entityId: event.target.value })} />
          <button type="button" data-testid="ticket-safety-request" disabled={busy} onClick={requestSafetyEvent}>登记安全事件</button>
        </div>
      ) : null}
      {events.length > 0 ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-safety-events-table">
            <thead><tr><th>票</th><th>人份</th><th>原因</th><th>当时情形</th><th>处理</th><th>状态</th><th>申请人</th><th>批准人</th><th>操作</th></tr></thead>
            <tbody>
              {events.map((row) => (
                <tr key={row.id} data-testid={`ticket-safety-event-row-${row.id}`}>
                  <td>{row.parentTicketId ?? "-"}</td>
                  <td>{row.slotIndex ?? "-"}</td>
                  <td>{row.reason ?? "-"}</td>
                  <td>{row.situation ?? "-"}</td>
                  <td>{row.outcome ? OUTCOME_LABELS[row.outcome] ?? row.outcome : "-"}</td>
                  <td>{row.status === "pending" ? "待批准" : formatBusinessStatus(row.status)}</td>
                  <td>{row.requestedBy ?? "-"} · {cnDate(row.requestedAt)}</td>
                  <td>{row.approvedBy ? `${row.approvedBy}（${BASIS_LABELS[row.approverBasis ?? ""] ?? row.approverBasis ?? ""}）` : "—"}</td>
                  <td>
                    {row.status === "pending" && canApprove ? (
                      <HighRiskConfirm label="批准安全事件"
                        impact={row.outcome === "restore"
                          ? "批准后在原票下恢复本次 1 次完整补偿使用机会，不退现金、不重复积分成长值，同一事件只恢复一次。"
                          : "批准后免除该人份票款并发起原路退回（已核销也退），同票其他人照常使用。"}
                        disabled={busy} precheck={validateDecision}
                        onConfirm={() => void act("ticket_safety_event_approved", () => api.post(
                          `/api/v1/ticketing/safety-events/${encodeURIComponent(row.id)}/approve`, { action: "approve", reason: decisionNote.trim() }))}
                        data-testid={`ticket-safety-approve-${row.id}`} />
                    ) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
