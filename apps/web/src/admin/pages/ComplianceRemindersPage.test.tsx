// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ComplianceRemindersPage } from "./ComplianceRemindersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TYPES = ["business_license", "food_license", "alcohol_license", "fire_certificate", "special_equipment", "insurance",
  "supplier_qualification", "inspection_certificate", "custom"];
const NAMES = ["营业执照", "食品经营许可证", "酒类经营许可证", "消防安全证照", "特种设备使用证", "保险", "供应商资质", "检验检测证书", "其他证照"];
const CONFIGS = TYPES.map((itemType) => ({
  itemType,
  graceDays: itemType === "business_license" ? 15 : null,
  graceMinDays: itemType === "business_license" ? 7 : null,
  graceMaxDays: itemType === "business_license" ? 30 : null,
  warningMonths: 3,
  warningMinMonths: null,
  warningMaxMonths: null,
}));
const ITEM = { id: "cei_1", itemType: "food_license", itemName: "食品经营许可证", documentNo: "FD-1", validTo: "2026-09-30", status: "active", expiryAction: "block_operation" };

function makeApi(put = vi.fn().mockResolvedValue({}), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn((path: string) => Promise.resolve(
      path.endsWith("/item-type-configs") ? CONFIGS : path.endsWith("/expiry-items") ? { items: [ITEM] } : { items: [] },
    )),
    getPaged: vi.fn(),
    post,
    put,
    patch: vi.fn(),
    delete: vi.fn(),
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
  render(<ComplianceRemindersPage {...props} />);
}

// 第十八批 E 段第 22 条（PGM:531）：按证照类型配宽限期与预警提前量（值与上下限同配）、证照类型中文对齐后端九类、登记续期。
describe("ComplianceRemindersPage 按证照类型配置与续期登记", () => {
  it("配置块按后端九类显示中文名，编辑提交六个蛇形键；保险不显示宽限", async () => {
    const put = vi.fn().mockResolvedValue({});
    renderPage(makeApi(put));
    await waitFor(() => expect(screen.queryByTestId("compliance-type-config-row-business_license")?.textContent ?? "").toContain("营业执照"));
    TYPES.forEach((type, index) => expect(screen.getByTestId(`compliance-type-config-row-${type}`).textContent).toContain(NAMES[index]));
    expect(screen.getByTestId("compliance-type-config-row-insurance").textContent).toContain("不适用");

    fireEvent.click(screen.getByTestId("compliance-type-config-edit-insurance"));
    expect(screen.queryByTestId("compliance-type-config-grace-days")).toBeNull();
    fireEvent.click(screen.getByTestId("compliance-type-config-modal-cancel"));

    fireEvent.click(screen.getByTestId("compliance-type-config-edit-business_license"));
    fireEvent.change(screen.getByTestId("compliance-type-config-grace-days"), { target: { value: "20" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-months"), { target: { value: "2" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-min-months"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-max-months"), { target: { value: "6" } });
    fireEvent.click(screen.getByTestId("compliance-type-config-submit"));
    await waitFor(() => expect(put).toHaveBeenCalled());
    const [path, body] = put.mock.calls[0] ?? [];
    expect(path).toBe("/api/v1/compliance/item-type-configs/business_license");
    expect(body).toMatchObject({
      grace_days: 20, grace_min_days: 7, grace_max_days: 30, warning_months: 2, warning_min_months: 1, warning_max_months: 6,
    });
    expect(Object.keys(body).sort()).toEqual(["grace_days", "grace_max_days", "grace_min_days", "idempotency_key",
      "warning_max_months", "warning_min_months", "warning_months"]);
  });

  it("证照行内登记续期，请求体蛇形键带新有效期止", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(undefined, post));
    await waitFor(() => expect(screen.queryByTestId("compliance-expiry-renew-cei_1")).not.toBeNull());
    expect(screen.getByTestId("compliance-expiry-row-cei_1").textContent).toContain("食品经营许可证");
    fireEvent.click(screen.getByTestId("compliance-expiry-renew-cei_1"));
    fireEvent.change(screen.getByTestId("compliance-renew-valid-to"), { target: { value: "2027-09-30" } });
    fireEvent.click(screen.getByTestId("compliance-renew-submit"));
    await waitFor(() => expect(post.mock.calls.some((call) => call[0] === "/api/v1/compliance/expiry-items/cei_1/renewal")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/compliance/expiry-items/cei_1/renewal");
    expect(call?.[1]).toMatchObject({ valid_to: "2027-09-30" });
    expect(Object.keys(call?.[1] ?? {})).toContain("idempotency_key");
  });
});

// 第十八批 AF 段 198-R5：扫描入口不读 operator_note，契约删字段；「操作备注」框只供扫描使用，删框并停发。
// 第十八批 AN 段 258-R1：基准日留空由后端按当天、操作人取登录人，请求体不带 operator_user_id。
describe("ComplianceRemindersPage 扫描生成提醒停发操作备注", () => {
  it("扫描表单不再有「操作备注」框，请求体不含 operator_note", async () => {
    const post = vi.fn().mockResolvedValue({ generatedCount: 0 });
    renderPage(makeApi(undefined, post));
    await waitFor(() => expect(screen.queryByTestId("compliance-scan")).not.toBeNull());
    expect(screen.queryByTestId("compliance-scan-note")).toBeNull();
    expect(screen.getByTestId("compliance-scan-form").textContent).not.toContain("操作备注");

    fireEvent.click(screen.getByTestId("compliance-scan"));

    await waitFor(() => expect(post.mock.calls.some((call) => call[0] === "/api/v1/compliance/reminders/scan")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/compliance/reminders/scan");
    expect(call?.[1]).not.toHaveProperty("operator_note");
    expect(call?.[1]).not.toHaveProperty("operator_user_id");
    expect(call?.[1]).toMatchObject({ default_lookahead_days: 30 });
  });
});

// 第十八批 AN 段第 258 条 258-R2、258-R3（PGM:531「管理证照、资质、保险和合同关键日期的到期预警与到期阻断……」；PGM:287「……枚举取值……
// 必须全部为中文……」）：真实页面经真实客户端，只桩网络层，响应按后端蛇形键。日期类型按后端六类闭集；合同关键日期商户由合同带出、
// 证照主体即所选商户，责任人都取登录人。
describe("ComplianceRemindersPage 补录合同关键日期与证照按后端闭集与必填项", () => {
  afterEach(() => vi.unstubAllGlobals());

  const MERCHANTS = [{ id: "mer_a", merchant_name: "星光酒吧" }, { id: "mer_b", merchant_name: "云端餐厅" }];
  const KEY_DATE = { id: "ckd_1", contract_id: "ct_1", merchant_id: "mer_a", date_type: "contract_end", title: "合同期满",
    due_date: "2027-01-31", status: "active", source_type: "manual", human_confirmed: true };

  function renderReal(posts: Array<{ url: string; body: Record<string, unknown> }>) {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      let data: unknown = { items: [], page: 1, page_size: 50, total: 0 };
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, body: JSON.parse(String(init?.body)) as Record<string, unknown> });
        data = { id: "new_1" };
      } else if (url.includes("/api/v1/platform/merchants")) {
        data = { items: MERCHANTS, page: 1, page_size: 500, total: 2 };
      } else if (url.includes("/api/v1/compliance/contract-key-dates")) {
        data = { items: [KEY_DATE], page: 1, page_size: 50, total: 1 };
      }
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
    const setGlobalError = vi.fn();
    render(<ComplianceRemindersPage api={new AdminApi()} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={setGlobalError} effectivePermissions={ADMIN_PERMS} />);
    return setGlobalError;
  }

  it("合同关键日期：列表与下拉按后端六类显示中文；不填到期日拦截；提交带登录人为责任人、不带商户", async () => {
    const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
    renderReal(posts);
    await waitFor(() => expect(screen.queryByTestId("compliance-key-date-row-ckd_1")?.textContent ?? "").toContain("合同到期"));
    fireEvent.click(screen.getByTestId("compliance-key-date-create-open"));
    const options = Array.from(screen.getByTestId("compliance-key-date-type").querySelectorAll("option")).map((o) => o.textContent);
    expect(options).toEqual(["合同开始", "合同到期", "续约通知", "押金退还", "免租期结束", "其他"]);

    fireEvent.change(screen.getByTestId("compliance-key-date-contract"), { target: { value: "ct_1" } });
    fireEvent.change(screen.getByTestId("compliance-key-date-title"), { target: { value: "续约提醒" } });
    fireEvent.click(screen.getByTestId("compliance-key-date-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("compliance-key-date-guided-error")?.textContent ?? "").toContain("关联合同、标题、到期日必填"));
    expect(screen.queryByTestId("compliance-key-date-create")).toBeNull();

    fireEvent.change(screen.getByTestId("compliance-key-date-due"), { target: { value: "2027-01-31" } });
    fireEvent.click(screen.getByTestId("compliance-key-date-guided-next"));
    fireEvent.click(screen.getByTestId("compliance-key-date-create"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/compliance/contract-key-dates");
    expect(posts[0]!.body).toMatchObject({ contract_id: "ct_1", title: "续约提醒", date_type: "renewal_notice", due_date: "2027-01-31",
      owner_user_id: "u1", source_type: "manual", human_confirmed: true });
    expect(posts[0]!.body).not.toHaveProperty("merchant_id");
  });

  it("证照：下拉列出商户；未选商户不发请求并提示；选商户后主体与商户都是所选商户、责任人为登录人", async () => {
    const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
    const setGlobalError = renderReal(posts);
    await waitFor(() => {
      const merchantSelect = screen.queryByTestId("compliance-expiry-merchant")?.textContent ?? "";
      expect(merchantSelect).toContain("星光酒吧");
      expect(merchantSelect).toContain("云端餐厅");
    });

    fireEvent.change(screen.getByTestId("compliance-expiry-name"), { target: { value: "营业执照" } });
    fireEvent.change(screen.getByTestId("compliance-expiry-valid-to"), { target: { value: "2027-06-30" } });
    fireEvent.click(screen.getByTestId("compliance-expiry-create"));
    expect(setGlobalError).toHaveBeenCalledWith("商户、证照名称、有效期止必填");
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("compliance-expiry-merchant"), { target: { value: "mer_b" } });
    fireEvent.click(screen.getByTestId("compliance-expiry-create"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/compliance/expiry-items");
    expect(posts[0]!.body).toMatchObject({ subject_type: "merchant", subject_id: "mer_b", merchant_id: "mer_b", item_type: "business_license",
      item_name: "营业执照", valid_to: "2027-06-30", owner_user_id: "u1", source_type: "manual", human_confirmed: true });
  });
});
