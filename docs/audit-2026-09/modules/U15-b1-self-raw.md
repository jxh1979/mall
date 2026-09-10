CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U15 未发布材料第 15 段（完整接口定义）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T04:09:18Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `8dbf43ebcee85135c191481fd942a9df375bf06f`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 15 段，1 文件 37017 行（openapi-p0.yaml 37017），main 主归属 M392；适用判据＝映射行并集 18 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。OVERSIZE-SINGLE-FILE：1315326 字节 YAML 不逐行通读；覆盖方法＝相对基线 diff 全读（+712/−180）+ 仓库门禁 `verify-openapi-contract.mjs` 的 json/yaml 深比较（/tmp/u14_contract_check.log）；重复锚点 `&a1` 为基线既有（PyYAML 严格解析基线亦报错）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U15-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U14 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U15-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用同第 14 段（U04-001、U01-001/U02-001、U05-011、U05 定稿）；本段与第 14 段为同一契约的两种序列化，findings 以第 14 段为主、本段登记同根。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/openapi/openapi-p0.yaml | 37017 | 1315326 | 93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d | 全读 |

## 2. 可执行证据原文（/tmp/u15_b1_run.log）

```
START=2026-09-09T21:12:33Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
docs/openapi/openapi-p0.yaml | 37017 | 1315326 | 93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d
=== manifest entries ===
docs/openapi/openapi-p0.yaml 37017 1315326 93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d
=== baseline -> unpublished tree (numstat) ===
712	180	docs/openapi/openapi-p0.yaml
=== main HEAD vs baseline (blob) ===
docs/openapi/openapi-p0.yaml baseline=161f4c1aa1 head=161f4c1aa1 unpublished=1ab4fd6009 main_changed_since_baseline=no
=== segment probes (/tmp/u15_probes.sh) ===
-- yaml: version line, new paths, anchors (pre-existing in baseline)
4:  version: V0.14
34:  /api/v1/staff/shifts/{id}/history:
74:  /api/v1/staff/settings/shift-visible-days:
163:  /api/v1/staff/settings/payroll-penalty-cap:
279:  /api/v1/staff/self-registrations:
347:  /api/v1/staff/registration-companies:
379:  /api/v1/staff/profiles/{id}/employment-status:
442:  /api/v1/staff/locker-events:
497:  /api/v1/platform/merchants/{merchant_id}/self-operated:
anchors_material=2 anchors_baseline=2 aliases_material=25
-- quoting/indent re-dump evidence: count of lines changed only by quote style
46
-- yaml write ops with failure none / app terminal
12
38-      operationId: listStaffShiftHistory
78-      operationId: getStaffShiftVisibility
109-      operationId: setStaffShiftVisibility
167-      operationId: getStaffPenaltyCap
198-      operationId: requestStaffPenaltyCap
283-      operationId: submitStaffSelfRegistration
351-      operationId: listStaffRegistrationCompanies
383-      operationId: setStaffEmploymentStatus
446-      operationId: recordStaffLockerScan
22625-      operationId: upsertStaffShift
22733-      operationId: batchUpsertStaffShifts
22844-      operationId: punchStaffAttendance
283-      operationId: submitStaffSelfRegistration
351-      operationId: listStaffRegistrationCompanies
-- repo contract gate (json vs yaml deep-equal) in overlay
OpenAPI contract verification passed.
verify_openapi_contract_exit=0
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

- 对象口径：未发布提交 03acdb98 的只读输入副本第 15 段（1 文件 `docs/openapi/openapi-p0.yaml` 37017 行 1315326 字节，OVERSIZE-SINGLE-FILE），不是 main；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05 -- docs/openapi/openapi-p0.yaml`（+712/−180）：与第 14 段 JSON 同一契约的 YAML 序列化——同样的 9 条新路径与 8 处修订；另有 46 行仅因重新序列化改变引号/缩进风格（`"#/components…"`→`'#/components…'`、列表缩进）。
- 覆盖方法（超大文件如实声明）：不逐行通读；diff 全读 + 仓库门禁 `tools/verify-openapi-contract.mjs` 在 `/tmp/u-overlay` 内做 json/yaml 深比较退出 0（§2）+ grep 统计（`x-audit-event-failure: none` 12 处含 4 个 GET、允许 `app` 的员工口 2 个、`&a1` 锚点 2 处与基线相同）。
- 第一性原理线／顺从式线／对抗式线：同第 14 段；本段独有对抗点：重新序列化是否改变语义（门禁深比较证明未变）；重复锚点 `&a1`（第 826/7759 行）是否为材料引入（否：基线同样 2 处且 PyYAML 严格解析基线同报错，仓库门禁自带解析器通过）。
- 只读依赖追流：第 14 段（同一契约，findings 以第 14 段为主，本段登记同根）。
- 可执行验证：门禁绿（§2）；本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- main 自基线未改本文件 → 快进合并；与第 14 段 JSON 深比较一致。

## 5. 18 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001/003/004/007/011/017 | 同第 14 段 | 同一契约：自营端点（已答删除）、失败事件 none、`app` 终端、写口不幂等（CX-U15-001 登记同根） |
| GEN-002 | 通过 | json/yaml 深比较一致 |
| GEN-005/006/009/010/015 | 通过 | 同第 14 段 |
| GEN-008 | 通过（登记） | 重新序列化 46 行仅风格变化；锚点/别名 2/25 与基线相同（OBS-1） |
| GEN-012/014 | 不适用 | 契约 |
| GEN-013 | 通过 | 门禁绿 |
| GEN-016 | 通过（登记） | `info.version: V0.14`（同第 14 段 OBS-2） |
| GEN-018 | 通过 | diff、grep、门禁输出 |

## 6. 本次 Findings（临时 ID CX-U15-00N；正式 ID 由 B2 分配）

### CX-U15-001 · 同根登记（第 14 段 CX-U14-001…004）· 发布前须修：是

- 位置：`docs/openapi/openapi-p0.yaml` 对应节：8 个写口 `x-audit-event-failure: none`（`:109/:198/:283/:383/:446/:22625/:22733/:22844` 附近）、`submitStaffSelfRegistration`/`listStaffRegistrationCompanies` `x-terminal-allowed` 含 `app`（`:283/:351`）、`/api/v1/platform/merchants/{merchant_id}/self-operated`（`:497-558`）、`setStaffEmploymentStatus`/`setStaffShiftVisibility` `x-idempotency-required: false`（`:377/:103`）。
- 判据：同第 14 段；依据类型：diff + 门禁深比较（两文件等价）。
- 工程缺陷：是。建议登记：是（同根，不另编处置）。最小处置：随第 14 段处置修 JSON 后按仓库流程同步 YAML（或反之），提交前 `verify-openapi-contract` 绿。

## 7. 观察（登记、不入队）

- OBS-1：重新序列化把双引号改单引号、列表缩进由 8 空格改 6 空格（46 行），语义不变（门禁深比较）；重复锚点 `&a1` 两处为基线既有（PyYAML 严格模式对基线亦报错；仓库门禁自带解析器通过），归 M392 既有。
- OBS-2：本段与第 14 段为同一契约两种序列化，B2 建议以第 14 段定稿 ID 为主、本段只登记同根，避免重复入队。
- 技能：`using-superpowers`、`verification-before-completion`（门禁实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**（同根第 14 段）。超大文件按「diff 全读 + 门禁深比较 + grep 统计」覆盖（如实声明未逐行通读）；18 项判据逐项核对；1 项同根登记（CX-U15-001）、2 项观察；无新增独立缺陷；无 HTML 修改申请；未修源、未提交/推送。本报告落盘前未读取 `U15-b1-grok-raw.md` 任何正文。
---

# U15 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.72「当前 U15 B2-r1 整批候选」U15-001…009；modules/U15-b1-grok-raw.md V0.01（51956bytes SHA86dfb24bb9ffa73df1e3ff8c30ddd9187f7cdb5e85d24a743e6d3f1316f82292）；本文件 B1 段（11125bytes SHAddf6b4710acc5b1886f634c6ae604193195e372f9c46d1c93295e86cf4ecf55f）。2026-09-10T04:21:28Z
- 席甲 B2 核实（只读，见 ledger V2.87 U15 节）：Grok CMD-PARSE yjPathDiffs=0/yjCompDiffs=0（yaml 与 json 同树深等）、CMD-YAMLONLY 39 处与 U14 CMD-JSONONLY 对称、CMD-MATRIX-YAMLONLY EXIT 0（矩阵只读 json）；yaml `:80/:442-456/:119/:393/:33334-33375/:27308-27320` 与 U14 对应节同义；U14-001…008 定稿处置逐条适用；`apply-openapi-p0-examples.mjs` 同写 json 与 yaml 为既有生成路径。

## 勘误（席甲 B1 段）

- 席甲 B1 以一条同根登记（CX-U15-001）覆盖第 14 段四项，漏判：只叠 yaml 时矩阵 `--check` 仍绿的漏检窗（U15-001）、GET 可见天数「未配置的上下限为空」（U15-004）、locker 双身份与双值成功事件（U15-005）、注册 schema/example 互斥（U15-007）；B2 按 U14 逐条拆分登记。席甲 B1 票 FAIL 不变。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 39 处深差与矩阵只读 json 的盲区均为可复现门禁事实；处置只引用既有生成器 |
| 002 | PASS | 与 U14-002 同一写口的 yaml 序列化；已答删除 |
| 003 | PASS | 与 U14-003 同义；PGM L579/BSM L935 |
| 004 | PASS | 与 U14-004 同义；BSM L1444 |
| 005 | PASS | 与 U14-005 同义；PGM L582 |
| 006 | PASS | 与 U14-006 同义 |
| 007 | PASS | 与 U14-007 同义 |
| 008 | PASS | 与 U14-008 同义；12 处 none |
| 009 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 8 条，定稿以此为准）

1. U15-009 补回 Grok §8 第 1 条：712 个未改 operation 与 main 深等，英文 summary 归 M392 既有、本段未加重。
2. U15-009 席甲 OBS-2「以第 14 段 ID 为主」不取消 U15-001…008；本批已拆分为八条同根登记，处置统一走 json→生成器。
3. U15-003 同根补 U13-004。
4. U15-001 单元格补「以 main 445a49f4 为底」「与 U14+U16+U17 同一 PR」及脚本名 `docs:openapi-examples` / `docs:operation-matrix` / `docs:openapi-closure` / `docs:security-baseline`；`apply-openapi-p0-examples.mjs` 为既有 json→yaml 同写入口。
5. U15-005「随 U14-005」读作 U05-006/GR-005 的「kiosk 设备签名/设备令牌认证单端上报」，不是另发令牌。
6. U15-003 保留完整句：私人手机通道、无指纹 4xx、公司列表若仍需匿名发现须另证来源校验、不能作为无绑定注册的前置。
7. U15-001 补建议句：基线三件重生时同时钉 yaml SHA，或写明 `verify:openapi` 已覆盖 yaml；不另开 ID。
8. U15-004 同根补 U06-005。

## 结论

席甲 B2-r1 票：**PASS**（候选 U15-001…009 成立；发布前须修「是」8 / 「否」1；添加 1（U15-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 78823bytes SHA91998882cc206f5f5ec50ba0dfde9c1d5287a19f7303dca63ce343b323cd773b，前 51956bytes 原字节不变）。
