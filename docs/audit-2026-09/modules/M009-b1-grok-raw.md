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
