import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

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
  // 引导式「开通下属」弹窗状态（范式：字段留页面持有，弹窗只编排）。
  const [addOpen, setAddOpen] = React.useState(false);
  const [addError, setAddError] = React.useState("");
  const [addSaving, setAddSaving] = React.useState(false);
  // 行明细抽屉：整行可点 → 该下属账号/完成情况完整字段（状态中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openSubordinateDetail(row: SubordinateRow) {
    setDetail({
      title: `下属账号明细 · ${row.username}`,
      fields: [
        { label: "登录名", value: row.username },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "所属机构", value: row.partnerOrgId ?? "—" },
        { label: "账号类型", value: formatBusinessStatus(row.accountType) },
        { label: "账号编号", value: row.id },
      ],
    });
  }

  function openCompletionDetail(row: CompletionRow) {
    setDetail({
      title: `下属工作完成情况 · ${row.username}`,
      fields: [
        { label: "登录名", value: row.username },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "工单完成", value: `${row.workOrderCompleted}/${row.workOrderTotal}` },
        { label: "巡检完成", value: `${row.inspectionCompleted}/${row.inspectionTotal}` },
        { label: "账号编号", value: row.subordinateId },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<SubordinateRow[]>("/api/v1/partner/subordinates");
      setRows(items ?? []);
      const done = await api.get<CompletionRow[]>("/api/v1/partner/subordinates/completion");
      setCompletion(done ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "下属账号读取失败" },
  );

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体/双保险校验保留。
  async function addSubordinate() {
    const name = username.trim();
    if (!name) { setAddError("请填写下属账号登录名"); return; }
    setAddSaving(true);
    setAddError("");
    try {
      await api.post("/api/v1/partner/subordinates", { idempotencyKey: `partner-sub-${name}`, username: name });
      setActionResult({ action: "partner_subordinate_created", username: name });
      setUsername("");
      setAddOpen(false);
      reload();
    } catch (e) { setAddError((e as Error).message); } finally { setAddSaving(false); }
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
            <thead><tr><th>账号</th><th>登录名</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>
              {rows.length === 0 ? (
                <tr><td colSpan={5} className="admin-empty" data-testid="vendor-subordinates-empty">暂无下属账号</td></tr>
              ) : rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-subordinate-${row.id}`}
                  onClick={() => openSubordinateDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSubordinateDetail(row); } }}>
                  <td>{row.id}</td><td>{row.username}</td><td>{formatBusinessStatus(row.status)}</td>
                  <td onClick={(event) => event.stopPropagation()}>{row.status === "active" && (
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                      data-testid={`vendor-subordinate-disable-${row.id}`} onClick={() => void disableSubordinate(row.id)}>停用</PermissionButton>
                  )}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看下属账号详情" onClick={(event) => { event.stopPropagation(); openSubordinateDetail(row); }}>查看详情</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-form-row" data-testid="vendor-subordinates-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="vendor-subordinate-add-open"
            onClick={() => { setUsername(""); setAddError(""); setAddOpen(true); }}>＋ 开通下属</PermissionButton>
        </div>
        <h3>下属工作完成情况</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-subordinate-completion-table">
            <thead><tr><th>登录名</th><th>状态</th><th>工单(完成/总)</th><th>巡检(完成/总)</th><th>详情</th></tr></thead>
            <tbody>
              {completion.length === 0 ? (
                <tr><td colSpan={5} className="admin-empty" data-testid="vendor-subordinate-completion-empty">暂无数据</td></tr>
              ) : completion.map((row) => (
                <tr key={row.subordinateId} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-subordinate-completion-${row.subordinateId}`}
                  onClick={() => openCompletionDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCompletionDetail(row); } }}>
                  <td>{row.username}</td><td>{formatBusinessStatus(row.status)}</td>
                  <td>{row.workOrderCompleted}/{row.workOrderTotal}</td>
                  <td>{row.inspectionCompleted}/{row.inspectionTotal}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看下属工作完成情况详情" onClick={(event) => { event.stopPropagation(); openCompletionDetail(row); }}>查看详情</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <GuidedFlowModal
        open={addOpen}
        title="开通下属账号"
        testId="vendor-subordinate-add-guided"
        submitTestId="vendor-subordinate-add"
        submitLabel="开通下属"
        submitting={addSaving}
        error={addError || undefined}
        onClose={() => setAddOpen(false)}
        onSubmit={() => void addSubordinate()}
        steps={[
          {
            title: "下属信息",
            description: "填写下属登录名。下属继承机构被授权范围；机构停用/过期或超上限时开通会被拒。",
            validate: () => (username.trim() ? null : "请填写下属账号登录名"),
            content: (
              <label>下属登录名<input data-testid="vendor-subordinate-username" aria-label="下属登录名" placeholder="下属登录名" value={username} onChange={(event) => setUsername(event.target.value)} /></label>
            ),
          },
        ]}
        summary={[
          { label: "下属登录名", value: username.trim() || "未填写" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
