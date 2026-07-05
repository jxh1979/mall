import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

// 后端 ContractRecord / ContractApprovalFlowStepRecord / SpaceExpiryColorRuleRecord（客户端 unwrap 已 camel 化）。
type ContractRow = {
  id: string; merchantId: string; lifecycleStatus: string;
  effectiveDate?: string; expiryDate?: string; commissionMode?: string | null; projectId?: string | null;
};
type FlowStep = { stageSeq: number; stageName: string; approverPermissionCode: string };
type ColorRule = { id: string; categoryName: string; color: string; thresholdDays: number };
type ChangeRow = { id: string; contractId: string; changeType: string; approvalStatus: string };
type SettlementRow = { id: string; contractId: string; triggerType: string; status: string };
type SpaceRental = { spaceId: string; spaceName: string; rentalStatus: string; color?: string | null; daysToExpiry?: number | null };

const DRAFT = ["contract.draft.manage"];
const ACTIVATE = ["contract.lifecycle.activate"];
const RENT_AR = ["finance.ar_ap.manage"];
const SETTLE = ["contract.settlement.manage"];
const FLOW_CFG = ["contract.approval.flow.manage"];
const COLOR_CFG = ["contract.space.color.manage"];
const CHANGE_MANAGE = ["contract.change.manage"];
const DISPUTE_RESOLVE = ["contract.settlement.dispute.resolve"];
const CHANGE_TYPES = [
  { v: "renewal", l: "续租" }, { v: "move_out", l: "退租" }, { v: "area_change", l: "面积变更" },
  { v: "rent_change", l: "租金变更" }, { v: "business_scope_change", l: "经营范围变更" }, { v: "early_termination", l: "提前终止" },
];
const RENTAL_LABEL: Record<string, string> = {
  vacant: "空置", negotiating: "洽谈中", let: "已租", expiring: "即将到期", moved_out: "已退租", unavailable: "不可租",
};
const RENTAL_COLOR: Record<string, string> = {
  vacant: "#9ca3af", negotiating: "#3b82f6", let: "#10b981", expiring: "#F59E0B", moved_out: "#6b7280", unavailable: "#ef4444",
};
const OPEN_SETTLEMENT = ["pending_settlement", "deposit_offsetting", "arrears_collecting"];

// 分组并行审批：默认四岗，按 stageSeq 串行。审批权限码即并行槽 id。
const APPROVE_ROLES: { code: string; label: string }[] = [
  { code: "contract.approve.business", label: "业务过" },
  { code: "contract.approve.finance", label: "财务过" },
  { code: "contract.approve.legal", label: "法务过" },
  { code: "contract.approve.gm", label: "总经理过" },
];
// 审批链表格「审批岗位」列显名：权限码 → 岗位业务名。
const ROLE_LABELS: Record<string, string> = {
  "contract.approve.business": "业务岗",
  "contract.approve.finance": "财务岗",
  "contract.approve.legal": "法务岗",
  "contract.approve.gm": "总经理",
};

/**
 * 合同生命周期工作台（ADM-CONT-001，V162 重建）：起草 → 分组并行审批 → 生效固化 → 退租结算 + 审批链/到期着色配置。
 * 覆盖验收 ADM-CONT-001..008：起草/四级并行审批/固化快照/铺位着色配置/变更入口/退租结算/审批链配置。
 */
export function ContractWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [flow, setFlow] = React.useState<FlowStep[]>([]);
  const [colorRules, setColorRules] = React.useState<ColorRule[]>([]);
  const [draft, setDraft] = React.useState({
    merchantId: "", contractSubjectId: "", settlementSubjectId: "", effectiveDate: "", expiryDate: "",
    baseRentYuan: "", commissionMode: "fixed_ratio", energyFeeBasis: "by_area",
  });
  const [spaceId, setSpaceId] = React.useState("");
  const [billPeriod, setBillPeriod] = React.useState("");
  const [colorForm, setColorForm] = React.useState({ categoryName: "", color: "#F59E0B", thresholdDays: "30" });
  const [selectedId, setSelectedId] = React.useState("");
  const [changes, setChanges] = React.useState<ChangeRow[]>([]);
  const [settlements, setSettlements] = React.useState<SettlementRow[]>([]);
  const [spaceRental, setSpaceRental] = React.useState<SpaceRental[]>([]);
  const [changeForm, setChangeForm] = React.useState({ changeType: "rent_change", note: "" });
  const [disputeReason, setDisputeReason] = React.useState("");
  // 商户显名字典（页面级拉取，失败降级回 ID）。
  const [merchantNames, setMerchantNames] = React.useState<Record<string, string>>({});
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·日期中文·引用 TraceLink）。合同/变更/退租结算三张业务表接入。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [rows, steps, rules, spaces] = await Promise.all([
        api.get<ContractRow[]>("/api/v1/contracts"),
        api.get<FlowStep[]>("/api/v1/contracts/approval-flow"),
        api.get<ColorRule[]>("/api/v1/contracts/expiry-color-rules"),
        api.get<SpaceRental[]>("/api/v1/contracts/space-rental-status"),
      ]);
      setContracts(rows ?? []);
      setFlow(steps ?? []);
      setColorRules(rules ?? []);
      setSpaceRental(spaces ?? []);
      // 显名字典拉取失败不阻断主流程，表格降级显示 ID。
      try {
        const merchants = await api.get<Array<{ id: string; displayName?: string; companyName?: string }>>("/api/v1/platform/merchants");
        setMerchantNames(Object.fromEntries((merchants ?? []).filter((m) => m.displayName || m.companyName).map((m) => [m.id, (m.displayName ?? m.companyName) as string])));
      } catch {
        /* 无权限或接口失败 → 保持空字典 */
      }
    },
    { onError: setGlobalError, fallbackMessage: "合同数据读取失败", onStart: () => setGlobalError(null) },
  );

  // 下钻定位：从别的页点合同号进来（?focus=合同id）→ 选中该合同并滚动高亮其行；无 focus 时行为不变。
  React.useEffect(() => {
    if (!focusObjectId || contracts.length === 0) return;
    if (!contracts.some((row) => row.id === focusObjectId)) return;
    void selectContract(focusObjectId);
    const el = typeof document !== "undefined" ? document.querySelector(`[data-testid="contract-${focusObjectId}"]`) : null;
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusObjectId, contracts.length]);

  async function createDraft() {
    if (!draft.merchantId.trim() || !draft.contractSubjectId.trim() || !draft.effectiveDate || !draft.expiryDate) {
      setGlobalError("起草需填写商户、合同主体、生效/到期日期");
      return;
    }
    const cent = Math.round(Number(draft.baseRentYuan) * 100);
    await api.post("/api/v1/contracts", {
      merchantId: draft.merchantId.trim(),
      contractSubjectId: draft.contractSubjectId.trim(),
      settlementSubjectId: (draft.settlementSubjectId.trim() || draft.contractSubjectId.trim()),
      effectiveDate: draft.effectiveDate,
      expiryDate: draft.expiryDate,
      rentTerms: Number.isFinite(cent) && cent > 0 ? { baseRentCent: cent } : {},
      commissionMode: draft.commissionMode,
      energyFeeBasis: draft.energyFeeBasis,
    });
    setActionResult({ action: "contract_drafted", merchantId: draft.merchantId.trim() });
    setDraft({ ...draft, merchantId: "", baseRentYuan: "" });
    reload();
  }

  async function submit(row: ContractRow) {
    await api.post(`/api/v1/contracts/${row.id}/submit`, {});
    setActionResult({ action: "contract_submitted", contractId: row.id });
    reload();
  }

  async function approve(row: ContractRow, code: string) {
    await api.post(`/api/v1/contracts/${row.id}/approve`, { approverPermissionCode: code, decision: "approve" });
    setActionResult({ action: "contract_approved", contractId: row.id, role: code });
    reload();
  }

  async function activate(row: ContractRow) {
    if (!spaceId.trim()) {
      setGlobalError("生效需填写所租空间 id");
      return;
    }
    await api.post(`/api/v1/contracts/${row.id}/activate`, { spaceIds: [spaceId.trim()] });
    setActionResult({ action: "contract_activated", contractId: row.id });
    reload();
  }

  // P1-B 收入闭环：手工补排某账期租金/物业/能源 AR（生效已自动排整租期，此处用于补排/重排，幂等已挂账则跳过）。
  async function generatePeriodBills(row: ContractRow) {
    if (!billPeriod) {
      setGlobalError("排租金AR需先选账期（年-月）");
      return;
    }
    const bills = await api.post<unknown[]>(`/api/v1/contracts/${row.id}/period-bills`, { period: billPeriod });
    setActionResult({ action: "contract_period_bills_generated", contractId: row.id, period: billPeriod, count: (bills ?? []).length });
    reload();
  }

  async function settle(row: ContractRow) {
    // 退租开账 → 待结算（填争议原因则挂起有争议）。
    await api.post(`/api/v1/contracts/${row.id}/settle`, {
      triggerType: "move_out", disputeReason: disputeReason.trim() || null,
    });
    setActionResult({ action: "contract_settle_opened", contractId: row.id });
    setDisputeReason("");
    await selectContract(row.id);
    reload();
  }

  async function selectContract(id: string) {
    setSelectedId(id);
    const [chs, sts] = await Promise.all([
      api.get<ChangeRow[]>(`/api/v1/contracts/${id}/changes`),
      api.get<SettlementRow[]>(`/api/v1/contracts/${id}/settlements`),
    ]);
    setChanges(chs ?? []);
    setSettlements(sts ?? []);
  }

  async function raiseChange() {
    if (!selectedId) {
      setGlobalError("先点合同行『明细』选一份合同");
      return;
    }
    await api.post(`/api/v1/contracts/${selectedId}/changes`, {
      changeType: changeForm.changeType, payload: changeForm.note.trim() ? { note: changeForm.note.trim() } : {},
    });
    setActionResult({ action: "contract_change_raised", contractId: selectedId, type: changeForm.changeType });
    void selectContract(selectedId);
  }

  async function approveChange(changeId: string, code: string) {
    await api.post(`/api/v1/contracts/changes/${changeId}/approve`, { approverPermissionCode: code, decision: "approve" });
    setActionResult({ action: "contract_change_approved", changeId, role: code });
    void selectContract(selectedId);
  }

  async function settlementAction(sid: string, action: "advance" | "complete" | "resolve-dispute") {
    await api.post(`/api/v1/contracts/settlements/${sid}/${action}`, {});
    setActionResult({ action: "contract_settlement_" + action, settlementId: sid });
    await selectContract(selectedId);
    reload();
  }

  async function resetFlowToDefault() {
    await api.post("/api/v1/contracts/approval-flow", {
      name: "平台默认合同审批链",
      steps: [
        { stageSeq: 1, stageName: "业务审核", approverPermissionCode: "contract.approve.business" },
        { stageSeq: 1, stageName: "财务审核", approverPermissionCode: "contract.approve.finance" },
        { stageSeq: 1, stageName: "法务审核", approverPermissionCode: "contract.approve.legal" },
        { stageSeq: 2, stageName: "总经理终审", approverPermissionCode: "contract.approve.gm" },
      ],
    });
    setActionResult({ action: "contract_flow_saved" });
    reload();
  }

  async function addColorRule() {
    const days = Number(colorForm.thresholdDays);
    if (!colorForm.categoryName.trim() || !Number.isFinite(days) || days <= 0) {
      setGlobalError("着色分类需填写分类名与正整数提前天数");
      return;
    }
    await api.post("/api/v1/contracts/expiry-color-rules", {
      categoryName: colorForm.categoryName.trim(), color: colorForm.color, thresholdDays: days,
    });
    setActionResult({ action: "contract_color_rule_added", category: colorForm.categoryName.trim() });
    setColorForm({ ...colorForm, categoryName: "" });
    reload();
  }

  // 合同整行明细：全字段（含生效/到期，抽成模式，商户下钻）。
  function openContractDetail(row: ContractRow) {
    setDetail({
      title: `合同明细 · ${shortId(row.id)}`,
      fields: [
        { label: "合同号", value: row.id },
        {
          label: "商户",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{merchantNames[row.merchantId] ?? row.merchantId}</TraceLink>,
        },
        { label: "生命周期状态", value: formatBusinessStatus(row.lifecycleStatus) },
        { label: "生效日期", value: cnDate(row.effectiveDate) },
        { label: "到期日期", value: cnDate(row.expiryDate) },
        { label: "抽成模式", value: formatBusinessStatus(row.commissionMode) },
        { label: "所属综合体", value: row.projectId ?? "—" },
      ],
    });
  }

  // 合同变更整行明细：变更号/类型/审批状态（均中文），并挂本合同下钻。
  function openChangeDetail(ch: ChangeRow) {
    setDetail({
      title: `合同变更明细 · ${shortId(ch.id)}`,
      fields: [
        { label: "变更号", value: ch.id },
        {
          label: "所属合同",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={ch.contractId}>{shortId(ch.contractId)}</TraceLink>,
        },
        { label: "变更类型", value: formatBusinessStatus(ch.changeType) },
        { label: "审批状态", value: formatBusinessStatus(ch.approvalStatus) },
      ],
    });
  }

  // 退租结算整行明细：结算单号/触发/状态（均中文），并挂本合同下钻。
  function openSettlementDetail(st: SettlementRow) {
    setDetail({
      title: `退租结算明细 · ${shortId(st.id)}`,
      fields: [
        { label: "结算单号", value: st.id },
        {
          label: "所属合同",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={st.contractId}>{shortId(st.contractId)}</TraceLink>,
        },
        { label: "触发方式", value: formatBusinessStatus(st.triggerType) },
        { label: "结算状态", value: formatBusinessStatus(st.status) },
      ],
    });
  }

  return (
    <section data-testid="contract-workbench-page">
      <AdminPageHeader
        title="合同生命周期工作台"
        description="起草 → 分组并行审批（业务/财务/法务并行 → 总经理终审，按综合体可配）→ 生效固化收益快照 + 绑铺位 → 变更/退租逐例结算。转租不支持。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>合同</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contracts-table">
            <thead><tr><th>合同号</th><th>商户</th><th>状态</th><th>租期</th><th>抽成</th><th className="admin-op-col">操作</th></tr></thead>
            <tbody>
              {contracts.map((row) => (
                <tr key={row.id} data-testid={`contract-${row.id}`}
                  className={["admin-row-clickable", row.id === focusObjectId ? "admin-row-selected" : ""].filter(Boolean).join(" ")}
                  tabIndex={0}
                  onClick={() => openContractDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}>
                  <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td>
                  <td title={row.merchantId} onClick={(event) => event.stopPropagation()}>
                    <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>
                      {merchantNames[row.merchantId] ?? <span className="admin-id-cell">{shortId(row.merchantId)}</span>}
                    </TraceLink>
                  </td>
                  <td>{formatBusinessStatus(row.lifecycleStatus)}</td>
                  <td>{cnDate(row.effectiveDate) + " → " + cnDate(row.expiryDate)}</td>
                  <td>{formatBusinessStatus(row.commissionMode)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {row.lifecycleStatus === "drafting" && (
                      <PermissionButton permissionsState={effectivePermissions} permissions={DRAFT}
                        data-testid={`contract-submit-${row.id}`} onClick={() => void submit(row)}>提交审批</PermissionButton>
                    )}
                    {row.lifecycleStatus === "pending_confirmation" && APPROVE_ROLES.map((r) => (
                      <PermissionButton key={r.code} permissionsState={effectivePermissions} permissions={[r.code]}
                        data-testid={`contract-approve-${r.code}-${row.id}`} onClick={() => void approve(row, r.code)}>{r.label}</PermissionButton>
                    ))}
                    {row.lifecycleStatus === "pending_effective" && (
                      <PermissionButton permissionsState={effectivePermissions} permissions={ACTIVATE}
                        data-testid={`contract-activate-${row.id}`} onClick={() => void activate(row)}>生效(绑铺位)</PermissionButton>
                    )}
                    {row.lifecycleStatus === "effective" && (
                      <>
                        <PermissionButton permissionsState={effectivePermissions} permissions={RENT_AR}
                          data-testid={`contract-period-bills-${row.id}`} onClick={() => void generatePeriodBills(row)}>排租金AR</PermissionButton>
                        <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE}
                          data-testid={`contract-settle-${row.id}`} onClick={() => void settle(row)}>退租开账</PermissionButton>
                      </>
                    )}
                    <button data-testid={`contract-detail-${row.id}`} onClick={() => void selectContract(row.id)}>明细</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="contract-draft-form">
          <input aria-label="商户 id" placeholder="商户 id" value={draft.merchantId} onChange={(e) => setDraft({ ...draft, merchantId: e.target.value })} />
          <input aria-label="合同主体 id" placeholder="合同主体 id" value={draft.contractSubjectId} onChange={(e) => setDraft({ ...draft, contractSubjectId: e.target.value })} />
          <input aria-label="结算主体 id" placeholder="结算主体 id(缺省=合同主体)" value={draft.settlementSubjectId} onChange={(e) => setDraft({ ...draft, settlementSubjectId: e.target.value })} />
          <input aria-label="生效日期" type="date" value={draft.effectiveDate} onChange={(e) => setDraft({ ...draft, effectiveDate: e.target.value })} />
          <input aria-label="到期日期" type="date" value={draft.expiryDate} onChange={(e) => setDraft({ ...draft, expiryDate: e.target.value })} />
          <input aria-label="月租金（元）" type="number" min="0" placeholder="月租金（元）" value={draft.baseRentYuan} onChange={(e) => setDraft({ ...draft, baseRentYuan: e.target.value })} />
          <select aria-label="抽成模式" value={draft.commissionMode} onChange={(e) => setDraft({ ...draft, commissionMode: e.target.value })}>
            <option value="fixed_ratio">固定比例</option>
            <option value="guarantee_or_commission_higher">保底与抽成取高</option>
            <option value="tiered_ratio">阶梯比例</option>
            <option value="guarantee_plus_commission">保底+抽成叠加</option>
          </select>
          <select aria-label="能源费分摊" value={draft.energyFeeBasis} onChange={(e) => setDraft({ ...draft, energyFeeBasis: e.target.value })}>
            <option value="by_area">按面积</option>
            <option value="by_usage">按用量</option>
            <option value="by_contract">按合同约定</option>
            <option value="none">不分摊</option>
          </select>
          <PermissionButton permissionsState={effectivePermissions} permissions={DRAFT}
            data-testid="contract-draft-create" onClick={() => void createDraft()}>起草合同</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="contract-activate-form">
          <input aria-label="生效绑定空间 id" placeholder="生效绑定空间 id" value={spaceId} onChange={(e) => setSpaceId(e.target.value)} />
          <input aria-label="排租金AR账期" type="month" data-testid="contract-bill-period" value={billPeriod} onChange={(e) => setBillPeriod(e.target.value)} />
        </div>

        <h3>审批链（分组并行，按综合体可配）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-flow-table">
            <thead><tr><th>阶段</th><th>阶段名</th><th>审批岗位</th></tr></thead>
            <tbody>
              {flow.map((s, i) => (
                <tr key={i} data-testid={`contract-flow-step-${i}`}>
                  <td>{s.stageSeq}</td><td>{s.stageName}</td><td title={s.approverPermissionCode}>{ROLE_LABELS[s.approverPermissionCode] ?? s.approverPermissionCode}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={FLOW_CFG}
            data-testid="contract-flow-reset" onClick={() => void resetFlowToDefault()}>重置为默认链(①业务/财务/法务并行→②总经理)</PermissionButton>
        </div>

        <h3>铺位到期着色分类</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-color-table">
            <thead><tr><th>分类</th><th>颜色</th><th>提前天数</th></tr></thead>
            <tbody>
              {colorRules.map((r) => (
                <tr key={r.id} data-testid={`contract-color-${r.id}`}>
                  <td>{r.categoryName}</td>
                  <td><span style={{ background: r.color, padding: "0 12px" }}>{r.color}</span></td>
                  <td>{r.thresholdDays}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="contract-color-form">
          <input aria-label="分类名" placeholder="分类名(如 临期红)" value={colorForm.categoryName} onChange={(e) => setColorForm({ ...colorForm, categoryName: e.target.value })} />
          <input aria-label="颜色" type="color" value={colorForm.color} onChange={(e) => setColorForm({ ...colorForm, color: e.target.value })} />
          <input aria-label="提前天数" type="number" min="1" placeholder="提前天数" value={colorForm.thresholdDays} onChange={(e) => setColorForm({ ...colorForm, thresholdDays: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={COLOR_CFG}
            data-testid="contract-color-add" onClick={() => void addColorRule()}>新增着色分类</PermissionButton>
        </div>

        <h3>合同变更（6 类，走同一审批链）{selectedId ? <span className="admin-id-cell" title={selectedId}> · {shortId(selectedId)}</span> : "（点合同『明细』选）"}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-changes-table">
            <thead><tr><th>变更号</th><th>类型</th><th>审批</th><th className="admin-op-col">操作</th></tr></thead>
            <tbody>
              {changes.map((ch) => (
                <tr key={ch.id} data-testid={`contract-change-${ch.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openChangeDetail(ch)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openChangeDetail(ch); } }}>
                  <td className="admin-id-cell" title={ch.id}>{shortId(ch.id)}</td><td>{formatBusinessStatus(ch.changeType)}</td><td>{formatBusinessStatus(ch.approvalStatus)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>{ch.approvalStatus === "pending" && APPROVE_ROLES.map((r) => (
                    <PermissionButton key={r.code} permissionsState={effectivePermissions} permissions={[r.code]}
                      data-testid={`change-approve-${r.code}-${ch.id}`} onClick={() => void approveChange(ch.id, r.code)}>{r.label}</PermissionButton>
                  ))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="contract-change-form">
          <select aria-label="变更类型" value={changeForm.changeType} onChange={(e) => setChangeForm({ ...changeForm, changeType: e.target.value })}>
            {CHANGE_TYPES.map((t) => <option key={t.v} value={t.v}>{t.l}</option>)}
          </select>
          <input aria-label="变更说明" placeholder="变更说明 / payload" value={changeForm.note} onChange={(e) => setChangeForm({ ...changeForm, note: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={CHANGE_MANAGE}
            data-testid="contract-change-raise" onClick={() => void raiseChange()}>发起变更</PermissionButton>
        </div>

        <h3>退租结算（5 态：待结算→保证金抵扣中→欠费追缴中→已结清 / 有争议）</h3>
        <div className="admin-inline-form" data-testid="contract-dispute-form">
          <input aria-label="争议原因" placeholder="争议原因（退租开账时填则挂起『有争议』）" value={disputeReason} onChange={(e) => setDisputeReason(e.target.value)} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-settlements-table">
            <thead><tr><th>结算单号</th><th>触发</th><th>状态</th><th className="admin-op-col">操作</th></tr></thead>
            <tbody>
              {settlements.map((st) => (
                <tr key={st.id} data-testid={`contract-settlement-${st.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openSettlementDetail(st)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(st); } }}>
                  <td className="admin-id-cell" title={st.id}>{shortId(st.id)}</td><td>{formatBusinessStatus(st.triggerType)}</td><td>{formatBusinessStatus(st.status)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {st.status === "disputed" && (
                      <PermissionButton permissionsState={effectivePermissions} permissions={DISPUTE_RESOLVE}
                        data-testid={`settlement-resolve-${st.id}`} onClick={() => void settlementAction(st.id, "resolve-dispute")}>裁决→待结算</PermissionButton>
                    )}
                    {OPEN_SETTLEMENT.includes(st.status) && (
                      <>
                        <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE}
                          data-testid={`settlement-advance-${st.id}`} onClick={() => void settlementAction(st.id, "advance")}>推进一相</PermissionButton>
                        <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE}
                          data-testid={`settlement-complete-${st.id}`} onClick={() => void settlementAction(st.id, "complete")}>结清</PermissionButton>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>铺位平面图（6 态着色）</h3>
        <div data-testid="contract-floorplan" style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {spaceRental.map((sp) => (
            <div key={sp.spaceId} data-testid={`space-${sp.spaceId}`}
              title={`${RENTAL_LABEL[sp.rentalStatus] ?? sp.rentalStatus}${sp.daysToExpiry != null ? ` · ${sp.daysToExpiry} 天到期` : ""}`}
              style={{ minWidth: 96, padding: "8px 10px", borderRadius: 6, color: "#fff",
                background: sp.color ?? RENTAL_COLOR[sp.rentalStatus] ?? "#9ca3af" }}>
              <div style={{ fontWeight: 600 }}>{sp.spaceName}</div>
              <div style={{ fontSize: 12 }}>{RENTAL_LABEL[sp.rentalStatus] ?? sp.rentalStatus}</div>
            </div>
          ))}
          {spaceRental.length === 0 && <span className="admin-muted">暂无铺位</span>}
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
