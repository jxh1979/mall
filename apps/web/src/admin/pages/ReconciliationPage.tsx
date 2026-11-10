import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import type { AdminPageProps, ReconciliationBatchRow, ReconciliationDifferenceRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { hasPermissions } from "../security/PermissionGate";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { FinanceDailyClosePanel } from "./FinanceDailyClosePanel";
import { FinancePeriodClosePanel } from "./FinancePeriodClosePanel";
import { BarShiftPanel } from "./BarShiftPanel";

const RESOLVE_PERM = ["finance.reconciliation.resolve"];
const REGISTER_PERM = ["finance.reconciliation.difference.create"];
// 第十八批 Y 段复核修复（ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证」）：登记差异与提交复核两处上传控件
// 除照片、视频外另收渠道导出的 PDF、Excel、CSV 原件。
const STATEMENT_ACCEPT = "image/*,video/*,.pdf,.xlsx,.csv";
// 第十八批 Y 段第 180 条 180-R2（ENUM:189「金额差异、数量差异、状态差异、缺失差异（单边缺失）」；BSM:1746「平台侧金额必须系统自动取数、不许手填；
// 第三方侧接入前可人工录入但须标记并强制复核」）：差异类型与来源中文名，与后端四类、system/manual 一致。
const DIFFERENCE_TYPES: Record<string, string> = { amount: "金额差异", count: "数量差异", status: "状态差异", missing: "缺失差异" };
const SOURCES: Record<string, string> = { system: "系统取数", manual: "人工录入" };
const EMPTY_REGISTRATION = { batchId: "", differenceType: "amount", businessRef: "", actualYuan: "" };
const labelOf = (labels: Record<string, string>, code?: string) => labels[code ?? ""] ?? code ?? "-";
const scopeText = (batch?: ReconciliationBatchRow) =>
  batch ? `${KEY_VALUE_LABELS.scope_type?.[batch.scopeType ?? ""] ?? batch.scopeType ?? "-"} ${batch.scopeId ?? ""}`.trim() : "-";
const stageTime = (value?: string | null) => (value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-");
// 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限」）：
// 停留时限按项目、按阶段配置，持 finance.reconciliation.policy.manage 才显示。
const POLICY_PERM = ["finance.reconciliation.policy.manage"];
const LIMIT_STAGES: Record<string, string> = { pending_identification: "待识别", pending_handling: "待处理" };
const EMPTY_LIMIT = { projectId: "", stage: "pending_identification", workingDays: "", minWorkingDays: "", maxWorkingDays: "" };
type StageLimit = { projectId: string; stage: string; workingDays: number; minWorkingDays?: number | null; maxWorkingDays?: number | null };
const limitText = (limits: StageLimit[]) => limits.map((item) => `${labelOf(LIMIT_STAGES, item.stage)}：${item.workingDays} 个工作日${item.minWorkingDays == null
  ? "（未配置，按出厂默认）" : `（下限 ${item.minWorkingDays}，上限 ${item.maxWorkingDays}）`}`).join("；");

const TEXT = {
  title: "对账差异",
  description: "查看对账批次和差异、登记差异；差异按待识别、待处理、待复核、已关闭推进：识别写差异原因，处理后填处理结论提交复核，主管复核通过才关闭。平台侧金额由系统取数；第三方侧接入前可人工录入，须同时上传渠道对账单原件。",
  evidence: "接口证据：读取 batches/differences；登记差异调用 POST /api/v1/finance/reconciliation/differences；识别调用 POST /api/v1/finance/reconciliation/differences/{id}/identify；提交复核调用 POST /api/v1/finance/reconciliation/differences/{id}/close。",
  loadFailed: "对账数据读取失败",
  reasonRequired: "提交复核必须填写处理结论。",
  causeRequired: "识别必须填写差异原因。",
  batches: "对账批次",
  differences: "对账差异",
  unresolved: "未处理",
  amount: "差异金额",
  reason: "处理结论",
  attachment: "补充凭证（渠道对账单原件等，支持 PDF、Excel、CSV 原件）",
  finalApprover: "最终审批人（可不填，默认直接上级）",
  submit: "提交复核",
  identify: "识别",
  cause: "差异原因",
  reviewing: "复核中，到主管复核页审批",
  gotoReview: "去主管复核页",
  responsible: "负责人",
  stageEnteredAt: "本阶段起始时间",
  boundary: "差异按待识别、待处理、待复核、已关闭推进；提交复核后由主管复核页审批，复核通过才关闭，驳回退回待处理。",
  batchNo: "批次号",
  differenceNo: "差异号",
  status: "状态",
  businessDate: "营业日",
  scope: "范围",
  differenceType: "差异类型",
  businessRef: "业务单号",
  // 180-R3（ENUM:188「按订单」）：平台侧按收款记录、票务订单或酒吧账单系统取数。
  businessRefInput: "业务单号（收款记录号、票务订单号或酒吧账单号）",
  expected: "平台侧金额",
  actual: "第三方侧金额",
  diff: "差额",
  source: "来源",
  register: "登记差异",
  statement: "渠道对账单原件（支持 PDF、Excel、CSV 原件）",
  registerRequired: "请选择批次，填写业务单号与第三方侧金额（元，不小于 0），并上传渠道对账单原件。",
  selected: "选中差异",
  focusClear: "清除筛选，返回全部",
  limitTitle: "差异停留时限（按项目）",
  limitProject: "项目编号",
  limitStage: "阶段",
  limitDays: "停留时限（工作日）",
  limitMin: "时限下限（工作日）",
  limitMax: "时限上限（工作日）",
  limitQuery: "查询",
  limitSave: "保存停留时限",
  limitHint: "填写项目编号后查询；未配置的阶段按出厂默认 1 个工作日。时限与上下限须同时填写，超时未推进的自动提醒并升级到上一级负责人。",
  limitProjectRequired: "请填写项目编号",
  escalated: "（已超时升级）",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点承载表 KPI 数字=带 ?kpi= 参数重进本页，
// 只渲染对应那张承载表，感知上=新页面。对账两张承载表：batches=对账批次、differences=对账差异。
// 差异金额/选中差异两卡为聚合/状态值，本页无独立承载表→保持不可点。
type KpiTarget = "batches" | "differences";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "batches" && table !== "differences") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "batches" ? TEXT.batches : TEXT.differences;
}

function amountValue(value: unknown) {
  return Number(value ?? 0);
}

function formatAmount(value: unknown) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(amountValue(value) / 100);
}

export function ReconciliationPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [batches, setBatches] = React.useState<ReconciliationBatchRow[]>([]);
  const [differences, setDifferences] = React.useState<ReconciliationDifferenceRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(differences);
  // 第十八批 Y 段第 181 条 181-R1（ENUM:191「待识别、待处理、待复核、已关闭」「各阶段由财务岗全程负责」）：待识别的行填差异原因「识别」，
  // 待处理的行填处理结论、补凭证、可指定最终审批人「提交复核」，复核在主管复核页审批。
  const canResolve = hasPermissions(effectivePermissions, RESOLVE_PERM);
  const [causeNote, setCauseNote] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [finalApprover, setFinalApprover] = React.useState("");
  const [reviewAttachments, setReviewAttachments] = React.useState<{ id: string; fileName: string }[]>([]);
  const [handling, setHandling] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  // 180-R2（ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证」）：「登记差异」表单——来源固定人工录入，未上传原件不能提交。
  const canRegister = hasPermissions(effectivePermissions, REGISTER_PERM);
  const [registration, setRegistration] = React.useState(EMPTY_REGISTRATION);
  const [statements, setStatements] = React.useState<{ id: string; fileName: string }[]>([]);
  const [registering, setRegistering] = React.useState(false);
  const [registerError, setRegisterError] = React.useState("");
  // 181-R2：差异停留时限（按项目）。
  const canManagePolicy = hasPermissions(effectivePermissions, POLICY_PERM);
  const [limitForm, setLimitForm] = React.useState(EMPTY_LIMIT);
  const [limitCurrent, setLimitCurrent] = React.useState("");
  const [limitError, setLimitError] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 -> 该批次/差异完整字段（金额¥·渠道 TraceLink·状态中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  // 点 KPI 数字 → pushState 只改查询串（pathname 原样保留子路径前缀链路），进聚焦视图。
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
      const [batchRows, differenceRows] = await Promise.all([
        api.get<ReconciliationBatchRow[]>("/api/v1/finance/reconciliation/batches"),
        api.get<ReconciliationDifferenceRow[]>("/api/v1/finance/reconciliation/differences"),
      ]);
      setBatches(batchRows);
      setDifferences(differenceRows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  // 180-R2：财务差异正式关闭后为 closed，其余都算未处理。
  const unresolvedCount = differences.filter((item) => item.status !== "closed").length;
  const totalDifference = differences.reduce((sum, item) => sum + amountValue(item.amountCent), 0);
  const batchById = new Map(batches.map((batch) => [batch.id, batch]));
  const businessDateOf = (item: ReconciliationDifferenceRow) => cnDate(batchById.get(item.batchId ?? "")?.businessDate);

  // 首批接线（指标登记簿方案A）：「对账差异」笔数改走统一明细端点（与明细页同源，全历史计数）；
  // 端点失败（含无 analytics.dashboard.view 的财务账号）→ 回退本地列表计数并隐藏「完整明细」入口，体验不降级。
  const diffCountKpi = useMetricKpiValue(api, "finance.recon_diff_count", {});
  const diffCountDisplay = diffCountKpi.status === "ready" && diffCountKpi.value !== null ? diffCountKpi.value : differences.length;

  // 181-R1：识别（待识别 → 待处理），写差异原因。
  async function identify() {
    if (!selected) return;
    if (!causeNote.trim()) {
      setFormError(TEXT.causeRequired);
      return;
    }
    setHandling(true);
    setFormError("");
    setGlobalError(null);
    try {
      const saved = await api.post<ReconciliationDifferenceRow>(`/api/v1/finance/reconciliation/differences/${selected.id}/identify`, {
        causeNote: causeNote.trim(),
      });
      setActionResult({ action: "reconciliation_difference_identified", differenceId: selected.id, status: saved.status });
      setCauseNote("");
      await reload();
    } catch (submitError) {
      setFormError(submitError instanceof Error ? submitError.message : String(submitError));
    } finally {
      setHandling(false);
    }
  }

  // 181-R1：提交复核（待处理 → 待复核），复核通过才关闭；人工录入没有原件的后端拒收，在这里补上原件再提交。
  async function submitReview() {
    if (!selected) return;
    if (!reason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    setHandling(true);
    setFormError("");
    setGlobalError(null);
    try {
      const saved = await api.post<ReconciliationDifferenceRow>(`/api/v1/finance/reconciliation/differences/${selected.id}/close`, {
        closeReason: reason.trim(),
        evidenceAttachmentIds: reviewAttachments.map((item) => item.id),
        finalApproverId: finalApprover.trim() || undefined,
      });
      setActionResult({ action: "reconciliation_difference_review_submitted", differenceId: selected.id, status: saved.status, reviewId: saved.reviewId });
      setReason("");
      setFinalApprover("");
      setReviewAttachments([]);
      await reload();
    } catch (submitError) {
      setFormError(submitError instanceof Error ? submitError.message : String(submitError));
    } finally {
      setHandling(false);
    }
  }

  // 凭证走证据上传接口（同 FinanceRefundPanel）；登记差异的对账单原件与提交复核补的凭证同用，每传成一份即记一份。
  async function uploadEvidence(files: FileList, add: (item: { id: string; fileName: string }) => void) {
    for (const file of Array.from(files)) {
      const form = new FormData();
      form.append("file", file);
      form.append("business_type", "finance_reconciliation_difference");
      form.append("attachment_role", "reconciliation_statement");
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
      add({ id: saved.id, fileName: saved.fileName ?? file.name });
    }
  }

  // 登记差异：对账单原件上传后才能提交登记。
  async function uploadStatements(files: FileList | null) {
    if (!files?.length || registering) return;
    setRegistering(true);
    setRegisterError("");
    try {
      await uploadEvidence(files, (item) => setStatements((previous) => [...previous, item]));
    } catch (uploadError) {
      setRegisterError(uploadError instanceof Error ? uploadError.message : "对账单原件上传失败");
    } finally {
      setRegistering(false);
    }
  }

  async function uploadReviewAttachments(files: FileList | null) {
    if (!files?.length || handling) return;
    setHandling(true);
    setFormError("");
    try {
      await uploadEvidence(files, (item) => setReviewAttachments((previous) => [...previous, item]));
    } catch (uploadError) {
      setFormError(uploadError instanceof Error ? uploadError.message : "凭证上传失败");
    } finally {
      setHandling(false);
    }
  }

  async function submitRegistration() {
    const actualAmountCent = Math.round(Number(registration.actualYuan) * 100);
    if (!registration.batchId || !registration.businessRef.trim() || !registration.actualYuan.trim()
      || !Number.isFinite(actualAmountCent) || actualAmountCent < 0 || statements.length === 0) {
      setRegisterError(TEXT.registerRequired);
      return;
    }
    setRegistering(true);
    setRegisterError("");
    setGlobalError(null);
    try {
      const saved = await api.post<ReconciliationDifferenceRow>("/api/v1/finance/reconciliation/differences", {
        batchId: registration.batchId,
        differenceType: registration.differenceType,
        businessRef: registration.businessRef.trim(),
        source: "manual",
        actualAmountCent,
        evidenceAttachmentIds: statements.map((item) => item.id),
      });
      setActionResult({ action: "reconciliation_difference_registered", differenceId: saved.id, status: saved.status, evidenceAttachmentIds: saved.evidenceAttachmentIds });
      setRegistration(EMPTY_REGISTRATION);
      setStatements([]);
      await reload();
    } catch (submitError) {
      setRegisterError(submitError instanceof Error ? submitError.message : String(submitError));
    } finally {
      setRegistering(false);
    }
  }

  // 181-R2：填项目编号查两阶段停留时限；按阶段填时限与上下限保存（三值同填，后端校验 1 ≤ 下限 ≤ 时限 ≤ 上限）。
  async function queryLimits() {
    const projectId = limitForm.projectId.trim();
    if (!projectId) {
      setLimitError(TEXT.limitProjectRequired);
      return;
    }
    setLimitError("");
    try {
      setLimitCurrent(limitText(await api.get<StageLimit[]>(`/api/v1/finance/reconciliation/stage-limits?project_id=${encodeURIComponent(projectId)}`)));
    } catch (queryError) {
      setLimitError(queryError instanceof Error ? queryError.message : String(queryError));
    }
  }

  async function saveLimit() {
    const projectId = limitForm.projectId.trim();
    if (!projectId) {
      setLimitError(TEXT.limitProjectRequired);
      return;
    }
    const num = (value: string) => (value.trim() === "" ? null : Number(value));
    setLimitError("");
    try {
      const saved = await api.put<StageLimit>(`/api/v1/finance/reconciliation/stage-limits/${encodeURIComponent(projectId)}`, {
        stage: limitForm.stage, workingDays: num(limitForm.workingDays), minWorkingDays: num(limitForm.minWorkingDays), maxWorkingDays: num(limitForm.maxWorkingDays),
      });
      setActionResult({ action: "reconciliation_stage_limit_saved", projectId: saved.projectId, workingDays: saved.workingDays });
      await queryLimits();
    } catch (saveError) {
      setLimitError(saveError instanceof Error ? saveError.message : String(saveError));
    }
  }

  const limitInput = (key: "workingDays" | "minWorkingDays" | "maxWorkingDays", label: string, testId: string) => (
    <label>{label}<input data-testid={testId} inputMode="numeric" value={limitForm[key]} onChange={(event) => setLimitForm({ ...limitForm, [key]: event.target.value })} /></label>
  );

  function openBatchDetail(batch: ReconciliationBatchRow) {
    setDetail({
      title: `${TEXT.batches} · ${batch.batchNo ?? batch.id}`,
      fields: [
        { label: TEXT.batchNo, value: batch.batchNo ?? batch.id },
        { label: TEXT.businessDate, value: cnDate(batch.businessDate) },
        { label: TEXT.scope, value: scopeText(batch) },
      ],
    });
  }

  function openDifferenceDetail(item: ReconciliationDifferenceRow) {
    setDetail({
      title: `${TEXT.differences} · ${item.id}`,
      fields: [
        { label: TEXT.differenceNo, value: item.id },
        { label: TEXT.businessDate, value: businessDateOf(item) },
        { label: TEXT.differenceType, value: labelOf(DIFFERENCE_TYPES, item.differenceType) },
        { label: TEXT.businessRef, value: item.businessRef ?? "-" },
        { label: TEXT.expected, value: formatAmount(item.expectedAmountCent) },
        { label: TEXT.actual, value: formatAmount(item.actualAmountCent) },
        { label: TEXT.diff, value: formatAmount(item.amountCent) },
        { label: TEXT.source, value: labelOf(SOURCES, item.source) },
        { label: TEXT.status, value: formatBusinessStatus(item.status) },
        { label: TEXT.responsible, value: item.responsibleUserId ?? "-" },
        { label: TEXT.stageEnteredAt, value: stageTime(item.stageEnteredAt) },
        { label: TEXT.cause, value: item.identifiedCause ?? "-" },
      ],
    });
  }

  const pagedBatches = usePagedRows(batches, 10);
  const pagedDifferences = usePagedRows(differences, 10);

  const batchesSection = (
    <>
      <h3>{TEXT.batches}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="reconciliation-batches-table">
          <thead><tr><th>{TEXT.batchNo}</th><th>{TEXT.businessDate}</th><th>{TEXT.scope}</th><th>详情</th></tr></thead>
          <tbody>{pagedBatches.pageRows.map((batch) => <tr key={batch.id} className="admin-row-clickable" tabIndex={0} data-testid={`reconciliation-batch-row-${batch.id}`} onClick={() => openBatchDetail(batch)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBatchDetail(batch); } }}><td>{batch.batchNo ?? batch.id}</td><td>{cnDate(batch.businessDate)}</td><td>{scopeText(batch)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看对账批次详情" onClick={(event) => { event.stopPropagation(); openBatchDetail(batch); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="reconciliation-batches-pager" page={pagedBatches.page} pageCount={pagedBatches.pageCount} total={pagedBatches.total} onPrev={pagedBatches.prev} onNext={pagedBatches.next} />
      </div>
    </>
  );

  // 「登记差异」表单：差异区照常引用；范围内还没有任何批次和差异时 AdminDataState 判空，空态只挡列表，表单在空态下照常可见（见下方 return）。
  const registerForm = canRegister ? (
    <div className="admin-form-grid" data-testid="reconciliation-register-form">
      <label>{TEXT.batches}<select data-testid="reconciliation-register-batch" value={registration.batchId} onChange={(event) => setRegistration({ ...registration, batchId: event.target.value })}>
        <option value="">请选择</option>
        {batches.map((batch) => <option key={batch.id} value={batch.id}>{`${cnDate(batch.businessDate)} · ${scopeText(batch)}`}</option>)}
      </select></label>
      <label>{TEXT.differenceType}<select data-testid="reconciliation-register-type" value={registration.differenceType} onChange={(event) => setRegistration({ ...registration, differenceType: event.target.value })}>
        {Object.entries(DIFFERENCE_TYPES).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
      </select></label>
      <label>{TEXT.businessRefInput}<input data-testid="reconciliation-register-ref" value={registration.businessRef} onChange={(event) => setRegistration({ ...registration, businessRef: event.target.value })} /></label>
      <label>{TEXT.actual}（元）<input data-testid="reconciliation-register-actual" inputMode="decimal" value={registration.actualYuan} onChange={(event) => setRegistration({ ...registration, actualYuan: event.target.value })} /></label>
      <label>{TEXT.statement}<input type="file" multiple accept={STATEMENT_ACCEPT} data-testid="reconciliation-register-statement" disabled={registering} onChange={(event) => { void uploadStatements(event.target.files); event.target.value = ""; }} /></label>
      <ul>{statements.map((item) => <li key={item.id}>{item.fileName}</li>)}</ul>
      <button type="button" data-testid="reconciliation-register-submit" disabled={registering || statements.length === 0} onClick={() => void submitRegistration()}>{TEXT.register}</button>
      {registerError ? <div data-testid="reconciliation-register-error" className="admin-inline-error">{registerError}</div> : null}
    </div>
  ) : null;

  // 差异区：复核表单（对选中差异操作）+ 差异承载表。聚焦「对账差异」时连同表单一起呈现，操作不断。
  const differencesSection = (
    <>
      {registerForm}
      {/* 181-R1：按选中差异所处阶段出操作——待识别出「识别」，待处理出「提交复核」，待复核只提示到主管复核页审批，已关闭无操作。 */}
      {canResolve && selected?.status === "pending_identification" ? (
        <div className="admin-form-grid" data-testid="reconciliation-identify-form">
          <label>{TEXT.cause}<input data-testid="reconciliation-identify-cause" value={causeNote} onChange={(event) => setCauseNote(event.target.value)} /></label>
          <button type="button" data-testid="reconciliation-identify-submit" disabled={handling} onClick={() => void identify()}>{TEXT.identify}</button>
        </div>
      ) : null}
      {canResolve && selected?.status === "pending_handling" ? (
        <div className="admin-form-grid" data-testid="reconciliation-review-form">
          <label>{TEXT.reason}<input data-testid="reconciliation-review-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          <label>{TEXT.attachment}<input type="file" multiple accept={STATEMENT_ACCEPT} data-testid="reconciliation-review-attachment" disabled={handling} onChange={(event) => { void uploadReviewAttachments(event.target.files); event.target.value = ""; }} /></label>
          <ul data-testid="reconciliation-review-attachments">{reviewAttachments.map((item) => <li key={item.id}>{item.fileName}</li>)}</ul>
          <label>{TEXT.finalApprover}<input data-testid="reconciliation-review-final-approver" value={finalApprover} onChange={(event) => setFinalApprover(event.target.value)} /></label>
          <button type="button" data-testid="reconciliation-review-submit" disabled={handling} onClick={() => void submitReview()}>{TEXT.submit}</button>
        </div>
      ) : null}
      {selected?.status === "pending_review" ? (
        <div className="admin-state" data-testid="reconciliation-reviewing">
          {TEXT.reviewing}
          {navigateTo ? <button type="button" data-testid="reconciliation-goto-review" onClick={() => navigateTo("ADM-GOV-006")}>{TEXT.gotoReview}</button> : null}
        </div>
      ) : null}
      <div className="admin-form-row">
        <span className="admin-muted-action" data-testid="reconciliation-boundary">{TEXT.boundary}</span>
      </div>
      {formError ? <div data-testid="reconciliation-error" className="admin-inline-error">{formError}</div> : null}
      <h3>{TEXT.differences}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="reconciliation-differences-table">
          <thead><tr><th>{TEXT.selected}</th><th>{TEXT.differenceNo}</th><th>{TEXT.businessDate}</th><th>{TEXT.differenceType}</th><th>{TEXT.businessRef}</th><th>{TEXT.expected}</th><th>{TEXT.actual}</th><th>{TEXT.diff}</th><th>{TEXT.source}</th><th>{TEXT.status}</th><th>{TEXT.responsible}</th><th>{TEXT.stageEnteredAt}</th><th>详情</th></tr></thead>
          <tbody>{pagedDifferences.pageRows.map((item) => <tr key={item.id} className="admin-row-clickable" tabIndex={0} data-testid={`reconciliation-difference-row-${item.id}`} onClick={() => openDifferenceDetail(item)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDifferenceDetail(item); } }}><td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此对账差异 ${item.id}`} data-testid={`reconciliation-select-${item.id}`} checked={(selected?.id ?? "") === item.id} onChange={() => setSelectedId(item.id)} /></td><td>{item.id}</td><td>{businessDateOf(item)}</td><td>{labelOf(DIFFERENCE_TYPES, item.differenceType)}</td><td>{item.businessRef ?? "-"}</td><td>{formatAmount(item.expectedAmountCent)}</td><td>{formatAmount(item.actualAmountCent)}</td><td>{formatAmount(item.amountCent)}</td><td>{labelOf(SOURCES, item.source)}</td><td>{formatBusinessStatus(item.status)}{item.stageEscalatedAt ? TEXT.escalated : ""}</td><td>{item.responsibleUserId ?? "-"}</td><td>{stageTime(item.stageEnteredAt)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看对账差异详情" onClick={(event) => { event.stopPropagation(); openDifferenceDetail(item); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="reconciliation-differences-pager" page={pagedDifferences.page} pageCount={pagedDifferences.pageCount} total={pagedDifferences.total} onPrev={pagedDifferences.prev} onNext={pagedDifferences.next} />
      </div>
    </>
  );

  return (
    <section data-testid="reconciliation-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      {/* P0-E 对账旁路手填封堵：把「金额只能系统取数」写在页面上，避免有人再去找手填入口。 */}
      <div className="admin-state" data-testid="reconciliation-no-manual-notice">
        对账金额口径：平台侧金额<strong>由系统取数</strong>；第三方侧接入前可人工录入，须同时上传渠道对账单原件。
      </div>
      <TechDetails><div className="admin-generic-evidence" data-testid="reconciliation-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="finance.reconciliation_batch.viewed / finance_reconciliation.difference_found / finance.reconciliation.difference_identified / finance.reconciliation.difference_review_submitted" />
      <AdminDataState loading={loading} error={error} empty={batches.length === 0 && differences.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            {/* 聚焦视图：只渲染被点 KPI 对应那张承载表，其余表/KPI 卡全隐藏，感知上=新页面。 */}
            <div className="admin-state" data-testid="reconciliation-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{kpiFocus.table === "batches" ? batches.length : diffCountDisplay}）
              <button type="button" data-testid="reconciliation-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
              {/* 聚焦视图出口（设计稿二拍板）：本页聚焦交互不动，加「查看完整明细」进统一明细页（可核可分享）。 */}
              {kpiFocus.table === "differences" && diffCountKpi.status === "ready"
                ? <MetricDetailLink navigateTo={navigateTo} metric="finance.recon_diff_count" testid="reconciliation-metric-detail-differences" />
                : null}
            </div>
            {kpiFocus.table === "batches" ? batchesSection : differencesSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="reconciliation-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.batches}</span>
                <button type="button" className="admin-kpi-value" data-testid="reconciliation-kpi-batches" onClick={() => enterKpiFocus("batches")}>{batches.length}</button>
                <small>{TEXT.batchNo}</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.differences}</span>
                <button type="button" className="admin-kpi-value" data-testid="reconciliation-kpi-differences" onClick={() => enterKpiFocus("differences")}>{diffCountDisplay}</button>
                <small>{TEXT.unresolved}: {unresolvedCount}</small>
              </div>
              {/* 差异金额=聚合金额、选中差异=选择态，均无独立承载表→不可点。 */}
              <div className="admin-kpi-card"><span>{TEXT.amount}</span><strong>{formatAmount(totalDifference)}</strong><small>{TEXT.status}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong className="admin-kpi-value--text" title={selected?.id ?? "-"}>{selected?.id ?? "-"}</strong><small>{formatBusinessStatus(selected?.status)}</small></div>
            </div>

            {differencesSection}
            {batchesSection}
          </>
        )}
      </AdminDataState>
      {/* 第十八批 Y 段复核修复（BSM:1682「按营业日生成日结批次。」）：范围内还没有任何批次和差异时空态只挡列表，「登记差异」表单照常可见，
          同页日结区块生成日结后随即重读出新批次；KPI 聚焦视图不放（不含日结区块）。 */}
      {kpiFocus || loading || error || batches.length > 0 || differences.length > 0 ? null : registerForm}
      {/* 第十八批 Y 段第 181 条 181-R2：差异停留时限（按项目），持 finance.reconciliation.policy.manage 才显示；放在 AdminDataState 之外，没有差异时也能配，KPI 聚焦视图不放。 */}
      {kpiFocus || !canManagePolicy ? null : (
        <>
          <h3>{TEXT.limitTitle}</h3>
          <div className="admin-form-grid" data-testid="reconciliation-stage-limit-form">
            <label>{TEXT.limitProject}<input data-testid="reconciliation-stage-limit-project" value={limitForm.projectId} onChange={(event) => setLimitForm({ ...limitForm, projectId: event.target.value })} /></label>
            <button type="button" data-testid="reconciliation-stage-limit-query" onClick={() => void queryLimits()}>{TEXT.limitQuery}</button>
            <label>{TEXT.limitStage}<select data-testid="reconciliation-stage-limit-stage" value={limitForm.stage} onChange={(event) => setLimitForm({ ...limitForm, stage: event.target.value })}>
              {Object.entries(LIMIT_STAGES).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
            </select></label>
            {limitInput("workingDays", TEXT.limitDays, "reconciliation-stage-limit-days")}
            {limitInput("minWorkingDays", TEXT.limitMin, "reconciliation-stage-limit-min")}
            {limitInput("maxWorkingDays", TEXT.limitMax, "reconciliation-stage-limit-max")}
            <button type="button" data-testid="reconciliation-stage-limit-save" onClick={() => void saveLimit()}>{TEXT.limitSave}</button>
            <span className="admin-muted-action" data-testid="reconciliation-stage-limit-current">{limitCurrent || TEXT.limitHint}</span>
            {limitError ? <div data-testid="reconciliation-stage-limit-error" className="admin-inline-error">{limitError}</div> : null}
          </div>
        </>
      )}
      {/* 第十八批 P 段第 133 条 133-R3（ENUM:188 对账口径含「按日结」）：日结区块放在 AdminDataState 之外——没有第三方对账批次和差异时（只收现金、POS 的酒吧正常就是这样）仍要能查与锁日结；KPI 聚焦视图只放被点那张承载表。 */}
      {kpiFocus ? null : <FinanceDailyClosePanel api={api} user={user} effectivePermissions={effectivePermissions} setGlobalError={setGlobalError} onGenerated={reload} />}
      {/* 第十八批 Z 段第 188 条 188-R2（BSM:1803；第十八批裁决 41「财务中心按综合体＋月份在平台办理」）：封账区块与日结区块并列，持 finance.period_close.execute 才显示，KPI 聚焦视图不放。 */}
      {kpiFocus ? null : <FinancePeriodClosePanel api={api} effectivePermissions={effectivePermissions} />}
      {/* 第十八批 R 段第 142 条 142-R3（BSM:1848「财务看退款、差异、日结和结算依据。」；PGM:664「差异认定……由该酒吧对应的财务 BP 负责」）：收银交班复核平账与见证归档区块，同在 AdminDataState 之外、KPI 聚焦视图不放。 */}
      {kpiFocus ? null : <BarShiftPanel reviewOnly api={api} user={user} effectivePermissions={effectivePermissions} setActionResult={setActionResult} setGlobalError={setGlobalError} />}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
