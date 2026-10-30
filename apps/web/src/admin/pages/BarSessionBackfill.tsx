// 文件版本：V0.02
import React from "react";
import { withBase } from "../../basePath";
import { FormModal } from "../components/FormModal";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BarReservationRow, BarTableSessionRow } from "../types";

// 第十八批 F 段第 14 条 14-R7：台位会话两类人工补录（销售归属 14-R3、服务责任区间 14-R6）的发起与并列展示。
// BSM:1642「补录必须附原始记录凭证（微信截图、通话录音等）并经审核，以通话录音为凭证的，录音起始时间必须早于客人到店时间、且早于客人开卡时间
// 达到后台配置的时长……补录记录与原始记录并列展示，标明“人工补录”」；BSM:1021「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」。
// 凭证是否通话录音按上传后服务端回的文件类型（audio/*）判，与后端提交校验同口径。

type Api = AdminPageProps["api"];
export type SessionBackfill = {
  id: string; proposed?: Record<string, unknown>; originalSnapshot?: { sessionSales?: string | null; guestSource?: string | null } | null; evidenceAttachmentIds?: string[];
  recordingStartedAt?: string | null; reviewId?: string; status: string;
};
export type BackfillProof = { reason: string; responsible_staff_id: string; evidence_attachment_ids: string[]; recording_started_at?: string };

const STATUS: Record<string, string> = { pending: "待复核", approved: "已通过", rejected: "已驳回" };
export const time = (value?: string | null) => (value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—");
export const ManualTag = () => <span className="admin-badge is-warn">人工补录</span>;
export const failure = (e: unknown, fallback: string) => (e instanceof Error && e.message) || fallback;
/** 第十八批 K 段 95-R1（BSM:1021）：开台客人来源中文名（KEY_VALUE_LABELS.guest_source）。 */
export const guestSourceLabel = (code?: string | null) => KEY_VALUE_LABELS.guest_source?.[code ?? ""] ?? "—";

export function loadSessionBackfills(api: Api, type: "bar_session_sales" | "bar_session_service", sessionId: string) {
  return api.get<SessionBackfill[]>(`/api/v1/evidence/manual-backfills?target_type=${type}&target_id=${encodeURIComponent(sessionId)}`);
}

/** 补录行末四列：证据、录音起始、复核单号（链到审批链）、状态。 */
export function BackfillTail({ row }: { row: SessionBackfill }) {
  return <>
    <td>{(row.evidenceAttachmentIds ?? []).map((id, index) => (
      <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">证据{index + 1} </a>
    ))}</td>
    <td>{time(row.recordingStartedAt)}</td>
    <td>{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>{row.reviewId}</a> : "—"}</td>
    <td>{STATUS[row.status] ?? row.status}</td>
  </>;
}

type FormProps = {
  open: boolean; title: string; testId: string; api: Api; submitting: boolean; error: string; children: React.ReactNode;
  validate: () => string | null; onSubmit: (proof: BackfillProof) => void; onClose: () => void;
};

/** 补录凭证表单：对象特有字段由 children 给，公共的原因、责任人、原始记录凭证（business_type=manual_backfill）与录音起始时间在此。 */
export function BackfillFormModal({ open, title, testId, api, submitting, error, children, validate, onSubmit, onClose }: FormProps) {
  const [reason, setReason] = React.useState("");
  const [responsible, setResponsible] = React.useState("");
  const [recording, setRecording] = React.useState("");
  const [proofs, setProofs] = React.useState<{ id: string; name: string; audio: boolean }[]>([]);
  const [uploading, setUploading] = React.useState(false);
  const [uploadError, setUploadError] = React.useState("");
  React.useEffect(() => { if (open) { setReason(""); setResponsible(""); setRecording(""); setProofs([]); setUploadError(""); } }, [open]);
  async function upload(files: FileList | null) {
    if (!files?.length || !api.postForm) return;
    setUploading(true); setUploadError("");
    try {
      for (const file of Array.from(files)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", "manual_backfill");
        const saved = await api.postForm<{ id: string; contentType?: string }>("/api/v1/evidence/attachments/upload", data);
        setProofs((current) => [...current, { id: saved.id, name: file.name, audio: Boolean(saved.contentType?.startsWith("audio/")) }]);
      }
    } catch (e) { setUploadError(failure(e, "证据上传失败")); } finally { setUploading(false); }
  }
  return (
    <FormModal open={open} title={title} testId={testId} submitTestId={`${testId}-submit`} submitLabel="提交补录" submitting={submitting}
      submitDisabled={uploading || proofs.length === 0} error={uploadError || error}
      description="补录须写原因和责任人，并附原始记录凭证（微信截图、通话录音等）；以通话录音为凭证的须填录音起始时间。提交后进主管复核，通过才生效，原记录不改。"
      validate={() => validate() ?? (!reason.trim() ? "请填写补录原因" : !responsible.trim() ? "请填写责任人"
        : proofs.some((proof) => proof.audio) && !recording ? "凭证含通话录音，请填写录音起始时间" : null)}
      onSubmit={() => onSubmit({ reason: reason.trim(), responsible_staff_id: responsible.trim(), evidence_attachment_ids: proofs.map((proof) => proof.id),
        ...(recording ? { recording_started_at: new Date(recording).toISOString() } : {}) })}
      onClose={onClose}>
      {children}
      <label>补录原因<input data-testid={`${testId}-reason`} value={reason} onChange={(event) => setReason(event.target.value)} /></label>
      <label>责任人账号<input data-testid={`${testId}-responsible`} value={responsible} onChange={(event) => setResponsible(event.target.value)} /></label>
      <label>原始记录凭证（微信截图、通话录音等，可多选）
        <input data-testid={`${testId}-evidence`} type="file" multiple accept="image/*,audio/*,video/*,.m4a,.aac,.mp3,.amr,.wav" disabled={submitting || uploading}
          onChange={(event) => void upload(event.target.files)} />
      </label>
      <ul>{proofs.map((proof) => <li key={proof.id}>{proof.name}（已上传）</li>)}</ul>
      <label>录音起始时间（凭证含通话录音时必填）
        <input data-testid={`${testId}-recording`} type="datetime-local" value={recording} onChange={(event) => setRecording(event.target.value)} />
      </label>
    </FormModal>
  );
}

type SalesProps = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & {
  session?: BarTableSessionRow; reservation?: BarReservationRow; onChanged?: () => void;
};
const GUEST_SOURCES = ["walk_in", "platform_own", "marketing"] as const;

/** 本场客人来源与销售归属：现值、该场全部销售归属补录并列；本场与其订台都没有销售、仍开台时可发起补录（店长复核，后端另核账单结清）。 */
export function BarSalesBackfillPanel({ api, effectivePermissions, setActionResult, session, reservation, onChanged }: SalesProps) {
  const [rows, setRows] = React.useState<SessionBackfill[]>([]);
  const [error, setError] = React.useState("");
  const [open, setOpen] = React.useState(false);
  const [sales, setSales] = React.useState("");
  const [approver, setApprover] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  // 第十八批 K 段 95-R2（BSM:1021「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）：店长持改派权限本人办理，后端另核结清与补录界线。
  const [reassignOpen, setReassignOpen] = React.useState(false);
  const [source, setSource] = React.useState("walk_in");
  const [reassignSales, setReassignSales] = React.useState("");
  const [reassignError, setReassignError] = React.useState("");
  const sessionId = session?.id;
  const [loaded, setLoaded] = React.useState(0);
  React.useEffect(() => {
    let live = true;
    setRows([]); setError("");
    if (sessionId) {
      loadSessionBackfills(api, "bar_session_sales", sessionId)
        .then((result) => { if (live) setRows(result); })
        .catch((e) => { if (live) setError(failure(e, "销售归属补录读取失败")); });
    }
    return () => { live = false; };
  }, [api, sessionId, loaded]);

  async function submit(proof: BackfillProof) {
    if (!session) return;
    const requestBody = { sales_staff_id: sales.trim(), ...proof, ...(approver.trim() ? { final_approver_id: approver.trim() } : {}) };
    setBusy(true); setFormError("");
    try {
      const result = await api.post(`/api/v1/bar/table-sessions/${encodeURIComponent(session.id)}/sales-backfill`, requestBody);
      setActionResult({ action: "bar_sales_backfill_submitted", requestBody, result });
      setOpen(false); setLoaded((n) => n + 1);
    } catch (e) { setFormError(failure(e, "销售归属补录提交失败")); } finally { setBusy(false); }
  }

  async function reassign() {
    if (!session) return;
    const requestBody = { guest_source: source, ...(source === "marketing" ? { sales_staff_id: reassignSales.trim() } : {}) };
    setBusy(true); setReassignError("");
    try {
      const result = await api.post(`/api/v1/bar/table-sessions/${encodeURIComponent(session.id)}/attribution-reassign`, requestBody);
      setActionResult({ action: "bar_table_session_attribution_reassigned", requestBody, result });
      setReassignOpen(false); onChanged?.();
    } catch (e) { setReassignError(failure(e, "改派客人来源与销售失败")); } finally { setBusy(false); }
  }

  const canStart = session?.status === "open" && !session.salesStaffId && !reservation?.salesStaffId;
  return (
    <details data-testid="bar-sales-backfill"><summary>本场销售归属与人工补录</summary>
      <p data-testid="bar-sales-current">当前场次：{session?.id ?? "请先选择"}；客人来源：{guestSourceLabel(session?.guestSource)}；销售：{session?.salesStaffId ?? "无"}
        {session?.salesStaffId && rows.some((row) => row.status === "approved") ? <ManualTag /> : null}</p>
      {error ? <p role="alert">{error}</p> : null}
      <table className="admin-table" data-testid="bar-sales-backfills">
        <thead><tr><th>标记</th><th>原记录</th><th>拟归属人</th><th>证据</th><th>录音起始</th><th>复核单号</th><th>状态</th></tr></thead>
        <tbody>{rows.length === 0 ? <tr><td colSpan={7}>暂无销售归属补录</td></tr> : rows.map((row) => (
          <tr key={row.id} data-testid={`bar-sales-backfill-${row.id}`}>
            <td><ManualTag /></td><td>{row.originalSnapshot?.sessionSales ?? `无销售（${guestSourceLabel(row.originalSnapshot?.guestSource)}）`}</td><td>{String(row.proposed?.salesStaffId ?? "—")}</td>
            <BackfillTail row={row} />
          </tr>
        ))}</tbody>
      </table>
      {canStart ? (
        <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table_session.create", "evidence.attachment.create"]} disabled={busy}
          data-testid="bar-sales-backfill-open" onClick={() => { setSales(""); setApprover(""); setFormError(""); setOpen(true); }}>补录销售归属</PermissionButton>
      ) : null}
      {session?.status === "open" ? (
        <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table_session.attribution_reassign"]} disabled={busy}
          data-testid="bar-attribution-reassign-open"
          onClick={() => { setSource(session.guestSource ?? "walk_in"); setReassignSales(session.salesStaffId ?? ""); setReassignError(""); setReassignOpen(true); }}>
          改派来源与销售</PermissionButton>
      ) : null}
      <FormModal open={reassignOpen} title="改派客人来源与销售人员" testId="bar-attribution-reassign-form" submitTestId="bar-attribution-reassign-form-submit"
        submitLabel="确认改派" submitting={busy} error={reassignError}
        validate={() => (source === "marketing" && !reassignSales.trim() ? "营销带客须填写销售账号" : null)}
        onSubmit={() => void reassign()} onClose={() => setReassignOpen(false)}>
        <label>客人来源<select data-testid="bar-attribution-reassign-source" value={source} onChange={(event) => setSource(event.target.value)}>
          {GUEST_SOURCES.map((code) => <option key={code} value={code}>{guestSourceLabel(code)}</option>)}
        </select></label>
        {source === "marketing" ? (
          <label>销售账号<input data-testid="bar-attribution-reassign-staff" value={reassignSales} onChange={(event) => setReassignSales(event.target.value)} /></label>
        ) : null}
      </FormModal>
      <BackfillFormModal open={open} title="补录销售归属" testId="bar-sales-backfill-form" api={api} submitting={busy} error={formError}
        validate={() => (!sales.trim() ? "请填写拟归属销售账号" : null)} onSubmit={(proof) => void submit(proof)} onClose={() => setOpen(false)}>
        <label>拟归属销售账号<input data-testid="bar-sales-backfill-staff" value={sales} onChange={(event) => setSales(event.target.value)} /></label>
        <label>最终审批人（店长，选填）<input data-testid="bar-sales-backfill-approver" value={approver} onChange={(event) => setApprover(event.target.value)} /></label>
      </BackfillFormModal>
    </details>
  );
}
