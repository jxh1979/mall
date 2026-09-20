import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { Can, PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import { IconButton } from "../components/IconButton";
import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

// 平面图导入域内枚举中文（全局状态表未含的形状/关联类型，页内映射；状态码走全局表兜底）。
const SHAPE_TYPE_LABELS: Record<string, string> = {
  shop: "铺位",
  booth: "摊位",
  public_area: "公共区",
  area: "区域",
  entrance: "入口",
  facility: "设施",
  risk_zone: "风险区",
  route: "路径",
};
const SOURCE_FORMAT_OPTIONS: { value: string; label: string }[] = [
  { value: "svg", label: "矢量图" },
  { value: "standard_json", label: "标准数据" },
];
// M018-002：页内枚举字典与后端闭集一致（FloorPlanImportParser 错误字段/错误码、FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES / REF_TYPES），
// 不再走全局码表（无 parsed/archived 键，published 在全局表是「已上架」）。
const ERROR_FIELD_LABELS: Record<string, string> = {
  source_format: "导入格式",
  viewBox: "画布范围",
  points: "顶点",
  polygon_points: "多边形顶点",
  shapes: "图形列表",
  source_payload: "来源内容",
};
const ERROR_CODE_LABELS: Record<string, string> = {
  PAYLOAD_REQUIRED: "缺少来源内容",
  UNSUPPORTED_SOURCE_FORMAT: "不支持的导入格式",
  VIEWBOX_REQUIRED: "缺少画布范围",
  POINTS_REQUIRED: "缺少顶点",
  POINTS_TOO_FEW: "顶点不足",
  POLYLINE_NOT_CLOSED: "折线未闭合",
  POINT_OUT_OF_RANGE: "顶点越界",
  SHAPES_REQUIRED: "缺少图形列表",
  JSON_PARSE_FAILED: "数据解析失败",
  NO_SHAPES_FOUND: "未找到图形",
};
const FLOOR_PLAN_STATUS_LABELS: Record<string, string> = { active: "启用", inactive: "停用", archived: "已存档" };
const IMPORT_TASK_STATUS_LABELS: Record<string, string> = { parsed: "已解析", failed: "解析失败", published: "已发布", cancelled: "已取消" };
const CANDIDATE_STATUS_LABELS: Record<string, string> = { pending: "待复核", published: "已发布", rejected: "已驳回" };
const zhOrDash = (map: Record<string, string>, code: string | null | undefined) =>
  code ? map[code] ?? "未识别" : "-";
const REF_TYPE_LABELS: Record<string, string> = {
  space: "空间", merchant: "商户", project: "综合体项目", amusement_project: "游乐项目", device: "设备", custom: "自定义",
};
const fpLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? "未识别" : "-");
function floorPlanStatusLabel(status?: string) {
  return fpLabel(FLOOR_PLAN_STATUS_LABELS, status);
}

// 后端 floorplan 域记录映射(接现有 /api/v1/floor-plans/* 契约)。
type FloorPlan = { id: string; projectId?: string; buildingCode?: string; floorNo?: string; planName?: string; versionNo?: string; status?: string };
type ImportTask = { id: string; floorPlanId?: string; sourceFormat?: string; sourceName?: string; status?: string; candidateCount?: number; errorCount?: number };
type ImportCandidate = { id: string; candidateNo?: number; shapeType?: string; refType?: string; refId?: string; label?: string; status?: string };
type ImportError = { id: string; lineNo?: number; fieldName?: string; errorCode?: string; errorMessage?: string };
type FloorPlanImportDetails = { task?: ImportTask; candidates?: ImportCandidate[]; errors?: ImportError[] };

const MANAGE = ["floor_plan.manage"];
// M018-003：导入任务/候选的读写码与后端 FloorPlanController 一致（floor_plan.import.view / floor_plan.import.manage，V034 独立种子）。
const IMPORT_VIEW = ["floor_plan.import.view"];
const IMPORT_MANAGE = ["floor_plan.import.manage"];
// M018-001：新增平面图表单字段与后端 validateCreateRequest / V023 必填列一致。
const EMPTY_PLAN_FORM = { projectId: "", planName: "", buildingCode: "", floorNo: "", imageUrl: "", imageWidth: "", imageHeight: "", versionNo: "" };
// 高危动作二次确认标题(发布候选写入正式平面图区域,不可静默覆盖):由 HighRiskConfirm 弹窗承载。
const PUBLISH_CONFIRM_TITLE = "发布平面图导入候选";

/**
 * P2 平面图导入(商场平面图):上传/导入商场平面布局,解析出候选区域(铺位/摊位/公共区),
 * 人工复核候选与错误后,发布到正式平面图区域。发布=高危动作,经 confirmHighImpactAction 二次确认。
 * 2026-06 改版丢失前端后重建为独立 admin 页(接现有 floorplan 后端)。
 * 行明细抽屉：候选/错误=业务明细数据行，整行可点看该候选/错误完整字段（形状/关联/状态中文·错误说明）；
 * 平面图/导入任务两表为主从入口（行内已有「查看导入」「候选明细」下钻按钮），不重复接明细。
 * KPI 说明：本页为向导式主从页(平面图→导入任务→候选/错误),KPI 卡(待发布候选/导入错误)只在选中导入后
 * 的明细上下文出现,直接紧贴其候选/错误表,无「页面级汇总表」可聚焦,故不做 KPI 聚焦重进(与分析页同理)。
 */
export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [floorPlans, setFloorPlans] = React.useState<FloorPlan[]>([]);
  const [selectedPlanId, setSelectedPlanId] = React.useState("");
  const [imports, setImports] = React.useState<ImportTask[]>([]);
  const [selectedImportId, setSelectedImportId] = React.useState("");
  const [floorPlanImportDetails, setFloorPlanImportDetails] = React.useState<FloorPlanImportDetails | null>(null);
  const [planForm, setPlanForm] = React.useState(EMPTY_PLAN_FORM);
  const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
  const [projectsNotice, setProjectsNotice] = React.useState<string | null>(null);
  const planKeyRef = React.useRef("");
  const importKeyRef = React.useRef("");
  const publishKeyRef = React.useRef("");
  // 引导式「新增平面图」弹窗状态（范式：字段留页面持有，弹窗只编排）。
  const [planOpen, setPlanOpen] = React.useState(false);
  const [planError, setPlanError] = React.useState("");
  const [planSaving, setPlanSaving] = React.useState(false);
  const [importOpen, setImportOpen] = React.useState(false);
  const [importError, setImportError] = React.useState("");
  const [importSaving, setImportSaving] = React.useState(false);
  const [importForm, setImportForm] = React.useState({ sourceFormat: "svg", sourceName: "", sourcePayload: "" });
  const [publishCandidateIds, setPublishCandidateIds] = React.useState<string[]>([]);
  // 行明细抽屉：候选/错误明细数据行整行可点 → 完整字段（枚举中文·序号/行号原样）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  // M018-010：项目清单只服务于新建弹窗下拉，是副资源——只授 floor_plan.view 的账号不发请求（后端 /platform/projects 要 governance.project.view + 平台 scope），
  // 读取失败只降级提示，平面图表照常渲染（同模块营业时间页的 loadSecondary 范式）。
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [list, projectList] = await Promise.all([
        api.get<{ items?: FloorPlan[] } | FloorPlan[]>("/api/v1/floor-plans"),
        loadSecondaryIfPermitted(
          hasPermissions(effectivePermissions, ["governance.project.view"]),
          () => api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
          [] as { items?: { id: string; name?: string }[] } | { id: string; name?: string }[],
        ),
      ]);
      setFloorPlans(Array.isArray(list) ? list : (list?.items ?? []));
      const projectData = projectList.data;
      setProjects(Array.isArray(projectData) ? projectData : (projectData?.items ?? []));
      setProjectsNotice(projectList.notice);
    },
    { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
  );

  // M018-004：下钻读取失败（403/网络）不再静默——统一走 setGlobalError。
  const loadImports = async (floorPlanId: string) => {
    setSelectedPlanId(floorPlanId);
    setSelectedImportId("");
    setFloorPlanImportDetails(null);
    importKeyRef.current = "";
    publishKeyRef.current = "";
    try {
      const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
      setImports(Array.isArray(res) ? res : (res?.items ?? []));
    } catch (e) { setGlobalError((e as Error).message); }
  };

  const loadCandidates = async (floorPlanId: string, importId: string) => {
    setSelectedImportId(importId);
    publishKeyRef.current = "";
    setPublishCandidateIds([]);
    try {
      const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
      setFloorPlanImportDetails(details);
    } catch (e) { setGlobalError((e as Error).message); }
  };

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体保留（双保险再校验一次名称）。
  // M018-001：请求体与后端 FloorPlanService.validateCreateRequest / V023 契约一致（楼层、底图地址、底图宽高、版本号必填；状态闭集 active/inactive/archived，无 draft）。
  const planFormError = () => {
    if (!planForm.projectId.trim()) return "请选择所属项目";
    if (!planForm.planName.trim()) return "请填写平面图名称";
    if (!planForm.floorNo.trim()) return "请填写楼层";
    if (!planForm.imageUrl.trim()) return "请填写底图地址";
    if (!(Number(planForm.imageWidth) > 0) || !(Number(planForm.imageHeight) > 0)) return "请填写大于 0 的底图宽高";
    if (!planForm.versionNo.trim()) return "请填写版本号";
    return null;
  };
  const createPlan = async () => {
    const formError = planFormError();
    if (formError) { setPlanError(formError); return; }
    setPlanSaving(true);
    setPlanError("");
    try {
      if (!planKeyRef.current) planKeyRef.current = `fp-${Math.random().toString(36).slice(2)}`;
      // M018-005：幂等键以 camel 键交给 AdminApi（normalizeRequestBody 只认 idempotencyKey，snake 键会被生成键覆盖，失败重试不再同键）。
      await api.post("/api/v1/floor-plans", {
        idempotencyKey: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
        building_code: planForm.buildingCode, floor_no: planForm.floorNo, image_url: planForm.imageUrl,
        image_width: Number(planForm.imageWidth), image_height: Number(planForm.imageHeight), version_no: planForm.versionNo, status: "active",
      });
      setActionResult({ action: "floor_plan_created", planName: planForm.planName });
      setPlanOpen(false);
      reload();
    } catch (e) { setPlanError((e as Error).message); } finally { setPlanSaving(false); }
  };

  // M018-007：来源名称与后端 validateImportCreateRequest（source_name 必填）对齐。
  const importFormError = () => {
    if (!importForm.sourceName.trim()) return "请填写来源名称";
    if (!importForm.sourcePayload.trim()) return "请填写来源内容";
    return null;
  };
  const createImport = async () => {
    const formError = importFormError();
    if (formError) { setImportError(formError); return; }
    setImportSaving(true);
    setImportError("");
    try {
      if (!importKeyRef.current) importKeyRef.current = `fpi-${selectedPlanId}-${Math.random().toString(36).slice(2)}`;
      const res = await api.post<ImportTask>(`/api/v1/floor-plans/${selectedPlanId}/imports`, {
        idempotencyKey: importKeyRef.current, source_format: importForm.sourceFormat,
        source_name: importForm.sourceName, source_payload: importForm.sourcePayload,
      });
      setActionResult({ action: "floor_plan_import_created", importId: res?.id });
      importKeyRef.current = "";
      setImportOpen(false);
      await loadImports(selectedPlanId);
    } catch (e) { setImportError((e as Error).message); } finally { setImportSaving(false); }
  };

  // M018-008：只发布勾选的候选（勾选＝人工复核），未勾选不默认全发。
  const requireSelectedCandidates = () => {
    if (publishCandidateIds.length === 0) { setGlobalError("请勾选要发布的区域"); return false; }
    return true;
  };
  const publishImport = async () => {
    try {
      if (!requireSelectedCandidates()) return;
      const ids = publishCandidateIds;
      if (!publishKeyRef.current) publishKeyRef.current = `fpp-${selectedImportId}-${Math.random().toString(36).slice(2)}`;
      const result = await api.post(`/api/v1/floor-plans/${selectedPlanId}/imports/${selectedImportId}/publish`, {
        idempotencyKey: publishKeyRef.current, candidate_ids: ids,
      });
      setActionResult({ action: "floor_plan_import_published", candidateIds: ids, result });
      await loadCandidates(selectedPlanId, selectedImportId);
    } catch (e) { setGlobalError((e as Error).message); }
  };

  function openCandidateDetail(candidate: ImportCandidate) {
    setDetail({
      title: `候选明细 · ${candidate.label ?? candidate.id}`,
      fields: [
        { label: "序号", value: candidate.candidateNo ?? "—" },
        { label: "类型", value: fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType) },
        { label: "关联", value: `${fpLabel(REF_TYPE_LABELS, candidate.refType)}：${candidate.refId ?? "—"}` },
        { label: "标签", value: candidate.label ?? "—" },
        { label: "状态", value: fpLabel(CANDIDATE_STATUS_LABELS, candidate.status) },
        { label: "候选编号", value: candidate.id },
      ],
    });
  }

  function openErrorDetail(row: ImportError) {
    setDetail({
      title: `导入错误明细 · 第 ${row.lineNo ?? "—"} 行`,
      fields: [
        { label: "行号", value: row.lineNo ?? "—" },
        { label: "字段", value: zhOrDash(ERROR_FIELD_LABELS, row.fieldName) },
        { label: "错误码", value: zhOrDash(ERROR_CODE_LABELS, row.errorCode) },
        { label: "说明", value: row.errorMessage ?? "—" },
      ],
    });
  }

  const pendingCandidates = floorPlanImportDetails?.candidates?.filter((item) => item.status === "pending") ?? [];

  return (
    <section data-testid="floor-plan-import-page">
      <AdminPageHeader title="商场平面图导入" description="上传商场平面图，解析出候选区域，人工复核候选与错误后发布到正式平面图。发布为高危动作，需二次确认。" />
      <AuditTrailHint event="floor_plan.created / floor_plan.import_published" />
      <TechDetails><div className="admin-generic-evidence" data-testid="floor-plan-evidence">/api/v1/floor-plans</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row" data-testid="floor-plan-create-form">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="floor-plan-create-open"
            onClick={() => { setPlanForm(EMPTY_PLAN_FORM); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
          >＋ 新增平面图</PermissionButton>
        </div>
        <table className="admin-table" data-testid="floor-plans-table">
          <thead><tr><th>名称</th><th>楼栋/层</th><th>版本</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {floorPlans.map((plan) => (
              <tr key={plan.id}>
                <td>{plan.planName ?? plan.id}</td><td>{plan.buildingCode ?? "-"}/{plan.floorNo ?? "-"}</td>
                <td>{plan.versionNo ?? "-"}</td><td>{floorPlanStatusLabel(plan.status)}</td>
                <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></Can></td>
              </tr>
            ))}
          </tbody>
        </table>
        {selectedPlanId ? (
          <>
            <h3>导入任务</h3>
            <div className="admin-form-row" data-testid="floor-plan-import-form">
              <PermissionButton permissionsState={effectivePermissions} permissions={IMPORT_MANAGE} data-testid="floor-plan-import-open" onClick={() => { setImportForm({ sourceFormat: "svg", sourceName: "", sourcePayload: "" }); importKeyRef.current = ""; setImportError(""); setImportOpen(true); }}>＋ 创建导入</PermissionButton>
            </div>
            <table className="admin-table" data-testid="floor-plan-imports-table">
              <thead><tr><th>来源</th><th>状态</th><th>候选数</th><th>错误数</th><th>操作</th></tr></thead>
              <tbody>
                {imports.map((task) => (
                  <tr key={task.id}>
                    <td>{task.sourceName ?? task.id}</td><td>{fpLabel(IMPORT_TASK_STATUS_LABELS, task.status)}</td>
                    <td>{task.candidateCount ?? 0}</td><td>{task.errorCount ?? 0}</td>
                    <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></Can></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null}
        {floorPlanImportDetails ? (
          <>
            <div className="admin-kpi-grid" data-testid="floor-plan-import-summary">
              <div className="admin-kpi-card"><span>待发布候选</span><strong data-testid="floor-plan-pending-count">{pendingCandidates.length}</strong><small>候选数</small></div>
              <div className="admin-kpi-card"><span>导入错误</span><strong>{floorPlanImportDetails.errors?.length ?? 0}</strong><small>错误数</small></div>
            </div>
            <h3>候选明细</h3>
            <table className="admin-table" data-testid="floor-plan-candidates-table">
              <thead><tr><th>序号</th><th>类型</th><th>关联</th><th>标签</th><th>状态</th><th>详情</th></tr></thead>
              <tbody>
                {(floorPlanImportDetails.candidates ?? []).map((candidate) => (
                  <tr key={candidate.id} className="admin-row-clickable" tabIndex={0} data-testid={`floor-plan-candidate-row-${candidate.id}`}
                    onClick={() => openCandidateDetail(candidate)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCandidateDetail(candidate); } }}>
                    <td>{candidate.candidateNo ?? "-"}</td><td>{fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType)}</td>
                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{fpLabel(CANDIDATE_STATUS_LABELS, candidate.status)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看候选详情" onClick={(event) => { event.stopPropagation(); openCandidateDetail(candidate); }}>查看详情</button></td></tr>
                ))}
              </tbody>
            </table>
            <h3>导入错误</h3>
            <table className="admin-table" data-testid="floor-plan-errors-table">
              <thead><tr><th>行号</th><th>字段</th><th>错误码</th><th>说明</th><th>详情</th></tr></thead>
              <tbody>
                {(floorPlanImportDetails.errors ?? []).map((row) => (
                  <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`floor-plan-error-row-${row.id}`}
                    onClick={() => openErrorDetail(row)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openErrorDetail(row); } }}>
                    <td>{row.lineNo ?? "-"}</td><td>{zhOrDash(ERROR_FIELD_LABELS, row.fieldName)}</td><td>{zhOrDash(ERROR_CODE_LABELS, row.errorCode)}</td><td>{row.errorMessage ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看导入错误详情" onClick={(event) => { event.stopPropagation(); openErrorDetail(row); }}>查看详情</button></td></tr>
                ))}
              </tbody>
            </table>
            <div className="admin-form-grid" data-testid="floor-plan-publish-form">
              <fieldset data-testid="floor-plan-publish-candidates">
                <legend>待发布区域</legend>
                {pendingCandidates.map((item) => (
                  <label key={item.id}>
                    <input
                      type="checkbox"
                      checked={publishCandidateIds.includes(item.id)}
                      onChange={(event) => {
                        setPublishCandidateIds((current) => event.target.checked ? [...current, item.id] : current.filter((id) => id !== item.id));
                      }}
                    />
                    {item.label ?? item.id}
                  </label>
                ))}
              </fieldset>
              <HighRiskConfirm
                label="发布候选到正式平面图"
                impact={`${PUBLISH_CONFIRM_TITLE}：本次发布 ${publishCandidateIds.length} 个已勾选候选，发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
                precheck={requireSelectedCandidates}
                onConfirm={() => void publishImport()}
                permissionsState={effectivePermissions}
                permissions={IMPORT_MANAGE}
                data-testid="floor-plan-publish"
              />
            </div>
          </>
        ) : null}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={planOpen}
        title="新增平面图"
        testId="floor-plan-create-guided"
        submitTestId="floor-plan-create"
        submitLabel="创建平面图"
        submitting={planSaving}
        error={planError || undefined}
        onClose={() => setPlanOpen(false)}
        onSubmit={() => void createPlan()}
        steps={[
          {
            title: "基本信息",
            description: "平面图名称、楼层、底图地址、底图宽高、版本号必填；楼栋选填。新建平面图为启用状态，导入候选复核后再发布。",
            validate: planFormError,
            content: (
              <>
                <label>所属项目<select data-testid="floor-plan-project" value={planForm.projectId} onChange={(e) => setPlanForm({ ...planForm, projectId: e.target.value })}>
                  <option value="">请选择</option>
                  {projects.map((project) => <option key={project.id} value={project.id}>{project.name ?? project.id}</option>)}
                </select></label>
                {projectsNotice ? <span className="admin-muted-action" data-testid="floor-plan-projects-degraded">{projectsNotice}</span> : null}
                <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
                <label>底图地址<input data-testid="floor-plan-image-url" value={planForm.imageUrl} onChange={(e) => setPlanForm({ ...planForm, imageUrl: e.target.value })} /></label>
                <label>底图宽<input data-testid="floor-plan-image-width" type="number" min="1" value={planForm.imageWidth} onChange={(e) => setPlanForm({ ...planForm, imageWidth: e.target.value })} /></label>
                <label>底图高<input data-testid="floor-plan-image-height" type="number" min="1" value={planForm.imageHeight} onChange={(e) => setPlanForm({ ...planForm, imageHeight: e.target.value })} /></label>
                <label>版本号<input data-testid="floor-plan-version" value={planForm.versionNo} onChange={(e) => setPlanForm({ ...planForm, versionNo: e.target.value })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "所属项目", value: projects.find((p) => p.id === planForm.projectId)?.name ?? (planForm.projectId || "未选择") },
          { label: "平面图名称", value: planForm.planName.trim() || "未填写" },
          { label: "楼栋", value: planForm.buildingCode.trim() || "—" },
          { label: "楼层", value: planForm.floorNo.trim() || "未填写" },
          { label: "底图地址", value: planForm.imageUrl.trim() || "未填写" },
          { label: "底图宽×高", value: planForm.imageWidth && planForm.imageHeight ? `${planForm.imageWidth} × ${planForm.imageHeight}` : "未填写" },
          { label: "版本号", value: planForm.versionNo.trim() || "未填写" },
          { label: "状态", value: "启用" },
        ]}
      />
      <GuidedFlowModal
        open={importOpen}
        title="创建导入"
        testId="floor-plan-import-guided"
        submitTestId="floor-plan-import-create"
        submitLabel="创建导入"
        submitting={importSaving}
        error={importError || undefined}
        onClose={() => setImportOpen(false)}
        onSubmit={() => void createImport()}
        steps={[
          {
            title: "导入内容",
            description: "选择来源格式，填写名称和内容。",
            validate: importFormError,
            content: (
              <>
                <label>来源格式<select data-testid="floor-plan-import-format" value={importForm.sourceFormat} onChange={(e) => setImportForm({ ...importForm, sourceFormat: e.target.value })}>
                  {SOURCE_FORMAT_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                </select></label>
                <label>来源名称<input data-testid="floor-plan-import-name" value={importForm.sourceName} onChange={(e) => setImportForm({ ...importForm, sourceName: e.target.value })} /></label>
                <label>来源内容<textarea data-testid="floor-plan-import-payload" value={importForm.sourcePayload} onChange={(e) => setImportForm({ ...importForm, sourcePayload: e.target.value })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "来源格式", value: SOURCE_FORMAT_OPTIONS.find((opt) => opt.value === importForm.sourceFormat)?.label ?? importForm.sourceFormat },
          { label: "来源名称", value: importForm.sourceName.trim() || "—" },
        ]}
      />
    </section>
  );
}
