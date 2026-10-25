// 文件版本：V0.01
import type { Page, Route } from "@playwright/test";

// 客人端 mock 后端：覆盖 /api/v1/customer/*（购票与订台定金都走客人本人订单／订台在线付款，不再调 finance/payment-intents）。
// 与 admin mock-api 一致使用 { code, message, data } 信封；列表直接返回数组（unwrap 拆 data）。
// 写操作维护最小状态，使「下单 -> 支付意图」「订台 -> 定金意图」「签到累计」可被两端验收。

type ApiData = Record<string, unknown> | Array<Record<string, unknown>> | null;

type CustomerMockOptions = {
  // 让某个 GET 列表为空，用于覆盖空态（如 reservation-options）。
  emptyReservationOptions?: boolean;
  // 模拟下单失败（购票或订台），用于错误兜底两端一致性验收。
  orderFailure?: boolean;
};

const now = "2026-06-15T12:00:00.000Z";

function envelope(data: ApiData) {
  return { code: "OK", message: "ok", data };
}

function json(route: Route, data: ApiData, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(status >= 400 ? data : envelope(data)),
  });
}

function requestBody(route: Route): Record<string, unknown> {
  try {
    return route.request().postDataJSON() as Record<string, unknown>;
  } catch {
    return {};
  }
}

function bodyValue(body: Record<string, unknown>, snakeKey: string, camelKey: string) {
  return body[snakeKey] ?? body[camelKey];
}

function createState() {
  return {
    ticketProducts: [
      { ticket_type_id: "ticket-1", ticket_name: "周末通票", weekday_price_cent: 9900, weekend_price_cent: 12900, holiday_price_cent: 15900, currency: "CNY", status: "active" },
      { ticket_type_id: "ticket-2", ticket_name: "夜场单人票", weekday_price_cent: 6900, weekend_price_cent: 8900, holiday_price_cent: 9900, currency: "CNY", status: "active" },
    ],
    reservationOptions: [
      { bar_id: "bar-main", table_id: "table-1", table_type: "card_seat", table_name: "A01 卡座", capacity: 6, allowed_pass_free_guest_count: 2, min_consumption_cent: 100000, currency: "CNY", status: "available",
        reserved: false, service_fee_rate_bps: 1000, deposit_amount_cent: 20000 },
    ],
    orders: [] as Record<string, unknown>[],
    checkIns: [] as Record<string, unknown>[],
  };
}

type CustomerMockState = ReturnType<typeof createState>;

function listRoutes(pathname: string, state: CustomerMockState, options: CustomerMockOptions): ApiData | undefined {
  // 1.8 我的票：任意订单 id 的票实例列表
  if (/^\/api\/v1\/customer\/orders\/[^/]+\/tickets$/.test(pathname)) {
    return [
      { id: "ent-1", ticket_name: "周末通票", entitlement_type: "ticket", status: "active", total_times: 1, used_times: 0, remaining_times: 1, num_people: 1, valid_from: now, valid_to: "2026-12-31T23:59:59.000Z" },
    ];
  }
  switch (pathname) {
    // 我的票＝本人当前持有的票（GET /customer/my-tickets），按父票聚合。
    case "/api/v1/customer/my-tickets":
      return [
        { id: "ent-1", ticket_name: "周末通票", entitlement_type: "ticket", status: "active", total_times: 1, used_times: 0, remaining_times: 1, num_people: 1, valid_from: now, valid_to: "2026-12-31T23:59:59.000Z",
          parent_ticket_id: "pt-1", slot_index: 1, ticket_no: "TK-CUST-1", transfer_state: null, compensation_kind: null },
      ];
    case "/api/v1/customer/ticket-products":
      return state.ticketProducts;
    case "/api/v1/customer/bar-reservation-options":
      return options.emptyReservationOptions ? [] : state.reservationOptions;
    case "/api/v1/customer/orders":
      return state.orders;
    case "/api/v1/customer/tickets/ent-1/qr":
      return {
        id: "cred-1",
        ticket_id: "ent-1",
        order_id: "order-1",
        credential_token: "cred-customer-1",
        credential_type: "dynamic_qr",
        issued_at: now,
        expires_at: "2026-06-15T12:00:30.000Z",
        refreshed_count: 1,
      };
    case "/api/v1/customer/points":
      return { customer_id: "cust-1", balance: 1280, level: "黄金会员", recent_summary: null };
    case "/api/v1/customer/check-ins":
      return state.checkIns;
    case "/api/v1/customer/stored-value/account":
      return { id: "sv-1", customer_id: "cust-1", project_id: "venue-1", balance_cent: 58800, status: "active", created_at: now, updated_at: now };
    case "/api/v1/customer/stored-value/accounts":
      // ⑧ 多卡模型（AUD-061）：通用卡 + 商户卡分卡余额；WalletPage 走此复数端点。
      return [
        { id: "sv-generic", customer_id: "cust-1", project_id: "venue-1", merchant_id: null, card_scope: "project", fund_ownership: null, balance_cent: 58800, status: "active", created_at: now, updated_at: now },
        { id: "sv-merchant-1", customer_id: "cust-1", project_id: "venue-1", merchant_id: "merchant-1", card_scope: "merchant", fund_ownership: "platform_collect", balance_cent: 12000, status: "active", created_at: now, updated_at: now },
      ];
    case "/api/v1/customer/memberships":
      return [{ id: "mem-1", customer_id: "cust-1", plan_id: "plan-gold", project_id: "venue-1", start_at: "2026-06-01", expire_at: "2027-05-31", status: "active", auto_renew: true, payment_intent_id: null, created_at: now }];
    default:
      return undefined;
  }
}

function handleWrite(route: Route, pathname: string, state: CustomerMockState, options: CustomerMockOptions): Promise<void> {
  const body = requestBody(route);

  if (pathname === "/api/v1/customer/auth/phone-login") {
    return json(route, { customer_id: "cust-1", masked_phone: "138****8888", customer_token: "mock-customer-token", expires_at: "2026-12-31T23:59:59.000Z" });
  }

  if (pathname === "/api/v1/customer/ticket-orders") {
    if (options.orderFailure) return json(route, { code: "ORDER_REJECTED", message: "票种库存不足" }, 409);
    const quantity = Number(bodyValue(body, "quantity", "quantity") ?? 1);
    const order = {
      id: `order-${state.orders.length + 1}`,
      order_no: `TO-CUST-${state.orders.length + 1}`,
      customer_id: "cust-1",
      ticket_type_id: String(bodyValue(body, "ticket_type_id", "ticketTypeId") ?? "ticket-1"),
      price_rule_id: null,
      quantity,
      visit_date: bodyValue(body, "visit_date", "visitDate") ?? "2026-06-20",
      sales_channel: "app",
      buyer_phone: bodyValue(body, "contact_phone", "contactPhone") ?? "138****8888",
      amount_cent: 9900 * quantity,
      currency: "CNY",
      pay_status: "unpaid",
      issue_status: "not_issued",
      status: "created",
      created_at: now,
      updated_at: now,
    };
    state.orders.push(order);
    return json(route, order);
  }

  if (pathname === "/api/v1/customer/bar-reservations") {
    if (options.orderFailure) return json(route, { code: "RESERVATION_REJECTED", message: "该时段卡座已被预订" }, 409);
    return json(route, {
      id: "reservation-1",
      bar_id: String(bodyValue(body, "bar_id", "barId") ?? "bar-main"),
      table_id: String(bodyValue(body, "table_id", "tableId") ?? "table-1"),
      customer_id: "cust-1",
      reserved_at: bodyValue(body, "reservation_time", "reservationTime") ?? now,
      guest_count: Number(bodyValue(body, "guest_count", "guestCount") ?? 2),
      deposit_amount_cent: 20000,
      deposit_status: "pending",
      order_source: "customer_web",
      status: "reserved",
      deposit_intent_id: null,
      created_at: now,
    });
  }

  // 客人本人订单发起在线付款：支付渠道未配置（拉起参数为空），客户端应显示「支付暂不可用，未扣款」。
  if (/^\/api\/v1\/customer\/orders\/[^/]+\/payments$/.test(pathname)) {
    return json(route, { payment_intent_id: "pi-1", business_order_id: pathname.split("/")[5], amount_cent: 9900, channel: "wechat",
      gateway_status: "NOT_CONFIGURED", payment_url: null, launch: null });
  }

  // 客人本人订台定金在线付款：支付渠道未配置（拉起参数为空）。
  if (/^\/api\/v1\/customer\/bar-reservations\/[^/]+\/deposit-payments$/.test(pathname)) {
    return json(route, { payment_intent_id: "pi-dep-1", business_order_id: pathname.split("/")[5], amount_cent: 20000, channel: "wechat",
      gateway_status: "NOT_CONFIGURED", payment_url: null, launch: null });
  }

  if (pathname === "/api/v1/customer/check-ins") {
    const streak = state.checkIns.length + 1;
    const record = { id: `checkin-${streak}`, customer_id: "cust-1", check_in_date: "2026-06-15", streak, awarded_points: 10 * streak, source_terminal: "customer-web", created_at: now };
    state.checkIns.push(record);
    return json(route, record);
  }

  return json(route, { id: "mock-customer-post", status: "success" });
}

export async function installCustomerMockApi(page: Page, options: CustomerMockOptions = {}) {
  const state = createState();
  await page.route(/\/api\/(v1|open)\//, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const isWrite = ["POST", "PUT", "PATCH", "DELETE"].includes(method);

    if (url.pathname === "/api/v1/system/health") {
      return json(route, { ok: true, service: "customer-mock-api", time: now });
    }
    // 3.3 免登录会话恢复：mock 无 httpOnly cookie，对齐真实后端返回 401（否则兜底 200 空数组
    // 会被前端当作会话恢复成功，测试一进页面就是已登录态）。
    if (url.pathname === "/api/v1/customer/session") {
      return json(route, { code: "UNAUTHORIZED", message: "no customer session" }, 401);
    }
    if (isWrite) return handleWrite(route, url.pathname, state, options);
    const data = listRoutes(url.pathname, state, options);
    return json(route, data ?? []);
  });
}
