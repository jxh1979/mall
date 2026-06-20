import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, ReadinessItemRow } from "../types";

export function OpsReadinessPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<ReadinessItemRow[]>([]);
  const [evidenceRef, setEvidenceRef] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<ReadinessItemRow[]>("/api/v1/ops/production-readiness/items");
      setRows(items);
    },
    { onError: setGlobalError, fallbackMessage: "生产证据读取失败" },
  );

  async function verifyBlocker() {
    if (!evidenceRef.trim()) {
      setFormError("必须填写证据引用，不能把密钥或明文凭证直接写入证据。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const item = rows[0];
    try {
      const result = await api.patch(`/api/v1/ops/production-readiness/items/${item?.id ?? "blocker-1"}`, { status: "verified", evidenceRef });
      setActionResult({ action: "ops_blocker_verify", status: "verified", result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "生产 blocker 验证失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ops_blocker_verify", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="ops-readiness-page">
      <AdminPageHeader title="生产 blocker 与证据" description="生产账号、NAS、数据库、对象存储、JWT/第三方密钥轮换均以证据引用推进，不保存明文。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="ops-evidence-ref" value={evidenceRef} onChange={(event) => setEvidenceRef(event.target.value)} placeholder="evidence://..." />
          <button type="button" data-testid="ops-verify-blocker" disabled={submitting} onClick={() => void verifyBlocker()}>标记已验证</button>
        </div>
        {formError ? <div data-testid="ops-readiness-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ops-readiness-table">
            <thead><tr><th>Blocker</th><th>责任人</th><th>状态</th></tr></thead>
            <tbody>{rows.map((row) => <tr key={row.id}><td>{row.blockerCode ?? row.title}</td><td>{row.owner}</td><td>{row.status}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
