# 合同关键日期与证照资质保险到期字段 P0 颗粒度建议 V0.1

> 本文用于回答总控问题：合同关键日期、证照、资质、保险到期字段在 P0 应预留到什么颗粒度。结论先给出：**建议选择方案 B：标准通用事项台账**。它比只放几个日期字段更可靠，又不会把 P0 做成完整法务/资产/资质系统。

## 1. 目标边界

P0 目标不是做完整合同法务系统，也不是做完整商业地产租赁系统。P0 只需要支持三件事：

1. **能提醒**：合同、证照、资质、保险临近到期时，系统能提醒责任人。
2. **能卡流程**：已过期、缺失或不符合准入规则时，后续售票、点酒、经营、结算、供应、检修等流程先被阻断；未命中七类红线且非权限不足的事项才进入升级审批。
3. **能留证据**：所有日期、附件、责任人、提醒、放行、复核都能被审计追溯。

P0 可引入本地 AI 大模型做辅助分析判断：对合同文本、人工录入摘要、证照/保险材料中的关键日期、责任主体、提醒建议、阻断风险和疑似缺失项给出提示；但 AI 结果只作为“待人工确认建议”，不得自动替代合同台账、审批、阻断或放行结论。扫描件 OCR、电子签章、保险理赔材料闭环、复杂证照自动校验仍放 P1/P2。

## 2. 可选方案

| 方案 | 做法 | 优点 | 风险 | 建议 |
|---|---|---|---|---|
| A. 最小字段方案 | 在合同表放 `start_date/end_date/remind_date`，在资质表放 `valid_to/expiry_warning_days` | 最省事、最快编码 | 很快不够用；免租期、续约通知、押金、涨租、保险、消防、特种设备会散落成补丁字段；流程阻断不好统一 | 不推荐，仅适合临时原型 |
| B. 标准通用事项台账 | 合同关键日期独立成 `contract_key_dates`；证照/资质/保险统一成 `compliance_expiry_items` 或扩展现有 `merchant_credentials` | 颗粒度足够，能提醒、卡流程、留证据；实施成本可控；后续能平滑进入 P1 | 需要 P0 多建一到两张轻量表，并做到期扫描任务 | **推荐** |
| C. 完整法务/资质系统 | 合同、证照、保险、资质、审批、续签、法务审查、电子签章、OCR 全量建模 | 能力最完整 | P0 成本过高，会拖慢票务、酒吧、游乐和供应链闭环 | P1/P2 再做 |

## 3. 推荐方案 B 的 P0 字段颗粒度

### 3.1 合同主表保留的基础字段

合同主表只保留“稳定摘要字段”，不要把所有关键日期都堆成列。

| 字段 | P0 要求 | 用途 |
|---|---|---|
| `project_id` | 必填 | 支持未来多综合体 |
| `merchant_id` | 必填 | 合同归属商户 |
| `contract_no` | 必填/系统生成 | 合同编号 |
| `contract_name` | 必填 | 页面展示 |
| `cooperation_mode` | 必填 | 租赁、联营、自营、代运营、服务商接入等 |
| `business_scope_summary` | 必填 | 后续准入判断依据 |
| `space_scope_summary` | P0 推荐 | 经营空间范围摘要 |
| `settlement_summary` | P0 推荐 | 租金、分成、保底、结算原则摘要 |
| `contract_start_date` | 必填 | 合同有效期起 |
| `contract_end_date` | 必填 | 合同有效期止 |
| `status` | 必填 | 草拟、待确认、待生效、已生效、续签中、已到期、已终止、归档 |
| `admission_action` | 系统固定、只读 | 固定表示超合同/过期时先阻断，未命中七类红线且非权限不足的事项才进入升级审批；不得配置为仅预警或跳过阻断 |
| `approval_level_on_violation` | 条件必填 | 需要放行时的审批层级 |
| `attachment_group_id` | P0 推荐 | 合同、补充协议、附件归档 |

### 3.2 `contract_key_dates` 合同关键日期表

建议 P0 正式预留独立表，核心是“一个关键日期一条记录”。

| 字段 | P0 要求 | 说明 |
|---|---|---|
| `id` | 必填 | 主键 |
| `project_id` | 必填 | 综合体项目 |
| `contract_id` | 必填 | 关联合同 |
| `merchant_id` | 冗余必填 | 便于按商户查询和提醒 |
| `date_type` | 必填 | 日期类型枚举 |
| `date_name` | P0 推荐 | 页面显示名称，如“免租期结束日” |
| `due_date` | 必填 | 关键日期 |
| `remind_days` | 必填，默认 30 | 提前多少天提醒 |
| `remind_level` | P0 推荐 | 普通、重要、紧急 |
| `block_policy` | 必填 | `none/warn/block/escalate` |
| `approval_level_on_due` | 条件必填 | 到期后允许放行所需层级 |
| `owner_dept` | P0 推荐 | 招商、运营、财务、法务、工程等 |
| `owner_user_id` | P0 推荐 | 责任人 |
| `attachment_required` | P0 推荐 | 到期处理是否必须上传附件 |
| `attachment_group_id` | 选填 | 对应补充协议、付款凭证、续约材料 |
| `status` | 必填 | active、due_soon、expired、waived、renewed、closed |
| `last_reminded_at` | 选填 | 最近一次提醒时间 |
| `closed_by/closed_at/close_reason` | 选填 | 关闭或豁免留痕 |
| `created_at/updated_at/created_by/updated_by` | 必填 | 审计字段 |

`date_type` 首批建议枚举：

| 类型 | P0/P1 | 说明 |
|---|---|---|
| `contract_start` | P0 | 合同开始 |
| `contract_end` | P0 | 合同结束 |
| `renewal_notice` | P0 | 续约/终止提前通知日 |
| `rent_free_end` | P0 预留 | 免租期结束 |
| `deposit_due` | P0 预留 | 押金缴纳或退还节点 |
| `rent_escalation` | P1 | 阶梯涨租生效日 |
| `settlement_cycle_change` | P1 | 结算周期变更 |
| `custom` | P0 | 后台自定义关键日期 |

### 3.3 证照、资质、保险到期台账

现有文档已有 `merchant_credentials`，P0 可以直接扩展它，不强制重命名。为了后续更通用，建议产品口径称为“合规到期事项”，技术落地可先复用/扩展 `merchant_credentials`。

| 字段 | P0 要求 | 说明 |
|---|---|---|
| `id` | 必填 | 主键 |
| `project_id` | 必填 | 综合体项目 |
| `subject_type` | 必填 | merchant、platform、supplier、service_provider、amusement_project、equipment |
| `subject_id` | 必填 | 关联主体 |
| `merchant_id` | 条件必填 | 商户相关事项必填 |
| `item_type` | 必填 | business_license、food_license、alcohol_license、fire_safety、special_equipment、insurance、other |
| `item_name` | 必填 | 证照/资质/保险名称 |
| `document_no` | 选填/P0 推荐 | 证照编号、保单号 |
| `issuing_authority` | 选填 | 发证机关或保险公司 |
| `valid_from` | 选填 | 有效期开始 |
| `valid_to` | 必填 | 有效期结束 |
| `expiry_warning_days` | 必填，默认 30 | 到期预警天数 |
| `expiry_action` | 系统固定、只读 | status=expiring 时派生为 warn；status∈{expired,revoked} 时派生为 block。升级审批另行留痕，不作为可配值 |
| `approval_level_on_expired` | 条件必填 | 过期后放行审批层级 |
| `is_required` | 必填 | 是否该业态必填 |
| `required_reason` | 选填 | 必填原因，如“酒吧售酒必填酒类许可” |
| `business_scope` | 选填 | 适用经营范围 |
| `space_scope` | 选填 | 适用空间/点位范围 |
| `attachment_group_id` | P0 推荐 | 证照影像、保单、批文附件 |
| `status` | 必填 | pending_review、valid、expiring、expired、revoked、waived、archived |
| `owner_dept` | P0 推荐 | 责任部门 |
| `owner_user_id` | P0 推荐 | 责任人 |
| `last_checked_at` | 选填 | 最近一次复核时间 |
| `last_reminded_at` | 选填 | 最近一次提醒时间 |
| `waive_reason/waived_by/waived_at` | 条件必填 | 豁免或升级放行留痕 |
| `created_at/updated_at/created_by/updated_by` | 必填 | 审计字段 |

`item_type` 首批建议：

| 类型 | P0/P1 | 说明 |
|---|---|---|
| `business_license` | P0 | 营业执照 |
| `food_license` | P0 | 餐饮/食品经营许可 |
| `alcohol_license` | P0 | 酒类经营相关许可或备案材料 |
| `fire_safety` | P0 | 消防验收、消防证明或场地方消防材料 |
| `special_equipment` | P0 预留/P1 深化 | 特种设备、游乐设施相关资质 |
| `public_liability_insurance` | P0 预留 | 公众责任险 |
| `employer_liability_insurance` | P1 | 雇主责任险 |
| `property_insurance` | P1 | 财产险 |
| `supplier_qualification` | P0 预留 | 供应商资质 |
| `inspection_certificate` | P1 | 检修/检测合格证明 |
| `other` | P0 | 自定义 |

## 4. 流程卡控建议

| 场景 | P0 处理 |
|---|---|
| 合同已过期 | 后续经营、结算、票务权益绑定、商户侧操作先阻断；未命中七类红线且非权限不足的事项才进入升级审批 |
| 合同关键日期临近 | 生成提醒，不直接阻断 |
| 必填证照缺失 | 对应业态业务阻断，例如酒吧售酒、餐饮出餐、游乐特种设备开放 |
| 证照/资质/保险已过期 | `expiry_action` 由系统派生为 block 并先阻断；未命中七类红线且非权限不足的事项才进入升级审批 |
| 证照临近到期 | 生成提醒，进入治理总览 |
| 高级审批放行 | 必填放行原因、审批层级、审批人、附件/证据，写入事件中心 |

## 5. 页面与提醒颗粒度

P0 页面不需要复杂法务视图，但至少应有：

1. 合同详情页：合同摘要、开始/结束、关键日期列表、附件、准入状态。
2. 商户详情页：证照/资质/保险列表、有效期、到期状态、阻断策略。
3. 治理总览：30 天内到期、已过期、已阻断、已升级审批放行。
4. 提醒任务列表：责任人、事项、到期日、状态、处理动作。
5. 准入拦截详情：哪个合同/证照/保险导致阻断，是否允许发起升级审批。

提醒规则建议：

| 类型 | 默认提醒 |
|---|---|
| 合同结束 | 90/60/30/7 天 |
| 续约通知 | 以合同约定日期为准 |
| 证照/资质 | 60/30/7 天 |
| 保险 | 30/15/7 天 |
| 消防/特种设备 | 60/30/7 天，过期默认阻断 |

## 6. 推荐总控选择

我建议你选择：

**方案 B：标准通用事项台账。**

执行口径：

1. 权限点首批由平台统一预置，实施阶段可边配置边补充。
2. 合同主表只放基础摘要字段。
3. 合同关键日期用 `contract_key_dates` 独立承载。
4. 证照/资质/保险到期用现有 `merchant_credentials` 扩展，或等价落为 `compliance_expiry_items` 通用台账。
5. P0 做提醒、阻断、升级审批、附件归档和留痕；OCR、电子签章、保险理赔、复杂费用账单进入 P1/P2。

## 7. 总控拍板

| 事项 | 已确认口径 |
|---|---|
| 合同关键日期颗粒度 | 选方案 B，独立 `contract_key_dates` |
| 证照/资质/保险颗粒度 | 选方案 B，扩展 `merchant_credentials` 或落 `compliance_expiry_items` |
| 默认到期提醒 | 合同 90/60/30/7；证照 60/30/7；保险 30/15/7 |
| 默认阻断策略 | 必填证照缺失/过期、合同过期、消防/特种设备过期默认阻断；允许高级审批放行 |
| P0 是否引入本地 AI 大模型辅助判断 | 引入，但只做关键日期、证照风险、缺失项和异常条款的本地辅助分析，人工确认后才能入台账、提醒、阻断或放行 |
| 是否 P0 做 OCR/电子签章/保险理赔 | OCR 扫描件识别、电子签章、保险理赔不做，放 P1/P2；可读取的文本型合同可由本地 AI 辅助分析 |
