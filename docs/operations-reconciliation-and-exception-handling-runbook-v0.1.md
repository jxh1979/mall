# 运营对账与异常处理操作手册（Runbook）V0.1

> 新建文档（`文档goalP1(2026-06-20)`）：运营视角 SOP，面向对账员、风控/合规复核员、财务复核、运营管理员与平台管理员，把分散在多份开发设计/规格中的"对账差异判责、异常复核队列、补偿审批、能力开关操作"收敛为可执行操作流程。本文不定义新数据模型与新接口，只引用既有权威口径：
>
> - 对账四类差异与强制化：`third-party-access-layer-spec-v0.2.md` §2B.6、`third-party-integration-development-design.md` §12、`v099-v124-feature-implementation-and-acceptance-spec-v0.1.md` §8（V105）。
> - 统一异常责任链（risk_alerts）：`v099-v124-feature-implementation-and-acceptance-spec-v0.1.md` §6（V103）、`evidence-risk-layer-spec-v0.2.md` §8。
> - 企微违规命中复核：`v099-v124-feature-implementation-and-acceptance-spec-v0.1.md` §9（V106）、`compliance-and-personal-information-protection-requirements-v0.1.md` §4。
> - 补偿与异常：`third-party-supplier-integration-kit-v0.1.md` §7、`third-party-integration-development-design.md` §11。
> - 能力开关：`integration-capability-switches-v0.1.md`、`v099-v124-feature-implementation-and-acceptance-spec-v0.1.md` §14（V111 终端开关）/§10（V107 MDM 门控）。
>
> 机器识别标记：OPERATIONS_RECONCILIATION_AND_EXCEPTION_RUNBOOK_READY。

---

## 1. 适用范围与角色

| 角色 | 职责 | 关键权限点（引用） |
|---|---|---|
| 对账员 | 处理回流对账批次、识别/确认四类差异 | `finance.reconciliation.difference.create`、`finance.reconciliation.view` |
| 财务复核 | 金额类差异判责、结算口径确认、入账 | `finance.reconciliation.difference.create`（high, requires_approval） |
| 风控/合规复核员 | 异常复核队列、risk_alerts 处置、企微命中复核 | `risk_alert.claim/transfer/escalate/resolve/dismiss`、`wecom.risk_hit.review` |
| 运营管理员 | 数量/事实确认、商户协同、能力开关查看 | `risk_alert.view`、`terminal.switch.view` |
| 平台管理员 | 补偿审批、能力开关开启/灰度/回滚、解锁 | `terminal.switch.manage`、`openplatform.auth.unlock`、各 `requires_approval` 审批 |

> 权限码以 `v099-v124-feature-implementation-and-acceptance-spec` 与各开发设计 seed 为准；本文只引用，不新增权限码。

---

## 2. 对账四类差异（amount / count / status / missing）判责与处置

### 2.1 四类差异定义（强制枚举，不自定义）

四类差异枚举与 `third-party-integration-development-design.md` §12.2 强制枚举、V105 `chk_frd_difference_type`(amount/count/status/missing) 对齐。`missing` 对应 §12.2 双向 `missing_in_platform`/`missing_in_third_party`。

| 差异类型 | 含义 | 单位口径 |
|---|---|---|
| `amount` | 平台侧与第三方侧金额不一致 | 金额一律整数分（D1） |
| `count` | 订单数/核销数不一致 | 按回流层级（订单级/券核销级）分别计 |
| `status` | 订单/核销/退款状态不一致 | 按状态优先级修正 |
| `missing` | 一侧有另一侧无（平台缺/第三方缺） | 主动补拉或人工确认/重发 |

> 前置铁律（R6-D07）：平台侧 expected 由系统取数自动生成，**禁止人工手填**；纯手填（无系统取数支撑）由服务层拒绝（V105）。第三方侧真实接口未接入前以 `source=manual` 登记并**强制进复核**，真实接口接入后由自动匹配替换，manual 记录保留可追溯。

### 2.2 判责矩阵（谁判、采用谁的口径）

| 差异类型 | 主责判定方 | 处置流程 | 关闭依据 |
|---|---|---|---|
| `amount` | 财务复核 | 核对平台流水 vs 第三方流水（金额分），确认采用口径；涉退款冲正联动 AR/AP | 处理结论=采用口径 + 财务签字 |
| `count` | 运营管理员 | 按回流层级核对订单/核销条数，定位漏回流/重复回流 | 事实确认 + 补拉/标重复 |
| `status` | 运营 + 财务协同 | 按状态优先级（终态优先、已支付/已核销优先）修正 | 修正到一致状态 |
| `missing`（平台缺） | 对账员 | 主动补拉第三方数据或人工导入，落平台标准模型 | 补齐后重新比对一致 |
| `missing`（第三方缺） | 对账员 + 商户 | 重发或请商户/第三方确认事实 | 商户确认 or 重发成功 |

> 商户协同：涉及商户业务解释的差异（如商户侧订单解释）由商户在"商户确认视图"确认；平台系统保留差异流转与责任留痕（access-layer §6 后续供应商依赖第3条）。

### 2.3 处置流程（回流→对账批次→四类差异→复核→结算）

数据流与状态流转见 access-layer §2B.6.1。运营操作步骤：

1. **进入对账批次**：回流标准模型按回流层级（订单级/券核销级）+ 渠道（meituan/dianping/douyin）分流归集为对账批次（`reconciliation_batches`，状态 pending→processing）。
2. **系统自动识别差异**：平台侧 expected（系统取数）vs 第三方侧 actual 自动比对，生成四类差异（差异记录 to_identify→open）。
3. **领取并判责**：按 §2.2 矩阵由对应角色领取处置；金额类入财务复核队列。
4. **形成处理结论**：确认平台口径/第三方口径/转财务/忽略（须填原因）。
5. **关闭与结算**：已关闭差异结论进入日结/对账/结算依据（差异记录 open→pending_review→closed/ignored；批次 processing→matched/diff_found→closed）。

### 2.4 升级为统一异常

无法在对账层关闭、或涉及跨域责任（如金额差异疑似舞弊、商户归属冲突）时，按 §3 升级为 `risk_alerts`（source_domain=finance/thirdparty），进统一异常责任链复核。

---

## 3. 异常复核队列（谁看哪个队列）

平台有三类需人工复核的队列，运营须明确各队列归属与入口，避免漏看。

### 3.1 队列总览

| 队列 | 数据来源 | 谁看 | 入口（operationId 引用） |
|---|---|---|---|
| 统一异常责任链 | `risk_alerts`（跨 10 域汇聚，V103） | 风控/合规复核员（认领后流转） | `searchRiskAlerts` GET `/api/v1/evidence/risk-alerts` |
| 企微违规命中复核 | `wecom_risk_hits`（V106） | 合规复核员 | `listWecomRiskHits` GET `/api/v1/compliance/wecom/risk-hits` |
| 接入/对账异常差异 | 接入异常页 + 差异清单（access-layer §8/§9） | 对账员 + 运营 | 见 §2.3 对账批次入口 |

> 孤岛 analytics 域 `analytics_risk_alerts` 为既有桥接对象，企微命中标 `source_domain='wecom'` 衔接；统一层为 `/evidence/risk-alerts/*`（V103 §6 注）。

### 3.2 risk_alerts 责任链复核（5 态 / 6 动作）

状态流转：open →(claim)→ handling →(escalate)→ escalated →(resolve)→ resolved / →(dismiss)→ dismissed。

| 步骤 | 动作 | operationId | 要点 |
|---|---|---|---|
| 1 上报 | report | `reportRiskAlert` | 任一域异常带 `idempotency_key`，重放不重复建 |
| 2 认领 | claim | `claimRiskAlert` | 复核员认领进 handling |
| 3 转交 | transfer | `transferRiskAlert` | risk_level=high 需对应权限 |
| 4 升级 | escalate | `escalateRiskAlert` | 升级到上级处置人 |
| 5 关闭 | resolve | `resolveEvidenceRiskAlert` | **必须携 `resolution_conclusion`**（DB CHECK `chk_risk_alerts_resolved_conclusion` 强制，无结论被拒）；resolve `requires_approval=TRUE` |
| 5' 误报 | dismiss | `dismissRiskAlert` | 误报驳回，`requires_approval=TRUE` |

> 全程 append-only 留痕（`risk_alert_actions`），事件 `exception.*`（reported/claimed/transferred/escalated/resolved/dismissed）。运营铁律：**异常关闭必须有结论**，无结论无法置 resolved。

### 3.3 企微命中复核（合规）

状态流转：open →(领取)→ reviewing →(确认违规)→ confirmed / →(误报)→ dismissed。

| 步骤 | operationId | 要点 |
|---|---|---|
| 查看命中 | `listWecomRiskHits` | 规则码 STAFF_PRIVATE_DEAL/STAFF_PRIVATE_PAYMENT/ABUSE/ILLEGAL_PROMISE/LURE_PRIVATE_WECHAT；`matched_snippet` 默认脱敏 |
| 复核处置 | `reviewWecomRiskHit` POST `…/{id}/review` | `wecom.risk_hit.review`(high, requires_approval)；确认违规→confirmed，误报→dismissed；已处置再 review 报 `WECOM_RISK_HIT_ALREADY_RESOLVED` |

> 合规边界（compliance §4.3）：查看命中**不脱敏原文**仅限最小授权 + 审批的取证/质检场景，且必须写访问审计（`wecom_access_audits`/`audit_events`，可举证"谁、何时、看了哪条会话原文"）。普通展示按权限脱敏。
>
> 误报申诉（compliance §4 待业务确认）：员工对被判定违规的申诉入口、复核改判与改判后命中状态（confirmed→dismissed）处置口径，待业务确认后补；当前以 dismiss 动作 + review_note 记录改判依据。

---

## 4. 补偿审批流

补偿类型与流程引用 `third-party-supplier-integration-kit-v0.1.md` §7 与 `third-party-integration-development-design.md` §11。运营须区分"自动处理"与"需人工审批"两类。

### 4.1 补偿分类与审批要求

| 异常 | 自动处理 | 人工审批要求 |
|---|---|---|
| Webhook 重复 | 幂等忽略重复业务结果 | 无 |
| Webhook 漏发 | 轮询发现差异后补事件 | 高风险需复核 |
| 签名失败 | 拒绝并告警 | IT 排查 |
| 供应商状态未知 | 持续轮询，超阈值告警 | 财务/IT 决策 |
| **支付成功但平台失败** | 补建订单或退款 | **需审批 + 财务复核** |
| **核销成功但库存失败** | 标记业务异常，补偿库存 | 运营/仓库复核 |
| **库存差异入账（V108）** | approved 后生成 ar_ap_items | `finance.inventory_variance.post`(high, requires_approval)；仅 approved 可入账，幂等防重复入账 |
| 人脸核验超时 | **进入人工抽查放行、客人继续进场**——不要求逐人查验证件，但应当对明显疑似未成年的人员进行查验，不得把疑似未成年人当作普通未抽查人员直接放行；落事件 + 异常记录 | 酒吧主管异常流程（compliance §2） |

### 4.2 补偿审批 SOP

1. **触发**：异常补偿任务生成（自动或对账/复核升级），高风险/资金类标 `requires_approval`。
2. **复核**：对应角色（财务/运营/仓库）核对证据与口径。
3. **审批**：平台管理员或具备 `requires_approval` 权限的审批人审批；资金类（支付补偿、退款、库存差异入账）必须财务复核 + 审批双签。
4. **执行与留痕**：补偿执行写事件中心；幂等键防重复执行（如 V108 同 variance 只入账一次 `uq_inventory_variance_posted`）。
5. **关闭**：补偿结果回写差异/异常记录，无法自动补偿的升级为 risk_alerts（§3.2）。

> 铁律：①平台侧 expected 不可手填；②资金类补偿必经审批 + 财务复核；③补偿幂等，重复触发不重复执行；④人脸核验失败/超时一律进入人工抽查放行、客人继续进场，不要求逐人查验证件，但应当对明显疑似未成年的人员进行查验，不得把疑似未成年人当作普通未抽查人员直接放行。

---

## 5. 能力开关操作指引（查看 / 开启 / 灰度 / 回滚 / 权限 / 开启前置检查）

能力开关矩阵以 `integration-capability-switches-v0.1.md` 为权威（默认除 160 本地 AI 与 NAS 备份外全部 off）。终端能力开关运行口径见 `v099-v124-...spec` §14（V111，D5 10 端权威码）。

### 5.1 查看

| 操作 | 入口/口径 |
|---|---|
| 查看集成能力开关矩阵 | `integration-capability-switches-v0.1.md` §3（含默认值、关闭后业务口径、开启后要求、关联证据 PRD-*） |
| 查看终端能力开关 | `listCapabilitySwitches` GET `/api/v1/terminals/capability-switches`（perm `terminal.switch.view`） |
| 关闭态校验 | 开关 off 时对应后台入口须显示"关闭/未启用"，写接口不得伪调用外部供应商（开关矩阵 §5 验收2） |

### 5.2 开启前置检查（开启即生产准入，缺一不可）

开启某能力开关前，必须确认 `integration-capability-switches-v0.1.md` §3"开启后的要求"列对应项齐备：

| 能力 | 开启前置检查清单 |
|---|---|
| `face.verification.enabled` 人脸/年龄核验 | 供应商 SDK/API、成功/失败样例、错误码（`FACE_*`）、合规材料/数据处理协议、测试账号、credential_ref |
| `gate_nfc_scan_device` 闸机/NFC/扫码设备 | 设备协议/API、核销校验、开闸结果、离线队列、设备事件、签名、补偿证据、credential_ref |
| `aggregate_payment` 聚合支付 | 商户号、回调验签、退款权限、对账文件、credential_ref、失败补偿、生产证据 |
| `groupbuy_ticket_channel` 团购/渠道 | 渠道 API/SDK、券码核销、订单回流、退款同步、对账口径、生产证据 |
| `wecom` 企业微信 | 企业微信应用、通讯录、会话存档、回调、同步任务、告警、credential_ref |
| `sms_notification` 短信 | 供应商签名/模板报备、API/SDK、credential_ref、发送回执、限频与失败补偿、生产证据 |
| `managed_work_devices` MDM | Android Enterprise/商业 MDM 选型、enroll、策略下发、远程锁定/擦除测试、设备绑定证据 |
| `object_storage` / `video_platform` | 见开关矩阵 §3 对应"开启后的要求"列 |

> 通用前置：①真实 SDK/API；②credential_ref（生产密钥不入文档正文，仅记保管位置/负责人/到期日/轮换 Runbook）；③Webhook/轮询；④对账/补偿；⑤失败样例；⑥生产证据。缺项不得开启（开关矩阵 §5 验收3）。

### 5.3 灰度（开启）

1. 先在 mock/沙箱环境通过 adapter 契约测试（签名失败、重复回调、漏回调、状态不一致、密钥轮换、对账差异六类，supplier kit §10）。
2. 终端开关按 D5 权威码作用域可按**单终端/单门店**置 on 灰度（V111 `updateCapabilitySwitch`，运行时漂移别名经 CHECK 收敛）。
3. 灰度期密钥轮换先灰度验证再切生产（supplier kit §8）。
4. 灰度观察：回流成功率、对账差异率、失败补偿队列、告警。

### 5.4 回滚

| 场景 | 回滚动作 |
|---|---|
| 开启后供应商不稳定/对账差异激增 | 将开关置 off，对应业务降级为人工/站内流程（开关矩阵"关闭后业务口径"列），不得一边关一边宣称自动能力在线 |
| 终端能力异常 | `updateCapabilitySwitch` 置 off，该终端发起该能力动作被拒并留痕（GuardedAction 拒绝路径，V111） |
| MDM 违规设备 | `handleMdmViolation` disposition=device_blocked → 设备 blocked + management_status=blocked + 撤销凭证（V107） |

> 回滚铁律：开关 off 后业务能力必须真实降级（隐藏/人工替代），off 态写接口不得伪调用供应商。

### 5.5 权限

| 操作 | 权限点 |
|---|---|
| 查看终端能力开关 | `terminal.switch.view` |
| 变更终端能力开关（开启/灰度/回滚） | `terminal.switch.manage` |
| MDM 违规处置 | `mdm.violation.handle`(high, requires_approval) |
| 开放应用人工解锁 | `openplatform.auth.unlock` |

---

## 6. 验收标准（运营 SOP 可执行性）

1. 对账四类差异（amount/count/status/missing）各有明确判责方与处置流程；平台侧 expected 无人工手填，第三方侧未接入期 `source=manual` 强制进复核。
2. 三类复核队列（risk_alerts / 企微命中 / 接入对账异常）归属清晰，复核员知道看哪个队列、用哪个入口。
3. risk_alerts 关闭必带结论（DB CHECK 强制）；企微命中查看不脱敏原文需最小授权+审批并写访问审计。
4. 资金类与高风险补偿必经审批+财务复核，补偿幂等不重复执行；人脸核验失败一律进入人工抽查放行、客人继续进场，不要求逐人查验证件，但应当对明显疑似未成年的人员进行查验，不得把疑似未成年人当作普通未抽查人员直接放行。
5. 能力开关有查看/开启前置检查/灰度/回滚/权限完整指引；开启前置项缺一不得开启，off 态业务真实降级。

---

## 7. 待业务确认项

| 项 | 说明 | 来源 |
|---|---|---|
| 企微规则误报申诉流程 | 员工对违规判定的申诉入口、复核改判与改判后命中状态处置 | compliance §4 待业务确认 |
| 企微授权配置责任人落地 | R6 已确认 superadmin 一次性配置 + 指定岗位复核，落地执行待确认岗位 | compliance §4 |
| 各能力供应商资料 | 人脸/年龄核验、闸机/设备、短信、聚合支付等真实供应商 SDK/API、签名、错误码、对账文件、合规材料 | supplier kit §9、access-layer §8.3、开关矩阵 §3 |

## 8. 变更日志

| 日期 | 类型 | 内容 | 原因/依据 |
|---|---|---|---|
| `文档goalP1(2026-06-20)` | 新建 | 新建运营对账与异常处理 Runbook：①对账四类差异判责与处置；②三类异常复核队列（risk_alerts/企微命中/接入对账异常）归属与流程；③补偿审批流；④能力开关查看/开启/灰度/回滚/权限/开启前置检查操作指引 | 对账/异常/补偿/开关操作口径分散在多份开发设计，缺运营视角可执行 SOP。引用 integration-capability-switches、v099-v124-feature 规格 V103/V105/V106/V107/V111 等权威口径，不新增数据模型/接口 |
