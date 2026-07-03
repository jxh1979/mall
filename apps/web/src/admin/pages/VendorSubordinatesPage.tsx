import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";

type SubordinateRow = { id: string; username: string; partnerOrgId: string; accountType: string; status: string };
type CompletionRow = { subordinateId: string; username: string; status: string; workOrderTotal: number; workOrderCompleted: number; inspectionTotal: number; inspectionCompleted: number };

const MANAGE = ["partner.subordinate.manage"];

/**
 * 外包下属账号自助门户（ADM-VND-004，条目2）：机构管理员在平台设定的下属上限内自助开通/停用本机构下属账号。
 * 下属继承机构被授权范围；机构停用/过期或超上限时开通被拒（后端 PartnerService 强制）。
 */
export function VendorSubordinatesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SubordinateRow[]>([]);
  const [completion, setCompletion] = React.useState<CompletionRow[]>([]);
  const [username, setUsername] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<SubordinateRow[]>("/api/v1/partner/subordinates");
      setRows(items ?? []);
      const done = await api.get<CompletionRow[]>("/api/v1/partner/subordinates/completion");
      setCompletion(done ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "下属账号读取失败" },
  );

  async function addSubordinate() {
    const name = username.trim();
    if (!name) { setGlobalError("请填写下属账号登录名"); return; }
    await api.post("/api/v1/partner/subordinates", { idempotencyKey: `partner-sub-${name}`, username: name });
    setActionResult({ action: "partner_subordinate_created", username: name });
    setUsername("");
    reload();
  }

  async function disableSubordinate(id: string) {
    await api.post(`/api/v1/partner/subordinates/${id}/disable`, {});
    setActionResult({ action: "partner_subordinate_disabled", userId: id });
    reload();
  }

  return (
    <section data-testid="vendor-subordinates-page">
      <AdminPageHeader
        title="外包下属账号"
        description="机构管理员在平台设定的下属上限内自助开通/停用本机构下属账号、查看其状态。下属继承机构被授权范围；机构停用或过期、超上限时开通被拒。仅 partner.subordinate.manage 可改。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-subordinates-table">
            <thead><tr><th>账号</th><th>登录名</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {rows.length === 0 ? (
                <tr><td colSpan={4} className="admin-empty" data-testid="vendor-subordinates-empty">暂无下属账号</td></tr>
              ) : rows.map((row) => (
                <tr key={row.id} data-testid={`vendor-subordinate-${row.id}`}>
                  <td>{row.id}</td><td>{row.username}</td><td>{formatBusinessStatus(row.status)}</td>
                  <td>{row.status === "active" && (
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                      data-testid={`vendor-subordinate-disable-${row.id}`} onClick={() => void disableSubordinate(row.id)}>停用</PermissionButton>
                  )}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="vendor-subordinates-form">
          <input aria-label="下属登录名" placeholder="下属登录名" value={username}
            onChange={(event) => setUsername(event.target.value)} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="vendor-subordinate-add" onClick={() => void addSubordinate()}>开通下属</PermissionButton>
        </div>
        <h3>下属工作完成情况</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-subordinate-completion-table">
            <thead><tr><th>登录名</th><th>状态</th><th>工单(完成/总)</th><th>巡检(完成/总)</th></tr></thead>
            <tbody>
              {completion.length === 0 ? (
                <tr><td colSpan={4} className="admin-empty" data-testid="vendor-subordinate-completion-empty">暂无数据</td></tr>
              ) : completion.map((row) => (
                <tr key={row.subordinateId} data-testid={`vendor-subordinate-completion-${row.subordinateId}`}>
                  <td>{row.username}</td><td>{formatBusinessStatus(row.status)}</td>
                  <td>{row.workOrderCompleted}/{row.workOrderTotal}</td>
                  <td>{row.inspectionCompleted}/{row.inspectionTotal}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
