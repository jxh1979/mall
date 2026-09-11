import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { IconButton } from "../components/IconButton";
import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { formatBusinessStatus } from "../i18n/statusLabels";
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
const ERROR_FIELD_LABELS: Record<string, string> = {
  geometry: "图形",
  shape_type: "图形类型",
  source_format: "导入格式",
  project_id: "所属项目",
};
const ERROR_CODE_LABELS: Record<string, string> = {
  INVALID_POLYGON: "多边形无效",
  VALIDATION_FAILED: "校验未通过",
};
const zhOrDash = (map: Record<string, string>, code: string | null | undefined) =>
  code ? map[code] ?? "其他" : "-";
const REF_TYPE_LABELS: Record<string, string> = { space: "空间", merchant: "商户", facility: "设施" };
const fpLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? code : "-");

// 后端 floorplan 域记录映射(接现有 /api/v1/floor-plans/* 契约)。
type FloorPlan = { id: string; projectId?: string; buildingCode?: string; floorNo?: string; planName?: string; versionNo?: string; status?: string };
type ImportTask = { id: string; floorPlanId?: string; sourceFormat?: string; sourceName?: string; status?: string; candidateCount?: number; errorCount?: number };
type ImportCandidate = { id: string; candidateNo?: number; shapeType?: string; refType?: string; refId?: string; label?: string; status?: string };
type ImportError = { id: string; lineNo?: number; fieldName?: string; errorCode?: string; errorMessage?: string };
type FloorPlanImportDetails = { task?: ImportTask; candidates?: ImportCandidate[]; errors?: ImportError[] };

const MANAGE = ["floor_plan.manage"];
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
  const [planForm, setPlanForm] = React.useState({ projectId: "", planName: "", buildingCode: "", floorNo: "" });
  const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
  const planKeyRef = React.useRef("");
  const importKeyRef = React.useRef("");
  const publishKeyRef = React.useRef("");
  // 引导式「新增平面图」弹窗状态（范式：字段留页面持有，弹窗只编排）。
  const [planOpen, setPlanOpen] = React.useState(false);
  const [planError, setPlanError] = React.useState("");
  const [planSaving, setPlanSaving] = React.useState(false);
  const [importForm, setImportForm] = React.useState({ sourceFormat: "svg", sourceName: "", sourcePayload: "" });
  const [publishCandidateIds, setPublishCandidateIds] = React.useState("");
  // 行明细抽屉：候选/错误明细数据行整行可点 → 完整字段（枚举中文·序号/行号原样）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [list, projectList] = await Promise.all([
        api.get<{ items?: FloorPlan[] } | FloorPlan[]>("/api/v1/floor-plans"),
        api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
      ]);
      setFloorPlans(Array.isArray(list) ? list : (list?.items ?? []));
      setProjects(Array.isArray(projectList) ? projectList : (projectList?.items ?? []));
    },
    { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
  );

  const loadImports = async (floorPlanId: string) => {
    setSelectedPlanId(floorPlanId);
    setSelectedImportId("");
    setFloorPlanImportDetails(null);
    const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
    setImports(Array.isArray(res) ? res : (res?.items ?? []));
  };

  const loadCandidates = async (floorPlanId: string, importId: string) => {
    setSelectedImportId(importId);
    const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
    setFloorPlanImportDetails(details);
  };

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体保留（双保险再校验一次名称）。
  const createPlan = async () => {
    if (!planForm.projectId.trim()) { setPlanError("请选择所属项目"); return; }
    if (!planForm.planName.trim()) { setPlanError("请填写平面图名称"); return; }
    setPlanSaving(true);
    setPlanError("");
    try {
      if (!planKeyRef.current) planKeyRef.current = `fp-${planForm.projectId}-${planForm.planName.trim()}`;
      await api.post("/api/v1/floor-plans", {
        idempotency_key: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
        building_code: planForm.buildingCode, floor_no: planForm.floorNo, status: "draft",
      });
      setActionResult({ action: "floor_plan_created", planName: planForm.planName });
      setPlanOpen(false);
      reload();
    } catch (e) { setPlanError((e as Error).message); } finally { setPlanSaving(false); }
  };

  const createImport = async () => {
    try {
      const res = await api.post<ImportTask>(`/api/v1/floor-plans/${selectedPlanId}/imports`, {
        idempotency_key: importKeyRef.current || (importKeyRef.current = `fpi-${selectedPlanId}-${importForm.sourceName || "import"}`), source_format: importForm.sourceFormat,
        source_name: importForm.sourceName, source_payload: importForm.sourcePayload,
      });
      setActionResult({ action: "floor_plan_import_created", importId: res?.id });
      await loadImports(selectedPlanId);
    } catch (e) { setGlobalError((e as Error).message); }
  };

  const publishImport = async () => {
    try {
      const typed = publishCandidateIds.split(",").map((s) => s.trim()).filter(Boolean);
      const ids = typed.length > 0 ? typed : pendingCandidates.map((item) => item.id).filter(Boolean);
      if (ids.length === 0) { setGlobalError("没有待发布的区域"); return; }
      if (!publishKeyRef.current) publishKeyRef.current = `fpp-${selectedImportId}`;
      const result = await api.post(`/api/v1/floor-plans/${selectedPlanId}/imports/${selectedImportId}/publish`, {
        idempotency_key: publishKeyRef.current, candidate_ids: ids,
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
        { label: "状态", value: formatBusinessStatus(candidate.status) },
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
      <AdminPageHeader title="商场平面图导入" description="P2 平面图导入：上传商场平面图，解析出候选区域，人工复核候选与错误后发布到正式平面图。发布为高危动作，需二次确认。" />
      <AuditTrailHint event="floor_plan.created / floor_plan.import_published" />
      <TechDetails><div className="admin-generic-evidence" data-testid="floor-plan-evidence">/api/v1/floor-plans</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row" data-testid="floor-plan-create-form">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="floor-plan-create-open"
            onClick={() => { setPlanForm({ projectId: "", planName: "", buildingCode: "", floorNo: "" }); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
          >＋ 新增平面图</PermissionButton>
        </div>
        <table className="admin-table" data-testid="floor-plans-table">
          <thead><tr><th>名称</th><th>楼栋/层</th><th>版本</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {floorPlans.map((plan) => (
              <tr key={plan.id}>
                <td>{plan.planName ?? plan.id}</td><td>{plan.buildingCode ?? "-"}/{plan.floorNo ?? "-"}</td>
                <td>{plan.versionNo ?? "-"}</td><td>{formatBusinessStatus(plan.status)}</td>
                <td><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></td>
              </tr>
            ))}
          </tbody>
        </table>
        {selectedPlanId ? (
          <>
            <h3>导入任务</h3>
            <div className="admin-form-grid" data-testid="floor-plan-import-form">
              <label>来源格式<select data-testid="floor-plan-import-format" value={importForm.sourceFormat} onChange={(e) => setImportForm({ ...importForm, sourceFormat: e.target.value })}>
                {SOURCE_FORMAT_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
              </select></label>
              <label>来源名称<input data-testid="floor-plan-import-name" value={importForm.sourceName} onChange={(e) => setImportForm({ ...importForm, sourceName: e.target.value })} /></label>
              <label>来源内容<textarea data-testid="floor-plan-import-payload" value={importForm.sourcePayload} onChange={(e) => setImportForm({ ...importForm, sourcePayload: e.target.value })} /></label>
              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="floor-plan-import-create" onClick={() => void createImport()}>创建导入</PermissionButton>
            </div>
            <table className="admin-table" data-testid="floor-plan-imports-table">
              <thead><tr><th>来源</th><th>状态</th><th>候选数</th><th>错误数</th><th>操作</th></tr></thead>
              <tbody>
                {imports.map((task) => (
                  <tr key={task.id}>
                    <td>{task.sourceName ?? task.id}</td><td>{formatBusinessStatus(task.status)}</td>
                    <td>{task.candidateCount ?? 0}</td><td>{task.errorCount ?? 0}</td>
                    <td><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></td>
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
                {(floorPlanImportDetails.candidates ?? []).slice(0, 50).map((candidate) => (
                  <tr key={candidate.id} className="admin-row-clickable" tabIndex={0} data-testid={`floor-plan-candidate-row-${candidate.id}`}
                    onClick={() => openCandidateDetail(candidate)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCandidateDetail(candidate); } }}>
                    <td>{candidate.candidateNo ?? "-"}</td><td>{fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType)}</td>
                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{formatBusinessStatus(candidate.status)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看候选详情" onClick={(event) => { event.stopPropagation(); openCandidateDetail(candidate); }}>查看详情</button></td></tr>
                ))}
              </tbody>
            </table>
            <h3>导入错误</h3>
            <table className="admin-table" data-testid="floor-plan-errors-table">
              <thead><tr><th>行号</th><th>字段</th><th>错误码</th><th>说明</th><th>详情</th></tr></thead>
              <tbody>
                {(floorPlanImportDetails.errors ?? []).slice(0, 50).map((row) => (
                  <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`floor-plan-error-row-${row.id}`}
                    onClick={() => openErrorDetail(row)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openErrorDetail(row); } }}>
                    <td>{row.lineNo ?? "-"}</td><td>{zhOrDash(ERROR_FIELD_LABELS, row.fieldName)}</td><td>{zhOrDash(ERROR_CODE_LABELS, row.errorCode)}</td><td>{row.errorMessage ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看导入错误详情" onClick={(event) => { event.stopPropagation(); openErrorDetail(row); }}>查看详情</button></td></tr>
                ))}
              </tbody>
            </table>
            <div className="admin-form-grid" data-testid="floor-plan-publish-form">
              <label>待发布区域（不填则发布当前待复核的全部）<input data-testid="floor-plan-publish-candidates" value={publishCandidateIds} onChange={(e) => setPublishCandidateIds(e.target.value)} /></label>
              <HighRiskConfirm
                label="发布候选到正式平面图"
                impact={`${PUBLISH_CONFIRM_TITLE}：发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
                onConfirm={() => void publishImport()}
                permissionsState={effectivePermissions}
                permissions={MANAGE}
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
            description: "平面图名称必填；楼栋/楼层选填。新建平面图初始为草稿，导入候选复核后再发布。",
            validate: () => {
              if (!planForm.projectId.trim()) return "请选择所属项目";
              if (!planForm.planName.trim()) return "请填写平面图名称";
              return null;
            },
            content: (
              <>
                <label>所属项目<select data-testid="floor-plan-project" value={planForm.projectId} onChange={(e) => setPlanForm({ ...planForm, projectId: e.target.value })}>
                  <option value="">请选择</option>
                  {projects.map((project) => <option key={project.id} value={project.id}>{project.name ?? project.id}</option>)}
                </select></label>
                <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "所属项目", value: projects.find((p) => p.id === planForm.projectId)?.name ?? (planForm.projectId || "未选择") },
          { label: "平面图名称", value: planForm.planName.trim() || "未填写" },
          { label: "楼栋", value: planForm.buildingCode.trim() || "—" },
          { label: "楼层", value: planForm.floorNo.trim() || "—" },
          { label: "初始状态", value: "草稿" },
        ]}
      />
    </section>
  );
}
