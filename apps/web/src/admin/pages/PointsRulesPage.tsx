import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { TraceLink } from "../components/TraceLink";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 积分规则引擎配置（会员忠诚度 PRD §3/§4）。后端既有 CRUD 端点 /api/v1/customer/points-rules（V060/V269）。
type PointsRuleRow = {
  id: string;
  projectId?: string | null;
  ruleName: string;
  earnMethod: string;
  earnRatio: number | string;
  levelThreshold?: string | null;
  validityType: string;
  validityDays?: number | string | null;
  expireMode?: string | null;
  clearDay?: string | null;
  status: string;
};

const VIEW = ["customer.points_rule.view"];
const MANAGE = ["customer.points_rule.manage"];

// 枚举中文化（全中文铁律；提交仍用原码，对齐 V060 DB CHECK / 服务层白名单）。
const EARN_METHODS: Array<[string, string]> = [
  ["consume_amount", "按消费金额"],
  ["check_in", "按签到打卡"],
  ["activity", "按活动"],
];
const VALIDITY_TYPES: Array<[string, string]> = [
  ["permanent", "永久有效"],
  ["fixed_days", "固定天数后过期"],
  ["natural_year", "按自然年清零"],
];
const EXPIRE_MODES: Array<[string, string]> = [
  ["", "不单独指定"],
  ["fifo_batch", "按批次先进先出过期"],
  ["whole_account_yearly", "整账户年度清零"],
];
const STATUSES: Array<[string, string]> = [
  ["active", "生效"],
  ["draft", "草稿"],
  ["paused", "暂停"],
  ["expired", "已过期"],
];

function label(pairs: Array<[string, string]>, code?: string | null): string {
  return pairs.find(([c]) => c === code)?.[1] ?? code ?? "—";
}

type FormState = {
  editingId: string;
  projectId: string;
  ruleName: string;
  earnMethod: string;
  earnRatio: string;
  levelThreshold: string;
  validityType: string;
  validityDays: string;
  expireMode: string;
  clearDay: string;
  status: string;
};

const EMPTY: FormState = {
  editingId: "",
  projectId: "",
  ruleName: "",
  earnMethod: "consume_amount",
  earnRatio: "",
  levelThreshold: "",
  validityType: "permanent",
  validityDays: "",
  expireMode: "",
  clearDay: "",
  status: "active",
};

/**
 * 积分规则配置页（ADM-CUS-003）：维护积分获取方式/系数、会员等级累计成长积分阈值、有效期与过期策略。
 * 兑换规则（积分兑虚拟券·券三类）在「券台账管理」页配置（ADM-CUS-002），本页仅积分获取与过期口径。
 * 查看需 customer.points_rule.view，维护需 customer.points_rule.manage。
 */
export function PointsRulesPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<PointsRuleRow[]>([]);
  const [form, setForm] = React.useState<FormState>(EMPTY);
  const [open, setOpen] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<PointsRuleRow[]>("/api/v1/customer/points-rules");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "积分规则读取失败" },
  );

  function openCreate() {
    setForm(EMPTY);
    setFormError("");
    setOpen(true);
  }

  function openEdit(row: PointsRuleRow) {
    setForm({
      editingId: row.id,
      projectId: row.projectId ?? "",
      ruleName: row.ruleName ?? "",
      earnMethod: row.earnMethod ?? "consume_amount",
      earnRatio: row.earnRatio == null ? "" : String(row.earnRatio),
      levelThreshold: row.levelThreshold ?? "",
      validityType: row.validityType ?? "permanent",
      validityDays: row.validityDays == null ? "" : String(row.validityDays),
      expireMode: row.expireMode ?? "",
      clearDay: row.clearDay ?? "",
      status: row.status ?? "active",
    });
    setFormError("");
    setOpen(true);
  }

  function validate(): string | null {
    if (!form.editingId && !form.projectId.trim()) return "请填写综合体 id（新增时必填；项目级管理员由登录归属自动绑定）";
    if (!form.ruleName.trim()) return "请填写规则名称";
    const ratio = Number(form.earnRatio);
    if (!Number.isFinite(ratio) || ratio < 0) return "获取系数需为不小于 0 的数字";
    if (form.levelThreshold.trim()) {
      try {
        JSON.parse(form.levelThreshold);
      } catch {
        return "等级阈值需为合法 JSON，如 {\"silver\":1000,\"gold\":5000}";
      }
    }
    return null;
  }

  async function submit() {
    const invalid = validate();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    setSubmitting(true);
    try {
      const body: Record<string, unknown> = {
        ruleName: form.ruleName.trim(),
        earnMethod: form.earnMethod,
        earnRatio: Number(form.earnRatio),
        levelThreshold: form.levelThreshold.trim() || "{}",
        validityType: form.validityType,
        validityDays: form.validityDays.trim() ? Number(form.validityDays) : null,
        expireMode: form.expireMode || null,
        clearDay: form.clearDay.trim() || null,
        status: form.status,
      };
      if (!form.editingId) body.projectId = form.projectId.trim();
      const path = form.editingId
        ? `/api/v1/customer/points-rules/${encodeURIComponent(form.editingId)}`
        : "/api/v1/customer/points-rules";
      await api.post(path, body);
      setActionResult({ action: form.editingId ? "points_rule_updated" : "points_rule_created", ruleName: form.ruleName.trim() });
      setOpen(false);
      setForm(EMPTY);
      reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "积分规则保存失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="points-rules-page">
      <AdminPageHeader
        title="积分规则配置"
        description="维护积分获取方式与系数、会员等级累计成长积分阈值、积分有效期与过期策略。积分兑虚拟券（券三类）在「券台账管理」页配置。改配置不改代码；仅有积分规则维护权限者可改。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="points-rule-create-open" onClick={openCreate}>
            ＋ 新增积分规则
          </PermissionButton>
          <span className="admin-muted-action">
            积分兑换券口径（商户折扣/店内实物/外部伙伴三类）请在
            <TraceLink navigateTo={navigateTo} pageId="ADM-CUS-002">「券台账管理」</TraceLink>
            页配置。
          </span>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="points-rules-table">
            <thead>
              <tr>
                <th>规则名称</th>
                <th>综合体</th>
                <th>获取方式</th>
                <th>获取系数</th>
                <th>有效期</th>
                <th>过期策略</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`points-rule-${row.id}`}>
                  <td>{row.ruleName}</td>
                  <td>{row.projectId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId}</TraceLink> : "平台缺省"}</td>
                  <td>{label(EARN_METHODS, row.earnMethod)}</td>
                  <td>{String(row.earnRatio ?? "—")}</td>
                  <td>{label(VALIDITY_TYPES, row.validityType)}{row.validityType === "fixed_days" && row.validityDays != null ? `（${row.validityDays} 天）` : ""}</td>
                  <td>{label(EXPIRE_MODES, row.expireMode ?? "")}</td>
                  <td>{label(STATUSES, row.status)}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`points-rule-edit-${row.id}`} onClick={() => openEdit(row)}>
                      编辑
                    </PermissionButton>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr><td colSpan={8} className="admin-muted-action">暂无积分规则，点上方「新增积分规则」创建。</td></tr>
              )}
            </tbody>
          </table>
        </div>
        <GuidedFlowModal
          open={open}
          title={form.editingId ? "编辑积分规则" : "新增积分规则"}
          testId="points-rule-guided"
          submitTestId="points-rule-save"
          submitLabel={form.editingId ? "保存修改" : "创建规则"}
          submitting={submitting}
          error={formError || undefined}
          onClose={() => setOpen(false)}
          onSubmit={() => void submit()}
          steps={[
            {
              title: "规则信息",
              description: "填写积分获取方式与系数、等级阈值、有效期与过期策略。",
              validate,
              content: (
                <>
                  {!form.editingId && (
                    <label>综合体 id<input aria-label="综合体 id" placeholder="综合体 id（项目级管理员可留空）" value={form.projectId} onChange={(e) => setForm({ ...form, projectId: e.target.value })} /></label>
                  )}
                  <label>规则名称<input aria-label="规则名称" placeholder="如：基础消费积分" value={form.ruleName} onChange={(e) => setForm({ ...form, ruleName: e.target.value })} /></label>
                  <label>获取方式
                    <select aria-label="获取方式" value={form.earnMethod} onChange={(e) => setForm({ ...form, earnMethod: e.target.value })}>
                      {EARN_METHODS.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                    </select>
                  </label>
                  <label>获取系数<input aria-label="获取系数" type="number" min="0" placeholder="按金额=每元积分数；按签到/活动=每次固定积分" value={form.earnRatio} onChange={(e) => setForm({ ...form, earnRatio: e.target.value })} /></label>
                  <label>等级阈值(JSON)<textarea aria-label="等级阈值JSON" rows={2} placeholder={'{"silver":1000,"gold":5000}'} value={form.levelThreshold} onChange={(e) => setForm({ ...form, levelThreshold: e.target.value })} /></label>
                  <label>有效期类型
                    <select aria-label="有效期类型" value={form.validityType} onChange={(e) => setForm({ ...form, validityType: e.target.value })}>
                      {VALIDITY_TYPES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                    </select>
                  </label>
                  {form.validityType === "fixed_days" && (
                    <label>有效天数<input aria-label="有效天数" type="number" min="1" placeholder="如 365" value={form.validityDays} onChange={(e) => setForm({ ...form, validityDays: e.target.value })} /></label>
                  )}
                  <label>过期策略
                    <select aria-label="过期策略" value={form.expireMode} onChange={(e) => setForm({ ...form, expireMode: e.target.value })}>
                      {EXPIRE_MODES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                    </select>
                  </label>
                  <label>清零日<input aria-label="清零日" placeholder="自然年清零日，如 12-31" value={form.clearDay} onChange={(e) => setForm({ ...form, clearDay: e.target.value })} /></label>
                  <label>状态
                    <select aria-label="状态" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                      {STATUSES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                    </select>
                  </label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "规则名称", value: form.ruleName.trim() || "未填写" },
            { label: "获取方式", value: label(EARN_METHODS, form.earnMethod) },
            { label: "获取系数", value: form.earnRatio.trim() || "未填写" },
            { label: "有效期", value: label(VALIDITY_TYPES, form.validityType) },
            { label: "状态", value: label(STATUSES, form.status) },
          ]}
        />
      </AdminDataState>
    </section>
  );
}
