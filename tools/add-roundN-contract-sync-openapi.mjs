// One-off: synchronize this round's new controller endpoints into the P0 OpenAPI contract.
// Source of truth = JSON; YAML regenerated from JSON. Idempotent: re-running yields the same spec.
//
// Covers: evidence risk-alerts (unified exception chain), finance综合体计费 (turnover/deposit/cam/cam-trueup/
// inventory-variance/reconciliation-difference), integration对账 (batches/differences/channel-auto/compensation/
// retry/order-syncs), bar (table-sessions transfer/merge/renew + bill split/merge/adjust + order-item-void decision +
// production-ticket transition/events), amusement (queue/reservation + safety incidents/notices/waiver), inventory
// (saved-bottle transfer/dispute + stock-batches + supplier qualification/reconciliation/settlement + warehouses +
// supply-contracts + risk-alerts scan), compliance (contract-key-dates/expiry-items/reminders + wecom/risk-hits),
// terminal device lifecycle (repair/lost/found/scrap) + mdm violations detect/handle, merchant-billing bills,
// customer points ledger/redemptions, analytics group-overview, ticketing refund lines, floor-plan shapes.
import fs from "node:fs";
import path from "node:path";
import YAML from "yaml";

const root = process.cwd();
const jsonPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const yamlPath = path.join(root, "docs", "openapi", "openapi-p0.yaml");
const spec = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

const writeResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const listResponses = {
  200: { $ref: "#/components/responses/Page" },
  403: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};

const idemProp = {
  idempotency_key: {
    type: "string",
    description: "操作幂等键；重复提交必须返回同一处理结果，避免重复写入。",
    maxLength: 128,
  },
};

function pathParam(name, desc) {
  return {
    name,
    in: "path",
    required: true,
    schema: { type: "string", maxLength: 64 },
    description: desc || `${name} 路径参数；必须引用当前数据范围内存在且允许访问的资源。`,
  };
}

// Inline request body builder. `required` lists required snake_case fields (idempotency_key auto-included when idem=true).
function body(props, required, { idem = true, additional = true } = {}) {
  const properties = idem ? { ...idemProp, ...props } : { ...props };
  const req = idem ? ["idempotency_key", ...(required || [])] : [...(required || [])];
  return {
    required: true,
    content: {
      "application/json": {
        schema: {
          type: "object",
          additionalProperties: additional ? true : false,
          ...(req.length ? { required: req } : {}),
          properties,
        },
      },
    },
  };
}

const S = (description, extra = {}) => ({ type: "string", description, maxLength: 255, ...extra });
const Sid = (description) => ({ type: "string", description, maxLength: 64 });
const Stext = (description) => ({ type: "string", description, maxLength: 4000 });
const I = (description, extra = {}) => ({ type: "integer", description, ...extra });
const Cent = (description) => ({ type: "integer", description: `${description}（单位：分，禁止浮点）`, minimum: -999999999999, maximum: 999999999999 });
const CentPos = (description) => ({ type: "integer", description: `${description}（单位：分，禁止浮点）`, minimum: 0, maximum: 999999999999 });
const Bool = (description) => ({ type: "boolean", description });
const Arr = (description) => ({ type: "array", description, items: { type: "string", maxLength: 64 } });
const Date_ = (description) => ({ type: "string", format: "date", description });
const Ts = (description) => ({ type: "string", format: "date-time", description });
const evidenceProp = { evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
const noteProp = { operator_note: S("操作留痕备注；进入审计。", { maxLength: 1000 }) };

// op() assembles a single operation. params: array of inline parameter objects.
function op(tag, operationId, summary, {
  permission,
  auth = "staff-token",
  terminals = ["web"],
  dataScope,
  idem = false,
  auditSuccess,
  auditFailure,
  requestBody,
  params,
  list = false,
}) {
  const o = {
    tags: [tag],
    operationId,
    summary,
    "x-auth-context": auth,
  };
  if (permission) o["x-permission"] = permission;
  if (dataScope) o["x-data-scope"] = dataScope;
  if (auth !== "open-app-credential" && auth !== "webhook-signature") {
    o["x-terminal-allowed"] = terminals;
  }
  o["x-idempotency-required"] = idem;
  o["x-audit-event-success"] = auditSuccess;
  o["x-audit-event-failure"] = auditFailure;
  if (params) o.parameters = params;
  if (requestBody) o.requestBody = requestBody;
  o.responses = list ? listResponses : writeResponses;
  return o;
}

// merge one or more method operations onto a path without clobbering existing siblings.
function addPath(p, methods) {
  spec.paths[p] = { ...(spec.paths[p] || {}), ...methods };
}

// ════════════════════════════════════════════════════════════════════════════
// evidence — 统一异常责任链 risk-alerts
// ════════════════════════════════════════════════════════════════════════════
const alertIdParam = [pathParam("alert_id", "风险预警 ID。")];
const riskTerms = ["web", "mobile_admin"];
const actionBody = (extra = {}, required = []) => body({ ...noteProp, ...evidenceProp, target_handler_id: Sid("目标处理人用户 ID（transfer/escalate 必填）。"), reason: Stext("操作原因。"), ...extra }, required);

addPath("/api/v1/evidence/risk-alerts", {
  post: op("evidence", "reportRiskAlert", "上报统一风险预警", {
    permission: "risk_alert.report", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.reported", auditFailure: "exception.report_failed",
    requestBody: body({
      ...noteProp, ...evidenceProp,
      source_domain: S("来源业务域。"), source_ref: S("来源业务引用 ID。"),
      alert_type: S("预警类型。"), risk_level: S("风险等级。"), summary: S("预警摘要。"),
      project_id: Sid("所属项目 ID（可空）。"), reason: Stext("上报原因。"),
      evidence_refs: Arr("证据引用列表。"),
    }, ["source_domain", "alert_type", "risk_level", "summary"]),
  }),
  get: op("evidence", "searchRiskAlerts", "查询风险预警列表", {
    permission: "risk_alert.view", terminals: riskTerms, dataScope: "platform",
    auditSuccess: "exception.alerts_viewed", auditFailure: "exception.alerts_view_failed",
    params: [
      { name: "source_domain", in: "query", required: false, schema: { type: "string", maxLength: 64 }, description: "按来源业务域过滤。" },
      { name: "status", in: "query", required: false, schema: { type: "string", maxLength: 32 }, description: "按状态过滤。" },
      { name: "project_id", in: "query", required: false, schema: { type: "string", maxLength: 64 }, description: "按项目过滤。" },
      { name: "limit", in: "query", required: false, schema: { type: "integer", minimum: 1, maximum: 200, default: 20 }, description: "返回条数上限。" },
    ],
    list: true,
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}", {
  get: op("evidence", "getRiskAlertDetail", "查询风险预警详情", {
    permission: "risk_alert.view", terminals: riskTerms, dataScope: "platform",
    auditSuccess: "exception.alert_viewed", auditFailure: "exception.alert_view_failed",
    params: alertIdParam, list: true,
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}/claim", {
  post: op("evidence", "claimRiskAlert", "认领风险预警", {
    permission: "risk_alert.claim", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.claimed", auditFailure: "exception.claim_failed",
    params: alertIdParam, requestBody: actionBody(),
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}/transfer", {
  post: op("evidence", "transferRiskAlert", "转交风险预警", {
    permission: "risk_alert.transfer", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.transferred", auditFailure: "exception.transfer_failed",
    params: alertIdParam, requestBody: actionBody({}, ["target_handler_id"]),
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}/escalate", {
  post: op("evidence", "escalateRiskAlert", "升级风险预警", {
    permission: "risk_alert.escalate", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.escalated", auditFailure: "exception.escalate_failed",
    params: alertIdParam, requestBody: actionBody({}, ["target_handler_id"]),
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}/resolve", {
  post: op("evidence", "resolveRiskAlert", "关闭风险预警（需结论）", {
    permission: "risk_alert.resolve", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.resolved", auditFailure: "exception.resolve_failed",
    params: alertIdParam,
    requestBody: body({ ...noteProp, ...evidenceProp, resolution_conclusion: Stext("处置结论。"), reason: Stext("处置原因。") }, ["resolution_conclusion"]),
  }),
});
addPath("/api/v1/evidence/risk-alerts/{alert_id}/dismiss", {
  post: op("evidence", "dismissRiskAlert", "忽略关闭风险预警", {
    permission: "risk_alert.dismiss", terminals: riskTerms, dataScope: "platform", idem: true,
    auditSuccess: "exception.dismissed", auditFailure: "exception.dismiss_failed",
    params: alertIdParam, requestBody: actionBody(),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// finance — 综合体计费
// ════════════════════════════════════════════════════════════════════════════
const finBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
addPath("/api/v1/finance/cam/allocations", {
  post: op("finance", "allocateCam", "CAM 公共区域维护费按权重分摊", {
    permission: "finance.cam.allocate", dataScope: "platform", idem: true,
    auditSuccess: "finance.cam.allocated", auditFailure: "finance.cam.allocate_failed",
    requestBody: body({
      ...finBase,
      period: S("计费周期标识。"), total_amount_cent: CentPos("待分摊总额。"), due_date: Date_("应收到期日。"),
      shares: { type: "array", description: "分摊明细列表。", items: { type: "object", additionalProperties: false, required: ["merchant_id", "weight_bps"], properties: { merchant_id: Sid("商户 ID。"), weight_bps: I("分摊权重（基点，万分比）。", { minimum: 0 }) } } },
    }, ["period", "total_amount_cent", "due_date", "shares"]),
  }),
});
addPath("/api/v1/finance/cam/trueups", {
  post: op("finance", "trueUpCam", "CAM 年度 true-up 多退少补对账", {
    permission: "finance.cam.trueup", dataScope: "platform", idem: true,
    auditSuccess: "finance.cam.trued_up", auditFailure: "finance.cam.trueup_failed",
    requestBody: body({
      ...finBase,
      settlement_period: S("结算周期标识。"), actual_total_amount_cent: CentPos("实际总额。"), due_date: Date_("应收/应退到期日。"),
      shares: { type: "array", description: "分摊明细列表。", items: { type: "object", additionalProperties: false, required: ["merchant_id", "weight_bps"], properties: { merchant_id: Sid("商户 ID。"), weight_bps: I("分摊权重（基点）。", { minimum: 0 }) } } },
    }, ["settlement_period", "actual_total_amount_cent", "due_date", "shares"]),
  }),
});
addPath("/api/v1/finance/deposits/actions", {
  post: op("finance", "recordDepositAction", "保证金台账动作（收取/退还/抵扣/没收）", {
    permission: "finance.deposit.manage", dataScope: "platform", idem: true,
    auditSuccess: "finance.deposit.action_recorded", auditFailure: "finance.deposit.action_failed",
    requestBody: body({
      ...finBase,
      merchant_id: Sid("商户 ID。"),
      action: { type: "string", description: "保证金动作。", enum: ["collect", "refund", "deduct", "forfeit"] },
      amount_cent: CentPos("动作金额。"), reason: Stext("动作原因。"),
    }, ["merchant_id", "action", "amount_cent"]),
  }),
});
addPath("/api/v1/finance/inventory-variances/post", {
  post: op("finance", "postInventoryVariance", "库存差异复核入账为 AR/AP", {
    permission: "finance.inventory_variance.post", dataScope: "platform", idem: true,
    auditSuccess: "finance.inventory_variance.posted", auditFailure: "finance.inventory_variance.post_failed",
    requestBody: body({
      ...finBase, inventory_variance_id: Sid("库存差异记录 ID。"), counterparty_id: Sid("责任方/对手方 ID。"),
    }, ["inventory_variance_id", "counterparty_id"]),
  }),
});
addPath("/api/v1/finance/reconciliation/differences", {
  post: op("finance", "createFinanceReconciliationDifference", "创建对账差异", {
    permission: "finance.reconciliation.difference.create", dataScope: "platform", idem: true,
    auditSuccess: "finance.reconciliation.difference_created", auditFailure: "finance.reconciliation.difference_create_failed",
    requestBody: body({
      ...finBase, batch_id: Sid("对账批次 ID。"), difference_type: S("差异类型。"), business_ref: S("业务引用。"),
      source: { type: "string", description: "差异来源。", enum: ["system", "manual"] },
      actual_amount_cent: Cent("实际金额（source=manual 时填写）。"),
    }, ["batch_id", "difference_type", "business_ref", "source"]),
  }),
});
addPath("/api/v1/finance/turnover-rent/bills", {
  post: op("finance", "billTurnoverRent", "营业额抽成计费生成应收", {
    permission: "finance.turnover_rent.bill", dataScope: "platform", idem: true,
    auditSuccess: "finance.turnover_rent.billed", auditFailure: "finance.turnover_rent.bill_failed",
    requestBody: body({
      ...finBase, rule_id: Sid("抽成规则 ID。"), period_start: Date_("计费周期起。"), period_end: Date_("计费周期止。"), due_date: Date_("应收到期日。"),
    }, ["rule_id", "period_start", "period_end", "due_date"]),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// integration — 对账 / 补偿 / 重试 / 订单同步
// ════════════════════════════════════════════════════════════════════════════
const intBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
addPath("/api/v1/integration/reconciliation/batches", {
  get: op("integration", "listIntegrationReconciliationBatches", "查询对账批次列表", {
    permission: "integration.reconciliation.view", dataScope: "platform",
    auditSuccess: "integration.reconciliation_batch.viewed", auditFailure: "integration.reconciliation_batch.view_failed", list: true,
  }),
  post: op("integration", "createIntegrationReconciliationBatch", "创建对账批次", {
    permission: "integration.reconciliation.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.reconciliation_batch.created", auditFailure: "integration.reconciliation_batch.create_failed",
    requestBody: body({
      ...intBase, provider_code: S("渠道/供应商编码。"), business_type: S("业务类型。"),
      period_start: Date_("对账周期起。"), period_end: Date_("对账周期止。"),
      expected_count: I("应对账笔数。"), actual_count: I("实际笔数。"), difference_count: I("差异笔数。"), total_difference_cent: Cent("差异总额。"),
    }, ["provider_code", "business_type", "period_start", "period_end"]),
  }),
});
addPath("/api/v1/integration/reconciliation/differences", {
  get: op("integration", "listIntegrationReconciliationDifferences", "查询对账差异列表", {
    permission: "integration.reconciliation.view", dataScope: "platform",
    auditSuccess: "integration.reconciliation_difference.viewed", auditFailure: "integration.reconciliation_difference.view_failed", list: true,
  }),
  post: op("integration", "createIntegrationReconciliationDifference", "创建对账差异", {
    permission: "integration.reconciliation.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.reconciliation_difference.created", auditFailure: "integration.reconciliation_difference.create_failed",
    requestBody: body({
      ...intBase, batch_id: Sid("对账批次 ID。"), provider_code: S("渠道编码。"), business_type: S("业务类型。"),
      external_business_id: S("外部业务 ID。"), difference_type: S("差异类型。"),
      internal_amount_cent: Cent("内部金额。"), provider_amount_cent: Cent("渠道金额。"), difference_amount_cent: Cent("差异金额。"),
      assigned_to_user_id: Sid("分派处理人。"),
    }, ["batch_id", "provider_code", "business_type", "external_business_id", "difference_type"]),
  }),
});
addPath("/api/v1/integration/reconciliation/differences/{id}/resolve", {
  post: op("integration", "resolveIntegrationReconciliationDifference", "处理对账差异", {
    permission: "integration.reconciliation.resolve", dataScope: "platform", idem: true,
    auditSuccess: "integration.reconciliation_difference.resolved", auditFailure: "integration.reconciliation_difference.resolve_failed",
    params: [pathParam("id", "对账差异 ID。")],
    requestBody: body({ ...intBase, operator_user_id: Sid("处理人用户 ID。"), status: S("目标状态。"), resolution_note: Stext("处理说明。") }, ["status"]),
  }),
});
addPath("/api/v1/integration/reconciliation/channel-auto", {
  post: op("integration", "runChannelAutoReconciliation", "渠道自动对账", {
    permission: "integration.reconciliation.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.channel_reconciliation.executed", auditFailure: "integration.channel_reconciliation.execute_failed",
    requestBody: body({
      ...idemProp,
      provider_code: S("渠道编码。"), business_type: S("业务类型。"), channel: S("渠道。"),
      period_start: Date_("对账周期起。"), period_end: Date_("对账周期止。"), dry_run: Bool("试运行标志。"),
      expected_entries: { type: "array", description: "期望对账明细。", items: { type: "object", additionalProperties: true, required: ["external_business_id"], properties: { external_business_id: S("外部业务 ID。"), amount_cent: Cent("金额。"), status: S("状态。"), expected_count: I("期望笔数。"), meta: { type: "object", additionalProperties: true, description: "附加元数据。" } } } },
    }, ["provider_code", "business_type", "channel", "period_start", "period_end", "expected_entries"]),
  }),
});
addPath("/api/v1/integration/compensation-tasks", {
  post: op("integration", "createCompensationTask", "创建补偿任务", {
    permission: "integration.compensation.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.compensation_task.created", auditFailure: "integration.compensation_task.create_failed",
    requestBody: body({
      ...intBase, provider_code: S("渠道编码。"), task_type: S("任务类型。"), business_type: S("业务类型。"),
      external_business_id: S("外部业务 ID。"), status: S("初始状态。"), max_retry_count: I("最大重试次数。"), next_retry_at: Ts("下次重试时间。"),
      payload: { type: "object", additionalProperties: true, description: "任务载荷。" },
    }, ["provider_code", "task_type"]),
  }),
});
addPath("/api/v1/integration/compensation-tasks/{id}/resolve", {
  post: op("integration", "resolveCompensationTask", "处理补偿任务", {
    permission: "integration.compensation.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.compensation_task.resolved", auditFailure: "integration.compensation_task.resolve_failed",
    params: [pathParam("id", "补偿任务 ID。")],
    requestBody: body({ ...intBase, operator_user_id: Sid("处理人用户 ID。"), status: S("目标状态。"), resolution_note: Stext("处理说明。") }, ["status"]),
  }),
});
addPath("/api/v1/integration/order-syncs", {
  post: op("integration", "createOrderSync", "创建订单同步", {
    permission: "integration.order_sync.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.order_sync.created", auditFailure: "integration.order_sync.create_failed",
    requestBody: body({
      ...intBase, provider_code: S("渠道编码。"), business_type: S("业务类型。"), sync_type: S("同步类型。"),
      external_business_id: S("外部业务 ID。"), status: S("状态。"), amount_cent: Cent("金额。"), currency: S("币种。"),
      channel: S("渠道。"), external_channel_order_id: S("外部渠道订单号。"),
      provider_payload: { type: "object", additionalProperties: true, description: "渠道原始载荷。" },
    }, ["provider_code", "business_type", "external_business_id"]),
  }),
});
addPath("/api/v1/integration/retry-tasks", {
  post: op("integration", "createRetryTask", "创建重试任务", {
    permission: "integration.retry.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.retry_task.created", auditFailure: "integration.retry_task.create_failed",
    requestBody: body({
      ...intBase, provider_code: S("渠道编码。"), task_type: S("任务类型。"), business_type: S("业务类型。"),
      external_business_id: S("外部业务 ID。"), status: S("状态。"), max_retry_count: I("最大重试次数。"), next_retry_at: Ts("下次重试时间。"),
      payload: { type: "object", additionalProperties: true, description: "任务载荷。" },
    }, ["provider_code", "task_type"]),
  }),
});
addPath("/api/v1/integration/retry-tasks/{id}/run-now", {
  post: op("integration", "runRetryTaskNow", "立即执行重试任务", {
    permission: "integration.retry.manage", dataScope: "platform", idem: true,
    auditSuccess: "integration.retry_task.run_now", auditFailure: "integration.retry_task.run_now_failed",
    params: [pathParam("id", "重试任务 ID。")],
    requestBody: body({ action: S("执行动作。"), reason: Stext("执行原因。"), evidence_attachment_ids: Arr("证据附件 ID 列表。") }, []),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// bar — 台位会话 / 账单拆并调整 / 退菜决策 / 出品工单
// ════════════════════════════════════════════════════════════════════════════
const barTerms = ["tablet", "web"];
const barScope = "project-bar-table";
const barBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
const billIdP = [pathParam("bill_id", "酒水账单 ID。")];
const sessionIdP = [pathParam("session_id", "台位会话 ID。")];

addPath("/api/v1/bar/table-sessions", {
  get: op("bar", "listBarTableSessions", "查询酒水台位会话列表", {
    permission: "bar.table_session.view", terminals: barTerms, dataScope: barScope,
    auditSuccess: "bar.table_session.list", auditFailure: "bar.table_session.list_failed", list: true,
  }),
  post: op("bar", "createBarTableSession", "创建酒水台位会话（开台）", {
    permission: "bar.table_session.create", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.table_session.create", auditFailure: "bar.table_session.create_failed",
    requestBody: body({
      ...barBase, bar_id: Sid("吧台 ID。"), merchant_id: Sid("商户 ID。"), business_date: Date_("营业日。"),
      table_id: Sid("台位 ID。"), reservation_id: Sid("关联预订 ID。"), customer_id: Sid("客户 ID。"),
      guest_count: I("客数。"), sales_staff_id: Sid("销售员 ID。"), service_staff_id: Sid("服务员 ID。"), opened_by_user_id: Sid("开台操作人。"),
    }, []),
  }),
});
addPath("/api/v1/bar/table-sessions/{session_id}/merge", {
  post: op("bar", "mergeBarTableSession", "合并酒水台位会话", {
    permission: "bar.table_session.merge", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.table_session.merge", auditFailure: "bar.table_session.merge_failed",
    params: sessionIdP,
    requestBody: body({ ...barBase, merged_session_id: Sid("被合并会话 ID。"), reason: Stext("合并原因。"), requested_by_user_id: Sid("发起人。"), primary_session_version: I("主会话版本号。"), merged_session_version: I("被合并会话版本号。") }, []),
  }),
});
addPath("/api/v1/bar/table-sessions/{session_id}/renew", {
  post: op("bar", "renewBarTableSession", "续台（延长台位会话）", {
    permission: "bar.table_session.renew", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.table_session.renew", auditFailure: "bar.table_session.renew_failed",
    params: sessionIdP,
    requestBody: body({ ...barBase, renewed_until: Ts("续台至时间。"), additional_min_consumption_cent: CentPos("追加最低消费。"), additional_deposit_amount_cent: CentPos("追加押金。"), additional_service_fee_cent: CentPos("追加服务费。"), reason: Stext("续台原因。"), requested_by_user_id: Sid("发起人。"), session_version: I("会话版本号。") }, []),
  }),
});
addPath("/api/v1/bar/table-sessions/{session_id}/transfer", {
  post: op("bar", "transferBarTableSession", "转台（台位会话转移）", {
    permission: "bar.table_session.transfer", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.table_session.transfer", auditFailure: "bar.table_session.transfer_failed",
    params: sessionIdP,
    requestBody: body({ ...barBase, target_table_id: Sid("目标台位 ID。"), reason: Stext("转台原因。"), requested_by_user_id: Sid("发起人。"), session_version: I("会话版本号。") }, ["target_table_id"]),
  }),
});
addPath("/api/v1/bar/bills/{bill_id}/operations", {
  get: op("bar", "listBarBillOperations", "查询酒水账单拆并操作记录", {
    permission: "bar.bill.split_merge", terminals: barTerms, dataScope: barScope,
    auditSuccess: "bar.bill_operation.list", auditFailure: "bar.bill_operation.list_failed", params: billIdP, list: true,
  }),
});
addPath("/api/v1/bar/bills/{bill_id}/adjustments", {
  post: op("bar", "adjustBarBill", "对酒水账单应用调整", {
    permission: "bar.bill.adjust", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.bill.adjust", auditFailure: "bar.bill.adjust_failed", params: billIdP,
    requestBody: body({ ...barBase, adjustment_type: S("调整类型。"), amount_cent: Cent("调整金额。"), reason: Stext("调整原因。"), requested_by_user_id: Sid("发起人。") }, []),
  }),
});
addPath("/api/v1/bar/bills/{bill_id}/merge", {
  post: op("bar", "mergeBarBill", "合并酒水账单", {
    permission: "bar.bill.split_merge", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.bill.merge", auditFailure: "bar.bill.merge_failed", params: billIdP,
    requestBody: body({ ...barBase, merged_bill_id: Sid("被合并账单 ID。"), reason: Stext("合并原因。"), requested_by_user_id: Sid("发起人。") }, []),
  }),
});
addPath("/api/v1/bar/bills/{bill_id}/split", {
  post: op("bar", "splitBarBill", "拆分酒水账单", {
    permission: "bar.bill.split_merge", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.bill.split", auditFailure: "bar.bill.split_failed", params: billIdP,
    requestBody: body({ ...barBase, split_amount_cent: CentPos("拆出金额。"), reason: Stext("拆分原因。"), requested_by_user_id: Sid("发起人。") }, []),
  }),
});
addPath("/api/v1/bar/order-item-voids/{void_id}/decision", {
  post: op("bar", "decideBarOrderItemVoid", "退菜退酒审批决策（通过/驳回）", {
    permission: "bar.order_item.void", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.order_item_void.decide", auditFailure: "bar.order_item_void.decide_failed",
    params: [pathParam("void_id", "退菜退酒申请 ID。")],
    requestBody: body({ decision: { type: "string", description: "审批结论。", enum: ["approved", "rejected"] }, reason: Stext("决策原因。") }, ["decision"]),
  }),
});
addPath("/api/v1/bar/production-tickets/{ticket_id}/events", {
  get: op("bar", "listBarProductionTicketEvents", "查询出品工单事件流", {
    permission: "bar.production_ticket.view", terminals: barTerms, dataScope: barScope,
    auditSuccess: "bar.production_ticket_event.list", auditFailure: "bar.production_ticket_event.list_failed",
    params: [pathParam("ticket_id", "出品工单 ID。")], list: true,
  }),
});
addPath("/api/v1/bar/production-tickets/{ticket_id}/transition", {
  post: op("bar", "transitionBarProductionTicket", "流转出品工单状态", {
    permission: "bar.production_ticket.manage", terminals: barTerms, dataScope: barScope, idem: true,
    auditSuccess: "bar.production_ticket.transition", auditFailure: "bar.production_ticket.transition_failed",
    params: [pathParam("ticket_id", "出品工单 ID。")],
    requestBody: body({ ...barBase, target_status: S("目标状态。"), station_code: S("出品工位编码。"), operator_user_id: Sid("操作人。"), reason: Stext("流转原因。") }, ["target_status"]),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// amusement — 排队 / 预约 / 安全事件 / 安全告知
// ════════════════════════════════════════════════════════════════════════════
const amuTerms = ["tablet", "staff_work_phone"];
const amuScope = "project-amusement";
const amuBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
const statusChangeBody = body({ action: S("状态动作。"), reason: Stext("原因。"), evidence_attachment_ids: Arr("证据附件 ID 列表。") }, []);

addPath("/api/v1/amusement/queue-entries", {
  post: op("amusement", "createAmusementQueueEntry", "创建游乐排队记录", {
    permission: "amusement.queue.manage", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.queue_entry.create", auditFailure: "amusement.queue_entry.create_failed",
    requestBody: body({ project_id: Sid("所属项目 ID。"), session_id: Sid("场次 ID。"), customer_id: Sid("客户 ID。"), queue_no: S("排队号。"), status: S("初始状态。") }, []),
  }),
});
addPath("/api/v1/amusement/queue-entries/{id}/status", {
  post: op("amusement", "changeAmusementQueueEntryStatus", "变更游乐排队记录状态", {
    permission: "amusement.queue.manage", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.queue_entry.status_change", auditFailure: "amusement.queue_entry.status_change_failed",
    params: [pathParam("id", "排队记录 ID。")], requestBody: statusChangeBody,
  }),
});
addPath("/api/v1/amusement/reservations", {
  post: op("amusement", "createAmusementReservation", "创建游乐预约", {
    permission: "amusement.reservation.manage", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.reservation.create", auditFailure: "amusement.reservation.create_failed",
    requestBody: body({ project_id: Sid("所属项目 ID。"), session_id: Sid("场次 ID。"), customer_id: Sid("客户 ID。"), reservation_status: S("预约状态。") }, []),
  }),
});
addPath("/api/v1/amusement/reservations/{id}/status", {
  post: op("amusement", "changeAmusementReservationStatus", "变更游乐预约状态", {
    permission: "amusement.reservation.manage", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.reservation.status_change", auditFailure: "amusement.reservation.status_change_failed",
    params: [pathParam("id", "预约记录 ID。")], requestBody: statusChangeBody,
  }),
});
addPath("/api/v1/amusement/incidents", {
  get: op("amusement", "listAmusementIncidents", "查询游乐安全事件列表", {
    permission: "amusement.incident.view", terminals: amuTerms, dataScope: amuScope,
    auditSuccess: "amusement.incident.list", auditFailure: "amusement.incident.list_failed", list: true,
  }),
  post: op("amusement", "createAmusementIncident", "上报游乐安全事件", {
    permission: "amusement.incident.create", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.incident.create", auditFailure: "amusement.incident.create_failed",
    requestBody: body({
      ...amuBase, project_id: Sid("所属项目 ID。"), amusement_project_id: Sid("游乐项目 ID。"), session_id: Sid("场次 ID。"),
      device_id: Sid("设备 ID。"), customer_id: Sid("客户 ID。"), severity: S("严重程度。"), title: S("事件标题。"),
      description: Stext("事件描述。"), reported_by_user_id: Sid("上报人。"), reported_channel: S("上报渠道。"), occurred_at: Ts("发生时间。"),
    }, []),
  }),
});
addPath("/api/v1/amusement/incidents/{incidentId}/actions", {
  post: op("amusement", "addAmusementIncidentAction", "添加事件处置动作/流转状态", {
    permission: "amusement.incident.process", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.incident.action", auditFailure: "amusement.incident.action_failed",
    params: [pathParam("incidentId", "安全事件 ID。")],
    requestBody: body({ ...amuBase, target_status: S("目标状态。"), operator_user_id: Sid("处置人。") }, []),
  }),
});
addPath("/api/v1/amusement/incidents/{incidentId}/close", {
  post: op("amusement", "closeAmusementIncident", "关闭游乐安全事件", {
    permission: "amusement.incident.close", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.incident.close", auditFailure: "amusement.incident.close_failed",
    params: [pathParam("incidentId", "安全事件 ID。")],
    requestBody: body({ ...amuBase, resolution: Stext("处置结论。"), closed_by_user_id: Sid("关闭人。") }, []),
  }),
});
addPath("/api/v1/amusement/safety-notices", {
  get: op("amusement", "listAmusementSafetyNotices", "查询游乐安全告知列表", {
    permission: "amusement.safety_notice.view", terminals: amuTerms, dataScope: amuScope,
    auditSuccess: "amusement.safety_notice.list", auditFailure: "amusement.safety_notice.list_failed", list: true,
  }),
  post: op("amusement", "createAmusementSafetyNotice", "创建游乐安全告知/免责须知", {
    permission: "amusement.safety_notice.manage", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.safety_notice.create", auditFailure: "amusement.safety_notice.create_failed",
    requestBody: body({
      ...amuBase, project_id: Sid("所属项目 ID。"), amusement_project_id: Sid("游乐项目 ID。"), notice_type: S("告知类型。"),
      title: S("标题。"), content_version: S("内容版本。"), content_text: Stext("告知正文。"), status: S("状态。"),
      effective_from: Ts("生效起。"), effective_to: Ts("生效止。"), operator_user_id: Sid("操作人。"),
    }, []),
  }),
});
addPath("/api/v1/amusement/safety-notices/{noticeId}/acknowledgements", {
  post: op("amusement", "acknowledgeAmusementSafetyNotice", "登记安全告知签字确认（免责确认）", {
    permission: "amusement.waiver.acknowledge", terminals: amuTerms, dataScope: amuScope, idem: true,
    auditSuccess: "amusement.waiver.acknowledge", auditFailure: "amusement.waiver.acknowledge_failed",
    params: [pathParam("noticeId", "安全告知 ID。")],
    requestBody: body({
      ...amuBase, project_id: Sid("所属项目 ID。"), amusement_project_id: Sid("游乐项目 ID。"), session_id: Sid("场次 ID。"),
      customer_id: Sid("客户 ID。"), visitor_name: S("游客姓名。"), signer_name: S("签署人姓名。"), acknowledgement_method: S("确认方式。"),
      content_version: S("内容版本。"), content_hash: S("内容哈希。"), acknowledged_at: Ts("确认时间。"), operator_user_id: Sid("操作人。"),
    }, []),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// inventory — 存酒转赠/争议 / 批次 / 供应商资质对账结算 / 库房 / 供应合同 / 风险扫描
// ════════════════════════════════════════════════════════════════════════════
const invTerms = ["web", "mobile_admin"];
const invWebTerms = ["web"];
const invBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
const savedBottleTerms = ["tablet", "web"];
const savedBottleScope = "project-bar-table";

addPath("/api/v1/inventory/risk-alerts", {
  get: op("inventory", "listInventoryRiskAlerts", "查询库存风险预警列表", {
    permission: "inventory.risk_alert.view", terminals: invTerms, dataScope: "warehouse",
    auditSuccess: "inventory.risk_alert.list", auditFailure: "inventory.risk_alert.list_failed", list: true,
  }),
});
addPath("/api/v1/inventory/risk-alerts/scan", {
  post: op("inventory", "scanInventoryRiskAlerts", "触发库存风险扫描", {
    permission: "inventory.risk.scan", terminals: invTerms, dataScope: "warehouse", idem: true,
    auditSuccess: "inventory.risk.scan", auditFailure: "inventory.risk.scan_failed",
    requestBody: body({ operator_note: S("操作留痕备注。", { maxLength: 1000 }), expiry_warning_days: I("临期预警天数阈值。"), operator_user_id: Sid("操作人。") }, []),
  }),
});
addPath("/api/v1/inventory/stock-batches", {
  get: op("inventory", "listInventoryStockBatches", "查询库存批次列表", {
    permission: "inventory.stock.view", terminals: invTerms, dataScope: "warehouse",
    auditSuccess: "inventory.stock_batch.list", auditFailure: "inventory.stock_batch.list_failed", list: true,
  }),
  post: op("inventory", "createInventoryStockBatch", "创建库存批次", {
    permission: "inventory.stock.batch.manage", terminals: invTerms, dataScope: "warehouse", idem: true,
    auditSuccess: "inventory.stock_batch.create", auditFailure: "inventory.stock_batch.create_failed",
    requestBody: body({ ...invBase, item_id: Sid("物料 ID。"), location_id: Sid("库位 ID。"), batch_no: S("批次号。"), supplier_id: Sid("供应商 ID。"), quantity: I("数量。"), expire_at: Ts("过期时间。"), status: S("状态。") }, []),
  }),
});
addPath("/api/v1/inventory/supplier-qualifications", {
  get: op("inventory", "listSupplierQualifications", "查询供应商资质列表", {
    permission: "inventory.supplier.qualification.manage", terminals: invWebTerms, dataScope: "supplier",
    auditSuccess: "inventory.supplier_qualification.list", auditFailure: "inventory.supplier_qualification.list_failed",
    params: [{ name: "supplier_id", in: "query", required: false, schema: { type: "string", maxLength: 64 }, description: "按供应商过滤。" }], list: true,
  }),
  post: op("inventory", "createSupplierQualification", "创建/更新供应商资质", {
    permission: "inventory.supplier.qualification.manage", terminals: invWebTerms, dataScope: "supplier", idem: true,
    auditSuccess: "inventory.supplier_qualification.create", auditFailure: "inventory.supplier_qualification.create_failed",
    requestBody: body({ supplier_id: Sid("供应商 ID。"), qualification_type: S("资质类型。"), qualification_no: S("资质编号。"), issued_by: S("发证机构。"), valid_from: Date_("有效期起。"), valid_to: Date_("有效期止。"), attachment_ids: Arr("资质附件 ID 列表。"), status: S("状态。") }, ["qualification_type"]),
  }),
});
addPath("/api/v1/inventory/supplier-reconciliations", {
  get: op("inventory", "listSupplierReconciliations", "查询供应商对账列表", {
    permission: "inventory.supplier.reconcile", terminals: invWebTerms, dataScope: "supplier",
    auditSuccess: "inventory.supplier_reconciliation.list", auditFailure: "inventory.supplier_reconciliation.list_failed", list: true,
  }),
  post: op("inventory", "createSupplierReconciliation", "创建供应商对账单", {
    permission: "inventory.supplier.reconcile", terminals: invWebTerms, dataScope: "supplier", idem: true,
    auditSuccess: "inventory.supplier_reconciliation.create", auditFailure: "inventory.supplier_reconciliation.create_failed",
    requestBody: body({ ...invBase, supplier_id: Sid("供应商 ID。"), period_start: Date_("对账周期起。"), period_end: Date_("对账周期止。"), expected_amount_cent: Cent("应对账金额。"), actual_amount_cent: Cent("实际金额。"), status: S("状态。"), operator_user_id: Sid("操作人。") }, []),
  }),
});
addPath("/api/v1/inventory/supplier-settlement-methods", {
  get: op("inventory", "listSupplierSettlementMethods", "查询供应商结算档列表", {
    permission: "inventory.supplier.settlement.manage", terminals: invWebTerms, dataScope: "supplier",
    auditSuccess: "inventory.supplier_settlement_method.list", auditFailure: "inventory.supplier_settlement_method.list_failed",
    params: [{ name: "supplier_id", in: "query", required: false, schema: { type: "string", maxLength: 64 }, description: "按供应商过滤。" }], list: true,
  }),
  post: op("inventory", "createSupplierSettlementMethod", "创建/更新供应商结算档", {
    permission: "inventory.supplier.settlement.manage", terminals: invWebTerms, dataScope: "supplier", idem: true,
    auditSuccess: "inventory.supplier_settlement_method.create", auditFailure: "inventory.supplier_settlement_method.create_failed",
    requestBody: body({ supplier_id: Sid("供应商 ID。"), settlement_method: S("结算方式。"), payment_term_days: I("账期天数。"), applicable_category: S("适用品类。"), is_default: Bool("是否默认。"), status: S("状态。") }, ["settlement_method"]),
  }),
});
addPath("/api/v1/inventory/supply-contracts", {
  get: op("inventory", "listSupplyContracts", "查询供应合同列表", {
    permission: "inventory.supply_contract.manage", terminals: invWebTerms, dataScope: "supplier",
    auditSuccess: "inventory.supply_contract.list", auditFailure: "inventory.supply_contract.list_failed", list: true,
  }),
});
addPath("/api/v1/inventory/warehouses", {
  get: op("inventory", "listWarehouses", "查询库房列表", {
    permission: "inventory.warehouse.manage", terminals: invWebTerms, dataScope: "warehouse",
    auditSuccess: "inventory.warehouse.list", auditFailure: "inventory.warehouse.list_failed", list: true,
  }),
});
addPath("/api/v1/inventory/saved-bottles/{id}/dispute", {
  post: op("inventory", "disputeSavedBottle", "受理存酒争议", {
    permission: "bar.saved_bottle.manage", terminals: savedBottleTerms, dataScope: savedBottleScope, idem: true,
    auditSuccess: "bar.saved_bottle.dispute", auditFailure: "bar.saved_bottle.dispute_failed",
    params: [pathParam("id", "存酒记录 ID。")],
    requestBody: body({ ...invBase, reason: Stext("争议原因。"), photo_attachment_ids: Arr("照片附件 ID 列表。"), video_attachment_ids: Arr("视频附件 ID 列表。") }, []),
  }),
});
addPath("/api/v1/inventory/saved-bottles/{id}/transfer", {
  post: op("inventory", "transferSavedBottle", "存酒整瓶转赠", {
    permission: "bar.saved_bottle.manage", terminals: savedBottleTerms, dataScope: savedBottleScope, idem: true,
    auditSuccess: "bar.saved_bottle.transfer", auditFailure: "bar.saved_bottle.transfer_failed",
    params: [pathParam("id", "存酒记录 ID。")],
    requestBody: body({ ...invBase, to_name_or_nickname: S("受赠人姓名/昵称。"), to_phone: S("受赠人电话。"), reason: Stext("转赠原因。") }, []),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// compliance — 合同关键日期 / 到期项 / 提醒 / 企微违规命中
// ════════════════════════════════════════════════════════════════════════════
const compBase = { ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。") };
addPath("/api/v1/compliance/contract-key-dates", {
  get: op("compliance", "listContractKeyDates", "查询合同关键日期列表", {
    permission: "compliance.contract_key_date.view",
    auditSuccess: "compliance.contract_key_date.list", auditFailure: "compliance.contract_key_date.list_failed", list: true,
  }),
  post: op("compliance", "createContractKeyDate", "创建合同关键日期", {
    permission: "compliance.contract_key_date.manage", idem: true,
    auditSuccess: "compliance.contract_key_date.create", auditFailure: "compliance.contract_key_date.create_failed",
    requestBody: body({
      ...compBase, contract_id: Sid("合同 ID。"), merchant_id: Sid("商户 ID。"), date_type: S("日期类型。"), title: S("标题。"),
      due_date: Date_("关键日期。"), reminder_days: { type: "array", description: "提前提醒天数列表。", items: { type: "integer" } }, status: S("状态。"),
      owner_user_id: Sid("责任人。"), source_type: S("来源类型。"), human_confirmed: Bool("是否人工确认。"),
    }, []),
  }),
});
addPath("/api/v1/compliance/expiry-items", {
  get: op("compliance", "listComplianceExpiryItems", "查询合规到期项列表", {
    permission: "compliance.expiry_item.view",
    auditSuccess: "compliance.expiry_item.list", auditFailure: "compliance.expiry_item.list_failed", list: true,
  }),
  post: op("compliance", "createComplianceExpiryItem", "创建合规到期项", {
    permission: "compliance.expiry_item.manage", idem: true,
    auditSuccess: "compliance.expiry_item.create", auditFailure: "compliance.expiry_item.create_failed",
    requestBody: body({
      ...compBase, subject_type: S("主体类型。"), subject_id: Sid("主体 ID。"), merchant_id: Sid("商户 ID。"), item_type: S("到期项类型。"),
      item_name: S("到期项名称。"), document_no: S("证照编号。"), issuing_authority: S("发证机构。"), valid_from: Date_("有效期起。"), valid_to: Date_("有效期止。"),
      reminder_days: { type: "array", description: "提前提醒天数列表。", items: { type: "integer" } }, expiry_action: S("到期动作。"), status: S("状态。"),
      owner_user_id: Sid("责任人。"), source_type: S("来源类型。"), human_confirmed: Bool("是否人工确认。"),
    }, []),
  }),
});
addPath("/api/v1/compliance/reminders", {
  get: op("compliance", "listComplianceReminders", "查询合规提醒列表", {
    permission: "compliance.reminder.view",
    auditSuccess: "compliance.reminder.list", auditFailure: "compliance.reminder.list_failed", list: true,
  }),
});
addPath("/api/v1/compliance/reminders/scan", {
  post: op("compliance", "scanComplianceReminders", "触发合规提醒扫描", {
    permission: "compliance.reminder.scan", idem: true,
    auditSuccess: "compliance.reminder.scan", auditFailure: "compliance.reminder.scan_failed",
    requestBody: body({ operator_note: S("操作留痕备注。", { maxLength: 1000 }), scan_date: Date_("扫描基准日。"), default_lookahead_days: I("默认前瞻天数。"), operator_user_id: Sid("操作人。") }, []),
  }),
});
addPath("/api/v1/compliance/wecom/risk-hits", {
  get: op("compliance", "listWecomRiskHits", "查询企微会话违规命中列表", {
    permission: "wecom.risk_hit.view",
    auditSuccess: "wecom.risk_hit.list", auditFailure: "wecom.risk_hit.list_failed",
    params: [
      { name: "status", in: "query", required: false, schema: { type: "string", maxLength: 32 }, description: "按处置状态过滤。" },
      { name: "conversation_id", in: "query", required: false, schema: { type: "string", maxLength: 64 }, description: "按会话过滤。" },
    ], list: true,
  }),
});
addPath("/api/v1/compliance/wecom/risk-hits/{id}/review", {
  // 注意：服务端未对 review 强制幂等键（WecomRiskHitReviewRequest 无 idempotency_key）。
  post: op("compliance", "reviewWecomRiskHit", "复核处置企微违规命中", {
    permission: "wecom.risk_hit.review", idem: false,
    auditSuccess: "wecom.risk_hit.review", auditFailure: "wecom.risk_hit.review_failed",
    params: [pathParam("id", "企微违规命中 ID。")],
    requestBody: body({ status: { type: "string", description: "目标处置状态。", enum: ["reviewing", "confirmed", "dismissed"] }, review_note: Stext("复核说明。") }, ["status"], { idem: false }),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// terminals — 设备生命周期 + MDM 违规
// ════════════════════════════════════════════════════════════════════════════
const devLifecycleBody = () => body({ ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表；进入审计留痕。"), staff_id: Sid("关联员工 ID。"), reason: Stext("流转原因；进入留痕事件。") }, []);
const devIdP = [pathParam("id", "终端设备 ID。")];
addPath("/api/v1/terminals/devices/{id}/found", {
  post: op("terminals", "markTerminalDeviceFound", "标记设备找回", {
    permission: "terminal.device.lifecycle", terminals: ["web", "mobile_admin"], idem: true,
    auditSuccess: "terminal.device.found", auditFailure: "terminal.device.found_failed", params: devIdP, requestBody: devLifecycleBody(),
  }),
});
addPath("/api/v1/terminals/devices/{id}/lost", {
  post: op("terminals", "markTerminalDeviceLost", "标记设备挂失", {
    permission: "terminal.device.lifecycle", terminals: ["web", "mobile_admin"], idem: true,
    auditSuccess: "terminal.device.lost", auditFailure: "terminal.device.lost_failed", params: devIdP, requestBody: devLifecycleBody(),
  }),
});
addPath("/api/v1/terminals/devices/{id}/repair", {
  post: op("terminals", "markTerminalDeviceRepair", "设备送修", {
    permission: "terminal.device.lifecycle", terminals: ["web", "mobile_admin"], idem: true,
    auditSuccess: "terminal.device.repair", auditFailure: "terminal.device.repair_failed", params: devIdP, requestBody: devLifecycleBody(),
  }),
});
addPath("/api/v1/terminals/devices/{id}/scrap", {
  post: op("terminals", "markTerminalDeviceScrap", "设备报废", {
    permission: "terminal.device.lifecycle", terminals: ["web", "mobile_admin"], idem: true,
    auditSuccess: "terminal.device.scrap", auditFailure: "terminal.device.scrap_failed", params: devIdP, requestBody: devLifecycleBody(),
  }),
});
addPath("/api/v1/terminals/mdm/violations/detect", {
  post: op("terminals", "detectMdmViolations", "服务端检测设备违规", {
    permission: "mdm.device.manage", terminals: ["web", "mobile_admin"], idem: true,
    auditSuccess: "mdm.violation.detect", auditFailure: "mdm.violation.detect_failed",
    requestBody: body({ ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表。"), device_id: Sid("设备 ID。"), app_packages: Arr("应用包名列表。"), site_domains: Arr("访问域名列表。") }, ["device_id"]),
  }),
});
addPath("/api/v1/terminals/mdm/violations/{id}/handle", {
  post: op("terminals", "handleMdmViolation", "违规处置闭环", {
    permission: "mdm.violation.handle", terminals: ["web"], idem: true,
    auditSuccess: "mdm.violation.handle", auditFailure: "mdm.violation.handle_failed",
    params: [pathParam("id", "MDM 违规记录 ID。")],
    requestBody: body({ ...noteProp, evidence_attachment_ids: Arr("证据附件 ID 列表。"), disposition: { type: "string", description: "处置动作。", enum: ["acknowledged", "device_blocked", "dismissed"] }, handled_note: Stext("处置说明。") }, ["disposition"]),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// merchant-billing — 商户账单
// ════════════════════════════════════════════════════════════════════════════
const mbIdP = [pathParam("id", "商户账单 ID。")];
addPath("/api/v1/merchant-billing/bills", {
  post: op("merchant-billing", "createMerchantBill", "创建商户账单", {
    permission: "merchant.billing.manage", idem: true,
    auditSuccess: "merchant.billing.create", auditFailure: "merchant.billing.create_failed",
    requestBody: body({
      operator_note: S("操作留痕备注。", { maxLength: 1000 }), merchant_id: Sid("商户 ID。"), ar_ap_item_id: Sid("关联 AR/AP 条目 ID。"),
      bill_period_start: Date_("账期起。"), bill_period_end: Date_("账期止。"), due_date: Date_("应收到期日。"), total_amount_cent: Cent("账单总额。"),
      source_type: S("来源类型。"), source_id: Sid("来源 ID。"),
      lines: { type: "array", description: "账单明细行。", items: { type: "object", additionalProperties: true, description: "账单明细行 MerchantBillLineRequest。" } },
    }, ["merchant_id"]),
  }),
});
addPath("/api/v1/merchant-billing/bills/{id}/confirm", {
  post: op("merchant-billing", "confirmMerchantBill", "确认商户账单", {
    permission: "merchant.billing.confirm", idem: true,
    auditSuccess: "merchant.billing.confirm", auditFailure: "merchant.billing.confirm_failed", params: mbIdP,
    requestBody: body({ operator_note: S("操作留痕备注。", { maxLength: 1000 }), operator_user_id: Sid("操作人。") }, []),
  }),
});
addPath("/api/v1/merchant-billing/bills/{id}/disputes", {
  post: op("merchant-billing", "submitMerchantBillDispute", "提交账单争议", {
    permission: "merchant.billing.dispute.submit", idem: true,
    auditSuccess: "merchant.billing.dispute.submit", auditFailure: "merchant.billing.dispute.submit_failed", params: mbIdP,
    requestBody: body({ operator_note: S("操作留痕备注。", { maxLength: 1000 }), evidence_attachment_ids: Arr("证据附件 ID 列表。"), reason_code: S("争议原因码。"), description: Stext("争议说明。"), operator_user_id: Sid("操作人。") }, []),
  }),
});
addPath("/api/v1/merchant-billing/bills/{id}/dunning-records", {
  post: op("merchant-billing", "recordMerchantBillDunning", "记录账单催收", {
    permission: "merchant.billing.dunning.record", idem: true,
    auditSuccess: "merchant.billing.dunning.record", auditFailure: "merchant.billing.dunning.record_failed", params: mbIdP,
    requestBody: body({ operator_note: S("操作留痕备注。", { maxLength: 1000 }), dunning_channel: S("催收渠道。"), message_template: S("催收模板。"), status: S("状态。"), sent_at: Ts("发送时间。"), operator_user_id: Sid("操作人。") }, []),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// customer — 积分流水 / 兑换（customer-token）
// ════════════════════════════════════════════════════════════════════════════
const custTerms = ["app", "miniapp", "web"];
addPath("/api/v1/customer/points/ledger", {
  get: op("customer", "getCustomerPointsLedger", "查询积分流水", {
    permission: "not_applicable", auth: "customer-token", terminals: custTerms, dataScope: "self",
    auditSuccess: "customer.points.ledger_viewed", auditFailure: "customer.points.ledger_view_failed", list: true,
  }),
});
addPath("/api/v1/customer/points/redemption-items", {
  get: op("customer", "listCustomerPointsRedemptionItems", "查询积分兑换商品目录", {
    permission: "not_applicable", auth: "customer-token", terminals: custTerms, dataScope: "self",
    auditSuccess: "customer.points.redemption_items_viewed", auditFailure: "customer.points.redemption_items_view_failed", list: true,
  }),
});
addPath("/api/v1/customer/points/redemptions", {
  get: op("customer", "listCustomerPointsRedemptions", "查询我的积分兑换记录", {
    permission: "not_applicable", auth: "customer-token", terminals: custTerms, dataScope: "self",
    auditSuccess: "customer.points.redemptions_viewed", auditFailure: "customer.points.redemptions_view_failed", list: true,
  }),
  post: op("customer", "redeemCustomerPoints", "提交积分兑换", {
    permission: "not_applicable", auth: "customer-token", terminals: custTerms, dataScope: "self", idem: true,
    auditSuccess: "customer.points.redeem", auditFailure: "customer.points.redeem_failed",
    requestBody: body({ catalog_item_id: Sid("兑换商品 ID。"), quantity: I("兑换数量。", { minimum: 1 }), source_terminal: S("来源终端。") }, ["catalog_item_id", "quantity"]),
  }),
});

// ════════════════════════════════════════════════════════════════════════════
// analytics / ticketing / floor-plan — 单端点
// ════════════════════════════════════════════════════════════════════════════
addPath("/api/v1/analytics/group-overview", {
  get: op("analytics", "getGroupOverview", "集团总览视图", {
    permission: "analytics.group.view", terminals: ["web"], dataScope: "group",
    auditSuccess: "analytics.group_overview.viewed", auditFailure: "analytics.group_overview.view_failed", list: true,
  }),
});
addPath("/api/v1/ticketing/refunds/{refund_id}/lines", {
  get: op("ticketing", "listTicketRefundLines", "查询退票明细行", {
    permission: "ticket.refund.view", terminals: ["web"],
    auditSuccess: "ticket.refund_line.list", auditFailure: "ticket.refund_line.list_failed",
    params: [pathParam("refund_id", "退票单 ID。")], list: true,
  }),
});
addPath("/api/v1/floor-plans/{id}/shapes", {
  post: op("floor-plan", "saveFloorPlanShape", "保存平面图图形", {
    permission: "floor_plan.manage", terminals: ["web"], idem: true,
    auditSuccess: "floor_plan.shape.save", auditFailure: "floor_plan.shape.save_failed",
    params: [pathParam("id", "平面图 ID。")],
    requestBody: body({
      operator_note: S("操作留痕备注。", { maxLength: 1000 }), shape_id: Sid("图形 ID。"), shape_type: S("图形类型。"), ref_type: S("引用对象类型。"), ref_id: Sid("引用对象 ID。"),
      label: S("图形标签。"), z_index: I("叠放层级。"), status: S("状态。"),
      polygon_points: { type: "array", description: "多边形顶点列表。", items: { type: "object", additionalProperties: true, description: "顶点坐标 FloorPlanPoint（x/y）。" } },
    }, []),
  }),
});

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("round contract-sync OpenAPI operations added.");
