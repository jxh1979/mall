import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { adminRoutes } from "../adminRoutes";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AccessUserRow, AdminPageProps, PermissionDefinitionRow, PermissionTemplateRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

const TEMPLATE_MANAGE_PERMISSION = "access.permission_template.manage";
const TEMPLATE_APPLY_PERMISSION = "access.permission_template.apply";

// 模板类型枚举中文化（base/temporary/copy 不进全局表：base 全局义为“普通会员”会串味，页内小映射）。
const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板" };
function templateTypeLabel(type?: string | null) {
  const value = type ?? "base";
  return TEMPLATE_TYPE_LABELS[value] ?? value;
}
// 有效性口径（单次/限时/长期），抽屉与表格共用一处。
function validityLabel(row: PermissionTemplateRow) {
  return row.singleUse ? "单次有效" : row.validUntil ? "限时" : "长期";
}

type Mode = "existing" | "new";

function permissionCode(row: PermissionDefinitionRow) {
  return row.permissionCode ?? row.code ?? "";
}

function templatePermissionCodes(row: PermissionTemplateRow | undefined) {
  return row?.permissionCodes ?? row?.permissions ?? [];
}

function displayTemplateName(row: PermissionTemplateRow) {
  return row.templateName ?? row.name ?? row.templateCode ?? row.id;
}

function toDateTimeLocal(value?: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
  return date.toISOString().slice(0, 16);
}

function toIsoOrUndefined(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

function parseTargetUsers(value: string) {
  return value
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function PermissionTemplatesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<PermissionTemplateRow[]>([]);
  const [permissions, setPermissions] = React.useState<PermissionDefinitionRow[]>([]);
  const [users, setUsers] = React.useState<AccessUserRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [mode, setMode] = React.useState<Mode>("existing");
  const [templateName, setTemplateName] = React.useState("");
  const [templateType, setTemplateType] = React.useState("base");
  const [validFrom, setValidFrom] = React.useState("");
  const [validUntil, setValidUntil] = React.useState("");
  const [singleUse, setSingleUse] = React.useState(false);
  const [selectedPermissionCodes, setSelectedPermissionCodes] = React.useState<string[]>([]);
  const [targetUsersText, setTargetUsersText] = React.useState("user-1");
  const [copyName, setCopyName] = React.useState("普通员工01");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  // 引导式流程 phase-11：新建权限模板收起为主按钮 + 分步引导弹窗（选权限网格作为其中一步）。
  // 独立 state：与内联「选中编辑」表单完全隔离，避免选中行的 useEffect 回填污染新建字段。
  const [createOpen, setCreateOpen] = React.useState(false);
  const [createError, setCreateError] = React.useState("");
  const [createSaving, setCreateSaving] = React.useState(false);
  const [newName, setNewName] = React.useState("");
  const [newType, setNewType] = React.useState("base");
  const [newValidFrom, setNewValidFrom] = React.useState("");
  const [newValidUntil, setNewValidUntil] = React.useState("");
  const [newSingleUse, setNewSingleUse] = React.useState(false);
  const [newPermissionCodes, setNewPermissionCodes] = React.useState<string[]>([]);
  // 行明细抽屉：模板表整行可点 → 该模板完整字段（类型/状态/有效性中文·权限点列全）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const selected = mode === "existing" ? rows.find((row) => row.id === selectedId) : undefined;

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [templateRows, permissionRows, userRows] = await Promise.all([
        api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"),
        api.get<PermissionDefinitionRow[]>("/api/v1/access/permissions"),
        api.get<AccessUserRow[]>("/api/v1/access/users"),
      ]);
      setRows(templateRows);
      setPermissions(permissionRows.filter((item) => permissionCode(item)));
      setUsers(userRows);
      setSelectedId((current) => current || templateRows[0]?.id || "");
    },
    { onError: setGlobalError, fallbackMessage: "权限模板读取失败", onStart: () => setGlobalError(null) },
  );

  React.useEffect(() => {
    if (mode === "new") return;
    const next = rows.find((row) => row.id === selectedId) ?? rows[0];
    if (!next) return;
    if (next.id !== selectedId) setSelectedId(next.id);
    setTemplateName(displayTemplateName(next));
    setTemplateType(next.templateType ?? "base");
    setValidFrom(toDateTimeLocal(next.validFrom));
    setValidUntil(toDateTimeLocal(next.validUntil));
    setSingleUse(Boolean(next.singleUse));
    setSelectedPermissionCodes(templatePermissionCodes(next));
    setCopyName(`${displayTemplateName(next)}01`);
  }, [mode, rows, selectedId]);

  React.useEffect(() => {
    if (users.length > 0 && targetUsersText === "user-1") {
      setTargetUsersText(users[0]?.id ?? "user-1");
    }
  }, [targetUsersText, users]);

  const groupedPermissions = React.useMemo(() => {
    // 权限分组与左侧菜单对应:从 adminRoutes 派生「权限域→菜单模块」,并按菜单模块出现顺序排列,
    // 使管理员按左侧目录的思路勾选权限。未匹配任何菜单的域归入「其他」置末。
    const domainToModule = new Map<string, string>();
    const moduleOrder: string[] = [];
    for (const route of adminRoutes) {
      if (!moduleOrder.includes(route.module)) moduleOrder.push(route.module);
      for (const code of route.requiredPermissions ?? []) {
        const dom = code.split(".")[0];
        if (dom && !domainToModule.has(dom)) domainToModule.set(dom, route.module);
      }
    }
    const groups = new Map<string, PermissionDefinitionRow[]>();
    for (const row of permissions) {
      const domain = row.domain ?? permissionCode(row).split(".")[0] ?? "other";
      const moduleName = domainToModule.get(domain) ?? "其他";
      groups.set(moduleName, [...(groups.get(moduleName) ?? []), row]);
    }
    return [...moduleOrder, "其他"]
      .filter((m) => groups.has(m))
      .map((m) => [m, groups.get(m)!] as [string, PermissionDefinitionRow[]]);
  }, [permissions]);

  // 引导式「新建权限模板」入口：重置独立新建 state 并打开弹窗（不触碰内联编辑表单/选中态）。
  function openCreateTemplate() {
    setNewName("");
    setNewType("base");
    setNewValidFrom("");
    setNewValidUntil("");
    setNewSingleUse(false);
    setNewPermissionCodes([]);
    setCreateError("");
    setCreateOpen(true);
  }

  function toggleNewPermission(code: string, checked: boolean) {
    setNewPermissionCodes((current) => {
      const set = new Set(current);
      if (checked) set.add(code);
      else set.delete(code);
      return [...set].sort();
    });
  }

  function togglePermission(code: string, checked: boolean) {
    setSelectedPermissionCodes((current) => {
      const set = new Set(current);
      if (checked) set.add(code);
      else set.delete(code);
      return [...set].sort();
    });
  }

  function validateNewTemplate() {
    if (!newName.trim()) return "模板名称不能为空。";
    if (newPermissionCodes.length === 0) return "至少选择一个权限点。";
    if (newType === "temporary" && !newValidUntil) return "临时模板必须设置有效截止时间。";
    return null;
  }

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错（createError），成功才关。原接口/请求体/校验保留（POST 集合路径）。
  async function createTemplate() {
    const message = validateNewTemplate();
    if (message) { setCreateError(message); return; }
    setCreateSaving(true);
    setCreateError("");
    setGlobalError(null);
    try {
      const body = {
        templateName: newName.trim(),
        templateType: newType,
        permissionCodes: newPermissionCodes,
        dataScope: "project",
        validFrom: toIsoOrUndefined(newValidFrom),
        validUntil: toIsoOrUndefined(newValidUntil),
        singleUse: newSingleUse,
        operatorNote: newType === "temporary" ? "后台权限模板临时授权配置" : "后台权限模板配置",
      };
      const saved = await api.post<PermissionTemplateRow>("/api/v1/access/permission-templates", body);
      setActionResult({ action: "保存权限模板", templateId: saved.id, permissionCount: newPermissionCodes.length, singleUse: newSingleUse });
      // 新建成功后选中该模板，方便随后复制/应用（复制/应用动作依赖选中态）。
      setMode("existing");
      setSelectedId(saved.id);
      setCreateOpen(false);
      await reload();
    } catch (actionError) {
      const message2 = actionError instanceof Error ? actionError.message : "保存权限模板失败";
      setCreateError(message2);
      setGlobalError(message2);
      setActionResult({ action: "保存权限模板", status: "failed", message: message2 });
    } finally {
      setCreateSaving(false);
    }
  }

  function validateTemplateForm() {
    if (!templateName.trim()) {
      setFormError("模板名称不能为空。");
      return false;
    }
    if (selectedPermissionCodes.length === 0) {
      setFormError("至少选择一个权限点。");
      return false;
    }
    if (templateType === "temporary" && !validUntil) {
      setFormError("临时模板必须设置有效截止时间。");
      return false;
    }
    setFormError("");
    return true;
  }

  // 内联「选中编辑」保存：仅更新已选中的既有模板（PUT）；新建走引导弹窗 createTemplate。
  async function saveTemplate() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要编辑的模板；新建请点「＋ 新建权限模板」。");
      return;
    }
    if (!validateTemplateForm()) return;
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = {
        templateName: templateName.trim(),
        templateType,
        sourceTemplateId: selected?.sourceTemplateId ?? undefined,
        permissionCodes: selectedPermissionCodes,
        dataScope: "project",
        validFrom: toIsoOrUndefined(validFrom),
        validUntil: toIsoOrUndefined(validUntil),
        singleUse,
        operatorNote: templateType === "temporary" ? "后台权限模板临时授权配置" : "后台权限模板配置",
      };
      const saved = await api.put<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}`, body);
      setActionResult({ action: "保存权限模板", templateId: saved.id, permissionCount: selectedPermissionCodes.length, singleUse });
      setMode("existing");
      setSelectedId(saved.id);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "保存权限模板失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "保存权限模板", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  async function copyTemplate() {
    if (!selected?.id) {
      setFormError("请先在表格中选择基础模板。");
      return;
    }
    if (!copyName.trim()) {
      setFormError("复制模板名称不能为空。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    try {
      const copied = await api.post<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/copy`, {
        templateName: copyName.trim(),
        templateType: "copy",
        operatorNote: "后台复制权限模板",
      });
      setActionResult({ action: "复制模板", sourceTemplateId: selected.id, copied });
      setMode("existing");
      setSelectedId(copied.id);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "复制模板失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  function validateApply() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要应用的模板。");
      return false;
    }
    if (parseTargetUsers(targetUsersText).length === 0) {
      setFormError("至少输入一个目标员工 ID。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function applyTemplate() {
    if (!validateApply() || !selected?.id) return;
    setGlobalError(null);
    try {
      const applied = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/apply`, {
        targetUserIds: parseTargetUsers(targetUsersText),
        mode: singleUse ? "single_use" : "append",
        operatorNote: singleUse ? "后台应用单次有效权限模板" : "后台应用权限模板",
      });
      setActionResult({ action: singleUse ? "单次有效临时模板已应用" : "模板已应用", sourceTemplateId: selected.id, targetUserIds: parseTargetUsers(targetUsersText), applied });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "应用模板失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "应用权限模板", status: "failed", message });
    }
  }

  function openTemplateDetail(row: PermissionTemplateRow) {
    const codes = templatePermissionCodes(row);
    setDetail({
      title: `权限模板 · ${displayTemplateName(row)}`,
      fields: [
        { label: "模板名称", value: displayTemplateName(row) },
        { label: "类型", value: templateTypeLabel(row.templateType) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "有效性", value: validityLabel(row) },
        { label: "权限数", value: codes.length || "—" },
        { label: "权限点", value: codes.length ? codes.join("、") : "—" },
      ],
    });
  }

  return (
    <section data-testid="permission-templates-page">
      <AdminPageHeader title="权限模板" description="支持基础模板、复制模板、临时模板、时效和单次有效；复制不能影响原模板。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row" data-testid="permission-template-create-row">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            data-testid="permission-template-create-open"
            onClick={openCreateTemplate}
          >＋ 新建权限模板</PermissionButton>
          <span className="admin-muted-action">下方为「选中编辑」既有模板与复制/应用；新建请用此按钮。</span>
        </div>
        {/* 选中编辑既有模板（走 PUT）：字段 testid 加 -edit- 前缀，与引导弹窗内的新建字段区隔，避免同名撞号。 */}
        <div className="admin-form-row admin-permission-template-form">
          <label>模板名称
            <input data-testid="permission-template-edit-name" value={templateName} onChange={(event) => setTemplateName(event.target.value)} placeholder="例如：普通员工01" />
          </label>
          <label>模板类型
            <select data-testid="permission-template-edit-type" value={templateType} onChange={(event) => setTemplateType(event.target.value)}>
              <option value="base">基础模板</option>
              <option value="temporary">临时模板</option>
              <option value="copy">复制模板</option>
            </select>
          </label>
          <label>生效时间
            <input data-testid="permission-template-edit-valid-from" type="datetime-local" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} />
          </label>
          <label>截止时间
            <input data-testid="permission-template-edit-valid-until" type="datetime-local" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} />
          </label>
          <label className="admin-checkbox"><input data-testid="permission-template-edit-single-use" type="checkbox" checked={singleUse} onChange={(event) => setSingleUse(event.target.checked)} /> 单次有效</label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            disabled={submitting || !selected?.id}
            onClick={() => void saveTemplate()}
            data-testid="permission-template-update"
          >
            保存修改
          </PermissionButton>
          <span data-testid="permission-template-selected-count" className="admin-muted-action">已选 {selectedPermissionCodes.length} 个权限</span>
        </div>

        <div className="admin-form-row admin-permission-apply-row">
          <label>复制名称
            <input data-testid="permission-template-copy-name" value={copyName} onChange={(event) => setCopyName(event.target.value)} placeholder="复制后的模板名称" />
          </label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            disabled={!selected?.id}
            onClick={() => void copyTemplate()}
            data-testid="permission-template-copy"
          >
            复制模板
          </PermissionButton>
          <label>目标员工 ID
            <input data-testid="permission-template-target-users" value={targetUsersText} onChange={(event) => setTargetUsersText(event.target.value)} placeholder="多个用逗号分隔" />
          </label>
          <HighRiskConfirm
            label="应用模板"
            impact="将把选中模板的权限项应用到目标账号并落库；单次有效模式会影响目标账号实际可操作范围。"
            disabled={!selected?.id}
            precheck={validateApply}
            onConfirm={() => void applyTemplate()}
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_APPLY_PERMISSION]}
            data-testid="permission-template-apply"
          />
        </div>

        {formError ? <div className="admin-inline-error" data-testid="permission-templates-error">{formError}</div> : null}

        <div className="admin-permission-grid" data-testid="permission-template-permission-grid">
          {groupedPermissions.map(([moduleName, items]) => (
            <fieldset key={moduleName} className="admin-permission-domain">
              <legend>{moduleName}</legend>
              {items.map((item) => {
                const code = permissionCode(item);
                return (
                  <label key={code} className="admin-permission-option">
                    <input
                      data-testid={`permission-code-${code}`}
                      type="checkbox"
                      checked={selectedPermissionCodes.includes(code)}
                      onChange={(event) => togglePermission(code, event.target.checked)}
                    />
                    <span>{code}</span>
                    <small>{item.permissionName ?? item.name ?? item.riskLevel ?? "权限点"}</small>
                  </label>
                );
              })}
            </fieldset>
          ))}
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="permission-templates-table">
            <thead><tr><th>选择</th><th>模板名称</th><th>类型</th><th>状态</th><th>有效性</th><th>权限数</th><th>详情</th></tr></thead>
            <tbody>{rows.length === 0 ? (
              <tr><td colSpan={7} className="admin-muted-action">暂无权限模板，可先新建基础模板或临时模板。</td></tr>
            ) : rows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`permission-template-row-${row.id}`} onClick={() => openTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此权限模板 ${row.id}`} data-testid={`permission-template-select-${row.id}`} checked={mode === "existing" && selectedId === row.id} onChange={() => { setMode("existing"); setSelectedId(row.id); }} /></td>
                <td>{displayTemplateName(row)}</td>
                <td>{templateTypeLabel(row.templateType)}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td>{validityLabel(row)}</td>
                <td>{templatePermissionCodes(row).length || "-"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看权限模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={createOpen}
        title="新建权限模板"
        testId="permission-template-create-guided"
        submitTestId="permission-template-save"
        submitLabel="保存模板"
        submitting={createSaving}
        error={createError || undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void createTemplate()}
        steps={[
          {
            title: "模板信息",
            description: "模板名称必填；临时模板须设置有效截止时间；单次有效模板应用后一次即失效。",
            validate: () => {
              if (!newName.trim()) return "模板名称不能为空。";
              if (newType === "temporary" && !newValidUntil) return "临时模板必须设置有效截止时间。";
              return null;
            },
            content: (
              <>
                <label>模板名称<input data-testid="permission-template-name" value={newName} onChange={(event) => setNewName(event.target.value)} placeholder="例如：普通员工01" /></label>
                <label>模板类型
                  <select data-testid="permission-template-type" value={newType} onChange={(event) => setNewType(event.target.value)}>
                    <option value="base">基础模板</option>
                    <option value="temporary">临时模板</option>
                    <option value="copy">复制模板</option>
                  </select>
                </label>
                <label>生效时间<input data-testid="permission-template-valid-from" type="datetime-local" value={newValidFrom} onChange={(event) => setNewValidFrom(event.target.value)} /></label>
                <label>截止时间<input data-testid="permission-template-valid-until" type="datetime-local" value={newValidUntil} onChange={(event) => setNewValidUntil(event.target.value)} /></label>
                <label className="admin-checkbox"><input data-testid="permission-template-single-use" type="checkbox" checked={newSingleUse} onChange={(event) => setNewSingleUse(event.target.checked)} /> 单次有效</label>
              </>
            ),
          },
          {
            title: "选择权限",
            description: "按左侧菜单分组勾选权限点，至少选择一个。",
            validate: () => (newPermissionCodes.length === 0 ? "至少选择一个权限点。" : null),
            content: (
              <div className="admin-permission-grid" data-testid="permission-template-new-permission-grid">
                {groupedPermissions.map(([moduleName, items]) => (
                  <fieldset key={moduleName} className="admin-permission-domain">
                    <legend>{moduleName}</legend>
                    {items.map((item) => {
                      const code = permissionCode(item);
                      return (
                        <label key={code} className="admin-permission-option">
                          <input
                            data-testid={`permission-template-new-code-${code}`}
                            type="checkbox"
                            checked={newPermissionCodes.includes(code)}
                            onChange={(event) => toggleNewPermission(code, event.target.checked)}
                          />
                          <span>{code}</span>
                          <small>{item.permissionName ?? item.name ?? item.riskLevel ?? "权限点"}</small>
                        </label>
                      );
                    })}
                  </fieldset>
                ))}
              </div>
            ),
          },
        ]}
        summary={[
          { label: "模板名称", value: newName.trim() || "未填写" },
          { label: "类型", value: templateTypeLabel(newType) },
          { label: "生效时间", value: newValidFrom || "—" },
          { label: "截止时间", value: newValidUntil || "—" },
          { label: "单次有效", value: newSingleUse ? "是" : "否" },
          { label: "已选权限", value: newPermissionCodes.length ? `${newPermissionCodes.length} 个：${newPermissionCodes.join("、")}` : "未选择" },
        ]}
      />
    </section>
  );
}
