CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M027 B1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B1（修复前全模块三线全审；本模块首次开工，本版历史未审）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` `6ce763ff537721555abc86c1a5815208f0ec55f5`（PR #467 已合并）。主工作区含其他模块未推本地提交与暂存（含已暂存未发布的 `V427`，工作区另有未跟踪 `V428`）；**本模块 13 个迁移文件三态一致**（工作区 / HEAD / origin/main blob 相同，`git diff HEAD` 与 `git diff origin/main` 对该 13 路径均为空）
- 模块：`plan/modules.md` V0.04「租户范围与数据隔离（技术不变量）／数据库迁移／第1段」；标记 NORMAL；计划有效内容 375 行；`git ls-files -z -- <13 paths> | xargs -0 cat | wc -l` → **375**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察，不代替本席实算
- 映射 18 项：`M027 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`（无 HTML 派生条目）
- findings.md / ledger：只读背景；ledger 记本模块 PENDING→IN_PROGRESS、B3 累计 0、无任何本版 M027 段/finding。本席不改计划 / findings / ledger
- 独立性：未读 `M027-b1-self-raw.md`；未用 MCP / 子代理 / 第三席；未改业务代码 / 三 HTML / 锁 / 台账；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时审计产物；psql 只 SELECT，口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行 `sed -n` 读取，未 source、未回显
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 结论：**PASS**（13 文件全文覆盖、18 项逐项核对、三线完成；临时 GR 项 0；无未定稿工程缺陷；无未答 questions）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc `audit` 派评审席，不写 `docs/arc/audits/` |
| graft | 已读并调用 | `graft grep` 定位 `currentProjectScopeQuietly`、finance `merchant_id`、唯一约束名；约束名在 Java 无命中后改短符号 / 源码检索 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| long-running-background-tasks | 已读 | 本轮 psql / sha256 / graft 均前台完成，未挂 monitor |
| grilling / grill-me | 已读 | 只借追问结构自问自答，事实自读，不问人、不派子代理 |
| batch-grill-me | **缺口** | 本席技能树无独立 `batch-grill-me`；已并入 grilling 自问自答 |
| ubiquitous-language | **缺口** | 本席加载路径无此技能（仅见其它工具目录副本）；§6 已划掉 domain-modeling，不冒充调用 |
| codebase-design | 已读 | 迁移是浅接口（DDL）+ 深不变量（约束/索引）；缺陷若有应落在约束是否真护住声称，不在再抽一层 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B1 禁止修复、不加诊断桩、不对库执行写 |
| tdd / test-driven-development | 已读 | 用来判断外模块测试能否因约束变坏而红；本席不写测试 |
| verification-before-completion | 已读 | 先跑 psql 现态 / git 三态 / 仓储 ON CONFLICT 对照再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收 |
| requesting-code-review | 存在，B4 用 | B1 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | 可选存在（`~/.qwen/skills/...` 等） | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B1 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B1 不适用（铁律 5 问人闸全部作废） |
| i-have-adhd | 存在 | Codex 回执模式；本席正文只落 raw |

Arc `audit` 技能要求派子代理：与本轮「只允许两席」冲突，按用户指令不执行。

graft 本轮有储蓄行的调用：`currentProjectScopeQuietly` ≈302,162；finance `merchant_id` ≈49,098。唯一约束全名一次 0 hit、无储蓄行。合计约 **351,260** tokens。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git rev-parse HEAD:<path>` + `git rev-parse origin/main:<path>`。

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

覆盖：**13/13 全文逐行，不抽样**。引入提交（抽样 `V044`）：`b1fd6534`。Flyway 已应用、不可改写；缺陷只能经新的前向迁移修复或登记。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位，不另发明业务规则）：

- `docs/business-system-map-for-operations-v0.1.html` L708 红线④原文：「看不到别的商户和别的项目的数据」；同条：持密钥最高权限只可逐笔开放、七类校验不设关闭开关
- `docs/platform-governance-map-for-operations-v0.1.html` L653：「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」
- `docs/business-enum-quick-reference-v0.1.html`：无「租户隔离列/唯一键」枚举锚点；HTML 沉默处不反推列名、可空性或回填口径

---

## 2. 可执行证据（verification-before-completion；类型 4）

### 2.1 连接与库

| 项 | 证据 |
|---|---|
| 库 | `complex_test` / 用户 `complex` / `-h 127.0.0.1` |
| 服务端 | `SELECT version()` → PostgreSQL **16.14** on x86_64-pc-linux-musl；`inet_server_addr()`=`172.18.0.4` |
| 口令 | 只读 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行；未 source、未回显、命令结束后 `unset PGPASSWORD` |
| 语句 | 仅 SELECT / 信息模式 / `pg_catalog`；无 INSERT/UPDATE/DELETE/DDL |
| RLS | `SELECT COUNT(*) FROM pg_policies WHERE schemaname='public'` → **0**（本模块隔离是列+应用谓词+唯一约束，不是 RLS） |

### 2.2 Flyway 现态（13 条均已应用、success=t）

`SELECT … FROM flyway_schema_history WHERE version IN ('044','045','046','047','049','051','052','065','174','214','237','243','244')`：

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

库最高成功版本 **428**（`installed_rank` 最大 396）。与操作员「complex_test 已迁到含 V428」一致。本席未用自写 CRC32 去「核对」Flyway 算法（zlib 整文件 CRC 与库内 checksum 数值不等，属算法不同，**不以该差异立项**）。完整性证据是：13 文件 blob 三态不变 + `success=t`；若文件相对迁入时被改写，下次 Flyway validate 会因校验和失败而拒启动。

### 2.3 列现态（information_schema）

隔离列均为 **可空** VARCHAR（与迁移「存量兼容 / NULL=未归属」一致），长度：

| 长度 | 表 |
|---|---|
| VARCHAR(64) | 财务/证据/库存/游乐/平面图/员工服务单/退款规则的 merchant_id 或 project_id（及 `ar_ap_items.merchant_id/project_id`） |
| VARCHAR(80) | `compliance_reminders.merchant_id`（V051） |
| VARCHAR(100) | 酒吧 6 表 `merchant_id`（V047：`bar_tables/reservations/products/orders/production_tickets/bills`） |

`merchants.id` 与 `projects.id` 现态均为 **VARCHAR(64) NOT NULL**。酒吧/合规列宽于主数据 PK，首次正常写入（令牌里的商户/项目 id ≤64）不会撞长度。长度不一致记观察，不立项（无首次正常路径复现）。

唯一约束键列可空性（防「去重声称落空」）：

| 约束 | 键列 | is_nullable |
|---|---|---|
| `uq_daily_close_scope_date` | `business_date`,`scope_type`,`scope_id` | **NO / NO / NO** |
| `uq_compliance_reminder_dedup` | `source_type`,`source_id`,`reminder_type`,`due_date` | **NO / NO / NO / NO** |
| `uq_refund_authority_project`（表达式唯一索引） | `subject_type`,`subject_id`,`refund_domain` NOT NULL；`project_id` YES，用 `COALESCE(project_id,'__global__')` 封 NULL 槽 | 见 §2.4 |
| `uq_refund_auto_rule_project` | 同上模式 | 见 §2.4 |

### 2.4 约束 / 索引现态（pg_constraint / pg_indexes）

| 对象 | 现态 | 后续 |
|---|---|---|
| `uq_daily_close_scope_date` | UNIQUE (business_date, scope_type, scope_id)，`convalidated=t` | 未被后继迁移 DROP；V069/V149 接续日结汇总/append-only，不取代该唯一 |
| `uq_compliance_reminder_dedup` | UNIQUE (source_type, source_id, reminder_type, due_date)，`convalidated=t` | 仍在 |
| `chk_stock_adjustment_type` | CHECK (adjustment_type IN gain/loss/damage/waste/count_correction) **NOT VALID**，`convalidated=f` | **全库后继迁移无 `VALIDATE CONSTRAINT chk_stock_adjustment_type`**（V116/V123/V159 的 VALIDATE 针对其它约束） |
| 旧 `uq_refund_authority` / `uq_refund_auto_rule` | **已不存在** | V174 `DROP CONSTRAINT IF EXISTS` 后被表达式唯一索引取代 |
| `uq_refund_authority_project` | `CREATE UNIQUE INDEX … (subject_type, subject_id, refund_domain, COALESCE(project_id, '__global__'))` | 与 `JdbcRefundAuthorizationRepository.saveLimit` ON CONFLICT 表达式逐字匹配 |
| `uq_refund_auto_rule_project` | 同上 `(refund_domain, rule_type, COALESCE(project_id, '__global__'))` | 与 `saveRule` ON CONFLICT 匹配 |
| V046/V047/V045/V052/V065/V237/V243/V244 部分索引 | 均存在且 `indisvalid=t`；谓词 `WHERE merchant_id/project_id IS NOT NULL`（V214 的 project 索引为全列 btree，无部分谓词） | — |
| `staff_base_salaries` 的 `project_id` 索引 | **无**（仅 PK `user_id`） | V214 给了列+回填，7 张兄弟表建了 `idx_*_project`，本表未建 |

`chk_stock_adjustment_type` 观察（不立项，理由见 §6 Q2 / §7）：

- 表 `inventory_stock_adjustments` 现 **0 行**；`adjustment_type NOT IN (五枚举)` 计数 **0**
- PostgreSQL 16 外部规范：NOT VALID 的 CHECK **仍对后续 INSERT/UPDATE 强制**，只跳过加约束时对已有行的全表扫描（`ALTER TABLE … ADD CONSTRAINT … NOT VALID` 文档语义）
- Java `InventoryStockMovementService` 调整类型集合 = 同一五值；与约束一致
- 首次正常写入报损/盘点调整会被 CHECK 拦住非法值；`VALIDATE` 只改目录标志 `convalidated`，对空表无用户可见行为差
- 迁移注释是「backfill 完成后 VALIDATE」的后续动作说明，不是「当前已 VALIDATE」的完成声称

### 2.5 行计数（隔离列 NULL 情况）

绝大多数本模块表在 `complex_test` 为 0 行。非空者：

| 表 | n | merchant_id NULL | project_id NULL |
|---|---:|---:|---:|
| `ar_ap_items` | 28 | 28 | 28 |
| `audit_events` | 183 | 175 | — |
| `inventory_suppliers` | 1 | 1 | — |

`ar_ap_items` 28 行全部 `source_type='stored_value_topup'`、`direction='internal_accounting'`、`id` 形如 `arap_internal_sv_notif_*`，`created_at` 在 **2026-09-15**（晚于 V243 安装 2026-09-09）。这是储值通知集成测试残留的**平台内部核算**行：`FinanceService.recordInternalAccountingRevenue` 注释写明「无 merchant scope（平台口径）」，走无 `projectId` 的兼容构造（`ArApItemRecord` 15 参 overload 把 projectId 置 null）。**不是** V243 回填漏了合同/物业费/结算来源。V243 回填①②③针对 `contract/contract_rent/property_fee/settlement/cam`；注释已写 manual/energy_fee/ticket_distribution「等」推不出则留 null。不立项。

---

## 3. 逐文件全文覆盖 + 第一性原理三问

分类总览（2.1 顺序：冲突→细化→技术性→添加）：本模块 13 文件整体是 BSM 红线④ / PGM「项目与商户隔离」的**下位技术不变量**（列、索引、唯一、CHECK），**细化**；索引/IF NOT EXISTS/NOT VALID/表达式唯一索引为**技术性**（无新业务实体）。HTML 未规定列名或 VARCHAR 长度，沉默不反推、不按添加问。无与 HTML 明文冲突处。

### 3.1 V044 财务隔离（L1–14）

| 行 | 内容 | 审法 |
|---|---|---|
| 1–3 | 注释：`ar_ap_items.merchant_id` 可空；创建时来自调用方安全令牌、永不来自请求参数 | 读 + 对照 `FinanceService.createItem` |
| 4 | `ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64)` | 现态 VARCHAR(64) YES |
| 6–8 | 部分索引 `idx_ar_ap_items_merchant` | 现态存在 |
| 10–14 | `uq_daily_close_scope_date UNIQUE (business_date, scope_type, scope_id)`；注释：幂等键只护精确重试，本约束捕不同 key 打同一经营日+范围 | 现态存在；键列均 NOT NULL |

三问：

1. **为何必须存在**：删掉 merchant_id 列/日结唯一，红线④「看不到别的商户的数据」与「同一经营日重复日结」在库层没有第二道闸。上位：BSM L708 ④⑤。
2. **声称是否执行**：`createItem`（`FinanceService.java:1109–1143`）`merchantId = authorizationService.currentMerchantScope()`（actor 令牌），`projectId` 默认 `currentProjectScopeQuietly()`；请求体无 merchant 覆盖。日结：`lockDailyClose`（L1240–1243）先 `dailyCloseExists` 再插入，id=`daily_close_`+幂等 key slug；仓储 `ON CONFLICT (id) DO UPDATE`（`JdbcFinanceRepository.java:310`）**不是**打在 `uq_daily_close_scope_date` 上——与注释一致：应用层幂等走 id，库唯一捕不同 key。键列 NOT NULL，Postgres 多 NULL 互不冲突的漏洞在此键上不成立。
3. **未验证前提**：并发两个不同幂等 key 打同一日+范围时，应用先查后插窗口靠唯一约束；本席禁止写库，未打并发。首次单操作员正常日结可被 `dailyCloseExists` 拦住。不把未打的并发 500 映射当缺陷立项。

### 3.2 V045 证据隔离（L1–31）

四表 `audit_events/evidence_events/evidence_attachments/evidence_reviews` 加可空 `merchant_id VARCHAR(64)` + 四条部分索引。现态均在。

三问：用途=证据域红线④；`EvidenceEventService` 写入 `currentMerchantScope().orElse(null)`（L71/L405 等），来自 actor 而非请求体商户字段；部分索引现态匹配。`audit_events` 175/183 行 merchant_id NULL=平台/无商户主体留痕，与可空设计一致。

### 3.3 V046 库存隔离 + CHECK（L1–62）

10 张库存相关表加 `merchant_id VARCHAR(64)` + 部分索引；`inventory_stock_adjustments` 加 `chk_stock_adjustment_type … NOT VALID`（L56–62）。

三问：用途=库存红线④ + 调整类型收口。索引现态全在。CHECK 五值与 `InventoryStockMovementService` 的 `Set.of("gain","loss","damage","waste","count_correction")` 一致。NOT VALID 后续 VALIDATE 未做：见 §2.4，**不立项**。

### 3.4 V047 酒吧 merchant_id（L1–14）

六表 `VARCHAR(100)` + 六条部分索引。现态 VARCHAR(100) YES，索引在。无「令牌而非请求参数」注释；现行 `BarTableSessionService`/`BarPosCatalogService` 用 `currentMerchantScope()` 写入/过滤。长度 100 vs 主数据 64：观察。

### 3.5 V049 游乐四表加列（L1–7）

`ADD COLUMN merchant_id VARCHAR(64)`（无 IF NOT EXISTS、无索引）。现态列在。索引由 **V065 接续补齐**（本模块内闭环）。`AmusementSafetyService` 等写入 `currentMerchantScope().orElse(null)`。

### 3.6 V051 合规提醒（L1–14）

`merchant_id VARCHAR(80)` + 部分索引 + `uq_compliance_reminder_dedup UNIQUE (source_type, source_id, reminder_type, due_date)`。

`JdbcComplianceRepository.saveReminder`（L147）：`ON CONFLICT (source_type, source_id, reminder_type, due_date) DO UPDATE SET …` **与唯一约束列顺序一致**。四键均 NOT NULL。去重声称成立。

### 3.7 V052 平面图（L1–5）

`mall_floor_plans.merchant_id VARCHAR(64)` + 部分索引。现态在。`FloorPlanService.listFloorPlans` 用 `currentMerchantIdQuietly()`（永不抛）再 `projectScopeOrAll()`；写入带 `merchant_id` 参数源。后继 V423 加平面图 CHECK，不取代本列。

### 3.8 V065 游乐索引（L1–10）

注释写明补 V049 缺的部分索引（ISO-GAP-03）。四索引现态均在。接续完成。

### 3.9 V174 退款授权项目隔离（L1–12）

DROP 旧 UNIQUE（不含 project）→ `COALESCE(project_id,'__global__')` 表达式唯一索引两条。

现态：旧约束不在；新索引定义与注释、与 `JdbcRefundAuthorizationRepository.java:61,94` ON CONFLICT 表达式**逐字相同**。`RefundAuthorizationJdbcIntegrationTests`（外模块）按三槽（P1/P2/全局）共存 + 同自然键 upsert 锁行为。Postgres UNIQUE 将多个 NULL 视为互不相等——本迁移用 COALESCE 封槽，声称可复现。

### 3.10 V214 员工/服务单 project_id（L1–87）

八表加 `project_id VARCHAR(64)`；回填 `DISTINCT ON (user_id) … ORDER BY user_id, created_at DESC`（服务单用 `performer_user_id`）；七张表建 project 索引（**无** `staff_base_salaries`）。

仓储口径（`JdbcStaffRepository.java:33–36`）：

```
STAFF_PROJECT_SUBSELECT = (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :user_id ORDER BY p.created_at DESC LIMIT 1)
```

注释写「与迁移回填同口径」。`upsertBaseSalary` INSERT 使用该子查询。`JdbcServiceOrderRepository.saveOrder` L127–128 按 `performer` 同一子查询。**回填 SQL 与现行写入推导一致**。

观察：`upsertBaseSalary` 的 `ON CONFLICT (user_id) DO UPDATE` **不刷新** `project_id`。首次插入（档案已在）正常路径会带上 project；「先写薪资、后建档案、再改薪资」才可能留 NULL。非首次正常照做，不立项。缺索引：该表 PK 即 `user_id`，月结走档案 JOIN 而非 `WHERE staff_base_salaries.project_id=`，首次路径不撞全表扫。

### 3.11 V237 库存 project + 高价值复核 merchant（L1–38）

`inventory_stock_balances.project_id` + 部分索引；`inventory_high_value_reviews.merchant_id` + 从 `saved_bottles` / `inventory_stock_adjustments` 回填。

`JdbcInventoryRepository.atomicIncreaseStockBalance`（L986–1007）注释/SQL：写入 `project_id`，冲突时 `COALESCE(EXCLUDED.project_id, 旧值)` 防空值抹掉。`InventoryService.listStockBalances`：商户 → `listStockBalancesByMerchant`（`WHERE merchant_id=:id`）；项目 → `listStockBalancesByProject`（`WHERE project_id=:id`）；否则全量。NULL 行走**全量读通道**，不进项目切片——与注释「NULL=不限项目，避免历史行回归平台/商户读通道」一致，不是「项目员工可见全部 NULL 行」。

高价值回填两 subject：`saved_bottle_void`、`damage_report`/`stock_variance`。表现 0 行。

### 3.12 V243 ar_ap project_id（L1–41）

列+部分索引；三路回填（合同/物业费/结算 CAM）。`createItem`（L1110–1143）默认 `currentProjectScopeQuietly()`；P2 结算过账可 override（防把补贴打进过账人项目）。`saveArApItem` INSERT 含 `project_id`；`ON CONFLICT (id) DO UPDATE` **不改** merchant_id/project_id（首次落账粘住隔离列）。

28 行内部核算 project_id NULL：见 §2.5，属平台口径构造，不立项。

### 3.13 V244 流水/批次 project_id（L1–40）

两表加列+部分索引；回填从 `inventory_stock_balances` 按 item+location 推导，`COALESCE(m.target_location_id, m.source_location_id)`。现态列/索引在；表 0 行。新写入：`InventoryStockMovementService.saveMovement` 传 `currentProjectScopeQuietly().orElse(null)`；列表 `listStockMovementsPaged` 谓词 `(:project_id IS NULL OR project_id = :project_id)`（L634–637）。与「写入用令牌项目、历史 NULL 走全量通道」一致。

回填对调拨（source 与 target 均非空）取 **target** 侧余额的 project。现行写入不走该回填 SQL、而走令牌。空表无历史调拨可复现错绑。不立项。

---

## 4. 顺从式：18 项逐条（定义已全文读）

每条先抄适用结论，再给本面证据或「无本面」理由。定义来源 `criteria-lock.md` L12–100。

| ID | 适用？ | 证据 / 理由 |
|---|---|---|
| **GEN-001** 必要性与最小性 | **适用，通过** | 13 文件均为红线④的列/索引/唯一/CHECK；无纯转发、无为本模块假设场景新增抽象。V065 是 V049 缺索引的最小补丁而非第二套模型 |
| **GEN-002** 可执行正确性 | **适用，通过** | Flyway 13 条 success=t；information_schema/pg_catalog 列、索引、约束与 SQL 文本一致；仓储 ON CONFLICT 与 V051/V174 唯一目标匹配 |
| **GEN-003** 测试有效性 | **无本面** | 本模块 13 文件不含测试。外模块 `RefundAuthorizationJdbcIntegrationTests` 锁 V174 行为，作旁证不计入本模块测试覆盖。可执行证据改用类型 4（库现态） |
| **GEN-004** 信任边界校验 | **适用，通过** | 注释声称「令牌而非请求参数」：`createItem`/`EvidenceEventService`/`Bar*Service`/`Amusement*Service`/`InventoryStockMovementService` 从 `AuthorizationService` 的 actor 取 merchant/project，不从请求体抄租户键 |
| **GEN-005** 授权与最小权限 | **适用，通过（本面=列级不变量，不是鉴权实现）** | 迁移不实现 RBAC；提供按商户/项目过滤的列与索引。鉴权在外模块。无「仅靠界面隐藏」的本面代码 |
| **GEN-006** 数据完整性 | **适用，通过** | 日结唯一、合规去重唯一、退款 COALESCE 唯一、调整类型 CHECK；键列 NOT NULL 或 COALESCE 封 NULL；账平 CHECK 在 V159（外模块）且 `convalidated=t` |
| **GEN-007** 并发与幂等 | **适用，通过** | V044 注释+唯一约束覆盖「不同幂等 key 打同一日结」；V051 ON CONFLICT 自然键；V174 ON CONFLICT 表达式与索引一致。未写库打并发 |
| **GEN-008** 接口与模式一致性 | **适用，通过（长度差异见观察）** | 列名/可空性/枚举与 Java 一致。酒吧 VARCHAR(100)、合规 VARCHAR(80) 与主数据 VARCHAR(64) 不一致但首次路径 id≤64 不失败 |
| **GEN-009** 错误处理与恢复 | **无本面** | 纯 DDL/DML 回填；失败由 Flyway 事务回滚。无吞错、无伪成功 SQL |
| **GEN-010** 安全与敏感信息 | **适用，通过** | 无密钥、无动态 SQL 拼接用户输入；隔离列支持防越权读的库侧挂钩。文件不含口令 |
| **GEN-011** 可观测与审计证据 | **无本面** | 迁移不写日志。`audit_events.merchant_id` 只提供列 |
| **GEN-012** 资源与性能边界 | **适用，通过（缺一索引见观察）** | 过滤列均有 btree（部分索引）。`staff_base_salaries.project_id` 无索引，但访问主键是 `user_id` |
| **GEN-013** 构建配置与依赖 | **无本面** | 无 pom/CI。Flyway 顺序由文件名前缀保证，已在目标库执行 |
| **GEN-014** 数据库迁移正确性 | **适用，通过** | 版本单调、已应用、三态不变、现态匹配；破坏性 DROP 仅 V174 旧 UNIQUE，被表达式索引取代且代码已切 ON CONFLICT。不可改写已应用脚本 |
| **GEN-015** 界面可用性 | **无本面** | 无 UI |
| **GEN-016** 文档可验证性 | **适用，通过** | 迁移内注释的令牌来源、回填口径、COALESCE、NULL 放行、ON CONFLICT 目标均可从现行源码/pg_catalog 复现。V046「随后 VALIDATE」是未完成后续动作说明，不是「已经 VALIDATE」的假完成（空表+新行仍强制，见 §2.4） |
| **GEN-017** 业务来源分类 | **适用，通过** | 全体按冲突→细化→技术性→添加走完：无冲突、无添加；细化为红线④下位；技术性为索引/IF NOT EXISTS/表达式唯一/NOT VALID |
| **GEN-018** 第一性原理证据 | **适用，通过** | 本报告每条结论落到 HTML 原文 / 库 SELECT / PG 规范语义 / 源码行号四类之一；不以历史模块 PASS 或注释自我证明 |

---

## 5. 对抗式（grilling 结构自问自答；事实自读）

❓ **Q1** - **唯一约束键可空是否让去重落空？**  
➡️ 日结三键、合规四键 information_schema 均为 NOT NULL，Postgres「多 NULL 不相等」打不穿。退款 `project_id` 可空，V174 已用 `COALESCE(...,'__global__')`；库内 `pg_get_indexdef` 与 JDBC ON CONFLICT 一致。**无缺陷。**

❓ **Q2** - **NOT VALID 之后有没有 VALIDATE？非法历史调整类型能否活下来？**  
➡️ `convalidated=f`，后继无 VALIDATE。但 PG 16：NOT VALID CHECK 仍拦新写。现表 0 行、0 违例。首次正常报损写入仍受 CHECK。VALIDATE 只翻目录位。**不立项。**

❓ **Q3** - **ON CONFLICT 是否打错索引（会变成每次 INSERT 新行或 500）？**  
➡️ 合规、退款两条自然键 ON CONFLICT 与唯一目标匹配。日结 ON CONFLICT 打 **id** 而非 `(date,scope)`，正是 V044 要靠第二道 UNIQUE 捕的场景。库存余额 ON CONFLICT `(item_id, location_id)` 不在本模块建的隔离索引上，是更早迁移的自然键。**无错配。**

❓ **Q4** - **回填口径是否与现行仓储写入分叉，导致新行/历史行两套 project？**  
➡️ V214：仓储子查询 = 回填 `DISTINCT ON (user_id) ORDER BY created_at DESC`。V243：新行 `createItem` 用令牌；历史按合同/物业费/结算单据；内部核算显式平台 NULL。V237/V244：新行令牌；历史从余额推导。分叉是「历史无令牌」的设计，不是口径漂移。**无缺陷。**

❓ **Q5** - **merchant_id 真能从请求参数打进来吗（绕过令牌）？**  
➡️ 本模块只加列。现行写入面抽查：财务 createItem、证据、酒吧、游乐、库存流水均从 `AuthorizationService` 的 actor 取。请求 DTO 即使带 merchant_id，createItem 不读它。外模块若另有绕过，不在这 13 文件。**本面声称成立。**

❓ **Q6** - **NULL merchant/project 是否等于「所有人可见」从而击穿红线④？**  
➡️ SQL 项目切片是 `project_id = :id`（等号，不含 `OR IS NULL`）。NULL 行只出现在**未加该谓词的全量通道**（平台）。商户切片 `merchant_id = :id` 同样看不到 NULL。这是「未归属行不进入他商户/他项目切片」，不是放行越权。与 V237/V244 注释「走全量读通道」一致。**无缺陷。**

❓ **Q7** - **VARCHAR(100)/80 vs 64 会不会截断或 FK 失败？**  
➡️ 无 FK 到 `merchants.id`。PG 超长是报错不是截断。主数据 id 64。首次正常令牌 id 不会超 64。**不立项。**

❓ **Q8** - **HTML 沉默是否被拿来发明「必须 NOT NULL」或「必须 RLS」？**  
➡️ 否。HTML 只要求看不到别的商户/项目的数据、隔离不设关闭开关。列可空、无 RLS 是实现细化。不为理论补 RLS。

❓ **Q9** - **已应用迁移被改写了吗？**  
➡️ 13 文件 HEAD=origin/main=工作区。Flyway success=t。本席不改这些文件。

❓ **Q10** - **调拨回填 COALESCE(target, source) 会不会把 A 项目出库打成 B 项目？**  
➡️ 仅历史回填；新行用令牌。表空。要两三件（历史调拨 + 两侧余额不同 project + 只走回填）才成立。**不立项。**

❓ **Q11** - **staff_base_salaries 缺 project 索引是否让项目月结全表扫到超时？**  
➡️ 月结 SQL 从 `staff_profiles` DISTINCT ON 取 project 再过滤，薪资表按 `user_id` PK 点查。首次正常月结不靠该缺失索引。**不立项。**

❓ **Q12** - **有没有后继迁移把这些列 drop / 改类型 / 换唯一键，使本模块 SQL 变成谎言？**  
➡️ 检索后继：V174 取代的是 V156 旧 UNIQUE（不在本模块文件列表，但本模块 V174 正是修复）。本模块自建对象除 CHECK 未 VALIDATE 外均仍在。V049 缺索引由 V065 补。**接续完整（VALIDATE 除外，见 Q2）。**

---

## 6. 注释声称 ↔ 现行源码对照表

| 声称（文件:行） | 现行源码 | 结果 |
|---|---|---|
| V044 L2–3 令牌而非请求参数 | `FinanceService.createItem` L1109–1143 `currentMerchantScope` / `currentProjectScopeQuietly` | 可复现 |
| V044 L11–12 幂等 key 只管精确重试，UNIQUE 捕同日+范围不同 key | `lockDailyClose` L1240–1246 id 来自幂等 key；`saveDailyClose` ON CONFLICT **(id)**；另有 `uq_daily_close_scope_date` | 可复现 |
| V045 L2–3 同上令牌 | `EvidenceEventService` 多处 `currentMerchantScope()` | 可复现 |
| V046 L2–4 同上令牌 | `InventoryStockMovementService` / `atomicIncreaseStockBalance` 参数来自服务层 scope | 可复现 |
| V046 L57–58 随后 VALIDATE | 无后继 VALIDATE；`convalidated=f` | 后续动作未做；新行仍强制；**不立项** |
| V174 L3–4 COALESCE 防多 NULL | 索引定义与 JDBC ON CONFLICT 含同一表达式 | 可复现 |
| V214 L2–3 最新档案推导；仓储同步 | `STAFF_PROJECT_SUBSELECT`；服务单按 performer | 可复现 |
| V237 L4–6 / V244 L8–9 NULL=不限项目走全量通道 | `listStockBalancesByProject` 等值匹配；全量方法无 project 谓词 | 可复现 |
| V237 L4 / V244 L6 写入 currentProjectScopeQuietly | `InventoryStockMovementService` L401/L414/L265 | 可复现 |
| V243 L2–3 落账时令牌写入 | `createItem`；内部核算走无 project 构造（平台口径） | 主路径可复现；平台内部核算故意 NULL |
| V051 去重唯一 | `saveReminder` ON CONFLICT 四列 | 可复现 |

---

## 7. 观察（登记、不立项、不进修复队列）

1. **`chk_stock_adjustment_type` 仍 NOT VALID**（V046 L59–62；`pg_constraint.convalidated=f`）。空表、新行仍检查、枚举与代码一致。首次正常路径无失败可复现。
2. **merchant_id 长度三档**（64/80/100）。主数据 PK 64。首次路径不截断。
3. **`staff_base_salaries.project_id` 无索引**；PK 访问，月结走档案 JOIN。
4. **`upsertBaseSalary` ON CONFLICT 不更新 project_id**：需「无档案先写薪资再改薪资」才留 stale/NULL。
5. **`ar_ap_items` 28 行平台内部核算 project/merchant 皆空**：测试残留 + 平台口径构造，晚于 V243，符合「推不出留 null」。
6. **无 RLS**：隔离靠列+应用谓词。HTML 未要求 RLS。
7. **本模块无测试文件**：GEN-003 无本面；行为由库现态 + 外模块 JDBC 测试旁证。
8. **工作区他模块脏**：`AuthorizationService.java` 等有暂存改动；HEAD 的 `currentMerchantScope()` 无商户即抛，部分调用方仍 `.orElse(null)`。不纳入本模块 finding 范围。
9. **Flyway checksum 数值**：库内 checksum 与 naive zlib CRC32 不同算法，不以之为漂移证据。

ponytail-review（只评这 13 个已应用 SQL）：无可删抽象、无多余包装。`Lean already. Ship.` net: 0。NOT VALID 未 VALIDATE 是少做一步目录动作，不是多写的代码。

---

## 8. 临时 findings

**无。** 无 GR-01…。无冲突项。无添加项。无需要 B2 问操作员的业务发明。

工程缺陷是否成立：本模块 **0** 条成立。

冻结 ID：无（B2 才分配）。四分类：无待分条目。

---

## 9. PASS/FAIL 闸

单席 PASS 必须同时满足（runbook §9 / 提示词）：

| 条件 | 本席 |
|---|---|
| 本模块所有文件有覆盖记录 | 13/13 全文 |
| 映射的所有判据 ID 已核对 | GEN-001…018 逐项 |
| 三线均执行 | §3 第一性原理 / §4 顺从式 / §5 对抗式 |
| 所有声称均有四类证据之一 | §2 命令输出、§6 源码行、HTML 原文、PG NOT VALID/UNIQUE 语义 |
| 无未答 questions | 无添加项，未写 questions/ |
| 无未定稿缺陷 | 临时 GR 0 |
| Grok 哈希行有效 | 本文件第一行独立实算 |

**本席 B1 结论：PASS。**

未改计划 / findings / ledger；未 fetch/commit/push/merge/deploy；未对数据库执行写语句；未读席甲 raw。

## M027 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **36658** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02。不加判据。
- 正本：`/home/james/complex-management` main 主工作区。本步只读两份封存 B1 前缀与 findings 顶部本批候选段。
- 席甲：Claude Fable 5.1，作者自审票。本席只读 `M027-b1-self-raw.md` 前 **24425** 字节（V0.01）；其后自 B2 标题起的追加段不作为本票依据。
- 本席 B2-r1：**PASS**（无 ID 可逐条；11 条观察逐条登记同意、第 1 条明确不入队；添加 0、不发 2.2、无 HTML 申请、无 questions、不需更正段）
- 正文实算锁（非独占行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，内容同值；ledger 顶行同值。本段未新增 `CRITERIA-LOCK-SHA256:` 行。独占锁哈希字段行仍仅文首一处。
- 无 MCP、无子代理、无第三席。未改源码/迁移/HTML/lock/findings/ledger；未 fetch/commit/push/merge/deploy；未对任何数据库执行写语句；未写本指定路径以外的任何文件。

此票只判断 findings.md V2.86 顶部「当前 M027 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、登记/队列与流程读法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **PASS** 原文。不加判据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M027-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 36658 | `ce202dc090ccc503d2adbaf829b1cba19fedf5f1a4f83a9f666640e2fc0d4353` |
| `docs/audit-2026-09/modules/M027-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 24425 | `80440ad2982921604526203cde1d95010a7d49e1943639a726b59b5917a6cd61` |
| `docs/audit-2026-09/findings.md`（全文件；只采顶部「当前 M027 B2-r1 整批候选」至该节结束） | V2.86 | 1079589 | `80c1021382d871a6841c6ce7ad377df475f541d165d1e34f045919975102ed14` |

席甲 raw 全文现大于 24425 bytes；本席按指令只取前 24425 字节，SHA 与封存值相同，其后 V0.02 B2 段不读、不采。

只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

`questions/M027*`：不存在。候选段止于「修复队列『否』（0）。」；其下他模块历史段不重审。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/升队/登记/流程再接受或反驳；禁止表演性同意；B2 不修复。本批重点：第 1 条 NOT VALID 应否入队、§2 截断要否更正段、空队列跳过 B3/B4 |
| grilling | 只借追问结构自查：11 条是否漏并/改写、有无应升 finding、VALIDATE 有无部署安全的最小处置 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/U+FFFD 计数/`questions/M027*` 存在性 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审 13 迁移 |
| codebase-design / ponytail-review | 本步对象是台账归并；最小是登记，不发明 VALIDATE/回填规则 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本步对象是审计文档差集，未查源码图 |

### 完整差集（本席观察 1…9、席甲 OBS-1…6、双方 findings 空集）

漏并：无。改写原意：无。两席 B1 临时 GR/CX 项均为空，候选无 ID 分配与空 findings 集一致。无单席独有观察被丢。无本批新开修复队列「是」或「否」ID。

| 候选登记 | 本次来源（不改写原意） | 本席票 |
|---|---|---|
| 1 | 席甲 OBS-1 + 本席观察 1 | **PASS（只登记；明确同意不入队；报操作员）** |
| 2 | 席甲 OBS-2 + 本席观察 2 | **PASS（只登记）** |
| 3 | 席甲 OBS-4 + 本席观察 3 | **PASS（只登记）** |
| 4 | 本席观察 4（席甲未编号；跨模块） | **PASS（只登记，不入本模块队列）** |
| 5 | 席甲 OBS-3（本席 §3.5 接续完成、未编号） | **PASS（只登记）** |
| 6 | 席甲 OBS-5（本席 GEN-016 通过、未编号） | **PASS（只登记）** |
| 7 | 席甲 OBS-6 + 本席观察 5 | **PASS（只登记）** |
| 8 | 本席观察 6 | **PASS（只登记）** |
| 9 | 本席观察 7 + 席甲 GEN-003 行 | **PASS（只登记）** |
| 10 | 本席观察 8 | **PASS（只登记，跨模块）** |
| 11 | 本席观察 9 | **PASS（只登记）** |

冲突入队 0。细化入队 0。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」（0）。修复队列「否」（0）。ID 分配 0。

---

### 一、差集核实（11 条逐项；第 1 条升队复核）

**候选 1 — `chk_stock_adjustment_type` NOT VALID 从未 VALIDATE。票：同意只登记，明确不入队。**

两席 B1 同根：V046:56-62 以 `NOT VALID` 建立，后继无 `VALIDATE CONSTRAINT chk_stock_adjustment_type`，库内 `convalidated=f`。本席 B1 Q2/观察 1：表 0 行、0 违例；PG 16 外部规范 NOT VALID CHECK 仍拦后续 INSERT/UPDATE；闭集五值与 `InventoryStockMovementService.ALLOWED_ADJUSTMENT_TYPES` 一致；首次正常报损/盘点调整无失败可复现；注释是「随后 VALIDATE」的后续动作说明，不是「当前已 VALIDATE」的完成声称（GEN-016 通过）。席甲 OBS-1 另写：补一条 VALIDATE 前向迁移在生产存量含闭集外旧值时会使部署失败，而生产数据两席都不能查，是否补属发布决策、报操作员。候选把两席事实并成一条观察，未改写「首次路径无失败」原意，也未把 GEN-014 通过改成缺陷。

应否升为 finding / 入队：否。V0.17：细化或技术性须查出工程缺陷才进修复队列。工程缺陷在此要首次正常路径可复现失败，或把未做动作写成已完成。两者都不成立。若主张入队，最小处置须「不会因存量数据使部署失败」。本席检过可写形态：① 裸 `VALIDATE CONSTRAINT`——生产若有闭集外旧值，迁移失败、部署失败，排除；② VALIDATE 前 UPDATE/删除闭集外旧值——要把旧值映射到哪一枚举或是否删除，是业务发明，HTML 沉默，属添加/发布决策，不是本模块最小技术修复；③ 有违例则跳过 VALIDATE——脏库上是空操作，`convalidated` 仍为 f，与现状无用户可见差，不构成修复；④ 改写已应用的 V046——GEN-014 禁止。故不存在「既完成 VALIDATE、又保证部署不因存量失败、又不发明业务规则」的最小处置。同意候选：只登记、报操作员、不分配 ID、不入队。

**候选 2 — merchant_id 宽度三档。票：同意只登记。**

席甲 OBS-2 + 本席观察 2 同根：V047=100、V051=80、其余 64，`merchants.id`=64；更宽不截断（PG 超长报错不截断；首次令牌 id≤64）。无首次路径失败。候选未改写。不入队。

**候选 3 — V214 八表加列、七表建索引，`staff_base_salaries` 缺 project 索引。票：同意只登记。**

席甲 OBS-4 + 本席观察 3 同根：每人一行、按 `user_id` PK 访问；本席 B1 Q11 月结走档案 JOIN 再点查，首次路径不靠该缺失索引。候选「按主键访问」未改写。不入队。

**候选 4 — `upsertBaseSalary` ON CONFLICT 不更新 project_id。票：同意只登记（跨模块）。**

本席观察 4 独有；席甲未编号，GEN-006 只核了 INSERT 子查询与回填同口径。候选补「属 access/staff 仓储所在模块」是范围归属，不是改写「需无档案先写薪资、之后再改薪资才留 stale/NULL」。非本 13 文件、非首次正常照做。不入本模块队列。无漏并。

**候选 5 — V049 ADD COLUMN 无 IF NOT EXISTS、当时未建索引（V065 已补）。票：同意只登记。**

席甲 OBS-3。本席 §3.5/§3.8 记为接续完成、未进观察清单。候选登记席甲观察，不把已由 V065 闭环的缺索引升缺陷。Flyway 每库一次，无 IF NOT EXISTS 不构成首次路径失败。不入队。

**候选 6 — V237/V244 头注基线提交号／当时最高迁移号。票：同意只登记。**

席甲 OBS-5。本席 GEN-016 判通过（历史落笔记录，已应用迁移不可改）。候选未改写成「注释撒谎」。不入队。

**候选 7 — V243 回填生产命中不可验证 + complex_test 28 行。票：同意只登记。**

席甲 OBS-6（生产存量两席不能查）+ 本席观察 5（28 行 `stored_value_topup` / 平台内部核算、晚于 V243、符合「推不出留 null」）。候选合并未改写任一侧。不把「测试库无回填对象」升成回填失败。不入队。

**候选 8 — 无 RLS。票：同意只登记。**

本席观察 6。HTML 只要求看不到别的商户/项目的数据、隔离不设关闭开关，未要求行级安全策略。本席 B1 Q8 不为理论补 RLS。候选「HTML 未要求」未改写。不入队、不升添加。

**候选 9 — 本模块无测试文件。票：同意只登记。**

本席观察 7（GEN-003 无本面，类型 4 库现态 + 外模块 JDBC 旁证）与席甲 GEN-003「通过（间接）」同根不同措辞。候选登记观察、不立项，与双方 B1 均通过 GEN-003 一致。不入队。

**候选 10 — 他模块暂存改动；HEAD `currentMerchantScope()` 无商户即抛 vs 调用方 `.orElse(null)`。票：同意只登记（跨模块）。**

本席观察 8。本模块 13 文件三态一致，该分叉属各调用方所在模块；候选写明与 M023/M024 既有登记同源。不入本模块队列。无漏并。

**候选 11 — 库内 Flyway checksum 与朴素 CRC32 不同算法。票：同意只登记。**

本席观察 9。不以算法差作漂移证据。完整性证据仍是 13 blob 三态不变 + `success=t`。候选未改写。不入队。

覆盖核：席甲 OBS-1…6 分别落在候选 1/2/5/3/6/7；本席观察 1…9 分别落在候选 1/2/3/4/7/8/9/10/11。本席对抗 Q10（调拨回填 COALESCE）/Q11 结论为「不立项」且未写入 §7 观察 1–9，候选未单列不构成漏并。无应升为 finding 而被压成观察的项。

---

### 二、席甲 self raw §2 证据块替换字符 — 不需要更正段

封存前缀 24425 字节内：U+FFFD **1** 处；另有 3 处按字节截断、未插入替换符。均在 §2 代码/注释摘录：

1. `JdbcRefundAuthorizationRepository.java:25` 注释截在「绝不跨到其它综合体」后（U+FFFD）。
2. `JdbcStaffRepository.java:103` INSERT 截在 `created_b`。
3. 同文件 `:117` INSERT 截在 `note,`。
4. 同文件 `:237` 注释截在「只在平」。

成因与指令一致：取证脚本按字节 `cut` 切断多字节字符或固定窗口。所引事实不依赖被切断的尾字节：退款「只回退全局」由同块完整 SQL `:31-32`（`project_id = :pid OR project_id IS NULL`）支撑；席甲已在「补充只读核对」用完整 `ON CONFLICT … COALESCE(project_id, '__global__')` 补探针 6 空窗；V214 写入口径由完整 `STAFF_PROJECT_SUBSELECT`（`:35`）与 INSERT `:88` 支撑。B1 结论、OBS 编号与本批候选均不建立在残缺尾字上。更正段只用于纠正 B1 结论或覆盖表事实错误（先例是行数笔误），此处无事实错误。**不需要更正段。** 非阻断取证呈现瑕疵，不否决整批。

---

### 三、流程读法 — 同意候选

V0.17 原文：B2 定稿后本模块修复队列为空（可以有只登记的 findings，但没有要改的）→ 跳过 B3、B4（无可修、无 diff 可审，**B3 次数不变**），直接进 B5 做全量审计确认；B5 双 PASS 才收口；不得因「B1 没发现问题」就直接判 PASS 收口。跳过 B3 不加次数。未执行过 B3 时 B5 文件名用 **r0**。

本批比「有只登记 findings、队列空」更空：无 ID、队列「是」「否」均为 0。仍适用跳过分支。ledger 本模块 B3 累计现为 0，跳过后仍为 0。下一步是 B5-r0 整模块重审，不是收口。本票 **不是** 模块 PASS，**不是** 授权跳过 B5。

runbook §8 同句。候选「B2 定稿后跳过 B3、B4（无可修、无 diff，B3 次数不变），直接进 B5……B5 双 PASS 才收口」读法正确。

---

### 四、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M027*`。两席 B1 均写无添加、无业务发明、未发 2.2、未按铁律 4 申请改三 HTML。11 条观察无应升添加（不为 HTML 沉默发明 MUST NOT NULL / 必须 RLS / 必须 VALIDATE / 必须给 staff_base_salaries 建索引）。不 BLOCKED-ASK。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 不得把候选当定稿，更不得开工 B3 或宣称模块 PASS。

- 无 ID 分配。修复队列「是」= 空。修复队列「否」= 空。
- 11 条全部只登记不入队。第 1 条明确同意不入队：不存在部署安全的最小 VALIDATE 处置。
- 一次 B3 写入集合：不适用（跳过 B3）。
- 禁止：改 13 个已应用迁移、补 VALIDATE/回填/RLS、改 V046 历史文件、改三 HTML / 锁 / 台账、发 2.2、因 B1 无发现直接收口。

### 结论

全批差集无漏并、无原意改写。席甲 OBS-1…6 与本席观察 1…9 均已登记。双方 findings 空集 → 无 ID、队列空，正确。第 1 条两席均判首次路径无失败可复现；约束对新写入已生效且与服务层闭集一致；补 VALIDATE 无部署安全的最小处置 → **同意不入队**，报操作员。无应升 finding 被压成观察。§2 截断 **不需要更正段**。流程：定稿后跳过 B3/B4，B3 次数不变（保持 0），进 B5-r0；B5 双 PASS 才收口。添加 0。HTML 申请 0。questions 0。逐条：1 PASS、2 PASS、3 PASS、4 PASS、5 PASS、6 PASS、7 PASS、8 PASS、9 PASS、10 PASS、11 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权跳过 B5。步骤仍 B2；M027 IN_PROGRESS；B3=0。
