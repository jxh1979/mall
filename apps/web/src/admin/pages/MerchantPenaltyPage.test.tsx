// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { MerchantPenaltyPage } from "./MerchantPenaltyPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 C 段第 62 条：商户处罚记录页（BSM 第 1838 行；第十八批裁决 8「只配单次扣分上下限（按项目可配），运营登记处罚时自己填扣分」）。
const CONFIGURED = {
  projectId: "p1", minPenaltyPoints: 1, maxPenaltyPoints: 10,
  records: [{ id: "mpen_1", merchantId: "m1", penaltyPoints: 3, reason: "占道经营", recordedBy: "ops1", recordedAt: "2026-10-27T10:00:00Z" }],
};

function makeApi(list: unknown = CONFIGURED) {
  const get = vi.fn().mockResolvedValue(list);
  const post = vi.fn().mockResolvedValue({});
  const put = vi.fn().mockResolvedValue({});
  const api: AdminApiClient = {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put,
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
  return { api, get, post, put };
}

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const RECORD_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["merchant.penalty.record"], denies: [], wildcard: false } };

async function renderAndQuery(api: AdminApiClient, permissions = ALL) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "ops", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: permissions,
  };
  render(<MerchantPenaltyPage {...props} />);
  await waitFor(() => expect(screen.getByTestId("merchant-penalty-page").textContent).toContain("商户处罚记录"));
  await userEvent.type(screen.getByLabelText("处罚记录项目 id"), "p1");
  await userEvent.click(screen.getByTestId("merchant-penalty-query"));
}

describe("MerchantPenaltyPage 商户处罚记录", () => {
  it("查询后显示本项目单次扣分上下限与处罚记录", async () => {
    const { api, get } = makeApi();
    await renderAndQuery(api);
    await waitFor(() => expect(screen.getByTestId("merchant-penalty-bounds-current").textContent).toContain("下限 1、上限 10"));
    expect(get).toHaveBeenCalledWith("/api/v1/merchant-penalties?project_id=p1");
    expect(screen.getByTestId("merchant-penalty-mpen_1").textContent).toContain("占道经营");
  });

  it("未配置上下限时提示不能登记处罚", async () => {
    const { api } = makeApi({ projectId: "p1", minPenaltyPoints: null, maxPenaltyPoints: null, records: [] });
    await renderAndQuery(api);
    await waitFor(() => expect(screen.getByTestId("merchant-penalty-bounds-current").textContent)
      .toContain("本项目未配置单次扣分上下限，不能登记处罚"));
  });

  it("登记处罚提交项目、商户、扣分与违规说明", async () => {
    const { api, post } = makeApi();
    await renderAndQuery(api);
    await userEvent.type(screen.getByLabelText("处罚商户 id"), "m1");
    await userEvent.type(screen.getByLabelText("扣分"), "3");
    await userEvent.type(screen.getByLabelText("违规说明"), "占道经营");
    await userEvent.click(screen.getByTestId("merchant-penalty-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/merchant-penalties",
      { projectId: "p1", merchantId: "m1", penaltyPoints: 3, reason: "占道经营" }));
  });

  it("保存单次扣分上下限走按项目的配置端点", async () => {
    const { api, put } = makeApi({ projectId: "p1", minPenaltyPoints: null, maxPenaltyPoints: null, records: [] });
    await renderAndQuery(api);
    await userEvent.type(screen.getByLabelText("单次扣分下限"), "1");
    await userEvent.type(screen.getByLabelText("单次扣分上限"), "10");
    await userEvent.click(screen.getByTestId("merchant-penalty-bounds-save"));
    await waitFor(() => expect(put).toHaveBeenCalledWith("/api/v1/merchant-penalty-bounds/p1",
      { minPenaltyPoints: 1, maxPenaltyPoints: 10 }));
  });

  it("没有配置上下限权限时保存按钮不可用", async () => {
    const { api } = makeApi();
    await renderAndQuery(api, RECORD_ONLY);
    await waitFor(() => expect(screen.getByTestId("merchant-penalty-bounds-save")).toBeDisabled());
    expect(screen.getByTestId("merchant-penalty-submit")).not.toBeDisabled();
  });
});
