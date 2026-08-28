# 第一轮 第三方能力开关与 Mock 策略 R1（2026-06-18）

> 定位：把"能力开关 + mock/stub + 失败场景 + 字段映射占位 + 后续供应商映射口径"整合为单一执行视图，并叠加本轮审计结论。供应商未确定时统一保留**接口边界、能力开关、mock/stub、失败场景、字段映射占位**五件套。
> 整合源：`integration-capability-switches-v0.1.md`、`partial-requirement-mock-coverage-v0.1.md`、`external-dependency-gates-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`、`t3d-local-llm-ai-quality-v0.1.md`。
> 机器标记：R1_CAPABILITY_SWITCH_MOCK_STRATEGY_READY、DEFAULT_EXTERNAL_OFF_EXCEPT_LOCAL_AI_AND_NAS。

## 1. 统一原则（沿用并强化既有口径）

1. 默认除 **160 本地 AI（on）** 与 **NAS 备份（on, 必需）** 外，外部集成**全部关闭**。
2. 关闭 = 不读外部接口、不要供应商密钥、不要生产证据；但**对应自动能力必须降级/隐藏/转人工**，不得"一边关接口一边宣称自动能力上线"。
3. **mock ≠ 生产证据**：8 条 partial 需求可用 mock 联调/业务验收，但生产闭环仍按供应商资料与生产阻断台账回填。
4. 供应商资料到位时：先建供应商专项计划 + 字段映射 → 再新增 OpenAPI operation/权限/事件/验签/失败回退/测试/Semgrep → 最后才改开关与准入门状态。

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-17，本文不再重复记载。
## 3. 本轮审计结论（mock 充分性）

- **合格**：8 条 partial 需求（REQ-17/21/23b/25/32/40/41/44）均有 mock/adapter/页面/后端测试/E2E 替身，**足以支撑联调与业务验收**（`partial-requirement-mock-coverage-v0.1.md` 已逐条列证据文件）。
- **R1 新缺口 1（P0，登录链路）**：`sms_notification` 开关**未并入** `integration-capability-switches-v0.1.md` 与 `-latest.json`，而登录链路已硬依赖 `sms_code`。占位文档已写好，需执行落地动作（占位文档 §5）。在并入前，能力开关矩阵对"短信关闭后登录如何降级"无机器可验证口径。
- **R1 新缺口 2（P1）**：兑换/卡座入场 verify 接口未进 OpenAPI（审计报告 R1-LOOP-08），导致 REQ-23 类 mock 验收缺契约锚点。
- **160 本地 AI 闭环**：自洽、可验收，无新增风险。

## 4. 验收标准（机器可验证）
- `verify:external-dependency-gates` → `EXTERNAL_DEPENDENCY_GATES_PASS`
- `verify:t3-coding-package` / `verify:t3b-supplier-mock-kit` / `verify:t3d-local-llm-ai-quality` 全 PASS
- `verify:partial-requirement-mock-coverage` PASS
- OpenAPI 不得出现真实机器人/视频/设备控制路径或独立 Python AI 服务路径
- 能力开关关闭项：页面显示"未启用"，写接口不得伪调用外部供应商（switches §5 验收 1-6）
- **R1 新增建议**：把 §2 SMS 行并入主矩阵后，新增验收"短信关闭 → 登录走 mock OTP、催缴/告警降级 manual 且留痕"

## 5. 后续供应商映射口径（接续《待供应商资料补齐清单》）
所有 `<vendor.*>` 占位字段、`PAY_*/SMS_*/FACE_*/VIDEO_*/STORAGE_*/MDM_*/ROBOT_*/DEVICE_*` 占位错误码，在供应商 SDK/API/签名/回调/错误码到位后用真实值替换；替换前不得写死供应商私有字段进票务/酒吧/财务等业务主模型（统一基线 D3）。
