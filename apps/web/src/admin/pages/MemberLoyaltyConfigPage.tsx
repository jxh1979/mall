import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 会员忠诚度运营配置（会员忠诚度 PRD §3 等级 / §6 连续签到 + 生日加倍）。后端新增端点 /api/v1/loyalty/config/*。
type MemberLevelRow = {
  id: string; projectId?: string | null; levelCode: string; levelName: string; levelRank: number;
  upgradeThreshold: number; keepThreshold: number; keepPeriodDays: number; graceDays: number;
  pointMultiplierBasisPoints: number; status: string;
};
type CheckInTierRow = { id: string; projectId?: string | null; streakDays: number; awardPoints: number; status: string };
type BirthdayRuleRow = { id: string; projectId?: string | null; ruleName: string; windowType: string; multiplierBasisPoints: number; status: string };

const VIEW = ["customer.loyalty.view"];
const MANAGE = ["customer.loyalty.manage"];

const LEVEL_STATUSES: Array<[string, string]> = [["active", "生效"], ["inactive", "停用"], ["archived", "归档"]];
const TIER_STATUSES: Array<[string, string]> = [["active", "生效"], ["inactive", "停用"]];
const WINDOW_TYPES: Array<[string, string]> = [["birthday_day", "生日当天"], ["birthday_month", "生日当月"]];

function label(pairs: Array<[string, string]>, code?: string | null): string {
  return pairs.find(([c]) => c === code)?.[1] ?? code ?? "—";
}
function bpToMultiplier(bp?: number | null): string {
  return bp == null ? "—" : `${(bp / 10000).toFixed(2)} 倍`;
}

type LevelForm = { editingId: string; levelCode: string; levelName: string; levelRank: string; upgradeThreshold: string; keepThreshold: string; keepPeriodDays: string; graceDays: string; multiplier: string; status: string };
const EMPTY_LEVEL: LevelForm = { editingId: "", levelCode: "", levelName: "", levelRank: "", upgradeThreshold: "", keepThreshold: "", keepPeriodDays: "365", graceDays: "0", multiplier: "1.0", status: "active" };
type TierForm = { editingId: string; streakDays: string; awardPoints: string; status: string };
const EMPTY_TIER: TierForm = { editingId: "", streakDays: "", awardPoints: "", status: "active" };
type BirthdayForm = { editingId: string; ruleName: string; windowType: string; multiplier: string; status: string };
const EMPTY_BIRTHDAY: BirthdayForm = { editingId: "", ruleName: "", windowType: "birthday_day", multiplier: "2.0", status: "active" };

/**
 * 会员等级与忠诚度配置页（ADM-CUS-004）：会员等级档位/门槛/保级/积分倍率、连续签到阶梯奖励、生日加倍规则。
 * 均按综合体（project_id）配置；平台管理员填综合体 id 查询/配置，项目级管理员由登录归属自动绑定（服务层覆盖）。
 * 查看需 customer.loyalty.view，维护需 customer.loyalty.manage。
 * 备注：列表仅显示生效中(active)配置；将某项改为停用/归档后即从列表隐去。
 */
export function MemberLoyaltyConfigPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [projectId, setProjectId] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [levels, setLevels] = React.useState<MemberLevelRow[]>([]);
  const [tiers, setTiers] = React.useState<CheckInTierRow[]>([]);
  const [birthdays, setBirthdays] = React.useState<BirthdayRuleRow[]>([]);
  const [levelForm, setLevelForm] = React.useState<LevelForm>(EMPTY_LEVEL);
  const [tierForm, setTierForm] = React.useState<TierForm>(EMPTY_TIER);
  const [birthdayForm, setBirthdayForm] = React.useState<BirthdayForm>(EMPTY_BIRTHDAY);

  const qs = query.trim() ? `?project_id=${encodeURIComponent(query.trim())}` : "";
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [lv, ti, bd] = await Promise.all([
        api.get<MemberLevelRow[]>(`/api/v1/loyalty/config/member-levels${qs}`),
        api.get<CheckInTierRow[]>(`/api/v1/loyalty/config/check-in-tiers${qs}`),
        api.get<BirthdayRuleRow[]>(`/api/v1/loyalty/config/birthday-rules${qs}`),
      ]);
      setLevels(lv ?? []); setTiers(ti ?? []); setBirthdays(bd ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "会员忠诚度配置读取失败" },
  );

  // 查询按钮改变 committed query 时重取（首次挂载已由 useAdminResource 自身拉取，故跳过）。
  const firstRender = React.useRef(true);
  React.useEffect(() => {
    if (firstRender.current) { firstRender.current = false; return; }
    void reload();
  }, [query, reload]);

  // 写路径共用 project_id：查询框已锁定的综合体优先，否则用录入框（新增时平台管理员必填）。
  function writeProjectId(): string { return query.trim() || projectId.trim(); }

  async function saveLevel() {
    if (!levelForm.editingId && !writeProjectId()) { setGlobalError("请先在上方填写综合体 id（新增时必填）"); return; }
    if (!levelForm.levelCode.trim() || !levelForm.levelName.trim()) { setGlobalError("请填写等级代码与等级名称"); return; }
    const mult = Math.round(Number(levelForm.multiplier) * 10000);
    if (!Number.isFinite(mult) || mult <= 0) { setGlobalError("积分倍率需为大于 0 的数字（如 1.0、1.5）"); return; }
    if (Number(levelForm.keepPeriodDays) < 1) { setGlobalError("保级考核周期需为不小于 1 的天数"); return; }
    await api.post("/api/v1/loyalty/config/member-levels", {
      id: levelForm.editingId || null,
      projectId: writeProjectId(),
      levelCode: levelForm.levelCode.trim(), levelName: levelForm.levelName.trim(),
      levelRank: Number(levelForm.levelRank) || 0,
      upgradeThreshold: Number(levelForm.upgradeThreshold) || 0,
      keepThreshold: Number(levelForm.keepThreshold) || 0,
      keepPeriodDays: Number(levelForm.keepPeriodDays), graceDays: Number(levelForm.graceDays) || 0,
      pointMultiplierBasisPoints: mult, status: levelForm.status,
    });
    setActionResult({ action: levelForm.editingId ? "member_level_updated" : "member_level_created", levelCode: levelForm.levelCode.trim() });
    setLevelForm(EMPTY_LEVEL); reload();
  }

  async function saveTier() {
    if (!tierForm.editingId && !writeProjectId()) { setGlobalError("请先在上方填写综合体 id（新增时必填）"); return; }
    if (Number(tierForm.streakDays) < 1) { setGlobalError("连续签到天数需为不小于 1 的整数"); return; }
    if (Number(tierForm.awardPoints) < 0) { setGlobalError("奖励积分需为不小于 0 的数字"); return; }
    await api.post("/api/v1/loyalty/config/check-in-tiers", {
      id: tierForm.editingId || null, projectId: writeProjectId(),
      streakDays: Number(tierForm.streakDays), awardPoints: Number(tierForm.awardPoints), status: tierForm.status,
    });
    setActionResult({ action: tierForm.editingId ? "check_in_tier_updated" : "check_in_tier_created", streakDays: Number(tierForm.streakDays) });
    setTierForm(EMPTY_TIER); reload();
  }

  async function saveBirthday() {
    if (!birthdayForm.editingId && !writeProjectId()) { setGlobalError("请先在上方填写综合体 id（新增时必填）"); return; }
    if (!birthdayForm.ruleName.trim()) { setGlobalError("请填写生日规则名称"); return; }
    const mult = Math.round(Number(birthdayForm.multiplier) * 10000);
    if (!Number.isFinite(mult) || mult <= 10000) { setGlobalError("生日倍率需大于 1.0 倍（如 2.0）"); return; }
    await api.post("/api/v1/loyalty/config/birthday-rules", {
      id: birthdayForm.editingId || null, projectId: writeProjectId(),
      ruleName: birthdayForm.ruleName.trim(), windowType: birthdayForm.windowType,
      multiplierBasisPoints: mult, status: birthdayForm.status,
    });
    setActionResult({ action: birthdayForm.editingId ? "birthday_rule_updated" : "birthday_rule_created", ruleName: birthdayForm.ruleName.trim() });
    setBirthdayForm(EMPTY_BIRTHDAY); reload();
  }

  return (
    <section data-testid="member-loyalty-config-page">
      <AdminPageHeader
        title="会员等级与忠诚度配置"
        description="按综合体维护会员等级档位/升级门槛/保级/积分倍率、连续签到阶梯奖励、生日加倍规则。改配置不改代码；列表仅显示生效中配置。查看需会员忠诚度查看权限，维护需会员忠诚度维护权限。"
      />
      <div className="admin-form-row" data-testid="loyalty-project-filter">
        <label>综合体 id<input aria-label="综合体 id 查询" placeholder="综合体 id（留空看平台缺省）" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <button type="button" data-testid="loyalty-query" onClick={() => setQuery(projectId)}>查询该综合体配置</button>
        <span className="admin-muted-action">项目级管理员由登录归属自动绑定，无需填写。</span>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {/* ---- 会员等级 ---- */}
        <h3>会员等级档位（按累计成长积分自动升级；保级/宽限期到期才可能下调）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="member-levels-table">
            <thead><tr><th>排序</th><th>代码</th><th>名称</th><th>升级门槛</th><th>保级门槛</th><th>考核周期(天)</th><th>宽限期(天)</th><th>积分倍率</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {levels.map((r) => (
                <tr key={r.id} data-testid={`member-level-${r.id}`}>
                  <td>{r.levelRank}</td><td>{r.levelCode}</td><td>{r.levelName}</td>
                  <td>{r.upgradeThreshold}</td><td>{r.keepThreshold}</td><td>{r.keepPeriodDays}</td><td>{r.graceDays}</td>
                  <td>{bpToMultiplier(r.pointMultiplierBasisPoints)}</td><td>{label(LEVEL_STATUSES, r.status)}</td>
                  <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`member-level-edit-${r.id}`}
                    onClick={() => setLevelForm({ editingId: r.id, levelCode: r.levelCode, levelName: r.levelName, levelRank: String(r.levelRank), upgradeThreshold: String(r.upgradeThreshold), keepThreshold: String(r.keepThreshold), keepPeriodDays: String(r.keepPeriodDays), graceDays: String(r.graceDays), multiplier: (r.pointMultiplierBasisPoints / 10000).toFixed(2), status: r.status })}>编辑</PermissionButton></td>
                </tr>
              ))}
              {levels.length === 0 && <tr><td colSpan={10} className="admin-muted-action">暂无等级配置。</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="admin-form-grid" data-testid="member-level-form">
          <label>代码<input aria-label="等级代码" placeholder="gold" value={levelForm.levelCode} onChange={(e) => setLevelForm({ ...levelForm, levelCode: e.target.value })} /></label>
          <label>名称<input aria-label="等级名称" placeholder="金卡" value={levelForm.levelName} onChange={(e) => setLevelForm({ ...levelForm, levelName: e.target.value })} /></label>
          <label>排序<input aria-label="等级排序" type="number" min="0" value={levelForm.levelRank} onChange={(e) => setLevelForm({ ...levelForm, levelRank: e.target.value })} /></label>
          <label>升级门槛<input aria-label="升级门槛" type="number" min="0" placeholder="累计成长积分" value={levelForm.upgradeThreshold} onChange={(e) => setLevelForm({ ...levelForm, upgradeThreshold: e.target.value })} /></label>
          <label>保级门槛<input aria-label="保级门槛" type="number" min="0" value={levelForm.keepThreshold} onChange={(e) => setLevelForm({ ...levelForm, keepThreshold: e.target.value })} /></label>
          <label>考核周期(天)<input aria-label="考核周期天数" type="number" min="1" value={levelForm.keepPeriodDays} onChange={(e) => setLevelForm({ ...levelForm, keepPeriodDays: e.target.value })} /></label>
          <label>宽限期(天)<input aria-label="宽限期天数" type="number" min="0" value={levelForm.graceDays} onChange={(e) => setLevelForm({ ...levelForm, graceDays: e.target.value })} /></label>
          <label>积分倍率<input aria-label="积分倍率" type="number" min="0" step="0.1" placeholder="1.0" value={levelForm.multiplier} onChange={(e) => setLevelForm({ ...levelForm, multiplier: e.target.value })} /></label>
          <label>状态<select aria-label="等级状态" value={levelForm.status} onChange={(e) => setLevelForm({ ...levelForm, status: e.target.value })}>{LEVEL_STATUSES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}</select></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="member-level-save" onClick={() => void saveLevel()}>{levelForm.editingId ? "保存修改" : "新增等级"}</PermissionButton>
          {levelForm.editingId && <button type="button" data-testid="member-level-cancel" onClick={() => setLevelForm(EMPTY_LEVEL)}>取消编辑</button>}
        </div>

        {/* ---- 连续签到阶梯 ---- */}
        <h3>连续签到阶梯奖励（连续签到达天数发对应积分；断签重置）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="check-in-tiers-table">
            <thead><tr><th>连续天数</th><th>奖励积分</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {tiers.map((r) => (
                <tr key={r.id} data-testid={`check-in-tier-${r.id}`}>
                  <td>{r.streakDays}</td><td>{r.awardPoints}</td><td>{label(TIER_STATUSES, r.status)}</td>
                  <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`check-in-tier-edit-${r.id}`}
                    onClick={() => setTierForm({ editingId: r.id, streakDays: String(r.streakDays), awardPoints: String(r.awardPoints), status: r.status })}>编辑</PermissionButton></td>
                </tr>
              ))}
              {tiers.length === 0 && <tr><td colSpan={4} className="admin-muted-action">暂无签到阶梯配置。</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="admin-form-grid" data-testid="check-in-tier-form">
          <label>连续天数<input aria-label="连续签到天数" type="number" min="1" placeholder="7" value={tierForm.streakDays} onChange={(e) => setTierForm({ ...tierForm, streakDays: e.target.value })} /></label>
          <label>奖励积分<input aria-label="签到奖励积分" type="number" min="0" placeholder="100" value={tierForm.awardPoints} onChange={(e) => setTierForm({ ...tierForm, awardPoints: e.target.value })} /></label>
          <label>状态<select aria-label="签到阶梯状态" value={tierForm.status} onChange={(e) => setTierForm({ ...tierForm, status: e.target.value })}>{TIER_STATUSES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}</select></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="check-in-tier-save" onClick={() => void saveTier()}>{tierForm.editingId ? "保存修改" : "新增阶梯"}</PermissionButton>
          {tierForm.editingId && <button type="button" data-testid="check-in-tier-cancel" onClick={() => setTierForm(EMPTY_TIER)}>取消编辑</button>}
        </div>

        {/* ---- 生日加倍 ---- */}
        <h3>生日加倍规则（生日当天/当月消费积分加倍，与等级倍率叠加）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="birthday-rules-table">
            <thead><tr><th>规则名称</th><th>生效窗口</th><th>倍率</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {birthdays.map((r) => (
                <tr key={r.id} data-testid={`birthday-rule-${r.id}`}>
                  <td>{r.ruleName}</td><td>{label(WINDOW_TYPES, r.windowType)}</td><td>{bpToMultiplier(r.multiplierBasisPoints)}</td><td>{label(TIER_STATUSES, r.status)}</td>
                  <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`birthday-rule-edit-${r.id}`}
                    onClick={() => setBirthdayForm({ editingId: r.id, ruleName: r.ruleName, windowType: r.windowType, multiplier: (r.multiplierBasisPoints / 10000).toFixed(2), status: r.status })}>编辑</PermissionButton></td>
                </tr>
              ))}
              {birthdays.length === 0 && <tr><td colSpan={5} className="admin-muted-action">暂无生日加倍规则。</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="admin-form-grid" data-testid="birthday-rule-form">
          <label>规则名称<input aria-label="生日规则名称" placeholder="生日双倍积分" value={birthdayForm.ruleName} onChange={(e) => setBirthdayForm({ ...birthdayForm, ruleName: e.target.value })} /></label>
          <label>生效窗口<select aria-label="生日生效窗口" value={birthdayForm.windowType} onChange={(e) => setBirthdayForm({ ...birthdayForm, windowType: e.target.value })}>{WINDOW_TYPES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}</select></label>
          <label>倍率<input aria-label="生日倍率" type="number" min="1" step="0.1" placeholder="2.0" value={birthdayForm.multiplier} onChange={(e) => setBirthdayForm({ ...birthdayForm, multiplier: e.target.value })} /></label>
          <label>状态<select aria-label="生日规则状态" value={birthdayForm.status} onChange={(e) => setBirthdayForm({ ...birthdayForm, status: e.target.value })}>{TIER_STATUSES.map(([c, l]) => <option key={c} value={c}>{l}</option>)}</select></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="birthday-rule-save" onClick={() => void saveBirthday()}>{birthdayForm.editingId ? "保存修改" : "新增规则"}</PermissionButton>
          {birthdayForm.editingId && <button type="button" data-testid="birthday-rule-cancel" onClick={() => setBirthdayForm(EMPTY_BIRTHDAY)}>取消编辑</button>}
        </div>
      </AdminDataState>
    </section>
  );
}
