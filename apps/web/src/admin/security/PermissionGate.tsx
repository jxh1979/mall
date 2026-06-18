import React from "react";
import type { AdminUser } from "../types";

export const adminRolePermissions: Record<string, string[]> = {
  platform_admin: ["*"],
  super_admin: ["*"],
  ops_manager: ["overview.read", "finance.summary.read", "ops.alert.read", "governance.topology.read", "organization.read", "organization.write", "merchant.read", "merchant.write", "contract.read", "contract.submit", "contract.approve", "contract.archive", "admission.audit.read", "admission.audit.export", "governance.supervisor_review.view", "governance.supervisor_review.manage", "governance.supervisor_review.approve", "merchant.service_work_order.view", "merchant.service_work_order.create", "merchant.service_work_order.assign", "merchant.service_work_order.process", "merchant.service_work_order.close", "risk.alert.read", "risk.alert.resolve", "analytics.business.read"],
  auditor: ["overview.read", "finance.summary.read", "ops.alert.read", "governance.topology.read", "admission.audit.read", "admission.audit.export", "governance.supervisor_review.view", "governance.supervisor_review.manage", "governance.supervisor_review.approve", "ticket.order.read", "ticket.verify.read", "ticket.refund", "amusement.safety.read", "amusement.incident.write", "amusement.incident.close", "finance.ledger.read", "finance.ledger.write", "finance.adjust.approve", "finance.reconciliation.read", "finance.reconciliation.resolve", "merchant.portal.read", "merchant.service_work_order.view", "merchant.billing.view", "integration.event.read", "integration.retry.manage", "integration.compensation.manage", "risk.alert.read", "risk.alert.resolve", "audit.event.read", "audit.export", "ops.readiness.read", "ops.evidence.write"],
  it_admin: ["user.read", "user.create", "user.disable", "role.assign", "permission_template.read", "permission_template.write", "permission_template.grant", "integration.adapter.read", "integration.adapter.write", "secret.ref.bind", "integration.event.read", "integration.retry.manage", "integration.compensation.manage", "ops.readiness.read", "ops.evidence.write"],
  hr: ["user.read", "user.create", "user.disable", "role.assign"],
  finance: ["subject_binding.read", "subject_binding.write", "subject_binding.approve", "contract.read", "contract.submit", "contract.approve", "contract.archive", "governance.supervisor_review.view", "governance.supervisor_review.manage", "governance.supervisor_review.approve", "ticket.policy.read", "ticket.policy.write", "ticket.policy.publish", "ticket.dynamic.read", "ticket.dynamic.simulate", "ticket.dynamic.publish", "bar.commission.read", "bar.commission.settle", "inventory.ledger.read", "inventory.adjust", "inventory.count", "finance.ledger.read", "finance.ledger.write", "finance.adjust.approve", "finance.reconciliation.read", "finance.reconciliation.resolve", "analytics.business.read"],
  finance_operator: ["subject_binding.read", "subject_binding.write", "subject_binding.approve", "contract.read", "contract.submit", "contract.approve", "contract.archive", "governance.supervisor_review.view", "governance.supervisor_review.manage", "governance.supervisor_review.approve", "ticket.policy.read", "ticket.policy.write", "ticket.policy.publish", "ticket.dynamic.read", "ticket.dynamic.simulate", "ticket.dynamic.publish", "bar.commission.read", "bar.commission.settle", "inventory.ledger.read", "inventory.adjust", "inventory.count", "finance.ledger.read", "finance.ledger.write", "finance.adjust.approve", "finance.reconciliation.read", "finance.reconciliation.resolve", "analytics.business.read"],
  legal: ["subject_binding.read", "subject_binding.write", "subject_binding.approve", "contract.read", "contract.submit", "contract.approve", "contract.archive", "admission.audit.read", "admission.audit.export", "governance.supervisor_review.view", "governance.supervisor_review.manage", "governance.supervisor_review.approve"],
  merchant_ops: ["organization.read", "organization.write", "merchant.read", "merchant.write", "merchant.service_work_order.view", "merchant.service_work_order.create", "merchant.service_work_order.assign", "merchant.service_work_order.process", "merchant.service_work_order.close"],
  merchant_operator: ["merchant.portal.read", "merchant.service_work_order.view", "merchant.billing.view"],
  merchant_admin: ["merchant.portal.read", "merchant.service_work_order.view", "merchant.billing.view"],
  ticket_manager: ["ticket.policy.read", "ticket.policy.write", "ticket.policy.publish", "ticket.dynamic.read", "ticket.dynamic.simulate", "ticket.dynamic.publish", "ticket.order.read", "ticket.verify.read", "ticket.refund"],
  bar_manager: ["bar.table.read", "bar.table.reserve", "bar.table.transfer", "bar.table.merge", "bar.table.renew", "bar.order.read", "bar.order.write", "bar.bill.adjust", "bar.bill.settle", "bar.storage.read", "bar.saved_bottle.manage", "bar.commission.read", "bar.commission.settle"],
  bar_operator: ["bar.table.read", "bar.table.reserve", "bar.table.transfer", "bar.table.merge", "bar.table.renew"],
  sales: ["bar.table.read", "bar.table.reserve", "bar.table.transfer", "bar.table.merge", "bar.table.renew", "amusement.group.read", "amusement.group.write"],
  cashier: ["bar.order.read", "bar.order.write", "bar.bill.adjust", "bar.bill.settle", "bar.storage.read", "bar.saved_bottle.manage"],
  amusement_manager: ["amusement.project.read", "amusement.session.write", "amusement.group.read", "amusement.group.write", "amusement.safety.read", "amusement.incident.write", "amusement.incident.close"],
  warehouse: ["inventory.ledger.read", "inventory.adjust", "inventory.count"],
  integration_manager: ["integration.adapter.read", "integration.adapter.write", "secret.ref.bind", "integration.event.read", "integration.retry.manage", "integration.compensation.manage"],
};

function normalizeAccountType(accountType: string) {
  return accountType.trim().toLowerCase().replace(/-/g, "_");
}

function grantedPermissionsFor(user: AdminUser) {
  return [...(adminRolePermissions[normalizeAccountType(user.accountType)] ?? []), ...(user.permissions ?? [])];
}

export function userHasPermissions(user: AdminUser, permissions: string[]) {
  const granted = grantedPermissionsFor(user);
  return granted.includes("*") || permissions.every((permission) => granted.includes(permission));
}

export function PermissionGate({ user, permissions, children }: { user: AdminUser; permissions: string[]; children: React.ReactNode }) {
  if (!userHasPermissions(user, permissions)) {
    return <div className="admin-denied" data-testid="admin-permission-denied">权限不足：当前角色不能访问该后台页面。</div>;
  }
  return <>{children}</>;
}
