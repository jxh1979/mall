CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M012 同一 6 文件；`runbook.md` V0.02；控制提示词 160-audit-prompt.md V0.17
- 模块：M012 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M012 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`，COUNT=21 unique=21 MISSING=[]；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=bd445728a6ec7bb36a9e7ff19fe56927410ac61e`。B3-r1 after tree 对象 `50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f` 可解析为 tree；本模块 6 文件当前工作区 blob 与该 after 逐文件全等。当前整工作区临时索引 `GIT_INDEX_FILE=mktemp git add -A && git write-tree`=`6511110568c3d99f6c2fb0bb9a48a5b42eee596e`（findings V1.42 / ledger 顶段 / M011 闭环产物 / M012 B1·B4 raw 已写入，不等于 B3 after，属台账前进，本席未改源）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m012-b5-r1-verify.rVrWN2`，结束已 `rm -rf`；另一次失败的 `/tmp/m012-b5-r1-verify.UtOwvb` 同样已清。workspace vitest 的 `VITEST_CACHE_DIR`/`TMPDIR` 指向 `/tmp/m012-b5-r1-*`，结束后删除）
- 未读取：`M012-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.42 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.42 — 001（绑定页真实键）、002（三态 action）、003（projectId 挂接）、004（中文业态/空间类型/空间状态/表头）、005（夹具现行闭集）、006（去预填与隐藏字段，操作员答删除）、007（手机号非空，操作员答删除）、008（存档/删除申请测试+写路径 catch）已修转「否」；009 仅登记（含拓扑下钻无测试）。当前修复队列「是」=0。B2-r2 定稿禁止项（不得在 B5 升格为缺陷）：三主体选择器、entity.binding.view 创建闸、创建绑定入口、下钻用例、拉 `/space-types`、改 types.ts/statusLabels.ts

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–008 在当前六文件上独立复验成立；009 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger V2.40，2957 行 / 444855 字节 / SHA256 `25873d00d18ccd58d0c7a441fb14ce1bfe4153582a669c527e4a2ef6897b7705` |
| findings | V1.42；3408 行 / 533183 字节 / SHA256 `ab0720f09024aca62ad2a5a9238bb5982cdb77a89649a5f0f29829870d425879` |
| 映射行抽出 | 21 条、无重复、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | BSM-001 `:505` 命中「整体说明：这套系统到底管什么」；BSM-003 `:563` 命中「系统通用能力：数据可改可删、操作有引导、权限分岗」；PGM-001 `:256` 命中「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」；全中文铁律在同锚点段 `:287`。GEN 无 HTML 行 |
| 计划 FILES | 6 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1485（切分时）；当前工作区 `\n` 计数 **1755**（B3-r1 +357/−103 后的现态，不改计划凑数） |
| questions | M012-006.md、M012-007.md 操作员答复段首行均为精确「删除」；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；65856 字节；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |

映射闭集（锁 6391 行原文抽出，21 unique）：

BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001.

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

### 1.1 6 文件当前行数 / 字节 / SHA256（工作区实算，行=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | git blob（work=after） | HEAD blob | 全文 |
|---|---|---:|---:|---|---|---|---|
| 1 | `apps/web/src/admin/pages/GovernanceTopologyPage.tsx` | 244 | 15598 | `403f714dd58b96f33d4da39a6870a6287054d61296fd036b0deada9cca32b342` | `1c363f479b8581416bf50d4390f622dad46c3057` | `f0a2f2e99312d2894a14bbd2965e4722b170fda3` | 已读 L1–L244（文件头 V0.01） |
| 2 | `apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx` | 220 | 11317 | `146d449fa98a19beec0f61c6a602c3d1049a31c9344b5eea57e85f080637868d` | `fba79864cde5e07fcdde8d6c923072f807327442` | `7d2e96b7de0e01990c38e3fbcb8d4f68995a3b51` | 已读 L1–L220（11 `it(`） |
| 3 | `apps/web/src/admin/pages/OrganizationMerchantPage.tsx` | 492 | 30248 | `95328007702f02c6c80f9c846eea72f80a94a5813f6b97349e8759964a4c1d95` | `7d5b3ee9c5ee5f590c30401e0b02c1722efe5d94` | `64b9cc0cc90740d0b6ab8941be3a3703f33a38d4` | 已读 L1–L492（文件头 V0.02） |
| 4 | `apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx` | 331 | 19650 | `a32dc1eb11bef528b532b64888b1752d8a3a63e943b96bdf0e33ea77b816110b` | `823e37e36256078cc9dd7f789dc9824a5c842c40` | `5592efd6cc3f8907a3f4f89452e1d4afe9a0f84b` | 已读 L1–L331（15 `it(`；文件头 V0.02） |
| 5 | `apps/web/src/admin/pages/SubjectBindingsPage.tsx` | 251 | 14279 | `028c97a810da1fce27768dfe3ca8983dbfca8af2d60d85eb3994aa81e5ea3378` | `ce4e76c7d45fa8ac02dbc12c4d7733016ba5fedd` | `c8b6cf293104cd449f20f36af51b981bde4d32bc` | 已读 L1–L251（文件头 V0.01） |
| 6 | `apps/web/src/admin/pages/SubjectBindingsPage.test.tsx` | 217 | 11116 | `81d89f5b917d0ca96d9eefd51e830405c0d2c2960431a40b3d55021bf2b2f1bd` | `2cdcc838ba4844254e5d0de2ac08f488f5fba57a` | `70ea091751ca5b3dadbde5eb78e1f43408d72475` | 已读 L1–L217（11 `it(`） |

合计行 1755、字节 102208。六文件均整文件读完，未按标题抽样。`rg "^\s*it\("` 计 11+15+11=37，与 vitest「Tests 37 passed」一致。

六文件 blob 与 B3-r1 after tree `50c07ff1` 全等；HEAD blob 仍为 before（B3 未提交）。`git cat-file -t`：after 与 before `83d85aa3001e3c4d29475c6c24a6533e0544a5e0` 均为 `tree`。

必要只读依赖（验证本模块调用/字段，不扩审模块；types.ts / statusLabels.ts / adminRoutes.ts / V184 与 HEAD blob 全等，本席未改）：

| 路径 | 用途 | 与 HEAD |
|---|---|---|
| `PlatformController.java` | GET/POST 权限码、`/merchants/{id}/archive\|unarchive\|delete-request\|delete-approve`、`/business-entity-bindings/{id}/change`、`requirePlatformScope`、PII 掩码 | 只读 |
| `PlatformService.java` | `MERCHANT_COOPERATION_FORMS` 六类、`SUBJECT_STATUSES` 五态、`BUSINESS_ENTITY_ROLES` 三值、`SPACE_STATUSES`={active,unavailable}、`changeBusinessEntityBinding` action∈{active,inactive,suspended} 中文报错「启用/停用/暂停」、`saveMerchant` 三主体 `trimToNull` 可空、存档 `requireText(reason)` | 只读 |
| `BusinessEntityBindingRecord.java` | `merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason` | 只读 |
| `SpaceRecord.java` | 有 `projectId`，无 `venueId` | 只读 |
| `StatusChangeRequest.java` | `idempotencyKey, action, reason, evidenceAttachmentIds` | 只读 |
| `MerchantProfileRequest.java` | 档案字段 + 可空三主体/面积/租金/status | 只读 |
| `apps/web/src/customer/i18n/statusLabels.ts`（admin 再导出） | `formatBusinessStatus`；blob `3ac5cefeb5a3c364de707a29479ba661ec076ffa` | SAME |
| `apps/web/src/admin/types.ts` | `GovernanceSpaceRow.venueId` 标旧臆造；`SubjectBindingRow` 七臆造键仍在（本页不用） | SAME `8d26e82118d6f9e607fe85e18ec638a81adc5dd4` |
| `apps/web/src/admin/adminRoutes.ts` | ADM-GOV-001/002/003 权限码（本席未改） | SAME |
| `V184__space_type_dictionary.sql` | 8 码 type_name 种子 | SAME |

`parentEntityId` 在本 6 文件 0 命中（与 M009-012 转入口径一致，不重开）。`venueId`/`businessObjectId` 仅出现在注释（说明不再读取）。

### 1.2 活库 `complex_test`（只抽出三个 `COMPLEX_DATABASE_*`，库名改写为 `complex_test`；USER_LEN=7 PASS_LEN=48；不打印密文）

来源：`/home/james/.complex/complex.env` 按行抽取三键，**未** `source` 整文件。`psql -h 127.0.0.1 -p 5432 -U complex -d complex_test`；`ON_ERROR_STOP=1`；只 SELECT。

| 项 | 实算 |
|---|---|
| `current_database` / `current_user` | `complex_test` / `complex` |
| Flyway 当前版本 | **410** |
| `space_type_dictionary` 8 行（status=active） | building=楼栋、floor=楼层、zone=区块/区域、shop=商铺/铺位、public_area=公共区域、parking=车位/车库、booth=摊位/展位、other=其他。与 V184 种子及页内 `SPACE_TYPE_LABELS` **逐字相同** |
| `spaces` 列 | 有 `project_id`/`space_type`/`status`；**无** `venue_id` |
| `chk_spaces_status` | `active` / `unavailable` |
| `business_entity_bindings` 列 | 含 `merchant_id`/`operation_entity_id`/`contract_entity_id`/`settlement_entity_id`/`status`/`effective_from`/`effective_to`/`change_reason`（另有旧 `source_*`/`target_*`/`binding_type` 列，本页不读） |
| 探针 `type_code='__m012_b5_probe_should_not_exist__'` | count=0 |
| 任一 `complex_test*` 模式/数据 | 本席未改（只读 SELECT；28 个 `complex_test%` 库名仅列出、未连接其它库） |

---

## 2. 全量三线覆盖表

| 文件 | 段 | 第一性原理 | 顺从式 | 对抗式 | 结论 |
|---|---|---|---|---|---|
| GovernanceTopologyPage.tsx | L1–L23 空间类型/状态页内表 | 删掉则 PGM-001 点位类型/状态对用户直出英文 | 8 码= V184/活库 type_name；active/unavailable→可用/不可用 | `shop` 不再走 statusLabels 回退；未知码仍 formatBusinessStatus（闭集外） | 覆盖 |
| 同上 | L25–L107 读四接口 + 缺 entity.binding.view 不请求主体 | 删掉则 BSM-001 基础盘子无入口；缺权不白屏 | ADM-GOV-001 路由不含该码，降级为空是有意 | 测试锁「根本不发 business-entities」 | 覆盖 |
| 同上 | L109–L111 attachedSpaces | 003：真实 SpaceRecord 用 projectId | `projectIds.has(space.projectId)` | 读 venueId 时恒 0，本用例必红 | 覆盖 |
| 同上 | L113–L205 表/抽屉/KPI | 只展示关系、不做变更（边界文案） | 业态 `formatBusinessStatus(venueType)`；商户 TraceLink | 下钻 focusObjectId 无测试（009，不升） | 覆盖 |
| GovernanceTopologyPage.test.tsx | L1–L220 11 例 | GEN-003 关键分支可失败 | 夹具 management_company/shop/project_id；断言「酒吧」「商铺/铺位」「可用」「1 个已挂接」 | 不含 hall/operation/venue_id | 覆盖 |
| OrganizationMerchantPage.tsx | L1–L96 词表/空预填 | 删掉则商户档案无前台 | 业态六值、合作方式六类与后台同词；初始 state 全空 | 无 11 位正则；合作方式可空项 | 覆盖 |
| 同上 | L129–L192 读取/创建 | 006：不传三主体/隐藏字段 | POST 仅 idempotencyKey/merchantName/merchantType/contactName/contactPhone[+cooperationForm] | 无 entity.binding.view 时不再写 org-1 | 覆盖 |
| 同上 | L205–L261 存档删除 + catch | 008：失败可见 | prompt 空理由拦截；archive/unarchive/delete-request/approve 均 try/catch | 取消 prompt 也报「必填」（气味，见观察） | 覆盖 |
| 同上 | L263–L491 三表/引导流/抽屉 | BSM-003 操作有引导 | 业态中文下拉；项目业态 formatBusinessStatus | KPI 聚焦隐藏创建钮 | 覆盖 |
| OrganizationMerchantPage.test.tsx | L1–L331 15 例 | GEN-003 | 夹具 lease/management_company/catering；创建体不含假主体/假证据/1F-A/二期测试商户；空电话拦截；存档空理由+有理由同条；删除申请；404 catch | 未锁 `businessEntityId` 键名本身（观察） | 覆盖 |
| SubjectBindingsPage.tsx | L1–L50 局部类型 + 三态 | 001/002：读真实键、必填 action | BindingRow=后台记录；BINDING_ACTIONS 启用/停用/暂停 | types.ts 七臆造键本页不读 | 覆盖 |
| 同上 | L69–L180 变更提交 | 006/002：原因空初始必填；body=action/reason/idempotencyKey | HighRiskConfirm + entity.binding.manage；catch 写 formError+setGlobalError | 不传假证据；无创建绑定入口（禁止升格） | 覆盖 |
| 同上 | L182–L250 表/KPI/抽屉 | 唯一绑定按 merchantId；合同主体按 contractEntityId；表头「编号」 | 状态列仍 formatBusinessStatus（观察，不升） | 页头仍写「证据附件」（观察，不升） | 覆盖 |
| SubjectBindingsPage.test.tsx | L1–L217 11 例 | GEN-003 | 夹具真实 snake_case + active/inactive；行含 m-1/be_op_1；未选状态/空原因不发 POST；选状态+原因后 body 含 inactive 与原因、不含 evidence-full-admin | 一条提交断言（002+006） | 覆盖 |

三线均执行。无 finding 不等于未审。

---

## 3. 第一性原理（每文件三问）

### GovernanceTopologyPage.tsx（L1–L244）

1. **为何必须存在**：删掉则 BSM-001「哪个综合体、谁在经营、在哪经营」与 PGM-001「哪个综合体、哪个楼层、哪个区域、哪个点位、哪个商户」在后台无拓扑入口。有上位需求。
2. **声称是否执行**：自称只展示关系、读四接口。当前：`attachedSpaces` 按 `projectId`；业态/空间类型/空间状态中文；缺 `entity.binding.view` 不请求主体。KPI/抽屉/缺权不白屏有测试。边界「不做主体变更」成立（无写接口）。
3. **未验证前提**：`focusObjectId` 下钻滚动无测试（009 登记，B2-r2 禁止为本项加用例）。空间类型未知码回退 `formatBusinessStatus`——字典 8 码已覆盖，闭集外不是本页发明。

### GovernanceTopologyPage.test.tsx（L1–L220）

1. **为何必须存在**：GEN-003 要求关键分支可失败。KPI 聚焦、中文业态/类型、已挂接非 0、缺权不请求有上位。
2. **声称是否执行**：11 例 vitest 均绿（CMD-VITEST）。夹具 `management_company`/`shop`/`project_id`；断言「酒吧」「商铺/铺位」「可用」「1 个已挂接」；`not.toMatch(/\bbar\b|\bshop\b/)`。
3. **未验证前提**：夹具 `venue_type:"bar"` 经 `formatBusinessStatus` 变为「酒吧」，与 statusLabels 现表一致。下钻无用例（009）。

### OrganizationMerchantPage.tsx（L1–L492）

1. **为何必须存在**：BSM-001 基础盘子；BSM-003 导语「数据可改可删、操作有引导」。本页是商户建档/存档/删除申请的前台。
2. **声称是否执行**：证据字符串列出 GET 三接口与 POST `/merchants`。引导流名称/业态/电话非空有测试。创建请求不再预填、不传三主体与隐藏字段，与页内「不在本页替代四类主体绑定」一致。存档空理由拦截、有理由 POST、删除申请、写失败 catch 有测试。
3. **未验证前提**：后台 `saveMerchant` 对三主体只 `trimToNull`（假 ID 可落库）——本页已不传，前提转移到「用户不在本页填三主体」。手机号无格式闭集（007 已删正则）。`includeArchived` 开关发查询，但测试夹具不按查询过滤（气味）。

### OrganizationMerchantPage.test.tsx（L1–L331）

1. **为何必须存在**：锁创建校验、KPI、中文词表、008 存档删除。
2. **声称是否执行**：15 例绿。创建体负例锁假值；空电话；存档同条空/有理由；删除申请；404→`setGlobalError(any String)`。
3. **未验证前提**：创建正例不断言 JSON **键名** 不含 `business_entity_id`（只锁假字面量）。实现已省略键。属测试锁偏松，不是产品回归（观察 GR-M012-B5-03）。

### SubjectBindingsPage.tsx（L1–L251）

1. **为何必须存在**：PGM-001「归属、权限、合同和责任」；BSM-001「谁在经营、签了什么合同」。四类主体绑定是底座入口。
2. **声称是否执行**：读 `/business-entity-bindings`，变更 POST `.../change` 带 `action`+`reason`+幂等键。表格读 `merchantId/operationEntityId/contractEntityId/settlementEntityId`。KPI 唯一对象按 merchantId。表头「编号」。原因空初始必填。
3. **未验证前提**：页头 `TEXT.description` 仍写「必须带……证据附件」，表单无附件控件、请求不传 evidence（006 明确不做上传器；009 登记口径）。表/抽屉状态走 `formatBusinessStatus`（active→有效、inactive→未激活、suspended→已封禁），与下拉「启用/停用/暂停」及后台报错词不是同一套中文——002 最小处置只要求下拉+POST action，B2-r2 禁止改 statusLabels，B4 已记观察。无创建绑定入口（HTML 沉默 + 禁止升格）。

### SubjectBindingsPage.test.tsx（L1–L217）

1. **为何必须存在**：GEN-003。真实键、三态、空原因、一条提交断言。
2. **声称是否执行**：11 例绿。行文本含真实 ID；未选状态/空原因 POST=0；确认后 URL 含 `/change`、body 含 inactive 与中文原因、不含 `evidence-full-admin`。
3. **未验证前提**：`expect(body).toContain("inactive")` 不解析 JSON 键 `action`；实现 `body.action` 经 AdminApi `toSnakeCaseKey` 仍为 `action`。非法 action 无用例（002 定稿收窄为一条提交断言）。

---

## 4. 顺从式（21 条逐项）

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BSM-001 | 三页是「基础盘子」入口：主体/商户/项目/空间/绑定 | **通过**。绑定列读真实键，谁在经营/合同/结算在本页可见（ID，后台记录无名称，OBS-02 不升联表）。创建商户不再写伪造主体。HTML「哪个综合体、谁在经营、在哪经营、签了什么合同、谁能操作、谁负责。」（BSM `:509-510`） |
| BSM-003 | 锁摘录仅为 h2+导语「不属于某一个子系统的通用规则」；子条存档/引导/分岗属 BSM-004/005/007，**未映射本模块** | 页有引导新建与存档删除，作为通用规则的承载面存在。子条细节不升本模块冲突。008 已补存档/删除申请测试与 catch |
| GEN-001 | 三页均有 HTML 上位；KPI 聚焦为交互收窄不是无需求抽象 | 通过（KPI 同构不单独开 finding） |
| GEN-002 | 绑定变更可提交；拓扑已挂接；创建/存档声称与行为 | **通过**。action 必填且下拉三态；已挂接按 projectId；创建/存档路径与测试一致。页头「证据附件」文案与 006 省略附件的差集见观察，不升缺陷（009/B4 口径） |
| GEN-003 | 37 例均执行且有断言 | **通过（本轮）**。夹具改现行闭集；真实键/action/已挂接/中文/空预填/空电话/存档删除/catch 均可失败。绿测本身不替代行为证据；本席以源码+ vitest + 活库字典对照 |
| GEN-004 | 创建/变更输入 | **通过**。名称/业态/电话非空；action 必选；原因必填；存档理由必填。无隐藏字段。11 位正则已删 |
| GEN-005 | 按钮权限与缺 `entity.binding.view` 不请求 | 读路径降级设计成立（拓扑测试覆盖）。写路径：创建不再依赖该权填主体（006）。HighRiskConfirm `entity.binding.manage`；存档 `merchant.profile.manage`；审批 `merchant.profile.approve`。不把该权当创建闸（禁止升格） |
| GEN-006 | 无本模块 SQL；一次 POST 一单 | **无实现面**（库约束在 M010/后台）。观察：merchants 三主体列无 FK（本页已不传） |
| GEN-007 | 幂等键含 `Date.now()` | 观察：单击首次成功不撞；双击/重试换键。铁律 6 收窄不修（009） |
| GEN-008 | 页/后台/活库/夹具枚举与字段 | **通过（本轮修复面）**。绑定字段=记录；空间 projectId；8 码中文=活库；合作方式六类；主体角色三值夹具。表列绑定状态共用 statusLabels 的「有效/未激活/已封禁」与下拉「启用/停用/暂停」并存——002 未要求改表列、禁改 statusLabels，记观察不升 |
| GEN-009 | 创建/变更/存档/删除 catch | **通过**。四处写路径均 catch；008 测试锁失败可见 |
| GEN-010 | 无密钥、无注入点；电话为表单/展示 | 通过（展示层 React 文本节点；PII 掩码在 Controller，本页展示 API 所给） |
| GEN-011 | `AuditTrailHint`；变更带幂等键 | 假附件 ID 已删。留痕事件名在页外。通过 |
| GEN-012 | 列表默认 size=500，页未自扩 | 通过（后台 LIST_MAX；非本页发明） |
| GEN-013 | 6 文件无构建配置 | **无实现面**：原因=本模块无 package/CI/tsconfig |
| GEN-014 | 6 文件无迁移 | **无实现面**：原因=本模块无 SQL/Flyway |
| GEN-015 | 键盘行点击、抽屉 Esc、中文文案 | **通过（修复面）**。业态/空间类型/空间状态/表头「编号」中文。TechDetails API 路径属系统内部标识符（OBS-10）。绑定表状态词见观察 |
| GEN-016 | 页内「接口证据」字符串 | 所列路径与 `api.get/post` 一致。通过（自称可复现，不把 OpenAPI 当需求源） |
| GEN-017 | 分类纪律 | 本席未把 HTML 沉默当冲突；未把禁止升格项升缺陷；添加项 006/007 已答「删除」且已修。本轮无新添加项 |
| GEN-018 | 本报告依据 | 类型 1 HTML 逐字 + 类型 4 命令（vitest/tsc/psql/sha256/unwrap 仿真）。未用历史席位意见当通过依据 |
| PGM-001 | 底座+全中文+可点击 | **通过（修复面）**。英文业态/空间类型/「ID」表头已改。绑定对象可见。可点击：拓扑/项目商户 TraceLink 存在；绑定列展示 ID（系统内部标识符，OBS-01/02）。全中文铁律原文 PGM `:287`。空间 8 码与活库一致 |

---

## 5. 对抗式（grilling 自问自答，不派子代理、不问操作员）

Frontier 已结算：

1. **真实 GET 绑定的行，页面读到的键是什么？** 活库列=`merchant_id/operation_entity_id/contract_entity_id/settlement_entity_id`；AdminApi unwrap→camelCase；页面 BindingRow 同名。测试夹具 snake_case 经真实 AdminApi。读臆造键时整行「-」，本用例必红。
2. **点「提交绑定变更」的 JSON 有没有后台必填 `action`？** 有。下拉三态；未选则拦截不发请求；选 inactive 后 body 含该码。`PlatformService:602-604` 白名单与报错「启用/停用/暂停」一致。AdminApi `normalizeRequestBody`→snake_case，`action` 键名不变。
3. **空间「已挂接」在真实 SpaceRecord 上是否非 0？** 活库 `spaces.project_id` 存在、无 `venue_id`。页面按 `projectId`。测试断言「1 个已挂接」。
4. **项目业态/空间类型用户看见什么？** `formatBusinessStatus("bar")`=`酒吧`；页内表 `shop`=`商铺/铺位`（statusLabels 对 shop 仍 passthrough，故必须走页内表）。活库 8 行 type_name 与页内表逐字相同。测试 `not.toMatch(/\bbar\b|\bshop\b/)`。
5. **测试是否会因真实闭集而红？** 夹具已改 `management_company`/`lease`/`shop`/`active|inactive`。`operation`/`fixed_rent`/`hall`/`terminated` 不再作为合法正例。
6. **无 entity.binding.view 的账号第一次点创建会提交什么主体？** 不传三主体键。缺权只影响主体列表降级（读路径），创建不把该权当闸。
7. **HTML 有没有规定手机号必须 11 位大陆号？** 无。007 已改为非空。空串引导流即时拦。
8. **绑定页没有「创建绑定」是不是冲突？** HTML 未规定必须在本页创建。B2-r2 禁止升格。
9. **存档空理由 / 写失败？** 空理由 `setGlobalError("存档理由必填")` 且 POST=0；404 信封 catch 后 `setGlobalError(any String)`。取消存档不填理由与后台/HTML「点取消存档即可」一致。
10. **pending_admission 商户行会不会英文直出？** `formatBusinessStatus` 无该键，passthrough。夹具未给该状态，测试不红。属 OBS-04/statusLabels 跨模块词表，B2-r2 禁止改 statusLabels，不升本模块缺陷。
11. **双击创建/变更换幂等键？** `Date.now()`。铁律 6：首次正常单击不撞。009。

ponytail-review（只列不改）：

- `SubjectBindingsPage.tsx:L40`: shrink: 页头「证据附件」与 006 省略附件重复声明。删该分句或改成与表单一致。属文案，009 口径。
- `SubjectBindingsPage.tsx:L238` 状态列：shrink: 复用 `BINDING_ACTIONS` label 而非 `formatBusinessStatus`。002 未要求；禁改 statusLabels。不作为本轮缺陷。
- KPI URL 三页复制：yagni 不成立（全站样板，有测试）。不删。
- `net: 文案一句 + 状态列复用已有数组，约十余行可收；不把 KPI 样板算膨胀。`

---

## 6. 定稿 001–008 在当前树上的独立复验（不作 B4 替代，只记录本席现态）

| ID | 当前源码事实 | 测试锁 | 本席 |
|---|---|---|---|
| 001 | 页读 merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason；KPI 唯一按 merchantId、合同主体按 contractEntityId、选中展示 merchantId | 行含 m-1/be_op_1/be_ct_1/be_st_1/initial | 成立 |
| 002 | 三态中文下拉；POST `action`；未选拦截 | 未选不发请求；选 inactive 后一条提交含该码 | 成立 |
| 003 | `attachedSpaces` 用 projectId | 「1 个已挂接」 | 成立 |
| 004 | 业态 formatBusinessStatus；空间 8 码页内表；空间状态可用/不可用；表头「编号」 | 「酒吧」「商铺/铺位」「可用」「合同主体编号」；`not ID` | 成立 |
| 005 | 夹具 management_company/lease/shop/active|inactive | 「业态主体（管理公司）」「租赁」；下拉无 fixed_rent | 成立 |
| 006 | 预填清空；创建不传三主体/区块/面积/备用/租金/status/operatorNote/假证据；合作方式可空；绑定原因空初始不传假证据 | 电话初始空；body 不含 contract-subject-1/att-…/1F-A/二期测试商户/evidence-full-admin | 成立 |
| 007 | 电话非空，无 11 位正则 | 「联系电话不能为空」 | 成立 |
| 008 | 存档空理由拦截与有理由 POST 同条；删除申请一条；写路径 catch | 三条均绿 | 成立 |
| 009 | 登记：下钻无测试、无创建绑定入口、其余观察 | 本席不升格 | 维持否 |

禁止项核对（现态，非 B4 diff）：无三主体选择器；创建不读 `entity.binding.view` 当闸；无创建绑定按钮；测试无 `focusObjectId` 新用例；不拉 `/space-types`；types.ts/statusLabels.ts/adminRoutes.ts/V184/后台源码 blob=HEAD。

---

## 7. 本次 findings / 观察

**未定稿工程缺陷：无。** 添加项：无。HTML 修改申请：无。锁外判据：无。

观察（临时编号；**不**升缺陷；与已定稿 ID 同根因已标明）：

### GR-M012-B5-01（观察）

- **内容**：绑定页表单三态为「启用/停用/暂停」，表格/抽屉 `formatBusinessStatus` 将同一码显示为「有效/未激活/已封禁」。
- **位置**：`SubjectBindingsPage.tsx:29-33` vs `:89,:202,:238`；statusLabels `active=有效/inactive=未激活/suspended=已封禁`。
- **同根**：B1 OBS-04；M012-002 最小处置只要求下拉+POST action；B2-r2 禁止改 statusLabels.ts；B4-r1 观察 3。
- **不升**：禁止项 + 009「其余观察维持否」。

### GR-M012-B5-02（观察）

- **内容**：`TEXT.description` 仍写「所有变更必须带原因、幂等键和证据附件」；006 后请求不传证据、无上传器。
- **位置**：`SubjectBindingsPage.tsx:40`；提交体 `:163-167`。
- **同根**：006「不做附件上传器」；009 登记；B4-r1 观察 4。
- **不升**。

### GR-M012-B5-03（观察）

- **内容**：创建 POST 测试锁假字面量，未写 `not.toContain("businessEntityId")` 等键名。实现已省略三主体键。若将来写回 `businessEntityId: entities[0]?.id`（值 `org-1`），该用例不红。
- **位置**：`OrganizationMerchantPage.test.tsx:221-224` vs 页面 `:175-182`。
- **同根**：006；B4-r1 观察 1。实现满足定稿。测试锁偏松，不是产品回归。

### GR-M012-B5-04（观察）

- **内容**：`pending_admission`/`pending_activation`/`unavailable` 不在 statusLabels；`formatBusinessStatus` passthrough 英文。空间 `unavailable` 已被页内表覆盖（004）。商户 `pending_admission`、项目 `pending_activation` 若出现会英文直出。夹具未给，测试不红。
- **同根**：OBS-04；禁改 statusLabels。不升。

### GR-M012-B5-05（观察）

- **内容**：幂等键 `Date.now()`，双击换键。
- **同根**：OBS-07 / 009；铁律 6。不升。

### GR-M012-B5-06（观察）

- **内容**：拓扑 `focusObjectId` 下钻无测试；本页无「创建绑定」入口。
- **同根**：M012-009；B2-r2 禁止升格下钻用例与创建绑定入口。不升。

### GR-M012-B5-07（观察，气味）

- 测试夹具忽略 `includeArchived`/`merchantType` 查询串，已存档行在未勾选时仍出现（测试需要 m-3 行内删除）。
- 删除申请对 prompt 点取消（`null`）与空串同一句「删除理由必填」。
- 创建成功不重置表单字段。
- 表头「面积(㎡)」含单位符号。
- 抽屉注释仍含「留痕证据」（B4-r1 观察 5）。
- 均不构成未定稿缺陷。

---

## 8. 命令与真实输出 / 退出码

未把未运行测试写通过。失败如实记录。

### CMD-LOCK

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

exit 0。三工具同值（见文首独占行）。889020 字节。sidecar 一行同值。

### CMD-TREE

`git cat-file -t 50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f` → `tree` exit 0。
`git cat-file -t 83d85aa3001e3c4d29475c6c24a6533e0544a5e0` → `tree` exit 0。
六文件 `git ls-tree after` blob = `git hash-object` 工作区，match=YES。
`HEAD=origin/main=bd445728a6ec7bb36a9e7ff19fe56927410ac61e`。`HEAD^{tree}=6302ad2470959383e3f8e209aa53802927ee149a`。

### CMD-VITEST-1（失败，如实）

`/tmp/m012-b5-r1-verify.UtOwvb` 只链 `apps/web/node_modules`（该目录无 `.bin`，vitest/tsc 在仓库根 hoist）。

- `npx vitest run <三文件>` → **exit 127**，`sh: 1: vitest: not found`
- `npx tsc --noEmit` → npx 在 **该副本的 npm_config_cache=/tmp/...** 试图取 `tsc@2.0.4`（弃用包提示「This is not the tsc command you are looking for」）→ **exit 1**。**未**对主仓 `npm install`；cache 在随后 `rm -rf` 中删除。

### CMD-VITEST-2（隔离成功）

`/tmp/m012-b5-r1-verify.rVrWN2`：rsync `apps/web`（排除 node_modules/dist），`ln -s` 仓库根 `node_modules` + `apps/web/node_modules`，`PATH` 含根 `.bin`，`VITEST_CACHE_DIR`/`TMPDIR` 在该目录。

```
cd $WORKDIR/apps/web && vitest run \
  src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

**exit 0**。`Test Files  3 passed (3)`；`Tests  37 passed (37)`；Duration 1.77s。jsdom：`Window.scrollTo` / `getComputedStyle(pseudo)` Not implemented（不影响红绿）。结束后 `rm -rf` 该目录。

```
cd $WORKDIR/apps/web && tsc --noEmit
```

**exit 0**。stdout 空（无 `error TS`）。

### CMD-VITEST-3（用户指定 npm workspace；cache 在 /tmp）

```
VITEST_CACHE_DIR=/tmp/m012-b5-r1-vitest-cache TMPDIR=/tmp/m012-b5-r1-tmp \
npm run --workspace apps/web test:unit -- \
  src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

**exit 0**。3 files / **37 passed**；Duration 1.77s。cache 目录随后删除。`git status` 六文件仍为 B3 修改，无新脏文件。

**不把 37 绿写成行为通过的唯一证据**；与源码/活库对照见 §3–§6。

未跑：后端 IT、全仓 vitest、npm install、浏览器 E2E、会清共享库的脚本。

### CMD-PSQL（只读）

见 §1.2。`PSQL_EXIT=0`。Flyway 410；字典 8 行；`spaces` 无 venue_id 有 project_id；绑定列含四 ID；leftover_probe=0。

### CMD-LABELS（python 解析 statusLabels）

`bar→酒吧`；`shop/building/zone/hall/unavailable/lease/pending_admission→passthrough`；`active→有效`；`inactive→未激活`；`suspended→已封禁`；`management_company→业态主体（管理公司）`；`other→其他`。

---

## 9. 技能实用 / 缺口

本席实际读取并按 V0.17 §6 / B5 收窄使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能；用户指令/controller 优先于技能本体（禁止子代理处收窄） |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md` | 内部 sharpen（目录存在） |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；37 绿 ≠ 行为对 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 测试 2s 内结束，未起 monitor |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、Arc `audit`（会派子代理并另写 `docs/arc/audits/`，与双席/§7 冲突）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：未找到（`domain-modeling` 存在，V0.17 明确不用）

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三席。

---

## 10. 结论

本席 B5-r1：**PASS**。

同时满足：6/6 文件有覆盖记录；21/21 映射判据已核对（GEN-013/014 无实现面已写原因）；三线均执行；声称有四类证据之一；无未答 questions；无未定稿缺陷；哈希行有效。无有条件通过。

模块仍 `IN_PROGRESS`。须席甲自席 raw 后合取；双 PASS 且 B5 后四值全等，才允许 ledger 改 `PASS` 并写闭环文件。本席不行使否决。
