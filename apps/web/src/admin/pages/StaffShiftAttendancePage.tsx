import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
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

  const saveShift = async () => {
    if (!userId.trim() || !start || !end) {
      setGlobalError("请填全 员工/开始/结束时间");
      return;
    }
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
    await reload();
  };

  return (
    <section data-testid="staff-shift-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="staff.shift.saved / staff.attendance.punched" />
      <div className="admin-toolbar">
        <label>日期:<CnDatePicker data-testid="shift-date" value={date} onChange={(v) => setDate(v || today())} /></label>
      </div>
      <h3>排班(同员工同开始时刻重复保存=覆盖修改)</h3>
      <div className="admin-form-grid" data-testid="shift-editor">
        <label>员工 user_id<input data-testid="shift-user-input" value={userId} onChange={(event) => setUserId(event.target.value)} /></label>
        <label>开始<input data-testid="shift-start-input" type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} /></label>
        <label>结束<input data-testid="shift-end-input" type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></label>
        <label>点位(space_id 逗号分隔)<input data-testid="shift-points-input" value={points} onChange={(event) => setPoints(event.target.value)} /></label>
        <label>备注<input data-testid="shift-note-input" value={note} onChange={(event) => setNote(event.target.value)} /></label>
        <button type="button" data-testid="shift-save" onClick={() => void saveShift()}>保存排班</button>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="shift-table">
            <thead><tr><th>员工</th><th>开始</th><th>结束</th><th>点位</th><th>备注</th></tr></thead>
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3>当日考勤记录(严格无宽限判定)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="attendance-table">
            <thead><tr><th>员工</th><th>类型</th><th>打卡时间(服务器)</th><th>方式</th><th>判定</th><th>点位</th></tr></thead>
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
