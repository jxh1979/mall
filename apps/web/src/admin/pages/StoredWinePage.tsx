// 文件版本：V0.06
import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import type { AdminPageProps, StoredWineRemedyRow, StoredWineRow } from "../types";

// 后端 /saved-bottles 还返回到期/更新/剩余数量等（StoredWineRow 未全量声明），
// 行明细抽屉就近读取这些字段，不改共享 types.ts。
type StoredWineDetailRow = StoredWineRow & {
  expireAt?: string;
  updatedAt?: string;
  remainingQuantity?: number;
  initialQuantity?: number;
  unit?: string;
};

// FS-025 转存流水（GET /saved-bottles/{id}/renewals；adminClient 驼峰化）
type StoredWineRenewalRow = {
  id: string;
  status?: string;
  extensionDays?: number;
  previousExpireAt?: string;
  newExpireAt?: string;
  renewalCount?: number;
  maxRenewals?: number;
  operatorId?: string;
  reviewedBy?: string;
  createdAt?: string;
};

// 后端写权限码（InventoryController：/saved-bottles/{id}/claim、/transfer、/verification-code）
const STORED_WINE_CLAIM_PERMISSION = "bar.saved_bottle.manage";
// FS-025 员工代办转存的营销主管复核权限码（InventoryService.RENEWAL_REVIEW）
const STORED_WINE_RENEWAL_REVIEW_PERMISSION = "bar.saved_bottle.renewal.review";
// FS-017 赔付方案审批：门店主管用高价值复核码、财务用既有应收应付管理码（InventoryService.REMEDY_*_APPROVE）。
const STORED_WINE_REMEDY_SUPERVISOR_PERMISSION = "inventory.high_value.review";
const STORED_WINE_REMEDY_FINANCE_PERMISSION = "finance.ar_ap.manage";

// FS-017 状态中文（对客交付 / 内部追收 / 承担方 / 接收状态）。
const REMEDY_DELIVERY_LABELS: Record<string, string> = { pending_approval: "待审批", approved: "已审批待交付", closed: "已结案" };
const REMEDY_RECOVERY_LABELS: Record<string, string> = { none: "无追收", pending: "待收回", recovered: "已收回" };
const REMEDY_PARTY_LABELS: Record<string, string> = { none: "—", original_merchant: "原门店", staff: "责任员工" };
const REMEDY_BEARER_LABELS: Record<string, string> = { original_merchant: "原门店承担", platform_company: "娱乐平台公司承担" };
const REMEDY_TYPE_LABELS: Record<string, string> = { replacement: "同品同量补存", compensation: "赔付" };
const HANDOVER_CONDITIONS: Array<{ value: string; label: string }> = [
  { value: "intact", label: "完好" },
  { value: "damaged", label: "毁损" },
  { value: "short", label: "短少" },
];

function splitIds(value: string) {
  return value.split(/[\s,，;；]+/).map((item) => item.trim()).filter(Boolean);
}

const TEXT = {
  title: "存酒",
  description: "展示客人存酒资产、在线领取核销、证据附件和幂等留痕；高价值酒水保持在线校验，不做离线核销。",
  evidence: "接口证据：读取 /api/v1/inventory/saved-bottles；按手机号检索 POST /api/v1/inventory/saved-bottles/lookup；发码 POST /api/v1/inventory/saved-bottles/{id}/verification-code（scene=claim|transfer）；领取 POST /api/v1/inventory/saved-bottles/{id}/claim；转让 POST /api/v1/inventory/saved-bottles/{id}/transfer（to_name_or_nickname、to_phone、verification_code）；转存 POST /api/v1/inventory/saved-bottles/{id}/renewals（verification_code）、复核 POST /api/v1/inventory/saved-bottles/renewals/{id}/review（decision=approve|reject，驳回带 reason）、记录 GET /api/v1/inventory/saved-bottles/{id}/renewals；作废 POST /api/v1/inventory/saved-bottles/{id}/cancel（photo/video_attachment_ids）；补存/赔付 POST /api/v1/inventory/saved-bottles/{id}/remedy、记录 GET …/{id}/remedies、审批/结案/垫付/员工责任/追收收回 POST /api/v1/inventory/saved-bottles/remedies/{id}/approve|settle|advance|staff-liability|recovery/settle；门店退出 POST /api/v1/inventory/saved-bottles/exit-notice、交接 POST /api/v1/inventory/saved-bottles/handover。",
  loadFailed: "存酒数据读取失败",
  claimFailed: "存酒领取核销失败",
  transferFailed: "存酒转让失败",
  sendCodeFailed: "验证码发送失败",
  noBottle: "当前没有可领取的存酒记录。",
  codeRequired: "领取前必须录入当前持有人的短信验证码或动态取酒码。",
  sendCode: "向当前持有人发送验证码",
  codeSent: "验证码已发送至当前持有人手机",
  transfer: "转让给他人",
  transferToName: "受让人姓名/昵称",
  transferToPhone: "受让人手机号",
  transferCode: "出让人短信验证码",
  transferRequired: "转让须填写受让人姓名/昵称、受让人手机号与出让人短信验证码。",
  transferBoundary: "转让须经当前持有人短信验证码确认，验证成功即换持有人并整瓶转让，原持有人的动态取酒码立即作废。",
  renewal: "转存（延期）",
  renewalCode: "客人本人短信验证码",
  renewalRequired: "转存须填写客人本人短信验证码。",
  renewalBoundary: "转存须客人本人短信验证码确认（与转让同款）；员工代办转存另须当班营销主管复核后才顺延到期日；转存次数不得超过配置上限，只能在当前到期日前办理。",
  renewalFailed: "存酒转存失败",
  renewalReview: "营销主管复核",
  renewalReviewFailed: "转存复核失败",
  renewalList: "转存记录",
  renewalEmpty: "该存酒暂无转存记录。",
  renewalPending: "待营销主管复核",
  renewalEffective: "已生效",
  renewalRejected: "已驳回",
  renewalReject: "驳回代办转存",
  renewalRejectReason: "驳回原因",
  renewalRejectRequired: "驳回须填写原因。",
  search: "检索",
  searchPlaceholder: "输入手机号检索存酒（姓名、昵称不得用于检索）",
  quantityInvalid: "领取数量必须大于 0。",
  totalBottles: "存酒记录",
  storedBottles: "可领取",
  totalQuantity: "剩余数量",
  selectedBottle: "选中存酒",
  onlineCheck: "在线校验",
  onlineCheckHelp: "高价值酒水不支持离线领取",
  claim: "在线领取核销",
  code: "短信验证码 / 动态取酒码",
  quantity: "领取数量",
  note: "备注",
  boundary: "领取会写入幂等键、核验码、附件证据和审计事件；重复提交必须返回同一处理结果，避免重复扣减客户资产。",
  select: "选择",
  savedNo: "存酒号",
  customer: "客户",
  item: "酒水",
  status: "状态",
  createdAt: "时间",
  auditNote: "完整后台验收领取",
  totalBottlesHelp: "存酒记录总数",
  storedHelp: "在线可领取",
  totalQuantityHelp: "剩余可领数量",
  focusClear: "清除筛选，返回全部",
  // FS-017（BSM L1072）
  cancel: "作废",
  cancelReason: "作废原因",
  cancelPhotos: "照片证据 id（逗号分隔）",
  cancelVideos: "视频证据 id（逗号分隔）",
  cancelRequired: "作废须填写照片与视频证据 id（照片+视频双证据+主管复核）。",
  cancelNotCancellable: "仅存酒中或待核身复核的存酒可作废。",
  cancelBoundary: "存酒作废需照片+视频双证据+主管复核；高价值存酒先冻结待主管复核通过后才作废，作废后在下方登记补存或赔付。",
  cancelFailed: "存酒作废失败",
  remedy: "补存/赔付",
  remedyType: "方式",
  remedyAmount: "赔付金额（元）",
  remedyPhotos: "补存瓶登记照片 id（逗号分隔）",
  remedyLabel: "补存瓶底标签值",
  remedyLocation: "出库库位",
  remedySubmit: "登记补存/赔付",
  remedyBoundary: "作废后由该酒吧为客人以同品、同等剩余量补存，到期日不变，补酒成本计入经营损失；无法补存的登记赔付方案，经门店主管和财务审批后支付，赔款实际交付后才结案。对客交付与内部追收分别记录，员工未赔付不阻止对客交付。",
  remedyNotCancelled: "仅已作废的存酒可登记补存/赔付。",
  remedyAmountInvalid: "赔付金额必须大于 0。",
  remedyReplacementRequired: "补存须填写登记照片 id、补存瓶底标签值与出库库位。",
  remedyFailed: "补存/赔付登记失败",
  remedyList: "补存/赔付记录",
  remedyEmpty: "该存酒暂无补存/赔付记录。",
  remedyDelivery: "对客",
  remedyRecovery: "追收",
  remedyBearer: "承担方",
  remedyApproveSupervisor: "主管审批",
  remedyApproveFinance: "财务审批",
  remedySettle: "登记交付/支付凭证并结案",
  remedyAdvance: "垫付并追收",
  remedyStaff: "认定员工责任",
  remedyStaffUser: "责任员工 user_id",
  remedyRecoverySettle: "追收已收回",
  remedyEvidence: "交付/收回凭证 id（逗号分隔）",
  remedyActionFailed: "补存/赔付处理失败",
  exit: "门店退出",
  exitMerchant: "退出门店（商户）id",
  exitNotice: "清点并通知客人原店取回",
  exitNoticeBoundary: "退出前清点有效期内未结存酒并逐瓶短信通知客人在原店取回。",
  exitRequired: "须填写退出门店 id。",
  exitFailed: "退出清点通知失败",
  handover: "交接到平台",
  handoverLocation: "指定领取地点",
  handoverPhotos: "交接现场照片 id（逗号分隔）",
  handoverVideos: "交接现场视频 id（逗号分隔）",
  handoverItems: "逐瓶清点（实收数量 / 接收状态）",
  handoverRequired: "交接须填写退出门店 id 与指定领取地点。",
  handoverBoundary: "门店退出后由所属娱乐平台公司接收有效期内未结存酒实物和记录，指定领取地点，继续按原有效期与原取酒规则处理；接收时已存在的毁损、短少仍由原门店承担，可经审批先垫付并向原门店追收。",
  handoverFailed: "存酒交接失败",
  custody: "保管方/领取地点",
  custodyOriginal: "原店",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带 ?kpi= 参数重进本页，只渲染存酒表并按子集过滤。
type KpiTarget = "bottles" | "stored";
function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target === "bottles" || target === "stored" ? target : null;
}

function numberValue(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function StoredWinePage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, reportViewedObjects }: AdminPageProps) {
  const [rows, setRows] = React.useState<StoredWineRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("saved_bottle", rows), [rows, reportViewedObjects]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [verificationCode, setVerificationCode] = React.useState("");
  // FS-016 转让分组：受让人三件套 + 出让人（当前持有人）短信验证码。
  const [transferToName, setTransferToName] = React.useState("");
  const [transferToPhone, setTransferToPhone] = React.useState("");
  const [transferCode, setTransferCode] = React.useState("");
  const [transferError, setTransferError] = React.useState("");
  // FS-025 转存分组：客人本人短信验证码；选中存酒后加载其转存记录（待复核项可由营销主管复核）。
  const [renewalCode, setRenewalCode] = React.useState("");
  const [renewalError, setRenewalError] = React.useState("");
  const [renewals, setRenewals] = React.useState<StoredWineRenewalRow[]>([]);
  const [renewalRejectReason, setRenewalRejectReason] = React.useState<Record<string, string>>({});
  const [codeNotice, setCodeNotice] = React.useState("");
  // FS-017 作废（照片+视频双证据）/ 补存赔付 / 门店退出清点与交接。
  const [cancelReason, setCancelReason] = React.useState("");
  const [cancelPhotos, setCancelPhotos] = React.useState("");
  const [cancelVideos, setCancelVideos] = React.useState("");
  const [cancelError, setCancelError] = React.useState("");
  const [remedyType, setRemedyType] = React.useState("replacement");
  const [remedyAmount, setRemedyAmount] = React.useState("");
  const [remedyPhotos, setRemedyPhotos] = React.useState("");
  const [remedyLabel, setRemedyLabel] = React.useState("");
  const [remedyLocation, setRemedyLocation] = React.useState("");
  const [remedyStaffUser, setRemedyStaffUser] = React.useState("");
  const [remedyEvidence, setRemedyEvidence] = React.useState("");
  const [remedyError, setRemedyError] = React.useState("");
  const [remedies, setRemedies] = React.useState<StoredWineRemedyRow[]>([]);
  const [exitMerchantId, setExitMerchantId] = React.useState("");
  const [exitResult, setExitResult] = React.useState("");
  const [exitError, setExitError] = React.useState("");
  const [handoverLocation, setHandoverLocation] = React.useState("");
  const [handoverPhotos, setHandoverPhotos] = React.useState("");
  const [handoverVideos, setHandoverVideos] = React.useState("");
  const [handoverItems, setHandoverItems] = React.useState<Record<string, { quantity: string; condition: string }>>({});
  const [claimQuantity, setClaimQuantity] = React.useState("1");
  const [operatorNote, setOperatorNote] = React.useState(TEXT.auditNote);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该存酒完整字段（状态中文·存/取/到期时间中文·数量）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openBottleDetail(row: StoredWineRow) {
    const full = row as StoredWineDetailRow;
    setDetail({
      title: `存酒明细 · ${row.savedNo ?? row.id}`,
      fields: [
        { label: TEXT.savedNo, value: row.savedNo ?? row.id },
        { label: TEXT.customer, value: row.customerName ?? row.customerId ?? "—" },
        { label: TEXT.item, value: row.itemName ?? row.itemId ?? "—" },
        { label: TEXT.quantity, value: full.remainingQuantity !== undefined ? `${full.remainingQuantity}${full.unit ? ` ${full.unit}` : ""}` : (row.quantity ?? "—") },
        { label: TEXT.status, value: formatBusinessStatus(row.status) },
        { label: "存酒时间", value: cnDate(row.createdAt) },
        { label: "取酒/更新时间", value: cnDate(full.updatedAt) },
        { label: "到期时间", value: cnDate(full.expireAt) },
        { label: TEXT.custody, value: row.pickupLocation ?? TEXT.custodyOriginal },
        { label: "补存来源（原作废存酒）", value: row.remedyOfSavedBottleId ?? "—" },
      ],
    });
  }

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(target);
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
      const data = await api.get<StoredWineRow[]>("/api/v1/inventory/saved-bottles");
      setRows(data);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const storedCount = rows.filter((row) => (row.status ?? "") === "stored").length;
  // 列表接口返回 remainingQuantity（旧 quantity 字段兼容保留）：剩余数量=全部登记行当前剩余数量直加。
  const totalQuantity = rows.reduce((sum, row) => sum + numberValue(row.remainingQuantity ?? row.quantity), 0);

  // 第二批接线（指标登记簿）：存酒总数/在存/剩余数量三个 KPI 数字改走统一明细端点（与明细页同源，
  // 全历史/台账当前值口径与本页全量列表一致）；失败（含无权限）回退本地计算并隐藏「完整明细」入口。
  const bottleCountKpi = useMetricKpiValue(api, "bar.saved_bottle_count", {});
  const storedKpi = useMetricKpiValue(api, "bar.saved_bottle_stored", {});
  const quantityKpi = useMetricKpiValue(api, "bar.saved_bottle_quantity", {});
  const bottleCountDisplay = bottleCountKpi.status === "ready" && bottleCountKpi.value !== null ? bottleCountKpi.value : rows.length;
  const storedDisplay = storedKpi.status === "ready" && storedKpi.value !== null ? storedKpi.value : storedCount;
  const quantityDisplay = quantityKpi.status === "ready" && quantityKpi.value !== null ? quantityKpi.value : totalQuantity;

  // 聚焦时先按子集过滤内存数组（stored=在线可领取）再排序；检索只按手机号走后端 lookup（PGM L878），不做姓名/昵称关键词过滤。
  const kpiRows = kpiFocus === "stored" ? rows.filter((row) => (row.status ?? "") === "stored") : rows;
  const filteredRows = kpiRows;
  const sort = useSortedRows(filteredRows, {
    savedNo: (row) => row.savedNo ?? row.id,
    customer: (row) => row.customerName ?? row.customerId,
    item: (row) => row.itemName ?? row.itemId,
    quantity: (row) => numberValue(row.remainingQuantity ?? row.quantity),
    status: (row) => row.status,
    createdAt: (row) => row.createdAt,
  });
  const paged = usePagedRows(sort.sorted, 10, `${query}:${sort.key}:${sort.direction}:${kpiFocus ?? ""}`);

  function validateClaim() {
    if (!selected) {
      setFormError(TEXT.noBottle);
      return false;
    }
    const quantity = Number(claimQuantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return false;
    }
    if (!verificationCode.trim()) {
      setFormError(TEXT.codeRequired);
      return false;
    }
    return true;
  }

  // FS-016：按手机号检索（手机号走 POST body，不进 URL）；清空即回到全量列表。
  async function lookupByPhone() {
    const phone = query.trim();
    if (!phone) {
      await reload();
      return;
    }
    setGlobalError(null);
    try {
      const data = await api.post<StoredWineRow[]>("/api/v1/inventory/saved-bottles/lookup", { phone });
      setRows(data);
    } catch (lookupError) {
      const message = lookupError instanceof Error ? lookupError.message : TEXT.loadFailed;
      setGlobalError(message);
    }
  }

  // FS-016/FS-025：向当前持有人手机发送短信验证码（scene=claim|transfer|renew）。
  async function sendVerificationCode(scene: "claim" | "transfer" | "renew") {
    if (!selected) {
      setFormError(TEXT.noBottle);
      return;
    }
    setCodeNotice("");
    setGlobalError(null);
    try {
      const body = { idempotency_key: `admin-saved-bottle-code-${selected.id}-${scene}-${Date.now()}`, scene };
      await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/verification-code`, body);
      setCodeNotice(TEXT.codeSent);
    } catch (sendError) {
      const message = sendError instanceof Error ? sendError.message : TEXT.sendCodeFailed;
      setGlobalError(message);
      setCodeNotice(message);
    }
  }

  function validateTransfer() {
    if (!selected) {
      setTransferError(TEXT.noBottle);
      return false;
    }
    if (!transferToName.trim() || !transferToPhone.trim() || !transferCode.trim()) {
      setTransferError(TEXT.transferRequired);
      return false;
    }
    return true;
  }

  async function transferStoredWine() {
    if (!selected || !validateTransfer()) {
      return;
    }
    setSubmitting(true);
    setTransferError("");
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-transfer-${selected.id}-${transferToPhone.trim()}`,
        operator_note: operatorNote,
        evidence_attachment_ids: [],
        to_name_or_nickname: transferToName.trim(),
        to_phone: transferToPhone.trim(),
        verification_code: transferCode.trim(),
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/transfer`, body);
      setActionResult({ action: "stored_wine_transferred", savedBottleId: selected.id, requestBody: body, result });
      setTransferCode("");
      await reload();
    } catch (transferErr) {
      const message = transferErr instanceof Error ? transferErr.message : TEXT.transferFailed;
      setTransferError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_transferred", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  const loadRenewals = React.useCallback(async (savedBottleId: string) => {
    try {
      const data = await api.get<StoredWineRenewalRow[]>(`/api/v1/inventory/saved-bottles/${savedBottleId}/renewals`);
      setRenewals(Array.isArray(data) ? data : []);
    } catch {
      setRenewals([]);
    }
  }, [api]);

  React.useEffect(() => {
    if (!selectedId) {
      setRenewals([]);
      return;
    }
    void loadRenewals(selectedId);
  }, [selectedId, loadRenewals]);

  function validateRenewal() {
    if (!selected) {
      setRenewalError(TEXT.noBottle);
      return false;
    }
    if (!renewalCode.trim()) {
      setRenewalError(TEXT.renewalRequired);
      return false;
    }
    return true;
  }

  async function renewStoredWine() {
    if (!selected || !validateRenewal()) {
      return;
    }
    setSubmitting(true);
    setRenewalError("");
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-renew-${selected.id}-${Date.now()}`,
        operator_note: operatorNote,
        verification_code: renewalCode.trim(),
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/renewals`, body);
      setActionResult({ action: "stored_wine_renewed", savedBottleId: selected.id, requestBody: body, result });
      setRenewalCode("");
      await loadRenewals(selected.id);
      await reload();
    } catch (renewErr) {
      const message = renewErr instanceof Error ? renewErr.message : TEXT.renewalFailed;
      setRenewalError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_renewed", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function validateRenewalReject(renewalId: string) {
    if (!(renewalRejectReason[renewalId] ?? "").trim()) {
      setRenewalError(TEXT.renewalRejectRequired);
      return false;
    }
    setRenewalError("");
    return true;
  }

  // 复核含否决：approve 顺延到期日；reject 须带原因，到期日不动、不计转存次数。
  async function reviewRenewal(renewalId: string, decision: "approve" | "reject") {
    if (!selected) {
      return;
    }
    setSubmitting(true);
    setRenewalError("");
    setGlobalError(null);
    try {
      const reason = decision === "reject" ? (renewalRejectReason[renewalId] ?? "").trim() : "";
      const body = {
        idempotency_key: `admin-saved-bottle-renewal-review-${renewalId}-${decision}`,
        operator_note: operatorNote,
        decision,
        ...(reason ? { reason } : {}),
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/renewals/${renewalId}/review`, body);
      setActionResult({ action: "stored_wine_renewal_reviewed", renewalId, requestBody: body, result });
      await loadRenewals(selected.id);
      await reload();
    } catch (reviewErr) {
      const message = reviewErr instanceof Error ? reviewErr.message : TEXT.renewalReviewFailed;
      setRenewalError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_renewal_reviewed", renewalId, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // ===== FS-017 作废 / 补存赔付 / 门店退出 =====
  function validateCancel() {
    if (!selected) {
      setCancelError(TEXT.noBottle);
      return false;
    }
    if (!["stored", "pending_identity_review"].includes(selected.status ?? "")) {
      setCancelError(TEXT.cancelNotCancellable);
      return false;
    }
    if (splitIds(cancelPhotos).length === 0 || splitIds(cancelVideos).length === 0) {
      setCancelError(TEXT.cancelRequired);
      return false;
    }
    setCancelError("");
    return true;
  }

  async function cancelStoredWine() {
    if (!selected || !validateCancel()) {
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-cancel-${selected.id}`,
        operator_note: operatorNote,
        reason: cancelReason.trim(),
        photo_attachment_ids: splitIds(cancelPhotos),
        video_attachment_ids: splitIds(cancelVideos),
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/cancel`, body);
      setActionResult({ action: "stored_wine_cancelled", savedBottleId: selected.id, requestBody: body, result });
      await reload();
    } catch (cancelErr) {
      const message = cancelErr instanceof Error ? cancelErr.message : TEXT.cancelFailed;
      setCancelError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_cancelled", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  const loadRemedies = React.useCallback(async (savedBottleId: string) => {
    try {
      const data = await api.get<StoredWineRemedyRow[]>(`/api/v1/inventory/saved-bottles/${savedBottleId}/remedies`);
      setRemedies(Array.isArray(data) ? data : []);
    } catch {
      setRemedies([]);
    }
  }, [api]);

  React.useEffect(() => {
    if (!selectedId) {
      setRemedies([]);
      return;
    }
    void loadRemedies(selectedId);
  }, [selectedId, loadRemedies]);

  function validateRemedy() {
    if (!selected) {
      setRemedyError(TEXT.noBottle);
      return false;
    }
    if (selected.status !== "cancelled") {
      setRemedyError(TEXT.remedyNotCancelled);
      return false;
    }
    if (remedyType === "compensation") {
      const yuan = Number(remedyAmount);
      if (!Number.isFinite(yuan) || yuan <= 0) {
        setRemedyError(TEXT.remedyAmountInvalid);
        return false;
      }
    } else if (splitIds(remedyPhotos).length === 0 || !remedyLabel.trim() || !remedyLocation.trim()) {
      setRemedyError(TEXT.remedyReplacementRequired);
      return false;
    }
    setRemedyError("");
    return true;
  }

  async function registerRemedy() {
    if (!selected || !validateRemedy()) {
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-remedy-${selected.id}-${Date.now()}`,
        operator_note: operatorNote,
        remedy_type: remedyType,
        amount_cent: remedyType === "compensation" ? Math.round(Number(remedyAmount) * 100) : undefined,
        reason: cancelReason.trim() || undefined,
        registration_photo_refs: remedyType === "replacement" ? splitIds(remedyPhotos) : undefined,
        bottle_label_value: remedyType === "replacement" ? remedyLabel.trim() : undefined,
        location_id: remedyType === "replacement" ? remedyLocation.trim() : undefined,
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/remedy`, body);
      setActionResult({ action: "stored_wine_remedy_registered", savedBottleId: selected.id, requestBody: body, result });
      await loadRemedies(selected.id);
      await reload();
    } catch (remedyErr) {
      const message = remedyErr instanceof Error ? remedyErr.message : TEXT.remedyFailed;
      setRemedyError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_remedy_registered", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  async function remedyAction(remedyId: string, path: string, extra: Record<string, unknown>) {
    setSubmitting(true);
    setRemedyError("");
    setGlobalError(null);
    try {
      const body = { idempotency_key: `admin-saved-bottle-remedy-${path.replace("/", "-")}-${remedyId}`, operator_note: operatorNote, ...extra };
      const result = await api.post(`/api/v1/inventory/saved-bottles/remedies/${remedyId}/${path}`, body);
      setActionResult({ action: `stored_wine_remedy_${path.replace("/", "_")}`, remedyId, requestBody: body, result });
      if (selected) {
        await loadRemedies(selected.id);
      }
    } catch (actionErr) {
      const message = actionErr instanceof Error ? actionErr.message : TEXT.remedyActionFailed;
      setRemedyError(message);
      setGlobalError(message);
      setActionResult({ action: `stored_wine_remedy_${path.replace("/", "_")}`, remedyId, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function validateExit() {
    if (!exitMerchantId.trim()) {
      setExitError(TEXT.exitRequired);
      return false;
    }
    setExitError("");
    return true;
  }

  async function noticeExit() {
    if (!validateExit()) {
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = { idempotency_key: `admin-saved-bottle-exit-notice-${exitMerchantId.trim()}-${Date.now()}`, operator_note: operatorNote, merchant_id: exitMerchantId.trim() };
      const result = await api.post<{ bottleCount?: number; notifiedCount?: number }>("/api/v1/inventory/saved-bottles/exit-notice", body);
      setExitResult(`已清点 ${numberValue(result?.bottleCount)} 瓶，已通知 ${numberValue(result?.notifiedCount)} 位客人在原店取回`);
      setActionResult({ action: "stored_wine_exit_noticed", requestBody: body, result });
    } catch (exitErr) {
      const message = exitErr instanceof Error ? exitErr.message : TEXT.exitFailed;
      setExitError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_exit_noticed", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 交接清单只收退出门店（barProjectId＝所属酒吧商户 id）仍在保管有效期内、尚未交平台的在存存酒（BSM L1072 ④）；平台视图下别家酒吧不进 items。
  const exitMerchant = exitMerchantId.trim();
  const handoverRows = rows.filter((row) => (row.status ?? "") === "stored" && !row.pickupLocation
    && row.barProjectId === exitMerchant
    && !(row.expireAt && new Date(row.expireAt).getTime() < Date.now()));

  function validateHandover() {
    if (!exitMerchantId.trim() || !handoverLocation.trim()) {
      setExitError(TEXT.handoverRequired);
      return false;
    }
    setExitError("");
    return true;
  }

  async function handoverToPlatform() {
    if (!validateHandover()) {
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-handover-${exitMerchantId.trim()}-${Date.now()}`,
        operator_note: operatorNote,
        merchant_id: exitMerchantId.trim(),
        pickup_location: handoverLocation.trim(),
        photo_attachment_ids: splitIds(handoverPhotos),
        video_attachment_ids: splitIds(handoverVideos),
        items: handoverRows.map((row) => ({
          saved_bottle_id: row.id,
          received_quantity: numberValue(handoverItems[row.id]?.quantity ?? row.remainingQuantity ?? row.quantity),
          condition: handoverItems[row.id]?.condition ?? "intact",
        })),
      };
      const result = await api.post("/api/v1/inventory/saved-bottles/handover", body);
      setActionResult({ action: "stored_wine_handed_over", requestBody: body, result });
      await reload();
    } catch (handoverErr) {
      const message = handoverErr instanceof Error ? handoverErr.message : TEXT.handoverFailed;
      setExitError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_handed_over", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function setHandoverItem(id: string, patch: Partial<{ quantity: string; condition: string }>) {
    setHandoverItems((current) => ({ ...current, [id]: { quantity: current[id]?.quantity ?? "", condition: current[id]?.condition ?? "intact", ...patch } }));
  }

  async function claimStoredWine() {
    if (!selected) {
      setFormError(TEXT.noBottle);
      return;
    }
    const quantity = Number(claimQuantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return;
    }
    if (!verificationCode.trim()) {
      setFormError(TEXT.codeRequired);
      return;
    }
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-claim-${selected.id}-${quantity}`,
        operator_note: operatorNote,
        evidence_attachment_ids: ["att-saved-bottle-claim-1"],
        claim_items: [{ saved_bottle_id: selected.id, item_name: selected.itemName ?? selected.savedNo ?? selected.id, claim_quantity: quantity }],
        verification_code: verificationCode,
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/claim`, body);
      setActionResult({ action: "stored_wine_claimed", savedBottleId: selected.id, requestBody: body, result });
      await reload();
    } catch (claimError) {
      const message = claimError instanceof Error ? claimError.message : TEXT.claimFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_claimed", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="stored-wine-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="stored-wine-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="bar.saved_bottle.viewed / bar.saved_bottle_picked_up" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="stored-wine-kpi-focus-banner">
            正在查看：{kpiFocus === "stored" ? TEXT.storedBottles : TEXT.totalBottles}（{kpiRows.length}）
            <button type="button" data-testid="stored-wine-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="stored-wine-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.totalBottles}</span><button type="button" className="admin-kpi-value" data-testid="stored-wine-kpi-bottles" onClick={() => enterKpiFocus("bottles")}>{bottleCountDisplay}</button><small>{TEXT.totalBottlesHelp}{bottleCountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.saved_bottle_count" testid="stored-wine-metric-detail-bottles" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.storedBottles}</span><button type="button" className="admin-kpi-value" data-testid="stored-wine-kpi-stored" onClick={() => enterKpiFocus("stored")}>{storedDisplay}</button><small>{TEXT.storedHelp}{storedKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.saved_bottle_stored" testid="stored-wine-metric-detail-stored" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.totalQuantity}</span><strong data-testid="stored-wine-kpi-quantity">{quantityDisplay}</strong><small>{TEXT.totalQuantityHelp}{quantityKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.saved_bottle_quantity" testid="stored-wine-metric-detail-quantity" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedBottle}</span><strong className="admin-kpi-value--text" title={selected?.savedNo ?? "-"}>{selected?.savedNo ?? "-"}</strong><small>{selected?.customerName ?? "-"}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.onlineCheck}</span><strong>必需</strong><small>{TEXT.onlineCheckHelp}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="stored-wine-claim-form">
          <label>{TEXT.code}<input data-testid="stored-wine-claim-code" value={verificationCode} onChange={(event) => setVerificationCode(event.target.value)} /></label>
          <button type="button" className="admin-secondary-button" data-testid="stored-wine-claim-send-code" disabled={!selected} onClick={() => void sendVerificationCode("claim")}>{TEXT.sendCode}</button>
          <label>{TEXT.quantity}<input data-testid="stored-wine-claim-quantity" type="number" min="1" value={claimQuantity} onChange={(event) => setClaimQuantity(event.target.value)} /></label>
          <label>{TEXT.note}<input data-testid="stored-wine-claim-note" value={operatorNote} onChange={(event) => setOperatorNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.claim}
            impact="将在线核销选中存酒并扣减客户资产，写入幂等键、核验码与审计留痕，重复提交返回同一结果。"
            disabled={submitting}
            precheck={validateClaim}
            onConfirm={() => void claimStoredWine()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-claim"
          />
          <span className="admin-muted-action" data-testid="stored-wine-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="stored-wine-error" className="admin-inline-error">{formError}</div> : null}
        {codeNotice ? <div data-testid="stored-wine-code-notice" className="admin-muted-action">{codeNotice}</div> : null}

        <div className="admin-form-grid" data-testid="stored-wine-transfer-form">
          <label>{TEXT.transferToName}<input data-testid="stored-wine-transfer-to-name" value={transferToName} onChange={(event) => setTransferToName(event.target.value)} /></label>
          <label>{TEXT.transferToPhone}<input data-testid="stored-wine-transfer-to-phone" value={transferToPhone} onChange={(event) => setTransferToPhone(event.target.value)} /></label>
          <label>{TEXT.transferCode}<input data-testid="stored-wine-transfer-code" value={transferCode} onChange={(event) => setTransferCode(event.target.value)} /></label>
          <button type="button" className="admin-secondary-button" data-testid="stored-wine-transfer-send-code" disabled={!selected} onClick={() => void sendVerificationCode("transfer")}>{TEXT.sendCode}</button>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.transfer}
            impact={TEXT.transferBoundary}
            disabled={submitting}
            precheck={validateTransfer}
            onConfirm={() => void transferStoredWine()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-transfer"
          />
          <span className="admin-muted-action" data-testid="stored-wine-transfer-boundary">{TEXT.transferBoundary}</span>
        </div>
        {transferError ? <div data-testid="stored-wine-transfer-error" className="admin-inline-error">{transferError}</div> : null}

        <div className="admin-form-grid" data-testid="stored-wine-renewal-form">
          <label>{TEXT.renewalCode}<input data-testid="stored-wine-renewal-code" value={renewalCode} onChange={(event) => setRenewalCode(event.target.value)} /></label>
          <button type="button" className="admin-secondary-button" data-testid="stored-wine-renewal-send-code" disabled={!selected} onClick={() => void sendVerificationCode("renew")}>{TEXT.sendCode}</button>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.renewal}
            impact={TEXT.renewalBoundary}
            disabled={submitting}
            precheck={validateRenewal}
            onConfirm={() => void renewStoredWine()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-renewal"
          />
          <span className="admin-muted-action" data-testid="stored-wine-renewal-boundary">{TEXT.renewalBoundary}</span>
        </div>
        {renewalError ? <div data-testid="stored-wine-renewal-error" className="admin-inline-error">{renewalError}</div> : null}
        {selected ? (
          <div data-testid="stored-wine-renewal-list">
            <strong>{TEXT.renewalList}</strong>
            {renewals.length === 0 ? (
              <div className="admin-muted-action" data-testid="stored-wine-renewal-empty">{TEXT.renewalEmpty}</div>
            ) : (
              <ul className="admin-plain-list">
                {renewals.map((renewal) => (
                  <li key={renewal.id} data-testid={`stored-wine-renewal-row-${renewal.id}`}>
                    第 {renewal.renewalCount ?? "-"}/{renewal.maxRenewals ?? "-"} 次 · 顺延 {renewal.extensionDays ?? "-"} 天 · 新到期 {cnDate(renewal.newExpireAt)} · 经办 {renewal.operatorId ?? "-"} · {renewal.status === "pending_review" ? TEXT.renewalPending : renewal.status === "effective" ? TEXT.renewalEffective : renewal.status === "rejected" ? TEXT.renewalRejected : formatBusinessStatus(renewal.status)}
                    {renewal.status === "pending_review" ? (
                      <>
                        <HighRiskConfirm
                          label={TEXT.renewalReview}
                          impact="复核通过后立即从当前到期日顺延本次转存天数；复核人不得是经办人。"
                          disabled={submitting}
                          onConfirm={() => void reviewRenewal(renewal.id, "approve")}
                          permissionsState={effectivePermissions}
                          permissions={[STORED_WINE_RENEWAL_REVIEW_PERMISSION]}
                          data-testid={`stored-wine-renewal-review-${renewal.id}`}
                        />
                        <input aria-label={`驳回原因 ${renewal.id}`} data-testid={`stored-wine-renewal-reject-reason-${renewal.id}`} placeholder={TEXT.renewalRejectReason} value={renewalRejectReason[renewal.id] ?? ""} onChange={(event) => setRenewalRejectReason((current) => ({ ...current, [renewal.id]: event.target.value }))} />
                        <HighRiskConfirm
                          label={TEXT.renewalReject}
                          impact="驳回后该笔代办转存关闭：到期日不变、不计入转存次数，客人本人仍可再转存。"
                          disabled={submitting}
                          precheck={() => validateRenewalReject(renewal.id)}
                          onConfirm={() => void reviewRenewal(renewal.id, "reject")}
                          permissionsState={effectivePermissions}
                          permissions={[STORED_WINE_RENEWAL_REVIEW_PERMISSION]}
                          data-testid={`stored-wine-renewal-reject-${renewal.id}`}
                        />
                      </>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ) : null}

        <div className="admin-form-grid" data-testid="stored-wine-cancel-form">
          <label>{TEXT.cancelReason}<input data-testid="stored-wine-cancel-reason" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label>
          <label>{TEXT.cancelPhotos}<input data-testid="stored-wine-cancel-photos" value={cancelPhotos} onChange={(event) => setCancelPhotos(event.target.value)} /></label>
          <label>{TEXT.cancelVideos}<input data-testid="stored-wine-cancel-videos" value={cancelVideos} onChange={(event) => setCancelVideos(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.cancel}
            impact={TEXT.cancelBoundary}
            disabled={submitting}
            precheck={validateCancel}
            onConfirm={() => void cancelStoredWine()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-cancel"
          />
          <span className="admin-muted-action" data-testid="stored-wine-cancel-boundary">{TEXT.cancelBoundary}</span>
        </div>
        {cancelError ? <div data-testid="stored-wine-cancel-error" className="admin-inline-error">{cancelError}</div> : null}

        {selected?.status === "cancelled" ? (
          <>
            <div className="admin-form-grid" data-testid="stored-wine-remedy-panel">
              <label>{TEXT.remedyType}
                <select data-testid="stored-wine-remedy-type" value={remedyType} onChange={(event) => setRemedyType(event.target.value)}>
                  <option value="replacement">{REMEDY_TYPE_LABELS.replacement}</option>
                  <option value="compensation">{REMEDY_TYPE_LABELS.compensation}</option>
                </select>
              </label>
              {remedyType === "compensation" ? (
                <label>{TEXT.remedyAmount}<input data-testid="stored-wine-remedy-amount" type="number" min="0.01" step="0.01" value={remedyAmount} onChange={(event) => setRemedyAmount(event.target.value)} /></label>
              ) : (
                <>
                  <label>{TEXT.remedyPhotos}<input data-testid="stored-wine-remedy-photos" value={remedyPhotos} onChange={(event) => setRemedyPhotos(event.target.value)} /></label>
                  <label>{TEXT.remedyLabel}<input data-testid="stored-wine-remedy-label" value={remedyLabel} onChange={(event) => setRemedyLabel(event.target.value)} /></label>
                  <label>{TEXT.remedyLocation}<input data-testid="stored-wine-remedy-location" value={remedyLocation} onChange={(event) => setRemedyLocation(event.target.value)} /></label>
                </>
              )}
            </div>
            <div className="admin-form-row">
              <HighRiskConfirm
                label={TEXT.remedySubmit}
                impact={TEXT.remedyBoundary}
                disabled={submitting}
                precheck={validateRemedy}
                onConfirm={() => void registerRemedy()}
                permissionsState={effectivePermissions}
                permissions={[STORED_WINE_CLAIM_PERMISSION]}
                data-testid="stored-wine-remedy-submit"
              />
              <span className="admin-muted-action" data-testid="stored-wine-remedy-boundary">{TEXT.remedyBoundary}</span>
            </div>
          </>
        ) : null}
        {remedyError ? <div data-testid="stored-wine-remedy-error" className="admin-inline-error">{remedyError}</div> : null}
        {selected ? (
          <div data-testid="stored-wine-remedy-list">
            <strong>{TEXT.remedyList}</strong>
            {remedies.length === 0 ? (
              <div className="admin-muted-action" data-testid="stored-wine-remedy-empty">{TEXT.remedyEmpty}</div>
            ) : (
              <>
                <div className="admin-form-grid">
                  <label>{TEXT.remedyStaffUser}<input data-testid="stored-wine-remedy-staff-user" value={remedyStaffUser} onChange={(event) => setRemedyStaffUser(event.target.value)} /></label>
                  <label>{TEXT.remedyEvidence}<input data-testid="stored-wine-remedy-evidence" value={remedyEvidence} onChange={(event) => setRemedyEvidence(event.target.value)} /></label>
                </div>
                <ul className="admin-plain-list">
                  {remedies.map((remedy) => (
                    <li key={remedy.id} data-testid={`stored-wine-remedy-row-${remedy.id}`}>
                      {REMEDY_TYPE_LABELS[remedy.remedyType ?? ""] ?? remedy.remedyType ?? "-"}
                      {remedy.amountCent !== undefined && remedy.amountCent !== null ? ` · ${(remedy.amountCent / 100).toFixed(2)} 元` : ""}
                      {" · "}{TEXT.remedyBearer}：{REMEDY_BEARER_LABELS[remedy.bearerType ?? ""] ?? remedy.bearerType ?? "-"}
                      {" · "}{TEXT.remedyDelivery}：<span data-testid={`stored-wine-remedy-delivery-${remedy.id}`}>{REMEDY_DELIVERY_LABELS[remedy.customerDeliveryStatus ?? ""] ?? formatBusinessStatus(remedy.customerDeliveryStatus)}</span>
                      {" · "}{TEXT.remedyRecovery}：<span data-testid={`stored-wine-remedy-recovery-${remedy.id}`}>{REMEDY_PARTY_LABELS[remedy.recoveryPartyType ?? "none"] ?? remedy.recoveryPartyType}{remedy.recoveryPartyType && remedy.recoveryPartyType !== "none" ? "·" : ""}{REMEDY_RECOVERY_LABELS[remedy.recoveryStatus ?? "none"] ?? remedy.recoveryStatus}</span>
                      {remedy.customerDeliveryStatus === "pending_approval" ? (
                        <>
                          <HighRiskConfirm label={TEXT.remedyApproveSupervisor} impact="门店主管审批赔付方案；审批人不得是登记人。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "approve", { role: "supervisor" })}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_REMEDY_SUPERVISOR_PERMISSION]}
                            data-testid={`stored-wine-remedy-approve-sup-${remedy.id}`} />
                          <HighRiskConfirm label={TEXT.remedyApproveFinance} impact="财务审批赔付方案；两方审批后才可支付结案。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "approve", { role: "finance" })}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_REMEDY_FINANCE_PERMISSION]}
                            data-testid={`stored-wine-remedy-approve-fin-${remedy.id}`} />
                        </>
                      ) : null}
                      {remedy.customerDeliveryStatus === "approved" ? (
                        <HighRiskConfirm label={TEXT.remedySettle} impact="赔款实际交付后才结案；记交付/支付凭证与时间并计入经营损失。" disabled={submitting}
                          onConfirm={() => void remedyAction(remedy.id, "settle", { evidence_ids: splitIds(remedyEvidence) })}
                          permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                          data-testid={`stored-wine-remedy-settle-${remedy.id}`} />
                      ) : null}
                      {(remedy.recoveryPartyType ?? "none") === "none" ? (
                        <>
                          <HighRiskConfirm label={TEXT.remedyAdvance} impact="娱乐平台公司审批先垫付，对客结案后向原门店挂追收应收。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "advance", {})}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                            data-testid={`stored-wine-remedy-advance-${remedy.id}`} />
                          <HighRiskConfirm label={TEXT.remedyStaff} impact="复核认定责任员工照价赔偿；金额由系统按内部结算价计，不改对客状态。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "staff-liability", { staff_user_id: remedyStaffUser.trim(), evidence_ids: splitIds(remedyEvidence) })}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                            data-testid={`stored-wine-remedy-staff-${remedy.id}`} />
                        </>
                      ) : null}
                      {remedy.recoveryPartyType === "staff" && remedy.recoveryStatus === "pending" ? (
                        <HighRiskConfirm label={TEXT.remedyRecoverySettle} impact="登记员工赔偿已收回（不改对客状态）。" disabled={submitting}
                          onConfirm={() => void remedyAction(remedy.id, "recovery/settle", { evidence_ids: splitIds(remedyEvidence) })}
                          permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                          data-testid={`stored-wine-remedy-recovery-settle-${remedy.id}`} />
                      ) : null}
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        ) : null}

        <div className="admin-form-grid" data-testid="stored-wine-exit-block">
          <label>{TEXT.exitMerchant}<input data-testid="stored-wine-exit-merchant" value={exitMerchantId} onChange={(event) => setExitMerchantId(event.target.value)} /></label>
          <label>{TEXT.handoverLocation}<input data-testid="stored-wine-handover-location" value={handoverLocation} onChange={(event) => setHandoverLocation(event.target.value)} /></label>
          <label>{TEXT.handoverPhotos}<input data-testid="stored-wine-handover-photos" value={handoverPhotos} onChange={(event) => setHandoverPhotos(event.target.value)} /></label>
          <label>{TEXT.handoverVideos}<input data-testid="stored-wine-handover-videos" value={handoverVideos} onChange={(event) => setHandoverVideos(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.exitNotice}
            impact={TEXT.exitNoticeBoundary}
            disabled={submitting}
            precheck={validateExit}
            onConfirm={() => void noticeExit()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-exit-notice"
          />
          <HighRiskConfirm
            label={TEXT.handover}
            impact={TEXT.handoverBoundary}
            disabled={submitting}
            precheck={validateHandover}
            onConfirm={() => void handoverToPlatform()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-handover"
          />
          <span className="admin-muted-action" data-testid="stored-wine-exit-boundary">{TEXT.handoverBoundary}</span>
        </div>
        {exitResult ? <div data-testid="stored-wine-exit-result" className="admin-muted-action">{exitResult}</div> : null}
        {exitError ? <div data-testid="stored-wine-exit-error" className="admin-inline-error">{exitError}</div> : null}
        {handoverRows.length > 0 ? (
          <div data-testid="stored-wine-handover-items">
            <strong>{TEXT.handoverItems}</strong>
            <ul className="admin-plain-list">
              {handoverRows.map((row) => (
                <li key={row.id} data-testid={`stored-wine-handover-item-${row.id}`}>
                  {row.savedNo ?? row.id} · {row.itemName ?? row.itemId ?? "-"}
                  <input aria-label={`实收数量 ${row.id}`} data-testid={`stored-wine-handover-qty-${row.id}`} type="number" min="0" value={handoverItems[row.id]?.quantity ?? String(row.remainingQuantity ?? row.quantity ?? "")} onChange={(event) => setHandoverItem(row.id, { quantity: event.target.value })} />
                  <select aria-label={`接收状态 ${row.id}`} data-testid={`stored-wine-handover-condition-${row.id}`} value={handoverItems[row.id]?.condition ?? "intact"} onChange={(event) => setHandoverItem(row.id, { condition: event.target.value })}>
                    {HANDOVER_CONDITIONS.map((condition) => <option key={condition.value} value={condition.value}>{condition.label}</option>)}
                  </select>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
        </>
        )}

        <div className="admin-list-toolbar" data-testid="stored-wine-filter">
          <input data-testid="stored-wine-search" value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); void lookupByPhone(); } }} aria-label={TEXT.searchPlaceholder} placeholder={TEXT.searchPlaceholder} inputMode="tel" />
          <button type="button" className="admin-secondary-button" data-testid="stored-wine-search-submit" onClick={() => void lookupByPhone()}>{TEXT.search}</button>
          <span className="admin-muted-action" data-testid="stored-wine-filter-count">共 {filteredRows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="stored-wine-filter-empty">无匹配存酒记录</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="stored-wine-table">
            <thead>
              <tr><th>{TEXT.select}</th><th className="admin-sort-th" data-testid="stored-wine-sort-savedNo" onClick={() => sort.toggle("savedNo")} {...sort.thProps("savedNo")}>{TEXT.savedNo}{sort.indicator("savedNo")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-customer" onClick={() => sort.toggle("customer")} {...sort.thProps("customer")}>{TEXT.customer}{sort.indicator("customer")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-item" onClick={() => sort.toggle("item")} {...sort.thProps("item")}>{TEXT.item}{sort.indicator("item")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-quantity" onClick={() => sort.toggle("quantity")} {...sort.thProps("quantity")}>{TEXT.quantity}{sort.indicator("quantity")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-status" onClick={() => sort.toggle("status")} {...sort.thProps("status")}>{TEXT.status}{sort.indicator("status")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-createdAt" onClick={() => sort.toggle("createdAt")} {...sort.thProps("createdAt")}>{TEXT.createdAt}{sort.indicator("createdAt")}</th><th>{TEXT.custody}</th><th>详情</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`stored-wine-row-${row.id}`}
                  onClick={() => openBottleDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBottleDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此存酒 ${row.id}`} data-testid={`stored-wine-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.savedNo ?? row.id}</td>
                  <td>{row.customerName ?? row.customerId ?? "-"}</td>
                  <td>{row.itemName ?? row.itemId ?? "-"}</td>
                  <td>{row.remainingQuantity ?? row.quantity ?? "-"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td>{cnDate(row.createdAt)}</td>
                  <td data-testid={`stored-wine-custody-${row.id}`}>{row.pickupLocation ?? TEXT.custodyOriginal}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看存酒详情" onClick={(event) => { event.stopPropagation(); openBottleDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="stored-wine-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
