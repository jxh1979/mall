import { describe, expect, it } from "vitest";

import type { EffectivePermissions, EffectivePermissionsState } from "../types";
import { hasPermissions } from "./PermissionGate";

function ready(value: Partial<EffectivePermissions>): EffectivePermissionsState {
  return {
    status: "ready",
    value: { grants: [], denies: [], wildcard: false, ...value },
  };
}

describe("hasPermissions", () => {
  it("grant-only: 全部 required 在 grants 内才放行", () => {
    const state = ready({ grants: ["finance.ledger.read", "finance.ledger.write"] });
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.ledger.write"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.adjust.approve"])).toBe(false);
  });

  it("deny-override: 任一 required 命中 denies 即拒（优先于 grants）", () => {
    const state = ready({
      grants: ["finance.ar_ap.view", "finance.ledger.read"],
      denies: ["finance.ar_ap.view"],
    });
    expect(hasPermissions(state, ["finance.ar_ap.view"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.ar_ap.view"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
  });

  it("wildcard: 无 DENY 命中时放行任意 required", () => {
    const state = ready({ wildcard: true });
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["integration.adapter.read", "secret.ref.bind"])).toBe(true);
  });

  it("wildcard+deny: wildcard 仍受 DENY 约束（DENY 优先于 wildcard）", () => {
    const state = ready({ wildcard: true, denies: ["finance.refund.create"] });
    expect(hasPermissions(state, ["finance.refund.create"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.refund.create"])).toBe(false);
  });

  it("loading: fail-safe 一律拒绝", () => {
    const state: EffectivePermissionsState = { status: "loading" };
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(false);
    expect(hasPermissions(state, [])).toBe(false);
  });

  it("error: fail-safe 一律拒绝", () => {
    const state: EffectivePermissionsState = { status: "error" };
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(false);
    expect(hasPermissions(state, [])).toBe(false);
  });

  it("空 required: ready 态放行（无需任何权限）", () => {
    expect(hasPermissions(ready({}), [])).toBe(true);
    expect(hasPermissions(ready({ grants: ["finance.ledger.read"] }), [])).toBe(true);
    expect(hasPermissions(ready({ wildcard: true }), [])).toBe(true);
  });
});
