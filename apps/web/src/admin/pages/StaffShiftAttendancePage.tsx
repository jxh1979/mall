import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

type ShiftRow = { id: string; userId?: string; workDate?: string; startAt?: string; endAt?: string; pointSpaceIds?: string[]; note?: string };
type AttendanceRow = { id: string; userId?: string; punchType?: string; punchedAt?: string; method?: string; judgement?: string; spaceId?: string; deviceFingerprint?: string };

const TEXT = {
  title: "排班与考勤",
  description: "主管排班到分钟并指定点位(后台与手机双端同一套规则);员工手机 NFC/扫码打卡;严格考勤:晚于班次开始即迟到、早于结束即早退,只记录不自动扣罚。",
  loadFailed: "排班考勤读取失败",
};

const JUDGE_LABELS: Record<string, string> = { normal: "正常", late: "迟到", early_leave: "早退", no_shift: "无班次" };
const METHOD_LABELS: Record<string, string> = { nfc: "NFC", qr: "扫码", manual: "手动" };
const TYPE_LABELS: Record<string, string> = { check_in: "上班", check_out: "下班" };

function localTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—";
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult }: AdminPageProps) {
  const [date, setDate] = React.useState(today);
  const [shifts, setShifts] = React.useState<ShiftRow[]>([]);
  const [attendance, setAttendance] = React.useState<AttendanceRow[]>([]);
  const [userId, setUserId] = React.useState("");
  const [start, setStart] = React.useState("");
  const [end, setEnd] = React.useState("");
  const [points, setPoints] = React.useState("");
  const [note, setNote] = React.useState("");
  // 引导式「新增排班」弹窗状态（范式：字段留页面持有，弹窗只编排）。
  const [shiftOpen, setShiftOpen] = React.useState(false);
  const [shiftError, setShiftError] = React.useState("");
  const [shiftSaving, setShiftSaving] = React.useState(false);
  // 行明细抽屉：整行可点 → 该排班/考勤完整字段（枚举中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openShiftDetail(row: ShiftRow) {
    setDetail({
      title: `排班明细 · ${row.userId ?? row.id}`,
      fields: [
        { label: "员工", value: row.userId ?? "—" },
        { label: "开始", value: localTime(row.startAt) },
        { label: "结束", value: localTime(row.endAt) },
        { label: "点位", value: row.pointSpaceIds?.length ? row.pointSpaceIds.join("、") : "未指定" },
        { label: "备注", value: row.note ?? "—" },
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
        { label: "打卡时间(服务器)", value: localTime(row.punchedAt) },
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
      await api.post("/api/v1/staff/shifts", {
        userId: userId.trim(),
        workDate: date,
        startAt: new Date(start).toISOString(),
        endAt: new Date(end).toISOString(),
        pointSpaceIds: points.split(",").map((value) => value.trim()).filter(Boolean),
        note: note.trim() || null,
      });
      setActionResult({ action: "staff_shift_saved", userId: userId.trim(), workDate: date });
      setUserId(""); setStart(""); setEnd(""); setPoints(""); setNote("");
      setShiftOpen(false);
      await reload();
    } catch (e) { setShiftError((e as Error).message); } finally { setShiftSaving(false); }
  };

  return (
    <section data-testid="staff-shift-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="staff.shift.saved / staff.attendance.punched" />
      <div className="admin-toolbar">
        <label>日期:<CnDatePicker data-testid="shift-date" value={date} onChange={(v) => setDate(v || today())} /></label>
      </div>
      <h3>排班(同员工同开始时刻重复保存=覆盖修改)</h3>
      <div className="admin-form-row" data-testid="shift-editor">
        <button type="button" data-testid="shift-create-open" onClick={() => { setUserId(""); setStart(""); setEnd(""); setPoints(""); setNote(""); setShiftError(""); setShiftOpen(true); }}>＋ 新增排班</button>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="shift-table">
            <thead><tr><th>员工</th><th>开始</th><th>结束</th><th>点位</th><th>备注</th><th>详情</th></tr></thead>
            <tbody>
              {shifts.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`shift-row-${row.id}`}
                  onClick={() => openShiftDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openShiftDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{localTime(row.startAt)}</td>
                  <td>{localTime(row.endAt)}</td>
                  <td>{row.pointSpaceIds?.length ? row.pointSpaceIds.join("、") : "未指定"}</td>
                  <td>{row.note ?? "—"}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看排班详情" onClick={(event) => { event.stopPropagation(); openShiftDetail(row); }}>查看详情</button></td></tr>
              ))}
              {shifts.length === 0 ? <tr><td colSpan={99} className="admin-empty-cell">本日暂无排班，点「＋ 新增排班」开始</td></tr> : null}
            </tbody>
          </table>
        </div>
        <h3>当日考勤记录(严格无宽限判定)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="attendance-table">
            <thead><tr><th>员工</th><th>类型</th><th>打卡时间(服务器)</th><th>方式</th><th>判定</th><th>点位</th><th>详情</th></tr></thead>
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
        title="新增排班"
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
                <label>员工 user_id<input data-testid="shift-user-input" value={userId} onChange={(event) => setUserId(event.target.value)} /></label>
                <label>开始<input data-testid="shift-start-input" type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} /></label>
                <label>结束<input data-testid="shift-end-input" type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></label>
              </>
            ),
          },
          {
            title: "点位与备注",
            description: "点位（space_id，逗号分隔）与备注选填。",
            content: (
              <>
                <label>点位(space_id 逗号分隔)<input data-testid="shift-points-input" value={points} onChange={(event) => setPoints(event.target.value)} /></label>
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
          { label: "备注", value: note.trim() || "—" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
