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
  const [ownerPerson, setOwnerPerson] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  // 行明细抽屉：整行可点 → 该阻断项完整字段（状态中文·目标日期中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<ReadinessItemRow[]>("/api/v1/ops/production-readiness/items");
      setRows(items);
    },
    { onError: setGlobalError, fallbackMessage: "生产证据读取失败" },
  );

  // 第十八批 AN 段第 231 条 231-R1：选中行变化时带出该行责任人（默认项责任人为空，须填写；契约 owner_person「不得用空值替代责任人」）。
  React.useEffect(() => {
    setOwnerPerson(selected?.ownerPerson ?? "");
  }, [selected?.id, selected?.ownerPerson]);

  async function verifyBlocker() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要标记的阻断项。");
      return;
    }
    if (!evidenceRef.trim()) {
      setFormError("必须填写证据引用，不能把密钥或明文凭证直接写入证据。");
      return;
    }
    if (!ownerPerson.trim()) {
      setFormError("必须填写责任人。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    try {
      // 231-R1：按契约 ProductionReadinessItemUpdateRequest 发 passed 与必填项（幂等键由客户端补）；责任部门、目标日期、阻断范围、降级方式从选中行原样带回。
      await api.put(`/api/v1/ops/production-readiness/items/${selected.id}`, {
        status: "passed",
        evidenceLink: evidenceRef,
        ownerPerson,
        ownerDepartment: selected.ownerDepartment,
        targetDate: selected.targetDate,
        blockingScope: selected.blockingScope,
        fallbackMode: selected.fallbackMode,
      });
      // 结果区只放动作、状态与阻断项名称（响应里的分类码、操作手册引用等不展示）。
      setActionResult({ action: "ops_blocker_verify", status: "passed", item_name: selected.itemName });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "生产阻断项验证失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ops_blocker_verify", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function openBlockerDetail(row: ReadinessItemRow) {
    setDetail({
      title: `生产阻断项明细 · ${row.itemName ?? row.id}`,
      fields: [
        { label: "阻断项", value: row.itemName ?? "—" },
        { label: "责任部门", value: row.ownerDepartment || "—" },
        { label: "责任人", value: row.ownerPerson || "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "证据引用", value: row.evidenceLink ?? "—" },
        { label: "目标日期", value: cnDate(row.targetDate) },
        { label: "编号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="ops-readiness-page">
      <AdminPageHeader title="生产阻断项与证据" description="生产账号、网络存储、数据库、对象存储、登录令牌与第三方密钥轮换均以证据引用推进，不保存明文。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="ops-evidence-ref" value={evidenceRef} onChange={(event) => setEvidenceRef(event.target.value)} placeholder="证据引用（不得填密钥明文）" />
          <input data-testid="ops-owner-person" aria-label="责任人" value={ownerPerson} onChange={(event) => setOwnerPerson(event.target.value)} placeholder="责任人" />
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
                <td>{row.itemName ?? "—"}</td>
                <td>{`${row.ownerDepartment || "—"}／${row.ownerPerson || "—"}`}</td>
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
