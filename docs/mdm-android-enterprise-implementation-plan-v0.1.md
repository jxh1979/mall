# MDM / Android Enterprise 生产前置实施方案 V0.1

## 1. 文档定位

本文是工作手机、平板、售票型一体机和非售票型一体机进入真实生产前的设备管控实施入口。它不替代 `android-device-terminal-development-design.md` 的终端功能设计，也不替代 `server-160-runbook-v0.2.md` 的服务器部署流程；本文只回答：设备怎样纳管、谁负责、系统怎样判断设备是否可生产使用。

## 2. 默认方案

P0 默认采用 Android Enterprise / Android Management API + 商业 MDM 的组合方案：

| 项目 | 默认口径 |
|---|---|
| 设备范围 | 工作手机、平板、售票型一体机、非售票型一体机 |
| 责任方 | 平台公司 IT 负责选型、采购、配置和运维 |
| 系统职责 | 保存设备档案、设备凭证、纳管状态、策略状态和终端能力开关 |
| 供应商未确定前 | 自有系统先做字段、接口、状态、mock 和未纳管拦截 |
| 真实生产前 | 必须完成设备纳管、策略下发、丢失处置、应用限制和状态读取验收 |

生产前置总控台账通过 `GET /api/v1/ops/production-readiness/items`、`PUT /api/v1/ops/production-readiness/items/{id}` 和 `POST /api/v1/ops/production-readiness/checks` 承载；本文负责 MDM 实施步骤，系统台账负责展示阻断项、责任部门、计划日期、证据链接和降级方式。

## 3. P0 必须落库字段

| 字段 | 说明 |
|---|---|
| `device_id` | 平台内部设备 ID |
| `device_type` | `staff_work_phone`、`tablet`、`kiosk` |
| `kiosk_mode` | `ticketing` 或 `service`，仅一体机使用 |
| `mdm_provider` | 商业 MDM 或 Android Management API 供应商标识 |
| `mdm_device_ref` | MDM 侧设备 ID |
| `management_status` | `pending`、`managed`、`lost`、`retired`、`blocked` |
| `policy_status` | `pending`、`applied`、`failed`、`outdated` |
| `last_policy_sync_at` | 最近策略同步时间 |
| `device_credential_hash` | 设备凭证哈希，不保存明文密钥 |
| `capability_switch_profile` | 终端能力开关模板 |
| `owner_department` | 责任部门 |
| `owner_person` | 责任人 |
| `audit_event` | 最近一次纳管、策略、解绑或阻断事件 |

## 4. 系统拦截规则

| 场景 | P0 系统处理 |
|---|---|
| 设备未纳管 | 不得标记为生产可用，高风险接口拒绝调用 |
| 策略下发失败 | 保留测试/演示能力，不允许真实核销、支付、库存扣减和年龄核验 |
| 设备丢失或离职未归还 | 立即切换为 `blocked`，撤销设备凭证，要求 IT 在 MDM 执行远程锁定或擦除 |
| 一体机模式不匹配 | 非售票型一体机不得显示购票入口；售票型一体机必须走支付和出票链路 |
| Android 离线 | P0 仅允许查看策略摘要；高价值酒水核销、支付、卡座入场和票务核销保持在线 |

## 5. 验收标准

Given IT 已在 MDM 中纳管一台工作手机，  
When 设备首次绑定平台账号，  
Then 系统必须能读取或登记 `management_status=managed` 和 `policy_status=applied`，并写入设备绑定事件。

Given 设备状态为 `pending`、`lost`、`retired` 或 `blocked`，  
When 员工尝试执行票务核销、卡座入场、高价值酒水扣减或支付相关操作，  
Then 系统必须拒绝并记录拒绝原因。

Given 售票型一体机绑定 `kiosk_mode=ticketing`，  
When 客人进入一体机首页，  
Then 可展示购票、取票和订单查询入口；非售票型一体机不得展示购票入口。

## 6. 实施 Runbook

1. IT 确认商业 MDM 或 Android Management API 账号、组织、策略模板和测试设备。
2. 后端建立设备档案、设备凭证、纳管状态、策略状态和终端能力开关字段。
3. Android 端接入设备凭证、设备绑定、能力开关和策略摘要展示。
4. 测试使用 mock MDM 状态覆盖 `managed`、`policy_failed`、`lost`、`blocked`。
5. 真实生产前，IT 提供纳管截图、策略下发记录、丢失处置演练记录和设备清单。

## 7. 关联文档

- `android-device-terminal-development-design.md`
- `apps/android-terminal/README.md`
- `p0-end-to-end-acceptance-and-edge-cases-v0.1.md`
- `p0-openapi-operation-acceptance-matrix-v0.1.md`
- `server-160-runbook-v0.2.md`
