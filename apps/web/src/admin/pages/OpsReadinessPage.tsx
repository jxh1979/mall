import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, ReadinessItemRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 后端写权限码（ProductionReadinessController：PUT /api/v1/ops/production-readiness/items/{id} requirePermission）。
const READINESS_MANAGE_PERMISSION = "ops.production_readiness.manage";

export function OpsReadinessPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<ReadinessItemRow[]>([]);
  const { setSelectedId, selected } = useSelectedRow(rows);
  const [evidenceRef, setEvidenceRef] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  // 行明细抽屉：整行可点 → 该生产 blocker 完整字段（状态中文·到期时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<ReadinessItemRow[]>("/api/v1/ops/production-readiness/items");
      setRows(items);
    },
    { onError: setGlobalError, fallbackMessage: "生产证据读取失败" },
  );

  async function verifyBlocker() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要标记的生产 blocker。");
      return;
    }
    if (!evidenceRef.trim()) {
      setFormError("必须填写证据引用，不能把密钥或明文凭证直接写入证据。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    try {
      const result = await api.put(`/api/v1/ops/production-readiness/items/${selected.id}`, { status: "verified", evidenceRef });
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

  function openBlockerDetail(row: ReadinessItemRow) {
    setDetail({
      title: `生产阻断项明细 · ${row.blockerCode ?? row.title ?? row.id}`,
      fields: [
        { label: "阻断项", value: row.blockerCode ?? row.title ?? "—" },
        { label: "标题", value: row.title ?? "—" },
        { label: "责任人", value: row.owner ?? "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "证据引用", value: row.evidenceRef ?? "—" },
        { label: "到期时间", value: cnDate(row.dueAt) },
        { label: "编号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="ops-readiness-page">
      <AdminPageHeader title="生产 blocker 与证据" description="生产账号、NAS、数据库、对象存储、JWT/第三方密钥轮换均以证据引用推进，不保存明文。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="ops-evidence-ref" value={evidenceRef} onChange={(event) => setEvidenceRef(event.target.value)} placeholder="evidence://..." />
          <PermissionButton
            data-testid="ops-verify-blocker"
            disabled={submitting}
            permissionsState={effectivePermissions}
            permissions={[READINESS_MANAGE_PERMISSION]}
            onClick={() => void verifyBlocker()}
          >标记已验证</PermissionButton>
        </div>
        {formError ? <div data-testid="ops-readiness-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ops-readiness-table">
            <thead><tr><th>选择</th><th>阻断项</th><th>责任人</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`ops-readiness-row-${row.id}`}
                onClick={() => openBlockerDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBlockerDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此阻断项 ${row.id}`} data-testid={`ops-readiness-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{row.blockerCode ?? row.title}</td>
                <td>{row.owner}</td>
                <td>{formatBusinessStatus(row.status)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看生产阻断项详情" onClick={(event) => { event.stopPropagation(); openBlockerDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
