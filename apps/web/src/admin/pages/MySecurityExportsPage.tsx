// 文件版本：V0.01
import React from "react";
import { saveDownloadedFile } from "../client/adminClient";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { FormModal } from "../components/FormModal";
import { TablePager } from "../components/TablePager";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { FIELD_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps } from "../types";

// 后端 GET /api/v1/security/my-exports 的一行（客户端已 snake→camel）。
type MyExport = {
  id: string;
  exportType: string;
  filters?: Record<string, unknown> | null;
  rowCount: number;
  exportedAt: string;
  deviceNo: string;
  network?: string | null;
  editable: boolean;
  password: string;
  /** 23-R4 本人对这份导出最近一次可编辑版申请（主管复核单；没有为 null）。 */
  editableRequest?: EditableRequest | null;
};

type EditableRequest = { reviewId: string; reviewStatus: string; issued: boolean };

/** 可编辑版申请的审批状态。 */
function requestText(request?: EditableRequest | null): string {
  if (!request) return "—";
  if (request.reviewStatus === "approved") return request.issued ? "已发放" : "已批准，待下载";
  if (request.reviewStatus === "rejected") return "已驳回";
  return "待最高权限审批";
}

/** 还在路上的申请（待审批，或已批准未下载）：不再重复申请。 */
function inFlight(request?: EditableRequest | null): boolean {
  return !!request && (request.reviewStatus === "pending_review" || (request.reviewStatus === "approved" && !request.issued));
}

/** 导出内容（export_files.export_type 闭集）。 */
export const EXPORT_TYPE_LABELS: Record<string, string> = {
  analytics_monthly_report: "月度经营对比",
  finance_ar_ap_ledger: "应收应付台账",
  audit_operation_events: "操作留痕",
  settlement_invoice_items: "开票清单",
};

/** 筛选条件与取数范围：键按字段中文名，空值为「不限」。 */
export function filterText(filters?: Record<string, unknown> | null): string {
  const parts = Object.entries(filters ?? {}).map(([key, value]) => {
    const name = FIELD_LABELS[key.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase()] ?? key;
    const shown = value === null || value === undefined || value === "" ? "不限" : Array.isArray(value) ? value.join("、") : String(value);
    return `${name}：${shown}`;
  });
  return parts.length ? parts.join("；") : "—";
}

function beijingTime(value: string): string {
  return new Date(value).toLocaleString("zh-CN", { timeZone: "Asia/Shanghai", hour12: false });
}

/**
 * ADM-SEC-001 我的导出（第 23 条 23-R3，PGM:687「导出人可以在专门的地方查到自己导出文件的密码」）：只列本人导出的文件与打开密码。
 * 密码由系统按导出人、导出时间、导出设备与接入网络生成，不能取消、不能自己设定；系统不代发给接收方。
 * 23-R4（PGM:687「确需可编辑版本用于二次加工的，须另行申请、说明用途与接收方，经最高权限审批后发放，可编辑版同样带密码与暗纹并逐次留痕」）：
 * 非可编辑版的导出可申请可编辑版，显示审批状态；批准后下载一次，可编辑版作为新的一行列出、打开密码同样在这里查看。
 */
const PAGE_SIZE = 20;

export function MySecurityExportsPage({ api, setGlobalError }: AdminPageProps) {
  // 导出记录永久保存（PGM:686），本人的导出按页翻，更早的文件与密码同样查得到。
  const exports = useServerPagedRows(
    (page, size) => api.getPaged<MyExport>(`/api/v1/security/my-exports?page=${page}&size=${size}`),
    PAGE_SIZE, { onError: setGlobalError, fallbackMessage: "我的导出读取失败" });
  const { rows, loading, error, reload } = exports;
  const [applying, setApplying] = React.useState<MyExport | null>(null);
  const [form, setForm] = React.useState({ purpose: "", recipient: "" });
  const [applyError, setApplyError] = React.useState<string>();
  const submitRequest = async () => {
    if (!applying) return;
    try {
      await api.post(`/api/v1/security/export-files/${encodeURIComponent(applying.id)}/editable-requests`, {
        idempotency_key: `editable-${applying.id}-${Date.now()}`,
        purpose: form.purpose.trim(),
        recipient: form.recipient.trim(),
      });
      setApplying(null);
      reload();
    } catch (e) { setApplyError((e as Error).message); }
  };

  const download = async (row: MyExport) => {
    if (!api.getFile || !row.editableRequest) return;
    try {
      saveDownloadedFile(await api.getFile(`/api/v1/security/export-files/${encodeURIComponent(row.id)}/editable-copy?review_id=${encodeURIComponent(row.editableRequest.reviewId)}`));
      reload();
    } catch (e) { setGlobalError((e as Error).message); }
  };

  return (
    <section data-testid="my-exports-page">
      <AdminPageHeader
        title="我的导出"
        description="本人导出的文件与打开密码。导出文件一律带密码，密码由系统按导出人、导出时间、导出设备与接入网络生成，不能取消、不能自己设定。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="my-exports-table">
            <thead>
              <tr><th>导出时间</th><th>导出内容</th><th>筛选条件</th><th>行数</th><th>导出设备</th><th>可编辑版</th><th>打开密码</th><th>可编辑版申请</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`my-export-${row.id}`}>
                  <td>{beijingTime(row.exportedAt)}</td>
                  <td>{EXPORT_TYPE_LABELS[row.exportType] ?? row.exportType}</td>
                  <td>{filterText(row.filters)}</td>
                  <td>{row.rowCount}</td>
                  <td>{row.deviceNo}</td>
                  <td>{row.editable ? "是" : "否"}</td>
                  <td><code>{row.password}</code></td>
                  <td>
                    {requestText(row.editableRequest)}
                    {row.editableRequest?.reviewStatus === "approved" && !row.editableRequest.issued ? (
                      <> <button type="button" className="admin-secondary-button" data-testid={`my-export-editable-download-${row.id}`} onClick={() => void download(row)}>下载可编辑版</button></>
                    ) : null}
                    {!row.editable && !inFlight(row.editableRequest) ? (
                      <> <button type="button" className="admin-secondary-button" data-testid={`my-export-editable-request-${row.id}`}
                        onClick={() => { setForm({ purpose: "", recipient: "" }); setApplyError(undefined); setApplying(row); }}>申请可编辑版</button></>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <TablePager testid="my-exports-pager" page={exports.page} pageCount={exports.pageCount} total={exports.total} onPrev={exports.prev} onNext={exports.next} />
      </AdminDataState>
      <FormModal
        open={applying !== null}
        title="申请可编辑版"
        description="确需可编辑版本用于二次加工的，须说明用途与接收方，经最高权限审批后发放；可编辑版同样带密码与暗纹，一次批准发放一次。"
        testId="my-export-editable-modal"
        submitTestId="my-export-editable-submit"
        submitLabel="提交申请"
        error={applyError}
        validate={() => (!form.purpose.trim() ? "请填写用途" : !form.recipient.trim() ? "请填写接收方" : null)}
        onClose={() => setApplying(null)}
        onSubmit={() => void submitRequest()}
      >
        <label>用途<input data-testid="my-export-editable-purpose" value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} /></label>
        <label>接收方<input data-testid="my-export-editable-recipient" value={form.recipient} onChange={(e) => setForm({ ...form, recipient: e.target.value })} /></label>
      </FormModal>
    </section>
  );
}
