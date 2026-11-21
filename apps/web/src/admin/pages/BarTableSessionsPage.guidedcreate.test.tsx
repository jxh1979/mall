import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const BARS = [{ id: "bar-main", merchantName: "主酒吧", merchantType: "bar" }];
// 第十八批 BL 段第 380 条 380-R2（BSM:717）：新建卡座按所选酒吧取该酒吧名下启用中的空间。
const SPACE_OPTIONS: Record<string, Array<{ id: string; spaceName: string }>> = {
  "bar-main": [{ id: "sp_1", spaceName: "卡座-01" }],
  "bar-2": [{ id: "sp_9", spaceName: "二号卡座" }],
  "bar-own": [{ id: "sp_own", spaceName: "本店卡座" }],
};

function makeApi(post = vi.fn().mockResolvedValue({ id: "tbl-new" }), bars = BARS): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) => {
      if (path.startsWith("/api/v1/platform/merchants")) return Promise.resolve(bars);
      if (path.startsWith("/api/v1/bar/tables/space-options")) {
        return Promise.resolve(SPACE_OPTIONS[new URLSearchParams(path.split("?")[1]).get("bar_id") ?? ""] ?? []);
      }
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, user: AdminPageProps["user"] = { id: "u1", username: "admin", accountType: "platform_admin" }): void {
  const props: AdminPageProps = {
    api,
    user,
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarTableSessionsPage {...props} />);
}

// 引导式流程 phase-5：新建卡座从内联表单改为「＋新建卡座」按钮 → 分步引导弹窗（订台/开台不动）。
describe("BarTableSessionsPage 引导式新建卡座", () => {
  it("入口→步进→提交携带卡座字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "tbl-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    // 步骤1 卡座信息（barId 从下拉自动选中第一个 bar-main；tableName 需填）
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-main"));
    // 第十八批 Q 段 151-R2：只录可进人数一个数（BSM:1036「已确认与已入场总人数不超过卡座可进人数」；ENUM:105「成员确认接受即占用该卡座的免通票入场名额」）。
    expect(screen.getByTestId("bar-new-table-capacity")).toBeTruthy();
    expect(screen.queryByTestId("bar-new-table-free")).toBeNull();
    // 第十八批 BL 段第 380 条 380-R2（BSM:717「酒吧要知道桌台、卡座、吧台属于哪个空间和商户。」）：所属空间必选，从该酒吧名下可选空间里选。
    await waitFor(() => expect(spaceOptions()).toContain("卡座-01"));
    fireEvent.change(screen.getByTestId("bar-new-table-space"), { target: { value: "sp_1" } });
    fireEvent.change(screen.getByTestId("bar-new-table-name"), { target: { value: "VIP-01" } });
    fireEvent.change(screen.getByTestId("bar-new-table-spend"), { target: { value: "500" } });
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    // 确认步 → 提交（确认页也不再列「免通票入场人数」）
    await waitFor(() => expect(screen.getByTestId("bar-new-table-submit")).toBeTruthy());
    expect(screen.getByTestId("bar-new-table-guided").textContent).not.toContain("免通票入场人数");
    expect(screen.getByTestId("bar-new-table-guided-summary").textContent).toContain("所属空间卡座-01");
    fireEvent.click(screen.getByTestId("bar-new-table-submit"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/bar/tables");
    expect(call?.[1]).toEqual({ barId: "bar-main", tableName: "VIP-01", tableType: "card_seat", capacity: 4, minimumSpendCent: 50000, spaceId: "sp_1" });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("bar-new-table-guided")).toBeNull());
  });

  it("卡座信息缺卡座名称 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    // barId 从下拉自动选中 bar-main；不填卡座名称（默认空）→ 校验应命中卡座名称
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-main"));
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-guided-error").textContent).toContain("卡座名称"));
    expect(screen.queryByTestId("bar-new-table-submit")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(false);
  });

  // 第十八批 W 段 205-R1：PGM:287「枚举取值……提示、报错……必须全部为中文，不得出现任何英文」；第十八批裁决 37 操作员原话与所选选项都叫「包厢」。
  it("桌台类型叫「包厢」：下拉、名称提示与确认页都不出现英文 VIP 与「包坐」", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-main"));
    const typeSelect = screen.getByTestId("bar-new-table-type") as HTMLSelectElement;
    expect(Array.from(typeSelect.options).find((o) => o.value === "vip")?.textContent).toBe("包厢（同卡座流转）");
    expect(screen.getByTestId("bar-new-table-name").getAttribute("placeholder")).toBe("如 包厢-01");
    expect(screen.getByTestId("bar-new-table-guided").innerHTML).not.toMatch(/VIP|包坐/);
    fireEvent.change(typeSelect, { target: { value: "vip" } });
    await waitFor(() => expect(spaceOptions()).toContain("卡座-01"));
    fireEvent.change(screen.getByTestId("bar-new-table-space"), { target: { value: "sp_1" } });
    fireEvent.change(screen.getByTestId("bar-new-table-name"), { target: { value: "包厢-01" } });
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-guided-summary").textContent).toContain("桌台类型包厢"));
    expect(screen.getByTestId("bar-new-table-guided").innerHTML).not.toMatch(/VIP|包坐/);
  });

  // 第十八批 BL 段第 380 条 380-R2：不选所属空间 → 步内报错，不提交。
  it("不选所属空间点下一步 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    await waitFor(() => expect(spaceOptions()).toContain("卡座-01"));
    fireEvent.change(screen.getByTestId("bar-new-table-name"), { target: { value: "A-01" } });
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-guided-error").textContent).toContain("所属空间"));
    expect(screen.queryByTestId("bar-new-table-submit")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(false);
  });

  // 第十八批 BL 段第 380 条 380-R2：换所属酒吧 → 按新酒吧重取可选空间，已选空间清空。
  it("换所属酒吧后按新酒吧重取可选空间，已选空间清空", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post, [...BARS, { id: "bar-2", merchantName: "二号酒吧", merchantType: "bar" }]);
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    await waitFor(() => expect(spaceOptions()).toContain("卡座-01"));
    fireEvent.change(screen.getByTestId("bar-new-table-space"), { target: { value: "sp_1" } });
    fireEvent.change(screen.getByTestId("bar-new-table-bar"), { target: { value: "bar-2" } });
    await waitFor(() => expect(spaceOptions()).toContain("二号卡座"));
    expect((api.get as ReturnType<typeof vi.fn>).mock.calls.some((c) => c[0] === "/api/v1/bar/tables/space-options?bar_id=bar-2")).toBe(true);
    // 已选空间须随换酒吧清空（受控下拉的 DOM 值在选项里没有原空间时也是空串，故按状态验）：不重选空间直接下一步，步内报所属空间必选，不提交。
    fireEvent.change(screen.getByTestId("bar-new-table-name"), { target: { value: "B-01" } });
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("bar-new-table-guided-error")?.textContent ?? "").toContain("所属空间"));
    expect(screen.queryByTestId("bar-new-table-submit")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(false);
  });

  // 第十八批 BL 段控制席并入第 415 条（BSM:1017「各处填写「所属酒吧」时从已引进的酒吧商户里下拉选择」）：挂商户的酒吧经理所属酒吧锁定本店，
  // 不发平台商户请求，可选空间按本店取。
  it("挂商户的账号：所属酒吧锁定本店，不发平台商户请求，可选空间按本店取", async () => {
    const api = makeApi();
    renderPage(api, { id: "u2", username: "manager", accountType: "staff", merchantId: "bar-own" });
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-own"));
    await waitFor(() => expect(spaceOptions()).toContain("本店卡座"));
    expect((api.get as ReturnType<typeof vi.fn>).mock.calls.some((c) => String(c[0]).startsWith("/api/v1/platform/merchants"))).toBe(false);
  });
});

function spaceOptions(): Array<string | null> {
  return Array.from((screen.getByTestId("bar-new-table-space") as HTMLSelectElement).options).map((o) => o.textContent);
}

// 第十八批 BA 段第 267 条 267-R2（PGM:287「……字段名……提示、报错……必须全部为中文……不得以“这是标识”为由保留英文」）：
// 订台、开台表单字段名与占位提示里的「ID」改「编号」。
describe("BarTableSessionsPage 订台与开台表单不出现英文 ID（第 267 条）", () => {
  it("新建订台与开台表单的字段名、占位提示都写「编号」", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("bar-create-reservation-form").textContent).toContain("卡座编号"));
    for (const testId of ["bar-create-reservation-form", "bar-create-session-form"]) {
      const form = screen.getByTestId(testId);
      expect(form.textContent).not.toMatch(/(^|[^A-Za-z])ID([^A-Za-z]|$)/);
      for (const el of Array.from(form.querySelectorAll("[placeholder]"))) expect(el.getAttribute("placeholder")).not.toMatch(/(^|[^A-Za-z])ID([^A-Za-z]|$)/);
    }
  });
});
