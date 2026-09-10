CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U10 未发布材料第 10 段（员工数据库迁移）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T23:54:05Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `a886042ed45514f9bcc8eaf9da31ee1c3a9c2377`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 10 段，10 文件 90 行（backend/src/main/resources/db/migration/V400–V409，全部为 main 无对应的新增迁移），main 无主归属模块；适用判据＝通用判据 GEN-001–018（18 项）。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 迁移目录仅作编号与种子对照（只读）。未对任何 complex_test* 库应用迁移（共享测试库不写）；静态审查 + main 配置/种子对照。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U10-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U09 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U10-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：questions/U04-001.md 操作员 2026-09-09 已答「删除」（自营标注独立布尔）→ V408 `merchants.is_self_operated` 列按已答处置登记，不再弹窗；U01-009/U02-004 已登记迁移编号须重排。只读依赖追流（不审）：副本 `JdbcStaffRepository.java`（第 5 段写读列）、`StaffService.java`（第 6 段五态/方法集）、`SupervisorReviewService.java`（第 9 段 hr_review 线）、第 13 段设计文档与数据隔离说明；main `V086`、`V391`、`V410`、Flyway 配置。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql | 6 | 305 | 7c5c6587feb088372ca42a1a5db1a8f1a00f17d5902046956d5010b5dbd334ba | 全读 |
| backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql | 3 | 170 | ef514984445a6fa0299c051f3c4d0fecb1f60a8c016245c65ede4b7ffa6fdea6 | 全读 |
| backend/src/main/resources/db/migration/V402__staff_policy_settings.sql | 17 | 936 | f510a7a6da72f9bdfa9b324f858242e6b24abbbe9df1ca1768bb90089e749510 | 全读 |
| backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql | 4 | 200 | e4a62e9e51d36acb401384e7e909411564dd03b50f3b5fe34ccc8eb8d7e34061 | 全读 |
| backend/src/main/resources/db/migration/V404__staff_employment_status.sql | 4 | 284 | 1c0ddc2c0b38023c847b4214e9708be29d7bf7021cfb9fec4c9688638a4d3a46 | 全读 |
| backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql | 6 | 393 | bb00ba624b48ffaa4d63251e79cdf6b55a0f3f2066fa41ba0ad35b07a3235405 | 全读 |
| backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql | 14 | 545 | f59acfd5768d862a9f98b2f307620235e72111212526cd38160f89f72bd752ef | 全读 |
| backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql | 17 | 869 | b6ea6dc70923885aa083d23d7b8ef9ced2c387e9723b8f93d23e7a48803f22a8 | 全读 |
| backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql | 6 | 416 | fb750c0d4bea78938c3251f45983eeccc3069f44401e8bc8e4bc745e8dffabf3 | 全读 |
| backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql | 13 | 589 | 588e5f3de4475bbf49f6e262479d3e225e62fb6d550d13f4556883bd14b3abad | 全读 |

## 2. 可执行证据原文（/tmp/u10_b1_run.log）

```
START=2026-09-09T20:53:45Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql | 6 | 305 | 7c5c6587feb088372ca42a1a5db1a8f1a00f17d5902046956d5010b5dbd334ba
backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql | 3 | 170 | ef514984445a6fa0299c051f3c4d0fecb1f60a8c016245c65ede4b7ffa6fdea6
backend/src/main/resources/db/migration/V402__staff_policy_settings.sql | 17 | 936 | f510a7a6da72f9bdfa9b324f858242e6b24abbbe9df1ca1768bb90089e749510
backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql | 4 | 200 | e4a62e9e51d36acb401384e7e909411564dd03b50f3b5fe34ccc8eb8d7e34061
backend/src/main/resources/db/migration/V404__staff_employment_status.sql | 4 | 284 | 1c0ddc2c0b38023c847b4214e9708be29d7bf7021cfb9fec4c9688638a4d3a46
backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql | 6 | 393 | bb00ba624b48ffaa4d63251e79cdf6b55a0f3f2066fa41ba0ad35b07a3235405
backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql | 14 | 545 | f59acfd5768d862a9f98b2f307620235e72111212526cd38160f89f72bd752ef
backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql | 17 | 869 | b6ea6dc70923885aa083d23d7b8ef9ced2c387e9723b8f93d23e7a48803f22a8
backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql | 6 | 416 | fb750c0d4bea78938c3251f45983eeccc3069f44401e8bc8e4bc745e8dffabf3
backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql | 13 | 589 | 588e5f3de4475bbf49f6e262479d3e225e62fb6d550d13f4556883bd14b3abad
=== manifest entries ===
backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql 6 305 7c5c6587feb088372ca42a1a5db1a8f1a00f17d5902046956d5010b5dbd334ba
backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql 3 170 ef514984445a6fa0299c051f3c4d0fecb1f60a8c016245c65ede4b7ffa6fdea6
backend/src/main/resources/db/migration/V402__staff_policy_settings.sql 17 936 f510a7a6da72f9bdfa9b324f858242e6b24abbbe9df1ca1768bb90089e749510
backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql 4 200 e4a62e9e51d36acb401384e7e909411564dd03b50f3b5fe34ccc8eb8d7e34061
backend/src/main/resources/db/migration/V404__staff_employment_status.sql 4 284 1c0ddc2c0b38023c847b4214e9708be29d7bf7021cfb9fec4c9688638a4d3a46
backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql 6 393 bb00ba624b48ffaa4d63251e79cdf6b55a0f3f2066fa41ba0ad35b07a3235405
backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql 14 545 f59acfd5768d862a9f98b2f307620235e72111212526cd38160f89f72bd752ef
backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql 17 869 b6ea6dc70923885aa083d23d7b8ef9ced2c387e9723b8f93d23e7a48803f22a8
backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql 6 416 fb750c0d4bea78938c3251f45983eeccc3069f44401e8bc8e4bc745e8dffabf3
backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql 13 589 588e5f3de4475bbf49f6e262479d3e225e62fb6d550d13f4556883bd14b3abad
=== baseline 242f4872 -> unpublished tree e28dad05 (numstat, all new) ===
6	0	backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql
3	0	backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql
17	0	backend/src/main/resources/db/migration/V402__staff_policy_settings.sql
4	0	backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql
4	0	backend/src/main/resources/db/migration/V404__staff_employment_status.sql
6	0	backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql
14	0	backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql
17	0	backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql
6	0	backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql
13	0	backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql
=== main migrations: max version, V4xx list, do V400-V409 exist on main? ===
V410__m010_business_entity_topology_and_role_isolation.sql
0
2e812adf audit(160): M001–M010 checkpoint — 十个模块双席闭环的修复与审计台账
=== Flyway config: out-of-order / validate settings ===
backend/src/main/resources/application-prod.yml:21:  flyway:
backend/src/main/resources/application.yml:26:  flyway:
backend/src/main/resources/application.yml:30:    validate-on-migrate: true
=== V408: is_self_operated column (to be deleted per questions/U04-001.md) ===
2:ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE;
=== V403 vs main V086: old hash columns nullability; code no longer writes them ===
17:    phone_hash VARCHAR(64),                          -- 手机号不可逆哈希（PIPL）
18:    id_doc_hash VARCHAR(64),                         -- 证件不可逆哈希（可选）
0
=== V402 CHECKs; V404 five states; V405 method set; V407 hr_review seed + cap column; V409 unique open slot ===
V402__staff_policy_settings.sql:4:    policy_key VARCHAR(64) NOT NULL CHECK (policy_key IN ('shift_visible_days', 'payroll_penalty_cap_bps')),
V402__staff_policy_settings.sql:12:    CHECK (min_value <= numeric_value AND numeric_value <= max_value),
V402__staff_policy_settings.sql:13:    CHECK ((policy_key = 'shift_visible_days' AND min_value >= 1)
V402__staff_policy_settings.sql:16:INSERT INTO staff_policy_settings(policy_key, scope_id, numeric_value, min_value, max_value)
V404__staff_employment_status.sql:4:    CHECK (employment_status IN ('pending_entry', 'employed', 'trial', 'suspended', 'departed'));
V405__staff_work_phone_receipts.sql:6:    CHECK (method IN ('nfc', 'qr', 'manual', 'charging_disconnect'));
V407__staff_penalty_cap_approval_history.sql:11:    CHECK (0<=min_value AND min_value<=numeric_value AND numeric_value<=max_value AND max_value<=10000)
V407__staff_penalty_cap_approval_history.sql:13:INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status)
V407__staff_penalty_cap_approval_history.sql:17:    CHECK (penalty_cap_bps BETWEEN 0 AND 10000);
V409__staff_locker_scan_records.sql:4:    staff_profile_id VARCHAR(80) NOT NULL REFERENCES staff_profiles(id),
V409__staff_locker_scan_records.sql:9:    CHECK (retrieved_at IS NULL OR retrieved_at >= stored_at)
V409__staff_locker_scan_records.sql:11:CREATE UNIQUE INDEX staff_locker_open_slot ON staff_locker_records(cabinet_device_id,slot_ref)
=== main: chk_att_method constraint name exists (V405 DROP CONSTRAINT target) ===
backend/src/main/resources/db/migration/V210__staff_shift_attendance.sql:32:    CONSTRAINT chk_att_method CHECK (method IN ('nfc', 'qr', 'ma
=== main approval_lines seeds (finance/tax/legal) ===
8:    ('finance_review', '财务审核线', 1, NULL, 'active'),
9:    ('tax_review',     '税务审核线', 1, NULL, 'active'),
10:    ('legal_review',   '法务审核线', 1, NULL, 'active')
=== data-isolation spec / domain-ownership CSV in the 76 files reference V409 as max? ===
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
docs/domain-ownership-scope-audit-v0.1.csv
tools/generate-data-isolation-spec.mjs
tools/generate-domain-ownership-scope-audit.mjs
-- docs/data-isolation-and-tenant-scope-security-spec-v0.1.md: V409 
-- docs/domain-ownership-scope-audit-v0.1.csv: 
-- tools/generate-data-isolation-spec.mjs: 
=== predeploy gate that regenerates the spec (main) ===
package.json:61:    "verify:delivery-gates": "npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domai
package.json:91:    "docs:domain-ownership-scope": "node tools/generate-domain-ownership-scope-audit.mjs",
package.json:92:    "verify:domain-ownership-scope": "node tools/generate-domain-ownership-scope-audit.mjs --check",
package.json:96:    "verify:predeploy": "npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier &
package.json:136:    "docs:data-isolation-spec": "node tools/generate-data-isolation-spec.mjs",
package.json:137:    "verify:data-isolation-spec": "node tools/generate-data-isolation-spec.mjs --check"
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:1433:                <li>新员工注册分两条路径：商户员工
docs/platform-governance-map-for-operations-v0.1.html:578:                <li>员工注册分两条路径：公司自有
docs/platform-governance-map-for-operations-v0.1.html:582:                <li>办公区私人手机寄存柜流动使用
docs/business-enum-quick-reference-v0.1.html:240:    <tr><td class="k">员工状态</td><td>待入职、在职、试岗�
docs/business-enum-quick-reference-v0.1.html
docs/business-system-map-for-operations-v0.1.html
docs/platform-governance-map-for-operations-v0.1.html
END=2026-09-09T20:53:45Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 10 段（10 个迁移文件 V400–V409，共 90 行，全部为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段没有 main 归属模块，适用通用判据 GEN-001–018；main 迁移目录（`V086`、`V391`、`V410`）与 Flyway 配置只作对照（§2）。
- 第一性原理线：逐文件对照 HTML——V400 姓名加密长度与本人绑定权限（PGM L870 加密存放、L579 手机与员工绑定）；V401 注册发起人（PGM L578 审批职责分离）；V402 可见天数「按经营主体可配……出厂默认 7 天，须同时配置上下限」（BSM L1444）、扣除上限「全平台统一可配……出厂默认 20%，须同时配置上下限」（BSM L1458）；V403 手机/证件可解密存放（PGM L870–871）；V404 员工五态（BEQR L240 待入职/在职/试岗/停岗/离职）；V405 打卡方式含「工作手机被拿离充电设施这个动作本身也算领取」（BSM L1445）；V406 排班「只能追加记录、不能删除，修改前后的排班时间都完整保留」（BSM L1443）；V407 扣除上限修订留痕、人力线种子（BSM L1458「调整须经人力、财务、法务三审并由顶层平台公司最终确认」）、已月结单据锁定原上限（L1458 对上限变更生效周期沉默，沿用同段参数「只对后续周期生效」口径）；V408 自营列（U04-001 已答删除）+ 员工所属公司/注册来源（PGM L578 两条路径）；V409 寄存柜自动记录（PGM L582/BSM L938）。
- 顺从式线：18 项判据逐项（§5）。
- 对抗式线：编号是否能在 main 上应用（main 已有 V410，Flyway 默认不乱序）；V408 自营列是否随 U04-001 删除；旧哈希列是否仍被写入/是否 NOT NULL（V086 可空）；V402 CHECK 是否与服务校验一致（min≥1、cap 0..10000、min≤value≤max）；V405 是否丢弃历史 `manual`（保留）；V407 平行线种子是否齐（main V391 有 finance/tax/legal，V407 补 hr）；V409 唯一开箱格口索引；数据隔离说明/域归属 CSV 是否按 V409 生成（发布前门禁要重生成）。
- 只读依赖追流（不审、不计入本段结论）：副本 `JdbcStaffRepository.java`、`StaffService.java`、`SupervisorReviewService.java`、第 13 段文档；main `V086__staff_tables.sql`、`V391__approval_lines_seed.sql`、`V410__m010_business_entity_topology_and_role_isolation.sql`、Flyway 配置（§2 探针）。
- 10 文件全文逐行读完（§1）；相对基线全部新增（§2 numstat）。可执行验证：本段 DDL 未在任何 complex_test* 库应用（共享测试库不写）；席甲另建一次性隔离库 `u11_scratch`（2026-09-09T20:56:15Z 起，脚本 /tmp/u11_scratch_run.sh，日志 /tmp/u11_tests.log），overlay 后端从空库按 Flyway 顺序迁到 V409（`flyway_max_version=409`，V400–V409 全部执行成功，含 V405 `chk_att_method` DROP/ADD），第 11 段 7 个 DB 测试类 92/92 通过（MVN_EXIT=0），随后 DROP。该证据只证明 DDL 本身可执行、与第 5/6/11 段一致；不证明能在已应用 V410 的 main 库上执行（§6 CX-U10-001）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- main 迁移目录最新为 V410（M010 检查点，已合并并应用于 main/complex_test）；未发布 V400–V409 编号低于 main 已应用的最高版本，Flyway 默认 `outOfOrder=false` 下不会执行且校验报错（§2 配置探针）→ 发布前须整体重排为 ≥ V411（U01-009/U02-004 已登记同一事实，本段为迁移本体）。
- 10 文件 main 均无对应；V408 第 2 行（`is_self_operated` 列）随 questions/U04-001.md「删除」处置去掉。

## 5. 18 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过（登记） | 除 V408 第 2 行自营列（已答删除，CX-U10-002）外，各表/列均有 HTML 上位 |
| GEN-002 可执行正确性 | 不通过 | 编号 V400–V409 低于 main 已应用 V410，默认配置下不执行（CX-U10-001）；DDL 本身在空库 `u11_scratch` 按序执行成功（§3） |
| GEN-003 测试有效性 | 通过（登记） | 第 11 段 DB 测试依赖本段；席甲在隔离库 `u11_scratch` 实跑 92/92（§3），测试内容归 U11 审 |
| GEN-004 信任边界 | 通过 | CHECK 闭集：五态、打卡方法、注册来源、参数上下限 |
| GEN-005 授权 | 通过 | V400 仅给 role_staff_self 加 staff.device.bind（本人绑定，PGM L579） |
| GEN-006 数据完整性 | 通过（登记） | V402 CHECK min≤value≤max、cap 0..10000；V409 FK + 开箱格口唯一；V403 旧哈希列保留不再写入（OBS-1） |
| GEN-007 并发幂等 | 通过 | V407 `ON CONFLICT(line_code) DO NOTHING`；V400 `ON CONFLICT DO NOTHING` |
| GEN-008 接口一致性 | 通过（登记） | 列名与第 5/6 段仓储一致；V408 自营列与 U04-001 冲突（CX-U10-002） |
| GEN-009 错误处理 | 不适用 | DDL |
| GEN-010 安全 | 通过 | V403 加密列为 TEXT；未新增明文 PII 列 |
| GEN-011 审计证据 | 通过 | V406/V407 修订留痕表 |
| GEN-012 资源 | 通过 | V408/V409 索引；V406 索引 |
| GEN-013 构建 | 不通过 | 编号冲突使发布门禁（Flyway validate、数据隔离说明重生成）红（CX-U10-001） |
| GEN-014 迁移 | 不通过 | 同 CX-U10-001；数据隔离说明/域归属 CSV 须在重排后重生成 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | 每文件版本行；注释以「M010-0xx」自编号，与本审计 M010 模块 ID 撞名（OBS-2） |
| GEN-017 分类 | 通过 | CX-U10-002 归「添加（操作员已答删除）」引用 U04-001，不再问；其余无添加 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |

## 6. 本次 Findings（临时 ID CX-U10-00N；正式 ID 由 B2 分配）

### CX-U10-001 · 技术性 · 发布前须修：是

- 位置：`V400__…` 至 `V409__…`（10 文件编号）；main `db/migration/` 已含 `V410__m010_business_entity_topology_and_role_isolation.sql`（§2）；Flyway 配置未开 `out-of-order`（§2 探针）。
- 判据：GEN-002、GEN-013、GEN-014；依据类型：git/配置事实。
- 业务影响：合入 main 后 Flyway 启动校验报「resolved migration not applied / out of order」或跳过不执行，员工域新表新列（policy_settings、shift_revisions、penalty_cap_revisions、locker_records、company_id 等）不存在，第 5/6 段服务 SQL 运行期报错。
- 无业务发明理由：重排编号不改任何业务语义。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：以 main 为底把 10 个文件重排为 V411 起连续编号（保持内部顺序），随后重生成数据隔离说明与域归属 CSV 并跑全链 verify:predeploy；不在本轮修复。

### CX-U10-002 · 添加（操作员已答删除，questions/U04-001.md）· 发布前须修：是

- 位置：`V408__staff_registration_company_routes.sql:2` `ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE;`。
- HTML 原句：BSM L701/PGM L495 六类合作方式含「自营」；BSM L1433/PGM L578「在商户档案上标注为自营的商户视同公司自有」。
- 判据：GEN-001、GEN-008、GEN-017；依据类型：HTML 原句 + 操作员 2.2 答复。
- 事实：该列是第 4 段独立自营布尔的存储面；操作员 2026-09-09 已答「删除」。V408 其余三条（company_id、registration_source、部分索引）有 PGM L578 两条路径上位，保留。
- 工程缺陷：是。建议登记：是（引用已答复，不再问）。最小处置：删除 V408 第 2 行，其余保留；与 U03/U04/U05/U08 处置同批。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：V403 新增 `phone`/`id_doc` 加密列，旧 `phone_hash`/`id_doc_hash`（V086，可空）保留不再写入（第 5 段仓储 0 处引用）；注释「不尝试从旧哈希恢复资料」——存量档案的手机/证件在发布后为空，须由本人/审批人补录；HTML 对迁移沉默。
- OBS-2：注释与第 11 段测试以「M010-001/006/017/019/020/029」自编号，与本审计 M010 模块 ID 撞名，文档口径需区分。
- OBS-3：V402 `shift_visible_days` 只约束 min≥1、无上限；V407 `penalty_cap_bps` 默认 2000 与 V402 种子一致（出厂 20%）。
- OBS-4：V405 保留 `manual` 供历史读取、新增 `charging_disconnect`，与 HTML L1445 三种方式一致；`chk_att_method` 先 DROP 后 ADD，main V210 定义同名约束（§2 探针），隔离库实跑通过（§3）。
- OBS-5：V409 `cabinet_device_id` 无外键到 terminal_devices（柜机登记表），设备存在性由第 5 段服务校验。
- 技能：`using-superpowers`、`verification-before-completion`（未应用迁移如实记）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。10 文件全文三线审计、18 项判据逐项核对；发现 1 项技术性须修（CX-U10-001 编号低于 main V410）、1 项添加消费方（CX-U10-002，操作员已于 U04-001 答「删除」，本段引用不再问）、5 项观察；无 HTML 修改申请；未在任何 complex_test* 库应用迁移（隔离库 `u11_scratch` 建后即删）；未修源、未提交/推送。本报告落盘前未读取 `U10-b1-grok-raw.md` 任何正文。
---

# U10 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.62「当前 U10 B2-r1 整批候选」U10-001…004；modules/U10-b1-grok-raw.md V0.01（39453bytes SHAd394f3660c68fbeedf974b023d00f0e3e514950b01f209368195b0183650a9df）；本文件 B1 段（23336bytes SHAd05ee4d397427fa53ff3595e2a4e6c8c24a32e059bf34f433e84d87f97012899）。2026-09-10T00:07:52Z
- 席甲 B2 核实（只读，见 ledger V2.71 U10 节）：V402 `:16-17` 唯一 INSERT 为处罚上限、`:4` CHECK 允许 `shift_visible_days` 键；`application.yml:26-30` `validate-on-migrate: true`、无 `out-of-order`；main 迁移目录最高 V410；U05-005 定稿处置与 U10-003 一致；`questions/U04-001.md` 答复「删除」含 U10 V408 该列；两席隔离库证据一致（席甲 `u11_scratch` 空库到 V409 + U11 92/92；Grok `u10_grok_probe` 10/10 OK、可见天数 count=0）。

## 勘误（席甲 B1 段）

- 席甲 B1 OBS-3 只记 `shift_visible_days` CHECK 无上限、GEN-006 记「通过（登记）」，漏判 V402 `:16-17` 未种子可见天数出厂行（BSM L1444「出厂默认 7 天，须同时配置上下限」）；B2 接受 Grok GR-U10-003 为 U10-003（冲突，同根 U05-005）。席甲 B1 票 FAIL 不变，缺陷计数由 2 升为 3。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 10 文件名 ≤ main V410、`validate-on-migrate` 无 out-of-order；两席同判；处置只引用既有门禁（version-order 脚本、数据隔离说明重生成、verify:predeploy） |
| 002 | PASS | V408:2 即 U04-001 已答删除的存储面；保留 company_id/registration_source 有 PGM L578 上位；不再问 |
| 003 | PASS | V402 只种子处罚上限；BSM L1444 明文「须同时配置上下限」；处置与 U05-005 定稿同口径，数字由发布者配置 |
| 004 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 003「冻结判据」读作 GEN-002、GEN-008、GEN-014；BSM-001（L521「未填写上下限的参数不得上线」）/BSM-086（L1444）为分类对照 HTML 原文与同根 U05-005 引用，不是本段冻结判据 ID（本段无 main 归属，冻结集仅 GEN-001–018）。
2. 001 两条既有闸分开点名：编号用 `verify:migration-version-order`（挂在 `verify:delivery-gates`，未挂 predeploy）；数据隔离说明/域归属 CSV 用 `verify:predeploy` 内 `--check`，红则跑既有 `docs:*` 重生成。均非新闸。
3. 002 处置「U05 分流改读」以已定稿 U05-001 的 Java 字段 `cooperationForm` 为准；`cooperation_form` 为同一码的 SQL 列名。
4. 004 所列 Grok §8 条目为抽样；§8.6（V406/V407 无跨表 FK）、§8.8（CHECK 保留 manual）未写出、维持观察；席甲 OBS-1「须补录」不得读成发布前回填存量（铁律 8）。
5. 001 同 PR 列入 U12 与已定稿 U09-004 同向，保留。

## 结论

席甲 B2-r1 票：**PASS**（候选 U10-001…004 成立；发布前须修「是」3 / 「否」1；添加 1（U10-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 60527bytes SHA9ed28bafa432311904dc0d144b9f5767d211cf99c961adf0bf91a4e0b50c5106，前 39453bytes 原字节不变）。
