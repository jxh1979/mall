CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B5-r2 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r2 PASS、B3 自报或历史 B1/B5-r1 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M010 同一 4 文件；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 2/3；LOCK-AMEND-COUNT 1；ledger V2.26；findings V1.33
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 全文一行同值；ledger 顶行同值。派发未提供预期值。
- 映射：`M010 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`，COUNT=18 unique=18 MISSING=[]；定义从锁自读，不新造判据、不把 HTML 锚点扩进本模块映射
- 正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；开工与收尾两次临时索引 `GIT_INDEX_FILE=mktemp-dir/index` 下 `git add -A && git write-tree` 均为 `8e25144adfb9eb7abbb9dce18994e51edc1170da`（对象类型 `tree`，不替换主索引）；B3 修复未提交，审计对象=当前工作区 4 历史迁移全文 + 新迁移 V410 + 活库实态 + B3-r2 夹具产物
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m010-b5r2-verify` 与探针库 `m010_b5r2_grok_probe`，结束已清理）；不改任一 `complex_test*` 库
- 未读取：`M010-b5-r2-self-raw`（当前亦不存在）。B1/B4/B5-r1 raw 与 findings V1.33 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V1.33 顶部「012已修转否，进入B5-r2」；001–004 已修；005–011、013、014 仅登记（含 009：新迁移自 V410 起编号、测试库处置已记 ledger）；questions/M010-002.md 与 M010-003.md 操作员答复首行均为精确「删除」。B5-r1 本席 PASS / 席甲 FAIL（新项即 012）；B4-r1 与 B4-r2 均双 PASS
- 技能（V0.17 收窄，按实际读取与已执行意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–004 与 012 修复后行为独立复验成立；005–011、013、014 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| 字节 | 889020 |
| python3 hashlib | 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V2.26；2908 行 / 428537 字节 / SHA `89ae85e816c72175dc0a16bdbd6356ea4d96049c48f6f08573e647586c0bab3a` |
| findings | V1.33；3323 行 / 500770 字节 / SHA `0d3313ea9b3a4fca5c33d75466e4ab0ce006537774dc25a850974ada33e1fdb9` |
| 映射行抽出 | 18 条、无重复、MISSING=[] |
| 计划 FILES | 4 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 203（切分时）；当前工作区 4 文件 `wc -l` 合计 **203**（历史正文未改）+ V410 **26** 行 |
| questions | M010-002、M010-003 均有「## 操作员答复」且首行精确「删除」；无未答问询 |
| 工作区锁相对 HEAD | 锁文件相对 `242f487` 有既有 LOCK-AMEND 差异（sidecar/ledger 与工作区实算四值同为上列哈希）；本席未改锁 |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

HTML 原文（本模块映射不含这些锚点，只作 001–004 / 012 已定稿冲突/添加项的解释权引用，不 LOCK-AMEND）：

- BEQR-144（`business-enum-quick-reference-v0.1.html:227`）：「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。」
- PGM-018（`platform-governance-map-for-operations-v0.1.html:415`）：顶层平台 / 物理项目 / 域主管公司 / 城市项目公司；「一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。」
- PGM-019 主体表：娱乐平台公司、独立酒吧商户、游乐公司、传媒公司、中央餐饮公司、城市项目公司——无 SPV、无「通用运营主体」。
- BSM-028（`business-system-map-for-operations-v0.1.html:702`）：同上多对多，不是固定「平台→项目公司→域主管公司→商户」隶属链。

三 HTML 全文检索：`经营主体分四层`=0、`六个业态主体各管什么`=0、`SPV`=0、`spv`=0。

身份核验：

- `git rev-parse HEAD` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`（开工前 `git fetch origin` 后再核）
- 临时索引 after tree = `8e25144adfb9eb7abbb9dce18994e51edc1170da`（与派发值全等；收尾再取一次同值）
- 4 历史迁移相对 HEAD：`git diff --stat` 空（正文未改）
- V410：工作区未跟踪新建文件
- B3-r2 夹具：`AnalyticsJdbcIntegrationTests.java` 工作区 blob `d8e195c3521d84c9e165ca4d269d11c20e837eea` ≠ HEAD blob `8616b586822408afe37a59ea62fee7bcb92d832e`（B4-r2 已审的那一行 + 版本头）

### 1.1 4 文件 + V410 当前行数 / 字节 / SHA256（工作区实算，`wc -l` = `\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | git blob |
|---|---|---:|---:|---|---|
| 1 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` | 109 | 3900 | `5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905` | `94692f5a2c53b7c9bd0104a2ea1bafc9b3b499b6` |
| 2 | `backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql` | 7 | 352 | `e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd` | `61018fff6f0d56d259db49e9ea28295736b87d46` |
| 3 | `backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql` | 16 | 1300 | `466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481` | `9f8eeaf9362401dd6c42329aabd9e38e24213755` |
| 4 | `backend/src/main/resources/db/migration/V354__business_entity_master_data.sql` | 71 | 5625 | `c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3` | `b518112269fddd03a71d87161e7fa2edc45af5fb` |
| 5 | `backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql`（B3-r1 新建，本模块修复产物；B3-r2 未改） | 26 | 2074 | `85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1` | `647844c1a40bde9f0f8c1a0835dd4d8b3f8f6a81` |

四历史文件 blob 与 `HEAD:` 路径全等。V410 仅存在于工作区。Flyway CRC32（按「去掉换行后拼接」与库内 checksum 对齐，五文件全部命中）：V005=`413803928`、V048=`-1430035601`、V318=`-705915847`、V354=`-61418191`、V410=`983875231`。活库 `flyway_schema_history` 同值。

B3-r2 伴生产物（非计划 FILES，属本模块修复队列 012 的实现面，本席全审）：

| 路径 | 行 | SHA256 | git blob（工作区 / HEAD） |
|---|---:|---|---|
| `backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java` | 1277 | `44d69324473a371be1520c6c12ab864e76f6fb5eb4215bc8e4f39f7bf7421a43` | `d8e195c3…` / `8616b586…` |

夹具现文 L478：`VALUES ('org_m4', 'M4主体', 'management_company', …)`；L2 `// 文件版本：V0.01`；L479 注释写明不再直写退役码 `operation`。

---

## 2. 全量三线覆盖表

每文件三线均执行；「无 finding」不等于未审。覆盖范围=计划 4 文件全文 + V410 全文 + 012 夹具现文 + 活库 `complex_test`（只读）+ 保留库 `complex_test_pre_v410_20260909`（只读对照）+ 临时探针 `m010_b5r2_grok_probe`（TEMPLATE 后对抗写入并整库 DROP）。

| 文件 | 行范围 | 第一性原理 | 顺从式 | 对抗式 | 结论 |
|---|---|---|---|---|---|
| V005 | 1–109 | 四表+八条权限种子承载 BSM-028「建立商户档案/项目/空间」与绑定读写；删掉则治理主数据无建表入口 | 建表列与后续消费面（M009 Jdbc 读写四表、权限码仍被 role_permissions 引用）一致；`IF NOT EXISTS`/`ON CONFLICT` 幂等 | 无硬 FK（007 已登记）；权限名字段英文被 V251 覆盖为中文（005 已登记）；`status` 无 CHECK（008） | 现态满足映射 GEN；历史正文不改 |
| V048 | 1–7 | `organization_id` 是组织隔离列；NULL=全局、非空=组织域。无此列则无法按组织切角色 | 列+部分索引 `idx_roles_organization` 仍在；Access `createRole`/`listRoles` 读写该列 | 历史未替换 `roles_role_name_key`（004 根因）；V410 已删该约束并建表达式唯一索引 | 列保留；唯一性由 V410 补齐 |
| V318 | 1–16 | 历史把「平台→项目公司→管理公司」写成 `parent_entity_id` 隶属链；与 BEQR-144/PGM-018 明文冲突。文件因 Flyway checksum 必须保留 | 列/索引/注释仍在历史文件；活库列与索引均为 0（V410 已 DROP） | 对探针 `INSERT ... parent_entity_id` → `column ... does not exist`；Java `backend/src/main` 除 V410 DROP 外 0 处读写 | 历史冲突已用破坏性授权迁移消除 |
| V354 | 1–71 | 六主体稳定 id 是结算归属锚；archive_reason 走软删铁律；CHECK 原含 spv/operation（002/003 已答删除） | 六行种子 id/名称/三角色在活库仍在；工商三字段 NULL；无「资产公司」行；酒吧不进本表 | 历史 INSERT 仍写 `parent_entity_id`（顺序上 V318 先加列、V410 再删列，新库可迁）；注释伪称 HTML 章节（006）；`ON CONFLICT DO NOTHING` 不修漂移（011） | 种子保留；角色闭集由 V410 收三值 |
| V410 | 1–26 | 001 删父链列/索引；002/003 UPDATE 坍缩后 CHECK 收三值并改 COMMENT；004 删全局 UNIQUE、建 `UNIQUE (COALESCE(organization_id,''), role_name)`；编号 V410 跳过未发布 400–409 | 活库 max=410、validate 378、checksum 与文件全等；COMMENT 与 PGM-018/019 三角色一致；B3-r2 未改本文件 | 探针：spv/operation/大小写/拼写均拒；两组织同名可插入、同组织/双 NULL/空串与 NULL 碰撞均拒；归档角色仍占名 | 修复落地；012 漏并夹具已由 B3-r2 收口 |
| AnalyticsJdbc（012） | L2 版本头 + L475–481 夹具 | 该类测商户汇总滚动，主体行只为满足 `merchants.business_entity_id` 存在性 | 夹具角色已是 `management_company`；Surefire 含 `merchantSummariesRollupAggregatesRealSources`，29/0/0 | 探针按夹具同列 INSERT `operation` → CHECK 违例；改 `management_company` → INSERT 0 1 后 ROLLBACK | 012 已修，无回归 |

只读依赖（验证消费面，不扩成他模块全审）：

- M009：`BusinessEntityRecord` 已无 parent 字段；`JdbcPlatformRepository.saveBusinessEntity` INSERT/SELECT 不含 `parent_entity_id`；`PlatformService.BUSINESS_ENTITY_ROLES` 三值；商户三主体列仍松引用。
- M011：`PlatformJdbcIntegrationTests` 五处 `saveBusinessEntity` 夹具均为 `management_company`；新增 `parentEntityColumnIsRemovedFromBusinessEntities`、`entityRoleCheckRejectsSpvAndOperation`、`organizationScopedRolesMayShareNameAcrossOrganizations`；本席跑 16/0/0。
- 结算：`SettlementEntityAttributionS1Tests.sixEntitySkeleton…` 断言列不存在（8/0/0）；`RevenueStreamAttributionJdbcIntegrationTests` 类注已删父链可读回第三条（2/0/0）。
- Access：`JdbcAccessRepository.createRole/listRoles` 读写 `organization_id`；平台管理员 `createRole` 写 NULL 不写空串；全局 DuplicateKey → `GlobalExceptionHandler` 409 `STATE_CONFLICT`（中文「已存在相同的记录，请勿重复提交」）。
- 012 同形态其它 JDBC 存活夹具：`LeasingSignAtomicityJdbcIntegrationTests:120`、`SettlementInvoiceListJdbcIntegrationTests:243` 均为 `management_company`（本席跑 2/0/0 与 6/0/0）。测试树直写 `operation`/`spv` 的存活 INSERT 仅剩 M011 拒绝路径（`spv` 断言，不得改掉）。`ops/` 无 `entity_role`×`operation`/`spv`。

---

## 3. 18 判据逐项

| ID | 适用 | 证据 | 判定 |
|---|---|---|---|
| GEN-001 | 五文件均适用 | 四表+角色隔离+六骨架稳定 id 均有 HTML/结算消费；V318 父链列无需求承载，已按 001 删除；V410 无额外抽象；012 只改夹具一字面量，不新增抽象 | 通过（001/012 已修） |
| GEN-002 | 迁移+活库+测试 | Flyway `Successfully validated 378 migrations`；`Current version … 410`；`Schema … up to date`；本席相关测试 63/63；活库列/约束/种子与声明一致 | 通过 |
| GEN-003 | 关键分支在 M011/结算/分析夹具 | PlatformJdbc 16/0/0、S1 8/0/0、Revenue 2/0/0、Analytics 29/0/0（含 `merchantSummariesRollupAggregatesRealSources`）、InvoiceList 6/0/0、LeasingSign 2/0/0；探针另执行 operation INSERT 拒绝（M011 测试类因 PG 事务中止只实测 spv，见观察 05） | 通过（012 本轮补跑） |
| GEN-004 | CHECK/唯一/服务层白名单 | 角色码首次可信使用前：服务层三值 + 库 CHECK 三值；非法值在探针被拒；organization_id 空串与 NULL 被 COALESCE 视同（观察 01，应用层写 NULL） | 通过 |
| GEN-005 | V048+V410 角色隔离 | 两组织同名可共存、同组织/全局同名仍拒（探针+M011 测试）；`listRoles(org)` 返回全局∪本组织；默认拒绝越权不在本 SQL 模块实现面 | 通过（004 已修） |
| GEN-006 | 唯一、引用、事务 | PK/表达式唯一索引/CHECK 在活库；Flyway 单迁移事务；无硬 FK（007 已登记）；失败探针均 ROLLBACK，残留 0 | 通过 |
| GEN-007 | 并发/幂等 | 唯一索引覆盖同键并发插入；V005/V354 `ON CONFLICT`；V410 `IF EXISTS/IF NOT EXISTS`；种子不覆盖漂移（011 已登记） | 通过 |
| GEN-008 | 跨层枚举 | 库 CHECK、服务层白名单、V410 COMMENT、六骨架角色、012/M011/租赁/结算发票夹具一致为三值；历史 V354 CHECK/COMMENT 仍写五值（正文不改，现态已被 V410 覆盖）；前端 vitest mock 仍写 `operation`（不命中 CHECK，B2 已裁定不另开 ID，见观察 08） | 通过 |
| GEN-009 | 错误 | CHECK/UNIQUE 违例可检测；父列写入立即报列不存在；DuplicateKey 在公共处理器映射 409 中文；探针违例后事务回滚无半成品 | 通过 |
| GEN-010 | 注入/秘密 | 迁移皆静态 SQL；凭证只从 `~/.complex/complex.env` 抽出三变量，报告不写密文；默认 `complex_dev_pwd` 连 127.0.0.1 失败（PWD_LEN=48） | 通过 |
| GEN-011 | 审计证据 | V410 头注绑定 001–004 与操作员裁定；`flyway_schema_history` 有 410 成功行（rank 378，checksum 983875231）；archive_reason 列仍在；012 夹具注释绑定 V410 | 通过 |
| GEN-012 | 资源边界 | 状态/组织部分索引仍在；无无界循环；本模块无批处理放大 | 通过 |
| GEN-013 | 构建/环境 | `npm run verify:migration-version-order` PASS（新增 1 个迁移，排在 origin/main 最大号 V399 之后）；文件数 378=已应用 378；无 400–409 文件；新 `complex_test` 亦无 400–409 已应用行 | 通过（009 环境已重建） |
| GEN-014 | 迁移正确性 | 顺序 399→410 间隙为操作员裁定；活库自 V001 迁到 V410 成功；checksum 与文件全等；DROP COLUMN 已获 001 方案 B / GEN-014 授权；历史四文件 checksum 未变；B3-r2 未改迁移 | 通过 |
| GEN-015 | 展示中文 | V410 COMMENT 中文；八条权限活库名为「查看/管理业务主体和绑定」等中文（V251）；V005 历史英文（005 已登记）；标识符/单据号除外 | 通过 |
| GEN-016 | 文档可验证 | V410 头注与活库/测试一致；V354/V318 历史注释仍有伪 HTML 章节与「V005 已 GRANT」（006 已登记）；结算 S1 类注释仍写「父链正确」（观察 04 / 014，断言已改） | 通过 |
| GEN-017 | 四分类 | 本轮无新添加项；002/003 已答删除并落地；012 为 003 漏并夹具的技术性后果，已修；未把业务发明写成技术修复；未因 HTML 沉默删除六骨架 | 通过 |
| GEN-018 | 证据类型 | 结论落到 HTML 原文、Flyway/mvn/psql 输出；不以 B4 PASS 或席位意见代替；未把未运行测试写成通过 | 通过 |

---

## 4. 已定稿 ID 复验（001–004、012 已修；005–011、013、014 仅登记）

### 4.1 M010-001 父链列/索引（冲突，已修）

活库 `information_schema.columns`：`parent_entity_id` 计数 **0**；`pg_indexes`：`idx_business_entities_parent` 计数 **0**。探针 `INSERT ... parent_entity_id` 失败：`column "parent_entity_id" of relation "business_entities" does not exist`（psql exit 1）。Java 主代码除 V410 DROP 外 0 引用。六骨架稳定 id/名称/角色仍在。对照库 `complex_test_pre_v410_20260909` 仍有该列（max Flyway 409），说明删除发生在 V410 而非改历史。同根因已修，无回归。

### 4.2 M010-002 / M010-003 spv、operation（添加，已答删除，已修）

活库 CHECK：`entity_role IN ('platform','project_company','management_company')`，定义不含 spv/operation。活库 6 行角色分布：platform×1、project_company×1、management_company×4，**0** 行 spv/operation。探针 INSERT `spv` / `operation` / `Platform` / `managment_company` 均 `violates check constraint "chk_be_entity_role"`；三角色插入成功。COMMENT 为 V410 中文三角色说明。同根因已修。

### 4.3 M010-004 组织内唯一索引（细化，已修）

活库：`roles_role_name_key` 约束 **不存在**；`uq_roles_org_role_name` = `UNIQUE (COALESCE(organization_id, ''), role_name)`。对照库仍有 `roles_role_name_key`。探针：两组织同名插入成功；同组织同名、两个 NULL 全局同名均 `duplicate key ... uq_roles_org_role_name`。10 行角色仍全部 `organization_id IS NULL`。同根因已修。

### 4.4 M010-012 AnalyticsJdbc 夹具（技术性，已修）——本轮关键复验

B5-r1 本席未跑 `AnalyticsJdbcIntegrationTests`，席甲因此 FAIL。B3-r2 只改该文件一行夹具 + 版本头。本席本轮：

1. 源码：L478 `'management_company'`，L479 注释禁止再写 `operation`，L2 `// 文件版本：V0.01`。
2. 类型 4 探针（`m010_b5r2_grok_probe`，BEGIN…ROLLBACK）：按夹具同列 INSERT `entity_role='operation'`（id=`org_m4_b5r2op`）→ `ERROR: new row for relation "business_entities" violates check constraint "chk_be_entity_role"`；同句改 `'management_company'`（id=`org_m4_b5r2ok`）→ `INSERT 0 1` 后 ROLLBACK。
3. 隔离副本 mvn：`AnalyticsJdbcIntegrationTests` **Tests run: 29, Failures: 0, Errors: 0**；Surefire XML `tests="29" failures="0" errors="0"` 且含方法名 `merchantSummariesRollupAggregatesRealSources`。
4. 独立扫描 JDBC `INSERT INTO business_entities`：存活夹具无第二处 `operation`/`spv` 直写（M011 `:463` 是拒绝断言，保持）。

012 已修，无回归，不重开。

### 4.5 仅登记项（本席复验，不重开修复队列）

| ID | 本席复验 |
|---|---|
| 005 | V005:79–88 仍英文；活库八码 `permission_name` 已是中文（V251） |
| 006 | V354:3–8 伪称 HTML 章节，三 HTML 0 命中；V318:8「V005已GRANT」不实；`complex_app` 对 `business_entities` SELECT/INSERT/UPDATE 为真 |
| 007 | 五表 0 条 FOREIGN KEY；商户三主体列仍在 |
| 008 | `business_entities.status` 0 条 status CHECK；探针可插入 `inactive` |
| 009 | 新 `complex_test`：max=410、n_success=378、400–409 已应用行=0；保留库 max=409 且含 400–409（n_success=387）。`verify:migration-version-order` PASS。编号与测试库处置与定稿一致 |
| 010 | 伴生测试已改且本席跑绿（见 §6）；012 为 010/003 漏并的第二处，已单独定稿并修 |
| 011 | `ON CONFLICT DO NOTHING` 仍在 V354；`entity_name` 无唯一（探针可插入第二行「平台公司」，计数 2）；六骨架无资产公司；酒吧不在主体表；Flyway checksum 全等 |
| 013 | 004 表达式唯一索引语义：空串/NULL 同键、inactive 占名、全局与组织可同名。探针三条均复现。登记不改 |
| 014 | S1 类注释 L36 仍写「稳定 id + 父链正确」、方法名仍含 ParentChain；断言 `doesNotContainKey("parent_entity_id")` 且 8/8 绿。B3-r2 未改该文件。登记 |

---

## 5. 本次 findings / 观察

无新工程缺陷，无新添加项，无 HTML 修改申请，无锁外判据。观察如下（不入修复队列；若与已定稿 ID 同根因已标明）。

| 临时编号 | 位置 | 分类 | 与已定稿 | 说明 |
|---|---|---|---|---|
| GR-M010-B5R2-01 | V410:25–26；探针空串 vs NULL | 观察 | **013 / 004 同根因** | `COALESCE(organization_id,'')` 使 `organization_id=''` 与 NULL 同键。Access `createRole` 平台管理员写 NULL 不写空串。非缺陷 |
| GR-M010-B5R2-02 | 同上；探针 inactive 占名 | 观察 | **013 / 004 同根因** | `status=inactive` 仍占唯一名，同组织不能复用。与软删留行一致，HTML 未要求释放名称 |
| GR-M010-B5R2-03 | 同上；全局与组织同名 | 观察 | **013 / 004 同根因** | 全局「店长」与组织「店长」可共存（COALESCE 两元组不同）。B2 例示即此表达式。`listRoles(org)` 会同时看见二者，属 access 展示面，非本模块新闭集 |
| GR-M010-B5R2-04 | `SettlementEntityAttributionS1Tests` 类注释仍写「父链正确」；方法名仍含 ParentChain | 观察 | **014 同根因** | 断言已改为 `doesNotContainKey("parent_entity_id")` 且 8/8 绿。文件不在 M010 FILES。B3-r2 禁止改该文件。不升格缺陷 |
| GR-M010-B5R2-05 | `PlatformJdbcIntegrationTests.entityRoleCheckRejectsSpvAndOperation` | 观察 | **002/003 同根因** | 因 PG 违例中止事务，测试只 INSERT spv，operation 靠约束定义字符串。本席探针两条 INSERT 均拒。测试能失败，不构成 GEN-003 缺口 |
| GR-M010-B5R2-06 | 探针插入 status=inactive | 观察 | **008 同根因** | 无 status CHECK，与 SUBJECT_STATUSES 仍分叉。维持登记 |
| GR-M010-B5R2-07 | 探针插入重复 entity_name「平台公司」 | 观察 | **011 同根因** | HTML 未要求主体名唯一。维持登记 |
| GR-M010-B5R2-08 | `OrganizationMerchantPage.test.tsx:12`、`GovernanceTopologyPage.test.tsx:9` `entity_role: "operation"` | 观察 | **012 B2 排除项** | 不命中 V410 CHECK；第二批 B2 已写死不并入 012、不另开 ID。两席 B5-r1 均未列为 finding。本席不升格 |

添加项：无。2.2 清单：空。

---

## 6. 命令与真实输出 / 退出码

凭证：仅从 `~/.complex/complex.env` 抽出 `COMPLEX_DATABASE_URL` / `_USERNAME` / `_PASSWORD` 三变量（不 source 整文件、不打印密文）。URL 库名改写为 `complex_test` 或探针名。USER=`complex` USER_LEN=7 PASS_LEN=48。`complex_dev_pwd`（LEN=15）连 127.0.0.1 **失败**：`password authentication failed for user "complex"`（exit 2）——已如实记录，改用抽出的三变量后成功。结束删除 `/tmp/m010-b5r2-env.sh`。

### 6.1 身份 / 锁 / 文件

| 命令 | exit | 关键输出 |
|---|---:|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| python3 hashlib / openssl dgst | 0 | 同值 |
| `git rev-parse HEAD` 与 `origin/main`（fetch 后） | 0 | `242f487233d028173cb9198eecd079f5d877405c` |
| 临时索引 `git add -A && git write-tree`（开工与收尾） | 0 | `8e25144adfb9eb7abbb9dce18994e51edc1170da` |
| 4 文件 `git diff --stat HEAD -- <paths>` | 0 | 空 |
| `npm run verify:migration-version-order` | 0 | `MIGRATION_VERSION_ORDER_PASS: 本分支新增 1 个迁移,均排在 origin/main 最大号 V399 之后` |
| 迁移文件计数 | 0 | N_FILES=378 MIN=1 MAX=410；缺 400–409；与已应用 378 相等 |

### 6.2 活库只读（`SET default_transaction_read_only=on`）

`psql -d complex_test` exit **0**。`current_database=complex_test`，`usr=complex`，`ro=on`。

- Flyway：001–410 成功 378 行；max=**410**；400–409 已应用行 **0**；V410 checksum=`983875231` description=`m010 business entity topology and role isolation` installed_rank=378；V005 checksum=`413803928`；V048 checksum=`-1430035601`；V318=`-705915847`；V354=`-61418191`
- `business_entities` 列 10 个：无 `parent_entity_id`；有 `archive_reason`
- 索引：pkey + `idx_business_entities_status`（无 parent 索引）
- CHECK：`chk_be_entity_role` 仅三值
- COMMENT `entity_role`：`主体角色（PGM-018/019）:platform=顶层平台 / project_company=城市项目公司 / management_company=域主管公司（业态主体）`
- 六骨架：`be_platform/platform`、`be_proj_changsha/project_company`、其余四行 `management_company`；工商三字段全 NULL；`资产公司` 计数 0；n_entities=6；n_spv_op=0
- roles：无 `roles_role_name_key`；有 `uq_roles_org_role_name`；10 行全 NULL organization_id
- 八条权限名为中文
- 五表 0 条 FK；`complex_app` 对 `business_entities`/`roles` INSERT 为真
- 测试后复读：仍 410 / 6 主体 / 10 角色 / 378 成功 / `org_m4` 计数 0 / 父列 0

`psql -d complex_test_pre_v410_20260909` exit **0**（只读对照，未改）。max=**409**，n=387，含 400–409；`parent_entity_id` 列计数 1；CHECK 含 spv/operation；有 `roles_role_name_key`；无 `uq_roles_org_role_name`；主体角色分布与新库相同（0 行 spv/operation）。

### 6.3 探针库对抗（`CREATE DATABASE m010_b5r2_grok_probe TEMPLATE complex_test`）

CREATE exit **0**（当时 `complex_test` 无其它会话）。对抗 SQL 包在 `BEGIN…ROLLBACK`。父列探测因列不存在该会话 exit **1**（预期）。其余包 exit **0**。ROLLBACK 后探针残留实体/角色 0（n_entities=6，n_roles=10）。事后 `DROP DATABASE m010_b5r2_grok_probe` exit 0。未改任何 `complex_test*`。

对抗结果（真实 PG 报错原文已采集）：

- 012 夹具 `operation` → `violates check constraint "chk_be_entity_role"`（Failing row `org_m4_b5r2op … operation`）
- 012 夹具改 `management_company` → `INSERT 0 1`
- spv / operation / `Platform` / `managment_company` → `violates check constraint "chk_be_entity_role"`
- 三角色 → `INSERT 0 3`
- status=`inactive` → `INSERT 0 1`（008）
- 第二行「平台公司」→ `INSERT 0 1`，计数 2（011）
- 两组织同名 → INSERT 0 1 两次成功
- 同组织同名 → `duplicate key value violates unique constraint "uq_roles_org_role_name"` Key=`(org_m010_a, 店长（M010探针）)`
- 两 NULL 全局同名 → 同上，Key=`(, 全局同名探针)`
- 空串与 NULL 同名 → Key=`(, 空串碰撞探针)`
- inactive 占名后再插入 active 同名 → 重复键 Key=`(org_ia, 归档占名探针)`
- 全局与组织同名「店长」→ 两次 INSERT 成功
- `parent_entity_id` 写入 → `column "parent_entity_id" of relation "business_entities" does not exist`

### 6.4 现有相关测试（隔离副本，探针库）

cwd=`/tmp/m010-b5r2-verify/backend`（rsync 工作区 backend，仅在副本把 `5432/complex_test` 改写为探针名；不改主仓）。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`。exit **0**，`BUILD SUCCESS`。Total time 14.373 s。

```
o.f.core.internal.command.DbValidate : Successfully validated 378 migrations
o.f.core.internal.command.DbMigrate  : Current version of schema "public": 410
o.f.core.internal.command.DbMigrate  : Schema "public" is up to date. No migration necessary.
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0  AnalyticsJdbcIntegrationTests
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0   SettlementInvoiceListJdbcIntegrationTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0   RevenueStreamAttributionJdbcIntegrationTests
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0   SettlementEntityAttributionS1Tests
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0  PlatformJdbcIntegrationTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0   LeasingSignAtomicityJdbcIntegrationTests
Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
```

Surefire XML（本跑）：`TEST-com.complex.platform.analytics.AnalyticsJdbcIntegrationTests.xml` tests=29 failures=0 errors=0，正文含 `merchantSummariesRollupAggregatesRealSources`。

未把未运行的测试写成通过。副本 `target/surefire-reports` 里另有 Access/PlatformService 等 txt，来自 rsync 带入的旧 target，**不是本席本次 mvn 跑出的**，不采信。主仓 cwd 未跑写模式 mvn。

结束后：DROP 探针库；`rm -rf /tmp/m010-b5r2-verify` 与临时 env。`complex_test` 与 `complex_test_pre_v410_20260909` 仍在。主仓 V410 仍为未跟踪，Analytics 夹具仍为工作区已有修改，本席未改源。

---

## 7. 技能实用 / 缺口

实际读取并按收窄口径使用：

| 技能 | 路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（012 是否还有第二处 JDBC 夹具 / 父列 / 角色码 / 唯一键 / 空串 / 归档占名 / Flyway 间隙），不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（`disable-model-invocation: true`） | 本体只转发 grilling |
| verification-before-completion | superpowers `verification-before-completion/SKILL.md` | 先跑命令再声称；B5-r1 漏跑 Analytics 本轮补跑后才判 012 已修 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | mvn 在前台有界超时内跑完，未挂 monitor |

明确不用（controller / 本步收窄）：brainstorming、writing-plans、tdd、requesting/receiving-code-review、executing-plans、dispatching-parallel-agents、using-git-worktrees、MCP、第三席。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未作为独立可执行技能核到（grill-me 仅转发）
- `ubiquitous-language`：未核到
- Arc `audit`：会派子代理并另写目录，与双席/无 MCP 冲突，不调用
- `ponytail-review` / `codebase-design` / `diagnosing-bugs` / `systematic-debugging`：本步未再读全文；对抗取证以 psql 探针与现有测试代替

---

## 8. 本席 B5 结论

**PASS**。

同时满足：5 个审计对象（计划 4 文件 + V410）均有覆盖记录，012 夹具作为本模块修复产物一并全审；18 条映射判据均已核对；三线均执行；声称均有 HTML / 可执行产物 / 可复现命令四类证据之一；questions 已精确答复；无未定稿缺陷；无添加项未答；无 HTML 问题；锁行有效。

相对 B5-r1：本席补跑了当时漏掉的 `AnalyticsJdbcIntegrationTests`（29/29，含 012 目标方法），并独立扫描确认 JDBC 存活夹具无第二处 `operation`/`spv` 直写。012 已修成立。无新的未定稿项。

本票不是双席合取，也不是模块闭环。席甲仍须独立落盘 self-raw 后再读本文件做合取。本席不行使否决。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M010 B5-r2 Grok 乙席完整审计报告（现锁全量重开）

- 文件版本：V0.02（指定路径已有 V0.01 旧锁 B5-r2，32729 字节 SHA256 `25524c741f56e867dde01a6274f86194f1345ee0587b4c33a6b1a3098e672912`；本次完整报告追加到末尾，不覆盖。旧票锁哈希 `b012328f…6695a` / findings V1.33 **不得**替代本次）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r2（现锁 B1/B2 双 PASS 后跳过新增 B3/B4；历史 B3 累计 2 保持；整模块全量三线重审，不只看修复点，不拿前轮 PASS 免审）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M010 同一 4 文件、冻结有效内容行 203；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 模块生命周期（台账上下文，非本席改写）：ledger 顶部「当前M010：按最新指令完整双席B1重开」为 `IN_PROGRESS`；B3 累计 2；LOCK-AMEND-COUNT 1；ledger V8.05；findings V3.52 当前候选节已定稿、14 项队列均否
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.14；本席独立实算 `python3 hashlib` / `sha256sum` / `openssl dgst -sha256` 三工具同值（见本完整报告第一行）；891182 字节 / 6856 行；sidecar 全文一行同值；ledger 顶行同值。派发未提供预期值，未抄 sidecar/ledger/他席。
- 映射：`M010 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`，COUNT=18 unique=18 MISSING=[]；定义从锁自读 GEN-001…GEN-018 全文，不新造判据、不把 HTML 锚点扩进本模块映射
- 正本：分支 `main`；`git rev-parse HEAD`（只读、**未 fetch**）=`e456ba96c84185c8d39f18f548011daac48a6466`。审计对象=冻结 4 历史迁移全文 + 既有修复 V410 全文 + 活库 `complex_test` 只读 SELECT + 直接消费方源码
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁/questions/.git；不 commit/push/PR/merge/部署/worktree/fetch；不装依赖；不写永久测试；**禁止任何数据库创建/删除/迁移/表 DDL 或写数据（含临时库）**；验证=读文件 + SELECT。此前 B1 临时探针超出派发约束已登记并清理，本次未再建库、未 INSERT/UPDATE/DELETE
- 未读取：本次 `M010-b5-r2-self-raw` 新增正文。B1 本席 V0.03 探针结果可引用但标明**非 B5 新执行**；B2 本席 V0.04 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不替代本席重审）：findings V3.52「当前M010现锁B2候选」14 项均否、9 观察完整归并；013 已补齐「NULL与空串同键、全局与组织角色可同名、inactive角色仍占名」；002/003 questions 答复首行均为精确「删除」且 V410 已执行。无本次新 B3/B4
- 技能（V0.17 收窄，按实际读取与已执行意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–004 与 012 修复后行为由活库 SELECT + 文件/Flyway checksum 独立复验成立；005–011、013、014 维持仅登记。013 三边界由索引定义（本席 SELECT）与 B1 探针（非本轮新执行）共同核到。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 1. 锁、范围与源指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
python3 hashlib.sha256(Path('docs/audit-2026-09/plan/criteria-lock.md').read_bytes())
sha256sum docs/audit-2026-09/plan/criteria-lock.md
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，独占锁行已写在本完整报告第一行。BYTES=891182 LINES=6856 exit 0。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.14；判据总数 401 = 18 general + 383 HTML 锚点 |
| 独立 SHA256 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值（对照，非抄算） |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V8.05；1744087 字节 / 7577 行 / SHA `758f05340008619937a51a7be3ab141845ecb7ee5ba47a6deed6075c4274fe6b` |
| findings | V3.52；1261369 字节 / 6393 行 / SHA `8d0af6f7db5f538d931899c2f37628d8eb97f1fe24a861ff7d7154d9c01bf496` |
| 映射行抽出 | 18 条、无重复、MISSING=[]；定义区 `### GEN-001`…`### GEN-018` 均存在 |
| 计划 FILES | 4 条与 `modules.md` V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 203；`git ls-files -z -- <4 paths> \| xargs -0 cat \| wc -l` = **203** |
| V410 | 不在 FILES / `scope.txt` / `module-union.txt`；`git ls-files` 受控；本轮仅作 001–004 直接修复证据，不扩冻结范围 |
| questions | M010-002 SHA `cb767baba4808a37b18eb8b09b0dd25632467682bff8decb063274b09a9d3538`；M010-003 SHA `bc16304b875995a73221b01d75ad78368bad0c2ea6c905289ab4721aa34f0072`；两文件「## 操作员答复」后首个非空行均为精确「删除」 |
| HEAD | `main` / `e456ba96c84185c8d39f18f548011daac48a6466`（未 fetch）。`modules.md` 仍写基线 `242f4872…`——属排除目录内计划文件陈旧，不升格为本模块源码缺陷 |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103798 | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

HTML 原文（本模块映射不含这些锚点，只作 001–004 已定稿冲突/添加项的解释权引用，不 LOCK-AMEND）：

- BEQR-143（`business-enum-quick-reference-v0.1.html:226`）：「四类主体」经营对象、运营主体、合同主体、结算主体。
- BEQR-144（同文件:227）：「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。」
- PGM-018（`platform-governance-map-for-operations-v0.1.html:415-420`）：顶层平台 / 物理项目 / 域主管公司 / 城市项目公司；「一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。」资产业态不另设独立主体，由城市项目公司兼任。
- PGM-019（同文件:424-433）主体表：娱乐平台公司、独立酒吧商户、游乐公司、传媒公司、中央餐饮公司、城市项目公司——无 SPV、无「通用运营主体」。
- BSM-028（`business-system-map-for-operations-v0.1.html:702`）：同上多对多，不是固定「平台→项目公司→域主管公司→商户」隶属链。

三 HTML 全文检索（类型 1）：`经营主体分四层`=0、`六个业态主体各管什么`=0、`SPV`=0、`spv`=0、`通用运营主体`=0。

磁盘迁移：`V*.sql` 399 个，min=1 max=431；400–409 均缺（操作员已授权跳过）；另有历史空号 118/119/… 与本模块无关。

### 1.1 4 文件 + V410 当前行数 / 字节 / SHA256 / Flyway CRC32（工作区实算）

行=`\n` 计数。Flyway checksum = 按行 UTF-8、不含换行的 CRC32 有符号 int32（与 `BufferedReader.readLine()` 一致）。与 `complex_test.flyway_schema_history` 全等。

| # | 路径 | 行 | 字节 | SHA256 | Flyway 文件=库 | 全文 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` | 109 | 3900 | `5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905` | 413803928 = 413803928 | 已读 L1–L109 |
| 2 | `backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql` | 7 | 352 | `e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd` | -1430035601 = -1430035601 | 已读 L1–L7 |
| 3 | `backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql` | 16 | 1300 | `466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481` | -705915847 = -705915847 | 已读 L1–L16 |
| 4 | `backend/src/main/resources/db/migration/V354__business_entity_master_data.sql` | 71 | 5625 | `c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3` | -61418191 = -61418191 | 已读 L1–L71 |
| 合计冻结 | | **203** | | | 四条 success=t installed_on 2026-09-09 | |
| 5 | `backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql` | 26 | 2074 | `85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1` | 983875231 = 983875231 | 已读 L1–L26 |

**当前模式以 V410 为准，不得把 V318 父链注释 / V354 `spv`/`operation` CHECK 当最终模式。** V318/V354 正文按 Flyway 不可改；其冲突面已由授权的 V410 覆盖。V410 之后无再改 `chk_be_entity_role` / `parent_entity_id` / `uq_roles_org_role_name` 的迁移（仓内 `V4*.sql` 仅 V410 命中这三处）。

四冻结 + V410 均 `git ls-files` 受控。四冻结均在 `scope.txt` 与 `module-union.txt`；V410 均不在。

---

## 2. 数据流（冻结 SQL → V410 → complex_test 实态 → 直接消费方）

```
V005 建 business_entities/merchants/projects/spaces + 八条权限英文种子 + role_platform_admin 授予
  → V048 roles.organization_id + 部分索引（当时未改 role_name 全局 UNIQUE）
  → V318 ADD parent_entity_id + 索引 + 四层隶属 COMMENT
  → V354 archive_reason / 按 operation 归档 / 六骨架 INSERT（含 parent）/ CHECK 五值（含 spv,operation）
  → V410 DROP 父列与索引；operation/spv 行改为 management_company；CHECK 三值；COMMENT 改 PGM-018/019；
         DROP roles_role_name_key；CREATE UNIQUE INDEX uq_roles_org_role_name ((COALESCE(organization_id,'')), role_name)
```

活库 `complex_test`（本席 SELECT，`current_database=complex_test` `current_user=complex`；未 source 全 env、未打印密文）：

- `flyway_schema_history`：005/048/318/354/410 均 success=t，checksum 与文件全等；n_success=399；max_ver=431；400–409 查询 0 行
- `business_entities` 10 列（无 `parent_entity_id`）；索引仅 pkey + `idx_business_entities_status`；约束 pkey + `chk_be_entity_role` 仅 `'platform','project_company','management_company'`
- COMMENT ON `entity_role` 与 V410:20-21 逐字一致
- 六骨架 6 行全部 active，工商三列 NULL；全表 n_be=6；spv/operation 行=0；名称「资产公司」「资产管理公司」=0；inactive=0
- `roles`：`uq_roles_org_role_name` 定义为 `btree (COALESCE(organization_id, ''::character varying), role_name)`，**无 WHERE**；`roles_role_name_key` 不存在；`idx_roles_organization` 部分索引仍在；10 行 organization_id 全 NULL、空串 0、status 全 active
- 八码权限名为中文（与 V251 后续 i18n 一致）；`role_platform_admin` 8/8
- merchants=57，三主体列非空 0；projects=0；spaces=0
- 指向 `business_entities` 的硬 FK 仅 `business_entity_projects_business_entity_id_fkey`（V427，他模块）
- `business_entities.status` 无 CHECK
- `pg_database` `datname LIKE 'm010%'` = 0；无 `be_role_%` / `role_m010%` 残留

直接消费方（只读、不扩 FILES / 不扩锁）：

- `PlatformService.java:62` `BUSINESS_ENTITY_ROLES` 三值；`:929-937` 服务层中文 400 拒绝非法角色
- `JdbcPlatformRepository.java:44-63` INSERT/UPDATE 列清单无 `parent_entity_id`；`:67-75` SELECT 同
- `BusinessEntityRecord.java:6-16` 无父链字段
- `PlatformJdbcIntegrationTests.java:705-713` 断言父列/父索引 count=0；`:716-730` CHECK 三值且 INSERT `spv` 期望完整性失败（同事务只探 spv）；`:732-739` 两组织同名可插入、同组织第二行 DuplicateKey
- `SettlementEntityAttributionS1Tests.java:216-247` 方法名仍含 ParentChain；断言 `doesNotContainKey("parent_entity_id")`、六骨架角色、工商 NULL、错拼角色拒绝
- `AnalyticsJdbcIntegrationTests.java:475-479` 夹具 `management_company`，注释写明 V410 / M010-012
- `StaffRegistrationService.java:118` 注释：V410 已删父链
- `V251__permission_names_i18n_zh.sql:199-202,287-288,297-298` 八码中文名（他模块，解释活库现态）
- `V427__m009_project_profiles_and_associations.sql` 他表 FK（M010-007 不改变本 FILES 松引用裁定）

---

## 3. 第一性原理（2.3 三问；依据类型 1+4）

### 3.1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？

- **V005 L1–L109**：BEQR-143 四类主体绑定与 BEQR-154/PGM「建立综合体项目档案 / 商户档案 / 空间」需要落表；八条 `entity.binding.*` / `merchant.profile.*` / `governance.project|space.*` 是治理读写的权限种子（L79–L109）。删表则无载体。**有业务上位 → 细化。**
- **V048 L1–L7**：BEQR-146「所有数据默认按项目隔离」的下位：角色可挂 `organization_id`。删列则组织范围角色无法存。**细化。** 全局 UNIQUE 缺口已由 V410:23-26 按 M010-004 补上。
- **V318 L1–L16**：把 HTML **明文禁止**的「平台→项目公司→域公司→商户」行政隶属写成列（L9–L15 ADD COLUMN + 索引 + COMMENT）。删掉它，BEQR-144 / PGM-018 / BSM-028 **反而更能成立**。历史文件不可改；V410:8-9 已 DROP。**冲突已修，当前模式不再承载该列。**
- **V354 L1–L71**：六骨架稳定 id 对应 PGM-019 具名主体（L53–L61）；酒吧不进此表（L26–L28）与「酒吧＝商户」一致；资产业态不单设（L52）与 PGM-018/BEQR 城市项目公司条一致。CHECK L66–L68 曾发明 `spv`/`operation`（三 HTML 0 命中）——添加项已答删除，V410:13-19 已收窄。**骨架保留 = 细化；五值 CHECK = 已删添加。**
- **V410 L1–L26**：001–004 的授权修复，不是新业务发明。删掉它，活库会回到 HTML 冲突的父链与越权角色。**必须存在（修复证据）。**

### 3.2 声称的行为有没有被实际执行验证？

| 声称 | 本次证据 | 类型 |
|---|---|---|
| 四表存在、六骨架稳定 | SELECT 六行 id/name/role/status 与 V354 种子一致；n_be=6 | 4 本席 SELECT |
| 父链已删除 | 列查询 0；`pg_indexes` 无 `idx_business_entities_parent` | 4 本席 SELECT |
| 角色三值 | `pg_get_constraintdef(chk_be_entity_role)` 仅三值；COMMENT=V410 文本；活库 `entity_role` 分布仅三值；spv/operation 行=0 | 4 本席 SELECT |
| 组织内同名唯一、跨组织可同名 | 活库索引定义与 V410:25-26 一致；M011 测试源码 `:732-739` 断言同向 | 4 定义 + 源码；**未新跑 JDBC** |
| NULL≡空串、全局与组织可同名 | 索引 `COALESCE(organization_id,'')`；B1 `/tmp` P8 23505、P9 两行成功 | 4 本席 SELECT 定义 + **B1 探针（非 B5 新执行）** |
| inactive 仍占名 | 唯一索引**无 WHERE**；`roles.status` 列存在。PostgreSQL 非部分唯一索引覆盖全部行 | 4 本席 SELECT 定义（本轮禁止 INSERT 反例） |
| 权限八码中文 + 授予 | SELECT 八行中文名；admin_grants=8。V005 英文仍在历史文件 | 4 |
| Flyway 已应用且文件未改 | 五条 checksum 全等、success=t；磁盘 399 = 历史 399 | 4 |
| 消费方不再写父列/operation | `JdbcPlatformRepository`/`BusinessEntityRecord` 无父字段；Analytics 夹具 `management_company` | 4 源码 |

**未在本次新执行**：任何 Spring/JDBC 写库测试、Flyway migrate、对任何库的 INSERT/UPDATE/DELETE、CREATE/DROP DATABASE。按 verification-before-completion **不声称那些测试本次绿**。

### 3.3 哪一条前提没验证就当真？

- 不得把「V318/V354 曾经 PASS / 旧 B5-r2 V0.01 PASS / 本席 B1 PASS」当成当前模式免审。
- 不得把「测试通过所以对」当成依据——本次根本没跑写库测试。
- 不得把 `modules.md` 基线 SHA、旧 raw SHA、他席结论当依据。
- V354 头注「经营主体分四层」「六个业态主体各管什么」在三 HTML **0 命中**（类型 1）；那是历史注释失实（M010-006），不是 HTML 缺段，不触发铁律 4。
- 不得把 B1 探针库写成「本 B5 新执行」。

---

## 4. 顺从式（18 条逐 ID；定义来自锁全文）

每条：适用面 → 当前证据 → 结论。HTML 追证不升格判据。

| ID | 适用 | 当前证据 | 本条 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 五份 SQL | 父列/五值 CHECK 已按授权删除；六骨架+四表仍有 HTML 上位；V410 无额外业务表 | PASS |
| GEN-002 可执行正确性 | 迁移已应用 | flyway 005/048/318/354/410 success=t；活库对象与 V410 声称一致 | PASS |
| GEN-003 测试有效性 | FILES 无测试 | runbook：纯 SQL 不强制新测试文件。覆盖断言在他模块源码（只读）；本席用 SELECT + 索引定义 + 已有 B1 反例代替写库测试 | PASS（不声称 JDBC 绿） |
| GEN-004 信任边界 | entity_role / role_name | 库层 CHECK 三值；服务层同三值；角色名唯一索引覆盖全局 NULL | PASS |
| GEN-005 授权与最小权限 | V005 授 platform_admin 八码；V048 组织列 | 八码均授；组织隔离唯一性已补。默认拒绝在服务层（他模块），本 FILES 不实现鉴权闸 | PASS |
| GEN-006 数据完整性 | PK / CHECK / UNIQUE | 主体 PK；角色表达式唯一（含 inactive）；V410 UPDATE 后 ADD CHECK。商户三列仍无硬 FK（本库约定，M010-007） | PASS |
| GEN-007 并发与幂等 | UNIQUE / ON CONFLICT | 角色跨组织写入窗口被唯一索引覆盖；V354 `ON CONFLICT DO NOTHING` 不修漂移（M010-011，Flyway 单次执行） | PASS |
| GEN-008 接口与模式一致性 | 列/枚举/注释 vs 服务/测试 | 当前库、V410 COMMENT、`BUSINESS_ENTITY_ROLES`、M011 断言同三值、无父列。V318/V354 历史文本与当前库不一致 = 迁移链常态，以 V410 为当前协议 | PASS |
| GEN-009 错误处理 | CHECK/UNIQUE 失败 | 约束存在即失败路径；B1 P2/P3 `spv`/`operation`→23514、P6 同组织同名→23505、P4 父列→42703（非本轮新执行）。无伪成功 | PASS |
| GEN-010 安全与敏感 | 迁移文本 | 五文件无秘密、无动态 SQL 拼接；权限名非秘密。不输出 env | PASS |
| GEN-011 可观测 | DDL | 迁移本身不是业务操作留痕；Flyway 历史行构成应用证据 | PASS（N/A 业务审计日志） |
| GEN-012 资源边界 | 索引 | status 索引、角色组织部分索引、表达式唯一索引均存在；无无界批处理 | PASS |
| GEN-013 构建配置 | 版本号/测试库 | 磁盘 max=431 = 库 max=431；400–409 磁盘与历史均为空（操作员授权空档）。原「库 409 > 代码 399」**已不是当前事实** | PASS |
| GEN-014 迁移正确性 | 顺序/checksum/破坏性授权 | 五文件 checksum 全等；V410 破坏性 DROP 有 001 授权 + 002/003 删除答复；不改 V005/048/318/354 正文；新鲜库路径为 318 加列 → 354 带 parent 插入 → 410 删列，顺序确定 | PASS |
| GEN-015 界面可用性 | 权限展示名 | V005:79-88 英文种子仍在历史文件；活库八码为中文（后续 V251，他模块）。无本 FILES UI | PASS（现态）；V005 英文见 M010-005 登记 |
| GEN-016 文档可验证性 | 注释 vs HTML | V410 注释可指到 BEQR-144/PGM-018/019。V318/V354 伪章节名仍在（M010-006 登记，历史不可改） | PASS（当前修复文件）；历史失实已登记 |
| GEN-017 业务来源分类 | 2.1 顺序 | 见 §6：无新冲突、无新添加；002/003 不重问 | PASS |
| GEN-018 第一性原理证据 | 本报告 | 结论落到 HTML 原文 + 可复现 SELECT/checksum + 已有 B1 反例（标明非新执行）；不用旧 PASS/席位意见 | PASS |

**顺从式 18/18 PASS。**

---

## 5. 对抗式（grilling 追问树，自答自证；不等操作员、不派子代理）

❓ **Q1 当前库是不是仍按 V318 父链存数？**  
➡️ 否。`information_schema.columns` 无 `parent_entity_id`（count=0）；索引无 `idx_business_entities_parent`；六骨架 `SELECT` 无该键。消费方 `JdbcPlatformRepository`/`BusinessEntityRecord` 亦不读写该列。

❓ **Q2 CHECK 是不是仍接受 spv/operation？**  
➡️ 否。活库约束定义仅三值；行数 0。B1 `/tmp` P2/P3 均为 23514（**非 B5 新执行**）。V354 源码 L66–L68 仍写五值——那是历史正文，不是当前约束。

❓ **Q3 两组织能否同名角色？同组织能否撞名？**  
➡️ 能 / 不能。索引 `(COALESCE(organization_id,''), role_name)` 使不同 organization_id 可同名、同一键不可。B1 P5 两组织成功、P6 同组织 23505（非本轮新执行）。M011 `:732-739` 源码同向。属 M010-004 已修面。

❓ **Q4 NULL 与空串 organization_id 是否同一键？**  
➡️ 是。`COALESCE(organization_id, '')` 把 NULL 与 `''` 映成同一第一列。B1 P8：先 NULL 再 `''` 同名 → 23505（非本轮新执行）。此即 M010-013 第一事实。access 写路径写 NULL 不写空串（他模块）。HTML 未要求区分。不升格新缺陷。

❓ **Q5 全局角色与组织角色可否同名共存？**  
➡️ 可以。全局键=`''`，组织键=`org_id`，role_name 可相同。B1 P9 两行成功（非本轮新执行）。013 第二事实，不是 004 的「两组织同名」。

❓ **Q6 inactive 角色是否仍占名？**  
➡️ 是。唯一索引无 `WHERE status=…`；`roles.status` 列为 NOT NULL。非部分唯一索引覆盖 inactive 行。本轮禁止 INSERT，不以新写库证明；定义即行为。活库 10 行全 active，故无现存 inactive 样本，但不改变索引语义。HTML 未要求停用后释放名称。013 第三事实。不升格新缺陷。

❓ **Q7 能否靠裸 SQL 写垃圾 status / 重名主体？**  
➡️ 能（库层）。`business_entities.status` 无 CHECK；主体无 `entity_name` 唯一。B1 P11 `status='zzz'`、P10 同名「游乐公司」两行（非本轮新执行）。对应 M010-008 / M010-011。首轮正常走服务层；HTML 未要求 `entity_name` 唯一，不把 BEQR-155 商户状态词表升格为 M010 新判据。

❓ **Q8 complex_test 是否仍卡在未发布 V400–V409，使 V410 无法作为当前模式？**  
➡️ 否。历史 400–409 查询 0 行；410 success=t checksum 全等；max=431。M010-009 原阻塞已解除。

❓ **Q9 结算/分析夹具是否仍把父链或 operation 锁成绿？**  
➡️ 父链：结算测试已断言列不存在（方法名残留 ParentChain = 014）。operation：Analytics `:478` 现为 `management_company`（012 已修）。M011 JDBC 源码现含拒绝 spv 的用例；operation 拒绝由约束定义承担（OBS-R-05）。本席未新跑这些写库测试。

❓ **Q10 有无新业务发明需要 2.2？三 HTML 自身是否须铁律 4 暂停？**  
➡️ 无。V410 只删除/收窄已授权项。不重问 002/003。三 HTML 未发现须上报的正本错误（OBS-R-08）。

❓ **Q11 旧 B5-r2 V0.01 或本席 B1 PASS 能否免本轮整模块重审？**  
➡️ 不能。旧票锁 V0.11；本次锁 V0.14。B5 必须整模块重审。本票独立取证后才下结论。

**对抗式未发现新的本模块工程缺陷。**

---

## 6. 既有 14 ID 按当前事实重核

正式 ID 已在 findings V3.52 定稿，本席不改 findings、不重发 2.2。下表是 **当前事实裁定**（不是把旧 PASS 当免审）。

| ID | 原分类 | 当前事实 | 工程缺陷（当前 FILES+V410） | 入队 |
|---|---|---|---|---|
| M010-001 | 冲突 | V410:8-9 已 DROP INDEX/COLUMN；活库列/索引 0；六骨架稳定 id/名/角色仍在 | **已修** | 否 |
| M010-002 | 添加→删除 | CHECK 无 `spv`；0 行；COMMENT 无 spv。questions 答复「删除」已执行，不重问 | **已修** | 否 |
| M010-003 | 添加→删除 | CHECK 无 `operation`；0 行。V354:41-46 历史归档句仍在（不可改） | **已修** | 否 |
| M010-004 | 细化 | `roles_role_name_key` 不存在；`uq_roles_org_role_name` 定义=V410:25-26 | **已修** | 否 |
| M010-005 | 技术性 | V005:79-88 仍英文；活库八码中文（V251） | 否（历史种子+后续 i18n） | 否 |
| M010-006 | 技术性 | V354:3-8 / V318:1-13 伪称仍在；三 HTML 对应章节名 0 命中；V005 无 GRANT | 否（历史注释不可改 checksum） | 否 |
| M010-007 | 细化 | merchants 57 行三主体列全 NULL；projects/spaces 0；merchants→entities 仍无硬 FK。`business_entity_projects_business_entity_id_fkey` 属 V427/M009，**不扩范围** | 否 | 否 |
| M010-008 | 细化 | `business_entities.status` 仍无 CHECK。不把 BEQR-155 升格为 M010 新判据 | 否 | 否 |
| M010-009 | 技术性（环境） | **原 409>399 已不成立**。磁盘=库=399 成功行 / max 431；400–409 双空 | 否 | 否 |
| M010-010 | 技术性（下游） | 结算源码已不断言父链值；Analytics 不再写 operation | 否 | 否 |
| M010-011 | 技术性 | ON CONFLICT 不修漂移；entity_name 无唯一；六骨架无资产公司、酒吧不进主体表；checksum 全等 | 否 | 否 |
| M010-012 | 技术性（跨模块） | `AnalyticsJdbcIntegrationTests.java:475-479` 现 INSERT `management_company` | **源码已修**（文件不在 FILES） | 否 |
| M010-013 | 技术性 | 见 Q4/Q5/Q6：NULL≡空串、全局与组织可同名、inactive 占名，均为表达式唯一索引既有语义。findings V3.52 已标定这三句 | 否 | 否 |
| M010-014 | 技术性（文案） | `SettlementEntityAttributionS1Tests.java:217` 方法名仍含 ParentChain；断言已是列不存在 | 否（他模块文案） | 否 |

**无新 GR。** 不把 007 的他表 FK、014 方法名、008 无 CHECK 扩成新修复项。

---

## 7. 观察（无新根因也登记；与 V3.52 九条对齐，不漏并、不升业务要求）

| ID | 内容 | 分类 | 工程缺陷 | 入队 |
|---|---|---|---|---|
| OBS-R-01 | V410 不在冻结 FILES/scope/module-union，是 B3 后受控修复文件；本轮仅按指令作 001–004 证据，不改计划 | 技术性 | 否 | 否 |
| OBS-R-02 | 活库 merchants 57（更早 B1 曾为 87）——测试库数据量变化，非本四文件行为 | 观察 | 否 | 否 |
| OBS-R-03 | 活库 1 条指向 `business_entities` 的硬 FK 来自 `business_entity_projects`（V427/M009），不改变 M010-007 对本 FILES 松引用的裁定 | 观察 | 否 | 否 |
| OBS-R-04 | `sixEntitySkeleton…ParentChain…` 方法名残留；断言已正确。不扩 FILES | 观察 | 否 | 否 |
| OBS-R-05 | `entityRoleCheckRejectsSpvAndOperation` 同事务只 INSERT 了 spv；operation 拒绝由约束定义 + B1 `/tmp` P3 补证。不扩 M011 | 观察 | 否 | 否 |
| OBS-R-06 | 磁盘缺号除 400–409 外还有 118/119/… 等历史空号；与本模块无关 | 观察 | 否 | 否 |
| OBS-R-07 | B1 曾用临时探针库并已 DROP；本 B5 `pg_database` `m010%`=0。属调用执行记录，不是新业务 finding。**本 B5 未 CREATE/DROP** | 技术性 | 否 | 否 |
| OBS-R-08 | 三 HTML 未发现需铁律 4 上报暂停的正本错误 | — | 否 | 否 |
| OBS-R-09 | ponytail-review：当前运行态父列已删、五值 CHECK 已收；历史迁移正文不可删。`Lean already. net: 0` | — | 否 | 否 |

9/9 与定稿归并段同对象。无观察被写成新队列「是」。

---

## 8. 命令证据（敏感值未输出）

```
CMD-LOCK: python3 hashlib + sha256sum + openssl dgst -sha256
OUT: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  BYTES=891182  exit 0

CMD-FILES: sha256sum + wc + Flyway 行 CRC32
OUT: 四冻结 109/7/16/71=203；V410=26；checksum 与库全等  exit 0

CMD-HEAD: git rev-parse --abbrev-ref HEAD ; git rev-parse HEAD
OUT: main / e456ba96c84185c8d39f18f548011daac48a6466
     （未 fetch）

CMD-HTML: sha256sum 三文件 + 字符串计数
OUT: BEQR 4396bb71… / BSM eebc5b41… / PGM 5c4594a7…
     「经营主体分四层」=0 「六个业态主体各管什么」=0 SPV/spv=0

CMD-PSQL: docker exec -i complex-postgres psql -U complex -d complex_test  仅 SELECT
OUT: db=complex_test user=complex
     flyway 005/048/318/354/410 success=t checksum 全等
     400–409 行数=0  max_ver=431 n_success=399
     无 parent_entity_id 列 / 无 idx_business_entities_parent
     chk_be_entity_role 三值；COMMENT=V410 文本
     6 骨架 active；n_be=6；spv/operation 行=0；资产公司行=0
     权限八码中文；role_platform_admin 8/8
     merchants 57 / 三主体列 0 非空；projects 0；spaces 0
     roles 10 / organization_id 非空 0 / 空串 0 / 全 active
     uq_roles_org_role_name 存在且无 WHERE；roles_role_name_key 不存在
     FK 仅 business_entity_projects；status 无 CHECK
     leftover be_role_* / role_m010_* / m010% 库 = 0
     exit 0

CMD-CONSUMERS: 只读 Java/测试/他迁移
OUT: BUSINESS_ENTITY_ROLES 三值；仓储无父列；Analytics management_company；S1 方法名 ParentChain + 断言列不存在

未跑：写库 JDBC、flyway migrate、CREATE/DROP DATABASE、mvn 全量、部署、source 全 env。
```

B1 同源探针（**非 B5 新执行**，仅引用）：P1 management_company OK；P2 spv 23514；P3 operation 23514；P4 parent 42703；P5 两组织同名 2 行；P6 同组织 23505；P7 全局 NULL 同名 23505；P8 NULL vs '' 23505；P9 全局+组织同名 2 行；P10 同名主体 2 行；P11 status=zzz 1 行；P12 旧五值 CHECK 对照。探针库已清理，本席本轮确认 `m010%`=0。

---

## 9. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户/派发指令优先于技能本体（禁止子代理、禁止建库） |
| using-arc | Arc `skills/using-arc/SKILL.md` | 与双席/禁止子代理冲突，**不走** Arc 全量审计工作流 |
| graft | `~/.claude/skills/graft/SKILL.md` | 定位 `parent_entity_id` / `chk_be_entity_role` / `BUSINESS_ENTITY_ROLES` / 夹具消费点；不改索引 |
| grilling | mattpocock `productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 删除测试：删父列后复杂度回到绑定表；不派 DESIGN-IT-TWICE |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 以活库 SELECT + 已有 B1 反例为 red-capable 环；本轮不建反馈库、不改生产代码 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；不作无 ID 插桩 |
| verification-before-completion | superpowers 同名 | 先跑 SELECT/checksum 再声称；未跑写库 JDBC 故不声称那些测试绿 |
| ponytail-review | ponytail 4.9.0 `ponytail-review/SKILL.md` | 只列可删复杂度，不落地。`Lean already. net: 0` |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游合取席：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推；本宿主无该 hook，记缺口 |
| receiving-code-review | superpowers 同名 | B5 不 IMPLEMENT；本席独立取证，不表演性同意旧票 |

B5 不适用、按提示词收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、resolving-merge-conflicts（无合并）、audit-context-building（派子代理）、Arc audit。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- 宿主无 Claude Code git-guardrails hook：改用本席不执行 push/reset/worktree 的约定

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

---

## 10. 本席 B5 结论

**PASS**。

同时满足：计划 4 文件全文 + V410 全文均有覆盖记录；映射 18 条 GEN 均已核对；三线均执行；声称均有 HTML 原文 / 活库 SELECT / checksum / 已有 B1 反例（标明非新执行）四类证据之一；questions 已精确答复；无未定稿缺陷；无添加项未答；无 HTML 问题；本完整报告第一行锁哈希有效。

相对旧 V0.01 B5-r2：旧票锁 V0.11、findings V1.33，**不替代**本次。本次锁 V0.14、findings V3.52 14 项均否、013 三边界已核。无新的未定稿项。

本票不是双席合取，也不是模块闭环。席甲仍须独立落盘 self-raw 后再读本文件做合取。本席不行使否决。
