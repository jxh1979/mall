import { expect, test, type APIRequestContext } from "@playwright/test";

// 1.8 客人端"我的票" · 真实后端活证据
// 客人真登录 → 真实下单 → GET /customer/orders/{id}/tickets 本人可查（安全投影）；
// 他人订单 403（越权负例，安全关键）；不存在订单 404；无 token 401。

const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
const OTP = process.env.REAL_BACKEND_CUSTOMER_OTP ?? "000000";
const username = process.env.REAL_BACKEND_USERNAME ?? "admin";
const password = process.env.REAL_BACKEND_PASSWORD ?? "admin-pass";

async function bearer(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${backendBaseUrl}/api/v1/auth/login`, {
    data: { login_name: username, password },
  });
  expect(res.ok(), await res.text()).toBeTruthy();
  return `Bearer ${(await res.json()).data.access_token}`;
}

async function customerToken(request: APIRequestContext, phone: string): Promise<string> {
  const res = await request.post(`${backendBaseUrl}/api/v1/customer/auth/phone-login`, {
    data: { phone, sms_code: OTP, scene: "miniapp" },
  });
  expect(res.ok(), await res.text()).toBeTruthy();
  return (await res.json()).data.customer_token;
}

test.describe("1.8 我的票 · 真实后端活证据", () => {
  test.beforeEach(() => {
    test.skip(process.env.REAL_BACKEND_E2E !== "1", "set REAL_BACKEND_E2E=1 to run");
  });

  test("TICKETS-LIVE-01 本人订单可列票实例，他人 403，不存在 404，无 token 401", async ({ request }) => {
    const mine = await customerToken(request, "13911112222");

    // 平台先造一个上架票种（幂等 key 固定，可重跑），客人端能看到并真实下单。
    const auth = await bearer(request);
    const created = await request.post(`${backendBaseUrl}/api/v1/ticketing/ticket-types`, {
      headers: { authorization: auth },
      data: {
        idempotency_key: "live-18-ticket",
        ticket_name: "活证据全日票",
        base_calendar_price_mode: "calendar",
        weekday_price_cent: 9900,
        weekend_price_cent: 12900,
        holiday_price_cent: 15900,
        status: "active",
      },
    });
    expect(created.ok(), await created.text()).toBeTruthy();
    const ticketTypeId = (await created.json()).data.id;

    const orderRes = await request.post(`${backendBaseUrl}/api/v1/customer/ticket-orders`, {
      headers: { "X-Customer-Token": mine, "Idempotency-Key": `live-18-${Date.now()}` },
      data: {
        ticket_type_id: ticketTypeId,
        quantity: 1,
        visit_date: "2026-07-15",
        contact_phone: "13911112222",
      },
    });
    expect(orderRes.ok(), await orderRes.text()).toBeTruthy();
    const order = (await orderRes.json()).data;

    // 本人查订单票实例：200，安全投影结构（未支付/未出票时为空列表——结构与归属先行验证）。
    const ticketsRes = await request.get(`${backendBaseUrl}/api/v1/customer/orders/${order.id}/tickets`, {
      headers: { "X-Customer-Token": mine },
    });
    expect(ticketsRes.ok(), await ticketsRes.text()).toBeTruthy();
    const page = (await ticketsRes.json()).data;
    expect(Array.isArray(page.items)).toBeTruthy();
    for (const t of page.items) {
      expect(t.allocated_revenue_cent ?? t.allocatedRevenueCent, "内部分账字段不得出网").toBeUndefined();
      expect(t.entitlement_code ?? t.entitlementCode, "完整核销码不得出网").toBeUndefined();
    }

    // 他人越权：另一个真实客人查我的订单 → 403。
    const other = await customerToken(request, "13933334444");
    const forbidden = await request.get(`${backendBaseUrl}/api/v1/customer/orders/${order.id}/tickets`, {
      headers: { "X-Customer-Token": other },
    });
    expect(forbidden.status(), "他人订单必须 403").toBe(403);

    // 不存在订单 → 404；无 token → 401。
    const notFound = await request.get(`${backendBaseUrl}/api/v1/customer/orders/order_ghost/tickets`, {
      headers: { "X-Customer-Token": mine },
    });
    expect(notFound.status()).toBe(404);
    const unauthorized = await request.get(`${backendBaseUrl}/api/v1/customer/orders/${order.id}/tickets`);
    expect(unauthorized.status()).toBe(401);
  });
});
