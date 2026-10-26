// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationAdaptersPage } from "./IntegrationAdaptersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 后端 IntegrationValidation.ADAPTER_TYPES / MODES 闭集（= OpenAPI ProviderAdapterRequest.adapter_type / mode 枚举）。
const BACKEND_ADAPTER_TYPES = ["payment", "ticket_channel", "face_age", "wecom", "robot", "video", "device", "retail", "catering", "parking"];
const BACKEND_MODES = ["mock", "sandbox", "production"];
// OpenAPI ProviderAdapterRequest 声明的全部字段（additionalProperties: false）。
const ACCEPTED_KEYS = ["idempotency_key", "operator_note", "evidence_attachment_ids", "provider_code", "adapter_type", "mode", "field_mapping", "webhook_config", "polling_config", "production_ready"];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function fail(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

// 真实 AdminApi + 桩 fetch：断言真正发到后端的请求体（蛇形键 + 幂等键），后端响应也按蛇形键返回。
let postBodies: Array<Record<string, unknown>> = [];
function makeApi(onCreate: (body: Record<string, unknown>) => Response): AdminApi {
  postBodies = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/api/v1/integration/provider-adapters")) {
        const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
        postBodies.push(body);
        return Promise.resolve(onCreate(body));
      }
      return Promise.resolve(ok({ items: [{ id: "adapter-1", provider_code: "prov-a", adapter_type: "ticket_channel", mode: "mock", status: "pending", production_ready: false }], page: 1, page_size: 50, total: 1 }));
    }),
  );
  return new AdminApi("test-token");
}

function renderPage(api: AdminApi) {
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<IntegrationAdaptersPage {...props} />);
  return { setActionResult, setGlobalError };
}

async function openCreate() {
  await waitFor(() => expect(screen.queryByTestId("integration-adapter-row-adapter-1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("integration-create-open"));
}

const created = (body: Record<string, unknown>) => ok({ id: "adapter-new", provider_code: body.provider_code, adapter_type: body.adapter_type, mode: body.mode, status: "pending", production_ready: false });

// OBS-14-01：新增供应商适配器按后端真实契约提交（类型从闭集选、带中文标签，mode 必填，只发后端接受的字段）；
// 中文展示依据 PGM L287「全局原则 · 界面全中文（所有终端零英文）」。
describe("IntegrationAdaptersPage 引导式新增供应商适配器（后端真实契约）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("类型与接入模式只能从后端闭集选，选项全中文", async () => {
    renderPage(makeApi(created));
    await openCreate();
    await waitFor(() => expect(screen.queryByTestId("integration-adapter-type")).not.toBeNull());
    const typeOptions = Array.from((screen.getByTestId("integration-adapter-type") as HTMLSelectElement).options).filter((option) => option.value);
    expect(typeOptions.map((option) => option.value).sort()).toEqual([...BACKEND_ADAPTER_TYPES].sort());
    const modeOptions = Array.from((screen.getByTestId("integration-mode") as HTMLSelectElement).options).filter((option) => option.value);
    expect(modeOptions.map((option) => option.value).sort()).toEqual([...BACKEND_MODES].sort());
    for (const option of [...typeOptions, ...modeOptions]) expect(option.textContent).not.toMatch(/[A-Za-z]/);
    expect(screen.getByTestId("integration").textContent ?? "").not.toMatch(/[A-Za-z]/);
  });

  it("按真实请求形状提交：只发后端接受的字段，成功后显示中文结果", async () => {
    const { setActionResult } = renderPage(makeApi(created));
    await openCreate();
    await waitFor(() => expect(screen.queryByTestId("integration-provider-code")).not.toBeNull());
    fireEvent.change(screen.getByTestId("integration-provider-code"), { target: { value: "prov-park" } });
    fireEvent.change(screen.getByTestId("integration-adapter-type"), { target: { value: "parking" } });
    fireEvent.change(screen.getByTestId("integration-mode"), { target: { value: "sandbox" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    const summary = screen.getByTestId("integration-summary").textContent ?? "";
    expect(summary).toContain("停车系统");
    expect(summary).not.toMatch(/parking|sandbox/);
    fireEvent.click(screen.getByTestId("integration-save"));
    await waitFor(() => expect(postBodies.length).toBe(1));
    const body = postBodies[0]!;
    expect(body).toMatchObject({ provider_code: "prov-park", adapter_type: "parking", mode: "sandbox", idempotency_key: expect.stringMatching(/^admin-web-/) });
    expect(Object.keys(body).filter((key) => !ACCEPTED_KEYS.includes(key))).toEqual([]);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith("已保存供应商适配器：prov-park"));
    await waitFor(() => expect(screen.queryByTestId("integration")).toBeNull());
    expect(screen.getByTestId("integration-adapter-row-adapter-new").textContent).toContain("停车系统");
  });

  it("未选接入模式 → 当步中文提示，不提交", async () => {
    renderPage(makeApi(created));
    await openCreate();
    await waitFor(() => expect(screen.queryByTestId("integration-provider-code")).not.toBeNull());
    fireEvent.change(screen.getByTestId("integration-provider-code"), { target: { value: "prov-pay" } });
    fireEvent.change(screen.getByTestId("integration-adapter-type"), { target: { value: "payment" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    await waitFor(() => expect(screen.getByTestId("integration-error").textContent).toBe("请选择接入模式"));
    expect(screen.queryByTestId("integration-save")).toBeNull();
    expect(postBodies).toEqual([]);
  });

  it("后端校验失败 → 弹窗内显示中文原因，弹窗不关", async () => {
    const { setActionResult } = renderPage(makeApi(() => fail(400, { success: false, code: "PARAM_INVALID", detail_code: "VALIDATION_FAILED", message: "适配类型格式不正确" })));
    await openCreate();
    await waitFor(() => expect(screen.queryByTestId("integration-provider-code")).not.toBeNull());
    fireEvent.change(screen.getByTestId("integration-provider-code"), { target: { value: "prov-pay" } });
    fireEvent.change(screen.getByTestId("integration-adapter-type"), { target: { value: "payment" } });
    fireEvent.change(screen.getByTestId("integration-mode"), { target: { value: "mock" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    fireEvent.click(screen.getByTestId("integration-save"));
    await waitFor(() => expect(screen.getByTestId("integration-error").textContent).toBe("适配类型格式不正确"));
    expect(screen.queryByTestId("integration")).not.toBeNull();
    expect(setActionResult).not.toHaveBeenCalled();
  });
});
