// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BW 段第 386 条 386-R2、386-R3（BSM:1354「分类、颜色和提前天数按综合体后台可配」）：
// 合同工作台着色分类区块选综合体、列表按所选综合体显示并标明是否平台默认、新增带综合体；本综合体的行可改、可移除，平台默认行不能。
const DEFAULT_RULES = [{ id: "secr_default_30", project_id: null, category_name: "即将到期", color: "#F59E0B", threshold_days: 30 }];
const A_RULES = [{ id: "clr-a", project_id: "proj-a", category_name: "A临期", color: "#DC2626", threshold_days: 15 }];
const PROJECTS = [{ id: "proj-a", name: "综合体甲" }, { id: "proj-b", name: "综合体乙" }];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeProps(postError = false) {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") {
      if (postError && url.includes("/contracts/expiry-color-rules")) {
        return Promise.resolve(new Response(JSON.stringify({
          success: false, code: "VALIDATION_FAILED", detail_code: "VALIDATION_FAILED", message: "必须指定综合体项目",
        }), { status: 400, headers: { "content-type": "application/json" } }));
      }
      return Promise.resolve(envelope({}));
    }
    if (url.includes("/contracts/expiry-color-rules?project_id=proj-a")) return Promise.resolve(envelope(A_RULES));
    if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope(DEFAULT_RULES));
    if (url.includes("/platform/projects")) return Promise.resolve(envelope(PROJECTS));
    return Promise.resolve(envelope([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin", organizationId: "proj-own" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } } as EffectivePermissionsState,
    navigateTo: vi.fn(),
  };
  return { props, fetchMock };
}

function colorPosts(fetchMock: ReturnType<typeof vi.fn>): Record<string, unknown>[] {
  return fetchMock.mock.calls.filter((c) => String(c[0]).includes("/contracts/expiry-color-rules")
    && (c[1] as { method?: string } | undefined)?.method === "POST")
    .map((c) => JSON.parse(String((c[1] as { body: string }).body)) as Record<string, unknown>);
}

function colorPost(fetchMock: ReturnType<typeof vi.fn>): Record<string, unknown> | null {
  return colorPosts(fetchMock)[0] ?? null;
}

// 第一次保存之后重取着色分类列表的地址（386-R2 方案②：保存成功后重取仍按所选综合体）。
function colorGetAfterPost(fetchMock: ReturnType<typeof vi.fn>): string | null {
  const calls = fetchMock.mock.calls
    .map((c) => `${(c[1] as { method?: string } | undefined)?.method ?? "GET"} ${String(c[0])}`)
    .filter((u) => u.includes("/contracts/expiry-color-rules"));
  const postAt = calls.findIndex((u) => u.startsWith("POST "));
  return postAt < 0 ? null : (calls[postAt + 1] ?? null);
}

const colorTable = () => within(screen.getByTestId("contract-color-table"));

// 页面加载落定（默认列表已上屏、综合体下拉已出现）后选综合体甲，等甲自己的行上屏。
async function pickProjectA() {
  await waitFor(() => expect(colorTable().queryByText("即将到期")).not.toBeNull());
  await waitFor(() => expect(screen.queryByTestId("contract-color-project")).not.toBeNull());
  fireEvent.change(screen.getByTestId("contract-color-project"), { target: { value: "proj-a" } });
  await waitFor(() => expect(colorTable().queryByText("A临期")).not.toBeNull());
}

describe("ContractWorkbenchPage 铺位到期着色分类按综合体（386-R2、386-R3）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("未选综合体：列表显示平台默认行，归属标明「平台默认」，平台默认行没有改、移除按钮", async () => {
    const { props } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(colorTable().queryByText("平台默认")).not.toBeNull());
    expect(screen.queryByTestId("contract-color-edit-secr_default_30")).toBeNull();
    expect(screen.queryByTestId("contract-color-remove-secr_default_30")).toBeNull();
  });

  it("选综合体甲：按甲取着色分类，显示甲自己的行且归属「本综合体」，不再显示平台默认", async () => {
    const { props, fetchMock } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await pickProjectA();
    expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/contracts/expiry-color-rules?project_id=proj-a"))).toBe(true);
    expect(colorTable().queryByText("本综合体")).not.toBeNull();
    expect(colorTable().queryByText("平台默认")).toBeNull();
    expect(screen.queryByTestId("contract-color-edit-clr-a")).not.toBeNull();
  });

  it("选综合体甲后新增：请求体带 project_id=proj-a、分类名与提前天数", async () => {
    const { props, fetchMock } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await pickProjectA();
    const form = within(screen.getByTestId("contract-color-form"));
    fireEvent.change(form.getByLabelText("分类名"), { target: { value: "甲临期红" } });
    fireEvent.change(form.getByLabelText("提前天数"), { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("contract-color-add"));
    await waitFor(() => expect(colorPost(fetchMock)).not.toBeNull());
    expect(colorPost(fetchMock)).toMatchObject({ project_id: "proj-a", category_name: "甲临期红", threshold_days: 20 });
    expect(colorPost(fetchMock)?.id).toBeUndefined();
    await waitFor(() => expect(colorGetAfterPost(fetchMock)).not.toBeNull());
    expect(colorGetAfterPost(fetchMock)).toContain("/contracts/expiry-color-rules?project_id=proj-a");
  });

  it("新增被后端拒绝（必须指定综合体项目）：把提示交给全局错误条", async () => {
    const { props } = makeProps(true);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(colorTable().queryByText("即将到期")).not.toBeNull());
    const form = within(screen.getByTestId("contract-color-form"));
    fireEvent.change(form.getByLabelText("分类名"), { target: { value: "临期红" } });
    fireEvent.change(form.getByLabelText("提前天数"), { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("contract-color-add"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("必须指定综合体项目")));
  });

  it("移除本综合体的一条：请求体带该行编号、active=false 与 project_id=proj-a", async () => {
    const { props, fetchMock } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await pickProjectA();
    fireEvent.click(screen.getByTestId("contract-color-remove-clr-a"));
    await waitFor(() => expect(colorPost(fetchMock)).not.toBeNull());
    expect(colorPost(fetchMock)).toMatchObject({ id: "clr-a", active: false, project_id: "proj-a" });
  });

  it("改本综合体的一条：表单带出原值、按钮为「保存修改」，提交带编号与新提前天数", async () => {
    const { props, fetchMock } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await pickProjectA();
    fireEvent.click(screen.getByTestId("contract-color-edit-clr-a"));
    const form = within(screen.getByTestId("contract-color-form"));
    await waitFor(() => expect((form.getByLabelText("分类名") as HTMLInputElement).value).toBe("A临期"));
    expect((form.getByLabelText("提前天数") as HTMLInputElement).value).toBe("15");
    expect(screen.getByTestId("contract-color-add").textContent).toContain("保存修改");
    fireEvent.change(form.getByLabelText("提前天数"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("contract-color-add"));
    await waitFor(() => expect(colorPost(fetchMock)).not.toBeNull());
    expect(colorPost(fetchMock)).toMatchObject({ id: "clr-a", project_id: "proj-a", threshold_days: 10 });
  });

  it("点「改」后又移除同一条：表单回到新增，之后新增的请求体不带已移除的编号", async () => {
    const { props, fetchMock } = makeProps();
    render(<ContractWorkbenchPage {...props} />);
    await pickProjectA();
    fireEvent.click(screen.getByTestId("contract-color-edit-clr-a"));
    await waitFor(() => expect(screen.getByTestId("contract-color-add").textContent).toContain("保存修改"));
    fireEvent.click(screen.getByTestId("contract-color-remove-clr-a"));
    await waitFor(() => expect(screen.getByTestId("contract-color-add").textContent).toContain("新增着色分类"));
    const form = within(screen.getByTestId("contract-color-form"));
    fireEvent.change(form.getByLabelText("分类名"), { target: { value: "临期红" } });
    fireEvent.change(form.getByLabelText("提前天数"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("contract-color-add"));
    await waitFor(() => expect(colorPosts(fetchMock)).toHaveLength(2));
    expect(colorPosts(fetchMock)[1]).toMatchObject({ project_id: "proj-a", category_name: "临期红", threshold_days: 10 });
    expect(colorPosts(fetchMock)[1]?.id).toBeUndefined();
  });
});
