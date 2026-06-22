import { describe, expect, it } from "vitest";

import { adminRoutes } from "./adminRoutes";

const permissionsByPage = Object.fromEntries(
  adminRoutes.map((route) => [route.pageId, route.requiredPermissions]),
);

describe("adminRoutes canonical permissions", () => {
  it("uses backend/OpenAPI canonical permission codes for high-risk admin entries", () => {
    expect(permissionsByPage["ADM-OV-001"]).toContain("analytics.dashboard.view");
    expect(permissionsByPage["ADM-ACC-001"]).toContain("access.user.view");
    expect(permissionsByPage["ADM-ACC-002"]).toContain("access.permission_template.view");
    expect(permissionsByPage["ADM-TKT-001"]).toContain("ticket.product.view");
    expect(permissionsByPage["ADM-TKT-002"]).toContain("ticket.dynamic_pricing.view");
    expect(permissionsByPage["ADM-BAR-001"]).toContain("bar.table_session.view");
    expect(permissionsByPage["ADM-FIN-001"]).toContain("finance.ar_ap.view");
    expect(permissionsByPage["ADM-INT-001"]).toContain("integration.provider_adapter.view");
  });

  it("does not keep legacy UI-only shorthand permissions on routes", () => {
    const allPermissions = adminRoutes.flatMap((route) => route.requiredPermissions);
    expect(allPermissions).not.toEqual(expect.arrayContaining([
      "overview.read",
      "user.read",
      "permission_template.read",
      "ticket.dynamic.read",
      "bar.table.read",
      "finance.ledger.read",
      "integration.adapter.read",
    ]));
  });
});
