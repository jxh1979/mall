CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M027 B5-r1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B5-r1（整模块三线全量确认审计，不只看 diff；B2 双 PASS 定稿后修复队列为空，按 V0.17 跳过 B3/B4；B3 累计仍为 0。文件名按本轮操作员指定用 r1，不因「未执行 B3 时提示词写 r0」另编号）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` `6ce763ff537721555abc86c1a5815208f0ec55f5`。本模块 13 个迁移文件工作区 / HEAD / origin/main blob **三态一致**（`git status --short -- <13 paths>` 空；`git hash-object` = `git rev-parse HEAD:<path>` = `git rev-parse origin/main:<path>`）
- 模块：`plan/modules.md` V0.04「租户范围与数据隔离（技术不变量）／数据库迁移／第1段」；标记 NORMAL；文件数 13；`git ls-files -z -- <13 paths> | xargs -0 cat | wc -l` → **375**（与计划「有效内容行数：375」一致）
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 派发词 / B1 raw / 其它报告）。sidecar 文件内容同值，仅作交叉观察
- 映射 18 项（锁末映射区原文）：`M027 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`。无 HTML 派生 ID。不新增判据
- findings.md V2.87 只读背景：本模块无 ID、修复队列为空、11 条观察只登记。本席**不把无 finding / B1 无发现当成模块已全对**；本轮按对象全文重审。已在 B2 双席定为只登记的 11 条，无新证据不改判
- 独立性：未读 `M027-b5-r1-self-raw.md`；可读旧 B1/B2（`M027-b1-grok-raw.md` / `M027-b1-self-raw.md`）作背景，**不得复用 B1/B2 票作为本步 finding**。未用 MCP / 子代理 / 第三席；未问人；未改业务代码 / 三 HTML / 锁 / findings / ledger；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时审计产物；psql 只 SELECT，口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行 `sed -n` 读取，未 source、未回显，结束后 `unset PGPASSWORD`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- `questions/M027*`：不存在
- 结论：**PASS**（13/13 全文覆盖、18 项逐项核对、三线完成；临时 GR 项 0；无未定稿工程缺陷；无添加项；无未答 questions。11 条 B2 观察维持只登记。本票不是授权跳过席甲 B5，模块收口仍须双 PASS + 闸 1 四值全等）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。grilling 本体「等用户回答 / 事实题派子代理」作废：只借追问结构，事实自读。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc `audit` 派评审席，不写 `docs/arc/audits/` |
| graft | 已读并调用 | `graft grep "ON CONFLICT"`（backend Java）；约束名/写入口径在命中截断后改短符号源码检索 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| long-running-background-tasks | 已读 | 本轮 psql / sha256 / graft 均前台完成，未挂 monitor |
| grilling / grill-me | 已读 | grill-me 仅转发 grilling。对抗式自问自答，不问人、不派子代理 |
| batch-grill-me | **缺口** | 本席技能树无独立 `batch-grill-me`；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | **缺口** | 本席加载路径无此技能；§6 已划掉 domain-modeling，不冒充调用 |
| codebase-design | 已读 | 迁移是浅接口（DDL）+ 深不变量（约束/索引/回填）；缺陷若有应落在约束是否真护住声称，不在再抽一层 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩、不对库执行写 |
| tdd / test-driven-development | 已读 | 用来判断外模块测试能否因约束变坏而红；本席不写测试 |
| verification-before-completion | 已读 | 先跑本轮 psql 现态 / git 三态 / ON CONFLICT 对照再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方本次 raw 可收（互盲） |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | 存在 | 编号、无寒暄；本文件第一行仍必须是判据锁哈希 |

明确不用（提示词划掉或与双席冲突）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`、Arc `audit`（会派子代理）。

graft 本轮有储蓄行的调用：`ON CONFLICT` ≈496,043 tokens（1 call）。

---

## 1. 模块覆盖（全文实读，行数/SHA 本轮独立实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git rev-parse HEAD:<path>` + `git rev-parse origin/main:<path>`。与 B1 时逐字节对照：**13 路径 SHA256 / 物理行 / 字节全部相同**。

| 路径 | 物理行 | 字节 | SHA256 | git blob（工作区=HEAD=origin/main） |
|---|---:|---:|---|---|
| `backend/src/main/resources/db/migration/V044__finance_security_isolation.sql` | 14 | 751 | `7547d6f7de4f7af6b73cab72fbaa37691116b757c47ac7464f0e3ac4b79dcf2d` | `2fc9402443f7a1b659d3a89125ce591773647d56` |
| `backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql` | 31 | 1043 | `9d5b55cc5a0b4cf3abe2c21dd53fb24be21a433a4d260ed6d71834f5629b9b67` | `33546ece9315b59251b5c17132c5ae26e5f5ba47` |
| `backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql` | 62 | 2911 | `00178d960b8a656689036ad53fc2706f97ebd02806b583eddfbe0cd46f82c5e8` | `0ce8bb7fac462882efb138dcc51f0f2c37062331` |
| `backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql` | 14 | 1360 | `8369056d19d23b12fbc3c7bc1ff8b4171f804e73028b229076e9149417addadb` | `e74a929dcde2d2e3b2b8fa3b369436c9c6d19a49` |
| `backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql` | 7 | 414 | `a7e6dc25e5643397567611dfdfaa1b11903b612d2553c0b3e70a9a65964b2541` | `094413b3740b401206619893ee9452223370ba6c` |
| `backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql` | 14 | 632 | `80b453285ae44ff4ec8224f6c64e035ee5402297149d9b9d5a1f2db6dc0777e7` | `a60fc287e09c4982ee60337363566e603c068fbf` |
| `backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql` | 5 | 207 | `7c319c79cb53ea49677940ee785737fbe5a0ea099f66212207518445bbc66063` | `ea458a034ece421f9a299ec61442a43f9a713e5d` |
| `backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql` | 10 | 765 | `c0e869114b15fb40dcddf7546e88fc821c9adc39cea94c3292c379495bb46bf0` | `f1914cd5571bc572886330e6720b65b319a20526` |
| `backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql` | 12 | 1012 | `04a7eb6af7876948092865cce076d81d950baba5271cafb80638687b858111bf` | `7944cbbc1bb1abc88d5cdc16515feafa31e11c02` |
| `backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql` | 87 | 4127 | `463857a23c9cbc18a7390a45e1e4cec2ad3578d35a47f41b12003f759620dfa0` | `5b2742adb91f4cea92e9f2547592fe118ec36391` |
| `backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql` | 38 | 2224 | `760f8373c5c2410bd7a50ff99a106eedcd869b0fd4161352bd50dd6fe702f9d0` | `931ce9ae9ce581f1ee492eccc09267d2b340cc1f` |
| `backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql` | 41 | 1974 | `178d132ab4238d2d341ea480c9b8e40017cbf30d3f8373fb78f995309234b662` | `c67bb1f3df719036d192e46b351dfdfa4ec35c8a` |
| `backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql` | 40 | 2635 | `70f74a458cf24475a0b78a8e360495b1a9468488226cd13ffc1592bf7344cebe` | `07204450e7de6d4b806014640af1758f433879bf` |
| **合计** | **375** | **20055** | — | — |

覆盖：**13/13 全文逐行，不抽样**。Flyway 已应用、不可改写；缺陷只能经新的前向迁移修复或登记。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位）：

- `docs/business-system-map-for-operations-v0.1.html` L707 红线④原文：「看不到别的商户和别的项目的数据」；同条：持密钥最高权限只可逐笔开放、七类校验不设关闭开关。SHA `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`
- `docs/platform-governance-map-for-operations-v0.1.html` L653：「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」。SHA `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`
- `docs/business-enum-quick-reference-v0.1.html`：无「租户隔离列/唯一键」枚举锚点；HTML 沉默处不反推列名、可空性或回填口径。SHA `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`

分类总览（2.1 顺序：冲突→细化→技术性→添加）：13 文件整体是 BSM 红线④ / PGM「项目与商户隔离」的**下位技术不变量**（列、索引、唯一、CHECK、回填），**细化**；索引 / `IF NOT EXISTS` / `NOT VALID` / 表达式唯一索引为**技术性**（无新业务实体）。HTML 未规定列名或 VARCHAR 长度，沉默不反推、不按添加问。无与 HTML 明文冲突处。无添加项。

---

## 2. 可执行证据（verification-before-completion；类型 4；本轮当场重跑）

### 2.1 连接与库

| 项 | 证据 |
|---|---|
| 库 | `complex_test` / 用户 `complex` / `-h 127.0.0.1` |
| 服务端 | `SELECT version()` → PostgreSQL **16.14** on x86_64-pc-linux-musl；`inet_server_addr()`=`172.18.0.4`；`current_database()\|current_user` = `complex_test\|complex` |
| 口令 | 只读 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行；未 source、未回显、命令结束后 `unset PGPASSWORD` |
| 语句 | 仅 SELECT / `\h` / 信息模式 / `pg_catalog`；无 INSERT/UPDATE/DELETE/DDL |
| RLS | `SELECT COUNT(*) FROM pg_policies WHERE schemaname='public'` → **0**（本模块隔离是列+应用谓词+唯一约束，不是 RLS） |

### 2.2 Flyway 现态（13 条均已应用、success=t）

`SELECT version, script, checksum, success, installed_on FROM flyway_schema_history WHERE version IN ('044','045','046','047','049','051','052','065','174','214','237','243','244')`：

| version | script | checksum（库内） | installed_on | success |
|---|---|---|---|---|
| 044 | V044__finance_security_isolation.sql | -182843814 | 2026-09-09 06:45:50.856711 | t |
| 045 | V045__evidence_security_isolation.sql | -1679173742 | 2026-09-09 06:45:50.863002 | t |
| 046 | V046__inventory_security_fixes.sql | -1422152229 | 2026-09-09 06:45:50.887712 | t |
| 047 | V047__bar_security_merchant_isolation.sql | 1403914415 | 2026-09-09 06:45:50.929993 | t |
| 049 | V049__amusement_security_fixes.sql | -1574552726 | 2026-09-09 06:45:50.965975 | t |
| 051 | V051__compliance_security_fixes.sql | -1008319877 | 2026-09-09 06:45:50.984707 | t |
| 052 | V052__floorplan_merchant_isolation.sql | -41470600 | 2026-09-09 06:45:50.994918 | t |
| 065 | V065__amusement_merchant_isolation_indexes.sql | -108722951 | 2026-09-09 06:45:51.124912 | t |
| 174 | V174__refund_authority_project_isolation.sql | 1712723501 | 2026-09-09 06:45:52.015399 | t |
| 214 | V214__tenant_scope_staff_service_tables.sql | -905068566 | 2026-09-09 06:45:52.229409 | t |
| 237 | V237__inventory_isolation_fixes.sql | 333115499 | 2026-09-09 06:45:52.353052 | t |
| 243 | V243__ar_ap_project_isolation.sql | -48591936 | 2026-09-09 06:45:52.402653 | t |
| 244 | V244__inventory_movement_batch_project_isolation.sql | -25656073 | 2026-09-09 06:45:52.41098 | t |

库最高成功版本 **428**（`V428__m021_settlement_format_share_media.sql` success=t；同时可见已应用 `V427`）。不以库内 checksum 与朴素 CRC32 的算法差作漂移证据（B2 观察 11，无新证据不改判）。完整性证据是：13 文件 blob 三态不变 + `success=t`。

### 2.3 列现态（information_schema；本轮全量列出）

隔离列均为 **可空** VARCHAR（与迁移「存量兼容 / NULL=未归属」一致）：

| 长度 | 表.列 |
|---|---|
| VARCHAR(64) YES | 财务/证据/库存/游乐/平面图/员工服务单/退款规则的 merchant_id 或 project_id（及 `ar_ap_items.merchant_id/project_id`） |
| VARCHAR(80) YES | `compliance_reminders.merchant_id`（V051） |
| VARCHAR(100) YES | 酒吧 6 表 `merchant_id`（V047） |
| VARCHAR(64) NO | `merchants.id`、`projects.id` |

本轮 `information_schema.columns` 44 行（含 merchants/projects PK）与上表一一对应，无缺失、无被后继迁移改类型。酒吧/合规列宽于主数据 PK，首次正常写入（令牌里的商户/项目 id ≤64）不会撞长度。长度三档维持 B2 观察 2，不立项。

唯一约束键列可空性：

| 约束 | 键列 | is_nullable |
|---|---|---|
| `uq_daily_close_scope_date` | `business_date`,`scope_type`,`scope_id` | **NO / NO / NO** |
| `uq_compliance_reminder_dedup` | `source_type`,`source_id`,`reminder_type`,`due_date` | **NO / NO / NO / NO** |
| `uq_refund_authority_project`（表达式唯一索引） | `subject_type`,`subject_id`,`refund_domain` NOT NULL；`project_id` YES，用 `COALESCE(project_id,'__global__')` 封 NULL 槽 | 见 §2.4 |
| `uq_refund_auto_rule_project` | `refund_domain`,`rule_type` NOT NULL；`project_id` YES，同上 COALESCE | 见 §2.4 |

`psql \h CREATE INDEX` 原文含 `[ NULLS [ NOT ] DISTINCT ]`。本库 `SHOW unique_nulls_not_distinct` → `unrecognized configuration parameter`（本映像未暴露该 GUC）；退款两条唯一索引用表达式封槽，不依赖 NULLS NOT DISTINCT。日结/合规键列均 NOT NULL，Postgres「多 NULL 互不相等」打不穿。

### 2.4 约束 / 索引现态（pg_constraint / pg_indexes / pg_index.indisvalid）

| 对象 | 现态 | 后续迁移 |
|---|---|---|
| `uq_daily_close_scope_date` | UNIQUE (business_date, scope_type, scope_id)，`convalidated=t` | 未被 DROP。V069 日结汇总 FK 接续；V149 append-only 触发器/REVOKE DELETE，不取代该唯一 |
| `uq_compliance_reminder_dedup` | UNIQUE (source_type, source_id, reminder_type, due_date)，`convalidated=t` | 后继迁移无 DROP/替换 |
| `chk_stock_adjustment_type` | CHECK (adjustment_type IN gain/loss/damage/waste/count_correction) **NOT VALID**，`convalidated=f` | **全库后继无 `VALIDATE CONSTRAINT chk_stock_adjustment_type`**（V123 VALIDATE 针对游乐 vendor FK，不是本约束） |
| 旧 `uq_refund_authority` / `uq_refund_auto_rule` | **已不存在**（`pg_constraint` 与 `pg_indexes` 0 行） | V174 `DROP CONSTRAINT IF EXISTS` 后被表达式唯一索引取代 |
| `uq_refund_authority_project` | `CREATE UNIQUE INDEX … (subject_type, subject_id, refund_domain, COALESCE(project_id, '__global__'::character varying))`，`indisvalid=t` `indisunique=t` | 与 `JdbcRefundAuthorizationRepository.saveLimit` L61 ON CONFLICT 表达式逐字匹配 |
| `uq_refund_auto_rule_project` | `(refund_domain, rule_type, COALESCE(project_id, '__global__'::character varying))` | 与 `saveRule` L94 ON CONFLICT 匹配 |
| V044–V065/V237/V243/V244 部分索引 | 均存在且 `indisvalid=t`；谓词 `WHERE merchant_id/project_id IS NOT NULL`（V214 的 7 条 project 索引为全列 btree，无部分谓词） | 后继无 DROP INDEX 这些名字 |
| `staff_base_salaries` 的 `project_id` 索引 | **0**（仅 PK `user_id`） | V214 给了列+回填，7 张兄弟表建了 `idx_*_project`，本表未建。B2 观察 3，无新证据不改判 |
| `mall_floor_plans.merchant_id` | 列+部分索引仍在 | V423 加/重建 `chk_mall_floor_plans_project_id_required`，不取代本列 |

`psql \h ALTER TABLE` 原文含 `ADD table_constraint [ NOT VALID ]` 与 `VALIDATE CONSTRAINT constraint_name`。本席禁止写库，不以 INSERT 探 CHECK；空表 + 服务层五值闭集 + 注释「随后 VALIDATE」不是「当前已 VALIDATE」的完成声称。B2 观察 1 维持只登记。

后继检索（类型 4）：`DROP (COLUMN\|INDEX\|CONSTRAINT)` 命中本模块隔离对象仅 V423 的平面图 **另一条** CHECK 名；无 DROP `merchant_id`/`project_id`、无 DROP 本模块部分索引、无 DROP `uq_daily_close_scope_date`/`uq_compliance_reminder_dedup`/`chk_stock_adjustment_type`。

### 2.5 行计数（隔离列 NULL）

绝大多数本模块表在 `complex_test` 为 0 行。非空者：

| 表 | n | merchant_id NULL | project_id NULL |
|---|---:|---:|---:|
| `ar_ap_items` | 28 | 28 | 28 |
| `audit_events` | 183 | 175 | — |
| `inventory_suppliers` | 1 | 1 | — |

`ar_ap_items` 28 行全部 `source_type='stored_value_topup'`、`direction='internal_accounting'`。这是储值通知集成测试残留的**平台内部核算**行：`FinanceService.recordInternalAccountingRevenue` L449「无 merchant scope（平台口径）」；L473–490 走无 `projectId` 的 15 参 overload（`ArApItemRecord.java:43–51` 把 projectId 置 null）。**不是** V243 回填漏了合同/物业费/结算来源。V243 回填①②③针对 `contract/contract_rent/property_fee/settlement/cam`；注释已写 manual/energy_fee/ticket_distribution「等」推不出则留 null。B2 观察 7，无新证据不改判。

`inventory_stock_adjustments` 0 行；`adjustment_type NOT IN (五枚举)` 计数 **0**。

---

## 3. 逐文件全文覆盖 + 第一性原理三问

每文件三问：①为何必须存在；②声称是否被实际执行；③哪个前提未经验证。依据只落类型 1/2/3/4。

### 3.1 V044 财务隔离（L1–14）

| 行 | 原文要点 | 审法 |
|---|---|---|
| 1–3 | 「populated from the caller's security token at creation time (never from a request parameter)」 | 读 + 对照 `FinanceService.createItem` / `ArApItemRequest` |
| 4 | `ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64)` | 现态 VARCHAR(64) YES |
| 6–8 | 部分索引 `idx_ar_ap_items_merchant` | 现态存在 `indisvalid=t` |
| 10–14 | `uq_daily_close_scope_date UNIQUE (business_date, scope_type, scope_id)`；「idempotency key guard only protects exact retries; this constraint catches different keys targeting the same logical date + scope」 | 现态 UNIQUE convalidated=t；键列均 NOT NULL |

三问：

1. **为何必须存在**：删掉 `merchant_id` 列/日结唯一，红线④「看不到别的商户的数据」与红线⑤「同一笔操作重复提交不重复生效」在库层没有第二道闸。上位：BSM L707 ④⑤（类型 1）。答得上 → 细化，不是添加。
2. **声称是否执行**：`ArApItemRequest.java:8–19` **无** merchant/project 字段。`createItem`（`FinanceService.java:1109–1143`）`merchantId = authorizationService.currentMerchantScope()`（actor 令牌），`projectId` 默认 `currentProjectScopeQuietly()`；请求体无商户覆盖。日结：`saveDailyClose`（`JdbcFinanceRepository.java:310`）`ON CONFLICT (id) DO UPDATE` **不是**打在 `uq_daily_close_scope_date` 上——与注释一致：应用层幂等走 id，库唯一捕不同 key。键列 NOT NULL。类型 4（列现态）+ 源码行（类型 4 可复现读取）。
3. **未验证前提**：并发两个不同幂等 key 打同一日+范围时，应用先查后插窗口靠唯一约束；本席禁止写库，未打并发。首次单操作员正常日结可被 `dailyCloseExists` 拦住。不把未打的并发 500 映射当缺陷立项。

### 3.2 V045 证据隔离（L1–31）

四表 `audit_events/evidence_events/evidence_attachments/evidence_reviews` 加可空 `merchant_id VARCHAR(64)` + 四条部分索引。现态均在、`indisvalid=t`。

三问：用途=证据域红线④。`EvidenceEventService` 多处 `currentMerchantScope().orElse(null)`（L71/L100/L170/L319/L368/L402），来自 actor 而非请求体商户字段。`audit_events` 175/183 行 merchant_id NULL=平台/无商户主体留痕，与可空设计一致。未验证：本席未写证据事件。

### 3.3 V046 库存隔离 + CHECK（L1–62）

10 张库存相关表加 `merchant_id VARCHAR(64)` + 部分索引；`inventory_stock_adjustments` 加 `chk_stock_adjustment_type … NOT VALID`（L56–62）。原文 L57–58：「NOT VALID defers validation of pre-existing rows; run VALIDATE CONSTRAINT once the backfill migration has completed.」

三问：用途=库存红线④ + 调整类型收口。索引现态全在。CHECK 五值与 `InventoryStockMovementService.java:23–24` `Set.of("gain","loss","damage","waste","count_correction")` 一致；服务层 L85–88 非法类型先 400。`saveStockAdjustment` L658–664 INSERT 含 `merchant_id`，来自 `currentMerchantScope().orElse(null)`。NOT VALID 后续 VALIDATE 未做：见 §2.4 / B2 观察 1，**不立项、不改判**。

### 3.4 V047 酒吧 merchant_id（L1–14）

六表 `VARCHAR(100)` + 六条部分索引。现态 VARCHAR(100) YES，索引在。无「令牌而非请求参数」注释；现行 `BarTableSessionService`/`BarPosCatalogService` 用 `currentMerchantScope()` 写入/过滤。长度 100 vs 主数据 64：B2 观察 2。

### 3.5 V049 游乐四表加列（L1–7）

`ADD COLUMN merchant_id VARCHAR(64)`（无 IF NOT EXISTS、无索引）。现态列在。索引由 **V065 接续补齐**（本模块内闭环）。`AmusementSafetyService` 写入 `currentMerchantScope().orElse(null)`（L205/L277）。Flyway 每库一次，无 IF NOT EXISTS 不构成首次路径失败。B2 观察 5，不改判。

### 3.6 V051 合规提醒（L1–14）

`merchant_id VARCHAR(80)` + 部分索引 + `uq_compliance_reminder_dedup UNIQUE (source_type, source_id, reminder_type, due_date)`。

`JdbcComplianceRepository.saveReminder` L147：`ON CONFLICT (source_type, source_id, reminder_type, due_date) DO UPDATE SET … merchant_id = EXCLUDED.merchant_id` **与唯一约束列顺序一致**。四键均 NOT NULL。去重声称成立。

### 3.7 V052 平面图（L1–5）

`mall_floor_plans.merchant_id VARCHAR(64)` + 部分索引。现态在。`FloorPlanService.listFloorPlans` 用 `currentMerchantIdQuietly()`（永不抛）再项目 scope。后继 V423 加平面图 CHECK，不取代本列。

### 3.8 V065 游乐索引（L1–10）

注释原文：「V049 amusement 4 表 merchant_id 此前无索引……补部分索引」。四索引现态均在、`indisvalid=t`。接续完成，不是第二套模型。

### 3.9 V174 退款授权项目隔离（L1–12）

原文 L1–4：「用户裁决：每个综合体各管各的退款审批规则」；「project_id 可空（NULL=全局/平台默认）……用 COALESCE 把 NULL 当 `__global__` 一个确定槽，避免 Postgres「多个 NULL 互不冲突」」。

DROP 旧 UNIQUE（不含 project）→ 两条表达式唯一索引。现态：旧约束不在；`pg_get_indexdef` 与 `JdbcRefundAuthorizationRepository.java:61,94` ON CONFLICT 表达式**逐字相同**。读侧 L31 `(project_id = :pid OR project_id IS NULL)` 只回退全局，不跨综合体。类型 4。

### 3.10 V214 员工/服务单 project_id（L1–87）

八表加 `project_id VARCHAR(64)`；回填 `DISTINCT ON (user_id) … ORDER BY user_id, created_at DESC`（服务单用 `performer_user_id`）；七张表建 project 索引（**无** `staff_base_salaries`）。L2–3：「project_id 由员工档案(staff_profiles,最新一条)推导……后续写入由仓储 SQL 同步推导。」L3–5：明确不动 `service_catalog_items/staff_payment_codes/service_pay_config`（「单综合体经营参数……多综合体前加列」）。

仓储口径（`JdbcStaffRepository.java:33–36`）：

```
STAFF_PROJECT_SUBSELECT = (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :user_id ORDER BY p.created_at DESC LIMIT 1)
```

与回填 `DISTINCT ON (user_id) ORDER BY created_at DESC` 同口径。八表现行 INSERT 均带该推导：`upsertBaseSalary` L88、`insertPayrollAdjustment` L103、`insertCommission` L117、薪资单 L153–165、`saveLeaveRequest` L490、`publishShiftRevision`/`upsertShift` L653/L671、考勤 L815、`JdbcServiceOrderRepository.saveOrder` L127–128 按 performer 同一子查询。**回填 SQL 与现行写入推导一致**。

`upsertBaseSalary` 的 `ON CONFLICT (user_id) DO UPDATE` **不刷新** `project_id`（L91–94）。首次插入（档案已在）正常路径会带上 project。B2 观察 4（跨模块），无新证据不改判。缺索引：该表 PK 即 `user_id`。B2 观察 3，不改判。

本轮新核：`information_schema` 对 `service_catalog_items/staff_payment_codes/service_pay_config` 的 `project_id/merchant_id` 查询 **0 行**；后继 V213/V272 只加费率/软删，未加隔离列。与 V214 L3–5 声称一致，不是假完成。不为「多综合体以后」发明必须现在加列（铁律 6 / 首次正常路径不撞）。见 §7 本轮观察 A。

### 3.11 V237 库存 project + 高价值复核 merchant（L1–38）

`inventory_stock_balances.project_id` + 部分索引；`inventory_high_value_reviews.merchant_id` + 从 `saved_bottles` / `inventory_stock_adjustments` 回填。L4–6：「历史行 project_id 保持 NULL（NULL=不限项目……避免历史库存对既有平台/商户读通道产生回归）」。

`atomicIncreaseStockBalance` L986–1007：写入 `project_id`，冲突时 `COALESCE(EXCLUDED.project_id, 旧值)` 防空值抹掉。`listStockBalancesByProject` L1073：`WHERE project_id = :project_id`（等号，不含 `OR IS NULL`）。NULL 行走**全量读通道**，不进项目切片——与注释一致。高价值回填两 subject：`saved_bottle_void`、`damage_report`/`stock_variance`，与 `InventoryHighValueReviewService` 三常量一致。`saveHighValueReview` L1583 INSERT 含 `merchant_id`。表现 0 行。

### 3.12 V243 ar_ap project_id（L1–41）

列+部分索引；三路回填（合同/物业费/结算 CAM）。`createItem` L1110–1143 默认 `currentProjectScopeQuietly()`；P2 结算过账可 override。`saveArApItem` INSERT 含 `project_id`；`ON CONFLICT (id) DO UPDATE` **不改** merchant_id/project_id（首次落账粘住隔离列）。`listArApItemsPaged` L168–169：`(:project_id IS NULL OR project_id = :project_id)` — 调用方有项目 scope 时等值匹配，看不到 NULL 历史行。28 行内部核算：见 §2.5。

### 3.13 V244 流水/批次 project_id（L1–40）

两表加列+部分索引；回填从 `inventory_stock_balances` 按 item+location 推导，`COALESCE(m.target_location_id, m.source_location_id)`。L8：「inventory_stock_adjustments 无任何列表读通道（仅 create + 按 id 复核）」。现态列/索引在；表 0 行。新写入：`InventoryStockMovementService` L401/L414 传 `currentProjectScopeQuietly().orElse(null)`；INSERT 含 `project_id`（`JdbcInventoryRepository.java:409`）。列表 `listStockMovementsPaged` 谓词 `(:project_id IS NULL OR project_id = :project_id)`（L634–637）。`findStockAdjustmentByIdForUpdate` L679–687 仅按 id，无列表——注释仍可从现行源码复现。回填对调拨取 target 侧余额 project；新行走令牌。空表无历史调拨可复现错绑。不立项。

---

## 4. 顺从式：18 项逐条（定义已全文读 `criteria-lock.md` L12–100）

每条先抄适用结论，再给本面证据或「无本面」理由。不新增判据。无 finding 不等于未审。

| ID | 适用？ | 证据 / 理由 |
|---|---|---|
| **GEN-001** 必要性与最小性 | **适用，通过** | 13 文件均为红线④的列/索引/唯一/CHECK/回填；无纯转发、无为本模块假设场景新增抽象。V065 是 V049 缺索引的最小补丁。V214 明确不动目录/收款码（注释可复现，后继未加列） |
| **GEN-002** 可执行正确性 | **适用，通过** | Flyway 13 条 success=t；information_schema/pg_catalog 列、索引、约束与 SQL 文本一致；仓储 ON CONFLICT 与 V051/V174 唯一目标匹配 |
| **GEN-003** 测试有效性 | **无本面** | 本模块 13 文件不含测试。外模块 JDBC 测试作旁证不计入本模块测试覆盖。可执行证据改用类型 4（库现态）。B2 观察 9，不改判 |
| **GEN-004** 信任边界校验 | **适用，通过** | 注释声称「令牌而非请求参数」：`ArApItemRequest` 无商户字段；`createItem`/`EvidenceEventService`/`Bar*Service`/`Amusement*Service`/`InventoryStockMovementService` 从 `AuthorizationService` 的 actor 取 merchant/project |
| **GEN-005** 授权与最小权限 | **适用，通过（本面=列级不变量，不是鉴权实现）** | 迁移不实现 RBAC；提供按商户/项目过滤的列与索引。项目切片等值匹配（`listStockBalancesByProject` `WHERE project_id = :id`）；退款读 `(project_id = :pid OR project_id IS NULL)` 只回退全局。无 RLS（B2 观察 8，HTML 未要求，不改判） |
| **GEN-006** 数据完整性 | **适用，通过** | 日结唯一、合规去重唯一、退款 COALESCE 唯一、调整类型 CHECK；键列 NOT NULL 或 COALESCE 封 NULL；V214 回填口径与现行仓储写入一致 |
| **GEN-007** 并发与幂等 | **适用，通过** | V044 注释+唯一约束覆盖「不同幂等 key 打同一日结」；V051 ON CONFLICT 自然键；V174 ON CONFLICT 表达式与索引一致。未写库打并发 |
| **GEN-008** 接口与模式一致性 | **适用，通过（长度差异见观察 2）** | 列名/可空性/枚举与 Java 一致。酒吧 VARCHAR(100)、合规 VARCHAR(80) 与主数据 VARCHAR(64) 不一致但首次路径 id≤64 不失败。CHECK 闭集 = `ALLOWED_ADJUSTMENT_TYPES` |
| **GEN-009** 错误处理与恢复 | **无本面** | 纯 DDL/DML 回填；失败由 Flyway 事务回滚。无吞错、无伪成功 SQL |
| **GEN-010** 安全与敏感信息 | **适用，通过** | 无密钥、无动态 SQL 拼接用户输入；隔离列支持防越权读的库侧挂钩。文件不含口令。本席未回显库口令 |
| **GEN-011** 可观测与审计证据 | **无本面** | 迁移不写日志。`audit_events.merchant_id` 只提供列 |
| **GEN-012** 资源与性能边界 | **适用，通过（缺一索引见观察 3）** | 过滤列均有 btree（部分索引）。`staff_base_salaries.project_id` 无索引，但访问主键是 `user_id` |
| **GEN-013** 构建配置与依赖 | **无本面** | 无 pom/CI。Flyway 顺序由文件名前缀保证，已在目标库执行 |
| **GEN-014** 数据库迁移正确性 | **适用，通过** | 版本单调、已应用、三态不变、现态匹配；破坏性 DROP 仅 V174 旧 UNIQUE，被表达式索引取代且代码已切 ON CONFLICT。不可改写已应用脚本。`chk_stock_adjustment_type` 仍 NOT VALID：对新写入已生效（B2 观察 1） |
| **GEN-015** 界面可用性 | **无本面** | 无 UI |
| **GEN-016** 文档可验证性 | **适用，通过** | 迁移内注释的令牌来源、回填口径、COALESCE、NULL 放行、ON CONFLICT 目标均可从现行源码/pg_catalog 复现。V046「随后 VALIDATE」是未完成后续动作说明，不是「已经 VALIDATE」的假完成。V237/V244 头注基线提交号为历史落笔（B2 观察 6） |
| **GEN-017** 业务来源分类 | **适用，通过** | 全体按冲突→细化→技术性→添加走完：无冲突、无添加；细化为红线④下位；技术性为索引/IF NOT EXISTS/表达式唯一/NOT VALID。V174 头注引「用户裁决」为业务来源，非本模块发明 |
| **GEN-018** 第一性原理证据 | **适用，通过** | 本报告每条结论落到 HTML 原文 / 库 SELECT / `\h CREATE INDEX` 原文 / 源码行号四类之一；不以历史模块 PASS、B1/B2 票或注释自我证明 |

---

## 5. 对抗式（grilling 结构自问自答；事实自读；不等操作员）

❓ **Q1** - **唯一约束键可空是否让去重落空？**  
➡️ 日结三键、合规四键 information_schema 均为 NOT NULL。退款 `project_id` 可空，V174 已用 `COALESCE(...,'__global__')`；库内 `pg_get_indexdef` 与 JDBC ON CONFLICT 一致。`\h CREATE INDEX` 含 `NULLS [ NOT ] DISTINCT`，本库未设该 GUC；封槽不依赖它。**无缺陷。**

❓ **Q2** - **NOT VALID 之后有没有 VALIDATE？非法历史调整类型能否活下来？**  
➡️ `convalidated=f`，后继无 VALIDATE。`\h ALTER TABLE` 列出 `NOT VALID` 与 `VALIDATE CONSTRAINT` 为两个动作。PG 对 NOT VALID CHECK 仍拦后续 INSERT/UPDATE（目录位只跳过加约束时的存量扫描）。现表 0 行、0 违例。首次正常报损写入仍受 CHECK + 服务层白名单。VALIDATE 只翻目录位。生产存量两席不能查。B2 已只登记，**无新证据，不改判、不立项。**

❓ **Q3** - **ON CONFLICT 是否打错索引（会变成每次 INSERT 新行或 500）？**  
➡️ 合规四列、退款两条 COALESCE 表达式与唯一目标匹配。日结 ON CONFLICT 打 **id** 而非 `(date,scope)`，正是 V044 要靠第二道 UNIQUE 捕的场景。库存余额 ON CONFLICT `(item_id, location_id)` 不在本模块建的隔离索引上，是更早迁移的自然键。流水 ON CONFLICT `(id)` 注释写明表上唯一仅主键。**无错配。**

❓ **Q4** - **回填口径是否与现行仓储写入分叉？**  
➡️ V214：仓储子查询 = 回填 `DISTINCT ON (user_id) ORDER BY created_at DESC`；服务单按 performer。V243：新行 `createItem` 用令牌；历史按合同/物业费/结算单据；内部核算显式平台 NULL（15 参 overload）。V237/V244：新行令牌；历史从余额/存酒/调整单推导。分叉是「历史无令牌」的设计，不是口径漂移。**无缺陷。**

❓ **Q5** - **merchant_id 真能从请求参数打进来吗？**  
➡️ `ArApItemRequest` 无该字段。财务/证据/酒吧/游乐/库存抽查均从 actor 取。本模块只加列。外模块若另有绕过，不在这 13 文件。**本面声称成立。**

❓ **Q6** - **NULL merchant/project 是否等于「所有人可见」从而击穿红线④？**  
➡️ 项目切片是 `project_id = :id`（等号）。NULL 行只出现在**未加该谓词的全量通道**（平台）。商户切片 `merchant_id = :id` 同样看不到 NULL。退款读故意 `OR project_id IS NULL` 只回退全局行。**无缺陷。**

❓ **Q7** - **VARCHAR(100)/80 vs 64 会不会截断或 FK 失败？**  
➡️ 无 FK 到 `merchants.id`。PG 超长是报错不是截断。主数据 id 64。首次正常令牌 id 不会超 64。B2 观察 2，不改判。

❓ **Q8** - **HTML 沉默是否被拿来发明「必须 NOT NULL」或「必须 RLS」或「必须给 catalog 加列」？**  
➡️ 否。HTML 只要求看不到别的商户/项目的数据、隔离不设关闭开关。列可空、无 RLS、V214 推迟目录/收款码，是实现细化/明示延期。不为理论补 RLS/加列。B2 观察 8 不改判。

❓ **Q9** - **已应用迁移被改写了吗？后继有没有 DROP 这些对象？**  
➡️ 13 文件 HEAD=origin/main=工作区。Flyway success=t。后继无 DROP 本模块隔离列/索引/日结唯一/合规唯一/本 CHECK。V174 取代的是 V156 旧 UNIQUE（本模块正是修复）。V049 缺索引由 V065 补。

❓ **Q10** - **调拨回填 COALESCE(target, source) 会不会把 A 项目出库打成 B 项目？**  
➡️ 仅历史回填；新行用令牌。表空。要两三件（历史调拨 + 两侧余额不同 project + 只走回填）才成立。**不立项。**

❓ **Q11** - **staff_base_salaries 缺 project 索引是否让项目月结全表扫到超时？**  
➡️ 月结 SQL 从 `staff_profiles` DISTINCT ON 取 project 再过滤，薪资表按 `user_id` PK 点查。首次正常月结不靠该缺失索引。B2 观察 3，不改判。

❓ **Q12** - **`upsertBaseSalary` / `upsertShift` ON CONFLICT 不更新 project_id，会不会留下 stale NULL？**  
➡️ 需「无档案先写、之后再改同一自然键」才留 NULL。首次正常「有档案再写薪资/班次」INSERT 子查询会带上 project。B2 观察 4 已覆盖薪资并标跨模块；班次同构（见 §7 本轮观察 A），**不升 finding、不改判观察 4。**

❓ **Q13** - **`inventory_stock_adjustments` 现在有没有列表读通道，使 V244「不涉及」变成谎言？**  
➡️ 现行仅 `saveStockAdjustment` + `findStockAdjustmentByIdForUpdate`（按 id）。无 list。注释仍可复现。**无缺陷。**

❓ **Q14** - **B1 无发现能否直接收口？**  
➡️ 不能。本轮独立重跑库现态、13 文件全文、18 项、grilling。收口仍须 B5 双 PASS。本票只是席乙一票。

---

## 6. 注释声称 ↔ 现行源码对照表（本轮重核）

| 声称（文件:行） | 现行源码 | 结果 |
|---|---|---|
| V044 L2–3 令牌而非请求参数 | `ArApItemRequest` 无 merchant；`createItem` L1109–1143 `currentMerchantScope` / `currentProjectScopeQuietly` | 可复现 |
| V044 L11–12 幂等 key 只管精确重试，UNIQUE 捕同日+范围不同 key | `saveDailyClose` ON CONFLICT **(id)**；另有 `uq_daily_close_scope_date` | 可复现 |
| V045 L2–3 同上令牌 | `EvidenceEventService` 多处 `currentMerchantScope()` | 可复现 |
| V046 L2–4 同上令牌 | `saveStockAdjustment` 含 merchant_id；服务层 `currentMerchantScope().orElse(null)` | 可复现 |
| V046 L57–58 随后 VALIDATE | 无后继 VALIDATE；`convalidated=f` | 后续动作未做；新行仍强制；**不立项（观察 1）** |
| V065 L1 补 V049 缺索引 | 四索引现态均在 | 可复现 |
| V174 L3–4 COALESCE 防多 NULL | 索引定义与 JDBC ON CONFLICT 含同一表达式 | 可复现 |
| V214 L2–3 最新档案推导；仓储同步 | `STAFF_PROJECT_SUBSELECT`；服务单按 performer | 可复现 |
| V214 L3–5 目录/收款码本迁移不动 | 三表无 project_id/merchant_id 列；V213/V272 未加 | 可复现（延期，非假完成） |
| V237 L4–6 / V244 L8–9 NULL=不限项目走全量通道 | `listStockBalancesByProject` 等值匹配；全量方法无 project 谓词 | 可复现 |
| V237 L4 / V244 L6 写入 currentProjectScopeQuietly | `InventoryStockMovementService` L401/L414/L265 | 可复现 |
| V243 L2–3 落账时令牌写入 | `createItem`；内部核算走无 project 构造（平台口径） | 主路径可复现；平台内部核算故意 NULL |
| V244 L8 调整单无列表读通道 | 仅 `findStockAdjustmentByIdForUpdate` | 可复现 |
| V051 去重唯一 | `saveReminder` ON CONFLICT 四列 | 可复现 |

---

## 7. 观察

### 7.1 B2 双席已定为只登记的 11 条（无新证据，不改判）

对照 findings.md V2.87 顶部 11 条，本轮复验事实仍成立：

1. `chk_stock_adjustment_type` NOT VALID、从未 VALIDATE、`convalidated=f`。新写入仍强制、闭集一致、空表 0 违例。不入队。
2. merchant_id 宽度三档（100/80/64）。更宽不截断。
3. V214 八表加列、七表建索引；`staff_base_salaries` 仅 PK `user_id`。
4. `upsertBaseSalary` ON CONFLICT 不更新 project_id（跨模块仓储）。
5. V049 ADD COLUMN 无 IF NOT EXISTS、当时未建索引（V065 已补）。
6. V237/V244 头注基线提交号/当时最高迁移号为历史信息。
7. V243 回填生产命中不可验证；complex_test 28 行平台内部核算、符合「推不出留 null」。
8. 无 RLS；HTML 未要求。
9. 本模块无测试文件；GEN-003 无本面。
10. 他模块工作区脏；HEAD `currentMerchantScope()` 无商户即抛（`AuthorizationService.java:424–430`），部分调用方仍 `.orElse(null)`。本模块 13 文件三态干净。跨模块，不入本队列。
11. 库内 Flyway checksum 与朴素 CRC32 算法不同，不以之作漂移证据。

### 7.2 本轮新观察（只登记，不立项，不分配 ID）

A. **`upsertShift` ON CONFLICT 同构不更新 `project_id`**（`JdbcStaffRepository.java:676–681`）。与观察 4 同类：首次 INSERT 用 `STAFF_PROJECT_SUBSELECT`；冲突更新只改时间/备注。需「无档案先写班次、再改同一 `(user_id,start_at)`」才留 NULL。非首次正常照做。属 access/staff 仓储所在模块。不升 finding、不并入观察 4 改判。

B. **V214 明示不动的三张经营参数表至今无隔离列**（`service_catalog_items` / `staff_payment_codes` / `service_pay_config`）。注释写「多综合体前加列」；后继 V213/V272 未加。不是「已经按项目隔离」的假完成。首次正常路径是这 8 张行数据表的写入，不因 HTML 沉默发明必须现在给目录加列。

ponytail-review（只评这 13 个已应用 SQL）：无可删抽象、无多余包装。`Lean already. Ship.` net: 0。NOT VALID 未 VALIDATE 是少做一步目录动作，不是多写的代码。

---

## 8. 临时 findings

**无。** 无 GR-01…。无冲突项。无添加项。无需要回 B2 问操作员的业务发明。

工程缺陷是否成立：本模块 **0** 条成立。不复用 B1/B2 票。不为理论场景加需求。

冻结 ID：无（本模块修复队列空，B2 未分配 ID）。四分类：无待分条目。

每条「本应作为 finding 的候选」均已在 grilling 用首次正常路径 / 四分类顺序否决；11 条旧观察无新证据不改判。

---

## 9. PASS/FAIL 闸

单席 PASS 必须同时满足（runbook §9 / 提示词）：

| 条件 | 本席 |
|---|---|
| 本模块所有文件有覆盖记录 | 13/13 全文 |
| 映射的所有判据 ID 已核对 | GEN-001…018 逐项（适用或无本面） |
| 三线均执行 | §3 第一性原理 / §4 顺从式 / §5 对抗式 |
| 所有声称均有四类证据之一 | §2 命令输出、§6 源码行、HTML 原文、`\h CREATE INDEX`/`ALTER TABLE` |
| 无未答 questions | 无添加项，`questions/M027*` 不存在 |
| 无未定稿缺陷 | 临时 GR 0 |
| Grok 哈希行有效 | 本文件第一行独立实算，未抄基准 |

**本席 B5-r1 结论：PASS。**

未改计划 / findings / ledger；未 fetch/commit/push/merge/deploy；未对数据库执行写语句；未读席甲本次 `M027-b5-r1-self-raw.md`。本票不是模块收口，不是授权跳过席甲 B5。
