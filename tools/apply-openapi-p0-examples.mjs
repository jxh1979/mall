import fs from "node:fs";
import path from "node:path";
import YAML from "yaml";

const root = process.cwd();
const jsonPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const yamlPath = path.join(root, "docs", "openapi", "openapi-p0.yaml");

const openapi = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

const requestBodyExamples = {
  Webhook: {
    event_id: "evt_provider_20260612_000001",
    event_type: "payment.paid",
    occurred_at: "2026-06-12T20:30:00+08:00",
    provider: "aggregate_pay",
    provider_order_id: "po_20260612_000001",
    idempotency_key: "provider_evt_evt_provider_20260612_000001",
    raw_payload_hash: "sha256:9f86d081884c7d659a2feaa0c55ad015",
    payload: {
      amount_cent: 18800,
      currency: "CNY",
      normalized_status: "paid"
    },
    signature_version: "v1"
  },
  CustomerTicketOrderRequest: {
    ticket_type_id: "ticket_weekend_adult",
    quantity: 2,
    visit_date: "2026-06-20",
    contact_phone: "13800138000"
  },
  Verification: {
    idempotency_key: "verify_20260612_000001",
    credential_type: "qr",
    credential_payload: "qr_signed_payload_or_token",
    device_id: "device_staff_phone_001",
    point_id: "gate_bar_a",
    business_context: {
      scenario: "ticket_checkin",
      ticket_id: "ticket_inst_000001"
    }
  },
  CustomerBarReservationRequest: {
    bar_id: "bar_a",
    table_id: "table_a_vip_01",
    table_type: "vip",
    reservation_time: "2026-06-20T21:00:00+08:00",
    guest_count: 6,
    latest_arrival_at: "2026-06-20T23:00:00+08:00"
  },
  BarOrderRequest: {
    idempotency_key: "bar_order_20260612_000001",
    bar_id: "bar_a",
    table_id: "table_a_vip_01",
    reservation_id: "bar_resv_000001",
    items: [
      {
        sku_id: "sku_whisky_001",
        quantity: 1,
        unit_price_cent: 128800
      }
    ]
  },
  PaymentIntent: {
    idempotency_key: "pay_intent_20260612_000001",
    business_order_type: "bar_order",
    business_order_id: "bar_order_000001",
    amount_cent: 128800,
    currency: "CNY",
    channel: "aggregate_pay"
  },
  AmusementSafetyActionRequest: {
    idempotency_key: "amu_submit_20260612_000001",
    action_result: "passed",
    comment: "开放前检查通过",
    attachment_ids: ["att_photo_001"],
    evidence_attachment_ids: ["att_photo_001"]
  },
  ProviderAdapterTestRequest: {
    idempotency_key: "provider_test_20260612_000001",
    provider_code: "aggregate_pay_mock",
    scenario: "webhook",
    mock_payload: {
      provider_event_id: "evt_mock_000001",
      provider_status: "SUCCESS",
      amount_cent: 18800
    }
  }
};

const responseExamples = {
  Success: {
    success: true,
    trace_id: "req_20260612_000001",
    data: {
      id: "biz_object_000001",
      status: "accepted"
    },
    meta: {
      server_time: "2026-06-12T12:00:00Z"
    }
  },
  Page: {
    success: true,
    trace_id: "req_20260612_000002",
    data: {
      items: [],
      page: 1,
      page_size: 20,
      total: 0
    }
  },
  WebhookAccepted: {
    success: true,
    trace_id: "req_20260612_000003",
    data: {
      event_id: "evt_platform_20260612_000001",
      dedup_status: "created",
      compensation_required: false
    },
    meta: {
      server_time: "2026-06-12T12:00:00Z"
    }
  }
};

function getJsonContent(container, name) {
  const content = container?.[name]?.content?.["application/json"];
  if (!content) {
    throw new Error(`Missing application/json content for ${name}`);
  }
  return content;
}

for (const [name, example] of Object.entries(requestBodyExamples)) {
  getJsonContent(openapi.components.requestBodies, name).example = example;
}

for (const [name, example] of Object.entries(responseExamples)) {
  getJsonContent(openapi.components.responses, name).example = example;
}

openapi["x-p0-example-coverage"] = {
  strategy: "P0 first covers high-risk write flows and external integration flows; full operation examples are P1 documentation hardening.",
  request_bodies: Object.keys(requestBodyExamples),
  responses: Object.keys(responseExamples)
};

fs.writeFileSync(jsonPath, `${JSON.stringify(openapi, null, 2)}\n`, "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(openapi, { lineWidth: 120 }), "utf8");

console.log(`Applied ${Object.keys(requestBodyExamples).length} request examples and ${Object.keys(responseExamples).length} response examples to P0 OpenAPI.`);
