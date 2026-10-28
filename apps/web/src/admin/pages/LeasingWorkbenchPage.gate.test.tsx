// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { LeasingWorkbenchPage } from "./LeasingWorkbenchPage";
import { apiErrorFromResponse } from "../../shared/apiError";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批第 30 条：PGM L502「黑名单品牌拦截同品牌新线索，以及由拉黑前既有线索新建意向。确需继续的，须由具备单独授予的黑名单例外放行权限人员操作，
// 按层级对应主管审批……」「意向五阶段中进入方案报价、商务谈判须主管确认才能推进」；PGM L500 五阶段逐级推进。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const LEAD = { id: "l-1", contactName: "张经理", source: "exhibition", brandId: "br-black", status: "following_up" };
const INTENT = { id: "in-1", leadId: "l-9", stage: "requirement_confirmed" };
const BRAND = { id: "br-black", brandName: "旧品牌", brandType: "blacklist", businessType: "retail", blacklistReason: "多次违约" };
const APPROVED = { id: "lbe-1", targetType: "intent_create", brandId: "br-black", leadId: "l-1", reason: "品牌整改完毕", status: "approved", requestedBy: "u1", approvedBy: "boss" };

function makeApi(post: ReturnType<typeof vi.fn>, exceptions: unknown[] = []): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/leasing/blacklist-exceptions")) return exceptions;
      if (url.includes("/leasing/leads")) return [LEAD];
      if (url.includes("/leasing/intents")) return [INTENT];
      if (url.includes("/leasing/brands")) return [BRAND];
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<LeasingWorkbenchPage {...props} />);
}

function blacklisted(path: string) {
  return apiErrorFromResponse(path, 409, JSON.stringify({
    success: false, code: "STATE_CONFLICT", detail_code: "LEASING_BRAND_BLACKLISTED",
    message: "该品牌已列入黑名单，拉黑前的既有线索不能新建意向",
  }));
}

function postedTo(post: ReturnType<typeof vi.fn>, path: string) {
  return post.mock.calls.find((call) => call[0] === path)?.[1] as Record<string, unknown> | undefined;
}

describe("LeasingWorkbenchPage 黑名单例外放行与意向关键阶段", () => {
  it("转意向被黑名单拦截后出现例外放行申请，提交带放行事项、线索与理由", async () => {
    const post = vi.fn(async (path: string) => {
      if (path === "/api/v1/leasing/intents") throw blacklisted(path);
      return { id: "lbe-new", status: "pending_approval" };
    });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("leasing-intent-create")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leasing-intent-create"));

    await waitFor(() => expect(screen.queryByTestId("leasing-blacklist-exception-form")).not.toBeNull());
    fireEvent.change(screen.getByTestId("leasing-blacklist-exception-reason"), { target: { value: "品牌整改完毕" } });
    fireEvent.click(screen.getByTestId("leasing-blacklist-exception-submit"));

    await waitFor(() => expect(postedTo(post, "/api/v1/leasing/blacklist-exceptions")).toBeDefined());
    expect(postedTo(post, "/api/v1/leasing/blacklist-exceptions")).toMatchObject({ targetType: "intent_create", leadId: "l-1", reason: "品牌整改完毕" });
  });

  it("品牌库选中黑名单品牌给出新建线索的例外放行申请，提交带品牌", async () => {
    const post = vi.fn(async () => ({ id: "lbe-new", status: "pending_approval" }));
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("leasing-brand-blacklist-exception-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leasing-brand-blacklist-exception-open"));
    fireEvent.change(screen.getByTestId("leasing-blacklist-exception-reason"), { target: { value: "品牌整改完毕" } });
    fireEvent.click(screen.getByTestId("leasing-blacklist-exception-submit"));

    await waitFor(() => expect(postedTo(post, "/api/v1/leasing/blacklist-exceptions")).toBeDefined());
    expect(postedTo(post, "/api/v1/leasing/blacklist-exceptions")).toMatchObject({ targetType: "lead_create", brandId: "br-black", reason: "品牌整改完毕" });
  });

  it("已批准的例外放行单「按批准转意向」提交带放行单号", async () => {
    const post = vi.fn(async () => ({ id: "in-new", stage: "initial_contact" }));
    renderPage(makeApi(post, [APPROVED]));
    await waitFor(() => expect(screen.queryByTestId("leasing-blacklist-exception-use-lbe-1")).not.toBeNull());
    expect(screen.getByTestId("leasing-blacklist-exception-row-lbe-1").textContent).toContain("已批准");
    fireEvent.click(screen.getByTestId("leasing-blacklist-exception-use-lbe-1"));

    await waitFor(() => expect(postedTo(post, "/api/v1/leasing/intents")).toBeDefined());
    expect(postedTo(post, "/api/v1/leasing/intents")).toMatchObject({ leadId: "l-1", blacklistExceptionId: "lbe-1" });
  });

  it("选中需求确认的意向默认推进到方案报价，提交不带主管编号并提示待主管确认", async () => {
    const post = vi.fn(async () => ({ id: "in-1", stage: "requirement_confirmed", pendingStage: "proposal_quoted" }));
    renderPage(makeApi(post));
    await waitFor(() => expect((screen.getByTestId("leasing-intent-stage") as HTMLSelectElement).value).toBe("proposal_quoted"));
    fireEvent.click(screen.getByTestId("leasing-intent-advance"));

    await waitFor(() => expect(postedTo(post, "/api/v1/leasing/intents/in-1/advance")).toBeDefined());
    const body = postedTo(post, "/api/v1/leasing/intents/in-1/advance");
    expect(body).toMatchObject({ targetStage: "proposal_quoted" });
    expect(body).not.toHaveProperty("supervisorUserId");
    await waitFor(() => expect(screen.queryByTestId("leasing-intent-pending-note")?.textContent ?? "").toContain("已提交主管确认"));
  });
});
