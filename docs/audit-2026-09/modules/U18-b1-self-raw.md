CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U18 未发布材料第 18 段（文档生成与检查工具）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T06:22:11Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `0ba423058adea5af61b1b8b550da57810611d58d`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 18 段，3 文件 492 行（generate-domain-ownership-scope-audit.mjs 230、generate-openapi-operation-matrix.mjs 193、verify-pii-encryption.mjs 69），main 主归属 M029/M394/M053；适用判据＝映射行并集 18 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。可执行验证：在 `/tmp/u-overlay` 内运行 `generate-domain-ownership-scope-audit.mjs --check`、`verify-pii-encryption.mjs`、两个 openapi 生成器（/tmp/u13_18_tools.log）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U18-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U17 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U18-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：第 13 段 CSV（由本段生成器改写，须同批）、第 10 段迁移（隔离分类源）、第 5 段加密写入（PII 门禁登记表）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| tools/generate-domain-ownership-scope-audit.mjs | 230 | 13783 | c364a59c356cece0625d164653bd37c1bf1cb26e6f0b2591becdd0f2b183798b | 全读 |
| tools/generate-openapi-operation-matrix.mjs | 193 | 13858 | b05d7137b38cd7cb173e1d7a422e2cd3c6dd8effaa306a3ef104a5525c4fb669 | 全读 |
| tools/verify-pii-encryption.mjs | 69 | 5532 | 17a5d59856cfca619aa26295dadee86fc13dc584291901e5dab34134e6a38159 | 全读 |

## 2. 可执行证据原文（/tmp/u18_b1_run.log）

```
START=2026-09-09T21:12:34Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
tools/generate-domain-ownership-scope-audit.mjs | 230 | 13783 | c364a59c356cece0625d164653bd37c1bf1cb26e6f0b2591becdd0f2b183798b
tools/generate-openapi-operation-matrix.mjs | 193 | 13858 | b05d7137b38cd7cb173e1d7a422e2cd3c6dd8effaa306a3ef104a5525c4fb669
tools/verify-pii-encryption.mjs | 69 | 5532 | 17a5d59856cfca619aa26295dadee86fc13dc584291901e5dab34134e6a38159
=== manifest entries ===
tools/generate-domain-ownership-scope-audit.mjs 230 13783 c364a59c356cece0625d164653bd37c1bf1cb26e6f0b2591becdd0f2b183798b
tools/generate-openapi-operation-matrix.mjs 193 13858 b05d7137b38cd7cb173e1d7a422e2cd3c6dd8effaa306a3ef104a5525c4fb669
tools/verify-pii-encryption.mjs 69 5532 17a5d59856cfca619aa26295dadee86fc13dc584291901e5dab34134e6a38159
=== baseline -> unpublished tree (numstat) ===
11	1	tools/generate-domain-ownership-scope-audit.mjs
3	1	tools/generate-openapi-operation-matrix.mjs
4	0	tools/verify-pii-encryption.mjs
=== main HEAD vs baseline (blob) ===
tools/generate-domain-ownership-scope-audit.mjs baseline=6fc05e2cd9 head=6fc05e2cd9 unpublished=7d5be6ba36 main_changed_since_baseline=no
tools/generate-openapi-operation-matrix.mjs baseline=76bda7d416 head=76bda7d416 unpublished=40752625cc main_changed_since_baseline=no
tools/verify-pii-encryption.mjs baseline=13ce9dd5b8 head=13ce9dd5b8 unpublished=bb5a6785e2 main_changed_since_baseline=no
=== segment probes (/tmp/u18_probes.sh) ===
-- domain-ownership generator: new rules / global table / file_version column
23:  { pattern: /^staff_shift_revisions$/, source: "review_id/shift_id", path: "review_id -> supervisor_reviews; published shift_id -> staff_shifts -> staff_pro
24:  { pattern: /^staff_locker_records$/, source: "staff_profile_id", path: "staff_profile_id -> staff_profiles -> current authenticated employee; cabinet and s
71:  "staff_penalty_cap_revisions",
148:  if (table === "staff_policy_settings") return {
201:      file_version: "V0.01",
205:  const headers = ["table_name", "scope_type", "scope_source", "scope_path", "multi_complex_note", "file_version", "status"];
-- CSV/tool pairing: main tool + material CSV would fail --check? (headers differ)
table_name,scope_type,scope_source,scope_path,multi_complex_note,status
table_name,scope_type,scope_source,scope_path,multi_complex_note,file_version,status
-- verify-pii registry additions + JdbcStaffRepository encrypt wrapping
14:  { table: "staff_profiles", column: "name", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
15:  { table: "staff_profiles", column: "phone", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
16:  { table: "staff_profiles", column: "id_doc", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
891:                .addValue("name", piiCipher.encrypt(profile.name()))
892:                .addValue("phone", piiCipher.encrypt(profile.phone()))
893:                .addValue("id_doc", piiCipher.encrypt(profile.idDoc()))
-- operation matrix generator: version from doc first line
5:const matrixPath = "docs/p0-openapi-operation-acceptance-matrix-v0.1.md";
83:const documentVersion = fs.readFileSync(matrixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";
84:const document = `# P0 OpenAPI Operation 验收矩阵 ${documentVersion}
184:  const current = fs.existsSync(matrixPath) ? fs.readFileSync(matrixPath, "utf8") : "";
-- gates run in overlay (from tools log)
dis_check_exit=0
dos_check_exit=0
DOMAIN_OWNERSHIP_SCOPE_PASS: 332 tables classified
matrix_gen_exit=0
appendix_gen_exit=0
pii_exit=0
PII_ENCRYPTION_PASS: 16 个 PII 列均经 piiCipher.encrypt 落库
-- package.json gates referencing these tools (main)
26:    "docs:operation-matrix": "node tools/generate-openapi-operation-matrix.mjs",
27:    "verify:operation-matrix": "node tools/generate-openapi-operation-matrix.mjs --check",
28:    "docs:openapi-closure": "node tools/generate-openapi-contract-closure-appendix.mjs",
29:    "verify:openapi-closure": "node tools/generate-openapi-contract-closure-appendix.mjs --check",
91:    "docs:domain-ownership-scope": "node tools/generate-domain-ownership-scope-audit.mjs",
92:    "verify:domain-ownership-scope": "node tools/generate-domain-ownership-scope-audit.mjs --check",
97:    "verify:pii-encryption": "node tools/verify-pii-encryption.mjs",
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

- 对象口径：未发布提交 03acdb98 的只读输入副本第 18 段（3 文件 492 行：`tools/generate-domain-ownership-scope-audit.mjs` 230、`tools/generate-openapi-operation-matrix.mjs` 193、`tools/verify-pii-encryption.mjs` 69），不是 main；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05`（域归属生成器 +11/−1：`staff_shift_revisions`/`staff_locker_records` 继承规则、`staff_penalty_cap_revisions` 全局表、`staff_policy_settings` 直接分类、CSV 新增 `file_version` 列；矩阵生成器 +3/−1：版本从文档首行读；PII 门禁 +4/0：登记 `staff_profiles.name/phone/id_doc` 三列）。
- 第一性原理线：三个工具是预部署门禁的实现面（`verify:domain-ownership-scope`、`verify:operation-matrix`、`verify:pii-encryption`）。审计点：分类规则是否与迁移/隔离模型一致（柜记录经档案继承、上限修订全局、参数表按 scope_id 直接）；PII 登记是否与第 5 段加密写入对应（`JdbcStaffRepository:891-893` 三处 `piiCipher.encrypt`）；门禁在 overlay 内是否绿（§2：三者退出 0）。
- 顺从式线：18 项通用判据逐项（§5）。对抗式线：`staff_penalty_cap_revisions` 归全局是否越权（上限为全平台参数，修订留痕不含租户数据，合理）；`file_version` 列使每行携带常量并触发 CSV 全量重写（第 13 段同批）；`staff_policy_settings` 分类把 `scope_id` 视为经营主体键（与 U06 候选「写读键推导不一致」同根，分类本身按写入约定）。
- 只读依赖追流：第 13 段 CSV、第 10 段迁移、第 5 段仓储、第 17 段附录生成器。
- 3 文件全文逐行读完（§1）；diff 逐块读完；可执行验证：overlay 内 `generate-domain-ownership-scope-audit.mjs --check` 退出 0（332 表）、`verify-pii-encryption.mjs` 退出 0（16 列）、矩阵生成器输出与副本一致（§2）。本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- 三文件 main 自基线未改 → 快进合并；域归属生成器与第 13 段 CSV 须同批（表头 6 列→7 列）。

## 5. 18 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 新表须分类、新 PII 列须入门禁登记 |
| GEN-002 可执行正确性 | 通过 | 三门禁 overlay 内绿 |
| GEN-003 测试有效性 | 通过 | 门禁自身即检查（`--check`） |
| GEN-004/005/007/009/012/014/015 | 不适用 | 工具 |
| GEN-006 数据完整性 | 通过 | 分类与迁移列一致 |
| GEN-008 接口一致性 | 通过（登记） | CSV 表头变更须与第 13 段同批（U13 候选） |
| GEN-010 安全 | 通过 | PII 登记补齐三列 |
| GEN-011 审计证据 | 通过 | 门禁输出可复核 |
| GEN-013 构建 | 通过（登记） | 只合工具不合 CSV 则 `--check` 红（U13 候选同批） |
| GEN-016 文档 | 通过（登记） | 每文件版本行；`file_version` 常量列（OBS-1） |
| GEN-017 分类 | 通过 | 无添加 |
| GEN-018 证据 | 通过 | diff、门禁输出 |

## 6. 本次 Findings（临时 ID CX-U18-00N；正式 ID 由 B2 分配）

无新的工程缺陷。CSV/工具同批的处置已在第 13 段候选登记；本段工具改动自洽且门禁绿。

## 7. 观察（登记、不入队）

- OBS-1：CSV 每行 `file_version: "V0.01"` 常量列（`:201/:205`）——版本铁律对生成物的机械落地，使 333 行全量重写；对盘点无信息量（ponytail），登记不入队。
- OBS-2：`staff_policy_settings` 分类注释「shift_visible_days: scope_id = employer business entity」按写入约定描述；U06 候选指出员工读取键推导不同，分类不受影响。
- OBS-3：矩阵生成器缺省版本 `V0.01` 回退（第 17 段 OBS-1 同）。
- 技能：`using-superpowers`、`verification-before-completion`（门禁实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**PASS**。3 文件全文三线审计、18 项判据逐项核对；无新增工程缺陷、无添加项、无 HTML 修改申请；三门禁 overlay 内绿；同批要求已在第 13 段登记；未修源、未提交/推送。本报告落盘前未读取 `U18-b1-grok-raw.md` 任何正文。
---

# U18 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.80「当前 U18 B2-r1 整批候选」U18-001…005；modules/U18-b1-grok-raw.md V0.01（47569bytes SHA62a22c72d3c39cdea561e82e577be26cbd6e5f4f4a10037d54a5ced50a1952d9）；本文件 B1 段（13947bytes SHA68604104ad4448534a3d57090541f5304b0d01c2c8d9740f5a61d779c26bf715）。2026-09-10T17:35:31Z
- 席甲 B2 核实（只读，见 ledger V3.00 U18 节）：域归属生成器 `:23-24/:71/:148-152/:201-205`、PII 门禁 `:14-16`、矩阵生成器 `:83-84/:175` 原文如引；main `JdbcStaffRepository` 仍 `addValue("name", profile.name())` 与 `phone_hash`/`id_doc_hash`；HTML PGM L870 加密存放句属实；三路径 main blob＝基线；Grok 四种叠法结论与席甲 overlay（未发布树自洽绿）不矛盾——席甲 B1 只跑了自洽叠法，未跑「只叠三工具到 main」叠法。

## 勘误（席甲 B1 段）

- 席甲 B1 票 PASS 被 Grok FAIL 否决。席甲以「同批要求已在第 13 段登记」判本段无新缺陷，漏判：只叠本段三工具到 main 即域归属/PII 两门禁红，是本段自己的发布路径缺陷（001）；矩阵生成器版本回读/§7 写死/现值入历史行是 U17-008 的根因文件（002，席甲 OBS-3 误作观察）；域归属生成器 `file_version` 列与员工表规则须与 CSV/迁移同批（003，OBS-1/OBS-2 误作观察）；PII 登记三列须与 U05 加密写口/U10 V403 同批（004，席甲未提）。席甲 B1 段原字节不改；本追加段票以 Grok 四项为准。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 只叠三工具两门禁红为可复现事实；处置只引用既有 `docs:domain-ownership-scope`/`docs:operation-matrix`，禁止整树覆盖 |
| 002 | PASS | `:83-84/:175` 属实；与 U17-007 同口径或支，不发明第三套版本政策 |
| 003 | PASS | 表头 6→7 列与四张员工表规则须与 CSV/迁移同批属实；分类句为细化不判缺陷 |
| 004 | PASS | PGM L870 上位、main 明文写入属实；细化项的缺陷在发布不同批，须修是 |
| 005 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断修正 8 条，定稿以此为准）

1. U18-003 来源格改为「Grok GR-U18-003 + 席甲 OBS-1（升：列/同批）」；席甲 OBS-2 的分类句未升缺陷，只并入「分类句为细化、不判缺陷」；「常量列无信息量」留 005 观察。分类/须修不变。
2. U18-005 OBS-2 改写为「U06-001 须修写读键（已定稿须修项），分类按写入约定、不升本段缺陷」；不得写成「已修」，U-FIX worktree 不是已发布。
3. U18-005 补席甲 OBS-3 升 002 后的余量「缺省 V0.01 静默回退」观察（与 U17-011 同口径），与 Grok §8.6「无 existsSync（铁律 6 不修）」分列，不并成一条；不升队。
4. U18-005 第 3 条补回模板全句「若与业务 PRD 冲突，以 OpenAPI、统一口径基准、权限矩阵、OpenAPI 追溯附录和本文为准」；分类/须修不变，不得删该句。
5. U18-002 事实句：变更表各行写死 V0.1 正确；写入现值的模板行是 `:175`；根因文件行号以 `:83-84` / `:166-177` / `:175` 为准（产物面 U17-008 已定稿）。
6. U18-004「重编号后 V414」为 V400–V409 保持内部顺序时 V403 的预期号，以 U10 实际重编号为准，本段不另编迁移号。
7. U18-001 同 PR 集合（U05/U10/U13/U14/U17）补写明本段 002/003/004 同一 PR；禁止整树覆盖保持。
8. U18-005 第 1 条「三文件 main＝基线」即 Grok「对抗式覆盖不会回退这三路径上的 M009–M012」，不得读成整包覆盖未发布树也无冲突（那是 001 的兄弟文件面）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U18-001…005 成立；发布前须修「是」4 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 73326bytes SHA660774b088cbce3c17b8c6d719bed9063aaf5189b6620acca647557bfea07d16，前 47569bytes 原字节不变）。
