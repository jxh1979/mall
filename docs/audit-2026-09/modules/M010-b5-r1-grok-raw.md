CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B5-r1 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 PASS、B3 自报或历史 B1 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M010 同一 4 文件；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 1/3；LOCK-AMEND-COUNT 1；ledger V2.22；findings V1.30
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 全文一行同值
- 映射：`M010 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`，COUNT=18 unique=18 MISSING=[]；定义从锁自读，不新造判据、不把 HTML 锚点扩进本模块映射
- 正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；开工时工作区 after tree=`2432fd77b8bdaa6cc4dc2ed207b12dade54519c9`（临时索引 `GIT_INDEX_FILE=mktemp-dir/index` 下 `git add -A && git write-tree`，不替换主索引；对象类型 `tree`）；B3 修复未提交，审计对象=当前工作区 4 历史迁移全文 + 新迁移 V410 + 活库实态
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m010-b5-verify`，结束清理）；不改任一 `complex_test*` 库
- 未读取：`M010-b5-r1-self-raw`（当前亦不存在）。B1/B4 raw 与 findings V1.30 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V1.30 顶部「001–004已修转否，进入B5-r1」；005–011 仅登记（含 009：新迁移自 V410 起编号、测试库处置已记 ledger）；questions/M010-002.md 与 M010-003.md 操作员答复均为「删除」
- 技能（V0.17 收窄，按实际读取与已执行意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–004 修复后行为独立复验成立；005–011 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。

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
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V2.22；2889 行 / 422996 字节 / SHA `766155cc7c11ac05548fd9a90c346fd8b033187c21bbe745afc3590c17c984ec` |
| findings | V1.30；3297 行 / 494418 字节 / SHA `5706ef278f12c5ba83a00e70ee7b46900d485e61a2f3bf44adcd34fa45a65710` |
| 映射行抽出 | 18 条、无重复、MISSING=[] |
| 计划 FILES | 4 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 203（切分时）；当前工作区 4 文件 `wc -l` 合计 **203**（历史正文未改）+ V410 **26** 行 |
| questions | M010-002、M010-003 均有「## 操作员答复」且首行精确「删除」；无未答问询 |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

HTML 原文（本模块映射不含这些锚点，只作 001–004 已定稿冲突/添加项的解释权引用，不 LOCK-AMEND）：

- BEQR-144（`business-enum-quick-reference-v0.1.html:227`）：「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。」
- PGM-018（`platform-governance-map-for-operations-v0.1.html:415`）：顶层平台 / 物理项目 / 域主管公司 / 城市项目公司；「一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。」
- PGM-019 主体表：娱乐平台公司、独立酒吧商户、游乐公司、传媒公司、中央餐饮公司、城市项目公司——无 SPV、无「通用运营主体」。
- BSM-028：同上多对多，不是固定「平台→项目公司→域主管公司→商户」隶属链。

三 HTML 全文检索：`经营主体分四层`=0、`六个业态主体各管什么`=0、`SPV`=0、`spv`=0。

身份核验：

- `git rev-parse HEAD` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`
- 临时索引 after tree = `2432fd77b8bdaa6cc4dc2ed207b12dade54519c9`（与派发值全等）
- 4 历史迁移相对 HEAD：`git diff --stat` 空（正文未改）
- V410：工作区未跟踪新建文件

### 1.1 4 文件 + V410 当前行数 / 字节 / SHA256（工作区实算，`wc -l` = `\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | git blob |
|---|---|---:|---:|---|---|
| 1 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` | 109 | 3900 | `5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905` | `94692f5a2c53b7c9bd0104a2ea1bafc9b3b499b6` |
| 2 | `backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql` | 7 | 352 | `e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd` | `61018fff6f0d56d259db49e9ea28295736b87d46` |
| 3 | `backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql` | 16 | 1300 | `466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481` | `9f8eeaf9362401dd6c42329aabd9e38e24213755` |
| 4 | `backend/src/main/resources/db/migration/V354__business_entity_master_data.sql` | 71 | 5625 | `c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3` | `b518112269fddd03a71d87161e7fa2edc45af5fb` |
| 5 | `backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql`（B3-r1 新建，本模块修复产物） | 26 | 2074 | `85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1` | `647844c1a40bde9f0f8c1a0835dd4d8b3f8f6a81` |

四历史文件 blob 与 `HEAD:` 路径全等。V410 仅存在于工作区。Flyway CRC32（按「去掉换行后拼接」与库内 checksum 对齐，五文件全部命中）：V005=`413803928`、V048=`-1430035601`、V318=`-705915847`、V354=`-61418191`、V410=`983875231`。活库 `flyway_schema_history` 同值。

---

## 2. 全量三线覆盖表

每文件三线均执行；「无 finding」不等于未审。覆盖范围=计划 4 文件全文 + V410 全文 + 活库 `complex_test`（只读）+ 保留库 `complex_test_pre_v410_20260909`（只读对照）+ 临时探针 `m010_b5_grok_probe`（TEMPLATE 后对抗写入并整库 DROP）。

| 文件 | 行范围 | 第一性原理 | 顺从式 | 对抗式 | 结论 |
|---|---|---|---|---|---|
| V005 | 1–109 | 四表+八条权限种子承载 BSM-028「建立商户档案/项目/空间」与绑定读写；删掉则治理主数据无建表入口 | 建表列与后续消费面（M009 Jdbc 读写四表、权限码仍被 role_permissions 引用）一致；`IF NOT EXISTS`/`ON CONFLICT` 幂等 | 无硬 FK（007 已登记）；权限名字段英文被 V251 覆盖为中文（005 已登记）；`status` 无 CHECK（008） | 现态满足映射 GEN；历史正文不改 |
| V048 | 1–7 | `organization_id` 是组织隔离列；NULL=全局、非空=组织域。无此列则无法按组织切角色 | 列+部分索引 `idx_roles_organization` 仍在；Access `createRole`/`listRoles` 读写该列 | 历史未替换 `roles_role_name_key`（004 根因）；V410 已删该约束并建表达式唯一索引 | 列保留；唯一性由 V410 补齐 |
| V318 | 1–16 | 历史把「平台→项目公司→管理公司」写成 `parent_entity_id` 隶属链；与 BEQR-144/PGM-018 明文冲突。文件因 Flyway checksum 必须保留 | 列/索引/注释仍在历史文件；活库列与索引均为 0（V410 已 DROP） | 对探针 `INSERT ... parent_entity_id` → `column ... does not exist`；Java `backend/src/main` 除 V410 DROP 外 0 处读写 | 历史冲突已用破坏性授权迁移消除 |
| V354 | 1–71 | 六主体稳定 id 是结算归属锚；archive_reason 走软删铁律；CHECK 原含 spv/operation（002/003 已答删除） | 六行种子 id/名称/三角色在活库仍在；工商三字段 NULL；无「资产公司」行；酒吧不进本表 | 历史 INSERT 仍写 `parent_entity_id`（顺序上 V318 先加列、V410 再删列，新库可迁）；注释伪称 HTML 章节（006）；`ON CONFLICT DO NOTHING` 不修漂移（011） | 种子保留；角色闭集由 V410 收三值 |
| V410 | 1–26 | 001 删父链列/索引；002/003 UPDATE 坍缩后 CHECK 收三值并改 COMMENT；004 删全局 UNIQUE、建 `UNIQUE (COALESCE(organization_id,''), role_name)`；编号 V410 跳过未发布 400–409 | 活库 max=410、validate 378、checksum 与文件全等；COMMENT 与 PGM-018/019 三角色一致 | 探针：spv/operation/大小写/拼写均拒；两组织同名可插入、同组织/双 NULL/空串与 NULL 碰撞均拒；归档角色仍占名 | 修复落地；见观察项，无新缺陷 |

只读依赖（验证消费面，不扩成他模块全审）：

- M009：`BusinessEntityRecord` 已无 parent 字段；`JdbcPlatformRepository.saveBusinessEntity` INSERT/SELECT 不含 `parent_entity_id`；`PlatformService.BUSINESS_ENTITY_ROLES` 三值；商户三主体列仍松引用。
- M011：`PlatformJdbcIntegrationTests` 五处 `saveBusinessEntity` 夹具均为 `management_company`；新增 `parentEntityColumnIsRemovedFromBusinessEntities`、`entityRoleCheckRejectsSpvAndOperation`、`organizationScopedRolesMayShareNameAcrossOrganizations`。
- 结算：`SettlementEntityAttributionS1Tests.sixEntitySkeleton…` 断言列不存在；`RevenueStreamAttributionJdbcIntegrationTests` 已删父链可读回方法。
- Access：`JdbcAccessRepository.createRole/listRoles` 读写 `organization_id`；全局 DuplicateKey → `GlobalExceptionHandler` 409 `STATE_CONFLICT`（中文「已存在相同的记录，请勿重复提交」）。

---

## 3. 18 判据逐项

| ID | 适用 | 证据 | 判定 |
|---|---|---|---|
| GEN-001 | 五文件均适用 | 四表+角色隔离+六骨架稳定 id 均有 HTML/结算消费；V318 父链列无需求承载，已按 001 删除；V410 无额外抽象 | 通过（001 已修） |
| GEN-002 | 迁移+活库+测试 | Flyway `Successfully validated 378 migrations`；`Current version … 410`；`Schema … up to date`；相关测试 26/26；活库列/约束/种子与声明一致 | 通过 |
| GEN-003 | 关键分支在 M011/结算伴生测试 | PlatformJdbc 16/0/0、S1 8/0/0、Revenue 2/0/0；探针另执行 operation INSERT 拒绝（测试类因 PG 事务中止只实测 spv，见观察 05） | 通过 |
| GEN-004 | CHECK/唯一/服务层白名单 | 角色码首次可信使用前：服务层三值 + 库 CHECK 三值；非法值在探针被拒；organization_id 空串与 NULL 被 COALESCE 视同（观察 01，应用层写 NULL） | 通过 |
| GEN-005 | V048+V410 角色隔离 | 两组织同名可共存、同组织/全局同名仍拒（探针+测试）；`listRoles(org)` 返回全局∪本组织；默认拒绝越权不在本 SQL 模块实现面 | 通过（004 已修） |
| GEN-006 | 唯一、引用、事务 | PK/表达式唯一索引/CHECK 在活库；Flyway 单迁移事务；无硬 FK（007 已登记）；失败探针均 ROLLBACK，残留 0 | 通过 |
| GEN-007 | 并发/幂等 | 唯一索引覆盖同键并发插入；V005/V354 `ON CONFLICT`；V410 `IF EXISTS/IF NOT EXISTS`；种子不覆盖漂移（011 已登记） | 通过 |
| GEN-008 | 跨层枚举 | 库 CHECK、服务层白名单、V410 COMMENT、六骨架角色一致为三值；历史 V354 CHECK/COMMENT 仍写五值（正文不改，现态已被 V410 覆盖）；夹具已改 management_company | 通过 |
| GEN-009 | 错误 | CHECK/UNIQUE 违例可检测；父列写入立即报列不存在；DuplicateKey 在公共处理器映射 409 中文；探针违例后事务回滚无半成品 | 通过 |
| GEN-010 | 注入/秘密 | 迁移皆静态 SQL；凭证只从 `~/.complex/complex.env` 抽出三变量，报告不写密文；默认 `complex_dev_pwd` 连 127.0.0.1 失败（PWD_LEN=48） | 通过 |
| GEN-011 | 审计证据 | V410 头注绑定 001–004 与操作员裁定；`flyway_schema_history` 有 410 成功行（rank 378，checksum 983875231）；archive_reason 列仍在 | 通过 |
| GEN-012 | 资源边界 | 状态/组织部分索引仍在；无无界循环；本模块无批处理放大 | 通过 |
| GEN-013 | 构建/环境 | `npm run verify:migration-version-order` PASS（新增 1 个迁移，排在 origin/main 最大号 V399 之后）；文件数 378=已应用 378；无 400–409 文件；新 `complex_test` 亦无 400–409 已应用行 | 通过（009 环境已重建） |
| GEN-014 | 迁移正确性 | 顺序 399→410 间隙为操作员裁定；活库自 V001 迁到 V410 成功；checksum 与文件全等；DROP COLUMN 已获 001 方案 B / GEN-014 授权；历史四文件 checksum 未变 | 通过 |
| GEN-015 | 展示中文 | V410 COMMENT 中文；八条权限活库名为「查看/管理业务主体和绑定」等中文（V251）；V005 历史英文（005 已登记）；标识符/单据号除外 | 通过 |
| GEN-016 | 文档可验证 | V410 头注与活库/测试一致；V354/V318 历史注释仍有伪 HTML 章节与「V005 已 GRANT」（006 已登记）；结算 S1 类注释仍写「父链正确」（观察 04，断言已改） | 通过 |
| GEN-017 | 四分类 | 本轮无新添加项；002/003 已答删除并落地；未把业务发明写成技术修复；未因 HTML 沉默删除六骨架 | 通过 |
| GEN-018 | 证据类型 | 结论落到 HTML 原文、Flyway/mvn/psql 输出；不以 B4 PASS 或席位意见代替 | 通过 |

---

## 4. 已定稿 ID 复验（001–004 已修；005–011 仅登记）

### 4.1 M010-001 父链列/索引（冲突，已修）

活库 `information_schema.columns`：`parent_entity_id` 计数 **0**；`pg_indexes`：`idx_business_entities_parent` 计数 **0**。探针 `INSERT ... parent_entity_id` 失败：`column "parent_entity_id" of relation "business_entities" does not exist`。Java 主代码除 V410 DROP 外 0 引用。六骨架稳定 id/名称/角色仍在。对照库 `complex_test_pre_v410_20260909` 仍有该列（max Flyway 409），说明删除发生在 V410 而非改历史。同根因已修，无回归。

### 4.2 M010-002 / M010-003 spv、operation（添加，已答删除，已修）

活库 CHECK：`entity_role IN ('platform','project_company','management_company')`，定义不含 spv/operation。活库 6 行角色分布：platform×1、project_company×1、management_company×4，**0** 行 spv/operation。探针 INSERT `spv` / `operation` / `Platform` / `managment_company` 均 `violates check constraint "chk_be_entity_role"`；三角色插入成功。COMMENT 为 V410 中文三角色说明。同根因已修。

### 4.3 M010-004 组织内唯一索引（细化，已修）

活库：`roles_role_name_key` 约束 **不存在**；`uq_roles_org_role_name` = `UNIQUE (COALESCE(organization_id, ''), role_name)`。对照库仍有 `roles_role_name_key`。探针：两组织同名插入成功；同组织同名、两个 NULL 全局同名均 `duplicate key ... uq_roles_org_role_name`。10 行角色仍全部 `organization_id IS NULL`。同根因已修。

### 4.4 仅登记项（本席复验，不重开修复队列）

| ID | 本席复验 |
|---|---|
| 005 | V005:79–88 仍英文；活库八码 `permission_name` 已是中文（V251） |
| 006 | V354:3–8 伪称 HTML 章节，三 HTML 0 命中；V318:8「V005已GRANT」不实；`complex_app` 对 `business_entities` SELECT/INSERT/UPDATE 为真（V140） |
| 007 | 五表 0 条 FOREIGN KEY；商户三主体列仍在 |
| 008 | `business_entities.status` 0 条 status CHECK；探针可插入 `inactive` |
| 009 | 新 `complex_test`：max=410、n_success=378、400–409 已应用行=0；保留库 max=409 且含 400–409。`verify:migration-version-order` PASS。编号与测试库处置与定稿一致 |
| 010 | 伴生测试已改且本席跑绿（见 §6） |
| 011 | `ON CONFLICT DO NOTHING` 仍在 V354；`entity_name` 无唯一（探针可插入第二行「平台公司」）；六骨架无资产公司；酒吧不在主体表；Flyway checksum 全等 |

---

## 5. 本次 findings / 观察

无新工程缺陷，无新添加项，无 HTML 修改申请，无锁外判据。观察如下（不入修复队列；若与已定稿 ID 同根因已标明）。

| 临时编号 | 位置 | 分类 | 与已定稿 | 说明 |
|---|---|---|---|---|
| GR-M010-B5-01 | V410:25–26；探针空串 vs NULL | 观察 | 004 设计 | `COALESCE(organization_id,'')` 使 `organization_id=''` 与 NULL 同键。Access `createRole` 平台管理员写 NULL 不写空串。非缺陷 |
| GR-M010-B5-02 | 同上；探针 inactive 占名 | 观察 | 004/软删 | `status=inactive` 仍占唯一名，同组织不能复用。与软删留行一致，HTML 未要求释放名称 |
| GR-M010-B5-03 | 同上；全局与组织同名 | 观察 | 004 例示 | 全局「店长」与组织「店长」可共存（COALESCE 两元组不同）。B2 例示即此表达式。`listRoles(org)` 会同时看见二者，属 access 展示面，非本模块新闭集 |
| GR-M010-B5-04 | `SettlementEntityAttributionS1Tests` 类注释仍写「父链正确」；方法名仍含 ParentChain | 观察 | 001 伴生文案 | 断言已改为 `doesNotContainKey("parent_entity_id")` 且 8/8 绿。文件不在 M010 FILES。不升格缺陷 |
| GR-M010-B5-05 | `PlatformJdbcIntegrationTests.entityRoleCheckRejectsSpvAndOperation` | 观察 | 002/003 | 因 PG 违例中止事务，测试只 INSERT spv，operation 靠约束定义字符串。本席探针两条 INSERT 均拒。测试能失败，不构成 GEN-003 缺口 |
| GR-M010-B5-06 | 探针插入 status=inactive | 观察 | **008 同根因** | 无 status CHECK，与 SUBJECT_STATUSES 仍分叉。维持登记 |
| GR-M010-B5-07 | 探针插入重复 entity_name「平台公司」 | 观察 | **011 同根因** | HTML 未要求主体名唯一。维持登记 |

添加项：无。2.2 清单：空。

---

## 6. 命令与真实输出 / 退出码

凭证：仅从 `~/.complex/complex.env` 抽出 `COMPLEX_DATABASE_URL` / `_USERNAME` / `_PASSWORD` 三变量（不 source 整文件、不打印密文）。URL 库名改写为 `complex_test` 或探针名。USER=`complex` USER_LEN=7 PASS_LEN=48。`complex_dev_pwd`（LEN=15）连 127.0.0.1 **失败**：`password authentication failed for user "complex"`（exit 2）——已如实记录，改用抽出的三变量后成功。

### 6.1 身份 / 锁 / 文件

| 命令 | exit | 关键输出 |
|---|---:|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| python3 hashlib / openssl dgst | 0 | 同值 |
| `git rev-parse HEAD` 与 `origin/main` | 0 | `242f487233d028173cb9198eecd079f5d877405c` |
| 临时索引 `git add -A && git write-tree` | 0 | `2432fd77b8bdaa6cc4dc2ed207b12dade54519c9` |
| 4 文件 `git diff --stat HEAD -- <paths>` | 0 | 空 |
| `npm run verify:migration-version-order` | 0 | `MIGRATION_VERSION_ORDER_PASS: 本分支新增 1 个迁移,均排在 origin/main 最大号 V399 之后` |
| 迁移文件计数 | 0 | N_FILES=378 MIN=1 MAX=410；缺 400–409；与已应用 378 相等 |

### 6.2 活库只读（`SET default_transaction_read_only=on`）

`psql -d complex_test` exit **0**。`current_database=complex_test`，`usr=complex`，`ro=on`。

- Flyway：001–410 成功 378 行；max=**410**；400–409 已应用行 **0**；V410 checksum=`983875231` description=`m010 business entity topology and role isolation` installed_rank=378
- `business_entities` 列 10 个：无 `parent_entity_id`；有 `archive_reason`
- 索引：pkey + `idx_business_entities_status`（无 parent 索引）
- CHECK：`chk_be_entity_role` 仅三值
- COMMENT `entity_role`：`主体角色（PGM-018/019）:platform=顶层平台 / project_company=城市项目公司 / management_company=域主管公司（业态主体）`
- 六骨架：`be_platform/platform`、`be_proj_changsha/project_company`、其余四行 `management_company`；工商三字段全 NULL；`资产公司` 计数 0
- roles：无 `roles_role_name_key`；有 `uq_roles_org_role_name`；10 行全 NULL organization_id
- 八条权限名为中文；`role_platform_admin` 八码均在
- 五表 0 条 FK；`complex_app` 对 `business_entities`/`roles` INSERT 为真
- 测试后复读：仍 410 / 6 主体 / 10 角色 / 378 成功 / 探针 id 残留 0

`psql -d complex_test_pre_v410_20260909` exit **0**（只读对照，未改）。max=**409**，n=387，含 400–409；`parent_entity_id` 列计数 1；CHECK 含 spv/operation；有 `roles_role_name_key`；主体角色分布与新库相同（0 行 spv/operation）。

### 6.3 探针库对抗（`CREATE DATABASE m010_b5_grok_probe TEMPLATE complex_test`）

CREATE exit **0**（当时 `complex_test` 无其它会话）。对抗 SQL 包在 `BEGIN…ROLLBACK`（父列探测因 `ON_ERROR_STOP` 在报错处结束该会话，INSERT 未成功）。exit_adv=0；父列探测会话 exit **3**（预期：列不存在）。ROLLBACK 后探针残留实体/角色 0。事后 `DROP DATABASE m010_b5_grok_probe` exit 0。未改任何 `complex_test*`。

对抗结果（真实 PG 报错原文已采集）：

- spv / operation / `Platform` / `managment_company` → `violates check constraint "chk_be_entity_role"`
- 两组织同名 → INSERT 0 1 两次成功
- 同组织同名 → `duplicate key value violates unique constraint "uq_roles_org_role_name"` Key=`(org_m010_a, 店长（M010探针）)`
- 两 NULL 全局同名 → 同上，Key=`(, 全局同名探针)`
- 空串与 NULL 同名 → Key=`(, 空串碰撞探针)`
- inactive 占名后再插入 active 同名 → 重复键
- `parent_entity_id` 写入 → `column "parent_entity_id" of relation "business_entities" does not exist`

### 6.4 现有相关测试（隔离副本，探针库）

第一次：`/tmp` 副本 `mvn test` **未设 JAVA_HOME** → exit **1**：`Fatal error compiling: error: release version 21 not supported`。未跑测试，未连库。已如实记录。

第二次：`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`，cwd=`/tmp/m010-b5-verify/backend`，URL 指向 `m010_b5_grok_probe`。exit **0**，`BUILD SUCCESS`。

```
o.f.core.internal.command.DbValidate : Successfully validated 378 migrations
o.f.core.internal.command.DbMigrate  : Current version of schema "public": 410
o.f.core.internal.command.DbMigrate  : Schema "public" is up to date. No migration necessary.
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0  RevenueStreamAttributionJdbcIntegrationTests
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0  SettlementEntityAttributionS1Tests
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 PlatformJdbcIntegrationTests
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
```

未把未运行的测试写成通过。主仓 cwd 未跑写模式 mvn（主仓若已有历史 surefire 文件，非本席本次写入）。

---

## 7. 技能实用 / 缺口

实际读取并按收窄口径使用：

| 技能 | 路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（父列/角色码/唯一键/空串/归档占名/Flyway 间隙），不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling |
| verification-before-completion | superpowers `verification-before-completion/SKILL.md` | 先跑命令再声称；JAVA_HOME 缺失的失败未折算为业务 PASS |
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

同时满足：5 个审计对象（计划 4 文件 + V410）均有覆盖记录；18 条映射判据均已核对；三线均执行；声称均有 HTML / 可执行产物 / 可复现命令四类证据之一；questions 已精确答复；无未定稿缺陷；无添加项未答；无 HTML 问题；锁行有效。

本票不是双席合取，也不是模块闭环。席甲仍须独立落盘 self-raw 后再读本文件做合取。本席不行使否决。

## M010 第二批 B2-r1 整批归并复核（Grok 席乙）
文件自身版本：V0.02（本段追加；上方 V0.01 B5-r1 全文 26455 bytes 原样保留，不改写第 1 行锁段起点，本段不写独占锁哈希字段行）

- 席位：Grok（固定外审席乙，grok-4.6）。步骤：第二批 B2-r1 归并复核。不是 B5 重审、不是修复、不是模块 PASS、不是 B3。
- 触发：B5-r1 合取 FAIL（席甲 FAIL／本席 PASS）。席甲一条无定稿 ID（CX-M010-B5-01）按运行手册 §8 整批回 B2。本席不因 B5 判 PASS 拒绝复核。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M010 冻结 4 文件 / 18 判据 GEN-001–018）；ledger V2.23 IN_PROGRESS；B3 累计 1/3。
- 正本：`/home/james/complex-management` main `HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）。B3-r1 修复（新迁移 V410 与伴生）在工作区未提交。本席只读核源，不改主索引/源码/业务 HTML/台账/锁；不 commit/push/PR/merge/部署。
- 本席本批 B2：**PASS**
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。三工具同值（`sha256sum` / `python3 hashlib` / `openssl dgst -sha256`）；锁文件 889020 字节；sidecar 全文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未用席甲本批 B2 追加段（当前 self-raw 仍为 V0.01 前缀）。
- 席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本批不行使否决。
- 技能按 B2 收窄：grilling（自问自答取证，不向操作员发问）/ verification-before-completion（先核哈希、源码与 CHECK 探针再给票）/ receiving-code-review（先核源与候选差集再接受或反驳，禁止表演性同意）/ ponytail 与 ponytail-review（最懒可行，不落地代码）。未冒称 batch-grill-me。未派 Arc audit 子代理。

此票只判断 findings.md 顶部 V1.31「当前 M010 B5-r1 合取FAIL（席甲FAIL），第二批 B2-r1 整批候选（M010-012…，尚未定稿）」相对本次两份完整 B5-r1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B5 **PASS** 原文。已修 001–004 与 005–011 不在本批，保持不变。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M010-b5-r1-self-raw.md` | V0.01 | 19855 | `51f6eced096f242a354e8b7fe65cf3bf59b1701b0ca352ab4ef08a43eb35f293` |
| `docs/audit-2026-09/modules/M010-b5-r1-grok-raw.md` 前缀（本席 B5-r1） | V0.01 | 26455 | `11ba869c8d7d73fd30dab4ad4435df8684e667e8fe5d43438b14baa2a44e5d0b` |
| `docs/audit-2026-09/findings.md` 最上候选段 | V1.31 | 497515 | `66f2b8f7996c6f946f9d25874ecfc2ce65bf0b3b88be29da044557b7ae6709c8` |

只读最上「第二批 B2-r1 整批候选」M010-012…014；下方他模块历史原文与 001–011 定稿不重审、不当本批依据。席甲 B5-r1 结论 FAIL（§4 已定稿 11 项复验、§5 判据表、§6 CX-M010-B5-01、§7 观察、§2 主仓实跑含 AnalyticsJdbcIntegrationTests 红）。本席 B5-r1 结论 PASS，观察 GR-M010-B5-01…07，无新缺陷；探针库复验 001–004 成立，未运行 AnalyticsJdbcIntegrationTests。

冻结映射：`M010 → [GEN-001 … GEN-018]` COUNT=18 unique=18。只用既有冻结判据，无 LOCK-AMEND、无 HTML 修改申请、无 2.2 添加项。

### 差集（无遗漏、不改写本席 B5 原意）

| 本席 B5 | 席甲 B5 | 候选 ID | 差集结论 |
|---|---|---|---|
| （B5 未单列。B4-r1 观察 1 同根：AnalyticsJdbcIntegrationTests L477 仍 INSERT `entity_role='operation'`，当时按 B2 伴生清单不入 B4 队列、交 B5 升格） | CX-M010-B5-01 技术性缺陷，建议入队 | 012 | 并入。本席 B5 PASS 未跑该类 ≠ 复核拒绝。独立核源+CHECK 探针后缺陷成立、入队，见重点裁定。 |
| GR-M010-B5-01/02/03 观察，工程缺陷否 | （席甲无对等 CX） | 013 | 并入登记。004 设计说明，不改。 |
| GR-M010-B5-04 观察，工程缺陷否 | （席甲无对等 CX） | 014 | 并入登记。001 伴生文案残留；**不**随 012 同批改名。 |
| GR-M010-B5-05 测试结构说明 | （席甲无对等 CX；M011 拒 spv INSERT 仍绿） | 不新开 | 归既有 002/003。候选未另开 ID，正确。 |
| GR-M010-B5-06 status=inactive | 席甲 OBS-B5-02 | 不新开 | 归既有 008。正确。 |
| GR-M010-B5-07 重复 entity_name | （席甲无对等 CX） | 不新开 | 归既有 011。正确。 |
| （无） | OBS-B5-01 测试库改名保留 / OBS-B5-03 V400–V409 | 不新开 | 归既有 009。正确。 |
| （无） | OBS-B5-04 V410 头注「操作员裁定」 | 不新开 | 过程事实，非 finding。正确。 |

席甲 CX-M010-B5-01 与本席 GR-M010-B5-01…07 均落在 012–014 或既有 002/003/008/009/011。无第三条根因、无新 ID、无 LOCK-AMEND、无 HTML 修改申请、无 2.2。

### 逐 ID 票（012–014）

| ID | 分类 | 工程缺陷 | 修复队列 | 本席票 | 理由（四分类→最懒处置） |
|---|---|---|---|---|---|
| 012 | 技术性（003 的跨模块机械后果） | 是 | 是 | PASS | 见重点裁定。一行夹具 `operation`→`management_company` + 文件版本头。不改被测生产代码。无业务发明。 |
| 013 | 技术性（观察） | 否 | 否 | PASS | 004 表达式唯一索引的三个语义说明，非缺陷。登记不改。 |
| 014 | 技术性（文案） | 否 | 否 | PASS | S1 类注释/方法名残留；断言已改且 8/8 绿。登记。B3 **不得**顺带改该文件。 |

队列「是」=012（1 项，B3-r2 范围，B3 累计 1/3）；「否」=013、014。已修 001–004 与 005–011 保持否。添加 0，questions 0。

### 候选表对本席 B5 观察的误写（明确指出，不改写原意）

1. **编号映射未写错。** GR-B5-01/02/03→013、04→014、05→002/003、06→008、07→011，与本席 B5 §5 原文同根。012 来源写「席甲 CX-M010-B5-01 + Grok **B4-r1** 观察 1」是对的——本席 **B5 没有**这条观察（§6.4 只跑了 PlatformJdbc 16 + 结算 S1 8 + Revenue 2，未跑 AnalyticsJdbcIntegrationTests）。定稿不要改写成「Grok B5 支持入队」。
2. **013 引导句「004 索引语义说明」压缩了 02。** 01=空串与 NULL 同键，02=inactive 占名（软删留行，不只是索引语义），03=全局与组织可同名。013 单元格三条事实本身完整，只改引导句即可，不拆 ID。
3. **014「若 012 入队同批顺带改名亦可，不强制」。** 这是把登记项预开进 B3。012 与 014 不同文件、不同根因。本席裁定：B3-r2 **禁止**改 `SettlementEntityAttributionS1Tests`。014 保持否。不构成否决。

### 重点独立裁定

#### 012 AnalyticsJdbcIntegrationTests L475–478 夹具直写 `operation`（入队）

本席不为合取同意席甲票，按源码与探针独立判定。

四分类（冲突→细化→技术性→添加）：V410 CHECK 已与 HTML 三角色一致，不是新的 HTML 冲突；不是 HTML 原则未落到本模块迁移；不是新业务能力。余下是 003 收窄 CHECK 后、B3-r1 伴生清单（M011 六处）漏掉的 JDBC 夹具，分类 **技术性**。

位置（工作区现文）：`backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java:467–478` `merchantSummariesRollupAggregatesRealSources`：

```
VALUES ('org_m4', 'M4主体', 'operation', 'm4-code', 'M4', 'vault://m4', 'active', now(), now())
```

文件无版本头。该类测的是商户汇总滚动，`entity_role` 只为满足 `merchants.business_entity_id` 存在性，不是分析模块的角色闭集测试。

本席类型 4（`complex_test`，只抽出三变量，`BEGIN…ROLLBACK`，事后残留 0）：

- Flyway 354/410 success=t；`chk_be_entity_role` = `IN ('platform','project_company','management_company')`。
- 按夹具同列 INSERT `entity_role='operation'`（id=`org_m4_b2probe`）→ `ERROR: new row for relation "business_entities" violates check constraint "chk_be_entity_role"`，psql exit 3。
- 同句改 `'management_company'`（id=`org_m4_b2ok`）→ `INSERT 0 1`，随后 ROLLBACK，exit 0。
- `id IN ('org_m4_b2probe','org_m4_b2ok','org_m4')` 计数 0。

席甲 §2 主仓实跑该类 29 条中 `merchantSummariesRollupAggregatesRealSources` ERROR，与上列因果链同构。本席 B2 未在主仓跑写模式 mvn、未重复 B5 套件；红的机制已由源码+CHECK 探针独立成立。GEN-003：该用例在目标行为断言前因 CHECK 违例 ERROR，测试未执行目标行为。GEN-008：测试夹具枚举与迁移/库 CHECK 不一致。

与 003/010 的关系（故 **新开 012，不重开 003/010**）：003 的 CHECK 收三值已修并经 B4 双 PASS，队列保持否。010 是「下游测试会红」的登记项，定稿否，B3 伴生收窄为 M011 六处——遵守当时边界，不是漏改 V410。漏的是伴生清单本身。001–011 永久不复用、本批保持不变。禁止把本项推给分析模块以后再修：与首批 001「禁止 010 逃逸到各自模块」同一口径。

工程缺陷：**是**。队列：**是**。无业务发明：夹具角色坍缩为已有 `management_company`，与 003 存量 UPDATE / M011 六处同法。

最小处置（ponytail 最懒，且是最懒）：

- 只改 L477 `'operation'` → `'management_company'`。
- 文件无版本头，加 `// 文件版本：V0.01`（与 PlatformJdbc / S1 伴生文件同一形式）。
- **不**改被测生产代码（`JdbcAnalyticsRepository` 等）。
- **不**新开测试文件、不在 M011 再加一条。TDD：红＝该用例现状 ERROR；绿＝改后该类通过。
- **不**改方法名、不改商户夹具、不改 `ON CONFLICT`、不顺手改 014、不改前端 vitest mock。

票：**PASS（是/是）**。

#### 012 是否还有其它遗漏的下游夹具

本席独立扫 JDBC `INSERT INTO business_entities`（测试树）与 `ops/`：

| 位点 | 角色字面量 | 判定 |
|---|---|---|
| AnalyticsJdbcIntegrationTests:477 | `'operation'` | **本 ID，入队** |
| SettlementInvoiceListJdbcIntegrationTests:243–244 | `'management_company'` | 席甲所称已改，属实 |
| LeasingSignAtomicityJdbcIntegrationTests:120–121 | `'management_company'` | 席甲所称已改，属实 |
| PlatformJdbcIntegrationTests:460 | `'management_company'` 合法插入 | 003 伴生，保持 |
| PlatformJdbcIntegrationTests:463 | `'spv'` **拒绝**断言 | 003 的能红测试，**不得**改掉 |
| SettlementEntityAttributionS1Tests:244–245 | `'managment_company'` 拼写错误拒绝 | 001 伴生拒绝路径，保持 |
| `ops/` | 无 `entity_role`×`operation`/`spv` | 无第二处 |

席甲「测试树与 ops 中直写 operation/spv 的仅此一处」在 **JDBC 存活夹具** 意义上成立。本席另扫到两处前端 vitest mock（`apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:12`、`GovernanceTopologyPage.test.tsx:9` `entity_role: "operation"`）——不命中 V410 CHECK、本批两席 B5 均未列为 finding，**不另开 ID、不并入 012**。`IdempotencyServiceTests` 的 `"operation"` 是幂等 `operationId`，不动。`PlatformServiceTests` 的 `"spv"` 是 M009 白名单拒绝用例，不动。e2e `areaType: "operation"` 是空间类型，不是主体角色。历史迁移 V318/V354 正文不改。

#### 013 GR-B5-01/02/03 并入登记（否）

三条都是 004 `UNIQUE (COALESCE(organization_id,''), role_name)` 的既有语义，不是新闭集：

- 01：`organization_id=''` 与 NULL 同键。Access `createRole` 写 NULL 不写空串。
- 02：`status=inactive` 仍占名，与软删留行一致；HTML 未要求释放名称。
- 03：全局与组织可同名（B2 例示即此表达式）；`listRoles` 展示面属 access。

分类技术性观察。缺陷否、队列否。三事实可共一个登记 ID，不拆、不改索引。合规。

#### 014 GR-B5-04 文案残留（否，且禁止顺带）

`SettlementEntityAttributionS1Tests.java:36` 类注释仍写「稳定 id + 父链正确」；`:217` 方法名仍含 `ParentChain`。`:230–231` 断言已是 `doesNotContainKey("parent_entity_id")`。文件已有 `// 文件版本：V0.01`，不在 M010 FILES。本席 B5 8/8 绿。GEN-016 的「旧证据写成当前事实」在本模块 FILES 内才强制入队；他模块注释扫尾不是 012 的机械红。缺陷否、队列否。候选「顺带改名亦可」删掉。合规。

### 是/否队列合规性

- 入队 012：本模块修复直接导致的仓库内红测试，有冻结判据 GEN-003/008，有本批可修实现面（一行夹具），无业务发明。
- 登记 013/014：无执行面红、无 HTML 新规则、不扩 004/001。不把登记项塞进 B3-r2。
- 001–011 不动。

### 最小修正意见（定稿措辞；**不**构成否决）

队列已与本席票一致（是=012，否=013/014），故整批 PASS，**不**要求 B2-r2。定稿时仅改表内措辞，不改队列：

1. 012 工程缺陷定为「是（B5 席甲升格；本席 B4-r1 观察 1 同根，B5 未跑该类故未列入）」。来源保持 CX-M010-B5-01 + B4-r1 观察 1，不要写成 Grok B5 已支持入队。
2. 012 处置写死：只改 AnalyticsJdbcIntegrationTests L477 + 加版本头；TDD 用该用例自身红绿；不改生产代码；不并前端 mock。
3. 013 引导句改为「004 表达式唯一索引语义（空串/NULL 同键、inactive 占名、全局与组织可同名）」。
4. 014 删除「同批顺带改名亦可」；写明 B3-r2 不改 S1 文件。
5. 已修 001–004 与 005–011 不动。B3-r2 只修 012 及归因的这一行夹具；不改 HTML/判据/权限种子/V410/历史迁移。

### 本席实跑（B2，只读 / 事务回滚）

- 锁文件三工具：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）
- 输入 SHA/字节：self-raw 19855/`51f6eced…f293`；本文件前缀 26455/`11ba869c…5d0b`；findings V1.31 497515/`66f2b8f7…09c8`
- HEAD=`242f487233d028173cb9198eecd079f5d877405c` = origin/main
- 夹具与 JDBC INSERT 位点、ops、前端 mock：仓库 grep + 读源
- `complex_test` 只读：Flyway 410、CHECK 三值
- `BEGIN` INSERT `operation` → `chk_be_entity_role` 违例，exit 3；`management_company` INSERT+ROLLBACK exit 0；残留 0。凭证只抽三变量，事后删除 `/tmp` env。未改任一 `complex_test*` 持久行。
- 未在主仓跑写模式 mvn；未建长期 /tmp 工程副本。

### 结论

本席第二批 B2-r1：**PASS**

012 入队（003 漏并的 JDBC 夹具，一行改 `management_company` 即最懒）；013/014 否队列合规；差集无漏并。候选 014「顺带改名」与 013 引导句压缩属定稿措辞，不改队列。无添加项需 2.2。无 HTML 修改申请。无条件通过。本票只覆盖本批归并，**不是**模块 PASS，**不是** B5 合取，**不是**已修 012。
