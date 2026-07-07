import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, BarVenueRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
import { PermissionButton } from "../security/Can";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

/** 分 → 元展示（两位小数）；空显示占位。 */
function yuan(cent: number | null | undefined): string {
  if (cent === null || cent === undefined) return "—";
  return (cent / 100).toFixed(2);
}

/** 元输入 → 分（四舍五入到整数分）；空串返回 null。 */
function toCent(input: string): number | null {
  const trimmed = input.trim();
  if (trimmed === "") return null;
  const value = Number(trimmed);
  if (!Number.isFinite(value)) return null;
  return Math.round(value * 100);
}

type VenueForm = {
  barName: string;
  barCode: string;
  projectId: string;
  merchantId: string;
  spaceId: string;
  minimumSpendYuan: string;
};
const EMPTY_FORM: VenueForm = { barName: "", barCode: "", projectId: "", merchantId: "", spaceId: "", minimumSpendYuan: "" };

/**
 * 酒吧门店管理页（ADM-BAR-007，V263）：酒吧=独立门店实体，挂项目（综合体），可选绑经营商户与所在空间。
 * ① 列表：名称/项目/商户/空间/低消/状态/存档态；② 引导式「＋新建酒吧」（门店信息→归属→确认）；
 * ③ 存档/删除/审批操作列（照 BarProductsPage 范式）。
 * 建出的门店供各处（卡座/订台/开台、商品/分类、套餐）的「酒吧」下拉选择。
 */
export function BarVenuesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<BarVenueRow[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
  const [form, setForm] = React.useState<VenueForm>(EMPTY_FORM);
  const [formError, setFormError] = React.useState<string | null>(null);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [showArchived, setShowArchived] = React.useState(false);
  const [createOpen, setCreateOpen] = React.useState(false);

  const projectName = React.useCallback(
    (id: string | null | undefined): string => {
      if (!id) return "—";
      return projects.find((p) => p.id === id)?.name ?? id;
    },
    [projects],
  );
  const merchantName = React.useCallback(
    (id: string | null | undefined): string => {
      if (!id) return "—";
      return merchants.find((m) => m.id === id)?.merchantName ?? id;
    },
    [merchants],
  );
  const spaceName = React.useCallback(
    (id: string | null | undefined): string => {
      if (!id) return "—";
      return spaces.find((s) => s.id === id)?.spaceName ?? id;
    },
    [spaces],
  );

  function archiveLabel(row: BarVenueRow): string {
    if (row.deleteStatus === "pending") return "待审批删除";
    if (row.archivedAt) return "已存档";
    return "生效中";
  }

  function openDetail(row: BarVenueRow) {
    setDetail({
      title: `门店明细 · ${row.barName ?? row.id}`,
      fields: [
        { label: "门店编号", value: row.id },
        { label: "门店名称", value: row.barName ?? "—" },
        { label: "门店编码", value: row.barCode ?? "—" },
        { label: "所属项目", value: projectName(row.projectId) },
        { label: "经营商户", value: merchantName(row.merchantId) },
        { label: "所在空间", value: spaceName(row.spaceId) },
        { label: "默认低消（元）", value: row.minimumSpendCent === null || row.minimumSpendCent === undefined ? "未设" : `¥${yuan(row.minimumSpendCent)}` },
        { label: "存档状态", value: archiveLabel(row) },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<BarVenueRow[]>(`/api/v1/bar/venues?includeArchived=${showArchived}`);
      setRows(items ?? []);
      // 归属下拉参考数据（失败不阻断门店列表，降级为只显示 id）。
      try { setProjects(await api.get<GovernanceProjectRow[]>("/api/v1/platform/projects") ?? []); } catch { setProjects([]); }
      try { setMerchants(await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants") ?? []); } catch { setMerchants([]); }
      try { setSpaces(await api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces") ?? []); } catch { setSpaces([]); }
    },
    { onError: setGlobalError, fallbackMessage: "酒吧门店列表读取失败" },
  );

  const firstArchivedToggle = React.useRef(true);
  React.useEffect(() => {
    if (firstArchivedToggle.current) { firstArchivedToggle.current = false; return; }
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showArchived]);

  function setField(key: keyof VenueForm, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit() {
    setFormError(null);
    const barName = form.barName.trim();
    const projectId = form.projectId.trim();
    if (!barName || !projectId) {
      setFormError("门店名称、所属项目必填");
      return;
    }
    const minimumSpendCent = toCent(form.minimumSpendYuan);
    if (form.minimumSpendYuan.trim() !== "" && (minimumSpendCent === null || minimumSpendCent < 0)) {
      setFormError("默认低消必须为非负数字（留空=不设）");
      return;
    }
    try {
      const created = await api.post<BarVenueRow>("/api/v1/bar/venues", {
        barName,
        barCode: form.barCode.trim() || null,
        projectId,
        merchantId: form.merchantId.trim() || null,
        spaceId: form.spaceId.trim() || null,
        minimumSpendCent,
        status: "active",
      });
      setForm(EMPTY_FORM);
      setCreateOpen(false);
      setActionResult({ action: "bar_venue_created", id: created?.id, barName });
      await reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "酒吧门店创建失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  async function archiveVenue(id: string, archive: boolean) {
    try {
      await api.post(`/api/v1/bar/venues/${id}/${archive ? "archive" : "unarchive"}`, {});
      setActionResult({ action: `bar_venue_${archive ? "archived" : "unarchived"}`, id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "存档操作失败");
    }
  }

  async function requestDeleteVenue(id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null) return;
    try {
      await api.post(`/api/v1/bar/venues/${id}/delete-request`, { reason });
      setActionResult({ action: "bar_venue_delete_requested", id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除申请失败");
    }
  }

  async function approveDeleteVenue(id: string, decision: "approved" | "rejected") {
    try {
      const decided = await api.post<{ deleted?: boolean }>(`/api/v1/bar/venues/${id}/delete-approve`, { decision });
      setActionResult({ action: `bar_venue_delete_${decision}`, id, deleted: decided?.deleted ?? false });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除审批失败");
    }
  }

  function venueActions(row: BarVenueRow) {
    if (row.deleteStatus === "pending") {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.approve"]} data-testid={`bar-venue-delete-approve-${row.id}`} onClick={() => void approveDeleteVenue(row.id, "approved")}>批准删除</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.approve"]} data-testid={`bar-venue-delete-reject-${row.id}`} onClick={() => void approveDeleteVenue(row.id, "rejected")}>驳回</PermissionButton>
        </>
      );
    }
    if (row.archivedAt) {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.manage"]} data-testid={`bar-venue-unarchive-${row.id}`} onClick={() => void archiveVenue(row.id, false)}>取消存档</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.manage"]} data-testid={`bar-venue-delete-request-${row.id}`} onClick={() => void requestDeleteVenue(row.id)}>申请删除</PermissionButton>
        </>
      );
    }
    return (
      <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.manage"]} data-testid={`bar-venue-archive-${row.id}`} onClick={() => void archiveVenue(row.id, true)}>存档</PermissionButton>
    );
  }

  return (
    <section data-testid="bar-venues-page">
      <AdminPageHeader
        title="酒吧门店管理"
        description="酒吧是独立门店实体，挂项目（综合体），可选绑经营商户与所在空间。桌台、商品、分类、套餐等均归属到具体酒吧门店。此处新建/管理酒吧门店，供各处的「酒吧」下拉选择。"
      />

      <div className="admin-form" data-testid="bar-venue-create-form">
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.venue.manage"]} data-testid="bar-venue-create-open" onClick={() => { setFormError(null); setCreateOpen(true); }}>
            ＋ 新建酒吧
          </PermissionButton>
        </div>
        <GuidedFlowModal
          open={createOpen}
          title="新建酒吧"
          testId="bar-venue-guided"
          submitTestId="bar-venue-submit"
          submitLabel="创建门店"
          error={formError ?? undefined}
          onClose={() => setCreateOpen(false)}
          onSubmit={() => void submit()}
          steps={[
            {
              title: "门店信息",
              description: "填写门店名称（必填）与门店编码（可选）。",
              validate: () => (form.barName.trim() ? null : "门店名称必填"),
              content: (
                <>
                  <label><span>门店名称</span><input data-testid="bar-venue-name" value={form.barName} onChange={(e) => setField("barName", e.target.value)} /></label>
                  <label><span>门店编码（可选）</span><input data-testid="bar-venue-code" value={form.barCode} onChange={(e) => setField("barCode", e.target.value)} /></label>
                </>
              ),
            },
            {
              title: "归属",
              description: "所属项目必填；经营商户、所在空间、默认低消可选。",
              validate: () => (form.projectId.trim() ? null : "所属项目必填"),
              content: (
                <>
                  <label><span>所属项目</span>
                    <select data-testid="bar-venue-project" value={form.projectId} onChange={(e) => setField("projectId", e.target.value)}>
                      <option value="">请选择项目</option>
                      {projects.map((p) => (<option key={p.id} value={p.id}>{p.name ?? p.id}</option>))}
                    </select>
                  </label>
                  <label><span>经营商户（可选）</span>
                    <select data-testid="bar-venue-merchant" value={form.merchantId} onChange={(e) => setField("merchantId", e.target.value)}>
                      <option value="">不绑定</option>
                      {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName ?? m.id}</option>))}
                    </select>
                  </label>
                  <label><span>所在空间（可选）</span>
                    <select data-testid="bar-venue-space" value={form.spaceId} onChange={(e) => setField("spaceId", e.target.value)}>
                      <option value="">不绑定</option>
                      {spaces.map((s) => (<option key={s.id} value={s.id}>{s.spaceName ?? s.id}</option>))}
                    </select>
                  </label>
                  <label><span>默认低消（元，可空）</span><input data-testid="bar-venue-minimum-spend" inputMode="decimal" value={form.minimumSpendYuan} onChange={(e) => setField("minimumSpendYuan", e.target.value)} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "门店名称", value: form.barName.trim() || "未填写" },
            { label: "门店编码", value: form.barCode.trim() || "未填写" },
            { label: "所属项目", value: projectName(form.projectId.trim() || undefined) },
            { label: "经营商户", value: form.merchantId.trim() ? merchantName(form.merchantId.trim()) : "不绑定" },
            { label: "所在空间", value: form.spaceId.trim() ? spaceName(form.spaceId.trim()) : "不绑定" },
            { label: "默认低消（元）", value: form.minimumSpendYuan.trim() || "不设" },
          ]}
        />
      </div>

      <label className="admin-inline-check" data-testid="bar-venue-show-archived">
        <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
        显示已存档门店
      </label>

      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-venues-table">
            <thead>
              <tr>
                <th>门店</th>
                <th>项目</th>
                <th>商户</th>
                <th>空间</th>
                <th>默认低消（元）</th>
                <th>状态</th>
                <th>存档状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-venue-row-${row.id}`}
                  onClick={() => openDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(row); } }}>
                  <td>{row.barName ?? row.id}<br /><small>{row.id}</small></td>
                  <td>{projectName(row.projectId)}</td>
                  <td>{merchantName(row.merchantId)}</td>
                  <td>{spaceName(row.spaceId)}</td>
                  <td>{yuan(row.minimumSpendCent)}</td>
                  <td>{row.status === "active" ? "启用" : (row.status ?? "—")}</td>
                  <td data-testid={`bar-venue-archive-label-${row.id}`}>{archiveLabel(row)}</td>
                  <td onClick={(e) => e.stopPropagation()}>{venueActions(row)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
