// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ArchivedContentPage } from "./ArchivedContentPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

// 第十八批 BY 段第 383 条 383-R6（BSM:569「进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。」）：
//   商户服务工单含已存档列表按 page、size 分页，本页逐页取完，第 100 条以后的已存档工单也列得出。假响应用文本体。

function okText(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`, { status: 200, headers: { "content-type": "application/json" } });
}

const PAGE1 = Array.from({ length: 100 }, (_, i) => `{"id":"wo-${i}","title":"工单${i}","status":"closed","archived_at":null}`);
const OLD = '{"id":"wo-old","title":"早年空调报修","status":"closed","archived_at":"2026-10-01T03:00:00Z","archived_by":"usr-ops-1"}';

describe("已存档内容页（第 383 条）商户服务工单逐页取完", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("第 1 页 100 条都未存档、第 2 页 1 条已存档：表里出现这一条，第二次请求带 page=2&size=100", async () => {
    const gets: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      gets.push(url);
      if (url.includes("page=2&size=100")) return Promise.resolve(okText(`{"items":[${OLD}],"page":2,"page_size":100,"total":1}`));
      return Promise.resolve(okText(`{"items":[${PAGE1.join(",")}],"page":1,"page_size":100,"total":100}`));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "运营", accountType: "staff" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: ["merchant.service_work_order.view"], denies: [], wildcard: false } },
    };
    render(<ArchivedContentPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("archived-content-row-wo-old")).not.toBeNull(), { timeout: 3000 });
    const workOrderGets = gets.filter((url) => url.includes("/api/v1/merchant-service/work-orders"));
    expect(workOrderGets).toHaveLength(2);
    expect(workOrderGets[1]).toContain("/api/v1/merchant-service/work-orders?include_archived=true&page=2&size=100");
    expect(screen.queryByTestId("archived-content-row-wo-0")).toBeNull();
  });
});
