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
