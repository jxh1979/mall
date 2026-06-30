# P0 操作事件与证据留痕矩阵 V0.1

> 本文档用于回答“所有关键操作有没有留痕、留什么、谁能看、保存多久、如何复核”。开发、测试、运营和第三方供应商均应按本文设计事件与证据。

## 1. 总原则

- 有业务后果的动作必须产生日志事件；有财务、合规、库存、客户争议风险的动作必须绑定证据。
- 事件记录不可由业务人员直接删除；更正只能通过反向事件、撤销事件或补偿事件完成。
- 证据原件与业务展示分离：普通展示按权限脱敏，原始证据仅限最小授权查看。
- 第三方回调、轮询、人工补录、异常补偿必须使用同一事件模型，不能形成两套账。
- 本文负责高风险业务动作的证据、复核和保留策略；接口级完整成功/失败事件字典以 `docs/openapi/openapi-p0.yaml/json` 和 `p0-openapi-contract-closure-appendix-v0.1.md` 为准。若本文未逐条列出某 OpenAPI 事件名，不代表该事件可以不落库。

## 2. 事件字段最小集

| 字段 | 必填 | 说明 |
|---|---:|---|
| `event_id` | 是 | 全局唯一 |
| `event_name` | 是 | 使用统一事件字典 |
| `occurred_at` | 是 | 事件发生时间 |
| `recorded_at` | 是 | 系统记录时间 |
| `actor_type` | 是 | staff / customer / merchant / system / third_party |
| `actor_id` | 是 | 操作人或系统主体 |
| `terminal_type` | 是 | 采用统一 10 端枚举 |
| `terminal_id` | 否 | 设备、浏览器或第三方应用标识 |
| `object_type` | 是 | ticket / entitlement / bill / stock / contract 等 |
| `object_id` | 是 | 被操作对象 |
| `before_snapshot` | 视情况 | 状态或关键字段变更前 |
| `after_snapshot` | 视情况 | 状态或关键字段变更后 |
| `reason_code` | 高风险必填 | 退款、撤销、人工放行、调账、报损等 |
| `evidence_refs` | 视情况 | 图片、视频、附件、签名、回执、第三方报文 |
| `trace_id` | 是 | 串联请求链路 |
| `idempotency_key` | 第三方/支付必填 | 防重复 |

## 3. P0 操作事件与证据矩阵

| 场景 | 事件名 | 必须证据 | 责任主体 | 复核规则 |
|---|---|---|---|---|
| 商户主体绑定创建 | `entity_binding.created` | 绑定对象快照、合同/流程引用 | 招商/财务 | 高风险，需复核 |
| 商户主体绑定变更 | `entity_binding.changed` | 变更前后快照、审批单、原因 | 财务主管/平台管理员 | 必须审批 |
| 合同提交审批 | `contract.submitted` | 合同附件、摘要字段、提交人 | 招商/法务 | 法务确认 |
| 合同生效/终止 | `contract.lifecycle_changed` | 审批记录、生效/终止依据 | 法务/财务 | 必须审批 |
| 权限模板创建 | `permission_template.created` | 模板权限快照、创建原因 | HR/平台管理员 | 可配置复核 |
| 权限模板复制 | `permission_template.copied` | 源模板 ID、复制快照 | HR/平台管理员 | 中风险留痕 |
| 权限模板应用 | `permission_template.applied` | 目标员工、有效期、审批单 | HR/平台管理员 | 高风险审批 |
| 临时模板到期 | `permission_template.expired` | 系统到期任务记录 | 系统 | 自动复核 |
| 票务价格发布 | `ticket_price.published` | 平日/周末/节假日基础价、派生规则 | 票务主管 | 必须审批 |
| 票务订单支付成功 | `ticket_order.paid` | 聚合支付回执、订单快照 | 系统/支付通道 | 日结对账 |
| 售票型一体机辅助购票 | `kiosk.ticket.order_created` | 一体机设备、点位、服务人员、票种、价格快照、库存锁定、订单快照、辅助售票原因 | 授权服务人员/一体机 | 走官方票务价和支付链路；酒吧门口/前台只售卖本酒吧单店门票 |
| 售票型一体机取票/查单 | `kiosk.ticket.picked_up` | 订单摘要、取票码/手机号摘要、设备、点位、服务人员、防伪二维码状态 | 授权服务人员/一体机 | 不改变票务权益状态 |
| 票务二维码刷新 | `ticket_credential.refreshed` | 哈希、有效期、随机串摘要 | 系统 | 事后核对 |
| 权益核销成功 | `verification.succeeded` | 核销码摘要、员工、终端、定位/设备 | 现场员工 | 异常抽检 |
| 核销拒绝 | `verification.failed` | 拒绝原因、码摘要、终端 | 系统/员工 | 风控统计 |
| 核销撤销 | `verification.reversed` | 原核销记录、原因、审批 | 现场主管 | 必须审批 |
| 人工放行 | `verification.manual_overridden` | 原因、照片/附件、审批人 | 现场主管 | 必须审批 |
| 酒吧年龄核验 | `bar.age_verification.requested` / `bar.age_verification.passed` / `bar.age_verification.rejected` / `bar.age_verification.blocked` | 核验结果、方式、最小必要证明 | 酒吧门口员工/第三方 | 合规抽检 |
| 酒吧人脸核验 | `bar.age_verification.passed` / `bar.age_verification.rejected` / `bar.age_verification.blocked` | 第三方结果流水、活体/比对结果摘要 | 第三方适配器 | 供应商准入后启用 |
| 卡座客人入场 | `bar.table_guest.checked_in` / `bar.table_guest.rejected` | 预订卡座、人数额度、入场人次、幂等键、剩余额度 | 酒吧门口员工 | 超额拒绝，并发只允许成功扣占一次 |
| 卡座销售归属 | `order.sales_attributed` | 订台/账单、销售人员、岗位快照、来源、变更原因 | 销售人员/运营主管 | 变更需复核 |
| 卡座提成计算 | `bar.table_commission.calculated` / `bar.table_commission.adjusted` | 卡座账单、可计提消费额、排除金额、提成率、提成金额、复核人 | 财务/运营 | 日结后调整只能冲正 |
| 通票赠酒领取 | `bar.gift_redeemed` | 权益记录、领取人、吧台员工 | 吧台 | 平台承担成本 |
| 点酒出酒 | `bar.order_delivered` | 订单、出酒员工、库存扣减结果 | 吧台 | 高价值在线 |
| 客人存酒登记 | `bar.saved_bottle.created` | 瓶底标签、照片、手机号/昵称、备注 | 吧台 | 可复核 |
| 存酒领取 | `bar.saved_bottle_picked_up` | 领取签收、照片、员工、时间 | 吧台 | 高价值复核 |
| 库存入库 | `inventory.inbound_confirmed` | 采购/供应商单据、扫码记录 | 仓库 | 抽检 |
| 库存出库/吧台消耗 | `inventory.outbound_verified` | 出库原因、关联订单/耗用 | 仓库/吧台 | 高价值复核 |
| 库存调整/报损 | `inventory.counted` / `inventory.damaged` / `inventory.surplus_recorded` / `inventory.reversed` | 差异说明、照片/视频、审批 | 仓库主管/财务 | 必须审批 |
| 游乐安全巡检提交 | `amusement.safety_inspection.submitted` / `amusement.safety_inspection.passed` / `amusement.safety_inspection.blocked` | 项目、场次、设备点位、模板版本、单项结果、证据、最高严重度 | 巡检员 | 阻断/重大项自动暂停 |
| 游乐安全整改复检 | `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rechecked` | 整改说明、责任人、整改证据、复检结果、复检证据 | 游乐运营/巡检员 | 复检失败继续暂停 |
| 游乐恢复开放审批 | `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected` | 审批人、审批结论、原因、关联复检记录 | 游乐主管/安全负责人 | 必须审批 |
| 游乐核销 | `amusement.verification_succeeded` / `amusement.verification_failed` | 权益、场次、点位、员工 | 游乐员工 | 容量校验 |
| 游乐人工放行 | `amusement.verification_overridden` | 原因、现场证据、审批 | 游乐主管 | 必须审批 |
| 聚合支付回调 | `payment.gateway_notified` | 原始报文摘要、签名校验结果 | 支付适配器 | 幂等处理 |
| 支付轮询校验 | `payment.poll_checked` | 查询结果摘要、差异状态 | 系统 | 定时对账 |
| 应收应付生成 | `finance_ar_ap.created` | 来源单据、金额、主体绑定 | 财务/系统 | 日结检查 |
| 收付款登记 | `finance_payment.registered` | 支付回执/线下凭证、经办人 | 财务 | 必须审批或复核 |
| 日结锁定 | `finance_daily_close.locked` | 日结范围、金额汇总、差异列表 | 财务主管 | 锁定后只允许补录 |
| 第三方字段映射变更 | `open_mapping.changed` | 字段映射前后快照、供应商版本 | IT/平台管理员 | 必须审批 |
| 第三方密钥轮换 | `open_credential.rotated` | 台账编号、到期提醒、执行人 | IT | Runbook 留痕 |
| 第三方异常补偿 | `open_compensation.executed` | 原始异常、补偿动作、结果 | IT/财务 | 必须复核 |
| API 契约生成 | `api.contract.generated` | OpenAPI 版本、生成时间、生成工具、校验结果 | 后端/架构 | 契约变更需复核 |
| 数据库迁移 | `db.migration.applied` / `db.migration.failed` | Flyway 版本、迁移脚本、执行环境、失败日志 | 后端/运维 | 失败阻断发布 |
| 160 发布 | `deployment.released` | 发布版本、发布人、脚本输出、健康检查结果 | IT/运维 | 失败必须回滚或补救 |
| NAS 备份完成 | `backup.completed` | 快照编号、范围、校验结果、NAS 路径摘要 | IT/运维 | 失败不得判定可恢复 |
| 恢复演练完成 | `restore.drill_completed` | 备份快照、恢复环境、验证结果、报告链接 | IT/运维 | 上线前至少一次通过 |
| 生产前置状态查询 | `production_readiness.viewed` / `production_readiness.view_failed` | 查询人、数据范围、筛选条件、失败原因 | 总控/IT/运维 | 敏感证据链接按权限脱敏 |
| 生产前置状态更新 | `production_readiness.updated` / `production_readiness.update_failed` | 前后状态、责任人、计划日期、证据链接、阻断范围、降级方式 | IT/运维 | 高风险，需复核 |
| 生产准入检查 | `production_readiness.checked` / `production_readiness.check_failed` | 检查范围、阻断项、通过项、执行人、检查时间 | 总控/IT/运维 | 任一阻断项未通过不得标记真实生产就绪 |
| MDM 策略检查 | `mdm.policy_checked` | MDM 状态、策略状态、设备清单、失败设备；MDM 白名单/预装清单/AMAPI 拦截留痕字段（P1） | IT/运维 | **依赖外部 MDM，本期以终端开关代偿，纳管后恢复硬禁用**：本期 MDM 自有侧暂缓，"未纳管设备禁用高风险动作"由**终端开关 + 人工准入**代偿，验收降级，由 IT 安全负责人签字（2026-06-18 业务确认）（见下方 D10 收口口径） |
| 凭证轮换检查 | `credential.rotation_checked` | 凭证台账、到期项、轮换记录、异常项 | IT/运维 | 长期无责任凭证阻断生产 |
| 恢复演练检查 | `restore.drill_checked` | 最近演练时间、结果、失败原因、补救计划 | IT/运维 | 未演练不得判定生产可恢复 |
| QA 用例执行 | `qa.test_executed` | 用例编号、环境、输入摘要、执行结果、缺陷链接 | 测试负责人 | P0 失败项必须闭环 |
| 敏感证据查看 | `sensitive.viewed` | 查看原因、审批单、查看范围 | 法务/风控 | 必须审批 |
| 证据导出 | `evidence.exported` | 导出原因、水印、文件摘要 | 法务/风控 | 必须审批 |

### 3.1 MDM "未纳管禁用" 收口口径（D10，与 `android-device-terminal-development-design` 同一口径，2026-06-18 业务定稿）

本文 `mdm.policy_checked` 原复核规则"未纳管设备不得生产使用"与 `android-device-terminal-development-design`"MDM 自有侧本期暂缓"存在矛盾。两文件统一收口口径如下（2026-06-18 业务定稿）：

- **本期 MDM 自有侧暂缓**：仅保留外部 MDM/Android Enterprise/AMAPI 接口与后台开关占位，不落 `management_status`/`policy_status`/`last_policy_sync_at`/`device_credential_hash` 等自有侧字段。
- **"未纳管设备禁用高风险动作"由终端开关 + 人工准入代偿**：在外部 MDM 纳管未就绪期间，通过终端能力开关关闭未纳管设备的高风险动作（核销、点单、放行、结算、调账等），并以人工准入流程审核设备发放，达到等效"未纳管禁用"效果。
- **验收降级，由 IT 安全负责人签字（2026-06-18 业务确认）**：MDM 强管控验收降级为"终端开关 + 人工准入"代偿验收，由 IT 安全负责人作为签字责任人对降级口径负责留痕。
- **`mdm.policy_checked` 事件标注**：标注"依赖外部 MDM，本期以终端开关代偿，纳管后恢复硬禁用"；外部 MDM 纳管能力就绪后恢复对未纳管设备的硬禁用。
- **MDM 白名单 / 预装清单 / AMAPI 拦截留痕字段为 P1**：相关留痕字段在外部 MDM 纳管落地阶段（P1）补齐，不计入本期 P0 闭环。

验收降级签字责任人 = IT 安全负责人（2026-06-18 业务确认定稿）。

## 4. 证据保留与访问

| 证据类型 | P0 保留策略 | 访问控制 |
|---|---|---|
| 支付/退款/对账回执 | 至少按财务和税务要求保留 | 财务、审计、授权 IT |
| 年龄/人脸核验结果 | 仅保存最小必要结果和第三方流水，不默认保存原始人脸图像 | 酒吧合规、法务最小授权 |
| 工作会话/质检材料 | 按配置留存，到期归档/销毁/匿名化 | 质检、法务、授权主管 |
| 库存/报损照片视频 | 与库存争议周期一致 | 仓库、财务、风控 |
| 合同与主体绑定附件 | 合同生命周期及法定留存期 | 招商、法务、财务 |
| 第三方原始报文 | 默认保存摘要、签名校验结果、状态码、必要业务键；必要原文字段级脱敏后加密保存 | IT、审计 |

### 4.1 接口级事件字典执行口径

1. OpenAPI 每个 operation 的 `x-audit-event-success` 和 `x-audit-event-failure` 必须进入事件中心。
2. `p0-openapi-contract-closure-appendix-v0.1.md` 第 4 节是接口级完整事件字典；本文第 3 节只保留高风险业务场景证据策略。
3. 开发和测试不得因为本文第 3 节未出现某个接口级事件名，就省略该事件落库或测试断言。
4. 新增、删除或重命名 OpenAPI 事件后，必须执行 `npm run docs:openapi-closure` 并通过 `npm run verify:openapi-closure`。

## 5. 验收标准

1. 每个矩阵内事件均能在事件中心按 `event_name`、对象、人员、时间、终端查询。
2. 任一退款、人工放行、库存调整、财务调账、敏感导出都必须能追溯到审批和证据。
3. 第三方回调重复发送时只生成一条有效业务结果，重复请求写幂等日志。
4. 日结锁定后，任何影响金额的变更都不能直接覆盖原记录，只能追加补录/冲正事件。
5. 敏感证据查看和导出必须二次授权并记录用途。
6. 任一 OpenAPI operation 成功或失败时，必须能在 `p0-openapi-contract-closure-appendix-v0.1.md` 找到事件名，并在事件中心查到对应记录。

## 6. 边界用例

| 场景 | 预期处理 |
|---|---|
| 第三方回调成功但业务处理失败 | 记录回调，进入补偿队列，轮询校验最终状态 |
| 现场离线缓存了证据但未提交 | 高价值酒水、核销、支付不允许离线完成；普通附件恢复网络后补传 |
| 同一票码被截图重复使用 | 动态码过期拒绝，事后核对哈希和随机串 |
| 证据文件上传失败 | 业务高风险动作不得完成；低风险动作可暂存为待补证 |
| 人脸供应商未采购完成 | 酒吧正式年龄人脸核验不能标记为已通过，仅可测试环境模拟 |

## 7. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-13 | V0.1 OpenAPI 事件字典收口 | 明确本文负责高风险证据策略，接口级完整成功/失败事件字典以 `p0-openapi-contract-closure-appendix-v0.1.md` 为准；新增 `verify:openapi-closure` 同步闸口 |
| 2026-06-13 | V0.1 运维/质量事件补强 | 新增 API 契约、数据库迁移、160 发布、NAS 备份、恢复演练、生产前置状态、MDM、凭证轮换和 QA 执行事件 |
| 2026-06-18 | D10 MDM 收口【待业务最终确认】 | `mdm.policy_checked` 复核规则由"未纳管设备不得生产使用"改为"依赖外部 MDM，本期以终端开关代偿，纳管后恢复硬禁用"；新增 §3.1 收口口径（终端开关+人工准入代偿、验收降级须签字责任人、MDM 白名单/预装清单/AMAPI 拦截留痕字段 P1），与 `android-device-terminal-development-design` 同一口径 |
| 2026-06-18 | D10 MDM 收口定稿 | §3.1 及 `mdm.policy_checked` 行：验收降级签字责任人由【待业务最终确认】定稿为"IT 安全负责人（2026-06-18 业务确认）"；其余代偿口径（终端开关+人工准入代偿、白名单/预装清单/AMAPI 留痕字段 P1）不变，与 `android-device-terminal-development-design` 同一口径 |
