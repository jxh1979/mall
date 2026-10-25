// 文件版本：V0.01
import { describe, expect, it } from "vitest";

import { buildCustomerLink, parseCustomerLink } from "./links";

const HOST = "c.complex.example";

describe("parseCustomerLink（只认配置主机、https、/c/ 前缀与白名单路由）", () => {
  it("parsesSixAllowedRoutes", () => {
    expect(parseCustomerLink(`https://${HOST}/c/parking?project_id=proj-1&code=${encodeURIComponent("粤A12345")}`, HOST))
      .toEqual({ route: "parking", params: { project_id: "proj-1", code: "粤A12345" } });
    expect(parseCustomerLink(`https://${HOST}/c/bar?bar_id=bar-9`, HOST)).toEqual({ route: "bar", params: { bar_id: "bar-9" } });
    expect(parseCustomerLink(`https://${HOST}/c/table?table_id=tbl_1`, HOST)).toEqual({ route: "table", params: { table_id: "tbl_1" } });
    expect(parseCustomerLink(`https://${HOST}/c/ticket-claim?claim_code=clm_ab12`, HOST))
      .toEqual({ route: "ticket-claim", params: { claim_code: "clm_ab12" } });
    expect(parseCustomerLink(`https://${HOST}/c/voucher-claim?claim_code=vcl_9`, HOST))
      .toEqual({ route: "voucher-claim", params: { claim_code: "vcl_9" } });
    expect(parseCustomerLink(`https://${HOST}/c/invitation?token=inv_1&sig=ab12cd&table_id=t-1&capacity=6`, HOST))
      .toEqual({ route: "invitation", params: { token: "inv_1", sig: "ab12cd", table_id: "t-1", capacity: "6" } });
  });

  it("invitationRequiresTokenSigTableCapacity", () => {
    const base = `https://${HOST}/c/invitation?token=inv_1&sig=ab12cd&table_id=t-1&capacity=6`;
    for (const drop of ["token", "sig", "table_id", "capacity"]) {
      const link = base.replace(new RegExp(`[?&]${drop}=[^&]*`), (m) => (m.startsWith("?") ? "?" : ""));
      expect(parseCustomerLink(link, HOST), drop).toBeNull();
    }
    expect(parseCustomerLink(base.replace("capacity=6", "capacity=0"), HOST)).toBeNull();
  });

  it("rejectsForeignHostHttpUnknownRouteAndJavascriptScheme", () => {
    expect(parseCustomerLink("https://evil.example/c/table?table_id=t-1", HOST)).toBeNull();
    expect(parseCustomerLink(`https://${HOST}@evil.example/c/table?table_id=t-1`, HOST)).toBeNull();
    expect(parseCustomerLink(`http://${HOST}/c/table?table_id=t-1`, HOST)).toBeNull();
    expect(parseCustomerLink(`https://${HOST}/c/admin?x=1`, HOST)).toBeNull();
    expect(parseCustomerLink(`https://${HOST}/table?table_id=t-1`, HOST)).toBeNull();
    expect(parseCustomerLink("javascript:alert(1)//https://c.complex.example/c/table?table_id=t", HOST)).toBeNull();
    expect(parseCustomerLink(`https://${HOST}/c/table?table_id=${encodeURIComponent("<script>")}`, HOST)).toBeNull();
    // 未配置链接主机：一律不认。
    expect(parseCustomerLink(`https://${HOST}/c/table?table_id=t-1`, "")).toBeNull();
  });

  it("buildCustomerLink 生成的链接能被原样解析，且不写路由不认的参数", () => {
    const link = buildCustomerLink(HOST, "ticket-claim", { claim_code: "clm_ab12", customer_token: "secret" });
    expect(link).toBe(`https://${HOST}/c/ticket-claim?claim_code=clm_ab12`);
    expect(parseCustomerLink(link!, HOST)).toEqual({ route: "ticket-claim", params: { claim_code: "clm_ab12" } });
    expect(buildCustomerLink(HOST, "invitation", { token: "inv_1" })).toBeNull();
  });
});
