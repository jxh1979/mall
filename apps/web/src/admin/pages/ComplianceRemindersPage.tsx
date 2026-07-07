import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { Tabs } from "antd";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField, cnDate } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminPageProps } from "../types";

// 合规到期台账域内枚举中文（全局状态表未含的日期类型/来源/严重度，页内映射；状态码走全局表兜底）。
const DATE_TYPE_LABELS: Record<string, string> = {
  rent_review: "租金调整", renewal: "续约", expiry: "到期", payment: "付款", inspection: "复检", other: "其它",
};
const ITEM_TYPE_LABELS: Record<string, string> = {
  business_license: "营业执照", food_permit: "食品经营许可", fire_permit: "消防许可",
  special_equipment: "特种设备", hygiene_permit: "卫生许可", other: "其它",
};
const SEVERITY_LABELS: Record<string, string> = { high: "高", medium: "中", low: "低" };
const SOURCE_LABELS: Record<string, string> = { contract: "合同派生", license: "证照派生", manual: "人工录入", system: "系统生成" };
const cLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? code : "-");
const yn = (v: boolean | undefined) => (v ? "是" : "否");

// 后端 compliance 域记录映射（接现有 /api/v1/compliance/* 契约，客户端已 snake→camel）。
type ContractKeyDate = { id: string; contractId?: string; merchantId?: string; dateType?: string; title?: string; dueDate?: string; status?: string; ownerUserId?: string; sourceType?: string; humanConfirmed?: boolean };
type ExpiryItem = { id: string; subjectType?: string; subjectId?: string; merchantId?: string; itemType?: string; itemName?: string; documentNo?: string; issuingAuthority?: string; validFrom?: string; validTo?: string; expiryAction?: string; status?: string; sourceType?: string; humanConfirmed?: boolean };
type Reminder = { id: string; sourceType?: string; sourceId?: string; reminderType?: string; dueDate?: string; reminderAt?: string; severity?: string; status?: string; blockingSuggested?: boolean; message?: string };

type ComplianceData = { reminders: Reminder[]; keyDates: ContractKeyDate[]; expiryItems: ExpiryItem[] };

const KEY_DATE_MANAGE = ["compliance.contract_key_date.manage"];
const EXPIRY_MANAGE = ["compliance.expiry_item.manage"];
const REMINDER_SCAN = ["compliance.reminder.scan"];

const complianceApiPaths = [
  "/api/v1/compliance/reminders",
  "/api/v1/compliance/contract-key-dates",
  "/api/v1/compliance/expiry-items",
];

/** PageResponse / 裸数组 双兼容取行。 */
function asRows<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  const items = (value as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/**
 * ADM-COMP-001 合规到期提醒台账：把合同关键日期、合规证照到期、到期提醒三类合规到期数据聚合成一个台账页。
 * 「扫描生成提醒」= 依合同关键日期 + 证照到期滚动生成到期提醒（幂等，重复扫描不重复生成）。
 * 三张表整行可点 → 行明细抽屉（日期/枚举/严重度中文化溯源）；两处人工补录（合同关键日期 / 证照到期项）。
 * 某读接口权限不足（如只有 reminder.view 无 key_date.view）时 allSettled 局部降级，不整页崩。
 */
export function ComplianceRemindersPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<ComplianceData | null>(null);
  const [scanForm, setScanForm] = React.useState({ scanDate: "", lookaheadDays: "30", operatorNote: "" });
  const [keyDateForm, setKeyDateForm] = React.useState({ contractId: "", title: "", dateType: "renewal", dueDate: "" });
  const [expiryForm, setExpiryForm] = React.useState({ itemType: "business_license", itemName: "", documentNo: "", validTo: "" });
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-4：主创建流=合同关键日期补录，收起为主按钮 + 分步引导弹窗（扫描、证照到期补录不变）。
  const [keyDateOpen, setKeyDateOpen] = React.useState(false);
  const [keyDateError, setKeyDateError] = React.useState<string | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const settled = await Promise.allSettled(complianceApiPaths.map((path) => api.get<unknown>(path)));
      const at = (index: number): unknown => {
        const outcome = settled[index];
        return outcome && outcome.status === "fulfilled" ? outcome.value : [];
      };
      setData({
        reminders: asRows<Reminder>(at(0)),
        keyDates: asRows<ContractKeyDate>(at(1)),
        expiryItems: asRows<ExpiryItem>(at(2)),
      });
    },
    { onError: setGlobalError, fallbackMessage: "合规到期台账读取失败", onStart: () => setGlobalError(null) },
  );

  const scanReminders = async () => {
    try {
      const result = await api.post<{ generatedCount?: number }>("/api/v1/compliance/reminders/scan", {
        idempotency_key: `crs-${Date.now()}`,
        scan_date: scanForm.scanDate || null,
        default_lookahead_days: Number(scanForm.lookaheadDays) || 30,
        operator_note: scanForm.operatorNote || null,
      });
      setActionResult({ action: "compliance_reminder_scanned", generatedCount: result?.generatedCount ?? 0 });
      reload();
    } catch (e) { setGlobalError((e as Error).message); }
  };

  const createKeyDate = async () => {
    setKeyDateError(null);
    if (!keyDateForm.contractId.trim() || !keyDateForm.title.trim()) {
      setKeyDateError("关联合同、标题必填");
      return;
    }
    try {
      await api.post("/api/v1/compliance/contract-key-dates", {
        idempotency_key: `ckd-${Date.now()}`, contract_id: keyDateForm.contractId, title: keyDateForm.title,
        date_type: keyDateForm.dateType, due_date: keyDateForm.dueDate || null, source_type: "manual", human_confirmed: true,
      });
      setActionResult({ action: "compliance_key_date_created", title: keyDateForm.title });
      setKeyDateOpen(false);
      reload();
    } catch (e) {
      const message = (e as Error).message;
      setKeyDateError(message);
      setGlobalError(message);
    }
  };

  const createExpiryItem = async () => {
    try {
      await api.post("/api/v1/compliance/expiry-items", {
        idempotency_key: `cei-${Date.now()}`, subject_type: "merchant", item_type: expiryForm.itemType,
        item_name: expiryForm.itemName, document_no: expiryForm.documentNo, valid_to: expiryForm.validTo || null,
        source_type: "manual", human_confirmed: true,
      });
      setActionResult({ action: "compliance_expiry_item_created", itemName: expiryForm.itemName });
      reload();
    } catch (e) { setGlobalError((e as Error).message); }
  };

  function openReminderDetail(row: Reminder) {
    setDetail({
      title: `到期提醒 · ${cLabel(DATE_TYPE_LABELS, row.reminderType)}`,
      fields: [
        { label: "提醒类型", value: cLabel(DATE_TYPE_LABELS, row.reminderType) },
        { label: "来源", value: `${cLabel(SOURCE_LABELS, row.sourceType)}：${row.sourceId ?? "—"}` },
        { label: "到期日", value: cnDate(row.dueDate) },
        { label: "提醒日", value: cnDate(row.reminderAt) },
        { label: "严重度", value: cLabel(SEVERITY_LABELS, row.severity) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "建议阻断", value: yn(row.blockingSuggested) },
        { label: "说明", value: row.message ?? "—" },
      ],
    });
  }

  function openKeyDateDetail(row: ContractKeyDate) {
    setDetail({
      title: `合同关键日期 · ${row.title ?? row.id}`,
      fields: [
        { label: "标题", value: row.title ?? "—" },
        { label: "日期类型", value: cLabel(DATE_TYPE_LABELS, row.dateType) },
        { label: "关联合同", value: row.contractId ?? "—" },
        { label: "关联商户", value: row.merchantId ?? "—" },
        { label: "到期日", value: cnDate(row.dueDate) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "来源", value: cLabel(SOURCE_LABELS, row.sourceType) },
        { label: "已人工确认", value: yn(row.humanConfirmed) },
        { label: "关键日期编号", value: row.id },
      ],
    });
  }

  function openExpiryDetail(row: ExpiryItem) {
    setDetail({
      title: `证照到期 · ${row.itemName ?? row.id}`,
      fields: [
        { label: "证照名称", value: row.itemName ?? "—" },
        { label: "证照类型", value: cLabel(ITEM_TYPE_LABELS, row.itemType) },
        { label: "证照编号", value: row.documentNo ?? "—" },
        { label: "发证机关", value: row.issuingAuthority ?? "—" },
        { label: "关联商户", value: row.merchantId ?? "—" },
        { label: "有效期起", value: cnDate(row.validFrom) },
        { label: "有效期止", value: cnDate(row.validTo) },
        { label: "到期动作", value: formatBusinessStatus(row.expiryAction) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "已人工确认", value: yn(row.humanConfirmed) },
      ],
    });
  }

  const reminders = data?.reminders ?? [];
  const keyDates = data?.keyDates ?? [];
  const expiryItems = data?.expiryItems ?? [];
  const blockingCount = reminders.filter((r) => r.blockingSuggested).length;

  return (
    <section data-testid="compliance-reminders-page">
      <AdminPageHeader title="合规到期提醒台账" description="合规到期台账：汇总合同关键日期、合规证照到期与到期提醒。可依合同/证照滚动扫描生成到期提醒（幂等），并人工补录关键日期与证照。" />
      <AuditTrailHint event="compliance.reminder_scanned / compliance.key_date_created / compliance.expiry_item_created" />
      <TechDetails><div className="admin-generic-evidence" data-testid="compliance-reminders-evidence">/api/v1/compliance</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="compliance-reminders-summary">
          <div className="admin-kpi-card"><span>到期提醒</span><strong data-testid="compliance-reminder-count">{reminders.length}</strong><small>建议阻断 {blockingCount}</small></div>
          <div className="admin-kpi-card"><span>合同关键日期</span><strong>{keyDates.length}</strong></div>
          <div className="admin-kpi-card"><span>证照到期项</span><strong>{expiryItems.length}</strong></div>
        </div>

        <div className="admin-form-grid" data-testid="compliance-scan-form">
          <label>扫描基准日（留空=今日）<input type="date" data-testid="compliance-scan-date" value={scanForm.scanDate} onChange={(e) => setScanForm({ ...scanForm, scanDate: e.target.value })} /></label>
          <label>提前天数<input data-testid="compliance-scan-lookahead" value={scanForm.lookaheadDays} onChange={(e) => setScanForm({ ...scanForm, lookaheadDays: e.target.value })} /></label>
          <label>操作备注<input data-testid="compliance-scan-note" value={scanForm.operatorNote} onChange={(e) => setScanForm({ ...scanForm, operatorNote: e.target.value })} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={REMINDER_SCAN} data-testid="compliance-scan" onClick={() => void scanReminders()}>扫描生成提醒</PermissionButton>
        </div>

        <div className="admin-preview" data-testid="compliance-reminders-guide">
          <strong>这一页做什么：</strong>合规到期三块——① 到期提醒（扫描生成、可建议阻断）② 合同关键日期 ③ 合规证照到期。上方设扫描基准日与提前天数后切标签查看。
        </div>
        <Tabs
          data-testid="compliance-reminders-tabs"
          items={[
            { key: "reminders", label: "① 到期提醒", forceRender: true, children: (<>
        <h3>到期提醒</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="compliance-reminders-table">
            <thead><tr><th>提醒类型</th><th>到期日</th><th>提醒日</th><th>严重度</th><th>建议阻断</th><th>状态</th></tr></thead>
            <tbody>
              {reminders.slice(0, 100).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`compliance-reminder-row-${row.id}`}
                  onClick={() => openReminderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openReminderDetail(row); } }}>
                  <td>{cLabel(DATE_TYPE_LABELS, row.reminderType)}</td><td>{cnDate(row.dueDate)}</td><td>{cnDate(row.reminderAt)}</td>
                  <td>{cLabel(SEVERITY_LABELS, row.severity)}</td><td>{yn(row.blockingSuggested)}</td><td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

          </>) },
          { key: "contract-dates", label: "② 合同关键日期", forceRender: true, children: (<>
        <h3>合同关键日期</h3>
        <div className="admin-form-row" data-testid="compliance-key-date-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={KEY_DATE_MANAGE} data-testid="compliance-key-date-create-open" onClick={() => { setKeyDateError(null); setKeyDateOpen(true); }}>＋ 补录关键日期</PermissionButton>
        </div>
        <GuidedFlowModal
          open={keyDateOpen}
          title="补录合同关键日期"
          testId="compliance-key-date-guided"
          submitTestId="compliance-key-date-create"
          submitLabel="补录关键日期"
          error={keyDateError ?? undefined}
          onClose={() => setKeyDateOpen(false)}
          onSubmit={() => void createKeyDate()}
          steps={[
            {
              title: "关键日期信息",
              description: "填写关联合同、标题、日期类型与到期日。",
              validate: () => (keyDateForm.contractId.trim() && keyDateForm.title.trim() ? null : "关联合同、标题必填"),
              content: (
                <>
                  <label>关联合同<input data-testid="compliance-key-date-contract" value={keyDateForm.contractId} onChange={(e) => setKeyDateForm({ ...keyDateForm, contractId: e.target.value })} /></label>
                  <label>标题<input data-testid="compliance-key-date-title" value={keyDateForm.title} onChange={(e) => setKeyDateForm({ ...keyDateForm, title: e.target.value })} /></label>
                  <label>日期类型
                    <select data-testid="compliance-key-date-type" value={keyDateForm.dateType} onChange={(e) => setKeyDateForm({ ...keyDateForm, dateType: e.target.value })}>
                      {Object.entries(DATE_TYPE_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                    </select>
                  </label>
                  <label>到期日<input type="date" data-testid="compliance-key-date-due" value={keyDateForm.dueDate} onChange={(e) => setKeyDateForm({ ...keyDateForm, dueDate: e.target.value })} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "关联合同", value: keyDateForm.contractId.trim() || "未填写" },
            { label: "标题", value: keyDateForm.title.trim() || "未填写" },
            { label: "日期类型", value: DATE_TYPE_LABELS[keyDateForm.dateType] ?? keyDateForm.dateType },
            { label: "到期日", value: keyDateForm.dueDate || "未填写" },
          ]}
        />
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="compliance-key-dates-table">
            <thead><tr><th>标题</th><th>日期类型</th><th>关联合同</th><th>到期日</th><th>来源</th><th>状态</th></tr></thead>
            <tbody>
              {keyDates.slice(0, 100).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`compliance-key-date-row-${row.id}`}
                  onClick={() => openKeyDateDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openKeyDateDetail(row); } }}>
                  <td>{row.title ?? "-"}</td><td>{cLabel(DATE_TYPE_LABELS, row.dateType)}</td><td>{row.contractId ?? "-"}</td>
                  <td>{cnDate(row.dueDate)}</td><td>{cLabel(SOURCE_LABELS, row.sourceType)}</td><td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

          </>) },
          { key: "licenses", label: "③ 合规证照到期", forceRender: true, children: (<>
        <h3>合规证照到期</h3>
        <div className="admin-form-grid" data-testid="compliance-expiry-form">
          <label>证照类型
            <select data-testid="compliance-expiry-type" value={expiryForm.itemType} onChange={(e) => setExpiryForm({ ...expiryForm, itemType: e.target.value })}>
              {Object.entries(ITEM_TYPE_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label>证照名称<input data-testid="compliance-expiry-name" value={expiryForm.itemName} onChange={(e) => setExpiryForm({ ...expiryForm, itemName: e.target.value })} /></label>
          <label>证照编号<input data-testid="compliance-expiry-doc" value={expiryForm.documentNo} onChange={(e) => setExpiryForm({ ...expiryForm, documentNo: e.target.value })} /></label>
          <label>有效期止<input type="date" data-testid="compliance-expiry-valid-to" value={expiryForm.validTo} onChange={(e) => setExpiryForm({ ...expiryForm, validTo: e.target.value })} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={EXPIRY_MANAGE} data-testid="compliance-expiry-create" onClick={() => void createExpiryItem()}>补录证照到期</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="compliance-expiry-items-table">
            <thead><tr><th>证照名称</th><th>类型</th><th>编号</th><th>有效期止</th><th>到期动作</th><th>状态</th></tr></thead>
            <tbody>
              {expiryItems.slice(0, 100).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`compliance-expiry-row-${row.id}`}
                  onClick={() => openExpiryDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openExpiryDetail(row); } }}>
                  <td>{row.itemName ?? "-"}</td><td>{cLabel(ITEM_TYPE_LABELS, row.itemType)}</td><td>{row.documentNo ?? "-"}</td>
                  <td>{cnDate(row.validTo)}</td><td>{formatBusinessStatus(row.expiryAction)}</td><td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
          </>) },
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
