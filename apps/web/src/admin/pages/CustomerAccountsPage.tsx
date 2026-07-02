import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 平台侧客人账号安全投影（后端 CustomerAccountView，unwrap 已 camel 化）。只有掩码手机号，明文不出网。
type CustomerAccountRow = {
  id: string;
  maskedPhone: string;
  status: string;
  suspendedUntil: string | null;
  createdAt: string;
};

const MANAGE = ["customer.account.manage"];

const STATUS_LABELS: Record<string, string> = {
  active: "正常",
  suspended: "封禁中",
  blacklisted: "黑名单",
  deregistered: "已注销",
};

/**
 * 客人账号管理页（ADM-CUS-001，V194 四态生命周期运营闭环）：按手机号精确查客人（盲索引，不提供全量浏览，
 * PII 最小化），对查到的账号执行封禁（到期自动恢复）/拉黑（永久）/恢复。客人自助注销走客人端，本页只读到该状态。
 * 仅 customer.account.manage 可操作（决定客人能否消费，安全敏感，每次操作落审计）。
 */
export function CustomerAccountsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [phone, setPhone] = React.useState("");
  const [account, setAccount] = React.useState<CustomerAccountRow | null>(null);
  const [searched, setSearched] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [suspendForm, setSuspendForm] = React.useState({ until: "", reason: "" });
  const [reason, setReason] = React.useState("");

  async function search() {
    const trimmed = phone.trim();
    if (!trimmed) {
      setGlobalError("请输入完整手机号（精确匹配）");
      return;
    }
    setLoading(true);
    try {
      const rows = await api.get<CustomerAccountRow[]>(
        `/api/v1/customer-accounts?phone=${encodeURIComponent(trimmed)}`,
      );
      setAccount(rows && rows.length > 0 ? rows[0] ?? null : null);
      setSearched(true);
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "客人账号查询失败");
    } finally {
      setLoading(false);
    }
  }

  async function act(path: string, body: Record<string, unknown>, action: string) {
    if (!account) return;
    try {
      const updated = await api.post<CustomerAccountRow>(`/api/v1/customer-accounts/${account.id}/${path}`, body);
      setAccount(updated ?? account);
      setActionResult({ action, customerId: account.id, ...body });
      setSuspendForm({ until: "", reason: "" });
      setReason("");
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "客人账号操作失败");
    }
  }

  function suspend() {
    if (!suspendForm.until) {
      setGlobalError("请选择封禁到期日（到期后客人再登录自动恢复正常）");
      return;
    }
    const until = new Date(`${suspendForm.until}T23:59:59`);
    if (Number.isNaN(until.getTime())) {
      setGlobalError("封禁到期日格式不正确");
      return;
    }
    void act("suspend", { suspendedUntil: until.toISOString(), reason: suspendForm.reason.trim() }, "customer_suspended");
  }

  return (
    <section data-testid="customer-accounts-page">
      <AdminPageHeader
        title="客人账号管理"
        description="按手机号精确查客人，执行封禁（到期自动恢复，期内无法购买使用）、拉黑（永久禁止）、恢复正常。客人自助注销在客人端完成，本页可见该状态。每次操作留审计痕。"
      />
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
                  <th>封禁到期</th>
                  <th>注册时间</th>
                </tr>
              </thead>
              <tbody>
                <tr data-testid={`customer-account-${account.id}`}>
                  <td>{account.maskedPhone}</td>
                  <td data-testid="customer-account-status">{STATUS_LABELS[account.status] ?? account.status}</td>
                  <td>{account.suspendedUntil ? new Date(account.suspendedUntil).toLocaleString() : "—"}</td>
                  <td>{account.createdAt ? new Date(account.createdAt).toLocaleDateString() : "—"}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <h3>封禁（临时）</h3>
          <div className="admin-inline-form" data-testid="customer-account-suspend-form">
            <input
              aria-label="封禁到期日"
              type="date"
              value={suspendForm.until}
              onChange={(event) => setSuspendForm({ ...suspendForm, until: event.target.value })}
            />
            <input
              aria-label="封禁原因"
              placeholder="封禁原因（留审计）"
              value={suspendForm.reason}
              onChange={(event) => setSuspendForm({ ...suspendForm, reason: event.target.value })}
            />
            <PermissionButton
              permissionsState={effectivePermissions}
              permissions={MANAGE}
              data-testid="customer-account-suspend"
              onClick={suspend}
            >
              封禁至到期日
            </PermissionButton>
          </div>

          <h3>拉黑 / 恢复</h3>
          <div className="admin-inline-form" data-testid="customer-account-actions">
            <input
              aria-label="操作原因"
              placeholder="操作原因（留审计）"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
            <PermissionButton
              permissionsState={effectivePermissions}
              permissions={MANAGE}
              data-testid="customer-account-blacklist"
              onClick={() => void act("blacklist", { reason: reason.trim() }, "customer_blacklisted")}
            >
              拉入黑名单（永久）
            </PermissionButton>
            <PermissionButton
              permissionsState={effectivePermissions}
              permissions={MANAGE}
              data-testid="customer-account-reinstate"
              onClick={() => void act("reinstate", { reason: reason.trim() }, "customer_reinstated")}
            >
              恢复正常
            </PermissionButton>
          </div>
        </>
      )}
    </section>
  );
}
