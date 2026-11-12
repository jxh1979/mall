// 文件版本：V0.03
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import type { AdminPageProps, GovernanceProjectRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 平台侧客人账号安全投影（后端 CustomerAccountView，unwrap 已 camel 化）。只有掩码手机号，明文不出网；解除黑名单后手机号已释放为空。
type CustomerAccountRow = {
  id: string;
  maskedPhone: string | null;
  status: string;
  suspendedUntil: string | null;
  createdAt: string;
};

type BlacklistRequestRow = {
  id: string;
  customerId: string;
  projectId: string;
  action: string;
  reason: string;
  status: string;
  requestedAt: string;
  nightManagerApprovedBy: string | null;
};

type BoundedSetting = { value: string; min: string; max: string };

const MANAGE = ["customer.account.manage"];
const REQUEST = ["customer.blacklist.request"];
const APPROVE = ["customer.blacklist.approve"];

// BSM 第 819 行 / ENUM 第 68 行「会员状态：有效、受限、已关闭、已注销四种」；列入黑名单即关闭账户。
const STATUS_LABELS: Record<string, string> = {
  active: "有效",
  suspended: "受限",
  blacklisted: "已关闭（黑名单）",
  deregistered: "已注销",
};
const ACTION_LABELS: Record<string, string> = { list: "列入黑名单", release: "解除黑名单" };

const key = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const emptySetting: BoundedSetting = { value: "", min: "", max: "" };
const bounded = (value: number, min: number | null, max: number | null): BoundedSetting =>
  ({ value: String(value), min: min == null ? "" : String(min), max: max == null ? "" : String(max) });

/**
 * 客人账号管理页（ADM-CUS-001）：按手机号精确查客人（盲索引，不提供全量浏览，PII 最小化）。
 * ROOT-03-R23（BSM 第 819 行、ENUM 第 68 行）：受限只停自助订台与员工代订，由客服主管填原因置入，截止按项目受限时长（出厂 90 天，值与上下限同配），
 * 可提前解除；黑名单由保安团队申请、申请所在项目当班夜班值班经理批准、娱乐平台负责人终审，两级都批准才关闭账户；解除同级复核，原账户保持已关闭、
 * 手机号可重新注册。ROOT-03-R22（ENUM 第 68 行）：同号一年内注销与注册合计超上限提示风控（全平台，出厂 3 次，值与上下限同配）。
 * 第十八批 AH 段 218-R2（BSM 第 800 行）：高频出入场预警阈值按项目配置，一晚、连续两日各带上下限（出厂 4 次、7 次），六值同配。
 */
export function CustomerAccountsPage(props: AdminPageProps) {
  const { api, setActionResult, setGlobalError, effectivePermissions, user, reportViewedObjects } = props;
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [project, setProject] = React.useState(user?.organizationId ?? "");
  const [phone, setPhone] = React.useState("");
  const [account, setAccount] = React.useState<CustomerAccountRow | null>(null);
  const [searched, setSearched] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [reason, setReason] = React.useState("");
  const [requests, setRequests] = React.useState<BlacklistRequestRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("customer_blacklist_request", requests), [requests, reportViewedObjects]);
  React.useEffect(() => reportViewedObjects?.("customer", account ? [account] : []), [account, reportViewedObjects]);
  const [restriction, setRestriction] = React.useState<BoundedSetting>(emptySetting);
  const [churn, setChurn] = React.useState<BoundedSetting>(emptySetting);
  const [frequentNight, setFrequentNight] = React.useState<BoundedSetting>(emptySetting);
  const [frequentTwoDay, setFrequentTwoDay] = React.useState<BoundedSetting>(emptySetting);
  // 行明细抽屉：整行可点 → 该客人账号完整字段（掩码手机号·中文状态·中文日期）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    api.get<GovernanceProjectRow[]>("/api/v1/platform/projects").then((rows) => {
      setProjects(rows ?? []);
      setProject((current) => current || rows?.[0]?.id || "");
    }).catch(() => setProjects([]));
    api.get<{ threshold: number; minThreshold: number | null; maxThreshold: number | null }>("/api/v1/customer-churn-alert-setting")
      .then((s) => setChurn({ value: String(s.threshold), min: s.minThreshold == null ? "" : String(s.minThreshold), max: s.maxThreshold == null ? "" : String(s.maxThreshold) }))
      .catch(() => setChurn(emptySetting));
  }, [api]);

  const loadRequests = React.useCallback(() => {
    api.get<BlacklistRequestRow[]>("/api/v1/customer-blacklist-requests?status=pending")
      .then((rows) => setRequests(rows ?? []))
      .catch(() => setRequests([]));
  }, [api]);
  React.useEffect(loadRequests, [loadRequests]);

  React.useEffect(() => {
    if (!project) return;
    api.get<{ durationDays: number; minDays: number | null; maxDays: number | null }>(`/api/v1/customer-restriction-settings/${encodeURIComponent(project)}`)
      .then((s) => setRestriction({ value: String(s.durationDays), min: s.minDays == null ? "" : String(s.minDays), max: s.maxDays == null ? "" : String(s.maxDays) }))
      .catch(() => setRestriction(emptySetting));
    api.get<{ nightThreshold: number; nightMin: number | null; nightMax: number | null; twoDayThreshold: number; twoDayMin: number | null;
      twoDayMax: number | null }>(`/api/v1/customer-frequent-entry-alert-settings/${encodeURIComponent(project)}`)
      .then((s) => { setFrequentNight(bounded(s.nightThreshold, s.nightMin, s.nightMax)); setFrequentTwoDay(bounded(s.twoDayThreshold, s.twoDayMin, s.twoDayMax)); })
      .catch(() => { setFrequentNight(emptySetting); setFrequentTwoDay(emptySetting); });
  }, [api, project]);

  function openAccountDetail(row: CustomerAccountRow) {
    setDetail({
      title: `客人账号明细 · ${row.maskedPhone ?? row.id}`,
      fields: [
        { label: "手机号", value: row.maskedPhone ?? "—（已释放）" },
        { label: "账号状态", value: STATUS_LABELS[row.status] ?? row.status },
        { label: "受限截止", value: row.suspendedUntil ? cnDate(row.suspendedUntil) : "—" },
        { label: "注册时间", value: cnDate(row.createdAt) },
        { label: "客人编号", value: row.id },
      ],
    });
  }

  async function search() {
    const trimmed = phone.trim();
    if (!trimmed) {
      setGlobalError("请输入完整手机号（精确匹配）");
      return;
    }
    setLoading(true);
    try {
      // AUD-020：手机号属 PII，走 POST body 传参，避免明文手机号进入 URL query string 落日志。
      const rows = await api.post<CustomerAccountRow[]>("/api/v1/customer-accounts/lookup", {
        phone: trimmed,
      });
      setAccount(rows && rows.length > 0 ? rows[0] ?? null : null);
      setSearched(true);
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "客人账号查询失败");
    } finally {
      setLoading(false);
    }
  }

  async function write(action: string, path: string, body: Record<string, unknown>, after?: (result: unknown) => void) {
    try {
      const result = await api.post<unknown>(path, body);
      after?.(result);
      setActionResult({ action, ...body });
      setReason("");
      return true;
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "客人账号操作失败");
      return false;
    }
  }

  function requireReason(): string | null {
    const trimmed = reason.trim();
    if (!trimmed) {
      setGlobalError("请填写原因（留痕）");
      return null;
    }
    return trimmed;
  }

  function accountAction(action: string, path: "suspend" | "reinstate", extra: Record<string, unknown> = {}) {
    if (!account) return;
    const why = requireReason();
    if (!why) return;
    void write(action, `/api/v1/customer-accounts/${account.id}/${path}`, { ...extra, reason: why },
      (updated) => setAccount((updated as CustomerAccountRow | undefined) ?? account));
  }

  function requestBlacklist(action: "list" | "release") {
    if (!account) return;
    const why = requireReason();
    if (!why) return;
    void write(`customer_blacklist_${action}_requested`, `/api/v1/customer-accounts/${account.id}/blacklist-requests`,
      { idempotencyKey: key("customer-blacklist"), action, reason: why, projectId: project }, () => loadRequests());
  }

  function decide(row: BlacklistRequestRow, decision: "approve" | "reject") {
    void write(`customer_blacklist_request_${decision}d`, `/api/v1/customer-blacklist-requests/${row.id}/${decision}`,
      { idempotencyKey: key(`customer-blacklist-${decision}`) }, () => loadRequests());
  }

  function saveBounded(forms: BoundedSetting[], names: string[], label: string, action: string, path: string) {
    const numbers = forms.flatMap((form) => [form.value, form.min, form.max]).map((v) => v.trim());
    if (numbers.some((v) => v === "" || !Number.isInteger(Number(v)))) {
      setGlobalError(`${label}须同时填写值及上下限（整数）`);
      return;
    }
    const body: Record<string, unknown> = { idempotencyKey: key(action) };
    names.forEach((name, i) => { body[name] = Number(numbers[i]); });
    api.put<unknown>(path, body).then((result) => setActionResult({ action, result }))
      .catch((error: unknown) => setGlobalError(error instanceof Error ? error.message : `${label}保存失败`));
  }

  function boundedInputs(form: BoundedSetting, setForm: (next: BoundedSetting) => void, label: string, testId: string) {
    return (
      <span data-testid={testId}>
        <input aria-label={label} type="number" min="1" value={form.value} onChange={(e) => setForm({ ...form, value: e.target.value })} />
        <input aria-label={`${label}下限`} type="number" min="1" value={form.min} onChange={(e) => setForm({ ...form, min: e.target.value })} />
        <input aria-label={`${label}上限`} type="number" min="1" value={form.max} onChange={(e) => setForm({ ...form, max: e.target.value })} />
      </span>
    );
  }

  return (
    <section data-testid="customer-accounts-page">
      <AdminPageHeader
        title="客人账号管理"
        description="按手机号精确查客人。受限只暂停自助订台与员工代订、其余功能照常，到期自动恢复；列入或解除黑名单须保安团队申请、当班夜班值班经理批准、娱乐平台负责人终审。每次操作留审计痕。"
      />
      <div className="admin-inline-form">
        <label>综合体项目<select aria-label="综合体项目" value={project} onChange={(e) => setProject(e.target.value)}>
          {projects.length === 0 && project ? <option value={project}>{project}</option> : null}
          {projects.map((p) => <option key={p.id} value={p.id}>{p.name ?? p.id}</option>)}
        </select></label>
      </div>
      <div className="admin-inline-form" data-testid="customer-account-search">
        <input
          aria-label="客人手机号"
          placeholder="客人手机号（完整精确匹配）"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
        />
        <button type="button" data-testid="customer-account-search-button" disabled={loading} onClick={() => void search()}>
          {loading ? "查询中…" : "查询"}
        </button>
      </div>

      {searched && !account && <p data-testid="customer-account-empty">未找到该手机号对应的客人账号。</p>}

      {account && (
        <>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="customer-account-table">
              <thead>
                <tr>
                  <th>手机号</th>
                  <th>账号状态</th>
                  <th>受限截止</th>
                  <th>注册时间</th>
                <th>详情</th></tr>
              </thead>
              <tbody>
                <tr data-testid={`customer-account-${account.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openAccountDetail(account)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAccountDetail(account); } }}>
                  <td>{account.maskedPhone ?? "—"}</td>
                  <td data-testid="customer-account-status">{STATUS_LABELS[account.status] ?? account.status}</td>
                  <td>{account.suspendedUntil ? cnDate(account.suspendedUntil) : "—"}</td>
                  <td>{cnDate(account.createdAt)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看客人账号详情" onClick={(event) => { event.stopPropagation(); openAccountDetail(account); }}>查看详情</button></td></tr>
              </tbody>
            </table>
          </div>

          <div className="admin-inline-form" data-testid="customer-account-actions">
            <input
              aria-label="操作原因"
              placeholder="原因（必填，留审计）"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
            <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="customer-account-suspend"
              disabled={!(account.status === "active"
                || (account.status === "suspended" && account.suspendedUntil != null && new Date(account.suspendedUntil) <= new Date()))}
              onClick={() => accountAction("customer_restricted", "suspend", { projectId: project })}>
              置为受限（按本项目受限时长）
            </PermissionButton>
            <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="customer-account-reinstate"
              disabled={account.status !== "suspended"}
              onClick={() => accountAction("customer_restriction_lifted", "reinstate")}>
              提前解除受限
            </PermissionButton>
            <PermissionButton permissionsState={effectivePermissions} permissions={REQUEST} data-testid="customer-account-blacklist"
              disabled={account.status === "blacklisted" || account.status === "deregistered"}
              onClick={() => requestBlacklist("list")}>
              申请列入黑名单
            </PermissionButton>
            <PermissionButton permissionsState={effectivePermissions} permissions={REQUEST} data-testid="customer-account-blacklist-release"
              disabled={account.status !== "blacklisted" || !account.maskedPhone}
              onClick={() => requestBlacklist("release")}>
              申请解除黑名单
            </PermissionButton>
          </div>
        </>
      )}

      <h3>黑名单申请（待批准）</h3>
      {requests.length === 0 ? <p data-testid="customer-blacklist-requests-empty">暂无待你批准的黑名单申请。</p> : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="customer-blacklist-requests">
            <thead><tr><th>客人编号</th><th>申请类型</th><th>原因</th><th>申请时间</th><th>当前环节</th><th>操作</th></tr></thead>
            <tbody>
              {requests.map((row) => {
                const finalStep = row.nightManagerApprovedBy != null;
                return (
                  <tr key={row.id} data-testid={`customer-blacklist-request-${row.id}`}>
                    <td>{row.customerId}</td>
                    <td>{ACTION_LABELS[row.action] ?? row.action}</td>
                    <td>{row.reason}</td>
                    <td>{cnDate(row.requestedAt)}</td>
                    <td>{finalStep ? "待娱乐平台负责人终审" : "待本项目当班夜班值班经理批准"}</td>
                    <td>
                      {finalStep ? (
                        <>
                          <PermissionButton permissionsState={effectivePermissions} permissions={APPROVE}
                            data-testid={`customer-blacklist-approve-${row.id}`} onClick={() => decide(row, "approve")}>终审批准</PermissionButton>
                          <PermissionButton permissionsState={effectivePermissions} permissions={APPROVE}
                            data-testid={`customer-blacklist-reject-${row.id}`} onClick={() => decide(row, "reject")}>驳回</PermissionButton>
                        </>
                      ) : (
                        // 第一级按排班判定（当班夜班值班经理），不设权限码，由后端校验。
                        <>
                          <button type="button" data-testid={`customer-blacklist-approve-${row.id}`} onClick={() => decide(row, "approve")}>批准</button>
                          <button type="button" data-testid={`customer-blacklist-reject-${row.id}`} onClick={() => decide(row, "reject")}>驳回</button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <h3>业务参数（值与上下限同配）</h3>
      <div className="admin-inline-form" data-testid="customer-restriction-setting">
        <span>受限时长（天，本项目）</span>
        {boundedInputs(restriction, setRestriction, "受限天数", "customer-restriction-inputs")}
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="customer-restriction-save"
          disabled={!project}
          onClick={() => saveBounded([restriction], ["durationDays", "minDays", "maxDays"], "受限时长", "customer_restriction_setting_saved",
            `/api/v1/customer-restriction-settings/${encodeURIComponent(project)}`)}>
          保存本项目受限时长
        </PermissionButton>
      </div>
      <div className="admin-inline-form" data-testid="customer-frequent-entry-setting">
        <span>高频出入场预警阈值（次，本项目；一晚＝一个营业日，达到即提示楼宇保安团队调阅录像、不阻断入场）</span>
        {boundedInputs(frequentNight, setFrequentNight, "一晚出入阈值", "customer-frequent-night-inputs")}
        {boundedInputs(frequentTwoDay, setFrequentTwoDay, "连续两日出入阈值", "customer-frequent-two-day-inputs")}
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="customer-frequent-entry-save"
          disabled={!project}
          onClick={() => saveBounded([frequentNight, frequentTwoDay],
            ["nightThreshold", "nightMin", "nightMax", "twoDayThreshold", "twoDayMin", "twoDayMax"], "出入阈值",
            "customer_frequent_entry_alert_setting_saved", `/api/v1/customer-frequent-entry-alert-settings/${encodeURIComponent(project)}`)}>
          保存本项目出入阈值
        </PermissionButton>
      </div>
      <div className="admin-inline-form" data-testid="customer-churn-setting">
        <span>同号一年内注销与注册合计次数上限（全平台，超过即提示风控、不阻断）</span>
        {boundedInputs(churn, setChurn, "次数上限", "customer-churn-inputs")}
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="customer-churn-save"
          onClick={() => saveBounded([churn], ["threshold", "minThreshold", "maxThreshold"], "次数上限", "customer_churn_alert_setting_saved",
            "/api/v1/customer-churn-alert-setting")}>
          保存次数上限
        </PermissionButton>
      </div>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
