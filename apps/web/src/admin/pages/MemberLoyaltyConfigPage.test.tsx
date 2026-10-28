// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MemberLoyaltyConfigPage } from "./MemberLoyaltyConfigPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const LEVELS = [{ id: "member_level_1", levelCode: "gold", levelName: "金卡", levelRank: 2, upgradeThreshold: 5000, pointMultiplierBasisPoints: 15000, status: "active" }];
const TIERS = [{ id: "check_in_tier_1", projectId: "prj_A", streakDays: 7, awardPoints: 100, status: "active" }];
const BIRTHDAYS = [{ id: "birthday_rule_1", ruleName: "生日双倍", windowType: "birthday_day", multiplierBasisPoints: 20000, status: "active" }];

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  const get = vi.fn().mockImplementation((url: string) => {
    if (url.includes("member-levels")) return Promise.resolve(LEVELS);
    if (url.includes("check-in-tiers")) return Promise.resolve(TIERS);
    if (url.includes("birthday-rules")) return Promise.resolve(BIRTHDAYS);
    return Promise.resolve([]);
  });
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
  render(<MemberLoyaltyConfigPage {...props} />);
}

describe("MemberLoyaltyConfigPage 会员等级与忠诚度(ADM-CUS-004)", () => {
  it("挂载读回三类配置并按倍率中文化", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/loyalty/config/member-levels"));
    expect(api.get).toHaveBeenCalledWith("/api/v1/loyalty/config/check-in-tiers");
    expect(api.get).toHaveBeenCalledWith("/api/v1/loyalty/config/birthday-rules");
    const level = await screen.findByTestId("member-level-member_level_1");
    expect(level.textContent).toContain("1.50 倍");
  });

  it("新增会员等级把倍率倍数转万分比后 POST", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await screen.findByTestId("member-levels-table");
    fireEvent.change(screen.getByLabelText("等级代码"), { target: { value: "silver" } });
    fireEvent.change(screen.getByLabelText("等级名称"), { target: { value: "银卡" } });
    fireEvent.change(screen.getByLabelText("积分倍率"), { target: { value: "1.2" } });
    fireEvent.click(screen.getByTestId("member-level-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/config/member-levels",
      expect.objectContaining({ levelCode: "silver", levelName: "银卡", pointMultiplierBasisPoints: 12000 }),
    ));
  });

  // 第十八批第 12 条（BSM L820/L821、ENUM L70/L72「全平台统一」，DEC-R3-2026-08-04-35）与同根的生日加倍规则（BSM L831 / ENUM L87「全国平台统一规定，
  // 不按项目分别配置」）：不填综合体 id 也能新增，提交体不带 projectId。
  it("会员等级与生日加倍规则全平台一套，新增不带综合体", async () => {
    const post = vi.fn().mockResolvedValue({});
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);
    await waitFor(() => expect(screen.getByTestId("member-levels-table").textContent).toContain("金卡"));
    fireEvent.change(screen.getByLabelText("等级代码"), { target: { value: "silver" } });
    fireEvent.change(screen.getByLabelText("等级名称"), { target: { value: "银卡" } });
    fireEvent.click(screen.getByTestId("member-level-save"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("projectId");
    fireEvent.change(screen.getByLabelText("生日规则名称"), { target: { value: "生日双倍积分" } });
    fireEvent.click(screen.getByTestId("birthday-rule-save"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2));
    expect(post.mock.calls[1]?.[0]).toBe("/api/v1/loyalty/config/birthday-rules");
    expect(post.mock.calls[1]?.[1]).not.toHaveProperty("projectId");
    expect(setGlobalError).not.toHaveBeenCalled();
  });

  // OBS-13-01（BSM L820）：等级只按滚动统计周期内的成长值与升级门槛定级，保护期等三项参数全平台统一另行配置；
  // HTML 没有每级的保级门槛、考核周期与宽限期，后台不再录入。
  it("等级档位不再录入保级门槛、考核周期与宽限期", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("member-levels-table").textContent).toContain("金卡"));
    expect(screen.queryByLabelText("宽限期天数")).toBeNull();
    expect(screen.queryByLabelText("保级门槛")).toBeNull();
    expect(screen.queryByLabelText("考核周期天数")).toBeNull();
    expect(screen.getByTestId("member-levels-table").textContent).not.toContain("宽限期");
    fireEvent.change(screen.getByLabelText("综合体 id 查询"), { target: { value: "prj_A" } });
    fireEvent.change(screen.getByLabelText("等级代码"), { target: { value: "silver" } });
    fireEvent.change(screen.getByLabelText("等级名称"), { target: { value: "银卡" } });
    fireEvent.click(screen.getByTestId("member-level-save"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("graceDays");
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("keepThreshold");
  });

  it("签到天数小于 1 被前端拦下报错不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);
    await screen.findByTestId("check-in-tiers-table");
    fireEvent.change(screen.getByLabelText("综合体 id 查询"), { target: { value: "prj_A" } });
    fireEvent.change(screen.getByLabelText("连续签到天数"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText("签到奖励积分"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("check-in-tier-save"));
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("连续签到天数"));
    expect(post).not.toHaveBeenCalled();
  });
});
