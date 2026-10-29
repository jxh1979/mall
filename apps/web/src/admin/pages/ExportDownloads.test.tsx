// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
import { MonthlyComparisonPage } from "./MonthlyComparisonPage";
import { SettlementInvoicePage } from "./SettlementInvoicePage";
import { WatermarkTracePage } from "./WatermarkTracePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第 23 条 23-R3（PGM:687「所有导出的文件一律带密码……导出文件中的数据不可复制、不可编辑」）：三处导出改为下载服务端生成的带密码 PDF，
// 浏览器不再自己拼 CSV、结果区不放数据行；开票清单导出要单独授予的导出权限；反查页可按导出密码反查导出人、时间与设备。

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const PDF = { blob: new Blob(["%PDF"], { type: "application/pdf" }), fileName: "导出.pdf" };

let created: Blob[];

beforeEach(() => {
  created = [];
  URL.createObjectURL = vi.fn((blob: Blob) => {
    created.push(blob);
    return "blob:export";
  }) as unknown as typeof URL.createObjectURL;
  URL.revokeObjectURL = vi.fn();
});

type TestApi = AdminApiClient & { getFile: Mock; post: Mock; get: Mock };

function makeApi(get: (path: string) => unknown = () => []): TestApi {
  return {
    get: vi.fn(async (path: string) => get(path)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    getFile: vi.fn().mockResolvedValue(PDF),
  } as unknown as TestApi;
}

function props(api: AdminApiClient, effectivePermissions = ALL): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
}

const INVOICE = {
  id: "sii_1", billId: "sb_1", settlementItemId: null, projectId: "p1",
  periodStart: "2026-05-01", periodEnd: "2026-05-31", incomeType: "ticket",
  subjectEntityId: "be_plat", subjectName: "娱乐平台公司", invoiceItemName: "综合体验服务费",
  taxCategory: "综合体验/娱乐平台服务", evidenceRequired: "对账单",
  amountCent: 10000, vatRateBps: 600, vatAmountCent: 566,
  culturalFeeApplicable: true, culturalFeeRateBps: 300, culturalFeeCent: 300,
  status: "pending", invoiceNo: null, remark: null,
};

function invoiceApi(): TestApi {
  return makeApi((path) => (path.includes("/summary")
    ? { subjects: [], rules: [], overIssued: [] }
    : { items: [INVOICE], page: 1, pageSize: 200, total: 1 }));
}

describe("第 23 条 23-R3 导出一律下载服务端生成的带密码 PDF", () => {
  it("月度对比导出调月报导出接口下载 PDF，不再在浏览器拼 CSV，提示到「我的导出」查密码", async () => {
    const api = makeApi((path) => (path.includes("/monthly-comparison") ? { month: "2026-07", rows: [] } : []));
    const page = props(api);
    render(<MonthlyComparisonPage {...page} />);
    await waitFor(() => expect(screen.queryByTestId("monthly-comparison-export")).not.toBeNull());
    fireEvent.click(screen.getByTestId("monthly-comparison-export"));

    await waitFor(() => expect(api.getFile).toHaveBeenCalledWith(expect.stringMatching(/^\/api\/v1\/analytics\/reports\/monthly-export\?month=\d{4}-\d{2}$/)));
    await waitFor(() => expect(created.map((blob) => blob.type)).toEqual(["application/pdf"]));
    expect(page.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ message: expect.stringContaining("我的导出") }));
    expect(screen.getByTestId("monthly-comparison-export").textContent).toContain("带密码");
  });

  it("应收应付台账导出下载 PDF，结果区只记已导出、不放数据行", async () => {
    const api = makeApi();
    api.getPaged = vi.fn().mockResolvedValue({
      items: [{ id: "ar-1", itemNo: "AR-0001", direction: "receivable", status: "open", amountCent: 10000, unpaidAmountCent: 10000, dueDate: "2026-08-01" }],
      total: 1, page: 1, size: 10,
    });
    const page = props(api);
    render(<FinanceArApLedgerPage {...page} />);
    await waitFor(() => expect(screen.queryByTestId("finance-export")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-export"));

    await waitFor(() => expect(api.getFile).toHaveBeenCalledWith("/api/v1/finance/ar-ap-items/export"));
    await waitFor(() => expect(page.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "ledger_export", status: "exported" })));
    const result = (page.setActionResult as Mock).mock.calls.at(-1)?.[0] as Record<string, unknown>;
    expect(result).not.toHaveProperty("result");
    expect(created.map((blob) => blob.type)).toEqual(["application/pdf"]);
  });

  it("开票清单导出带当前筛选调后端导出接口下载 PDF", async () => {
    const api = invoiceApi();
    render(<SettlementInvoicePage {...props(api)} />);
    await waitFor(() => expect(screen.queryByText("综合体验服务费")).not.toBeNull());
    fireEvent.change(screen.getByTestId("settlement-invoice-status-filter"), { target: { value: "pending" } });
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(expect.stringContaining("status=pending")));
    await waitFor(() => expect(screen.queryByText("综合体验服务费")).not.toBeNull());
    fireEvent.click(screen.getByTestId("settlement-invoice-export"));

    await waitFor(() => expect(api.getFile).toHaveBeenCalledWith("/api/v1/settlement/invoice-items/export?status=pending"));
    await waitFor(() => expect(created.map((blob) => blob.type)).toEqual(["application/pdf"]));
  });

  it("没有开票清单导出权限（settlement.invoice.export）的账号导出按钮不可用", async () => {
    const api = invoiceApi();
    render(<SettlementInvoicePage {...props(api, { status: "ready", value: { grants: ["settlement.invoice.view"], denies: [], wildcard: false } })} />);
    await waitFor(() => expect(screen.queryByText("综合体验服务费")).not.toBeNull());
    expect((screen.getByTestId("settlement-invoice-export") as HTMLButtonElement).disabled).toBe(true);
  });

  it("反查页按导出密码查到导出人、导出时间与导出设备（密码放请求体）", async () => {
    const api = makeApi();
    api.post.mockResolvedValue({
      exportFileId: "exp_1", exporterId: "usr_a", username: "张三", exportedAt: "2026-10-29T01:02:03Z", deviceNo: "WBABCDEFGHJKMN",
      network: "10.0.0.1", exportType: "finance_ar_ap_ledger", filters: { merchantId: "m1" }, rowCount: 3,
    });
    render(<WatermarkTracePage {...props(api)} />);
    await waitFor(() => expect(screen.queryByTestId("export-password-input")).not.toBeNull());
    fireEvent.change(screen.getByTestId("export-password-input"), { target: { value: "ABCDEFGHJK" } });
    fireEvent.click(screen.getByTestId("export-password-submit"));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/security/export-files/lookup", { password: "ABCDEFGHJK" }));
    await waitFor(() => expect(screen.queryByTestId("export-password-result")?.textContent ?? "").toContain("张三"));
    const text = screen.getByTestId("export-password-result").textContent ?? "";
    expect(text).toContain("WBABCDEFGHJKMN");
    expect(text).toContain("10.0.0.1");
    expect(text).toContain("应收应付台账");
    expect(text).toContain("2026/10/29 09:02:03");
  });
});
