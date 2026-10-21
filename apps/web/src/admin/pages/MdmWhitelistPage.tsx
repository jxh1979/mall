// 文件版本：V0.01
import React from "react";
import { Tabs } from "antd";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type Kind = "app" | "site";
type WhitelistRow = {
  id: string;
  packageName?: string;
  appLabel?: string | null;
  domain?: string;
  description?: string | null;
  scopeType: string;
  scopeId: string;
  enabled: boolean;
  reason?: string | null;
  updatedAt?: string;
};
type Form = { id: string | null; key: string; note: string; scopeType: string; scopeId: string; enabled: boolean; reason: string };

const BASE = "/api/v1/terminals/mdm";
const MANAGE = ["mdm.policy.manage"];
// 适用范围闭集照后端 MdmService.SCOPE_TYPES。
const SCOPE_LABELS: Record<string, string> = { global: "全局", project: "综合体", device_model: "机型" };
const KINDS = {
  app: { path: "app-whitelist", idPrefix: "mdm_app_", title: "预装软件", keyField: "packageName", keyLabel: "应用包名", keyTestId: "mdm-app-package-name", noteField: "appLabel", noteLabel: "应用名称", noteTestId: "mdm-app-label" },
  site: { path: "site-whitelist", idPrefix: "mdm_site_", title: "网站白名单", keyField: "domain", keyLabel: "网站域名", keyTestId: "mdm-site-domain", noteField: "description", noteLabel: "说明", noteTestId: "mdm-site-description" },
} as const;

const EMPTY_FORM: Form = { id: null, key: "", note: "", scopeType: "global", scopeId: "", enabled: true, reason: "" };

/**
 * 工作手机白名单（ADM-TER-003）：预装软件清单与网站白名单由后台维护（PGM「工作手机预装软件清单和网站白名单由后台维护」）。
 * 新增、编辑、停用都走同一个 PUT；手机端的拦截由企业级手机统一管控执行，未接入前清单只在平台保存与比对。
 */
export function MdmWhitelistPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<Record<Kind, WhitelistRow[]>>({ app: [], site: [] });
  const [editing, setEditing] = React.useState<Kind | null>(null);
  const [form, setForm] = React.useState<Form>(EMPTY_FORM);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [apps, sites] = await Promise.all([
        api.get<WhitelistRow[]>(`${BASE}/app-whitelist`),
        api.get<WhitelistRow[]>(`${BASE}/site-whitelist`),
      ]);
      setRows({ app: apps ?? [], site: sites ?? [] });
    },
    { onError: setGlobalError, fallbackMessage: "工作手机白名单读取失败" },
  );

  function bodyOf(kind: Kind, value: { key: string; note: string | null | undefined; scopeType: string; scopeId: string; enabled: boolean; reason: string | null | undefined }) {
    const config = KINDS[kind];
    return {
      [config.keyField]: value.key,
      [config.noteField]: value.note || undefined,
      scopeType: value.scopeType,
      scopeId: value.scopeType === "global" ? undefined : value.scopeId,
      enabled: value.enabled,
      reason: value.reason || undefined,
    };
  }

  async function put(kind: Kind, id: string, body: Record<string, unknown>) {
    const saved = await api.put<WhitelistRow>(`${BASE}/${KINDS[kind].path}/${id}`, body);
    setActionResult({ action: "mdm_whitelist_saved", kind, id, enabled: saved?.enabled });
    await reload();
  }

  function openEditor(kind: Kind, row?: WhitelistRow) {
    const config = KINDS[kind];
    setForm(row
      ? { id: row.id, key: String(row[config.keyField] ?? ""), note: String(row[config.noteField] ?? ""), scopeType: row.scopeType, scopeId: row.scopeType === "global" ? "" : row.scopeId, enabled: row.enabled, reason: row.reason ?? "" }
      : EMPTY_FORM);
    setFormError("");
    setEditing(kind);
  }

  async function save() {
    if (!editing) return;
    setSubmitting(true);
    try {
      const id = form.id ?? `${KINDS[editing].idPrefix}${crypto.randomUUID().replace(/-/g, "")}`;
      await put(editing, id, bodyOf(editing, { ...form, key: form.key.trim(), scopeId: form.scopeId.trim() }));
      setEditing(null);
    } catch (saveError) {
      setFormError(saveError instanceof Error ? saveError.message : "白名单保存失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function toggle(kind: Kind, row: WhitelistRow) {
    const config = KINDS[kind];
    try {
      await put(kind, row.id, bodyOf(kind, { key: String(row[config.keyField] ?? ""), note: row[config.noteField], scopeType: row.scopeType, scopeId: row.scopeId, enabled: !row.enabled, reason: row.reason }));
    } catch (toggleError) {
      setGlobalError(toggleError instanceof Error ? toggleError.message : "白名单启停失败");
    }
  }

  function table(kind: Kind) {
    const config = KINDS[kind];
    return (
      <>
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`mdm-${kind}-create-open`} onClick={() => openEditor(kind)}>＋ 新增{config.title}</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid={`mdm-${kind}-whitelist-table`}>
            <thead><tr><th>{config.keyLabel}</th><th>{config.noteLabel}</th><th>适用范围</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
            <tbody>{rows[kind].map((row) => (
              <tr key={row.id} data-testid={`mdm-${kind}-row-${row.id}`}>
                <td>{row[config.keyField]}</td>
                <td>{row[config.noteField] || "—"}</td>
                <td>{SCOPE_LABELS[row.scopeType] ?? row.scopeType}{row.scopeType === "global" ? "" : ` · ${row.scopeId}`}</td>
                <td>{row.enabled ? "启用" : "停用"}</td>
                <td>{row.updatedAt ? new Date(row.updatedAt).toLocaleString("zh-CN") : "—"}</td>
                <td>
                  <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`mdm-${kind}-edit-${row.id}`} onClick={() => openEditor(kind, row)}>编辑</PermissionButton>
                  <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`mdm-${kind}-toggle-${row.id}`} onClick={() => void toggle(kind, row)}>{row.enabled ? "停用" : "启用"}</PermissionButton>
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </>
    );
  }

  const config = editing ? KINDS[editing] : null;
  return (
    <section data-testid="mdm-whitelist-page">
      <AdminPageHeader title="工作手机白名单" description="受管工作手机只允许安装预装软件、只允许访问白名单网站；清单在此维护，由企业级手机统一管控拦截违规并留痕。" />
      <AdminDataState loading={loading} error={error} onRetry={reload}>
        <Tabs
          items={[
            { key: "app", label: KINDS.app.title, forceRender: true, children: table("app") },
            { key: "site", label: KINDS.site.title, forceRender: true, children: table("site") },
          ]}
        />
      </AdminDataState>
      {config && editing ? (
        <GuidedFlowModal
          open
          title={`${form.id ? "编辑" : "新增"}${config.title}`}
          testId={`mdm-${editing}-guided`}
          submitTestId={`mdm-${editing}-save`}
          submitLabel="保存"
          submitting={submitting}
          error={formError || undefined}
          onClose={() => setEditing(null)}
          onSubmit={() => void save()}
          steps={[{
            title: config.title,
            validate: () => {
              if (!form.key.trim()) return `请填写${config.keyLabel}`;
              if (form.scopeType !== "global" && !form.scopeId.trim()) return `请填写适用的${SCOPE_LABELS[form.scopeType]}编号`;
              return null;
            },
            content: (
              <>
                <label>{config.keyLabel}<input data-testid={config.keyTestId} value={form.key} onChange={(event) => setForm({ ...form, key: event.target.value })} /></label>
                <label>{config.noteLabel}<input data-testid={config.noteTestId} value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} /></label>
                <label>适用范围
                  <select data-testid={`mdm-${editing}-scope-type`} value={form.scopeType} onChange={(event) => setForm({ ...form, scopeType: event.target.value })}>
                    {Object.entries(SCOPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                </label>
                {form.scopeType === "global" ? null : (
                  <label>{SCOPE_LABELS[form.scopeType]}编号<input data-testid={`mdm-${editing}-scope-id`} value={form.scopeId} onChange={(event) => setForm({ ...form, scopeId: event.target.value })} /></label>
                )}
                <label><input type="checkbox" data-testid={`mdm-${editing}-enabled`} checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} />启用</label>
                <label>变更说明（选填）<input data-testid={`mdm-${editing}-reason`} value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} /></label>
              </>
            ),
          }]}
          summary={[
            { label: config.keyLabel, value: form.key.trim() },
            { label: config.noteLabel, value: form.note.trim() || "—" },
            { label: "适用范围", value: `${SCOPE_LABELS[form.scopeType]}${form.scopeType === "global" ? "" : ` · ${form.scopeId.trim()}`}` },
            { label: "状态", value: form.enabled ? "启用" : "停用" },
          ]}
        />
      ) : null}
    </section>
  );
}
