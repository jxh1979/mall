// 文件版本：V0.03
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

type ShiftRow = { nightDutyProjectId?: string | null; id: string; userId?: string; workDate?: string; startAt?: string; endAt?: string; pointSpaceIds?: string[]; note?: string; createdBy?: string; reviewId?: string };
type ReceiptEvent = { userId?: string; contextId?: string; id: string; deviceId: string; shiftId?: string; observedAt?: string; actualAt?: string; method: string; status: string; reason?: string; reviewId?: string };
type AttendanceRow = { id: string; userId?: string; punchType?: string; punchedAt?: string; method?: string; judgement?: string; spaceId?: string; deviceFingerprint?: string };

const TEXT = {
  title: "排班与考勤",
  description: "排班及每次修改均经主管上一级批准后发布，开始后锁定。员工领取本人工作手机记上班，NFC、扫码与离开充电设施不重复计时；严格按班次判定考勤。",
  loadFailed: "排班考勤读取失败",
};

const JUDGE_LABELS: Record<string, string> = { normal: "正常", late: "迟到", early_leave: "早退", no_shift: "无班次" };
type ReceiptReview = { id: string; reviewId: string; status: string; actualAt: string; shiftId: string; reason: string; handoverEvidence: string; initiatedBy: string; createdAt: string };

const METHOD_LABELS: Record<string, string> = { nfc: "NFC", qr: "扫码", manual: "人工记录", charging_disconnect: "离开充电设施", device_return: "本班手机归还" };
const TYPE_LABELS: Record<string, string> = { check_in: "上班", check_out: "下班" };
const WEEKDAY_LABELS = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];

/** 员工/点位这类「一行填多个」的输入：中英文逗号、空白、换行都当分隔符（排班员从名单里粘一串进来也认）。 */
function splitList(value: string) {
  return value.split(/[,，\s]+/).map((item) => item.trim()).filter(Boolean);
}

/** 结束时刻不晚于开始时刻 = 跨零点班，后端把班次结束顺延到次日；这里照同一口径提示排班员。 */
function crossesMidnight(startTime: string, endTime: string) {
  return Boolean(startTime && endTime && endTime <= startTime);
}

function localTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—";
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult, navigateTo, user }: AdminPageProps) {
  const [editingShift, setEditingShift] = React.useState<string | null>(null);
  const [visibility, setVisibility] = React.useState<{ value: string; minValue: string; maxValue: string } | null>(null);
  const [settingReason, setSettingReason] = React.useState("");
  const [settingMessage, setSettingMessage] = React.useState("");
  const [historyText, setHistoryText] = React.useState("");
  const [coverShift, setCoverShift] = React.useState<ShiftRow | null>(null);
  const [coverForm, setCoverForm] = React.useState({ userId: "", start: "", end: "", reason: "", handover: "" });
  const [coverError, setCoverError] = React.useState("");
  const [coverSaving, setCoverSaving] = React.useState(false);
  const [coverHistory, setCoverHistory] = React.useState("");
  const [receiptUser, setReceiptUser] = React.useState("");
  const [receiptRows, setReceiptRows] = React.useState<ReceiptEvent[] | null>(null);
  const [receiptError, setReceiptError] = React.useState("");
  const [reviewEvent, setReviewEvent] = React.useState<ReceiptEvent | null>(null);
  const [reviewForm, setReviewForm] = React.useState({ shiftId: "", actualAt: "", reason: "", handoverEvidence: "" });
  const [reviewError, setReviewError] = React.useState("");
  const [reviewSaving, setReviewSaving] = React.useState(false);
  const [receiptReviewHistory, setReceiptReviewHistory] = React.useState<ReceiptReview[] | null>(null);
  const [date, setDate] = React.useState(today);
  const [shifts, setShifts] = React.useState<ShiftRow[]>([]);
  const [attendance, setAttendance] = React.useState<AttendanceRow[]>([]);
  const [userId, setUserId] = React.useState("");
  const [start, setStart] = React.useState("");
  const [end, setEnd] = React.useState("");
  const [points, setPoints] = React.useState("");
  const [note, setNote] = React.useState("");
  const [nightDuty, setNightDuty] = React.useState(false);
  // 引导式「新增排班」弹窗状态（范式：字段留页面持有，弹窗只编排）。
  const [shiftOpen, setShiftOpen] = React.useState(false);
  const [shiftError, setShiftError] = React.useState("");
  const [shiftSaving, setShiftSaving] = React.useState(false);
  // 行明细抽屉：整行可点 → 该排班/考勤完整字段（枚举中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 按周批量排班（2026-08-02 用户拍板）：排班员一次排出一周的班，不再一人一天点一次。
  const [batchOpen, setBatchOpen] = React.useState(false);
  const [batchUsers, setBatchUsers] = React.useState("");
  const [batchStartDate, setBatchStartDate] = React.useState(today);
  const [batchDays, setBatchDays] = React.useState("7");
  const [batchWeekdays, setBatchWeekdays] = React.useState<number[]>([]);
  const [batchStartTime, setBatchStartTime] = React.useState("");
  const [batchEndTime, setBatchEndTime] = React.useState("");
  const [batchPoints, setBatchPoints] = React.useState("");
  const [batchNote, setBatchNote] = React.useState("");
  const [batchNightDuty, setBatchNightDuty] = React.useState(false);
  const [batchError, setBatchError] = React.useState("");
  const [batchSaving, setBatchSaving] = React.useState(false);
  const [batchSummary, setBatchSummary] = React.useState("");

  const toggleWeekday = (weekday: number) => {
    setBatchWeekdays((prev) => (prev.includes(weekday)
      ? prev.filter((day) => day !== weekday)
      : [...prev, weekday].sort((a, b) => a - b)));
  };

  function openShiftDetail(row: ShiftRow) {
    setDetail({
      title: `排班明细 · ${row.userId ?? row.id}`,
      fields: [
        { label: "员工", value: row.userId ?? "—" },
        { label: "开始", value: localTime(row.startAt) },
        { label: "结束", value: localTime(row.endAt) },
        { label: "点位", value: row.pointSpaceIds?.length ? row.pointSpaceIds.join("、") : "未指定" },
        { label: "备注", value: row.note ?? "—" },
        { label: "夜班值班职责", value: row.nightDutyProjectId ? "项目 " + row.nightDutyProjectId : "无" },
        { label: "排班编号", value: row.id },
      ],
    });
  }

  function openAttendanceDetail(row: AttendanceRow) {
    setDetail({
      title: `考勤明细 · ${row.userId ?? row.id}`,
      fields: [
        { label: "员工", value: row.userId ?? "—" },
        { label: "类型", value: TYPE_LABELS[row.punchType ?? ""] ?? row.punchType ?? "—" },
        { label: "已核实实际时间", value: localTime(row.punchedAt) },
        { label: "方式", value: METHOD_LABELS[row.method ?? ""] ?? row.method ?? "—" },
        { label: "判定", value: JUDGE_LABELS[row.judgement ?? ""] ?? row.judgement ?? "—" },
        { label: "点位", value: row.spaceId ?? "—" },
        { label: "设备指纹", value: row.deviceFingerprint ?? "—" },
        { label: "考勤编号", value: row.id },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [shiftRows, attRows] = await Promise.all([
        api.get<ShiftRow[]>(`/api/v1/staff/shifts?date=${date}`),
        api.get<AttendanceRow[]>(`/api/v1/staff/attendance?date=${date}`),
      ]);
      setShifts(shiftRows ?? []);
      setAttendance(attRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const isFirstDateChange = React.useRef(true);
  React.useEffect(() => {
    if (isFirstDateChange.current) {
      isFirstDateChange.current = false;
      return;
    }
    void reload();
  }, [date, reload]);

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体/双保险校验保留。
  const saveShift = async () => {
    if (!userId.trim() || !start || !end) {
      setShiftError("请填全 员工/开始/结束时间");
      return;
    }
    setShiftSaving(true);
    setShiftError("");
    try {
      const result = await api.post<{ reviewId?: string }>("/api/v1/staff/shifts", {
        userId: userId.trim(),
        shiftId: editingShift,
        workDate: date,
        startAt: new Date(start).toISOString(),
        endAt: new Date(end).toISOString(),
        pointSpaceIds: points.split(",").map((value) => value.trim()).filter(Boolean),
        note: note.trim() || null,
        nightDutyManager: nightDuty,
      });
      setActionResult({ 结果: "排班申请已提交，等待主管上一级审批", 审批单: result?.reviewId });
      setBatchSummary("排班申请已提交，批准后才会出现在生效排班中。审批单：" + (result?.reviewId ?? "—"));
      setEditingShift(null);
      setUserId(""); setStart(""); setEnd(""); setPoints(""); setNote(""); setNightDuty(false);
      setShiftOpen(false);
      await reload();
    } catch (e) { setShiftError((e as Error).message); } finally { setShiftSaving(false); }
  };

  const submitReceiptReview = async () => {
    if (!reviewEvent || !reviewForm.shiftId.trim() || !reviewForm.actualAt || !reviewForm.reason.trim() || !reviewForm.handoverEvidence.trim()) {
      setReviewError("请填写原班次、核实的实际领取时间、原因和实际交接核实事实"); return;
    }
    setReviewSaving(true); setReviewError("");
    try {
      const result = await api.post<ReceiptReview>("/api/v1/staff/attendance/receipt-events/" + encodeURIComponent(reviewEvent.id) + "/review", {
        shiftId: reviewForm.shiftId.trim(), actualAt: new Date(reviewForm.actualAt).toISOString(),
        reason: reviewForm.reason.trim(), handoverEvidence: reviewForm.handoverEvidence.trim(),
      });
      setActionResult({ 结果: "核时申请已提交，经办主管的上一级批准后才计入考勤", 审批单: result.reviewId });
      setReceiptReviewHistory([result]); setReviewEvent(null);
    } catch (error) { setReviewError((error as Error).message); } finally { setReviewSaving(false); }
  };

  const saveCover = async () => {
    if (!coverShift) return;
    const { userId: target, start: from, end: to, reason, handover } = coverForm;
    if (!target.trim() || !from || !to || !reason.trim() || !handover.trim()) {
      setCoverError("请填写顶班员工、起止时刻、事由及实际交接事实。"); return;
    }
    if (new Date(to) <= new Date(from) || new Date(from) < new Date(coverShift.startAt ?? "") || new Date(to) > new Date(coverShift.endAt ?? "")) {
      setCoverError("顶班时段须在原班次内，结束晚于开始。"); return;
    }
    setCoverSaving(true); setCoverError("");
    try {
      await api.post("/api/v1/staff/shifts/" + encodeURIComponent(coverShift.id) + "/cover", {
        coverUserId: target.trim(), startAt: new Date(from).toISOString(), endAt: new Date(to).toISOString(),
        reason: reason.trim(), handoverNote: handover.trim(),
      });
      setActionResult("顶班已安排，按获安排时段考勤；原员工的出勤和缺勤记录保留。");
      setCoverShift(null); await reload();
    } catch (error) { setCoverError((error as Error).message); } finally { setCoverSaving(false); }
  };

  // 批量排班：一批员工 × 日期窗口内勾中的星期几 × 每日同一时段，一次生成。跨零点顺延在后端做
  // （前端只提示），口径单点在服务端，避免两边各算一套。
  const saveBatchShifts = async () => {
    const userIds = splitList(batchUsers);
    const days = Number(batchDays);
    if (!userIds.length || !batchStartDate || !batchStartTime || !batchEndTime) {
      setBatchError("请填全 员工/起始日期/每日开始与结束时刻");
      return;
    }
    if (!Number.isInteger(days) || days < 1 || days > 31) {
      setBatchError("排班天数必须是 1 到 31 之间的整数");
      return;
    }
    setBatchSaving(true);
    setBatchError("");
    try {
      const result = await api.post<{ createdCount?: number; updatedCount?: number; reviewId?: string }>("/api/v1/staff/shifts/batch", {
        userIds,
        startDate: batchStartDate,
        days,
        weekdays: batchWeekdays,
        startTime: batchStartTime,
        endTime: batchEndTime,
        pointSpaceIds: splitList(batchPoints),
        note: batchNote.trim() || null,
        nightDutyManager: batchNightDuty,
      });
      const created = result?.createdCount ?? 0;
      const updated = result?.updatedCount ?? 0;
      setBatchSummary(`本次申请新建 ${created} 个班次、修改 ${updated} 个班次，共用一张审批单，批准后整批发布。审批单：${result?.reviewId ?? "—"}`);
      setActionResult({ 结果: "整批排班已提交审批", 新建班次: created, 修改班次: updated, 审批单: result?.reviewId });
      setBatchOpen(false);
      await reload();
    } catch (e) { setBatchError((e as Error).message); } finally { setBatchSaving(false); }
  };

  // 第五批（人事域）KPI 接线：卡片数字=统一明细端点合计（与「排班/考勤」列表同谓词，平台口径）；
  // 端点失败/无权 → status=fallback，卡回退「—」并隐藏「完整明细」入口（fail-safe）。
  // 注：卡为平台口径聚合（requirePlatformScope 拦商户级/外包，放行项目级）；项目级主管看到平台口径数字（卡已标注），与其项目过滤列表口径不同=分析端点框架级边界。
  const shiftKpi = useMetricKpiValue(api, "staff.shift_count", { from: date, to: date });
  const attendanceKpi = useMetricKpiValue(api, "staff.attendance_count", { from: date, to: date });
  const lateKpi = useMetricKpiValue(api, "staff.attendance_late_count", { from: date, to: date });

  return (
    <section data-testid="staff-shift-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="staff.shift.saved / staff.attendance.punched" />
      <div className="admin-toolbar">
        <label>日期:<CnDatePicker data-testid="shift-date" value={date} onChange={(v) => setDate(v || today())} /></label>
      </div>
      <div className="admin-kpi-grid" data-testid="staff-shift-summary">
        <div className="admin-kpi-card"><span>排班班次数</span><strong className="admin-kpi-value--text" data-testid="staff-shift-kpi-count">{shiftKpi.status === "ready" && shiftKpi.value !== null ? shiftKpi.value : "—"}</strong><small>平台口径·{date}{shiftKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.shift_count" from={date} to={date} testid="staff-shift-metric-detail-count" /> : null}</small></div>
        <div className="admin-kpi-card"><span>考勤打卡数</span><strong className="admin-kpi-value--text" data-testid="staff-attendance-kpi-count">{attendanceKpi.status === "ready" && attendanceKpi.value !== null ? attendanceKpi.value : "—"}</strong><small>平台口径·{date}{attendanceKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.attendance_count" from={date} to={date} testid="staff-attendance-metric-detail-count" /> : null}</small></div>
        <div className="admin-kpi-card"><span>迟到打卡数</span><strong className="admin-kpi-value--text" data-testid="staff-late-kpi-count">{lateKpi.status === "ready" && lateKpi.value !== null ? lateKpi.value : "—"}</strong><small>平台口径·{date}{lateKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.attendance_late_count" from={date} to={date} testid="staff-late-metric-detail-count" /> : null}</small></div>
      </div>
      <h3>已发布排班（开始后锁定，修改保留历史）</h3>
      <button type="button" onClick={async () => {
        try {
          const value = await api.get<{ value: number; minValue: number | null; maxValue: number | null }>("/api/v1/staff/settings/shift-visible-days");
          setVisibility({ value: String(value.value), minValue: value.minValue == null ? "" : String(value.minValue), maxValue: value.maxValue == null ? "" : String(value.maxValue) });
          setSettingMessage("");
        } catch (error) { setSettingMessage((error as Error).message); }
      }}>设置本经营主体的排班可见天数</button>
      {visibility ? <fieldset><legend>员工可见排班天数（含今天，首次配置须填写上下限）</legend>
        {([['value', '可见天数'], ['minValue', '天数下限'], ['maxValue', '天数上限']] as const).map(([key, label]) => <label key={key}>{label}
          <input type="number" min={1} aria-label={label} value={visibility[key]} onChange={(event) => setVisibility({ ...visibility, [key]: event.target.value })} />
        </label>)}
        <label>配置事由<input aria-label="配置事由" value={settingReason} onChange={(event) => setSettingReason(event.target.value)} /></label>
        <button type="button" onClick={async () => {
          if (Object.values(visibility).some((value) => !value.trim()) || !settingReason.trim()) { setSettingMessage("请填写天数、上下限及事由。"); return; }
          try {
            await api.post("/api/v1/staff/settings/shift-visible-days", { value: Number(visibility.value), minValue: Number(visibility.minValue), maxValue: Number(visibility.maxValue), reason: settingReason.trim() });
            setSettingMessage("本经营主体的排班可见天数已保存。");
          } catch (error) { setSettingMessage((error as Error).message); }
        }}>保存可见天数</button>
      </fieldset> : null}
      {settingMessage ? <p role="status">{settingMessage}</p> : null}
      {historyText ? <pre aria-label="排班修改历史">{historyText}</pre> : null}
      {coverHistory ? <pre aria-label="顶班记录">{coverHistory}</pre> : null}
      {coverShift ? <fieldset><legend>安排临时顶班 · 原员工 {coverShift.userId}</legend>
        <p>原班次：{localTime(coverShift.startAt)} 至 {localTime(coverShift.endAt)}。记录实际交接，原员工缺勤不被抹除；顶班不直接等同加班。</p>
        <label>顶班员工<input aria-label="顶班员工" value={coverForm.userId} onChange={(e) => setCoverForm({ ...coverForm, userId: e.target.value })} /></label>
        <label>顶班开始<input aria-label="顶班开始" type="datetime-local" value={coverForm.start} onChange={(e) => setCoverForm({ ...coverForm, start: e.target.value })} /></label>
        <label>顶班结束<input aria-label="顶班结束" type="datetime-local" value={coverForm.end} onChange={(e) => setCoverForm({ ...coverForm, end: e.target.value })} /></label>
        <label>顶班事由<input aria-label="顶班事由" value={coverForm.reason} onChange={(e) => setCoverForm({ ...coverForm, reason: e.target.value })} /></label>
        <label>实际交接事实<textarea aria-label="实际交接事实" value={coverForm.handover} onChange={(e) => setCoverForm({ ...coverForm, handover: e.target.value })} /></label>
        {coverError ? <p role="alert">{coverError}</p> : null}
        <button type="button" disabled={coverSaving} onClick={() => void saveCover()}>确认安排顶班</button>
        <button type="button" disabled={coverSaving} onClick={() => setCoverShift(null)}>取消顶班填写</button>
      </fieldset> : null}
      <div className="admin-form-row" data-testid="shift-editor">
        <button type="button" data-testid="shift-create-open" onClick={() => { setEditingShift(null); setUserId(""); setStart(""); setEnd(""); setPoints(""); setNote(""); setNightDuty(false); setShiftError(""); setShiftOpen(true); }}>＋ 新增排班</button>
        <button type="button" data-testid="shift-batch-open" onClick={() => { setBatchError(""); setBatchSummary(""); setBatchOpen(true); }}>＋ 批量排班(按周)</button>
      </div>
      {batchSummary ? <p data-testid="shift-batch-result">{batchSummary}</p> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="shift-table">
            <thead><tr><th>员工</th><th>开始</th><th>结束</th><th>点位</th><th>夜班值班职责</th><th>备注</th><th>详情</th></tr></thead>
            <tbody>
              {shifts.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`shift-row-${row.id}`}
                  onClick={() => openShiftDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openShiftDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{localTime(row.startAt)}</td>
                  <td>{localTime(row.endAt)}</td>
                  <td>{row.pointSpaceIds?.length ? row.pointSpaceIds.join("、") : "未指定"}</td>
                  <td>{row.nightDutyProjectId ? "本项目夜班值班经理" : "—"}</td>
                  <td>{row.note ?? "—"}</td>
                <td className="admin-row-detail-cell">
                  <button type="button" onClick={(event) => {
                    event.stopPropagation(); setCoverShift(row); setCoverError("");
                    setCoverForm({ userId: "", start: "", end: "", reason: "", handover: "" });
                  }}>安排顶班</button>
                  <button type="button" onClick={async (event) => {
                    event.stopPropagation();
                    try {
                      const entries = await api.get<Array<{ coverUserId: string; originalUserId: string; startAt: string; endAt: string; reason: string; handoverNote: string }>>("/api/v1/staff/shifts/" + encodeURIComponent(row.id) + "/covers");
                      setCoverHistory(entries.length ? entries.map((entry) => "原员工：" + entry.originalUserId + "；顶班员工：" + entry.coverUserId + "\n安排时段：" + localTime(entry.startAt) + " ~ " + localTime(entry.endAt) + "\n事由：" + entry.reason + "\n实际交接：" + entry.handoverNote).join("\n\n") : "暂无顶班记录。");
                    } catch (error) { setCoverHistory((error as Error).message); }
                  }}>顶班记录</button>
                  {row.createdBy === user.id && new Date(row.startAt ?? "").getTime() > Date.now() ? <button type="button" onClick={(event) => {
                    event.stopPropagation(); setEditingShift(row.id); setUserId(row.userId ?? "");
                    const inputTime = (value?: string) => { const d = new Date(value ?? ""); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0,16); };
                    setStart(inputTime(row.startAt)); setEnd(inputTime(row.endAt)); setPoints((row.pointSpaceIds ?? []).join(",")); setNote(""); setNightDuty(Boolean(row.nightDutyProjectId)); setShiftOpen(true);
                  }}>申请修改</button> : null}
                  <button type="button" onClick={async (event) => {
                    event.stopPropagation();
                    try {
                      const entries = await api.get<Array<{ reviewId: string; before: ShiftRow | null; after: ShiftRow; appliedAt: string | null }>>("/api/v1/staff/shifts/" + encodeURIComponent(row.id) + "/history");
                      setHistoryText(entries.length ? entries.map((entry) => "审批单：" + entry.reviewId + "；" + (entry.appliedAt ? "已发布" : "未发布") + "\n原时间：" + localTime(entry.before?.startAt) + " ~ " + localTime(entry.before?.endAt) + "\n申请时间：" + localTime(entry.after.startAt) + " ~ " + localTime(entry.after.endAt) + "\n夜班职责：" + (entry.before?.nightDutyProjectId ?? "无") + " → " + (entry.after.nightDutyProjectId ?? "无") + "\n事由：" + (entry.after.note ?? "—")).join("\n\n") : "暂无修改历史。");
                    } catch (error) { setHistoryText((error as Error).message); }
                  }}>修改历史</button><button type="button" className="admin-row-detail-btn" aria-label="查看排班详情" onClick={(event) => { event.stopPropagation(); openShiftDetail(row); }}>查看详情</button></td></tr>
              ))}
              {shifts.length === 0 ? <tr><td colSpan={99} className="admin-empty-cell">本日暂无排班，点「＋ 新增排班」开始</td></tr> : null}
            </tbody>
          </table>
        </div>
        <h3>工作手机领取事件</h3>
        <label>查询领取事件的员工<input aria-label="查询领取事件的员工" placeholder="留空查本人" value={receiptUser} onChange={(e) => setReceiptUser(e.target.value)} /></label>
        <button type="button" onClick={async () => {
          setReceiptError(""); setReceiptRows(null);
          try { setReceiptRows(await api.get<ReceiptEvent[]>("/api/v1/staff/attendance/receipt-events" + (receiptUser.trim() ? "?user_id=" + encodeURIComponent(receiptUser.trim()) : ""))); }
          catch (error) { setReceiptError((error as Error).message); }
        }}>查看领取事件</button>
        {receiptError ? <p role="alert">{receiptError}</p> : null}
        {receiptRows ? <div className="admin-table-scroll">
          <p>手机记录时间仅为待核实证据；时间未核实的事件不直接计作上班，原始记录保留供处理。</p>
          <table className="admin-table" aria-label="工作手机领取事件">
            <thead><tr><th>事件</th><th>设备</th><th>班次</th><th>手机记录时间</th><th>已核实实际时间</th><th>方式</th><th>状态</th><th>核实与审批</th></tr></thead>
            <tbody>{receiptRows.map((row) => <tr key={row.id}><td>{row.id}</td><td>{row.deviceId}</td><td>{row.shiftId ?? "待确认"}</td>
              <td>{localTime(row.observedAt)}</td><td>{localTime(row.actualAt)}</td><td>{METHOD_LABELS[row.method] ?? row.method}</td>
              <td>{({ pending_review: "时间待核实", accepted: "已计入考勤", rejected: "未采纳" } as Record<string, string>)[row.status] ?? row.status}</td>
              <td>{row.status === "pending_review" ? <button type="button" onClick={() => {
                setReviewEvent(row); setReviewError("");
                setReviewForm({ shiftId: row.shiftId ?? "", actualAt: "", reason: "", handoverEvidence: "" });
              }}>提交实际时间核实</button> : null}
                <button type="button" onClick={async () => {
                  setReceiptError("");
                  try { setReceiptReviewHistory(await api.get<ReceiptReview[]>("/api/v1/staff/attendance/receipt-events/" + encodeURIComponent(row.id) + "/reviews")); }
                  catch (error) { setReceiptError((error as Error).message); }
                }}>核时审批历史</button>
              </td></tr>)}
              {!receiptRows.length ? <tr><td colSpan={8}>暂无领取事件</td></tr> : null}</tbody>
          </table>
        </div> : null}
        {reviewEvent ? <fieldset aria-label="实际领取时间核实">
          <legend>核实原领取事件 {reviewEvent.id}</legend>
          <p>员工：{reviewEvent.userId ?? "原事件员工"}；设备：{reviewEvent.deviceId}；原领用：{reviewEvent.contextId ?? "待核对"}。经办主管的上一级审批，不能自行指定审批人。</p>
          <label>原班次编号<input aria-label="原班次编号" value={reviewForm.shiftId} onChange={(e) => setReviewForm({ ...reviewForm, shiftId: e.target.value })} /></label>
          <label>核实的实际领取时间<input aria-label="核实的实际领取时间" type="datetime-local" value={reviewForm.actualAt} onChange={(e) => setReviewForm({ ...reviewForm, actualAt: e.target.value })} /></label>
          <label>补证原因<input aria-label="补证原因" value={reviewForm.reason} onChange={(e) => setReviewForm({ ...reviewForm, reason: e.target.value })} /></label>
          <label>实际交接核实事实<textarea aria-label="实际交接核实事实" value={reviewForm.handoverEvidence} onChange={(e) => setReviewForm({ ...reviewForm, handoverEvidence: e.target.value })} /></label>
          <p>请写明核对的原领用登记、交接人员及已确认事实。手机墙钟不作为已核实时间；审批前保持待核，驳回不会直接认定缺勤。</p>
          {reviewError ? <p role="alert">{reviewError}</p> : null}
          <button type="button" disabled={reviewSaving} onClick={() => void submitReceiptReview()}>提交上一级核时审批</button>
          <button type="button" disabled={reviewSaving} onClick={() => setReviewEvent(null)}>取消补证填写</button>
        </fieldset> : null}
        {receiptReviewHistory ? <div aria-label="核时审批历史">
          <p>审批在“主管复核”中办理。原事件和每次补证记录均保留。</p>
          {receiptReviewHistory.map((entry) => <p key={entry.id}>审批单：{entry.reviewId}；{({ pending: "待上一级审批", approved: "已批准并计入考勤", rejected: "已驳回，原时间仍待核实" } as Record<string, string>)[entry.status] ?? entry.status}；
            班次：{entry.shiftId}；拟确认时间：{localTime(entry.actualAt)}；原因：{entry.reason}；核实事实：{entry.handoverEvidence}</p>)}
          {!receiptReviewHistory.length ? <p>暂无核时申请</p> : null}
        </div> : null}
        <h3>当日考勤记录(严格无宽限判定)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="attendance-table">
            <thead><tr><th>员工</th><th>类型</th><th>已核实实际时间</th><th>方式</th><th>判定</th><th>点位</th><th>详情</th></tr></thead>
            <tbody>
              {attendance.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`attendance-row-${row.id}`}
                  onClick={() => openAttendanceDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAttendanceDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{TYPE_LABELS[row.punchType ?? ""] ?? row.punchType ?? "—"}</td>
                  <td>{localTime(row.punchedAt)}</td>
                  <td>{METHOD_LABELS[row.method ?? ""] ?? row.method ?? "—"}</td>
                  <td>{JUDGE_LABELS[row.judgement ?? ""] ?? row.judgement ?? "—"}</td>
                  <td>{row.spaceId ?? "—"}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看考勤详情" onClick={(event) => { event.stopPropagation(); openAttendanceDetail(row); }}>查看详情</button></td></tr>
              ))}
              {attendance.length === 0 ? <tr><td colSpan={99} className="admin-empty-cell">本日暂无考勤记录</td></tr> : null}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <GuidedFlowModal
        open={shiftOpen}
        title={editingShift ? "申请修改排班" : "新增排班"}
        testId="shift-create-guided"
        submitTestId="shift-save"
        submitLabel="保存排班"
        submitting={shiftSaving}
        error={shiftError || undefined}
        onClose={() => setShiftOpen(false)}
        onSubmit={() => void saveShift()}
        steps={[
          {
            title: "员工与时间",
            description: `按当前所选日期（${date}）排班；员工、开始、结束时间必填，晚于开始即迟到、早于结束即早退。`,
            validate: () => (userId.trim() && start && end ? null : "请填全 员工/开始/结束时间"),
            content: (
              <>
                <label>员工 user_id<input data-testid="shift-user-input" value={userId} disabled={Boolean(editingShift)} onChange={(event) => setUserId(event.target.value)} /></label>
                <label>开始<input data-testid="shift-start-input" type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} /></label>
                <label>结束<input data-testid="shift-end-input" type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></label>
              </>
            ),
          },
          {
            title: "点位与备注",
            description: "点位选填；修改排班必须填写原因，批准后生效。",
            // U03-009：修改排班的原因在提交前拦截（与后端 requireReason 同口径），不等后端 400。
            validate: () => (editingShift && !note.trim() ? "修改排班必须填写原因" : null),
            content: (
              <>
                <label>点位(space_id 逗号分隔)<input data-testid="shift-points-input" value={points} onChange={(event) => setPoints(event.target.value)} /></label>
                <label><input type="checkbox" data-testid="shift-night-duty-input" checked={nightDuty} onChange={(event) => setNightDuty(event.target.checked)} />承担所属项目本日夜班值班经理职责</label>
                <p>职责随排班审批生效，同项目同日一名。改派先审批取消原班职责，再审批新安排；临时顶班按已记录的交接时段承接。</p>
                <label>备注<input data-testid="shift-note-input" value={note} onChange={(event) => setNote(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "日期", value: date },
          { label: "员工", value: userId.trim() || "未填写" },
          { label: "开始", value: start ? localTime(start) : "未填写" },
          { label: "结束", value: end ? localTime(end) : "未填写" },
          { label: "点位", value: points.split(",").map((v) => v.trim()).filter(Boolean).join("、") || "未指定" },
          { label: "夜班值班职责", value: nightDuty ? "承担所属项目本日夜班值班经理职责" : "无" },
          { label: "备注", value: note.trim() || "—" },
        ]}
      />
      <GuidedFlowModal
        open={batchOpen}
        title="批量排班(一次排一周)"
        testId="shift-batch-guided"
        submitTestId="shift-batch-save"
        submitLabel="提交整批审批"
        submitting={batchSaving}
        error={batchError || undefined}
        onClose={() => setBatchOpen(false)}
        onSubmit={() => void saveBatchShifts()}
        steps={[
          {
            title: "员工与日期范围",
            description: "一次排一周:填多名员工(逗号分隔)、起始日期与天数,再勾要排的星期几;不勾=范围内每天都排。",
            validate: () => {
              if (!splitList(batchUsers).length || !batchStartDate) return "请填 员工(逗号分隔) 与 起始日期";
              const days = Number(batchDays);
              return Number.isInteger(days) && days >= 1 && days <= 31 ? null : "排班天数必须是 1 到 31 之间的整数";
            },
            content: (
              <>
                <label>员工 user_id(逗号分隔,可多名)<input data-testid="shift-batch-users-input" value={batchUsers} onChange={(event) => setBatchUsers(event.target.value)} /></label>
                <label>起始日期<CnDatePicker data-testid="shift-batch-start-date" value={batchStartDate} onChange={(v) => setBatchStartDate(v || today())} /></label>
                <label>天数(自起始日期起算,默认一周 7 天)<input data-testid="shift-batch-days-input" type="number" min={1} max={31} value={batchDays} onChange={(event) => setBatchDays(event.target.value)} /></label>
                <fieldset data-testid="shift-batch-weekdays">
                  <legend>只排这几天(不勾=范围内每天都排)</legend>
                  {WEEKDAY_LABELS.map((label, index) => (
                    <label key={label}>
                      <input type="checkbox" data-testid={`shift-batch-weekday-${index + 1}`} checked={batchWeekdays.includes(index + 1)} onChange={() => toggleWeekday(index + 1)} />
                      {label}
                    </label>
                  ))}
                </fieldset>
              </>
            ),
          },
          {
            title: "每日时段与点位",
            description: "每天同一个时段,只填几点到几点;结束时刻不晚于开始时刻=跨零点班,系统自动把结束顺延到次日,不用另选日期。",
            validate: () => (batchStartTime && batchEndTime ? null : "请填全 每日开始与结束时刻"),
            content: (
              <>
                <label>每日开始时刻<input data-testid="shift-batch-start-time" type="time" value={batchStartTime} onChange={(event) => setBatchStartTime(event.target.value)} /></label>
                <label>每日结束时刻<input data-testid="shift-batch-end-time" type="time" value={batchEndTime} onChange={(event) => setBatchEndTime(event.target.value)} /></label>
                {crossesMidnight(batchStartTime, batchEndTime)
                  ? <p data-testid="shift-batch-overnight-hint">跨零点班:结束时间为<strong>次日 {batchEndTime}</strong>(系统自动顺延到第二天,排班员不用另选日期)。</p>
                  : null}
                <label>点位(space_id 逗号分隔,整批共用)<input data-testid="shift-batch-points-input" value={batchPoints} onChange={(event) => setBatchPoints(event.target.value)} /></label>
                <label><input type="checkbox" data-testid="shift-batch-night-duty-input" checked={batchNightDuty} onChange={(event) => setBatchNightDuty(event.target.checked)} />整批承担各自所属项目的夜班值班经理职责</label>
                <p>每个项目每日只能安排一名夜班值班经理；同一项目请勿同时选多名员工。</p>
                <label>备注(整批共用)<input data-testid="shift-batch-note-input" value={batchNote} onChange={(event) => setBatchNote(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "员工", value: splitList(batchUsers).length ? `${splitList(batchUsers).join("、")}(共 ${splitList(batchUsers).length} 名)` : "未填写" },
          { label: "日期范围", value: `${batchStartDate} 起 ${batchDays || "—"} 天` },
          { label: "只排", value: batchWeekdays.length ? batchWeekdays.map((day) => WEEKDAY_LABELS[day - 1]).join("、") : "范围内每天" },
          { label: "每日时段", value: `${batchStartTime || "未填写"} 至 ${batchEndTime || "未填写"}${crossesMidnight(batchStartTime, batchEndTime) ? "(次日)" : ""}` },
          { label: "点位", value: splitList(batchPoints).join("、") || "未指定" },
          { label: "夜班值班职责", value: batchNightDuty ? "整批承担，按项目每日一名" : "无" },
          { label: "备注", value: batchNote.trim() || "—" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
