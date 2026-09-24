CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# XM-D029 B5-r1 席乙全量重审

版本：V0.01
席位：Grok 独立只读外审席乙，grok-4.7/xhigh
时间：2026-09-24T00:03Z 测试结束，随后落盘
正本：/home/james/complex-management main 工作区。范围内 32 个文件与 origin/main（4aa318a5）加上冻结 diff /home/james/audit-tmp/xmd029_b3r1.diff 逐字节相同。该 diff 65541 字节，SHA256 1fa24e3a4dcd8e8dc2cc700d4c7801f1c18a92382e8b29e925c5e6cc19ed8eb4，git apply 后 14 files changed, 336 insertions, 100 deletions。
锁：docs/audit-2026-09/plan/criteria-lock.md V0.14。本行哈希由本席在落盘前执行 sha256sum 实算，与 sidecar 一致，不是从派发指令抄来的。LOCK-AMEND-COUNT 沿用台账已有值 1，本席不改锁。
提示词：仓库内 docs/audit-2026-09/160-audit-prompt.md 与 docs/160-audit-prompt.md 均不存在。本席使用现存的 .scratch/module-recheck-20260908/160-audit-prompt.md，首行版本 V0.17。执行说明用 docs/audit-2026-09/plan/runbook.md V0.02。
独立性：未读 modules/XM-D029-b5-r1-self-raw.md（落盘时该路径不存在）。未用 MCP、子代理、第三席。未问人。未改业务代码、HTML、锁、findings、ledger。未 fetch/commit/push/merge/deploy。未对 complex、complex_test 执行写语句。
结论：PASS。本轮新问题 0。001 至 008 仍为已修，本席没有看到回归。已登记给 XM-M036-A 的空间种类删除申请主键撞键在本跑复现一次，不改判进本模块。

## 1. 技能

按 V0.17 §6 在本机核对后，实际读过并按收窄口径使用的技能：

- using-superpowers：读过。操作员本轮指令优先于技能默认动作：不派子代理、不问人、不改业务。
- grilling：读过。对抗式只用它的追问结构。技能原文里的「等用户回答」和「派子代理找事实」按 §6 作废，事实由本席读源和跑命令。
- verification-before-completion：PASS 之前实跑了下面第 6 节的 mvn，并核对了红名单。
- systematic-debugging：读过。本轮禁止修代码，Phase 4 不作。第一次 mvn 在 0.5 秒失败，错误是 release version 21 not supported；换 JAVA_HOME 到 Temurin 21 后重跑，没有改业务代码。
- diagnosing-bugs：读过。不向操作员要环境，不另写脚本。反馈环就是一次性空库上的 mvn -o test。
- codebase-design：读过。没有重做接口，没有派并行设计。用来核对平台管理员全量是否收在 isPlatformAdminBridge 与 platformWide 上，而不是散落到每个调用方各写一套。
- ponytail-review：读过。B3 这 14 个文件的改动没有为假设场景新加一层可删抽象。requiresTopAuthorityGrant 的 amountCent 仍未使用，这是改档位之前就有的参数，B2 已定只看动作名，本轮不另立项。
- writing-for-agents：读过。本文件按席甲能逐项复核来写：结论旁边是文件行和命令结果。

如实缺口，未冒充已调用：

- graft：技能已读。范围文件已由台账点名，调用方用检索定位后读了源。未跑 graft CLI。审计要求全文读改动和上下文，不以索引卡片代替。
- batch-grill-me、ubiquitous-language：在本机 mattpocock 缓存和 grok bundled skills 里没有。
- git-guardrails-claude-code：技能文件在 mattpocock misc 下存在。本席不是 Claude Code，没有它的 hooks。实际约束是本轮禁令：不 fetch/commit/push/merge，worktree remove 不用 --force。
- 明确不用：子代理、dispatching-parallel-agents、executing-plans、finishing-a-development-branch、research、ponytail-audit/debt/gain、code-review、brainstorming、writing-plans、TDD（本轮不改代码）、requesting/receiving-code-review（那是 B4）。using-git-worktrees 技能本体不用；操作员本轮明确要求一次性 worktree 跑测试，这一条覆盖技能里的「不用 worktree」。

## 2. 范围与读过的文件

27 个 1aa4c9c2 带出的代码文件，加上 B3 改到的 PlatformService.java 与 8 个测试文件。其中 4 个测试已在 27 个文件里。主工作区与 origin/main+冻结 diff 逐文件 cmp，32 个路径全部 SAME。

生产：

- AuthorizationService.java、JdbcAccessRepository.java
- AuthService.java、BearerAuthenticationFilter.java、MasterAuthorityService.java、ProductionAuthService.java、ProductionBearerAuthenticationFilter.java、SessionIssuer.java
- ProjectRecord.java、ProjectAssociationRequest.java、PlatformService.java
- SupervisorReviewEvaluateRequest.java、SupervisorReviewService.java
- LeasingService.java
- tools/generate-domain-ownership-scope-audit.mjs、tools/verify-pii-encryption.mjs

测试：AuthorizationServiceTests、M006SecurityRegressionTests、M010RegressionTests、StaffTestActors、AuthServiceTests、MasterAuthorityServiceTests、ProductionAuthJdbcIntegrationTests、BarStoredValueFundChainJdbcIntegrationTests、StoredValueNotificationIntegrationTest、SupervisorReviewServiceTests、LeasingServiceTests、LeasingSignAtomicityJdbcIntegrationTests、AccessJdbcIntegrationTests、AccessServiceTests、WecomRiskHitsEndpointReproTests、PlatformServiceTests。

调用方读过、但不在写入范围：AccessService 的列表与 blockGrantRequest、isMoneyPermission、grantReviewPolicy；AdmissionGuard.blockToReview；JdbcPlatformRepository 的分页重载与商户联系人加密；AccessRepository 接口上 null 即全量的默认方法。

## 3. 第一性三问

### 3.1 平台管理员全量，普通岗位不放开

为什么必须存在：删掉平台管理员的空范围，BSM-007「拥有系统里全部日常功能权限……不会出现管理员反而没权限」和操作员 D-032「不约束平台管理员」做不到；同时 PGM-041「只有项目之上的获授权岗位才可以跨项目汇总」要求普通项目岗位仍然被项目和商户挡住。

实际行为：isPlatformAdmin 就是 isPlatformAdminBridge，条件是 status=active 且 accountType 规范化后等于 platform_admin（AuthorizationService.java:279-281、:517-520）。currentMerchantScope、currentMerchantIdQuietly、currentProjectScopeQuietly 对这个桥返回空（:425-435、:443-457、:509-515）。空在调用方是不过滤。商户账号仍得到自己的 merchantId；没有商户号且不是平台管理员的人，currentMerchantScope 抛 MERCHANT_SCOPE_REQUIRED。非平台管理员的 currentProjectScopeQuietly 走 requireProjectScope，没有项目号就抛 PROJECT_SCOPE_REQUIRED，不再像旧注释那样「永不抛、无组织也看全量」。那句旧注释已改成与代码一致。

AccessService 只有 isPlatformAdmin 才把 organizationId 传 null：listPermissionTemplates :108、listRoles :293、listPositionRoleTemplates :413-414、listAccessUsers :1151-1152。商户账号走 listUsersByMerchant(自己的 merchantId)（:1153-1154），项目员工走 requireProjectScope（:1156）。Jdbc 五个带参列表仅在参数为 null 时转无过滤重载（JdbcAccessRepository.java:75、:202、:215、:310、:418-419）。空字符串不会转全量。

PlatformService.platformWide 先 requirePlatformScope，再看 currentProjectScopeQuietly 是否为空（:419-422）。因此只有平台管理员为真。列表改走不带 projectId 的重载：经营主体 :92-94、商户 :152-156、项目 :474-476、空间 :546-548、绑定 :769-771。JdbcPlatformRepository 这些重载是 COUNT/SELECT 全表（或商户只按存档与业态），不是把 null 塞进 WHERE project_id。单对象：requireMerchant 在 platformWide 时只要求商户存在（:410-416）；requireBusinessEntity 在 platformWide 时只要求主体存在，普通岗位仍要 businessEntityInProject（:974-982）；spaceExists 同理（:532-538）。新建：creationProject 对平台管理员强制显式 project_id，且项目必须存在（:452-454）；createSpace 同样 requireText(projectId) 再 requireCurrentProject（:725-729）。普通岗位 requireCurrentProject 仍要求等于当前项目（:437-440）。上级空间必须属于正在建的那个项目（:738-739）。项目归属配置 assignMerchantProject / associateBusinessEntityProject 仍要 isMasterAuthority，平台管理员调用会 MASTER_AUTHORITY_REQUIRED（:388-392）。测试 companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster 把平台管理员认领断言为抛错（PlatformServiceTests.java:1266-1270）。

执行证据：第 6 节全量 3532 测。平台管理员跨项目列表、普通员工只见 org_1、空间跨项目拒绝，都不在红名单。

未验证前提：没有另起一个 HTTP 进程去点平台管理员账号。服务方法和测试就是这条链路的可执行证据。currentMerchantIdQuietly 在 currentActor 抛错时仍返回空，这是改动前就有的 catch。正常登录入口不会以未认证身份走到这里，不另立缺陷。

### 3.2 最高权限只逐笔

为什么必须存在：治理地图第 528 行第④类写明持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕。整表开放与这句话冲突，B2 已定 002。

实际行为：isMasterAuthority 认 master_authority + usr_master_authority + master-authority + active（AuthorizationService.java:139-144），不再把 platform_admin 当成最高权限。listPending 只有 isPlatformAdmin 拿整表并记一条 pending_listed_cross_scope（SupervisorReviewService.java:895-898）。最高权限走 scopeProjectOf：有组织号就用组织号，没有就用空字符串，不把整份列表打成 403（:948-951），再按 visibleTo 过滤（:908-925）。requireVisibleReview 对平台管理员直接返回；对其他人先按可见规则，看不见且是最高权限时放行，并记 governance.supervisor_review.viewed_cross_scope，businessId 是复核单号（:927-941）。approve 不因为最高权限跳过节点：resolveDecidableNode 只在当前节点 kind 为 master 时让密钥主体裁决（:328-331），否则不是当前审批人就拒绝。主会话续期仍拒绝（AuthService.java:183-185，ProductionAuthService.java:208-209）。

执行证据：SupervisorReviewServiceTests.masterOpensAnotherProjectsReviewOnlyOneByOneAndLeavesATrace 与待审清单用例不在红名单。全量红名单里没有主管复核类。

未验证前提：没有用真实 U 盘签名。测试用的是内存里的 MASTER_USER_ID 主体。签发侧 issueMasterSession(String deviceId) 要求设备号，账号类型写成 master_authority（AuthService 相对发布前父提交的净差）。这与「最高权限不是平台管理员」一致。

### 3.3 受益人只强制授权类，比较还在

为什么必须存在：治理地图第 529 行写申请人与直接受益人不得审批，例子包括退款收款人和被授权对象。B2-r2 丁案写死：强制登记只覆盖动作名含 grant 或 authorization，或恰为 access.grant_request.money / access.scope_change.money。免单、退款、借记、扣款、调账移出强制清单。:208 与转发处的受益人等于比较保留。客人与往来方没有员工账号关联，登记为 XM，不在本模块重开。

实际行为：requiresBeneficiary（SupervisorReviewService.java:633-637）就是这个布尔。建单 blockForAdmission :464、evaluate 在 blocked 时 :143、审批 :204、转发 :795 都走它。六参 blockForAdmission 仍把受益人传 null（:423）。AdmissionGuard.blockToReview 仍只调六参（AdmissionGuard.java:126-133），所以酒吧免单不再因缺受益人建单失败。审批比较在 :208：reviewerId.equals(s.beneficiaryId()) 就 SUPERVISOR_SELF_REVIEW_FORBIDDEN。转发比较在 :804-806：目标不得是申请人或受益人。这两处不依赖 requiresBeneficiary，受益人只要写进排期就会比。

执行证据：admissionWaiverWithoutBeneficiaryIsCreatedAndNotRejectedForMissingBeneficiary、grantRequestWithoutBeneficiaryIsStillRejected、granteeCannotApproveTheirOwnGrantRequest 不在红名单。AccessServiceTests 补了内存仓库的 scheduleSupervisorReview / findSupervisorReviewSchedule，授权类用例不在红名单。

未验证前提：没有在空库里另插一条「客人 id 等于员工 id」的行。B2 已用 complex_test 只读查询说明两个编号体系交集为 0。本轮没有新证据推翻这一点，XM 维持。

### 3.4 整组消耗，逐码留痕

为什么必须存在：PGM-041 写临时授权可设单次使用，授予、收回、到期、单次消耗、失效全部留痕。整组消耗是已定稿要保留的行为，缺的是每个被标消耗的码一条日志。

实际行为：权限效果的 source_id 改为 grant_group_id（JdbcAccessRepository.java 相对发布前父提交，SELECT 里 grant_group_id::text AS source_id）。markSingleUsePermissionConsumed 在模板申请没有命中时，按 grant_group_id 把同组 single_use 且未消耗的行全部标 consumed，RETURNING permission_code（:1045-1055）。日志按返回的每个码各 INSERT 一条，usage_log_id 用新的 UUID（:1062-1073）。createTemporaryGrant 多码共用一个 grant_group_id（:623-651）。模板申请路径仍只记触发的那一个码（:1062）。004 的事实是临时授权按组消耗却只记一个码，不是模板申请。模板一行申请本来就不是按码各标一行。本轮没有新证据把它升成未修的 004。

执行证据：AccessJdbcIntegrationTests.consumingOneCodeOfASingleUseGrantGroupLogsEveryConsumedCode 不在红名单。断言是两个码 containsExactly，少一条就会失败。

未验证前提：没有并发双请求去撞同一组。UPDATE 的 consumed = FALSE 仍在，后到的请求得到空 RETURNING，consumed 为 false。

### 3.5 资金类不自动升档，分红仍升

为什么必须存在：PGM-041 只把持股分红的方案配置、算单、审批和发放写成由最高权限审批。其余高危授权按可配政策和「只指定最终审批人」。B2 写死 access.grant_request.money 与 access.scope_change.money 即使权限码含 dividend 也回到 grantReviewPolicy，不看权限码。

实际行为：requiresTopAuthorityGrant 只判断动作名含 dividend（SupervisorReviewService.java:623-626）。blockForAdmission :456-462 与 resolvePolicy :613-617 都用它。自报 permission_grant_l1/l2 作为动作名仍拒绝（:603-604）。AccessService.isMoneyPermission 仍可能把含 dividend 的权限码判成涉钱，从而把动作名设为 access.grant_request.money 或 access.scope_change.money（:2316-2321、:689-690、:767-770）。这两个动作名不含 dividend，所以不会被升到 l3。档位来自 grantReviewPolicy（:1915-1930）。

执行证据：moneyGrantKeepsItsPolicyLevelWhileDividendGoesToTopAuthority 不在红名单。money 断言 policyCode=permission_grant_l1，dividend 动作 finance.dividend.release 断言 permission_grant_l3。若资金类仍被升档，前一条会失败。

未验证前提：没有把后台授权政策 JSON 改成另一种风险档再跑一遍。代码按权限的 riskLevel 取 policy.riskReviews，测试夹具给出 l1。生产政策的具体档位不在本模块文件里。

### 3.6 刷新设备号

为什么必须存在：网页后台登录和刷新都不带设备号。若刷新对所有会话强制 device_id，网页访问令牌到期后续不上。设备会话仍要同一设备号。主会话不可续期，PGM-058。

实际行为：createSession 把空白设备号写成哨兵 unknown（AuthService.java:367，ProductionAuthService.java:435）。刷新：deviceBound 为「非空白且不是 unknown」；已绑定，或缺省请求里带了设备号但不等于会话设备号，都 SESSION_MISMATCH（AuthService.java:179-181，ProductionAuthService.java:204-206）。未绑定且请求不带设备号，不进入这个拒绝。其后仍拒绝主会话续期。网页登录测试传的 deviceId 是 null；login() 助手传 staff-phone-1。因此新测试同时锁住「无设备可续期」和「有设备缺号或错号都拒」。

执行证据：webSessionRefreshesWithoutDeviceButDeviceBoundSessionNeedsTheSameDevice 不在红名单。

未验证前提：没有打开浏览器点后台刷新。B2 已核 adminClient.doRefresh 请求体为 {}。本轮没有新的前端改动在范围内。

### 3.7 生产缺公钥拒启动

为什么必须存在：生产环境没有验签公钥，破窗通道不存在。M008 的 fail-closed 要保留，不能用库凭证开关豁免。

实际行为：MasterAuthorityService 构造函数在 prod profile 且公钥解析为 null 时抛 IllegalStateException，文案是「生产环境必须配置 U 盘破窗验签公钥 COMPLEX_MASTER_AUTHORITY_PUBKEY」（:95-99）。没有恢复豁免。WecomRiskHitsEndpointReproTests 用 DynamicPropertySource 注入运行时生成的 Ed25519 公钥，与 ProductionAuthJdbcIntegrationTests 同一做法。该测试类 1 测通过，不再是上下文失败。

执行证据：全量里 WecomRiskHitsEndpointReproTests 为 Tests run: 1, Failures: 0。公钥缺失的拒绝是构造函数源码，不是这个测试的反例断言。测试的职责是夹具不再误伤启动。

未验证前提：没有在 prod profile 下故意不配公钥再启动一次进程。构造函数的 if 就是这条拒绝，且 diff 没有把它删掉。

### 3.8 其余保留项

身份拆分、职级封顶两级（AuthorizationService.currentSupervisorLevel :319-320，角色节点 roleNodeLevelGate 最高 2）、租赁签约自动商户改为传入 intent.projectId() 且三个主体字段先空（LeasingService 相对发布前父提交的净差，四主体绑定调用还在）、领域所有权工具只把 CSV 的 file_version 从 V0.01 改成 V0.02、PII 工具登记 merchants.contact_name 与 secondary_contact_name。这些在 B1/B2 已定为保留。本轮重读净差，没有新的冲突。

PII 实跑：在与发布版相同的 worktree 上执行 node tools/verify-pii-encryption.mjs，exit 0，输出 PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库。JdbcPlatformRepository.java:625 与 :631 对这两个姓名列调用 piiCipher.encrypt。

ProjectRecord 增加 ownerPerson、address、businessScope，并保留旧的 11 参构造（缺省 null）。ProjectAssociationRequest 是幂等键、项目号、原因三个字段。二者服务于已有的项目档案和最高权限认领，不新造状态枚举。

## 4. 顺从式

判据并集 229：GEN 18、PGM 45、BSM 60、BEQR 106。本席从 criteria-lock.md 模块映射区抽出，与台账数字一致。每条只给本模块的证据，不继承旧模块 PASS。

「适用」= 范围内文件有这条锚点的行为，本席按第 3 节或下表核对过。「无本面」= 范围内文件不实现该锚点的状态、枚举或流程。无本面不是「没审」。

### 4.1 GEN

- GEN-001 必要性与最小性：适用。B3 只恢复平台管理员空范围、逐笔留痕、收窄受益人强制、逐码日志、分红才升档、未绑定设备可续期、测试公钥、恢复 never() 校验。没有为理论场景加接口。
- GEN-002 可执行正确性：适用。第 6 节 mvn。范围内新断言均不在 24 条红名单。
- GEN-003 测试有效性：适用。第 5 节写明哪些断言会失败。测试通过不替代第 3 节的源码核对。
- GEN-004 信任边界：适用。刷新不信任客户端自报的设备号，除非与会话已绑定值相等；平台管理员新建项目号必须在库中存在。
- GEN-005 授权与最小权限：适用。平台管理员全量与普通岗位、商户隔离分开。最高权限不整表。查他人有效权限：平台管理员或最高权限才跨组织（AuthorizationService.java:160）。
- GEN-006 数据完整性：适用。单次消耗的 UPDATE 仍带 consumed = FALSE。平台管理员新建不能把上级空间配到另一个项目。
- GEN-007 并发与幂等：适用。消耗用条件更新；审批节点 updateChainNode 失败即 APPROVAL_NODE_ALREADY_DECIDED。本轮没有新的并发缺陷证据。
- GEN-008 接口与模式一致：适用。内存 AuthService 与 ProductionAuthService 的设备号判断同形。网页哨兵都是 unknown。
- GEN-009 错误处理：适用。非平台管理员缺项目号是 PROJECT_SCOPE_REQUIRED，不是空过滤。平台管理员缺显式项目号是校验失败或 PROJECT_NOT_FOUND。
- GEN-010 安全与敏感信息：适用。平台管理员日常通配仍受显式 DENY 优先（permissionGrant :77-84，wildcard 只加 daily 码，:200-205）。敏感数据不因平台管理员身份自动获得，与 BSM-007 后半句一致。本轮没有把资金权限塞进日常通配。
- GEN-011 可观测：适用。整组每个码一条 consumed 日志；最高权限跨范围按编号读有 viewed_cross_scope；平台管理员整表有一条 pending_listed_cross_scope。
- GEN-012 资源与性能：适用。待审清单仍是先取 limit 再在内存过滤，注释写明量变大再下推 SQL。这是既有写法，本轮不新增需求。
- GEN-013 构建配置：适用。生产缺公钥拒启动保留。Wecom 测试补的是测试进程属性，不是放宽生产条件。
- GEN-014 迁移：无本面。范围内没有迁移文件。
- GEN-015 界面：无本面。范围内没有 HTML/前端。网页不带设备号是 B2 已核的客户端事实，本轮前端文件不在范围。
- GEN-016 文档可验证：适用。范围方法的注释与代码一致（平台管理员空范围、其余缺项目即拒）。旧的「永不抛」注释已不在。
- GEN-017 业务来源分类：适用。001 至 008 的分类维持 B2 定稿。本轮没有新的业务发明要改分类。
- GEN-018 第一性原理证据：适用。第 3 节每条都落到源码行或命令输出。

### 4.2 PGM 适用

- PGM-002 治理顺序：准入闸仍先建复核单再抛 ADMISSION_BLOCKED。免单不再被受益人强制拦住。
- PGM-004、PGM-005、PGM-006、PGM-017、PGM-022：普通岗位的项目、空间、商户、主体列表仍按当前项目；平台管理员走无项目重载。见 3.1。
- PGM-007：只碰到准入拦截建单，不实现合同状态机。受益人规则见 3.3。
- PGM-009、PGM-040、PGM-041、PGM-042：见 3.2、3.4、3.5，以及职级只映射到上两级、第 3 级只给密钥主体。
- PGM-012、PGM-048：商户账号 currentMerchantScope 返回自己的商户；requirePlatformScope 对带 merchantId 的账号和 outsourced_vendor 仍拒绝。listUsersByMerchant 只有非空商户号。
- PGM-014：复核留痕见 3.2、3.3、3.4。
- PGM-016、PGM-057、PGM-058：破窗公钥与主会话不可续期见 3.6、3.7。换公钥时 revokeOutstandingMasterSessions 仍在（MasterAuthorityService.java:107）。
- PGM-034：只验证企微 repro 测试在补了测试公钥后能启动并通过 1 测。不审企微会话业务。
- PGM-043：外包供应商仍被 requirePlatformScope 拒绝。本轮没有放宽外包或审计岗位。
- PGM-059：PII 门禁补登记商户第一、第二联系人姓名，实跑 18 列通过。客人与员工档案手机号不在本模块改动。

### 4.3 PGM 无本面

PGM-001、PGM-003、PGM-008、PGM-010、PGM-011、PGM-013、PGM-015、PGM-018、PGM-019、PGM-020、PGM-021、PGM-023、PGM-024、PGM-026、PGM-027、PGM-031、PGM-032、PGM-033、PGM-035、PGM-036、PGM-038、PGM-044、PGM-060、PGM-065、PGM-066。

会话 device_id 不是 PGM-011/036 的设备领用台账，也不是 PGM-008/032 的工作手机台账。分红升档不是 PGM-035 的提成发放流程。

### 4.4 BSM 适用

- BSM-003、BSM-004：岗位模板生命周期仍拒绝非平台管理员，且 M006SecurityRegressionTests 恢复了 verify(..., never()).savePositionTemplateLifecycle。商户删除审批仍要求最高权限（PlatformService.requestDeleteMerchant / approveDeleteMerchant 里的 isMasterAuthority）。本轮没有改删除规则本身。
- BSM-007：原文（docs/business-system-map-for-operations-v0.1.html:588）「平台管理员与最高权限是两个不同的角色」「拥有系统里全部日常功能权限」「不会出现管理员反而没权限」「资金操作……敏感数据……删除，以及最终业务审批……不因平台管理员这个身份自动获得」。数据范围见 3.1。身份拆分见 3.2。日常通配不包含资金码，见 GEN-010。
- BSM-114：不同主体只看自己的范围。平台管理员整表，普通岗位按项目和商户，最高权限待审不整表、按编号逐笔。见 3.1、3.2。

### 4.5 BSM 无本面

BSM-001、BSM-002、BSM-005、BSM-006、BSM-009、BSM-011、BSM-012、BSM-014、BSM-017、BSM-018、BSM-026、BSM-027、BSM-028、BSM-029、BSM-035、BSM-037、BSM-038、BSM-039、BSM-040、BSM-041、BSM-042、BSM-043、BSM-044、BSM-045、BSM-046、BSM-047、BSM-051、BSM-052、BSM-053、BSM-054、BSM-055、BSM-056、BSM-057、BSM-058、BSM-059、BSM-060、BSM-061、BSM-062、BSM-063、BSM-064、BSM-065、BSM-066、BSM-067、BSM-072、BSM-073、BSM-074、BSM-075、BSM-076、BSM-077、BSM-084、BSM-085、BSM-086、BSM-087、BSM-088、BSM-115、BSM-120。

第④类逐笔原文在治理地图第 528 行，不在这些 BSM 锚点里。本模块按那句原文审了最高权限，见 3.2。

### 4.6 BEQR 适用

- BEQR-063 高危动作：分红动作升 permission_grant_l3；资金类授权动作留在 grantReviewPolicy。见 3.5。
- BEQR-143：租赁自动商户不再把合同主体填进商户三个主体字段，四主体绑定仍由 createFourPartyBinding 写入。这是已保留的净差，本轮没有再改。
- BEQR-149 职级：非密钥主体的主管级别最高 2。
- BEQR-151 最终权限：DENY 优先；平台管理员 wildcard 只补日常码；单次授权在 requirePermissions 里按来源消费一次。
- BEQR-152 项目与敏感数据：项目隔离对普通岗位仍在；平台管理员跨项目是 D-032 与 BSM-007，不是把敏感数据自动授出。
- BEQR-159 权限状态：同组 single_use 行被标 consumed，每个码有消耗日志。

### 4.7 BEQR 无本面

BEQR-005、BEQR-020、BEQR-026、BEQR-028、BEQR-029、BEQR-030、BEQR-031、BEQR-032、BEQR-033、BEQR-034、BEQR-035、BEQR-036、BEQR-037、BEQR-038、BEQR-039、BEQR-040、BEQR-041、BEQR-042、BEQR-043、BEQR-044、BEQR-045、BEQR-046、BEQR-047、BEQR-048、BEQR-049、BEQR-050、BEQR-051、BEQR-052、BEQR-053、BEQR-054、BEQR-055、BEQR-056、BEQR-057、BEQR-058、BEQR-059、BEQR-060、BEQR-061、BEQR-062、BEQR-064、BEQR-065、BEQR-066、BEQR-067、BEQR-068、BEQR-069、BEQR-070、BEQR-071、BEQR-072、BEQR-073、BEQR-074、BEQR-075、BEQR-076、BEQR-077、BEQR-078、BEQR-079、BEQR-080、BEQR-081、BEQR-082、BEQR-083、BEQR-084、BEQR-144、BEQR-145、BEQR-146、BEQR-147、BEQR-148、BEQR-150、BEQR-153、BEQR-154、BEQR-155、BEQR-156、BEQR-157、BEQR-158、BEQR-160、BEQR-161、BEQR-162、BEQR-163、BEQR-164、BEQR-165、BEQR-166、BEQR-167、BEQR-168、BEQR-169、BEQR-170、BEQR-171、BEQR-172、BEQR-173、BEQR-174、BEQR-175、BEQR-176、BEQR-177、BEQR-178、BEQR-179、BEQR-180、BEQR-181、BEQR-182、BEQR-183、BEQR-184、BEQR-185、BEQR-186、BEQR-187、BEQR-189。

范围内没有这些枚举的状态机。储值测试夹具改演员是已登记 OBS-4，本轮红名单里没有新增储值失败。

以上适用与无本面合计 229，两边不重叠。GEN 18。PGM 适用 20、无本面 25，合计 45。BSM 适用 4（BSM-003、BSM-004、BSM-007、BSM-114）、无本面 56，合计 60。BEQR 适用 6（BEQR-063、BEQR-143、BEQR-149、BEQR-151、BEQR-152、BEQR-159）、无本面 100，合计 106。文中别处再提到已分类的 ID，只是交叉引用，不另占一条。

## 5. 对抗式

自问自答，不等操作员。

问：平台管理员若被种子绑了 merchant_id，列表和 currentMerchantIdQuietly 会不会被收成那一个商户？答：不会。桥在读 merchantId 之前返回空。AuthorizationServiceTests 用 accountType=platform_admin 且 merchantId=merchant 的 authenticate 助手，断言两个方法都是 empty。这条若退回「含 merchant」，测试失败。

问：普通员工或商户把 null 传进 Jdbc 五个列表，能不能看全量？答：服务层不这么传。只有 isPlatformAdmin 传 null。商户分支要求 merchantId 非空白。项目分支 requireProjectScope，空白即抛。

问：最高权限无项目号时，listPending 会不会 403 整表，或反过来看见所有项目？答：scopeProjectOf 对最高权限不调用 requireProjectScope。visibleTo 用空项目号比较发起人组织，对不上就看不见，除非本人是当前节点、被转发人或发起人。跨项目的单要按编号 findReview，并留下含复核单号的事件。

问：平台管理员是不是被当成最高权限，从而逐笔限制没了、或者第 3 级职级回来了？答：不是。accountType 必须是 master_authority 才是 isMasterAuthority。currentSupervisorLevel 对非密钥主体 Math.min(derived, 2)。

问：动作名子串会不会把免单、退款重新拉回强制受益人，或把资金类重新拉回 l3？答：现有动作名里 bar.bill_waiver、refund、finance.refund、ticketing.refund_approve、ar_ap_adjustment 不含 grant/authorization，也不是那两个精确的 access.*.money。access.grant_request.money 含 grant，所以强制受益人，但不含 dividend，所以不升 l3。finance.dividend.release 含 dividend，升 l3，这是 PGM-041 要的。OBS-2「现有动作表下不另加规则」没有新的撞词证据，不改判。

问：access.scope_change 不带 .money 会不会被新规则强制受益人？答：不会。它不含 grant/authorization，也不等于 access.scope_change.money。丁案写明不新加入。带 .money 的范围变更会被强制，因为精确等于。

问：网页会话若被写成 unknown，刷新不带设备号会不会被当成设备会话拒绝？答：不会。deviceBound 把 unknown 排除。请求空白时条件为假。

问：设备会话刷新不带设备号，或带错的，会不会放过？答：不会。deviceBound 为真且与请求不相等就 SESSION_MISMATCH。新测试对 staff-phone-1 的缺号和错号都断言这个码。若判断写反，网页那条成功断言或这两条拒绝断言会失败。

问：生产公钥豁免有没有从后门回来？答：没有。构造函数仍是 prod 且 publicKey == null 就抛。测试只给自己的进程加公钥。

问：新日志会不会只在返回码列表为空时仍报消耗成功？答：consumed 为 templateRows > 0 或返回列表非空。空列表且没有模板行则返回 false，不写日志。

问：普通岗位创建空间或商户时，平台管理员的「只需对象存在」有没有漏到他们身上？答：没有。platformWide 为假时 requireCurrentProject 和 businessEntityInProject 仍在。PlatformServiceTests 里普通员工列表 total 必须是 1 且 id 精确等于本项目；跨项目建空间、用别的项目的商户做上级或商户，都断言抛错。这些断言少一个 id 或多一个 total 就会失败。

问：平台管理员列表 total 为 2 的测试会不会在实现退回单项目时仍绿？答：不会。twoProjectFixtures 有两个项目，平台管理员断言 total 等于 2。退回单项目则 total 为 1，测试失败。

ponytail：没有发现本轮为了假设场景新加、可以整段删掉的抽象。不要求再删。

## 6. 全量测试

方法与台账 D-032 段相同。一次性 worktree：git worktree add --detach /home/james/wt-xmd029-b5-grok origin/main，HEAD 4aa318a5。git apply 冻结 diff。建空库 xmd029b5g（用户 complex，库名不是 complex 也不是 complex_test）。只在该 worktree 内把 backend/src/test 里 110 个文件的 5432/complex_test 改成 5432/xmd029b5g。主工作区改写命中数为 0。环境变量 COMPLEX_DATABASE_URL 与 SETTLE_TEST_DB_URL 都指向 jdbc:postgresql://127.0.0.1:5432/xmd029b5g。用户名与密码只从 /etc/complex/complex.env 的对应行读入进程环境，不写入仓库，不打印。

第一次 mvn -o test 在 2026-09-23T23:59:03Z 开始，约 0.5 秒后 exit 1：maven-compiler-plugin 报 release version 21 not supported。该次后台 shell 没有用到带 javac 21 的 JAVA_HOME。没有跑到测试，空库尚未被 Flyway 写入业务。

第二次：JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64，java 与 javac 均为 21.0.12.1。MVN_START=2026-09-24T00:02:33Z，MVN_END=2026-09-24T00:03:24Z，在 worktree 的 backend 目录执行 mvn -o test，exit 1（有失败用例，不是编译失败）。Surefire：263 个套件，3532 测，失败 8，错误 16，跳过 17，红用例 24。

24 条红用例名：

- 与 /home/james/audit-tmp/xmd029_b3_green.red 的 23 条相同：StaffProfile 2 条（请填写所属公司）、FieldLabelsTests.everyFieldKeyThrownInMainSourcesHasChineseLabel（缺 business_entity_id）、AmusementServiceTests 1 条、ComplianceControllerTests 3 条 403、FacilityEquipmentArchiveDelete 3 条（没有操作权限 facility.equipment.approve）、FinanceControllerTests 12 条（上下文缺 CustomerService bean）、CustomerGuardedActionAndRejectionAuditTests 1 条。
- 多出的 1 条：PlatformJdbcIntegrationTests.completingSpaceTypeRequestKeepsPriorRequestHistory，DuplicateKeyException，space_type_delete_requests 主键冲突。findings V3.98 已把它登记给 XM-M036-A（同一毫秒、stdr_加类型号加时间戳）。B4 双 PASS 时它没落在那 23 条里，本跑撞上了。这是已登记项的再次出现，不是本模块新缺陷，不改判。

范围内新补的授权、复核、平台列表、设备刷新、企微启动、整组留痕断言都不在这 24 条里。

清理：worktree 内 git checkout 还原跟踪文件；git status --porcelain 为空（target 与 data 仅被忽略）。git worktree remove 未加 --force，目录已不存在。DROP DATABASE xmd029b5g。之后 pg_database 里仍有 complex 与 complex_test，没有 xmd029b5g。主工作区测试树没有 5432/xmd029b5g。

## 7. 已定稿项

001 至 008 在 findings V3.98 为修复队列否。本席按当前源码重核了定稿边界，没有看到没修好或修出新分支的证据，所以不把它们重新送回修复队列。

不改判的只登记与 XM：

- OBS-1 换公钥作废主会话：revokeOutstandingMasterSessions 仍在，不升成缺陷。
- OBS-2 子串撞词：现有动作名下没有新的误伤，不另加规则。
- OBS-3 visibleTo 对普通员工的同项目同商户过滤保留。
- OBS-4 储值夹具与平台管理员全量无关。本跑没有新的储值红。
- M010 演员改动：001 修后不强制回改。本跑没有把 M010RegressionTests 打进红名单。
- XM-M036-A：上述 23 条旧红，加本跑再次出现的空间种类主键撞键。

## 8. 本轮问题与结论

本轮新问题：0。没有新的冲突、细化缺陷、技术性缺陷或添加项。四分类没有新条目可走。

结论：PASS。
