import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OperatingParamsPage } from "./OperatingParamsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("anomaly-config")) return { surgePct: 0.5, highPct: 0.8, enabled: true };
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
  render(<OperatingParamsPage {...props} />);
}

// 引导式流程 phase-6：运营参数录入从内联表单改为「＋录入运营参数」按钮 → 分步引导弹窗。
describe("OperatingParamsPage 引导式录入运营参数", () => {
  it("入口→步进→提交携带综合体 id 与面积", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("operating-params-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("operating-params-create-open"));
    fireEvent.change(screen.getByLabelText("综合体编号"), { target: { value: "project_1" } });
    fireEvent.change(screen.getByLabelText("可经营面积（㎡）"), { target: { value: "8888" } });
    fireEvent.click(screen.getByTestId("operating-params-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("operating-params-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/analytics/operating-params")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/analytics/operating-params");
    expect(call?.[1]).toMatchObject({ projectId: "project_1", operatingAreaSqm: 8888, status: "active" });
    await waitFor(() => expect(screen.queryByTestId("operating-params-guided")).toBeNull());
  });

  it("缺综合体 id → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("operating-params-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("operating-params-create-open"));
    // 只填面积，不填综合体 id
    fireEvent.change(screen.getByLabelText("可经营面积（㎡）"), { target: { value: "8888" } });
    fireEvent.click(screen.getByTestId("operating-params-guided-next"));
    await waitFor(() => expect(screen.getByTestId("operating-params-guided-error").textContent).toContain("综合体编号"));
    expect(screen.queryByTestId("operating-params-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/analytics/operating-params")).toBe(false);
  });
});

// 第十八批 BA 段第 267 条 267-R3（PGM:287；BSM:1832「节假日日历可给网址自动抓取，也可手工维护」）：写「网址」不写 URL；
// 数据源格式名 holiday-cn JSON 照第十八批裁决 73「文件格式名不用中文」保持原样。
describe("OperatingParamsPage 节假日抓取写「网址」（第 267 条）", () => {
  it("抓取区标题、字段名、按钮与空网址报错都写「网址」，不含 URL", async () => {
    const setGlobalError = vi.fn();
    render(<OperatingParamsPage api={makeApi()} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={setGlobalError} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("holiday-fetch").textContent).toBe("从网址抓取"));
    expect(document.body.textContent).not.toContain("URL");
    fireEvent.click(screen.getByTestId("holiday-fetch"));
    expect(setGlobalError).toHaveBeenCalledWith("请填节假日数据源网址(holiday-cn JSON 格式)");
  });
});

// 第十八批 BA 段收尾复核修正（PGM:287「……字段名……提示、报错……必须全部为中文」）：字段名、占位与读屏名里的英文 id 改「编号」。
describe("OperatingParamsPage 字段名不夹英文 id（第 267 条收尾复核）", () => {
  it("引导录入的字段名、占位、读屏名与确认摘要都写「综合体编号」，不出现与中文相邻的 id", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("operating-params-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("operating-params-create-open"));
    await waitFor(() => expect(screen.getByTestId("operating-params-guided")).toBeTruthy());
    const texts = [document.body.textContent ?? "", ...Array.from(document.querySelectorAll("[placeholder],[aria-label]"))
      .flatMap((el) => [el.getAttribute("placeholder") ?? "", el.getAttribute("aria-label") ?? ""])];
    for (const text of texts) expect(text).not.toMatch(/[\u4e00-\u9fff] ?id(?![A-Za-z_])|(^|[^A-Za-z_])id ?[\u4e00-\u9fff]/i);
    expect(screen.getByLabelText("综合体编号").getAttribute("placeholder")).toBe("综合体编号");
  });
});
