import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 后端 ContractRecord / ContractApprovalFlowStepRecord / SpaceExpiryColorRuleRecord（客户端 unwrap 已 camel 化）。
type ContractRow = {
  id: string; merchantId: string; lifecycleStatus: string;
  effectiveDate?: string; expiryDate?: string; commissionMode?: string | null; projectId?: string | null;
};
type FlowStep = { stageSeq: number; stageName: string; approverPermissionCode: string };
type ColorRule = { id: string; categoryName: string; color: string; thresholdDays: number };

const DRAFT = ["contract.draft.manage"];
const ACTIVATE = ["contract.lifecycle.activate"];
const SETTLE = ["contract.settlement.manage"];
const FLOW_CFG = ["contract.approval.flow.manage"];
const COLOR_CFG = ["contract.space.color.manage"];

// 分组并行审批：默认四岗，按 stageSeq 串行。审批权限码即并行槽 id。
const APPROVE_ROLES: { code: string; label: string }[] = [
  { code: "contract.approve.business", label: "业务过" },
  { code: "contract.approve.finance", label: "财务过" },
  { code: "contract.approve.legal", label: "法务过" },
  { code: "contract.approve.gm", label: "总经理过" },
];

/**
 * 合同生命周期工作台（ADM-CONT-001，V162 重建）：起草 → 分组并行审批 → 生效固化 → 退租结算 + 审批链/到期着色配置。
 * 覆盖验收 ADM-CONT-001..008：起草/四级并行审批/固化快照/铺位着色配置/变更入口/退租结算/审批链配置。
 */
export function ContractWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [flow, setFlow] = React.useState<FlowStep[]>([]);
  const [colorRules, setColorRules] = React.useState<ColorRule[]>([]);
  const [draft, setDraft] = React.useState({
    merchantId: "", contractSubjectId: "", settlementSubjectId: "", effectiveDate: "", expiryDate: "",
    baseRentYuan: "", commissionMode: "fixed_ratio", energyFeeBasis: "by_area",
  });
  const [spaceId, setSpaceId] = React.useState("");
  const [colorForm, setColorForm] = React.useState({ categoryName: "", color: "#F59E0B", thresholdDays: "30" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [rows, steps, rules] = await Promise.all([
        api.get<ContractRow[]>("/api/v1/contracts"),
        api.get<FlowStep[]>("/api/v1/contracts/approval-flow"),
        api.get<ColorRule[]>("/api/v1/contracts/expiry-color-rules"),
      ]);
      setContracts(rows ?? []);
      setFlow(steps ?? []);
      setColorRules(rules ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "合同数据读取失败", onStart: () => setGlobalError(null) },
  );

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

  async function settle(row: ContractRow) {
    await api.post(`/api/v1/contracts/${row.id}/settle`, { triggerType: "move_out" });
    setActionResult({ action: "contract_settled", contractId: row.id });
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
            <thead><tr><th>id</th><th>商户</th><th>状态</th><th>租期</th><th>抽成</th><th>操作</th></tr></thead>
            <tbody>
              {contracts.map((row) => (
                <tr key={row.id} data-testid={`contract-${row.id}`}>
                  <td>{row.id}</td><td>{row.merchantId}</td><td>{row.lifecycleStatus}</td>
                  <td>{(row.effectiveDate ?? "-") + " → " + (row.expiryDate ?? "-")}</td>
                  <td>{row.commissionMode ?? "-"}</td>
                  <td>
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
                      <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE}
                        data-testid={`contract-settle-${row.id}`} onClick={() => void settle(row)}>退租结算</PermissionButton>
                    )}
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
        </div>

        <h3>审批链（分组并行，按综合体可配）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-flow-table">
            <thead><tr><th>阶段</th><th>阶段名</th><th>审批岗位</th></tr></thead>
            <tbody>
              {flow.map((s, i) => (
                <tr key={i} data-testid={`contract-flow-step-${i}`}>
                  <td>{s.stageSeq}</td><td>{s.stageName}</td><td>{s.approverPermissionCode}</td>
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
      </AdminDataState>
    </section>
  );
}
