import { expect, test, type APIRequestContext } from "@playwright/test";

// 招商工作台 UI ↔ 后端契约对齐 · 真实后端活证据（审计 P1-1 / 修复 1.3）
// 契约对齐后：LeasingWorkbenchPage.tsx 的真实下拉 option 值直接打进真实后端，
// 断言全部被 requireEnum 接受（正向）；同时旧的错值仍被 400 拒（负向守卫，证明已彻底移除）。
// 前端来源：apps/web/src/admin/pages/LeasingWorkbenchPage.tsx 来源/线索状态/推进阶段下拉
//          vs backend LeasingService.java LEAD_SOURCES / LEAD_STATUSES / INTENT_STAGES。

const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
const username = process.env.REAL_BACKEND_USERNAME ?? "admin";
const password = process.env.REAL_BACKEND_PASSWORD ?? "admin-pass";

// 前端当前真实下拉值（修复 1.3 后）。
const FRONTEND_SOURCES = ["outbound", "online_inquiry", "referral", "exhibition"];
const FRONTEND_INTENT_STAGES = ["requirement_confirmed", "proposal_quoted", "negotiation", "pending_signing"];
// 修复前的错值，现已从前端移除，后端应继续拒。
const REMOVED_SOURCES = ["walk_in", "online"];
const REMOVED_STAGES = ["terms_agreed", "ready_to_sign"];

async function bearer(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${backendBaseUrl}/api/v1/auth/login`, {
    data: { login_name: username, password },
  });
  expect(res.ok(), await res.text()).toBeTruthy();
  const body = await res.json();
  return `Bearer ${body.data.access_token}`;
}

test.describe("招商工作台 UI/后端契约对齐 · 真实后端活证据", () => {
  test.beforeEach(() => {
    test.skip(process.env.REAL_BACKEND_E2E !== "1", "set REAL_BACKEND_E2E=1 to run");
  });

  // 正向：前端来源下拉每个 option 都是后端 LEAD_SOURCES 合法值 → 真实后端接受（2xx，落库）。
  for (const source of FRONTEND_SOURCES) {
    test(`LEASE-CONTRACT 前端来源 source=${source} 被真实后端接受（2xx）`, async ({ request }) => {
      const auth = await bearer(request);
      const res = await request.post(`${backendBaseUrl}/api/v1/leasing/leads`, {
        headers: { authorization: auth },
        data: { contact_name: "契约对齐活证据", source, idempotency_key: `align-src-${source}` },
      });
      expect(res.ok(), await res.text()).toBeTruthy();
    });
  }

  // 负向守卫：已从前端移除的错值仍被后端 400 拒（落库前，零写入）。
  for (const source of REMOVED_SOURCES) {
    test(`LEASE-CONTRACT 已移除来源 source=${source} 仍被真实后端 400 拒`, async ({ request }) => {
      const auth = await bearer(request);
      const res = await request.post(`${backendBaseUrl}/api/v1/leasing/leads`, {
        headers: { authorization: auth },
        data: { contact_name: "已移除值", source, idempotency_key: `removed-src-${source}` },
      });
      expect(res.status()).toBe(400);
      expect((await res.json()).message).toContain("source is invalid");
    });
  }

  // 正向：前端推进阶段每个 option 都是后端 INTENT_STAGES 合法值 → 通过 requireEnum（不再是 target_stage is invalid）。
  // 对不存在的意向 advance：合法枚举会通过枚举校验、之后因意向不存在失败，故 message 不含 "target_stage is invalid"。
  for (const stage of FRONTEND_INTENT_STAGES) {
    test(`LEASE-CONTRACT 前端推进阶段 stage=${stage} 通过真实后端枚举校验`, async ({ request }) => {
      const auth = await bearer(request);
      const res = await request.post(
        `${backendBaseUrl}/api/v1/leasing/intents/intent_probe_nonexistent/advance`,
        {
          headers: { authorization: auth },
          data: { target_stage: stage, idempotency_key: `align-stage-${stage}` },
        },
      );
      expect((await res.json()).message ?? "").not.toContain("target_stage is invalid");
    });
  }

  // 负向守卫：已移除阶段仍被后端 400 枚举拒。
  for (const stage of REMOVED_STAGES) {
    test(`LEASE-CONTRACT 已移除阶段 stage=${stage} 仍被真实后端 400 拒`, async ({ request }) => {
      const auth = await bearer(request);
      const res = await request.post(
        `${backendBaseUrl}/api/v1/leasing/intents/intent_probe_nonexistent/advance`,
        {
          headers: { authorization: auth },
          data: { target_stage: stage, idempotency_key: `removed-stage-${stage}` },
        },
      );
      expect(res.status()).toBe(400);
      expect((await res.json()).message).toContain("target_stage is invalid");
    });
  }
});
