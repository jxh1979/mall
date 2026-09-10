CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U14 未发布材料第 14 段（完整接口定义）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T03:40:49Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `14dba62327c7475cb8dbd333c9f80fad0291d8d6`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 14 段，1 文件 51250 行（openapi-p0.json 51250），main 主归属 M391；适用判据＝映射行并集 18 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。OVERSIZE-SINGLE-FILE：1696081 字节 JSON 不逐行通读；覆盖方法＝相对基线 diff 全读（+764/−33）+ 在 `/tmp/u-overlay` 运行仓库门禁 `node tools/verify-openapi-contract.mjs`（json/yaml 深比较、占位符检查，/tmp/u14_contract_check.log）+ python 载入 JSON 统计 operation 数与新增路径。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U14-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U13 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U14-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：questions/U04-001.md（setMerchantSelfOperated 已答删除）、U01-001/U02-001（终端类型 app 自报）、U05-011（写口幂等键）、U05 定稿（失败审计事件）。只读依赖追流（不审）：副本 StaffController/PlatformController（第 5/8 段）、闭合附录与矩阵（第 16/17 段）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/openapi/openapi-p0.json | 51250 | 1696081 | f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384 | 全读 |

## 2. 可执行证据原文（/tmp/u14_b1_run.log）

```
START=2026-09-09T21:12:33Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
docs/openapi/openapi-p0.json | 51250 | 1696081 | f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384
=== manifest entries ===
docs/openapi/openapi-p0.json 51250 1696081 f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384
=== baseline -> unpublished tree (numstat) ===
764	33	docs/openapi/openapi-p0.json
=== main HEAD vs baseline (blob) ===
docs/openapi/openapi-p0.json baseline=bfa444d6f1 head=bfa444d6f1 unpublished=0cd43e7305 main_changed_since_baseline=no
=== segment probes (/tmp/u14_probes.sh) ===
-- operation count + info.version (json, material vs main)
material ops 730 info.version V0.14
write_ops_failure_none 8
   ('/api/v1/staff/shifts', 'post', 'upsertStaffShift', True, ['web', 'staff_private_phone'])
   ('/api/v1/staff/shifts/batch', 'post', 'batchUpsertStaffShifts', True, ['web', 'staff_private_phone'])
   ('/api/v1/staff/attendance/punch', 'post', 'punchStaffAttendance', True, ['staff_work_phone'])
   ('/api/v1/staff/self-registrations', 'post', 'submitStaffSelfRegistration', True, ['app', 'staff_private_phone'])
   ('/api/v1/staff/profiles/{id}/employment-status', 'post', 'setStaffEmploymentStatus', False, ['web', 'staff_work_phone', 'staff_private_phone'])
   ('/api/v1/staff/settings/shift-visible-days', 'post', 'setStaffShiftVisibility', False, ['web', 'staff_work_phone', 'staff_private_phone'])
   ('/api/v1/staff/settings/payroll-penalty-cap', 'post', 'requestStaffPenaltyCap', True, ['web', 'staff_work_phone', 'staff_private_phone'])
   ('/api/v1/staff/locker-events', 'post', 'recordStaffLockerScan', True, ['kiosk'])
staff_ops_allowing_app [('/api/v1/staff/registration-companies', 'get'), ('/api/v1/staff/self-registrations', 'post')]
write_ops_idempotency_false_new [('/api/v1/staff/profiles/{id}/employment-status', 'post'), ('/api/v1/staff/settings/shift-visible-days', 'post')]
self_operated_path_present True
main ops 720 info.version 0.1.0
-- baseline failure events for punch/shifts (json)
/api/v1/staff/attendance/punch baseline failure= staff.attendance.punch_failed success= staff.attendance.punched terminals= ['staff_private_phone', 'web']
/api/v1/staff/shifts baseline failure= staff.shift.save_failed success= staff.shift.saved terminals= ['web', 'staff_private_phone']
/api/v1/staff/shifts/batch baseline failure= staff.shift.save_failed success= staff.shift.saved terminals= ['web', 'staff_private_phone']
-- main test asserting declared failure events
593:    void failurePathsRecordDeclaredFailureAuditEvents() {
594:        // P1 审计闭合:失败路径按 openapi x-audit-event-failure 落审计事件后再抛。
-- repo contract gate in overlay (/tmp/u14_contract_check.log tail)
OpenAPI contract verification passed.
verify_openapi_contract_exit=0
-- HTML anchors
579:                <li>私人手机 App 安装时生成并绑定一个该手机专属的设备标识，把手机和员�
=== shared overlay tools/gates log (/tmp/u13_18_tools.log) ===
START=2026-09-09T21:09:25Z
node=v24.20.0
=== openapi json/yaml parity (python; yaml via PyYAML if present) ===
json_ops 730 json_info_version V0.14
=== main openapi: 9 new paths absent? ===
/staff/shifts/{id}/history                    main=0 unpub=1
/staff/settings/shift-visible-days            main=0 unpub=1
/staff/settings/payroll-penalty-cap           main=0 unpub=1
/staff/self-registrations                     main=0 unpub=1
/staff/registration-companies                 main=0 unpub=1
/staff/profiles/{id}/employment-status        main=0 unpub=1
/staff/locker-events                          main=0 unpub=1
/merchants/{merchant_id}/self-operated        main=0 unpub=1
=== data-isolation spec --check (overlay = material) ===
dis_check_exit=0
data isolation spec is in sync with Flyway migration scope baseline.
=== domain-ownership --check (overlay) ===
dos_check_exit=0
DOMAIN_OWNERSHIP_SCOPE_PASS: 332 tables classified
=== regenerate operation matrix + closure appendix in overlay; compare to material copies ===
matrix_gen_exit=0
generated docs/p0-openapi-operation-acceptance-matrix-v0.1.md with 730 operations
appendix_gen_exit=0
generated /tmp/u-overlay/docs/p0-openapi-contract-closure-appendix-v0.1.md with 730 operations, 329 permissions, 1460 events
matrix_same_as_material=yes diff_lines=0
appendix_same_as_material=yes diff_lines=0
=== verify-pii-encryption (overlay) ===
pii_exit=0
PII_ENCRYPTION_PASS: 16 个 PII 列均经 piiCipher.encrypt 落库
=== security baseline generator available? (do not run network) ===
tools/generate-formal-security-baseline.mjs
=== main data-isolation spec auto block header (V410) vs material (V409) ===
/home/james/complex-management/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:59:> 事实源：`backend/src/main/resources/db/migration`。当前解
END=2026-09-09T21:09:26Z
grep: /tmp/u13_18_tools.log: binary file matches
END=2026-09-09T21:12:33Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 14 段（1 文件 `docs/openapi/openapi-p0.json` 51250 行 1696081 字节，OVERSIZE-SINGLE-FILE），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05 -- docs/openapi/openapi-p0.json`（+764/−33）：新增 9 条路径（排班历史、可见天数读/写、扣除上限读/写、公司员工自助注册、可注册公司、任职状态、寄存柜事件、商户自营标注）与既有 8 个 operation 的描述/终端/事件/字段修订（`StaffRegistrationRequest` phone/id_doc 取代哈希、`merchant_id` 必填；排班成功事件改 `governance.supervisor_review.evaluated`；打卡终端改工作手机、方法枚举去 manual 加 charging_disconnect）。
- 覆盖方法（超大文件如实声明）：不逐行通读 1.7MB JSON；相对基线 diff 全读 + python 载入整份 JSON 统计（730 operation、`info.version=V0.14`、写口失败事件为 `none` 的清单、允许 `app` 终端的员工接口、新增写口幂等标记、自营路径存在）+ 在 `/tmp/u-overlay` 运行仓库门禁 `tools/verify-openapi-contract.mjs`（json/yaml 深比较、占位符检查）退出 0（§2）。
- 第一性原理线：契约是代码与文档的交汇——逐条对照 HTML：PGM L1.16「谁发起、为什么、谁审批、结果是什么，都要能查到」（失败路径审计事件）；PGM L579 私人手机安装时绑定设备标识（自助注册终端身份）；BSM L701/L1433 六类合作方式含自营（自营标注端点已由操作员答「删除」）；BSM L1445 打卡三种方式（枚举）；BSM L1443 排班审批（成功事件改审批评估）。
- 顺从式线：18 项通用判据逐项（§5）。
- 对抗式线：`x-audit-event-failure: none` 是否出现在写口（是，8 个，其中打卡与排班两处是从基线既有失败事件退化为 none）；自助注册/可注册公司允许 `app` 终端（U01-001 同根：客户端自报类型）；`setStaffEmploymentStatus`/`setStaffShiftVisibility` `x-idempotency-required: false`（U05-011 同根）；`setMerchantSelfOperated` 路径（U04-001 已答删除）；json 与 yaml 是否一致（门禁绿）。
- 只读依赖追流（不审）：第 5/6/8 段控制器（实现面）、第 16/17 段生成物、第 13 段安全基线（SHA/计数）。
- 可执行验证：`verify-openapi-contract.mjs` 退出 0（§2）；本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- main 自基线未改本文件（blob＝基线，720 operation、`info.version 0.1.0`）→ 快进合并；9 条新路径 main 均无（§2 探针 0 命中）。

## 5. 18 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 不通过（登记） | `setMerchantSelfOperated` 为已答删除的添加项契约面（CX-U14-003） |
| GEN-002 可执行正确性 | 通过 | 门禁绿；730 operation 与第 16/17 段生成物一致 |
| GEN-003 测试有效性 | 通过（登记） | main `failurePathsRecordDeclaredFailureAuditEvents` 按 `x-audit-event-failure` 断言失败事件；声明为 none 即无断言（CX-U14-001） |
| GEN-004 信任边界 | 通过（登记） | 公开两口 `security: []`、`x-data-scope: self`；终端自报 `app`（CX-U14-002） |
| GEN-005 授权 | 通过 | 新口权限码与第 5/6/8 段控制器一致（`staff.profile.self` 仅柜机事件） |
| GEN-006 数据完整性 | 通过 | 枚举闭集（任职五态、打卡三方式、三条审批线） |
| GEN-007 并发幂等 | 不通过 | 两个新增写口 `x-idempotency-required: false`（CX-U14-004，同根 U05-011） |
| GEN-008 接口一致性 | 通过（登记） | 与实现面字段一致；`x-data-scope: project` 对「按经营主体」描述的设置口（OBS-1） |
| GEN-009 错误处理 | 通过 | 响应码闭集 |
| GEN-010 安全 | 通过 | 手机/证件原值仅传输，描述明确后端加密与脱敏 |
| GEN-011 审计证据 | 不通过 | 8 个写口失败事件 `none`，其中打卡/排班两处从基线既有事件退化（CX-U14-001） |
| GEN-012 资源 | 不适用 | 契约 |
| GEN-013 构建 | 通过 | 门禁绿；第 16/17 段生成物由本文件生成且一致 |
| GEN-014 迁移 | 不适用 | 无 |
| GEN-015 界面 | 通过 | 摘要/描述中文 |
| GEN-016 文档 | 通过（登记） | `info.version` 由 semver `0.1.0` 改为 `V0.14`（OBS-2） |
| GEN-017 分类 | 通过 | 添加项只有 U04-001 已答删除的契约面，不再问 |
| GEN-018 证据 | 通过 | 结论落到 diff、python 统计、门禁输出 |

## 6. 本次 Findings（临时 ID CX-U14-00N；正式 ID 由 B2 分配）

### CX-U14-001 · 技术性 · 发布前须修：是

- 位置：8 个写 operation `x-audit-event-failure: none`：`submitStaffSelfRegistration`、`setStaffEmploymentStatus`、`setStaffShiftVisibility`、`requestStaffPenaltyCap`、`recordStaffLockerScan`（新增）；`punchStaffAttendance`（基线 `staff.attendance.punch_failed`→none）、`upsertStaffShift`/`batchUpsertStaffShifts`（基线 `staff.shift.save_failed`→none）。main 测试 `StaffJdbcIntegrationTests.failurePathsRecordDeclaredFailureAuditEvents`（`:593-624`）以契约声明的失败事件为断言依据。
- HTML 原句：PGM L1.16「谁发起、为什么、谁审批、结果是什么，都要能查到」。
- 判据：GEN-011、GEN-003；依据类型：HTML 原句 + diff 事实。
- 业务影响：失败路径不再有约定的审计事件，审计闭合门禁对这些口失去断言面；打卡失败（他人手机/未验证设备被拒）与排班提交失败无留痕。
- 无业务发明理由：为写口声明并记录失败事件是仓库既有范式（`x-audit-event-failure` + 测试）。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：为 8 个写口声明失败事件（如 `staff.self_register_failed`、`staff.attendance.punch_failed` 保留、`staff.shift.submit_failed` 等）并在第 5/6 段实现失败路径 `recordFailure`，第 16/17 段随之再生成。

### CX-U14-002 · 冲突（同根 U01-001/U02-001）· 发布前须修：是

- 位置：`submitStaffSelfRegistration`、`listStaffRegistrationCompanies` `x-terminal-allowed: [app, staff_private_phone]`。
- HTML 原句：PGM L579/BSM L935「私人手机 App 安装时生成并绑定一个该手机专属的设备标识，把手机和员工绑定」。
- 判据：GEN-004、GEN-008；依据类型：HTML 原句。
- 业务影响：契约允许以自报 `app` 类型访问注册口，与 U01-001（客户端自报终端类型）同根；按定稿处置后应只承认设备绑定的 `staff_private_phone` 身份（含设备证明，U05-003）。
- 工程缺陷：是。建议登记：是。最小处置：与 U01-001/U02-001/U05-003 同 PR 改终端列表与请求体（增加设备证明字段）；不新增终端类型。

### CX-U14-003 · 添加（操作员已答删除，questions/U04-001.md）· 发布前须修：是

- 位置：`/api/v1/platform/merchants/{merchant_id}/self-operated` POST 全节（含 `self_operated` 布尔请求体、事件 `merchant.self_operated_changed/_change_failed`）。
- 判据：GEN-001、GEN-017；依据类型：操作员 2.2 答复。
- 工程缺陷：是。建议登记：是（引用已答复，不再问）。最小处置：删除该路径；operation 计数 730→729；第 13/16/17 段生成物与安全基线随之再生成。

### CX-U14-004 · 技术性（同根 U05-011）· 发布前须修：是

- 位置：`setStaffEmploymentStatus`、`setStaffShiftVisibility` `x-idempotency-required: false`（其余新增写口 true）。
- 判据：GEN-007、GEN-008；依据类型：diff 事实。
- 业务影响：契约与实现同时缺幂等，与仓库写口幂等范式不一致。
- 工程缺陷：是。建议登记：是。最小处置：改 true 并在请求体加 `idempotency_key`，与 U05-011 同 PR。

## 7. 观察（登记、不入队）

- OBS-1：可见天数/扣除上限设置口 `x-data-scope: project` 而描述为「按当前经营主体」；员工域既有 operation 多为 platform/project 口径，属 main 既有范式，归 M391。
- OBS-2：`info.version` 由 `0.1.0` 改为 `V0.14`（版本铁律格式取代 semver）；门禁未依赖该字段（`verify-openapi-contract` 绿）。
- OBS-3：管理写口（设置可见天数、上限申请、任职状态）允许 `staff_private_phone`，与基线既有 `upsertStaffShift` 允许私人手机同范式（HTML BSM L1443「主管在手机上也能排」未区分手机类型）；归 M391 既有。
- OBS-4：`submitStaffRegistration` 终端改为 web/merchant_app/merchant_portal/staff_work_phone、`merchant_id` 必填，与 HTML 两条路径一致；`punchStaffAttendance` `device_fingerprint` 改为「可不填」与 U07 设备级验签一致。
- 技能：`using-superpowers`、`verification-before-completion`（门禁实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。超大文件按「diff 全读 + 整份载入统计 + 仓库门禁」覆盖（如实声明未逐行通读）；18 项判据逐项核对；发现 2 项技术性（CX-U14-001 失败事件 none、CX-U14-004 写口不幂等）、1 项冲突同根（CX-U14-002 自报 app 终端）、1 项添加消费方（CX-U14-003 自营端点，已答删除）、4 项观察；无 HTML 修改申请；门禁绿；未修源、未提交/推送。本报告落盘前未读取 `U14-b1-grok-raw.md` 任何正文。
---

# U14 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.70「当前 U14 B2-r1 整批候选」U14-001…009；modules/U14-b1-grok-raw.md V0.01（43111bytes SHA5e716790f4b04710ccee15614e6f3520df2c4d6a0ec1d03ccd47bf7bdbebb73b）；本文件 B1 段（17652bytes SHAcb5482ce21b51247ee5fa014112b7b6b6a5c691ad0aec580aa58ecaabf88f69a）。2026-09-10T03:55:00Z
- 席甲 B2 核实（只读，见 ledger V2.84 U14 节）：Grok CMD-PARSE 计数（730/720、public 7/5、failure=none 12/写口 8、schema 仅 StaffRegistrationRequest 变）与席甲 §2 一致；`:36554-36556` GET 可见天数 description、`:36971-36987` locker 双身份与双值 success 字面量、`:46393-46398`/`:38580` required/example 互斥原文如引；U05-003/005/006/011、U04-001、U13-001/004/006 定稿处置一致；只叠 json 的两闸红为可复现门禁事实。

## 勘误（席甲 B1 段）

- 席甲 B1 漏判：只叠本 json 到 main 的契约/矩阵门禁红（U14-001，叠法）；GET 可见天数 description 把空上下限写成合同（U14-004）；locker 双身份契约与双值成功事件（U14-005）；StaffRegistrationRequest schema/example/Java 注释三套互斥（U14-007）。席甲 B1 票 FAIL 不变，缺陷计数由 4 升为 8。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 仅叠 json 两闸红、同树绿为可复现事实；处置只引用既有生成器与门禁 |
| 002 | PASS | 自营写口即 U04-001 已答删除轴的契约面；不再问 |
| 003 | PASS | 无设备字段、含 app 终端、public；PGM L579/BSM L935/BEQR L233；随 U05-003/U01-001/U07-004 |
| 004 | PASS | description 与 BSM L1444/L521 相反；随 U05-005/U10-003 |
| 005 | PASS | 双身份契约与 PGM L582 自动记录相悖；成功事件双值字面量属实 |
| 006 | PASS | 两口 false 与同控制器写口不一致；随 U05-011 |
| 007 | PASS | required/example/注释互斥属实 |
| 008 | PASS | 12 处 none、写口 8 属实；main 0 |
| 009 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 8 条，定稿以此为准）

1. U14-005 处置读作「柜机以已登记 kiosk 设备签名/设备令牌认证单端上报」，与 U05-006/U13-006 同一契约，不是另发一套令牌。
2. U14-003 补回条件句：「公司列表若仍需匿名发现，须另证来源校验，不能作为无绑定注册的前置」；来源校验已在 U07-004，不发明。
3. U14-003 同根：U01-001 为私人手机通道/不自报 app；设备公钥/指纹为 U05-003/U01-007/U02-002；处置两句并行。
4. U14-003 字段名 `device_public_key`/`device_fingerprint_hash` 随 U05-003 语义，不把属性名钉成新业务字段。
5. U14-001「生成 yaml」＝与 U15 同批改到 `verify:openapi` 深等；附录用既有 `docs:openapi-closure`、矩阵用 `docs:operation-matrix`。
6. U14-009 席甲 OBS-4 不覆盖 Grok §8.6：打卡 `device_fingerprint` 为声明不采用的死字段，随打卡改写可删，不单独立项。
7. U14-009 第 3 条只指本段未重复错误句；U05-004 实现面仍须修。
8. U14-001 处置格补「与 U05+U06+U07+U10+U13+U15+U16+U17 同一 PR」。

## 结论

席甲 B2-r1 票：**PASS**（候选 U14-001…009 成立；发布前须修「是」8 / 「否」1；添加 1（U14-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 69235bytes SHA06eec5171442f94567fae223b7d94d2857b480e53fd2710805635185a5ff9c51，前 43111bytes 原字节不变）。
