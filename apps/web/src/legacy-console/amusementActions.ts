import type { FormInstance } from "antd";
import { postJson } from "./apiClient";
import { adminRoutes, redirectToAdmin } from "./redirectToAdmin";
import type { NumericFormValues } from "./stockMovementRequest";
import type { AmusementGroupBooking, AmusementProject, AmusementSession, Snapshot } from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type AmusementActionDependencies = {
  amusementFlowForm: FormInstance;
  amusementGroupBookingForm: FormInstance;
  amusementVerifyForm: FormInstance;
  runAction: RunAction;
  snapshot: Snapshot | null;
};

export type AmusementActions = ReturnType<typeof createAmusementActions>;

export function createAmusementActions({
  amusementFlowForm,
  amusementGroupBookingForm,
  amusementVerifyForm,
  runAction,
  snapshot,
}: AmusementActionDependencies) {
  async function handleAmusementFlow(values: NumericFormValues) {
    await runAction("游乐项目和场次创建", async () => {
      const project = await postJson<AmusementProject>("/api/v1/amusement/projects", {
        projectName: values.projectName,
        capacity: Number(values.capacity || 20),
      });
      await postJson(`/api/v1/amusement/projects/${project.id}/rules`, { ruleType: "age", ruleValue: "P0 manual check" });
      const startsAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();
      const endsAt = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
      const session = await postJson<AmusementSession>(`/api/v1/amusement/sessions`, {
        sessionName: values.sessionName,
        startsAt,
        endsAt,
        capacity: Number(values.sessionCapacity || values.capacity || 20),
      });
      amusementVerifyForm.setFieldValue("sessionId", session.id);
      return { project, session };
    }, () => amusementFlowForm.resetFields());
  }

  // 终端铁律：Web 不可直接核销/人工放行（改变核销结果）。改为跳转管理后台执行。
  async function handleAmusementVerify(_values: Record<string, string>) {
    redirectToAdmin("游乐核销/人工放行", adminRoutes.amusementSessions);
  }

  async function handleAmusementGroupBooking(values: NumericFormValues) {
    const session = snapshot?.amusementSessions.find((item) => item.id === values.sessionId);
    if (!session) throw new Error("请选择可用场次");
    await runAction("P2 团体/包场申请", () => postJson<AmusementGroupBooking>("/api/v1/amusement/group-bookings", {
      projectId: session.projectId,
      sessionId: session.id,
      bookingType: values.bookingType || "group",
      customerName: values.customerName,
      contactPhone: values.contactPhone || undefined,
      partySize: Number(values.partySize || 1),
      reservedCapacity: Number(values.reservedCapacity || values.partySize || 1),
      operatorNote: values.operatorNote || undefined,
    }), () => amusementGroupBookingForm.resetFields());
  }

  async function handleApproveGroupBooking(bookingId: string) {
    await runAction("P2 团体/包场审批", () => postJson<AmusementGroupBooking>(`/api/v1/amusement/group-bookings/${bookingId}/approve`, {
      approvalNote: "Web 管理端审批并锁定容量",
    }));
  }

  async function handleReleaseGroupBooking(bookingId: string) {
    await runAction("P2 团体/包场释放", () => postJson<AmusementGroupBooking>(`/api/v1/amusement/group-bookings/${bookingId}/release`, {
      reason: "Web 管理端释放已锁定容量",
    }));
  }

  return {
    handleAmusementFlow,
    handleAmusementGroupBooking,
    handleAmusementVerify,
    handleApproveGroupBooking,
    handleReleaseGroupBooking,
  };
}
