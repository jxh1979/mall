// 文件版本：V0.05
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type Kind = "points" | "growth" | "national" | "sales_contract" | "recovery_waiver" | "merchant_points" | "merchant_growth" | "level_rolling_months" | "level_protection_months" | "level_notice_days" | "merchant_card_bonus" | "stored_value_bonus";
type Row = { id: string; kind: Kind; version: string; status: string; ticketTypeId?: string; businessFormat?: string; merchantId?: string; effectiveAt: string; publishedAt?: string; reviewId?: string; values: Record<string, unknown> };
type Option = { id: string; name?: string; entityName?: string; ticketName?: string; barId?: string };
const kinds: Record<Kind, string> = { points: "门票固定积分", growth: "门票成长值", national: "全国积分等待与发行单价", sales_contract: "合同销售主体", recovery_waiver: "全国待追回积分豁免线", merchant_points: "商户业态积分获取率", merchant_growth: "商户业态成长值规则", level_rolling_months: "会员等级滚动统计周期（月）", level_protection_months: "会员等级保护期（月）", level_notice_days: "降级提前通知天数（天）", merchant_card_bonus: "商户级卡充值赠送比例（商户提出、平台代录）", stored_value_bonus: "综合体通用卡充值赠送比例" };
// OBS-13-01（BSM L820「业务参数，全平台统一可配……须同时配置上下限」「变更须经审批、只对后续周期生效」）：三项会员等级参数全国生效，无公告要求。
const levelKind = (k: Kind) => k === "level_rolling_months" || k === "level_protection_months" || k === "level_notice_days";
// 第十八批第 11 条（ENUM L86「商户级卡同样可以设赠送……赠送比例由发卡商户提出、由平台运营代为录入后台」「赠送比例可配（业务参数，按项目可配，须同时配置上下限）」；
// BSM L521 涉钱配置版本化审批）：商户级卡按发卡商户、通用卡按综合体（可空＝无项目通用卡），无公告要求。
const bonusKind = (k: Kind) => k === "merchant_card_bonus" || k === "stored_value_bonus";
const bonusFields: Array<[string, string]> = [["fixedValue", "赠送比例（万分比）"], ["minimumValue", "赠送比例下限（万分比）"], ["maximumValue", "赠送比例上限（万分比）"]];
// 第十一批 TK-04-R1（BSM L822「按商户业态可配，须同时配置上下限」；操作员 2026-10-21 裁决补充第 4 条）：商户业态规则全国生效，按业态分别配置。
const formats: Record<string, string> = { bar: "酒吧", catering: "餐饮", retail: "零售", amusement: "游乐", media: "传媒", other: "其他" };
const labels: Record<string, string> = { pending_review: "待审批", approved: "已审批", rejected: "已驳回" };
const numberFields: Record<Kind, Array<[string, string]>> = {
  points: [["fixedValue", "每人份／次份固定积分"], ["minimumValue", "积分下限"], ["maximumValue", "积分上限"]],
  growth: [["fixedValue", "每人份／次份成长值"], ["minimumValue", "成长值下限"], ["maximumValue", "成长值上限"]],
  national: [["waitingHours", "核销后等待小时数"], ["minimumWaitingHours", "等待时长下限"], ["maximumWaitingHours", "等待时长上限"], ["issuancePriceCent", "每积分发行单价（分）"], ["minimumPriceCent", "单价下限（分）"], ["maximumPriceCent", "单价上限（分）"], ["merchantWaitingHours", "商户消费履约完成后等待小时数"], ["minimumMerchantWaitingHours", "商户消费等待下限"], ["maximumMerchantWaitingHours", "商户消费等待上限"]],
  sales_contract: [],
  recovery_waiver: [["fixedValue", "会员全部待追回积分豁免线"], ["minimumValue", "豁免线下限"], ["maximumValue", "豁免线上限"]],
  merchant_points: [["fixedValue", "每产生 1 积分所需消费（分）"], ["minimumValue", "积分获取率下限（分）"], ["maximumValue", "积分获取率上限（分）"]],
  merchant_growth: [["fixedValue", "每记 1 成长值所需消费（分）"], ["minimumValue", "成长值规则下限（分）"], ["maximumValue", "成长值规则上限（分）"]],
  level_rolling_months: [["fixedValue", "滚动统计周期（月）"], ["minimumValue", "滚动统计周期下限（月）"], ["maximumValue", "滚动统计周期上限（月）"]],
  level_protection_months: [["fixedValue", "保护期（月）"], ["minimumValue", "保护期下限（月）"], ["maximumValue", "保护期上限（月）"]],
  level_notice_days: [["fixedValue", "降级提前通知天数"], ["minimumValue", "提前通知天数下限"], ["maximumValue", "提前通知天数上限"]],
  merchant_card_bonus: bonusFields,
  stored_value_bonus: bonusFields,
};
const list = (value: Option[] | { items: Option[] }) => Array.isArray(value) ? value : value.items;

export function TicketRewardConfigPage({ api, effectivePermissions, setActionResult, setGlobalError }: AdminPageProps) {
  const [project, setProject] = React.useState("");
  const [kind, setKind] = React.useState<Kind>("points");
  const [ticket, setTicket] = React.useState("");
  const [values, setValues] = React.useState<Record<string, string>>({ waitingHours: "24" });
  const [format, setFormat] = React.useState("");
  const [merchant, setMerchant] = React.useState("");
  const [rows, setRows] = React.useState<Row[]>([]);
  const [projects, setProjects] = React.useState<Option[]>([]);
  const [tickets, setTickets] = React.useState<Option[]>([]);
  const [entities, setEntities] = React.useState<Option[]>([]);
  const [effective, setEffective] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [approver, setApprover] = React.useState("");
  const [reviewers, setReviewers] = React.useState<Record<string, string>>({ business_review: "", finance_review: "", legal_review: "" });
  const [attachments, setAttachments] = React.useState<string[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [publicationReason, setPublicationReason] = React.useState("");
  const manage = ["ticket.reward.config.manage"];
  const merchantKind = kind === "merchant_points" || kind === "merchant_growth";
  const merchantBonus = kind === "merchant_card_bonus";
  const generalBonus = kind === "stored_value_bonus";
  const nationwide = kind === "national" || kind === "recovery_waiver" || merchantKind || levelKind(kind) || merchantBonus;

  const load = React.useCallback(async () => {
    const qs = nationwide ? "" : project ? `?project_id=${encodeURIComponent(project)}` : generalBonus ? "" : null;
    if (qs === null) { setRows([]); return; }
    setRows(await api.get<Row[]>(`/api/v1/loyalty/ticket-reward-configs${qs}`));
  }, [api, project, nationwide, generalBonus]);
  React.useEffect(() => { void load().catch(e => setError(e.message)); }, [load]);
  React.useEffect(() => {
    void Promise.all([
      api.get<Option[] | { items: Option[] }>("/api/v1/platform/projects"),
      api.get<Option[] | { items: Option[] }>("/api/v1/ticketing/ticket-types?includeArchived=false"),
      api.get<Option[] | { items: Option[] }>("/api/v1/platform/business-entities"),
    ]).then(([p, t, e]) => { setProjects(list(p)); setTickets(list(t)); setEntities(list(e)); })
      .catch(e => setError(`项目、票种或主体列表读取失败：${e.message}`));
  }, [api]);
  async function action(run: () => Promise<unknown>) {
    if (busy) return; setBusy(true); setError("");
    try { const result = await run(); setActionResult(result); await load(); }
    catch (e) { const message = e instanceof Error ? e.message : "操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function submit() {
    const payload: Record<string, unknown> = {};
    for (const [key, label] of numberFields[kind]) {
      if (!values[key]?.trim() || !Number.isFinite(Number(values[key]))) throw new Error(`请填写${label}`);
      payload[key] = Number(values[key]);
    }
    if (!effective || !reason.trim() || !approver.trim()) throw new Error("请填写未来生效时间、原因和最终审批人");
    if (!nationwide && !generalBonus && (!project || !ticket)) throw new Error("请选择综合体和票种");
    if (merchantKind && !format) throw new Error("请选择商户业态");
    if (merchantBonus && !merchant.trim()) throw new Error("请填写发卡商户 id");
    if (kind === "national") payload.receiverEntityId = values.receiverEntityId;
    if (kind === "sales_contract") Object.assign(payload, { sellerEntityId: values.sellerEntityId, sellerMerchantId: tickets.find(t => t.id === ticket)?.barId ?? null, contractNumber: values.contractNumber, attachmentIds: attachments });
    return api.post("/api/v1/loyalty/ticket-reward-configs", {
      idempotencyKey: crypto.randomUUID(), kind, projectId: nationwide ? null : project || null,
      ticketTypeId: nationwide || generalBonus ? null : ticket, ...(merchantKind ? { businessFormat: format } : {}), ...(merchantBonus ? { merchantId: merchant.trim() } : {}), values: payload, effectiveAt: new Date(effective).toISOString(),
      reason, finalApproverId: approver.trim(),
      parallelLines: kind === "sales_contract" ? Object.entries(reviewers).map(([lineCode, finalApproverId]) => ({ lineCode, finalApproverId: finalApproverId.trim() })) : [],
    });
  }
  async function upload(files: FileList | null) {
    if (!files?.length) return;
    await action(async () => {
      if (!project || !ticket || !api.postForm) throw new Error("请先选择项目和票种后上传销售合同");
      const ids: string[] = [];
      for (const file of Array.from(files)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "ticket_sales_contract");
        form.append("business_id", `${project}:${ticket}`); form.append("attachment_role", "sales_contract");
        ids.push((await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form)).id);
      }
      setAttachments(old => [...old, ...ids]); return { 已上传合同份数: ids.length };
    });
  }
  const select = (key: string, title: string) => <label>{title}<select aria-label={title} value={values[key] ?? ""} onChange={e => setValues(v => ({ ...v, [key]: e.target.value }))}><option value="">请选择</option>{entities.map(e => <option key={e.id} value={e.id}>{e.entityName ?? e.name ?? e.id}</option>)}</select></label>;
  return <section data-testid="ticket-reward-config-page">
    <AdminPageHeader title="门票积分、成长值与销售责任" description="配置审批通过后须在生效前发布。积分和成长值分别设置；积分归实际使用会员，合同销售方独立承担发行费。" />
    {error && <p role="alert">{error}</p>}
    <div className="admin-form-row">
      <label>配置类型<select aria-label="配置类型" value={kind} onChange={e => { const next = e.target.value as Kind; setKind(next); setValues(next === "recovery_waiver" ? { fixedValue: "100" } : next === "national" ? { waitingHours: "24", merchantWaitingHours: "24" } : {}); }}>{Object.entries(kinds).map(([k, name]) => <option key={k} value={k}>{name}</option>)}</select></label>
      {merchantKind && <label>商户业态<select aria-label="商户业态" value={format} onChange={e => setFormat(e.target.value)}><option value="">请选择</option>{Object.entries(formats).map(([k, name]) => <option key={k} value={k}>{name}</option>)}</select></label>}
      {merchantBonus && <label>发卡商户 id<input aria-label="发卡商户 id" value={merchant} onChange={e => setMerchant(e.target.value)} /></label>}
      {!nationwide && <><label>综合体<select aria-label="综合体" value={project} onChange={e => { setProject(e.target.value); setAttachments([]); }}><option value="">{generalBonus ? "不限项目（无项目通用卡）" : "请选择"}</option>{projects.map(p => <option key={p.id} value={p.id}>{p.name ?? p.id}</option>)}</select></label>
        {!generalBonus && <label>票种<select aria-label="票种" value={ticket} onChange={e => { setTicket(e.target.value); setAttachments([]); }}><option value="">请选择</option>{tickets.map(t => <option key={t.id} value={t.id}>{t.ticketName ?? t.id}</option>)}</select></label>}</>}
    </div>
    <div className="admin-form-row">
      {numberFields[kind].map(([key, title]) => <label key={key}>{title}<input aria-label={title} type="number" min="0" step={key.includes("Price") ? "0.000001" : "1"} value={values[key] ?? ""} onChange={e => setValues(v => ({ ...v, [key]: e.target.value }))} /></label>)}
      {kind === "national" && select("receiverEntityId", "全国积分结算收款主体")}
      {kind === "sales_contract" && <>{select("sellerEntityId", "合同销售法律主体")}<label>合同编号<input aria-label="合同编号" value={values.contractNumber ?? ""} onChange={e => setValues(v => ({ ...v, contractNumber: e.target.value }))} /></label><label>销售合同凭证<input aria-label="销售合同凭证" type="file" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label><span>已上传 {attachments.length} 份合同凭证</span></>}
      <label>生效时间<input aria-label="生效时间" type="datetime-local" value={effective} onChange={e => setEffective(e.target.value)} /></label>
      <label>变更原因<input aria-label="变更原因" value={reason} onChange={e => setReason(e.target.value)} /></label>
      <label>最终审批人账号<input aria-label="最终审批人账号" value={approver} onChange={e => setApprover(e.target.value)} /></label>
      {kind === "sales_contract" && Object.entries({ business_review: "业务", finance_review: "财务", legal_review: "法务" }).map(([key, title]) => <label key={key}>{title}线最终审批人<input aria-label={`${title}线最终审批人`} value={reviewers[key]} onChange={e => setReviewers(v => ({ ...v, [key]: e.target.value }))} /></label>)}
      <PermissionButton permissionsState={effectivePermissions} permissions={manage} disabled={busy} onClick={() => void action(submit)}>提交审批</PermissionButton>
    </div>
    <label>公告说明<input aria-label="公告说明" value={publicationReason} onChange={e => setPublicationReason(e.target.value)} /></label>
    <table className="admin-table"><thead><tr><th>类型／版本</th><th>审批</th><th>生效时间</th><th>公告</th></tr></thead><tbody>
      {rows.filter(r => r.kind === kind).map(row => <tr key={row.id}><td>{kinds[row.kind]}{row.businessFormat ? `（${formats[row.businessFormat] ?? row.businessFormat}）` : ""}{row.merchantId ? `（发卡商户 ${row.merchantId}）` : ""} · {row.version}<details><summary>查看审批内容</summary>{numberFields[row.kind].map(([field, title]) => <p key={field}>{title}：{String(row.values[field] ?? "未填")}</p>)}{["sellerEntityId", "receiverEntityId"].map(field => row.values[field] ? <p key={field}>{field === "sellerEntityId" ? "合同销售方" : "积分结算收款方"}：{entities.find(entity => entity.id === row.values[field])?.entityName ?? String(row.values[field])}</p> : null)}{row.values.contractNumber ? <p>合同编号：{String(row.values.contractNumber)}</p> : null}{Array.isArray(row.values.attachmentIds) && row.values.attachmentIds.map((id, i) => <a key={String(id)} href={`/api/v1/evidence/attachments/${encodeURIComponent(String(id))}/file`} target="_blank" rel="noreferrer">合同凭证 {i + 1}</a>)}</details></td><td>{labels[row.status] ?? row.status}{row.reviewId ? `（${row.reviewId}）` : ""}</td><td>{new Date(row.effectiveAt).toLocaleString("zh-CN")}</td><td>{row.publishedAt ? new Date(row.publishedAt).toLocaleString("zh-CN") : levelKind(row.kind) || bonusKind(row.kind) ? "无需公告" : <PermissionButton permissionsState={effectivePermissions} permissions={manage} disabled={busy || row.status !== "approved" || !publicationReason.trim()} onClick={() => void action(() => api.post(`/api/v1/loyalty/ticket-reward-configs/${encodeURIComponent(row.id)}/publish`, { idempotencyKey: crypto.randomUUID(), reason: publicationReason }))}>发布已审批版本</PermissionButton>}</td></tr>)}
    </tbody></table>
  </section>;
}
