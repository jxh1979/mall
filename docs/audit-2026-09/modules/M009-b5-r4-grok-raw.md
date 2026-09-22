CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B5-r4 修复后整模块独立审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r4
席位：固定 Grok 外审席乙
计划：160-audit-prompt.md V0.17；runbook V0.02；modules.md M009（20 文件，NORMAL）
本文件路径：docs/audit-2026-09/modules/M009-b5-r4-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

整模块结论：**FAIL**。
不等于模块生命周期 PASS，不等于 B5 双 PASS。席甲本次 B5 self 未读。本席 FAIL 具否决权。本提示不预设结论；下为独立实审。

未读取：`docs/audit-2026-09/modules/M009-b5-r4-self-raw.md`（调用时不存在）。未读适配器缓冲。可读本席 B4-r4 原票 `M009-b4-r4-grok-raw.md` V0.01（仅作六文件冻结边界，不代替本轮 20 文件三线）。findings.md 顶部 V3.26、ledger V7.36 作已定稿队列与操作员范围输入，不把台账句子当成证据。

---

## 0. 技能自核（§6，先于审计）

本席在本环境实际核到的技能与处置：

| 技能 | 核到路径 | 本轮 |
|---|---|---|
| graft | `/home/james/.claude/skills/graft/SKILL.md` 存在 | 已读。任务已给出 modules.md 20 文件闭集，直读源码；未跑 graft CLI（定位无增量）。 |
| Arc `audit` | `/home/james/.claude/plugins/marketplaces/howells/plugins/arc/skills/audit/SKILL.md` | 已读。本体派评审子代理 = 第三席，按 V0.17 §6 **不用**。 |
| audit-context-building | trailofbits 插件 SKILL.md | 已读。本体派 function-analyzer 子代理，**不用**。 |
| differential-review | trailofbits 插件 SKILL.md | 已读。本体可派 adversarial-modeler，**不用**；对抗由本席自读自构。 |
| grilling | 派发目录不存在；实际在 `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 已读全文。本体「等用户回答 / 事实题派子代理」双双作废；只借追问结构，事实自读。 |
| verification-before-completion | superpowers 6.3.0 | 已读并执行：未跑 mvn（禁写构建产物）；实读 Surefire；实跑只读 PII/迁移顺序门禁。 |
| receiving-code-review | superpowers 6.3.0 | 已读。B5 禁止修复；只核实、不 IMPLEMENT。 |
| requesting-code-review / using-git-worktrees / dispatching-parallel-agents / subagent-driven-development / code-review / domain-modeling / research | 按 V0.17 §6 | **不用**（第三席、worktree 分叉、第四闸或另写研究文件）。 |

缺口：grilling 注册路径与实际路径不一致，已按实路径读取，不冒充调用 grilling 工具。未用 MCP。未派子代理。未 fetch/commit/push/worktree/部署。未改业务代码、三 HTML、lock、sidecar、findings、ledger。只新建本 raw。

---

## 1. 独立锁实算与输入

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`；交叉 `python3 hashlib` 与 `openssl dgst -sha256`。三工具同值。889020 字节。sidecar 全文一行同值。ledger 顶行同值。criteria-lock 仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批未扩锁。M009 映射行 6388，本席点算 87 ID、unique 87、无重复。映射 **不含** PGM-057/PGM-059；059 不偷偷映射 PGM-059。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B5-r4：B3 累计第 4 次（操作员仅授权一次额外修复，额度已用尽）之后的**整模块**三线重审 |
| 范围 | plan/modules.md M009 全部 20 文件 + 映射 87 判据 + general；不是只看刚改的 6 文件 |
| before / after（B3-r4 冻结树） | `d23497895e200310c4e9692323d5f4e431234981` / `4a8843e8fd431fdd9c9639d2ba96013049986c7a`；`git cat-file -t` 均为 tree |
| 六文件 name-status | 5×M + 1×A；`git diff --stat` 91 插入 / 12 删除；`git diff --check` 两树 exit 0 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；分支 `main` |
| findings | V3.26：058 第2项已修转否；059 新建/保存加密已修转否；058 第1项操作员不动、残留未解决 |
| ledger | V7.36；M009 `IN_PROGRESS`；B3=4 |
| 操作员 | 「2 可以 3.应该加密 1不动」，再对 2/3 答「继续」。无 HTML 修改许可，无新业务添加授权 |
| 三 HTML 两树 blob | 未改（B4 已核；本席抽查 Controller/锁未改） |

---

## 2. 二十文件覆盖表（行数 = 字节中换行数；SHA = SHA-256 全文）

当前合计换行 3375（计划冻结 2476 为历史切分值，本轮按当前全文审，不按旧行数截断）。

| # | 路径 | 行 | SHA-256 | 覆盖 |
|---|---|---|---|---|
| 1 | backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | 全文 |
| 2 | backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | 全文 |
| 3 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | 全文 |
| 4 | backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | 全文 |
| 5 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855 | 全文 |
| 6 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | 296 | 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2 | 全文 |
| 7 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | 全文 |
| 8 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1179 | 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e | 全文 |
| 9 | backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | 全文 |
| 10 | backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java | 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | 全文 |
| 11 | backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | 全文 |
| 12 | backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java | 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | 全文 |
| 13 | backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java | 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | 全文 |
| 14 | backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java | 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | 全文 |
| 15 | backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java | 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | 全文 |
| 16 | backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java | 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | 全文 |
| 17 | backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java | 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | 全文 |
| 18 | backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java | 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | 全文 |
| 19 | backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java | 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | 全文 |
| 20 | backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java | 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | 全文 |

必要只读依赖（不扩审、不扩修）：`AuthorizationService.currentActorHasPermission` / `permissionGrant` / `requirePermissions`；`PiiCipher.encrypt/decrypt`；`PageResponse.of`；三份测试 Surefire 与源；`tools/verify-pii-encryption.mjs`；`V431__merchant_contact_names_ciphertext.sql`；`V427` blob。

### 2.1 六文件工作区仍等于 after 树（git hash-object）

| 路径 | after=工作区 blob |
|---|---|
| JdbcPlatformRepository.java | `f4c086abb98e715325399f4069596809269ad259` |
| PlatformService.java | `63e53b3177610f87a5b49e82173e375dbaa7d951` |
| V431__merchant_contact_names_ciphertext.sql | `5fe129ae2425ede84c80998e29ecd14d27930d51` |
| PlatformJdbcIntegrationTests.java | `40f0068afcc68951ac68e9c234dc9d3239d8e3ff` |
| PlatformServiceTests.java | `c9315fa5f2b6bdb30d3ec2551586b3cb15c5e85f` |
| tools/verify-pii-encryption.mjs | `d5864320e53f2d2ed926635eb4012158a75a685f` |

Controller 工作区 blob `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` = after，**本批未改**（第1项不动的物理证据）。

---

## 3. 第一性原理线

依据类型只承认：① HTML 逐字原文 ② 可执行产物实际行为 ③ 外部规范原文 ④ 可复现命令+输出。不接受「上一轮自己判过」「测试过所以对」。

### 3.1 这段东西为什么必须存在？

删掉本 20 文件，三份 HTML 中至少下列做不到：PGM-006/026 建立商户档案与四类主体绑定；PGM-004/017/020 综合体项目档案；PGM-005 空间/点位；BSM-004 商户/空间类型存档与审批删除；BSM-028/BEQR-143–148/154–155 合作方式、主体角色、状态闭集。答得上 → 有业务上位需求。package-info.java 仅编译单元（已定稿 M009-053 技术性）。VenueBusinessHoursRequest 为营业时间写入载体，服务实现不在本 20 文件（OBS-02）。

### 3.2 声称的行为有没有被实际执行验证过？

本席**未**重跑 `mvn test`（禁写构建产物）。实读已有 Surefire（mtime 2026-09-21 22:45，与 B3-r4 后验证窗口一致）：

```
PlatformControllerTests  tests=14 failures=0 errors=0 skipped=0
PlatformJdbcIntegrationTests tests=24 failures=0 errors=0 skipped=0
PlatformServiceTests tests=70 failures=0 errors=0 skipped=0
TOTAL 108 failures 0 errors 0 skipped 0
```

含本批新断言：`merchantViewAuditRecordsOnlyReturnedPageAndQueryContext`、`merchantContactNamesAreEncryptedAtRestAndRoundTrip`。仍含第1项表征：`listingMerchantsReturnsPlaintextPhoneWithPiiPermission`（有 `pii.contact_phone.view` 时期望明文）。测试通过本身不证明 HTML 被满足——该用例把与 PGM-043 冲突的明文路径锁成绿。

只读门禁实跑：

```
node tools/verify-pii-encryption.mjs
→ PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库
PII_EXIT:0
```

登记含 `merchants.contact_name` / `secondary_contact_name`。

```
node tools/verify-migration-version-order.mjs
→ MIGRATION_VERSION_ORDER_FAIL: 仅 V427__m009_project_profiles_and_associations.sql（V427 不大于 origin/main 最大 V430）
MIG_EXIT:1
```

before/after/工作区 V427 blob 全等 `e485a5d29fea55fa714091485aa26facc8c45160`。V431 排在现有最大 430 之后。V427 不是本次六文件引入；不顺手改号；不把全局迁移门禁写成全绿。

### 3.3 哪个前提未经验证就当真？

- 操作员「1不动」**不是** HTML 改文，也不是判据满足。未验证「持权明文符合 PGM-043」。
- PiiCipher 对无 `enc:v1:` 前缀原样透传（`PiiCipher.java:52-55`）。旧姓名未重存仍是明文；不得声称库内已全部加密。
- V431 只改 TEXT，无 CHECK/回填（授权边界）。
- `currentActorHasPermission` 走布尔、不 `markSingleUsePermissionConsumed`；master 在 `permissionGrant` 直接放行。未验证单次授权会被本列表消耗。
- 本席未对生产库或 `complex_test` 发写语句；加密往返以 Surefire JDBC 用例为证据，不另开库。

---

## 4. 顺从式：逐文件

假定实现意图正确，再核冻结判据与 HTML。

**package-info.java**：仅 `package com.complex.platform.governance;`。技术性，无业务发明。与 M009-053 一致。

**Records（BusinessEntity/Binding/Merchant/DeleteDecision）**：字段与 JDBC 映射一致。MerchantRecord 仍明文承载联系人姓名/手机（业务层明文、仓储加解密）。`withMaskedPhones` 只掩手机、不掩姓名。`withEntities` 只改三主体列。

**DTOs**：SpaceTypeRequest 注释已改为「新建、编码不可重复、新建启用」（M009-057）。MerchantProfileRequest 含可空三主体与 projectId。ProjectRequest 含 ownerPerson/address/businessScope。BindingRequest 无 effectiveTo。Archive/Delete 请求带 idempotencyKey+reason。VenueBusinessHoursRequest 为星期几/时刻载体。

**PlatformRepository**：分页默认、软删、占用统计、项目过滤、listMerchantsByBusinessEntity 隔离通道（M009-048 登记）。`saveSpaceType` 注释「按 type_code upsert」与 JDBC `ON CONFLICT (type_code) DO UPDATE` 一致——该方法同时服务新建（服务层先拒重码）和存档/恢复/删除状态更新，**不是** 057 那种「创建接口存在则更新」的假承诺。

**PlatformController**：治理 GET 均 `requirePermission` + `requirePlatformScope`。商户写带幂等键。`createSpaceType` 控制器仍无 `requireIdempotencyKey`（服务 `executeWrite` 仍把 key 交给幂等器，OBS-01）。`approveDeleteSpaceType` 控制器无 `requirePermission`，服务内 master 检查（历史观察）。**281–294 `canViewPhonePlaintext` = `currentActorHasPermission(pii.contact_phone.view)`，命中则列表/新建/更新/存档/删除申请回写原文手机号。** 注释引用「员工日常界面只显示脱敏号码」（PGM-059 原文，本模块未映射），实现却提供明文开关。

**PlatformService V0.12**：`listMerchants` 150–165 已写 `merchantIds`、`entry=/api/v1/platform/merchants`、`projectId`（null→`""`）、`includeArchived`、`businessFormat`（无筛→`""`）、实际 `page`/`pageSize`、既有 count/total。不写姓名/手机。白名单：业态六值、合作六类、主体状态五值（归档走存档轴）、项目六态、主体三角色、空间二态。绑定：锁商户、存在性、未结束槽、财税三主体不得普通改、未来生效不投影、生效绑定不可普通停用。存档/删除/占用/最高权限直删/禁自审仍在。普通档案拒直接 terminated。createSpace 要求当前项目、同项目父空间、非负面积。createSpaceType 拒重码、固定 active。

**JdbcPlatformRepository V0.09**：`merchantParameters` 625/631 `piiCipher.encrypt` 两姓名；`mapMerchant` 718/724 `decrypt`。手机号仍加密。`owner_person`、`legal_representative`、项目 `owner_person` 仍明文（OBS-03，禁止扩列）。`listMerchantsPaged(projectId,…)` SQL `project_id = :project_id` + 业态/存档谓词 + COUNT/LIMIT；page/size 归一与审计 metadata 同源。`assignMerchantProject` 仅空归属可写。`countMerchantReferences` 排除已结束 inactive 绑定。

**V431**（直接依赖，非 20 文件之一）：两列 `ALTER … TYPE TEXT`，不删数据、无 CHECK、无回填。

顺从结论：授权修的第2/3项与代码、测试、门禁一致。第1项与 PGM-043/041/GEN-010 **仍不一致**，不能顺从为「符合 HTML」。

---

## 5. 对抗式

假定实现错误，构造反例（grilling 结构，事实自答）。

1. **持 `pii.contact_phone.view` 或 master 日常列表是否仍见全文手机号？** 是。Controller 286/290–291 短路掩码；`listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 断言 `18800000011`。`permissionGrant` 对 master 返回 null 即成功，故 USB 身份经布尔查询同样明文。与 PGM-043「页面与导出同属强制脱敏面」「七项含手机号」冲突。操作员 1不动 → 残留，不修。
2. **布尔查询是否消耗单次 grant？** 否。`currentActorHasPermission` 266–277 只 `permissionGrant` 后 return true，不走 `requirePermissions` 56–60 的 `markSingleUsePermissionConsumed`。与 PGM-041「单次消耗、失效全部留痕」冲突。同属第1项。
3. **merchant.viewed 能否关联本页对象？** 能。metadata 含本页 id 列表与入口/筛选/分页。`merchantViewAuditRecordsOnlyReturnedPageAndQueryContext`：page=2 size=1 只记第二户；空页 `merchantIds=[]`；`page=0,size=0` 归一 1/500；metadata 字符串不含 Jane / 18800000011 / 未回页的 id。第2项对抗未破。
4. **姓名是否仍明文落库？** 新建/保存路径否。JDBC encrypt；门禁 18 列绿；JDBC 用例 120 字中文、null、空串：空值原样、非空 `enc:v1:` 且读回一致。旧行无前缀则 decrypt 透传——对抗「全库已加密」成立，故不得声称已全加密。
5. **是否扩了 owner_person / 法人 / 共享 PiiCipher / Controller / CHECK / 回填 / HTML？** 六文件 diff 与 blob 对照：否。
6. **V431 是否插队？** origin/main 最大 430；V431 在其后。乱序的是旧 V427，blob 未变。
7. **跨项目列表？** `listMerchantsPaged` 带 `project_id=`；`currentProject()` 无真实项目则 null，过滤为空。已有 `projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin` / `masterHasNoGlobalListOrOrdinaryProfileBypass`。
8. **普通改结算主体？** `requireUnchangedFinancialSubjects` + 生效绑定不可停用。测试仍在 Surefire。
9. **空间无项目/跨项目父空间？** createSpace `requireText(projectId)` + `requireCurrentProject` 父子。
10. **直接 terminated？** `MERCHANT_EXIT_REQUIRES_BUFFER`。
11. **createSpaceType 重码 upsert？** 服务冲突拒绝；与 057 一致。
12. **敏感页留痕缺对象？** 第2项已补；object_id 仍为 `"all"`，对象在 metadata.merchantIds。授权边界只改 metadata，不另立审计系统。不把 object_id 升格新缺陷。
13. **联系人姓名 API 仍明文返回。** 操作员第3项是落库加密，不是展示脱敏；不偷偷用未映射 PGM-059；不新造展示掩码为添加项。

未发现超出已定稿 058-1 残留、且本模块冻结判据能独立证成的新工程缺陷。

---

## 6. 逐判据核查（87，含 general）

判定：满足 = 本模块实现与该锚点无冲突且相关入口有证据；不满足 = 有本模块代码与原文冲突；无入口 = 本 20 文件不承载该业务，亦未发明相反规则。

| ID | 结果 | 依据 |
|---|---|---|
| GEN-001 | 满足 | 20 文件均有治理主数据承载；package-info 已登记技术性 |
| GEN-002 | 部分不满足 | 第2/3项入口与测试一致；第1项 Controller 明文与 HTML 脱敏声称不一致 |
| GEN-003 | 满足本批断言；第1项测试锁错行为 | 108 例 0 红；明文用例把冲突锁绿，不能当 HTML 证据 |
| GEN-004 | 满足相关入口 | 业态/合作/状态/角色/非负/存在性/项目归属校验仍在 |
| GEN-005 | 部分不满足 | 项目隔离仍在；手机明文旁路削弱最小权限/敏感查看 |
| GEN-006 | 满足本批 | 绑定锁、占用、软删、V431 不删数据 |
| GEN-007 | 无新缺陷 | 写路径仍走幂等执行器；空间类型部分入口历史 OBS |
| GEN-008 | 满足 059 | 两列 TEXT 与密文长度；DTO/JDBC/测试一致 |
| GEN-009 | 满足相关入口 | 中文 400/冲突码；失败不伪成功 |
| GEN-010 | **不满足** | 日常列表/写回可对持权者返回手机原文（GR-B5-01） |
| GEN-011 | 第2项满足；第1项消耗留痕不满足 | merchant.viewed 已有对象/入口/筛选/分页；单次 grant 不消耗 |
| GEN-012 | 满足 | COUNT+LIMIT，size cap 500 |
| GEN-013 | 无本模块构建入口 | 未改依赖 |
| GEN-014 | V431 满足；V427 背景失败 | V431 在 430 后；V427 乱序非本 diff |
| GEN-015 | 无本模块页面 | 前端不在 20 文件 |
| GEN-016 | 满足 057 边界 | SpaceTypeRequest 注释已改；Controller 注释仍引未映射 PGM-059 句，不升格（无新授权改注释） |
| GEN-017 | 满足分类纪律 | 无新添加项；059 仍细化；058-1 仍冲突 |
| GEN-018 | 本报告遵守 | 结论落到 HTML/Surefire/门禁/源码行 |
| BEQR-063 | 无入口 | 沽清/反结不在本 20 文件 |
| BEQR-074 | 无入口 | 隐私桌台不在本模块 |
| BEQR-143 | 满足 | 绑定四主体 + 存在性 |
| BEQR-144 | 满足承载 | 多对多项目关联，无行政 parent 链 |
| BEQR-145 | 满足角色白名单 | `project_company`；职责细节他模块 |
| BEQR-146 | 无经营域入口 | 未发明酒吧/游乐主体角色 |
| BEQR-147 | 无收入计算入口 | 未发明分成 |
| BEQR-148 | 满足 | 合作六类代码闭集 |
| BEQR-149 | 无入口 | 无职级字段 |
| BEQR-150 | 无入口 | 无终端开关 |
| BEQR-151 | 无入口 | 最终权限在 AuthorizationService，只读依赖 |
| BEQR-152 | 满足项目隔离；敏感查看见 GEN-010 | 普通岗位跨项目拒绝仍在 |
| BEQR-153 | 无入口 | 终端开关优先级他模块 |
| BEQR-154 | 满足 | PROJECT_STATUSES 六态 |
| BEQR-155 | 满足可设状态 + 拒直接 terminated | 归档走存档轴；缓冲流程本身不在本批新建 |
| BEQR-156 | 无合同状态机 | 绑定不是合同 |
| BEQR-157 | 无员工状态 | |
| BEQR-158 | 无设备状态 | |
| BEQR-159 | 无权限状态机 | |
| BEQR-166 | 承载登记 | Jdbc 实现审批链表（M009-049）；规则他模块 |
| BEQR-189 | 无入口 | 管理费 |
| BSM-001 | 满足盘子承载 | 项目/商户/空间/主体 |
| BSM-003 | 满足通用能力入口 | 存档删除/权限分岗在本模块相关路径 |
| BSM-004 | 满足已修路径 | 存档、申请、最高权限审批、占用、软删可查 |
| BSM-005 | 无页面 | 分步引导在前端模块 |
| BSM-006 | 无入口 | 动态二维码 |
| BSM-007 | 相关：敏感查看不因平台管理员自动获得 | 明文路径依赖单独权限码；master 布尔旁路见第1项 |
| BSM-009 | 满足入口卡片承载 | 基础管理后台实现第1段 |
| BSM-028 | 满足建立档案/空间/绑定 | 六类合作；项目归属 |
| BSM-029 | 无票务/酒吧连接实现 | 未发明相反规则 |
| BSM-072 | 满足隔离方向 | requirePlatformScope；商户不能看平台全局 |
| BSM-074 | 同 BSM-072 | |
| BSM-114 | 无各端入口表实现 | 后台 API 按权限码 |
| BSM-115 | 无跨系统连接表 | 未切断归属 |
| BSM-120 | 无票务已拍板项实现 | 未发明相反规则 |
| PGM-001 | 满足底座定位 | 归属/资格/责任在档案与绑定 |
| PGM-002 | 满足对象优先 | 先项目/商户/空间再写 |
| PGM-003–016 | 满足为入口卡片/无本段实现 | 本模块只做综合体/空间/商户主体后台；合同/员工/终端/设备/商户侧/开放接口/楼宇等无发明 |
| PGM-016 | 卡片级 | 「个人信息保护」细则主要在未映射 PGM-057/059；本模块用已映射 PGM-043/041/GEN-010 审敏感页与授权 |
| PGM-017 | 满足 | 对象带 projectId；普通无跨项目 |
| PGM-018 | 满足 | 三角色；多对多关联表 |
| PGM-019 | 无收入职责表实现 | 未发明 spv/operation 角色 |
| PGM-020 | 满足 | 项目档案含负责人/地址/经营范围（013 已修） |
| PGM-021 | 无独立「业务意义」段实现 | 无冲突 |
| PGM-026 | 满足档案+绑定；059 姓名落库加密（新写） | 主次联系人新建/保存加密；旧数据不声称全加密 |
| PGM-027 | 满足 | 平台治理读 fail-closed |
| PGM-031 | 无经销商入口 | |
| PGM-035 | 无提成入口 | |
| PGM-040 | 满足权限点颗粒 | merchant.profile.view/manage 等 |
| PGM-041 | **不满足（单次消耗）** | 明文查看走布尔、不消耗单次授权 |
| PGM-042 | 无职级 | |
| PGM-043 | **部分不满足** | 留痕对象/入口/筛选/分页已补；页面强制脱敏手机号仍可被权限豁免关闭 |
| PGM-044 | 无桌台权限入口 | |
| PGM-048 | 无商户侧页面 | 平台后台拒绝 merchant scope |
| PGM-060 | 无会员/人脸入口 | 未发明后台导入会员 |
| PGM-065 | 无跨系统连接表实现 | 档案/绑定仍是下游前置 |
| PGM-066 | 满足已拍板中与本模块相关者 | 项目归属、中文校验文案（047 已修）、过程留痕；界面全中文主要在前端 |

无 LOCK-AMEND 提案。不把 PGM-059 当执行判据。

---

## 7. 本批授权项核对（2/3 已修，1 残留）

### 7.1 M009-058 第2项（队列否）

`PlatformService.java:150-165` 与测试 `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext`：本页 merchantIds、稳定入口、projectId、includeArchived、businessFormat、实际 page/pageSize、count/total；禁止姓名/手机。Controller 未改。**该项对抗未破。**

### 7.2 M009-059（队列否）

JDBC 两列加解密；V431 TEXT；PII 登记升列；JDBC 用例 120 字/null/空串。无回填、无 CHECK、未改共享 PiiCipher。**仅新建及后续保存加密；旧明文未重存不声称已全加密。**

### 7.3 M009-058 第1项（操作员不动，必须如实记录）

事实仍在，禁止本批修改，**不得伪称已符合 HTML**。

---

## 8. Findings（临时编号；全部列完）

### GR-M009-B5-01（沿用正式 ID M009-058 第1项残留）

- 路径/行：`PlatformController.java:281-294`（`canViewPhonePlaintext` / `maskPhonesForUnauthorized`）；调用点 68、75、85、94、114、122、131；`MerchantRecord.java:86-96`；测试 `PlatformControllerTests.java:164-179`；只读依赖 `AuthorizationService.java:266-277` 与 `80-83`
- 事实：持 `pii.contact_phone.view`（及经 permissionGrant 的真实 master）时，日常商户列表与写回返回两联系人手机号原文。布尔 `currentActorHasPermission` 不调用 `markSingleUsePermissionConsumed`。无权限路径仍掩码（`listingMerchantsReturnsApiResponsePageShape` 期望 `188****0011`）。
- 四分类：**冲突**（判定顺序：与 HTML 明文矛盾，先于细化）
- 依据类型：① HTML 原文 + ④ Surefire 用例名与期望值
- 冻结判据：PGM-043（页面/导出强制脱敏闭集含手机号；敏感页不提供关闭开关——权限豁免即关闭开关）；PGM-041（临时授权单次消耗须留痕）；GEN-010；BSM-007（敏感数据查看须单独授予——授予后日常界面仍应脱敏，与「不见明文」同向）。对照 PGM-059「员工日常界面只显示脱敏号码」仅作背景，**不映射、不 LOCK-AMEND**。
- 工程缺陷：**是**
- 操作员处置：第1项不动。本席不修、不把队列改回「是」、不申请改 HTML。缺陷未消除。
- 最小处置（仅登记，B5 不执行）：删除日常响应明文豁免、改走 `withMaskedPhones`，明文查看改经消耗单次授权的既有通道。超出本批授权。

无其它须给临时 ID 的新问题。无添加项。无 HTML 自身缺陷需铁律 4 暂停。

---

## 9. 原 OBS 完整保留（不冒称已处置）

| ID | 内容 | 本轮 |
|---|---|---|
| OBS-01 | `createSpaceType` 控制器无 `requireIdempotencyKey`；`IdempotencyService.execute` 仍要 key | Controller 176–180 仍无；不修 |
| OBS-02 | 营业时间 list/save 走外部 VenueBusinessHoursService，平台列表形态差异 | DTO 在模块内，服务不在 20 文件 |
| OBS-03 | `owner_person` / `legal_representative` 明文 | JDBC 627/610 仍直绑；禁止扩列 |
| OBS-04 | V339/V394 `pii.contact_phone.view` 种子为 058 背景 | 未改迁移种子 |

映射建议（历史，不执行）：PGM-057/059 若要成为本模块执行判据须走 LOCK-AMEND 五步；最早受影响模块按实际引用重开，不能预先声称仅 M009。

---

## 10. 可复现命令与输出（本席实跑 / 实读）

锁：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

PII：`PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库` exit 0

迁移顺序：exit 1，仅旧 V427；V427 blob before=after=工作区 `e485a5d29fea55fa714091485aa26facc8c45160`

两树 `git diff --check` exit 0

Surefire 实读：108 tests，0 failure，0 error，0 skipped（未本席重跑 mvn）

---

## 11. 结论

B5-r4 席乙：**FAIL**。

原因：整模块 20 文件与 87 判据审完后，已冻结冲突 **M009-058 第1项**（手机号日常明文豁免 + 布尔单次授权旁路）仍在现码与现测试中；与 PGM-043 / PGM-041 / GEN-010 矛盾。操作员「1不动」保留该实现，**不是**符合 HTML，也不是无条件通过。第2项留痕与第3项两姓名新写加密已按授权落地，不能把这两项绿写成整模块 PASS。

不修复。不改队列。不 LOCK-AMEND。不进入下一模块。本票不是模块收口文件。
