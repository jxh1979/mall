// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { cnDate } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { useAdminAction } from "../hooks/useAdminAction";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { PermissionButton } from "../security/Can";
import type { PagedResult } from "../client/adminClient";
import type { AdminPageProps, BusinessEntityRow } from "../types";

// 税费申报登记（ADM-FIN-010；第十八批 AC 段第 210 条 210-R2，第十八批裁决 42）：财务按公司、税种、所属期手工登记「实际申报金额」，
// 经财务、税务、法务审核并由顶层平台公司老板最终确认后生效（BSM:1760）；分红扣本期已生效的申报金额。登记即提交审批，驳回后可重新登记。
type Declaration = {
  id: string;
  businessEntityId: string;
  entityName?: string | null;
  taxType: string;
  periodStartMonth: string;
  periodEndMonth: string;
  declaredAmountCent: number;
  status: string;
  reviewId?: string | null;
  createdBy?: string;
  createdAt?: string;
};

const MANAGE = ["finance.entity_tax.manage"];
const PAGE_SIZE = 20;
const TAX_LABELS: Record<string, string> = { vat: "增值税", cultural_fee: "文化事业建设费", surcharge: "附加税", corporate_income: "企业所得税", other: "其他税费" };
const STATUS_LABELS: Record<string, string> = { pending_review: "待财务、税务、法务审核与最高确认", approved: "已生效", rejected: "已驳回（可重新登记）" };
const LINES = { finance_review: "财务最终审批人", tax_review: "税务最终审批人", legal_review: "法务最终审批人" } as const;
type LineCode = keyof typeof LINES;
const LINE_CODES = Object.keys(LINES) as LineCode[];
const EMPTY_FORM = { businessEntityId: "", taxType: "vat", periodStartMonth: "", periodEndMonth: "", amountYuan: "", reason: "" };
const EMPTY_REVIEWERS: Record<LineCode, string> = { finance_review: "", tax_review: "", legal_review: "" };

function yuan(cent: number): string {
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100);
}

export function EntityTaxDeclarationsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [filter, setFilter] = React.useState({ businessEntityId: "", month: "", taxType: "", status: "" });
  const [open, setOpen] = React.useState(false);
  const [form, setForm] = React.useState(EMPTY_FORM);
  const [reviewers, setReviewers] = React.useState(EMPTY_REVIEWERS);
  const [requestKey, setRequestKey] = React.useState("");

  // 经营主体下拉（读不到不阻断列表：列表行自带主体名）。
  React.useEffect(() => {
    api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities")
      .then((rows) => setEntities(Array.isArray(rows) ? rows : []))
      .catch(() => setEntities([]));
  }, [api]);

  const fetchPage = React.useCallback((page: number, size: number): Promise<PagedResult<Declaration>> => {
    const params = new URLSearchParams({ page: String(page), page_size: String(size) });
    if (filter.businessEntityId) params.set("business_entity_id", filter.businessEntityId);
    if (filter.month) params.set("month", filter.month);
    if (filter.taxType) params.set("tax_type", filter.taxType);
    if (filter.status) params.set("status", filter.status);
    return api.getPaged<Declaration>(`/api/v1/finance/entity-taxes?${params.toString()}`);
  }, [api, filter]);
  const rows = useServerPagedRows<Declaration>(fetchPage, PAGE_SIZE, { onError: setGlobalError, fallbackMessage: "税费申报登记读取失败" });
  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: async () => rows.reload(),
    setGlobalError,
    setActionResult,
    makeResult: (action, request, result) => ({ action, idempotencyKey: request.idempotencyKey, result }),
    fallbackMessage: "税费申报登记失败",
  });

  function openCreate() {
    setFormError("");
    setForm({ ...EMPTY_FORM, businessEntityId: filter.businessEntityId });
    setReviewers(EMPTY_REVIEWERS);
    setRequestKey(`entity-tax-${Date.now()}`);
    setOpen(true);
  }

  function validateDeclaration(): string | null {
    if (!form.businessEntityId) return "请选择经营主体。";
    if (!form.periodStartMonth || !form.periodEndMonth) return "请填写所属期首月和末月（按季申报的填整季）。";
    if (form.periodEndMonth < form.periodStartMonth) return "所属期末月不能早于首月。";
    const amount = Number(form.amountYuan);
    if (!form.amountYuan.trim() || !Number.isFinite(amount) || amount < 0) return "请按申报表填写实际申报金额（元），不能为负数。";
    if (!form.reason.trim()) return "请填写登记原因（如按哪一期申报表登记）。";
    return null;
  }

  function validateReviewers(): string | null {
    return LINE_CODES.some((code) => !reviewers[code].trim()) ? "请填写财务、税务、法务三条线的最终审批人。" : null;
  }

  async function submit() {
    const invalid = validateDeclaration() ?? validateReviewers();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = {
      idempotencyKey: requestKey,
      businessEntityId: form.businessEntityId,
      taxType: form.taxType,
      periodStartMonth: form.periodStartMonth,
      periodEndMonth: form.periodEndMonth,
      declaredAmountCent: Math.round(Number(form.amountYuan) * 100),
      reason: form.reason.trim(),
      parallelLines: LINE_CODES.map((lineCode) => ({ lineCode, finalApproverId: reviewers[lineCode].trim() })),
    };
    if (await runAction("entity_tax_declared", request, () => api.post("/api/v1/finance/entity-taxes", request))) setOpen(false);
  }

  const entityName = (id: string) => entities.find((e) => e.id === id)?.entityName ?? id;

  return (
    <section data-testid="entity-tax-page">
      <AdminPageHeader title="税费申报登记" description="按经营主体、税种、所属期登记每笔税的实际申报金额（按季申报的整季登记一条）；登记后经财务、税务、法务审核，再由顶层平台公司最终确认才生效，提交人不能审批自己的登记。持股分红扣本期已生效的申报金额。" />
      <div className="admin-form-row" data-testid="entity-tax-filter">
        <label>经营主体<select data-testid="entity-tax-filter-entity" value={filter.businessEntityId} onChange={(event) => setFilter({ ...filter, businessEntityId: event.target.value })}>
          <option value="">全部主体</option>
          {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
        </select></label>
        <label>所属月<input data-testid="entity-tax-filter-month" type="month" value={filter.month} onChange={(event) => setFilter({ ...filter, month: event.target.value })} /></label>
        <label>税种<select data-testid="entity-tax-filter-type" value={filter.taxType} onChange={(event) => setFilter({ ...filter, taxType: event.target.value })}>
          <option value="">全部税种</option>
          {Object.entries(TAX_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>状态<select data-testid="entity-tax-filter-status" value={filter.status} onChange={(event) => setFilter({ ...filter, status: event.target.value })}>
          <option value="">全部状态</option>
          {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="entity-tax-create-open" onClick={openCreate}>＋ 登记税费申报</PermissionButton>
      </div>
      {rows.error ? <div className="admin-form-error">{rows.error.message}</div> : null}
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="entity-tax-table">
          <thead><tr><th>经营主体</th><th>税种</th><th>所属期</th><th>实际申报金额（元）</th><th>状态</th><th>登记人</th><th>登记时间</th><th>审批</th></tr></thead>
          <tbody>
            {rows.rows.length === 0 ? (
              <tr data-testid="entity-tax-table-empty"><td colSpan={8} className="admin-muted-action">{rows.loading ? "读取中…" : "当前筛选下暂无税费申报登记。"}</td></tr>
            ) : rows.rows.map((row) => (
              <tr key={row.id} data-testid={`entity-tax-row-${row.id}`}>
                <td>{row.entityName ?? entityName(row.businessEntityId)}</td>
                <td>{TAX_LABELS[row.taxType] ?? row.taxType}</td>
                <td>{row.periodStartMonth}～{row.periodEndMonth}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{yuan(row.declaredAmountCent)}</td>
                <td>{STATUS_LABELS[row.status] ?? row.status}</td>
                <td>{row.createdBy ?? "—"}</td>
                <td>{cnDate(row.createdAt)}</td>
                <td>{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a> : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <TablePager testid="entity-tax-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
      </div>

      <GuidedFlowModal
        open={open}
        title="登记税费实际申报金额"
        testId="entity-tax-create-guided"
        submitTestId="entity-tax-create-save"
        submitLabel="登记并提交审批"
        submitting={busyAction === "entity_tax_declared"}
        error={formError || undefined}
        onClose={() => setOpen(false)}
        onSubmit={() => void submit()}
        steps={[
          {
            title: "申报信息",
            description: "按税务申报表填写：所属期按月，按季申报的整季登记一条（首月～末月）。同一主体、税种、所属期在待审批或已生效时不能重复登记。",
            validate: validateDeclaration,
            content: (
              <>
                <label>经营主体<select data-testid="entity-tax-create-entity" value={form.businessEntityId} onChange={(event) => setForm({ ...form, businessEntityId: event.target.value })}>
                  <option value="">请选择</option>
                  {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
                </select></label>
                <label>税种<select data-testid="entity-tax-create-type" value={form.taxType} onChange={(event) => setForm({ ...form, taxType: event.target.value })}>
                  {Object.entries(TAX_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>所属期首月<input data-testid="entity-tax-create-start" type="month" value={form.periodStartMonth} onChange={(event) => setForm({ ...form, periodStartMonth: event.target.value })} /></label>
                <label>所属期末月<input data-testid="entity-tax-create-end" type="month" value={form.periodEndMonth} onChange={(event) => setForm({ ...form, periodEndMonth: event.target.value })} /></label>
                <label>实际申报金额（元）<input data-testid="entity-tax-create-amount" type="number" step="0.01" min="0" value={form.amountYuan} onChange={(event) => setForm({ ...form, amountYuan: event.target.value })} /></label>
                <label>登记原因<textarea data-testid="entity-tax-create-reason" value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} placeholder="如：2026 年一季度增值税申报表" /></label>
              </>
            ),
          },
          {
            title: "审批线",
            description: "财务、税务、法务三条线各填最终审批人（员工账号编号，须在该线起点人的直接上级链上）；三线通过后由顶层平台公司最终确认。",
            validate: validateReviewers,
            content: (
              <>
                {LINE_CODES.map((code) => (
                  <label key={code}>{LINES[code]}<input data-testid={`entity-tax-create-${code}`} value={reviewers[code]} placeholder="员工账号编号" onChange={(event) => setReviewers({ ...reviewers, [code]: event.target.value })} /></label>
                ))}
              </>
            ),
          },
        ]}
        summary={[
          { label: "经营主体", value: form.businessEntityId ? entityName(form.businessEntityId) : "未选择" },
          { label: "税种", value: TAX_LABELS[form.taxType] ?? form.taxType },
          { label: "所属期", value: `${form.periodStartMonth || "—"}～${form.periodEndMonth || "—"}` },
          { label: "实际申报金额", value: form.amountYuan ? `¥${form.amountYuan}` : "未填写" },
          { label: "登记原因", value: form.reason.trim() || "未填写" },
          { label: "生效条件", value: "财务、税务、法务全部通过后，由顶层平台公司最终确认" },
        ]}
      />
    </section>
  );
}
