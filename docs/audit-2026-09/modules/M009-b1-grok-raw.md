CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 20 文件，冻结有效内容行 2476
- 冻结判据：87 条（提示词所列闭集，与锁映射行逐字相同）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M009-001 … GR-M009-007
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw、未问操作员

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先于技能本体 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 接口/深度/删除测试；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- `git-guardrails-claude-code`：存在于 mattpocock `misc/`，本席未改 git、未直推

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；映射行 `M009 → [BEQR-063 … PGM-066]` 与开工口径 87 项逐字相同（本席自锁文件抽出，COUNT=87 unique=87 MISSING=[]）
- `plan/modules.md` M009 FILES 20 条与用户清单逐字相同
- 20 文件 `\n` 计数之和 = **2476**，与冻结有效内容行一致
- 20 文件字节/SHA256 与用户固定清单逐文件 `match=True`

HEAD：`main` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`（`git fetch origin` 后 `rev-parse` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

ledger 只读核验（不改）：V2.00，388068 bytes，SHA256 `7dcd740e3fdc6fa21b7ffd7a58d9974e55dd1c93fa6ffa6d1f5171087810ba0a`；顶部锁行与本席独算相同。findings V1.16 仅作已登记边界背景，未把 M001–M008 票当本 20 文件已审。

三份 HTML 行号核验：锁内 69 处来源行（BEQR 21 + BSM 14 段标题 + PGM 34 段标题）对本席读取的现行 HTML **MISS_COUNT=0**。

## 2. 20 文件覆盖表（全文已读；行=splitlines=\n 计数）

| # | 路径 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| 1 | backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 19 | 664 | 595d53d5c4c54fe840e318817d07e4e3985218e636e1e6c07cfba0cebb96eef7 |
| 2 | backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 |
| 3 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 79 | 4611 | e881a9899b23ca328a372d032bd6aa992377d56049e64569bbb595b32b0e24ff |
| 4 | backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 8 | 307 | 07da344cf81a7417e5a67a7cb8c5c8a465e8476717b16342d24e95ea3728db33 |
| 5 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1100 | 61439 | a9dce38d0268cdc3a46debbea349881d955b8d6db9617b6a484a5b7e9f8539e3 |
| 6 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | 227 | 13040 | 49fc52cbbefb9c99df047af03c275304b087ca31671c0e17b971bc1b520c5ed2 |
| 7 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 122 | 5769 | a77bff24123e3a6bedff7504279e09f064cdc6bcfc5900a07902360c9c109ced |
| 8 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | 739 | 34404 | 85e0d87f125593c1dfe329c258cee8320352ae2675c88ffd34164eb34062472c |
| 9 | backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | 41 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 |
| 10 | .../dto/BusinessEntityBindingRequest.java | 17 | 442 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da |
| 11 | .../dto/BusinessEntityRequest.java | 17 | 431 | 85e3e5d2c1487b382deb214a546179b8fb8a85af204ae9d25a83ef539c0b867f |
| 12 | .../dto/MerchantArchiveRequest.java | 5 | 218 | 210a1af4d678da01b25cb419d08d47f8d1684c3f7a2f599bdbb6a24903ff0b6d |
| 13 | .../dto/MerchantDeleteApprovalRequest.java | 5 | 225 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 |
| 14 | .../dto/MerchantDeleteRequest.java | 5 | 214 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 |
| 15 | .../dto/MerchantProfileRequest.java | 27 | 767 | cbcf242470823799c3d2fcbe996de4665e4d440614a4263a2635ccb4439bea99 |
| 16 | .../dto/ProjectRequest.java | 17 | 375 | 8316db69c16d3013dbfc07ead14673321a49d05c3131163ae58597543004c76e |
| 17 | .../dto/SpaceRequest.java | 21 | 525 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa |
| 18 | .../dto/SpaceTypeRequest.java | 15 | 457 | d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2 |
| 19 | .../dto/StatusChangeRequest.java | 11 | 231 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f |
| 20 | .../dto/VenueBusinessHoursRequest.java | 22 | 700 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 |

Jdbc 1100 / Service 739 / Controller 227 均整文件读完，未按标题抽样。Jdbc 自 L1 包声明读至 L1100 `setMerchantArchiveReason`；其中 L683 起实现 `SupervisorReviewRepository`（审批链/假期/组织上级），按「文件已有职责不因模块名裁掉」纳入本模块三线，但不把 M015/M017 的营业时间服务或主管复核 Service 扩成全量审计。

相关只读依赖（验证本模块调用/字段，不扩审）：`ApiRequests.requireIdempotencyKey`、`FieldLabels`、`AuditedOperationExecutor`/`IdempotencyService`、`TicketingService.expireBarSingleStoreTickets`、`VenueBusinessHoursService.save`（Controller 转发）、`V005/V010/V259/V318/V348/V384` 迁移注释与约束、M011 测试三类。`VenueBusinessHoursService` 本体属后续模块 FILES，本席只核本 Controller/DTO 入口契约。

## 3. 用户 B/C 原意（不误报、不延伸）

操作员已确认 B/C 是业务配置原意，先前 A/D 额外授权已消耗、不延用。本模块 20 文件无「业务参数，按项目可配」的 B/C 句，也无 CLI PASS/FAIL 标识。BEQR-155 缓冲期「业务参数，按项目可配，上下限出厂为三十至六十天」是冻结原文本身，不是 B/C 那两处酒吧打折句，不拿 B/C 授权去改缓冲期口径。

## 4. 第一性原理（逐文件）

删除测试：删掉这 20 文件，BSM-009/PGM-006/PGM-026「建立商户档案、四类主体绑定、综合体/空间」在后台将无写入面。答得上上位。但不能因此把错误枚举、错误删除语义、错误层级链留下。

| 文件 | 为何存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| BusinessEntityRecord | 法律/经营主体主数据 | 创建/列表经 Service→Repo；探针写入 `entityRole=management_company`、`parentEntityId=be_l2` | parent 链被注释写成 L1→L2→L3，与 HTML 多对多冲突（GR-M009-004） |
| BusinessEntityBindingRecord | 四类主体一组关系 | 创建时拦第二份 active；改 inactive 后可再绑 | 与商户档案三字段不是同一组（GR-M009-005） |
| MerchantRecord | 商户档案+存档删除轴+电话掩码辅助 | 19 参构造把存档/删除默认成 none；`withMaskedPhones` 存在 | 19 参被 update 使用会抹存档（GR-M009-001） |
| MerchantDeleteDecision | 审批结果 | `deleted=true` 在内存仓储=行消失 | 注释写「物理删除」；Jdbc 实为 UPDATE |
| JdbcPlatformRepository | Postgres 适配+审批链仓储 | 本席 JDBC 集成测试因库口令失败未跑（见 §10）；SQL 文本已整读：`deleteMerchant`=`UPDATE delete_status='approved'`；绑定 unique 部分索引在 V010 | 活库约束/加密/分页未在本席命令下执行 |
| PlatformController | HTTP 面 | 单元 9 PASS：无权限 403、缺幂等 400、列表形态 | `createSpaceType` 未调 `requireIdempotencyKey`（服务层仍拒空键）；营业时间转发其它类 |
| PlatformRepository | 仓储接口/默认内存切片 | 默认 `deleteMerchant` 抛 Unsupported；默认 `findMerchantById` 扫全表 | 测试内存实现覆盖 delete=remove |
| PlatformService | 写路径/校验/审计 | 单元 17 PASS；隔离探针复现存档被更新抹掉、合作模式任意串、绑定与档案分叉 | 5 参构造才注入 TicketingService，单测/探针走 4 参，撤场到期路径未实跑 |
| package-info.java | 包声明 | `package com.complex.platform.governance;` | 无行为 |
| BusinessEntityBindingRequest | 绑写入参 | 四 ID + 生效日 + 理由 | 不校验商户/主体存在 |
| BusinessEntityRequest | 主体入参 | 名称/角色/parent 必填名 | 角色无闭集 |
| MerchantArchiveRequest | 存档理由 | Service `requireText(reason)` | 与 HTML「存档填理由」一致 |
| MerchantDeleteApprovalRequest | 审批 | approved/rejected | 无最高权限身份校验，只靠权限码 |
| MerchantDeleteRequest | 删除申请理由 | 须先存档 | 与 HTML「先存档再申请」一致 |
| MerchantProfileRequest | 商户档案 | 业态六值有校验；合作模式/状态无闭集 | GR-M009-002/003 |
| ProjectRequest | 项目/场地 | 默认 status=active | 非综合体六态（观察/GR-M009-003 同类） |
| SpaceRequest | 空间 | space_type 须字典 active | 状态仍 active 默认 |
| SpaceTypeRequest | 空间类型字典 | upsert by type_code | Controller 不预检幂等键 |
| StatusChangeRequest | 绑定状态变更 | action∈active/inactive/suspended | 英文内部键进中文报错 |
| VenueBusinessHoursRequest | 场地营业时间 DTO | Controller GET/POST 存在 | 保存语义在其它模块 Service；本 DTO 无草案/审批字段 |

## 5. 顺从式（87 条逐 ID）

分类：冲突优先。无实现面写原因，不把 HTML 沉默当冲突。

| ID | 覆盖结论 | 2.1 | 工程缺陷 |
|---|---|---|---|
| BEQR-063 | 沽清/反结/免单等酒吧高危动作。20 文件无账单/沽清实现 | 不适用（无实现面） | 否 |
| BEQR-074 | 隐私桌台。无桌台权限实现 | 不适用 | 否 |
| BEQR-143 | 绑定表有经营对象(merchant)+运营/合同/结算四字段，active 唯一。档案另存三主体且无运营主体，可与绑定分叉；创建商户不写绑定 | 冲突 | **是 → GR-M009-005** |
| BEQR-144 | HTML：物理项目与运营公司多对多，不是「平台→项目公司→域公司→商户」隶属链。Record/V318 口径把 L3.parent=L2、L2.parent=L1 经 API 落库 | 冲突 | **是 → GR-M009-004** |
| BEQR-145 | 城市项目公司职责。本模块无强制「项目公司不管理酒吧人员设备定价」的校验，只存主体行 | 细化（字段可挂角色）；无本文件发明新主体名 | 否（职责闸在后续域） |
| BEQR-146 | 业态公司职责。`merchant_type` 六值与 BSM-072 酒吧/餐饮/零售/游乐/传媒/其他对齐（内部码） | 细化 | 否 |
| BEQR-147 | 收入归属。本模块不记账 | 不适用 | 否 |
| BEQR-148 | 合作模式六类。`cooperationForm` 自由字符串，测试与探针写入 `fixed_rent_plus_commission` / `not-a-mode` | 冲突 | **是 → GR-M009-002** |
| BEQR-149 | 职级 M1–M10。无职级字段 | 不适用 | 否 |
| BEQR-150 | 十一端。无终端开关实现 | 不适用 | 否 |
| BEQR-151 | 最终权限公式。本模块只 `requirePermission`+`requirePlatformScope` | 细化（调用既有授权） | 否 |
| BEQR-152 | 项目隔离/敏感价。电话脱敏在 Controller；内部价不在本文件 | 细化（PII 展示） | 否 |
| BEQR-153 | 终端开关优先级。无开关实现 | 不适用 | 否 |
| BEQR-154 | 综合体六态。`createProject` 默认 `active`，无闭集 | 冲突（与 003 同根：状态词表） | **并入 GR-M009-003** |
| BEQR-155 | 商户六态+退出缓冲期。状态默认 `active`、接受任意串；存档即撤场到期（源码） | 冲突 | **是 → GR-M009-003、GR-M009-007** |
| BEQR-156 | 合同状态/当地时。无合同表写入 | 不适用 | 否 |
| BEQR-157 | 员工状态。无员工实现 | 不适用 | 否 |
| BEQR-158 | 设备状态。无设备实现 | 不适用 | 否 |
| BEQR-159 | 权限状态五值。权限不在本文件维护 | 不适用 | 否 |
| BEQR-166 | 合同审批链。Jdbc 实现 supervisor 链/FOR UPDATE/平行线节点表；商户删除只用 `merchant.profile.approve` 单人审批，不是该条「主线+部门平行线」 | 商户删除路径：冲突于「具备最高权限的人」的链模型见 BSM-004；链 SQL 本身细化后续模块 | 商户删除审批模型并入 GR-M009-006 观察；不把未审的复核 Service 当已完成 |
| BEQR-189 | 服务点单管理费。无服务目录 | 不适用 | 否 |
| BSM-001 | 先管盘子。本模块是盘子写入面 | 细化 | 否（缺陷在具体枚举/删除） |
| BSM-003 | 通用可改可删。由 004 具体化 | — | 见 004 |
| BSM-004 | 配置主数据存档/删除=软删留库可查可恢复、先存档再审批、禁自审、占用检查、填理由 | 冲突（更新抹存档；删除语义物理/墓碑分裂；已批准删除不进 includeArchived） | **是 → GR-M009-001、GR-M009-006** |
| BSM-005 | 分步引导弹窗。后台 UI 不在本 20 文件 | 不适用（无前端实现面） | 否 |
| BSM-006 | 动态码。无二维码实现 | 不适用 | 否 |
| BSM-007 | 平台管理员≠最高权限；删除不因管理员身份自动得。权限码分离 manage/approve 是细化；种子授给 `role_platform_admin` 在迁移 M010，本文件只消费权限码 | 本文件无种子 | 否（不扩 M010） |
| BSM-009 | 基础管理入口。本模块承载商户/主体/空间/项目写入 | 细化 | 否 |
| BSM-028 | 建综合体/商户/六类合作/多对多。合作模式与层级见 002/004 | 冲突 | 并入 002/004 |
| BSM-029 | 下游要空间商户归属。本模块提供 ID | 细化 | 否 |
| BSM-072 | 业态六值。应用层 `MERCHANT_BUSINESS_FORMATS` 六值，非法业态拒 | 细化 | 否 |
| BSM-074 | 商户不能看平台全局。写路径 `requirePlatformScope` | 细化 | 否 |
| BSM-114 | 入口表。本模块是管理后台 API | 细化 | 否 |
| BSM-115 | 基础管理连所有业务。提供归属 ID | 细化 | 否 |
| BSM-120 | 已拍板清单（购票/积分/职级等）。与本 20 文件无直接实现面的条目不发明；「集团视图预留项目归属」仅有 merchantId/venue 型 projects 行 | 无实现面为主；项目形态观察 | 否 |
| GEN-001 | 20 文件均有用途；`requireDate` 死代码；`readObjectMap` 未用 | 技术性观察 | 否 |
| GEN-002 | 单元 26 PASS；Jdbc 11 ERROR 因库认证失败，不得报 JDBC PASS | 技术性 | JDBC 未闭合，见 §11；不单独成业务票 |
| GEN-003 | 单测可失败（非法业态/非法 action/占用拒删）。内存 `deleteMerchant=remove` 不能代表 Jdbc UPDATE。存档+更新无测试 | 技术性 | **是 → 并入 001/006**（测试未锁缺陷） |
| GEN-004 | 业态有校验；合作模式/状态/parent/绑定对象存在性无校验 | 技术性+冲突已在业务 ID | 并入 002/003/005 |
| GEN-005 | 各写入口 `requirePermission`+平台范围；读入口 view 权限 | 细化 | 否 |
| GEN-006 | 存档字段被 update 覆盖；绑定无对象存在性 | 冲突/细化 | **001、005** |
| GEN-007 | 写路径走幂等执行器；`createSpaceType` 空键在服务层仍 `VALIDATION_FAILED` | 技术性观察 | 否 |
| GEN-008 | 内存删除 vs Jdbc UPDATE；档案三字段 vs 绑定四字段；OpenAPI/前端不在本 FILES | 技术性 | **并入 005/006** |
| GEN-009 | 错误码中文为主；服务异常原文含内部码 | 观察 | 否 |
| GEN-010 | 电话 PII 仓库加密（Jdbc，集成测试未跑）；动态 COUNT 表名来自 information_schema | 技术性观察 | 否（未证实注入） |
| GEN-011 | executeWrite 记成功/失败事件；存档/删除有事件名 | 细化 | 否 |
| GEN-012 | 列表 size cap 500 | 技术性观察 | 否 |
| GEN-013 | 无本模块构建脚本 | 不适用 | 否 |
| GEN-014 | 无迁移文件（M010） | 不适用 | 否 |
| GEN-015 | 无本模块 UI；API JSON 用内部键 | 观察（内部标识符） | 否 |
| GEN-016 | 注释「物理删除」与 Jdbc UPDATE、HTML 软删不一致 | 技术性 | **并入 006** |
| GEN-017 | 本批冲突项见 findings；无把发明标成技术性 | — | — |
| GEN-018 | 结论落到 HTML 原文、单元输出、隔离探针；Jdbc 活库失败如实 | — | — |
| PGM-001 | 治理底座/全中文/金额/先成功。本模块是底座写入；金额精度无本文件计算 | 细化；报错观察 | 否 |
| PGM-002 | 先看对象再合同。本模块只建对象，不卡合同 | 无合同闸实现面 | 否 |
| PGM-003 | 模块入口卡片。无 HTML/UI | 不适用 | 否 |
| PGM-004 | 综合体和集团卡片 | 不适用（文案） | 否 |
| PGM-005 | 空间点位卡片。空间 API 存在 | 细化 | 否 |
| PGM-006 | 商户和主体。见 002/005 | 冲突 | 并入 002/005 |
| PGM-007 | 合同和准入。无合同写入 | 不适用 | 否 |
| PGM-008 | 员工和手机。无 | 不适用 | 否 |
| PGM-009 | 权限和职级。只消费权限码 | 不适用（权限配置面） | 否 |
| PGM-010 | 终端开关。无 | 不适用 | 否 |
| PGM-011 | 设备。无 | 不适用 | 否 |
| PGM-012 | 商户侧全开放。本模块是平台后台 API + `requirePlatformScope` | 细化 | 否 |
| PGM-013 | 开放接口。无 | 不适用 | 否 |
| PGM-014 | 留痕复核。写路径走审计执行器；审批链 SQL 在 Jdbc | 细化 | 否 |
| PGM-015 | 楼宇运营。无机电 | 不适用 | 否 |
| PGM-016 | 应急与隐私。电话脱敏+加密；U 盘破窗无本文件入口 | 细化（PII）；USB 无实现面 | 否 |
| PGM-017 | 当前一项目未来集团。projects 行带 merchant/venue，无集团视图 | 细化不足，观察 | 否 |
| PGM-018 | 多对多非隶属。与 004 同 | 冲突 | **GR-M009-004** |
| PGM-019 | 各主体职责表。无收入入账 | 不适用 | 否 |
| PGM-020 | 建立综合体项目档案。`createProject` 存在但形态像场地 | 观察 | 否（不另发明集团 KPI） |
| PGM-021 | 避免数据混。无跨项目隔离字段在本 DTO | 观察 | 否 |
| PGM-026 | 商户档案+四类绑定+六类合作。见 002/005 | 冲突 | **002/005** |
| PGM-027 | 商户侧边界。平台写口 | 细化 | 否 |
| PGM-031 | 经销商门户。无 | 不适用 | 否 |
| PGM-035 | 提成发放。无 | 不适用 | 否 |
| PGM-040 | 单一功能权限。权限点拆分存在 | 细化 | 否 |
| PGM-041 | 权限规则。无模板装配 | 不适用 | 否 |
| PGM-042 | 职级规则。无 | 不适用 | 否 |
| PGM-043 | 外包/审计/导出脱敏。电话掩码符合「展示脱敏」；导出密码暗纹无本文件 | 细化（展示）；其余无实现面 | 否 |
| PGM-044 | 隐私桌台/挂单/经销权限。无 | 不适用 | 否 |
| PGM-048 | 商户侧能力表。无商户端实现 | 不适用 | 否 |
| PGM-060 | 会员/生物信息。无 | 不适用 | 否 |
| PGM-065 | 治理连接各域。提供 ID | 细化 | 否 |
| PGM-066 | 已拍板。与本文件有关的「对象带综合体项目归属」仅部分字段；其余无实现面 | 观察 | 否 |

## 6. 对抗式最小反例（自构，未问操作员，未打真实业务库）

1. 非法业态 `tenant` → `MERCHANT_BUSINESS_FORMAT_INVALID`（单元）。
2. 合法六值 `bar` 创建后按业态过滤只见酒吧（单元）。
3. 缺 `idempotency_key` POST `/merchants` → 400 `VALIDATION_FAILED`（Controller 单测；handler 日志 raw=`idempotency_key is required`）。
4. 无 `merchant.profile.manage` → 403（Controller 单测）。
5. 未登录列商户 → 401 或 403（Controller 单测）。
6. 同一商户第二份 active 绑定 → `ACTIVE_BINDING_EXISTS`（单元）。
7. 先 inactive 再绑新 active → 成功（单元）。
8. `action=active_bypass` → `VALIDATION_FAILED`（单元）。
9. 未存档申请删除 → `MERCHANT_NOT_ARCHIVED`（源码；单元覆盖存档后删除）。
10. 占用 refs>0 审批 → `MERCHANT_IN_USE`，行仍在（单元）。
11. 申请人=审批人 → `MERCHANT_SELF_APPROVE_FORBIDDEN`（源码；单元换 `usr_admin2`）。
12. **存档后 updateMerchant**：存档标记被 19 参构造抹成 null，默认列表重新可见（隔离探针 GR-M009-001）。
13. **合作模式** `fixed_rent_plus_commission` 与 `not-a-mode` 均写入成功（探针+既有单测）。
14. **状态** `待准入` 与 `active` 均接受，无缓冲期字段（探针）。
15. **创建商户不写绑定**；随后绑定运营/合同/结算与档案三字段不同 → `PROFILE_VS_BIND_DIVERGE=true`（探针）。
16. 内存审批通过 → `merchants.remove`，行不在；Jdbc 源码为 UPDATE 墓碑（GR-M009-006）。
17. `createSpaceType` 空幂等键 → 服务层 `VALIDATION_FAILED` / `idempotency_key is required`（探针）。
18. 未知 `space_type` → `VALIDATION_FAILED`（单元）。
19. parent=`be_l2` + role=`management_company` 原样落库，无多对多校验（探针）。
20. 理论：无鉴权时 `actorId()` 捕获异常返回 null，自审闸跳过——Controller 先鉴权，首轮正常路径不发生，不升缺陷。

B/C 两句未进入本模块反例。

## 7. Findings（整批收齐；不修）

### GR-M009-001

- 文件:行号：`PlatformService.java:599-637`（`saveMerchant` 走 19 参构造）；`MerchantRecord.java:37-47`（19 参默认 `archivedAt=null`、`deleteStatus=none`）；调用点 `updateMerchant:166-172`
- 2.1：**冲突**（BSM-004 存档后从选择列表隐藏，取消存档才恢复；更新档案不是取消存档）
- 判据：BSM-004、GEN-006、GEN-002、GEN-003
- 2.3 类型：4（隔离探针）+ 1（HTML）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：`docs/business-system-map-for-operations-v0.1.html:566`「存档（先下线）：…从各处的选择列表里隐藏…想恢复点『取消存档』即可。」
- 证据：CMD-PROBE1 输出 `AFTER_ARCHIVE_AT=true` → `AFTER_UPDATE_ARCHIVED=null` → `LIST_DEFAULT_HAS=true`。既有单测无「存档后更新」断言。
- 业务影响：已存档商户被一次档案 PATCH/PUT 静默复活，可再被新业务选用；若当时 `deleteStatus=pending` 也会被抹成 none。
- 最小处置：update 必须读出原行再改档案字段，保留 archived/delete 轴；或拒绝已存档未恢复的更新。不要改 HTML。
- 无业务发明理由：不适用（本条是冲突）。

### GR-M009-002

- 文件:行号：`PlatformService.java:633` `trimToNull(request.cooperationForm())` 无闭集；`MerchantProfileRequest.java:24`；测试 `PlatformServiceTests.java:252,265` 断言 `fixed_rent_plus_commission`
- 2.1：**冲突**
- 判据：BEQR-148、BSM-028、PGM-026
- 2.3 类型：1 + 2（单测写入该值且 17 PASS）+ 4（探针 `COOP_ARBITRARY_ACCEPTED=true`）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BEQR-148「自营、租赁、联营、代运营、服务商接入、其他。选『其他』的合同必须在生效前指定套用哪几项收费模板…」
- 证据：收费模板码被存进合作模式字段；任意 `not-a-mode` 同样成功。
- 业务影响：合作模式词表被发明/混用，后续结算/合同模板无法按六类判断；「其他」须指定收费模板的闸无法落在该字段上。
- 最小处置：写入闭集校验六类（内部码若需要则一一对应中文六类）；收费模板不进 `cooperation_form`。不改 HTML 六类清单。

### GR-M009-003

- 文件:行号：`PlatformService.java:634` `trimToDefault(..., "active")`；`createProject:368` 同样默认 `active`；无商户/项目状态闭集
- 2.1：**冲突**
- 判据：BEQR-155、BEQR-154
- 2.3 类型：1 + 4（`CREATE_STATUS=active`、`STATUS_CN_ACCEPTED=true`）+ 2（夹具 `draft`/`active`）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BEQR-155「待准入、正常、暂停、终止、归档、黑名单」+「商户退出不是立即生效，一律设缓冲期…」；BEQR-154「草稿、待启用、已启用、暂停、终止、归档」
- 证据：默认 `active`（FieldLabels 译「启用状态」，不是「正常」）；`draft` 进入商户夹具；中文「待准入」也被当作自由串接受；无缓冲期字段/状态机。
- 业务影响：暂停/黑名单/退出缓冲无法在本写入面成立；综合体六态未约束。
- 最小处置：商户/综合体状态分别按两条闭集校验；退出（终止）走缓冲期参数（参数本身按项目可配，属冻结原文，不是 B/C 授权）。不发明第七类商户状态。

### GR-M009-004

- 文件:行号：`BusinessEntityRecord.java:9-11` 注释「L3 的 parent = L2 项目公司；L2 的 parent = L1 平台」；`PlatformService.java:120` `trimToNull(request.parentEntityId())` 无角色/多对多校验
- 2.1：**冲突**
- 判据：BEQR-144、PGM-018、BSM-028
- 2.3 类型：1 + 4（`ENTITY_ROLE=management_company ENTITY_PARENT=be_l2`）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BEQR-144「物理项目与运营公司是多对多关系，不是固定的『平台→项目公司→域公司→商户』行政隶属链。」PGM-018 同旨。
- 证据：API 把单亲 parent 链当组织落库；注释把禁止的行政链写成口径。V318 在 M010，本模块实现面已经消费该列。
- 业务影响：一家运营公司服务多项目、一项目多家运营公司分域，无法用单 parent 表达；系统会按禁止的隶属链理解主体。
- 最小处置：停止把 parent 当作行政隶属；多对多协作走绑定/协作关系，而不是 L1←L2←L3。不改 HTML。

### GR-M009-005

- 文件:行号：`MerchantRecord.java:10-12` 仅 business/contract/settlement 三主体、无运营主体；`saveMerchant:621-623`；`createBusinessEntityBinding:511-533` 另写四字段；创建商户不调用绑定
- 2.1：**冲突**
- 判据：BEQR-143、PGM-026
- 2.3 类型：1 + 4（`BINDINGS_AFTER_CREATE=0`、`PROFILE_VS_BIND_DIVERGE=true`）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BEQR-143「经营对象、运营主体、合同主体、结算主体（可分属不同公司，同一经营对象下唯一绑定）」；PGM-026「把一个商户的『经营对象、运营主体、合同主体、结算主体』绑成一组唯一关系…合同、结算、权限、看板、第三方接入都引用同一组绑定。」
- 证据：档案三字段与绑定四字段可同时存在且不一致；合同/结算若有的读档案、有的读绑定，则「唯一一组」不成立。绑定还不校验商户行存在。
- 最小处置：以绑定表为唯一真源，创建/更新商户同步或禁止档案旁路字段；禁止无经营对象的绑定。不新造第五类主体。

### GR-M009-006

- 文件:行号：`MerchantDeleteDecision.java:4-7`「approved 时商户已物理删除」；`PlatformService.java:272-317` 注释「物理删除」并 `repository.deleteMerchant`；`PlatformRepository.java:32-34`「物理删除」；测试内存实现 `PlatformServiceTests.java:491-493` `merchants.remove`；**Jdbc** `JdbcPlatformRepository.java:169-172` 实为 `UPDATE delete_status='approved'`；列表 `listMerchantsPaged:385` `delete_status <> 'approved'` 在 `includeArchived=true` 时仍排除
- 2.1：**冲突**（BSM-004「删除不是真从数据库里抹掉…可查、可审计、可恢复」）+ GEN-008 适配器分裂
- 判据：BSM-004、GEN-008、GEN-003、GEN-016
- 2.3 类型：1 + 2（单元 `approveDeleteRemovesArchivedMerchantWhenNoReferences` 断言 includeArchived 列表也不见）+ 4（探针 `INMEMORY_STILL_PRESENT=false`）+ Jdbc 源码（活库测试未跑，见 §11）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BSM-004「删除只隐藏不抹除…数据始终留在库里可查、可审计、可恢复」；「后台另设『已存档内容』页…可以看到全部被存档的对象」。CONTEXT.md 禁止歧义别名「物理删除」。
- 证据：内存路径抹行；生产 Jdbc 留行但任何列表参数都看不到墓碑，也无按分类恢复已批准删除的读面。服务仍向调用方返回 `deleted=true`。
- 业务影响：测试绿不能代表生产可恢复；运营无法在「已存档/已删除」页找回已批准删除的商户。
- 最小处置：统一为软删留行；列表提供可审计已删除视图；改注释/决策语义；修内存仓储与单测对准 Jdbc。不改 HTML 改回物理删。

### GR-M009-007

- 文件:行号：`PlatformService.java:208-212` 存档且 `merchantType=bar` 时调用 `ticketingService.expireBarSingleStoreTickets`；相关依赖 `TicketingService.java:686-697` 注释「撤场=商户存档」
- 2.1：**冲突**
- 判据：BEQR-155、BSM-004
- 2.3 类型：1 + 源码（类型 4：本席探针用 4 参构造 `ticketingService=null`，**未实跑到期**；生产 5 参构造会注入。首轮正常存档酒吧商户会走该调用）
- 工程缺陷：**是**；建议入队：**是**
- HTML 原文：BEQR-155「商户退出不是立即生效，一律设缓冲期（业务参数，按项目可配…）」；BSM-004 存档是「选择列表隐藏、可取消存档」，不是已售权益立即到期。
- 证据：源码把存档等同撤场并立即改已售单店票 valid_to。单测/探针均未注入 TicketingService，故既有 17 PASS 不能为该分支背书。
- 业务影响：点存档即对客人已买票提前到期，无三十至六十天缓冲、无「转到其他酒吧/自行取走/退款」说明流程。
- 最小处置：存档保持可逆隐藏；退出/终止另走缓冲期状态机。不要把 B/C 授权套到缓冲期参数上。

无添加项（不走 2.2）。无 HTML 自身问题申请。无 LOCK-AMEND。B/C 不进修复队列。

## 8. 无缺陷技术性观察（无业务发明）

1. `package-info.java` 仅包名。ponytail：`delete:` 无行为，可删，净 1 行。不入队。
2. `PlatformService.requireDate` 未调用，英文 `" is required"`。死代码。`PlatformService.java:681-685`。`delete:`。
3. `JdbcPlatformRepository.readObjectMap` / `OBJECT_MAP_TYPE` 未用。
4. Controller `createSpaceType` 未预检幂等键；服务层仍拒绝空键。不一致，行为已拒。
5. `listMerchantsByBusinessEntity` 注释写零调用是隔离门禁所需。不删。
6. 列表默认 500、Jdbc COUNT+LIMIT。性能边界观察。
7. Jdbc 实现 `SupervisorReviewRepository`（约 L683–1061）：审批链 persistence，业务完整闸在后续模块。本文件 SQL 有 `FOR UPDATE`、节点 INSERT、假期日历。不把未审复核 Service 当本模块 PASS。
8. `countMerchantReferences` 动态枚举含 `merchant_id` 的基表。表名来自 information_schema。未证实注入。可能把历史行算进占用（与「活引用」比偏严）。观察。
9. 电话脱敏在 Controller、加密在 Jdbc，避免脱敏值进幂等缓存。符合 PGM-043 展示脱敏。Jdbc 加密集成测试未跑。
10. `actorId()==null` 跳过自审：Controller 先鉴权，首轮正常不发生。
11. 绑定 `effectiveFrom` 默认 `LocalDate.now(ZoneOffset.UTC)`，不是门店当地。BEQR-156 针对合同。观察。
12. 营业时间 DTO/Controller 入口存在，保存即生效/英文 `venue_id is required` 在 `VenueBusinessHoursService`（非本 20 文件）。不扩修。
13. 单元测试夹具 `permission denied` 英文是 mock，不是生产文案。
14. GEN-014 迁移不在本 FILES。
15. BEQR-063/074/147/149–153/156–159/189、BSM-005/006、PGM-007–011/013/015/019/031/035/041/042/044/048/060 等无本文件实现面，见 §5。

ponytail-review（只列，不改）：

- `MerchantRecord.java:L37-47: yagni: 19参构造为少改测试而默认擦除存档轴。读-改-写保留字段。`
- `PlatformService.java:L681-685: delete: 未用 requireDate。`
- `JdbcPlatformRepository.java:L1079-1085: delete: 未用 readObjectMap。`
- `package-info.java:L1: delete: 空包信息。`
- net: 约 −15 行可能；正确性修复（001/006）会加回读原行，净行数不作为目标。

## 9. 本席实跑命令

均未对真实业务库做创建/删除/审批。正本 20 文件 `git status --porcelain` 空。探针在 `/tmp` 编译运行后删除。

| 标签 | 命令 | EXIT | 要点 |
|---|---|---:|---|
| CMD-LOCK | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | 见文首 |
| CMD-HEAD | `git fetch` + `rev-parse` HEAD/origin/main/merge-base | 0 | 三值 `242f487233d028173cb9198eecd079f5d877405c` |
| CMD-BYTES | 20 文件 sha256/行/字节 python | 0 | 全部 match=True，合计 2476 行 |
| CMD-HTML | 69 处锁行号对现行三 HTML | 0 | MISS_COUNT=0 |
| CMD-UNIT | `mvn -Dtest=PlatformServiceTests,PlatformControllerTests test`（JAVA_HOME=temurin-21） | 0 | Tests run: 26, Failures: 0。首次系统 mvn 因 javac release 21 失败，改 JAVA_HOME 后重跑 |
| CMD-JDBC | 同上 `PlatformJdbcIntegrationTests` | 1 | 11 ERROR。根因 `FATAL: password authentication failed for user "complex"`。Spring `failure.threshold=1` 跳过其余。**未声称 JDBC PASS** |
| CMD-PROBE1 | `/tmp` 真实 `PlatformService`+内存仓储：存档后更新 | 0 | 抹存档、合作模式任意、parent 链 |
| CMD-PROBE2 | 绑定分叉+内存物理删+空幂等键 | 0 | DIVERGE=true；INMEMORY_STILL_PRESENT=false |
| CMD-SCHEMA | `sudo -n -u postgres psql complex_test` | 非 0 | sudo 需密码，未提权、未扫用户数据 |

未跑：全后端 test、`verify:predeploy`、对 `complex` 生产库、真实 HTTP 创建/删除/审批、TicketingService 到期（4 参构造为 null）。

## 10. 限制（如实，不是有条件 PASS）

- JDBC 活库路径本席未能执行：本机未导出可用的 `COMPLEX_DATABASE_PASSWORD`，测试默认口令认证失败。约束/PII 密文/部分唯一索引以源码与既有测试文本为证，不以本席本轮 JDBC 绿报。这使 GEN-002 对 Jdbc 适配器未闭合，**整席不得 PASS**。
- 酒吧存档→已售票到期（GR-M009-007）未注入 TicketingService 实跑；缺陷以生产构造与源码为准，首轮正常会走。
- 未把旧 322 模块/旧 M009 结论当本 471 模块完成。
- 未创建 worktree、未改三 HTML。
- `batch-grill-me` / `ubiquitous-language` 缺口见 §0。

## 11. 审计结论

**FAIL**

原因（任一即否）：存在未定稿工程缺陷 GR-M009-001…007；Jdbc 可执行声称未在本席命令下跑通；无有条件通过。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 Grok 独立审计原文

- 席位：固定 Grok 外审席乙
- 阶段：B1 修复前整模块全量三线审计（指定路径已有 V0.01，本次追加）
- 报告自身版本：V0.02
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；controller `160-audit-prompt.md` V0.17（本机 `.scratch/module-recheck-20260908/160-audit-prompt.md`，645 行）
- 冻结判据：87 条，与锁映射行逐字相同
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列：GR-M009-001 … GR-M009-010
- 添加项（完整列出，不代用户回答）：GR-M009-011
- 未修源码、未改三 HTML、未改台账/锁/主索引/20 源文件、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读 M009-b1-self-raw、未问操作员
- 正本 HEAD=origin/main=`242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）
- 观测台账文件头为 V2.01（开工提示写 V2.00）；本席不改台账。锁实算见本报告第一行。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能；用户指令优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 仅作优先级：用户指令 > 仓库铁律 > Arc；不走 Arc 默认审计产物 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证；不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 接口深度/删除测试；不派 DESIGN-IT-TWICE |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（临时探针）再下结论；不改生产代码 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席：步骤+完成判据同文件 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 测试在前台跑完再引用结果 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- `git-guardrails-claude-code`：本席未改 git、未直推

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行只出现在本次完整报告第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` 以及 Python `hashlib.sha256` 对照
- 结果：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值；`criteria-lock.md` 889020 bytes / 6850 行 / 版本 V0.11
- 映射行 `M009 → [BEQR-063 … PGM-066]` 与开工 87 项逐字相同：COUNT=87 unique=87 MISSING=[] EXTRA=[]
- `plan/modules.md` M009 FILES 20 条与用户清单逐字相同；`git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` 口径下内容行合计 2476
- 20 文件字节/行数/SHA256 与开工固定清单 **全部 MATCH**
- 审计结束时再次实算 20 文件 SHA 前缀，与开工一致，本席未改源

HEAD 核验：

```
git rev-parse HEAD
git rev-parse origin/main
```

两者均为 `242f487233d028173cb9198eecd079f5d877405c`。

## 2. 20 文件全量三线覆盖

阅读方式：每文件全文（Jdbc 1100、Service 739、Controller 227 未截断）。依赖只读：`ProjectRecord`、`SpaceRecord`、`SpaceTypeRecord`、`VenueBusinessHoursService`、`AuthorizationService.requirePlatformScope`、`AuditedOperationExecutor`/`IdempotencyService`、`FieldLabels`/`GlobalExceptionHandler`、`V005`/`V259`/`V318`/`V354`、既有测试三份。不扩为 M010–M013 全量审计。

### 2.1 覆盖表

| # | 文件 | 行 | 字节 | SHA256 | 三线 |
|---|---|---:|---:|---|---|
| 1 | `BusinessEntityRecord.java` | 19 | 664 | 595d53d5…eef7 | 完 |
| 2 | `BusinessEntityBindingRecord.java` | 19 | 463 | f97af4de…f422 | 完 |
| 3 | `MerchantRecord.java` | 79 | 4611 | e881a989…24ff | 完 |
| 4 | `MerchantDeleteDecision.java` | 8 | 307 | 07da344c…db33 | 完 |
| 5 | `JdbcPlatformRepository.java` | 1100 | 61439 | a9dce38d…39e3 | 完 |
| 6 | `PlatformController.java` | 227 | 13040 | 49fc52cb…5ed2 | 完 |
| 7 | `PlatformRepository.java` | 122 | 5769 | a77bff24…9ced | 完 |
| 8 | `PlatformService.java` | 739 | 34404 | 85e0d87f…472c | 完 |
| 9 | `package-info.java` | 1 | 41 | e3094c51…21d9 | 完 |
| 10 | `dto/BusinessEntityBindingRequest.java` | 17 | 442 | 66d4a8cd…11da | 完 |
| 11 | `dto/BusinessEntityRequest.java` | 17 | 431 | 85e3e5d2…867f | 完 |
| 12 | `dto/MerchantArchiveRequest.java` | 5 | 218 | 210a1af4…0b6d | 完 |
| 13 | `dto/MerchantDeleteApprovalRequest.java` | 5 | 225 | 2516fb1d…12c4 | 完 |
| 14 | `dto/MerchantDeleteRequest.java` | 5 | 214 | 4898e50f…8f45 | 完 |
| 15 | `dto/MerchantProfileRequest.java` | 27 | 767 | cbcf2424…ea99 | 完 |
| 16 | `dto/ProjectRequest.java` | 17 | 375 | 8316db69…c76e | 完 |
| 17 | `dto/SpaceRequest.java` | 21 | 525 | 63857df1…b7aa | 完 |
| 18 | `dto/SpaceTypeRequest.java` | 15 | 457 | d181151f…29f2 | 完 |
| 19 | `dto/StatusChangeRequest.java` | 11 | 231 | 75b75cfa…f60f | 完 |
| 20 | `dto/VenueBusinessHoursRequest.java` | 22 | 700 | 6347c6df…33f1 | 完 |

### 2.2 第一性原理（每文件三问）

1. **BusinessEntityRecord**  
   - 为何必须存在：BEQR-143/PGM-006/019 要落运营/合同/结算等法律主体。删掉则四类绑定无对象。  
   - 声称 vs 执行：record 只承载字段；`parentEntityId` 注释写「L3 parent=L2、L2 parent=L1 平台」。探针+锁原文证明这与 PGM-018「不是固定隶属层级」冲突（GR-M009-001）。  
   - 未验证前提：注释引 `terminology-canonical`，不是三份 HTML。三份 HTML 全文检索无「经营主体分四层：平台 → 项目公司」。

2. **BusinessEntityBindingRecord**  
   - 必须存在：BEQR-143「同一经营对象下唯一绑定」。  
   - 执行：Service 创建时查 `findActiveBusinessEntityBindingByMerchantId`；库有部分唯一索引 `uq_business_entity_bindings_active_merchant`。  
   - 前提：把 merchant 当作经营对象。创建绑定不校验商户/三个主体行是否存在（GR-M009-005）。

3. **MerchantRecord**  
   - 必须存在：BSM-028/PGM-026 商户档案。  
   - 19 参构造把存档/删除默认成 `null/"none"`。生产 `updateMerchant` 走该构造，探针 `UPDATE_WIPED_ARCHIVE=true`（GR-M009-002）。  
   - `withMaskedPhones` 服务 PGM-060/展示脱敏，controller 调用，属细化。

4. **MerchantDeleteDecision**  
   - 必须存在：删除审批结果。  
   - 注释写「物理删除」「deleted=true」；Jdbc 实际 `UPDATE delete_status='approved'` 且行仍在（SQL 探针 `still_in_table=1`）（GR-M009-006）。

5. **JdbcPlatformRepository**（1–1100 全读）  
   - 必须存在：主体/商户/项目/空间/绑定持久化。后半同时实现 `SupervisorReviewRepository`（审批链/组织层级/节假日），属本文件已有职责，不裁掉。  
   - `deleteMerchant` L170–172 不是 `DELETE FROM`。`saveMerchant` ON CONFLICT 会写出 archive/delete 列，故 19 参更新会落库擦除。`countMerchantReferences` 元数据扫带 `merchant_id` 的基表，与 BSM-004 占用检查同方向。  
   - `saveChainNodes` 循环 INSERT、本方法无事务边界；调用方若无事务会部分写入。Controller/Service 本模块不调用这些方法 → 记观察，不另发明闸。  
   - `readObjectMap`/`readStringList` 的 contract 文案残留：`readObjectMap` 无调用点（死代码，技术性）。

6. **PlatformController**  
   - 必须存在：后台 HTTP 面。写接口 `requirePermission` + 多数 `requireIdempotencyKey`；写路径 Service 再 `requirePlatformScope`。读列表只校验 view，不按项目/商户过滤。  
   - `createSpaceType`（L150–154）未调 `requireIdempotencyKey`，但 Service→`IdempotencyService.execute` 仍会要求键。观察，非绕过。  
   - 营业时间入口转到模块外 `VenueBusinessHoursService`；本 DTO/Controller 仍是本模块承载。  
   - 手机号脱敏在 controller，避免污染幂等缓存：与注释及 PGM-060 细化一致。探针证明缺幂等键原文 `idempotency_key is required` 经 `FieldLabels` 变成「请勿重复提交，请刷新后再试」（依赖模块，观察）。

7. **PlatformRepository**  
   - 必须存在：仓储缝。`listMerchantsByBusinessEntity` 注释写零调用是常态、门禁认 scoped SQL。本模块 Service/Controller **未调用**，隔离能力未接到列表入口。  
   - 默认 `deleteMerchant` 抛 `UnsupportedOperationException`；Jdbc/内存 fake 覆写语义不一致（GR-M009-006）。

8. **PlatformService**  
   - 必须存在：创建/改/存档/删审/绑定/空间类型。  
   - 主路径已用既有单测 17/17 跑通；关键缺陷路径（存档后再改档、非法 entity_role、幽灵绑定）单测未覆盖，由 `/tmp` 探针补证据。  
   - `requireDate` L681–686 无调用（死代码）。`businessId` 不识别 `SpaceTypeRecord`/`MerchantDeleteDecision`（审计 businessId 为空，观察 GEN-011）。  
   - 酒吧存档调用 `ticketingService.expireBarSingleStoreTickets(id, now)`（L210–212），无缓冲期（GR-M009-007）。

9. **package-info.java**  
   - 仅 `package com.complex.platform.governance;`。技术性包声明，无业务发明。

10–20. **DTO**  
   - 各请求体是对应入口的输入缝，删掉则 HTML 后台录入做不到。  
   - `MerchantProfileRequest.cooperationForm` 无六类约束（GR-M009-003）。  
   - `BusinessEntityRequest.entityRole`/`status` 无白名单（GR-M009-004/009/011）。  
   - `ProjectRequest` 字段是 `merchantId/venueType/floor/capacity`，不是综合体档案的负责人/地址/经营范围（GR-M009-008）。  
   - `StatusChangeRequest.action` 允许 active/inactive/suspended（GR-M009-004）。  
   - `VenueBusinessHoursRequest`：星期 0–6、跨零点、disabled 停用留行，属营业时间配置细化；保存逻辑在模块外 Service。

### 2.3 顺从式（假定实现意图正确）

数据流：HTTP `PlatformController` →（权限/幂等）→ `PlatformService`（平台 scope + `executeWrite` 幂等留痕）→ `PlatformRepository`/`JdbcPlatformRepository` → PostgreSQL。读商户：Jdbc decrypt PII → Controller 按 `pii.contact_phone.view` 决定是否 `withMaskedPhones`。存档：`saveMerchant(withArchive)` + `setMerchantArchiveReason`。删审：pending → 禁自审 → 占用计数 → `deleteMerchant`。绑定：一商户一 active，变更非 active 写 `effective_to=今天`。空间：`space_type` 必须在字典 active 码。业态：应用层+库 `chk_merchants_business_format` 六值（与 BSM-072 枚举码 media 一致，细化）。

已核对：幂等键（除 space-type 入口措辞不一致）、平台写 scope、删除申请须先存档、自审拒绝、占用拒绝、PII 信封、分页 COUNT+LIMIT、绑定部分唯一索引。这些路径与 HTML 部分对齐，不能抵消下列冲突。

### 2.4 对抗式（grilling 树，事实自取）

| 反例 | 结果 | 归入 |
|---|---|---|
| 存档后再 `updateMerchant` | `UPDATE_WIPED_ARCHIVE=true`，默认列表重新可见 | 001 队列 GR-M009-002 |
| `entityRole=not_a_real_role` | Service **接受**；DB CHECK 拒绝 | GR-M009-009 |
| 绑定 `merchant_id` 不存在 | Service **接受** | GR-M009-005 |
| `cooperationForm=fixed_rent_plus_commission` / `revenue_share` | 接受并落 record | GR-M009-003 |
| 商户账号写治理 | `PLATFORM_SCOPE_REQUIRED`（文案却是「无权查看平台级经营分析」） | 观察 |
| 商户账号直接调 Service 列表 | `LIST_AS_MERCHANT=ACCEPTED count=1`（无商户过滤） | 观察 OBS-02 |
| 非法 entity_role INSERT | `BAD_ROLE_INSERT=CHECK_VIOLATION` | GR-M009-009 |
| 执行 Jdbc 同款 `UPDATE delete_status='approved'` | 行仍在，`still_in_table=1` | GR-M009-006 |
| 第二份 active 绑定 | 库唯一索引 `uq_business_entity_bindings_active_merchant`；Jdbc 测断言该名 | 顺从式通过 |
| 合作方式/商户状态 CHECK | 库无 `cooperation_form`/`status` 枚举约束 | GR-M009-003/004 |
| 并发双 active 绑定 | 部分唯一索引覆盖真实窗口 | 非本批缺陷 |
| U 盘最高权限直删 | 无入口，仍须申请+他人审批 | GR-M009-010 |

未把「要两三件不巧才发生」的理论项升级为缺陷（例如 `saveChainNodes` 无事务取决于调用方）。

## 3. 87 冻结判据逐项

判据全文来自 `criteria-lock.md` 定义区；HTML 原文已从锁内 ````html` 块与三份正本对照。类型 1=HTML 原文，2=可执行产物，4=命令输出。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 高危动作（沽清/反结等） | 无酒吧现场动作实现 | **无实现面**：本 20 文件不承载沽清/反结；商户删除走另一套审批，不把 HTML 沉默当冲突 |
| BEQR-074 隐私桌台 | 无 | **无实现面** |
| BEQR-143 四类主体 | 绑定四字段+唯一 active | **FAIL** GR-M009-005：创建商户不生成唯一绑定；可绑幽灵商户 |
| BEQR-144 经营主体关系 多对多 | `parentEntityId`+注释四层链 | **FAIL** GR-M009-001 |
| BEQR-145 城市项目公司 | 无独立「只做资产、不管酒吧」的校验 | **部分**：角色 `project_company` 仅能作为字符串写入；职责边界无代码执行。不因沉默判删 |
| BEQR-146 运营公司职责 | 无票务/结算执行 | **无实现面**（职责表在 PGM-019，本模块只存主体行） |
| BEQR-147 收入归属 | 无计费 | **无实现面** |
| BEQR-148 合作模式六类 | `cooperationForm` | **FAIL** GR-M009-003 |
| BEQR-149 职级 | 无 | **无实现面** |
| BEQR-150 终端十一端 | 无 | **无实现面** |
| BEQR-151 最终权限计算 | 无 | **无实现面**（只调 `AuthorizationService` 权限码） |
| BEQR-152 项目隔离 | 列表无 project 谓词 | **观察 OBS-02**：HTTP 列表只校验功能权限；V005 把 view 授给平台管理员。未证明商户角色持有 view。不把「可能下放」升级为冲突 |
| BEQR-153 终端开关 | 无 | **无实现面** |
| BEQR-154 综合体项目状态 | `ProjectRequest.status` 默认 `active` | **FAIL** GR-M009-004；且 `/projects` 并非综合体档案 GR-M009-008 |
| BEQR-155 商户/运营主体状态+退出缓冲 | status=`active`/`draft`；存档立即停票 | **FAIL** GR-M009-004、GR-M009-007。缓冲期天数是业务参数，不拿 30/60 当冲突；缺「不是立即生效」是冲突 |
| BEQR-156 合同状态 | 无合同状态机 | **无实现面** |
| BEQR-157 员工状态 | 无 | **无实现面** |
| BEQR-158 设备状态 | 无 | **无实现面** |
| BEQR-159 权限状态 | 无 | **无实现面** |
| BEQR-166 合同审批链 | Jdbc 实现审批链表，本模块 API 不暴露 | **无本模块入口**：持久化供他模块；不把缺合同审批 API 当本模块删除项 |
| BEQR-189 管理费 | 无 | **无实现面** |
| BSM-001 整体说明 | 综合体盘子入口 | **细化**：本模块是后台主数据 API，不实现全图 |
| BSM-003 通用能力总述 | 存档删除分岗 | 见 BSM-004/007 |
| BSM-004 存档/删除审批/软删可恢复 | 存档+申请+审批+占用 | **FAIL** GR-M009-002/006/010。单条存档免审批符合「逐条存档不用审批」 |
| BSM-005 分步引导 | 后端一次 POST | **无实现面（UI）**：HTML 改的是后台页；一次 API 可被分步前端调用。不因后端单请求判冲突 |
| BSM-006 动态二维码 | 无 | **无实现面** |
| BSM-007 平台管理员≠最高权限；删除须单授 | `merchant.profile.approve` 与 manage 分离 | **部分对齐**。依赖 V259 把 approve 写入 `role_platform_admin`（他模块）。观察 OBS-03，不把迁移种子算成本 20 文件发明 |
| BSM-009 基础管理入口 | `/api/v1/platform/*` | **细化** |
| BSM-028 主要功能（综合体/商户/多对多/六类合作） | 全文件 | **FAIL** 001/003/008 |
| BSM-029 和谁有关 | 无票务/酒吧执行 | **无实现面**（关系说明，本模块只提供被引用主数据） |
| BSM-072 业态六值+枚举码 media | `MERCHANT_BUSINESS_FORMATS` | **通过（细化）**：应用+库 CHECK 与 HTML「系统枚举码 media」一致。探针非法业态单测 17 套件已拒 |
| BSM-074 商户管理相关方 | 无 | **无实现面** |
| BSM-114 各类入口 | 本模块是管理后台 API | **细化** |
| BSM-115 基础管理连所有业务 | 主数据供下游引用 | **细化**；不在此实现卡住后续流程 |
| BSM-120 已拍板清单 | 多条非本模块 | **无实现面**（集团视图预留见 008 缺口） |
| GEN-001 | 死代码/双写 | OBS-04 `requireDate`、`readObjectMap`；19 参兼容构造造成 002，不能当无害兼容（铁律 8 不为存量兼容） |
| GEN-002 | 声称物理删 vs UPDATE | **FAIL** 006；单测 17 PASS 不能替代 |
| GEN-003 | 关键分支 | **FAIL**：17+9 绿测未覆盖存档后更新、Jdbc `deleteMerchant`、非法 role。Jdbc 11 测 1 红为共享库 Flyway 409≠源 399，见命令证据 |
| GEN-004 | 输入校验 | **FAIL** 003/005/009 |
| GEN-005 | 授权 | 写路径 `requirePlatformScope` 拒商户账号（探针）。读路径无 scope。OBS-02 |
| GEN-006 | 完整性 | **FAIL** 002；绑定 unique index 通过 |
| GEN-007 | 幂等 | 写路径经 `IdempotencyService.requireIdempotencyKey`。space-type 控制器漏检但仍进同一执行器。观察 |
| GEN-008 | 跨层一致 | **FAIL** 004（英/中状态）；内存 fake 物理删 vs Jdbc 软标 |
| GEN-009 | 错误 | **FAIL** 009：非法 role 期望 400，DB CHECK 走未拦的完整性异常（handler 注释：NOT NULL/CHECK 落 500） |
| GEN-010 | 安全/PII | 商户电话 encrypt/decrypt + 展示脱敏。Jdbc PII 测在 11 件中未失败（失败件是 Flyway 版本断言）。**本项通过** |
| GEN-011 | 留痕 | `executeWrite` 记审计。SpaceType/DeleteDecision 的 businessId 为 null。观察 |
| GEN-012 | 资源 | 分页上限 500；`countMerchantReferences` 按表 COUNT。观察，非首轮故障 |
| GEN-013 | 构建 | 单测可在无 DB 下跑通。Jdbc 测依赖 `COMPLEX_DATABASE_PASSWORD` 与 `complex_test` |
| GEN-014 | 迁移 | **无本模块迁移文件**（M010）。Jdbc SQL 依赖 V318 parent 列、V354 role CHECK、V259 删除列 |
| GEN-015 | 中文展示 | 业务拒识多为中文。混排 `(bar/catering/...)` 与 `approved` 作系统内部标识符，不升冲突。缺键翻译成「请勿重复提交」在 FieldLabels（他模块） |
| GEN-016 | 文档 | 本 20 文件几乎无对外文档声称 |
| GEN-017 | 四分类 | 本报告按冲突→细化→技术性→添加 |
| GEN-018 | 证据类型 | 下列 finding 均落到 HTML 原文或命令输出 |
| PGM-001～003 总述/顺序/模块入口 | 入口卡片 | **细化**：本模块是治理后台实现的一段 |
| PGM-004 综合体和集团 | `/projects`+主体 | **FAIL** 008 |
| PGM-005 空间和点位 | spaces + 类型字典 | **细化**：字典硬校验存在；点位种类用 typeCode |
| PGM-006 商户和主体 | merchants+bindings+entities | **FAIL** 001/003/005 |
| PGM-007～016 合同/员工/权限/终端/设备/商户侧/开放/留痕/楼宇/应急（入口卡） | 无对应 API | **无实现面**（入口说明；权限码在本模块只引用） |
| PGM-017 带综合体项目归属 | 主体/商户无 complex id；project 挂 merchant | **FAIL** 008 |
| PGM-018 多对多非隶属 | parent 链 | **FAIL** 001 |
| PGM-019 各主体职责表 | 无职责执行 | **无实现面** |
| PGM-020 建立综合体项目档案 | `createProject` | **FAIL** 008 |
| PGM-021 业务意义 | 无集团视图 | **无实现面**（预留失败见 008） |
| PGM-026 商户档案/六类合作/四类绑定 | 全商户/绑定路径 | **FAIL** 003/005 |
| PGM-027 业务边界 | 写拒商户账号 | **细化** |
| PGM-031 经销商 | 无 | **无实现面** |
| PGM-035 提成人员 | 无 | **无实现面** |
| PGM-040～044 权限职级桌台 | 只引用权限码 | **无实现面**（单一功能授权在 access 模块） |
| PGM-048 商户侧全开放 | 本模块是平台后台 | **无实现面** |
| PGM-060 会员/隐私 | 联系人电话脱敏+加密 | **细化通过**（PII 路径） |
| PGM-065 连接关系 | 主数据被引用 | **细化** |
| PGM-066 已拍板/待排期 | 无待排期执行 | **无实现面** |

## 4. Findings

### GR-M009-001 — 用 parent 链实现「平台→项目公司→业态主体」固定隶属

- **冻结判据**：PGM-018、BEQR-144、BSM-028  
- **2.3 类型**：1 HTML 原文 + 4 命令（三份 HTML 检索无「四层隶属」句）  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`BusinessEntityRecord.java:9-11`；`PlatformService.java:116-127`（原样写入 `parentEntityId`）；`JdbcPlatformRepository.java:47-65,488-500`  
- **HTML 原文**：PGM-018「物理项目与运营公司是多对多协作，不是固定隶属层级」「一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。」BEQR-144「不是固定的“平台→项目公司→域公司→商户”行政隶属链。」  
- **实现**：单父指针 + 注释「L3 的 parent = L2 项目公司；L2 的 parent = L1 平台」。一个管理公司不能挂多个项目。  
- **业务影响**：集团/多项目协作被编码成行政树，后续权限和归属会按错误拓扑走。  
- **最小反例**：`createBusinessEntity(parentEntityId=be_proj_changsha, entityRole=management_company)` 被接受；无法表达「同一娱乐平台公司服务两个城市项目」。  
- **最小处置**：停止把 parent 当行政隶属；主体与项目的多对多用绑定/协作关系表达。不要加新层级状态机。  
- **不是 HTML 问题**：三份正本检索 `经营主体分四层|平台 → 项目公司` 为 0。V354 注释伪称出自 PGM，属他模块迁移文案，本席不上修 HTML。

### GR-M009-002 — 更新商户档案擦掉存档与删除申请

- **冻结判据**：BSM-004、GEN-006、GEN-002、GEN-003  
- **2.3 类型**：2 可执行产物（临时探针）  
- **四分类**：**冲突**（与「存档可恢复、列表隐藏、删除须先存档」矛盾，不是新业务）  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:599-639` 走 19 参构造；`MerchantRecord.java:37-46` 默认 `archivedAt=null, deleteStatus="none"`；`JdbcPlatformRepository.java:97-120` ON CONFLICT 写出这些列  
- **HTML 原文**：BSM-004「点存档就从各处的选择列表里隐藏……想恢复点取消存档即可。」  
- **命令证据**（`/tmp/m009-b1-probe`，javac+java exit 0）：

```
ARCHIVE_AT=2026-09-09T06:58:04.439189087Z
LIST_DEFAULT_HAS=false
UPDATE_ARCHIVE=null
UPDATE_WIPED_ARCHIVE=true
```

- **业务影响**：已下线商户被一次「改资料」重新出现在默认列表；挂起的删除申请被清成 none，审批链被绕过。首轮后台改档就会发生。  
- **最小处置**：更新时先读已有行，只覆盖档案字段，保留 archived_*/delete_*。删掉把语义当「旧构造默认值」的 19 参生产路径。

### GR-M009-003 — 合作方式不校验六类，并接受发明值

- **冻结判据**：BEQR-148、PGM-026、BSM-028  
- **2.3 类型**：1 + 2  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`MerchantProfileRequest.java:23`；`PlatformService.java:633` `trimToNull(request.cooperationForm())` 无白名单；库无 cooperation CHECK  
- **HTML 原文**：BEQR-148「自营、租赁、联营、代运营、服务商接入、其他。」PGM-026 同六类。  
- **证据**：探针 `CREATE_COOP=fixed_rent_plus_commission` 后更新为 `revenue_share`。单测 `updateMerchantWritesAuditAndPreservesId`、Jdbc 夹具、Controller 断言都把发明值当正确。  
- **业务影响**：财务/招商按六类合作无法分类；「其他须指定收费模板」在商户档这一层无法卡住。  
- **最小处置**：只接受六类（内部码若需要，映射到这六类中文口径）；拒绝 `fixed_rent_plus_commission` 这类收费项冒充合作方式。收费项属合同，不在本字段发明。

### GR-M009-004 — 主体/商户/项目/绑定状态不是速查表枚举

- **冻结判据**：BEQR-154、BEQR-155、GEN-008  
- **2.3 类型**：1 + 2  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:124,634,368,459,526` 默认 `"active"`；绑定变更 L552 `active|inactive|suspended`；Jdbc 测使用 `"draft"`  
- **HTML 原文**：BEQR-154「草稿、待启用、已启用、暂停、终止、归档」。BEQR-155「待准入、正常、暂停、终止、归档、黑名单」。  
- **证据**：探针 `CREATE_STATUS=active`。库无 merchants.status CHECK。  
- **业务影响**：前台无法按业务状态经营；「归档」被拆成 archivedAt 而 status 仍可是 active（与 002 叠加）。  
- **最小处置**：状态机与速查表中文口径对齐（内部码可映射，API/校验不得只认 active/draft）。不要另造第七态。

### GR-M009-005 — 四类主体唯一绑定不是创建商户的必经，且可绑不存在对象

- **冻结判据**：BEQR-143、PGM-026、GEN-004  
- **2.3 类型**：1 + 2  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:157-163,511-533`；`createBusinessEntityBinding` 只 `requireText` 三个主体 ID  
- **HTML 原文**：BEQR-143「经营对象、运营主体、合同主体、结算主体（可分属不同公司，同一经营对象下唯一绑定）」。PGM-026「绑成一组唯一关系」「签约转商户……四类主体默认指向同一公司、后续可拆」。  
- **证据**：探针 `GHOST_BINDING=ACCEPTED`。创建商户可不写绑定表。商户行上 `businessEntityId/contractEntityId/settlementEntityId` 与绑定表可分叉。  
- **业务影响**：合同/结算/权限无法引用「同一组绑定」；幽灵 ID 进入绑定表。  
- **最小处置**：创建或首次启用商户时写一组绑定（默认同一公司）；绑定的四个对象必须存在。已有 active 唯一索引保留。

### GR-M009-006 — 删除声称物理删，实现是改状态；内存测与 Jdbc 不一致；无恢复

- **冻结判据**：BSM-004、GEN-002、GEN-003、GEN-008  
- **2.3 类型**：1 + 4 SQL 探针 + 2 单测读码  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`MerchantDeleteDecision.java:3-5`；`PlatformService.java:272-317` 注释「物理删除」；`JdbcPlatformRepository.java:170-172`；测试 fake `PlatformServiceTests.java:491-493` `merchants.remove`  
- **HTML 原文**：BSM-004「删除不是真从数据库里抹掉……数据始终留在库里可查、可审计、可恢复」。  
- **证据**：

```
UPDATE merchants SET delete_status = 'approved' ...
id=mch_m009_del delete_status=approved still_in_table=1
ROLLBACK
```

单测 `approveDeleteRemovesArchivedMerchantWhenNoReferences` 断言 `listMerchants(true)` 不含该商户，是因为内存 **remove**，不能代表 Jdbc。  
- **业务影响**：审批通过后运营以为数据已灭失或以为可进「已删除分类恢复」，两边都不真。includeArchived 仍过滤 `delete_status<>'approved'`，已删行两边列表都看不见且无恢复 API。  
- **最小处置**：按 BSM-004 软删：留行、前台不显、分类可查可恢复；改掉「物理删除」注释/决策语义；测试打真实 Jdbc 路径，不要用 Map.remove 冒充。不要真 `DELETE FROM`。

### GR-M009-007 — 酒吧商户存档立即停票，没有退出缓冲期

- **冻结判据**：BEQR-155  
- **2.3 类型**：1 + 源码调用链（TicketingService 只读）  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:208-212` 注释「存档=撤场」并立即 `expireBarSingleStoreTickets(id, now)`  
- **HTML 原文**：BEQR-155「商户退出不是立即生效，一律设缓冲期（业务参数……）」。缓冲天数本身是用户原意业务参数，**不拿 30/60 当冲突**；缺「不是立即生效」是冲突。  
- **业务影响**：点存档当日客人票被提前到期，缓冲期内「转到其他酒吧/自行取走/退款」来不及发生。首轮存档酒吧即发生。  
- **最小处置**：存档继续隐藏新业务选用（BSM-004）；退出/撤场停票放到缓冲期满之后的既有状态机，不要在 archive 当时调用 expire。

### GR-M009-008 — `/projects` 不是综合体项目档案

- **冻结判据**：PGM-020、PGM-017、PGM-004、BSM-028  
- **2.3 类型**：1 + 4（字段清单）  
- **四分类**：**冲突**  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`ProjectRequest.java:9-15`；`PlatformService.java:345-373`；依赖 `ProjectRecord`（不在 20 文件内，只读）`merchantId, venueType, name, code, floor, capacity`  
- **HTML 原文**：PGM-020「建立综合体项目档案」「记录项目状态、负责人、地址和经营范围」「为商户、空间、合同、员工、设备、票务和看板提供归属」。PGM-017「所有基础对象都要带综合体项目归属」。  
- **实现**：项目挂在商户下，带楼层与容量，像场馆/点位。主体与商户行没有综合体项目 ID。  
- **业务影响**：未来集团视图/项目隔离没有归属根；城市项目公司与「一个综合体」对不上这条 API。  
- **最小处置**：综合体项目档案按 PGM-020 字段建；现 `projects` 若实为场地，应改口或迁到空间，不得继续冒充综合体。

### GR-M009-009 — 主体角色应用层不校验，非法值只在 DB CHECK 以非业务错误失败

- **冻结判据**：GEN-004、GEN-009  
- **2.3 类型**：2 + 4  
- **四分类**：**冲突**（错误处理/信任边界，无新业务概念）  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:113-127` 只 `requireText(entityRole)`；`GlobalExceptionHandler` 不拦 CHECK（依赖，只读）  
- **证据**：探针 `INVALID_ROLE=ACCEPTED`；SQL `BAD_ROLE_INSERT=CHECK_VIOLATION`。  
- **业务影响**：后台录入错角色得到 500/英文完整性错误，而不是可理解的中文校验失败。首轮录入非法角色即发生。  
- **最小处置**：在 Service 按已有主体角色词表拒收，返回中文 VALIDATION_FAILED。词表与 011 添加项一并提交，不在本项发明新角色。

### GR-M009-010 — 最高权限 U 盘免审批删除未实现

- **冻结判据**：BSM-004  
- **2.3 类型**：1  
- **四分类**：**冲突**（明文例外未落地，不是新闸）  
- **工程缺陷**：是；**建议入队**：是  
- **位置**：`PlatformService.java:242-318` 删除只有 存档→申请→他人审批；无密钥持有人直达  
- **HTML 原文**：BSM-004「删除一律须由具备最高权限的人审批……不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」  
- **首轮**：最高权限本人删除仍被要求 pending+他人审批，会撞上。  
- **最小处置**：识别既有最高权限/U 盘执行者后允许其发起即执行软删并留痕；不要新造审批框架。申请人禁自审保留。

### GR-M009-011 — 添加：主体角色 `spv` / `operation`

- **冻结判据**：GEN-017、PGM-018/019（对照最近条目）  
- **2.3 类型**：1  
- **四分类**：**添加**  
- **工程缺陷**：分类待 2.2；**建议入队**：否（B2 先问）  
- **位置**：本模块 `createBusinessEntity` 接受任意 `entityRole`；库 CHECK 含 `'platform','project_company','management_company','spv','operation'`（V354，他模块，只读验证）  
- **HTML**：三份 HTML 无 SPV/特殊目的载体，也无「operation」角色名。有的是顶层平台、城市项目公司、域主管公司/娱乐平台公司等。  
- **添加了什么**：可把主体存成 `spv` 或历史 `operation`。  
- **添加后代价**：与四类主体/公司职责表对不上，收入归属引用会漂。  
- **删除代价**：要收紧白名单并处理谁再写这些角色（不为存量做回填，铁律 8）。  
- **供主调度 2.2**：三份 HTML 中无相关条目；最接近 PGM-019 主体表。不代用户答保留/删除。

## 5. 观察（无新根因缺陷，完整登记）

- **OBS-01** `createSpaceType` 控制器未调 `requireIdempotencyKey`，Service 仍强制。GEN-008 入口不一致，非绕过。  
- **OBS-02** 列表 API 不调 `requirePlatformScope`、不调 `listMerchantsByBusinessEntity`。探针显示商户身份直调 Service 能列出他商户。HTTP 另有权限码；V005 仅把 view 授给平台管理员。不升冲突。  
- **OBS-03** V259 将 `merchant.profile.approve` 写入 `role_platform_admin`（他模块）。与 BSM-007「删除不因平台管理员自动获得」张力留给持有该迁移的模块。  
- **OBS-04** `requireDate` 死代码；`readObjectMap` 无调用。技术性，无业务发明：纯编译残留。ponytail-review：`PlatformService.java:L681-686 delete: unused requireDate. nothing.` `JdbcPlatformRepository.java:L1079-1085 delete: unused readObjectMap. nothing.` net: 小。  
- **OBS-05** `package-info.java` 空包声明。技术性。  
- **OBS-06** Jdbc 后半审批链/节假日：本模块 API 不调用。`saveChainNodes` 无本方法事务。不首轮发生在本 API。  
- **OBS-07** 写失败文案「商户账号无权查看平台级经营分析」用在创建商户。依赖 `AuthorizationService`，观察。  
- **OBS-08** `FieldLabels` 把缺幂等键译成「请勿重复提交，请刷新后再试」。他模块。  
- **OBS-09** `ProjectRecord`/`SpaceRecord`/`VenueBusinessHoursService` 不在 20 文件清单，属切分缝，不 LOCK-AMEND。  
- **OBS-10** 内存 `deleteMerchant` 与 Jdbc 语义分叉已并入 006，不另开根因。  
- **OBS-11** 共享 `complex_test` Flyway 版本 409 > 源码 latest 399，Jdbc 套件 1 红。环境漂移，不是本 20 文件逻辑。见下。

## 6. 命令证据

### 6.1 锁与文件指纹

`sha256sum` + Python hashlib：锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。20 文件 ALL_MATCH（见 §1）。

### 6.2 单元测试（本席实跑）

```
cd backend
mvn -Dtest=com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformControllerTests test
```

Surefire：`PlatformServiceTests` tests=17 failures=0 errors=0；`PlatformControllerTests` tests=9 failures=0 errors=0。exit 0。  
不能据此称行为符合 HTML（GEN-003）：绿测未覆盖 002/005/009。

### 6.3 JDBC 集成测试（本席实跑）

只导出三个 `COMPLEX_DATABASE_*`（URL 改写为 `complex_test`，不打印密文）。`psql SELECT current_database()` → `complex_test`。未跑清理脚本，未对真实业务库做审批/删除（SQL 探针包在 BEGIN/ROLLBACK）。

```
mvn -Dtest=com.complex.platform.governance.PlatformJdbcIntegrationTests test
```

**Tests run: 11, Failures: 1, Errors: 0。BUILD FAILURE。不得报 PASS。**  
失败：`residualMigrationCreatesBindingContractTablesAndSeedsPermissions:263 expected "399" but was "409"`。Flyway 日志：`Schema public has a version (409) that is newer than the latest available migration (399)`。  
未闭合影响：不能用该断言证明源码 latest 与库一致；其余 10 个（分页、主体/商户/项目空间落库、绑定唯一、PII 加密）在本次 run 中未在失败列表出现。本席不把那 10 个改写成「套件通过」。

### 6.4 临时探针（自动清理用途，未写入正本测试）

`/tmp/m009-b1-probe/M009B1Probe.java`，javac --release 21，java exit 0。关键输出见 002/003/005/009。

### 6.5 SQL 探针（事务回滚）

非法 `entity_role` → CHECK_VIOLATION。`UPDATE delete_status='approved'` 后行仍在。`uq_business_entity_bindings_active_merchant` 部分唯一索引存在。`chk_merchants_business_format` 六值。无 cooperation/status 枚举 CHECK。全部 ROLLBACK。

## 7. 审计结论

**FAIL**

原因（任一即不得 PASS）：存在未定稿工程缺陷 GR-M009-001…010；添加项 GR-M009-011 未答；Jdbc 套件未绿。无有条件通过。

本席禁止修复。B2 分配正式 ID、四分类定稿、添加项走 questions。20 源文件/三 HTML/锁/主索引字节本席未改。


## M009 B2-r1 整批归并复核（Grok 席乙）
文件自身版本：V0.03（本段追加；上方 V0.02 B1 全文 75079 bytes 原样保留，不改写第 386 行锁段起点，不覆盖历史 36869 bytes 前缀）

- 席位：Grok（固定外审席乙）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）；ledger V2.02 IN_PROGRESS；B3=0。
- 正本：`/home/james/complex-management` main `HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）。
- 本席 B2：**FAIL**
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。sidecar 全文一行同值；ledger 顶行同值。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未用席甲本批 B2 追加段作证据（self-raw 在 31878 字节前缀之后另有追加，只保留不采信）。
- 技能按 B2 收窄：using-superpowers / grilling（自问自答取证）/ verification-before-completion / ponytail（最懒可行，不落地代码）。未冒称 batch-grill-me。

此票只判断 findings.md 顶部 V1.17「当前 M009 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷与修复队列。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.02 B1 **FAIL** 原文。席甲票仍是作者自审票；本席 FAIL 具否决权。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.17，449655 bytes，SHA256 `443c7380e0aa6581823384ef6c35e6bf306be73e432756fdbae1895938eefdd4`。只读最上「当前 M009 B2-r1 整批候选」M009-001…025；下方他模块历史原文不重审、不当依据。
- 席甲 B1：`docs/audit-2026-09/modules/M009-b1-self-raw.md` V0.01 前缀 31878 bytes，SHA256 `76f4af4b282b89831e27f6e32b003e0980a4a5b1f64d2e457cd6314a201da0a6`（Claude Fable 5.1，操作员 2026-09-09 接替指令）。只读该前缀；其后追加不作为本席依据。
- 本席 B1：本文件 V0.02 全文 75079 bytes，SHA256 `37c0eca7809fa90d319c5dfd51c05cd4256c011c69c56ff620c5809f36c51cf4`；本次完整报告=第 386 行独占锁行至当时 EOF。前 36869 bytes 为 Codex 首派、退出码未观测的历史报告，只保留不采信。
- 冻结判据：映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 unique=87。本席独立 `sha256sum`+Python hashlib 与 sidecar/ledger 三值全等。20 文件行合计 2476，SHA 前缀与 B1 覆盖表 MATCH；相对 HEAD 这 20 路径 `git diff --stat` 空。

### 差集（无遗漏、不改写本席 B1 原意）

| 本席 B1 | 席甲 B1 | 候选 ID | 差集结论 |
|---|---|---|---|
| GR-M009-002 冲突 | CX-01 细化 | 001 | 并入。分类取冲突（改档擦存档与 BSM-004「想恢复点取消存档」明文相反）。不改写本席原意。 |
| （无独立 GR；并 CX-02） | CX-02 | 002 | 并入。技术性成立。 |
| GR-M009-003 冲突 | CX-03 细化 | 003 | 并入。分类取冲突。跨文件最小改动见下裁定。 |
| GR-M009-004 冲突 | CX-04（含 BEQR-154 项目状态；候选误写 CX-20，席甲 B1 无 CX-20） | 004 | 并入，无丢条。不另开 ID。 |
| GR-M009-009 冲突（错误处理） | CX-05 技术性 | 005 | 并入。分类改技术性成立：三角色有 PGM-018/019 上位，不发明 spv/operation。 |
| GR-M009-005 冲突（存在性+建档即绑定） | CX-06 细化 | 006 | 并入后拆半：存在性入队；建档即绑定仅登记。不把本席「创建商户必写绑定」原句改写成 HTML 已要求建档即绑定。 |
| （无独立 GR） | CX-07 | 007 | 并入。 |
| （GR-005 双源一句） | CX-08 | 008 | 并入。本席 B1 把双源写在 005 内，候选单列不丢。 |
| GR-M009-010 冲突 | CX-09 冲突 | 009 | 并入。两面合并一 ID 成立。 |
| GR-M009-006 冲突 | CX-10 | 010 | 并入。 |
| GR-M009-007 冲突 | （席甲无对等 CX） | 011 | 并入。不丢本席停票项。 |
| GR-M009-001 冲突 | OBS-06 技术性 | 012 | 并入。分类取冲突，见下裁定。 |
| GR-M009-008 冲突 | OBS-02 中 PGM-020 字段遗漏 | 013 | 并入。分类可收窄为细化（遗漏）；**队列候选「是」不成立**，见下裁定。 |
| （无独立 GR） | CX-11 | 014 | 并入。 |
| OBS-02 观察 | CX-12 缺陷 | 015 | 并入。本席独立升级为缺陷入队，见下裁定。 |
| OBS-04 | （席甲无对等） | 016 | 并入。 |
| OBS-11 | OBS-01 | 017 | 并入。环境、否队列。 |
| （OBS-02 其余 / 缓冲期） | OBS-02 其余 | 018 | 并入。无实现面遗漏，否。 |
| OBS-03（V259 种子张力） | OBS-03 同构审批 | 019 | 并入跨模块登记，不另开。 |
| （无独立 GR） | OBS-04 listMerchants 无过滤消费方 | 020 | 并入。 |
| （无独立 GR） | OBS-08 comment | 021 | 并入。 |
| OBS-01 | OBS-07 | 022 | 并入。 |
| OBS-05/06/09/10 | OBS-05/09/10/11 | 023 | 并入登记。 |
| OBS-07/08 | （席甲无对等） | 024 | 并入跨模块。 |
| GR-M009-011 添加 | （席甲 GEN-017 写无添加） | 025 | 并入。路由 M010，本批不发 2.2，见下裁定。 |

席甲 CX-01…12、OBS-01…11 与本席 GR-001…011、OBS-01…11 均落在 001–025，无第三条根因、无新 ID、无 LOCK-AMEND、无 HTML 修改申请。

### 逐 ID 票（001–025）

| ID | 分类 | 工程缺陷 | 修复队列 | 本席票 | 理由（四分类→最懒处置） |
|---|---|---|---|---|---|
| 001 | 冲突 | 是 | 是 | PASS | BSM-004 存档须取消存档才回到列表；19 参更新写出 archived_*/delete_*。最小：更新先读行、25 参保留这些列。不改列表过滤。 |
| 002 | 技术性 | 是 | 是 | PASS | 无 HTML 授权以 PUT 凭客户端 id 建档。与 001 同处 requireMerchant→404。无业务发明。 |
| 003 | 冲突 | 是 | 是 | PASS | 见重点裁定。六类代码为既有中文口径的技术映射；跨文件三处归因同一 ID 接受。 |
| 004 | 冲突 | 是 | 是 | PASS | BEQR-154/155 明文六态；任意字符串入库。归档走 archived_at 端点、不作可设代码，避免与 V259 双轴发明第七态。不造状态机/缓冲期（018）。绑定三态 HTML 沉默，保持技术态。 |
| 005 | 技术性 | 是 | 是 | PASS | 服务层白名单 platform/project_company/management_company 对应 PGM-018/019 顶层平台/城市项目公司/域主管公司。非法值改 400 中文，不发明 spv/operation（025）。 |
| 006 | 细化 | 是 | 是（仅存在性） | PASS | 前半：绑定前校验四对象存在。后半建档即绑定：仅登记，见重点裁定。 |
| 007 | 细化 | 是 | 是 | PASS | 与 006 同法存在性。PGM-001/005 归属链。 |
| 008 | 细化 | 是 | 是 | PASS | 见重点裁定。**不改否**。 |
| 009 | 冲突 | 是 | 是 | PASS | 见重点裁定。CX-09 与 GR-010 合并成立。 |
| 010 | 冲突 | 是 | 是 | PASS | BSM-004「不是真抹掉…可查可审计可恢复」。改注释；includeArchived 纳入 approved；恢复复用 unarchive。不真 DELETE。M011 内存 remove 归 M011。 |
| 011 | 冲突 | 是 | 是 | PASS | 见重点裁定。删除调用是最懒，不留半成品。 |
| 012 | 冲突 | 是 | 是 | PASS | 见重点裁定。删除范围成立、不越界。 |
| 013 | 细化（遗漏） | 否（无本模块可修实现面） | **否** | **FAIL** | 见重点裁定。候选「是（B2裁定）」+ V410 三列不是 B3 可做的最小修复。 |
| 014 | 技术性 | 是 | 是 | PASS | pageSize=50 与全量 items。改 PageResponse.of 或删 page()。无业务发明。 |
| 015 | 细化 | 是 | 是 | PASS | 见重点裁定。本席 B1 观察升级为缺陷入队，非合取表演。 |
| 016 | 技术性 | 是 | 是 | PASS | GEN-001 删 requireDate / readObjectMap。 |
| 017 | 技术性（环境） | 否 | 否 | PASS | 见重点裁定。 |
| 018 | 细化（遗漏，无实现面） | 否 | 否 | PASS | 缓冲期/已存档内容页/数据清理/商户无 project_id 倒置归属。013 否后 PGM-020 三字段并此口径，不另开 ID。 |
| 019 | 技术性（跨模块） | 否 | 否 | PASS | 同构审批归各模块 B1。 |
| 020 | 技术性（跨模块） | 否 | 否 | PASS | 无过滤 listMerchants 消费方归合同/计费模块。本模块保留两种方法。 |
| 021 | 技术性 | 否 | 否 | PASS | 审批意见未落痕；留痕以 delete_reason 为准。仅登记。 |
| 022 | 技术性 | 否 | 否 | PASS | 控制器漏检幂等键但执行器仍必填。行为一致，不改。 |
| 023 | 技术性 | 否 | 否 | PASS | 切分缝/COUNT 循环/姓名明文/读端审计/中文错误。不 LOCK-AMEND。 |
| 024 | 技术性（跨模块） | 否 | 否 | PASS | 守卫文案与 FieldLabels 归 access/common。 |
| 025 | 添加（内容在 M010） | 待 M010 | 否 | PASS | 见重点裁定。本批不发 2.2，不构成绕过。 |

### 重点独立裁定

#### 012 parent 链

本席 B1 GR-M009-001 判冲突，原意保持：BEQR-144「不是固定的“平台→项目公司→域公司→商户”行政隶属链」；PGM-018「物理项目与运营公司是多对多协作，不是固定隶属层级」。`BusinessEntityRecord.java:9-11` 注释把 L3.parent=L2、L2.parent=L1 写成行政树，DTO/Service/Jdbc 原样写入。冲突优先于「0 消费方故技术性」。

本席本批复核（类型 4）：

- `parentEntityId|parent_entity_id` 生产 Java：仅 M009 四文件（Record/DTO/Service/Jdbc）+ M010 的 V318/V354。
- `apps/web` 全树 0 命中。
- governance 外生产调用方 0。结算侧仅测试插入/读回该列（`RevenueStreamAttributionJdbcIntegrationTests`、`SettlementEntityAttributionS1Tests`），不是生产消费。
- 列本身留在库里，那些测试仍可直写 SQL。

最小处置（ponytail）：从 M009 的 DTO、Record、Service 写入、Jdbc SQL 映射删除 `parentEntityId`/`parent_entity_id`。**不** DROP 列、**不**改 V318/V354、**不**新建协作关系表（那是添加）。列与注释归 M010。范围最懒且不越界。票 PASS。

#### 013 `/projects` 与三可空列

本席 B1 GR-M009-008 原判冲突：`ProjectRequest` 是 merchantId/venueType/floor/capacity，createProject 还强制 venueType，不像 PGM-020「建立综合体项目档案 / 记录项目状态、负责人、地址和经营范围」。

本批按 2.1 重走：PGM-020 是明确上位条目，缺字段是遗漏不是「HTML 禁止该表存在」。分类从冲突收窄为**细化（遗漏）**成立——这是分类修正，不是把「不是合格综合体档案」改写成「已经合格」。

下游身份（类型 4，不采信候选转述）：

- `analytics/JdbcAnalyticsRepository.java:72-78`：`SELECT p.id AS project_id … FROM projects p JOIN merchants m ON m.id = p.merchant_id`。**analytics:73 确按 projects.id 判项目。**
- `finance` 包内 0 处 `FROM projects`。`JdbcFinanceRepository.java:82-92` 是 AR/AP 写入 `project_id` 综合体维，不是 JOIN projects。候选「finance:92」不精确。
- 真正 `EXISTS(SELECT 1 FROM projects WHERE id = :id)` 在 `compliance/JdbcWecomComplianceRepository.java:92`。
- `complex_test` 当前 `projects` 行数 0，表仍被上述 SQL 当作项目身份用。

因此：**禁止**在 M009 B3 把 projects 改口迁到空间或换主键——会打下游。这也是本席收回「迁到空间」那句处置的原因。

V410 三可空列是不是 B3 可做的最小修复？**不是。**

1. `ProjectRecord.java` 不在 M009 二十文件，而在 **M015**（空间点位/楼层平面图，`modules.md` FILES 含该记录）。没有它，三字段无法进构造。
2. 新迁移文件属 M010 目录；M009 FILES 无迁移。
3. 页面归 M012。createProject 仍强制 venueType，三列补上后仍是场地形记录，并不实现 PGM-020「为商户、空间…提供归属」（现为 project.merchant_id，方向相反，属 018）。
4. 与 003 不同：003 的跨文件是白名单落地后**会打断已有签约写路径**的必要伴生；013 不改则现网继续缺字段，没有任何已有写入被本批其它 ID 打断。这是新建能力，不是解锁修复。
5. 018 已用「无实现面遗漏→否」口径。负责人/地址/经营范围在 M009 的 DTO/记录/库列上均为空实现面。

队列必须**否**，永久 ID 保留，口径并 018：全局遗漏，待持有 ProjectRecord/projects 表/页面的模块处理。禁止本批加 V410、改 M015 记录、造集团视图。候选「是（B2裁定）」使本席整批 FAIL。

#### 015 读端 platform scope

本席 B1 OBS-02 当时不升冲突，理由是未证明商户角色持有 view、V005 只把 view 种给平台管理员。本批不因合取改口，按 GEN-005 与写端对称独立重裁。

类型 1：GEN-005「受保护操作须在服务端按主体、对象和动作授权；默认拒绝，避免仅靠界面隐藏、调用方约定或可伪造字段」。BSM-072/074、PGM-027「商户不能查看平台全局和其他商户数据」。PGM-041「员工级完全自定义配置」「数据的项目与商户隔离不提供关闭开关」。

类型 2 源码：

- 写路径 `PlatformService` 各写方法首行 `requirePlatformScope()`，注释 L51-52 写明「防权限被下放给商户角色后越权治理」。
- `listMerchants` L132-154 **无**该守卫，只记审计。
- `PlatformController` GET `/merchants` 等只 `requirePermission`。
- `requirePlatformScope` L487-502：merchantId 非空即拒。
- `isExplicitOrDailyGrant`：来源角色只要不是把高危码从 `role_platform_admin` 自动生效，其它角色（含商户角色）GRANT `merchant.profile.view` 即通过。view 为 medium、requires_approval=false，属日常码。

「非 admin 前端 0 调用」正是 GEN-005 禁止的界面隐藏。写端已按同一威胁 fail-closed，读端不对称。分类细化（上位 BSM-072/PGM-027/041），缺陷是、队列是。最小：读端与写端同法 `requirePlatformScope()`。不改权限种子。票 PASS（升级观察，不是照抄席甲）。

#### 006 后半「创建商户不自动生成绑定」

PGM-026 原文三条分开：

1. 「建立商户档案、运营主体、负责人、第一联系人和第二联系人。」——建档，无绑定句。
2. 「把一个商户的“经营对象、运营主体、合同主体、结算主体”绑成一组唯一关系…都引用同一组绑定。」——绑定能力，未写「建档当下必须已绑」。
3. 「签约转商户自动带入…四类主体默认指向同一公司、后续可拆为不同公司。」——**签约转商户**，不是后台 createMerchant。

`LeasingService.autoCreateMerchantForSign` 已 createMerchant，随后 `createFourPartyBinding`（L298-309）落实第 3 条。后台建档不自动绑不与第 1/2 条矛盾。本席 B1 把「创建商户必写绑定」并进 GR-005 过宽。后半仅登记。前半幽灵绑定仍是、仍入队。票 PASS。

#### 003 跨文件与六类代码

BEQR-148/BSM-028/PGM-026 六类：自营、租赁、联营、代运营、服务商接入、其他。`self_operated/lease/joint_operation/managed_operation/service_provider/other` 是这六类的技术映射，不增加第七类（BSM-001 增删类须写入 HTML）。收费模板码冒充合作方式才是发明，白名单拒绝它们是删除冲突，不是发明。

`LeasingService.java:290` 经 `platformService.createMerchant` 写 `"leasing_auto"`。本模块一加白名单，签约建档即 400。把 `"leasing_auto"` 改为 `null`（现网 87 行 cooperation_form 全 NULL；签约阶段未定，不发明值）是白名单能落地的最小伴生，归因同一 ID 成立。`OrganizationMerchantPage.tsx:32` 三值同理，否则后台录入必失败。M012 测试夹具随页面改。e2e mock、AdmissionAudits 展示页不是本 ID 必改面。

`statusLabels.ts` 不在 M012 六文件；B3 可用页面内中文 option，不强制改 customer i18n。M011 夹具 `fixed_rent_plus_commission` / `operation` 归 003/005 最小测试，不另开 ID。票 PASS。

#### 008 商户三列与绑定表

PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」。生产 `findActiveBusinessEntityBindingByMerchantId` 仅 governance 三处；governance 外 0 生产消费方。唯一外部读是 `JdbcSettlementInvoiceRepository.java:114` `COALESCE(m.settlement_entity_id, m.business_entity_id)`。`changeBusinessEntityBinding` 不回写商户三列，分叉首轮就会发生。

最小：绑定创建/激活时把该商户三列写成绑定值；商户档案**更新**不再覆盖三列（与同条「财税主体不得由后台普通开关改动」同向）。不改结算侧、不 DROP 三列、不把结算改读绑定表。这是 M009 写侧投影，不是跨模块设计，**不改否**。票 PASS。

#### 025 spv/operation 与 2.2

GR-M009-011 原意：spv/operation 是添加，B2 先问。本批核实：

- CHECK 原文在 `V354__business_entity_master_data.sql:68`，该文件是 **M010** FILES。
- M009 二十文件无 `spv` 字面量；`'operation'` 出现在 **M011** 测试夹具，不在 20 文件。
- `complex_test`：`entity_role in ('spv','operation')` 行数 0；主体共 6 行。
- 005 白名单不含二者，M009 落地后不能再经本模块 API 写入。

添加内容的载体是 M010 的 CHECK，不是本 20 文件。2.2 只对本模块添加项发问。本批不写 questions、不 BLOCKED-ASK，转 M010 B1 按 2.2 处理，**合规**。本席不要求本批询问操作员，故不因 025 投 FAIL。若 M010 漏问，那是 M010 的 FAIL，不能用本批人闸提前。票 PASS。

#### 009 最高权限

BSM-004：「由具备最高权限的人审批通过后执行（申请人不能审批自己的申请）」；同条后文「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。BSM-007：「删除…不因平台管理员这个身份自动获得」；「最高权限…凭密钥…不再另设审批环节」。

现状：`approveDeleteMerchant` 控制器只 `requirePermission("merchant.profile.approve")`。`isMasterAuthority`（`AuthorizationService:139-142`）在权限判定 L80 已放行最高权限，故权限码拦不住「非最高权限但被单授 approve 的人」。自审闸 L301-304 对最高权限同样生效，本人发起仍须他人批，与「不需要经过任何审批」相反。

合并 CX-09+GR-010 到一 ID 成立。最小：approve 增加 `isMasterAuthority` 闸（控制器权限码可留：最高权限本已绕过码；无码的非最高权限仍拒）；`requestDelete` 发起人为最高权限时直接走软删执行（仍查占用、留痕）。不新造审批框架。同构四处归 019。票 PASS。

#### 011 存档即停票

BEQR-155：「商户退出不是立即生效，一律设缓冲期」。BSM-004 存档：「从各处的选择列表里隐藏…想恢复点取消存档即可」；逐条存档不用审批。存档≠退出。`PlatformService.java:208-212` 注释「存档=撤场」并立即 `expireBarSingleStoreTickets`，把可逆隐藏做成立即停票。

最小：删除该调用。专用五参构造里的 `TicketingService` 注入一并去掉，保留四参兼容构造，避免空依赖半成品。`TicketingService.expireBarSingleStoreTickets` 本体留在票务模块（注释已写供撤场用，真退出流程归 018/票务）。下单侧 TICKET_BAR_OFFLINE 不在本调用链，不受影响。不在本模块实现 30–60 天缓冲。票 PASS。

#### 017 complex_test 环境

本席本批只导出三个 `COMPLEX_DATABASE_*`，`psql -d complex_test`（类型 4）：

- `current_database()=complex_test`
- flyway 最新 version=409；400–409 共 10 行；399 有 1 行
- `merchants.is_self_operated` 列存在
- 正本迁移最大 V399（目录实列）

与 M009 二十文件无关。Jdbc 套件 1 红是环境污染。否队列。须操作员知悉（回滚到 V399 或另建干净库），本席不发 2.2。票 PASS。

### 最小修正意见（定稿前必须改候选，否则不能双 PASS）

1. **M009-013** 修复队列候选由「是（B2裁定）」改为 **否**。分类保持细化（遗漏）。永久 ID 不删。PGM-020 负责人/地址/经营范围与倒置归属一并按 018 全局遗漏登记。删除 V410/改 M015 `ProjectRecord`/本批造集团视图的处置句。
2. 其余 ID 按上表：队列是 = 001–012、014–016；队列否 = 013、017–025。添加项 0，questions 0。
3. 003 跨文件、008 写侧同步、015 读端守卫、025 不发 2.2，均保持。
4. 全批修复只针对正式定稿「是」的 ID 及可归因最小测试；不改 HTML/判据/权限种子。B3 仍为 0，直至本批双 PASS。

### 本席实跑（B2，只读）

- 锁文件 hashlib：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- 20 文件行和 2476，相对 HEAD diff 空
- parentEntityId / apps/web / 绑定消费方 / cooperation 写入点 / requirePlatformScope / isMasterAuthority / expireBar 调用 / V354 CHECK / projects 下游 SQL：仓库 grep+读源
- `psql` complex_test：flyway 409、V400–V409=10、is_self_operated=t、cooperation_form 87/87 NULL、spv|operation 行=0、projects 行=0、chk_be_entity_role 含 spv/operation；未对业务库提交
- `/tmp/m009-b1-probe` 已删除（本席 B1 临时探针，按指令清理）

### 结论

本席 B2-r1：**FAIL**

否决点仅 013 修复队列：候选把它写成 B3 可做的最小实现（V410 三列+跨 M010/M015 记录），本席裁定为超出本模块 20 文件的新建能力，必须改否。其余 24 个 ID 的分类/缺陷/队列本席接受。无添加项需本批 2.2。无 HTML 问题。无条件通过。改候选后须再跑 B2-r2，不能带着 013=是 进入 B3。

## M009 B2-r2 整批归并复核（Grok 席乙）
文件自身版本：V0.04（本段追加；上方 V0.01 历史前缀 + V0.02 B1 + V0.03 B2-r1 共 97095 bytes 原样保留，不改写第 386 行锁段起点，本段不写独占锁哈希字段行）

- 席位：Grok（固定外审席乙）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）；ledger V2.03 IN_PROGRESS；B3=0。
- 正本：`/home/james/complex-management` main `HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）。
- 本席 B2-r2：**PASS**
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。sidecar 全文一行同值；ledger 顶行同值。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未用席甲本批 B2-r1/B2-r2 追加段作证据（self-raw 在 31878 字节前缀之后的 V0.02/V0.03 追加只保留不采信）。
- 技能按 B2 收窄：using-superpowers / grilling（自问自答取证）/ verification-before-completion / receiving-code-review（先核源与候选差集再接受或反驳，禁止表演性同意）/ ponytail-review（最懒可行，不落地代码）。未冒称 batch-grill-me。未派 Arc audit 子代理。

此票只判断 findings.md 顶部 V1.18「当前 M009 B2-r2 整批候选」相对本次两份完整 B1 raw 的全批差集、2.1 四分类、工程缺陷与修复队列，并核对该候选是否准确落实本席 B2-r1（V0.03）否决意见、有无新的改写或遗漏。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.02 B1 **FAIL**、V0.03 B2-r1 **FAIL** 原文。席甲票仍是作者自审票；本席 FAIL 具否决权。本批不因席甲已改判而照抄其票。

### 本批输入（完整指定文件；本席独立实算）

| 项 | 版本/范围 | 字节 | SHA256 | 本席实算 |
|---|---|---:|---|---|
| 席甲 B1 | `docs/audit-2026-09/modules/M009-b1-self-raw.md` V0.01 前缀 | 31878 | `76f4af4b282b89831e27f6e32b003e0980a4a5b1f64d2e457cd6314a201da0a6` | 全等。其后追加不采信 |
| 本席 B1 | 本文件第 386 行独占锁行起的 V0.02 段；V0.02 全文 | 75079 | `37c0eca7809fa90d319c5dfd51c05cd4256c011c69c56ff620c5809f36c51cf4` | 全等。前 36869 bytes 历史只保留不采信 |
| 本席 B2-r1 | 本文件 V0.03 追加段；追加前全文 | 97095 | `9a647121e1256610b96f68052aa5770c2eb4f484af629ef18a03c7c5137aeda7` | 全等。本段追加前缀即此 |
| 候选台账 | `docs/audit-2026-09/findings.md` V1.18 顶部「当前 M009 B2-r2 整批候选」M009-001…025 | 462980 | `9f38b7e49c6a2dc16fb6eaf5c13cb284aba7e16e9e78d0314558cb9bbc2e33f0` | 全等。其下一节 r1 候选原文只作历史对照，不再是本批对象 |
| criteria-lock.md | V0.11 | 889020 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` | 与 sidecar 同行、ledger 顶行全等 |
| ledger.md | V2.03 只读顶部 | — | 顶行锁哈希同上 | `LOCK-AMEND-COUNT: 1`；M009 `IN_PROGRESS`；B3=0 |

冻结判据：映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 unique=87。20 文件行合计 2476，相对 HEAD 这 20 路径 `git diff --stat` 空。本文件独占锁行仍仅第 1 行与第 386 行两处，本段未新增。`questions/` 无 M009 文件。

### r1→r2 候选逐格差集（程序对照，不采信席甲转述）

对 V1.18 顶部 r2 表与其下保留的 r1 表 25 行七列逐格比较：有单元格变化的 ID **仅 003、013**。其余 23 行七列字节级相同。

| ID | 变化列 | r1 候选 | r2 候选 | 是否落实本席 r1 |
|---|---|---|---|---|
| 003 | 仅处置 | `COOPERATION_FORMS与statusLabels改为六类`；`B2请裁定是否接受跨文件最小改动` | `COOPERATION_FORMS改为六类并用页面内中文option（statusLabels.ts不在M012六文件，不强制改）`；`M011夹具中的fixed_rent_plus_commission/operation随003/005最小测试改`；`三处改动均归因本ID（B2-r1外审已接受）` | 是。与 r1「statusLabels.ts 不在 M012 六文件；B3 可用页面内中文 option，不强制改 customer i18n。M011 夹具归 003/005 最小测试」逐句对齐 |
| 013 | 工程缺陷、修复队列、事实、处置 | 缺陷「是」；队列「是（B2裁定）」；事实未写记录/迁移/页面归属，下游写成 `compliance:92与analytics:73`；处置为 V410 三列+改 ProjectRecord/ProjectRequest/Jdbc，并留「B2若判超出则改否」 | 缺陷「否（本模块无可修实现面）」；队列「否」；事实补 `JdbcWecomComplianceRepository:92`、`JdbcAnalyticsRepository:72–78`、`complex_test当前0行`、`ProjectRecord属M015、迁移属M010、页面属M012`；处置删除 V410/改 M015/迁到空间，改为 018 口径全局遗漏、永久 ID 保留 | 是。与 r1 最小修正意见第 1 条及 013 票「细化（遗漏）/否/否」全等 |

r2 顶部节无 `V410` 字面。`迁到空间` 只出现在 013 处置的**禁止**句，不是允许动作。队列集合 r2：是 = 001–012、014–016（15 项）；否 = 013、017–025。与 r1 要求第 2 条全等。

**r1 裁定未被误读。** 无把「013 否」读成删 ID、无把「003 不强制改 statusLabels」读成不改页面、无把「012 冲突但删除范围限 M009 面」扩成 DROP 列/改 V318/V354、无把「015 升级入队」改回观察、无把「008 不改否」改成转结算、无把「025 不发 2.2」改成本批 questions。

### 全量差集（两份完整 B1 → 001–025；本批重走，不抄席甲）

席甲 B1（只采信 31878 字节前缀）：CX-01…12 缺陷建议入队；OBS-01…11 登记；GEN-017 写无添加。本席 B1（只采信第 386 行起 V0.02）：GR-M009-001…010 缺陷建议入队；GR-M009-011 添加（先问）；OBS-01…11 登记。

| 本席 B1 | 席甲 B1 | 正式 ID | 差集 |
|---|---|---|---|
| GR-M009-002 冲突 | CX-01 细化 | 001 | 并入。分类取冲突 |
| （无独立 GR） | CX-02 | 002 | 并入。技术性 |
| GR-M009-003 冲突 | CX-03 细化 | 003 | 并入。分类取冲突。跨文件最小伴生见 003 票 |
| GR-M009-004 冲突 | CX-04（含 BEQR-154；候选来源列误写 CX-20，席甲 B1 无 CX-20，r1 已记不另开 ID） | 004 | 并入，无丢条 |
| GR-M009-009 | CX-05 技术性 | 005 | 并入。分类技术性：三角色有 PGM-018/019 上位 |
| GR-M009-005 | CX-06 细化 | 006 | 并入后拆半：存在性入队；建档即绑定仅登记 |
| （无独立 GR） | CX-07 | 007 | 并入 |
| （GR-005 双源一句） | CX-08 | 008 | 并入单列，不丢。写侧投影不改否 |
| GR-M009-010 | CX-09 冲突 | 009 | 并入同一 ID |
| GR-M009-006 | CX-10 | 010 | 并入 |
| GR-M009-007 | （席甲无对等 CX） | 011 | 并入，不丢停票项 |
| GR-M009-001 冲突 | OBS-06 技术性 | 012 | 并入。分类取冲突。删除范围限 M009 面 |
| GR-M009-008 | OBS-02 中 PGM-020 字段遗漏 | 013 | 并入。分类收窄为细化（遗漏）；本模块无实现面，队列否，并 018 口径 |
| （无独立 GR） | CX-11 | 014 | 并入 |
| OBS-02 观察 | CX-12 缺陷 | 015 | 并入。本席独立升为缺陷入队 |
| OBS-04 | （席甲无对等） | 016 | 并入 |
| OBS-11 | OBS-01 | 017 | 并入。环境，否 |
| （OBS-02 其余） | OBS-02 其余 | 018 | 并入。无实现面遗漏，否。013 三字段与倒置归属同此口径，不另开 ID |
| OBS-03 | OBS-03 | 019 | 并入跨模块登记 |
| （无独立 GR） | OBS-04 消费方 | 020 | 并入 |
| （无独立 GR） | OBS-08 comment | 021 | 并入 |
| OBS-01 | OBS-07 | 022 | 并入 |
| OBS-05/06/09/10 | OBS-05/09/10/11 | 023 | 并入登记 |
| OBS-07/08 | （席甲无对等） | 024 | 并入跨模块 |
| GR-M009-011 添加 | （席甲写无添加） | 025 | 并入。载体在 M010，本批不发 2.2 |

无第三条根因、无第 26 个 ID、无 LOCK-AMEND、无 HTML 修改申请、无本批 questions。两席 B1 临时编号均落在 001–025。

### 逐 ID 票（001–025）

分类按 2.1 顺序独立重裁。队列按提示词：冲突 / 有工程缺陷 / 答「删除」的添加 → 是；只登记 → 否。025 非本模块添加项，本批不发 2.2。

| ID | 分类 | 工程缺陷 | 修复队列 | 本席本批票 | 理由 |
|---|---|---|---|---|---|
| 001 | 冲突 | 是 | 是 | PASS | 与 r1 同。BSM-004「想恢复点取消存档」；19 参更新写出 archived_*/delete_*。最小：更新先读行、25 参保留。不改列表过滤 |
| 002 | 技术性 | 是 | 是 | PASS | 无 HTML 授权以 PUT 凭客户端 id 建档。与 001 同处 requireMerchant→404。无业务发明 |
| 003 | 冲突 | 是 | 是 | PASS | 见下「003 修订核实」。六类代码为既有中文口径的技术映射；跨文件三处归因同一 ID |
| 004 | 冲突 | 是 | 是 | PASS | BEQR-154/155 明文六态。归档走 archived_at、不作可设代码。不造状态机/缓冲期（018）。绑定三态保持。来源列「CX-20」仍为 r1 已接受的误写，不另开 ID |
| 005 | 技术性 | 是 | 是 | PASS | 白名单 platform/project_company/management_company 对应 PGM-018/019。非法值 400 中文。不发明 spv/operation（025） |
| 006 | 细化 | 是 | 是（仅存在性） | PASS | 前半幽灵绑定入队。后半建档即绑定：PGM-026 三条分开，后台 createMerchant 未要求当下已绑；仅登记。候选事实仍留「请Grok裁定」为 r1 原文残留，实质处置已是存在性校验，不视为未闭合分歧 |
| 007 | 细化 | 是 | 是 | PASS | 与 006 同法存在性。PGM-001/005 |
| 008 | 细化 | 是 | 是 | PASS | 写侧投影：绑定创建/激活回写商户三列，档案更新不覆盖。不改结算侧、不 DROP 列。**不改否**。候选处置仍留「B2若判跨模块则改否」为 r1 原文残留；本席独立裁定与 r1 同，不改否 |
| 009 | 冲突 | 是 | 是 | PASS | BSM-004/007：approve 加 isMasterAuthority 闸；最高权限发起直达软删。同构归 019 |
| 010 | 冲突 | 是 | 是 | PASS | 改四处注释；includeArchived 纳入 approved；恢复复用 unarchive。不真 DELETE。M011 内存 remove 归 M011 |
| 011 | 冲突 | 是 | 是 | PASS | 存档≠退出。删除 expireBar 调用及专用五参注入。不在本模块实现缓冲期 |
| 012 | 冲突 | 是 | 是 | PASS | BEQR-144/PGM-018 明文否定行政隶属链。从 M009 DTO/Record/Service/Jdbc 删除 parentEntityId 写入与映射。**不** DROP 列、**不**改 V318/V354、**不**新建协作表。候选分类列仍写「冲突（Grok）/技术性（本方，待B2）」为 r1 原文残留；本席独立裁冲突，队列是。表下说明已记「012冲突且删除范围限M009面」 |
| 013 | 细化（遗漏） | 否（本模块无可修实现面） | 否 | PASS | 见下「013 修订核实」。r1 否决点已闭合 |
| 014 | 技术性 | 是 | 是 | PASS | pageSize=50 与全量 items。改 PageResponse.of 或删 page()。无业务发明 |
| 015 | 细化 | 是 | 是 | PASS | GEN-005 默认拒绝；写端已 fail-closed，读端不对称。最小：读端 requirePlatformScope()。不改权限种子。候选缺陷列仍写「是（本方）/观察（Grok，待B2）」为 r1 原文残留；本席独立升为缺陷入队，与 r1 同 |
| 016 | 技术性 | 是 | 是 | PASS | GEN-001 删 requireDate / readObjectMap |
| 017 | 技术性（环境） | 否 | 否 | PASS | complex_test Flyway 409≠源 399、is_self_operated 列，非本 20 文件。须操作员知悉。不发 2.2 |
| 018 | 细化（遗漏，无实现面） | 否 | 否 | PASS | 缓冲期/已存档内容页/数据清理/商户无 project_id 倒置归属。013 三字段并此口径，不另开 ID。r2 的 018 行正文与 r1 相同，不要求把三字段抄进 018 单元格 |
| 019 | 技术性（跨模块） | 否 | 否 | PASS | 同构审批归各模块 B1 |
| 020 | 技术性（跨模块） | 否 | 否 | PASS | 无过滤 listMerchants 消费方归合同/计费。本模块保留两种方法 |
| 021 | 技术性 | 否 | 否 | PASS | 审批意见未落痕；留痕以 delete_reason 为准。仅登记 |
| 022 | 技术性 | 否 | 否 | PASS | 控制器漏检幂等键但执行器仍必填。行为一致，不改 |
| 023 | 技术性 | 否 | 否 | PASS | 切分缝/COUNT/姓名明文/读端审计/中文错误。不 LOCK-AMEND |
| 024 | 技术性（跨模块） | 否 | 否 | PASS | 守卫文案与 FieldLabels 归 access/common |
| 025 | 添加（内容在 M010） | 待 M010 | 否 | PASS | CHECK 在 V354（M010 FILES）。M009 二十文件无 spv 字面量。005 白名单不含二者。本批不写 questions、不 BLOCKED-ASK。若 M010 漏问是 M010 的 FAIL |

冲突 7（001/003/004/009/010/011/012）。添加 0（025 转 M010）。本批 questions 0。

### 003 修订核实（独立，不抄席甲）

本席 r1：「`statusLabels.ts` 不在 M012 六文件；B3 可用页面内中文 option，不强制改 customer i18n。M011 夹具 `fixed_rent_plus_commission` / `operation` 归 003/005 最小测试，不另开 ID。」

本批类型 4：

- `plan/modules.md` M012 FILES 六条：`GovernanceTopologyPage.tsx/.test.tsx`、`OrganizationMerchantPage.tsx/.test.tsx`、`SubjectBindingsPage.tsx/.test.tsx`。无 `statusLabels.ts`。
- `OrganizationMerchantPage.tsx:32` 仍为三个收费模板码；`:453` option 文案走 `formatBusinessStatus`（即 customer i18n）。故「页面内中文 option」是让白名单落地、又不强迫改 M012 以外文件的最小伴生。
- M011 三测试文件确有 `fixed_rent_plus_commission` 与 `"operation"` 夹具，随 003/005 最小测试改、不另开 ID，成立。
- 「三处改动」= 本模块白名单 + `LeasingService` `"leasing_auto"`→null + `OrganizationMerchantPage.COOPERATION_FORMS`（页面内中文 option）。M012 测试随页面改。e2e mock、AdmissionAudits 展示页仍不是本 ID 必改面（r1 原边界；r2 未把它们写入必改面，也未扩大必改面）。

分类冲突、缺陷是、队列是。票 PASS。无误读。

### 013 修订核实（独立，不抄席甲）

本席 r1 整批 FAIL 的唯一否决点：候选把 013 写成 B3 可做的 V410 三列+跨 M010/M015。裁定：细化（遗漏）、本模块无可修实现面、队列否、永久 ID 保留、并 018 口径；禁止本批加 V410、改 M015 `ProjectRecord`、迁到空间、造集团视图。

本批类型 4 重核归属，不采信候选转述：

- `ProjectRecord.java` 在 M015 FILES（`modules.md` 空间点位段），不在 M009 二十文件。现记录字段 `merchantId/venueType/name/code/floor/capacity/status`，无负责人/地址/经营范围。
- M009 FILES 无 `.sql`。V318/V354 属 M010。新迁移必落 M010 目录。
- 页面属 M012 六文件，无项目档案页。
- `ProjectRequest.java`（M009 DTO）同样无三字段；只改 DTO 而无记录/列/页面，不能实现 PGM-020，也不是「解锁修复」。
- 下游：`compliance/JdbcWecomComplianceRepository.java:92` `EXISTS(SELECT 1 FROM projects WHERE id = :id)`；`analytics/JdbcAnalyticsRepository.java:72-78` `FROM projects p JOIN merchants m`。r2 事实引用已纠正 r1 候选「compliance:92」的不精确写法。禁止把 projects 改口迁到空间。
- 与 003 不同：003 跨文件是白名单落地后会打断已有签约/录入的必要伴生；013 不改则现网继续缺字段，没有任何已有写入被本批其它 ID 打断。这是新建能力。

r2：分类细化（遗漏）、缺陷否、队列否、禁止加迁移/改 M015/迁到空间、永久 ID 保留、与 018 同口径登记全局遗漏。V410 已从 r2 节删除。票 PASS。r1 否决点闭合。无误读。

### 残留措辞（不构成新改写，不因此 FAIL）

012 分类列、015 缺陷列、006 事实「请Grok裁定」、008 处置「B2若判则改否」仍是 r1 候选原文。r2 未改这四行。表下说明已记录本席 r1 独立裁定（012 冲突且范围限 M009 面、015 升级入队、006 后半仅登记、008 写侧投影不改否、025 不发 2.2）。本席按实质队列/分类投票，不因残留「待B2」把已闭合项重新否决。定稿后这些残留不再表示未裁定。

### 最小修复边界（仅当本批双 PASS 后的 B3；本席本批不修）

队列是（15）：001–012、014–016。队列否（10）：013、017–025。B3 只修当时仍为「是」的 ID 及可归因最小行为测试。允许边界与 r1 同：

- 001/002 同处：更新先读行，25 参保留 archived_*/delete_*/createdAt；不存在 404。不改列表过滤。
- 003：本模块非空白名单六类代码；LeasingService `"leasing_auto"`→null；OrganizationMerchantPage 改为六类并用页面内中文 option。不强制改 `statusLabels.ts`。M011 夹具随 003/005 最小测试改。不发明第七类。
- 004：服务层白名单；归档不作可设代码；不造状态机。
- 005：三角色白名单；400 中文；不含 spv/operation。
- 006：绑定前四对象存在性。不自动在 createMerchant 写绑定。
- 007：项目/空间存在性。
- 008：绑定创建/激活回写商户三列；档案更新不覆盖三列。不改结算侧。
- 009：approve 的 isMasterAuthority 闸；最高权限发起直达软删。
- 010：注释 + includeArchived 纳入 approved + 复用 unarchive。不真 DELETE。
- 011：删除存档路径的 expireBar 调用及专用注入。
- 012：仅删 M009 面 parentEntityId。不动列/V318/V354，不新建协作表。
- 014：PageResponse.of 或删 page()。
- 015：读端 requirePlatformScope()。不改权限种子。
- 016：删两处死代码。

禁止：改三份业务 HTML；改 013/017–025；本批加迁移/改 M015 `ProjectRecord`/迁到空间/造集团视图；锁外新判据；本批 2.2；改权限种子。全部改动文件自身版本递增。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 hashlib / sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶行、本文件既有两处独占行同值）
- 输入四文件字节与 SHA：上表全等
- r1/r2 表程序逐格：仅 003、013 有单元格变化
- `plan/modules.md`：M009=20 无 sql 无 ProjectRecord；M010 含 V318/V354；M011=3 测试；M012=6 无 statusLabels.ts；M015 含 ProjectRecord.java
- 点读：ProjectRecord 十字段无三列；ProjectRequest 无三列；analytics:72–78；compliance:92；V354:68 CHECK 含 spv/operation；OrganizationMerchantPage:32/:453；M011 夹具 fixed_rent_plus_commission 与 operation
- 20 文件行和 2476；相对 HEAD diff 空
- 未跑：主仓测试、psql 写、部署、修复。未重跑 B1 探针（r1 已清 `/tmp/m009-b1-probe`）

工作区其它脏文件属既有父链，本模块 20 路径干净。本席未写 findings/ledger/锁/源/HTML。

### 结论

候选按本席 B2-r1 意见修订：013 队列改否并入 018 口径，删除 V410/改 M015/迁到空间等处置句；003 补注 statusLabels.ts 不强制改、页面内中文 option、M011 夹具随 003/005 最小测试改。其余 23 行七列与 r1 候选相同。全量差集无漏并、无新 ID、无新判据、无本批添加项。r1 任一裁定均未被误读。残留「待B2」字样不改变已裁定的队列/分类。

本席队列：**001–012、014–016 是（15 项）；013、017–025 否（10 项）**。

本席 B2-r2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M009 `IN_PROGRESS`，B3=0。不覆盖上方 B1 FAIL 与 B2-r1 FAIL 原文。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 Grok 独立审计原文（2026-09-14 操作员新授权全量复核）

- 席位：Grok 独立审计席乙
- 阶段：B1 修复前整模块全量审计（只审不修）
- 报告自身版本：V0.05（指定路径已有 V0.01–V0.04 历史段，本段追加；不改写此前任何历史段或旧结论）
- 日期：2026-09-14
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` FILES 20，计划冻结有效内容行 2476；本席对当前工作区 `git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` 实算 **2879**（文件已比计划切分时增长，以现值审计）
- 冻结判据：87 条（`criteria-lock.md` 映射行 `M009 → [BEQR-063 … PGM-066]`，本席自锁文件抽出 COUNT=87 unique=87 MISSING=[]）
- 正本：`/home/james/complex-management` main 主工作区
- 当前 HEAD（`git rev-parse HEAD`）：`c039798a8a89993ace0d03bad566665d37a1877e`（与派发一致）
- 控制：`160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；ledger 顶部基准 `CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`、`LOCK-AMEND-COUNT: 1`；模块当前状态 IN_PROGRESS，本轮 B3=0/3
- 结论：**FAIL**
- 未修源码、未改三 HTML、未改台账/锁/sidecar/findings/测试/配置、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调远程 MCP、未读本轮或旧次 Codex self-raw、未采信旧 PASS、未问操作员

本席互盲：未读取 Codex 本次/旧次 self raw、未读取旧 M009 raw/findings/结论正文作为依据、未读取另一套 fresh-first-principles 结果。只读 ledger 顶部基准、模块当前状态、已答复 questions（M010-002/M010-003/U04-001，答复首行均为「删除」）。具体事实来自当前代码、三份 HTML 锚点正文、complex_test 只读约束，以及可复现命令输出。

---

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用（不派子代理、不问第四道人闸、不写额外文档）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先于技能本体。bundled 路径不存在，已改读实际路径 |
| verification-before-completion | 同套件 `verification-before-completion/SKILL.md` | 先跑命令再声称；75/75 以 surefire XML 为准 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling；内部 sharpen |
| codebase-design | 同套件 `engineering/codebase-design/SKILL.md` | 接口/深度/删除测试；不派 DESIGN-IT-TWICE 子代理 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改生产代码 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环再下结论；本轮用 REPL 探针而非改仓 |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席：步骤+完成判据同文件 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到独立 SKILL.md
- `ubiquitous-language`：本机技能树未找到
- `using-superpowers` 的 grok bundled 路径不存在（已用 Claude 插件缓存实路径）
- Arc `audit` 会派评审子代理并另写产物：与双席/§7 冲突，**不调用**
- `ponytail-review` 存在于 `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`，B1 只审不修，未按该技能落地删除

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

graft CLI 存在，对 `JdbcPlatformRepository.java` 跑过一次 skeleton（仅作目录，正文仍逐行通读）。

---

## 1. 独算哈希与 HEAD / 20 文件全覆盖

### 1.1 锁哈希（本席独立执行）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`

输出：

```
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
```

- sidecar `plan/criteria-lock.sha256` 同值
- ledger 顶行同值
- 与派发期望锁哈希一致
- `LOCK-AMEND-COUNT: 1`

### 1.2 HEAD

```
c039798a8a89993ace0d03bad566665d37a1877e
```

与派发一致。审当前工作区，不审其他分支或历史提交。

### 1.3 二十文件 SHA256（本席独立 `sha256sum`，与调度清单逐项比对）

| 路径 | 行数 | 本席 SHA256 | 与调度清单 |
|---|---:|---|---|
| `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java` | 17 | `45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java` | 19 | `f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` | 90 | `66a840d9b71935b15328b08b4daea61867851084dc27634025e45bd1cb12ace2` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java` | 9 | `e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | 1167 | `8ce7f0af00668768c0430a58158481444cfd45d2f358e6500b8bd99dd78527c4` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | 277 | `9b476721e1768d39ca8043fe556cfb26b2917595bb32af1ddaa275e571e525c3` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java` | 145 | `42e2e86a36bdd7dbd8975ef5f9b21082b5242a3fab52a1dbe8b7af7091b371cd` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 991 | `9ca85b1b07c4432e3f3000c1e1343b2b19f5cd0109e8dfd92855634384febcf3` | 一致 |
| `backend/src/main/java/com/complex/platform/governance/package-info.java` | 1 | `e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9` | 一致 |
| `.../dto/BusinessEntityBindingRequest.java` | 17 | `66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da` | 一致 |
| `.../dto/BusinessEntityRequest.java` | 17 | `8c4b7795909a6dc597fab7937105d6b54979d9ad900a8e8d45d688fe1cbb725e` | 一致 |
| `.../dto/MerchantArchiveRequest.java` | 6 | `e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93` | 一致 |
| `.../dto/MerchantDeleteApprovalRequest.java` | 5 | `2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4` | 一致 |
| `.../dto/MerchantDeleteRequest.java` | 5 | `4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45` | 一致 |
| `.../dto/MerchantProfileRequest.java` | 27 | `cbcf242470823799c3d2fcbe996de4665e4d440614a4263a2635ccb4439bea99` | 一致 |
| `.../dto/ProjectRequest.java` | 17 | `8316db69c16d3013dbfc07ead14673321a49d05c3131163ae58597543004c76e` | 一致 |
| `.../dto/SpaceRequest.java` | 21 | `63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa` | 一致 |
| `.../dto/SpaceTypeRequest.java` | 15 | `d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2` | 一致 |
| `.../dto/StatusChangeRequest.java` | 11 | `75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f` | 一致 |
| `.../dto/VenueBusinessHoursRequest.java` | 22 | `6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1` | 一致 |

二十文件 SHA 全等，无漏文件、无额外文件纳入本模块。

### 1.4 逐文件覆盖（全文，非抽样）

| 文件 | 覆盖行 | 读法 |
|---|---|---|
| package-info.java | 1–1 | 逐字 |
| BusinessEntityRecord.java | 1–17 | 逐字 |
| BusinessEntityBindingRecord.java | 1–19 | 逐字 |
| MerchantDeleteDecision.java | 1–9 | 逐字 |
| MerchantRecord.java | 1–90 | 逐字（含 19 参兼容构造、withArchive/withMaskedPhones/withEntities/withDelete） |
| 全部 11 个 dto | 各文件全长 | 逐字 |
| PlatformRepository.java | 1–145 | 逐字 |
| PlatformController.java | 1–277 | 逐字（含存档删除链、空间种类审批、营业时间、脱敏） |
| PlatformService.java | 1–991 | 逐字（含白名单、存档删除、绑定同步、空间种类、executeWrite） |
| JdbcPlatformRepository.java | 1–1167 | 逐字；**审批链部分 758–1167 全覆盖**（supervisor_review_decisions / org_hierarchy / approval_lines / supervisor_review_nodes / holiday_calendar / setMerchantArchiveReason） |

未只读骨架、未只看旧修改。调用方与 SQL 约束仅为本模块溯源（complex_test 的 CHECK/唯一索引、AuthorizationService.requirePlatformScope/isMasterAuthority、FieldLabels 出口翻译、PageResponse.of、IdempotencyService.requireIdempotencyKey、VenueBusinessHoursService.list 形状）。

---

## 2. 已答复 questions（只采信答复段）

- `questions/M010-002.md`：`## 操作员答复` 首个非空行 **删除**（`chk_be_entity_role` 去掉 `spv`）。属 M010 迁移；本模块服务层白名单已是三值。
- `questions/M010-003.md`：首行 **删除**（去掉 `operation`）。
- `questions/U04-001.md`：首行 **删除**（自营不得做独立于六类合作方式的布尔）。本模块 `MerchantRecord` 无 `selfOperated` 字段；complex_test `merchants` 无 `is_self_operated` 列。

---

## 3. 三线结果与三问

### 3.1 第一性原理

**问1：为什么必须存在？删了哪条 HTML 做不到？**

- 商户档案 / 合作方式六类 / 业态六值 / 四类主体绑定 / 存档与审批删除 / 综合体项目与空间点位 / 主体三角色：删掉则 BSM-028、BSM-004、BEQR-143/148/154/155、PGM-018/020/026 做不到。
- `JdbcPlatformRepository` 后半审批链：删掉则 BEQR-166「系统按组织层级自动补全」的持久化做不到（实现面在本文件，业务发起面多在合同模块）。
- `listMerchantsByBusinessEntity`：注释写明门禁以「该表存在 scoped 查询」为隔离能力；删了会让 tenant-isolation 门禁 fail-closed。技术性，指向 PGM-027/BSM-072 商户不能看平台全局。
- `VenueBusinessHoursRequest` + Controller 两个端点：场地营业时间是场次划分前提；DTO 在本模块，服务实现在 M015 切片。

**问2：声称的行为有没有被实际执行验证？**

有。命令与输出见 §6。单元/Web/JDBC 三套 75/75 绿**不能**代替缺陷证据：同一套 in-memory 夹具 `seedMerchant` 把不存在的 `be_1` 写成 `businessEntityId` 仍绿。REPL 探针（§6.3）单独打红 GR-01/02/03/05。

**问3：哪条前提未验证就当真？**

- 「商户三个主体列只会来自生效绑定」——创建路径直接写请求里的 ID，停用绑定后也不清投影（GR-01/02）。
- 「空间必有综合体项目归属」——`createSpace` 在 `projectId==null` 时跳过存在性检查（GR-03）。
- 「`POST /projects` 就是综合体项目档案」——DTO 没有负责人/地址/经营范围，却必填 `venueType`（GR-04）。
- 「删除审批留痕事件名等于结论」——驳回仍记 `merchant.delete_approved`（GR-05）。
- 「测试绿 = 行为对」——本模块明确禁止该推理（GEN-003）。

### 3.2 顺从式

假定实现意图正确，逐条核 87 项：见 §4。通过项是「本模块实现与 HTML 一致，或本模块无该实现面且未发明相反规则」。失败项绑定 GR-01…05。HTML 沉默不作为删除依据。

### 3.3 对抗式

假定实现错误，构造最小反例（grilling 收窄：事实自取）：

1. 创建商户时塞三个不存在的主体 ID → 成功落库（GR-01）。
2. 先绑定再 `action=inactive` → 商户三列仍指向旧主体（GR-02）。
3. `createSpace` 不传 `projectId` → 空间 `projectId=null`（GR-03）。
4. 删除申请后最高权限 `rejected` → 审计事件仍是 `merchant.delete_approved`（GR-05）。
5. 缺幂等键：日志 `idempotency_key is required`，但 `FieldLabels.translate` 把响应当成「请勿重复提交，请刷新后再试」——**用户面中文，不构成缺陷**。
6. 并发双活绑定：服务层 `ACTIVE_BINDING_EXISTS` + 库 `uq_business_entity_bindings_active_merchant`；`DuplicateKeyException` 映射 409 中文。第一次正常运行会被拦住，不另开缺陷。
7. 空间种类 `DELETE`：服务恒 400「删除须先存档…」；审批在 service 内 `isMasterAuthority`。Controller 审批端点无独立权限码，但 HTML 要求的是最高权限而非另增权限点——不升缺陷。
8. `actorId()==null` 跳过自审闸：无鉴权/测试路径，第一次正常登录不会撞上 → 备注。

不为两三个不巧同时发生的极端情景新增闸/状态/计数器。

---

## 4. 87 判据逐项核查

符号：PASS＝本模块无冲突且无未验证的关键声称；FAIL＝绑定下方 finding；N/A-PASS＝本模块无该实现面、未发明相反业务规则（HTML 沉默≠应删）。

### 4.1 BEQR（21）

| ID | 结果 | 要点 |
|---|---|---|
| BEQR-063 | N/A-PASS | 沽清/反结不在本模块 |
| BEQR-074 | N/A-PASS | 隐私桌台不在本模块 |
| BEQR-143 | **FAIL** | GR-01/GR-02：不是「同一经营对象下唯一绑定」的单一来源 |
| BEQR-144 | PASS | 三角色白名单；无行政隶属链写入；complex_test `business_entities` 无 `parent_entity_id` |
| BEQR-145 | PASS | `project_company` 可建；职责正文无相反编码 |
| BEQR-146 | PASS | `management_company` 可建；域职责不在本切片结算 |
| BEQR-147 | N/A-PASS | 收入归属计算不在本模块 |
| BEQR-148 | PASS | `MERCHANT_COOPERATION_FORMS` 六类；空＝尚未确定；选「其他」的收费模板在合同生效闸，不在本 DTO |
| BEQR-149 | N/A-PASS | 无职级 |
| BEQR-150 | N/A-PASS | 无十一端开关 |
| BEQR-151 | N/A-PASS | 无最终权限计算 |
| BEQR-152 | PASS | 治理读端 `requirePlatformScope`；营业时间按项目 scope 过滤（调用方） |
| BEQR-153 | N/A-PASS | 无终端开关优先级 |
| BEQR-154 | PASS | `PROJECT_STATUSES` 六态与原文一致 |
| BEQR-155 | PASS | 可设五态 + 存档走 `archived_at`（BSM-004 存档操作，不作第六个可 set 的 status 码） |
| BEQR-156 | N/A-PASS | 无合同状态机 |
| BEQR-157 | N/A-PASS | 无员工状态 |
| BEQR-158 | N/A-PASS | 无设备状态 |
| BEQR-159 | N/A-PASS | 无权限状态 |
| BEQR-166 | PASS | 本文件实现链节点/组织上级/平行线；OBS：`org_hierarchy` 主键 `user_id`，一人一个直接上级，平行线走不同人时仍可补全 |
| BEQR-189 | N/A-PASS | 无服务点单管理费 |

### 4.2 BSM（14）

| ID | 结果 | 要点 |
|---|---|---|
| BSM-001 | PASS | 基础盘子 API 存在；多综合体预留缺口见 GR-03/04，不把整条整体说明判无 |
| BSM-003 | PASS | 父锚点导语 |
| BSM-004 | **FAIL** | 存档/先存档再删/占用检查/最高权限直删/软删行保留均有；**驳回删除的审计事件名说成已批准**（GR-05） |
| BSM-005 | N/A-PASS | 分步引导是网页；本模块提供写入 API |
| BSM-006 | N/A-PASS | 无动态码 |
| BSM-007 | PASS | `isMasterAuthority` 与平台管理员分离；删除审批拒非最高权限 |
| BSM-009 | PASS | 基础管理入口能力由本模块后端承载一部分 |
| BSM-028 | **FAIL** | 六类合作/多对多角色有；综合体档案字段与空间归属见 GR-03/04 |
| BSM-029 | PASS | 无相反连接 |
| BSM-072 | PASS | 业态六值 `MERCHANT_BUSINESS_FORMATS`；读端平台 scope |
| BSM-074 | PASS | 同 072 |
| BSM-114 | N/A-PASS | 入口表无本模块 UI |
| BSM-115 | PASS | 无相反 |
| BSM-120 | **FAIL** | 「保证所有对象带综合体项目归属」被 GR-03 打穿；其余已拍板无相反实现 |

### 4.3 GEN（18）

| ID | 结果 | 要点 |
|---|---|---|
| GEN-001 | PASS | 方法均有可验证用途；scoped 列表为隔离门禁 |
| GEN-002 | **FAIL** | GR-01/02/03 主路径行为与声称不一致 |
| GEN-003 | **FAIL** | 75/75 绿；但 `seedMerchant` 把幽灵 `be_1` 当合法创建，锁不住 GR-01。测试通过不代替行为证据 |
| GEN-004 | **FAIL** | 创建商户三主体 ID 未做存在性/所有权校验 |
| GEN-005 | PASS | 写权限码 + 服务层 `requirePlatformScope`；空间种类删除审批走最高权限 |
| GEN-006 | **FAIL** | 绑定非唯一来源、停用后投影残留、空间可空归属 |
| GEN-007 | PASS | 幂等键；活绑定部分唯一索引；重复键 409 |
| GEN-008 | PASS | 六类/业态/三角色/空间二态与库 CHECK（complex_test）同词表 |
| GEN-009 | PASS | 业务拒绝中文 400；空间 status 已前置，不再把 V203 CHECK 丢成 500 |
| GEN-010 | PASS | 联系人手机写加密、读解密、无查看权则掩码 |
| GEN-011 | **FAIL** | GR-05 |
| GEN-012 | PASS | 列表 SQL 分页 size≤500 |
| GEN-013 | PASS | `mvn -o` 可构建 |
| GEN-014 | PASS | 本模块无迁移文件；complex_test `chk_be_entity_role` 已是三值，与服务层一致（M010 已答复删除后的库态） |
| GEN-015 | N/A-PASS | 无界面文件 |
| GEN-016 | N/A-PASS | 无文档文件 |
| GEN-017 | PASS | 四分类已依序做 |
| GEN-018 | PASS | 结论落到 HTML/命令输出 |

### 4.4 PGM（34）

| ID | 结果 | 要点 |
|---|---|---|
| PGM-001 | PASS | 底座定位；报错出口翻译中文 |
| PGM-002 | PASS | 无相反治理顺序 |
| PGM-003…016 | N/A-PASS | 模块入口卡片；本切片无相反实现 |
| PGM-017 | **FAIL** | GR-03：空间可不带综合体项目归属 |
| PGM-018 | PASS | 三角色 `platform/project_company/management_company` |
| PGM-019 | PASS | 职责表无相反编码 |
| PGM-020 | **FAIL** | GR-04：不能记录负责人/地址/经营范围 |
| PGM-021 | PASS | 业务意义无相反 |
| PGM-026 | **FAIL** | GR-01/02 |
| PGM-027 | PASS | 商户不能看平台全局：读端+写端平台 scope |
| PGM-031 | N/A-PASS | 经销商治理不在本写路径 |
| PGM-035 | N/A-PASS | 无提成发放 |
| PGM-040 | PASS | 单一功能权限码存在 |
| PGM-041 | PASS | 项目隔离靠平台 scope；敏感手机分权 |
| PGM-042 | N/A-PASS | 无职级配置 |
| PGM-043 | N/A-PASS | 无外包/审计人员实现 |
| PGM-044 | N/A-PASS | 无隐私桌台实现 |
| PGM-048 | PASS | 商户侧边界靠 scope，不在本模块开放商户门户 |
| PGM-060 | PASS | 本模块只处理商户联系人手机，不做会员导入 |
| PGM-065 | PASS | 连接表无相反 |
| PGM-066 | **FAIL** | 「保证所有对象带综合体项目归属」被 GR-03 打穿；「不阻塞单综合体运营」不授权无归属对象 |

---

## 5. 临时 findings（依序：冲突 → 细化 → 技术性 → 添加）

### 5.1 冲突

#### GR-01 — 创建商户可写入不存在的三个主体 ID，且不创建绑定行

- 严重度：高
- 分类：**冲突**
- 文件:行号：`PlatformService.java:850-901`（`saveMerchant` 创建分支 878–880）；`MerchantProfileRequest.java:12-14`；对照绑定校验 `PlatformService.java:671-676`
- 冻结判据：PGM-026、BEQR-143、GEN-004、GEN-006
- 依据类型：HTML 原文 + 可复现命令输出
- HTML 逐字（PGM-026 / 治理地图 L492–498）：
  > 把一个商户的“经营对象、运营主体、合同主体、结算主体”绑成一组唯一关系（四类主体可分属不同公司）；合同、结算、权限、看板、第三方接入都引用同一组绑定。
- 另 BEQR-143：`四类主体……同一经营对象下唯一绑定`
- 实际行为（REPL，exit 0）：

```
=== GR-01 createMerchant with ghost businessEntityId ===
created.id=mch_cb34fa8f-57f0-4c1d-a427-3dfd1f6e2921
created.businessEntityId=be_does_not_exist
created.contractEntityId=be_contract_ghost
created.settlementEntityId=be_settlement_ghost
repo.businessEntityExists(be_does_not_exist)=false
```

- 第一次正常运行就会撞上：招商/后台创建商户表单带三个主体字段（请求体合法）即可写入幽灵 ID；后续合同/结算若读商户列会指向不存在主体。更新路径已改为不覆盖三列，创建路径仍是第二写入源。

#### GR-02 — 停用四类主体绑定后，商户三列投影仍指向旧主体

- 严重度：高
- 分类：**冲突**
- 文件:行号：`PlatformService.java:747-749`（仅 `active` 时 `syncMerchantEntities`）；`831-836`
- 冻结判据：PGM-026、BEQR-143、GEN-006
- 依据类型：HTML 原文 + 可复现命令输出
- HTML：同上「都引用同一组绑定」
- 实际行为：

```
afterBind.businessEntityId=be_op
afterBind.contractEntityId=be_ct
afterBind.settlementEntityId=be_st
afterInactive.businessEntityId=be_op
afterInactive.contractEntityId=be_ct
afterInactive.settlementEntityId=be_st
```

- 第一次正常运行：运营把绑定停用（合同换主体前的常规动作）后，读商户档案仍看到旧运营/合同/结算主体。库有活绑定部分唯一索引，但投影未随停用失效。

#### GR-03 — 创建空间允许 `project_id` 为空，对象可以不带综合体项目归属

- 严重度：高
- 分类：**冲突**
- 文件:行号：`PlatformService.java:595-598`（仅当 `projectId != null` 才查项目）；`JdbcPlatformRepository` 写入不强制；complex_test `spaces.project_id` `is_nullable=YES`、无 FK
- 冻结判据：PGM-017、PGM-066、PGM-020、GEN-006
- 依据类型：HTML 原文 + 可复现命令输出 + SQL
- HTML（PGM-017 L412）：
  > 当前先按一个综合体落地，但所有基础对象都要带“综合体项目归属”。
- HTML（PGM-066 L953）：
  > 已拍板：未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属。
- 实际行为：

```
=== GR-03 createSpace with null projectId ===
space.id=sp_da6d0345-d62f-4173-a3c9-85a6194d4262 projectId=null (null=true)
```

- SQL（complex_test）：`spaces.project_id` nullable；当前存量 0 行空值，但 API 可新写出空值。PGM-066「不阻塞单综合体运营」指集团视图，不授权无归属对象。

### 5.2 细化

#### GR-04 — `POST /projects` 不能记录综合体项目档案的负责人、地址、经营范围，却必填场地类型

- 严重度：中
- 分类：**细化**（上位：PGM-020/BSM-028「建立综合体项目档案」）
- 文件:行号：`dto/ProjectRequest.java:5-16`；`PlatformService.java:376-409`（`venueType` `requireText`，无负责人/地址/经营范围）
- 冻结判据：PGM-020、BSM-028、GEN-002
- 依据类型：HTML 原文 + 可复现命令输出（读 DTO 全文）
- HTML（PGM-020 L440–444）：
  > 建立综合体项目档案。
  > 记录项目状态、负责人、地址和经营范围。
  > 为商户、空间、合同、员工、设备、票务和看板提供归属。
- 命令 `python3 -c "print(Path('.../ProjectRequest.java').read_text())"` 全文即：`idempotencyKey, operatorNote, evidenceAttachmentIds, merchantId, venueType, name, code, floor, capacity, status`。无负责人、地址、经营范围；`venueType` 为必填（服务层 `requireText(..., "venue_type")`）。JDBC 夹具曾用 `venueType="complex"` 表示综合体，与「场地类型」混在同一列。第一次建项目档案就会发现 HTML 要求的三件事写不进去。

#### GR-05 — 删除审批驳回仍写入成功事件 `merchant.delete_approved`

- 严重度：中
- 分类：**细化**（上位：BSM-004「删除留痕」）
- 文件:行号：`PlatformService.java:298-306`（`executeWrite` 的 successEvent 固定 `"merchant.delete_approved"`）；驳回分支 327–330
- 冻结判据：BSM-004、GEN-011
- 依据类型：HTML 原文 + 可复现命令输出
- HTML（BSM-004）：
  > 删除留痕：谁在什么时候、以什么理由删了什么，都有记录可查。
- 实际行为：

```
decision=rejected deleted=false
auditEvents=...merchant.delete_requested,merchant.delete_approved,
```

- 第一次正常驳回就会在审计里留下「已批准删除」事件名，和结论相反。

### 5.3 技术性（无业务发明 + 无 HTML 冲突；逐条理由）

| 编号 | 内容 | 无业务发明的理由 | 工程缺陷 |
|---|---|---|---|
| T-01 | `listMerchantsByBusinessEntity` | 行级隔离通道，门禁认「表上有 scoped 查询」；不引入新业务概念 | 无。零调用是注释声明的常态 |
| T-02 | `JdbcPlatformRepository` 实现 `SupervisorReviewRepository` 及审批链 JDBC | 把已有审批模型落到表，无新角色/新状态发明 | 无阻断。OBS：`findDirectSuperior` 不按 `line_code` 过滤，一人一个上级，平行线用不同人时仍可走 |
| T-03 | 联系人手机 `PiiCipher` + `withMaskedPhones` | 加密/展示脱敏，服务技术，权限码已有 | 无 |
| T-04 | 列表 `COUNT+LIMIT` 真分页，size 封顶 500 | 资源边界 | 无。`listSpaceTypes`/`venue-business-hours` 用 `PageResponse.of` 切内存，当前字典 8 条、场地×7 远小于 500 |
| T-05 | 空间种类 `DELETE` 恒 400、走存档+审批 | 落实 BSM-004，不是新业务 | 无。Controller 审批端点无独立权限码，但 HTML 闸是最高权限，服务层已 `isMasterAuthority` |
| T-06 | `package-info.java` 仅 package 声明 | 编译单元 | 无 |

技术性项无工程缺陷，只登记。

### 5.4 添加

无。未发现本模块引入三份 HTML 没有、又不能指向上位条目的业务发明。`venueType/floor/capacity` 归入 GR-04 对 PGM-020 的细化（错把综合体档案做成场地行），不另走 2.2。

B1 不发问、不修复。

### 5.5 备注（非缺陷）

- 缺幂等键日志是英文 `idempotency_key is required`，HTTP 正文经 `FieldLabels.translate` 为「请勿重复提交，请刷新后再试」。用户面中文。
- `actorId()==null` 跳过自审：无鉴权上下文，第一次正常登录不会撞。
- 商户/主体行无 `project_id` 列：与 GR-03（空间可空）不同；PGM-066 允许单综合体运营，列缺失记备注，不另开闸。
- 本席曾用环境 URL 连到库名 `complex`（非测试库）只读了一眼约束；**业务行不采信**。正式约束与测试以 `complex_test` 为准（角色 CHECK 三值、活绑定部分唯一索引、空间 `project_id` 可空）。
- in-memory 测试种子八个空间种类，与字典一致；不把测试绿当 HTML 通过。

---

## 6. 实际命令与退出码

### 6.1 锁与文件哈希

`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → 见 §1.1，进程成功（exit 0）。

`sha256sum` 二十文件 → 见 §1.3，exit 0。

`git rev-parse HEAD` → `c039798a8a89993ace0d03bad566665d37a1877e`，exit 0。

### 6.2 测试（complex_test，只导出三个 `COMPLEX_DATABASE_*`，不 source 整份 env，不打印秘密）

```
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
mvn -o -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests test
```

- 把 URL 库名改写为 `complex_test` 后启动（测试类 JDBC 也硬编码 `jdbc:postgresql://127.0.0.1:5432/complex_test`）。
- surefire XML：
  - `PlatformServiceTests`：tests=46 failures=0 errors=0 skipped=0
  - `PlatformControllerTests`：tests=12 failures=0 errors=0 skipped=0
  - `PlatformJdbcIntegrationTests`：tests=17 failures=0 errors=0 skipped=0
- 合计 **75/75**。日志含 `BUILD SUCCESS`。对应退出码 **0**。
- Controller 日志摘录（非秘密）：`raw_message=idempotency_key is required`（随后经 FieldLabels 翻译，见备注）。

75/75 **不是**模块 PASS 证据。

### 6.3 REPL 探针（stdin 编译到 `/tmp`，用毕删除，仓库内无探针文件）

用与 `PlatformServiceTests` 相同的 in-memory 装配跑创建/停用/空归属/驳回删除。`javac` exit 0，`java` exit 0。输出见 GR-01/02/03/05。

### 6.4 complex_test 只读 SQL（三个变量，库名 `complex_test`）

`psql` exit 0。要点：

- `chk_be_entity_role` = `platform | project_company | management_company`（无 spv/operation）
- `uq_business_entity_bindings_active_merchant` 存在
- `spaces.project_id` nullable，当前空值行 0
- `merchants` 无 `project_id`/`complex_id`/`is_self_operated`
- `org_hierarchy` 主键 `user_id`
- `space_type_dictionary` status CHECK 含 `active/disabled/archived/pending_delete`

未对 `complex`（非 test）业务行作证据。

---

## 7. 单席结论

**FAIL**

有工程缺陷（GR-01…GR-05）。不存在有条件通过。B1 只审不修，不进入修复、不发 2.2 问询。建议 B2 归并时把 GR-01/02/03 作为冲突或带缺陷的细化入修复队列；GR-04/05 为细化缺陷入队；技术性 T-01…T-06 只登记；添加项 0。

本席未改任何业务或审计控制文件（本文件为指定追加目标）。

## M009 B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.06（本段追加；上方 V0.05 B1 前缀 146230 字节原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙。步骤：M009 新授权轮 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main。派发钉住 HEAD `c039798a8a89993ace0d03bad566665d37a1877e`（本席 `git merge-base --is-ancestor` 确认其为当前 `HEAD` `e456ba96c84185c8d39f18f548011daac48a6466` 的祖先；`c039798..HEAD` 对 M009 20 文件 `git log` 为空，20 文件 SHA 与 B1 清单全等）。工作区另有未提交审计稿。未 push/merge。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）；runbook V0.02（SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`）；criteria-lock V0.11；modules.md V0.04 M009 FILES 20。
- 锁实算（本席本步 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`）：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。四值全等来自本席独算与只读比对，**不是**派发提供的期望锁哈希（见下「锁哈希用语澄清」）。
- ledger 只读：V3.53，M009 顶部「操作员已批准013…恢复B2」，模块 IN_PROGRESS，本轮 B3=0/3；原 BLOCKED-ASK 段保留为历史。未改 ledger/findings/HTML/源码/测试/配置。
- 本席 B2：**PASS**（20 行全部覆盖并给票。添加 0、不发 2.2。非阻断定稿措辞见各 ID，不否决。）
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。除本指定路径外未写文件。未写源代码或永久探针。未 source 整份 env、未打印凭证。

此票只判断 findings.md V2.03 顶部「当前 M009：2026-09-14 新授权轮 B2 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL** 原文。不把本票写成「台账已按本席意见改过」。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 / 范围 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M009-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.05 | 146230 | `ffc57c87438442c0afceeed9848721abf2a75b5655cbd81a17673a8ed0b2daf9` |
| `docs/audit-2026-09/modules/M009-b1-self-raw.md` 最后独占锁行至 B1 结论 | V0.04 B1 | （锁行后至「B1结论」段） | findings 记载全文 `103af2a09d98fa4fc5f3d080a450459d3b1ebfd256f6cea97fde12bf0d0e1f7f`（B1 落盘时） |
| 同上文件全文（本席读取时） | V0.04 B1 + 席甲已追加 B2 V0.05 | 92474 | `779ba9dc45137ed6d0fd053ff37d013930c61c170b7f397c09b58adbb8f795a8` |
| `docs/audit-2026-09/findings.md` 顶部 | V2.03 | 812025 | `e8a5fb671e16e5a58cfe274007cbd551e09de56665575731f8d74e46a9357706` |
| `docs/audit-2026-09/ledger.md` | V3.53 | 628379 | `56ee0114f63a9e1f650661f8c4249e767f2c64f367dc8028b3c7c9245aa687e8` |

席甲 self-raw 在 B1 结论后已追加「M009 新授权轮 B2 第1次 Codex复核票」V0.05（结论 FAIL，20 行事实票与五项待定稿修正）。B2 允许共享输入与互核，本席已读取该追加段；**B1 finding 集合仍只取两份 B1 正文**，不把席甲 B2 意见当新证据。

三 HTML 只读（与 B1 同）：治理地图 V0.12 SHA `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`；业务地图 V0.14 SHA `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；枚举 V0.07 SHA `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`。

操作员获批范围（回复原文「同意上述具体范围」）：现有项目档案后端与数据库增加负责人 `ownerPerson`、地址 `address`、经营范围 `businessScope`；三项暂时可空，兼容旧数据与旧调用，不自动补历史值；包含必要的新建/读取及验证；不含页面接入或旧项目补录入口；不改 HTML，不改变权限/合同/结算规则。此授权不是 B2 双 PASS，也不是无边界开发。

### 锁哈希用语澄清（不改写 V0.05 B1 原文）

本席 B1 §1.1 写「与派发期望锁哈希一致」。B1 派发 `-p` **没有**提供期望锁哈希，只要求本席独算。本步重申：四值全等的依据是本席对 `criteria-lock.md` 的真实 `sha256sum`、sidecar 内容、ledger 顶行、以及 B1 独占锁行同为 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。不得把那句读成「对照了派发期望值」。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并，不跑 Arc audit（会派子代理并另写产物） |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止沿用本席 B1 的 PGM-041/中文出口 PASS |
| grilling | 只借追问结构自查：038 守卫是否真隔离、047 翻译层是否透传、030 是否要自动建绑定、031/040/041 能否只收窄入口、013 是否夹带页面 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/`graft` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：本批最小是收窄已有入口/补已有字段校验，不新造审批/调度 |
| ponytail-review | 只评最小删除/收窄，不落地 |
| writing-for-agents | 本 raw 结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → **未用** |
| tdd / ponytail | B2 禁止修复，未落测试、未改源 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（CX-01…10、12、13；GR-01…05；T-01…06）

漏并：无。误合：无。被改写原意：无（T-01/02/03/05 候选由技术性改为细化，属分类顺序纠正，Grok B1 原文保留）。CX-11「黑名单须联合审批」B1 已排除，候选未纳入，正确。旧 001–012、014–017、019–029、032–037 最新队列值保持否，未把历史已修/登记当成新票。021 审批意见由 043 覆盖，不重复入队。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M009-038 | CX-01（本席 B1 未发现，B2 核源成立） | 冲突/是/是 | **PASS（冲突/是/是；最小处置须收窄到已有项目标识对象，见下）** |
| M009-039 | CX-02 + GR-03 | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-040 | CX-03（本席 B1 未单列审批缺口，B2 核源成立） | 冲突/是/是 | **PASS（冲突/是/是；首次绑定≠变更，见与 031/041 合取）** |
| M009-041 | CX-04 未来生效 | 冲突/是/是 | **PASS（冲突/是/是；日期校验落在 PlatformService 入口即覆盖签约调用链）** |
| M009-031 | CX-04 停用 + GR-02；沿用旧 ID | 冲突/是/是 | **PASS（冲突/是/是；旧观察重新入队证据足够）** |
| M009-042 | CX-05 | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-043 | CX-06 + GR-05；覆盖旧 021 | 冲突/是/是 | **PASS（冲突/是/是；最小处置已含对象/理由/决定，不只改事件名）** |
| M009-044 | CX-07 | 细化/是/是 | **PASS（细化/是/是）** |
| M009-045 | CX-08 | 细化/是/是 | **PASS（细化/是/是）** |
| M009-046 | CX-09 | 细化/是/是 | **PASS（细化/是/是）** |
| M009-030 | CX-10 + GR-01；沿用旧 ID | 候选「冲突/细化待复核」 | **PASS（统一为细化/是/是；非法引用拦截，不强制初建自动绑）** |
| M009-047 | CX-12（本席 B1 出口中文 PASS 被推翻） | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-018 | CX-13；沿用旧 ID | 冲突/是/是 | **PASS（冲突/是/是；旧遗漏改为收窄现网立即终止入口，证据足够）** |
| M009-013 | GR-04；沿用旧 ID | 细化/是/是 | **PASS（细化/是/是；获批范围与候选一致，铁律6停止已解除）** |
| M009-048 | T-01 | 细化/否/否 | **PASS（细化/否/否；有 HTML 上位，分类顺序纠正成立）** |
| M009-049 | T-02 | 细化/否/否 | **PASS（细化/否/否）** |
| M009-050 | T-03 | 细化/否/否 | **PASS（细化/否/否）** |
| M009-051 | T-04 | 技术性/否/否 | **PASS（技术性/否/否）** |
| M009-052 | T-05 | 细化/否/否 | **PASS（细化/否/否；恢复缺陷在 042）** |
| M009-053 | T-06 + package-info | 技术性/否/否 | **PASS（技术性/否/否）** |

冲突入队 9（038、039、040、041、031、042、043、047、018）。细化入队 5（044、045、046、030、013）。技术性入队 0。观察/登记否 6（048–053）。添加 0。questions 0。无 HTML 修改申请。

修复队列「是」14：「否」6。旧 ID 复用仅 013/018/030/031，与历史同根；038–053 为新号，未占用旧号。

### 038 独立裁定（冲突／是／是）——推翻本席 B1 PGM-041 PASS

本席 B1 把 `requirePlatformScope` 当成项目隔离。B2 按对方探针与源码重裁，**不以旧 PASS 为依据**。

核源：

- `AuthorizationService.requirePlatformScope:481-497`：平台管理员桥接放行；外包拒绝；`merchantId` 非空拒绝。**不读** `organizationId`，不按项目过滤。
- `currentProjectScopeQuietly:503-506` / `requireProjectScope:288-293`：项目 ID＝`actor.organizationId()`，无则 403。本模块 `listProjects`/`listSpaces`/`listMerchants` **零调用**。
- `PlatformController:58-67,125-147`：功能权限 + `requirePlatformScope` 后全表 list。
- `PlatformService.listProjects:362-363`、`listSpaces:421-422`、`listMerchants:140-147`：无项目谓词。
- `JdbcPlatformRepository.listProjectsPaged:476-488` `SELECT … FROM projects` 无 WHERE；`listSpacesPaged:492-505` 无 `project_id` 谓词；`listMerchantsPaged:455-472` 无项目列。
- 席甲 B1 探针：`PROBE_SCOPE=employeeProject:prj_…;RETURNED_PROJECTS=[prj_…, prj_…]`（两项目）。与源码一致，第一次正常项目员工带功能权限即撞上。
- `merchants` / `business_entities` 查询列无 `project_id`（本席 B1 备注仍成立）。PGM-041 原文：「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权……数据的项目与商户隔离不提供关闭开关」。

四分类：**冲突**（HTML 明文隔离 vs 现网全表）。工程缺陷：**是**。队列：**是**。

最小处置（收窄候选措辞，供定稿，不表示台账已改）：复用已有 `organizationId` / `requireProjectScope` 语义——**仅**对已有项目标识的对象：`listProjects` 按 `projects.id`，`listSpaces`/`createSpace` 按 `spaces.project_id` 及父空间所属项目；`platform_admin` 保持现网全量；不改 `requirePlatformScope` 对商户账号/外包的拒绝（那是 PGM-027 / 已修 M009-015）。**不**给商户/经营主体表新增 `project_id`，**不**新增可关闭的数据范围开关。商户表跨项目残留因无列可滤，本项不发明归属规则。

票：**PASS（是）**。

### 047 独立裁定（冲突／是／是）——推翻本席 B1「报错出口均中文」

本席 B1 只核缺幂等键：`idempotency_key is required` → `FieldLabels.translate` →「请勿重复提交，请刷新后再试」。该点仍对，**不能**证明全部出网 message 已中文。

核源：

- `FieldLabels.translate:511-536`：已含汉字则原样透传（`:535-536`）。
- `PlatformService.requireBusinessEntityRole:816-817` 等把中文闭集与英文码写在同一句：`主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)`。业态/合作方式/状态同构（`:143-145`、`:763-764`、`:773-775`、`:784-797`、`:806-807`）。
- `GlobalExceptionHandler:61` 对响应体 `FieldLabels.translate`。
- 席甲探针 `PROBE_API_MESSAGE=` 即为上述透传句。PGM-001：「展示给用户的一切文字……必须全部为中文，不得出现任何英文（如 active/draft 英文状态）」；内部标识符四样可留英文，**用户 message 不在四样之列**。

四分类：**冲突**。缺陷：**是**。队列：**是**。最小处置：删除对用户 message 中的英文括注，保留错误码；复用句内已有中文。不改 FieldLabels 透传策略、不把日志 `raw_message` 当展示面。

票：**PASS（是）**。

### 030 独立裁定（细化／是／是）——统一分类，禁止自动建绑定门槛

共同事实：`saveMerchant` 创建分支 `PlatformService:878-880` 把请求里的三主体 ID `trimToNull` 直写，不走 `requireBusinessEntity:823-828`（绑定路径已用）。席甲 `ENTITY=nonexistent`、本席 GR-01 幽灵 ID 探针同根。`JdbcSettlementInvoiceRepository:114` `COALESCE(m.settlement_entity_id,m.business_entity_id)` 证明投影有下游效果。更新路径已不覆盖三列（M009-008）。

HTML：PGM-026「绑成一组唯一关系……都引用同一组绑定」；BEQR-143「同一经营对象下唯一绑定」。招商「签约转商户……四类主体默认指向同一公司」在 `LeasingService:281-311`：先 `createMerchant` 再 `createFourPartyBinding`，**不是** `PlatformService.createMerchant` 内自动插绑定。

旧 M009-030 曾为观察/队列否（B2-r2「建档即绑定仅登记」）。本轮两席可执行幽灵引用 + 下游读取，重新入队证据足够。正式 ID 沿用 030，不新开号。

四分类按顺序：有 PGM-026 上位 → **细化**（不是无来源添加；也不是要把「每次建档必须自动建绑定」写成新准入）。工程缺陷：**是**（已提供的 ID 未做存在性校验）。队列：**是**。

最小处置：创建路径对**已提供**的三 ID 复用现有 `requireBusinessEntity`；未提供则保持可空。**不**把所有初建商户强制自动建绑定，**不**把 Leasing 签约链的默认同一公司前移进本模块建档入口。

票：**PASS（细化/是/是）**。候选表「冲突（Grok）/细化（Codex）；待B2复核」应以本裁定替换；本席不改 findings。

### 031 / 040 / 041 合取（三 ID 均冲突／是／是）

同一 HTML（PGM-026）：普通运营不得改收款/结算/收入归属方；调整只对生效时间之后有效；不追溯改历史结算快照。财税并行审核是既有规则，本批**不新造**审批系统或调度。

核源：

- `createBusinessEntityBinding:683-698`：无论 `effectiveFrom` 是否未来，`status=active` 并 `syncMerchantEntities`。席甲 `PROBE_FUTURE=2027-09-14;STATUS=active;CURRENT_ENTITY=be_future`。
- `changeBusinessEntityBinding:747-749`：仅 `active` 时同步；停用写 `effectiveTo=今天` 且不校验相对 `effectiveFrom`。席甲停用后投影仍为旧主体，且出现 `effectiveTo < effectiveFrom`。本席 GR-02 同。
- 普通停用后再建一组 active 会再次 sync，即 CX-03 替换路径。无财务/法务/公司权责入口。
- `LeasingService.createFourPartyBinding:300-311` 把合同 `effectiveFrom` 传入**同一** `createBusinessEntityBinding`。日期/激活校验必须落在 PlatformService，不必改 Leasing 文件、也不把 M009 扩到招商模块。

最小处置（收窄现有入口，三 ID 分症状保留）：

- **040**：禁止普通停用/暂停/再绑/替换使**当前**三主体投影变化。直接清空投影也是变更，不能当「修 031」的普通副作用。`createBusinessEntityBinding` 在商户尚无当前主体时的**首次**绑定不是「变更」，040 不关掉首次建立。不把最高权限 U 盘冒充财税并审，不新造审批链。
- **041**：`effectiveFrom > 今天` 不得 `status=active`、不得 `syncMerchantEntities`。不新增 pending 状态、不新增调度。未来日期若只作记录，用现有非 active 状态 + 日期字段保存且不投影。该校验在服务入口，人工与签约调用一并生效。
- **031**：停用不得写出 `effectiveTo < effectiveFrom`。投影与真正生效中的绑定一致；若普通停用会导致当前财税主体变化，走 040 拒绝而不是停用后偷偷清空。不改历史结算快照。旧 031 观察因本轮停用探针 + 倒置日期重新入队，证据足够。

票：三 ID 均 **PASS（是）**。

### 013 独立裁定（细化／是／是）——获批范围与候选一致

HTML PGM-020：「建立综合体项目档案。记录项目状态、负责人、地址和经营范围。」`ProjectRequest.java:5-16`、`ProjectRecord.java:5-16`（该文件在 M015 清单，不在 M009 20 文件）、`saveProject` SQL 十列、`createProject:396-407` 均无三字段；`venueType` 反而必填。删除/收窄/改一个已有字段不能补齐。分类：**细化**（明确 HTML 需求缺失，不是 2.2 添加）。

操作员已批准后端/数据库三字段、可空、必要新建/读取/验证。**不含**页面、旧项目补录、HTML、权限/合同/结算规则。候选 013 行与该边界一致。表内「须先解决铁律6」在批准后应为历史措辞，队列值仍是「是」；本席不改表。

B3 可执行面（获批「必要」内，非悄悄扩页）：`ProjectRequest` / `PlatformService` 新建与现有读取映射 / `JdbcPlatformRepository` SQL / 同包 `ProjectRecord` / `projects` 表新迁移。JSON 缺字段→null 兼容旧 HTTP；仓内位置构造须随记录组件补上。**禁止** UPDATE 猜填历史行、禁止 M012 页面、禁止补录入口。

旧 013 曾为全局遗漏/队列否；本轮字段仍缺且已获新增授权，重新入队证据足够。

票：**PASS（是）**。

### 其余入队 ID（核源后均 PASS）

- **039**：`createSpace:595-606` 仅当 `projectId!=null` 查项目；父空间只查存在。探针 `PROBE_PROJECT_REQUIRED=null`、`PROBE_PARENT_CROSS_PROJECT=true`。PGM-017/066 要项目归属。最小：`projectId` 必填 + 父同项目。不发明集团架构。
- **042**：删除/最高权限直删置 `disabled`（`:528-532`、`:567`）；`unarchiveSpaceType:503-504` 只接受 `archived`。BSM-004「可恢复」。复用取消存档入口接受已软删 `disabled`，保留占用检查，不新造恢复流程。
- **043**：`executeWrite:924` `Map.of()`；驳回仍用 successEvent `merchant.delete_approved`（`:303`、`:327-330`）。空间类型驳回事件名同为 `space_type.delete_approved`（`:571-573`），同根，不另开 ID。最小处置已写对象 ID、理由、实际决定、businessId，**不能**只改事件名。旧 021 意见字段由本项覆盖，021 保持否。
- **044**：`saveSpaceTypeDeleteRequest:385-394` 恒 INSERT `pending`；批准只改种类行。上位 BSM-004 留痕。更新已有申请终态，不新建状态机；故障注入不扩修。
- **045**：`countMerchantReferences:196-226` 对含 `merchant_id` 的基表无条件 COUNT。真实库探针失效绑定返回 1。只收窄已失效主体绑定，其它合同/资金/权益占用保留。
- **046**：容量/面积/租金无下限；探针 -1。拒绝负数；可空/零保留；不发明上限或租金规则。
- **018**：`requireSubjectStatus` 接受 `terminated`；`updateMerchant` 经 `saveMerchant` 立即写状态。探针 `PROBE_IMMEDIATE_EXIT=terminated`。BEQR-155「商户退出不是立即生效，一律设缓冲期」。收窄普通档案更新/创建的立即终止通道；不把存档当退出；不实施完整 30–60 天缓冲调度；不把暂停/黑名单升成联合审批（CX-11 仍不成立）。旧 018 由「无实现面登记」改为收窄现网入口，本轮反例足够。

### 登记项 048–053

有 HTML 上位的 T-01/02/03/05 按顺序归细化，无独立工程缺陷，队列否。T-04 分页、T-06 `package-info` 技术性、无业务发明、队列否。`listMerchantsByBusinessEntity` 零调用保留为隔离通道，不因零调用删除。不另开缺陷。

### 整批结论

**PASS**

20 行全部给票。不存在有条件通过。本票不授权 B3、不修改台账。席甲 self-raw 同批 B2 现为 FAIL（待把 030 分类、038/031/040/041/013/043 最小边界写入定稿后再票）；合取须双 PASS。本席已给出可执行统一值，供定稿采用，**未**自称 findings/ledger 已改。

未改业务 HTML、代码、测试、配置或台账；未 push/merge；未派子代理。


## M009 第二批B2整批归并复核（Grok 乙席）

- 文件自身版本：V0.07（本段追加；上方 V0.01–V0.06 前缀原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙。步骤：M009 第二批 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main。当前 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`（本席 `git rev-parse`）。相对 B1 钉住的 `c039798a8a89993ace0d03bad566665d37a1877e`，`git diff --stat` 对 M009 20 文件与三份业务 HTML 为空；推进仅另外三份 M046 认证测试。20 文件 SHA 与 B1 清单全等（本席本步重算）。工作区另有未提交审计稿。未 commit/push/merge/部署。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）；runbook V0.02（SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`）；冻结 471 模块计划 `plan/modules.md` V0.04 M009 FILES 20；criteria-lock V0.11。锁文件实算 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`，sidecar 内容同值，ledger 顶行同值，`LOCK-AMEND-COUNT: 1`。本段不写独占锁哈希字段行。
- ledger 只读：V3.56，M009 IN_PROGRESS，新轮 B3=0/3；013/038 授权已解决，当前进入第二批 B2。未改 ledger/findings/HTML/源码/测试/配置。
- 本席 B2：**FAIL**（20 行全部覆盖并给票。添加 0、不发 2.2。038 现网最高权限识别与项目 scope 不能按候选原文承载归属补录；041 未来改 inactive 后创建路径失去唯一绑定。任一即整批 FAIL。无条件通过。）
- 无 MCP、无子代理、无第三席、不问操作员。除本指定路径外未写文件。未写源代码或永久探针。未 source 整份环境、未打印凭证。未跑会改库的新审计文件。

此票只判断 findings.md V2.06 顶部「当前M009第二批B2完整候选」（唯一当前表，20 行）相对本次两份完整 B1 raw、第一批双方 B2 意见、以及操作员已批准的 013/038 范围。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL**、不覆盖 V0.06 第一批 B2 **PASS**。第一批本席 PASS 不自动覆盖本次新方案。席甲票仍是作者自审票；本席 FAIL 具否决权。

操作员授权（已实际收到，不再等待）：013 回复原文「同意上述具体范围」；038 在普通对话回复「批准」。后续不再弹窗。本批无待答复问题、无 2.2 添加项、无 HTML 修改申请。授权解除铁律 6 停止，**不等于**工程缺口已闭合。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 / 范围 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/findings.md` | V2.06 全文；顶部 20 行是唯一当前表 | 830686 | `24c96cdb46ccf686356ed8ac6d62a93a53a851d1c34a551bad5c4662f473d551` |
| `docs/audit-2026-09/ledger.md` | V3.56 | （本席 `sha256sum`） | `ccaf1a996f814e63e8d7f2790f7e93a671ede8de3cc2f5cd5e268fc1df3dccc3` |
| `docs/audit-2026-09/modules/M009-b1-grok-raw.md` 前缀（本席 B1） | V0.05 | 146230 | `ffc57c87438442c0afceeed9848721abf2a75b5655cbd81a17673a8ed0b2daf9` |
| 同上文件本段追加前全文 | V0.05 B1 + V0.06 第一批 B2 PASS | 166965 | `b53dc0cf008823b261111180d044759cc54c0bc3c89801fda9a89de67125259b` |
| `docs/audit-2026-09/modules/M009-b1-self-raw.md` | V0.04 本次 B1 + V0.05/V0.06 第一批 B2 FAIL | 95100 | `f3dace6ad1401d110c9b412cf2a0ea0036a203152a8e1948c44115deb409af6d` |
| 席甲 B1 落盘时全文（ledger/findings 记载，不替代现值） | V0.04 B1 | — | `103af2a09d98fa4fc5f3d080a450459d3b1ebfd256f6cea97fde12bf0d0e1f7f` |

B1 finding 集合只取两份 B1 正文（席甲 CX-01…10、12、13，CX-11 已排除；本席 GR-01…05、T-01…06）。第一批 B2 双方意见作归并输入：本席 V0.06 PASS（038 当时只收窄项目/空间、留下商户全表）已被本批新方案取代，**本席撤回该 038 处置，不沿用**。席甲 V0.05/V0.06 FAIL 要求补商户项目归属与公司多对多，操作员已批；本席核的是批准后的 V2.06 执行边界能否落地，不是再问一次授权。

三 HTML 只读（与 B1 同）：治理地图 V0.12 SHA `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`；业务地图 V0.14 SHA `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；枚举 V0.07 SHA `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`。

后文 findings 中的原候选/暂停说明一律当历史；不以旧待答复状态当当前。旧 001–037 除 013/018/030/031 外保持否；021 由 043 覆盖，不重复入队。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定本席只做第二批 B2 归并，不跑 Arc audit（会派子代理并另写产物） |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止沿用本席 V0.06「038 只修项目/空间」PASS |
| grilling | 只借追问结构自查：USB 身份是否等于 isMasterAuthority、scope 是否把 platform-company 当项目、未来 inactive 是否丢掉唯一、招商是否先写三主体 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/`graft` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design / ponytail-review | 只评最小删除/收窄，不落地 |
| writing-for-agents | 本 raw 结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → **未用** |
| tdd / ponytail | B2 禁止修复，未落测试、未改源 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 只读查询符号位置；未因提示另写图谱/产物 |

### 完整差集（CX-01…10、12、13；GR-01…05；T-01…06）

漏并：无。误合：无。被改写原意：无（T-01/02/03/05 由技术性改为细化，属 2.1 分类顺序纠正，Grok B1 原文保留）。CX-11「黑名单须联合审批」B1 已排除，候选未纳入，正确。030 分类已统一为细化。六登记 048–053 均在表内。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M009-038 | CX-01（本席 B1 未发现，B2 核源成立） | 冲突/是/是 | **FAIL（冲突/是/是成立；授权范围方向对，但现网最高权限识别与项目 scope 不能承载补录/跨项目关联，见下）** |
| M009-039 | CX-02 + GR-03 | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-040 | CX-03 | 冲突/是/是 | **PASS（冲突/是/是；与 031/041 合取，首次空主体可建、已有非空身份普通入口拒绝、不清空）** |
| M009-041 | CX-04 未来生效 | 冲突/是/是 | **FAIL（冲突/是/是与招商先写三主体的修法方向对；未来改 inactive 后创建路径未保持唯一未结束绑定，见下）** |
| M009-031 | CX-04 停用 + GR-02；沿用旧 ID | 冲突/是/是 | **PASS（冲突/是/是；普通停用生效绑定拒绝、投影不变、不倒置日期、不重开已结束历史期间）** |
| M009-042 | CX-05 | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-043 | CX-06 + GR-05；覆盖旧 021 | 冲突/是/是 | **PASS（冲突/是/是；对象 ID、理由、决定、驳回事件名，不能只改事件名）** |
| M009-044 | CX-07 | 细化/是/是 | **PASS（细化/是/是；本次申请终态与类型行同事务，最高权限直删也结清）** |
| M009-045 | CX-08 | 细化/是/是 | **PASS（细化/是/是；不得把 future inactive 且 effectiveTo=null 当已失效）** |
| M009-046 | CX-09 | 细化/是/是 | **PASS（细化/是/是）** |
| M009-030 | CX-10 + GR-01；沿用旧 ID | 细化/是/是 | **PASS（细化/是/是；只校验已提供 ID，缺省可空，不强制建档自动绑）** |
| M009-047 | CX-12 | 冲突/是/是 | **PASS（冲突/是/是）** |
| M009-018 | CX-13；沿用旧 ID | 冲突/是/是 | **PASS（冲突/是/是；收窄立即 terminated，不搞黑名单联合审批）** |
| M009-013 | GR-04；沿用旧 ID | 细化/是/是 | **PASS（细化/是/是；旧 Java 构造与 JSON 缺字段兼容）** |
| M009-048 | T-01 | 细化/否/否 | **PASS（细化/否/否；038 仍须给该查询加项目谓词，见 038 覆盖面，不另开缺陷）** |
| M009-049 | T-02 | 细化/否/否 | **PASS（细化/否/否）** |
| M009-050 | T-03 | 细化/否/否 | **PASS（细化/否/否）** |
| M009-051 | T-04 | 技术性/否/否 | **PASS（技术性/否/否）** |
| M009-052 | T-05 | 细化/否/否 | **PASS（细化/否/否；恢复缺陷在 042）** |
| M009-053 | T-06 + package-info | 技术性/否/否 | **PASS（技术性/否/否）** |

冲突入队 9、细化入队 5、登记否 6、添加 0 的**归并结构**成立。整批因 038、041 两处执行边界缺口 **FAIL**，不得定稿进 B3。

---

### 阻断 1：M009-038 授权方向对，现网权限不能按候选原文承载

四分类 **冲突**、工程缺陷 **是**、队列 **是** 仍成立。本席 V0.06 把处置收窄成「只修已有 project 标识的项目/空间、platform_admin 保持全量、商户表不新增 project_id」——与 PGM-041 / BEQR-152「所有业务数据默认按项目隔离」及 PGM-018「一个运营公司可以服务多个项目」矛盾，**本批作废该处置**。操作员已批准补 `merchants.project_id`、公司—项目多对多、后端读写隔离及必要维护；普通 platform_admin 不自动全量；历史不猜填。这一方向与 HTML 和授权一致。

核源（现网，未改）：

- `AuthorizationService.isMasterAuthority:139-143`：`isPlatformAdminBridge`（`accountType==platform_admin`）**并且** `usr_master_authority` **并且** `master-authority`。
- `SessionIssuer:11-16`：生产 U 盘账号类型是 `master_authority`，「不是平台管理员，不得当日常全权通配」；「不得按平台管理员身份签发」。
- `ProductionAuthService.issueMasterSession:373-414`：upsert `account_type=master_authority`，`organization_id='platform-company'`，`merchant_id=NULL`；签发的 `AccessActor` 同此四元组。
- `AuthorizationService.permissionGrant:80-83`：`isMasterAuthority(actor)` 才通配已知功能权限。生产 U 盘因账号类型不是 `platform_admin`，**通配不触发**。
- `PlatformService.isMasterAuthority:839-847` 只转发上述实现。`PlatformServiceTests:119-124` 用 `platform_admin` + 主账号 id/名 + `organizationId=org_1` 冒充最高权限，与生产签发相反。
- `currentProjectScopeQuietly:499-506` / `requireProjectScope:288-293`：项目 ID＝`actor.organizationId()`，空白才 403；**红线④：不因平台管理员返回空（全量）**。U 盘会话会得到字符串 `platform-company`，它不是 `projects` 表里的综合体。
- 列表仍全表：`listMerchantsPaged:455-472`、`listBusinessEntitiesPaged:437-451`、`listBusinessEntityBindingsPaged:509-515`、`listProjectsPaged:476-488`、`listSpacesPaged:492-505` 均无项目谓词。`listMerchantsByBusinessEntity:230+` 按经营主体全表反查。席甲探针 `PROBE_SCOPE` 两项目仍成立。

候选缺口（不是再要授权，是候选文字还不能执行）：

1. **现网 `isMasterAuthority` 认不出生产 U 盘会话。** 候选写「复用现有经授权 U 盘登录的 isMasterAuthority」——按当前实现，真插盘过不了该闸，也过不了 `requirePermission` 通配。测试夹具用 `platform_admin` 会造出「测试绿、生产 U 盘 403」。BSM-007 明文：平台管理员与最高权限是两个角色。
2. **`currentProjectScopeQuietly` 会把 `platform-company` / 测试 `org_1` 当成综合体项目。** 候选写「普通请求显式 projectId 只能与当前 scope 相同且项目存在」。若同一规则套在 U 盘补录上：目标真实项目 ≠ `platform-company` → 被拒；写成相等 → `projectExists('platform-company')` 失败。结果是最高权限无法把无归属商户补到真实项目，也无法给公司关联真实项目。
3. **认领绕过。** 候选「只补原来 null」若被读成普通岗位可把 `project_id` 从 null 写成当前项目，就是认领。权限边界段禁止认领，但后端维护段没写死「null→项目仅最高权限」。现网特权路径又不可用时，B3 极易用普通补 null 当唯一能跑的修法。
4. **覆盖面。** `listMerchantsByBusinessEntity` 若仍按公司全表反查，048 登记否不能当隔离已完成。COUNT 必须先过滤，候选已写，须落到该查询与绑定/公司 EXISTS。

HTML 内最高权限机制**可以**承载「逐笔归属配置、不新角色/权限码/审批链、不让 platform_admin 全量」：BSM-007 密钥本人不再另设审批；治理地图红线④允许持 U 盘对「看不到别的项目的数据」逐笔开放并留痕，不是关隔离开关，也不是集团汇总。本次授权含必要维护接口、不含页面/集团汇总/新审批。缺的是把**生产 U 盘三件套**写成闸，而不是调用一个要求 `platform_admin` 的函数。

**不得**为通过而：新增角色/权限码/审批链；让 platform_admin 或 USB 列表自动全量；把 `platform-company` 写入 `merchants.project_id`；让普通岗位认领。

需修正文字（替换 038「权限边界」整段，并补执行闸；其余数据承载/新建/列表过滤/Leasing 传 `intent.projectId` 可保留）：

> 权限边界：普通列表（含 platform_admin）一律取**真实综合体项目** scope：`currentProjectScopeQuietly` 的返回值必须能在 `projects` 表命中；命不中（含 `platform-company`、空、非项目组织 ID）则列表/COUNT 为空集，不得退回全表。普通新建商户/公司关联当前项目：项目取该真实 scope，请求里的 projectId 只能与之相同。普通更新禁止改 `project_id`（含把 null 写成当前项目、含改到其它项目）。  
> 显式补录无归属商户、给公司增加指定已存在项目的关联：仅识别生产 U 盘最高权限——`id=usr_master_authority` 且 `username=master-authority` 且 `account_type=master_authority`（与 `SessionIssuer` / `ProductionAuthService.issueMasterSession` 一致）。目标项目取请求中已存在的 projectId，只校验项目存在、对象当前无归属或按规则追加公司多对多、幂等键、功能权限/审计留痕；**禁止**要求该目标等于 currentProjectScopeQuietly()。不得把 organizationId 原样写入 project_id。  
> 现网 `AuthorizationService.isMasterAuthority` 因要求 `platform_admin` 不能识别生产 U 盘，本 ID 的必要依赖包括：把该识别改到与 SessionIssuer 三件套一致（或 PlatformService/Controller 与 requirePermission 通配同步改到同一识别），B3 归因 038，不新权限码、不改 HTML、不扩大待审模块清单。测试夹具禁止再用 platform_admin 冒充最高权限。USB 例外只用于本次归属配置，不能用于 040 财税主体，也不给任何列表自动全量。

最低可执行修法：① AuthorizationService 识别与 SessionIssuer 对齐（必要依赖，同 LeasingService）；② 普通 `updateMerchant` 拒绝任何 `projectId` 变更；③ USB 补录走显式已存在 projectId；④ SQL COUNT/分页/`listMerchantsByBusinessEntity`/公司 EXISTS/绑定经 merchant.project_id 先过滤；⑤ Leasing 建档传入已校验 `intent.projectId`，旧调用缺字段从真实当前项目取。

---

### 阻断 2：M009-041 / 合取唯一性——未来改 inactive 后创建可并排预约

040/031/045/Leasing 先写三主体 的**方向**成立，且覆盖本席 V0.06「只改 PlatformService 日期、不动 Leasing 前一步」的漏洞。

核源：

- `LeasingService.autoCreateMerchantForSign:281-292` 先把三个 `contractSubjectId` 写入 `createMerchant`，再 `createFourPartyBinding:300-311` 调 `createBusinessEntityBinding`。候选把这一处三列改 null、保留随后权威绑定与日期，避免未来合同在第一步就出现当前主体——必须测真实调用顺序，只改该点/项目传递，不改其它招商逻辑。此项 **PASS 方向**。
- `createBusinessEntityBinding:683-698`：无论 `effectiveFrom` 是否未来，均 `status=active` 并 `syncMerchantEntities`。席甲 `PROBE_FUTURE=2027-09-14;STATUS=active`。
- 唯一性只认 active：`uq_business_entity_bindings_active_merchant`（V010:15-18）`WHERE status='active'`；`findActiveBusinessEntityBindingByMerchantId:551-560` 同样只查 active；创建/激活冲突也只问 active（`:677-682`、`:725-731`）。

候选：未来绑定存既有 inactive + `effectiveTo=null`、不投影；到期只经现有 active 动作；045 不得把这种未结束记录当失效。**一旦未来不再是 active，现网唯一索引与 findActive 都不再拦住第二条未结束预约。** 合取段把「没有其它有效或未结束的待生效唯一绑定冲突」写在**激活**句下，创建路径没写。第一次正常跑：治理入口对同一空主体商户连建两条 future inactive，库层与服务层都放行，违反 PGM-026/BEQR-143「一组唯一关系」。不是两三件极端巧合。

040「首次建立且三主体空可继续」会被读成允许两次「首次」。031「生效绑定普通停用拒绝」管不到尚未投影的 future。必须在创建与激活都占用同一未结束槽。

需修正文字（写入 041 行及 031/040/041 合取段，不新状态/调度）：

> 未结束绑定占用唯一槽：该商户若已有 `status=active`，或 `status=inactive` 且 `effectiveTo IS NULL`（未来预约），创建另一条绑定一律拒绝。不得只依赖 active 部分唯一索引。激活不得早于 effectiveFrom，须商户有效、项目匹配、且没有另一条未结束绑定。已结束历史（inactive 且 effectiveTo 非空）不重新打开、不改回未结束。普通停用/暂停已生效绑定直接拒绝，投影不变，清空不是修复。

最低可执行修法：服务层创建/激活共用「未结束绑定」查询（active **或** inactive∧effectiveTo 为空）；未来写入 inactive+effectiveTo=null 且不 sync；Leasing 该 createMerchant 三列改 null 并传 projectId；同日生效仍走现有 active+sync。测试：未来签约调用顺序、两条 future 创建第二条失败、045 仍计未来预约、到期后走现有 active。

031/040/045 在补上该句后可与 041 一并执行；当前因合取缺口，041 票 FAIL，031/040/045 事实票仍 PASS、不得单独先修。

---

### 其余入队 ID（核源后均 PASS，不单独否决）

- **039**：`createSpace:595-606` 仅当 projectId 非空才查项目；父空间只查存在。探针空项目/父跨项目成立。最小：projectId 必填且等于当前真实项目；父空间与 merchantId 若有须同项目；`spaceExists` 复用同一项目范围。不发明集团架构。
- **042**：删除置 disabled；`unarchiveSpaceType:503-504` 只接受 archived。复用该入口同时接受 archived 与 disabled；pending_delete 仍不能绕过审批。
- **043**：`executeWrite:924` 固定 `Map.of()`；`businessId:966-982` 不识别 `MerchantDeleteDecision`/`SpaceTypeRecord`；驳回仍用 `merchant.delete_approved`（`:303`、`:327-330`），空间类型同构 `:571-573`。最小：对象 ID、不可变 metadata 中的理由与决定、驳回事件名反映 rejected；恢复清空档案理由不抹事件理由；删除申请与最高权限直删也留 reason。无新审计系统。旧 021 不重入队。
- **044**：`saveSpaceTypeDeleteRequest:385-394` 恒 INSERT pending；批准只改种类行。更新**本次**待审申请为 approved/rejected；最高权限直删也结清本次申请；与类型行同一事务；保留旧历史申请；不新建状态机。故障注入不扩修。
- **045**：`countMerchantReferences:196-226` 对含 merchant_id 的基表无条件 COUNT。只排除真正已结束的 inactive 历史绑定；`future inactive ∧ effectiveTo=null` 仍算占用。其它合同/钱账/权益引用保持。
- **046**：容量/面积/租金无下限；拒绝负数；可空/零保留；不发明上限或租金规则。
- **018**：`requireSubjectStatus:781-788` 接受 terminated；`updateMerchant` 立即写。探针 `PROBE_IMMEDIATE_EXIT=terminated`。普通建档拒绝 terminated；普通更新拒绝由其它状态直接切到 terminated；已是 terminated 且无状态变化的保存不擅自改状态。不新建退出调度，不把存档当退出，不新增黑名单联合审批（CX-11 仍不成立）。
- **030**：创建分支 `:878-880` 直写三 ID，不走 `requireBusinessEntity`。统一细化：已提供则存在性 + 038 项目内公司范围；缺省可空；不强制自动绑。Leasing 权威主体走随后绑定入口（041 那一处改 null）。
- **047**：`FieldLabels.translate` 遇汉字原样透传；`requireBusinessEntityRole:816-817` 等中英并列。删用户 message 中的英文括注；保留中文与内部错误码；不改 FieldLabels 策略。
- **013**：`ProjectRequest:5-16`、`ProjectRecord:5-16`（M015 清单，本批允许作 013 依赖）、`createProject:396-407` 无三字段。获批：ownerPerson/address/businessScope 可空；新建/列表读回/JDBC/新迁移；旧 JSON 缺字段与旧 Java 构造（按 MerchantRecord 19 参兼容构造同法加次级构造）保留；无页面、无旧项目补录入口。新迁移按当时最大号下一号（当前仓库已见 V426），不改已发布迁移。

### 登记项 048–053

有 HTML 上位的 T-01/02/03/05 按顺序归细化，无独立工程缺陷，队列否。T-04 分页、T-06 package-info 技术性、无业务发明、队列否。`listMerchantsByBusinessEntity` 零调用保留为隔离通道，不因零调用删除；其项目谓词归 038 覆盖面，不另开 ID。

### 依赖与禁止扩大

允许为本模块已定稿 ID 改必要依赖：ProjectRecord（013）、LeasingService 及其验证（038/041 仅必要调用点与项目传递）、AuthorizationService 的最高权限**识别对齐 SessionIssuer**（038，不新权限码）、新迁移及现有测试。不扩大待审模块清单、不改判据、不改业务 HTML。本席只审不改。

### 整批结论

**FAIL**

20 行全部给票。阻断两项：038 现网最高权限/scope 不能承载已授权的归属补录与公司跨项目关联；041 未来改 inactive 后创建路径未保持未结束唯一绑定。其余 18 行事实/分类/队列可在修正上述两段文字后维持。不存在「PASS 但须再补」。第一批本席 V0.06 PASS 不覆盖本方案。本票不授权 B3、不修改台账。合取须双 PASS。

未改业务 HTML、代码、测试、配置、findings、ledger；未 commit/push/merge/部署；未派子代理；未新增报告文件。

---

## M009 第三批B2整批归并复核（Grok 乙席）

- 文件自身版本：V0.08（本段追加；上方 V0.01–V0.07 前缀原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙。步骤：M009 第三批 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 日期：2026-09-14
- ledger 只读：V3.57，M009 IN_PROGRESS，B3=0/3；013/038 授权已收到并持续有效，本席不重问。未改 ledger/findings/HTML/源码/测试/配置/锁。
- 本席 B2：**PASS**（20 行全部覆盖并给票。添加 0、不发 2.2。前两批阻断已写入当前候选且可执行。无条件通过。）

此票只判断 findings.md V2.07 顶部「当前M009第三批B2完整候选」（唯一当前表，20 行，14 是/6 否）相对本次两份完整 B1 raw、前两批双方 B2 完整意见、以及操作员已批准的 013/038 范围。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL**、V0.06 第一批 B2 **PASS**、V0.07 第二批 B2 **FAIL**。先前票不自动覆盖本方案。席甲票仍是作者自审票；本席 FAIL 具否决权，本批无否决点。

### 输入指纹（本席独立实算）

| 对象 | 声称 | 本席实算 | 核对 |
|---|---|---|---|
| findings.md V2.07 | 833659 bytes，SHA256 `7276a84f4af5d44937de776329b6cb151501cf0c84e8839a4f194b259a8d1178` | 同字节、同 SHA | 全等。只采顶部「当前M009第三批B2完整候选」20 行；后文旧候选/待答复作历史 |
| ledger.md V3.57 | M009 IN_PROGRESS，B3=0/3 | 顶部 V3.57；锁行 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；LOCK-AMEND-COUNT=1 | 状态与 B3 计数相符 |
| 本文件追加前 | 190101 bytes，SHA256 `5c90c2708cc3f19fcd52d7ad41bd1f4d64817d93ce2a370d4661be445e0527e6` | 同 | 全等。含 V0.05 B1、V0.06 PASS、V0.07 FAIL |
| self-raw（派发给定） | V0.04 B1 + V0.05/V0.06/V0.07 B2 FAIL，99926 bytes，SHA256 `f3dace6ad1401d110c9b412cf2a0ea0036a203152a8e1948c44115deb409af6d` | 当前文件前 99926 字节 SHA 即该值 | 见下段澄清 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` | `git rev-parse HEAD` 同值 | 相对 B1 的 `c039798` 仅 3 个 M046 认证测试；20 源文件及 3 HTML 与 B1 逐文件 SHA 全等 |
| criteria-lock.md | sidecar 同行 | 本席 `sha256sum` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` | 与 sidecar/ledger 全等。本段不写独占 `CRITERIA-LOCK-SHA256:` 行 |

**V0.07 表并列口径澄清（不回改旧段）：** V0.07 输入表把 self-raw 写成「95100 bytes」并与 SHA `f3dace6a…deb409af6d` 并列。本席现核：当前文件前 95100 字节 SHA 为 `c186db9d0c79bea9495a1c2fd5133559ac6b73aa5f1a6ef02f912ca617b29cf1`；前 99926 字节 SHA 才是 `f3dace6a…`。该 SHA 对应的是已含 V0.07 的 99926 字节前缀，不是 95100 字节旧前缀。派发本批时的权威输入是 99926 bytes 且含 V0.07。本席读取时 self-raw 整文件已因席甲本批第三批追加变为 105977 bytes / SHA `0c4415394df4e75826a7cd7cc779f07f927a5733298688d34e5d85cfdb4b5ddd`；**不以席甲本批第三批票作差集或合取证据**，B1 finding 集合与前两批 B2 意见仍只取 V0.04–V0.07 前缀。

B1 finding 集合：席甲 CX-01…10、12、13（CX-11 黑名单联合审批仍排除）；本席 GR-01…05、T-01…06。前两批 B2：本席 V0.06 PASS（038 当时只收窄项目/空间）已被批准后的完整归属方案取代，**不沿用该 038 处置**；本席 V0.07 FAIL 的两项阻断（生产 USB 识别/scope、创建与激活共同未结束唯一槽）须在本表落实。席甲 V0.07 FAIL 的三项（专用 project_id 补录、公司显式 projectId、事务内商户行锁）同样须落实。013/038 授权原文持续有效，无待答复 2.2 项。

### 技能（本批实际读取）

| 技能 | 用法 |
|---|---|
| graft（只读） | `graft grep` 定位 isMasterAuthority / SessionIssuer 常量 / 列表与绑定 / Leasing 调用点 / AuditedOperationExecutor；`graft check` 只读确认图谱未另建。未 `graft build`、未新建节点或产物 |
| using-superpowers | 用户指令优先；本任务是指定 B2 归并，不跑 brainstorming / Arc audit |
| grilling | 只作内部追问树（USB 删除是否发明跨项目规则、空上下文 403、未结束槽），不问操作员、不派子代理 |
| verification-before-completion | 先跑哈希/HEAD/SQL/源码再给票 |
| using-arc | 只核路由：用户指定本席只做第三批 B2，不跑 Arc audit（会派子代理并另写产物） |

未冒称：batch-grill-me、receiving-code-review 技能文件本批未读、ponytail 落地、tdd、research。未派子代理。未读另一套 fresh-first-principles 审计结论。

### 差集

CX-01…10/12/13 与 GR-01…05、T-01…06 全部落入顶部 20 行；无漏并、无改写原意、无新 ID、无把 CX-11 塞回。旧 013/018/030/031 沿用；旧 021 由 043 覆盖不复开；旧 001–037 其余保持否。分类：9 冲突 + 5 有缺陷细化入队是，2 技术性登记否 + 4 细化登记否。无添加项。

### 第二批阻断是否已被当前候选消化

1. **专用商户归属补录。** V2.07：只条件更新 `project_id`，保留联系人/租金等其它档案；不复用全量 `updateMerchant`/`saveMerchant`（现网 `:865-899` 必填 merchantName/merchantType 并覆盖联系人）。仅真实 USB：null→明确已存在项目或同值幂等；普通更新不能认领 null、不能迁移。公司另一关联入口，两者共用短 `ProjectAssociationRequest`。公司新建 `BusinessEntityRequest` 可选 `projectId` 并保留旧构造。对应席甲 V0.07 第 1/2 点。**已写入，可执行。**
2. **生产 USB 识别与真实项目。** 现网 `AuthorizationService.isMasterAuthority:139-143` 仍是 `isPlatformAdminBridge`（`account_type=platform_admin`）∧ `MASTER_USER_ID` ∧ `MASTER_USERNAME`。`ProductionAuthService.issueMasterSession` 签发 `account_type=master_authority`、`organization_id=platform-company`、`merchant_id=NULL`。`SessionIssuer:11-16` 已有三常量。候选把识别改为三常量合取，供现有 `requirePermission:80` 分支复用；不改签发/验签/会话，不新角色/权限码。普通 `platform_admin` 即便同名同 ID 也不算 USB（缺 `MASTER_ACCOUNT_TYPE`）。普通列表均不全量：非空 scope 不在 `projects` → 空 items/total=0；空上下文沿用现有 `requireProjectScope:290-292` 的 403（`PROJECT_SCOPE_REQUIRED`），不为凑空列表改公共授权语义。普通写要求真实当前项目。USB 显式归属/公司显式新建目标取请求已存在 `projectId`，**不要求等于** `platform-company`。对应本席 V0.07 阻断 1。**已写入，可执行。**
3. **038 与 043/044 合取，是否发明跨项目授权。** 见下节专核。结论：保持已有明确最高权限流程，不是本批发明新跨项目规则。
4. **五类列表 + `listMerchantsByBusinessEntity`。** 项目/空间/商户/公司/绑定的 COUNT 与取页先 SQL 过滤；公司 EXISTS；绑定沿 `merchant.project_id`。`listMerchantsByBusinessEntity` 归 038：明确项目参数 + 项目与经营主体同时过滤；现网 `:232` 在 `businessEntityId==null` 时退 `listMerchants()` 全表，048 登记否不能留下该反查。**已写入。**
5. **创建/激活共同未结束槽 + 行锁。** 创建与激活都拒绝另一条 `active`，或 `inactive ∧ effectiveTo IS NULL`；同一既有 `IdempotencyService.runAndComplete` 事务（生产路径 `TransactionTemplate`）锁对应商户行后查/写。未来 inactive 不投影；不能提前激活；已结束历史不重开；已生效不能普通停用或换身份；不清空。045 仍把未来未结束当占用。对应本席 V0.07 阻断 2 与席甲 V0.07 第 3 点。**已写入。**
6. **Leasing 真实顺序。** 现网 `autoCreateMerchantForSign:281-292` 先把三列写成 `contractSubjectId`，再 `createFourPartyBinding:300-311`。候选：三列改 null，并传已校验 `intent.projectId`；紧接绑定保留三主体与生效日。同日生效仍同步；无新状态/调度。**已写入。**

### 038 与既有最高权限删除/审批（HTML + 现码）

**现码（20 文件未改）：**

- `requestDeleteMerchant:274-285`：已有 `if (isMasterAuthority())` 直软删，按请求中的商户 ID，查占用并留痕。注释写明 BSM-004「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。
- `approveDeleteMerchant:318-320`：非最高权限则 403；驳回/通过按请求商户 ID。
- `requestDeleteSpaceType:528-536` / `approveDeleteSpaceType:549-551`：同类最高权限闸。空间种类是平台字典，不在 038「须验所属项目」的商户/绑定/空间对象清单里。
- `requirePlatformScope:481-497`：USB `merchantId=NULL` 且不是外包，现已放行（不要求 `platform_admin`）。把 `isMasterAuthority` 对齐 SessionIssuer 后，上述分支即可识别真 USB，不必把 `platform-company` 当成项目。
- `currentProjectScopeQuietly:503-506` 对 USB 返回 `"platform-company"`。complex_test：`projects` 无 `platform-company` 行。若 038 把「写入一律要求当前 scope 是真实项目」套到这些已有最高权限分支上，生产 USB 删除/审批会被误封。

**HTML 原文（不是本批新规则）：**

- BSM-004（业务地图删除段）：「存档不用审批，删除一律须由具备最高权限的人审批通过后执行……持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」对象是明确的这一条删除，要留痕。
- 红线④（业务地图 L707 / 治理地图 L528）：「看不到别的商户和别的项目的数据」；「第④类……持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕。」禁止整类关闭隔离，禁止全量列表。
- BSM-007：平台管理员与最高权限是两个角色；最高权限全平台只设一人，即持授权 U 盘者。
- 操作员 038 授权：不含页面/集团汇总/新审批系统/新跨项目授权规则。

**裁定：** 候选「只保留原本有最高权限判断的业务分支，按请求中明确商户 ID 执行并留痕；不把特例扩成普通档案修改或全局查询；绝不绕过 040」= 保持上述已有 BSM-004 流程，并用红线④「逐笔+留痕」解释为何 USB 的 `organizationId` 不是项目也不能误封这些分支。这不是新造跨项目岗位或关隔离开关。USB 列表仍空（非空非项目 → total=0），普通岗位仍不能认领/迁移/全表。空间种类删除/审批保持平台级现闸，不塞进项目 scope。043 的对象 ID/理由/驳回事件、044 的本次申请与类型行同事务（含最高权限直删结清 `saveSpaceTypeDeleteRequest` 刚插入的 pending 行）可在这些分支上落实。

本边界与 038/043/044 **无矛盾**，不要求再改候选文字。

### 逐 ID 票

| ID | 候选分类/缺陷/队列 | 本席票 | 核源要点 |
|---|---|---|---|
| 038 | 冲突/是/是 | **PASS** | 见上。PGM-041/BEQR-152 明文项目隔离；现网五类列表无项目谓词（Jdbc `:437-525`）。最小修法即候选执行边界，不再另写。 |
| 039 | 冲突/是/是 | **PASS** | `createSpace:595-606` 项目可空；父空间只查存在。`spaceExists:413-414` 无项目范围。最小：projectId 必填且等于当前真实项目；父空间与 merchantId 若有须同项目；spaceExists 复用同一范围。 |
| 040 | 冲突/是/是 | **PASS** | `changeBusinessEntityBinding:718-749` 普通 inactive/suspended 改当前绑定并可再绑；PGM-026 要求财税并审。收窄普通入口；USB 例外不得用于换三主体。 |
| 041 | 冲突/是/是 | **PASS** | 创建恒 `active` 并 `syncMerchantEntities`（`:683-698`）。未来改 inactive+`effectiveTo=null`、不投影；创建与激活共用未结束槽；行锁；Leasing 三列改 null。同日仍同步。 |
| 031 | 冲突/是/是 | **PASS** | 停用后投影仍在（GR-02）；`effectiveTo=today` 可早于未来 `effectiveFrom`。与 040/041 合取：已生效普通失效拒绝、投影不变；未生效不写倒置日期；已结束不重开。 |
| 042 | 冲突/是/是 | **PASS** | 删除置 disabled；`unarchiveSpaceType:503-504` 只接受 archived。复用该入口同时接受 archived/disabled；pending_delete 仍不能旁路。 |
| 043 | 冲突/是/是 | **PASS** | `executeWrite:924` `Map.of()`；`businessId:966-982` 不识别 `MerchantDeleteDecision`/`SpaceTypeRecord`；驳回仍 `merchant.delete_approved`（`:303`、`:327-330`），空间种类同构 `:571-573`。补对象 ID、不可变 metadata 理由/决定、驳回事件 rejected；恢复清空档案理由不抹事件理由；申请删除与最高权限直删留 reason。旧 021 不重入队。 |
| 044 | 细化/是/是 | **PASS** | `saveSpaceTypeDeleteRequest:385-394` 恒 INSERT pending；批准只改种类行。更新**本次**待审行为 approved/rejected；最高权限直删也结清；与类型行同一事务；保留旧历史；不新建状态机。 |
| 045 | 细化/是/是 | **PASS** | `countMerchantReferences:196-226` 对含 merchant_id 的基表无条件 COUNT。只排除真正已结束的 inactive 历史绑定；future inactive∧`effectiveTo=null` 仍占用。其它合同/钱账/权益保持。 |
| 046 | 细化/是/是 | **PASS** | 容量/面积/租金无下限。拒绝负数；可空/零保留；不发明上限或计价。 |
| 030 | 细化/是/是 | **PASS** | 创建分支 `:878-880` 直写三 ID。已提供则存在性 + 038 项目内公司范围；缺省可空；不强制自动绑。Leasing 权威主体走随后绑定（041 改 null）。本席 V0.06 已接受统一细化，本批维持。 |
| 047 | 冲突/是/是 | **PASS** | `requireBusinessEntityRole:816-817` 等中英并列；`FieldLabels.translate` 遇汉字原样透传。删用户 message 英文括注；保留中文与内部错误码；不改 FieldLabels 策略。 |
| 018 | 冲突/是/是 | **PASS** | `requireSubjectStatus:781-788` 接受 terminated。BEQR-155：「商户退出不是立即生效，一律设缓冲期」。普通建档拒绝 terminated；普通更新拒绝由其它状态直接切到 terminated；已是 terminated 且无状态变化的保存不擅自改状态。不新建退出调度，不把存档当退出，不新增黑名单联合审批（CX-11 仍不成立）。 |
| 013 | 细化/是/是 | **PASS** | `ProjectRequest:5-16`、`ProjectRecord:5-16`、`createProject:396-407`、complex_test `projects` 十列均无负责人/地址/经营范围。获批：`ownerPerson`/`address`/`businessScope` 可空；新建/列表读回/JDBC/新迁移；旧 JSON 缺字段与旧 Java 构造保留；无页面、无旧项目编辑补录。新迁移按当时最大号下一号（仓库已见 V426），不改已发布迁移。 |
| 048 | 细化/否/否 | **PASS** | 有 PGM-027/BSM-072 上位；无独立缺陷。项目谓词归 038，不因零调用删除，也不把登记否读成保留跨项目反查。 |
| 049 | 细化/否/否 | **PASS** | 已有审批链 JDBC；不新增角色/流程。`findDirectSuperior` 一人一个上级仅观察。 |
| 050 | 细化/否/否 | **PASS** | 联系人加密与读时脱敏；无新业务发明，当前无缺陷。 |
| 051 | 技术性/否/否 | **PASS** | COUNT/LIMIT/`PageResponse` 资源承载；无业务实体/状态/收费/权限/业务默认值。 |
| 052 | 细化/否/否 | **PASS** | 直接 DELETE 拒绝、导向存档审批；恢复缺陷在 042，不重复判缺陷。 |
| 053 | 技术性/否/否 | **PASS** | `package-info.java` 仅 package 声明。无业务发明。 |

### 本席实跑（B2，只读）

- `sha256sum` findings/ledger/两 raw/criteria-lock；`git rev-parse HEAD`；20 文件及 3 HTML 逐文件 SHA 与 B1 清单全等。
- 20 文件相对 `c039798`：无 diff。HEAD `e456ba96` 相对该提交仅 `AuthServiceTests` / `MasterAuthorityServiceTests` / `ProductionAuthJdbcIntegrationTests`。
- self-raw 前 99926 字节 SHA = 派发给定值。
- graft grep：`isMasterAuthority`、`MASTER_*`、列表/绑定/Leasing/删除审批。
- 只读 `complex_test`（仅三个 `COMPLEX_DATABASE_*`，库名改写为 `complex_test`，不打印凭据，exit 0）：
  - `uq_business_entity_bindings_active_merchant` 仅 `status='active'`
  - `merchants` 无 `project_id`
  - `projects` 无 owner/address/business_scope；`id='platform-company'` 行数 0
  - `spaces.project_id` nullable
  - `business_entity_projects` 表不存在
- 未跑会改库的测试套件；75/75 历史绿不代替本票。
- questions/ 现有答复段首行均为「保留」或「删除」；无 M009 未答复项。

### 依赖与禁止扩大

允许为本模块已定稿 ID 改必要依赖：`ProjectRecord`（013）；`LeasingService` 及其对应验证（038/041 仅必要调用点与项目传递）；`AuthorizationService` 共用最高权限识别及直接受影响的身份夹具/验证（038，保持已有角色和权限码；`PlatformServiceTests:119-124` 现用 `platform_admin`+`org_1` 冒充，须改真实三件套）；新迁移及现有测试。不扩大待审模块清单、不改判据、不改业务 HTML。本席只审不改。

### 整批结论

**PASS**

20 行全部给票。差集完整；四分类与队列归属正确；14 是 / 6 否可执行。前两批阻断已并入 V2.07 文字，核源后无新阻断。038 与 043/044 合取保持已有 BSM-004 最高权限明确对象流程，不是发明跨项目授权规则。不存在「PASS 但必须再补」。本票不授权 B3、不修改台账。合取须双 PASS。

未改业务 HTML、代码、测试、配置、findings、ledger、锁；未 commit/push/merge/部署；未派子代理；未新增报告文件。

---

## M009 第四批B2整批候选V2.09复核（Grok 乙席）

- 文件自身版本：V0.09（本段追加；上方 V0.01–V0.08 前缀原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙。步骤：M009 第四批 B2 整批候选复核。不是 B1 重审、不是 B3 修复、不是 B4/B5、不是模块 PASS。
- 日期：2026-09-14
- 正本：`/home/james/complex-management` `main`。HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`。未提交、未推送、未部署。
- 计划：160-audit-prompt.md V0.17；`plan/modules.md` M009 仍 20 文件、冻结有效行 **2476**、NORMAL；映射 87 条（lock 行 6388）未改。禁止改 plan/lock。
- ledger 只读：顶部 V3.61；M009 `IN_PROGRESS`；B3 已执行 **1/3**。候选未双 PASS，**不可实施**。013/038 操作员批准持续有效，本席不重问。未改 ledger/findings/HTML/源码/测试/配置/锁。
- 本席 B2：**PASS**（23 行全部覆盖并给票。添加 0、不发 2.2。原 14 是 / 6 否全文未翻。054–056 差集完整、分类与最小边界可执行。无条件通过。）

此票只判断 findings.md V2.09 顶部「当前M009第四批B2完整候选（唯一当前表；未双PASS）」23 行（17 是 / 6 否）相对两份已封存 B4-r1 本次完整票。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL**、V0.06 第一批 B2 **PASS**、V0.07 第二批 B2 **FAIL**、V0.08 第三批 B2 **PASS**、也不覆盖本席 B4-r1 V0.02 **内容 FAIL / 归因 PASS**。先前票不自动覆盖本方案。席甲票仍是作者自审票；本席 FAIL 具否决权，本批无否决点。未读本批 Codex B2 未封存稿或当前投票稿。

### 输入指纹（本席独立实算）

| 对象 | 声称 | 本席实算 | 核对 |
|---|---|---|---|
| B4-r1 self V0.02 | 359046 bytes，SHA256 `3ac0c87d592b3cbe0e3b7697e3112cd819d1c9e3649e35fed2c1e1f7eb98ea74` | 同字节、同 SHA | 全等。只采末尾本次票（自最后一个独占锁行 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` 起至 `END_CURRENT_M009_B4_SELF_V0.02`），旧 r1 前缀不作当前票 |
| B4-r1 grok V0.02 | 318436 bytes，SHA256 `92c5964ee2fbf14077f24dc595295a86ea1a0658bd75872a43dd72b1c688bdd1` | 同字节、同 SHA | 全等。只采末尾本次票（自 1726 行独占锁行起至文末结论），110075 字节旧 r1 前缀不作当前票 |
| findings.md V2.09 | SHA256 `121cca9e64a137a9fb500eae161a2c23c6104e7aaf5683311a7fac2269749d21` | 854332 bytes，同 SHA | 全等。只采顶部「当前M009第四批B2完整候选」23 行及 054–056 / 继承边界；后文第三批定稿与旧候选作历史 |
| 本文件追加前 | 208039 bytes，SHA256 `f079ff64a85e4a5540c3f135de0d3f796037f9876a260870c4188600b92a5654` | 同 | 全等。含 V0.05 B1 至 V0.08 第三批 B2。本段不产生新 B1 锚点 |
| before / after 树 | `fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac` / `66f312549c6f1ea7ed5e510f639e9238c4c674e9` | `git cat-file -t` 均为 tree；工作区 25 路径与 after 一致 | 代码保持。本席不重跑 B4 全量套件，执行证据采已封存 B4 原始输出 |
| criteria-lock.md | sidecar / ledger 同行 | 本席 `sha256sum` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` | 与 sidecar、ledger 顶部全等。本段不写独占锁哈希字段行 |
| HTML 三份 | B4 已核 | 本席重读 BSM-004 / PGM-041 原文 | 与 lock 引用一致；未改 HTML |

B4 两席本次完整报告：归因双 PASS、内容双 FAIL。按回退规则，本批含无正式 ID 的 GR-01…03 / CX-B4-01，整批先回 B2，禁止只修已有 14 条。冻结 diff：25 路径、185872 bytes、SHA256 `67a1597fa96e386cb9d21ee0a4c6484213676de9dff433547a88ab08136b9f46`（两席 B4 一致）。

### 技能（本批实际读取）

按本轮控制提示词收窄。已读再用：`using-superpowers`（开工自检）；`receiving-code-review`（先核实候选与本席旧票，禁止表演性同意，B2 期间不实施修复）；`grilling`（只借追问结构，事实自读，不派子代理、不等操作员）；`verification-before-completion`（SHA / 行数 / HTML / 入口代码 / 封存输出在给票前核完）；`graft`（只读：`changePositionTemplateLifecycle`、`requirePlatformAdmin`、`isMasterAuthority`、`currentMerchantScope`）。另读 `codebase-design`（054 只动单方法入口缝，不改共享 `requirePlatformAdmin`）；`diagnosing-bugs` / `systematic-debugging`（只借读错误→追数据流，不加诊断埋点、不改码）。

缺口/不适用：`i-have-adhd` 本环境未核到独立 SKILL.md；`batch-grill-me` 未作证据。明确不用：子代理类技能、`using-git-worktrees`、`research`、`ponytail-audit/debt/gain`。未核实的技能未写成已调用。未跑 `complex_test` 本票新套件——B4 封存输出已构成类型 4 证据；本席只读核源与 SHA。

### 差集（B4 本次 findings → 候选正式 ID）

| B4 临时号 | 封存证据要点 | 候选正式 ID | 差集结论 |
|---|---|---|---|
| 本席 GR-01 + 席甲 CX-B4-01 | 真 USB 调 `changePositionTemplateLifecycle` 在 `requirePlatformAdmin`（`:2181-2185`，调用点 `:488`）被拒；内层已有 `isMasterAuthority` 审批/直办。B4 只读重跑 1 例 0 failure / 1 error | **M009-054** | 同一项。分类取冲突（见下），不是席甲 B4 当时建议的「细化」 |
| 本席 GR-02 | `BarStoredValueFundChainJdbcIntegrationTests` 1F+4E、`StoredValueNotificationIntegrationTest` 1E，码 `MERCHANT_SCOPE_REQUIRED` | **M009-055** 的资金/通知部分 | 并入。不得因「原有失败」条件 PASS |
| 席甲 B4 十一类原文已列、本席 GR-02 未单列 | `M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues:480` 同类 `MERCHANT_SCOPE_REQUIRED` | **M009-055** 的员工隐私部分 | 席甲完整 B4 已列，属本批直接回归，不是无证据扩旧失败 |
| 本席 GR-03 | `M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` →「必须登记直接受益人」 | **M009-056** | 一对一。细化缺陷 |

无遗漏临时 finding。本席 B4 OBS-01…08 与席甲「八项 before 可复现 / AccessServiceTests 六项受益人」保持观察，**不新分配正式 ID**。六登记否（048–053）未当缺陷重开。

### 本席上票事实纠正（不回改 B4 旧段）

1. **GR-02 / GR-03 不得标「本批引入 / 本批夹具连带」。** 席甲 B4 `cxr_baseline_selected_run`：恢复 before 树 `AuthorizationService` **且**原身份夹具后，所选 94 例仍 **1 failure + 7 errors**（M006 受益人 1E、M010 商户范围 1E、Bar 1F+4E、通知消费 1E）。`currentMerchantScope` 在 before 树 `:424-431` 已对无 `merchantId` 一律抛 `MERCHANT_SCOPE_REQUIRED`，与 after `:425-431` 同文；把夹具 `accountType` 从 `platform_admin` 改为 `MASTER_ACCOUNT_TYPE` **没有制造**这八项原有失败。本席 B4 §5 把资金/通知写成「本批引入」、把受益人写成「本批夹具连带」，事实不成立。**纠正后仍须修当前直接夹具**：原有 ≠ 条件 PASS，不能恢复假 `platform_admin` 最高权限或放宽生产守卫。
2. **「派发记录 ProductionAuth 8 errors」无对应本轮派发内容。** 席甲十一类合计 **282 / 1 failure + 8 errors**，8 errors 是 M006×2 + M010×1 + Bar×4 + 通知×1，**不是** `ProductionAuthJdbcIntegrationTests`。本席 B4 把「8 errors」误记到 ProductionAuth，本票作废该归因。
3. **本席 fresh XML 13 类 320 / 1 failure + 13 errors 独立口径保留。** 核计数：十一类 282 去掉旧 mtime 的 M010 44 例，加入 AccessJdbc 24 + AccessService 50 + ProductionAuth 8 → 320。错误：原 8E 去掉 M010 的 1E，加入 AccessServiceTests 未改类 6E → 13E；1 failure 仍为 Bar 柜台退款断言。ProductionAuth 8 例本席当时全过。AccessServiceTests 六项受益人失败只作观察，本次不扩入 055/056。
4. **行数口径禁止混用。** 冻结 `plan/modules.md` M009=**2476**。before 同 20 文件本席实算 **2879**，after / 工作区 **3368**（`git cat-file`/`cat` 换行计数）。席甲 B4 头「原 20 文件/2879 行」是修复前实读，**不是**冻结计划计数。候选已区分。禁止改 plan/lock。

### 054 / 055 / 056 核源

**054（冲突 / 工程缺陷是 / 队列是）**
- HTML BSM-004 原文：`docs/business-system-map-for-operations-v0.1.html:576-577,588`——删除须由具备最高权限的人审批；「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」；「平台管理员与最高权限是两个不同的角色……最高权限……即持有授权 U 盘密钥的那个人」。
- 现码：`AccessService.changePositionTemplateLifecycle:485-545` 入口无条件 `requirePlatformAdmin`（`:488` → `:2181-2185` 只认 `isPlatformAdmin`）。同方法内层 `request_delete` / `approve_delete` / `reject_delete` 已按 `isMasterAuthority` 直办或审批。Controller `:192-202` 已单独查功能权限。生产 USB 是 `master_authority` 三件套（after `AuthorizationService.isMasterAuthority:139-144`），不是 `platform_admin`，进不了已有删除审批分支。这与 BSM-004 明文矛盾，**冲突**优先于席甲 B4「细化」建议。
- 最小边界同意候选：仅这一方法入口兼容真实 `isMasterAuthority`；**不改**共享 `requirePlatformAdmin`、不放宽 Access 其它入口、不改资金。保留已有 `archive` / `restore` / `request_delete` / `approve_delete` / `reject_delete`、功能权限 / DENY / 状态 / 占用 / 独立审批 / 审计。席甲 B4「archive/restore 不一并放宽」会在同一入口再按动作拆缝，且该用例 USB 段随后仍走 restore/archive/本人 request_delete（`:595-602`）；方法级兼容是更小且与内层已有动作一致的缝。
- 依据：类型 1（BSM-004）+ 类型 4（B4 封存 1 例 error 堆栈 `:592/:488/:2183`）。冻结 BSM-004 / GEN-005 / GEN-008。

**055（细化 / 工程缺陷是 / 队列是）**
- 不是新业务权限。PGM-041 / GEN-003 / GEN-005 / GEN-008：测试须在合法商户范围执行目标行为；无 `merchantId` 被拒是隔离正果，但夹具因此到不了金额/通知/脱敏断言。
- before 夹具已是 `platform_admin` + `merchantId=null`（Bar `:65-66` 旧文）；after 只改 `accountType`。`currentMerchantScope` before 已拒。before 94 例同一批商户范围失败仍在。
- 同意只调三类对应失败用例的合法操作人/商户资料/既有调用前提：`BarStoredValueFundChainJdbcIntegrationTests`（4E+1F）、`StoredValueNotificationIntegrationTest.consumeWrites…`、`M010RegressionTests.OwnAndManagementProfileResponsesDoNotReturnPersonalValues`。资金用目标商户操作人及已有权限（可复用 `TestSecurityActors.asMerchantUser`），不再用固定 master ID 配普通 `platform_admin`。
- 通知：消费改走既有合法**商户卡**路径，保留金额/余额/通知/`card_type` 断言；`consumeInternal(projectId, merchantId=null)` 无商户范围项目卡内部扣款须继续正确拒绝并锁住，**不能**把错误改成预期来伪装项目卡消费已可用。项目卡充值通知（该类另 1 例 B4 已过）保持。M010 隐私验证放在同一明确商户的合法管理/本人上下文，脱敏及明文禁止断言保留。
- 禁止：改生产资金/员工服务；为绿删除隔离检查或恢复假 master；把 AccessServiceTests 六项或其它无本批直接证据的旧失败扩入。Bar `:199` 的 1 failure 是商户范围先抛导致「到发卡商户门店办理」断言落空，夹具合法化后该拒绝断言仍须成立。
- 依据：类型 4（封存十一类 / 94 例 before 输出）+ 类型 1（PGM-041 项目/商户隔离）。

**056（细化 / 工程缺陷是 / 队列是）**
- `SupervisorReviewService.approve:203-205`：`permission_grant_l3` 属须受益人类型；schedule 缺受益人即 `BENEFICIARY_REQUIRED`。该用例请求第四参为 null，且未打 `findSupervisorReviewSchedule`。before 94 例在原身份夹具下已 1E（旧行号 `:418`），不是改 `accountType` 制造的。
- 同意只在该用例补测试授权的真实受益人及既有 schedule 等数据；仍先证普通高职级不能代替 USB，再证真 USB 合法批准同一事项。不偷换 `actionType`，不豁免生产受益人/利益冲突检查。
- 依据：类型 4 + GEN-003 / GEN-005 / GEN-008。

### 原 14 是 / 6 否

对 V2.08 历史定稿表与 V2.09 当前表逐列（8 列）比对：038/039/040/041/031/042/043/044/045/046/030/047/018/013 及 048–053 **全文全等**。B4 核心三类 106 与直接七类 188 主路径通过，**不得**把 14 是翻否；只有将来 B4 双 PASS 后才改队列。031/040/041 共同边界两处正文全等；013 必要依赖段全等。继承 038 边界 9 条相对获批原文，仅「新建商户」条把「经既有 USB…」写成「公司新建经既有 USB…」，语义仍是已批准的公司新建显式 `projectId`，**不是**新授权、不发 2.2。

### 逐 ID 票

| ID | 分类/缺陷/队列 | 本席 | 要点 |
|---|---|---|---|
| 038 | 冲突/是/是 | **PASS** | 保持已批准归属/补录/真 USB 识别。不因 B4 主路径绿翻否 |
| 039 | 冲突/是/是 | **PASS** | 同左 |
| 040 | 冲突/是/是 | **PASS** | 同左 |
| 041 | 冲突/是/是 | **PASS** | 同左 |
| 031 | 冲突/是/是 | **PASS** | 同左 |
| 042 | 冲突/是/是 | **PASS** | 同左 |
| 043 | 冲突/是/是 | **PASS** | 同左 |
| 044 | 细化/是/是 | **PASS** | 同左 |
| 045 | 细化/是/是 | **PASS** | 同左 |
| 046 | 细化/是/是 | **PASS** | 同左 |
| 030 | 细化/是/是 | **PASS** | 同左 |
| 047 | 冲突/是/是 | **PASS** | 同左 |
| 018 | 冲突/是/是 | **PASS** | 同左 |
| 013 | 细化/是/是 | **PASS** | 已批准三字段可空；不重问 |
| 054 | 冲突/是/是 | **PASS** | 见上。单方法兼容真 USB，不改共享入口 |
| 055 | 细化/是/是 | **PASS** | 见上。三类直接夹具合法化，不改生产资金/员工 |
| 056 | 细化/是/是 | **PASS** | 见上。只补该用例受益人与 schedule |
| 048 | 细化/否/否 | **PASS** | 登记否。反查通道已由 038 收 projectId |
| 049 | 细化/否/否 | **PASS** | 登记否 |
| 050 | 细化/否/否 | **PASS** | 登记否 |
| 051 | 技术性/否/否 | **PASS** | 无业务发明 |
| 052 | 细化/否/否 | **PASS** | 恢复缺陷在 042 |
| 053 | 技术性/否/否 | **PASS** | package-info 仅声明 |

### 第一性原理三问（本批候选）

1. 为何必须存在：删 054，BSM-004 的 USB 配置删除/本人直办在岗位模板入口做不到。删 055/056，本批直接回归到不了已锁金额/通知/脱敏/「职级≠USB」断言（GEN-003），不是新业务发明。
2. 声称是否执行：生产行为以 B4 封存 Maven 输出为准；before 94 例与十一类 282 原文已核。本票未再跑套件。
3. 未验证就当真的前提：不得把「失败原已存在」当成已修好；不得把项目卡内部扣款拒绝改写成已可用。候选文字已禁止这两条。

### 依赖与禁止扩大

第 2 批 B3（仅当本批 B2 双 PASS 之后，由唯一修复者整批执行）范围=当前全部 17 是；已落实的 14 条行为保持，未坏的不重复改。054 允许的生产改动仅 `AccessService.changePositionTemplateLifecycle` 入口兼容，属 038 已准的身份对齐之直接受影响入口，**不**把 Access 整模块列入待审清单。055/056 只改本批直接失败夹具/测试资料。不改判据、不改业务 HTML、不改共享 `requirePlatformAdmin`、不改资金/员工生产服务。

### 整批结论

**PASS**

23 行全部给票。差集完整（GR-01+CX-B4-01→054，GR-02+席甲已列 M010 同类商户范围→055，GR-03→056）。四分类与队列归属正确；17 是 / 6 否可执行。原 14 是未因 B4 主路径通过而置否。054 冲突成立；055/056 为夹具细化缺陷，不能因 before 已失败就条件 PASS。本席 B4「本批引入 / ProductionAuth 8 errors」已纠正，候选正文与核源一致。添加 0，013/038 不重问。不存在「PASS 但必须再补」。本票不授权 B3、不修改台账、不自行定稿 findings。合取须双 PASS。B3 当前 1/3，双 PASS 前候选不可实施。

未改业务 HTML、代码、测试、配置、findings、ledger、锁；未 commit/push/merge/部署；未派子代理；未新增报告文件。

## M009 第五批B2整批归并复核（Grok 乙席）

- 文件自身版本：V0.10（本段追加；上方 V0.01–V0.09 前缀 224632 bytes / SHA256 `7a73c22c80fbed6762175274fca1fd75f19d927128f3353555b4edde182d5ced` 原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙。步骤：M009 第五批 B2 整批候选复核。不是 B5 重审、不是 B3 修复、不是 B4、不是模块 PASS。
- 日期：2026-09-14
- 正本：`/home/james/complex-management` `main`。HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`。未提交、未推送、未部署。
- 计划：160-audit-prompt.md V0.17；`plan/modules.md` M009 仍 20 文件、冻结有效行 **2476**、NORMAL；映射 87 条未改。禁止改 plan/lock。
- ledger 只读：V3.66；M009 `IN_PROGRESS`；B3 已执行 **2/3**。候选未双 PASS，**不可实施**。013/038 批准持续；054-056 先改后确认历史保留，不倒填事先批准。操作员「如果不是修改html，那么修复没有问题」非 HTML 修复授权已收到，且本批 057 原文/拟改/影响已在普通对话说明，无待答问题。未改 ledger/findings/HTML/源码/测试/配置/锁。
- 本席 B2：**PASS**（24 行全部覆盖并给票。添加 0、不发 2.2。原 17 已修 + 6 登记继续否。唯一新项 M009-057 细化/是/是，最小边界仅注释+自身版本。无条件通过。）

此票只判断 findings.md V2.12 顶部「当前M009第五批B2完整候选（唯一当前表；待双PASS）」24 行（1 是 / 23 否）相对两份已封存 B5-r2 本次完整票（各自最后一个独占锁行起至文末，不用旧前缀）。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL**、V0.06–V0.09 各批 B2、也不覆盖本席 B5-r2 V0.02 **PASS**。本席 B5 PASS 不得单席覆盖席甲 B5 FAIL；不能因为只差注释就跳过 B2。席甲票仍是作者自审票；本席 FAIL 具否决权，本批无否决点。未读本批 Codex B2 未封存稿或当前投票稿。不代改本席 B5-r2 旧票。

### 输入指纹（本席独立实算）

| 对象 | 声称 | 本席实算 | 核对 |
|---|---|---|---|
| B5-r2 self V0.03 | 48547 bytes，SHA256 `5137ff3d152bb08b00561bc57f470d95d9fdd938e27a2624ac601cf6cd481435` | 同字节、同 SHA | 全等。只采最后独占锁行起的完整报告（自 136 行锁哈希至文末 FAIL）；旧前缀不作当前票 |
| B5-r2 grok V0.02 | 92216 bytes，SHA256 `9d6e4c6fa44b480e1e46c2e60582a4c6fb9abef1d633d7f542cc31c25efcc75a` | 同字节、同 SHA | 全等。只采最后哈希行起的完整报告（自 636 行独占锁行至文末 PASS）；60863 字节级旧前缀不作当前票 |
| findings.md V2.12 | 866097 bytes，SHA256 `c37083760956300222e0ca223e29cffc4523a37408677560c04643491efc5539` | 同字节、同 SHA | 全等。只采顶部「当前M009第五批B2完整候选」24 行及 057 原文/拟改段；后文第四批历史表只作 23 行对照 |
| 本文件追加前 | 224632 bytes，SHA256 `7a73c22c80fbed6762175274fca1fd75f19d927128f3353555b4edde182d5ced` | 同 | 全等。含 V0.05 B1 至 V0.09 第四批 B2。本段不产生新 B1 锚点 |
| criteria-lock.md | sidecar / ledger 同行 | 本席 `sha256sum` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节） | 与 sidecar 一行、ledger 顶行全等。本段不写独占锁哈希字段行 |
| SpaceTypeRequest.java | B5 两席表 SHA `d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2` | 同 SHA；15 行 | 工作区未改。第 6 行仍是「录入/修改…存在则更新」原句 |

B5 两席本次完整报告：席甲 **FAIL**（唯一 CX-B5-01）；本席当时 **PASS**（无新问题）。合取 FAIL，整批回 B2。禁止用本席 B5 PASS 覆盖席甲已确认问题。B5/B2 期间没有改源码。

### 技能（本批实际读取）

按本轮控制提示词收窄。已读再用：`graft`（只读：`SpaceTypeRequest` / `createSpaceType` / `saveSpaceType` 入边，确认无 `updateSpaceType` 生产入口）；`receiving-code-review`（先核源与候选差集再接受或反驳，禁止表演性同意，B2 期间不实施修复）。grilling 只借自问结构，本批未重读其 SKILL.md。verification-before-completion 按 B2 收窄：给票前核 SHA / 24 行差集 / 注释 / `createSpaceType` / 既有测试正文；**本票不重跑全模块或 283/239 套件**，测试证据采已封存 B5 输出 + 当前测试源。160-audit-prompt.md V0.17 的 B2 归并/合取/2.1 四分类段已读。

缺口/不适用：未冒称 `batch-grill-me`、Arc `audit`、子代理类技能。明确不用：MCP、第三席、`using-git-worktrees`、`research`、`tdd` 落测试。未核实的技能未写成已调用。

### 差集（两席 B5 本次 findings → 候选正式 ID）

| B5 临时号 | 封存证据要点 | 候选正式 ID | 差集结论 |
|---|---|---|---|
| 席甲 CX-B5-01 | `SpaceTypeRequest.java:6` 声称 typeCode 存在则更新名称/排序/状态；`PlatformService.createSpaceType:551-558` 重复编码抛 `SPACE_TYPE_CODE_EXISTS`，新建写死 `active`；`PlatformServiceTests:374-387` 两例在席甲 283 / 本席 239 回归中通过 | **M009-057** | 一对一。本席 B5 未单列，B2 按并集核源后**独立成立**，不是照抄 |
| 本席 B5-r2 | 三线问题全集：无。无 GR 临时号 | （无新 ID） | 并集不因本席当时 PASS 变空 |

无遗漏临时 finding。下列观察**不分配正式 ID、不入修复队列、不改 HTML/锁、不增判据**：

1. 席甲 B5：锁内 BSM-114「考勤打卡」对现行 HTML「查看考勤」；BSM-120 页脚日期 2026-09-08→2026-09-10。M009 20 文件无考勤动作或页脚实现。
2. 本席 B5「87/87 命中」原文是「锁内映射 HTML 来源行 vs 现行三 HTML checked 87 miss 0（GEN 无 HTML 行；其余**首条摘录落在引用行邻域**）」，不等于 69 条业务片段全文同字节。本席**不回改** B5-r2 旧票。
3. 本席 B5 另记 JDBC 无参分页重载、空间类型审批 Controller 未再挂功能权限码：已标明不入队，候选未新开 ID，本席同意保持观察。

048–053 六登记否未当缺陷重开。原 17 已修不得因本批只差注释而翻回「是」。

### 057 独立核源（不照抄）

**事实（本席实读，不是转述席甲票）**

- `backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java:6` 原文逐字：`/** 条目3 空间类型字典 录入/修改请求（后台管理员维护）。typeCode 为唯一键，存在则更新名称/排序/状态。 */`
- 同文件无 `// 文件版本` 或其它自身版本行（与 `ProjectRequest.java:2`「文件版本：V0.01」对照）。
- `PlatformService.createSpaceType:533-560`：已有 `typeCode` 则 `ApiException.conflict("SPACE_TYPE_CODE_EXISTS", "该种类编码已存在，不能用新增改已有种类")`；`saveSpaceType` 状态参数为字面量 `"active"`，忽略 `request.status()`；`sortOrder` 空则 100。无按编码更新名称/排序/状态的分支。
- `PlatformController`：`POST /space-types` 只调 `createSpaceType`；另有 archive / unarchive / delete-request / delete-approve；**无** `updateSpaceType`，无对 `/space-types` 的更新 PUT/PATCH。graft 入边确认生产写口是 create + 存档/恢复/删除申请，不是 upsert。
- 既有测试（只读，本票未跑）：
  - `:374-380` `createSpaceTypeRejectsExistingCodeEvenWhenActive`：对已有 `shop` 再 POST，断言 `SPACE_TYPE_CODE_EXISTS`。
  - `:382-387` `createSpaceTypeIgnoresRequestedDisabledAndStaysActive`：请求 `status=disabled`，断言结果仍 `active`。
  - `:826-836` 方法名仍写 `createSpaceTypeUpsertsDictionary…`，方法体只新建 `kiosk` 并用于 `createSpace`，**不断言**重复编码更新。不把该方法名扩进 057。
- B5 封存类型 4：席甲 11 类 283/0/0/0（含 PlatformServiceTests 69）；本席 10 类 239/0/0/0（同含 PlatformServiceTests 69）。两例在双方回归中通过，证明注释里的更新承诺第一次按文档调用即失败。

**四分类（顺序：冲突 → 细化 → 技术性 → 添加）**

1. **不是冲突。** 三份 HTML 没有「空间类型 typeCode 存在则更新」明文。BSM-004（`business-system-map-for-operations-v0.1.html:566`）讲配置主数据可存档/审批删除；PGM-005（`platform-governance-map-for-operations-v0.1.html:347`）讲维护楼层/区域/点位。矛盾发生在 **DTO 文档声称 vs 可执行入口**，不是 vs HTML 原文。
2. **是细化。** 能指向上位：BSM-004 配置类主数据维护（空间类型字典已是 042/043/044/052 的同一配置对象）；PGM-005 空间和点位。按 2.1「能指到 HTML 上位条目的一律先归细化，不进技术性通道」。
3. **不是技术性。** DTO 注释描述的是唯一键、录入/修改、状态更新，属于字典维护契约，不是无业务发明的构建配置。禁止因「只是 DTO / 只是注释」整包塞进技术性。
4. **不是添加。** 候选最小修法是删掉错误的录入/修改与存在则更新承诺，**不**新增更新接口、不改字段/类/运行分支/测试/HTML。把注释改成与已有 create-only 行为一致，不是发明新业务。

锁内 PGM-020 锚点是 `platform-governance-map-for-operations-v0.1.html:440` 综合体项目档案「主要功能」（状态/负责人/地址/经营范围，第三条含空间归属）。空间类型**字典**的直接上位是 BSM-004 + PGM-005；PGM-020 作相关治理上下文可留在冻结列，**不**把分类改成添加或技术性，也不要求改冻结列才能定稿。

冻结工程依据成立：GEN-002（文档声称须与入口/断言一致）、GEN-008（DTO 与服务/测试错误语义一致）、GEN-016（范围内文档不得把旧 upsert 写成当前事实）。依据类型：类型 2（当前源）+ 类型 4（B5 封存 Maven）+ 类型 1（BSM-004 / PGM-005 原文）。

**最小边界：同意候选。**

- 只改该 Java 注释为：`空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。`
- 按版本铁律补自身版本：原文件无版本，加入 `V0.01`（现有 DTO 惯例为 package 后 `// 文件版本：V0.01`，不另发明格式）。
- 禁止：新增更新功能；改 DTO 字段/类签名/`status` 入参语义；改 `createSpaceType` 分支、错误码、审计事件名、调用者；改测试（含不强制改 `:826` 方法名）；改三份业务 HTML。
- 不为注释加重复测试：既有 `:374-387` 已锁拒绝重复编码与新建固定启用。
- 非阻断、不入队：`executeWrite` 仍用 `space_type.updated` / `space_type.update_failed` 事件名，属历史 upsert 残留，改事件名会动运行/审计面，057 明确不改调用者；第一次按注释调用撞的是 `SPACE_TYPE_CODE_EXISTS`，不是事件名。

**授权：** 057 改的是 Java 注释，不是三份 HTML。适用已收到的非 HTML 修复授权。无 2.2 添加项，无 HTML 申请。B2 双 PASS 前仍不得改注释。

### 原 17 已修 / 6 登记否

对 V2.12 当前表去掉 057 的 23 行，与「历史第四批 / 当前M009第四批修复队列」23 行做 8 列全文比对：**逐字节全等**。队列列保持全否。B4-r2 内容/归因双 PASS 后已翻否；B5 两席均认可 17 项修复后行为仍在。本批唯一新问题不得把 038–056 / 048–053 翻回「是」。048–053 继续登记否。013/038 不重问。054-056 先改后确认时间线保留。

### 逐 ID 票

| ID | 分类/缺陷/队列 | 本席 | 要点 |
|---|---|---|---|
| 038 | 冲突/是/否 | **PASS** | 已修保持。不因注释票翻回 |
| 039 | 冲突/是/否 | **PASS** | 同左 |
| 040 | 冲突/是/否 | **PASS** | 同左 |
| 041 | 冲突/是/否 | **PASS** | 同左 |
| 031 | 冲突/是/否 | **PASS** | 同左 |
| 042 | 冲突/是/否 | **PASS** | 同左 |
| 043 | 冲突/是/否 | **PASS** | 同左 |
| 044 | 细化/是/否 | **PASS** | 同左 |
| 045 | 细化/是/否 | **PASS** | 同左 |
| 046 | 细化/是/否 | **PASS** | 同左 |
| 030 | 细化/是/否 | **PASS** | 同左 |
| 047 | 冲突/是/否 | **PASS** | 同左 |
| 018 | 冲突/是/否 | **PASS** | 同左 |
| 013 | 细化/是/否 | **PASS** | 已批准三字段可空；不重问 |
| 054 | 冲突/是/否 | **PASS** | 已修。先改后确认历史保留 |
| 055 | 细化/是/否 | **PASS** | 已修。同左 |
| 056 | 细化/是/否 | **PASS** | 已修。同左 |
| 048 | 细化/否/否 | **PASS** | 登记否 |
| 049 | 细化/否/否 | **PASS** | 登记否 |
| 050 | 细化/否/否 | **PASS** | 登记否 |
| 051 | 技术性/否/否 | **PASS** | 无业务发明 |
| 052 | 细化/否/否 | **PASS** | 恢复缺陷在 042 |
| 053 | 技术性/否/否 | **PASS** | package-info 仅声明 |
| 057 | 细化/是/是 | **PASS** | 见上。注释失真成立；只改注释+V0.01；不实现更新 |

### 第一性原理三问（本批候选）

1. 为何必须存在：删 057，调用者仍会把 `POST /space-types` 当成「存在则更新」，第一次对已有编码调用即 409，与 DTO 文档相反（GEN-002/016）。不是极端巧合。
2. 声称是否执行：当前源 `createSpaceType` 拒绝重复编码、新建固定 active；既有两测试正文即反例。本票未再跑套件。实现更新会否定这两测试并新增业务能力，超出最小修法。
3. 未验证就当真的前提：不得把本席 B5 PASS 写成「注释问题不存在」；不得把「只是注释」写成可跳过 B2 或可条件 PASS。

### 依赖与禁止扩大

第 3 批 B3（仅当本批 B2 双 PASS 之后，由唯一修复者整批执行）范围=当前全部 1 是，即仅 057。已落实的 17 条行为保持，未坏的不重复改。057 允许的生产改动仅 `SpaceTypeRequest.java` 的误导注释 + 自身版本 V0.01。不改判据、不改业务 HTML、不改运行分支/测试、不把空间类型做成可更新字典。

### 整批结论

**PASS**

24 行全部给票。差集完整（席甲 CX-B5-01 → M009-057；本席 B5 无新问题，并集仍须入队）。四分类与队列归属正确：057 细化/是/是；原 17 已修 + 6 登记继续否。最小边界可执行，且与已收到的非 HTML 修复授权一致。添加 0，不发 2.2，无 HTML 申请。两处 HTML 片段观察与本席 B5「邻域命中≠全文同字节」观察不入队，不改本席 B5 旧票。不存在「PASS 但必须再补」。本票不授权 B3、不修改台账、不自行定稿 findings。合取须双 PASS。B3 当前 2/3，双 PASS 前候选不可实施。

未改业务 HTML、代码、测试、配置、findings、ledger、锁；未 commit/push/merge/部署；未派子代理；未新增报告文件。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 Grok 独立审计原文（2026-09-21 重开）

- 席位：固定只读外审席乙 Grok（grok-4.6 / xhigh）
- 阶段：B1 修复前整模块全量重审
- 报告自身版本：V0.11（指定路径已有 V0.10 及更早前缀；本次只追加，原 239474 字节 SHA256 `f53b5784697bcb945d98d2681de9ca751c5ab9e3448b601217a39c3a12056a64` 全部原字节保留）
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 正本：`/home/james/complex-management` `main` HEAD `e456ba96c84185c8d39f18f548011daac48a6466`
- 计划源：`docs/audit-2026-09/plan/modules.md` FILES 20；冻结有效内容行 2476（计划值不改）；当前物理换行合计 **3369**
- 冻结判据：映射行 87 条（本席自 `criteria-lock.md` 抽出 COUNT=87 UNIQUE=87 MISSING=[]）
- 提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；`docs/audit-2026-09/plan/runbook.md` V0.02
- 既有输入：ledger 2026-09-21 补登记 XM-008 / XM-010（不是已定稿 finding 或分类）；M009-050 仅历史登记，不得据旧「无缺陷」免审；M009-013/038 等已批准范围及历史修复保留；B3 累计 3 保持，不等于额外 B3 授权；M038 保留现场，本次不审
- 禁止：fetch/commit/push/merge/worktree/deploy；MCP/子代理/第三席；改代码/HTML/数据库/lock/findings/ledger；写 helper/log；运行会写库或构建产物的测试。静态证据不冒称实跑。未读 Codex 本次新追加 self 结论。
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 的临时编号：GR-M009-B1-01
- LOCK-AMEND 需要（不自行扩判据、不入修复队列）：LA-M009-B1-01
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 Codex 新 self、未问操作员

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户专项规则优先于 AGENTS worktree/fetch |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 接口/深度/删除测试；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 静态追数据流；按指令不跑写库测试，故无红环实跑，缺口见下 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→追数据流；不作无 ID 改动 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称（哈希/行数/rg/python 解析）；未跑 Maven |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席：步骤+完成判据同文件 |
| receiving-code-review | superpowers 同名 | 只借「先核实」；B1 禁止修复 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本席技能树未找到可调用的 SKILL.md（mattpocock 1.2.3 无此目录）
- `ubiquitous-language`：本席会话技能树未找到（他处安装副本不冒充已调用）
- `i-have-adhd`：ponytail 插件目录无此技能；仅在 `~/.grok/marketplace-cache/.../skills/i-have-adhd` 见到副本。本报告按 B1 合同写完整证据，不以该输出模式裁剪 87 判据
- `git-guardrails-claude-code`：存在于 mattpocock `misc/`，本席未改 git、未直推，未调用
- diagnosing-bugs Phase 1「红环必须已跑过」：操作员禁止写库/构建产物测试，本席用静态命令+源码/HTML 原文，**不把未跑 Maven 写成已验证行为**
- Arc `audit`：会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本段第一行）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值；`LOCK-AMEND-COUNT: 1`（只读核，未改）
- `criteria-lock.md` 版本 V0.11；映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 UNIQUE=87
- `plan/modules.md` M009 FILES 20 条与冻结清单逐字相同
- 20 文件 `\n` 计数之和 = **3369**（`git ls-files -z -- <paths> | xargs -0 cat | wc -l`）
- HEAD：`git rev-parse HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`git rev-parse --abbrev-ref HEAD` = `main`
- 本席未 fetch、未设 `GIT_INDEX_FILE`、未动主索引
- 20 文件 SHA256 与开工冻结清单逐文件 `match=True`（见 §2）
- 未读 `M009-b1-self-raw.md` 本次 V0.11 新段

三份 HTML 本次核到的关键原文行（只读）：

- 治理 HTML:677「审计人员查看或导出敏感信息（资金流水、人脸与身份等个人信息、完整操作记录导出）需先经审批并全程留痕。」
- 治理 HTML:681 敏感页打开须记下「这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）」；留痕不提供关闭开关
- 治理 HTML:870「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」
- 治理 HTML:871「员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。」
- 锁定义：870/871 全文在 **PGM-059**（父锚点 PGM-057「个人信息看不到明文」）。**M009 映射含 PGM-016 入口卡片与 PGM-043，不含 PGM-057/PGM-059。**

R-006 操作员「覆盖：生日与姓名也要加密」：历史裁决输入，不冒充 HTML 或冻结判据。

## 2. 20 文件覆盖表（全文已读）

行 = 文件内 `\n` 计数。SHA256 = `sha256sum` 实算。冻结列与开工指令逐字比对。

| # | 路径 | 行 | 字节 | SHA256 | 冻结 | 覆盖 |
|---|---|---:|---:|---|---|---|
| 1 | backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 17 | 543 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | match | 全文 |
| 2 | backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | match | 全文 |
| 3 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 116 | 7319 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | match | 全文 |
| 4 | backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 9 | 400 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | match | 全文 |
| 5 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 76785 | 2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a | match | 全文 |
| 6 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | 296 | 16702 | 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2 | match | 全文 |
| 7 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 204 | 10238 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | match | 全文 |
| 8 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1173 | 61467 | 984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1 | match | 全文 |
| 9 | backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | 41 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | match | 全文 |
| 10 | .../dto/BusinessEntityBindingRequest.java | 17 | 442 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | match | 全文 |
| 11 | .../dto/BusinessEntityRequest.java | 26 | 944 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | match | 全文 |
| 12 | .../dto/MerchantArchiveRequest.java | 6 | 319 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | match | 全文 |
| 13 | .../dto/MerchantDeleteApprovalRequest.java | 5 | 225 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | match | 全文 |
| 14 | .../dto/MerchantDeleteRequest.java | 5 | 214 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | match | 全文 |
| 15 | .../dto/MerchantProfileRequest.java | 41 | 1800 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | match | 全文 |
| 16 | .../dto/ProjectRequest.java | 26 | 878 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | match | 全文 |
| 17 | .../dto/SpaceRequest.java | 21 | 525 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | match | 全文 |
| 18 | .../dto/SpaceTypeRequest.java | 16 | 457 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | match | 全文 |
| 19 | .../dto/StatusChangeRequest.java | 11 | 231 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | match | 全文 |
| 20 | .../dto/VenueBusinessHoursRequest.java | 22 | 700 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | match | 全文 |

合计行 3369。漂移：无。

只读直接依赖（不扩大模块、不审 M038）：`PhoneMask.java`、`AuthorizationService.currentActorHasPermission/requirePlatformScope/currentProjectScopeQuietly`、`AuditEventService.recordSuccess`、`AuditedOperationExecutor.executeWrite`、`PiiCipher` 调用点、`tools/verify-pii-encryption.mjs` 登记表、`V339__multi_party_contact_phone_pii.sql` 权限种子（M010，只作调用链事实）、`VenueBusinessHoursService.save`（M015，控制器挂载）、`PlatformControllerTests.listingMerchantsReturnsPlaintextPhoneWithPiiPermission`（M011，静态读，未跑）。

## 3. 第一性原理线

### 3.1 为何必须存在

删除本 20 文件，治理 HTML/BSM/PGM 中「建立商户档案、四类主体绑定、综合体项目、空间点位、存档删除审批」在本仓库无后台实现入口。PGM-006/020/026、BSM-004/028 做不到。package-info 仅为编译单元（历史 M009-053）。VenueBusinessHoursRequest 为控制器已挂载的 DTO，删除则 `/venue-business-hours` 无请求体类型。

### 3.2 声称是否实际执行

本席按指令 **未跑 Maven / 未连库写测试**。行为证据 = 源码路径 + `rg` + python 解析（类型 4），加上 HTML 原文（类型 1）。不把静态阅读写成「测试已绿」。

可核实现状（静态）：

- 列表/写回商户均经 `maskPhonesForUnauthorized`（Controller:68–124, 281–295）
- 持 `pii.contact_phone.view` 时原样返回 `MerchantRecord`（含明文电话）
- 落库：`contact_phone`/`secondary_contact_phone` 经 `piiCipher.encrypt`；`contact_name`/`secondary_contact_name`/`owner_person` 直接绑定（Jdbc:625–632）
- 读库：电话 decrypt，姓名原样（Jdbc:710–725）
- 列表审计：`merchant.viewed` + `businessId="all"` + metadata `count/total`（Service:151–159）
- `rg -n pii.contact_phone.viewed backend/src/main` → **0 命中**。通用审计存在，见上；不能用「无该字符串」代替调用链

### 3.3 未验证就当真的前提（grilling 自答）

1. 「有 `pii.contact_phone.view` 就可以列表明文」——实现如此，注释也如此；**不等于** HTML 871。871 在未映射的 PGM-059。
2. 「没有 `pii.contact_phone.viewed` 就没有留痕」——不成立。须追 `merchant.viewed`。追完仍缺对象 ID 与「本次是否明文」。
3. 「M009-050 写过无缺陷」——历史登记，不能免审。
4. 「R-006 已要求姓名加密」——不是冻结判据。
5. 「测试会锁住正确需求」——M011 `listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 把持权明文断言为正确；本席未跑，只静态读到该意图。
6. 「V339 注释引用 871」——迁移（他模块）同时把该权限种给 `role_platform_admin`，与 BSM-007「敏感数据查看不因平台管理员自动获得」相冲；不在本 20 文件，只记调用链观察。

## 4. 顺从式：87 判据逐项

符号：适用=本 20 文件有实现面；卡片=仅入口/他域，本模块无相反实现；缺陷=见 findings。

| ID | 适用 | 证据（文件:行 / HTML） | 结论 |
|---|---|---|---|
| BEQR-063 | 卡片 | 高危动作属酒吧域；本模块无沽清/反结入口 | 无冲突 |
| BEQR-074 | 卡片 | 隐私桌台属酒吧；本模块无桌台权限点 | 无冲突 |
| BEQR-143 | 适用 | BindingRequest 三主体+商户；Service:770–775 存在性；绑定激活投影 Service:963–967 | 满足（历史 030/008） |
| BEQR-144 | 适用 | entity_role 三值 Service:62；多对多 `business_entity_projects` Jdbc:1224–1237 | 满足 |
| BEQR-145 | 适用 | 角色 `project_company` 白名单；职责正文不在本 API 发明 | 无冲突 |
| BEQR-146 | 适用 | 角色 `management_company`；业态六值 Service:46–47 | 无冲突 |
| BEQR-147 | 卡片 | 收入归属不在本 20 文件结算 | 无冲突 |
| BEQR-148 | 适用 | 合作方式六类 Service:50–51,883–889 | 满足（历史 003） |
| BEQR-149 | 卡片 | 职级 M1–M10 不在本 API | 无冲突 |
| BEQR-150 | 卡片 | 十一端不在本 API | 无冲突 |
| BEQR-151 | 卡片 | 权限计算在 AuthorizationService（他模块） | 无冲突 |
| BEQR-152 | 适用 | 列表 SQL `project_id` Jdbc:1274–1332；写入 `requireCurrentProject` Service:411–417 | 满足（历史 038） |
| BEQR-153 | 卡片 | 终端开关优先级不在本 API | 无冲突 |
| BEQR-154 | 适用 | PROJECT_STATUSES 六态 Service:58–59,904–910 | 满足 |
| BEQR-155 | 适用 | SUBJECT_STATUSES；普通入口拒直接 terminated Service:1018–1019；归档走存档端点 | 满足（历史 018/004） |
| BEQR-156 | 卡片 | 合同状态不在本 API | 无冲突 |
| BEQR-157 | 卡片 | 员工状态不在本 API | 无冲突 |
| BEQR-158 | 卡片 | 设备状态不在本 API | 无冲突 |
| BEQR-159 | 卡片 | 权限状态不在本 API | 无冲突 |
| BEQR-166 | 适用 | Jdbc `saveChainNodes`/`findDirectSuperior` :980–1021；本模块承载审批链持久化，不发明新角色 | 登记级（历史 049） |
| BEQR-189 | 卡片 | 管理费方式属服务点单 | 无冲突 |
| BSM-001 | 适用 | 综合体基础盘子：项目/商户/空间入口存在 | 无冲突 |
| BSM-003 | 适用 | 存档删除+权限分岗在本模块落地 | 无冲突 |
| BSM-004 | 适用 | 商户/空间类型存档、申请删除、最高权限审批、占用检查、软删 | 满足（042/044/045） |
| BSM-005 | 卡片 | 分步引导属前端（M012）；本模块提供 API | 无冲突 |
| BSM-006 | 卡片 | 动态二维码不在本 API | 无冲突 |
| BSM-007 | 适用 | 删除审批走最高权限；敏感查看权限在本控制器消费。留痕不完整见 GR-01。平台管理员直授在 V339（M010）观察 | 部分；缺陷 GR-01 |
| BSM-009 | 适用 | 基础管理入口：综合体/商户/空间 | 无冲突 |
| BSM-028 | 适用 | 建项目/商户/合作方式/多对多；红线④项目隔离见 038 修复 | 满足 |
| BSM-029 | 卡片 | 连接关系说明；本模块提供归属数据 | 无冲突 |
| BSM-072 | 适用 | requirePlatformScope 拒商户账号看平台列表 Controller:47–67 | 满足 |
| BSM-074 | 适用 | 同上隔离 | 满足 |
| BSM-114 | 卡片 | 入口表；本模块是管理后台 API | 无冲突 |
| BSM-115 | 卡片 | 子系统连接说明 | 无冲突 |
| BSM-120 | 卡片 | 已拍板清单；私人手机/职级等不在本 20 文件实现。页脚日期属历史观察，不恢复旧句 | 无冲突 |
| GEN-001 | 适用 | 20 文件均有用途；零调用 `listMerchantsByBusinessEntity` 有门禁注释，历史 048 | 无新删除项 |
| GEN-002 | 适用 | 入口与错误路径在源码闭合；未跑测试 | 静态满足；行为未实跑 |
| GEN-003 | 他模块 | 测试在 M011。静态见明文断言。本席未跑 | 不把测试绿当证据 |
| GEN-004 | 适用 | requireText/白名单/非负/存在性 | 满足（046/030） |
| GEN-005 | 适用 | 写端 requirePermission+requirePlatformScope；空间类型审批服务内 `isMasterAuthority` | 满足 |
| GEN-006 | 适用 | 绑定锁商户；删除占用；日期不倒置 | 满足（031/041） |
| GEN-007 | 适用 | 写端多数 requireIdempotencyKey；`createSpaceType` 控制器未强制键，靠 typeCode 唯一冲突 | 观察，不单列缺陷 |
| GEN-008 | 适用 | 业态/状态/角色代码与中文报错并列；SpaceTypeRequest 注释已改为新建（057） | 满足 |
| GEN-009 | 适用 | ApiException 中文；不吞错 | 满足 |
| GEN-010 | 适用 | 电话落库加密；姓名等明文见 LA。持权 API 明文不是「匿名泄露」 | 电话加密成立；姓名走 LOCK-AMEND |
| GEN-011 | 适用 | 写操作用 executeWrite 带对象 ID；**读商户列表 businessId=all** | **缺陷 GR-01** |
| GEN-012 | 适用 | page size cap 500 | 满足（历史 051） |
| GEN-013 | 不适用 | 无构建文件 | — |
| GEN-014 | 他模块 | 迁移在 M010；本模块 JDBC 与现行列对齐（含 project_id、三项目字段） | 无改已应用迁移 |
| GEN-015 | 边界 | 本模块自有校验中文；`requireIdempotencyKey` 英文句在 common（他模块） | 观察，不在本 20 文件改 |
| GEN-016 | 适用 | Controller:276–278 注释引用 871 却实现权限关脱敏 | 与 LA / GR-01 相关 |
| GEN-017 | 适用 | 本报告按冲突→细化→技术性→添加 | 见 findings |
| GEN-018 | 适用 | 结论落到 HTML 原文或命令输出 | 本文件 |
| PGM-001 | 适用 | 全中文/留痕/重复提交原则；客人点开脱敏是客人查询通道，不直接覆盖商户联系人 | 商户电话走 PGM-043/LA |
| PGM-002 | 适用 | 先对象再合同再人员；本模块管对象/商户 | 无冲突 |
| PGM-003 | 适用 | 模块入口集合 | 无冲突 |
| PGM-004 | 适用 | 综合体和集团卡片 | 无冲突 |
| PGM-005 | 适用 | 空间 API | 无冲突 |
| PGM-006 | 适用 | 商户和主体 API | 无冲突 |
| PGM-007 | 卡片 | 合同准入主要在合同模块 | 无冲突 |
| PGM-008 | 卡片 | 员工手机不在本 API | 无冲突 |
| PGM-009 | 卡片 | 权限配置在 access | 无冲突 |
| PGM-010 | 卡片 | 终端开关不在本 API | 无冲突 |
| PGM-011 | 卡片 | 设备不在本 API | 无冲突 |
| PGM-012 | 卡片 | 商户侧页面不在本 API | 无冲突 |
| PGM-013 | 卡片 | 开放接口不在本 API | 无冲突 |
| PGM-014 | 适用 | 写/读均记 audit_events | 读列表对象粒度不足=GR-01 |
| PGM-015 | 卡片 | 楼宇运营不在本 API | 无冲突 |
| PGM-016 | 适用 | 入口卡片「应急与隐私…个人信息保护」。卡片原文无 871/870 细则 | 不能单独支撑明文禁令；细则在未映射 PGM-059 |
| PGM-017 | 适用 | 对象带项目归属；merchant.project_id / entity_projects | 满足（038） |
| PGM-018 | 适用 | 三角色白名单 | 满足 |
| PGM-019 | 卡片 | 收入职责表；本模块不发明收费 | 无冲突 |
| PGM-020 | 适用 | 项目档案含 ownerPerson/address/businessScope（013） | 满足 |
| PGM-021 | 卡片 | 业务意义说明 | 无冲突 |
| PGM-026 | 适用 | 联系人字段、四类绑定、财税变更拒绝普通入口（040） | 满足 |
| PGM-027 | 适用 | 商户不能看平台全局：requirePlatformScope | 满足 |
| PGM-031 | 卡片 | 经销商治理不在本 API | 无冲突 |
| PGM-035 | 卡片 | 提成发放不在本 API | 无冲突 |
| PGM-040 | 适用 | 单一功能权限码在控制器 | 无冲突 |
| PGM-041 | 适用 | 项目隔离无关闭开关（代码路径强制）；高敏「导出个人信息」本模块无导出口。查看留痕见 GR-01 | 隔离满足；查看留痕不足 |
| PGM-042 | 卡片 | 职级规则不在本 API | 无冲突 |
| PGM-043 | 适用 | :677 审计敏感查看审批留痕；:681 敏感页须记录看到哪些商户。商户列表含手机号即敏感页 | **缺陷 GR-01** |
| PGM-044 | 卡片 | 桌台挂单经销权限不在本 API | 无冲突 |
| PGM-048 | 卡片 | 商户侧全开放；本模块平台 API 隔离 | 无冲突 |
| PGM-060 | 卡片 | 会员生物信息；本模块无会员注册 | 无冲突 |
| PGM-065 | 卡片 | 治理连接各业务；本模块提供归属 | 无冲突 |
| PGM-066 | 适用 | 有过程动作须留痕；界面全中文。列表查看是过程动作 | 留痕粒度不足=GR-01 |

未映射但 HTML 存在、且本模块正在实现其对象：**PGM-057 / PGM-059**。不引用它们入队。见 LOCK-AMEND。

## 5. 对抗式（grilling 结构，自问自答）

❓ Q1 持 `pii.contact_phone.view` 的 GET `/merchants` 一次最多 500 条，能否不留「看到哪些商户」？
➡️ 能。`merchant.viewed` 的 businessId 固定 `"all"`。PGM-043 要求记下哪个商户。**成立。**

❓ Q2 无 `pii.contact_phone.viewed` 是否等于无审计？
➡️ 不等于。须追 `recordSuccess("merchant.viewed", ...)`。通用审计存在，仍不够对象级。

❓ Q3 权限是否构成 871「关闭开关」？
➡️ 是实现上的关闭开关（Controller:281–287）。871 在 PGM-059，未映射，**不能用 PGM-059 入队**。

❓ Q4 姓名明文落库是否与 870 冲突？
➡️ 870 在 PGM-059。R-006 非锁。**LOCK-AMEND，不自行扩判据。**

❓ Q5 普通岗位跨项目读写是否仍在？
➡️ 列表 WHERE project_id；写入 requireCurrentProject。商户无项目则列表不命中（SQL NULL 比较）。038 范围仍在。

❓ Q6 绑定停用能否改结算主体？
➡️ `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` 拒绝生效绑定停用与不一致三主体。040/031 仍在。

❓ Q7 未来生效日是否立即投影？
➡️ `effectiveFrom.isAfter(today)` 存 inactive 且不 sync。041 仍在。

❓ Q8 空间父级跨项目？
➡️ 父空间 `requireCurrentProject(parent.projectId())` 且自身 projectId 必填。039 仍在。

❓ Q9 直接 terminated？
➡️ 新建/改到 terminated 拒绝。018 仍在。

❓ Q10 空间类型恢复是否只接受 archived？
➡️ 接受 archived 与 disabled。042 仍在。

❓ Q11 负数面积/租金/容量？
➡️ requireNonNegative + capacity<0。046 仍在。

❓ Q12 审批缺对象 ID / 驳回事件名？
➡️ executeWrite `businessId()` 覆盖 Merchant/SpaceType/Binding；驳回用 `merchant.delete_rejected` / `space_type.delete_rejected`。043 仍在。

❓ Q13 占用统计是否把已结束绑定当活引用？
➡️ 对 `business_entity_bindings` 排除已结束 inactive。045 仍在。

❓ Q14 列表脱敏有无「关闭开关」在代码里？
➡️ 有：`canViewPhonePlaintext()`。最小删除即去掉该旁路、一律掩码——须 PGM-059 映射后才能当修复依据。

ponytail-review（不落地）：`PlatformController.java:281-295` delete: 持权明文旁路。替换：一律 `withMaskedPhones()`。`net: ~15` 行可删（待 LOCK-AMEND 后 B3）。`listMerchantsByBusinessEntity` 零调用但门禁依赖，不删（048）。

## 6. 历史定稿复核（去重；不把全部否当已实现）

findings 现值 24 行全否（18 已修 + 6 登记）。静态复核：

| ID | 现值 | 本次 |
|---|---|---|
| 038 项目隔离 | 否/已修 | 仍在 JDBC/Service |
| 039 空间项目 | 否/已修 | 仍在 |
| 040/031/041 绑定财税/日期 | 否/已修 | 仍在 |
| 042 空间类型恢复 | 否/已修 | 仍在 |
| 043/044 审计与申请状态 | 否/已修 | 仍在 |
| 045 占用 | 否/已修 | 仍在 |
| 046 非负 | 否/已修 | 仍在 |
| 030 建档主体存在 | 否/已修 | 仍在 |
| 047 中文报错 | 否/已修 | 本模块白名单提示已中文 |
| 018 terminated | 否/已修 | 仍在 |
| 013 项目三字段 | 否/已修 | ProjectRequest/JDBC 仍有 |
| 054–056 | 否/已修 | 主要在 access/测试，本 20 文件无回归点 |
| 057 注释 | 否/已修 | SpaceTypeRequest:6 已是「新建…不得重复」 |
| 048–049,051–053 | 登记否 | 维持观察，不删 |
| **050 PII** | 登记否「当前无缺陷」 | **不能免审**。电话加密仍在；脱敏关闭开关与对象级留痕是新证据 |

不重复立项已修 ID。050 旧「无缺陷」不作抗辩。

## 7. Findings

### GR-M009-B1-01

- 席位临时编号：GR-M009-B1-01（对应交接 XM-008 的留痕/调用链部分，不是 XM 定稿）
- 文件:行号：
  - `PlatformController.java:59-68, 281-295`（列表/写回经掩码旁路）
  - `PlatformService.java:144-160`（`merchant.viewed` / businessId=`"all"` / metadata 仅 count、total）
  - `MerchantRecord.java:89-96`（`withMaskedPhones`）
  - 直接依赖 `PhoneMask.java:19` 权限码；`AuditEventService.java:27-38`
- 事实：
  1. `rg` 确认 `backend/src/main` **无** `pii.contact_phone.viewed`。
  2. 调用链：`listMerchants` → `auditEventService.recordSuccess("merchant.viewed", "merchant.profile.view", "merchant", "all", null, null, Map.of("count", …, "total", …))`。不写商户 ID 列表，不写是否返回明文电话，不写是否动用 `pii.contact_phone.view`。
  3. `canViewPhonePlaintext()` 为真时 `maskPhonesForUnauthorized` 直接返回原 `PageResponse`/`MerchantRecord`，列表与 create/update/archive 单条均可含明文 `contactPhone`/`secondaryContactPhone`。
  4. 治理 HTML:681（锁 PGM-043 原文）：敏感页须记下「这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）」；该留痕不提供关闭开关。商户档案含手机号，属同锚点七项个人信息中的「手机号」。
  5. 治理 HTML:677（同 PGM-043）：审计人员查看敏感信息须审批并全程留痕。本路径把「有权限」当成一次列表即可看完全页明文，审计事件无法复原看到了哪些商户。
  6. BSM-007 / GEN-011：敏感查看须留痕；关键操作证据须可关联到对象。
- 2.1 分类：**细化**（HTML 已有敏感页/敏感查看留痕，实现下位不完整；不是新业务发明）。冲突优先已排除：没有一条 HTML 写「列表允许只记 count」。
- 2.3 依据类型：1（HTML:677/:681 原文）+ 4（`rg` 无 viewed 串；源码行如上）
- 冻结判据：PGM-043、GEN-011、BSM-007、PGM-066
- 工程缺陷：**是**
- 最小处置：在现有 `merchant.viewed`（或等价通用审计）写入本次返回的商户 ID 集合；若走明文旁路，metadata 须可区分「掩码 / 明文」且记下操作者。不强制新事件名 `pii.contact_phone.viewed`。不在本条擅自拆掉明文旁路（那依赖 PGM-059 映射，见 LOCK-AMEND）。不改 M011 测试文件（非本模块）。

### 不入队（避免扩锁 / 去重）

- **持权列表明文本身** vs HTML 871「日常界面不见明文、无关闭开关」：871 在 **PGM-059**，M009 映射没有该 ID。按铁律 6 / §5 闸 2，**不引用 PGM-059 进修复队列**。
- **联系人姓名/负责人/法定代表人/统一社会信用代码/银行账户未 encrypt** vs HTML 870：同属 PGM-059。Jdbc:625/631 姓名明文、:627 `owner_person` 明文、business_entities 三字段明文。电话两列已 encrypt，与相邻分支不同。R-006 不冒充冻结判据。
- V339 把 `pii.contact_phone.view` 授予 `role_platform_admin`：M010 迁移，本次不审该文件。
- M015 营业时间「保存即生效」：服务在他模块；M015-010 已登记 XM。本控制器仅挂载。
- `requireIdempotencyKey` 英文「idempotency_key is required」：common 模块。
- M038：不审。

## 8. LOCK-AMEND 需要（LA-M009-B1-01）

- 触发模块：M009
- 变更类型：只改 **模块映射**，不改判据定义全文
- 建议映射补入 M009：`PGM-057`（h2：应急恢复和客人隐私：**个人信息看不到明文**）、`PGM-059`（h3：客人与员工的个人信息保护 = HTML:870/:871）
- HTML 出处：`platform-governance-map-for-operations-v0.1.html:857/:868`（已在锁定义区，铁律 4 已批准的 HTML 原文）
- 为何必要：本 20 文件已经实现商户联系人手机号落库加密、读时解密、界面按权限脱敏/明文。这正是 PGM-059 的对象。不映射则 871/870 永远不能在本模块执行，会出现「代码在做隐私、锁却说本模块不用审隐私细则」。
- 映射后才能合法判定的主张：日常界面无关闭开关；姓名等是否属「等个人信息」一律加密（R-006 仍只是历史输入，映射后由 B2 按 PGM-059 原文分类，不把 R-006 写成 HTML）
- 本席 **不改** `criteria-lock.md` / sidecar / ledger 基准。须走提示词 §5 闸 2 五步。COUNT 是否计入：两条均为 HTML 派生；若登记注明铁律 4 批准依据，按闸 2 口径可能不计入次数——由调度席登记，本席不代填。
- 补映射后最早受影响模块：M009 自身（当前 IN_PROGRESS 须改回 PENDING 再从 B1 重开——由调度执行，本席不写 ledger）

## 9. 三线完成记录

| 线 | 范围 | 结果 |
|---|---|---|
| 第一性原理 | 20 文件三问 | 完成 |
| 顺从式 | 87/87 | 完成；1 条缺陷 + 1 条 LOCK-AMEND |
| 对抗式 | 越权/重复/绑定/日期/脱敏开关/审计调用链 | 完成 |

## 10. 结论

**FAIL**

未完整审完不得 PASS；本席已审完 20 文件与 87 判据，因 GR-M009-B1-01 工程缺陷成立，且隐私细则锁映射缺失须 LOCK-AMEND，单席不得 PASS。无添加项（不发 2.2）。无 HTML 修改申请。B3 累计 3 不变、本席不授权第 4 次修复。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 Grok 独立审计原文（2026-09-21 重开 B1 本次报告）

- 席位：Grok 独立审计席乙
- 阶段：B1 修复前整模块全量三线审计（完整 20 文件 / 87 判据；不是只审交接两项）
- 报告自身版本：V0.12（指定路径已有 V0.11 及更早前缀；本次只追加。调用开始时全文 269852 bytes、SHA256 `8aa550b5accea6e75bfc4d3217128ae7c880306fd6448d63f4ff6696577861ac` 全部原字节保留）
- 标明：2026-09-21 重开 B1 本次报告。上一派发因提供预期锁哈希，该次票不采信；本段结论独立形成，不沿用本文件 V0.11 段结论，未读取 Codex 2026-09-21 任一本次 self 段
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 正本：`/home/james/complex-management` `main` HEAD `e456ba96c84185c8d39f18f548011daac48a6466`（`git rev-parse HEAD` 实算，未 fetch）
- 计划：`docs/audit-2026-09/plan/modules.md` FILES 20；计划冻结有效行仍写 2476，当前 `git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` = **3369**
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；`docs/audit-2026-09/plan/runbook.md` V0.02
- 冻结判据：`criteria-lock.md` V0.11 映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 UNIQUE=87
- 既有输入：ledger 2026-09-21 补登记 XM-008 / XM-010（不是已定稿 finding 或分类）；R-006 仅历史裁决输入，不冒充 HTML/冻结判据；M009-050 历史仅登记，不得据旧「无缺陷」免审；M009-013/038 等已批准范围及历史修复保留；B3 累计 3 保持，不等于额外 B3 授权；M038 保留现场，本次不审
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 的临时编号：GR-M009-B1-01、GR-M009-B1-02
- 另报 LOCK-AMEND 需要（映射 PGM-057 / PGM-059），本席不改 lock/sidecar/ledger
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写测试、未派子代理、未调 MCP、未问操作员、未运行会写库或产生构建产物的测试

## 0. 技能自核（V0.17 §6，按 B1 收窄；本机实列目录）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户专项指令优先于技能本体（禁 worktree/fetch/子代理） |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证；作废「等用户回答」和「dispatch a sub-agent」 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling；内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 接口/深度/删除测试；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环再下结论；B1 禁止改生产代码，环用静态命令+源码/HTML 对照 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不作无 ID 插桩 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑 Maven 即不声称测试通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到该目录
- `ubiquitous-language`：本机未找到（`domain-modeling` 存在，V0.17 明确不用）
- `git-guardrails-claude-code`：存在于 mattpocock `misc/`，本席未改 git、未直推
- Arc `audit` 会派评审子代理并另写产物：与双席/§7 冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本段第一行）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值；ledger 顶部同值；`LOCK-AMEND-COUNT: 1`（只读核，未改）
- 该值由本席对现行文件实算，不以派发指令、历史 raw 或 sidecar 代填；事后发现与 sidecar/ledger 全等
- `criteria-lock.md` 889020 bytes / 版本 V0.11；映射 COUNT=87 unique=87 MISSING=[]
- `plan/modules.md` M009 FILES 20 条与用户清单逐字相同
- 20 文件 `\n` 计数之和 = **3369**（与操作员本次范围一致；计划文件仍记载切分时 2476，属计划冻结行，不是当前源码行）
- 20 文件 SHA256 与本次冻结清单逐文件 `match=True`，**无漂移**
- HEAD：`git rev-parse HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main`。本席未 `git fetch`。本地 `origin/main` 引用为 `ebb1eb99c610df330c07d24278798c2d3611299a`，仅作本地引用记录，不采信为远程现值
- 本席未设 `GIT_INDEX_FILE`，未动主索引。M009 20 文件对工作区无 diff。工作区另有非本模块脏文件（含 `VenueBusinessHoursService.java`），不在 20 文件闭集，本次不审其 diff
- 三份 HTML 只读 SHA：治理 `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`；业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；枚举 `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`
- 治理 HTML 行核：677/680/681/868/870/871 目标子串 **HIT**
- findings.md 版本 V3.20；M009 现值（「M009本次复审已收口」+ 第五批表）24 行队列全「否」（18 已修 + 6 登记含 M009-050）。M009-050 不得免审 XM-008/010
- 未读 `M009-b1-self-raw.md` 的 2026-09-21 本次 self 段

## 2. 20 文件覆盖表（全文已读；行=`b'\\n'` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 冻结 match |
|---|---|---:|---:|---|---|
| 1 | backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 17 | 391 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | True |
| 2 | backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | True |
| 3 | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 116 | 7319 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | True |
| 4 | backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 9 | 400 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | True |
| 5 | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 76785 | 2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a | True |
| 6 | backend/src/main/java/com/complex/platform/governance/PlatformController.java | 296 | 16702 | 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2 | True |
| 7 | backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 204 | 10238 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | True |
| 8 | backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1173 | 61467 | 984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1 | True |
| 9 | backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | 41 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | True |
| 10 | .../dto/BusinessEntityBindingRequest.java | 17 | 442 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | True |
| 11 | .../dto/BusinessEntityRequest.java | 26 | 944 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | True |
| 12 | .../dto/MerchantArchiveRequest.java | 6 | 319 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | True |
| 13 | .../dto/MerchantDeleteApprovalRequest.java | 5 | 225 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | True |
| 14 | .../dto/MerchantDeleteRequest.java | 5 | 214 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | True |
| 15 | .../dto/MerchantProfileRequest.java | 41 | 1800 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | True |
| 16 | .../dto/ProjectRequest.java | 26 | 878 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | True |
| 17 | .../dto/SpaceRequest.java | 21 | 525 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | True |
| 18 | .../dto/SpaceTypeRequest.java | 16 | 457 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | True |
| 19 | .../dto/StatusChangeRequest.java | 11 | 231 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | True |
| 20 | .../dto/VenueBusinessHoursRequest.java | 22 | 700 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | True |

合计：3369 行 / 180541 字节 / DRIFT=0。每文件均全文阅读并纳入三线，无抽样冒称整文件 PASS。

只读直接依赖（非本模块文件，用于调用链，不作本模块修复对象）：`PhoneMask.java`、`PiiCipher.java`、`AuthorizationService.java`（`currentActorHasPermission` / `isMasterAuthority` / `requirePlatformScope` / `requireProjectScope`）、`AuditEventService.java`、`AuditedOperationExecutor.java`、`IdempotencyService.java`、`VenueBusinessHoursService.java`（controller 入口在本模块）。M038 页面本次不审。

## 3. 第一性原理（2.3 三问）

### 3.1 为什么必须存在

删掉本 20 文件，治理 HTML「综合体和集团 / 商户和主体 / 空间」与业务地图「基础管理」的建立项目、商户档案、四类主体绑定、空间种类、存档删除审批将没有本聚合的 HTTP/服务/持久化入口。DTO/Record 是这些入口的载体。`package-info.java` 仅编译单元（GEN-017 技术性）。答得上上位需求；不能因「看起来重复」直接删除。

### 3.2 声称的行为有没有被实际执行验证

本席按专项禁令**未**运行 Maven、未连 `complex_test`、未写库、未产构建物。证据类型以 2.3 之 1（HTML 原文）与 4（可复现静态命令：sha256sum / wc / 源码与 HTML 行核）为主。静态可见测试 `PlatformControllerTests.listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 把「持 `pii.contact_phone.view` 即返回明文」写成正例，**不能**当作「行为正确」的可执行证明（GEN-003：测试本身在审计范围内）。不把静态阅读冒称实跑。

### 3.3 哪个前提未经验证就当真

- 「有 `pii.contact_phone.view` 就可以在日常列表/写回里看明文」是 V339 种子注释与 `PhoneMask` 注释的实现前提，与治理 HTML:871「不见明文；界面脱敏不提供关闭开关」及 PGM-043 正文「页面、导出同属强制脱敏面」冲突，不能当已冻结需求。
- 「没有 `pii.contact_phone.viewed` 字符串 = 没有审计」不成立：`PlatformService.listMerchants:151-159` 调用 `auditEventService.recordSuccess("merchant.viewed", ...)`。通用审计存在，但对象级/审批/明文标记不足（见 GR-M009-B1-01）。
- 「M009-050 已登记无缺陷」只覆盖「手机号 PiiCipher + 无权限时读时脱敏存在」，不能覆盖持权明文、对象级留痕、姓名落库。
- R-006「生日与姓名也要加密」不是 HTML/冻结判据，本席不以其代填 PGM-059。
- 加密存放的明文细则在 PGM-059（html:868–872），**未映射到 M009**；本模块只映射了卡片 PGM-016 与 PGM-043/060。无映射则不能把 870/871 当成「本模块映射已覆盖」，见 LOCK-AMEND。

## 4. 对抗式（grilling 结构，事实自取，不等操作员、不派子代理）

1. **无权限能否读明文手机号？** `PlatformController.maskPhonesForUnauthorized:285-295`：`canViewPhonePlaintext()` 为假则 `MerchantRecord.withMaskedPhones()`。无权限列表路径静态为掩码。
2. **持 `pii.contact_phone.view` 列表是否明文？** 同方法：为真则原样返回整页。`canViewPhonePlaintext:281-282` 只问 `currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)`。**是关闭开关。**
3. **USB 主授权？** `AuthorizationService.permissionGrant:80-83`：`isMasterAuthority` 直接放行任意码；`isMasterAuthority:139-143` 为三常量合取。主授权列表同样明文，且不经单独审批。
4. **有无 GET 单条？** 无 `GET /merchants/{id}`。单条明文出口是 create/update/archive/unarchive/delete-request/assign-project 的 `maskPhonesForUnauthorized(MerchantRecord)` 写回。
5. **查看是否先审批？** 无。权限码或主授权即明文。与治理 HTML:677「审计人员查看或导出敏感信息需先经审批并全程留痕」（PGM-043 正文，已映射）矛盾。
6. **通用审计调用链？** `listMerchants` → `merchant.viewed`，permissionCode=`merchant.profile.view`，businessId=`"all"`，metadata 仅 count/total。`AuditEventService.record` 会记 actor/time。**不是零审计。** 不满足 L681「这一次看到的是哪些对象（哪个商户）」；不区分掩码/明文；写回明文走 `merchant.updated` 等写事件，不是敏感查看事件。
7. **姓名是否与手机号同范式加密？** `JdbcPlatformRepository.merchantParameters:625-632`：`contact_name`/`secondary_contact_name` 直绑；`contact_phone`/`secondary_contact_phone` 走 `piiCipher.encrypt`。`mapMerchant:718-725` 对姓名不 decrypt、对手机号 decrypt。库层 CHECK 只卡手机号信封（测试名 `chk_merchants_contact_phone_pii`）。
8. **跨项目？** `PlatformService.currentProject:405-408` + 带 `projectId` 的 paged SQL。scope 项目不存在则 `currentProject()` 返回 null，接口默认过滤后空页。`assignMerchantProject` 仅 USB。历史 038 现场仍在。
9. **绑定唯一/日期？** `lockMerchant` + `hasUnfinishedBusinessEntityBinding`；未来 `effectiveFrom` 存 inactive 不投影；生效绑定普通停用拒绝。历史 040/041/031 现场仍在。
10. **直接 terminated？** `saveMerchant:1018-1020` 拒绝。历史 018 现场仍在。
11. **负数容量/面积/租金？** `requireNonNegative` / createProject capacity。历史 046 现场仍在。
12. **空间类型注释？** `SpaceTypeRequest.java:6` 现为「新建、编码不可重复、新建启用」。历史 057 现场仍在。

## 5. 顺从式：87 判据逐项

符号：满足＝本模块实现面与锚点无冲突（历史已修项按现值核）；缺陷＝本报告 finding；N/A＝本模块无该业务实现面且无相反规则。证据均为现行源码行或 HTML 行，类型 1 或 4。

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| BEQR-063 | 弱 | N/A | 高危酒吧动作不在本 20 文件实现 |
| BEQR-074 | 弱 | N/A | 隐私桌台不在本包 |
| BEQR-143 | 是 | 满足 | 绑定三主体 + 商户经营对象；`createBusinessEntityBinding` 校验存在性；唯一未结束槽 |
| BEQR-144 | 是 | 满足 | 无固定父链；`business_entity_projects` 多对多；`associateBusinessEntityProject` |
| BEQR-145 | 是 | 满足 | `BUSINESS_ENTITY_ROLES` 含 project_company；不把资产业态做成第四角色 |
| BEQR-146 | 弱 | 满足 | 业态六值 `MERCHANT_BUSINESS_FORMATS`；职责细则在合同/结算他模块 |
| BEQR-147 | 弱 | N/A | 收入归属不在本包计算 |
| BEQR-148 | 是 | 满足 | 合作方式六码；「其他」收费模板在合同生效，本模块不发明模板 |
| BEQR-149 | 弱 | N/A | 职级 30 档不在本包配置 |
| BEQR-150 | 弱 | N/A | 十一端不在本包 |
| BEQR-151 | 弱 | N/A | 最终权限计算在 access 包 |
| BEQR-152 | 是 | 满足+缺陷旁路 | 五类列表按 `currentProject()` 隔离（038 现场）。敏感经营价不在本包。**手机号明文开关见 GR-01，依据主要是 PGM-043/BSM-007/871 而非本条价格句** |
| BEQR-153 | 弱 | N/A | 终端开关他模块 |
| BEQR-154 | 是 | 满足 | `PROJECT_STATUSES` 六态 |
| BEQR-155 | 是 | 满足 | `SUBJECT_STATUSES`；归档走存档轴；直接 terminated 拒绝（018）；缓冲调度按已定稿不在本模块新建 |
| BEQR-156 | 弱 | N/A | 合同状态机他模块 |
| BEQR-157 | 弱 | N/A | 员工状态他模块 |
| BEQR-158 | 弱 | N/A | 设备状态他模块 |
| BEQR-159 | 弱 | N/A | 权限状态他模块 |
| BEQR-166 | 是 | 满足 | `JdbcPlatformRepository` 实现审批链/上级表（049 登记）；本模块不新造流程 |
| BEQR-189 | 弱 | N/A | 服务点单管理费他模块 |
| BSM-001 | 是 | 满足 | 本包承载「哪个综合体、谁在经营」基础盘子 |
| BSM-003 | 是 | 满足 | 通用可改可删/分岗；具体见 004/007 |
| BSM-004 | 是 | 满足 | 商户/空间类型存档、删除审批、USB 直删、占用拒绝、恢复 archived+disabled（042/044 现场） |
| BSM-005 | 弱 | N/A | 分步引导是后台页，本包仅 API DTO |
| BSM-006 | 弱 | N/A | 动态码他模块 |
| BSM-007 | 是 | **缺陷 GR-01** | 敏感数据查看须逐项授予并留痕；平台管理员不自动获得。V394 后该码 `requires_approval=TRUE` 且非 daily，平台管理员日常桥不自动放行；**持码或 USB 主授权仍直接明文且无对象级敏感查看审批留痕** |
| BSM-009 | 是 | 满足 | 基础管理入口的后端 |
| BSM-028 | 是 | 满足 | 建项目（含 013 三字段）、商户六类合作、多对多、城市项目公司角色 |
| BSM-029 | 是 | 满足 | 为票务/酒吧/财务提供项目/空间/商户归属 |
| BSM-072 | 是 | 满足 | 业态六值；`requirePlatformScope` 拒商户账号看平台全局 |
| BSM-074 | 弱 | 满足 | 「和谁有关」连接叙述；无相反实现 |
| BSM-114 | 是 | 满足 | 权限码分入口 + 项目 scope |
| BSM-115 | 弱 | 满足 | 连接叙述 |
| BSM-120 | 弱 | 满足 | 已确认规则在已映射锚点落实；不把待排期当已实现 |
| GEN-001 | 是 | 满足 | 20 文件均有用途；`listMerchantsByBusinessEntity` 隔离通道（048） |
| GEN-002 | 是 | 满足* | 入口与错误路径静态一致；*未实跑测试 |
| GEN-003 | 是 | 观察 | 存在能失败断言的测试，但明文用例把缺陷当正例；本席未跑，不声称绿 |
| GEN-004 | 是 | 满足 | 业态/状态/角色/非负/存在性/项目归属在首次使用前校验 |
| GEN-005 | 是 | 满足+GR-01 | 写入口权限+平台 scope；敏感查看授权模型与 HTML 冲突见 GR-01 |
| GEN-006 | 是 | 满足 | 绑定锁行、未结束槽、存档/删除状态机、占用统计排除已结束历史 |
| GEN-007 | 是 | 满足 | 写路径 `executeWrite`/幂等键；空间类型存档无键但是幂等状态写 |
| GEN-008 | 是 | 满足 | 六业态/六合作/状态闭集与库约束同词；057 注释已改 |
| GEN-009 | 是 | 满足 | 非法值 400 中文，不落到 CHECK 500（036 现场） |
| GEN-010 | 是 | **缺陷 GR-01/02** | 手机号加密+无权限掩码存在；持权明文与姓名明文落库 |
| GEN-011 | 是 | **缺陷 GR-01** | 写操作与列表 `*.viewed` 有通用审计；敏感明文查看不满足对象级 |
| GEN-012 | 是 | 满足 | 分页 LIMIT 500（051） |
| GEN-013 | 弱 | N/A | 无本模块构建脚本 |
| GEN-014 | 弱 | N/A | 迁移属 M010；本包只消费列 |
| GEN-015 | 弱 | N/A | 无 UI；中文错误已去英文括注（047） |
| GEN-016 | 是 | 满足 | 057 注释与行为一致 |
| GEN-017 | 是 | 满足 | package-info 技术性；PII 开关是冲突不是添加 |
| GEN-018 | 是 | 本报告 | 结论落到 HTML 原文与可复现静态命令 |
| PGM-001 | 是 | 满足 | 治理底座定位；中文规则 047 |
| PGM-002 | 弱 | 满足 | 治理顺序叙述 |
| PGM-003 | 是 | 满足 | 模块入口卡片对应本包对象 |
| PGM-004 | 是 | 满足 | 综合体和集团卡片 |
| PGM-005 | 是 | 满足 | 空间种类字典新建/存档删除 |
| PGM-006 | 是 | 满足 | 商户和主体 |
| PGM-007 | 弱 | N/A | 合同准入主实现他模块 |
| PGM-008 | 弱 | N/A | 员工和手机主实现他模块 |
| PGM-009 | 是 | 满足 | 权限点在 controller；细则 040/041 |
| PGM-010 | 弱 | N/A | 终端开关他模块 |
| PGM-011 | 弱 | N/A | 设备他模块 |
| PGM-012 | 是 | 满足 | 商户看不到平台全局（platform scope） |
| PGM-013 | 弱 | N/A | 开放接口他模块 |
| PGM-014 | 是 | 满足 | 删除/绑定写路径留痕；敏感查看见 GR-01 |
| PGM-015 | 弱 | N/A | 楼宇运营他模块 |
| PGM-016 | 是 | 卡片满足/细则缺口 | 卡片「个人信息保护」已映射；870/871 细则在未映射的 PGM-057/059，见 LOCK-AMEND |
| PGM-017 | 是 | 满足 | 对象带项目归属；空间必须 projectId 且本项目（039） |
| PGM-018 | 是 | 满足 | 多对多；USB 配归属 |
| PGM-019 | 是 | 满足 | 三角色职责不串成行政链 |
| PGM-020 | 是 | 满足 | 项目档案+013 三字段；商户联系人档案字段存在 |
| PGM-021 | 弱 | 满足 | 预留集团视图，本包不造集团汇总（授权排除） |
| PGM-026 | 是 | 满足 | 商户档案、两联系人、四类主体唯一绑定、合作六类、非负租金面积 |
| PGM-027 | 是 | 满足 | 商户不能看平台全局和其他商户；048 隔离查询保留 |
| PGM-031 | 弱 | N/A | 经销商主体他模块 |
| PGM-035 | 弱 | N/A | 提成发放他模块 |
| PGM-040 | 是 | 满足 | 单一功能权限点 |
| PGM-041 | 是 | 满足+GR-01 | 项目隔离无关闭开关（038）；导出/页面强制脱敏在同文件 PGM-043 正文。本条「与导出脱敏同例」支持隔离无开关，不单独发明手机号开关 |
| PGM-042 | 弱 | N/A | 职级规则他模块 |
| PGM-043 | 是 | **缺陷 GR-01** | 映射锚点正文含 :677 审批留痕、:680 强制脱敏且页面与导出同属脱敏面、:681 打开敏感页须记哪些对象且留痕无关闭开关 |
| PGM-044 | 弱 | N/A | 桌台挂单经销权限表不在本包执行 |
| PGM-048 | 是 | 满足 | 商户侧全开放边界：本包拒商户账号平台 scope |
| PGM-060 | 弱 | N/A | 会员/营销/生物信息主实现他模块；本包无相反发明 |
| PGM-065 | 弱 | 满足 | 连接关系叙述 |
| PGM-066 | 是 | 满足 | 已拍板：对象带项目归属、商户侧全开放、权限最小颗粒；待排期不实现 |

87/87 均有核对记录。无添加项，不发 2.2。

## 6. Findings（临时 GR；B2 再分配永久 ID）

### GR-M009-B1-01 — 日常商户接口持权/主授权返回联系人手机号明文，脱敏可被权限关闭；通用 `merchant.viewed` 不是对象级敏感查看审批留痕

- 文件:行号：
  - `PlatformController.java:276-295`（`canViewPhonePlaintext` / `maskPhonesForUnauthorized` 列表与单条写回）
  - `PlatformController.java:59-68,71-95,108-133`（list/create/update/assign/archive/unarchive/delete-request 均走掩码函数）
  - `MerchantRecord.java:89-96`（`withMaskedPhones`）
  - `PlatformService.java:151-159`（`merchant.viewed`，businessId=`"all"`，metadata 仅 count/total）
  - 依赖：`PhoneMask.java:19` 权限码；`AuthorizationService.java:80-83,139-143,266-276`（主授权放行 / `currentActorHasPermission`）
  - 静态测试：`PlatformControllerTests.java:163-179` 将持权明文写成正例
- 事实：无该权限时掩码（前 3 后 4）。有 `pii.contact_phone.view` 或 USB 主授权时，列表整页与写回单条返回解密后的明文手机号。该权限即界面脱敏关闭开关。本模块无 `GET /merchants/{id}`，单条明文=写回。`rg` 无 `pii.contact_phone.viewed` **不能**证明无审计：列表有 `merchant.viewed`，经 `AuditEventService.record` 记 actor/time/permission，但 object=`all`、不列商户 ID、不标是否明文、无查看前审批。写回明文只记写事件。
- 冻结依据（2.3 类型 1）：
  - **PGM-043**（已映射，来源治理 HTML:673 段正文）：:677「审计人员查看或导出敏感信息……需先经审批并全程留痕」；:680「导出文件是否脱敏……不提供关闭开关」「纸质凭条与页面、导出同属强制脱敏面」；:681「打开敏感页……记下……这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）；这条留痕不提供关闭开关」
  - **BSM-007**（已映射）：敏感数据查看与导出（含客人与员工个人信息）不因平台管理员自动获得，必须逐项单独授予并留痕
  - **PGM-059**（锁内有定义、**未映射 M009**，原文 :871「员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关」）——作为 HTML 派生 ID 可引用；同时触发 LOCK-AMEND 补映射，见 §7
  - 辅 **GEN-010 / GEN-011**
- 2.1 分类：**冲突**（先于细化）。与已映射 PGM-043/BSM-007 明文矛盾，不是新业务发明。
- 工程缺陷：**是**
- 最小处置（建议，B1 不修）：日常 `GET/POST/PUT` 商户档案响应一律掩码，删除或停用 `pii.contact_phone.view` 作为本接口关闭开关的行为；若保留经审批的敏感查看，须先审批、每次记录对象 ID 与是否明文，且不得把 USB 主授权静默等同日常明文。测试改为断言日常路径不见明文。不改三 HTML。跨模块同开关（ledger 所列 M223 等）不在本次修复面。
- 去重：不是 M009-050。050 只登记「PiiCipher + 无权限脱敏存在、当时无缺陷」。本条是持权明文+关闭开关+对象级留痕缺口。不复用 050 的「否」。

### GR-M009-B1-02 — 商户联系人姓名明文落库，与相邻手机号 PiiCipher 及「一律加密存放」不一致

- 文件:行号：`JdbcPlatformRepository.java:625-632`（写：`contact_name`/`secondary_contact_name` 直绑，手机号 `piiCipher.encrypt`）；`:718-725`（读：姓名原文，手机号 decrypt）
- 事实：同一 `merchantParameters`/`mapMerchant` 对两联系人手机号加密信封、对两联系人姓名明文。DB CHECK 只约束手机号 `enc:v1:%`（测试 `plaintextPhoneWriteIsRejectedByDatabaseConstraint`）。`owner_person`、主体 `legal_representative`、项目 `owner_person` 同样明文，列为观察、不另开 ID（XM-010 指定联系人姓名）。
- 冻结依据：
  - **PGM-059** 原文 :870「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」（锁内 ID，未映射 M009 → LOCK-AMEND）
  - **PGM-043** :680 封闭七项含「姓名、手机号」（已映射；该句主场景是导出/页面脱敏，加密存放的直接句在 870）
  - 辅 **GEN-010**
  - **不**引用 R-006 作为冻结依据
- 2.1 分类：能指到 PGM-059/043 上位条目 → **细化**（HTML 有加密/姓名保护，本包实现不完整）。若 B2 认为 870 仅覆盖「客人与员工」不含商户联系人，则本条缺乏已映射冻结句，应走 LOCK-AMEND 而非发明「商户联系人必须加密」新判据——**不得**在未补映射时把 R-006 当锁。
- 工程缺陷：**是**（在采纳 PGM-059 或确认七项姓名含商户联系人的前提下）。若 B2 认定 870 主体不及商户联系人且拒绝扩映射，则本条降为观察+LOCK-AMEND，不入修复队列。
- 最小处置：仅当冻结依据成立后，对 `contact_name`/`secondary_contact_name` 复用现有 `PiiCipher` 写加密读解密，并加与手机号同款 CHECK；不在 B1 扩到生日/员工姓名/其他模块列。

## 7. LOCK-AMEND 需要（本席不执行五步）

M009 映射含 PGM-016（卡片「应急与隐私」）、PGM-043、PGM-060，**不含** PGM-057（h2「……个人信息看不到明文」html:857）、PGM-059（h3「客人与员工的个人信息保护」html:868，正文即 870–872）。

870/871 是 XM-008/010 的直接 HTML 句，却不在本模块映射里。阶段 B 禁止自行扩判据。需要的是**已有 ID 的模块映射补入**，不是新写判据定义。

建议登记：触发模块 M009；变更 ID = PGM-057、PGM-059（HTML 出处：治理地图 :857 / :868；非 general）。不建议搭售 PGM-058（破窗 U 盘，本 20 文件无实现面）。补映射后最早受影响模块=M009（当前 IN_PROGRESS 须改回 PENDING 再从 B1 重开——由调度执行，本席不写 ledger）。`LOCK-AMEND-COUNT` 是否计入按 §5 闸 2：该两 ID 带 HTML 出处；是否「铁律 4 批准依据」由调度判断。

没有该映射时：GR-01 仍可由已映射 PGM-043/BSM-007 独立成立；GR-02 对 870 的直接引用偏弱，必须在 B2 二选一（用 PGM-043 七项姓名 / 走 LOCK-AMEND / 降观察）。

## 8. 历史已修/登记对照（现值去重，不免审 050）

现场抽核（不把旧 PASS 当证据）：013 三字段在 `ProjectRequest`/`saveProject`/`mapProject`；038 项目过滤与 USB 归属；039 空间 projectId；040/041/031 绑定；042–044 空间类型；045 占用排除已结束历史；046 非负；030 新建主体存在性；047 中文错误；018 拒绝直接 terminated；057 注释。054–056 属 access/测试溢出 ID，本 20 文件无对应生产面，不重开。048–053、050 登记项：050 不能覆盖本报告两条缺陷。

## 9. 观察（不入队）

- OBS-01：`createSpaceType` controller 未先 `requireIdempotencyKey`，但 `IdempotencyService.execute` 仍强制键；缺键仍 400。
- OBS-02：`VenueBusinessHoursService.list` 对平台管理员 `listAll`（M015-015 注释）。入口在本 controller，实现不在 20 文件；与五类主数据 `currentProject()` 不一致。不升缺陷、不审 M015/M038。
- OBS-03：`owner_person` / `legal_representative` 明文，同 GR-02 根因，不另开 ID。
- OBS-04：V339 把 `pii.contact_phone.view` 种给 `role_platform_admin`；V394 将其 `requires_approval=TRUE`。日常桥不自动放行该码，但角色行仍在。有效授权仍是关闭开关。

## 10. 三线完成记录

| 线 | 范围 | 结果 |
|---|---|---|
| 第一性原理 | 20 文件三问 | 完成 |
| 顺从式 | 87/87 | 完成；2 条缺陷 + LOCK-AMEND |
| 对抗式 | 越权/明文开关/审计调用链/绑定/日期/删除/隔离 | 完成 |

未完整审完不得 PASS。本席已审完 20 文件与 87 判据。

## 11. 结论

**FAIL**

因 GR-M009-B1-01 工程缺陷成立（已映射 PGM-043 / BSM-007），单席不得 PASS。GR-M009-B1-02 在 PGM-059 引用或 LOCK-AMEND 确认前由 B2 定分类。无添加项。无 HTML 修改申请。B3 累计 3 不变，本席不授权第 4 次修复。

## M009 B2 2026-09-21重开批次：Grok整批候选复核
版本：V0.13
席位：Grok 独立审计席乙
阶段：B2 整批候选复核（不修复）
本席B2结论：**PASS**（只表示本批候选归并/四分类/工程缺陷/修复队列/最小范围有效，不表示模块 PASS，不表示修复 PASS，不授权第 4 次 B3）

### 0. 技能与禁令
本席实际读取并按 B2 收窄使用：using-superpowers、grilling（只自取事实、不等操作员、不派子代理）、receiving-code-review（先对照 B1/锁/HTML/源码再接受或反驳，禁止表演性同意）、verification-before-completion（先跑命令再写结论）、ponytail-review（冲突优先删除明文豁免，不新造审批通道）、codebase-design、diagnosing-bugs、systematic-debugging、writing-for-agents。B2 不调用 brainstorming/writing-plans/tdd/ponytail 修复本体/requesting-code-review。缺口不冒称：本机未找到 i-have-adhd、batch-grill-me、ubiquitous-language。未调 MCP、未派子代理/第三席、未 fetch/commit/push/merge/worktree/部署，未写业务代码/三 HTML/lock/sidecar/ledger/findings/questions。

### 1. 输入核验（验证封存前缀，不以追加后全文 SHA 当漂移）
| 输入 | 指定封存前缀 | 本席实算 | 结果 |
|---|---|---|---|
| `docs/audit-2026-09/modules/M009-b1-self-raw.md` 最后 `CRITERIA-LOCK-SHA256:` 行起完整 B1 V0.12 | 163739 bytes SHA256 `69b413e04d3b50acfb7a84ef8b6de2d57c00cd82a5449f708772f6ed07a70f19` | 前缀 163739 bytes 同 SHA；当前全文 167491 bytes（多出的是席甲已追加的 B2 段，**不采为 B1 输入、不作为本席同意依据**） | 匹配 |
| `docs/audit-2026-09/modules/M009-b1-grok-raw.md` 最后哈希行起完整 B1 V0.12 | 301012 bytes SHA256 `0996c0167e39458ee87b7bcd4b9a9fd125a10be660a2d8e8054b44b5d2a8368a` | 追加前全文=前缀 301012 bytes 同 SHA | 匹配 |
| 候选 `docs/audit-2026-09/findings.md` | 版本 V3.21，全文 SHA256 `943b552433a7210fa07043e34b4a29d22e004f660dcd3019ad3d53c04123bd40` | 实算全文 1197458 bytes 同 SHA | 匹配 |

正本 `/home/james/complex-management` 分支 `main`，`git rev-parse HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`（未 fetch）。本席 B1 为上述 grok 前缀最后哈希行起 V0.12，不采信本文件 V0.11 段。候选顶部「当前 M009 2026-09-21 重开 B1→B2 候选」及完整 26 项表为本批输入；其下 M038 与历史 M009 各表不是本批新输入。20 文件 `\n` 合计 3369，SHA 与本席 B1 表逐文件一致（含 `VenueBusinessHoursRequest.java` 现值 `6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1`），DRIFT=0。M009 映射 87 条含 PGM-041/043/BSM-007/PGM-026/GEN-005/010/011，**不含** PGM-057/059。三 HTML 只读 SHA 与本席 B1 相同。`questions/` 无 M009 新文件。ledger 模块行 M009 = `IN_PROGRESS`，B3 累计 **3**，`LOCK-AMEND-COUNT: 1`；本席只读核，未改。

### 2. 完整差集（B1 全部意见/观察/LOCK-AMEND 建议均须入账）
| 本轮 B1 输入 | 候选处置 | 本席独立结论 |
|---|---|---|
| CX-01 日常商户接口持 `pii.contact_phone.view` 或 USB 主授权返回两联系人手机号原文；`currentActorHasPermission` 走 `permissionGrant` 布尔，不走 `requirePermissions` 的 `markSingleUsePermissionConsumed` | 并入新 **M009-058** | 事实成立。`PlatformController.java:281-294` 列表/写回明文豁免；`:59-68,71-95,108-133` 均走该函数。`AuthorizationService.java:266-276` 成功只 `return true`，消费循环只在 `:43-61`。无权限路径 `withMaskedPhones`。前端 `OrganizationMerchantPage` 直接显示 `contactPhone` 是调用链补证，不扩大本模块修复面 |
| CX-02 `merchant.viewed` 非零审计，但 `businessId="all"`、metadata 仅 count/total，无本次商户 IDs/页面/入口/筛选 | 并入 **M009-058** 同一项 | 事实成立。`PlatformService.java:151-159`。不是「零审计」，也不是旧 M009-043 的写事件对象 ID/删除理由/驳回事件名问题 |
| GR-M009-B1-01（明文关闭开关 + 对象级敏感页留痕缺口，分类冲突） | 正式 ID **M009-058**，冲突/工程是/队列是 | 未改写本席 finding：冲突优先，两处分置同一最小删除+补现有事件 metadata。同意 |
| CX-03 / GR-M009-B1-02 联系人姓名直绑明文 | 新 **M009-059**，选本席允许的「降观察、不入队」 | **独立同意，不是因调度解释自动同意**。理由见 §4 |
| 本席 §7 LOCK-AMEND 建议（补映射 PGM-057/059） | 完整保留建议；本批 058 不靠未映射句定稿，059 不入队，不改 lock | 同意本批**不执行**五步。见 §5：本席 B1「最早=M009」不得预设 |
| OBS-01 createSpaceType 未先 `requireIdempotencyKey`，下游仍拒无键 | 保留观察，不修 | 同意 |
| OBS-02 营业时间 `listAll` 属外部 service/平台列表差异 | 保留，不审 M015/M038 | 同意 |
| OBS-03 `owner_person` / `legal_representative` 明文 | 与 059 同范围说明，不扩大加密列、不另开 ID | 同意 |
| OBS-04 V339/V394 权限种子为 058 背景 | 保留未动 | 同意 |
| 历史 24 项（038…057 等） | 表内保留；18 已修队列否；048–053 登记否；050 边界收窄仍否 | 同意。旧「无缺陷」不豁免 058/059。054–056 仍属 access/测试溢出 ID，本 20 文件无对应生产面，不重开 |
| 添加项 / 新 questions / HTML 修改 | 无 | 同意。本席 B1 亦无添加项 |

漏并检查：CX-01/02/03、GR-01/02、LOCK-AMEND 建议、OBS-01…04、050/043 去重边界、B3=3 约束全部入账。无本席 B1 意见被丢掉。无新 ID 发明业务概念。26 项表唯一、无重复。

### 3. 四分类、工程缺陷、队列、最小范围
**M009-058**（CX-01+CX-02+GR-01）
- 分类：**冲突**（判定顺序冲突优先）。已映射 PGM-043 原文：页面与导出同属强制脱敏面；导出脱敏不提供关闭开关；打开敏感页须记下谁/何时/哪个页面/这一次哪些对象（含哪个商户），留痕无关闭开关。日常列表/写回因权限码或 USB 主授权返回手机号原文，等于给页面脱敏加了关闭开关；`merchant.viewed` 有操作人/时间但对象=`all`、无页面/入口/筛选/返回商户集合，与「哪些对象」明文矛盾。辅已映射 PGM-041「单次消耗全部留痕」、BSM-007 敏感个人信息查看不因平台管理员自动获得、GEN-005/010/011。席甲 CX-02 曾单列细化，并入后升冲突不改写本席 GR-01。
- 工程缺陷：**是**。修复队列：**是**。
- 不以未映射 PGM-059/871 为 058 独立定稿必要依据（871 可作背景）。PGM-043 已足够证成页面脱敏无开关与对象级留痕。
- 最小修复范围同意候选，且符合本席 B1「日常响应一律掩码；若保留经审批明文查看才须另做审批」的条件分支——候选选择**不保留**日常明文通道，因此：
  1. 删除 `PlatformController` 日常列表/写回明文豁免，复用已有 `withMaskedPhones`；持权与 USB 日常路径都走掩码。
  2. 现有 `merchant.viewed` 补返回商户 IDs 与稳定页面/入口、查询页/筛选信息；不记姓名/手机号原文。
  3. 单次布尔 grant 旁路随该豁免删除消失；**不**改 `AuthorizationService` / 全局授权器 / 其他 controller。
  4. **不**新增明文审批通道；**不**取消 USB 本人既有免审批（BSM-007 最高权限动作由密钥保管把关、不另设审批，仍只适用于 HTML 已写的启动/破窗/清理等，不得把 USB 静默等同日常列表明文）。
  5. 只改本模块 Controller/Service 及必要既有 Controller/Service 测试；不改三 HTML、不迁库、不加权限码/角色。跨模块同开关（XM-008 所列其他 controller）不在本次修复面。
- ponytail：能删豁免就不新造查看通道。`net: 删除明文分支，不新增审批面。`

**M009-059**（CX-03+GR-02）见 §4。

**登记/技术性（队列否）**：048/049/050/052 细化无新缺陷；051 分页 COUNT/LIMIT 技术性无业务发明；053 `package-info.java` 仅包声明技术性无业务发明。同意保持。

队列现值：是 = 仅 058；否 = 其余 25（18 已修工程是但队列否 + 7 登记/观察含 059）。无添加项，不发 2.2。

### 4. 对 059「降观察不入队」的独立判断（不同意则 FAIL）
本席 B1 对 GR-02 写明三分支：① 用已映射 PGM-043 七项姓名当成在库加密义务；② LOCK-AMEND 补映射 PGM-059 后再入队；③ 认定 870 主体不及商户联系人且拒绝扩映射 → **降观察、不入修复队列**。候选明确选 ③。本席对照 HTML 原文独立裁定，**同意 ③，B2 不因此 FAIL**。

依据（2.3 类型 1，不是调度口径）：
- PGM-059 原文主语是「**客人和员工**的手机号、身份信息等个人信息，在系统里一律加密存放」。没有证据把每个商户第一/第二联系人确定为系统客人或员工。把该句扩成「所有商事联系人在库必须加密」是新业务规则，阶段 B 禁止发明。
- PGM-043 七项（姓名、手机号…）出现在**导出强制脱敏**句，并写「纸质凭条与页面、导出同属强制脱敏面」。那是展示/导出打码，不是存储加密义务。同段还写员工姓名对全体员工不脱敏。不能把导出封闭清单自动变成商户联系人列的加密存放判据。
- R-006「覆盖：生日与姓名也要加密」是历史操作员裁决输入，**不是**三份 HTML，也不是冻结判据。
- PGM-026「建立商户档案…第一联系人和第二联系人」足以把「姓名作为档案字段存在」归**细化**；加密缺口在当前映射下**未被冻结句证成**。
- 现场事实保留：`JdbcPlatformRepository.java:625/631` 姓名直绑、`:718/724` 读原文，两列迁移 VARCHAR(120)，相邻手机号走 `piiCipher`，回填组件尚未登记这两列。OBS-03 同根因不另开 ID。生日归 XM-010 所属其他模块。

因此：分类细化；工程缺陷 **否（当前冻结依据未证成，不代表未来豁免）**；队列 **否**。只登记，不修代码/列/回填/检查脚本。**绝不宣称姓名已加密，也绝不宣称永久无须加密。** 若后续出现覆盖商户联系人存储加密的明确需求，再走 B1/B2，并按提示词判断是否 HTML/LOCK-AMEND。

本席 B1 finding 正文「降为观察+LOCK-AMEND」中的 LOCK-AMEND 是**建议补映射**，与 §7 三选一的「降观察」并列，不是「选 ③ 就必须本批执行五步」。候选已完整保留建议且本批不启用未映射句入队，符合完整 B1。

### 5. LOCK-AMEND（本批不扩锁；若必须则明确 FAIL）
本席判断：**本批没有锁外仍必要、且必须立刻执行五步的判据**，不把 B2 打成 FAIL 去逼扩锁。
- 058 已由映射内 PGM-043/041、BSM-007、GEN 成立。
- 059 不入队，故不采用未映射 870/871 作执行判据。
- 需要的是已有 ID 的映射补入建议，不是新写定义；建议全文保留。不搭售 PGM-058。

若未来确须把 PGM-057/059 补进 M009 映射：必须完整五步，**最早受影响模块按变更后全锁引用实算，不可预设只 M009**。本席现核：PGM-059 已映射 70 个模块，最早 **M002**（随后 M019、M028、M031…）；PGM-057 已映射 51 个模块，最早也是 **M002**。本席 B1 §7 写「补映射后最早=M009」在全锁引用口径下不成立，以本段更正；候选已指出 M002 亦引用，本席同意该更正。本席不写 lock/ledger，不偷改锁。

### 6. 轮次与收口边界
B3 累计 **3** 不得清零、不得自动加轮。B2 双 PASS 只定稿台账，仍受既有上限约束：若 058 定稿队列为是，调度**不得**自行执行第 4 次 B3；应按 V0.17 记 `BLOCKED-3ROUNDS` 交操作员决定是否另起修复轮。本席不提前写 ledger 终态。无 HTML 修改申请。

### 7. 分歧清单
无导致 FAIL 的分歧。同意 058 冲突/工程是/队列是与最小删除范围；独立同意 059 降观察不入队；同意本批不执行 LOCK-AMEND 但保留映射建议；更正最早模块预设。未漏并。未把 050 旧无缺陷或 043 历史写事件冒充本批列表问题。

**B2 PASS。**

## M009 B2 操作员第2、3项裁决后重新归并复核（Grok 乙席）
版本：V0.14
席位：Grok 独立审计席乙
阶段：B2 整批候选复核（不修复）
本席B2结论：**PASS**（只表示本批授权修复范围的归并/四分类/工程缺陷/修复队列/最小方案可执行，**不**表示模块 PASS，**不**表示整模块符合未改 HTML，**不**表示第1项手机号明文豁免已修，**不**授权超过一次的额外 B3）

本段不写独占 `CRITERIA-LOCK-SHA256:` 行。

### 0. 技能自核（V0.17 §6，按 B2 收窄；本机实列目录）
本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能；用户专项指令优先（禁第三席/子代理/MCP/fetch/commit/push/worktree/部署） |
| using-arc | Arc plugin `using-arc/SKILL.md` | 用户指令优先于 Arc；本任务是指定 B2 归并，不跑 ideate/implement/audit 子代理流 |
| i-have-adhd | `~/.claude/plugins/marketplaces/i-have-adhd/skills/i-have-adhd/SKILL.md` | 只借输出形状（先结论、编号、无寒暄）；本体 disable-model-invocation，不另造产物 |
| grilling / grill-me | mattpocock `productivity/grilling` | 只用追问树自取事实；作废等用户回答与 dispatch sub-agent |
| receiving-code-review | superpowers 同名 | 先对照 B1/锁/HTML/源码再接受或反驳，禁止表演性同意；B2 不实施修复 |
| verification-before-completion | superpowers 同名 | 先跑 sha256/源码/调用者/列类型命令再写 PASS |
| ponytail-review | ponytail 4.9.0 | 只列可删：不新造审计通道、不新造回填/CHECK/姓名脱敏 |
| codebase-design | mattpocock engineering | 接口深度：加解密藏在已有 merchantParameters/mapMerchant 缝，不扩大接口 |
| diagnosing-bugs / systematic-debugging | mattpocock / superpowers | 只读错误→调用链→列宽；不作无 ID 插桩 |
| writing-for-agents | mattpocock productivity | 本段写给下游 B3：完成判据同文件 |
| graft | `~/.claude/skills/graft/SKILL.md` | 只读定位 merchantParameters/mapMerchant/PiiCipher/contact_name/merchant.viewed |

B2 不适用、不调用：brainstorming / writing-plans（A）、tdd / test-driven-development / ponytail 修复本体（B3）、requesting-code-review（B4）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：
- `batch-grill-me`：mattpocock 技能树未找到该目录
- `ubiquitous-language`：未找到（`domain-modeling` 存在，V0.17 明确不用）
- `git-guardrails-claude-code`：存在于 mattpocock `misc/`，本席未改 git

未调 MCP、未派子代理/第三席、未 fetch/commit/push/merge/worktree/部署，未写业务代码/三 HTML/lock/sidecar/ledger/findings/questions。唯一写入：本指定 grok-raw 追加。

### 1. 输入核验
| 输入 | 指定 | 本席实算 | 结果 |
|---|---|---|---|
| self-raw 有效完整 B1 V0.12 | 截至 163739 bytes SHA256 `69b413e04d3b50acfb7a84ef8b6de2d57c00cd82a5449f708772f6ed07a70f19` | 前缀 163739 bytes 同 SHA；当前全文 171319 bytes（其后为席甲 V0.13/V0.14 B2 段，**不采为 B1 输入、不以席甲 V0.14 票为依据**） | 匹配 |
| grok-raw 有效完整 B1 V0.12 | 截至 301012 bytes SHA256 `0996c0167e39458ee87b7bcd4b9a9fd125a10be660a2d8e8054b44b5d2a8368a` | 前缀 301012 bytes 同 SHA；本文件追加前全文 313305 bytes SHA256 `f8c85942ac902db546e650e67a4807a8eff2d2eaa9c0fb7acb8e6e83a0fea5f7`（含本席历史 V0.13 B2 票，现被新裁决替代，**不改历史原文**） | 匹配 |
| 候选 findings.md | V3.24 全文 SHA256 `d044dbc707df525c0442e56e72c741688d01c100f29a4af3baed24f0a6dd4300`；顶部「当前 M009 B2：按操作员裁决重定第2、3项（未修复）」完整 26 项表 | 实算全文同 SHA；26 行 ID 无重复 | 匹配 |
| ledger | 用户写 V7.32；当前 IN_PROGRESS；B3 历史 3；最多额外 1 次至 4 | 实读顶部 `CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`、`LOCK-AMEND-COUNT: 1`；版本行 **V7.33**（与用户口称 V7.32 差一版，以文件为准）；模块表 M009 = `IN_PROGRESS` / B3 累计 **3** / 备注「操作员授权第2/3项一次追加修复；第1项不动。B2重新归并未定稿」 | 状态与授权匹配；版本号以实文件 V7.33 记录 |
| criteria-lock.md | 只读核，不改 | 实算 SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` 与 sidecar/ledger 顶行全等；版本 V0.11；M009 映射 87 条含 PGM-026/041/043、GEN-008/010/011，**不含** PGM-057/059 | 匹配 |
| 正本 | `/home/james/complex-management` main | `git rev-parse --abbrev-ref HEAD` = `main`；`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`（未 fetch） | 匹配 |
| 20 文件 | B1 冻结 SHA | 本席逐文件 sha256，DRIFT=0（与 B1 V0.12 表全等） | 匹配 |
| 三 HTML | 只读 | 枚举 `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；治理 `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` | 未改 |
| questions/ | 无新 M009 添加项 | `questions/` 下 M009* = 空 | 无 2.2 闸 |
| 控制 | 160-audit-prompt.md V0.17 | 实读 `.scratch/module-recheck-20260908/160-audit-prompt.md` SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 | 匹配 |

操作员新指令（优先于先前范围，但不批准改 HTML、不准篡改原始 FAIL、不准把手机号问题标已修）：
1. 先反对自行新增，之后明确「2 可以 3.应该加密 1不动」——1=删除手机号明文豁免（拒绝修改）；2=补商户编号/入口/筛选分页留痕（批准）；3=联系人姓名加密（明确要求）。
2. 随后对「允许按第2、3项重新复核，并追加执行1次修复」答「继续」。历史 B3=3 不清零，最多额外 1 次至 4。

### 2. 完整差集（禁止只审 2 行）
| 本轮 B1 / 历史意见 | 候选 V3.24 处置 | 本席独立结论 |
|---|---|---|
| CX-01 / GR-M009-B1-01 持权或 USB 主授权日常列表/写回返回两联系人手机号原文；`currentActorHasPermission` 不消费单次授权 | 并入 **M009-058**；分类仍冲突；工程是；**队列仅第2项**；第1项明文豁免按操作员「1不动」不修，原 FAIL 事实保留 | **同意**。事实仍成立：`PlatformController.java:59-68,71-95,108-133,276-295`。本席 V0.13 曾把删除豁免写入最小范围，**现被操作员第1项否决替代，不改 V0.13 原文**。不得把 058 改写成已符合 HTML |
| CX-02 `merchant.viewed` 非零审计，businessId=`all`，metadata 仅 count/total | 并入 **M009-058** 只修留痕：补 merchantIds、入口 `/api/v1/platform/merchants`、projectId、includeArchived、businessFormat（无筛选用空串）、实际 page/pageSize | **同意且源码核可执行**。`PlatformService.java:144-160`；`PlatformController.java:26-28,59-68` 映射确为 `/api/v1/platform` + `GET /merchants`；`AuditEventService.java:123` `Map.copyOf` **拒绝 null 值** |
| CX-03 / GR-M009-B1-02 主次联系人姓名直绑明文 | 新当前队列 **M009-059** 是；分类细化；工程是（用户已明确范围）；不采用 R-006 / 未映射 PGM-059 作执行判据 | **同意入队**。独立理由见 §4。本席 V0.13「降观察不入队」被操作员「3.应该加密」替代，**不改 V0.13 原文** |
| 本席 B1 §7 / V0.13 §5 LOCK-AMEND 建议（补映射 PGM-057/059） | 完整保留建议，本批不执行五步 | **同意保留、本批不执行**。见 §5 |
| OBS-01 createSpaceType 未先 `requireIdempotencyKey`，下游仍拒无键 | 保留观察 | 同意，不修 |
| OBS-02 营业时间外部 service 平台列表差异 | 保留，不审 M015/M038 | 同意 |
| OBS-03 `owner_person` / `legal_representative` 明文 | 与 059 同根因说明，不扩大加密列、不另开 ID | **同意且必须遵守**。候选最小边界已写不得改这两列 |
| OBS-04 V339/V394 权限种子为 058 第1项背景 | 保留未动 | 同意。第1项不动则种子也不在本批改 |
| 历史 24 项 038/039/040/041/031/042/043/044/045/046/030/047/018/013/054/055/056/048/049/050/051/052/053/057 | 完整 26 项表内保留；队列全部 **否**；分类/工程缺陷字段未翻成新缺陷 | **同意**。050 旧「无缺陷」仍不得豁免 058/059。043 是写事件对象 ID，不是列表 `merchant.viewed`。054–056 仍属 access/测试溢出 ID，本 20 文件无对应生产面，不重开 |
| 添加项 / 新 questions / HTML 修改 | 无 | 同意。本席 B1 亦无添加。不申请改三 HTML |

漏并检查：CX-01/02/03、GR-01/02、LOCK-AMEND 建议、OBS-01…04、050/043 去重边界、B3=3 与额外 1 次上限、第1项残留全部入账。无本席 B1 意见被丢掉。无新 ID。26 项表唯一。

当前队列：**是** = M009-058（仅第2项）+ M009-059；**否** = 其余 24。

### 3. 分类 / 工程缺陷 / 队列 / 最小范围

**M009-058**
- 分类：**冲突**（判定顺序冲突优先）。已映射 PGM-043 原文要求打开敏感页记下谁/何时/哪个页面/这一次哪些对象（含哪个商户），留痕无关闭开关；日常列表因权限码返回手机号原文仍与「页面强制脱敏、不提供关闭开关」矛盾。操作员「1不动」**不是**把该冲突改判为符合 HTML，也不是铁律 4 改文。
- 工程缺陷：**是**（两项事实都在：明文豁免 + 对象级留痕缺口）。
- 修复队列：**是（仅第2项）**。第1项明文豁免/单次布尔不消费/USB 日常明文 **禁止本批修改** `PlatformController`、`AuthorizationService`、`MerchantRecord.withMaskedPhones`、其它控制器。
- 第2项最小范围同意候选，并加可执行约束（源码核，不是扩需求）：
  1. 只改 `PlatformService.listMerchants` 已有 `merchant.viewed` 的 metadata；不新建事件名、不改 businessType、不把 businessId 从 `all` 改成必须单对象（对象集合放 metadata）。
  2. metadata 必须含：`merchantIds`（**本页** `result.items()` 的 id 列表，空页为 `[]`，禁止全库 id）、稳定入口字面 `/api/v1/platform/merchants`、`projectId`、`includeArchived`、`businessFormat`（无筛选用空串）、**实际返回** `page`/`pageSize`（取 `PageResponse.page()` / `pageSize()`，即仓储归一化后的值：页 `<1`→1，size `<1`→500，Jdbc 路径另 `min(size,500)`）、既有 `count`/`total`。
  3. **禁止**写入姓名/手机号。`projectId` 在 `currentProject()` 为 null 时必须用空串（或其它非 null 占位），**禁止** `Map.of`/`Map.copyOf` 放入 null——`AuditEventService.java:123` 会 NPE，会把「项目不存在→空页」的现网行为打成 500。
  4. 必要既有 `PlatformServiceTests`（M011，内存仓储+`InMemoryAuditEventRepository` 已能读 `AuditEvent.metadata`）覆盖不同页、筛选、空页、默认页归一化；自身版本递增。不测私有方法。
  5. 不改 Controller 映射/查询参数名，不改 HTML。

**M009-059** 见 §4。

**登记/技术性（队列否）**：048/049/050/052 细化无新缺陷；051 分页技术性无业务发明；053 `package-info.java` 仅包声明技术性无业务发明。057 已修队列否。同意保持。

无添加项，不发 2.2。无 HTML 修改申请。

### 4. 对 059 入队与「不必改锁」的独立判断（不同意则 FAIL）
本席 B1 对 GR-02 写过三分支：① 用已映射 PGM-043 七项姓名当在库加密义务；② LOCK-AMEND 补映射 PGM-059 后再入队；③ 870 主语不及商户联系人且拒绝扩映射 → 降观察。V0.13 选 ③。操作员现明确「3.应该加密」，且候选**不**把 R-006 或未映射 PGM-059 当执行判据，改用已映射 **PGM-026 + GEN-010 + GEN-008**。

本席对照锁原文与源码独立裁定：**选「入队且本批不 LOCK-AMEND」；不因此 FAIL。**

依据（2.3 类型 1+4，不是调度口径）：
- PGM-026 已映射，原文「建立商户档案…第一联系人和第二联系人」——两列是该档案的下位字段，分类仍是**细化**，不是新业务实体。
- GEN-010 已映射：「不得引入…明文秘密或敏感信息泄露」。主次联系人姓名是自然人姓名，与同记录已加密手机号同属联系人敏感字段；操作员本次把保护范围明确到这两列，消除本席 V0.13「冻结句未证成」的不确定。这是对已发现问题的处置，不是新写判据定义，也不是把 PGM-059 偷映射进 M009。
- GEN-008 已映射：前后端/库/DTO 对类型与约定须一致。同 `merchantParameters`/`mapMerchant` 对手机号走 `piiCipher.encrypt/decrypt`、对姓名直绑，类型与敏感处理不一致。V005:23 / V042:5 两列 `VARCHAR(120)`；AES-GCM 信封 `enc:v1:` + base64(12B IV + UTF-8 + 16B tag)，120 个汉字 ≈ 527 字符，**现列宽不够**，前向迁移改 TEXT 是类型一致性，不是新业务规则。
- **明确不采用**：PGM-059「客人和员工…一律加密存放」（锁内有定义、未映射 M009）；R-006 历史裁决；PGM-043 导出七项（展示/导出打码，不是本条存储义务）。这些只作观察/建议保留。
- 工程缺陷：**是**。队列：**是**。范围锁死 `merchants.contact_name` / `secondary_contact_name`。

与保留第1项的关系：**无冲突**。第1项是 Controller 响应层手机号明文豁免；059 是仓储层姓名加解密。候选写明不改 Controller/手机号/共享 `PiiCipher`。加密后 API 仍返回解密后的姓名（与今日 JSON 同形），不引入姓名脱敏开关，不把 871 日常界面句套到商户联系人。若把 059 做成「姓名也必须掩码」或「因此必须删除手机号豁免」，才与第1项冲突——候选没有这样做。本席若见到那种扩面会 FAIL。

### 5. LOCK-AMEND（本批不扩锁；必须则明确 FAIL）
**本批没有锁外仍必要、且必须立刻执行五步的判据。** 不把 B2 打成 FAIL 去逼扩锁。
- 058 第2项由已映射 PGM-043 / GEN-011 成立；第1项残留仍用原已映射 PGM-041/GEN-010/PGM-043 记账，不启用未映射 871 作新执行句。
- 059 执行句是已映射 PGM-026/GEN-010/GEN-008 + 操作员对本模块两列的明确处置，不是新 general ID，也不是把 PGM-059 写入 M009 映射。
- 补映射 PGM-057/059 的建议全文保留。若未来要把 870/871 变成 M009 映射内执行句：必须完整五步；最早受影响模块按变更后全锁引用实算（本席 V0.13 已核 PGM-059 最早 **M002**，不可预设只 M009）。本席不写 lock/ledger。

若候选改为「必须以 PGM-059 为 059 必要依据」或「把第1项标成已修」，本席会 FAIL。现行候选不是。

### 6. 最小方案是否够（源码 / 调用者 / 类型 / 迁移；不预设 PASS）
**058 够。** 生产列表审计只有 `PlatformService.listMerchants:151-159` 一处 `merchant.viewed`。Controller 入口稳定。`PageResponse` 字段名为 `page`/`pageSize`/`items`/`total`。Jdbc `listMerchantsPaged(String projectId, …)` 与接口 default 均做页归一化。不需要改仓储 SQL。

**059 够，且不得加 CHECK/回填。** 证据：
- 写：`graft`/`callers` 显示 `merchantParameters` 仅被 `saveMerchant:78-121` 调用；两姓名在 `:625`/`:631` 直绑，两手机号已 `piiCipher.encrypt`。其它 `UPDATE merchants`（`:193` 软删、`:1210` 存档理由、`:1243` 项目归属）**不碰姓名列**。
- 读：本模块全部商户 SELECT 走 `mapMerchant:710-739`；姓名 `:718`/`:724` 直读，手机号已 `decrypt`。分析/招商/进场等其它表的 `contact_name` 不在操作员第3项范围。
- 既有 `PiiCipher.encrypt/decrypt`：null/空/无 `enc:v1:` 前缀原样透传（`PiiCipher.java:32-55`，`PiiCipherTests` 已覆盖）。这是**现成**行为，不是本批新增历史兼容组件。AGENTS.md 第 8 条禁止新增回填/退化设计；旧行未走 `saveMerchant` 仍明文可读，**不得声称已全部加密**。
- **禁止**给姓名加 `LIKE 'enc:v1:%'` CHECK：存量明文 + 部分 UPDATE（归档/改项目）会在未重存姓名时整行重验失败。手机号 CHECK 先例不自动套用。ponytail：`delete:` 不造 CHECK/回填。
- 列类型：V005 `contact_name VARCHAR(120)`、V042 `secondary_contact_name VARCHAR(120)`。120 汉字信封超 120。候选前向迁移两列 TEXT（不删数据、不改旧迁移）必要且最小。工作区现有最大迁移 **V430**，候选拟 V431，**写前再核号**（含 origin/main 与未发布 V427 现场）。新文件可归因 059；属跨模块迁移落点，闸 3 按 ID 归因，不重开 M010 审计。
- 验证：`PlatformJdbcIntegrationTests`（M011，已有手机号密文/幂等/明文拒写先例 `:640-669`）用公开 `saveMerchant`/`findMerchantById` + 裸 SQL 断言两列 `enc:v1:`、读回一致、120 汉字、null/空串。`tools/verify-pii-encryption.mjs`（M053，现只登记两 phone 列 `:22-23`，断言 `addValue("col", piiCipher.encrypt(...))`）只加两姓名列并升版。跨模块测试/门禁有 055/056 先例，闸 3 归因 059。
- 不改：共享 `PiiCipher`、手机号、`owner_person`、`legal_representative`、其它模块姓名/客人生日、HTML、Controller。

**必要修复文件（B3，本席不修）**
1. `backend/src/main/java/com/complex/platform/governance/PlatformService.java` — 058 metadata；自身版本
2. `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java` — 058 留痕反例（M011）
3. `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` — 059 两列加解密；自身版本
4. 新前向迁移（拟 V431，写前核号）— 两列 `VARCHAR(120)`→`TEXT`
5. `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` — 059 密文/往返（M011）
6. `tools/verify-pii-encryption.mjs` — 登记两列并升版（M053）

验证（B3，本席本轮不跑会写库的测试）：`PlatformServiceTests`；`complex_test` 上 `PlatformJdbcIntegrationTests` 相关例（只 export 三个 DB 变量）；`node tools/verify-pii-encryption.mjs`。不跑生产库、不提交、不发布。

### 7. 轮次与 B5 边界
B3 历史累计 **3**，不清零。操作员「继续」授权**最多一次**追加修复至第 4 次。本票 PASS **不是**执行 B3，只表示定稿后那一次整批可修 058 第2项 + 059。
后续 B5 **不能**因 058 队列已修而隐去第1项残留：手机号日常明文豁免与单次不消费仍是未修冲突，整模块不得因本批修复被写成符合未改 HTML。059 不得声称存量已全部加密。
无 HTML 修改申请。本席不写 ledger 终态。

### 8. 分歧清单
无导致 FAIL 的分歧。不同意把 V0.13 的 059 观察结论继续当当前可执行方案；同意操作员新指令替代队列。不同意 LOCK-AMEND。不同意改 HTML。不同意把第1项标已修。

**B2 PASS。**

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M009 B1 Grok 独立审计原文（2026-09-22 现锁重开）

- 席位：Grok 独立审计席乙
- 阶段：B1 修复前整模块全量审计（LOCK-AMEND 后串行重开；不继承旧 PASS/FAIL）
- 报告自身版本：V0.15（追加；旧前缀 332492 字节 SHA256 `dad795a16e0599fdd3c48f96b5a0640e72b6422150db505ace2de2f23d0d8c2d` 原样保留）
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 20；冻结行数 2476；**本席实算** `git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` = **3375**
- 冻结判据：现锁映射 **88** 条（原 87 + 映射补入 PGM-059）
- 锁文件：`docs/audit-2026-09/plan/criteria-lock.md` V0.14；sidecar 同行
- 本席独立实算 SHA256：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 sidecar、ledger 顶部同值；本席未改任何锁/台账）
- LOCK-AMEND-COUNT：1（获批 HTML 同步，本席不执行五步）
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`；审计对象=主工作区磁盘现实
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：**GR-M009-B1-01**（单次授权查看未消费；与现 findings M009-058 残留同根，B2 归并）
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 fetch/commit/push/安装/部署、未写库集成测试、未派子代理、未调 MCP、未读本轮 self-raw、未问操作员、未进 B2

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际列出目录并读取 SKILL.md 后按收窄口径使用（grilling 自问自答、不等操作员、不派子代理）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用 Arc 全仓审计派子代理 |
| verification-before-completion | 同 superpowers | PASS/FAIL 前实跑命令 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗式结构；事实自取 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 深模块/缝：明文查看缝应走已有 `requirePermission` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 消费缺口：红环=布尔查询 vs 授权动作 |
| tdd / test-driven-development | 两套均存在 | B1 不写测试；只跑已有纯单元 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 禁止新增功能；明文分支复用已有消费 |
| writing-for-agents | 存在 | 本 raw 给下游 B2 读 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选；存在；本席无 git 写 |
| i-have-adhd | marketplace 存在 | 输出形态 |
| graft | `~/.claude/skills/graft/SKILL.md` 与仓内副本 | 先 graft 再全文；专项全读覆盖「不读全文」约定 |
| using-arc / audit | Arc 审计技能存在 | **按意图不走**：会派子代理/写 `docs/arc/audits/`，与双席及唯一写入冲突 |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。`grill-me` 与 grilling 同目录技能，按 grilling 收窄。划掉的 worktree/子代理/research 未调用。

## 1. 锁哈希与范围核验

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
cat docs/audit-2026-09/plan/criteria-lock.sha256
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
```

ledger 顶部同值。本席未写 ledger。

M009 映射行（锁 L6394）88 ID，缺定义 0。GEN 18 条无 HTML 摘录（通用工程判据）。其余 70 条来源路径+行号指向三 HTML；标题/`td.k` 行命中 70/70。PGM-043 / PGM-059 带铁律 4 批准注记。本席对内文 `<strong>`/`&quot;` 造成的探针假 DIFF 不升 LOCK-AMEND：获批关键句已在现 HTML 与锁摘录同时存在（见下）。

**三 HTML 现文（本席自读，不引用旧「禁止所有商户明文」）**

- 治理 HTML 文件版本 **V0.13**。
- PGM-059 现文 L871（锁 PGM-059 摘录同句）：「员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。**商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。**」
- PGM-043 现文 L680 末句（锁 PGM-043 摘录同句）：「**商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。**」
- PGM-041 现文 L656（锁 PGM-041 摘录同句，**原文保持未改**）：「**临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。**」
- PGM-059 L870：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」主语仍是客人与员工；商户联系人姓名加密是操作员第 3 项已修路径，不把 R-006 当新判据。

操作员「都批准修改」三项：① 授权/真实 U 盘最高权限可在档案列表及结果看完整手机号；② 该页例外不开放明文导出/打印；③ 656 原文保持、代码修单次消费。本席按现文核代码；**本次代码尚未修单次消费**。

## 2. 20 文件覆盖表（全文已读；行=`b'\\n'` 计数 = git `cat|wc -l`）

| # | 路径 | 行 | SHA256 |
|---|---|---:|---|
| 1 | `.../BusinessEntityRecord.java` | 17 | V0.01；四类主体记录 |
| 2 | `.../BusinessEntityBindingRecord.java` | 19 | 绑定记录 |
| 3 | `.../MerchantRecord.java` | 116 | V0.04；`withMaskedPhones` L89-96 |
| 4 | `.../MerchantDeleteDecision.java` | 9 | V0.01 |
| 5 | `.../JdbcPlatformRepository.java` | 1338 | V0.09；姓名/手机 `PiiCipher` |
| 6 | `.../PlatformController.java` | 296 | V0.02；L281-295 布尔明文 |
| 7 | `.../PlatformRepository.java` | 204 | V0.06 |
| 8 | `.../PlatformService.java` | 1179 | V0.12；`merchant.viewed` L152-165 |
| 9 | `.../package-info.java` | 1 | 仅 package |
| 10 | `dto/BusinessEntityBindingRequest.java` | 17 | |
| 11 | `dto/BusinessEntityRequest.java` | 26 | V0.03 |
| 12 | `dto/MerchantArchiveRequest.java` | 6 | |
| 13 | `dto/MerchantDeleteApprovalRequest.java` | 5 | |
| 14 | `dto/MerchantDeleteRequest.java` | 5 | |
| 15 | `dto/MerchantProfileRequest.java` | 41 | V0.02 |
| 16 | `dto/ProjectRequest.java` | 26 | V0.01；owner/address/scope |
| 17 | `dto/SpaceRequest.java` | 21 | |
| 18 | `dto/SpaceTypeRequest.java` | 16 | V0.01；注释=新建不可重复 |
| 19 | `dto/StatusChangeRequest.java` | 11 | |
| 20 | `dto/VenueBusinessHoursRequest.java` | 22 | |
| | **合计** | **3375** | 20 文件，无抽样 |

直接依赖（只读、不扩模块/判据）：`AuthorizationService`（`requirePermissions` 消费 / `currentActorHasPermission` 不消费 / `isMasterAuthority` / `isExplicitOrDailyGrant` / `requirePlatformScope`）；`PhoneMask.VIEW_PLAINTEXT_PERMISSION`；`PiiCipher` 无前缀透传；`ApiRequests.requireIdempotencyKey`；`IdempotencyService.execute` 转调之；`AccessService.changePositionTemplateLifecycle`（054 重判）；V339/V394/V431 迁移；`tools/verify-pii-encryption.mjs` 两姓名列。未把 SiteEntry/Inventory/Leasing/FeeRecipient 控制器扩进本模块（XM-008 他归属）。

## 3. 第一性原理（2.3 三问）

### 3.1 这段东西为什么必须存在？

删掉 20 文件，三 HTML 中「建立商户档案、运营主体、第一/第二联系人」（PGM-026）、「四类主体唯一绑定」（BEQR-143）、「配置可存档/删除要审批」（BSM-004）、「项目隔离」（PGM-041/BEQR-152）做不到。答得上 → 有业务上位。VenueBusinessHours 入口在本 Controller，实现在他文件，本模块只暴露 HTTP 缝。`package-info` 删了仍编译，走技术性登记（053）。

### 3.2 声称的行为有没有被实际执行验证过？

| 声称 | 证据 |
|---|---|
| 无 `pii.contact_phone.view` 列表见掩码 | `PlatformControllerTests` 既有例 L155-157；本席未单跑该例，源码 L285-294 `withMaskedPhones` |
| 持权列表见明文 | 本席实跑 `PlatformControllerTests#listingMerchantsReturnsPlaintextPhoneWithPiiPermission`：**Tests run: 1 Failures: 0**；该例 stub 的是 `currentActorHasPermission`，不是 `requirePermission` |
| USB 最高权限可不持码见明文 | 源码 `permissionGrant` L80-84 `isMasterAuthority` 直接成功；HTML L871 允许。类型 1+4 |
| 平台管理员身份不自动取得 | V394 将 `pii.contact_phone.view` 置 `requires_approval=TRUE`（且 V339 为 `high`）→ 非 daily；`isExplicitOrDailyGrant` 过滤 `role_platform_admin` 非日常种子。类型 4 |
| `merchant.viewed` 含对象/入口/筛选/页 | 本席实跑 `PlatformServiceTests` **70/70 零失败**，含 `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext` |
| 单次授权动作消费 | 本席实跑 `AuthorizationServiceTests#singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization`：**1/1 通过**（`requirePermission` 路径） |
| 有效集查询不消费 | 本席实跑 `effectivePermissionsQueryDoesNotConsumeSingleUseGrant`：**1/1 通过** |
| 姓名保存加密 | 源码 Jdbc L625/631 encrypt、L718/724 decrypt；V431 两列 TEXT；`verify-pii-encryption.mjs` L22-23。未跑写库 JDBC |
| 旧姓名未回填 | `PiiCipher` L32-35/52-55 无 `enc:v1:` 原样透传。**不声称已全部加密** |

### 3.3 哪个前提未经验证就当真？

- **已推翻**：现 HTML 仍禁止一切商户联系人明文。现文 L871 明确例外。
- **仍未验证为已实现**：持单次 `pii.contact_phone.view` 的人看完列表/写回后 grant 被消费。源码 `canViewPhonePlaintext` 只调布尔查询，**从不**调 `markSingleUsePermissionConsumed`。
- 未跑写库集成、未查生产库、未声称 Flyway 全绿。V427 未发布顺序门禁为环境红，不修。

## 4. 对抗式（grilling 自问自答）

❓ **Q1 无授权普通岗能否在列表拿完整手机号？**  
➡️ 不能。`maskPhonesForUnauthorized` 在 `canViewPhonePlaintext()==false` 时 `withMaskedPhones`。对齐 L871「其他人员仍只显示脱敏号码」。

❓ **Q2 单独授权且有效、或 USB 最高权限，列表和 create/update/archive 写回能否见完整号？**  
➡️ 能。列表 L68、create L75、update L85、assign L94、archive/unarchive/delete-request 均走同一掩码函数。对齐「档案列表和档案操作结果」。无单独 GET-by-id；不发明端点。

❓ **Q3 平台管理员身份是否自动取得该查看？**  
➡️ 日常桥只加 daily；本码 high+须审批，不是 daily；`role_platform_admin` 的 V339 种子被 `isExplicitOrDailyGrant` 滤掉。对齐「平台管理员身份本身不自动取得」。不把已应用 V339 当本模块可删缺陷。

❓ **Q4 单次授权看一次列表后还能再看明文吗？**  
➡️ **能。这是本轮成立的缺陷。** `currentActorHasPermission` L265-277 调 `permissionGrant` 后直接 `return true`，注释写明给脱敏布尔用、不消费。`requirePermissions` L41-61 才消费。列表查看是行使该权限的动作，不是菜单预览。与 PGM-041「单次消耗」冲突。操作员批准最小代码修消费、禁止把全局布尔改成消费、禁止新查看端点/新角色。

❓ **Q5 本模块是否开放明文导出/打印？**  
➡️ 20 文件无 export/print 入口。HTML 明确该页例外不授予导出/打印。本模块不因此发明导出口。他控制器持权明文属 XM-008 他归属，不扩。

❓ **Q6 查看是否留痕且含对象？**  
➡️ `listMerchants` L152-165：`merchantIds`/`entry`/`projectId`/`includeArchived`/`businessFormat`/`page`/`pageSize`/`count`/`total`；单元测试断言不含姓名手机号。第 2 项保持已修。

❓ **Q7 保存路径姓名是否加密、旧行是否回填？**  
➡️ 保存加密已在；旧行透传。第 3 项保持；不宣称全已加密。

❓ **Q8 能否用新全局查询或新角色修消费？**  
➡️ 禁止。最小缝：明文返回前调既有 `requirePermission("pii.contact_phone.view")`；无权限则掩码（catch `PERMISSION_DENIED`）。USB 走 master 短路、consume 集合空。不改 `currentActorHasPermission` 全局语义。

## 5. 顺从式：88 判据逐项

证据类型：1=HTML 原文 2=可执行产物 4=命令/源码行。无 3。

| ID | 本模块实现面 | 现判 | 证据 |
|---|---|---|---|
| GEN-001 | 有 | 对齐；053 空包已登记 | package-info；ponytail 不删隔离通道 `listMerchantsByBusinessEntity` |
| GEN-002 | 有 | 主路径可执行；消费缺口见 GR-01 | Controller 映射均存在；单元 70+1+1 |
| GEN-003 | 测试在 M011 | 既有单测能失败 | 掩码/明文/留痕/消费各有断言 |
| GEN-004 | 有 | 对齐 | 业态/状态/非负/主体存在 |
| GEN-005 | 有 | 列表有平台范围+项目谓词；明文查看未走消费闸 | L66-67 `requirePermission`+`requirePlatformScope`；GR-01 |
| GEN-006 | 有 | 对齐 | 绑定锁商户；删除占用；退出拒绝 terminated |
| GEN-007 | 有 | 写路径 executeWrite 幂等；空间种类 archive 等无幂等键 | createSpaceType 仍经 IdempotencyService.requireIdempotencyKey（OBS-01） |
| GEN-008 | 有 | 业态/状态中文错误；内部码英文 | 047 已修用户括注 |
| GEN-009 | 有 | 对齐 | 种类删除须存档审批 |
| GEN-010 | 有 | 手机加密+掩码；姓名保存加密；明文查看授权例外现合法 | Jdbc PiiCipher；GR-01 是授权消耗不是存储 |
| GEN-011 | 有 | 列表 viewed 已补对象上下文 | L152-165；单测 70/70 |
| GEN-012 | 有 | 分页 LIMIT/COUNT | 051 |
| GEN-013 | 无本模块构建脚本 | 无实现面 | — |
| GEN-014 | 迁移不在 20 文件 | V427/V431 只登记环境/已修姓名列 | 不改已应用迁移 |
| GEN-015 | 错误中文 | 对齐 047 | idempotency 英文句在 common，不扩 |
| GEN-016 | 注释 | 057 已改为新建不可重复 | SpaceTypeRequest L6 |
| GEN-017 | 分类 | 本席依序冲突→细化 | 见 findings |
| GEN-018 | 本报告 | 四类证据 | 本节 |
| BEQR-063 | 无 | 无实现面 | 酒吧高危动作 |
| BEQR-074 | 无 | 无实现面 | 隐私桌台 |
| BEQR-143 | 有 | 对齐 | 绑定三主体+经营对象 |
| BEQR-144 | 有 | 项目/公司多对多 | `business_entity_projects`（V427 环境） |
| BEQR-145 | 角色枚举 | `project_company` | requireBusinessEntityRole |
| BEQR-146 | 无运营公司业务 | 无实现面 | |
| BEQR-147 | 无收入记账 | 无实现面 | |
| BEQR-148 | 有 | 六类合作方式 | MERCHANT_COOPERATION_FORMS |
| BEQR-149 | 无职级写入 | 无实现面 | |
| BEQR-150 | 无终端 | 无实现面 | |
| BEQR-151 | 依赖 Access | 不扩 | |
| BEQR-152 | 有 | 项目隔离 | currentProject/requireCurrentProject |
| BEQR-153 | 无终端开关 | 无实现面 | |
| BEQR-154 | 有 | 项目六态 | PROJECT_STATUSES |
| BEQR-155 | 有 | 商户状态+退出缓冲 | 018 |
| BEQR-156 | 无合同状态机 | 无实现面 | |
| BEQR-157 | 无员工 | 无实现面 | |
| BEQR-158 | 无设备 | 无实现面 | |
| BEQR-159 | 无权限状态写入 | 无实现面 | |
| BEQR-166 | Jdbc 审批链持久化 | 登记 049 | 本 API 不驱动链 |
| BEQR-189 | 无管理费 | 无实现面 | |
| BSM-001 | 无 | 无实现面（全局原则在他模块） | 标题命中 HTML L505 |
| BSM-003 | 引导/权限分岗 | 部分：权限在 Access | |
| BSM-004 | 有 | 存档/删除审批 | archive/delete-request/approve |
| BSM-005 | 无前端 | 无实现面 | |
| BSM-006 | 无二维码 | 无实现面 | |
| BSM-007 | 依赖 Access | 平台管理员≠最高权限；删除须 master | approveDeleteMerchant `isMasterAuthority` |
| BSM-009 | 入口 | Controller `/api/v1/platform` | |
| BSM-028 | 项目档案三字段 | 013 已修 | ProjectRequest L17-19 |
| BSM-029 | 无 | 无实现面 | |
| BSM-072 | 商户范围 | requirePlatformScope 拒商户账号 | L67 |
| BSM-074 | 无 | 无实现面 | |
| BSM-114 | 入口隔离 | 历史摘录观察保留，不改 HTML | |
| BSM-115 | 无 | 无实现面 | |
| BSM-120 | 页脚日期 | 历史观察保留 | |
| PGM-001 | 中文 | 047 | |
| PGM-002–016 | 模块入口说明 | 无/弱实现面（本段是后台拓扑） | 不发明缺失模块 |
| PGM-017 | 空间项目 | 039 已修 | createSpace require projectId |
| PGM-018 | 多对多 | V427 表；环境红不修 | |
| PGM-019 | 主体职责 | 角色三值 | |
| PGM-020 | 档案字段 | 面积租金非负；项目三字段 | 046/013 |
| PGM-021 | 无 | 无实现面 | |
| PGM-026 | 有 | 档案+绑定+联系人 | 姓名保存加密 059 |
| PGM-027 | 边界 | 048 隔离查询承载 | |
| PGM-031 | 无经销 | 无实现面 | |
| PGM-035 | 无提成 | 无实现面 | |
| PGM-040 | 单功能授权 | 各端点独立 permission 码 | |
| PGM-041 | 有 | **单次消耗 FAIL** | L656 vs Controller L281-282 |
| PGM-042 | 无职级 | 无实现面 | |
| PGM-043 | 有 | 查看例外+禁止导出打印；留痕已修；消费缺口 | L680 现文 |
| PGM-044 | 无桌台 | 无实现面 | |
| PGM-048 | 商户侧全开放 | 本后台不是商户侧 | 无冲突 |
| PGM-059 | 有 | 明文例外现对齐；消费缺口 | L868-871 现文；本模块新映射 |
| PGM-060 | 无会员生物 | 无实现面 | |
| PGM-065 | 无 | 连接说明 | |
| PGM-066 | 中文/留痕已拍板 | 047/043 | |

## 6. 既有 26 项重判（当前值；旧 001–059 历史不当现值）

| ID | 分类保持 | 工程缺陷现值 | 队列建议 | 本席重判 |
|---|---|---|---|---|
| 038 | 冲突 | 历史是；现值否 | 否 | HTTP 列表/写入走 `currentProject`/`requireCurrentProject`；Jdbc paged `project_id=` |
| 039 | 冲突 | 否 | 否 | createSpace `requireText(projectId)` + 父空间同项目 |
| 040 | 冲突 | 否 | 否 | 生效绑定停用拒绝；三主体变更拒绝 |
| 041 | 冲突 | 否 | 否 | 未来 effectiveFrom → inactive 不投影 |
| 031 | 冲突 | 否 | 否 | 与 040/041 同分支 |
| 042 | 冲突 | 否 | 否 | unarchive 接受 archived/disabled |
| 043 | 冲突 | 否 | 否 | executeWrite businessId；驳回事件名 delete_rejected |
| 044 | 细化 | 否 | 否 | `completeSpaceTypeDeleteRequest` |
| 045 | 细化 | 否 | 否 | 占用排除已结束 inactive 历史绑定 |
| 046 | 细化 | 否 | 否 | 容量/面积/租金非负 |
| 030 | 细化 | 否 | 否 | 新建非空三主体存在+项目 |
| 047 | 冲突 | 否 | 否 | 服务层中文无英文枚举括注 |
| 018 | 冲突 | 否 | 否 | 拒绝直接 terminated |
| 013 | 细化 | 否 | 否 | ownerPerson/address/businessScope |
| 054 | 冲突 | 否 | 否 | AccessService L500 最高权限兼容生命周期；不扩模块 |
| 055 | 细化 | 否 | 否 | 他模块测试夹具；不扩、不重开 |
| 056 | 细化 | 否 | 否 | 同上 |
| 048 | 细化 | 否 | 否 | 隔离查询承载仍在 |
| 049 | 细化 | 否 | 否 | 审批链 JDBC 仍在 |
| 050 | 细化 | 否 | 否 | 手机加密+无权限掩码；不豁免消费缺口 |
| 051 | 技术性 | 否 | 否 | 分页无业务发明 |
| 052 | 细化 | 否 | 否 | DELETE 拒绝导向审批 |
| 053 | 技术性 | 否 | 否 | 空包 |
| 057 | 细化 | 否 | 否 | 注释已改为新建不可重复 |
| **058** | 冲突 | **是（仅单次消费）** | **是（B2 定稿）** | 见下拆项 |
| **059** | 细化 | 是（仅未回填旧行，不入队） | **否** | 保存路径已加密；旧明文透传；保持 |

**058 拆项（相对现 HTML，不引用旧禁令）：**

1. ~~日常界面一切商户明文均禁止~~ → **现文已批准授权/USB 在列表和结果看完整号。代码已按权限/USB 返回明文。此项不再是与现 HTML 的冲突。**
2. 对象上下文留痕 → **已修，保持。** 单测 70/70。
3. 导出/打印明文 → 本 20 文件无导出口；现文禁止该页例外授予导出/打印。**本模块无缺陷。** 他模块 XM-008 不扩。
4. **单次授权使用后须消耗** → **未修。冲突 PGM-041 L656。** `PlatformController.java:281-294` 用 `currentActorHasPermission`；`AuthorizationService.requirePermissions:41-61` 才 `markSingleUsePermissionConsumed`。操作员批准修代码、原文不改。优先既有消费机制。

**059：** Jdbc L625/631/718/724 + V431 TEXT + 门禁脚本两列。`PiiCipher` 无前缀透传。**不声称全部加密。** 队列否。

## 7. 原 OBS-01–04 重判

| OBS | 原意 | 现判 |
|---|---|---|
| OBS-01 | createSpaceType 控制器未调 `requireIdempotencyKey`，Service 仍强制 | **仍成立，观察不入队。** Controller L177-180 无 `requireIdempotencyKey`；`executeWrite`→`IdempotencyService.execute` L76 仍 `requireIdempotencyKey`。GEN-008 入口不一致，非绕过。 |
| OBS-02 | 列表不调 requirePlatformScope，商户直调 Service 可列他商户 | **已不成立。** Controller L67 与 Service `currentProject()` 均 `requirePlatformScope`。 |
| OBS-03 | V259 把 `merchant.profile.approve` 种给 role_platform_admin | **跨模块迁移观察，不入本队列。** |
| OBS-04 | `requireDate` / `readObjectMap` 死代码 | **已不成立。** 本 20 文件 0 命中。 |

## 8. 新 Findings（临时 GR；B2 分配永久 ID）

### GR-M009-B1-01 — 商户联系人明文查看走布尔权限查询，单次授权不消耗

- 文件:行：`backend/src/main/java/com/complex/platform/governance/PlatformController.java:281-294`；对比 `backend/src/main/java/com/complex/platform/access/AuthorizationService.java:41-61` 与 `:265-277`
- 原文：PGM-041「临时授权可设有效期或单次使用；权限的授予、收回、到期、**单次消耗**、失效全部留痕。」PGM-059「经单独授权且**查看权限有效**的人员」
- 正常触发：给员工一条 `pii.contact_phone.view` 且 `single_use=true` 的临时/模板授权；`GET /api/v1/platform/merchants`（或 create/update/archive 写回）。第一次见明文；再请求仍见明文，`consumed` 仍 false。
- 四分类：**冲突**（先于细化）。能指到 PGM-041 明文「单次消耗」。
- 冻结判据：PGM-041、PGM-059、GEN-005、GEN-011
- 证据类型：**1**（HTML L656/L871）+ **4**（上引源码）+ **2**（`requirePermission` 消费单测绿；Controller 明文例 stub 的是布尔查询）
- 工程缺陷：**成立**。建议 B2 并入/重开 **M009-058** 队列为是，范围收窄为消费，不回滚明文例外、不改 HTML。
- 最小修复（B1 不修；ponytail）：在 `maskPhonesForUnauthorized` 的明文分支调用既有 `authorizationService.requirePermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)`；无权限则掩码。禁止改 `currentActorHasPermission` 全局语义；禁止新端点/新角色/新权限码/新布尔。USB `isMasterAuthority` 在 `permissionGrant` 返回 null，consume 集合为空。必要改 M011 既有 Controller 测试 stub。不改其他姓名生日、不迁回填、不改三 HTML。
- 操作员已批准追加 1 次 B3 至累计 5；**本席 B1 不修**，队列须双席 B2 定稿。

无添加项。无 HTML 修改申请。无 LOCK-AMEND 需要（PGM-043/059 已同步现文）。

## 9. 跨模块 XM / 环境红（不扩、不修）

- XM-008 他控制器持权明文：M223/M249/M266/M282/M396，不进本队列。
- XM-010 他表姓名：按各模块 B1，不扩其他姓名/生日。
- XM-011 / XM-012：非本模块。
- 旧 V427 顺序门禁 / 未发布 `business_entity_projects`：环境红保留。
- V339 `role_platform_admin` 仍有 `pii.contact_phone.view` 种子行，运行时被过滤；不改已应用迁移。

## 10. 本席实跑命令（只读/纯单元）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  exit 0

git ls-files -z -- <20 files> | xargs -0 cat | wc -l
→ 3375  exit 0

# 前置：mkdir /tmp/m009-b1-grok-unit 且 assert 不在仓内
cd backend && mvn -o -Dtest=AuthorizationServiceTests#singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization,PlatformControllerTests#listingMerchantsReturnsPlaintextPhoneWithPiiPermission test -q
→ Tests run: 1+1, Failures: 0  MVN_EXIT=0

mvn -o -Dtest=AuthorizationServiceTests#effectivePermissionsQueryDoesNotConsumeSingleUseGrant,PlatformServiceTests test -q
→ AuthorizationServiceTests 1/1；PlatformServiceTests 70/70  Failures: 0  MVN_EXIT=0
```

未跑 `PlatformJdbcIntegrationTests`（写库）。未对生产库、未 `psql` 写。未 fetch。

## 11. 限制

- 主工作区脏；单元测试不作为全链绿。
- 未读本轮 `M009-b1-self-raw.md`。
- 未把 3375 vs 计划 2476 当缺陷（计划冻结行是切分时快照）。
- 不存在有条件通过。

## 12. 审计结论

**FAIL。**

整模块 20 文件/88 判据/三线已跑完。26 项中 24 项现值无待修；059 保持已修保存路径、不入队、不宣称全加密；058 第 2 项留痕保持；058 在现 HTML 下仅余 **单次授权不消费**（GR-M009-B1-01）。B3 累计仍 4；操作员批准本批追加至 5 须等 B2 双席定稿后由修复者执行。本席停止，不推进 B2。

## M009 现锁B2整批归并复核（Grok 乙席）
版本：V0.16（本段追加；上方 V0.15 及更早前缀 358208 bytes SHA256 `ded487f99430a0396fed1a18d48b03541c41a40390318ba8ed36f4139d758f65` 原样保留，不改写。本段不写独占锁哈希字段行）

- 席位：Grok 独立审计席乙。步骤：M009 现锁 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）；runbook V0.02（SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`）；criteria-lock V0.14。
- 本席 B2：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未 fetch/commit/push/merge/部署/写库。未改源码、测试、findings、ledger、lock、主 `.git`。席甲 self-raw 在完整 B1 V0.16 之后另有 B2 V0.17 追加，**不采为同意依据**。
- `receiving-code-review` 收窄：先核源码与本席隔离探针再接受或反驳，禁止表演性同意。本席 B1 V0.15 的后置 `requirePermission` 修法被同组反证推翻，本票按源码改判最小边界，不沿用 V0.15 修法原文。

此票只判断 findings.md 顶部 V3.47「当前M009现锁B2候选」相对本次两份完整 B1 的全批差集、2.1 四分类、当前工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.15 B1 **FAIL** 原文。

### 0. 技能自核（V0.17 §6，按 B2 收窄；本机实列目录）

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| receiving-code-review | 同 superpowers | 先核实再接受/反驳；B2 禁止 IMPLEMENT |
| verification-before-completion | 同 superpowers | PASS 前实跑隔离探针 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 明文许可缝复用已有 `requirePermissions` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=布尔查询 vs 按来源消费；同组先消费功能权 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 能复用既有一次核验就不改全局布尔、不新权限码 |
| writing-for-agents | 存在 | 本 raw 给下游 B3 读 |
| git-guardrails-claude-code | 存在 | 本席无 git 写 |
| graft | `~/.claude/skills/graft/SKILL.md` | 先定位授权/掩码符号再读精确行 |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。划掉的 worktree/子代理/research/Arc audit 未调用。

### 1. 输入版本 SHA（本席独立实算）

| 输入 | 口径 | 字节 | SHA256 | 核验 |
|---|---|---:|---|---|
| 本席完整 B1 V0.15 | 追加前 `M009-b1-grok-raw.md` 全文（含历史前缀+本次 B1） | 358208 | `ded487f99430a0396fed1a18d48b03541c41a40390318ba8ed36f4139d758f65` | 与候选顶部声明全等 |
| 席甲完整 B1 V0.16 | `M009-b1-self-raw.md` 在「## M009现锁B2归并 Codex」之前、末尾恰好一个换行 | 241724 | `8002dd01a9a103d55083e07e27f900c087b5942870f17c146029e6559804de06` | 与候选顶部声明全等 |
| 候选台账 | `docs/audit-2026-09/findings.md` V3.47 顶部「当前M009现锁B2候选」整 26 行 | 1257342（全文） | `23d89a7048f073b1867e6fe49f62ed00258ecd3bdcc6e0a26ed5758b6a6a8b51` | 表内 26 行、仅 058 队列是 |
| criteria-lock.md | 只读核，不改 | 891182 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` | 与 sidecar 正文、ledger 顶行全等；映射 88 unique 88，含 PGM-059 |
| HEAD | `git rev-parse` 未 fetch | — | `e456ba96c84185c8d39f18f548011daac48a6466` 分支 main | 与两席 B1 声明一致 |
| 20 文件 | `git ls-files -z -- <FILES> \| xargs -0 cat \| wc -l` | 3375 行 | — | 与两席 B1 一致；计划冻结 2476 不当现值 |

席甲当前 self-raw 全文 242316 bytes（SHA `4aa8b540195800a058224975ddaff7c57561f171bfff22b4ee31a057e60bfd33`）多出的是其 B2 V0.17 段，**不是本批 B1 输入**。

### 2. 差集（临时号 → 候选永久 ID）

| 来源 | 内容 | 候选处置 | 本席 |
|---|---|---|---|
| GR-M009-B1-01 | 明文走 `currentActorHasPermission`，单次不消费；建议后置 `requirePermission(phone)` | 并入 **M009-058**，不另分 ID；修法改为同次 `requirePermissions` | **同意并入**。缺陷事实成立。后置修法见 §4 **否决** |
| CX 现锁 B1V0.16 | 同一 058；另证同组功能权+电话权先消费使第一次脱敏 | 并入 058 最小边界 | **同意**。探针 B 复现 |
| 两席 038–057、059 | 现值无待修 / 059 保存已加密旧行不回填 | 26 行全覆盖，队列仅 058 | **同意**，不新开 ID |
| 两席 OBS 编号 | 历史语义不一致 | 按描述并列保留，不覆盖、不新增 finding | **同意** |
| XM-008/010/011/012 | 他模块既有输入 | 不扩大本模块 | **同意** |
| 添加项 | 0 | 无 2.2 | **同意** |
| LOCK-AMEND / HTML | 0 | 三 HTML 不改 | **同意** |

无漏并。无把 GR-01 另立新 ID。无把已修留痕/姓名保存加密重开。无把 059 旧行回填重开。

### 3. 26 项逐项（分类 / 当前工程缺陷 / 队列 / 最小修法）

V0.17：冲突→队列是；细化/技术性仅当前工程缺陷才入队；只登记→否。

| ID | 候选分类 | 缺陷 | 队列 | 本席 | 依据（B1 差集+本席现码） |
|---|---|---|---|---|---|
| 038 | 冲突 | 否 | 否 | PASS | 列表 `currentProject`；普通岗不能跨项目。历史修复保留 |
| 039 | 冲突 | 否 | 否 | PASS | createSpace 要求本项目+父空间同项目 |
| 040 | 冲突 | 否 | 否 | PASS | 普通路径不能改已有三主体 |
| 041 | 冲突 | 否 | 否 | PASS | 未来 effectiveFrom 不立即投影 |
| 031 | 冲突 | 否 | 否 | PASS | 与 040/041 同分支；不停用清空投影 |
| 042 | 冲突 | 否 | 否 | PASS | unarchive 接受 archived/disabled |
| 043 | 冲突 | 否 | 否 | PASS | 审计对象 ID/理由/驳回事件名 |
| 044 | 细化 | 否 | 否 | PASS | 本次申请结成 approved/rejected |
| 045 | 细化 | 否 | 否 | PASS | 占用排除已结束 inactive；未来 inactive 仍占用 |
| 046 | 细化 | 否 | 否 | PASS | 容量/面积/租金非负 |
| 030 | 细化 | 否 | 否 | PASS | 新建非空三主体存在+项目 |
| 047 | 冲突 | 否 | 否 | PASS | 用户 message 无英文枚举括注 |
| 018 | 冲突 | 否 | 否 | PASS | 普通路径拒绝直接 terminated |
| 013 | 细化 | 否 | 否 | PASS | ownerPerson/address/businessScope 可空已落 |
| 054 | 冲突 | 否 | 否 | PASS | AccessService 最高权限兼容；不扩模块重修 |
| 055 | 细化 | 否 | 否 | PASS | 他模块测试夹具；不放宽生产权限 |
| 056 | 细化 | 否 | 否 | PASS | 同上，不绕过受益人检查 |
| 048 | 细化 | 否 | 否 | PASS | 隔离查询承载，不因零调用删除 |
| 049 | 细化 | 否 | 否 | PASS | JDBC 审批链持久化，不发明角色链 |
| 050 | 细化 | 否 | 否 | PASS | 手机加密+无权限掩码；不豁免 058 |
| 051 | 技术性 | 否 | 否 | PASS | COUNT/LIMIT/PageResponse；无业务发明 |
| 052 | 细化 | 否 | 否 | PASS | 直接 DELETE 拒绝、走存档审批 |
| 053 | 技术性 | 否 | 否 | PASS | package-info 仅编译单元；无业务发明 |
| 057 | 细化 | 否 | 否 | PASS | SpaceTypeRequest 注释=新建不可重复 |
| **058** | 冲突 | **是** | **是** | **PASS** | 见 §4。并入 GR-01+CX，不回滚明文例外、不改 HTML |
| **059** | 细化 | **否** | **否** | **PASS** | Jdbc `merchantParameters`/`mapMerchant` 已 `PiiCipher` 两姓名列；V431 TEXT；门禁脚本已登记。`PiiCipher` 无前缀透传=旧行未回填。**批准边界，本批不重开**。不声称已全部加密。本席 B1 曾写「缺陷是（仅未回填）/队列否」；按操作员范围，**当前**工程缺陷列否，与队列否一致 |

051/053 技术性：无业务实体/枚举/状态/角色权限/价格/默认值，Grok 独立维持该分类。

### 4. 058 反证与最小边界（本席独立核实）

**现码（未修）**

- 七个返回 `MerchantRecord` 的入口先 `requirePermission(功能权)`，再两个 `maskPhonesForUnauthorized` 重载经 `canViewPhonePlaintext()` → `currentActorHasPermission("pii.contact_phone.view")`（`PlatformController.java:59-133,281-295`）。
- 七入口功能权：`listMerchants`=`merchant.profile.view`；`create/update/assign/archive/unarchive/delete-request`=`merchant.profile.manage`。`approveDeleteMerchant` 返回 `MerchantDeleteDecision`，不在七入口，保持原 `merchant.profile.approve`。
- `requirePermissions`（`AuthorizationService.java:41-61`）先对传入码逐个 `permissionGrant`（缺一即 deny），再 `putIfAbsent(source+":"+sourceId)` **按来源消费一次**。
- `markSingleUsePermissionConsumed`（`JdbcAccessRepository.java:1025-1046`）：模板按 **application.id** 整行 `consumed=TRUE`；临时授权按 **grant_group_id** 整组更新。`permission_code` 只进用量日志。模板 SQL `JOIN permission_template_items`（L876-886）使同一 `pta.id` 对多项各产生一条 effect——**同组多码是正常授权模型，不是极端巧合**。
- `isActive` 在 `consumed==true` 时直接无效（L520-522）。
- `currentActorHasPermission`（L265-277）只读、捕获 deny、**不**调用 mark。注释写明给脱敏布尔用。
- 列表 `merchant.viewed`（`PlatformService.java:152-165`）已含 merchantIds/entry/project/筛选/分页。第 2 项保持已修。
- 姓名保存加密保持已修（Jdbc L625/631/718/724）。

**本席隔离探针（/tmp，真实 `PlatformController`+`AuthorizationService`，仓储 mock；javac0/java0）**

| 探针 | 授权形状 | 结果 |
|---|---|---|
| A 单独电话单次 | view=ROLE 非常驻消费；phone=TEMPORARY_GRANT 同 source `once` | `first=18800000011 second=18800000011 consumed=false` — 可重复明文。本席 B1 原缺陷成立 |
| B 同组单次 | view 与 phone 均为 TEMPORARY_GRANT source `once` | `FIRST=188****0011 consumed=true`；第二次 `PERMISSION_DENIED merchant.profile.view` — **席甲反证成立** |
| C 候选同次核验 | 先只读问 phone，再 `requirePermissions(view, phone)`，用本次 boolean 决定明文、禁止消费后再查 | `first=18800000011 allow=true consumed=true requery=false`；第二次功能权拒绝 |
| D 无电话权 | 仅 ROLE view | `allow=false phone=188****0011`，功能权成功 |
| E 反例：始终双码 | 在 D 之后 `requirePermissions(view, phone)` | `没有操作权限：pii.contact_phone.view` — **若把电话权写成端点必选即新增角色权限，禁止** |

因此：本席 B1 V0.15「在 mask helper 明文分支后置 `requirePermission(phone)` / catch 脱敏」**漏 B 路径**——功能权先按来源烧掉整组，第一次合法持权也只能脱敏。候选改为复用已有 `requirePermissions` 同次核功能权+可选电话权，按来源一次消费，并把**本次**明文许可传入两 mask 重载，避免消费后 `currentActorHasPermission` 已是 false（探针 C `requery=false`）。

**最小边界（同意候选，下列为执行绑定，不是新 finding）**

1. 只改 `PlatformController` 七个商户响应入口+两 mask 重载；文件版本递增。必要适配现有 `PlatformControllerTests`（M011 验证面，归因 058）：单独电话单次须第一次明文且消费、同组单次第一次明文且整组消费、列表多条一次消费、写回、消费失败拒绝明文、无电话权仅原功能权+脱敏。不改其它生产模块。
2. 共享 helper **接收该入口原功能权码**（上列 view vs manage），不得把 manage 入口改成还要 `merchant.profile.view`，不得把 `pii.contact_phone.view` 做成端点必选（探针 E）。
3. 有电话权：`requirePermissions(原功能权, pii.contact_phone.view)` 一次；无电话权：只 `requirePermission(原功能权)` 并脱敏。USB/`isMasterAuthority` 仍走 grant=null、consume 集合空、本次明文=true。
4. 本次是否明文是 helper 的**请求内局部 boolean**，只传给私有 mask 重载。不是新业务开关、不是新权限码、不是查询参数、不是改 `currentActorHasPermission` 全局语义。禁止消费后再查布尔。
5. `requirePlatformScope`、幂等键、`merchant.viewed`、公共只读布尔、其它 Controller 的 PhoneMask 布尔查询、HTML、数据库、回填、其它姓名/生日列一律不动。
6. 列表按请求消费一次，不按行。

分类保持**冲突**（PGM-041 L656「单次消耗」；PGM-059「查看权限有效」；GEN-002/005/011）。工程缺陷是、队列是。不回滚获批明文例外。

### 5. 观察编号（并列保留，不新增 finding、不覆盖原文）

| 编号 | Grok 历史语义（B1 V0.15） | Codex V0.16 观察段语义 | 现值 |
|---|---|---|---|
| OBS-01 | createSpaceType 控制器未调 `requireIdempotencyKey`，Service/`executeWrite` 仍强制 | 同：幂等由既有 executor 负责 | 仍成立，观察不入队 |
| OBS-02 | 列表不调 requirePlatformScope（**已不成立**，L67 已有） | 营业时段仅委托已有服务，不扩模块 | **两义并列保留**；均无当前队列 |
| OBS-03 | V259 `merchant.profile.approve` 种给 role_platform_admin，跨模块 | owner_person/legal_representative 不在本次姓名加密范围 | **两义并列保留**；均无当前队列 |
| OBS-04 | requireDate/readObjectMap 死代码（**已不成立**，20 文件 0 命中） | V339/V394 权限种子不因管理员身份默认敏感 | **两义并列保留**；均无当前队列 |

候选顶部「原观察不覆盖」段与上表一致。禁止用后一组覆盖前一组编号。

### 6. 本席实跑（B2，只读；无写模式）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  exit 0

python 实算 grok-raw 358208 SHA ded487f9… ；self B1 V0.16 前缀 241724 SHA 8002dd01… ；findings 全文 SHA 23d89a70…
git ls-files 20 路径 | xargs cat | wc -l → 3375
/tmp/m009-b2-grok-probe：javac 0；java 0；输出见 §4 表 A–E
```

未跑写库 JDBC。未 fetch。未改业务文件。

### 7. 审计结论

无导致 FAIL 的分歧。26 项差集完整；唯一当前队列 **M009-058**；059 旧行未回填保持批准边界；OBS 按描述并列；无添加、无 HTML、无扩锁、无新 ID。

本席 B1 后置 helper 修法被正常同组单次路径否决，候选最小修法与探针 C/D/E 一致，**接受**。本地 boolean 不得做成新权限。

本席 B2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。B3 累计仍 4；操作员已批准本批追加至 5，须定稿后由修复者执行。步骤仍 B2，M009 `IN_PROGRESS`。不覆盖上方 B1 FAIL 原文。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M009 B1 Grok 独立审计原文（第6次发布修复前完整B1）

- 席位：Grok（固定外审席乙）。步骤：B1 修复前整模块全量三线审计。
- 报告自身版本：V0.17（指定路径已有 V0.01–V0.16；本次只追加。调用开始时全文 373312 bytes、SHA256 `ee1284e27e54c2855fe03f6b67e86caee30f00c1df530a610cccc5dca6138056` 全部原字节保留，不改写任何历史段或旧结论）
- 正本：`/home/james/complex-management` 分支 `main` 工作区。本席未 fetch / 未 commit / 未 push / 未改 hook 或 index。
- HEAD（只读）`e456ba96c84185c8d39f18f548011daac48a6466`；本地 `origin/main`=`d8f12c33766c2a0f39a45d5f4b8d66f6753804e4`（与派发新事实逐字相同）。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；`plan/runbook.md` V0.02 SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`。
- 计划：`plan/modules.md` V0.04 M009 FILES 20，标记 NORMAL，冻结行数仍写 2476；**本席实算** `git ls-files -z -- <20 paths> | xargs -0 cat | wc -l` = **3381**（以实跑为准）。
- 锁：`plan/criteria-lock.md` V0.14；映射 `M009 → [BEQR-063 … PGM-066]` COUNT=88 UNIQUE=88 MISSING=[]。
- 台账：findings V3.54 顶部现锁 26 项队列全否（B5-r5 收口后现值）。ledger 只读：`CRITERIA-LOCK-SHA256` 同行、`LOCK-AMEND-COUNT: 1`；模块当前 IN_PROGRESS；B3 累计 **5**，第 6 次未执行。本席不改 ledger/findings/lock/三 HTML/源码/测试/迁移/生成清单。
- 授权口径（操作员最新）：「批准 M009 追加一次发布修复」——**仅**迁移编号、配套生成清单及必要引用；不新业务；不动三份需求 HTML / 既有数据库业务数据 / 迁移历史。
- 禁止已遵守：无子代理 / Claude / MCP；未读本次 `M009-b1-self` 新正文；未读 M012 本次新 raw；未跑写库 / DDL / 测试；不创建 §7 以外产物。
- 源码阅读：20 文件逐字节 SHA 与本席 B5-r5 V0.02 表 **MISMATCH=0**，按派发允许复用该次全文阅读，**本段全部 88 项、26 ID、发布新事实均重新判断**，不继承 B5-r5 PASS。

本席步骤结论：**FAIL**（有当前工程缺陷，见 GR-M009-B1-01/02/03）。历史 26 项当前仍无工程缺陷。不是模块 PASS，不是 B2/B3。

---

## 0. 技能自核（V0.17 §6，按 B1 收窄）

已读并用收窄口径（不派子代理、不等操作员、B1 期间不修）：

| 技能 | 本机路径 | 本步用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检；用户/控制稿优先 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗/第一性追问结构；事实自读 |
| diagnosing-bugs | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 发布红的反馈环=现有 `--check` / `verify:migration-version-order`，本步不落测试 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | 先跑命令再写结论 |
| codebase-design | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 脱敏缝仍在 Controller；加密缝仍在 JDBC；编号缝在 Flyway 文件名 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；048 隔离通道不删；不把生成清单手改当「更短」 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错→复现；B1 不改码 |
| writing-for-agents | `.../mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | raw 结构 |
| graft | `/home/james/.claude/skills/graft/SKILL.md` | 已读；本步以 hash/cmp/`--check`/只读 SQL 为证据，不把卡片当锁 |

缺口（不冒充已调用）：本宿主未核到独立 `i-have-adhd` SKILL.md（按控制稿输出口径执行）；可选 `batch-grill-me` / `ubiquitous-language` / `git-guardrails-claude-code` 本步未调。`tdd`/`ponytail` 本体属 B3，本步不落测试、不修。划掉的子代理/worktree/research/MCP 不用。

B1 不适用：brainstorming / writing-plans（A）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts。

---

## 1. 锁哈希与范围（本席独立实算）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
```

| 源 | 值 |
|---|---|
| 本席实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| `plan/criteria-lock.sha256` | 同上 |
| `ledger.md` 顶部 | 同上 |
| lock 字节/行/版本 | 891182 / 6856 / V0.14；定义 401；M009 映射 88 |
| LOCK-AMEND-COUNT（只读） | 1 |

实算 = sidecar = ledger。第四值由调度在两席完整 raw 落齐后取本段首行。本席不改 sidecar/ledger。

三 HTML 只读 SHA（现文）：

| 文件 | SHA256 | 文件版本注释 |
|---|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` | 现文 `td.k`=197 |
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` | h2+h3=27+93 |
| `docs/platform-governance-map-for-operations-v0.1.html` | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` | `<!-- 文件版本：V0.13 -->`；h2+h3=18+48 |

锁摘录首行对现文对应行：**70/70 HTML 派生 ID 首行 EXACT**；GEN 18 条定义区全文。无 LOCK-AMEND 提案。无 HTML 修改申请。

---

## 2. 20 文件行数与 SHA（相对 B5-r5 逐字节）

命令：对 20 路径 `Path.read_bytes()` → `count(b'\\n')` + SHA256；对照本席 B5-r5 V0.02 表。

合计 **3381** 行，**MISMATCH 0**。因此复用该次全文阅读，本段重新判断。

| 行 | SHA256 | 路径 |
|---:|---|---|
| 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 | `.../governance/BusinessEntityRecord.java` |
| 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 | `.../governance/BusinessEntityBindingRecord.java` |
| 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 | `.../governance/MerchantRecord.java` |
| 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 | `.../governance/MerchantDeleteDecision.java` |
| 1338 | 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855 | `.../governance/JdbcPlatformRepository.java` |
| 302 | 53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6 | `.../governance/PlatformController.java` V0.03 |
| 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc | `.../governance/PlatformRepository.java` |
| 1179 | 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e | `.../governance/PlatformService.java` |
| 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 | `.../governance/package-info.java` |
| 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da | `.../dto/BusinessEntityBindingRequest.java` |
| 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 | `.../dto/BusinessEntityRequest.java` |
| 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 | `.../dto/MerchantArchiveRequest.java` |
| 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 | `.../dto/MerchantDeleteApprovalRequest.java` |
| 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 | `.../dto/MerchantDeleteRequest.java` |
| 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 | `.../dto/MerchantProfileRequest.java` |
| 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 | `.../dto/ProjectRequest.java` |
| 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa | `.../dto/SpaceRequest.java` |
| 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 | `.../dto/SpaceTypeRequest.java` |
| 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f | `.../dto/StatusChangeRequest.java` |
| 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 | `.../dto/VenueBusinessHoursRequest.java` |
| **3381** | | |

工作区相对 HEAD 仍有未提交治理改动（`git status --porcelain` 见 Jdbc/Controller/Service 等 MM）；**磁盘现值**即上表，与 B5-r5 同字节。`ProjectRecord.java` / `VenueBusinessHoursService.java` / `ProjectAssociationRequest.java` 不在本 20 文件，只作只读追证，不扩 XM。

本席未跑单测/JDBC（派发禁止写库测试）。行为证据=同字节源码重读 + 本轮只读命令。

---

## 3. 三项批准 — 仅当前冻结现文（不引旧禁令）

出处均为治理 HTML 现字节。锁：L656∈PGM-041；L680∈PGM-043；L871∈PGM-059。本席对现文 L656/L680/L871 与锁摘录逐字核对，命中。

**PGM656（单次消费原句保持，L656）**

> 临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。

**PGM871（授权商户联系人列表/操作结果可明文，L871）**

> 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。

**PGM680（不扩导出打印，L680 该句后段）**

> 商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。

本 20 文件无导出/打印入口。未发明导出口径、姓名生日展示规则或旧数据回填。

项目三字段现文（PGM-020 L443）：「记录项目状态、负责人、地址和经营范围。」`ProjectRequest.java:17-19` 仍为 `ownerPerson/address/businessScope` 可空。

---

## 4. 发布新事实（本席独立实算，类型 4）

未 fetch。本地 `origin/main` 与派发 SHA 一致。

### 4.1 迁移编号闸

`package.json`：`verify:migration-version-order` = `node tools/verify-migration-version-order.mjs`。该键在 `verify:delivery-gates` 链内；**不在** `verify:predeploy` 链（predeploy 字符串无 `migration-version-order`）。与 AGENTS.md §9「未挂 verify:predeploy」一致。

本席实跑：

```
npm run verify:migration-version-order
→ EXIT 1
MIGRATION_VERSION_ORDER_FAIL: origin/main 当前最大迁移号是 V430,本分支新增的迁移必须排在它之后:
- V427__m009_project_profiles_and_associations.sql(V427 不大于 V430) → 请重命名为 V431 或更后
```

磁盘相对 `origin/main` **仅 2 个新增文件**：

| 文件 | 本地 V | ≤ origin/main max 430 | 闸结果 |
|---|---:|---|---|
| `V427__m009_project_profiles_and_associations.sql` | 427 | 是 | **红** |
| `V431__merchant_contact_names_ciphertext.sql` | 431 | 否 | 闸不报（号已大于 430） |

`origin/main` 迁移 397 个、最大 **V430**（含 V428/V429/V430，**无 V427**）。本地 399 个、最大 V431。仓库内 **无 V432 文件**。

脚本提示「改名为 V431 或更后」**不能照抄**：V431 已被姓名扩容占用。派发拟改 **V432** 与本席实算一致。**执行前须再读当时 `origin/main` 最大号**（本席未 fetch，不能担保远端此刻仍是 430）。

V427 现文（不改业务句）：`projects` 加 `owner_person/address/business_scope`；`merchants.project_id`；`business_entity_projects`。这是已批准 M009-013/038 的既有迁移，不是新业务。

V431 现文：两列 `contact_name`/`secondary_contact_name` 改 TEXT。已批准 M009-059 的既有迁移；号已合法，仍未随 `origin/main` 发布。

### 4.2 生成清单

`--check` **不写仓库**（本席另在 `/tmp` 生成对照后删除，未改正本）。

| 命令 | 正本 exit | 候选 exit |
|---|---:|---:|
| `node tools/generate-domain-ownership-scope-audit.mjs --check` | **1** `docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations` | **1** 同文 |
| `node tools/generate-data-isolation-spec.mjs --check` | **1** `data isolation spec is out of sync with Flyway migrations` | **1** 同文 |

delivery-gates 顺序：domain-ownership-scope 在 data-isolation-spec **之前**。与「预检查首红是 domain-ownership-scope；后续 data-isolation 待机械同步」一致。

`/tmp` 生成器对照（已删临时目录）：

- CSV：335→336 表；**新增** `business_entity_projects,direct,project_id,...`；`merchants` 行 `scope_source` 从 `business_entity_id` 变为 `project_id`（生成器 `directColumns` 数组把 `project_id` 放在 `business_entity_id` 之前，属机械优先，不是新隔离规则）。
- data-iso 自动区：现文「最新 **V428** / **170** 张」；生成器现算「**V431** / **172** 张」。现自动区 **没有** `` `merchants` ``、**没有** `business_entity_projects`（170 行）。+2 = V427 的 `ADD COLUMN merchants.project_id` 使 merchants 进入解析集 + 新建 `business_entity_projects`。
- 正本 CSV SHA `1122f92a…` 与候选 **同字节**。
- 正本 vs 候选 data-iso：**同长度 42095 字节但哈希不同**，唯一正文差是自动区标题 V428 vs V430，表数仍写 170。候选手改标题 **不能** 使 `--check` 变绿。

编号若改为 V432，生成器「最新迁移」字段会变成 V432；表增量仍来自该文件内容，不是新表业务。

### 4.3 隔离候选 `.scratch/publish-m009-m011-20260922`

| 项 | 本席实算 |
|---|---|
| 生产 Java `backend/src/main/java/**/*.java` | 主 1199 / 候选 1199 / **同字节 1199 / diff 0 / only_main 0 / only_cand 0** |
| 本 20 文件 | 候选存在且与正本同字节 |
| 迁移 `V*.sql` | 399=399，文件名集合相同，**字节差 0**（含 V427 与 V431） |
| 候选 surefire `TEST-*.xml` | **10 个 XML、tests=299、failures=0、errors=0、skipped=0**（mtime 属候选历史，**不是本席本步跑的测试**） |

299 构成（只读 XML，非本席复跑）：

| 类 | tests |
|---|---:|
| `AuthorizationServiceTests` | 25 |
| `M006SecurityRegressionTests` | 39 |
| `M010RegressionTests` | 44 |
| `AuthServiceTests` | 11 |
| `MasterAuthorityServiceTests` | 12 |
| `PlatformControllerTests` | 19 |
| `PlatformJdbcIntegrationTests` | 24 |
| `PlatformServiceTests` | 70 |
| `SupervisorReviewServiceTests` | 44 |
| `LeasingServiceTests` | 11 |
| **合计** | **299** |

这是切片编译/该批用例，**不是全后端**。其中含 Auth / Authorization / 审批（SupervisorReview）/ JDBC。**不能**凭「299 全 PASS」主张单次权限消费语义可丢：

- `PlatformController.java:281-288` 仍 `currentActorHasPermission(pii)` 只读探权，有权则 `requirePermissions(功能码, PhoneMask.VIEW_PLAINTEXT_PERMISSION)`，无权则只 `requirePermission(功能码)`。
- `AuthorizationService.java:41-61` `requirePermissions`：**同一来源单次授权只 `markSingleUsePermissionConsumed` 一次**；USB/`isMasterAuthority` 的 `permissionGrant` 返回 null，不进 consume。
- `currentActorHasPermission:266-276` 走 `permissionGrant`，**不** `markConsumed`。
- 七个商户响应入口仍经 `requireMerchantPermission`（list/create/update/assign/archive/unarchive/delete-request）。`delete-approve` 仍只功能权、无电话明文。

最小实际依赖成立。编译绿 ≠ 消费语义可删。

### 4.4 库只读（docker exec `complex-postgres`，SELECT only，未 source env 文件）

**`complex_test`（旧未发布测试库）**

- `max(version)=431`，success 行 399。
- **已应用 V427 与 V431**（另有 V428–V430）。
- `merchants.project_id varchar(64)`；`contact_name`/`secondary_contact_name` **text**；`projects.owner_person/address/business_scope` text；`to_regclass('business_entity_projects')` 有表。

**`complex_test_m009_pub_upgrade_20260922`（已到 430 的升级库）**

- `max=430`，success 397。
- **无** 427/431/432 历史行。
- `merchants` **无** `project_id`；姓名列仍 `character varying`；`business_entity_projects` 为 NULL；`projects` 无三字段。

**`complex`**：`max=385`，不是本批发布验证面。

因此：V427→V432 **只改文件名** 对「已记 V427 的 `complex_test`」**不能**当成已解决衔接——Flyway 会看到已应用文件消失 + 新 V432 待执行，而列/表已在。派发禁止改 history、禁止删业务。验证面必须拆开，见 GR-M009-B1-04。

---

## 5. 第一性原理（2.3 三问）

1. **为什么必须存在？** 删掉本 20 文件，PGM-017/020 项目档案与三字段、PGM-026 商户档案与四类主体、BSM-004 存档删除、PGM-041/059 权限与联系人电话例外都做不到。删掉未发布的 V427 内容（不是删已批准业务），则已批准的 `merchants.project_id` / 三字段 / 公司-项目多对多在 **新库和已到 430 的库** 落不下去。package-info 无业务发明。VenueBusinessHours DTO 在列，实现属他模块。
2. **声称行为是否被实际执行验证？** 20 文件行为：本席未跑测试；以与 B5-r5 同 SHA 的源码重读 + Controller/JDBC/Service 关键缝现文为据，**不**把旧 surefire 或候选 299 当成本步测试。发布声称：`verify:migration-version-order` 本步 EXIT 1；两份 `--check` 本步 EXIT 1。
3. **未验证就当真的前提？** （a）远端 `origin/main` 可能在本席未 fetch 之后继续涨号——V432 只是对 **当前本地** max=430 且 V431 已占用的拟号。（b）未对升级库实跑 Flyway migrate（禁止写 DDL）。（c）V339 种子是否直授 `pii.contact_phone.view` 属 XM，不扩。（d）旧姓名行无 `enc:` 前缀仍透传，不声称存量已全部加密。

依据类型：HTML 原文（1）、本轮命令输出（4）、Flyway 脚本原文（3，仓库内 AGENTS/verify 脚本）、只读 `information_schema`/`flyway_schema_history`（2+4）。

---

## 6. 顺从式（88/88；假定实现意图正确）

映射 88 ID 均有 `### <ID> —` 定义。适用：D=本 20 文件有直接实现；B=边界；N=无实现面。**本步相对 B5-r5 的判断变化只在 GEN-014 / GEN-016**（发布闸现红）；其余 86 项因源码同字节、现文锚点仍命中，重新判断为无新实现缺陷。

| ID | 标题 | HTML/来源 | 本模块 | 本席本步 |
|---|---|---|---|---|
| BEQR-063 | 高危动作 | enum:111 | N/B 删审占用非酒吧全集 | 无新缺陷 |
| BEQR-074 | 隐私桌台 | :125 | N | 无新缺陷 |
| BEQR-143 | 四类主体 | :226 现文命中 | D 绑定+投影 | 无新缺陷 |
| BEQR-144 | 经营主体关系 | :227 多对多非行政链 | D | 无新缺陷 |
| BEQR-145 | 城市项目公司 | :228 | D project_company | 无新缺陷 |
| BEQR-146 | 运营公司职责 | :229 | B management_company | 无新缺陷 |
| BEQR-147 | 主要收入归属 | :230 | N | 无新缺陷 |
| BEQR-148 | 合作模式 | :231 六类 | D cooperationForm | 无新缺陷 |
| BEQR-149 | 职级 | :232 | N | 无新缺陷 |
| BEQR-150 | 终端十一端 | :233 | N | 无新缺陷 |
| BEQR-151 | 最终权限计算 | :234 | B AuthorizationService | 无新缺陷；消费语义不可因编译丢 |
| BEQR-152 | 项目与敏感数据权限 | :235 | D currentProject | 无新缺陷 |
| BEQR-153 | 终端开关优先级 | :236 | N | 无新缺陷 |
| BEQR-154 | 综合体项目状态 | :237 六态 | D PROJECT_STATUSES | 无新缺陷 |
| BEQR-155 | 商户/运营主体状态 | :238 退出缓冲 | D 拒直接 terminated | 无新缺陷 |
| BEQR-156 | 合同状态 | :239 | N | 无新缺陷 |
| BEQR-157 | 员工状态 | :240 | N | 无新缺陷 |
| BEQR-158 | 设备状态 | :241 | N | 无新缺陷 |
| BEQR-159 | 权限状态 | :242 | N | 无新缺陷 |
| BEQR-166 | 合同审批链 | :255 | B JDBC 持久化 | 无新缺陷 |
| BEQR-189 | 管理费方式 | :291 | N | 无新缺陷 |
| BSM-001 | 整体说明 | sys:505 | B | 无新缺陷 |
| BSM-003 | 系统通用能力 | :563 | B | 无新缺陷 |
| BSM-004 | 配置可存档可删除 | :566 | D | 无新缺陷 |
| BSM-005 | 分步引导 | :581 | N 前端 | 无新缺陷 |
| BSM-006 | 动态二维码 | :584 | N | 无新缺陷 |
| BSM-007 | 平台管理员日常权限 | :587 高敏不自动得 | B 电话明文 | 无新缺陷 |
| BSM-009 | 基础管理 | :596 | B | 无新缺陷 |
| BSM-028 | 主要功能 | :698 项目档案 | D 三字段 | 无新缺陷 |
| BSM-029 | 和谁有关 | :714 | B | 无新缺陷 |
| BSM-072 | 商户管理 | :1308 | D 平台 scope 拒商户号 | 无新缺陷 |
| BSM-074 | 和谁有关 | :1326 | B | 无新缺陷 |
| BSM-114 | 各类入口 | :1827 | B | 无新缺陷 |
| BSM-115 | 相互关系 | :1897 | B | 无新缺陷 |
| BSM-120 | 已确认规则 | :1957 | B 不把计划写成完成 | 无新缺陷 |
| GEN-001 | 必要性与最小性 | 通用 | D | 无新缺陷 |
| GEN-002 | 可执行正确性 | 通用 | D | 编号闸本步红，并入 GEN-014，不另开业务 ID |
| GEN-003 | 测试有效性 | 通用 | D | 本步不跑测试；不引用旧 XML 当本步绿 |
| GEN-004 | 信任边界 | 通用 | D | 无新缺陷 |
| GEN-005 | 授权与最小权限 | 通用 | D | 消费缝仍在；见对抗 |
| GEN-006 | 数据完整性 | 通用 | D | 无新缺陷 |
| GEN-007 | 并发与幂等 | 通用 | D | 写路径幂等；单次按来源一次 |
| GEN-008 | 接口与模式一致性 | 通用 | D | 无新缺陷 |
| GEN-009 | 错误处理 | 通用 | D | 无新缺陷 |
| GEN-010 | 安全与敏感信息 | 通用 | D 电话+姓名加密 | 无新缺陷 |
| GEN-011 | 可观测与审计 | 通用 | D merchant.viewed | 无新缺陷 |
| GEN-012 | 资源边界 | 通用 | D LIMIT 500 | 无新缺陷 |
| GEN-013 | 构建配置 | 通用 | N 本切片无构建文件 | 无新缺陷 |
| **GEN-014** | **数据库迁移正确性** | 通用 | **D 本步发布面** | **现缺陷 GR-01/04** |
| GEN-015 | 界面可用性 | 通用 | B 中文错误 | 无新缺陷 |
| **GEN-016** | **文档可验证性** | 通用 | **D 生成清单** | **现缺陷 GR-02/03** |
| GEN-017 | 业务来源分类 | 通用 | D | 本步新项均技术性，无添加 |
| GEN-018 | 第一性原理证据 | 通用 | D | 本报告 |
| PGM-001 | 整体定位 | gov:256 | B | 无新缺陷 |
| PGM-002 | 治理顺序 | :308 | B | 无新缺陷 |
| PGM-003 | 模块入口 | :339 | B | 无新缺陷 |
| PGM-004 | 综合体和集团 | :342 | D | 无新缺陷 |
| PGM-005 | 空间和点位 | :347 | D | 无新缺陷 |
| PGM-006 | 商户和主体 | :352 | D | 无新缺陷 |
| PGM-007 | 合同和准入 | :357 | N/B 拒改已有财税主体 | 无新缺陷 |
| PGM-008 | 员工和手机 | :362 | N | 无新缺陷 |
| PGM-009 | 权限和职级 | :367 | B | 无新缺陷 |
| PGM-010 | 终端开关 | :372 | N | 无新缺陷 |
| PGM-011 | 设备管理 | :378 | N 营业时间仅转发 | 无新缺陷 |
| PGM-012 | 商户侧 | :383 | B | 无新缺陷 |
| PGM-013 | 开放接口 | :388 | N | 无新缺陷 |
| PGM-014 | 留痕复核 | :393 | D | 无新缺陷 |
| PGM-015 | 楼宇运营 | :399 | N | 无新缺陷 |
| PGM-016 | 应急与隐私 | :404 | B→PGM-059 | 无新缺陷 |
| PGM-017 | 综合体和集团详 | :412 | D 对象带项目归属 | 无新缺陷 |
| PGM-018 | 物理项目与运营公司多对多 | :415 | D business_entity_projects | 实现在 20 文件+V427；发布编号见 GR-01 |
| PGM-019 | 各主体负责 | :424 | D 三角色白名单 | 无新缺陷 |
| PGM-020 | 主要功能 | :440 三字段 | D ProjectRequest | 无新缺陷 |
| PGM-021 | 业务意义 | :452 | B | 无新缺陷 |
| PGM-026 | 主要功能 | :492 档案+四类主体 | D | 无新缺陷 |
| PGM-027 | 业务边界 | :507 | D requirePlatformScope | 无新缺陷 |
| PGM-031 | 外部经销商 | :555 | N | 无新缺陷 |
| PGM-035 | 提成发放 | :599 | N | 无新缺陷 |
| PGM-040 | 权限和职级父段 | :643 | B 含 656 | 无新缺陷 |
| PGM-041 | 权限规则 | :647 含 L656 | D requirePermissions | 无新缺陷 |
| PGM-042 | 职级规则 | :664 | N | 无新缺陷 |
| PGM-043 | 外包与审计 | :673 含 L680 | D 无导出口 | 无新缺陷 |
| PGM-044 | 桌台挂单经销 | :688 | N | 无新缺陷 |
| PGM-048 | 商户侧全开放 | :749 | B | 无新缺陷 |
| PGM-059 | 个人信息保护 | :868 含 L871 | D 掩码/明文/加密 | 无新缺陷 |
| PGM-060 | 会员生物信息 | :879 | N 不扩姓名生日 | 无新缺陷 |
| PGM-065 | 连接关系 | :935 | B | 无新缺陷 |
| PGM-066 | 已拍板规则 | :951 | D 中文/留痕 | 无新缺陷 |

顺从结论：已批准的项目三字段 / 商户项目归属 / 姓名加密 / 单次电话查看消费在 **20 文件源码**上仍对齐现文。发布路径上 GEN-014/016 **不**满足「顺序确定、文档可从当前范围复现」。

---

## 7. 对抗式（假定实现错误；grilling 自问，事实自读）

| 攻击 | 结果 | 证据 |
|---|---|---|
| 无电话权列表读明文 | 掩码 | Controller `requireMerchantPermission` 无权分支 + `withMaskedPhones`；同字节 |
| 有权但消费失败仍出明文 | helper 在 service 前 `requirePermissions`；失败抛错不进 mask | `PlatformController.java:281-288`；`AuthorizationService:56-60` mark 失败 `deny` |
| 同组功能+电话同一 grant 第二次 | 按来源一次 consume | `requirePermissions` `source+sourceId` LinkedHashMap |
| 只查询布尔再另一次消费 | 明文 boolean 来自本次 helper 局部量，mask 不再重查；布尔方法不 mark | `:266-276` vs `:41-61` |
| 平台管理员自动明文 | Controller 无 platform_admin 豁免；日常桥不含该高危码时 `permissionGrant` 否→掩码 | L871 + 授权器 |
| USB 主授权明文且不误伤单次 | master 返回 null，不入 consume | `AuthorizationService:80-84` |
| 列表留痕反查姓名电话 | metadata 含 merchantIds/entry/projectId/筛选/页，无姓名电话字段 | `PlatformService.java:152-164` |
| 跨项目列表 | `currentProject` + JDBC `project_id` | Service:150-151, 411-414 |
| 普通改结算主体 / 未来绑立即投影 / 直接 terminated / 负数 | 源码闸仍在（同字节） | 不重复开 ID |
| 导出打印绕开掩码 | 20 文件无 export/print 入口 | grep 范围=20 文件 |
| 姓名仍 VARCHAR(120) | `complex_test` 已 TEXT；升级库仍 varchar——**正是未发布 V431 的升级面** | 只读 information_schema |
| 把生日/员工姓名拉进本模块 | JDBC 仅两列 encrypt；owner_person 明文 | `JdbcPlatformRepository.java:625-632,718-725` |
| **仅编译 299 PASS ⇒ 可删单次消费** | **不成立** | 299 非全后端；Controller/AuthorizationService 仍接线；派发明文禁止 |
| **只把 V427 改名为 V431** | **撞车** | V431 已存在；闸文案「或更后」 |
| **只改名、宣称 complex_test 已衔接** | **不成立** | 该库 history 已有 427；禁止改 history |
| **只改 data-iso 标题 V428→V430** | **仍红** | 候选已如此，`--check` 仍 1 |
| **对已有列的库把 V432 写成 IF NOT EXISTS 兼容** | **禁止** | AGENTS「不为存量数据做兼容」；无业务发明；不是本授权 |
| 并发双建空间类型 ON CONFLICT | 顺序已拒；竞态观察不立项 | 同 B5 OBS |

无把观察写成添加。无锁外新判据。

---

## 8. 历史 26 项逐项重判

台账当前值队列全否。本席不改台账。分类顺序：冲突→细化→技术性→添加。源码同字节 ⇒ 实现面相对 B5-r5 未变；本步仍逐项给当前判定。

| ID | 分类 | 工程缺陷 | 建议队列 | 冻结依据 | 当前事实 |
|---|---|---|---|---|---|
| M009-038 | 冲突（历史） | 否 | 否 | PGM-041 / BEQR-152 / GEN-005 | 五类列表 `currentProject`；写入校验项目；普通不能认领。V427 的 `merchants.project_id` 是其已批准落库，编号问题不回开本 ID |
| M009-039 | 冲突（历史） | 否 | 否 | PGM-017 / GEN-004 / GEN-006 | createSpace 要求当前项目；父空间同项目 |
| M009-040 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-005 | 生效绑非 active 变更拒绝 |
| M009-041 | 冲突（历史） | 否 | 否 | PGM-026 / GEN-006 / GEN-008 | 未来绑 inactive 不投影 |
| M009-031 | 冲突（历史） | 否 | 否 | PGM-026 / BEQR-143 / GEN-006 | 与 040/041 同闸 |
| M009-042 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-009 | unarchive 接受 archived 与 disabled |
| M009-043 | 冲突（历史） | 否 | 否 | BSM-004 / PGM-066 / GEN-011 | 审计对象 ID / rejected |
| M009-044 | 细化 | 否 | 否 | BSM-004 / GEN-006 / GEN-011 | 本次申请结清 |
| M009-045 | 细化 | 否 | 否 | BSM-004 / GEN-002 / GEN-006 | 占用排除已结束 inactive；未来未结束仍占。`JdbcPlatformRepository.java:221-224` |
| M009-046 | 细化 | 否 | 否 | PGM-020 / PGM-026 / GEN-004 / GEN-006 | 负值拒绝 |
| M009-030 | 细化 | 否 | 否 | PGM-026 / BEQR-143 / GEN-004 | 初建校验主体存在且在项目 |
| M009-047 | 冲突（历史） | 否 | 否 | PGM-001 / PGM-066 / GEN-008 / GEN-015 | 角色错误无英文枚举括注 |
| M009-018 | 冲突（历史） | 否 | 否 | BEQR-155 / GEN-006 | `PlatformService.java:1021-1024` 拒直接 terminated |
| M009-013 | 细化 | 否 | 否 | PGM-020 / BSM-028 / GEN-002 | 三字段已在 ProjectRequest/JDBC；**未发布编号不复用本 ID** |
| M009-054 | 冲突（历史） | 否 | 否 | BSM-004 / GEN-005 / GEN-008 | AccessService 不在 20 文件；不扩 XM |
| M009-055 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 / PGM-041 | 他模块夹具；不扩 |
| M009-056 | 细化 | 否 | 否 | GEN-003 / GEN-005 / GEN-008 | 同上 |
| M009-048 | 细化 | 否 | 否 | PGM-027 / BSM-072 | listMerchantsByBusinessEntity 仍在；零调用不是删除理由 |
| M009-049 | 细化 | 否 | 否 | BEQR-166 | JDBC 审批链仍在 |
| M009-050 | 细化 | 否 | 否 | PGM-041 / GEN-010 | 手机 PiiCipher+无权限掩码仍在；不覆盖 058/059 |
| M009-051 | 技术性 | 否 | 否 | GEN-012 | COUNT/LIMIT。无业务发明 |
| M009-052 | 细化 | 否 | 否 | BSM-004 | DELETE 拒绝、导向存档 |
| M009-053 | 技术性 | 否 | 否 | GEN-001 / GEN-017 | package-info 仅声明 |
| M009-057 | 细化 | 否 | 否 | BSM-004 / PGM-005 / PGM-020 / GEN-002 / GEN-008 / GEN-016 | SpaceTypeRequest 注释=新建不可重复固定启用 |
| M009-058 | 冲突（历史） | **否** | 否 | PGM-041 L656 / PGM-059 L871 / GEN-002 / GEN-005 / GEN-011 | 七入口 helper 同次核功能+可选电话、按来源一次；无电话权不碰电话权；`merchant.viewed` 含对象与查询上下文、不含姓名电话。持权/USB 明文符合 L871；无导出符合 L680 |
| M009-059 | 细化 | **否** | 否 | PGM-026 / GEN-010 / GEN-008 | JDBC 两列 PiiCipher；owner/法人未动。旧行无前缀透传，不声称存量全部已加密。V431 未发布是编号/发布面，不回开本 ID |

无添加项。无 HTML 申请。

---

## 9. 新 findings（GR 临时编号；B1 不修、不分配正式 ID）

### GR-M009-B1-01

- **分类**：技术性（无业务发明）
- **工程缺陷**：是
- **建议队列**：是（待 B2 定稿）
- **旧 ID 能否复用**：**不能**。M009-013/038 是三字段与项目归属的**业务实现**（当前缺陷否）。M010-009 属 M010，且事实是旧 409/399，不是「V427≤远端 430」。U10-001 等历史编号闸是他模块已闭环材料，ID 永不复用。
- **冻结依据**：GEN-014（迁移须顺序确定、可在目标库执行并保持模式与代码一致）；GEN-002；AGENTS.md §9 原文（类型 3+4）。不加新判据。
- **证据 / 原业务句**：
  - 命令：`npm run verify:migration-version-order` **exit 1**，唯一点名 `V427__m009_project_profiles_and_associations.sql`。
  - `origin/main` max=**430**（397 文件）；本地新增 V427≤430、V431>430。
  - 脚本在 delivery-gates，**不在** predeploy。
  - V427 业务句仍是已批准 013/038，不是新能力。
- **最小修法**：把**未发布**文件 `V427__m009_project_profiles_and_associations.sql` 重编号为 **`V432__…`（执行前再读远端最大号；若远端已>430 则取 max+1 且避开已占用的 V431）**。不改 SQL 业务语句、不改 V428–V430、不改 V431 内容、不改 Java/HTML/旧迁移 checksum。文件头「文件版本：V0.01」保持。必要引用：文件名/注释中的 V427 字样随文件名更新，避免文档假完成（GEN-016）。
- **无业务发明理由**：只改 Flyway 版本号以满足已冻结「本分支新增 V 号必须大于 origin/main 当前最大号」。不增加实体、状态、权限、校验或默认值。

### GR-M009-B1-02

- **分类**：技术性
- **工程缺陷**：是
- **建议队列**：是
- **旧 ID**：**不能复用**。无 M009 ID 覆盖 domain-ownership 生成清单。M008-007 是入口快照刷新，不是本 CSV。
- **冻结依据**：GEN-016（路径/版本/完成声明须可从当前范围复现，不得把计划或旧证据写成当前事实）。
- **证据**：`node tools/generate-domain-ownership-scope-audit.mjs --check` exit 1。现 CSV `merchants,direct,business_entity_id,...`；生成器对现迁移得到 `merchants,direct,project_id,...` 且新增 `business_entity_projects`。正本与候选 CSV **同红同字节**。
- **最小修法**：在 GR-01 编号落地后跑现有 `npm run docs:domain-ownership-scope`（生成器写该 CSV），再 `--check`。**禁止手编业务列/新 scope 类型。** merchants 的 `scope_source` 变为 `project_id` 是生成器 `directColumns` 优先序的机械结果，不解释成新隔离政策，也不因此改 Java 过滤。
- **无业务发明理由**：同步已有生成器与 Flyway 目录；不引入 HTML 没有的业务概念。

### GR-M009-B1-03

- **分类**：技术性
- **工程缺陷**：是
- **建议队列**：是
- **旧 ID**：**不能复用**。与 GR-02 同属「配套生成清单」，但是**另一产物、另一条 delivery-gates 后续红**，分列以便 B2 归因到不同文件。
- **冻结依据**：GEN-016
- **证据**：`node tools/generate-data-isolation-spec.mjs --check` exit 1。现自动区「V428 / 170 张」，无 merchants、无 business_entity_projects。生成器现算 V431 / 172。候选把标题改成 V430 仍 170 且仍红。
- **最小修法**：编号落地后跑现有 `npm run docs:data-isolation-spec`。只动 `<!-- DATA_ISOLATION_SPEC_AUTO_START -->`…`END` 区块。不手写业务表、不把计划写成已发布。
- **无业务发明理由**：同 GR-02。+2 张来自已批准 V427 的 `project_id` 列与 `business_entity_projects`，不是新表业务。

### GR-M009-B1-04

- **分类**：技术性
- **工程缺陷**：**否**（禁止改 history / 删业务 **不是** 一个要用 DDL 去「修掉」的缺陷）
- **建议队列**：**否**（约束条款，挂在 GR-01 的验证边界上）
- **旧 ID**：不能复用；也不是 013/059 回开。
- **冻结依据**：GEN-014 后半「可在目标数据库执行并保持模式与代码一致」；AGENTS 不为存量做兼容/回填。
- **证据**：
  - `complex_test` 已成功应用 **V427 与 V431**，列/表已在。
  - `complex_test_m009_pub_upgrade_20260922` 停在 **430**、无 427/431、无新列/新表。
  - 无 V432 文件。
- **必须写入修法/验证说明（否则会把只改名夸大成已衔接）**：
  1. **新库**：空库按 origin/main（至 430）+ 本地 V431 + 重编号后的 V432 顺序 migrate。
  2. **已到 430 的升级库**：以 `complex_test_m009_pub_upgrade_20260922` 这类 **history 无 427** 的库 migrate V431 再 V432。
  3. **旧未发布测试库 `complex_test`**：已记 V427。**不得** DELETE/UPDATE `flyway_schema_history`，**不得** 删业务行，**不得** 用 IF NOT EXISTS 把 V432 改成兼容补丁。该库 **不是** 「改名后应再跑一遍 V432」的验证面；它已经具备 427/431 的模式。
- **无业务发明理由**：只划分验证边界，不改产品行为。

---

## 10. 观察（不入队、不扩 XM、不覆盖历史 OBS 编号）

并列保留既有 OBS 两义（空间类型幂等委托 / 营业时段转发；owner/legal 不在批准加密范围；V339/V394 种子；死代码 requireDate 已无）。XM-008/010/011/012 不扩。

本步附加观察（编号不挤历史 OBS-01..04）：

- **P-OBS-A** V431 号已 >430，闸不报，但仍未随 origin/main 发布；与 V432 同批发布，不另开业务 ID。
- **P-OBS-B** 闸文案「改名为 V431」在 V431 已占用时是过时提示，修法以实算 max+空号为准。
- **P-OBS-C** `complex` max=385，不是本批发布验证面。
- **P-OBS-D** 候选 299 绿含 JDBC 姓名往返等，属候选历史 XML；本席本步未复跑，不当本步测试绿。
- **P-OBS-E** 空间类型 `saveSpaceType` ON CONFLICT 并发双新建仍可能变成更新。顺序已拒。不立项。

ponytail-review：20 文件无可删业务；048 隔离通道保留。`net: 0` 可删生产行。Lean already for 20 Java. 发布面该机械同步的清单不是「可删复杂度」。

---

## 11. 四分类汇总

| 类 | 本步 |
|---|---|
| 冲突 | 无新冲突。20 文件与三项现文未破。 |
| 细化 | 历史 013/038/058/059 等实现仍在；不回开。 |
| 技术性 | GR-01/02/03 有工程缺陷；GR-04 无缺陷有边界。无业务发明理由见上。 |
| 添加 | **无**。不新增能力、不改 HTML、不回填、不把生成器优先序写成新隔离政策。 |

Grok 本席复核：上述技术性分类成立。若甲席把编号/清单写成添加，本席不同意——HTML 已有项目归属与三字段；清单是生成器对已有迁移的投影。

---

## 12. 本席实跑命令（本步，非旧 XML）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  exit 0

git ls-files -z -- <20> | xargs -0 cat | wc -l → 3381
20 文件 SHA vs B5-r5 → MISMATCH 0

git rev-parse HEAD → e456ba96c84185c8d39f18f548011daac48a6466
git rev-parse origin/main → d8f12c33766c2a0f39a45d5f4b8d66f6753804e4

npm run verify:migration-version-order → EXIT 1（仅 V427）
node tools/generate-domain-ownership-scope-audit.mjs --check → EXIT 1
node tools/generate-data-isolation-spec.mjs --check → EXIT 1
（候选同两命令亦 EXIT 1）

生产 Java 主 vs 候选：1199=1199 同字节
迁移文件名/字节：399=399 diff 0

docker exec complex-postgres psql -U complex -d complex_test
  max=431；已有 427 与 431；project_id/TEXT/bep/三字段均在
docker exec … -d complex_test_m009_pub_upgrade_20260922
  max=430；无 427/431/432；无 project_id/bep/三字段；姓名仍 varchar

候选 surefire 只读：299/0/0/0（非本席复跑）
```

未跑：写库 JDBC、全后端、`docs:domain-ownership-scope`/`docs:data-isolation-spec` 对正本的写入、Flyway migrate、fetch、测试。

---

## 13. 限制（如实，不是有条件 PASS）

- 本席未 fetch，远端最大号以本地 `origin/main` 为准。
- 本席未跑本 20 文件测试；058/059 行为以同字节源码+授权器现文重判。
- GEN-014 的迁移文件物理上在 M010 FILES；本步按 M009 映射的 GEN-014/016 与操作员「M009 追加一次发布修复」授权审查发布面，不接管 M010 其它 SQL。
- 不宣称全链绿、不宣称只改名已衔接、不宣称存量姓名已全部加密。

---

## 14. 审计结论

范围 20/20 文件、3381 行、88/88 判据定义+HTML 70/70 首行 EXACT。三线完成。历史 26 项当前工程缺陷否。新问题 4 条 GR：01/02/03 为发布编号与配套生成清单的技术性工程缺陷；04 为验证边界（缺陷否、队列否）。无添加、无 HTML 申请、无扩锁、无中途 B2/B3。

**B1 本席 FAIL。**

未经 B2 双 PASS 不定稿、不进 B3。B3 累计仍 5。步骤仍 B1。不覆盖上方历史 PASS/FAIL 原文。合取与 ledger 由调度在两席完整 raw 落齐后处理；本席不写收口、不改 ledger。

## M009 第6次发布修复 B2整批归并复核（Grok 乙席）
版本：V0.18（本段追加；上方 V0.01–V0.17 前缀 413874 bytes SHA256 `3132b99553831987d9ed370ad7139e6ae9f3f36bf1e84847713a4cf0a3526ccb` 原样保留，不改写任何历史段或旧结论。本段不写独占锁哈希字段行）

- 席位：Grok 独立审计席乙。步骤：M009 第6次发布修复 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）；runbook V0.02（SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`）；criteria-lock 现文 891182 bytes，实算 SHA `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（sidecar 正文同值；ledger 顶行同值；本席 V0.17 B1 锁行同值。四值全等。映射仍 88 unique 88）。
- 本席 B2：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未 fetch/commit/push/merge/部署。未改源码、HTML、测试、DB、findings、ledger、lock、主 `.git`。未跑 DB/测试。未在 `/tmp` 写对照副本；哈希与现文核验均在正本 cwd 进程内完成。
- `receiving-code-review` 收窄：先核两份本次完整 B1、冻结 GEN 原文、现文件头/常量与候选最小修法，再接受或反驳；禁止表演性同意；B2 禁止 IMPLEMENT。
- 席甲 `M009-b1-self-raw.md` 在完整 B1 V0.18+V0.19/0.20 之后另有 B2 V0.21，**不采为同意依据**。

此票只判断 `findings.md` 顶部 V3.55「当前M009第6次发布修复 B2整批候选」完整 30 行，相对本次两份完整 B1 的全批差集、2.1 四分类、当前工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.17 B1 **FAIL** 原文。不把本票写成「台账已按本席意见改过」。未获双 PASS 之前没有 B3。B3 累计仍 5；操作员已批准第 6 次一次发布修复，须定稿后由修复者执行。不另增功能、判据、HTML 申请或 2.2。

### 0. 技能自核（V0.17 §6，按 B2 收窄）

| 技能 | 本机路径 | 本步用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员「无子代理/不修」覆盖通用派工 |
| receiving-code-review | 同 superpowers | 先核实再接受/反驳；B2 禁止 IMPLEMENT |
| verification-before-completion | 同 superpowers | PASS 前实算输入 SHA、现迁移号、生成器/规格头与常量；不把未跑的测试当绿 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗：版本补记是否越界、是否回开 013/038、063 是否被写成待修 |
| writing-for-agents | 同套件 | 本 raw 给下游 B3 读：步骤与完成判据同文件 |
| git-guardrails | 存在 | 本席无 git 写 |

B2 不适用、未调用：brainstorming / writing-plans / tdd / ponytail 落地 / requesting-code-review / 子代理 / worktree / MCP / Arc audit。`batch-grill-me`、`ubiquitous-language` 本机不存在，不冒称。graft 技能已读；本步是审计 raw 归并，源码只核候选点名的现版本常量，未用 graft 检索替代 B1。

### 1. 输入版本 SHA（本席独立实算）

| 输入 | 口径 | 字节 | SHA256 | 核验 |
|---|---|---:|---|---|
| 本席本次完整 B1 V0.17 | 本文件追加前全文（历史前缀+本次 B1；新增段起 byte 373312） | 413874 | `3132b99553831987d9ed370ad7139e6ae9f3f36bf1e84847713a4cf0a3526ccb` | 与候选顶部声明全等 |
| 席甲本次完整 B1 | `M009-b1-self-raw.md` 自原 byte 242316 的 V0.18，含 V0.19/0.20 同份补证，**不含** V0.21 B2 票；切片止于 269299（V0.20 段后恰好两个换行） | 269299（前缀） | `db48bc859cf0342f3f62fa84b5f3532a41e49201320fc9d5af8fe29c53b0a5c8` | 与候选顶部声明全等 |
| 席甲当前全文 | 含其 B2 V0.21 | 269953 | `1f9cb66283ba862a76a15a4dc14c6eda7c05de7913b4f4874c03c43b5c09b8a3` | 多出的是作者自审票，不是 B1 输入 |
| 候选台账 | `docs/audit-2026-09/findings.md` V3.55 顶部完整 30 行 | 1280366（全文） | `b94bacf10e1f58c2cb925e50c58ea7a22957cfebfa38e4c7418b7b0becc08929` | 表内 30 ID；队列是仅 060/061/062 |
| criteria-lock.md | 只读核，不改 | 891182 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` | 四值全等 |
| HEAD / origin/main | `git rev-parse` 未 fetch | — | `e456ba96c84185c8d39f18f548011daac48a6466` / `d8f12c33766c2a0f39a45d5f4b8d66f6753804e4` | 与两席 B1 声明一致 |
| 20 文件行 | `git ls-files -z -- <FILES> \| xargs -0 cat \| wc -l` | 3381 | — | 与两席 B1 一致 |

V0.19/V0.20 是席甲对同一份完整 B1 的补证（版本铁律同步说明 + 隔离库 Flyway 红证据），不是第二份 B1，不另开差集轮次。

### 2. 差集（临时号 → 候选永久 ID）

| 来源 | 内容 | 候选处置 | 本席 |
|---|---|---|---|
| GR-M009-B1-01 | 未发布 V427 ≤ origin/main 最大 430；闸 exit1；改名为当时空闲高号（现 432）；SQL 业务句与头 V0.01 保持 | **M009-060** 技术性 / 缺陷是 / 队列是 | **同意**。不复用 013/038 |
| CX-PUB-01 | 同一编号缺陷；建议新 ID 060；旧库 427/431 历史留存、独立库验证 | 编号面并入 **060**；旧库验证边界另列 **063 否** | **同意拆分**。与本席 GR-04 一致，不是把一条缺陷拆成两条待修 |
| GR-M009-B1-02 | domain-ownership CSV `--check` exit1；跑现生成器重生；禁止手编业务列 | **M009-061** | **同意** |
| CX-PUB-02 | 两份生成清单一并红，V0.18 曾建议单号 061 | CSV→**061**，规格→**062** | **同意按产物分列**。席甲原一条覆盖两文件，分列便于 B4 归因，不增加缺陷种类 |
| GR-M009-B1-03 | data-isolation 自动区 V428/170 仍红；只刷新 AUTO 块 | **M009-062** | **同意** |
| GR-M009-B1-04 | `complex_test` 已有 427/431，不能靠改名声称旧库再跑 432；缺陷否、队列否 | **M009-063** 技术性 / 否 / 否 | **同意登记约束、不入队** |
| 历史 26 ID | 两席本次均当前工程缺陷否、队列否 | 30 行中这 26 行均否 | **同意**。不回开 013/038/058/059 |
| 本席 P-OBS-A…E 及历史 OBS/XM | 不入队 | 「所有旧观察/XM保留」 | **同意**。不篡改、不另开 ID |
| 席甲发布依赖观察 | Auth/Authorization/审批/JdbcAccess 为现授权实现依赖 | 观察保留，不入队 | **同意**。299 绿与 1199 同字节是已有事实，不宣称它模块全审完 |
| 添加 / HTML / 扩锁 | 0 | 0 | **同意**。无 2.2 |

无漏并。无把 GR-01 并回 013/038。无把 GR-04 写成队列是。无把 P-OBS-A（V431 未随 main 发布）升为新业务 ID。

### 3. 30 项逐项（分类 / 当前工程缺陷 / 队列 / 最小修法）

V0.17：冲突→队列是；细化/技术性仅当前工程缺陷才入队；只登记→否。分类顺序冲突→细化→技术性→添加。引用已完成 B1 全量事实，不重做 88 项。

**历史 26 行（均否）**

| ID | 候选分类 | 缺陷 | 队列 | 本席 | 依据（B1 差集，不重审实现） |
|---|---|---|---|---|---|
| 038 | 冲突 | 否 | 否 | PASS | 项目隔离与多对多已落；V427 是其已批落库，编号问题不回开 |
| 039 | 冲突 | 否 | 否 | PASS | createSpace 当前项目+父空间同项目 |
| 040 | 冲突 | 否 | 否 | PASS | 普通路径不能改已有三主体 |
| 041 | 冲突 | 否 | 否 | PASS | 未来 effectiveFrom 不立即投影 |
| 031 | 冲突 | 否 | 否 | PASS | 与 040/041 同闸；停用不清空投影 |
| 042 | 冲突 | 否 | 否 | PASS | unarchive 接受 archived/disabled |
| 043 | 冲突 | 否 | 否 | PASS | 审计对象 ID/理由/驳回事件名 |
| 044 | 细化 | 否 | 否 | PASS | 本次申请结清 |
| 045 | 细化 | 否 | 否 | PASS | 占用排除已结束 inactive；未来 inactive 仍占 |
| 046 | 细化 | 否 | 否 | PASS | 容量/面积/租金非负 |
| 030 | 细化 | 否 | 否 | PASS | 新建非空三主体存在+项目 |
| 047 | 冲突 | 否 | 否 | PASS | 用户 message 无英文枚举括注 |
| 018 | 冲突 | 否 | 否 | PASS | 普通路径拒绝直接 terminated |
| 013 | 细化 | 否 | 否 | PASS | 三字段可空已落；**未发布编号不复用本 ID** |
| 054 | 冲突 | 否 | 否 | PASS | AccessService 不在 20 文件；不扩 XM |
| 055 | 细化 | 否 | 否 | PASS | 他模块夹具；不放宽生产权限 |
| 056 | 细化 | 否 | 否 | PASS | 同上，不绕过受益人检查 |
| 048 | 细化 | 否 | 否 | PASS | 隔离查询承载，不因零调用删除 |
| 049 | 细化 | 否 | 否 | PASS | JDBC 审批链，不发明角色链 |
| 050 | 细化 | 否 | 否 | PASS | 手机加密+掩码；不覆盖 058/059 |
| 051 | 技术性 | 否 | 否 | PASS | COUNT/LIMIT；无业务发明 |
| 052 | 细化 | 否 | 否 | PASS | 直接 DELETE 拒绝、走存档审批 |
| 053 | 技术性 | 否 | 否 | PASS | package-info 仅编译单元 |
| 057 | 细化 | 否 | 否 | PASS | SpaceTypeRequest 注释=新建不可重复 |
| 058 | 冲突 | 否 | 否 | PASS | 七入口同次核验+按来源一次消费；持权明文符合 L871。本批不重开 |
| 059 | 细化 | 否 | 否 | PASS | 两姓名列已加密读写；旧行不回填。V431 未发布是发布面，不回开 |

**本批 4 个新 ID**

| ID | 候选分类 | 缺陷 | 队列 | 本席 | 冻结依据（核原文） | 最小修法是否越界 |
|---|---|---|---|---|---|---|
| **060** | 技术性 | **是** | **是** | **PASS** | **GEN-014**「迁移须顺序确定、可在目标数据库执行并保持模式与代码一致」；从属 GEN-002（声明的编号闸须能执行且与声称一致）、GEN-013（delivery-gates 失败须显式）。AGENTS §9：本分支新增 V 号必须大于 origin/main 当前最大。不是新判据。 | 见 §4.1：纯重命名 V427→当时空闲高号（现 432），SQL 字节不改，头 V0.01 保持。**不越界** |
| **061** | 技术性 | **是** | **是** | **PASS** | **GEN-016**「路径/版本/完成声明须可从当前范围复现，不得把计划或旧证据写成当前事实」；从属 GEN-008（清单与迁移模式一致） | 见 §4.2：现生成器重生 CSV；仅既有 `file_version` 常量 V0.01→V0.02 及自身头 V0.04→V0.05。**不越界** |
| **062** | 技术性 | **是** | **是** | **PASS** | **GEN-016** 同上 | 见 §4.3：原工具刷新 AUTO 块；规格自身标题 V0.03→V0.04；生成器代码不改、手写条款不改。**不越界** |
| **063** | 技术性 | **否** | **否** | **PASS** | **GEN-014** 后半「可在目标数据库执行」；从属 GEN-006（失败不得留下靠篡改 history 的部分一致）。AGENTS「不为存量数据做兼容」 | 只登记验证边界，挂 060。不 repair、不删 history、不加 IF NOT EXISTS。**不越界** |

2.1：060/061/062/063 与三份 HTML 明文不冲突（编号/生成清单不是「删掉项目三字段」）。不能指成 HTML 下位细化——否则会把已修 013/038 业务实现改判未修。无业务实体/枚举/状态/角色权限/校验/价格/流程/默认值发明 → **技术性**。本席独立维持该分类。若写成添加，本席否决（HTML 已有项目归属与三字段；清单是生成器对已有迁移的投影）。

### 4. 版本铁律补记是否越界（用户点名核验）

操作员说明：这不是修改本席原 finding，是补遵守文件版本要求；若不同意须指出实际越界项。本席对照 V0.17 B1 原文、冻结 GEN、现文件，逐项如下。**无实际越界。**

#### 4.1 060：纯重命名，头 V0.01 保持

本席 GR-01 原文：把未发布 `V427__m009_project_profiles_and_associations.sql` 重编号为 V432（执行前再读远端最大号；避开已占用 V431）；不改 SQL 业务语句；文件头「文件版本：V0.01」保持。

本步只读现文：该文件头确为 `-- 文件版本：V0.01`；正文无 `V427`/`V432` 字样（注释只写已批 013/038）；本地迁移 399 个、有 427 与 431、**无 432**；`origin/main` 最大 **430**、无 427。与两席 B1 命令事实一致。本席 B2 未重跑闸脚本/Flyway。

纯改名未改内容，不递增 SQL 头，符合版本铁律「更新文件**内容**才前进版本」；也符合 GEN-014（只修执行序号，不改模式语句）。不改 428–430、不改 431、不开 outOfOrder/ignore。**不越界。**

本席 B1 §13「GEN-014 的迁移文件物理上在 M010 FILES」是指 Flyway 目录在冻结计划中列入 M010 FILES，且本步不接管 M010 其它 SQL；不是把 V427 改判为 M010 缺陷。冻结 M010 仅 005/048/318/354。候选不借他模块轮次，本席同意。不另开 ID。

席甲 V0.20 Flyway 红证据（隔离库先迁至 430 共 397 条成功，再加载现主目录 migrate exit1，文案 `Detected resolved migration not applied to database: 427`；未开 outOfOrder/ignore；原 `complex_test` 未改）：本席 B2 不重跑 DB。该输出与本席 B1 §4.4 只读状态（该库 max=430、无 427/431/432、无 `project_id`/bep/三字段）合取，作为 060 的实际红证据采信。不把该隔离库写成新产物类别。

#### 4.2 061：生成器只同步既有版本常量

本席 GR-02 原文：编号落地后跑现有生成器写 CSV；禁止手编业务列/新 scope 类型；`merchants.scope_source` 变为 `project_id` 是 `directColumns` 数组把 `project_id` 放在 `business_entity_id` 之前的机械优先，不是新隔离政策。

现文：`tools/generate-domain-ownership-scope-audit.mjs:1` 头 `V0.04`；`:204` 已有 `file_version: "V0.01"`；CSV 已有 `file_version` 列，335 数据行皆 V0.01；`merchants` 现为 `direct,business_entity_id`；无 `business_entity_projects` 行。与 B1「335→将多 1 表、merchants 机械改 project_id」一致。

版本铁律（操作员 AGENTS 既有原文，非本批新判据）：更新文件内容则该文件自身版本末位 +1。CSV 重生后内容变，必须升 `file_version`。该列由生成器常量写出，故只改该既有常量 V0.01→V0.02，否则只能手改 335 行（更大且违反「禁止手编」）。生成器源码因此被改一行常量，自身头 V0.04→V0.05。算法、`directColumns`、`inheritedRules`、列结构、`status` 明确不改。

这是执行已同意「跑现有生成器」时对 AGENTS 的机械遵守，不是新字段、不是新隔离政策、不改 GR-02 缺陷事实。席甲 V0.18 已写「更新已有文件版本」；V0.19 只把该句收成现常量的两位递增。**不越界。**

#### 4.3 062：自动区机械刷新 + 规格自身标题

本席 GR-03 原文：只动 `<!-- DATA_ISOLATION_SPEC_AUTO_START -->`…`END`；不手写业务表。现文：规格第 1 行标题 **V0.03**；自动区「最新迁移 **V428**，**170** 张」；无 `` `merchants` ``、无 `business_entity_projects`。生成器 `tools/generate-data-isolation-spec.mjs` 头 V0.01，候选写「生成器代码不改」——与「只跑原工具」一致。

跑生成器后规格**文件内容**变（自动区将变为换号后最新 V432 / 172 张）。AGENTS 要求该文件自身版本 +1。标题 `V0.03→V0.04` 是该文件自身版本，位于 AUTO 块外，但不是 §1/§2 手写隔离语义或回填策略。生成器不改，避免把标题政策写进工具。

「只改自动区」约束的是业务条款与表清单不得手写；自身标题递增是同一被改文件的版本维护，归属 062，不另开 ID。**不越界。**

候选「仅标题改 V430 仍红」是本席 B1 §4.2 已核实事实（候选同长不同哈希、表数仍 170、`--check` 仍 1）。本席 B2 不重跑 `--check`。

#### 4.4 063：否 / 否，不是修法扩大

本席 GR-04：缺陷否、队列否；验证面拆开；（1）空库全迁移；（2）history 无 427 的 430 库升级；（3）原 `complex_test` 已记 427，不得 DELETE/UPDATE `flyway_schema_history`、不得删业务、不得把 V432 写成 IF NOT EXISTS。该旧未发布库不是改名后应再跑 432 的目标。

候选 063 全文即该约束的正式 ID，工程缺陷否、修复队列否。不把「旧库无法靠改名衔接」做成要修的 DDL。**不越界。** 独立库 `complex_test_m009_pub_upgrade_20260922` 已由根/席甲创建并跑到 430（397 条），本席 B1 只读确认过其状态；本 B2 不重连库。

若出现以下任一，本席将改判 FAIL（当前均未出现）：把 013/038 改回队列是；把 063 改成队列是并 repair/删 history；改 SQL 业务句或伪增 V427 文件头；改生成器分类算法/列/status；手写安全规格业务条款；把生成器优先序写成新隔离政策；新增 HTML/锁外判据；把观察升为新业务 ID。

### 5. 观察（全保留，不入队，不覆盖历史编号）

并列保留：历史 OBS 两义（空间类型幂等委托 / 营业时段转发；owner/legal 不在批准加密范围；V339/V394 种子；死代码 requireDate 已无）。XM-008/010/011/012 不扩。

本席 V0.17 附加观察原编号保持：

- **P-OBS-A** V431 号已 >430，闸不报，但仍未随 origin/main 发布；与 V432 同批发布，不另开业务 ID。
- **P-OBS-B** 闸文案「改名为 V431」在 V431 已占用时过时；修法以实算 max+空号为准。
- **P-OBS-C** `complex` max=385，不是本批发布验证面。
- **P-OBS-D** 候选 299 绿属候选历史 XML；本席 B1 未复跑，本 B2 亦不跑测试。
- **P-OBS-E** 空间类型 `saveSpaceType` ON CONFLICT 并发双新建仍可能变成更新。顺序已拒。不立项。

席甲发布依赖观察保留：B5 须核候选相对 origin 的已有 Auth/Authorization/审批/JdbcAccess 生产差异及相关测试是当前授权实现的直接依赖；不为删依赖而改 Java。1199 生产 Java 与主工作区同字节、此前 clean 299 PASS 是已验证事实，不宣称其它模块全审完或旧 PR 全部已清。

过程偏差记录（不升缺陷）：本席 B1 曾在 `/tmp` 生成对照后删除；不是正本写入，但未遵守本轮「只许正本 `.scratch`」的运行时落点。本 B2 未再写 `/tmp`。不把 B1 说成全程无临时写入。

### 6. 本席实跑（B2，只读；无写模式、无 DB、无测试）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  exit 0

python 实算：grok-raw 追加前 413874 SHA 3132b995… ；self B1 前缀 269299 SHA db48bc85… ；findings 全文 1280366 SHA b94bacf1…
git rev-parse HEAD → e456ba96c84185c8d39f18f548011daac48a6466
git rev-parse origin/main → d8f12c33766c2a0f39a45d5f4b8d66f6753804e4
git ls-files 20 路径 | xargs cat | wc -l → 3381
origin/main 迁移最大 V430（无 V427）；本地 399 文件最大 V431；V432 不存在
V427 头 V0.01，正文无 V427/V432 字样
generate-domain-ownership-scope-audit.mjs 头 V0.04，file_version 常量 V0.01
CSV 335 数据行；merchants 现 business_entity_id；无 business_entity_projects
data-isolation 规格标题 V0.03；自动区 V428 / 170 张
generate-data-isolation-spec.mjs 头 V0.01（062 不改）
```

未跑：`verify:migration-version-order`、两份 `--check`、Flyway、JDBC、全后端、fetch、任何写库。编号闸与 `--check` 红、隔离库 Flyway exit1，均引用已完成 B1 命令输出。

### 7. 审计结论

无导致 FAIL 的分歧。30 项差集完整：26 旧 ID 均否；GR01/CX01→060 是；GR02/CX02CSV→061 是；GR03/CX02规格→062 是；GR04/CX01旧库边界→063 否。三项队列是，全部技术性，无业务发明。版本补记是操作员 AGENTS 对已同意机械修法的文件自身版本维护，不是改本席原 finding，无实际越界。观察全保留。无添加、无 HTML、无扩锁、不复用 013/038。

本席 B2：**PASS**

未经双 PASS 不定稿、不进 B3，更不称模块 PASS。B3 累计仍 5。步骤仍 B2。不覆盖上方 B1 FAIL 原文。不改 findings/ledger。合取由调度在两席本批 B2 落齐后处理。
