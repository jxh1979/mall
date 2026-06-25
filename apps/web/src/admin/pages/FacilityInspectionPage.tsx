import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type TaskRow = { id: string; equipmentId: string; spaceId?: string; dueAt?: string; status: string };
type TemplateRow = { id: string; name: string; equipmentType?: string; items: string[]; status: string };

const MANAGE = ["facility.inspection.manage"];
const TYPES = ["", "hvac", "elevator", "fire", "power", "water", "weak_current", "other"];
const RESULTS = ["pass", "fail"];

/**
 * 楼宇设施巡检（ADM-FAC-003，③ 楼宇运营 S3）：巡检模板 + 按设备维保周期生成的到期任务（超期红点）+ 完成写记录。
 * 功能标志 facility.inspection.enabled 默认关（关时写被拒）。「生成到期任务」亦由后台 @Scheduled 作业周期触发。
 */
export function FacilityInspectionPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [tasks, setTasks] = React.useState<TaskRow[]>([]);
  const [templates, setTemplates] = React.useState<TemplateRow[]>([]);
  const [tplForm, setTplForm] = React.useState({ name: "", equipmentType: "", items: "" });
  const [draft, setDraft] = React.useState<Record<string, { inspector: string; result: string }>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      setTasks((await api.get<TaskRow[]>("/api/v1/facility/inspection-tasks")) ?? []);
      setTemplates((await api.get<TemplateRow[]>("/api/v1/facility/inspection-templates")) ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "设施巡检读取失败", onStart: () => setGlobalError(null) },
  );

  async function generate() {
    await api.post("/api/v1/facility/inspection-tasks/generate", {});
    setActionResult({ action: "facility_inspection_generated" });
    reload();
  }

  async function complete(task: TaskRow) {
    const d = draft[task.id] ?? { inspector: "", result: "pass" };
    await api.post(`/api/v1/facility/inspection-tasks/${task.id}/complete`, {
      inspector: d.inspector.trim() || null,
      result: d.result || "pass",
    });
    setActionResult({ action: "facility_inspection_completed", taskId: task.id });
    reload();
  }

  async function createTemplate() {
    if (!tplForm.name.trim()) {
      setGlobalError("请填写模板名称");
      return;
    }
    await api.post("/api/v1/facility/inspection-templates", {
      name: tplForm.name.trim(),
      equipmentType: tplForm.equipmentType || null,
      items: tplForm.items.split(",").map((s) => s.trim()).filter(Boolean),
    });
    setActionResult({ action: "facility_inspection_template_created", name: tplForm.name.trim() });
    setTplForm({ name: "", equipmentType: "", items: "" });
    reload();
  }

  return (
    <section data-testid="facility-inspection-page">
      <AdminPageHeader
        title="楼宇设施巡检"
        description="按设备维保周期自动生成到期巡检任务（超期标红），完成后写巡检记录并顺延下一周期；巡检模板定义巡检项清单。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-inline-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="facility-inspection-generate" onClick={() => void generate()}>生成到期任务</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-inspection-task-table">
            <thead><tr><th>任务 id</th><th>设备</th><th>到期</th><th>状态</th><th>巡检人</th><th>结论</th><th>操作</th></tr></thead>
            <tbody>
              {tasks.map((task) => (
                <tr key={task.id} data-testid={`facility-inspection-task-${task.id}`}>
                  <td>{task.id}</td><td>{task.equipmentId}</td>
                  <td>{task.dueAt ? new Date(task.dueAt).toLocaleString() : "-"}</td>
                  <td>{task.status === "overdue"
                    ? <span data-testid={`facility-inspection-overdue-${task.id}`} style={{ color: "#c00" }}>超期</span>
                    : task.status}</td>
                  <td><input aria-label={`巡检人-${task.id}`} placeholder="巡检人"
                    value={draft[task.id]?.inspector ?? ""}
                    onChange={(e) => setDraft({ ...draft, [task.id]: { inspector: e.target.value, result: draft[task.id]?.result ?? "pass" } })} /></td>
                  <td><select aria-label={`结论-${task.id}`} value={draft[task.id]?.result ?? "pass"}
                    onChange={(e) => setDraft({ ...draft, [task.id]: { inspector: draft[task.id]?.inspector ?? "", result: e.target.value } })}>
                    {RESULTS.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select></td>
                  <td>{task.status === "completed"
                    ? "已完成"
                    : <button type="button" data-testid={`facility-inspection-complete-${task.id}`} onClick={() => void complete(task)}>完成</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <AdminPageHeader title="巡检模板" description="定义巡检项清单（逗号分隔）。" />
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-inspection-template-table">
            <thead><tr><th>id</th><th>名称</th><th>适用类型</th><th>巡检项</th><th>状态</th></tr></thead>
            <tbody>
              {templates.map((tpl) => (
                <tr key={tpl.id} data-testid={`facility-inspection-template-${tpl.id}`}>
                  <td>{tpl.id}</td><td>{tpl.name}</td><td>{tpl.equipmentType ?? "通用"}</td>
                  <td>{(tpl.items ?? []).join("、")}</td><td>{tpl.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-inspection-template-form">
          <input aria-label="模板名称" placeholder="模板名称" value={tplForm.name} onChange={(e) => setTplForm({ ...tplForm, name: e.target.value })} />
          <select aria-label="适用类型" value={tplForm.equipmentType} onChange={(e) => setTplForm({ ...tplForm, equipmentType: e.target.value })}>
            {TYPES.map((t) => <option key={t} value={t}>{t || "通用"}</option>)}
          </select>
          <input aria-label="巡检项" placeholder="巡检项（逗号分隔）" value={tplForm.items} onChange={(e) => setTplForm({ ...tplForm, items: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="facility-inspection-template-create" onClick={() => void createTemplate()}>新建模板</PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
