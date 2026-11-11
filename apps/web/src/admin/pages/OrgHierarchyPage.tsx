// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { cnDate } from "../components/RowDetailDrawer";

// M087-A-01 / A-03（PGM-1.7.1「系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点」
// 「每个部门或业务线须预先设定其平行审批的最低起点层级」；PGM-1.10.1「HR 部门定每个人的层级/职位」）：
// 组织层级（直接上级）与平行审批线维护页。写入口与定岗同一门禁 access.role.assign。

type OrgSuperiorRow = { userId: string; superiorId?: string | null; lineCode?: string | null; updatedBy?: string | null; updatedAt?: string | null };
type ApprovalLineRow = { lineCode: string; lineName: string; minStartRank: number; entryUserId?: string | null; hrFinalApproverId?: string | null; status: string };
type UserRow = { id: string; username: string };

const MANAGE = ["access.role.assign"];

const TEXT = {
  title: "组织层级与审批线",
  description: "维护每名员工的直接上级（审批链按此逐级补全）与各部门/业务线的平行审批最低起点层级。直接上级不得是本人、不得成环；层级未维护的发起人，其单据审批链无法生成、无人可裁决。",
  evidence: "接口证据：GET/PUT /api/v1/access/org-hierarchy/{user_id}；GET/PUT /api/v1/access/approval-lines/{line_code}。",
  loadFailed: "组织层级读取失败",
  actionFailed: "组织层级操作失败",
  hierarchy: "直接上级",
  lines: "平行审批线",
  user: "员工账号",
  superior: "直接上级账号（留空 = 清除）",
  lineCode: "所属审批线（可空）",
  setSuperior: "保存直接上级",
  lineCodeInput: "审批线编码",
  lineName: "审批线名称",
  minStartRank: "最低起点层级（1-30）",
  entryUser: "起点人账号（可空）",
  status: "状态",
  hrApprover: "部门人力会签负责人（可空）",
  saveLine: "保存审批线",
  noHierarchy: "尚未维护任何直接上级",
  noLines: "尚未配置平行审批线",
  updatedAt: "更新时间",
  updatedBy: "更新人",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function zhStatus(status?: string | null) {
  return status === "inactive" ? "停用" : "启用";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  if (action === "org_superior_set") {
    const superior = String(request.superiorId ?? "").trim();
    return superior ? `已把 ${String(request.userId ?? "")} 的直接上级设为 ${superior}` : `已清除 ${String(request.userId ?? "")} 的直接上级`;
  }
  const line = result && typeof result === "object" && "lineCode" in result ? String((result as { lineCode?: unknown }).lineCode ?? "") : String(request.lineCode ?? "");
  return `已保存平行审批线 ${line}（最低起点层级 ${String(request.minStartRank ?? "")}）`;
}

export function OrgHierarchyPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<OrgSuperiorRow[]>([]);
  const [lines, setLines] = React.useState<ApprovalLineRow[]>([]);
  const [users, setUsers] = React.useState<UserRow[]>([]);
  const [superiorForm, setSuperiorForm] = React.useState({ userId: "", superiorId: "", lineCode: "", operatorNote: "" });
  const [lineForm, setLineForm] = React.useState({ lineCode: "", lineName: "", minStartRank: "1", entryUserId: "", hrFinalApproverId: "", status: "active" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [hierarchy, approvalLines, userRows] = await Promise.all([
        api.get<OrgSuperiorRow[]>("/api/v1/access/org-hierarchy?limit=500"),
        api.get<ApprovalLineRow[]>("/api/v1/access/approval-lines"),
        api.get<UserRow[]>("/api/v1/access/users"),
      ]);
      setRows(hierarchy ?? []);
      setLines(approvalLines ?? []);
      setUsers(userRows ?? []);
      const firstUser = (userRows ?? [])[0]?.id ?? "";
      setSuperiorForm((current) => (current.userId || !firstUser ? current : { ...current, userId: firstUser }));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function saveSuperior() {
    const userId = superiorForm.userId.trim();
    if (!userId) {
      setFormError("请选择员工账号");
      return;
    }
    if (superiorForm.superiorId.trim() === userId) {
      setFormError("直接上级不能是本人");
      return;
    }
    const request = {
      idempotencyKey: nextKey("org-superior"),
      userId,
      superiorId: superiorForm.superiorId.trim() || null,
      lineCode: superiorForm.lineCode.trim() || null,
      operatorNote: superiorForm.operatorNote.trim() || "后台维护直接上级",
    };
    await runAction("org_superior_set", request, () => api.put(`/api/v1/access/org-hierarchy/${encodeURIComponent(userId)}`, {
      idempotencyKey: request.idempotencyKey, superiorId: request.superiorId, lineCode: request.lineCode, operatorNote: request.operatorNote,
    }));
  }

  async function saveLine() {
    const code = lineForm.lineCode.trim();
    const rank = Number(lineForm.minStartRank);
    if (!code || !lineForm.lineName.trim()) {
      setFormError("请填写审批线编码与名称");
      return;
    }
    if (!Number.isInteger(rank) || rank < 1 || rank > 30) {
      setFormError("最低起点层级须为 1 到 30 的整数");
      return;
    }
    const request = {
      idempotencyKey: nextKey("approval-line"),
      lineCode: code,
      lineName: lineForm.lineName.trim(),
      minStartRank: rank,
      entryUserId: lineForm.entryUserId.trim() || null,
      hrFinalApproverId: lineForm.hrFinalApproverId.trim() || null,
      status: lineForm.status,
    };
    await runAction("approval_line_saved", request, () => api.put(`/api/v1/access/approval-lines/${encodeURIComponent(code)}`, {
      idempotencyKey: request.idempotencyKey, lineName: request.lineName, minStartRank: request.minStartRank,
      entryUserId: request.entryUserId, hrFinalApproverId: request.hrFinalApproverId, status: request.status,
    }));
  }

  function selectLine(row: ApprovalLineRow) {
    setLineForm({ lineCode: row.lineCode, lineName: row.lineName, minStartRank: String(row.minStartRank), entryUserId: row.entryUserId ?? "", hrFinalApproverId: row.hrFinalApproverId ?? "", status: row.status });
  }

  return (
    <section data-testid="org-hierarchy-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="org-hierarchy-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="access.org_hierarchy.superior_set / access.approval_line.saved" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>{TEXT.hierarchy}</h3>
        {rows.length === 0 ? <div className="admin-muted-action" data-testid="org-hierarchy-empty">{TEXT.noHierarchy}</div> : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="org-hierarchy-table">
              <thead><tr><th>{TEXT.user}</th><th>{TEXT.hierarchy}</th><th>审批线</th><th>{TEXT.updatedBy}</th><th>{TEXT.updatedAt}</th></tr></thead>
              <tbody>{rows.map((row) => (
                <tr key={row.userId} data-testid={`org-hierarchy-row-${row.userId}`}>
                  <td>{row.userId}</td><td>{row.superiorId ?? "-"}</td><td>{row.lineCode ?? "-"}</td><td>{row.updatedBy ?? "-"}</td><td>{row.updatedAt ? cnDate(row.updatedAt) : "-"}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        <div className="admin-inline-form" data-testid="org-hierarchy-form">
          <select aria-label={TEXT.user} data-testid="org-hierarchy-user" value={superiorForm.userId} onChange={(event) => setSuperiorForm({ ...superiorForm, userId: event.target.value })}>
            {users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}
          </select>
          <input aria-label={TEXT.superior} data-testid="org-hierarchy-superior" placeholder={TEXT.superior} value={superiorForm.superiorId} onChange={(event) => setSuperiorForm({ ...superiorForm, superiorId: event.target.value })} />
          <input aria-label={TEXT.lineCode} data-testid="org-hierarchy-line" placeholder={TEXT.lineCode} value={superiorForm.lineCode} onChange={(event) => setSuperiorForm({ ...superiorForm, lineCode: event.target.value })} />
          <input aria-label="操作备注" placeholder="操作备注" value={superiorForm.operatorNote} onChange={(event) => setSuperiorForm({ ...superiorForm, operatorNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busyAction !== ""} data-testid="org-hierarchy-save" onClick={() => void saveSuperior()}>{TEXT.setSuperior}</PermissionButton>
        </div>

        <h3>{TEXT.lines}</h3>
        <p>人事事项按员工所属部门的配置自动确定人力会签负责人；未配置、权限或范围不符时保持待处理，补齐后重新生成审批链。</p>
        {lines.length === 0 ? <div className="admin-muted-action" data-testid="approval-lines-empty">{TEXT.noLines}</div> : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="approval-lines-table">
              <thead><tr><th>{TEXT.lineCodeInput}</th><th>{TEXT.lineName}</th><th>{TEXT.minStartRank}</th><th>{TEXT.entryUser}</th><th>{TEXT.hrApprover}</th><th>{TEXT.status}</th><th></th></tr></thead>
              <tbody>{lines.map((row) => (
                <tr key={row.lineCode} data-testid={`approval-line-row-${row.lineCode}`}>
                  <td>{row.lineCode}</td><td>{row.lineName}</td><td>{row.minStartRank}</td><td>{row.entryUserId ?? "-"}</td><td>{row.hrFinalApproverId ?? "待配置"}</td><td>{zhStatus(row.status)}</td>
                  <td><button type="button" data-testid={`approval-line-select-${row.lineCode}`} onClick={() => selectLine(row)}>编辑</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        <div className="admin-inline-form" data-testid="approval-line-form">
          <input aria-label={TEXT.lineCodeInput} data-testid="approval-line-code" placeholder={TEXT.lineCodeInput} value={lineForm.lineCode} onChange={(event) => setLineForm({ ...lineForm, lineCode: event.target.value })} />
          <input aria-label={TEXT.lineName} data-testid="approval-line-name" placeholder={TEXT.lineName} value={lineForm.lineName} onChange={(event) => setLineForm({ ...lineForm, lineName: event.target.value })} />
          <input aria-label={TEXT.minStartRank} data-testid="approval-line-rank" type="number" min="1" max="30" value={lineForm.minStartRank} onChange={(event) => setLineForm({ ...lineForm, minStartRank: event.target.value })} />
          <input aria-label={TEXT.entryUser} data-testid="approval-line-entry" placeholder={TEXT.entryUser} value={lineForm.entryUserId} onChange={(event) => setLineForm({ ...lineForm, entryUserId: event.target.value })} />
          <input aria-label={TEXT.hrApprover} data-testid="approval-line-hr" placeholder={TEXT.hrApprover} value={lineForm.hrFinalApproverId} onChange={(event) => setLineForm({ ...lineForm, hrFinalApproverId: event.target.value })} />
          <select aria-label={TEXT.status} data-testid="approval-line-status" value={lineForm.status} onChange={(event) => setLineForm({ ...lineForm, status: event.target.value })}><option value="active">启用</option><option value="inactive">停用</option></select>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busyAction !== ""} data-testid="approval-line-save" onClick={() => void saveLine()}>{TEXT.saveLine}</PermissionButton>
        </div>
        {formError ? <div data-testid="org-hierarchy-error" className="admin-inline-error">{formError}</div> : null}
      </AdminDataState>
    </section>
  );
}
