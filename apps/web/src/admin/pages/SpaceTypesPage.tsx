import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { HighRiskConfirm } from "../components/HighRiskConfirm";

// 空间类型字典行（后端 SpaceTypeRecord，客户端 unwrap 已 camel 化）。
type SpaceTypeRow = {
  id: string;
  typeCode: string;
  typeName: string;
  sortOrder: number;
  status: string;
};

const MANAGE = ["governance.space.manage"];

/**
 * 空间类型字典管理页（ADM-GOV-007，条目3b）：平台管理员维护空间类型（楼栋/楼层/区块/铺位/公共区域…），
 * 作为空间建档 space_type 的下拉数据源与硬校验依据。新增/启用后即可用于空间 space_type 校验。
 */
export function SpaceTypesPage({ api, user, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";
  const [rows, setRows] = React.useState<SpaceTypeRow[]>([]);
  const [form, setForm] = React.useState({ typeCode: "", typeName: "", sortOrder: "100", status: "active" });
  // 引导式流程 phase-7：新增空间类型收起为主按钮 + 分步引导弹窗。
  const [createOpen, setCreateOpen] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "空间种类读取失败" },
  );

  // 下钻定位:从别的页点空间引用进来(?focus=id)→ 滚动到命中的空间类型行(按 id 或 typeCode 匹配)并高亮;无 focus 或未命中时不动。
  React.useEffect(() => {
    if (!focusObjectId || rows.length === 0 || typeof document === "undefined") return;
    const el = document.querySelector(`[data-focus-space="${focusObjectId}"]`);
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
  }, [focusObjectId, rows.length]);

  const isFocused = (row: SpaceTypeRow) => !!focusObjectId && (focusObjectId === row.id || focusObjectId === row.typeCode);

  // 编码/显示名非空校验（步内 + 提交双保险）。
  function typeInvalid() {
    return !form.typeCode.trim() || !form.typeName.trim();
  }

  async function saveType() {
    const code = form.typeCode.trim();
    const name = form.typeName.trim();
    if (typeInvalid()) {
      setFormError("请填写种类编码与显示名");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    try {
      await api.post("/api/v1/platform/space-types", {
        idempotencyKey: `space-type-${code}-${form.sortOrder}`,
        typeCode: code,
        typeName: name,
        sortOrder: Number(form.sortOrder) || 100,
        status: form.status,
      });
      setActionResult({ action: "space_type_saved", typeCode: code });
      setForm({ typeCode: "", typeName: "", sortOrder: "100", status: "active" });
      setCreateOpen(false);
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新增空间种类失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function archiveType(row: SpaceTypeRow) {
    setGlobalError(null);
    try {
      await api.post(`/api/v1/platform/space-types/${row.id}/archive`);
      setActionResult({ action: "space_type_archived", typeCode: row.typeCode });
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "存档空间种类失败";
      setGlobalError(message);
    }
  }

  async function requestDeleteType(row: SpaceTypeRow) {
    setGlobalError(null);
    const reason = typeof window !== "undefined" ? window.prompt("请填写删除理由") : "";
    if (!reason || !reason.trim()) return;
    try {
      await api.post(`/api/v1/platform/space-types/${row.id}/delete-request`, { reason: reason.trim() });
      setActionResult({ action: "space_type_delete_requested", typeCode: row.typeCode });
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "申请删除失败";
      setGlobalError(message);
    }
  }

  async function approveDeleteType(row: SpaceTypeRow) {
    setGlobalError(null);
    try {
      await api.post(`/api/v1/platform/space-types/${row.id}/delete-approve`, { decision: "approved" });
      setActionResult({ action: "space_type_delete_approved", typeCode: row.typeCode });
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "审批删除失败";
      setGlobalError(message);
    }
  }

  return (
    <section data-testid="space-types-page">
      <AdminPageHeader
        title="空间种类"
        description="维护空间种类。建空间时从本表选择种类。可新增。下线用存档。删除须先存档，再申请并经最高权限审批。有空间还在用的种类不能删除。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="space-types-table">
            <thead>
              <tr><th>编码</th><th>名称</th><th>排序</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.typeCode} data-testid={`space-type-${row.typeCode}`}
                    data-focus-space={isFocused(row) ? focusObjectId : undefined}
                    className={isFocused(row) ? "admin-row-selected" : undefined}>
                  <td>{row.typeCode}</td><td>{row.typeName}</td><td>{row.sortOrder}</td><td>{formatBusinessStatus(row.status)}</td>
                  <td>
                    {row.status === "active" ? (
                      <HighRiskConfirm
                        label="存档"
                        impact={`把种类「${row.typeName}」存档后，不能再用来新建空间。已经建好的空间仍保留这个种类。存档之后才能申请删除。`}
                        onConfirm={() => void archiveType(row)}
                        permissionsState={effectivePermissions}
                        permissions={MANAGE}
                        data-testid={`space-type-archive-${row.typeCode}`}
                      />
                    ) : null}
                    {row.status === "archived" ? (
                      <HighRiskConfirm
                        label="申请删除"
                        impact={`申请删除种类「${row.typeName}」。须填写理由，并由最高权限审批。有空间占用则不能删。`}
                        onConfirm={() => void requestDeleteType(row)}
                        permissionsState={effectivePermissions}
                        permissions={MANAGE}
                        data-testid={`space-type-delete-request-${row.typeCode}`}
                      />
                    ) : null}
                    {row.status === "pending_delete" && isMaster ? (
                      <HighRiskConfirm
                        label="审批删除"
                        impact={`由最高权限审批删除种类「${row.typeName}」。申请人不能批自己。`}
                        onConfirm={() => void approveDeleteType(row)}
                        permissionsState={effectivePermissions}
                        permissions={MANAGE}
                        data-testid={`space-type-delete-approve-${row.typeCode}`}
                      />
                    ) : row.status === "pending_delete" ? (
                      <span>待最高权限审批</span>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-form-row" data-testid="space-types-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="space-types-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>
            ＋ 新增空间种类
          </PermissionButton>
        </div>
      </AdminDataState>
      <GuidedFlowModal
        open={createOpen}
        title="新增空间种类"
        testId="space-types-guided"
        submitTestId="space-types-save"
        submitLabel="保存种类"
        submitting={submitting}
        error={formError || undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void saveType()}
        steps={[
          {
            title: "种类信息",
            description: "填写种类编码与显示名，设置排序与启用状态。编码/显示名必填。",
            validate: () => (typeInvalid() ? "请填写种类编码与显示名" : null),
            content: (
              <>
                <label>种类编码<input aria-label="种类编码" placeholder="种类编码（如铺位）" value={form.typeCode}
                  onChange={(event) => setForm({ ...form, typeCode: event.target.value })} /></label>
                <label>显示名<input aria-label="显示名" placeholder="显示名（如 一体机位）" value={form.typeName}
                  onChange={(event) => setForm({ ...form, typeName: event.target.value })} /></label>
                <label>排序<input aria-label="排序" type="number" min="0" placeholder="排序" value={form.sortOrder}
                  onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} /></label>
                <label>状态<select aria-label="状态" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
                  <option value="active">启用</option>
                  <option value="disabled">停用</option>
                </select></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "种类编码", value: form.typeCode.trim() || "未填写" },
          { label: "显示名", value: form.typeName.trim() || "未填写" },
          { label: "排序", value: String(Number(form.sortOrder) || 100) },
          { label: "状态", value: form.status === "active" ? "启用" : "停用" },
        ]}
      />
    </section>
  );
}
