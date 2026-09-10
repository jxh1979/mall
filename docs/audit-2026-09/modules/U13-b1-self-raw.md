CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U13 未发布材料第 13 段（员工设计及交付说明）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T01:37:19Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `3803eba074455c757c5e6664341977e30d0f1b87`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 13 段，7 文件 1184 行（data-isolation-and-tenant-scope-security-spec-v0.1.md 371、domain-ownership-scope-audit-v0.1.csv 333、security-baseline-latest.json 14、r5-spec-T1-staff-tables-dev-design-20260618.md 197、r5-spec-T9-staff-device-android-dev-design-20260618.md 54、findings-register-v1.md 53、formal-security-baseline-v1.md 162），main 主归属 M028/M411/M058/M420；适用判据＝映射行并集 60 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。可执行验证：在 `/tmp/u-overlay`（基线 + 76 未发布文件）内运行材料自带生成器/门禁：`node tools/generate-data-isolation-spec.mjs --check`、`node tools/generate-domain-ownership-scope-audit.mjs --check`、`node tools/verify-pii-encryption.mjs`（/tmp/u13_18_tools.log），不触主仓。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U13-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U12 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U13-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：U10-001（迁移编号须 ≥V411 后重生成隔离规格）、U05 定稿（封顶分母、事假基数）、U07-001（工作手机 staff_id 无写入路径）、questions/U04-001.md（自营布尔已答删除→接口计数/安全基线再生成）。只读依赖追流（不审）：副本 V400–V409、openapi、tools（第 10/14/15/18 段）；main 当前 data-isolation 规格（已按 V410 再生成）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 371 | 41083 | f41d8414b5011ad31d558531e675ac9c16c030d7fbda2cc1ac6471b64e0c8217 | 全读 |
| docs/domain-ownership-scope-audit-v0.1.csv | 333 | 59483 | 2049652b0a9aca3ca50769c30a36591309f49488d54925030deca96aa5c0b928 | 全读 |
| docs/production-readiness-evidence/security-baseline-latest.json | 14 | 730 | c849b52f854c57e7ee79eb1df158ea1c3a460f2c4ec129b829f91ec631740fd9 | 全读 |
| docs/r5-spec-T1-staff-tables-dev-design-20260618.md | 197 | 19290 | ed164937c9f3da0e89cca43bf57c1a7f26eb8b1fc330ce8a878309a773d4009f | 全读 |
| docs/r5-spec-T9-staff-device-android-dev-design-20260618.md | 54 | 5841 | 8ea401b8eb6413a42dd24dde3dd954e721259c650c55a8c30911aac46b8287e6 | 全读 |
| security-scans/findings-register-v1.md | 53 | 7806 | 00abf315b8cd5ed81a9f5c3cd05a2f5bf28a89b635abbae32b647cb32caedeba | 全读 |
| security-scans/formal-security-baseline-v1.md | 162 | 15321 | 7e9dd55bd8fa69f0ecd4c8edef0b7a265cfef7521747d026391ca7d452d6fd05 | 全读 |

## 2. 可执行证据原文（/tmp/u13_b1_run.log）

```
START=2026-09-09T21:12:33Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 371 | 41083 | f41d8414b5011ad31d558531e675ac9c16c030d7fbda2cc1ac6471b64e0c8217
docs/domain-ownership-scope-audit-v0.1.csv | 333 | 59483 | 2049652b0a9aca3ca50769c30a36591309f49488d54925030deca96aa5c0b928
docs/production-readiness-evidence/security-baseline-latest.json | 14 | 730 | c849b52f854c57e7ee79eb1df158ea1c3a460f2c4ec129b829f91ec631740fd9
docs/r5-spec-T1-staff-tables-dev-design-20260618.md | 197 | 19290 | ed164937c9f3da0e89cca43bf57c1a7f26eb8b1fc330ce8a878309a773d4009f
docs/r5-spec-T9-staff-device-android-dev-design-20260618.md | 54 | 5841 | 8ea401b8eb6413a42dd24dde3dd954e721259c650c55a8c30911aac46b8287e6
security-scans/findings-register-v1.md | 53 | 7806 | 00abf315b8cd5ed81a9f5c3cd05a2f5bf28a89b635abbae32b647cb32caedeba
security-scans/formal-security-baseline-v1.md | 162 | 15321 | 7e9dd55bd8fa69f0ecd4c8edef0b7a265cfef7521747d026391ca7d452d6fd05
=== manifest entries ===
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 371 41083 f41d8414b5011ad31d558531e675ac9c16c030d7fbda2cc1ac6471b64e0c8217
docs/domain-ownership-scope-audit-v0.1.csv 333 59483 2049652b0a9aca3ca50769c30a36591309f49488d54925030deca96aa5c0b928
docs/production-readiness-evidence/security-baseline-latest.json 14 730 c849b52f854c57e7ee79eb1df158ea1c3a460f2c4ec129b829f91ec631740fd9
docs/r5-spec-T1-staff-tables-dev-design-20260618.md 197 19290 ed164937c9f3da0e89cca43bf57c1a7f26eb8b1fc330ce8a878309a773d4009f
docs/r5-spec-T9-staff-device-android-dev-design-20260618.md 54 5841 8ea401b8eb6413a42dd24dde3dd954e721259c650c55a8c30911aac46b8287e6
security-scans/findings-register-v1.md 53 7806 00abf315b8cd5ed81a9f5c3cd05a2f5bf28a89b635abbae32b647cb32caedeba
security-scans/formal-security-baseline-v1.md 162 15321 7e9dd55bd8fa69f0ecd4c8edef0b7a265cfef7521747d026391ca7d452d6fd05
=== baseline -> unpublished tree (numstat) ===
2	2	docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
333	329	docs/domain-ownership-scope-audit-v0.1.csv
5	4	docs/production-readiness-evidence/security-baseline-latest.json
18	9	docs/r5-spec-T1-staff-tables-dev-design-20260618.md
17	7	docs/r5-spec-T9-staff-device-android-dev-design-20260618.md
3	3	security-scans/findings-register-v1.md
15	15	security-scans/formal-security-baseline-v1.md
=== main HEAD vs baseline (blob) ===
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md baseline=93a02c148b head=5e4127b456 unpublished=f4f0b6c6cb main_changed_since_baseline=yes
docs/domain-ownership-scope-audit-v0.1.csv baseline=71ec9b9550 head=71ec9b9550 unpublished=199d326116 main_changed_since_baseline=no
docs/production-readiness-evidence/security-baseline-latest.json baseline=45594e6f0a head=45594e6f0a unpublished=9b25c02b38 main_changed_since_baseline=no
docs/r5-spec-T1-staff-tables-dev-design-20260618.md baseline=cf60300b03 head=cf60300b03 unpublished=32f0c5bc70 main_changed_since_baseline=no
docs/r5-spec-T9-staff-device-android-dev-design-20260618.md baseline=1663d5ba4f head=1663d5ba4f unpublished=9c966cfbf5 main_changed_since_baseline=no
security-scans/findings-register-v1.md baseline=99db49dd29 head=99db49dd29 unpublished=7e466d104b main_changed_since_baseline=no
security-scans/formal-security-baseline-v1.md baseline=91dd711a2b head=91dd711a2b unpublished=a3ccd2c068 main_changed_since_baseline=no
=== segment probes (/tmp/u13_probes.sh) ===
-- data-isolation spec auto-block header: main vs material
59:> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V410**，发现 **170** 张含 scope 字段的表
59:> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V409**，发现 **170** 张含 scope 字段的表
-- CSV: header + row count + file_version column + new tables
table_name,scope_type,scope_source,scope_path,multi_complex_note,file_version,status
333
332
265:staff_locker_records,inherited,staff_profile_id,staff_profile_id -> staff_profiles -> current authenticated employee
270:staff_penalty_cap_revisions,global_reference,global_reference,global reference/configuration table,safe as global re
271:staff_policy_settings,direct,scope_id,shift_visible_days: scope_id = employer business entity; payroll_penalty_cap_b
273:staff_shift_revisions,inherited,review_id/shift_id,review_id -> supervisor_reviews; published shift_id -> staff_shif
-- security-baseline evidence json (material) + referenced log present in 76 files?
{
  "status": "passed",
  "evidence_type": "formal_security_baseline_v1",
  "verified_at": "2026-09-08T11:59:55Z",
  "verified_on_host": "192.168.88.160",
  "verification_command": "npm run verify:security-baseline",
  "baseline_path": "security-scans/formal-security-baseline-v1.md",
  "findings_register_path": "security-scans/findings-register-v1.md",
  "openapi_operation_count": 730,
  "security_baseline_version": "V1.02",
  "plaintext_secret_present": false,
  "notes": "M010 新增注册、排班审批参数、档案状态和寄存事件接口后同步730项接口；160可丢弃副本 r1-predeploy-final.log 中 verify:security-baseline 实际通过。生产前置阻断仍以原台账为准。",
  "version": "V0.02"
}
0
-- OpenAPI SHA in material baseline docs vs actual sha256 of material openapi json
.scratch/module-recheck-20260908/unpublished-03acdb98/security-scans/findings-register-v1.md:6:- OpenAPI operation count: 730
.scratch/module-recheck-20260908/unpublished-03acdb98/security-scans/findings-register-v1.md:7:- OpenAPI SHA-256: `f8de4f8c2957953911030512a
.scratch/module-recheck-20260908/unpublished-03acdb98/security-scans/formal-security-baseline-v1.md:6:- OpenAPI operation count: 730
.scratch/module-recheck-20260908/unpublished-03acdb98/security-scans/formal-security-baseline-v1.md:7:- OpenAPI SHA-256: `f8de4f8c2957953911
f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384
-- T1/T9 doc claims vs code (denominator, staff_id assignment, V400-V409)
7:> **迁移**：三表最初由 V066 建立；本次 M010 在现有迁移之后追加 V400—V409；测试用 `MigrationVersions.latest()` 动态读版本（R3
11:- 注册分两条路径：商户通过 `/staff/registrations` 代提交，后端由经营主体上下级关系选取对应域公司；公司员工通过公�
12:- 姓名、手机号和证件由现有 PiiCipher 加密；日常接口固定脱敏手机和证件。任职状态独立保存待入职、在职、试用、停
14:- 普通扣除合计按当月固定工资与奖金的比例封顶，默认20%，提成不进入分母，事假单算。上限变更要求人事、财务、�
11:1. **注册**：公司员工从登录页本人填写资料并选择公司，调用 `POST /api/v1/staff/self-registrations`；商户代员工提交调用 `/st
29:公司工作手机沿用 `terminal_devices.staff_id` 对应员工，重新登记保持该对应关系。设备管理员在本机登记设备编号、公钥�
33:打卡页使用实际扫码或 NFC 回调，不提供手工录入打卡。应用运行且员工已登录时，系统充电断开事件会提交领取记录�
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

- 对象口径：未发布提交 03acdb98 的只读输入副本第 13 段（7 文件 1184 行：数据隔离规格、域归属盘点 CSV、安全基线证据 JSON、T1/T9 设计规格、安全扫描登记册与正式安全基线），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05`（§2 numstat：规格 +2/−2、CSV +333/−329 全量重写、证据 JSON +5/−4、T1 +18/−9、T9 +17/−7、登记册 +3/−3、正式基线 +15/−15）。
- 第一性原理线：这些文件都是「派生/证据/设计说明」：隔离规格与 CSV 由生成器从迁移目录生成，安全基线由生成器从 openapi 生成，设计规格记录实现口径。审计问题是：生成物是否与材料自身一致（在 overlay 内重跑生成器/门禁）、是否与 main 当前状态冲突（main 已按 V410 再生成隔离规格）、设计说明是否与代码及 HTML 一致（GEN-016）。
- 顺从式线：60 项判据逐项（§5）。
- 对抗式线：隔离规格自动块「V409」对 main「V410」；CSV 新增 `file_version` 列与第 18 段生成器是否配套；证据 JSON 引用的 `r1-predeploy-final.log` 是否在 76 文件内（否，§2 探针 0）；安全基线记录的 OpenAPI SHA `f8de4f8c…` 是否等于材料 openapi json 的实际 SHA（是，§2）；T1「按当月固定工资与奖金的比例封顶」是否把 U05-004 冲突写进了文档；T9「工作手机沿用 `terminal_devices.staff_id` 对应员工」是否有写入路径（无，U07-001）；T1「追加 V400—V409」对 U10-001。
- 只读依赖追流（不审）：第 10 段迁移、第 14/15 段 openapi、第 18 段工具；main 当前隔离规格。
- 7 文件全文逐行读完（§1，CSV 333 行含表头）；diff 逐块读完（§2）。可执行验证：`/tmp/u-overlay` 内 `generate-data-isolation-spec.mjs --check` 退出 0、`generate-domain-ownership-scope-audit.mjs --check` 退出 0（332 表）、`verify-pii-encryption.mjs` 退出 0（16 列）（§2 引用 /tmp/u13_18_tools.log）。本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- 隔离规格：main 自基线已改（M010 检查点按 V410 再生成，自动块「V410」），材料自动块「V409」→ 文本冲突且语义落后，须在迁移重编号后重生成（CX-U13-001）。其余 6 文件 main＝基线。

## 5. 60 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 生成物随迁移/契约变化必须更新；设计说明记录口径 |
| GEN-002 可执行正确性 | 通过（登记） | overlay 内三个门禁绿；但对 main 合入后隔离规格块过期（CX-U13-001） |
| GEN-003 测试有效性 | 不适用 | 文档 |
| GEN-004/005/007/009/010/012/015 | 不适用 | 无实现面（证据 JSON 无秘密，`plaintext_secret_present=false`） |
| GEN-006 数据完整性 | 通过（登记） | 安全基线 SHA 与材料 openapi 实际 SHA 一致；证据 JSON 引用的运行日志不在材料内（OBS-2） |
| GEN-008 接口一致性 | 通过（登记） | CSV 新列 `file_version` 与第 18 段生成器配套（须同批，CX-U13-002） |
| GEN-011 审计证据 | 通过（登记） | 证据 JSON 声称 160 副本实跑通过，不可复核（OBS-2） |
| GEN-013 构建 | 不通过 | 合入 main 后 `verify:data-isolation-spec` 红（自动块版本落后）；`verify:domain-ownership-scope` 若只合 CSV 不合工具亦红（CX-U13-001/002） |
| GEN-014 迁移 | 不通过（登记） | 自动块须在 U10-001 重编号后重生成（CX-U13-001） |
| GEN-016 文档 | 不通过 | T1 把封顶分母写成「固定工资与奖金」（U05-004 冲突入文）、T9 写工作手机 `staff_id` 对应关系「沿用/保持」但无分配路径（U07-001）（CX-U13-003）；T1「追加 V400—V409」随 U10-001 改 |
| GEN-017 分类 | 通过 | 无添加；文档随代码定稿改 |
| GEN-018 证据 | 通过 | 结论落到 diff、门禁实跑、SHA 对照 |
| 其余 M028/M058/M411/M420 映射行（42 项） | 通过／不适用 | 隔离模型条款（direct/inherited/global 分类）在 CSV 内新增 4 表分类合理：`staff_policy_settings` direct(scope_id)、`staff_locker_records`/`staff_shift_revisions` inherited、`staff_penalty_cap_revisions` global；安全基线条款：public operation 5→7 为两条公开注册接口（第 14 段候选） |

## 6. 本次 Findings（临时 ID CX-U13-00N；正式 ID 由 B2 分配）

### CX-U13-001 · 技术性 · 发布前须修：是

- 位置：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:59`（自动块「当前解析到最新迁移 **V409**」）；main 同行「**V410**」（M010 检查点已再生成）；生成器 `tools/generate-data-isolation-spec.mjs`（main，未随材料改）。
- 判据：GEN-013、GEN-014；依据类型：git/门禁事实。
- 业务影响：合入后自动块与迁移目录不一致，`npm run verify:data-isolation-spec`（预部署门禁）红；且材料迁移须重编号 ≥V411（U10-001），本块必然要重生成。
- 无业务发明理由：生成物重生成。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：以 main 为底、U10 重编号后运行 `npm run docs:data-isolation-spec`，提交前 `verify:data-isolation-spec`；不手改自动块。

### CX-U13-002 · 技术性 · 发布前须修：是

- 位置：`docs/domain-ownership-scope-audit-v0.1.csv`（333 行全量重写：新增 `file_version` 列、4 张员工域表）；配套生成器改动在第 18 段 `tools/generate-domain-ownership-scope-audit.mjs`（inheritedRules/globalTables/classify/headers）；main 生成器与 CSV 均为基线版（表头 6 列）。
- 判据：GEN-008、GEN-013；依据类型：门禁事实（overlay 内 `--check` 绿，是因为工具与 CSV 同在）。
- 业务影响：CSV 与工具必须同批；只合其一则 `verify:domain-ownership-scope` 红；迁移重编号不影响本 CSV 内容（按表名分类），但 U04-001 删除 `is_self_operated` 列不改表集合。
- 工程缺陷：是。建议登记：是。最小处置：第 13 段 CSV 与第 18 段工具同一 PR，合入后重跑 `docs:domain-ownership-scope` 并 `--check`。

### CX-U13-003 · 技术性（文档与定稿代码不一致）· 发布前须修：是

- 位置：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:14`「普通扣除合计按当月固定工资与奖金的比例封顶」（把 U05-004 冲突口径写成规格；HTML BSM L1458 总薪资＝基础工资、各项补贴、绩效工资与奖金之和）；`:7`「追加 V400—V409」（U10-001 重编号）；`docs/r5-spec-T9-staff-device-android-dev-design-20260618.md:29`「公司工作手机沿用 `terminal_devices.staff_id` 对应员工，重新登记保持该对应关系」（U07-001：76 文件内无任何领用/分配路径写入 `staff_id`，只有保留）；`:11` 公司员工自助注册路径（随 U05-003 设备证明补充）。
- 判据：GEN-016、BSM-087、PGM-032；依据类型：HTML 原句 + 定稿对照。
- 业务影响：设计文档把待修口径固化，发布者按文档实现会复现 U05-004/U07-001。
- 工程缺陷：是。建议登记：是。最小处置：随 U05-004/U07-001/U10-001/U05-003 修复同批改这四处措辞；不另写新规则。

## 7. 观察（登记、不入队）

- OBS-1：`security-baseline-latest.json` / `findings-register-v1.md` / `formal-security-baseline-v1.md` 记录 730 个 operation、OpenAPI SHA `f8de4f8c…`（与材料 openapi json 实际 SHA 一致）；第 14 段候选（删除 `setMerchantSelfOperated`、补失败事件、终端类型）落地后 SHA/计数变化，须重跑 `verify:security-baseline` 再生成三件（随第 14 段处置，不另立须修）。
- OBS-2：证据 JSON 声称「160 可丢弃副本 r1-predeploy-final.log 中 verify:security-baseline 实际通过」，该日志不在 76 文件内（0 命中），本席不能复核；`verified_at 2026-09-08T11:59:55Z`。
- OBS-3：CSV 每行常量 `file_version=V0.01`（生成器写死）是版本铁律的机械落地，对盘点无信息量（ponytail）；登记不入队。
- OBS-4：T9 文档明写「实机 NFC、Keystore、充电设施触发及应用进程退出后的硬件上报尚未验证」，与 U01-008（无 Android 测试）一致，属诚实陈述。
- OBS-5：T1「首次私人手机绑定直接生效，换机及异常后复核保留」为行为变更的文档化（HTML 对首绑审批沉默，U05/U06 已核）。
- 技能：`using-superpowers`、`verification-before-completion`（overlay 门禁实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。7 文件全文三线审计、60 项判据逐项核对；发现 3 项技术性须修（CX-U13-001 隔离规格块落后于 main V410、CX-U13-002 CSV 与生成器须同批、CX-U13-003 设计文档固化待修口径）、5 项观察；无添加项；无 HTML 修改申请；overlay 内三门禁绿；未修源、未提交/推送。本报告落盘前未读取 `U13-b1-grok-raw.md` 任何正文。
---

# U13 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.68「当前 U13 B2-r1 整批候选」U13-001…009；modules/U13-b1-grok-raw.md V0.01（52906bytes SHAc92778a5544b7df47c613e995c058811efcdaa2efc6a2d5bde98db2363a3f173）；本文件 B1 段（22836bytes SHA3c8149cd5d710b6f8c0fb8b42f4ef8acd15019cbb6c500d0e65d406d48490803）。2026-09-10T03:26:13Z
- 席甲 B2 核实（只读，见 ledger V2.81 U13 节）：T1 `:7/:11/:12/:14/:15/:130/:179`、T9 `:11` 原文与 Grok 引文逐字一致；BEQR L240「员工状态待入职、在职、试岗、停岗、离职」（全文「试岗」1 处）；安全基线 `:44` public 7；隔离规格 `git diff 242f4872 HEAD` 仅 V399→V410、`git diff 242f4872 e28dad05` 仅标题与 V399→V409；U05-002/004/003/006、U10-001 定稿处置与候选一致。

## 勘误（席甲 B1 段）

- 席甲 B1 漏判：T1 `:14` 事假句无「当期应发」被减数（U13-003）、T1 `:12`「试用、停职」与 BEQR L240 不符（U13-005）、T1 `:15` 柜机双身份写成设计（U13-006）、T1 `:130` 表与 `:11` 口径相反（U13-007）、T1 `:179`「断言到 V066」（U13-008）。
- 席甲 CX-003 把 T9 `:29`「工作手机沿用 staff_id」列为措辞缺陷有误：该句为正确合同，缺陷在实现 U07-001；B2 移为 U13-009 观察。席甲 B1 票 FAIL 不变，缺陷计数由 3 升为 8。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 隔离规格 V409/V410、CSV 表头与 4 新表、安全产物 730/7 对 720/5、T1:7 均为生成物/门禁事实；处置只引用既有 docs:* 生成器与 verify:* 门禁 |
| 002 | PASS | T1:14 封顶句与 BSM L1458 总薪资定义相反；随 U05-004 改句 |
| 003 | PASS | T1:14 事假句缺被减数；BSM L1450 明文当期应发；随 U05-002 |
| 004 | PASS | T1:11/T9:11/public 7 把无设备证明的公开注册写成设计；随 U05-003/U01-001 改句 |
| 005 | PASS | BEQR L240 原文试岗/停岗；T1 用近义错词；UI 已正确 |
| 006 | PASS | T1:15 并列双身份与 PGM L582 自动记录相悖；随 U05-006 改句 |
| 007 | PASS | 同文件 :11 与 :130 相反属实 |
| 008 | PASS | :179 V066 钉与 :7 及 U10-001 矛盾属实 |
| 009 | PASS | 观察归并未改写双方原意；T9:29 移出有据（Grok §8.15） |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 7 条，定稿以此为准）

1. U13-001 处置读作「以 main 445a49f4 为底叠入第 18 段生成器后跑 `docs:data-isolation-spec` / `docs:domain-ownership-scope`」，不得用无 `file_version` 列的当前 main 脚本把 CSV 生成回 328 表。
2. U13-001 处置格补「与 U05+U06+U07+U09+U10+U11+U12+U14+U15+U18 同一 PR」。
3. U13-002 冻结判据补 BSM-120；分类仍冲突。
4. U13-003 冻结判据补 BSM-120；分类仍冲突。
5. U13-004 同根：U01-001 为私人手机通道/终端身份，U05-003 为设备公钥/指纹；处置两句并行，不得读成只改通道。
6. U13-009 之席甲 OBS-1「并入 001」指重生面归 001 须修，不是把安全三件重生拿掉；009 只保留「未发布 SHA 与未发布 OpenAPI 一致、r1 日志不在 76 文件」的观察。
7. U13-009 第 4 条「不计」只覆盖缺 package.json 的 JSON.parse，不覆盖 CMD-SEC-OVL 的 count 720 mismatch。

## 结论

席甲 B2-r1 票：**PASS**（候选 U13-001…009 成立；发布前须修「是」8 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 78905bytes SHA8a5b859249f5f6e948b59b5853251fe04c1bbe4ca28705b1ba9c362f491e0eab，前 52906bytes 原字节不变）。


## U13 B2-r2 单条复核 U13-010（席甲：Claude Fable 5.1，作者自审票）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 与 V0.02 B2-r1 原字节保留；不写独占锁哈希字段行）
- 输入：findings.md V1.74（682231 bytes SHA 9eccbd45d887075336ded8d857b7d375de2dbc1463aa18fca23ffeb4ab0d1420）顶部候选 U13-010；modules/U13-b1-grok-raw.md V0.03（Grok B2-r2 单条票，本席落盘前未读，落盘后全文读取）。
- 席甲独立核实（U-FIX B3 期间只读实测，见 ledger V2.91 进度记录）：纯净 main 445a49f4 副本重生 → 两件 md 0 处 NAS 提及、`--strict` 4 条红；生成器 `productionCredentialLedgerVerifiable` 16 项条件中唯一 FALSE 为 runbook `/reminder_days/`（席甲候选误写「13 个条件」，勘误）；`git log -S reminder_days` 显示 b969b8cf 把该行从 runbook 挪到 `docs/scope-wide-coding-rule-canonical-V0.01.md`；`verify:security-baseline` 在 `verify:predeploy` 链上且 CI 跑该链。
- 采纳 Grok 非阻断修正 6 条：①「13 个条件」改「16 项中唯一 FALSE 为 runbook /reminder_days/」；②重生丢 NAS 还叠加 `nasRestoreDrillEvidenceReady=false`（证据 JSON flyway_version 039 vs 生成器钉 018），只扩 reminder_days 读取面后走 `nasRestoreDrillEvidenceFromNas` 初值分支即可 `--strict` 绿，不并修 flyway 钉、不新开 ID；③「main 已提交版本 2 处」指 baseline 2 处、findings-register 另 1 处；④`docs:security-baseline` 只写两件 md，`security-baseline-latest.json` 另一路径（U13-001 三件之一，已随 732 同步）；⑤同 PR 集合补 U15；⑥ledger 任务标签 V2.90 与文件版本 V2.91 之差为并行进度记录写入，候选正文以 findings V1.74 为准。
- 归属：根因文件 `tools/generate-formal-security-baseline.mjs` 属 main 模块（Grok 指 M419），本轮不重开 main 模块，挂 U13-010 作为 U13-001 执行面。

| ID | 票 | 依据 |
|---|---|---|
| U13-010 | PASS | 技术性（main 既有生成器/校验器漂移）、工程缺陷是、发布前须修是；最小处置只扩生成器读取面，不改校验器/runbook/HTML，与 b969b8cf 搬家意图一致 |

## 结论

席甲 B2-r2 单条票：**PASS**（U13-010 成立；添加 0；questions 0；无 HTML 修改申请）。本票为作者自审票，以 Grok B2-r2 票合取；Grok FAIL 具否决权。双 PASS 即定稿并入 U-FIX B3 批；U13 状态保持 REVIEWED-FAIL。
