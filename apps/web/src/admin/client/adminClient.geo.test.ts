// 文件版本：V0.01
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "./adminClient";

// 第十八批 N 段第 123 条 123-R3（BSM:1876「Web 端同样适用：核销、收款、退款、放行等敏感操作在授权位置范围之外一律拒绝并留痕。」）：
// 现场动作闭集（与后端定位闸同一份 12 条）带上当下位置，不论终端；预览、读取与其他写请求一律不带。

function stubFetch() {
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
    new Response(JSON.stringify({ code: "OK", data: { ok: true } }), { status: 200, headers: { "content-type": "application/json" } }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function geoHeaders(fetchMock: ReturnType<typeof stubFetch>, index: number): Array<string | null> {
  const headers = fetchMock.mock.calls[index]![1]!.headers as Headers;
  return [headers.get("x-geo-latitude"), headers.get("x-geo-longitude")];
}

describe("AdminApi 敏感操作带位置（第 123 条 123-R3）", () => {
  beforeEach(() => {
    vi.spyOn(navigator.geolocation, "getCurrentPosition").mockImplementation((success) =>
      success({ coords: { latitude: 31.1, longitude: 121.5 } } as GeolocationPosition));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("核销、订台入场登记带上 x-geo-latitude 与 x-geo-longitude", async () => {
    const fetchMock = stubFetch();
    const api = new AdminApi();

    await api.post("/api/v1/vouchers/redemptions", { voucherInstanceId: "vi-1" });
    await api.post("/api/v1/bar/reservations/r-1/check-in-guest", { guestCount: 2, barId: "bar-1" });

    expect(geoHeaders(fetchMock, 0)).toEqual(["31.1", "121.5"]);
    expect(geoHeaders(fetchMock, 1)).toEqual(["31.1", "121.5"]);
  });

  it("核销预览、读取与闭集外的写请求都不带位置", async () => {
    const fetchMock = stubFetch();
    const api = new AdminApi();

    await api.post("/api/v1/vouchers/redemptions/preview", { code: "VC-1" });
    await api.get("/api/v1/vouchers/redemptions");
    await api.post("/api/v1/ticketing/orders", { ticketTypeId: "ticket-1" });

    for (const index of [0, 1, 2]) expect(geoHeaders(fetchMock, index)).toEqual([null, null]);
  });

  it("商户门户（merchant_portal）调闭集接口同样带位置", async () => {
    const fetchMock = stubFetch();
    const api = new AdminApi();
    api.terminalType = "merchant_portal";

    await api.post("/api/v1/parking/manual-releases", { projectId: "p-1", plateNumber: "沪A12345" });

    expect(geoHeaders(fetchMock, 0)).toEqual(["31.1", "121.5"]);
  });

  it("取不到位置照常发出、不带位置头，由后端拒绝", async () => {
    vi.spyOn(navigator.geolocation, "getCurrentPosition").mockImplementation((_success, failure) =>
      failure?.({ code: 2, message: "unavailable" } as GeolocationPositionError));
    const fetchMock = stubFetch();

    await new AdminApi().post("/api/v1/bar/bills/b-1/settle", { paymentMethod: "cash" });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(geoHeaders(fetchMock, 0)).toEqual([null, null]);
  });
});
