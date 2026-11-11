# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.63

## 1. 文档定位

本文档由 `docs/openapi/openapi-p0.json` 反向生成，用于收口本轮文档审计发现的三个执行缺口：

1. OpenAPI 已声明的权限码没有全部进入人工权限矩阵。
2. OpenAPI 已声明的成功/失败事件没有全部进入人工事件证据矩阵。
3. P0 任务文档不能逐 operation 追溯到权限、事件、终端和验收责任。

本文不是新需求池，不改变已拍板业务口径。若本文与历史 PRD、历史接口表或旧能力路径冲突，以 `00-current-effective-document-entry-v0.1.md`、`docs/openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 和本文为准。

## 2. 收口结论

| 检查项 | 当前结果 | 执行结论 |
|---|---:|---|
| OpenAPI operation 数 | 1360 | 全部必须进入 Java Controller、DTO、Service、权限、事件和验收追溯 |
| OpenAPI 权限/通用权限状态数 | 444 | 人工权限矩阵正文未覆盖 343 项；缺口以本文第 3 节补齐 |
| OpenAPI 成功/失败事件数 | 2720 | 人工事件矩阵正文未覆盖 2671 项；完整事件字典以本文第 4 节为准 |
| operation 级任务追溯 | 1360 | 人工任务映射正文未逐项覆盖 1354 个 operation；逐 operation 映射以本文第 5 节为准 |
| 写接口数量 | 837 | 写接口、外部接入接口进入编码或联调前必须补请求/响应示例 |

## 3. 权限码与认证上下文补充表

> 本节覆盖 OpenAPI 中全部 `x-permission`。其中 `public`、`authenticated`、`not_applicable` 是通用权限状态，不是员工岗位权限；员工、设备、客人、开放应用和 Webhook 的身份来源必须看 `x-auth-context`。

| 权限码/通用状态 | 认证上下文 | 数据范围 | operation 数 | 代表 operationId | 覆盖状态 |
|---|---|---:|---:|---|---|
| `access.grant_request.apply` | staff-token | project | 1 | createAccessGrantRequest | OpenAPI 补充，以本附录为准 |
| `access.grant_request.review` | staff-token | project | 1 | reviewAccessGrantRequest | OpenAPI 补充，以本附录为准 |
| `access.grant_request.view` | staff-token | platform, project | 3 | listAccessGrantRequests, getAccessGrantSummary, getAccessGrantPolicy | OpenAPI 补充，以本附录为准 |
| `access.permission_template.apply` | staff-token | project | 1 | applyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.manage` | staff-token | project | 3 | createPermissionTemplate, updatePermissionTemplate, copyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.view` | staff-token | project | 1 | listPermissionTemplates | 已在权限矩阵正文出现 |
| `access.permission.view` | staff-token | project | 1 | listPermissions | 已在权限矩阵正文出现 |
| `access.role.assign` | staff-token | project | 15 | assignUserRole, unassignUserRole, listPositionRoleTemplates, createPositionRoleTemplate, updatePositionRoleTemplate, deletePositionRoleTemplate, changePositionRoleTemplateLifecycle, assignUserPosition, changeUserScope, setUserPermissionOverrides, removeUserPermissionOverrides, listApprovalLines ... | OpenAPI 补充，以本附录为准 |
| `access.role.manage` | staff-token | project | 4 | createRole, listRoleProjectOptions, getRoleViewableProjects, updateRoleViewableProjects | 已在权限矩阵正文出现 |
| `access.role.view` | staff-token | project | 1 | listRoles | 已在权限矩阵正文出现 |
| `access.staff_level.manage` | staff-token | project | 1 | assignStaffLevel | OpenAPI 补充，以本附录为准 |
| `access.staff_level.view` | staff-token | project | 1 | listStaffLevels | OpenAPI 补充，以本附录为准 |
| `access.temporary_grant.create` | staff-token | project | 1 | createTemporaryGrant | 已在权限矩阵正文出现 |
| `access.temporary_grant.revoke` | staff-token | project | 1 | revokeTemporaryGrant | 已在权限矩阵正文出现 |
| `access.user.manage` | staff-token | project | 1 | createAccessUser | 已在权限矩阵正文出现 |
| `access.user.view` | staff-token | project | 2 | listAccessUsers, getUserEffectivePermissions | 已在权限矩阵正文出现 |
| `admission.policy.manage` | staff-token | platform | 2 | updateApprovalDeadline, updateAccessGrantPolicy | OpenAPI 补充，以本附录为准 |
| `amusement.business_day.manage` | staff-token | project | 1 | submitAmusementBusinessDayConfig | OpenAPI 补充，以本附录为准 |
| `amusement.business_day.view` | staff-token | project | 1 | listAmusementBusinessDayConfigs | OpenAPI 补充，以本附录为准 |
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
| `amusement.park_fault.view` | staff-token | project | 6 | listAmusementParkWideFaults, listAmusementParkWideFaultActions, getAmusementCompensationPolicy, listAmusementFaultCompensationBatches, listAmusementFaultCompensationItems, listAmusementCompensationBatchesByEvent | OpenAPI 补充，以本附录为准 |
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
| `amusement.session.end` | staff-token | project-amusement | 1 | endAmusementSession | OpenAPI 补充，以本附录为准 |
| `amusement.session.manage` | staff-token | project, project-amusement | 5 | createAmusementProject, createAmusementSession, assignAmusementProjectSpace, listAmusementProjectsWithoutSpace, listAmusementProjectsWithoutDeviceRegistry | 已在权限矩阵正文出现 |
| `amusement.session.view` | staff-token | project-amusement | 5 | listAmusementProjects, listAmusementProjectStatusBoard, listAmusementSessions, listAmusementSessionEndings, listAmusementSessionFollowups | 已在权限矩阵正文出现 |
| `amusement.verification.execute` | staff-token | project-amusement-point | 2 | createAmusementVerification, createAmusementExitVerification | 已在权限矩阵正文出现 |
| `amusement.verification.view` | staff-token | project-amusement | 3 | listAmusementVerifications, listAmusementCountReleases, listAmusementCapacitySnapshots | 已在权限矩阵正文出现 |
| `amusement.waiver.acknowledge` | staff-token | project-amusement | 1 | acknowledgeAmusementSafetyNotice | OpenAPI 补充，以本附录为准 |
| `analytics.dashboard.view` | staff-token | platform, project | 18 | listBusinessMetricSnapshots, getP0OverviewDashboard, getAnalyticsDomainDashboard, listMerchantHealthScores, listAnalyticsMerchants, listMetricDefinitions, getMetricDetail, getRobotAnalytics, getAnalyticsTrafficHourly, listOperatingParams, getAiThresholds, listNotificationWindows ... | 已在权限矩阵正文出现 |
| `analytics.group.view` | staff-token | group | 1 | getGroupOverview | OpenAPI 补充，以本附录为准 |
| `analytics.metric_anomaly.manage` | staff-token | project | 1 | disposeMetricAnomaly | OpenAPI 补充，以本附录为准 |
| `analytics.operating_params.manage` | staff-token | platform, project | 6 | upsertOperatingParams, updateAiThresholds, upsertRevenueAttributionRule, updateAnomalyConfig, upsertHolidayPeriod, fetchHolidayCalendar | OpenAPI 补充，以本附录为准 |
| `analytics.report.export` | staff-token | platform | 1 | exportMonthlyReport | OpenAPI 补充，以本附录为准 |
| `analytics.review.lock` | staff-token | project | 2 | listReviewLocks, createReviewLock | OpenAPI 补充，以本附录为准 |
| `analytics.risk_alert.manage` | staff-token | project | 3 | generateLocalLlmAiQualitySuggestion, createRiskAlert, resolveRiskAlert | 已在权限矩阵正文出现 |
| `analytics.risk_alert.view` | staff-token | project | 8 | listAiQualityResults, listAiQualityTasks, listCustomerIntents, listMetricAnomalies, listMetricAnomalyTimeline, listRiskAlerts, listRiskRules, listStaffServiceScores | 已在权限矩阵正文出现 |
| `audit.operation_log.export` | staff-token | platform, project-bar-table | 2 | exportBarSuspendedOrders, exportAuditOperationEvents | OpenAPI 补充，以本附录为准 |
| `audit.operation_log.view` | staff-token | platform | 1 | queryAuditOperationEvents | OpenAPI 补充，以本附录为准 |
| `authenticated` | staff-token | current-session, merchant, platform, self | 9 | logout, refreshToken, getEffectivePermissions, approveDeleteSpaceType, listManualBackfills, recordPageView, listMyExports, requestEditableExportCopy, downloadEditableExportCopy | 已在权限矩阵正文出现 |
| `bar.bill.adjust` | staff-token | project-bar-table | 2 | raiseBarBillAnomaly, adjustBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.anomaly.resolve` | staff-token | project-bar-table | 1 | resolveBarBillAnomaly | OpenAPI 补充，以本附录为准 |
| `bar.bill.hang` | staff-token | project-bar-table | 2 | suspendBarOrder, requestBarOrderSuspensionCancel | OpenAPI 补充，以本附录为准 |
| `bar.bill.hang_cancel` | staff-token | project-bar-table | 2 | confirmBarOrderSuspensionBadDebt, decideBarOrderSuspensionCancel | OpenAPI 补充，以本附录为准 |
| `bar.bill.refund` | staff-token | project-merchant | 1 | getBarBillRefundContext | OpenAPI 补充，以本附录为准 |
| `bar.bill.reopen` | staff-token | project-bar-table | 1 | reopenBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.split_merge` | staff-token | project-bar-table | 3 | listBarBillOperations, mergeBarBill, splitBarBill | OpenAPI 补充，以本附录为准 |
| `bar.bill.tip` | staff-token | project-bar-table | 1 | addBarBillTip | OpenAPI 补充，以本附录为准 |
| `bar.business_day.manage` | staff-token | project | 1 | submitBarBusinessDayConfig | OpenAPI 补充，以本附录为准 |
| `bar.business_day.view` | staff-token | project | 1 | listBarBusinessDayConfigs | OpenAPI 补充，以本附录为准 |
| `bar.category.approve` | staff-token | project | 1 | approveDeleteBarProductCategory | OpenAPI 补充，以本附录为准 |
| `bar.category.manage` | staff-token | project | 4 | saveBarProductCategory, archiveBarProductCategory, unarchiveBarProductCategory, requestDeleteBarProductCategory | OpenAPI 补充，以本附录为准 |
| `bar.category.view` | staff-token | project | 1 | listBarProductCategories | OpenAPI 补充，以本附录为准 |
| `bar.combo.approve` | staff-token | project | 1 | approveDeleteBarCombo | OpenAPI 补充，以本附录为准 |
| `bar.combo.manage` | staff-token | project | 4 | saveBarCombo, archiveBarCombo, unarchiveBarCombo, requestDeleteBarCombo | OpenAPI 补充，以本附录为准 |
| `bar.combo.view` | staff-token | project | 1 | listBarCombos | OpenAPI 补充，以本附录为准 |
| `bar.deposit_rule.manage` | staff-token | project-bar | 1 | createBarDepositRule | OpenAPI 补充，以本附录为准 |
| `bar.entitlement.read` | staff-token | project-bar | 1 | listBarEntitlementRedemptions | OpenAPI 补充，以本附录为准 |
| `bar.entitlement.redeem` | staff-token | project-bar | 1 | redeemBarDrinkEntitlement | OpenAPI 补充，以本附录为准 |
| `bar.order_item.void` | staff-token | project, project-bar-table | 4 | voidBarOrderItem, decideBarOrderItemVoid, listBarOrderItemVoids, listBarVoidReasonCodes | OpenAPI 补充，以本附录为准 |
| `bar.order_item.void_decide` | staff-token | project | 2 | saveBarVoidReasonCode, archiveBarVoidReasonCode | OpenAPI 补充，以本附录为准 |
| `bar.order.create` | staff-token | project, project-bar-table | 6 | createBarOrder, createBarOrderBill, deliverBarOrder, addBarOrderItem, listBarProductionTickets, placeBarScanOrder | 已在权限矩阵正文出现 |
| `bar.order.view` | staff-token | project-bar-table | 3 | listBarOrders, listBarSuspendedOrders, listBarBillMerchantRewards | 已在权限矩阵正文出现 |
| `bar.payroll.manage` | staff-token | project-merchant | 4 | draftBarPayrollRule, confirmBarPayrollRule, submitBarPayrollRule, closeBarPayrollMonth | OpenAPI 补充，以本附录为准 |
| `bar.payroll.view` | staff-token | project-merchant | 3 | listBarPayrollRules, listBarPayrollRuns, listBarServiceResponsibilities | OpenAPI 补充，以本附录为准 |
| `bar.product.approve` | staff-token | project | 1 | approveDeleteBarProduct | OpenAPI 补充，以本附录为准 |
| `bar.product.manage` | staff-token | project | 6 | createBarProduct, assignBarProductCategory, archiveBarProduct, unarchiveBarProduct, requestDeleteBarProduct, uploadBarCatalogImage | OpenAPI 补充，以本附录为准 |
| `bar.product.sold_out` | staff-token | project-bar-table | 1 | markBarProductSoldOut | OpenAPI 补充，以本附录为准 |
| `bar.product.view` | staff-token | project | 3 | listBarProducts, listBarProductsByCategory, getBarCatalogImage | OpenAPI 补充，以本附录为准 |
| `bar.production_ticket.manage` | staff-token | project-bar-table | 1 | transitionBarProductionTicket | OpenAPI 补充，以本附录为准 |
| `bar.production_ticket.view` | staff-token | project-bar-table | 1 | listBarProductionTicketEvents | OpenAPI 补充，以本附录为准 |
| `bar.reservation.approve` | staff-token | project-bar | 1 | approveDeleteBarReservation | OpenAPI 补充，以本附录为准 |
| `bar.reservation.create` | staff-token | project-bar, project-merchant-space | 2 | createBarReservation, listBarTableAvailability | 已在权限矩阵正文出现 |
| `bar.reservation.invitation.manage` | staff-token | project-bar | 2 | issueBarReservationInvitation, revokeBarReservationInvitation | OpenAPI 补充，以本附录为准 |
| `bar.reservation.manage` | staff-token | project, project-bar | 7 | payBarReservationDeposit, checkInBarReservationDeposit, cancelBarReservation, archiveBarReservation, unarchiveBarReservation, requestDeleteBarReservation, saveBarArrivalReminderPolicy | OpenAPI 补充，以本附录为准 |
| `bar.reservation.view` | staff-token | project, project-bar, project-bar-table | 3 | listBarReservations, getBarReservationInvitation, getBarArrivalReminderPolicy | 已在权限矩阵正文出现 |
| `bar.saved_bottle.manage` | staff-token | project-bar-customer, project-bar-table | 20 | listSavedBottles, createSavedBottle, claimSavedBottle, lookupSavedBottlesByPhone, transferSavedBottle, requestSavedBottleVerificationCode, dispenseSavedBottleManualPickup, listSavedBottleRenewals, renewSavedBottle, cancelSavedBottle, disputeSavedBottle, registerSavedBottleRemedy ... | 已在权限矩阵正文出现 |
| `bar.saved_bottle.manual_pickup_policy.manage` | staff-token | project | 1 | configureSavedBottleManualPickupPolicy | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.manual_pickup.confirm_marketing` | staff-token | project-bar-customer | 1 | confirmSavedBottleManualPickupMarketing | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.manual_pickup.initiate` | staff-token | project-bar-customer | 1 | initiateSavedBottleManualPickup | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.manual_pickup.review` | staff-token | project-bar-customer | 1 | reviewSavedBottleManualPickup | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.reminder_policy.manage` | staff-token | project | 2 | listSavedBottleReminderPolicies, configureSavedBottleReminderPolicy | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.renewal_policy.manage` | staff-token | project | 1 | configureSavedBottleRenewalPolicy | OpenAPI 补充，以本附录为准 |
| `bar.saved_bottle.renewal.review` | staff-token | project-bar-customer | 1 | reviewSavedBottleRenewal | OpenAPI 补充，以本附录为准 |
| `bar.shift.close` | staff-token | project-bar-table | 1 | closeBarShift | OpenAPI 补充，以本附录为准 |
| `bar.shift.manage` | staff-token | project-bar-table | 4 | listBarShifts, openBarShift, joinBarShift, signBarShift | OpenAPI 补充，以本附录为准 |
| `bar.staff_booking.restore` | staff-token | project-bar | 2 | listBarStaffBookingSuspensions, restoreBarStaffBookingSuspension | OpenAPI 补充，以本附录为准 |
| `bar.table_commission.calculate` | staff-token | project-bar | 1 | calculateBarTableCommission | 已在权限矩阵正文出现 |
| `bar.table_session.attribution_reassign` | staff-token | project-bar-table | 1 | reassignBarTableSessionAttribution | OpenAPI 补充，以本附录为准 |
| `bar.table_session.create` | staff-token | project-bar-table | 2 | createBarTableSession, submitBarTableSessionSalesBackfill | OpenAPI 补充，以本附录为准 |
| `bar.table_session.merge` | staff-token | project-bar-table | 1 | mergeBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.split` | staff-token | project-bar-table | 1 | splitBarTableSession | OpenAPI 补充，以本附录为准 |
| `bar.table_session.transfer` | staff-token | project-bar-table, project-merchant | 4 | transferBarTableSession, undoBarTableTransfer, handoverBarServiceResponsibility, submitBarTableSessionServiceBackfill | OpenAPI 补充，以本附录为准 |
| `bar.table_session.view` | staff-token | project-bar-table | 1 | listBarTableSessions | OpenAPI 补充，以本附录为准 |
| `bar.table.disable` | staff-token | project-bar | 2 | disableBarTable, enableBarTable | OpenAPI 补充，以本附录为准 |
| `bar.table.guest_checkin` | staff-token | project-bar | 1 | verifyBarReservationInvitationEntry | 已在权限矩阵正文出现 |
| `bar.table.manage` | staff-token | project-bar, project-bar-table | 3 | createBarTable, assignBarTableSpace, listBarTablesWithoutSpace | 已在权限矩阵正文出现 |
| `bar.table.open_close` | staff-token | project-bar, project-merchant | 3 | openBarTable, clearBarTable, closeBarServiceResponsibility | 已在权限矩阵正文出现 |
| `bar.table.view` | staff-token | project-bar | 1 | listBarTables | 已在权限矩阵正文出现 |
| `compliance.contract_key_date.manage` | staff-token | - | 1 | createContractKeyDate | OpenAPI 补充，以本附录为准 |
| `compliance.contract_key_date.view` | staff-token | - | 1 | listContractKeyDates | OpenAPI 补充，以本附录为准 |
| `compliance.expiry_item.manage` | staff-token | -, platform | 3 | createComplianceExpiryItem, saveComplianceItemTypeConfig, renewComplianceExpiryItem | OpenAPI 补充，以本附录为准 |
| `compliance.expiry_item.view` | staff-token | - | 2 | listComplianceExpiryItems, listComplianceItemTypeConfigs | OpenAPI 补充，以本附录为准 |
| `compliance.grace_period.request` | staff-token | merchant | 1 | requestComplianceGracePeriod | OpenAPI 补充，以本附录为准 |
| `compliance.reminder.scan` | staff-token | - | 1 | scanComplianceReminders | OpenAPI 补充，以本附录为准 |
| `compliance.reminder.view` | staff-token | - | 1 | listComplianceReminders | OpenAPI 补充，以本附录为准 |
| `contract.approval.flow.manage` | staff-token | platform | 2 | listContractApprovalFlow, saveContractApprovalFlow | OpenAPI 补充，以本附录为准 |
| `contract.change.manage` | staff-token | platform | 3 | listContractChanges, raiseContractChange, listContractAmendments | OpenAPI 补充，以本附录为准 |
| `contract.deposit_months.manage` | staff-token | platform | 2 | getContractDepositMonths, saveContractDepositMonths | OpenAPI 补充，以本附录为准 |
| `contract.deposit_refund_wait.manage` | staff-token | project | 2 | getContractDepositRefundWait, saveContractDepositRefundWait | OpenAPI 补充，以本附录为准 |
| `contract.draft.manage` | staff-token | platform | 6 | listContracts, createContractDraft, submitContract, listSpaceRentalStatus, addContractAttachments, editContractDraftRentTerms | 已在权限矩阵正文出现 |
| `contract.lifecycle.activate` | staff-token | platform | 1 | activateContract | OpenAPI 补充，以本附录为准 |
| `contract.scope_request.raise` | staff-token | platform | 1 | requestContractScopeGrant | OpenAPI 补充，以本附录为准 |
| `contract.settlement.dispute.resolve` | staff-token | platform | 1 | resolveContractDispute | OpenAPI 补充，以本附录为准 |
| `contract.settlement.manage` | staff-token | platform | 3 | settleContract, listContractSettlements, checkContractSettlementDocuments | OpenAPI 补充，以本附录为准 |
| `contract.space.color.manage` | staff-token | platform | 2 | listSpaceExpiryColorRules, saveSpaceExpiryColorRule | OpenAPI 补充，以本附录为准 |
| `contract.view` | staff-token | merchant, platform | 4 | listMerchantPortalContracts, listVacantSpaces, listExpiringContracts, getRentToSales | 已在权限矩阵正文出现 |
| `customer.account.manage` | staff-token | platform, project | 7 | lookupCustomerAccountByPhone, suspendCustomerAccount, reinstateCustomerAccount, getCustomerRestrictionSetting, configureCustomerRestrictionSetting, getCustomerChurnAlertSetting, configureCustomerChurnAlertSetting | OpenAPI 补充，以本附录为准 |
| `customer.blacklist.approve` | staff-token | project | 2 | approveCustomerBlacklistRequest, rejectCustomerBlacklistRequest | OpenAPI 补充，以本附录为准 |
| `customer.blacklist.request` | staff-token | project | 2 | requestCustomerBlacklistChange, listCustomerBlacklistRequests | OpenAPI 补充，以本附录为准 |
| `customer.home_content.manage` | staff-token | project | 2 | getCustomerHomeContent, publishCustomerHomeContent | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.evaluate` | staff-token | project | 1 | evaluateMemberLevel | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.manage` | staff-token | platform, project | 4 | saveLoyaltyConfigMemberLevel, saveLoyaltyConfigCheckInTier, saveLoyaltyConfigBirthdayRule, fulfillPointsRedemption | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.view` | staff-token | platform, project | 4 | listMemberLevels, listLoyaltyConfigMemberLevels, listLoyaltyConfigCheckInTiers, listLoyaltyConfigBirthdayRules | OpenAPI 补充，以本附录为准 |
| `customer.points.freeze` | staff-token | project | 2 | freezePointsAccount, unfreezePointsAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.consume` | customer-token | self | 1 | payCustomerTicketOrderWithStoredValue | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.freeze` | staff-token | project | 2 | freezeStoredValueAccount, unfreezeStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.refund` | staff-token | merchant, platform, project | 7 | refundStoredValueAccount, configureStoredValueMerchantRefundRule, getStoredValueMerchantRefundRule, configureStoredValueMerchantCard, counterRefundStoredValueAccount, getMerchantCardExitNoticePolicy, saveMerchantCardExitNoticePolicy | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.topup` | customer-token | customer | 2 | customerStoredValueTopup, customerStoredValueSettle | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.view` | customer-token | customer | 2 | customerStoredValueAccount, customerStoredValueAccounts | OpenAPI 补充，以本附录为准 |
| `customer.voucher.manage` | staff-token | merchant | 3 | createVoucherDefinition, issueVouchers, revokeVoucher | OpenAPI 补充，以本附录为准 |
| `customer.voucher.redeem` | staff-token | merchant | 2 | redeemVoucher, previewVoucherRedemption | OpenAPI 补充，以本附录为准 |
| `customer.voucher.view` | customer-token, staff-token | customer, merchant | 5 | customerVoucherWallet, listVoucherDefinitions, listVoucherInstances, listVoucherRedemptions, listVoucherOverview | OpenAPI 补充，以本附录为准 |
| `data.cleanup.approve` | staff-token | platform | 1 | approveDataCleanupJob | OpenAPI 补充，以本附录为准 |
| `data.cleanup.request` | staff-token | platform | 3 | listDataCleanupJobs, previewDataCleanup, submitDataCleanupJob | OpenAPI 补充，以本附录为准 |
| `dealer_portal.inquiry.manage` | staff-token | dealer | 2 | dealerPortalListInquiries, dealerPortalCreateInquiry | OpenAPI 补充，以本附录为准 |
| `dealer_portal.order.manage` | staff-token | dealer | 11 | dealerPortalListOrders, dealerPortalCreateOrder, dealerPortalGetOrder, dealerPortalListOrderLines, dealerPortalSubmitOrder, dealerPortalSignOrder, getDealerPortalDelivery, uploadDealerDeliveryEvidence, readDealerDeliveryEvidence, dealerPortalListInvoices, dealerPortalRequestOrderCancel | OpenAPI 补充，以本附录为准 |
| `dealer_portal.order.pay` | staff-token | dealer | 2 | dealerPortalListPayments, dealerPortalRegisterPayment | OpenAPI 补充，以本附录为准 |
| `dealer_portal.profile.submit` | staff-token | dealer | 1 | dealerPortalSubmitProfile | OpenAPI 补充，以本附录为准 |
| `dealer_portal.profile.view` | staff-token | dealer | 2 | dealerPortalMe, dealerPortalListContracts | OpenAPI 补充，以本附录为准 |
| `dealer_portal.return.request` | staff-token | dealer | 4 | dealerPortalListReturns, dealerPortalRequestReturn, getDealerPortalReturnSources, getDealerPortalReturnFinancials | OpenAPI 补充，以本附录为准 |
| `dealer_portal.stock.view` | staff-token | dealer | 1 | dealerPortalExternalStock | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.approve` | staff-token | project | 1 | decideDealerInquiryQuote | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.manage` | staff-token | project | 1 | createDealerInquiry | OpenAPI 补充，以本附录为准 |
| `dealer.inquiry.quote` | staff-token | project | 2 | quoteDealerInquiry, expireDealerInquiry | OpenAPI 补充，以本附录为准 |
| `dealer.invoice.manage` | staff-token | project | 4 | reverseDealerInvoice, requestDealerInvoice, issueDealerInvoice, recordDealerInvoiceReversalResult | OpenAPI 补充，以本附录为准 |
| `dealer.manage` | staff-token | project | 6 | createDealer, submitDealer, deactivateDealer, createDealerContract, designateDealerContractHandler, updateDealerContractTerms | OpenAPI 补充，以本附录为准 |
| `dealer.order.manage` | staff-token | project, project-merchant | 7 | createDealerOrderDraft, submitDealerOrder, registerDealerOrderPayment, recordDealerCounterpartyConfirmation, signDealerOrder, requestDealerOrderCancel, requestDealerDeliveryResolution | OpenAPI 补充，以本附录为准 |
| `dealer.order.payment.verify` | staff-token | project | 3 | verifyDealerOrderPayment, openDealerOrderRefundApproval, confirmDealerOverpaymentUse | OpenAPI 补充，以本附录为准 |
| `dealer.order.refund.execute` | staff-token | project | 1 | executeDealerOrderRefund | OpenAPI 补充，以本附录为准 |
| `dealer.order.review` | staff-token | project | 2 | reviewDealerOrder, decideDealerOrderCancel | OpenAPI 补充，以本附录为准 |
| `dealer.order.ship` | staff-token | project, project-merchant | 3 | shipDealerOrder, completeDealerDeliveryPhysical, shipDealerReplacement | OpenAPI 补充，以本附录为准 |
| `dealer.order.view` | staff-token | project, project-merchant | 8 | listDealerOrders, listDealerOverpayments, getDealerOrder, listDealerOrderLines, listDealerOrderPayments, listDealerCounterpartyConfirmations, listDealerOrderRefunds, getDealerDelivery | OpenAPI 补充，以本附录为准 |
| `dealer.portal_account.manage` | staff-token | project | 2 | listDealerPortalAccounts, createDealerPortalAccount | OpenAPI 补充，以本附录为准 |
| `dealer.price_catalog.approve` | staff-token | project | 1 | decideDealerPriceCatalog | OpenAPI 补充，以本附录为准 |
| `dealer.price_catalog.edit` | staff-token | project | 2 | createDealerPriceCatalogDraft, submitDealerPriceCatalog | OpenAPI 补充，以本附录为准 |
| `dealer.return.inspect` | staff-token | project | 2 | receiveDealerReturn, inspectDealerReturn | OpenAPI 补充，以本附录为准 |
| `dealer.return.manage` | staff-token | project | 2 | requestDealerReturn, confirmDealerReturnFreight | OpenAPI 补充，以本附录为准 |
| `dealer.return.refund.execute` | staff-token | project | 2 | recordDealerReturnRefundResult, executeDealerReturnRefunds | OpenAPI 补充，以本附录为准 |
| `dealer.return.review` | staff-token | project | 1 | decideDealerReturn | OpenAPI 补充，以本附录为准 |
| `dealer.return.view` | staff-token | project | 6 | listDealerReturns, getDealerReturn, listDealerReturnLines, listDealerInvoices, getDealerReturnSources, getDealerReturnFinancial | OpenAPI 补充，以本附录为准 |
| `dealer.review` | staff-token | project | 1 | reviewDealer | OpenAPI 补充，以本附录为准 |
| `dealer.view` | staff-token | project | 7 | listDealers, listDealerContracts, listDealerPriceCatalogs, listDealerExternalStock, listDealerPriceCatalogLines, listDealerInquiries, getDealerAdmission | OpenAPI 补充，以本附录为准 |
| `energy.meter.manage` | staff-token | platform | 10 | listEnergyMeters, createEnergyMeter, updateEnergyMeter, initializeEnergyMeter, replaceEnergyMeter, energyMeterHistory, listEnergyReviewSettings, configureEnergyReviewSettings, readEnergyMeterAttachment, retireEnergyMeter | OpenAPI 补充，以本附录为准 |
| `energy.reading.approve` | staff-token | platform | 2 | assignEnergyVerifier, listEnergyEscalations | OpenAPI 补充，以本附录为准 |
| `energy.reading.manage` | staff-token | platform | 10 | listEnergyReadings, recordEnergyReading, reviewEnergyReading, getEnergyMeterDifference, settleEnergyFee, settleEnergyFeeShortcut, listAbnormalReadings, verifyEnergyReading, listEnergyActualCharges, recordEnergyActualCharge | OpenAPI 补充，以本附录为准 |
| `entity.binding.manage` | staff-token | project-merchant | 4 | createBusinessEntity, associateBusinessEntityProject, createBusinessEntityBinding, changeBusinessEntityBinding | 已在权限矩阵正文出现 |
| `entity.binding.view` | staff-token | project-merchant | 2 | listBusinessEntities, listBusinessEntityBindings | 已在权限矩阵正文出现 |
| `evidence.attachment.create` | staff-token | merchant, project-object | 4 | createEvidenceAttachmentUploadIntent, bindEvidenceAttachment, completeEvidenceAttachmentUpload, uploadEvidenceAttachment | 已在权限矩阵正文出现 |
| `evidence.event.create` | staff-token | project-device | 1 | createEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.event.view` | staff-token | project-object | 1 | listEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.manual_backfill.policy.manage` | staff-token | project | 2 | getManualBackfillPolicy, saveManualBackfillPolicy | OpenAPI 补充，以本附录为准 |
| `evidence.review.create` | staff-token | project-object | 1 | createEvidenceReview | 已在权限矩阵正文出现 |
| `evidence.timeline.view` | staff-token | merchant, project-object | 3 | getEvidenceTimeline, getTraceabilityAutoCaptureRate, readEvidenceAttachmentFile | 已在权限矩阵正文出现 |
| `facility.alarm.manage` | staff-token | platform, project | 8 | saveFacilityAlarmResponsePolicy, listFacilityAlarms, listFacilityAlarmDuties, assignFacilityAlarmDuty, cancelFacilityAlarmDuty, listFacilityFaultRules, createFacilityFaultRule, retireFacilityFaultRule | OpenAPI 补充，以本附录为准 |
| `facility.alarm.take_over` | staff-token | platform | 3 | listMyFacilityAlarms, takeOverFacilityAlarm, recordFacilityAlarmDisposition | OpenAPI 补充，以本附录为准 |
| `facility.equipment.approve` | staff-token | platform | 1 | approveDeleteFacilityEquipment | OpenAPI 补充，以本附录为准 |
| `facility.equipment.manage` | staff-token | platform | 12 | listFacilityEquipment, createFacilityEquipment, listFacilityMaintenanceDueEquipment, updateFacilityEquipment, changeFacilityEquipmentStatus, recordFacilityEquipmentMaintenanceDone, createFacilityEquipmentScrapRequest, archiveFacilityEquipment, unarchiveFacilityEquipment, requestDeleteFacilityEquipment, listFacilityEquipmentScraps, completeFacilityEquipmentScrapDisposal | OpenAPI 补充，以本附录为准 |
| `facility.fault_rule.confirm` | staff-token | project | 1 | confirmFacilityFaultRule | OpenAPI 补充，以本附录为准 |
| `facility.inspection.manage` | staff-token | platform | 10 | listFacilityInspectionTemplates, createFacilityInspectionTemplate, listFacilityInspectionTasks, createFacilityInspectionTask, getFacilityInspectionTask, startFacilityInspectionTask, listFacilityInspectionRecords, submitFacilityInspectionRecord, recheckFacilityInspectionTask, confirmFacilityInspectionSupervisor | OpenAPI 补充，以本附录为准 |
| `facility.inspection.report_exception_assigned` | staff-token | self | 1 | reportInspectionException | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.accept` | staff-token | platform | 1 | acceptSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.create_assigned` | staff-token | self | 1 | createSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.review` | staff-token | platform | 1 | reviewSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.submit_assigned` | staff-token | self | 1 | submitSiteEntryReport | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.upload_completion_assigned` | staff-token | project-assignee, self | 2 | uploadSiteEntryCompletion, uploadSiteEntryAttachment | OpenAPI 补充，以本附录为准 |
| `facility.site_entry.view_assigned` | staff-token | project-assignee, self | 4 | listSiteEntryReports, getSiteEntryReport, listSiteEntryHistory, readSiteEntryAttachment | OpenAPI 补充，以本附录为准 |
| `facility.work_order.manage` | staff-token | platform | 26 | getComplaintFollowup, classifyComplaint, confirmComplaintLoop, listFacilityWorkOrders, createFacilityWorkOrder, listFacilityOverdueWorkOrders, getFacilityWorkOrder, assignFacilityWorkOrder, reassignFacilityWorkOrder, startFacilityWorkOrder, completeFacilityWorkOrder, acceptFacilityWorkOrder ... | OpenAPI 补充，以本附录为准 |
| `facility.work_order.view_assigned` | staff-token | platform | 1 | listWorkOrdersAssignedToMe | OpenAPI 补充，以本附录为准 |
| `finance.ar_ap.export` | staff-token | project-entity | 1 | exportArApItems | OpenAPI 补充，以本附录为准 |
| `finance.ar_ap.manage` | staff-token | platform, project, project-bar-table, project-entity, project-merchant | 9 | createArApItem, reviewTransferDisposition, writeOffBarOrderSuspension, generateContractRentBill, generateContractPeriodBills, recordFacilityEquipmentScrapFinance, allocatePurchasePrepayment, reviewPurchaseReturnFinance, postPurchasePrepaymentResolution | 已在权限矩阵正文出现 |
| `finance.ar_ap.view` | staff-token | platform, project-entity, project-merchant, project-order | 7 | listArApItems, listArApPayments, getInternalPayableExecution, listFinanceRefunds, listArOverdueDebtors, getFinanceRefund, queryFinanceRefund | 已在权限矩阵正文出现 |
| `finance.daily_close.lock` | staff-token | project-entity | 5 | createDailyClose, confirmDailyClose, lockDailyClose, markDailyCloseFinal, archiveDailyClose | 已在权限矩阵正文出现 |
| `finance.daily_close.manage` | staff-token | project-bar-table, project-entity | 5 | listDailyCloses, listDailyCloseSummaries, listDailyCloseAdjustments, resolveBarShift, witnessBarShift | OpenAPI 补充，以本附录为准 |
| `finance.deposit.manage` | staff-token | platform | 1 | recordDepositAction | OpenAPI 补充，以本附录为准 |
| `finance.dividend.manage` | staff-token | platform | 11 | createDividendPlan, updateDividendPlan, computeDividendStatements, confirmDividendLegalAmount, submitDividendStatement, approveDividendStatement, rejectDividendStatement, markDividendStatementPaid, releaseDividendReserve, recordDividendPeriodHold, liftDividendPeriodHold | OpenAPI 补充，以本附录为准 |
| `finance.dividend.view` | staff-token | merchant, platform | 5 | listDividendPlans, listDividendStatements, getDividendStatement, listDividendReserveReleases, listDividendPeriodHolds | OpenAPI 补充，以本附录为准 |
| `finance.entity_tax.manage` | staff-token | platform | 1 | declareEntityTax | OpenAPI 补充，以本附录为准 |
| `finance.entity_tax.view` | staff-token | platform | 1 | listEntityTaxDeclarations | OpenAPI 补充，以本附录为准 |
| `finance.inventory_variance.post` | staff-token | platform | 1 | postInventoryVariance | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.approve` | staff-token | project-entity | 1 | approveArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.request` | staff-token | project-entity | 1 | requestArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.merchant_expense.manage` | staff-token | merchant, platform | 3 | createMerchantExpenseEntry, reverseMerchantExpenseEntry, syncMerchantExpenseAuto | OpenAPI 补充，以本附录为准 |
| `finance.merchant_expense.view` | staff-token | merchant | 3 | listMerchantExpenses, summarizeMerchantExpenses, listMerchantExpensePeriodLocks | OpenAPI 补充，以本附录为准 |
| `finance.payment_intent.create` | staff-token | project-bar-table, project-order | 3 | createBarBillPayIntent, createPaymentIntent, createTicketOrderPayIntent | 已在权限矩阵正文出现 |
| `finance.payment.register` | staff-token | platform, project-bar-table, project-entity, project-merchant, project-order | 8 | settleBarBill, requestBarBillStoredValueSmsCode, createArApPayment, confirmOfflinePayment, initiateInternalPayment, recordInternalPaymentFailure, postponeInternalPaymentForFunds, confirmTicketOrderPayment | 已在权限矩阵正文出现 |
| `finance.payroll.payment.register` | staff-token | project-entity | 3 | preparePayrollBankPayment, submitPayrollBankPaymentFact, confirmPayrollBankPaymentResult | OpenAPI 补充，以本附录为准 |
| `finance.payroll.view` | staff-token | project-entity | 1 | getPayrollBankPaymentContext | OpenAPI 补充，以本附录为准 |
| `finance.period_close.execute` | staff-token | project | 2 | listFinancePeriodCloses, closeFinancePeriod | OpenAPI 补充，以本附录为准 |
| `finance.points_recovery.view` | staff-token | project-merchant | 1 | getPointsRecoveryDetail | OpenAPI 补充，以本附录为准 |
| `finance.points_recovery.waive` | staff-token | project-merchant | 1 | waivePointsRecovery | OpenAPI 补充，以本附录为准 |
| `finance.property_fee.manage` | staff-token | platform | 3 | listPropertyFeeRates, createPropertyFeeRate, billPropertyFee | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.difference.create` | staff-token | project-entity | 1 | createFinanceReconciliationDifference | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.policy.manage` | staff-token | project | 2 | listFinanceReconciliationStageLimits, saveFinanceReconciliationStageLimit | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.resolve` | staff-token | project-entity | 2 | closeFinanceReconciliationDifference, identifyFinanceReconciliationDifference | OpenAPI 补充，以本附录为准 |
| `finance.reconciliation.view` | staff-token | project-entity | 2 | listReconciliationBatches, listReconciliationDifferences | 已在权限矩阵正文出现 |
| `finance.refund.create` | staff-token | project-order | 4 | createFinanceRefund, retryFinanceRefund, recordFinanceRefundExecution, confirmFinanceRefundResult | 已在权限矩阵正文出现 |
| `finance.settlement.view` | staff-token | platform | 2 | listSettlementBills, listSettlementTransactions | OpenAPI 补充，以本附录为准 |
| `finance.turnover_rent.bill` | staff-token | platform | 2 | billTurnoverRent, createTurnoverRentRule | OpenAPI 补充，以本附录为准 |
| `floor_plan.import.manage` | staff-token | project-space | 2 | createFloorPlanImport, publishFloorPlanImport | OpenAPI 补充，以本附录为准 |
| `floor_plan.import.view` | staff-token | project-space | 2 | listFloorPlanImports, getFloorPlanImportDetails | OpenAPI 补充，以本附录为准 |
| `floor_plan.manage` | staff-token | -, project-space | 2 | createFloorPlan, saveFloorPlanShape | OpenAPI 补充，以本附录为准 |
| `floor_plan.view` | staff-token | project-space | 2 | listFloorPlans, listFloorPlanShapes | OpenAPI 补充，以本附录为准 |
| `governance.project.manage` | staff-token | project | 1 | createProject | 已在权限矩阵正文出现 |
| `governance.project.view` | staff-token | project | 1 | listProjects | 已在权限矩阵正文出现 |
| `governance.space.manage` | staff-token | platform, project | 7 | createSpace, saveVenueBusinessHours, createSpaceType, deleteSpaceType, archiveSpaceType, unarchiveSpaceType, requestDeleteSpaceType | 已在权限矩阵正文出现 |
| `governance.space.view` | staff-token | platform, project, project-merchant-space | 3 | listSpaces, listVenueBusinessHours, listSpaceTypes | 已在权限矩阵正文出现 |
| `governance.supervisor_review.approve` | staff-token | project-entity | 2 | approveSupervisorReview, forwardSupervisorReview | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.manage` | staff-token | project-entity | 2 | evaluateSupervisorReview, regenerateSupervisorReviewChain | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.view` | staff-token | platform, project, project-entity, project-merchant | 7 | listApprovalDeadlines, listSupervisorReviewPolicies, listPendingSupervisorReviews, getSupervisorReviewChain, readSupervisorReviewOriginal, readAmusementBusinessDayConfigOriginal, readBarBusinessDayConfigOriginal | OpenAPI 补充，以本附录为准 |
| `integration.channel_store_map.manage` | staff-token | platform | 4 | saveIntegrationChannelStoreMap, saveIntegrationChannelTicketTypeMap, archiveIntegrationChannelTicketTypeMap, archiveIntegrationChannelStoreMap | OpenAPI 补充，以本附录为准 |
| `integration.channel_store_map.view` | staff-token | platform, project-merchant | 2 | listIntegrationChannelStoreMaps, listIntegrationChannelTicketTypeMaps | OpenAPI 补充，以本附录为准 |
| `integration.compensation.manage` | staff-token | platform | 2 | createCompensationTask, resolveCompensationTask | OpenAPI 补充，以本附录为准 |
| `integration.order_sync.manage` | staff-token | platform | 1 | createOrderSync | OpenAPI 补充，以本附录为准 |
| `integration.order_sync.view` | staff-token | platform | 1 | listIntegrationUnattributedOrderSyncs | OpenAPI 补充，以本附录为准 |
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
| `inventory.handover.scan` | staff-token | project-warehouse-bar | 8 | scanTraceCodeHandover, listTraceHandovers, createTraceHandover, getTraceHandover, backfillTraceHandoverSegment, scanTraceSegment, reportRobotSegment, listTraceCodeOutboundOrders | OpenAPI 补充，以本附录为准 |
| `inventory.high_value.review` | staff-token | project, project-bar-customer, project-warehouse-bar | 4 | markInventoryItemHighValue, listHighValueReviews, reviewHighValue, approveSavedBottleRemedy | OpenAPI 补充，以本附录为准 |
| `inventory.purchase_request.manage` | staff-token | project-merchant | 11 | createPurchaseRequest, approvePurchaseRequest, orderPurchaseRequest, arrivePurchaseRequest, receivePurchaseRequest, getPurchaseSuggestions, resolvePurchaseQuantity, registerPurchaseConsignmentSale, requestPurchaseReturn, handoverPurchaseReturn, requestPurchasePrepaymentResolution | 已在权限矩阵正文出现 |
| `inventory.purchase_request.view` | staff-token | project-merchant | 3 | listPurchaseRequests, getPurchaseWorkbenchOptions, getPurchaseWorkflowDetail | 已在权限矩阵正文出现 |
| `inventory.purchase.approve` | staff-token | project-merchant | 1 | confirmPurchaseConsignmentSale | OpenAPI 补充，以本附录为准 |
| `inventory.risk_alert.view` | staff-token | warehouse | 1 | listInventoryRiskAlerts | OpenAPI 补充，以本附录为准 |
| `inventory.risk.scan` | staff-token | warehouse | 1 | scanInventoryRiskAlerts | OpenAPI 补充，以本附录为准 |
| `inventory.stock.adjust` | staff-token | project-merchant, project-warehouse | 3 | createStockAdjustment, requestQuarantineDisposition, completeQuarantineDisposition | 已在权限矩阵正文出现 |
| `inventory.stock.batch.manage` | staff-token | warehouse | 1 | createInventoryStockBatch | OpenAPI 补充，以本附录为准 |
| `inventory.stock.move` | staff-token | project, project-warehouse | 9 | createInventoryItem, updateInventoryItemSafetyStock, createStockIn, createStockMovement, transferStockMovement, createTypedStockMovement, createDamageStockAdjustment, createLossStockAdjustment, createWasteStockAdjustment | 已在权限矩阵正文出现 |
| `inventory.stock.view` | staff-token | project, project-merchant, project-warehouse, warehouse | 8 | listInventoryItems, listStockBalances, listStockMovements, listInventoryStockBatches, listBarConsumptionStats, listQuarantineWarehouses, listInventoryQuarantine, listQuarantineDispositions | 已在权限矩阵正文出现 |
| `inventory.stocktake.create` | staff-token | project | 1 | createInventoryStocktake | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.variance_record` | staff-token | project | 1 | recordInventoryStocktakeVariance | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.variance_review` | staff-token | project | 1 | reviewInventoryStocktakeVariance | OpenAPI 补充，以本附录为准 |
| `inventory.stocktake.view` | staff-token | project | 2 | listInventoryStocktakes, getInventoryStocktake | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.manage` | staff-token | project | 1 | createInventorySupplier | 已在权限矩阵正文出现 |
| `inventory.supplier.qualification.manage` | staff-token | supplier | 2 | listSupplierQualifications, createSupplierQualification | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.reconcile` | staff-token | supplier | 2 | listSupplierReconciliations, createSupplierReconciliation | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.settlement.manage` | staff-token | supplier | 2 | listSupplierSettlementMethods, createSupplierSettlementMethod | OpenAPI 补充，以本附录为准 |
| `inventory.supplier.view` | staff-token | project | 1 | listInventorySuppliers | 已在权限矩阵正文出现 |
| `inventory.supply_contract.manage` | staff-token | project-merchant | 5 | listSupplyContracts, createSupplyContract, approveSupplyContract, getSupplyContractTerms, saveSupplyContractTerms | OpenAPI 补充，以本附录为准 |
| `inventory.supply_entity.manage` | staff-token | platform | 1 | createSupplyEntity | OpenAPI 补充，以本附录为准 |
| `inventory.supply_entity.view` | staff-token | platform | 1 | listSupplyEntities | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.print` | staff-token | project | 1 | printInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.register` | staff-token | project | 1 | registerInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.trace_code.view` | staff-token | project | 2 | listInventoryTraceCodes, getInventoryTraceCode | OpenAPI 补充，以本附录为准 |
| `inventory.transfer_order.manage` | staff-token | project | 14 | createTransferOrder, shipTransferOrder, returnTransferOrder, cancelTransferOrder, submitTransferOrder, editTransferOrderDraft, supplierReviewTransferOrder, pickTransferOrder, deliverTransferOrder, receiveTransferOrder, registerTransferDisposition, registerTransferUnreturnable ... | OpenAPI 补充，以本附录为准 |
| `inventory.transfer_order.view` | staff-token | project | 3 | listTransferOrders, getTransferOrder, getTransferOrderFulfillment | OpenAPI 补充，以本附录为准 |
| `inventory.warehouse.manage` | staff-token | project-warehouse-bar, warehouse | 5 | listWarehouseLocations, createWarehouseLocation, listWarehouses, createWarehouse, setWarehousePourDevice | OpenAPI 补充，以本附录为准 |
| `leasing.brand.blacklist` | staff-token | platform | 1 | blacklistLeasingBrand | OpenAPI 补充，以本附录为准 |
| `leasing.brand.blacklist_exception` | staff-token | platform | 1 | createLeasingBlacklistException | OpenAPI 补充，以本附录为准 |
| `leasing.brand.manage` | staff-token | platform | 2 | listLeasingBrands, createLeasingBrand | OpenAPI 补充，以本附录为准 |
| `leasing.convert` | staff-token | platform | 1 | signLeasingIntent | OpenAPI 补充，以本附录为准 |
| `leasing.intent.manage` | staff-token | platform | 3 | listLeasingIntents, createLeasingIntent, advanceLeasingIntent | OpenAPI 补充，以本附录为准 |
| `leasing.lead.manage` | staff-token | platform | 9 | listLeasingLeads, createLeasingLead, listLeasingLeadsFollowUpDue, assignLeasingLead, updateLeasingLeadStatus, getLeasingLeadContactPhone, listLeasingBlacklistExceptions, listLeasingFollowUps, createLeasingFollowUp | OpenAPI 补充，以本附录为准 |
| `mdm.device.manage` | staff-token | -, project-device | 3 | enrollMdmDevice, reportMdmPolicyViolation, detectMdmViolations | OpenAPI 补充，以本附录为准 |
| `mdm.policy.manage` | staff-token | project-device | 4 | listMdmAppWhitelist, upsertMdmAppWhitelist, listMdmSiteWhitelist, upsertMdmSiteWhitelist | OpenAPI 补充，以本附录为准 |
| `mdm.violation.handle` | staff-token | - | 1 | handleMdmViolation | OpenAPI 补充，以本附录为准 |
| `mdm.violation.view` | staff-token | project-device | 1 | listMdmPolicyViolations | OpenAPI 补充，以本附录为准 |
| `media.performance.contract.manage` | staff-token | project | 4 | createMediaPerformanceShow, updateMediaPerformanceShow, lockMediaPerformanceShow, createMediaPerformanceShowVersion | OpenAPI 补充，以本附录为准 |
| `media.performance.dispute.resolve` | staff-token | project | 1 | resolveMediaPerformanceDispute | OpenAPI 补充，以本附录为准 |
| `media.performance.fulfillment.confirm` | staff-token | project | 2 | confirmMediaPerformanceFulfillment, disputeMediaPerformanceFulfillment | OpenAPI 补充，以本附录为准 |
| `media.performance.fulfillment.record` | staff-token | project | 1 | recordMediaPerformanceFulfillment | OpenAPI 补充，以本附录为准 |
| `media.performance.settle` | staff-token | project | 2 | settleMediaPerformanceShow, adjustMediaPerformanceSettlement | OpenAPI 补充，以本附录为准 |
| `media.performance.view` | staff-token | project | 3 | listMediaPerformanceShows, countMediaPerformancePendingSettlements, getMediaPerformanceShow | OpenAPI 补充，以本附录为准 |
| `merchant.billing.confirm` | staff-token | - | 1 | confirmMerchantBill | OpenAPI 补充，以本附录为准 |
| `merchant.billing.dispute.submit` | staff-token | - | 1 | submitMerchantBillDispute | OpenAPI 补充，以本附录为准 |
| `merchant.billing.dunning.record` | staff-token | - | 1 | recordMerchantBillDunning | OpenAPI 补充，以本附录为准 |
| `merchant.billing.manage` | staff-token | -, project-merchant | 5 | createMerchantBill, listMerchantFeeNotificationRecipients, saveMerchantFeeNotificationRecipient, getMerchantFeeNotificationRecipient, deactivateMerchantFeeNotificationRecipient | OpenAPI 补充，以本附录为准 |
| `merchant.billing.view` | staff-token | merchant, project-merchant | 3 | listMerchantBills, getMerchantPortalFeeStatement, getMerchantPortalUnreadBills | OpenAPI 补充，以本附录为准 |
| `merchant.penalty.bounds.manage` | staff-token | project | 1 | saveMerchantPenaltyBounds | OpenAPI 补充，以本附录为准 |
| `merchant.penalty.record` | staff-token | project | 2 | listMerchantPenalties, recordMerchantPenalty | OpenAPI 补充，以本附录为准 |
| `merchant.portal.card_exit_notice.publish` | staff-token | merchant | 2 | listMyMerchantCardExitNotices, publishMerchantCardExitNotice | OpenAPI 补充，以本附录为准 |
| `merchant.portal.payee_account.approve` | staff-token | merchant | 1 | approveMerchantPortalPayeeAccountChange | OpenAPI 补充，以本附录为准 |
| `merchant.portal.settlement.dispute` | staff-token | merchant | 2 | submitMerchantPortalSettlementDispute, rejectMerchantPortalSettlementResolution | OpenAPI 补充，以本附录为准 |
| `merchant.portal.settlement.view` | staff-token | merchant | 3 | listMerchantPortalTicketSettlements, listMerchantPortalNotices, listMerchantPortalSettlements | OpenAPI 补充，以本附录为准 |
| `merchant.profile.approve` | staff-token | project-merchant | 1 | approveDeleteMerchant | OpenAPI 补充，以本附录为准 |
| `merchant.profile.manage` | staff-token | project, project-merchant | 11 | createMerchant, updateMerchant, assignMerchantProject, archiveMerchant, unarchiveMerchant, requestDeleteMerchant, configureMerchantPendingClearAlert, configureMerchantTableTransferLimit, configureMerchantStaffBookingNoShowLimit, configureMerchantPourLossRate, requestMerchantOperatingProject | 已在权限矩阵正文出现 |
| `merchant.profile.view` | staff-token | project-merchant | 6 | listMerchants, getMerchantPendingClearAlert, getMerchantTableTransferLimit, getMerchantStaffBookingNoShowLimit, getMerchantPourLossRate, getMerchantOperatingProjects | 已在权限矩阵正文出现 |
| `merchant.service_work_order.approve` | staff-token | project-merchant | 1 | approveDeleteMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.assign` | staff-token | platform, project-merchant | 5 | listMerchantServiceWorkOrderPolicies, saveMerchantServiceWorkOrderPolicy, saveMerchantServiceWorkOrderCloseMethod, assignMerchantServiceWorkOrder, listMerchantServiceWorkOrderOwnerCandidates | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.close` | staff-token | project-merchant | 5 | closeMerchantServiceWorkOrder, cancelMerchantServiceWorkOrderByStaff, archiveMerchantServiceWorkOrder, unarchiveMerchantServiceWorkOrder, requestDeleteMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.create` | staff-token | merchant, project-merchant | 3 | createMerchantServiceWorkOrder, cancelMerchantServiceWorkOrder, confirmMerchantServiceWorkOrderByMerchant | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.process` | staff-token | platform, project-merchant | 6 | listMyMerchantServiceWorkOrders, addMerchantServiceWorkOrderAction, getMerchantServiceWorkOrderMaterials, issueMerchantServiceWorkOrderMaterial, markMerchantServiceWorkOrderCostBearer, disputeMerchantServiceWorkOrderCostBearer | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.view` | staff-token | platform, project-merchant | 2 | listMerchantServiceWorkOrders, listMerchantServiceWorkOrderCloseMethods | OpenAPI 补充，以本附录为准 |
| `not_applicable` | customer-token, device-credential, open-app-credential, public, staff-token, webhook-signature | customer, customer-project-bar, open-endpoint, platform, project-device, public-project, public-project-bar, self | 77 | getOpenEventResult, receiveOpenWebhook, getCustomerSession, listCustomerBarReservationOptions, listMyCustomerBarReservations, createCustomerBarReservation, confirmCustomerBarInvitation, getCustomerBarInvitation, previewCustomerBarInvitation, removeCustomerBarInvitationMember, getCustomerBarInvitationEntryCode, createCustomerConsent ... | 已在权限矩阵正文出现 |
| `ops.production_readiness.check` | staff-token | project | 1 | createProductionReadinessCheck | 已在权限矩阵正文出现 |
| `ops.production_readiness.manage` | staff-token | project | 1 | updateProductionReadinessItem | 已在权限矩阵正文出现 |
| `ops.production_readiness.view` | staff-token | project | 1 | listProductionReadinessItems | 已在权限矩阵正文出现 |
| `parking.config.manage` | staff-token | project | 1 | saveParkingPolicy | OpenAPI 补充，以本附录为准 |
| `parking.ops.view` | staff-token | project | 3 | getParkingPolicy, getParkingOverview, listParkingCredits | OpenAPI 补充，以本附录为准 |
| `parking.release.record` | staff-token | project | 2 | listParkingManualReleases, recordParkingManualRelease | OpenAPI 补充，以本附录为准 |
| `parking.vehicle.manage` | staff-token | project | 6 | listParkingMonthlyVehicles, registerParkingMonthlyVehicle, revokeParkingMonthlyVehicle, listParkingStaffVehicles, registerParkingStaffVehicle, revokeParkingStaffVehicle | OpenAPI 补充，以本附录为准 |
| `partner.org.manage` | staff-token | platform | 3 | createPartnerOrg, bindUserToPartnerOrg, unbindUserFromPartnerOrg | OpenAPI 补充，以本附录为准 |
| `partner.org.view` | staff-token | platform | 1 | listPartnerOrgs | OpenAPI 补充，以本附录为准 |
| `partner.subordinate.manage` | staff-token | self | 2 | createPartnerSubordinate, disablePartnerSubordinate | OpenAPI 补充，以本附录为准 |
| `partner.subordinate.view` | staff-token | self | 2 | listPartnerSubordinates, listSubordinateCompletion | OpenAPI 补充，以本附录为准 |
| `platform.notification_window.manage` | staff-token | platform | 1 | upsertNotificationWindow | OpenAPI 补充，以本附录为准 |
| `platform.sensitive_operation_range.manage` | staff-token | project | 2 | getProjectSensitiveOperationRange, saveProjectSensitiveOperationRange | OpenAPI 补充，以本附录为准 |
| `public` | public | customer, none, public-project, self | 20 | login, issueMasterAuthChallenge, masterAuthLogin, customerPhoneLogin, requestCustomerLoginSmsCode, registerCustomerMember, getCustomerTerms, listCustomerEffectiveTerminalSwitches, customerLogout, listCustomerTicketProducts, getSystemHealth, listCustomerBarScanMenu ... | 已在权限矩阵正文出现 |
| `refund.authority.manage` | staff-token | project | 2 | upsertRefundAuthorityLimit, upsertRefundAutoRule | OpenAPI 补充，以本附录为准 |
| `refund.authority.view` | staff-token | project | 2 | listRefundAuthorityLimits, listRefundAutoRules | OpenAPI 补充，以本附录为准 |
| `risk_alert.claim` | staff-token | platform | 1 | claimRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.dismiss` | staff-token | platform | 1 | dismissRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.escalate` | staff-token | platform | 1 | escalateRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.report` | staff-token | platform | 1 | reportRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.resolve` | staff-token | platform | 1 | resolveEvidenceRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.transfer` | staff-token | platform | 1 | transferRiskAlert | OpenAPI 补充，以本附录为准 |
| `risk_alert.view` | staff-token | platform | 2 | searchRiskAlerts, getRiskAlertDetail | 已在权限矩阵正文出现 |
| `security.watermark.trace` | staff-token | project | 2 | traceWatermark, lookupExportPassword | OpenAPI 补充，以本附录为准 |
| `service.catalog.approve` | staff-token | platform | 2 | activateServiceCatalogFeeDraft, approveDeleteServiceCatalogItem | OpenAPI 补充，以本附录为准 |
| `service.catalog.manage` | staff-token | platform | 8 | upsertServiceCatalogItem, draftServiceCatalogFeeChange, archiveServiceCatalogItem, unarchiveServiceCatalogItem, requestDeleteServiceCatalogItem, listServiceCatalogAll, listServiceOrders, setServicePayConfig | OpenAPI 补充，以本附录为准 |
| `service.order.create` | staff-token | platform | 7 | listServiceCatalog, createServiceOrder, completeServiceOrder, cancelServiceOrder, serviceOrderPaymentCode, markServiceOrderPaid, getServicePayConfig | OpenAPI 补充，以本附录为准 |
| `service.order.perform` | staff-token | platform | 5 | setMyServicePaymentCode, myServicePaymentCode, listMyServiceOrders, acceptServiceOrder, myServiceStats | OpenAPI 补充，以本附录为准 |
| `settlement.bill.post` | staff-token | platform | 1 | settlementPostBill | OpenAPI 补充，以本附录为准 |
| `settlement.bill.special_approve` | staff-token | platform | 2 | settlementConfigApprovePassIncentive, settlementConfigRejectPassIncentive | OpenAPI 补充，以本附录为准 |
| `settlement.config.manage` | staff-token | platform, project | 24 | settlementConfigUpsertRetention, settlementConfigUpsertDistributionMethod, settlementConfigUpsertMerchantRate, settlementConfigUpsertComprehensiveServiceFee, settlementConfigUpsertComboDiscount, settlementConfigUpsertChannelFee, settlementConfigUpsertCommissionRate, settlementConfigUpsertPointsAccrualRate, settlementConfigUpsertRankTiers, settlementConfigUpsertRevenueStreamAttribution, settlementConfigActivateRankTiers, settlementConfigSaveDistributionScheme ... | OpenAPI 补充，以本附录为准 |
| `settlement.config.view` | staff-token | platform, project | 5 | settlementConfigOverview, settlementConfigDistributionScheme, listInternalPayDayRules, getSettlementObjectionPeriod, settlementConfigListInternalPriceItems | OpenAPI 补充，以本附录为准 |
| `settlement.invoice.export` | staff-token | platform | 1 | exportSettlementInvoiceItems | OpenAPI 补充，以本附录为准 |
| `settlement.invoice.manage` | staff-token | platform | 2 | generateSettlementInvoiceItems, changeSettlementInvoiceItemStatus | OpenAPI 补充，以本附录为准 |
| `settlement.invoice.view` | staff-token | platform | 2 | listSettlementInvoiceItems, settlementInvoiceSummary | OpenAPI 补充，以本附录为准 |
| `staff.attendance.punch` | staff-token | platform, self | 4 | punchStaffAttendance, issueStaffPhoneTimeAnchor, recordStaffPhoneReceipt, getCurrentStaffWorkPhoneContext | OpenAPI 补充，以本附录为准 |
| `staff.attendance.view` | staff-token | platform | 1 | listStaffAttendance | OpenAPI 补充，以本附录为准 |
| `staff.device.bind` | device-credential, staff-token | project | 2 | bindStaffDeviceCredential, reportStaffDeviceIntegrity | OpenAPI 补充，以本附录为准 |
| `staff.device.review` | staff-token | project | 1 | approveStaffDeviceRebind | OpenAPI 补充，以本附录为准 |
| `staff.device.revoke` | staff-token | project | 1 | revokeStaffDeviceCredential | OpenAPI 补充，以本附录为准 |
| `staff.leave.submit` | staff-token | platform | 3 | submitStaffLeave, listMyStaffLeaves, requestStaffLeaveRevoke | OpenAPI 补充，以本附录为准 |
| `staff.notice.view` | staff-token | project | 2 | listEmployeeNotices, acknowledgeEmployeeNotice | OpenAPI 补充，以本附录为准 |
| `staff.payroll.manage` | staff-token | platform, project | 31 | listPayrollTaxBrackets, setStaffBaseSalary, addStaffPayrollAdjustment, addStaffCommission, closeStaffPayrollMonth, closeStaffCommissionSettlement, listStaffCommissionSettlements, listStaffPayroll, getStaffPenaltyCap, requestStaffPenaltyCap, listPayrollTaxVersions, listPayrollTaxAssessments ... | OpenAPI 补充，以本附录为准 |
| `staff.payroll.view_mine` | staff-token | self | 7 | listStaffAdjustmentAppeals, submitStaffAdjustmentAppeal, listMyStaffPayroll, listMyStaffPayrollCommissions, staffPayrollScoreMine, staffPayrollScoreAppeal, staffPayrollDetailMine | OpenAPI 补充，以本附录为准 |
| `staff.profile.approve` | staff-token | project | 1 | approveDeleteStaffProfile | OpenAPI 补充，以本附录为准 |
| `staff.profile.review` | staff-token | project | 8 | reviewStaffRegistration, setStaffSalesPosition, requestDeleteStaffProfile, createStaffDispatch, endStaffDispatch, setStaffEmploymentStatus, getStaffBusinessHandover, assignStaffBusinessHandover | OpenAPI 补充，以本附录为准 |
| `staff.profile.self` | staff-token | self | 2 | getMyStaffProfile, issueStaffLockerScanToken | OpenAPI 补充，以本附录为准 |
| `staff.profile.submit` | staff-token | project | 3 | submitStaffRegistration, archiveStaffProfile, unarchiveStaffProfile | OpenAPI 补充，以本附录为准 |
| `staff.profile.view` | staff-token | project | 2 | listStaffProfiles, listStaffDispatches | OpenAPI 补充，以本附录为准 |
| `staff.shift.manage` | staff-token | platform, project, project-merchant, self-project | 14 | listStaffShifts, upsertStaffShift, batchUpsertStaffShifts, listStaffLeaves, reviewStaffLeave, revokeStaffLeave, getStaffShiftVisibility, setStaffShiftVisibility, listStaffShiftHistory, coverStaffShift, listStaffShiftCovers, submitStaffAttendanceReceiptReview ... | OpenAPI 补充，以本附录为准 |
| `staff.shift.view` | staff-token | platform, self-project | 5 | listMyStaffShifts, listMyStaffAttendance, myStaffAttendanceMonthView, listStaffPhoneReceipts, listStaffAttendanceReceiptReviews | OpenAPI 补充，以本附录为准 |
| `staff.tax_bracket.manage` | staff-token | platform, project | 5 | upsertPayrollTaxBracket, draftPayrollTaxVersion, submitPayrollTaxVersion, draftPayrollTaxAssessment, submitPayrollTaxAssessment | OpenAPI 补充，以本附录为准 |
| `system.version.view` | staff-token | project | 1 | getSystemVersion | 已在权限矩阵正文出现 |
| `terminal.device.bind` | staff-token | project, project-device | 3 | bindDevice, returnTerminalDevice, registerTerminalDeviceSigningKey | 已在权限矩阵正文出现 |
| `terminal.device.lifecycle` | staff-token | -, project-device | 9 | updateTerminalDeviceMerchant, markTerminalDeviceFound, markTerminalDeviceLost, markTerminalDeviceRepair, markTerminalDeviceScrap, markTerminalDeviceArchived, registerEntryDevice, completeTerminalDeviceRepair, changeTerminalDeviceResponsibleStaff | OpenAPI 补充，以本附录为准 |
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
| `ticket.entitlement.manage` | staff-token | project | 5 | createEntitlementTemplate, archiveEntitlementTemplate, unarchiveEntitlementTemplate, requestDeleteEntitlementTemplate, createPartnerVoucherConfig | 已在权限矩阵正文出现 |
| `ticket.entitlement.view` | staff-token | project | 1 | listEntitlementTemplates | 已在权限矩阵正文出现 |
| `ticket.order.create` | staff-token | platform, project-channel | 3 | createTicketOrder, cancelTicketOrder, requestKioskBindCode | 已在权限矩阵正文出现 |
| `ticket.order.read` | staff-token | project | 1 | listTicketOrders | OpenAPI 补充，以本附录为准 |
| `ticket.points.billing.manage` | staff-token | platform | 4 | submitTicketPointsBillingRule, submitTicketPointsCalendar, submitTicketPointsOutage, confirmTicketPointsBillUnchanged | OpenAPI 补充，以本附录为准 |
| `ticket.points.billing.view` | staff-token | platform, project-merchant | 8 | listTicketPointsBillingRules, listTicketPointsCalendars, listTicketPointsOutages, listTicketPointsBills, listTicketPointsBillItems, listTicketPointsBillObjections, submitTicketPointsBillObjection, listTicketPointsBillScheduleAdjustments | OpenAPI 补充，以本附录为准 |
| `ticket.price_rule.approve` | staff-token | project-channel | 1 | approveDeleteTicketPriceRule | OpenAPI 补充，以本附录为准 |
| `ticket.price_variant.manage` | staff-token | platform, project-channel | 5 | createTicketPriceRule, archiveTicketPriceRule, unarchiveTicketPriceRule, requestDeleteTicketPriceRule, createTicketDistributionMerchantRate | 已在权限矩阵正文出现 |
| `ticket.price_variant.view` | staff-token | project-channel | 1 | listTicketPriceRules | 已在权限矩阵正文出现 |
| `ticket.product.manage` | staff-token | platform, project | 9 | updateTicketQrRefreshConfig, createTicketType, bindTicketTypeEntitlement, publishTicketType, archiveTicketType, unarchiveTicketType, requestDeleteTicketType, updateTicketTypePricing, saveTicketExpiryReminderPolicy | 已在权限矩阵正文出现 |
| `ticket.product.view` | staff-token | platform, project | 5 | getTicketQrRefreshConfig, listTicketTypes, listTicketTypeGiftCoverage, ticketTypePriceCheck, getTicketExpiryReminderPolicy | 已在权限矩阵正文出现 |
| `ticket.refund.approve` | staff-token | project-order | 4 | confirmTicketRefundPayout, approveTicketRefund, approveTicketSafetyEvent, rejectTicketRefund | 已在权限矩阵正文出现 |
| `ticket.refund.request` | staff-token | project-order | 2 | requestTicketRefund, requestTicketSafetyEvent | 已在权限矩阵正文出现 |
| `ticket.refund.view` | staff-token | -, project-order | 3 | listTicketRefunds, listTicketSafetyEvents, listTicketRefundLines | OpenAPI 补充，以本附录为准 |
| `ticket.reward.config.manage` | staff-token | project-merchant | 2 | submitTicketRewardConfig, publishTicketRewardConfig | OpenAPI 补充，以本附录为准 |
| `ticket.reward.config.view` | staff-token | project-merchant | 1 | listTicketRewardConfigs | OpenAPI 补充，以本附录为准 |
| `ticket.student_cert.review` | staff-token | platform | 4 | listStudentCertifications, approveStudentCertification, rejectStudentCertification, readStudentCertificationFile | OpenAPI 补充，以本附录为准 |
| `ticket.ticket.issue` | staff-token | project-order | 1 | issueTicket | 已在权限矩阵正文出现 |
| `ticket.transfer.read` | staff-token | project | 1 | listTicketTransfers | OpenAPI 补充，以本附录为准 |
| `ticket.type.approve` | staff-token | project | 1 | approveDeleteTicketType | OpenAPI 补充，以本附录为准 |
| `ticket.verify.anomaly.resolve` | staff-token | project-merchant | 1 | resolveTicketVerificationAnomaly | OpenAPI 补充，以本附录为准 |
| `ticket.verify.reverse` | staff-token | project-point | 1 | reverseTicketVerification | OpenAPI 补充，以本附录为准 |
| `ticket.verify.view` | staff-token | project | 2 | listTicketInstances, listIssuedEntitlements | 已在权限矩阵正文出现 |
| `verification.execute` | staff-token | project, project-merchant, project-point | 4 | importPartnerVoucherRedemption, createTicketVerification, raiseTicketVerificationAnomaly, previewTicketVerification | 已在权限矩阵正文出现 |
| `verification.view` | staff-token | project-merchant | 1 | listTicketVerifications | 已在权限矩阵正文出现 |
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
| `access.grant_policy.change_failed` | failure | `updateAccessGrantPolicy` | `POST` | `/api/v1/access/grant-policy` | AUTH-账号权限 | `admission.policy.manage` | 接口级事件，以本附录为准 |
| `access.grant_policy.changed` | success | `updateAccessGrantPolicy` | `POST` | `/api/v1/access/grant-policy` | AUTH-账号权限 | `admission.policy.manage` | 接口级事件，以本附录为准 |
| `access.grant_request.applied` | success | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.apply` | 接口级事件，以本附录为准 |
| `access.grant_request.apply_failed` | failure | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.apply` | 接口级事件，以本附录为准 |
| `access.grant_request.review_failed` | failure | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | AUTH-账号权限 | `access.grant_request.review` | 接口级事件，以本附录为准 |
| `access.grant_request.reviewed` | success | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | AUTH-账号权限 | `access.grant_request.review` | 接口级事件，以本附录为准 |
| `access.grant_request.viewed` | success | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `access.org_hierarchy.superior_set` | success | `setOrgDirectSuperior` | `PUT` | `/api/v1/access/org-hierarchy/{user_id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.archive` | success | `changePositionRoleTemplateLifecycle` | `POST` | `/api/v1/access/position-role-templates/{id}/lifecycle` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.lifecycle_failed` | failure | `changePositionRoleTemplateLifecycle` | `POST` | `/api/v1/access/position-role-templates/{id}/lifecycle` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.save_failed` | failure | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.save_failed` | failure | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.saved` | success | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.saved` | success | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.position_role_template.viewed` | success | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role_project_options.viewed` | success | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `access.role.assign_failed` | failure | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.assigned` | success | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.unassign_failed` | failure | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.role.unassigned` | success | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.staff_level.assign_failed` | failure | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.assigned` | success | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.viewed` | success | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
| `access.user_permission_override.remove` | success | `removeUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides/remove` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_permission_override.remove_failed` | failure | `removeUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides/remove` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_permission_override.set` | success | `setUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_permission_override.set_failed` | failure | `setUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_position.assign_failed` | failure | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_position.assigned` | success | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_scope.change_failed` | failure | `changeUserScope` | `PUT` | `/api/v1/access/users/{id}/scope` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user_scope.changed` | success | `changeUserScope` | `PUT` | `/api/v1/access/users/{id}/scope` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `access.user.changed` | success | `createAccessUser` | `POST` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.manage` | 接口级事件，以本附录为准 |
| `access.user.changed_failed` | failure | `createAccessUser` | `POST` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.manage` | 接口级事件，以本附录为准 |
| `access.user.viewed` | success | `listAccessUsers` | `GET` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `amusement_group_booking.approved.failure` | failure | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | AMU-游乐安全 | `amusement.group_booking.approve` | 接口级事件，以本附录为准 |
| `amusement_group_booking.approved.success` | success | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | AMU-游乐安全 | `amusement.group_booking.approve` | 接口级事件，以本附录为准 |
| `amusement_group_booking.created.failure` | failure | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.created.success` | success | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.released.failure` | failure | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.released.success` | success | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | AMU-游乐安全 | `amusement.group_booking.manage` | 接口级事件，以本附录为准 |
| `amusement_group_booking.viewed.failure` | failure | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.view` | 接口级事件，以本附录为准 |
| `amusement_group_booking.viewed.success` | success | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | AMU-游乐安全 | `amusement.group_booking.view` | 接口级事件，以本附录为准 |
| `amusement.business_day.original_view_failed` | failure | `readAmusementBusinessDayConfigOriginal` | `GET` | `/api/v1/amusement/business-day-configs/{id}` | AMU-游乐安全 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `amusement.business_day.original_viewed` | success | `readAmusementBusinessDayConfigOriginal` | `GET` | `/api/v1/amusement/business-day-configs/{id}` | AMU-游乐安全 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `amusement.business_day.submit_completed` | success | `submitAmusementBusinessDayConfig` | `POST` | `/api/v1/amusement/business-day-configs` | AMU-游乐安全 | `amusement.business_day.manage` | 接口级事件，以本附录为准 |
| `amusement.business_day.submit_failed` | failure | `submitAmusementBusinessDayConfig` | `POST` | `/api/v1/amusement/business-day-configs` | AMU-游乐安全 | `amusement.business_day.manage` | 接口级事件，以本附录为准 |
| `amusement.business_day.view_failed` | failure | `listAmusementBusinessDayConfigs` | `GET` | `/api/v1/amusement/business-day-configs` | AMU-游乐安全 | `amusement.business_day.view` | 接口级事件，以本附录为准 |
| `amusement.business_day.viewed` | success | `listAmusementBusinessDayConfigs` | `GET` | `/api/v1/amusement/business-day-configs` | AMU-游乐安全 | `amusement.business_day.view` | 接口级事件，以本附录为准 |
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
| `amusement.fault_compensation.viewed` | success | `listAmusementCompensationBatchesByEvent` | `GET` | `/api/v1/amusement/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.fault_compensation.viewed` | success | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `amusement.incident.action` | success | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | AMU-游乐安全 | `amusement.incident.process` | 接口级事件，以本附录为准 |
| `amusement.incident.action_failed` | failure | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | AMU-游乐安全 | `amusement.incident.process` | 接口级事件，以本附录为准 |
| `amusement.incident.close` | success | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | AMU-游乐安全 | `amusement.incident.close` | 接口级事件，以本附录为准 |
| `amusement.incident.close_failed` | failure | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | AMU-游乐安全 | `amusement.incident.close` | 接口级事件，以本附录为准 |
| `amusement.incident.create` | success | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.create` | 接口级事件，以本附录为准 |
| `amusement.incident.create_failed` | failure | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.create` | 接口级事件，以本附录为准 |
| `amusement.incident.list` | success | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.view` | 接口级事件，以本附录为准 |
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
| `amusement.safety_template.change_failed` | failure | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_template.changed` | success | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.manage` | 接口级事件，以本附录为准 |
| `amusement.safety_template.view_failed` | failure | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.view` | 接口级事件，以本附录为准 |
| `amusement.safety_template.viewed` | success | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | AMU-游乐安全 | `amusement.safety_template.view` | 接口级事件，以本附录为准 |
| `amusement.session.change_failed` | failure | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.change_failed` | failure | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.changed` | success | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.changed` | success | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.manage` | 接口级事件，以本附录为准 |
| `amusement.session.end_completed` | success | `endAmusementSession` | `POST` | `/api/v1/amusement/sessions/{id}/end` | AMU-游乐安全 | `amusement.session.end` | 接口级事件，以本附录为准 |
| `amusement.session.end_failed` | failure | `endAmusementSession` | `POST` | `/api/v1/amusement/sessions/{id}/end` | AMU-游乐安全 | `amusement.session.end` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementCountReleases` | `GET` | `/api/v1/amusement/count-releases` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementSessionEndings` | `GET` | `/api/v1/amusement/sessions/{id}/endings` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementSessionFollowups` | `GET` | `/api/v1/amusement/sessions/{id}/followups` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.view_failed` | failure | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementCountReleases` | `GET` | `/api/v1/amusement/count-releases` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementSessionEndings` | `GET` | `/api/v1/amusement/sessions/{id}/endings` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementSessionFollowups` | `GET` | `/api/v1/amusement/sessions/{id}/followups` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.session.viewed` | success | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.status_board.view_failed` | failure | `listAmusementProjectStatusBoard` | `GET` | `/api/v1/amusement/projects/status-board` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.status_board.viewed` | success | `listAmusementProjectStatusBoard` | `GET` | `/api/v1/amusement/projects/status-board` | AMU-游乐安全 | `amusement.session.view` | 接口级事件，以本附录为准 |
| `amusement.verification_failed` | failure | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.execute` | 已在事件矩阵正文出现 |
| `amusement.verification_succeeded` | success | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.execute` | 已在事件矩阵正文出现 |
| `amusement.verification.view_failed` | failure | `listAmusementCapacitySnapshots` | `GET` | `/api/v1/amusement/capacity-snapshots` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.verification.view_failed` | failure | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.verification.viewed` | success | `listAmusementCapacitySnapshots` | `GET` | `/api/v1/amusement/capacity-snapshots` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
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
| `analytics.anomaly_config.update_failed` | failure | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.anomaly_config.updated` | success | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.ar_debtors.view_failed` | failure | `listArOverdueDebtors` | `GET` | `/api/v1/analytics/finance-receivables/debtors` | ANA-风控分析 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `analytics.ar_debtors.viewed` | success | `listArOverdueDebtors` | `GET` | `/api/v1/analytics/finance-receivables/debtors` | ANA-风控分析 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
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
| `analytics.overdue_work_orders.view_failed` | failure | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | ANA-风控分析 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `analytics.overdue_work_orders.viewed` | success | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | ANA-风控分析 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `analytics.rent_to_sales.view_failed` | failure | `getRentToSales` | `GET` | `/api/v1/analytics/rent-to-sales` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.rent_to_sales.viewed` | success | `getRentToSales` | `GET` | `/api/v1/analytics/rent-to-sales` | ANA-风控分析 | `contract.view` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.update_failed` | failure | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `analytics.revenue_attribution_rule.updated` | success | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
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
| `api_call.logged` | success | `getOpenParkingConfig` | `GET` | `/api/open/d/{endpoint_key}/parking/config` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `listOpenParkingPayments` | `GET` | `/api/open/d/{endpoint_key}/parking/payments` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `getOpenParkingConfig` | `GET` | `/api/open/d/{endpoint_key}/parking/config` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `listOpenParkingPayments` | `GET` | `/api/open/d/{endpoint_key}/parking/payments` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `ar_ap_payment.list_failed` | failure | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `ar_ap_payment.listed` | success | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `audit.operation_log.export_failed` | failure | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | P0-待归类 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `audit.operation_log.exported` | success | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | P0-待归类 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `auth.login.failed` | failure | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.login.succeeded` | success | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.logout` | success | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.logout_failed` | failure | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.master.challenge_issued` | success | `issueMasterAuthChallenge` | `POST` | `/api/v1/auth/master/challenge` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.master.login_failed` | failure | `masterAuthLogin` | `POST` | `/api/v1/auth/master/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.master.login_succeeded` | success | `masterAuthLogin` | `POST` | `/api/v1/auth/master/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.token_refresh_failed` | failure | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.token.refreshed` | success | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `bar.arrival_reminder_policy_update_failed` | failure | `saveBarArrivalReminderPolicy` | `PUT` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.arrival_reminder_policy_updated` | success | `saveBarArrivalReminderPolicy` | `PUT` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.bill_anomaly_raise_failed` | failure | `raiseBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly` | BAR-酒吧经营 | `bar.bill.adjust` | 接口级事件，以本附录为准 |
| `bar.bill_anomaly_raised` | success | `raiseBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly` | BAR-酒吧经营 | `bar.bill.adjust` | 接口级事件，以本附录为准 |
| `bar.bill_anomaly_resolve_failed` | failure | `resolveBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly/resolve` | BAR-酒吧经营 | `bar.bill.anomaly.resolve` | 接口级事件，以本附录为准 |
| `bar.bill_anomaly_resolved` | success | `resolveBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly/resolve` | BAR-酒吧经营 | `bar.bill.anomaly.resolve` | 接口级事件，以本附录为准 |
| `bar.bill_operation.list` | success | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill_operation.list_failed` | failure | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | BAR-酒吧经营 | `bar.bill.split_merge` | 接口级事件，以本附录为准 |
| `bar.bill_reopen_failed` | failure | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | BAR-酒吧经营 | `bar.bill.reopen` | 接口级事件，以本附录为准 |
| `bar.bill_reopened` | success | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | BAR-酒吧经营 | `bar.bill.reopen` | 接口级事件，以本附录为准 |
| `bar.bill_rewards.view_failed` | failure | `listBarBillMerchantRewards` | `GET` | `/api/v1/bar/bills/{bill_id}/rewards` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.bill_rewards.viewed` | success | `listBarBillMerchantRewards` | `GET` | `/api/v1/bar/bills/{bill_id}/rewards` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
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
| `bar.business_day.original_view_failed` | failure | `readBarBusinessDayConfigOriginal` | `GET` | `/api/v1/bar-business-day-configs/{id}` | BAR-酒吧经营 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `bar.business_day.original_viewed` | success | `readBarBusinessDayConfigOriginal` | `GET` | `/api/v1/bar-business-day-configs/{id}` | BAR-酒吧经营 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `bar.business_day.submit_completed` | success | `submitBarBusinessDayConfig` | `POST` | `/api/v1/bar-business-day-configs` | BAR-酒吧经营 | `bar.business_day.manage` | 接口级事件，以本附录为准 |
| `bar.business_day.submit_failed` | failure | `submitBarBusinessDayConfig` | `POST` | `/api/v1/bar-business-day-configs` | BAR-酒吧经营 | `bar.business_day.manage` | 接口级事件，以本附录为准 |
| `bar.business_day.view_failed` | failure | `listBarBusinessDayConfigs` | `GET` | `/api/v1/bar-business-day-configs` | BAR-酒吧经营 | `bar.business_day.view` | 接口级事件，以本附录为准 |
| `bar.business_day.viewed` | success | `listBarBusinessDayConfigs` | `GET` | `/api/v1/bar-business-day-configs` | BAR-酒吧经营 | `bar.business_day.view` | 接口级事件，以本附录为准 |
| `bar.catalog.image_uploaded` | success | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.catalog.image_uploaded_failed` | failure | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.category.delete_approve_failed` | failure | `approveDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-approve` | BAR-酒吧经营 | `bar.category.approve` | 接口级事件，以本附录为准 |
| `bar.category.delete_approved` | success | `approveDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-approve` | BAR-酒吧经营 | `bar.category.approve` | 接口级事件，以本附录为准 |
| `bar.category.delete_request_failed` | failure | `requestDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-request` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.category.delete_requested` | success | `requestDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-request` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.category.saved` | success | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.category.saved_failed` | failure | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `bar.combo.delete_approve_failed` | failure | `approveDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-approve` | BAR-酒吧经营 | `bar.combo.approve` | 接口级事件，以本附录为准 |
| `bar.combo.delete_approved` | success | `approveDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-approve` | BAR-酒吧经营 | `bar.combo.approve` | 接口级事件，以本附录为准 |
| `bar.combo.delete_request_failed` | failure | `requestDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-request` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `bar.combo.delete_requested` | success | `requestDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-request` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `bar.combo.saved` | success | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `bar.combo.saved_failed` | failure | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
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
| `bar.order_item_void_decided` | success | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void_failed` | failure | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.decide_failed` | failure | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.view_failed` | failure | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_void.viewed` | success | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item_voided` | success | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `bar.order_item.created` | success | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_item.created_failed` | failure | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_suspend_failed` | failure | `suspendBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/suspend` | BAR-酒吧经营 | `bar.bill.hang` | 接口级事件，以本附录为准 |
| `bar.order_suspended` | success | `suspendBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/suspend` | BAR-酒吧经营 | `bar.bill.hang` | 接口级事件，以本附录为准 |
| `bar.order_suspension_bad_debt` | success | `confirmBarOrderSuspensionBadDebt` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/bad-debt` | BAR-酒吧经营 | `bar.bill.hang_cancel` | 接口级事件，以本附录为准 |
| `bar.order_suspension_bad_debt_failed` | failure | `confirmBarOrderSuspensionBadDebt` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/bad-debt` | BAR-酒吧经营 | `bar.bill.hang_cancel` | 接口级事件，以本附录为准 |
| `bar.order_suspension_cancel_decide_failed` | failure | `decideBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel-decision` | BAR-酒吧经营 | `bar.bill.hang_cancel` | 接口级事件，以本附录为准 |
| `bar.order_suspension_cancel_decided` | success | `decideBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel-decision` | BAR-酒吧经营 | `bar.bill.hang_cancel` | 接口级事件，以本附录为准 |
| `bar.order_suspension_cancel_request_failed` | failure | `requestBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel` | BAR-酒吧经营 | `bar.bill.hang` | 接口级事件，以本附录为准 |
| `bar.order_suspension_cancel_requested` | success | `requestBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel` | BAR-酒吧经营 | `bar.bill.hang` | 接口级事件，以本附录为准 |
| `bar.order_suspension_write_off_failed` | failure | `writeOffBarOrderSuspension` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/write-off` | BAR-酒吧经营 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `bar.order_suspension_written_off` | success | `writeOffBarOrderSuspension` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/write-off` | BAR-酒吧经营 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `bar.order_suspension.export_failed` | failure | `exportBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders/export` | BAR-酒吧经营 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `bar.order_suspension.exported` | success | `exportBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders/export` | BAR-酒吧经营 | `audit.operation_log.export` | 接口级事件，以本附录为准 |
| `bar.order_suspension.view_failed` | failure | `listBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.order_suspension.viewed` | success | `listBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.order.view_failed` | failure | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.order.viewed` | success | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.confirm_failed` | failure | `confirmBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/confirm` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.confirmed_merchant` | success | `confirmBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/confirm` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.draft_failed` | failure | `draftBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.drafted` | success | `draftBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.read` | success | `listBarPayrollRules` | `GET` | `/api/v1/bar/payroll-rules` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.read_failed` | failure | `listBarPayrollRules` | `GET` | `/api/v1/bar/payroll-rules` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.submit_failed` | failure | `submitBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/submit` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll_rule.submitted` | success | `submitBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/submit` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll.month_processed` | success | `closeBarPayrollMonth` | `POST` | `/api/v1/bar/payroll-runs/close` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll.month_processed_failed` | failure | `closeBarPayrollMonth` | `POST` | `/api/v1/bar/payroll-runs/close` | BAR-酒吧经营 | `bar.payroll.manage` | 接口级事件，以本附录为准 |
| `bar.payroll.read` | success | `listBarPayrollRuns` | `GET` | `/api/v1/bar/payroll-runs` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.payroll.read_failed` | failure | `listBarPayrollRuns` | `GET` | `/api/v1/bar/payroll-runs` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.product_sold_out` | success | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | BAR-酒吧经营 | `bar.product.sold_out` | 接口级事件，以本附录为准 |
| `bar.product_sold_out_failed` | failure | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | BAR-酒吧经营 | `bar.product.sold_out` | 接口级事件，以本附录为准 |
| `bar.product.category_assigned` | success | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.category_assigned_failed` | failure | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.created` | success | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.created_failed` | failure | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.delete_approve_failed` | failure | `approveDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-approve` | BAR-酒吧经营 | `bar.product.approve` | 接口级事件，以本附录为准 |
| `bar.product.delete_approved` | success | `approveDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-approve` | BAR-酒吧经营 | `bar.product.approve` | 接口级事件，以本附录为准 |
| `bar.product.delete_request_failed` | failure | `requestDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-request` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.delete_requested` | success | `requestDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-request` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `bar.product.viewed` | success | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.product.viewed_failed` | failure | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket_event.list` | success | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | BAR-酒吧经营 | `bar.production_ticket.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket_event.list_failed` | failure | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | BAR-酒吧经营 | `bar.production_ticket.view` | 接口级事件，以本附录为准 |
| `bar.production_ticket.transition` | success | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | BAR-酒吧经营 | `bar.production_ticket.manage` | 接口级事件，以本附录为准 |
| `bar.production_ticket.transition_failed` | failure | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | BAR-酒吧经营 | `bar.production_ticket.manage` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed` | success | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed_failed` | failure | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.refund_context.read` | success | `getBarBillRefundContext` | `GET` | `/api/v1/bar/bills/{id}/refund-context` | BAR-酒吧经营 | `bar.bill.refund` | 接口级事件，以本附录为准 |
| `bar.refund_context.read_failed` | failure | `getBarBillRefundContext` | `GET` | `/api/v1/bar/bills/{id}/refund-context` | BAR-酒吧经营 | `bar.bill.refund` | 接口级事件，以本附录为准 |
| `bar.reservation_checked_in` | success | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_checkin_failed` | failure | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_paid` | success | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_pay_failed` | failure | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_entered` | success | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.reservation_invitation_entry_failed` | failure | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.reservation_invitation_issue_failed` | failure | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_issued` | success | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_revoke_failed` | failure | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_invitation_revoked` | success | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | BAR-酒吧经营 | `bar.reservation.invitation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.cancel_failed` | failure | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.cancelled` | success | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.create_failed` | failure | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.reservation.created` | success | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.reservation.delete_approve_failed` | failure | `approveDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-approve` | BAR-酒吧经营 | `bar.reservation.approve` | 接口级事件，以本附录为准 |
| `bar.reservation.delete_approved` | success | `approveDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-approve` | BAR-酒吧经营 | `bar.reservation.approve` | 接口级事件，以本附录为准 |
| `bar.reservation.delete_request_failed` | failure | `requestDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-request` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.delete_requested` | success | `requestDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-request` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation.view_failed` | failure | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.reservation.viewed` | success | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `bar.saved_bottle_picked_up` | success | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | INV-库存存酒 | `bar.saved_bottle.manage` | 已在事件矩阵正文出现 |
| `bar.saved_bottle_picked_up` | success | `dispenseSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/dispense` | INV-库存存酒 | `bar.saved_bottle.manage` | 已在事件矩阵正文出现 |
| `bar.saved_bottle_transferred` | success | `transferSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/transfer` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.cancel_failed` | failure | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.cancelled` | success | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.create_failed` | failure | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.created` | success | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 已在事件矩阵正文出现 |
| `bar.saved_bottle.dispute` | success | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.dispute_failed` | failure | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.exit_notice_failed` | failure | `noticeSavedBottleExit` | `POST` | `/api/v1/inventory/saved-bottles/exit-notice` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.exit_noticed` | success | `noticeSavedBottleExit` | `POST` | `/api/v1/inventory/saved-bottles/exit-notice` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.handed_over` | success | `handoverSavedBottlesToPlatform` | `POST` | `/api/v1/inventory/saved-bottles/handover` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.handover_failed` | failure | `handoverSavedBottlesToPlatform` | `POST` | `/api/v1/inventory/saved-bottles/handover` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.handover_view_failed` | failure | `listSavedBottleHandovers` | `GET` | `/api/v1/inventory/saved-bottles/handovers` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.handover_viewed` | success | `listSavedBottleHandovers` | `GET` | `/api/v1/inventory/saved-bottles/handovers` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_initiate_failed` | failure | `initiateSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/{id}/manual-pickups` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.initiate` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_initiated` | success | `initiateSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/{id}/manual-pickups` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.initiate` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_marketing_confirm_failed` | failure | `confirmSavedBottleManualPickupMarketing` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/confirm-marketing` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.confirm_marketing` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_marketing_confirmed` | success | `confirmSavedBottleManualPickupMarketing` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/confirm-marketing` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.confirm_marketing` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_policy_config_failed` | failure | `configureSavedBottleManualPickupPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/manual-pickup-policy` | INV-库存存酒 | `bar.saved_bottle.manual_pickup_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_policy_configured` | success | `configureSavedBottleManualPickupPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/manual-pickup-policy` | INV-库存存酒 | `bar.saved_bottle.manual_pickup_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_review_failed` | failure | `reviewSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/review` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.manual_pickup_reviewed` | success | `reviewSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/review` | INV-库存存酒 | `bar.saved_bottle.manual_pickup.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.pickup_code_issue_failed` | failure | `getCustomerSavedBottlePickupCode` | `GET` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/pickup-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.pickup_code_issued` | success | `getCustomerSavedBottlePickupCode` | `GET` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/pickup-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.pickup_failed` | failure | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.pickup_failed` | failure | `dispenseSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/dispense` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_advance_failed` | failure | `advanceSavedBottleRemedyByPlatform` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/advance` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_advanced` | success | `advanceSavedBottleRemedyByPlatform` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/advance` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_approve_failed` | failure | `approveSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/approve` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_approved` | success | `approveSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/approve` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_recovery_settle_failed` | failure | `settleSavedBottleRemedyRecovery` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/recovery/settle` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_recovery_settled` | success | `settleSavedBottleRemedyRecovery` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/recovery/settle` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_register_failed` | failure | `registerSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/{id}/remedy` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_registered` | success | `registerSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/{id}/remedy` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_settle_failed` | failure | `settleSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/settle` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_settled` | success | `settleSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/settle` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_staff_liability_assign_failed` | failure | `assignSavedBottleRemedyStaffLiability` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/staff-liability` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_staff_liability_assigned` | success | `assignSavedBottleRemedyStaffLiability` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/staff-liability` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_view_failed` | failure | `listSavedBottleRemedies` | `GET` | `/api/v1/inventory/saved-bottles/{id}/remedies` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.remedy_viewed` | success | `listSavedBottleRemedies` | `GET` | `/api/v1/inventory/saved-bottles/{id}/remedies` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.reminder_policy_config_failed` | failure | `configureSavedBottleReminderPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/reminder-policy` | INV-库存存酒 | `bar.saved_bottle.reminder_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.reminder_policy_configured` | success | `configureSavedBottleReminderPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/reminder-policy` | INV-库存存酒 | `bar.saved_bottle.reminder_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.reminder_policy_view_failed` | failure | `listSavedBottleReminderPolicies` | `GET` | `/api/v1/inventory/saved-bottles/reminder-policy` | INV-库存存酒 | `bar.saved_bottle.reminder_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.reminder_policy_viewed` | success | `listSavedBottleReminderPolicies` | `GET` | `/api/v1/inventory/saved-bottles/reminder-policy` | INV-库存存酒 | `bar.saved_bottle.reminder_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renew_failed` | failure | `renewCustomerSavedBottle` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renew` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_policy_config_failed` | failure | `configureSavedBottleRenewalPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/renewal-policy` | INV-库存存酒 | `bar.saved_bottle.renewal_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_policy_configured` | success | `configureSavedBottleRenewalPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/renewal-policy` | INV-库存存酒 | `bar.saved_bottle.renewal_policy.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_request_failed` | failure | `renewSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/renewals` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_requested` | success | `renewSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/renewals` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_review_failed` | failure | `reviewSavedBottleRenewal` | `POST` | `/api/v1/inventory/saved-bottles/renewals/{id}/review` | INV-库存存酒 | `bar.saved_bottle.renewal.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_reviewed` | success | `reviewSavedBottleRenewal` | `POST` | `/api/v1/inventory/saved-bottles/renewals/{id}/review` | INV-库存存酒 | `bar.saved_bottle.renewal.review` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_view_failed` | failure | `listSavedBottleRenewals` | `GET` | `/api/v1/inventory/saved-bottles/{id}/renewals` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewal_viewed` | success | `listSavedBottleRenewals` | `GET` | `/api/v1/inventory/saved-bottles/{id}/renewals` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.renewed` | success | `renewCustomerSavedBottle` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renew` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.transfer_failed` | failure | `transferSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/transfer` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.verification_code_request_failed` | failure | `requestCustomerSavedBottleRenewalCode` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renewal-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.verification_code_request_failed` | failure | `requestSavedBottleVerificationCode` | `POST` | `/api/v1/inventory/saved-bottles/{id}/verification-code` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.verification_code_requested` | success | `requestCustomerSavedBottleRenewalCode` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renewal-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.verification_code_requested` | success | `requestSavedBottleVerificationCode` | `POST` | `/api/v1/inventory/saved-bottles/{id}/verification-code` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.view_failed` | failure | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.view_failed` | failure | `lookupSavedBottlesByPhone` | `POST` | `/api/v1/inventory/saved-bottles/lookup` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.viewed` | success | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.viewed` | success | `lookupSavedBottlesByPhone` | `POST` | `/api/v1/inventory/saved-bottles/lookup` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.scan_order_create_failed` | failure | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.scan_order_created` | success | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.service.closed` | success | `closeBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-close` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.service.closed_failed` | failure | `closeBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-close` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.service.handover` | success | `handoverBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-handover` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.service.handover_failed` | failure | `handoverBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-handover` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.service.read` | success | `listBarServiceResponsibilities` | `GET` | `/api/v1/bar/table-sessions/{id}/service-responsibilities` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.service.read_failed` | failure | `listBarServiceResponsibilities` | `GET` | `/api/v1/bar/table-sessions/{id}/service-responsibilities` | BAR-酒吧经营 | `bar.payroll.view` | 接口级事件，以本附录为准 |
| `bar.shift_close_failed` | failure | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | BAR-酒吧经营 | `bar.shift.close` | 接口级事件，以本附录为准 |
| `bar.shift_closed` | success | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | BAR-酒吧经营 | `bar.shift.close` | 接口级事件，以本附录为准 |
| `bar.shift_join_failed` | failure | `joinBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/join` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_joined` | success | `joinBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/join` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_open_failed` | failure | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_opened` | success | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_resolve_failed` | failure | `resolveBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/resolve` | BAR-酒吧经营 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `bar.shift_resolved` | success | `resolveBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/resolve` | BAR-酒吧经营 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `bar.shift_sign_failed` | failure | `signBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/sign` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_signed` | success | `signBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/sign` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift_witness_failed` | failure | `witnessBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/witness` | BAR-酒吧经营 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `bar.shift_witnessed` | success | `witnessBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/witness` | BAR-酒吧经营 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `bar.shift.view_failed` | failure | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.shift.viewed` | success | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | BAR-酒吧经营 | `bar.shift.manage` | 接口级事件，以本附录为准 |
| `bar.staff_booking_restore_failed` | failure | `restoreBarStaffBookingSuspension` | `POST` | `/api/v1/bar/staff-booking-suspensions/{id}/restore` | BAR-酒吧经营 | `bar.staff_booking.restore` | 接口级事件，以本附录为准 |
| `bar.staff_booking_restored` | success | `restoreBarStaffBookingSuspension` | `POST` | `/api/v1/bar/staff-booking-suspensions/{id}/restore` | BAR-酒吧经营 | `bar.staff_booking.restore` | 接口级事件，以本附录为准 |
| `bar.staff_booking_suspension.view_failed` | failure | `listBarStaffBookingSuspensions` | `GET` | `/api/v1/bar/staff-booking-suspensions` | BAR-酒吧经营 | `bar.staff_booking.restore` | 接口级事件，以本附录为准 |
| `bar.staff_booking_suspension.viewed` | success | `listBarStaffBookingSuspensions` | `GET` | `/api/v1/bar/staff-booking-suspensions` | BAR-酒吧经营 | `bar.staff_booking.restore` | 接口级事件，以本附录为准 |
| `bar.table_availability.view_failed` | failure | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.table_availability.viewed` | success | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | BAR-酒吧经营 | `bar.reservation.create` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculate_failed` | failure | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculated` | success | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 已在事件矩阵正文出现 |
| `bar.table_session.attribution_reassign_failed` | failure | `reassignBarTableSessionAttribution` | `POST` | `/api/v1/bar/table-sessions/{session_id}/attribution-reassign` | BAR-酒吧经营 | `bar.table_session.attribution_reassign` | 接口级事件，以本附录为准 |
| `bar.table_session.attribution_reassigned` | success | `reassignBarTableSessionAttribution` | `POST` | `/api/v1/bar/table-sessions/{session_id}/attribution-reassign` | BAR-酒吧经营 | `bar.table_session.attribution_reassign` | 接口级事件，以本附录为准 |
| `bar.table_session.create` | success | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `bar.table_session.create_failed` | failure | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `bar.table_session.list` | success | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.view` | 接口级事件，以本附录为准 |
| `bar.table_session.list_failed` | failure | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | BAR-酒吧经营 | `bar.table_session.view` | 接口级事件，以本附录为准 |
| `bar.table_session.merge` | success | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | BAR-酒吧经营 | `bar.table_session.merge` | 接口级事件，以本附录为准 |
| `bar.table_session.merge_failed` | failure | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | BAR-酒吧经营 | `bar.table_session.merge` | 接口级事件，以本附录为准 |
| `bar.table_session.transfer_failed` | failure | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table_session.transfer_undo_failed` | failure | `undoBarTableTransfer` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer-undo` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table_space_backlog.view_failed` | failure | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space_backlog.viewed` | success | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space.assign_failed` | failure | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table_space.assigned` | success | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.change_failed` | failure | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.changed` | success | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.clear_failed` | failure | `clearBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/clear` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.cleared` | success | `clearBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/clear` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.disable_failed` | failure | `disableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/disable` | BAR-酒吧经营 | `bar.table.disable` | 接口级事件，以本附录为准 |
| `bar.table.disabled` | success | `disableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/disable` | BAR-酒吧经营 | `bar.table.disable` | 接口级事件，以本附录为准 |
| `bar.table.enable_failed` | failure | `enableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/enable` | BAR-酒吧经营 | `bar.table.disable` | 接口级事件，以本附录为准 |
| `bar.table.enabled` | success | `enableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/enable` | BAR-酒吧经营 | `bar.table.disable` | 接口级事件，以本附录为准 |
| `bar.table.open_failed` | failure | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.opened` | success | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.split` | success | `splitBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/split` | BAR-酒吧经营 | `bar.table_session.split` | 接口级事件，以本附录为准 |
| `bar.table.split_failed` | failure | `splitBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/split` | BAR-酒吧经营 | `bar.table_session.split` | 接口级事件，以本附录为准 |
| `bar.table.transfer_undone` | success | `undoBarTableTransfer` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer-undo` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table.transferred` | success | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `bar.table.view_failed` | failure | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `bar.table.viewed` | success | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.archive_failed` | failure | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.archived` | success | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.save_failed` | failure | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `bar.void_reason_code.saved` | success | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void_decide` | 接口级事件，以本附录为准 |
| `business_entity.change_failed` | failure | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.changed` | success | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.project_associate_failed` | failure | `associateBusinessEntityProject` | `POST` | `/api/v1/platform/business-entities/{entity_id}/projects` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.project_associated` | success | `associateBusinessEntityProject` | `POST` | `/api/v1/platform/business-entities/{entity_id}/projects` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.viewed` | success | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `complaint.classified` | success | `classifyComplaint` | `PUT` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `complaint.classify_failed` | failure | `classifyComplaint` | `PUT` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `complaint.loop_confirm_failed` | failure | `confirmComplaintLoop` | `POST` | `/api/v1/complaint-followups/{chain}/{work_order_id}/loop-confirm` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `complaint.loop_confirmed` | success | `confirmComplaintLoop` | `POST` | `/api/v1/complaint-followups/{chain}/{work_order_id}/loop-confirm` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.create` | success | `createContractKeyDate` | `POST` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.manage` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.create_failed` | failure | `createContractKeyDate` | `POST` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.manage` | 接口级事件，以本附录为准 |
| `compliance.contract_key_date.list` | success | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.view` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.create` | success | `createComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.create_failed` | failure | `createComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.list` | success | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.renew_failed` | failure | `renewComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items/{id}/renewal` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.expiry_item.renewed` | success | `renewComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items/{id}/renewal` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.grace.submit_failed` | failure | `requestComplianceGracePeriod` | `POST` | `/api/v1/compliance/expiry-items/{id}/grace-requests` | P0-待归类 | `compliance.grace_period.request` | 接口级事件，以本附录为准 |
| `compliance.grace.submitted` | success | `requestComplianceGracePeriod` | `POST` | `/api/v1/compliance/expiry-items/{id}/grace-requests` | P0-待归类 | `compliance.grace_period.request` | 接口级事件，以本附录为准 |
| `compliance.item_type_config.save_failed` | failure | `saveComplianceItemTypeConfig` | `PUT` | `/api/v1/compliance/item-type-configs/{item_type}` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.item_type_config.saved` | success | `saveComplianceItemTypeConfig` | `PUT` | `/api/v1/compliance/item-type-configs/{item_type}` | P0-待归类 | `compliance.expiry_item.manage` | 接口级事件，以本附录为准 |
| `compliance.reminder.list` | success | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | P0-待归类 | `compliance.reminder.view` | 接口级事件，以本附录为准 |
| `compliance.reminder.scan` | success | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | P0-待归类 | `compliance.reminder.scan` | 接口级事件，以本附录为准 |
| `compliance.reminder.scan_failed` | failure | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | P0-待归类 | `compliance.reminder.scan` | 接口级事件，以本附录为准 |
| `contract.activate_failed` | failure | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | P0-待归类 | `contract.lifecycle.activate` | 接口级事件，以本附录为准 |
| `contract.activated` | success | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | P0-待归类 | `contract.lifecycle.activate` | 接口级事件，以本附录为准 |
| `contract.approval_flow_save_failed` | failure | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.approval_flow_saved` | success | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `contract.attachments_add_failed` | failure | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.attachments_added` | success | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.change_raise_failed` | failure | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.change_raised` | success | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `contract.color_rule_save_failed` | failure | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.color_rule_saved` | success | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `contract.deposit_months_config_failed` | failure | `saveContractDepositMonths` | `POST` | `/api/v1/contracts/deposit-months` | P0-待归类 | `contract.deposit_months.manage` | 接口级事件，以本附录为准 |
| `contract.deposit_months_configured` | success | `saveContractDepositMonths` | `POST` | `/api/v1/contracts/deposit-months` | P0-待归类 | `contract.deposit_months.manage` | 接口级事件，以本附录为准 |
| `contract.deposit_refund_wait_config_failed` | failure | `saveContractDepositRefundWait` | `POST` | `/api/v1/contracts/deposit-refund-wait` | P0-待归类 | `contract.deposit_refund_wait.manage` | 接口级事件，以本附录为准 |
| `contract.deposit_refund_wait_configured` | success | `saveContractDepositRefundWait` | `POST` | `/api/v1/contracts/deposit-refund-wait` | P0-待归类 | `contract.deposit_refund_wait.manage` | 接口级事件，以本附录为准 |
| `contract.dispute_resolve_failed` | failure | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | P0-待归类 | `contract.settlement.dispute.resolve` | 接口级事件，以本附录为准 |
| `contract.dispute_resolved` | success | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | P0-待归类 | `contract.settlement.dispute.resolve` | 接口级事件，以本附录为准 |
| `contract.draft_failed` | failure | `createContractDraft` | `POST` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.draft_rent_terms_update_failed` | failure | `editContractDraftRentTerms` | `POST` | `/api/v1/contracts/{contract_id}/rent-terms` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.draft_rent_terms_updated` | success | `editContractDraftRentTerms` | `POST` | `/api/v1/contracts/{contract_id}/rent-terms` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.drafted` | success | `createContractDraft` | `POST` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.fee_billed` | success | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.period_bill_failed` | failure | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.rent_bill_failed` | failure | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.rent_billed` | success | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `contract.scope_request_raise_failed` | failure | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | P0-待归类 | `contract.scope_request.raise` | 接口级事件，以本附录为准 |
| `contract.scope_request_raised` | success | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | P0-待归类 | `contract.scope_request.raise` | 接口级事件，以本附录为准 |
| `contract.settle_failed` | failure | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settled` | success | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_documents_check_failed` | failure | `checkContractSettlementDocuments` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/documents-check` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.settlement_documents_checked` | success | `checkContractSettlementDocuments` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/documents-check` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `contract.submit_failed` | failure | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.submitted` | success | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | P0-待归类 | `contract.draft.manage` | 已在事件矩阵正文出现 |
| `customer.account.deregister_failed` | failure | `deregisterCustomerAccount` | `POST` | `/api/v1/customer/deregister` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.account.deregister_preview_failed` | failure | `previewCustomerDeregistration` | `GET` | `/api/v1/customer/deregister/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.account.deregister_previewed` | success | `previewCustomerDeregistration` | `GET` | `/api/v1/customer/deregister/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
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
| `customer.bar_invitation.entry_code_failed` | failure | `getCustomerBarInvitationEntryCode` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}/entry-code` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.entry_code_issued` | success | `getCustomerBarInvitationEntryCode` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}/entry-code` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.member_remove_failed` | failure | `removeCustomerBarInvitationMember` | `POST` | `/api/v1/customer/bar-invitations/{reservation_id}/members/{member_id}/remove` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.member_removed` | success | `removeCustomerBarInvitationMember` | `POST` | `/api/v1/customer/bar-invitations/{reservation_id}/members/{member_id}/remove` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.preview_failed` | failure | `previewCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/preview` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_invitation.previewed` | success | `previewCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/preview` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_view_failed` | failure | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_viewed` | success | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.create_failed` | failure | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.created` | success | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservations.view_failed` | failure | `listMyCustomerBarReservations` | `GET` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservations.viewed` | success | `listMyCustomerBarReservations` | `GET` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.create_failed` | failure | `createCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.created` | success | `createCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.paid` | success | `confirmCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders/{scan_order_id}/confirm` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `customer.bar_scan_order.pay_failed` | failure | `confirmCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders/{scan_order_id}/confirm` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `customer.birthday_rule.listed` | success | `listLoyaltyConfigBirthdayRules` | `GET` | `/api/v1/loyalty/config/birthday-rules` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.birthday_rule.save_failed` | failure | `saveLoyaltyConfigBirthdayRule` | `POST` | `/api/v1/loyalty/config/birthday-rules` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.birthday_rule.saved` | success | `saveLoyaltyConfigBirthdayRule` | `POST` | `/api/v1/loyalty/config/birthday-rules` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.blacklist_requests_viewed` | success | `listCustomerBlacklistRequests` | `GET` | `/api/v1/customer-blacklist-requests` | CUS-客人端 | `customer.blacklist.request` | 接口级事件，以本附录为准 |
| `customer.blacklist.approve_failed` | failure | `approveCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/approve` | CUS-客人端 | `customer.blacklist.approve` | 接口级事件，以本附录为准 |
| `customer.blacklist.approved` | success | `approveCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/approve` | CUS-客人端 | `customer.blacklist.approve` | 接口级事件，以本附录为准 |
| `customer.blacklist.reject_failed` | failure | `rejectCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/reject` | CUS-客人端 | `customer.blacklist.approve` | 接口级事件，以本附录为准 |
| `customer.blacklist.rejected` | success | `rejectCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/reject` | CUS-客人端 | `customer.blacklist.approve` | 接口级事件，以本附录为准 |
| `customer.blacklist.request_failed` | failure | `requestCustomerBlacklistChange` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist-requests` | CUS-客人端 | `customer.blacklist.request` | 接口级事件，以本附录为准 |
| `customer.blacklist.requested` | success | `requestCustomerBlacklistChange` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist-requests` | CUS-客人端 | `customer.blacklist.request` | 接口级事件，以本附录为准 |
| `customer.check_in_failed` | failure | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_in_tier.listed` | success | `listLoyaltyConfigCheckInTiers` | `GET` | `/api/v1/loyalty/config/check-in-tiers` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.check_in_tier.save_failed` | failure | `saveLoyaltyConfigCheckInTier` | `POST` | `/api/v1/loyalty/config/check-in-tiers` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.check_in_tier.saved` | success | `saveLoyaltyConfigCheckInTier` | `POST` | `/api/v1/loyalty/config/check-in-tiers` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.check_ins_view_failed` | failure | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_ins_viewed` | success | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.checked_in` | success | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.churn_alert_setting.config_failed` | failure | `configureCustomerChurnAlertSetting` | `PUT` | `/api/v1/customer-churn-alert-setting` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.churn_alert_setting.configured` | success | `configureCustomerChurnAlertSetting` | `PUT` | `/api/v1/customer-churn-alert-setting` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.complaint_rate_failed` | failure | `rateCustomerComplaint` | `POST` | `/api/v1/customer/complaints/{work_order_id}/satisfaction` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.complaint_rated` | success | `rateCustomerComplaint` | `POST` | `/api/v1/customer/complaints/{work_order_id}/satisfaction` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.complaint_submit_failed` | failure | `submitCustomerComplaint` | `POST` | `/api/v1/customer/complaints` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.complaint_submitted` | success | `submitCustomerComplaint` | `POST` | `/api/v1/customer/complaints` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.complaints_view_failed` | failure | `listMyCustomerComplaints` | `GET` | `/api/v1/customer/complaints` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.complaints_viewed` | success | `listMyCustomerComplaints` | `GET` | `/api/v1/customer/complaints` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.create_failed` | failure | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.created` | success | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.home_content_publish_failed` | failure | `publishCustomerHomeContent` | `POST` | `/api/v1/home-contents/{project_id}/publish` | CUS-客人端 | `customer.home_content.manage` | 接口级事件，以本附录为准 |
| `customer.home_content_published` | success | `publishCustomerHomeContent` | `POST` | `/api/v1/home-contents/{project_id}/publish` | CUS-客人端 | `customer.home_content.manage` | 接口级事件，以本附录为准 |
| `customer.level_evaluate_failed` | failure | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `customer.logged_out` | success | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.logout_failed` | failure | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.member_level.listed` | success | `listLoyaltyConfigMemberLevels` | `GET` | `/api/v1/loyalty/config/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.member_level.save_failed` | failure | `saveLoyaltyConfigMemberLevel` | `POST` | `/api/v1/loyalty/config/member-levels` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.member_level.saved` | success | `saveLoyaltyConfigMemberLevel` | `POST` | `/api/v1/loyalty/config/member-levels` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.my_saved_bottles_view_failed` | failure | `listMyCustomerSavedBottles` | `GET` | `/api/v1/customer/my-saved-bottles` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.my_saved_bottles_viewed` | success | `listMyCustomerSavedBottles` | `GET` | `/api/v1/customer/my-saved-bottles` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.my_tickets_view_failed` | failure | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.my_tickets_viewed` | success | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.order_tickets_view_failed` | failure | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.order_tickets_viewed` | success | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.orders_view_failed` | failure | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.orders_viewed` | success | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.otp_request_failed` | failure | `requestBarBillStoredValueSmsCode` | `POST` | `/api/v1/bar/bills/{bill_id}/stored-value/sms` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `customer.otp_request_failed` | failure | `requestCustomerLoginSmsCode` | `POST` | `/api/v1/customer/auth/sms-codes` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.otp_request_failed` | failure | `requestCustomerStoredValueSmsCode` | `POST` | `/api/v1/customer/stored-value/sms` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.otp_request_failed` | failure | `requestKioskBindCode` | `POST` | `/api/v1/ticketing/kiosk-bind-codes` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `customer.otp_requested` | success | `requestBarBillStoredValueSmsCode` | `POST` | `/api/v1/bar/bills/{bill_id}/stored-value/sms` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `customer.otp_requested` | success | `requestCustomerLoginSmsCode` | `POST` | `/api/v1/customer/auth/sms-codes` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.otp_requested` | success | `requestCustomerStoredValueSmsCode` | `POST` | `/api/v1/customer/stored-value/sms` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.otp_requested` | success | `requestKioskBindCode` | `POST` | `/api/v1/ticketing/kiosk-bind-codes` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `customer.payment_confirm_failed` | failure | `confirmCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments/{payment_intent_id}/confirm` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_confirm_failed` | failure | `confirmCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments/{payment_intent_id}/confirm` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_confirmed` | success | `confirmCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments/{payment_intent_id}/confirm` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_confirmed` | success | `confirmCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments/{payment_intent_id}/confirm` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_create_failed` | failure | `startCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_create_failed` | failure | `startCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_created` | success | `startCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.payment_created` | success | `startCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_view_failed` | failure | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_viewed` | success | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_voucher_fulfill_failed` | failure | `fulfillPointsRedemption` | `POST` | `/api/v1/loyalty/points-redemptions/{redemption_id}/fulfill` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
| `customer.points_voucher_fulfilled` | success | `fulfillPointsRedemption` | `POST` | `/api/v1/loyalty/points-redemptions/{redemption_id}/fulfill` | CUS-客人端 | `customer.loyalty.manage` | 接口级事件，以本附录为准 |
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
| `customer.register_failed` | failure | `registerCustomerMember` | `POST` | `/api/v1/customer/auth/register` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.registered` | success | `registerCustomerMember` | `POST` | `/api/v1/customer/auth/register` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.restriction_setting.config_failed` | failure | `configureCustomerRestrictionSetting` | `PUT` | `/api/v1/customer-restriction-settings/{project_id}` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.restriction_setting.configured` | success | `configureCustomerRestrictionSetting` | `PUT` | `/api/v1/customer-restriction-settings/{project_id}` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `customer.stored_value.pay_failed` | failure | `payCustomerTicketOrderWithStoredValue` | `POST` | `/api/v1/customer/orders/{order_id}/stored-value/pay` | CUS/TIC-客人购票 | `customer.stored_value.consume` | 接口级事件，以本附录为准 |
| `customer.stored_value.settle` | success | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.settle_failed` | failure | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.topup` | success | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.stored_value.topup_failed` | failure | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | CUS-客人端 | `customer.stored_value.topup` | 接口级事件，以本附录为准 |
| `customer.student_cert.applied` | success | `applyCustomerStudentCertification` | `POST` | `/api/v1/customer/student-certifications` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.student_cert.apply_failed` | failure | `applyCustomerStudentCertification` | `POST` | `/api/v1/customer/student-certifications` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.student_cert.approve_failed` | failure | `approveStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/approve` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.student_cert.approved` | success | `approveStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/approve` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.student_cert.reject_failed` | failure | `rejectStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/reject` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.student_cert.rejected` | success | `rejectStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/reject` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.student_certs_view_failed` | failure | `listStudentCertifications` | `GET` | `/api/v1/student-certifications` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.student_certs_viewed` | success | `listStudentCertifications` | `GET` | `/api/v1/student-certifications` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `customer.ticket_order.create_failed` | failure | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_order.created` | success | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_products_view_failed` | failure | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `customer.ticket_products_viewed` | success | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_incoming_view_failed` | failure | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_incoming_viewed` | success | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_outgoing_view_failed` | failure | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_transfer_outgoing_viewed` | success | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_usages_view_failed` | failure | `listCustomerTicketUsages` | `GET` | `/api/v1/customer/tickets/{ticket_id}/usages` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.ticket_usages_viewed` | success | `listCustomerTicketUsages` | `GET` | `/api/v1/customer/tickets/{ticket_id}/usages` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `data.cleanup.execute_failed` | failure | `approveDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/approve` | OPS-生产前置 | `data.cleanup.approve` | 接口级事件，以本附录为准 |
| `data.cleanup.executed` | success | `approveDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/approve` | OPS-生产前置 | `data.cleanup.approve` | 接口级事件，以本附录为准 |
| `dealer.admission_view_failed` | failure | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.admission_viewed` | success | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `dealer.contract_create_failed` | failure | `createDealerContract` | `POST` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_created` | success | `createDealerContract` | `POST` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_terms_update_failed` | failure | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract_terms_updated` | success | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract.handler_designate_failed` | failure | `designateDealerContractHandler` | `POST` | `/api/v1/dealers/contracts/{id}/designated-handler` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.contract.handler_designated` | success | `designateDealerContractHandler` | `POST` | `/api/v1/dealers/contracts/{id}/designated-handler` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.counterparty_confirmation.record_failed` | failure | `recordDealerCounterpartyConfirmation` | `POST` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.counterparty_confirmation.recorded` | success | `recordDealerCounterpartyConfirmation` | `POST` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.counterparty_confirmation.view_failed` | failure | `listDealerCounterpartyConfirmations` | `GET` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.counterparty_confirmation.viewed` | success | `listDealerCounterpartyConfirmations` | `GET` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
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
| `dealer.external_stock.viewed` | success | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | P0-待归类 | `dealer_portal.stock.view` | 接口级事件，以本附录为准 |
| `dealer.external_stock.viewed` | success | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
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
| `dealer.invoice.issue_failed` | failure | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.issued` | success | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.request_failed` | failure | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.requested` | success | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reversal_requested` | success | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reversal_result_failed` | failure | `recordDealerInvoiceReversalResult` | `POST` | `/api/v1/dealers/invoices/{id}/reversal-result` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reversal_result_recorded` | success | `recordDealerInvoiceReversalResult` | `POST` | `/api/v1/dealers/invoices/{id}/reversal-result` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.invoice.reverse_failed` | failure | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | P0-待归类 | `dealer.invoice.manage` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_decide_failed` | failure | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_decided` | success | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | P0-待归类 | `dealer.order.review` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_request_failed` | failure | `dealerPortalRequestOrderCancel` | `POST` | `/api/v1/dealer-portal/orders/{id}/cancel-request` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_request_failed` | failure | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_requested` | success | `dealerPortalRequestOrderCancel` | `POST` | `/api/v1/dealer-portal/orders/{id}/cancel-request` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.cancel_requested` | success | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.draft_failed` | failure | `createDealerOrderDraft` | `POST` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.draft_failed` | failure | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.drafted` | success | `createDealerOrderDraft` | `POST` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.drafted` | success | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_use_confirmed` | success | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
| `dealer.order.overpayment_use_failed` | failure | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | P0-待归类 | `dealer.order.payment.verify` | 接口级事件，以本附录为准 |
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
| `dealer.order.viewed` | success | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.order.viewed` | success | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `dealer.portal_account.create_failed` | failure | `createDealerPortalAccount` | `POST` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal_account.created` | success | `createDealerPortalAccount` | `POST` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.access_rejected` | failure | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `dealer.portal.delivery_evidence_view_failed` | failure | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.delivery_evidence_viewed` | success | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.invoices_view_failed` | failure | `dealerPortalListInvoices` | `GET` | `/api/v1/dealer-portal/orders/{id}/invoices` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.invoices_viewed` | success | `dealerPortalListInvoices` | `GET` | `/api/v1/dealer-portal/orders/{id}/invoices` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `dealer.portal.return_financials_view_failed` | failure | `getDealerPortalReturnFinancials` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-financials` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.portal.return_financials_viewed` | success | `getDealerPortalReturnFinancials` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-financials` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.portal.return_sources_view_failed` | failure | `getDealerPortalReturnSources` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-sources` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.portal.return_sources_viewed` | success | `getDealerPortalReturnSources` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-sources` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.decide_failed` | failure | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | P0-待归类 | `dealer.price_catalog.approve` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.decided` | success | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | P0-待归类 | `dealer.price_catalog.approve` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.draft_failed` | failure | `createDealerPriceCatalogDraft` | `POST` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.drafted` | success | `createDealerPriceCatalogDraft` | `POST` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.submit_failed` | failure | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.price_catalog.submitted` | success | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | P0-待归类 | `dealer.price_catalog.edit` | 接口级事件，以本附录为准 |
| `dealer.return.decide_failed` | failure | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | P0-待归类 | `dealer.return.review` | 接口级事件，以本附录为准 |
| `dealer.return.decided` | success | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | P0-待归类 | `dealer.return.review` | 接口级事件，以本附录为准 |
| `dealer.return.financial_view_failed` | failure | `getDealerReturnFinancial` | `GET` | `/api/v1/dealers/returns/{id}/financial` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.financial_viewed` | success | `getDealerReturnFinancial` | `GET` | `/api/v1/dealers/returns/{id}/financial` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.freight_confirm_failed` | failure | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.freight_confirmed` | success | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.inspect_failed` | failure | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.inspected` | success | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.receive_failed` | failure | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.received` | success | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | P0-待归类 | `dealer.return.inspect` | 接口级事件，以本附录为准 |
| `dealer.return.refund_execute_failed` | failure | `executeDealerReturnRefunds` | `POST` | `/api/v1/dealers/returns/{id}/refunds/execute` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.return.refund_execute_failed` | failure | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.return.refund_requested` | success | `executeDealerReturnRefunds` | `POST` | `/api/v1/dealers/returns/{id}/refunds/execute` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `dealer.return.request_failed` | failure | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.request_failed` | failure | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.requested` | success | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `dealer.return.requested` | success | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.manage` | 接口级事件，以本附录为准 |
| `dealer.return.sources_view_failed` | failure | `getDealerReturnSources` | `GET` | `/api/v1/dealers/orders/{id}/return-sources` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.return.sources_viewed` | success | `getDealerReturnSources` | `GET` | `/api/v1/dealers/orders/{id}/return-sources` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `dealer.review_failed` | failure | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | P0-待归类 | `dealer.review` | 接口级事件，以本附录为准 |
| `dealer.reviewed` | success | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | P0-待归类 | `dealer.review` | 接口级事件，以本附录为准 |
| `dealer.submit_failed` | failure | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | P0-待归类 | `dealer_portal.profile.submit` | 接口级事件，以本附录为准 |
| `dealer.submit_failed` | failure | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.submitted` | success | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | P0-待归类 | `dealer_portal.profile.submit` | 接口级事件，以本附录为准 |
| `dealer.submitted` | success | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | P0-待归类 | `dealer.manage` | 接口级事件，以本附录为准 |
| `dealer.viewed` | success | `listDealers` | `GET` | `/api/v1/dealers` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `device.policy_violation` | success | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `device.policy_violation` | success | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `device.policy_violation_failed` | failure | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `device.policy_violation_failed` | failure | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `dividend.approve_failed` | failure | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.approved` | success | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.compute_failed` | failure | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.computed` | success | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.hold_lift_failed` | failure | `liftDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds/{hold_id}/lift` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.hold_lifted` | success | `liftDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds/{hold_id}/lift` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.hold_record_failed` | failure | `recordDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.hold_recorded` | success | `recordDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.holds_list_failed` | failure | `listDividendPeriodHolds` | `GET` | `/api/v1/finance/dividends/holds` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.holds_listed` | success | `listDividendPeriodHolds` | `GET` | `/api/v1/finance/dividends/holds` | FIN-财务台账 | `finance.dividend.view` | 接口级事件，以本附录为准 |
| `dividend.legal_confirm_failed` | failure | `confirmDividendLegalAmount` | `POST` | `/api/v1/finance/dividends/statements/{id}/legal-confirmation` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
| `dividend.legal_confirmed` | success | `confirmDividendLegalAmount` | `POST` | `/api/v1/finance/dividends/statements/{id}/legal-confirmation` | FIN-财务台账 | `finance.dividend.manage` | 接口级事件，以本附录为准 |
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
| `energy.actual_charge_recorded` | success | `recordEnergyActualCharge` | `POST` | `/api/v1/energy/actual-charges` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.actual_charge_recorded_failed` | failure | `recordEnergyActualCharge` | `POST` | `/api/v1/energy/actual-charges` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.actual_charges_viewed` | success | `listEnergyActualCharges` | `GET` | `/api/v1/energy/actual-charges` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.actual_charges_viewed_failed` | failure | `listEnergyActualCharges` | `GET` | `/api/v1/energy/actual-charges` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.attachment_viewed` | success | `readEnergyMeterAttachment` | `GET` | `/api/v1/energy/meters/{meter_id}/attachments/{attachment_id}/file` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.attachment_viewed_failed` | failure | `readEnergyMeterAttachment` | `GET` | `/api/v1/energy/meters/{meter_id}/attachments/{attachment_id}/file` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.escalations_viewed` | success | `listEnergyEscalations` | `GET` | `/api/v1/energy/escalations` | P0-待归类 | `energy.reading.approve` | 接口级事件，以本附录为准 |
| `energy.escalations_viewed_failed` | failure | `listEnergyEscalations` | `GET` | `/api/v1/energy/escalations` | P0-待归类 | `energy.reading.approve` | 接口级事件，以本附录为准 |
| `energy.fee_settle_failed` | failure | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settle_failed` | failure | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settled` | success | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.fee_settled` | success | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.history_viewed` | success | `energyMeterHistory` | `GET` | `/api/v1/energy/meters/{meter_id}/history` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.history_viewed_failed` | failure | `energyMeterHistory` | `GET` | `/api/v1/energy/meters/{meter_id}/history` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_initialized` | success | `initializeEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/opening` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_initialized_failed` | failure | `initializeEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/opening` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_replaced` | success | `replaceEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/replacement` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_replaced_failed` | failure | `replaceEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/replacement` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_retired` | success | `retireEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/retirement` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_retired_failed` | failure | `retireEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/retirement` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_save_failed` | failure | `createEnergyMeter` | `POST` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_save_failed` | failure | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_saved` | success | `createEnergyMeter` | `POST` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.meter_saved` | success | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.reading_recorded` | success | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_recorded_failed` | failure | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_reviewed` | success | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_reviewed_failed` | failure | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_verification_submitted` | success | `verifyEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/verification` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.reading_verification_submitted_failed` | failure | `verifyEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/verification` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `energy.review_settings_submitted` | success | `configureEnergyReviewSettings` | `POST` | `/api/v1/energy/projects/{project_id}/review-settings` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.review_settings_submitted_failed` | failure | `configureEnergyReviewSettings` | `POST` | `/api/v1/energy/projects/{project_id}/review-settings` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.settings_viewed` | success | `listEnergyReviewSettings` | `GET` | `/api/v1/energy/projects/{project_id}/review-settings` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.settings_viewed_failed` | failure | `listEnergyReviewSettings` | `GET` | `/api/v1/energy/projects/{project_id}/review-settings` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `energy.verifier_assigned` | success | `assignEnergyVerifier` | `POST` | `/api/v1/energy/readings/{reading_id}/assignment` | P0-待归类 | `energy.reading.approve` | 接口级事件，以本附录为准 |
| `energy.verifier_assigned_failed` | failure | `assignEnergyVerifier` | `POST` | `/api/v1/energy/readings/{reading_id}/assignment` | P0-待归类 | `energy.reading.approve` | 接口级事件，以本附录为准 |
| `entity_binding.change_failed` | failure | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `entity_binding.change_failed` | failure | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `entity_binding.changed` | success | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | GOV-平台治理 | `entity.binding.manage` | 已在事件矩阵正文出现 |
| `entity_binding.changed` | success | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.manage` | 已在事件矩阵正文出现 |
| `entity_binding.view_failed` | failure | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `entity_binding.viewed` | success | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `entity_tax.declare_failed` | failure | `declareEntityTax` | `POST` | `/api/v1/finance/entity-taxes` | FIN-财务台账 | `finance.entity_tax.manage` | 接口级事件，以本附录为准 |
| `entity_tax.declared` | success | `declareEntityTax` | `POST` | `/api/v1/finance/entity-taxes` | FIN-财务台账 | `finance.entity_tax.manage` | 接口级事件，以本附录为准 |
| `entity_tax.list_failed` | failure | `listEntityTaxDeclarations` | `GET` | `/api/v1/finance/entity-taxes` | FIN-财务台账 | `finance.entity_tax.view` | 接口级事件，以本附录为准 |
| `entity_tax.listed` | success | `listEntityTaxDeclarations` | `GET` | `/api/v1/finance/entity-taxes` | FIN-财务台账 | `finance.entity_tax.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_bind_failed` | failure | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_bound` | success | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_complete_failed` | failure | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_completed` | success | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_file_view_failed` | failure | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_file_viewed` | success | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.attachment_upload_failed` | failure | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `evidence.attachment_upload_failed` | failure | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_upload_failed` | failure | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.attachment_uploaded` | success | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | P0-待归类 | `facility.site_entry.upload_completion_assigned` | 接口级事件，以本附录为准 |
| `evidence.event_view_failed` | failure | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.event_viewed` | success | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.events_report_failed` | failure | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.events_reported` | success | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_policy_update_failed` | failure | `saveManualBackfillPolicy` | `PUT` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | EVD-事件证据 | `evidence.manual_backfill.policy.manage` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_policy_updated` | success | `saveManualBackfillPolicy` | `PUT` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | EVD-事件证据 | `evidence.manual_backfill.policy.manage` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submit_failed` | failure | `backfillTraceHandoverSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/segments/{segment}/backfill` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submit_failed` | failure | `submitBarTableSessionSalesBackfill` | `POST` | `/api/v1/bar/table-sessions/{session_id}/sales-backfill` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submit_failed` | failure | `submitBarTableSessionServiceBackfill` | `POST` | `/api/v1/bar/table-sessions/{id}/service-backfill` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submitted` | success | `backfillTraceHandoverSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/segments/{segment}/backfill` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submitted` | success | `submitBarTableSessionSalesBackfill` | `POST` | `/api/v1/bar/table-sessions/{session_id}/sales-backfill` | BAR-酒吧经营 | `bar.table_session.create` | 接口级事件，以本附录为准 |
| `evidence.manual_backfill_submitted` | success | `submitBarTableSessionServiceBackfill` | `POST` | `/api/v1/bar/table-sessions/{id}/service-backfill` | BAR-酒吧经营 | `bar.table_session.transfer` | 接口级事件，以本附录为准 |
| `evidence.review_create_failed` | failure | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.review_created` | success | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.timeline_view_failed` | failure | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.timeline_viewed` | success | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_created` | success | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_failed` | failure | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
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
| `facility.alarm_disposition_failed` | failure | `recordFacilityAlarmDisposition` | `POST` | `/api/v1/facility/alarms/{alarm_id}/disposition` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `facility.alarm_disposition_recorded` | success | `recordFacilityAlarmDisposition` | `POST` | `/api/v1/facility/alarms/{alarm_id}/disposition` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `facility.alarm_duty_assign_failed` | failure | `assignFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_duty_assigned` | success | `assignFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_duty_cancel_failed` | failure | `cancelFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties/{duty_id}/cancel` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_duty_cancelled` | success | `cancelFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties/{duty_id}/cancel` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_policy_save_failed` | failure | `saveFacilityAlarmResponsePolicy` | `POST` | `/api/v1/facility/alarms/policies` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_policy_saved` | success | `saveFacilityAlarmResponsePolicy` | `POST` | `/api/v1/facility/alarms/policies` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.alarm_take_over_failed` | failure | `takeOverFacilityAlarm` | `POST` | `/api/v1/facility/alarms/{alarm_id}/take-over` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `facility.alarm_taken_over` | success | `takeOverFacilityAlarm` | `POST` | `/api/v1/facility/alarms/{alarm_id}/take-over` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `facility.equipment_archived` | success | `archiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/archive` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_create_failed` | failure | `createFacilityEquipment` | `POST` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_created` | success | `createFacilityEquipment` | `POST` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_delete_approved` | success | `approveDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-approve` | P0-待归类 | `facility.equipment.approve` | 接口级事件，以本附录为准 |
| `facility.equipment_delete_requested` | success | `requestDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-request` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_maintain_failed` | failure | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_maintained` | success | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_disposal_complete_failed` | failure | `completeFacilityEquipmentScrapDisposal` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/disposal` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_disposal_completed` | success | `completeFacilityEquipmentScrapDisposal` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/disposal` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_finance_record_failed` | failure | `recordFacilityEquipmentScrapFinance` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/finance` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_finance_recorded` | success | `recordFacilityEquipmentScrapFinance` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/finance` | P0-待归类 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_request_failed` | failure | `createFacilityEquipmentScrapRequest` | `POST` | `/api/v1/facility/equipment/{equipment_id}/scrap-requests` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scrap_requested` | success | `createFacilityEquipmentScrapRequest` | `POST` | `/api/v1/facility/equipment/{equipment_id}/scrap-requests` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_scraps_viewed` | success | `listFacilityEquipmentScraps` | `GET` | `/api/v1/facility/equipment-scraps` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_status_change_failed` | failure | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_status_changed` | success | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_unarchived` | success | `unarchiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/unarchive` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_update_failed` | failure | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.equipment_updated` | success | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `facility.fault_rule_confirm_failed` | failure | `confirmFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/confirm` | P0-待归类 | `facility.fault_rule.confirm` | 接口级事件，以本附录为准 |
| `facility.fault_rule_confirmed` | success | `confirmFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/confirm` | P0-待归类 | `facility.fault_rule.confirm` | 接口级事件，以本附录为准 |
| `facility.fault_rule_create_failed` | failure | `createFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.fault_rule_created` | success | `createFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.fault_rule_retire_failed` | failure | `retireFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/retire` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.fault_rule_retired` | success | `retireFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/retire` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_exception_report_failed` | failure | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | P0-待归类 | `facility.inspection.report_exception_assigned` | 接口级事件，以本附录为准 |
| `facility.inspection_exception_reported` | success | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | P0-待归类 | `facility.inspection.report_exception_assigned` | 接口级事件，以本附录为准 |
| `facility.inspection_record_submit_failed` | failure | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_record_submitted` | success | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_close_failed` | failure | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_closed` | success | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_create_failed` | failure | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_created` | success | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_recheck_failed` | failure | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_rechecked` | success | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_start_failed` | failure | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_task_started` | success | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template_create_failed` | failure | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `facility.inspection_template_created` | success | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
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
| `facility.work_order_accept_failed` | failure | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_accepted` | success | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_assign_failed` | failure | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_assigned` | success | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_bearer_dispute_failed` | failure | `disputeFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/bearer-dispute` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_bearer_disputed` | success | `disputeFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/bearer-dispute` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_complete_failed` | failure | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_completed` | success | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_confirmer_designate_failed` | failure | `designateFacilityWorkOrderConfirmer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/confirmer` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_confirmer_designated` | success | `designateFacilityWorkOrderConfirmer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/confirmer` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_cost_bearer_mark_failed` | failure | `markFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/cost-bearer` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_cost_bearer_marked` | success | `markFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/cost-bearer` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_create_failed` | failure | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_created` | success | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_material_issue_failed` | failure | `issueFacilityWorkOrderMaterial` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/materials` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_material_issued` | success | `issueFacilityWorkOrderMaterial` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/materials` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_overdue_mark_failed` | failure | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_overdue_marked` | success | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_policy_save_failed` | failure | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_policy_saved` | success | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_reassign_failed` | failure | `reassignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/reassign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_reassigned` | success | `reassignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/reassign` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_resume_failed` | failure | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_resumed` | success | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_review_failed` | failure | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_reviewed` | success | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_rework_failed` | failure | `reworkFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/rework` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_rework_requested` | success | `reworkFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/rework` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_start_failed` | failure | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_started` | success | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_suspend_failed` | failure | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `facility.work_order_suspended` | success | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `finance_ar_ap.create_failed` | failure | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `finance_ar_ap.created` | success | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 已在事件矩阵正文出现 |
| `finance_ar_ap.export_failed` | failure | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | FIN-财务台账 | `finance.ar_ap.export` | 接口级事件，以本附录为准 |
| `finance_ar_ap.exported` | success | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | FIN-财务台账 | `finance.ar_ap.export` | 接口级事件，以本附录为准 |
| `finance_ar_ap.view_failed` | failure | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_ar_ap.viewed` | success | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_daily_close.archive_failed` | failure | `archiveDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/archive` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.archived` | success | `archiveDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/archive` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_daily_close.confirm_failed` | failure | `confirmDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/confirm` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.confirmed` | success | `confirmDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/confirm` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_daily_close.final_mark_failed` | failure | `markDailyCloseFinal` | `POST` | `/api/v1/finance/daily-closes/{id}/final-marks` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.final_marked` | success | `markDailyCloseFinal` | `POST` | `/api/v1/finance/daily-closes/{id}/final-marks` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_daily_close.generate_failed` | failure | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.generated` | success | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_daily_close.lock_failed` | failure | `lockDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/lock` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.locked` | success | `lockDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/lock` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_payment.register_failed` | failure | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.register_failed` | failure | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.registered` | success | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_payment.registered` | success | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_period.close_failed` | failure | `closeFinancePeriod` | `POST` | `/api/v1/finance/period-closes` | FIN-财务台账 | `finance.period_close.execute` | 接口级事件，以本附录为准 |
| `finance_period.closed` | success | `closeFinancePeriod` | `POST` | `/api/v1/finance/period-closes` | FIN-财务台账 | `finance.period_close.execute` | 接口级事件，以本附录为准 |
| `finance_period.view_failed` | failure | `listFinancePeriodCloses` | `GET` | `/api/v1/finance/period-closes` | FIN-财务台账 | `finance.period_close.execute` | 接口级事件，以本附录为准 |
| `finance_period.viewed` | success | `listFinancePeriodCloses` | `GET` | `/api/v1/finance/period-closes` | FIN-财务台账 | `finance.period_close.execute` | 接口级事件，以本附录为准 |
| `finance_property_fee.bill_failed` | failure | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.billed` | success | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_set` | success | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `finance_property_fee.rate_set_failed` | failure | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
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
| `finance_turnover_rent_rule.create_failed` | failure | `createTurnoverRentRule` | `POST` | `/api/v1/finance/turnover-rent/rules` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `finance_turnover_rent_rule.created` | success | `createTurnoverRentRule` | `POST` | `/api/v1/finance/turnover-rent/rules` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `finance.daily_close_adjustment.view_failed` | failure | `listDailyCloseAdjustments` | `GET` | `/api/v1/finance/daily-closes/{id}/adjustments` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close_adjustment.viewed` | success | `listDailyCloseAdjustments` | `GET` | `/api/v1/finance/daily-closes/{id}/adjustments` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.view_failed` | failure | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.viewed` | success | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close.view_failed` | failure | `listDailyCloses` | `GET` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close.viewed` | success | `listDailyCloses` | `GET` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.deposit.action_failed` | failure | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | FIN-财务台账 | `finance.deposit.manage` | 接口级事件，以本附录为准 |
| `finance.deposit.action_recorded` | success | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | FIN-财务台账 | `finance.deposit.manage` | 接口级事件，以本附录为准 |
| `finance.internal_payment.fail_record_failed` | failure | `recordInternalPaymentFailure` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts/{attempt_id}/failure` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.internal_payment.failed` | success | `recordInternalPaymentFailure` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts/{attempt_id}/failure` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.internal_payment.initiate_failed` | failure | `initiateInternalPayment` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.internal_payment.initiated` | success | `initiateInternalPayment` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.internal_payment.postpone_failed` | failure | `postponeInternalPaymentForFunds` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/funds-insufficient` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.internal_payment.postponed` | success | `postponeInternalPaymentForFunds` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/funds-insufficient` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance.inventory_variance.post_failed` | failure | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | FIN-财务台账 | `finance.inventory_variance.post` | 接口级事件，以本附录为准 |
| `finance.inventory_variance.posted` | success | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | FIN-财务台账 | `finance.inventory_variance.post` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_approved` | success | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_decision_failed` | failure | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_request_failed` | failure | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_requested` | success | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.payroll.attempts_view_failed` | failure | `getPayrollBankPaymentContext` | `GET` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | FIN-财务台账 | `finance.payroll.view` | 接口级事件，以本附录为准 |
| `finance.payroll.attempts_viewed` | success | `getPayrollBankPaymentContext` | `GET` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | FIN-财务台账 | `finance.payroll.view` | 接口级事件，以本附录为准 |
| `finance.payroll.payment_failed` | failure | `confirmPayrollBankPaymentResult` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/result` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.payroll.payment_failed` | failure | `preparePayrollBankPayment` | `POST` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.payroll.payment_failed` | failure | `submitPayrollBankPaymentFact` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/submit` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.payroll.prepare` | success | `preparePayrollBankPayment` | `POST` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.payroll.result` | success | `confirmPayrollBankPaymentResult` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/result` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.payroll.submit` | success | `submitPayrollBankPaymentFact` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/submit` | FIN-财务台账 | `finance.payroll.payment.register` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed` | success | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed_failed` | failure | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance.reconciliation_stage_limit.save_failed` | failure | `saveFinanceReconciliationStageLimit` | `PUT` | `/api/v1/finance/reconciliation/stage-limits/{project_id}` | FIN-财务台账 | `finance.reconciliation.policy.manage` | 接口级事件，以本附录为准 |
| `finance.reconciliation_stage_limit.saved` | success | `saveFinanceReconciliationStageLimit` | `PUT` | `/api/v1/finance/reconciliation/stage-limits/{project_id}` | FIN-财务台账 | `finance.reconciliation.policy.manage` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_create_failed` | failure | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.difference.create` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_created` | success | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `finance.reconciliation.difference.create` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_identified` | success | `identifyFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/identify` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_identify_failed` | failure | `identifyFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/identify` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_review_submit_failed` | failure | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.reconciliation.difference_review_submitted` | success | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | FIN-财务台账 | `finance.reconciliation.resolve` | 接口级事件，以本附录为准 |
| `finance.turnover_rent.bill_failed` | failure | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `finance.turnover_rent.billed` | success | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | FIN-财务台账 | `finance.turnover_rent.bill` | 接口级事件，以本附录为准 |
| `floor_plan.create_failed` | failure | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.created` | success | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_candidate_viewed` | success | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.import_create_failed` | failure | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_created` | success | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_publish_failed` | failure | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_published` | success | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | P0-待归类 | `floor_plan.import.manage` | 接口级事件，以本附录为准 |
| `floor_plan.import_viewed` | success | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `floor_plan.shape_viewed` | success | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `floor_plan.shape.save` | success | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.shape.save_failed` | failure | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.manage` | 接口级事件，以本附录为准 |
| `floor_plan.viewed` | success | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `governance.approval_deadline.change_failed` | failure | `updateApprovalDeadline` | `PUT` | `/api/v1/platform/approval-deadlines/{action_type}` | GOV-平台治理 | `admission.policy.manage` | 接口级事件，以本附录为准 |
| `governance.approval_deadline.changed` | success | `updateApprovalDeadline` | `PUT` | `/api/v1/platform/approval-deadlines/{action_type}` | GOV-平台治理 | `admission.policy.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.chain_regenerated` | success | `regenerateSupervisorReviewChain` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/chain/regenerate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decide_failed` | failure | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decided` | success | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluate_failed` | failure | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluated` | success | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.forwarded` | success | `forwardSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/forward` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.original_view_failed` | failure | `readSupervisorReviewOriginal` | `GET` | `/api/v1/platform/supervisor-review/{review_id}` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.original_viewed` | success | `readSupervisorReviewOriginal` | `GET` | `/api/v1/platform/supervisor-review/{review_id}` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.policies_viewed` | success | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `integration.channel_reconciliation.completed` | success | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.channel_reconciliation.failed` | failure | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | INT-第三方集成 | `integration.reconciliation.manage` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.archive_failed` | failure | `archiveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps/{id}/archive` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.archived` | success | `archiveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps/{id}/archive` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.save_failed` | failure | `saveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.saved` | success | `saveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps` | INT-第三方集成 | `integration.channel_store_map.manage` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.view_failed` | failure | `listIntegrationChannelStoreMaps` | `GET` | `/api/v1/integration/channel-store-maps` | INT-第三方集成 | `integration.channel_store_map.view` | 接口级事件，以本附录为准 |
| `integration.channel_store_map.viewed` | success | `listIntegrationChannelStoreMaps` | `GET` | `/api/v1/integration/channel-store-maps` | INT-第三方集成 | `integration.channel_store_map.view` | 接口级事件，以本附录为准 |
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
| `integration.order_sync.unattributed_view_failed` | failure | `listIntegrationUnattributedOrderSyncs` | `GET` | `/api/v1/integration/order-syncs/unattributed` | INT-第三方集成 | `integration.order_sync.view` | 接口级事件，以本附录为准 |
| `integration.order_sync.unattributed_viewed` | success | `listIntegrationUnattributedOrderSyncs` | `GET` | `/api/v1/integration/order-syncs/unattributed` | INT-第三方集成 | `integration.order_sync.view` | 接口级事件，以本附录为准 |
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
| `inventory.adjustment_failed` | failure | `createDamageStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/damage` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.adjustment_failed` | failure | `createLossStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/loss` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.adjustment_failed` | failure | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
| `inventory.adjustment_failed` | failure | `createWasteStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/waste` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.bar_consumption_stats.view_failed` | failure | `listBarConsumptionStats` | `GET` | `/api/v1/inventory/bar-consumption-stats` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.bar_consumption_stats.viewed` | success | `listBarConsumptionStats` | `GET` | `/api/v1/inventory/bar-consumption-stats` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.cost_ledger.view_failed` | failure | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_ledger.viewed` | success | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_summary.view_failed` | failure | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.cost_summary.viewed` | success | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | INV-库存存酒 | `inventory.cost.view` | 接口级事件，以本附录为准 |
| `inventory.counted` | success | `createDamageStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/damage` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
| `inventory.counted` | success | `createLossStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/loss` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
| `inventory.counted` | success | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | INV-库存存酒 | `inventory.stock.adjust` | 已在事件矩阵正文出现 |
| `inventory.counted` | success | `createWasteStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/waste` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
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
| `inventory.item.high_value_mark_failed` | failure | `markInventoryItemHighValue` | `POST` | `/api/v1/inventory/items/{id}/high-value-mark` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.item.high_value_marked` | success | `markInventoryItemHighValue` | `POST` | `/api/v1/inventory/items/{id}/high-value-mark` | INV-库存存酒 | `inventory.high_value.review` | 接口级事件，以本附录为准 |
| `inventory.item.safety_stock_update_failed` | failure | `updateInventoryItemSafetyStock` | `POST` | `/api/v1/inventory/items/{id}/safety-stock` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.item.safety_stock_updated` | success | `updateInventoryItemSafetyStock` | `POST` | `/api/v1/inventory/items/{id}/safety-stock` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.item.viewed` | success | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.item.viewed_failed` | failure | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.outbound_verified` | success | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.move` | 已在事件矩阵正文出现 |
| `inventory.purchase_request_view_failed` | failure | `getPurchaseWorkflowDetail` | `GET` | `/api/v1/inventory/purchase-requests/{id}` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_view_failed` | failure | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_viewed` | success | `getPurchaseWorkflowDetail` | `GET` | `/api/v1/inventory/purchase-requests/{id}` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_request_viewed` | success | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_suggestion.view_failed` | failure | `getPurchaseSuggestions` | `GET` | `/api/v1/inventory/purchase-suggestions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase_suggestion.viewed` | success | `getPurchaseSuggestions` | `GET` | `/api/v1/inventory/purchase-suggestions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase_workbench.view_failed` | failure | `getPurchaseWorkbenchOptions` | `GET` | `/api/v1/inventory/purchase-workbench-options` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase_workbench.viewed` | success | `getPurchaseWorkbenchOptions` | `GET` | `/api/v1/inventory/purchase-workbench-options` | INV-库存存酒 | `inventory.purchase_request.view` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `allocatePurchasePrepayment` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-allocations` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `confirmPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-consignment-sales/{id}/confirm` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `handoverPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-returns/{id}/handover` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `postPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-prepayment-resolutions/{id}/post` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `registerPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-requests/{id}/consignment-sales` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `requestPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-resolutions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `requestPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-requests/{id}/returns` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `resolvePurchaseQuantity` | `POST` | `/api/v1/inventory/purchase-requests/{id}/resolutions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `reviewPurchaseReturnFinance` | `POST` | `/api/v1/inventory/purchase-returns/{id}/finance-review` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.completed` | success | `saveSupplyContractTerms` | `POST` | `/api/v1/inventory/supply-contracts/{id}/terms` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `allocatePurchasePrepayment` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-allocations` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `confirmPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-consignment-sales/{id}/confirm` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `handoverPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-returns/{id}/handover` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `postPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-prepayment-resolutions/{id}/post` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `registerPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-requests/{id}/consignment-sales` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `requestPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-resolutions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `requestPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-requests/{id}/returns` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `resolvePurchaseQuantity` | `POST` | `/api/v1/inventory/purchase-requests/{id}/resolutions` | INV-库存存酒 | `inventory.purchase_request.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `reviewPurchaseReturnFinance` | `POST` | `/api/v1/inventory/purchase-returns/{id}/finance-review` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.purchase.failed` | failure | `saveSupplyContractTerms` | `POST` | `/api/v1/inventory/supply-contracts/{id}/terms` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
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
| `inventory.supply_contract.view_failed` | failure | `getSupplyContractTerms` | `GET` | `/api/v1/inventory/supply-contracts/{id}/terms` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_contract.view_failed` | failure | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_contract.viewed` | success | `getSupplyContractTerms` | `GET` | `/api/v1/inventory/supply-contracts/{id}/terms` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_contract.viewed` | success | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | INV-库存存酒 | `inventory.supply_contract.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_entity_change_failed` | failure | `createSupplyEntity` | `POST` | `/api/v1/inventory/supply-entities` | INV-库存存酒 | `inventory.supply_entity.manage` | 接口级事件，以本附录为准 |
| `inventory.supply_entity_changed` | success | `createSupplyEntity` | `POST` | `/api/v1/inventory/supply-entities` | INV-库存存酒 | `inventory.supply_entity.manage` | 接口级事件，以本附录为准 |
| `inventory.trace_code.list_failed` | failure | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.listed` | success | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.print_failed` | failure | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | INV-库存存酒 | `inventory.trace_code.print` | 接口级事件，以本附录为准 |
| `inventory.trace_code.printed` | success | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | INV-库存存酒 | `inventory.trace_code.print` | 接口级事件，以本附录为准 |
| `inventory.trace_code.register_failed` | failure | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.register` | 接口级事件，以本附录为准 |
| `inventory.trace_code.registered` | success | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | INV-库存存酒 | `inventory.trace_code.register` | 接口级事件，以本附录为准 |
| `inventory.trace_code.view_failed` | failure | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.trace_code.viewed` | success | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | INV-库存存酒 | `inventory.trace_code.view` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposal_complete_failed` | failure | `completeTransferDisposal` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/disposal-completed` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposal_completed` | success | `completeTransferDisposal` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/disposal-completed` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposition_register_failed` | failure | `registerTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposition_registered` | success | `registerTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposition_review_failed` | failure | `reviewTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/review` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_disposition_reviewed` | success | `reviewTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/review` | INV-库存存酒 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_fulfillment_view_failed` | failure | `getTransferOrderFulfillment` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `inventory.transfer_fulfillment_viewed` | success | `getTransferOrderFulfillment` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_cancel_failed` | failure | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_cancelled` | success | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_create_failed` | failure | `createTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_created` | success | `createTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_deliver_failed` | failure | `deliverTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/deliver` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_delivered` | success | `deliverTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/deliver` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_draft_edit_failed` | failure | `editTransferOrderDraft` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/lines` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_draft_edited` | success | `editTransferOrderDraft` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/lines` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_pick_failed` | failure | `pickTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/pick` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_picked` | success | `pickTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/pick` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_receive_failed` | failure | `receiveTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/receive` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_received` | success | `receiveTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/receive` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_return_failed` | failure | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_returned` | success | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_ship_failed` | failure | `shipTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/ship` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_shipped` | success | `shipTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/ship` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_submit_failed` | failure | `submitTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/submit` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_submitted` | success | `submitTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/submit` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_supplier_review_failed` | failure | `supplierReviewTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/supplier-review` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_order_supplier_reviewed` | success | `supplierReviewTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/supplier-review` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_unreturnable_confirm_failed` | failure | `confirmTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/confirm` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_unreturnable_confirmed` | success | `confirmTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/confirm` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_unreturnable_register_failed` | failure | `registerTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/unreturnable` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.transfer_unreturnable_registered` | success | `registerTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/unreturnable` | INV-库存存酒 | `inventory.transfer_order.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse_location.create_failed` | failure | `createWarehouseLocation` | `POST` | `/api/v1/inventory/warehouse-locations` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse_location.created` | success | `createWarehouseLocation` | `POST` | `/api/v1/inventory/warehouse-locations` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse_location.list` | success | `listWarehouseLocations` | `GET` | `/api/v1/inventory/warehouse-locations` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse_location.list_failed` | failure | `listWarehouseLocations` | `GET` | `/api/v1/inventory/warehouse-locations` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.create_failed` | failure | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.created` | success | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.list` | success | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.list_failed` | failure | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.pour_device_update_failed` | failure | `setWarehousePourDevice` | `PUT` | `/api/v1/inventory/warehouses/{id}/pour-device` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.pour_device_updated` | success | `setWarehousePourDevice` | `PUT` | `/api/v1/inventory/warehouses/{id}/pour-device` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `leasing.blacklist_exception.request_failed` | failure | `createLeasingBlacklistException` | `POST` | `/api/v1/leasing/blacklist-exceptions` | P0-待归类 | `leasing.brand.blacklist_exception` | 接口级事件，以本附录为准 |
| `leasing.blacklist_exception.requested` | success | `createLeasingBlacklistException` | `POST` | `/api/v1/leasing/blacklist-exceptions` | P0-待归类 | `leasing.brand.blacklist_exception` | 接口级事件，以本附录为准 |
| `leasing.blacklist_exception.viewed` | success | `listLeasingBlacklistExceptions` | `GET` | `/api/v1/leasing/blacklist-exceptions` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.blacklist_change_failed` | failure | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | P0-待归类 | `leasing.brand.blacklist` | 接口级事件，以本附录为准 |
| `leasing.brand.blacklist_changed` | success | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | P0-待归类 | `leasing.brand.blacklist` | 接口级事件，以本附录为准 |
| `leasing.brand.create_failed` | failure | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.created` | success | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.brand.viewed` | success | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.create_failed` | failure | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.follow_up.created` | success | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.advance_failed` | failure | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.advanced` | success | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.create_failed` | failure | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.created` | success | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.intent.sign_failed` | failure | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | P0-待归类 | `leasing.convert` | 接口级事件，以本附录为准 |
| `leasing.intent.signed` | success | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | P0-待归类 | `leasing.convert` | 接口级事件，以本附录为准 |
| `leasing.intent.viewed` | success | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.assign_failed` | failure | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.assigned` | success | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.contact_phone_view_failed` | failure | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.contact_phone_viewed` | success | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.create_failed` | failure | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.created` | success | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.status_change_failed` | failure | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.status_changed` | success | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `leasing.lead.viewed` | success | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `master_auth.challenge_rate_limited` | failure | `issueMasterAuthChallenge` | `POST` | `/api/v1/auth/master/challenge` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
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
| `mdm.violation.handle_failed` | failure | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | TERM-终端设备 | `mdm.violation.handle` | 接口级事件，以本附录为准 |
| `mdm.violation.handled` | success | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | TERM-终端设备 | `mdm.violation.handle` | 接口级事件，以本附录为准 |
| `mdm.violation.view_failed` | failure | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `mdm.violation.viewed` | success | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `media_fulfillment.confirm_failed` | failure | `confirmMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/confirm` | P0-待归类 | `media.performance.fulfillment.confirm` | 接口级事件，以本附录为准 |
| `media_fulfillment.confirmed` | success | `confirmMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/confirm` | P0-待归类 | `media.performance.fulfillment.confirm` | 接口级事件，以本附录为准 |
| `media_fulfillment.dispute_failed` | failure | `disputeMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute` | P0-待归类 | `media.performance.fulfillment.confirm` | 接口级事件，以本附录为准 |
| `media_fulfillment.dispute_resolve_failed` | failure | `resolveMediaPerformanceDispute` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute/resolve` | P0-待归类 | `media.performance.dispute.resolve` | 接口级事件，以本附录为准 |
| `media_fulfillment.dispute_resolved` | success | `resolveMediaPerformanceDispute` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute/resolve` | P0-待归类 | `media.performance.dispute.resolve` | 接口级事件，以本附录为准 |
| `media_fulfillment.disputed` | success | `disputeMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute` | P0-待归类 | `media.performance.fulfillment.confirm` | 接口级事件，以本附录为准 |
| `media_fulfillment.record_failed` | failure | `recordMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment` | P0-待归类 | `media.performance.fulfillment.record` | 接口级事件，以本附录为准 |
| `media_fulfillment.recorded` | success | `recordMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment` | P0-待归类 | `media.performance.fulfillment.record` | 接口级事件，以本附录为准 |
| `media_settlement.adjust_failed` | failure | `adjustMediaPerformanceSettlement` | `POST` | `/api/v1/settlement/media-shows/{id}/settlements/{settlementId}/adjust` | P0-待归类 | `media.performance.settle` | 接口级事件，以本附录为准 |
| `media_settlement.adjusted` | success | `adjustMediaPerformanceSettlement` | `POST` | `/api/v1/settlement/media-shows/{id}/settlements/{settlementId}/adjust` | P0-待归类 | `media.performance.settle` | 接口级事件，以本附录为准 |
| `media_settlement.settle_failed` | failure | `settleMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/settle` | P0-待归类 | `media.performance.settle` | 接口级事件，以本附录为准 |
| `media_settlement.settled` | success | `settleMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/settle` | P0-待归类 | `media.performance.settle` | 接口级事件，以本附录为准 |
| `media_show.create_failed` | failure | `createMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.created` | success | `createMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.list_failed` | failure | `listMediaPerformanceShows` | `GET` | `/api/v1/settlement/media-shows` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `media_show.listed` | success | `listMediaPerformanceShows` | `GET` | `/api/v1/settlement/media-shows` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `media_show.lock_failed` | failure | `lockMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/lock` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.locked` | success | `lockMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/lock` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.update_failed` | failure | `updateMediaPerformanceShow` | `PATCH` | `/api/v1/settlement/media-shows/{id}` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.updated` | success | `updateMediaPerformanceShow` | `PATCH` | `/api/v1/settlement/media-shows/{id}` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.version_create_failed` | failure | `createMediaPerformanceShowVersion` | `POST` | `/api/v1/settlement/media-shows/{id}/versions` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.version_created` | success | `createMediaPerformanceShowVersion` | `POST` | `/api/v1/settlement/media-shows/{id}/versions` | P0-待归类 | `media.performance.contract.manage` | 接口级事件，以本附录为准 |
| `media_show.view_failed` | failure | `getMediaPerformanceShow` | `GET` | `/api/v1/settlement/media-shows/{id}` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `media_show.viewed` | success | `getMediaPerformanceShow` | `GET` | `/api/v1/settlement/media-shows/{id}` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `member.level_changed` | success | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `member.level_notice.read` | success | `markCustomerLevelNoticeRead` | `POST` | `/api/v1/customer/level-notices/{notice_id}/read` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `member.level_notice.read_failed` | failure | `markCustomerLevelNoticeRead` | `POST` | `/api/v1/customer/level-notices/{notice_id}/read` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
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
| `merchant_portal.fee_statement.view_failed` | failure | `getMerchantPortalFeeStatement` | `GET` | `/api/v1/merchant-portal/fee-statement` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `merchant_portal.fee_statement.viewed` | success | `getMerchantPortalFeeStatement` | `GET` | `/api/v1/merchant-portal/fee-statement` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `merchant_portal.payee_account.approve_failed` | failure | `approveMerchantPortalPayeeAccountChange` | `POST` | `/api/v1/merchant-portal/ticket-settlements/{ar_ap_item_id}/account-change` | P0-待归类 | `merchant.portal.payee_account.approve` | 接口级事件，以本附录为准 |
| `merchant_portal.payee_account.approved` | success | `approveMerchantPortalPayeeAccountChange` | `POST` | `/api/v1/merchant-portal/ticket-settlements/{ar_ap_item_id}/account-change` | P0-待归类 | `merchant.portal.payee_account.approve` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.dispute_failed` | failure | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.disputed` | success | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.resolution_reject_failed` | failure | `rejectMerchantPortalSettlementResolution` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/reject-resolution` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
| `merchant_portal.settlement.resolution_rejected` | success | `rejectMerchantPortalSettlementResolution` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/reject-resolution` | P0-待归类 | `merchant.portal.settlement.dispute` | 接口级事件，以本附录为准 |
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
| `merchant_service.listMerchantServiceWorkOrders.succeeded` | success | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `merchant_service.work_order_close_method_save_failed` | failure | `saveMerchantServiceWorkOrderCloseMethod` | `PUT` | `/api/v1/merchant-service/work-order-close-methods` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.work_order_close_method_saved` | success | `saveMerchantServiceWorkOrderCloseMethod` | `PUT` | `/api/v1/merchant-service/work-order-close-methods` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.work_order_policy_save_failed` | failure | `saveMerchantServiceWorkOrderPolicy` | `PUT` | `/api/v1/merchant-service/work-order-policies` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.work_order_policy_saved` | success | `saveMerchantServiceWorkOrderPolicy` | `PUT` | `/api/v1/merchant-service/work-order-policies` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.archived` | success | `archiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/archive` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.bearer_dispute_failed` | failure | `disputeMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/bearer-dispute` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.bearer_disputed` | success | `disputeMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/bearer-dispute` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cancel_failed` | failure | `cancelMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cancel` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cancel_failed` | failure | `cancelMerchantServiceWorkOrderByStaff` | `POST` | `/api/v1/merchant-service/work-orders/{id}/staff-cancel` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cancelled` | success | `cancelMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cancel` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cancelled` | success | `cancelMerchantServiceWorkOrderByStaff` | `POST` | `/api/v1/merchant-service/work-orders/{id}/staff-cancel` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cost_bearer_mark_failed` | failure | `markMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cost-bearer` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.cost_bearer_marked` | success | `markMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cost-bearer` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.delete_approved` | success | `approveDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-approve` | P0-待归类 | `merchant.service_work_order.approve` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.delete_requested` | success | `requestDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-request` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.material_issue_failed` | failure | `issueMerchantServiceWorkOrderMaterial` | `POST` | `/api/v1/merchant-service/work-orders/{id}/materials` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.material_issued` | success | `issueMerchantServiceWorkOrderMaterial` | `POST` | `/api/v1/merchant-service/work-orders/{id}/materials` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.merchant_confirm_failed` | failure | `confirmMerchantServiceWorkOrderByMerchant` | `POST` | `/api/v1/merchant-service/work-orders/{id}/merchant-confirm` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.merchant_confirmed` | success | `confirmMerchantServiceWorkOrderByMerchant` | `POST` | `/api/v1/merchant-service/work-orders/{id}/merchant-confirm` | P0-待归类 | `merchant.service_work_order.create` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.unarchived` | success | `unarchiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/unarchive` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `merchant_service.work_order.viewed` | success | `listMyMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders/assigned-to-me` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `merchant.archive_failed` | failure | `archiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/archive` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.archived` | success | `archiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/archive` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.billing.confirm` | success | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | P0-待归类 | `merchant.billing.confirm` | 接口级事件，以本附录为准 |
| `merchant.billing.confirm_failed` | failure | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | P0-待归类 | `merchant.billing.confirm` | 接口级事件，以本附录为准 |
| `merchant.billing.create` | success | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `merchant.billing.create_failed` | failure | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `merchant.billing.dispute.submit` | success | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | P0-待归类 | `merchant.billing.dispute.submit` | 接口级事件，以本附录为准 |
| `merchant.billing.dispute.submit_failed` | failure | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | P0-待归类 | `merchant.billing.dispute.submit` | 接口级事件，以本附录为准 |
| `merchant.billing.dunning.record` | success | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | P0-待归类 | `merchant.billing.dunning.record` | 接口级事件，以本附录为准 |
| `merchant.billing.dunning.record_failed` | failure | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | P0-待归类 | `merchant.billing.dunning.record` | 接口级事件，以本附录为准 |
| `merchant.delete_approve_failed` | failure | `approveDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-approve` | GOV-平台治理 | `merchant.profile.approve` | 接口级事件，以本附录为准 |
| `merchant.delete_approved` | success | `approveDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-approve` | GOV-平台治理 | `merchant.profile.approve` | 接口级事件，以本附录为准 |
| `merchant.delete_request_failed` | failure | `requestDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-request` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.delete_requested` | success | `requestDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-request` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.operating_project.request_failed` | failure | `requestMerchantOperatingProject` | `POST` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.operating_project.requested` | success | `requestMerchantOperatingProject` | `POST` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.penalty_bounds_update_failed` | failure | `saveMerchantPenaltyBounds` | `PUT` | `/api/v1/merchant-penalty-bounds/{project_id}` | ANA-风控分析 | `merchant.penalty.bounds.manage` | 接口级事件，以本附录为准 |
| `merchant.penalty_bounds_updated` | success | `saveMerchantPenaltyBounds` | `PUT` | `/api/v1/merchant-penalty-bounds/{project_id}` | ANA-风控分析 | `merchant.penalty.bounds.manage` | 接口级事件，以本附录为准 |
| `merchant.penalty_record_failed` | failure | `recordMerchantPenalty` | `POST` | `/api/v1/merchant-penalties` | ANA-风控分析 | `merchant.penalty.record` | 接口级事件，以本附录为准 |
| `merchant.penalty_recorded` | success | `recordMerchantPenalty` | `POST` | `/api/v1/merchant-penalties` | ANA-风控分析 | `merchant.penalty.record` | 接口级事件，以本附录为准 |
| `merchant.pending_clear_alert_config_failed` | failure | `configureMerchantPendingClearAlert` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.pending_clear_alert_configured` | success | `configureMerchantPendingClearAlert` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.pour_loss_rate_config_failed` | failure | `configureMerchantPourLossRate` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.pour_loss_rate_configured` | success | `configureMerchantPourLossRate` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.project_assign_failed` | failure | `assignMerchantProject` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/project` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.project_assigned` | success | `assignMerchantProject` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/project` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.reward.customer.viewed` | success | `getOwnMerchantConsumptionRewards` | `GET` | `/api/v1/customer/merchant-rewards` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `merchant.reward.customer.viewed.failed` | failure | `getOwnMerchantConsumptionRewards` | `GET` | `/api/v1/customer/merchant-rewards` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `merchant.staff_booking_no_show_limit_config_failed` | failure | `configureMerchantStaffBookingNoShowLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.staff_booking_no_show_limit_configured` | success | `configureMerchantStaffBookingNoShowLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.table_transfer_limit_config_failed` | failure | `configureMerchantTableTransferLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.table_transfer_limit_configured` | success | `configureMerchantTableTransferLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.unarchive_failed` | failure | `unarchiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/unarchive` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.unarchived` | success | `unarchiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/unarchive` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.update_failed` | failure | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.update_failed` | failure | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.view_failed` | failure | `getMerchantOperatingProjects` | `GET` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.view_failed` | failure | `getMerchantPourLossRate` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.view_failed` | failure | `getMerchantStaffBookingNoShowLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `getMerchantOperatingProjects` | `GET` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `getMerchantPendingClearAlert` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `getMerchantPourLossRate` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `getMerchantStaffBookingNoShowLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `getMerchantTableTransferLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `approveDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-approve` | P0-待归类 | `facility.equipment.approve` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `approveDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-approve` | P0-待归类 | `merchant.service_work_order.approve` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `approveDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-approve` | GOV-平台治理 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `approveDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-approve` | AUTH-账号权限 | `staff.profile.approve` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `archiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/archive` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/archive` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `archiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/archive` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/archive` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `archiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/archive` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/archive` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `archiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/archive` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/archive` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/archive` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/archive` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/archive` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `archiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/archive` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `countMediaPerformancePendingSettlements` | `GET` | `/api/v1/settlement/media-shows/pending-settlement-count` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `countMediaPerformancePendingSettlements` | `GET` | `/api/v1/settlement/media-shows/pending-settlement-count` | P0-待归类 | `media.performance.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `createAmusementExitVerification` | `POST` | `/api/v1/amusement/verifications/exit` | AMU-游乐安全 | `amusement.verification.execute` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `createAmusementExitVerification` | `POST` | `/api/v1/amusement/verifications/exit` | AMU-游乐安全 | `amusement.verification.execute` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `createPartnerVoucherConfig` | `POST` | `/api/v1/ticketing/partner-vouchers/configs` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `createPartnerVoucherConfig` | `POST` | `/api/v1/ticketing/partner-vouchers/configs` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `createTicketDistributionMerchantRate` | `POST` | `/api/v1/ticketing/distribution-merchant-rates` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `createTicketDistributionMerchantRate` | `POST` | `/api/v1/ticketing/distribution-merchant-rates` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `deactivateMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients/{id}/deactivate` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `deactivateMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients/{id}/deactivate` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | P0-待归类 | `dealer_portal.stock.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | P0-待归类 | `dealer_portal.inquiry.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | P0-待归类 | `dealer_portal.order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | P0-待归类 | `dealer_portal.order.pay` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | P0-待归类 | `dealer_portal.return.request` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | P0-待归类 | `dealer_portal.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `deleteSpaceType` | `DELETE` | `/api/v1/platform/space-types/{id}` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `deleteSpaceType` | `DELETE` | `/api/v1/platform/space-types/{id}` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `findCustomerParkingSession` | `GET` | `/api/v1/customer/parking/sessions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `findCustomerParkingSession` | `GET` | `/api/v1/customer/parking/sessions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `forwardSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/forward` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getAccessGrantPolicy` | `GET` | `/api/v1/access/grant-policy` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getAccessGrantPolicy` | `GET` | `/api/v1/access/grant-policy` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getBarArrivalReminderPolicy` | `GET` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getBarArrivalReminderPolicy` | `GET` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getBarCatalogImage` | `GET` | `/api/v1/bar/catalog/images/{name}` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getBarCatalogImage` | `GET` | `/api/v1/bar/catalog/images/{name}` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | BAR-酒吧经营 | `bar.reservation.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getComplaintFollowup` | `GET` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getComplaintFollowup` | `GET` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getContractDepositMonths` | `GET` | `/api/v1/contracts/deposit-months` | P0-待归类 | `contract.deposit_months.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getContractDepositMonths` | `GET` | `/api/v1/contracts/deposit-months` | P0-待归类 | `contract.deposit_months.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getContractDepositRefundWait` | `GET` | `/api/v1/contracts/deposit-refund-wait` | P0-待归类 | `contract.deposit_refund_wait.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getContractDepositRefundWait` | `GET` | `/api/v1/contracts/deposit-refund-wait` | P0-待归类 | `contract.deposit_refund_wait.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerBarTableCurrentBill` | `GET` | `/api/v1/customer/bar-tables/{table_id}/current-bill` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerBarTableCurrentBill` | `GET` | `/api/v1/customer/bar-tables/{table_id}/current-bill` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerChurnAlertSetting` | `GET` | `/api/v1/customer-churn-alert-setting` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerChurnAlertSetting` | `GET` | `/api/v1/customer-churn-alert-setting` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerHomeContent` | `GET` | `/api/v1/home-contents/{project_id}` | CUS-客人端 | `customer.home_content.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerHomeContent` | `GET` | `/api/v1/home-contents/{project_id}` | CUS-客人端 | `customer.home_content.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerParkingCredit` | `GET` | `/api/v1/customer/parking/credits` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerRestrictionSetting` | `GET` | `/api/v1/customer-restriction-settings/{project_id}` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerRestrictionSetting` | `GET` | `/api/v1/customer-restriction-settings/{project_id}` | CUS-客人端 | `customer.account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerSession` | `GET` | `/api/v1/customer/session` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerSession` | `GET` | `/api/v1/customer/session` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerStoredValueTopupRules` | `GET` | `/api/v1/customer/stored-value/topup-rules` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerStoredValueTopupRules` | `GET` | `/api/v1/customer/stored-value/topup-rules` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getCustomerTerms` | `GET` | `/api/v1/customer/terms` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getCustomerTerms` | `GET` | `/api/v1/customer/terms` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getFacilityWorkOrderMaterials` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/materials` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getFacilityWorkOrderMaterials` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/materials` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getInternalPayableExecution` | `GET` | `/api/v1/finance/internal-payables/{ar_ap_item_id}` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getInternalPayableExecution` | `GET` | `/api/v1/finance/internal-payables/{ar_ap_item_id}` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getManualBackfillPolicy` | `GET` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | EVD-事件证据 | `evidence.manual_backfill.policy.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getManualBackfillPolicy` | `GET` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | EVD-事件证据 | `evidence.manual_backfill.policy.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMerchantCardExitNoticePolicy` | `GET` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantCardExitNoticePolicy` | `GET` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMerchantFeeNotificationRecipient` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients/{id}` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantFeeNotificationRecipient` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients/{id}` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantPendingClearAlert` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMerchantPortalUnreadBills` | `GET` | `/api/v1/merchant-portal/unread-bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantPortalUnreadBills` | `GET` | `/api/v1/merchant-portal/unread-bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMerchantServiceWorkOrderMaterials` | `GET` | `/api/v1/merchant-service/work-orders/{id}/materials` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantServiceWorkOrderMaterials` | `GET` | `/api/v1/merchant-service/work-orders/{id}/materials` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMerchantTableTransferLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMyStaffProfile` | `GET` | `/api/v1/staff/profiles/mine` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMyStaffProfile` | `GET` | `/api/v1/staff/profiles/mine` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getMyStudentCertification` | `GET` | `/api/v1/customer/student-certifications/current` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getMyStudentCertification` | `GET` | `/api/v1/customer/student-certifications/current` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getParkingOverview` | `GET` | `/api/v1/parking/overview` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getParkingOverview` | `GET` | `/api/v1/parking/overview` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getParkingPolicy` | `GET` | `/api/v1/parking/policies/{project_id}` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getParkingPolicy` | `GET` | `/api/v1/parking/policies/{project_id}` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | EVD-事件证据 | `risk_alert.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getSettlementObjectionPeriod` | `GET` | `/api/v1/settlement/internal-pay-days/objection-period` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getSettlementObjectionPeriod` | `GET` | `/api/v1/settlement/internal-pay-days/objection-period` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getSupervisorReviewChain` | `GET` | `/api/v1/platform/supervisor-review/{review_id}/chain` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getSupervisorReviewChain` | `GET` | `/api/v1/platform/supervisor-review/{review_id}/chain` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getSystemHealth` | `GET` | `/api/v1/system/health` | SYS-系统健康 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getSystemHealth` | `GET` | `/api/v1/system/health` | SYS-系统健康 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getSystemVersion` | `GET` | `/api/v1/system/version` | SYS-系统健康 | `system.version.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getSystemVersion` | `GET` | `/api/v1/system/version` | SYS-系统健康 | `system.version.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getTicketExpiryReminderPolicy` | `GET` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getTicketExpiryReminderPolicy` | `GET` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getTraceHandover` | `GET` | `/api/v1/inventory/trace-handovers/{id}` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getTraceHandover` | `GET` | `/api/v1/inventory/trace-handovers/{id}` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getTransferOrder` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getTransferOrder` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `importPartnerVoucherRedemption` | `POST` | `/api/v1/ticketing/partner-vouchers/redemptions` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `importPartnerVoucherRedemption` | `POST` | `/api/v1/ticketing/partner-vouchers/redemptions` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | AUTH-账号权限 | `access.grant_request.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAccessUsers` | `GET` | `/api/v1/access/users` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementCompensationBatchesByEvent` | `GET` | `/api/v1/amusement/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | AMU-游乐安全 | `amusement.incident.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementParkWideFaults` | `GET` | `/api/v1/amusement/park-wide-faults` | AMU-游乐安全 | `amusement.park_fault.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listAmusementSafetyNotices` | `GET` | `/api/v1/amusement/safety-notices` | AMU-游乐安全 | `amusement.safety_notice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listApprovalDeadlines` | `GET` | `/api/v1/platform/approval-deadlines` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listApprovalDeadlines` | `GET` | `/api/v1/platform/approval-deadlines` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | BAR-酒吧经营 | `bar.combo.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | BAR-酒吧经营 | `bar.category.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | BAR-酒吧经营 | `bar.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | BAR-酒吧经营 | `bar.order_item.void` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listComplianceItemTypeConfigs` | `GET` | `/api/v1/compliance/item-type-configs` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listComplianceItemTypeConfigs` | `GET` | `/api/v1/compliance/item-type-configs` | P0-待归类 | `compliance.expiry_item.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | P0-待归类 | `compliance.reminder.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | P0-待归类 | `contract.approval.flow.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | P0-待归类 | `contract.change.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | P0-待归类 | `compliance.contract_key_date.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listContracts` | `GET` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContracts` | `GET` | `/api/v1/contracts` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | P0-待归类 | `contract.settlement.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerAmusementProjects` | `GET` | `/api/v1/customer/amusement/projects` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerAmusementProjects` | `GET` | `/api/v1/customer/amusement/projects` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-tables/{table_id}/menu` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-tables/{table_id}/menu` | CUS/BAR-客人订台 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerBlacklistRequests` | `GET` | `/api/v1/customer-blacklist-requests` | CUS-客人端 | `customer.blacklist.request` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerComplaintStoreOptions` | `GET` | `/api/v1/customer/complaints/store-options` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerComplaintStoreOptions` | `GET` | `/api/v1/customer/complaints/store-options` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerEffectiveTerminalSwitches` | `GET` | `/api/v1/customer/terminal-switches/effective` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerEffectiveTerminalSwitches` | `GET` | `/api/v1/customer/terminal-switches/effective` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerLevelNotices` | `GET` | `/api/v1/customer/level-notices` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerLevelNotices` | `GET` | `/api/v1/customer/level-notices` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerMerchantCardExitNotices` | `GET` | `/api/v1/customer/merchant-card-exit-notices` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerMerchantCardExitNotices` | `GET` | `/api/v1/customer/merchant-card-exit-notices` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerProjects` | `GET` | `/api/v1/customer/projects` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerProjects` | `GET` | `/api/v1/customer/projects` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerStoredValueTransactions` | `GET` | `/api/v1/customer/stored-value/accounts/{account_id}/transactions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerStoredValueTransactions` | `GET` | `/api/v1/customer/stored-value/accounts/{account_id}/transactions` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerTicketChannels` | `GET` | `/api/v1/customer/ticket-channels` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerTicketChannels` | `GET` | `/api/v1/customer/ticket-channels` | CUS/TIC-客人购票 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listCustomerTicketExpiryReminders` | `GET` | `/api/v1/customer/ticket-expiry-reminders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listCustomerTicketExpiryReminders` | `GET` | `/api/v1/customer/ticket-expiry-reminders` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDataCleanupJobs` | `GET` | `/api/v1/ops/data-cleanup/jobs` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDataCleanupJobs` | `GET` | `/api/v1/ops/data-cleanup/jobs` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | P0-待归类 | `dealer.order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | P0-待归类 | `dealer.portal_account.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | P0-待归类 | `dealer.return.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listDealers` | `GET` | `/api/v1/dealers` | P0-待归类 | `dealer.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | P0-待归类 | `energy.meter.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | P0-待归类 | `energy.reading.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityAlarmDuties` | `GET` | `/api/v1/facility/alarm-duties` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityAlarmDuties` | `GET` | `/api/v1/facility/alarm-duties` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityAlarms` | `GET` | `/api/v1/facility/alarms` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityAlarms` | `GET` | `/api/v1/facility/alarms` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityEquipmentScraps` | `GET` | `/api/v1/facility/equipment-scraps` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityFaultRules` | `GET` | `/api/v1/facility/fault-rules` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityFaultRules` | `GET` | `/api/v1/facility/fault-rules` | P0-待归类 | `facility.alarm.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | P0-待归类 | `facility.inspection.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listFinanceReconciliationStageLimits` | `GET` | `/api/v1/finance/reconciliation/stage-limits` | FIN-财务台账 | `finance.reconciliation.policy.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFinanceReconciliationStageLimits` | `GET` | `/api/v1/finance/reconciliation/stage-limits` | FIN-财务台账 | `finance.reconciliation.policy.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | P0-待归类 | `floor_plan.import.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listInternalPayDayRules` | `GET` | `/api/v1/settlement/internal-pay-days/rules` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listInternalPayDayRules` | `GET` | `/api/v1/settlement/internal-pay-days/rules` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingBlacklistExceptions` | `GET` | `/api/v1/leasing/blacklist-exceptions` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | P0-待归类 | `leasing.brand.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | P0-待归类 | `leasing.intent.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | P0-待归类 | `leasing.lead.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLoyaltyConfigBirthdayRules` | `GET` | `/api/v1/loyalty/config/birthday-rules` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLoyaltyConfigCheckInTiers` | `GET` | `/api/v1/loyalty/config/check-in-tiers` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listLoyaltyConfigMemberLevels` | `GET` | `/api/v1/loyalty/config/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listManualBackfills` | `GET` | `/api/v1/evidence/manual-backfills` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listManualBackfills` | `GET` | `/api/v1/evidence/manual-backfills` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantFeeNotificationRecipients` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantFeeNotificationRecipients` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantPenalties` | `GET` | `/api/v1/merchant-penalties` | ANA-风控分析 | `merchant.penalty.record` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantPenalties` | `GET` | `/api/v1/merchant-penalties` | ANA-风控分析 | `merchant.penalty.record` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantPortalNotices` | `GET` | `/api/v1/merchant-portal/notices` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantPortalNotices` | `GET` | `/api/v1/merchant-portal/notices` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantPortalTicketSettlements` | `GET` | `/api/v1/merchant-portal/ticket-settlements` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantPortalTicketSettlements` | `GET` | `/api/v1/merchant-portal/ticket-settlements` | P0-待归类 | `merchant.portal.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantServiceWorkOrderCloseMethods` | `GET` | `/api/v1/merchant-service/work-order-close-methods` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantServiceWorkOrderCloseMethods` | `GET` | `/api/v1/merchant-service/work-order-close-methods` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantServiceWorkOrderOwnerCandidates` | `GET` | `/api/v1/merchant-service/work-orders/{id}/owner-candidates` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantServiceWorkOrderOwnerCandidates` | `GET` | `/api/v1/merchant-service/work-orders/{id}/owner-candidates` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMerchantServiceWorkOrderPolicies` | `GET` | `/api/v1/merchant-service/work-order-policies` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantServiceWorkOrderPolicies` | `GET` | `/api/v1/merchant-service/work-order-policies` | P0-待归类 | `merchant.service_work_order.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | P0-待归类 | `merchant.service_work_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyExports` | `GET` | `/api/v1/security/my-exports` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyExports` | `GET` | `/api/v1/security/my-exports` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyFacilityAlarms` | `GET` | `/api/v1/facility/alarms/mine` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyFacilityAlarms` | `GET` | `/api/v1/facility/alarms/mine` | P0-待归类 | `facility.alarm.take_over` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyMerchantCardExitNotices` | `GET` | `/api/v1/merchant-portal/card-exit-notices` | P0-待归类 | `merchant.portal.card_exit_notice.publish` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyMerchantCardExitNotices` | `GET` | `/api/v1/merchant-portal/card-exit-notices` | P0-待归类 | `merchant.portal.card_exit_notice.publish` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders/assigned-to-me` | P0-待归类 | `merchant.service_work_order.process` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyStaffPayrollCommissions` | `GET` | `/api/v1/staff/payroll/mine/commissions` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyStaffPayrollCommissions` | `GET` | `/api/v1/staff/payroll/mine/commissions` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listOrgHierarchy` | `GET` | `/api/v1/access/org-hierarchy` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listOrgHierarchy` | `GET` | `/api/v1/access/org-hierarchy` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listParkingCredits` | `GET` | `/api/v1/parking/credits` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listParkingCredits` | `GET` | `/api/v1/parking/credits` | P0-待归类 | `parking.ops.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listParkingManualReleases` | `GET` | `/api/v1/parking/manual-releases` | P0-待归类 | `parking.release.record` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listParkingManualReleases` | `GET` | `/api/v1/parking/manual-releases` | P0-待归类 | `parking.release.record` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listParkingMonthlyVehicles` | `GET` | `/api/v1/parking/monthly-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listParkingMonthlyVehicles` | `GET` | `/api/v1/parking/monthly-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listParkingStaffVehicles` | `GET` | `/api/v1/parking/staff-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listParkingStaffVehicles` | `GET` | `/api/v1/parking/staff-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPartnerSubordinates` | `GET` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listPendingSupervisorReviews` | `GET` | `/api/v1/platform/supervisor-review/pending` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPendingSupervisorReviews` | `GET` | `/api/v1/platform/supervisor-review/pending` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPermissions` | `GET` | `/api/v1/access/permissions` | AUTH-账号权限 | `access.permission.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listProjects` | `GET` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | FIN-财务台账 | `finance.property_fee.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listServiceOrders` | `GET` | `/api/v1/services/orders` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listServiceOrders` | `GET` | `/api/v1/services/orders` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSettlementBills` | `GET` | `/api/v1/settlement/bills` | P0-待归类 | `finance.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSettlementBills` | `GET` | `/api/v1/settlement/bills` | P0-待归类 | `finance.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSettlementTransactions` | `GET` | `/api/v1/settlement/transactions` | P0-待归类 | `finance.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSettlementTransactions` | `GET` | `/api/v1/settlement/transactions` | P0-待归类 | `finance.settlement.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | P0-待归类 | `facility.site_entry.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | P0-待归类 | `contract.space.color.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | P0-待归类 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSpaces` | `GET` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listStaffAppFunctions` | `GET` | `/api/v1/terminals/staff-app-functions` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffAppFunctions` | `GET` | `/api/v1/terminals/staff-app-functions` | P0-待归类 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | STAFF-员工 | `staff.attendance.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | STAFF-员工 | `staff.attendance.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | P0-待归类 | `partner.subordinate.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listSupplyEntities` | `GET` | `/api/v1/inventory/supply-entities` | INV-库存存酒 | `inventory.supply_entity.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listSupplyEntities` | `GET` | `/api/v1/inventory/supply-entities` | INV-库存存酒 | `inventory.supply_entity.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listTraceCodeOutboundOrders` | `GET` | `/api/v1/inventory/trace-codes/{id}/outbound-orders` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listTraceCodeOutboundOrders` | `GET` | `/api/v1/inventory/trace-codes/{id}/outbound-orders` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listTraceHandovers` | `GET` | `/api/v1/inventory/trace-handovers` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listTraceHandovers` | `GET` | `/api/v1/inventory/trace-handovers` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listTransferOrders` | `GET` | `/api/v1/inventory/transfer-orders` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listTransferOrders` | `GET` | `/api/v1/inventory/transfer-orders` | INV-库存存酒 | `inventory.transfer_order.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listVenueBusinessHours` | `GET` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | P0-待归类 | `wecom.risk_hit.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | P0-待归类 | `wecom.risk_hit.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | P0-待归类 | `facility.work_order.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | P0-待归类 | `facility.work_order.view_assigned` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `markStaffPayrollPaidLegacy` | `POST` | `/api/v1/staff/payroll/pay` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | STAFF-员工 | `staff.shift.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `previewDataCleanup` | `POST` | `/api/v1/ops/data-cleanup/preview` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `previewDataCleanup` | `POST` | `/api/v1/ops/data-cleanup/preview` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | P0-待归类 | `audit.operation_log.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | P0-待归类 | `audit.operation_log.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `readFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media/{media_id}/file` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `readFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media/{media_id}/file` | P0-待归类 | `facility.work_order.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `readStudentCertificationFile` | `GET` | `/api/v1/student-certifications/{certification_id}/files/{index}` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `readStudentCertificationFile` | `GET` | `/api/v1/student-certifications/{certification_id}/files/{index}` | CUS-客人端 | `ticket.student_cert.review` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | P0-待归类 | `dealer.return.refund.execute` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `regenerateSupervisorReviewChain` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/chain/regenerate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `requestDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-request` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `requestDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-request` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `requestDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-request` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `requestDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-request` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `saveMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `saveMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients` | P0-待归类 | `merchant.billing.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `setOrgDirectSuperior` | `PUT` | `/api/v1/access/org-hierarchy/{user_id}` | AUTH-账号权限 | `access.role.assign` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `settlementConfigDistributionScheme` | `GET` | `/api/v1/settlement/config/distribution-scheme` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `settlementConfigDistributionScheme` | `GET` | `/api/v1/settlement/config/distribution-scheme` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | P0-待归类 | `settlement.config.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | P0-待归类 | `settlement.invoice.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `submitDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/submit` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `submitDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/submit` | OPS-生产前置 | `data.cleanup.request` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `unarchiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/unarchive` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/unarchive` | BAR-酒吧经营 | `bar.combo.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `unarchiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/unarchive` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/unarchive` | BAR-酒吧经营 | `bar.product.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `unarchiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/unarchive` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/unarchive` | BAR-酒吧经营 | `bar.category.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `unarchiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/unarchive` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/unarchive` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/unarchive` | P0-待归类 | `facility.equipment.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/unarchive` | P0-待归类 | `merchant.service_work_order.close` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/unarchive` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | failure | `unarchiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/unarchive` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `not_applicable` | success | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `open_mapping.changed` | success | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | INT-第三方集成 | `integration.provider_adapter.update` | 已在事件矩阵正文出现 |
| `operating_params.change_failed` | failure | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `operating_params.changed` | success | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | ANA-风控分析 | `analytics.operating_params.manage` | 接口级事件，以本附录为准 |
| `operation_handover.created` | success | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `operation_handover.failed` | failure | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `parking.credit.granted` | success | `getCustomerParkingCredit` | `GET` | `/api/v1/customer/parking/credits` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `parking.manual_release_record_failed` | failure | `recordParkingManualRelease` | `POST` | `/api/v1/parking/manual-releases` | P0-待归类 | `parking.release.record` | 接口级事件，以本附录为准 |
| `parking.manual_release_recorded` | success | `recordParkingManualRelease` | `POST` | `/api/v1/parking/manual-releases` | P0-待归类 | `parking.release.record` | 接口级事件，以本附录为准 |
| `parking.monthly_vehicle_register_failed` | failure | `registerParkingMonthlyVehicle` | `POST` | `/api/v1/parking/contracts/{contract_id}/vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.monthly_vehicle_registered` | success | `registerParkingMonthlyVehicle` | `POST` | `/api/v1/parking/contracts/{contract_id}/vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.monthly_vehicle_revoke_failed` | failure | `revokeParkingMonthlyVehicle` | `POST` | `/api/v1/parking/monthly-vehicles/{vehicle_id}/revoke` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.monthly_vehicle_revoked` | success | `revokeParkingMonthlyVehicle` | `POST` | `/api/v1/parking/monthly-vehicles/{vehicle_id}/revoke` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.payment_confirm_failed` | failure | `confirmCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments/{payment_id}/confirm` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `parking.payment_create_failed` | failure | `createCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `parking.payment_created` | success | `createCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `parking.payment_paid` | success | `confirmCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments/{payment_id}/confirm` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `parking.policy_save_failed` | failure | `saveParkingPolicy` | `PUT` | `/api/v1/parking/policies/{project_id}` | P0-待归类 | `parking.config.manage` | 接口级事件，以本附录为准 |
| `parking.policy_saved` | success | `saveParkingPolicy` | `PUT` | `/api/v1/parking/policies/{project_id}` | P0-待归类 | `parking.config.manage` | 接口级事件，以本附录为准 |
| `parking.staff_vehicle_register_failed` | failure | `registerParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.staff_vehicle_registered` | success | `registerParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.staff_vehicle_revoke_failed` | failure | `revokeParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles/{vehicle_id}/revoke` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `parking.staff_vehicle_revoked` | success | `revokeParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles/{vehicle_id}/revoke` | P0-待归类 | `parking.vehicle.manage` | 接口级事件，以本附录为准 |
| `partner_org.update_failed` | failure | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner_org.updated` | success | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.manage` | 接口级事件，以本附录为准 |
| `partner_org.viewed` | success | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | P0-待归类 | `partner.org.view` | 接口级事件，以本附录为准 |
| `partner_subordinate.create_failed` | failure | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.created` | success | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.disable_failed` | failure | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
| `partner_subordinate.disabled` | success | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | P0-待归类 | `partner.subordinate.manage` | 接口级事件，以本附录为准 |
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
| `permission_template.viewed` | success | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | AUTH-账号权限 | `access.permission_template.view` | 接口级事件，以本附录为准 |
| `permission.viewed` | success | `listPermissions` | `GET` | `/api/v1/access/permissions` | AUTH-账号权限 | `access.permission.view` | 接口级事件，以本附录为准 |
| `platform.sensitive_operation_range.save_failed` | failure | `saveProjectSensitiveOperationRange` | `PUT` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | GOV-平台治理 | `platform.sensitive_operation_range.manage` | 接口级事件，以本附录为准 |
| `platform.sensitive_operation_range.saved` | success | `saveProjectSensitiveOperationRange` | `PUT` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | GOV-平台治理 | `platform.sensitive_operation_range.manage` | 接口级事件，以本附录为准 |
| `platform.sensitive_operation_range.view_failed` | failure | `getProjectSensitiveOperationRange` | `GET` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | GOV-平台治理 | `platform.sensitive_operation_range.manage` | 接口级事件，以本附录为准 |
| `platform.sensitive_operation_range.viewed` | success | `getProjectSensitiveOperationRange` | `GET` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | GOV-平台治理 | `platform.sensitive_operation_range.manage` | 接口级事件，以本附录为准 |
| `points.recovery.view_failed` | failure | `getPointsRecoveryDetail` | `GET` | `/api/v1/finance/points-recoveries/{customerId}` | FIN-财务台账 | `finance.points_recovery.view` | 接口级事件，以本附录为准 |
| `points.recovery.viewed` | success | `getPointsRecoveryDetail` | `GET` | `/api/v1/finance/points-recoveries/{customerId}` | FIN-财务台账 | `finance.points_recovery.view` | 接口级事件，以本附录为准 |
| `points.recovery.waive_completed` | success | `waivePointsRecovery` | `POST` | `/api/v1/finance/points-recoveries/{customerId}/waive` | FIN-财务台账 | `finance.points_recovery.waive` | 接口级事件，以本附录为准 |
| `points.recovery.waive_failed` | failure | `waivePointsRecovery` | `POST` | `/api/v1/finance/points-recoveries/{customerId}/waive` | FIN-财务台账 | `finance.points_recovery.waive` | 接口级事件，以本附录为准 |
| `production_readiness.check_failed` | failure | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | OPS-生产前置 | `ops.production_readiness.check` | 已在事件矩阵正文出现 |
| `production_readiness.checked` | success | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | OPS-生产前置 | `ops.production_readiness.check` | 已在事件矩阵正文出现 |
| `production_readiness.update_failed` | failure | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | OPS-生产前置 | `ops.production_readiness.manage` | 已在事件矩阵正文出现 |
| `production_readiness.updated` | success | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | OPS-生产前置 | `ops.production_readiness.manage` | 已在事件矩阵正文出现 |
| `production_readiness.view_failed` | failure | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | OPS-生产前置 | `ops.production_readiness.view` | 已在事件矩阵正文出现 |
| `production_readiness.viewed` | success | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | OPS-生产前置 | `ops.production_readiness.view` | 已在事件矩阵正文出现 |
| `project.create_failed` | failure | `createProject` | `POST` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.manage` | 接口级事件，以本附录为准 |
| `project.created` | success | `createProject` | `POST` | `/api/v1/platform/projects` | GOV-平台治理 | `governance.project.manage` | 接口级事件，以本附录为准 |
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
| `refund_authority.change_failed` | failure | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.change_failed` | failure | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.changed` | success | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `refund_authority.changed` | success | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | FIN-财务台账 | `refund.authority.manage` | 接口级事件，以本附录为准 |
| `risk_alert.create_failed` | failure | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.created` | success | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.view_failed` | failure | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `risk_alert.viewed` | success | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `role.change_failed` | failure | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.changed` | success | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.change_failed` | failure | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.changed` | success | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewable_projects.viewed` | success | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.viewed` | success | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
| `security.export_password.trace_failed` | failure | `lookupExportPassword` | `POST` | `/api/v1/security/export-files/lookup` | EVD-事件证据 | `security.watermark.trace` | 接口级事件，以本附录为准 |
| `security.export_password.traced` | success | `lookupExportPassword` | `POST` | `/api/v1/security/export-files/lookup` | EVD-事件证据 | `security.watermark.trace` | 接口级事件，以本附录为准 |
| `security.export.editable_download_failed` | failure | `downloadEditableExportCopy` | `GET` | `/api/v1/security/export-files/{id}/editable-copy` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.export.editable_downloaded` | success | `downloadEditableExportCopy` | `GET` | `/api/v1/security/export-files/{id}/editable-copy` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.export.editable_request_failed` | failure | `requestEditableExportCopy` | `POST` | `/api/v1/security/export-files/{id}/editable-requests` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.export.editable_requested` | success | `requestEditableExportCopy` | `POST` | `/api/v1/security/export-files/{id}/editable-requests` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.page_view.failed` | failure | `recordPageView` | `POST` | `/api/v1/security/page-views` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.sensitive_page.viewed` | success | `recordPageView` | `POST` | `/api/v1/security/page-views` | EVD-事件证据 | `authenticated` | 接口级事件，以本附录为准 |
| `security.watermark.trace_failed` | failure | `traceWatermark` | `POST` | `/api/v1/security/watermarks/trace` | EVD-事件证据 | `security.watermark.trace` | 接口级事件，以本附录为准 |
| `security.watermark.traced` | success | `traceWatermark` | `POST` | `/api/v1/security/watermarks/trace` | EVD-事件证据 | `security.watermark.trace` | 接口级事件，以本附录为准 |
| `service.catalog.archive_failed` | failure | `archiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/archive` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.archive_failed` | failure | `unarchiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/unarchive` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.archived` | success | `archiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/archive` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.delete_approve_failed` | failure | `approveDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-approve` | P0-待归类 | `service.catalog.approve` | 接口级事件，以本附录为准 |
| `service.catalog.delete_approved` | success | `approveDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-approve` | P0-待归类 | `service.catalog.approve` | 接口级事件，以本附录为准 |
| `service.catalog.delete_request_failed` | failure | `requestDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-request` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.delete_requested` | success | `requestDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-request` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.fee_activate_failed` | failure | `activateServiceCatalogFeeDraft` | `POST` | `/api/v1/services/catalog/{id}/fee-activate` | P0-待归类 | `service.catalog.approve` | 接口级事件，以本附录为准 |
| `service.catalog.fee_activated` | success | `activateServiceCatalogFeeDraft` | `POST` | `/api/v1/services/catalog/{id}/fee-activate` | P0-待归类 | `service.catalog.approve` | 接口级事件，以本附录为准 |
| `service.catalog.fee_draft_failed` | failure | `draftServiceCatalogFeeChange` | `POST` | `/api/v1/services/catalog/{id}/fee-draft` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.fee_drafted` | success | `draftServiceCatalogFeeChange` | `POST` | `/api/v1/services/catalog/{id}/fee-draft` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.save_failed` | failure | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.saved` | success | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.catalog.unarchived` | success | `unarchiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/unarchive` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.order.accept_failed` | failure | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.accepted` | success | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.order.cancel_failed` | failure | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.cancelled` | success | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.complete_failed` | failure | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.completed` | success | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.create_failed` | failure | `createServiceOrder` | `POST` | `/api/v1/services/orders` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.created` | success | `createServiceOrder` | `POST` | `/api/v1/services/orders` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.paid` | success | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.order.pay_confirm_failed` | failure | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.pay_config.save_failed` | failure | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.pay_config.saved` | success | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | P0-待归类 | `service.catalog.manage` | 接口级事件，以本附录为准 |
| `service.payment_code.present_failed` | failure | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.payment_code.presented` | success | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | P0-待归类 | `service.order.create` | 接口级事件，以本附录为准 |
| `service.payment_code.save_failed` | failure | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `service.payment_code.saved` | success | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | P0-待归类 | `service.order.perform` | 接口级事件，以本附录为准 |
| `settlement.bill.post_failed` | failure | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | P0-待归类 | `settlement.bill.post` | 接口级事件，以本附录为准 |
| `settlement.bill.posted` | success | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | P0-待归类 | `settlement.bill.post` | 接口级事件，以本附录为准 |
| `settlement.config.archive_failed` | failure | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.archived` | success | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.bar_commission_rate_configs.update_failed` | failure | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.bar_commission_rate_configs.updated` | success | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.channel_fee_configs.update_failed` | failure | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.channel_fee_configs.updated` | success | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.comprehensive_service_fee_configs.update_failed` | failure | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.comprehensive_service_fee_configs.updated` | success | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.distribution_notice_period.update_failed` | failure | `settlementConfigSaveDistributionNoticePeriod` | `POST` | `/api/v1/settlement/config/distribution-scheme/notice-period` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.distribution_notice_period.updated` | success | `settlementConfigSaveDistributionNoticePeriod` | `POST` | `/api/v1/settlement/config/distribution-scheme/notice-period` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.distribution_scheme.update_failed` | failure | `settlementConfigSaveDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.distribution_scheme.updated` | success | `settlementConfigSaveDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activate_failed` | failure | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.draft.activated` | success | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.high_value_thresholds.update_failed` | failure | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.high_value_thresholds.updated` | success | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.update_failed` | failure | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.internal_price_catalog.updated` | success | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.pass_gift_cost_configs.update_failed` | failure | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.pass_gift_cost_configs.updated` | success | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.approve_failed` | failure | `settlementConfigApprovePassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/approve` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.approved` | success | `settlementConfigApprovePassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/approve` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.reject_failed` | failure | `settlementConfigRejectPassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/reject` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.rejected` | success | `settlementConfigRejectPassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/reject` | P0-待归类 | `settlement.bill.special_approve` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.submit_failed` | failure | `settlementConfigSubmitPassIncentive` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/incentive-applications` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.config.pass_incentive.submitted` | success | `settlementConfigSubmitPassIncentive` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/incentive-applications` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
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
| `settlement.internal_pay_day_rule.approved` | success | `decideInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules/{rule_id}/decision` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.internal_pay_day_rule.decision_failed` | failure | `decideInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules/{rule_id}/decision` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.internal_pay_day_rule.submit_failed` | failure | `submitInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.internal_pay_day_rule.submitted` | success | `submitInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.export_failed` | failure | `exportSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items/export` | P0-待归类 | `settlement.invoice.export` | 接口级事件，以本附录为准 |
| `settlement.invoice.exported` | success | `exportSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items/export` | P0-待归类 | `settlement.invoice.export` | 接口级事件，以本附录为准 |
| `settlement.invoice.generate_failed` | failure | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.generated` | success | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.status_change_failed` | failure | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.invoice.status_changed` | success | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | P0-待归类 | `settlement.invoice.manage` | 接口级事件，以本附录为准 |
| `settlement.objection_period.config_failed` | failure | `saveSettlementObjectionPeriod` | `POST` | `/api/v1/settlement/internal-pay-days/objection-period` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `settlement.objection_period.configured` | success | `saveSettlementObjectionPeriod` | `POST` | `/api/v1/settlement/internal-pay-days/objection-period` | P0-待归类 | `settlement.config.manage` | 接口级事件，以本附录为准 |
| `space_type.archived` | success | `archiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/archive` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.delete_approved` | success | `approveDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-approve` | GOV-平台治理 | `authenticated` | 接口级事件，以本附录为准 |
| `space_type.delete_requested` | success | `requestDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-request` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.unarchived` | success | `unarchiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/unarchive` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.update_failed` | failure | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.updated` | success | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space_type.viewed` | success | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | GOV-平台治理 | `governance.space.view` | 接口级事件，以本附录为准 |
| `space.update_failed` | failure | `createSpace` | `POST` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `space.updated` | success | `createSpace` | `POST` | `/api/v1/platform/spaces` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
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
| `staff.business_handover.assign_failed` | failure | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.assigned` | success | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.view_failed` | failure | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.business_handover.viewed` | success | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.close_failed` | failure | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.closed` | success | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.commission_settlement.viewed` | success | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.dispatch.create_failed` | failure | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.created` | success | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.end_failed` | failure | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.ended` | success | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.dispatch.viewed` | success | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | STAFF-员工 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.leave.review_failed` | failure | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.reviewed` | success | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.revoke_request_failed` | failure | `requestStaffLeaveRevoke` | `POST` | `/api/v1/staff/leaves/{id}/revoke-request` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.revoke_requested` | success | `requestStaffLeaveRevoke` | `POST` | `/api/v1/staff/leaves/{id}/revoke-request` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.revoke_review_failed` | failure | `revokeStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/revoke` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.revoke_reviewed` | success | `revokeStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/revoke` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.leave.submit_failed` | failure | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.leave.submitted` | success | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | STAFF-员工 | `staff.leave.submit` | 接口级事件，以本附录为准 |
| `staff.locker.record_failed` | failure | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | STAFF-员工 | `not_applicable` | 接口级事件，以本附录为准 |
| `staff.locker.recorded` | success | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | STAFF-员工 | `not_applicable` | 接口级事件，以本附录为准 |
| `staff.locker.scan_token_failed` | failure | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `staff.locker.scan_token_issued` | success | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | STAFF-员工 | `staff.profile.self` | 接口级事件，以本附录为准 |
| `staff.notice_acknowledge_failed` | failure | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice_acknowledged` | success | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice.viewed` | success | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_add_failed` | failure | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeal_submit_failed` | failure | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeal_submitted` | success | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_appeals_viewed` | success | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.adjustment_requested` | success | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.base_save_failed` | failure | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calculate` | success | `staffPayrollCalculate` | `POST` | `/api/v1/staff/payroll/calculations` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calculate_failed` | failure | `staffPayrollCalculate` | `POST` | `/api/v1/staff/payroll/calculations` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calculation_list` | success | `staffPayrollCalculationList` | `GET` | `/api/v1/staff/payroll/calculations` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calculation_list_failed` | failure | `staffPayrollCalculationList` | `GET` | `/api/v1/staff/payroll/calculations` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_draft` | success | `staffPayrollCalendarDraft` | `POST` | `/api/v1/staff/payroll/calendars` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_draft_failed` | failure | `staffPayrollCalendarDraft` | `POST` | `/api/v1/staff/payroll/calendars` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_list` | success | `staffPayrollCalendarList` | `GET` | `/api/v1/staff/payroll/calendars` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_list_failed` | failure | `staffPayrollCalendarList` | `GET` | `/api/v1/staff/payroll/calendars` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_submit` | success | `staffPayrollCalendarSubmit` | `POST` | `/api/v1/staff/payroll/calendars/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.calendar_submit_failed` | failure | `staffPayrollCalendarSubmit` | `POST` | `/api/v1/staff/payroll/calendars/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.commission_add_failed` | failure | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.commission_added` | success | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.detail` | success | `staffPayrollDetail` | `GET` | `/api/v1/staff/payroll/statements/{id}` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.detail_failed` | failure | `staffPayrollDetail` | `GET` | `/api/v1/staff/payroll/statements/{id}` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.detail_mine` | success | `staffPayrollDetailMine` | `GET` | `/api/v1/staff/payroll/mine/statements/{id}` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.detail_mine_failed` | failure | `staffPayrollDetailMine` | `GET` | `/api/v1/staff/payroll/mine/statements/{id}` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.month_close_failed` | failure | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.pay_failed` | failure | `markStaffPayrollPaidLegacy` | `POST` | `/api/v1/staff/payroll/pay` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_request_failed` | failure | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_requested` | success | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.penalty_cap_viewed` | success | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_draft` | success | `staffPayrollPeriodDraft` | `POST` | `/api/v1/staff/payroll/period-facts` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_draft_failed` | failure | `staffPayrollPeriodDraft` | `POST` | `/api/v1/staff/payroll/period-facts` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_list` | success | `staffPayrollPeriodList` | `GET` | `/api/v1/staff/payroll/period-facts` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_list_failed` | failure | `staffPayrollPeriodList` | `GET` | `/api/v1/staff/payroll/period-facts` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_submit` | success | `staffPayrollPeriodSubmit` | `POST` | `/api/v1/staff/payroll/period-facts/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.period_submit_failed` | failure | `staffPayrollPeriodSubmit` | `POST` | `/api/v1/staff/payroll/period-facts/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_draft` | success | `staffPayrollRuleDraft` | `POST` | `/api/v1/staff/payroll/performance-rules` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_draft_failed` | failure | `staffPayrollRuleDraft` | `POST` | `/api/v1/staff/payroll/performance-rules` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_list` | success | `staffPayrollRuleList` | `GET` | `/api/v1/staff/payroll/performance-rules` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_list_failed` | failure | `staffPayrollRuleList` | `GET` | `/api/v1/staff/payroll/performance-rules` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_submit` | success | `staffPayrollRuleSubmit` | `POST` | `/api/v1/staff/payroll/performance-rules/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.rule_submit_failed` | failure | `staffPayrollRuleSubmit` | `POST` | `/api/v1/staff/payroll/performance-rules/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_draft` | success | `staffPayrollSchemeDraft` | `POST` | `/api/v1/staff/payroll/schemes` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_draft_failed` | failure | `staffPayrollSchemeDraft` | `POST` | `/api/v1/staff/payroll/schemes` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_list` | success | `staffPayrollSchemeList` | `GET` | `/api/v1/staff/payroll/schemes` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_list_failed` | failure | `staffPayrollSchemeList` | `GET` | `/api/v1/staff/payroll/schemes` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_submit` | success | `staffPayrollSchemeSubmit` | `POST` | `/api/v1/staff/payroll/schemes/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.scheme_submit_failed` | failure | `staffPayrollSchemeSubmit` | `POST` | `/api/v1/staff/payroll/schemes/{id}/submit` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.score_appeal` | success | `staffPayrollScoreAppeal` | `POST` | `/api/v1/staff/payroll/performance-adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.score_appeal_failed` | failure | `staffPayrollScoreAppeal` | `POST` | `/api/v1/staff/payroll/performance-adjustments/{id}/appeals` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.score_deduct` | success | `staffPayrollScoreDeduct` | `POST` | `/api/v1/staff/payroll/performance-adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.score_deduct_failed` | failure | `staffPayrollScoreDeduct` | `POST` | `/api/v1/staff/payroll/performance-adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.score_list` | success | `staffPayrollScoreList` | `GET` | `/api/v1/staff/payroll/performance-adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.score_list_failed` | failure | `staffPayrollScoreList` | `GET` | `/api/v1/staff/payroll/performance-adjustments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.score_mine` | success | `staffPayrollScoreMine` | `GET` | `/api/v1/staff/payroll/mine/performance-adjustments` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.score_mine_failed` | failure | `staffPayrollScoreMine` | `GET` | `/api/v1/staff/payroll/mine/performance-adjustments` | STAFF-员工 | `staff.payroll.view_mine` | 接口级事件，以本附录为准 |
| `staff.payroll.settle` | success | `staffPayrollSettle` | `POST` | `/api/v1/staff/payroll/settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.settle_failed` | failure | `staffPayrollSettle` | `POST` | `/api/v1/staff/payroll/settlements` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_view_failed` | failure | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax_bracket_viewed` | success | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessment_draft_failed` | failure | `draftPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessment_drafted` | success | `draftPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessment_submit_failed` | failure | `submitPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments/{id}/submit` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessment_submitted` | success | `submitPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments/{id}/submit` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessments_view_failed` | failure | `listPayrollTaxAssessments` | `GET` | `/api/v1/staff/payroll/tax-assessments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.assessments_viewed` | success | `listPayrollTaxAssessments` | `GET` | `/api/v1/staff/payroll/tax-assessments` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.single_bracket_write_rejected` | failure | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.version_draft_failed` | failure | `draftPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.version_drafted` | success | `draftPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.version_submit_failed` | failure | `submitPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions/{id}/submit` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.version_submitted` | success | `submitPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions/{id}/submit` | STAFF-员工 | `staff.tax_bracket.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.versions_view_failed` | failure | `listPayrollTaxVersions` | `GET` | `/api/v1/staff/payroll/tax-versions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.payroll.tax.versions_viewed` | success | `listPayrollTaxVersions` | `GET` | `/api/v1/staff/payroll/tax-versions` | STAFF-员工 | `staff.payroll.manage` | 接口级事件，以本附录为准 |
| `staff.profile.archived` | success | `archiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/archive` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.profile.delete_approved` | success | `approveDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-approve` | AUTH-账号权限 | `staff.profile.approve` | 接口级事件，以本附录为准 |
| `staff.profile.delete_requested` | success | `requestDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-request` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.employment_status_change_failed` | failure | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.employment_status_change_requested` | success | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | STAFF-员工 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.sales_position_change_failed` | failure | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.sales_position_changed` | success | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.profile.unarchived` | success | `unarchiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/unarchive` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.profile.viewed` | success | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.register_failed` | failure | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.registered` | success | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.registration_companies_viewed` | success | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.review_failed` | failure | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `staff.self_register_failed` | failure | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.self_registered` | success | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | STAFF-员工 | `public` | 接口级事件，以本附录为准 |
| `staff.shift.cover_failed` | failure | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.covered` | success | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.covers_viewed` | success | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.history_viewed` | success | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.save_failed` | failure | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.save_failed` | failure | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.submitted` | success | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.submitted` | success | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_change_failed` | failure | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_changed` | success | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `staff.shift.visibility_viewed` | success | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | STAFF-员工 | `staff.shift.manage` | 接口级事件，以本附录为准 |
| `stored_value.consume` | success | `payCustomerTicketOrderWithStoredValue` | `POST` | `/api/v1/customer/orders/{order_id}/stored-value/pay` | CUS/TIC-客人购票 | `customer.stored_value.consume` | 接口级事件，以本附录为准 |
| `stored_value.counter_refund` | success | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.counter_refund_failed` | failure | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.exit_notice_policy_config_failed` | failure | `saveMerchantCardExitNoticePolicy` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.exit_notice_policy_configured` | success | `saveMerchantCardExitNoticePolicy` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.exit_notice_publish_failed` | failure | `publishMerchantCardExitNotice` | `POST` | `/api/v1/merchant-portal/card-exit-notices` | P0-待归类 | `merchant.portal.card_exit_notice.publish` | 接口级事件，以本附录为准 |
| `stored_value.exit_notice_published` | success | `publishMerchantCardExitNotice` | `POST` | `/api/v1/merchant-portal/card-exit-notices` | P0-待归类 | `merchant.portal.card_exit_notice.publish` | 接口级事件，以本附录为准 |
| `stored_value.frozen` | success | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.frozen_failed` | failure | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.merchant_card_config_failed` | failure | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_card_configured` | success | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_configured` | success | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.merchant_refund_rule_failed` | failure | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.refund` | success | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.refund_failed` | failure | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | CUS-客人端 | `customer.stored_value.refund` | 接口级事件，以本附录为准 |
| `stored_value.unfrozen` | success | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.unfrozen_failed` | failure | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
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
| `terminal_switch.view_failed` | failure | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal_switch.viewed` | success | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.access_rejected` | failure | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.device.archive_failed` | failure | `markTerminalDeviceArchived` | `POST` | `/api/v1/terminals/devices/{id}/archive` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.archived` | success | `markTerminalDeviceArchived` | `POST` | `/api/v1/terminals/devices/{id}/archive` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.bind_failed` | failure | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.bound` | success | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.entry_device_register_failed` | failure | `registerEntryDevice` | `POST` | `/api/v1/terminals/devices/entry-devices` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.entry_device_registered` | success | `registerEntryDevice` | `POST` | `/api/v1/terminals/devices/entry-devices` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.found` | success | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.found_failed` | failure | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.lost` | success | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.lost_failed` | failure | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.merchant_change_failed` | failure | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.merchant_changed` | success | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repair_complete_failed` | failure | `completeTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair-complete` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repair_completed` | success | `completeTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair-complete` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repair_failed` | failure | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.repaired` | success | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.responsible_staff_change_failed` | failure | `changeTerminalDeviceResponsibleStaff` | `POST` | `/api/v1/terminals/devices/{id}/responsible-staff` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.responsible_staff_changed` | success | `changeTerminalDeviceResponsibleStaff` | `POST` | `/api/v1/terminals/devices/{id}/responsible-staff` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.returned` | success | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.returned_failed` | failure | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.scrap_failed` | failure | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.scrapped` | success | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | TERM-终端设备 | `terminal.device.lifecycle` | 接口级事件，以本附录为准 |
| `terminal.device.signing_key_register_failed` | failure | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.signing_key_registered` | success | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.unbind_failed` | failure | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.device.unbound` | success | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.device.view_failed` | failure | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.device.viewed` | success | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.kiosk_config_viewed` | success | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat` | success | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat_failed` | failure | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
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
| `ticket_price_variant.archive_failed` | failure | `archiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/archive` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.archived` | success | `archiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/archive` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.change_failed` | failure | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.changed` | success | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.delete_approve_failed` | failure | `approveDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-approve` | TIC-票务权益 | `ticket.price_rule.approve` | 接口级事件，以本附录为准 |
| `ticket_price_variant.delete_approved` | success | `approveDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-approve` | TIC-票务权益 | `ticket.price_rule.approve` | 接口级事件，以本附录为准 |
| `ticket_price_variant.delete_request_failed` | failure | `requestDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-request` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.delete_requested` | success | `requestDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-request` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.unarchive_failed` | failure | `unarchiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/unarchive` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.unarchived` | success | `unarchiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/unarchive` | TIC-票务权益 | `ticket.price_variant.manage` | 接口级事件，以本附录为准 |
| `ticket_price_variant.view_failed` | failure | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.view` | 接口级事件，以本附录为准 |
| `ticket_price_variant.viewed` | success | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | TIC-票务权益 | `ticket.price_variant.view` | 接口级事件，以本附录为准 |
| `ticket_price.publish_failed` | failure | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_price.published` | success | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | TIC-票务权益 | `ticket.product.manage` | 已在事件矩阵正文出现 |
| `ticket_product_entitlement.bound` | success | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product_entitlement.bound_failed` | failure | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.archive_failed` | failure | `archiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/archive` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.archived` | success | `archiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/archive` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.change_failed` | failure | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.changed` | success | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.delete_approve_failed` | failure | `approveDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-approve` | TIC-票务权益 | `ticket.type.approve` | 接口级事件，以本附录为准 |
| `ticket_product.delete_approved` | success | `approveDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-approve` | TIC-票务权益 | `ticket.type.approve` | 接口级事件，以本附录为准 |
| `ticket_product.delete_request_failed` | failure | `requestDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-request` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.delete_requested` | success | `requestDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-request` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.pricing_change_failed` | failure | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.pricing_changed` | success | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.unarchive_failed` | failure | `unarchiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/unarchive` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.unarchived` | success | `unarchiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/unarchive` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket_product.view_failed` | failure | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.view_failed` | failure | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
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
| `ticket_safety_event.approve_failed` | failure | `approveTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events/{safety_event_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_safety_event.approved` | success | `approveTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events/{safety_event_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_safety_event.request_failed` | failure | `requestTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket_safety_event.requested` | success | `requestTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket_safety_event.view_failed` | failure | `listTicketSafetyEvents` | `GET` | `/api/v1/ticketing/safety-events` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket_safety_event.viewed` | success | `listTicketSafetyEvents` | `GET` | `/api/v1/ticketing/safety-events` | TIC-票务权益 | `ticket.refund.view` | 接口级事件，以本附录为准 |
| `ticket_transfer.claim_failed` | failure | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.claimed` | success | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.create_failed` | failure | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.created` | success | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.revoke_failed` | failure | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.revoked` | success | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | CUS/TIC-客人购票 | `not_applicable` | 接口级事件，以本附录为准 |
| `ticket_transfer.view_failed` | failure | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | TIC-票务权益 | `ticket.transfer.read` | 接口级事件，以本附录为准 |
| `ticket_transfer.viewed` | success | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | TIC-票务权益 | `ticket.transfer.read` | 接口级事件，以本附录为准 |
| `ticket.expiry_reminder_policy_update_failed` | failure | `saveTicketExpiryReminderPolicy` | `PUT` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
| `ticket.expiry_reminder_policy_updated` | success | `saveTicketExpiryReminderPolicy` | `PUT` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | TIC-票务权益 | `ticket.product.manage` | 接口级事件，以本附录为准 |
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
| `ticket.verification_anomaly_raise_failed` | failure | `raiseTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verifications/{id}/anomaly` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `ticket.verification_anomaly_raised` | success | `raiseTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verifications/{id}/anomaly` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `ticket.verification_anomaly_resolve_failed` | failure | `resolveTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verification-anomalies/{id}/resolve` | TIC-票务权益 | `ticket.verify.anomaly.resolve` | 接口级事件，以本附录为准 |
| `ticket.verification_anomaly_resolved` | success | `resolveTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verification-anomalies/{id}/resolve` | TIC-票务权益 | `ticket.verify.anomaly.resolve` | 接口级事件，以本附录为准 |
| `ticket.verification_reverse_failed` | failure | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `ticket.verification_reversed` | success | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `venue_business_hours.update_failed` | failure | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
| `venue_business_hours.updated` | success | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | GOV-平台治理 | `governance.space.manage` | 接口级事件，以本附录为准 |
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
| `voucher.redeem_code_issue_failed` | failure | `getCustomerVoucherRedeemCode` | `GET` | `/api/v1/customer/vouchers/{voucher_instance_id}/redeem-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher.redeem_code_issued` | success | `getCustomerVoucherRedeemCode` | `GET` | `/api/v1/customer/vouchers/{voucher_instance_id}/redeem-code` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `voucher.redeem_failed` | failure | `redeemVoucher` | `POST` | `/api/v1/vouchers/redemptions` | CUS-客人端 | `customer.voucher.redeem` | 接口级事件，以本附录为准 |
| `voucher.redeem_preview_failed` | failure | `previewVoucherRedemption` | `POST` | `/api/v1/vouchers/redemptions/preview` | CUS-客人端 | `customer.voucher.redeem` | 接口级事件，以本附录为准 |
| `voucher.redeem_previewed` | success | `previewVoucherRedemption` | `POST` | `/api/v1/vouchers/redemptions/preview` | CUS-客人端 | `customer.voucher.redeem` | 接口级事件，以本附录为准 |
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
| `wecom.risk_hit.review_failed` | failure | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | P0-待归类 | `wecom.risk_hit.review` | 接口级事件，以本附录为准 |
| `wecom.risk.hit_reviewed` | success | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | P0-待归类 | `wecom.risk_hit.review` | 接口级事件，以本附录为准 |

## 5. operationId 到任务、权限、事件、终端的追溯表

> 本节用于开发排期、测试用例、供应商联调和审计抽样。任务簇是执行归类，不替代 `p0-implementation-task-list.md` 中的正式任务编号；若某 operation 准备进入编码，必须在具体任务单中引用本表 operationId。

| 建议任务簇 | operationId | 方法 | 路径 | 摘要 | 权限 | 成功/失败事件 | 终端/调用通道 | 覆盖状态 |
|---|---|---|---|---|---|---|---|---|
| AMU-游乐安全 | `acknowledgeAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices/{noticeId}/acknowledgements` | 登记安全告知签字确认（免责确认） | `amusement.waiver.acknowledge` | `amusement.waiver.acknowledge` / `amusement.waiver.acknowledge_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `addAmusementIncidentAction` | `POST` | `/api/v1/amusement/incidents/{incidentId}/actions` | 添加事件处置动作/流转状态 | `amusement.incident.process` | `amusement.incident.action` / `amusement.incident.action_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | Approve P2 amusement group booking and lock capacity | `amusement.group_booking.approve` | `amusement_group_booking.approved.success` / `amusement_group_booking.approved.failure` | web | operation 级补充 |
| AMU-游乐安全 | `approveAmusementReopen` | `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | 严重项恢复开放审批 | `amusement.safety_inspection.approve_reopen` | `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected` | web | operation 级补充 |
| AMU-游乐安全 | `approveDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-approve` | 审批游乐项目权益绑定删除申请 | `amusement.project_entitlement.approve` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `archiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/archive` | 存档（停用）游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `assignAmusementProjectSpace` | `PUT` | `/api/v1/amusement/projects/{id}/space` | 游乐项目空间指派（回填） | `amusement.session.manage` | `amusement.project_space.assigned` / `amusement.project_space.assign_failed` | web | operation 级补充 |
| AMU-游乐安全 | `changeAmusementQueueEntryStatus` | `POST` | `/api/v1/amusement/queue-entries/{id}/status` | 变更游乐排队记录状态 | `amusement.queue.manage` | `amusement.queue_entry.status_change` / `amusement.queue_entry.status_change_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `changeAmusementReservationStatus` | `POST` | `/api/v1/amusement/reservations/{id}/status` | 变更游乐预约状态 | `amusement.reservation.manage` | `amusement.reservation.status_change` / `amusement.reservation.status_change_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `claimAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions/{id}/claim` | 一体机领取核销权益会话（防错领 + 会话时效） | `amusement.entitlement_session.claim` | `amusement.entitlement_session.claimed` / `amusement.entitlement_session.claim_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `closeAmusementIncident` | `POST` | `/api/v1/amusement/incidents/{incidentId}/close` | 关闭游乐安全事件 | `amusement.incident.close` | `amusement.incident.close` / `amusement.incident.close_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `configureAmusementCompensationPolicy` | `POST` | `/api/v1/amusement/compensation-policies/{project_id}` | 配置全园故障补偿有效期参数（含上下限） | `amusement.compensation_policy.manage` | `amusement.fault_compensation.policy_configured` / `amusement.fault_compensation.policy_config_failed` | web | operation 级补充 |
| AMU-游乐安全 | `configureAmusementOverCapacityPolicy` | `PUT` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | 配置游乐项目超容处置策略 | `amusement.over_capacity_policy.configure` | `amusement.over_capacity_policy.configured` / `amusement.over_capacity_policy.configure_failed` | web | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/compensation-batches/{batch_id}/confirm` | 第二岗位确认并发放补偿 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.fault_compensation.batch_issued` / `amusement.fault_compensation.batch_issue_failed` | web | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | 主管确认恢复营业 | `amusement.maintenance.confirm` | `amusement.project.resumed` / `amusement.maintenance_resume_failed` | kiosk, web, mobile_admin, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementParkWideFaultActivation` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-activation` | 第二岗位确认全园故障状态生效 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.park_fault.activated` / `amusement.park_fault.activate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/confirm-lift` | 第二岗位确认解除全园故障状态 | `amusement.park_fault.confirm_as_engineering_safety` | `amusement.park_fault.lifted` / `amusement.park_fault.lift_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementCapacityConfig` | `POST` | `/api/v1/amusement/projects/{id}/capacity-config` | 维护游乐点位设备并发容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.changed` / `amusement.capacity_config.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementDevice` | `POST` | `/api/v1/amusement/projects/{id}/devices` | 维护游乐项目游乐设备 | `amusement.device.manage` | `amusement.device.changed` / `amusement.device.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementExitVerification` | `POST` | `/api/v1/amusement/verifications/exit` | 游乐出场核销 | `amusement.verification.execute` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | Create P2 amusement group or private booking request | `amusement.group_booking.manage` | `amusement_group_booking.created.success` / `amusement_group_booking.created.failure` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementIncident` | `POST` | `/api/v1/amusement/incidents` | 上报游乐安全事件 | `amusement.incident.create` | `amusement.incident.create` / `amusement.incident.create_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | 提交游乐定期检修 | `amusement.maintenance.submit` | `amusement.maintenance_recorded` / `amusement.maintenance_record_failed` | kiosk, web, mobile_admin, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | 维护游乐项目 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements` | 维护游乐项目可用权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | 创建游乐项目规则 | `amusement.project.manage` | `amusement.project_rule.changed` / `amusement.project_rule.changed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementQueueEntry` | `POST` | `/api/v1/amusement/queue-entries` | 创建游乐排队记录 | `amusement.queue.manage` | `amusement.queue_entry.create` / `amusement.queue_entry.create_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementReservation` | `POST` | `/api/v1/amusement/reservations` | 创建游乐预约 | `amusement.reservation.manage` | `amusement.reservation.create` / `amusement.reservation.create_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections` | 创建开放前安全巡检 | `amusement.safety_inspection.create` | `amusement.safety_inspection.created` / `amusement.safety_inspection.create_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyNotice` | `POST` | `/api/v1/amusement/safety-notices` | 创建游乐安全告知/免责须知 | `amusement.safety_notice.manage` | `amusement.safety_notice.create` / `amusement.safety_notice.create_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | 维护巡检模板 | `amusement.safety_template.manage` | `amusement.safety_template.changed` / `amusement.safety_template.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | 维护游乐场次安排 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | 游乐权益核销 | `amusement.verification.execute` | `amusement.verification_succeeded` / `amusement.verification_failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| AMU-游乐安全 | `disableAmusementCapacityConfig` | `POST` | `/api/v1/amusement/capacity-config/{configId}/disable` | 停用游乐容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.changed` / `amusement.capacity_config.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `endAmusementSession` | `POST` | `/api/v1/amusement/sessions/{id}/end` | 确认实际清场后提前结束场次 | `amusement.session.end` | `amusement.session.end_completed` / `amusement.session.end_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `getAmusementCompensationPolicy` | `GET` | `/api/v1/amusement/compensation-policies/{project_id}` | 查询全园故障补偿有效期参数 | `amusement.park_fault.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `getAmusementEntitlementSession` | `GET` | `/api/v1/amusement/entitlement-sessions/{id}` | 一体机查询展示权益会话（按 id 或 session_code） | `amusement.entitlement_session.view` | `amusement.entitlement_session.viewed` / `amusement.entitlement_session.view_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | 查询游乐定期检修记录详情 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | 查询项目或场次安全与容量准入 | `amusement.safety_inspection.view` | `amusement.open_eligibility.viewed` / `amusement.open_eligibility.blocked` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `getAmusementOverCapacityPolicy` | `GET` | `/api/v1/amusement/projects/{projectId}/over-capacity-policy` | 查询游乐项目超容处置策略 | `amusement.over_capacity_policy.view` | `amusement.over_capacity_policy.viewed` / `amusement.over_capacity_policy.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementEntitlementSession` | `POST` | `/api/v1/amusement/entitlement-sessions` | 发起权益会话（工作手机扫附赠权益码） | `amusement.entitlement_session.initiate` | `amusement.entitlement_session.initiated` / `amusement.entitlement_session.initiate_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementFaultCompensationBatch` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | 发起全园故障补偿批次（第一岗位：快照受影响人份） | `amusement.park_fault.confirm_as_head` | `amusement.fault_compensation.batch_initiated` / `amusement.fault_compensation.batch_initiate_failed` | web | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementParkWideFault` | `POST` | `/api/v1/amusement/park-wide-faults` | 发起全园故障状态（第一岗位） | `amusement.park_fault.confirm_as_head` | `amusement.park_fault.initiated` / `amusement.park_fault.initiate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `initiateAmusementParkWideFaultLift` | `POST` | `/api/v1/amusement/park-wide-faults/{fault_id}/initiate-lift` | 发起解除全园故障状态（第一岗位） | `amusement.park_fault.confirm_as_head` | `amusement.park_fault.lift_initiated` / `amusement.park_fault.lift_initiate_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementBusinessDayConfigs` | `GET` | `/api/v1/amusement/business-day-configs` | 查看授权综合体的游乐营业日配置目录与版本 | `amusement.business_day.view` | `amusement.business_day.viewed` / `amusement.business_day.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `listAmusementCapacityConfigs` | `GET` | `/api/v1/amusement/projects/{id}/capacity-config` | 查询游乐点位设备并发容量配置 | `amusement.capacity_config.manage` | `amusement.capacity_config.viewed` / `amusement.capacity_config.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementCapacitySnapshots` | `GET` | `/api/v1/amusement/capacity-snapshots` | 查看游乐项目当前在场人数与硬容量 | `amusement.verification.view` | `amusement.verification.viewed` / `amusement.verification.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementCompensationBatchesByEvent` | `GET` | `/api/v1/amusement/compensation-batches` | 按事件类型查询补偿批次（含场次提前结束） | `amusement.park_fault.view` | `amusement.fault_compensation.viewed` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementCountReleases` | `GET` | `/api/v1/amusement/count-releases` | 查看场次及日切人数释放记录 | `amusement.verification.view` | `amusement.session.viewed` / `amusement.session.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `listAmusementDevices` | `GET` | `/api/v1/amusement/projects/{id}/devices` | 查询游乐项目游乐设备 | `amusement.device.view` | `amusement.device.viewed` / `amusement.device.view_failed` | web, kiosk, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | 查询游乐设备状态 | `amusement.device_status.view` | `amusement.device_status.viewed` / `amusement.device_status.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementEntitlementSessions` | `GET` | `/api/v1/amusement/entitlement-sessions` | 查询权益会话列表（merchant scope） | `amusement.entitlement_session.view` | `amusement.entitlement_session.listed` / `amusement.entitlement_session.list_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementFaultCompensationBatches` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/compensation-batches` | 查询某次全园故障的补偿批次 | `amusement.park_fault.view` | `amusement.fault_compensation.viewed` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementFaultCompensationItems` | `GET` | `/api/v1/amusement/compensation-batches/{batch_id}/items` | 查询补偿批次的补偿记录 | `amusement.park_fault.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | List P2 amusement group and private bookings | `amusement.group_booking.view` | `amusement_group_booking.viewed.success` / `amusement_group_booking.viewed.failure` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementIncidents` | `GET` | `/api/v1/amusement/incidents` | 查询游乐安全事件列表 | `amusement.incident.view` | `amusement.incident.list` / `not_applicable` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementInspectionTasks` | `GET` | `/api/v1/amusement/inspection-tasks` | 查询游乐巡检到期待办 | `amusement.safety_inspection.view` | `amusement.inspection_task.viewed` / `amusement.inspection_task.viewed_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | 查询游乐定期检修记录 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementParkWideFaultActions` | `GET` | `/api/v1/amusement/park-wide-faults/{fault_id}/actions` | 查询全园故障状态留痕 | `amusement.park_fault.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementParkWideFaults` | `GET` | `/api/v1/amusement/park-wide-faults` | 查询全园故障状态记录 | `amusement.park_fault.view` | `amusement.park_fault.viewed` / `not_applicable` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectEntitlements` | `GET` | `/api/v1/amusement/projects/{id}/entitlements` | 查询游乐项目可用权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.viewed` / `amusement.project_entitlement.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | 查询游乐项目 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectStatusBoard` | `GET` | `/api/v1/amusement/projects/status-board` | 查看游乐设施状态看板 | `amusement.session.view` | `amusement.status_board.viewed` / `amusement.status_board.view_failed` | web, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectsWithoutDeviceRegistry` | `GET` | `/api/v1/amusement/projects/without-device-registry` | 待建档（无在役设备档案）的游乐项目清单 | `amusement.session.manage` | `amusement.device_registry_backlog.viewed` / `amusement.device_registry_backlog.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjectsWithoutSpace` | `GET` | `/api/v1/amusement/projects/without-space` | 未回填空间的游乐项目清单 | `amusement.session.manage` | `amusement.project_space_backlog.viewed` / `amusement.project_space_backlog.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | 查询游乐排队记录 | `amusement.queue.view` | `amusement.queue.viewed` / `amusement.queue.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementReservations` | `GET` | `/api/v1/amusement/reservations` | 查询游乐预约 | `amusement.reservation.view` | `amusement.reservation.viewed` / `amusement.reservation.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyInspections` | `GET` | `/api/v1/amusement/safety-inspections` | 查询安全巡检 | `amusement.safety_inspection.view` | `amusement.safety_inspection.viewed` / `amusement.safety_inspection.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyNotices` | `GET` | `/api/v1/amusement/safety-notices` | 查询游乐安全告知列表 | `amusement.safety_notice.view` | `amusement.safety_notice.list` / `not_applicable` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | 查询巡检模板 | `amusement.safety_template.view` | `amusement.safety_template.viewed` / `amusement.safety_template.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSessionEndings` | `GET` | `/api/v1/amusement/sessions/{id}/endings` | 查看场次结束原件 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `listAmusementSessionFollowups` | `GET` | `/api/v1/amusement/sessions/{id}/followups` | 查看场次结束后的未履约事项 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | 查询游乐场次安排 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | 查询游乐核销记录 | `amusement.verification.view` | `amusement.verification.viewed` / `amusement.verification.view_failed` | web | operation 级补充 |
| AMU-游乐安全 | `readAmusementBusinessDayConfigOriginal` | `GET` | `/api/v1/amusement/business-day-configs/{id}` | 逐笔查看游乐营业日配置审批原件 | `governance.supervisor_review.view` | `amusement.business_day.original_viewed` / `amusement.business_day.original_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `recheckAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | 提交复检结果 | `amusement.safety_inspection.recheck` | `amusement.safety_inspection.rechecked` / `amusement.safety_inspection.recheck_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `rectifyAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | 提交整改说明和证据 | `amusement.safety_inspection.rectify` | `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rectification_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | Release P2 amusement group booking and free locked capacity | `amusement.group_booking.manage` | `amusement_group_booking.released.success` / `amusement_group_booking.released.failure` | web | operation 级补充 |
| AMU-游乐安全 | `removeAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/remove` | 解除游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `requestDeleteAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/delete-request` | 发起游乐项目权益绑定删除申请 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `submitAmusementBusinessDayConfig` | `POST` | `/api/v1/amusement/business-day-configs` | 提交综合体游乐营业日最高权限审批 | `amusement.business_day.manage` | `amusement.business_day.submit_completed` / `amusement.business_day.submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `submitAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | 提交巡检结果 | `amusement.safety_inspection.submit` | `amusement.safety_inspection.submitted` / `amusement.safety_inspection.blocked` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `unarchiveAmusementProjectEntitlement` | `POST` | `/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/unarchive` | 恢复游乐项目权益绑定 | `amusement.project_entitlement.manage` | `amusement.project_entitlement.changed` / `amusement.project_entitlement.change_failed` | web | operation 级补充 |
| ANA-风控分析 | `createReviewLock` | `POST` | `/api/v1/analytics/review-locks` | 锁定经营复盘快照 | `analytics.review.lock` | `analytics.review.locked` / `analytics.review.lock_failed` | web | operation 级补充 |
| ANA-风控分析 | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | 创建风险alert | `analytics.risk_alert.manage` | `risk_alert.created` / `risk_alert.create_failed` | web | operation 级补充 |
| ANA-风控分析 | `disposeMetricAnomaly` | `POST` | `/api/v1/analytics/metric-anomalies/{id}/dispose` | 处置指标异常 | `analytics.metric_anomaly.manage` | `analytics.metric_anomaly.disposed` / `analytics.metric_anomaly.disposed_failed` | web | operation 级补充 |
| ANA-风控分析 | `exportMonthlyReport` | `GET` | `/api/v1/analytics/reports/monthly-export` | 月度经营报表导出(分权) | `analytics.report.export` | `analytics.monthly_report.exported` / `analytics.monthly_report.export_failed` | web | operation 级补充 |
| ANA-风控分析 | `fetchHolidayCalendar` | `POST` | `/api/v1/analytics/holiday-calendar/fetch` | 从 URL 抓取节假日日历 | `analytics.operating_params.manage` | `analytics.holiday_calendar.fetched` / `analytics.holiday_calendar.fetch_failed` | web | operation 级补充 |
| ANA-风控分析 | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | Generate local LLM AI quality suggestion | `analytics.risk_alert.manage` | `analytics.ai_quality.local_llm_suggestion.created` / `analytics.ai_quality.local_llm_suggestion.failed` | web | operation 级补充 |
| ANA-风控分析 | `getAbandonedEntitlementRevenue` | `GET` | `/api/v1/analytics/abandoned-entitlement-revenue` | 放弃权益额外收入报表(票种×权益类型) | `analytics.dashboard.view` | `analytics.abandoned_entitlement_revenue.viewed` / `analytics.abandoned_entitlement_revenue.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAiThresholds` | `GET` | `/api/v1/analytics/ai-thresholds` | AI/对标阈值配置 | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | P0 经营看板下钻（按域） | `analytics.dashboard.view` | `analytics.dashboard_domain.viewed` / `analytics.dashboard_domain.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnalyticsTrafficHourly` | `GET` | `/api/v1/analytics/traffic-hourly` | 分时段客流分布 | `analytics.dashboard.view` | `analytics.traffic_hourly.viewed` / `analytics.traffic_hourly.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnomalyConfig` | `GET` | `/api/v1/analytics/anomaly-config` | 读指标异常检测阈值 | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
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
| ANA-风控分析 | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | List P2 business metric snapshots for operational analytics | `analytics.dashboard.view` | `analytics_business_metric_snapshot.viewed.success` / `analytics_business_metric_snapshot.viewed.failure` | web | operation 级补充 |
| ANA-风控分析 | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | 查询客户意向 | `analytics.risk_alert.view` | `analytics.customer_intent.viewed` / `analytics.customer_intent.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listExpiringContracts` | `GET` | `/api/v1/analytics/occupancy/expiring-contracts` | 到期预警合同明细 | `contract.view` | `analytics.expiring_contracts.viewed` / `analytics.expiring_contracts.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listHolidayPeriods` | `GET` | `/api/v1/analytics/holiday-calendar` | 节假日日历 | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | 查询商户健康度scores | `analytics.dashboard.view` | `analytics.merchant_health.viewed` / `analytics.merchant_health.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMerchantPenalties` | `GET` | `/api/v1/merchant-penalties` | 查询本项目商户处罚记录与单次扣分上下限 | `merchant.penalty.record` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | 查询指标异常 | `analytics.risk_alert.view` | `analytics.metric_anomaly.viewed` / `analytics.metric_anomaly.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricAnomalyTimeline` | `GET` | `/api/v1/analytics/metric-anomalies/{id}/timeline` | 查询指标异常处置历史时间线 | `analytics.risk_alert.view` | `analytics.metric_anomaly.timeline_viewed` / `analytics.metric_anomaly.timeline_viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | 查询指标定义 | `analytics.dashboard.view` | `analytics.metric_definition.viewed` / `analytics.metric_definition.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listNotificationWindows` | `GET` | `/api/v1/analytics/notification-windows` | 提醒窗天数配置 | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `listOperatingParams` | `GET` | `/api/v1/analytics/operating-params` | 查询综合体经营参数（坪效面积） | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `listOverdueWorkOrders` | `GET` | `/api/v1/analytics/work-order-sla/overdue-orders` | 超时工单明细(时效钻取) | `facility.work_order.manage` | `analytics.overdue_work_orders.viewed` / `analytics.overdue_work_orders.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRevenueAttributionRules` | `GET` | `/api/v1/analytics/revenue-attribution-rules` | 商户收入归因口径规则清单 | `analytics.dashboard.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| ANA-风控分析 | `listReviewLocks` | `GET` | `/api/v1/analytics/review-locks` | 经营复盘锁定记录 | `analytics.review.lock` | `analytics.review_lock.viewed` / `analytics.review_lock.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | 风险预警 | `analytics.risk_alert.view` | `risk_alert.viewed` / `risk_alert.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | 查询风险规则 | `analytics.risk_alert.view` | `analytics.risk_rule.viewed` / `analytics.risk_rule.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | 查询员工服务scores | `analytics.risk_alert.view` | `analytics.staff_service_score.viewed` / `analytics.staff_service_score.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listVacantSpaces` | `GET` | `/api/v1/analytics/occupancy/vacant-spaces` | 空置铺位明细(出租率钻取) | `contract.view` | `analytics.vacant_spaces.viewed` / `analytics.vacant_spaces.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `recordMerchantPenalty` | `POST` | `/api/v1/merchant-penalties` | 登记商户处罚（扣分由登记人填写） | `merchant.penalty.record` | `merchant.penalty_recorded` / `merchant.penalty_record_failed` | web | operation 级补充 |
| ANA-风控分析 | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | 处理风险alert | `analytics.risk_alert.manage` | `analytics.risk_alert.resolved` / `analytics.risk_alert.resolved_failed` | web | operation 级补充 |
| ANA-风控分析 | `saveMerchantPenaltyBounds` | `PUT` | `/api/v1/merchant-penalty-bounds/{project_id}` | 配置本项目单次扣分上下限 | `merchant.penalty.bounds.manage` | `merchant.penalty_bounds_updated` / `merchant.penalty_bounds_update_failed` | web | operation 级补充 |
| ANA-风控分析 | `updateAiThresholds` | `POST` | `/api/v1/analytics/ai-thresholds` | 维护 AI/对标阈值 | `analytics.operating_params.manage` | `analytics.ai_thresholds.updated` / `analytics.ai_thresholds.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `updateAnomalyConfig` | `POST` | `/api/v1/analytics/anomaly-config` | 改指标异常检测阈值 | `analytics.operating_params.manage` | `analytics.anomaly_config.updated` / `analytics.anomaly_config.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertHolidayPeriod` | `POST` | `/api/v1/analytics/holiday-calendar` | 手动录入节假日区间 | `analytics.operating_params.manage` | `analytics.holiday_calendar.updated` / `analytics.holiday_calendar.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertNotificationWindow` | `POST` | `/api/v1/analytics/notification-windows` | 维护提醒窗天数 | `platform.notification_window.manage` | `analytics.notification_windows.updated` / `analytics.notification_windows.update_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertOperatingParams` | `POST` | `/api/v1/analytics/operating-params` | 新增或更新综合体经营参数（坪效面积） | `analytics.operating_params.manage` | `operating_params.changed` / `operating_params.change_failed` | web | operation 级补充 |
| ANA-风控分析 | `upsertRevenueAttributionRule` | `POST` | `/api/v1/analytics/revenue-attribution-rules` | 维护商户收入归因口径规则 | `analytics.operating_params.manage` | `analytics.revenue_attribution_rule.updated` / `analytics.revenue_attribution_rule.update_failed` | web | operation 级补充 |
| AUTH-登录会话 | `issueMasterAuthChallenge` | `POST` | `/api/v1/auth/master/challenge` | 破窗登录：取一次性挑战码 | `public` | `auth.master.challenge_issued` / `master_auth.challenge_rate_limited` | web | operation 级补充 |
| AUTH-登录会话 | `login` | `POST` | `/api/v1/auth/login` | 员工或商户员工登录 | `public` | `auth.login.succeeded` / `auth.login.failed` | web, merchant_portal, merchant_app, staff_work_phone, tablet, kiosk, dealer_portal | 已在任务映射正文出现 |
| AUTH-登录会话 | `logout` | `POST` | `/api/v1/auth/logout` | 退出登录 | `authenticated` | `auth.logout` / `auth.logout_failed` | web, merchant_portal, merchant_app, staff_work_phone, tablet, kiosk, dealer_portal | 已在任务映射正文出现 |
| AUTH-登录会话 | `masterAuthLogin` | `POST` | `/api/v1/auth/master/login` | 破窗登录：提交挑战签名换取最高权限会话 | `public` | `auth.master.login_succeeded` / `auth.master.login_failed` | web | operation 级补充 |
| AUTH-登录会话 | `refreshToken` | `POST` | `/api/v1/auth/refresh` | 刷新会话 | `authenticated` | `auth.token.refreshed` / `auth.token_refresh_failed` | web, merchant_portal, merchant_app, staff_work_phone, tablet, kiosk | operation 级补充 |
| AUTH-账号权限 | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | 确认/同意员工通知 | `staff.notice.view` | `staff.notice_acknowledged` / `staff.notice_acknowledge_failed` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| AUTH-账号权限 | `applyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/apply` | 应用权限模板 | `access.permission_template.apply` | `permission_template.applied` / `permission_template.apply_failed` | web | operation 级补充 |
| AUTH-账号权限 | `approveDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-approve` | 审批员工档案删除申请 | `staff.profile.approve` | `staff.profile.delete_approved` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `approveStaffDeviceRebind` | `POST` | `/api/v1/staff/device-credentials/{id}/approve-rebind` | 审批换机生效 | `staff.device.review` | `staff_device.rebound` / `staff_device.rebind_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `archiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/archive` | 存档员工档案 | `staff.profile.submit` | `staff.profile.archived` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | 分配员工职级与薪资 | `access.staff_level.manage` | `access.staff_level.assigned` / `access.staff_level.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `assignUserPosition` | `PUT` | `/api/v1/access/users/{id}/position` | 分配用户岗位并套用岗位权限 | `access.role.assign` | `access.user_position.assigned` / `access.user_position.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `assignUserRole` | `POST` | `/api/v1/access/role-assignments` | 为用户分配角色 | `access.role.assign` | `access.role.assigned` / `access.role.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `bindStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials` | 绑定/换机申请员工设备凭证 | `staff.device.bind` | `staff_device.bound` / `staff_device.bind_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `changePositionRoleTemplateLifecycle` | `POST` | `/api/v1/access/position-role-templates/{id}/lifecycle` | 岗位角色模板存档、取消存档、申请删除与审批删除 | `access.role.assign` | `access.position_role_template.archive` / `access.position_role_template.lifecycle_failed` | web | operation 级补充 |
| AUTH-账号权限 | `changeUserScope` | `PUT` | `/api/v1/access/users/{id}/scope` | 变更人员所属项目或商户（新范围岗位权限送审） | `access.role.assign` | `access.user_scope.changed` / `access.user_scope.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `copyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/copy` | 复制权限模板 | `access.permission_template.manage` | `permission_template.copied` / `permission_template.copy_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests` | 创建需主管复核的授权申请 | `access.grant_request.apply` | `access.grant_request.applied` / `access.grant_request.apply_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createAccessUser` | `POST` | `/api/v1/access/users` | 创建权限用户 | `access.user.manage` | `access.user.changed` / `access.user.changed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createPermissionTemplate` | `POST` | `/api/v1/access/permission-templates` | 创建权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createPositionRoleTemplate` | `POST` | `/api/v1/access/position-role-templates` | 创建岗位角色模板 | `access.role.assign` | `access.position_role_template.saved` / `access.position_role_template.save_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createRole` | `POST` | `/api/v1/access/roles` | 创建角色 | `access.role.manage` | `role.changed` / `role.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants` | 临时授权 | `access.temporary_grant.create` | `permission_template.applied` / `temporary_grant.create_failed` | web | operation 级补充 |
| AUTH-账号权限 | `deletePositionRoleTemplate` | `DELETE` | `/api/v1/access/position-role-templates/{id}` | 删除岗位角色模板 | `access.role.assign` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `getAccessGrantPolicy` | `GET` | `/api/v1/access/grant-policy` | 读取授权政策 | `access.grant_request.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `getAccessGrantSummary` | `GET` | `/api/v1/access/grant-requests/summary` | 授权申请治理汇总 | `access.grant_request.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | 查询当前账号有效权限集 | `authenticated` | `not_applicable` / `not_applicable` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `getRoleViewableProjects` | `GET` | `/api/v1/access/roles/{id}/viewable-projects` | 角色可查看项目现状 | `access.role.manage` | `role.viewable_projects.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | 查询指定账号有效权限集 | `access.user.view` | `not_applicable` / `not_applicable` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `listAccessGrantRequests` | `GET` | `/api/v1/access/grant-requests` | 授权申请列表 | `access.grant_request.view` | `access.grant_request.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listAccessUsers` | `GET` | `/api/v1/access/users` | 查询权限用户 | `access.user.view` | `access.user.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listApprovalLines` | `GET` | `/api/v1/access/approval-lines` | 查询部门和平行审批线配置 | `access.role.assign` | `access.approval_line.listed` / `access.approval_line.list_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | 查询员工通知 | `staff.notice.view` | `staff.notice.viewed` / `not_applicable` | staff_private_phone, web, mobile_admin, staff_work_phone | operation 级补充 |
| AUTH-账号权限 | `listOrgHierarchy` | `GET` | `/api/v1/access/org-hierarchy` | 查询组织层级（直接上级与所属部门） | `access.role.assign` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listPermissions` | `GET` | `/api/v1/access/permissions` | 权限点列表 | `access.permission.view` | `permission.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | 权限模板列表 | `access.permission_template.view` | `permission_template.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listPositionRoleTemplates` | `GET` | `/api/v1/access/position-role-templates` | 岗位角色模板列表 | `access.role.assign` | `access.position_role_template.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listRoleProjectOptions` | `GET` | `/api/v1/access/role-project-options` | 角色可查看项目候选清单 | `access.role.manage` | `access.role_project_options.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listRoles` | `GET` | `/api/v1/access/roles` | 角色列表 | `access.role.view` | `role.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | 查询职级目录 | `access.staff_level.view` | `access.staff_level.viewed` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | 查询员工档案 | `staff.profile.view` | `staff.profile.viewed` / `not_applicable` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `removeUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides/remove` | 解除员工单独授予或显式拒绝 | `access.role.assign` | `access.user_permission_override.remove` / `access.user_permission_override.remove_failed` | web | operation 级补充 |
| AUTH-账号权限 | `reportStaffDeviceIntegrity` | `POST` | `/api/v1/staff/device-credentials/{id}/report-integrity` | 端侧完整性上报 | `staff.device.bind` | `staff_device.integrity_reported` / `staff_device.integrity_report_failed` | staff_private_phone | operation 级补充 |
| AUTH-账号权限 | `requestDeleteStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/delete-request` | 申请删除员工档案 | `staff.profile.review` | `staff.profile.delete_requested` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `reviewAccessGrantRequest` | `POST` | `/api/v1/access/grant-requests/{id}/review` | 复核授权申请 | `access.grant_request.review` | `access.grant_request.reviewed` / `access.grant_request.review_failed` | web | operation 级补充 |
| AUTH-账号权限 | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | 审批员工注册 | `staff.profile.review` | `staff.approved` / `staff.review_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials/{id}/revoke` | 解绑/失效设备凭证 | `staff.device.revoke` | `staff_device.revoked` / `staff_device.revoke_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | 撤销临时授权 | `access.temporary_grant.revoke` | `permission_template.revoked` / `temporary_grant.revoke_failed` | web | operation 级补充 |
| AUTH-账号权限 | `setOrgDirectSuperior` | `PUT` | `/api/v1/access/org-hierarchy/{user_id}` | 设置员工的直接上级与所属部门 | `access.role.assign` | `access.org_hierarchy.superior_set` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `setStaffSalesPosition` | `POST` | `/api/v1/staff/profiles/{id}/sales-position` | 设置员工销售岗标识 | `staff.profile.review` | `staff.profile.sales_position_changed` / `staff.profile.sales_position_change_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `setUserPermissionOverrides` | `POST` | `/api/v1/access/users/{id}/permission-overrides` | 设置员工单独授予或显式拒绝 | `access.role.assign` | `access.user_permission_override.set` / `access.user_permission_override.set_failed` | web | operation 级补充 |
| AUTH-账号权限 | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | 商户代员工提交注册 | `staff.profile.submit` | `staff.registered` / `staff.register_failed` | web, merchant_app, merchant_portal, staff_work_phone | operation 级补充 |
| AUTH-账号权限 | `unarchiveStaffProfile` | `POST` | `/api/v1/staff/profiles/{id}/unarchive` | 取消存档员工档案 | `staff.profile.submit` | `staff.profile.unarchived` / `not_applicable` | web | operation 级补充 |
| AUTH-账号权限 | `unassignUserRole` | `POST` | `/api/v1/access/role-assignments/unassign` | 取消用户角色 | `access.role.assign` | `access.role.unassigned` / `access.role.unassign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updateAccessGrantPolicy` | `POST` | `/api/v1/access/grant-policy` | 保存授权政策 | `admission.policy.manage` | `access.grant_policy.changed` / `access.grant_policy.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updatePermissionTemplate` | `PUT` | `/api/v1/access/permission-templates/{id}` | 修改权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updatePositionRoleTemplate` | `PUT` | `/api/v1/access/position-role-templates/{id}` | 更新岗位角色模板 | `access.role.assign` | `access.position_role_template.saved` / `access.position_role_template.save_failed` | web | operation 级补充 |
| AUTH-账号权限 | `updateRoleViewableProjects` | `PUT` | `/api/v1/access/roles/{id}/viewable-projects` | 增减角色可查看项目 | `access.role.manage` | `role.viewable_projects.changed` / `role.viewable_projects.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `upsertApprovalLine` | `PUT` | `/api/v1/access/approval-lines/{line_code}` | 维护部门和平行审批线配置 | `access.role.assign` | `access.approval_line.saved` / `access.approval_line.save_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `addBarBillTip` | `POST` | `/api/v1/bar/bills/{bill_id}/tip` | 账单加小费 | `bar.bill.tip` | `bar.bill_tip_added` / `bar.bill_tip_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | 新增酒吧订单物品 | `bar.order.create` | `bar.order_item.created` / `bar.order_item.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `adjustBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/adjustments` | 对酒水账单应用调整 | `bar.bill.adjust` | `bar.bill.adjust` / `bar.bill.adjust_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `approveDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-approve` | 审批酒吧套餐删除申请 | `bar.combo.approve` | `bar.combo.delete_approved` / `bar.combo.delete_approve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `approveDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-approve` | 审批酒吧商品删除申请 | `bar.product.approve` | `bar.product.delete_approved` / `bar.product.delete_approve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `approveDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-approve` | 审批酒吧商品分类删除申请 | `bar.category.approve` | `bar.category.delete_approved` / `bar.category.delete_approve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `approveDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-approve` | 审批卡座预订（订台记录）删除申请 | `bar.reservation.approve` | `bar.reservation.delete_approved` / `bar.reservation.delete_approve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/archive` | 存档酒吧套餐 | `bar.combo.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/archive` | 存档酒吧商品 | `bar.product.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/archive` | 存档酒吧商品分类 | `bar.category.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/archive` | 存档卡座预订（订台记录） | `bar.reservation.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `archiveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes/{code}/archive` | 下线退酒原因（软删留理由） | `bar.order_item.void_decide` | `bar.void_reason_code.archived` / `bar.void_reason_code.archive_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `assignBarProductCategory` | `POST` | `/api/v1/bar/catalog/products/{product_id}/category` | 给酒吧商品设分类 | `bar.product.manage` | `bar.product.category_assigned` / `bar.product.category_assigned_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `assignBarTableSpace` | `PUT` | `/api/v1/bar/tables/{table_id}/space` | 卡座/桌台空间指派（回填） | `bar.table.manage` | `bar.table_space.assigned` / `bar.table_space.assign_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | 卡座销售提成计算 | `bar.table_commission.calculate` | `bar.table_commission.calculated` / `bar.table_commission.calculate_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | 订台取消/爽约（按时限退定金或没收） | `bar.reservation.manage` | `bar.reservation.cancelled` / `bar.reservation.cancel_failed` | web, kiosk, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | 订台到店登记（定金不抵扣账单，保持已支付待判；达标后原路退回、未达标关台没收） | `bar.reservation.manage` | `bar.reservation_checked_in` / `bar.reservation_checkin_failed` | web, kiosk, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `clearBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/clear` | 清台登记恢复空闲 | `bar.table.open_close` | `bar.table.cleared` / `bar.table.clear_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `closeBarPayrollMonth` | `POST` | `/api/v1/bar/payroll-runs/close` | 按实际来源生成不可变月结及差额 | `bar.payroll.manage` | `bar.payroll.month_processed` / `bar.payroll.month_processed_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `closeBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-close` | 实际服务完成后关闭责任区间 | `bar.table.open_close` | `bar.service.closed` / `bar.service.closed_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `closeBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/close` | 班结对账 | `bar.shift.close` | `bar.shift_closed` / `bar.shift_close_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `confirmBarOrderSuspensionBadDebt` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/bad-debt` | 经理确认挂起订单无法收款（转坏账） | `bar.bill.hang_cancel` | `bar.order_suspension_bad_debt` / `bar.order_suspension_bad_debt_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `confirmBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/confirm` | 双方分别本人确认约定 | `bar.payroll.manage` | `bar.payroll_rule.confirmed_merchant` / `bar.payroll_rule.confirm_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarBillPayIntent` | `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | 酒吧账单支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | tablet, web, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | 创建订台定金规则（固定额/按低消比例） | `bar.deposit_rule.manage` | `bar.deposit_rule.created` / `bar.deposit_rule.create_failed` | web, kiosk, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `createBarOrder` | `POST` | `/api/v1/bar/orders` | 点单 | `bar.order.create` | `bar.order_created` / `bar.order_create_failed` | tablet, web, kiosk, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | 创建酒吧订单账单 | `bar.order.create` | `bar.bill.created` / `bar.bill.created_failed` | web, kiosk, tablet, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarProduct` | `POST` | `/api/v1/bar/products` | 创建酒吧商品 | `bar.product.manage` | `bar.product.created` / `bar.product.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | 订台或卡座预订 | `bar.reservation.create` | `bar.reservation.created` / `bar.reservation.create_failed` | web, miniapp, app, kiosk, staff_private_phone, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarTable` | `POST` | `/api/v1/bar/tables` | 创建桌台或卡座 | `bar.table.manage` | `bar.table.changed` / `bar.table.change_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarTableSession` | `POST` | `/api/v1/bar/table-sessions` | 创建酒水台位会话（开台） | `bar.table_session.create` | `bar.table_session.create` / `bar.table_session.create_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `decideBarOrderItemVoid` | `POST` | `/api/v1/bar/order-item-voids/{void_id}/decision` | 退菜退酒审批决策（通过/驳回） | `bar.order_item.void` | `bar.order_item_void_decided` / `bar.order_item_void.decide_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `decideBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel-decision` | 经理审批取消挂起订单 | `bar.bill.hang_cancel` | `bar.order_suspension_cancel_decided` / `bar.order_suspension_cancel_decide_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | 出酒或送达确认 | `bar.order.create` | `bar.order_delivered` / `bar.order_deliver_failed` | staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `disableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/disable` | 停用台位（店长） | `bar.table.disable` | `bar.table.disabled` / `bar.table.disable_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `draftBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules` | 保存完整规则草案 | `bar.payroll.manage` | `bar.payroll_rule.drafted` / `bar.payroll_rule.draft_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `enableBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/enable` | 恢复停用台位（店长） | `bar.table.disable` | `bar.table.enabled` / `bar.table.enable_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `exportBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders/export` | 导出挂起订单风险看板（敏感） | `audit.operation_log.export` | `bar.order_suspension.exported` / `bar.order_suspension.export_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `getBarArrivalReminderPolicy` | `GET` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | 查最晚到店时间前提醒提前分钟数（按项目） | `bar.reservation.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `getBarBillRefundContext` | `GET` | `/api/v1/bar/bills/{id}/refund-context` | 读取本账单真实退款来源及累计明细 | `bar.bill.refund` | `bar.refund_context.read` / `bar.refund_context.read_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `getBarCatalogImage` | `GET` | `/api/v1/bar/catalog/images/{name}` | 回读酒吧商品/套餐图片 | `bar.product.view` | `not_applicable` / `not_applicable` | tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `getBarReservationInvitation` | `GET` | `/api/v1/bar/reservations/{id}/invitation` | 查询订卡邀请函 | `bar.reservation.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `handoverBarServiceResponsibility` | `POST` | `/api/v1/bar/table-sessions/{id}/service-handover` | 登记真实服务交接 | `bar.table_session.transfer` | `bar.service.handover` / `bar.service.handover_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `issueBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation` | 签发订卡邀请函 | `bar.reservation.invitation.manage` | `bar.reservation_invitation_issued` / `bar.reservation_invitation_issue_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `joinBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/join` | 加入本钱箱 | `bar.shift.manage` | `bar.shift_joined` / `bar.shift_join_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarBillMerchantRewards` | `GET` | `/api/v1/bar/bills/{bill_id}/rewards` | 查询酒水账单产生的商户消费积分与成长值 | `bar.order.view` | `bar.bill_rewards.viewed` / `bar.bill_rewards.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarBillOperations` | `GET` | `/api/v1/bar/bills/{bill_id}/operations` | 查询酒水账单拆并操作记录 | `bar.bill.split_merge` | `bar.bill_operation.list` / `bar.bill_operation.list_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarBusinessDayConfigs` | `GET` | `/api/v1/bar-business-day-configs` | 查看授权酒吧的营业日配置目录与版本 | `bar.business_day.view` | `bar.business_day.viewed` / `bar.business_day.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `listBarCombos` | `GET` | `/api/v1/bar/catalog/combos` | 查询酒吧套餐 | `bar.combo.view` | `not_applicable` / `not_applicable` | web, tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarEntitlementRedemptions` | `GET` | `/api/v1/bar/entitlement-redemptions` | 查询酒吧酒水权益兑换台账 | `bar.entitlement.read` | `bar.entitlement_redemption.viewed` / `bar.entitlement_redemption.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarOrderItemVoids` | `GET` | `/api/v1/bar/order-item-voids` | 退酒审批待审队列 | `bar.order_item.void` | `bar.order_item_void.viewed` / `bar.order_item_void.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarOrders` | `GET` | `/api/v1/bar/orders` | 查询酒吧订单 | `bar.order.view` | `bar.order.viewed` / `bar.order.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarPayrollRules` | `GET` | `/api/v1/bar/payroll-rules` | 读取收费及分配规则原件 | `bar.payroll.view` | `bar.payroll_rule.read` / `bar.payroll_rule.read_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarPayrollRuns` | `GET` | `/api/v1/bar/payroll-runs` | 读取月结及未结事项 | `bar.payroll.view` | `bar.payroll.read` / `bar.payroll.read_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarProductCategories` | `GET` | `/api/v1/bar/catalog/categories` | 查询酒吧商品分类 | `bar.category.view` | `not_applicable` / `not_applicable` | web, tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarProductionTicketEvents` | `GET` | `/api/v1/bar/production-tickets/{ticket_id}/events` | 查询出品工单事件流 | `bar.production_ticket.view` | `bar.production_ticket_event.list` / `bar.production_ticket_event.list_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | 查询酒吧出品票据 | `bar.order.create` | `bar.production_ticket.viewed` / `bar.production_ticket.viewed_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarProducts` | `GET` | `/api/v1/bar/products` | 查询酒吧商品 | `bar.product.view` | `bar.product.viewed` / `bar.product.viewed_failed` | web, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarProductsByCategory` | `GET` | `/api/v1/bar/catalog/products` | 按分类查询酒吧商品 | `bar.product.view` | `not_applicable` / `not_applicable` | web, tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | 查询酒吧预约 | `bar.reservation.view` | `bar.reservation.viewed` / `bar.reservation.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarServiceResponsibilities` | `GET` | `/api/v1/bar/table-sessions/{id}/service-responsibilities` | 读取原服务责任时间区间 | `bar.payroll.view` | `bar.service.read` / `bar.service.read_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarShifts` | `GET` | `/api/v1/bar/shifts` | 查询收银班次 | `bar.shift.manage` | `bar.shift.viewed` / `bar.shift.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarStaffBookingSuspensions` | `GET` | `/api/v1/bar/staff-booking-suspensions` | 查询代订权限停用中的经手员工 | `bar.staff_booking.restore` | `bar.staff_booking_suspension.viewed` / `bar.staff_booking_suspension.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarSuspendedOrders` | `GET` | `/api/v1/bar/suspended-orders` | 挂起订单风险看板 | `bar.order.view` | `bar.order_suspension.viewed` / `bar.order_suspension.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarTableAvailability` | `GET` | `/api/v1/bar/tables/availability` | 酒吧空位视图(员工代订用) | `bar.reservation.create` | `bar.table_availability.viewed` / `bar.table_availability.view_failed` | staff_private_phone, web, tablet, kiosk, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `listBarTables` | `GET` | `/api/v1/bar/tables` | 查询桌台或卡座 | `bar.table.view` | `bar.table.viewed` / `bar.table.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarTableSessions` | `GET` | `/api/v1/bar/table-sessions` | 查询酒水台位会话列表 | `bar.table_session.view` | `bar.table_session.list` / `bar.table_session.list_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarTablesWithoutSpace` | `GET` | `/api/v1/bar/tables/without-space` | 未回填空间的卡座/桌台清单 | `bar.table.manage` | `bar.table_space_backlog.viewed` / `bar.table_space_backlog.view_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarVoidReasonCodes` | `GET` | `/api/v1/bar/void-reason-codes` | 列退酒原因（中文下拉数据源） | `bar.order_item.void` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `markBarProductSoldOut` | `POST` | `/api/v1/bar/products/{product_id}/sold-out` | 商品沽清/恢复 | `bar.product.sold_out` | `bar.product_sold_out` / `bar.product_sold_out_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `mergeBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/merge` | 合并酒水账单 | `bar.bill.split_merge` | `bar.bill.merge` / `bar.bill.merge_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `mergeBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/merge` | 合并酒水台位会话 | `bar.table_session.merge` | `bar.table_session.merge` / `bar.table_session.merge_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `openBarShift` | `POST` | `/api/v1/bar/shifts/open` | 开班 | `bar.shift.manage` | `bar.shift_opened` / `bar.shift_open_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | 开台 | `bar.table.open_close` | `bar.table.opened` / `bar.table.open_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | 订台定金支付 | `bar.reservation.manage` | `bar.reservation_deposit_paid` / `bar.reservation_deposit_pay_failed` | web, kiosk, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `placeBarScanOrder` | `POST` | `/api/v1/bar/table-sessions/{session_id}/scan-orders` | 扫码点单（绑桌台会话） | `bar.order.create` | `bar.scan_order_created` / `bar.scan_order_create_failed` | miniapp | operation 级补充 |
| BAR-酒吧经营 | `raiseBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly` | 登记账单争议（置异常） | `bar.bill.adjust` | `bar.bill_anomaly_raised` / `bar.bill_anomaly_raise_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `readBarBusinessDayConfigOriginal` | `GET` | `/api/v1/bar-business-day-configs/{id}` | 逐笔查看酒吧营业日配置审批原件 | `governance.supervisor_review.view` | `bar.business_day.original_viewed` / `bar.business_day.original_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `reassignBarTableSessionAttribution` | `POST` | `/api/v1/bar/table-sessions/{session_id}/attribution-reassign` | 改派开台客人来源与销售人员（店长办理） | `bar.table_session.attribution_reassign` | `bar.table_session.attribution_reassigned` / `bar.table_session.attribution_reassign_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `redeemBarDrinkEntitlement` | `POST` | `/api/v1/bar/entitlement-redemptions/drink` | 酒水权益兑换承接 | `bar.entitlement.redeem` | `bar.entitlement_redeemed` / `bar.entitlement_redeem_failed` | staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `reopenBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/reopen` | 账单反结 | `bar.bill.reopen` | `bar.bill_reopened` / `bar.bill_reopen_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestBarBillStoredValueSmsCode` | `POST` | `/api/v1/bar/bills/{bill_id}/stored-value/sms` | 酒吧账单储值付款短信验证码 | `finance.payment.register` | `customer.otp_requested` / `customer.otp_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestBarOrderSuspensionCancel` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/cancel` | 收银员发起取消挂起订单 | `bar.bill.hang` | `bar.order_suspension_cancel_requested` / `bar.order_suspension_cancel_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestDeleteBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/delete-request` | 申请删除酒吧套餐 | `bar.combo.manage` | `bar.combo.delete_requested` / `bar.combo.delete_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestDeleteBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/delete-request` | 申请删除酒吧商品 | `bar.product.manage` | `bar.product.delete_requested` / `bar.product.delete_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestDeleteBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/delete-request` | 申请删除酒吧商品分类 | `bar.category.manage` | `bar.category.delete_requested` / `bar.category.delete_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `requestDeleteBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/delete-request` | 申请删除卡座预订（订台记录） | `bar.reservation.manage` | `bar.reservation.delete_requested` / `bar.reservation.delete_request_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `resolveBarBillAnomaly` | `POST` | `/api/v1/bar/bills/{bill_id}/anomaly/resolve` | 主管处置异常账单 | `bar.bill.anomaly.resolve` | `bar.bill_anomaly_resolved` / `bar.bill_anomaly_resolve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `resolveBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/resolve` | 交班复核平账 | `finance.daily_close.manage` | `bar.shift_resolved` / `bar.shift_resolve_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `restoreBarStaffBookingSuspension` | `POST` | `/api/v1/bar/staff-booking-suspensions/{id}/restore` | 店长复核恢复员工代订权限 | `bar.staff_booking.restore` | `bar.staff_booking_restored` / `bar.staff_booking_restore_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `revokeBarReservationInvitation` | `POST` | `/api/v1/bar/reservations/{id}/invitation/revoke` | 吊销订卡邀请函 | `bar.reservation.invitation.manage` | `bar.reservation_invitation_revoked` / `bar.reservation_invitation_revoke_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarArrivalReminderPolicy` | `PUT` | `/api/v1/bar/arrival-reminder-policies/{project_id}` | 配置最晚到店时间前提醒提前分钟数及上下限（按项目） | `bar.reservation.manage` | `bar.arrival_reminder_policy_updated` / `bar.arrival_reminder_policy_update_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarCombo` | `POST` | `/api/v1/bar/catalog/combos` | 新增或修改酒吧套餐 | `bar.combo.manage` | `bar.combo.saved` / `bar.combo.saved_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories` | 新增或修改酒吧商品分类 | `bar.category.manage` | `bar.category.saved` / `bar.category.saved_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `saveBarVoidReasonCode` | `POST` | `/api/v1/bar/void-reason-codes` | 新增或修改退酒原因 | `bar.order_item.void_decide` | `bar.void_reason_code.saved` / `bar.void_reason_code.save_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | 账单结清（可带一张本商户折扣券：核销并原子抵扣应付，一次结账一张） | `finance.payment.register` | `bar.bill.settled` / `bar.bill.settle_failed` | tablet, web, kiosk, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `signBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/sign` | 交班单签字 | `bar.shift.manage` | `bar.shift_signed` / `bar.shift_sign_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `splitBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/split` | 拆分酒水账单 | `bar.bill.split_merge` | `bar.bill.split` / `bar.bill.split_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `splitBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/split` | 拆分酒水台位会话（拆台） | `bar.table_session.split` | `bar.table.split` / `bar.table.split_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `submitBarBusinessDayConfig` | `POST` | `/api/v1/bar-business-day-configs` | 提交酒吧营业日最高权限审批 | `bar.business_day.manage` | `bar.business_day.submit_completed` / `bar.business_day.submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `submitBarPayrollRule` | `POST` | `/api/v1/bar/payroll-rules/{id}/submit` | 提交有原件的已配置审批 | `bar.payroll.manage` | `bar.payroll_rule.submitted` / `bar.payroll_rule.submit_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `submitBarTableSessionSalesBackfill` | `POST` | `/api/v1/bar/table-sessions/{session_id}/sales-backfill` | 补录台位会话销售归属（人工补录，店长复核） | `bar.table_session.create` | `evidence.manual_backfill_submitted` / `evidence.manual_backfill_submit_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `submitBarTableSessionServiceBackfill` | `POST` | `/api/v1/bar/table-sessions/{id}/service-backfill` | 补录台位会话服务责任区间（人工补录，主管复核） | `bar.table_session.transfer` | `evidence.manual_backfill_submitted` / `evidence.manual_backfill_submit_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `suspendBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/suspend` | 挂起包厢未付款订单 | `bar.bill.hang` | `bar.order_suspended` / `bar.order_suspend_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `transferBarTableSession` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer` | 转台（台位会话转移） | `bar.table_session.transfer` | `bar.table.transferred` / `bar.table_session.transfer_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `transitionBarProductionTicket` | `POST` | `/api/v1/bar/production-tickets/{ticket_id}/transition` | 流转出品工单状态 | `bar.production_ticket.manage` | `bar.production_ticket.transition` / `bar.production_ticket.transition_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `unarchiveBarCombo` | `POST` | `/api/v1/bar/catalog/combos/{id}/unarchive` | 取消存档酒吧套餐 | `bar.combo.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `unarchiveBarProduct` | `POST` | `/api/v1/bar/catalog/products/{id}/unarchive` | 取消存档酒吧商品 | `bar.product.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `unarchiveBarProductCategory` | `POST` | `/api/v1/bar/catalog/categories/{id}/unarchive` | 取消存档酒吧商品分类 | `bar.category.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `unarchiveBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/unarchive` | 取消存档卡座预订（订台记录） | `bar.reservation.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| BAR-酒吧经营 | `undoBarTableTransfer` | `POST` | `/api/v1/bar/table-sessions/{session_id}/transfer-undo` | 撤销最近一次转台 | `bar.table_session.transfer` | `bar.table.transfer_undone` / `bar.table_session.transfer_undo_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `uploadBarCatalogImage` | `POST` | `/api/v1/bar/catalog/images` | 上传酒吧商品/套餐图片 | `bar.product.manage` | `bar.catalog.image_uploaded` / `bar.catalog.image_uploaded_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `verifyBarReservationInvitationEntry` | `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | 订卡邀请函入场核验 | `bar.table.guest_checkin` | `bar.reservation_invitation_entered` / `bar.reservation_invitation_entry_failed` | staff_work_phone, web | operation 级补充 |
| BAR-酒吧经营 | `voidBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/void-item` | 退菜退酒 | `bar.order_item.void` | `bar.order_item_voided` / `bar.order_item_void_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `witnessBarShift` | `POST` | `/api/v1/bar/shifts/{shift_id}/witness` | 交班见证归档 | `finance.daily_close.manage` | `bar.shift_witnessed` / `bar.shift_witness_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `writeOffBarOrderSuspension` | `POST` | `/api/v1/bar/orders/{order_id}/suspension/write-off` | 财务确认坏账订单核销结案 | `finance.ar_ap.manage` | `bar.order_suspension_written_off` / `bar.order_suspension_write_off_failed` | web | operation 级补充 |
| CUS-客人端 | `applyCustomerStudentCertification` | `POST` | `/api/v1/customer/student-certifications` | 客人申请学生认证 | `not_applicable` | `customer.student_cert.applied` / `customer.student_cert.apply_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `approveCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/approve` | 批准黑名单申请的当前这一级（夜班值班经理批准 → 娱乐平台负责人终审，终审通过即执行） | `customer.blacklist.approve` | `customer.blacklist.approved` / `customer.blacklist.approve_failed` | web | operation 级补充 |
| CUS-客人端 | `approveStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/approve` | 学生认证审核通过 | `ticket.student_cert.review` | `customer.student_cert.approved` / `customer.student_cert.approve_failed` | web | operation 级补充 |
| CUS-客人端 | `claimCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/claim` | 确认领取转赠(手机号只指定人可领;链接/二维码先确认者得) | `not_applicable` | `voucher_transfer.claimed` / `voucher_transfer.claim_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `configureCustomerChurnAlertSetting` | `PUT` | `/api/v1/customer-churn-alert-setting` | 配置同号一年内注销与注册合计次数上限（全平台，值与上下限同配） | `customer.account.manage` | `customer.churn_alert_setting.configured` / `customer.churn_alert_setting.config_failed` | web | operation 级补充 |
| CUS-客人端 | `configureCustomerRestrictionSetting` | `PUT` | `/api/v1/customer-restriction-settings/{project_id}` | 配置本项目会员受限时长（天数与上下限同配） | `customer.account.manage` | `customer.restriction_setting.configured` / `customer.restriction_setting.config_failed` | web | operation 级补充 |
| CUS-客人端 | `configureStoredValueMerchantCard` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-config` | 配置商户储值卡（仅平台）：发卡资格 + 资金模式（预收/代收）+ 消费扣款顺序 | `customer.stored_value.refund` | `stored_value.merchant_card_configured` / `stored_value.merchant_card_config_failed` | web | operation 级补充 |
| CUS-客人端 | `configureStoredValueMerchantRefundRule` | `POST` | `/api/v1/loyalty/stored-value/merchant-refund-rules` | 配置商户级储值卡退款规则（发卡商户自设可否退/比例） | `customer.stored_value.refund` | `stored_value.merchant_refund_rule_configured` / `stored_value.merchant_refund_rule_failed` | web | operation 级补充 |
| CUS-客人端 | `confirmCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments/{payment_intent_id}/confirm` | 客人回前台确认定金在线付款（查单） | `not_applicable` | `customer.payment_confirmed` / `customer.payment_confirm_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `confirmCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments/{payment_id}/confirm` | 客人停车缴费确认 | `not_applicable` | `parking.payment_paid` / `parking.payment_confirm_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `counterRefundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/counter-refund` | 会员储值到店柜台退款（商户预收卡唯一可达退款路径） | `customer.stored_value.refund` | `stored_value.counter_refund` / `stored_value.counter_refund_failed` | web | operation 级补充 |
| CUS-客人端 | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | 隐私同意记录 | `not_applicable` | `customer.consent.created` / `customer.consent.create_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `createCustomerParkingPayment` | `POST` | `/api/v1/customer/parking/payments` | 客人停车缴费下单 | `not_applicable` | `parking.payment_created` / `parking.payment_create_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `createVoucherDefinition` | `POST` | `/api/v1/vouchers/definitions` | 新建券定义（商户折扣券/店内实物券；外部伙伴券仍配置在通票权益模板 V282） | `customer.voucher.manage` | `voucher.definition_created` / `voucher.definition_create_failed` | web | operation 级补充 |
| CUS-客人端 | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | 客人每日签到 | `not_applicable` | `customer.checked_in` / `customer.check_in_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerLogout` | `POST` | `/api/v1/customer/auth/logout` | 客人登出（清除免登录会话 cookie） | `public` | `customer.logged_out` / `customer.logout_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | 客人手机号登录 | `public` | `customer.auth.login` / `customer.auth.login_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | 客人储值账户查询 | `customer.stored_value.view` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueAccounts` | `GET` | `/api/v1/customer/stored-value/accounts` | 客人储值卡列表（我的全部卡：通用卡 + 各商户卡分别余额） | `customer.stored_value.view` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | 客人储值充值入账（支付成功后） | `customer.stored_value.topup` | `customer.stored_value.settle` / `customer.stored_value.settle_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | 客人储值充值（绑支付意图） | `customer.stored_value.topup` | `customer.stored_value.topup` / `customer.stored_value.topup_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `customerVoucherWallet` | `GET` | `/api/v1/customer/vouchers` | 我的券包（客人端只读：积分兑换券/营销赠券/通票第三方券聚合，按状态分组展示） | `customer.voucher.view` | `voucher.customer_wallet_viewed` / `voucher.customer_wallet_view_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `deregisterCustomerAccount` | `POST` | `/api/v1/customer/deregister` | 客人自助注销本人账号（个人信息删除、积分券清零、名下存酒须确认放弃） | `not_applicable` | `customer.account.deregistered` / `customer.account.deregister_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | 手动触发会员等级评估 | `customer.loyalty.evaluate` | `member.level_changed` / `customer.level_evaluate_failed` | web | operation 级补充 |
| CUS-客人端 | `findCustomerParkingSession` | `GET` | `/api/v1/customer/parking/sessions` | 客人按车牌或临时码查在场车辆停车费 | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | 冻结会员积分账户 | `customer.points.freeze` | `customer.points.frozen` / `customer.points.freeze_failed` | web | operation 级补充 |
| CUS-客人端 | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | 冻结会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.frozen` / `stored_value.frozen_failed` | web | operation 级补充 |
| CUS-客人端 | `fulfillPointsRedemption` | `POST` | `/api/v1/loyalty/points-redemptions/{redemption_id}/fulfill` | 积分兑换券领取（店员端） | `customer.loyalty.manage` | `customer.points_voucher_fulfilled` / `customer.points_voucher_fulfill_failed` | web | operation 级补充 |
| CUS-客人端 | `getCustomerChurnAlertSetting` | `GET` | `/api/v1/customer-churn-alert-setting` | 查同号注销与注册频次预警上限（全平台） | `customer.account.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `getCustomerHomeContent` | `GET` | `/api/v1/home-contents/{project_id}` | 查客人端首页已发布与待审批的综合体介绍与活动公告 | `customer.home_content.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `getCustomerParkingCredit` | `GET` | `/api/v1/customer/parking/credits` | 客人查看本日停车消费抵免 | `not_applicable` | `parking.credit.granted` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | 积分余额 | `not_applicable` | `customer.points_viewed` / `customer.points_view_failed` | miniapp, app | 已在任务映射正文出现 |
| CUS-客人端 | `getCustomerPointsLedger` | `GET` | `/api/v1/customer/points/ledger` | 查询积分流水 | `not_applicable` | `customer.points.ledger_viewed` / `customer.points.ledger_view_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `getCustomerRestrictionSetting` | `GET` | `/api/v1/customer-restriction-settings/{project_id}` | 查本项目会员受限时长（未配置时回落出厂 90 天） | `customer.account.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `getCustomerSavedBottlePickupCode` | `GET` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/pickup-code` | 官方动态取酒码（按当前持有人签发，窗口=全场统一刷新间隔，转让后原持有人码作废） | `not_applicable` | `bar.saved_bottle.pickup_code_issued` / `bar.saved_bottle.pickup_code_issue_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `getCustomerSession` | `GET` | `/api/v1/customer/session` | 客人会话恢复（免登录） | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `getCustomerStoredValueTopupRules` | `GET` | `/api/v1/customer/stored-value/topup-rules` | 充值前规则告知（扣款顺序、赠送金性质、退卡口径） | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `getCustomerTerms` | `GET` | `/api/v1/customer/terms` | 当前会员条款与隐私条款（匿名可读） | `public` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `getCustomerVoucherRedeemCode` | `GET` | `/api/v1/customer/vouchers/{voucher_instance_id}/redeem-code` | 我的券动态核销码（按全场统一刷新间隔过期） | `not_applicable` | `voucher.redeem_code_issued` / `voucher.redeem_code_issue_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `getMerchantCardExitNoticePolicy` | `GET` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | 查商户卡退出公示天数（全平台） | `customer.stored_value.refund` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `getMyStudentCertification` | `GET` | `/api/v1/customer/student-certifications/current` | 我的学生认证 | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `getOwnMerchantConsumptionRewards` | `GET` | `/api/v1/customer/merchant-rewards` | 查看本人商户消费积分与成长值 | `not_applicable` | `merchant.reward.customer.viewed` / `merchant.reward.customer.viewed.failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `getStoredValueMerchantRefundRule` | `GET` | `/api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}` | 查询商户级储值卡退款规则 | `customer.stored_value.refund` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `initiateCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers` | 发起券转赠(手机号/微信链接/二维码) | `not_applicable` | `voucher_transfer.created` / `voucher_transfer.create_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `issueVouchers` | `POST` | `/api/v1/vouchers/instances` | 营销赠发券（按定义给客人发放；商户 scope 只能发自家券） | `customer.voucher.manage` | `voucher.issued` / `voucher.issue_failed` | web | operation 级补充 |
| CUS-客人端 | `listCustomerAmusementProjects` | `GET` | `/api/v1/customer/amusement/projects` | 可预定游乐项目与场次（客人端只读） | `public` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerBlacklistRequests` | `GET` | `/api/v1/customer-blacklist-requests` | 黑名单申请列表（待批准在前） | `customer.blacklist.request` | `customer.blacklist_requests_viewed` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | 客人签到记录 | `not_applicable` | `customer.check_ins_viewed` / `customer.check_ins_view_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerComplaintStoreOptions` | `GET` | `/api/v1/customer/complaints/store-options` | 客人投诉「涉及门店」选项 | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerEffectiveTerminalSwitches` | `GET` | `/api/v1/customer/terminal-switches/effective` | 客人端读取本端已关闭的开关行（匿名可读） | `public` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerLevelNotices` | `GET` | `/api/v1/customer/level-notices` | 本人未读会员等级通知（App/小程序启动与回前台拉取并弹出） | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerMerchantCardExitNotices` | `GET` | `/api/v1/customer/merchant-card-exit-notices` | 商户储值卡退出公告（客人端） | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | 我的订单 | `not_applicable` | `customer.orders_viewed` / `customer.orders_view_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `listCustomerPointsRedemptionItems` | `GET` | `/api/v1/customer/points/redemption-items` | 查询积分兑换商品目录 | `not_applicable` | `customer.points.redemption_items_viewed` / `customer.points.redemption_items_view_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerPointsRedemptions` | `GET` | `/api/v1/customer/points/redemptions` | 查询我的积分兑换记录 | `not_applicable` | `customer.points.redemptions_viewed` / `customer.points.redemptions_view_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerProjects` | `GET` | `/api/v1/customer/projects` | 综合体介绍、营业时间与活动公告（客人端首页） | `public` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listCustomerStoredValueTransactions` | `GET` | `/api/v1/customer/stored-value/accounts/{account_id}/transactions` | 我的余额逐笔记录 | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listIncomingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/incoming` | 待我领取的转赠(手机号渠道) | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `listLoyaltyConfigBirthdayRules` | `GET` | `/api/v1/loyalty/config/birthday-rules` | 生日加倍规则配置列表（全平台） | `customer.loyalty.view` | `customer.birthday_rule.listed` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `listLoyaltyConfigCheckInTiers` | `GET` | `/api/v1/loyalty/config/check-in-tiers` | 连续签到阶梯配置列表（按综合体） | `customer.loyalty.view` | `customer.check_in_tier.listed` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `listLoyaltyConfigMemberLevels` | `GET` | `/api/v1/loyalty/config/member-levels` | 会员等级档位配置列表（全平台） | `customer.loyalty.view` | `customer.member_level.listed` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | 会员等级配置列表 | `customer.loyalty.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `listMyCustomerComplaints` | `GET` | `/api/v1/customer/complaints` | 我的投诉 | `not_applicable` | `customer.complaints_viewed` / `customer.complaints_view_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `listMyCustomerSavedBottles` | `GET` | `/api/v1/customer/my-saved-bottles` | 我的存酒（本人名下全部存酒与到期日，跨店汇总，每条标明所属酒吧） | `not_applicable` | `customer.my_saved_bottles_viewed` / `customer.my_saved_bottles_view_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `listOutgoingCustomerVoucherTransfers` | `GET` | `/api/v1/customer/voucher-transfers/outgoing` | 我发起的转赠(撤回入口/状态查看) | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `listStudentCertifications` | `GET` | `/api/v1/student-certifications` | 学生认证审核列表（票务运营岗） | `ticket.student_cert.review` | `customer.student_certs_viewed` / `customer.student_certs_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherDefinitions` | `GET` | `/api/v1/vouchers/definitions` | 券定义列表（统一台账模板：类型/核销方式/结算口径；内部结算价按 voucher.internal_price.view 权限脱敏；每行带只读 merchant_type＝券定义商户业态） | `customer.voucher.view` | `voucher.definitions_viewed` / `voucher.definitions_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherInstances` | `GET` | `/api/v1/vouchers/instances` | 券实例台账（收编积分兑换券＋营销赠券；状态机 issued→redeemed/expired/revoked） | `customer.voucher.view` | `voucher.instances_viewed` / `voucher.instances_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherOverview` | `GET` | `/api/v1/vouchers/overview` | 全部券聚合（读层并入不搬表：统一台账 ∪ 通票第三方合作券 V282） | `customer.voucher.view` | `voucher.overview_viewed` / `voucher.overview_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listVoucherRedemptions` | `GET` | `/api/v1/vouchers/redemptions` | 统一核销记录（append-only：核销点/操作员/结算挂钩一张表；settlement_amount_cent=内部结算价按 voucher.internal_price.view 权限脱敏） | `customer.voucher.view` | `voucher.redemptions_viewed` / `voucher.redemptions_view_failed` | web | operation 级补充 |
| CUS-客人端 | `lookupCustomerAccountByPhone` | `POST` | `/api/v1/customer-accounts/lookup` | 按手机号精确查客人账号（盲索引，返回安全投影；手机号走请求体不入 URL） | `customer.account.manage` | `customer.account.viewed` / `customer.account.view_failed` | web | operation 级补充 |
| CUS-客人端 | `markCustomerLevelNoticeRead` | `POST` | `/api/v1/customer/level-notices/{notice_id}/read` | 会员等级通知标记已读（客人点「知道了」） | `not_applicable` | `member.level_notice.read` / `member.level_notice.read_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `previewCustomerDeregistration` | `GET` | `/api/v1/customer/deregister/preview` | 客人注销预览（提醒名下各储值卡本金/赠送金与名下存酒） | `not_applicable` | `customer.account.deregister_previewed` / `customer.account.deregister_preview_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `previewCustomerVoucherTransfer` | `GET` | `/api/v1/customer/voucher-transfers/preview` | 领取预览(打开链接/二维码,不算领取) | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS-客人端 | `previewVoucherRedemption` | `POST` | `/api/v1/vouchers/redemptions/preview` | 按客人出示的券码查券（只读：不改券状态、不落惰性过期、不消费码、不动库存与预占） | `customer.voucher.redeem` | `voucher.redeem_previewed` / `voucher.redeem_preview_failed` | staff_work_phone, kiosk | operation 级补充 |
| CUS-客人端 | `publishCustomerHomeContent` | `POST` | `/api/v1/home-contents/{project_id}/publish` | 提交发布客人端首页综合体介绍与活动公告（审批后生效） | `customer.home_content.manage` | `customer.home_content_published` / `customer.home_content_publish_failed` | web | operation 级补充 |
| CUS-客人端 | `rateCustomerComplaint` | `POST` | `/api/v1/customer/complaints/{work_order_id}/satisfaction` | 客人对本人投诉评满意度 | `not_applicable` | `customer.complaint_rated` / `customer.complaint_rate_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `readStudentCertificationFile` | `GET` | `/api/v1/student-certifications/{certification_id}/files/{index}` | 回读学生证件照片 | `ticket.student_cert.review` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| CUS-客人端 | `redeemCustomerPoints` | `POST` | `/api/v1/customer/points/redemptions` | 提交积分兑换 | `not_applicable` | `customer.points.redeem` / `customer.points.redeem_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `redeemVoucher` | `POST` | `/api/v1/vouchers/redemptions` | 统一核销（实物券扣库存＋内部结算价挂应付；酒吧商户（merchant_type=bar）的折扣券不在此核销——须在酒吧结账 settleBarBill 时抵扣核销，本端点返回 VOUCHER_REQUIRES_BILL；非酒吧商户的折扣券仍可在此独立核销（操作员裁决 2026-10-20 第三轮第 9 条）；每券一次；响应 settlement_amount_cent 按 voucher.internal_price.view 权限脱敏） | `customer.voucher.redeem` | `voucher.redeemed` / `voucher.redeem_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| CUS-客人端 | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | 会员储值退卡/退余额（后台） | `customer.stored_value.refund` | `stored_value.refund` / `stored_value.refund_failed` | web | operation 级补充 |
| CUS-客人端 | `registerCustomerMember` | `POST` | `/api/v1/customer/auth/register` | 客人注册会员（短信验证＋生日＋满 18 周岁＋同意当前会员条款与隐私条款） | `public` | `customer.registered` / `customer.register_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `reinstateCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/reinstate` | 提前解除受限（原因必填） | `customer.account.manage` | `customer.account.reinstated` / `customer.account.reinstate_failed` | web | operation 级补充 |
| CUS-客人端 | `rejectCustomerBlacklistRequest` | `POST` | `/api/v1/customer-blacklist-requests/{request_id}/reject` | 驳回黑名单申请（由当前这一级有权批准的人驳回，账户不变） | `customer.blacklist.approve` | `customer.blacklist.rejected` / `customer.blacklist.reject_failed` | web | operation 级补充 |
| CUS-客人端 | `rejectStudentCertification` | `POST` | `/api/v1/student-certifications/{certification_id}/reject` | 学生认证审核驳回 | `ticket.student_cert.review` | `customer.student_cert.rejected` / `customer.student_cert.reject_failed` | web | operation 级补充 |
| CUS-客人端 | `renewCustomerSavedBottle` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renew` | 客人本人转存（延期）：短信验证码确认即从当前到期日顺延（转存次数不超上限、只能在到期日前办理） | `not_applicable` | `bar.saved_bottle.renewed` / `bar.saved_bottle.renew_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `requestCustomerBlacklistChange` | `POST` | `/api/v1/customer-accounts/{customer_id}/blacklist-requests` | 申请列入或解除黑名单（保安团队提交，待夜班值班经理与娱乐平台负责人两级批准） | `customer.blacklist.request` | `customer.blacklist.requested` / `customer.blacklist.request_failed` | web | operation 级补充 |
| CUS-客人端 | `requestCustomerLoginSmsCode` | `POST` | `/api/v1/customer/auth/sms-codes` | 客人登录取短信验证码 | `public` | `customer.otp_requested` / `customer.otp_request_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `requestCustomerSavedBottleRenewalCode` | `POST` | `/api/v1/customer/saved-bottles/{saved_bottle_id}/renewal-code` | 客人本人转存发码（只对当前持有人本人，发到本人手机） | `not_applicable` | `bar.saved_bottle.verification_code_requested` / `bar.saved_bottle.verification_code_request_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `requestCustomerStoredValueSmsCode` | `POST` | `/api/v1/customer/stored-value/sms` | 储值付款短信验证码 | `not_applicable` | `customer.otp_requested` / `customer.otp_request_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `revokeCustomerVoucherTransfer` | `POST` | `/api/v1/customer/voucher-transfers/{transfer_id}/revoke` | 撤回转赠(领取前发起人可撤回) | `not_applicable` | `voucher_transfer.revoked` / `voucher_transfer.revoke_failed` | miniapp, app | operation 级补充 |
| CUS-客人端 | `revokeVoucher` | `POST` | `/api/v1/vouchers/instances/{id}/revoke` | 作废券（仅未核销可作废；理由必填进审计留痕） | `customer.voucher.manage` | `voucher.revoked` / `voucher.revoke_failed` | web | operation 级补充 |
| CUS-客人端 | `saveLoyaltyConfigBirthdayRule` | `POST` | `/api/v1/loyalty/config/birthday-rules` | 新增或更新生日加倍规则（全平台，仅全国范围账号） | `customer.loyalty.manage` | `customer.birthday_rule.saved` / `customer.birthday_rule.save_failed` | web | operation 级补充 |
| CUS-客人端 | `saveLoyaltyConfigCheckInTier` | `POST` | `/api/v1/loyalty/config/check-in-tiers` | 新增或更新本项目连续签到阶梯 | `customer.loyalty.manage` | `customer.check_in_tier.saved` / `customer.check_in_tier.save_failed` | web | operation 级补充 |
| CUS-客人端 | `saveLoyaltyConfigMemberLevel` | `POST` | `/api/v1/loyalty/config/member-levels` | 新增或更新会员等级档位（全平台，仅全国范围账号） | `customer.loyalty.manage` | `customer.member_level.saved` / `customer.member_level.save_failed` | web | operation 级补充 |
| CUS-客人端 | `saveMerchantCardExitNoticePolicy` | `POST` | `/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy` | 配置商户卡退出公示天数及上下限（全平台） | `customer.stored_value.refund` | `stored_value.exit_notice_policy_configured` / `stored_value.exit_notice_policy_config_failed` | web | operation 级补充 |
| CUS-客人端 | `startCustomerDepositPayment` | `POST` | `/api/v1/customer/bar-reservations/{reservation_id}/deposit-payments` | 客人本人订台发起定金在线付款 | `not_applicable` | `customer.payment_created` / `customer.payment_create_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `submitCustomerComplaint` | `POST` | `/api/v1/customer/complaints` | 客人提交投诉（文字＋照片、视频、语音） | `not_applicable` | `customer.complaint_submitted` / `customer.complaint_submit_failed` | app, miniapp | operation 级补充 |
| CUS-客人端 | `suspendCustomerAccount` | `POST` | `/api/v1/customer-accounts/{customer_id}/suspend` | 置为受限（只停自助订台与员工代订，到期自动恢复有效） | `customer.account.manage` | `customer.account.suspended` / `customer.account.suspend_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | 解冻会员积分账户 | `customer.points.freeze` | `customer.points.unfrozen` / `customer.points.unfreeze_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | 解冻会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.unfrozen` / `stored_value.unfrozen_failed` | web | operation 级补充 |
| CUS/BAR-客人订台 | `confirmCustomerBarInvitation` | `POST` | `/api/v1/customer/bar-invitations/confirm` | 客人确认接受订卡邀请函 | `not_applicable` | `customer.bar_invitation.confirmed` / `customer.bar_invitation.confirm_failed` | miniapp, app | operation 级补充 |
| CUS/BAR-客人订台 | `confirmCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders/{scan_order_id}/confirm` | 扫码点单回前台确认付款（查单） | `public` | `customer.bar_scan_order.paid` / `customer.bar_scan_order.pay_failed` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | 客人卡座预订 | `not_applicable` | `customer.bar_reservation.created` / `customer.bar_reservation.create_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS/BAR-客人订台 | `createCustomerBarScanOrder` | `POST` | `/api/v1/customer/bar-tables/{table_id}/scan-orders` | 扫码点单下单（即时付款） | `public` | `customer.bar_scan_order.created` / `customer.bar_scan_order.create_failed` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `getCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}` | 客人查看自己的订卡邀请函 | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS/BAR-客人订台 | `getCustomerBarInvitationEntryCode` | `GET` | `/api/v1/customer/bar-invitations/{reservation_id}/entry-code` | 本人动态入场码 | `not_applicable` | `customer.bar_invitation.entry_code_issued` / `customer.bar_invitation.entry_code_failed` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `getCustomerBarTableCurrentBill` | `GET` | `/api/v1/customer/bar-tables/{table_id}/current-bill` | 本桌当前账单 | `public` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | 客人可订台资源 | `not_applicable` | `customer.bar_reservation_options_viewed` / `customer.bar_reservation_options_view_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS/BAR-客人订台 | `listCustomerBarScanMenu` | `GET` | `/api/v1/customer/bar-tables/{table_id}/menu` | 扫码点单查看菜单 | `public` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `listMyCustomerBarReservations` | `GET` | `/api/v1/customer/bar-reservations` | 我的订台 | `not_applicable` | `customer.bar_reservations.viewed` / `customer.bar_reservations.view_failed` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `previewCustomerBarInvitation` | `GET` | `/api/v1/customer/bar-invitations/preview` | 扫邀请函二维码预览 | `not_applicable` | `customer.bar_invitation.previewed` / `customer.bar_invitation.preview_failed` | app, miniapp | operation 级补充 |
| CUS/BAR-客人订台 | `removeCustomerBarInvitationMember` | `POST` | `/api/v1/customer/bar-invitations/{reservation_id}/members/{member_id}/remove` | 开卡人移出尚未入场的被邀请人 | `not_applicable` | `customer.bar_invitation.member_removed` / `customer.bar_invitation.member_remove_failed` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `claimCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/claim` | 确认领取转赠(手机号只指定人可领;链接/二维码先确认者得) | `not_applicable` | `ticket_transfer.claimed` / `ticket_transfer.claim_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `confirmCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments/{payment_intent_id}/confirm` | 客人回前台确认在线付款（查单） | `not_applicable` | `customer.payment_confirmed` / `customer.payment_confirm_failed` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | 客人自助购票下单 | `not_applicable` | `customer.ticket_order.created` / `customer.ticket_order.create_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `getCustomerTicketQr` | `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | 我的动态二维码 | `not_applicable` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `getOwnTicketUseRewards` | `GET` | `/api/v1/customer/ticket-rewards` | 查看本人实际使用门票奖励及待追回积分 | `not_applicable` | `ticket.reward.customer.viewed` / `ticket.reward.customer.viewed.failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `initiateCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers` | 发起门票转赠(手机号/微信链接/二维码) | `not_applicable` | `ticket_transfer.created` / `ticket_transfer.create_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerOrderTickets` | `GET` | `/api/v1/customer/orders/{order_id}/tickets` | 客人查询本人订单内票实例/权益（安全投影；点票出码，无需手输票 id） | `not_applicable` | `customer.order_tickets_viewed` / `customer.order_tickets_view_failed` | miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketChannels` | `GET` | `/api/v1/customer/ticket-channels` | 第三方渠道接入状态（匿名可读，供客户端置灰） | `public` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketExpiryReminders` | `GET` | `/api/v1/customer/ticket-expiry-reminders` | 未核销票到期提醒（App/小程序启动与回前台拉取） | `not_applicable` | `not_applicable` / `not_applicable` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | 客人可购票种和票价 | `public` | `customer.ticket_products_viewed` / `customer.ticket_products_view_failed` | miniapp, app, kiosk | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketUsages` | `GET` | `/api/v1/customer/tickets/{ticket_id}/usages` | 我的票使用记录 | `not_applicable` | `customer.ticket_usages_viewed` / `customer.ticket_usages_view_failed` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `listIncomingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/incoming` | 待我领取的转赠(手机号渠道) | `not_applicable` | `customer.ticket_transfer_incoming_viewed` / `customer.ticket_transfer_incoming_view_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listMyTickets` | `GET` | `/api/v1/customer/my-tickets` | 我的票(持有人视角:自购未转+受赠,含转赠中标记) | `not_applicable` | `customer.my_tickets_viewed` / `customer.my_tickets_view_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listOutgoingCustomerTicketTransfers` | `GET` | `/api/v1/customer/ticket-transfers/outgoing` | 我发起的转赠(撤回入口/状态查看) | `not_applicable` | `customer.ticket_transfer_outgoing_viewed` / `customer.ticket_transfer_outgoing_view_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listPublishedTicketRewardRules` | `GET` | `/api/v1/customer/ticket-reward-rules` | 查看已公布门票积分及成长值规则 | `not_applicable` | `ticket.reward.rules.viewed` / `ticket.reward.rules.viewed.failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `payCustomerTicketOrderWithStoredValue` | `POST` | `/api/v1/customer/orders/{order_id}/stored-value/pay` | 客人本人订单储值付款（可只付一部分） | `customer.stored_value.consume` | `stored_value.consume` / `customer.stored_value.pay_failed` | app, miniapp | operation 级补充 |
| CUS/TIC-客人购票 | `previewCustomerTicketTransfer` | `GET` | `/api/v1/customer/ticket-transfers/preview` | 领取预览(打开链接/二维码,不算领取) | `not_applicable` | `not_applicable` / `not_applicable` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `revokeCustomerTicketTransfer` | `POST` | `/api/v1/customer/ticket-transfers/{transfer_id}/revoke` | 撤回转赠(领取前发起人可撤回) | `not_applicable` | `ticket_transfer.revoked` / `ticket_transfer.revoke_failed` | miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `startCustomerTicketPayment` | `POST` | `/api/v1/customer/orders/{order_id}/payments` | 客人本人订单发起在线付款 | `not_applicable` | `customer.payment_created` / `customer.payment_create_failed` | app, miniapp | operation 级补充 |
| EVD-事件证据 | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | 关联证据附件到业务对象 | `evidence.attachment.create` | `evidence.attachment_bound` / `evidence.attachment_bind_failed` | web, staff_work_phone | operation 级补充 |
| EVD-事件证据 | `claimRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/claim` | 认领风险预警 | `risk_alert.claim` | `exception.claimed` / `exception.claim_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | 完成上传确认 | `evidence.attachment.create` | `evidence.attachment_completed` / `evidence.attachment_complete_failed` | web, staff_work_phone | operation 级补充 |
| EVD-事件证据 | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | 申请证据附件预签名上传 | `evidence.attachment.create` | `evidence.upload_intent_created` / `evidence.upload_intent_failed` | web, staff_work_phone | operation 级补充 |
| EVD-事件证据 | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | 创建证据事件 | `evidence.event.create` | `evidence.events_reported` / `evidence.events_report_failed` | staff_work_phone, kiosk, web | operation 级补充 |
| EVD-事件证据 | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | 风险或异常复核 | `evidence.review.create` | `evidence.review_created` / `evidence.review_create_failed` | web | operation 级补充 |
| EVD-事件证据 | `dismissRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/dismiss` | 忽略关闭风险预警 | `risk_alert.dismiss` | `exception.dismissed` / `exception.dismiss_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `downloadEditableExportCopy` | `GET` | `/api/v1/security/export-files/{id}/editable-copy` | 下载导出文件可编辑版（一次批准发放一次） | `authenticated` | `security.export.editable_downloaded` / `security.export.editable_download_failed` | web, merchant_portal | operation 级补充 |
| EVD-事件证据 | `escalateRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/escalate` | 升级风险预警 | `risk_alert.escalate` | `exception.escalated` / `exception.escalate_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | 业务时间线 | `evidence.timeline.view` | `evidence.timeline_viewed` / `evidence.timeline_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `getManualBackfillPolicy` | `GET` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | 查人工补录参数（补录时限与录音提前量，按项目） | `evidence.manual_backfill.policy.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| EVD-事件证据 | `getRiskAlertDetail` | `GET` | `/api/v1/evidence/risk-alerts/{alert_id}` | 查询风险预警详情 | `risk_alert.view` | `not_applicable` / `not_applicable` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `getTraceabilityAutoCaptureRate` | `GET` | `/api/v1/evidence/traceability/auto-capture-rate` | 查询业务流程追溯自动采集率 | `evidence.timeline.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| EVD-事件证据 | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | 事件查询 | `evidence.event.view` | `evidence.event_viewed` / `evidence.event_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `listManualBackfills` | `GET` | `/api/v1/evidence/manual-backfills` | 查某对象的人工补录记录（与原始记录并列展示） | `authenticated` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| EVD-事件证据 | `listMyExports` | `GET` | `/api/v1/security/my-exports` | 我的导出（本人导出文件与打开密码） | `authenticated` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| EVD-事件证据 | `lookupExportPassword` | `POST` | `/api/v1/security/export-files/lookup` | 导出密码反查 | `security.watermark.trace` | `security.export_password.traced` / `security.export_password.trace_failed` | web | operation 级补充 |
| EVD-事件证据 | `readEvidenceAttachmentFile` | `GET` | `/api/v1/evidence/attachments/{attachment_id}/file` | 回读证据文件 | `evidence.timeline.view` | `evidence.attachment_file_viewed` / `evidence.attachment_file_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `recordPageView` | `POST` | `/api/v1/security/page-views` | 登记页面打开并发暗纹码 | `authenticated` | `security.sensitive_page.viewed` / `security.page_view.failed` | web, merchant_portal, merchant_app, staff_work_phone, staff_private_phone | operation 级补充 |
| EVD-事件证据 | `reportRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts` | 上报统一风险预警 | `risk_alert.report` | `exception.reported` / `exception.report_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `requestEditableExportCopy` | `POST` | `/api/v1/security/export-files/{id}/editable-requests` | 申请导出文件可编辑版（最高权限审批） | `authenticated` | `security.export.editable_requested` / `security.export.editable_request_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| EVD-事件证据 | `resolveEvidenceRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/resolve` | 关闭风险预警（需结论） | `risk_alert.resolve` | `exception.resolved` / `exception.resolve_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `saveManualBackfillPolicy` | `PUT` | `/api/v1/evidence/manual-backfill-policies/{project_id}` | 配置人工补录参数（补录时限、录音提前量及各自上下限，按项目） | `evidence.manual_backfill.policy.manage` | `evidence.manual_backfill_policy_updated` / `evidence.manual_backfill_policy_update_failed` | web | operation 级补充 |
| EVD-事件证据 | `searchRiskAlerts` | `GET` | `/api/v1/evidence/risk-alerts` | 查询风险预警列表 | `risk_alert.view` | `exception.alerts_viewed` / `exception.alerts_view_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `traceWatermark` | `POST` | `/api/v1/security/watermarks/trace` | 截屏或拍屏照片按暗纹反查 | `security.watermark.trace` | `security.watermark.traced` / `security.watermark.trace_failed` | web | operation 级补充 |
| EVD-事件证据 | `transferRiskAlert` | `POST` | `/api/v1/evidence/risk-alerts/{alert_id}/transfer` | 转交风险预警 | `risk_alert.transfer` | `exception.transferred` / `exception.transfer_failed` | web, mobile_admin | operation 级补充 |
| EVD-事件证据 | `uploadEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/upload` | 上传证据照片/视频 | `evidence.attachment.create` | `evidence.attachment_uploaded` / `evidence.attachment_upload_failed` | web, staff_work_phone | operation 级补充 |
| FIN-财务台账 | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | 锁定后补录审批决定 | `finance.locked_backfill.approve` | `finance.locked_backfill_approved` / `finance.locked_backfill_decision_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `approveDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/approve` | 审批通过：pending_review → approved；净发放额>0 时给主理人挂应付（source_type=dividend_payout，一单一挂）；提交人不得审批自己提交的单 | `finance.dividend.manage` | `dividend.approved` / `dividend.approve_failed` | web | operation 级补充 |
| FIN-财务台账 | `archiveDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/archive` | 归档日结 | `finance.daily_close.lock` | `finance_daily_close.archived` / `finance_daily_close.archive_failed` | web | operation 级补充 |
| FIN-财务台账 | `billPropertyFee` | `POST` | `/api/v1/finance/property-fee/bills` | 物业费按期计费（每平米固定单价 × 面积，不退不补） | `finance.property_fee.manage` | `finance_property_fee.billed` / `finance_property_fee.bill_failed` | web | operation 级补充 |
| FIN-财务台账 | `billTurnoverRent` | `POST` | `/api/v1/finance/turnover-rent/bills` | 营业额抽成计费生成应收 | `finance.turnover_rent.bill` | `finance.turnover_rent.billed` / `finance.turnover_rent.bill_failed` | web | operation 级补充 |
| FIN-财务台账 | `closeFinancePeriod` | `POST` | `/api/v1/finance/period-closes` | 封账（按综合体＋月份） | `finance.period_close.execute` | `finance_period.closed` / `finance_period.close_failed` | web | operation 级补充 |
| FIN-财务台账 | `closeFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/close` | 提交差异关闭复核 | `finance.reconciliation.resolve` | `finance.reconciliation.difference_review_submitted` / `finance.reconciliation.difference_review_submit_failed` | web | operation 级补充 |
| FIN-财务台账 | `computeDividendStatements` | `POST` | `/api/v1/finance/dividends/statements/compute` | 期间分红作业：对每个生效中方案出计算单（净利润=收入−成本−费用−税费；幂等可重跑，草稿/预览/驳回作废重算，进入审批流跳过） | `finance.dividend.manage` | `dividend.computed` / `dividend.compute_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/confirm` | 确认日结 | `finance.daily_close.lock` | `finance_daily_close.confirmed` / `finance_daily_close.confirm_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmDividendLegalAmount` | `POST` | `/api/v1/finance/dividends/statements/{id}/legal-confirmation` | 财务登记依法可分配金额（仅正式草稿/已驳回单；本次分配取经营口径与核定额较小者并重算） | `finance.dividend.manage` | `dividend.legal_confirmed` / `dividend.legal_confirm_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmFinanceRefundResult` | `POST` | `/api/v1/finance/refunds/{refund_id}/confirm` | 另一人核实已有执行记录的实际退款结果 | `finance.refund.create` | `finance_refund.confirmed` / `finance_refund.confirm_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | 现金或 POS 线下收款确认 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `confirmPayrollBankPaymentResult` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/result` | 核实工资银行付款实际结果 | `finance.payroll.payment.register` | `finance.payroll.result` / `finance.payroll.payment_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| FIN-财务台账 | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | 手工新增应收应付台账项 | `finance.ar_ap.manage` | `finance_ar_ap.created` / `finance_ar_ap.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | 应收应付收付款登记 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | 生成日结 | `finance.daily_close.lock` | `finance_daily_close.generated` / `finance_daily_close.generate_failed` | web | operation 级补充 |
| FIN-财务台账 | `createDividendPlan` | `POST` | `/api/v1/finance/dividends/plans` | 新建分红方案（比例/税率/周期/收入域可配，80/20 仅默认值；销货成本、全部费用与该期税费固定扣减，发放付账日必填） | `finance.dividend.manage` | `dividend.plan_created` / `dividend.plan_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences` | 创建对账差异 | `finance.reconciliation.difference.create` | `finance.reconciliation.difference_created` / `finance.reconciliation.difference_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | 创建财务退款记录 | `finance.refund.create` | `finance_refund.created` / `finance_refund.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses` | 手工录入商户费用（人工/摊销/其他，理由必填） | `finance.merchant_expense.manage` | `merchant_expense.manual_created` / `merchant_expense.manual_create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createPaymentIntent` | `POST` | `/api/v1/finance/payment-intents` | 创建聚合支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, kiosk, miniapp, app | operation 级补充 |
| FIN-财务台账 | `createPropertyFeeRate` | `POST` | `/api/v1/finance/property-fee/rates` | 维护物业费费率（项目级、可按期） | `finance.property_fee.manage` | `finance_property_fee.rate_set` / `finance_property_fee.rate_set_failed` | web | operation 级补充 |
| FIN-财务台账 | `createTurnoverRentRule` | `POST` | `/api/v1/finance/turnover-rent/rules` | 新建营业额抽成规则草案 | `finance.turnover_rent.bill` | `finance_turnover_rent_rule.created` / `finance_turnover_rent_rule.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `declareEntityTax` | `POST` | `/api/v1/finance/entity-taxes` | 登记税费实际申报金额并提交财务、税务、法务审核与最高确认 | `finance.entity_tax.manage` | `entity_tax.declared` / `entity_tax.declare_failed` | web | operation 级补充 |
| FIN-财务台账 | `exportArApItems` | `GET` | `/api/v1/finance/ar-ap-items/export` | 导出应收应付台账快照 | `finance.ar_ap.export` | `finance_ar_ap.exported` / `finance_ar_ap.export_failed` | web | operation 级补充 |
| FIN-财务台账 | `getDividendStatement` | `GET` | `/api/v1/finance/dividends/statements/{id}` | 分红计算单详情（头 + 算式明细行，逐项可追溯到三个来源台账） | `finance.dividend.view` | `dividend.statement_viewed` / `dividend.statement_view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| FIN-财务台账 | `getFinanceRefund` | `GET` | `/api/v1/finance/refunds/{refund_id}` | 读取退款详情和全部办理记录 | `finance.ar_ap.view` | `finance_refund.viewed` / `finance_refund.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `getInternalPayableExecution` | `GET` | `/api/v1/finance/internal-payables/{ar_ap_item_id}` | 查普通内部结算往来的付款时点与执行状态 | `finance.ar_ap.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| FIN-财务台账 | `getPayrollBankPaymentContext` | `GET` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | 读取工资银行记录与同日补差事项 | `finance.payroll.view` | `finance.payroll.attempts_viewed` / `finance.payroll.attempts_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| FIN-财务台账 | `getPointsRecoveryDetail` | `GET` | `/api/v1/finance/points-recoveries/{customerId}` | 查看会员全部积分追回及豁免记录 | `finance.points_recovery.view` | `points.recovery.viewed` / `points.recovery.view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| FIN-财务台账 | `identifyFinanceReconciliationDifference` | `POST` | `/api/v1/finance/reconciliation/differences/{difference_id}/identify` | 识别对账差异 | `finance.reconciliation.resolve` | `finance.reconciliation.difference_identified` / `finance.reconciliation.difference_identify_failed` | web | operation 级补充 |
| FIN-财务台账 | `initiateInternalPayment` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts` | 登记内部应付已发起付款(银行在途) | `finance.payment.register` | `finance.internal_payment.initiated` / `finance.internal_payment.initiate_failed` | web | operation 级补充 |
| FIN-财务台账 | `liftDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds/{hold_id}/lift` | 恢复暂缓分红（登记恢复的协商结果，之后该期照常出正式单） | `finance.dividend.manage` | `dividend.hold_lifted` / `dividend.hold_lift_failed` | web | operation 级补充 |
| FIN-财务台账 | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | 查询应收应付台账 | `finance.ar_ap.view` | `finance_ar_ap.viewed` / `finance_ar_ap.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listArApPayments` | `GET` | `/api/v1/finance/ar-ap-items/{id}/payments` | 读取所选台账原收付款流水 | `finance.ar_ap.view` | `ar_ap_payment.listed` / `ar_ap_payment.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDailyCloseAdjustments` | `GET` | `/api/v1/finance/daily-closes/{id}/adjustments` | 查询日结的锁定后补录 | `finance.daily_close.manage` | `finance.daily_close_adjustment.viewed` / `finance.daily_close_adjustment.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDailyCloses` | `GET` | `/api/v1/finance/daily-closes` | 查询日结批次 | `finance.daily_close.manage` | `finance.daily_close.viewed` / `finance.daily_close.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | 查询日结七源汇总 | `finance.daily_close.manage` | `finance.daily_close_summary.viewed` / `finance.daily_close_summary.view_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `listDividendPeriodHolds` | `GET` | `/api/v1/finance/dividends/holds` | 整期暂不分红／暂缓分红登记列表（平台财务持股分红页用，最近登记在前） | `finance.dividend.view` | `dividend.holds_listed` / `dividend.holds_list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDividendPlans` | `GET` | `/api/v1/finance/dividends/plans` | 分红方案列表（商户 scope 账号只见本商户） | `finance.dividend.view` | `dividend.plans_listed` / `dividend.plans_list_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| FIN-财务台账 | `listDividendReserveReleases` | `GET` | `/api/v1/finance/dividends/reserve-releases` | 发放保护线留存·待释放/已释放台账（商户 scope 账号只见本商户，主理人视图同入口） | `finance.dividend.view` | `dividend.reserve_ledger_listed` / `dividend.reserve_ledger_list_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| FIN-财务台账 | `listDividendStatements` | `GET` | `/api/v1/finance/dividends/statements` | 分红计算单分页（商户 scope 账号只见本商户，主理人视图同入口） | `finance.dividend.view` | `dividend.statements_listed` / `dividend.statements_list_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| FIN-财务台账 | `listEntityTaxDeclarations` | `GET` | `/api/v1/finance/entity-taxes` | 税费申报登记分页查询（按经营主体、所属月、税种、状态筛选） | `finance.entity_tax.view` | `entity_tax.listed` / `entity_tax.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listFinancePeriodCloses` | `GET` | `/api/v1/finance/period-closes` | 查询已封账月份 | `finance.period_close.execute` | `finance_period.viewed` / `finance_period.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listFinanceReconciliationStageLimits` | `GET` | `/api/v1/finance/reconciliation/stage-limits` | 查询对账差异停留时限（按项目） | `finance.reconciliation.policy.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| FIN-财务台账 | `listFinanceRefunds` | `GET` | `/api/v1/finance/refunds` | 读取退款台账及未结状态 | `finance.ar_ap.view` | `finance_refund.listed` / `finance_refund.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listMerchantExpensePeriodLocks` | `GET` | `/api/v1/finance/merchant-expenses/period-locks` | 查询某月已关账（分红核算完成、费用台账冻结）的商户清单（前端据此禁用费用录入/冲销） | `finance.merchant_expense.view` | `merchant_expense.period_locks_viewed` / `merchant_expense.period_locks_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses` | 商户费用归集台账分页查询（分红净利润费用侧） | `finance.merchant_expense.view` | `merchant_expense.listed` / `merchant_expense.list_failed` | web | operation 级补充 |
| FIN-财务台账 | `listPropertyFeeRates` | `GET` | `/api/v1/finance/property-fee/rates` | 列出某项目的物业费费率 | `finance.property_fee.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| FIN-财务台账 | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | 查询对账批次 | `finance.reconciliation.view` | `finance.reconciliation_batch.viewed` / `finance.reconciliation_batch.viewed_failed` | web | operation 级补充 |
| FIN-财务台账 | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | 对账差异 | `finance.reconciliation.view` | `finance_reconciliation.difference_found` / `finance_reconciliation.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listRefundAuthorityLimits` | `GET` | `/api/v1/finance/refund-authority/limits` | 查询退款自助额度配置 | `refund.authority.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| FIN-财务台账 | `listRefundAutoRules` | `GET` | `/api/v1/finance/refund-authority/rules` | 查询退款自动放行规则配置 | `refund.authority.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| FIN-财务台账 | `lockDailyClose` | `POST` | `/api/v1/finance/daily-closes/{id}/lock` | 锁定日结 | `finance.daily_close.lock` | `finance_daily_close.locked` / `finance_daily_close.lock_failed` | web | operation 级补充 |
| FIN-财务台账 | `markDailyCloseFinal` | `POST` | `/api/v1/finance/daily-closes/{id}/final-marks` | 五类数据最终标记 | `finance.daily_close.lock` | `finance_daily_close.final_marked` / `finance_daily_close.final_mark_failed` | web | operation 级补充 |
| FIN-财务台账 | `markDividendStatementPaid` | `POST` | `/api/v1/finance/dividends/statements/{id}/mark-paid` | 标记发放：approved → paid（实际打款走应收应付台账既有付款确认流程） | `finance.dividend.manage` | `dividend.paid_marked` / `dividend.mark_paid_failed` | web | operation 级补充 |
| FIN-财务台账 | `postInventoryVariance` | `POST` | `/api/v1/finance/inventory-variances/post` | 库存差异复核入账为 AR/AP | `finance.inventory_variance.post` | `finance.inventory_variance.posted` / `finance.inventory_variance.post_failed` | web | operation 级补充 |
| FIN-财务台账 | `postponeInternalPaymentForFunds` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/funds-insufficient` | 资金不足顺延至下一个付账日 | `finance.payment.register` | `finance.internal_payment.postponed` / `finance.internal_payment.postpone_failed` | web | operation 级补充 |
| FIN-财务台账 | `preparePayrollBankPayment` | `POST` | `/api/v1/finance/payroll/{statementId}/payment-attempts` | 准备工资银行付款 | `finance.payroll.payment.register` | `finance.payroll.prepare` / `finance.payroll.payment_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| FIN-财务台账 | `queryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/query` | 查询原退款单的渠道权威结果 | `finance.ar_ap.view` | `finance_refund.queried` / `finance_refund.query_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordDepositAction` | `POST` | `/api/v1/finance/deposits/actions` | 押金台账动作（收取/退还/抵扣/没收） | `finance.deposit.manage` | `finance.deposit.action_recorded` / `finance.deposit.action_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordDividendPeriodHold` | `POST` | `/api/v1/finance/dividends/holds` | 登记整期暂不分红或暂缓分红（原因、协商结果必填，不另走审批；该期正式草稿随登记作废） | `finance.dividend.manage` | `dividend.hold_recorded` / `dividend.hold_record_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordFinanceRefundExecution` | `POST` | `/api/v1/finance/refunds/{refund_id}/execution` | 实际支付经办人本人登记原路执行凭证 | `finance.refund.create` | `finance_refund.execution_recorded` / `finance_refund.execution_failed` | web | operation 级补充 |
| FIN-财务台账 | `recordInternalPaymentFailure` | `POST` | `/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts/{attempt_id}/failure` | 登记内部应付付款明确失败(付款失败待处理) | `finance.payment.register` | `finance.internal_payment.failed` / `finance.internal_payment.fail_record_failed` | web | operation 级补充 |
| FIN-财务台账 | `rejectDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/reject` | 驳回：pending_review → rejected（理由必填；驳回后可重算） | `finance.dividend.manage` | `dividend.rejected` / `dividend.reject_failed` | web | operation 级补充 |
| FIN-财务台账 | `releaseDividendReserve` | `POST` | `/api/v1/finance/dividends/statements/{id}/release-reserve` | 手动释放该计算单的发放保护线留存（re-tag 待释放留存应付为已释放，使主理人可真正拿到该笔钱） | `finance.dividend.manage` | `dividend.reserve_released` / `dividend.reserve_release_failed` | web | operation 级补充 |
| FIN-财务台账 | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | 锁定后补录审批申请 | `finance.locked_backfill.request` | `finance.locked_backfill_requested` / `finance.locked_backfill_request_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `retryFinanceRefund` | `POST` | `/api/v1/finance/refunds/{refund_id}/retry` | 明确失败或尚未发起的退款在原单重试 | `finance.refund.create` | `finance_refund.retried` / `finance_refund.retry_failed` | web | operation 级补充 |
| FIN-财务台账 | `reverseMerchantExpenseEntry` | `POST` | `/api/v1/finance/merchant-expenses/{id}/reverse` | 红字冲销费用台账行（负金额新行，不改历史行，理由必填） | `finance.merchant_expense.manage` | `merchant_expense.reversed` / `merchant_expense.reverse_failed` | web | operation 级补充 |
| FIN-财务台账 | `saveFinanceReconciliationStageLimit` | `PUT` | `/api/v1/finance/reconciliation/stage-limits/{project_id}` | 配置对账差异停留时限（按项目、按阶段，时限与上下限同配） | `finance.reconciliation.policy.manage` | `finance.reconciliation_stage_limit.saved` / `finance.reconciliation_stage_limit.save_failed` | web | operation 级补充 |
| FIN-财务台账 | `submitDividendStatement` | `POST` | `/api/v1/finance/dividends/statements/{id}/submit` | 提交审阅：draft → pending_review（预览单不可提交） | `finance.dividend.manage` | `dividend.submitted` / `dividend.submit_failed` | web | operation 级补充 |
| FIN-财务台账 | `submitPayrollBankPaymentFact` | `POST` | `/api/v1/finance/payroll/payment-attempts/{id}/submit` | 登记工资银行已发起事实 | `finance.payroll.payment.register` | `finance.payroll.submit` / `finance.payroll.payment_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| FIN-财务台账 | `summarizeMerchantExpenses` | `GET` | `/api/v1/finance/merchant-expenses/summary` | 商户费用期间汇总（按商户+月份分类型合计，分红引擎费用侧取数点） | `finance.merchant_expense.view` | `merchant_expense.summary_viewed` / `merchant_expense.summary_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `syncMerchantExpenseAuto` | `POST` | `/api/v1/finance/merchant-expenses/sync-auto` | 自动挂接能耗费/物业费/租金账单到费用台账（按期幂等可重跑） | `finance.merchant_expense.manage` | `merchant_expense.auto_synced` / `merchant_expense.auto_sync_failed` | web | operation 级补充 |
| FIN-财务台账 | `updateDividendPlan` | `PUT` | `/api/v1/finance/dividends/plans/{id}` | 修改分红方案（改动审计留痕；历史分红单按单内快照追溯，不受影响） | `finance.dividend.manage` | `dividend.plan_updated` / `dividend.plan_update_failed` | web | operation 级补充 |
| FIN-财务台账 | `upsertRefundAuthorityLimit` | `POST` | `/api/v1/finance/refund-authority/limits` | 新增或更新退款自助额度 | `refund.authority.manage` | `refund_authority.changed` / `refund_authority.change_failed` | web | operation 级补充 |
| FIN-财务台账 | `upsertRefundAutoRule` | `POST` | `/api/v1/finance/refund-authority/rules` | 新增或更新退款自动放行规则 | `refund.authority.manage` | `refund_authority.changed` / `refund_authority.change_failed` | web | operation 级补充 |
| FIN-财务台账 | `waivePointsRecovery` | `POST` | `/api/v1/finance/points-recoveries/{customerId}/waive` | 财务办理会员全部待追回积分小额豁免 | `finance.points_recovery.waive` | `points.recovery.waive_completed` / `points.recovery.waive_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| GOV-平台治理 | `approveDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-approve` | 审批商户删除申请 | `merchant.profile.approve` | `merchant.delete_approved` / `merchant.delete_approve_failed` | web | operation 级补充 |
| GOV-平台治理 | `approveDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-approve` | 审批空间种类删除申请 | `authenticated` | `space_type.delete_approved` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | 主管审批或驳回复核记录 | `governance.supervisor_review.approve` | `governance.supervisor_review.decided` / `governance.supervisor_review.decide_failed` | web | operation 级补充 |
| GOV-平台治理 | `archiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/archive` | 存档商户（先下线） | `merchant.profile.manage` | `merchant.archived` / `merchant.archive_failed` | web | operation 级补充 |
| GOV-平台治理 | `archiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/archive` | 存档空间种类 | `governance.space.manage` | `space_type.archived` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `assignMerchantProject` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/project` | 补商户主项目（最高权限） | `merchant.profile.manage` | `merchant.project_assigned` / `merchant.project_assign_failed` | web | operation 级补充 |
| GOV-平台治理 | `associateBusinessEntityProject` | `POST` | `/api/v1/platform/business-entities/{entity_id}/projects` | 为经营主体追加关联项目（最高权限） | `entity.binding.manage` | `business_entity.project_associated` / `business_entity.project_associate_failed` | web | operation 级补充 |
| GOV-平台治理 | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | 变更四类主体绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `configureMerchantPendingClearAlert` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | 配置酒吧待清台提醒时限(值与上下限同配) | `merchant.profile.manage` | `merchant.pending_clear_alert_configured` / `merchant.pending_clear_alert_config_failed` | web | operation 级补充 |
| GOV-平台治理 | `configureMerchantPourLossRate` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | 配置酒吧损耗率阈值(值与上下限同配) | `merchant.profile.manage` | `merchant.pour_loss_rate_configured` / `merchant.pour_loss_rate_config_failed` | web | operation 级补充 |
| GOV-平台治理 | `configureMerchantStaffBookingNoShowLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | 配置酒吧员工代订爽约率上限(值与上下限同配) | `merchant.profile.manage` | `merchant.staff_booking_no_show_limit_configured` / `merchant.staff_booking_no_show_limit_config_failed` | web | operation 级补充 |
| GOV-平台治理 | `configureMerchantTableTransferLimit` | `PUT` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | 配置酒吧转台次数上限(值与上下限同配) | `merchant.profile.manage` | `merchant.table_transfer_limit_configured` / `merchant.table_transfer_limit_config_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | 创建经营、运营、合同、结算主体档案 | `entity.binding.manage` | `business_entity.changed` / `business_entity.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | 创建四类主体唯一绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createMerchant` | `POST` | `/api/v1/platform/merchants` | 创建商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `createProject` | `POST` | `/api/v1/platform/projects` | 创建项目 | `governance.project.manage` | `project.created` / `project.create_failed` | web | operation 级补充 |
| GOV-平台治理 | `createSpace` | `POST` | `/api/v1/platform/spaces` | 创建场馆、区域、点位 | `governance.space.manage` | `space.updated` / `space.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `createSpaceType` | `POST` | `/api/v1/platform/space-types` | 录入/修改空间类型字典 | `governance.space.manage` | `space_type.updated` / `space_type.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `deleteSpaceType` | `DELETE` | `/api/v1/platform/space-types/{id}` | 直接删除空间种类（已停用） | `governance.space.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | 评估业务动作是否需要主管复核 | `governance.supervisor_review.manage` | `governance.supervisor_review.evaluated` / `governance.supervisor_review.evaluate_failed` | web | operation 级补充 |
| GOV-平台治理 | `forwardSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/forward` | 把待复核单的当前节点转交给指定人 | `governance.supervisor_review.approve` | `governance.supervisor_review.forwarded` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `getMerchantOperatingProjects` | `GET` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | 查询商户经营项目与加经营项目申请 | `merchant.profile.view` | `merchant.viewed` / `merchant.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `getMerchantPendingClearAlert` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pending-clear-alert` | 查询酒吧待清台提醒时限 | `merchant.profile.view` | `merchant.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `getMerchantPourLossRate` | `GET` | `/api/v1/platform/merchants/{merchant_id}/pour-loss-rate` | 查询酒吧损耗率阈值 | `merchant.profile.view` | `merchant.viewed` / `merchant.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `getMerchantStaffBookingNoShowLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit` | 查询酒吧员工代订爽约率上限 | `merchant.profile.view` | `merchant.viewed` / `merchant.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `getMerchantTableTransferLimit` | `GET` | `/api/v1/platform/merchants/{merchant_id}/table-transfer-limit` | 查询酒吧转台次数上限 | `merchant.profile.view` | `merchant.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `getProjectSensitiveOperationRange` | `GET` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | 查询项目敏感操作授权范围 | `platform.sensitive_operation_range.manage` | `platform.sensitive_operation_range.viewed` / `platform.sensitive_operation_range.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `getSupervisorReviewChain` | `GET` | `/api/v1/platform/supervisor-review/{review_id}/chain` | 查看复核单的审批链节点 | `governance.supervisor_review.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listApprovalDeadlines` | `GET` | `/api/v1/platform/approval-deadlines` | 查询各审批类型的审批时限 | `governance.supervisor_review.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | 查询经营、运营、合同、结算主体档案 | `entity.binding.view` | `business_entity.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | 查询四类主体绑定 | `entity.binding.view` | `entity_binding.viewed` / `entity_binding.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listMerchants` | `GET` | `/api/v1/platform/merchants` | 查询商户 | `merchant.profile.view` | `merchant.viewed` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| GOV-平台治理 | `listPendingSupervisorReviews` | `GET` | `/api/v1/platform/supervisor-review/pending` | 待复核清单（到期与升级、转交状态） | `governance.supervisor_review.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listProjects` | `GET` | `/api/v1/platform/projects` | 查询综合体项目 | `governance.project.view` | `project.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listSpaces` | `GET` | `/api/v1/platform/spaces` | 查询场馆、区域、点位 | `governance.space.view` | `space.viewed` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| GOV-平台治理 | `listSpaceTypes` | `GET` | `/api/v1/platform/space-types` | 空间类型字典列表 | `governance.space.view` | `space_type.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | 查询主管复核与违规阻断策略 | `governance.supervisor_review.view` | `governance.supervisor_review.policies_viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `listVenueBusinessHours` | `GET` | `/api/v1/platform/venue-business-hours` | 查询场地营业时间 | `governance.space.view` | `venue_business_hours.viewed` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `readSupervisorReviewOriginal` | `GET` | `/api/v1/platform/supervisor-review/{review_id}` | 按编号读取有权查看的主管复核原件 | `governance.supervisor_review.view` | `governance.supervisor_review.original_viewed` / `governance.supervisor_review.original_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| GOV-平台治理 | `regenerateSupervisorReviewChain` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/chain/regenerate` | 组织层级补齐后重生成审批链 | `governance.supervisor_review.manage` | `governance.supervisor_review.chain_regenerated` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `requestDeleteMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/delete-request` | 申请删除商户 | `merchant.profile.manage` | `merchant.delete_requested` / `merchant.delete_request_failed` | web | operation 级补充 |
| GOV-平台治理 | `requestDeleteSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/delete-request` | 申请删除空间种类 | `governance.space.manage` | `space_type.delete_requested` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `requestMerchantOperatingProject` | `POST` | `/api/v1/platform/merchants/{merchant_id}/operating-projects` | 申请商户加经营项目(须审批生效) | `merchant.profile.manage` | `merchant.operating_project.requested` / `merchant.operating_project.request_failed` | web | operation 级补充 |
| GOV-平台治理 | `saveProjectSensitiveOperationRange` | `PUT` | `/api/v1/platform/projects/{project_id}/sensitive-operation-range` | 配置项目敏感操作授权范围(圆心、半径与上下限同配) | `platform.sensitive_operation_range.manage` | `platform.sensitive_operation_range.saved` / `platform.sensitive_operation_range.save_failed` | web | operation 级补充 |
| GOV-平台治理 | `saveVenueBusinessHours` | `POST` | `/api/v1/platform/venue-business-hours` | 配置场地营业时间 | `governance.space.manage` | `venue_business_hours.updated` / `venue_business_hours.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `unarchiveMerchant` | `POST` | `/api/v1/platform/merchants/{merchant_id}/unarchive` | 取消存档商户 | `merchant.profile.manage` | `merchant.unarchived` / `merchant.unarchive_failed` | web | operation 级补充 |
| GOV-平台治理 | `unarchiveSpaceType` | `POST` | `/api/v1/platform/space-types/{id}/unarchive` | 取消存档空间种类 | `governance.space.manage` | `space_type.unarchived` / `not_applicable` | web | operation 级补充 |
| GOV-平台治理 | `updateApprovalDeadline` | `PUT` | `/api/v1/platform/approval-deadlines/{action_type}` | 配置一种审批类型的审批时限（时限与上下限同配） | `admission.policy.manage` | `governance.approval_deadline.changed` / `governance.approval_deadline.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | 修改商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| INT-第三方集成 | `archiveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps/{id}/archive` | 停用渠道门店映射 | `integration.channel_store_map.manage` | `integration.channel_store_map.archived` / `integration.channel_store_map.archive_failed` | web | operation 级补充 |
| INT-第三方集成 | `archiveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps/{id}/archive` | 停用渠道票种映射 | `integration.channel_store_map.manage` | `integration.channel_ticket_type_map.archived` / `integration.channel_ticket_type_map.archive_failed` | web | operation 级补充 |
| INT-第三方集成 | `createCompensationTask` | `POST` | `/api/v1/integration/compensation-tasks` | 创建补偿任务 | `integration.compensation.manage` | `integration.compensation_task.created` / `integration.compensation_task.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createIntegrationReconciliationBatch` | `POST` | `/api/v1/integration/reconciliation/batches` | 创建对账批次 | `integration.reconciliation.manage` | `integration.reconciliation_batch.created` / `integration.reconciliation_batch.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createIntegrationReconciliationDifference` | `POST` | `/api/v1/integration/reconciliation/differences` | 创建对账差异 | `integration.reconciliation.manage` | `integration.reconciliation_difference.created` / `integration.reconciliation_difference.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createOrderSync` | `POST` | `/api/v1/integration/order-syncs` | 创建订单同步 | `integration.order_sync.manage` | `integration.order_sync.created` / `integration.order_sync.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createProviderAdapter` | `POST` | `/api/v1/integration/provider-adapters` | 创建供应商适配器占位或 mock adapter | `integration.provider_adapter.create` | `provider_adapter.created` / `provider_adapter.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `createRetryTask` | `POST` | `/api/v1/integration/retry-tasks` | 创建重试任务 | `integration.retry.manage` | `integration.retry_task.created` / `integration.retry_task.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationChannelStoreMaps` | `GET` | `/api/v1/integration/channel-store-maps` | 查询渠道门店映射 | `integration.channel_store_map.view` | `integration.channel_store_map.viewed` / `integration.channel_store_map.view_failed` | web | operation 级补充 |
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
| INT-第三方集成 | `listIntegrationUnattributedOrderSyncs` | `GET` | `/api/v1/integration/order-syncs/unattributed` | 未归属商户的第三方回流订单 | `integration.order_sync.view` | `integration.order_sync.unattributed_viewed` / `integration.order_sync.unattributed_view_failed` | web | operation 级补充 |
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
| INT-第三方集成 | `runChannelAutoReconciliation` | `POST` | `/api/v1/integration/reconciliation/channel-auto` | 渠道自动对账 | `integration.reconciliation.manage` | `integration.channel_reconciliation.completed` / `integration.channel_reconciliation.failed` | web | operation 级补充 |
| INT-第三方集成 | `runRetryTaskNow` | `POST` | `/api/v1/integration/retry-tasks/{id}/run-now` | 立即执行重试任务 | `integration.retry.manage` | `integration.retry_task.run_now` / `integration.retry_task.run_now_failed` | web | operation 级补充 |
| INT-第三方集成 | `saveIntegrationChannelStoreMap` | `POST` | `/api/v1/integration/channel-store-maps` | 登记/改绑渠道门店映射 | `integration.channel_store_map.manage` | `integration.channel_store_map.saved` / `integration.channel_store_map.save_failed` | web | operation 级补充 |
| INT-第三方集成 | `saveIntegrationChannelTicketTypeMap` | `POST` | `/api/v1/integration/channel-ticket-type-maps` | 登记/改绑渠道票种映射 | `integration.channel_store_map.manage` | `integration.channel_ticket_type_map.saved` / `integration.channel_ticket_type_map.save_failed` | web | operation 级补充 |
| INT-第三方集成 | `testProviderAdapterMock` | `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | mock adapter 契约测试 | `integration.provider_adapter.test` | `provider_adapter.tested` / `provider_adapter.test_failed` | web | operation 级补充 |
| INT-第三方集成 | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | 更新供应商适配器配置 | `integration.provider_adapter.update` | `open_mapping.changed` / `provider_adapter.update_failed` | web | operation 级补充 |
| INV-库存存酒 | `advanceSavedBottleRemedyByPlatform` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/advance` | 门店退出交平台后、原门店承担的补存/赔付由娱乐平台公司审批先垫付，对客结案后向原门店挂追收应收 | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_advanced` / `bar.saved_bottle.remedy_advance_failed` | web | operation 级补充 |
| INV-库存存酒 | `allocatePurchasePrepayment` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-allocations` | 用原真实预付款冲抵本采购当前应付 | `finance.ar_ap.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | 提交采购审批申请 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `approveSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/approve` | 赔付方案审批：role=supervisor 核 inventory.high_value.review（门店主管），role=finance 核 finance.ar_ap.manage（财务）；审批人不得是登记人；两方齐备才可支付结案 | `inventory.high_value.review` | `bar.saved_bottle.remedy_approved` / `bar.saved_bottle.remedy_approve_failed` | web | operation 级补充 |
| INV-库存存酒 | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | 提交供应合同主线及业务财务法务并行审批 | `inventory.supply_contract.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | 采购单到货(ordered/expected→arrived) | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `assignSavedBottleRemedyStaffLiability` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/staff-liability` | 复核认定责任员工照价赔偿登记（金额由服务端按内部结算价×数量计，不收前端金额；不改对客状态） | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_staff_liability_assigned` / `bar.saved_bottle.remedy_staff_liability_assign_failed` | web | operation 级补充 |
| INV-库存存酒 | `backfillTraceHandoverSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/segments/{segment}/backfill` | 交接缺段人工补录（附证据、进主管复核，通过后才生效） | `inventory.handover.scan` | `evidence.manual_backfill_submitted` / `evidence.manual_backfill_submit_failed` | staff_work_phone, web | operation 级补充 |
| INV-库存存酒 | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | 存酒作废(高价值→照片+视频双证据建复核;非高价值直接作废) | `bar.saved_bottle.manage` | `bar.saved_bottle.cancelled` / `bar.saved_bottle.cancel_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `cancelTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/cancel` | 取消调拨／要货单 | `inventory.transfer_order.manage` | `inventory.transfer_order_cancelled` / `inventory.transfer_order_cancel_failed` | web | operation 级补充 |
| INV-库存存酒 | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | 存酒领取 | `bar.saved_bottle.manage` | `bar.saved_bottle_picked_up` / `bar.saved_bottle.pickup_failed` | staff_work_phone, web | operation 级补充 |
| INV-库存存酒 | `completeQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/dispositions/{id}/complete` | 登记批准的实际隔离处置 | `inventory.stock.adjust` | `inventory.quarantine.disposition_completed` / `inventory.quarantine.disposition_complete_failed` | web | operation 级补充 |
| INV-库存存酒 | `completeTransferDisposal` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/disposal-completed` | 借出方登记实物处置完成 | `inventory.transfer_order.manage` | `inventory.transfer_disposal_completed` / `inventory.transfer_disposal_complete_failed` | web | operation 级补充 |
| INV-库存存酒 | `configureSavedBottleManualPickupPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/manual-pickup-policy` | 配置人工例外取酒消费历史条件（按项目；最高权限本人办理并留痕；须同时配置上下限） | `bar.saved_bottle.manual_pickup_policy.manage` | `bar.saved_bottle.manual_pickup_policy_configured` / `bar.saved_bottle.manual_pickup_policy_config_failed` | web | operation 级补充 |
| INV-库存存酒 | `configureSavedBottleReminderPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/reminder-policy` | 配置存酒到期提醒节点（按项目；出厂默认到期前第 14、7、3、1 天） | `bar.saved_bottle.reminder_policy.manage` | `bar.saved_bottle.reminder_policy_configured` / `bar.saved_bottle.reminder_policy_config_failed` | web | operation 级补充 |
| INV-库存存酒 | `configureSavedBottleRenewalPolicy` | `PUT` | `/api/v1/inventory/saved-bottles/renewal-policy` | 配置存酒转存次数上限（按项目）与顺延期限（按酒吧）（须同时配置上下限；每次保存 version+1 并留痕） | `bar.saved_bottle.renewal_policy.manage` | `bar.saved_bottle.renewal_policy_configured` / `bar.saved_bottle.renewal_policy_config_failed` | web | operation 级补充 |
| INV-库存存酒 | `confirmPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-consignment-sales/{id}/confirm` | 由不同人员独立核实寄售售出事实 | `inventory.purchase.approve` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `confirmSavedBottleManualPickupMarketing` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/confirm-marketing` | 人工例外取酒③营销确认（归属营销本人；离职/无归属由营销主管） | `bar.saved_bottle.manual_pickup.confirm_marketing` | `bar.saved_bottle.manual_pickup_marketing_confirmed` / `bar.saved_bottle.manual_pickup_marketing_confirm_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `confirmTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/confirm` | 对方确认借用物无法归还并提交主管复核 | `inventory.transfer_order.manage` | `inventory.transfer_unreturnable_confirmed` / `inventory.transfer_unreturnable_confirm_failed` | web | operation 级补充 |
| INV-库存存酒 | `createDamageStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/damage` | 报损（damage）转库存调整 | `inventory.stock.move` | `inventory.counted` / `inventory.adjustment_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventoryItem` | `POST` | `/api/v1/inventory/items` | 创建库存物品 | `inventory.stock.move` | `inventory.item.created` / `inventory.item.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventoryStockBatch` | `POST` | `/api/v1/inventory/stock-batches` | 创建库存批次 | `inventory.stock.batch.manage` | `inventory.stock_batch.create` / `inventory.stock_batch.create_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createInventoryStocktake` | `POST` | `/api/v1/inventory/stocktakes` | 建库存盘点单 | `inventory.stocktake.create` | `inventory.stocktake.created` / `inventory.stocktake.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | 创建供应商 | `inventory.supplier.manage` | `inventory.supplier_changed` / `inventory.supplier_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createLossStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/loss` | 报损失（loss）转库存调整 | `inventory.stock.move` | `inventory.counted` / `inventory.adjustment_failed` | web | operation 级补充 |
| INV-库存存酒 | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | 创建采购申请 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | web | operation 级补充 |
| INV-库存存酒 | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | 存酒登记 | `bar.saved_bottle.manage` | `bar.saved_bottle.created` / `bar.saved_bottle.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | 盘点差异、报损、报溢 | `inventory.stock.adjust` | `inventory.counted` / `inventory.adjustment_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockIn` | `POST` | `/api/v1/inventory/stock-in` | 入库 | `inventory.stock.move` | `inventory.inbound_confirmed` / `inventory.inbound_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | 库存移动或扣减 | `inventory.stock.move` | `inventory.outbound_verified` / `inventory.stock_movement_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createSupplierQualification` | `POST` | `/api/v1/inventory/supplier-qualifications` | 创建/更新供应商资质 | `inventory.supplier.qualification.manage` | `inventory.supplier_qualification.create` / `inventory.supplier_qualification.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplierReconciliation` | `POST` | `/api/v1/inventory/supplier-reconciliations` | 创建供应商对账单 | `inventory.supplier.reconcile` | `inventory.supplier_reconciliation.create` / `inventory.supplier_reconciliation.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplierSettlementMethod` | `POST` | `/api/v1/inventory/supplier-settlement-methods` | 创建/更新供应商结算档 | `inventory.supplier.settlement.manage` | `inventory.supplier_settlement_method.create` / `inventory.supplier_settlement_method.create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | 创建供应合同(三模式) | `inventory.supply_contract.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createSupplyEntity` | `POST` | `/api/v1/inventory/supply-entities` | 新建供货主体 | `inventory.supply_entity.manage` | `inventory.supply_entity_changed` / `inventory.supply_entity_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createTraceHandover` | `POST` | `/api/v1/inventory/trace-handovers` | 创建扫码交接任务(五段链) | `inventory.handover.scan` | `inventory.handover_created` / `inventory.handover_create_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders` | 新建跨主体流转单 | `inventory.transfer_order.manage` | `inventory.transfer_order_created` / `inventory.transfer_order_create_failed` | web | operation 级补充 |
| INV-库存存酒 | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | 创建typed库存流水 | `inventory.stock.move` | `inventory.stock_movement.created` / `inventory.stock_movement.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | 创建结构化库房(六类) | `inventory.warehouse.manage` | `inventory.warehouse.created` / `inventory.warehouse.create_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createWarehouseLocation` | `POST` | `/api/v1/inventory/warehouse-locations` | 在库房下建库位 | `inventory.warehouse.manage` | `inventory.warehouse_location.created` / `inventory.warehouse_location.create_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createWasteStockAdjustment` | `POST` | `/api/v1/inventory/stock-movements/waste` | 报废（waste）转库存调整 | `inventory.stock.move` | `inventory.counted` / `inventory.adjustment_failed` | web | operation 级补充 |
| INV-库存存酒 | `deliverTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/deliver` | 门店要货送达待签收 | `inventory.transfer_order.manage` | `inventory.transfer_order_delivered` / `inventory.transfer_order_deliver_failed` | web | operation 级补充 |
| INV-库存存酒 | `dispenseSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/dispense` | 人工例外取酒④本人到场放酒（取酒流水 verification_method=manual_exception） | `bar.saved_bottle.manage` | `bar.saved_bottle_picked_up` / `bar.saved_bottle.pickup_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `disputeSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/dispute` | 受理存酒争议 | `bar.saved_bottle.manage` | `bar.saved_bottle.dispute` / `bar.saved_bottle.dispute_failed` | web | operation 级补充 |
| INV-库存存酒 | `editTransferOrderDraft` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/lines` | 门店要货草稿改单 | `inventory.transfer_order.manage` | `inventory.transfer_order_draft_edited` / `inventory.transfer_order_draft_edit_failed` | web | operation 级补充 |
| INV-库存存酒 | `getInventoryCostSummary` | `GET` | `/api/v1/inventory/cost-summary` | 期间出库成本汇总（分红成本侧取数点） | `inventory.cost.view` | `inventory.cost_summary.viewed` / `inventory.cost_summary.view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `getInventoryStocktake` | `GET` | `/api/v1/inventory/stocktakes/{id}` | 查询库存盘点单详情（含差异明细） | `inventory.stocktake.view` | `inventory.stocktake.detail_viewed` / `inventory.stocktake.detail_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `getInventoryTraceCode` | `GET` | `/api/v1/inventory/trace-codes/{id}` | 查询溯源码责任链（按 id 或 trace_code） | `inventory.trace_code.view` | `inventory.trace_code.viewed` / `inventory.trace_code.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `getPurchaseSuggestions` | `GET` | `/api/v1/inventory/purchase-suggestions` | 读取智能采购建议（建议量＝安全库存＋近 7 天销量－当前可用－在途，大于 0 才列出） | `inventory.purchase_request.manage` | `inventory.purchase_suggestion.viewed` / `inventory.purchase_suggestion.view_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `getPurchaseWorkbenchOptions` | `GET` | `/api/v1/inventory/purchase-workbench-options` | 读取采购工作台可用目录（逐项目及货权过滤） | `inventory.purchase_request.view` | `inventory.purchase_workbench.viewed` / `inventory.purchase_workbench.view_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `getPurchaseWorkflowDetail` | `GET` | `/api/v1/inventory/purchase-requests/{id}` | 读取采购数量、逐次验收及独立结算事实 | `inventory.purchase_request.view` | `inventory.purchase_request_viewed` / `inventory.purchase_request_view_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `getSupplyContractTerms` | `GET` | `/api/v1/inventory/supply-contracts/{id}/terms` | 读取供应合同条款及审批状态 | `inventory.supply_contract.manage` | `inventory.supply_contract.viewed` / `inventory.supply_contract.view_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `getTraceHandover` | `GET` | `/api/v1/inventory/trace-handovers/{id}` | 查交接链（原扫码五段、异常与人工补录并列） | `inventory.handover.scan` | `not_applicable` / `not_applicable` | staff_work_phone, web | operation 级补充 |
| INV-库存存酒 | `getTransferOrder` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}` | 调拨与借调单详情 | `inventory.transfer_order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| INV-库存存酒 | `getTransferOrderFulfillment` | `GET` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | 门店要货签收结果与差异处理 | `inventory.transfer_order.view` | `inventory.transfer_fulfillment_viewed` / `inventory.transfer_fulfillment_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `handoverPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-returns/{id}/handover` | 登记批准退供的实际交接并出库 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `handoverSavedBottlesToPlatform` | `POST` | `/api/v1/inventory/saved-bottles/handover` | 门店退出存酒交所属娱乐平台公司（平台操作者办理；该门店有效期内未结存酒须全部清点；指定领取地点、原有效期与原取酒规则不变；毁损与实收为 0 的短少走作废路径，部分短少按差额） | `bar.saved_bottle.manage` | `bar.saved_bottle.handed_over` / `bar.saved_bottle.handover_failed` | web | operation 级补充 |
| INV-库存存酒 | `initiateSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/{id}/manual-pickups` | 人工例外取酒①店长发起（消费历史条件按项目配置，走客人查询专门通道留痕） | `bar.saved_bottle.manual_pickup.initiate` | `bar.saved_bottle.manual_pickup_initiated` / `bar.saved_bottle.manual_pickup_initiate_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listBarConsumptionStats` | `GET` | `/api/v1/inventory/bar-consumption-stats` | 期间吧台消耗统计（按杯、按瓶、套餐） | `inventory.stock.view` | `inventory.bar_consumption_stats.viewed` / `inventory.bar_consumption_stats.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listHighValueReviews` | `GET` | `/api/v1/inventory/high-value-reviews` | 查询库存域高价值复核单(报损/盘点差异/存酒作废) | `inventory.high_value.review` | `inventory.high_value_review.viewed` / `inventory.high_value_review.view_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryCostLedger` | `GET` | `/api/v1/inventory/cost-ledger` | 查询库存成本流水（移动加权） | `inventory.cost.view` | `inventory.cost_ledger.viewed` / `inventory.cost_ledger.view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | 查询库存明细 | `inventory.stock.view` | `inventory.item.viewed` / `inventory.item.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventoryQuarantine` | `GET` | `/api/v1/inventory/quarantine` | 查看仓库隔离库存 | `inventory.stock.view` | `inventory.quarantine.viewed` / `inventory.quarantine.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventoryRiskAlerts` | `GET` | `/api/v1/inventory/risk-alerts` | 查询库存风险预警列表 | `inventory.risk_alert.view` | `inventory.risk_alert.list` / `inventory.risk_alert.list_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryStockBatches` | `GET` | `/api/v1/inventory/stock-batches` | 查询库存批次列表 | `inventory.stock.view` | `inventory.stock_batch.list` / `inventory.stock_batch.list_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryStocktakes` | `GET` | `/api/v1/inventory/stocktakes` | 查询库存盘点单列表 | `inventory.stocktake.view` | `inventory.stocktake.viewed` / `inventory.stocktake.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | 查询供应商 | `inventory.supplier.view` | `inventory.supplier_viewed` / `inventory.supplier_view_failed` | web, kiosk, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listInventoryTraceCodes` | `GET` | `/api/v1/inventory/trace-codes` | 查询溯源码主数据列表 | `inventory.trace_code.view` | `inventory.trace_code.listed` / `inventory.trace_code.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | 查询采购申请 | `inventory.purchase_request.view` | `inventory.purchase_request_viewed` / `inventory.purchase_request_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listQuarantineDispositions` | `GET` | `/api/v1/inventory/quarantine/{id}/dispositions` | 查看隔离后续处置记录 | `inventory.stock.view` | `inventory.quarantine.dispositions_viewed` / `inventory.quarantine.dispositions_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listQuarantineWarehouses` | `GET` | `/api/v1/inventory/quarantine/warehouses` | 选择可查看的隔离库存仓库 | `inventory.stock.view` | `inventory.quarantine.warehouses_viewed` / `inventory.quarantine.warehouses_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottleHandovers` | `GET` | `/api/v1/inventory/saved-bottles/handovers` | 存酒交接单列表（商户 scope 只看本店） | `bar.saved_bottle.manage` | `bar.saved_bottle.handover_viewed` / `bar.saved_bottle.handover_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottleRemedies` | `GET` | `/api/v1/inventory/saved-bottles/{id}/remedies` | 该存酒的补存/赔付记录（对客交付状态与内部追收状态分别记录） | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_viewed` / `bar.saved_bottle.remedy_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottleReminderPolicies` | `GET` | `/api/v1/inventory/saved-bottles/reminder-policy` | 读存酒到期提醒节点（出厂行与各项目行；出厂默认到期前第 14、7、3、1 天） | `bar.saved_bottle.reminder_policy.manage` | `bar.saved_bottle.reminder_policy_viewed` / `bar.saved_bottle.reminder_policy_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottleRenewals` | `GET` | `/api/v1/inventory/saved-bottles/{id}/renewals` | 存酒转存记录（含待复核；固化配置版本、顺延天数、新到期日、累计转存次数、经办人与客人确认方式） | `bar.saved_bottle.manage` | `bar.saved_bottle.renewal_viewed` / `bar.saved_bottle.renewal_view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | 查询存酒 | `bar.saved_bottle.manage` | `bar.saved_bottle.viewed` / `bar.saved_bottle.view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | 查询库存余额 | `inventory.stock.view` | `inventory.stock_balance.viewed` / `inventory.stock_balance.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | 查询库存流水 | `inventory.stock.view` | `inventory.stock_movement.viewed` / `inventory.stock_movement.view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listSupplierQualifications` | `GET` | `/api/v1/inventory/supplier-qualifications` | 查询供应商资质列表 | `inventory.supplier.qualification.manage` | `inventory.supplier_qualification.list` / `inventory.supplier_qualification.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplierReconciliations` | `GET` | `/api/v1/inventory/supplier-reconciliations` | 查询供应商对账列表 | `inventory.supplier.reconcile` | `inventory.supplier_reconciliation.list` / `inventory.supplier_reconciliation.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplierSettlementMethods` | `GET` | `/api/v1/inventory/supplier-settlement-methods` | 查询供应商结算档列表 | `inventory.supplier.settlement.manage` | `inventory.supplier_settlement_method.list` / `inventory.supplier_settlement_method.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplyContracts` | `GET` | `/api/v1/inventory/supply-contracts` | 查询供应合同列表 | `inventory.supply_contract.manage` | `inventory.supply_contract.viewed` / `inventory.supply_contract.view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSupplyEntities` | `GET` | `/api/v1/inventory/supply-entities` | 供货主体列表 | `inventory.supply_entity.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| INV-库存存酒 | `listTraceCodeOutboundOrders` | `GET` | `/api/v1/inventory/trace-codes/{id}/outbound-orders` | 库管出库可选的酒吧订单（按溯源码） | `inventory.handover.scan` | `not_applicable` / `not_applicable` | staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listTraceHandovers` | `GET` | `/api/v1/inventory/trace-handovers` | 按溯源码查交接单 | `inventory.handover.scan` | `not_applicable` / `not_applicable` | staff_work_phone, web | operation 级补充 |
| INV-库存存酒 | `listTransferOrders` | `GET` | `/api/v1/inventory/transfer-orders` | 跨主体调拨与借调单列表 | `inventory.transfer_order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| INV-库存存酒 | `listWarehouseLocations` | `GET` | `/api/v1/inventory/warehouse-locations` | 查询库位列表 | `inventory.warehouse.manage` | `inventory.warehouse_location.list` / `inventory.warehouse_location.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `listWarehouses` | `GET` | `/api/v1/inventory/warehouses` | 查询库房列表 | `inventory.warehouse.manage` | `inventory.warehouse.list` / `inventory.warehouse.list_failed` | web | operation 级补充 |
| INV-库存存酒 | `lookupSavedBottlesByPhone` | `POST` | `/api/v1/inventory/saved-bottles/lookup` | 按手机号检索存酒（检索只能输入手机号，姓名与昵称不得用于检索；手机号走请求体不进 URL） | `bar.saved_bottle.manage` | `bar.saved_bottle.viewed` / `bar.saved_bottle.view_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `markInventoryItemHighValue` | `POST` | `/api/v1/inventory/items/{id}/high-value-mark` | 物料手工标记或取消标记高价值 | `inventory.high_value.review` | `inventory.item.high_value_marked` / `inventory.item.high_value_mark_failed` | web | operation 级补充 |
| INV-库存存酒 | `noticeSavedBottleExit` | `POST` | `/api/v1/inventory/saved-bottles/exit-notice` | 门店退出前清点有效期内未结存酒并逐瓶短信通知客人在原店取回 | `bar.saved_bottle.manage` | `bar.saved_bottle.exit_noticed` / `bar.saved_bottle.exit_notice_failed` | web | operation 级补充 |
| INV-库存存酒 | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | 采购单下单(approved→ordered) | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `pickTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/pick` | 门店要货拣配完成 | `inventory.transfer_order.manage` | `inventory.transfer_order_picked` / `inventory.transfer_order_pick_failed` | web | operation 级补充 |
| INV-库存存酒 | `postPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-prepayment-resolutions/{id}/post` | 完整批准后由财务过账预付余额处理 | `finance.ar_ap.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `printInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes/{id}/print` | 出库打印溯源码（registered→printed） | `inventory.trace_code.print` | `inventory.trace_code.printed` / `inventory.trace_code.print_failed` | web | operation 级补充 |
| INV-库存存酒 | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | 逐次登记实到、合格接收及拒收 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `receiveTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/receive` | 调拨收货／门店要货签收 | `inventory.transfer_order.manage` | `inventory.transfer_order_received` / `inventory.transfer_order_receive_failed` | web | operation 级补充 |
| INV-库存存酒 | `recordInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances` | 录入库存盘点差异行 | `inventory.stocktake.variance_record` | `inventory.stocktake.variance_recorded` / `inventory.stocktake.variance_record_failed` | web | operation 级补充 |
| INV-库存存酒 | `registerInventoryTraceCode` | `POST` | `/api/v1/inventory/trace-codes` | 贴码登记溯源码（入库） | `inventory.trace_code.register` | `inventory.trace_code.registered` / `inventory.trace_code.register_failed` | web | operation 级补充 |
| INV-库存存酒 | `registerPurchaseConsignmentSale` | `POST` | `/api/v1/inventory/purchase-requests/{id}/consignment-sales` | 提交合同指定的寄售售出事实 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `registerSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/{id}/remedy` | 存酒作废后对客补存/赔付登记（replacement=同品同量补存、到期日不变、库存同量出库、补酒成本按内部结算价计经营损失、登记即结案；compensation=赔付待门店主管+财务审批） | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_registered` / `bar.saved_bottle.remedy_register_failed` | web | operation 级补充 |
| INV-库存存酒 | `registerTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions` | 登记门店要货差异处理 | `inventory.transfer_order.manage` | `inventory.transfer_disposition_registered` / `inventory.transfer_disposition_register_failed` | web | operation 级补充 |
| INV-库存存酒 | `registerTransferUnreturnable` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/unreturnable` | 登记借用物无法归还 | `inventory.transfer_order.manage` | `inventory.transfer_unreturnable_registered` / `inventory.transfer_unreturnable_register_failed` | web | operation 级补充 |
| INV-库存存酒 | `renewSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/renewals` | 员工代办存酒转存（延期）：客人本人短信验证码确认后待当班营销主管复核（ENUM L120 / BSM L1072） | `bar.saved_bottle.manage` | `bar.saved_bottle.renewal_requested` / `bar.saved_bottle.renewal_request_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `reportRobotSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/robot-report` | 机器人配送段上报(被动留痕,受第三方开关门控) | `inventory.handover.scan` | `inventory.handover_robot_reported` / `inventory.handover_robot_report_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `requestPurchasePrepaymentResolution` | `POST` | `/api/v1/inventory/purchase-requests/{id}/prepayment-resolutions` | 按合同申请退未用预付款或减未付预付义务 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `requestPurchaseReturn` | `POST` | `/api/v1/inventory/purchase-requests/{id}/returns` | 发起退供应商主线及财务审批 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `requestQuarantineDisposition` | `POST` | `/api/v1/inventory/quarantine/{id}/dispositions` | 申请隔离品复售或报损 | `inventory.stock.adjust` | `inventory.quarantine.disposition_requested` / `inventory.quarantine.disposition_request_failed` | web | operation 级补充 |
| INV-库存存酒 | `requestSavedBottleVerificationCode` | `POST` | `/api/v1/inventory/saved-bottles/{id}/verification-code` | 向当前持有人手机发送存酒短信验证码（scene=transfer\|claim\|renew） | `bar.saved_bottle.manage` | `bar.saved_bottle.verification_code_requested` / `bar.saved_bottle.verification_code_request_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `resolvePurchaseQuantity` | `POST` | `/api/v1/inventory/purchase-requests/{id}/resolutions` | 登记补货安排或发起余量终止审批 | `inventory.purchase_request.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `returnTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/return` | 借用归还（借出方收货扫码） | `inventory.transfer_order.manage` | `inventory.transfer_order_returned` / `inventory.transfer_order_return_failed` | web | operation 级补充 |
| INV-库存存酒 | `reviewHighValue` | `POST` | `/api/v1/inventory/high-value-reviews/{id}/review` | 复核高价值动作(approve生效/reject回滚)；存酒库房存货处置须门店主管与财务两道审批 | `inventory.high_value.review` | `inventory.high_value_review_decided` / `inventory.high_value_review_decide_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewInventoryStocktakeVariance` | `POST` | `/api/v1/inventory/stocktakes/{id}/variances/{vid}/review` | 复核库存盘点差异（approved/rejected） | `inventory.stocktake.variance_review` | `inventory.stocktake.variance_reviewed` / `inventory.stocktake.variance_review_failed` | web | operation 级补充 |
| INV-库存存酒 | `reviewPurchaseReturnFinance` | `POST` | `/api/v1/inventory/purchase-returns/{id}/finance-review` | 复核退供成本差额并冲应付或登记待回款 | `finance.ar_ap.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewSavedBottleManualPickup` | `POST` | `/api/v1/inventory/saved-bottles/manual-pickups/{id}/review` | 人工例外取酒②另一名主管复核（复核人不得是发起人） | `bar.saved_bottle.manual_pickup.review` | `bar.saved_bottle.manual_pickup_reviewed` / `bar.saved_bottle.manual_pickup_review_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `reviewSavedBottleRenewal` | `POST` | `/api/v1/inventory/saved-bottles/renewals/{id}/review` | 当班营销主管复核员工代办转存（复核人不得是经办人；decision=approve 从当前到期日顺延，decision=reject 驳回：到期日不动、不计转存次数、须带 reason） | `bar.saved_bottle.renewal.review` | `bar.saved_bottle.renewal_reviewed` / `bar.saved_bottle.renewal_review_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `reviewTraceHandoverException` | `POST` | `/api/v1/inventory/trace-handovers/exceptions/{id}/review` | 交接异常复核(缺口/重复/差异) | `inventory.handover.review` | `inventory.handover_exception_resolved` / `inventory.handover_exception_review_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewTransferDisposition` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/dispositions/{disposition_id}/review` | 财务复核门店要货差异处理 | `finance.ar_ap.manage` | `inventory.transfer_disposition_reviewed` / `inventory.transfer_disposition_review_failed` | web | operation 级补充 |
| INV-库存存酒 | `saveSupplyContractTerms` | `POST` | `/api/v1/inventory/supply-contracts/{id}/terms` | 登记待审批供应合同的明确执行条款 | `inventory.supply_contract.manage` | `inventory.purchase.completed` / `inventory.purchase.failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `scanInventoryRiskAlerts` | `POST` | `/api/v1/inventory/risk-alerts/scan` | 触发库存风险扫描 | `inventory.risk.scan` | `inventory.risk.scan` / `inventory.risk.scan_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | 物品追踪码扫码交接 | `inventory.handover.scan` | `operation_handover.created` / `operation_handover.failed` | staff_work_phone | operation 级补充 |
| INV-库存存酒 | `scanTraceSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/scan-segment` | 员工扫码交接分段(主动留痕) | `inventory.handover.scan` | `inventory.handover_segment_scanned` / `inventory.handover_segment_scan_failed` | staff_work_phone, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `settleSavedBottleRemedy` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/settle` | 赔款实际交付后结案（记交付/支付凭证与时间，赔付金额此时记经营损失；不读任何内部追收字段） | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_settled` / `bar.saved_bottle.remedy_settle_failed` | web | operation 级补充 |
| INV-库存存酒 | `settleSavedBottleRemedyRecovery` | `POST` | `/api/v1/inventory/saved-bottles/remedies/{id}/recovery/settle` | 员工赔偿收回登记（向原门店的追收经应收台账结清自动回写） | `bar.saved_bottle.manage` | `bar.saved_bottle.remedy_recovery_settled` / `bar.saved_bottle.remedy_recovery_settle_failed` | web | operation 级补充 |
| INV-库存存酒 | `setWarehousePourDevice` | `PUT` | `/api/v1/inventory/warehouses/{id}/pour-device` | 登记吧台是否配有出酒量采集设备 | `inventory.warehouse.manage` | `inventory.warehouse.pour_device_updated` / `inventory.warehouse.pour_device_update_failed` | web | operation 级补充 |
| INV-库存存酒 | `shipTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/ship` | 调拨发货／借用交货扫码 | `inventory.transfer_order.manage` | `inventory.transfer_order_shipped` / `inventory.transfer_order_ship_failed` | web | operation 级补充 |
| INV-库存存酒 | `submitTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/submit` | 门店要货提交供货主体审核 | `inventory.transfer_order.manage` | `inventory.transfer_order_submitted` / `inventory.transfer_order_submit_failed` | web | operation 级补充 |
| INV-库存存酒 | `supplierReviewTransferOrder` | `POST` | `/api/v1/inventory/transfer-orders/{order_id}/supplier-review` | 供货主体审核门店要货 | `inventory.transfer_order.manage` | `inventory.transfer_order_supplier_reviewed` / `inventory.transfer_order_supplier_review_failed` | web | operation 级补充 |
| INV-库存存酒 | `transferSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/transfer` | 存酒转让（出让人短信验证码确认，验证成功即换持有人、整瓶转让、原持有人动态取酒码作废） | `bar.saved_bottle.manage` | `bar.saved_bottle_transferred` / `bar.saved_bottle.transfer_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | 调拨库存流水 | `inventory.stock.move` | `inventory.stock_transfer.created` / `inventory.stock_transfer.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `updateInventoryItemSafetyStock` | `POST` | `/api/v1/inventory/items/{id}/safety-stock` | 修改已有物料的安全库存 | `inventory.stock.move` | `inventory.item.safety_stock_updated` / `inventory.item.safety_stock_update_failed` | web | operation 级补充 |
| OPEN-开放平台 | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | 回调处理结果查询 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `getOpenParkingConfig` | `GET` | `/api/open/d/{endpoint_key}/parking/config` | 停车系统拉取平台下发配置 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `listOpenParkingPayments` | `GET` | `/api/open/d/{endpoint_key}/parking/payments` | 停车系统拉取平台已收停车费 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | 第三方或供应商回调 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPS-生产前置 | `approveDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/approve` | 审批并执行数据清理 | `data.cleanup.approve` | `data.cleanup.executed` / `data.cleanup.execute_failed` | web | operation 级补充 |
| OPS-生产前置 | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | 执行生产前置准入检查 | `ops.production_readiness.check` | `production_readiness.checked` / `production_readiness.check_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `listDataCleanupJobs` | `GET` | `/api/v1/ops/data-cleanup/jobs` | 查询数据清理作业 | `data.cleanup.request` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| OPS-生产前置 | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | 查询生产前置责任状态 | `ops.production_readiness.view` | `production_readiness.viewed` / `production_readiness.view_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `previewDataCleanup` | `POST` | `/api/v1/ops/data-cleanup/preview` | 数据清理试运行 | `data.cleanup.request` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| OPS-生产前置 | `submitDataCleanupJob` | `POST` | `/api/v1/ops/data-cleanup/jobs/{id}/submit` | 提交数据清理申请 | `data.cleanup.request` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| OPS-生产前置 | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | 更新生产前置责任状态 | `ops.production_readiness.manage` | `production_readiness.updated` / `production_readiness.update_failed` | web, mobile_admin | 已在任务映射正文出现 |
| P0-待归类 | `acceptFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/accept` | 主管验收工单 | `facility.work_order.manage` | `facility.work_order_accepted` / `facility.work_order_accept_failed` | web | operation 级补充 |
| P0-待归类 | `acceptServiceOrder` | `POST` | `/api/v1/services/orders/{id}/accept` | 接单(仅指定履约人) | `service.order.perform` | `service.order.accepted` / `service.order.accept_failed` | web, staff_work_phone | operation 级补充 |
| P0-待归类 | `acceptSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/accept` | 物业验收完工（平台侧） | `facility.site_entry.accept` | `facility.site_entry_report_accepted` / `facility.site_entry.accept_failed` | web | operation 级补充 |
| P0-待归类 | `activateContract` | `POST` | `/api/v1/contracts/{contract_id}/activate` | 生效(固化快照+绑铺位) | `contract.lifecycle.activate` | `contract.activated` / `contract.activate_failed` | web | operation 级补充 |
| P0-待归类 | `activateServiceCatalogFeeDraft` | `POST` | `/api/v1/services/catalog/{id}/fee-activate` | 复核转正服务目录项管理费草案(差人) | `service.catalog.approve` | `service.catalog.fee_activated` / `service.catalog.fee_activate_failed` | web | operation 级补充 |
| P0-待归类 | `addContractAttachments` | `POST` | `/api/v1/contracts/{contract_id}/attachments` | 追加合同附件(电子签章扫描件归档) | `contract.draft.manage` | `contract.attachments_added` / `contract.attachments_add_failed` | web | operation 级补充 |
| P0-待归类 | `addMerchantServiceWorkOrderAction` | `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | Append merchant service work order action | `merchant.service_work_order.process` | `merchant_service.addMerchantServiceWorkOrderAction.succeeded` / `merchant_service.addMerchantServiceWorkOrderAction.failed` | web, mobile_admin, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `adjustMediaPerformanceSettlement` | `POST` | `/api/v1/settlement/media-shows/{id}/settlements/{settlementId}/adjust` | 调整已出账结算 | `media.performance.settle` | `media_settlement.adjusted` / `media_settlement.adjust_failed` | web | operation 级补充 |
| P0-待归类 | `advanceLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/advance` | 意向阶段推进(5 阶段) | `leasing.intent.manage` | `leasing.intent.advanced` / `leasing.intent.advance_failed` | web | operation 级补充 |
| P0-待归类 | `approveDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-approve` | 审批设施设备删除申请 | `facility.equipment.approve` | `facility.equipment_delete_approved` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `approveDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-approve` | 审批商户服务工单删除申请 | `merchant.service_work_order.approve` | `merchant_service.work_order.delete_approved` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `approveDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-approve` | 审批服务目录项删除申请 | `service.catalog.approve` | `service.catalog.delete_approved` / `service.catalog.delete_approve_failed` | web | operation 级补充 |
| P0-待归类 | `approveMerchantPortalPayeeAccountChange` | `POST` | `/api/v1/merchant-portal/ticket-settlements/{ar_ap_item_id}/account-change` | 收款方财务负责人审批更换收款账户 | `merchant.portal.payee_account.approve` | `merchant_portal.payee_account.approved` / `merchant_portal.payee_account.approve_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `archiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/archive` | 存档设施设备 | `facility.equipment.manage` | `facility.equipment_archived` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `archiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/archive` | 存档商户服务工单 | `merchant.service_work_order.close` | `merchant_service.work_order.archived` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `archiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/archive` | 存档服务目录项 | `service.catalog.manage` | `service.catalog.archived` / `service.catalog.archive_failed` | web | operation 级补充 |
| P0-待归类 | `assignEnergyVerifier` | `POST` | `/api/v1/energy/readings/{reading_id}/assignment` | 已配置主管指派真实复抄人员 | `energy.reading.approve` | `energy.verifier_assigned` / `energy.verifier_assigned_failed` | web | operation 级补充 |
| P0-待归类 | `assignFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties` | 安排接警值守（挂已发布班次） | `facility.alarm.manage` | `facility.alarm_duty_assigned` / `facility.alarm_duty_assign_failed` | web | operation 级补充 |
| P0-待归类 | `assignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/assign` | 工单派单 | `facility.work_order.manage` | `facility.work_order_assigned` / `facility.work_order_assign_failed` | web | operation 级补充 |
| P0-待归类 | `assignLeasingLead` | `POST` | `/api/v1/leasing/leads/{lead_id}/assign` | 招商线索派单(主管手动指派) | `leasing.lead.manage` | `leasing.lead.assigned` / `leasing.lead.assign_failed` | web | operation 级补充 |
| P0-待归类 | `assignMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | Assign merchant service work order | `merchant.service_work_order.assign` | `merchant_service.assignMerchantServiceWorkOrder.succeeded` / `merchant_service.assignMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `bindUserToPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/bind-user` | 账号绑定外部协作机构 | `partner.org.manage` | `partner.org.user_bound` / `partner.org.user_bind_failed` | web | operation 级补充 |
| P0-待归类 | `blacklistLeasingBrand` | `POST` | `/api/v1/leasing/brands/{brand_id}/blacklist` | 品牌拉黑/解黑(高危需审批) | `leasing.brand.blacklist` | `leasing.brand.blacklist_changed` / `leasing.brand.blacklist_change_failed` | web | operation 级补充 |
| P0-待归类 | `cancelFacilityAlarmDuty` | `POST` | `/api/v1/facility/alarm-duties/{duty_id}/cancel` | 撤销接警值守安排 | `facility.alarm.manage` | `facility.alarm_duty_cancelled` / `facility.alarm_duty_cancel_failed` | web | operation 级补充 |
| P0-待归类 | `cancelMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cancel` | 商户在派单前取消工单 | `merchant.service_work_order.create` | `merchant_service.work_order.cancelled` / `merchant_service.work_order.cancel_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `cancelMerchantServiceWorkOrderByStaff` | `POST` | `/api/v1/merchant-service/work-orders/{id}/staff-cancel` | 客户关怀部取消已派单的重复或误提工单 | `merchant.service_work_order.close` | `merchant_service.work_order.cancelled` / `merchant_service.work_order.cancel_failed` | web | operation 级补充 |
| P0-待归类 | `cancelServiceOrder` | `POST` | `/api/v1/services/orders/{id}/cancel` | 取消服务单(履约前;留原因) | `service.order.create` | `service.order.cancelled` / `service.order.cancel_failed` | web | operation 级补充 |
| P0-待归类 | `changeFacilityEquipmentStatus` | `POST` | `/api/v1/facility/equipment/{equipment_id}/status` | 变更设备状态/上报告警 | `facility.equipment.manage` | `facility.equipment_status_changed` / `facility.equipment_status_change_failed` | web | operation 级补充 |
| P0-待归类 | `changeSettlementInvoiceItemStatus` | `POST` | `/api/v1/settlement/invoice-items/{id}/status` | 流转开票状态 | `settlement.invoice.manage` | `settlement.invoice.status_changed` / `settlement.invoice.status_change_failed` | web | operation 级补充 |
| P0-待归类 | `checkContractSettlementDocuments` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/documents-check` | 退租结算齐单核对（缺单保持待处理） | `contract.settlement.manage` | `contract.settlement_documents_checked` / `contract.settlement_documents_check_failed` | web | operation 级补充 |
| P0-待归类 | `classifyComplaint` | `PUT` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | 客户关怀部认定投诉类别、更正涉及门店 | `facility.work_order.manage` | `complaint.classified` / `complaint.classify_failed` | web | operation 级补充 |
| P0-待归类 | `closeMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | Close merchant service work order | `merchant.service_work_order.close` | `merchant_service.closeMerchantServiceWorkOrder.succeeded` / `merchant_service.closeMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `completeDealerDeliveryPhysical` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/physical-completion` | 确认差异实物实际处理 | `dealer.order.ship` | `dealer.delivery.physical_completed` / `dealer.delivery.physical_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeFacilityEquipmentScrapDisposal` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/disposal` | 登记设备报废实际处置 | `facility.equipment.manage` | `facility.equipment_scrap_disposal_completed` / `facility.equipment_scrap_disposal_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/complete` | 完成工单处理 | `facility.work_order.manage` | `facility.work_order_completed` / `facility.work_order_complete_failed` | web | operation 级补充 |
| P0-待归类 | `completeServiceOrder` | `POST` | `/api/v1/services/orders/{id}/complete` | 完成履约(公司代收:管理费归公司,应得月结进薪资) | `service.order.create` | `service.order.completed` / `service.order.complete_failed` | staff_work_phone, web | operation 级补充 |
| P0-待归类 | `configureEnergyReviewSettings` | `POST` | `/api/v1/energy/projects/{project_id}/review-settings` | 申请项目核实期限及上下限配置 | `energy.meter.manage` | `energy.review_settings_submitted` / `energy.review_settings_submitted_failed` | web | operation 级补充 |
| P0-待归类 | `confirmComplaintLoop` | `POST` | `/api/v1/complaint-followups/{chain}/{work_order_id}/loop-confirm` | 客户关怀部确认客诉闭环 | `facility.work_order.manage` | `complaint.loop_confirmed` / `complaint.loop_confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmDealerOverpaymentUse` | `POST` | `/api/v1/dealers/overpayments/{id}/use` | 确认超收用途 | `dealer.order.payment.verify` | `dealer.order.overpayment_use_confirmed` / `dealer.order.overpayment_use_failed` | web | operation 级补充 |
| P0-待归类 | `confirmDealerReturnFreight` | `POST` | `/api/v1/dealers/returns/{id}/freight-confirmation` | 确认退回运费承担方 | `dealer.return.manage` | `dealer.return.freight_confirmed` / `dealer.return.freight_confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/confirm` | 确认设备故障处置规则 | `facility.fault_rule.confirm` | `facility.fault_rule_confirmed` / `facility.fault_rule_confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmFacilityInspectionSupervisor` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm` | 主管确认巡检任务关闭 | `facility.inspection.manage` | `facility.inspection_task_closed` / `facility.inspection_task_close_failed` | web | operation 级补充 |
| P0-待归类 | `confirmMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/confirm` | 确认实际履约 | `media.performance.fulfillment.confirm` | `media_fulfillment.confirmed` / `media_fulfillment.confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmMerchantBill` | `POST` | `/api/v1/merchant-billing/bills/{id}/confirm` | 确认商户账单 | `merchant.billing.confirm` | `merchant.billing.confirm` / `merchant.billing.confirm_failed` | web | operation 级补充 |
| P0-待归类 | `confirmMerchantServiceWorkOrderByMerchant` | `POST` | `/api/v1/merchant-service/work-orders/{id}/merchant-confirm` | 商户确认工单已解决并关闭 | `merchant.service_work_order.create` | `merchant_service.work_order.merchant_confirmed` / `merchant_service.work_order.merchant_confirm_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `confirmTicketPointsBillUnchanged` | `POST` | `/api/v1/loyalty/ticket-points-billing/objections/{id}/confirm-unchanged` | 申请复核月结原金额无误 | `ticket.points.billing.manage` | `ticket.points.billing.objection.resolution.submitted` / `ticket.points.billing.objection.resolution.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `countMediaPerformancePendingSettlements` | `GET` | `/api/v1/settlement/media-shows/pending-settlement-count` | 演出结算挂起待确认条数（总控驾驶舱结算待确认卡） | `media.performance.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
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
| P0-待归类 | `createFacilityEquipmentScrapRequest` | `POST` | `/api/v1/facility/equipment/{equipment_id}/scrap-requests` | 申请设备报废 | `facility.equipment.manage` | `facility.equipment_scrap_requested` / `facility.equipment_scrap_request_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules` | 登记设备故障处置规则 | `facility.alarm.manage` | `facility.fault_rule_created` / `facility.fault_rule_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks` | 创建巡检任务 | `facility.inspection.manage` | `facility.inspection_task_created` / `facility.inspection_task_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityInspectionTemplate` | `POST` | `/api/v1/facility/inspections/templates` | 创建巡检模板 | `facility.inspection.manage` | `facility.inspection_template_created` / `facility.inspection_template_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders` | 创建维保/物业工单 | `facility.work_order.manage` | `facility.work_order_created` / `facility.work_order_create_failed` | web | operation 级补充 |
| P0-待归类 | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | 创建 P1/P2 商场平面图 | `floor_plan.manage` | `floor_plan.created` / `floor_plan.create_failed` | web | operation 级补充 |
| P0-待归类 | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | 创建 SVG 或标准 JSON 导入任务 | `floor_plan.import.manage` | `floor_plan.import_created` / `floor_plan.import_create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingBlacklistException` | `POST` | `/api/v1/leasing/blacklist-exceptions` | 申请招商黑名单例外放行 | `leasing.brand.blacklist_exception` | `leasing.blacklist_exception.requested` / `leasing.blacklist_exception.request_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingBrand` | `POST` | `/api/v1/leasing/brands` | 新建品牌(4 类) | `leasing.brand.manage` | `leasing.brand.created` / `leasing.brand.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingFollowUp` | `POST` | `/api/v1/leasing/follow-ups` | 新建跟进记录(回写 last_follow_up_at) | `leasing.lead.manage` | `leasing.follow_up.created` / `leasing.follow_up.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingIntent` | `POST` | `/api/v1/leasing/intents` | 线索转意向(stage 起始 initial_contact) | `leasing.intent.manage` | `leasing.intent.created` / `leasing.intent.create_failed` | web | operation 级补充 |
| P0-待归类 | `createLeasingLead` | `POST` | `/api/v1/leasing/leads` | 新建招商线索 | `leasing.lead.manage` | `leasing.lead.created` / `leasing.lead.create_failed` | web | operation 级补充 |
| P0-待归类 | `createMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows` | 新建逐场演出合同记录（草案） | `media.performance.contract.manage` | `media_show.created` / `media_show.create_failed` | web | operation 级补充 |
| P0-待归类 | `createMediaPerformanceShowVersion` | `POST` | `/api/v1/settlement/media-shows/{id}/versions` | 从锁定版本新建合同版本草案 | `media.performance.contract.manage` | `media_show.version_created` / `media_show.version_create_failed` | web | operation 级补充 |
| P0-待归类 | `createMerchantBill` | `POST` | `/api/v1/merchant-billing/bills` | 创建商户账单 | `merchant.billing.manage` | `merchant.billing.create` / `merchant.billing.create_failed` | web | operation 级补充 |
| P0-待归类 | `createMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders` | Create merchant service work order | `merchant.service_work_order.create` | `merchant_service.createMerchantServiceWorkOrder.succeeded` / `merchant_service.createMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `createPartnerOrg` | `POST` | `/api/v1/partner/orgs` | 录入/修改外部协作机构 | `partner.org.manage` | `partner_org.updated` / `partner_org.update_failed` | web | operation 级补充 |
| P0-待归类 | `createPartnerSubordinate` | `POST` | `/api/v1/partner/subordinates` | 自助开通下属账号 | `partner.subordinate.manage` | `partner_subordinate.created` / `partner_subordinate.create_failed` | web | operation 级补充 |
| P0-待归类 | `createServiceOrder` | `POST` | `/api/v1/services/orders` | 代客下单(指定履约人) | `service.order.create` | `service.order.created` / `service.order.create_failed` | staff_work_phone, web, app | operation 级补充 |
| P0-待归类 | `createSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports` | 外包创建进场报备单（施工/装修进场前报备） | `facility.site_entry.create_assigned` | `facility.site_entry.create_assigned` / `facility.site_entry.create_failed` | web | operation 级补充 |
| P0-待归类 | `deactivateDealer` | `POST` | `/api/v1/dealers/{id}/deactivate` | 停用经销商 | `dealer.manage` | `dealer.deactivated` / `dealer.deactivate_failed` | web | operation 级补充 |
| P0-待归类 | `deactivateMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients/{id}/deactivate` | 停用商户费用通知接收人 | `merchant.billing.manage` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `dealerPortalCreateInquiry` | `POST` | `/api/v1/dealer-portal/inquiries` | 门户：未定价商品询价 | `dealer_portal.inquiry.manage` | `dealer.inquiry.created` / `dealer.inquiry.create_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalCreateOrder` | `POST` | `/api/v1/dealer-portal/orders` | 门户：建单（草稿） | `dealer_portal.order.manage` | `dealer.order.drafted` / `dealer.order.draft_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalExternalStock` | `GET` | `/api/v1/dealer-portal/contracts/{id}/external-stock` | 门户：库存外显 | `dealer_portal.stock.view` | `dealer.external_stock.viewed` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalGetOrder` | `GET` | `/api/v1/dealer-portal/orders/{id}` | 门户：订单详情 | `dealer_portal.order.manage` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListContracts` | `GET` | `/api/v1/dealer-portal/contracts` | 门户：本企业经销合同 | `dealer_portal.profile.view` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListInquiries` | `GET` | `/api/v1/dealer-portal/inquiries` | 门户：本企业询价单 | `dealer_portal.inquiry.manage` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListInvoices` | `GET` | `/api/v1/dealer-portal/orders/{id}/invoices` | 门户：查看本企业订单发票 | `dealer_portal.order.manage` | `dealer.portal.invoices_viewed` / `dealer.portal.invoices_view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListOrderLines` | `GET` | `/api/v1/dealer-portal/orders/{id}/lines` | 门户：订单明细 | `dealer_portal.order.manage` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListOrders` | `GET` | `/api/v1/dealer-portal/orders` | 门户：本企业经销订单 | `dealer_portal.order.manage` | `dealer.order.viewed` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListPayments` | `GET` | `/api/v1/dealer-portal/orders/{id}/payments` | 门户：付款记录 | `dealer_portal.order.pay` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalListReturns` | `GET` | `/api/v1/dealer-portal/orders/{id}/returns` | 门户：退货单 | `dealer_portal.return.request` | `not_applicable` / `not_applicable` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalMe` | `GET` | `/api/v1/dealer-portal/me` | 门户：本企业资料与准入状态 | `dealer_portal.profile.view` | `not_applicable` / `dealer.portal.access_rejected` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalRegisterPayment` | `POST` | `/api/v1/dealer-portal/orders/{id}/payments` | 门户：登记付款 | `dealer_portal.order.pay` | `dealer.order.payment_registered` / `dealer.order.payment_register_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalRequestOrderCancel` | `POST` | `/api/v1/dealer-portal/orders/{id}/cancel-request` | 门户：申请取消订单（出库前） | `dealer_portal.order.manage` | `dealer.order.cancel_requested` / `dealer.order.cancel_request_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalRequestReturn` | `POST` | `/api/v1/dealer-portal/orders/{id}/returns` | 门户：申请退货 | `dealer_portal.return.request` | `dealer.return.requested` / `dealer.return.request_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSignOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/sign` | 门户：确认签收 | `dealer_portal.order.manage` | `dealer.delivery.received` / `dealer.delivery.receive_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSubmitOrder` | `POST` | `/api/v1/dealer-portal/orders/{id}/submit` | 门户：提交订单 | `dealer_portal.order.manage` | `dealer.order.submitted` / `dealer.order.submit_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `dealerPortalSubmitProfile` | `POST` | `/api/v1/dealer-portal/me/submission` | 门户：提交注册资料送审 | `dealer_portal.profile.submit` | `dealer.submitted` / `dealer.submit_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `decideDealerInquiryQuote` | `POST` | `/api/v1/dealers/inquiries/{id}/decision` | 审批报价 | `dealer.inquiry.approve` | `dealer.inquiry.decided` / `dealer.inquiry.decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-decision` | 取消审批 | `dealer.order.review` | `dealer.order.cancel_decided` / `dealer.order.cancel_decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/decision` | 审批价格目录 | `dealer.price_catalog.approve` | `dealer.price_catalog.decided` / `dealer.price_catalog.decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/decision` | 审批经销退货 | `dealer.return.review` | `dealer.return.decided` / `dealer.return.decide_failed` | web | operation 级补充 |
| P0-待归类 | `decideInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules/{rule_id}/decision` | 审批付账日配置(提交人不得自审) | `settlement.config.manage` | `settlement.internal_pay_day_rule.approved` / `settlement.internal_pay_day_rule.decision_failed` | web | operation 级补充 |
| P0-待归类 | `designateDealerContractHandler` | `POST` | `/api/v1/dealers/contracts/{id}/designated-handler` | 指定合同销售主体经办人 | `dealer.manage` | `dealer.contract.handler_designated` / `dealer.contract.handler_designate_failed` | web | operation 级补充 |
| P0-待归类 | `designateFacilityWorkOrderConfirmer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/confirmer` | 指定提单确认人 | `facility.work_order.manage` | `facility.work_order_confirmer_designated` / `facility.work_order_confirmer_designate_failed` | web | operation 级补充 |
| P0-待归类 | `disablePartnerSubordinate` | `POST` | `/api/v1/partner/subordinates/{user_id}/disable` | 停用下属账号 | `partner.subordinate.manage` | `partner_subordinate.disabled` / `partner_subordinate.disable_failed` | web | operation 级补充 |
| P0-待归类 | `disputeFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/bearer-dispute` | 工单费用承担方争议挂起 | `facility.work_order.manage` | `facility.work_order_bearer_disputed` / `facility.work_order_bearer_dispute_failed` | web | operation 级补充 |
| P0-待归类 | `disputeMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute` | 对实际履约提出争议 | `media.performance.fulfillment.confirm` | `media_fulfillment.disputed` / `media_fulfillment.dispute_failed` | web | operation 级补充 |
| P0-待归类 | `disputeMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/bearer-dispute` | 商户服务工单费用承担方争议转上级判定 | `merchant.service_work_order.process` | `merchant_service.work_order.bearer_disputed` / `merchant_service.work_order.bearer_dispute_failed` | web | operation 级补充 |
| P0-待归类 | `draftServiceCatalogFeeChange` | `POST` | `/api/v1/services/catalog/{id}/fee-draft` | 提交服务目录项管理费变更草案(指定生效日) | `service.catalog.manage` | `service.catalog.fee_drafted` / `service.catalog.fee_draft_failed` | web | operation 级补充 |
| P0-待归类 | `editContractDraftRentTerms` | `POST` | `/api/v1/contracts/{contract_id}/rent-terms` | 草稿编辑补录租金条款(仅草拟中) | `contract.draft.manage` | `contract.draft_rent_terms_updated` / `contract.draft_rent_terms_update_failed` | web | operation 级补充 |
| P0-待归类 | `energyMeterHistory` | `GET` | `/api/v1/energy/meters/{meter_id}/history` | 读取原始计量与核实处置事实 | `energy.meter.manage` | `energy.history_viewed` / `energy.history_viewed_failed` | web | operation 级补充 |
| P0-待归类 | `executeDealerOrderRefund` | `POST` | `/api/v1/dealers/refunds/{id}/execute` | 将已批准经销退款交统一财务办理 | `dealer.order.refund.execute` | `dealer.order.refund_executed` / `dealer.order.refund_execute_failed` | web | operation 级补充 |
| P0-待归类 | `executeDealerReturnRefunds` | `POST` | `/api/v1/dealers/returns/{id}/refunds/execute` | 登记经销退货统一财务待办 | `dealer.return.refund.execute` | `dealer.return.refund_requested` / `dealer.return.refund_execute_failed` | web | operation 级补充 |
| P0-待归类 | `executeWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/execute` | 签名确认并执行精确清理 | `wecom.retention.manage` | `wecom.cleanup_executed` / `wecom.cleanup_execute_failed` | web | operation 级补充 |
| P0-待归类 | `expireDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/expire` | 报价置为已失效 | `dealer.inquiry.quote` | `dealer.inquiry.expired` / `dealer.inquiry.expire_failed` | web | operation 级补充 |
| P0-待归类 | `exportAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events/export` | 导出操作留痕（敏感） | `audit.operation_log.export` | `audit.operation_log.exported` / `audit.operation_log.export_failed` | web | operation 级补充 |
| P0-待归类 | `exportSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items/export` | 导出开票清单（带密码 PDF） | `settlement.invoice.export` | `settlement.invoice.exported` / `settlement.invoice.export_failed` | web | operation 级补充 |
| P0-待归类 | `generateContractPeriodBills` | `POST` | `/api/v1/contracts/{contract_id}/period-bills` | 某期全费目挂账(租金+物业费+能源费) | `finance.ar_ap.manage` | `contract.fee_billed` / `contract.period_bill_failed` | web | operation 级补充 |
| P0-待归类 | `generateContractRentBill` | `POST` | `/api/v1/contracts/{contract_id}/rent-bills` | 生成某期合同租金应收(base+递增−免租) | `finance.ar_ap.manage` | `contract.rent_billed` / `contract.rent_bill_failed` | web | operation 级补充 |
| P0-待归类 | `generateSettlementInvoiceItems` | `POST` | `/api/v1/settlement/invoice-items/generate` | 按结算单生成开票清单 | `settlement.invoice.manage` | `settlement.invoice.generated` / `settlement.invoice.generate_failed` | web | operation 级补充 |
| P0-待归类 | `getComplaintFollowup` | `GET` | `/api/v1/complaint-followups/{chain}/{work_order_id}` | 读取客诉跟进 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getContractDepositMonths` | `GET` | `/api/v1/contracts/deposit-months` | 查押金收取月数(全平台统一) | `contract.deposit_months.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getContractDepositRefundWait` | `GET` | `/api/v1/contracts/deposit-refund-wait` | 查押金退还等待期(按项目) | `contract.deposit_refund_wait.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getDealerAdmission` | `GET` | `/api/v1/dealers/{id}/admission` | 查看经销商本次准入材料 | `dealer.view` | `dealer.admission_viewed` / `dealer.admission_view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerDelivery` | `GET` | `/api/v1/dealers/orders/{id}/delivery` | 查看发运、签收及差异处理 | `dealer.order.view` | `dealer.delivery.viewed` / `dealer.delivery.view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerOrder` | `GET` | `/api/v1/dealers/orders/{id}` | 经销订单详情 | `dealer.order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getDealerPortalDelivery` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery` | 查看发运、签收及差异处理 | `dealer_portal.order.manage` | `dealer.delivery.viewed` / `dealer.delivery.view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `getDealerPortalReturnFinancials` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-financials` | 本企业查看各退货款项进度 | `dealer_portal.return.request` | `dealer.portal.return_financials_viewed` / `dealer.portal.return_financials_view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `getDealerPortalReturnSources` | `GET` | `/api/v1/dealer-portal/orders/{id}/return-sources` | 本企业查看真实可退签收来源 | `dealer_portal.return.request` | `dealer.portal.return_sources_viewed` / `dealer.portal.return_sources_view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `getDealerReturn` | `GET` | `/api/v1/dealers/returns/{id}` | 经销退货单详情 | `dealer.return.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getDealerReturnFinancial` | `GET` | `/api/v1/dealers/returns/{id}/financial` | 查看货款及首程运费资金进度 | `dealer.return.view` | `dealer.return.financial_viewed` / `dealer.return.financial_view_failed` | web | operation 级补充 |
| P0-待归类 | `getDealerReturnSources` | `GET` | `/api/v1/dealers/orders/{id}/return-sources` | 查看真实可退签收来源 | `dealer.return.view` | `dealer.return.sources_viewed` / `dealer.return.sources_view_failed` | web | operation 级补充 |
| P0-待归类 | `getEnergyMeterDifference` | `GET` | `/api/v1/energy/meters/{meter_id}/difference` | 查询总分表差异 | `energy.reading.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getFacilityInspectionTask` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}` | 巡检任务详情 | `facility.inspection.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getFacilityWorkOrder` | `GET` | `/api/v1/facility/work-orders/{work_order_id}` | 工单详情 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getFacilityWorkOrderMaterials` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/materials` | 工单用料面板 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | 查询导入候选和错误 | `floor_plan.import.view` | `floor_plan.import_candidate_viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getLeasingLeadContactPhone` | `GET` | `/api/v1/leasing/leads/{lead_id}/contact-phone` | 查看某一条招商线索的完整联系人手机号 | `leasing.lead.manage` | `leasing.lead.contact_phone_viewed` / `leasing.lead.contact_phone_view_failed` | web | operation 级补充 |
| P0-待归类 | `getMediaPerformanceShow` | `GET` | `/api/v1/settlement/media-shows/{id}` | 逐场演出合同详情（含履约事实与三项结算） | `media.performance.view` | `media_show.viewed` / `media_show.view_failed` | web | operation 级补充 |
| P0-待归类 | `getMerchantFeeNotificationRecipient` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients/{id}` | 查看商户费用通知接收人 | `merchant.billing.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getMerchantPortalFeeStatement` | `GET` | `/api/v1/merchant-portal/fee-statement` | 商户专区·费用总表与当期综合账单(本商户) | `merchant.billing.view` | `merchant_portal.fee_statement.viewed` / `merchant_portal.fee_statement.view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `getMerchantPortalUnreadBills` | `GET` | `/api/v1/merchant-portal/unread-bills` | 商户专区·首页未读账单提醒(本商户) | `merchant.billing.view` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `getMerchantServiceWorkOrderMaterials` | `GET` | `/api/v1/merchant-service/work-orders/{id}/materials` | 商户服务工单用料面板 | `merchant.service_work_order.process` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getParkingOverview` | `GET` | `/api/v1/parking/overview` | 在场与出场记录及车位占用 | `parking.ops.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getParkingPolicy` | `GET` | `/api/v1/parking/policies/{project_id}` | 查看本项目停车业务参数 | `parking.ops.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getServicePayConfig` | `GET` | `/api/v1/services/pay-config` | 公司统一收款码(下单出示用) | `service.order.create` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getSettlementObjectionPeriod` | `GET` | `/api/v1/settlement/internal-pay-days/objection-period` | 查非积分商户账单异议期(按项目) | `settlement.config.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getSiteEntryReport` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}` | 查看单个进场报备单 | `facility.site_entry.view_assigned` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `getWecomCleanupPreview` | `GET` | `/api/v1/compliance/wecom/cleanup-previews/{id}` | 查看本人清理预览 | `wecom.retention.manage` | `wecom.cleanup_viewed` / `wecom.cleanup_view_failed` | web | operation 级补充 |
| P0-待归类 | `initializeEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/opening` | 登记真实启用初值 | `energy.meter.manage` | `energy.meter_initialized` / `energy.meter_initialized_failed` | web | operation 级补充 |
| P0-待归类 | `inspectDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/inspection` | 退货验收 | `dealer.return.inspect` | `dealer.return.inspected` / `dealer.return.inspect_failed` | web | operation 级补充 |
| P0-待归类 | `issueDealerInvoice` | `POST` | `/api/v1/dealers/invoices/{id}/issue` | 登记发票开具 | `dealer.invoice.manage` | `dealer.invoice.issued` / `dealer.invoice.issue_failed` | web | operation 级补充 |
| P0-待归类 | `issueFacilityWorkOrderMaterial` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/materials` | 登记工单用料 | `facility.work_order.manage` | `facility.work_order_material_issued` / `facility.work_order_material_issue_failed` | web | operation 级补充 |
| P0-待归类 | `issueMerchantServiceWorkOrderMaterial` | `POST` | `/api/v1/merchant-service/work-orders/{id}/materials` | 登记商户服务工单用料 | `merchant.service_work_order.process` | `merchant_service.work_order.material_issued` / `merchant_service.work_order.material_issue_failed` | web | operation 级补充 |
| P0-待归类 | `issueWecomCleanupChallenge` | `POST` | `/api/v1/compliance/wecom/cleanup-previews/{id}/challenge` | 申请本次清理的U盘动作挑战 | `wecom.retention.manage` | `wecom.cleanup_challenged` / `wecom.cleanup_challenge_failed` | web | operation 级补充 |
| P0-待归类 | `listComplianceExpiryItems` | `GET` | `/api/v1/compliance/expiry-items` | 查询合规到期项列表 | `compliance.expiry_item.view` | `compliance.expiry_item.list` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listComplianceItemTypeConfigs` | `GET` | `/api/v1/compliance/item-type-configs` | 查询按证照类型的宽限期与预警提前量配置 | `compliance.expiry_item.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listComplianceReminders` | `GET` | `/api/v1/compliance/reminders` | 查询合规提醒列表 | `compliance.reminder.view` | `compliance.reminder.list` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContractAmendments` | `GET` | `/api/v1/contracts/{contract_id}/amendments` | 变更子合同追溯清单 | `contract.change.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContractApprovalFlow` | `GET` | `/api/v1/contracts/approval-flow` | 查审批流程模板(新建/到期终止/变更三类；项目覆盖优先，缺 contract_type 返回三类) | `contract.approval.flow.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContractChanges` | `GET` | `/api/v1/contracts/{contract_id}/changes` | 合同变更清单 | `contract.change.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContractKeyDates` | `GET` | `/api/v1/compliance/contract-key-dates` | 查询合同关键日期列表 | `compliance.contract_key_date.view` | `compliance.contract_key_date.list` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContracts` | `GET` | `/api/v1/contracts` | 合同清单 | `contract.draft.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listContractSettlements` | `GET` | `/api/v1/contracts/{contract_id}/settlements` | 退租结算清单 | `contract.settlement.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerContracts` | `GET` | `/api/v1/dealers/{id}/contracts` | 经销合同清单 | `dealer.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerCounterpartyConfirmations` | `GET` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | 经办人确认留证清单 | `dealer.order.view` | `dealer.counterparty_confirmation.viewed` / `dealer.counterparty_confirmation.view_failed` | web | operation 级补充 |
| P0-待归类 | `listDealerExternalStock` | `GET` | `/api/v1/dealers/contracts/{id}/external-stock` | 经销库存外显 | `dealer.view` | `dealer.external_stock.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerInquiries` | `GET` | `/api/v1/dealers/{id}/inquiries` | 询价清单 | `dealer.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerInvoices` | `GET` | `/api/v1/dealers/orders/{id}/invoices` | 经销订单电子发票列表 | `dealer.return.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderLines` | `GET` | `/api/v1/dealers/orders/{id}/lines` | 经销订单明细 | `dealer.order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderPayments` | `GET` | `/api/v1/dealers/orders/{id}/payments` | 经销订单到账记录 | `dealer.order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerOrderRefunds` | `GET` | `/api/v1/dealers/orders/{id}/refunds` | 经销订单退款记录 | `dealer.order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerOrders` | `GET` | `/api/v1/dealers/{id}/orders` | 经销订单清单 | `dealer.order.view` | `dealer.order.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerOverpayments` | `GET` | `/api/v1/dealers/{id}/overpayments` | 经销超收记录 | `dealer.order.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerPortalAccounts` | `GET` | `/api/v1/dealers/{id}/portal-accounts` | 经销商门户账号列表 | `dealer.portal_account.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerPriceCatalogLines` | `GET` | `/api/v1/dealers/price-catalogs/{id}/lines` | 价格目录明细 | `dealer.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerPriceCatalogs` | `GET` | `/api/v1/dealers/contracts/{id}/price-catalogs` | 合同价格目录版本清单 | `dealer.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerReturnLines` | `GET` | `/api/v1/dealers/returns/{id}/lines` | 经销退货单明细 | `dealer.return.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealerReturns` | `GET` | `/api/v1/dealers/orders/{id}/returns` | 经销订单退货单列表 | `dealer.return.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listDealers` | `GET` | `/api/v1/dealers` | 经销商清单 | `dealer.view` | `dealer.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listEffectiveDisabledTerminalSwitches` | `GET` | `/api/v1/terminals/capability-switches/effective` | 当前终端的已关闭开关行（前端菜单 gating 数据源） | `not_applicable` | `not_applicable` / `not_applicable` | web, miniapp, app, kiosk, tablet, staff_work_phone, staff_private_phone, merchant_portal, merchant_app, mobile_admin | operation 级补充 |
| P0-待归类 | `listEnergyActualCharges` | `GET` | `/api/v1/energy/actual-charges` | 按账期查看真实独表费用 | `energy.reading.manage` | `energy.actual_charges_viewed` / `energy.actual_charges_viewed_failed` | web | operation 级补充 |
| P0-待归类 | `listEnergyEscalations` | `GET` | `/api/v1/energy/escalations` | 当前主管的未结超期异常 | `energy.reading.approve` | `energy.escalations_viewed` / `energy.escalations_viewed_failed` | web | operation 级补充 |
| P0-待归类 | `listEnergyMeters` | `GET` | `/api/v1/energy/meters` | 表计列表 | `energy.meter.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listEnergyReadings` | `GET` | `/api/v1/energy/readings` | 抄表读数列表 | `energy.reading.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listEnergyReviewSettings` | `GET` | `/api/v1/energy/projects/{project_id}/review-settings` | 查看项目期限配置审批记录 | `energy.meter.manage` | `energy.settings_viewed` / `energy.settings_viewed_failed` | web | operation 级补充 |
| P0-待归类 | `listFacilityAlarmDuties` | `GET` | `/api/v1/facility/alarm-duties` | 接警值守安排列表 | `facility.alarm.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityAlarms` | `GET` | `/api/v1/facility/alarms` | 设备告警接警记录 | `facility.alarm.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityEquipment` | `GET` | `/api/v1/facility/equipment` | 设备台账列表 | `facility.equipment.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityEquipmentScraps` | `GET` | `/api/v1/facility/equipment-scraps` | 设备报废处置清单 | `facility.equipment.manage` | `facility.equipment_scraps_viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityFaultRules` | `GET` | `/api/v1/facility/fault-rules` | 设备故障处置规则列表 | `facility.alarm.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionRecords` | `GET` | `/api/v1/facility/inspections/tasks/{task_id}/records` | 巡检记录列表 | `facility.inspection.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionTasks` | `GET` | `/api/v1/facility/inspections/tasks` | 巡检任务列表 | `facility.inspection.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityInspectionTemplates` | `GET` | `/api/v1/facility/inspections/templates` | 巡检模板列表 | `facility.inspection.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityMaintenanceDueEquipment` | `GET` | `/api/v1/facility/equipment/maintenance-due` | 查询维保到期设备 | `facility.equipment.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityOverdueWorkOrders` | `GET` | `/api/v1/facility/work-orders/overdue` | 逾期工单列表 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media` | 客人投诉附件清单 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFacilityWorkOrders` | `GET` | `/api/v1/facility/work-orders` | 工单列表 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | 查询平面图导入任务 | `floor_plan.import.view` | `floor_plan.import_viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | 查询 P1/P2 商场平面图 | `floor_plan.view` | `floor_plan.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | 查询平面图正式区域 | `floor_plan.view` | `floor_plan.shape_viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listInternalPayDayRules` | `GET` | `/api/v1/settlement/internal-pay-days/rules` | 查三个付账日与钱路指派规则(含待审与历史版本) | `settlement.config.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingBlacklistExceptions` | `GET` | `/api/v1/leasing/blacklist-exceptions` | 招商黑名单例外放行单清单 | `leasing.lead.manage` | `leasing.blacklist_exception.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingBrands` | `GET` | `/api/v1/leasing/brands` | 招商品牌/客户库清单 | `leasing.brand.manage` | `leasing.brand.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingFollowUps` | `GET` | `/api/v1/leasing/follow-ups` | 跟进记录(按 ref 查) | `leasing.lead.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingIntents` | `GET` | `/api/v1/leasing/intents` | 招商意向清单 | `leasing.intent.manage` | `leasing.intent.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingLeads` | `GET` | `/api/v1/leasing/leads` | 招商线索清单 | `leasing.lead.manage` | `leasing.lead.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listLeasingLeadsFollowUpDue` | `GET` | `/api/v1/leasing/leads/follow-up-due` | 待跟进线索(3 天未跟进) | `leasing.lead.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listMediaPerformanceShows` | `GET` | `/api/v1/settlement/media-shows` | 逐场演出合同记录列表 | `media.performance.view` | `media_show.listed` / `media_show.list_failed` | web | operation 级补充 |
| P0-待归类 | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | List merchant bills | `merchant.billing.view` | `merchant_billing.bill.viewed` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantFeeNotificationRecipients` | `GET` | `/api/v1/merchant-billing/fee-notification-recipients` | 查询商户费用通知接收人 | `merchant.billing.manage` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantPortalContracts` | `GET` | `/api/v1/merchant-portal/contracts` | 商户专区·我的合同(只读关键信息) | `contract.view` | `merchant_portal.contract.viewed` / `merchant_portal.contract.view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantPortalNotices` | `GET` | `/api/v1/merchant-portal/notices` | 商户专区·通告(本商户) | `merchant.portal.settlement.view` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantPortalSettlements` | `GET` | `/api/v1/merchant-portal/settlements` | 商户专区·我的对账(结算单只读) | `merchant.portal.settlement.view` | `merchant_portal.settlement.viewed` / `merchant_portal.settlement.view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantPortalTicketSettlements` | `GET` | `/api/v1/merchant-portal/ticket-settlements` | 商户专区·门票分账(本商户内部结算往来与待付状态) | `merchant.portal.settlement.view` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrderCloseMethods` | `GET` | `/api/v1/merchant-service/work-order-close-methods` | 商户服务工单关单方式（报修、其他服务请求） | `merchant.service_work_order.view` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrderOwnerCandidates` | `GET` | `/api/v1/merchant-service/work-orders/{id}/owner-candidates` | 商户服务工单派单负责人候选 | `merchant.service_work_order.assign` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrderPolicies` | `GET` | `/api/v1/merchant-service/work-order-policies` | 商户服务工单处理时限列表（工单类型×优先级） | `merchant.service_work_order.assign` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | List merchant service work orders | `merchant.service_work_order.view` | `merchant_service.listMerchantServiceWorkOrders.succeeded` / `not_applicable` | web, mobile_admin, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMyFacilityAlarms` | `GET` | `/api/v1/facility/alarms/mine` | 我的接警 | `facility.alarm.take_over` | `not_applicable` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| P0-待归类 | `listMyMerchantCardExitNotices` | `GET` | `/api/v1/merchant-portal/card-exit-notices` | 商户专区·本商户储值卡退出公告 | `merchant.portal.card_exit_notice.publish` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `listMyMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders/assigned-to-me` | 我的商户服务工单（员工手机「我的工单」） | `merchant.service_work_order.process` | `merchant_service.work_order.viewed` / `not_applicable` | staff_private_phone, staff_work_phone | operation 级补充 |
| P0-待归类 | `listMyServiceOrders` | `GET` | `/api/v1/services/orders/assigned-to-me` | 我的服务任务(履约人手机) | `service.order.perform` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| P0-待归类 | `listParkingCredits` | `GET` | `/api/v1/parking/credits` | 停车消费抵免台账 | `parking.ops.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listParkingManualReleases` | `GET` | `/api/v1/parking/manual-releases` | 人工放行记录 | `parking.release.record` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listParkingMonthlyVehicles` | `GET` | `/api/v1/parking/monthly-vehicles` | 月租车牌清单 | `parking.vehicle.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listParkingStaffVehicles` | `GET` | `/api/v1/parking/staff-vehicles` | 员工车辆清单 | `parking.vehicle.manage` | `not_applicable` / `not_applicable` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `listPartnerOrgs` | `GET` | `/api/v1/partner/orgs` | 外部协作机构列表 | `partner.org.view` | `partner_org.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listPartnerSubordinates` | `GET` | `/api/v1/partner/subordinates` | 本机构下属账号列表 | `partner.subordinate.view` | `partner_subordinate.viewed` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listServiceCatalog` | `GET` | `/api/v1/services/catalog` | 服务目录(上架项,下单用) | `service.order.create` | `not_applicable` / `not_applicable` | web, app, staff_work_phone | operation 级补充 |
| P0-待归类 | `listServiceCatalogAll` | `GET` | `/api/v1/services/catalog/all` | 服务目录全量(含下架,后台维护) | `service.catalog.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listServiceOrders` | `GET` | `/api/v1/services/orders` | 服务单列表(后台) | `service.catalog.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSettlementBills` | `GET` | `/api/v1/settlement/bills` | 月度结算单头列表 | `finance.settlement.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSettlementInvoiceItems` | `GET` | `/api/v1/settlement/invoice-items` | 开票清单列表 | `settlement.invoice.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSettlementTransactions` | `GET` | `/api/v1/settlement/transactions` | 通票核销结算流水列表 | `finance.settlement.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSiteEntryHistory` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/history` | 查看各轮施工材料与验收记录 | `facility.site_entry.view_assigned` | `facility.site_entry.history_viewed` / `facility.site_entry.history_view_failed` | web | operation 级补充 |
| P0-待归类 | `listSiteEntryReports` | `GET` | `/api/v1/facility/site-entry-reports` | 查看进场报备/验收单列表（外包只见本机构，平台全量） | `facility.site_entry.view_assigned` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSpaceExpiryColorRules` | `GET` | `/api/v1/contracts/expiry-color-rules` | 查到期着色分类 | `contract.space.color.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listSpaceRentalStatus` | `GET` | `/api/v1/contracts/space-rental-status` | 铺位平面图着色数据源(可租/已租/即将到期) | `contract.draft.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listStaffAppFunctions` | `GET` | `/api/v1/terminals/staff-app-functions` | 员工 App 本端可用功能（按设备类型下发） | `not_applicable` | `not_applicable` / `not_applicable` | staff_work_phone, staff_private_phone, tablet | operation 级补充 |
| P0-待归类 | `listSubordinateCompletion` | `GET` | `/api/v1/partner/subordinates/completion` | 机构管理员查看本机构下属工作完成情况 | `partner.subordinate.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
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
| P0-待归类 | `listWecomRiskHits` | `GET` | `/api/v1/compliance/wecom/risk-hits` | 查询企微会话违规命中列表 | `wecom.risk_hit.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `listWorkOrdersAssignedToMe` | `GET` | `/api/v1/facility/work-orders/assigned-to-me` | 我的工单(员工自有手机) | `facility.work_order.view_assigned` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| P0-待归类 | `lockMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/lock` | 锁定合同版本 | `media.performance.contract.manage` | `media_show.locked` / `media_show.lock_failed` | web | operation 级补充 |
| P0-待归类 | `markFacilityWorkOrderCostBearer` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/cost-bearer` | 标明工单费用承担方 | `facility.work_order.manage` | `facility.work_order_cost_bearer_marked` / `facility.work_order_cost_bearer_mark_failed` | web | operation 级补充 |
| P0-待归类 | `markFacilityWorkOrdersOverdue` | `POST` | `/api/v1/facility/work-orders/mark-overdue` | 批量标记逾期工单 | `facility.work_order.manage` | `facility.work_order_overdue_marked` / `facility.work_order_overdue_mark_failed` | web | operation 级补充 |
| P0-待归类 | `markMerchantServiceWorkOrderCostBearer` | `POST` | `/api/v1/merchant-service/work-orders/{id}/cost-bearer` | 标明商户服务工单费用承担方 | `merchant.service_work_order.process` | `merchant_service.work_order.cost_bearer_marked` / `merchant_service.work_order.cost_bearer_mark_failed` | web | operation 级补充 |
| P0-待归类 | `markServiceOrderPaid` | `POST` | `/api/v1/services/orders/{id}/mark-paid` | 确认收款到公司账户(服务员现场) | `service.order.create` | `service.order.paid` / `service.order.pay_confirm_failed` | staff_work_phone, web | operation 级补充 |
| P0-待归类 | `myServicePaymentCode` | `GET` | `/api/v1/services/payment-code/mine` | 我的收款码 | `service.order.perform` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `myServiceStats` | `GET` | `/api/v1/services/orders/my-stats` | 我的服务统计(当月次数+参考收入) | `service.order.perform` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| P0-待归类 | `openDealerOrderRefundApproval` | `POST` | `/api/v1/dealers/refunds/{id}/approval` | 开启经销退款审批 | `dealer.order.payment.verify` | `dealer.order.refund_requested` / `dealer.order.refund_request_failed` | web | operation 级补充 |
| P0-待归类 | `previewWecomCleanup` | `POST` | `/api/v1/compliance/wecom/cleanup-previews` | 创建逐条消息清理预览 | `wecom.retention.manage` | `wecom.cleanup_previewed` / `wecom.cleanup_preview_failed` | web | operation 级补充 |
| P0-待归类 | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | 发布平面图导入候选 | `floor_plan.import.manage` | `floor_plan.import_published` / `floor_plan.import_publish_failed` | web | operation 级补充 |
| P0-待归类 | `publishMerchantCardExitNotice` | `POST` | `/api/v1/merchant-portal/card-exit-notices` | 商户专区·发布本商户储值卡退出公告 | `merchant.portal.card_exit_notice.publish` | `stored_value.exit_notice_published` / `stored_value.exit_notice_publish_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `publishTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs/{id}/publish` | 公布已批准的门票奖励规则 | `ticket.reward.config.manage` | `ticket.reward.config.publish.completed` / `ticket.reward.config.publish.completed.failed` | web | operation 级补充 |
| P0-待归类 | `queryAuditOperationEvents` | `GET` | `/api/v1/audit/operation-events` | 跨对象查询操作留痕 | `audit.operation_log.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `quoteDealerInquiry` | `POST` | `/api/v1/dealers/inquiries/{id}/quote` | 对询价报价 | `dealer.inquiry.quote` | `dealer.inquiry.quoted` / `dealer.inquiry.quote_failed` | web | operation 级补充 |
| P0-待归类 | `raiseContractChange` | `POST` | `/api/v1/contracts/{contract_id}/changes` | 发起合同变更(6 类) | `contract.change.manage` | `contract.change_raised` / `contract.change_raise_failed` | web | operation 级补充 |
| P0-待归类 | `readDealerDeliveryEvidence` | `GET` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence/{attachmentId}` | 受控读取本企业订单交接原文件 | `dealer_portal.order.manage` | `dealer.portal.delivery_evidence_viewed` / `dealer.portal.delivery_evidence_view_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `readEnergyMeterAttachment` | `GET` | `/api/v1/energy/meters/{meter_id}/attachments/{attachment_id}/file` | 受控读取本表计及换表关联原件 | `energy.meter.manage` | `energy.attachment_viewed` / `energy.attachment_viewed_failed` | web | operation 级补充 |
| P0-待归类 | `readFacilityWorkOrderCustomerMedia` | `GET` | `/api/v1/facility/work-orders/{work_order_id}/customer-media/{media_id}/file` | 回读客人投诉附件 | `facility.work_order.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `readSiteEntryAttachment` | `GET` | `/api/v1/facility/site-entry-reports/{report_id}/attachments/{attachment_id}/file` | 受控读取本报备完工文件 | `facility.site_entry.view_assigned` | `facility.site_entry.attachment_accessed` / `facility.site_entry.attachment_access_failed` | web | operation 级补充 |
| P0-待归类 | `reassignFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/reassign` | 工单转派 | `facility.work_order.manage` | `facility.work_order_reassigned` / `facility.work_order_reassign_failed` | web | operation 级补充 |
| P0-待归类 | `receiveDealerReturn` | `POST` | `/api/v1/dealers/returns/{id}/receive` | 退货收货 | `dealer.return.inspect` | `dealer.return.received` / `dealer.return.receive_failed` | web | operation 级补充 |
| P0-待归类 | `recheckFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/recheck` | 复查整改后的巡检任务 | `facility.inspection.manage` | `facility.inspection_task_rechecked` / `facility.inspection_task_recheck_failed` | web | operation 级补充 |
| P0-待归类 | `recordDealerCounterpartyConfirmation` | `POST` | `/api/v1/dealers/orders/{id}/counterparty-confirmations` | 登记对方确认留证 | `dealer.order.manage` | `dealer.counterparty_confirmation.recorded` / `dealer.counterparty_confirmation.record_failed` | web | operation 级补充 |
| P0-待归类 | `recordDealerInvoiceReversalResult` | `POST` | `/api/v1/dealers/invoices/{id}/reversal-result` | 登记发票真实红冲结果 | `dealer.invoice.manage` | `dealer.invoice.reversal_result_recorded` / `dealer.invoice.reversal_result_failed` | web | operation 级补充 |
| P0-待归类 | `recordDealerReturnRefundResult` | `POST` | `/api/v1/dealers/returns/{id}/refund-result` | 登记退货退款执行结果 | `dealer.return.refund.execute` | `not_applicable` / `dealer.return.refund_execute_failed` | web | operation 级补充 |
| P0-待归类 | `recordEnergyActualCharge` | `POST` | `/api/v1/energy/actual-charges` | 核对供能单据登记实际费用 | `energy.reading.manage` | `energy.actual_charge_recorded` / `energy.actual_charge_recorded_failed` | web | operation 级补充 |
| P0-待归类 | `recordEnergyReading` | `POST` | `/api/v1/energy/readings` | 录入抄表读数 | `energy.reading.manage` | `energy.reading_recorded` / `energy.reading_recorded_failed` | web | operation 级补充 |
| P0-待归类 | `recordFacilityAlarmDisposition` | `POST` | `/api/v1/facility/alarms/{alarm_id}/disposition` | 记录设备告警处置要求 | `facility.alarm.take_over` | `facility.alarm_disposition_recorded` / `facility.alarm_disposition_failed` | web | operation 级补充 |
| P0-待归类 | `recordFacilityEquipmentMaintenanceDone` | `POST` | `/api/v1/facility/equipment/{equipment_id}/maintenance-done` | 记录设备维保完成 | `facility.equipment.manage` | `facility.equipment_maintained` / `facility.equipment_maintain_failed` | web | operation 级补充 |
| P0-待归类 | `recordFacilityEquipmentScrapFinance` | `POST` | `/api/v1/facility/equipment-scraps/{scrap_id}/finance` | 财务登记设备报废残值或损失 | `finance.ar_ap.manage` | `facility.equipment_scrap_finance_recorded` / `facility.equipment_scrap_finance_record_failed` | web | operation 级补充 |
| P0-待归类 | `recordMediaPerformanceFulfillment` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment` | 传媒经办登记实际履约 | `media.performance.fulfillment.record` | `media_fulfillment.recorded` / `media_fulfillment.record_failed` | web | operation 级补充 |
| P0-待归类 | `recordMerchantBillDunning` | `POST` | `/api/v1/merchant-billing/bills/{id}/dunning-records` | 记录账单催收 | `merchant.billing.dunning.record` | `merchant.billing.dunning.record` / `merchant.billing.dunning.record_failed` | web | operation 级补充 |
| P0-待归类 | `recordParkingManualRelease` | `POST` | `/api/v1/parking/manual-releases` | 当班保安登记人工放行 | `parking.release.record` | `parking.manual_release_recorded` / `parking.manual_release_record_failed` | web | operation 级补充 |
| P0-待归类 | `registerDealerOrderPayment` | `POST` | `/api/v1/dealers/orders/{id}/payments` | 登记经销订单到账 | `dealer.order.manage` | `dealer.order.payment_registered` / `dealer.order.payment_register_failed` | web | operation 级补充 |
| P0-待归类 | `registerParkingMonthlyVehicle` | `POST` | `/api/v1/parking/contracts/{contract_id}/vehicles` | 为合同登记月租车牌 | `parking.vehicle.manage` | `parking.monthly_vehicle_registered` / `parking.monthly_vehicle_register_failed` | web | operation 级补充 |
| P0-待归类 | `registerParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles` | 所属主体登记员工车辆 | `parking.vehicle.manage` | `parking.staff_vehicle_registered` / `parking.staff_vehicle_register_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `rejectMerchantPortalSettlementResolution` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/reject-resolution` | 商户专区·不接受主管裁决 | `merchant.portal.settlement.dispute` | `merchant_portal.settlement.resolution_rejected` / `merchant_portal.settlement.resolution_reject_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `renewComplianceExpiryItem` | `POST` | `/api/v1/compliance/expiry-items/{id}/renewal` | 证照续期登记（更新有效期） | `compliance.expiry_item.manage` | `compliance.expiry_item.renewed` / `compliance.expiry_item.renew_failed` | web | operation 级补充 |
| P0-待归类 | `replaceEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/replacement` | 登记旧终值、新初值与实际换表交接 | `energy.meter.manage` | `energy.meter_replaced` / `energy.meter_replaced_failed` | web | operation 级补充 |
| P0-待归类 | `reportInspectionException` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/report-exception` | 外包对被指派巡检任务上报异常 | `facility.inspection.report_exception_assigned` | `facility.inspection_exception_reported` / `facility.inspection_exception_report_failed` | web | operation 级补充 |
| P0-待归类 | `requestComplianceGracePeriod` | `POST` | `/api/v1/compliance/expiry-items/{id}/grace-requests` | 商户申请证照宽限期（最高权限审批） | `compliance.grace_period.request` | `compliance.grace.submitted` / `compliance.grace.submit_failed` | web | operation 级补充 |
| P0-待归类 | `requestContractScopeGrant` | `POST` | `/api/v1/contracts/{contract_id}/scope-requests` | 超范围下单经营申请(拦下后可申请,走统一审批) | `contract.scope_request.raise` | `contract.scope_request_raised` / `contract.scope_request_raise_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerDeliveryResolution` | `POST` | `/api/v1/dealers/delivery/discrepancies/{id}/resolutions` | 申请交付差异处理 | `dealer.order.manage` | `dealer.delivery.resolution_requested` / `dealer.delivery.resolution_request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerInvoice` | `POST` | `/api/v1/dealers/orders/{id}/invoices` | 申请电子发票 | `dealer.invoice.manage` | `dealer.invoice.requested` / `dealer.invoice.request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerOrderCancel` | `POST` | `/api/v1/dealers/orders/{id}/cancel-request` | 申请取消经销订单（出库前） | `dealer.order.manage` | `dealer.order.cancel_requested` / `dealer.order.cancel_request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDealerReturn` | `POST` | `/api/v1/dealers/orders/{id}/returns` | 申请经销退货 | `dealer.return.manage` | `dealer.return.requested` / `dealer.return.request_failed` | web | operation 级补充 |
| P0-待归类 | `requestDeleteFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/delete-request` | 申请删除设施设备 | `facility.equipment.manage` | `facility.equipment_delete_requested` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `requestDeleteMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/delete-request` | 申请删除商户服务工单 | `merchant.service_work_order.close` | `merchant_service.work_order.delete_requested` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `requestDeleteServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/delete-request` | 申请删除服务目录项 | `service.catalog.manage` | `service.catalog.delete_requested` / `service.catalog.delete_request_failed` | web | operation 级补充 |
| P0-待归类 | `resolveContractDispute` | `POST` | `/api/v1/contracts/settlements/{settlement_id}/resolve-dispute` | 裁决退租争议→回到待结算 | `contract.settlement.dispute.resolve` | `contract.dispute_resolved` / `contract.dispute_resolve_failed` | web | operation 级补充 |
| P0-待归类 | `resolveMediaPerformanceDispute` | `POST` | `/api/v1/settlement/media-shows/{id}/fulfillment/dispute/resolve` | 主管裁决履约争议 | `media.performance.dispute.resolve` | `media_fulfillment.dispute_resolved` / `media_fulfillment.dispute_resolve_failed` | web | operation 级补充 |
| P0-待归类 | `resumeFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/resume` | 恢复工单 | `facility.work_order.manage` | `facility.work_order_resumed` / `facility.work_order_resume_failed` | web | operation 级补充 |
| P0-待归类 | `retireEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}/retirement` | 登记真实终段读数并永久停用表计 | `energy.meter.manage` | `energy.meter_retired` / `energy.meter_retired_failed` | web | operation 级补充 |
| P0-待归类 | `retireFacilityFaultRule` | `POST` | `/api/v1/facility/fault-rules/{rule_id}/retire` | 停用设备故障处置规则 | `facility.alarm.manage` | `facility.fault_rule_retired` / `facility.fault_rule_retire_failed` | web | operation 级补充 |
| P0-待归类 | `reverseDealerInvoice` | `POST` | `/api/v1/dealers/returns/{id}/invoice-reversal` | 红冲原发票并按最新未退金额重开 | `dealer.invoice.manage` | `dealer.invoice.reversal_requested` / `dealer.invoice.reverse_failed` | web | operation 级补充 |
| P0-待归类 | `reviewDealer` | `POST` | `/api/v1/dealers/{id}/review` | 经销商准入审核 | `dealer.review` | `dealer.reviewed` / `dealer.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/review` | 经销订单审核（通过即预留库存并通知付款） | `dealer.order.review` | `dealer.order.reviewed` / `dealer.order.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/review` | 复核异常抄表读数 | `energy.reading.manage` | `energy.reading_reviewed` / `energy.reading_reviewed_failed` | web | operation 级补充 |
| P0-待归类 | `reviewFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/review` | 报修方回访工单 | `facility.work_order.manage` | `facility.work_order_reviewed` / `facility.work_order_review_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| P0-待归类 | `reviewSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/review` | 物业审核进场报备（平台侧） | `facility.site_entry.review` | `facility.site_entry.review` / `facility.site_entry.review_failed` | web | operation 级补充 |
| P0-待归类 | `reviewWecomRiskHit` | `POST` | `/api/v1/compliance/wecom/risk-hits/{id}/review` | 复核处置企微违规命中 | `wecom.risk_hit.review` | `wecom.risk.hit_reviewed` / `wecom.risk_hit.review_failed` | web | operation 级补充 |
| P0-待归类 | `revokeParkingMonthlyVehicle` | `POST` | `/api/v1/parking/monthly-vehicles/{vehicle_id}/revoke` | 撤销月租车牌 | `parking.vehicle.manage` | `parking.monthly_vehicle_revoked` / `parking.monthly_vehicle_revoke_failed` | web | operation 级补充 |
| P0-待归类 | `revokeParkingStaffVehicle` | `POST` | `/api/v1/parking/staff-vehicles/{vehicle_id}/revoke` | 撤销员工车辆 | `parking.vehicle.manage` | `parking.staff_vehicle_revoked` / `parking.staff_vehicle_revoke_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `reworkFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/rework` | 验收或提单确认不通过退回返工 | `facility.work_order.manage` | `facility.work_order_rework_requested` / `facility.work_order_rework_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| P0-待归类 | `saveComplianceItemTypeConfig` | `PUT` | `/api/v1/compliance/item-type-configs/{item_type}` | 配置某证照类型的宽限期与预警提前量 | `compliance.expiry_item.manage` | `compliance.item_type_config.saved` / `compliance.item_type_config.save_failed` | web | operation 级补充 |
| P0-待归类 | `saveContractApprovalFlow` | `POST` | `/api/v1/contracts/approval-flow` | 配置审批流程模板(按合同类型,按综合体覆盖) | `contract.approval.flow.manage` | `contract.approval_flow_saved` / `contract.approval_flow_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveContractDepositMonths` | `POST` | `/api/v1/contracts/deposit-months` | 配置押金收取月数及上下限(全平台统一) | `contract.deposit_months.manage` | `contract.deposit_months_configured` / `contract.deposit_months_config_failed` | web | operation 级补充 |
| P0-待归类 | `saveContractDepositRefundWait` | `POST` | `/api/v1/contracts/deposit-refund-wait` | 配置押金退还等待期及上下限(按项目) | `contract.deposit_refund_wait.manage` | `contract.deposit_refund_wait_configured` / `contract.deposit_refund_wait_config_failed` | web | operation 级补充 |
| P0-待归类 | `saveFacilityAlarmResponsePolicy` | `POST` | `/api/v1/facility/alarms/policies` | 保存设备告警响应时限（告警类型×优先级一格） | `facility.alarm.manage` | `facility.alarm_policy_saved` / `facility.alarm_policy_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveFacilityWorkOrderPolicy` | `POST` | `/api/v1/facility/work-orders/policies` | 保存工单 SLA/验收策略 | `facility.work_order.manage` | `facility.work_order_policy_saved` / `facility.work_order_policy_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveFloorPlanShape` | `POST` | `/api/v1/floor-plans/{id}/shapes` | 保存平面图图形 | `floor_plan.manage` | `floor_plan.shape.save` / `floor_plan.shape.save_failed` | web | operation 级补充 |
| P0-待归类 | `saveMerchantFeeNotificationRecipient` | `POST` | `/api/v1/merchant-billing/fee-notification-recipients` | 保存商户费用通知接收人 | `merchant.billing.manage` | `not_applicable` / `not_applicable` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `saveMerchantServiceWorkOrderCloseMethod` | `PUT` | `/api/v1/merchant-service/work-order-close-methods` | 保存报修或其他服务请求的关单方式 | `merchant.service_work_order.assign` | `merchant_service.work_order_close_method_saved` / `merchant_service.work_order_close_method_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveMerchantServiceWorkOrderPolicy` | `PUT` | `/api/v1/merchant-service/work-order-policies` | 保存商户服务工单处理时限（工单类型×优先级一格） | `merchant.service_work_order.assign` | `merchant_service.work_order_policy_saved` / `merchant_service.work_order_policy_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveParkingPolicy` | `PUT` | `/api/v1/parking/policies/{project_id}` | 配置本项目停车业务参数（值与上下限同配） | `parking.config.manage` | `parking.policy_saved` / `parking.policy_save_failed` | web | operation 级补充 |
| P0-待归类 | `saveSettlementObjectionPeriod` | `POST` | `/api/v1/settlement/internal-pay-days/objection-period` | 配置非积分商户账单异议期及上下限(按项目) | `settlement.config.manage` | `settlement.objection_period.configured` / `settlement.objection_period.config_failed` | web | operation 级补充 |
| P0-待归类 | `saveSpaceExpiryColorRule` | `POST` | `/api/v1/contracts/expiry-color-rules` | 配置到期着色分类(颜色+提前天数) | `contract.space.color.manage` | `contract.color_rule_saved` / `contract.color_rule_save_failed` | web | operation 级补充 |
| P0-待归类 | `scanComplianceReminders` | `POST` | `/api/v1/compliance/reminders/scan` | 触发合规提醒扫描 | `compliance.reminder.scan` | `compliance.reminder.scan` / `compliance.reminder.scan_failed` | web | operation 级补充 |
| P0-待归类 | `serviceOrderPaymentCode` | `GET` | `/api/v1/services/orders/{id}/payment-code` | 出示公司统一收款码(H批代收;个人码流程已废弃) | `service.order.create` | `service.payment_code.presented` / `service.payment_code.present_failed` | staff_work_phone, web, app | operation 级补充 |
| P0-待归类 | `setMyServicePaymentCode` | `POST` | `/api/v1/services/payment-code` | 录入我的收款码(履约人) | `service.order.perform` | `service.payment_code.saved` / `service.payment_code.save_failed` | web | operation 级补充 |
| P0-待归类 | `setServicePayConfig` | `POST` | `/api/v1/services/pay-config` | 配置公司统一收款码 | `service.catalog.manage` | `service.pay_config.saved` / `service.pay_config.save_failed` | web | operation 级补充 |
| P0-待归类 | `settleContract` | `POST` | `/api/v1/contracts/{contract_id}/settle` | 退租/到期结算(逐例顺序) | `contract.settlement.manage` | `contract.settled` / `contract.settle_failed` | web | operation 级补充 |
| P0-待归类 | `settleEnergyFee` | `POST` | `/api/v1/energy/settlements/energy-fee` | 按账期结算能源费 | `energy.reading.manage` | `energy.fee_settled` / `energy.fee_settle_failed` | web | operation 级补充 |
| P0-待归类 | `settleEnergyFeeShortcut` | `POST` | `/api/v1/energy/settle-energy-fee` | 按账期结算能源费(兼容路径) | `energy.reading.manage` | `energy.fee_settled` / `energy.fee_settle_failed` | web | operation 级补充 |
| P0-待归类 | `settleMediaPerformanceShow` | `POST` | `/api/v1/settlement/media-shows/{id}/settle` | 按锁定合同结算三项费用 | `media.performance.settle` | `media_settlement.settled` / `media_settlement.settle_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateAttributionRuleAlias` | `POST` | `/api/v1/settlement/config/attribution-rules/{id}/activate` | 复核转正:归因规则草案(弃用薄别名) | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/activate` | 复核转正:通票分账方案草案通告 | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateDraft` | `POST` | `/api/v1/settlement/config/{configType}/{id}/activate` | 复核转正:单行/归属/归因规则草案转正 | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigActivateRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers/activate` | 复核转正:排名档位梯子整体换梯 | `settlement.config.manage` | `settlement.config.draft.activated` / `settlement.config.draft.activate_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigApprovePassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/approve` | 批准短期经营激励申请(项目之上的平台权限) | `settlement.bill.special_approve` | `settlement.config.pass_incentive.approved` / `settlement.config.pass_incentive.approve_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigArchive` | `POST` | `/api/v1/settlement/config/{configType}/{id}/archive` | 软删结算配置(status→inactive) | `settlement.config.manage` | `settlement.config.archived` / `settlement.config.archive_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigDistributionScheme` | `GET` | `/api/v1/settlement/config/distribution-scheme` | 通票分账方案:版本、通告期与缺档位行待办 | `settlement.config.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `settlementConfigListInternalPriceItems` | `GET` | `/api/v1/settlement/config/internal-price-items` | 内部结算SKU目录三维筛选列表 | `settlement.config.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `settlementConfigOverview` | `GET` | `/api/v1/settlement/config/overview` | 结算配置中心总览 | `settlement.config.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `settlementConfigRejectPassIncentive` | `POST` | `/api/v1/settlement/config/incentive-applications/{id}/reject` | 驳回短期经营激励申请(项目之上的平台权限) | `settlement.bill.special_approve` | `settlement.config.pass_incentive.rejected` / `settlement.config.pass_incentive.reject_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigSaveDistributionNoticePeriod` | `POST` | `/api/v1/settlement/config/distribution-scheme/notice-period` | 维护通票分账方案通告期 | `settlement.config.manage` | `settlement.config.distribution_notice_period.updated` / `settlement.config.distribution_notice_period.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigSaveDistributionScheme` | `POST` | `/api/v1/settlement/config/distribution-scheme` | 通票分账方案整套落草案(参与家数档位表+第一段名单) | `settlement.config.manage` | `settlement.config.distribution_scheme.updated` / `settlement.config.distribution_scheme.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigSubmitPassIncentive` | `POST` | `/api/v1/settlement/config/distribution-scheme/{id}/incentive-applications` | 提交短期经营激励申请(首次或到期重审) | `settlement.config.manage` | `settlement.config.pass_incentive.submitted` / `settlement.config.pass_incentive.submit_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertChannelFee` | `POST` | `/api/v1/settlement/config/channel-fee` | 维护渠道费用(通路费/销售渠道费,草案) | `settlement.config.manage` | `settlement.config.channel_fee_configs.updated` / `settlement.config.channel_fee_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertComboDiscount` | `POST` | `/api/v1/settlement/config/combo-discount` | 维护套票折扣归属(草案) | `settlement.config.manage` | `settlement.config.ticket_combo_discount_configs.updated` / `settlement.config.ticket_combo_discount_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertCommissionRate` | `POST` | `/api/v1/settlement/config/commission-rate` | 维护卡座销售提成费率(草案) | `settlement.config.manage` | `settlement.config.bar_commission_rate_configs.updated` / `settlement.config.bar_commission_rate_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertComprehensiveServiceFee` | `POST` | `/api/v1/settlement/config/comprehensive-service-fee` | 维护门票综合服务费(草案) | `settlement.config.manage` | `settlement.config.comprehensive_service_fee_configs.updated` / `settlement.config.comprehensive_service_fee_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertDistributionMethod` | `POST` | `/api/v1/settlement/config/distribution-method` | 维护通票分配方式(草案) | `settlement.config.manage` | `settlement.config.ticket_distribution_configs.updated` / `settlement.config.ticket_distribution_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertHighValueThreshold` | `POST` | `/api/v1/settlement/config/high-value-threshold` | 提交高价值判定阈值调整(最高权限审批) | `settlement.config.manage` | `settlement.config.high_value_thresholds.updated` / `settlement.config.high_value_thresholds.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertInternalPriceItem` | `POST` | `/api/v1/settlement/config/internal-price-item` | 维护内部结算SKU目录条目(草案) | `settlement.config.manage` | `settlement.config.internal_price_catalog.updated` / `settlement.config.internal_price_catalog.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertMerchantRate` | `POST` | `/api/v1/settlement/config/merchant-rate` | 维护每客单价/固定额(草案) | `settlement.config.manage` | `settlement.config.ticket_distribution_merchant_rates.updated` / `settlement.config.ticket_distribution_merchant_rates.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertPassGiftCost` | `POST` | `/api/v1/settlement/config/pass-gift-cost` | 维护赠送权益预估成本预扣规则(草案) | `settlement.config.manage` | `settlement.config.pass_gift_cost_configs.updated` / `settlement.config.pass_gift_cost_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertPointsAccrualRate` | `POST` | `/api/v1/settlement/config/points-accrual-rate` | 维护积分累计率(草案) | `settlement.config.manage` | `settlement.config.points_accrual_rate_configs.updated` / `settlement.config.points_accrual_rate_configs.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRankTiers` | `POST` | `/api/v1/settlement/config/rank-tiers` | 维护排名档位梯子(草案·整套替换) | `settlement.config.manage` | `settlement.config.ticket_distribution_rank_tiers.updated` / `settlement.config.ticket_distribution_rank_tiers.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRetention` | `POST` | `/api/v1/settlement/config/retention` | 维护项目公司留存比例(草案) | `settlement.config.manage` | `settlement.config.platform_retention_rules.updated` / `settlement.config.platform_retention_rules.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementConfigUpsertRevenueStreamAttribution` | `POST` | `/api/v1/settlement/config/revenue-stream-attribution` | 维护业态收入归属(草案) | `settlement.config.manage` | `settlement.config.revenue_stream_attribution.updated` / `settlement.config.revenue_stream_attribution.update_failed` | web | operation 级补充 |
| P0-待归类 | `settlementInvoiceSummary` | `GET` | `/api/v1/settlement/invoice-items/summary` | 开票清单按主体汇总 | `settlement.invoice.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `settlementPostBill` | `POST` | `/api/v1/settlement/bills/{id}/post` | 结算单过账(挂应付台账) | `settlement.bill.post` | `settlement.bill.posted` / `settlement.bill.post_failed` | web | operation 级补充 |
| P0-待归类 | `shipDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/ship` | 仓库出库 | `dealer.order.ship` | `dealer.order.shipped` / `dealer.order.ship_failed` | web | operation 级补充 |
| P0-待归类 | `shipDealerReplacement` | `POST` | `/api/v1/dealers/delivery/resolutions/{id}/replacement-shipment` | 按批准差异补交出库 | `dealer.order.ship` | `dealer.delivery.replacement_shipped` / `dealer.delivery.replacement_ship_failed` | web | operation 级补充 |
| P0-待归类 | `signDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/sign` | 登记签收 | `dealer.order.manage` | `dealer.delivery.received` / `dealer.delivery.receive_failed` | web | operation 级补充 |
| P0-待归类 | `signLeasingIntent` | `POST` | `/api/v1/leasing/intents/{intent_id}/sign` | 意向签约→转商户+喂入合同 draft | `leasing.convert` | `leasing.intent.signed` / `leasing.intent.sign_failed` | web | operation 级补充 |
| P0-待归类 | `startFacilityInspectionTask` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/start` | 开始巡检任务 | `facility.inspection.manage` | `facility.inspection_task_started` / `facility.inspection_task_start_failed` | web | operation 级补充 |
| P0-待归类 | `startFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/start` | 开始处理工单 | `facility.work_order.manage` | `facility.work_order_started` / `facility.work_order_start_failed` | web | operation 级补充 |
| P0-待归类 | `submitContract` | `POST` | `/api/v1/contracts/{contract_id}/submit` | 提交进统一审批(→待确认) | `contract.draft.manage` | `contract.submitted` / `contract.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealer` | `POST` | `/api/v1/dealers/{id}/submit` | 提交经销商准入审核 | `dealer.manage` | `dealer.submitted` / `dealer.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealerOrder` | `POST` | `/api/v1/dealers/orders/{id}/submit` | 提交经销订单审核 | `dealer.order.manage` | `dealer.order.submitted` / `dealer.order.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitDealerPriceCatalog` | `POST` | `/api/v1/dealers/price-catalogs/{id}/submit` | 提交价格目录审批 | `dealer.price_catalog.edit` | `dealer.price_catalog.submitted` / `dealer.price_catalog.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitFacilityInspectionRecord` | `POST` | `/api/v1/facility/inspections/tasks/{task_id}/records` | 提交巡检记录 | `facility.inspection.manage` | `facility.inspection_record_submitted` / `facility.inspection_record_submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitInternalPayDayRule` | `POST` | `/api/v1/settlement/internal-pay-days/rules` | 提交三个付账日或钱路指派规则(待另一人审批) | `settlement.config.manage` | `settlement.internal_pay_day_rule.submitted` / `settlement.internal_pay_day_rule.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitMerchantBillDispute` | `POST` | `/api/v1/merchant-billing/bills/{id}/disputes` | 提交账单争议 | `merchant.billing.dispute.submit` | `merchant.billing.dispute.submit` / `merchant.billing.dispute.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitMerchantPortalSettlementDispute` | `POST` | `/api/v1/merchant-portal/settlements/{settlement_id}/dispute` | 商户专区·结算单异议留言 | `merchant.portal.settlement.dispute` | `merchant_portal.settlement.disputed` / `merchant_portal.settlement.dispute_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| P0-待归类 | `submitSiteEntryReport` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/submit` | 外包提交进场报备（进入物业审核） | `facility.site_entry.submit_assigned` | `facility.site_entry.submit_assigned` / `facility.site_entry.submit_failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsBillingRule` | `POST` | `/api/v1/loyalty/ticket-points-billing/rules` | 提交全国积分账期审批 | `ticket.points.billing.manage` | `ticket.points.billing.rule.submitted` / `ticket.points.billing.rule.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsBillObjection` | `POST` | `/api/v1/loyalty/ticket-points-billing/bills/{id}/objections` | 在异议期提交月结异议 | `ticket.points.billing.view` | `ticket.points.billing.objection.submitted` / `ticket.points.billing.objection.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsCalendar` | `POST` | `/api/v1/loyalty/ticket-points-billing/calendars` | 提交实际工作日日历审批 | `ticket.points.billing.manage` | `ticket.points.billing.calendar.submitted` / `ticket.points.billing.calendar.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketPointsOutage` | `POST` | `/api/v1/loyalty/ticket-points-billing/portal-outages` | 提交实际门户中断复核 | `ticket.points.billing.manage` | `ticket.points.billing.outage.submitted` / `ticket.points.billing.outage.submitted.failed` | web | operation 级补充 |
| P0-待归类 | `submitTicketRewardConfig` | `POST` | `/api/v1/loyalty/ticket-reward-configs` | 提交门票奖励规则或销售合同审批 | `ticket.reward.config.manage` | `ticket.reward.config.submit.completed` / `ticket.reward.config.submit.completed.failed` | web | operation 级补充 |
| P0-待归类 | `suspendFacilityWorkOrder` | `POST` | `/api/v1/facility/work-orders/{work_order_id}/suspend` | 挂起工单 | `facility.work_order.manage` | `facility.work_order_suspended` / `facility.work_order_suspend_failed` | web | operation 级补充 |
| P0-待归类 | `swapStaffWorkPhone` | `POST` | `/api/v1/terminals/devices/work-phone-swap` | 夜班值班经理确认同班故障换机 | `terminal.work_phone.swap` | `terminal.work_phone.swapped` / `terminal.work_phone.swap_failed` | web, staff_work_phone | operation 级补充 |
| P0-待归类 | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | 企微会话同步入站（PIPL 合规，开关门控） | `wecom.sync` | `wecom.conversation_synced` / `wecom.conversation_sync_failed` | <br>channel: provider-webhook | operation 级补充 |
| P0-待归类 | `takeOverFacilityAlarm` | `POST` | `/api/v1/facility/alarms/{alarm_id}/take-over` | 设备告警明确接手（已收到并接手） | `facility.alarm.take_over` | `facility.alarm_taken_over` / `facility.alarm_take_over_failed` | web, staff_work_phone | operation 级补充 |
| P0-待归类 | `unarchiveFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}/unarchive` | 取消存档设施设备 | `facility.equipment.manage` | `facility.equipment_unarchived` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `unarchiveMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/unarchive` | 取消存档商户服务工单 | `merchant.service_work_order.close` | `merchant_service.work_order.unarchived` / `not_applicable` | web | operation 级补充 |
| P0-待归类 | `unarchiveServiceCatalogItem` | `POST` | `/api/v1/services/catalog/{id}/unarchive` | 取消存档服务目录项 | `service.catalog.manage` | `service.catalog.unarchived` / `service.catalog.archive_failed` | web | operation 级补充 |
| P0-待归类 | `unbindUserFromPartnerOrg` | `POST` | `/api/v1/partner/orgs/{org_id}/unbind-user` | 账号解绑外部协作机构 | `partner.org.manage` | `partner.org.user_unbound` / `partner.org.user_unbind_failed` | web | operation 级补充 |
| P0-待归类 | `updateDealerContractTerms` | `POST` | `/api/v1/dealers/contracts/{id}/terms` | 合同付款条款与运费模板 | `dealer.manage` | `dealer.contract_terms_updated` / `dealer.contract_terms_update_failed` | web | operation 级补充 |
| P0-待归类 | `updateEnergyMeter` | `POST` | `/api/v1/energy/meters/{meter_id}` | 更新能源表计 | `energy.meter.manage` | `energy.meter_saved` / `energy.meter_save_failed` | web | operation 级补充 |
| P0-待归类 | `updateFacilityEquipment` | `POST` | `/api/v1/facility/equipment/{equipment_id}` | 更新设备台账 | `facility.equipment.manage` | `facility.equipment_updated` / `facility.equipment_update_failed` | web | operation 级补充 |
| P0-待归类 | `updateLeasingLeadStatus` | `POST` | `/api/v1/leasing/leads/{lead_id}/status` | 招商线索状态流转(7 态) | `leasing.lead.manage` | `leasing.lead.status_changed` / `leasing.lead.status_change_failed` | web | operation 级补充 |
| P0-待归类 | `updateMediaPerformanceShow` | `PATCH` | `/api/v1/settlement/media-shows/{id}` | 修改草案合同记录 | `media.performance.contract.manage` | `media_show.updated` / `media_show.update_failed` | web | operation 级补充 |
| P0-待归类 | `uploadDealerDeliveryEvidence` | `POST` | `/api/v1/dealer-portal/orders/{id}/delivery-evidence` | 上传本企业订单交接凭证 | `dealer_portal.order.manage` | `evidence.attachment_uploaded` / `evidence.attachment_upload_failed` | dealer_portal | operation 级补充 |
| P0-待归类 | `uploadSiteEntryAttachment` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/attachments` | 上传本报备真实完工文件 | `facility.site_entry.upload_completion_assigned` | `evidence.attachment_uploaded` / `evidence.attachment_upload_failed` | web | operation 级补充 |
| P0-待归类 | `uploadSiteEntryCompletion` | `POST` | `/api/v1/facility/site-entry-reports/{report_id}/completion` | 外包上传完工资料（待物业验收） | `facility.site_entry.upload_completion_assigned` | `facility.site_entry_completion_uploaded` / `facility.site_entry.upload_completion_failed` | web | operation 级补充 |
| P0-待归类 | `upsertServiceCatalogItem` | `POST` | `/api/v1/services/catalog` | 维护服务目录项(固定参考价) | `service.catalog.manage` | `service.catalog.saved` / `service.catalog.save_failed` | web | operation 级补充 |
| P0-待归类 | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | 拒绝设置企微自动到期策略 | `wecom.retention.manage` | `not_applicable` / `wecom.retention_policy_failed` | web | operation 级补充 |
| P0-待归类 | `verifyDealerOrderPayment` | `POST` | `/api/v1/dealers/payments/{id}/verify` | 财务核验到账 | `dealer.order.payment.verify` | `dealer.order.payment_verified` / `dealer.order.payment_verify_failed` | web | operation 级补充 |
| P0-待归类 | `verifyEnergyReading` | `POST` | `/api/v1/energy/readings/{reading_id}/verification` | 第二人核表并发起真实主管确认 | `energy.reading.manage` | `energy.reading_verification_submitted` / `energy.reading_verification_submitted_failed` | web | operation 级补充 |
| P0-待归类 | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | 分级查看企微会话消息（脱敏/原文，原文需审批+审计） | `wecom.view_raw` | `wecom.raw_viewed` / `wecom.raw_view_failed` | web, mobile_admin | operation 级补充 |
| STAFF-员工 | `addStaffCommission` | `POST` | `/api/v1/staff/payroll/commissions` | 提成入台账(手录/系统写入口) | `staff.payroll.manage` | `staff.payroll.commission_added` / `staff.payroll.commission_add_failed` | web | operation 级补充 |
| STAFF-员工 | `addStaffPayrollAdjustment` | `POST` | `/api/v1/staff/payroll/adjustments` | 发起奖金/处罚复核(必须带事由；HR 会签＋直属领导向上三级通过后入账) | `staff.payroll.manage` | `staff.payroll.adjustment_requested` / `staff.payroll.adjustment_add_failed` | web | operation 级补充 |
| STAFF-员工 | `assignStaffBusinessHandover` | `POST` | `/api/v1/staff/profiles/{id}/handover` | 将一个真实未结事项交给在岗人员 | `staff.profile.review` | `staff.business_handover.assigned` / `staff.business_handover.assign_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` | 按周批量排班(一次排一周) | `staff.shift.manage` | `staff.shift.submitted` / `staff.shift.save_failed` | web | operation 级补充 |
| STAFF-员工 | `closeStaffCommissionSettlement` | `POST` | `/api/v1/staff/commission-settlements/close` | 提成月结拨付（酒吧 → 人员所属主体） | `staff.payroll.manage` | `staff.commission_settlement.closed` / `staff.commission_settlement.close_failed` | web | operation 级补充 |
| STAFF-员工 | `closeStaffPayrollMonth` | `POST` | `/api/v1/staff/payroll/close` | 旧一键月结入口已关闭 | `staff.payroll.manage` | `not_applicable` / `staff.payroll.month_close_failed` | web | operation 级补充 |
| STAFF-员工 | `coverStaffShift` | `POST` | `/api/v1/staff/shifts/{id}/cover` | 记录临时顶班 | `staff.shift.manage` | `staff.shift.covered` / `staff.shift.cover_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `createStaffDispatch` | `POST` | `/api/v1/staff/dispatches` | 登记派驻记录（员工 → 派驻酒吧） | `staff.profile.review` | `staff.dispatch.created` / `staff.dispatch.create_failed` | web | operation 级补充 |
| STAFF-员工 | `draftPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments` | 保存专业税务认定草案 | `staff.tax_bracket.manage` | `staff.payroll.tax.assessment_drafted` / `staff.payroll.tax.assessment_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `draftPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions` | 保存完整税表草案 | `staff.tax_bracket.manage` | `staff.payroll.tax.version_drafted` / `staff.payroll.tax.version_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `endStaffDispatch` | `POST` | `/api/v1/staff/dispatches/{dispatch_id}/end` | 结束派驻记录 | `staff.profile.review` | `staff.dispatch.ended` / `staff.dispatch.end_failed` | web | operation 级补充 |
| STAFF-员工 | `getCurrentStaffWorkPhoneContext` | `GET` | `/api/v1/staff/attendance/work-phone-context` | 读取本人工作手机当前领用记录 | `staff.attendance.punch` | `staff.attendance.context_viewed` / `staff.attendance.context_view_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `getMyStaffProfile` | `GET` | `/api/v1/staff/profiles/mine` | 查看本人员工档案(员工手机) | `staff.profile.self` | `not_applicable` / `not_applicable` | staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `getStaffBusinessHandover` | `GET` | `/api/v1/staff/profiles/{id}/handover` | 查看停离职状态与真实未结业务 | `staff.profile.review` | `staff.business_handover.viewed` / `staff.business_handover.view_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `getStaffPenaltyCap` | `GET` | `/api/v1/staff/settings/payroll-penalty-cap` | 读取普通扣除上限 | `staff.payroll.manage` | `staff.payroll.penalty_cap_viewed` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `getStaffShiftVisibility` | `GET` | `/api/v1/staff/settings/shift-visible-days` | 读取排班可见天数 | `staff.shift.manage` | `staff.shift.visibility_viewed` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `issueStaffLockerScanToken` | `POST` | `/api/v1/staff/lockers/scan-token` | 申请寄存柜扫码凭证 | `staff.profile.self` | `staff.locker.scan_token_issued` / `staff.locker.scan_token_failed` | staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `issueStaffPhoneTimeAnchor` | `POST` | `/api/v1/staff/attendance/time-anchors` | 为本人工作手机签发班次时间锚 | `staff.attendance.punch` | `staff.attendance.anchor_issued` / `staff.attendance.anchor_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `listMyStaffAttendance` | `GET` | `/api/v1/staff/attendance/mine` | 我的打卡记录(最近30天) | `staff.shift.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| STAFF-员工 | `listMyStaffLeaves` | `GET` | `/api/v1/staff/leaves/mine` | 我的请假(员工手机) | `staff.leave.submit` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listMyStaffPayroll` | `GET` | `/api/v1/staff/payroll/mine` | 我的薪资(员工手机;仅已月结月份) | `staff.payroll.view_mine` | `not_applicable` / `not_applicable` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_private_phone, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `listMyStaffPayrollCommissions` | `GET` | `/api/v1/staff/payroll/mine/commissions` | 我的提成明细(员工手机) | `staff.payroll.view_mine` | `not_applicable` / `not_applicable` | staff_private_phone, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listMyStaffShifts` | `GET` | `/api/v1/staff/shifts/mine` | 我的班次与点位(员工手机) | `staff.shift.view` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listPayrollTaxAssessments` | `GET` | `/api/v1/staff/payroll/tax-assessments` | 读取员工月份税务认定历史 | `staff.payroll.manage` | `staff.payroll.tax.assessments_viewed` / `staff.payroll.tax.assessments_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `listPayrollTaxBrackets` | `GET` | `/api/v1/staff/payroll/tax-brackets` | 查询旧单档税表历史 | `staff.payroll.manage` | `staff.payroll.tax_bracket_viewed` / `staff.payroll.tax_bracket_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `listPayrollTaxVersions` | `GET` | `/api/v1/staff/payroll/tax-versions` | 读取完整薪资税表版本 | `staff.payroll.manage` | `staff.payroll.tax.versions_viewed` / `staff.payroll.tax.versions_view_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `listStaffAdjustmentAppeals` | `GET` | `/api/v1/staff/payroll/adjustments/appeals` | 扣罚申诉清单(本人/人事按员工) | `staff.payroll.view_mine` | `staff.payroll.adjustment_appeals_viewed` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffAttendance` | `GET` | `/api/v1/staff/attendance` | 按日查考勤(后台/主管) | `staff.attendance.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| STAFF-员工 | `listStaffAttendanceReceiptReviews` | `GET` | `/api/v1/staff/attendance/receipt-events/{id}/reviews` | 查看领取时间核实记录 | `staff.shift.view` | `staff.attendance.receipt_review_viewed` / `staff.attendance.receipt_review_view_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffCommissionSettlements` | `GET` | `/api/v1/staff/commission-settlements` | 查询提成月结拨付记录（按月） | `staff.payroll.manage` | `staff.commission_settlement.viewed` / `not_applicable` | web | operation 级补充 |
| STAFF-员工 | `listStaffDispatches` | `GET` | `/api/v1/staff/dispatches` | 查询派驻记录 | `staff.profile.view` | `staff.dispatch.viewed` / `not_applicable` | web, mobile_admin | operation 级补充 |
| STAFF-员工 | `listStaffLeaves` | `GET` | `/api/v1/staff/leaves` | 查请假(主管/后台,按状态) | `staff.shift.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| STAFF-员工 | `listStaffPayroll` | `GET` | `/api/v1/staff/payroll` | 全员薪资单(人事,按月) | `staff.payroll.manage` | `not_applicable` / `not_applicable` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `listStaffPhoneReceipts` | `GET` | `/api/v1/staff/attendance/receipt-events` | 查询工作手机领取事件和待核事实 | `staff.shift.view` | `staff.attendance.receipts_viewed` / `staff.attendance.receipts_view_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffRegistrationCompanies` | `GET` | `/api/v1/staff/registration-companies` | 获取可注册公司 | `public` | `staff.registration_companies_viewed` / `not_applicable` | staff_private_phone | operation 级补充 |
| STAFF-员工 | `listStaffShiftCovers` | `GET` | `/api/v1/staff/shifts/{id}/covers` | 查看班次顶班记录 | `staff.shift.manage` | `staff.shift.covers_viewed` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffShiftHistory` | `GET` | `/api/v1/staff/shifts/{id}/history` | 查看排班修改历史 | `staff.shift.manage` | `staff.shift.history_viewed` / `not_applicable` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `listStaffShifts` | `GET` | `/api/v1/staff/shifts` | 查排班(后台/主管) | `staff.shift.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| STAFF-员工 | `listStaffWorkPhoneContextsForRecovery` | `GET` | `/api/v1/staff/attendance/work-phone-contexts` | 主管查询本工作手机指定员工的历史领用记录 | `staff.shift.manage` | `staff.attendance.contexts_viewed` / `staff.attendance.contexts_view_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `markStaffPayrollPaidLegacy` | `POST` | `/api/v1/staff/payroll/pay` | 旧手填已发工资入口已关闭 | `staff.payroll.manage` | `not_applicable` / `staff.payroll.pay_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `myStaffAttendanceMonthView` | `GET` | `/api/v1/staff/attendance/month-view` | 考勤月视图(员工手机,逐日六态) | `staff.shift.view` | `not_applicable` / `not_applicable` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `punchStaffAttendance` | `POST` | `/api/v1/staff/attendance/punch` | 考勤打卡(NFC/扫码) | `staff.attendance.punch` | `staff.attendance.punched` / `staff.attendance.punch_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `recordStaffLockerScan` | `POST` | `/api/v1/terminals/kiosks/{device_id}/locker-events` | 柜机上报寄存柜扫码存取 | `not_applicable` | `staff.locker.recorded` / `staff.locker.record_failed` | kiosk | operation 级补充 |
| STAFF-员工 | `recordStaffPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events` | 提交持久化工作手机领取事件 | `staff.attendance.punch` | `staff.attendance.receipt_recorded` / `staff.attendance.receipt_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `recoverStaffWorkPhoneReceipt` | `POST` | `/api/v1/staff/attendance/receipt-events/recover` | 主管代补报原工作手机未传领取事件 | `staff.shift.manage` | `staff.attendance.receipt_recovered` / `staff.attendance.receipt_recover_failed` | staff_work_phone | operation 级补充 |
| STAFF-员工 | `requestStaffLeaveRevoke` | `POST` | `/api/v1/staff/leaves/{id}/revoke-request` | 申请销假(员工本人,手机端) | `staff.leave.submit` | `staff.leave.revoke_requested` / `staff.leave.revoke_request_failed` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `requestStaffPenaltyCap` | `POST` | `/api/v1/staff/settings/payroll-penalty-cap` | 申请调整普通扣除上限 | `staff.payroll.manage` | `staff.payroll.penalty_cap_requested` / `staff.payroll.penalty_cap_request_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `reviewStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/review` | 请假审批(排班主管) | `staff.shift.manage` | `staff.leave.reviewed` / `staff.leave.review_failed` | web | operation 级补充 |
| STAFF-员工 | `revokeStaffLeave` | `POST` | `/api/v1/staff/leaves/{id}/revoke` | 销假审批(排班主管):批准或驳回员工本人的销假申请 | `staff.shift.manage` | `staff.leave.revoke_reviewed` / `staff.leave.revoke_review_failed` | web | operation 级补充 |
| STAFF-员工 | `setStaffBaseSalary` | `POST` | `/api/v1/staff/payroll/base` | 旧固定工资直写入口已关闭 | `staff.payroll.manage` | `not_applicable` / `staff.payroll.base_save_failed` | web | operation 级补充 |
| STAFF-员工 | `setStaffEmploymentStatus` | `POST` | `/api/v1/staff/profiles/{id}/employment-status` | 发起员工任职状态变更复核 | `staff.profile.review` | `staff.profile.employment_status_change_requested` / `staff.profile.employment_status_change_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `setStaffShiftVisibility` | `POST` | `/api/v1/staff/settings/shift-visible-days` | 设置排班可见天数 | `staff.shift.manage` | `staff.shift.visibility_changed` / `staff.shift.visibility_change_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `staffPayrollCalculate` | `POST` | `/api/v1/staff/payroll/calculations` | 生成完整工资税前原件 | `staff.payroll.manage` | `staff.payroll.calculate` / `staff.payroll.calculate_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollCalculationList` | `GET` | `/api/v1/staff/payroll/calculations` | 读取完整工资税前核算原件 | `staff.payroll.manage` | `staff.payroll.calculation_list` / `staff.payroll.calculation_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollCalendarDraft` | `POST` | `/api/v1/staff/payroll/calendars` | 保存工资日历草案 | `staff.payroll.manage` | `staff.payroll.calendar_draft` / `staff.payroll.calendar_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollCalendarList` | `GET` | `/api/v1/staff/payroll/calendars` | 读取工资日历 | `staff.payroll.manage` | `staff.payroll.calendar_list` / `staff.payroll.calendar_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollCalendarSubmit` | `POST` | `/api/v1/staff/payroll/calendars/{id}/submit` | 提交工资日历审批 | `staff.payroll.manage` | `staff.payroll.calendar_submit` / `staff.payroll.calendar_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollDetail` | `GET` | `/api/v1/staff/payroll/statements/{id}` | 读取工资逐项、补差、实际付款及未结事项 | `staff.payroll.manage` | `staff.payroll.detail` / `staff.payroll.detail_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollDetailMine` | `GET` | `/api/v1/staff/payroll/mine/statements/{id}` | 读取本人完整工资详情 | `staff.payroll.view_mine` | `staff.payroll.detail_mine` / `staff.payroll.detail_mine_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_private_phone, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollPeriodDraft` | `POST` | `/api/v1/staff/payroll/period-facts` | 保存期间计薪事实草案 | `staff.payroll.manage` | `staff.payroll.period_draft` / `staff.payroll.period_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollPeriodList` | `GET` | `/api/v1/staff/payroll/period-facts` | 读取期间计薪事实 | `staff.payroll.manage` | `staff.payroll.period_list` / `staff.payroll.period_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollPeriodSubmit` | `POST` | `/api/v1/staff/payroll/period-facts/{id}/submit` | 提交期间计薪事实审批 | `staff.payroll.manage` | `staff.payroll.period_submit` / `staff.payroll.period_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollRuleDraft` | `POST` | `/api/v1/staff/payroll/performance-rules` | 保存经营主体扣分分值表草案 | `staff.payroll.manage` | `staff.payroll.rule_draft` / `staff.payroll.rule_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollRuleList` | `GET` | `/api/v1/staff/payroll/performance-rules` | 读取经营主体扣分分值表 | `staff.payroll.manage` | `staff.payroll.rule_list` / `staff.payroll.rule_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollRuleSubmit` | `POST` | `/api/v1/staff/payroll/performance-rules/{id}/submit` | 提交经营主体扣分分值表审批 | `staff.payroll.manage` | `staff.payroll.rule_submit` / `staff.payroll.rule_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollSchemeDraft` | `POST` | `/api/v1/staff/payroll/schemes` | 保存薪资方案草案 | `staff.payroll.manage` | `staff.payroll.scheme_draft` / `staff.payroll.scheme_draft_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollSchemeList` | `GET` | `/api/v1/staff/payroll/schemes` | 读取薪资方案 | `staff.payroll.manage` | `staff.payroll.scheme_list` / `staff.payroll.scheme_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollSchemeSubmit` | `POST` | `/api/v1/staff/payroll/schemes/{id}/submit` | 提交薪资方案审批 | `staff.payroll.manage` | `staff.payroll.scheme_submit` / `staff.payroll.scheme_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollScoreAppeal` | `POST` | `/api/v1/staff/payroll/performance-adjustments/{id}/appeals` | 本人发起绩效扣分申诉 | `staff.payroll.view_mine` | `staff.payroll.score_appeal` / `staff.payroll.score_appeal_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_private_phone, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollScoreDeduct` | `POST` | `/api/v1/staff/payroll/performance-adjustments` | 按批准分值表发起绩效扣分 | `staff.payroll.manage` | `staff.payroll.score_deduct` / `staff.payroll.score_deduct_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollScoreList` | `GET` | `/api/v1/staff/payroll/performance-adjustments` | 读取员工绩效扣分及冲回 | `staff.payroll.manage` | `staff.payroll.score_list` / `staff.payroll.score_list_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollScoreMine` | `GET` | `/api/v1/staff/payroll/mine/performance-adjustments` | 读取本人绩效扣分及冲回 | `staff.payroll.view_mine` | `staff.payroll.score_mine` / `staff.payroll.score_mine_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_private_phone, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `staffPayrollSettle` | `POST` | `/api/v1/staff/payroll/settlements` | 引用批准专业税额定格工资 | `staff.payroll.manage` | `staff.payroll.settle` / `staff.payroll.settle_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `submitPayrollTaxAssessment` | `POST` | `/api/v1/staff/payroll/tax-assessments/{id}/submit` | 提交专业税务认定审批 | `staff.tax_bracket.manage` | `staff.payroll.tax.assessment_submitted` / `staff.payroll.tax.assessment_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `submitPayrollTaxVersion` | `POST` | `/api/v1/staff/payroll/tax-versions/{id}/submit` | 提交税表财税法及最高确认 | `staff.tax_bracket.manage` | `staff.payroll.tax.version_submitted` / `staff.payroll.tax.version_submit_failed` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `submitStaffAdjustmentAppeal` | `POST` | `/api/v1/staff/payroll/adjustments/{id}/appeals` | 员工对已生效扣罚提出申诉 | `staff.payroll.view_mine` | `staff.payroll.adjustment_appeal_submitted` / `staff.payroll.adjustment_appeal_submit_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `submitStaffAttendanceReceiptReview` | `POST` | `/api/v1/staff/attendance/receipt-events/{id}/review` | 提交工作手机领取时间核实 | `staff.shift.manage` | `staff.attendance.receipt_review_submitted` / `staff.attendance.receipt_review_submit_failed` | web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `submitStaffLeave` | `POST` | `/api/v1/staff/leaves` | 提交请假(员工手机) | `staff.leave.submit` | `staff.leave.submitted` / `staff.leave.submit_failed` | staff_private_phone, web, staff_work_phone | operation 级补充 |
| STAFF-员工 | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | 公司员工本人注册 | `public` | `staff.self_registered` / `staff.self_register_failed` | staff_private_phone | operation 级补充 |
| STAFF-员工 | `upsertPayrollTaxBracket` | `POST` | `/api/v1/staff/payroll/tax-brackets` | 明确拒绝旧单档税表写入 | `staff.tax_bracket.manage` | `not_applicable` / `staff.payroll.tax.single_bracket_write_rejected` | app, dealer_portal, kiosk, merchant_app, merchant_portal, miniapp, mobile_admin, staff_work_phone, web | operation 级补充 |
| STAFF-员工 | `upsertStaffShift` | `POST` | `/api/v1/staff/shifts` | 主管排班(双端) | `staff.shift.manage` | `staff.shift.submitted` / `staff.shift.save_failed` | web, staff_private_phone, staff_work_phone | operation 级补充 |
| SYS-系统健康 | `getSystemHealth` | `GET` | `/api/v1/system/health` | 健康检查 | `public` | `not_applicable` / `not_applicable` | web, mobile_admin<br>channel: monitoring | operation 级补充 |
| SYS-系统健康 | `getSystemVersion` | `GET` | `/api/v1/system/version` | 版本和构建信息 | `system.version.view` | `not_applicable` / `not_applicable` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `addTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-rollouts/{id}/targets` | 加入灰度白名单 | `terminal.switch.manage` | `terminal_canary.target_added` / `terminal_canary.target_add_failed` | web | operation 级补充 |
| TERM-终端设备 | `archiveTerminalCanaryTarget` | `POST` | `/api/v1/terminals/canary-targets/{id}/archive` | 移出灰度白名单（软删除） | `terminal.switch.manage` | `terminal_canary.target_archived` / `terminal_canary.target_archive_failed` | web | operation 级补充 |
| TERM-终端设备 | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | 设备绑定 | `terminal.device.bind` | `terminal.device.bound` / `terminal.device.bind_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `changeTerminalDeviceResponsibleStaff` | `POST` | `/api/v1/terminals/devices/{id}/responsible-staff` | 入场三道环节设备与其他现场设备更换责任人 | `terminal.device.lifecycle` | `terminal.device.responsible_staff_changed` / `terminal.device.responsible_staff_change_failed` | web | operation 级补充 |
| TERM-终端设备 | `completeTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair-complete` | 设备修复完成 | `terminal.device.lifecycle` | `terminal.device.repair_completed` / `terminal.device.repair_complete_failed` | web | operation 级补充 |
| TERM-终端设备 | `detectMdmViolations` | `POST` | `/api/v1/terminals/mdm/violations/detect` | 服务端检测设备违规 | `mdm.device.manage` | `device.policy_violation` / `device.policy_violation_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `enrollMdmDevice` | `POST` | `/api/v1/terminals/mdm/devices/enroll` | MDM 设备纳管/策略同步 | `mdm.device.manage` | `mdm.policy_checked` / `mdm.policy_check_failed` | web | operation 级补充 |
| TERM-终端设备 | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | 一体机配置，含售票型与非售票型模式 | `not_applicable` | `terminal.kiosk_config_viewed` / `terminal.access_rejected` | kiosk | operation 级补充 |
| TERM-终端设备 | `handleMdmViolation` | `POST` | `/api/v1/terminals/mdm/violations/{id}/handle` | 违规处置闭环 | `mdm.violation.handle` | `mdm.violation.handled` / `mdm.violation.handle_failed` | web | operation 级补充 |
| TERM-终端设备 | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | 一体机心跳 | `not_applicable` | `terminal.kiosk_heartbeat` / `terminal.kiosk_heartbeat_failed` | kiosk | operation 级补充 |
| TERM-终端设备 | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | 查询端能力开关 | `terminal.switch.view` | `terminal_switch.viewed` / `terminal_switch.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmAppWhitelist` | `GET` | `/api/v1/terminals/mdm/app-whitelist` | 查询 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.viewed` / `mdm.app_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | 查询 MDM 设备违规留痕 | `mdm.violation.view` | `mdm.violation.viewed` / `mdm.violation.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmSiteWhitelist` | `GET` | `/api/v1/terminals/mdm/site-whitelist` | 查询 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.viewed` / `mdm.site_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listTerminalCanaryRollouts` | `GET` | `/api/v1/terminals/canary-rollouts` | 查询终端开关灰度试点 | `terminal.switch.view` | `terminal_canary.viewed` / `terminal_canary.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listTerminalDevices` | `GET` | `/api/v1/terminals/devices` | 终端设备台账 | `terminal.switch.view` | `terminal.device.viewed` / `terminal.device.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceArchived` | `POST` | `/api/v1/terminals/devices/{id}/archive` | 报废设备归档 | `terminal.device.lifecycle` | `terminal.device.archived` / `terminal.device.archive_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceFound` | `POST` | `/api/v1/terminals/devices/{id}/found` | 标记设备找回 | `terminal.device.lifecycle` | `terminal.device.found` / `terminal.device.found_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceLost` | `POST` | `/api/v1/terminals/devices/{id}/lost` | 标记设备挂失 | `terminal.device.lifecycle` | `terminal.device.lost` / `terminal.device.lost_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceRepair` | `POST` | `/api/v1/terminals/devices/{id}/repair` | 设备送修 | `terminal.device.lifecycle` | `terminal.device.repaired` / `terminal.device.repair_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `markTerminalDeviceScrap` | `POST` | `/api/v1/terminals/devices/{id}/scrap` | 设备报废 | `terminal.device.lifecycle` | `terminal.device.scrapped` / `terminal.device.scrap_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `registerEntryDevice` | `POST` | `/api/v1/terminals/devices/entry-devices` | 后台登记设备进台账（入场三道环节设备、扫码设备、其他现场设备） | `terminal.device.lifecycle` | `terminal.device.entry_device_registered` / `terminal.device.entry_device_register_failed` | web | operation 级补充 |
| TERM-终端设备 | `registerTerminalDeviceSigningKey` | `POST` | `/api/v1/terminals/devices/{id}/signing-key` | 登记或轮换已绑定设备签名公钥 | `terminal.device.bind` | `terminal.device.signing_key_registered` / `terminal.device.signing_key_register_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | 上报 MDM 设备违规 | `mdm.device.manage` | `device.policy_violation` / `device.policy_violation_failed` | web | operation 级补充 |
| TERM-终端设备 | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | 归还终端设备 | `terminal.device.bind` | `terminal.device.returned` / `terminal.device.returned_failed` | staff_work_phone, kiosk, web | operation 级补充 |
| TERM-终端设备 | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | 设备解绑 | `terminal.device.unbind` | `terminal.device.unbound` / `terminal.device.unbind_failed` | web | operation 级补充 |
| TERM-终端设备 | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | 修改端能力开关 | `terminal.switch.manage` | `terminal_switch.changed` / `terminal_switch.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `updateTerminalDeviceMerchant` | `PUT` | `/api/v1/terminals/devices/{id}/merchant` | 设备商户归属设置/更换/解除 | `terminal.device.lifecycle` | `terminal.device.merchant_changed` / `terminal.device.merchant_change_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `upsertMdmAppWhitelist` | `PUT` | `/api/v1/terminals/mdm/app-whitelist/{id}` | 新增或更新 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.changed` / `mdm.app_whitelist.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertMdmSiteWhitelist` | `PUT` | `/api/v1/terminals/mdm/site-whitelist/{id}` | 新增或更新 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.changed` / `mdm.site_whitelist.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertTerminalCanaryRollout` | `PUT` | `/api/v1/terminals/canary-rollouts` | 开启或结束灰度试点 | `terminal.switch.manage` | `terminal_canary.rollout_changed` / `terminal_canary.rollout_change_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-approve` | 审批权益模板删除申请 | `ticket.entitlement.approve` | `ticket_entitlement_template.delete_approved` / `ticket_entitlement_template.delete_approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-approve` | 审批价格规则删除申请 | `ticket.price_rule.approve` | `ticket_price_variant.delete_approved` / `ticket_price_variant.delete_approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-approve` | 审批票种删除申请 | `ticket.type.approve` | `ticket_product.delete_approved` / `ticket_product.delete_approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | 退款审批 | `ticket.refund.approve` | `ticket_refund.approved` / `ticket_refund.approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events/{safety_event_id}/approve` | 批准通票安全事件 | `ticket.refund.approve` | `ticket_safety_event.approved` / `ticket_safety_event.approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `archiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/archive` | 存档权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.archived` / `ticket_entitlement_template.archive_failed` | web | operation 级补充 |
| TIC-票务权益 | `archiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/archive` | 存档价格规则 | `ticket.price_variant.manage` | `ticket_price_variant.archived` / `ticket_price_variant.archive_failed` | web | operation 级补充 |
| TIC-票务权益 | `archiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/archive` | 存档票种 | `ticket.product.manage` | `ticket_product.archived` / `ticket_product.archive_failed` | web | operation 级补充 |
| TIC-票务权益 | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | 绑定票类型权益 | `ticket.product.manage` | `ticket_product_entitlement.bound` / `ticket_product_entitlement.bound_failed` | web | operation 级补充 |
| TIC-票务权益 | `cancelTicketOrder` | `POST` | `/api/v1/ticketing/orders/{order_id}/cancel` | 取消未支付票务订单（回补库存） | `ticket.order.create` | `ticket_order.cancelled` / `ticket_order.cancel_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `confirmTicketOrderPayment` | `POST` | `/api/v1/ticketing/orders/{order_id}/confirm-payment` | 确认票务订单支付成功（回写 paid） | `finance.payment.register` | `ticket_order.payment_confirmed` / `ticket_order.payment_confirm_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `confirmTicketRefundPayout` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/confirm-payout` | 退款确认拨付 | `ticket.refund.approve` | `ticket_refund.payout_confirmed` / `ticket_refund.payout_confirm_failed` | web | operation 级补充 |
| TIC-票务权益 | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | 创建权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.changed` / `ticket_entitlement_template.changed_failed` | web | operation 级补充 |
| TIC-票务权益 | `createPartnerVoucherConfig` | `POST` | `/api/v1/ticketing/partner-vouchers/configs` | 配置通票合作方券结算口径 | `ticket.entitlement.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `createTicketDistributionMerchantRate` | `POST` | `/api/v1/ticketing/distribution-merchant-rates` | 配置票务分销商户费率（每客单价或固定额） | `ticket.price_variant.manage` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `createTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules` | Create P2 ticket dynamic pricing rule draft | `ticket.dynamic_pricing.manage` | `ticket_dynamic_pricing.created.success` / `ticket_dynamic_pricing.created.failure` | web | operation 级补充 |
| TIC-票务权益 | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | 创建票务订单（含一体机辅助售票） | `ticket.order.create` | `ticket_order.created` / `ticket_order.create_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketOrderPayIntent` | `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | 创建票务支付意图（含一体机辅助售票） | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | 创建平日、周末、节假日及派生价规则 | `ticket.price_variant.manage` | `ticket_price_variant.changed` / `ticket_price_variant.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | 创建票种 | `ticket.product.manage` | `ticket_product.changed` / `ticket_product.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | 正式核销 | `verification.execute` | `verification.succeeded` / `verification.failed` | staff_work_phone, kiosk<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `getTicketCredential` | `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | 获取动态二维码凭证 | `ticket.credential.view` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | web, miniapp, app | operation 级补充 |
| TIC-票务权益 | `getTicketExpiryReminderPolicy` | `GET` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | 查未核销票到期提醒提前小时数（按项目） | `ticket.product.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `getTicketQrRefreshConfig` | `GET` | `/api/v1/ticketing/qr-refresh-config` | 读全场统一动态码刷新间隔 | `ticket.product.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `importPartnerVoucherRedemption` | `POST` | `/api/v1/ticketing/partner-vouchers/redemptions` | 导入合作方券核销回流 | `verification.execute` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | 出票（含一体机辅助售票） | `ticket.ticket.issue` | `ticket.issued` / `ticket.issue_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | 查询权益模板 | `ticket.entitlement.view` | `ticket_entitlement_template.viewed` / `ticket_entitlement_template.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | 查询已发放权益 | `ticket.verify.view` | `ticket_issued_entitlement.viewed` / `ticket_issued_entitlement.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketDynamicPricingRules` | `GET` | `/api/v1/ticketing/dynamic-pricing-rules` | List P2 ticket dynamic pricing rules | `ticket.dynamic_pricing.view` | `ticket_dynamic_pricing.viewed.success` / `ticket_dynamic_pricing.viewed.failure` | web | operation 级补充 |
| TIC-票务权益 | `listTicketInstances` | `GET` | `/api/v1/ticketing/ticket-instances` | 查询父票实例 | `ticket.verify.view` | `ticket_instance.viewed` / `ticket_instance.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketOrders` | `GET` | `/api/v1/ticketing/orders` | 查询票务订单 | `ticket.order.read` | `ticket_order.viewed` / `ticket_order.viewed_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | 查询平日、周末、节假日及派生价规则 | `ticket.price_variant.view` | `ticket_price_variant.viewed` / `ticket_price_variant.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketRefundLines` | `GET` | `/api/v1/ticketing/refunds/{refund_id}/lines` | 查询退票明细行 | `ticket.refund.view` | `ticket.refund_line.list` / `ticket.refund_line.list_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketRefunds` | `GET` | `/api/v1/ticketing/refunds` | 查询退款单列表 | `ticket.refund.view` | `ticket_refund.viewed` / `ticket_refund.view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| TIC-票务权益 | `listTicketSafetyEvents` | `GET` | `/api/v1/ticketing/safety-events` | 查询通票安全事件 | `ticket.refund.view` | `ticket_safety_event.viewed` / `ticket_safety_event.view_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| TIC-票务权益 | `listTicketTransfers` | `GET` | `/api/v1/ticketing/ticket-transfers` | 查询门票转赠记录(客服纠纷排查,只读) | `ticket.transfer.read` | `ticket_transfer.viewed` / `ticket_transfer.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketTypeGiftCoverage` | `GET` | `/api/v1/ticketing/ticket-types/{id}/gift-coverage` | 票面赠酒清单（按酒吧分组） | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | 票种查询 | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | 查询票核销记录 | `verification.view` | `verification.viewed` / `verification.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| TIC-票务权益 | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | 票务核销预览 | `verification.execute` | `verification.previewed` / `verification.failed` | staff_work_phone, kiosk, web<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `publishTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules/{id}/publish` | Publish P2 ticket dynamic pricing rule | `ticket.dynamic_pricing.publish` | `ticket_dynamic_pricing.published.success` / `ticket_dynamic_pricing.published.failure` | web | operation 级补充 |
| TIC-票务权益 | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | 发布票种 | `ticket.product.manage` | `ticket_price.published` / `ticket_price.publish_failed` | web | operation 级补充 |
| TIC-票务权益 | `raiseTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verifications/{id}/anomaly` | 核销提起异常复核 | `verification.execute` | `ticket.verification_anomaly_raised` / `ticket.verification_anomaly_raise_failed` | web | operation 级补充 |
| TIC-票务权益 | `rejectTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/reject` | 退款驳回 | `ticket.refund.approve` | `ticket_refund.rejected` / `ticket_refund.reject_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestDeleteEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/delete-request` | 发起权益模板删除申请 | `ticket.entitlement.manage` | `ticket_entitlement_template.delete_requested` / `ticket_entitlement_template.delete_request_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestDeleteTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/delete-request` | 发起价格规则删除申请 | `ticket.price_variant.manage` | `ticket_price_variant.delete_requested` / `ticket_price_variant.delete_request_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestDeleteTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/delete-request` | 发起票种删除申请 | `ticket.product.manage` | `ticket_product.delete_requested` / `ticket_product.delete_request_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestKioskBindCode` | `POST` | `/api/v1/ticketing/kiosk-bind-codes` | 一体机辅助购票给客人手机号发绑定验证码 | `ticket.order.create` | `customer.otp_requested` / `customer.otp_request_failed` | web, kiosk | operation 级补充 |
| TIC-票务权益 | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | 退款或撤销申请 | `ticket.refund.request` | `ticket_refund.requested` / `ticket_refund.request_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| TIC-票务权益 | `requestTicketSafetyEvent` | `POST` | `/api/v1/ticketing/safety-events` | 登记通票安全事件 | `ticket.refund.request` | `ticket_safety_event.requested` / `ticket_safety_event.request_failed` | web, merchant_portal, merchant_app | operation 级补充 |
| TIC-票务权益 | `resolveTicketVerificationAnomaly` | `POST` | `/api/v1/ticketing/verification-anomalies/{id}/resolve` | 主管处置核销异常 | `ticket.verify.anomaly.resolve` | `ticket.verification_anomaly_resolved` / `ticket.verification_anomaly_resolve_failed` | web | operation 级补充 |
| TIC-票务权益 | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | 核销冲正 | `ticket.verify.reverse` | `ticket.verification_reversed` / `ticket.verification_reverse_failed` | web, staff_work_phone, mobile_admin | operation 级补充 |
| TIC-票务权益 | `saveTicketExpiryReminderPolicy` | `PUT` | `/api/v1/ticketing/expiry-reminder-policies/{project_id}` | 配置未核销票到期提醒提前小时数及上下限（按项目） | `ticket.product.manage` | `ticket.expiry_reminder_policy_updated` / `ticket.expiry_reminder_policy_update_failed` | web | operation 级补充 |
| TIC-票务权益 | `simulateTicketDynamicPricing` | `POST` | `/api/v1/ticketing/dynamic-pricing/simulations` | Simulate P2 ticket dynamic pricing without creating an order | `ticket.dynamic_pricing.simulate` | `ticket_dynamic_pricing.simulated.success` / `ticket_dynamic_pricing.simulated.failure` | web | operation 级补充 |
| TIC-票务权益 | `ticketTypePriceCheck` | `GET` | `/api/v1/ticketing/ticket-types/{id}/price-check` | 票种定价核算（系统算出售价 vs 填的售价） | `ticket.product.view` | `not_applicable` / `not_applicable` | web | operation 级补充 |
| TIC-票务权益 | `unarchiveEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates/{id}/unarchive` | 取消存档权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.unarchived` / `ticket_entitlement_template.unarchive_failed` | web | operation 级补充 |
| TIC-票务权益 | `unarchiveTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules/{id}/unarchive` | 取消存档价格规则 | `ticket.price_variant.manage` | `ticket_price_variant.unarchived` / `ticket_price_variant.unarchive_failed` | web | operation 级补充 |
| TIC-票务权益 | `unarchiveTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/unarchive` | 取消存档票种 | `ticket.product.manage` | `ticket_product.unarchived` / `ticket_product.unarchive_failed` | web | operation 级补充 |
| TIC-票务权益 | `updateTicketQrRefreshConfig` | `PUT` | `/api/v1/ticketing/qr-refresh-config` | 改全场统一动态码刷新间隔 | `ticket.product.manage` | `ticket.qr_refresh_config.updated` / `ticket.qr_refresh_config.update_failed` | web | operation 级补充 |
| TIC-票务权益 | `updateTicketTypePricing` | `PUT` | `/api/v1/ticketing/ticket-types/{id}/pricing` | 改票种售价／折扣（凑整误差闸） | `ticket.product.manage` | `ticket_product.pricing_changed` / `ticket_product.pricing_change_failed` | web | operation 级补充 |

## 6. OpenAPI 示例覆盖执行口径

当前 OpenAPI 已有 P0 首批高风险样例覆盖：

| 类型 | 已覆盖 |
|---|---|
| request body | Webhook, CustomerTicketOrderRequest, Verification, CustomerBarReservationRequest, BarOrderRequest, PaymentIntent, AmusementSafetyActionRequest, ProviderAdapterTestRequest |
| response | Success, Page, WebhookAccepted |

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
| 售票型与非售票型一体机能力混用 | 按 `kiosk_mode`、设备绑定和能力开关拒绝（服务端按可信终端判，一体机售票只认启用售票的一体机）；`x-terminal-allowed` 是契约声明，列出可调用该接口的终端，运行时不逐接口按它拦 |
| 旧路径表格与 OpenAPI 冲突 | 旧路径表格只作为历史能力名，正式实现以 OpenAPI 和本文为准 |

版本说明：本文档版本与 `docs/openapi/openapi-p0.json` 的 `info.version` 同步，契约变更即版本递增；不另维护变更记录表（历史见 OpenAPI 与 git 记录）。
