// 文件版本：V0.01
import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarVenueSelect } from "./BarVenueSelect";
import { AdminApi } from "../client/adminClient";

// 第十八批 BL 段控制席并入第 415 条（BSM:1017「各处填写「所属酒吧」时从已引进的酒吧商户里下拉选择，不再手工输入。」）：
// 挂商户的账号（带商户号）不调要平台范围的商户列表，下拉只列本店一项、默认选中、不可改；平台管理员照旧可选全部酒吧。走真实 AdminApi（文本体响应）。
const MERCHANTS = [
  { id: "bar-a", merchant_name: "甲酒吧", merchant_type: "bar" },
  { id: "bar-b", merchant_name: "乙酒吧", merchant_type: "bar" },
  { id: "shop-1", merchant_name: "便利店", merchant_type: "retail" },
];

function stubFetch(): string[] {
  const urls: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    urls.push(String(input));
    return Promise.resolve(new Response(
      JSON.stringify({ success: true, code: "OK", message: "success", data: { items: MERCHANTS, page: 1, page_size: 500, total: MERCHANTS.length } }),
      { status: 200, headers: { "content-type": "application/json" } },
    ));
  }));
  return urls;
}

function Harness({ merchantId }: { merchantId?: string | null }) {
  const [value, setValue] = React.useState("");
  return <BarVenueSelect api={new AdminApi("test-token")} value={value} onChange={setValue} testId="venue" merchantId={merchantId} />;
}

const options = () => Array.from((screen.getByTestId("venue") as HTMLSelectElement).options).map((o) => o.textContent);

describe("BarVenueSelect 挂商户账号锁定本店（第 415 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("挂商户的账号：只有本店一项并默认选中、不可改，不发平台商户请求", async () => {
    const urls = stubFetch();
    render(<Harness merchantId="bar-own" />);
    await waitFor(() => expect((screen.getByTestId("venue") as HTMLSelectElement).value).toBe("bar-own"));
    expect(options()).toEqual(["本店"]);
    expect((screen.getByTestId("venue") as HTMLSelectElement).disabled).toBe(true);
    expect(urls.some((url) => url.includes("/api/v1/platform/merchants"))).toBe(false);
  });

  it("平台管理员：照旧列全部酒吧商户，可选", async () => {
    const urls = stubFetch();
    render(<Harness />);
    await waitFor(() => expect(options()).toContain("乙酒吧"));
    expect(options()).toContain("甲酒吧");
    expect(options()).not.toContain("便利店");
    expect(urls.some((url) => url.includes("/api/v1/platform/merchants"))).toBe(true);
    const select = screen.getByTestId("venue") as HTMLSelectElement;
    expect(select.disabled).toBe(false);
    fireEvent.change(select, { target: { value: "bar-b" } });
    await waitFor(() => expect(select.value).toBe("bar-b"));
  });
});
