// 文件版本：V0.01
import { describe, expect, it, vi } from "vitest";

import { CustomerApi, type CustomerTransport } from "./client";

// OpenAPI：客人投诉（/customer/complaints）与学生认证（/customer/student-certifications）为 multipart/form-data，幂等键必填
// （「操作幂等键（与 Idempotency-Key 头同值）；重复提交返回第一次的结果」）。
describe("CustomerApi.postForm", () => {
  it("formSubmitCarriesSameKeyInHeaderAndField：表单字段蛇形、文件原样交给本端组表单；结果未明再提交沿用原请求编号", async () => {
    const transport = vi.fn<CustomerTransport>()
      .mockRejectedValueOnce(new Error("网络中断"))
      .mockResolvedValueOnce({ status: 200, text: JSON.stringify({ code: "OK", message: "ok", data: { work_order_id: "wo-1" } }) });
    const api = new CustomerApi({ terminalType: "app", clientVersion: "1.0.0", transport, getToken: () => "tok" });
    const photo = { name: "a.jpg" };
    const submit = () => api.postForm<{ workOrderId: string }>("/api/v1/customer/complaints",
      { projectId: "prj-1", title: "灯坏了" }, [{ field: "files", file: photo, fileName: "a.jpg" }]);

    await expect(submit()).rejects.toThrow("网络中断");
    await expect(submit()).resolves.toEqual({ workOrderId: "wo-1" });

    const [first, second] = transport.mock.calls.map(([request]) => request);
    const key = first!.headers["idempotency-key"];
    expect(key).toBeTruthy();
    expect(first!.method).toBe("POST");
    expect(first!.body).toBeUndefined();
    expect(first!.headers["content-type"]).toBeUndefined();
    expect(first!.headers["x-customer-token"]).toBe("tok");
    expect(first!.form).toEqual({ fields: { project_id: "prj-1", title: "灯坏了", idempotency_key: key },
      files: [{ field: "files", file: photo, fileName: "a.jpg" }] });
    expect(second!.headers["idempotency-key"]).toBe(key);
  });
});
