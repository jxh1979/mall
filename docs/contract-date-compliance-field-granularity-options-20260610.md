# 合同关键日期与证照资质保险到期开发设计 V0.11

> 版本：V0.11 ｜ 最后更新：2026-08-31 ｜ 状态：现行开发设计
> 业务最终解释权：三份业务 HTML。本文把 HTML 规则映射到 V028/V051 当前实现，并明确尚未落地的差距。

## 1. 业务目标

1. 合同、证照、资质和保险临近到期时，提前提醒责任人并同时通知商户侧。
2. 超合同范围、超空间范围、过期、资质异常、权限不足时先阻断；权限不足不设放行通道。
3. 证照或资质到期后，只能申请一个宽限期；宽限期时长按证照类型可配且须同时配置上下限，经最高权限审批后生效，期间可经营但每日提醒；超过宽限期仍未更新的一律阻断，不得再次延长。
4. 日期、材料、责任人、提醒、审批、阻断和处理结果均可追溯。
5. 合同先线下签纸质，系统上传扫描件归档；合同租金规则包括递增和免租期。
6. 合同到期、续约及其他合同所在地日期判断，以合同对应门店/点位的当地时间为准，不使用服务器默认时区。

P0 可引入本地 AI 大模型做辅助分析判断：对合同文本、人工录入摘要、证照/保险材料中的关键日期、责任主体、提醒建议、阻断风险和疑似缺失项给出提示；但 AI 结果只作为“待人工确认建议”，不得自动替代合同台账、审批、阻断或放行结论。

## 2. 方案结论

当前采用通用事项台账：

- 合同主表保存稳定摘要和合同快照。
- `contract_key_dates` 一行承载一个合同关键日期。
- `compliance_expiry_items` 承载证照、资质和保险到期事项。
- `compliance_reminders` 承载扫描产生的提醒。

V028 已建立上述三张表，V051 为提醒表补商户隔离与去重约束。本文件不再把方案写成待选择建议，也不再声称存在 `merchant_credentials` 表。

## 3. 合同主表业务字段

合同至少承载：

| 业务字段 | HTML 规则 | 当前实现 |
|---|---|---|
| 商户、项目、空间 | 定义合同归属和经营空间 | `merchant_id/project_id` + `contract_spaces` |
| 合作模式 | 自营、租赁、联营、代运营、服务商接入、其他；选“其他”须备注，并在合同生效前指定套用的收费模板 | `contracts` 当前没有明确合作模式字段；`merchants.cooperation_form` 只是无 CHECK 的商户字段，不能替代合同的合作模式、备注和收费模板选择；“其他”模式的生效校验尚未实现 |
| 经营范围 | 合同勾选允许经营品类，超范围当场阻断并按规定申请审批 | `allowed_business_scope` |
| 租期 | 合同有效期起止 | `effective_date/expiry_date` |
| 租金与结算摘要 | 固定租金、每平米、抽成、物业费、能源费、押金、违约金、递增、免租期 | `rent_terms/commission_mode/energy_fee_basis/deposit_cent/penalty_terms/settlement_rules`；公区能源分摊模式仍待按 HTML 收敛 |
| 主体 | 合同、运营、结算主体引用同一四类主体绑定 | 当前三个主体列未与 active binding 强制一致 |
| 状态 | 草拟、待确认、待生效、已生效、续签中、已到期、已终止、归档 | 当前代码另有内部 `amended`，不得改写 HTML 八态 |
| 附件 | 线下纸质签署后上传扫描件归档 | `attachment_ids` |
| 快照 | 审批生效时固化租金规则、物业费/能源费口径、分成比例、押金/违约金条款四项供下游引用 | `snapshot`；当前 `buildSnapshot()` 写 `rentTerms/energyFeeBasis/commissionMode/depositCent/penaltyTerms/frozenAt`，但费用字段混用、旧公区能源模式仍未收敛，`commissionMode` 也不是明确的分成比例，四项口径尚未完整准确落库 |

合同到期不得由系统无审批自动终止。当前批任务直接把 effective 改为 expired，与 HTML 冲突，须作为实现差距处理。

## 4. `contract_key_dates`

### 4.1 当前 V028 字段

| 字段 | 当前类型/约束 | 说明 |
|---|---|---|
| `id` | `VARCHAR(80)` PK | 关键日期 ID |
| `contract_id` | `VARCHAR(80)` NOT NULL | 合同 |
| `merchant_id` | `VARCHAR(80)` NOT NULL | 商户查询维度 |
| `date_type` | `VARCHAR(40)` + CHECK | `contract_start/contract_end/renewal_notice/deposit_return/rent_free_end/custom` |
| `title` | `VARCHAR(160)` NOT NULL | 页面名称 |
| `due_date` | `DATE` NOT NULL | 到期日 |
| `reminder_days` | `JSONB` NOT NULL，默认 `[]` | 提醒提前日集合；业务默认不得在代码写死 |
| `status` | `VARCHAR(32)` + CHECK | `active/completed/waived/archived` |
| `owner_user_id` | `VARCHAR(80)` NOT NULL | 责任人 |
| `source_type` | `VARCHAR(32)` + CHECK | `manual/ai_suggestion/imported` |
| `human_confirmed` | `BOOLEAN` NOT NULL | 人工确认标记 |
| `evidence_attachment_ids` | `JSONB` NOT NULL，默认 `[]` | 证据附件 |
| `created_at/updated_at` | `TIMESTAMPTZ` | 时间留痕 |

### 4.2 HTML 目标与当前差距

- 合同约定的租金递增幅度、免租期、交付日和装修截止日均须录入系统供自动计算账目；系统只据以计算，不对合同约定本身作判断。
- 交付日即起租日，可用 `contract_start` 承载；免租期及递增幅度由 `rent_terms` 承载，免租期结束另有 `rent_free_end`；租金递增是幅度和租金条款，不属于关键日期类型。
- 装修截止日由合同约定，商户须在该日之前完成，铺位一般按毛坯交付。该日期当前在 `date_type` 中无专门值，只能落 `custom`，尚未完整承载；合同结束和续约通知已有当前类型。
- 合同结束应提醒，但三份 HTML 没有规定固定提醒组合；提醒时点必须来自合同约定或经授权的可配业务参数，不得由文档自设默认。
- 合同到期终止须走审批；日期提醒记录不能直接替代审批。
- 合同到期、续约及其他合同所在地日期判断，须按合同对应门店/点位当地时间执行，不使用服务器默认时区；当前 `LocalZone.ID` 固定为 `Asia/Shanghai`，只承载当前单地实现，尚未满足跨地区合同日期判断。

## 5. `compliance_expiry_items`

### 5.1 当前 V028 字段

| 字段 | 当前类型/约束 | 说明 |
|---|---|---|
| `id` | `VARCHAR(80)` PK | 到期事项 ID |
| `subject_type` | `VARCHAR(40)` + CHECK | `merchant/contract/supplier/asset/amusement_device` |
| `subject_id` | `VARCHAR(80)` NOT NULL | 关联对象 |
| `merchant_id` | `VARCHAR(80)` NOT NULL | 商户查询维度 |
| `item_type` | `VARCHAR(40)` + CHECK | `business_license/food_license/alcohol_license/fire_certificate/special_equipment/insurance/supplier_qualification/inspection_certificate/custom` |
| `item_name` | `VARCHAR(160)` NOT NULL | 事项名称 |
| `document_no` | `VARCHAR(120)` | 证照/保单编号 |
| `issuing_authority` | `VARCHAR(160)` | 发证机构 |
| `valid_from/valid_to` | `DATE` / `DATE` NOT NULL | 有效期；结束日不得早于开始日 |
| `reminder_days` | `JSONB` NOT NULL，默认 `[]` | 提醒提前日集合 |
| `expiry_action` | `VARCHAR(40)` + CHECK | `remind_only/block_operation/supervisor_approval` |
| `status` | `VARCHAR(32)` + CHECK | `active/completed/waived/archived` |
| `owner_user_id` | `VARCHAR(80)` NOT NULL | 责任人 |
| `source_type` | `VARCHAR(32)` + CHECK | `manual/ai_suggestion/imported` |
| `human_confirmed` | `BOOLEAN` NOT NULL | 人工确认标记 |
| `evidence_attachment_ids` | `JSONB` NOT NULL，默认 `[]` | 证据附件 |
| `created_at/updated_at` | `TIMESTAMPTZ` | 时间留痕 |

### 5.2 HTML 目标与当前差距

1. 业态资质按国家法律要求确定，至少覆盖酒类、食品、烟草等；当前 `item_type` 无烟草专门值，只能落 `custom`，属于实现差距。
2. 到期预警按证照类型可配，出厂默认提前三个月，必须同时配置上下限；当前 `reminder_days` 只是逐事项 JSON，尚未体现按类型配置、默认值与上下限。
3. 预警同时送达商户侧；当前通用合规端点没有商户专区只读视图。
4. 当前表没有按证照类型配置宽限期时长及上下限，也没有宽限期申请、最高权限审批、生效起止、每日提醒和“不得再次延长”的完整字段/状态机。
5. `expiry_action=supervisor_approval` 不能被解释为过期后可反复高级审批放行。业务只能按 HTML 申请一次最高权限审批的宽限期，宽限期结束后必须阻断。

## 6. `compliance_reminders`

V028/V051 当前字段与约束：

| 字段 | 当前类型/约束 | 说明 |
|---|---|---|
| `id` | `VARCHAR(80)` PK | 提醒 ID |
| `merchant_id` | `VARCHAR(80)`，V051 增加 | 商户隔离维度 |
| `source_type/source_id` | `VARCHAR(40)` + CHECK / `VARCHAR(80)` | 来源限合同关键日期或合规到期事项 |
| `reminder_type` | `VARCHAR(32)` + CHECK | `upcoming/expired` |
| `due_date/reminder_at` | `DATE` NOT NULL | 到期日与提醒日 |
| `severity` | `VARCHAR(16)` + CHECK | `info/medium/high/critical` |
| `status` | `VARCHAR(32)` + CHECK | `open/acknowledged/closed` |
| `blocking_suggested` | `BOOLEAN` NOT NULL | 是否建议阻断 |
| `message` | `VARCHAR(400)` NOT NULL | 提醒内容 |
| `generated_at` | `TIMESTAMPTZ` NOT NULL | 生成时间 |

V051 还以 `(source_type, source_id, reminder_type, due_date)` 唯一约束防止重复扫描产生重复提醒。当前扫描服务的行为是：

- 只扫描 `active` 的合同关键日期和合规到期事项。
- 以 `reminder_days` 最大值或请求回退值判断即将到期。
- 合同结束过期时给阻断建议；合规事项只要不是 `remind_only` 就给阻断建议。

实现差距：`ComplianceService.reminderDays()` 在未填提醒日时仍写入一组代码固定的回退值，扫描入口也有固定回退天数；这些值不是 HTML 业务默认，且 James 已裁固定提醒组合不保留。扫描服务尚未按证照类型配置“默认三个月 + 上下限”，也未实现商户侧通知、宽限期每日提醒、最高权限审批和超期不得再延。

## 7. 本地 AI 辅助边界

### 7.1 允许的输入与输出

- 输入：合同文本、人工录入摘要、证照和保险材料。
- 输出：关键日期、责任主体、提醒建议、阻断风险和疑似缺失项提示。
- 输出状态：只能作为待人工确认建议。

### 7.2 硬边界

- 未经人工确认的 AI 建议不得进入正式台账。
- AI 不得自动发出正式提醒、决定阻断、决定审批或决定放行。
- AI 不得覆盖纸质合同、扫描件归档、人工台账和审批留痕。

当前 V028 已预留 `source_type=ai_suggestion` 与 `human_confirmed`，`ComplianceService` 拒绝保存未人工确认记录；当前没有实际本地 AI 分析引擎，也没有持久化待人工确认建议的完整流程，属于待实现能力。

## 8. 页面与流程

| 页面/入口 | HTML 要求 | 当前状态 |
|---|---|---|
| 合同详情 | 合同摘要、租期、关键日期、扫描件、准入状态 | 合同与合规能力分散，需组合展示 |
| 商户合规详情 | 证照、资质、保险、有效期、预警和阻断原因 | 平台通用合规页已存在 |
| 商户专区“我的证照” | 查看本商户全部证照到期情况，接收预警 | 尚无页面和商户侧只读端点 |
| 宽限期申请与审批 | 一次申请、可配时长、最高权限审批 | 尚无完整状态机 |
| 每日提醒 | 宽限期内每日提醒 | 尚未实现 |

## 9. 验收检查

| 检查 | 通过条件 |
|---|---|
| 提醒配置 | 按证照类型配置，出厂默认提前三个月，同时有上下限 |
| 商户通知 | 预警同时送达商户侧，商户专区可查全部证照状态 |
| 宽限期 | 只允许一次，经最高权限审批；期限按证照类型可配且有上下限 |
| 宽限期内 | 可继续经营但每日提醒 |
| 宽限期后 | 仍未更新则阻断，不得再次延长或另走普通高级审批 |
| 权限不足 | 无放行通道，换有权限人员办理 |
| 合同终止 | 到期终止走审批，不由日期任务直接终止 |
| 合同日期时区 | 到期、续约及其他合同所在地日期按合同对应门店/点位当地时间判断，不使用服务器默认时区；固定 `Asia/Shanghai` 的实现差距已登记 |
| 合同计费输入 | 递增幅度、免租期、交付日和装修截止日均已录入供自动算账；装修截止日由合同约定，商户须在该日前完成，铺位一般按毛坯交付；系统不判断合同约定本身 |
| 合同生效快照 | 目标固化四项；当前快照字段映射及其未完整准确承载的差距均已列明 |
| 纸质合同 | 线下签署后上传扫描件归档 |
| AI 建议 | 只作待人工确认建议，未确认不入台账、不提醒、不阻断、不放行 |
| 实现诚实 | V028/V051 现有字段与 HTML 缺口分别列明，不虚构 `merchant_credentials`，不把代码固定回退值写成业务默认 |

## 10. 依据

- `platform-governance-map-for-operations-v0.1.html` 的准入、合同生命周期、合同运营细则及 James 2026-08-31 点名写入的本地 AI 原句
- BSM「招商租赁与合同履约」
- `business-enum-quick-reference-v0.1.html` 的合作模式、合同状态、合同变更类型和退租结算状态
- 当前实现：V028、V051、`ComplianceService`、`JdbcComplianceRepository`
