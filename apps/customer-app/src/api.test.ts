// 文件版本：V0.01
import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchTransport } from "./api";

// 投诉附件、学生证件照片走 multipart：App 端用 FormData 组表单（不手写 content-type，由运行环境带上分隔符），不带 cookie。
describe("fetchTransport", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("formRequestBuildsFormData：表单字段与文件组成 FormData 发出", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const photo = new File(["p"], "a.jpg", { type: "image/jpeg" });
    await fetchTransport({ method: "POST", url: "/api/v1/customer/complaints", headers: { "idempotency-key": "k-1" },
      form: { fields: { title: "灯坏了", idempotency_key: "k-1" }, files: [{ field: "files", file: photo, fileName: "a.jpg" }] } });
    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect(init.credentials).toBe("omit");
    expect(init.body).toBeInstanceOf(FormData);
    const form = init.body as FormData;
    expect(form.get("title")).toBe("灯坏了");
    expect(form.get("idempotency_key")).toBe("k-1");
    expect((form.get("files") as File).name).toBe("a.jpg");
    expect(new Headers(init.headers).get("content-type")).toBeNull();
  });
});
