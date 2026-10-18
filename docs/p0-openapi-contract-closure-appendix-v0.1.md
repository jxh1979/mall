# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.23

## 1. 文档定位

本文档由 `docs/openapi/openapi-p0.json` 反向生成，用于收口本轮文档审计发现的三个执行缺口：

1. OpenAPI 已声明的权限码没有全部进入人工权限矩阵。
2. OpenAPI 已声明的成功/失败事件没有全部进入人工事件证据矩阵。
3. P0 任务文档不能逐 operation 追溯到权限、事件、终端和验收责任。

本文不是新需求池，不改变已拍板业务口径。若本文与历史 PRD、历史接口表或旧能力路径冲突，以 `00-current-effective-document-entry-v0.1.md`、`docs/openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 和本文为准。

## 2. 收口结论

| 检查项 | 当前结果 | 执行结论 |
|---|---:|---|
| OpenAPI operation 数 | 882 | 全部必须进入 Java Controller、DTO、Service、权限、事件和验收追溯 |
| OpenAPI 权限/通用权限状态数 | 365 | 人工权限矩阵正文未覆盖 264 项；缺口以本文第 3 节补齐 |
| OpenAPI 成功/失败事件数 | 1764 | 人工事件矩阵正文未覆盖 1723 项；完整事件字典以本文第 4 节为准 |
| operation 级任务追溯 | 882 | 人工任务映射正文未逐项覆盖 876 个 operation；逐 operation 映射以本文第 5 节为准 |
| 写接口数量 | 524 | 写接口、外部接入接口进入编码或联调前必须补请求/响应示例 |

## 3. 权限码与认证上下文补充表

> 本节覆盖 OpenAPI 中全部 `x-permission`。其中 `public`、`authenticated`、`not_applicable` 是通用权限状态，不是员工岗位权限；员工、设备、客人、开放应用和 Webhook 的身份来源必须看 `x-auth-context`。

| 权限码/通用状态 | 认证上下文 | 数据范围 | operation 数 | 代表 operationId | 覆盖状态 |
|---|---|---:|---:|---|---|
| `access.grant_request.apply` | staff-token | project | 1 | createAccessGrantRequest | OpenAPI 补充，以本附录为准 |
| `access.grant_request.review` | staff-token | project | 1 | reviewAccessGrantRequest | OpenAPI 补充，以本附录为准 |
| `access.grant_request.view` | staff-token | project | 2 | listAccessGrantRequests, getAccessGrantSummary | OpenAPI 补充，以本附录为准 |
| `access.permission_template.apply` | staff-token | project | 1 | applyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.manage` | staff-token | project | 3 | createPermissionTemplate, updatePermissionTemplate, copyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.view` | staff-token | project | 1 | listPermissionTemplates | 已在权限矩阵正文出现 |
| `access.permission.view` | staff-token | project | 1 | listPermissions | 已在权限矩阵正文出现 |
| `access.role.assign` | staff-token | project | 9 | assignUserRole, unassignUserRole, listPositionRoleTemplates, createPositionRoleTemplate, updatePositionRoleTemplate, deletePositionRoleTemplate, assignUserPosition, listApprovalLines, upsertApprovalLine | OpenAPI 补充，以本附录为准 |
| `access.role.manage` | staff-token | project | 4 | createRole, listRoleProjectOptions, getRoleViewableProjects, updateRoleViewableProjects | 已在权限矩阵正文出现 |
| `access.role.view` | staff-token | project | 1 | listRoles | 已在权限矩阵正文出现 |
| `access.staff_level.manage` | staff-token | project | 1 | assignStaffLevel | OpenAPI 补充，以本附录为准 |
| `access.staff_level.view` | staff-token | project | 1 | listStaffLevels | OpenAPI 补充，以本附录为准 |
| `access.temporary_grant.create` | staff-token | project | 1 | createTemporaryGrant | 已在权限矩阵正文出现 |
| `access.temporary_grant.revoke` | staff-token | project | 1 | revokeTemporaryGrant | 已在权限矩阵正文出现 |
| `access.user.manage` | staff-token | project | 1 | createAccessUser | 已在权限矩阵正文出现 |
| `access.user.view` | staff-token | project | 2 | listAccessUsers, getUserEffectivePermissions | 已在权限矩阵正文出现 |
| `amusement.capacity_config.manage` | staff-token | project-amusement | 3 | listAmusementCapacityConfigs, createAmusementCapacityConfig, disableAmusementCapacityConfig | OpenAPI 补充，以本附录为准 |
| `amusement.compensation_policy.manage` | staff-token | project | 1 | configureAmusementCompensationPolicy | OpenAPI 补充，以本附录为准 |
| `amusement.device_status.view` | staff-token | project | 1 | listAmusementDeviceStatuses | 已在权限矩阵正文出现 |
| `amusement.device.manage` | staff-token | project-amusement | 1 | createAmusementDevice | 已在权限矩阵正文出现 |
| `amusement.device.view` | staff-token | project-amusement | 1 | listAmusementDevices | 已在权限矩阵正文出现 |
| `amusement.entitlement_session.claim` | staff-token | merchant | 1 | claimAmusementEntitlementSession | OpenAPI 补充，以本附录为准 |
| `amusement.entitlement_session.initiate` | staff-token | merchant | 1 | initiateAmusementEntitlementSession | OpenAPI 补充，以本附录为准 |
| `amusement.entitlement_session.view` | staff-token | merchant | 2 | listAmusementEntitlementSessions, getAmusementEntitlementSession | OpenAPI 补充，以本附录为准 |
| `amusement.group_booking.approve` | staff-token | project-amusement | 1 | approveAmusementGroupBooking | OpenAPI 补充，以本附录为准 |
| `amusement.group_booking.manage` | staff-token | project-amusement | 2 | createAmusementGroupBooking, releaseAmusementGroupBooking | OpenAPI 补充，以本附录为准 |
| `amusement.group_booking.view` | staff-token | project-amusement | 1 | listAmusementGroupBookings | OpenAPI 补充，以本附录为准 |
| `amusement.incident.close` | staff-token | project-amusement | 1 | closeAmusementIncident | OpenAPI 补充，以本附录为准 |
| `amusement.incident.create` | staff-token | project-amusement | 1 | createAmusementIncident | OpenAPI 补充，以本附录为准 |
| `amusement.incident.process` | staff-token | project-amusement | 1 | addAmusementIncidentAction | OpenAPI 补充，以本附录为准 |
| `amusement.incident.view` | staff-token | project-amusement | 1 | listAmusementIncidents | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.confirm` | staff-token | project-amusement | 1 | confirmAmusementMaintenanceResume | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.read` | staff-token | project-amusement | 2 | listAmusementMaintenanceRecords, getAmusementMaintenanceRecord | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.submit` | staff-token | project-amusement-point | 1 | createAmusementMaintenanceRecord | OpenAPI 补充，以本附录为准 |
| `amusement.over_capacity_policy.configure` | staff-token | project-amusement | 1 | configureAmusementOverCapacityPolicy | OpenAPI 补充，以本附录为准 |
| `amusement.over_capacity_policy.view` | staff-token | project-amusement | 1 | getAmusementOverCapacityPolicy | OpenAPI 补充，以本附录为准 |
| `amusement.park_fault.confirm_as_engineering_safety` | staff-token | project | 3 | confirmAmusementParkWideFaultActivation, confirmAmusementParkWideFaultLift, confirmAmusementFaultCompensationBatch | OpenAPI 补充，以本附录为准 |
| `amusement.park_fault.confirm_as_head` | staff-token | project | 3 | initiateAmusementParkWideFault, initiateAmusementParkWideFaultLift, initiateAmusementFaultCompensationBatch | OpenAPI 补充，以本附录为准 |
| `amusement.park_fault.view` | staff-token | project | 5 | listAmusementParkWideFaults, listAmusementParkWideFaultActions, getAmusementCompensationPolicy, listAmusementFaultCompensationBatches, listAmusementFaultCompensationItems | OpenAPI 补充，以本附录为准 |
| `amusement.project_entitlement.approve` | staff-token | project-amusement | 1 | approveDeleteAmusementProjectEntitlement | OpenAPI 补充，以本附录为准 |
| `amusement.project_entitlement.manage` | staff-token | project-amusement | 6 | listAmusementProjectEntitlements, createAmusementProjectEntitlement, removeAmusementProjectEntitlement, archiveAmusementProjectEntitlement, unarchiveAmusementProjectEntitlement, requestDeleteAmusementProjectEntitlement | OpenAPI 补充，以本附录为准 |
| `amusement.project.manage` | staff-token | project | 1 | createAmusementProjectRule | 已在权限矩阵正文出现 |
| `amusement.queue.manage` | staff-token | project-amusement | 2 | createAmusementQueueEntry, changeAmusementQueueEntryStatus | OpenAPI 补充，以本附录为准 |
| `amusement.queue.view` | staff-token | project | 1 | listAmusementQueueEntries | 已在权限矩阵正文出现 |
| `amusement.reservation.manage` | staff-token | project-amusement | 2 | createAmusementReservation, changeAmusementReservationStatus | OpenAPI 补充，以本附录为准 |
| `amusement.reservation.view` | staff-token | project | 1 | listAmusementReservations | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.approve_reopen` | staff-token | project-amusement | 1 | approveAmusementReopen | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.create` | staff-token | project-amusement | 1 | createAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.recheck` | staff-token | project-amusement | 1 | recheckAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.rectify` | staff-token | project-amusement | 1 | rectifyAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.submit` | staff-token | project-amusement | 1 | submitAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.view` | staff-token | project-amusement | 3 | getAmusementOpenEligibility, listAmusementSafetyInspections, listAmusementInspectionTasks | 已在权限矩阵正文出现 |
| `amusement.safety_notice.manage` | staff-token | project-amusement | 1 | createAmusementSafetyNotice | OpenAPI 补充，以本附录为准 |
| `amusement.safety_notice.view` | staff-token | project-amusement | 1 | listAmusementSafetyNotices | OpenAPI 补充，以本附录为准 |
| `amusement.safety_template.manage` | staff-token | project-amusement | 1 | createAmusementSafetyTemplate | 已在权限矩阵正文出现 |
| `amusement.safety_template.view` | staff-token | project-amusement | 1 | listAmusementSafetyTemplates | 已在权限矩阵正文出现 |
| `amusement.session.manage` | staff-token | project, project-amusement | 5 | createAmusementProject, createAmusementSession, assignAmusementProjectSpace, listAmusementProjectsWithoutSpace, listAmusementProjectsWithoutDeviceRegistry | 已在权限矩阵正文出现 |
| `amusement.session.view` | staff-token | project-amusement | 2 | listAmusementProjects, listAmusementSessions | 已在权限矩阵正文出现 |
| `amusement.verification.execute` | staff-token | project-amusement-point | 1 | createAmusementVerification | 已在权限矩阵正文出现 |
| `amusement.verification.view` | staff-token | project-amusement | 1 | listAmusementVerifications | 已在权限矩阵正文出现 |
| `amusement.waiver.acknowledge` | staff-token | project-amusement | 1 | acknowledgeAmusementSafetyNotice | OpenAPI 补充，以本附录为准 |
| `analytics.dashboard.view` | staff-token | platform, project | 19 | listBusinessMetricSnapshots, getP0OverviewDashboard, getAnalyticsDomainDashboard, listMerchantHealthScores, listAnalyticsMerchants, listMetricDefinitions, getMetricDetail, getRobotAnalytics, getAnalyticsTrafficHourly, listOperatingParams, getAiThresholds, listNotificationWindows ... | 已在权限矩阵正文出现 |
| `analytics.group.view` | staff-token | group | 1 | getGroupOverview | OpenAPI 补充，以本附录为准 |
| `analytics.metric_anomaly.manage` | staff-token | project | 1 | disposeMetricAnomaly | OpenAPI 补充，以本附录为准 |
| `analytics.operating_params.manage` | staff-token | platform, project | 7 | upsertOperatingParams, updateAiThresholds, upsertRevenueAttributionRule, upsertBusinessDayCutoff, updateAnomalyConfig, upsertHolidayPeriod, fetchHolidayCalendar | OpenAPI 补充，以本附录为准 |
| `analytics.report.export` | staff-token | platform | 1 | exportMonthlyReport | OpenAPI 补充，以本附录为准 |
| `analytics.review.lock` | staff-token | project | 2 | listReviewLocks, createReviewLock | OpenAPI 补充，以本附录为准 |
| `analytics.risk_alert.manage` | staff-token | project | 3 | generateLocalLlmAiQualitySuggestion, createRiskAlert, resolveRiskAlert | 已在权限矩阵正文出现 |
| `analytics.risk_alert.view` | staff-token | project | 8 | listAiQualityResults, listAiQualityTasks, listCustomerIntents, listMetricAnomalies, listMetricAnomalyTimeline, listRiskAlerts, listRiskRules, listStaffServiceScores | 已在权限矩阵正文出现 |
| `audit.operation_log.export` | staff-token | platform | 1 | exportAuditOperationEvents | OpenAPI 补充，以本附录为准 |
| `audit.operation_log.view` | staff-token | platform | 1 | queryAuditOperationEvents | OpenAPI 补充，以本附录为准 |
| `authenticated` | staff-token | current-session, self | 3 | logout, refreshToken, getEffectivePermissions | 已在权限矩阵正文出现 |
| `bar.bill.adjust` | staff-token | project-bar-table | 1 | adjustBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.reopen` | staff-token | project-bar-table | 1 | reopenBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.split_merge` | staff-token | project-bar-table | 3 | listBarBillOperations, mergeBarBill, splitBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.tip` | staff-token | project-bar-table | 1 | addBarBillTip | OpenAPI 补充，以本附录为准 |
| `bar.category.manage` | staff-token | project | 1 | saveBarProductCategory | OpenAPI 补充，以本附录为准 |
| `bar.category.view` | staff-token | project | 1 | listBarProductCategories | OpenAPI 补充，以本附录为准 |
| `bar.combo.manage` | staff-token | project | 1 | saveBarCombo | OpenAPI 补充，以本附录为准 |
| `bar.combo.view` | staff-token | project | 1 | listBarCombos | OpenAPI 补充，以本附录为准 |
| `bar.deposit_rule.manage` | staff-token | project-bar | 1 | createBarDepositRule | OpenAPI 补充，以本附录为准 |
| `bar.entitlement.read` | staff-token | project-bar | 1 | listBarEntitlementRedemptions | OpenAPI 补充，以本附录为准 |
| `bar.entitlement.redeem` | staff-token | project-bar | 1 | redeemBarDrinkEntitlement | OpenAPI 补充，以本附录为准 |
| `bar.order_item.void` | staff-token | project, project-bar-table | 4 | voidBarOrderItem, decideBarOrderItemVoid, listBarOrderItemVoids, listBarVoidReasonCodes | OpenAPI 补充，以本附录为准 |
| `bar.order_item.void_decide` | staff-token | project | 2 | saveBarVoidReasonCode, archiveBarVoidReasonCode | OpenAPI 补充，以本附录为准 |
| `bar.order.create` | staff-token | project, project-bar-table | 6 | createBarOrder, createBarOrderBill, deliverBarOrder, addBarOrderItem, listBarProductionTickets, placeBarScanOrder | 已在权限矩阵正文出现 |
| `bar.order.view` | staff-token | project-bar-table | 1 | listBarOrders | 已在权限矩阵正文出现 |
| `bar.product.manage` | staff-token | project | 3 | createBarProduct, assignBarProductCategory, uploadBarCatalogImage | OpenAPI 补充，以本附录为准 |
| `bar.product.sold_out` | staff-token | project-bar-table | 1 | markBarProductSoldOut | OpenAPI 补充，以本附录为准 |
| `bar.product.view` | staff-token | project | 2 | listBarProducts, listBarProductsByCategory | OpenAPI 补充，以本附录为准 |
| `bar.production_ticket.manage` | staff-token | project-bar-table | 1 | transitionBarProductionTicket | OpenAPI 补充，以本附录为准 |
| `bar.production_ticket.view` | staff-token | project-bar-table | 1 | listBarProductionTicketEvents | OpenAPI 补充，以本附录为准 |
| `bar.reservation.create` | staff-token | project-bar, project-merchant-space | 2 | createBarReservation, listBarTableAvailability | 已在权限矩阵正文出现 |
| `bar.reservation.invitation.manage` | staff-token | project-bar | 2 | issueBarReservationInvitation, revokeBarReservationInvitation | OpenAPI 补充，以本附录为准 |
| `bar.reservation.manage` | staff-token | project-bar | 3 | payBarReservationDeposit, checkInBarReservationDeposit, cancelBarReservation | OpenAPI 补充，以本附录为准 |
| `bar.reservation.view` | staff-token | project-bar, project-bar-table | 2 | listBarReservations, getBarReservationInvitation | 已在权限矩阵正文出现 |
| `bar.saved_bottle.manage` | staff-token | project-bar-customer, project-bar-table | 5 | listSavedBottles, createSavedBottle, claimSavedBottle, cancelSavedBottle, disputeSavedBottle | 已在权限矩阵正文出现 |
| `bar.shift.close` | staff-token | project-bar-table | 1 | closeBarShift | OpenAPI 补充，以本附录为准 |
| `bar.shift.manage` | staff-token | project-bar-table | 2 | listBarShifts, openBarShift | OpenAPI 补充，以本附录为准 |
| `bar.table_commission.calculate` | staff-token | project-bar | 1 | calculateBarTableCommission | 已在权限矩阵正文出现 |
| `bar.table_session.create` | staff-token | project-bar-table | 1 | createBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.merge` | staff-token | project-bar-table | 1 | mergeBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.renew` | staff-token | project-bar-table | 1 | renewBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.transfer` | staff-token | project-bar-table | 1 | transferBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.view` | staff-token | project-bar-table | 1 | listBarTableSessions | OpenAPI 补充，以本附录为准 |
| `bar.table.guest_checkin` | staff-token | project-bar | 3 | checkInBarReservationGuest, verifyBarReservationInvitationEntry, verifyBarReservationEntryAllowance | 已在权限矩阵正文出现 |
| `bar.table.manage` | staff-token | project-bar, project-bar-table | 3 | createBarTable, assignBarTableSpace, listBarTablesWithoutSpace | 已在权限矩阵正文出现 |
| `bar.table.open_close` | staff-token | project-bar | 1 | openBarTable | 已在权限矩阵正文出现 |
| `bar.table.view` | staff-token | project-bar | 1 | listBarTables | 已在权限矩阵正文出现 |
| `compliance.contract_key_date.manage` | staff-token | - | 1 | createContractKeyDate | OpenAPI 补充，以本附录为准 |
| `compliance.contract_key_date.view` | staff-token | - | 1 | listContractKeyDates | OpenAPI 补充，以本附录为准 |
| `compliance.expiry_item.manage` | staff-token | - | 1 | createComplianceExpiryItem | OpenAPI 补充，以本附录为准 |
| `compliance.expiry_item.view` | staff-token | - | 1 | listComplianceExpiryItems | OpenAPI 补充，以本附录为准 |
| `compliance.reminder.scan` | staff-token | - | 1 | scanComplianceReminders | OpenAPI 补充，以本附录为准 |
| `compliance.reminder.view` | staff-token | - | 1 | listComplianceReminders | OpenAPI 补充，以本附录为准 |
| `contract.approval.flow.manage` | staff-token | platform | 2 | listContractApprovalFlow, saveContractApprovalFlow | OpenAPI 补充，以本附录为准 |
| `contract.approve.business` | staff-token | platform | 2 | approveContractStage, approveContractChange | OpenAPI 补充，以本附录为准 |
| `contract.change.manage` | staff-token | platform | 3 | listContractChanges, raiseContractChange, listContractAmendments | OpenAPI 补充，以本附录为准 |
| `contract.draft.manage` | staff-token | platform | 6 | listContracts, createContractDraft, submitContract, listContractApprovals, listSpaceRentalStatus, addContractAttachments | 已在权限矩阵正文出现 |
| `contract.lifecycle.activate` | staff-token | platform | 1 | activateContract | OpenAPI 补充，以本附录为准 |
| `contract.scope_request.raise` | staff-token | platform | 1 | requestContractScopeGrant | OpenAPI 补充，以本附录为准 |
| `contract.settlement.dispute.resolve` | staff-token | platform | 1 | resolveContractDispute | OpenAPI 补充，以本附录为准 |
| `contract.settlement.manage` | staff-token | platform | 4 | settleContract, listContractSettlements, completeContractSettlement, advanceContractSettlement | OpenAPI 补充，以本附录为准 |
| `contract.space.color.manage` | staff-token | platform | 2 | listSpaceExpiryColorRules, saveSpaceExpiryColorRule | OpenAPI 补充，以本附录为准 |
| `contract.view` | staff-token | merchant, platform | 4 | listMerchantPortalContracts, listVacantSpaces, listExpiringContracts, getRentToSales | 已在权限矩阵正文出现 |
| `customer.account.manage` | staff-token | platform | 4 | lookupCustomerAccountByPhone, suspendCustomerAccount, blacklistCustomerAccount, reinstateCustomerAccount | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.evaluate` | staff-token | project | 1 | evaluateMemberLevel | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.view` | staff-token | project | 1 | listMemberLevels | OpenAPI 补充，以本附录为准 |
| `customer.points.freeze` | staff-token | project | 2 | freezePointsAccount, unfreezePointsAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.consume` | customer-token | customer | 1 | customerStoredValueConsume | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.freeze` | staff-token | project | 2 | freezeStoredValueAccount, unfreezeStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.refund` | staff-token | merchant, platform, project | 7 | refundStoredValueAccount, configureStoredValueMerchantRefundRule, getStoredValueMerchantRefundRule, configureStoredValueMerchantCard, configureStoredValueBonusPolicy, getStoredValueBonusPolicy, counterRefundStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.topup` | customer-token | customer | 2 | customerStoredValueTopup, customerStoredValueSettle | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.view` | customer-token | customer | 2 | customerStoredValueAccount, customerStoredValueAccounts | OpenAPI 补充，以本附录为准 |
| `customer.voucher.manage` | staff-token | merchant | 3 | createVoucherDefinition, issueVouchers, revokeVoucher | OpenAPI 补充，以本附录为准 |
| `customer.voucher.redeem` | staff-token | merchant | 1 | redeemVoucher | OpenAPI 补充，以本附录为准 |
| `customer.voucher.view` | customer-token, staff-token | customer, merchant | 5 | customerVoucherWallet, listVoucherDefinitions, listVoucherInstances, listVoucherRedemptions, listVoucherOverview | OpenAPI 补充，以本附录为准 |
| `dealer_portal.inquiry.manage` | staff-token | dealer | 2 | dealerPortalListInquiries, dealerPortalCreateInquiry | OpenAPI 补充，以本附录为准 |
| `dealer_portal.order.manage` | staff-token | dealer | 9 | dealerPortalListOrders, dealerPortalCreateOrder, dealerPortalGetOrder, dealerPortalListOrderLines, dealerPortalSubmitOrder, dealerPortalSignOrder, getDealerPortalDelivery, uploadDealerDeliveryEvidence, readDealerDeliveryEvidence | OpenAPI 补充，以本附录为准 |
| `dealer_portal.order.pay` | staff-token | dealer | 2 | dealerPortalListPayments, dealerPortalRegisterPayment | OpenAPI 补充，以本附录为准 |
| `dealer_portal.profile.submit` | staff-token | dealer | 1 | dealerPortalSubmitProfile | OpenAPI 补充，以本附录为准 |
| `dealer_portal.profile.view` | staff-token | dealer | 2 | dealerPortalMe, dealerPortalListContracts | OpenAPI 补充，以本附录为准 |
| `dealer_portal.return.request` | staff-token | dealer | 2 | dealerPortalListReturns, dealerPortalRequestReturn | OpenAPI 补充，以本附录为准 |
| `dealer_portal.stock.view` | staff-token | dealer | 1 | dealerPortalExternalStock | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.approve` | staff-token | project | 1 | decideDealerInquiryQuote | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.manage` | staff-token | project | 1 | createDealerInquiry | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.quote` | staff-token | project | 2 | quoteDealerInquiry, expireDealerInquiry | OpenAPI 补充，以本附录为准 |
| `dealer.invoice.manage` | staff-token | project | 3 | reverseDealerInvoice, requestDealerInvoice, issueDealerInvoice | OpenAPI 补充，以本附录为准 |
| `dealer.manage` | staff-token | project | 5 | createDealer, submitDealer, deactivateDealer, createDealerContract, updateDealerContractTerms | OpenAPI 补充，以本附录为准 |
| `dealer.order.manage` | staff-token | project, project-merchant | 6 | createDealerOrderDraft, submitDealerOrder, registerDealerOrderPayment, signDealerOrder, requestDealerOrderCancel, requestDealerDeliveryResolution | OpenAPI 补充，以本附录为准 |
| `dealer.order.payment.verify` | staff-token | project | 3 | verifyDealerOrderPayment, openDealerOrderRefundApproval, confirmDealerOverpaymentUse | OpenAPI 补充，以本附录为准 |
| `dealer.order.refund.execute` | staff-token | project | 1 | executeDealerOrderRefund | OpenAPI 补充，以本附录为准 |
| `dealer.order.review` | staff-token | project | 2 | reviewDealerOrder, decideDealerOrderCancel | OpenAPI 补充，以本附录为准 |
| `dealer.order.ship` | staff-token | project, project-merchant | 3 | shipDealerOrder, completeDealerDeliveryPhysical, shipDealerReplacement | OpenAPI 补充，以本附录为准 |
| `dealer.order.view` | staff-token | project, project-merchant | 7 | listDealerOrders, listDealerOverpayments, getDealerOrder, listDealerOrderLines, listDealerOrderPayments, listDealerOrderRefunds, getDealerDelivery | OpenAPI 补充，以本附录为准 |
| `dealer.portal_account.manage` | staff-token | project | 2 | listDealerPortalAccounts, createDealerPortalAccount | OpenAPI 补充，以本附录为准 |
| `dealer.price_catalog.approve` | staff-token | project | 1 | decideDealerPriceCatalog | OpenAPI 补充，以本附录为准 |
| `dealer.price_catalog.edit` | staff-token | project | 2 | createDealerPriceCatalogDraft, submitDealerPriceCatalog | OpenAPI 补充，以本附录为准 |
| `dealer.return.inspect` | staff-token | project | 2 | receiveDealerReturn, inspectDealerReturn | OpenAPI 补充，以本附录为准 |
| `dealer.return.manage` | staff-token | project | 2 | requestDealerReturn, confirmDealerReturnFreight | OpenAPI 补充，以本附录为准 |
| `dealer.return.refund.execute` | staff-token | project | 1 | recordDealerReturnRefundResult | OpenAPI 补充，以本附录为准 |
| `dealer.return.review` | staff-token | project | 1 | decideDealerReturn | OpenAPI 补充，以本附录为准 |
| `dealer.return.view` | staff-token | project | 4 | listDealerReturns, getDealerReturn, listDealerReturnLines, listDealerInvoices | OpenAPI 补充，以本附录为准 |
| `dealer.review` | staff-token | project | 1 | reviewDealer | OpenAPI 补充，以本附录为准 |
| `dealer.view` | staff-token | project | 7 | listDealers, listDealerContracts, listDealerPriceCatalogs, listDealerExternalStock, listDealerPriceCatalogLines, listDealerInquiries, getDealerAdmission | OpenAPI 补充，以本附录为准 |
| `energy.meter.manage` | staff-token | platform | 3 | listEnergyMeters, createEnergyMeter, updateEnergyMeter | OpenAPI 补充，以本附录为准 |
| `energy.reading.manage` | staff-token | platform | 7 | listEnergyReadings, recordEnergyReading, reviewEnergyReading, getEnergyMeterDifference, settleEnergyFee, settleEnergyFeeShortcut, listAbnormalReadings | OpenAPI 补充，以本附录为准 |
| `entity.binding.manage` | staff-token | project-merchant | 3 | createBusinessEntity, createBusinessEntityBinding, changeBusinessEntityBinding | 已在权限矩阵正文出现 |
| `entity.binding.view` | staff-token | project-merchant | 2 | listBusinessEntities, listBusinessEntityBindings | 已在权限矩阵正文出现 |
| `evidence.attachment.create` | staff-token | merchant, project-object | 4 | createEvidenceAttachmentUploadIntent, bindEvidenceAttachment, completeEvidenceAttachmentUpload, uploadEvidenceAttachment | 已在权限矩阵正文出现 |
| `evidence.event.create` | staff-token | project-device | 1 | createEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.event.view` | staff-token | project-object | 1 | listEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.review.create` | staff-token | project-object | 1 | createEvidenceReview | 已在权限矩阵正文出现 |
| `evidence.timeline.view` | staff-token | merchant, project-object | 3 | getEvidenceTimeline, getTraceabilityAutoCaptureRate, readEvidenceAttachmentFile | 已在权限矩阵正文出现 |
| `facility.equipment.manage` | staff-token | platform | 6 | listFacilityEquipment, createFacilityEquipment, listFacilityMaintenanceDueEquipment, updateFacilityEquipment, changeFacilityEquipmentStatus, recordFacilityEquipmentMaintenanceDone | OpenAPI 补充，以本附录为准 |
| `facility.inspection.manage` | staff-token | platform | 10 | listFacilityInspectionTemplates, createFacilityInspectionTemplate, listFacilityInspectionTasks, createFacilityInspectionTask, getFacilityInspectionTask, startFacilityInspectionTask, listFacilityInspectionRecords, submitFacilityInspectionRecord, recheckFacilityInspectionTask, confirmFacilityInspectionSupervisor | OpenAPI 补充，以本附录为准 |
| `facility.inspection.report_exception_assigned` | staff-token | self | 1 | reportInspectionException | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.accept` | staff-token | platform | 1 | acceptSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.create_assigned` | staff-token | self | 1 | createSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.review` | staff-token | platform | 1 | reviewSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.submit_assigned` | staff-token | self | 1 | submitSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.upload_completion_assigned` | staff-token | project-assignee, self | 2 | uploadSiteEntryCompletion, uploadSiteEntryAttachment | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.view_assigned` | staff-token | project-assignee, self | 4 | listSiteEntryReports, getSiteEntryReport, listSiteEntryHistory, readSiteEntryAttachment | OpenAPI 补充，以本附录为准 |
| `facility.work_order.manage` | staff-token | platform | 14 | listFacilityWorkOrders, createFacilityWorkOrder, listFacilityOverdueWorkOrders, getFacilityWorkOrder, assignFacilityWorkOrder, startFacilityWorkOrder, completeFacilityWorkOrder, acceptFacilityWorkOrder, reviewFacilityWorkOrder, suspendFacilityWorkOrder, resumeFacilityWorkOrder, markFacilityWorkOrdersOverdue ... | OpenAPI 补充，以本附录为准 |
| `facility.work_order.view_assigned` | staff-token | platform | 1 | listWorkOrdersAssignedToMe | OpenAPI 补充，以本附录为准 |
| `finance.ar_ap.export` | staff-token | project-entity | 1 | exportArApItems | OpenAPI 补充，以本附录为准 |
| `finance.ar_ap.manage` | staff-token | platform, project-entity | 3 | createArApItem, generateContractRentBill, generateContractPeriodBills | 已在权限矩阵正文出现 |
| `finance.ar_ap.view` | staff-token | platform, project-entity, project-order | 6 | listArApItems, listArApPayments, listFinanceRefunds, listArOverdueDebtors, getFinanceRefund, queryFinanceRefund | 已在权限矩阵正文出现 |
| `finance.daily_close.lock` | staff-token | project-entity | 1 | createDailyClose | 已在权限矩阵正文出现 |
| `finance.daily_close.manage` | staff-token | project-entity | 1 | listDailyCloseSummaries | OpenAPI 补充，以本附录为准 |
| `finance.deposit.manage` | staff-token | platform | 1 | recordDepositAction | OpenAPI 补充，以本附录为准 |
| `finance.dividend.manage` | staff-token | platform | 8 | createDividendPlan, updateDividendPlan, computeDividendStatements, submitDividendStatement, approveDividendStatement, rejectDividendStatement, markDividendStatementPaid, releaseDividendReserve | OpenAPI 补充，以本附录为准 |
| `finance.dividend.view` | staff-token | merchant | 4 | listDividendPlans, listDividendStatements, getDividendStatement, listDividendReserveReleases | OpenAPI 补充，以本附录为准 |
| `finance.inventory_variance.post` | staff-token | platform | 1 | postInventoryVariance | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.approve` | staff-token | project-entity | 1 | approveArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.request` | staff-token | project-entity | 1 | requestArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.merchant_expense.manage` | staff-token | merchant, platform | 3 | createMerchantExpenseEntry, reverseMerchantExpenseEntry, syncMerchantExpenseAuto | OpenAPI 补充，以本附录为准 |
| `finance.merchant_expense.view` | staff-token | merchant | 3 | listMerchantExpenses, summarizeMerchantExpenses, listMerchantExpensePeriodLocks | OpenAPI 补充，以本附录为准 |
| `finance.payment_intent.create` | staff-token | project-bar-table, project-order | 3 | createBarBillPayIntent, createPaymentIntent, createTicketOrderPayIntent | 已在权限矩阵正文出现 |
| `finance.payment.register` | staff-token | platform, project-bar-table, project-entity, project-order | 4 | settleBarBill, createArApPayment, confirmOfflinePayment, confirmTicketOrderPayment | 已在权限矩阵正文出现 |
| `finance.property_fee.manage` | staff-token | platform | 3 | listPropertyFeeRates, createPropertyFeeRate, billPropertyFee | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.difference.create` | staff-token | platform | 1 | createFinanceReconciliationDifference | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.resolve` | staff-token | project-entity-channel | 1 | closeFinanceReconciliationDifference | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.view` | staff-token | project, project-entity-channel | 2 | listReconciliationBatches, listReconciliationDifferences | 已在权限矩阵正文出现 |
| `finance.refund.create` | staff-token | project-order | 4 | createFinanceRefund, retryFinanceRefund, recordFinanceRefundExecution, confirmFinanceRefundResult | 已在权限矩阵正文出现 |
| `finance.turnover_rent.bill` | staff-token | platform | 1 | billTurnoverRent | OpenAPI 补充，以本附录为准 |
| `floor_plan.import.manage` | staff-token | project-space | 2 | createFloorPlanImport, publishFloorPlanImport | OpenAPI 补充，以本附录为准 |
| `floor_plan.import.view` | staff-token | project-space | 2 | listFloorPlanImports, getFloorPlanImportDetails | OpenAPI 补充，以本附录为准 |
| `floor_plan.manage` | staff-token | -, project-space | 2 | createFloorPlan, saveFloorPlanShape | OpenAPI 补充，以本附录为准 |
| `floor_plan.view` | staff-token | project-space | 2 | listFloorPlans, listFloorPlanShapes | OpenAPI 补充，以本附录为准 |
| `governance.project.manage` | staff-token | project | 1 | createProject | 已在权限矩阵正文出现 |
| `governance.project.view` | staff-token | project | 1 | listProjects | 已在权限矩阵正文出现 |
| `governance.space.manage` | staff-token | platform, project | 3 | createSpace, saveVenueBusinessHours, createSpaceType | 已在权限矩阵正文出现 |
| `governance.space.view` | staff-token | platform, project, project-merchant-space | 3 | listSpaces, listVenueBusinessHours, listSpaceTypes | 已在权限矩阵正文出现 |
| `governance.supervisor_review.approve` | staff-token | project-entity | 1 | approveSupervisorReview | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.manage` | staff-token | project-entity | 1 | evaluateSupervisorReview | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.view` | staff-token | project-entity | 1 | listSupervisorReviewPolicies | OpenAPI 补充，以本附录为准 |
| `integration.channel_store_map.manage` | staff-token | platform | 2 | saveIntegrationChannelTicketTypeMap, archiveIntegrationChannelTicketTypeMap | OpenAPI 补充，以本附录为准 |
| `integration.channel_store_map.view` | staff-token | platform | 1 | listIntegrationChannelTicketTypeMaps | OpenAPI 补充，以本附录为准 |
| `integration.compensation.manage` | staff-token | platform | 2 | createCompensationTask, resolveCompensationTask | OpenAPI 补充，以本附录为准 |
| `integration.order_sync.manage` | staff-token | platform | 1 | createOrderSync | OpenAPI 补充，以本附录为准 |
| `integration.provider_adapter.create` | staff-token | project-provider | 1 | createProviderAdapter | 已在权限矩阵正文出现 |
| `integration.provider_adapter.production_ready` | staff-token | project-provider | 1 | markProviderAdapterProductionReady | 已在权限矩阵正文出现 |
| `integration.provider_adapter.test` | staff-token | project-provider | 1 | testProviderAdapterMock | 已在权限矩阵正文出现 |
| `integration.provider_adapter.update` | staff-token | project-provider | 1 | updateProviderAdapter | 已在权限矩阵正文出现 |
| `integration.provider_adapter.view` | staff-token | project, project-provider | 16 | listIntegrationCompensationTasks, listIntegrationDevices, listIntegrationDeviceEvents, listIntegrationOfflineSyncBatches, listIntegrationOrderSyncs, listProviderAdapters, listIntegrationRetailSkus, listIntegrationRetryTasks, listRobotTaskEvents, listRobotTasks, listVideoCameras, listVoucherVerifications ... | 已在权限矩阵正文出现 |
| `integration.reconciliation.manage` | staff-token | platform | 3 | createIntegrationReconciliationBatch, createIntegrationReconciliationDifference, runChannelAutoReconciliation | OpenAPI 补充，以本附录为准 |
| `integration.reconciliation.resolve` | staff-token | platform | 1 | resolveIntegrationReconciliationDifference | OpenAPI 补充，以本附录为准 |
| `integration.reconciliation.view` | staff-token | platform | 2 | listIntegrationReconciliationBatches, listIntegrationReconciliationDifferences | OpenAPI 补充，以本附录为准 |
| `integration.retry.manage` | staff-token | platform | 3 | createRetryTask, runRetryTaskNow, recordIntegrationPollingCheck | OpenAPI 补充，以本附录为准 |
| `integration.video.view` | staff-token | project | 1 | listIntegrationVideoAnchors | OpenAPI 补充，以本附录为准 |
| `inventory.cost.view` | staff-token | project-warehouse | 2 | listInventoryCostLedger, getInventoryCostSummary | OpenAPI 补充，以本附录为准 |
| `inventory.handover.review` | staff-token | project-warehouse-bar | 1 | reviewTraceHandoverException | OpenAPI 补充，以本附录为准 |
| `inventory.handover.scan` | staff-token | project-warehouse-bar | 4 | scanTraceCodeHandover, createTraceHandover, scanTraceSegment, reportRobotSegment | OpenAPI 补充，以本附录为准 |
| `inventory.high_value.review` | staff-token | project-warehouse-bar | 2 | listHighValueReviews, reviewHighValue | OpenAPI 补充，以本附录为准 |
| `inventory.purchase_request.manage` | staff-token | project | 1 | createPurchaseRequest | 已在权限矩阵正文出现 |
| `inventory.purchase_request.view` | staff-token | project | 1 | listPurchaseRequests | 已在权限矩阵正文出现 |
| `inventory.purchase.approve` | staff-token | project-warehouse-bar | 5 | approvePurchaseRequest, orderPurchaseRequest, arrivePurchaseRequest, receivePurchaseRequest, approveSupplyContract | OpenAPI 补充，以本附录为准 |
| `inventory.risk_alert.view` | staff-token | warehouse | 1 | listInventoryRiskAlerts | OpenAPI 补充，以本附录为准 |
| `inventory.risk.scan` | staff-token | warehouse | 1 | scanInventoryRiskAlerts | OpenAPI 补充，以本附录为准 |
| `inventory.stock.adjust` | staff-token | project-merchant, project-warehouse | 3 | createStockAdjustment, requestQuarantineDisposition, completeQuarantineDisposition | 已在权限矩阵正文出现 |
| `inventory.stock.batch.manage` | staff-token | warehouse | 1 | createInventoryStockBatch | OpenAPI 补充，以本附录为准 |
| `inventory.stock.move` | staff-token | project, project-warehouse | 5 | createInventoryItem, createStockIn, createStockMovement, transferStockMovement, createTypedStockMovement | 已在权限矩阵正文出现 |
| `inventory.stock.view` | staff-token | project, project-merchant, project-warehouse, warehouse | 7 | listInventoryItems, listStockBalances, listStockMovements, listInventoryStockBatches, listQuarantineWarehouses, listInventoryQuarantine, listQuarantineDispositions | 已在权限矩阵正文出现 |
| `inventory.stocktake.create` | staff-token | project | 1 | createInventoryStocktake | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.variance_record` | staff-token | project | 1 | recordInventoryStocktakeVariance | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.variance_review` | staff-token | project | 1 | reviewInventoryStocktakeVariance | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.view` | staff-token | project | 2 | listInventoryStocktakes, getInventoryStocktake | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.manage` | staff-token | project | 1 | createInventorySupplier | 已在权限矩阵正文出现 |
| `inventory.supplier.qualification.manage` | staff-token | supplier | 2 | listSupplierQualifications, createSupplierQualification | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.reconcile` | staff-token | supplier | 2 | listSupplierReconciliations, createSupplierReconciliation | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.settlement.manage` | staff-token | supplier | 2 | listSupplierSettlementMethods, createSupplierSettlementMethod | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.view` | staff-token | project | 1 | listInventorySuppliers | 已在权限矩阵正文出现 |
| `inventory.supply_contract.manage` | staff-token | project-warehouse-bar, supplier | 2 | listSupplyContracts, createSupplyContract | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.print` | staff-token | project | 1 | printInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.register` | staff-token | project | 1 | registerInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.view` | staff-token | project | 2 | listInventoryTraceCodes, getInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.transfer_order.manage` | staff-token | project | 2 | returnTransferOrder, cancelTransferOrder | OpenAPI 补充，以本附录为准 |
| `inventory.warehouse.manage` | staff-token | project-warehouse-bar, warehouse | 2 | listWarehouses, createWarehouse | OpenAPI 补充，以本附录为准 |
| `leasing.brand.blacklist` | staff-token | platform | 1 | blacklistLeasingBrand | OpenAPI 补充，以本附录为准 |
| `leasing.brand.manage` | staff-token | platform | 2 | listLeasingBrands, createLeasingBrand | OpenAPI 补充，以本附录为准 |
| `leasing.convert` | staff-token | platform | 1 | signLeasingIntent | OpenAPI 补充，以本附录为准 |
| `leasing.intent.manage` | staff-token | platform | 3 | listLeasingIntents, createLeasingIntent, advanceLeasingIntent | OpenAPI 补充，以本附录为准 |
| `leasing.lead.manage` | staff-token | platform | 8 | listLeasingLeads, createLeasingLead, listLeasingLeadsFollowUpDue, assignLeasingLead, updateLeasingLeadStatus, getLeasingLeadContactPhone, listLeasingFollowUps, createLeasingFollowUp | OpenAPI 补充，以本附录为准 |
| `mdm.device.manage` | staff-token | -, project-device | 3 | enrollMdmDevice, reportMdmPolicyViolation, detectMdmViolations | OpenAPI 补充，以本附录为准 |
| `mdm.policy.manage` | staff-token | project-device | 4 | listMdmAppWhitelist, upsertMdmAppWhitelist, listMdmSiteWhitelist, upsertMdmSiteWhitelist | OpenAPI 补充，以本附录为准 |
| `mdm.violation.handle` | staff-token | - | 1 | handleMdmViolation | OpenAPI 补充，以本附录为准 |
| `mdm.violation.view` | staff-token | project-device | 1 | listMdmPolicyViolations | OpenAPI 补充，以本附录为准 |
| `merchant.billing.confirm` | staff-token | - | 1 | confirmMerchantBill | OpenAPI 补充，以本附录为准 |
| `merchant.billing.dispute.submit` | staff-token | - | 1 | submitMerchantBillDispute | OpenAPI 补充，以本附录为准 |
| `merchant.billing.dunning.record` | staff-token | - | 1 | recordMerchantBillDunning | OpenAPI 补充，以本附录为准 |
| `merchant.billing.manage` | staff-token | - | 1 | createMerchantBill | OpenAPI 补充，以本附录为准 |
| `merchant.billing.view` | staff-token | project-merchant | 1 | listMerchantBills | OpenAPI 补充，以本附录为准 |
| `merchant.portal.settlement.dispute` | staff-token | merchant | 1 | submitMerchantPortalSettlementDispute | OpenAPI 补充，以本附录为准 |
| `merchant.portal.settlement.view` | staff-token | merchant | 1 | listMerchantPortalSettlements | OpenAPI 补充，以本附录为准 |
| `merchant.profile.manage` | staff-token | project, project-merchant | 2 | createMerchant, updateMerchant | 已在权限矩阵正文出现 |
| `merchant.profile.view` | staff-token | project-merchant | 1 | listMerchants | 已在权限矩阵正文出现 |
| `merchant.service_work_order.assign` | staff-token | project-merchant | 1 | assignMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.close` | staff-token | project-merchant | 1 | closeMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.create` | staff-token | project-merchant | 1 | createMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.process` | staff-token | project-merchant | 1 | addMerchantServiceWorkOrderAction | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.view` | staff-token | project-merchant | 1 | listMerchantServiceWorkOrders | OpenAPI 补充，以本附录为准 |
| `not_applicable` | customer-token, device-credential, open-app-credential, public, staff-token, webhook-signature | customer, customer-project-bar, open-endpoint, platform, project-device, public-project-bar, self | 43 | getOpenEventResult, receiveOpenWebhook, getCustomerSession, listCustomerBarReservationOptions, createCustomerBarReservation, confirmCustomerBarInvitation, getCustomerBarInvitation, createCustomerConsent, listCustomerOrders, getCustomerPoints, createCustomerTicketOrder, getCustomerTicketQr ... | 已在权限矩阵正文出现 |
| `ops.production_readiness.check` | staff-token | project | 1 | createProductionReadinessCheck | 已在权限矩阵正文出现 |
| `ops.production_readiness.manage` | staff-token | project | 1 | updateProductionReadinessItem | 已在权限矩阵正文出现 |
| `ops.production_readiness.view` | staff-token | project | 1 | listProductionReadinessItems | 已在权限矩阵正文出现 |
| `partner.org.manage` | staff-token | platform | 3 | createPartnerOrg, bindUserToPartnerOrg, unbindUserFromPartnerOrg | OpenAPI 补充，以本附录为准 |
| `partner.org.view` | staff-token | platform | 1 | listPartnerOrgs | OpenAPI 补充，以本附录为准 |
| `partner.subordinate.manage` | staff-token | self | 2 | createPartnerSubordinate, disablePartnerSubordinate | OpenAPI 补充，以本附录为准 |
| `partner.subordinate.view` | staff-token | self | 2 | listPartnerSubordinates, listSubordinateCompletion | OpenAPI 补充，以本附录为准 |
| `platform.notification_window.manage` | staff-token | platform | 1 | upsertNotificationWindow | OpenAPI 补充，以本附录为准 |
| `public` | public | customer, none, public-project, self | 7 | login, customerPhoneLogin, customerLogout, listCustomerTicketProducts, getSystemHealth, listStaffRegistrationCompanies, submitStaffSelfRegistration | 已在权限矩阵正文出现 |
| `refund.authority.manage` | staff-token | project | 2 | upsertRefundAuthorityLimit, upsertRefundAutoRule | OpenAPI 补充，以本附录为准 |
| `refund.authority.view` | staff-token | project | 2 | listRefundAuthorityLimits, listRefundAutoRules | OpenAPI 补充，以本附录为准 |
| `risk_alert.claim` | staff-token | platform | 1 | claimRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.dismiss` | staff-token | platform | 1 | dismissRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.escalate` | staff-token | platform | 1 | escalateRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.report` | staff-token | platform | 1 | reportRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.resolve` | staff-token | platform | 1 | resolveEvidenceRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.transfer` | staff-token | platform | 1 | transferRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.view` | staff-token | platform | 2 | searchRiskAlerts, getRiskAlertDetail | 已在权限矩阵正文出现 |
| `service.catalog.manage` | staff-token | platform | 4 | upsertServiceCatalogItem, listServiceCatalogAll, listServiceOrders, setServicePayConfig | OpenAPI 补充，以本附录为准 |
| `service.order.create` | staff-token | platform | 7 | listServiceCatalog, createServiceOrder, completeServiceOrder, cancelServiceOrder, serviceOrderPaymentCode, markServiceOrderPaid, getServicePayConfig | OpenAPI 补充，以本附录为准 |
| `service.order.perform` | staff-token | platform | 5 | setMyServicePaymentCode, myServicePaymentCode, listMyServiceOrders, acceptServiceOrder, myServiceStats | OpenAPI 补充，以本附录为准 |
| `settlement.bill.post` | staff-token | platform | 1 | settlementPostBill | OpenAPI 补充，以本附录为准 |
| `settlement.bill.special_approve` | staff-token | platform | 1 | settlementSpecialApproveBill | OpenAPI 补充，以本附录为准 |
| `settlement.config.manage` | staff-token | platform | 17 | settlementConfigUpsertRetention, settlementConfigUpsertDistributionMethod, settlementConfigUpsertMerchantRate, settlementConfigUpsertComprehensiveServiceFee, settlementConfigUpsertComboDiscount, settlementConfigUpsertChannelFee, settlementConfigUpsertCommissionRate, settlementConfigUpsertPointsAccrualRate, settlementConfigUpsertRankTiers, settlementConfigUpsertRevenueStreamAttribution, settlementConfigActivateRankTiers, settlementConfigActivateDraft ... | OpenAPI 补充，以本附录为准 |
| `settlement.config.view` | staff-token | platform | 2 | settlementConfigOverview, settlementConfigListInternalPriceItems | OpenAPI 补充，以本附录为准 |
| `settlement.invoice.manage` | staff-token | platform | 2 | generateSettlementInvoiceItems, changeSettlementInvoiceItemStatus | OpenAPI 补充，以本附录为准 |
| `settlement.invoice.view` | staff-token | platform | 2 | listSettlementInvoiceItems, settlementInvoiceSummary | OpenAPI 补充，以本附录为准 |
| `staff.attendance.punch` | staff-token | platform, self | 4 | punchStaffAttendance, issueStaffPhoneTimeAnchor, recordStaffPhoneReceipt, getCurrentStaffWorkPhoneContext | OpenAPI 补充，以本附录为准 |
| `staff.attendance.view` | staff-token | platform | 1 | listStaffAttendance | OpenAPI 补充，以本附录为准 |
| `staff.device.bind` | device-credential, staff-token | project | 2 | bindStaffDeviceCredential, reportStaffDeviceIntegrity | OpenAPI 补充，以本附录为准 |
| `staff.device.review` | staff-token | project | 1 | approveStaffDeviceRebind | OpenAPI 补充，以本附录为准 |
| `staff.device.revoke` | staff-token | project | 1 | revokeStaffDeviceCredential | OpenAPI 补充，以本附录为准 |
| `staff.leave.submit` | staff-token | platform | 2 | submitStaffLeave, listMyStaffLeaves | OpenAPI 补充，以本附录为准 |
| `staff.notice.view` | staff-token | project | 2 | listEmployeeNotices, acknowledgeEmployeeNotice | OpenAPI 补充，以本附录为准 |
| `staff.payroll.manage` | staff-token | platform, project | 10 | listPayrollTaxBrackets, setStaffBaseSalary, addStaffPayrollAdjustment, addStaffCommission, closeStaffPayrollMonth, closeStaffCommissionSettlement, listStaffCommissionSettlements, listStaffPayroll, getStaffPenaltyCap, requestStaffPenaltyCap | OpenAPI 补充，以本附录为准 |
| `staff.payroll.view_mine` | staff-token | platform, self | 3 | listStaffAdjustmentAppeals, submitStaffAdjustmentAppeal, listMyStaffPayroll | OpenAPI 补充，以本附录为准 |
| `staff.profile.review` | staff-token | project | 7 | reviewStaffRegistration, setStaffSalesPosition, createStaffDispatch, endStaffDispatch, setStaffEmploymentStatus, getStaffBusinessHandover, assignStaffBusinessHandover | OpenAPI 补充，以本附录为准 |
| `staff.profile.self` | staff-token | self | 1 | issueStaffLockerScanToken | OpenAPI 补充，以本附录为准 |
| `staff.profile.submit` | staff-token | project | 1 | submitStaffRegistration | OpenAPI 补充，以本附录为准 |
| `staff.profile.view` | staff-token | project | 2 | listStaffProfiles, listStaffDispatches | OpenAPI 补充，以本附录为准 |
| `staff.shift.manage` | staff-token | platform, project, project-merchant, self-project | 13 | listStaffShifts, upsertStaffShift, batchUpsertStaffShifts, listStaffLeaves, reviewStaffLeave, getStaffShiftVisibility, setStaffShiftVisibility, listStaffShiftHistory, coverStaffShift, listStaffShiftCovers, submitStaffAttendanceReceiptReview, recoverStaffWorkPhoneReceipt ... | OpenAPI 补充，以本附录为准 |
| `staff.shift.view` | staff-token | platform, self-project | 5 | listMyStaffShifts, listMyStaffAttendance, myStaffAttendanceMonthView, listStaffPhoneReceipts, listStaffAttendanceReceiptReviews | OpenAPI 补充，以本附录为准 |
| `staff.tax_bracket.manage` | staff-token | platform | 1 | upsertPayrollTaxBracket | OpenAPI 补充，以本附录为准 |
| `system.version.view` | staff-token | project | 1 | getSystemVersion | 已在权限矩阵正文出现 |
| `terminal.device.bind` | staff-token | project, project-device | 5 | bindDevice, returnTerminalDevice, getTerminalOfflineEntitlements, syncTerminalTasks, registerTerminalDeviceSigningKey | 已在权限矩阵正文出现 |
| `terminal.device.lifecycle` | staff-token | -, project-device | 5 | updateTerminalDeviceMerchant, markTerminalDeviceFound, markTerminalDeviceLost, markTerminalDeviceRepair, markTerminalDeviceScrap | OpenAPI 补充，以本附录为准 |
| `terminal.device.unbind` | staff-token | project-device | 1 | unbindDevice | 已在权限矩阵正文出现 |
| `terminal.switch.manage` | staff-token | project-device | 4 | upsertTerminalCanaryRollout, addTerminalCanaryTarget, archiveTerminalCanaryTarget, updateCapabilitySwitch | 已在权限矩阵正文出现 |
| `terminal.switch.view` | staff-token | project-device | 3 | listTerminalCanaryRollouts, listCapabilitySwitches, listTerminalDevices | 已在权限矩阵正文出现 |
| `terminal.work_phone.swap` | staff-token | project-merchant | 1 | swapStaffWorkPhone | OpenAPI 补充，以本附录为准 |
| `ticket.credential.view` | staff-token | project-order-customer | 1 | getTicketCredential | 已在权限矩阵正文出现 |
| `ticket.dynamic_pricing.manage` | staff-token | project-channel | 1 | createTicketDynamicPricingRule | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.publish` | staff-token | project-channel | 1 | publishTicketDynamicPricingRule | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.simulate` | staff-token | project-channel | 1 | simulateTicketDynamicPricing | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.view` | staff-token | project-channel | 1 | listTicketDynamicPricingRules | OpenAPI 补充，以本附录为准 |
| `ticket.entitlement.approve` | staff-token | project | 1 | approveDeleteEntitlementTemplate | OpenAPI 补充，以本附录为准 |
| `ticket.entitlement.manage` | staff-token | project | 4 | createEntitlementTemplate, archiveEntitlementTemplate, unarchiveEntitlementTemplate, requestDeleteEntitlementTemplate | 已在权限矩阵正文出现 |
| `ticket.entitlement.view` | staff-token | project | 1 | listEntitlementTemplates | 已在权限矩阵正文出现 |
| `ticket.order.create` | staff-token | platform, project-channel | 2 | createTicketOrder, cancelTicketOrder | 已在权限矩阵正文出现 |
| `ticket.order.read` | staff-token | project | 1 | listTicketOrders | OpenAPI 补充，以本附录为准 |
| `ticket.points.billing.manage` | staff-token | platform | 4 | submitTicketPointsBillingRule, submitTicketPointsCalendar, submitTicketPointsOutage, confirmTicketPointsBillUnchanged | OpenAPI 补充，以本附录为准 |
| `ticket.points.billing.view` | staff-token | platform, project-merchant | 8 | listTicketPointsBillingRules, listTicketPointsCalendars, listTicketPointsOutages, listTicketPointsBills, listTicketPointsBillItems, listTicketPointsBillObjections, submitTicketPointsBillObjection, listTicketPointsBillScheduleAdjustments | OpenAPI 补充，以本附录为准 |
| `ticket.price_variant.manage` | staff-token | project-channel | 1 | createTicketPriceRule | 已在权限矩阵正文出现 |
| `ticket.price_variant.view` | staff-token | project-channel | 1 | listTicketPriceRules | 已在权限矩阵正文出现 |
| `ticket.product.manage` | staff-token | platform, project | 5 | updateTicketQrRefreshConfig, createTicketType, bindTicketTypeEntitlement, publishTicketType, updateTicketTypePricing | 已在权限矩阵正文出现 |
| `ticket.product.view` | staff-token | platform, project | 4 | getTicketQrRefreshConfig, listTicketTypes, listTicketTypeGiftCoverage, ticketTypePriceCheck | 已在权限矩阵正文出现 |
| `ticket.refund.approve` | staff-token | project-order | 3 | confirmTicketRefundPayout, approveTicketRefund, rejectTicketRefund | 已在权限矩阵正文出现 |
| `ticket.refund.request` | staff-token | project-order | 1 | requestTicketRefund | 已在权限矩阵正文出现 |
| `ticket.refund.view` | staff-token | -, project-order | 2 | listTicketRefunds, listTicketRefundLines | OpenAPI 补充，以本附录为准 |
| `ticket.reward.config.manage` | staff-token | project-merchant | 2 | submitTicketRewardConfig, publishTicketRewardConfig | OpenAPI 补充，以本附录为准 |
| `ticket.reward.config.view` | staff-token | project-merchant | 1 | listTicketRewardConfigs | OpenAPI 补充，以本附录为准 |
| `ticket.ticket.issue` | staff-token | project-order | 1 | issueTicket | 已在权限矩阵正文出现 |
| `ticket.transfer.read` | staff-token | project | 1 | listTicketTransfers | OpenAPI 补充，以本附录为准 |
| `ticket.verify.reverse` | staff-token | project-point | 1 | reverseTicketVerification | OpenAPI 补充，以本附录为准 |
| `ticket.verify.view` | staff-token | project | 2 | listTicketInstances, listIssuedEntitlements | 已在权限矩阵正文出现 |
| `verification.execute` | staff-token | project-point | 2 | createTicketVerification, previewTicketVerification | 已在权限矩阵正文出现 |
| `verification.view` | staff-token | project, project-merchant | 2 | getIssuedEntitlementFacePhoto, listTicketVerifications | 已在权限矩阵正文出现 |
| `wecom.retention.manage` | staff-token | platform | 7 | listWecomRetentionPolicies, upsertWecomRetentionPolicy, listWecomCleanupPreviews, previewWecomCleanup, getWecomCleanupPreview, issueWecomCleanupChallenge, executeWecomCleanup | OpenAPI 补充，以本附录为准 |
| `wecom.risk_hit.review` | staff-token | - | 1 | reviewWecomRiskHit | OpenAPI 补充，以本附录为准 |
| `wecom.risk_hit.view` | staff-token | - | 1 | listWecomRiskHits | OpenAPI 补充，以本附录为准 |
| `wecom.sync` | webhook-signature | open-endpoint | 1 | syncWecomConversation | OpenAPI 补充，以本附录为准 |
| `wecom.view_raw` | staff-token | project | 1 | viewWecomConversationMessages | OpenAPI 补充，以本附录为准 |

## 4. OpenAPI 事件字典

> 本节覆盖每个 operation 的 `x-audit-event-success` 和 `x-audit-event-failure`。人工事件证据矩阵保留高风险证据策略；接口级完整事件名以本节为准。实现时不得只更新业务表而不写事件。

| 事件名 | 结果 | operationId | 方法 | 路径 | 建议任务簇 | 权限 | 覆盖状态 |
|---|---|---|---|---|---|---|---|
| `access.approval_line.list_failed` | failure | `listApprovalLines` | `GET` | `/api/v1/access/approval-lines` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.approval_line.listed` | success | `listApprovalLines` | `GET` | `/api/v1/access/approval-lines` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.approval_line.save_failed` | failure | `upsertApprovalLine` | `PUT` | `/api/v1/access/approval-lines/{line_code}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.approval_line.saved` | success | `upsertApprovalLine` | `PUT` | `/api/v1/access/approval-lines/{line_code}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed` | success | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed` | success | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed_failed` | failure | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed_failed` | failure | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `access.grant_request.applied` | success | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.apply` | 接口级事件，以本附录为准 |
| `access.grant_request.apply_failed` | failure | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.apply` | 接口级事件，以本附录为准 |
| `access.grant_request.review_failed` | failure | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | AUTH-账号权限 | `access.grant_request.review` | 接口级事件，以本附录为准 |
| `access.grant_request.reviewed` | success | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | AUTH-账号权限 | `access.grant_request.review` | 接口级事件，以本附录为准 |
| `access.grant_request.summary_view_failed` | failure | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `access.grant_request.summary_viewed` | success | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `access.grant_request.view_failed` | failure | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `access.grant_request.viewed` | success | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `access.position_role_template.delete_failed` | failure | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.deleted` | success | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.save_failed` | failure | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.save_failed` | failure | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.saved` | success | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.saved` | success | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.view_failed` | failure | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.viewed` | success | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role_project_options.view_failed` | failure | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `access.role_project_options.viewed` | success | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `access.role.assign_failed` | failure | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.assigned` | success | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.unassign_failed` | failure | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.unassigned` | success | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.staff_level.assign_failed` | failure | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.assigned` | success | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.viewed` | success | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
| `access.staff_level.viewed_failed` | failure | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
| `access.user_position.assign_failed` | failure | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_position.assigned` | success | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user.changed` | success | `createAccessUser` | `POST` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.manage` | 接口级事件，以本附录为准 |
| `access.user.changed_failed` | failure | `createAccessUser` | `POST` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.manage` | 接口级事件，以本附录为准 |
| `access.user.viewed` | success | `listAccessUsers` | `GET` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `access.user.viewed_failed` | failure | `listAccessUsers` | `GET` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `amusement_group_booking.approved.failure` | failure | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | AMU-游乐安全 | `amusement.group_booking.approve` | 接口级事件，以本附录为准 |
| `amusement_group_booking.approved.success` | success | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | AMU-游乐安全 | `amusement.group_booking.approve` | 接口级事件，以本附录为准 |
| `amusement_group_booking.created.failure` | failure | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.created.success` | success | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.released.failure` | failure | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.released.success` | success | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.viewed.failure` | failure | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.view` | 接口级事件，以本附录为准 |
| `amusement_group_booking.viewed.success` | success | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.view` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.change_failed` | failure | `createAmusementCapacityConfig` | `POST` | `/api/v1/amusement/projects/{id}/capacity-config` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.change_failed` | failure | `disableAmusementCapacityConfig` | `POST` | `/api/v1/amusement/capacity-config/{configId}/disable` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.changed` | success | `createAmusementCapacityConfig` | `POST` | `/api/v1/amusement/projects/{id}/capacity-config` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.changed` | success | `disableAmusementCapacityConfig` | `POST` | `/api/v1/amusement/capacity-config/{configId}/disable` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.view_failed` | failure | `listAmusementCapacityConfigs` | `GET` | `/api/v1/amusement/projects/{id}/capacity-config` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.capacity_config.viewed` | success | `listAmusementCapacityConfigs` | `GET` | `/api/v1/amusement/projects/{id}/capacity-config` | AMU-游乐安全 | `amusement.capacity_config.manage` | 接口级事件，以本附录为准 |
| `amusement.device_registry_backlog.view_failed` | failure | `listAmusementProjectsWithoutDeviceRegistry` | `GET` | `/api/v1/amusement/projects/without-device-registry` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.device_registry_backlog.viewed` | success | `listAmusementProjectsWithoutDeviceRegistry` | `GET` | `/api/v1/amusement/projects/without-device-registry` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.device_status.viewed` | success | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | AMU-游乐安全 | `amusement.device_status.view` | 接口级事件，以本附录为准 |
| `amusement.device_status.viewed_failed` | failure | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | AMU-游乐安全 | `amusement.device_status.view` | 接口级事件，以本附录为准 |
| `amusement.device.change_failed` | failure | `createAmusementDevice` | `POST` | `/api/v1/amusement/projects/{id}/devices` | AMU-游乐安全 | `amusement.device.manage` | 接口级事件，以本附录为准 |
| `amusement.device.changed` | success | `createAmusementDevice` | `POST` | `/api/v1/amusement/projects/{id}/devices` | AMU-游乐安全 | `amusement.device.manage` | 接口级事件，以本附录为准 |
| `amusement.device.view_failed` | failure | `listAmusementDevices` | `GET` | `/api/v1/amusement/projects/{id}/devices` | AMU-游乐安全 | `amusement.device.view` | 接口级事件，以本附录为准 |
| `amusement.device.viewed` | success | `listAmusementDevices` | `GET` | `/api/v1/amusement/projects/{id}/devices` | AMU-游乐安全 | `amusement.device.view` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.claim_failed` | failure | `claimAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions/{id}/claim` | AMU-游乐安全 | `amusement.entitlement_session.claim` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.claimed` | success | `claimAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions/{id}/claim` | AMU-游乐安全 | `amusement.entitlement_session.claim` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.initiate_failed` | failure | `initiateAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions` | AMU-游乐安全 | `amusement.entitlement_session.initiate` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.initiated` | success | `initiateAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions` | AMU-游乐安全 | `amusement.entitlement_session.initiate` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.list_failed` | failure | `listAmusementEntitlementSessions` | `GET` | `/api/v1/amusement/entitlement-sessions` | AMU-游乐安全 | `amusement.entitlement_session.view` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.listed` | success | `listAmusementEntitlementSessions` | `GET` | `/api/v1/amusement/entitlement-sessions` | AMU-游乐安全 | `amusement.entitlement_session.view` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.view_failed` | failure | `getAmusementEntitlementSession` | `GET` | `/api/v1/amusement/entitlement-sessions/{id}` | AMU-游乐安全 | `amusement.entitlement_session.view` | 接口级事件，以本附录为准 |
| `amusement.entitlement_session.viewed` | success | `getAmusementEntitlementSession` | `GET` | `/api/v1/amusement/entitlement-sessions/{id}` | AMU-游乐安全 | `amusement.entitlement_session.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.batch_initiate_failed` | failure | `initiateAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.batch_initiated` | success | `initiateAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.batch_issue_failed` | failure | `confirmAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/compensation-batches/{batch_id}/confirm` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.batch_issued` | success | `confirmAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/compensation-batches/{batch_id}/confirm` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.policy_config_failed` | failure | `configureAmusementCompensationPolicy` | `POST` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.compensation_policy.manage` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.policy_configured` | success | `configureAmusementCompensationPolicy` | `POST` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.compensation_policy.manage` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.view_failed` | failure | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.view_failed` | failure | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.view_failed` | failure | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.viewed` | success | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.viewed` | success | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.viewed` | success | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.incident.action` | success | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | AMU-游乐安全 | `amusement.incident.process` | 接口级事件，以本附录为准 |
| `amusement.incident.action_failed` | failure | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | AMU-游乐安全 | `amusement.incident.process` | 接口级事件，以本附录为准 |
| `amusement.incident.close` | success | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | AMU-游乐安全 | `amusement.incident.close` | 接口级事件，以本附录为准 |
| `amusement.incident.close_failed` | failure | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | AMU-游乐安全 | `amusement.incident.close` | 接口级事件，以本附录为准 |
| `amusement.incident.create` | success | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.create` | 接口级事件，以本附录为准 |
| `amusement.incident.create_failed` | failure | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.create` | 接口级事件，以本附录为准 |
| `amusement.incident.list` | success | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.view` | 接口级事件，以本附录为准 |
| `amusement.incident.list_failed` | failure | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.view` | 接口级事件，以本附录为准 |
| `amusement.inspection_task.viewed` | success | `listAmusementInspectionTasks` | `GET` | `/api/v1/amusement/inspection-tasks` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.inspection_task.viewed_failed` | failure | `listAmusementInspectionTasks` | `GET` | `/api/v1/amusement/inspection-tasks` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.maintenance_record_failed` | failure | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.submit` | 接口级事件，以本附录为准 |
| `amusement.maintenance_recorded` | success | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.submit` | 接口级事件，以本附录为准 |
| `amusement.maintenance_resume_failed` | failure | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | AMU-游乐安全 | `amusement.maintenance.confirm` | 接口级事件，以本附录为准 |
| `amusement.maintenance.view_failed` | failure | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.view_failed` | failure | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.viewed` | success | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.viewed` | success | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.open_eligibility.blocked` | failure | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.open_eligibility.viewed` | success | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.over_capacity_policy.configure_failed` | failure | `configureAmusementOverCapacityPolicy` | `PUT` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | AMU-游乐安全 | `amusement.over_capacity_policy.configure` | 接口级事件，以本附录为准 |
| `amusement.over_capacity_policy.configured` | success | `configureAmusementOverCapacityPolicy` | `PUT` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | AMU-游乐安全 | `amusement.over_capacity_policy.configure` | 接口级事件，以本附录为准 |
| `amusement.over_capacity_policy.view_failed` | failure | `getAmusementOverCapacityPolicy` | `GET` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | AMU-游乐安全 | `amusement.over_capacity_policy.view` | 接口级事件，以本附录为准 |
| `amusement.over_capacity_policy.viewed` | success | `getAmusementOverCapacityPolicy` | `GET` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | AMU-游乐安全 | `amusement.over_capacity_policy.view` | 接口级事件，以本附录为准 |
| `amusement.park_fault.activate_failed` | failure | `confirmAmusementParkWideFaultActivation` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-activation` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.park_fault.activated` | success | `confirmAmusementParkWideFaultActivation` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-activation` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.park_fault.initiate_failed` | failure | `initiateAmusementParkWideFault` | `POST` | `/api/v1/amusement/park-wide-faults` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.park_fault.initiated` | success | `initiateAmusementParkWideFault` | `POST` | `/api/v1/amusement/park-wide-faults` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.park_fault.lift_failed` | failure | `confirmAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-lift` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.park_fault.lift_initiate_failed` | failure | `initiateAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/initiate-lift` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.park_fault.lift_initiated` | success | `initiateAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/initiate-lift` | AMU-游乐安全 | `amusement.park_fault.confirm_as_head` | 接口级事件，以本附录为准 |
| `amusement.park_fault.lifted` | success | `confirmAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-lift` | AMU-游乐安全 | `amusement.park_fault.confirm_as_engineering_safety` | 接口级事件，以本附录为准 |
| `amusement.park_fault.view_failed` | failure | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.park_fault.view_failed` | failure | `listAmusementParkWideFaults` | `GET` | `/api/v1/amusement/park-wide-faults` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.park_fault.viewed` | success | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.park_fault.viewed` | success | `listAmusementParkWideFaults` | `GET` | `/api/v1/amusement/park-wide-faults` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `approveDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-approve` | AMU-游乐安全 | `amusement.project_entitlement.approve` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `archiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/archive` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `createAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `removeAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/remove` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `requestDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-request` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.change_failed` | failure | `unarchiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/unarchive` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `approveDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-approve` | AMU-游乐安全 | `amusement.project_entitlement.approve` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `archiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/archive` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `createAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `removeAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/remove` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `requestDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-request` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.changed` | success | `unarchiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/unarchive` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.view_failed` | failure | `listAmusementProjectEntitlements` | `GET` | `/api/v1/amusement/projects/{id}/entitlements` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_entitlement.viewed` | success | `listAmusementProjectEntitlements` | `GET` | `/api/v1/amusement/projects/{id}/entitlements` | AMU-游乐安全 | `amusement.project_entitlement.manage` | 接口级事件，以本附录为准 |
| `amusement.project_rule.changed` | success | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | AMU-游乐安全 | `amusement.project.manage` | 接口级事件，以本附录为准 |
| `amusement.project_rule.changed_failed` | failure | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | AMU-游乐安全 | `amusement.project.manage` | 接口级事件，以本附录为准 |
| `amusement.project_space_backlog.view_failed` | failure | `listAmusementProjectsWithoutSpace` | `GET` | `/api/v1/amusement/projects/without-space` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.project_space_backlog.viewed` | success | `listAmusementProjectsWithoutSpace` | `GET` | `/api/v1/amusement/projects/without-space` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.project_space.assign_failed` | failure | `assignAmusementProjectSpace` | `PUT` | `/api/v1/amusement/projects/{id}/space` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.project_space.assigned` | success | `assignAmusementProjectSpace` | `PUT` | `/api/v1/amusement/projects/{id}/space` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.project.resumed` | success | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | AMU-游乐安全 | `amusement.maintenance.confirm` | 接口级事件，以本附录为准 |
| `amusement.queue_entry.create` | success | `createAmusementQueueEntry` | `POST` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.manage` | 接口级事件，以本附录为准 |
| `amusement.queue_entry.create_failed` | failure | `createAmusementQueueEntry` | `POST` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.manage` | 接口级事件，以本附录为准 |
| `amusement.queue_entry.status_change` | success | `changeAmusementQueueEntryStatus` | `POST` | `/api/v1/amusement/queue-entries/{id}/status` | AMU-游乐安全 | `amusement.queue.manage` | 接口级事件，以本附录为准 |
| `amusement.queue_entry.status_change_failed` | failure | `changeAmusementQueueEntryStatus` | `POST` | `/api/v1/amusement/queue-entries/{id}/status` | AMU-游乐安全 | `amusement.queue.manage` | 接口级事件，以本附录为准 |
| `amusement.queue.viewed` | success | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.view` | 接口级事件，以本附录为准 |
| `amusement.queue.viewed_failed` | failure | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.view` | 接口级事件，以本附录为准 |
| `amusement.reservation.create` | success | `createAmusementReservation` | `POST` | `/api/v1/amusement/reservations` | AMU-游乐安全 | `amusement.reservation.manage` | 接口级事件，以本附录为准 |
| `amusement.reservation.create_failed` | failure | `createAmusementReservation` | `POST` | `/api/v1/amusement/reservations` | AMU-游乐安全 | `amusement.reservation.manage` | 接口级事件，以本附录为准 |
| `amusement.reservation.status_change` | success | `changeAmusementReservationStatus` | `POST` | `/api/v1/amusement/reservations/{id}/status` | AMU-游乐安全 | `amusement.reservation.manage` | 接口级事件，以本附录为准 |
| `amusement.reservation.status_change_failed` | failure | `changeAmusementReservationStatus` | `POST` | `/api/v1/amusement/reservations/{id}/status` | AMU-游乐安全 | `amusement.reservation.manage` | 接口级事件，以本附录为准 |
| `amusement.reservation.viewed` | success | `listAmusementReservations` | `GET` | `/api/v1/amusement/reservations` | AMU-游乐安全 | `amusement.reservation.view` | 接口级事件，以本附录为准 |
| `amusement.reservation.viewed_failed` | failure | `listAmusementReservations` | `GET` | `/api/v1/amusement/reservations` | AMU-游乐安全 | `amusement.reservation.view` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.blocked` | failure | `submitAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | AMU-游乐安全 | `amusement.safety_inspection.submit` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.create_failed` | failure | `createAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections` | AMU-游乐安全 | `amusement.safety_inspection.create` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.created` | success | `createAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections` | AMU-游乐安全 | `amusement.safety_inspection.create` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.recheck_failed` | failure | `recheckAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | AMU-游乐安全 | `amusement.safety_inspection.recheck` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.rechecked` | success | `recheckAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | AMU-游乐安全 | `amusement.safety_inspection.recheck` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.rectification_failed` | failure | `rectifyAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | AMU-游乐安全 | `amusement.safety_inspection.rectify` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.rectification_submitted` | success | `rectifyAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | AMU-游乐安全 | `amusement.safety_inspection.rectify` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.reopen_rejected` | failure | `approveAmusementReopen` | `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | AMU-游乐安全 | `amusement.safety_inspection.approve_reopen` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.reopened` | success | `approveAmusementReopen` | `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | AMU-游乐安全 | `amusement.safety_inspection.approve_reopen` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.submitted` | success | `submitAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | AMU-游乐安全 | `amusement.safety_inspection.submit` | 已在事件矩阵正文出现 |
| `amusement.safety_inspection.view_failed` | failure | `listAmusementSafetyInspections` | `GET` | `/api/v1/amusement/safety-inspections` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.safety_inspection.viewed` | success | `listAmusementSafetyInspections` | `GET` | `/api/v1/amusement/safety-inspections` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.safety_notice.create` | success | `createAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices` | AMU-游乐安全 | `amusement.safety_notice.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_notice.create_failed` | failure | `createAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices` | AMU-游乐安全 | `amusement.safety_notice.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_notice.list` | success | `listAmusementSafetyNotices` | `GET` | `/api/v1/amusement/safety-notices` | AMU-游乐安全 | `amusement.safety_notice.view` | 接口级事件，以本附录为准 |
| `amusement.safety_notice.list_failed` | failure | `listAmusementSafetyNotices` | `GET` | `/api/v1/amusement/safety-notices` | AMU-游乐安全 | `amusement.safety_notice.view` | 接口级事件，以本附录为准 |
| `amusement.safety_template.change_failed` | failure | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_template.changed` | success | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_template.view_failed` | failure | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.view` | 接口级事件，以本附录为准 |
| `amusement.safety_template.viewed` | success | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.view` | 接口级事件，以本附录为准 |
| `amusement.session.change_failed` | failure | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.change_failed` | failure | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.changed` | success | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.changed` | success | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.verification_failed` | failure | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.execute` | 已在事件矩阵正文出现 |
| `amusement.verification_succeeded` | success | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.execute` | 已在事件矩阵正文出现 |
| `amusement.verification.view_failed` | failure | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.verification.viewed` | success | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.waiver.acknowledge` | success | `acknowledgeAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices/{noticeId}/acknowledgements` | AMU-游乐安全 | `amusement.waiver.acknowledge` | 接口级事件，以本附录为准 |
| `amusement.waiver.acknowledge_failed` | failure | `acknowledgeAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices/{noticeId}/acknowledgements` | AMU-游乐安全 | `amusement.waiver.acknowledge` | 接口级事件，以本附录为准 |
| `analytics_business_metric_snapshot.viewed.failure` | failure | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics_business_metric_snapshot.viewed.success` | success | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.abandoned_entitlement_revenue.view_failed` | failure | `getAbandonedEntitlementRevenue` | `GET` | `/api/v1/analytics/abandoned-entitlement-revenue` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.abandoned_entitlement_revenue.viewed` | success | `getAbandonedEntitlementRevenue` | `GET` | `/api/v1/analytics/abandoned-entitlement-revenue` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.abnormal_readings.view_failed` | failure | `listAbnormalReadings` | `GET` | `/api/v1/analytics/energy/abnormal-readings` | ANA-风控分析 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `analytics.abnormal_readings.viewed` | success | `listAbnormalReadings` | `GET` | `/api/v1/analytics/energy/abnormal-readings` | ANA-风控分析 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_result.viewed` | success | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_result.viewed_failed` | failure | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_task.viewed` | success | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_task.viewed_failed` | failure | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality.local_llm_suggestion.created` | success | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_quality.local_llm_suggestion.failed` | failure | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_thresholds.update_failed` | failure | `updateAiThresholds` | `POST` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_thresholds.updated` | success | `updateAiThresholds` | `POST` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_thresholds.view_failed` | failure | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.ai_thresholds.viewed` | success | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.anomaly_config.update_failed` | failure | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.anomaly_config.updated` | success | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.anomaly_config.view_failed` | failure | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.anomaly_config.viewed` | success | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.ar_debtors.view_failed` | failure | `listArOverdueDebtors` | `GET` | `/api/v1/analytics/finance-receivables/debtors` | ANA-风控分析 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `analytics.ar_debtors.viewed` | success | `listArOverdueDebtors` | `GET` | `/api/v1/analytics/finance-receivables/debtors` | ANA-风控分析 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `analytics.business_day_cutoff.update_failed` | failure | `upsertBusinessDayCutoff` | `POST` | `/api/v1/analytics/business-day-cutoffs` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.business_day_cutoff.updated` | success | `upsertBusinessDayCutoff` | `POST` | `/api/v1/analytics/business-day-cutoffs` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.business_day_cutoff.view_failed` | failure | `listBusinessDayCutoffs` | `GET` | `/api/v1/analytics/business-day-cutoffs` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.business_day_cutoff.viewed` | success | `listBusinessDayCutoffs` | `GET` | `/api/v1/analytics/business-day-cutoffs` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.customer_intent.viewed` | success | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.customer_intent.viewed_failed` | failure | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_domain.view_failed` | failure | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_domain.viewed` | success | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_view_failed` | failure | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_viewed` | success | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.expiring_contracts.view_failed` | failure | `listExpiringContracts` | `GET` | `/api/v1/analytics/occupancy/expiring-contracts` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.expiring_contracts.viewed` | success | `listExpiringContracts` | `GET` | `/api/v1/analytics/occupancy/expiring-contracts` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.group_overview.view_failed` | failure | `getGroupOverview` | `GET` | `/api/v1/analytics/group-overview` | ANA-风控分析 | `analytics.group.view` | 接口级事件，以本附录为准 |
| `analytics.group_overview.viewed` | success | `getGroupOverview` | `GET` | `/api/v1/analytics/group-overview` | ANA-风控分析 | `analytics.group.view` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.fetch_failed` | failure | `fetchHolidayCalendar` | `POST` | `/api/v1/analytics/holiday-calendar/fetch` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.fetched` | success | `fetchHolidayCalendar` | `POST` | `/api/v1/analytics/holiday-calendar/fetch` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.update_failed` | failure | `upsertHolidayPeriod` | `POST` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.updated` | success | `upsertHolidayPeriod` | `POST` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.view_failed` | failure | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.holiday_calendar.viewed` | success | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.holiday_comparison.view_failed` | failure | `getHolidayComparison` | `GET` | `/api/v1/analytics/holiday-comparison` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.holiday_comparison.viewed` | success | `getHolidayComparison` | `GET` | `/api/v1/analytics/holiday-comparison` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_health.viewed` | success | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_health.viewed_failed` | failure | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_summary.viewed` | success | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_summary.viewed_failed` | failure | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.disposed` | success | `disposeMetricAnomaly` | `POST` | `/api/v1/analytics/metric-anomalies/{id}/dispose` | ANA-风控分析 | `analytics.metric_anomaly.manage` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.disposed_failed` | failure | `disposeMetricAnomaly` | `POST` | `/api/v1/analytics/metric-anomalies/{id}/dispose` | ANA-风控分析 | `analytics.metric_anomaly.manage` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.timeline_viewed` | success | `listMetricAnomalyTimeline` | `GET` | `/api/v1/analytics/metric-anomalies/{id}/timeline` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.timeline_viewed_failed` | failure | `listMetricAnomalyTimeline` | `GET` | `/api/v1/analytics/metric-anomalies/{id}/timeline` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.viewed` | success | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.viewed_failed` | failure | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_definition.viewed` | success | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_definition.viewed_failed` | failure | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_detail.view_failed` | failure | `getMetricDetail` | `GET` | `/api/v1/analytics/metric-detail` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_detail.viewed` | success | `getMetricDetail` | `GET` | `/api/v1/analytics/metric-detail` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.monthly_comparison.view_failed` | failure | `getMonthlyComparison` | `GET` | `/api/v1/analytics/monthly-comparison` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.monthly_comparison.viewed` | success | `getMonthlyComparison` | `GET` | `/api/v1/analytics/monthly-comparison` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.monthly_report.export_failed` | failure | `exportMonthlyReport` | `GET` | `/api/v1/analytics/reports/monthly-export` | ANA-风控分析 | `analytics.report.export` | 接口级事件，以本附录为准 |
| `analytics.monthly_report.exported` | success | `exportMonthlyReport` | `GET` | `/api/v1/analytics/reports/monthly-export` | ANA-风控分析 | `analytics.report.export` | 接口级事件，以本附录为准 |
| `analytics.notification_windows.update_failed` | failure | `upsertNotificationWindow` | `POST` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `platform.notification_window.manage` | 接口级事件，以本附录为准 |
| `analytics.notification_windows.updated` | success | `upsertNotificationWindow` | `POST` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `platform.notification_window.manage` | 接口级事件，以本附录为准 |
| `analytics.notification_windows.view_failed` | failure | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.notification_windows.viewed` | success | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.operating_params.view_failed` | failure | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.operating_params.viewed` | success | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.overdue_work_orders.view_failed` | failure | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | ANA-风控分析 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `analytics.overdue_work_orders.viewed` | success | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | ANA-风控分析 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `analytics.rent_to_sales.view_failed` | failure | `getRentToSales` | `GET` | `/api/v1/analytics/rent-to-sales` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.rent_to_sales.viewed` | success | `getRentToSales` | `GET` | `/api/v1/analytics/rent-to-sales` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.update_failed` | failure | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.updated` | success | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.view_failed` | failure | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.viewed` | success | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.review_lock.viewed` | success | `listReviewLocks` | `GET` | `/api/v1/analytics/review-locks` | ANA-风控分析 | `analytics.review.lock` | 接口级事件，以本附录为准 |
| `analytics.review_lock.viewed_failed` | failure | `listReviewLocks` | `GET` | `/api/v1/analytics/review-locks` | ANA-风控分析 | `analytics.review.lock` | 接口级事件，以本附录为准 |
| `analytics.review.lock_failed` | failure | `createReviewLock` | `POST` | `/api/v1/analytics/review-locks` | ANA-风控分析 | `analytics.review.lock` | 接口级事件，以本附录为准 |
| `analytics.review.locked` | success | `createReviewLock` | `POST` | `/api/v1/analytics/review-locks` | ANA-风控分析 | `analytics.review.lock` | 接口级事件，以本附录为准 |
| `analytics.risk_alert.resolved` | success | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.risk_alert.resolved_failed` | failure | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.risk_rule.viewed` | success | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.risk_rule.viewed_failed` | failure | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.robot.viewed` | success | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.robot.viewed_failed` | failure | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.staff_service_score.viewed` | success | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.staff_service_score.viewed_failed` | failure | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.traffic_hourly.view_failed` | failure | `getAnalyticsTrafficHourly` | `GET` | `/api/v1/analytics/traffic-hourly` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.traffic_hourly.viewed` | success | `getAnalyticsTrafficHourly` | `GET` | `/api/v1/analytics/traffic-hourly` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.vacant_spaces.view_failed` | failure | `listVacantSpaces` | `GET` | `/api/v1/analytics/occupancy/vacant-spaces` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.vacant_spaces.viewed` | success | `listVacantSpaces` | `GET` | `/api/v1/analytics/occupancy/vacant-spaces` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `ar_ap_payment.list_failed` | failure | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `ar_ap_payment.listed` | success | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `audit.operation_log.export_failed` | failure | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | P0-待归类 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `audit.operation_log.exported` | success | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | P0-待归类 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `audit.operation_log.view_failed` | failure | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | P0-待归类 | `audit.operation_log.view` | 接口级事件，以本附录为准 |
| `audit.operation_log.viewed` | success | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | P0-待归类 | `audit.operation_log.view` | 接口级事件，以本附录为准 |
| `auth.login.failed` | failure | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.login.succeeded` | success | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.logout` | success | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.logout_failed` | failure | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.token_refresh_failed` | failure | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.token.refreshed` | success | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `bar.bill_operation.list` | success | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill_operation.list_failed` | failure | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill_reopen_failed` | failure | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | BAR-酒吧经营 | `bar.bill.reopen` | 接口级事件，以本附录为准 |
| `bar.bill_reopened` | success | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | BAR-酒吧经营 | `bar.bill.reopen` | 接口级事件，以本附录为准 |
| `bar.bill_tip_added` | success | `addBarBillTip` | `POST` | `/api/v1/bar/bills/{bill_id}/tip` | BAR-酒吧经营 | `bar.bill.tip` | 接口级事件，以本附录为准 |
| `bar.bill_tip_failed` | failure | `addBarBillTip` | `POST` | `/api/v1/bar/bills/{bill_id}/tip` | BAR-酒吧经营 | `bar.bill.tip` | 接口级事件，以本附录为准 |
| `bar.bill.adjust` | success | `adjustBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/adjustments` | BAR-酒吧经营 | `bar.bill.adjust` | 接口级事件，以本附录为准 |
| `bar.bill.adjust_failed` | failure | `adjustBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/adjustments` | BAR-酒吧经营 | `bar.bill.adjust` | 接口级事件，以本附录为准 |
| `bar.bill.created` | success | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.bill.created_failed` | failure | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.bill.merge` | success | `mergeBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/merge` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill.merge_failed` | failure | `mergeBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/merge` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill.settle_failed` | failure | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `bar.bill.settled` | success | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `bar.bill.split` | success | `splitBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/split` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill.split_failed` | failure | `splitBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/split` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.catalog.image_uploaded` | success | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.catalog.image_uploaded_failed` | failure | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.category.saved` | success | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.category.saved_failed` | failure | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.category.viewed` | success | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.view` | 接口级事件，以本附录为准 |
| `bar.category.viewed_failed` | failure | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.view` | 接口级事件，以本附录为准 |
| `bar.combo.saved` | success | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `bar.combo.saved_failed` | failure | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `bar.combo.viewed` | success | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.view` | 接口级事件，以本附录为准 |
| `bar.combo.viewed_failed` | failure | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.view` | 接口级事件，以本附录为准 |
| `bar.deposit_rule.create_failed` | failure | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | BAR-酒吧经营 | `bar.deposit_rule.manage` | 接口级事件，以本附录为准 |
| `bar.deposit_rule.created` | success | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | BAR-酒吧经营 | `bar.deposit_rule.manage` | 接口级事件，以本附录为准 |
| `bar.entitlement_redeem_failed` | failure | `redeemBarDrinkEntitlement` | `POST` | `/api/v1/bar/entitlement-redemptions/drink` | BAR-酒吧经营 | `bar.entitlement.redeem` | 接口级事件，以本附录为准 |
| `bar.entitlement_redeemed` | success | `redeemBarDrinkEntitlement` | `POST` | `/api/v1/bar/entitlement-redemptions/drink` | BAR-酒吧经营 | `bar.entitlement.redeem` | 接口级事件，以本附录为准 |
| `bar.entitlement_redemption.view_failed` | failure | `listBarEntitlementRedemptions` | `GET` | `/api/v1/bar/entitlement-redemptions` | BAR-酒吧经营 | `bar.entitlement.read` | 接口级事件，以本附录为准 |
| `bar.entitlement_redemption.viewed` | success | `listBarEntitlementRedemptions` | `GET` | `/api/v1/bar/entitlement-redemptions` | BAR-酒吧经营 | `bar.entitlement.read` | 接口级事件，以本附录为准 |
| `bar.order_create_failed` | failure | `createBarOrder` | `POST` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_created` | success | `createBarOrder` | `POST` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_deliver_failed` | failure | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_delivered` | success | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | BAR-酒吧经营 | `bar.order.create` | 已在事件矩阵正文出现 |
| `bar.order_item_void_failed` | failure | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.decide` | success | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.decide_failed` | failure | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.view_failed` | failure | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.viewed` | success | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_voided` | success | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item.created` | success | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_item.created_failed` | failure | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order.view_failed` | failure | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.order.viewed` | success | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.product_sold_out` | success | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | BAR-酒吧经营 | `bar.product.sold_out` | 接口级事件，以本附录为准 |
| `bar.product_sold_out_failed` | failure | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | BAR-酒吧经营 | `bar.product.sold_out` | 接口级事件，以本附录为准 |
| `bar.product.category_assigned` | success | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.category_assigned_failed` | failure | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.created` | success | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.created_failed` | failure | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.viewed` | success | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.product.viewed` | success | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.product.viewed_failed` | failure | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.product.viewed_failed` | failure | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket_event.list` | success | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | BAR-酒吧经营 | `bar.production_ticket.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket_event.list_failed` | failure | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | BAR-酒吧经营 | `bar.production_ticket.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket.transition` | success | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | BAR-酒吧经营 | `bar.production_ticket.manage` | 接口级事件，以本附录为准 |
| `bar.production_ticket.transition_failed` | failure | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | BAR-酒吧经营 | `bar.production_ticket.manage` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed` | success | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed_failed` | failure | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_deduct_failed` | failure | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_deducted` | success | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_paid` | success | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_pay_failed` | failure | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_entered` | success | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_entry_failed` | failure | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_issue_failed` | failure | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_issued` | success | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_revoke_failed` | failure | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_revoked` | success | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_view_failed` | failure | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_viewed` | success | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.reservation.cancel_failed` | failure | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.cancelled` | success | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.create_failed` | failure | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.reservation.created` | success | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.reservation.view_failed` | failure | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.reservation.viewed` | success | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.saved_bottle_picked_up` | success | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | INV-库存存酒 | `bar.saved_bottle.manage` | 已在事件矩阵正文出现 |
| `bar.saved_bottle.cancel_failed` | failure | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.cancelled` | success | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.create_failed` | failure | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.created` | success | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 已在事件矩阵正文出现 |
| `bar.saved_bottle.dispute` | success | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.dispute_failed` | failure | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.pickup_failed` | failure | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.view_failed` | failure | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.viewed` | success | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.scan_order_create_failed` | failure | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.scan_order_created` | success | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.shift_close_failed` | failure | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | BAR-酒吧经营 | `bar.shift.close` | 接口级事件，以本附录为准 |
| `bar.shift_closed` | success | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | BAR-酒吧经营 | `bar.shift.close` | 接口级事件，以本附录为准 |
| `bar.shift_open_failed` | failure | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_opened` | success | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift.view_failed` | failure | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift.viewed` | success | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.table_availability.view_failed` | failure | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.table_availability.viewed` | success | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculate_failed` | failure | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculated` | success | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 已在事件矩阵正文出现 |
| `bar.table_guest.checked_in` | success | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.table_guest.entry_allowance_verified` | success | `verifyBarReservationEntryAllowance` | `POST` | `/api/v1/bar/reservations/{id}/entry-allowance/verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 接口级事件，以本附录为准 |
| `bar.table_guest.entry_allowance_verify_failed` | failure | `verifyBarReservationEntryAllowance` | `POST` | `/api/v1/bar/reservations/{id}/entry-allowance/verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 接口级事件，以本附录为准 |
| `bar.table_guest.rejected` | failure | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.table_session.create` | success | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `bar.table_session.create_failed` | failure | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `bar.table_session.list` | success | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.view` | 接口级事件，以本附录为准 |
| `bar.table_session.list_failed` | failure | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.view` | 接口级事件，以本附录为准 |
| `bar.table_session.merge` | success | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | BAR-酒吧经营 | `bar.table_session.merge` | 接口级事件，以本附录为准 |
| `bar.table_session.merge_failed` | failure | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | BAR-酒吧经营 | `bar.table_session.merge` | 接口级事件，以本附录为准 |
| `bar.table_session.renew` | success | `renewBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/renew` | BAR-酒吧经营 | `bar.table_session.renew` | 接口级事件，以本附录为准 |
| `bar.table_session.renew_failed` | failure | `renewBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/renew` | BAR-酒吧经营 | `bar.table_session.renew` | 接口级事件，以本附录为准 |
| `bar.table_session.transfer` | success | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table_session.transfer_failed` | failure | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table_space_backlog.view_failed` | failure | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space_backlog.viewed` | success | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space.assign_failed` | failure | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space.assigned` | success | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.change_failed` | failure | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.changed` | success | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.open_failed` | failure | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.opened` | success | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.view_failed` | failure | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `bar.table.viewed` | success | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.archive_failed` | failure | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.archived` | success | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.save_failed` | failure | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.saved` | success | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.view_failed` | failure | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.viewed` | success | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `business_entity.change_failed` | failure | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.changed` | success | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.view_failed` | failure | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `business_entity.viewed` | success | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.create` | success | `createContractKeyDate` | `POST` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.manage` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.create_failed` | failure | `createContractKeyDate` | `POST` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.manage` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.list` | success | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.view` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.list_failed` | failure | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.view` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.create` | success | `createComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.create_failed` | failure | `createComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.list` | success | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.list_failed` | failure | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `compliance.reminder.list` | success | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | P0-待归类 | `compliance.reminder.view` | 接口级事件，以本附录为准 |
| `compliance.reminder.list_failed` | failure | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | P0-待归类 | `compliance.reminder.view` | 接口级事件，以本附录为准 |
| `compliance.reminder.scan` | success | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | P0-待归类 | `compliance.reminder.scan` | 接口级事件，以本附录为准 |
| `compliance.reminder.scan_failed` | failure | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | P0-待归类 | `compliance.reminder.scan` | 接口级事件，以本附录为准 |
| `contract.activate_failed` | failure | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | P0-待归类 | `contract.lifecycle.activate` | 接口级事件，以本附录为准 |
| `contract.activated` | success | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | P0-待归类 | `contract.lifecycle.activate` | 接口级事件，以本附录为准 |
| `contract.amendments_view_failed` | failure | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.amendments_viewed` | success | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.approval_flow_save_failed` | failure | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.approval_flow_saved` | success | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.approval_flow_view_failed` | failure | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.approval_flow_viewed` | success | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.approvals_view_failed` | failure | `listContractApprovals` | `GET` | `/api/v1/contracts/{contract_id}/approvals` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.approvals_viewed` | success | `listContractApprovals` | `GET` | `/api/v1/contracts/{contract_id}/approvals` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.approve_failed` | failure | `approveContractStage` | `POST` | `/api/v1/contracts/{contract_id}/approve` | P0-待归类 | `contract.approve.business` | 接口级事件，以本附录为准 |
| `contract.approved_slot` | success | `approveContractStage` | `POST` | `/api/v1/contracts/{contract_id}/approve` | P0-待归类 | `contract.approve.business` | 接口级事件，以本附录为准 |
| `contract.attachments_add_failed` | failure | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.attachments_added` | success | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.change_approve_failed` | failure | `approveContractChange` | `POST` | `/api/v1/contracts/changes/{change_id}/approve` | P0-待归类 | `contract.approve.business` | 接口级事件，以本附录为准 |
| `contract.change_approved` | success | `approveContractChange` | `POST` | `/api/v1/contracts/changes/{change_id}/approve` | P0-待归类 | `contract.approve.business` | 接口级事件，以本附录为准 |
| `contract.change_raise_failed` | failure | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.change_raised` | success | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.changes_view_failed` | failure | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.changes_viewed` | success | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.color_rule_save_failed` | failure | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.color_rule_saved` | success | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.color_rules_view_failed` | failure | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.color_rules_viewed` | success | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.dispute_resolve_failed` | failure | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | P0-待归类 | `contract.settlement.dispute.resolve` | 接口级事件，以本附录为准 |
| `contract.dispute_resolved` | success | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | P0-待归类 | `contract.settlement.dispute.resolve` | 接口级事件，以本附录为准 |
| `contract.draft_failed` | failure | `createContractDraft` | `POST` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.drafted` | success | `createContractDraft` | `POST` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.fee_billed` | success | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.period_bill_failed` | failure | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.rent_bill_failed` | failure | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.rent_billed` | success | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.scope_request_raise_failed` | failure | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | P0-待归类 | `contract.scope_request.raise` | 接口级事件，以本附录为准 |
| `contract.scope_request_raised` | success | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | P0-待归类 | `contract.scope_request.raise` | 接口级事件，以本附录为准 |
| `contract.settle_failed` | failure | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settled` | success | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_advance_failed` | failure | `advanceContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/advance` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_advanced` | success | `advanceContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/advance` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_complete_failed` | failure | `completeContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/complete` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_completed` | success | `completeContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/complete` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlements_view_failed` | failure | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlements_viewed` | success | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.space_rental_view_failed` | failure | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.space_rental_viewed` | success | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.submit_failed` | failure | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.submitted` | success | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | P0-待归类 | `contract.draft.manage` | 已在事件矩阵正文出现 |
| `contract.view_failed` | failure | `listContracts` | `GET` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.viewed` | success | `listContracts` | `GET` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `customer.account.blacklist_failed` | failure | `blacklistCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.blacklisted` | success | `blacklistCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.deregister_failed` | failure | `deregisterCustomerAccount` | `POST` | `/api/v1/customer/deregister` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.account.deregistered` | success | `deregisterCustomerAccount` | `POST` | `/api/v1/customer/deregister` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.account.reinstate_failed` | failure | `reinstateCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/reinstate` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.reinstated` | success | `reinstateCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/reinstate` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.suspend_failed` | failure | `suspendCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/suspend` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.suspended` | success | `suspendCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/suspend` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.view_failed` | failure | `lookupCustomerAccountByPhone` | `POST` | `/api/v1/customer-accounts/lookup` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.account.viewed` | success | `lookupCustomerAccountByPhone` | `POST` | `/api/v1/customer-accounts/lookup` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.auth.login` | success | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.auth.login_failed` | failure | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.confirm_failed` | failure | `confirmCustomerBarInvitation` | `POST` | `/api/v1/customer/bar-invitations/confirm` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.confirmed` | success | `confirmCustomerBarInvitation` | `POST` | `/api/v1/customer/bar-invitations/confirm` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.view_failed` | failure | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.viewed` | success | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_view_failed` | failure | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_viewed` | success | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.create_failed` | failure | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.created` | success | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_scan_menu.view_failed` | failure | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-sessions/{session_id}/menu` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_scan_menu.viewed` | success | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-sessions/{session_id}/menu` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.create_failed` | failure | `placeCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-sessions/{session_id}/scan-orders` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.created` | success | `placeCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-sessions/{session_id}/scan-orders` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_in_failed` | failure | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_ins_view_failed` | failure | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_ins_viewed` | success | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.checked_in` | success | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.create_failed` | failure | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.created` | success | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.level_evaluate_failed` | failure | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `customer.logged_out` | success | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.logout_failed` | failure | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.member_levels_view_failed` | failure | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.member_levels_viewed` | success | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.my_tickets_view_failed` | failure | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.my_tickets_viewed` | success | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.order_tickets_view_failed` | failure | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.order_tickets_viewed` | success | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.orders_view_failed` | failure | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.orders_viewed` | success | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_view_failed` | failure | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_viewed` | success | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.freeze_failed` | failure | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.frozen` | success | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.ledger_view_failed` | failure | `getCustomerPointsLedger` | `GET` | `/api/v1/customer/points/ledger` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.ledger_viewed` | success | `getCustomerPointsLedger` | `GET` | `/api/v1/customer/points/ledger` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redeem` | success | `redeemCustomerPoints` | `POST` | `/api/v1/customer/points/redemptions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redeem_failed` | failure | `redeemCustomerPoints` | `POST` | `/api/v1/customer/points/redemptions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redemption_items_view_failed` | failure | `listCustomerPointsRedemptionItems` | `GET` | `/api/v1/customer/points/redemption-items` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redemption_items_viewed` | success | `listCustomerPointsRedemptionItems` | `GET` | `/api/v1/customer/points/redemption-items` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redemptions_view_failed` | failure | `listCustomerPointsRedemptions` | `GET` | `/api/v1/customer/points/redemptions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.redemptions_viewed` | success | `listCustomerPointsRedemptions` | `GET` | `/api/v1/customer/points/redemptions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.unfreeze_failed` | failure | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.unfrozen` | success | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.session_view_failed` | failure | `getCustomerSession` | `GET` | `/api/v1/customer/session` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.session_viewed` | success | `getCustomerSession` | `GET` | `/api/v1/customer/session` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.stored_value_account_viewed` | success | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `customer.stored_value_account_viewed_failed` | failure | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `customer.stored_value_accounts_viewed` | success | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `customer.stored_value_accounts_viewed_failed` | failure | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `customer.stored_value.consume` | success | `customerStoredValueConsume` | `POST` | `/api/v1/customer/stored-value/consume` | CUS-客人端 | `customer.stored_value.consume` | 接口级事件，以本附录为准 |
| `customer.stored_value.consume_failed` | failure | `customerStoredValueConsume` | `POST` | `/api/v1/customer/stored-value/consume` | CUS-客人端 | `customer.stored_value.consume` | 接口级事件，以本附录为准 |
| `customer.stored_value.settle` | success | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.settle_failed` | failure | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.topup` | success | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.topup_failed` | failure | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.ticket_order.create_failed` | failure | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_order.created` | success | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_products_view_failed` | failure | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `customer.ticket_products_viewed` | success | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_incoming_view_failed` | failure | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_incoming_viewed` | success | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_outgoing_view_failed` | failure | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_outgoing_viewed` | success | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_preview_view_failed` | failure | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_preview_viewed` | success | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_incoming_view_failed` | failure | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_incoming_viewed` | success | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_outgoing_view_failed` | failure | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_outgoing_viewed` | success | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_preview_view_failed` | failure | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.voucher_transfer_preview_viewed` | success | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `dealer.admission_view_failed` | failure | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.admission_viewed` | success | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.contract_create_failed` | failure | `createDealerContract` | `POST` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_created` | success | `createDealerContract` | `POST` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_terms_update_failed` | failure | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_terms_updated` | success | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_view_failed` | failure | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.contract_viewed` | success | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.contract.view_failed` | failure | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `dealer.contract.viewed` | success | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `dealer.create_failed` | failure | `createDealer` | `POST` | `/api/v1/dealers` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.created` | success | `createDealer` | `POST` | `/api/v1/dealers` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.deactivate_failed` | failure | `deactivateDealer` | `POST` | `/api/v1/dealers/{id}/deactivate` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.deactivated` | success | `deactivateDealer` | `POST` | `/api/v1/dealers/{id}/deactivate` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.physical_complete_failed` | failure | `completeDealerDeliveryPhysical` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/physical-completion` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.delivery.physical_completed` | success | `completeDealerDeliveryPhysical` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/physical-completion` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.delivery.receive_failed` | failure | `dealerPortalSignOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/sign` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.receive_failed` | failure | `signDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/sign` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.received` | success | `dealerPortalSignOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/sign` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.received` | success | `signDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/sign` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.replacement_ship_failed` | failure | `shipDealerReplacement` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/replacement-shipment` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.delivery.replacement_shipped` | success | `shipDealerReplacement` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/replacement-shipment` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.delivery.resolution_request_failed` | failure | `requestDealerDeliveryResolution` | `POST` | `/api/v1/dealers/delivery/discrepancies/{id}/resolutions` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.resolution_requested` | success | `requestDealerDeliveryResolution` | `POST` | `/api/v1/dealers/delivery/discrepancies/{id}/resolutions` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.view_failed` | failure | `getDealerDelivery` | `GET` | `/api/v1/dealers/orders/{id}/delivery` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.delivery.view_failed` | failure | `getDealerPortalDelivery` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.delivery.viewed` | success | `getDealerDelivery` | `GET` | `/api/v1/dealers/orders/{id}/delivery` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.delivery.viewed` | success | `getDealerPortalDelivery` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.external_stock.view_failed` | failure | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | P0-待归类 | `dealer_portal.stock.view` | 接口级事件，以本附录为准 |
| `dealer.external_stock.view_failed` | failure | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.external_stock.viewed` | success | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | P0-待归类 | `dealer_portal.stock.view` | 接口级事件，以本附录为准 |
| `dealer.external_stock.viewed` | success | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.inquiry_view_failed` | failure | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.inquiry_viewed` | success | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.inquiry.create_failed` | failure | `createDealerInquiry` | `POST` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.inquiry.create_failed` | failure | `dealerPortalCreateInquiry` | `POST` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.inquiry.created` | success | `createDealerInquiry` | `POST` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.inquiry.created` | success | `dealerPortalCreateInquiry` | `POST` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.inquiry.decide_failed` | failure | `decideDealerInquiryQuote` | `POST` | `/api/v1/dealers/inquiries/{id}/decision` | P0-待归类 | `dealer.inquiry.approve` | 接口级事件，以本附录为准 |
| `dealer.inquiry.decided` | success | `decideDealerInquiryQuote` | `POST` | `/api/v1/dealers/inquiries/{id}/decision` | P0-待归类 | `dealer.inquiry.approve` | 接口级事件，以本附录为准 |
| `dealer.inquiry.expire_failed` | failure | `expireDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/expire` | P0-待归类 | `dealer.inquiry.quote` | 接口级事件，以本附录为准 |
| `dealer.inquiry.expired` | success | `expireDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/expire` | P0-待归类 | `dealer.inquiry.quote` | 接口级事件，以本附录为准 |
| `dealer.inquiry.quote_failed` | failure | `quoteDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/quote` | P0-待归类 | `dealer.inquiry.quote` | 接口级事件，以本附录为准 |
| `dealer.inquiry.quoted` | success | `quoteDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/quote` | P0-待归类 | `dealer.inquiry.quote` | 接口级事件，以本附录为准 |
| `dealer.inquiry.view_failed` | failure | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.inquiry.viewed` | success | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.issue_failed` | failure | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.issued` | success | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.request_failed` | failure | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.requested` | success | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reverse_failed` | failure | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reversed` | success | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.view_failed` | failure | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.invoice.viewed` | success | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_decide_failed` | failure | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_decided` | success | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_request_failed` | failure | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_requested` | success | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.draft_failed` | failure | `createDealerOrderDraft` | `POST` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.draft_failed` | failure | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.drafted` | success | `createDealerOrderDraft` | `POST` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.drafted` | success | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_use_confirmed` | success | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_use_failed` | failure | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_view_failed` | failure | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_viewed` | success | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.payment_register_failed` | failure | `dealerPortalRegisterPayment` | `POST` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `dealer.order.payment_register_failed` | failure | `registerDealerOrderPayment` | `POST` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.payment_registered` | success | `dealerPortalRegisterPayment` | `POST` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `dealer.order.payment_registered` | success | `registerDealerOrderPayment` | `POST` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.payment_verified` | success | `verifyDealerOrderPayment` | `POST` | `/api/v1/dealers/payments/{id}/verify` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.payment_verify_failed` | failure | `verifyDealerOrderPayment` | `POST` | `/api/v1/dealers/payments/{id}/verify` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.refund_execute_failed` | failure | `executeDealerOrderRefund` | `POST` | `/api/v1/dealers/refunds/{id}/execute` | P0-待归类 | `dealer.order.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.order.refund_executed` | success | `executeDealerOrderRefund` | `POST` | `/api/v1/dealers/refunds/{id}/execute` | P0-待归类 | `dealer.order.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.order.refund_request_failed` | failure | `openDealerOrderRefundApproval` | `POST` | `/api/v1/dealers/refunds/{id}/approval` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.refund_requested` | success | `openDealerOrderRefundApproval` | `POST` | `/api/v1/dealers/refunds/{id}/approval` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.review_failed` | failure | `reviewDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/review` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.reviewed` | success | `reviewDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/review` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.ship_failed` | failure | `shipDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/ship` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.order.shipped` | success | `shipDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/ship` | P0-待归类 | `dealer.order.ship` | 接口级事件，以本附录为准 |
| `dealer.order.submit_failed` | failure | `dealerPortalSubmitOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/submit` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.submit_failed` | failure | `submitDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/submit` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.submitted` | success | `dealerPortalSubmitOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/submit` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.submitted` | success | `submitDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/submit` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.view_failed` | failure | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.portal_account.create_failed` | failure | `createDealerPortalAccount` | `POST` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal_account.created` | success | `createDealerPortalAccount` | `POST` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal_account.view_failed` | failure | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal_account.viewed` | success | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.access_rejected` | failure | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `dealer.portal.delivery_evidence_view_failed` | failure | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.delivery_evidence_viewed` | success | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.viewed` | success | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `dealer.price_catalog_line_view_failed` | failure | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.price_catalog_line_viewed` | success | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.price_catalog_view_failed` | failure | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.price_catalog_viewed` | success | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.decide_failed` | failure | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | P0-待归类 | `dealer.price_catalog.approve` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.decided` | success | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | P0-待归类 | `dealer.price_catalog.approve` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.draft_failed` | failure | `createDealerPriceCatalogDraft` | `POST` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.drafted` | success | `createDealerPriceCatalogDraft` | `POST` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.submit_failed` | failure | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.submitted` | success | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.return.decide_failed` | failure | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | P0-待归类 | `dealer.return.review` | 接口级事件，以本附录为准 |
| `dealer.return.decided` | success | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | P0-待归类 | `dealer.return.review` | 接口级事件，以本附录为准 |
| `dealer.return.freight_confirm_failed` | failure | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.freight_confirmed` | success | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.inspect_failed` | failure | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.inspected` | success | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.receive_failed` | failure | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.received` | success | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.refund_execute_failed` | failure | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.return.refund_executed` | success | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.return.request_failed` | failure | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.request_failed` | failure | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.requested` | success | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.requested` | success | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.view_failed` | failure | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.view_failed` | failure | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.view_failed` | failure | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.view_failed` | failure | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.viewed` | success | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.viewed` | success | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.viewed` | success | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.viewed` | success | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.review_failed` | failure | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | P0-待归类 | `dealer.review` | 接口级事件，以本附录为准 |
| `dealer.reviewed` | success | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | P0-待归类 | `dealer.review` | 接口级事件，以本附录为准 |
| `dealer.submit_failed` | failure | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | P0-待归类 | `dealer_portal.profile.submit` | 接口级事件，以本附录为准 |
| `dealer.submit_failed` | failure | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.submitted` | success | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | P0-待归类 | `dealer_portal.profile.submit` | 接口级事件，以本附录为准 |
| `dealer.submitted` | success | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.view_failed` | failure | `listDealers` | `GET` | `/api/v1/dealers` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.viewed` | success | `listDealers` | `GET` | `/api/v1/dealers` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `device.policy_violation` | success | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `device.policy_violation_failed` | failure | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `dividend.approve_failed` | failure | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.approved` | success | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.compute_failed` | failure | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.computed` | success | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.mark_paid_failed` | failure | `markDividendStatementPaid` | `POST` | `/api/v1/finance/dividends/statements/{id}/mark-paid` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.paid_marked` | success | `markDividendStatementPaid` | `POST` | `/api/v1/finance/dividends/statements/{id}/mark-paid` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.plan_create_failed` | failure | `createDividendPlan` | `POST` | `/api/v1/finance/dividends/plans` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.plan_created` | success | `createDividendPlan` | `POST` | `/api/v1/finance/dividends/plans` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.plan_update_failed` | failure | `updateDividendPlan` | `PUT` | `/api/v1/finance/dividends/plans/{id}` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.plan_updated` | success | `updateDividendPlan` | `PUT` | `/api/v1/finance/dividends/plans/{id}` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.plans_list_failed` | failure | `listDividendPlans` | `GET` | `/api/v1/finance/dividends/plans` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.plans_listed` | success | `listDividendPlans` | `GET` | `/api/v1/finance/dividends/plans` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.reject_failed` | failure | `rejectDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/reject` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.rejected` | success | `rejectDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/reject` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.reserve_ledger_list_failed` | failure | `listDividendReserveReleases` | `GET` | `/api/v1/finance/dividends/reserve-releases` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.reserve_ledger_listed` | success | `listDividendReserveReleases` | `GET` | `/api/v1/finance/dividends/reserve-releases` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.reserve_release_failed` | failure | `releaseDividendReserve` | `POST` | `/api/v1/finance/dividends/statements/{id}/release-reserve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.reserve_released` | success | `releaseDividendReserve` | `POST` | `/api/v1/finance/dividends/statements/{id}/release-reserve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.statement_view_failed` | failure | `getDividendStatement` | `GET` | `/api/v1/finance/dividends/statements/{id}` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.statement_viewed` | success | `getDividendStatement` | `GET` | `/api/v1/finance/dividends/statements/{id}` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.statements_list_failed` | failure | `listDividendStatements` | `GET` | `/api/v1/finance/dividends/statements` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.statements_listed` | success | `listDividendStatements` | `GET` | `/api/v1/finance/dividends/statements` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.submit_failed` | failure | `submitDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/submit` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.submitted` | success | `submitDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/submit` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settle_failed` | failure | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settle_failed` | failure | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settled` | success | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settled` | success | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.meter_difference.view_failed` | failure | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.meter_difference.viewed` | success | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.meter_save_failed` | failure | `createEnergyMeter` | `POST` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_save_failed` | failure | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_saved` | success | `createEnergyMeter` | `POST` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_saved` | success | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter.view_failed` | failure | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter.viewed` | success | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.reading_record_failed` | failure | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_recorded` | success | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_review_failed` | failure | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_reviewed` | success | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading.view_failed` | failure | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading.viewed` | success | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `entity_binding.change_failed` | failure | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `entity_binding.change_failed` | failure | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `entity_binding.changed` | success | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | GOV-平台治理 | `entity.binding.manage` | 已在事件矩阵正文出现 |
| `entity_binding.changed` | success | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.manage` | 已在事件矩阵正文出现 |
| `entity_binding.view_failed` | failure | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `entity_binding.viewed` | success | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_bind_failed` | failure | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_bound` | success | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_complete_failed` | failure | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_completed` | success | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_file_view_failed` | failure | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_file_viewed` | success | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_upload_failed` | failure | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `evidence.attachment_upload_failed` | failure | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded_failed` | failure | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.event_view_failed` | failure | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.event_viewed` | success | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.events_report_failed` | failure | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.events_reported` | success | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.review_create_failed` | failure | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.review_created` | success | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.timeline_view_failed` | failure | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.timeline_viewed` | success | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.traceability_auto_capture_rate_view_failed` | failure | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.traceability_auto_capture_rate_viewed` | success | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_created` | success | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_failed` | failure | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `exception.alert_view_failed` | failure | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `exception.alert_viewed` | success | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `exception.alerts_view_failed` | failure | `searchRiskAlerts` | `GET` | `/api/v1/evidence/risk-alerts` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `exception.alerts_viewed` | success | `searchRiskAlerts` | `GET` | `/api/v1/evidence/risk-alerts` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `exception.claim_failed` | failure | `claimRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/claim` | EVD-事件证据 | `risk_alert.claim` | 接口级事件，以本附录为准 |
| `exception.claimed` | success | `claimRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/claim` | EVD-事件证据 | `risk_alert.claim` | 接口级事件，以本附录为准 |
| `exception.dismiss_failed` | failure | `dismissRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/dismiss` | EVD-事件证据 | `risk_alert.dismiss` | 接口级事件，以本附录为准 |
| `exception.dismissed` | success | `dismissRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/dismiss` | EVD-事件证据 | `risk_alert.dismiss` | 接口级事件，以本附录为准 |
| `exception.escalate_failed` | failure | `escalateRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/escalate` | EVD-事件证据 | `risk_alert.escalate` | 接口级事件，以本附录为准 |
| `exception.escalated` | success | `escalateRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/escalate` | EVD-事件证据 | `risk_alert.escalate` | 接口级事件，以本附录为准 |
| `exception.report_failed` | failure | `reportRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts` | EVD-事件证据 | `risk_alert.report` | 接口级事件，以本附录为准 |
| `exception.reported` | success | `reportRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts` | EVD-事件证据 | `risk_alert.report` | 接口级事件，以本附录为准 |
| `exception.resolve_failed` | failure | `resolveEvidenceRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/resolve` | EVD-事件证据 | `risk_alert.resolve` | 接口级事件，以本附录为准 |
| `exception.resolved` | success | `resolveEvidenceRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/resolve` | EVD-事件证据 | `risk_alert.resolve` | 接口级事件，以本附录为准 |
| `exception.transfer_failed` | failure | `transferRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/transfer` | EVD-事件证据 | `risk_alert.transfer` | 接口级事件，以本附录为准 |
| `exception.transferred` | success | `transferRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/transfer` | EVD-事件证据 | `risk_alert.transfer` | 接口级事件，以本附录为准 |
| `facility.equipment_create_failed` | failure | `createFacilityEquipment` | `POST` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_created` | success | `createFacilityEquipment` | `POST` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_maintain_failed` | failure | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_maintained` | success | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_status_change_failed` | failure | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_status_changed` | success | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_update_failed` | failure | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_updated` | success | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment.maintenance_due_view_failed` | failure | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment.maintenance_due_viewed` | success | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment.view_failed` | failure | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment.viewed` | success | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_exception_report_failed` | failure | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | P0-待归类 | `facility.inspection.report_exception_assigned` | 接口级事件，以本附录为准 |
| `facility.inspection_exception_reported` | success | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | P0-待归类 | `facility.inspection.report_exception_assigned` | 接口级事件，以本附录为准 |
| `facility.inspection_record_submit_failed` | failure | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_record_submitted` | success | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_record.view_failed` | failure | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_record.viewed` | success | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_close_failed` | failure | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_closed` | success | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_create_failed` | failure | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_created` | success | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_recheck_failed` | failure | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_rechecked` | success | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_start_failed` | failure | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_started` | success | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task.view_failed` | failure | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task.view_failed` | failure | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task.viewed` | success | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task.viewed` | success | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template_create_failed` | failure | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template_created` | success | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template.view_failed` | failure | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template.viewed` | success | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.site_entry_completion_uploaded` | success | `uploadSiteEntryCompletion` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/completion` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry_report_accepted` | success | `acceptSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/accept` | P0-待归类 | `facility.site_entry.accept` | 接口级事件，以本附录为准 |
| `facility.site_entry.accept_failed` | failure | `acceptSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/accept` | P0-待归类 | `facility.site_entry.accept` | 接口级事件，以本附录为准 |
| `facility.site_entry.attachment_access_failed` | failure | `readSiteEntryAttachment` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/attachments/{attachment_id}/file` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.attachment_accessed` | success | `readSiteEntryAttachment` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/attachments/{attachment_id}/file` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.create_assigned` | success | `createSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.create_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.create_failed` | failure | `createSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.create_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.history_view_failed` | failure | `listSiteEntryHistory` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/history` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.history_viewed` | success | `listSiteEntryHistory` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/history` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.review` | success | `reviewSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/review` | P0-待归类 | `facility.site_entry.review` | 接口级事件，以本附录为准 |
| `facility.site_entry.review_failed` | failure | `reviewSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/review` | P0-待归类 | `facility.site_entry.review` | 接口级事件，以本附录为准 |
| `facility.site_entry.submit_assigned` | success | `submitSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/submit` | P0-待归类 | `facility.site_entry.submit_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.submit_failed` | failure | `submitSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/submit` | P0-待归类 | `facility.site_entry.submit_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.upload_completion_failed` | failure | `uploadSiteEntryCompletion` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/completion` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.view_failed` | failure | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.view_failed` | failure | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.viewed` | success | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.site_entry.viewed` | success | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `facility.work_order_accept_failed` | failure | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_accepted` | success | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_assign_failed` | failure | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_assigned` | success | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_assigned_list.view_failed` | failure | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | P0-待归类 | `facility.work_order.view_assigned` | 接口级事件，以本附录为准 |
| `facility.work_order_assigned_list.viewed` | success | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | P0-待归类 | `facility.work_order.view_assigned` | 接口级事件，以本附录为准 |
| `facility.work_order_complete_failed` | failure | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_completed` | success | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_create_failed` | failure | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_created` | success | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_overdue_mark_failed` | failure | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_overdue_marked` | success | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_policy_save_failed` | failure | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_policy_saved` | success | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_resume_failed` | failure | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_resumed` | success | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_review_failed` | failure | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_reviewed` | success | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_start_failed` | failure | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_started` | success | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_suspend_failed` | failure | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_suspended` | success | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.overdue_view_failed` | failure | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.overdue_viewed` | success | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.view_failed` | failure | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.view_failed` | failure | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.viewed` | success | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order.viewed` | success | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `finance_ar_ap.create_failed` | failure | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `finance_ar_ap.created` | success | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 已在事件矩阵正文出现 |
| `finance_ar_ap.export_failed` | failure | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | FIN-财务台账 | `finance.ar_ap.export` | 接口级事件，以本附录为准 |
| `finance_ar_ap.exported` | success | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | FIN-财务台账 | `finance.ar_ap.export` | 接口级事件，以本附录为准 |
| `finance_ar_ap.view_failed` | failure | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_ar_ap.viewed` | success | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_daily_close.lock_failed` | failure | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.locked` | success | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_payment.register_failed` | failure | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.register_failed` | failure | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.registered` | success | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_payment.registered` | success | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_property_fee.bill_failed` | failure | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.billed` | success | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_set` | success | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_set_failed` | failure | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_view_failed` | failure | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_viewed` | success | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_reconciliation.difference_found` | success | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance_reconciliation.view_failed` | failure | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance_refund.confirm_failed` | failure | `confirmFinanceRefundResult` | `POST` | `/api/v1/finance/refunds/{refund_id}/confirm` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.confirmed` | success | `confirmFinanceRefundResult` | `POST` | `/api/v1/finance/refunds/{refund_id}/confirm` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.create_failed` | failure | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.created` | success | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.execution_failed` | failure | `recordFinanceRefundExecution` | `POST` | `/api/v1/finance/refunds/{refund_id}/execution` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.execution_recorded` | success | `recordFinanceRefundExecution` | `POST` | `/api/v1/finance/refunds/{refund_id}/execution` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.list_failed` | failure | `listFinanceRefunds` | `GET` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_refund.listed` | success | `listFinanceRefunds` | `GET` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_refund.queried` | success | `queryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/query` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_refund.query_failed` | failure | `queryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/query` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_refund.retried` | success | `retryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/retry` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.retry_failed` | failure | `retryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/retry` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.view_failed` | failure | `getFinanceRefund` | `GET` | `/api/v1/finance/refunds/{refund_id}` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_refund.viewed` | success | `getFinanceRefund` | `GET` | `/api/v1/finance/refunds/{refund_id}` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.view_failed` | failure | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.viewed` | success | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.deposit.action_failed` | failure | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | FIN-财务台账 | `finance.deposit.manage` | 接口级事件，以本附录为准 |
| `finance.deposit.action_recorded` | success | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | FIN-财务台账 | `finance.deposit.manage` | 接口级事件，以本附录为准 |
| `finance.inventory_variance.post_failed` | failure | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | FIN-财务台账 | `finance.inventory_variance.post` | 接口级事件，以本附录为准 |
| `finance.inventory_variance.posted` | success | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | FIN-财务台账 | `finance.inventory_variance.post` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_approved` | success | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_decision_failed` | failure | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_request_failed` | failure | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_requested` | success | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed` | success | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed_failed` | failure | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_close_failed` | failure | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_closed` | success | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_create_failed` | failure | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.difference.create` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_created` | success | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.difference.create` | 接口级事件，以本附录为准 |
| `finance.turnover_rent.bill_failed` | failure | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `finance.turnover_rent.billed` | success | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `floor_plan.create_failed` | failure | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.created` | success | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_candidate_view_failed` | failure | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.import_candidate_viewed` | success | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.import_create_failed` | failure | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_created` | success | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_publish_failed` | failure | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_published` | success | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_view_failed` | failure | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.import_viewed` | success | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.shape_view_failed` | failure | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `floor_plan.shape_viewed` | success | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `floor_plan.shape.save` | success | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.shape.save_failed` | failure | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.view_failed` | failure | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `floor_plan.viewed` | success | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decide_failed` | failure | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decided` | success | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluate_failed` | failure | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluated` | success | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.policies_view_failed` | failure | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.policies_viewed` | success | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `integration.channel_reconciliation.execute_failed` | failure | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.channel_reconciliation.executed` | success | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.archive_failed` | failure | `archiveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps/{id}/archive` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.archived` | success | `archiveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps/{id}/archive` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.save_failed` | failure | `saveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.saved` | success | `saveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.view_failed` | failure | `listIntegrationChannelTicketTypeMaps` | `GET` | `/api/v1/integration/channel-ticket-type-maps` | INT-第三方集成 | `integration.channel_store_map.view` | 接口级事件，以本附录为准 |
| `integration.channel_ticket_type_map.viewed` | success | `listIntegrationChannelTicketTypeMaps` | `GET` | `/api/v1/integration/channel-ticket-type-maps` | INT-第三方集成 | `integration.channel_store_map.view` | 接口级事件，以本附录为准 |
| `integration.compensation_task.create_failed` | failure | `createCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.compensation.manage` | 接口级事件，以本附录为准 |
| `integration.compensation_task.created` | success | `createCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.compensation.manage` | 接口级事件，以本附录为准 |
| `integration.compensation_task.resolve_failed` | failure | `resolveCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks/{id}/resolve` | INT-第三方集成 | `integration.compensation.manage` | 接口级事件，以本附录为准 |
| `integration.compensation_task.resolved` | success | `resolveCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks/{id}/resolve` | INT-第三方集成 | `integration.compensation.manage` | 接口级事件，以本附录为准 |
| `integration.compensation_task.viewed` | success | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.compensation_task.viewed_failed` | failure | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device_event.viewed` | success | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device_event.viewed_failed` | failure | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device.viewed` | success | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device.viewed_failed` | failure | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.offline_sync_batch.viewed` | success | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.offline_sync_batch.viewed_failed` | failure | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.order_sync.create_failed` | failure | `createOrderSync` | `POST` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.order_sync.manage` | 接口级事件，以本附录为准 |
| `integration.order_sync.created` | success | `createOrderSync` | `POST` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.order_sync.manage` | 接口级事件，以本附录为准 |
| `integration.order_sync.viewed` | success | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.order_sync.viewed_failed` | failure | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.polling_check.record_failed` | failure | `recordIntegrationPollingCheck` | `POST` | `/api/v1/integration/polling-checks` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.polling_check.recorded` | success | `recordIntegrationPollingCheck` | `POST` | `/api/v1/integration/polling-checks` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.reconciliation_batch.create_failed` | failure | `createIntegrationReconciliationBatch` | `POST` | `/api/v1/integration/reconciliation/batches` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.reconciliation_batch.created` | success | `createIntegrationReconciliationBatch` | `POST` | `/api/v1/integration/reconciliation/batches` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.reconciliation_batch.view_failed` | failure | `listIntegrationReconciliationBatches` | `GET` | `/api/v1/integration/reconciliation/batches` | INT-第三方集成 | `integration.reconciliation.view` | 接口级事件，以本附录为准 |
| `integration.reconciliation_batch.viewed` | success | `listIntegrationReconciliationBatches` | `GET` | `/api/v1/integration/reconciliation/batches` | INT-第三方集成 | `integration.reconciliation.view` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.create_failed` | failure | `createIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.created` | success | `createIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.resolve_failed` | failure | `resolveIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences/{id}/resolve` | INT-第三方集成 | `integration.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.resolved` | success | `resolveIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences/{id}/resolve` | INT-第三方集成 | `integration.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.view_failed` | failure | `listIntegrationReconciliationDifferences` | `GET` | `/api/v1/integration/reconciliation/differences` | INT-第三方集成 | `integration.reconciliation.view` | 接口级事件，以本附录为准 |
| `integration.reconciliation_difference.viewed` | success | `listIntegrationReconciliationDifferences` | `GET` | `/api/v1/integration/reconciliation/differences` | INT-第三方集成 | `integration.reconciliation.view` | 接口级事件，以本附录为准 |
| `integration.retail_sku.viewed` | success | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.retail_sku.viewed_failed` | failure | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.retry_task.create_failed` | failure | `createRetryTask` | `POST` | `/api/v1/integration/retry-tasks` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.retry_task.created` | success | `createRetryTask` | `POST` | `/api/v1/integration/retry-tasks` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.retry_task.run_now` | success | `runRetryTaskNow` | `POST` | `/api/v1/integration/retry-tasks/{id}/run-now` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.retry_task.run_now_failed` | failure | `runRetryTaskNow` | `POST` | `/api/v1/integration/retry-tasks/{id}/run-now` | INT-第三方集成 | `integration.retry.manage` | 接口级事件，以本附录为准 |
| `integration.retry_task.viewed` | success | `listIntegrationRetryTasks` | `GET` | `/api/v1/integration/retry-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.retry_task.viewed_failed` | failure | `listIntegrationRetryTasks` | `GET` | `/api/v1/integration/retry-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.robot_report_failed` | failure | `reportIntegrationRobotTaskEvent` | `POST` | `/api/v1/integration/robot/report` | INT-第三方集成 | `not_applicable` | 接口级事件，以本附录为准 |
| `integration.robot_reported` | success | `reportIntegrationRobotTaskEvent` | `POST` | `/api/v1/integration/robot/report` | INT-第三方集成 | `not_applicable` | 接口级事件，以本附录为准 |
| `integration.robot_task_event.viewed` | success | `listRobotTaskEvents` | `GET` | `/api/v1/integration/robot/task-events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.robot_task_event.viewed_failed` | failure | `listRobotTaskEvents` | `GET` | `/api/v1/integration/robot/task-events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.robot_task.viewed` | success | `listRobotTasks` | `GET` | `/api/v1/integration/robot/tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.robot_task.viewed_failed` | failure | `listRobotTasks` | `GET` | `/api/v1/integration/robot/tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.video_camera.viewed` | success | `listVideoCameras` | `GET` | `/api/v1/integration/video/cameras` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.video_camera.viewed_failed` | failure | `listVideoCameras` | `GET` | `/api/v1/integration/video/cameras` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.video_clip_view_failed` | failure | `listIntegrationVideoAnchors` | `GET` | `/api/v1/integration/video-anchors` | INT-第三方集成 | `integration.video.view` | 接口级事件，以本附录为准 |
| `integration.video_clip_viewed` | success | `listIntegrationVideoAnchors` | `GET` | `/api/v1/integration/video-anchors` | INT-第三方集成 | `integration.video.view` | 接口级事件，以本附录为准 |
| `integration.voucher_verification.viewed` | success | `listVoucherVerifications` | `GET` | `/api/v1/integration/voucher-verifications` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.voucher_verification.viewed_failed` | failure | `listVoucherVerifications` | `GET` | `/api/v1/integration/voucher-verifications` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.webhook_received` | success | `receiveIntegrationTypedWebhook` | `POST` | `/api/v1/integration/{type}/webhook` | INT-第三方集成 | `not_applicable` | 接口级事件，以本附录为准 |
| `integration.webhook_rejected` | failure | `receiveIntegrationTypedWebhook` | `POST` | `/api/v1/integration/{type}/webhook` | INT-第三方集成 | `not_applicable` | 接口级事件，以本附录为准 |
| `integration.wecom_binding.viewed` | success | `listWecomBindings` | `GET` | `/api/v1/integration/wecom/bindings` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_binding.viewed_failed` | failure | `listWecomBindings` | `GET` | `/api/v1/integration/wecom/bindings` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_conversation.viewed` | success | `listWecomConversations` | `GET` | `/api/v1/integration/wecom/conversations` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_conversation.viewed_failed` | failure | `listWecomConversations` | `GET` | `/api/v1/integration/wecom/conversations` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_message.viewed` | success | `listWecomMessages` | `GET` | `/api/v1/integration/wecom/messages` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_message.viewed_failed` | failure | `listWecomMessages` | `GET` | `/api/v1/integration/wecom/messages` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_risk_hit.viewed` | success | `listIntegrationWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_risk_hit.viewed_failed` | failure | `listIntegrationWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `inventory.adjustment_failed` | failure | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.cost_ledger.view_failed` | failure | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_ledger.viewed` | success | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_summary.view_failed` | failure | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_summary.viewed` | success | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.counted` | success | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | INV-库存存酒 | `inventory.stock.adjust` | 已在事件矩阵正文出现 |
| `inventory.handover_create_failed` | failure | `createTraceHandover` | `POST` | `/api/v1/inventory/trace-handovers` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.handover_created` | success | `createTraceHandover` | `POST` | `/api/v1/inventory/trace-handovers` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.handover_exception_resolved` | success | `reviewTraceHandoverException` | `POST` | `/api/v1/inventory/trace-handovers/exceptions/{id}/review` | INV-库存存酒 | `inventory.handover.review` | 接口级事件，以本附录为准 |
| `inventory.handover_exception_review_failed` | failure | `reviewTraceHandoverException` | `POST` | `/api/v1/inventory/trace-handovers/exceptions/{id}/review` | INV-库存存酒 | `inventory.handover.review` | 接口级事件，以本附录为准 |
| `inventory.handover_robot_report_failed` | failure | `reportRobotSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/robot-report` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.handover_robot_reported` | success | `reportRobotSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/robot-report` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.handover_segment_scan_failed` | failure | `scanTraceSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/scan-segment` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.handover_segment_scanned` | success | `scanTraceSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/scan-segment` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `inventory.high_value_review_decide_failed` | failure | `reviewHighValue` | `POST` | `/api/v1/inventory/high-value-reviews/{id}/review` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.high_value_review_decided` | success | `reviewHighValue` | `POST` | `/api/v1/inventory/high-value-reviews/{id}/review` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.high_value_review.view_failed` | failure | `listHighValueReviews` | `GET` | `/api/v1/inventory/high-value-reviews` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.high_value_review.viewed` | success | `listHighValueReviews` | `GET` | `/api/v1/inventory/high-value-reviews` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.inbound_confirmed` | success | `createStockIn` | `POST` | `/api/v1/inventory/stock-in` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
| `inventory.inbound_failed` | failure | `createStockIn` | `POST` | `/api/v1/inventory/stock-in` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.item.created` | success | `createInventoryItem` | `POST` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.item.created_failed` | failure | `createInventoryItem` | `POST` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.item.viewed` | success | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.item.viewed_failed` | failure | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.outbound_verified` | success | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
| `inventory.purchase_request_change_failed` | failure | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_changed` | success | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_view_failed` | failure | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_viewed` | success | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_change_failed` | failure | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_change_failed` | failure | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_change_failed` | failure | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_changed` | success | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_changed` | success | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase_status_changed` | success | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.quarantine.disposition_complete_failed` | failure | `completeQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/dispositions/{id}/complete` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.quarantine.disposition_completed` | success | `completeQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/dispositions/{id}/complete` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.quarantine.disposition_request_failed` | failure | `requestQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/{id}/dispositions` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.quarantine.disposition_requested` | success | `requestQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/{id}/dispositions` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.quarantine.dispositions_view_failed` | failure | `listQuarantineDispositions` | `GET` | `/api/v1/inventory/quarantine/{id}/dispositions` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.quarantine.dispositions_viewed` | success | `listQuarantineDispositions` | `GET` | `/api/v1/inventory/quarantine/{id}/dispositions` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.quarantine.view_failed` | failure | `listInventoryQuarantine` | `GET` | `/api/v1/inventory/quarantine` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.quarantine.viewed` | success | `listInventoryQuarantine` | `GET` | `/api/v1/inventory/quarantine` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.quarantine.warehouses_view_failed` | failure | `listQuarantineWarehouses` | `GET` | `/api/v1/inventory/quarantine/warehouses` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.quarantine.warehouses_viewed` | success | `listQuarantineWarehouses` | `GET` | `/api/v1/inventory/quarantine/warehouses` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.received` | success | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.received_failed` | failure | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.risk_alert.list` | success | `listInventoryRiskAlerts` | `GET` | `/api/v1/inventory/risk-alerts` | INV-库存存酒 | `inventory.risk_alert.view` | 接口级事件，以本附录为准 |
| `inventory.risk_alert.list_failed` | failure | `listInventoryRiskAlerts` | `GET` | `/api/v1/inventory/risk-alerts` | INV-库存存酒 | `inventory.risk_alert.view` | 接口级事件，以本附录为准 |
| `inventory.risk.scan` | success | `scanInventoryRiskAlerts` | `POST` | `/api/v1/inventory/risk-alerts/scan` | INV-库存存酒 | `inventory.risk.scan` | 接口级事件，以本附录为准 |
| `inventory.risk.scan_failed` | failure | `scanInventoryRiskAlerts` | `POST` | `/api/v1/inventory/risk-alerts/scan` | INV-库存存酒 | `inventory.risk.scan` | 接口级事件，以本附录为准 |
| `inventory.stock_balance.viewed` | success | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_balance.viewed_failed` | failure | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_batch.create` | success | `createInventoryStockBatch` | `POST` | `/api/v1/inventory/stock-batches` | INV-库存存酒 | `inventory.stock.batch.manage` | 接口级事件，以本附录为准 |
| `inventory.stock_batch.create_failed` | failure | `createInventoryStockBatch` | `POST` | `/api/v1/inventory/stock-batches` | INV-库存存酒 | `inventory.stock.batch.manage` | 接口级事件，以本附录为准 |
| `inventory.stock_batch.list` | success | `listInventoryStockBatches` | `GET` | `/api/v1/inventory/stock-batches` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_batch.list_failed` | failure | `listInventoryStockBatches` | `GET` | `/api/v1/inventory/stock-batches` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_movement_failed` | failure | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.created` | success | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.created_failed` | failure | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.view_failed` | failure | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.viewed` | success | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_transfer.created` | success | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_transfer.created_failed` | failure | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stocktake.create_failed` | failure | `createInventoryStocktake` | `POST` | `/api/v1/inventory/stocktakes` | INV-库存存酒 | `inventory.stocktake.create` | 接口级事件，以本附录为准 |
| `inventory.stocktake.created` | success | `createInventoryStocktake` | `POST` | `/api/v1/inventory/stocktakes` | INV-库存存酒 | `inventory.stocktake.create` | 接口级事件，以本附录为准 |
| `inventory.stocktake.detail_view_failed` | failure | `getInventoryStocktake` | `GET` | `/api/v1/inventory/stocktakes/{id}` | INV-库存存酒 | `inventory.stocktake.view` | 接口级事件，以本附录为准 |
| `inventory.stocktake.detail_viewed` | success | `getInventoryStocktake` | `GET` | `/api/v1/inventory/stocktakes/{id}` | INV-库存存酒 | `inventory.stocktake.view` | 接口级事件，以本附录为准 |
| `inventory.stocktake.variance_record_failed` | failure | `recordInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances` | INV-库存存酒 | `inventory.stocktake.variance_record` | 接口级事件，以本附录为准 |
| `inventory.stocktake.variance_recorded` | success | `recordInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances` | INV-库存存酒 | `inventory.stocktake.variance_record` | 接口级事件，以本附录为准 |
| `inventory.stocktake.variance_review_failed` | failure | `reviewInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances/{vid}/review` | INV-库存存酒 | `inventory.stocktake.variance_review` | 接口级事件，以本附录为准 |
| `inventory.stocktake.variance_reviewed` | success | `reviewInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances/{vid}/review` | INV-库存存酒 | `inventory.stocktake.variance_review` | 接口级事件，以本附录为准 |
| `inventory.stocktake.view_failed` | failure | `listInventoryStocktakes` | `GET` | `/api/v1/inventory/stocktakes` | INV-库存存酒 | `inventory.stocktake.view` | 接口级事件，以本附录为准 |
| `inventory.stocktake.viewed` | success | `listInventoryStocktakes` | `GET` | `/api/v1/inventory/stocktakes` | INV-库存存酒 | `inventory.stocktake.view` | 接口级事件，以本附录为准 |
| `inventory.supplier_change_failed` | failure | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_changed` | success | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_qualification.create` | success | `createSupplierQualification` | `POST` | `/api/v1/inventory/supplier-qualifications` | INV-库存存酒 | `inventory.supplier.qualification.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_qualification.create_failed` | failure | `createSupplierQualification` | `POST` | `/api/v1/inventory/supplier-qualifications` | INV-库存存酒 | `inventory.supplier.qualification.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_qualification.list` | success | `listSupplierQualifications` | `GET` | `/api/v1/inventory/supplier-qualifications` | INV-库存存酒 | `inventory.supplier.qualification.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_qualification.list_failed` | failure | `listSupplierQualifications` | `GET` | `/api/v1/inventory/supplier-qualifications` | INV-库存存酒 | `inventory.supplier.qualification.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_reconciliation.create` | success | `createSupplierReconciliation` | `POST` | `/api/v1/inventory/supplier-reconciliations` | INV-库存存酒 | `inventory.supplier.reconcile` | 接口级事件，以本附录为准 |
| `inventory.supplier_reconciliation.create_failed` | failure | `createSupplierReconciliation` | `POST` | `/api/v1/inventory/supplier-reconciliations` | INV-库存存酒 | `inventory.supplier.reconcile` | 接口级事件，以本附录为准 |
| `inventory.supplier_reconciliation.list` | success | `listSupplierReconciliations` | `GET` | `/api/v1/inventory/supplier-reconciliations` | INV-库存存酒 | `inventory.supplier.reconcile` | 接口级事件，以本附录为准 |
| `inventory.supplier_reconciliation.list_failed` | failure | `listSupplierReconciliations` | `GET` | `/api/v1/inventory/supplier-reconciliations` | INV-库存存酒 | `inventory.supplier.reconcile` | 接口级事件，以本附录为准 |
| `inventory.supplier_settlement_method.create` | success | `createSupplierSettlementMethod` | `POST` | `/api/v1/inventory/supplier-settlement-methods` | INV-库存存酒 | `inventory.supplier.settlement.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_settlement_method.create_failed` | failure | `createSupplierSettlementMethod` | `POST` | `/api/v1/inventory/supplier-settlement-methods` | INV-库存存酒 | `inventory.supplier.settlement.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_settlement_method.list` | success | `listSupplierSettlementMethods` | `GET` | `/api/v1/inventory/supplier-settlement-methods` | INV-库存存酒 | `inventory.supplier.settlement.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_settlement_method.list_failed` | failure | `listSupplierSettlementMethods` | `GET` | `/api/v1/inventory/supplier-settlement-methods` | INV-库存存酒 | `inventory.supplier.settlement.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_view_failed` | failure | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.view` | 接口级事件，以本附录为准 |
| `inventory.supplier_viewed` | success | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.view` | 接口级事件，以本附录为准 |
| `inventory.supply_contract.list` | success | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_contract.list_failed` | failure | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.trace_code.list_failed` | failure | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.listed` | success | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.print_failed` | failure | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | INV-库存存酒 | `inventory.trace_code.print` | 接口级事件，以本附录为准 |
| `inventory.trace_code.printed` | success | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | INV-库存存酒 | `inventory.trace_code.print` | 接口级事件，以本附录为准 |
| `inventory.trace_code.register_failed` | failure | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.register` | 接口级事件，以本附录为准 |
| `inventory.trace_code.registered` | success | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.register` | 接口级事件，以本附录为准 |
| `inventory.trace_code.view_failed` | failure | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.viewed` | success | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_cancel_failed` | failure | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_cancelled` | success | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_return_failed` | failure | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_returned` | success | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.create_failed` | failure | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.created` | success | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.list` | success | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.list_failed` | failure | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.blacklist_change_failed` | failure | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | P0-待归类 | `leasing.brand.blacklist` | 接口级事件，以本附录为准 |
| `leasing.brand.blacklist_changed` | success | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | P0-待归类 | `leasing.brand.blacklist` | 接口级事件，以本附录为准 |
| `leasing.brand.create_failed` | failure | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.created` | success | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.view_failed` | failure | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.viewed` | success | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.create_failed` | failure | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.created` | success | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.view_failed` | failure | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.viewed` | success | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.advance_failed` | failure | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.advanced` | success | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.create_failed` | failure | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.created` | success | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.sign_failed` | failure | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | P0-待归类 | `leasing.convert` | 接口级事件，以本附录为准 |
| `leasing.intent.signed` | success | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | P0-待归类 | `leasing.convert` | 接口级事件，以本附录为准 |
| `leasing.intent.view_failed` | failure | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.viewed` | success | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.assign_failed` | failure | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.assigned` | success | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.contact_phone_view_failed` | failure | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.contact_phone_viewed` | success | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.create_failed` | failure | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.created` | success | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.status_change_failed` | failure | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.status_changed` | success | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.view_failed` | failure | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.view_failed` | failure | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.viewed` | success | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.viewed` | success | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `mdm.app_whitelist.change_failed` | failure | `upsertMdmAppWhitelist` | `PUT` | `/api/v1/terminals/mdm/app-whitelist/{id}` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.app_whitelist.changed` | success | `upsertMdmAppWhitelist` | `PUT` | `/api/v1/terminals/mdm/app-whitelist/{id}` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.app_whitelist.view_failed` | failure | `listMdmAppWhitelist` | `GET` | `/api/v1/terminals/mdm/app-whitelist` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.app_whitelist.viewed` | success | `listMdmAppWhitelist` | `GET` | `/api/v1/terminals/mdm/app-whitelist` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.policy_check_failed` | failure | `enrollMdmDevice` | `POST` | `/api/v1/terminals/mdm/devices/enroll` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `mdm.policy_checked` | success | `enrollMdmDevice` | `POST` | `/api/v1/terminals/mdm/devices/enroll` | TERM-终端设备 | `mdm.device.manage` | 已在事件矩阵正文出现 |
| `mdm.site_whitelist.change_failed` | failure | `upsertMdmSiteWhitelist` | `PUT` | `/api/v1/terminals/mdm/site-whitelist/{id}` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.site_whitelist.changed` | success | `upsertMdmSiteWhitelist` | `PUT` | `/api/v1/terminals/mdm/site-whitelist/{id}` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.site_whitelist.view_failed` | failure | `listMdmSiteWhitelist` | `GET` | `/api/v1/terminals/mdm/site-whitelist` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.site_whitelist.viewed` | success | `listMdmSiteWhitelist` | `GET` | `/api/v1/terminals/mdm/site-whitelist` | TERM-终端设备 | `mdm.policy.manage` | 接口级事件，以本附录为准 |
| `mdm.violation.detect` | success | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `mdm.violation.detect_failed` | failure | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `mdm.violation.handle` | success | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | TERM-终端设备 | `mdm.violation.handle` | 接口级事件，以本附录为准 |
| `mdm.violation.handle_failed` | failure | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | TERM-终端设备 | `mdm.violation.handle` | 接口级事件，以本附录为准 |
| `mdm.violation.view_failed` | failure | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `mdm.violation.viewed` | success | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `member.level_changed` | success | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `merchant_billing.bill.view_failed` | failure | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `merchant_billing.bill.viewed` | success | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `merchant_expense.auto_sync_failed` | failure | `syncMerchantExpenseAuto` | `POST` | `/api/v1/finance/merchant-expenses/sync-auto` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.auto_synced` | success | `syncMerchantExpenseAuto` | `POST` | `/api/v1/finance/merchant-expenses/sync-auto` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.list_failed` | failure | `listMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_expense.listed` | success | `listMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_expense.manual_create_failed` | failure | `createMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.manual_created` | success | `createMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.period_locks_view_failed` | failure | `listMerchantExpensePeriodLocks` | `GET` | `/api/v1/finance/merchant-expenses/period-locks` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_expense.period_locks_viewed` | success | `listMerchantExpensePeriodLocks` | `GET` | `/api/v1/finance/merchant-expenses/period-locks` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_expense.reverse_failed` | failure | `reverseMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses/{id}/reverse` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.reversed` | success | `reverseMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses/{id}/reverse` | FIN-财务台账 | `finance.merchant_expense.manage` | 接口级事件，以本附录为准 |
| `merchant_expense.summary_view_failed` | failure | `summarizeMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses/summary` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_expense.summary_viewed` | success | `summarizeMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses/summary` | FIN-财务台账 | `finance.merchant_expense.view` | 接口级事件，以本附录为准 |
| `merchant_portal.contract.view_failed` | failure | `listMerchantPortalContracts` | `GET` | `/api/v1/merchant-portal/contracts` | P0-待归类 | `contract.view` | 接口级事件，以本附录为准 |
| `merchant_portal.contract.viewed` | success | `listMerchantPortalContracts` | `GET` | `/api/v1/merchant-portal/contracts` | P0-待归类 | `contract.view` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.dispute_failed` | failure | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.disputed` | success | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.view_failed` | failure | `listMerchantPortalSettlements` | `GET` | `/api/v1/merchant-portal/settlements` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.viewed` | success | `listMerchantPortalSettlements` | `GET` | `/api/v1/merchant-portal/settlements` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `merchant_service.addMerchantServiceWorkOrderAction.failed` | failure | `addMerchantServiceWorkOrderAction` | `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.addMerchantServiceWorkOrderAction.succeeded` | success | `addMerchantServiceWorkOrderAction` | `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.assignMerchantServiceWorkOrder.failed` | failure | `assignMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.assignMerchantServiceWorkOrder.succeeded` | success | `assignMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.closeMerchantServiceWorkOrder.failed` | failure | `closeMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.closeMerchantServiceWorkOrder.succeeded` | success | `closeMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.createMerchantServiceWorkOrder.failed` | failure | `createMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.createMerchantServiceWorkOrder.succeeded` | success | `createMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.listMerchantServiceWorkOrders.failed` | failure | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `merchant_service.listMerchantServiceWorkOrders.succeeded` | success | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `merchant.billing.confirm` | success | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | P0-待归类 | `merchant.billing.confirm` | 接口级事件，以本附录为准 |
| `merchant.billing.confirm_failed` | failure | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | P0-待归类 | `merchant.billing.confirm` | 接口级事件，以本附录为准 |
| `merchant.billing.create` | success | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `merchant.billing.create_failed` | failure | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `merchant.billing.dispute.submit` | success | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | P0-待归类 | `merchant.billing.dispute.submit` | 接口级事件，以本附录为准 |
| `merchant.billing.dispute.submit_failed` | failure | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | P0-待归类 | `merchant.billing.dispute.submit` | 接口级事件，以本附录为准 |
| `merchant.billing.dunning.record` | success | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | P0-待归类 | `merchant.billing.dunning.record` | 接口级事件，以本附录为准 |
| `merchant.billing.dunning.record_failed` | failure | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | P0-待归类 | `merchant.billing.dunning.record` | 接口级事件，以本附录为准 |
| `merchant.update_failed` | failure | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.update_failed` | failure | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.view_failed` | failure | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `open_mapping.changed` | success | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | INT-第三方集成 | `integration.provider_adapter.update` | 已在事件矩阵正文出现 |
| `operating_params.change_failed` | failure | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `operating_params.changed` | success | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `operation_handover.created` | success | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `operation_handover.failed` | failure | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `partner_org.update_failed` | failure | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner_org.updated` | success | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner_org.view_failed` | failure | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.view` | 接口级事件，以本附录为准 |
| `partner_org.viewed` | success | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.view` | 接口级事件，以本附录为准 |
| `partner_subordinate_completion.view_failed` | failure | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `partner_subordinate_completion.viewed` | success | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `partner_subordinate.create_failed` | failure | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.created` | success | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.disable_failed` | failure | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.disabled` | success | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.view_failed` | failure | `listPartnerSubordinates` | `GET` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `partner_subordinate.viewed` | success | `listPartnerSubordinates` | `GET` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `partner.org.user_bind_failed` | failure | `bindUserToPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/bind-user` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner.org.user_bound` | success | `bindUserToPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/bind-user` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner.org.user_unbind_failed` | failure | `unbindUserFromPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/unbind-user` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner.org.user_unbound` | success | `unbindUserFromPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/unbind-user` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `payment.intent_created` | success | `createBarBillPayIntent` | `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | BAR-酒吧经营 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `payment.intent_created` | success | `createPaymentIntent` | `POST` | `/api/v1/finance/payment-intents` | FIN-财务台账 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `payment.intent_created` | success | `createTicketOrderPayIntent` | `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | TIC-票务权益 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `payment.intent_failed` | failure | `createBarBillPayIntent` | `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | BAR-酒吧经营 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `payment.intent_failed` | failure | `createPaymentIntent` | `POST` | `/api/v1/finance/payment-intents` | FIN-财务台账 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `payment.intent_failed` | failure | `createTicketOrderPayIntent` | `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | TIC-票务权益 | `finance.payment_intent.create` | 接口级事件，以本附录为准 |
| `permission_template.applied` | success | `applyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/apply` | AUTH-账号权限 | `access.permission_template.apply` | 已在事件矩阵正文出现 |
| `permission_template.applied` | success | `createTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants` | AUTH-账号权限 | `access.temporary_grant.create` | 已在事件矩阵正文出现 |
| `permission_template.apply_failed` | failure | `applyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/apply` | AUTH-账号权限 | `access.permission_template.apply` | 接口级事件，以本附录为准 |
| `permission_template.change_failed` | failure | `createPermissionTemplate` | `POST` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.manage` | 接口级事件，以本附录为准 |
| `permission_template.change_failed` | failure | `updatePermissionTemplate` | `PUT` | `/api/v1/access/permission-templates/{id}` | AUTH-账号权限 | `access.permission_template.manage` | 接口级事件，以本附录为准 |
| `permission_template.changed` | success | `createPermissionTemplate` | `POST` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.manage` | 接口级事件，以本附录为准 |
| `permission_template.changed` | success | `updatePermissionTemplate` | `PUT` | `/api/v1/access/permission-templates/{id}` | AUTH-账号权限 | `access.permission_template.manage` | 接口级事件，以本附录为准 |
| `permission_template.copied` | success | `copyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/copy` | AUTH-账号权限 | `access.permission_template.manage` | 已在事件矩阵正文出现 |
| `permission_template.copy_failed` | failure | `copyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/copy` | AUTH-账号权限 | `access.permission_template.manage` | 接口级事件，以本附录为准 |
| `permission_template.revoked` | success | `revokeTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | AUTH-账号权限 | `access.temporary_grant.revoke` | 接口级事件，以本附录为准 |
| `permission_template.view_failed` | failure | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.view` | 接口级事件，以本附录为准 |
| `permission_template.viewed` | success | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.view` | 接口级事件，以本附录为准 |
| `permission.view_failed` | failure | `listPermissions` | `GET` | `/api/v1/access/permissions` | AUTH-账号权限 | `access.permission.view` | 接口级事件，以本附录为准 |
| `permission.viewed` | success | `listPermissions` | `GET` | `/api/v1/access/permissions` | AUTH-账号权限 | `access.permission.view` | 接口级事件，以本附录为准 |
| `production_readiness.check_failed` | failure | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | OPS-生产前置 | `ops.production_readiness.check` | 已在事件矩阵正文出现 |
| `production_readiness.checked` | success | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | OPS-生产前置 | `ops.production_readiness.check` | 已在事件矩阵正文出现 |
| `production_readiness.update_failed` | failure | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | OPS-生产前置 | `ops.production_readiness.manage` | 已在事件矩阵正文出现 |
| `production_readiness.updated` | success | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | OPS-生产前置 | `ops.production_readiness.manage` | 已在事件矩阵正文出现 |
| `production_readiness.view_failed` | failure | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | OPS-生产前置 | `ops.production_readiness.view` | 已在事件矩阵正文出现 |
| `production_readiness.viewed` | success | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | OPS-生产前置 | `ops.production_readiness.view` | 已在事件矩阵正文出现 |
| `project.create_failed` | failure | `createProject` | `POST` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.manage` | 接口级事件，以本附录为准 |
| `project.created` | success | `createProject` | `POST` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.manage` | 接口级事件，以本附录为准 |
| `project.view_failed` | failure | `listProjects` | `GET` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.view` | 接口级事件，以本附录为准 |
| `project.viewed` | success | `listProjects` | `GET` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.view` | 接口级事件，以本附录为准 |
| `provider_adapter.create_failed` | failure | `createProviderAdapter` | `POST` | `/api/v1/integration/provider-adapters` | INT-第三方集成 | `integration.provider_adapter.create` | 接口级事件，以本附录为准 |
| `provider_adapter.created` | success | `createProviderAdapter` | `POST` | `/api/v1/integration/provider-adapters` | INT-第三方集成 | `integration.provider_adapter.create` | 接口级事件，以本附录为准 |
| `provider_adapter.production_ready_failed` | failure | `markProviderAdapterProductionReady` | `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | INT-第三方集成 | `integration.provider_adapter.production_ready` | 接口级事件，以本附录为准 |
| `provider_adapter.production_ready_marked` | success | `markProviderAdapterProductionReady` | `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | INT-第三方集成 | `integration.provider_adapter.production_ready` | 接口级事件，以本附录为准 |
| `provider_adapter.test_failed` | failure | `testProviderAdapterMock` | `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | INT-第三方集成 | `integration.provider_adapter.test` | 接口级事件，以本附录为准 |
| `provider_adapter.tested` | success | `testProviderAdapterMock` | `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | INT-第三方集成 | `integration.provider_adapter.test` | 接口级事件，以本附录为准 |
| `provider_adapter.update_failed` | failure | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | INT-第三方集成 | `integration.provider_adapter.update` | 接口级事件，以本附录为准 |
| `provider_adapter.view_failed` | failure | `listProviderAdapters` | `GET` | `/api/v1/integration/provider-adapters` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `provider_adapter.viewed` | success | `listProviderAdapters` | `GET` | `/api/v1/integration/provider-adapters` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `refund_authority_limit.view_failed` | failure | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `refund_authority_limit.viewed` | success | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `refund_authority.change_failed` | failure | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.change_failed` | failure | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.changed` | success | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.changed` | success | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_auto_rule.view_failed` | failure | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `refund_auto_rule.viewed` | success | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `risk_alert.create_failed` | failure | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.created` | success | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.view_failed` | failure | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `risk_alert.viewed` | success | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `role.change_failed` | failure | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.changed` | success | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.view_failed` | failure | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
| `role.viewable_projects.change_failed` | failure | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.changed` | success | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.view_failed` | failure | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.viewed` | success | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewed` | success | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
| `service.catalog.save_failed` | failure | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.saved` | success | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.view_failed` | failure | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.catalog.view_failed` | failure | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.viewed` | success | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.catalog.viewed` | success | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.order.accept_failed` | failure | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.accepted` | success | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.cancel_failed` | failure | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.cancelled` | success | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.complete_failed` | failure | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.completed` | success | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.create_failed` | failure | `createServiceOrder` | `POST` | `/api/v1/services/orders` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.created` | success | `createServiceOrder` | `POST` | `/api/v1/services/orders` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.list_failed` | failure | `listServiceOrders` | `GET` | `/api/v1/services/orders` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.order.listed` | success | `listServiceOrders` | `GET` | `/api/v1/services/orders` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.order.mine_view_failed` | failure | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.mine_viewed` | success | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.paid` | success | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.pay_confirm_failed` | failure | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.stats_view_failed` | failure | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.stats_viewed` | success | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.pay_config.save_failed` | failure | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.pay_config.saved` | success | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.pay_config.view_failed` | failure | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.pay_config.viewed` | success | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.payment_code.present_failed` | failure | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.payment_code.presented` | success | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.payment_code.save_failed` | failure | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.payment_code.saved` | success | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.payment_code.view_failed` | failure | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.payment_code.viewed` | success | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `settlement.bill.post_failed` | failure | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | P0-待归类 | `settlement.bill.post` | 接口级事件，以本附录为准 |
| `settlement.bill.posted` | success | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | P0-待归类 | `settlement.bill.post` | 接口级事件，以本附录为准 |
| `settlement.bill.special_approve_failed` | failure | `settlementSpecialApproveBill` | `POST` | `/api/v1/settlement/bills/{id}/special-approval` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.bill.special_approved` | success | `settlementSpecialApproveBill` | `POST` | `/api/v1/settlement/bills/{id}/special-approval` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.config.archive_failed` | failure | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.archived` | success | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.bar_commission_rate_configs.update_failed` | failure | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.bar_commission_rate_configs.updated` | success | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.channel_fee_configs.update_failed` | failure | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.channel_fee_configs.updated` | success | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.comprehensive_service_fee_configs.update_failed` | failure | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.comprehensive_service_fee_configs.updated` | success | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.high_value_thresholds.update_failed` | failure | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.high_value_thresholds.updated` | success | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.update_failed` | failure | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.updated` | success | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.view_failed` | failure | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.viewed` | success | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `settlement.config.overview.view_failed` | failure | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `settlement.config.overview.viewed` | success | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `settlement.config.pass_gift_cost_configs.update_failed` | failure | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.pass_gift_cost_configs.updated` | success | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.platform_retention_rules.update_failed` | failure | `settlementConfigUpsertRetention` | `POST` | `/api/v1/settlement/config/retention` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.platform_retention_rules.updated` | success | `settlementConfigUpsertRetention` | `POST` | `/api/v1/settlement/config/retention` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.points_accrual_rate_configs.update_failed` | failure | `settlementConfigUpsertPointsAccrualRate` | `POST` | `/api/v1/settlement/config/points-accrual-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.points_accrual_rate_configs.updated` | success | `settlementConfigUpsertPointsAccrualRate` | `POST` | `/api/v1/settlement/config/points-accrual-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.revenue_stream_attribution.update_failed` | failure | `settlementConfigUpsertRevenueStreamAttribution` | `POST` | `/api/v1/settlement/config/revenue-stream-attribution` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.revenue_stream_attribution.updated` | success | `settlementConfigUpsertRevenueStreamAttribution` | `POST` | `/api/v1/settlement/config/revenue-stream-attribution` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_combo_discount_configs.update_failed` | failure | `settlementConfigUpsertComboDiscount` | `POST` | `/api/v1/settlement/config/combo-discount` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_combo_discount_configs.updated` | success | `settlementConfigUpsertComboDiscount` | `POST` | `/api/v1/settlement/config/combo-discount` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_configs.update_failed` | failure | `settlementConfigUpsertDistributionMethod` | `POST` | `/api/v1/settlement/config/distribution-method` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_configs.updated` | success | `settlementConfigUpsertDistributionMethod` | `POST` | `/api/v1/settlement/config/distribution-method` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_merchant_rates.update_failed` | failure | `settlementConfigUpsertMerchantRate` | `POST` | `/api/v1/settlement/config/merchant-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_merchant_rates.updated` | success | `settlementConfigUpsertMerchantRate` | `POST` | `/api/v1/settlement/config/merchant-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_rank_tiers.update_failed` | failure | `settlementConfigUpsertRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.ticket_distribution_rank_tiers.updated` | success | `settlementConfigUpsertRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.generate_failed` | failure | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.generated` | success | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.list` | success | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `settlement.invoice.list_failed` | failure | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `settlement.invoice.status_change_failed` | failure | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.status_changed` | success | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.summary` | success | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `settlement.invoice.summary_failed` | failure | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `space_type.update_failed` | failure | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.updated` | success | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.view_failed` | failure | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `space_type.viewed` | success | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `space.update_failed` | failure | `createSpace` | `POST` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space.updated` | success | `createSpace` | `POST` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space.view_failed` | failure | `listSpaces` | `GET` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `space.viewed` | success | `listSpaces` | `GET` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `staff_device.bind_failed` | failure | `bindStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials` | AUTH-账号权限 | `staff.device.bind` | 接口级事件，以本附录为准 |
| `staff_device.bound` | success | `bindStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials` | AUTH-账号权限 | `staff.device.bind` | 接口级事件，以本附录为准 |
| `staff_device.integrity_report_failed` | failure | `reportStaffDeviceIntegrity` | `POST` | `/api/v1/staff/device-credentials/{id}/report-integrity` | AUTH-账号权限 | `staff.device.bind` | 接口级事件，以本附录为准 |
| `staff_device.integrity_reported` | success | `reportStaffDeviceIntegrity` | `POST` | `/api/v1/staff/device-credentials/{id}/report-integrity` | AUTH-账号权限 | `staff.device.bind` | 接口级事件，以本附录为准 |
| `staff_device.rebind_failed` | failure | `approveStaffDeviceRebind` | `POST` | `/api/v1/staff/device-credentials/{id}/approve-rebind` | AUTH-账号权限 | `staff.device.review` | 接口级事件，以本附录为准 |
| `staff_device.rebound` | success | `approveStaffDeviceRebind` | `POST` | `/api/v1/staff/device-credentials/{id}/approve-rebind` | AUTH-账号权限 | `staff.device.review` | 接口级事件，以本附录为准 |
| `staff_device.revoke_failed` | failure | `revokeStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials/{id}/revoke` | AUTH-账号权限 | `staff.device.revoke` | 接口级事件，以本附录为准 |
| `staff_device.revoked` | success | `revokeStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials/{id}/revoke` | AUTH-账号权限 | `staff.device.revoke` | 接口级事件，以本附录为准 |
| `staff.approved` | success | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.attendance_mine.view_failed` | failure | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance_mine.viewed` | success | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance_month.view_failed` | failure | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance_month.viewed` | success | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance.anchor_failed` | failure | `issueStaffPhoneTimeAnchor` | `POST` | `/api/v1/staff/attendance/time-anchors` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.anchor_issued` | success | `issueStaffPhoneTimeAnchor` | `POST` | `/api/v1/staff/attendance/time-anchors` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.context_view_failed` | failure | `getCurrentStaffWorkPhoneContext` | `GET` | `/api/v1/staff/attendance/work-phone-context` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.context_viewed` | success | `getCurrentStaffWorkPhoneContext` | `GET` | `/api/v1/staff/attendance/work-phone-context` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.contexts_view_failed` | failure | `listStaffWorkPhoneContextsForRecovery` | `GET` | `/api/v1/staff/attendance/work-phone-contexts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.contexts_viewed` | success | `listStaffWorkPhoneContextsForRecovery` | `GET` | `/api/v1/staff/attendance/work-phone-contexts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.punch_failed` | failure | `punchStaffAttendance` | `POST` | `/api/v1/staff/attendance/punch` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.punched` | success | `punchStaffAttendance` | `POST` | `/api/v1/staff/attendance/punch` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_failed` | failure | `recordStaffPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_recorded` | success | `recordStaffPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events` | STAFF-员工 | `staff.attendance.punch` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_recover_failed` | failure | `recoverStaffWorkPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events/recover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_recovered` | success | `recoverStaffWorkPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events/recover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_review_submit_failed` | failure | `submitStaffAttendanceReceiptReview` | `POST` | `/api/v1/staff/attendance/receipt-events/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_review_submitted` | success | `submitStaffAttendanceReceiptReview` | `POST` | `/api/v1/staff/attendance/receipt-events/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_review_view_failed` | failure | `listStaffAttendanceReceiptReviews` | `GET` | `/api/v1/staff/attendance/receipt-events/{id}/reviews` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance.receipt_review_viewed` | success | `listStaffAttendanceReceiptReviews` | `GET` | `/api/v1/staff/attendance/receipt-events/{id}/reviews` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance.receipts_view_failed` | failure | `listStaffPhoneReceipts` | `GET` | `/api/v1/staff/attendance/receipt-events` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance.receipts_viewed` | success | `listStaffPhoneReceipts` | `GET` | `/api/v1/staff/attendance/receipt-events` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.attendance.view_failed` | failure | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | STAFF-员工 | `staff.attendance.view` | 接口级事件，以本附录为准 |
| `staff.attendance.viewed` | success | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | STAFF-员工 | `staff.attendance.view` | 接口级事件，以本附录为准 |
| `staff.business_handover.assign_failed` | failure | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.assigned` | success | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.view_failed` | failure | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.viewed` | success | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.close_failed` | failure | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.closed` | success | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.view_failed` | failure | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.viewed` | success | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.dispatch.create_failed` | failure | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.created` | success | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.end_failed` | failure | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.ended` | success | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.view_failed` | failure | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.dispatch.viewed` | success | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.leave_mine.view_failed` | failure | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave_mine.viewed` | success | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.review_failed` | failure | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.reviewed` | success | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.submit_failed` | failure | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.submitted` | success | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.view_failed` | failure | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.viewed` | success | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.locker.record_failed` | failure | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | STAFF-员工 | `not_applicable` | 接口级事件，以本附录为准 |
| `staff.locker.recorded` | success | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | STAFF-员工 | `not_applicable` | 接口级事件，以本附录为准 |
| `staff.locker.scan_token_failed` | failure | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `staff.locker.scan_token_issued` | success | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `staff.notice_acknowledge_failed` | failure | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice_acknowledged` | success | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice.view_failed` | failure | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice.viewed` | success | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.payroll_mine.view_failed` | failure | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll_mine.viewed` | success | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_add_failed` | failure | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeal_submit_failed` | failure | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeal_submitted` | success | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeals_view_failed` | failure | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeals_viewed` | success | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_requested` | success | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.base_save_failed` | failure | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.base_saved` | success | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.commission_add_failed` | failure | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.commission_added` | success | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.month_close_failed` | failure | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.month_closed` | success | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_request_failed` | failure | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_requested` | success | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_view_failed` | failure | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_viewed` | success | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_change_failed` | failure | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_changed` | success | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_view_failed` | failure | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_viewed` | success | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.view_failed` | failure | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.viewed` | success | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.profile.employment_status_change_failed` | failure | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.employment_status_change_requested` | success | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.sales_position_change_failed` | failure | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.sales_position_changed` | success | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.view_failed` | failure | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.profile.viewed` | success | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.register_failed` | failure | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.registered` | success | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.registration_companies_view_failed` | failure | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.registration_companies_viewed` | success | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.review_failed` | failure | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.self_register_failed` | failure | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.self_registered` | success | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.shift_mine.view_failed` | failure | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.shift_mine.viewed` | success | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `staff.shift.cover_failed` | failure | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.covered` | success | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.covers_view_failed` | failure | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.covers_viewed` | success | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.history_view_failed` | failure | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.history_viewed` | success | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.save_failed` | failure | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.save_failed` | failure | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.submitted` | success | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.submitted` | success | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.view_failed` | failure | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.viewed` | success | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_change_failed` | failure | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_changed` | success | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_view_failed` | failure | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_viewed` | success | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `stored_value.bonus_policy_config_failed` | failure | `configureStoredValueBonusPolicy` | `POST` | `/api/v1/loyalty/stored-value/bonus-policies` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.bonus_policy_configured` | success | `configureStoredValueBonusPolicy` | `POST` | `/api/v1/loyalty/stored-value/bonus-policies` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.bonus_policy_view_failed` | failure | `getStoredValueBonusPolicy` | `GET` | `/api/v1/loyalty/stored-value/bonus-policies/{project_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.bonus_policy_viewed` | success | `getStoredValueBonusPolicy` | `GET` | `/api/v1/loyalty/stored-value/bonus-policies/{project_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.counter_refund` | success | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.counter_refund_failed` | failure | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.frozen` | success | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.frozen_failed` | failure | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.merchant_card_config_failed` | failure | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_card_configured` | success | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_configured` | success | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_failed` | failure | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_view_failed` | failure | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_viewed` | success | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.refund` | success | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.refund_failed` | failure | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.unfrozen` | success | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.unfrozen_failed` | failure | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `supply_contract.approve_failed` | failure | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `supply_contract.approved` | success | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `supply_contract.create_failed` | failure | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `supply_contract.created` | success | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `system.health_check_failed` | failure | `getSystemHealth` | `GET` | `/api/v1/system/health` | SYS-系统健康 | `public` | 接口级事件，以本附录为准 |
| `system.health_checked` | success | `getSystemHealth` | `GET` | `/api/v1/system/health` | SYS-系统健康 | `public` | 接口级事件，以本附录为准 |
| `system.version_view_failed` | failure | `getSystemVersion` | `GET` | `/api/v1/system/version` | SYS-系统健康 | `system.version.view` | 接口级事件，以本附录为准 |
| `system.version_viewed` | success | `getSystemVersion` | `GET` | `/api/v1/system/version` | SYS-系统健康 | `system.version.view` | 接口级事件，以本附录为准 |
| `temporary_grant.create_failed` | failure | `createTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants` | AUTH-账号权限 | `access.temporary_grant.create` | 接口级事件，以本附录为准 |
| `temporary_grant.revoke_failed` | failure | `revokeTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | AUTH-账号权限 | `access.temporary_grant.revoke` | 接口级事件，以本附录为准 |
| `terminal_canary.rollout_change_failed` | failure | `upsertTerminalCanaryRollout` | `PUT` | `/api/v1/terminals/canary-rollouts` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.rollout_changed` | success | `upsertTerminalCanaryRollout` | `PUT` | `/api/v1/terminals/canary-rollouts` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.target_add_failed` | failure | `addTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-rollouts/{id}/targets` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.target_added` | success | `addTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-rollouts/{id}/targets` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.target_archive_failed` | failure | `archiveTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-targets/{id}/archive` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.target_archived` | success | `archiveTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-targets/{id}/archive` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_canary.view_failed` | failure | `listTerminalCanaryRollouts` | `GET` | `/api/v1/terminals/canary-rollouts` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal_canary.viewed` | success | `listTerminalCanaryRollouts` | `GET` | `/api/v1/terminals/canary-rollouts` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal_switch.change_failed` | failure | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_switch.changed` | success | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_switch.effective_view_failed` | failure | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal_switch.effective_viewed` | success | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal_switch.view_failed` | failure | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal_switch.viewed` | success | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.access_rejected` | failure | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.device.bind_failed` | failure | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.bound` | success | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.found` | success | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.found_failed` | failure | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.lost` | success | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.lost_failed` | failure | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.merchant_change_failed` | failure | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.merchant_changed` | success | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repair` | success | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repair_failed` | failure | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.returned` | success | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.returned_failed` | failure | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.scrap` | success | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.scrap_failed` | failure | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.signing_key_register_failed` | failure | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.signing_key_registered` | success | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.unbind_failed` | failure | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.device.unbound` | success | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.device.view_failed` | failure | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.device.viewed` | success | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.kiosk_config_viewed` | success | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat` | success | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat_failed` | failure | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.offline_entitlements.viewed` | success | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.offline_entitlements.viewed_failed` | failure | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.sync_task.accepted` | success | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.sync_task.accepted_failed` | failure | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.work_phone.swap_failed` | failure | `swapStaffWorkPhone` | `POST` | `/api/v1/terminals/devices/work-phone-swap` | P0-待归类 | `terminal.work_phone.swap` | 接口级事件，以本附录为准 |
| `terminal.work_phone.swapped` | success | `swapStaffWorkPhone` | `POST` | `/api/v1/terminals/devices/work-phone-swap` | P0-待归类 | `terminal.work_phone.swap` | 接口级事件，以本附录为准 |
| `ticket_credential.refresh_failed` | failure | `getCustomerTicketQr` | `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_credential.refresh_failed` | failure | `getTicketCredential` | `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | TIC-票务权益 | `ticket.credential.view` | 接口级事件，以本附录为准 |
| `ticket_credential.refreshed` | success | `getCustomerTicketQr` | `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | CUS/TIC-客人购票 | `not_applicable` | 已在事件矩阵正文出现 |
| `ticket_credential.refreshed` | success | `getTicketCredential` | `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | TIC-票务权益 | `ticket.credential.view` | 已在事件矩阵正文出现 |
| `ticket_dynamic_pricing.created.failure` | failure | `createTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules` | TIC-票务权益 | `ticket.dynamic_pricing.manage` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.created.success` | success | `createTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules` | TIC-票务权益 | `ticket.dynamic_pricing.manage` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.published.failure` | failure | `publishTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules/{id}/publish` | TIC-票务权益 | `ticket.dynamic_pricing.publish` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.published.success` | success | `publishTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules/{id}/publish` | TIC-票务权益 | `ticket.dynamic_pricing.publish` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.simulated.failure` | failure | `simulateTicketDynamicPricing` | `POST` | `/api/v1/ticketing/dynamic-pricing/simulations` | TIC-票务权益 | `ticket.dynamic_pricing.simulate` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.simulated.success` | success | `simulateTicketDynamicPricing` | `POST` | `/api/v1/ticketing/dynamic-pricing/simulations` | TIC-票务权益 | `ticket.dynamic_pricing.simulate` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.viewed.failure` | failure | `listTicketDynamicPricingRules` | `GET` | `/api/v1/ticketing/dynamic-pricing-rules` | TIC-票务权益 | `ticket.dynamic_pricing.view` | 接口级事件，以本附录为准 |
| `ticket_dynamic_pricing.viewed.success` | success | `listTicketDynamicPricingRules` | `GET` | `/api/v1/ticketing/dynamic-pricing-rules` | TIC-票务权益 | `ticket.dynamic_pricing.view` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.archive_failed` | failure | `archiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/archive` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.archived` | success | `archiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/archive` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.changed` | success | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.changed_failed` | failure | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.delete_approve_failed` | failure | `approveDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-approve` | TIC-票务权益 | `ticket.entitlement.approve` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.delete_approved` | success | `approveDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-approve` | TIC-票务权益 | `ticket.entitlement.approve` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.delete_request_failed` | failure | `requestDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-request` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.delete_requested` | success | `requestDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-request` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.unarchive_failed` | failure | `unarchiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/unarchive` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.unarchived` | success | `unarchiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/unarchive` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.viewed` | success | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.view` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.viewed_failed` | failure | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.view` | 接口级事件，以本附录为准 |
| `ticket_face_photo.view_failed` | failure | `getIssuedEntitlementFacePhoto` | `GET` | `/api/v1/ticketing/issued-entitlements/{entitlement_id}/face-photo` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `ticket_face_photo.viewed` | success | `getIssuedEntitlementFacePhoto` | `GET` | `/api/v1/ticketing/issued-entitlements/{entitlement_id}/face-photo` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `ticket_instance.viewed` | success | `listTicketInstances` | `GET` | `/api/v1/ticketing/ticket-instances` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_instance.viewed_failed` | failure | `listTicketInstances` | `GET` | `/api/v1/ticketing/ticket-instances` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_issued_entitlement.viewed` | success | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_issued_entitlement.viewed_failed` | failure | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_order.cancel_failed` | failure | `cancelTicketOrder` | `POST` | `/api/v1/ticketing/orders/{order_id}/cancel` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.cancelled` | success | `cancelTicketOrder` | `POST` | `/api/v1/ticketing/orders/{order_id}/cancel` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.create_failed` | failure | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.created` | success | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.payment_confirm_failed` | failure | `confirmTicketOrderPayment` | `POST` | `/api/v1/ticketing/orders/{order_id}/confirm-payment` | TIC-票务权益 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `ticket_order.payment_confirmed` | success | `confirmTicketOrderPayment` | `POST` | `/api/v1/ticketing/orders/{order_id}/confirm-payment` | TIC-票务权益 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `ticket_order.viewed` | success | `listTicketOrders` | `GET` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.read` | 接口级事件，以本附录为准 |
| `ticket_order.viewed_failed` | failure | `listTicketOrders` | `GET` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.read` | 接口级事件，以本附录为准 |
| `ticket_price_variant.change_failed` | failure | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.changed` | success | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.view_failed` | failure | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.view` | 接口级事件，以本附录为准 |
| `ticket_price_variant.viewed` | success | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.view` | 接口级事件，以本附录为准 |
| `ticket_price.publish_failed` | failure | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_price.published` | success | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | TIC-票务权益 | `ticket.product.manage` | 已在事件矩阵正文出现 |
| `ticket_product_entitlement.bound` | success | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product_entitlement.bound_failed` | failure | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.change_failed` | failure | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.changed` | success | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.pricing_change_failed` | failure | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.pricing_changed` | success | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.view_failed` | failure | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.view_failed` | failure | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.view_failed` | failure | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_refund.approve_failed` | failure | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.approved` | success | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.payout_confirm_failed` | failure | `confirmTicketRefundPayout` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/confirm-payout` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.payout_confirmed` | success | `confirmTicketRefundPayout` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/confirm-payout` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.reject_failed` | failure | `rejectTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/reject` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.rejected` | success | `rejectTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/reject` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.request_failed` | failure | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket_refund.requested` | success | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket_refund.view_failed` | failure | `listTicketRefunds` | `GET` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket_refund.viewed` | success | `listTicketRefunds` | `GET` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket_transfer.claim_failed` | failure | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.claimed` | success | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.create_failed` | failure | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.created` | success | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.revoke_failed` | failure | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.revoked` | success | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.view_failed` | failure | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | TIC-票务权益 | `ticket.transfer.read` | 接口级事件，以本附录为准 |
| `ticket_transfer.viewed` | success | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | TIC-票务权益 | `ticket.transfer.read` | 接口级事件，以本附录为准 |
| `ticket.issue_failed` | failure | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | TIC-票务权益 | `ticket.ticket.issue` | 接口级事件，以本附录为准 |
| `ticket.issued` | success | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | TIC-票务权益 | `ticket.ticket.issue` | 接口级事件，以本附录为准 |
| `ticket.points.billing.bills.viewed` | success | `listTicketPointsBills` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.bills.viewed.failed` | failure | `listTicketPointsBills` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.calendar.submitted` | success | `submitTicketPointsCalendar` | `POST` | `/api/v1/loyalty/ticket-points-billing/calendars` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.calendar.submitted.failed` | failure | `submitTicketPointsCalendar` | `POST` | `/api/v1/loyalty/ticket-points-billing/calendars` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.calendars.viewed` | success | `listTicketPointsCalendars` | `GET` | `/api/v1/loyalty/ticket-points-billing/calendars` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.calendars.viewed.failed` | failure | `listTicketPointsCalendars` | `GET` | `/api/v1/loyalty/ticket-points-billing/calendars` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.items.viewed` | success | `listTicketPointsBillItems` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/items` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.items.viewed.failed` | failure | `listTicketPointsBillItems` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/items` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objection.resolution.submitted` | success | `confirmTicketPointsBillUnchanged` | `POST` | `/api/v1/loyalty/ticket-points-billing/objections/{id}/confirm-unchanged` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objection.resolution.submitted.failed` | failure | `confirmTicketPointsBillUnchanged` | `POST` | `/api/v1/loyalty/ticket-points-billing/objections/{id}/confirm-unchanged` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objection.submitted` | success | `submitTicketPointsBillObjection` | `POST` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objection.submitted.failed` | failure | `submitTicketPointsBillObjection` | `POST` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objections.viewed` | success | `listTicketPointsBillObjections` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.objections.viewed.failed` | failure | `listTicketPointsBillObjections` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.outage.submitted` | success | `submitTicketPointsOutage` | `POST` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.outage.submitted.failed` | failure | `submitTicketPointsOutage` | `POST` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.outages.viewed` | success | `listTicketPointsOutages` | `GET` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.outages.viewed.failed` | failure | `listTicketPointsOutages` | `GET` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.rule.submitted` | success | `submitTicketPointsBillingRule` | `POST` | `/api/v1/loyalty/ticket-points-billing/rules` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.rule.submitted.failed` | failure | `submitTicketPointsBillingRule` | `POST` | `/api/v1/loyalty/ticket-points-billing/rules` | P0-待归类 | `ticket.points.billing.manage` | 接口级事件，以本附录为准 |
| `ticket.points.billing.rules.viewed` | success | `listTicketPointsBillingRules` | `GET` | `/api/v1/loyalty/ticket-points-billing/rules` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.rules.viewed.failed` | failure | `listTicketPointsBillingRules` | `GET` | `/api/v1/loyalty/ticket-points-billing/rules` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.schedule.adjustments.viewed` | success | `listTicketPointsBillScheduleAdjustments` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/schedule-adjustments` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.points.billing.schedule.adjustments.viewed.failed` | failure | `listTicketPointsBillScheduleAdjustments` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/schedule-adjustments` | P0-待归类 | `ticket.points.billing.view` | 接口级事件，以本附录为准 |
| `ticket.qr_refresh_config.update_failed` | failure | `updateTicketQrRefreshConfig` | `PUT` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket.qr_refresh_config.updated` | success | `updateTicketQrRefreshConfig` | `PUT` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket.qr_refresh_config.view_failed` | failure | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket.qr_refresh_config.viewed` | success | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket.refund_line.list` | success | `listTicketRefundLines` | `GET` | `/api/v1/ticketing/refunds/{refund_id}/lines` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket.refund_line.list_failed` | failure | `listTicketRefundLines` | `GET` | `/api/v1/ticketing/refunds/{refund_id}/lines` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket.reward.config.listed` | success | `listTicketRewardConfigs` | `GET` | `/api/v1/loyalty/ticket-reward-configs` | P0-待归类 | `ticket.reward.config.view` | 接口级事件，以本附录为准 |
| `ticket.reward.config.listed.failed` | failure | `listTicketRewardConfigs` | `GET` | `/api/v1/loyalty/ticket-reward-configs` | P0-待归类 | `ticket.reward.config.view` | 接口级事件，以本附录为准 |
| `ticket.reward.config.publish.completed` | success | `publishTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs/{id}/publish` | P0-待归类 | `ticket.reward.config.manage` | 接口级事件，以本附录为准 |
| `ticket.reward.config.publish.completed.failed` | failure | `publishTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs/{id}/publish` | P0-待归类 | `ticket.reward.config.manage` | 接口级事件，以本附录为准 |
| `ticket.reward.config.submit.completed` | success | `submitTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs` | P0-待归类 | `ticket.reward.config.manage` | 接口级事件，以本附录为准 |
| `ticket.reward.config.submit.completed.failed` | failure | `submitTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs` | P0-待归类 | `ticket.reward.config.manage` | 接口级事件，以本附录为准 |
| `ticket.reward.customer.viewed` | success | `getOwnTicketUseRewards` | `GET` | `/api/v1/customer/ticket-rewards` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket.reward.customer.viewed.failed` | failure | `getOwnTicketUseRewards` | `GET` | `/api/v1/customer/ticket-rewards` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket.reward.rules.viewed` | success | `listPublishedTicketRewardRules` | `GET` | `/api/v1/customer/ticket-reward-rules` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket.reward.rules.viewed.failed` | failure | `listPublishedTicketRewardRules` | `GET` | `/api/v1/customer/ticket-reward-rules` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket.verification_reverse_failed` | failure | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `ticket.verification_reversed` | success | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `venue_business_hours.update_failed` | failure | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `venue_business_hours.updated` | success | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `venue_business_hours.view_failed` | failure | `listVenueBusinessHours` | `GET` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `venue_business_hours.viewed` | success | `listVenueBusinessHours` | `GET` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `verification.failed` | failure | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.failed` | failure | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.previewed` | success | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `verification.succeeded` | success | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.view_failed` | failure | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `verification.viewed` | success | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `voucher_transfer.claim_failed` | failure | `claimCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/claim` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher_transfer.claimed` | success | `claimCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/claim` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher_transfer.create_failed` | failure | `initiateCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher_transfer.created` | success | `initiateCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher_transfer.revoke_failed` | failure | `revokeCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/revoke` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher_transfer.revoked` | success | `revokeCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/revoke` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher.customer_wallet_view_failed` | failure | `customerVoucherWallet` | `GET` | `/api/v1/customer/vouchers` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.customer_wallet_viewed` | success | `customerVoucherWallet` | `GET` | `/api/v1/customer/vouchers` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.definition_create_failed` | failure | `createVoucherDefinition` | `POST` | `/api/v1/vouchers/definitions` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `voucher.definition_created` | success | `createVoucherDefinition` | `POST` | `/api/v1/vouchers/definitions` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `voucher.definitions_view_failed` | failure | `listVoucherDefinitions` | `GET` | `/api/v1/vouchers/definitions` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.definitions_viewed` | success | `listVoucherDefinitions` | `GET` | `/api/v1/vouchers/definitions` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.instances_view_failed` | failure | `listVoucherInstances` | `GET` | `/api/v1/vouchers/instances` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.instances_viewed` | success | `listVoucherInstances` | `GET` | `/api/v1/vouchers/instances` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.issue_failed` | failure | `issueVouchers` | `POST` | `/api/v1/vouchers/instances` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `voucher.issued` | success | `issueVouchers` | `POST` | `/api/v1/vouchers/instances` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `voucher.overview_view_failed` | failure | `listVoucherOverview` | `GET` | `/api/v1/vouchers/overview` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.overview_viewed` | success | `listVoucherOverview` | `GET` | `/api/v1/vouchers/overview` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.redeem_failed` | failure | `redeemVoucher` | `POST` | `/api/v1/vouchers/redemptions` | CUS-客人端 | `customer.voucher.redeem` | 接口级事件，以本附录为准 |
| `voucher.redeemed` | success | `redeemVoucher` | `POST` | `/api/v1/vouchers/redemptions` | CUS-客人端 | `customer.voucher.redeem` | 接口级事件，以本附录为准 |
| `voucher.redemptions_view_failed` | failure | `listVoucherRedemptions` | `GET` | `/api/v1/vouchers/redemptions` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.redemptions_viewed` | success | `listVoucherRedemptions` | `GET` | `/api/v1/vouchers/redemptions` | CUS-客人端 | `customer.voucher.view` | 接口级事件，以本附录为准 |
| `voucher.revoke_failed` | failure | `revokeVoucher` | `POST` | `/api/v1/vouchers/instances/{id}/revoke` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `voucher.revoked` | success | `revokeVoucher` | `POST` | `/api/v1/vouchers/instances/{id}/revoke` | CUS-客人端 | `customer.voucher.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_challenge_failed` | failure | `issueWecomCleanupChallenge` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/challenge` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_challenged` | success | `issueWecomCleanupChallenge` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/challenge` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_execute_failed` | failure | `executeWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/execute` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_executed` | success | `executeWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/execute` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_list_failed` | failure | `listWecomCleanupPreviews` | `GET` | `/api/v1/compliance/wecom/cleanup-previews` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_listed` | success | `listWecomCleanupPreviews` | `GET` | `/api/v1/compliance/wecom/cleanup-previews` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_preview_failed` | failure | `previewWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_previewed` | success | `previewWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_view_failed` | failure | `getWecomCleanupPreview` | `GET` | `/api/v1/compliance/wecom/cleanup-previews/{id}` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.cleanup_viewed` | success | `getWecomCleanupPreview` | `GET` | `/api/v1/compliance/wecom/cleanup-previews/{id}` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.conversation_sync_failed` | failure | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | P0-待归类 | `wecom.sync` | 接口级事件，以本附录为准 |
| `wecom.conversation_synced` | success | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | P0-待归类 | `wecom.sync` | 接口级事件，以本附录为准 |
| `wecom.raw_view_failed` | failure | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | P0-待归类 | `wecom.view_raw` | 接口级事件，以本附录为准 |
| `wecom.raw_viewed` | success | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | P0-待归类 | `wecom.view_raw` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_failed` | failure | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_view_failed` | failure | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_viewed` | success | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.risk_hit.list` | success | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | P0-待归类 | `wecom.risk_hit.view` | 接口级事件，以本附录为准 |
| `wecom.risk_hit.list_failed` | failure | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | P0-待归类 | `wecom.risk_hit.view` | 接口级事件，以本附录为准 |
| `wecom.risk_hit.review` | success | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | P0-待归类 | `wecom.risk_hit.review` | 接口级事件，以本附录为准 |
| `wecom.risk_hit.review_failed` | failure | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | P0-待归类 | `wecom.risk_hit.review` | 接口级事件，以本附录为准 |

## 5. operationId 到任务、权限、事件、终端的追溯表

> 本节用于开发排期、测试用例、供应商联调和审计抽样。任务簇是执行归类，不替代 `p0-implementation-task-list.md` 中的正式任务编号；若某 operation 准备进入编码，必须在具体任务单中引用本表 operationId。

| 建议任务簇 | operationId | 方法 | 路径 | 摘要 | 权限 | 成功/失败事件 | 终端/调用通道 | 覆盖状态 |
|---|---|---|---|---|---|---|---|---|
| AMU-游乐安全 | `acknowledgeAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices/{noticeId}/acknowledgements` | 登记安全告知签字确认（免责确认） | `amusement.waiver.acknowledge` | `amusement.waiver.acknowledge` / `amusement.waiver.acknowledge_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | 添加事件处置动作/流转状态 | `amusement.incident.process` | `amusement.incident.action` / `amusement.incident.action_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | Approve P2 amusement group booking and lock capacity | `amusement.group_booking.approve` | `amusement_group_booking.approved.success` / `amusement_group_booking.approved.failure` | web | operation 级补充 |
| AMU-游乐安全 | `approveAmusementReopen` | `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | 严重项恢复开放审批 | `amusement.safety_inspection.approve_reopen` | `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected` | web | operation 级补充 |
| AMU-游乐安全 | `approveDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-approve` | 审批游乐项目权益绑定删除申请 | `amusement.project_entitlement.approve` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `archiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/archive` | 存档（停用）游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `assignAmusementProjectSpace` | `PUT` | `/api/v1/amusement/projects/{id}/space` | 游乐项目空间指派（回填） | `amusement.session.manage` | `amusement.project_space.assigned` / `amusement.project_space.assign_failed` | web | operation 级补充 |
| AMU-游乐安全 | `changeAmusementQueueEntryStatus` | `POST` | `/api/v1/amusement/queue-entries/{id}/status` | 变更游乐排队记录状态 | `amusement.queue.manage` | `amusement.queue_entry.status_change` / `amusement.queue_entry.status_change_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `changeAmusementReservationStatus` | `POST` | `/api/v1/amusement/reservations/{id}/status` | 变更游乐预约状态 | `amusement.reservation.manage` | `amusement.reservation.status_change` / `amusement.reservation.status_change_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `claimAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions/{id}/claim` | 一体机领取核销权益会话（防错领 + 会话时效） | `amusement.entitlement_session.claim` | `amusement.entitlement_session.claimed` / `amusement.entitlement_session.claim_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | 关闭游乐安全事件 | `amusement.incident.close` | `amusement.incident.close` / `amusement.incident.close_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `configureAmusementCompensationPolicy` | `POST` | `/api/v1/amusement/compensation-policies/{project_id}` | 配置全园故障补偿有效期参数（含上下限） | `amusement.compensation_policy.manage` | `amusement.fault_compensation.policy_configured` / `amusement.fault_compensation.policy_config_failed` | web | operation 级补充 |
| AMU-游乐安全 | `configureAmusementOverCapacityPolicy` | `PUT` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | 配置游乐项目超容处置策略 | `amusement.over_capacity_policy.configure` | `amusement.over_capacity_policy.configured` / `amusement.over_capacity_policy.configure_failed` | web | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/compensation-batches/{batch_id}/confirm` | 第二岗位确认并发放补偿 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.fault_compensation.batch_issued` / `amusement.fault_compensation.batch_issue_failed` | web | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | 主管确认恢复营业 | `amusement.maintenance.confirm` | `amusement.project.resumed` / `amusement.maintenance_resume_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementParkWideFaultActivation` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-activation` | 第二岗位确认全园故障状态生效 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.park_fault.activated` / `amusement.park_fault.activate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-lift` | 第二岗位确认解除全园故障状态 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.park_fault.lifted` / `amusement.park_fault.lift_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementCapacityConfig` | `POST` | `/api/v1/amusement/projects/{id}/capacity-config` | 维护游乐点位设备并发容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.changed` / `amusement.capacity_config.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementDevice` | `POST` | `/api/v1/amusement/projects/{id}/devices` | 维护游乐项目游乐设备 | `amusement.device.manage` | `amusement.device.changed` / `amusement.device.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | Create P2 amusement group or private booking request | `amusement.group_booking.manage` | `amusement_group_booking.created.success` / `amusement_group_booking.created.failure` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | 上报游乐安全事件 | `amusement.incident.create` | `amusement.incident.create` / `amusement.incident.create_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | 提交游乐定期检修 | `amusement.maintenance.submit` | `amusement.maintenance_recorded` / `amusement.maintenance_record_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | 维护游乐项目 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements` | 维护游乐项目可用权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | 创建游乐项目规则 | `amusement.project.manage` | `amusement.project_rule.changed` / `amusement.project_rule.changed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementQueueEntry` | `POST` | `/api/v1/amusement/queue-entries` | 创建游乐排队记录 | `amusement.queue.manage` | `amusement.queue_entry.create` / `amusement.queue_entry.create_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementReservation` | `POST` | `/api/v1/amusement/reservations` | 创建游乐预约 | `amusement.reservation.manage` | `amusement.reservation.create` / `amusement.reservation.create_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections` | 创建开放前安全巡检 | `amusement.safety_inspection.create` | `amusement.safety_inspection.created` / `amusement.safety_inspection.create_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices` | 创建游乐安全告知/免责须知 | `amusement.safety_notice.manage` | `amusement.safety_notice.create` / `amusement.safety_notice.create_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | 维护巡检模板 | `amusement.safety_template.manage` | `amusement.safety_template.changed` / `amusement.safety_template.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | 维护游乐场次和容量 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | 游乐权益核销 | `amusement.verification.execute` | `amusement.verification_succeeded` / `amusement.verification_failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| AMU-游乐安全 | `disableAmusementCapacityConfig` | `POST` | `/api/v1/amusement/capacity-config/{configId}/disable` | 停用游乐容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.changed` / `amusement.capacity_config.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | 查询全园故障补偿有效期参数 | `amusement.park_fault.view` | `amusement.fault_compensation.viewed` / `amusement.fault_compensation.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `getAmusementEntitlementSession` | `GET` | `/api/v1/amusement/entitlement-sessions/{id}` | 一体机查询展示权益会话（按 id 或 session_code） | `amusement.entitlement_session.view` | `amusement.entitlement_session.viewed` / `amusement.entitlement_session.view_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | 查询游乐定期检修记录详情 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | 查询项目或场次安全与容量准入 | `amusement.safety_inspection.view` | `amusement.open_eligibility.viewed` / `amusement.open_eligibility.blocked` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `getAmusementOverCapacityPolicy` | `GET` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | 查询游乐项目超容处置策略 | `amusement.over_capacity_policy.view` | `amusement.over_capacity_policy.viewed` / `amusement.over_capacity_policy.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions` | 发起权益会话（工作手机扫附赠权益码） | `amusement.entitlement_session.initiate` | `amusement.entitlement_session.initiated` / `amusement.entitlement_session.initiate_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | 发起全园故障补偿批次（第一岗位：快照受影响人份） | `amusement.park_fault.confirm_as_head` | `amusement.fault_compensation.batch_initiated` / `amusement.fault_compensation.batch_initiate_failed` | web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementParkWideFault` | `POST` | `/api/v1/amusement/park-wide-faults` | 发起全园故障状态（第一岗位） | `amusement.park_fault.confirm_as_head` | `amusement.park_fault.initiated` / `amusement.park_fault.initiate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/initiate-lift` | 发起解除全园故障状态（第一岗位） | `amusement.park_fault.confirm_as_head` | `amusement.park_fault.lift_initiated` / `amusement.park_fault.lift_initiate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementCapacityConfigs` | `GET` | `/api/v1/amusement/projects/{id}/capacity-config` | 查询游乐点位设备并发容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.viewed` / `amusement.capacity_config.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementDevices` | `GET` | `/api/v1/amusement/projects/{id}/devices` | 查询游乐项目游乐设备 | `amusement.device.view` | `amusement.device.viewed` / `amusement.device.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | 查询游乐设备状态 | `amusement.device_status.view` | `amusement.device_status.viewed` / `amusement.device_status.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementEntitlementSessions` | `GET` | `/api/v1/amusement/entitlement-sessions` | 查询权益会话列表（merchant scope） | `amusement.entitlement_session.view` | `amusement.entitlement_session.listed` / `amusement.entitlement_session.list_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | 查询某次全园故障的补偿批次 | `amusement.park_fault.view` | `amusement.fault_compensation.viewed` / `amusement.fault_compensation.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | 查询补偿批次的补偿记录 | `amusement.park_fault.view` | `amusement.fault_compensation.viewed` / `amusement.fault_compensation.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | List P2 amusement group and private bookings | `amusement.group_booking.view` | `amusement_group_booking.viewed.success` / `amusement_group_booking.viewed.failure` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | 查询游乐安全事件列表 | `amusement.incident.view` | `amusement.incident.list` / `amusement.incident.list_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementInspectionTasks` | `GET` | `/api/v1/amusement/inspection-tasks` | 查询游乐巡检到期待办 | `amusement.safety_inspection.view` | `amusement.inspection_task.viewed` / `amusement.inspection_task.viewed_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | 查询游乐定期检修记录 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | 查询全园故障状态留痕 | `amusement.park_fault.view` | `amusement.park_fault.viewed` / `amusement.park_fault.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementParkWideFaults` | `GET` | `/api/v1/amusement/park-wide-faults` | 查询全园故障状态记录 | `amusement.park_fault.view` | `amusement.park_fault.viewed` / `amusement.park_fault.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectEntitlements` | `GET` | `/api/v1/amusement/projects/{id}/entitlements` | 查询游乐项目可用权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.viewed` / `amusement.project_entitlement.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | 查询游乐项目 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectsWithoutDeviceRegistry` | `GET` | `/api/v1/amusement/projects/without-device-registry` | 待建档（无在役设备档案）的游乐项目清单 | `amusement.session.manage` | `amusement.device_registry_backlog.viewed` / `amusement.device_registry_backlog.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectsWithoutSpace` | `GET` | `/api/v1/amusement/projects/without-space` | 未回填空间的游乐项目清单 | `amusement.session.manage` | `amusement.project_space_backlog.viewed` / `amusement.project_space_backlog.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | 查询游乐排队记录 | `amusement.queue.view` | `amusement.queue.viewed` / `amusement.queue.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementReservations` | `GET` | `/api/v1/amusement/reservations` | 查询游乐预约 | `amusement.reservation.view` | `amusement.reservation.viewed` / `amusement.reservation.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyInspections` | `GET` | `/api/v1/amusement/safety-inspections` | 查询安全巡检 | `amusement.safety_inspection.view` | `amusement.safety_inspection.viewed` / `amusement.safety_inspection.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyNotices` | `GET` | `/api/v1/amusement/safety-notices` | 查询游乐安全告知列表 | `amusement.safety_notice.view` | `amusement.safety_notice.list` / `amusement.safety_notice.list_failed` | tablet, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | 查询巡检模板 | `amusement.safety_template.view` | `amusement.safety_template.viewed` / `amusement.safety_template.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | 查询游乐场次和容量 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | 查询游乐核销记录 | `amusement.verification.view` | `amusement.verification.viewed` / `amusement.verification.view_failed` | web, tablet | operation 级补充 |
| AMU-游乐安全 | `recheckAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | 提交复检结果 | `amusement.safety_inspection.recheck` | `amusement.safety_inspection.rechecked` / `amusement.safety_inspection.recheck_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `rectifyAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | 提交整改说明和证据 | `amusement.safety_inspection.rectify` | `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rectification_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | Release P2 amusement group booking and free locked capacity | `amusement.group_booking.manage` | `amusement_group_booking.released.success` / `amusement_group_booking.released.failure` | web | operation 级补充 |
| AMU-游乐安全 | `removeAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/remove` | 解除游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `requestDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-request` | 发起游乐项目权益绑定删除申请 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `submitAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | 提交巡检结果 | `amusement.safety_inspection.submit` | `amusement.safety_inspection.submitted` / `amusement.safety_inspection.blocked` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `unarchiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/unarchive` | 恢复游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| ANA-风控分析 | `createReviewLock` | `POST` | `/api/v1/analytics/review-locks` | 锁定经营复盘快照 | `analytics.review.lock` | `analytics.review.locked` / `analytics.review.lock_failed` | web | operation 级补充 |
| ANA-风控分析 | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | 创建风险alert | `analytics.risk_alert.manage` | `risk_alert.created` / `risk_alert.create_failed` | web | operation 级补充 |
| ANA-风控分析 | `disposeMetricAnomaly` | `POST` | `/api/v1/analytics/metric-anomalies/{id}/dispose` | 处置指标异常 | `analytics.metric_anomaly.manage` | `analytics.metric_anomaly.disposed` / `analytics.metric_anomaly.disposed_failed` | web | operation 级补充 |
| ANA-风控分析 | `exportMonthlyReport` | `GET` | `/api/v1/analytics/reports/monthly-export` | 月度经营报表导出(分权) | `analytics.report.export` | `analytics.monthly_report.exported` / `analytics.monthly_report.export_failed` | web | operation 级补充 |
| ANA-风控分析 | `fetchHolidayCalendar` | `POST` | `/api/v1/analytics/holiday-calendar/fetch` | 从 URL 抓取节假日日历 | `analytics.operating_params.manage` | `analytics.holiday_calendar.fetched` / `analytics.holiday_calendar.fetch_failed` | web | operation 级补充 |
| ANA-风控分析 | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | Generate local LLM AI quality suggestion | `analytics.risk_alert.manage` | `analytics.ai_quality.local_llm_suggestion.created` / `analytics.ai_quality.local_llm_suggestion.failed` | web | operation 级补充 |
| ANA-风控分析 | `getAbandonedEntitlementRevenue` | `GET` | `/api/v1/analytics/abandoned-entitlement-revenue` | 放弃权益额外收入报表(票种×权益类型) | `analytics.dashboard.view` | `analytics.abandoned_entitlement_revenue.viewed` / `analytics.abandoned_entitlement_revenue.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | AI/对标阈值配置 | `analytics.dashboard.view` | `analytics.ai_thresholds.viewed` / `analytics.ai_thresholds.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | P0 经营看板下钻（按域） | `analytics.dashboard.view` | `analytics.dashboard_domain.viewed` / `analytics.dashboard_domain.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnalyticsTrafficHourly` | `GET` | `/api/v1/analytics/traffic-hourly` | 分时段客流分布 | `analytics.dashboard.view` | `analytics.traffic_hourly.viewed` / `analytics.traffic_hourly.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | 读指标异常检测阈值 | `analytics.dashboard.view` | `analytics.anomaly_config.viewed` / `analytics.anomaly_config.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getGroupOverview` | `GET` | `/api/v1/analytics/group-overview` | 集团总览视图 | `analytics.group.view` | `analytics.group_overview.viewed` / `analytics.group_overview.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getHolidayComparison` | `GET` | `/api/v1/analytics/holiday-comparison` | 节假日对比(今年 vs 去年同名) | `analytics.dashboard.view` | `analytics.holiday_comparison.viewed` / `analytics.holiday_comparison.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getMetricDetail` | `GET` | `/api/v1/analytics/metric-detail` | KPI统一指标明细(指标登记簿) | `analytics.dashboard.view` | `analytics.metric_detail.viewed` / `analytics.metric_detail.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getMonthlyComparison` | `GET` | `/api/v1/analytics/monthly-comparison` | 月度对比(环比/同比) | `analytics.dashboard.view` | `analytics.monthly_comparison.viewed` / `analytics.monthly_comparison.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | P0 经营总览 | `analytics.dashboard.view` | `analytics.dashboard_viewed` / `analytics.dashboard_view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getRentToSales` | `GET` | `/api/v1/analytics/rent-to-sales` | 店级/业态级/平台级租售比 | `contract.view` | `analytics.rent_to_sales.viewed` / `analytics.rent_to_sales.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | 获取机器人经营分析 | `analytics.dashboard.view` | `analytics.robot.viewed` / `analytics.robot.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAbnormalReadings` | `GET` | `/api/v1/analytics/energy/abnormal-readings` | 异常读数明细(能耗钻取) | `energy.reading.manage` | `analytics.abnormal_readings.viewed` / `analytics.abnormal_readings.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | 查询ai质检结果 | `analytics.risk_alert.view` | `analytics.ai_quality_result.viewed` / `analytics.ai_quality_result.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | 查询ai质检任务 | `analytics.risk_alert.view` | `analytics.ai_quality_task.viewed` / `analytics.ai_quality_task.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | 查询经营分析商户 | `analytics.dashboard.view` | `analytics.merchant_summary.viewed` / `analytics.merchant_summary.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listArOverdueDebtors` | `GET` | `/api/v1/analytics/finance-receivables/debtors` | 欠费商户明细排行(账龄钻取) | `finance.ar_ap.view` | `analytics.ar_debtors.viewed` / `analytics.ar_debtors.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listBusinessDayCutoffs` | `GET` | `/api/v1/analytics/business-day-cutoffs` | 查询经营日切分配置(按业务域) | `analytics.dashboard.view` | `analytics.business_day_cutoff.viewed` / `analytics.business_day_cutoff.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | List P2 business metric snapshots for operational analytics | `analytics.dashboard.view` | `analytics_business_metric_snapshot.viewed.success` / `analytics_business_metric_snapshot.viewed.failure` | web | operation 级补充 |
| ANA-风控分析 | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | 查询客户意向 | `analytics.risk_alert.view` | `analytics.customer_intent.viewed` / `analytics.customer_intent.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listExpiringContracts` | `GET` | `/api/v1/analytics/occupancy/expiring-contracts` | 到期预警合同明细 | `contract.view` | `analytics.expiring_contracts.viewed` / `analytics.expiring_contracts.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | 节假日日历 | `analytics.dashboard.view` | `analytics.holiday_calendar.viewed` / `analytics.holiday_calendar.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | 查询商户健康度scores | `analytics.dashboard.view` | `analytics.merchant_health.viewed` / `analytics.merchant_health.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | 查询指标异常 | `analytics.risk_alert.view` | `analytics.metric_anomaly.viewed` / `analytics.metric_anomaly.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricAnomalyTimeline` | `GET` | `/api/v1/analytics/metric-anomalies/{id}/timeline` | 查询指标异常处置历史时间线 | `analytics.risk_alert.view` | `analytics.metric_anomaly.timeline_viewed` / `analytics.metric_anomaly.timeline_viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | 查询指标定义 | `analytics.dashboard.view` | `analytics.metric_definition.viewed` / `analytics.metric_definition.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | 提醒窗天数配置 | `analytics.dashboard.view` | `analytics.notification_windows.viewed` / `analytics.notification_windows.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | 查询综合体经营参数（坪效面积） | `analytics.dashboard.view` | `analytics.operating_params.viewed` / `analytics.operating_params.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | 超时工单明细(时效钻取) | `facility.work_order.manage` | `analytics.overdue_work_orders.viewed` / `analytics.overdue_work_orders.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | 商户收入归因口径规则清单 | `analytics.dashboard.view` | `analytics.revenue_attribution_rule.viewed` / `analytics.revenue_attribution_rule.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listReviewLocks` | `GET` | `/api/v1/analytics/review-locks` | 经营复盘锁定记录 | `analytics.review.lock` | `analytics.review_lock.viewed` / `analytics.review_lock.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | 风险预警 | `analytics.risk_alert.view` | `risk_alert.viewed` / `risk_alert.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | 查询风险规则 | `analytics.risk_alert.view` | `analytics.risk_rule.viewed` / `analytics.risk_rule.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | 查询员工服务scores | `analytics.risk_alert.view` | `analytics.staff_service_score.viewed` / `analytics.staff_service_score.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listVacantSpaces` | `GET` | `/api/v1/analytics/occupancy/vacant-spaces` | 空置铺位明细(出租率钻取) | `contract.view` | `analytics.vacant_spaces.viewed` / `analytics.vacant_spaces.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | 处理风险alert | `analytics.risk_alert.manage` | `analytics.risk_alert.resolved` / `analytics.risk_alert.resolved_failed` | web | operation 级补充 |
| ANA-风控分析 | `updateAiThresholds` | `POST` | `/api/v1/analytics/ai-thresholds` | 维护 AI/对标阈值 | `analytics.operating_params.manage` | `analytics.ai_thresholds.updated` / `analytics.ai_thresholds.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | 改指标异常检测阈值 | `analytics.operating_params.manage` | `analytics.anomaly_config.updated` / `analytics.anomaly_config.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertBusinessDayCutoff` | `POST` | `/api/v1/analytics/business-day-cutoffs` | 修改某业务域的经营日切分小时 | `analytics.operating_params.manage` | `analytics.business_day_cutoff.updated` / `analytics.business_day_cutoff.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertHolidayPeriod` | `POST` | `/api/v1/analytics/holiday-calendar` | 手动录入节假日区间 | `analytics.operating_params.manage` | `analytics.holiday_calendar.updated` / `analytics.holiday_calendar.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertNotificationWindow` | `POST` | `/api/v1/analytics/notification-windows` | 维护提醒窗天数 | `platform.notification_window.manage` | `analytics.notification_windows.updated` / `analytics.notification_windows.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | 新增或更新综合体经营参数（坪效面积） | `analytics.operating_params.manage` | `operating_params.changed` / `operating_params.change_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | 维护商户收入归因口径规则 | `analytics.operating_params.manage` | `analytics.revenue_attribution_rule.updated` / `analytics.revenue_attribution_rule.update_failed` | web | operation 级补充 |
| AUTH-登录会话 | `login` | `POST` | `/api/v1/auth/login` | 员工或商户员工登录 | `public` | `auth.login.succeeded` / `auth.login.failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | 已在任务映射正文出现 |
| AUTH-登录会话 | `logout` | `POST` | `/api/v1/auth/logout` | 退出登录 | `authenticated` | `auth.logout` / `auth.logout_failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | 已在任务映射正文出现 |
| AUTH-登录会话 | `refreshToken` | `POST` | `/api/v1/auth/refresh` | 刷新会话 | `authenticated` | `auth.token.refreshed` / `auth.token_refresh_failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | operation 级补充 |
| AUTH-账号权限 | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | 确认/同意员工通知 | `staff.notice.view` | `staff.notice_acknowledged` / `staff.notice_acknowledge_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `applyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/apply` | 应用权限模板 | `access.permission_template.apply` | `permission_template.applied` / `permission_template.apply_failed` | web | operation 级补充 |
| AUTH-账号权限 | `approveStaffDeviceRebind` | `POST` | `/api/v1/staff/device-credentials/{id}/approve-rebind` | 审批换机生效 | `staff.device.review` | `staff_device.rebound` / `staff_device.rebind_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | 分配员工职级与薪资 | `access.staff_level.manage` | `access.staff_level.assigned` / `access.staff_level.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | 分配用户岗位并套用岗位权限 | `access.role.assign` | `access.user_position.assigned` / `access.user_position.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | 为用户分配角色 | `access.role.assign` | `access.role.assigned` / `access.role.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `bindStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials` | 绑定/换机申请员工设备凭证 | `staff.device.bind` | `staff_device.bound` / `staff_device.bind_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `copyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/copy` | 复制权限模板 | `access.permission_template.manage` | `permission_template.copied` / `permission_template.copy_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | 创建需主管复核的授权申请 | `access.grant_request.apply` | `access.grant_request.applied` / `access.grant_request.apply_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createAccessUser` | `POST` | `/api/v1/access/users` | 创建权限用户 | `access.user.manage` | `access.user.changed` / `access.user.changed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createPermissionTemplate` | `POST` | `/api/v1/access/permission-templates` | 创建权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | 创建岗位角色模板 | `access.role.assign` | `access.position_role_template.saved` / `access.position_role_template.save_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createRole` | `POST` | `/api/v1/access/roles` | 创建角色 | `access.role.manage` | `role.changed` / `role.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants` | 临时授权 | `access.temporary_grant.create` | `permission_template.applied` / `temporary_grant.create_failed` | web | operation 级补充 |
| AUTH-账号权限 | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | 删除岗位角色模板 | `access.role.assign` | `access.position_role_template.deleted` / `access.position_role_template.delete_failed` | web | operation 级补充 |
| AUTH-账号权限 | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | 授权申请治理汇总 | `access.grant_request.view` | `access.grant_request.summary_viewed` / `access.grant_request.summary_view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | 查询当前账号有效权限集 | `authenticated` | `access.effective_permission.viewed` / `access.effective_permission.viewed_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | 角色可查看项目现状 | `access.role.manage` | `role.viewable_projects.viewed` / `role.viewable_projects.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | 查询指定账号有效权限集 | `access.user.view` | `access.effective_permission.viewed` / `access.effective_permission.viewed_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | 授权申请列表 | `access.grant_request.view` | `access.grant_request.viewed` / `access.grant_request.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listAccessUsers` | `GET` | `/api/v1/access/users` | 查询权限用户 | `access.user.view` | `access.user.viewed` / `access.user.viewed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listApprovalLines` | `GET` | `/api/v1/access/approval-lines` | 查询部门和平行审批线配置 | `access.role.assign` | `access.approval_line.listed` / `access.approval_line.list_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | 查询员工通知 | `staff.notice.view` | `staff.notice.viewed` / `staff.notice.view_failed` | staff_private_phone, web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `listPermissions` | `GET` | `/api/v1/access/permissions` | 权限点列表 | `access.permission.view` | `permission.viewed` / `permission.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | 权限模板列表 | `access.permission_template.view` | `permission_template.viewed` / `permission_template.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | 岗位角色模板列表 | `access.role.assign` | `access.position_role_template.viewed` / `access.position_role_template.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | 角色可查看项目候选清单 | `access.role.manage` | `access.role_project_options.viewed` / `access.role_project_options.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listRoles` | `GET` | `/api/v1/access/roles` | 角色列表 | `access.role.view` | `role.viewed` / `role.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | 查询职级目录 | `access.staff_level.view` | `access.staff_level.viewed` / `access.staff_level.viewed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | 查询员工档案 | `staff.profile.view` | `staff.profile.viewed` / `staff.profile.view_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `reportStaffDeviceIntegrity` | `POST` | `/api/v1/staff/device-credentials/{id}/report-integrity` | 端侧完整性上报 | `staff.device.bind` | `staff_device.integrity_reported` / `staff_device.integrity_report_failed` | staff_private_phone | operation 级补充 |
| AUTH-账号权限 | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | 复核授权申请 | `access.grant_request.review` | `access.grant_request.reviewed` / `access.grant_request.review_failed` | web | operation 级补充 |
| AUTH-账号权限 | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | 审批员工注册 | `staff.profile.review` | `staff.approved` / `staff.review_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials/{id}/revoke` | 解绑/失效设备凭证 | `staff.device.revoke` | `staff_device.revoked` / `staff_device.revoke_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | 撤销临时授权 | `access.temporary_grant.revoke` | `permission_template.revoked` / `temporary_grant.revoke_failed` | web | operation 级补充 |
| AUTH-账号权限 | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | 设置员工销售岗标识 | `staff.profile.review` | `staff.profile.sales_position_changed` / `staff.profile.sales_position_change_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | 商户代员工提交注册 | `staff.profile.submit` | `staff.registered` / `staff.register_failed` | web, merchant_app, merchant_portal, staff_work_phone | operation 级补充 |
| AUTH-账号权限 | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | 取消用户角色 | `access.role.assign` | `access.role.unassigned` / `access.role.unassign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updatePermissionTemplate` | `PUT` | `/api/v1/access/permission-templates/{id}` | 修改权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | 更新岗位角色模板 | `access.role.assign` | `access.position_role_template.saved` / `access.position_role_template.save_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | 增减角色可查看项目 | `access.role.manage` | `role.viewable_projects.changed` / `role.viewable_projects.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `upsertApprovalLine` | `PUT` | `/api/v1/access/approval-lines/{line_code}` | 维护部门和平行审批线配置 | `access.role.assign` | `access.approval_line.saved` / `access.approval_line.save_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `addBarBillTip` | `POST` | `/api/v1/bar/bills/{bill_id}/tip` | 账单加小费 | `bar.bill.tip` | `bar.bill_tip_added` / `bar.bill_tip_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | 新增酒吧订单物品 | `bar.order.create` | `bar.order_item.created` / `bar.order_item.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `adjustBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/adjustments` | 对酒水账单应用调整 | `bar.bill.adjust` | `bar.bill.adjust` / `bar.bill.adjust_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | 下线退酒原因（软删留理由） | `bar.order_item.void_decide` | `bar.void_reason_code.archived` / `bar.void_reason_code.archive_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | 给酒吧商品设分类 | `bar.product.manage` | `bar.product.category_assigned` / `bar.product.category_assigned_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | 卡座/桌台空间指派（回填） | `bar.table.manage` | `bar.table_space.assigned` / `bar.table_space.assign_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | 卡座销售提成计算 | `bar.table_commission.calculate` | `bar.table_commission.calculated` / `bar.table_commission.calculate_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | 订台取消/爽约（按时限退定金或没收） | `bar.reservation.manage` | `bar.reservation.cancelled` / `bar.reservation.cancel_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | 订台到店核销（定金转抵扣进账单） | `bar.reservation.manage` | `bar.reservation_deposit_deducted` / `bar.reservation_deposit_deduct_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | 卡座免通票入场名额核销 | `bar.table.guest_checkin` | `bar.table_guest.checked_in` / `bar.table_guest.rejected` | staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | 班结对账 | `bar.shift.close` | `bar.shift_closed` / `bar.shift_close_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `createBarBillPayIntent` | `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | 酒吧账单支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | 创建订台定金规则（固定额/按低消比例） | `bar.deposit_rule.manage` | `bar.deposit_rule.created` / `bar.deposit_rule.create_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `createBarOrder` | `POST` | `/api/v1/bar/orders` | 点单 | `bar.order.create` | `bar.order_created` / `bar.order_create_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | 创建酒吧订单账单 | `bar.order.create` | `bar.bill.created` / `bar.bill.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarProduct` | `POST` | `/api/v1/bar/products` | 创建酒吧商品 | `bar.product.manage` | `bar.product.created` / `bar.product.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | 订台或卡座预订 | `bar.reservation.create` | `bar.reservation.created` / `bar.reservation.create_failed` | web, tablet, miniapp, app, kiosk, staff_private_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarTable` | `POST` | `/api/v1/bar/tables` | 创建桌台或卡座 | `bar.table.manage` | `bar.table.changed` / `bar.table.change_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | 创建酒水台位会话（开台） | `bar.table_session.create` | `bar.table_session.create` / `bar.table_session.create_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | 退菜退酒审批决策（通过/驳回） | `bar.order_item.void` | `bar.order_item_void.decide` / `bar.order_item_void.decide_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | 出酒或送达确认 | `bar.order.create` | `bar.order_delivered` / `bar.order_deliver_failed` | tablet, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | 查询订卡邀请函 | `bar.reservation.view` | `bar.reservation_invitation_viewed` / `bar.reservation_invitation_view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | 签发订卡邀请函 | `bar.reservation.invitation.manage` | `bar.reservation_invitation_issued` / `bar.reservation_invitation_issue_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | 查询酒水账单拆并操作记录 | `bar.bill.split_merge` | `bar.bill_operation.list` / `bar.bill_operation.list_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | 查询酒吧套餐 | `bar.combo.view` | `bar.combo.viewed` / `bar.combo.viewed_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarEntitlementRedemptions` | `GET` | `/api/v1/bar/entitlement-redemptions` | 查询酒吧酒水权益兑换台账 | `bar.entitlement.read` | `bar.entitlement_redemption.viewed` / `bar.entitlement_redemption.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | 退酒审批待审队列 | `bar.order_item.void` | `bar.order_item_void.viewed` / `bar.order_item_void.view_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `listBarOrders` | `GET` | `/api/v1/bar/orders` | 查询酒吧订单 | `bar.order.view` | `bar.order.viewed` / `bar.order.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | 查询酒吧商品分类 | `bar.category.view` | `bar.category.viewed` / `bar.category.viewed_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | 查询出品工单事件流 | `bar.production_ticket.view` | `bar.production_ticket_event.list` / `bar.production_ticket_event.list_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | 查询酒吧出品票据 | `bar.order.create` | `bar.production_ticket.viewed` / `bar.production_ticket.viewed_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarProducts` | `GET` | `/api/v1/bar/products` | 查询酒吧商品 | `bar.product.view` | `bar.product.viewed` / `bar.product.viewed_failed` | web, tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | 按分类查询酒吧商品 | `bar.product.view` | `bar.product.viewed` / `bar.product.viewed_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | 查询酒吧预约 | `bar.reservation.view` | `bar.reservation.viewed` / `bar.reservation.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | 查询收银班次 | `bar.shift.manage` | `bar.shift.viewed` / `bar.shift.view_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | 酒吧空位视图(员工代订用) | `bar.reservation.create` | `bar.table_availability.viewed` / `bar.table_availability.view_failed` | staff_private_phone, web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarTables` | `GET` | `/api/v1/bar/tables` | 查询桌台或卡座 | `bar.table.view` | `bar.table.viewed` / `bar.table.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | 查询酒水台位会话列表 | `bar.table_session.view` | `bar.table_session.list` / `bar.table_session.list_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | 未回填空间的卡座/桌台清单 | `bar.table.manage` | `bar.table_space_backlog.viewed` / `bar.table_space_backlog.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | 列退酒原因（中文下拉数据源） | `bar.order_item.void` | `bar.void_reason_code.viewed` / `bar.void_reason_code.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | 商品沽清/恢复 | `bar.product.sold_out` | `bar.product_sold_out` / `bar.product_sold_out_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `mergeBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/merge` | 合并酒水账单 | `bar.bill.split_merge` | `bar.bill.merge` / `bar.bill.merge_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | 合并酒水台位会话 | `bar.table_session.merge` | `bar.table_session.merge` / `bar.table_session.merge_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | 开班 | `bar.shift.manage` | `bar.shift_opened` / `bar.shift_open_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | 开台 | `bar.table.open_close` | `bar.table.opened` / `bar.table.open_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | 订台定金支付 | `bar.reservation.manage` | `bar.reservation_deposit_paid` / `bar.reservation_deposit_pay_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | 扫码点单（绑桌台会话） | `bar.order.create` | `bar.scan_order_created` / `bar.scan_order_create_failed` | miniapp, tablet | operation 级补充 |
| BAR-酒吧经营 | `redeemBarDrinkEntitlement` | `POST` | `/api/v1/bar/entitlement-redemptions/drink` | 酒水权益兑换承接 | `bar.entitlement.redeem` | `bar.entitlement_redeemed` / `bar.entitlement_redeem_failed` | staff_work_phone, tablet, web | operation 级补充 |
| BAR-酒吧经营 | `renewBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/renew` | 续台（延长台位会话） | `bar.table_session.renew` | `bar.table_session.renew` / `bar.table_session.renew_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | 账单反结 | `bar.bill.reopen` | `bar.bill_reopened` / `bar.bill_reopen_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | 吊销订卡邀请函 | `bar.reservation.invitation.manage` | `bar.reservation_invitation_revoked` / `bar.reservation_invitation_revoke_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | 新增或修改酒吧套餐 | `bar.combo.manage` | `bar.combo.saved` / `bar.combo.saved_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | 新增或修改酒吧商品分类 | `bar.category.manage` | `bar.category.saved` / `bar.category.saved_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | 新增或修改退酒原因 | `bar.order_item.void_decide` | `bar.void_reason_code.saved` / `bar.void_reason_code.save_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | 账单结清 | `finance.payment.register` | `bar.bill.settled` / `bar.bill.settle_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `splitBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/split` | 拆分酒水账单 | `bar.bill.split_merge` | `bar.bill.split` / `bar.bill.split_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | 转台（台位会话转移） | `bar.table_session.transfer` | `bar.table_session.transfer` / `bar.table_session.transfer_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | 流转出品工单状态 | `bar.production_ticket.manage` | `bar.production_ticket.transition` / `bar.production_ticket.transition_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | 上传酒吧商品/套餐图片 | `bar.product.manage` | `bar.catalog.image_uploaded` / `bar.catalog.image_uploaded_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `verifyBarReservationEntryAllowance` | `POST` | `/api/v1/bar/reservations/{id}/entry-allowance/verify` | 卡座免通票入场名额校验 | `bar.table.guest_checkin` | `bar.table_guest.entry_allowance_verified` / `bar.table_guest.entry_allowance_verify_failed` | staff_work_phone, kiosk | operation 级补充 |
| BAR-酒吧经营 | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | 订卡邀请函入场核验 | `bar.table.guest_checkin` | `bar.reservation_invitation_entered` / `bar.reservation_invitation_entry_failed` | staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | 退菜退酒 | `bar.order_item.void` | `bar.order_item_voided` / `bar.order_item_void_failed` | tablet, web | operation 级补充 |
| CUS-客人端 | `blacklistCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist` | 拉黑客人账号（永久禁止使用服务，无到期） | `customer.account.manage` | `customer.account.blacklisted` / `customer.account.blacklist_failed` | web | operation 级补充 |
| CUS-客人端 | `claimCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/claim` | 确认领取转赠(手机号只指定人可领;链接/二维码先确认者得) | `not_applicable` | `voucher_transfer.claimed` / `voucher_transfer.claim_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `configureStoredValueBonusPolicy` | `POST` | `/api/v1/loyalty/stored-value/bonus-policies` | 配置综合体通用卡充值赠送比例（仅平台，按项目，须同时配置上下限） | `customer.stored_value.refund` | `stored_value.bonus_policy_configured` / `stored_value.bonus_policy_config_failed` | web | operation 级补充 |
| CUS-客人端 | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | 配置商户储值卡（仅平台）：发卡资格 + 资金模式（预收/代收）+ 消费扣款顺序 | `customer.stored_value.refund` | `stored_value.merchant_card_configured` / `stored_value.merchant_card_config_failed` | web | operation 级补充 |
| CUS-客人端 | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | 配置商户级储值卡退款规则（发卡商户自设可否退/比例） | `customer.stored_value.refund` | `stored_value.merchant_refund_rule_configured` / `stored_value.merchant_refund_rule_failed` | web | operation 级补充 |
| CUS-客人端 | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | 会员储值到店柜台退款（商户预收卡唯一可达退款路径） | `customer.stored_value.refund` | `stored_value.counter_refund` / `stored_value.counter_refund_failed` | web | operation 级补充 |
| CUS-客人端 | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | 隐私同意记录 | `not_applicable` | `customer.consent.created` / `customer.consent.create_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `createVoucherDefinition` | `POST` | `/api/v1/vouchers/definitions` | 新建券定义（商户折扣券/店内实物券；外部伙伴券仍配置在通票权益模板 V282） | `customer.voucher.manage` | `voucher.definition_created` / `voucher.definition_create_failed` | web | operation 级补充 |
| CUS-客人端 | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | 客人每日签到 | `not_applicable` | `customer.checked_in` / `customer.check_in_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | 客人登出（清除免登录会话 cookie） | `public` | `customer.logged_out` / `customer.logout_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | 客人手机号登录 | `public` | `customer.auth.login` / `customer.auth.login_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | 客人储值账户查询 | `customer.stored_value.view` | `customer.stored_value_account_viewed` / `customer.stored_value_account_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | 客人储值卡列表（我的全部卡：通用卡 + 各商户卡分别余额） | `customer.stored_value.view` | `customer.stored_value_accounts_viewed` / `customer.stored_value_accounts_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueConsume` | `POST` | `/api/v1/customer/stored-value/consume` | 客人储值消费（余额扣减） | `customer.stored_value.consume` | `customer.stored_value.consume` / `customer.stored_value.consume_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | 客人储值充值入账（支付成功后） | `customer.stored_value.topup` | `customer.stored_value.settle` / `customer.stored_value.settle_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | 客人储值充值（绑支付意图） | `customer.stored_value.topup` | `customer.stored_value.topup` / `customer.stored_value.topup_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerVoucherWallet` | `GET` | `/api/v1/customer/vouchers` | 我的券包（客人端只读：积分兑换券/营销赠券/通票第三方券聚合，按状态分组展示） | `customer.voucher.view` | `voucher.customer_wallet_viewed` / `voucher.customer_wallet_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `deregisterCustomerAccount` | `POST` | `/api/v1/customer/deregister` | 客人自助注销本人账号（注销后登录/动作被拒，历史数据物理保留但客人不可见） | `not_applicable` | `customer.account.deregistered` / `customer.account.deregister_failed` | miniapp, web | operation 级补充 |
| CUS-客人端 | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | 手动触发会员等级评估 | `customer.loyalty.evaluate` | `member.level_changed` / `customer.level_evaluate_failed` | web | operation 级补充 |
| CUS-客人端 | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | 冻结会员积分账户 | `customer.points.freeze` | `customer.points.frozen` / `customer.points.freeze_failed` | web | operation 级补充 |
| CUS-客人端 | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | 冻结会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.frozen` / `stored_value.frozen_failed` | web | operation 级补充 |
| CUS-客人端 | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | 积分余额 | `not_applicable` | `customer.points_viewed` / `customer.points_view_failed` | web, miniapp, app | 已在任务映射正文出现 |
| CUS-客人端 | `getCustomerPointsLedger` | `GET` | `/api/v1/customer/points/ledger` | 查询积分流水 | `not_applicable` | `customer.points.ledger_viewed` / `customer.points.ledger_view_failed` | app, miniapp, web | operation 级补充 |
| CUS-客人端 | `getCustomerSession` | `GET` | `/api/v1/customer/session` | 客人会话恢复（免登录） | `not_applicable` | `customer.session_viewed` / `customer.session_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `getStoredValueBonusPolicy` | `GET` | `/api/v1/loyalty/stored-value/bonus-policies/{project_id}` | 查询综合体通用卡充值赠送比例（仅平台） | `customer.stored_value.refund` | `stored_value.bonus_policy_viewed` / `stored_value.bonus_policy_view_failed` | web | operation 级补充 |
| CUS-客人端 | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | 查询商户级储值卡退款规则 | `customer.stored_value.refund` | `stored_value.merchant_refund_rule_viewed` / `stored_value.merchant_refund_rule_view_failed` | web | operation 级补充 |
| CUS-客人端 | `initiateCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers` | 发起券转赠(手机号/微信链接/二维码) | `not_applicable` | `voucher_transfer.created` / `voucher_transfer.create_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `issueVouchers` | `POST` | `/api/v1/vouchers/instances` | 营销赠发券（按定义给客人发放；商户 scope 只能发自家券） | `customer.voucher.manage` | `voucher.issued` / `voucher.issue_failed` | web | operation 级补充 |
| CUS-客人端 | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | 客人签到记录 | `not_applicable` | `customer.check_ins_viewed` / `customer.check_ins_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | 我的订单 | `not_applicable` | `customer.orders_viewed` / `customer.orders_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `listCustomerPointsRedemptionItems` | `GET` | `/api/v1/customer/points/redemption-items` | 查询积分兑换商品目录 | `not_applicable` | `customer.points.redemption_items_viewed` / `customer.points.redemption_items_view_failed` | app, miniapp, web | operation 级补充 |
| CUS-客人端 | `listCustomerPointsRedemptions` | `GET` | `/api/v1/customer/points/redemptions` | 查询我的积分兑换记录 | `not_applicable` | `customer.points.redemptions_viewed` / `customer.points.redemptions_view_failed` | app, miniapp, web | operation 级补充 |
| CUS-客人端 | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | 待我领取的转赠(手机号渠道) | `not_applicable` | `customer.voucher_transfer_incoming_viewed` / `customer.voucher_transfer_incoming_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | 会员等级配置列表 | `customer.loyalty.view` | `customer.member_levels_viewed` / `customer.member_levels_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | 我发起的转赠(撤回入口/状态查看) | `not_applicable` | `customer.voucher_transfer_outgoing_viewed` / `customer.voucher_transfer_outgoing_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listVoucherDefinitions` | `GET` | `/api/v1/vouchers/definitions` | 券定义列表（统一台账模板：类型/核销方式/结算口径；内部结算价按 voucher.internal_price.view 权限脱敏） | `customer.voucher.view` | `voucher.definitions_viewed` / `voucher.definitions_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherInstances` | `GET` | `/api/v1/vouchers/instances` | 券实例台账（收编积分兑换券＋营销赠券；状态机 issued→redeemed/expired/revoked） | `customer.voucher.view` | `voucher.instances_viewed` / `voucher.instances_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherOverview` | `GET` | `/api/v1/vouchers/overview` | 全部券聚合（读层并入不搬表：统一台账 ∪ 通票第三方合作券 V282） | `customer.voucher.view` | `voucher.overview_viewed` / `voucher.overview_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherRedemptions` | `GET` | `/api/v1/vouchers/redemptions` | 统一核销记录（append-only：核销点/操作员/结算挂钩一张表；settlement_amount_cent=内部结算价按 voucher.internal_price.view 权限脱敏） | `customer.voucher.view` | `voucher.redemptions_viewed` / `voucher.redemptions_view_failed` | web | operation 级补充 |
| CUS-客人端 | `lookupCustomerAccountByPhone` | `POST` | `/api/v1/customer-accounts/lookup` | 按手机号精确查客人账号（盲索引，返回安全投影；手机号走请求体不入 URL） | `customer.account.manage` | `customer.account.viewed` / `customer.account.view_failed` | web | operation 级补充 |
| CUS-客人端 | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | 领取预览(打开链接/二维码,不算领取) | `not_applicable` | `customer.voucher_transfer_preview_viewed` / `customer.voucher_transfer_preview_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `redeemCustomerPoints` | `POST` | `/api/v1/customer/points/redemptions` | 提交积分兑换 | `not_applicable` | `customer.points.redeem` / `customer.points.redeem_failed` | app, miniapp, web | operation 级补充 |
| CUS-客人端 | `redeemVoucher` | `POST` | `/api/v1/vouchers/redemptions` | 统一核销（三类口径：折扣券结账抵扣/实物券扣库存＋内部结算价挂应付；每券一次；响应 settlement_amount_cent 按 voucher.internal_price.view 权限脱敏） | `customer.voucher.redeem` | `voucher.redeemed` / `voucher.redeem_failed` | web | operation 级补充 |
| CUS-客人端 | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | 会员储值退卡/退余额（后台） | `customer.stored_value.refund` | `stored_value.refund` / `stored_value.refund_failed` | web | operation 级补充 |
| CUS-客人端 | `reinstateCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/reinstate` | 恢复客人账号为正常（解封/解黑） | `customer.account.manage` | `customer.account.reinstated` / `customer.account.reinstate_failed` | web | operation 级补充 |
| CUS-客人端 | `revokeCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/revoke` | 撤回转赠(领取前发起人可撤回) | `not_applicable` | `voucher_transfer.revoked` / `voucher_transfer.revoke_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `revokeVoucher` | `POST` | `/api/v1/vouchers/instances/{id}/revoke` | 作废券（仅未核销可作废；理由必填进审计留痕） | `customer.voucher.manage` | `voucher.revoked` / `voucher.revoke_failed` | web | operation 级补充 |
| CUS-客人端 | `suspendCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/suspend` | 封禁客人账号（临时，到期客人再登录自动恢复正常；期内购买/使用被拒） | `customer.account.manage` | `customer.account.suspended` / `customer.account.suspend_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | 解冻会员积分账户 | `customer.points.freeze` | `customer.points.unfrozen` / `customer.points.unfreeze_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | 解冻会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.unfrozen` / `stored_value.unfrozen_failed` | web | operation 级补充 |
| CUS/BAR-客人订台 | `confirmCustomerBarInvitation` | `POST` | `/api/v1/customer/bar-invitations/confirm` | 客人确认接受订卡邀请函 | `not_applicable` | `customer.bar_invitation.confirmed` / `customer.bar_invitation.confirm_failed` | web, miniapp, app | operation 级补充 |
| CUS/BAR-客人订台 | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | 客人卡座预订 | `not_applicable` | `customer.bar_reservation.created` / `customer.bar_reservation.create_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/BAR-客人订台 | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | 客人查看自己的订卡邀请函 | `not_applicable` | `customer.bar_invitation.viewed` / `customer.bar_invitation.view_failed` | web, miniapp, app | operation 级补充 |
| CUS/BAR-客人订台 | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | 客人可订台资源 | `not_applicable` | `customer.bar_reservation_options_viewed` / `customer.bar_reservation_options_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/BAR-客人订台 | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-sessions/{session_id}/menu` | 客人扫码点单查看菜单 | `not_applicable` | `customer.bar_scan_menu.viewed` / `customer.bar_scan_menu.view_failed` | web, miniapp, app | operation 级补充 |
| CUS/BAR-客人订台 | `placeCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-sessions/{session_id}/scan-orders` | 客人自助扫码点单 | `not_applicable` | `customer.bar_scan_order.created` / `customer.bar_scan_order.create_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | 确认领取转赠(手机号只指定人可领;链接/二维码先确认者得) | `not_applicable` | `ticket_transfer.claimed` / `ticket_transfer.claim_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | 客人自助购票下单 | `not_applicable` | `customer.ticket_order.created` / `customer.ticket_order.create_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `getCustomerTicketQr` | `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | 我的动态二维码 | `not_applicable` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `getOwnTicketUseRewards` | `GET` | `/api/v1/customer/ticket-rewards` | 查看本人实际使用门票奖励及待追回积分 | `not_applicable` | `ticket.reward.customer.viewed` / `ticket.reward.customer.viewed.failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | 发起门票转赠(手机号/微信链接/二维码) | `not_applicable` | `ticket_transfer.created` / `ticket_transfer.create_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | 客人查询本人订单内票实例/权益（安全投影；点票出码，无需手输票 id） | `not_applicable` | `customer.order_tickets_viewed` / `customer.order_tickets_view_failed` | miniapp, web | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | 客人可购票种和票价 | `public` | `customer.ticket_products_viewed` / `customer.ticket_products_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/TIC-客人购票 | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | 待我领取的转赠(手机号渠道) | `not_applicable` | `customer.ticket_transfer_incoming_viewed` / `customer.ticket_transfer_incoming_view_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | 我的票(持有人视角:自购未转+受赠,含转赠中标记) | `not_applicable` | `customer.my_tickets_viewed` / `customer.my_tickets_view_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | 我发起的转赠(撤回入口/状态查看) | `not_applicable` | `customer.ticket_transfer_outgoing_viewed` / `customer.ticket_transfer_outgoing_view_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listPublishedTicketRewardRules` | `GET` | `/api/v1/customer/ticket-reward-rules` | 查看已公布门票积分及成长值规则 | `not_applicable` | `ticket.reward.rules.viewed` / `ticket.reward.rules.viewed.failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | 领取预览(打开链接/二维码,不算领取) | `not_applicable` | `customer.ticket_transfer_preview_viewed` / `customer.ticket_transfer_preview_view_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | 撤回转赠(领取前发起人可撤回) | `not_applicable` | `ticket_transfer.revoked` / `ticket_transfer.revoke_failed` | web, miniapp, app | operation 级补充 |
| EVD-事件证据 | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | 关联证据附件到业务对象 | `evidence.attachment.create` | `evidence.attachment_bound` / `evidence.attachment_bind_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `claimRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/claim` | 认领风险预警 | `risk_alert.claim` | `exception.claimed` / `exception.claim_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | 完成上传确认 | `evidence.attachment.create` | `evidence.attachment_completed` / `evidence.attachment_complete_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | 申请证据附件预签名上传 | `evidence.attachment.create` | `evidence.upload_intent_created` / `evidence.upload_intent_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | 创建证据事件 | `evidence.event.create` | `evidence.events_reported` / `evidence.events_report_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| EVD-事件证据 | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | 风险或异常复核 | `evidence.review.create` | `evidence.review_created` / `evidence.review_create_failed` | web | operation 级补充 |
| EVD-事件证据 | `dismissRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/dismiss` | 忽略关闭风险预警 | `risk_alert.dismiss` | `exception.dismissed` / `exception.dismiss_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `escalateRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/escalate` | 升级风险预警 | `risk_alert.escalate` | `exception.escalated` / `exception.escalate_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | 业务时间线 | `evidence.timeline.view` | `evidence.timeline_viewed` / `evidence.timeline_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | 查询风险预警详情 | `risk_alert.view` | `exception.alert_viewed` / `exception.alert_view_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | 查询业务流程追溯自动采集率 | `evidence.timeline.view` | `evidence.traceability_auto_capture_rate_viewed` / `evidence.traceability_auto_capture_rate_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | 事件查询 | `evidence.event.view` | `evidence.event_viewed` / `evidence.event_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | 回读证据文件 | `evidence.timeline.view` | `evidence.attachment_file_viewed` / `evidence.attachment_file_view_failed` | web, tablet | operation 级补充 |
| EVD-事件证据 | `reportRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts` | 上报统一风险预警 | `risk_alert.report` | `exception.reported` / `exception.report_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `resolveEvidenceRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/resolve` | 关闭风险预警（需结论） | `risk_alert.resolve` | `exception.resolved` / `exception.resolve_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `searchRiskAlerts` | `GET` | `/api/v1/evidence/risk-alerts` | 查询风险预警列表 | `risk_alert.view` | `exception.alerts_viewed` / `exception.alerts_view_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `transferRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/transfer` | 转交风险预警 | `risk_alert.transfer` | `exception.transferred` / `exception.transfer_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | 上传证据照片/视频 | `evidence.attachment.create` | `evidence.attachment_uploaded` / `evidence.attachment_uploaded_failed` | web, tablet, staff_work_phone | operation 级补充 |
| FIN-财务台账 | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | 锁定后补录审批决定 | `finance.locked_backfill.approve` | `finance.locked_backfill_approved` / `finance.locked_backfill_decision_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | 审批通过：pending_review → approved；净发放额>0 时给主理人挂应付（source_type=dividend_payout，一单一挂）；提交人不得审批自己提交的单 | `finance.dividend.manage` | `dividend.approved` / `dividend.approve_failed` | web | operation 级补充 |
| FIN-财务台账 | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | 物业费按期计费（每平米固定单价 × 面积，不退不补） | `finance.property_fee.manage` | `finance_property_fee.billed` / `finance_property_fee.bill_failed` | web | operation 级补充 |
| FIN-财务台账 | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | 营业额抽成计费生成应收 | `finance.turnover_rent.bill` | `finance.turnover_rent.billed` / `finance.turnover_rent.bill_failed` | web | operation 级补充 |
| FIN-财务台账 | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | 正式关闭对账差异 | `finance.reconciliation.resolve` | `finance.reconciliation.difference_closed` / `finance.reconciliation.difference_close_failed` | web | operation 级补充 |
| FIN-财务台账 | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | 期间分红作业：对每个生效中方案出计算单（净利润=收入−成本−费用；幂等可重跑，草稿/预览/驳回作废重算，进入审批流跳过） | `finance.dividend.manage` | `dividend.computed` / `dividend.compute_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmFinanceRefundResult` | `POST` | `/api/v1/finance/refunds/{refund_id}/confirm` | 另一人核实已有执行记录的实际退款结果 | `finance.refund.create` | `finance_refund.confirmed` / `finance_refund.confirm_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | 现金或 POS 线下收款确认 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | 手工新增应收应付台账项 | `finance.ar_ap.manage` | `finance_ar_ap.created` / `finance_ar_ap.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | 应收应付收付款登记 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | 日结锁定 | `finance.daily_close.lock` | `finance_daily_close.locked` / `finance_daily_close.lock_failed` | web | operation 级补充 |
| FIN-财务台账 | `createDividendPlan` | `POST` | `/api/v1/finance/dividends/plans` | 新建分红方案（比例/税率/周期/基数构成全可配，80/20 仅默认值） | `finance.dividend.manage` | `dividend.plan_created` / `dividend.plan_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | 创建对账差异 | `finance.reconciliation.difference.create` | `finance.reconciliation.difference_created` / `finance.reconciliation.difference_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | 创建财务退款记录 | `finance.refund.create` | `finance_refund.created` / `finance_refund.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses` | 手工录入商户费用（人工/摊销/其他，理由必填） | `finance.merchant_expense.manage` | `merchant_expense.manual_created` / `merchant_expense.manual_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createPaymentIntent` | `POST` | `/api/v1/finance/payment-intents` | 创建聚合支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, tablet, kiosk, miniapp, app | operation 级补充 |
| FIN-财务台账 | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | 维护物业费费率（项目级、可按期） | `finance.property_fee.manage` | `finance_property_fee.rate_set` / `finance_property_fee.rate_set_failed` | web | operation 级补充 |
| FIN-财务台账 | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | 导出应收应付台账快照 | `finance.ar_ap.export` | `finance_ar_ap.exported` / `finance_ar_ap.export_failed` | web | operation 级补充 |
| FIN-财务台账 | `getDividendStatement` | `GET` | `/api/v1/finance/dividends/statements/{id}` | 分红计算单详情（头 + 算式明细行，逐项可追溯到三个来源台账） | `finance.dividend.view` | `dividend.statement_viewed` / `dividend.statement_view_failed` | web, merchant_portal | operation 级补充 |
| FIN-财务台账 | `getFinanceRefund` | `GET` | `/api/v1/finance/refunds/{refund_id}` | 读取退款详情和全部办理记录 | `finance.ar_ap.view` | `finance_refund.viewed` / `finance_refund.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | 查询应收应付台账 | `finance.ar_ap.view` | `finance_ar_ap.viewed` / `finance_ar_ap.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | 读取所选台账原收付款流水 | `finance.ar_ap.view` | `ar_ap_payment.listed` / `ar_ap_payment.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | 查询日结六源汇总 | `finance.daily_close.manage` | `finance.daily_close_summary.viewed` / `finance.daily_close_summary.view_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `listDividendPlans` | `GET` | `/api/v1/finance/dividends/plans` | 分红方案列表（商户 scope 账号只见本商户） | `finance.dividend.view` | `dividend.plans_listed` / `dividend.plans_list_failed` | web, merchant_portal | operation 级补充 |
| FIN-财务台账 | `listDividendReserveReleases` | `GET` | `/api/v1/finance/dividends/reserve-releases` | 发放保护线留存·待释放/已释放台账（商户 scope 账号只见本商户，主理人视图同入口） | `finance.dividend.view` | `dividend.reserve_ledger_listed` / `dividend.reserve_ledger_list_failed` | web, merchant_portal | operation 级补充 |
| FIN-财务台账 | `listDividendStatements` | `GET` | `/api/v1/finance/dividends/statements` | 分红计算单分页（商户 scope 账号只见本商户，主理人视图同入口） | `finance.dividend.view` | `dividend.statements_listed` / `dividend.statements_list_failed` | web, merchant_portal | operation 级补充 |
| FIN-财务台账 | `listFinanceRefunds` | `GET` | `/api/v1/finance/refunds` | 读取退款台账及未结状态 | `finance.ar_ap.view` | `finance_refund.listed` / `finance_refund.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listMerchantExpensePeriodLocks` | `GET` | `/api/v1/finance/merchant-expenses/period-locks` | 查询某月已关账（分红核算完成、费用台账冻结）的商户清单（前端据此禁用费用录入/冲销） | `finance.merchant_expense.view` | `merchant_expense.period_locks_viewed` / `merchant_expense.period_locks_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses` | 商户费用归集台账分页查询（分红净利润费用侧） | `finance.merchant_expense.view` | `merchant_expense.listed` / `merchant_expense.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | 列出某项目的物业费费率 | `finance.property_fee.manage` | `finance_property_fee.rate_viewed` / `finance_property_fee.rate_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | 查询对账批次 | `finance.reconciliation.view` | `finance.reconciliation_batch.viewed` / `finance.reconciliation_batch.viewed_failed` | web | operation 级补充 |
| FIN-财务台账 | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | 对账差异 | `finance.reconciliation.view` | `finance_reconciliation.difference_found` / `finance_reconciliation.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | 查询退款自助额度配置 | `refund.authority.view` | `refund_authority_limit.viewed` / `refund_authority_limit.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | 查询退款自动放行规则配置 | `refund.authority.view` | `refund_auto_rule.viewed` / `refund_auto_rule.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `markDividendStatementPaid` | `POST` | `/api/v1/finance/dividends/statements/{id}/mark-paid` | 标记发放：approved → paid（实际打款走应收应付台账既有付款确认流程） | `finance.dividend.manage` | `dividend.paid_marked` / `dividend.mark_paid_failed` | web | operation 级补充 |
| FIN-财务台账 | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | 库存差异复核入账为 AR/AP | `finance.inventory_variance.post` | `finance.inventory_variance.posted` / `finance.inventory_variance.post_failed` | web | operation 级补充 |
| FIN-财务台账 | `queryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/query` | 查询原退款单的渠道权威结果 | `finance.ar_ap.view` | `finance_refund.queried` / `finance_refund.query_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | 押金台账动作（收取/退还/抵扣/没收） | `finance.deposit.manage` | `finance.deposit.action_recorded` / `finance.deposit.action_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordFinanceRefundExecution` | `POST` | `/api/v1/finance/refunds/{refund_id}/execution` | 实际支付经办人本人登记原路执行凭证 | `finance.refund.create` | `finance_refund.execution_recorded` / `finance_refund.execution_failed` | web | operation 级补充 |
| FIN-财务台账 | `rejectDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/reject` | 驳回：pending_review → rejected（理由必填；驳回后可重算） | `finance.dividend.manage` | `dividend.rejected` / `dividend.reject_failed` | web | operation 级补充 |
| FIN-财务台账 | `releaseDividendReserve` | `POST` | `/api/v1/finance/dividends/statements/{id}/release-reserve` | 手动释放该计算单的发放保护线留存（re-tag 待释放留存应付为已释放，使主理人可真正拿到该笔钱） | `finance.dividend.manage` | `dividend.reserve_released` / `dividend.reserve_release_failed` | web | operation 级补充 |
| FIN-财务台账 | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | 锁定后补录审批申请 | `finance.locked_backfill.request` | `finance.locked_backfill_requested` / `finance.locked_backfill_request_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `retryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/retry` | 明确失败或尚未发起的退款在原单重试 | `finance.refund.create` | `finance_refund.retried` / `finance_refund.retry_failed` | web | operation 级补充 |
| FIN-财务台账 | `reverseMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses/{id}/reverse` | 红字冲销费用台账行（负金额新行，不改历史行，理由必填） | `finance.merchant_expense.manage` | `merchant_expense.reversed` / `merchant_expense.reverse_failed` | web | operation 级补充 |
| FIN-财务台账 | `submitDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/submit` | 提交审阅：draft → pending_review（预览单不可提交） | `finance.dividend.manage` | `dividend.submitted` / `dividend.submit_failed` | web | operation 级补充 |
| FIN-财务台账 | `summarizeMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses/summary` | 商户费用期间汇总（按商户+月份分类型合计，分红引擎费用侧取数点） | `finance.merchant_expense.view` | `merchant_expense.summary_viewed` / `merchant_expense.summary_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `syncMerchantExpenseAuto` | `POST` | `/api/v1/finance/merchant-expenses/sync-auto` | 自动挂接能耗费/物业费/租金账单到费用台账（按期幂等可重跑） | `finance.merchant_expense.manage` | `merchant_expense.auto_synced` / `merchant_expense.auto_sync_failed` | web | operation 级补充 |
| FIN-财务台账 | `updateDividendPlan` | `PUT` | `/api/v1/finance/dividends/plans/{id}` | 修改分红方案（改动审计留痕；历史分红单按单内快照追溯，不受影响） | `finance.dividend.manage` | `dividend.plan_updated` / `dividend.plan_update_failed` | web | operation 级补充 |
| FIN-财务台账 | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | 新增或更新退款自助额度 | `refund.authority.manage` | `refund_authority.changed` / `refund_authority.change_failed` | web | operation 级补充 |
| FIN-财务台账 | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | 新增或更新退款自动放行规则 | `refund.authority.manage` | `refund_authority.changed` / `refund_authority.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | 主管审批或驳回复核记录 | `governance.supervisor_review.approve` | `governance.supervisor_review.decided` / `governance.supervisor_review.decide_failed` | web | operation 级补充 |
| GOV-平台治理 | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | 变更四类主体绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | 创建经营、运营、合同、结算主体档案 | `entity.binding.manage` | `business_entity.changed` / `business_entity.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | 创建四类主体唯一绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createMerchant` | `POST` | `/api/v1/platform/merchants` | 创建商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `createProject` | `POST` | `/api/v1/platform/projects` | 创建项目 | `governance.project.manage` | `project.created` / `project.create_failed` | web | operation 级补充 |
| GOV-平台治理 | `createSpace` | `POST` | `/api/v1/platform/spaces` | 创建场馆、区域、点位 | `governance.space.manage` | `space.updated` / `space.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | 录入/修改空间类型字典 | `governance.space.manage` | `space_type.updated` / `space_type.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | 评估业务动作是否需要主管复核 | `governance.supervisor_review.manage` | `governance.supervisor_review.evaluated` / `governance.supervisor_review.evaluate_failed` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | 查询经营、运营、合同、结算主体档案 | `entity.binding.view` | `business_entity.viewed` / `business_entity.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | 查询四类主体绑定 | `entity.binding.view` | `entity_binding.viewed` / `entity_binding.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listMerchants` | `GET` | `/api/v1/platform/merchants` | 查询商户 | `merchant.profile.view` | `merchant.viewed` / `merchant.view_failed` | web, merchant_portal | operation 级补充 |
| GOV-平台治理 | `listProjects` | `GET` | `/api/v1/platform/projects` | 查询综合体项目 | `governance.project.view` | `project.viewed` / `project.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listSpaces` | `GET` | `/api/v1/platform/spaces` | 查询场馆、区域、点位 | `governance.space.view` | `space.viewed` / `space.view_failed` | web, merchant_portal | operation 级补充 |
| GOV-平台治理 | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | 空间类型字典列表 | `governance.space.view` | `space_type.viewed` / `space_type.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | 查询主管复核与违规阻断策略 | `governance.supervisor_review.view` | `governance.supervisor_review.policies_viewed` / `governance.supervisor_review.policies_view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listVenueBusinessHours` | `GET` | `/api/v1/platform/venue-business-hours` | 查询场地营业时间 | `governance.space.view` | `venue_business_hours.viewed` / `venue_business_hours.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | 配置场地营业时间 | `governance.space.manage` | `venue_business_hours.updated` / `venue_business_hours.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | 修改商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| INT-第三方集成 | `archiveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps/{id}/archive` | 停用渠道票种映射 | `integration.channel_store_map.manage` | `integration.channel_ticket_type_map.archived` / `integration.channel_ticket_type_map.archive_failed` | web | operation 级补充 |
| INT-第三方集成 | `createCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks` | 创建补偿任务 | `integration.compensation.manage` | `integration.compensation_task.created` / `integration.compensation_task.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createIntegrationReconciliationBatch` | `POST` | `/api/v1/integration/reconciliation/batches` | 创建对账批次 | `integration.reconciliation.manage` | `integration.reconciliation_batch.created` / `integration.reconciliation_batch.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences` | 创建对账差异 | `integration.reconciliation.manage` | `integration.reconciliation_difference.created` / `integration.reconciliation_difference.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createOrderSync` | `POST` | `/api/v1/integration/order-syncs` | 创建订单同步 | `integration.order_sync.manage` | `integration.order_sync.created` / `integration.order_sync.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createProviderAdapter` | `POST` | `/api/v1/integration/provider-adapters` | 创建供应商适配器占位或 mock adapter | `integration.provider_adapter.create` | `provider_adapter.created` / `provider_adapter.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createRetryTask` | `POST` | `/api/v1/integration/retry-tasks` | 创建重试任务 | `integration.retry.manage` | `integration.retry_task.created` / `integration.retry_task.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationChannelTicketTypeMaps` | `GET` | `/api/v1/integration/channel-ticket-type-maps` | 查询渠道票种映射 | `integration.channel_store_map.view` | `integration.channel_ticket_type_map.viewed` / `integration.channel_ticket_type_map.view_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | 查询第三方集成补偿任务 | `integration.provider_adapter.view` | `integration.compensation_task.viewed` / `integration.compensation_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | 查询第三方集成设备事件 | `integration.provider_adapter.view` | `integration.device_event.viewed` / `integration.device_event.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | 查询第三方集成设备 | `integration.provider_adapter.view` | `integration.device.viewed` / `integration.device.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | 查询第三方集成离线批次 | `integration.provider_adapter.view` | `integration.offline_sync_batch.viewed` / `integration.offline_sync_batch.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | 查询第三方集成订单syncs | `integration.provider_adapter.view` | `integration.order_sync.viewed` / `integration.order_sync.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationReconciliationBatches` | `GET` | `/api/v1/integration/reconciliation/batches` | 查询对账批次列表 | `integration.reconciliation.view` | `integration.reconciliation_batch.viewed` / `integration.reconciliation_batch.view_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationReconciliationDifferences` | `GET` | `/api/v1/integration/reconciliation/differences` | 查询对账差异列表 | `integration.reconciliation.view` | `integration.reconciliation_difference.viewed` / `integration.reconciliation_difference.view_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | 查询第三方集成零售SKU | `integration.provider_adapter.view` | `integration.retail_sku.viewed` / `integration.retail_sku.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationRetryTasks` | `GET` | `/api/v1/integration/retry-tasks` | 查询第三方集成retry任务 | `integration.provider_adapter.view` | `integration.retry_task.viewed` / `integration.retry_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationVideoAnchors` | `GET` | `/api/v1/integration/video-anchors` | 调阅视频点位时间锚点（敏感，写调阅审计） | `integration.video.view` | `integration.video_clip_viewed` / `integration.video_clip_view_failed` | web, mobile_admin | operation 级补充 |
| INT-第三方集成 | `listIntegrationWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | 查询企业微信风险命中 | `integration.provider_adapter.view` | `integration.wecom_risk_hit.viewed` / `integration.wecom_risk_hit.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listProviderAdapters` | `GET` | `/api/v1/integration/provider-adapters` | 供应商适配器查询 | `integration.provider_adapter.view` | `provider_adapter.viewed` / `provider_adapter.view_failed` | web | operation 级补充 |
| INT-第三方集成 | `listRobotTaskEvents` | `GET` | `/api/v1/integration/robot/task-events` | 查询机器人task事件 | `integration.provider_adapter.view` | `integration.robot_task_event.viewed` / `integration.robot_task_event.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listRobotTasks` | `GET` | `/api/v1/integration/robot/tasks` | 查询机器人任务 | `integration.provider_adapter.view` | `integration.robot_task.viewed` / `integration.robot_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listVideoCameras` | `GET` | `/api/v1/integration/video/cameras` | 查询视频摄像头 | `integration.provider_adapter.view` | `integration.video_camera.viewed` / `integration.video_camera.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listVoucherVerifications` | `GET` | `/api/v1/integration/voucher-verifications` | 查询券核销记录 | `integration.provider_adapter.view` | `integration.voucher_verification.viewed` / `integration.voucher_verification.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomBindings` | `GET` | `/api/v1/integration/wecom/bindings` | 查询企业微信绑定 | `integration.provider_adapter.view` | `integration.wecom_binding.viewed` / `integration.wecom_binding.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomConversations` | `GET` | `/api/v1/integration/wecom/conversations` | 查询企业微信会话 | `integration.provider_adapter.view` | `integration.wecom_conversation.viewed` / `integration.wecom_conversation.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomMessages` | `GET` | `/api/v1/integration/wecom/messages` | 查询企业微信消息 | `integration.provider_adapter.view` | `integration.wecom_message.viewed` / `integration.wecom_message.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `markProviderAdapterProductionReady` | `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | 标记供应商适配器生产就绪 | `integration.provider_adapter.production_ready` | `provider_adapter.production_ready_marked` / `provider_adapter.production_ready_failed` | web | operation 级补充 |
| INT-第三方集成 | `receiveIntegrationTypedWebhook` | `POST` | `/api/v1/integration/{type}/webhook` | 第三方统一 webhook 入口（开关门控） | `not_applicable` | `integration.webhook_received` / `integration.webhook_rejected` | <br>channel: provider-webhook | operation 级补充 |
| INT-第三方集成 | `recordIntegrationPollingCheck` | `POST` | `/api/v1/integration/polling-checks` | 登记第三方定时轮询校验快照 | `integration.retry.manage` | `integration.polling_check.recorded` / `integration.polling_check.record_failed` | web | operation 级补充 |
| INT-第三方集成 | `reportIntegrationRobotTaskEvent` | `POST` | `/api/v1/integration/robot/report` | 机器人任务事件上报（被动留痕） | `not_applicable` | `integration.robot_reported` / `integration.robot_report_failed` | <br>channel: provider-webhook | operation 级补充 |
| INT-第三方集成 | `resolveCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks/{id}/resolve` | 处理补偿任务 | `integration.compensation.manage` | `integration.compensation_task.resolved` / `integration.compensation_task.resolve_failed` | web | operation 级补充 |
| INT-第三方集成 | `resolveIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences/{id}/resolve` | 处理对账差异 | `integration.reconciliation.resolve` | `integration.reconciliation_difference.resolved` / `integration.reconciliation_difference.resolve_failed` | web | operation 级补充 |
| INT-第三方集成 | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | 渠道自动对账 | `integration.reconciliation.manage` | `integration.channel_reconciliation.executed` / `integration.channel_reconciliation.execute_failed` | web | operation 级补充 |
| INT-第三方集成 | `runRetryTaskNow` | `POST` | `/api/v1/integration/retry-tasks/{id}/run-now` | 立即执行重试任务 | `integration.retry.manage` | `integration.retry_task.run_now` / `integration.retry_task.run_now_failed` | web | operation 级补充 |
| INT-第三方集成 | `saveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps` | 登记/改绑渠道票种映射 | `integration.channel_store_map.manage` | `integration.channel_ticket_type_map.saved` / `integration.channel_ticket_type_map.save_failed` | web | operation 级补充 |
| INT-第三方集成 | `testProviderAdapterMock` | `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | mock adapter 契约测试 | `integration.provider_adapter.test` | `provider_adapter.tested` / `provider_adapter.test_failed` | web | operation 级补充 |
| INT-第三方集成 | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | 更新供应商适配器配置 | `integration.provider_adapter.update` | `open_mapping.changed` / `provider_adapter.update_failed` | web | operation 级补充 |
| INV-库存存酒 | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | 采购单审批(created/submitted→approved/rejected) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | 审批供应合同(draft→approved/rejected) | `inventory.purchase.approve` | `supply_contract.approved` / `supply_contract.approve_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | 采购单到货(ordered/expected→arrived) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | 存酒作废(高价值→照片+视频双证据建复核;非高价值直接作废) | `bar.saved_bottle.manage` | `bar.saved_bottle.cancelled` / `bar.saved_bottle.cancel_failed` | tablet, web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | 取消借调单 | `inventory.transfer_order.manage` | `inventory.transfer_order_cancelled` / `inventory.transfer_order_cancel_failed` | web | operation 级补充 |
| INV-库存存酒 | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | 存酒领取 | `bar.saved_bottle.manage` | `bar.saved_bottle_picked_up` / `bar.saved_bottle.pickup_failed` | tablet, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `completeQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/dispositions/{id}/complete` | 登记批准的实际隔离处置 | `inventory.stock.adjust` | `inventory.quarantine.disposition_completed` / `inventory.quarantine.disposition_complete_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventoryItem` | `POST` | `/api/v1/inventory/items` | 创建库存物品 | `inventory.stock.move` | `inventory.item.created` / `inventory.item.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventoryStockBatch` | `POST` | `/api/v1/inventory/stock-batches` | 创建库存批次 | `inventory.stock.batch.manage` | `inventory.stock_batch.create` / `inventory.stock_batch.create_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createInventoryStocktake` | `POST` | `/api/v1/inventory/stocktakes` | 建库存盘点单 | `inventory.stocktake.create` | `inventory.stocktake.created` / `inventory.stocktake.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | 创建供应商 | `inventory.supplier.manage` | `inventory.supplier_changed` / `inventory.supplier_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | 创建采购申请 | `inventory.purchase_request.manage` | `inventory.purchase_request_changed` / `inventory.purchase_request_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | 存酒登记 | `bar.saved_bottle.manage` | `bar.saved_bottle.created` / `bar.saved_bottle.create_failed` | tablet, web | operation 级补充 |
| INV-库存存酒 | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | 盘点差异、报损、报溢 | `inventory.stock.adjust` | `inventory.counted` / `inventory.adjustment_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockIn` | `POST` | `/api/v1/inventory/stock-in` | 入库 | `inventory.stock.move` | `inventory.inbound_confirmed` / `inventory.inbound_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | 库存移动或扣减 | `inventory.stock.move` | `inventory.outbound_verified` / `inventory.stock_movement_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `createSupplierQualification` | `POST` | `/api/v1/inventory/supplier-qualifications` | 创建/更新供应商资质 | `inventory.supplier.qualification.manage` | `inventory.supplier_qualification.create` / `inventory.supplier_qualification.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplierReconciliation` | `POST` | `/api/v1/inventory/supplier-reconciliations` | 创建供应商对账单 | `inventory.supplier.reconcile` | `inventory.supplier_reconciliation.create` / `inventory.supplier_reconciliation.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplierSettlementMethod` | `POST` | `/api/v1/inventory/supplier-settlement-methods` | 创建/更新供应商结算档 | `inventory.supplier.settlement.manage` | `inventory.supplier_settlement_method.create` / `inventory.supplier_settlement_method.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | 创建供应合同(三模式) | `inventory.supply_contract.manage` | `supply_contract.created` / `supply_contract.create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createTraceHandover` | `POST` | `/api/v1/inventory/trace-handovers` | 创建扫码交接任务(五段链) | `inventory.handover.scan` | `inventory.handover_created` / `inventory.handover_create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | 创建typed库存流水 | `inventory.stock.move` | `inventory.stock_movement.created` / `inventory.stock_movement.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | 创建结构化库房(六类) | `inventory.warehouse.manage` | `inventory.warehouse.created` / `inventory.warehouse.create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | 受理存酒争议 | `bar.saved_bottle.manage` | `bar.saved_bottle.dispute` / `bar.saved_bottle.dispute_failed` | tablet, web | operation 级补充 |
| INV-库存存酒 | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | 期间出库成本汇总（分红成本侧取数点） | `inventory.cost.view` | `inventory.cost_summary.viewed` / `inventory.cost_summary.view_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `getInventoryStocktake` | `GET` | `/api/v1/inventory/stocktakes/{id}` | 查询库存盘点单详情（含差异明细） | `inventory.stocktake.view` | `inventory.stocktake.detail_viewed` / `inventory.stocktake.detail_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | 查询溯源码责任链（按 id 或 trace_code） | `inventory.trace_code.view` | `inventory.trace_code.viewed` / `inventory.trace_code.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listHighValueReviews` | `GET` | `/api/v1/inventory/high-value-reviews` | 查询库存域高价值复核单(报损/盘点差异/存酒作废) | `inventory.high_value.review` | `inventory.high_value_review.viewed` / `inventory.high_value_review.view_failed` | web, tablet, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | 查询库存成本流水（移动加权） | `inventory.cost.view` | `inventory.cost_ledger.viewed` / `inventory.cost_ledger.view_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | 查询库存明细 | `inventory.stock.view` | `inventory.item.viewed` / `inventory.item.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventoryQuarantine` | `GET` | `/api/v1/inventory/quarantine` | 查看仓库隔离库存 | `inventory.stock.view` | `inventory.quarantine.viewed` / `inventory.quarantine.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventoryRiskAlerts` | `GET` | `/api/v1/inventory/risk-alerts` | 查询库存风险预警列表 | `inventory.risk_alert.view` | `inventory.risk_alert.list` / `inventory.risk_alert.list_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryStockBatches` | `GET` | `/api/v1/inventory/stock-batches` | 查询库存批次列表 | `inventory.stock.view` | `inventory.stock_batch.list` / `inventory.stock_batch.list_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryStocktakes` | `GET` | `/api/v1/inventory/stocktakes` | 查询库存盘点单列表 | `inventory.stocktake.view` | `inventory.stocktake.viewed` / `inventory.stocktake.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | 查询供应商 | `inventory.supplier.view` | `inventory.supplier_viewed` / `inventory.supplier_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | 查询溯源码主数据列表 | `inventory.trace_code.view` | `inventory.trace_code.listed` / `inventory.trace_code.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | 查询采购申请 | `inventory.purchase_request.view` | `inventory.purchase_request_viewed` / `inventory.purchase_request_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listQuarantineDispositions` | `GET` | `/api/v1/inventory/quarantine/{id}/dispositions` | 查看隔离后续处置记录 | `inventory.stock.view` | `inventory.quarantine.dispositions_viewed` / `inventory.quarantine.dispositions_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listQuarantineWarehouses` | `GET` | `/api/v1/inventory/quarantine/warehouses` | 选择可查看的隔离库存仓库 | `inventory.stock.view` | `inventory.quarantine.warehouses_viewed` / `inventory.quarantine.warehouses_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | 查询存酒 | `bar.saved_bottle.manage` | `bar.saved_bottle.viewed` / `bar.saved_bottle.view_failed` | web, tablet, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | 查询库存余额 | `inventory.stock.view` | `inventory.stock_balance.viewed` / `inventory.stock_balance.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | 查询库存流水 | `inventory.stock.view` | `inventory.stock_movement.viewed` / `inventory.stock_movement.view_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `listSupplierQualifications` | `GET` | `/api/v1/inventory/supplier-qualifications` | 查询供应商资质列表 | `inventory.supplier.qualification.manage` | `inventory.supplier_qualification.list` / `inventory.supplier_qualification.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplierReconciliations` | `GET` | `/api/v1/inventory/supplier-reconciliations` | 查询供应商对账列表 | `inventory.supplier.reconcile` | `inventory.supplier_reconciliation.list` / `inventory.supplier_reconciliation.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplierSettlementMethods` | `GET` | `/api/v1/inventory/supplier-settlement-methods` | 查询供应商结算档列表 | `inventory.supplier.settlement.manage` | `inventory.supplier_settlement_method.list` / `inventory.supplier_settlement_method.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | 查询供应合同列表 | `inventory.supply_contract.manage` | `inventory.supply_contract.list` / `inventory.supply_contract.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | 查询库房列表 | `inventory.warehouse.manage` | `inventory.warehouse.list` / `inventory.warehouse.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | 采购单下单(approved→ordered) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | 出库打印溯源码（registered→printed） | `inventory.trace_code.print` | `inventory.trace_code.printed` / `inventory.trace_code.print_failed` | web | operation 级补充 |
| INV-库存存酒 | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | 采购单入库确认(arrived→received,生成入库流水) | `inventory.purchase.approve` | `inventory.received` / `inventory.received_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `recordInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances` | 录入库存盘点差异行 | `inventory.stocktake.variance_record` | `inventory.stocktake.variance_recorded` / `inventory.stocktake.variance_record_failed` | web | operation 级补充 |
| INV-库存存酒 | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | 贴码登记溯源码（入库） | `inventory.trace_code.register` | `inventory.trace_code.registered` / `inventory.trace_code.register_failed` | web | operation 级补充 |
| INV-库存存酒 | `reportRobotSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/robot-report` | 机器人配送段上报(被动留痕,受第三方开关门控) | `inventory.handover.scan` | `inventory.handover_robot_reported` / `inventory.handover_robot_report_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `requestQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/{id}/dispositions` | 申请隔离品复售或报损 | `inventory.stock.adjust` | `inventory.quarantine.disposition_requested` / `inventory.quarantine.disposition_request_failed` | web | operation 级补充 |
| INV-库存存酒 | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | 借调归还 | `inventory.transfer_order.manage` | `inventory.transfer_order_returned` / `inventory.transfer_order_return_failed` | web | operation 级补充 |
| INV-库存存酒 | `reviewHighValue` | `POST` | `/api/v1/inventory/high-value-reviews/{id}/review` | 主管复核高价值动作(approve生效/reject回滚) | `inventory.high_value.review` | `inventory.high_value_review_decided` / `inventory.high_value_review_decide_failed` | web, tablet, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances/{vid}/review` | 复核库存盘点差异（approved/rejected） | `inventory.stocktake.variance_review` | `inventory.stocktake.variance_reviewed` / `inventory.stocktake.variance_review_failed` | web | operation 级补充 |
| INV-库存存酒 | `reviewTraceHandoverException` | `POST` | `/api/v1/inventory/trace-handovers/exceptions/{id}/review` | 交接异常复核(缺口/重复/差异) | `inventory.handover.review` | `inventory.handover_exception_resolved` / `inventory.handover_exception_review_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `scanInventoryRiskAlerts` | `POST` | `/api/v1/inventory/risk-alerts/scan` | 触发库存风险扫描 | `inventory.risk.scan` | `inventory.risk.scan` / `inventory.risk.scan_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | 物品追踪码扫码交接 | `inventory.handover.scan` | `operation_handover.created` / `operation_handover.failed` | staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `scanTraceSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/scan-segment` | 员工扫码交接分段(主动留痕) | `inventory.handover.scan` | `inventory.handover_segment_scanned` / `inventory.handover_segment_scan_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | 调拨库存流水 | `inventory.stock.move` | `inventory.stock_transfer.created` / `inventory.stock_transfer.created_failed` | web | operation 级补充 |
| OPEN-开放平台 | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | 回调处理结果查询 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | 第三方或供应商回调 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPS-生产前置 | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | 执行生产前置准入检查 | `ops.production_readiness.check` | `production_readiness.checked` / `production_readiness.check_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | 查询生产前置责任状态 | `ops.production_readiness.view` | `production_readiness.viewed` / `production_readiness.view_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | 更新生产前置责任状态 | `ops.production_readiness.manage` | `production_readiness.updated` / `production_readiness.update_failed` | web, mobile_admin | 已在任务映射正文出现 |
| P0-待归类 | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | 主管验收工单 | `facility.work_order.manage` | `facility.work_order_accepted` / `facility.work_order_accept_failed` | web | operation 级补充 |
| P0-待归类 | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | 接单(仅指定履约人) | `service.order.perform` | `service.order.accepted` / `service.order.accept_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `acceptSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/accept` | 物业验收完工（平台侧） | `facility.site_entry.accept` | `facility.site_entry_report_accepted` / `facility.site_entry.accept_failed` | web | operation 级补充 |
| P0-待归类 | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | 生效(固化快照+绑铺位) | `contract.lifecycle.activate` | `contract.activated` / `contract.activate_failed` | web | operation 级补充 |
| P0-待归类 | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | 追加合同附件(电子签章扫描件归档) | `contract.draft.manage` | `contract.attachments_added` / `contract.attachments_add_failed` | web | operation 级补充 |
| P0-待归类 | `addMerchantServiceWorkOrderAction` | `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | Append merchant service work order action | `merchant.service_work_order.process` | `merchant_service.addMerchantServiceWorkOrderAction.succeeded` / `merchant_service.addMerchantServiceWorkOrderAction.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `advanceContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/advance` | 逐相推进退租结算 | `contract.settlement.manage` | `contract.settlement_advanced` / `contract.settlement_advance_failed` | web | operation 级补充 |
| P0-待归类 | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | 意向阶段推进(5 阶段) | `leasing.intent.manage` | `leasing.intent.advanced` / `leasing.intent.advance_failed` | web | operation 级补充 |
| P0-待归类 | `approveContractChange` | `POST` | `/api/v1/contracts/changes/{change_id}/approve` | 变更审批(走同一并行链,全过即应用+新版本快照) | `contract.approve.business` | `contract.change_approved` / `contract.change_approve_failed` | web | operation 级补充 |
| P0-待归类 | `approveContractStage` | `POST` | `/api/v1/contracts/{contract_id}/approve` | 审批当前阶段并行槽(会签) | `contract.approve.business` | `contract.approved_slot` / `contract.approve_failed` | web | operation 级补充 |
| P0-待归类 | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | 工单派单 | `facility.work_order.manage` | `facility.work_order_assigned` / `facility.work_order_assign_failed` | web | operation 级补充 |
| P0-待归类 | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | 招商线索派单(主管手动指派) | `leasing.lead.manage` | `leasing.lead.assigned` / `leasing.lead.assign_failed` | web | operation 级补充 |
| P0-待归类 | `assignMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | Assign merchant service work order | `merchant.service_work_order.assign` | `merchant_service.assignMerchantServiceWorkOrder.succeeded` / `merchant_service.assignMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `bindUserToPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/bind-user` | 账号绑定外部协作机构 | `partner.org.manage` | `partner.org.user_bound` / `partner.org.user_bind_failed` | web | operation 级补充 |
| P0-待归类 | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | 品牌拉黑/解黑(高危需审批) | `leasing.brand.blacklist` | `leasing.brand.blacklist_changed` / `leasing.brand.blacklist_change_failed` | web | operation 级补充 |
| P0-待归类 | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | 取消服务单(履约前;留原因) | `service.order.create` | `service.order.cancelled` / `service.order.cancel_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | 变更设备状态/上报告警 | `facility.equipment.manage` | `facility.equipment_status_changed` / `facility.equipment_status_change_failed` | web | operation 级补充 |
| P0-待归类 | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | 流转开票状态 | `settlement.invoice.manage` | `settlement.invoice.status_changed` / `settlement.invoice.status_change_failed` | web | operation 级补充 |
| P0-待归类 | `closeMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | Close merchant service work order | `merchant.service_work_order.close` | `merchant_service.closeMerchantServiceWorkOrder.succeeded` / `merchant_service.closeMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `completeContractSettlement` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/complete` | 结清退租结算→已结清+终止合同 | `contract.settlement.manage` | `contract.settlement_completed` / `contract.settlement_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeDealerDeliveryPhysical` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/physical-completion` | 确认差异实物实际处理 | `dealer.order.ship` | `dealer.delivery.physical_completed` / `dealer.delivery.physical_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | 完成工单处理 | `facility.work_order.manage` | `facility.work_order_completed` / `facility.work_order_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | 完成履约(公司代收:管理费归公司,应得月结进薪资) | `service.order.create` | `service.order.completed` / `service.order.complete_failed` | staff_work_phone, web | operation 级补充 |
| P0-待归类 | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | 确认超收用途 | `dealer.order.payment.verify` | `dealer.order.overpayment_use_confirmed` / `dealer.order.overpayment_use_failed` | web | operation 级补充 |
| P0-待归类 | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | 确认退回运费承担方 | `dealer.return.manage` | `dealer.return.freight_confirmed` / `dealer.return.freight_confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | 主管确认巡检任务关闭 | `facility.inspection.manage` | `facility.inspection_task_closed` / `facility.inspection_task_close_failed` | web | operation 级补充 |
| P0-待归类 | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | 确认商户账单 | `merchant.billing.confirm` | `merchant.billing.confirm` / `merchant.billing.confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmTicketPointsBillUnchanged` | `POST` | `/api/v1/loyalty/ticket-points-billing/objections/{id}/confirm-unchanged` | 申请复核月结原金额无误 | `ticket.points.billing.manage` | `ticket.points.billing.objection.resolution.submitted` / `ticket.points.billing.objection.resolution.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `createComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items` | 创建合规到期项 | `compliance.expiry_item.manage` | `compliance.expiry_item.create` / `compliance.expiry_item.create_failed` | web | operation 级补充 |
| P0-待归类 | `createContractDraft` | `POST` | `/api/v1/contracts` | 起草合同(草拟) | `contract.draft.manage` | `contract.drafted` / `contract.draft_failed` | web | operation 级补充 |
| P0-待归类 | `createContractKeyDate` | `POST` | `/api/v1/compliance/contract-key-dates` | 创建合同关键日期 | `compliance.contract_key_date.manage` | `compliance.contract_key_date.create` / `compliance.contract_key_date.create_failed` | web | operation 级补充 |
| P0-待归类 | `createDealer` | `POST` | `/api/v1/dealers` | 经销商建档（待提交） | `dealer.manage` | `dealer.created` / `dealer.create_failed` | web | operation 级补充 |
| P0-待归类 | `createDealerContract` | `POST` | `/api/v1/dealers/{id}/contracts` | 登记经销合同 | `dealer.manage` | `dealer.contract_created` / `dealer.contract_create_failed` | web | operation 级补充 |
| P0-待归类 | `createDealerInquiry` | `POST` | `/api/v1/dealers/{id}/inquiries` | 登记询价（未定价商品） | `dealer.inquiry.manage` | `dealer.inquiry.created` / `dealer.inquiry.create_failed` | web | operation 级补充 |
| P0-待归类 | `createDealerOrderDraft` | `POST` | `/api/v1/dealers/{id}/orders` | 经销订单建单（草稿） | `dealer.order.manage` | `dealer.order.drafted` / `dealer.order.draft_failed` | web | operation 级补充 |
| P0-待归类 | `createDealerPortalAccount` | `POST` | `/api/v1/dealers/{id}/portal-accounts` | 开通经销商门户账号 | `dealer.portal_account.manage` | `dealer.portal_account.created` / `dealer.portal_account.create_failed` | web | operation 级补充 |
| P0-待归类 | `createDealerPriceCatalogDraft` | `POST` | `/api/v1/dealers/contracts/{id}/price-catalogs` | 新建价格目录草稿版本 | `dealer.price_catalog.edit` | `dealer.price_catalog.drafted` / `dealer.price_catalog.draft_failed` | web | operation 级补充 |
| P0-待归类 | `createEnergyMeter` | `POST` | `/api/v1/energy/meters` | 创建能源表计 | `energy.meter.manage` | `energy.meter_saved` / `energy.meter_save_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityEquipment` | `POST` | `/api/v1/facility/equipment` | 创建设备台账 | `facility.equipment.manage` | `facility.equipment_created` / `facility.equipment_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | 创建巡检任务 | `facility.inspection.manage` | `facility.inspection_task_created` / `facility.inspection_task_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | 创建巡检模板 | `facility.inspection.manage` | `facility.inspection_template_created` / `facility.inspection_template_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | 创建维保/物业工单 | `facility.work_order.manage` | `facility.work_order_created` / `facility.work_order_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | 创建 P1/P2 商场平面图 | `floor_plan.manage` | `floor_plan.created` / `floor_plan.create_failed` | web | operation 级补充 |
| P0-待归类 | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | 创建 SVG 或标准 JSON 导入任务 | `floor_plan.import.manage` | `floor_plan.import_created` / `floor_plan.import_create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | 新建品牌(4 类) | `leasing.brand.manage` | `leasing.brand.created` / `leasing.brand.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | 新建跟进记录(回写 last_follow_up_at) | `leasing.lead.manage` | `leasing.follow_up.created` / `leasing.follow_up.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | 线索转意向(stage 起始 initial_contact) | `leasing.intent.manage` | `leasing.intent.created` / `leasing.intent.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | 新建招商线索 | `leasing.lead.manage` | `leasing.lead.created` / `leasing.lead.create_failed` | web | operation 级补充 |
| P0-待归类 | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | 创建商户账单 | `merchant.billing.manage` | `merchant.billing.create` / `merchant.billing.create_failed` | web | operation 级补充 |
| P0-待归类 | `createMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders` | Create merchant service work order | `merchant.service_work_order.create` | `merchant_service.createMerchantServiceWorkOrder.succeeded` / `merchant_service.createMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | 录入/修改外部协作机构 | `partner.org.manage` | `partner_org.updated` / `partner_org.update_failed` | web | operation 级补充 |
| P0-待归类 | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | 自助开通下属账号 | `partner.subordinate.manage` | `partner_subordinate.created` / `partner_subordinate.create_failed` | web | operation 级补充 |
| P0-待归类 | `createServiceOrder` | `POST` | `/api/v1/services/orders` | 代客下单(指定履约人) | `service.order.create` | `service.order.created` / `service.order.create_failed` | staff_work_phone, web, tablet, app | operation 级补充 |
| P0-待归类 | `createSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports` | 外包创建进场报备单（施工/装修进场前报备） | `facility.site_entry.create_assigned` | `facility.site_entry.create_assigned` / `facility.site_entry.create_failed` | web | operation 级补充 |
| P0-待归类 | `deactivateDealer` | `POST` | `/api/v1/dealers/{id}/deactivate` | 停用经销商 | `dealer.manage` | `dealer.deactivated` / `dealer.deactivate_failed` | web | operation 级补充 |
| P0-待归类 | `dealerPortalCreateInquiry` | `POST` | `/api/v1/dealer-portal/inquiries` | 门户：未定价商品询价 | `dealer_portal.inquiry.manage` | `dealer.inquiry.created` / `dealer.inquiry.create_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | 门户：建单（草稿） | `dealer_portal.order.manage` | `dealer.order.drafted` / `dealer.order.draft_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | 门户：库存外显 | `dealer_portal.stock.view` | `dealer.external_stock.viewed` / `dealer.external_stock.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | 门户：订单详情 | `dealer_portal.order.manage` | `dealer.order.viewed` / `dealer.order.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | 门户：本企业经销合同 | `dealer_portal.profile.view` | `dealer.contract.viewed` / `dealer.contract.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | 门户：本企业询价单 | `dealer_portal.inquiry.manage` | `dealer.inquiry.viewed` / `dealer.inquiry.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | 门户：订单明细 | `dealer_portal.order.manage` | `dealer.order.viewed` / `dealer.order.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | 门户：本企业经销订单 | `dealer_portal.order.manage` | `dealer.order.viewed` / `dealer.order.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | 门户：付款记录 | `dealer_portal.order.pay` | `dealer.order.viewed` / `dealer.order.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | 门户：退货单 | `dealer_portal.return.request` | `dealer.return.viewed` / `dealer.return.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | 门户：本企业资料与准入状态 | `dealer_portal.profile.view` | `dealer.portal.viewed` / `dealer.portal.access_rejected` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalRegisterPayment` | `POST` | `/api/v1/dealer-portal/orders/{id}/payments` | 门户：登记付款 | `dealer_portal.order.pay` | `dealer.order.payment_registered` / `dealer.order.payment_register_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | 门户：申请退货 | `dealer_portal.return.request` | `dealer.return.requested` / `dealer.return.request_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSignOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/sign` | 门户：确认签收 | `dealer_portal.order.manage` | `dealer.delivery.received` / `dealer.delivery.receive_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSubmitOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/submit` | 门户：提交订单 | `dealer_portal.order.manage` | `dealer.order.submitted` / `dealer.order.submit_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | 门户：提交注册资料送审 | `dealer_portal.profile.submit` | `dealer.submitted` / `dealer.submit_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `decideDealerInquiryQuote` | `POST` | `/api/v1/dealers/inquiries/{id}/decision` | 审批报价 | `dealer.inquiry.approve` | `dealer.inquiry.decided` / `dealer.inquiry.decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | 取消审批 | `dealer.order.review` | `dealer.order.cancel_decided` / `dealer.order.cancel_decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | 审批价格目录 | `dealer.price_catalog.approve` | `dealer.price_catalog.decided` / `dealer.price_catalog.decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | 审批经销退货 | `dealer.return.review` | `dealer.return.decided` / `dealer.return.decide_failed` | web | operation 级补充 |
| P0-待归类 | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | 停用下属账号 | `partner.subordinate.manage` | `partner_subordinate.disabled` / `partner_subordinate.disable_failed` | web | operation 级补充 |
| P0-待归类 | `executeDealerOrderRefund` | `POST` | `/api/v1/dealers/refunds/{id}/execute` | 将已批准经销退款交统一财务办理 | `dealer.order.refund.execute` | `dealer.order.refund_executed` / `dealer.order.refund_execute_failed` | web | operation 级补充 |
| P0-待归类 | `executeWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/execute` | 签名确认并执行精确清理 | `wecom.retention.manage` | `wecom.cleanup_executed` / `wecom.cleanup_execute_failed` | web | operation 级补充 |
| P0-待归类 | `expireDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/expire` | 报价置为已失效 | `dealer.inquiry.quote` | `dealer.inquiry.expired` / `dealer.inquiry.expire_failed` | web | operation 级补充 |
| P0-待归类 | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | 导出操作留痕（敏感） | `audit.operation_log.export` | `audit.operation_log.exported` / `audit.operation_log.export_failed` | web | operation 级补充 |
| P0-待归类 | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | 某期全费目挂账(租金+物业费+能源费) | `finance.ar_ap.manage` | `contract.fee_billed` / `contract.period_bill_failed` | web | operation 级补充 |
| P0-待归类 | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | 生成某期合同租金应收(base+递增−免租) | `finance.ar_ap.manage` | `contract.rent_billed` / `contract.rent_bill_failed` | web | operation 级补充 |
| P0-待归类 | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | 按结算单生成开票清单 | `settlement.invoice.manage` | `settlement.invoice.generated` / `settlement.invoice.generate_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | 查看经销商本次准入材料 | `dealer.view` | `dealer.admission_viewed` / `dealer.admission_view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerDelivery` | `GET` | `/api/v1/dealers/orders/{id}/delivery` | 查看发运、签收及差异处理 | `dealer.order.view` | `dealer.delivery.viewed` / `dealer.delivery.view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | 经销订单详情 | `dealer.order.view` | `dealer.order.viewed` / `dealer.order.view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerPortalDelivery` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery` | 查看发运、签收及差异处理 | `dealer_portal.order.manage` | `dealer.delivery.viewed` / `dealer.delivery.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | 经销退货单详情 | `dealer.return.view` | `dealer.return.viewed` / `dealer.return.view_failed` | web | operation 级补充 |
| P0-待归类 | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | 查询总分表差异 | `energy.reading.manage` | `energy.meter_difference.viewed` / `energy.meter_difference.view_failed` | web | operation 级补充 |
| P0-待归类 | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | 巡检任务详情 | `facility.inspection.manage` | `facility.inspection_task.viewed` / `facility.inspection_task.view_failed` | web | operation 级补充 |
| P0-待归类 | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | 工单详情 | `facility.work_order.manage` | `facility.work_order.viewed` / `facility.work_order.view_failed` | web | operation 级补充 |
| P0-待归类 | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | 查询导入候选和错误 | `floor_plan.import.view` | `floor_plan.import_candidate_viewed` / `floor_plan.import_candidate_view_failed` | web | operation 级补充 |
| P0-待归类 | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | 查看某一条招商线索的完整联系人手机号 | `leasing.lead.manage` | `leasing.lead.contact_phone_viewed` / `leasing.lead.contact_phone_view_failed` | web | operation 级补充 |
| P0-待归类 | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | 公司统一收款码(下单出示用) | `service.order.create` | `service.pay_config.viewed` / `service.pay_config.view_failed` | staff_private_phone, web, tablet | operation 级补充 |
| P0-待归类 | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | 查看单个进场报备单 | `facility.site_entry.view_assigned` | `facility.site_entry.viewed` / `facility.site_entry.view_failed` | web | operation 级补充 |
| P0-待归类 | `getWecomCleanupPreview` | `GET` | `/api/v1/compliance/wecom/cleanup-previews/{id}` | 查看本人清理预览 | `wecom.retention.manage` | `wecom.cleanup_viewed` / `wecom.cleanup_view_failed` | web | operation 级补充 |
| P0-待归类 | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | 退货验收 | `dealer.return.inspect` | `dealer.return.inspected` / `dealer.return.inspect_failed` | web | operation 级补充 |
| P0-待归类 | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | 登记发票开具 | `dealer.invoice.manage` | `dealer.invoice.issued` / `dealer.invoice.issue_failed` | web | operation 级补充 |
| P0-待归类 | `issueWecomCleanupChallenge` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/challenge` | 申请本次清理的U盘动作挑战 | `wecom.retention.manage` | `wecom.cleanup_challenged` / `wecom.cleanup_challenge_failed` | web | operation 级补充 |
| P0-待归类 | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | 查询合规到期项列表 | `compliance.expiry_item.view` | `compliance.expiry_item.list` / `compliance.expiry_item.list_failed` | web | operation 级补充 |
| P0-待归类 | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | 查询合规提醒列表 | `compliance.reminder.view` | `compliance.reminder.list` / `compliance.reminder.list_failed` | web | operation 级补充 |
| P0-待归类 | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | 变更子合同追溯清单 | `contract.change.manage` | `contract.amendments_viewed` / `contract.amendments_view_failed` | web | operation 级补充 |
| P0-待归类 | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | 查审批链(生效链：项目覆盖优先) | `contract.approval.flow.manage` | `contract.approval_flow_viewed` / `contract.approval_flow_view_failed` | web | operation 级补充 |
| P0-待归类 | `listContractApprovals` | `GET` | `/api/v1/contracts/{contract_id}/approvals` | 合同审批留痕(按阶段) | `contract.draft.manage` | `contract.approvals_viewed` / `contract.approvals_view_failed` | web | operation 级补充 |
| P0-待归类 | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | 合同变更清单 | `contract.change.manage` | `contract.changes_viewed` / `contract.changes_view_failed` | web | operation 级补充 |
| P0-待归类 | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | 查询合同关键日期列表 | `compliance.contract_key_date.view` | `compliance.contract_key_date.list` / `compliance.contract_key_date.list_failed` | web | operation 级补充 |
| P0-待归类 | `listContracts` | `GET` | `/api/v1/contracts` | 合同清单 | `contract.draft.manage` | `contract.viewed` / `contract.view_failed` | web | operation 级补充 |
| P0-待归类 | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | 退租结算清单 | `contract.settlement.manage` | `contract.settlements_viewed` / `contract.settlements_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | 经销合同清单 | `dealer.view` | `dealer.contract_viewed` / `dealer.contract_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | 经销库存外显 | `dealer.view` | `dealer.external_stock.viewed` / `dealer.external_stock.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | 询价清单 | `dealer.view` | `dealer.inquiry_viewed` / `dealer.inquiry_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | 经销订单电子发票列表 | `dealer.return.view` | `dealer.invoice.viewed` / `dealer.invoice.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | 经销订单明细 | `dealer.order.view` | `dealer.order.viewed` / `dealer.order.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | 经销订单到账记录 | `dealer.order.view` | `dealer.order.viewed` / `dealer.order.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | 经销订单退款记录 | `dealer.order.view` | `dealer.order.viewed` / `dealer.order.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | 经销订单清单 | `dealer.order.view` | `dealer.order.viewed` / `dealer.order.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | 经销超收记录 | `dealer.order.view` | `dealer.order.overpayment_viewed` / `dealer.order.overpayment_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | 经销商门户账号列表 | `dealer.portal_account.manage` | `dealer.portal_account.viewed` / `dealer.portal_account.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | 价格目录明细 | `dealer.view` | `dealer.price_catalog_line_viewed` / `dealer.price_catalog_line_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | 合同价格目录版本清单 | `dealer.view` | `dealer.price_catalog_viewed` / `dealer.price_catalog_view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | 经销退货单明细 | `dealer.return.view` | `dealer.return.viewed` / `dealer.return.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | 经销订单退货单列表 | `dealer.return.view` | `dealer.return.viewed` / `dealer.return.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealers` | `GET` | `/api/v1/dealers` | 经销商清单 | `dealer.view` | `dealer.viewed` / `dealer.view_failed` | web | operation 级补充 |
| P0-待归类 | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | 当前终端的已关闭开关行（前端菜单 gating 数据源） | `not_applicable` | `terminal_switch.effective_viewed` / `terminal_switch.effective_view_failed` | web, miniapp, app, kiosk, tablet, staff_work_phone, staff_private_phone, merchant_portal, merchant_app, mobile_admin | operation 级补充 |
| P0-待归类 | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | 表计列表 | `energy.meter.manage` | `energy.meter.viewed` / `energy.meter.view_failed` | web | operation 级补充 |
| P0-待归类 | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | 抄表读数列表 | `energy.reading.manage` | `energy.reading.viewed` / `energy.reading.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | 设备台账列表 | `facility.equipment.manage` | `facility.equipment.viewed` / `facility.equipment.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | 巡检记录列表 | `facility.inspection.manage` | `facility.inspection_record.viewed` / `facility.inspection_record.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | 巡检任务列表 | `facility.inspection.manage` | `facility.inspection_task.viewed` / `facility.inspection_task.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | 巡检模板列表 | `facility.inspection.manage` | `facility.inspection_template.viewed` / `facility.inspection_template.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | 查询维保到期设备 | `facility.equipment.manage` | `facility.equipment.maintenance_due_viewed` / `facility.equipment.maintenance_due_view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | 逾期工单列表 | `facility.work_order.manage` | `facility.work_order.overdue_viewed` / `facility.work_order.overdue_view_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | 工单列表 | `facility.work_order.manage` | `facility.work_order.viewed` / `facility.work_order.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | 查询平面图导入任务 | `floor_plan.import.view` | `floor_plan.import_viewed` / `floor_plan.import_view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | 查询 P1/P2 商场平面图 | `floor_plan.view` | `floor_plan.viewed` / `floor_plan.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | 查询平面图正式区域 | `floor_plan.view` | `floor_plan.shape_viewed` / `floor_plan.shape_view_failed` | web | operation 级补充 |
| P0-待归类 | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | 招商品牌/客户库清单 | `leasing.brand.manage` | `leasing.brand.viewed` / `leasing.brand.view_failed` | web | operation 级补充 |
| P0-待归类 | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | 跟进记录(按 ref 查) | `leasing.lead.manage` | `leasing.follow_up.viewed` / `leasing.follow_up.view_failed` | web | operation 级补充 |
| P0-待归类 | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | 招商意向清单 | `leasing.intent.manage` | `leasing.intent.viewed` / `leasing.intent.view_failed` | web | operation 级补充 |
| P0-待归类 | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | 招商线索清单 | `leasing.lead.manage` | `leasing.lead.viewed` / `leasing.lead.view_failed` | web | operation 级补充 |
| P0-待归类 | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | 待跟进线索(3 天未跟进) | `leasing.lead.manage` | `leasing.lead.viewed` / `leasing.lead.view_failed` | web | operation 级补充 |
| P0-待归类 | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | List merchant bills | `merchant.billing.view` | `merchant_billing.bill.viewed` / `merchant_billing.bill.view_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `listMerchantPortalContracts` | `GET` | `/api/v1/merchant-portal/contracts` | 商户专区·我的合同(只读关键信息) | `contract.view` | `merchant_portal.contract.viewed` / `merchant_portal.contract.view_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `listMerchantPortalSettlements` | `GET` | `/api/v1/merchant-portal/settlements` | 商户专区·我的对账(结算单只读) | `merchant.portal.settlement.view` | `merchant_portal.settlement.viewed` / `merchant_portal.settlement.view_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | List merchant service work orders | `merchant.service_work_order.view` | `merchant_service.listMerchantServiceWorkOrders.succeeded` / `merchant_service.listMerchantServiceWorkOrders.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | 我的服务任务(履约人手机) | `service.order.perform` | `service.order.mine_viewed` / `service.order.mine_view_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | 外部协作机构列表 | `partner.org.view` | `partner_org.viewed` / `partner_org.view_failed` | web | operation 级补充 |
| P0-待归类 | `listPartnerSubordinates` | `GET` | `/api/v1/partner/subordinates` | 本机构下属账号列表 | `partner.subordinate.view` | `partner_subordinate.viewed` / `partner_subordinate.view_failed` | web | operation 级补充 |
| P0-待归类 | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | 服务目录(上架项,下单用) | `service.order.create` | `service.catalog.viewed` / `service.catalog.view_failed` | staff_private_phone, web, tablet, app | operation 级补充 |
| P0-待归类 | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | 服务目录全量(含下架,后台维护) | `service.catalog.manage` | `service.catalog.viewed` / `service.catalog.view_failed` | web | operation 级补充 |
| P0-待归类 | `listServiceOrders` | `GET` | `/api/v1/services/orders` | 服务单列表(后台) | `service.catalog.manage` | `service.order.listed` / `service.order.list_failed` | web | operation 级补充 |
| P0-待归类 | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | 开票清单列表 | `settlement.invoice.view` | `settlement.invoice.list` / `settlement.invoice.list_failed` | web | operation 级补充 |
| P0-待归类 | `listSiteEntryHistory` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/history` | 查看各轮施工材料与验收记录 | `facility.site_entry.view_assigned` | `facility.site_entry.history_viewed` / `facility.site_entry.history_view_failed` | web | operation 级补充 |
| P0-待归类 | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | 查看进场报备/验收单列表（外包只见本机构，平台全量） | `facility.site_entry.view_assigned` | `facility.site_entry.viewed` / `facility.site_entry.view_failed` | web | operation 级补充 |
| P0-待归类 | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | 查到期着色分类 | `contract.space.color.manage` | `contract.color_rules_viewed` / `contract.color_rules_view_failed` | web | operation 级补充 |
| P0-待归类 | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | 铺位平面图着色数据源(可租/已租/即将到期) | `contract.draft.manage` | `contract.space_rental_viewed` / `contract.space_rental_view_failed` | web | operation 级补充 |
| P0-待归类 | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | 机构管理员查看本机构下属工作完成情况 | `partner.subordinate.view` | `partner_subordinate_completion.viewed` / `partner_subordinate_completion.view_failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsBillingRules` | `GET` | `/api/v1/loyalty/ticket-points-billing/rules` | 查看全国积分账期规则 | `ticket.points.billing.view` | `ticket.points.billing.rules.viewed` / `ticket.points.billing.rules.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsBillItems` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/items` | 查看实际积分发行费明细 | `ticket.points.billing.view` | `ticket.points.billing.items.viewed` / `ticket.points.billing.items.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsBillObjections` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | 查看月结异议及复核记录 | `ticket.points.billing.view` | `ticket.points.billing.objections.viewed` / `ticket.points.billing.objections.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsBills` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills` | 查看合同销售方积分发行费月结账单 | `ticket.points.billing.view` | `ticket.points.billing.bills.viewed` / `ticket.points.billing.bills.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsBillScheduleAdjustments` | `GET` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/schedule-adjustments` | 查看实际门户故障导致的未付期限调整 | `ticket.points.billing.view` | `ticket.points.billing.schedule.adjustments.viewed` / `ticket.points.billing.schedule.adjustments.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsCalendars` | `GET` | `/api/v1/loyalty/ticket-points-billing/calendars` | 查看工作日及银行工作日日历审批 | `ticket.points.billing.view` | `ticket.points.billing.calendars.viewed` / `ticket.points.billing.calendars.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketPointsOutages` | `GET` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | 查看实际门户中断记录 | `ticket.points.billing.view` | `ticket.points.billing.outages.viewed` / `ticket.points.billing.outages.viewed.failed` | web | operation 级补充 |
| P0-待归类 | `listTicketRewardConfigs` | `GET` | `/api/v1/loyalty/ticket-reward-configs` | 查看门票奖励版本 | `ticket.reward.config.view` | `ticket.reward.config.listed` / `ticket.reward.config.listed.failed` | web | operation 级补充 |
| P0-待归类 | `listWecomCleanupPreviews` | `GET` | `/api/v1/compliance/wecom/cleanup-previews` | 查询永久清理记录 | `wecom.retention.manage` | `wecom.cleanup_listed` / `wecom.cleanup_list_failed` | web | operation 级补充 |
| P0-待归类 | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | 查看企微永久保存规则 | `wecom.retention.manage` | `wecom.retention_policy_viewed` / `wecom.retention_policy_view_failed` | web | operation 级补充 |
| P0-待归类 | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | 查询企微会话违规命中列表 | `wecom.risk_hit.view` | `wecom.risk_hit.list` / `wecom.risk_hit.list_failed` | web | operation 级补充 |
| P0-待归类 | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | 我的工单(员工自有手机) | `facility.work_order.view_assigned` | `facility.work_order_assigned_list.viewed` / `facility.work_order_assigned_list.view_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | 批量标记逾期工单 | `facility.work_order.manage` | `facility.work_order_overdue_marked` / `facility.work_order_overdue_mark_failed` | web | operation 级补充 |
| P0-待归类 | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | 确认收款到公司账户(服务员现场) | `service.order.create` | `service.order.paid` / `service.order.pay_confirm_failed` | staff_work_phone, web, tablet | operation 级补充 |
| P0-待归类 | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | 我的收款码 | `service.order.perform` | `service.payment_code.viewed` / `service.payment_code.view_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | 我的服务统计(当月次数+参考收入) | `service.order.perform` | `service.order.stats_viewed` / `service.order.stats_view_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `openDealerOrderRefundApproval` | `POST` | `/api/v1/dealers/refunds/{id}/approval` | 开启经销退款审批 | `dealer.order.payment.verify` | `dealer.order.refund_requested` / `dealer.order.refund_request_failed` | web | operation 级补充 |
| P0-待归类 | `previewWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews` | 创建逐条消息清理预览 | `wecom.retention.manage` | `wecom.cleanup_previewed` / `wecom.cleanup_preview_failed` | web | operation 级补充 |
| P0-待归类 | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | 发布平面图导入候选 | `floor_plan.import.manage` | `floor_plan.import_published` / `floor_plan.import_publish_failed` | web | operation 级补充 |
| P0-待归类 | `publishTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs/{id}/publish` | 公布已批准的门票奖励规则 | `ticket.reward.config.manage` | `ticket.reward.config.publish.completed` / `ticket.reward.config.publish.completed.failed` | web | operation 级补充 |
| P0-待归类 | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | 跨对象查询操作留痕 | `audit.operation_log.view` | `audit.operation_log.viewed` / `audit.operation_log.view_failed` | web | operation 级补充 |
| P0-待归类 | `quoteDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/quote` | 对询价报价 | `dealer.inquiry.quote` | `dealer.inquiry.quoted` / `dealer.inquiry.quote_failed` | web | operation 级补充 |
| P0-待归类 | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | 发起合同变更(6 类) | `contract.change.manage` | `contract.change_raised` / `contract.change_raise_failed` | web | operation 级补充 |
| P0-待归类 | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | 受控读取本企业订单交接原文件 | `dealer_portal.order.manage` | `dealer.portal.delivery_evidence_viewed` / `dealer.portal.delivery_evidence_view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `readSiteEntryAttachment` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/attachments/{attachment_id}/file` | 受控读取本报备完工文件 | `facility.site_entry.view_assigned` | `facility.site_entry.attachment_accessed` / `facility.site_entry.attachment_access_failed` | web | operation 级补充 |
| P0-待归类 | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | 退货收货 | `dealer.return.inspect` | `dealer.return.received` / `dealer.return.receive_failed` | web | operation 级补充 |
| P0-待归类 | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | 复查整改后的巡检任务 | `facility.inspection.manage` | `facility.inspection_task_rechecked` / `facility.inspection_task_recheck_failed` | web | operation 级补充 |
| P0-待归类 | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | 登记退货退款执行结果 | `dealer.return.refund.execute` | `dealer.return.refund_executed` / `dealer.return.refund_execute_failed` | web | operation 级补充 |
| P0-待归类 | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | 录入抄表读数 | `energy.reading.manage` | `energy.reading_recorded` / `energy.reading_record_failed` | web | operation 级补充 |
| P0-待归类 | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | 记录设备维保完成 | `facility.equipment.manage` | `facility.equipment_maintained` / `facility.equipment_maintain_failed` | web | operation 级补充 |
| P0-待归类 | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | 记录账单催收 | `merchant.billing.dunning.record` | `merchant.billing.dunning.record` / `merchant.billing.dunning.record_failed` | web | operation 级补充 |
| P0-待归类 | `registerDealerOrderPayment` | `POST` | `/api/v1/dealers/orders/{id}/payments` | 登记经销订单到账 | `dealer.order.manage` | `dealer.order.payment_registered` / `dealer.order.payment_register_failed` | web | operation 级补充 |
| P0-待归类 | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | 外包对被指派巡检任务上报异常 | `facility.inspection.report_exception_assigned` | `facility.inspection_exception_reported` / `facility.inspection_exception_report_failed` | web | operation 级补充 |
| P0-待归类 | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | 超范围下单经营申请(拦下后可申请,走既有审批链) | `contract.scope_request.raise` | `contract.scope_request_raised` / `contract.scope_request_raise_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerDeliveryResolution` | `POST` | `/api/v1/dealers/delivery/discrepancies/{id}/resolutions` | 申请交付差异处理 | `dealer.order.manage` | `dealer.delivery.resolution_requested` / `dealer.delivery.resolution_request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | 申请电子发票 | `dealer.invoice.manage` | `dealer.invoice.requested` / `dealer.invoice.request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | 申请取消经销订单（出库前） | `dealer.order.manage` | `dealer.order.cancel_requested` / `dealer.order.cancel_request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | 申请经销退货 | `dealer.return.manage` | `dealer.return.requested` / `dealer.return.request_failed` | web | operation 级补充 |
| P0-待归类 | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | 裁决退租争议→回到待结算 | `contract.settlement.dispute.resolve` | `contract.dispute_resolved` / `contract.dispute_resolve_failed` | web | operation 级补充 |
| P0-待归类 | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | 恢复工单 | `facility.work_order.manage` | `facility.work_order_resumed` / `facility.work_order_resume_failed` | web | operation 级补充 |
| P0-待归类 | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | 红冲原发票并按最新未退金额重开 | `dealer.invoice.manage` | `dealer.invoice.reversed` / `dealer.invoice.reverse_failed` | web | operation 级补充 |
| P0-待归类 | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | 经销商准入审核 | `dealer.review` | `dealer.reviewed` / `dealer.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/review` | 经销订单审核（通过即预留库存并通知付款） | `dealer.order.review` | `dealer.order.reviewed` / `dealer.order.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | 复核异常抄表读数 | `energy.reading.manage` | `energy.reading_reviewed` / `energy.reading_review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | 报修方回访工单 | `facility.work_order.manage` | `facility.work_order_reviewed` / `facility.work_order_review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/review` | 物业审核进场报备（平台侧） | `facility.site_entry.review` | `facility.site_entry.review` / `facility.site_entry.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | 复核处置企微违规命中 | `wecom.risk_hit.review` | `wecom.risk_hit.review` / `wecom.risk_hit.review_failed` | web | operation 级补充 |
| P0-待归类 | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | 配置审批链(分组并行,按综合体覆盖) | `contract.approval.flow.manage` | `contract.approval_flow_saved` / `contract.approval_flow_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | 保存工单 SLA/验收策略 | `facility.work_order.manage` | `facility.work_order_policy_saved` / `facility.work_order_policy_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | 保存平面图图形 | `floor_plan.manage` | `floor_plan.shape.save` / `floor_plan.shape.save_failed` | web | operation 级补充 |
| P0-待归类 | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | 配置到期着色分类(颜色+提前天数) | `contract.space.color.manage` | `contract.color_rule_saved` / `contract.color_rule_save_failed` | web | operation 级补充 |
| P0-待归类 | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | 触发合规提醒扫描 | `compliance.reminder.scan` | `compliance.reminder.scan` / `compliance.reminder.scan_failed` | web | operation 级补充 |
| P0-待归类 | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | 出示公司统一收款码(H批代收;个人码流程已废弃) | `service.order.create` | `service.payment_code.presented` / `service.payment_code.present_failed` | staff_work_phone, web, tablet, app | operation 级补充 |
| P0-待归类 | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | 录入我的收款码(履约人) | `service.order.perform` | `service.payment_code.saved` / `service.payment_code.save_failed` | staff_private_phone, web | operation 级补充 |
| P0-待归类 | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | 配置公司统一收款码 | `service.catalog.manage` | `service.pay_config.saved` / `service.pay_config.save_failed` | web | operation 级补充 |
| P0-待归类 | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | 退租/到期结算(逐例顺序) | `contract.settlement.manage` | `contract.settled` / `contract.settle_failed` | web | operation 级补充 |
| P0-待归类 | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | 按账期结算能源费 | `energy.reading.manage` | `energy.fee_settled` / `energy.fee_settle_failed` | web | operation 级补充 |
| P0-待归类 | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | 按账期结算能源费(兼容路径) | `energy.reading.manage` | `energy.fee_settled` / `energy.fee_settle_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | 复核转正:归因规则草案(弃用薄别名) | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | 复核转正:单行/归属/归因规则草案转正 | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | 复核转正:排名档位梯子整体换梯 | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | 软删结算配置(status→inactive) | `settlement.config.manage` | `settlement.config.archived` / `settlement.config.archive_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | 内部结算SKU目录三维筛选列表 | `settlement.config.view` | `settlement.config.internal_price_catalog.viewed` / `settlement.config.internal_price_catalog.view_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | 结算配置中心总览 | `settlement.config.view` | `settlement.config.overview.viewed` / `settlement.config.overview.view_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | 维护第三方渠道抽成率(草案) | `settlement.config.manage` | `settlement.config.channel_fee_configs.updated` / `settlement.config.channel_fee_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertComboDiscount` | `POST` | `/api/v1/settlement/config/combo-discount` | 维护套票折扣归属(草案) | `settlement.config.manage` | `settlement.config.ticket_combo_discount_configs.updated` / `settlement.config.ticket_combo_discount_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | 维护卡座销售提成费率(草案) | `settlement.config.manage` | `settlement.config.bar_commission_rate_configs.updated` / `settlement.config.bar_commission_rate_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | 维护门票综合服务费(草案) | `settlement.config.manage` | `settlement.config.comprehensive_service_fee_configs.updated` / `settlement.config.comprehensive_service_fee_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertDistributionMethod` | `POST` | `/api/v1/settlement/config/distribution-method` | 维护通票分配方式(草案) | `settlement.config.manage` | `settlement.config.ticket_distribution_configs.updated` / `settlement.config.ticket_distribution_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | 维护高价值判定阈值(草案) | `settlement.config.manage` | `settlement.config.high_value_thresholds.updated` / `settlement.config.high_value_thresholds.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | 维护内部结算SKU目录条目(草案) | `settlement.config.manage` | `settlement.config.internal_price_catalog.updated` / `settlement.config.internal_price_catalog.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertMerchantRate` | `POST` | `/api/v1/settlement/config/merchant-rate` | 维护每客单价/固定额(草案) | `settlement.config.manage` | `settlement.config.ticket_distribution_merchant_rates.updated` / `settlement.config.ticket_distribution_merchant_rates.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | 维护赠送权益预估成本预扣规则(草案) | `settlement.config.manage` | `settlement.config.pass_gift_cost_configs.updated` / `settlement.config.pass_gift_cost_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertPointsAccrualRate` | `POST` | `/api/v1/settlement/config/points-accrual-rate` | 维护积分累计率(草案) | `settlement.config.manage` | `settlement.config.points_accrual_rate_configs.updated` / `settlement.config.points_accrual_rate_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers` | 维护排名档位梯子(草案·整套替换) | `settlement.config.manage` | `settlement.config.ticket_distribution_rank_tiers.updated` / `settlement.config.ticket_distribution_rank_tiers.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRetention` | `POST` | `/api/v1/settlement/config/retention` | 维护项目公司留存比例(草案) | `settlement.config.manage` | `settlement.config.platform_retention_rules.updated` / `settlement.config.platform_retention_rules.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRevenueStreamAttribution` | `POST` | `/api/v1/settlement/config/revenue-stream-attribution` | 维护业态收入归属(草案) | `settlement.config.manage` | `settlement.config.revenue_stream_attribution.updated` / `settlement.config.revenue_stream_attribution.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | 开票清单按主体汇总 | `settlement.invoice.view` | `settlement.invoice.summary` / `settlement.invoice.summary_failed` | web | operation 级补充 |
| P0-待归类 | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | 结算单过账(挂应付台账) | `settlement.bill.post` | `settlement.bill.posted` / `settlement.bill.post_failed` | web | operation 级补充 |
| P0-待归类 | `settlementSpecialApproveBill` | `POST` | `/api/v1/settlement/bills/{id}/special-approval` | 超池结算单特批(平台负责人短期激励) | `settlement.bill.special_approve` | `settlement.bill.special_approved` / `settlement.bill.special_approve_failed` | web | operation 级补充 |
| P0-待归类 | `shipDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/ship` | 仓库出库 | `dealer.order.ship` | `dealer.order.shipped` / `dealer.order.ship_failed` | web | operation 级补充 |
| P0-待归类 | `shipDealerReplacement` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/replacement-shipment` | 按批准差异补交出库 | `dealer.order.ship` | `dealer.delivery.replacement_shipped` / `dealer.delivery.replacement_ship_failed` | web | operation 级补充 |
| P0-待归类 | `signDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/sign` | 登记签收 | `dealer.order.manage` | `dealer.delivery.received` / `dealer.delivery.receive_failed` | web | operation 级补充 |
| P0-待归类 | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | 意向签约→转商户+喂入合同 draft | `leasing.convert` | `leasing.intent.signed` / `leasing.intent.sign_failed` | web | operation 级补充 |
| P0-待归类 | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | 开始巡检任务 | `facility.inspection.manage` | `facility.inspection_task_started` / `facility.inspection_task_start_failed` | web | operation 级补充 |
| P0-待归类 | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | 开始处理工单 | `facility.work_order.manage` | `facility.work_order_started` / `facility.work_order_start_failed` | web | operation 级补充 |
| P0-待归类 | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | 提交进审批链(→待确认) | `contract.draft.manage` | `contract.submitted` / `contract.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | 提交经销商准入审核 | `dealer.manage` | `dealer.submitted` / `dealer.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/submit` | 提交经销订单审核 | `dealer.order.manage` | `dealer.order.submitted` / `dealer.order.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | 提交价格目录审批 | `dealer.price_catalog.edit` | `dealer.price_catalog.submitted` / `dealer.price_catalog.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | 提交巡检记录 | `facility.inspection.manage` | `facility.inspection_record_submitted` / `facility.inspection_record_submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | 提交账单争议 | `merchant.billing.dispute.submit` | `merchant.billing.dispute.submit` / `merchant.billing.dispute.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | 商户专区·结算单异议留言 | `merchant.portal.settlement.dispute` | `merchant_portal.settlement.disputed` / `merchant_portal.settlement.dispute_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `submitSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/submit` | 外包提交进场报备（进入物业审核） | `facility.site_entry.submit_assigned` | `facility.site_entry.submit_assigned` / `facility.site_entry.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsBillingRule` | `POST` | `/api/v1/loyalty/ticket-points-billing/rules` | 提交全国积分账期审批 | `ticket.points.billing.manage` | `ticket.points.billing.rule.submitted` / `ticket.points.billing.rule.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsBillObjection` | `POST` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | 在异议期提交月结异议 | `ticket.points.billing.view` | `ticket.points.billing.objection.submitted` / `ticket.points.billing.objection.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsCalendar` | `POST` | `/api/v1/loyalty/ticket-points-billing/calendars` | 提交实际工作日日历审批 | `ticket.points.billing.manage` | `ticket.points.billing.calendar.submitted` / `ticket.points.billing.calendar.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsOutage` | `POST` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | 提交实际门户中断复核 | `ticket.points.billing.manage` | `ticket.points.billing.outage.submitted` / `ticket.points.billing.outage.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs` | 提交门票奖励规则或销售合同审批 | `ticket.reward.config.manage` | `ticket.reward.config.submit.completed` / `ticket.reward.config.submit.completed.failed` | web | operation 级补充 |
| P0-待归类 | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | 挂起工单 | `facility.work_order.manage` | `facility.work_order_suspended` / `facility.work_order_suspend_failed` | web | operation 级补充 |
| P0-待归类 | `swapStaffWorkPhone` | `POST` | `/api/v1/terminals/devices/work-phone-swap` | 夜班值班经理确认同班故障换机 | `terminal.work_phone.swap` | `terminal.work_phone.swapped` / `terminal.work_phone.swap_failed` | web, staff_work_phone | operation 级补充 |
| P0-待归类 | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | 企微会话同步入站（PIPL 合规，开关门控） | `wecom.sync` | `wecom.conversation_synced` / `wecom.conversation_sync_failed` | <br>channel: provider-webhook | operation 级补充 |
| P0-待归类 | `unbindUserFromPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/unbind-user` | 账号解绑外部协作机构 | `partner.org.manage` | `partner.org.user_unbound` / `partner.org.user_unbind_failed` | web | operation 级补充 |
| P0-待归类 | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | 合同付款条款与运费模板 | `dealer.manage` | `dealer.contract_terms_updated` / `dealer.contract_terms_update_failed` | web | operation 级补充 |
| P0-待归类 | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | 更新能源表计 | `energy.meter.manage` | `energy.meter_saved` / `energy.meter_save_failed` | web | operation 级补充 |
| P0-待归类 | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | 更新设备台账 | `facility.equipment.manage` | `facility.equipment_updated` / `facility.equipment_update_failed` | web | operation 级补充 |
| P0-待归类 | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | 招商线索状态流转(7 态) | `leasing.lead.manage` | `leasing.lead.status_changed` / `leasing.lead.status_change_failed` | web | operation 级补充 |
| P0-待归类 | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | 上传本企业订单交接凭证 | `dealer_portal.order.manage` | `evidence.attachment_uploaded` / `evidence.attachment_upload_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | 上传本报备真实完工文件 | `facility.site_entry.upload_completion_assigned` | `evidence.attachment_uploaded` / `evidence.attachment_upload_failed` | web | operation 级补充 |
| P0-待归类 | `uploadSiteEntryCompletion` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/completion` | 外包上传完工资料（待物业验收） | `facility.site_entry.upload_completion_assigned` | `facility.site_entry_completion_uploaded` / `facility.site_entry.upload_completion_failed` | web | operation 级补充 |
| P0-待归类 | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | 维护服务目录项(固定参考价) | `service.catalog.manage` | `service.catalog.saved` / `service.catalog.save_failed` | web | operation 级补充 |
| P0-待归类 | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | 拒绝设置企微自动到期策略 | `wecom.retention.manage` | `not_applicable` / `wecom.retention_policy_failed` | web | operation 级补充 |
| P0-待归类 | `verifyDealerOrderPayment` | `POST` | `/api/v1/dealers/payments/{id}/verify` | 财务核验到账 | `dealer.order.payment.verify` | `dealer.order.payment_verified` / `dealer.order.payment_verify_failed` | web | operation 级补充 |
| P0-待归类 | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | 分级查看企微会话消息（脱敏/原文，原文需审批+审计） | `wecom.view_raw` | `wecom.raw_viewed` / `wecom.raw_view_failed` | web, mobile_admin | operation 级补充 |
| STAFF-员工 | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | 提成入台账(手录/系统写入口) | `staff.payroll.manage` | `staff.payroll.commission_added` / `staff.payroll.commission_add_failed` | web | operation 级补充 |
| STAFF-员工 | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | 发起奖金/处罚复核(必须带事由；HR 会签＋直属领导向上三级通过后入账) | `staff.payroll.manage` | `staff.payroll.adjustment_requested` / `staff.payroll.adjustment_add_failed` | web | operation 级补充 |
| STAFF-员工 | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | 将一个真实未结事项交给在岗人员 | `staff.profile.review` | `staff.business_handover.assigned` / `staff.business_handover.assign_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | 按周批量排班(一次排一周) | `staff.shift.manage` | `staff.shift.submitted` / `staff.shift.save_failed` | web, staff_private_phone | operation 级补充 |
| STAFF-员工 | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | 提成月结拨付（酒吧 → 人员所属主体） | `staff.payroll.manage` | `staff.commission_settlement.closed` / `staff.commission_settlement.close_failed` | web | operation 级补充 |
| STAFF-员工 | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | 月结(定格全员薪资单) | `staff.payroll.manage` | `staff.payroll.month_closed` / `staff.payroll.month_close_failed` | web | operation 级补充 |
| STAFF-员工 | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | 记录临时顶班 | `staff.shift.manage` | `staff.shift.covered` / `staff.shift.cover_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | 登记派驻记录（员工 → 派驻酒吧） | `staff.profile.review` | `staff.dispatch.created` / `staff.dispatch.create_failed` | web | operation 级补充 |
| STAFF-员工 | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | 结束派驻记录 | `staff.profile.review` | `staff.dispatch.ended` / `staff.dispatch.end_failed` | web | operation 级补充 |
| STAFF-员工 | `getCurrentStaffWorkPhoneContext` | `GET` | `/api/v1/staff/attendance/work-phone-context` | 读取本人工作手机当前领用记录 | `staff.attendance.punch` | `staff.attendance.context_viewed` / `staff.attendance.context_view_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | 查看停离职状态与真实未结业务 | `staff.profile.review` | `staff.business_handover.viewed` / `staff.business_handover.view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | 读取普通扣除上限 | `staff.payroll.manage` | `staff.payroll.penalty_cap_viewed` / `staff.payroll.penalty_cap_view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | 读取排班可见天数 | `staff.shift.manage` | `staff.shift.visibility_viewed` / `staff.shift.visibility_view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | 申请寄存柜扫码凭证 | `staff.profile.self` | `staff.locker.scan_token_issued` / `staff.locker.scan_token_failed` | staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `issueStaffPhoneTimeAnchor` | `POST` | `/api/v1/staff/attendance/time-anchors` | 为本人工作手机签发班次时间锚 | `staff.attendance.punch` | `staff.attendance.anchor_issued` / `staff.attendance.anchor_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | 我的打卡记录(最近30天) | `staff.shift.view` | `staff.attendance_mine.viewed` / `staff.attendance_mine.view_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | 我的请假(员工手机) | `staff.leave.submit` | `staff.leave_mine.viewed` / `staff.leave_mine.view_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | 我的薪资(员工手机;仅已月结月份) | `staff.payroll.view_mine` | `staff.payroll_mine.viewed` / `staff.payroll_mine.view_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | 我的班次与点位(员工手机) | `staff.shift.view` | `staff.shift_mine.viewed` / `staff.shift_mine.view_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | 个税税表档位清单 | `staff.payroll.manage` | `staff.payroll.tax_bracket_viewed` / `staff.payroll.tax_bracket_view_failed` | web | operation 级补充 |
| STAFF-员工 | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | 扣罚申诉清单(本人/人事按员工) | `staff.payroll.view_mine` | `staff.payroll.adjustment_appeals_viewed` / `staff.payroll.adjustment_appeals_view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | 按日查考勤(后台/主管) | `staff.attendance.view` | `staff.attendance.viewed` / `staff.attendance.view_failed` | web, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffAttendanceReceiptReviews` | `GET` | `/api/v1/staff/attendance/receipt-events/{id}/reviews` | 查看领取时间核实记录 | `staff.shift.view` | `staff.attendance.receipt_review_viewed` / `staff.attendance.receipt_review_view_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | 查询提成月结拨付记录（按月） | `staff.payroll.manage` | `staff.commission_settlement.viewed` / `staff.commission_settlement.view_failed` | web | operation 级补充 |
| STAFF-员工 | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | 查询派驻记录 | `staff.profile.view` | `staff.dispatch.viewed` / `staff.dispatch.view_failed` | web, mobile_admin | operation 级补充 |
| STAFF-员工 | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | 查请假(主管/后台,按状态) | `staff.shift.manage` | `staff.leave.viewed` / `staff.leave.view_failed` | web, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | 全员薪资单(人事,按月) | `staff.payroll.manage` | `staff.payroll.viewed` / `staff.payroll.view_failed` | web | operation 级补充 |
| STAFF-员工 | `listStaffPhoneReceipts` | `GET` | `/api/v1/staff/attendance/receipt-events` | 查询工作手机领取事件和待核事实 | `staff.shift.view` | `staff.attendance.receipts_viewed` / `staff.attendance.receipts_view_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | 获取可注册公司 | `public` | `staff.registration_companies_viewed` / `staff.registration_companies_view_failed` | staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | 查看班次顶班记录 | `staff.shift.manage` | `staff.shift.covers_viewed` / `staff.shift.covers_view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | 查看排班修改历史 | `staff.shift.manage` | `staff.shift.history_viewed` / `staff.shift.history_view_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | 查排班(后台/主管) | `staff.shift.manage` | `staff.shift.viewed` / `staff.shift.view_failed` | web, staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffWorkPhoneContextsForRecovery` | `GET` | `/api/v1/staff/attendance/work-phone-contexts` | 主管查询本工作手机指定员工的历史领用记录 | `staff.shift.manage` | `staff.attendance.contexts_viewed` / `staff.attendance.contexts_view_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | 考勤月视图(员工手机,逐日六态) | `staff.shift.view` | `staff.attendance_month.viewed` / `staff.attendance_month.view_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `punchStaffAttendance` | `POST` | `/api/v1/staff/attendance/punch` | 考勤打卡(NFC/扫码) | `staff.attendance.punch` | `staff.attendance.punched` / `staff.attendance.punch_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | 柜机上报寄存柜扫码存取 | `not_applicable` | `staff.locker.recorded` / `staff.locker.record_failed` | kiosk | operation 级补充 |
| STAFF-员工 | `recordStaffPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events` | 提交持久化工作手机领取事件 | `staff.attendance.punch` | `staff.attendance.receipt_recorded` / `staff.attendance.receipt_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `recoverStaffWorkPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events/recover` | 主管代补报原工作手机未传领取事件 | `staff.shift.manage` | `staff.attendance.receipt_recovered` / `staff.attendance.receipt_recover_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | 申请调整普通扣除上限 | `staff.payroll.manage` | `staff.payroll.penalty_cap_requested` / `staff.payroll.penalty_cap_request_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | 请假审批(排班主管) | `staff.shift.manage` | `staff.leave.reviewed` / `staff.leave.review_failed` | web, staff_private_phone | operation 级补充 |
| STAFF-员工 | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | 固定工资设定(人事) | `staff.payroll.manage` | `staff.payroll.base_saved` / `staff.payroll.base_save_failed` | web | operation 级补充 |
| STAFF-员工 | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | 发起员工任职状态变更复核 | `staff.profile.review` | `staff.profile.employment_status_change_requested` / `staff.profile.employment_status_change_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | 设置排班可见天数 | `staff.shift.manage` | `staff.shift.visibility_changed` / `staff.shift.visibility_change_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | 员工对已生效扣罚提出申诉 | `staff.payroll.view_mine` | `staff.payroll.adjustment_appeal_submitted` / `staff.payroll.adjustment_appeal_submit_failed` | web, staff_work_phone, staff_private_phone | operation 级补充 |
| STAFF-员工 | `submitStaffAttendanceReceiptReview` | `POST` | `/api/v1/staff/attendance/receipt-events/{id}/review` | 提交工作手机领取时间核实 | `staff.shift.manage` | `staff.attendance.receipt_review_submitted` / `staff.attendance.receipt_review_submit_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | 提交请假(员工手机) | `staff.leave.submit` | `staff.leave.submitted` / `staff.leave.submit_failed` | staff_private_phone, web | operation 级补充 |
| STAFF-员工 | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | 公司员工本人注册 | `public` | `staff.self_registered` / `staff.self_register_failed` | staff_private_phone | operation 级补充 |
| STAFF-员工 | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | 维护个税税表档位 | `staff.tax_bracket.manage` | `staff.payroll.tax_bracket_changed` / `staff.payroll.tax_bracket_change_failed` | web | operation 级补充 |
| STAFF-员工 | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | 主管排班(双端) | `staff.shift.manage` | `staff.shift.submitted` / `staff.shift.save_failed` | web, staff_private_phone | operation 级补充 |
| SYS-系统健康 | `getSystemHealth` | `GET` | `/api/v1/system/health` | 健康检查 | `public` | `system.health_checked` / `system.health_check_failed` | web, mobile_admin<br>channel: monitoring | operation 级补充 |
| SYS-系统健康 | `getSystemVersion` | `GET` | `/api/v1/system/version` | 版本和构建信息 | `system.version.view` | `system.version_viewed` / `system.version_view_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `addTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-rollouts/{id}/targets` | 加入灰度白名单 | `terminal.switch.manage` | `terminal_canary.target_added` / `terminal_canary.target_add_failed` | web | operation 级补充 |
| TERM-终端设备 | `archiveTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-targets/{id}/archive` | 移出灰度白名单（软删除） | `terminal.switch.manage` | `terminal_canary.target_archived` / `terminal_canary.target_archive_failed` | web | operation 级补充 |
| TERM-终端设备 | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | 设备绑定 | `terminal.device.bind` | `terminal.device.bound` / `terminal.device.bind_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | 服务端检测设备违规 | `mdm.device.manage` | `mdm.violation.detect` / `mdm.violation.detect_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `enrollMdmDevice` | `POST` | `/api/v1/terminals/mdm/devices/enroll` | MDM 设备纳管/策略同步 | `mdm.device.manage` | `mdm.policy_checked` / `mdm.policy_check_failed` | web | operation 级补充 |
| TERM-终端设备 | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | 一体机配置，含售票型与非售票型模式 | `not_applicable` | `terminal.kiosk_config_viewed` / `terminal.access_rejected` | kiosk | operation 级补充 |
| TERM-终端设备 | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | 获取终端离线权益 | `terminal.device.bind` | `terminal.offline_entitlements.viewed` / `terminal.offline_entitlements.viewed_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | 违规处置闭环 | `mdm.violation.handle` | `mdm.violation.handle` / `mdm.violation.handle_failed` | web | operation 级补充 |
| TERM-终端设备 | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | 一体机心跳 | `not_applicable` | `terminal.kiosk_heartbeat` / `terminal.kiosk_heartbeat_failed` | kiosk | operation 级补充 |
| TERM-终端设备 | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | 查询端能力开关 | `terminal.switch.view` | `terminal_switch.viewed` / `terminal_switch.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmAppWhitelist` | `GET` | `/api/v1/terminals/mdm/app-whitelist` | 查询 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.viewed` / `mdm.app_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | 查询 MDM 设备违规留痕 | `mdm.violation.view` | `mdm.violation.viewed` / `mdm.violation.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmSiteWhitelist` | `GET` | `/api/v1/terminals/mdm/site-whitelist` | 查询 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.viewed` / `mdm.site_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listTerminalCanaryRollouts` | `GET` | `/api/v1/terminals/canary-rollouts` | 查询终端开关灰度试点 | `terminal.switch.view` | `terminal_canary.viewed` / `terminal_canary.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | 终端设备台账 | `terminal.switch.view` | `terminal.device.viewed` / `terminal.device.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | 标记设备找回 | `terminal.device.lifecycle` | `terminal.device.found` / `terminal.device.found_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | 标记设备挂失 | `terminal.device.lifecycle` | `terminal.device.lost` / `terminal.device.lost_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | 设备送修 | `terminal.device.lifecycle` | `terminal.device.repair` / `terminal.device.repair_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | 设备报废 | `terminal.device.lifecycle` | `terminal.device.scrap` / `terminal.device.scrap_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | 登记或轮换已绑定设备签名公钥 | `terminal.device.bind` | `terminal.device.signing_key_registered` / `terminal.device.signing_key_register_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | 上报 MDM 设备违规 | `mdm.device.manage` | `device.policy_violation` / `device.policy_violation_failed` | web | operation 级补充 |
| TERM-终端设备 | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | 归还终端设备 | `terminal.device.bind` | `terminal.device.returned` / `terminal.device.returned_failed` | staff_work_phone, tablet, kiosk, web | operation 级补充 |
| TERM-终端设备 | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | 同步终端任务 | `terminal.device.bind` | `terminal.sync_task.accepted` / `terminal.sync_task.accepted_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | 设备解绑 | `terminal.device.unbind` | `terminal.device.unbound` / `terminal.device.unbind_failed` | web | operation 级补充 |
| TERM-终端设备 | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | 修改端能力开关 | `terminal.switch.manage` | `terminal_switch.changed` / `terminal_switch.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | 设备商户归属设置/更换/解除 | `terminal.device.lifecycle` | `terminal.device.merchant_changed` / `terminal.device.merchant_change_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `upsertMdmAppWhitelist` | `PUT` | `/api/v1/terminals/mdm/app-whitelist/{id}` | 新增或更新 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.changed` / `mdm.app_whitelist.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertMdmSiteWhitelist` | `PUT` | `/api/v1/terminals/mdm/site-whitelist/{id}` | 新增或更新 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.changed` / `mdm.site_whitelist.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertTerminalCanaryRollout` | `PUT` | `/api/v1/terminals/canary-rollouts` | 开启或结束灰度试点 | `terminal.switch.manage` | `terminal_canary.rollout_changed` / `terminal_canary.rollout_change_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-approve` | 审批权益模板删除申请 | `ticket.entitlement.approve` | `ticket_entitlement_template.delete_approved` / `ticket_entitlement_template.delete_approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | 退款审批 | `ticket.refund.approve` | `ticket_refund.approved` / `ticket_refund.approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `archiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/archive` | 存档权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.archived` / `ticket_entitlement_template.archive_failed` | web | operation 级补充 |
| TIC-票务权益 | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | 绑定票类型权益 | `ticket.product.manage` | `ticket_product_entitlement.bound` / `ticket_product_entitlement.bound_failed` | web | operation 级补充 |
| TIC-票务权益 | `cancelTicketOrder` | `POST` | `/api/v1/ticketing/orders/{order_id}/cancel` | 取消未支付票务订单（回补库存） | `ticket.order.create` | `ticket_order.cancelled` / `ticket_order.cancel_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `confirmTicketOrderPayment` | `POST` | `/api/v1/ticketing/orders/{order_id}/confirm-payment` | 确认票务订单支付成功（回写 paid） | `finance.payment.register` | `ticket_order.payment_confirmed` / `ticket_order.payment_confirm_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `confirmTicketRefundPayout` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/confirm-payout` | 退款确认拨付 | `ticket.refund.approve` | `ticket_refund.payout_confirmed` / `ticket_refund.payout_confirm_failed` | web | operation 级补充 |
| TIC-票务权益 | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | 创建权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.changed` / `ticket_entitlement_template.changed_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules` | Create P2 ticket dynamic pricing rule draft | `ticket.dynamic_pricing.manage` | `ticket_dynamic_pricing.created.success` / `ticket_dynamic_pricing.created.failure` | web | operation 级补充 |
| TIC-票务权益 | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | 创建票务订单（含一体机辅助售票） | `ticket.order.create` | `ticket_order.created` / `ticket_order.create_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketOrderPayIntent` | `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | 创建票务支付意图（含一体机辅助售票） | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | 创建平日、周末、节假日及派生价规则 | `ticket.price_variant.manage` | `ticket_price_variant.changed` / `ticket_price_variant.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | 创建票种 | `ticket.product.manage` | `ticket_product.changed` / `ticket_product.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | 正式核销 | `verification.execute` | `verification.succeeded` / `verification.failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `getIssuedEntitlementFacePhoto` | `GET` | `/api/v1/ticketing/issued-entitlements/{entitlement_id}/face-photo` | 取购票人脸照（核销比对本人） | `verification.view` | `ticket_face_photo.viewed` / `ticket_face_photo.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `getTicketCredential` | `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | 获取动态二维码凭证 | `ticket.credential.view` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | web, miniapp, app | operation 级补充 |
| TIC-票务权益 | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | 读全场统一动态码刷新间隔 | `ticket.product.view` | `ticket.qr_refresh_config.viewed` / `ticket.qr_refresh_config.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | 出票（含一体机辅助售票） | `ticket.ticket.issue` | `ticket.issued` / `ticket.issue_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | 查询权益模板 | `ticket.entitlement.view` | `ticket_entitlement_template.viewed` / `ticket_entitlement_template.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | 查询已发放权益 | `ticket.verify.view` | `ticket_issued_entitlement.viewed` / `ticket_issued_entitlement.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketDynamicPricingRules` | `GET` | `/api/v1/ticketing/dynamic-pricing-rules` | List P2 ticket dynamic pricing rules | `ticket.dynamic_pricing.view` | `ticket_dynamic_pricing.viewed.success` / `ticket_dynamic_pricing.viewed.failure` | web | operation 级补充 |
| TIC-票务权益 | `listTicketInstances` | `GET` | `/api/v1/ticketing/ticket-instances` | 查询父票实例 | `ticket.verify.view` | `ticket_instance.viewed` / `ticket_instance.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketOrders` | `GET` | `/api/v1/ticketing/orders` | 查询票务订单 | `ticket.order.read` | `ticket_order.viewed` / `ticket_order.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | 查询平日、周末、节假日及派生价规则 | `ticket.price_variant.view` | `ticket_price_variant.viewed` / `ticket_price_variant.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketRefundLines` | `GET` | `/api/v1/ticketing/refunds/{refund_id}/lines` | 查询退票明细行 | `ticket.refund.view` | `ticket.refund_line.list` / `ticket.refund_line.list_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketRefunds` | `GET` | `/api/v1/ticketing/refunds` | 查询退款单列表 | `ticket.refund.view` | `ticket_refund.viewed` / `ticket_refund.view_failed` | web, merchant_portal | operation 级补充 |
| TIC-票务权益 | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | 查询门票转赠记录(客服纠纷排查,只读) | `ticket.transfer.read` | `ticket_transfer.viewed` / `ticket_transfer.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | 票面赠酒清单（按酒吧分组） | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | 票种查询 | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | 查询票核销记录 | `verification.view` | `verification.viewed` / `verification.view_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TIC-票务权益 | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | 票务核销预览（已废止，不得新增调用） | `verification.execute` | `verification.previewed` / `verification.failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `publishTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules/{id}/publish` | Publish P2 ticket dynamic pricing rule | `ticket.dynamic_pricing.publish` | `ticket_dynamic_pricing.published.success` / `ticket_dynamic_pricing.published.failure` | web | operation 级补充 |
| TIC-票务权益 | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | 发布票种 | `ticket.product.manage` | `ticket_price.published` / `ticket_price.publish_failed` | web | operation 级补充 |
| TIC-票务权益 | `rejectTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/reject` | 退款驳回 | `ticket.refund.approve` | `ticket_refund.rejected` / `ticket_refund.reject_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-request` | 发起权益模板删除申请 | `ticket.entitlement.manage` | `ticket_entitlement_template.delete_requested` / `ticket_entitlement_template.delete_request_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | 退款或撤销申请 | `ticket.refund.request` | `ticket_refund.requested` / `ticket_refund.request_failed` | web, merchant_portal | operation 级补充 |
| TIC-票务权益 | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | 核销冲正 | `ticket.verify.reverse` | `ticket.verification_reversed` / `ticket.verification_reverse_failed` | web, staff_work_phone, mobile_admin | operation 级补充 |
| TIC-票务权益 | `simulateTicketDynamicPricing` | `POST` | `/api/v1/ticketing/dynamic-pricing/simulations` | Simulate P2 ticket dynamic pricing without creating an order | `ticket.dynamic_pricing.simulate` | `ticket_dynamic_pricing.simulated.success` / `ticket_dynamic_pricing.simulated.failure` | web | operation 级补充 |
| TIC-票务权益 | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | 票种定价核算（系统算出售价 vs 填的售价） | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `unarchiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/unarchive` | 取消存档权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.unarchived` / `ticket_entitlement_template.unarchive_failed` | web | operation 级补充 |
| TIC-票务权益 | `updateTicketQrRefreshConfig` | `PUT` | `/api/v1/ticketing/qr-refresh-config` | 改全场统一动态码刷新间隔 | `ticket.product.manage` | `ticket.qr_refresh_config.updated` / `ticket.qr_refresh_config.update_failed` | web | operation 级补充 |
| TIC-票务权益 | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | 改票种售价／折扣（凑整误差闸） | `ticket.product.manage` | `ticket_product.pricing_changed` / `ticket_product.pricing_change_failed` | web | operation 级补充 |

## 6. OpenAPI 示例覆盖执行口径

当前 OpenAPI 已有 P0 首批高风险样例覆盖：

| 类型 | 已覆盖 |
|---|---|
| request body | Webhook, CustomerTicketOrderRequest, Verification, CustomerBarReservationRequest, BarOrderRequest, PaymentIntent, AmusementSafetyActionRequest, ProviderAdapterTestRequest, FinanceRefundRequest, RefundOperationRequest, RefundExecutionRequest, RefundResultConfirmRequest |
| response | Success, Page, WebhookAccepted, FinanceRefundResult, FinanceRefundDetail, FinanceRefundList, ArApPaymentList |

执行口径：

1. 上表只是 P0 首批高风险样例，不代表所有写接口和外部接口样例已齐。
2. 任一写接口或外部接入接口进入 Controller、SDK/mock、接口自动化或供应商联调前，必须补齐请求样例、成功响应样例、主要失败响应样例。
3. 支付、核销、订台、出票、退款、年龄/人脸核验、应收应付、Webhook、轮询校验和补偿接口不得无样例进入联调。
4. 示例字段必须使用 OpenAPI schema 的 snake_case 字段名，不得沿用旧 TypeScript/SQLite 原型字段。

## 7. 验收标准

### AC-CLOSURE-001 权限闭环

Given 任一 P0 operation 进入开发，When 开发者查看权限要求，Then 必须能从 OpenAPI 和本文第 3 节找到 `x-permission`、`x-auth-context`、`x-data-scope` 和允许终端。

### AC-CLOSURE-002 事件闭环

Given 任一 P0 operation 成功或失败，When 测试审计事件，Then 必须能从本文第 4 节找到对应成功/失败事件名，并在事件中心查到操作者、终端、设备、业务对象、结果、失败原因和 trace_id。

### AC-CLOSURE-003 任务追溯闭环

Given 任一 P0 任务准备进入编码，When 任务包含后端接口、前端调用、Android 调用或供应商联调，Then 任务说明必须引用本文第 5 节中的 operationId，不得只写“见相关接口”。

### AC-CLOSURE-004 示例准入

Given 任一 P0 写接口或外部接口进入联调，When 没有请求样例、成功响应样例和主要失败响应样例，Then 该接口不得标记为联调就绪。

### AC-CLOSURE-005 历史路径隔离

Given 文档中仍保留 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 历史能力路径，When 开发或测试引用接口，Then 必须改用 OpenAPI exact path 或 operationId；历史路径不得作为 Controller、SDK/mock 或接口自动化依据。

## 8. 边界场景

| 场景 | 必测要求 |
|---|---|
| 权限码只在 OpenAPI 出现，不在人工权限矩阵正文出现 | 以本文第 3 节为准，并在下一次权限矩阵维护时同步 |
| 事件名只在 OpenAPI 出现，不在人工事件矩阵正文出现 | 以本文第 4 节为准，事件中心必须照常落库 |
| 客人端、设备端、Webhook 使用 `not_applicable` | 不得误建员工权限点，必须按 `x-auth-context` 校验身份 |
| 售票型与非售票型一体机能力混用 | 按 `kiosk_mode`、设备绑定、能力开关和 `x-terminal-allowed` 拒绝 |
| 旧路径表格与 OpenAPI 冲突 | 旧路径表格只作为历史能力名，正式实现以 OpenAPI 和本文为准 |

版本说明：本文档版本与 `docs/openapi/openapi-p0.json` 的 `info.version` 同步，契约变更即版本递增；不另维护变更记录表（历史见 OpenAPI 与 git 记录）。
