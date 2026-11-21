// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BR 段第 357 条 357-R1（BSM:753「游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算」；ENUM:53「采用哪种规则由域主管公司决定，
// 商户不能决定」）：项目表显示该项目引用的内部结算价目录条目，未引用写「不引用（不参与域内结算）」；表格上方选项目、填编码保存，请求体为蛇形键
// { idempotency_key, catalog_item_code }，成功后列表显示编码，后端拒绝时显示后端文案。
type Call = { url: string; method: string; body: Record<string, unknown> | null };
const UNUSABLE = "所填目录条目不存在、不属于本项目所属商户，或还没有获批生效的内部结算价";

function textBody(status: number, payload: unknown): Response {
  return new Response(JSON.stringify(payload), { status, headers: { "content-type": "application/json" } });
}

function ok(data: unknown): Response {
  return textBody(200, { success: true, code: "OK", message: "success", data });
}

function makeApi(calls: Call[], reject: boolean): AdminApi {
  let reference: string | null = null;
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = String(init?.method ?? "GET").toUpperCase();
    const body = init?.body ? JSON.parse(String(init.body)) as Record<string, unknown> : null;
    calls.push({ url, method, body });
    if (method === "PUT" && url.includes("/internal-price-item")) {
      if (reject) {
        return Promise.resolve(textBody(400, { success: false, code: "BAD_REQUEST", detail_code: "AMUSEMENT_PRICE_ITEM_UNUSABLE", message: UNUSABLE }));
      }
      reference = String(body?.catalog_item_code);
      return Promise.resolve(ok({ id: "amuse-1", project_name: "旋转木马", catalog_item_code: reference }));
    }
    if (url.includes("/amusement/projects") && method === "GET" && !url.includes("/entitlements")) {
      return Promise.resolve(ok({ items: [{ id: "amuse-1", project_name: "旋转木马", capacity: 30, status: "open", catalog_item_code: reference }],
        page: 1, page_size: 50, total: 1 }));
    }
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(calls: Call[], reject = false, user: AdminPageProps["user"] = { id: "u1", username: "admin", accountType: "platform_admin" }) {
  const props: AdminPageProps = {
    api: makeApi(calls, reject),
    user,
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AmusementSessionsPage {...props} />);
}

describe("AmusementSessionsPage 游乐项目内部结算价条目引用（357-R1）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("未引用显示「不引用（不参与域内结算）」；选项目填编码保存，蛇形键提交后列表显示编码", async () => {
    const calls: Call[] = [];
    renderPage(calls);
    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1").textContent).toContain("不引用（不参与域内结算）"));
    await waitFor(() => expect((screen.getByTestId("amusement-price-ref-project") as HTMLSelectElement).value).toBe("amuse-1"));

    fireEvent.change(screen.getByTestId("amusement-price-ref-code"), { target: { value: "sku_ride_1" } });
    fireEvent.click(screen.getByTestId("amusement-price-ref-save"));

    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1").textContent).toContain("sku_ride_1"));
    const put = calls.find((c) => c.method === "PUT")!;
    expect(put.url).toContain("/api/v1/amusement/projects/amuse-1/internal-price-item");
    expect(Object.keys(put.body ?? {}).sort()).toEqual(["catalog_item_code", "idempotency_key"]);
    expect(put.body?.catalog_item_code).toBe("sku_ride_1");
  });

  it("后端拒绝（不属于本项目所属商户或未获批）时显示后端文案", async () => {
    const calls: Call[] = [];
    renderPage(calls, true);
    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1").textContent).toContain("不引用（不参与域内结算）"));
    await waitFor(() => expect((screen.getByTestId("amusement-price-ref-project") as HTMLSelectElement).value).toBe("amuse-1"));

    fireEvent.change(screen.getByTestId("amusement-price-ref-code"), { target: { value: "sku_other" } });
    fireEvent.click(screen.getByTestId("amusement-price-ref-save"));

    await waitFor(() => expect(screen.getByTestId("amusement-price-ref-error").textContent).toContain(UNUSABLE));
  });

  it("游乐商户账号（带商户范围）不显示引用表单，项目表照常显示该列（ENUM:53 商户不能决定）", async () => {
    const calls: Call[] = [];
    renderPage(calls, false, { id: "u2", username: "amusement_shop", accountType: "staff", merchantId: "shop-1" });
    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1").textContent).toContain("不引用（不参与域内结算）"));
    expect(screen.queryByTestId("amusement-price-ref-form")).toBeNull();
    expect(screen.queryByTestId("amusement-price-ref-save")).toBeNull();
  });

  it("平台账号显示引用表单", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1").textContent).toContain("不引用（不参与域内结算）"));
    expect(screen.getByTestId("amusement-price-ref-form")).toBeTruthy();
  });
});
