import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type {
  AdminPageProps,
  GovernanceProjectRow,
  MerchantAdmissionRow,
  PropertyFeeBillRow,
  PropertyFeeRateRow,
} from "../types";

// 后端写权限码（与 FinanceController /property-fee/* requirePermission 对齐）。
const MANAGE_PERMS = ["finance.property_fee.manage"];

const TEXT = {
  title: "物业费管理",
  description:
    "独立设置物业费费率（每平米固定单价）并按期逐户出账：不再依赖合同期账单捆绑生成。计费按「单价 × 商户面积」派生应收，不退不补、按期幂等。",
  loadFailed: "物业费费率读取失败",
  rateFailed: "物业费费率保存失败",
  billFailed: "物业费出账失败",
  projectRequired: "请先选择项目公司。",
  rateRequired: "每平米单价必须是不小于 0 的数字。",
  periodRequired: "请填写计费期（如 2026-07）。",
  merchantsRequired: "请至少勾选一个商户。",
  saveRate: "保存费率",
  runBill: "生成物业费账单",
};

// 分 ↔ 元：展示两位小数（等宽数字由 tabular-nums 保证），缺失不伪造。
function centToYuan(cent: number | undefined): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

function projectLabel(project: GovernanceProjectRow): string {
  return project.name ?? project.code ?? project.id;
}

function merchantLabel(merchant: MerchantAdmissionRow): string {
  return merchant.merchantName ?? merchant.displayName ?? merchant.companyName ?? merchant.id;
}

export function FinancePropertyFeePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [merchantNotice, setMerchantNotice] = React.useState<string | null>(null);
  const [rates, setRates] = React.useState<PropertyFeeRateRow[]>([]);
  // 本次会话生成的账单（后端无账单列表端点；历史账单在「应收应付台账」按来源=物业费查看）。
  const [bills, setBills] = React.useState<PropertyFeeBillRow[]>([]);

  const [projectId, setProjectId] = React.useState("");
  const [effectivePeriod, setEffectivePeriod] = React.useState(""); // 空=该项目默认费率
  const [rateYuan, setRateYuan] = React.useState(""); // 每平米单价（元）
  const [billPeriod, setBillPeriod] = React.useState("");
  const [selectedMerchants, setSelectedMerchants] = React.useState<Record<string, boolean>>({});

  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-8：新增物业费率收起为主按钮 + 分步引导弹窗（账单生成是动作，不改）。
  const [rateOpen, setRateOpen] = React.useState(false);

  // 项目 + 商户名单（下拉/勾选用）——一次拉齐；费率随所选项目按需刷新。
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：商户名单仅供出账勾选，可能因权限/500 单独失败——降级为区块提示，
      // 不拖垮费率维护主内容（项目为主资源，读失败才整页报错）。
      const [projectRows, merchantRes] = await Promise.all([
        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
        loadSecondary(() => api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"), [] as MerchantAdmissionRow[]),
      ]);
      setProjects(projectRows);
      setMerchants(merchantRes.data);
      setMerchantNotice(merchantRes.notice);
      if (!projectId && projectRows[0]) setProjectId(projectRows[0].id);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  // 所选项目变化 → 拉该项目费率列表（GET /property-fee/rates?project_id=）。
  const loadRates = React.useCallback(
    async (pid: string) => {
      if (!pid) {
        setRates([]);
        return;
      }
      const rows = await api.get<PropertyFeeRateRow[]>(
        `/api/v1/finance/property-fee/rates?project_id=${encodeURIComponent(pid)}`,
      );
      setRates(rows);
    },
    [api],
  );

  React.useEffect(() => {
    void loadRates(projectId).catch((e) => setGlobalError(e instanceof Error ? e.message : TEXT.loadFailed));
  }, [projectId, loadRates, setGlobalError]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: () => loadRates(projectId),
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.rateFailed,
  });

  async function saveRate() {
    if (!projectId) {
      setFormError(TEXT.projectRequired);
      return;
    }
    const numericRate = Number(rateYuan);
    if (!Number.isFinite(numericRate) || numericRate < 0) {
      setFormError(TEXT.rateRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("property-fee-rate"),
      projectId,
      effectivePeriod: effectivePeriod.trim() || null,
      ratePerSquareMeterCent: Math.round(numericRate * 100),
      status: "active",
    };
    const ok = await runAction("property_fee_rate_saved", request,
      () => api.post("/api/v1/finance/property-fee/rates", request));
    // 成功才关弹窗并清单价；失败留弹窗内显错（formError 已由 runAction 置）。
    if (ok) {
      setRateYuan("");
      setRateOpen(false);
    }
  }

  async function runBill() {
    if (!projectId) {
      setFormError(TEXT.projectRequired);
      return;
    }
    if (!billPeriod.trim()) {
      setFormError(TEXT.periodRequired);
      return;
    }
    const merchantIds = Object.entries(selectedMerchants).filter(([, on]) => on).map(([id]) => id);
    if (merchantIds.length === 0) {
      setFormError(TEXT.merchantsRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("property-fee-bill"),
      operatorNote: "后台【物业费管理】独立出账",
      evidenceAttachmentIds: ["att-property-fee-bill"],
      projectId,
      period: billPeriod.trim(),
      merchantIds,
    };
    setGlobalError(null);
    try {
      const generated = await api.post<PropertyFeeBillRow[]>("/api/v1/finance/property-fee/bills", request);
      setBills(Array.isArray(generated) ? generated : []);
      setActionResult(makeResult("property_fee_billed", request, generated));
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.billFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "property_fee_billed", status: "failed", message });
    }
  }

  function openRateDetail(row: PropertyFeeRateRow) {
    setDetail({
      title: `物业费费率明细 · ${row.id}`,
      fields: [
        { label: "项目公司", value: projects.find((p) => p.id === row.projectId) ? projectLabel(projects.find((p) => p.id === row.projectId)!) : row.projectId ?? "—" },
        { label: "生效期", value: row.effectivePeriod ? row.effectivePeriod : "默认费率（未命中期时回退）" },
        { label: "每平米单价", value: `¥${centToYuan(row.ratePerSquareMeterCent)} / ㎡ / 期` },
        { label: "状态", value: row.status === "active" ? "生效" : (row.status ?? "—") },
        { label: "创建时间", value: cnDate(row.createdAt) },
        { label: "费率编号", value: row.id },
      ],
    });
  }

  function openBillDetail(row: PropertyFeeBillRow) {
    setDetail({
      title: `物业费账单明细 · ${row.id}`,
      fields: [
        { label: "计费期", value: row.period ?? "—" },
        { label: "商户", value: merchants.find((m) => m.id === row.merchantId) ? merchantLabel(merchants.find((m) => m.id === row.merchantId)!) : row.merchantId ?? "—" },
        { label: "计费面积", value: row.areaSquareMeter !== undefined ? `${row.areaSquareMeter} ㎡` : "—" },
        { label: "每平米单价", value: `¥${centToYuan(row.ratePerSquareMeterCent)}` },
        { label: "应收金额", value: `¥${centToYuan(row.amountCent)}` },
        { label: "台账单据", value: row.arApItemId ?? "—" },
        { label: "账单编号", value: row.id },
      ],
    });
  }

  const activeMerchants = merchants.filter((m) => m.status === "active" || m.status === undefined);

  return (
    <section data-testid="finance-property-fee-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="finance_property_fee.rate_set / finance_property_fee.billed" />

      {/* ── 费率设置 ───────────────────────────────────────────── */}
      {/* 项目公司选择器留在页面（既是下方费率表的筛选，也是新增费率的归属，弹窗内复用同一 projectId）。 */}
      <h3>物业费费率设置</h3>
      <div className="admin-form-row" data-testid="property-fee-rate-form">
        <label>项目公司<select data-testid="property-fee-project" value={projectId} onChange={(event) => setProjectId(event.target.value)}>
          {projects.length === 0 ? <option value="">（暂无项目）</option> : null}
          {projects.map((project) => <option key={project.id} value={project.id}>{projectLabel(project)}</option>)}
        </select></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="property-fee-rate-create-open"
          onClick={() => { setFormError(""); setRateOpen(true); }}
        >＋ 新增物业费率</PermissionButton>
      </div>

      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="property-fee-rates-table">
            <thead><tr><th>生效期</th><th>每平米单价（元）</th><th>状态</th><th>创建时间</th><th>详情</th></tr></thead>
            <tbody>
              {rates.length === 0 ? (
                <tr data-testid="property-fee-rates-empty"><td colSpan={5} className="admin-muted-action">该项目暂无物业费费率，请先在上方设置。</td></tr>
              ) : rates.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`property-fee-rate-row-${row.id}`}
                  onClick={() => openRateDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRateDetail(row); } }}>
                  <td>{row.effectivePeriod ? row.effectivePeriod : "默认"}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.ratePerSquareMeterCent)}</td>
                  <td>{row.status === "active" ? "生效" : (row.status ?? "—")}</td>
                  <td>{cnDate(row.createdAt)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看物业费费率详情" onClick={(event) => { event.stopPropagation(); openRateDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>

      {/* ── 账单生成 ───────────────────────────────────────────── */}
      <h3>物业费账单生成</h3>
      <div className="admin-form-row" data-testid="property-fee-bill-form">
        <label>计费期<input data-testid="property-fee-bill-period" type="month" value={billPeriod} onChange={(event) => setBillPeriod(event.target.value)} /></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="property-fee-bill-run"
          disabled={busyAction === "property_fee_billed"}
          onClick={runBill}
        >{TEXT.runBill}</PermissionButton>
        <span className="admin-muted-action">按「所选项目当期费率 × 商户面积」逐户挂应收；不退不补、同一(项目·期·商户)重复出账自动跳过。</span>
      </div>
      {merchantNotice ? (
        <div className="admin-state" data-testid="property-fee-merchants-degraded">{merchantNotice}</div>
      ) : null}
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="property-fee-merchants-table">
          <thead><tr><th>选择</th><th>商户</th><th>计费面积（㎡）</th><th>状态</th></tr></thead>
          <tbody>
            {activeMerchants.length === 0 ? (
              <tr><td colSpan={4} className="admin-muted-action">暂无商户。</td></tr>
            ) : activeMerchants.map((m) => (
              <tr key={m.id} data-testid={`property-fee-merchant-row-${m.id}`}>
                <td><input type="checkbox" aria-label={`勾选商户 ${merchantLabel(m)}`} data-testid={`property-fee-merchant-select-${m.id}`}
                  checked={!!selectedMerchants[m.id]}
                  onChange={(event) => setSelectedMerchants((prev) => ({ ...prev, [m.id]: event.target.checked }))} /></td>
                <td>{merchantLabel(m)}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{m.leasedAreaSquareMeter ?? m.areaSquareMeter ?? "—"}</td>
                <td>{m.status === "active" ? "生效" : (m.status ?? "—")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {bills.length > 0 ? (
        <>
          <h4 data-testid="property-fee-bills-heading">本次生成账单（{bills.length} 户）</h4>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="property-fee-bills-table">
              <thead><tr><th>计费期</th><th>商户</th><th>面积（㎡）</th><th>应收金额（元）</th><th>台账单据</th><th>详情</th></tr></thead>
              <tbody>
                {bills.map((row) => (
                  <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`property-fee-bill-row-${row.id}`}
                    onClick={() => openBillDetail(row)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBillDetail(row); } }}>
                    <td>{row.period ?? "—"}</td>
                    <td>{merchants.find((m) => m.id === row.merchantId) ? merchantLabel(merchants.find((m) => m.id === row.merchantId)!) : row.merchantId ?? "—"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{row.areaSquareMeter ?? "—"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.amountCent)}</td>
                    <td>{row.arApItemId ?? "—"}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看物业费账单详情" onClick={(event) => { event.stopPropagation(); openBillDetail(row); }}>查看详情</button></td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="admin-muted-action" data-testid="property-fee-bills-note">历史物业费账单可在「应收应付台账」按来源「物业费」查询（本页仅显示本次生成结果）。</p>
        </>
      ) : null}

      {formError ? <div className="admin-form-error" data-testid="property-fee-form-error">{formError}</div> : null}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={rateOpen}
        title="新增物业费率"
        testId="property-fee-rate-guided"
        submitTestId="property-fee-rate-save"
        submitLabel={TEXT.saveRate}
        submitting={busyAction === "property_fee_rate_saved"}
        error={formError || undefined}
        onClose={() => setRateOpen(false)}
        onSubmit={saveRate}
        steps={[
          {
            title: "费率信息",
            description: `归属项目：${projects.find((p) => p.id === projectId) ? projectLabel(projects.find((p) => p.id === projectId)!) : "请先在页面选择项目公司"}。生效期留空=该项目默认费率；单价按每平米元填写（不小于 0）。`,
            validate: () => {
              if (!projectId) return TEXT.projectRequired;
              const numericRate = Number(rateYuan);
              if (!Number.isFinite(numericRate) || numericRate < 0) return TEXT.rateRequired;
              return null;
            },
            content: (
              <>
                <label>生效期（空=默认费率）<input data-testid="property-fee-rate-period" type="month" value={effectivePeriod} onChange={(event) => setEffectivePeriod(event.target.value)} /></label>
                <label>每平米单价（元/㎡/期）<input data-testid="property-fee-rate-amount" type="number" min={0} step="0.01" value={rateYuan} onChange={(event) => setRateYuan(event.target.value)} placeholder="如 25.00" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "归属项目", value: projects.find((p) => p.id === projectId) ? projectLabel(projects.find((p) => p.id === projectId)!) : (projectId || "未选择") },
          { label: "生效期", value: effectivePeriod.trim() || "默认费率" },
          { label: "每平米单价", value: rateYuan.trim() ? `¥${rateYuan.trim()} / ㎡ / 期` : "未填写" },
        ]}
      />
    </section>
  );
}
