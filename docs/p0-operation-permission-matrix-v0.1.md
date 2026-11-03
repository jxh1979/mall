# P0 操作权限矩阵 V0.11

> **口径修订（2026-08-06 R10）：「场次容量」已废止**——BSM「容量控制」条 明确「**只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量**」（DEC-R5-2026-08-04-04；R8 用户裁决 D-02）。本文原「场次容量」一律改称「**项目容量（实时在玩人数）**」；双阈值不变（运营容量软上限告警放行、安全容量硬上限阻断并自动停运）。仍按场次/时段或点位/设备维度罗列的页面、接口、组件与字段只作历史结构参考，容量口径一律以本条为准，相关字段待代码轮下线，期间不得据以新增受控维度。

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

> 本文档用于把“谁能做什么”落成可开发、可测试、可审计的权限矩阵。它不是角色名称清单，而是权限点、模板、审批、留痕和端侧入口的执行标准。

## 1. 权威口径

- P0 权限采用 `domain.resource.action` 点式编码，凡与本文冲突，以 `unified-standards-baseline-v0.1.md` 和本文为准。
- 本文负责 P0 权限治理规则、模板规则、临时授权、单次有效、显式拒绝和高风险审批口径。接口级完整 `x-permission` 清单以 OpenAPI 和 `p0-openapi-contract-closure-appendix-v0.1.md` 反向生成结果为准；本文不得与其使用同义权限码。
- 客人 Token、设备凭证、开放平台应用凭证和 Webhook 签名不是员工权限码。OpenAPI 中必须写为 `x-auth-context=customer-token/device-credential/open-app-credential/webhook-signature`，`x-permission` 使用 `not_applicable`；测试时按认证上下文、签名、设备绑定和数据范围校验，不进入员工权限模板配置。
- 权限必须支持“角色 + 权限模板 + 员工单独授权/限制 + 临时授权”的组合，不允许只靠固定角色写死。
- 运营可通过模板批量配置员工权限，避免逐个员工手工勾选。
- 高风险权限必须具备审批、留痕、复核和撤销能力。

## 2. 权限模板机制

### 2.1 模板类型

| 模板类型 | 用途 | 生命周期 | 是否可复制 | 典型例子 |
|---|---|---:|---:|---|
| 基础模板 | 长期岗位/角色权限预设 | 长期有效，可停用 | 是 | 普通员工、销售人员、票务主管、酒吧吧台、财务出纳 |
| 临时模板 | 临时支援、活动、夜场、突发任务 | 支持起止时间或单次有效 | 是 | 节假日临时核销、活动临时主管、盘点支援 |
| 复制模板 | 从已有模板复制后局部调整 | 继承来源模板快照后独立维护 | 是 | 普通员工01、酒吧支援01 |

### 2.2 模板规则

1. 模板必须有唯一编号、名称、类型、适用范围、创建人、审核人、启用时间、停用时间和状态。
2. 模板复制时复制的是当时权限快照，不随源模板后续变化自动变化。
3. 临时模板可设置 `valid_from`、`valid_to`、`single_use`，到期或单次使用后自动失效。
4. 临时模板必须填写用途说明；涉及退款、库存调整、财务调整、敏感数据导出时必须审批后生效（原含「人工放行」，已废止·见文首口径）。
5. 员工最终权限 = 角色权限 + 基础模板授权 + 有效临时模板授权 + 员工单独授予 - 员工/模板显式拒绝。
6. 显式拒绝优先级最高；同一权限存在授予和拒绝时，按拒绝处理，并在前端显示冲突原因。
7. 每次模板创建、复制、修改、应用、撤销、到期、单次消耗都必须写入审计事件。

## 3. P0 权限点矩阵

| 业务动作 | 权限编码 | 默认可授予对象 | 风险 | 是否需审批 | 必须留痕事件 | 说明 |
|---|---|---|---|---|---|---|
| 查看综合体/项目基础资料 | `governance.project.view` | 平台管理员、运营主管、财务 | 低 | 否 | `project.viewed` | 只读 |
| 维护综合体/项目基础资料 | `governance.project.manage` | 平台管理员 | 高 | 是 | `project.updated` | 影响全局数据口径 |
| 场馆/区域/点位查看 | `governance.space.view` | 平台管理员、运营主管、招商、商户管理员 | 低 | 否 | `space.viewed` | 按综合体、商户和门店数据范围过滤 |
| 场馆/区域/点位维护 | `governance.space.manage` | 平台管理员、运营主管 | 中 | 可配置 | `space.updated` | 影响核销点位、设备绑定和一体机能力范围 |
| 商户档案查看 | `merchant.profile.view` | 招商、运营、财务、商户管理员 | 低 | 否 | `merchant.viewed` | 按数据范围过滤 |
| 商户档案维护 | `merchant.profile.manage` | 招商主管、平台管理员 | 中 | 可配置 | `merchant.updated` | 影响合同和结算 |
| 四类主体绑定查看 | `entity.binding.view` | 平台管理员、招商、财务 | 中 | 否 | `entity_binding.viewed` | 经营主体、运营主体、合同主体、结算主体 |
| 四类主体绑定变更 | `entity.binding.manage` | 平台管理员、财务主管 | 高 | 是 | `entity_binding.changed` | 必须保留变更前后快照 |
| 合同创建/编辑草稿 | `contract.draft.manage` | 招商、法务 | 中 | 否 | `contract.draft_saved` | 不代表生效 |
| 合同生效/终止/放行 | `contract.lifecycle.approve` | 法务、财务、总控授权人 | 高 | 是 | `contract.lifecycle_changed` | 与主体绑定联动 |
| 权限模板查看 | `access.permission_template.view` | 平台管理员、HR、运营主管 | 中 | 否 | `permission_template.viewed` | 不展示敏感字段时可放宽 |
| 权限模板创建/复制/修改 | `access.permission_template.manage` | 平台管理员、HR 权限管理员 | 高 | 是 | `permission_template.changed` | 模板变更必须审批或复核 |
| 权限模板应用到员工 | `access.permission_template.apply` | 平台管理员、HR 权限管理员 | 高 | 是 | `permission_template.applied` | 临时模板必须有有效期 |
| 临时权限撤销 | `access.permission_template.revoke` | 平台管理员、HR 权限管理员、上级主管 | 高 | 是 | `permission_template.revoked` | 支持批量撤销 |
| 票种/票务价格配置 | `ticket.product.manage` | 票务主管、平台管理员 | 高 | 是 | `ticket_product.changed` | 平日/周末/节假日价为基础价 |
| 渠道价/新人/学生/多人票配置 | `ticket.price_variant.manage` | 票务主管、平台管理员 | 高 | 是 | `ticket_price_variant.changed` | 可按基础价生成或固定价 |
| 票务订单查看 | `ticket.order.view` | 票务、财务、客服 | 低 | 否 | `ticket_order.viewed` | 隐私字段按权限脱敏 |
| 退款发起 | `ticket.refund.request` | 客服、票务主管 | 高 | 是 | `ticket_refund.requested` | 不直接改金额 |
| 退款审批 | `ticket.refund.approve` | 财务主管、运营主管 | 高 | 是 | `ticket_refund.approved` | 与支付通道/财务台账联动 |
| 票/券/权益核销 | `verification.execute` | 工作手机、授权闸机/核销点设备、票务/酒吧/游乐员工 | 中 | 否 | `verification.succeeded` / `verification.failed` | 必须在线核验动态码；平板不作为门票核销设备；售票型一体机只允许授权服务人员辅助售票/取票/查单，默认不做核销 |
| 核销撤销 | `verification.reverse` | 现场主管 | 高 | 是 | `verification.reversed` | 必须填写原因 |
| 人工放行（**已废止·见文首口径**） | `verification.manual_override`（停用，写入口已下线） | — | — | — | `verification.manual_overridden`（停用） | 该动作不再提供，任何岗位、任何权限都不得放行不满足规则的核销 |
| 酒吧入场年龄核验 | `bar.entry.age_verify` | 酒吧门口员工、现场主管 | 高 | 否 | `bar.age_verification.requested` / `bar.age_verification.passed` / `bar.age_verification.rejected` / `bar.age_verification.blocked` | 酒吧售酒/入场合规前置 |
| 酒吧人脸核验结果写入 | `bar.entry.face_result.write` | 第三方人脸服务适配器、授权设备 | 高 | 系统准入 | `bar.age_verification.passed` / `bar.age_verification.rejected` / `bar.age_verification.blocked` | 供应商确定前仅保留接口和模拟数据；生产未配置不得标记通过 |
| 卡座预订入场人数核销 | `bar.table.guest_checkin` | 酒吧门口员工、现场主管 | 中 | 否 | `bar.table_guest.checked_in` / `bar.table_guest.rejected` | 只允许进入预定卡座所在酒吧；需幂等和并发额度锁 |
| 卡座销售归属登记 | `bar.table_sales.attribute` | 销售人员、授权服务员、运营主管 | 中 | 变更需复核 | `order.sales_attributed` | 销售人员岗位提成率 15%，非销售人员 10%；仅针对卡座消费额，不含通票 |
| 卡座提成查看/复核 | `bar.table_commission.view` / `bar.table_commission.review` | 财务、运营主管、HR/薪酬授权人 | 高 | 是 | `bar.table_commission.calculated` / `bar.table_commission.adjusted` | 普通销售人员只能看本人提成摘要，不得改提成基数 |
| 开台/关台 | `bar.table.open_close` | 酒吧服务员、吧台、主管 | 中 | 否 | `bar.table.opened` / `bar.table.cleared` | P0 基础酒吧闭环 |
| 点酒/出酒 | `bar.order.create` | 服务员、吧台 | 中 | 否 | `bar.order_created` / `bar.order_delivered` | 高价值酒水保持在线 |
| 酒水赠饮核销 | `bar.drink_benefit.redeem` | 吧台、主管 | 中 | 否 | `bar.gift_redeemed` | 通票赠送权益成本由**娱乐平台公司**承担（2026-07-28）；A 吧赠酒只能在 A 吧核销 |
| 存酒登记/领取/作废 | `bar.saved_bottle.manage` | 吧台、主管 | 高 | 领取/作废可配置 | `bar.saved_bottle.created` / `bar.saved_bottle_picked_up` / `bar.saved_bottle.voided` | 必须照片/签收/手机号 |
| 库存入库/出库 | `inventory.stock.move` | 仓库、吧台、主管 | 中 | 可配置 | `inventory.inbound_confirmed` / `inventory.outbound_verified` / `inventory.transferred` | P0 必须支持库存扣减闭环 |
| 库存盘点差异确认 | `inventory.stock.adjust` | 仓库主管、财务复核 | 高 | 是 | `inventory.counted` / `inventory.damaged` / `inventory.surplus_recorded` / `inventory.reversed` | 高价值酒水需附件 |
| 游乐项目/场次查看 | `amusement.session.view` | 游乐员工、游乐主管、平台管理员 | 低 | 否 | `amusement.session.viewed` | P0 基础项目、场次和容量查询 |
| 游乐项目/场次维护 | `amusement.session.manage` | 游乐主管、平台管理员 | 中 | 可配置 | `amusement.session.changed` | P0 基础项目容量（实时在玩人数）（原称「场次容量」，已废止·见文首口径） |
| 游乐核销记录查看 | `amusement.verification.view` | 游乐员工、游乐主管、平台管理员 | 低 | 否 | `amusement.verification.viewed` | 查询核销成功、失败和安全拒绝原因（原含「人工放行」，已废止·见文首口径） |
| 游乐核销（原含「/人工放行」，已废止·见文首口径） | `amusement.verification.execute`（`amusement.verification.override` 停用，游乐改判接口已下线） | 游乐员工、主管 | 中 | 否 | `amusement.verification_succeeded` / `amusement.verification_failed` | 与票务权益联动 |
| 游乐安全巡检模板查看/维护 | `amusement.safety_template.view` / `amusement.safety_template.manage` | 巡检员、游乐主管、平台管理员 | 高 | 模板维护可配置审批 | `amusement.safety_template.viewed` / `amusement.safety_template.changed` | P0 开放前、营业中抽检和阻断项模板 |
| 游乐安全巡检任务查看/创建 | `amusement.safety_inspection.view` / `amusement.safety_inspection.create` | 巡检员、游乐主管、平台管理员 | 高 | 创建可配置审批 | `amusement.safety_inspection.viewed` / `amusement.safety_inspection.created` | P0 巡检任务、记录和整改复检查询 |
| 游乐安全巡检提交 | `amusement.safety_inspection.submit` | 巡检员、游乐主管 | 高 | 否 | `amusement.safety_inspection.submitted` / `amusement.safety_inspection.blocked` | 开放前必检，阻断项自动暂停 |
| 游乐安全整改/复检 | `amusement.safety_inspection.rectify` / `amusement.safety_inspection.recheck` | 游乐运营、巡检员、游乐主管 | 高 | 可配置 | `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rechecked` | 严重项未复检通过不得恢复 |
| 游乐严重项恢复开放 | `amusement.safety_inspection.approve_reopen` | 游乐主管、安全负责人、总控授权人 | 高 | 是 | `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected` | 复检通过后仍需主管审批 |
| 游乐设备查看 | `amusement.device.view` | 游乐员工、游乐主管、平台管理员、授权运维 | 低（normal） | 否 | `amusement.device.viewed` / `amusement.device.view_failed` | 只读；按 `project-amusement` 数据范围返回项目下游乐设备 |
| 游乐设备建档/维护 | `amusement.device.manage` | 游乐主管、平台管理员 | 高 | 是 | `amusement.device.changed` / `amusement.device.change_failed` | `device_code` 项目内唯一，作为容量配置 device_id 与巡检对象来源；高风险写需审批与幂等 |
| 应收应付台账维护 | `finance.ar_ap.manage` | 财务 | 高 | 是 | `finance_ar_ap.changed` | P0 必须完整支持 |
| 收付款登记 | `finance.payment.register` | 财务、出纳 | 高 | 是 | `finance_payment.registered` | 与聚合支付/线下凭证联动 |
| 日结锁定/解锁申请 | `finance.daily_close.lock` / `finance.daily_close.unlock_request` | 财务主管、总控授权人 | 高 | 是 | `finance_daily_close.changed` | 锁定后不得直接改金额 |
| 第三方应用配置 | `openplatform.app.manage` | 平台管理员、IT | 高 | 是 | `open_app.changed` | 聚合支付、企业微信、机器人、视频、设备等 |
| 第三方密钥轮换 | `openplatform.credential.rotate` | IT、安全负责人 | 高 | 是 | `open_credential.rotated` | 人工台账 + 到期提醒 + Runbook |
| 第三方字段映射配置 | `openplatform.mapping.manage` | IT、平台管理员 | 高 | 是 | `open_mapping.changed` | 供应商 API 到位后补齐 |
| 第三方对账/补偿执行 | `openplatform.reconcile.execute` | 财务、IT | 高 | 是 | `open_reconcile.executed` | Webhook + 定时轮询校验 |
| 敏感证据查看 | `evidence.sensitive.view` | 法务、风控、授权主管 | 高 | 是 | `sensitive.viewed` | 按最小授权 |
| 证据导出 | `evidence.export` | 法务、风控负责人 | 高 | 是 | `evidence.exported` | 必须导出水印和用途 |
| 终端开关配置 | `terminal.switch.manage` | 平台管理员、IT | 高 | 是 | `terminal_switch.changed` | 按项目/商户/模块/设备/版本 |
| MDM 策略查看 | `device.mdm_policy.view` | IT、平台管理员 | 中 | 否 | `mdm_policy.viewed` | Android Enterprise / 商业 MDM |
| MDM 策略下发 | `device.mdm_policy.apply` | IT | 高 | 是 | `mdm_policy.applied` | 上线前置项 |

### 3.1 OpenAPI 精确权限码补充

以下权限码用于把已确认 P0 动作拆成 OpenAPI `x-permission` 可执行粒度，不新增业务范围。后续接口、前端按钮、测试用例、操作留痕不得再使用 `*`、`view/manage` 合并值或同义旧码。

| 业务动作 | 权限编码 | 默认可授予对象 | 风险 | 是否需审批 | 必须留痕事件 | 说明 |
|---|---|---|---|---|---|---|
| 查看系统版本和构建信息 | `system.version.view` | IT、运维、平台管理员 | 低 | 否 | `system.version_viewed` | 不含密钥、数据库连接、服务器敏感信息 |
| 查看生产前置责任状态 | `ops.production_readiness.view` | 总控、IT、运维、项目负责人 | 中 | 否 | `production_readiness.viewed` | 只读查询生产阻断项、责任部门、计划日期和降级方式 |
| 更新生产前置责任状态 | `ops.production_readiness.manage` | IT、运维、项目负责人 | 高 | 是 | `production_readiness.updated` | 更新责任人、状态、证据和降级方式，必须保留操作人和附件 |
| 执行生产准入检查 | `ops.production_readiness.check` | 总控、IT、运维、项目负责人 | 高 | 是 | `production_readiness.checked` | 任一阻断项未通过时不得标记真实生产就绪 |
| 合同摘要查看 | `contract.view` | 招商、法务、财务、运营主管 | 中 | 否 | `contract.viewed` | 附件和敏感字段按数据范围脱敏 |
| 权限点查看 | `access.permission.view` | 平台管理员、HR 权限管理员 | 中 | 否 | `permission.viewed` | 只读 |
| 角色查看 | `access.role.view` | 平台管理员、HR 权限管理员、运营主管 | 中 | 否 | `role.viewed` | 不展示无权范围 |
| 角色创建/修改/停用 | `access.role.manage` | 平台管理员、HR 权限管理员 | 高 | 是 | `role.changed` | 不得绕过权限模板审计 |
| 临时授权创建 | `access.temporary_grant.create` | 平台管理员、HR 权限管理员、上级主管 | 高 | 是 | `permission_template.applied` | 必须有用途、有效期或单次有效配置 |
| 临时授权撤销 | `access.temporary_grant.revoke` | 平台管理员、HR 权限管理员、上级主管 | 高 | 是 | `permission_template.revoked` | 支持批量撤销 |
| 设备绑定 | `terminal.device.bind` | IT、平台管理员、授权现场主管 | 高 | 是 | `terminal.device.bound` | 必须校验 MDM/设备指纹 |
| 设备解绑 | `terminal.device.unbind` | IT、平台管理员 | 高 | 是 | `terminal.device.unbound` | 解绑后设备不得继续调用业务接口 |
| 端能力开关查看 | `terminal.switch.view` | IT、平台管理员、运营主管 | 中 | 否 | `terminal_switch.viewed` | 只读 |
| 一体机心跳 | `terminal.kiosk.heartbeat` | 已绑定一体机 | 中 | 系统准入 | `terminal.kiosk_heartbeat` | 仅设备凭证可调用 |
| 票务产品查看 | `ticket.product.view` | 票务、运营、财务、客服 | 低 | 否 | `ticket_product.viewed` | 只读 |
| 票务价格派生规则查看 | `ticket.price_variant.view` | 票务、运营、财务 | 低 | 否 | `ticket_price_variant.viewed` | 只读 |
| 票务订单创建 | `ticket.order.create` | 客人端、售票型一体机授权服务人员、授权员工 | 中 | 否 | `ticket_order.created` | 必须走价格、库存和支付校验；售票一体机必须绑定设备点位并过滤可售票种，酒吧门口/前台只能售卖本酒吧单店门票 |
| 出票 | `ticket.ticket.issue` | 系统、票务主管、售票型一体机 | 高 | 系统准入 | `ticket.issued` | 必须绑定订单和支付状态 |
| 票码凭证查看 | `ticket.credential.view` | 客人本人、票务/客服授权人 | 中 | 否 | `ticket_credential.refreshed` | 动态码不得长期缓存 |
| 酒吧桌台/卡座查看 | `bar.table.view` | 酒吧员工、运营主管、平台管理员 | 低 | 否 | `bar.table.viewed` | 按酒吧数据范围过滤 |
| 酒吧桌台/卡座维护 | `bar.table.manage` | 酒吧主管、平台管理员 | 中 | 可配置 | `bar.table.changed` | 影响卡座人数、低消和终端绑定 |
| 卡座预订创建 | `bar.reservation.create` | 客人端、酒吧员工、运营主管 | 中 | 否 | `bar.reservation.created` | 卡座免通票人数按卡座配置 |
| 卡座提成计算 | `bar.table_commission.calculate` | 财务、运营主管、系统任务 | 高 | 是 | `bar.table_commission.calculated` | 销售人员 15%，非销售人员 10%；不含通票 |
| 供应商档案查看 | `inventory.supplier.view` | 采购、仓库、财务 | 低 | 否 | `inventory.supplier_viewed` | 只读 |
| 供应商档案维护 | `inventory.supplier.manage` | 采购主管、平台管理员 | 中 | 可配置 | `inventory.supplier_changed` | 拉黑/恢复需留痕 |
| 采购申请查看 | `inventory.purchase_request.view` | 采购、仓库、财务 | 低 | 否 | `inventory.purchase_request_viewed` | 只读 |
| 采购申请维护 | `inventory.purchase_request.manage` | 采购、采购主管 | 中 | 可配置 | `inventory.purchase_request_changed` | 审批链按金额和品类配置 |
| 创建支付意图 | `finance.payment_intent.create` | 系统、收银、授权终端 | 高 | 系统准入 | `payment.intent_created` | 聚合支付为主，保留直连抽象 |
| 创建财务退款记录 | `finance.refund.create` | 财务、客服主管 | 高 | 是 | `finance_refund.created` | 不删除原单，只做冲正 |
| 应收应付台账查看 | `finance.ar_ap.view` | 财务、运营主管、授权商户 | 中 | 否 | `finance_ar_ap.viewed` | 按主体绑定和数据范围过滤 |
| 事件查询 | `evidence.event.view` | 运营主管、风控、法务、IT | 中 | 否 | `evidence.event_viewed` | 敏感字段按 `evidence.sensitive.view` 控制 |
| 业务时间线查看 | `evidence.timeline.view` | 运营主管、风控、法务、客服主管 | 中 | 否 | `evidence.timeline_viewed` | 按业务对象授权 |
| 证据附件创建/绑定 | `evidence.attachment.create` | 授权员工、现场主管、系统 | 中 | 否 | `evidence.attachment_bound` | 上传采用预签名三段式 |
| 异常复核创建 | `evidence.review.create` | 风控、运营主管、法务 | 高 | 是 | `evidence.review_created` | 用于风险、退款等复核（原含「人工放行」，已废止·见文首口径） |
| P0 经营总览查看 | `analytics.dashboard.view` | 总控、运营主管、财务 | 中 | 否 | `analytics.dashboard_viewed` | 聚合数据不应暴露明细隐私 |
| 风险预警查看 | `analytics.risk_alert.view` | 风控、运营主管、总控 | 高 | 否 | `risk_alert.viewed` | 明细证据另受敏感权限控制 |
| 供应商适配器查看 | `integration.provider_adapter.view` | IT、平台管理员、采购 | 中 | 否 | `provider_adapter.viewed` | 供应商未定时可查看 mock adapter |
| 供应商适配器创建 | `integration.provider_adapter.create` | IT、平台管理员 | 高 | 是 | `provider_adapter.created` | 可创建 mock 或真实占位 |
| 供应商适配器更新 | `integration.provider_adapter.update` | IT、平台管理员 | 高 | 是 | `open_mapping.changed` | 字段映射、Webhook/轮询、密钥引用 |
| 供应商适配器 mock 测试 | `integration.provider_adapter.test` | IT、测试、平台管理员 | 中 | 否 | `provider_adapter.tested` | 供应商未定不得阻塞 P0 mock 验收 |
| 供应商适配器生产就绪标记 | `integration.provider_adapter.production_ready` | IT、采购、法务资料齐备后的授权人 | 高 | 是 | `provider_adapter.production_ready_marked` | 不得由开发人员单独标记生产就绪 |

### 3.2 OpenAPI 接口级补充权限码

以下权限码和通用状态来自当前 OpenAPI，用于补齐接口级 `x-permission` 字典，不新增业务范围。完整 operation 映射以 `p0-openapi-contract-closure-appendix-v0.1.md` 为准。

| 权限编码/通用状态 | 用途 | 授权口径 |
|---|---|---|
| `access.user.view` | 账号列表/账号查看接口 | 平台管理员、HR 权限管理员、运营主管按数据范围查看；**F-AUTHZ-01 跨租户读收口**：非平台 scope 仅可读本商户账号，越界查询返回 403 `CROSS_MERCHANT_FORBIDDEN`（详见 §3.3） |
| `access.user.manage` | 账号创建/维护接口 | 平台管理员、HR 权限管理员，高风险变更需留痕 |
| `amusement.device_status.view` | 游乐设备状态查看 | 游乐主管、平台管理员、授权运维查看 |
| `amusement.project.manage` | 游乐项目规则维护 | 游乐主管、平台管理员维护，影响开放规则时需复核 |
| `amusement.queue.view` | 游乐排队/队列查看 | P2 深化前只作查看和状态参考，不开放复杂预约排队 |
| `amusement.reservation.view` | 游乐预约记录查看 | P2 深化前只作查看，不作为 P0 预约排队承诺 |
| `analytics.risk_alert.manage` | 风险预警创建/处理 | 风控、运营主管、总控授权人 |
| `bar.order.view` | 酒吧订单查看 | 酒吧员工、运营主管、财务按酒吧/卡座范围查看 |
| `bar.reservation.view` | 酒吧订台记录查看 | 酒吧员工、运营主管、销售人员按数据范围查看 |
| `evidence.event.create` | 终端/系统批量上报事件 | 仅授权终端或系统服务按认证上下文调用 |
| `finance.reconciliation.view` | 财务对账批次查看 | 财务、运营主管、总控按数据范围查看 |
| `inventory.stock.view` | 库存余额/库存流水查看 | 仓库、吧台、财务、运营主管按仓库/酒吧范围查看 |
| `ops.production_readiness.view` | 生产前置责任状态查询 | 总控、IT、运维、项目负责人按项目范围查看 |
| `ops.production_readiness.manage` | 生产前置责任状态维护 | IT、运维、项目负责人维护，状态变更需留痕 |
| `ops.production_readiness.check` | 生产准入检查 | 总控、IT、运维、项目负责人执行真实生产前置检查 |
| `ticket.entitlement.view` | 票务权益模板/已发权益查看 | 票务、客服、运营主管按数据范围查看 |
| `ticket.entitlement.manage` | 票务权益模板绑定/维护 | 票务主管、平台管理员，影响票种权益时需审批或复核 |
| `ticket.verify.view` | 票务核销记录查看 | 票务、现场主管、客服、审计按数据范围查看 |
| `public` | 公开访问通用状态 | 不是员工权限码；必须配合 `x-auth-context=public` 和公开数据范围 |
| `authenticated` | 已登录通用状态 | 不是岗位权限码；必须配合 `x-auth-context=staff-token` 或对应认证上下文 |

### 3.3 治理写入与跨租户读收口（F-AUTHZ，2026-06-22 登记）

> 本节登记两条授权治理口径，作为 §3/§3.1/§3.2 既有权限码的横切约束（仅追加，不改既有行）。约束在 Service/授权层强制，违反时按下表错误码拒绝并留痕。

| 编号 | 约束 | 适用范围 | 拒绝错误码 | 实现要点 |
|---|---|---|---|---|
| F-AUTHZ-02 | 治理写仅平台 | 平台治理对象写接口（`governance.*` 维护、`entity.binding.manage`、平台级配置等仅平台可变更的对象） | 403 `FORBIDDEN`（跨租户写一律拒绝） | 写路径调用 `requirePlatformScope()`：仅平台 scope（`currentMerchantScope()` 为空/平台）可执行治理写；商户 scope 即便被全开放也不得写平台全局对象 |
| F-AUTHZ-01 | `access.user.view` 跨租户读收口 | 账号读接口 `listAccessUsers`、`getUserEffectivePermissions` 等以 `access.user.view` 授权的查询 | 403 `CROSS_MERCHANT_FORBIDDEN` | 非平台 scope 仅可读本商户账号；按 `currentMerchantScope()` 过滤，越界查询其他商户账号或其有效权限集时拒绝并记失败事件 |

## 4. 端侧权限边界

| 端 | P0 可执行动作 | 禁止动作 |
|---|---|---|
| 管理后台 Web | 配置、审批、对账、报表、权限模板 | 不能绕过审批直接改锁定金额 |
| 商户门户/商户 App | 授权范围内查看经营、核销、订单、对账 | 不能查看其他商户数据 |
| 工作手机 | 扫码核销、NFC 外部介质读取、现场证据采集 | 不能维护全局价格、合同、财务规则 |
| 平板端 | 酒吧点单、最低消费校验、支付引导 | 不能购票、核销门票、游乐核销或导出敏感数据 |
| 一体机 | 售票型：授权服务人员辅助到店客人购票/取票/订单查询；非售票型：权益展示/领取、订台、吧台买酒、游乐状态查看和授权状态检查 | 不能访问员工后台；非售票型不得出现购票入口；售票型不得绕过票务价格/库存/支付审批，绑定酒吧门口/前台时不得售卖综合体通票 |
| 小程序/App/Web 游客端 | 购票、会员、订单、二维码展示 | 不能人工修改权益 |

## 5. 验收标准

1. 给定一个基础模板，管理员可以复制为新模板并修改部分权限，源模板后续变化不影响新模板。
2. 给定一个临时模板，系统必须能按起止时间自动生效和失效。
3. 给定一个单次有效模板，员工首次完成授权动作后模板应用记录变为已消耗，第二次不再生效。
4. 给定同一权限同时存在授予和拒绝，系统按拒绝处理，并展示冲突来源。
5. 给定高风险权限变更，未完成审批时不得生效。
6. 每一次模板、角色、员工权限变更都能追溯到人、时间、原因、变更前后快照和终端。

## 6. 边界用例

| 场景 | 预期处理 |
|---|---|
| 临时模板到期时员工正在操作 | 提交时重新校验权限，到期后拒绝并提示申请续期 |
| 模板被停用但已应用到员工 | 已应用权限同步失效，审计记录保留 |
| 员工跨商户/跨酒吧支援 | 必须限定数据范围和终端范围 |
| 主管离职或调岗 | 自动触发权限复核任务，禁用高风险权限 |
| 第三方适配器写入核验结果 | 仅允许写入被授权的结果字段，不授予后台管理权限 |

## 7. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-10 | V0.1 审计收口 | 明确本文为 P0 权限码唯一事实源；补场馆/区域/点位权限；历史 `platform.*`、`access.template.*` 写法统一折算为本文权限码；游乐场次事件统一为 `amusement.session.changed` |
| 2026-06-13 | V0.1 OpenAPI 权限补充 | 补齐当前 OpenAPI 缺失的 17 个接口级权限码/通用状态；接口级完整权限清单以 `p0-openapi-contract-closure-appendix-v0.1.md` 为准 |
| 2026-06-13 | V0.1 生产前置权限收口 | 新增生产前置责任状态查看、维护和准入检查三项权限 |
| 2026-06-19 | R6 全闭环(2026-06-19) | 追加 §8 "商户侧授权范围全开放的权限点逐一映射"，按酒吧/游乐/餐饮零售商户列出授权范围内可用权限点清单与不可见边界（仅追加，未改既有行） |
| 2026-06-22 | V0.1 游乐设备权限与 F-AUTHZ 收口 | §3 新增 `amusement.device.view`（domain=amusement，风险 normal）与 `amusement.device.manage`（风险 high，需审批）；新增 §3.3 登记 F-AUTHZ-02 治理写仅平台（`requirePlatformScope`）与 F-AUTHZ-01 `access.user.view` 跨租户读收口（`CROSS_MERCHANT_FORBIDDEN`）；§3.2 `access.user.view` 行补注 F-AUTHZ-01。仅追加，未改既有权限码与既有行。 |
| 2026-11-03 | V0.11 第十八批 O 段第 130 条同步 | §4 平板端「P0 可执行动作」删去「开台、出酒」，与平板只放行酒吧点酒下单与支付一致（BSM:1901「只做酒吧点酒下单，绑定酒吧和卡座，校验最低消费，支持扫码支付。」；PGM:634「平板只负责酒吧点酒下单。」） |

## 8. 商户侧授权范围全开放的权限点逐一映射

> **标注：R6 全闭环(2026-06-19)。** 本节回应治理 HTML #permission「权限细到单一功能最小颗粒度、角色模板+员工级完全自定义」，把"商户侧授权范围全开放"落成逐一权限点清单：当商户在其被授权范围内（同商户、同项目、所绑空间/业态）被全开放时，下列权限点对该商户**可用**；同时明确商户侧**始终不可见**的平台全局对象。本节为追加，不改 §3/§3.1/§3.2 既有权限码与既有行；权限码以上述既有表为唯一事实源，本节只做"按商户业态的可用性映射"。
>
> **通用前提**：①商户侧所有可用权限点一律按"同商户/同项目数据范围"过滤（端侧边界见 §4 商户门户/商户 App 行：授权范围内查看经营、核销、订单、对账；不能查看其他商户数据）；②高风险动作仍受 §3 既有"是否需审批/必须留痕"约束，全开放指授权范围内可被分配，不等于免审批；③客人 Token/设备凭证/开放平台凭证不是员工权限码（§1）。

### 8.1 酒吧商户（授权范围全开放）可用权限点清单

| 功能（逐一） | 权限编码 | 风险/审批沿用 §3 | 商户侧可用性 |
|---|---|---|---|
| 卡座/桌台查看 | `bar.table.view` | 低/否 | 可用（按本商户酒吧范围） |
| 卡座/桌台维护 | `bar.table.manage` | 中/可配置 | 可用（影响人数、低消、终端绑定） |
| 订台（卡座预订）创建 | `bar.reservation.create` | 中/否 | 可用 |
| 订台记录查看 | `bar.reservation.view` | 低/否 | 可用 |
| 卡座预订入场人数核销 | `bar.table.guest_checkin` | 中/否 | 可用（仅本酒吧预定卡座，需幂等与并发额度锁） |
| 开台/关台 | `bar.table.open_close` | 中/否 | 可用 |
| 点酒/出酒 | `bar.order.create` | 中/否 | 可用 |
| 酒吧订单查看 | `bar.order.view` | 低/否 | 可用（按本酒吧/卡座范围） |
| 账单：酒水赠饮核销 | `bar.drink_benefit.redeem` | 中/否 | 可用（通票赠送权益成本由**娱乐平台公司**承担，2026-07-28；不得跨吧核销） |
| 存酒登记/领取/作废 | `bar.saved_bottle.manage` | 高/领取作废可配置 | 可用（须照片/签收/手机号） |
| 库存入库/出库 | `inventory.stock.move` | 中/可配置 | 可用（本商户库存扣减闭环） |
| 库存余额/流水查看 | `inventory.stock.view` | 低/否 | 可用（按本仓库/酒吧范围） |
| 库存盘点差异确认 | `inventory.stock.adjust` | 高/是 | 可用（高价值酒水需附件，仓库主管/财务复核） |
| 卡座销售归属登记 | `bar.table_sales.attribute` | 中/变更需复核 | 可用 |
| 卡座提成计算 | `bar.table_commission.calculate` | 高/是 | 可用（销售15%/非销售10%，不含通票） |
| 卡座提成查看/复核 | `bar.table_commission.view` / `bar.table_commission.review` | 高/是 | 可用（普通销售仅看本人摘要，不得改基数） |
| 入场年龄核验 | `bar.entry.age_verify` | 高/否 | 可用（售酒/入场合规前置） |
| 票/券/权益核销 | `verification.execute` | 中/否 | 可用（在线核验动态码） |
| 核销撤销（原含「/ 人工放行」，已废止·见文首口径） | `verification.reverse`（`verification.manual_override` 停用，写入口已下线） | 高/是 | 可用（现场主管/总控，须原因/证据） |
| 员工：账号查看/维护 | `access.user.view` / `access.user.manage` | 中-高/高变更留痕 | 可用（限本商户范围内员工） |
| 员工：角色/模板/临时授权 | `access.role.view` / `access.permission_template.view` / `access.temporary_grant.create` / `access.temporary_grant.revoke` | 中-高 | 可用（限本商户范围；员工级完全自定义按 §2.2 组合，DENY 优先） |
| 对账：应收应付台账查看 | `finance.ar_ap.view` | 中/否 | 可用（按主体绑定/数据范围） |
| 对账：财务对账批次查看 | `finance.reconciliation.view` | 中/否 | 可用（按本商户数据范围） |
| 看板：经营总览查看 | `analytics.dashboard.view` | 中/否 | 可用（聚合数据，不暴露明细隐私） |
| 证据/时间线/附件 | `evidence.event.view` / `evidence.timeline.view` / `evidence.attachment.create` | 中/否 | 可用（敏感字段另受 `evidence.sensitive.view` 控制） |

**不可见（酒吧商户侧始终无权）**：平台全局配置（`governance.project.manage`、`terminal.switch.manage`、`openplatform.app.manage`、`device.mdm_policy.apply`、`openplatform.credential.rotate`）、其他商户数据（任何越本商户数据范围的查看/操作）、全局风控（`analytics.risk_alert.manage`、`evidence.sensitive.view` 跨商户、跨商户对账/补偿 `openplatform.reconcile.execute`）。

### 8.2 游乐商户（授权范围全开放）可用权限点清单

| 功能（逐一） | 权限编码 | 风险/审批沿用 §3 | 商户侧可用性 |
|---|---|---|---|
| 项目/场次查看 | `amusement.session.view` | 低/否 | 可用 |
| 项目/场次维护 | `amusement.session.manage` | 中/可配置 | 可用（基础项目容量（实时在玩人数）（原称「场次容量」，已废止·见文首口径）） |
| 设备状态查看 | `amusement.device_status.view` | 中/否 | 可用 |
| 核销记录查看 | `amusement.verification.view` | 低/否 | 可用 |
| 核销执行（原含「/ 人工放行」，已废止·见文首口径） | `amusement.verification.execute`（`amusement.verification.override` 停用，改判接口已下线） | 中/否 | 可用（与票务权益联动） |
| 安全巡检模板查看/维护 | `amusement.safety_template.view` / `amusement.safety_template.manage` | 高/维护可配置审批 | 可用 |
| 安全巡检任务查看/创建/提交 | `amusement.safety_inspection.view` / `.create` / `.submit` | 高 | 可用（开放前必检，阻断项自动暂停） |
| 安全整改/复检 | `amusement.safety_inspection.rectify` / `.recheck` | 高/可配置 | 可用（严重项未复检通过不得恢复） |
| 严重项恢复开放 | `amusement.safety_inspection.approve_reopen` | 高/是 | 可用（复检通过后仍需主管审批） |
| 票/券/权益核销 | `verification.execute` | 中/否 | 可用 |
| 库存入库/出库与查看 | `inventory.stock.move` / `inventory.stock.view` | 中-低 | 可用（如游乐有耗材/物料库存） |
| 员工/角色/临时授权 | `access.user.*` / `access.role.view` / `access.temporary_grant.*` | 中-高 | 可用（限本商户范围） |
| 对账查看 | `finance.ar_ap.view` / `finance.reconciliation.view` | 中/否 | 可用（按数据范围） |
| 看板查看 | `analytics.dashboard.view` | 中/否 | 可用（聚合，不暴露明细） |
| 证据/时间线/附件 | `evidence.event.view` / `evidence.timeline.view` / `evidence.attachment.create` | 中/否 | 可用 |

**不可见（游乐商户侧始终无权）**：平台全局配置（`governance.project.manage`、`terminal.switch.manage`、`openplatform.app.manage`、`device.mdm_policy.apply`）、其他商户数据、全局风控（`analytics.risk_alert.manage`、跨商户敏感证据与对账补偿）。

### 8.3 餐饮/零售商户（授权范围全开放）可用权限点清单

| 功能（逐一） | 权限编码 | 风险/审批沿用 §3 | 商户侧可用性 |
|---|---|---|---|
| 票/券/权益核销 | `verification.execute` | 中/否 | 可用（餐饮/零售券核销，在线核验动态码） |
| 核销记录查看（票务核销） | `ticket.verify.view` | 中/否 | 可用（按数据范围） |
| 库存入库/出库 | `inventory.stock.move` | 中/可配置 | 可用（本商户库存扣减闭环） |
| 库存余额/流水查看 | `inventory.stock.view` | 低/否 | 可用 |
| 库存盘点差异确认 | `inventory.stock.adjust` | 高/是 | 可用（差异需复核/附件） |
| 供应商档案查看/维护 | `inventory.supplier.view` / `inventory.supplier.manage` | 低-中 | 可用（拉黑/恢复留痕） |
| 采购申请查看/维护 | `inventory.purchase_request.view` / `inventory.purchase_request.manage` | 低-中/可配置 | 可用（审批链按金额/品类配置） |
| 员工/角色/临时授权 | `access.user.*` / `access.role.view` / `access.temporary_grant.*` | 中-高 | 可用（限本商户范围） |
| 对账：应收应付/对账批次查看 | `finance.ar_ap.view` / `finance.reconciliation.view` | 中/否 | 可用（按主体绑定/数据范围） |
| 看板：经营总览查看 | `analytics.dashboard.view` | 中/否 | 可用（聚合，不暴露明细隐私） |
| 证据/时间线/附件 | `evidence.event.view` / `evidence.timeline.view` / `evidence.attachment.create` | 中/否 | 可用 |

**不可见（餐饮/零售商户侧始终无权）**：平台全局配置（`governance.project.manage`、`terminal.switch.manage`、`openplatform.app.manage`、`openplatform.mapping.manage`、`device.mdm_policy.apply`）、其他商户数据（任何越本商户数据范围者）、全局风控（`analytics.risk_alert.manage`、跨商户敏感证据 `evidence.sensitive.view`、跨商户对账补偿 `openplatform.reconcile.execute`）。

> **统一边界说明（三类商户共用）**：①票务价格/票种配置（`ticket.product.manage`、`ticket.price_variant.manage`）、出票（`ticket.ticket.issue`）默认属平台/票务主管，非商户侧全开放范围，除非另行授权；②卡座/酒吧专属权限点（`bar.*`）仅对酒吧业态商户开放，游乐/餐饮零售商户即使全开放也不出现卡座/点酒/存酒权限；③"全开放"指在本商户授权范围内权限点**可被分配且默认开放**，最终生效仍按 §2.2「角色 + 模板 + 员工单独授予/限制 + 临时授权 − 显式拒绝」组合，显式拒绝优先。
