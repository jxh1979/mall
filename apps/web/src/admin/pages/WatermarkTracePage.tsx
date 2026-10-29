// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { adminRoutes } from "../adminRoutes";
import { EXPORT_TYPE_LABELS, filterText } from "./MySecurityExportsPage";
import type { AdminPageProps } from "../types";

// 后端 POST /api/v1/security/watermarks/trace 的结果（客户端已 snake→camel）。
type TraceResult = {
  code: string;
  accountId: string;
  username?: string | null;
  deviceNo: string;
  network?: string | null;
  kind: "page" | "export";
  pageId?: string | null;
  exportFileId?: string | null;
  openedAt: string;
};

// 后端 POST /api/v1/security/export-files/lookup 的结果（第 23 条 23-R3）。
type PasswordTrace = {
  exportFileId: string;
  exporterId: string;
  username?: string | null;
  exportedAt: string;
  deviceNo: string;
  network?: string | null;
  exportType: string;
  filters?: Record<string, unknown> | null;
  rowCount: number;
};

function beijingTime(value: string): string {
  return new Date(value).toLocaleString("zh-CN", { timeZone: "Asia/Shanghai", hour12: false });
}

function pageName(pageId?: string | null): string {
  const route = adminRoutes.find((item) => item.pageId === pageId);
  return route?.label ?? pageId ?? "—";
}

/**
 * ADM-SEC-002 暗纹与导出密码反查（第 23 条 23-R2，PGM:687「暗纹可反向推算：把页面的截屏图片、或对着屏幕拍摄的照片上传到系统，
 * 即可算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的」）：上传截屏或拍屏照片，显示打开该页面的账号、设备与时间。
 * 反查权限须单独授予（PGM:666），解不出暗纹或目标账号不在本人可触及范围内都按查不到处理。
 */
export function WatermarkTracePage({ api, setGlobalError }: AdminPageProps) {
  const [file, setFile] = React.useState<File | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [result, setResult] = React.useState<TraceResult | null>(null);
  const [password, setPassword] = React.useState("");
  const [exportTrace, setExportTrace] = React.useState<PasswordTrace | null>(null);

  // 第 23 条 23-R3（PGM:687「可由系统反向推算出对应的导出人、导出时间与导出设备」）：密码放请求体，不放网址。
  async function lookup() {
    if (!password.trim()) return;
    setBusy(true);
    setExportTrace(null);
    setGlobalError(null);
    try {
      setExportTrace(await api.post<PasswordTrace>("/api/v1/security/export-files/lookup", { password: password.trim() }));
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "反查失败");
    } finally {
      setBusy(false);
    }
  }

  async function trace() {
    if (!file || !api.postForm) return;
    setBusy(true);
    setResult(null);
    setGlobalError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      setResult(await api.postForm<TraceResult>("/api/v1/security/watermarks/trace", form));
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "反查失败");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section data-testid="watermark-trace-page">
      <AdminPageHeader
        title="暗纹与导出密码反查"
        description="上传页面截屏，或对着屏幕拍的照片（PNG、JPEG），按页面暗纹算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的；输入导出文件的打开密码，算出是谁、什么时间、在哪一台设备上导出的。"
      />
      <div className="admin-toolbar">
        <label>
          截屏或照片
          <input type="file" accept="image/png,image/jpeg" data-testid="watermark-trace-file" disabled={busy}
            onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        </label>
        <button type="button" data-testid="watermark-trace-submit" disabled={!file || busy} onClick={() => void trace()}>
          {busy ? "反查中" : "反查"}
        </button>
      </div>
      {result ? (
        <table className="admin-table" data-testid="watermark-trace-result">
          <tbody>
            <tr><th>打开账号</th><td>{result.username ?? result.accountId}</td></tr>
            <tr><th>设备编号</th><td>{result.deviceNo}</td></tr>
            <tr><th>接入网络</th><td>{result.network ?? "—"}</td></tr>
            <tr><th>{result.kind === "export" ? "导出文件" : "打开页面"}</th><td>{result.kind === "export" ? result.exportFileId : pageName(result.pageId)}</td></tr>
            <tr><th>{result.kind === "export" ? "导出时间" : "打开时间"}</th><td>{beijingTime(result.openedAt)}</td></tr>
            <tr><th>暗纹码</th><td>{result.code}</td></tr>
          </tbody>
        </table>
      ) : null}
      <div className="admin-toolbar">
        <label>
          导出文件的打开密码
          <input value={password} maxLength={10} data-testid="export-password-input" disabled={busy}
            onChange={(event) => setPassword(event.target.value)} />
        </label>
        <button type="button" data-testid="export-password-submit" disabled={!password.trim() || busy} onClick={() => void lookup()}>
          {busy ? "反查中" : "按密码反查"}
        </button>
      </div>
      {exportTrace ? (
        <table className="admin-table" data-testid="export-password-result">
          <tbody>
            <tr><th>导出人</th><td>{exportTrace.username ?? exportTrace.exporterId}</td></tr>
            <tr><th>导出时间</th><td>{beijingTime(exportTrace.exportedAt)}</td></tr>
            <tr><th>导出设备</th><td>{exportTrace.deviceNo}</td></tr>
            <tr><th>接入网络</th><td>{exportTrace.network ?? "—"}</td></tr>
            <tr><th>导出内容</th><td>{EXPORT_TYPE_LABELS[exportTrace.exportType] ?? exportTrace.exportType}</td></tr>
            <tr><th>筛选条件</th><td>{filterText(exportTrace.filters)}</td></tr>
            <tr><th>行数</th><td>{exportTrace.rowCount}</td></tr>
          </tbody>
        </table>
      ) : null}
    </section>
  );
}
