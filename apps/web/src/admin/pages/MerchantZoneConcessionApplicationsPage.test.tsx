// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { MerchantZoneConcessionApplicationsPage } from "./MerchantZoneConcessionApplicationsPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BV 段 264-R3（第十八批裁决 78「商户用自己的账号提交让利申请（哪张票、哪项权益）并在线确认」）。
// 真实 AdminApi：两个列表接口的 data 是数组，客户端原样返回数组并把蛇形键转成驼峰（同生产）；假响应用文本体。
const TICKETS = '[{"id":"tt-1","ticket_name":"夏日通票"},{"id":"tt-2","ticket_name":"本店单店票"}]';
const APPLICATIONS = '[{"id":"tca-1","ticket_type_id":"tt-1","ticket_name":"夏日通票","merchant_id":"mch-1","merchant_name":"一号酒吧",'
  + '"entitlement_content":"赠送招牌鸡尾酒一杯","confirmed_by":"m1","confirmed_at":"2026-11-20T02:00:00Z","binding_id":null,"template_name":null,"bound_at":null},'
  + '{"id":"tca-2","ticket_type_id":"tt-2","ticket_name":"本店单店票","merchant_id":"mch-1","merchant_name":"一号酒吧",'
  + '"entitlement_content":"赠送小食一份","confirmed_by":"m1","confirmed_at":"2026-11-19T02:00:00Z","binding_id":null,"template_name":null,"bound_at":null}]';
const SUBMITTED = '{"id":"tca-3","ticket_type_id":"tt-1","ticket_name":"夏日通票","merchant_id":"mch-1","merchant_name":"一号酒吧",'
  + '"entitlement_content":"赠送招牌鸡尾酒一杯","confirmed_by":"m1","confirmed_at":"2026-11-21T02:00:00Z","binding_id":null,"template_name":null,"bound_at":null}';

type Sent = { method: string; url: string; body: Record<string, unknown> };

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function stubBackend(sent: Sent[], postResponse: () => Response = () => okBody(SUBMITTED)): void {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    sent.push({ method, url, body: method === "GET" ? {} : JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
    if (method !== "GET") return postResponse();
    return okBody(url.endsWith("/api/v1/merchant-portal/concession-ticket-types") ? TICKETS : APPLICATIONS);
  }));
}

const CAN_APPLY: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["merchant.portal.concession.apply"], denies: [], wildcard: false },
};

function renderPage(): AdminPageProps {
  const props = {
    api: new AdminApi(),
    user: { id: "m1", username: "merchant", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: CAN_APPLY,
  } as unknown as AdminPageProps;
  render(<MerchantZoneConcessionApplicationsPage {...props} />);
  return props;
}

async function fillForm(): Promise<void> {
  await waitFor(() => expect(screen.queryByTestId("merchant-concession-ticket")?.querySelectorAll("option").length ?? 0).toBe(3));
  fireEvent.change(screen.getByTestId("merchant-concession-ticket"), { target: { value: "tt-1" } });
  fireEvent.change(screen.getByTestId("merchant-concession-content"), { target: { value: "赠送招牌鸡尾酒一杯" } });
}

describe("MerchantZoneConcessionApplicationsPage 让利申请", () => {
  afterEach(() => { vi.clearAllMocks(); vi.unstubAllGlobals(); });

  it("列出可申请的票与本商户已确认的申请", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("merchant-concession-ticket")?.querySelectorAll("option").length ?? 0).toBe(3));
    await waitFor(() => expect(screen.queryByTestId("merchant-concession-table")?.textContent ?? "").toContain("赠送招牌鸡尾酒一杯"));
    const table = screen.getByTestId("merchant-concession-table").textContent ?? "";
    expect(table).toContain("夏日通票");
    expect(table).toContain("本店单店票");
    expect(table).toContain("赠送小食一份");
    expect(table).not.toContain("还没有让利申请");
  });

  it("没勾选确认时不能提交", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    renderPage();
    await fillForm();
    expect((screen.getByTestId("merchant-concession-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-concession-submit"));
    expect(sent.filter((c) => c.method !== "GET")).toEqual([]);
  });

  it("勾选确认后提交，请求体是蛇形的四个键，随后重读列表", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    const props = renderPage();
    await fillForm();
    fireEvent.click(screen.getByTestId("merchant-concession-confirm"));
    await waitFor(() => expect((screen.getByTestId("merchant-concession-submit") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("merchant-concession-submit"));
    await waitFor(() => expect(sent.filter((c) => c.method === "POST").length).toBe(1));
    const post = sent.find((c) => c.method === "POST");
    expect(post?.url).toMatch(/\/api\/v1\/merchant-portal\/concession-applications$/);
    expect(Object.keys(post?.body ?? {}).sort()).toEqual(["confirmed", "entitlement_content", "idempotency_key", "ticket_type_id"]);
    expect(post?.body).toMatchObject({ ticket_type_id: "tt-1", entitlement_content: "赠送招牌鸡尾酒一杯", confirmed: true });
    await waitFor(() => expect(sent.filter((c) => c.method === "GET" && c.url.endsWith("/api/v1/merchant-portal/concession-applications")).length).toBe(2));
    expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "merchant_concession_application_submitted", ticketName: "夏日通票" }));
  });

  it("后端拒绝时显示中文报错", async () => {
    const sent: Sent[] = [];
    stubBackend(sent, () => new Response(
      '{"success":false,"code":"PARAM_INVALID","detail_code":"VALIDATION_FAILED","message":"让利权益内容不能超过 200 字"}',
      { status: 400, headers: { "content-type": "application/json" } },
    ));
    const props = renderPage();
    await fillForm();
    fireEvent.click(screen.getByTestId("merchant-concession-confirm"));
    await waitFor(() => expect((screen.getByTestId("merchant-concession-submit") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("merchant-concession-submit"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith("让利权益内容不能超过 200 字"));
  });
});
