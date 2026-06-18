import type { FormInstance } from "antd";
import { getJson, postJson } from "./apiClient";
import type { NumericFormValues } from "./stockMovementRequest";
import type { FloorPlan, FloorPlanImportDetails, FloorPlanImportTask, Snapshot } from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type FloorPlanActionDependencies = {
  floorPlanForm: FormInstance;
  floorPlanImportForm: FormInstance;
  floorPlanPublishForm: FormInstance;
  runAction: RunAction;
  selectedFloorPlanId: string;
  selectedFloorPlanImportId: string;
  setSelectedFloorPlanId: (value: string) => void;
  setSelectedFloorPlanImportId: (value: string) => void;
  snapshot: Snapshot | null;
};

export type FloorPlanActions = ReturnType<typeof createFloorPlanActions>;

export function createFloorPlanActions({
  floorPlanForm,
  floorPlanImportForm,
  floorPlanPublishForm,
  runAction,
  selectedFloorPlanId,
  selectedFloorPlanImportId,
  setSelectedFloorPlanId,
  setSelectedFloorPlanImportId,
  snapshot,
}: FloorPlanActionDependencies) {
  async function handleFloorPlanCreate(values: NumericFormValues) {
    await runAction("P2 创建平面图", () => postJson<FloorPlan>("/api/v1/floor-plans", {
      projectId: values.projectId || undefined,
      buildingCode: values.buildingCode || undefined,
      floorNo: values.floorNo,
      planName: values.planName,
      imageUrl: values.imageUrl,
      imageWidth: Number(values.imageWidth || 1),
      imageHeight: Number(values.imageHeight || 1),
      versionNo: values.versionNo || "v1",
      status: values.status || "active",
      operatorNote: values.operatorNote || undefined,
    }), () => floorPlanForm.resetFields());
  }

  async function handleFloorPlanImport(values: Record<string, string>) {
    const floorPlanId = values.floorPlanId || selectedFloorPlanId || snapshot?.floorPlans[0]?.id;
    if (!floorPlanId) throw new Error("未选择平面图");
    await runAction("P2 创建导入任务", () => postJson<FloorPlanImportTask>(`/api/v1/floor-plans/${floorPlanId}/imports`, {
      sourceFormat: values.sourceFormat || "standard_json",
      sourceName: values.sourceName,
      sourcePayload: values.sourcePayload,
      operatorNote: values.operatorNote || undefined,
    }), () => floorPlanImportForm.resetFields());
  }

  async function handleLoadFloorPlanImportDetails(floorPlanId: string, importId: string) {
    setSelectedFloorPlanId(floorPlanId);
    setSelectedFloorPlanImportId(importId);
    await runAction("P2 加载导入候选明细", () => getJson<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`));
  }

  async function handlePublishFloorPlanImport(values: Record<string, string>) {
    const floorPlanId = values.floorPlanId || selectedFloorPlanId || snapshot?.floorPlans[0]?.id;
    const importId = values.importId || selectedFloorPlanImportId || snapshot?.floorPlanImports[0]?.id;
    if (!floorPlanId || !importId) throw new Error("未选择导入任务");
    const candidateIds = String(values.candidateIds || "").split(/[\s,，]+/).map((value) => value.trim()).filter(Boolean);
    if (candidateIds.length === 0) throw new Error("候选 ID");
    await runAction("P2 发布导入候选", () => postJson<FloorPlanImportTask>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/publish`, {
      candidateIds,
      operatorNote: values.operatorNote || "Web 确认后发布平面图候选",
    }), () => floorPlanPublishForm.resetFields());
  }

  return {
    handleFloorPlanCreate,
    handleFloorPlanImport,
    handleLoadFloorPlanImportDetails,
    handlePublishFloorPlanImport,
  };
}
