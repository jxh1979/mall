// 文件版本：V0.11
import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import type { AdminPageProps, InventoryItemRow, StoredWineRemedyRow, StoredWineRow } from "../types";
import { PaymentConfirmationPanel } from "./PaymentConfirmationPanel";
import { StoredWineEvidenceUpload, type StoredWineEvidenceFile } from "./StoredWineEvidenceUpload";

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

// 第十八批 AP 段 254-R5：人工例外取酒申请（GET /saved-bottles/{id}/manual-pickups；adminClient 驼峰化）
type StoredWineManualPickupRow = {
  id: string;
  status?: string;
  historyCondition?: string;
  reason?: string;
  initiatedBy?: string;
  initiatedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  marketingConfirmedBy?: string;
  marketingConfirmedAt?: string;
  dispensedBy?: string;
  dispensedAt?: string;
};

// 后端写权限码（InventoryController：/saved-bottles/{id}/claim、/transfer、/verification-code）
const STORED_WINE_CLAIM_PERMISSION = "bar.saved_bottle.manage";
// FS-025 员工代办转存的营销主管复核权限码（InventoryService.RENEWAL_REVIEW）
const STORED_WINE_RENEWAL_REVIEW_PERMISSION = "bar.saved_bottle.renewal.review";
// FS-017 赔付方案审批：门店主管用高价值复核码、财务用既有应收应付管理码（InventoryService.REMEDY_*_APPROVE）。
// 第十八批 AP 段 254-R6（第十八批裁决 58）：争议不成立恢复存酒、争议中的存酒作废同用门店主管码。
const STORED_WINE_REMEDY_SUPERVISOR_PERMISSION = "inventory.high_value.review";
const STORED_WINE_REMEDY_FINANCE_PERMISSION = "finance.ar_ap.manage";
// 第十八批 AP 段 254-R5：人工例外取酒各步权限码（InventoryService.MANUAL_PICKUP_*；发起时服务端另核客人查询专门通道码 customer.account.manage；放酒用存酒管理码）。
const STORED_WINE_MANUAL_PICKUP_INITIATE_PERMISSIONS = ["bar.saved_bottle.manual_pickup.initiate", "customer.account.manage"];
const STORED_WINE_MANUAL_PICKUP_REVIEW_PERMISSION = "bar.saved_bottle.manual_pickup.review";
const STORED_WINE_MANUAL_PICKUP_CONFIRM_MARKETING_PERMISSION = "bar.saved_bottle.manual_pickup.confirm_marketing";
// 命中的消费历史条件（服务端按 BSM:1073 原句顺序取第一个满足的）。
const MANUAL_PICKUP_CONDITION_LABELS: Record<string, string> = {
  orders_within_1m: "近 1 个月笔数达标",
  orders_within_3m: "近 3 个月笔数达标",
  orders_total: "累计笔数达标",
};

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

// 第十八批 AP 段 254-R4：存酒登记表单初值（数量默认 1）。
const EMPTY_REGISTER = { itemId: "", quantity: "1", label: "", phone: "", name: "", note: "", code: "" };

const TEXT = {
  title: "存酒",
  description: "展示客人存酒资产、在线领取核销、证据附件和幂等留痕；高价值酒水保持在线校验，不做离线核销。",
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
  boundary: "领取会写入幂等键、核验码和审计事件；重复提交必须返回同一处理结果，避免重复扣减客户资产。",
  select: "选择",
  savedNo: "存酒号",
  customer: "客户",
  item: "酒水",
  status: "状态",
  createdAt: "时间",
  totalBottlesHelp: "存酒记录总数",
  storedHelp: "在线可领取",
  totalQuantityHelp: "剩余可领数量",
  focusClear: "清除筛选，返回全部",
  // FS-017（BSM L1072）
  cancel: "作废",
  cancelReason: "作废原因",
  cancelPhotos: "作废照片（拍照上传）",
  cancelVideos: "作废视频（录像上传）",
  cancelNotCancellable: "仅存酒中、待核身复核或争议中的存酒可作废。",
  // 第十八批 AY 段 269-R2（第十八批裁决 14）：高低价值由服务端在作废当刻判定，前端不预检照片视频。
  cancelBoundary: "高价值存酒作废须上传照片和视频，先冻结、待主管复核通过后才作废；其余存酒提交即作废。作废后在下方登记补存或赔付。",
  cancelFailed: "存酒作废失败",
  // 第十八批 AP 段 252-R1（BSM:1940）、254-R6（第十八批裁决 58）
  cancelReasonRequired: "作废须填写作废原因",
  dispute: "登记争议",
  disputeReason: "争议原因",
  disputeReasonRequired: "登记争议须填写争议原因",
  disputeBoundary: "登记后停止取酒、转让与转存，原持有人的动态取酒码立即作废；由门店主管核实后恢复存酒或作废。",
  disputeFailed: "存酒争议登记失败",
  disputeDismiss: "争议不成立，恢复存酒",
  disputeDismissBoundary: "门店主管核实争议不成立：恢复为存酒中、照常可取，剩余量与到期日不变；争议成立请在下方作废。",
  disputeDismissFailed: "恢复存酒失败",
  remedy: "补存/赔付",
  remedyType: "方式",
  remedyAmount: "赔付金额（元）",
  remedyPhotos: "补存瓶登记照片（拍照上传）",
  remedyLabel: "补存瓶底标签值",
  remedyLocation: "出库库位",
  remedyNote: "补存瓶人工备注",
  remedySubmit: "登记补存/赔付",
  remedyBoundary: "作废后由该酒吧为客人以同品、同等剩余量补存，到期日不变，补酒成本计入经营损失；无法补存的登记赔付方案，经门店主管和财务审批后支付，赔款实际交付后才结案。对客交付与内部追收分别记录，员工未赔付不阻止对客交付。",
  remedyNotCancelled: "仅已作废的存酒可登记补存/赔付。",
  remedyAmountInvalid: "赔付金额必须大于 0。",
  remedyReplacementRequired: "补存须上传登记照片，并填写补存瓶底标签值、出库库位与补存瓶人工备注。",
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
  // 第十八批 AY 段 S1（PGM:287）：标签与提示全中文，只改文案。
  remedyStaffUser: "责任员工编号",
  remedyRecoverySettle: "追收已收回",
  remedyEvidence: "交付/收回凭证（上传照片或视频）",
  remedyActionFailed: "补存/赔付处理失败",
  exit: "门店退出",
  exitMerchant: "退出门店（商户）编号",
  exitNotice: "清点并通知客人原店取回",
  exitNoticeBoundary: "退出前清点有效期内未结存酒并逐瓶短信通知客人在原店取回。",
  exitRequired: "须填写退出门店编号。",
  exitFailed: "退出清点通知失败",
  handover: "交接到平台",
  handoverLocation: "指定领取地点",
  handoverPhotos: "交接现场照片（拍照上传）",
  handoverVideos: "交接现场视频（录像上传）",
  handoverItems: "逐瓶清点（实收数量 / 接收状态）",
  handoverRequired: "交接须填写退出门店编号与指定领取地点。",
  handoverBoundary: "门店退出后由所属娱乐平台公司接收有效期内未结存酒实物和记录，指定领取地点，继续按原有效期与原取酒规则处理；接收时已存在的毁损、短少仍由原门店承担，可经审批先垫付并向原门店追收。",
  handoverFailed: "存酒交接失败",
  // 第十八批 AP 段 254-R4（BSM:1037、BSM:1170；BSM:1072「存酒登记先取隐私同意……」；第十八批裁决 57 取短信验证码）
  register: "存酒登记",
  registerItem: "酒水",
  registerItemPlaceholder: "选择酒水",
  registerQuantity: "数量",
  registerLabel: "瓶底标签值",
  registerPhone: "客人手机号",
  registerName: "客人姓名/昵称",
  registerPhotos: "登记照片（拍照上传）",
  registerNote: "人工备注",
  registerSendCode: "向客人发送隐私同意验证码",
  registerCode: "客人报出的验证码",
  registerConsent: "已向客人说明隐私政策，客人报出短信验证码即视为同意。",
  registerSubmit: "登记存酒",
  registerCodeSent: "隐私同意验证码已发送至客人手机",
  registerItemRequired: "请选择存酒酒水并填写大于 0 的数量。",
  registerFieldsRequired: "存酒登记须填写瓶底标签值、客人手机号与姓名/昵称。",
  registerPhotoRequired: "存酒登记须拍照上传至少一张登记照片。",
  registerNoteRequired: "存酒登记须填写人工备注。",
  registerCodeRequired: "须填写客人报出的隐私同意短信验证码。",
  registerPhoneRequired: "发送验证码前须填写客人手机号。",
  registerFailed: "存酒登记失败",
  registerUploadFailed: "登记照片上传失败",
  registerMerchantOnly: "存酒登记须由酒吧员工账号办理。",
  // 第十八批 AP 段 254-R5（BSM:1073 人工例外取酒须同时满足四项）
  manualPickup: "人工例外取酒",
  manualPickupBoundary: "仅在客人手机遗失、停机等无法获取验证码或动态取酒码时适用：店长发起（申请手机号须在本项目内满足任一消费历史条件）→ 另一名主管复核 → 该客户对应的营销人员确认 → 客人本人到场放酒，不允许他人代领。",
  manualPickupReason: "发起原因",
  manualPickupReasonRequired: "发起人工例外取酒须填写原因",
  manualPickupInitiate: "店长发起人工例外取酒",
  manualPickupReview: "另一主管复核",
  manualPickupConfirmMarketing: "营销人员确认",
  manualPickupQuantity: "放酒数量",
  manualPickupInPerson: "客户本人到场",
  manualPickupDispense: "本人到场放酒",
  manualPickupEmpty: "该存酒暂无人工例外取酒申请。",
  manualPickupFailed: "人工例外取酒办理失败",
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

export function StoredWinePage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, reportViewedObjects }: AdminPageProps) {
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
  // 第十八批 AP 段 254-R5：人工例外取酒（该瓶申请列表、发起原因、放酒数量与本人到场勾选）。
  const [manualPickups, setManualPickups] = React.useState<StoredWineManualPickupRow[]>([]);
  const [manualPickupReason, setManualPickupReason] = React.useState("");
  const [manualPickupQuantity, setManualPickupQuantity] = React.useState("1");
  const [manualPickupInPerson, setManualPickupInPerson] = React.useState(false);
  const [manualPickupError, setManualPickupError] = React.useState("");
  // FS-017 作废（照片+视频双证据）/ 补存赔付 / 门店退出清点与交接。
  const [cancelReason, setCancelReason] = React.useState("");
  // 第十八批 AY 段 269-R1：六处证据改为真实上传，状态存服务端回的附件编号与文件名。
  // 收尾复核（BSM:1940、第十八批裁决 14）：提交成功后清空本次所用证据；换选存酒清空作废、补存与结案追收证据（交接证据随退出门店，不随选中存酒）。
  const [cancelPhotos, setCancelPhotos] = React.useState<StoredWineEvidenceFile[]>([]);
  const [cancelVideos, setCancelVideos] = React.useState<StoredWineEvidenceFile[]>([]);
  const [cancelError, setCancelError] = React.useState("");
  // 第十八批 AP 段 254-R6：登记争议（原因必填、进审计）。
  const [disputeReason, setDisputeReason] = React.useState("");
  const [disputeError, setDisputeError] = React.useState("");
  const [remedyType, setRemedyType] = React.useState("replacement");
  const [remedyAmount, setRemedyAmount] = React.useState("");
  const [remedyPhotos, setRemedyPhotos] = React.useState<StoredWineEvidenceFile[]>([]);
  const [remedyLabel, setRemedyLabel] = React.useState("");
  const [remedyLocation, setRemedyLocation] = React.useState("");
  const [remedyStaffUser, setRemedyStaffUser] = React.useState("");
  const [remedyEvidence, setRemedyEvidence] = React.useState<StoredWineEvidenceFile[]>([]);
  const [remedyError, setRemedyError] = React.useState("");
  const [remedies, setRemedies] = React.useState<StoredWineRemedyRow[]>([]);
  const [exitMerchantId, setExitMerchantId] = React.useState("");
  const [exitResult, setExitResult] = React.useState("");
  const [exitError, setExitError] = React.useState("");
  const [handoverLocation, setHandoverLocation] = React.useState("");
  const [handoverPhotos, setHandoverPhotos] = React.useState<StoredWineEvidenceFile[]>([]);
  const [handoverVideos, setHandoverVideos] = React.useState<StoredWineEvidenceFile[]>([]);
  const [handoverItems, setHandoverItems] = React.useState<Record<string, { quantity: string; condition: string }>>({});
  const [claimQuantity, setClaimQuantity] = React.useState("1");
  // 第十八批 AP 段 254-R3（BSM:1170「存酒登记必须拍照、填写人工备注……」）：补存新瓶的人工备注，空默认，只在同品补存时作为 operator_note 发出。
  const [remedyNote, setRemedyNote] = React.useState("");
  // 第十八批 AP 段 254-R4：存酒登记只对酒吧员工账号开放，酒吧＝本账号商户（平台账号登记会落空商户）。
  const merchantId = user.merchantId ?? "";
  const [register, setRegister] = React.useState(EMPTY_REGISTER);
  const [registerPhotos, setRegisterPhotos] = React.useState<Array<{ id: string; fileName: string }>>([]);
  const [registerItems, setRegisterItems] = React.useState<InventoryItemRow[]>([]);
  const [registerError, setRegisterError] = React.useState("");
  const [registerNotice, setRegisterNotice] = React.useState("");
  const [uploading, setUploading] = React.useState(false);
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
    if (!merchantId) return;
    api.get<InventoryItemRow[]>("/api/v1/inventory/items")
      .then((data) => setRegisterItems(Array.isArray(data) ? data : []), () => setRegisterItems([]));
  }, [api, merchantId]);

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

  const loadManualPickups = React.useCallback(async (savedBottleId: string) => {
    try {
      const data = await api.get<StoredWineManualPickupRow[]>(`/api/v1/inventory/saved-bottles/${savedBottleId}/manual-pickups`);
      setManualPickups(Array.isArray(data) ? data : []);
    } catch {
      setManualPickups([]);
    }
  }, [api]);

  React.useEffect(() => {
    if (!selectedId) {
      setManualPickups([]);
      return;
    }
    void loadManualPickups(selectedId);
  }, [selectedId, loadManualPickups]);

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

  // ===== 第十八批 AP 段 254-R5：人工例外取酒四步（店长发起 → 另一主管复核 → 营销确认 → 本人到场放酒）=====
  // 各步幂等键带时刻：停在某步的申请可重新发起；服务端拒绝（自审、营销不符、消费历史不满足等）原文上屏。
  function validateManualPickup() {
    if (!manualPickupReason.trim()) {
      setManualPickupError(TEXT.manualPickupReasonRequired);
      return false;
    }
    setManualPickupError("");
    return true;
  }

  function validateManualPickupQuantity() {
    const quantity = Number(manualPickupQuantity);
    if (!Number.isInteger(quantity) || quantity <= 0) {
      setManualPickupError(TEXT.quantityInvalid);
      return false;
    }
    setManualPickupError("");
    return true;
  }

  async function manualPickupAction(action: string, path: string, body: Record<string, unknown>) {
    if (!selected) {
      return;
    }
    setSubmitting(true);
    setManualPickupError("");
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/inventory/saved-bottles/${path}`, body);
      setActionResult({ action, savedBottleId: selected.id, requestBody: body, result });
      setManualPickupReason("");
      setManualPickupInPerson(false);
      await loadManualPickups(selected.id);
      await reload();
    } catch (pickupErr) {
      const message = pickupErr instanceof Error ? pickupErr.message : TEXT.manualPickupFailed;
      setManualPickupError(message);
      setGlobalError(message);
      setActionResult({ action, savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function initiateManualPickup() {
    if (!selected) {
      return;
    }
    void manualPickupAction("stored_wine_manual_pickup_initiated", `${selected.id}/manual-pickups`,
      { idempotency_key: `admin-saved-bottle-manual-pickup-${selected.id}-${Date.now()}`, reason: manualPickupReason.trim() });
  }

  function dispenseManualPickup(pickupId: string) {
    if (!selected) {
      return;
    }
    void manualPickupAction("stored_wine_manual_pickup_dispensed", `manual-pickups/${pickupId}/dispense`, {
      idempotency_key: `admin-saved-bottle-manual-pickup-dispense-${pickupId}-${Date.now()}`,
      present_in_person: true,
      claim_items: [{ saved_bottle_id: selected.id, quantity: Number(manualPickupQuantity) }],
    });
  }

  // ===== FS-017 作废 / 补存赔付 / 门店退出 =====
  function validateCancel() {
    if (!selected) {
      setCancelError(TEXT.noBottle);
      return false;
    }
    if (!["stored", "pending_identity_review", "disputed"].includes(selected.status ?? "")) {
      setCancelError(TEXT.cancelNotCancellable);
      return false;
    }
    if (!cancelReason.trim()) {
      setCancelError(TEXT.cancelReasonRequired);
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
        reason: cancelReason.trim(),
        photo_attachment_ids: cancelPhotos.map((f) => f.id),
        video_attachment_ids: cancelVideos.map((f) => f.id),
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/cancel`, body);
      setActionResult({ action: "stored_wine_cancelled", savedBottleId: selected.id, requestBody: body, result });
      setCancelPhotos([]);
      setCancelVideos([]);
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

  // 第十八批 AP 段 254-R6（第十八批裁决 58）：登记争议（原因必填）；争议不成立由门店主管恢复存酒中，争议成立走上面的作废。
  // 幂等键带时刻：服务端操作号按瓶固定，恢复后再次登记争议若沿用同号会被当成重交。
  function validateDispute() {
    if (!disputeReason.trim()) {
      setDisputeError(TEXT.disputeReasonRequired);
      return false;
    }
    setDisputeError("");
    return true;
  }

  async function submitDisputeAction(action: string, path: string, body: Record<string, unknown>, fallback: string) {
    if (!selected) {
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/${path}`, body);
      setActionResult({ action, savedBottleId: selected.id, requestBody: body, result });
      setDisputeReason("");
      await reload();
    } catch (disputeErr) {
      const message = disputeErr instanceof Error ? disputeErr.message : fallback;
      setDisputeError(message);
      setGlobalError(message);
      setActionResult({ action, savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function disputeStoredWine() {
    if (!selected || !validateDispute()) {
      return;
    }
    void submitDisputeAction("stored_wine_disputed", "dispute",
      { idempotency_key: `admin-saved-bottle-dispute-${selected.id}-${Date.now()}`, reason: disputeReason.trim() }, TEXT.disputeFailed);
  }

  function dismissDispute() {
    if (!selected) {
      return;
    }
    void submitDisputeAction("stored_wine_dispute_dismissed", "dispute-dismissal",
      { idempotency_key: `admin-saved-bottle-dispute-dismissal-${selected.id}-${Date.now()}` }, TEXT.disputeDismissFailed);
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
    } else if (remedyPhotos.length === 0 || !remedyLabel.trim() || !remedyLocation.trim() || !remedyNote.trim()) {
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
        ...(remedyType === "replacement" ? { operator_note: remedyNote.trim() } : {}),
        remedy_type: remedyType,
        amount_cent: remedyType === "compensation" ? Math.round(Number(remedyAmount) * 100) : undefined,
        reason: cancelReason.trim() || undefined,
        registration_photo_refs: remedyType === "replacement" ? remedyPhotos.map((f) => f.id) : undefined,
        bottle_label_value: remedyType === "replacement" ? remedyLabel.trim() : undefined,
        location_id: remedyType === "replacement" ? remedyLocation.trim() : undefined,
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/remedy`, body);
      setActionResult({ action: "stored_wine_remedy_registered", savedBottleId: selected.id, requestBody: body, result });
      setRemedyPhotos([]);
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
      const body = { idempotency_key: `admin-saved-bottle-remedy-${path.replace("/", "-")}-${remedyId}`, ...extra };
      const result = await api.post(`/api/v1/inventory/saved-bottles/remedies/${remedyId}/${path}`, body);
      setActionResult({ action: `stored_wine_remedy_${path.replace("/", "_")}`, remedyId, requestBody: body, result });
      if ("evidence_ids" in extra) setRemedyEvidence([]);
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
      const body = { idempotency_key: `admin-saved-bottle-exit-notice-${exitMerchantId.trim()}-${Date.now()}`, merchant_id: exitMerchantId.trim() };
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
        merchant_id: exitMerchantId.trim(),
        pickup_location: handoverLocation.trim(),
        photo_attachment_ids: handoverPhotos.map((f) => f.id),
        video_attachment_ids: handoverVideos.map((f) => f.id),
        items: handoverRows.map((row) => ({
          saved_bottle_id: row.id,
          received_quantity: numberValue(handoverItems[row.id]?.quantity ?? row.remainingQuantity ?? row.quantity),
          condition: handoverItems[row.id]?.condition ?? "intact",
        })),
      };
      const result = await api.post("/api/v1/inventory/saved-bottles/handover", body);
      setActionResult({ action: "stored_wine_handed_over", requestBody: body, result });
      setHandoverPhotos([]);
      setHandoverVideos([]);
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
        // 第十八批 AY 段 268-R2：项形状同服务端所读（saved_bottle_id、quantity）；幂等键带时刻，同一瓶可分次取用同样数量。
        idempotency_key: `admin-saved-bottle-claim-${selected.id}-${Date.now()}`,
        claim_items: [{ saved_bottle_id: selected.id, quantity }],
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

  // ===== 第十八批 AP 段 254-R4：存酒登记（酒吧员工账号，酒吧＝本账号商户；照片真实上传；客人报隐私同意短信验证码）=====
  function setRegisterField(field: keyof typeof EMPTY_REGISTER, value: string) {
    setRegister((current) => ({ ...current, [field]: value }));
  }

  async function uploadRegisterPhotos(files: FileList | null) {
    if (!files || files.length === 0) return;
    setUploading(true);
    setRegisterError("");
    try {
      const uploaded: Array<{ id: string; fileName: string }> = [];
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", "saved_bottle_registration");
        form.append("attachment_role", "photo");
        if (!api.postForm) throw new Error("当前客户端不支持文件上传。");
        const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
        uploaded.push({ id: saved.id, fileName: saved.fileName ?? file.name });
      }
      setRegisterPhotos((current) => [...current, ...uploaded]);
    } catch (uploadError) {
      setRegisterError(uploadError instanceof Error ? uploadError.message : TEXT.registerUploadFailed);
    } finally {
      setUploading(false);
    }
  }

  async function sendDepositConsentCode() {
    const phone = register.phone.trim();
    if (!phone) {
      setRegisterError(TEXT.registerPhoneRequired);
      return;
    }
    setRegisterError("");
    setRegisterNotice("");
    setGlobalError(null);
    try {
      // 幂等键带时刻：客人没收到可重发。
      const body = { idempotency_key: `admin-saved-bottle-deposit-consent-${Date.now()}`, customer_phone: phone };
      const result = await api.post("/api/v1/inventory/saved-bottles/deposit-consent-code", body);
      setActionResult({ action: "stored_wine_deposit_consent_code_sent", requestBody: body, result });
      setRegisterNotice(TEXT.registerCodeSent);
    } catch (sendError) {
      const message = sendError instanceof Error ? sendError.message : TEXT.sendCodeFailed;
      setRegisterError(message);
      setGlobalError(message);
    }
  }

  function validateRegister() {
    const quantity = Number(register.quantity);
    const error = !register.itemId || !Number.isInteger(quantity) || quantity <= 0 ? TEXT.registerItemRequired
      : !register.label.trim() || !register.phone.trim() || !register.name.trim() ? TEXT.registerFieldsRequired
      : registerPhotos.length === 0 ? TEXT.registerPhotoRequired
      : !register.note.trim() ? TEXT.registerNoteRequired
      : !register.code.trim() ? TEXT.registerCodeRequired
      : "";
    setRegisterError(error);
    return !error;
  }

  async function registerStoredWine() {
    const item = registerItems.find((row) => row.id === register.itemId);
    // 到期日不发：服务端按本项目存酒有效期在登记时固化（254-R2）；存酒编号由幂等键派生。
    const body = {
      idempotency_key: `admin-saved-bottle-register-${Date.now()}`,
      bar_id: merchantId,
      items: [{ item_id: register.itemId, quantity: Number(register.quantity), unit: item?.unit, bottle_name: item?.itemName }],
      bottle_label_value: register.label.trim(),
      customer_phone: register.phone.trim(),
      customer_name_or_nickname: register.name.trim(),
      registration_photo_refs: registerPhotos.map((photo) => photo.id),
      manual_note: register.note.trim(),
      verification_code: register.code.trim(),
    };
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/inventory/saved-bottles", body);
      setActionResult({ action: "stored_wine_registered", requestBody: body, result });
      setRegister(EMPTY_REGISTER);
      setRegisterPhotos([]);
      setRegisterNotice("");
      await reload();
    } catch (registerErr) {
      const message = registerErr instanceof Error ? registerErr.message : TEXT.registerFailed;
      setRegisterError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_registered", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="stored-wine-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      {kpiFocus ? null : merchantId ? (
        <div data-testid="stored-wine-register">
          <strong>{TEXT.register}</strong>
          <div className="admin-form-grid" data-testid="stored-wine-register-form">
            <label>{TEXT.registerItem}
              <select data-testid="stored-wine-register-item" value={register.itemId} onChange={(event) => setRegisterField("itemId", event.target.value)}>
                <option value="">{TEXT.registerItemPlaceholder}</option>
                {registerItems.map((row) => <option key={row.id} value={row.id}>{row.itemName ?? row.id}</option>)}
              </select>
            </label>
            <label>{TEXT.registerQuantity}<input data-testid="stored-wine-register-quantity" type="number" min="1" value={register.quantity} onChange={(event) => setRegisterField("quantity", event.target.value)} /></label>
            <label>{TEXT.registerLabel}<input data-testid="stored-wine-register-label" value={register.label} onChange={(event) => setRegisterField("label", event.target.value)} /></label>
            <label>{TEXT.registerPhone}<input data-testid="stored-wine-register-phone" inputMode="tel" value={register.phone} onChange={(event) => setRegisterField("phone", event.target.value)} /></label>
            <label>{TEXT.registerName}<input data-testid="stored-wine-register-name" value={register.name} onChange={(event) => setRegisterField("name", event.target.value)} /></label>
            <label>{TEXT.registerPhotos}<input data-testid="stored-wine-register-photo-upload" type="file" multiple accept="image/*" disabled={uploading} onChange={(event) => void uploadRegisterPhotos(event.target.files)} /></label>
            <label>{TEXT.registerNote}<input data-testid="stored-wine-register-note" value={register.note} onChange={(event) => setRegisterField("note", event.target.value)} /></label>
            <button type="button" className="admin-secondary-button" data-testid="stored-wine-register-send-code" onClick={() => void sendDepositConsentCode()}>{TEXT.registerSendCode}</button>
            <label>{TEXT.registerCode}<input data-testid="stored-wine-register-code" value={register.code} onChange={(event) => setRegisterField("code", event.target.value)} /></label>
          </div>
          {registerPhotos.length > 0 ? <div className="admin-muted-action" data-testid="stored-wine-register-photos">{registerPhotos.map((photo) => photo.fileName).join("、")}</div> : null}
          <div className="admin-form-row">
            <HighRiskConfirm
              label={TEXT.registerSubmit}
              impact={TEXT.registerConsent}
              disabled={submitting || uploading}
              precheck={validateRegister}
              onConfirm={() => void registerStoredWine()}
              permissionsState={effectivePermissions}
              permissions={[STORED_WINE_CLAIM_PERMISSION]}
              data-testid="stored-wine-register-submit"
            />
            <span className="admin-muted-action" data-testid="stored-wine-register-consent">{TEXT.registerConsent}</span>
          </div>
          {registerNotice ? <div className="admin-muted-action" data-testid="stored-wine-register-notice">{registerNotice}</div> : null}
          {registerError ? <div className="admin-inline-error" data-testid="stored-wine-register-error">{registerError}</div> : null}
        </div>
      ) : (
        <div className="admin-muted-action" data-testid="stored-wine-register-merchant-only">{TEXT.registerMerchantOnly}</div>
      )}
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

        {selected ? (
          <div data-testid="stored-wine-manual-pickup">
            <strong>{TEXT.manualPickup}</strong>
            <span className="admin-muted-action" data-testid="stored-wine-manual-pickup-boundary">{TEXT.manualPickupBoundary}</span>
            {selected.status === "stored" ? (
              <div className="admin-form-row">
                <label>{TEXT.manualPickupReason}<input data-testid="stored-wine-manual-pickup-reason" value={manualPickupReason} onChange={(event) => setManualPickupReason(event.target.value)} /></label>
                <HighRiskConfirm
                  label={TEXT.manualPickupInitiate}
                  impact={TEXT.manualPickupBoundary}
                  disabled={submitting}
                  precheck={validateManualPickup}
                  onConfirm={initiateManualPickup}
                  permissionsState={effectivePermissions}
                  permissions={STORED_WINE_MANUAL_PICKUP_INITIATE_PERMISSIONS}
                  data-testid="stored-wine-manual-pickup-initiate"
                />
              </div>
            ) : null}
            {manualPickups.length === 0 ? (
              <div className="admin-muted-action" data-testid="stored-wine-manual-pickup-empty">{TEXT.manualPickupEmpty}</div>
            ) : (
              <ul className="admin-plain-list">
                {manualPickups.map((pickup) => (
                  <li key={pickup.id} data-testid={`stored-wine-manual-pickup-row-${pickup.id}`}>
                    {formatBusinessStatus(pickup.status)} · {MANUAL_PICKUP_CONDITION_LABELS[pickup.historyCondition ?? ""] ?? "-"} · {TEXT.manualPickupReason} {pickup.reason ?? "-"} · 发起 {pickup.initiatedBy ?? "-"} {cnDate(pickup.initiatedAt)}
                    {pickup.reviewedBy ? ` · 复核 ${pickup.reviewedBy} ${cnDate(pickup.reviewedAt)}` : ""}
                    {pickup.marketingConfirmedBy ? ` · 营销确认 ${pickup.marketingConfirmedBy} ${cnDate(pickup.marketingConfirmedAt)}` : ""}
                    {pickup.dispensedBy ? ` · 放酒 ${pickup.dispensedBy} ${cnDate(pickup.dispensedAt)}` : ""}
                    {pickup.status === "initiated" ? (
                      <HighRiskConfirm
                        label={TEXT.manualPickupReview}
                        impact="复核人不得是发起人；复核通过后交该客户对应的营销人员确认。"
                        disabled={submitting}
                        onConfirm={() => void manualPickupAction("stored_wine_manual_pickup_reviewed", `manual-pickups/${pickup.id}/review`,
                          { idempotency_key: `admin-saved-bottle-manual-pickup-review-${pickup.id}-${Date.now()}` })}
                        permissionsState={effectivePermissions}
                        permissions={[STORED_WINE_MANUAL_PICKUP_REVIEW_PERMISSION]}
                        data-testid={`stored-wine-manual-pickup-review-${pickup.id}`}
                      />
                    ) : null}
                    {pickup.status === "reviewed" ? (
                      <HighRiskConfirm
                        label={TEXT.manualPickupConfirmMarketing}
                        impact="由该客户对应的营销人员确认身份与取酒申请；营销已离职的由其直接上级确认，无归属营销的由当班营销主管确认。"
                        disabled={submitting}
                        onConfirm={() => void manualPickupAction("stored_wine_manual_pickup_marketing_confirmed", `manual-pickups/${pickup.id}/confirm-marketing`,
                          { idempotency_key: `admin-saved-bottle-manual-pickup-confirm-marketing-${pickup.id}-${Date.now()}` })}
                        permissionsState={effectivePermissions}
                        permissions={[STORED_WINE_MANUAL_PICKUP_CONFIRM_MARKETING_PERMISSION]}
                        data-testid={`stored-wine-manual-pickup-confirm-marketing-${pickup.id}`}
                      />
                    ) : null}
                    {pickup.status === "marketing_confirmed" ? (
                      <>
                        <input aria-label={`${TEXT.manualPickupQuantity} ${pickup.id}`} data-testid={`stored-wine-manual-pickup-quantity-${pickup.id}`} type="number" min="1" value={manualPickupQuantity} onChange={(event) => setManualPickupQuantity(event.target.value)} />
                        <label><input type="checkbox" data-testid={`stored-wine-manual-pickup-in-person-${pickup.id}`} checked={manualPickupInPerson} onChange={(event) => setManualPickupInPerson(event.target.checked)} />{TEXT.manualPickupInPerson}</label>
                        <HighRiskConfirm
                          label={TEXT.manualPickupDispense}
                          impact="只允许存酒客户本人到场办理，不允许他人代领；放酒后扣减剩余量并留取酒流水。"
                          disabled={submitting || !manualPickupInPerson}
                          precheck={validateManualPickupQuantity}
                          onConfirm={() => dispenseManualPickup(pickup.id)}
                          permissionsState={effectivePermissions}
                          permissions={[STORED_WINE_CLAIM_PERMISSION]}
                          data-testid={`stored-wine-manual-pickup-dispense-${pickup.id}`}
                        />
                      </>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
            {manualPickupError ? <div className="admin-inline-error" data-testid="stored-wine-manual-pickup-error">{manualPickupError}</div> : null}
          </div>
        ) : null}

        {selected && ["stored", "pending_identity_review"].includes(selected.status ?? "") ? (
          <>
            <div className="admin-form-grid" data-testid="stored-wine-dispute-form">
              <label>{TEXT.disputeReason}<input data-testid="stored-wine-dispute-reason" value={disputeReason} onChange={(event) => setDisputeReason(event.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <HighRiskConfirm
                label={TEXT.dispute}
                impact={TEXT.disputeBoundary}
                disabled={submitting}
                precheck={validateDispute}
                onConfirm={disputeStoredWine}
                permissionsState={effectivePermissions}
                permissions={[STORED_WINE_CLAIM_PERMISSION]}
                data-testid="stored-wine-dispute"
              />
              <span className="admin-muted-action" data-testid="stored-wine-dispute-boundary">{TEXT.disputeBoundary}</span>
            </div>
          </>
        ) : null}
        {selected?.status === "disputed" ? (
          <div className="admin-form-row">
            <HighRiskConfirm
              label={TEXT.disputeDismiss}
              impact={TEXT.disputeDismissBoundary}
              disabled={submitting}
              onConfirm={dismissDispute}
              permissionsState={effectivePermissions}
              permissions={[STORED_WINE_REMEDY_SUPERVISOR_PERMISSION]}
              data-testid="stored-wine-dispute-dismiss"
            />
            <span className="admin-muted-action" data-testid="stored-wine-dispute-dismiss-boundary">{TEXT.disputeDismissBoundary}</span>
          </div>
        ) : null}
        {disputeError ? <div data-testid="stored-wine-dispute-error" className="admin-inline-error">{disputeError}</div> : null}

        <div className="admin-form-grid" data-testid="stored-wine-cancel-form">
          <label>{TEXT.cancelReason}<input data-testid="stored-wine-cancel-reason" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label>
          <StoredWineEvidenceUpload api={api} label={TEXT.cancelPhotos} businessType="saved_bottle_cancel" attachmentRole="photo" accept="image/*"
            files={cancelPhotos} onChange={setCancelPhotos} testId="stored-wine-cancel-photos" />
          <StoredWineEvidenceUpload api={api} label={TEXT.cancelVideos} businessType="saved_bottle_cancel" attachmentRole="video" accept="video/*"
            files={cancelVideos} onChange={setCancelVideos} testId="stored-wine-cancel-videos" />
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.cancel}
            impact={TEXT.cancelBoundary}
            disabled={submitting}
            precheck={validateCancel}
            onConfirm={() => void cancelStoredWine()}
            permissionsState={effectivePermissions}
            permissions={selected?.status === "disputed"
              ? [STORED_WINE_CLAIM_PERMISSION, STORED_WINE_REMEDY_SUPERVISOR_PERMISSION]
              : [STORED_WINE_CLAIM_PERMISSION]}
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
                  <StoredWineEvidenceUpload api={api} label={TEXT.remedyPhotos} businessType="saved_bottle_registration" attachmentRole="photo" accept="image/*"
                    files={remedyPhotos} onChange={setRemedyPhotos} testId="stored-wine-remedy-photos" />
                  <label>{TEXT.remedyLabel}<input data-testid="stored-wine-remedy-label" value={remedyLabel} onChange={(event) => setRemedyLabel(event.target.value)} /></label>
                  <label>{TEXT.remedyLocation}<input data-testid="stored-wine-remedy-location" value={remedyLocation} onChange={(event) => setRemedyLocation(event.target.value)} /></label>
                  <label>{TEXT.remedyNote}<input data-testid="stored-wine-remedy-note" value={remedyNote} onChange={(event) => setRemedyNote(event.target.value)} /></label>
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
                  <StoredWineEvidenceUpload api={api} label={TEXT.remedyEvidence} businessType="saved_bottle_remedy" attachmentRole="other" accept="image/*,video/*"
                    files={remedyEvidence} onChange={setRemedyEvidence} testId="stored-wine-remedy-evidence" />
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
                      {remedy.customerDeliveryStatus === "approved" ? (<>
                        {/* 第十八批 AM 段第 241 条 241-R6（BSM:1803；第十八批裁决 55、65「存酒对客赔付」、68）：赔款交付结案前须有已通过的付款确认。 */}
                        <PaymentConfirmationPanel api={api} effectivePermissions={effectivePermissions} setActionResult={setActionResult} objectType="saved_bottle_remedy"
                          objectId={remedy.id} createPath={`/api/v1/inventory/saved-bottles/remedies/${encodeURIComponent(remedy.id)}/payment-confirmations`}
                          permission={STORED_WINE_CLAIM_PERMISSION} />
                        <HighRiskConfirm label={TEXT.remedySettle} impact="赔款实际交付后才结案；记交付/支付凭证与时间并计入经营损失。" disabled={submitting}
                          onConfirm={() => void remedyAction(remedy.id, "settle", { evidence_ids: remedyEvidence.map((f) => f.id) })}
                          permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                          data-testid={`stored-wine-remedy-settle-${remedy.id}`} />
                      </>) : null}
                      {(remedy.recoveryPartyType ?? "none") === "none" ? (
                        <>
                          <HighRiskConfirm label={TEXT.remedyAdvance} impact="娱乐平台公司审批先垫付，对客结案后向原门店挂追收应收。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "advance", {})}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                            data-testid={`stored-wine-remedy-advance-${remedy.id}`} />
                          <HighRiskConfirm label={TEXT.remedyStaff} impact="复核认定责任员工照价赔偿；金额由系统按内部结算价计，不改对客状态。" disabled={submitting}
                            onConfirm={() => void remedyAction(remedy.id, "staff-liability", { staff_user_id: remedyStaffUser.trim(), evidence_ids: remedyEvidence.map((f) => f.id) })}
                            permissionsState={effectivePermissions} permissions={[STORED_WINE_CLAIM_PERMISSION]}
                            data-testid={`stored-wine-remedy-staff-${remedy.id}`} />
                        </>
                      ) : null}
                      {remedy.recoveryPartyType === "staff" && remedy.recoveryStatus === "pending" ? (
                        <HighRiskConfirm label={TEXT.remedyRecoverySettle} impact="登记员工赔偿已收回（不改对客状态）。" disabled={submitting}
                          onConfirm={() => void remedyAction(remedy.id, "recovery/settle", { evidence_ids: remedyEvidence.map((f) => f.id) })}
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
          <StoredWineEvidenceUpload api={api} label={TEXT.handoverPhotos} businessType="saved_bottle_handover" attachmentRole="photo" accept="image/*"
            files={handoverPhotos} onChange={setHandoverPhotos} testId="stored-wine-handover-photos" />
          <StoredWineEvidenceUpload api={api} label={TEXT.handoverVideos} businessType="saved_bottle_handover" attachmentRole="video" accept="video/*"
            files={handoverVideos} onChange={setHandoverVideos} testId="stored-wine-handover-videos" />
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
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此存酒 ${row.id}`} data-testid={`stored-wine-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => { setSelectedId(row.id); setCancelPhotos([]); setCancelVideos([]); setRemedyPhotos([]); setRemedyEvidence([]); }} /></td>
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
