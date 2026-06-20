# 四类主体唯一绑定开发设计 V0.1

> 版本：V0.1 ｜ 最后更新：2026-06-20 ｜ 覆盖迁移基线：见各小节迁移号（最新 V124）｜ 状态：现行
> 新功能承接见 docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md
> <!-- 文档goalP3(2026-06-20)：补版本/日期/覆盖迁移头，未改正文 -->

文档类型：Implementation  
适用范围：平台治理 P0、合同、结算、权限、看板、第三方接入。  
目标：把“经营对象、运营主体、合同主体、结算主体可以不同名/不同公司，但必须唯一绑定并留痕”的总控口径落成统一数据模型。

## 1. 定义

| 名称 | 含义 | 示例 |
|---|---|---|
| 经营对象 | 平台内部识别的经营单元，可对应一个门店、酒吧、游乐项目组合或商户经营单元 | “A区酒吧经营对象” |
| 运营主体 | 实际负责运营的人或公司 | “某运营管理有限公司” |
| 合同主体 | 与平台签署合同的主体 | “某商业管理有限公司” |
| 结算主体 | 实际收款/付款/开票/对账的主体 | “某财务结算有限公司” |
| 主体绑定关系 | 上述四类主体在某一经营对象下的生效组合 | `BEB-20260610-0001` |

## 2. P0 原则

1. 同一经营对象同一时间只能有一个生效主体绑定关系。
2. 合同、结算、权限、看板、第三方接入必须引用同一 `business_entity_binding_id`。
3. 主体可不同名、不同公司，但必须记录统一社会信用代码、证照/附件、有效期和授权依据。
4. 变更主体绑定不得覆盖历史；新业务使用新版本，历史业务保留旧版本。
5. 主体绑定关系变更、合同生效/终止、结算主体变更必须进入复核。

## 3. 数据模型

### 3.1 `business_entities`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `entity_name` | varchar | 是 | 主体名称 |
| `entity_type` | enum | 是 | `company`、`individual`、`platform_internal`、`other` |
| `credit_code` | varchar | 条件必填 | 统一社会信用代码/证件号哈希 |
| `legal_representative` | varchar | 否 | 法定代表人 |
| `contact_phone` | varchar | 否 | 联系电话，展示脱敏 |
| `license_attachment_id` | uuid | 否 | 证照附件 |
| `status` | enum | 是 | `draft`、`active`、`suspended`、`terminated`、`archived` |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

### 3.2 `business_entity_bindings`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `binding_no` | varchar | 是 | 主体绑定关系编号 |
| `business_object_id` | uuid | 是 | 经营对象 |
| `operator_entity_id` | uuid | 是 | 运营主体 |
| `contract_entity_id` | uuid | 是 | 合同主体 |
| `settlement_entity_id` | uuid | 是 | 结算主体 |
| `authorization_basis` | enum | 是 | `contract`、`supplement_agreement`、`trial_authorization`、`management_decision` |
| `authorization_attachment_id` | uuid | 是 | 授权依据附件 |
| `valid_from` | date | 是 | 生效日 |
| `valid_to` | date | 否 | 失效日 |
| `status` | enum | 是 | `draft`、`pending_review`、`active`、`suspended`、`expired`、`terminated`、`rejected` |
| `version` | int | 是 | 同一经营对象下版本号 |
| `created_by` | uuid | 是 | 创建人 |
| `reviewed_by` | uuid | 否 | 复核人 |
| `reviewed_at` | timestamp | 否 | 复核时间 |

唯一约束：

| 约束 | 说明 |
|---|---|
| `project_id + binding_no` 唯一 | 编号唯一 |
| `business_object_id + status=active` 唯一 | 同一经营对象只能一个生效绑定 |
| `business_object_id + version` 唯一 | 历史版本可追溯 |

### 3.3 `business_entity_binding_usages`

用于追踪哪些对象引用了该绑定关系。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `business_entity_binding_id` | uuid | 是 | 主体绑定 |
| `usage_type` | enum | 是 | `contract`、`ar_ap_item`、`permission_scope`、`dashboard_scope`、`open_app`、`third_party_mapping` |
| `usage_id` | uuid/varchar | 是 | 引用对象 |
| `created_at` | timestamp | 是 | 创建时间 |

## 4. 流程

### 4.1 首次绑定

1. 平台运营创建经营对象。
2. 录入运营主体、合同主体、结算主体。
3. 上传合同或授权附件。
4. 提交复核。
5. 复核通过后生成生效绑定关系。
6. 后续合同、结算、权限和第三方应用只能选择该生效绑定关系。

### 4.2 绑定变更

1. 发起新版本绑定关系。
2. 系统检查历史未结应收应付、未完成合同、未关闭差异和第三方接入。
3. 复核通过后新版本生效。
4. 新业务引用新版本，历史记录仍引用旧版本。

### 4.3 异常阻断

| 异常 | 系统动作 |
|---|---|
| 合同主体与绑定关系不一致 | 阻断合同生效 |
| 结算主体与应收应付台账不一致 | 阻断台账确认 |
| 第三方应用绑定主体与合同不一致 | 阻断上线或进入高级审批 |
| 经营对象无生效绑定 | 阻断新增经营动作 |
| 绑定关系已失效 | 不允许新业务引用 |

## 5. API 建议

| API | 方法 | 用途 |
|---|---|---|
| `/api/v1/platform/business-entities` | CRUD | 主体档案 |
| `/api/v1/platform/business-entity-bindings` | GET/POST | 主体绑定草稿、创建和查询 |
| `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 提交变更、复核通过或驳回 |
| `/api/v1/platform/business-entity-bindings/active?businessObjectId=` | GET | 查询经营对象当前生效绑定 |
| `/api/v1/platform/business-entity-bindings/{id}/usages` | GET | 查询引用对象 |

## 6. 权限

| 动作 | 权限 | 说明 |
|---|---|---|
| 创建主体 | `entity.binding.manage` | 商户不可自行创建正式主体 |
| 提交绑定 | `entity.binding.manage` | 需附件 |
| 复核绑定 | `entity.binding.manage` | 高风险动作，必须留变更前后快照 |
| 查看绑定 | `entity.binding.view` | 商户仅看自身授权范围 |
| 导出绑定 | 平台治理导出 | 二次确认 + 审计 |

## 7. 反模式

| 不要这样做 | 应该这样做 | 原因 |
|---|---|---|
| 在合同、结算、第三方接入里各自存一套主体文本 | 统一引用 `business_entity_binding_id` | 防止主体漂移 |
| 变更主体时覆盖旧记录 | 新增版本并保留旧引用 | 历史账务和合同必须可追溯 |
| 允许无附件的正式绑定 | 必须上传合同/补充协议/授权文件 | 主体责任需要依据 |
| 只校验商户 ID 不校验结算主体 | 结算必须校验 settlement entity | 避免收付款主体错误 |
| 把“空间共享”默认视为主体共享 | 共享必须显式授权和绑定 | 防止越权经营 |

## 8. 测试用例规格

| 测试 ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| GOV-BEB-UT-001 | 首次创建绑定 | 四类主体 + 附件 | 生成草稿绑定编号 |
| GOV-BEB-UT-002 | 复核通过 | 待复核绑定 | 状态变 active，同经营对象无其他 active |
| GOV-BEB-UT-003 | 重复生效阻断 | 同经营对象已有 active | 阻止第二个 active |
| GOV-BEB-UT-004 | 合同主体不一致 | 合同选择不同主体 | 阻断合同生效 |
| GOV-BEB-UT-005 | 变更新版本 | 原绑定 active，新绑定复核通过 | 新业务引用新版本，历史不改 |
| GOV-BEB-IT-001 | 绑定到应收应付 | 合同费用生成台账 | 台账引用同一绑定关系 |
| GOV-BEB-IT-002 | 绑定到开放应用 | 第三方应用上线 | 主体与合同一致才允许上线 |
| GOV-BEB-IT-003 | 绑定失效 | 绑定 terminated | 新经营动作被阻断 |

## 9. 错误处理矩阵

| 错误 | 检测 | 响应 | 留痕 |
|---|---|---|---|
| 缺少附件 | 提交复核前校验 | 阻断提交 | 操作审计 |
| 主体证照缺失 | 主体档案校验 | 标记待补充，不允许正式绑定 | 主体审计 |
| 同经营对象重复 active | 唯一约束/业务校验 | 阻断 | 错误日志 + 审计 |
| 引用已失效绑定 | 业务动作前校验 | 阻断或高级审批 | 准入事件 |
| 第三方主体不一致 | 上线前校验 | 阻断上线 | 接入审计 |

## 10. 验收标准

采用 `pm-critic-and-acceptance-criteria-20260610.md §3.2` 的 Given/When/Then 作为 P0 验收门槛。补充两条：

1. Given 商户存在多个业态，When 绑定关系生效，Then 系统必须按合同经营范围限制业态、空间和结算口径，不得默认全域开放。
2. Given 绑定关系被终止，When 用户查看历史合同和历史台账，Then 历史记录仍显示终止前主体快照和绑定版本。

## 11. 引用

| 主题 | 文档 |
|---|---|
| 平台治理 PRD | `platform-governance-prd-v0.1.md §7.4-§7.5` |
| 字段字典 | `platform-governance-product-spec-v0.2.md` |
| 应收应付 | `finance-ar-ap-ledger-development-design-v0.1.md` |
| PM critic 验收 | `pm-critic-and-acceptance-criteria-20260610.md §3.2` |
| P0 任务清单 | `p0-implementation-task-list.md §18` |
