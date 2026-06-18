import { describe, expect, it } from "vitest";

import { adminRoutes } from "../adminRoutes";
import type { AdminUser } from "../types";
import { userHasPermissions } from "./PermissionGate";

const documentedPrimaryRoleMatrix = [
  { pageId: "ADM-OV-001", route: "/admin/overview", roles: ["super_admin", "ops_manager", "auditor"] },
  { pageId: "ADM-GOV-001", route: "/admin/governance/topology", roles: ["super_admin", "ops_manager", "auditor"] },
  { pageId: "ADM-GOV-002", route: "/admin/governance/organizations", roles: ["super_admin", "ops_manager", "merchant_ops"] },
  { pageId: "ADM-GOV-003", route: "/admin/governance/subject-bindings", roles: ["super_admin", "finance", "legal"] },
  { pageId: "ADM-GOV-004", route: "/admin/contracts", roles: ["super_admin", "legal", "finance", "ops_manager"] },
  { pageId: "ADM-GOV-005", route: "/admin/governance/admission-audits", roles: ["super_admin", "ops_manager", "legal", "auditor"] },
  { pageId: "ADM-GOV-006", route: "/admin/governance/supervisor-review", roles: ["super_admin", "ops_manager", "legal", "finance", "auditor"] },
  { pageId: "ADM-ACC-001", route: "/admin/access/users", roles: ["super_admin", "it_admin", "hr"] },
  { pageId: "ADM-ACC-002", route: "/admin/access/permission-templates", roles: ["super_admin", "it_admin"] },
  { pageId: "ADM-TKT-001", route: "/admin/ticketing/policies", roles: ["super_admin", "ticket_manager", "finance"] },
  { pageId: "ADM-TKT-002", route: "/admin/ticketing/dynamic-pricing", roles: ["super_admin", "ticket_manager", "finance"] },
  { pageId: "ADM-TKT-003", route: "/admin/ticketing/orders", roles: ["super_admin", "ticket_manager", "auditor"] },
  { pageId: "ADM-BAR-001", route: "/admin/bar/table-sessions", roles: ["super_admin", "bar_manager", "sales"] },
  { pageId: "ADM-BAR-002", route: "/admin/bar/orders", roles: ["super_admin", "bar_manager", "cashier"] },
  { pageId: "ADM-BAR-003", route: "/admin/bar/stored-wine", roles: ["super_admin", "bar_manager", "cashier"] },
  { pageId: "ADM-BAR-004", route: "/admin/bar/commissions", roles: ["super_admin", "finance", "bar_manager"] },
  { pageId: "ADM-AMU-001", route: "/admin/amusement/sessions", roles: ["super_admin", "amusement_manager"] },
  { pageId: "ADM-AMU-002", route: "/admin/amusement/group-bookings", roles: ["super_admin", "amusement_manager", "sales"] },
  { pageId: "ADM-AMU-003", route: "/admin/amusement/safety-incidents", roles: ["super_admin", "amusement_manager", "auditor"] },
  { pageId: "ADM-INV-001", route: "/admin/inventory/ledger", roles: ["super_admin", "warehouse", "finance"] },
  { pageId: "ADM-FIN-001", route: "/admin/finance/ar-ap-ledger", roles: ["super_admin", "finance", "auditor"] },
  { pageId: "ADM-FIN-002", route: "/admin/finance/reconciliation", roles: ["super_admin", "finance", "auditor"] },
  { pageId: "ADM-MER-001", route: "/admin/merchant-service/tickets", roles: ["super_admin", "merchant_ops", "ops_manager"] },
  { pageId: "ADM-MER-002", route: "/admin/merchant-portal/scope", roles: ["merchant_operator", "merchant_admin", "super_admin", "auditor"] },
  { pageId: "ADM-INT-001", route: "/admin/integrations/adapters", roles: ["super_admin", "it_admin", "integration_manager"] },
  { pageId: "ADM-INT-002", route: "/admin/integrations/events", roles: ["super_admin", "it_admin", "integration_manager", "auditor"] },
  { pageId: "ADM-RISK-001", route: "/admin/risk/alerts", roles: ["super_admin", "auditor", "ops_manager"] },
  { pageId: "ADM-EVD-001", route: "/admin/audit/events", roles: ["super_admin", "auditor"] },
  { pageId: "ADM-ANA-001", route: "/admin/analytics/business", roles: ["super_admin", "ops_manager", "finance"] },
  { pageId: "ADM-OPS-001", route: "/admin/ops/readiness", roles: ["super_admin", "it_admin", "auditor"] },
];

function user(accountType: string): AdminUser {
  return { id: `user-${accountType}`, username: accountType, accountType, permissions: [] };
}

describe("PermissionGate role matrix", () => {
  it("allows every documented primary role to read its page route", () => {
    for (const row of documentedPrimaryRoleMatrix) {
      const route = adminRoutes.find((item) => item.pageId === row.pageId);
      expect(route, `${row.pageId} route`).toBeDefined();
      for (const role of row.roles) {
        expect(
          userHasPermissions(user(role), route!.requiredPermissions),
          `${role} should access ${row.pageId} ${row.route}`,
        ).toBe(true);
      }
    }
  });

  it("keeps cross-domain page access denied for ordinary operators", () => {
    const financeRoute = adminRoutes.find((item) => item.pageId === "ADM-FIN-001")!;
    const integrationRoute = adminRoutes.find((item) => item.pageId === "ADM-INT-001")!;
    const barRoute = adminRoutes.find((item) => item.pageId === "ADM-BAR-002")!;

    expect(userHasPermissions(user("bar_manager"), financeRoute.requiredPermissions)).toBe(false);
    expect(userHasPermissions(user("finance"), integrationRoute.requiredPermissions)).toBe(false);
    expect(userHasPermissions(user("ticket_manager"), barRoute.requiredPermissions)).toBe(false);
  });
});
