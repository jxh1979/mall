CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U16 未发布材料第 16 段（接口覆盖说明）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T04:35:56Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `7708f4533ca3128f6da1ef94a80f47e49985a017`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 16 段，1 文件 2615 行（p0-openapi-contract-closure-appendix-v0.1.md 2615），main 主归属 M389；适用判据＝映射行并集 43 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。可执行验证：在 `/tmp/u-overlay` 内以材料自带 `tools/generate-openapi-contract-closure-appendix.mjs` 重生成附录并与副本逐字节比较（/tmp/u13_18_tools.log：appendix_same_as_material）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U16-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U15 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U16-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：第 14/15 段 openapi 候选（失败事件 none、app 终端、自营端点删除）——本附录由 openapi 生成，处置为随契约修正后重生成。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/p0-openapi-contract-closure-appendix-v0.1.md | 2615 | 564772 | d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e | 全读 |

## 2. 可执行证据原文（/tmp/u16_b1_run.log）

```
START=2026-09-09T21:12:34Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
docs/p0-openapi-contract-closure-appendix-v0.1.md | 2615 | 564772 | d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e
=== manifest entries ===
docs/p0-openapi-contract-closure-appendix-v0.1.md 2615 564772 d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e
=== baseline -> unpublished tree (numstat) ===
51	20	docs/p0-openapi-contract-closure-appendix-v0.1.md
=== main HEAD vs baseline (blob) ===
docs/p0-openapi-contract-closure-appendix-v0.1.md baseline=94714d4eca head=94714d4eca unpublished=49f93c4a67 main_changed_since_baseline=no
=== segment probes (/tmp/u16_probes.sh) ===
-- appendix header/version + counts
# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.03
17:| OpenAPI operation 数 | 730 | 全部必须进入 Java Controller、DTO、Service、权限、事件和验收追溯 |
19:| OpenAPI 成功/失败事件数 | 1460 | 人工事件矩阵正文未覆盖 1418 项；完整事件字典以本文第 4 节为准 |
21:| 写接口数量 | 440 | 写接口、外部接入接口进入编码或联调前必须补请求/响应示例 |
-- rows reflecting openapi candidates (failure none, public self-registration, self-operated)
12
273:| `merchant.profile.manage` | staff-token | project, project-merchant | 3 | createMerchant, updateMerchant, setMerchantSelfOperated | �
289:| `public` | public | customer, none, public-project, self | 7 | login, customerPhoneLogin, customerLogout, listCustomerTicketProducts, 
1382:| `merchant.self_operated_change_failed` | failure | `setMerchantSelfOperated` | `POST` | `/api/v1/platform/merchants/{merchant_id}/sel
1383:| `merchant.self_operated_changed` | success | `setMerchantSelfOperated` | `POST` | `/api/v1/platform/merchants/{merchant_id}/self-oper
1404:| `none` | failure | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | P0-待归类 | `public` | 接口级�
1625:| `staff.self_registered` | success | `submitStaffSelfRegistration` | `POST` | `/api/v1/staff/self-registrations` | P0-待归类 | `pub
600
570
-- regenerated in overlay == material? (from tools log)
appendix_gen_exit=0
appendix_same_as_material=yes diff_lines=0
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
END=2026-09-09T21:12:34Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 16 段（1 文件 `docs/p0-openapi-contract-closure-appendix-v0.1.md` 2615 行 564772 字节），不是 main；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05`（+51/−20）：标题 V0.1→V0.03，计数 720→730 operation / 434→440 写口 / 1440→1460 事件，权限表 4 行（`merchant.profile.manage` +setMerchantSelfOperated、`public` +两条公开注册口、`staff.payroll.manage`/`staff.profile.review`/`staff.shift.manage` 新口、新增 `staff.profile.self`），事件表新增 21 行（含 12 行 `none`）与删除 5 行（`staff.shift.saved/save_failed`、`staff.attendance.punch_failed`），任务追溯表新增/修订 13 行。
- 第一性原理线：本文是 `tools/generate-openapi-contract-closure-appendix.mjs` 从 openapi 生成的派生物——审计点是「是否由材料自身工具生成且一致」（overlay 内重生成与副本逐字节相同，§2）与「契约缺陷是否被固化」（第 14 段候选在本文以 `none` 事件、`public` 口、自营端点行体现）。
- 顺从式线：43 项判据逐项（§5）。对抗式线：手改痕迹（无：重生成一致）；「P0-待归类」类别行数 570→600（新增 30 行全部待归类，main 既有范式）。
- 只读依赖追流：第 14/15 段契约、第 17 段生成器改动（版本号从文档首行读取）。
- 1 文件全文逐行读完（§1）；diff 逐块读完；可执行验证：overlay 内 `node tools/generate-openapi-contract-closure-appendix.mjs` 退出 0、输出与副本 sha256 相同（§2）。本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- main 自基线未改本文件 → 快进合并；但内容随第 14 段契约修正必须重生成（730→729 等）。

## 5. 43 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 生成物随契约更新 |
| GEN-002 可执行正确性 | 通过 | 重生成一致 |
| GEN-003/004/005/006/007/009/010/012/014/015 | 不适用 | 派生文档 |
| GEN-008 接口一致性 | 通过（登记） | 与第 14 段一致，因此也固化了第 14 段候选（CX-U16-001） |
| GEN-011 审计证据 | 不通过（登记） | 事件字典新增 12 行 `none`、删除基线既有失败事件 3 行——契约问题在第 14 段，本文随之（CX-U16-001） |
| GEN-013 构建 | 通过 | `verify:openapi-closure --check` 口径：overlay 内生成物一致 |
| GEN-016 文档 | 通过 | 版本 V0.03 由生成器从首行读取 |
| GEN-017 分类 | 通过 | 无添加（自营端点行随 U04-001 删除消失） |
| GEN-018 证据 | 通过 | diff、重生成比对 |
| 其余 M389 映射行（25 项） | 通过／不适用 | 权限/事件/任务追溯闭合口径未被手改；新增口全部落在「P0-待归类」（OBS-1） |

## 6. 本次 Findings（临时 ID CX-U16-00N；正式 ID 由 B2 分配）

### CX-U16-001 · 技术性（派生物随第 14 段重生成）· 发布前须修：是

- 位置：全文（由 openapi 生成）：`:17-21` 计数、`:273/:289` 权限行、`:1382-1404` 事件行（含 `setMerchantSelfOperated` 两行、12 行 `none`）、任务追溯表新增行。
- 判据：GEN-008、GEN-011、GEN-013；依据类型：重生成比对 + 第 14 段候选。
- 业务影响：若第 14 段修正契约（删自营端点、补失败事件、改终端）而本文不重生成，`verify:openapi-closure --check` 红。
- 无业务发明理由：生成物。
- 工程缺陷：是。建议登记：是。最小处置：契约修正后运行 `npm run docs:openapi-closure` 并 `--check`；不手改。

## 7. 观察（登记、不入队）

- OBS-1：新增 30 行任务追溯全部归入「P0-待归类」（570→600），与 main 既有 570 行同范式，归 M389。
- OBS-2：`staff.locker.store / staff.locker.retrieve` 以斜杠并列写在成功事件列，与生成器按单事件名的字典口径不一致（源于契约字段值），随第 14 段 CX-U14-001 处置一并规范。
- 技能：`using-superpowers`、`verification-before-completion`（重生成比对附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。1 文件全文三线审计、43 项判据逐项核对；1 项技术性须修（CX-U16-001 随第 14 段契约修正重生成）、2 项观察；无添加项；无 HTML 修改申请；overlay 重生成一致；未修源、未提交/推送。本报告落盘前未读取 `U16-b1-grok-raw.md` 任何正文。
---

# U16 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.76「当前 U16 B2-r1 整批候选」U16-001…008；modules/U16-b1-grok-raw.md V0.01（50713bytes SHA61a7d7edb9a25d84c2b28ec92c13bbaf21ee5ac277e54060dbc375913dc6386e）；本文件 B1 段（11699bytes SHA1998abe2a5646e728942a4bbd23a54b2977524f5a7015fdd7d854e49e1a45646）。2026-09-10T04:50:50Z
- 席甲 B2 核实（只读，见 ledger V2.93 U16 节）：附录 `:1-21/:273/:289/:1129-1132/:1382-1405/:1596/:2400/:2437/:2477/:2611-2615` 原文如引；main 附录 `upsertStaffShift` 原成功名 `staff.shift.saved`、`punchStaffAttendance` 原失败名 `staff.attendance.punch_failed`；上游 json 三口 `x-audit-event-success: governance.supervisor_review.evaluated` 属实；`docs:openapi-closure --check` 只叠附录到 main 红、main 生成器对未发布稿红（版本行）；U14-001/002/003/005/008 定稿处置逐条适用。

## 勘误（席甲 B1 段）

- 席甲 B1 以一条「派生物随第 14 段重生」覆盖，漏判：自营写口入字典与公开注册无设备证明应分项（U16-002/003）；三口成功事件盗用 `governance.supervisor_review.evaluated`（U16-006，U14-008 只覆盖失败侧）；标题 V0.03 与 §9 变更记录 V0.1（U16-007）。席甲 B1 票 FAIL 不变。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 只叠附录 `--check` 红、main 生成器版本行红为可复现门禁事实；处置只引用既有生成器 |
| 002 | PASS | 字典收纳 U04-001 已答删除写口；随 U14-002 重生 |
| 003 | PASS | public 7 口、无设备字段、含 app；PGM L578/L579、BSM L935 |
| 004 | PASS | 16 行 none、AC-CLOSURE-002 自相矛盾；随 U14-008 |
| 005 | PASS | 双值字面量属实；随 U14-005 |
| 006 | PASS | 三口成功名与 evaluateSupervisorReview 同名属实；operation 级命名不发明业务 |
| 007 | PASS | 标题/变更记录不一致属实；随第 17 段生成器修正后重生 |
| 008 | PASS | 观察归并未改写双方原意；HTML 入口张力只登记 |

## 勘误与采纳（接受 Grok B2-r1 非阻断修正 10 条，定稿以此为准）

1. U16-008 第 14 条补回 Grok 原意：§1 L11「以本文为准」基线既有，处置是改 JSON 后重生，**不得删这句**（对「人工矩阵过时、以 OpenAPI 为准」仍有技术用途）。
2. U16-001 单元格补「禁止整树覆盖」；同 PR 集合写全 U14+U15+U16+U17（U13 安全基线随后 `docs:security-baseline`）；脚本名为既有 `docs:openapi-closure` / `verify:openapi-closure`。
3. U16-006 写明：`staff.shift.submitted` / `staff.payroll.penalty_cap_requested` 只是 `x-audit-event-success` 标识符；括号「排班提交待审」引用 HTML BSM L1443「须经上一级审批后生效」，不发明新审批态、不新开 2.2；`recordSuccess` 归 U06 同 PR，本段最小是改 json 后重生附录；两排班口共用一名与基线共用 `staff.shift.saved` 同构。
4. U16-005「单一成功名（动作进明细）」读作 U14-005 定稿的「单一名**或**按 action 分两个 operation」。
5. U16-003 同根补 U07-004（频控）；保留完整句：私人手机通道、无指纹 4xx、公司列表若仍需匿名发现须另证来源校验且不能作为无绑定注册的前置。不改冲突分类。
6. U16-008 第 7 条保留 Grok 原句「不单独立项 / 首轮不因此单独红门禁」；不另开 ID。
7. U16-004 本段最小是契约事件名随 json 重生；`recordFailure` 归第 5/6 段同 PR。
8. U16-007 两条处置为或支（内容变化时标题递增并追加 §9 一行，或删除变更表只留标题版本），发布者择一，不发明第三套版本政策。
9. U16-008：席甲 OBS-1「+30 行待归类」与 Grok「10 个新 operation 进 P0」计数口径不同（席甲含修订行），作观察并存，不升缺陷。
10. U16-008 第 1 条「本路径 main＝基线」即 Grok「对抗式覆盖 M009–M012 已修字节对本文件不成立」，不得读成整包覆盖未发布 OpenAPI 也无冲突（那是 001/002/003 的字典背书面）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U16-001…008 成立；发布前须修「是」7 / 「否」1；添加 1（U16-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 77378bytes SHAaaa57644449227869bda88790565ad6c780e50927f883bc95f536b39ad507341，前 50713bytes 原字节不变）。
