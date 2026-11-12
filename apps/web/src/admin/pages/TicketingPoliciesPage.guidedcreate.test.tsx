// 文件版本：V0.03
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程(AUD-015 起):新建票种=分步弹窗(基本信息→绑定权益并发布→确认)。
// 服务端建票固定 draft,发布闸要求≥1个权益绑定+理由——向导内一次完成 create→bind→publish 三段。
function makeApi(postSpy: ReturnType<typeof vi.fn>, putSpy?: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => {
      if (path.includes("/entitlement-templates")) {
        return { items: [{ id: "tpl-1", name: "门票权益", entitlementType: "entry", status: "active",
          entitlementKind: "virtual", agreementPriceCent: 8000 }] } as unknown;
      }
      if (path.includes("/qr-refresh-config")) {
        return { refreshIntervalSeconds: 15 } as unknown;
      }
      if (path.includes("/platform/merchants")) {
        return { items: [{ id: "m-bar-1", merchantName: "一号酒吧", merchantType: "bar" }] } as unknown;
      }
      return { items: [] } as unknown;
    }),
    post: postSpy,
    put: putSpy ?? vi.fn(async () => ({ refreshIntervalSeconds: 15 } as unknown)),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>, putSpy?: ReturnType<typeof vi.fn>) {
  const props: AdminPageProps = {
    api: makeApi(postSpy, putSpy),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
  return props;
}

// 第十八批 W 段 177-R1：等商户列表落到通用「新建模板」的提供方商户下拉里（页面被动加载落定）再操作。
async function templateFormReady() {
  await waitFor(() => expect(Array.from((screen.queryByTestId("entitlement-template-merchant") as HTMLSelectElement | null)?.options ?? [])
    .map((o) => o.value)).toContain("m-bar-1"));
  return within(screen.getByTestId("entitlement-template-form"));
}

describe("TicketingPoliciesPage 引导式新建票种", () => {
  afterEach(() => { vi.clearAllMocks(); vi.unstubAllGlobals(); });

  it("基本信息校验→绑权益填理由→确认发布,后端收到 create/bind/publish 三段", async () => {
    const postSpy = vi.fn(async (path: string, _body?: unknown) => {
      if (path.endsWith("/ticketing/ticket-types")) return { id: "tt-new" } as unknown;
      return {} as unknown;
    });
    renderPage(postSpy);

    await waitFor(() => expect(screen.getByTestId("ticket-create-open")).toBeTruthy());
    // 未打开时弹窗不渲染。
    expect(screen.queryByTestId("ticket-name")).toBeNull();

    fireEvent.click(screen.getByTestId("ticket-create-open"));
    // 第一步:名称有默认值。只填平日/周末,缺节假日 → 下一步触发校验,弹窗留开显错。
    expect(screen.getByTestId("ticket-name")).toBeTruthy();
    fireEvent.change(screen.getByTestId("ticket-weekday-price"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("ticket-weekend-price"), { target: { value: "120" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    expect(screen.getByTestId("ticket-policy-error").textContent).toContain("节假日");

    // 补齐节假日 → 进第二步:未选权益模板不能继续。
    fireEvent.change(screen.getByTestId("ticket-holiday-price"), { target: { value: "150" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    await waitFor(() => expect(screen.getByTestId("ticket-create-template")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    expect(screen.getByTestId("ticket-policy-error").textContent).toContain("权益模板");

    // 选模板+填发布理由 → 确认页 → 发布:create→bind→publish 三段依次落后端。
    fireEvent.change(screen.getByTestId("ticket-create-template"), { target: { value: "tpl-1" } });
    fireEvent.change(screen.getByTestId("ticket-publish-reason"), { target: { value: "暑期档新票上架" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    fireEvent.click(screen.getByTestId("ticket-publish"));

    await waitFor(() => {
      const posted = postSpy.mock.calls.map((c) => String(c[0]));
      expect(posted.some((p) => p.endsWith("/ticketing/ticket-types"))).toBe(true);
      expect(posted.some((p) => p.includes("/ticket-types/tt-new/entitlements"))).toBe(true);
      expect(posted.some((p) => p.includes("/ticket-types/tt-new/publish"))).toBe(true);
    });
    // 建票载荷:不再传 status(服务端固定 draft),带有效期/刷新/可退/单店字段。
    const createCall = postSpy.mock.calls.find((c) => String(c[0]).endsWith("/ticketing/ticket-types"));
    const createBody = (createCall?.[1] ?? {}) as Record<string, unknown>;
    expect(createBody.status).toBeUndefined();
    expect(createBody.validity_days).toBe(1);
    // #172：刷新间隔不再随建票按票种下发（改为全场统一配置端点）。
    expect(createBody.refresh_interval_seconds).toBeUndefined();
    expect(createBody.refundable).toBe(false);
    // 发布载荷带必填理由。
    const publishCall = postSpy.mock.calls.find((c) => String(c[0]).includes("/publish"));
    expect(((publishCall?.[1] ?? {}) as Record<string, unknown>).reason).toBe("暑期档新票上架");
    // 发布成功后弹窗关闭。
    await waitFor(() => expect(screen.queryByTestId("ticket-name")).toBeNull());
  });

  // #172(2026-07-19 拍板):全场统一动态码刷新间隔——页级配置,默认15秒,5秒为单位到60秒,改后 PUT 落后端。
  it("全场统一刷新间隔:默认15秒,5秒步进到60,保存走 PUT /qr-refresh-config", async () => {
    const postSpy = vi.fn(async () => ({} as unknown));
    const putSpy = vi.fn(async (_path: string, _body?: unknown) => ({ refreshIntervalSeconds: 20 } as unknown));
    renderPage(postSpy, putSpy);
    await waitFor(() => expect(screen.getByTestId("ticket-qr-refresh-config-select")).toBeTruthy());
    const select = screen.getByTestId("ticket-qr-refresh-config-select") as HTMLSelectElement;
    expect(select.value).toBe("15");
    expect(Array.from(select.options).map((o) => o.value)).toEqual(
      ["5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"]);
    // 建票向导内不再有按票种的刷新下拉。
    fireEvent.click(screen.getByTestId("ticket-create-open"));
    expect(screen.queryByTestId("ticket-refresh-seconds")).toBeNull();
    // 改到 20 并保存 → PUT 全场统一配置端点。
    fireEvent.change(select, { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("ticket-qr-refresh-config-save"));
    await waitFor(() => {
      expect(putSpy).toHaveBeenCalled();
      const putCall = putSpy.mock.calls.find((c) => String(c[0]).includes("/ticketing/qr-refresh-config"));
      expect(putCall).toBeTruthy();
      expect(((putCall?.[1] ?? {}) as { refreshIntervalSeconds?: number }).refreshIntervalSeconds).toBe(20);
    });
  });

  // 配置中心批5三个尾项：后端早就有、运营在后台点不到的三个配置项补上入口。
  it("建票能选分类、建权益模板不设人脸照开关、签约价能改", async () => {
    const postSpy = vi.fn(async (path: string, _body?: unknown) => {
      if (path.endsWith("/ticketing/ticket-types")) return { id: "tt-new" } as unknown;
      return {} as unknown;
    });
    const putSpy = vi.fn(async (_path: string, _body?: unknown) => ({} as unknown));
    renderPage(postSpy, putSpy);
    await waitFor(() => expect(screen.getByTestId("ticket-create-open")).toBeTruthy());

    // ① 建票种选分类：默认标准票，可选新人/学生，随建票载荷发到后端（V230 ticket_category）。
    fireEvent.click(screen.getByTestId("ticket-create-open"));
    const category = screen.getByTestId("ticket-category") as HTMLSelectElement;
    expect(category.value).toBe("standard");
    expect(Array.from(category.options).map((o) => o.value)).toEqual(["standard", "new_person", "student"]);
    fireEvent.change(category, { target: { value: "student" } });
    fireEvent.change(screen.getByTestId("ticket-weekday-price"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("ticket-weekend-price"), { target: { value: "120" } });
    fireEvent.change(screen.getByTestId("ticket-holiday-price"), { target: { value: "150" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    await waitFor(() => expect(screen.getByTestId("ticket-create-template")).toBeTruthy());
    fireEvent.change(screen.getByTestId("ticket-create-template"), { target: { value: "tpl-1" } });
    fireEvent.change(screen.getByTestId("ticket-publish-reason"), { target: { value: "学生票上架" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    fireEvent.click(screen.getByTestId("ticket-publish"));
    await waitFor(() => {
      const createCall = postSpy.mock.calls.find((c) => String(c[0]).endsWith("/ticketing/ticket-types"));
      expect(((createCall?.[1] ?? {}) as Record<string, unknown>).ticket_category).toBe("student");
    });

    // ② TK-06（BSM:843「不设需要人脸照的票种」）：建权益模板没有「需要人脸照」开关，载荷也不带该字段。
    //   177-R1：先选提供方商户，载荷带所选 merchant_id。
    const tplForm = await templateFormReady();
    fireEvent.change(tplForm.getByLabelText("模板名称"), { target: { value: "游乐入场" } });
    fireEvent.change(tplForm.getByLabelText("提供方商户"), { target: { value: "m-bar-1" } });
    expect(screen.queryByTestId("entitlement-template-require-face-photo")).toBeNull();
    fireEvent.click(screen.getByTestId("entitlement-template-create"));
    await waitFor(() => {
      const tplCall = postSpy.mock.calls.find((c) => String(c[0]).endsWith("/ticketing/entitlement-templates"));
      expect(tplCall).toBeTruthy();
      expect((tplCall?.[1] ?? {}) as Record<string, unknown>).not.toHaveProperty("require_face_photo");
      expect(((tplCall?.[1] ?? {}) as Record<string, unknown>).merchant_id).toBe("m-bar-1");
    });

    // ③ 批10/E：模板侧改价入口已下线——页面明说唯一可改价处 = 内部结算价目录（草案→他人复核转正），
    //   且不存在任何 PUT /agreement-price 请求（端点已删，防前端还挂着一个必 404 的按钮骗运营）。
    expect(screen.queryByTestId("agreement-price-form")).toBeNull();
    expect(screen.getByTestId("agreement-price-scope-hint").textContent).toContain("内部结算价目录");
    expect(putSpy.mock.calls.some((c) => String(c[0]).includes("/agreement-price"))).toBe(false);
  });

  // 第十八批 W 段 177-R1（BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：
  //   通用「新建模板」提供方商户必选，未选不发请求、提示中文；权益类型下拉不含赠饮（控制席并入：赠饮在组合配置酒吧行建）。
  it("177-R1 新建模板：未选提供方商户不发请求并提示，权益类型不含赠饮", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({} as unknown));
    const props = renderPage(postSpy);
    const tplForm = await templateFormReady();
    const typeOptions = Array.from((tplForm.getByLabelText("权益类型") as HTMLSelectElement).options).map((o) => o.value);
    expect(typeOptions).toContain("entry");
    expect(typeOptions).not.toContain("gift_drink");
    fireEvent.change(tplForm.getByLabelText("模板名称"), { target: { value: "入园1次" } });
    fireEvent.click(screen.getByTestId("entitlement-template-create"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith("请选择权益的提供方商户"));
    expect(postSpy.mock.calls.some((c) => String(c[0]).endsWith("/ticketing/entitlement-templates"))).toBe(false);
  });

  // 第十八批 AH 段 217-R1 并入第 217 条补充（ENUM:36 权益类型「入场、游乐、酒水（赠饮/兑换券）……」；契约 EntitlementTemplateRequest.entitlement_type
  //   枚举）：通用「新建模板」权益类型只留 ENUM:36 有、契约有、后端收的入园、游乐；ride（游乐项目）即游乐、bar（酒吧）不是权益类型，去掉。
  it("217 补充 新建模板：权益类型下拉只有入园、游乐", async () => {
    renderPage(vi.fn(async () => ({} as unknown)));
    const tplForm = await templateFormReady();
    const typeOptions = Array.from((tplForm.getByLabelText("权益类型") as HTMLSelectElement).options).map((o) => o.value);
    expect(typeOptions).toEqual(["entry", "amusement"]);
  });

  // 177-R1（PGM:287「……提示、报错……必须全部为中文」）：后端拒绝时把中文报错显示出来，不再成未处理的拒绝。
  it("177-R1 新建模板：后端 400 时显示中文报错", async () => {
    const templatePosts: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/platform/merchants")) {
        return new Response('{"success":true,"code":"OK","message":"success","data":{"items":[{"id":"m-bar-1","merchant_name":"一号酒吧","merchant_type":"bar"}]}}',
          { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/ticketing/entitlement-templates") && init?.method === "POST") {
        templatePosts.push(String(init.body));
        return new Response('{"success":false,"code":"PARAM_INVALID","detail_code":"PARAM_INVALID","message":"商户未填写"}',
          { status: 400, headers: { "content-type": "application/json" } });
      }
      return new Response('{"success":true,"code":"OK","message":"success","data":{"items":[]}}',
        { status: 200, headers: { "content-type": "application/json" } });
    }));
    const setGlobalError = vi.fn();
    const setActionResult = vi.fn();
    render(<TicketingPoliciesPage {...({ api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps)} />);
    const tplForm = await templateFormReady();
    fireEvent.change(tplForm.getByLabelText("模板名称"), { target: { value: "入园1次" } });
    fireEvent.change(tplForm.getByLabelText("提供方商户"), { target: { value: "m-bar-1" } });
    fireEvent.click(screen.getByTestId("entitlement-template-create"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("商户未填写"));
    expect(templatePosts.length).toBe(1);
    expect(JSON.parse(templatePosts[0] ?? "{}").merchant_id).toBe("m-bar-1");
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "entitlement_template_created" }));
  });
});
