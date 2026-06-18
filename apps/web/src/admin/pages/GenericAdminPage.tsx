import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import type { AdminPageProps, AdminRoute } from "../types";
import { genericAdminPageConfigs, type GenericAdminAction } from "../adminPageConfigs";

type GenericRow = Record<string, unknown>;

type LoadedGroup = {
  label: string;
  endpoint: string;
  rows: GenericRow[];
};

function asRows(value: unknown): GenericRow[] {
  if (Array.isArray(value)) return value.filter((item): item is GenericRow => Boolean(item) && typeof item === "object" && !Array.isArray(item));
  if (value && typeof value === "object") return [value as GenericRow];
  return [];
}

function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.map(displayValue).join(" / ") : "-";
  return JSON.stringify(value);
}

function resolveEndpoint(endpoint: string, action: GenericAdminAction, row: GenericRow | undefined) {
  return endpoint.replace(/\{[^}]+\}/g, () => {
    const key = action.rowIdKey ?? "id";
    const id = row?.[key] ?? row?.id;
    return encodeURIComponent(String(id ?? "missing-row"));
  });
}

export function GenericAdminPage({ route, api, setActionResult }: AdminPageProps & { route: AdminRoute }) {
  const config = genericAdminPageConfigs[route.pageId];
  const [groups, setGroups] = React.useState<LoadedGroup[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const fetchData = React.useCallback(async () => {
    if (!config || config.contractGap || config.dataSources.length === 0) {
      setGroups([]);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const loaded = await Promise.all(config.dataSources.map(async (source) => {
        const rows = asRows(await api.get<unknown>(source.endpoint)).map((row) => ({ ...row, __source: source.label }));
        return { label: source.label, endpoint: source.endpoint, rows };
      }));
      setGroups(loaded);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "读取失败");
      setGroups([]);
    } finally {
      setLoading(false);
    }
  }, [api, config]);

  React.useEffect(() => {
    void fetchData();
  }, [fetchData]);

  if (!config) return <div className="admin-state admin-error">页面配置缺失：{route.pageId}</div>;

  const rows = groups.flatMap((group) => group.rows);
  const firstRow = rows[0];

  async function runAction(action: GenericAdminAction) {
    if (action.kind === "local") {
      setActionResult({ action: action.label, ...(action.result ?? { status: "logged" }) });
      return;
    }
    if (!action.endpoint) return;
    if (action.requiresRow !== false && !firstRow) {
      setActionResult({ action: action.label, status: "blocked", reason: "当前没有可操作记录" });
      return;
    }
    const endpoint = resolveEndpoint(action.endpoint, action, firstRow);
    const method = action.method ?? "POST";
    const body = action.body ?? { operatorNote: "完整后台验收操作" };
    const result = method === "PATCH" ? await api.patch<unknown>(endpoint, body) : method === "PUT" ? await api.put<unknown>(endpoint, body) : await api.post<unknown>(endpoint, body);
    setActionResult({ action: action.label, result });
    await fetchData();
  }

  return (
    <section className="admin-generic-page" data-testid="admin-generic-page">
      <AdminPageHeader title={config.title} description={config.description} />
      <div className="admin-generic-toolbar">
        <div className="admin-generic-evidence" data-testid="admin-generic-evidence">接口证据：{config.evidence}</div>
        <div className="admin-generic-actions">
          {config.primaryAction ? <button type="button" data-testid={`generic-primary-action-${config.pageId}`} onClick={() => void runAction(config.primaryAction!)}>{config.primaryAction.label}</button> : null}
          {config.secondaryAction ? <button type="button" data-testid={`generic-secondary-action-${config.pageId}`} onClick={() => void runAction(config.secondaryAction!)}>{config.secondaryAction.label}</button> : null}
        </div>
      </div>
      <AuditTrailHint event={`${config.pageId}.read/action`} />
      {config.contractGap ? (
        <div className="admin-state admin-gap" data-testid="admin-contract-gap"><strong>正式接口缺口</strong><span>{config.contractGap}</span></div>
      ) : error ? (
        <div className="admin-state admin-error" data-testid="admin-generic-error"><strong>读取失败</strong><span>{error}</span><button type="button" onClick={() => void fetchData()}>重试</button></div>
      ) : loading ? (
        <div className="admin-state loading-state" data-testid="admin-generic-loading">加载中...</div>
      ) : rows.length === 0 ? (
        <div className="admin-state admin-empty" data-testid="admin-generic-empty">暂无数据</div>
      ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="admin-generic-table">
            <thead><tr>{config.columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead>
            <tbody>{rows.map((row, index) => <tr key={`${displayValue(row.id)}-${index}`}>{config.columns.map((column) => <td key={column.key}>{displayValue(row[column.key])}</td>)}</tr>)}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}
