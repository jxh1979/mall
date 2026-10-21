// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// HP-F02（BSM「物业服务」运营细则 L1385；ENUM L262「复杂类（大型设备报修等）须主管验收＋提单方确认双闸」）：
// 已验收（主管已验收、待提单确认）的大型维修工单可做「提单确认」；自动工单可「指定确认人」；
// 第十二批操作员裁决第 2 条：验收或提单确认不通过，填原因「退回返工」；第 12 条：关联设备的报修才两道。
const ACCEPTED = {
  id: "wo-1", code: "FWO-1", workOrderType: "repair", priority: "high", status: "accepted", title: "设备告警: 2号冷机",
  source: "equipment_alarm", equipmentId: "feq-1", assigneeId: "usr_eng", confirmerId: "usr_eng", verifierId: "usr_lead",
  verifiedAt: "2026-10-21T08:00:00Z", reporterConfirmedAt: null,
};
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>, rows: object[] = [ACCEPTED]): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url === "/api/v1/facility/work-orders" ? rows : [])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn(), userId = "u1") {
  const props = {
    api,
    user: { id: userId, username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

async function openActionAndClick(testId: string) {
  await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-1"));
  await waitFor(() => expect(screen.queryByTestId(testId)).not.toBeNull());
  fireEvent.click(screen.getByTestId(testId));
}

describe("FacilityWorkbenchPage 主管验收＋提单方确认双闸（HP-F02）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("已验收的大型维修工单给承接人本人显示「提单确认」，提交到 review 并带满意度", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post, [{ ...ACCEPTED, confirmerId: "usr_own" }]), vi.fn(), "usr_own");

    await openActionAndClick("facility-work-order-review-wo-1");

    expect(screen.getAllByText("提单确认").length).toBeGreaterThan(0);
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/review",
      expect.objectContaining({ satisfaction: 5 })));
  });

  it("退回返工须先在备注填写不通过原因，填了才提交 rework", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    // 待验收（主管验收不通过）：裁决第 16 条只管已验收后，待验收时主管照旧可退回。
    renderPage(makeApi(post, [{ ...ACCEPTED, status: "pending_acceptance", verifierId: null, verifiedAt: null }]), setGlobalError);

    await openActionAndClick("facility-work-order-rework-wo-1");
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("原因")));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-work-order-note"), { target: { value: "复查仍漏水" } });
    await openActionAndClick("facility-work-order-rework-wo-1");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/rework",
      expect.objectContaining({ note: "复查仍漏水" })));
  });

  it("自动工单可「指定确认人」，提交 confirmerId 到 confirmer", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-confirmer")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-work-order-confirmer"), { target: { value: "usr_b" } });
    await openActionAndClick("facility-work-order-designate-confirmer-wo-1");

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/confirmer",
      expect.objectContaining({ confirmerId: "usr_b" })));
  });

  it("裁决第 12 条：没关联设备的普通报修只按配置验收（无提单确认、退回返工），关联设备的报修才两道", async () => {
    const plain = { id: "wo-2", code: "FWO-2", workOrderType: "repair", priority: "normal", status: "pending_acceptance",
      title: "门锁报修", source: "platform", assigneeId: "usr_eng", equipmentId: null, reporterConfirmedAt: null };
    const linked = { ...plain, id: "wo-3", code: "FWO-3", title: "2号冷机报修", equipmentId: "feq-1" };
    renderPage(makeApi(vi.fn(), [plain, linked]));

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-2")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-2"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-accept-wo-2")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-review-wo-2")).toBeNull();
    expect(screen.queryByTestId("facility-work-order-rework-wo-2")).toBeNull();

    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-3"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-review-wo-3")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-rework-wo-3")).not.toBeNull();
  });

  it("验收方下拉不再有「主管验收＋提单方确认」，报修策略默认主管验收，工单明细列两项确认", async () => {
    renderPage(makeApi(vi.fn()));

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-policy-repair-high")?.textContent).toContain("主管"));
    expect(screen.getByTestId("facility-work-order-policy-repair-high").textContent).not.toContain("提单方确认");
    const options = Array.from((screen.getByTestId("facility-policy-acceptance-by") as HTMLSelectElement).options).map((o) => o.value);
    expect(options).toEqual(["handler", "supervisor", "reporter"]);
    expect((screen.getByTestId("facility-policy-acceptance-by") as HTMLSelectElement).value).toBe("supervisor");

    fireEvent.click(screen.getByTestId("facility-work-order-wo-1"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")?.textContent).toContain("提单确认承接人"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("主管验收");
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("已验收");
  });

  // 第十二批操作员裁决第 15 条「提单员工本人（推荐）」：员工手工建的关联设备报修单，提单员工就是承接人——后台持 manage 的旁人
  // 不显示「提单确认」，承接人本人才显示；提单人就是本次维修人时由物业主管「指定确认人」；已验收后的退回返工按第 16 条也只给承接人本人。
  it("裁决第 15 条：员工手工建的关联设备报修单只给承接人本人显示「提单确认」，主管可「指定确认人」", async () => {
    const staffRow = { id: "wo-4", code: "FWO-4", workOrderType: "repair", priority: "high", status: "accepted", title: "2号冷机异响",
      source: "platform", reporterType: "staff", reporterId: "usr_own", confirmerId: "usr_own", equipmentId: "feq-1",
      assigneeId: "usr_eng", verifiedAt: "2026-10-21T08:00:00Z", reporterConfirmedAt: null };
    renderPage(makeApi(vi.fn(), [staffRow]));

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-4")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-4"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-designate-confirmer-wo-4")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-review-wo-4")).toBeNull();
    expect(screen.queryByTestId("facility-work-order-rework-wo-4")).toBeNull();
  });

  it("裁决第 15 条：承接人本人在后台也能「提单确认」", async () => {
    const staffRow = { id: "wo-1", code: "FWO-1", workOrderType: "repair", priority: "high", status: "accepted", title: "2号冷机异响",
      source: "platform", reporterType: "staff", reporterId: "usr_own", confirmerId: "usr_own", equipmentId: "feq-1",
      assigneeId: "usr_eng", verifiedAt: "2026-10-21T08:00:00Z", reporterConfirmedAt: null };
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post, [staffRow]), vi.fn(), "usr_own");

    await openActionAndClick("facility-work-order-review-wo-1");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/review",
      expect.objectContaining({ satisfaction: 5 })));
  });

  // BSM L1385「由该设备或点位的在岗责任人承接提单确认」；裁决第 15 条「和告警单一样」：告警单同样只给承接人本人显示「提单确认」，
  // 持 manage 的旁人点了必被后端 403 CONFIRMER_MISMATCH，故不显示（指定确认人照旧；已验收后的退回返工按第 16 条同样不显示）。
  it("告警单：持 manage 的旁人不显示「提单确认」", async () => {
    renderPage(makeApi(vi.fn(), [{ ...ACCEPTED, confirmerId: "usr_own" }]), vi.fn(), "u1");

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-1"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-designate-confirmer-wo-1")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-review-wo-1")).toBeNull();
    expect(screen.queryByTestId("facility-work-order-rework-wo-1")).toBeNull();
  });

  // 第十二批操作员裁决第 16 条「只许承接人本人（推荐）」：「主管验收后只剩提单确认这一道，退回也只让承接人本人做，与第 15 条一致；
  // 后台对旁人不显示该按钮」。已验收的告警单与员工手工建的关联设备报修单只给承接人本人（且不是本次维修人）显示「退回返工」；待验收照旧。
  it("裁决第 16 条：已验收后「退回返工」只给承接人本人显示，旁人与身兼维修人的承接人不显示，待验收时主管照旧", async () => {
    const staffRow = { ...ACCEPTED, id: "wo-6", code: "FWO-6", source: "platform", reporterType: "staff", reporterId: "usr_own",
      confirmerId: "usr_own" };
    const alarmRow = { ...ACCEPTED, id: "wo-7", code: "FWO-7", confirmerId: "usr_own" };
    const pendingRow = { ...ACCEPTED, id: "wo-8", code: "FWO-8", status: "pending_acceptance", confirmerId: "usr_own", verifierId: null,
      verifiedAt: null };
    renderPage(makeApi(vi.fn(), [staffRow, alarmRow, pendingRow]), vi.fn(), "u1");

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-6")).not.toBeNull());
    for (const id of ["wo-6", "wo-7"]) {
      fireEvent.click(screen.getByTestId(`facility-work-order-actions-${id}`));
      await waitFor(() => expect(screen.queryByTestId(`facility-work-order-designate-confirmer-${id}`)).not.toBeNull());
      expect(screen.queryByTestId(`facility-work-order-rework-${id}`)).toBeNull();
    }
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-8"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-rework-wo-8")).not.toBeNull());
  });

  it("裁决第 16 条：承接人本人在已验收后可「退回返工」，提交到 rework", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post, [{ ...ACCEPTED, confirmerId: "usr_own" }]), vi.fn(), "usr_own");

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-note")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-work-order-note"), { target: { value: "提单确认不通过：仍有异响" } });
    await openActionAndClick("facility-work-order-rework-wo-1");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/rework",
      expect.objectContaining({ note: "提单确认不通过：仍有异响" })));
  });

  // BSM L1385「本人负责此次维修时，由物业主管指定其他人员并留痕」；裁决第 15 条「提单人就是本次维修人时，由物业主管指定他人并留痕」：
  // 承接人就是本次维修人时，后端对其提单确认一律 409 CONFIRMER_DESIGNATION_REQUIRED，故本人也不显示「提单确认」，由主管「指定确认人」。
  it("承接人就是本次维修人时本人也不显示「提单确认」，显示「指定确认人」", async () => {
    const staffRow = { id: "wo-5", code: "FWO-5", workOrderType: "repair", priority: "high", status: "accepted", title: "2号冷机异响",
      source: "platform", reporterType: "staff", reporterId: "usr_eng", confirmerId: "usr_eng", equipmentId: "feq-1",
      assigneeId: "usr_eng", verifiedAt: "2026-10-21T08:00:00Z", reporterConfirmedAt: null };
    renderPage(makeApi(vi.fn(), [staffRow]), vi.fn(), "usr_eng");

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-5")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-5"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-designate-confirmer-wo-5")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-review-wo-5")).toBeNull();
    // 裁决第 16 条：已验收后的退回返工同样只给不是本次维修人的承接人本人（后端对身兼维修人的承接人 409）。
    expect(screen.queryByTestId("facility-work-order-rework-wo-5")).toBeNull();
  });
});
