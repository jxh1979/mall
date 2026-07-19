import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MemberLoyaltyConfigPage } from "./MemberLoyaltyConfigPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const LEVELS = [{ id: "member_level_1", projectId: "prj_A", levelCode: "gold", levelName: "金卡", levelRank: 2, upgradeThreshold: 5000, keepThreshold: 3000, keepPeriodDays: 365, graceDays: 30, pointMultiplierBasisPoints: 15000, status: "active" }];
const TIERS = [{ id: "check_in_tier_1", projectId: "prj_A", streakDays: 7, awardPoints: 100, status: "active" }];
const BIRTHDAYS = [{ id: "birthday_rule_1", projectId: "prj_A", ruleName: "生日双倍", windowType: "birthday_day", multiplierBasisPoints: 20000, status: "active" }];

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
    fireEvent.change(screen.getByLabelText("综合体 id 查询"), { target: { value: "prj_A" } });
    fireEvent.change(screen.getByLabelText("等级代码"), { target: { value: "silver" } });
    fireEvent.change(screen.getByLabelText("等级名称"), { target: { value: "银卡" } });
    fireEvent.change(screen.getByLabelText("积分倍率"), { target: { value: "1.2" } });
    fireEvent.click(screen.getByTestId("member-level-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/config/member-levels",
      expect.objectContaining({ levelCode: "silver", levelName: "银卡", pointMultiplierBasisPoints: 12000, projectId: "prj_A" }),
    ));
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
