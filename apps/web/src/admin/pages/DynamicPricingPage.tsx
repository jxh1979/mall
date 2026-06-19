import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, TicketDynamicPricingRuleRow, TicketDynamicPricingSimulationResult } from "../types";

// 后端写权限码（TicketingController：/dynamic-pricing-rules/{id}/publish）
const DYNAMIC_PUBLISH_PERMISSION = "ticket.dynamic_pricing.publish";

const TEXT = {
  title: "\u52a8\u6001\u5b9a\u4ef7",
  description: "P2 \u52a8\u6001\u5b9a\u4ef7\u7528\u4e8e\u89c4\u5219\u8349\u7a3f\u3001\u4ef7\u683c\u4eff\u771f\u548c\u53d1\u5e03\u5ba1\u8ba1\uff1bP0 \u4ecd\u4ee5\u5e73\u65e5/\u5468\u672b/\u8282\u5047\u65e5\u57fa\u7840\u4ef7\u4e3a\u4e3b\u3002",
  evidence: "\u63a5\u53e3\u8bc1\u636e\uff1a\u8bfb\u53d6 /api/v1/ticketing/dynamic-pricing-rules\uff1b\u521b\u5efa\u8349\u7a3f POST /api/v1/ticketing/dynamic-pricing-rules\uff1b\u4eff\u771f POST /api/v1/ticketing/dynamic-pricing/simulations\uff1b\u53d1\u5e03 POST /api/v1/ticketing/dynamic-pricing-rules/{id}/publish\u3002",
  loadFailed: "\u52a8\u6001\u5b9a\u4ef7\u89c4\u5219\u8bfb\u53d6\u5931\u8d25",
  actionFailed: "\u52a8\u6001\u5b9a\u4ef7\u64cd\u4f5c\u5931\u8d25",
  ruleRequired: "\u8bf7\u5148\u9009\u62e9\u8981\u53d1\u5e03\u7684\u89c4\u5219\u3002",
  nameRequired: "\u89c4\u5219\u540d\u79f0\u5fc5\u586b\u3002",
  ruleName: "\u89c4\u5219\u540d\u79f0",
  channel: "\u6e20\u9053",
  adjustmentRate: "\u767e\u5206\u6bd4\u8c03\u6574",
  visitDate: "\u5230\u8bbf\u65e5\u671f",
  createDraft: "\u521b\u5efa\u8349\u7a3f",
  simulate: "\u4ef7\u683c\u4eff\u771f",
  publish: "\u53d1\u5e03\u89c4\u5219",
  boundary: "\u4eff\u771f\u53ea\u8fd4\u56de\u8ba1\u7b97\u7ed3\u679c\uff0c\u4e0d\u521b\u5efa\u8ba2\u5355\u3001\u4e0d\u4fee\u6539\u5df2\u552e\u7968\u3001\u4e0d\u6539\u5199\u5e73\u65e5/\u5468\u672b/\u8282\u5047\u65e5\u57fa\u7840\u4ef7\u3002",
  ruleCount: "\u89c4\u5219\u6570",
  draftCount: "\u8349\u7a3f",
  publishedCount: "\u5df2\u53d1\u5e03",
  selectedRule: "\u9009\u4e2d\u89c4\u5219",
  finalPrice: "\u4eff\u771f\u5230\u624b\u4ef7",
  noSimulation: "\u672a\u4eff\u771f",
  select: "\u9009\u62e9",
  priority: "\u4f18\u5148\u7ea7",
  adjustment: "\u8c03\u6574",
  status: "\u72b6\u6001",
  effective: "\u751f\u6548\u671f",
  createNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u52a8\u6001\u5b9a\u4ef7\u8349\u7a3f",
  publishReason: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u53d1\u5e03",
};

function formatAmount(amountCent: number | undefined) {
  if (amountCent === undefined) return TEXT.noSimulation;
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent) / 100);
}

function ruleLabel(rule: TicketDynamicPricingRuleRow | undefined) {
  return rule?.ruleName ?? rule?.id ?? "-";
}

export function DynamicPricingPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rules, setRules] = React.useState<TicketDynamicPricingRuleRow[]>([]);
  const { selectedId: selectedRuleId, setSelectedId: setSelectedRuleId, selected: selectedRule } = useSelectedRow(rules);
  const [ruleName, setRuleName] = React.useState("Weekend Boost");
  const [salesChannel, setSalesChannel] = React.useState("web");
  const [adjustmentRate, setAdjustmentRate] = React.useState("10");
  const [visitDate, setVisitDate] = React.useState("2026-06-20");
  const [simulation, setSimulation] = React.useState<TicketDynamicPricingSimulationResult | null>(null);
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");

  const { loading, error, reload: reloadResource } = useAdminResource(
    async () => {
      setRules(await api.get<TicketDynamicPricingRuleRow[]>("/api/v1/ticketing/dynamic-pricing-rules"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const draftCount = rules.filter((rule) => rule.status === "draft").length;
  const publishedCount = rules.filter((rule) => rule.status === "published").length;

  async function runAction(action: string, request: () => Promise<unknown>, reload = true) {
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await request();
      setActionResult({ action, result });
      if (reload) await reloadResource();
      return result;
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, status: "failed", message });
      return null;
    } finally {
      setBusyAction("");
    }
  }

  async function createDraft() {
    if (!ruleName.trim()) {
      setFormError(TEXT.nameRequired);
      return;
    }
    await runAction("ticket_dynamic_rule_created", () => api.post("/api/v1/ticketing/dynamic-pricing-rules", {
      ticketTypeId: "ticket-1",
      ruleName,
      priority: 10,
      effectiveFrom: "2026-06-01",
      effectiveTo: "2026-06-30",
      salesChannel,
      adjustmentType: "percent_delta",
      adjustmentRate: Number(adjustmentRate),
      minPriceCent: 9900,
      maxPriceCent: 15000,
      conditionRule: { inventory_level: "low" },
      operatorNote: TEXT.createNote,
      evidenceAttachmentIds: ["att-dynamic-admin-1"],
    }));
  }

  async function simulate() {
    const result = await runAction("ticket_dynamic_pricing_simulated", () => api.post<TicketDynamicPricingSimulationResult>("/api/v1/ticketing/dynamic-pricing/simulations", {
      ticketTypeId: selectedRule?.ticketTypeId ?? "ticket-1",
      visitDate,
      salesChannel,
      context: { inventory_level: "low" },
    }), false);
    if (result) setSimulation(result as TicketDynamicPricingSimulationResult);
  }

  function validatePublish() {
    if (!selectedRule) {
      setFormError(TEXT.ruleRequired);
      return false;
    }
    return true;
  }

  async function publishRule() {
    if (!selectedRule) {
      setFormError(TEXT.ruleRequired);
      return;
    }
    await runAction("ticket_dynamic_rule_published", () => api.post(`/api/v1/ticketing/dynamic-pricing-rules/${selectedRule.id}/publish`, {
      action: "publish",
      reason: TEXT.publishReason,
      evidenceAttachmentIds: ["att-dynamic-publish-1"],
    }));
  }

  return (
    <section data-testid="dynamic-pricing-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="dynamic-pricing-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="ticket_dynamic_pricing.viewed / created / simulated / published" />
      <AdminDataState loading={loading} error={error} empty={rules.length === 0} onRetry={reloadResource}>
        <div className="admin-kpi-grid" data-testid="dynamic-pricing-summary">
          <div className="admin-kpi-card"><span>{TEXT.ruleCount}</span><strong>{rules.length}</strong><small>{TEXT.boundary}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.draftCount}</span><strong>{draftCount}</strong><small>draft</small></div>
          <div className="admin-kpi-card"><span>{TEXT.publishedCount}</span><strong>{publishedCount}</strong><small>published</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedRule}</span><strong>{ruleLabel(selectedRule)}</strong><small>{selectedRule?.salesChannel ?? salesChannel}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.finalPrice}</span><strong>{formatAmount(simulation?.finalPriceCent)}</strong><small>{simulation?.matchedRuleId ?? TEXT.noSimulation}</small></div>
        </div>

        <div className="admin-form-row" data-testid="dynamic-pricing-form">
          <input data-testid="dynamic-pricing-rule-name" value={ruleName} onChange={(event) => setRuleName(event.target.value)} placeholder={TEXT.ruleName} />
          <input data-testid="dynamic-pricing-channel" value={salesChannel} onChange={(event) => setSalesChannel(event.target.value)} placeholder={TEXT.channel} />
          <input data-testid="dynamic-pricing-adjustment-rate" value={adjustmentRate} onChange={(event) => setAdjustmentRate(event.target.value)} placeholder={TEXT.adjustmentRate} />
          <input data-testid="dynamic-pricing-visit-date" value={visitDate} onChange={(event) => setVisitDate(event.target.value)} placeholder={TEXT.visitDate} />
          <button type="button" data-testid="dynamic-pricing-create" disabled={busyAction !== ""} onClick={() => void createDraft()}>{TEXT.createDraft}</button>
          <button type="button" data-testid="dynamic-pricing-simulate" disabled={busyAction !== ""} onClick={() => void simulate()}>{TEXT.simulate}</button>
          <HighRiskConfirm
            label={TEXT.publish}
            impact="将发布选中动态定价规则并写入审计留痕，规则生效后影响对应渠道/日期的售价计算。"
            disabled={busyAction !== ""}
            precheck={validatePublish}
            onConfirm={() => void publishRule()}
            permissionsState={effectivePermissions}
            permissions={[DYNAMIC_PUBLISH_PERMISSION]}
            data-testid="dynamic-pricing-publish"
          />
        </div>
        {formError ? <div data-testid="dynamic-pricing-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dynamic-pricing-table">
            <thead>
              <tr><th>{TEXT.select}</th><th>{TEXT.ruleName}</th><th>{TEXT.channel}</th><th>{TEXT.priority}</th><th>{TEXT.adjustment}</th><th>{TEXT.status}</th><th>{TEXT.effective}</th></tr>
            </thead>
            <tbody>
              {rules.map((rule) => (
                <tr key={rule.id}>
                  <td><input type="radio" data-testid={`dynamic-pricing-select-${rule.id}`} checked={(selectedRule?.id ?? "") === rule.id} onChange={() => setSelectedRuleId(rule.id)} /></td>
                  <td>{rule.ruleName ?? rule.id}</td>
                  <td>{rule.salesChannel ?? "-"}</td>
                  <td>{rule.priority ?? "-"}</td>
                  <td>{rule.adjustmentType ?? "-"} {rule.adjustmentRate ?? ""}</td>
                  <td>{rule.status ?? "-"}</td>
                  <td>{rule.effectiveFrom ?? "-"} / {rule.effectiveTo ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
