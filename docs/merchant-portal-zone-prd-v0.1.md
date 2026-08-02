# 商户专区产品口径 PRD v0.1（3.18）

> 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。

> 2026-07-04 逻辑一致性修订：对齐 `merchant-space-contract-model-v0.1.md` 当前实现模型（商户数据边界=账户自带 merchant scope）+ 厘清 §5 分期与 §6 落地状态口径矛盾；最终业务解释以三份业务 HTML 为准。
>
> 形态裁决（2026-07-02 用户拍板，方案 A）：商户门户 = **后台内商户专区**——商户账号登录现有完整后台，
> 只见与自己相关的账单 / 工单 / 对账菜单；复用既有账号权限体系与终端开关（terminal_type=merchant_portal
> 总开关已在 V197 预留）。**不建**独立网址 / 独立 App；将来商户规模上来需要独立形态时，以本专区能力为
> 内核平滑迁出。本文件为业务口径 PRD，编码任务另行排期。

## 1. 背景与定位

- 商户（租户 / 联营方 / 服务商）当前依赖平台侧人工传达账单与工单进展；已有 ADM-MER-002「商户授权工作台」
  承载平台给商户账号授权的一侧，商户自助的一侧缺正式口径。
- 本专区定位：**商户自助查看与自助发起**，不改变三份业务 HTML 及已批准资金／审批流程确定的责任边界。

## 2. 商户账号与数据边界

- 商户账号 = 既有员工账号体系中 `merchant` 归属账号（access_users.merchant_id），登录入口与平台员工相同。
- 数据边界铁律：**只见本商户**——账单 / 工单 / 对账 / 合同均按账户自带 merchant scope（`access_users.merchant_id`）过滤（复用
  currentMerchantScope 既有管道）；跨商户读一律拒绝并留审计。当前实现的商户隔离键为 `merchants.business_entity_id`；“我的合同”按 `contracts.merchant_id` 命中本商户，主体／铺位关系细节见 `merchant-space-contract-model-v0.1.md`。该实现模型不得覆盖三份业务 HTML。
- 终端管控：受 `merchant_portal` 终端总开关与模块开关约束，关闭即不可用（前端隐藏 + 后端拒绝）。

## 3. P0 能力范围（本专区第一批）

| 能力 | 说明 | 承载 |
| --- | --- | --- |
| 我的账单 | 租金 / 物业费（固定单价×面积）/ 能源费 / 保证金账单与账龄，只读 | 复用 AR/AP 台账商户过滤视图 |
| 我的工单 | 提交报修 / 投诉 / 整改工单，跟踪进展；仅本商户工单 | 复用商户服务工单域 |
| 我的对账 | 查看与本商户相关的结算 / 分成 / 对账单，异议留言 | 复用结算 / 对账域商户视图 |
| 我的合同 | 本商户合同关键信息（租期 / 空间 / 状态）只读 | 复用合同域商户过滤 |
| 费用通知 | 商户费用短信 / 站内通知接收人自助维护 | 复用 V182 接收人配置 |

## 4. 明确不做（P0 边界）

- 不做独立域名 / App / 小程序（形态裁决 A）；不做商户自助改价、自助改合同；
- 不做在线支付通道绑定（费用缴纳仍走平台既有收款流程，专区仅展示与提醒）；
- 不做商户侧数据导出（敏感导出须走平台审批口径，同资金流水导出分权 V196 范式）。

## 5. 分期（历史规划·已被 §6 落地状态取代）

> **口径校正（2026-07-04）**：本节是立项时的分期设想；实际交付时 P0 五能力（我的账单/工单/对账/合同/费用通知）**已一次性全部上线**，见 §6（ADM-MER-003..007，2026-07-02）。故下方 P1/P2 拆分为**历史规划留档**，**当前真实落地状态以 §6 为准**。「P0/P1」在本文指同一批「专区第一批」能力，非两个独立里程碑。

- ~~**P1（第一批编码）**：我的账单 + 我的工单（读 + 发起），菜单按 merchant 账号类型显隐。~~（已并入一次性上线）
- ~~**P2**：我的对账 + 异议留言、我的合同、费用通知自助维护。~~（已并入一次性上线）
- 每批交付须带：权限点最小化清单、页面矩阵行 + Playwright 验收、终端开关联动、审计事件。

## 6. 一期落地状态(2026-07-02)

P0 五能力全部上线(后台「商户专区」菜单组,ADM-MER-003..007):

| 能力 | 页面 | 承载 |
| --- | --- | --- |
| 我的账单 | ADM-MER-003 | 复用商户账单域(只读,账单/账龄/费用构成) |
| 我的工单 | ADM-MER-004 | 复用商户服务工单域(读+发起,创建强制落本商户) |
| 我的对账 | ADM-MER-005 | 商户自助层 /api/v1/merchant-portal/settlements(+异议留言,平台裁决闭环不变) |
| 我的合同 | ADM-MER-006 | 商户自助层 /api/v1/merchant-portal/contracts(关键信息最小披露) |
| 费用通知 | ADM-MER-007 | 复用接收人配置(商户自助口径:强制本商户+登录主体) |

> **联系人手机号 PII 加密（P0-A / ★GAP-07 / SEC-04，迁移 V339，2026-07-27）**：`merchant_fee_notification_recipient.recipient_phone` 落库为 AES-GCM 信封密文（`enc:v1:`），仓库层写时加密、读时解密，业务层拿到的仍是明文；数据库 `CHECK` 约束（`NOT VALID`）卡死任何明文新写入，仓库以外的手写 SQL / 批量导入绕不过；存量明文由启动期 `MultiPartyContactPhonePiiBackfill` 幂等回填就地加密（三层防二次加密）。读端按权限脱敏：持 `pii.contact_phone.view`（高危，默认仅平台管理员）才见明文，其余见掩码（前 3 后 4）。短信触达链路不受影响——发送侧（`MerchantFeeNotificationRecipientResolver`）经仓库读取，拿到的是解密后的明文。本域无按手机号的后端检索路径，加密不影响任何既有查询，也未建盲索引。统一口径见 `compliance-and-personal-information-protection-requirements-v0.1.md` §6.1。


- 分页口径(AUD-005 收尾·#187):/settlements 与 /contracts 走真分页——`page`/`size`(默认 page=1、size=500),`total` 取同 scope 谓词的 `SELECT COUNT(*)` 真值、第 501+ 条经翻页可达(平台管理员全量口径下不再截断/撒谎 total);商户自 scope 仍无 LIMIT 谓词隔离、只见本商户。
- 数据边界铁律已活证据实证:只见本商户 / 跨商户拒 / 员工无商户归属 fail-closed(ops/160/merchant-zone-live-evidence-slice{1,2,34,5}.sh);
- 终端管控:merchant 归属账号整个后台会话按 merchant_portal 终端上报,受总开关(V197)+商户专区模块开关(V199)约束,总开关关闭即整端硬拒(AND 语义);
- 权限点最小化清单:merchant.billing.view / merchant.service_work_order.view+create / merchant.portal.settlement.view+dispute / contract.view / merchant.portal.fee_recipient.manage(V200,商户自助动作不设审批)。

## 7. 与既有文档的关系

- 权限 / 职级：`platform-governance-map-for-operations` HTML「权限和职级」为最终解释权；
- 账单口径：`finance-ar-ap-ledger-development-design`；物业费固定计费见 F-OPS-08；
- 商户授权（平台侧）：ADM-MER-002 既有工作台，与本专区互为两侧。
