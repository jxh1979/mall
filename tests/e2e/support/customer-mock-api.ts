import type { Page, Route } from "@playwright/test";

// 客人端 mock 后端：覆盖 /api/v1/customer/* 与购票/订台所需的 finance/payment-intents。
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
      { bar_id: "bar-main", table_id: "table-1", table_type: "卡座", table_name: "A01 卡座", capacity: 6, allowed_pass_free_guest_count: 2, min_consumption_cent: 100000, currency: "CNY", status: "available" },
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
      sales_channel: "customer_web",
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

  if (pathname === "/api/v1/finance/payment-intents") {
    return json(route, {
      id: "intent-1",
      status: "pending",
      business_order_type: bodyValue(body, "business_order_type", "businessOrderType"),
      business_order_id: bodyValue(body, "business_order_id", "businessOrderId"),
      amount_cent: bodyValue(body, "amount_cent", "amountCent"),
      channel: bodyValue(body, "channel", "channel") ?? "aggregate_pay",
    });
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
    if (isWrite) return handleWrite(route, url.pathname, state, options);
    const data = listRoutes(url.pathname, state, options);
    return json(route, data ?? []);
  });
}
