// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-03 clients-R8（BSM 第 1379 行「客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）」）：
// 关闭客人投诉须填客人反馈；满意度由客人本人在客人端评（R26，BSM 第 1385 行），员工回访不再提交满意度。附件按工单查看权限受控查看。
const COMPLAINT = {
  id: "wo-c1", code: "FWO-C1", workOrderType: "complaint", priority: "normal", status: "pending_review", title: "服务员态度差",
  source: "customer", reporterType: "customer", assigneeId: "usr_care",
};
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/facility/work-orders") return [COMPLAINT];
      if (url === "/api/v1/facility/work-orders/wo-c1/customer-media") {
        return [{ id: "ccm_1", contentType: "audio/mp4", fileSize: 4, createdAt: "2026-10-22T00:00:00Z" }];
      }
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props = {
    api,
    user: { id: "usr_care", username: "care", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

async function clickReview() {
  await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-c1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-c1"));
  await waitFor(() => expect(screen.queryByTestId("facility-work-order-review-wo-c1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("facility-work-order-review-wo-c1"));
}

describe("FacilityWorkbenchPage 客人投诉关闭（clients-R8）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("关闭客人投诉须先填客人反馈，提交带客人反馈、不带满意度", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);

    await clickReview();
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("关闭客人投诉须填写客人反馈"));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-work-order-customer-feedback"), { target: { value: "客人接受道歉" } });
    await clickReview();
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-c1/review",
      expect.objectContaining({ customerFeedback: "客人接受道歉", satisfaction: undefined })));
  });

  it("客人投诉明细可查看客人附件", async () => {
    renderPage(makeApi(vi.fn()));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-wo-c1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-wo-c1"));
    await waitFor(() => expect(screen.getByTestId("facility-work-order-customer-media")).toBeInTheDocument());
    expect(screen.getByText(/语音 1/).closest("a")?.getAttribute("href"))
      .toContain("/api/v1/facility/work-orders/wo-c1/customer-media/ccm_1/file");
  });
});

// 第十八批 M 段第 111 条 111-R3：BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」。
describe("FacilityWorkbenchPage 客诉跟进（第 111 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("投诉行的操作里有「客诉跟进」，点开读该单的物业链跟进", async () => {
    const api = makeApi(vi.fn(async () => ({}) as unknown));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-actions-wo-c1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-actions-wo-c1"));
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-complaint-followup-wo-c1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-complaint-followup-wo-c1"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/complaint-followups/facility/wo-c1"));
    expect(screen.getByTestId("complaint-followup")).toBeTruthy();
  });
});
