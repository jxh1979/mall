import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PointsRulesPage } from "./PointsRulesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const RULES = [
  { id: "points_rule_1", projectId: "prj_A", ruleName: "基础消费积分", earnMethod: "consume_amount", earnRatio: 1, levelThreshold: "{}", validityType: "fixed_days", validityDays: 365, expireMode: "fifo_batch", clearDay: null, status: "active" },
];
function makeApi(get = vi.fn().mockResolvedValue(RULES), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient, setGlobalError = vi.fn(), setActionResult = vi.fn()) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS };
  render(<PointsRulesPage {...props} />);
}

describe("PointsRulesPage 积分规则配置(ADM-CUS-003)", () => {
  it("读回规则列表并中文化展示获取方式/有效期", async () => {
    const get = vi.fn().mockResolvedValue(RULES);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/customer/points-rules"));
    const row = await screen.findByTestId("points-rule-points_rule_1");
    expect(row.textContent).toContain("按消费金额");
    expect(row.textContent).toContain("固定天数后过期");
  });

  it("新增规则走 POST 基路径并带获取方式与综合体", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(RULES), post));
    fireEvent.click(await screen.findByTestId("points-rule-create-open"));
    fireEvent.change(screen.getByLabelText("综合体 id"), { target: { value: "prj_A" } });
    fireEvent.change(screen.getByLabelText("规则名称"), { target: { value: "签到积分" } });
    fireEvent.change(screen.getByLabelText("获取方式"), { target: { value: "check_in" } });
    fireEvent.change(screen.getByLabelText("获取系数"), { target: { value: "5" } });
    fireEvent.click(screen.getByTestId("points-rule-guided-next"));
    await waitFor(() => expect(screen.getByTestId("points-rule-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("points-rule-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/customer/points-rules",
      expect.objectContaining({ ruleName: "签到积分", earnMethod: "check_in", earnRatio: 5, projectId: "prj_A" }),
    ));
  });

  it("非法 JSON 等级阈值被前端拦下不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(RULES), post));
    fireEvent.click(await screen.findByTestId("points-rule-create-open"));
    fireEvent.change(screen.getByLabelText("综合体 id"), { target: { value: "prj_A" } });
    fireEvent.change(screen.getByLabelText("规则名称"), { target: { value: "坏JSON" } });
    fireEvent.change(screen.getByLabelText("获取系数"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("等级阈值JSON"), { target: { value: "{not json}" } });
    fireEvent.click(screen.getByTestId("points-rule-guided-next"));
    expect(screen.getByTestId("points-rule-guided-error").textContent).toContain("JSON");
    expect(post).not.toHaveBeenCalled();
  });
});
