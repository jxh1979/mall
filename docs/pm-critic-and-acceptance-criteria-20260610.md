# PM Critic 对抗式产品审查与验收标准补充 V0.6

生成日期：2026-06-10  
使用方法：按 `pm-utility-pm-critic` 对当前产品方案做 PM 红队审查，并按 `pm-deliver-acceptance-criteria` 为关键功能切片补充 Given/When/Then 验收标准。  
审查对象：综合体管理系统 P0/P1/P2 产品文档包，重点参考 `business-decision-log-v0.1.md`、`unified-standards-baseline-v0.1.md`、`finance-settlement-product-prd-v0.1.md`、`platform-governance-prd-v0.1.md`、`p0-implementation-task-list.md`、`final-open-decisions-and-owner-checklist-20260610.md`。

> V0.2 修订说明（2026-06-10）：按本报告 P0-1/P0-2 的红队发现，已新增 `finance-ar-ap-ledger-development-design-v0.1.md` 和 `business-entity-binding-development-design-v0.1.md`，并在 `p0-implementation-task-list.md` 中补 DB-016/DB-017、GOV-001/002、FIN-001~004。P0-1/P0-2 从“缺工程落点”降为“实施时必须按新增文档验收”。
>
> V0.3 修订说明（2026-06-10）：总控已确认 PM Critic Findings 的 P0/P1/P2/P3 均按修复建议执行并收口。P0-3 以 P0 核心闭环/前置能力/P1 增强分层验收控制；P1-1~P1-3、P2-1~P2-2、P3-1~P3-2 均按下表“处理结果”执行，不再作为开放产品问题。
>
> V0.4 修订说明（2026-06-10）：按本轮全量文档对抗式复核，补充合并后入口审计、旧“未决”标题收口、Product Design 入口可读性检查和新增 Given/When/Then 验收标准。
>
> V0.5 修订说明（2026-06-10）：按 P0 执行矩阵补齐，新增权限模板、操作留痕、端到端验收、供应商适配占位和酒吧人脸/年龄核验生产阻断的 Findings Closure 与 Given/When/Then 补充。
>
> V0.6 修订说明（2026-06-10）：按本轮产品文档审计官要求，复核综合体/商户/酒吧/游乐/票务/会员/各端边界、通票/赠酒/存酒/卡座/低消/支付/核销/留痕闭环、权限颗粒度、第三方双向接口和可开发/上线阻断边界；修复事件名漂移、第三方敏感原始报文策略待定和一体机售票追溯锚点。

## 1. PM Critic Findings 与处理结果

### P0 Findings

| 编号 | 问题 | 修复建议 | 处理结果 |
|---|---|---|---|
| P0-1 | P0 完整应收应付台账已拍板，但字段、状态和验收标准曾分散 | 将本文件第 3.1 的验收标准作为 P0 财务经营台账的验收门槛；补 `ar_ap_items` 或等价表设计 | 已执行：新增 `finance-ar-ap-ledger-development-design-v0.1.md`，P0 清单补 DB-017、FIN-001~004 |
| P0-2 | 四类主体唯一绑定已拍板，但合同、结算、权限、看板、第三方接入曾缺统一工程落点 | 采用本文件第 3.2 验收标准；补 `business_entity_bindings` 或等价表 | 已执行：新增 `business-entity-binding-development-design-v0.1.md`，P0 清单补 DB-016、GOV-001/002 |
| P0-3 | P0 范围持续变大，存在交付切片过宽风险 | 用“P0 核心闭环 + P0 前置能力 + P1增强”三层验收；每个 P0 能力必须能单独验收 | 已执行：P0 清单按里程碑、任务编号、分派顺序和端到端完成判定分层，不再扩大新业务范围 |

### P1 Findings

| 编号 | 问题 | 修复建议 | 处理结果 |
|---|---|---|---|
| P1-1 | 商户账单门户、催缴、费用异议与 P0 应收应付台账边界 | P0 只给内部财务/管理层台账；P1 才开放商户侧账单门户、催缴和异议 | 已执行：`finance-ar-ap-ledger-development-design-v0.1.md` 明确 P0 非范围；P1 边界保留 |
| P1-2 | 视频证据 P0 索引/跳转，P1 平台内播放依赖供应商 API | P0 验收只检查事件绑定、权限、审计和跳转；平台内播放必须等视频平台 API | 已执行：最终清单和第三方/证据风控文档按 P0 索引、P1/P2 播放能力收口 |
| P1-3 | 游乐安全巡检失败严重等级配置 | P0 先配置安全巡检项等级；严重项失败自动暂停，普通项预警主管 | 已执行：游乐开发设计、验收标准和 PM 验收均按严重/阻断项自动暂停收口 |

### P2 Findings

| 编号 | 问题 | 修复建议 | 处理结果 |
|---|---|---|---|
| P2-1 | P0 本地 AI 合同分析可能被误扩展成自动法务判断 | 明确 AI 只给建议，不自动阻断/放行；所有阻断、放行、台账入库必须人工确认 | 已执行：合同日期方案 B、PM 报告和最终口径均要求本地 AI 辅助 + 人工确认 |
| P2-2 | 集团视图、预约排队、团体/包场、真实闸机/NFC 不应倒逼回 P0 | 在路线图中保持“接口预留、真实联动后置”的口径 | 已执行：最终清单和 P0 任务清单保持 P0 扫码/接口预留，真实联动 P1/P2 |

### P3 Findings

| 编号 | 问题 | 修复建议 | 处理结果 |
|---|---|---|---|
| P3-1 | 历史审计报告数量多，外部读者容易读到旧结论 | 交付清单指定当前权威入口；历史审计仅供追溯 | 已执行：`delivery-package-manifest-v0.1.md` 和索引已指定权威入口 |
| P3-2 | 多处“建议”是历史措辞，不一定代表仍需总控处理 | 对权威文档继续保留执行型措辞，历史文档不必全部重写 | 已执行：当前权威文档按已确认执行口径收口，历史文档保留证据属性 |

## 2. Acceptance Criteria Scope

本轮验收标准覆盖以下功能切片：

1. P0 完整应收应付台账。
2. 经营对象、运营主体、合同主体、结算主体唯一绑定。
3. P0 移动管理端高风险待办。
4. P0 视频证据索引与敏感查看审计。
5. P0 游乐安全巡检失败处理。
6. 第三方回流失败人工导入兜底。
7. P0 受管 Android 设备准入。
8. 160 运维脚本、备份和告警。

## 3. Given/When/Then 验收标准

### 3.1 P0 完整应收应付台账

Happy Path:

1. Given 财务人员拥有财务经营权限，When 创建一条应收台账，Then 系统必须记录金额、到期日、责任主体、经营对象、结算主体、来源对象、收款状态、账龄和创建人。
2. Given 合同固定费用已配置，When 到达生成周期，Then 系统应生成对应应收台账，并关联合同、空间、经营对象和结算主体。
3. Given 票务、酒吧或第三方核销产生结算依据，When 财务生成结算台账，Then 系统应能按订单、核销或合同固定规则分别形成应收或应付记录。
4. Given 应收款项已到账，When 财务登记收款，Then 台账状态应从待收款变为已收款或部分收款，并记录收款时间、收款人、渠道和凭证附件。

Edge Cases:

1. Given 同一来源对象已生成台账，When 用户再次生成，Then 系统必须提示重复并阻止重复生成，或要求以调整单方式处理。
2. Given 一条应收已部分收款，When 用户查看账龄，Then 系统必须按未收余额计算账龄。
3. Given 结算主体和运营主体不同，When 台账生成，Then 台账必须引用结算主体作为收付款主体，同时保留运营主体用于经营责任追溯。

Error States:

1. Given 缺少责任主体或结算主体，When 用户保存台账，Then 系统必须拒绝保存并提示缺失字段。
2. Given 日结已锁定，When 用户尝试直接修改影响金额的台账，Then 系统必须拒绝直接修改，并引导发起调整/冲正审批。
3. Given 附件上传失败，When 用户保存带附件的台账，Then 系统必须提示失败并允许重试，不得生成附件缺失但状态为已完成的记录。

Non-functional:

1. Given 用户查看台账列表，When 按责任主体、结算主体、账龄、状态筛选，Then 查询结果应在可接受时间内返回，并能导出 CSV/Excel。
2. Given 任意台账金额或状态变化，When 审计人员查看留痕，Then 必须看到变更前后值、操作者、时间、原因和关联附件。

### 3.2 四类主体唯一绑定

Happy Path:

1. Given 招商人员创建经营对象，When 录入运营主体、合同主体和结算主体，Then 系统必须生成唯一主体绑定关系编号。
2. Given 合同创建时选择经营对象，When 系统带出主体绑定关系，Then 合同、结算、权限、看板和第三方接入必须引用同一绑定关系。
3. Given 结算主体与合同主体不同，When 财务生成应收应付台账，Then 系统必须使用结算主体作为收付款主体，并保留合同主体作为合同责任方。

Edge Cases:

1. Given 同一经营对象已有生效绑定关系，When 用户创建第二个生效绑定关系，Then 系统必须阻止或要求先失效旧绑定关系。
2. Given 主体绑定关系发生变更，When 新合同或新结算依据生成，Then 新记录使用新版本，历史记录仍保留旧版本。

Error States:

1. Given 合同引用的主体绑定关系已失效，When 用户尝试提交合同生效，Then 系统必须阻断并提示重新确认主体绑定关系。
2. Given 第三方接入绑定的主体关系与合同不一致，When 接入应用申请上线，Then 系统必须阻断或进入高级审批。

Non-functional:

1. Given 任一主体字段变更，When 审计人员查看记录，Then 系统必须展示变更人、变更时间、变更原因、旧值和新值。

### 3.3 移动管理端高风险待办

Happy Path:

1. Given 主管登录移动管理端，When 存在高风险待办，Then 系统只展示与该主管权限范围匹配的待办。
2. Given 高风险退款、免单、调账或高价值报损待复核，When 主管打开待办，Then 页面必须展示业务对象、金额、责任人、证据、风险原因和可执行动作。
3. Given 主管完成复核，When 提交通过或驳回，Then 系统必须记录复核结果、原因、时间和操作设备。

Error States:

1. Given 主管无权限处理某待办，When 尝试打开处理动作，Then 系统必须拒绝并提示权限不足。
2. Given 网络异常导致提交失败，When 主管提交复核，Then 系统必须提示失败并保留待办未处理状态。

Non-functional:

1. Given 移动管理端访问敏感待办，When 用户进入详情，Then 系统必须记录访问审计。

### 3.4 视频证据索引与敏感查看审计

Happy Path:

1. Given 风险事件绑定了视频点位和时间段，When 用户查看事件详情，Then 系统应显示视频证据索引和受控跳转/短期取证链接。
2. Given 用户具备敏感证据查看权限，When 打开视频证据链接，Then 系统必须记录查看人、时间、事件、点位和理由。

Error States:

1. Given 视频平台链接过期，When 用户点击证据链接，Then 系统必须提示重新生成或联系有权限人员。
2. Given 用户无敏感证据查看权限，When 点击视频证据，Then 系统必须拒绝并记录访问尝试。

Non-functional:

1. Given 视频证据属于合规留存对象，When 留存期到期，Then 系统必须按配置归档、删除或匿名化，并保留处置记录。

### 3.5 游乐安全巡检失败处理

Happy Path:

1. Given 员工提交游乐安全巡检，When 严重/阻断项失败，Then 系统必须自动暂停对应项目或设备。
2. Given 普通巡检项失败，When 员工提交巡检，Then 系统应向主管发送预警并记录整改待办。
3. Given 项目处于暂停或检修中，When 主管未确认恢复，Then 系统不得允许恢复营业或继续核销。

Error States:

1. Given 巡检记录缺少必填项，When 用户提交，Then 系统必须拒绝并提示缺失项。
2. Given 主管确认恢复但仍存在阻断项未关闭，When 提交恢复，Then 系统必须拒绝。

### 3.6 第三方回流失败人工导入兜底

Happy Path:

1. Given 第三方回流失败，When 平台运营上传人工导入文件，Then 系统必须标记来源为 `manual_import`。
2. Given 导入文件包含订单或核销记录，When 系统处理文件，Then 必须执行重复识别、字段校验、差异复核和导入留痕。

Error States:

1. Given 文件模板不匹配，When 用户导入，Then 系统必须拒绝并提示模板错误。
2. Given 导入记录与既有记录冲突，When 系统识别差异，Then 不得自动覆盖，必须进入差异复核。

### 3.7 Android 受管设备准入

Happy Path:

1. Given 工作手机或平板已通过 Android Enterprise / Android Management API + 商业 MDM 注册，When 设备登录业务系统，Then 系统允许其按权限访问业务功能。
2. Given 设备未配置商业 MDM，When 尝试正式生产登录，Then 系统必须禁止正式发放或标记为试点设备。
3. Given 策略命中违规访问或私装应用，When MDM 或设备策略上报，Then 系统必须写入设备策略违规留痕并可触发封禁/复核。

Error States:

1. Given 设备时间偏差超限，When 执行票务或权益核验，Then 系统必须拒绝核验并提示校准时间。
2. Given 设备 Root/调试/异常环境命中，When 员工尝试操作，Then 系统必须失效业务能力并进入复核。

### 3.8 160 运维脚本、备份和告警

Happy Path:

1. Given 运维人员执行发布脚本，When dry-run 通过且备份完成，Then 才允许执行正式发布。
2. Given PostgreSQL/MinIO/Docker 数据到达备份计划时间，When 备份任务运行，Then 备份应同步到 NAS 并生成校验记录。
3. Given 关键服务健康检查失败，When 告警触发，Then 系统必须发送企业微信机器人告警，并用邮件兜底。

Error States:

1. Given 备份失败，When 发布脚本准备执行，Then 系统必须阻断发布并提示先修复备份。
2. Given 回滚脚本执行失败，When 服务无法恢复，Then Runbook 必须指向人工恢复步骤和责任人。

## 4. Status Summary

本轮 PM critic 发现的最高风险集中在 P0 应收应付台账和四类主体唯一绑定。V0.2 已把这两个风险从“方向正确但工程落点不足”补强为两份独立开发设计，并同步到 P0 任务清单。移动管理端、视频证据、游乐安全、第三方回流兜底、Android 受管设备和 160 运维也已补充可测试验收标准，便于工程和 QA 按场景验收。

## 5. Status

```yaml
status: reviewed_and_closed_by_default_recommendations
highest_severity: none_open
primary_risks:
  - none_open_as_product_decision
resolved_p0_followups:
  - p0_ar_ap_scope_development_design_added
  - entity_binding_development_design_added
acceptance_criteria_added:
  - p0_ar_ap_ledger
  - entity_binding
  - mobile_admin_high_risk_todos
  - video_evidence_index
  - amusement_safety_inspection
  - third_party_manual_import
  - android_managed_device
  - server_160_ops
recommended_next_docs:
  - supplier_api_mapping_after_docs_arrive_as_implementation_input
  - legal_retention_and_employee_notice_templates_as_default_configuration
```

## 6. V0.2 二次 PM Critic 处理结果

### 已处理

| 原发现 | 处理方式 | 当前状态 |
|---|---|---|
| P0-1 P0 完整应收应付台账缺统一工程落点 | 新增 `finance-ar-ap-ledger-development-design-v0.1.md`，补核心对象、表结构、状态机、流程、权限、反模式、测试和错误处理；`p0-implementation-task-list.md` 补 DB-017、FIN-001~004 | 已降级为实施验收项 |
| P0-2 四类主体唯一绑定缺统一工程落点 | 新增 `business-entity-binding-development-design-v0.1.md`，补主体档案、绑定关系、引用追踪、准入校验、API、权限和测试；`p0-implementation-task-list.md` 补 DB-016、GOV-001/002 | 已降级为实施验收项 |
| P0-3 P0 范围偏宽 | P0 任务清单中继续按核心闭环、前置能力、P1/P2 增强分层；新增任务只补已拍板 P0 的工程落点，不扩大新业务范围 | 已按分层验收执行，后续作为项目管理执行纪律 |

### 新增 P1 Findings

| 编号 | 问题 | 风险 | 修复建议 |
|---|---|---|---|
| P1-4 | 新增应收应付和主体绑定开发设计已足够指导编码，但正式 Flyway 迁移仍需在编码阶段按 Java/PostgreSQL 规范拆分 | 如果直接照文档一次性建大表，迁移回滚和索引优化可能粗糙 | 编码时把主体、应收应付、附件、审计、调整分成独立 Flyway 迁移，并补唯一约束和索引 |
| P1-5 | 法务/HR 留存期限和员工监控告知模板属于实施配置输入 | 若未配置就上线，合规风险高于产品风险 | 已按总控口径收口：除国家法令强制实名外，其它实名默认关闭；留存期限、员工告知、发票税务按推荐默认作为实施配置执行 |

### Given/When/Then 补充

1. Given 同一经营对象已有生效主体绑定，When 平台运营提交第二个生效绑定，Then 系统必须阻断或要求先终止旧绑定，并记录审计。
2. Given 合同固定费用到达生成周期，When 系统生成应收台账，Then 台账必须引用合同、空间、主体绑定关系和结算主体。
3. Given 台账已纳入锁定日结，When 财务尝试直接修改金额，Then 系统必须拒绝并引导创建调整审批。
4. Given 第三方应用上线申请引用的主体绑定与合同主体不一致，When 提交上线，Then 系统必须阻断或进入高级审批。
5. Given 应收应付导出包含敏感主体和金额，When 用户导出，Then 系统必须二次确认并记录导出人、时间、筛选条件和文件摘要。

## 7. V0.4 全量文档对抗式复核

**Artifact reviewed:** 当前 `docs/` 活跃文档包  
**Standards consulted:** `pm-utility-pm-critic`、`deliver-acceptance-criteria`、`stream-coding` Spec Gate、documentation-specialist audit workflow、Product Design 产品流审计视角  
**Findings:** 4 (P0: 0, P1: 1, P2: 2, P3: 1)

### P1 findings

#### F-01: 历史“未决”标题会误导实施方

**Location:** 票务开发设计 §14、若干变更日志  
**Issue:** 个别当前活跃文档仍使用“未决问题”作为章节或历史变更位置，虽然正文多数已经写成已确认。  
**Why it matters:** 外部工程或 QA 可能误以为仍需总控拍板，延误 P0 编码。  
**Fix:** 已将票务 §14 改为“已确认编码口径”，并把相关变更日志改成“已确认执行口径/后续排期”。

### P2 findings

#### F-02: 合并后入口正确，但部分历史计数段落仍易造成阅读噪音

**Location:** `delivery-package-manifest-v0.1.md` 历史修订说明  
**Issue:** 早期修订说明保留多个旧文件数，容易与当前 107 文件/62 活跃入口口径混淆。  
**Fix:** 已新增 V0.4 修订说明，明确当前总量、主目录活跃入口和合并规格入口。历史段落只作为时间线，不作为当前计数。

#### F-03: Product Design 视角下，非技术读者入口需要继续强化“读哪份合并规格”

**Location:** 交付包引导、能力层索引  
**Issue:** 文档合并后，旧“五件套”读法不再是最佳入口。  
**Fix:** 已将交付包引导改为“各层 PRD、合并规格、字段章节和验收章节”，并在能力层索引列出合并规格。

### P3 findings

#### F-04: 业务状态“待确认”与开放决策“待确认”同词

**Location:** 合同、日结、结算、对账等状态枚举  
**Issue:** “待确认”作为业务状态是正确的，但关键词扫描会产生噪音。  
**Fix:** 保留业务状态，不做无意义替换；后续审计应区分“业务状态枚举”和“产品开放问题”。

### V0.4 Given/When/Then 补充

1. Given 外部实施方从交付包开始阅读，When 打开 `delivery-package-manifest-v0.1.md`，Then 应能看到当前总量、活跃入口数量和合并规格入口，不需要读取归档源文件即可理解当前口径。
2. Given QA 检查票务开发设计，When 进入 §14，Then 看到的是已确认编码口径，而不是仍需主线程拍板的问题列表。
3. Given 产品/设计人员从能力层索引进入平台治理或业务经营，When 选择对应层，Then 应优先进入合并规格文档，并能在其中找到页面、字段、流程、低保真和验收内容。
4. Given 审计脚本扫描“待确认”，When 命中合同状态、日结状态或结算状态，Then 应归类为业务状态枚举，不作为开放决策缺陷。
5. Given 后续新增 P1/P2 功能，When 需要补验收标准，Then 必须采用 Given/When/Then，并覆盖 happy path、edge case、error state 和至少一个审计/权限/留痕类非功能条件。

## 8. V0.4 Status

```yaml
artifact:
  path: docs/
  type: documentation-package
findings:
  total: 4
  p0: 0
  p1: 1
  p2: 2
  p3: 1
refusal:
  triggered: false
recommended_action: accept_current_docs_as_closed_for_product_decision_and_use_v0_4_checks_for_next_changes
dispatch:
  mode: inline-execution
  client: codex
```

## 9. V0.5 执行矩阵补齐

> 修订说明（2026-06-10）：本轮按产品审计官口径继续修复文档，不新增开放拍板问题。已新增并接入索引/任务清单的执行型文档：
> - `p0-operation-permission-matrix-v0.1.md`
> - `p0-action-event-evidence-matrix-v0.1.md`
> - `p0-end-to-end-acceptance-and-edge-cases-v0.1.md`
> - `third-party-supplier-integration-kit-v0.1.md`

### V0.5 Findings Closure

| 编号 | 问题 | 修复结果 |
|---|---|---|
| V5-01 | 权限虽有最小颗粒度原则，但模板化批量授权、临时模板、单次有效和显式拒绝未形成执行矩阵 | 已补 P0 操作权限矩阵，并同步平台治理 PRD、合并规格、数据库设计、Java 后端设计、P0 任务清单 |
| V5-02 | 留痕要求分散，测试很难逐项判断“哪些动作必须留什么证据” | 已补 P0 操作事件与证据矩阵，并纳入 P0 任务 QA-008 |
| V5-03 | 端到端验收散在各业务 PRD，缺少跨票务、酒吧、库存、财务、第三方的统一验收入口 | 已补 P0 端到端验收标准与边界用例，并同步交付包、索引、P0 完成判定 |
| V5-04 | 第三方供应商未确定时，容易在文档里写死厂商字段或阻塞 P0 | 已补供应商接入预留包，明确统一接口、适配器、字段映射占位、Webhook+轮询、对账和补偿策略 |
| V5-05 | 酒吧年龄“人脸识别”存在采购/第三方接口前置，如果只写人工+人脸会误导生产上线 | 已补生产阻断口径：供应商未配置时只能 mock，生产不得标记正式通过 |
| V5-06 | 币类口径分散，第三方返回非 CNY 时可能被隐式换算 | 已在统一口径 D12、P0 任务清单和后端/数据库设计中明确 P0 CNY、整数分、非 CNY 异常复核 |

### V0.5 Given/When/Then 补充

1. Given 管理员已有“普通员工”基础模板，When 复制为“普通员工01”并修改权限，Then 新模板保留复制时快照并独立维护。
2. Given 临时模板设置了有效期或单次有效，When 到期或首次消耗后再次操作，Then 系统必须拒绝并记录到期/消耗事件。
3. Given 同一权限同时存在授予和显式拒绝，When 员工执行动作，Then 系统按拒绝处理并展示冲突来源。
4. Given 第三方供应商 SDK/API 未到位，When 开发 P0 接口，Then 只能实现统一适配器、mock、映射占位和契约测试，不得写死供应商私有字段。
5. Given 酒吧生产环境未配置人脸/年龄核验供应商或等效设备，When 员工尝试标记年龄核验通过，Then 系统必须阻断；测试环境只能通过 mock 标记。
6. Given 支付或渠道返回非 CNY 金额，When 进入财务入账，Then P0 必须拒绝或进入异常复核，不得自动换算。

## 10. V0.5 Status

```yaml
artifact:
  path: docs/
  type: documentation-package
findings:
  total: 6
  p0: 6
  open_product_decisions: 0
status: closed_by_document_updates
recommended_action: use_new_p0_matrices_and_supplier_kit_as_execution_gate
```

## 11. V0.6 全量执行性审计

**审计对象：** 当前 `docs/` 活跃文档包。  
**审计目标：** 判断文档是否足够让产品、开发、测试、运营和第三方供应商理解并执行，而不是仅做文字润色。  
**结论：** 无新的产品拍板项；存在 3 个可直接修复的执行一致性问题，已修复。

### V0.6 Findings Closure

| 编号 | 问题 | 风险 | 修复结果 |
|---|---|---|---|
| V6-01 | 部分执行文档残留旧事件名，如 `ticket.verification.*`、`verification.success`、`bar.saved_bottle.picked_up`、`amusement.verification.success` | 开发埋点、测试查询、数据分析和事件中心会按不同名字写入，导致报表空跑或审计漏查 | 已对齐为权威事件：`verification.succeeded/failed/manual_overridden`、`bar.saved_bottle_picked_up`、`amusement.verification_succeeded/failed/overridden` |
| V6-02 | 后端文档中“原始第三方 payload 脱敏或加密策略待定”仍是开放措辞 | 开发无法判断敏感报文落库策略，生产环境有个人信息和密钥泄露风险 | 已改为确定口径：默认只保存摘要、签名校验、状态码和必要业务键；必要原文字段级脱敏后加密保存，密钥引用走台账、到期提醒和 Runbook |
| V6-03 | 需求追溯矩阵的一体机条目只写通用自助服务，未把售票型一体机售票职责作为独立锚点 | 外部实施方可能仍误以为一体机不售票或把售票做成第 11 种终端 | 已更新 REQ-49：一体机仍为 `kiosk`，通过 `kiosk_mode=ticketing/service` 区分，售票型承担授权服务人员辅助购票/取票/查单并复用票务闭环 |

### V0.6 Given/When/Then 补充

1. Given 测试人员按事件中心查询票务核销，When 使用标准事件名检索，Then 成功核销必须落在 `verification.succeeded`，失败必须落在 `verification.failed`，不得只写 `ticket.verification.*` 旧名。
2. Given 酒吧存酒领取完成，When 事件中心记录取酒动作，Then 必须写入 `bar.saved_bottle_picked_up`，并能关联存酒编号、取用量、剩余量和员工。
3. Given 第三方回调报文含手机号、证件、人脸、Token 或 Secret，When 写入事件或原始报文表，Then 事件 payload 只能保存脱敏摘要和哈希；必要原文必须字段级脱敏后加密保存，并记录密钥引用。
4. Given 售票型一体机已启用，When 授权服务人员辅助到店客人购票，Then 系统必须复用官方票务价格、库存锁定、聚合支付、出票和防伪二维码能力，并按设备点位过滤可售票种，不得新增脱离票务主模型的售票链路。
5. Given 供应商尚未提供 SDK/API 文档，When 开发 P0 第三方能力，Then 只能完成统一接口、mock adapter、字段映射占位、Webhook+轮询校验和补偿框架，不得把供应商私有字段写入业务主模型。

### V0.6 Status

```yaml
artifact:
  path: docs/
  type: documentation-package
findings:
  total: 3
  p0: 3
open_product_decisions: 0
status: closed_by_document_updates
remaining_inputs:
  type: implementation_or_production_readiness
  examples:
    - supplier_sdk_api_documents
    - payment_merchant_credentials
    - face_age_verification_provider
    - android_enterprise_mdm_configuration
    - nas_backup_restore_drill
recommended_action: proceed_to_p0_development_with_supplier_mock_and_readiness_gates
```
