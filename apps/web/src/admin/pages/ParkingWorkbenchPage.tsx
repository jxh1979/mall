// 文件版本：V0.01
import React from "react";
import { Tabs } from "antd";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, GovernanceProjectRow } from "../types";

// HP-P01 停车场工作台（BSM「停车场」L1425–L1452）：业务参数按项目、值与上下限同配（L1434～L1437，出厂默认值实施前补定）；
// 在场与出场记录、缴费回写与车位占用由第三方停车系统回传（L1433／L1444）；月租车牌与员工车辆在本平台登记并下发（L1428／L1437）；
// 人工放行由当班保安登记原因、车牌，免费放行和减免须经物业主管审批（L1438）；
// 消费抵免台账列出抵免、构成资格的消费及其归属主体与按组成比例的成本分担（「消费抵免」条）。

type Unit = "minutes" | "yuan" | "percent";
const PARAMS: Array<[string, string, Unit]> = [
  ["freeMinutes", "免费时长（分钟）", "minutes"],
  ["hourlyRateCent", "超出后每小时费率（元）", "yuan"],
  ["dailyCapCent", "每日封顶（元）", "yuan"],
  ["exitGraceMinutes", "离场宽限时限（分钟）", "minutes"],
  ["creditThresholdCent", "消费抵免门槛（元）", "yuan"],
  ["creditMinutes", "抵免时长（分钟）", "minutes"],
  ["staffDiscountBp", "员工车辆优惠（%，100＝免费）", "percent"],
];
const SUFFIXES: Array<[string, string]> = [["", ""], ["Min", "下限"], ["Max", "上限"]];
const FACTOR: Record<Unit, number> = { minutes: 1, yuan: 100, percent: 100 };
const RELEASE_REASONS: Array<[string, string]> = [["recognition_failed", "识别失败"], ["gate_fault", "道闸故障"], ["fee_dispute", "缴费争议"]];
const WAIVE: Array<[string, string]> = [["none", "不减免"], ["free", "免费放行"], ["reduction", "减免"]];
const LABELS: Record<string, string> = {
  in_lot: "在场", exited: "已出场", temporary: "临停", monthly: "月租", staff: "员工", active: "有效", revoked: "已撤销",
  released: "已放行", pending_approval: "待物业主管审批", approved: "已批准", rejected: "已驳回", none: "不减免", free: "免费放行",
  reduction: "减免", available: "可用", used: "已使用", bar_bill: "酒吧账单", service_order: "服务点单",
  ...Object.fromEntries(RELEASE_REASONS),
};
const zh = (v?: string | null) => (v ? LABELS[v] ?? v : "-");
const yuan = (cent?: number | null) => (cent == null ? "-" : `¥${(cent / 100).toFixed(2)}`);
const time = (iso?: string | null) => (iso ? new Date(iso).toLocaleString("zh-CN", { hour12: false }) : "-");
const key = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

type Session = { id: string; plateNo?: string | null; tempCode?: string | null; vehicleKind: string; enteredAt: string;
  exitedAt?: string | null; vendorFeeCent?: number | null; vendorPaidCent?: number | null; platformPaidCent: number; status: string };
type Overview = { occupancy?: { totalSpaces: number; occupiedTotal: number; occupiedMonthly: number; occupiedStaff: number;
  reportedAt: string } | null; sessions: Session[] };
type Vehicle = { id: string; contractId?: string | null; userId?: string | null; plateNo: string; status: string; registeredAt: string };
type Release = { id: string; plateNo: string; releaseReason: string; reason: string; operatorId: string; releasedAt: string;
  waiveType: string; reductionCent?: number | null; status: string };
type Page<T> = { items: T[] };
type Credit = { id: string; customerId: string; businessDate: string; thresholdCent: number; creditMinutes: number; eligibleAmountCent: number;
  status: string; offsetHours?: number | null; costCent?: number | null;
  components: Array<{ sourceType: string; sourceId: string; merchantId: string; merchantName?: string | null; amountCent: number }>;
  allocations: Array<{ merchantId?: string | null; shareCent: number; costMonth: string }> };

/** 写动作通用：忙碌、报错、成功后刷新。 */
function useWrite(props: AdminPageProps, reload: () => void) {
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const run = async (action: string, request: Record<string, unknown>, call: () => Promise<unknown>) => {
    setBusy(true);
    setError("");
    try {
      const result = await call();
      props.setActionResult({ action, idempotencyKey: request.idempotencyKey, result });
      reload();
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败");
      return false;
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, setError, run };
}

function PolicyPanel({ props, project }: { props: AdminPageProps; project: string }) {
  const [form, setForm] = React.useState<Record<string, string>>({});
  const [notice, setNotice] = React.useState("");
  const load = React.useCallback(() => {
    props.api.get<Record<string, number>>(`/api/v1/parking/policies/${encodeURIComponent(project)}`).then((policy) => {
      const next: Record<string, string> = {};
      for (const [name, , unit] of PARAMS) for (const [suffix] of SUFFIXES) {
        const v = policy[`${name}${suffix}`];
        next[`${name}${suffix}`] = v == null ? "" : String(v / FACTOR[unit]);
      }
      setForm(next);
      setNotice("");
    }).catch(() => { setForm({}); setNotice("本项目尚未配置停车业务参数：停车业务暂不可用，出厂默认值实施前补定。"); });
  }, [props.api, project]);
  React.useEffect(load, [load]);
  const write = useWrite(props, load);

  async function save() {
    const body: Record<string, unknown> = { idempotencyKey: key("parking-policy") };
    for (const [name, label, unit] of PARAMS) for (const [suffix, text] of SUFFIXES) {
      const raw = (form[`${name}${suffix}`] ?? "").trim();
      if (raw === "" || !Number.isFinite(Number(raw))) {
        write.setError(`停车业务参数须同时填写值及上下限：${label}${text}`);
        return;
      }
      body[`${name}${suffix}`] = Math.round(Number(raw) * FACTOR[unit]);
    }
    await write.run("parking_policy_saved", body, () => props.api.put(`/api/v1/parking/policies/${encodeURIComponent(project)}`, body));
  }

  return (
    <div data-testid="parking-policy-form">
      {notice ? <p className="admin-muted-action" data-testid="parking-policy-unconfigured">{notice}</p> : null}
      <table className="admin-table">
        <thead><tr><th>业务参数</th><th>值</th><th>下限</th><th>上限</th></tr></thead>
        <tbody>
          {PARAMS.map(([name, label]) => (
            <tr key={name}>
              <td>{label}</td>
              {SUFFIXES.map(([suffix, text]) => (
                <td key={suffix}><input aria-label={`${label}${text}`} type="number" min="0" value={form[`${name}${suffix}`] ?? ""}
                  onChange={(e) => setForm({ ...form, [`${name}${suffix}`]: e.target.value })} /></td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {write.error ? <p className="admin-form-error" data-testid="parking-policy-error">{write.error}</p> : null}
      <PermissionButton permissionsState={props.effectivePermissions} permissions={["parking.config.manage"]}
        data-testid="parking-policy-save" disabled={write.busy} onClick={save}>保存本项目停车参数</PermissionButton>
    </div>
  );
}

function OverviewPanel({ props, project }: { props: AdminPageProps; project: string }) {
  const [data, setData] = React.useState<Overview | null>(null);
  const [error, setError] = React.useState("");
  React.useEffect(() => {
    props.api.get<Overview>(`/api/v1/parking/overview?project_id=${encodeURIComponent(project)}`)
      .then((d) => { setData(d); setError(""); }).catch((err) => setError(err instanceof Error ? err.message : "读取失败"));
  }, [props.api, project]);
  const o = data?.occupancy;
  return (
    <div data-testid="parking-overview">
      {error ? <p className="admin-form-error">{error}</p> : null}
      <p>{o ? `最近车位占用（停车系统回传 ${time(o.reportedAt)}）：在场 ${o.occupiedTotal}／车位 ${o.totalSpaces}，其中月租 ${o.occupiedMonthly}、员工 ${o.occupiedStaff}`
        : "停车系统尚未回传车位占用"}</p>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="parking-sessions-table">
          <thead><tr><th>车牌／临时码</th><th>车辆类型</th><th>入场</th><th>出场</th><th>停车系统应收</th><th>停车系统自收</th><th>平台已收</th><th>状态</th></tr></thead>
          <tbody>
            {(data?.sessions ?? []).map((s) => (
              <tr key={s.id}><td>{s.plateNo ?? `临时码 ${s.tempCode ?? "-"}`}</td><td>{zh(s.vehicleKind)}</td><td>{time(s.enteredAt)}</td>
                <td>{time(s.exitedAt)}</td><td>{yuan(s.vendorFeeCent)}</td><td>{yuan(s.vendorPaidCent)}</td><td>{yuan(s.platformPaidCent)}</td>
                <td>{zh(s.status)}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/** 员工车辆：平台工作台与商户专区共用（商户账号后端只放本商户员工）。 */
export function ParkingStaffVehiclesPanel({ props, project }: { props: AdminPageProps; project: string }) {
  const [rows, setRows] = React.useState<Vehicle[]>([]);
  const [userId, setUserId] = React.useState("");
  const [plate, setPlate] = React.useState("");
  const load = React.useCallback(() => {
    props.api.get<Page<Vehicle>>(`/api/v1/parking/staff-vehicles?project_id=${encodeURIComponent(project)}`)
      .then((p) => setRows(p?.items ?? [])).catch(() => setRows([]));
  }, [props.api, project]);
  React.useEffect(load, [load]);
  const write = useWrite(props, load);
  const register = async () => {
    if (!userId.trim() || !plate.trim()) { write.setError("员工账号与车牌必填。"); return; }
    const body = { idempotencyKey: key("parking-staff-vehicle"), projectId: project, userId: userId.trim(), plateNo: plate.trim() };
    if (await write.run("parking_staff_vehicle_registered", body, () => props.api.post("/api/v1/parking/staff-vehicles", body))) {
      setUserId(""); setPlate("");
    }
  };
  const revoke = (id: string) => {
    const body = { idempotencyKey: key("parking-staff-vehicle-revoke") };
    void write.run("parking_staff_vehicle_revoked", body, () => props.api.post(`/api/v1/parking/staff-vehicles/${encodeURIComponent(id)}/revoke`, body));
  };
  return (
    <div data-testid="parking-staff-vehicle-form">
      <h3>员工车辆（按本项目员工优惠下发；员工离职或停用后不再下发）</h3>
      <div className="admin-form-row">
        <label>员工账号<input aria-label="员工账号" value={userId} onChange={(e) => setUserId(e.target.value)} /></label>
        <label>员工车牌<input aria-label="员工车牌" value={plate} onChange={(e) => setPlate(e.target.value)} /></label>
        <PermissionButton permissionsState={props.effectivePermissions} permissions={["parking.vehicle.manage"]}
          data-testid="parking-staff-vehicle-save" disabled={write.busy} onClick={register}>登记员工车辆</PermissionButton>
      </div>
      {write.error ? <p className="admin-form-error">{write.error}</p> : null}
      <table className="admin-table" data-testid="parking-staff-vehicles-table">
        <thead><tr><th>车牌</th><th>员工账号</th><th>登记时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {rows.map((v) => (
            <tr key={v.id}><td>{v.plateNo}</td><td>{v.userId}</td><td>{time(v.registeredAt)}</td><td>{zh(v.status)}</td>
              <td>{v.status === "active" ? <PermissionButton permissionsState={props.effectivePermissions}
                permissions={["parking.vehicle.manage"]} disabled={write.busy} onClick={() => revoke(v.id)}>撤销</PermissionButton> : "-"}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MonthlyVehiclesPanel({ props, project }: { props: AdminPageProps; project: string }) {
  const [rows, setRows] = React.useState<Vehicle[]>([]);
  const [contractId, setContractId] = React.useState("");
  const [plate, setPlate] = React.useState("");
  const load = React.useCallback(() => {
    props.api.get<Page<Vehicle>>(`/api/v1/parking/monthly-vehicles?project_id=${encodeURIComponent(project)}`)
      .then((p) => setRows(p?.items ?? [])).catch(() => setRows([]));
  }, [props.api, project]);
  React.useEffect(load, [load]);
  const write = useWrite(props, load);
  const register = async () => {
    if (!contractId.trim() || !plate.trim()) { write.setError("合同编号与车牌必填。"); return; }
    const body = { idempotencyKey: key("parking-monthly-vehicle"), plateNo: plate.trim() };
    if (await write.run("parking_monthly_vehicle_registered", body,
      () => props.api.post(`/api/v1/parking/contracts/${encodeURIComponent(contractId.trim())}/vehicles`, body))) {
      setContractId(""); setPlate("");
    }
  };
  const revoke = (id: string) => {
    const body = { idempotencyKey: key("parking-monthly-vehicle-revoke") };
    void write.run("parking_monthly_vehicle_revoked", body, () => props.api.post(`/api/v1/parking/monthly-vehicles/${encodeURIComponent(id)}/revoke`, body));
  };
  return (
    <div data-testid="parking-monthly-vehicle-form">
      <h3>商户月租车牌（合同须约定车位月租；生效且在租期内的才下发给停车系统）</h3>
      <div className="admin-form-row">
        <label>合同编号<input aria-label="合同编号" value={contractId} onChange={(e) => setContractId(e.target.value)} /></label>
        <label>月租车牌<input aria-label="月租车牌" value={plate} onChange={(e) => setPlate(e.target.value)} /></label>
        <PermissionButton permissionsState={props.effectivePermissions} permissions={["parking.vehicle.manage"]}
          data-testid="parking-monthly-vehicle-save" disabled={write.busy} onClick={register}>登记月租车牌</PermissionButton>
      </div>
      {write.error ? <p className="admin-form-error">{write.error}</p> : null}
      <table className="admin-table" data-testid="parking-monthly-vehicles-table">
        <thead><tr><th>车牌</th><th>合同</th><th>登记时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {rows.map((v) => (
            <tr key={v.id}><td>{v.plateNo}</td><td>{v.contractId}</td><td>{time(v.registeredAt)}</td><td>{zh(v.status)}</td>
              <td>{v.status === "active" ? <PermissionButton permissionsState={props.effectivePermissions}
                permissions={["parking.vehicle.manage"]} disabled={write.busy} onClick={() => revoke(v.id)}>撤销</PermissionButton> : "-"}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** 消费抵免台账：同一客人当日一条；已使用的按实际抵掉小时×每小时内部结算价计成本，按组成比例分担，尾差由城市项目公司承担。 */
function CreditsPanel({ props, project }: { props: AdminPageProps; project: string }) {
  const [rows, setRows] = React.useState<Credit[]>([]);
  React.useEffect(() => {
    // 分页信封经 adminClient.get 拍平为条目数组。
    props.api.get<Credit[]>(`/api/v1/parking/credits?project_id=${encodeURIComponent(project)}`)
      .then((p) => setRows(Array.isArray(p) ? p : [])).catch(() => setRows([]));
  }, [props.api, project]);
  const merchantName = (c: Credit, id?: string | null) => c.components.find((x) => x.merchantId === id)?.merchantName ?? id ?? "-";
  return (
    <div data-testid="parking-credits">
      <p className="admin-muted-action">门槛只算酒吧账单与商户有效消费（门票不计入）；免费时长内离场、没抵掉计费时长的不算用掉。消费退款后未使用的重核或撤销，已使用的保留原承担。</p>
      <table className="admin-table" data-testid="parking-credits-table">
        <thead><tr><th>营业日</th><th>客人</th><th>状态</th><th>已累计有效消费</th><th>门槛</th><th>抵免时长</th><th>实际抵掉</th><th>抵免成本</th><th>构成消费与成本分担</th></tr></thead>
        <tbody>
          {rows.map((c) => (
            <tr key={c.id} data-testid={`parking-credit-${c.id}`}>
              <td>{c.businessDate}</td><td>{c.customerId}</td><td>{zh(c.status)}</td><td>{yuan(c.eligibleAmountCent)}</td>
              <td>{yuan(c.thresholdCent)}</td><td>{c.creditMinutes} 分钟</td><td>{c.offsetHours ? `${c.offsetHours} 小时` : "-"}</td>
              <td>{yuan(c.costCent)}</td>
              <td data-testid={`parking-credit-detail-${c.id}`}>
                {c.components.map((x) => <div key={x.sourceId}>{zh(x.sourceType)} {x.merchantName ?? x.merchantId} {yuan(x.amountCent)}</div>)}
                {c.allocations.map((a) => <div key={a.merchantId ?? "residual"}>
                  {a.merchantId ? `${merchantName(c, a.merchantId)} 承担 ${yuan(a.shareCent)}` : `尾差（城市项目公司承担）${yuan(a.shareCent)}`}</div>)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ReleasePanel({ props, project }: { props: AdminPageProps; project: string }) {
  const empty = { plateNo: "", releaseReason: "recognition_failed", reason: "", waiveType: "none", reductionYuan: "", finalApproverId: "" };
  const [rows, setRows] = React.useState<Release[]>([]);
  const [form, setForm] = React.useState(empty);
  const load = React.useCallback(() => {
    props.api.get<Page<Release>>(`/api/v1/parking/manual-releases?project_id=${encodeURIComponent(project)}`)
      .then((p) => setRows(p?.items ?? [])).catch(() => setRows([]));
  }, [props.api, project]);
  React.useEffect(load, [load]);
  const write = useWrite(props, load);
  const waived = form.waiveType !== "none";
  const save = async () => {
    if (!form.plateNo.trim() || !form.reason.trim()) { write.setError("车牌与核对说明必填。"); return; }
    if (waived && !form.finalApproverId.trim()) { write.setError("免费放行或减免须指定审批的物业主管。"); return; }
    if (form.waiveType === "reduction" && !(Number(form.reductionYuan) > 0)) { write.setError("减免金额须大于 0。"); return; }
    const body: Record<string, unknown> = { idempotencyKey: key("parking-release"), projectId: project, plateNo: form.plateNo.trim(),
      releaseReason: form.releaseReason, reason: form.reason.trim(), waiveType: form.waiveType };
    if (form.waiveType === "reduction") body.reductionCent = Math.round(Number(form.reductionYuan) * 100);
    if (waived) body.finalApproverId = form.finalApproverId.trim();
    if (await write.run("parking_manual_release_recorded", body, () => props.api.post("/api/v1/parking/manual-releases", body))) setForm(empty);
  };
  return (
    <div data-testid="parking-release-form">
      <p className="admin-muted-action">由当班保安人工核对后登记；操作人与时间由系统记录，登记即进异常队列。免费放行和减免须经物业主管审批。</p>
      <div className="admin-form-row">
        <label>车牌<input aria-label="车牌" value={form.plateNo} onChange={(e) => setForm({ ...form, plateNo: e.target.value })} /></label>
        <label>放行原因<select aria-label="放行原因" value={form.releaseReason} onChange={(e) => setForm({ ...form, releaseReason: e.target.value })}>
          {RELEASE_REASONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></label>
        <label>核对说明<input aria-label="核对说明" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></label>
        <label>免费或减免<select aria-label="免费或减免" value={form.waiveType} onChange={(e) => setForm({ ...form, waiveType: e.target.value })}>
          {WAIVE.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></label>
        {form.waiveType === "reduction" ? <label>减免金额（元）<input aria-label="减免金额（元）" type="number" min="0" value={form.reductionYuan}
          onChange={(e) => setForm({ ...form, reductionYuan: e.target.value })} /></label> : null}
        {waived ? <label>审批的物业主管账号<input aria-label="审批的物业主管账号" value={form.finalApproverId}
          onChange={(e) => setForm({ ...form, finalApproverId: e.target.value })} /></label> : null}
        <PermissionButton permissionsState={props.effectivePermissions} permissions={["parking.release.record"]}
          data-testid="parking-release-save" disabled={write.busy} onClick={save}>登记人工放行</PermissionButton>
      </div>
      {write.error ? <p className="admin-form-error">{write.error}</p> : null}
      <table className="admin-table" data-testid="parking-releases-table">
        <thead><tr><th>车牌</th><th>原因</th><th>说明</th><th>操作人</th><th>时间</th><th>免费或减免</th><th>状态</th></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}><td>{r.plateNo}</td><td>{zh(r.releaseReason)}</td><td>{r.reason}</td><td>{r.operatorId}</td><td>{time(r.releasedAt)}</td>
              <td>{r.waiveType === "reduction" ? `减免 ${yuan(r.reductionCent)}` : zh(r.waiveType)}</td><td>{zh(r.status)}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function ParkingWorkbenchPage(props: AdminPageProps) {
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [project, setProject] = React.useState(props.user.organizationId ?? "");
  React.useEffect(() => {
    props.api.get<GovernanceProjectRow[]>("/api/v1/platform/projects").then((rows) => {
      setProjects(rows ?? []);
      setProject((current) => current || rows?.[0]?.id || "");
    }).catch(() => setProjects([]));
  }, [props.api]);
  return (
    <section data-testid="parking-workbench-page">
      <AdminPageHeader title="停车场工作台" description="停车业务参数按项目配置并下发给第三方停车系统；在场记录、缴费结果与车位占用由停车系统回传；月租车牌、员工车辆与人工放行在此登记。" />
      <label>综合体项目<select aria-label="综合体项目" value={project} onChange={(e) => setProject(e.target.value)}>
        {projects.map((p) => <option key={p.id} value={p.id}>{p.name ?? p.id}</option>)}
        {project && !projects.some((p) => p.id === project) ? <option value={project}>{project}</option> : null}
      </select></label>
      {project ? (
        <Tabs key={project} items={[
          { key: "policy", label: "参数配置", children: <PolicyPanel props={props} project={project} /> },
          { key: "overview", label: "在场与记录", children: <OverviewPanel props={props} project={project} /> },
          { key: "credits", label: "消费抵免台账", children: <CreditsPanel props={props} project={project} /> },
          { key: "vehicles", label: "月租与员工车辆", children: (<>
            <MonthlyVehiclesPanel props={props} project={project} /><ParkingStaffVehiclesPanel props={props} project={project} /></>) },
          { key: "release", label: "人工放行", children: <ReleasePanel props={props} project={project} /> },
        ]} />
      ) : null}
    </section>
  );
}

/** 商户专区：所属主体为本商户员工登记车牌（BSM L1437）。 */
export function MerchantZoneStaffVehiclesPage(props: AdminPageProps) {
  const project = props.user.organizationId ?? "";
  return (
    <section data-testid="merchant-zone-staff-vehicles-page">
      <AdminPageHeader title="员工车辆" description="为本商户员工登记车牌，停车场按项目配置给予免费或折扣；员工离职或停用后不再享受。" />
      {project ? <ParkingStaffVehiclesPanel props={props} project={project} /> : <p className="admin-muted-action">当前账号未归属综合体项目。</p>}
    </section>
  );
}
