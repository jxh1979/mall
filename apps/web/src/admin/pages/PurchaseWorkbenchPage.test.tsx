// 文件版本：V0.06
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { PurchaseWorkbenchPage } from "./PurchaseWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(() => vi.unstubAllGlobals());
const grants = ["inventory.purchase_request.view", "inventory.purchase_request.manage", "inventory.supply_contract.manage", "inventory.purchase.approve", "finance.ar_ap.manage", "finance.ar_ap.view", "evidence.attachment.create"];
const contract = { id: "sc_1", contract_no: "合同一", supplier_id: "supplier_1", project_id: "p_1", contract_mode: "platform_self_use", purchasable_scope: ["item_A_01"], approval_status: "approved", status: "active", valid_from: "2026-01-01", valid_to: "2099-12-31" };
const purchase = { id: "pur_1", request_no: "采购一", supplier_id: "supplier_1", item_id: "item_A_01", quantity: 100, status: "arrived", supply_contract_id: "sc_1" };
const receipt = { id: "receipt_1", arrived_quantity: 80, accepted_quantity: 70, rejected_quantity: 10, received_at: "2026-01-10T02:00:00Z", warehouse_id: "wh_1", location_id: "loc_1", unit_cost_cent: 100, payable_id: "ap_1" };
function setup(config: { permissions?: string[]; detail?: Record<string, unknown>; contract?: Record<string, unknown>; contractTerms?: Record<string, unknown>; optionsFail?: boolean; failOnce?: string; merchants?: Record<string, unknown>[]; reportViewedObjects?: AdminPageProps["reportViewedObjects"] } = {}) {
  const posts: { path: string; body: Record<string, unknown> }[] = [], uploads: FormData[] = [];
  let failed = false;
  const actualContract = { ...contract, ...config.contract };
  const detail = { purchase, project_id: "p_1", review_id: "review_1", accepted_quantity: 70, terminated_quantity: 0, remaining_quantity: 30, receipts: [receipt], resolutions: [], consignment_sales: [], returns: [], payables: [], allocations: [], prepayment_cash_paid_cent: 0, prepayment_allocated_cent: 0, prepayment_payments: !config.permissions || config.permissions.some(p => ["finance.ar_ap.view", "finance.ar_ap.manage"].includes(p)) ? [{ id: "cash_1", amount_cent: 5000, bank_flow_ref: "银行流水一" }] : undefined, ...config.detail };
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = String(input);
    let data: unknown;
    if (path.endsWith("/evidence/attachments/upload")) { uploads.push(init!.body as FormData); data = { id: `proof_${uploads.length}` }; }
    else if (init?.method === "POST") {
      posts.push({ path, body: JSON.parse(String(init.body)) });
      if (!failed && config.failOnce && path.endsWith(config.failOnce)) { failed = true; throw new Error("网络暂不可用"); }
      data = path.endsWith("/purchase-requests") ? { id: "pur_1" } : { id: "result_1" };
    } else if (path.endsWith("/purchase-workbench-options")) {
      if (config.optionsFail) return new Response(JSON.stringify({ code: "FORBIDDEN", message: "目录读取范围不足" }), { status: 403 });
      data = { projects: [{ id: "p_1", name: "长沙综合体" }], suppliers: [{ id: "supplier_1", supplier_name: "真实供应商" }], inventory_items: [{ id: "item_A_01", item_name: "瓶装饮品", unit: "瓶", status: "active" }], entities: [{ id: "entity_1", entity_name: "项目采购公司", project_id: "p_1" }], merchants: config.merchants ?? [], warehouses: [{ id: "wh_1", name: "中心仓", project_id: "p_1", status: "active" }], locations: [{ id: "actual_loc", warehouse_id: "wh_1" }, { id: "loc_9", warehouse_id: "wh_9" }], contracts: [actualContract] };
    } else if (path.endsWith("/purchase-requests")) data = { items: [purchase] };
    else if (path.endsWith("/purchase-requests/pur_1")) data = detail;
    else if (path.endsWith("/supply-contracts/sc_1/terms")) data = { contract: actualContract, terms: config.contractTerms };
    else if (path.endsWith("/payments")) data = { items: [{ id: "cash_1", amount_cent: 5000, payment_reference: "银行流水一" }] };
    else if (path.includes("/purchase-suggestions?project_id=p_1")) data = [{ project_id: "p_1", merchant_id: null, item_id: "item_A_01", item_name: "瓶装饮品", safety_stock: 20, sales_quantity: 33, available_quantity: 12, in_transit_quantity: 10, suggested_quantity: 31, reason: "安全库存 20 ＋ 近 7 天销量 33 － 当前可用 12 － 在途 10 ＝ 建议采购 31" }];
    else throw new Error(`未约定的请求 ${path}`);
    return new Response(JSON.stringify({ code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  const navigateTo = vi.fn(), setActionResult = vi.fn();
  const props: AdminPageProps = { api: new AdminApi(), user: { id: "actor", username: "经办人", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), navigateTo, reportViewedObjects: config.reportViewedObjects, effectivePermissions: { status: "ready", value: { grants: config.permissions ?? grants, denies: [], wildcard: false } } };
  render(<PurchaseWorkbenchPage {...props} />);
  return { posts, uploads, navigateTo, setActionResult };
}
const fill = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
const click = (label: string) => fireEvent.click(screen.getByRole("button", { name: label }));
async function selectPurchase() { await screen.findByText("采购一"); click("查看采购"); await screen.findByText("采购 采购一"); }
async function upload() { fireEvent.change(screen.getByLabelText("本次真实凭证"), { target: { files: [new File(["实际原件字节"], "收货凭证.txt", { type: "text/plain" })] } }); await screen.findByText("收货凭证.txt（已上传）"); }
function fillReceive(accepted = "18") {
  for (const [label, value] of [["本次实到数量", "20"], ["本次合格接收数量", accepted], ["本次拒收数量", "2"], ["实际到货单价（分）", "125"], ["接收仓库", "wh_1"], ["实际库位编号", "actual_loc"], ["实际验收时间（当前设备时区）", "2026-01-11T10:00:00"], ["验收说明", "二瓶破损拒收"]] as const) fill(label, value);
}
it("真实上传后分别提交实到、合格、拒收；失败重试复用同一幂等键", async () => {
  const { posts, uploads } = setup({ failOnce: "/receive" }); await selectPurchase(); click("登记本次验收"); fillReceive(); await upload();
  expect(uploads[0]!.get("business_type")).toBe("inventory_purchase_request"); expect(uploads[0]!.get("business_id")).toBe("pur_1"); expect(uploads[0]!.get("file")).toBeInstanceOf(File);
  click("确认登记本次验收"); await screen.findByRole("alert"); click("确认登记本次验收"); await waitFor(() => expect(posts).toHaveLength(2));
  expect(posts[0]!.body).toMatchObject({ arrived_quantity: 20, accepted_quantity: 18, rejected_quantity: 2, unit_cost_cent: 125, location_id: "actual_loc", warehouse_id: "wh_1", evidence_attachment_ids: ["proof_1"] });
  expect(posts[0]!.body.idempotency_key).toBe(posts[1]!.body.idempotency_key);
  expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-requests/pur_1/receive");
});
// 第十八批 M 段第 119 条 119-R2（BSM:1936「……库房、库位……都要先在基础管理中定义清楚。」）：验收「实际库位编号」只能选所选接收仓库下已建档的库位。
it("登记验收选接收仓库后，「实际库位编号」只列该仓库下已建档的库位", async () => {
  setup(); await selectPurchase(); click("登记本次验收"); fill("接收仓库", "wh_1");
  await waitFor(() => expect(screen.getByLabelText("实际库位编号").tagName).toBe("SELECT"));
  const values = Array.from((screen.getByLabelText("实际库位编号") as HTMLSelectElement).options, o => o.value);
  expect(values).toContain("actual_loc");
  expect(values).not.toContain("loc_9");
});
it("验收数量不平或超过剩余量时不提交业务请求", async () => {
  const { posts } = setup(); await selectPurchase(); click("登记本次验收"); fillReceive("17"); await upload(); click("确认登记本次验收");
  await screen.findByText("合格接收数量与拒收数量之和必须等于本次实到数量"); expect(posts).toHaveLength(0);
  fill("本次实到数量", "40"); fill("本次合格接收数量", "38"); click("确认登记本次验收"); await screen.findByText("本次实到数量不能超过尚未处理余量"); expect(posts).toHaveLength(0);
});
it("普通只读采购账号不依赖合同维护或财务权限读取原件，写操作禁用", async () => {
  setup({ permissions: ["inventory.purchase_request.view"], detail: { payables: undefined } }); await selectPurchase();
  expect(screen.getByRole("button", { name: "登记本次验收" })).toBeDisabled();
  expect(screen.getByText("当前无本采购财务查看权限，款项并非零元或已结清。")).toBeInTheDocument();
});
it("第 82 条 82b-R8：主项目在别处、已批准在所选项目经营的商户可选为自采商户", async () => {
  setup({ merchants: [{ id: "m_1", merchant_name: "跨项目经营商户", project_id: "p_0", operating_project_ids: ["p_0", "p_1"] }, { id: "m_2", merchant_name: "别的项目商户", project_id: "p_0", operating_project_ids: ["p_0"] }] });
  await waitFor(() => expect(screen.getByText("合同一")).toBeInTheDocument());
  click("新建供货合同"); fill("合同综合体", "p_1"); fill("采购模式", "merchant_self_purchase");
  await waitFor(() => expect(Array.from((screen.getByLabelText("自采商户") as HTMLSelectElement).options, o => o.textContent)).toContain("跨项目经营商户"));
  expect(Array.from((screen.getByLabelText("自采商户") as HTMLSelectElement).options, o => o.textContent)).not.toContain("别的项目商户");
});
// 第十八批 AF 段 198-R5 判定 4：建单接口不记申请依据，「申请依据」框删去、请求停发；送审另有自己的「申请依据」框。
it("新建采购只提交所选批准合同的一种商品，供应商来自该合同，不设申请依据框", async () => {
  const { posts } = setup(); await screen.findByText("合同一"); click("新建采购申请"); fill("已批准供货合同", "sc_1"); fill("采购商品", "item_A_01"); fill("采购数量", "15"); fill("预计到货日", "2026-02-01"); expect(screen.queryByLabelText("申请依据")).toBeNull(); click("确认新建采购申请");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.body).toMatchObject({ supplier_id: "supplier_1", supply_contract_id: "sc_1", items: [{ item_id: "item_A_01", quantity: 15 }] }); expect(posts[0]!.body).not.toHaveProperty("operator_note");
});
// 第 23 条复核（PGM:687「这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）」；PGM:686 采购价、供应链价格）：查看合同读到逐商品合同单价后，
// 这份供货合同报为这一次看到的对象。
it("查看合同读到合同单价后，这份供货合同报为这一次看到的对象", async () => {
  const reportViewedObjects = vi.fn();
  setup({ reportViewedObjects });
  await waitFor(() => expect(screen.queryByText("合同一")).not.toBeNull());
  click("查看合同");
  await waitFor(() => expect(reportViewedObjects).toHaveBeenCalledWith("supply_contract", [{ id: "sc_1" }]));
});
it("合同条款采用结构化价格和实际原件，单价商品标识不被改写", async () => {
  const { posts, uploads } = setup({ contract: { approval_status: "draft" } }); await screen.findByText("合同一"); click("查看合同"); await screen.findByText("合同 合同一"); click("登记合同执行条款");
  fill("我方采购主体", "entity_1"); fill("结算方式", "credit_term"); fill("计价依据", "contract_unit_price"); fill("瓶装饮品（瓶）合同单价（分）", "850"); fill("到期起算事件", "all_fulfilled"); fill("起算后天数", "30"); await upload(); click("确认登记合同执行条款");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(uploads[0]!.get("business_type")).toBe("supply_contract"); expect(uploads[0]!.get("business_id")).toBe("sc_1");
  expect(posts[0]!.body.terms).toMatchObject({ purchaser_entity_id: "entity_1", settlement_method: "credit_term", due_anchor: "all_fulfilled", term_days: 30, unit_prices: { item_A_01: 850 } });
});
it("退供交接与财务复核分别请求，复核不会重复扣库存或假记付款", async () => {
  const ret = { id: "ret_1", receipt_id: "receipt_1", quantity: 2, status: "handed_over", accounting_status: "pending_review", requested_by: "other", confirmed_amount_cent: 200, actual_cost_cent: 180, cost_difference_cent: 20, reason: "质量问题", responsibility_terms: "供应商承担返运费" };
  const { posts, uploads } = setup({ detail: { returns: [ret] } }); await selectPurchase(); expect(screen.getByRole("button", { name: "登记退供实际交接" })).toBeDisabled(); click("复核退供账款"); fill("双方确认的供应商退款到期日（产生应收时必填）", "2026-02-15"); fill("财务复核依据", "核实原款已付及差额"); await upload(); click("确认复核退供账款");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-returns/ret_1/finance-review"); expect(posts[0]!.body).toMatchObject({ supplier_refund_due_date: "2026-02-15", reason: "核实原款已付及差额" }); expect(uploads[0]!.get("business_type")).toBe("purchase_return"); expect(uploads[0]!.get("business_id")).toBe("ret_1");
});
it("预付冲抵读取原真实付款并引用本采购当前入库账款，不新建现金付款", async () => {
  const { posts } = setup({ detail: { settlement_method: "prepaid", settlement_terms: { settlement_method: "prepaid" }, payables: [{ id: "pap_1", item_no: "预付一", source_type: "purchase_prepayment", amount_cent: 5000, paid_amount_cent: 5000, unpaid_amount_cent: 0, status: "settled" }, { id: "ap_1", item_no: "入库一", source_type: "purchase_inbound", source_id: "receipt_1", direction: "payable", is_current: true, amount_cent: 7000, paid_amount_cent: 0, unpaid_amount_cent: 7000, status: "pending" }] } });
  await selectPurchase(); click("分配已付预付款"); await screen.findByText("银行流水一／¥50.00"); fill("实际预付款流水", "cash_1"); fill("本采购待付入库款", "ap_1"); fill("本次冲抵金额（分）", "3000"); await upload(); click("确认分配已付预付款"); await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-requests/pur_1/prepayment-allocations"); expect(posts[0]!.body).toMatchObject({ source_payment_id: "cash_1", target_item_id: "ap_1", amount_cent: 3000 });
});
it("抵扣目标只列当前未锁账原件，通用重述仍关联原验收而不回改历史指针", async () => {
  const { posts } = setup({ detail: { settlement_method: "prepaid",
    receipts: [receipt, { ...receipt, id: "receipt_2", payable_id: "locked" }],
    payables: [
      { id: "ap_1", item_no: "旧入库", source_type: "purchase_inbound", source_id: "receipt_1", direction: "payable", amount_cent: 7000, unpaid_amount_cent: 7000, is_current: false, locked_day_close_id: "close_1" },
      { id: "restated", item_no: "当前重述", source_type: "purchase_inbound", source_id: "receipt_1", direction: "payable", amount_cent: 7100, unpaid_amount_cent: 7100, is_current: true },
      { id: "locked", item_no: "另笔已锁账", source_type: "purchase_inbound", source_id: "receipt_2", direction: "payable", amount_cent: 1000, unpaid_amount_cent: 1000, is_current: true, locked_day_close_id: "close_2" },
      { id: "foreign", item_no: "其他采购", source_type: "purchase_inbound", source_id: "foreign_receipt", direction: "payable", amount_cent: 1000, unpaid_amount_cent: 1000, is_current: true },
    ],
  } });
  await selectPurchase(); click("分配已付预付款");
  expect(screen.getByRole("option", { name: "当前重述／未付 ¥71.00" })).toBeInTheDocument();
  expect(screen.queryByRole("option", { name: /旧入库|另笔已锁账|其他采购/ })).not.toBeInTheDocument();
  fill("实际预付款流水", "cash_1"); fill("本采购待付入库款", "restated"); fill("本次冲抵金额（分）", "3000");
  await upload(); click("确认分配已付预付款");
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]!.body.target_item_id).toBe("restated");
});
it("预付更正后仍读取本采购全部原付款，不只取第一张应付下流水", async () => {
  setup({ detail: { settlement_method: "prepaid",
    payables: [{ id: "old-prepay", source_type: "purchase_prepayment", amount_cent: 5000, unpaid_amount_cent: 0 },
      { id: "new-prepay", source_type: "purchase_prepayment", amount_cent: 4000, unpaid_amount_cent: 1000 }],
    prepayment_payments: [{ id: "cash_1", amount_cent: 1000, bank_flow_ref: "原单真实流水" },
      { id: "cash_2", amount_cent: 2000, bank_flow_ref: "更正单真实流水" }],
  } });
  await selectPurchase(); click("分配已付预付款");
  await screen.findByRole("option", { name: "更正单真实流水／¥20.00" });
  expect(screen.getByRole("option", { name: "原单真实流水／¥10.00" })).toBeInTheDocument();
  expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).endsWith("/payments"))).toBe(false);
});
it("目录读取失败不隐藏已有采购，审批原件按真实reviewId导航", async () => {
  const { navigateTo } = setup({ optionsFail: true }); await selectPurchase(); expect(screen.getByRole("alert")).toHaveTextContent("可选合同、商品与仓库读取失败"); click("查看审批原件"); expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-009", "review_1");
});
it("寄售录入者不能自行确认，复核另需采购批准与财务权限", async () => {
  setup({ detail: { consignment_sales: [{ id: "sale_1", receipt_id: "receipt_1", quantity: 3, status: "pending_confirmation", requested_by: "actor", source_reference: "售出单一", sold_at: "2026-01-11T02:00:00Z" }] } }); await selectPurchase(); expect(screen.getByRole("button", { name: "复核寄售售出" })).toBeDisabled(); expect(screen.getByText("须由不同人员复核。")).toBeInTheDocument();
});
it("采购经办无需财务查看权也能按已锁定的寄售依据记录售出", async () => {
  const { posts } = setup({ permissions: ["inventory.purchase_request.view", "inventory.purchase_request.manage", "evidence.attachment.create"], detail: { payables: undefined, settlement_terms: undefined, settlement_method: "consignment", consignment_sale_basis: "签收销售清单" } });
  await selectPurchase(); click("登记寄售实际售出"); expect(screen.getByLabelText("合同售出确认依据")).toHaveValue("签收销售清单");
  fill("合格入库来源", "receipt_1"); fill("实际售出数量", "3"); fill("实际售出原始单据编号", "real-sale-1"); fill("实际售出时间（当前设备时区）", "2026-01-12T10:00:00"); await upload(); click("确认登记寄售实际售出");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.body).toMatchObject({ receipt_id: "receipt_1", quantity: 3, sale_basis: "签收销售清单", source_reference: "real-sale-1" });
});
it("合同提交携带真实主线及业务财务法务审批人，不直接设合同为生效", async () => {
  const { posts } = setup({ contract: { approval_status: "draft" } }); await screen.findByText("合同一"); click("查看合同"); await screen.findByText("合同 合同一"); click("发起合同审批");
  for (const [label, value] of [["申请依据", "原件及执行条款已核对"], ["主线最终审批人", "manager"], ["业务线最终审批人", "business"], ["财务线最终审批人", "finance"], ["法务线最终审批人", "legal"]] as const) fill(label, value);
  click("确认发起合同审批"); await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]!.path).toBe("/api/v1/inventory/supply-contracts/sc_1/approve");
  expect(posts[0]!.body).toMatchObject({ final_approver_id: "manager", parallel_lines: [{ line_code: "business_review", final_approver_id: "business" }, { line_code: "finance_review", final_approver_id: "finance" }, { line_code: "legal_review", final_approver_id: "legal" }] });
  expect(posts[0]!.body).not.toHaveProperty("approval_status");
});
it("终止余量需要主管审批及采购证据，跟进补货不伪造批准", async () => {
  const { posts } = setup(); await selectPurchase(); click("处理未交及拒收余量"); fill("余量处理方式", "terminate"); fill("余量处理数量", "30"); fill("处理依据", "供应商确认不再补交"); fill("主线最终审批人", "manager"); await upload(); click("确认处理未交及拒收余量");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.body).toMatchObject({ resolution_type: "terminate", quantity: 30, final_approver_id: "manager", evidence_attachment_ids: ["proof_1"] }); expect(posts[0]!.body).not.toHaveProperty("status");
});
it("预付未用余额须引用原付款及明确合同依据，采购尚待交付也可申请而非自动退", async () => {
  const { posts, uploads } = setup({ permissions: ["inventory.purchase_request.view", "inventory.purchase_request.manage", "evidence.attachment.create"], detail: { settlement_method: "prepaid", payables: undefined } });
  await selectPurchase(); click("处理未用预付余额"); fill("实际预付款流水", "cash_real"); fill("双方确认的预付款退款到期日", "2026-02-20"); fill("预付余额处理金额（分）", "1000"); fill("锁定合同余额处理依据", "补充约定第六条"); fill("处理原因", "双方确认退回部分未用预款"); fill("主线最终审批人", "manager"); fill("财务线最终审批人", "finance"); await upload(); click("确认处理未用预付余额");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-requests/pur_1/prepayment-resolutions"); expect(posts[0]!.body).toMatchObject({ resolution_type: "refund_unused", source_payment_id: "cash_real", amount_cent: 1000, refund_due_date: "2026-02-20", contract_basis: "补充约定第六条", final_approver_id: "manager", parallel_lines: [{ line_code: "finance_review", final_approver_id: "finance" }] }); expect(uploads[0]!.get("business_id")).toBe("pur_1");
});
it("批准预付调整由不同财务人员过账，证据绑定调整方案且不发送现金退款", async () => {
  const plan = { id: "ppr_1", resolution_type: "refund_unused", amount_cent: 1000, contract_basis: "原条款", reason: "双方确认", status: "approved", requested_by: "other", review_id: "r_ppr" };
  const { posts, uploads } = setup({ detail: { settlement_method: "prepaid", prepayment_resolutions: [plan], prepayment_refund_reserved_cent: 1000 } }); await selectPurchase(); click("登记已批准预付调整"); fill("预付调整复核依据", "已核对原支付及批准方案"); await upload(); click("确认登记已批准预付调整");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-prepayment-resolutions/ppr_1/post"); expect(uploads[0]!.get("business_type")).toBe("purchase_prepayment_resolution"); expect(uploads[0]!.get("business_id")).toBe("ppr_1"); expect(posts[0]!.body).not.toHaveProperty("paid");
});
// 第十八批第 33 条（BSM:1211「系统根据安全库存、销量和在途数量生成建议采购数量及理由，由采购人员人工调整并按审批链批准后生成采购单，不得自动向供应商下单」）。
// 第十八批 AF 段判定 4：理由只在采购建议表显示，建单不预填、不发申请依据。
it("采购建议列出建议量与理由，按建议发起采购预填合同商品数量，人工改数量后提交，建单不带申请依据", async () => {
  const reason = "安全库存 20 ＋ 近 7 天销量 33 － 当前可用 12 － 在途 10 ＝ 建议采购 31";
  const { posts } = setup();
  await waitFor(() => expect(screen.queryByLabelText("采购建议综合体")).not.toBeNull());
  await screen.findByText("合同一"); fill("采购建议综合体", "p_1");
  await waitFor(() => expect(screen.queryByText(reason)).not.toBeNull());
  expect(screen.getByText(reason).closest("tr")).toHaveTextContent("瓶装饮品平台31");
  click("按建议发起采购");
  expect(screen.getByLabelText("已批准供货合同")).toHaveValue("sc_1"); expect(screen.getByLabelText("采购商品")).toHaveValue("item_A_01");
  expect(screen.getByLabelText("采购数量")).toHaveValue(31); expect(screen.queryByLabelText("申请依据")).toBeNull();
  fill("采购数量", "30"); fill("预计到货日", "2026-02-01"); click("确认新建采购申请");
  await waitFor(() => expect(posts).toHaveLength(1)); expect(posts[0]!.path).toBe("/api/v1/inventory/purchase-requests");
  expect(posts[0]!.body).toMatchObject({ supplier_id: "supplier_1", supply_contract_id: "sc_1", items: [{ item_id: "item_A_01", quantity: 30 }] });
  expect(posts[0]!.body).not.toHaveProperty("operator_note");
  expect(posts.some(p => p.path.endsWith("/order"))).toBe(false);
});
