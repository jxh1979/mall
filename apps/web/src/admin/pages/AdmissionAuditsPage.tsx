// 文件版本：V0.05
import React from "react";
import { formatActionResult } from "../components/ActionResultView";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, ContractRow, MerchantAdmissionRow, SubjectBindingRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { firstSubmitTime, forgetSubmitTime } from "../../shared/operationKey";

const EVIDENCE_CREATE = ["evidence.event.create"];

const TEXT = {
  title: "商户准入审批流水",
  description: "把商户档案、四类主体绑定、合同草稿/审批三类准入证据放在同一条审计链路里，支撑总控、法务、财务和运营共同验收。",
  evidence: "接口证据：读取 /api/v1/platform/merchants、/api/v1/platform/business-entity-bindings、/api/v1/contracts；导出写入 /api/v1/evidence/events。",
  boundary: "商户准入必须同时核验商户档案、四类主体绑定、合同审批与附件归档；任何一段缺失都不能视为准入闭环完成。",
  loadFailed: "准入流水读取失败",
  exportReasonRequired: "导出原因不能为空。",
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<merchants|bindings|contracts>；点 KPI 数字=带参重进本页只渲染对应表。
// 三类资料关联只表示同一商户存在档案、绑定和合同记录，不代表审批通过。
type KpiTarget = "merchants" | "bindings" | "contracts";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "merchants" && table !== "bindings" && table !== "contracts") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  if (focus.table === "bindings") return "主体绑定";
  if (focus.table === "contracts") return "合同审批";
  return "商户档案";
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return formatActionResult(value);
}

function byMerchant<T extends { merchantId?: string; id?: string }>(rows: T[], merchantId: string) {
  return rows.filter((row) => row.merchantId === merchantId || row.id === merchantId);
}

function contractAttachments(row: ContractRow) {
  return row.attachmentIds?.length ? row.attachmentIds.join(" / ") : row.evidenceAttachmentIds?.join(" / ") ?? "-";
}

export function AdmissionAuditsPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, reportViewedObjects }: AdminPageProps) {
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("merchant", merchants), [merchants, reportViewedObjects]);
  // 第 32 条：三类统计数取后端分页 total（列表只拿到一页，按条数算会少）。
  const [totals, setTotals] = React.useState({ merchants: 0, bindings: 0, contracts: 0 });
  const [bindings, setBindings] = React.useState<SubjectBindingRow[]>([]);
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [exportReason, setExportReason] = React.useState("完整后台准入流水导出");
  const [formError, setFormError] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·日期中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-9：登记证据事件收起为主按钮 + 分步引导弹窗（KPI 聚焦/导出原因分步核对后提交）。
  const [exportOpen, setExportOpen] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 跨域副资源不请求（根因修）：合同列表端点需 contract.draft.manage，本页基础权限仅 contract.view——
      // 缺该权限角色命中 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、合同交叉引用降级为空。
      const canContractDraft = hasPermissions(effectivePermissions, ["contract.draft.manage"]);
      const [nextMerchants, nextBindings, nextContracts] = await Promise.all([
        api.getPaged<MerchantAdmissionRow>("/api/v1/platform/merchants"),
        api.getPaged<SubjectBindingRow>("/api/v1/platform/business-entity-bindings"),
        canContractDraft ? api.getPaged<ContractRow>("/api/v1/contracts") : Promise.resolve({ items: [] as ContractRow[], total: 0 }),
      ]);
      setMerchants(nextMerchants.items);
      setBindings(nextBindings.items);
      setContracts(nextContracts.items);
      setTotals({ merchants: nextMerchants.total, bindings: nextBindings.total, contracts: nextContracts.total });
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const merchantIds = new Set(merchants.map((row) => row.id).filter(Boolean));
  const bindingMerchantIds = new Set(bindings.map((row) => row.merchantId).filter(Boolean));
  const contractMerchantIds = new Set(contracts.map((row) => row.merchantId).filter(Boolean));
  // 第 32 条：三类资料关联与草稿数只能按本页已加载的行算（后端只出三类各自总数），页面上标明「本页」。
  const linkedMaterialsCount = [...merchantIds].filter((id) => bindingMerchantIds.has(id) && contractMerchantIds.has(id)).length;
  const draftCount = contracts.filter((row) => ["draft", "pending", "returned"].includes(row.lifecycleStatus ?? row.status ?? "")).length;

  async function exportAdmissionAudit() {
    if (!exportReason.trim()) {
      setFormError(TEXT.exportReasonRequired);
      return;
    }
    setFormError("");
    setGlobalError(null);
    const eventPayload = {
      merchantCount: totals.merchants,
      bindingCount: totals.bindings,
      contractCount: totals.contracts,
      linkedMaterialsCount,
    };
    // M725-F02：导出发生时间在这次登记第一次提交时取一次，登记成功之前同样的导出内容都沿用（切页回来、中间登记过别的也一样）——
    // 没拿到结果再点时请求内容逐字不变，客户端才认得出是同一次操作、沿用首发的提交编号，不会再登记一条。
    const operation = ["admission-audit-export", exportReason, eventPayload];
    const payload = {
      idempotencyKey: `admission-audit-export-${Date.now()}`,
      // 第 32 条：本次导出没有上传文件，不带附件；登记人取当前登录账号（与审计事件页导出同口径）。
      deviceCode: "web-admin",
      staffId: user.id,
      events: [{
        eventType: "merchant.admission.audit_exported",
        eventCategory: "governance",
        result: "success",
        summary: exportReason,
        payload: eventPayload,
        occurredAt: firstSubmitTime(operation),
      }],
    };
    // 提交失败留弹窗内显错（formError），成功才关闭弹窗（照 phase-8 范式）。
    setExporting(true);
    try {
      const result = await api.post<unknown>("/api/v1/evidence/events", payload);
      forgetSubmitTime(operation);
      setActionResult({
        action: "admission_audit_exported",
        audit: "merchant_admission_trace_ready",
        request: payload,
        result,
      });
      setExportOpen(false);
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "登记证据事件失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "admission_audit_exported", status: "failed", message });
    } finally {
      setExporting(false);
    }
  }

  function openMerchantDetail(row: MerchantAdmissionRow) {
    setDetail({
      title: `商户档案明细 · ${display(row.merchantName)}`,
      fields: [
        { label: "商户", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.id}>{display(row.merchantName)}</TraceLink> },
        { label: "联系人", value: display(row.contactName) },
        { label: "联系电话", value: display(row.contactPhone) },
        { label: "区块", value: display(row.mallBlockCode) },
        { label: "面积", value: display(row.areaSquareMeter) },
        { label: "合作形式", value: formatBusinessStatus(row.cooperationForm) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "商户编号", value: display(row.id) },
      ],
    });
  }

  function openBindingDetail(row: SubjectBindingRow) {
    setDetail({
      title: `主体绑定明细 · ${display(row.merchantId)}`,
      fields: [
        { label: "商户编号", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> },
        { label: "运营主体编号", value: display(row.operationEntityId) },
        { label: "合同主体编号", value: display(row.contractEntityId) },
        { label: "结算主体编号", value: display(row.settlementEntityId) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "生效起", value: cnDate(row.effectiveFrom) },
        { label: "生效止", value: cnDate(row.effectiveTo) },
        { label: "变更原因", value: display(row.changeReason) },
        { label: "绑定编号", value: display(row.id) },
      ],
    });
  }

  function openContractDetail(row: ContractRow) {
    setDetail({
      title: `合同草稿/审批明细 · ${display(row.merchantId)}`,
      fields: [
        { label: "商户", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> },
        { label: "合同主体", value: display(row.contractSubjectId) },
        { label: "运营主体", value: display(row.operationSubjectId) },
        { label: "结算主体", value: display(row.settlementSubjectId) },
        { label: "生效日", value: cnDate(row.effectiveDate) },
        { label: "到期日", value: cnDate(row.expiryDate) },
        { label: "附件", value: contractAttachments(row) },
        { label: "状态", value: formatBusinessStatus(row.lifecycleStatus ?? row.status) },
        { label: "审批复核单", value: display(row.reviewId) },
      ],
    });
  }

  const merchantsSection = (
    <>
      <div className="admin-section-title">商户档案</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="admission-audits-merchants-table">
          <thead><tr><th>商户</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{merchants.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`admission-audits-merchant-row-${row.id}`} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.id}>{display(row.merchantName)}</TraceLink></td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.areaSquareMeter)}</td><td>{formatBusinessStatus(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>
          ))}</tbody>
        </table>
      </div>
    </>
  );

  const bindingsSection = (
    <>
      <div className="admin-section-title">主体绑定</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="admission-audits-bindings-table">
          <thead><tr><th>商户编号</th><th>运营主体编号</th><th>合同主体编号</th><th>结算主体编号</th><th>状态</th><th>生效起</th><th>生效止</th><th>变更原因</th><th>绑定编号</th><th>详情</th></tr></thead>
          <tbody>{bindings.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`admission-audits-binding-row-${row.id}`} onClick={() => openBindingDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openBindingDetail(row); } }}><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink></td><td>{display(row.operationEntityId)}</td><td>{display(row.contractEntityId)}</td><td>{display(row.settlementEntityId)}</td><td>{formatBusinessStatus(row.status)}</td><td>{cnDate(row.effectiveFrom)}</td><td>{cnDate(row.effectiveTo)}</td><td>{display(row.changeReason)}</td><td>{display(row.id)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看主体绑定详情" onClick={(event) => { event.stopPropagation(); openBindingDetail(row); }}>查看详情</button></td></tr>
          ))}</tbody>
        </table>
      </div>
    </>
  );

  const contractsSection = (
    <>
      <div className="admin-section-title">合同草稿/审批</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="admission-audits-contracts-table">
          <thead><tr><th>商户</th><th>合同主体</th><th>运营主体</th><th>结算主体</th><th>生效日</th><th>到期日</th><th>附件</th><th>状态</th><th>审批复核单</th><th>详情</th></tr></thead>
          <tbody>{contracts.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`admission-audits-contract-row-${row.id}`} onClick={() => openContractDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink></td><td>{display(row.contractSubjectId)}</td><td>{display(row.operationSubjectId)}</td><td>{display(row.settlementSubjectId)}</td><td>{cnDate(row.effectiveDate)}</td><td>{cnDate(row.expiryDate)}</td><td>{contractAttachments(row)}</td><td>{formatBusinessStatus(row.lifecycleStatus ?? row.status)}</td><td>{display(row.reviewId)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看合同草稿/审批详情" onClick={(event) => { event.stopPropagation(); openContractDetail(row); }}>查看详情</button></td></tr>
          ))}</tbody>
        </table>
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "bindings" ? totals.bindings : kpiFocus.table === "contracts" ? totals.contracts : totals.merchants) : 0;

  return (
    <section data-testid="admission-audits-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="admission-audits-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="merchant_admission.viewed / merchant_admission.audit_exported" />
      {formError ? <div className="admin-inline-error" data-testid="admission-audits-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {merchants.length === 0 && bindings.length === 0 && contracts.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="admission-audits-empty">暂无商户准入流水</div>
        ) : kpiFocus ? (
          <>
            <div className="admin-state" data-testid="admission-audits-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{focusCount}）
              <button type="button" data-testid="admission-audits-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "bindings" ? bindingsSection : kpiFocus.table === "contracts" ? contractsSection : merchantsSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="admission-audits-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户档案</span><button type="button" className="admin-kpi-value" data-testid="admission-audits-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{totals.merchants}</button><small>档案证据</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>主体绑定</span><button type="button" className="admin-kpi-value" data-testid="admission-audits-kpi-bindings" onClick={() => enterKpiFocus("bindings")}>{totals.bindings}</button><small>四类主体</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>合同审批</span><button type="button" className="admin-kpi-value" data-testid="admission-audits-kpi-contracts" onClick={() => enterKpiFocus("contracts")}>{totals.contracts}</button><small>本页 {draftCount} 个草稿</small></div>
              <div className="admin-kpi-card"><span>三类资料关联</span><strong>{linkedMaterialsCount}</strong><small>按本页已加载的档案+绑定+合同计，不代表审批通过</small></div>
            </div>

            <div className="admin-form-row">
              <PermissionButton permissionsState={effectivePermissions} permissions={EVIDENCE_CREATE} data-testid="admission-audits-create-open" onClick={() => { setFormError(""); setExportOpen(true); }}>＋ 登记证据事件</PermissionButton>
              <span className="admin-muted-action" data-testid="admission-audits-boundary">{TEXT.boundary}</span>
            </div>

            {merchantsSection}
            {bindingsSection}
            {contractsSection}

            <div className="admin-muted-action" data-testid="admission-audits-cross-check">
              已关联商户 {merchants.map((row) => `${row.id}:${byMerchant(contracts, row.id).length} 份合同`).join("；") || "-"}
            </div>
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={exportOpen}
        title="登记证据事件"
        testId="admission-audits-guided"
        submitTestId="admission-audits-export"
        submitLabel="登记并导出"
        submitting={exporting}
        error={formError || undefined}
        onClose={() => setExportOpen(false)}
        onSubmit={() => void exportAdmissionAudit()}
        steps={[
          {
            title: "导出原因",
            description: `将当前商户档案（${totals.merchants}）、主体绑定（${totals.bindings}）、合同审批（${totals.contracts}）与三类资料关联（本页 ${linkedMaterialsCount}）打包登记为一条证据事件。请填写导出原因，作为审计留痕。`,
            validate: () => (exportReason.trim() ? null : TEXT.exportReasonRequired),
            content: (
              <label>导出原因<input data-testid="admission-audits-export-reason" value={exportReason} onChange={(event) => setExportReason(event.target.value)} /></label>
            ),
          },
        ]}
        summary={[
          { label: "导出原因", value: exportReason.trim() || "未填写" },
          { label: "商户档案", value: `${totals.merchants} 户` },
          { label: "主体绑定", value: `${totals.bindings} 条` },
          { label: "合同审批", value: `${totals.contracts} 份` },
          { label: "三类资料关联", value: `本页 ${linkedMaterialsCount} 户` },
        ]}
      />
    </section>
  );
}
