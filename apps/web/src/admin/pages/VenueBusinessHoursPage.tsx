import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, GovernanceSpaceRow } from "../types";
import { PermissionButton } from "../security/Can";

// 营业时间行（后端 VenueBusinessHoursRecord，客户端 unwrap 已 camel 化）。
type VenueBusinessHoursRow = {
  id: string;
  venueId: string;
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
  disabled: boolean;
  effectiveFrom?: string | null;
};

const MANAGE = ["governance.space.manage"];

// 库内 day_of_week：0=周日 .. 6=周六。展示按周一打头（营业排班的习惯读法）。
const WEEK_DAYS: { day: number; label: string }[] = [
  { day: 1, label: "周一" },
  { day: 2, label: "周二" },
  { day: 3, label: "周三" },
  { day: 4, label: "周四" },
  { day: 5, label: "周五" },
  { day: 6, label: "周六" },
  { day: 0, label: "周日" },
];

type DayDraft = { openTime: string; closeTime: string; disabled: boolean };

const EMPTY_DAY: DayDraft = { openTime: "", closeTime: "", disabled: false };

function crossesMidnight(openTime: string, closeTime: string): boolean {
  return !!openTime && !!closeTime && closeTime < openTime;
}

/**
 * 场地营业时间（ADM-GOV-008，D1）：给每个场地按星期几配「开业→歇业」时刻。
 *
 * 这张表决定场次怎么划：客人的入场资格票、当天无限权益都按场次核销——开业开场、歇业关场、
 * 过了场次的票自动作废。某个场地没配营业时间，那个场地的这类票当天一律核销不了。
 */
export function VenueBusinessHoursPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VenueBusinessHoursRow[]>([]);
  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
  const [spacesNotice, setSpacesNotice] = React.useState<string | null>(null);
  const [venueId, setVenueId] = React.useState("");
  const [drafts, setDrafts] = React.useState<Record<number, DayDraft>>({});
  const [busyDay, setBusyDay] = React.useState<number | null>(null);
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [hours, spaceRows] = await Promise.all([
        api.get<VenueBusinessHoursRow[]>("/api/v1/platform/venue-business-hours"),
        // 场地清单是副资源：读不到（无权限/失败）只影响下拉，已配的营业时间照常显示可改。
        loadSecondary(() => api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces"), [] as GovernanceSpaceRow[]),
      ]);
      setRows(hours ?? []);
      setSpaces(spaceRows.data ?? []);
      setSpacesNotice(spaceRows.notice);
    },
    { onError: setGlobalError, fallbackMessage: "场地营业时间读取失败", onStart: () => setGlobalError(null) },
  );

  // 已配过营业时间的场地即使不在场地清单里也要能选到（否则改不了已存在的配置）。
  const venueOptions = React.useMemo(() => {
    const byId = new Map<string, string>();
    for (const space of spaces) {
      if (space.id) byId.set(space.id, space.spaceName || space.name || space.id);
    }
    for (const row of rows) {
      if (!byId.has(row.venueId)) byId.set(row.venueId, row.venueId);
    }
    return [...byId.entries()].map(([id, label]) => ({ id, label }));
  }, [spaces, rows]);

  // 选中场地后，把该场地已配的七天填进编辑草稿（没配的留空=当前不营业）。
  React.useEffect(() => {
    if (!venueId) {
      setDrafts({});
      return;
    }
    const next: Record<number, DayDraft> = {};
    for (const { day } of WEEK_DAYS) {
      const saved = rows.find((row) => row.venueId === venueId && row.dayOfWeek === day);
      next[day] = saved
        ? { openTime: saved.openTime, closeTime: saved.closeTime, disabled: saved.disabled }
        : { ...EMPTY_DAY };
    }
    setDrafts(next);
  }, [venueId, rows]);

  function draftOf(day: number): DayDraft {
    return drafts[day] ?? EMPTY_DAY;
  }

  function patchDay(day: number, patch: Partial<DayDraft>) {
    setDrafts((prev) => ({ ...prev, [day]: { ...(prev[day] ?? EMPTY_DAY), ...patch } }));
  }

  async function saveDay(day: number) {
    const draft = draftOf(day);
    setFormError("");
    if (!venueId) {
      setFormError("请先选择场地。");
      return;
    }
    if (!draft.openTime || !draft.closeTime) {
      setFormError("请先填写开业时刻和歇业时刻。");
      return;
    }
    if (draft.openTime === draft.closeTime) {
      setFormError("开业时刻和歇业时刻不能相同，否则这一天等于全天不营业。");
      return;
    }
    setBusyDay(day);
    setGlobalError(null);
    try {
      await api.post("/api/v1/platform/venue-business-hours", {
        venueId,
        dayOfWeek: day,
        openTime: draft.openTime,
        closeTime: draft.closeTime,
        disabled: draft.disabled,
      });
      setActionResult({ action: "venue_business_hours_saved", venueId, dayOfWeek: day });
      await reload();
    } catch (saveError) {
      const message = saveError instanceof Error ? saveError.message : "营业时间保存失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setBusyDay(null);
    }
  }

  const configuredVenues = new Set(rows.filter((row) => !row.disabled).map((row) => row.venueId)).size;

  return (
    <section data-testid="venue-business-hours-page">
      <AdminPageHeader
        title="场地营业时间"
        description="按场地配每个星期几的开业和歇业时刻。这张表决定场次怎么划：入场资格票、当天无限权益都按场次核销——开业开场、歇业关场、过了场次的票自动作废。某个场地没配营业时间，该场地这类票当天一律核销不了；歇业时刻早于开业时刻表示营业到次日凌晨（如 20:00 至次日 02:00）。保存即刻生效，不设草稿。"
      />
      <TechDetails>
        <div className="admin-generic-evidence" data-testid="venue-business-hours-evidence">
          接口证据：读取 /api/v1/platform/venue-business-hours；读取 /api/v1/platform/spaces；保存 POST /api/v1/platform/venue-business-hours。
        </div>
      </TechDetails>
      <AuditTrailHint event="venue_business_hours.viewed / venue_business_hours.updated" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="venue-business-hours-summary">
          <div className="admin-kpi-card"><span>已配营业时间的场地</span><strong data-testid="venue-business-hours-kpi-venues">{configuredVenues}</strong><small>至少配了一天且未停用</small></div>
          <div className="admin-kpi-card"><span>营业时间配置条数</span><strong data-testid="venue-business-hours-kpi-rows">{rows.length}</strong><small>场地 × 星期几</small></div>
        </div>

        <div className="admin-form-row" data-testid="venue-business-hours-venue-picker">
          <label htmlFor="venue-business-hours-venue">场地</label>
          <select
            id="venue-business-hours-venue"
            data-testid="venue-business-hours-venue-select"
            value={venueId}
            onChange={(event) => setVenueId(event.target.value)}
          >
            <option value="">请选择场地</option>
            {venueOptions.map((option) => (
              <option key={option.id} value={option.id}>{option.label}</option>
            ))}
          </select>
          {spacesNotice ? <span className="admin-muted-action" data-testid="venue-business-hours-spaces-degraded">{spacesNotice}</span> : null}
        </div>

        {formError ? <div className="admin-inline-error" data-testid="venue-business-hours-error">{formError}</div> : null}

        {!venueId ? (
          <div className="admin-state admin-empty" data-testid="venue-business-hours-empty">请先选择一个场地，再配置该场地每天的营业时间。</div>
        ) : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="venue-business-hours-table">
              <thead>
                <tr><th>星期</th><th>开业时刻</th><th>歇业时刻</th><th>本日营业</th><th>说明</th><th>操作</th></tr>
              </thead>
              <tbody>
                {WEEK_DAYS.map(({ day, label }) => {
                  const draft = draftOf(day);
                  const saved = rows.find((row) => row.venueId === venueId && row.dayOfWeek === day);
                  return (
                    <tr key={day} data-testid={`venue-business-hours-day-${day}`}>
                      <td>{label}</td>
                      <td>
                        <input
                          type="time"
                          aria-label={`${label} 开业时刻`}
                          data-testid={`venue-business-hours-open-${day}`}
                          value={draft.openTime}
                          onChange={(event) => patchDay(day, { openTime: event.target.value })}
                        />
                      </td>
                      <td>
                        <input
                          type="time"
                          aria-label={`${label} 歇业时刻`}
                          data-testid={`venue-business-hours-close-${day}`}
                          value={draft.closeTime}
                          onChange={(event) => patchDay(day, { closeTime: event.target.value })}
                        />
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          aria-label={`${label} 本日营业`}
                          data-testid={`venue-business-hours-open-flag-${day}`}
                          checked={!draft.disabled}
                          onChange={(event) => patchDay(day, { disabled: !event.target.checked })}
                        />
                      </td>
                      <td data-testid={`venue-business-hours-note-${day}`}>
                        {draft.disabled
                          ? "本日不营业（配置保留，勾回即恢复）"
                          : crossesMidnight(draft.openTime, draft.closeTime)
                            ? "营业到次日凌晨"
                            : saved
                              ? "已生效"
                              : "尚未配置"}
                      </td>
                      <td>
                        <PermissionButton
                          permissionsState={effectivePermissions}
                          permissions={MANAGE}
                          data-testid={`venue-business-hours-save-${day}`}
                          disabled={busyDay !== null}
                          onClick={() => void saveDay(day)}
                        >
                          保存
                        </PermissionButton>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </AdminDataState>
    </section>
  );
}
