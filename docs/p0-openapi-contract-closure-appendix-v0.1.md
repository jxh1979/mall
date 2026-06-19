# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.1

## 1. 文档定位

本文档由 `docs/openapi/openapi-p0.json` 反向生成，用于收口本轮文档审计发现的三个执行缺口：

1. OpenAPI 已声明的权限码没有全部进入人工权限矩阵。
2. OpenAPI 已声明的成功/失败事件没有全部进入人工事件证据矩阵。
3. P0 任务文档不能逐 operation 追溯到权限、事件、终端和验收责任。

本文不是新需求池，不改变已拍板业务口径。若本文与历史 PRD、历史接口表或旧能力路径冲突，以 `00-current-effective-document-entry-v0.1.md`、`docs/openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 和本文为准。

## 2. 收口结论

| 检查项 | 当前结果 | 执行结论 |
|---|---:|---|
| OpenAPI operation 数 | 282 | 全部必须进入 Java Controller、DTO、Service、权限、事件和验收追溯 |
| OpenAPI 权限/通用权限状态数 | 166 | 人工权限矩阵正文未覆盖 64 项；缺口以本文第 3 节补齐 |
| OpenAPI 成功/失败事件数 | 564 | 人工事件矩阵正文未覆盖 520 项；完整事件字典以本文第 4 节为准 |
| operation 级任务追溯 | 282 | 人工任务映射正文未逐项覆盖 276 个 operation；逐 operation 映射以本文第 5 节为准 |
| 写接口数量 | 167 | 写接口、外部接入接口进入编码或联调前必须补请求/响应示例 |

## 3. 权限码与认证上下文补充表

> 本节覆盖 OpenAPI 中全部 `x-permission`。其中 `public`、`authenticated`、`not_applicable` 是通用权限状态，不是员工岗位权限；员工、设备、客人、开放应用和 Webhook 的身份来源必须看 `x-auth-context`。

| 权限码/通用状态 | 认证上下文 | 数据范围 | operation 数 | 代表 operationId | 覆盖状态 |
|---|---|---:|---:|---|---|
| `access.permission_template.apply` | staff-token | project | 1 | applyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.manage` | staff-token | project | 3 | createPermissionTemplate, updatePermissionTemplate, copyPermissionTemplate | 已在权限矩阵正文出现 |
| `access.permission_template.view` | staff-token | project | 1 | listPermissionTemplates | 已在权限矩阵正文出现 |
| `access.permission.view` | staff-token | project | 1 | listPermissions | 已在权限矩阵正文出现 |
| `access.role.manage` | staff-token | project | 1 | createRole | 已在权限矩阵正文出现 |
| `access.role.view` | staff-token | project | 1 | listRoles | 已在权限矩阵正文出现 |
| `access.staff_level.manage` | staff-token | project | 1 | assignStaffLevel | OpenAPI 补充，以本附录为准 |
| `access.staff_level.view` | staff-token | project | 1 | listStaffLevels | OpenAPI 补充，以本附录为准 |
| `access.temporary_grant.create` | staff-token | project | 1 | createTemporaryGrant | 已在权限矩阵正文出现 |
| `access.temporary_grant.revoke` | staff-token | project | 1 | revokeTemporaryGrant | 已在权限矩阵正文出现 |
| `access.user.manage` | staff-token | project | 1 | createAccessUser | 已在权限矩阵正文出现 |
| `access.user.view` | staff-token | project | 2 | listAccessUsers, getUserEffectivePermissions | 已在权限矩阵正文出现 |
| `amusement.device_status.view` | staff-token | project | 1 | listAmusementDeviceStatuses | 已在权限矩阵正文出现 |
| `amusement.group_booking.approve` | staff-token | project-amusement | 1 | approveAmusementGroupBooking | OpenAPI 补充，以本附录为准 |
| `amusement.group_booking.manage` | staff-token | project-amusement | 2 | createAmusementGroupBooking, releaseAmusementGroupBooking | OpenAPI 补充，以本附录为准 |
| `amusement.group_booking.view` | staff-token | project-amusement | 1 | listAmusementGroupBookings | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.confirm` | staff-token | project-amusement | 1 | confirmAmusementMaintenanceResume | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.read` | staff-token | project-amusement | 2 | listAmusementMaintenanceRecords, getAmusementMaintenanceRecord | OpenAPI 补充，以本附录为准 |
| `amusement.maintenance.submit` | staff-token | project-amusement-point | 1 | createAmusementMaintenanceRecord | OpenAPI 补充，以本附录为准 |
| `amusement.project.manage` | staff-token | project | 1 | createAmusementProjectRule | 已在权限矩阵正文出现 |
| `amusement.queue.view` | staff-token | project | 1 | listAmusementQueueEntries | 已在权限矩阵正文出现 |
| `amusement.reservation.view` | staff-token | project | 1 | listAmusementReservations | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.approve_reopen` | staff-token | project-amusement | 1 | approveAmusementReopen | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.create` | staff-token | project-amusement | 1 | createAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.recheck` | staff-token | project-amusement | 1 | recheckAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.rectify` | staff-token | project-amusement | 1 | rectifyAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.submit` | staff-token | project-amusement | 1 | submitAmusementSafetyInspection | 已在权限矩阵正文出现 |
| `amusement.safety_inspection.view` | staff-token | project-amusement | 2 | getAmusementOpenEligibility, listAmusementSafetyInspections | 已在权限矩阵正文出现 |
| `amusement.safety_template.manage` | staff-token | project-amusement | 1 | createAmusementSafetyTemplate | 已在权限矩阵正文出现 |
| `amusement.safety_template.view` | staff-token | project-amusement | 1 | listAmusementSafetyTemplates | 已在权限矩阵正文出现 |
| `amusement.session.manage` | staff-token | project-amusement | 2 | createAmusementProject, createAmusementSession | 已在权限矩阵正文出现 |
| `amusement.session.view` | staff-token | project-amusement | 2 | listAmusementProjects, listAmusementSessions | 已在权限矩阵正文出现 |
| `amusement.verification.execute` | staff-token | project-amusement-point | 1 | createAmusementVerification | 已在权限矩阵正文出现 |
| `amusement.verification.override` | staff-token | project-amusement, project-amusement-point | 2 | listAmusementManualOverrides, createAmusementManualOverride | 已在权限矩阵正文出现 |
| `amusement.verification.view` | staff-token | project-amusement | 1 | listAmusementVerifications | 已在权限矩阵正文出现 |
| `analytics.dashboard.view` | staff-token | project | 7 | listBusinessMetricSnapshots, getP0OverviewDashboard, getAnalyticsDomainDashboard, listMerchantHealthScores, listAnalyticsMerchants, listMetricDefinitions, getRobotAnalytics | 已在权限矩阵正文出现 |
| `analytics.risk_alert.manage` | staff-token | project | 3 | generateLocalLlmAiQualitySuggestion, createRiskAlert, resolveRiskAlert | 已在权限矩阵正文出现 |
| `analytics.risk_alert.view` | staff-token | project | 7 | listAiQualityResults, listAiQualityTasks, listCustomerIntents, listMetricAnomalies, listRiskAlerts, listRiskRules, listStaffServiceScores | 已在权限矩阵正文出现 |
| `authenticated` | staff-token | current-session, self | 3 | logout, refreshToken, getEffectivePermissions | 已在权限矩阵正文出现 |
| `bar.deposit_rule.manage` | staff-token | project-bar | 1 | createBarDepositRule | OpenAPI 补充，以本附录为准 |
| `bar.entry.age_verify` | staff-token | project-bar | 1 | createBarAgeVerification | 已在权限矩阵正文出现 |
| `bar.order.create` | staff-token | project, project-bar-table | 7 | createBarOrder, createBarOrderBill, deliverBarOrder, addBarOrderItem, listBarProductionTickets, listBarProducts, createBarProduct | 已在权限矩阵正文出现 |
| `bar.order.view` | staff-token | project-bar-table | 1 | listBarOrders | 已在权限矩阵正文出现 |
| `bar.reservation.create` | staff-token | project-bar | 1 | createBarReservation | 已在权限矩阵正文出现 |
| `bar.reservation.manage` | staff-token | project-bar | 3 | payBarReservationDeposit, checkInBarReservationDeposit, cancelBarReservation | OpenAPI 补充，以本附录为准 |
| `bar.reservation.view` | staff-token | project-bar-table | 1 | listBarReservations | 已在权限矩阵正文出现 |
| `bar.saved_bottle.manage` | staff-token | project-bar-customer | 4 | listSavedBottles, createSavedBottle, claimSavedBottle, cancelSavedBottle | 已在权限矩阵正文出现 |
| `bar.table_commission.calculate` | staff-token | project-bar | 1 | calculateBarTableCommission | 已在权限矩阵正文出现 |
| `bar.table.guest_checkin` | staff-token | project-bar | 1 | checkInBarReservationGuest | 已在权限矩阵正文出现 |
| `bar.table.manage` | staff-token | project-bar | 1 | createBarTable | 已在权限矩阵正文出现 |
| `bar.table.open_close` | staff-token | project-bar | 1 | openBarTable | 已在权限矩阵正文出现 |
| `bar.table.view` | staff-token | project-bar | 1 | listBarTables | 已在权限矩阵正文出现 |
| `contract.draft.manage` | staff-token | project-merchant | 1 | createContractDraft | 已在权限矩阵正文出现 |
| `contract.lifecycle.approve` | staff-token | project-merchant | 1 | approveContract | 已在权限矩阵正文出现 |
| `contract.view` | staff-token | project-merchant | 1 | listContracts | 已在权限矩阵正文出现 |
| `customer.loyalty.evaluate` | staff-token | project | 1 | evaluateMemberLevel | OpenAPI 补充，以本附录为准 |
| `customer.loyalty.view` | staff-token | project | 1 | listMemberLevels | OpenAPI 补充，以本附录为准 |
| `customer.membership.manage` | staff-token | project | 1 | saveMembershipPlan | OpenAPI 补充，以本附录为准 |
| `customer.membership.subscribe` | customer-token | customer | 1 | customerMembershipSubscribe | OpenAPI 补充，以本附录为准 |
| `customer.membership.view` | customer-token, staff-token | customer, project | 3 | listCustomerMemberships, customerMembershipBenefits, listMembershipPlans | OpenAPI 补充，以本附录为准 |
| `customer.points.freeze` | staff-token | project | 2 | freezePointsAccount, unfreezePointsAccount | OpenAPI 补充，以本附录为准 |
| `customer.points.transfer` | customer-token | customer | 1 | customerPointsTransfer | OpenAPI 补充，以本附录为准 |
| `customer.points.transfer_limit` | staff-token | project | 1 | savePointsTransferLimit | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.consume` | customer-token | customer | 1 | customerStoredValueConsume | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.freeze` | staff-token | project | 2 | freezeStoredValueAccount, unfreezeStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.refund` | staff-token | project | 1 | refundStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.topup` | customer-token | customer | 2 | customerStoredValueTopup, customerStoredValueSettle | OpenAPI 补充，以本附录为准 |
| `customer.stored_value.view` | customer-token | customer | 1 | customerStoredValueAccount | OpenAPI 补充，以本附录为准 |
| `entity.binding.manage` | staff-token | project-merchant | 3 | createBusinessEntity, createBusinessEntityBinding, changeBusinessEntityBinding | 已在权限矩阵正文出现 |
| `entity.binding.view` | staff-token | project-merchant | 2 | listBusinessEntities, listBusinessEntityBindings | 已在权限矩阵正文出现 |
| `evidence.attachment.create` | staff-token | project-object | 3 | createEvidenceAttachmentUploadIntent, bindEvidenceAttachment, completeEvidenceAttachmentUpload | 已在权限矩阵正文出现 |
| `evidence.event.create` | staff-token | project-device | 1 | createEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.event.view` | staff-token | project-object | 1 | listEvidenceEvents | 已在权限矩阵正文出现 |
| `evidence.review.create` | staff-token | project-object | 1 | createEvidenceReview | 已在权限矩阵正文出现 |
| `evidence.timeline.view` | staff-token | project-object | 1 | getEvidenceTimeline | 已在权限矩阵正文出现 |
| `finance.ar_ap.manage` | staff-token | project-entity | 1 | createArApItem | 已在权限矩阵正文出现 |
| `finance.ar_ap.view` | staff-token | project-entity | 1 | listArApItems | 已在权限矩阵正文出现 |
| `finance.daily_close.lock` | staff-token | project-entity | 1 | createDailyClose | 已在权限矩阵正文出现 |
| `finance.daily_close.manage` | staff-token | project-entity | 1 | listDailyCloseSummaries | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.approve` | staff-token | project-entity | 1 | approveArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.locked_backfill.request` | staff-token | project-entity | 1 | requestArApAdjustment | OpenAPI 补充，以本附录为准 |
| `finance.payment_intent.create` | staff-token | project-bar-table, project-order | 3 | createBarBillPayIntent, createPaymentIntent, createTicketOrderPayIntent | 已在权限矩阵正文出现 |
| `finance.payment.register` | staff-token | project-bar-table, project-entity, project-order | 3 | settleBarBill, createArApPayment, confirmOfflinePayment | 已在权限矩阵正文出现 |
| `finance.reconciliation.view` | staff-token | project | 1 | listReconciliationBatches | 已在权限矩阵正文出现 |
| `finance.refund.create` | staff-token | project-order | 1 | createFinanceRefund | 已在权限矩阵正文出现 |
| `floor_plan.import.manage` | staff-token | project-space | 2 | createFloorPlanImport, publishFloorPlanImport | OpenAPI 补充，以本附录为准 |
| `floor_plan.import.view` | staff-token | project-space | 2 | listFloorPlanImports, getFloorPlanImportDetails | OpenAPI 补充，以本附录为准 |
| `floor_plan.manage` | staff-token | project-space | 1 | createFloorPlan | OpenAPI 补充，以本附录为准 |
| `floor_plan.view` | staff-token | project-space | 2 | listFloorPlans, listFloorPlanShapes | OpenAPI 补充，以本附录为准 |
| `governance.project.manage` | staff-token | project | 1 | createProject | 已在权限矩阵正文出现 |
| `governance.project.view` | staff-token | project | 1 | listProjects | 已在权限矩阵正文出现 |
| `governance.space.manage` | staff-token | project | 1 | createSpace | 已在权限矩阵正文出现 |
| `governance.space.view` | staff-token | project-merchant-space | 1 | listSpaces | 已在权限矩阵正文出现 |
| `governance.supervisor_review.approve` | staff-token | project-entity | 1 | approveSupervisorReview | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.manage` | staff-token | project-entity | 1 | evaluateSupervisorReview | OpenAPI 补充，以本附录为准 |
| `governance.supervisor_review.view` | staff-token | project-entity | 1 | listSupervisorReviewPolicies | OpenAPI 补充，以本附录为准 |
| `integration.provider_adapter.create` | staff-token | project-provider | 1 | createProviderAdapter | 已在权限矩阵正文出现 |
| `integration.provider_adapter.production_ready` | staff-token | project-provider | 1 | markProviderAdapterProductionReady | 已在权限矩阵正文出现 |
| `integration.provider_adapter.test` | staff-token | project-provider | 1 | testProviderAdapterMock | 已在权限矩阵正文出现 |
| `integration.provider_adapter.update` | staff-token | project-provider | 1 | updateProviderAdapter | 已在权限矩阵正文出现 |
| `integration.provider_adapter.view` | staff-token | project, project-provider | 16 | listIntegrationCompensationTasks, listIntegrationDevices, listIntegrationDeviceEvents, listIntegrationOfflineSyncBatches, listIntegrationOrderSyncs, listProviderAdapters, listIntegrationRetailSkus, listIntegrationRetryTasks, listRobotTaskEvents, listRobotTasks, listVideoCameras, listVoucherVerifications ... | 已在权限矩阵正文出现 |
| `integration.video.view` | staff-token | project | 1 | listIntegrationVideoAnchors | OpenAPI 补充，以本附录为准 |
| `inventory.handover.review` | staff-token | project-warehouse-bar | 1 | reviewTraceHandoverException | OpenAPI 补充，以本附录为准 |
| `inventory.handover.scan` | staff-token | project-warehouse-bar | 4 | scanTraceCodeHandover, createTraceHandover, scanTraceSegment, reportRobotSegment | OpenAPI 补充，以本附录为准 |
| `inventory.high_value.review` | staff-token | project-warehouse-bar | 2 | listHighValueReviews, reviewHighValue | OpenAPI 补充，以本附录为准 |
| `inventory.purchase_request.manage` | staff-token | project | 1 | createPurchaseRequest | 已在权限矩阵正文出现 |
| `inventory.purchase_request.view` | staff-token | project | 1 | listPurchaseRequests | 已在权限矩阵正文出现 |
| `inventory.purchase.approve` | staff-token | project-warehouse-bar | 5 | approvePurchaseRequest, orderPurchaseRequest, arrivePurchaseRequest, receivePurchaseRequest, approveSupplyContract | OpenAPI 补充，以本附录为准 |
| `inventory.stock.adjust` | staff-token | project-warehouse | 1 | createStockAdjustment | 已在权限矩阵正文出现 |
| `inventory.stock.move` | staff-token | project, project-warehouse | 5 | createInventoryItem, createStockIn, createStockMovement, transferStockMovement, createTypedStockMovement | 已在权限矩阵正文出现 |
| `inventory.stock.view` | staff-token | project, project-warehouse | 3 | listInventoryItems, listStockBalances, listStockMovements | 已在权限矩阵正文出现 |
| `inventory.supplier.manage` | staff-token | project | 1 | createInventorySupplier | 已在权限矩阵正文出现 |
| `inventory.supplier.view` | staff-token | project | 1 | listInventorySuppliers | 已在权限矩阵正文出现 |
| `inventory.supply_contract.manage` | staff-token | project-warehouse-bar | 1 | createSupplyContract | OpenAPI 补充，以本附录为准 |
| `inventory.warehouse.manage` | staff-token | project-warehouse-bar | 1 | createWarehouse | OpenAPI 补充，以本附录为准 |
| `mdm.device.manage` | staff-token | project-device | 2 | enrollMdmDevice, reportMdmPolicyViolation | OpenAPI 补充，以本附录为准 |
| `mdm.policy.manage` | staff-token | project-device | 4 | listMdmAppWhitelist, upsertMdmAppWhitelist, listMdmSiteWhitelist, upsertMdmSiteWhitelist | OpenAPI 补充，以本附录为准 |
| `mdm.violation.view` | staff-token | project-device | 1 | listMdmPolicyViolations | OpenAPI 补充，以本附录为准 |
| `merchant.billing.view` | staff-token | project-merchant | 1 | listMerchantBills | OpenAPI 补充，以本附录为准 |
| `merchant.profile.manage` | staff-token | project, project-merchant | 2 | createMerchant, updateMerchant | 已在权限矩阵正文出现 |
| `merchant.profile.view` | staff-token | project-merchant | 1 | listMerchants | 已在权限矩阵正文出现 |
| `merchant.service_work_order.assign` | staff-token | project-merchant | 1 | assignMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.close` | staff-token | project-merchant | 1 | closeMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.create` | staff-token | project-merchant | 1 | createMerchantServiceWorkOrder | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.process` | staff-token | project-merchant | 1 | addMerchantServiceWorkOrderAction | OpenAPI 补充，以本附录为准 |
| `merchant.service_work_order.view` | staff-token | project-merchant | 1 | listMerchantServiceWorkOrders | OpenAPI 补充，以本附录为准 |
| `not_applicable` | customer-token, device-credential, open-app-credential, webhook-signature | customer, customer-project-bar, open-app, open-endpoint, project, project-device, public-project-bar | 17 | openAuthToken, getOpenEventResult, receiveOpenWebhook, listCustomerBarReservationOptions, createCustomerBarReservation, createCustomerConsent, listCustomerOrders, getCustomerPoints, createCustomerTicketOrder, getCustomerTicketQr, reportIntegrationRobotTaskEvent, receiveIntegrationTypedWebhook ... | 已在权限矩阵正文出现 |
| `openplatform.reconcile.execute` | staff-token | project-entity-channel | 1 | listReconciliationDifferences | 已在权限矩阵正文出现 |
| `ops.production_readiness.check` | staff-token | project | 1 | createProductionReadinessCheck | 已在权限矩阵正文出现 |
| `ops.production_readiness.manage` | staff-token | project | 1 | updateProductionReadinessItem | 已在权限矩阵正文出现 |
| `ops.production_readiness.view` | staff-token | project | 1 | listProductionReadinessItems | 已在权限矩阵正文出现 |
| `public` | public | customer, none, public-project | 4 | login, customerPhoneLogin, listCustomerTicketProducts, getSystemHealth | 已在权限矩阵正文出现 |
| `staff.device.bind` | device-credential, staff-token | project | 2 | bindStaffDeviceCredential, reportStaffDeviceIntegrity | OpenAPI 补充，以本附录为准 |
| `staff.device.review` | staff-token | project | 1 | approveStaffDeviceRebind | OpenAPI 补充，以本附录为准 |
| `staff.device.revoke` | staff-token | project | 1 | revokeStaffDeviceCredential | OpenAPI 补充，以本附录为准 |
| `staff.notice.view` | staff-token | project | 2 | listEmployeeNotices, acknowledgeEmployeeNotice | OpenAPI 补充，以本附录为准 |
| `staff.profile.review` | staff-token | project | 1 | reviewStaffRegistration | OpenAPI 补充，以本附录为准 |
| `staff.profile.submit` | staff-token | project | 1 | submitStaffRegistration | OpenAPI 补充，以本附录为准 |
| `staff.profile.view` | staff-token | project | 1 | listStaffProfiles | OpenAPI 补充，以本附录为准 |
| `system.version.view` | staff-token | project | 1 | getSystemVersion | 已在权限矩阵正文出现 |
| `terminal.device.bind` | staff-token | project, project-device | 4 | bindDevice, returnTerminalDevice, getTerminalOfflineEntitlements, syncTerminalTasks | 已在权限矩阵正文出现 |
| `terminal.device.unbind` | staff-token | project-device | 1 | unbindDevice | 已在权限矩阵正文出现 |
| `terminal.switch.manage` | staff-token | project-device | 1 | updateCapabilitySwitch | 已在权限矩阵正文出现 |
| `terminal.switch.view` | staff-token | project-device | 1 | listCapabilitySwitches | 已在权限矩阵正文出现 |
| `ticket.credential.view` | staff-token | project-order-customer | 1 | getTicketCredential | 已在权限矩阵正文出现 |
| `ticket.dynamic_pricing.manage` | staff-token | project-channel | 1 | createTicketDynamicPricingRule | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.publish` | staff-token | project-channel | 1 | publishTicketDynamicPricingRule | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.simulate` | staff-token | project-channel | 1 | simulateTicketDynamicPricing | OpenAPI 补充，以本附录为准 |
| `ticket.dynamic_pricing.view` | staff-token | project-channel | 1 | listTicketDynamicPricingRules | OpenAPI 补充，以本附录为准 |
| `ticket.entitlement.manage` | staff-token | project | 1 | createEntitlementTemplate | 已在权限矩阵正文出现 |
| `ticket.entitlement.view` | staff-token | project | 1 | listEntitlementTemplates | 已在权限矩阵正文出现 |
| `ticket.order.create` | staff-token | project, project-channel | 2 | createTicketOrder, issueTicketTypeDirectly | 已在权限矩阵正文出现 |
| `ticket.price_variant.manage` | staff-token | project-channel | 1 | createTicketPriceRule | 已在权限矩阵正文出现 |
| `ticket.price_variant.view` | staff-token | project-channel | 1 | listTicketPriceRules | 已在权限矩阵正文出现 |
| `ticket.product.manage` | staff-token | project | 3 | createTicketType, bindTicketTypeEntitlement, publishTicketType | 已在权限矩阵正文出现 |
| `ticket.product.view` | staff-token | project | 1 | listTicketTypes | 已在权限矩阵正文出现 |
| `ticket.refund.approve` | staff-token | project-order | 1 | approveTicketRefund | 已在权限矩阵正文出现 |
| `ticket.refund.request` | staff-token | project-order | 1 | requestTicketRefund | 已在权限矩阵正文出现 |
| `ticket.ticket.issue` | staff-token | project-order | 1 | issueTicket | 已在权限矩阵正文出现 |
| `ticket.verify.reverse` | staff-token | project-point | 1 | reverseTicketVerification | OpenAPI 补充，以本附录为准 |
| `ticket.verify.view` | staff-token | project | 1 | listIssuedEntitlements | 已在权限矩阵正文出现 |
| `verification.execute` | staff-token | project-point | 2 | createTicketVerification, previewTicketVerification | 已在权限矩阵正文出现 |
| `verification.view` | staff-token | project-merchant | 1 | listTicketVerifications | 已在权限矩阵正文出现 |
| `wecom.retention.manage` | staff-token | platform | 2 | listWecomRetentionPolicies, upsertWecomRetentionPolicy | OpenAPI 补充，以本附录为准 |
| `wecom.sync` | webhook-signature | open-endpoint | 1 | syncWecomConversation | OpenAPI 补充，以本附录为准 |
| `wecom.view_raw` | staff-token | project | 1 | viewWecomConversationMessages | OpenAPI 补充，以本附录为准 |

## 4. OpenAPI 事件字典

> 本节覆盖每个 operation 的 `x-audit-event-success` 和 `x-audit-event-failure`。人工事件证据矩阵保留高风险证据策略；接口级完整事件名以本节为准。实现时不得只更新业务表而不写事件。

| 事件名 | 结果 | operationId | 方法 | 路径 | 建议任务簇 | 权限 | 覆盖状态 |
|---|---|---|---|---|---|---|---|
| `access.effective_permission.viewed` | success | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed` | success | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed_failed` | failure | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | AUTH-账号权限 | `authenticated` | 接口级事件，以本附录为准 |
| `access.effective_permission.viewed_failed` | failure | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | AUTH-账号权限 | `access.user.view` | 接口级事件，以本附录为准 |
| `access.staff_level.assign_failed` | failure | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.assigned` | success | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | AUTH-账号权限 | `access.staff_level.manage` | 接口级事件，以本附录为准 |
| `access.staff_level.viewed` | success | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
| `access.staff_level.viewed_failed` | failure | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | AUTH-账号权限 | `access.staff_level.view` | 接口级事件，以本附录为准 |
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
| `amusement.device_status.viewed` | success | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | AMU-游乐安全 | `amusement.device_status.view` | 接口级事件，以本附录为准 |
| `amusement.device_status.viewed_failed` | failure | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | AMU-游乐安全 | `amusement.device_status.view` | 接口级事件，以本附录为准 |
| `amusement.maintenance_record_failed` | failure | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.submit` | 接口级事件，以本附录为准 |
| `amusement.maintenance_recorded` | success | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.submit` | 接口级事件，以本附录为准 |
| `amusement.maintenance_resume_failed` | failure | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | AMU-游乐安全 | `amusement.maintenance.confirm` | 接口级事件，以本附录为准 |
| `amusement.maintenance.view_failed` | failure | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.view_failed` | failure | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.viewed` | success | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.maintenance.viewed` | success | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | AMU-游乐安全 | `amusement.maintenance.read` | 接口级事件，以本附录为准 |
| `amusement.manual_override.view_failed` | failure | `listAmusementManualOverrides` | `GET` | `/api/v1/amusement/manual-overrides` | AMU-游乐安全 | `amusement.verification.override` | 接口级事件，以本附录为准 |
| `amusement.manual_override.viewed` | success | `listAmusementManualOverrides` | `GET` | `/api/v1/amusement/manual-overrides` | AMU-游乐安全 | `amusement.verification.override` | 接口级事件，以本附录为准 |
| `amusement.open_eligibility.blocked` | failure | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.open_eligibility.viewed` | success | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | AMU-游乐安全 | `amusement.safety_inspection.view` | 接口级事件，以本附录为准 |
| `amusement.project_rule.changed` | success | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | AMU-游乐安全 | `amusement.project.manage` | 接口级事件，以本附录为准 |
| `amusement.project_rule.changed_failed` | failure | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | AMU-游乐安全 | `amusement.project.manage` | 接口级事件，以本附录为准 |
| `amusement.project.resumed` | success | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | AMU-游乐安全 | `amusement.maintenance.confirm` | 接口级事件，以本附录为准 |
| `amusement.queue.viewed` | success | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.view` | 接口级事件，以本附录为准 |
| `amusement.queue.viewed_failed` | failure | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | AMU-游乐安全 | `amusement.queue.view` | 接口级事件，以本附录为准 |
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
| `amusement.verification_overridden` | success | `createAmusementManualOverride` | `POST` | `/api/v1/amusement/manual-overrides` | AMU-游乐安全 | `amusement.verification.override` | 已在事件矩阵正文出现 |
| `amusement.verification_override_failed` | failure | `createAmusementManualOverride` | `POST` | `/api/v1/amusement/manual-overrides` | AMU-游乐安全 | `amusement.verification.override` | 接口级事件，以本附录为准 |
| `amusement.verification_succeeded` | success | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.execute` | 已在事件矩阵正文出现 |
| `amusement.verification.view_failed` | failure | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `amusement.verification.viewed` | success | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | AMU-游乐安全 | `amusement.verification.view` | 接口级事件，以本附录为准 |
| `analytics_business_metric_snapshot.viewed.failure` | failure | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics_business_metric_snapshot.viewed.success` | success | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_result.viewed` | success | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_result.viewed_failed` | failure | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_task.viewed` | success | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality_task.viewed_failed` | failure | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.ai_quality.local_llm_suggestion.created` | success | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.ai_quality.local_llm_suggestion.failed` | failure | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.customer_intent.viewed` | success | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.customer_intent.viewed_failed` | failure | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_domain.view_failed` | failure | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_domain.viewed` | success | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_view_failed` | failure | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.dashboard_viewed` | success | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_health.viewed` | success | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_health.viewed_failed` | failure | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_summary.viewed` | success | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.merchant_summary.viewed_failed` | failure | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.viewed` | success | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_anomaly.viewed_failed` | failure | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.metric_definition.viewed` | success | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.metric_definition.viewed_failed` | failure | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.risk_alert.resolved` | success | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.risk_alert.resolved_failed` | failure | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `analytics.risk_rule.viewed` | success | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.risk_rule.viewed_failed` | failure | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.robot.viewed` | success | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.robot.viewed_failed` | failure | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | ANA-风控分析 | `analytics.dashboard.view` | 接口级事件，以本附录为准 |
| `analytics.staff_service_score.viewed` | success | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `analytics.staff_service_score.viewed_failed` | failure | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.logged` | success | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `api_call.rejected` | failure | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `auth.login.failed` | failure | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.login.succeeded` | success | `login` | `POST` | `/api/v1/auth/login` | AUTH-登录会话 | `public` | 接口级事件，以本附录为准 |
| `auth.logout` | success | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.logout_failed` | failure | `logout` | `POST` | `/api/v1/auth/logout` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.token_refresh_failed` | failure | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `auth.token.refreshed` | success | `refreshToken` | `POST` | `/api/v1/auth/refresh` | AUTH-登录会话 | `authenticated` | 接口级事件，以本附录为准 |
| `bar.age_verification.passed` | success | `createBarAgeVerification` | `POST` | `/api/v1/bar/age-verifications` | BAR-酒吧经营 | `bar.entry.age_verify` | 已在事件矩阵正文出现 |
| `bar.age_verification.rejected` | failure | `createBarAgeVerification` | `POST` | `/api/v1/bar/age-verifications` | BAR-酒吧经营 | `bar.entry.age_verify` | 已在事件矩阵正文出现 |
| `bar.bill.created` | success | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.bill.created_failed` | failure | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.bill.settle_failed` | failure | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `bar.bill.settled` | success | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | BAR-酒吧经营 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `bar.deposit_rule.create_failed` | failure | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | BAR-酒吧经营 | `bar.deposit_rule.manage` | 接口级事件，以本附录为准 |
| `bar.deposit_rule.created` | success | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | BAR-酒吧经营 | `bar.deposit_rule.manage` | 接口级事件，以本附录为准 |
| `bar.order_create_failed` | failure | `createBarOrder` | `POST` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_created` | success | `createBarOrder` | `POST` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_deliver_failed` | failure | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_delivered` | success | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | BAR-酒吧经营 | `bar.order.create` | 已在事件矩阵正文出现 |
| `bar.order_item.created` | success | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order_item.created_failed` | failure | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.order.view_failed` | failure | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.order.viewed` | success | `listBarOrders` | `GET` | `/api/v1/bar/orders` | BAR-酒吧经营 | `bar.order.view` | 接口级事件，以本附录为准 |
| `bar.product.created` | success | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.product.created_failed` | failure | `createBarProduct` | `POST` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.product.viewed` | success | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.product.viewed_failed` | failure | `listBarProducts` | `GET` | `/api/v1/bar/products` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed` | success | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.production_ticket.viewed_failed` | failure | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | BAR-酒吧经营 | `bar.order.create` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_deduct_failed` | failure | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_deducted` | success | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_paid` | success | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
| `bar.reservation_deposit_pay_failed` | failure | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | BAR-酒吧经营 | `bar.reservation.manage` | 接口级事件，以本附录为准 |
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
| `bar.saved_bottle.pickup_failed` | failure | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.view_failed` | failure | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.saved_bottle.viewed` | success | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | INV-库存存酒 | `bar.saved_bottle.manage` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculate_failed` | failure | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 接口级事件，以本附录为准 |
| `bar.table_commission.calculated` | success | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | BAR-酒吧经营 | `bar.table_commission.calculate` | 已在事件矩阵正文出现 |
| `bar.table_guest.checked_in` | success | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.table_guest.rejected` | failure | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | BAR-酒吧经营 | `bar.table.guest_checkin` | 已在事件矩阵正文出现 |
| `bar.table.change_failed` | failure | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.changed` | success | `createBarTable` | `POST` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.manage` | 接口级事件，以本附录为准 |
| `bar.table.open_failed` | failure | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.opened` | success | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | BAR-酒吧经营 | `bar.table.open_close` | 接口级事件，以本附录为准 |
| `bar.table.view_failed` | failure | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `bar.table.viewed` | success | `listBarTables` | `GET` | `/api/v1/bar/tables` | BAR-酒吧经营 | `bar.table.view` | 接口级事件，以本附录为准 |
| `business_entity.change_failed` | failure | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.changed` | success | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.manage` | 接口级事件，以本附录为准 |
| `business_entity.view_failed` | failure | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `business_entity.viewed` | success | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | GOV-平台治理 | `entity.binding.view` | 接口级事件，以本附录为准 |
| `contract.draft_save_failed` | failure | `createContractDraft` | `POST` | `/api/v1/platform/contracts` | GOV-平台治理 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.draft_saved` | success | `createContractDraft` | `POST` | `/api/v1/platform/contracts` | GOV-平台治理 | `contract.draft.manage` | 接口级事件，以本附录为准 |
| `contract.lifecycle_change_failed` | failure | `approveContract` | `POST` | `/api/v1/platform/contracts/{contract_id}/approve` | GOV-平台治理 | `contract.lifecycle.approve` | 接口级事件，以本附录为准 |
| `contract.lifecycle_changed` | success | `approveContract` | `POST` | `/api/v1/platform/contracts/{contract_id}/approve` | GOV-平台治理 | `contract.lifecycle.approve` | 已在事件矩阵正文出现 |
| `contract.view_failed` | failure | `listContracts` | `GET` | `/api/v1/platform/contracts` | GOV-平台治理 | `contract.view` | 接口级事件，以本附录为准 |
| `contract.viewed` | success | `listContracts` | `GET` | `/api/v1/platform/contracts` | GOV-平台治理 | `contract.view` | 接口级事件，以本附录为准 |
| `customer.auth.login` | success | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.auth.login_failed` | failure | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | CUS-客人端 | `public` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_view_failed` | failure | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation_options_viewed` | success | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.create_failed` | failure | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.bar_reservation.created` | success | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | CUS/BAR-客人订台 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_in_failed` | failure | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_ins_view_failed` | failure | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.check_ins_viewed` | success | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.checked_in` | success | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.create_failed` | failure | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.consent.created` | success | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.level_evaluate_failed` | failure | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `customer.member_levels_view_failed` | failure | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.member_levels_viewed` | success | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | CUS-客人端 | `customer.loyalty.view` | 接口级事件，以本附录为准 |
| `customer.membership_benefits_viewed` | success | `customerMembershipBenefits` | `GET` | `/api/v1/customer/membership-benefits` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.membership_benefits_viewed_failed` | failure | `customerMembershipBenefits` | `GET` | `/api/v1/customer/membership-benefits` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.membership_plans_admin_viewed` | success | `listMembershipPlans` | `GET` | `/api/v1/loyalty/membership-plans` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.membership_plans_admin_viewed_failed` | failure | `listMembershipPlans` | `GET` | `/api/v1/loyalty/membership-plans` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.membership_plans_viewed` | success | `listCustomerMembershipPlans` | `GET` | `/api/v1/customer/membership-plans` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.membership_plans_viewed_failed` | failure | `listCustomerMembershipPlans` | `GET` | `/api/v1/customer/membership-plans` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.membership.subscribe` | success | `customerMembershipSubscribe` | `POST` | `/api/v1/customer/memberships/subscribe` | CUS-客人端 | `customer.membership.subscribe` | 接口级事件，以本附录为准 |
| `customer.membership.subscribe_failed` | failure | `customerMembershipSubscribe` | `POST` | `/api/v1/customer/memberships/subscribe` | CUS-客人端 | `customer.membership.subscribe` | 接口级事件，以本附录为准 |
| `customer.memberships_viewed` | success | `listCustomerMemberships` | `GET` | `/api/v1/customer/memberships` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.memberships_viewed_failed` | failure | `listCustomerMemberships` | `GET` | `/api/v1/customer/memberships` | CUS-客人端 | `customer.membership.view` | 接口级事件，以本附录为准 |
| `customer.orders_view_failed` | failure | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.orders_viewed` | success | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_view_failed` | failure | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points_viewed` | success | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | CUS-客人端 | `not_applicable` | 接口级事件，以本附录为准 |
| `customer.points.freeze_failed` | failure | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.frozen` | success | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.unfreeze_failed` | failure | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.points.unfrozen` | success | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | CUS-客人端 | `customer.points.freeze` | 接口级事件，以本附录为准 |
| `customer.stored_value_account_viewed` | success | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
| `customer.stored_value_account_viewed_failed` | failure | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | CUS-客人端 | `customer.stored_value.view` | 接口级事件，以本附录为准 |
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
| `device.policy_violation` | success | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
| `device.policy_violation_failed` | failure | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.device.manage` | 接口级事件，以本附录为准 |
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
| `evidence.event_view_failed` | failure | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.event_viewed` | success | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.view` | 接口级事件，以本附录为准 |
| `evidence.events_report_failed` | failure | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.events_reported` | success | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | EVD-事件证据 | `evidence.event.create` | 接口级事件，以本附录为准 |
| `evidence.review_create_failed` | failure | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.review_created` | success | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | EVD-事件证据 | `evidence.review.create` | 接口级事件，以本附录为准 |
| `evidence.timeline_view_failed` | failure | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.timeline_viewed` | success | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | EVD-事件证据 | `evidence.timeline.view` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_created` | success | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `evidence.upload_intent_failed` | failure | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | EVD-事件证据 | `evidence.attachment.create` | 接口级事件，以本附录为准 |
| `finance_ar_ap.create_failed` | failure | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 接口级事件，以本附录为准 |
| `finance_ar_ap.created` | success | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.manage` | 已在事件矩阵正文出现 |
| `finance_ar_ap.view_failed` | failure | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_ar_ap.viewed` | success | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | FIN-财务台账 | `finance.ar_ap.view` | 接口级事件，以本附录为准 |
| `finance_daily_close.lock_failed` | failure | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 接口级事件，以本附录为准 |
| `finance_daily_close.locked` | success | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | FIN-财务台账 | `finance.daily_close.lock` | 已在事件矩阵正文出现 |
| `finance_payment.register_failed` | failure | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.register_failed` | failure | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 接口级事件，以本附录为准 |
| `finance_payment.registered` | success | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_payment.registered` | success | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | FIN-财务台账 | `finance.payment.register` | 已在事件矩阵正文出现 |
| `finance_reconciliation.difference_found` | success | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `openplatform.reconcile.execute` | 接口级事件，以本附录为准 |
| `finance_reconciliation.view_failed` | failure | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | FIN-财务台账 | `openplatform.reconcile.execute` | 接口级事件，以本附录为准 |
| `finance_refund.create_failed` | failure | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance_refund.created` | success | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | FIN-财务台账 | `finance.refund.create` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.view_failed` | failure | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.daily_close_summary.viewed` | success | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | FIN-财务台账 | `finance.daily_close.manage` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_approved` | success | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_decision_failed` | failure | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | FIN-财务台账 | `finance.locked_backfill.approve` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_request_failed` | failure | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.locked_backfill_requested` | success | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | FIN-财务台账 | `finance.locked_backfill.request` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed` | success | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
| `finance.reconciliation_batch.viewed_failed` | failure | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | FIN-财务台账 | `finance.reconciliation.view` | 接口级事件，以本附录为准 |
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
| `floor_plan.view_failed` | failure | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `floor_plan.viewed` | success | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | P0-待归类 | `floor_plan.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decide_failed` | failure | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.decided` | success | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | GOV-平台治理 | `governance.supervisor_review.approve` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluate_failed` | failure | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.evaluated` | success | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | GOV-平台治理 | `governance.supervisor_review.manage` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.policies_view_failed` | failure | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `governance.supervisor_review.policies_viewed` | success | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | GOV-平台治理 | `governance.supervisor_review.view` | 接口级事件，以本附录为准 |
| `integration.compensation_task.viewed` | success | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.compensation_task.viewed_failed` | failure | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device_event.viewed` | success | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device_event.viewed_failed` | failure | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device.viewed` | success | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.device.viewed_failed` | failure | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.offline_sync_batch.viewed` | success | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.offline_sync_batch.viewed_failed` | failure | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.order_sync.viewed` | success | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.order_sync.viewed_failed` | failure | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.retail_sku.viewed` | success | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.retail_sku.viewed_failed` | failure | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
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
| `integration.wecom_risk_hit.viewed` | success | `listWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `integration.wecom_risk_hit.viewed_failed` | failure | `listWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | INT-第三方集成 | `integration.provider_adapter.view` | 接口级事件，以本附录为准 |
| `inventory.adjustment_failed` | failure | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | INV-库存存酒 | `inventory.stock.adjust` | 接口级事件，以本附录为准 |
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
| `inventory.received` | success | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.received_failed` | failure | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | INV-库存存酒 | `inventory.purchase.approve` | 接口级事件，以本附录为准 |
| `inventory.stock_balance.viewed` | success | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_balance.viewed_failed` | failure | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_movement_failed` | failure | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.created` | success | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.created_failed` | failure | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.view_failed` | failure | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_movement.viewed` | success | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | INV-库存存酒 | `inventory.stock.view` | 接口级事件，以本附录为准 |
| `inventory.stock_transfer.created` | success | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.stock_transfer.created_failed` | failure | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | INV-库存存酒 | `inventory.stock.move` | 接口级事件，以本附录为准 |
| `inventory.supplier_change_failed` | failure | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_changed` | success | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.manage` | 接口级事件，以本附录为准 |
| `inventory.supplier_view_failed` | failure | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.view` | 接口级事件，以本附录为准 |
| `inventory.supplier_viewed` | success | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | INV-库存存酒 | `inventory.supplier.view` | 接口级事件，以本附录为准 |
| `inventory.warehouse.create_failed` | failure | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
| `inventory.warehouse.created` | success | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | INV-库存存酒 | `inventory.warehouse.manage` | 接口级事件，以本附录为准 |
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
| `mdm.violation.view_failed` | failure | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `mdm.violation.viewed` | success | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | TERM-终端设备 | `mdm.violation.view` | 接口级事件，以本附录为准 |
| `member.level_changed` | success | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | CUS-客人端 | `customer.loyalty.evaluate` | 接口级事件，以本附录为准 |
| `membership.plan_saved` | success | `saveMembershipPlan` | `POST` | `/api/v1/loyalty/membership-plans` | CUS-客人端 | `customer.membership.manage` | 接口级事件，以本附录为准 |
| `membership.plan_saved_failed` | failure | `saveMembershipPlan` | `POST` | `/api/v1/loyalty/membership-plans` | CUS-客人端 | `customer.membership.manage` | 接口级事件，以本附录为准 |
| `merchant_billing.bill.view_failed` | failure | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
| `merchant_billing.bill.viewed` | success | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | P0-待归类 | `merchant.billing.view` | 接口级事件，以本附录为准 |
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
| `merchant.update_failed` | failure | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.update_failed` | failure | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `createMerchant` | `POST` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.updated` | success | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | GOV-平台治理 | `merchant.profile.manage` | 接口级事件，以本附录为准 |
| `merchant.view_failed` | failure | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `merchant.viewed` | success | `listMerchants` | `GET` | `/api/v1/platform/merchants` | GOV-平台治理 | `merchant.profile.view` | 接口级事件，以本附录为准 |
| `open_auth.failed` | failure | `openAuthToken` | `POST` | `/api/open/auth/token` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `open_auth.succeeded` | success | `openAuthToken` | `POST` | `/api/open/auth/token` | OPEN-开放平台 | `not_applicable` | 接口级事件，以本附录为准 |
| `open_mapping.changed` | success | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | INT-第三方集成 | `integration.provider_adapter.update` | 已在事件矩阵正文出现 |
| `operation_handover.created` | success | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
| `operation_handover.failed` | failure | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | INV-库存存酒 | `inventory.handover.scan` | 接口级事件，以本附录为准 |
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
| `points.transfer_limit_saved` | success | `savePointsTransferLimit` | `POST` | `/api/v1/loyalty/points-transfer-limits` | CUS-客人端 | `customer.points.transfer_limit` | 接口级事件，以本附录为准 |
| `points.transfer_limit_saved_failed` | failure | `savePointsTransferLimit` | `POST` | `/api/v1/loyalty/points-transfer-limits` | CUS-客人端 | `customer.points.transfer_limit` | 接口级事件，以本附录为准 |
| `points.transferred` | success | `customerPointsTransfer` | `POST` | `/api/v1/customer/points/transfers` | CUS-客人端 | `customer.points.transfer` | 接口级事件，以本附录为准 |
| `points.transferred_failed` | failure | `customerPointsTransfer` | `POST` | `/api/v1/customer/points/transfers` | CUS-客人端 | `customer.points.transfer` | 接口级事件，以本附录为准 |
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
| `risk_alert.create_failed` | failure | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.created` | success | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.manage` | 接口级事件，以本附录为准 |
| `risk_alert.view_failed` | failure | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `risk_alert.viewed` | success | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | ANA-风控分析 | `analytics.risk_alert.view` | 接口级事件，以本附录为准 |
| `role.change_failed` | failure | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.changed` | success | `createRole` | `POST` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.manage` | 接口级事件，以本附录为准 |
| `role.view_failed` | failure | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
| `role.viewed` | success | `listRoles` | `GET` | `/api/v1/access/roles` | AUTH-账号权限 | `access.role.view` | 接口级事件，以本附录为准 |
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
| `staff.notice_acknowledge_failed` | failure | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice_acknowledged` | success | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice.view_failed` | failure | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.notice.viewed` | success | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | AUTH-账号权限 | `staff.notice.view` | 接口级事件，以本附录为准 |
| `staff.profile.view_failed` | failure | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.profile.viewed` | success | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | AUTH-账号权限 | `staff.profile.view` | 接口级事件，以本附录为准 |
| `staff.register_failed` | failure | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.registered` | success | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | AUTH-账号权限 | `staff.profile.submit` | 接口级事件，以本附录为准 |
| `staff.review_failed` | failure | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | AUTH-账号权限 | `staff.profile.review` | 接口级事件，以本附录为准 |
| `stored_value.frozen` | success | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
| `stored_value.frozen_failed` | failure | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | CUS-客人端 | `customer.stored_value.freeze` | 接口级事件，以本附录为准 |
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
| `terminal_switch.change_failed` | failure | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_switch.changed` | success | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | TERM-终端设备 | `terminal.switch.manage` | 接口级事件，以本附录为准 |
| `terminal_switch.view_failed` | failure | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal_switch.viewed` | success | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | TERM-终端设备 | `terminal.switch.view` | 接口级事件，以本附录为准 |
| `terminal.access_rejected` | failure | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.device.bind_failed` | failure | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.bound` | success | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.returned` | success | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.returned_failed` | failure | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.device.unbind_failed` | failure | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.device.unbound` | success | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | TERM-终端设备 | `terminal.device.unbind` | 接口级事件，以本附录为准 |
| `terminal.kiosk_config_viewed` | success | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat` | success | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.kiosk_heartbeat_failed` | failure | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | TERM-终端设备 | `not_applicable` | 接口级事件，以本附录为准 |
| `terminal.offline_entitlements.viewed` | success | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.offline_entitlements.viewed_failed` | failure | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.sync_task.accepted` | success | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
| `terminal.sync_task.accepted_failed` | failure | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | TERM-终端设备 | `terminal.device.bind` | 接口级事件，以本附录为准 |
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
| `ticket_entitlement_template.changed` | success | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.changed_failed` | failure | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.manage` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.viewed` | success | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.view` | 接口级事件，以本附录为准 |
| `ticket_entitlement_template.viewed_failed` | failure | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | TIC-票务权益 | `ticket.entitlement.view` | 接口级事件，以本附录为准 |
| `ticket_issued_entitlement.viewed` | success | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_issued_entitlement.viewed_failed` | failure | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | TIC-票务权益 | `ticket.verify.view` | 接口级事件，以本附录为准 |
| `ticket_order.create_failed` | failure | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.created` | success | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.issued` | success | `issueTicketTypeDirectly` | `POST` | `/api/v1/ticketing/ticket-types/{id}/issue` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
| `ticket_order.issued_failed` | failure | `issueTicketTypeDirectly` | `POST` | `/api/v1/ticketing/ticket-types/{id}/issue` | TIC-票务权益 | `ticket.order.create` | 接口级事件，以本附录为准 |
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
| `ticket_product.view_failed` | failure | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_product.viewed` | success | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | TIC-票务权益 | `ticket.product.view` | 接口级事件，以本附录为准 |
| `ticket_refund.approve_failed` | failure | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.approved` | success | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | TIC-票务权益 | `ticket.refund.approve` | 接口级事件，以本附录为准 |
| `ticket_refund.request_failed` | failure | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket_refund.requested` | success | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | TIC-票务权益 | `ticket.refund.request` | 接口级事件，以本附录为准 |
| `ticket.issue_failed` | failure | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | TIC-票务权益 | `ticket.ticket.issue` | 接口级事件，以本附录为准 |
| `ticket.issued` | success | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | TIC-票务权益 | `ticket.ticket.issue` | 接口级事件，以本附录为准 |
| `ticket.verification_reverse_failed` | failure | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `ticket.verification_reversed` | success | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | TIC-票务权益 | `ticket.verify.reverse` | 接口级事件，以本附录为准 |
| `verification.failed` | failure | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.failed` | failure | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.previewed` | success | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | TIC-票务权益 | `verification.execute` | 接口级事件，以本附录为准 |
| `verification.succeeded` | success | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.execute` | 已在事件矩阵正文出现 |
| `verification.view_failed` | failure | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `verification.viewed` | success | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | TIC-票务权益 | `verification.view` | 接口级事件，以本附录为准 |
| `wecom.conversation_sync_failed` | failure | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | P0-待归类 | `wecom.sync` | 接口级事件，以本附录为准 |
| `wecom.conversation_synced` | success | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | P0-待归类 | `wecom.sync` | 接口级事件，以本附录为准 |
| `wecom.raw_view_failed` | failure | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | P0-待归类 | `wecom.view_raw` | 接口级事件，以本附录为准 |
| `wecom.raw_viewed` | success | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | P0-待归类 | `wecom.view_raw` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_failed` | failure | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_set` | success | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_view_failed` | failure | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |
| `wecom.retention_policy_viewed` | success | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | P0-待归类 | `wecom.retention.manage` | 接口级事件，以本附录为准 |

## 5. operationId 到任务、权限、事件、终端的追溯表

> 本节用于开发排期、测试用例、供应商联调和审计抽样。任务簇是执行归类，不替代 `p0-implementation-task-list.md` 中的正式任务编号；若某 operation 准备进入编码，必须在具体任务单中引用本表 operationId。

| 建议任务簇 | operationId | 方法 | 路径 | 摘要 | 权限 | 成功/失败事件 | 终端/调用通道 | 覆盖状态 |
|---|---|---|---|---|---|---|---|---|
| AMU-游乐安全 | `approveAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/approve` | Approve P2 amusement group booking and lock capacity | `amusement.group_booking.approve` | `amusement_group_booking.approved.success` / `amusement_group_booking.approved.failure` | web | operation 级补充 |
| AMU-游乐安全 | `approveAmusementReopen` | `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | 严重项恢复开放审批 | `amusement.safety_inspection.approve_reopen` | `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected` | web | operation 级补充 |
| AMU-游乐安全 | `confirmAmusementMaintenanceResume` | `POST` | `/api/v1/amusement/maintenance-records/{id}/confirm-resume` | 主管确认恢复营业 | `amusement.maintenance.confirm` | `amusement.project.resumed` / `amusement.maintenance_resume_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `createAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings` | Create P2 amusement group or private booking request | `amusement.group_booking.manage` | `amusement_group_booking.created.success` / `amusement_group_booking.created.failure` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementMaintenanceRecord` | `POST` | `/api/v1/amusement/maintenance-records` | 提交游乐定期检修 | `amusement.maintenance.submit` | `amusement.maintenance_recorded` / `amusement.maintenance_record_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `createAmusementManualOverride` | `POST` | `/api/v1/amusement/manual-overrides` | 游乐人工放行 | `amusement.verification.override` | `amusement.verification_overridden` / `amusement.verification_override_failed` | staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementProject` | `POST` | `/api/v1/amusement/projects` | 维护游乐项目 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementProjectRule` | `POST` | `/api/v1/amusement/projects/{id}/rules` | 创建游乐项目规则 | `amusement.project.manage` | `amusement.project_rule.changed` / `amusement.project_rule.changed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections` | 创建开放前安全巡检 | `amusement.safety_inspection.create` | `amusement.safety_inspection.created` / `amusement.safety_inspection.create_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `createAmusementSafetyTemplate` | `POST` | `/api/v1/amusement/safety-templates` | 维护巡检模板 | `amusement.safety_template.manage` | `amusement.safety_template.changed` / `amusement.safety_template.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementSession` | `POST` | `/api/v1/amusement/sessions` | 维护游乐场次和容量 | `amusement.session.manage` | `amusement.session.changed` / `amusement.session.change_failed` | web | operation 级补充 |
| AMU-游乐安全 | `createAmusementVerification` | `POST` | `/api/v1/amusement/verifications` | 游乐权益核销 | `amusement.verification.execute` | `amusement.verification_succeeded` / `amusement.verification_failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| AMU-游乐安全 | `getAmusementMaintenanceRecord` | `GET` | `/api/v1/amusement/maintenance-records/{id}` | 查询游乐定期检修记录详情 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `getAmusementOpenEligibility` | `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | 查询项目或场次安全与容量准入 | `amusement.safety_inspection.view` | `amusement.open_eligibility.viewed` / `amusement.open_eligibility.blocked` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementDeviceStatuses` | `GET` | `/api/v1/amusement/device-statuses` | 查询游乐设备状态 | `amusement.device_status.view` | `amusement.device_status.viewed` / `amusement.device_status.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementGroupBookings` | `GET` | `/api/v1/amusement/group-bookings` | List P2 amusement group and private bookings | `amusement.group_booking.view` | `amusement_group_booking.viewed.success` / `amusement_group_booking.viewed.failure` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementMaintenanceRecords` | `GET` | `/api/v1/amusement/maintenance-records` | 查询游乐定期检修记录 | `amusement.maintenance.read` | `amusement.maintenance.viewed` / `amusement.maintenance.view_failed` | kiosk, web, mobile_admin | operation 级补充 |
| AMU-游乐安全 | `listAmusementManualOverrides` | `GET` | `/api/v1/amusement/manual-overrides` | 查询游乐人工放行 | `amusement.verification.override` | `amusement.manual_override.viewed` / `amusement.manual_override.view_failed` | web, tablet | operation 级补充 |
| AMU-游乐安全 | `listAmusementProjects` | `GET` | `/api/v1/amusement/projects` | 查询游乐项目 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementQueueEntries` | `GET` | `/api/v1/amusement/queue-entries` | 查询游乐排队记录 | `amusement.queue.view` | `amusement.queue.viewed` / `amusement.queue.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementReservations` | `GET` | `/api/v1/amusement/reservations` | 查询游乐预约 | `amusement.reservation.view` | `amusement.reservation.viewed` / `amusement.reservation.viewed_failed` | web | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyInspections` | `GET` | `/api/v1/amusement/safety-inspections` | 查询安全巡检 | `amusement.safety_inspection.view` | `amusement.safety_inspection.viewed` / `amusement.safety_inspection.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSafetyTemplates` | `GET` | `/api/v1/amusement/safety-templates` | 查询巡检模板 | `amusement.safety_template.view` | `amusement.safety_template.viewed` / `amusement.safety_template.view_failed` | web, staff_work_phone | operation 级补充 |
| AMU-游乐安全 | `listAmusementSessions` | `GET` | `/api/v1/amusement/sessions` | 查询游乐场次和容量 | `amusement.session.view` | `amusement.session.viewed` / `amusement.session.view_failed` | web, staff_work_phone, kiosk | operation 级补充 |
| AMU-游乐安全 | `listAmusementVerifications` | `GET` | `/api/v1/amusement/verifications` | 查询游乐核销记录 | `amusement.verification.view` | `amusement.verification.viewed` / `amusement.verification.view_failed` | web, tablet | operation 级补充 |
| AMU-游乐安全 | `recheckAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | 提交复检结果 | `amusement.safety_inspection.recheck` | `amusement.safety_inspection.rechecked` / `amusement.safety_inspection.recheck_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `rectifyAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | 提交整改说明和证据 | `amusement.safety_inspection.rectify` | `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rectification_failed` | staff_work_phone, web | operation 级补充 |
| AMU-游乐安全 | `releaseAmusementGroupBooking` | `POST` | `/api/v1/amusement/group-bookings/{id}/release` | Release P2 amusement group booking and free locked capacity | `amusement.group_booking.manage` | `amusement_group_booking.released.success` / `amusement_group_booking.released.failure` | web | operation 级补充 |
| AMU-游乐安全 | `submitAmusementSafetyInspection` | `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | 提交巡检结果 | `amusement.safety_inspection.submit` | `amusement.safety_inspection.submitted` / `amusement.safety_inspection.blocked` | staff_work_phone, web | operation 级补充 |
| ANA-风控分析 | `createRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts` | 创建风险alert | `analytics.risk_alert.manage` | `risk_alert.created` / `risk_alert.create_failed` | web | operation 级补充 |
| ANA-风控分析 | `generateLocalLlmAiQualitySuggestion` | `POST` | `/api/v1/analytics/ai-quality/local-llm-suggestions` | Generate local LLM AI quality suggestion | `analytics.risk_alert.manage` | `analytics.ai_quality.local_llm_suggestion.created` / `analytics.ai_quality.local_llm_suggestion.failed` | web | operation 级补充 |
| ANA-风控分析 | `getAnalyticsDomainDashboard` | `GET` | `/api/v1/analytics/dashboards/{domain}` | P0 经营看板下钻（按域） | `analytics.dashboard.view` | `analytics.dashboard_domain.viewed` / `analytics.dashboard_domain.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getP0OverviewDashboard` | `GET` | `/api/v1/analytics/dashboards/p0-overview` | P0 经营总览 | `analytics.dashboard.view` | `analytics.dashboard_viewed` / `analytics.dashboard_view_failed` | web | operation 级补充 |
| ANA-风控分析 | `getRobotAnalytics` | `GET` | `/api/v1/analytics/robot` | 获取机器人经营分析 | `analytics.dashboard.view` | `analytics.robot.viewed` / `analytics.robot.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAiQualityResults` | `GET` | `/api/v1/analytics/ai-quality/results` | 查询ai质检结果 | `analytics.risk_alert.view` | `analytics.ai_quality_result.viewed` / `analytics.ai_quality_result.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAiQualityTasks` | `GET` | `/api/v1/analytics/ai-quality/tasks` | 查询ai质检任务 | `analytics.risk_alert.view` | `analytics.ai_quality_task.viewed` / `analytics.ai_quality_task.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listAnalyticsMerchants` | `GET` | `/api/v1/analytics/merchants` | 查询经营分析商户 | `analytics.dashboard.view` | `analytics.merchant_summary.viewed` / `analytics.merchant_summary.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listBusinessMetricSnapshots` | `GET` | `/api/v1/analytics/business-metric-snapshots` | List P2 business metric snapshots for operational analytics | `analytics.dashboard.view` | `analytics_business_metric_snapshot.viewed.success` / `analytics_business_metric_snapshot.viewed.failure` | web | operation 级补充 |
| ANA-风控分析 | `listCustomerIntents` | `GET` | `/api/v1/analytics/customer-intents` | 查询客户意向 | `analytics.risk_alert.view` | `analytics.customer_intent.viewed` / `analytics.customer_intent.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMerchantHealthScores` | `GET` | `/api/v1/analytics/merchant-health` | 查询商户健康度scores | `analytics.dashboard.view` | `analytics.merchant_health.viewed` / `analytics.merchant_health.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricAnomalies` | `GET` | `/api/v1/analytics/metric-anomalies` | 查询指标异常 | `analytics.risk_alert.view` | `analytics.metric_anomaly.viewed` / `analytics.metric_anomaly.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listMetricDefinitions` | `GET` | `/api/v1/analytics/metric-definitions` | 查询指标定义 | `analytics.dashboard.view` | `analytics.metric_definition.viewed` / `analytics.metric_definition.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskAlerts` | `GET` | `/api/v1/analytics/risk-alerts` | 风险预警 | `analytics.risk_alert.view` | `risk_alert.viewed` / `risk_alert.view_failed` | web | operation 级补充 |
| ANA-风控分析 | `listRiskRules` | `GET` | `/api/v1/analytics/risk-rules` | 查询风险规则 | `analytics.risk_alert.view` | `analytics.risk_rule.viewed` / `analytics.risk_rule.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `listStaffServiceScores` | `GET` | `/api/v1/analytics/staff-service-score` | 查询员工服务scores | `analytics.risk_alert.view` | `analytics.staff_service_score.viewed` / `analytics.staff_service_score.viewed_failed` | web | operation 级补充 |
| ANA-风控分析 | `resolveRiskAlert` | `POST` | `/api/v1/analytics/risk-alerts/{id}/resolve` | 处理风险alert | `analytics.risk_alert.manage` | `analytics.risk_alert.resolved` / `analytics.risk_alert.resolved_failed` | web | operation 级补充 |
| AUTH-登录会话 | `login` | `POST` | `/api/v1/auth/login` | 员工或商户员工登录 | `public` | `auth.login.succeeded` / `auth.login.failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | 已在任务映射正文出现 |
| AUTH-登录会话 | `logout` | `POST` | `/api/v1/auth/logout` | 退出登录 | `authenticated` | `auth.logout` / `auth.logout_failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | 已在任务映射正文出现 |
| AUTH-登录会话 | `refreshToken` | `POST` | `/api/v1/auth/refresh` | 刷新会话 | `authenticated` | `auth.token.refreshed` / `auth.token_refresh_failed` | web, merchant_portal, staff_work_phone, tablet, kiosk | operation 级补充 |
| AUTH-账号权限 | `acknowledgeEmployeeNotice` | `POST` | `/api/v1/staff/notices/{id}/acknowledge` | 确认/同意员工通知 | `staff.notice.view` | `staff.notice_acknowledged` / `staff.notice_acknowledge_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `applyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/apply` | 应用权限模板 | `access.permission_template.apply` | `permission_template.applied` / `permission_template.apply_failed` | web | operation 级补充 |
| AUTH-账号权限 | `approveStaffDeviceRebind` | `POST` | `/api/v1/staff/device-credentials/{id}/approve-rebind` | 审批换机生效 | `staff.device.review` | `staff_device.rebound` / `staff_device.rebind_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `assignStaffLevel` | `PUT` | `/api/v1/access/users/{id}/staff-level` | 分配员工职级与薪资 | `access.staff_level.manage` | `access.staff_level.assigned` / `access.staff_level.assign_failed` | web | operation 级补充 |
| AUTH-账号权限 | `bindStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials` | 绑定/换机申请员工设备凭证 | `staff.device.bind` | `staff_device.bound` / `staff_device.bind_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `copyPermissionTemplate` | `POST` | `/api/v1/access/permission-templates/{id}/copy` | 复制权限模板 | `access.permission_template.manage` | `permission_template.copied` / `permission_template.copy_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createAccessUser` | `POST` | `/api/v1/access/users` | 创建权限用户 | `access.user.manage` | `access.user.changed` / `access.user.changed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createPermissionTemplate` | `POST` | `/api/v1/access/permission-templates` | 创建权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createRole` | `POST` | `/api/v1/access/roles` | 创建角色 | `access.role.manage` | `role.changed` / `role.change_failed` | web | operation 级补充 |
| AUTH-账号权限 | `createTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants` | 临时授权 | `access.temporary_grant.create` | `permission_template.applied` / `temporary_grant.create_failed` | web | operation 级补充 |
| AUTH-账号权限 | `getEffectivePermissions` | `GET` | `/api/v1/access/effective-permissions` | 查询当前账号有效权限集 | `authenticated` | `access.effective_permission.viewed` / `access.effective_permission.viewed_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `getUserEffectivePermissions` | `GET` | `/api/v1/access/users/{id}/effective-permissions` | 查询指定账号有效权限集 | `access.user.view` | `access.effective_permission.viewed` / `access.effective_permission.viewed_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `listAccessUsers` | `GET` | `/api/v1/access/users` | 查询权限用户 | `access.user.view` | `access.user.viewed` / `access.user.viewed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listEmployeeNotices` | `GET` | `/api/v1/staff/notices` | 查询员工通知 | `staff.notice.view` | `staff.notice.viewed` / `staff.notice.view_failed` | staff_private_phone, web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `listPermissions` | `GET` | `/api/v1/access/permissions` | 权限点列表 | `access.permission.view` | `permission.viewed` / `permission.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listPermissionTemplates` | `GET` | `/api/v1/access/permission-templates` | 权限模板列表 | `access.permission_template.view` | `permission_template.viewed` / `permission_template.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listRoles` | `GET` | `/api/v1/access/roles` | 角色列表 | `access.role.view` | `role.viewed` / `role.view_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffLevels` | `GET` | `/api/v1/access/staff-levels` | 查询职级目录 | `access.staff_level.view` | `access.staff_level.viewed` / `access.staff_level.viewed_failed` | web | operation 级补充 |
| AUTH-账号权限 | `listStaffProfiles` | `GET` | `/api/v1/staff/profiles` | 查询员工档案 | `staff.profile.view` | `staff.profile.viewed` / `staff.profile.view_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `reportStaffDeviceIntegrity` | `POST` | `/api/v1/staff/device-credentials/{id}/report-integrity` | 端侧完整性上报 | `staff.device.bind` | `staff_device.integrity_reported` / `staff_device.integrity_report_failed` | staff_private_phone | operation 级补充 |
| AUTH-账号权限 | `reviewStaffRegistration` | `POST` | `/api/v1/staff/registrations/{id}/review` | 审批员工注册 | `staff.profile.review` | `staff.approved` / `staff.review_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeStaffDeviceCredential` | `POST` | `/api/v1/staff/device-credentials/{id}/revoke` | 解绑/失效设备凭证 | `staff.device.revoke` | `staff_device.revoked` / `staff_device.revoke_failed` | web, mobile_admin | operation 级补充 |
| AUTH-账号权限 | `revokeTemporaryGrant` | `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | 撤销临时授权 | `access.temporary_grant.revoke` | `permission_template.revoked` / `temporary_grant.revoke_failed` | web | operation 级补充 |
| AUTH-账号权限 | `submitStaffRegistration` | `POST` | `/api/v1/staff/registrations` | 员工自助注册 | `staff.profile.submit` | `staff.registered` / `staff.register_failed` | staff_private_phone, web | operation 级补充 |
| AUTH-账号权限 | `updatePermissionTemplate` | `PUT` | `/api/v1/access/permission-templates/{id}` | 修改权限模板 | `access.permission_template.manage` | `permission_template.changed` / `permission_template.change_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `addBarOrderItem` | `POST` | `/api/v1/bar/orders/{order_id}/items` | 新增酒吧订单物品 | `bar.order.create` | `bar.order_item.created` / `bar.order_item.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `calculateBarTableCommission` | `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | 卡座销售提成计算 | `bar.table_commission.calculate` | `bar.table_commission.calculated` / `bar.table_commission.calculate_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `cancelBarReservation` | `POST` | `/api/v1/bar/reservations/{id}/cancel` | 订台取消/爽约（按时限退定金或没收） | `bar.reservation.manage` | `bar.reservation.cancelled` / `bar.reservation.cancel_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `checkInBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/checkin` | 订台到店核销（定金转抵扣进账单） | `bar.reservation.manage` | `bar.reservation_deposit_deducted` / `bar.reservation_deposit_deduct_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `checkInBarReservationGuest` | `POST` | `/api/v1/bar/reservations/{id}/check-in-guest` | 卡座免通票入场名额核销 | `bar.table.guest_checkin` | `bar.table_guest.checked_in` / `bar.table_guest.rejected` | staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `createBarAgeVerification` | `POST` | `/api/v1/bar/age-verifications` | 酒吧年龄和人脸核验 | `bar.entry.age_verify` | `bar.age_verification.passed` / `bar.age_verification.rejected` | staff_work_phone<br>device: age-verification-device | operation 级补充 |
| BAR-酒吧经营 | `createBarBillPayIntent` | `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | 酒吧账单支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `createBarDepositRule` | `POST` | `/api/v1/bar/deposit-rules` | 创建订台定金规则（固定额/按低消比例） | `bar.deposit_rule.manage` | `bar.deposit_rule.created` / `bar.deposit_rule.create_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `createBarOrder` | `POST` | `/api/v1/bar/orders` | 点单 | `bar.order.create` | `bar.order_created` / `bar.order_create_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `createBarOrderBill` | `POST` | `/api/v1/bar/orders/{order_id}/bill` | 创建酒吧订单账单 | `bar.order.create` | `bar.bill.created` / `bar.bill.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarProduct` | `POST` | `/api/v1/bar/products` | 创建酒吧商品 | `bar.order.create` | `bar.product.created` / `bar.product.created_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `createBarReservation` | `POST` | `/api/v1/bar/reservations` | 订台或卡座预订 | `bar.reservation.create` | `bar.reservation.created` / `bar.reservation.create_failed` | web, tablet, miniapp, app, kiosk | operation 级补充 |
| BAR-酒吧经营 | `createBarTable` | `POST` | `/api/v1/bar/tables` | 创建桌台或卡座 | `bar.table.manage` | `bar.table.changed` / `bar.table.change_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `deliverBarOrder` | `POST` | `/api/v1/bar/orders/{order_id}/deliver` | 出酒或送达确认 | `bar.order.create` | `bar.order_delivered` / `bar.order_deliver_failed` | tablet, staff_work_phone | operation 级补充 |
| BAR-酒吧经营 | `listBarOrders` | `GET` | `/api/v1/bar/orders` | 查询酒吧订单 | `bar.order.view` | `bar.order.viewed` / `bar.order.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarProductionTickets` | `GET` | `/api/v1/bar/production-tickets` | 查询酒吧出品票据 | `bar.order.create` | `bar.production_ticket.viewed` / `bar.production_ticket.viewed_failed` | web | operation 级补充 |
| BAR-酒吧经营 | `listBarProducts` | `GET` | `/api/v1/bar/products` | 查询酒吧商品 | `bar.order.create` | `bar.product.viewed` / `bar.product.viewed_failed` | web, tablet, kiosk | operation 级补充 |
| BAR-酒吧经营 | `listBarReservations` | `GET` | `/api/v1/bar/reservations` | 查询酒吧预约 | `bar.reservation.view` | `bar.reservation.viewed` / `bar.reservation.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `listBarTables` | `GET` | `/api/v1/bar/tables` | 查询桌台或卡座 | `bar.table.view` | `bar.table.viewed` / `bar.table.view_failed` | web, tablet | operation 级补充 |
| BAR-酒吧经营 | `openBarTable` | `POST` | `/api/v1/bar/tables/{table_id}/open` | 开台 | `bar.table.open_close` | `bar.table.opened` / `bar.table.open_failed` | tablet, web | operation 级补充 |
| BAR-酒吧经营 | `payBarReservationDeposit` | `POST` | `/api/v1/bar/reservations/{id}/pay-deposit` | 订台定金支付 | `bar.reservation.manage` | `bar.reservation_deposit_paid` / `bar.reservation_deposit_pay_failed` | web, kiosk, tablet, mobile_admin | operation 级补充 |
| BAR-酒吧经营 | `settleBarBill` | `POST` | `/api/v1/bar/bills/{bill_id}/settle` | 账单结清 | `finance.payment.register` | `bar.bill.settled` / `bar.bill.settle_failed` | tablet, web | operation 级补充 |
| CUS-客人端 | `createCustomerConsent` | `POST` | `/api/v1/customer/consents` | 隐私同意记录 | `not_applicable` | `customer.consent.created` / `customer.consent.create_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `customerCheckIn` | `POST` | `/api/v1/customer/check-ins` | 客人每日签到 | `not_applicable` | `customer.checked_in` / `customer.check_in_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerMembershipBenefits` | `GET` | `/api/v1/customer/membership-benefits` | 客人当前生效会员权益 | `customer.membership.view` | `customer.membership_benefits_viewed` / `customer.membership_benefits_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerMembershipSubscribe` | `POST` | `/api/v1/customer/memberships/subscribe` | 客人付费会员开通（绑支付意图） | `customer.membership.subscribe` | `customer.membership.subscribe` / `customer.membership.subscribe_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerPhoneLogin` | `POST` | `/api/v1/customer/auth/phone-login` | 客人手机号登录 | `public` | `customer.auth.login` / `customer.auth.login_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `customerPointsTransfer` | `POST` | `/api/v1/customer/points/transfers` | 客人积分转赠（风控防套现） | `customer.points.transfer` | `points.transferred` / `points.transferred_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueAccount` | `GET` | `/api/v1/customer/stored-value/account` | 客人储值账户查询 | `customer.stored_value.view` | `customer.stored_value_account_viewed` / `customer.stored_value_account_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueConsume` | `POST` | `/api/v1/customer/stored-value/consume` | 客人储值消费（余额扣减） | `customer.stored_value.consume` | `customer.stored_value.consume` / `customer.stored_value.consume_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueSettle` | `POST` | `/api/v1/customer/stored-value/settle` | 客人储值充值入账（支付成功后） | `customer.stored_value.topup` | `customer.stored_value.settle` / `customer.stored_value.settle_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `customerStoredValueTopup` | `POST` | `/api/v1/customer/stored-value/topup` | 客人储值充值（绑支付意图） | `customer.stored_value.topup` | `customer.stored_value.topup` / `customer.stored_value.topup_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `evaluateMemberLevel` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/evaluate-level` | 手动触发会员等级评估 | `customer.loyalty.evaluate` | `member.level_changed` / `customer.level_evaluate_failed` | web | operation 级补充 |
| CUS-客人端 | `freezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/freeze` | 冻结会员积分账户 | `customer.points.freeze` | `customer.points.frozen` / `customer.points.freeze_failed` | web | operation 级补充 |
| CUS-客人端 | `freezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/freeze` | 冻结会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.frozen` / `stored_value.frozen_failed` | web | operation 级补充 |
| CUS-客人端 | `getCustomerPoints` | `GET` | `/api/v1/customer/points` | 积分余额 | `not_applicable` | `customer.points_viewed` / `customer.points_view_failed` | web, miniapp, app | 已在任务映射正文出现 |
| CUS-客人端 | `listCustomerCheckIns` | `GET` | `/api/v1/customer/check-ins` | 客人签到记录 | `not_applicable` | `customer.check_ins_viewed` / `customer.check_ins_view_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerMembershipPlans` | `GET` | `/api/v1/customer/membership-plans` | 付费会员档位列表（客人端） | `not_applicable` | `customer.membership_plans_viewed` / `customer.membership_plans_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerMemberships` | `GET` | `/api/v1/customer/memberships` | 客人付费会员列表 | `customer.membership.view` | `customer.memberships_viewed` / `customer.memberships_viewed_failed` | web, miniapp, app | operation 级补充 |
| CUS-客人端 | `listCustomerOrders` | `GET` | `/api/v1/customer/orders` | 我的订单 | `not_applicable` | `customer.orders_viewed` / `customer.orders_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS-客人端 | `listMemberLevels` | `GET` | `/api/v1/loyalty/member-levels` | 会员等级配置列表 | `customer.loyalty.view` | `customer.member_levels_viewed` / `customer.member_levels_view_failed` | web | operation 级补充 |
| CUS-客人端 | `listMembershipPlans` | `GET` | `/api/v1/loyalty/membership-plans` | 付费会员档位列表（后台） | `customer.membership.view` | `customer.membership_plans_admin_viewed` / `customer.membership_plans_admin_viewed_failed` | web | operation 级补充 |
| CUS-客人端 | `refundStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/refund` | 会员储值退卡/退余额（后台） | `customer.stored_value.refund` | `stored_value.refund` / `stored_value.refund_failed` | web | operation 级补充 |
| CUS-客人端 | `saveMembershipPlan` | `POST` | `/api/v1/loyalty/membership-plans` | 保存付费会员档位（后台） | `customer.membership.manage` | `membership.plan_saved` / `membership.plan_saved_failed` | web | operation 级补充 |
| CUS-客人端 | `savePointsTransferLimit` | `POST` | `/api/v1/loyalty/points-transfer-limits` | 保存积分转赠风控配置（后台） | `customer.points.transfer_limit` | `points.transfer_limit_saved` / `points.transfer_limit_saved_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezePointsAccount` | `POST` | `/api/v1/loyalty/points-accounts/{customer_id}/unfreeze` | 解冻会员积分账户 | `customer.points.freeze` | `customer.points.unfrozen` / `customer.points.unfreeze_failed` | web | operation 级补充 |
| CUS-客人端 | `unfreezeStoredValueAccount` | `POST` | `/api/v1/loyalty/stored-value/{customer_id}/unfreeze` | 解冻会员储值账户（后台） | `customer.stored_value.freeze` | `stored_value.unfrozen` / `stored_value.unfrozen_failed` | web | operation 级补充 |
| CUS/BAR-客人订台 | `createCustomerBarReservation` | `POST` | `/api/v1/customer/bar-reservations` | 客人卡座预订 | `not_applicable` | `customer.bar_reservation.created` / `customer.bar_reservation.create_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/BAR-客人订台 | `listCustomerBarReservationOptions` | `GET` | `/api/v1/customer/bar-reservation-options` | 客人可订台资源 | `not_applicable` | `customer.bar_reservation_options_viewed` / `customer.bar_reservation_options_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/TIC-客人购票 | `createCustomerTicketOrder` | `POST` | `/api/v1/customer/ticket-orders` | 客人购票下单 | `not_applicable` | `customer.ticket_order.created` / `customer.ticket_order.create_failed` | web, miniapp, app, kiosk | operation 级补充 |
| CUS/TIC-客人购票 | `getCustomerTicketQr` | `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | 我的动态二维码 | `not_applicable` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | web, miniapp, app | operation 级补充 |
| CUS/TIC-客人购票 | `listCustomerTicketProducts` | `GET` | `/api/v1/customer/ticket-products` | 客人可购票种和票价 | `public` | `customer.ticket_products_viewed` / `customer.ticket_products_view_failed` | web, miniapp, app, kiosk | operation 级补充 |
| EVD-事件证据 | `bindEvidenceAttachment` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | 关联证据附件到业务对象 | `evidence.attachment.create` | `evidence.attachment_bound` / `evidence.attachment_bind_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `completeEvidenceAttachmentUpload` | `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | 完成上传确认 | `evidence.attachment.create` | `evidence.attachment_completed` / `evidence.attachment_complete_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `createEvidenceAttachmentUploadIntent` | `POST` | `/api/v1/evidence/attachments/upload-intents` | 申请证据附件预签名上传 | `evidence.attachment.create` | `evidence.upload_intent_created` / `evidence.upload_intent_failed` | web, staff_work_phone, tablet | operation 级补充 |
| EVD-事件证据 | `createEvidenceEvents` | `POST` | `/api/v1/evidence/events` | 创建证据事件 | `evidence.event.create` | `evidence.events_reported` / `evidence.events_report_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| EVD-事件证据 | `createEvidenceReview` | `POST` | `/api/v1/evidence/reviews` | 风险或异常复核 | `evidence.review.create` | `evidence.review_created` / `evidence.review_create_failed` | web | operation 级补充 |
| EVD-事件证据 | `getEvidenceTimeline` | `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | 业务时间线 | `evidence.timeline.view` | `evidence.timeline_viewed` / `evidence.timeline_view_failed` | web | operation 级补充 |
| EVD-事件证据 | `listEvidenceEvents` | `GET` | `/api/v1/evidence/events` | 事件查询 | `evidence.event.view` | `evidence.event_viewed` / `evidence.event_view_failed` | web | operation 级补充 |
| FIN-财务台账 | `approveArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments/{id}/approve` | 锁定后补录审批决定 | `finance.locked_backfill.approve` | `finance.locked_backfill_approved` / `finance.locked_backfill_decision_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `confirmOfflinePayment` | `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | 现金或 POS 线下收款确认 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `createArApItem` | `POST` | `/api/v1/finance/ar-ap-items` | 手工新增应收应付台账项 | `finance.ar_ap.manage` | `finance_ar_ap.created` / `finance_ar_ap.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createArApPayment` | `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | 应收应付收付款登记 | `finance.payment.register` | `finance_payment.registered` / `finance_payment.register_failed` | web | operation 级补充 |
| FIN-财务台账 | `createDailyClose` | `POST` | `/api/v1/finance/daily-closes` | 日结锁定 | `finance.daily_close.lock` | `finance_daily_close.locked` / `finance_daily_close.lock_failed` | web | operation 级补充 |
| FIN-财务台账 | `createFinanceRefund` | `POST` | `/api/v1/finance/refunds` | 创建财务退款记录 | `finance.refund.create` | `finance_refund.created` / `finance_refund.create_failed` | web | operation 级补充 |
| FIN-财务台账 | `createPaymentIntent` | `POST` | `/api/v1/finance/payment-intents` | 创建聚合支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, tablet, kiosk, miniapp, app | operation 级补充 |
| FIN-财务台账 | `listArApItems` | `GET` | `/api/v1/finance/ar-ap-items` | 查询应收应付台账 | `finance.ar_ap.view` | `finance_ar_ap.viewed` / `finance_ar_ap.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `listDailyCloseSummaries` | `GET` | `/api/v1/finance/daily-closes/{id}/summaries` | 查询日结六源汇总 | `finance.daily_close.manage` | `finance.daily_close_summary.viewed` / `finance.daily_close_summary.view_failed` | web, mobile_admin | operation 级补充 |
| FIN-财务台账 | `listReconciliationBatches` | `GET` | `/api/v1/finance/reconciliation/batches` | 查询对账批次 | `finance.reconciliation.view` | `finance.reconciliation_batch.viewed` / `finance.reconciliation_batch.viewed_failed` | web | operation 级补充 |
| FIN-财务台账 | `listReconciliationDifferences` | `GET` | `/api/v1/finance/reconciliation/differences` | 对账差异 | `openplatform.reconcile.execute` | `finance_reconciliation.difference_found` / `finance_reconciliation.view_failed` | web | operation 级补充 |
| FIN-财务台账 | `requestArApAdjustment` | `POST` | `/api/v1/finance/ar-ap-adjustments` | 锁定后补录审批申请 | `finance.locked_backfill.request` | `finance.locked_backfill_requested` / `finance.locked_backfill_request_failed` | web, mobile_admin | operation 级补充 |
| GOV-平台治理 | `approveContract` | `POST` | `/api/v1/platform/contracts/{contract_id}/approve` | 合同审批流 | `contract.lifecycle.approve` | `contract.lifecycle_changed` / `contract.lifecycle_change_failed` | web | operation 级补充 |
| GOV-平台治理 | `approveSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/{review_id}/approve` | 主管审批或驳回复核记录 | `governance.supervisor_review.approve` | `governance.supervisor_review.decided` / `governance.supervisor_review.decide_failed` | web | operation 级补充 |
| GOV-平台治理 | `changeBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | 变更四类主体绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntity` | `POST` | `/api/v1/platform/business-entities` | 创建经营、运营、合同、结算主体档案 | `entity.binding.manage` | `business_entity.changed` / `business_entity.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createBusinessEntityBinding` | `POST` | `/api/v1/platform/business-entity-bindings` | 创建四类主体唯一绑定 | `entity.binding.manage` | `entity_binding.changed` / `entity_binding.change_failed` | web | operation 级补充 |
| GOV-平台治理 | `createContractDraft` | `POST` | `/api/v1/platform/contracts` | 创建合同草稿和归档附件 | `contract.draft.manage` | `contract.draft_saved` / `contract.draft_save_failed` | web | operation 级补充 |
| GOV-平台治理 | `createMerchant` | `POST` | `/api/v1/platform/merchants` | 创建商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `createProject` | `POST` | `/api/v1/platform/projects` | 创建项目 | `governance.project.manage` | `project.created` / `project.create_failed` | web | operation 级补充 |
| GOV-平台治理 | `createSpace` | `POST` | `/api/v1/platform/spaces` | 创建场馆、区域、点位 | `governance.space.manage` | `space.updated` / `space.update_failed` | web | operation 级补充 |
| GOV-平台治理 | `evaluateSupervisorReview` | `POST` | `/api/v1/platform/supervisor-review/evaluate` | 评估业务动作是否需要主管复核 | `governance.supervisor_review.manage` | `governance.supervisor_review.evaluated` / `governance.supervisor_review.evaluate_failed` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntities` | `GET` | `/api/v1/platform/business-entities` | 查询经营、运营、合同、结算主体档案 | `entity.binding.view` | `business_entity.viewed` / `business_entity.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listBusinessEntityBindings` | `GET` | `/api/v1/platform/business-entity-bindings` | 查询四类主体绑定 | `entity.binding.view` | `entity_binding.viewed` / `entity_binding.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listContracts` | `GET` | `/api/v1/platform/contracts` | 查询合同摘要和附件归档 | `contract.view` | `contract.viewed` / `contract.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listMerchants` | `GET` | `/api/v1/platform/merchants` | 查询商户 | `merchant.profile.view` | `merchant.viewed` / `merchant.view_failed` | web, merchant_portal | operation 级补充 |
| GOV-平台治理 | `listProjects` | `GET` | `/api/v1/platform/projects` | 查询综合体项目 | `governance.project.view` | `project.viewed` / `project.view_failed` | web | operation 级补充 |
| GOV-平台治理 | `listSpaces` | `GET` | `/api/v1/platform/spaces` | 查询场馆、区域、点位 | `governance.space.view` | `space.viewed` / `space.view_failed` | web, merchant_portal | operation 级补充 |
| GOV-平台治理 | `listSupervisorReviewPolicies` | `GET` | `/api/v1/platform/supervisor-review-policies` | 查询主管复核与违规阻断策略 | `governance.supervisor_review.view` | `governance.supervisor_review.policies_viewed` / `governance.supervisor_review.policies_view_failed` | web | operation 级补充 |
| GOV-平台治理 | `updateMerchant` | `PUT` | `/api/v1/platform/merchants/{merchant_id}` | 修改商户 | `merchant.profile.manage` | `merchant.updated` / `merchant.update_failed` | web | operation 级补充 |
| INT-第三方集成 | `createProviderAdapter` | `POST` | `/api/v1/integration/provider-adapters` | 创建供应商适配器占位或 mock adapter | `integration.provider_adapter.create` | `provider_adapter.created` / `provider_adapter.create_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationCompensationTasks` | `GET` | `/api/v1/integration/compensation-tasks` | 查询第三方集成补偿任务 | `integration.provider_adapter.view` | `integration.compensation_task.viewed` / `integration.compensation_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationDeviceEvents` | `GET` | `/api/v1/integration/devices/events` | 查询第三方集成设备事件 | `integration.provider_adapter.view` | `integration.device_event.viewed` / `integration.device_event.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationDevices` | `GET` | `/api/v1/integration/devices` | 查询第三方集成设备 | `integration.provider_adapter.view` | `integration.device.viewed` / `integration.device.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationOfflineSyncBatches` | `GET` | `/api/v1/integration/devices/offline-sync-batches` | 查询第三方集成离线批次 | `integration.provider_adapter.view` | `integration.offline_sync_batch.viewed` / `integration.offline_sync_batch.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationOrderSyncs` | `GET` | `/api/v1/integration/order-syncs` | 查询第三方集成订单syncs | `integration.provider_adapter.view` | `integration.order_sync.viewed` / `integration.order_sync.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationRetailSkus` | `GET` | `/api/v1/integration/retail/skus` | 查询第三方集成零售SKU | `integration.provider_adapter.view` | `integration.retail_sku.viewed` / `integration.retail_sku.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationRetryTasks` | `GET` | `/api/v1/integration/retry-tasks` | 查询第三方集成retry任务 | `integration.provider_adapter.view` | `integration.retry_task.viewed` / `integration.retry_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listIntegrationVideoAnchors` | `GET` | `/api/v1/integration/video-anchors` | 调阅视频点位时间锚点（敏感，写调阅审计） | `integration.video.view` | `integration.video_clip_viewed` / `integration.video_clip_view_failed` | web, mobile_admin | operation 级补充 |
| INT-第三方集成 | `listProviderAdapters` | `GET` | `/api/v1/integration/provider-adapters` | 供应商适配器查询 | `integration.provider_adapter.view` | `provider_adapter.viewed` / `provider_adapter.view_failed` | web | operation 级补充 |
| INT-第三方集成 | `listRobotTaskEvents` | `GET` | `/api/v1/integration/robot/task-events` | 查询机器人task事件 | `integration.provider_adapter.view` | `integration.robot_task_event.viewed` / `integration.robot_task_event.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listRobotTasks` | `GET` | `/api/v1/integration/robot/tasks` | 查询机器人任务 | `integration.provider_adapter.view` | `integration.robot_task.viewed` / `integration.robot_task.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listVideoCameras` | `GET` | `/api/v1/integration/video/cameras` | 查询视频摄像头 | `integration.provider_adapter.view` | `integration.video_camera.viewed` / `integration.video_camera.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listVoucherVerifications` | `GET` | `/api/v1/integration/voucher-verifications` | 查询券核销记录 | `integration.provider_adapter.view` | `integration.voucher_verification.viewed` / `integration.voucher_verification.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomBindings` | `GET` | `/api/v1/integration/wecom/bindings` | 查询企业微信绑定 | `integration.provider_adapter.view` | `integration.wecom_binding.viewed` / `integration.wecom_binding.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomConversations` | `GET` | `/api/v1/integration/wecom/conversations` | 查询企业微信会话 | `integration.provider_adapter.view` | `integration.wecom_conversation.viewed` / `integration.wecom_conversation.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomMessages` | `GET` | `/api/v1/integration/wecom/messages` | 查询企业微信消息 | `integration.provider_adapter.view` | `integration.wecom_message.viewed` / `integration.wecom_message.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `listWecomRiskHits` | `GET` | `/api/v1/integration/wecom/risk-hits` | 查询企业微信风险命中 | `integration.provider_adapter.view` | `integration.wecom_risk_hit.viewed` / `integration.wecom_risk_hit.viewed_failed` | web | operation 级补充 |
| INT-第三方集成 | `markProviderAdapterProductionReady` | `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | 标记供应商适配器生产就绪 | `integration.provider_adapter.production_ready` | `provider_adapter.production_ready_marked` / `provider_adapter.production_ready_failed` | web | operation 级补充 |
| INT-第三方集成 | `receiveIntegrationTypedWebhook` | `POST` | `/api/v1/integration/{type}/webhook` | 第三方统一 webhook 入口（开关门控） | `not_applicable` | `integration.webhook_received` / `integration.webhook_rejected` | <br>channel: provider-webhook | operation 级补充 |
| INT-第三方集成 | `reportIntegrationRobotTaskEvent` | `POST` | `/api/v1/integration/robot/report` | 机器人任务事件上报（被动留痕） | `not_applicable` | `integration.robot_reported` / `integration.robot_report_failed` | <br>channel: provider-webhook | operation 级补充 |
| INT-第三方集成 | `testProviderAdapterMock` | `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | mock adapter 契约测试 | `integration.provider_adapter.test` | `provider_adapter.tested` / `provider_adapter.test_failed` | web | operation 级补充 |
| INT-第三方集成 | `updateProviderAdapter` | `PUT` | `/api/v1/integration/provider-adapters/{id}` | 更新供应商适配器配置 | `integration.provider_adapter.update` | `open_mapping.changed` / `provider_adapter.update_failed` | web | operation 级补充 |
| INV-库存存酒 | `approvePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/approve` | 采购单审批(created/submitted→approved/rejected) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `approveSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts/{id}/approve` | 审批供应合同(draft→approved/rejected) | `inventory.purchase.approve` | `supply_contract.approved` / `supply_contract.approve_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `arrivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/arrive` | 采购单到货(ordered/expected→arrived) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `cancelSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/cancel` | 存酒作废(高价值→照片+视频双证据建复核;非高价值直接作废) | `bar.saved_bottle.manage` | `bar.saved_bottle.cancelled` / `bar.saved_bottle.cancel_failed` | tablet, web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `claimSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | 存酒领取 | `bar.saved_bottle.manage` | `bar.saved_bottle_picked_up` / `bar.saved_bottle.pickup_failed` | tablet, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createInventoryItem` | `POST` | `/api/v1/inventory/items` | 创建库存物品 | `inventory.stock.move` | `inventory.item.created` / `inventory.item.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createInventorySupplier` | `POST` | `/api/v1/inventory/suppliers` | 创建供应商 | `inventory.supplier.manage` | `inventory.supplier_changed` / `inventory.supplier_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests` | 创建采购申请 | `inventory.purchase_request.manage` | `inventory.purchase_request_changed` / `inventory.purchase_request_change_failed` | web | operation 级补充 |
| INV-库存存酒 | `createSavedBottle` | `POST` | `/api/v1/inventory/saved-bottles` | 存酒登记 | `bar.saved_bottle.manage` | `bar.saved_bottle.created` / `bar.saved_bottle.create_failed` | tablet, web | operation 级补充 |
| INV-库存存酒 | `createStockAdjustment` | `POST` | `/api/v1/inventory/stock-adjustments` | 盘点差异、报损、报溢 | `inventory.stock.adjust` | `inventory.counted` / `inventory.adjustment_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockIn` | `POST` | `/api/v1/inventory/stock-in` | 入库 | `inventory.stock.move` | `inventory.inbound_confirmed` / `inventory.inbound_failed` | web, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `createStockMovement` | `POST` | `/api/v1/inventory/stock-movements` | 库存移动或扣减 | `inventory.stock.move` | `inventory.outbound_verified` / `inventory.stock_movement_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `createSupplyContract` | `POST` | `/api/v1/inventory/supply-contracts` | 创建供应合同(三模式) | `inventory.supply_contract.manage` | `supply_contract.created` / `supply_contract.create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createTraceHandover` | `POST` | `/api/v1/inventory/trace-handovers` | 创建扫码交接任务(五段链) | `inventory.handover.scan` | `inventory.handover_created` / `inventory.handover_create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `createTypedStockMovement` | `POST` | `/api/v1/inventory/stock-movements/{movement_type}` | 创建typed库存流水 | `inventory.stock.move` | `inventory.stock_movement.created` / `inventory.stock_movement.created_failed` | web | operation 级补充 |
| INV-库存存酒 | `createWarehouse` | `POST` | `/api/v1/inventory/warehouses` | 创建结构化库房(六类) | `inventory.warehouse.manage` | `inventory.warehouse.created` / `inventory.warehouse.create_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listHighValueReviews` | `GET` | `/api/v1/inventory/high-value-reviews` | 查询库存域高价值复核单(报损/盘点差异/存酒作废) | `inventory.high_value.review` | `inventory.high_value_review.viewed` / `inventory.high_value_review.view_failed` | web, tablet, mobile_admin | operation 级补充 |
| INV-库存存酒 | `listInventoryItems` | `GET` | `/api/v1/inventory/items` | 查询库存明细 | `inventory.stock.view` | `inventory.item.viewed` / `inventory.item.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listInventorySuppliers` | `GET` | `/api/v1/inventory/suppliers` | 查询供应商 | `inventory.supplier.view` | `inventory.supplier_viewed` / `inventory.supplier_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listPurchaseRequests` | `GET` | `/api/v1/inventory/purchase-requests` | 查询采购申请 | `inventory.purchase_request.view` | `inventory.purchase_request_viewed` / `inventory.purchase_request_view_failed` | web | operation 级补充 |
| INV-库存存酒 | `listSavedBottles` | `GET` | `/api/v1/inventory/saved-bottles` | 查询存酒 | `bar.saved_bottle.manage` | `bar.saved_bottle.viewed` / `bar.saved_bottle.view_failed` | web, tablet, staff_work_phone | operation 级补充 |
| INV-库存存酒 | `listStockBalances` | `GET` | `/api/v1/inventory/stock-balances` | 查询库存余额 | `inventory.stock.view` | `inventory.stock_balance.viewed` / `inventory.stock_balance.viewed_failed` | web | operation 级补充 |
| INV-库存存酒 | `listStockMovements` | `GET` | `/api/v1/inventory/stock-movements` | 查询库存流水 | `inventory.stock.view` | `inventory.stock_movement.viewed` / `inventory.stock_movement.view_failed` | web, staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `orderPurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/order` | 采购单下单(approved→ordered) | `inventory.purchase.approve` | `inventory.purchase_status_changed` / `inventory.purchase_status_change_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `receivePurchaseRequest` | `POST` | `/api/v1/inventory/purchase-requests/{id}/receive` | 采购单入库确认(arrived→received,生成入库流水) | `inventory.purchase.approve` | `inventory.received` / `inventory.received_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reportRobotSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/robot-report` | 机器人配送段上报(被动留痕,受第三方开关门控) | `inventory.handover.scan` | `inventory.handover_robot_reported` / `inventory.handover_robot_report_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewHighValue` | `POST` | `/api/v1/inventory/high-value-reviews/{id}/review` | 主管复核高价值动作(approve生效/reject回滚) | `inventory.high_value.review` | `inventory.high_value_review_decided` / `inventory.high_value_review_decide_failed` | web, tablet, mobile_admin | operation 级补充 |
| INV-库存存酒 | `reviewTraceHandoverException` | `POST` | `/api/v1/inventory/trace-handovers/exceptions/{id}/review` | 交接异常复核(缺口/重复/差异) | `inventory.handover.review` | `inventory.handover_exception_resolved` / `inventory.handover_exception_review_failed` | web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `scanTraceCodeHandover` | `POST` | `/api/v1/inventory/trace-codes/scan-handover` | 物品追踪码扫码交接 | `inventory.handover.scan` | `operation_handover.created` / `operation_handover.failed` | staff_work_phone, tablet | operation 级补充 |
| INV-库存存酒 | `scanTraceSegment` | `POST` | `/api/v1/inventory/trace-handovers/{id}/scan-segment` | 员工扫码交接分段(主动留痕) | `inventory.handover.scan` | `inventory.handover_segment_scanned` / `inventory.handover_segment_scan_failed` | staff_work_phone, tablet, web, mobile_admin | operation 级补充 |
| INV-库存存酒 | `transferStockMovement` | `POST` | `/api/v1/inventory/stock-movements/transfer` | 调拨库存流水 | `inventory.stock.move` | `inventory.stock_transfer.created` / `inventory.stock_transfer.created_failed` | web | operation 级补充 |
| OPEN-开放平台 | `getOpenEventResult` | `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | 回调处理结果查询 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `openAuthToken` | `POST` | `/api/open/auth/token` | 开放平台获取令牌 | `not_applicable` | `open_auth.succeeded` / `open_auth.failed` | <br>channel: server-to-server | operation 级补充 |
| OPEN-开放平台 | `receiveOpenWebhook` | `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | 第三方或供应商回调 | `not_applicable` | `api_call.logged` / `api_call.rejected` | <br>channel: server-to-server | operation 级补充 |
| OPS-生产前置 | `createProductionReadinessCheck` | `POST` | `/api/v1/ops/production-readiness/checks` | 执行生产前置准入检查 | `ops.production_readiness.check` | `production_readiness.checked` / `production_readiness.check_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `listProductionReadinessItems` | `GET` | `/api/v1/ops/production-readiness/items` | 查询生产前置责任状态 | `ops.production_readiness.view` | `production_readiness.viewed` / `production_readiness.view_failed` | web, mobile_admin | 已在任务映射正文出现 |
| OPS-生产前置 | `updateProductionReadinessItem` | `PUT` | `/api/v1/ops/production-readiness/items/{id}` | 更新生产前置责任状态 | `ops.production_readiness.manage` | `production_readiness.updated` / `production_readiness.update_failed` | web, mobile_admin | 已在任务映射正文出现 |
| P0-待归类 | `addMerchantServiceWorkOrderAction` | `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | Append merchant service work order action | `merchant.service_work_order.process` | `merchant_service.addMerchantServiceWorkOrderAction.succeeded` / `merchant_service.addMerchantServiceWorkOrderAction.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `assignMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | Assign merchant service work order | `merchant.service_work_order.assign` | `merchant_service.assignMerchantServiceWorkOrder.succeeded` / `merchant_service.assignMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `closeMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | Close merchant service work order | `merchant.service_work_order.close` | `merchant_service.closeMerchantServiceWorkOrder.succeeded` / `merchant_service.closeMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `createFloorPlan` | `POST` | `/api/v1/floor-plans` | 创建 P1/P2 商场平面图 | `floor_plan.manage` | `floor_plan.created` / `floor_plan.create_failed` | web | operation 级补充 |
| P0-待归类 | `createFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports` | 创建 SVG 或标准 JSON 导入任务 | `floor_plan.import.manage` | `floor_plan.import_created` / `floor_plan.import_create_failed` | web | operation 级补充 |
| P0-待归类 | `createMerchantServiceWorkOrder` | `POST` | `/api/v1/merchant-service/work-orders` | Create merchant service work order | `merchant.service_work_order.create` | `merchant_service.createMerchantServiceWorkOrder.succeeded` / `merchant_service.createMerchantServiceWorkOrder.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `getFloorPlanImportDetails` | `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | 查询导入候选和错误 | `floor_plan.import.view` | `floor_plan.import_candidate_viewed` / `floor_plan.import_candidate_view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanImports` | `GET` | `/api/v1/floor-plans/{id}/imports` | 查询平面图导入任务 | `floor_plan.import.view` | `floor_plan.import_viewed` / `floor_plan.import_view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlans` | `GET` | `/api/v1/floor-plans` | 查询 P1/P2 商场平面图 | `floor_plan.view` | `floor_plan.viewed` / `floor_plan.view_failed` | web | operation 级补充 |
| P0-待归类 | `listFloorPlanShapes` | `GET` | `/api/v1/floor-plans/{id}/shapes` | 查询平面图正式区域 | `floor_plan.view` | `floor_plan.shape_viewed` / `floor_plan.shape_view_failed` | web | operation 级补充 |
| P0-待归类 | `listMerchantBills` | `GET` | `/api/v1/merchant-billing/bills` | List merchant bills | `merchant.billing.view` | `merchant_billing.bill.viewed` / `merchant_billing.bill.view_failed` | web, merchant_portal | operation 级补充 |
| P0-待归类 | `listMerchantServiceWorkOrders` | `GET` | `/api/v1/merchant-service/work-orders` | List merchant service work orders | `merchant.service_work_order.view` | `merchant_service.listMerchantServiceWorkOrders.succeeded` / `merchant_service.listMerchantServiceWorkOrders.failed` | web, mobile_admin, merchant_portal | operation 级补充 |
| P0-待归类 | `listWecomRetentionPolicies` | `GET` | `/api/v1/compliance/wecom/retention-policies` | 企微留存策略列表 | `wecom.retention.manage` | `wecom.retention_policy_viewed` / `wecom.retention_policy_view_failed` | web | operation 级补充 |
| P0-待归类 | `publishFloorPlanImport` | `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | 发布平面图导入候选 | `floor_plan.import.manage` | `floor_plan.import_published` / `floor_plan.import_publish_failed` | web | operation 级补充 |
| P0-待归类 | `syncWecomConversation` | `POST` | `/api/v1/compliance/wecom/conversations/sync` | 企微会话同步入站（PIPL 合规，开关门控） | `wecom.sync` | `wecom.conversation_synced` / `wecom.conversation_sync_failed` | <br>channel: provider-webhook | operation 级补充 |
| P0-待归类 | `upsertWecomRetentionPolicy` | `POST` | `/api/v1/compliance/wecom/retention-policies` | 配置企微留存策略（默认180天） | `wecom.retention.manage` | `wecom.retention_policy_set` / `wecom.retention_policy_failed` | web | operation 级补充 |
| P0-待归类 | `viewWecomConversationMessages` | `GET` | `/api/v1/compliance/wecom/conversations/{id}/messages` | 分级查看企微会话消息（脱敏/原文，原文需审批+审计） | `wecom.view_raw` | `wecom.raw_viewed` / `wecom.raw_view_failed` | web, mobile_admin | operation 级补充 |
| SYS-系统健康 | `getSystemHealth` | `GET` | `/api/v1/system/health` | 健康检查 | `public` | `system.health_checked` / `system.health_check_failed` | web, mobile_admin<br>channel: monitoring | operation 级补充 |
| SYS-系统健康 | `getSystemVersion` | `GET` | `/api/v1/system/version` | 版本和构建信息 | `system.version.view` | `system.version_viewed` / `system.version_view_failed` | web, mobile_admin | operation 级补充 |
| TERM-终端设备 | `bindDevice` | `POST` | `/api/v1/terminals/devices/bind` | 设备绑定 | `terminal.device.bind` | `terminal.device.bound` / `terminal.device.bind_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `enrollMdmDevice` | `POST` | `/api/v1/terminals/mdm/devices/enroll` | MDM 设备纳管/策略同步 | `mdm.device.manage` | `mdm.policy_checked` / `mdm.policy_check_failed` | web | operation 级补充 |
| TERM-终端设备 | `getKioskConfig` | `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | 一体机配置，含售票型与非售票型模式 | `not_applicable` | `terminal.kiosk_config_viewed` / `terminal.access_rejected` | kiosk | operation 级补充 |
| TERM-终端设备 | `getTerminalOfflineEntitlements` | `GET` | `/api/v1/terminals/offline-entitlements` | 获取终端离线权益 | `terminal.device.bind` | `terminal.offline_entitlements.viewed` / `terminal.offline_entitlements.viewed_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `kioskHeartbeat` | `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | 一体机心跳 | `not_applicable` | `terminal.kiosk_heartbeat` / `terminal.kiosk_heartbeat_failed` | kiosk | operation 级补充 |
| TERM-终端设备 | `listCapabilitySwitches` | `GET` | `/api/v1/terminals/capability-switches` | 查询端能力开关 | `terminal.switch.view` | `terminal_switch.viewed` / `terminal_switch.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmAppWhitelist` | `GET` | `/api/v1/terminals/mdm/app-whitelist` | 查询 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.viewed` / `mdm.app_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmPolicyViolations` | `GET` | `/api/v1/terminals/mdm/violations` | 查询 MDM 设备违规留痕 | `mdm.violation.view` | `mdm.violation.viewed` / `mdm.violation.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `listMdmSiteWhitelist` | `GET` | `/api/v1/terminals/mdm/site-whitelist` | 查询 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.viewed` / `mdm.site_whitelist.view_failed` | web | operation 级补充 |
| TERM-终端设备 | `reportMdmPolicyViolation` | `POST` | `/api/v1/terminals/mdm/violations` | 上报 MDM 设备违规 | `mdm.device.manage` | `device.policy_violation` / `device.policy_violation_failed` | web | operation 级补充 |
| TERM-终端设备 | `returnTerminalDevice` | `POST` | `/api/v1/terminals/devices/return` | 归还终端设备 | `terminal.device.bind` | `terminal.device.returned` / `terminal.device.returned_failed` | staff_work_phone, tablet, kiosk, web | operation 级补充 |
| TERM-终端设备 | `syncTerminalTasks` | `POST` | `/api/v1/terminals/sync-tasks` | 同步终端任务 | `terminal.device.bind` | `terminal.sync_task.accepted` / `terminal.sync_task.accepted_failed` | staff_work_phone, tablet, kiosk | operation 级补充 |
| TERM-终端设备 | `unbindDevice` | `POST` | `/api/v1/terminals/devices/{id}/unbind` | 设备解绑 | `terminal.device.unbind` | `terminal.device.unbound` / `terminal.device.unbind_failed` | web | operation 级补充 |
| TERM-终端设备 | `updateCapabilitySwitch` | `PUT` | `/api/v1/terminals/capability-switches/{id}` | 修改端能力开关 | `terminal.switch.manage` | `terminal_switch.changed` / `terminal_switch.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertMdmAppWhitelist` | `PUT` | `/api/v1/terminals/mdm/app-whitelist/{id}` | 新增或更新 MDM 应用白名单 | `mdm.policy.manage` | `mdm.app_whitelist.changed` / `mdm.app_whitelist.change_failed` | web | operation 级补充 |
| TERM-终端设备 | `upsertMdmSiteWhitelist` | `PUT` | `/api/v1/terminals/mdm/site-whitelist/{id}` | 新增或更新 MDM 网站白名单 | `mdm.policy.manage` | `mdm.site_whitelist.changed` / `mdm.site_whitelist.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `approveTicketRefund` | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | 退款审批 | `ticket.refund.approve` | `ticket_refund.approved` / `ticket_refund.approve_failed` | web | operation 级补充 |
| TIC-票务权益 | `bindTicketTypeEntitlement` | `POST` | `/api/v1/ticketing/ticket-types/{id}/entitlements` | 绑定票类型权益 | `ticket.product.manage` | `ticket_product_entitlement.bound` / `ticket_product_entitlement.bound_failed` | web | operation 级补充 |
| TIC-票务权益 | `createEntitlementTemplate` | `POST` | `/api/v1/ticketing/entitlement-templates` | 创建权益模板 | `ticket.entitlement.manage` | `ticket_entitlement_template.changed` / `ticket_entitlement_template.changed_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules` | Create P2 ticket dynamic pricing rule draft | `ticket.dynamic_pricing.manage` | `ticket_dynamic_pricing.created.success` / `ticket_dynamic_pricing.created.failure` | web | operation 级补充 |
| TIC-票务权益 | `createTicketOrder` | `POST` | `/api/v1/ticketing/orders` | 创建票务订单 | `ticket.order.create` | `ticket_order.created` / `ticket_order.create_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketOrderPayIntent` | `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | 创建票务支付意图 | `finance.payment_intent.create` | `payment.intent_created` / `payment.intent_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `createTicketPriceRule` | `POST` | `/api/v1/ticketing/price-rules` | 创建平日、周末、节假日及派生价规则 | `ticket.price_variant.manage` | `ticket_price_variant.changed` / `ticket_price_variant.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketType` | `POST` | `/api/v1/ticketing/ticket-types` | 创建票种 | `ticket.product.manage` | `ticket_product.changed` / `ticket_product.change_failed` | web | operation 级补充 |
| TIC-票务权益 | `createTicketVerification` | `POST` | `/api/v1/ticketing/verifications` | 正式核销 | `verification.execute` | `verification.succeeded` / `verification.failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `getTicketCredential` | `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | 获取动态二维码凭证 | `ticket.credential.view` | `ticket_credential.refreshed` / `ticket_credential.refresh_failed` | web, miniapp, app | operation 级补充 |
| TIC-票务权益 | `issueTicket` | `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | 出票 | `ticket.ticket.issue` | `ticket.issued` / `ticket.issue_failed` | web, kiosk, miniapp, app | operation 级补充 |
| TIC-票务权益 | `issueTicketTypeDirectly` | `POST` | `/api/v1/ticketing/ticket-types/{id}/issue` | 发放票类型directly | `ticket.order.create` | `ticket_order.issued` / `ticket_order.issued_failed` | web | operation 级补充 |
| TIC-票务权益 | `listEntitlementTemplates` | `GET` | `/api/v1/ticketing/entitlement-templates` | 查询权益模板 | `ticket.entitlement.view` | `ticket_entitlement_template.viewed` / `ticket_entitlement_template.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listIssuedEntitlements` | `GET` | `/api/v1/ticketing/issued-entitlements` | 查询已发放权益 | `ticket.verify.view` | `ticket_issued_entitlement.viewed` / `ticket_issued_entitlement.viewed_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketDynamicPricingRules` | `GET` | `/api/v1/ticketing/dynamic-pricing-rules` | List P2 ticket dynamic pricing rules | `ticket.dynamic_pricing.view` | `ticket_dynamic_pricing.viewed.success` / `ticket_dynamic_pricing.viewed.failure` | web | operation 级补充 |
| TIC-票务权益 | `listTicketPriceRules` | `GET` | `/api/v1/ticketing/price-rules` | 查询平日、周末、节假日及派生价规则 | `ticket.price_variant.view` | `ticket_price_variant.viewed` / `ticket_price_variant.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketTypes` | `GET` | `/api/v1/ticketing/ticket-types` | 票种查询 | `ticket.product.view` | `ticket_product.viewed` / `ticket_product.view_failed` | web | operation 级补充 |
| TIC-票务权益 | `listTicketVerifications` | `GET` | `/api/v1/ticketing/verifications` | 查询票核销记录 | `verification.view` | `verification.viewed` / `verification.view_failed` | web, staff_work_phone, tablet, kiosk | operation 级补充 |
| TIC-票务权益 | `previewTicketVerification` | `POST` | `/api/v1/ticketing/verifications/preview` | 票务核销预览 | `verification.execute` | `verification.previewed` / `verification.failed` | staff_work_phone<br>device: verification-gate | operation 级补充 |
| TIC-票务权益 | `publishTicketDynamicPricingRule` | `POST` | `/api/v1/ticketing/dynamic-pricing-rules/{id}/publish` | Publish P2 ticket dynamic pricing rule | `ticket.dynamic_pricing.publish` | `ticket_dynamic_pricing.published.success` / `ticket_dynamic_pricing.published.failure` | web | operation 级补充 |
| TIC-票务权益 | `publishTicketType` | `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | 发布票种 | `ticket.product.manage` | `ticket_price.published` / `ticket_price.publish_failed` | web | operation 级补充 |
| TIC-票务权益 | `requestTicketRefund` | `POST` | `/api/v1/ticketing/refunds` | 退款或撤销申请 | `ticket.refund.request` | `ticket_refund.requested` / `ticket_refund.request_failed` | web, merchant_portal | operation 级补充 |
| TIC-票务权益 | `reverseTicketVerification` | `POST` | `/api/v1/ticketing/verifications/{id}/reverse` | 核销冲正 | `ticket.verify.reverse` | `ticket.verification_reversed` / `ticket.verification_reverse_failed` | web, staff_work_phone, mobile_admin | operation 级补充 |
| TIC-票务权益 | `simulateTicketDynamicPricing` | `POST` | `/api/v1/ticketing/dynamic-pricing/simulations` | Simulate P2 ticket dynamic pricing without creating an order | `ticket.dynamic_pricing.simulate` | `ticket_dynamic_pricing.simulated.success` / `ticket_dynamic_pricing.simulated.failure` | web | operation 级补充 |

## 6. OpenAPI 示例覆盖执行口径

当前 OpenAPI 已有 P0 首批高风险样例覆盖：

| 类型 | 已覆盖 |
|---|---|
| request body | OpenAuthTokenRequest, Webhook, CustomerTicketOrderRequest, Verification, CustomerBarReservationRequest, BarOrderRequest, PaymentIntent, AmusementSafetyActionRequest, ProviderAdapterTestRequest |
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
| 售票型与非售票型一体机能力混用 | 按 `kiosk_mode`、设备绑定、能力开关和 `x-terminal-allowed` 拒绝 |
| 旧路径表格与 OpenAPI 冲突 | 旧路径表格只作为历史能力名，正式实现以 OpenAPI 和本文为准 |

## 9. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-13 | V0.1 | 新增由 OpenAPI 反向生成的权限、事件、任务追溯收口附录，补齐本轮对抗式文档审计发现的执行缺口 |
