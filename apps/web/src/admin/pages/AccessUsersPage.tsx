import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import type { AccessUserRow, AdminPageProps, PermissionTemplateRow } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

function templateLabel(template: PermissionTemplateRow) {
  return template.templateName ?? template.name ?? template.id;
}

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

export function AccessUsersPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [rows, setRows] = React.useState<AccessUserRow[]>([]);
  const [templates, setTemplates] = React.useState<PermissionTemplateRow[]>([]);
  const [templateNotice, setTemplateNotice] = React.useState<string | null>(null);
  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [phone, setPhone] = React.useState("");
  const [accountType, setAccountType] = React.useState("staff");
  const [consentReference, setConsentReference] = React.useState("");
  const [evidenceAttachmentIds, setEvidenceAttachmentIds] = React.useState("");
  const [organizationId, setOrganizationId] = React.useState("org-1");
  const [merchantId, setMerchantId] = React.useState("");
  const [merchants, setMerchants] = React.useState<Array<{ id: string; displayName?: string; companyName?: string }>>([]);
  // AUD-033: 账号绑外部协作机构(此前 partner_org_id 无任何后台入口,顶层外包账号绑不到机构)。
  const [partnerOrgs, setPartnerOrgs] = React.useState<Array<{ id: string; orgName?: string; status?: string }>>([]);
  const [bindUserId, setBindUserId] = React.useState("");
  const [bindOrgId, setBindOrgId] = React.useState("");
  const [bindBusy, setBindBusy] = React.useState(false);
  const [status, setStatus] = React.useState("active");
  const [selectedTemplateId, setSelectedTemplateId] = React.useState("");
  // 组织 / 商户显名字典（页面级拉取，失败降级回 ID）。
  const [orgNames, setOrgNames] = React.useState<Record<string, string>>({});
  const [merchantNames, setMerchantNames] = React.useState<Record<string, string>>({});
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  // 引导式创建流（一期范式）：新建账号收起为主按钮 → 分步弹窗 → 确认创建。
  const [createOpen, setCreateOpen] = React.useState(false);
  // 行明细抽屉：整行可点 → 该员工账号完整字段（账号类型/状态中文·组织商户显名）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：权限模板仅供创建账号时套用（下拉选项），可能因权限/500 单独失败——
      // 降级为空（下拉仅剩「暂不赋权」）+ 区块提示，不拖垮员工账号主列表。
      // 原第三个 /access/permissions 拉取结果从未被使用却会拖垮整页（一失败即整页白屏），已删除。
      const [users, templatesRes] = await Promise.all([
        api.get<AccessUserRow[]>("/api/v1/access/users"),
        loadSecondary(() => api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"), [] as PermissionTemplateRow[]),
      ]);
      setRows(users);
      setTemplates(templatesRes.data);
      setTemplateNotice(templatesRes.notice);
      setSelectedTemplateId((current) => current || templatesRes.data[0]?.id || "");
      // 显名字典拉取失败不阻断主流程，直接降级显示 ID。
      try {
        const [entities, merchants] = await Promise.all([
          api.get<Array<{ id: string; name?: string; code?: string }>>("/api/v1/platform/business-entities"),
          api.get<Array<{ id: string; displayName?: string; companyName?: string }>>("/api/v1/platform/merchants"),
          api.get<Array<{ id: string; orgName?: string; status?: string }>>("/api/v1/partner/orgs").then(setPartnerOrgs).catch(() => setPartnerOrgs([])),
        ]);
        setOrgNames(Object.fromEntries((entities ?? []).filter((e) => e.name || e.code).map((e) => [e.id, (e.name ?? e.code) as string])));
        setMerchantNames(Object.fromEntries((merchants ?? []).filter((m) => m.displayName || m.companyName).map((m) => [m.id, (m.displayName ?? m.companyName) as string])));
        setMerchants(merchants ?? []);
      } catch {
        /* 无权限或接口失败 → 保持空字典，表格回退 ID */
      }
    },
    { onError: setGlobalError, fallbackMessage: "账号数据读取失败", onStart: () => setGlobalError(null) },
  );

  function validateForm() {
    if (!username.trim()) {
      setFormError("员工账号不能为空。");
      return false;
    }
    if (!password.trim()) {
      setFormError("初始密码不能为空；后台手动建号必须能完成登录闭环。 ");
      return false;
    }
    if (phone.trim() && !/^1\d{10}$/.test(phone.trim())) {
      setFormError("手机号必须是 11 位大陆手机号；不实名场景可留空。 ");
      return false;
    }
    if (!organizationId.trim()) {
      setFormError("组织 ID 不能为空，用于数据范围和留痕。 ");
      return false;
    }
    setFormError("");
    return true;
  }

  async function createUser() {
    if (!validateForm()) return;
    setGlobalError(null);
    setSubmitting(true);
    try {
      const created = await api.post<AccessUserRow>("/api/v1/access/users", {
        username: username.trim(),
        password: password.trim(),
        phone: phone.trim() || undefined,
        accountType,
        status,
        organizationId: organizationId.trim(),
        merchantId: merchantId.trim() || undefined,
        consentReference: consentReference.trim() || undefined,
        evidenceAttachmentIds: evidenceAttachmentIds.split(/[,，\n]/).map((item) => item.trim()).filter(Boolean),
        operatorNote: "后台手动录入工作人员",
      });
      let appliedTemplate: unknown = null;
      if (selectedTemplateId) {
        appliedTemplate = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selectedTemplateId)}/apply`, {
          targetUserIds: [created.id],
          mode: "append",
          operatorNote: "后台手动录入后应用权限模板",
        });
      }
      const enriched = { ...created, phone: phone.trim() || created.phone, organizationId: organizationId.trim(), merchantId: merchantId.trim() || created.merchantId, status, accountType };
      setRows((current) => [...current, enriched]);
      setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", username: created.username, accountType, templateId: selectedTemplateId || null, appliedTemplate });
      setUsername("");
      setPassword("");
      setPhone("");
      setConsentReference("");
      setEvidenceAttachmentIds("");
      setCreateOpen(false);
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "创建员工失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 下钻定位：从工单/巡检页点处理人（?focus=员工账号id）→ 高亮并滚动其行；无 focus 时行为不变。
  React.useEffect(() => {
    if (!focusObjectId || rows.length === 0) return;
    if (!rows.some((row) => row.id === focusObjectId)) return;
    const el = typeof document !== "undefined" ? document.querySelector(`[data-testid="access-user-row-${focusObjectId}"]`) : null;
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusObjectId, rows.length]);

  function orgDisplay(id?: string | null) {
    return id ? (orgNames[id] ?? id) : "—";
  }
  function merchantDisplay(id?: string | null) {
    return id ? (merchantNames[id] ?? id) : "—";
  }

  function openUserDetail(row: AccessUserRow) {
    setDetail({
      title: `员工账号 · ${row.username}`,
      fields: [
        { label: "员工账号", value: row.username },
        { label: "手机号", value: row.phone ?? "—" },
        { label: "账号类型", value: formatBusinessStatus(row.accountType) },
        { label: "组织", value: orgDisplay(row.organizationId) },
        { label: "商户", value: merchantDisplay(row.merchantId) },
        { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
        { label: "开通方式", value: row.createdVia ? formatBusinessStatus(row.createdVia) : "—" },
        { label: "授权凭据", value: row.consentReference ?? "—" },
        { label: "证据附件", value: row.evidenceAttachmentIds?.length ? row.evidenceAttachmentIds.join("、") : "—" },
      ],
    });
  }

  async function bindPartnerOrg() {
    if (!bindUserId || !bindOrgId) {
      setFormError("请选择账号与外部协作机构。");
      return;
    }
    setFormError("");
    setBindBusy(true);
    try {
      await api.post(`/api/v1/partner/orgs/${encodeURIComponent(bindOrgId)}/bind-user`, {
        idempotency_key: crypto.randomUUID(),
        user_id: bindUserId,
      });
      setActionResult({ action: "partner_org_user_bound", userId: bindUserId, orgId: bindOrgId });
      setBindUserId("");
      setBindOrgId("");
    } catch (bindError) {
      setFormError(bindError instanceof Error ? bindError.message : "绑定失败");
    } finally {
      setBindBusy(false);
    }
  }

  // 2026-07-18 收尾：解绑机构（绑定的逆操作）；后端校验该账号当前绑的就是所选机构。
  async function unbindPartnerOrg() {
    if (!bindUserId || !bindOrgId) {
      setFormError("请选择账号与外部协作机构。");
      return;
    }
    setFormError("");
    setBindBusy(true);
    try {
      await api.post(`/api/v1/partner/orgs/${encodeURIComponent(bindOrgId)}/unbind-user`, {
        idempotency_key: crypto.randomUUID(),
        user_id: bindUserId,
      });
      setActionResult({ action: "partner_org_user_unbound", userId: bindUserId, orgId: bindOrgId });
      setBindUserId("");
      setBindOrgId("");
    } catch (unbindError) {
      setFormError(unbindError instanceof Error ? unbindError.message : "解绑失败");
    } finally {
      setBindBusy(false);
    }
  }

  return (
    <section data-testid="access-users-page">
      <AdminPageHeader title="员工账号" description="管理员工账号、岗位角色和企业微信同步结果；禁用、手动录入和分配权限模板必须留痕。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {/* 引导式流程一期：内联填表收起为主按钮，点击弹出分步引导（GuidedFlowModal）。 */}
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["access.user.manage"]} data-testid="access-user-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ 新建员工账号</PermissionButton>
        </div>
        {formError ? <div data-testid="access-users-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-inline-form" data-testid="access-partner-org-bind">
          <h3>账号绑定外部协作机构</h3>
          <p className="admin-preview">外包/审计等机构的顶层账号须绑定所属机构后,外包工作台才能按机构过滤其工单/巡检数据;换机构=重新绑定覆盖。</p>
          <select aria-label="选择账号" data-testid="partner-bind-user" value={bindUserId} onChange={(event) => setBindUserId(event.target.value)}>
            <option value="">选择账号</option>
            {rows.filter((u) => u.accountType === "outsourced_vendor").map((u) => (<option key={u.id} value={u.id}>{u.username}（{shortId(u.id)}）</option>))}
          </select>
          <select aria-label="选择外部协作机构" data-testid="partner-bind-org" value={bindOrgId} onChange={(event) => setBindOrgId(event.target.value)}>
            <option value="">选择机构</option>
            {partnerOrgs.filter((o) => o.status === "active").map((o) => (<option key={o.id} value={o.id}>{o.orgName ?? o.id}</option>))}
          </select>
          <PermissionButton permissionsState={effectivePermissions} permissions={["partner.org.manage"]} data-testid="partner-bind-submit" onClick={() => void bindPartnerOrg()} disabled={bindBusy}>
            绑定机构
          </PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["partner.org.manage"]} data-testid="partner-unbind-submit" onClick={() => void unbindPartnerOrg()} disabled={bindBusy}>
            解绑
          </PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-users-table">
            <thead><tr><th>员工账号</th><th>手机号</th><th>账号类型</th><th>组织</th><th>商户</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{rows.length === 0 ? (
              <tr><td colSpan={7} className="admin-muted-action">暂无员工账号，可通过上方表单手动录入并套用权限模板。</td></tr>
            ) : rows.map((row) => (
              <tr key={row.id} data-testid={`access-user-row-${row.id}`}
                className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`}
                tabIndex={0}
                onClick={() => openUserDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openUserDetail(row); } }}>
                <td>{row.username}</td>
                <td>{row.phone ?? "-"}</td>
                <td>{formatBusinessStatus(row.accountType)}</td>
                <td>{row.organizationId ? (orgNames[row.organizationId] ?? <span className="admin-id-cell" title={row.organizationId}>{shortId(row.organizationId)}</span>) : "-"}</td>
                <td>{row.merchantId ? (merchantNames[row.merchantId] ?? <span className="admin-id-cell" title={row.merchantId}>{shortId(row.merchantId)}</span>) : "-"}</td>
                <td>{formatBusinessStatus(row.status ?? "active")}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看员工账号详情" onClick={(event) => { event.stopPropagation(); openUserDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
      <GuidedFlowModal
        open={createOpen}
        title="新建员工账号"
        testId="access-user-flow"
        submitTestId="access-user-create"
        submitLabel="新增员工并赋权"
        submitting={submitting}
        error={formError}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void createUser()}
        steps={[
          {
            title: "账号信息",
            description: "填写登录账号、初始密码与账号类型。",
            validate: () => {
              if (!username.trim()) return "员工账号不能为空。";
              if (!password.trim()) return "初始密码不能为空；后台手动建号必须能完成登录闭环。";
              if (phone.trim() && !/^1\d{10}$/.test(phone.trim())) return "手机号必须是 11 位大陆手机号；不实名场景可留空。";
              return null;
            },
            content: (
              <>
                <label>员工账号<input data-testid="access-user-name" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="登录用账号名" /></label>
                <label>初始密码<input data-testid="access-user-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="首次登录使用" /></label>
                <label>手机号<input data-testid="access-user-phone" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="可选，后端仅留哈希" /></label>
                <label>账号类型<select data-testid="access-user-account-type" value={accountType} onChange={(event) => setAccountType(event.target.value)}>
                  <option value="staff">普通员工</option>
                  <option value="sales">销售人员</option>
                  <option value="supervisor">主管</option>
                  <option value="merchant_admin">商户管理员</option>
                  <option value="merchant">商户员工</option>
                </select></label>
              </>
            ),
          },
          {
            title: "归属与授权",
            description: "指定账号归属的组织/商户，并留存授权凭据。",
            validate: () => (organizationId.trim() ? null : "组织 ID 不能为空，用于数据范围和留痕。"),
            content: (
              <>
                <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织 ID" /></label>
                <label>所属商户<select data-testid="access-user-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)}><option value="">不绑定 / 平台账号</option>{merchants.map((m) => <option key={m.id} value={m.id}>{(m.displayName ?? m.companyName ?? m.id)}（{shortId(m.id)}）</option>)}</select></label>
                <label>授权凭据<input data-testid="access-user-consent" value={consentReference} onChange={(event) => setConsentReference(event.target.value)} placeholder="同意/授权凭据编号（可选）" /></label>
                <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件 ID，逗号分隔（可选）" /></label>
              </>
            ),
          },
          {
            title: "状态与权限",
            description: "设定初始状态，可同时套用权限模板完成赋权。",
            content: (
              <>
                <label>初始状态<select data-testid="access-user-status" value={status} onChange={(event) => setStatus(event.target.value)}>
                  <option value="active">启用</option>
                  <option value="invited">待激活</option>
                  <option value="disabled">禁用</option>
                </select></label>
                <label>权限模板<select data-testid="access-user-template" value={selectedTemplateId} onChange={(event) => setSelectedTemplateId(event.target.value)}>
                  <option value="">暂不赋权</option>
                  {templates.map((template) => <option key={template.id} value={template.id}>{templateLabel(template)}</option>)}
                </select></label>
                {templateNotice ? <div className="admin-state" data-testid="access-user-template-degraded">{templateNotice}</div> : null}
              </>
            ),
          },
        ]}
        summary={[
          { label: "员工账号", value: username },
          { label: "手机号", value: phone.trim() || "未填写" },
          { label: "账号类型", value: formatBusinessStatus(accountType) },
          { label: "所属组织", value: orgDisplay(organizationId.trim() || null) },
          { label: "所属商户", value: merchantDisplay(merchantId.trim() || null) },
          { label: "初始状态", value: formatBusinessStatus(status) },
          { label: "权限模板", value: (() => { const template = templates.find((t) => t.id === selectedTemplateId); return template ? templateLabel(template) : "暂不赋权"; })() },
          { label: "授权凭据", value: consentReference.trim() || "未填写" },
          { label: "证据附件", value: evidenceAttachmentIds.trim() || "未填写" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
