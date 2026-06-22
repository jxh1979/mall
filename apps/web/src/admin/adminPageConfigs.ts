export type GenericAdminDataSource = { label: string; endpoint: string };
export type GenericAdminColumn = { key: string; label: string };
export type GenericAdminAction = {
  label: string;
  kind: "request" | "local";
  endpoint?: string;
  method?: "POST" | "PUT" | "PATCH";
  rowIdKey?: string;
  body?: Record<string, unknown>;
  result?: Record<string, unknown>;
  requiresRow?: boolean;
  /** 该写动作所需权限码（与后端 x-permission 对齐）；缺省=无需额外权限（本地/只读动作）。 */
  permission?: string[];
};
export type GenericAdminPageConfig = {
  pageId: string;
  title: string;
  description: string;
  evidence: string;
  dataSources: GenericAdminDataSource[];
  columns: GenericAdminColumn[];
  primaryAction?: GenericAdminAction;
  secondaryAction?: GenericAdminAction;
  contractGap?: string;
};

export const genericAdminPageConfigs: Record<string, GenericAdminPageConfig> = {
  "ADM-GOV-003": {
    pageId: "ADM-GOV-003", title: "四类主体绑定",
    description: "展示并变更经营主体、运营主体、合同主体、结算主体的唯一绑定关系，所有变更留痕。",
    evidence: "读取 /api/v1/platform/business-entity-bindings；变更调用 /api/v1/platform/business-entity-bindings/{id}/change。",
    dataSources: [{ label: "主体绑定", endpoint: "/api/v1/platform/business-entity-bindings" }],
    columns: [{ key: "businessObjectId", label: "业务对象" }, { key: "operatorSubject", label: "运营主体" }, { key: "contractSubject", label: "合同主体" }, { key: "settlementSubject", label: "结算主体" }, { key: "status", label: "状态" }],
    primaryAction: { label: "提交绑定变更", kind: "request", permission: ["entity.binding.manage"], endpoint: "/api/v1/platform/business-entity-bindings/{id}/change", body: { reason: "完整后台验收变更", evidenceAttachmentIds: ["evidence-full-admin"] } },
  },
  "ADM-GOV-004": {
    pageId: "ADM-GOV-004", title: "合同审批与附件归档",
    description: "展示合同核心摘要、合同主体、运营主体、结算主体、核心日期、租金条款、附件和审批状态，审批动作必须带幂等键和证据引用。",
    evidence: "读取 /api/v1/platform/contracts；审批调用 /api/v1/platform/contracts/{contract_id}/approve。",
    dataSources: [{ label: "合同", endpoint: "/api/v1/platform/contracts" }],
    columns: [{ key: "merchantId", label: "商户" }, { key: "contractSubjectId", label: "合同主体" }, { key: "operationSubjectId", label: "运营主体" }, { key: "settlementSubjectId", label: "结算主体" }, { key: "effectiveDate", label: "生效日" }, { key: "expiryDate", label: "到期日" }, { key: "rentTerms", label: "租金条款" }, { key: "shareholderRatioForReport", label: "股东比例(报表)" }, { key: "attachmentIds", label: "附件" }, { key: "lifecycleStatus", label: "审批状态" }],
    primaryAction: { label: "审批通过", kind: "request", permission: ["contract.lifecycle.approve"], endpoint: "/api/v1/platform/contracts/{contract_id}/approve", body: { idempotencyKey: "full-admin-contract-approve", operatorNote: "完整后台验收审批", evidenceAttachmentIds: ["evidence-full-admin"], decision: "approve", comment: "完整后台验收审批" } },
  },
  "ADM-GOV-006": {
    pageId: "ADM-GOV-006", title: "主管复核与违规阻断",
    description: "查看高风险业务动作的主管复核策略，并对合同/基础定义违规、账单减免、人工放行等动作进行默认阻断和主管审批留痕。",
    evidence: "读取 /api/v1/platform/supervisor-review-policies；评估调用 /api/v1/platform/supervisor-review/evaluate；审批调用 /api/v1/platform/supervisor-review/{review_id}/approve。",
    dataSources: [{ label: "复核策略", endpoint: "/api/v1/platform/supervisor-review-policies" }],
    columns: [{ key: "policyCode", label: "策略" }, { key: "actionType", label: "动作" }, { key: "requiredLevel", label: "主管级别" }, { key: "blocksByDefault", label: "默认阻断" }, { key: "requiredEvidenceTypes", label: "所需证据" }, { key: "status", label: "状态" }],
    primaryAction: { label: "评估违规动作", kind: "request", permission: ["governance.supervisor_review.manage"], endpoint: "/api/v1/platform/supervisor-review/evaluate", requiresRow: false, body: { idempotencyKey: "full-admin-supervisor-review-evaluate", operatorNote: "完整后台验收主管复核评估", evidenceAttachmentIds: ["evidence-full-admin"], actionType: "contract_violation", objectType: "contract", objectId: "contract-1", amountCent: 0, violationSummary: "合同主体与基础定义不一致" } },
    secondaryAction: { label: "主管审批放行", kind: "request", permission: ["governance.supervisor_review.manage"], endpoint: "/api/v1/platform/supervisor-review/review-demo/approve", requiresRow: false, body: { idempotencyKey: "full-admin-supervisor-review-approve", operatorNote: "完整后台验收主管审批", evidenceAttachmentIds: ["evidence-full-admin"], reviewerLevel: 3, decision: "approved", comment: "证据齐备，同意放行" } },
  },
  "ADM-TKT-002": {
    pageId: "ADM-TKT-002", title: "动态定价",
    description: "动态定价作为 P2 增强展示规则、仿真和发布；P0 仍以平日/周末/节假日基础价为主。",
    evidence: "读取 /api/v1/ticketing/dynamic-pricing-rules；仿真调用 /api/v1/ticketing/dynamic-pricing/simulations；发布调用 /api/v1/ticketing/dynamic-pricing-rules/{id}/publish。",
    dataSources: [{ label: "动态规则", endpoint: "/api/v1/ticketing/dynamic-pricing-rules" }],
    columns: [{ key: "ruleName", label: "规则" }, { key: "salesChannel", label: "渠道" }, { key: "adjustmentType", label: "调整" }, { key: "adjustmentRate", label: "幅度" }, { key: "status", label: "状态" }],
    primaryAction: { label: "发布规则", kind: "request", permission: ["ticket.dynamic_pricing.publish"], endpoint: "/api/v1/ticketing/dynamic-pricing-rules/{id}/publish", body: { reason: "完整后台验收发布" } },
    secondaryAction: { label: "价格仿真", kind: "request", permission: ["ticket.dynamic_pricing.simulate"], endpoint: "/api/v1/ticketing/dynamic-pricing/simulations", body: { ticketTypeId: "ticket-1", visitDate: "2026-06-20", salesChannel: "web" }, requiresRow: false },
  },
  "ADM-TKT-003": {
    pageId: "ADM-TKT-003", title: "出票与核销追踪",
    description: "追踪票务订单、支付状态、出票状态和核销证据，支持定位未出票和异常核销。",
    evidence: "读取 /api/v1/ticketing/orders；出票调用 /api/v1/ticketing/orders/{order_id}/issue。",
    dataSources: [{ label: "票务订单", endpoint: "/api/v1/ticketing/orders" }],
    columns: [{ key: "orderNo", label: "订单号" }, { key: "quantity", label: "数量" }, { key: "payStatus", label: "支付" }, { key: "issueStatus", label: "出票" }, { key: "status", label: "状态" }],
    primaryAction: { label: "触发出票", kind: "request", permission: ["ticket.ticket.issue"], endpoint: "/api/v1/ticketing/orders/{order_id}/issue", body: { operatorNote: "完整后台验收出票" } },
  },
  "ADM-BAR-002": {
    pageId: "ADM-BAR-002", title: "订单/出品/账单",
    description: "覆盖酒吧点单、出品和账单闭环；拆单、合单、挂账等高级 POS 能力继续按增强项扩展。",
    evidence: "读取 /api/v1/bar/orders；结账调用 /api/v1/bar/orders/{order_id}/bill；出品调用 /api/v1/bar/orders/{order_id}/deliver。",
    dataSources: [{ label: "酒吧订单", endpoint: "/api/v1/bar/orders" }],
    columns: [{ key: "orderNo", label: "订单号" }, { key: "tableId", label: "卡座" }, { key: "totalAmount", label: "金额" }, { key: "status", label: "状态" }, { key: "createdAt", label: "时间" }],
    primaryAction: { label: "生成账单", kind: "request", permission: ["bar.order.create"], endpoint: "/api/v1/bar/orders/{order_id}/bill", body: { operatorNote: "完整后台验收账单" } },
    secondaryAction: { label: "标记出品", kind: "request", permission: ["bar.order.create"], endpoint: "/api/v1/bar/orders/{order_id}/deliver", body: { operatorNote: "完整后台验收出品" } },
  },
  "ADM-BAR-003": {
    pageId: "ADM-BAR-003", title: "存酒",
    description: "展示存酒台账并支持在线领取核销，高价值酒水保持在线校验，不做离线核销。",
    evidence: "读取 /api/v1/inventory/saved-bottles；领取调用 /api/v1/inventory/saved-bottles/{id}/claim。",
    dataSources: [{ label: "存酒", endpoint: "/api/v1/inventory/saved-bottles" }],
    columns: [{ key: "savedNo", label: "存酒号" }, { key: "customerName", label: "客户" }, { key: "itemName", label: "酒水" }, { key: "quantity", label: "数量" }, { key: "status", label: "状态" }],
    primaryAction: { label: "在线领取核销", kind: "request", permission: ["bar.saved_bottle.manage"], endpoint: "/api/v1/inventory/saved-bottles/{id}/claim", body: { claim_items: [{ claim_quantity: 1 }], operator_note: "完整后台验收领取", evidence_attachment_ids: ["att-saved-bottle-claim-1"], verification_code: "123456" } },
  },
  "ADM-BAR-004": {
    pageId: "ADM-BAR-004", title: "销售提成",
    description: "卡座销售提成按销售人员 15%、非销售人员 10% 计算，不包含通票；页面只展示计算证据，不直接发放薪酬。",
    evidence: "读取 /api/v1/bar/orders；计算调用 /api/v1/bar/bills/{bill_id}/commissions/calculate。",
    dataSources: [{ label: "可计提账单", endpoint: "/api/v1/bar/orders" }],
    columns: [{ key: "orderNo", label: "订单号" }, { key: "billId", label: "账单" }, { key: "sellerRole", label: "销售角色" }, { key: "commissionRate", label: "提成率" }, { key: "status", label: "状态" }],
    primaryAction: { label: "计算提成", kind: "request", permission: ["bar.table_commission.calculate"], endpoint: "/api/v1/bar/bills/{bill_id}/commissions/calculate", rowIdKey: "billId", body: { includeTicketAmount: false, salesRate: 0.15, nonSalesRate: 0.1 } },
  },
  "ADM-AMU-003": {
    pageId: "ADM-AMU-003", title: "安全巡检/事故",
    description: "安全告知、巡检、整改和复检闭环必须可追踪，事故工单不进入无留痕处理。",
    evidence: "读取 /api/v1/amusement/safety-inspections；整改调用 /api/v1/amusement/safety-inspections/{id}/rectify。",
    dataSources: [{ label: "安全巡检", endpoint: "/api/v1/amusement/safety-inspections" }],
    columns: [{ key: "inspectionNo", label: "单号" }, { key: "projectName", label: "项目" }, { key: "riskLevel", label: "风险" }, { key: "rectificationStatus", label: "整改" }, { key: "status", label: "状态" }],
    primaryAction: { label: "提交整改", kind: "request", permission: ["amusement.safety_inspection.rectify"], endpoint: "/api/v1/amusement/safety-inspections/{id}/rectify", body: { rectificationNote: "完整后台验收整改", evidenceAttachmentIds: ["evidence-full-admin"] } },
  },
  "ADM-INV-001": {
    pageId: "ADM-INV-001", title: "库存台账",
    description: "库存台账展示物料、库存余额和流水，库存扣减策略以审计结论为准。",
    evidence: "读取 /api/v1/inventory/items、/api/v1/inventory/stock-balances、/api/v1/inventory/stock-movements；调整调用 /api/v1/inventory/stock-movements。",
    dataSources: [{ label: "物料", endpoint: "/api/v1/inventory/items" }, { label: "余额", endpoint: "/api/v1/inventory/stock-balances" }, { label: "流水", endpoint: "/api/v1/inventory/stock-movements" }],
    columns: [{ key: "__source", label: "来源" }, { key: "itemName", label: "物料" }, { key: "quantity", label: "数量" }, { key: "locationType", label: "位置" }, { key: "status", label: "状态" }],
    primaryAction: { label: "登记调整", kind: "request", permission: ["inventory.stock.move"], endpoint: "/api/v1/inventory/stock-movements", body: { from_location_id: "bar-main", to_location_id: "warehouse-main", items: [{ item_id: "item-1", quantity: 1, unit: "bottle" }], trace_code: "TRACE-ADMIN-001", operator_note: "完整后台验收调整", evidence_attachment_ids: ["att-stock-movement-1"] } },
  },
  "ADM-FIN-002": {
    pageId: "ADM-FIN-002", title: "对账差异",
    description: "展示聚合支付、账单和内部应收应付之间的对账批次与差异，人工处理必须留痕。",
    evidence: "读取 /api/v1/finance/reconciliation/batches、/api/v1/finance/reconciliation/differences。",
    dataSources: [{ label: "对账批次", endpoint: "/api/v1/finance/reconciliation/batches" }, { label: "对账差异", endpoint: "/api/v1/finance/reconciliation/differences" }],
    columns: [{ key: "__source", label: "来源" }, { key: "batchNo", label: "批次" }, { key: "differenceNo", label: "差异号" }, { key: "amount", label: "金额" }, { key: "status", label: "状态" }],
    primaryAction: { label: "标记人工复核", kind: "local", result: { status: "reconciliation_reviewed", audit: "manual_review_logged" } },
  },
  "ADM-MER-001": {
    pageId: "ADM-MER-001", title: "\u5546\u6237\u670d\u52a1\u5de5\u5355",
    description: "\u5546\u6237\u670d\u52a1\u3001\u7ef4\u4fee\u3001\u6295\u8bc9\u548c\u6574\u6539\u5de5\u5355\u4f5c\u4e3a P1 \u72ec\u7acb\u5b50\u57df\uff0c\u652f\u6301\u521b\u5efa\u540e\u7684\u6d3e\u5355\u3001\u5904\u7406\u548c\u5173\u95ed\u7559\u75d5\u3002",
    evidence: "\u8bfb\u53d6 /api/v1/merchant-service/work-orders\uff1b\u6d3e\u5355\u8c03\u7528 /api/v1/merchant-service/work-orders/{id}/assign\uff1b\u5173\u95ed\u8c03\u7528 /api/v1/merchant-service/work-orders/{id}/close\u3002",
    dataSources: [{ label: "\u5546\u6237\u5de5\u5355", endpoint: "/api/v1/merchant-service/work-orders" }],
    columns: [{ key: "workOrderType", label: "\u7c7b\u578b" }, { key: "title", label: "\u6807\u9898" }, { key: "priority", label: "\u4f18\u5148\u7ea7" }, { key: "status", label: "\u72b6\u6001" }, { key: "ownerDepartment", label: "\u8d23\u4efb\u90e8\u95e8" }],
    primaryAction: { label: "\u6d3e\u5355", kind: "request", permission: ["merchant.service_work_order.assign"], endpoint: "/api/v1/merchant-service/work-orders/{id}/assign", body: { operatorNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u6d3e\u5355", ownerDepartment: "property", ownerUserId: "staff_owner", operatorUserId: "staff_ops", evidenceAttachmentIds: ["evidence-full-admin"] } },
    secondaryAction: { label: "\u5173\u95ed\u5de5\u5355", kind: "request", permission: ["merchant.service_work_order.close"], endpoint: "/api/v1/merchant-service/work-orders/{id}/close", body: { operatorNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u5173\u95ed", resolution: "\u5df2\u5904\u7406\u5e76\u5b8c\u6210\u5546\u6237\u56de\u8bbf", satisfactionScore: 5, operatorUserId: "staff_ops", evidenceAttachmentIds: ["evidence-full-admin"] } },
  },
  "ADM-MER-002": {
    pageId: "ADM-MER-002", title: "商户授权工作台",
    description: "商户侧在授权业务范围内可查看自己的工单和商户账单，不授予平台治理、财务总账、第三方配置等跨主体能力。",
    evidence: "读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.portal.read、merchant.service_work_order.view、merchant.billing.view 控制。",
    dataSources: [{ label: "商户工单", endpoint: "/api/v1/merchant-service/work-orders" }, { label: "商户账单", endpoint: "/api/v1/merchant-billing/bills" }],
    columns: [{ key: "__source", label: "来源" }, { key: "merchantId", label: "商户" }, { key: "title", label: "事项" }, { key: "billNo", label: "账单号" }, { key: "totalAmount", label: "金额" }, { key: "authorizedScope", label: "授权范围" }, { key: "status", label: "状态" }, { key: "auditEvidenceRef", label: "留痕证据" }],
    primaryAction: { label: "确认授权范围", kind: "local", result: { status: "merchant_scope_confirmed", audit: "merchant_authorized_scope_checked" } },
  },
  "ADM-INT-002": {
    pageId: "ADM-INT-002", title: "回调/轮询/补偿",
    description: "第三方双向开放保留 Webhook、定时轮询校验、重试和补偿证据。",
    evidence: "读取 /api/v1/integration/retry-tasks、/api/v1/integration/compensation-tasks、/api/v1/integration/order-syncs、/api/v1/integration/voucher-verifications。",
    dataSources: [{ label: "重试任务", endpoint: "/api/v1/integration/retry-tasks" }, { label: "补偿任务", endpoint: "/api/v1/integration/compensation-tasks" }, { label: "订单同步", endpoint: "/api/v1/integration/order-syncs" }, { label: "券核销", endpoint: "/api/v1/integration/voucher-verifications" }],
    columns: [{ key: "__source", label: "来源" }, { key: "taskNo", label: "任务号" }, { key: "providerCode", label: "供应商" }, { key: "eventType", label: "事件" }, { key: "status", label: "状态" }],
    primaryAction: { label: "登记轮询校验", kind: "local", result: { status: "polling_checked", audit: "webhook_plus_polling_verified" } },
  },
  "ADM-RISK-001": {
    pageId: "ADM-RISK-001", title: "风控预警",
    description: "展示高风险客户、员工行为、第三方异常和生产风险预警，并保留处理证据。",
    evidence: "读取 /api/v1/analytics/risk-alerts；处理调用 /api/v1/analytics/risk-alerts/{id}/resolve。",
    dataSources: [{ label: "风控预警", endpoint: "/api/v1/analytics/risk-alerts" }],
    columns: [{ key: "alertType", label: "类型" }, { key: "riskLevel", label: "等级" }, { key: "summary", label: "摘要" }, { key: "status", label: "状态" }, { key: "createdAt", label: "时间" }],
    primaryAction: { label: "处理预警", kind: "request", permission: ["analytics.risk_alert.manage"], endpoint: "/api/v1/analytics/risk-alerts/{id}/resolve", body: { resolution: "完整后台验收处理" } },
  },
  "ADM-EVD-001": {
    pageId: "ADM-EVD-001", title: "审计事件",
    description: "统一查看员工、客户、第三方、财务和生产操作留痕；审计事件不可被普通业务流程删除。",
    evidence: "读取 /api/v1/evidence/events。",
    dataSources: [{ label: "审计事件", endpoint: "/api/v1/evidence/events" }],
    columns: [{ key: "eventType", label: "事件" }, { key: "eventCategory", label: "分类" }, { key: "sourceType", label: "来源" }, { key: "result", label: "结果" }, { key: "riskLevel", label: "风险" }],
    primaryAction: { label: "导出审计片段", kind: "local", result: { status: "audit_export_logged", exportScope: "current_page" } },
  },
  "ADM-ANA-001": {
    pageId: "ADM-ANA-001", title: "经营分析",
    description: "经营分析聚合收入、票务、酒吧、游乐、商户健康和风险指标，服务总控复盘。",
    evidence: "读取 /api/v1/analytics/business-metric-snapshots。",
    dataSources: [{ label: "经营指标", endpoint: "/api/v1/analytics/business-metric-snapshots" }],
    columns: [{ key: "metricDate", label: "日期" }, { key: "metricName", label: "指标" }, { key: "metricValue", label: "数值" }, { key: "dimension", label: "维度" }, { key: "status", label: "状态" }],
    primaryAction: { label: "锁定复盘快照", kind: "local", result: { status: "business_snapshot_locked", audit: "management_review_ready" } },
  },
};
