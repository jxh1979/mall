import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 超池特批入口(S9 遗留补齐):超池 draft 单在结算审阅页可见"超了多少",平台负责人走高危二次确认特批;
// 无 settlement.bill.special_approve 权限者看得到状态但没有按钮。

const CAN_APPROVE: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["finance.settlement.view", "settlement.bill.special_approve"], denies: [], wildcard: false },
};
const VIEW_ONLY: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["finance.settlement.view"], denies: [], wildcard: false },
};

const OVERPOOL_DRAFT = {
  id: "bill_op", projectId: "p1", periodStart: "2026-04-01", periodEnd: "2026-04-30",
  ticketRevenueCent: 100000, channelFeeCent: 0, compServiceFeePooledCent: 20000, compServiceFeeExclusiveCent: 0,
  feeBeneficiaryEntityId: null, feeBeneficiaryEntityName: null,
  availablePoolCent: 80000, distributedCent: 100000, comboDiscountCent: 0, roundingResidualCent: 0,
  distributionMethod: "by_fixed_amount", status: "draft",
  specialApprovalRef: null, specialApprovalValidUntil: null, pooledDistributedCent: 100000,
};
const APPROVED_CONFIRMED = {
  ...OVERPOOL_DRAFT, id: "bill_ok", status: "confirmed",
  specialApprovalRef: "SA-1", specialApprovalValidUntil: "2099-12-31",
};

// 流水表至少一行,否则整页 AdminDataState 落 empty 态、结算单区不渲染。
const TRANSACTION_ROW = {
  id: "s1", transactionNo: "ST-1", projectId: "p1", merchantId: "m1", exclusiveMerchantId: null,
  ticketTypeId: null, entitlementType: "entry", beneficiaryType: "merchant",
  verifiedCount: 2, numPeople: 2, verifiedAmountCent: 20000, settlementStatus: "draft", createdAt: "2026-04-15T10:00:00Z",
};

function makeApi(bills: unknown[] = [OVERPOOL_DRAFT]): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.startsWith("/api/v1/settlement/bills")) return bills;
      if (url.includes("/analytics/metric-detail")) throw new Error("metric off");
      return [];
    }),
    getPaged: vi.fn(async () => ({ items: [TRANSACTION_ROW], total: 1, page: 1, size: 20 })),
    post: vi.fn(async () => ({ specialApprovalRef: "SA-2026-04-001", validUntil: "2026-11-01" })),
    put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, perms: EffectivePermissionsState) {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult, setGlobalError: vi.fn(), effectivePermissions: perms, navigateTo: vi.fn(),
  };
  render(<SettlementReviewPage {...props} />);
  return { setActionResult };
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("SettlementReviewPage 超池特批入口", () => {
  it("超池 draft 单:显示超池金额与「超池待特批」,有权限者有特批按钮;空理由被前置校验拦下不发请求", async () => {
    const api = makeApi();
    renderPage(api, CAN_APPROVE);
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_op")).toBeTruthy());

    // 超了多少:池分配 1000.00 − 可分配池 800.00 = 200.00 元。
    expect(screen.getByTestId("settlement-bill-overpool-amount-bill_op").textContent).toBe("200.00");
    expect(screen.getByTestId("settlement-bill-overpool-bill_op").textContent).toContain("超池待特批");

    fireEvent.click(screen.getByTestId("settlement-bill-approve-bill_op"));
    expect(screen.getByTestId("settlement-special-approval-form")).toBeTruthy();

    // 理由为空 → precheck 拦截:不弹确认框、不发请求、给中文错误提示。
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("settlement-special-approval-submit"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect((api.post as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
    expect(screen.getByTestId("settlement-special-approval-error").textContent).toContain("特批引用必填");
  });

  it("填理由确认后发特批请求,成功提示含引用与有效期并刷新结算单", async () => {
    const api = makeApi();
    const { setActionResult } = renderPage(api, CAN_APPROVE);
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_op")).toBeTruthy());

    fireEvent.click(screen.getByTestId("settlement-bill-approve-bill_op"));
    fireEvent.change(screen.getByTestId("settlement-special-approval-ref"), { target: { value: " SA-2026-04-001 " } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("settlement-special-approval-submit"));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/settlement/bills/bill_op/special-approval",
      { specialApprovalRef: "SA-2026-04-001" },
    ));
    await waitFor(() => expect(setActionResult).toHaveBeenCalled());
    const message = String((setActionResult as ReturnType<typeof vi.fn>).mock.calls[0]?.[0] ?? "");
    expect(message).toContain("SA-2026-04-001");
    expect(message).toContain("2026-11-01");
    // 成功后重取结算单(首载 1 次 + 特批后 1 次)。
    const billCalls = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .filter(([url]) => String(url).startsWith("/api/v1/settlement/bills"));
    expect(billCalls.length).toBe(2);
    // 表单关闭。
    expect(screen.queryByTestId("settlement-special-approval-form")).toBeNull();
  });

  it("取消二次确认则不发请求", async () => {
    const api = makeApi();
    renderPage(api, CAN_APPROVE);
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_op")).toBeTruthy());

    fireEvent.click(screen.getByTestId("settlement-bill-approve-bill_op"));
    fireEvent.change(screen.getByTestId("settlement-special-approval-ref"), { target: { value: "SA-x" } });
    vi.spyOn(window, "confirm").mockReturnValue(false);
    fireEvent.click(screen.getByTestId("settlement-special-approval-submit"));
    expect((api.post as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
  });

  it("无特批权限:看得到超池状态与金额,但没有特批按钮", async () => {
    renderPage(makeApi(), VIEW_ONLY);
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_op")).toBeTruthy());

    expect(screen.getByTestId("settlement-bill-overpool-amount-bill_op").textContent).toBe("200.00");
    expect(screen.getByTestId("settlement-bill-overpool-bill_op").textContent).toContain("超池待特批");
    expect(screen.queryByTestId("settlement-bill-approve-bill_op")).toBeNull();
  });

  it("已特批 confirmed 单:显示特批引用与有效期,无按钮", async () => {
    renderPage(makeApi([APPROVED_CONFIRMED]), CAN_APPROVE);
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_ok")).toBeTruthy());

    const row = screen.getByTestId("settlement-review-bill-bill_ok");
    expect(row.textContent).toContain("已特批 SA-1");
    expect(row.textContent).toContain("有效至 2099-12-31");
    expect(screen.queryByTestId("settlement-bill-approve-bill_ok")).toBeNull();
  });
});
