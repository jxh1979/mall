import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

// 退款分级授权配置行（后端 RefundAuthorityLimitRecord / RefundAutoRuleRecord，客户端 unwrap 已 camel 化）。
type RefundLimitRow = {
  id: string;
  subjectType: string;
  subjectId: string;
  refundDomain: string;
  limitAmountCent: number;
  status: string;
};
type RefundRuleRow = {
  id: string;
  refundDomain: string;
  ruleType: string;
  enabled: boolean;
  status: string;
};

const DOMAINS = ["ticket", "bar", "finance", "all"];
const MANAGE = ["refund.authority.manage"];

/**
 * 退款分级授权配置页（ADM-FIN-003，Stage C，审计 A2 闭环）：平台管理员维护员工自助退款额度 + 自动放行规则，
 * 替代手工改库；配好后开启功能标志 refund.tiered_authorization.enabled 即票务/酒吧/财务三域分级授权生效
 *（额度内自助、超额复核）。仅 refund.authority.manage 可改（决定谁能退多少、哪些自动放行，安全敏感）。
 */
export function RefundAuthorityPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [limits, setLimits] = React.useState<RefundLimitRow[]>([]);
  const [rules, setRules] = React.useState<RefundRuleRow[]>([]);
  // 行明细抽屉：整行可点 -> 该额度/规则完整字段（主体/退款域/规则类型均中文·金额¥）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [limitForm, setLimitForm] = React.useState({ subjectId: "", refundDomain: "all", limitYuan: "" });
  const [ruleForm, setRuleForm] = React.useState({ refundDomain: "all", ruleType: "expired_unused" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [limitRows, ruleRows] = await Promise.all([
        api.get<RefundLimitRow[]>("/api/v1/finance/refund-authority/limits"),
        api.get<RefundRuleRow[]>("/api/v1/finance/refund-authority/rules"),
      ]);
      setLimits(limitRows ?? []);
      setRules(ruleRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "退款分级授权配置读取失败" },
  );

  async function saveLimit() {
    const cent = Math.round(Number(limitForm.limitYuan) * 100);
    if (!limitForm.subjectId.trim() || !Number.isFinite(cent) || cent < 0) {
      setGlobalError("请填写员工 id 与非负的单笔额度（元）");
      return;
    }
    await api.post("/api/v1/finance/refund-authority/limits", {
      subjectType: "staff",
      subjectId: limitForm.subjectId.trim(),
      refundDomain: limitForm.refundDomain,
      limitAmountCent: cent,
      status: "active",
    });
    setActionResult({ action: "refund_limit_saved", subjectId: limitForm.subjectId.trim(), limitAmountCent: cent });
    setLimitForm({ subjectId: "", refundDomain: "all", limitYuan: "" });
    reload();
  }

  async function saveRule(enabled: boolean, target: { refundDomain: string; ruleType: string }) {
    if (!target.ruleType.trim()) {
      setGlobalError("请填写规则类型，如 expired_unused");
      return;
    }
    await api.post("/api/v1/finance/refund-authority/rules", {
      refundDomain: target.refundDomain,
      ruleType: target.ruleType.trim(),
      enabled,
      status: "active",
    });
    setActionResult({ action: "refund_rule_saved", ruleType: target.ruleType.trim(), enabled });
    reload();
  }

  function openLimitDetail(row: RefundLimitRow) {
    setDetail({
      title: `员工自助退款额度 · ${row.subjectId}`,
      fields: [
        { label: "主体", value: formatBusinessStatus(row.subjectType) },
        { label: "员工 id", value: row.subjectId },
        { label: "退款域", value: formatBusinessStatus(row.refundDomain) },
        { label: "单笔额度（元）", value: `¥${(row.limitAmountCent / 100).toFixed(2)}` },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "配置编号", value: row.id },
      ],
    });
  }

  function openRuleDetail(row: RefundRuleRow) {
    setDetail({
      title: `退款自动放行规则 · ${formatBusinessStatus(row.ruleType)}`,
      fields: [
        { label: "退款域", value: formatBusinessStatus(row.refundDomain) },
        { label: "规则类型", value: formatBusinessStatus(row.ruleType) },
        { label: "启用", value: row.enabled ? "是" : "否" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "配置编号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="refund-authority-page">
      <AdminPageHeader
        title="退款分级授权"
        description="配置员工退款自助额度与自动放行规则。仅平台管理员可改（决定谁能退多少、哪些自动放行）；配好后开启功能标志即票务/酒吧/财务三域分级授权生效——额度内自助、超额复核。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>员工自助退款额度</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="refund-limits-table">
            <thead>
              <tr>
                <th>主体</th>
                <th>员工 id</th>
                <th>退款域</th>
                <th>单笔额度（元）</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {limits.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`refund-limit-${row.id}`}
                  onClick={() => openLimitDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openLimitDetail(row); } }}>
                  <td>{formatBusinessStatus(row.subjectType)}</td>
                  <td>{row.subjectId}</td>
                  <td>{formatBusinessStatus(row.refundDomain)}</td>
                  <td>{(row.limitAmountCent / 100).toFixed(2)}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="refund-limit-form">
          <input
            aria-label="员工 id"
            placeholder="员工 id"
            value={limitForm.subjectId}
            onChange={(event) => setLimitForm({ ...limitForm, subjectId: event.target.value })}
          />
          <select
            aria-label="退款域"
            value={limitForm.refundDomain}
            onChange={(event) => setLimitForm({ ...limitForm, refundDomain: event.target.value })}
          >
            {DOMAINS.map((domain) => (
              <option key={domain} value={domain}>
                {domain}
              </option>
            ))}
          </select>
          <input
            aria-label="单笔额度（元）"
            type="number"
            min="0"
            placeholder="单笔额度（元）"
            value={limitForm.limitYuan}
            onChange={(event) => setLimitForm({ ...limitForm, limitYuan: event.target.value })}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="refund-limit-save"
            onClick={() => void saveLimit()}
          >
            保存额度
          </PermissionButton>
        </div>

        <h3>退款自动放行规则</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="refund-rules-table">
            <thead>
              <tr>
                <th>退款域</th>
                <th>规则类型</th>
                <th>启用</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`refund-rule-${row.id}`}
                  onClick={() => openRuleDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRuleDetail(row); } }}>
                  <td>{formatBusinessStatus(row.refundDomain)}</td>
                  <td>{formatBusinessStatus(row.ruleType)}</td>
                  <td>{row.enabled ? "是" : "否"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td onClick={(event) => event.stopPropagation()}>
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={MANAGE}
                      data-testid={`refund-rule-toggle-${row.id}`}
                      onClick={() => void saveRule(!row.enabled, { refundDomain: row.refundDomain, ruleType: row.ruleType })}
                    >
                      {row.enabled ? "停用" : "启用"}
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="refund-rule-form">
          <select
            aria-label="规则退款域"
            value={ruleForm.refundDomain}
            onChange={(event) => setRuleForm({ ...ruleForm, refundDomain: event.target.value })}
          >
            {DOMAINS.map((domain) => (
              <option key={domain} value={domain}>
                {domain}
              </option>
            ))}
          </select>
          <input
            aria-label="规则类型"
            placeholder="规则类型，如 expired_unused"
            value={ruleForm.ruleType}
            onChange={(event) => setRuleForm({ ...ruleForm, ruleType: event.target.value })}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="refund-rule-add"
            onClick={() => void saveRule(true, ruleForm)}
          >
            启用规则
          </PermissionButton>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
