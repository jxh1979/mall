CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U17 未发布材料第 17 段（接口覆盖说明）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T05:51:21Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `755be9b78a6d1d0e31d90490fb44ac7488dfdf44`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 17 段，2 文件 1112 行（p0-openapi-operation-acceptance-matrix-v0.1.md 822、generate-openapi-contract-closure-appendix.mjs 290），main 主归属 M390/M394；适用判据＝映射行并集 43 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。可执行验证：在 `/tmp/u-overlay` 内以材料自带 `tools/generate-openapi-operation-matrix.mjs` 重生成矩阵并与副本逐字节比较（/tmp/u13_18_tools.log：matrix_same_as_material）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U17-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U16 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U17-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：第 14/15 段 openapi 候选；矩阵与生成器须随契约修正后重生成。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 822 | 224648 | 523a47691e13cb87f1548e84ddcf64cc2b1b7f9648486af4861814262fb7e285 | 全读 |
| tools/generate-openapi-contract-closure-appendix.mjs | 290 | 14368 | 12f7df9e17834230c24e5d58461625d78e5d2bb00725be1a231f693ecd3cac78 | 全读 |

## 2. 可执行证据原文（/tmp/u17_b1_run.log）

```
START=2026-09-09T21:12:34Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 822 | 224648 | 523a47691e13cb87f1548e84ddcf64cc2b1b7f9648486af4861814262fb7e285
tools/generate-openapi-contract-closure-appendix.mjs | 290 | 14368 | 12f7df9e17834230c24e5d58461625d78e5d2bb00725be1a231f693ecd3cac78
=== manifest entries ===
docs/p0-openapi-operation-acceptance-matrix-v0.1.md 822 224648 523a47691e13cb87f1548e84ddcf64cc2b1b7f9648486af4861814262fb7e285
tools/generate-openapi-contract-closure-appendix.mjs 290 14368 12f7df9e17834230c24e5d58461625d78e5d2bb00725be1a231f693ecd3cac78
=== baseline -> unpublished tree (numstat) ===
17	7	docs/p0-openapi-operation-acceptance-matrix-v0.1.md
3	1	tools/generate-openapi-contract-closure-appendix.mjs
=== main HEAD vs baseline (blob) ===
docs/p0-openapi-operation-acceptance-matrix-v0.1.md baseline=464ebe1034 head=464ebe1034 unpublished=b8ba1cbc15 main_changed_since_baseline=no
tools/generate-openapi-contract-closure-appendix.mjs baseline=77e372c8e0 head=77e372c8e0 unpublished=c44fe9c08b main_changed_since_baseline=no
=== segment probes (/tmp/u17_probes.sh) ===
-- matrix header/version + op count line + new rows
# P0 OpenAPI Operation 验收矩阵 V0.03
.scratch/module-recheck-20260908/unpublished-03acdb98/docs/p0-openapi-operation-acceptance-matrix-v0.1.md:20:> 当前 `docs/openapi/openapi-
docs/p0-openapi-operation-acceptance-matrix-v0.1.md:20:> 当前 `docs/openapi/openapi-p0.json` 共 720 个 operation。下表由 OpenAPI 生
745:| staff | `POST` | `/api/v1/staff/self-registrations` | `submitStaffSelfRegistration` | 公司员工本人注册 | `object` | `public` | `public` | `self` |
752:| staff | `POST` | `/api/v1/platform/merchants/{merchant_id}/self-operated` | `setMerchantSelfOperated` | 标注商户是否自营 | `object` | `merchant.pr
753:| staff | `POST` | `/api/v1/staff/locker-events` | `recordStaffLockerScan` | 记录寄存柜扫码存取 | `object` | `staff.profile.self` | `staff-token` |
-- closure appendix generator: version-from-doc change
174:const documentVersion = fs.readFileSync(appendixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";
175:const document = `# P0 OpenAPI 权限、事件、任务追溯收口附录 ${documentVersion}
-- regenerated in overlay == material? (from tools log)
matrix_gen_exit=0
matrix_same_as_material=yes diff_lines=0
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

- 对象口径：未发布提交 03acdb98 的只读输入副本第 17 段（2 文件 1112 行：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 822 行、`tools/generate-openapi-contract-closure-appendix.mjs` 290 行），不是 main；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05`（矩阵 +17/−7：标题 V0.03、720→730、`submitStaffRegistration` 终端/摘要、排班/打卡事件与终端、新增 10 行 operation；生成器 +3/−1：文件版本行、附录标题版本改为从现有附录首行读取 `V\d+\.\d+`，缺省 `V0.01`）。
- 第一性原理线：矩阵由 `tools/generate-openapi-operation-matrix.mjs`（第 18 段）从 openapi 生成，附录生成器是第 16 段附录的来源——审计点同第 16 段：生成一致性（overlay 内重生成矩阵与附录均与副本逐字节相同，§2）与契约缺陷固化。
- 顺从式线：43 项判据逐项（§5）。对抗式线：生成器改动是否改变语义（仅标题版本来源，`--check` 口径不变）；缺省 `V0.01` 回退是否会在首行无版本时静默降版（观察）。
- 只读依赖追流：第 14/15 段契约、第 16 段附录、第 18 段矩阵生成器。
- 2 文件全文逐行读完（§1）；diff 逐块读完；可执行验证：overlay 内 `generate-openapi-operation-matrix.mjs` 与 `generate-openapi-contract-closure-appendix.mjs` 退出 0、输出与副本 sha256 相同（§2）。本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- 两文件 main 自基线未改 → 快进合并；矩阵内容随第 14 段契约修正须重生成。

## 5. 43 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 生成物随契约更新；生成器读版本避免每次改脚本 |
| GEN-002 可执行正确性 | 通过 | 重生成一致 |
| GEN-003 测试有效性 | 不适用 | 文档工具 |
| GEN-004/005/006/007/009/010/012/014/015 | 不适用 | 无实现面 |
| GEN-008 接口一致性 | 通过（登记） | 矩阵固化第 14 段候选行（CX-U17-001） |
| GEN-011 审计证据 | 不通过（登记） | 矩阵事件列 `none` 随契约（CX-U17-001） |
| GEN-013 构建 | 通过 | `verify:operation-matrix --check`/`verify:openapi-closure --check` 口径：overlay 内一致 |
| GEN-016 文档 | 通过（登记） | 版本从文档首行读取；缺省 `V0.01`（OBS-1） |
| GEN-017 分类 | 通过 | 无添加 |
| GEN-018 证据 | 通过 | diff、重生成比对 |
| 其余 M390/M394 映射行（25 项） | 通过／不适用 | 矩阵列（权限/认证/范围/终端/幂等/事件）未被手改 |

## 6. 本次 Findings（临时 ID CX-U17-00N；正式 ID 由 B2 分配）

### CX-U17-001 · 技术性（派生物随第 14 段重生成）· 发布前须修：是

- 位置：矩阵 `:20`（730）、`:305`（`submitStaffRegistration` 终端）、`:627-630`（排班/打卡事件 `none`）、`:745-753`（新增 10 行含 `setMerchantSelfOperated`）。
- 判据：GEN-008、GEN-011、GEN-013；依据类型：重生成比对 + 第 14 段候选。
- 工程缺陷：是。建议登记：是。最小处置：契约修正后 `npm run docs:operation-matrix` 并 `--check`；生成器改动本身可保留。

## 7. 观察（登记、不入队）

- OBS-1：两个生成器改为从现有文档首行读版本、缺省 `V0.01`：首行无版本时会静默回退，属版本铁律的机械实现；`--check` 比较仍以整文为准，不影响门禁。
- OBS-2：矩阵「变更记录」行仍标 `2026-06-12 V0.1 … 总数为 730 个`——把历史行的数字改成现值而未新增变更记录行（生成器模板写死），归 M390 既有范式。
- 技能：`using-superpowers`、`verification-before-completion`（重生成比对附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。2 文件全文三线审计、43 项判据逐项核对；1 项技术性须修（CX-U17-001 随第 14 段重生成）、2 项观察；无添加项；无 HTML 修改申请；overlay 重生成一致；未修源、未提交/推送。本报告落盘前未读取 `U17-b1-grok-raw.md` 任何正文。
---

# U17 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.78「当前 U17 B2-r1 整批候选」U17-001…011；modules/U17-b1-grok-raw.md V0.01（48965bytes SHA41c4f2ec6068630d81e9524e26d7fce695ff3907a8889652cbaefc8947e563f6）；本文件 B1 段（11647bytes SHA21485c63a5a00a82decbc1d2f1a792854f2f6e9784c55312f5ba74437c5d7b23）。2026-09-10T06:06:03Z
- 席甲 B2 核实（只读，见 ledger V2.96 U17 节）：矩阵 `:1/:20/:628/:629/:631/:744-753/:821` 原文如引（`:752` setMerchantSelfOperated、`:746/:748` 幂等「否」、`:753` 双值名、`:628/:629/:750` 成功事件 evaluated；main `:628-629` 为 `staff.shift.saved`）；生成器 `:174` 版本回读缺省 V0.01、`:268-274` §9 写死 V0.1、`:45-65` taskGroup 无 staff 分支缺省 `P0-待归类`；main 生成器标题写死 V0.1 属实；两文件 main blob＝基线。

## 勘误（席甲 B1 段）

- 席甲 B1 以一条 CX-U17-001「派生物随第 14 段重生成」覆盖，漏判：002/003/004/005/006/010 应逐项同根登记；OBS-1「版本回读为机械实现」误判——内容变化不入账是本材料引入的版本缺陷（007）；OBS-2「变更记录行写现值归 M390 既有」误判——标题 V0.03 与 §9 仅 V0.1 由本材料把版本改为从现文件回读引入（008，根因文件属第 18 段）；taskGroup 无 staff（009）席甲未提。席甲 B1 票 FAIL 不变。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 只叠矩阵 `--check` 红为可复现门禁事实；处置只引用既有 `docs:operation-matrix` |
| 002 | PASS | 矩阵收纳 U04-001 已答删除写口；随 U14-002 重生 |
| 003 | PASS | public 两口、含 app、无设备字段；同根 U14-003 |
| 004 | PASS | 12 行 none、AC-OP-006 自相矛盾；随 U14-008 |
| 005 | PASS | 双值字面量属实；随 U14-005 |
| 006 | PASS | 三口与 evaluateSupervisorReview 同名属实；随 U16-006 |
| 007 | PASS | `:174/:268-274` 属实；或支处置与 GR-007 一致，不发明第三套版本政策 |
| 008 | PASS | `:1/:821` 属实；产物面登记、根因交第 18 段生成器 |
| 009 | PASS | `:45-65` 无 staff 分支属实；只加 staff 映射，不扩基线既有缺项 |
| 010 | PASS | `:746/:748` 幂等否属实；随 U14-006 |
| 011 | PASS | 观察归并未改写双方原意；HTML 入口张力只登记 |

## 勘误与采纳（接受 Grok B2-r1 非阻断修正 10 条，定稿以此为准）

1. U17-008 事实句改写为：矩阵标题 V0.03，变更表实有 6 行、各行仍为 V0.1，且 `:821` 历史行写入现值 730（`:822` 附录闸口行仍在）；不再写「只有一行」。
2. U17-008 补明：插值 `rows.length` 进 2026-06-12 历史行在 main 生成器模板已有（main 写 720）；本材料新引入的是标题从现文件回读导致 V0.03 与变更表 V0.1 分叉。须修仍是（产物面已分叉），分类/根因文件（第 18 段生成器）不变。
3. U17-001 同 PR 集合补本段 007/009（附录生成器修正）；脚本名为既有 `docs:operation-matrix` / `verify:operation-matrix`；禁止整树覆盖保持。
4. U17-011 第 3 条补回 Grok 原句：`submitStaffRegistration` 摘要改商户代提交为细化，「与 003 的公开本人注册口分工后才自洽」。
5. U17-003 同根格保留 U07-004 作频控链提示（GR-003 原文未列该 ID）；不改冲突分类；保留完整句：私人手机通道、无指纹 4xx、公司列表不能作为无绑定注册的前置。
6. U17-007 行号以 Grok `:174-175` / `:270-273` 为准（候选 `:268-274` 为含表头范围）；或支择一，不发明第三套版本政策。
7. U17-009「STAFF-员工，或并入 AUTH」为或支，发布者择一；不得顺手为 floor-plan / merchant-service 扩映射。
8. U17-006 保持：两成功名只是标识符；`recordSuccess` 归 U06；不并入 U14-008。
9. U17-011 第 17 条保持「不得删该句」，不压缩成只剩「基线既有」。
10. U17-011 第 1 条「两路径 main＝基线」即 Grok「对抗式覆盖不会回退 M009–M012 Java」，不得读成整包覆盖未发布 OpenAPI 也无冲突（那是 001/002/003 的字典背书面）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U17-001…011 成立；发布前须修「是」10 / 「否」1；添加 1（U17-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 79900bytes SHA4360bfa71471b2f021c753799580fb105c3fcb9bd25b49f74b20d1e71b819308，前 48965bytes 原字节不变）。
