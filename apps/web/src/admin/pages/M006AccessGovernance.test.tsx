// 文件版本：V0.02
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";
afterEach(cleanup);
const policy = {"windowDays":{"value":30,"min":1,"max":2147483647},"positionThreshold":{"value":3,"min":1,"max":2147483647},"userThreshold":{"value":3,"min":1,"max":2147483647},"riskReviews":{"low":{"approvalLevel":"one_level_up","requiredLevel":1},"medium":{"approvalLevel":"one_level_up","requiredLevel":1},"high":{"approvalLevel":"two_levels_up","requiredLevel":2},"critical":{"approvalLevel":"top_authority","requiredLevel":3}}};
function props() {
  const api = new AdminApi("test-token");
  vi.spyOn(api, "get").mockImplementation(async (path) => {
    if (path.endsWith("/grant-policy")) return policy as never;
    if (path.includes("/summary")) return { positionRecommendations: [], userAnomalies: [] } as never;
    return [] as never;
  });
  vi.spyOn(api, "getPaged").mockResolvedValue({ items: [], total: 51, page: 1, size: 50 });
  vi.spyOn(api, "post").mockResolvedValue(policy);
  return { api, user: { id: "admin", username: "管理员", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
  } as AdminPageProps;
}
it("M006-030 可前往授权申请第二页，概要查询不携带政策覆盖参数", async () => {
  const p = props();
  render(<AccessGovernancePage {...p} />);
  await waitFor(() => expect(screen.getByTestId("access-grant-next-page")).toBeTruthy());
  fireEvent.click(screen.getByTestId("access-grant-next-page"));
  await waitFor(() => expect(p.api.getPaged).toHaveBeenCalledWith("/api/v1/access/grant-requests?page=2"));
  expect(p.api.get).toHaveBeenCalledWith("/api/v1/access/grant-requests/summary");
  expect(vi.mocked(p.api.get).mock.calls.every(([path]) => !path.includes("position_threshold"))).toBe(true);
});
it("M006-016 后台保存阈值及其上下限和风险审批映射", async () => {
  const p = props();
  render(<AccessGovernancePage {...p} />);
  await waitFor(() => expect(screen.getByTestId("access-grant-policy-load")).toBeTruthy());
  fireEvent.click(screen.getByTestId("access-grant-policy-load"));
  await waitFor(() => expect(screen.getByTestId("grant-policy-positionThreshold-value")).toBeTruthy());
  fireEvent.change(screen.getByTestId("grant-policy-positionThreshold-value"), { target: { value: "5" } });
  fireEvent.change(screen.getByTestId("grant-policy-reason"), { target: { value: "调整频繁判断" } });
  fireEvent.click(screen.getByTestId("access-grant-policy-save"));
  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/access/grant-policy",
    expect.objectContaining({ operatorNote: "调整频繁判断", policy: expect.objectContaining({ positionThreshold: expect.objectContaining({ value: 5 }) }) })));
});

it("M006-026 模板先存档再申请删除，界面不再调用物理 DELETE", async () => {
  const p = props();
  vi.mocked(p.api.get).mockImplementation(async (path) => {
    if (path.includes("position-role-templates")) return [{ id: "mapping", positionCode: "ops_manager", staffLevelId: "level", status: "active" }] as never;
    return [] as never;
  });
  const remove = vi.spyOn(p.api, "delete");
  render(<AccessGovernancePage {...p} />);
  await waitFor(() => expect(screen.getByTestId("access-position-role-template-archive-mapping")).toBeTruthy());
  fireEvent.change(screen.getByTestId("access-position-template-reason"), { target: { value: "岗位配置停止使用" } });
  fireEvent.click(screen.getByTestId("access-position-role-template-archive-mapping"));
  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/access/position-role-templates/mapping/lifecycle",
    expect.objectContaining({ action: "archive", reason: "岗位配置停止使用" })));
  expect(remove).not.toHaveBeenCalled();
});
