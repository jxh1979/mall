# 全量审计 Findings 台账
版本：V0.73

## 当前状态：M001—M005已发布PASS；M006 B5-r5双PASS、四值全等、ledger已记PASS、收口文件已写；发布进行中

## M006 B5-r5 双席结论与收口（V0.73，2026-09-06T16:33:39Z）

- 席乙 Grok：modules/M006-b5-r5-grok-raw.md，31051 字节 / 406 行，SHA256 da7fa5257660d431d8dce08fe1c16363a804f27d0955e2caf89d9bcbf3b37d1f；适配器回执 EXIT_CODE=0（16:17:26Z–16:31:47Z）。审计结论 PASS；14 文件、52 判据逐项；111 PASS / 0 FAIL（含 JDBC 12）；GEN-012 / GEN-009 特别核对满足；旧 001—036 不回退；无新 GR。
- 席甲 Claude：modules/M006-b5-r5-self-raw.md，13014 字节，SHA256 d0cb18fa8681439003597d779ddd2702031c3a2982a5314ec3c1fb43ca2a3f4e，先于读取 Grok raw 落盘。审计结论 PASS；101 PASS / 0 FAIL；探针 G/H/S/T 全部满足。
- 合取：B5-r5 PASS。B5 后四值（④=b5-r5 grok raw 最后一条）均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0；三份 HTML 与 HEAD 逐字相同。
- 收口顺序：先 ledger M006 → PASS，再写 modules/M006-10-backend-main-access-01.md。
- 登记（不入队）：单参 listGrantRequests、私有 slug()、wildcard 恒 false、注释漂移（Grok 补：currentProjectScopeQuietly 注释）、StaffService.java:363（M008）。
- 下一步：按操作员既有逐模块发布授权，release worktree → propose-change.sh → merge-pr.sh → 160 ff 同步。

## M006 B4-r5 双席结论（V0.72，2026-09-06T16:08:52Z）

- 席乙 Grok：modules/M006-b4-r5-grok-raw.md，316372 字节 / 4587 行，SHA256 a118bc15df30b011e8c5f1ece56928d68a45dcaeaead378b81967beb91bbeee2；适配器回执 EXIT_CODE=0（15:54:35Z–16:08:10Z）。范围归因 PASS（业务 diff 每一行归 035/036；`.scratch/**` 与 docs/audit-2026-09/** 按 §1.2）；内容 PASS（独立红绿、copy 矩阵、查询计数）；本席 PASS。
- 席甲 Claude：modules/M006-b4-r5-self-raw.md，304692 字节，SHA256 19cc4ca81cccceb5dd04d37e2e2b6b7dc69f2e71f802438ef6eece21991b34db，先于读取 Grok raw 落盘；内容 PASS、范围归因 PASS。
- 双 PASS → M006-035、M006-036 修复队列改「否」；全 36 ID 当前均否。B3 保持 5。
- B4 后四值（④=b4-r5 grok raw 最后一条）均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。
- 下一步：B5-r5 全模块双审（先派 Grok，席甲报告在 Grok 退出后落盘）。

## M006 B3 第 5 次执行记录（V0.71，2026-09-06T15:52:26Z）

- 依据：操作员“恢复 M006，批准第 5 次修复”；B2 双 PASS 定稿 035/036。
- 先红后绿：新增 `platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission`（M006-035）与 `orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed`（M006-036）于既有 M006SecurityRegressionTests（V0.20→V0.21），修复前 2 FAIL；修复后 2 PASS，全类 36 PASS / 0 FAIL。
- 035 修复：AuthorizationService（V0.04→V0.05）新增私有 `dailyPermissions()` 惰性 Supplier，一次授权判定 / 有效集计算 / holdsPermissionViaRole 内只读一次权限字典；`isDailyPermission` 删除，`isExplicitOrDailyGrant` 改为接收 Supplier；判定语义不变，无缓存框架。
- 036 修复：AccessService（V0.13→V0.14）copyPermissionTemplate 非平台管理员分支先 `requireProjectScope(actor)` 再比较归属（与 updatePermissionTemplate 同范式）。
- before tree 4a10ba268b3408173639bc79dba3e52b935164a7（B2 定稿、测试追加前实算）；after tree 7e3d9e970fd7b01735a78089cbeb4921e122cf80；完整 `git diff --binary` 297255 字节，SHA256 bac943157098c1e9a11c65fc895f33950f6eb688cb5b2062252e20d71afa0102。首次执行因运行器路径错误中止于测试追加之后、修复之前，清理了误生成的 backend/.scratch 后续跑；源码修复只落一次。
- B3 前后四值（④=b5-r4 grok raw 最后一条）均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。三份 HTML 未改。
- B3 累计 4→5。下一步 B4-r5（两席审完整 diff，逐块归因 035/036）。

## M006 B2 定稿（V0.70，2026-09-06T15:51:12Z）

- 席乙 Grok B2：modules/M006-b1-grok-raw.md 追加 V0.08（全文 199985 字节，SHA256 c20b80b1150cff1b50211a560e33470d09e084adb9d231abcdbdd5b36cb1de55），适配器回执 EXIT_CODE=0（15:37:16Z–15:49:42Z）。全批结论 PASS：独立探针复现 201/1/0 与 orphan NPE（并以 update 入口 PROJECT_SCOPE_REQUIRED 对照）；035 技术性/缺陷是/队列是，036 细化/缺陷是/队列是；登记 6 项维持；旧 001—034 不重开；无添加、无 HTML 申请。
- 席甲 Claude B2：modules/M006-b1-self-raw.md 追加，PASS。
- 双 PASS → 定稿：M006-035、M006-036 修复队列「是」；其余 34 ID「否」。Grok 指出席甲 B5 raw §2 GEN-017 行“两条均为技术性”与 CX-B5-02 正文“细化”不一致，定稿以正文（细化）为准。
- 第 5 次 B3 依操作员指令（“恢复 M006，批准第 5 次修复”）执行；先落最小失败测试（红），再最懒修复（绿）。

## 操作员指令登记（V0.69，2026-09-06T15:46:10Z）

操作员原话（2026-09-06）：“恢复 M006，批准第 5 次修复”。据此：解除 B3 三次上限对本模块的限制一次，允许在本批 B2 双 PASS 定稿后执行第 5 次整批 B3（修复队列为「是」的已定稿 ID），随后仍按 B4-r5 完整 diff 双审 → B5-r5 全模块双审；未经双 PASS 不称模块 PASS。不改控制提示词 V0.17 原文，不影响其他模块上限。B2 定稿前不写源码。

## M006 B5-r4 双席结论与 B2 候选（V0.68）

两席本次整个模块均审完后合取判定：**B5-r4 FAIL**（席甲 FAIL 2 条新意见；席乙 Grok PASS 0 条；任一 FAIL 即 FAIL，席甲不以对方 PASS 撤回自票）。
- 席乙 Grok：modules/M006-b5-r4-grok-raw.md，27075 字节 / 372 行，SHA256 2e9767db6979830e1385e71938d5ff06f6994ccc4a490aab28ba315472efa8f8；适配器回执 EXIT_CODE=0（15:21:48Z–15:34:22Z）。审计结论 PASS；14 文件、52 判据逐项；109 PASS / 0 FAIL / 2 SKIP（含 M006JdbcRegressionTests 12 项，complex_test 库 TEMP 表）；HTTP/翻译探针 7 路全中文（“U 盘密钥”对齐 HTML 用词）；旧 34 ID 均不回退；无新 GR。
- 席甲 Claude：modules/M006-b5-r4-self-raw.md，20933 字节，SHA256 73e12799fdb8bc193e2a046241098a66338aa72cb1fde4e0daf914496a4cdb9a，先于读取 Grok raw 落盘。审计结论 FAIL：CX-B5-01、CX-B5-02（探针证据见该 raw §4）；99 PASS / 0 FAIL（未跑 JDBC 类）。
- B5 后四值（④=b5-r4 grok raw 最后一条）均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0；三份 HTML 与 HEAD 逐字相同。
- 回退统一规则：本批两条依据均为“无 ID 新缺陷” → 整批回 B2 定稿分配 ID；不得直接进 B3。

### M006-035 — 平台管理员有效权限计算对权限字典逐项重复查询（N+1）
- 来源：席甲 CX-B5-01。分类：技术性（无业务发明、不与 HTML 冲突）；冻结判据 GEN-012；工程缺陷 是；候选修复队列 是。
- 位置：backend/src/main/java/com/complex/platform/access/AuthorizationService.java:89-94（isDailyPermission 每次调用 accessRepository.listPermissions()）、141-168（computeEffectivePermissions wildcard 分支对字典每项调用 isDailyPermission）、96-101（isExplicitOrDailyGrant 对 role_platform_admin 效果逐条调用）；JdbcAccessRepository.java:49-56 listPermissions 为无缓存 SELECT。
- 证据（类型 4，席甲 raw §4 探针 G）：字典 200 条、platform_admin 主体，一次 effectivePermissions() 触发 listPermissions() 201 次；requirePermission(一个日常码) 1 次；普通 staff 0 次。放大倍数 = 权限字典规模（当前约 219）。
- 拟修边界（B2 定稿用，本轮不修）：在单次 computeEffectivePermissions / requirePermissions 内只取一次 listPermissions() 并复用日常权限码集合；不改判定语义、不引入缓存框架；补一条“单次调用 listPermissions 不超过 2 次”的可失败断言。
- 溯源：由第 1 轮 B3（M006-001 修复引入 isDailyPermission）产生；非 034 回归；b4-r1 两席 diff 审未评估查询次数。

### M006-036 — 无组织归属的非平台管理员复制模板时抛 NullPointerException
- 来源：席甲 CX-B5-02。分类：细化（上位 BSM-114 / PGM-048 跨组织隔离）；冻结判据 GEN-009；工程缺陷 是；候选修复队列 是。
- 位置：backend/src/main/java/com/complex/platform/access/AccessService.java:186-191（`!actor.organizationId().equals(source.ownerScopeId())` 在 organizationId 为 null 时 NPE，早于 L193 requireProjectScope）。
- 证据（类型 4，席甲 raw §4 探针 H）：orphan staff（organizationId=null，M006-003 已承认的真实主体形态）复制 org 拥有的模板 → NullPointerException（经 GlobalExceptionHandler 为 500 INTERNAL_ERROR）；同组织 staff 对照正常。fail-closed、无越权，但错误不可定位、非 ApiException。
- 拟修边界：L189 先 `authorizationService.requireProjectScope(actor)` 再比较（与 L146 updatePermissionTemplate 同范式）；补一条 orphan 主体得 ApiException 的可失败断言。

### 登记（不入队，两席合并）
- GEN-001：AccessService.listGrantRequests(Integer) 单参重载无生产调用方（席甲；Grok 亦列 `page(全量)`）。
- GEN-001：AccessService.java:1898 私有 slug() 无调用方（Grok B5-r4 ponytail 指出；本席 grep 核实 0 处调用）。
- EffectivePermissions.wildcard 恒为 false（computeEffectivePermissions 固定传 false），消费方 1 处（requireActorCanGrant 等）按 false 分支运行，行为正确（DENY 优先、权限已折入 grants）（Grok 指出）。
- 文件版本头递增（AccessService V0.13→V0.14、测试 V0.20→V0.21）未随第 4 次 B3 落地（两席均指出）；下一批修复一并处理。
- 模块外：StaffService.java:363 同类英文回显（M008）。
- Grok 分类备忘：控制器/服务英文字段键文案经 FieldLabels.translate 全中文；`没有操作权限：<权限码>` 中权限码为 PGM-066 允许的系统内部标识符。席甲同意（探针一致）。

### 本批 B2 流程
① 本段为席甲候选台账（2026-09-06T15:36:19Z）。② 派 Grok 对两份 b5-r4 raw 与本候选做差集/分类/队列复核。③ 两席各自把 B2 PASS/FAIL 追加到自己的 b1 raw（不写锁哈希行）。④ 双 PASS 才定稿。
轮次：B3 已执行 4 次，控制提示词 3 次上限已超；B2 双 PASS 定稿后第 5 次整批修复须操作员另行指令，本进程届时停在等人态并回执。

## M006 B4-r4 双席结论（V0.67）

- 席乙 Grok：modules/M006-b4-r4-grok-raw.md，198135 字节，SHA256 d2f552d871c6ca0d0293c5411000bb96f4618717548792d00c21e38efe9b8e52，适配器回执 EXIT_CODE=0（START 2026-09-06T15:06:23Z END 15:19:20Z）。内容 PASS、范围归因 PASS、本席 PASS；无新 finding。独立红绿：before 树 RED_ONE_EXIT 1，after 树 34 PASS / 0 FAIL。
- 席甲 Claude：modules/M006-b4-r4-self-raw.md，189047 字节，SHA256 025fd66ce84a2f60a6fdd5c19b333cd7eb3b272cdc76547207b70029b9c35a6b，先于读取 Grok raw 落盘。内容 PASS、范围归因 PASS。
- 双 PASS → M006-034 修复队列改「否」；全 34 ID 当前均否。B3 保持 4。
- Grok 两点更正/观察（本方核实）：① StaffService.java 在 plan/modules.md 属 M008（V0.66 段误写 M007，路径归因不受影响）；② StaffService.java:363 另有同类“（approved）或驳回（rejected）”英文回显，不在本 diff、不在操作员②点名范围，属 M008 文件，本轮不作为 M006 finding，记入后续。③ 拟修写明的文件版本头递增（AccessService V0.13→V0.14、测试 V0.20→V0.21）未做；版本头非用户可见文案、非冻结判据，不影响本轮结论，留待下一批修复一并处理。
- B3 前后四值（④=b5-r3 grok raw 最后一条）均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。
- 下一步：B5-r4 全模块双审（先派 Grok，席甲报告在 Grok 退出后落盘）。记录时间 2026-09-06T15:21:11Z。

## M006 B3第4次执行记录（V0.66，操作员例外）

操作员指令原话（2026-09-06）：①“接手 M006 的 4 处文案修复”；②“StaffService 那两处也一起改了”；③“你和Grok按2方审计提示词中的要求做一个m006全量审计,只是把codex gpt 6要执行的全部内容换成 fable 5.1 extra”。
据此：本批B3由Claude Fable 5.1执行；席甲（审计/修复/调度）本轮由Claude担任，Grok席乙不变；控制提示词V0.17其余规则不变。B3累计3→4（不抹旧三轮）。
- 034 修复落地：`backend/src/main/java/com/complex/platform/access/AccessService.java` 515、1061、1491、1694 四处人读文案改为纯中文（“删除配置须由最高权限（U 盘密钥）审批”“薪资币种当前仅支持人民币，不支持所填币种”×2“审批结论必须为通过或驳回”）；错误码、允许值集合、权限与币种判断未改。
- 操作员追加授权（指令②）：`backend/src/main/java/com/complex/platform/access/staff/StaffService.java` 247、1244 同类“（approve）或驳回（reject）”回显改为“审批结论必须为通过或驳回”。该文件属M007，本批diff中的这两块按操作员指令②归因034（同类文案缺陷、操作员点名扩展），不作为无ID越界。
- 最小验证：`M006SecurityRegressionTests.java` 新增 `invalidDecisionMessageIsChinese`（反射调用 normalizeDecision，断言错误码 VALIDATION_FAILED 且消息不含拉丁字母）；同类34项在160上javac运行器全PASS；staff包13源javac编译exit0。
- B3前树：c77d4453db7eb9b92388de630fdd5121cdef7959（第3轮after，源码此前未变）；B3后树：54999203b0587456b8d2f473b5ee0f5e3d67908b（含docs/audit-2026-09下第3轮raw与台账，均为§1.2排除的审计产物）。
- 下一步：B4-r4（两席审完整diff，逐块归因034）→ B5-r4（两席全模块三线审计）。

## M006 本次例外恢复及B2候选（V0.65）

操作员本次明确指令：“仅限本次，你把经过3次修复后仍未通过的问题交给claude cli,调用fable 5.1 模型 high 模式看看能否修复”。
仅本次授权解除M006的BLOCKED-3ROUNDS、把下一批修复执行者改为Claude CLI Fable 5.1/high；不改变控制提示词V0.17原文和后续模块默认角色/上限。保留真实B3累计3，实际新整批完成后记4，不抹掉旧三轮。新一批仍按B2整批定稿→委托修复→B4完整差异双审→B5全模块双审；未经后续验证不称模块PASS。没有重开M001—M005或全仓。
已实测 /home/james/.local/bin/claude 2.1.261，内置模型ID claude-fable-5-1；以--model claude-fable-5-1 --effort high无工具连通检查真实exit0/is_error=false，返回modelUsage canonicalModel=claude-fable-5-1、provider=firstParty。没有使用默认opus/xhigh或fallback模型。先一次解析把JSON事件数组误当对象，CLI自身仍exit0；修正解析后完整读取以上实际结果，无文件改动。
git fetch origin后HEAD=origin/main=395e6fbab027020da1c38237fad17e8edcd7a132。

### M006-034 — 用户可见报错残留英文枚举与缩写
- 整批输入：modules/M006-b5-r3-self-raw.md原V0.01独立票及接收追加V0.02；modules/M006-b5-r3-grok-raw.md完整V0.01。原票各1项，完整并集同类4处，无遗漏。
- 候选：冲突；工程缺陷是；修复队列是。无添加业务、无HTML申请；旧001—033保持否。
- 冻结判据PGM-001/PGM-066/GEN-002/GEN-015；依据为冻结全中文规则、真实当前代码和已落盘可运行错误响应探针。Grok4处完整范围保留，不缩成仅本方1694。
- 来源位置：backend/src/main/java/com/complex/platform/access/AccessService.java:515、1061、1491、1694；错误最终通过GlobalExceptionHandler:49—61、FieldLabels:504—529传到message。
- 拟修边界：该源文件4处人读报错文本、源文件版本V0.13→V0.14；在既有backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java补最小真实行为回归（V0.20→V0.21），覆盖4处错误出口及允许业务行为仍保持。需要的夹具可复用现有测试，不另加测试框架。
- 参考最小文字：审批结论“审批结论必须为通过或驳回”；最高权限“删除配置须由持授权密钥的最高权限人员审批”；两处币种“薪资币种当前仅支持人民币”。最终只改显示文案，不改内部枚举、错误码、权限判断、币种允许值、账号/支付/存储逻辑或通用翻译器。
- Claude仅为本次修复执行者，不能给自己代行Codex/Grok审计票；本次范围外内容发现后只报告。修复前B2仍待双PASS，不先行写源。


## M006 B5-r3完整双席意见与三轮交付（V0.64）

两席本次整个模块均审完后统一判定：**B5 FAIL；模块 BLOCKED-3ROUNDS；B3已执行3次**。
Grok于2026-09-06 04:49:42 UTC检查取得真实EXIT_CODE=0，报告317行/21532字节，SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb，全文已读；交付成功不等于审计通过。
本方独立原始完整报告34586字节，SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3，先于读取Grok封存。原票FAIL，原1条意见保留；后续接收记录仅追加，不覆盖原票。

### 本批全部意见（终态待恢复；未做B2定稿）
| 原始来源 | 问题及真实影响 | 位置 | 原分类/工程缺陷 | 当前处理 |
|---|---|---|---|---|
| self CX-B5-01 | 无效审批结论报错给用户显示approve/reject，实际Controller→Service→错误响应复现；不改变审批结果 | AccessService.java:1694，入口147—155/703—724 | 冲突/是，PGM-001/066、GEN-015，类型4 | 原始意见保留，待操作员恢复M006后整批B2 |
| Grok GR-01（与上条同类，完整保留其更宽范围） | 同上；另有岗位模板删除审核拒绝提示USB，以及两处薪资币种错误提示CNY/输入币种 | AccessService.java:515、1061、1491、1694 | 冲突/是，PGM-001/066、GEN-002/015，类型1+4 | 原始意见保留，四处合为一类文案问题的归并建议，尚未正式ID |

原始源：[本方完整报告](modules/M006-b5-r3-self-raw.md)、[Grok完整报告](modules/M006-b5-r3-grok-raw.md)。
本方接收后核对四处当前源码及GlobalExceptionHandler:49—61/FieldLabels:504—529；中文中夹英文原样透传。Grok额外三处的运行探针证据归属Grok，本方不冒称重跑了其99项测试。
旧M006-031与032在本轮两席各自测试中通过，保持旧33ID全部队列否；不得把这批新问题直接塞入已出队旧ID或无ID修复。没有遗漏另一类新意见、没有新增业务规则、没有HTML申请。
潜在最小变更仅四处提示文本；保留内部代码、允许的decision/币种值、权限与业务判断。恢复后仍须先整批B2双PASS再修，不在本轮突破三次上限。
验证：本方当前源98项回归全通过；独立真实审批响应反例1失败；Grok报告99项所选单元/JDBC测试通过及报错探针确认英文，明确省略2个StaffService测试（本方98项中已经实际执行），不把两组数字相加称197个不同测试。未跑真实支付厂商、全端浏览器/安卓、全库迁移或完整mvn test。

### B5后四值与锁
四值均为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`：①当前实算；②sidecar；③ledger顶部；④本次Grok raw最后一条独占哈希行。LOCK-AMEND-COUNT=0，校验实际命令/输出在本次操作员回执与本方raw接收追加段。
没有因失败修改锁、scope、模块映射或当前源码，三HTML仍与HEAD逐字相同。

### 三轮完整diff交付
以下所有六份raw都含对应两树**完整未过滤**git diff --binary；本次逐字包含校验均true，不仅是stat或节选。源码仍等于第三轮after，未回滚已通过修复。
| B3轮次 | before tree | after tree | 完整diff字节及SHA256 | 双席原始diff报告 |
|---|---|---|---|---|
| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | [self](modules/M006-b4-r1-self-raw.md) / [Grok](modules/M006-b4-r1-grok-raw.md) |
| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 | [self](modules/M006-b4-r2-self-raw.md) / [Grok](modules/M006-b4-r2-grok-raw.md) |
| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 | [self](modules/M006-b4-r3-self-raw.md) / [Grok](modules/M006-b4-r3-grok-raw.md) |

停止原因：控制提示词V0.17第4节“第3次B3后……该次B4或B5审完仍FAIL→……状态记BLOCKED-3ROUNDS……是否重启由操作员另行下指令”。当前B5完整结束、两席均FAIL，故不自动回B2/B3、不加第四轮、不开始M007、不提交合并推送M006。
需要操作员：新指令恢复M006；恢复后保留现有修复/原始报告，先处理本批全部意见，而非重开M001或全仓。


## M006 B4-r3整批出队（V0.63）
2026-09-06 04:32:19 UTC取得Grok真实exit0，完整448行/30050字节/SHA945fa354e3a1c02ae01a553284187b30ec917460764cebe31e0b4217348604dd已读完。其内容PASS、范围PASS、无新finding；独立绿测33+9通过，并分别从before源码复现031公司/旧记录及032错误文案的3个失败。未把本方98测试和完整编译算作自己实跑。
本方原B4 V0.01内容/范围PASS，原36492字节/SHAf65b1ae24f09b0b6238f9ae248fb0c759e9c87d4b82a076f400093d2e7e03553，现仅追加V0.02算术勘误（新键实际71字符而非报告误写67，代码/测试/原票未改）。双方raw所含完整diff均与两树实际14687字节逐字一致，6现源仍等于after。
按B4双PASS规则统一将031/032当前队列改否；全33ID当前均否。不是模块收口，须继续B5-r3整模块三线双审。Grok关于长度前缀的可选缩短不属finding，不作无ID改动。

## M006 B2定稿（V0.62）
2026-09-06 04:11:34 UTC取得Grok真实exit0；完整读取新增V0.06的218行/18285字节，SHA a8fce223cd34d693da0b4689d7190b6128118b628fe74af9f6fb89e26ab33c64。旧147529字节前缀未改；整份165814字节/SHA4e0d7aa205a52b2f1b84cf040b4080033e5a621fb4330b88e09b51f8e44025a4。本方既有本批B2 V0.15 PASS已先落盘；Grok本批B2 PASS，全部3条差集为空，两席共同确认候选V0.61的当前33ID表、分类及范围。
031、032队列是；033细化/工程缺陷否/队列否，Grok已独立接受当前完整源码构建证据，原B5意见保留；旧30项仍否。无添加项、无HTML修改申请。B3只修031/032，随后审完整diff并再次全模块复审。旧候选措辞保留为历史记录，以本段及当前队列表为现值。
031将沿用现有主体/组织字段收窄共享范围，现有缓存表不改结构；新范围的键使用已有摘要能力适配operation_id的128字符容量。旧共享记录缺少可靠主体归属时，拒绝再次执行并提示先核实原操作，不能返回他人结果或重复写入；同主体的新记录重试仍返回原结果。032只改本地错误提示和响应断言。

## M006 B5-r2整批归并候选（V0.61，待本批双席B2定稿）

本次完整两席B5均FAIL：Codex self V0.01，36285字节/SHA7c947c128b3ec0a7a6b891a795b613bf900db78aed9002e3e1e8e01f844a7395；Grok本次V0.02追加18373字节/SHA1c8b6d43dad24c118bc3bf013487cd9488d1fd80d7c029b6150cef7bd037daae，整份45265字节/SHA0f7c1000403b9f254988c63041f3f4dc3a1cd12ada32741a01c5ba396b60e6c9。真实exit0，03:51:20 UTC按5分钟间隔取得；完整224行新增报告已读，旧26892字节前缀保持。输入路径固定 modules/M006-b5-r2-self-raw.md 与 modules/M006-b5-r2-grok-raw.md，本批只采信两份本次完整报告。旧Grok文件不当作成功票；本方未读Grok后倒改自己的独立B5票。

共3条本批意见：CX-B5-01→031、GR-01→032、GR-02→033。当前候选共33ID，001—030继续队列否，031/032队列是，033队列否待外席复核。原30项分类、修复范围、历史票保持。B3=2，无HTML申请、无添加项待答；本批尚未定稿，不修复。B5后actual/sidecar/ledger/Grok末条哈希四值全为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。

### M006-031 — 不同公司组织使用相同请求编号时，创建角色结果串用

- 来源：本次self CX-B5-01。分类：冲突；工程缺陷是；候选修复队列是。
- 定位：AuthorizationService.java:199—218、AccessService.java:300—330；直接消费者IdempotencyService.java:72、JdbcIdempotencyRepository.java:23/37。
- 依据类型1/2/4：冻结GEN-002/004/005/007/009/010、BSM-028/114、PGM-001。不同组织应按实际归属访问/创建自己的对象。真实controller+service+resolver+幂等服务执行，两个已授权组织员工各自合法首次请求company-first-role，同正文；二者scope=p，结果只创建org-a角色，org-b返回org-a角色id及内容。JSON组织字段因@JsonIgnore为空，不能据此称无串用。完整可复现命令及输出在本次self raw。
- 边界：不假设随机UUID碰撞；接口允许任意非空幂等编号，服务端不能以单个前端randomUUID习惯代替主体隔离。公司组织与商户都沿用既有字段，不发明新业务身份。
- 拟修：收窄现有幂等结果的主体范围，消除无merchant主体共享p导致的跨组织回放；保持实际认证上下文、同主体正常重试只生效一次，并核对旧记录重试，不能仅改范围前缀造成已有操作再次生效。修改限AuthorizationService、AccessService及其必要既有幂等直接实现与对应最小验证；不扩全仓业务功能、不新增策略/审批/HTML。
- 最小验证：不同组织同编号结果各自隔离；同主体同编号重试仅产生一个角色；存量成功记录重试不重复写入；相关直接消费者保持原业务约束。

### M006-032 — 非法账号类型错误提示夹带英文枚举

- 来源：本次Grok GR-01，原票保留。分类：冲突；工程缺陷是；候选修复队列是。
- 定位：AccessService.java:48—49、921—923；FieldLabels.java:529；GlobalExceptionHandler.java:49—61。
- 冻结PGM-001/066、GEN-002/015；类型1原文“提示、报错…必须全部为中文”与类型4实际错误响应。
- 本方接收核实：当前真实controller调用createAccessUser(accountType=platform_admin)，取得INVALID_ACCOUNT_TYPE，再送真实GlobalExceptionHandler；实际响应“账号类型「platform_admin」不允许，可选值：[staff, merchant, merchant_admin, outsourced_vendor, supervisor, sales]”，中文断言失败。详见下方命令。不是只因源码中存在英文字段就判错。
- 拟修：只修改本模块账号类型错误分支，去除给用户显示的英文枚举/输入原值，使用准确的中文提示；保留错误码、账号类别和注册审批规则。避免通过重写全局FieldLabels扩大修改范围。补一个真实错误响应断言，其他历史测试保持。

### M006-033 — 新增授权政策源码未跟踪的构建意见（待Grok核实，不直接视为BUG）

- 原始Grok GR-02：技术性、工程缺陷是，理由是AccessGrantPolicy不在git ls-files与阶段A scope中；从classpath删其class且不提供现有源码后编译失败。原始FAIL不被本方覆盖。
- 本方候选：细化、无本次可成立工程缺陷、修复队列否。其上位是BSM-001既有政策可配置及上下限；有明确上位时按2.1应先归细化。
- 当前事实：该文件是已定稿M006-016在B3-r1生成的合法修复源，已完整包含在B3-r2 after tree 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9中，和当前字节相同。B3工作区尚未提交并非遗漏交付，流程规定B3不提交而B4完整tree必须覆盖untracked；阶段A清单冻结且B阶段不允许手工扩表。不能要求为B3新增合法修复源另扩scope/模块映射，亦不触发阶段A未跟踪人闸。
- 实测：复制既有target到自动清除临时目录，删除其中4个AccessGrantPolicy缓存class，再用当前完整必要源码45文件显式javac；退出0，新生成4个AccessGrantPolicy class。说明当前可构建并不依赖旧Policy class缓存。全部共享target不变，无源码改动。完整命令和输出见下。
- 待Grok在本批B2独立裁定：当前working-tree全源检查与其“删去依赖后只编14文件”的检查对象不同。若接受本方证据，则只登记并在模块发布时随全批修复一起跟踪提交，不修业务、不扩锁；若不接受，整批仍留B2，不单方将其意见抹去。

### 本批接收验证（不是回写本方独立B5票）

#### 真实非法账号类型响应

```powershell
@'
# V0.01 temporary B5 read-only verification
from pathlib import Path
import subprocess,tempfile
probe="\n    @Test void b5InvalidAccountTypeResponseIsChinese() throws Exception {\n        when(repository.listPermissions()).thenReturn(List.of(\n            new PermissionDefinition(\"access.user.manage\", \"管理账号\", \"access\", \"medium\", false, \"active\")));\n        var controller = new AccessController(service, authorization);\n        ApiException failure = null;\n        try {\n            controller.createAccessUser(new AccessUserRequest(\"b5-invalid\", \"检查错误提示\", List.of(),\n                \"example\", \"password\", null, \"platform_admin\", \"org\", null, \"active\"));\n        } catch(ApiException e) { failure = e; }\n        assertThat(failure).isNotNull();\n        assertThat(failure.code()).isEqualTo(\"INVALID_ACCOUNT_TYPE\");\n        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();\n        var method = handler.getClass().getDeclaredMethod(\"handleApiException\", ApiException.class);\n        method.setAccessible(true);\n        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);\n        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();\n        System.out.println(\"B5_ERROR_RESPONSE=\"+body.message());\n        assertThat(body.message()).doesNotContain(\"platform_admin\", \"staff\", \"outsourced_vendor\");\n    }\n"
runner="""package com.complex.platform.access;
public class M006Runner {
 public static void main(String[] args)throws Exception {
 int passed=0,failed=0;
 var c=M006SecurityRegressionTests.class;
 for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)&&m.getName().startsWith("b5Invalid")){
 var t=c.getDeclaredConstructor().newInstance();
 try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}
 m.setAccessible(true);m.invoke(t);passed++;System.out.println("PASS "+m.getName());
 }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println("FAIL "+m.getName()+": "+e.getCause());}
 finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}
 }
 System.out.println("RESULT "+passed+" PASS / "+failed+" FAIL");if(failed>0)System.exit(1);
 }
}"""
with tempfile.TemporaryDirectory(prefix="m006-b5-self-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 original=Path("backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java").read_text()
 test=root/"M006SecurityRegressionTests.java";test.write_text(original[:original.rfind("}")]+probe+"\n}\n",encoding="utf-8")
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"common/api/FieldLabels.java",base/"common/api/GlobalExceptionHandler.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [Path("backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java"),Path("backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java"),test,src]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

exit_code=1

```text
JAVAC_EXIT 0 SOURCES 51
 
B5_ERROR_RESPONSE=账号类型「platform_admin」不允许，可选值：[staff, merchant, merchant_admin, outsourced_vendor, supervisor, sales]
FAIL b5InvalidAccountTypeResponseIsChinese: java.lang.AssertionError: 
Expecting actual:
  "账号类型「platform_admin」不允许，可选值：[staff, merchant, merchant_admin, outsourced_vendor, supervisor, sales]"
not to contain:
  ["platform_admin", "staff", "outsourced_vendor"]
but found:
  ["platform_admin", "staff", "outsourced_vendor"]

RESULT 0 PASS / 1 FAIL
 Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended


```

#### 去除旧Policy缓存后编译当前必要全源

```powershell
@'
# V0.01 isolated current-working-tree compilation check
from pathlib import Path
import subprocess,tempfile,shutil,hashlib
policy=Path("backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java")
snapshot=subprocess.check_output(["git","show","2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9:"+str(policy)])
assert snapshot==policy.read_bytes()
with tempfile.TemporaryDirectory(prefix="m006-b5-fullsource-",dir=".scratch") as d:
 root=Path(d).resolve();cached=root/"cached";shutil.copytree("backend/target/classes",cached)
 removed=list((cached/"com/complex/platform/access").glob("AccessGrantPolicy*.class"))
 for p in removed:p.unlink()
 cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(cached)+":"+cpfile.read_text().strip();out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("REMOVED_CACHED_POLICY_CLASSES",len(removed))
 print("CURRENT_POLICY_SOURCE_EQUALS_B3_AFTER",snapshot==policy.read_bytes())
 print("CURRENT_SOURCE_FILES",len(sources),"JAVAC_EXIT",r.returncode)
 print("GENERATED_POLICY_CLASSES",len(list((out/"com/complex/platform/access").glob("AccessGrantPolicy*.class"))))
 print(r.stdout,r.stderr)
 raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

exit_code=0

```text
REMOVED_CACHED_POLICY_CLASSES 4
CURRENT_POLICY_SOURCE_EQUALS_B3_AFTER True
CURRENT_SOURCE_FILES 45 JAVAC_EXIT 0
GENERATED_POLICY_CLASSES 4
 

```



## M006 B4-r2双PASS与统一出队（V0.60）
本方完整报告70271字节/SHA9491d8b1492de2a500cf9dd7797c34159315379c1b59a8fa10d7e0f128298de4，审计PASS/越界PASS；Grok34061字节/SHAdcbddcaac3419cf121419635269e0c9c458dc23c8f6f21dce6ea05742ce615bf，真实exit0、审计PASS/越界PASS。双方本次完整5路径12hunk报告均已读完；Grok无新finding，不覆写其正文。
r2沿用25项整批修复队列，22项保留r1已完成内容，补齐009/026/027；当前55个已修改源文件均与r2冻结after tree一致。依B4双PASS规则，001—018/024—030本次统一出队；19—23保持否；30项均为否、无待答。原证据与分类保留；下方r1/B2文字仅为历史。B4双PASS不是模块PASS，下一步B5-r2整模块三线双审。
外席验证口径：触及两测试类23/23通过，含027旧版红/新版绿对照及真实TEMP迁移；披露尝试Maven编译失败（不按其意图声称未调用），后改隔离javac。其目标目录mtime不能证明所有构建产物未动，本方已核55源文件与冻结树逐字一致；未迁移共享库的独立证据见其报告。其摘要把BSM-035写作映射内不准确，实际M006映射未新增该ID；本项仍按已定稿GEN判据及锁内上位原文归因，不扩锁。

## M006 B4-r1整批路由（V0.59）
两席全部diff报告已完整落盘并读完：本方审计FAIL/越界PASS；Grok审计PASS/越界PASS（真实exit0）。依双席合取，本轮审计FAIL、越界PASS，保留既有修复，不回滚整份diff、不进B5。
全部3项FAIL依据仅来自本方：CX-B4-01→009历史岗位授权未接续；CX-B4-02→026历史岗位占用漏算；CX-B4-03→027旧卡续充资金模式不一致。均有现存定稿ID且仍在当前修复队列；Grok无另加FAIL。整批直接回B3，沿用全部25项定稿队列，不分配新ID、不另开B2、不改变分类及拟修边界。B3已完成1次，r2尚未完成。
输入：modules/M006-b4-r1-self-raw.md SHA0a74c28f85b4b02037b97661594f0b62dd74875cb47a34eb07c80d424a42e463；modules/M006-b4-r1-grok-raw.md SHA2e8c6e4918f6a76a3e8e3bc9f7d152473aca69af5be9b8ef44dfd02512b132b3。两份原文保留；以下原B2状态为历史，当前队列25是/5否不变。

## M006 B2整批双PASS定稿（V0.58当前）

同一候选V0.57（SHA f20a175d6024c22e51bebd7395554b68abff8c37771f9d4972d28a13ccfb9fe9）已获本方self V0.14 PASS及Grok V0.05 PASS。Grok真实exit0，完整追加27349字节/SHA3e0c3186a830319afb36d09e02e6c18dfdfdc0d69fad03183c3edc21bb9c037a；整份147529字节/SHA4595d291ca5b293690c8ca2e9642f3b95948901242cb744fa4af7ad89835f5f2。原始B1及旧票前缀逐字保留，本次完整报告已读。30项均接受，未拼接旧票。

本V0.58仅将整批候选状态改为定稿，不改变任何条目分类、证据、拟修正文或队列：9冲突15细化6技术性，25是、019—023为否。027严格采用当前拟修边界；现场报本人手机号、短信验证码验证后按账单扣款，不生成储值付款二维码。三HTML、锁/映射及开关启用状态不修改，不重开M001。无需再次业务提问。

B3统一处理全部25个队列是的ID；尚未实施，完成次数0。旧章节的候选/待复核语句仅为历史时间点，当前状态以本节及唯一当前队列表为准。B2 PASS不等于模块PASS，后续仍须完整B4/B5双审。

## M006 补齐既有支付与短信接入边界（V0.57当前）

Grok V0.04完整299行/27145字节，真实exit0，整体FAIL；已接受027细化分类/队列和其余29项，唯一FAIL-1为027拟修正文缺少HTML1480/枚举210的预留接口、默认关闭、不开启未配置渠道、不伪造成功和既有短信验证约束。本方此前只写进self/调用提示，未完整放入B3唯一归因来源的027拟修正文，现接受该否决并补齐。没有新增业务裁决、人闸或判据。

本V0.57保持9冲突15细化6技术性、25是5否；唯一实质候选更新为027拟修边界，最新手机号/订单支付业务不变，三HTML/锁不改。当前027正文如下以各项明文约束实施，不以页首说明代替条目边界。全部30项仍须新双席复核，不拼接旧部分票，B3仍0。

## M006 B2接收Grok唯一分类否决（V0.56当前）

Grok V0.03完整302行/25137字节，真实exit0，整体FAIL；唯一FAIL-1为027应由添加改为细化。其明确撤回原B1 GR-07添加判断，接受最新线上充值/POS/已有订单余额支付/现场手机号短信验证范围及工程缺陷是、队列是；其他29项也全部接受。Codex根据HTML832、585、867现文核实后接受该分类更正，不以self V0.10 PASS覆盖外席FAIL，不把接受其余29项拼成全批PASS。

本V0.56唯一实质候选改动为027分类添加→细化；队列仍25是5否，无添加项待问；原questions保留实际答复与历史，A/B旧HTML申请继续撤回。下一票仍审全部30项，B3未执行。分类统计现为9冲突、15细化、6技术性、0添加。

接收事实纠错：本方先前接收说明误称交易表为stored_value_transactions，已回160实读JdbcCustomerRepository1181/1212/1223和V090建表确认实际为customer_stored_value_transactions；Grok原表名正确。本错误只在说明/提示中，未据此改源或建表。实现须复用真实既有表，原raw保留并由本方新追加纠错，不篡改Grok。

## M006 手机号短信扣款：当前裁决（V0.55）

最新原话：“那么出示二维码这个事情就不要这个，改成这个报手机号码，然后短信扣款。”。保留此前线上充值、POS、已有订单余额支付，只把现场二维码改为报本人手机号、短信验证码验证后按账单金额扣款。操作员并未取消充值或恢复钱包只读。

当前范围：保留客人发起合法储值充值和按已有订单/现场账单支付的限定授权通路。客人在充值页选择金额并拉起微信、支付宝或银联线上支付，服务端核验对应渠道支付成功确认后按该笔真实充值金额入账，重复确认不得重复加余额；另保留POS刷卡充值。App已有订单（如门票）可选择指定储值卡余额支付。现场已有消费明细和总价的账单（如卡座），客人报本人手机号，经短信验证码验证通过后按该笔账单金额扣款；不生成储值付款二维码，不以收到短信通知代替验证码通过。取消钱包页脱离订单/账单随意填写金额扣款，保留本人余额及逐笔记录查询。沿用既有卡归属/适用商户/本金赠金/冻结/幂等以及HTML585储值支付短信验证边界，不将此解释为App订单余额支付可豁免已有短信验证，也不通过任意客人权限或任意商户放宽解决范围拒绝。新要求落在现有HTML832储值、585手机号短信验证、867余额记录和1065支付方式之内；三HTML与冻结判据不修改、不启用新的判据、不重开M001。本范围为整批B2候选，未双PASS前不得实施。

两份旧HTML申请M006-027-A/B全部撤回，不等旧弹窗答复、不执行迟到的旧稿批准。A的储值扩写是可选同步，不是实现上述已明确HTML下位流程的必要条件；B的原文冲突随二维码撤回而消失。没有修改任何HTML、锁定义、映射、sidecar或全局基准，不触发LOCK-AMEND，不重开已发布M001—005。questions当前首答保留；其他实际答复仍合法。

旧V0.53 B2 Grok在途调用的输入已过时，仅保留历史结果；V0.54亦未定稿。下一票必须对当前V0.55全部30ID整批复核，其他29项保持，仍25拟修5登记。本方原旧票草稿没有落盘PASS，无当前B2双PASS，B3=0。

## 历史V0.54二维码方案及HTML申请（已撤回，以下待批/重审文字不再适用）

操作员最新原话：**点击“充值”** 这个页面需要可以拉起微信、支付宝、银联付款，按对应金额线上支付，而不是通过柜台支付收款。然后我们收到支付宝银联或者微信的返回确认就可以把这笔钱进入到客人的储值账户了。当然也可以通过pos机刷卡充值。  客人消费储值金额有两种情况第一种情况是a p p上已经有的订单。比如说门票。比如说这个下单后的在线支付。那这个客人直接可以用余额直接支付用储值卡的余额。有一种情况是客人点了一系列的东西。然后最后有一个总价。那客人，比如说卡座的消费。那客人可以通过出示二维码的方式。然后扣款。当然会收到短信验证码。

当前027答复规范化为保留，取消原V0.53“钱包只读/删除全部写通路”的方案；其他29ID证据与候选不变，整批仍30ID、25拟修5登记。保留并不消除现有工程缺陷，未经B2整批双PASS不得修。该项拟修范围：保留客人发起合法储值充值和按已有订单/现场账单支付的限定授权通路；撤回“钱包只读、关闭全部topup/settle/consume”的旧处理范围。充值界面拉起微信/支付宝/银联支付，服务端核验支付成功确认后按该笔充值真实金额入账，重复确认不得重复加余额；另保留POS刷卡充值。App已有订单可选指定储值卡余额支付；现场已形成明细和总价的账单走客人出示码、扫码关联该账单、短信验证码验证后按账单金额扣款。取消钱包页脱离订单/账单随意填写金额扣款，保留本人余额和逐笔记录查询。沿用既有卡归属/适用商户/本金赠金/冻结/幂等边界；不通过放宽为任意客人权限或任意商户解决当前范围拒绝。该范围为用户业务裁决下的待复核方案；受保护HTML两处精确修改待批准，不能提前将拟写文字当成冻结判据，不能在B2实施支付/UI/权限修复。

旧V0.53 B2 Grok于2026-09-05 16:33:07 UTC派出，仍待真实退出；收到新说明后，该输入已过时，结果仅保留历史，无论PASS/FAIL均不能作为V0.54定稿票。本方旧V0.53审查草稿未落PASS，不将旧草稿移作新票。新B2不派发，先等HTML人闸答复；在途旧调用只每5分钟检查交付，不据此推进。

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html

申请 M006-027-A（储值条款）：第832行仅替换以下开头片段。
原文：预付卡 / 余额：客人可充值到余额、消费时扣余额；充值必须支付成功才入账、不凭空加余额，账户可冻结/解冻。
替换为：预付卡 / 余额：客人点击“充值”后，可选择微信、支付宝或银联，按充值金额在线支付；系统收到对应支付渠道的支付成功确认后，才将该笔充值金额记入客人的储值账户，不凭空加余额；也支持通过 POS 机刷卡充值，充值必须支付成功才入账。客人使用储值余额消费分两种情况：①支付 App 中已有订单，例如门票订单，客人在订单支付时选择储值卡余额支付；②支付已形成消费明细和总价的现场账单，例如卡座消费，客人出示储值付款二维码，收款端扫码关联该笔账单，经手机号短信验证码验证通过后，按账单金额扣款。储值消费不通过在钱包页随意填写金额直接扣款。账户可冻结/解冻。
理由：把操作员最新确认的线上充值、POS充值和两种余额消费写入储值正文；原卡种、资金归属、退款、时间记录等后文全部保留。
同批将第2行“<!-- 文件版本：V0.08；更新日期：2026-09-01 -->”改为“<!-- 文件版本：V0.09；更新日期：2026-09-05 -->”，履行每次更新版本递增规则。
批准状态：待操作员实际批准。

申请 M006-027-B（二维码条款）：第585行仅替换末尾括号。
原文：（说明：客人付款走第三方微信、支付宝，或用账户预存款经手机号加短信验证支付，系统自己不生成“付款出示码”，故不在本自动刷新范围内。）
替换为：（说明：客人付款可走微信、支付宝、银联等第三方支付渠道，或使用账户预存款支付；账户预存款支付须经手机号短信验证码验证通过。卡座消费等已形成消费明细和总价的现场账单，客人可出示储值付款二维码，由收款端扫码关联账单，验证通过后按账单金额扣款；系统生成的储值付款二维码适用上述动态刷新规则。）
理由：删除与本次储值付款二维码要求冲突的禁止句；沿用同段既有动态刷新规则，保留原账户预存款支付的短信验证要求，其余正文不变。
批准状态：待操作员实际批准。

流程影响：两处分别涉及已冻结 BSM-035、BSM-006，最早映射模块均M001。若批准并同步HTML，依V0.17 §5须完整LOCK-AMEND五步：先改定义和映射、再sidecar、双席复核、再更新ledger基准，最后M001至当前M006改PENDING并从M001 B1重审。原报告、原提交及批准历史保留；当前尚未改HTML/锁或重开。获批HTML出处同步不计LOCK-AMEND-COUNT，但仍必须双席复核。

## M006 操作员裁决已录入（V0.53历史，已被最新说明替代）

操作员原话：“客人不是只能看吗？他要操作什么东西呢？储值钱包他只能看这个交易记录和余额，他要操作什么。” 对027记删除：客人钱包只查本人余额和交易记录；删除客人充值、确认入账、自行扣款入口。上一版把“有储值业务”推成“客人钱包可发起交易”的解释撤回，Grok原添加意见保留。问题文件V0.02首个非空答复为删除且附原话，不再弹窗；没有改业务HTML。

当前30ID完整队列表：25是、5否、待答0。027详细边界已更新，其他29项及两席分歧保留；本次只是B2候选，未双PASS，未执行B3。原V0.52中的待答/24项描述是历史，不代表当前状态。两席下一票必须审本V0.53全部30ID及原B1+接收补充证据，不仅审027。

## M006 首次 B2 整批候选（V0.52；尚未双席定稿）

M001—M005均已发布；当前仅M006（6/322，14文件2988行，52映射判据）。B1两席完整报告已读完并归并：本方CX-01—23、Grok GR-01—11，重合4项合并为30个正式ID；Grok未编号的兼容构造器删除建议也归入019。24项拟入队、5项仅登记、027待操作员答复。所有结论均为B2候选，不是双PASS；Grok原判及本方异议均保留，不得按候选先修。

输入原始B1：modules/M006-b1-self-raw.md前73231字节/V0.05/SHA256 1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4；modules/M006-b1-grok-raw.md全44940字节/714行/SHA256 96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071，真实exit0。两者均FAIL。self先完整封存再读Grok，接收后说明不算新独立票。Grok披露隔离target失败而写backend/target，故不称其验证完全隔离；未发现源文件或对方raw被其改读，本方独立验证使用临时classes，55/55通过及18个缺陷证据保持原有边界，未把该命令限制违例记为交付失败。B3=0，运行失败重跑=0。

B1后四值复核：actual、sidecar、ledger基准、指定Grok B1最后哈希行均为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。没有改锁/映射/三HTML/源码。BSM-004/005/007/035、PGM-029/041的已冻结HTML原文仅作为本模块既有GEN-005/006/017等判据下的上位证据，本候选不宣称它们新增进入M006映射；该解释亦交Grok B2审，不能单方扩大判据。

### M006 当前队列表（唯一当前值；V0.61整批候选，待本批双PASS）

| ID | 来源 | 候选分类 | 工程缺陷 | 修复队列归属 | 问题 |
|---|---|---|---|---|---|
| M006-001 | CX-01；GR-01 | 冲突 | 是 | 否 | 平台管理员无条件通配提前返回，绕过显式 DENY 和高危权限单独授予 |
| M006-002 | CX-02 | 冲突 | 是 | 否 | 同组织被当成同商户，允许操作另一商户员工的权限或薪资 |
| M006-003 | CX-03 | 细化 | 是 | 否 | 无组织的非管理员被当成全局主体，读全量或向任意组织建号 |
| M006-004 | CX-04 | 细化 | 是 | 否 | 名称/幂等键slug生成全局主键，与upsert联用覆盖既有跨组织对象 |
| M006-005 | CX-05 | 细化 | 是 | 否 | 修改已应用模板可直接给用户新增自己不持有的高危权限，绕过授权审批 |
| M006-006 | CX-06 | 细化 | 是 | 否 | 审批使用申请时快照，实际执行却重新读取变动后的角色/岗位模板 |
| M006-007 | CX-07 | 冲突 | 是 | 否 | 直接受益人可以审批发给自己的高危授权 |
| M006-008 | CX-08 | 细化 | 是 | 否 | 两个不同幂等键并发审批同一申请都生效，生成两份临时授权 |
| M006-009 | CX-09 | 冲突 | 是 | 否 | 岗位切换只追加新角色/模板，不使旧岗位派生权限失效 |
| M006-010 | CX-10；GR-09 | 细化 | 是 | 否 | 正式可创建的主管账号和已分配管理职级无法被审批级别解析器识别 |
| M006-011 | CX-11；GR-05 | 冲突 | 是 | 否 | 手工员工建号直接active及发密码，形成绕过两条注册审批路径的第三入口 |
| M006-012 | CX-12 | 细化 | 是 | 否 | 按用户ID查询有效权限只认商户范围，合法公司人员连同组织自身也被拒绝 |
| M006-013 | CX-13 | 细化 | 是 | 否 | 临时授权接受空权限集合和倒置时间窗，返回active但授权不存在或不可使用 |
| M006-014 | CX-14 | 细化 | 是 | 否 | 一次合法操作检查两个权限时，第一个检查消耗整份单次模板，第二个检查拒绝 |
| M006-015 | CX-15；GR-04 | 冲突 | 是 | 否 | 岗位建议和个人异常总结返回固定英文并被页面原样显示 |
| M006-016 | CX-16 | 冲突 | 是 | 否 | 业务阈值/窗口及授权风险对应审批层级固化在代码或启动配置，缺后台配置边界 |
| M006-017 | CX-17 | 细化 | 是 | 否 | 单次临时授权首次消费的留痕外键指错对象，报错后授权已被标记消耗 |
| M006-018 | CX-18 | 细化 | 是 | 否 | 平台管理员修改组织模板时把原owner改成null，全局化并使原组织失去管理权 |
| M006-019 | CX-19；Grok §6 AccessUser兼容构造器删除建议 | 技术性 | 否 | 否 | 记录容器防御复制和兼容构造器 |
| M006-020 | CX-20 | 技术性 | 否 | 否 | 凭证接口、Spring注入和按运行profile选择实现 |
| M006-021 | CX-21 | 技术性 | 否 | 否 | 密码BCrypt、UTF-8/SHA-256和JDBC时间参数转换 |
| M006-022 | CX-22 | 技术性 | 否 | 否 | HTTP映射、响应包装和仓储接口默认失败机制 |
| M006-023 | CX-23 | 细化 | 否 | 否 | 主体、权限集合和临时授权统计数据记录 |
| M006-024 | GR-02 | 细化 | 是 | 否 | 账号状态未校验且非disabled一律发active凭证；员工五态等同账号状态的主张待B2裁定 |
| M006-025 | GR-03 | 冲突 | 是 | 否 | 手工建号把手机号存为无密钥SHA-256并经AccessUser返回 |
| M006-026 | GR-06 | 冲突 | 是 | 否 | 岗位角色模板直接物理删除，未经过留存与删除审批 |
| M006-027 | GR-07；操作员最新说明 | 细化 | 是 | 否 | 保留线上充值和订单余额支付；现场按手机号及短信验证码支付账单 |
| M006-028 | GR-08 | 技术性 | 是 | 否 | 未知或UserDetails认证主体被填成staff/active |
| M006-029 | GR-10 | 技术性 | 是 | 否 | 手工建号空密码成功返回但无登录凭证 |
| M006-030 | GR-11 | 细化 | 是 | 否 | 列表声称每页50却返回全量；授权申请缺默认时间过滤 |
| M006-031 | B5-r2 CX-B5-01 | 冲突 | 是 | 否 | 不同组织使用相同请求编号时创建角色结果串用 |
| M006-032 | B5-r2 GR-01 | 冲突 | 是 | 否 | 非法账号类型错误提示夹带英文枚举 |
| M006-033 | B5-r2 GR-02（原票保留，B2已一致） | 细化 | 否 | 否 | B3新增政策源码未跟踪的构建意见；当前必要全源可编译，Grok确认只登记 |
| M006-034 | B5-r3 CX-B5-01 + GR-01完整并集 | 冲突 | 是 | 否 | 授权复核、最高权限和两处薪资币种报错残留英文；第4次B3已修，B4-r4双PASS出队 |
| M006-035 | B5-r4 CX-B5-01 | 技术性 | 是 | 否 | 平台管理员有效权限计算对权限字典逐项重复查询（N+1） |
| M006-036 | B5-r4 CX-B5-02 | 细化 | 是 | 否 | 无组织归属的非平台管理员复制模板时抛NullPointerException |

### 全部候选证据与拟修边界

以下M006代码文件名无路径前缀时，均指backend/src/main/java/com/complex/platform/access/。仅为当前14文件调用链必要而窄读的DAO/审批/DTO/测试/HTML/UI不视为开启其它模块；确需联动的修复必须逐行归因当前定稿ID，完成后后续模块照常全审。任何HTML改动仍需逐句申请，本批没有HTML修改提案。

#### M006-001 平台管理员无条件通配提前返回，绕过显式 DENY 和高危权限单独授予

- 来源：CX-01；GR-01；P1；B2双PASS定稿。
- 文件：AuthorizationService.java:33–46、189–196、444–446；AccessService.java:513–516。
- 已冻结判据：GEN-005、GEN-010、GEN-017。
- 证据与分类分歧：实跑 ADMIN_DENY_BYPASS=require=allowed; effective denies=[ops.production_readiness.manage]；ADMIN_HIGH_WITHOUT_EXPLICIT_GRANT=true。相同主体的有效集承认DENY，而实际执行和布尔鉴权均放行；授予检查也直接接受wildcard。业务地图588（锁内BSM-007原文）明确日常管理员不自动取得资金、敏感数据、删除、最终审批权限；GEN-005鉴权边界及GEN-017冲突分类直接适用。 Grok GR-01实跑finance.refund.create显式DENY仍被管理员放行；并指出currentSupervisorLevel对管理员直接返回3。后者与010共同修复，不因管理员身份自动取得最终审批资格，也不删除HTML明文要求的日常自动权限。
- 拟修边界：删除不分日常/高危且先于DENY的通配旁路；日常权限和逐项授予一致计算；USB最高权限与日常管理员分别按既有身份事实处理。不得借修复新增业务类别。

#### M006-002 同组织被当成同商户，允许操作另一商户员工的权限或薪资

- 来源：CX-02；P1；B2双PASS定稿。
- 文件：AccessService.java:201–210、365–371、559–568、969–972、1112–1120、1615–1623。
- 已冻结判据：GEN-005、BSM-028、BSM-073、PGM-048。
- 证据与分类分歧：实跑CROSS_MERCHANT_ROLE_ASSIGNED=RoleAssignmentRecord[userId=victim, roleId=role_low]，grantor在merchant_a、victim在merchant_b、同org_a，目标实际获得LOW权限。同一错误边界也出现在套模板、撤销临时授权、分配职级薪资、直接临时授权；通用requireSameOrganization只比较org。HTML要求商户只操作本商户。
- 拟修边界：统一验证目标用户的组织及调用方商户范围；平台越域例外须遵守CX-01已确认边界。逐一覆盖同根调用，不只补角色入口。

#### M006-003 无组织的非管理员被当成全局主体，读全量或向任意组织建号

- 来源：CX-03；P1；B2双PASS定稿。
- 文件：AccessService.java:111–114、283–286、399–402、807–816、843–848；AuthorizationService.java:430–441。
- 已冻结判据：GEN-004、GEN-005、BSM-028、PGM-048。
- 证据与分类分歧：实跑UNSCOPED_USER_LIST=[existing]（existing属org_b）；UNSCOPED_CREATE_OTHER_ORG=org_b。AccessRepository默认方法及JdbcAccessRepository的null参数明确代表无过滤，服务直接传actor.organizationId()；createAccessUser反而只在actor组织非空时比较。currentProjectScopeQuietly也把空组织转为空过滤；它未核对任何跨项目授权。
- 拟修边界：非管理员缺必要归属时拒绝，不能把缺失身份事实解释成全量权限；已有获授权全局岗位须使用可核验的授权事实，不能仅靠null。

#### M006-004 名称/幂等键slug生成全局主键，与upsert联用覆盖既有跨组织对象

- 来源：CX-04；P1；B2双PASS定稿。
- 文件：AccessService.java:138、188、312、865、1174、1214、1260、1746–1751。
- 已冻结判据：GEN-004、GEN-005、GEN-006、GEN-008。
- 证据与分类分歧：实跑USER_ID_OVERWRITE=usr_same_name moved org_b -> org_a（same-name与same.name）；ROLE_ID_OVERWRITE=role_cashier moved to org_a且原成员权限改变；TEMPLATE_ID_OVERWRITE=org_a（shared-key与shared_key）。JdbcAccessRepository95–131、159–193、360–397、675–714确认同ID无条件更新归属及内容；授权申请也使用同一有损slug。
- 拟修边界：创建使用不碰撞的内部ID，区分创建与修改；创建不得通过任何ID碰撞覆盖既有账号/密码/角色/模板/授权申请。复用已有幂等结果处理，不将幂等键充当跨租户主键。

#### M006-005 修改已应用模板可直接给用户新增自己不持有的高危权限，绕过授权审批

- 来源：CX-05；P1；B2双PASS定稿。
- 文件：AccessService.java:144–167、193–241、513–524。
- 已冻结判据：GEN-005、GEN-006、BSM-120、PGM-066。
- 证据与分类分歧：实跑TEMPLATE_UPDATE_UNAPPROVED_HIGH=authorized high without grant request; manager lacks high。模板先LOW应用到员工，再由无HIGH的同组织模板管理员update为requiresApproval=true的HIGH，员工requirePermission(HIGH)立即成功。JdbcAccessRepository769–780从实时模板items计算已应用权限，update既不核验授出能力也不走审批。
- 拟修边界：关闭模板修改直接向既有应用增权的旁路；对当前模板变更、既有应用与新增授权形成一致的权限/审批边界，保留合法减少权限和经审批授权路径。

#### M006-006 审批使用申请时快照，实际执行却重新读取变动后的角色/岗位模板

- 来源：CX-06；P1；B2双PASS定稿。
- 文件：AccessService.java:661–684、1165–1238、1287–1304、1321–1380。
- 已冻结判据：GEN-005、GEN-006、GEN-011。
- 证据与分类分歧：实跑APPROVAL_SNAPSHOT_DRIFT：snapshot=[HIGH]；applied=[HIGH,LOW]。pending保存permissionCodes/targetRoleId/templateId，但applyApprovedGrant重新resolve，未对照被审批对象及权限快照。岗位映射、模板权限或有效期在待审期间变化同样有此入口。
- 拟修边界：生效前对照完整被审批授权事实；漂移则拒绝并重新申请，或严格执行原已批准快照，不能批准A执行B。

#### M006-007 直接受益人可以审批发给自己的高危授权

- 来源：CX-07；P1；B2双PASS定稿。
- 文件：AccessService.java:653–685。
- 已冻结判据：GEN-005、GEN-017。
- 证据与分类分歧：实跑BENEFICIARY_SELF_REVIEW=applied。申请人requester，目标usr_admin，审批人usr_admin。下游SupervisorReviewService只禁止initiatedBy本人，没有识别AccessGrantRequestRecord.targetUserId。治理地图529（锁内PGM-029）明确申请人与直接受益人均不得审批自己的申请。
- 拟修边界：在任何下游审批写入之前拒绝目标用户本人作为审批人，同时保留既有申请人自审禁止。

#### M006-008 两个不同幂等键并发审批同一申请都生效，生成两份临时授权

- 来源：CX-08；P1；B2双PASS定稿。
- 文件：AccessService.java:652–685；AccessRepository.java:144–150。
- 已冻结判据：GEN-006、GEN-007、PGM-001。
- 证据与分类分歧：实际Java服务双线程：CONCURRENT_REVIEW=[applied, applied] temporary_grant_count=2。仓储替身仅在两个普通SELECT后同步，让两请求都观察pending；业务代码未改。JdbcAccessRepository718–727及JdbcPlatformRepository711–720均普通SELECT，最终upsert无pending条件，证实此交错与真实SQL契约一致。两个线程自己的幂等键不同，幂等服务不能替代对象级竞争控制。
- 拟修边界：利用现有事务与数据库对同一申请串行锁定或CAS转态，只有首次成功审批应用授权；禁止以JVM全局锁代替数据库并发事实。

#### M006-009 岗位切换只追加新角色/模板，不使旧岗位派生权限失效

- 来源：CX-09；P1；B2双PASS定稿。
- 文件：AccessService.java:1043–1101。
- 已冻结判据：GEN-005、GEN-006、GEN-017、BSM-120。
- 证据与分类分歧：实跑POSITION_CHANGE_OLD_PERMISSION_RETAINED=[HIGH,LOW]：old岗位只有LOW，new岗位只有HIGH，保存新岗位后员工仍有两者。旧角色和模板应用没有任何失效调用。治理地图658（锁内PGM-041）要求保存岗位/职责等变更时重算，旧岗位/旧范围派生权限同步失效。
- 拟修边界：同一事务撤销旧岗位派生授权并装配新岗位；区分独立显式授权，不误撤销员工仍合法持有的其他来源；跨范围和高危授予保留审批要求。

#### M006-010 正式可创建的主管账号和已分配管理职级无法被审批级别解析器识别

- 来源：CX-10；GR-09；P2；B2双PASS定稿。
- 文件：AuthorizationService.java:245–257；AccessService.java:52–53、838–841。
- 已冻结判据：GEN-002、GEN-005、BSM-028。
- 证据与分类分歧：实跑SUPERVISOR_RANK_LEVEL=30/0：已分配lvl_m10_upper，accountType=supervisor，currentSupervisorLevel仍0。解析器只识别supervisor_lN或platform_admin，而账户类型白名单不允许supervisor_lN；实际SupervisorReviewService145–151先以该0拒绝，尚未走rank检查。
- 拟修边界：将审批身份解析与现有受控的角色/职级事实对齐；不得为了让测试通过而把任意staff、最高rank或未授权账号自动当审批人。

#### M006-011 手工员工建号直接active及发密码，形成绕过两条注册审批路径的第三入口

- 来源：CX-11；GR-05；P1；B2双PASS定稿。
- 文件：AccessController.java:209–213；AccessService.java:829–889。
- 已冻结判据：GEN-017、BSM-084、BSM-085、PGM-032、PGM-033。
- 证据与分类分歧：实跑MANUAL_REGISTRATION=active/manual_admin/roles=0。该路径不读取员工申请/公司审批，未自动赋员工自助；对照StaffService178–223，既有审批路径在approved后才createUser并assign role_staff_self。HTML明确商户代申请后对应域公司批准、公司自有员工本人App申请后公司批准，自营归公司路径。 Grok GR-05原主张：后台建号是HTML允许的入口，生效员工必须有员工自助；Grok B1不接受将其视为注册两路径冲突。收后核实业务地图582确实列出“新建员工账号”，但它只描述分步表单，并未明文豁免1433注册审批。两席分歧完整保留，不以本方单票定稿。
- 拟修边界：保留HTML582要求的后台引导式新建操作；封闭员工未经对应公司注册审批即激活并发放可用凭证的旁路，复用已有注册审批/批准后自动员工自助装配。后台可以辅助创建，不把“可新建”解释成“免审批”。只修员工路径；外部协作机构账号按其原有类别处理。GR-05要求的每位获批员工自动自助与本项共同验证，不用给未获批账号直接赋自助来掩盖审批问题。

#### M006-012 按用户ID查询有效权限只认商户范围，合法公司人员连同组织自身也被拒绝

- 来源：CX-12；P2；B2双PASS定稿。
- 文件：AuthorizationService.java:86–102、347–356。
- 已冻结判据：GEN-002、GEN-005、BSM-028。
- 证据与分类分歧：实跑ORG_STAFF_OWN_PERMISSIONS=MERCHANT_SCOPE_REQUIRED。actor org_a且merchant=null，target为同一个org_a用户。此接口已经由Controller224校验access.user.view；之后无条件currentMerchantScope使组织级管理路径不可用。
- 拟修边界：按实际公司/商户归属校验目标；无效归属拒绝，合法同组织管理请求可读，不能为修功能改成空scope全放行。

#### M006-013 临时授权接受空权限集合和倒置时间窗，返回active但授权不存在或不可使用

- 来源：CX-13；P2；B2双PASS定稿。
- 文件：AccessService.java:1104–1142、1241–1253、1688–1709。
- 已冻结判据：GEN-002、GEN-004、GEN-008。
- 证据与分类分歧：实跑INVERTED_TEMPORARY_WINDOW_ACCEPTED=active，EMPTY_TEMPORARY_PERMISSION_ACCEPTED=active/0，员工有效集为空。JdbcAccessRepository612–635对空列表一次INSERT也不执行。模板和待审临时申请同样缺日期先后验证；DTO未声明约束，@Valid不能补足。
- 拟修边界：在服务公共写边界校验非空有效权限、目标存在、有效期先后；非法输入拒绝，不能返回成功的空授权/永远无效授权。不擅自设置业务期限上限。

#### M006-014 一次合法操作检查两个权限时，第一个检查消耗整份单次模板，第二个检查拒绝

- 来源：CX-14；P1；B2双PASS定稿。
- 文件：AuthorizationService.java:33–73；AccessController.java:137–139。
- 已冻结判据：GEN-002、GEN-006、GEN-007。
- 证据与分类分歧：实际调用AccessController.reviewGrant：SINGLE_USE_TWO_GUARDS=PERMISSION_DENIED consumed=[single_review_template]。同一份单次模板已含review及supervisor.approve，两权限起初均有效；业务服务未被执行，模板已消耗。仓储以模板应用为消耗单位；不是两次业务操作。
- 拟修边界：同一业务请求的一组权限先完整核验，再按授权来源原子消耗一次；确保无法因重复检查提前烧掉唯一授权。保留并发第二消费者拒绝的现有语义。

#### M006-015 岗位建议和个人异常总结返回固定英文并被页面原样显示

- 来源：CX-15；GR-04；P2；B2双PASS定稿。
- 文件：AccessService.java:759–774。
- 已冻结判据：GEN-015、GEN-017、PGM-001、PGM-066。
- 证据与分类分歧：实跑SUMMARY_USER_VISIBLE_TEXT为两句英文。AccessGovernancePage.tsx341、454–455直接渲染row.recommendation/row.summary；此为成功响应，GlobalExceptionHandler的错误翻译不参与，前端测试却以中文fixture掩盖实际返回。 Grok GR-04同时把源码中的valid_until is required等异常字符串列为英文展示问题。本方收后核实GlobalExceptionHandler48–61会经FieldLabels.translate翻译；源码英文≠对客英文，不做无依据全包替换。成功响应两句英文确实直接显示，仍为冲突/待修；异常分支以真实响应而非字符串搜索判定。
- 拟修边界：将这两条用户可见业务说明改为中文；保留内部权限编码，禁止把与此无关的英文代码/标识批量替换。

#### M006-016 业务阈值/窗口及授权风险对应审批层级固化在代码或启动配置，缺后台配置边界

- 来源：CX-16；P2；B2双PASS定稿。
- 文件：AccessService.java:76–77、107–108、729–733、1406–1419。
- 已冻结判据：GEN-017、BSM-001。
- 证据与分类分歧：汇总默认30天、阈值3，最小值被Math.max静默钳到1，任意请求可传Integer.MAX_VALUE使本来存在的异常总结消失；源码riskRank直接把critical/high映射到3/2层级。这里的“频繁”“审批层级”改变业务判断，不是线程数之类技术设置。BSM-001要求业务阈值/时长/次数/档位后台配置、上下限及超限拒绝。
- 拟修边界：复用项目已有业务配置/审批策略承载对应政策值和边界；查询筛选与政策阈值分清，不由查询参数改写政策，不引入另一套配置体系或新业务分类。

#### M006-017 单次临时授权首次消费的留痕外键指错对象，报错后授权已被标记消耗

- 来源：CX-17；P1；B2双PASS定稿。
- 文件：AuthorizationService.java:66–72；AccessRepository.java:178–182（实现依赖JdbcAccessRepository802–843）。
- 已冻结判据：GEN-002、GEN-006、GEN-008、GEN-011。
- 证据与分类分歧：真实PostgreSQL complex_test中仅建立3张连接级TEMP表，按V003现有外键结构调用当前源码JdbcAccessRepository：TEMPORARY_SINGLE_USE_LOG=23503，CONSUMED_AFTER_LOG_FAILURE=true。临时grant UUID被写入引用permission_template_applications的application_id；查询当前全部受控迁移，仅V003定义该外键，没有后续解除/替换。控制器调用鉴权发生在业务service事务之前，该仓储方法本身无事务。
- 拟修边界：让临时授权留痕引用正确的授权来源，并使消耗与留痕同成同败；保留真实来源信息，不通过丢日志/吞异常/伪造模板应用解决。仅修依赖实现必需范围，后续所属模块仍完整审。

#### M006-018 平台管理员修改组织模板时把原owner改成null，全局化并使原组织失去管理权

- 来源：CX-18；P2；B2双PASS定稿。
- 文件：AccessService.java:161–166。
- 已冻结判据：GEN-005、GEN-006、GEN-016。
- 证据与分类分歧：实跑ADMIN_UPDATE_OWNER=null：原org_a模板经管理员改内容后ownerScopeType变platform且ownerScopeId=null。注释写Preserve existing owner，代码却只按当前actor决定归属。
- 拟修边界：读取并保留现有模板归属；内容编辑不隐含转移所有权，修改不存在的模板应明确报不存在。

#### M006-019 记录容器防御复制和兼容构造器

- 来源：CX-19；Grok §6 AccessUser兼容构造器删除建议；登记；B2双PASS定稿。
- 文件：AccessGrantRequestRecord.java:32–34；AccessUser.java:20–22、24–34。
- 已冻结判据：GEN-001、GEN-003、GEN-017。
- 证据与分类分歧：仅List.copyOf防外部修改、null转空及已有构造调用兼容，不引入员工资格/计费/审批规则。全部记录字段仍按员工/授权上位需求归细化，此登记仅指Java容器与构造机制；48个当前源编译和55测试实跑覆盖调用。 Grok §6另建议删除7参构造器，但其B1未证明无调用；本方48源fresh javac及StaffService已证明既有调用。候选不做为删而删的全仓调用改写；该未编号建议已归并于本ID，待Grok复核。
- 拟修边界：不删不问，不进修复队列。

#### M006-020 凭证接口、Spring注入和按运行profile选择实现

- 来源：CX-20；登记；B2双PASS定稿。
- 文件：AccessCredentialProvisioner.java:5–11；DevAccessCredentialProvisioner.java:13–24；JdbcAccessCredentialProvisioner.java:15–24；AccessService.java:68–109。
- 已冻结判据：GEN-001、GEN-003、GEN-013、GEN-017。
- 证据与分类分歧：dev/local内存认证桥与prod JDBC凭证桥确有两个实现，接口并非为了未来扩展；无依赖时的noop用于轻量测试装配，prod有实际bean。Spring装配、包级测试开关不新增业务授权规则。生产Autowired路径grantApproval固定true；不能把测试构造器false当生产逃生开关。 GR-10空密码伪成功不归入本项通过，单列029。
- 拟修边界：只登记工程装配，具体业务账户旁路按CX-11处理。

#### M006-021 密码BCrypt、UTF-8/SHA-256和JDBC时间参数转换

- 来源：CX-21；登记；B2双PASS定稿。
- 文件：JdbcAccessCredentialProvisioner.java:3–13、24、50–58；AccessService.java:911–921。
- 已冻结判据：GEN-001、GEN-010、GEN-017。
- 证据与分类分歧：密码仅写BCrypt哈希；SecureRandom、UTF-8、摘要/时间转换为数据处理机制，不新增业务资格。手机号摘要本身不是已证明的匿名化或完整PII加密方案，不以此宣称全项目隐私合规通过。 Grok GR-03否定手机号哈希充当加密；其数据用途问题已单列025为待修，故本项仅登记BCrypt/标准库/UTF-8/JDBC适配机制，不再把手机号存储口径当作本项无缺陷放行。原CX-21全文保留于独立raw。
- 拟修边界：不新增依赖、不改工作因子，不进修复队列；业务数据隐私按实际字段和用途继续由相应模块审。

#### M006-022 HTTP映射、响应包装和仓储接口默认失败机制

- 来源：CX-22；登记；B2双PASS定稿。
- 文件：AccessController.java:19–39及各路由包装；AccessRepository.java:83–125等默认接口方法。
- 已冻结判据：GEN-001、GEN-003、GEN-013、GEN-017。
- 证据与分类分歧：MVC/ApiResponse/PageResponse仅承载现有权限/员工业务，不构成第四来源。UnsupportedOperationException使未实现写能力显式失败；源码构建证明当前Jdbc实现可装配编译，未声称所有默认方法都在生产使用。 Grok GR-11假分页具体问题单列030，本项对包装类型的技术分类不替代其行为审计。
- 拟修边界：只登记包装/接口机制；权限边界、空成功及并发缺陷分别按已有CX处理。

#### M006-023 主体、权限集合和临时授权统计数据记录

- 来源：CX-23；登记；B2双PASS定稿。
- 文件：AccessActor.java、AccessGrantPositionSummary.java、AccessGrantSummary.java、AccessGrantUserSummary.java、EffectivePermissions.java及其他记录字段。
- 已冻结判据：GEN-001、GEN-017、BSM-028、BSM-120、PGM-066。
- 证据与分类分歧：主体归属和单功能权限对应BSM-028/BSM-120；频繁临时授权汇总的原文位于治理地图659（锁内PGM-041），有明确上位需求，不能当业务添加删除或询问。纯记录对象无独立I/O副作用；具体计算错误已单列。
- 拟修边界：只登记；不因Java record或内部英文标识删除既有业务数据结构。

#### M006-024 账号状态未校验且非disabled一律发active凭证；员工五态等同账号状态的主张待B2裁定

- 来源：GR-02；P2；B2双PASS定稿。
- 文件：AccessService.java:870；JdbcAccessCredentialProvisioner.java:30–33；AuthorizationService.java:217–218、444–452。
- 已冻结判据：GEN-004、GEN-008、GEN-017；Grok原引BEQR-157、PGM-001。
- 证据与分类分歧：Grok原主张（冲突）：active/disabled发明英文，必须改成BEQR-157的待入职、在职、试岗、停岗、离职；非disabled即凭证active。收后核实AccessUsersPage339–342已有启用/待激活/禁用三种“账号”选项，177/280用formatBusinessStatus显示中文；AccessUser也承载merchant/outsourced_vendor，不能把所有账号都当五种雇佣状态。StaffProfileRecord另有registrationStatus和status，但本席未据此宣称员工五态完整实现。已证真实缺陷是未知状态不拒绝、invited等非active状态仍生成active凭证，前后账号/凭证口径不一致。ProductionAuthService还检查用户active，所以不能从凭证active单独宣称离职者已实际登录成功。原意见全量保留，细化分类和收窄提案须由Grok B2明确同意。
- 拟修边界：在账号写入口校验现有账号状态集合，凭证与账号状态一致、非启用不能发可用凭证；保留已存在中文显示。员工是否已注册审批归011，不以将active改成中文在职解决。拒绝未知值的正常反例及active/invited/disabled正负路径必须验证。

#### M006-025 手工建号把手机号存为无密钥SHA-256并经AccessUser返回

- 来源：GR-03；P1；B2双PASS定稿。
- 文件：AccessService.java:872、911–922；AccessUser.java:14；直接依赖JdbcAccessRepository.java:163–188、999及V131。
- 已冻结判据：PGM-059、GEN-010、GEN-017。
- 证据与分类分歧：Grok原文：治理地图870“客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。”当前phone_hash是无密钥不可逆SHA256，Python反例仅证明算法不是加密。收后核实AccessUser.phoneHash直接进API DTO，主代码范围内只发现存取和返回，没有按该字段查号的使用；UI310还宣传“后端仅留哈希”。不能将此当已证明匿名化，也不能谎称可从既有摘要还原手机号。现有common/crypto/PiiCipher使用AES-GCM、BlindIndex使用独立密钥HMAC，均为可复用实现事实，非新的业务权威。
- 拟修边界：删除把无密钥手机号摘要当安全PII存储/对外返回的做法。优先收窄这个手工入口中没有真实消费用途的重复手机号采集/摘要暴露；确需继续保存的手机号复用既有PiiCipher加密存储，匹配用途复用BlindIndex，绝不把加密或HMAC说成原来的SHA。B3只改本入口及必要DAO/字段约束和对应测试，不扩成全仓个人信息整改、不臆造旧手机号回填、不在生产库做数据清理；具体最小方案须与B2口径一致。

#### M006-026 岗位角色模板直接物理删除，未经过留存与删除审批

- 来源：GR-06；P1；B2双PASS定稿。
- 文件：AccessService.java:466–482；AccessController.java:176–180；直接依赖JdbcAccessRepository.java:519–524。
- 已冻结判据：GEN-006、GEN-011、GEN-017、BSM-003。
- 证据与分类分歧：Grok原判细化+完整性缺陷：直接DELETE、无占用检查、无幂等键；并拒绝用未映射BSM-004为冲突依据。收后实读业务地图563–578：“下面这几条…整套系统都遵守”；570–571要求先存档、审批、软删除且库内可恢复；572检查占用。该原文已经在冻结定义区，未新增判据/映射。本候选以映射GEN-006/017及BSM-003明确总原则举证，提议冲突分类，由Grok B2裁定，不改其原报告。
- 拟修边界：删除直接物理DELETE旁路；复用项目已有存档/审批/留存机制承接岗位配置删除，核实占用、原因、审批身份及恢复可查。不能只把DELETE改成status=inactive就宣称删除审批已完成，不新建另一套审批体系。相关直接DAO/调用和最小验证归本ID，后续模块仍按计划全审。

#### M006-027 保留线上充值和订单余额支付；现场按手机号及短信验证码支付账单

- 来源：GR-07；P1；操作员保留限定能力；原HTML两申请已撤回；Grok V0.03已改判细化，Grok V0.04范围FAIL已接受并补齐条目正文；当前30项已获self V0.14/Grok V0.05整批双PASS定稿。
- 文件：AuthorizationService.java:21–25、42–47、449–452；直接消费者apps/web/src/customer/pages/WalletPage.tsx:66–94、141–175及client/customerService.ts:139–146；MemberWalletController.java:56–85。
- 判据与裁决：Grok原引BSM-114、GEN-005、GEN-017；最新保留裁决；已有冻结BSM-035储值及BSM-006手机号短信验证原文作为GEN-005/017上位证据，不扩模块映射。
- 证据与分类分歧：Grok GR-07原添加意见作为历史保留；本席B2 V0.03明确撤回原添加判断，按既有HTML上位改判细化，本候选已据唯一FAIL-1同步。操作员先明确微信/支付宝/银联线上充值、支付成功确认后入账及POS刷卡充值；已有App订单可用余额支付，现场已有总价账单消费。其后最新逐字说明：“那么出示二维码这个事情就不要这个，改成这个报手机号码，然后短信扣款。”撤回出示二维码，采用报本人手机号和短信验证码验证后扣账单金额。原V0.53只读方案与V0.54二维码方案均不执行。现有WalletPage66—77仅创建充值意向并提示到柜台，79—94提交金额及固定reason=钱包消费；StoredValueConsumeRequest没有订单/账单/短信绑定，钱包页未见记录展示。真实消费还受currentMerchantScope拒绝风险，未实跑成功消费，不把界面有按钮当成交易已经成功。HTML585现有手机号+短信验证，832现有客人充值、支付成功入账、消费扣余额，867要求逐笔记录；取消二维码后不需改HTML。Grok已明确接受上位依据和拟修范围并要求分类细化；当前已改细化，不再按添加等待操作员。
- 当前拟修边界：在HTML1480及枚举210既有“预留接口、后台开关默认关、不阻塞自研、不伪造结果”阶段内，保留客人合法充值和按订单/账单消费的限定权限桥，修复平台侧流程与结果衔接；不把本项当成开启未配置支付/短信能力或接通真实厂商的授权。充值页提供微信、支付宝、银联选择，经既有createPaymentIntent及PaymentGatewayPort申请支付；仅在真实渠道已配置且后台显式启用时使用真实拉起信息发起支付。开关关闭、RESERVED或NOT_CONFIGURED时应明确提示未启用/未配置，不能显示已支付、不能入账，不打开payment.gateway.enabled，不修改ReservedPaymentGateway来伪造SUCCEEDED，不拿测试期markPaymentIntentSucceeded或前端成功提示代替真实渠道回执。只有服务端核验对应渠道的真实支付成功确认、金额和已绑定本人卡范围后，才复用settleTopup/既有入账内核记入该笔充值；重复确认不得重复加余额，客户端不得自行宣称收款成功。POS刷卡充值复用confirmOfflineTopupAndSettle等既有柜台/线下确认链，不另造支付通道。App已有订单（如门票）由客人选择指定储值卡余额支付；现场已形成消费明细与总价的账单（如卡座）采用报本人手机号、短信验证码验证通过后按该笔账单金额扣款，不生成储值付款二维码。两类余额支付都绑定真实订单/账单与本人卡并执行HTML585的手机号短信验证，不豁免App订单。复用既有CustomerOtpVerifier等短信验证接口；短信能力未配置时拒绝验证/扣款，不以“已发短信”“已送达”或变动通知notifyStoredValueAfterCommit当作验证通过，不以本地测试固定码冒充生产短信验证。取消钱包页脱离订单/账单随意填写金额的consume入口及对应孤立调用，保留限定合法customer.stored_value.topup/consume通路，不仅删除权限桥，也不放宽任意客人/商户授权。保留本人余额和逐笔记录，查询复用真实既有customer_stored_value_transactions及现有卡/账务数据，不建平行台账。原有卡归属、适用商户、本金赠金、冻结与幂等边界保持。三HTML、锁定义、映射及开关启用状态不修改，不重开M001；整批B2双PASS前不实施。
- 操作员答复规范化：保留；最新原话及两次修改的完整上下文在questions/M006-027.md V0.06。无待批HTML修改。

#### M006-028 未知或UserDetails认证主体被填成staff/active

- 来源：GR-08；P2；B2双PASS定稿。
- 文件：AuthorizationService.java:127–150。
- 已冻结判据：GEN-004、GEN-005、GEN-017。
- 证据与分类分歧：Grok实际harness把UserDetails变成ID=ops、ACCOUNT_TYPE=staff、ORG=null、MERCHANT=null、STATUS=active。收后核实SecurityConfig81–82禁用basic/form，110–113的UserDetailsService始终抛不存在；两种员工Bearer过滤器分别供UserRecord/AccessActor，客人过滤器供AccessActor。因此不能夸大为已证生产网络攻击，但此默认身份补全会让未建立真实归属的主体进入业务函数，并与003空归属问题相连。无业务发明：框架principal到既有主体的身份适配，不新增业务角色。
- 拟修边界：删除猜测staff/active的fallback，只有既有可信Actor/UserRecord正常进入，未知主体显式拒绝。必要测试改用真实认证主体，保留员工/客人合法路径；不创建第三种身份提供者。

#### M006-029 手工建号空密码成功返回但无登录凭证

- 来源：GR-10；P2；B2双PASS定稿。
- 文件：AccessService.java:878–879；DevAccessCredentialProvisioner.java:27–28；JdbcAccessCredentialProvisioner.java:30–31。
- 已冻结判据：GEN-004、GEN-009、GEN-017。
- 证据与分类分歧：Grok记录生产源码空白password直接return，建号仍返回成功。收后核实AccessUsersPage90–92、301–302已经明确初始密码必填、手动建号需可登录；因此是后端未执行已有输入契约，不是新设业务密码要求。两适配器确实都支持此空返回，轻量测试默认noop不等于生产凭证成功。
- 拟修边界：在真实建号/需发凭证的共同业务入口、任何写入前拒绝空初始密码，正常密码必须证实对应凭证已发；011未审批员工不得靠密码校验变成直接生效。适配器必要失败显式化，保留已有无需新凭证的合法调用，不做全仓认证重构。

#### M006-030 列表声称每页50却返回全量；授权申请缺默认时间过滤

- 来源：GR-11；P2；B2双PASS定稿。
- 文件：AccessService.java:1733–1734、listGrantRequests相关方法；AccessController.java相关GET入口。
- 已冻结判据：GEN-008、GEN-012、BSM-028。
- 证据与分类分歧：Grok原分类技术性，原主张：page(items)一律page=1,pageSize=50,却无切片；windowDays=null无时间过滤。收后Controller读取证实多数GET没有page参数，而UI依赖全量选项。两类问题须区分：超过50项时元数据确实不实；不能简单截前50导致剩余角色/权限永远无法选到，也不能无证据把所有全量小字典称资源灾难。列表是权限管理既有上位需求的下位实现，提议细化，由B2双席复核。
- 拟修边界：修正分页元数据与实际取数契约；如采用分页，接口须能访问后续记录，必要消费者同步，禁止无出口截断。按真实列表用途/负载限制授权申请查询及数据库取数，复用现有时间窗口/分页能力；不得把技术分页上限当成新的业务政策或任意设置频繁阈值。

### B2 当前下一步

M006 IN_PROGRESS，B2整批双PASS；按全部25个队列是的已定稿ID统一进入B3，完成after tree后才计B3第1次。无待答、无待批HTML、无LOCK-AMEND、M001不重开。

---

## M005 B5-r1收口（V0.51当前）

两席全四文件232行、127脚本、9编译项、18GEN均PASS；Grok真实exit0，报告39052字节/416行/SHA cf0fdfa8f179392f93161e64afed533cea92fd84da79a5d1eda478a4451652a3；本方独立含补核前60471字节/SHA2dab6ab2974fa7dba20ee0e9246a3dfedd6376f69e4ca34dfdac5312cba21ec7，收后V0.03说明不作为新独立票。无新finding或待答，12ID队列全否；B3=1、运行失败0。四值原23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de全等、COUNT0。先写ledger PASS再模块收口，按已有授权进入发布。其他历史段以各自当时时态保留。

## M005 B4-r1双PASS接收（V0.50当前）

本方完整独立raw前17530字节/SHA2092d5f24b1b23cb86a7d22d967917fd35d53aac570ece461f2bcb649c9cdf2d为PASS，Grok完整raw32356字节/438行/SHA46f9501ac070309af410587efed1384e8919f502d9cc344e348affff4de10c52为PASS，真实exit0；两席越界均PASS。完整3529字节/8hunk/4路径diff逐字核验，six IDs 001—006均已修且统一出队为否；007—012继续否。B3=1、运行失败0、无添加/HTML申请/未答。四值原23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de全等，COUNT0。开始B5前保持源四文件和35scratch不变。本方raw末尾追加V0.02接收事实，原独立前缀不改。

下文B2候选/待修/未实施语句是历史时间点，唯一当前队列表已同步；当前状态以本节及上方状态行为准。完整B5双PASS及四值复核前，不写模块PASS。

## M005 B2整批定稿（V0.49；业务修复内容沿用已双PASS的V0.48候选）

本批正式定稿：本方B2 V0.05 PASS（原前43563字节保持，全47896字节/SHA0ad9ec052aec7837ef6749549e977b4457016b3a965c76d6e84378c689f69817）；Grok B2 V0.03真实exit0/PASS（追加前69679字节保持，全83350字节/SHAedd292d2d78e58bd141fcfcf20c3bf1237145f89f5d7b56e4f5e106cc960bf08；本次追加13671字节/SHAf17bca44f886939db6c260989deaf2962f132872af8a65e62a920d386255faec，完整81行已读）。双方针对V0.48/SHA927eb2ced3766ac3c54f2bc944ba0da3f5578e300384777d52e7e3d1d5375e22全部12ID无分歧，六是六否，005/006实质问题本次复验并接受。无添加/HTML申请/未答。以下原候选细节作为定稿修复范围沿用；历史“候选”语句指当时状态，本段为当前状态。B3尚未执行。

输入：本方原独立39710字节/V0.02/SHA44961cabea4ca790edf19d0960d001aa34161c7c47eb35aa1d27b028cbc1fca3（CX01/02），原票之后追加的接收补核不作为新盲审票。self当前V0.04/full SHA 552242925e80e266a7dd5b7aaf3c46eba2beb0d871edf50b30879bded93be783。Grok有效新完整报告从modules/M005-b1-grok-raw.md byte36359起，33320字节/SHAd30dab7380be95bfe1cdfe293a7e19f71c851825516a1da7937f075014c65ac7，FAIL/GR01/02；全69679字节/SHAbdcd2b52b3970093c2d1f4b5817e869f69ff7beae75521601b1603e63f52abc2。旧受影响Grok B1票不参与合取，但其实质疑点经本方收后验证并在本批005/006登记，不静默删除。B1后四值全等原23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；COUNT0。

本模块4源223行、128根脚本、18GEN全部审完；B3=0、运行失败0、无未答问题。当前整批全技术性，无添加、无业务HTML修改申请。工程缺陷进入队列，只登记技术内容当场为否；以下仍是候选，两席B2双PASS后才可整批B3。

### 正式ID与完整差集

| ID | 来源 | 分类 | 工程缺陷 | 当前修复队列 | 命题 |
|---|---|---|---|---|---|
| M005-001 | 本方CX-01；新Grok GEN013只验证当前环境 | 技术性 | 是 | 否 | 根engines >=22宽于既定构建/测试依赖支持范围 |
| M005-002 | 本方CX-02；新Grok将环境当操作员输入未判缺陷 | 技术性 | 是 | 否 | REAL_BACKEND_WEB_PORT未校验即进入shell命令 |
| M005-003 | 新独立Grok GR-01 | 技术性 | 是 | 否 | 15条live-evidence在默认套件跳过，声明的real-backend入口又未收集 |
| M005-004 | 新独立Grok GR-02；旧Grok GR-03；本方收后git grep核实 | 技术性 | 是 | 否 | smoke:p0-ops无已证调用的重复别名 |
| M005-005 | 旧受影响Grok GR-01观察；本方收后实际隔离复现 | 技术性 | 是 | 否 | 不设REAL_BACKEND_E2E直接跑真实配置时全skip却exit0 |
| M005-006 | 旧受影响Grok GR-02观察；本方源调用展开核实 | 技术性 | 是 | 否 | 默认E2E服务绑定所有接口，超出本地测试消费需求 |
| M005-007 | 本方T01；Grok两报告根字段覆盖 | 技术性 | 否 | 否 | 包协议字段、工作区、private/type、应用semver；无业务发明 |
| M005-008 | 本方T02；Grok全部128scripts覆盖 | 技术性 | 否 | 否 | 既有脚本/75node目标/108npm转发图；缺陷只归001—006 |
| M005-009 | 本方T03；Grok依赖及packageManager覆盖 | 技术性 | 否 | 否 | 现有Playwright/yaml真实消费、声明npm版本，不无谓升级依赖 |
| M005-010 | 本方T04；Grok mock配置覆盖 | 技术性 | 否 | 否 | mock测试超时/报告/globalSetup/Chromium/本地复用；003/006除外 |
| M005-011 | 本方T05；Grok real配置覆盖 | 技术性 | 否 | 否 | 真实测试报告/超时/Chromium/proxy/strictPort与wrapper消费；002/003/005除外 |
| M005-012 | 本方T06；Grok tsconfig全部覆盖 | 技术性 | 否 | 否 | 九个编译器选项及extends消费，实际类型正负验证；不改源 |

### 001 — 根Node相容范围（P2；GEN002/013；类型4）

位置package.json:140—141。根>=22放行22.0/22.12/23，现有jsdom要求^22.13.0或>=24（另支持20.19但根已>=22无需扩大支持），Vite等有22.12下界；实际npm checkEngine根通过而jsdom在22.0/23报EBADENGINE。逐锁记录相容统计22.0有26不相容、22.12有1、23有9，22.13/24均0。未声称跑过Node22/23二进制。

候选最小修复：package.json engines.node改为“^22.13.0 || >=24.0.0”。package-lock.json的packages[""].engines.node同值同步，只同步该根镜像；此文件虽在M004已审，作为本ID必要直接镜像修改，不重开M004、不改任何依赖包版本/URL/SRI/其他字段，不改冻结模块映射。B4完整diff与B5该镜像一致性检查纳入本ID。两文件自身版本：package.json首次增加documentVersion V0.01；package-lock.json已documentVersion V0.01→V0.02。应用version0.1.0、lockfileVersion3保持。仅此同文件版本维护归001，不把全锁重生成为修复。

### 002 — 端口输入与shell（P2；GEN004/009/010/013；类型4）

位置playwright.real-backend.config.ts:4—5、26。当前真实WebServerPlugin先new URL，再shell:true执行命令；带“合法端口/; printf 无害标记; #”的端口值通过URL并执行标记。完整干净exit0探针在self V0.02，noop npm只避免启动业务进程，其余原命令实际执行；首次辅助器留存线程被精确终止未计PASS，随后干净验证已exit0。仅证明可控制本地/CI环境者的命令构造风险，未证明公网攻击路径。

候选最小修复：const webPort = Number(process.env.REAL_BACKEND_WEB_PORT ?? "5174"); 在构造URL或命令前要求Number.isInteger(webPort)且1<=webPort<=65535，否则throw明确中文错误。后续模板插值使用已转换的number。用原生Number收敛输出，不增加库/通用shell框架；不额外要求字符串格式规则。同文件首次加“// 版本：V0.01”，只加一次。

### 003 — 测试选择器对应现有真实用例（P2；GEN001/002/003/009；类型2/4）

位置playwright.config.ts:4—5与playwright.real-backend.config.ts:8—9；package.json:82—86只是既有调用事实，不新增入口。已有customer-account-lifecycle-live-evidence 2条、leasing-contract-live-evidence 12条、order-tickets-live-evidence 1条，共15条默认因flag未设skip；现有real配置只收web-ui-real-backend 3条。新Grok实际无服务器15skip证明，默认47/9与真实3/1列表不等于都执行。

候选最小修复：默认testIgnore改为["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"]；真实testMatch改为同一数组。两数组仅匹配当前上述四个已存在文件，无新增业务用例、无新npm脚本、无改fixture业务断言。预期默认32条/6文件、真实18条/4文件，两个集合不交叉、联合等于现有50条/10文件。沿用既有REAL_BACKEND_E2E=1的npm/seeded入口；不把选择器修复宣称为18条业务用例已通过。默认配置首次加“// 版本：V0.01”。

### 004 — 删除无用途别名（P3；GEN001；类型4）

package.json:48精确删除"smoke:p0-ops": "npm run test:backend"这一项，保留smoke:p0/test:backend及backend README要求的防回流入口。新独立Grok与本方收后git grep（排除audit报告）均仅命中定义。本方最初对所有别名泛称便利性的说明收窄，不能把未证用途当保留理由。版本同001共享一次，不重复递增。

### 005 — 真实配置缺标志时显式失败（P2；GEN002/003/009/011；类型2/4）

对应首份报告的GR-01，与新GR-01的漏15条原因不同；保留来源区分。本方实际在scratch wrapper引用原real配置，仅隔离webServer/outputDir，不连后端，得到expected0/skipped3/exit0/last-run passed。公开的npm入口已设flag，不把它污为同样会skip；这里收窄到用户直接以--config调用真实配置的错误输入。

候选修复：真实配置加载时若process.env.REAL_BACKEND_E2E !== "1"则throw中文错误，提示设置变量或用npm run verify:ui:real-backend；不自动打开真实后端动作。这样缺flag显式非0，官方npm/seeded仍传1；--list审计也必须显式传1。沿用既有flag，不新增开关/状态/参数。

### 006 — 默认E2E只绑定loopback（P2；GEN004/010；类型2/4）

playwright.config.ts:17当前npm run dev:web→apps/web dev→vite --host0.0.0.0 --port5173，消费者和就绪URL却只访问127.0.0.1。旧报告声称“默认口令”不足以证明后端越权，明确不采用该推论。新独立Grok在覆盖表亦列同一真实展开；将此现有配置事实作为最小绑定收窄，不添加网络业务规则。

候选修复仅将webServer.command改为“npm --workspace apps/web run dev -- --host 127.0.0.1”，沿用workspace已有5173端口。实际Vite duplicate flag last-wins已经新Grok窄读实现确认；不顺带改变共享apps/web dev脚本、reuseExistingServer、超时或其他服务器参数。

### 007—012逐条无业务发明及证据

007：name/private/version/type/workspaces是npm协议字段；根/web路径与清单实际存在、JSON解析、镜像相符，不定义业务实体/权限。008：128scripts都是现有文件与命令的别名/组合；75node入口语法通过、134根/工作区脚本节点108npm边无悬空无环，不把tenant/pii等工具名字发明为业务流程。009：Playwright用于现有CLI、yaml由OpenAPI两个工具实际import；packageManager用于工具版本选择，本机npm11差异如实记，不自行加engines.npm或升级。010：默认测试报告/超时/Chromium/globalSetup存在且能加载，暖机只是辅助；不新增UI产品行为。011：真实reporter路径与wrapper读取一致，strictPort/proxy/timeout服务既有测试隔离；不以报告输出声称真实业务通过。012：九编译开关被apps/web extends消费，实际TypeScript正负诊断0 vs2322/7006，不发明业务枚举；tsconfig.base.json保持不改。

### 整批修复边界与验证（候选）

预计只改package.json、package-lock.json根镜像、playwright.config.ts、playwright.real-backend.config.ts；tsconfig.base.json、测试spec业务正文、三HTML、其他包记录和业务源码均保持。各hunk按001—006逐行归因，共享版本行按首次涉及本文件ID记账。B2通过前没有改源。

实际修复后验证：严格JSON/版本字段及root-lock engine相等；所有现有包记录除根engine及顶层documentVersion外深相等；workspace-aware脚本引用/语法与删键结果；正常端口1/5174/65535、非法0/65536/NaN/分号路径等在原配置加载前拦截；缺真实flag非0、显式flag1可列18条；默认32与真实18集合不交且并为原50；TypeScript继承/正负验证及两配置逐字段/CLI解析。必要临时反例只在scratch自动清理，不启动生产/DB，不凭--list宣称用例业务绿。完整B3前后树差异双B4、整模块双B5之后才收口发布。

新GrokB1误执行corepack --activate的环境操作已记录：当前/bin/npm11.19.0、corepack已知npm10.9.8，缺事前缓存值证据，不猜测回滚。该操作不是本模块业务源缺陷，不单独造ID/新状态/计数器；后续禁止激活/全局配置命令。




## M004 B5-r1收口（V0.47历史）

整3321行/220记录/18GEN两席PASS，Grok真实exit0，当前源SHA 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc；四值全等原锁，COUNT0。001—006修复队列全否，待答0、新发现0。先写ledger PASS，再写modules/M004-01-root-config-and-guidance-02.md，按已有授权进入发布。两席完整raw及接收事实说明见模块收口；本次不修改源代码或业务HTML。

## M004 B4-r1双PASS（V0.46历史接收）

本方原独立完整报告前12083字节/SHA02e6d6ffd88c6f9b515ef2537d1192e0b40f8588d1cd1af2c98c4b16496a50ef为PASS；接收说明追加后V0.02全13880字节/SHAe8462d5b258fdf83619c829007635c570fbf468c673b4d07dbf44c8372c8af0d，不作为新自审票。
首份Grok B4报告20152字节自述意外接触self正文，原PASS不采信。之后新进程独立重审相同before/after树，完整报告从20152字节后新锁哈希行至EOF，明确未读任何B4/B5 raw旧正文、未创建当前第三tree，审计PASS且越界PASS；真实EXIT_CODE=0。当前Grok raw V0.02全41272字节/SHA8d8ac42fcdab0a45b509026766ca72ff558815d50d581a5bd82517569b1fc80e，旧20152前缀完整保留，新段SHA 512af0048edaeade1c6bf27de3f2bb7f2c51407d010b3f662432a3f4e6b4412e。

本方独立票与新外席独立票共同构成本次B4双PASS；未借受影响旧票放行。2259字节完整binary diff在新Grok报告逐字相等，5hunk归001/001/002/002/003；source保持65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc。四值相等，COUNT0；原001—003本批已修现出队为否，004—006继续否，当前六ID队列全否。B3仍1、运行失败重跑0，无添加项或HTML申请。

接下来B5-r1对当前完整3321行/220记录/18GEN做三线全量复审，不只审差异。当前IN_PROGRESS，不是模块PASS。下方待修/候选/待外审均为当时时态，唯一队列表与本段为当前状态。

## M004 B2双PASS定稿（V0.45当前）

两席完整复核同一V0.44候选157999字节/SHA256 209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2，均PASS。本方b1 raw追加V0.04后76213字节/SHA789448acf5ea1f33bab8ad340c94b873b97b0ee2873a8e6f521bd65686a62950；Grok追加V0.03后85017字节/SHAabbb09df87fd38e272738558f170171a8f30077be8755570c6390597613b7d21，真实EXIT_CODE=0。分别保留70453/66531旧前缀，本次B2未增加独占锁哈希行。本方完整自审先落盘才读对方本次报告。

首批Grok FAIL保持历史原文。外席独立核实用户版本铁律、既有DEC-R8-96、npm10/11源码和真实往返，收回swKeyOrder为拒绝闭集的原依据，明确接受documentVersion V0.01为非协议的文件自身版本标记。没有用本方PASS覆盖外席FAIL，也没有让操作员重复批准。三包隔离验证双方均真实通过；其中负size空串特指nanoid/non-secure，不泛指默认CJS（默认CJS抛Wrong ID size）。

当前六ID分类/队列和修复边界均与V0.44同：001—003是、004—006否；10个既有依赖字段加1行自身版本元数据，单文件。无添加项、HTML变更或待答问题。现在准入整批B3-r1，源码尚未写，B3=0。以下V0.44的待双PASS字样保留为候选历史，当前状态以本段为准。

## M004 B1完整双席归并候选（V0.44当前，B2修订候选待双PASS）

输入本方modules/M004-b1-self-raw.md V0.02，64989字节/SHA256=cde9d4c51416b2122e90051759c05c9be3a770d126102db64c28405e10be3f32；Grok modules/M004-b1-grok-raw.md V0.01，49550字节/SHA256=f1d03742777444c383fc857265779a4bd92f52209251ba2f462fb264e3c14adb，真实EXIT_CODE=0。本方先完整落盘后才读Grok，双方3320行/220记录/18GEN均完整。Grok B1 PASS、无GR；本方B1 FAIL/CX01—03。Grok明确未做漏洞库比对，因此本方安全发现为差集，保留其原PASS，不冒写为Grok同意修复，必须B2独立复核。

B1后四值全等23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；COUNT0。当前B3=0，无源改动，无添加项/HTML申请或未答问题。下面六个正式ID覆盖三个工程问题及完整技术登记，不因同一包参与结构登记而复制安全finding。

### M004唯一当前队列表（已双PASS定稿）

| 正式ID | 全部来源 | 四分类 | 工程缺陷 | 修复队列 | 候选处置 |
|---|---|---|---|---|---|
| M004-001 | 本方CX-01；Grok无对应GR | 技术性 | 是 | 否 | nanoid锁3.3.12→同主版修补3.3.18及对应官方元数据 |
| M004-002 | 本方CX-02；Grok无对应GR | 技术性 | 是 | 否 | postcss锁8.5.15→8.5.23及对应官方元数据/包内nanoid范围 |
| M004-003 | 本方CX-03；Grok无对应GR | 技术性 | 是 | 否 | undici锁7.28.0→7.29.0及对应官方元数据 |
| M004-004 | Grok T01—T04；本方顶层/根/web/link登记 | 技术性 | 否 | 否 | 只登记锁格式、两个清单镜像及工作区链接，不改应用版本或清单 |
| M004-005 | Grok T05—T07、T09—T12；本方全部220记录的图/平台/peer/嵌套登记 | 技术性 | 否 | 否 | 只登记结构闭包与原生字段，无谓删包或扩平台不执行；三个安全版本问题只归001—003 |
| M004-006 | Grok T08；本方217条SRI/216去重官方元数据核验 | 技术性 | 否 | 否 | 只登记信任来源与完整性，不因SRI匹配就推定没有漏洞 |

### M004-001 nanoid开发依赖已知无限循环
package-lock.json:1929—1947（version1930）；父postcss约束2070。GEN-010/012/013，类型2/3/4，中等严重度限定为开发工具链；不将上游high标签直接当本系统线上高危。nanoid/non-secure.nanoid(-1)、customAlphabet('abc',0)()实际1秒超时并终止；postcss真实调用nanoid(6)正常。本方audit返回两个GHSA，nanoid3.3.18在父^3.3.12（及修后postcss^3.3.16）范围内，官方registry版本/SRI已实际核对。无业务直接传入负size的已证路径，本项不声称生产可利用。
无业务发明：固定已有软件的修补版本，无新接口、价格、流程或权限。最小修复仅本包version/resolved/integrity；不升主版或添加业务参数防护。

### M004-002 PostCSS开发构建依赖已知map路径缺陷
package-lock.json:2049—2077（version2050）；vite3062约束^8.5.15。GEN-004/010/013，类型2/3/4，中。实际8.5.15 postcss.parse读取测试临时目录合成绝对outside.map，命中AUDIT_SYNTHETIC_MARKER；未读真实秘密，临时目录已删除。官方postcss/postcss公告GHSA-fxqj-rqcc-2cmp修复8.5.23；当前更旧8.5.15只有扩展名检查。Vite主调用设置from，不把上游“未设置from”的条件直接等同项目漏洞。
最小修复仅本包version/resolved/integrity，并按官方8.5.23元数据将该记录dependencies.nanoid从^3.3.12改^3.3.16；其他依赖字段相同，不升vite、不改业务CSS处理流程。

### M004-003 undici开发测试依赖已知缓存头缺陷
package-lock.json:3043—3052（version3044）；jsdom1592约束^7.25.0。GEN-004/009/010/013，类型2/3/4，中。实际调用当前7.28.0 parseCacheControlHeader：空private为数组空串，混合private与private=hdr触发TypeError output[key].concat is not a function。官方nodejs/undici GHSA-4cwx-7wf7-3272修复7.29.0；audit同次还返回4个该版修补公告。
当前jsdom默认无cache interceptor，未证明本项目启用共享缓存或用户可利用。本项是锁引入已知库缺陷，不伪称线上跨用户泄漏。最小修复本包version/resolved/integrity，父范围与engines保持兼容，不升jsdom。

三项共同证据：npm audit --package-lock-only --ignore-scripts --json实际exit1，high3；同锁--omit=dev实际exit0、告警0。实际可复现安全缺陷与已经存在的兼容修补版本支持最小更新；不是因未知CVE猜测而加安全门禁。若Grok对工程缺陷/处置有异议，须在本批B2明确指出，未双PASS不修。

### M004-004—006 全部无缺陷技术登记
004：顶层1—6/根7—20/web21—43/link1305—1308/闭合3319—3320，原生JSON无重键，根与web清单字段完全相等，link目标存在。类型3/4，GEN002/006/008/013/017/018。只安装元数据，无业务发明。
005：两份raw全220记录覆盖表逐项保留；Grok438普通/optional依赖边、Codex582含peer解析边，口径不同非冲突；实际npm ls/ci dry-run均0，CodexArborist反例摘掉react检出50无效引用。optional平台/fsevents、peer、嵌套副本、engines和npm宿主差异如原始报告如实保留。无新增或删除包/业务能力的候选动作。类型3/4及内存解析2，GEN001/002/003/008/013/017/018。
006：217 registry条目全HTTPS官方域且SRI格式通过；Grok实际下载217 tarball重算sha512全部一致，Codex逐216去重name/version官方dist字段比对一致。两种完整性证据不能当作安全无漏洞证明。类型3/4，GEN004/010/011/018。只软件来源，无业务发明。

### 整批候选修复边界（未实施）
唯一源文件package-lock.json：只更新上述三个现有包记录的version/resolved/integrity与postcss内部nanoid约束（10个已有值），其它217记录、应用name/version、workspace根镜像、父清单以及业务HTML不变。依赖仍满足现有父范围；不用无范围audit fix，不整体升级树、不新增依赖或overrides。

版本处理（针对首批Grok B2 FAIL的修订论证，源仍未写）：本轮操作员直接提供的AGENTS.md instructions明确原文为：“版本铁律：所有版本号统一使用 Vx.xx 格式；任何对话中只要更新文件内容，该文件自身版本号末位必须前进一位（如 V0.02 → V0.03），发布日期或文件名不能代替版本号递增。”该要求未限定文档/HTML/JS；不是总调度新增的审计判据，也不能由本方自行豁免JSON文件。上位用户指令在本次会话已明确，不需要再次申请其已给出的要求。

npm顶层version=0.1.0代表应用语义版本、lockfileVersion=3代表协议格式；二者均保留，不能冒作本文件自身Vx.xx版本。拟在本文件顶层增加documentVersion: V0.01，仅承担操作员要求的文件自身版本记录，归M004-001这次实际修改文件的版本维护。此字段不声称是npm标准协议字段，不用于依赖解析/业务接口，不新增审计门禁或产出物。把版本只写在findings/raw中不能满足“该文件自身版本号”的原文；改应用name/version或增副文件会更大且不满足。当前修复队列和10个既有依赖字段更新不变。

已核事实：宿主npm11.19.0的Arborist loadVirtual/toJSON真实内存往返保留documentVersion，标准化后除该字段外全部对象deepEqual，582条边签名不变，220记录不变；原锁字节未写。另实际下载项目packageManager声明npm10.9.8官方tarball并核sha512，内存读其Arborist shrinkwrap.js:490—495的...data保留未知顶层字段、959—961只剔除旧dependencies。swKeyOrder是排序优先列表而非JSONschema拒绝闭集；未在npm文档列名仅说明扩展不参与安装，不能据此推断npm禁止该元数据。此兼容结论有源代码和真实运行支持，不把“允许保留”单独当必要性；必要性来自上述操作员原文。

首批Grok的B2 FAIL及其六项接受均原样保留在modules/M004-b1-grok-raw.md V0.02（66531字节/SHA 888887e239ab1b3bfc75aa6158ed7755e9fdd48d41d7f7e696dfacde15568a7f）；本方首批PASS不覆盖它。本次重新交整批复核，须其针对用户原文、扩展字段与协议字段区别、实际兼容性给新PASS/FAIL；未双PASS不实施。Grok若仍否决，应给与这条现存用户要求同时相容的最小可执行处置或明确剩余矛盾，不能直接把该要求缩为“仅审计文档版本”。

补充验证（仅候选隔离验证、不是B3或主工作区已修）：从官方registry实际下载三候选tgz逐个sha512匹配，在主工作区.scratch下自动删除的临时上下文加载真实包。nanoid3.3.18负size返回空串、customAlphabet零size返回空串、正常size6长度6；postcss8.5.23未读合成绝对outside.map（MAP null）；undici7.29.0空private与混合private均归private:true，不抛TypeError。三者exit0，临时上下文删除，主锁SHA保持0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d。官方候选元数据其他安装字段比对仅PostCSS内部nanoid约束变化，符合本批10值边界。

修复验证只按实际内容：相同lock的npm原生解析/图/ci dry-run/全audit与生产audit；实际修后包验证上面库级反例，必要时用临时可丢弃上下文，不改主工作区node_modules或运行服务。新增测试/辅助代码只在确需且归已定稿ID时以最小形式处理，不新增强制门禁/计数器。B3前后全路径tree与全部binary diff、B4双审及B5整模块复审照旧。

以上为修订候选，不是双席定稿；首批B2合取FAIL，下一调用重审整批V0.44。B3=0保持。


## M003 B5-r2双PASS，模块审计收口（V0.41当前）

两席完整审当前8文件621行、18GEN与全部26ID，双PASS，无新发现/待修/未答问题。Grok真实EXIT_CODE=0，完整报告33190字节/SHA256=0fe035344280ae6e9450b1fe0f5bc7fd91a2861da156927ac407c4350c351818；本方独立报告49514字节先落盘后才读Grok，收到外审后追加环境事实补核V0.02，现51460字节/SHA256=4fdd804f891c5bc412064b98027cb27224c3e39ec50708485088ff3ac1d0059d。原前缀完整保留，非新的B5或第三票。

B5后实算/sidecar/ledger/Grok报告四值均23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，LOCK-AMEND-COUNT=0。源码与B3-r2后像8文件SHA逐项相同；三HTML未改。018/025/026无残留，023删除/024保留落实；019既有撤回理由不变。Grok宿主JAVA_HOME错误按环境失败保留，未称后端测试PASS；本方实际Spring环境绑定补核exit0，无新缺陷。

B3累计2、运行失败重跑0。本批四值及完整双PASS成立后，先把ledger模块行IN_PROGRESS改PASS，再写modules/M003-01-root-config-and-guidance-01.md。当前审计完成，提交/PR/CI/合并/160同步尚待执行，不能将本段当发布完成证明。发布完成后方可进入M004。


M001既有审计与PR #410发布保持完成。历史B1/B2/B4和申请文字均保留当时状态，当前M002以最上方V0.29整批段及唯一队列表为准；历史“待批/待修/锁待同步”不代表现值。



## M003 B4-r2双PASS（V0.40当前）

本方raw V0.01，17126字节/SHA256=8c7e35e117ddfb390d7dd5ea5a936e60cd6410b1bc7a38336a3bad62aa888a30；Grok raw V0.01，25091字节/SHA256=23019239a89237433bee6bf94b94f6e9554be945fc80c9b40e86777f39180a75，真实EXIT_CODE=0。两席完整审4996字节/9hunk/3文件两树diff，B4及越界归因闸均PASS；Grok原raw中包含完整diff且字节一致。原018/025/026三项是现全改否，唯一队列表26项均否，B3累计2。

Grok三项观察（重复STAFF节点、目录“管理端”、AOP）均明确非缺陷，不分新ID、不增修。重复节点是同ID同标签；“管理端”并不肯定客人网页端；AOP有aspectjweaver不必存在starter-aop。下一阶段B5仍逐项检查完整当前事实，不以B4 PASS替代。无互盲泄漏，无HTML改动，023删除/024保留实际答复保持。现在进B5-r2全量审621行/18GEN，尚非模块PASS。

## M003 B5-r1回退整批B2双PASS定稿（V0.39当前）

两席实际审核同一V0.38候选，145293字节/SHA f205604867e66c6ee7ab5b605de9c6e01de111027f61031f628fc12e061a8e0f，均PASS。本方b1 raw追加V0.03后56657字节/SHA d141759f52582e517019922de2e4a11865b34aa6a28ee322fd273870d4b48d01；Grok b1 raw追加V0.03后79285字节/SHA b595810df2273dc8a57fd639e6a9de130f130966459d46848b656a568c076bea，真实EXIT_CODE=0。原44260/58006字节前缀均完整保留，B2追加没有新增独占锁哈希行。

Grok直接读PGM739与图边后明确撤回其B5中“不升客人Web冲突”的判断，认可026；认可019撤回、018沿用及025新编。当前26ID分类、队列和最小范围与V0.38一致且已双PASS定稿，3项是/23项否，无待批问题。只准备整批B3-r2，未声明模块PASS。旧候选字样是历史时态，以下唯一队列表当前定稿生效。

## M003 B5-r1整批回B2候选（V0.38当前）

完整输入为modules/M003-b5-r1-self-raw.md V0.02，27009字节，SHA256=c3553cb525c00c9afad9831546872598f6ec9cb7974d5b48c1fe7b96c1d8d611；modules/M003-b5-r1-grok-raw.md V0.01，26335字节，SHA256=69b40ae4e4185562ed753ea0f4cf9a128d4c43f24edf9740d40f7e326ee670d0，Grok真实EXIT_CODE=0。两席本次完整报告从最后一个独占哈希行至EOF均已读完，均FAIL；621行/18GEN审完才统一回退。B5后四值全等23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；COUNT0；源码本阶段无变化，B3仍1。未读Grok前本方完整self已落盘（含本方证据类型纠正），保持独立顺序。

本批共CX-01/02/03和GR-01四个临时发现，全部归并如下；另保留两席完整逐项无缺陷登记，不遗漏任何来源。候选当前26ID，3项是、23项否；没有添加项、没有新HTML修改或待答问题。尚未双PASS，禁止B3。旧段保留其发生时态，唯一队列表及本段为当前候选。

| 本次临时号 | 正式ID/分类 | 候选处理 | 差集及分歧 |
|---|---|---|---|
| CX-01 高危审批 | M003-019 / 细化 / 否 | 核实后撤回冲突判断 | PGM652本就允许上一级/上两级/最高层级；660禁止发起人选档且自动补链。CONTEXT119没有发起人选档或跳过中间审批的文字。本方原反例额外假定了选择界面，不能作已存在缺陷；接受Grok该反证。保留原raw与撤回事由，未改源码。 |
| CX-02 客人Web | M003-026 / 冲突 / 是 | 删除客人Web关系与customer Web标签 | Grok认为C4图不代表十一端故未判冲突；本方保留分歧：这不是要求C4列齐十一端，而是图明确画Guest使用名为Web(admin/customer)的入口，与PGM739客人无网页端直接相反。CONTEXT正确不能让README错误角色连线变真。须Grok对这份具体原文反证独立复核，不能单方判定B2 PASS。 |
| CX-03 Data JPA | M003-018 / 技术性 / 是 | 纠正组件事实 | 旧018已有技术栈登记、曾误称pom一致，沿用此ID，不分新技术栈ID。原self类型2/4已澄清只有4有效；当前完整pom声明starter-jdbc而无starter-data-jpa，精确rg无JPA使用。不是要求新增JPA或以版本号一致掩盖组件差异。 |
| GR-01 中文例外 | M003-025 / 冲突 / 是 | 接受并限缩到HTML封闭四样 | Grok原文完整保留。PGM287/965明确只有单据号、设备编号、二维码内容、系统内部标识符四样；AGENTS把枚举值/字段名列作例外可能被执行成展示英文。可直接用上位封闭清单消除例外歧义，不增业务规则。 |

编号说明：原018为其余工程入口/命令的技术性登记，明确技术版本和命令事实，但未给“客人使用Web”和“中文展示例外闭集”这两个独立业务冲突建立正式finding；025/026分别作为新的具体业务冲突创建，不重复复用已经修好的003/011。仅源行曾在整段覆盖范围内不等于该业务冲突已经有专属ID。CX03确属018已有技术栈命题，CX01确属019既有术语命题，沿用018/019；019撤回亦保留完整追溯。此编号与分类均交双席B2核对，若Grok认为需沿用别的既有ID，先改候选再复核，不直接修。

### 本批最小修复边界（候选，未实施）

018：README.md第18行Data JPA→JDBC；第19行starter-data-jpa + jdbc→starter-jdbc；README.en.md第16行Data JPA→JDBC。其余技术栈与POM字节不动。无业务发明：只陈述当前已经声明的框架组件，不定义业务角色、收费、状态或流程。

025：AGENTS.md第55行“7. **所有展示文字全中文**，仅机读标识（枚举值、字段名、日志键）保留原文。”替换为“7. **所有展示文字全中文**，可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样。”第56行适用终端原文保留。业务上位PGM287/965，不修改HTML。

026：README.md第135行GUEST→WEB(admin/customer)改为STAFF→WEB(业务人员)，保留下一行员工STAFF→ANDROID；英文README第90行同改STAFF→Web(staff)，第33行admin/customer web改admin web。图不再声明消费客人有Web端；不新增app节点、不改客户端代码或其它架构文件，保留其余图边/API/DB。先前Grok不认定冲突的理由与本方反证均保留。

三个源文件目前均V0.01；实施整批B3时各仅递增一次V0.02，版本变更归对应该文件已定稿ID。CONTEXT、PROGRESS、.env、两ignore、三HTML及其它非模块文件均不在本批源修复范围。已答023删除/024保留保持，无需再次弹窗。下一步本整批B2双席PASS后，才统一执行B3-r2。


## M003 B4-r1双PASS（V0.37当前）

Codex B4 raw V0.01 SHA256=3c549c80f4142c54b6af82f80eb4fb435b5d72cf2ded94ff5696d612a2955440（29510字节）；Grok B4 raw V0.01 SHA256=a4472cd9a0a97792387d356a78fd9b0dd48e4d8288d2379d4a9a854bec4e4f0d（43627字节），真实EXIT_CODE=0。两席审完整19006字节/22hunk/6文件两树diff均PASS，全文与逐块归因各在自身raw，未遗漏或过滤。原19项是现全部出队为否，其他5项仍否；24项唯一当前队列表均否，B3累计1。

Grok GR-OBS-01/02/03分别对应004单空格、006摘要未抄自身人员、011别名；均明确非缺陷/非FAIL，原报告保留，不扩修或新编ID。本方已实际用当前三Java源码编译验证旧PII空值失败、修复后未设/local32字节、prod默认拒绝；门禁/链接检查证据在self raw。024词条、两份ignore和三份HTML未变。

Grok披露一次全仓检索命中self raw中的一行已共享冻结diff片段，未读本方结论；命中内容与两席先前已收到的同一冻结输入相同，没有新增另一席分析信息。其披露完整保留，未删改原raw。本方完整self落盘前未接触Grok正文或JSON；B5继续使用精确源路径检索及排除审计产物，避免混入另一席报告。

下一步B5-r1三线全量审当前8文件621行/18GEN；仍IN_PROGRESS，不是模块PASS。以下B1/B2/B3历史内容中的待批/待修时态均以本段及唯一队列表为当前状态。

## M003 B2首批双PASS定稿（V0.36当前）

两席对同一findings V0.35 / SHA256=00de12e4e63c56e2f9460c70bd55ee36149bd78d97c81687ece69baa8a8e9a11完整24ID均PASS。Codex b1 raw追加V0.02后44260字节/SHA256=6f3e7c9600aefa49f8347dbaeafcc526b54b230eba9c605ed5e8f66c8b2cc3f3；Grok b1 raw追加V0.02后58006字节/SHA256=e7736c8bd139469157615222efda46f6390d83ab22e907a20816a0e594bd6292，真实EXIT_CODE=0。两份原始B1前缀分别36557/36585字节完整保留，B2均未新增独占锁哈希行。

Grok独立重跑解析/配置命令并接受017缺陷否；独立读BSM1725后撤回GR18缺文事实和添加分类，接受024细化/队列否；接受021表外快照说明、022历史时态澄清。全部19CX/16GR及逐项技术登记差集无遗漏，无新ID、无未答问题。

以下V0.35当前队列表的分类与是/否均现已定稿，语义不改：001—015、020—023共19项是；016—019、024共5项否。旧段中的候选/等待文字保留提议时态，以本段及两席V0.02为当前定稿证据；023/024实际答复早已齐全。下一步一次性修本模块全部19项；双PASS不等于模块PASS，B3仍0，源码尚未修改。

## M003 B1整批归并候选（V0.35；两项答齐，双席未定稿）

M001已发布PR #410；M002已发布PR #411，160 main=df580198172b2c497ef3cb7e25d2e8e5dc058920，均保持PASS。M003两席已审完整8文件625行与18GEN，B1均FAIL，B3=0；本段为当前整批候选，不能据此修复。没有重开已完成模块。

输入：modules/M003-b1-self-raw.md V0.01，36557字节，SHA256=7fc9d6901f4347f36fb8fd232c56fdb2cf5418387f5ac02b18d5aae0e6654995；modules/M003-b1-grok-raw.md V0.01，36585字节，SHA256=1fe8db0cfdc990715545f383fccedeeedac66e4c03ef308dd628ff7256af2d3b，真实EXIT_CODE=0。两份完整报告保留，当前无B2投票。本方先落完整raw后才读Grok。B1四值全等现锁23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。控制V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21已按用户最新通知重新读核，字节未变。

本批24个正式ID：19项候选入队、5项无待修，未答问题0。023已获操作员实际答复“删除（推荐）”，规范答复“删除”，转入候选修复队列；024实际答复“保留”，按正常审计继续，未证工程缺陷，队列否。两项答齐，恢复IN_PROGRESS，现仅可派B2整批双席复核，尚未定稿、不能B3。每项原观点/反证完整保留；GR-18的HTML不存在论据已被BSM1725反证，本方不接受错误事实，也不单方替换Grok原报告。按现行§4 B2①先答后复核顺序附题暂停；不先审另一批或修其中已有项。

### M003唯一当前候选队列表

| ID | 当前来源 | 分类 | 工程缺陷 | 修复队列归属 | 候选处置 |
|---|---|---|---|---|---|
| M003-001 | CX-01；GR-15（死变量） | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-002 | CX-02；GR-15（空密钥前提） | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-003 | CX-03；GR-07 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-004 | CX-04 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-005 | CX-05 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-006 | CX-06；GR-03 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-007 | CX-07 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-008 | CX-08；GR-02 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-009 | CX-09；GR-04 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-010 | CX-10 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-011 | CX-11；GR-01 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-012 | CX-12；GR-06 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-013 | CX-13；GR-09、GR-11 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-014 | CX-14；GR-08 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-015 | CX-15 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-016 | CX-16 | 技术性 | 否 | 否 | 只登记，不修改。 |
| M003-017 | CX-17；GR-14（保留分歧） | 技术性 | 否 | 否 | 只登记；不更改扫描目录和源码。 |
| M003-018 | B5-r1 CX-03；原CX-18工程登记 | 技术性 | 已修复 | 否 | B4-r2双PASS确认本批已修，出队；分类和原始证据保留。 |
| M003-019 | 原CX-19；B5-r1 CX-01核实撤回 | 细化 | 否 | 否 | 高危段未写发起人自选档位；PGM652明文有层级。保留原文，不把假定的选择界面当事实。 |
| M003-020 | GR-05；CX-19内部结算段拆分 | 冲突 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-021 | GR-10；CX-18快照解释分歧 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-022 | GR-13；CX-19历史记录解释分歧 | 技术性 | 已修复 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-023 | GR-17；CX-07同一行独立L4片段 | 添加 | 否 | 否 | B4-r1双PASS确认本批已修，出队；既有分类及业务答复保留。 |
| M003-024 | GR-18；CX-19营销段拆分 | 细化 | 否 | 否 | 操作员实际答复：保留。按正常审计继续；BSM1725已明文支持，分类分歧交Grok B2复核，未证工程缺陷，不改词条或HTML。 |
| M003-025 | B5-r1 GR-01 | 冲突 | 已修复 | 否 | B4-r2双PASS确认本批已修，出队；分类和原始证据保留。 |
| M003-026 | B5-r1 CX-02；Grok本轮不认定冲突，分歧保留待复核 | 冲突 | 已修复 | 否 | B4-r2双PASS确认本批已修，出队；分类和原始证据保留。 |

### M003逐项论据与分类理由

#### M003-001 — 5个认证配置变量没有正式后端消费者

位置：.env.example:14-18。来源：CX-01；GR-15（死变量）。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-002, GEN-013, GEN-016。证据类型：4。

rg 在 backend/src、tools、ops、apps/web/src、apps/android-terminal 追踪五个变量，仅另一个 env 样例与钥匙清单出现；正式 AuthService:41 / ProductionAuthService:49-52 使用 complex.auth.access-token-ttl 等属性。实际 Spring StandardEnvironment 注入 ACCESS_TOKEN_TTL_SECONDS=900、AUTH_FAILURE_LOCK_THRESHOLD=3 后，消费端仍解析 PT30M、5。JWT/CORS/客人TTL同样没有对应接线。

候选最小处置：删除样例中的无效配置声明，不为保留旧变量新增生产接线。

#### M003-002 — 显式空 PII 密钥不会回退到开发默认

位置：.env.example:20-22。来源：CX-02；GR-15（空密钥前提）。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-002, GEN-009, GEN-013, GEN-016。证据类型：2/4。

原文称留空用dev默认，但 Spring 实际解析空值长度为0；从当前三个源码编译 LocalKeyVault 调用 dataKey("pii")，local+显式空值抛 keyvault.keys.pii must be configured；未设置变量时才产生32字节默认密钥，prod默认被拒。

候选最小处置：删除空值赋值，明确未设置变量才采用本地默认；保留生产强密钥要求。

#### M003-003 — 删除规则的一律与唯一例外覆盖了账户注销

位置：AGENTS.md:50-51。来源：CX-03；GR-07。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017, GEN-005。证据类型：1。

AGENTS说所有删除软删除、唯一例外为人脸照；BSM账户注销明确姓名、手机号、证件等个人信息删除，只有限期事件记录例外。按AGENTS保留手机号便违反HTML。；GR-07补充：审批主体被写成role_platform_admin，而BSM:588明确平台管理员与最高权限不同，删除不自动授予平台管理员。

候选最小处置：删除一律/唯一例外的泛化，收窄为HTML配置主数据软删除规则并引用专项生命周期。；删除role_platform_admin即删除审批人的等同限定，按HTML既有最高权限审批规则表述。

HTML原文 docs/business-system-map-for-operations-v0.1.html:567：
> <p>过去"东西一旦录进去就改不了、删不掉"的问题解决了。对配置类主数据——<strong>商户档案、票种、价格规则、酒吧商品/分类/套餐</strong>等——现在可以：</p>

HTML原文 docs/business-system-map-for-operations-v0.1.html:570：
> <div class="explain-item"><strong>删除（走审批·软删除）</strong>：要删除，得先存档，再"申请删除"、填写删除理由，由<strong>具备最高权限的人</strong>审批通过后执行（申请人不能审批自己的申请）。删除走审批、要留痕。</div>

HTML原文 docs/business-system-map-for-operations-v0.1.html:862：
> <li>账户注销：客人可以自行申请注销账户。发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；放弃的存酒按存酒到期规则转为所在酒吧的自有库存资产，放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，商户级卡归发卡商户）。注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除；仅有一项例外：注销时点与该手机号的对应事件记录，仅为执行枚举速查表「会员身份状态」条的『同一手机号一年内注销与注册合计次数』异常预警及注销后重新注册的新人资格判定而留存，留存至该一年统计窗口届满即删除，不得用于其他目的、不与新账户建立任何关联；积分、券、未核销的票一律清零；消费记录、支付流水、退款记录等钱账留存，但脱去个人标识、改以不可反查的编号记录，以保证历史账目仍然对得平。个人信息的查询与导出按来源判定：属该商户的客户的，该商户可以查询、可以导出，但不得删除。</li>

HTML原文 docs/business-system-map-for-operations-v0.1.html:588：
> <p><strong>平台管理员与最高权限是两个不同的角色。</strong>平台管理员是日常运维岗位，人数上限由后台配置（业务参数，全平台统一可配，须同时配置上下限）：拥有系统里全部<strong>日常功能</strong>权限；今后每上线一个新功能，都必须先定好它算日常功能还是高危功能才允许上线，定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况，还没定类的一律先当高危功能对待、不自动同步；但资金操作（退款、调账、分红发放、税费调整）、敏感数据查看与导出（内部结算价、成本价、分账底价、企业微信会话原文、客人与员工个人信息）、删除，以及最终业务审批这几类，不因"平台管理员"这个身份自动获得，必须逐项单独授予并留痕，平台管理员即使同时具备业务权限，也不得审批自己发起的上述事项。<strong>最高权限是另一个角色，全平台只设一人，即持有授权 U 盘密钥的那个人</strong>：凭密钥可以执行系统启动、破窗登录、手动清理留存数据等最高权限动作，其权限由密钥的物理保管来把关，不再另设审批环节。其他岗位（营销员、服务员、收银、外包、审计等）按岗位分配各自能看能做的范围。此外，权限模板选中后可以直接查看和修改已配的权限，勾选权限的区域也按左侧菜单的板块归类，照着目录勾即可。</p>

#### M003-004 — GitHub checkout 浅克隆理由已经失实

位置：AGENTS.md:65-66。来源：CX-04。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-013, GEN-016。证据类型：4。

当前 .github/workflows/predeploy-gates.yml 显式 fetch-depth: 0，AGENTS却称默认浅克隆取不到origin/main。是否将迁移门禁接进predeploy另属代码模块，本项只修失实说明。

候选最小处置：删除失实浅克隆理由，不改变CI门禁。

#### M003-005 — 把三份会员领域 Markdown 置于 HTML 之上

位置：CONTEXT.md:12。来源：CX-05。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

该行称会员等改由三份领域权威文档、其他领域仍由HTML解释；当前治理HTML解释权明确三份HTML共同最高，PRD不得覆盖，未写入HTML不因此生效。

候选最小处置：删除反向改写权威级别的段落；其余源文件不改。

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:974：
> <p class="footer-note">用途：给不懂产品和技术的业务人员理解平台治理层范围。<br><strong>解释权声明：</strong>本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>

#### M003-006 — 固定项目公司隶属链与城市主体职责失实

位置：CONTEXT.md:23-24,30-31,34。来源：CX-06；GR-03。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

CONTEXT把项目公司写为本地定价售票分账总负责人、管理公司置于项目公司下/商户之上、资产职责只称长沙。HTML规定多对多协作，不是固定层级；城市公司不管理各域公司或票务，各城市项目公司均兼资产职责。

候选最小处置：删除固定上下级及越权职责，保留项目隔离和各主体明确边界，按已存在HTML收窄。

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:415：
> <h3>物理项目与运营公司：多对多协作，不是固定隶属层级</h3>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:418：
> <li><strong>物理项目</strong>：是空间、价格、经营数据和权限隔离维度。一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。</li>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:420：
> <li><strong>城市项目公司</strong>：每个城市项目公司同时承担本城市项目的资产职责，只负责资产、场地、物业、租赁、直接管理的第三方商户及自身人员和设备；它不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务或结算。历史合同、财务底稿与既往文件中出现的“资产公司”“资产管理公司”，指的就是本主体，同一主体两个叫法；资产业态不另设独立主体，由城市项目公司兼任。但作为综合体通用储值卡的发行与收款主体，长沙项目公司应当就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清；此项付款只是按各业态既定规则兑付客人已付的钱，不构成对该业态定价、票务或结算规则的管理权。</li>

HTML原文 docs/business-enum-quick-reference-v0.1.html:32：
> <tr><td class="k">定价、售票与收款主体</td><td>客人可通过官方 App/小程序、获授权第三方渠道或获授权一体机购票。单一酒吧门票由对应酒吧商户定价、销售并确认已核销部分的收入，票款由娱乐平台公司统一收款后结付给该酒吧商户（与《业务系统地图》口径一致）；酒吧通票由娱乐平台公司负责；纯游乐票和纯游乐通票由游乐公司负责；演出票由传媒公司负责；跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批，票款由娱乐平台公司统一收款。收款主体不等于收入归属主体：娱乐平台公司只把拆分后归属酒吧域的份额确认为自身收入，其余各域份额自收款之日起即为应付给对应域主管公司的款项，按已审批的域间规则结算，不得计入自身收入、不得挪用。城市项目公司不负责上述票务产品的定价、售票或收款。</td></tr>

#### M003-007 — 泛化娱乐公司的通票成本收入与酒吧服务费归属

位置：CONTEXT.md:32,35。来源：CX-07。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

娱乐公司被概括为所有通票及赠权成本归属；酒吧非散台服务费概括为商户自己收入。HTML娱乐公司仅负责酒吧域，纯游乐/演出与其无关；非散台服务费月结应发部分为代收代付、只有扣留部分计商户收入。

候选最小处置：删除过宽的收入/成本断言，保留HTML限定经营域及代收代付口径。

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:428：
> <tr><th scope="row">娱乐平台公司</th><td>负责酒吧经营域、酒吧通票以及跨域组合通票中的酒吧部分；审批酒吧商户提交的项目内部结算价，决定酒吧域采用内部结算价、固定金额、固定单价或排名档位等规则，并承担其决定或出资权益的结算责任。它参股并管理各独立酒吧商户的相关经营关系，但不取得单一酒吧门票收入；只按生效规则向酒吧商户另收门票综合服务费。纯游乐票、纯游乐通票和演出票与其无关。</td></tr>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:429：
> <tr><th scope="row">独立酒吧商户</th><td>负责自身人员、设备、经营和履约。设夜班值班经理，为夜间现场的最高负责人，每天一名，由系统根据当日排班自动指定；凡需其批准或知会的事项系统自动抄送该人，其须立即处理；白天没有夜班值班经理的时段，现场最高负责人为项目负责人。设弱电工程师，隶属娱乐平台公司，负责店面弱电设备的检查与维护，其检查安排在下班之后进行、与店内岗位错峰。自行定价、销售并确认单一酒吧门票已核销部分的收入（票款由娱乐平台公司统一收款后结付，到期未核销部分不归本商户），订台低消、二次消费、额外酒水归本商户；非散台服务费由本商户代收，按后台选定的分配方式发放给当班服务提供人，月结定格的应发部分作代收代付处理、不计本商户收入，月结定格的扣留部分计入本商户收入；后续实际发放只结清应付款、不改变收入归属。酒吧商户可提交价格资料、履约事实和异议，但不能自行决定酒吧通票域内分配方式；酒吧商户提出的异议由客户关怀部协调处理：经内部工单流程流转至相关部门处理，处理结果反馈商户并必须形成完整闭环；处理时限为业务参数（按项目可配，出厂默认二十个工作日，须配上下限）；处理期间按原有效数值先行结算，结论生效后按差额补退。</td></tr>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:430：
> <tr><th scope="row">游乐公司</th><td>负责游乐经营域、纯游乐票和纯游乐通票的定价、售票、收款、内部结算审批与域内分配；纯游乐业务不向娱乐平台公司支付门票综合服务费。</td></tr>

HTML原文 docs/business-enum-quick-reference-v0.1.html:53：
> <tr><td class="k">收费与免费权益结算</td><td>域内通票中的收费权益、豪华套票内小食或特殊权益等，核销后按对应域主管公司已批准的内部结算价或域内规则结算；采用哪种规则由域主管公司决定，商户不能决定。每一项零元免费权益在票种发布前必须逐项标注出资方并锁定版本：由域主管公司在票面强制要求提供的权益（含“每家酒吧不低于最低赠酒杯数”的赠酒）一律记为域主管公司出资，核销后按锁定内部结算价结算；只有商户主动申请并书面确认让利的权益才记为商户让利、不结算。未标注出资方的免费权益不得发布上架。未核销不结算。哪个域主管公司把权益对客价格或域内分配价格定得低于该权益锁定的内部结算价，哪个主体按内部结算价向实际提供方足额结算并自行承担差额；定得高于内部结算价时，高出内部结算价的部分也归该责任主体。全过程只以内部结算价为唯一基准，不使用进货成本作为另一套基准。</td></tr>

#### M003-008 — 会员唯一性仍按自然人声明

位置：CONTEXT.md:49-50。来源：CX-08；GR-02。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

HTML唯一标准是短信验证手机号，不同手机号不得宣称识别同一自然人。CONTEXT直接写同一自然人全国唯一。

候选最小处置：删除自然人唯一断言，按短信验证手机号现行原文收窄。免费会员/无订阅描述指自动注册不另引付款条件，是注册已有条目的细化，不另作添加。

HTML原文 docs/business-enum-quick-reference-v0.1.html:65：
> <tr><td class="k">会员唯一识别</td><td>以完成短信验证的手机号作为判断新会员的唯一标准。同一已验证手机号在全国平台内只建立一个会员账户，并跨城市、跨项目复用会员身份、成长值和等级；不同手机号不得仅凭姓名、生日等弱信息自动合并，系统也不得声称已经识别为同一自然人。</td></tr>

HTML原文 docs/business-enum-quick-reference-v0.1.html:66：
> <tr><td class="k">会员注册</td><td>会员主动提交注册，完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款后，系统自动注册成功并直接创建有效会员身份。任一条件不满足时注册失败且不创建会员身份，不产生“注册中”或“未通过”状态；微信身份不能代替手机号标准。</td></tr>

#### M003-009 — 零售参考价把可配置窗口写死为30天

位置：CONTEXT.md:73。来源：CX-09；GR-04。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

HTML滚动窗口按项目可配，默认三十天，无成交须前延；词表写过去滚动30天，正常配置为60天即出现不同最低价。

候选最小处置：删除固定30天限定，引用已有配置窗口及回溯口径。

HTML原文 docs/business-enum-quick-reference-v0.1.html:80：
> <tr><td class="k">项目零售参考价</td><td>同一项目、同一商品在后台配置的滚动取价窗口内（业务参数，按项目可配，出厂默认三十天，须同时配置上下限）；窗口内没有有效成交的，自动向前延长至找到最近一笔有效成交为止的最低有效实际成交价，不使用其他项目或项目外渠道数据。只纳入已完成且未取消、未作废的普通有价交易，排除纯积分、积分加现金、零元赠送、测试和异常人工记录；全额退款移除样本，部分退款按保留数量和最终现金支付额及原商品规格分摊重新计算。新商品规格无有效成交时，由实际销售或提供主体提交本项目首发公开销售价、对应域主管公司审批，暂作参考价；产生首笔有效成交后自动切换。价格不得直接手改，只能纠正源交易后由系统重算并保留版本与审计。</td></tr>

#### M003-010 — 全国积分结算中心被固定描述为法律主体

位置：CONTEXT.md:85。来源：CX-10。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-016, GEN-017。证据类型：1。

HTML明确中心可以不是独立公司，此时收款方为顶层平台公司；词表无条件把中心定义为签约收付款法律主体，丢失非独立情形。

候选最小处置：删除必须法律主体的限定，按HTML保留功能主体与实际收款主体区别。

HTML原文 docs/business-enum-quick-reference-v0.1.html:75：
> <tr><td class="k">有效消费与冻结</td><td>符合规则的有效消费在履约完成后产生积分和成长值。消费积分在退款期内冻结，退款期结束后才转为可用；只有转为可用才形成商户积分发行费。积分发行费 ＝ 转为可用的积分数 × 全国统一积分发行单价；收款方为全国积分结算中心，当全国积分结算中心不是独立公司时，收款方为顶层平台公司。积分加现金兑换整单不属于有效消费，不产生积分或成长值，也不刷新积分有效期。会员积分已被兑换后又发生特殊退款、可用积分不足以追回时，差额形成「待追回积分」挂账，由该会员后续获得的积分依次抵扣；待追回积分清偿完毕之前，该会员不得再进行任何积分兑换。待追回积分不形成负积分余额，也不形成对会员的现金债务。待追回积分只有四类出口，除此之外不得以其他方式消除：①追平——后续获得的积分依次抵扣至清零后自动解除兑换限制并留痕；②小额豁免——待追回额度不超过后台配置的豁免线的（业务参数，全平台统一可配，出厂默认 100 积分及以下，须同时配置上下限），由财务豁免并留痕；③不设时效——未清偿的一直挂账，不因时间流逝自动消灭；④会员注销——注销时待追回额度作废登记、不形成现金债权，也不再追偿。券抵扣的部分不属于有效消费，不产生积分与成长值，也不刷新积分有效期；客人实付部分照常。</td></tr>

HTML原文 docs/business-enum-quick-reference-v0.1.html:85：
> <tr><td class="k">会员系统服务费</td><td>单独收取并作为独立账单行；每份商户合同在“按有效会员消费金额比例”或“固定月费”中二选一，初期可配置为 0；收款方同积分发行费——全国积分结算中心，当其不是独立公司时为顶层平台公司。按比例收取的，对应交易发生退款时，该笔服务费按实退金额同比例退回；按固定月费收取的，不因交易退款而退还。</td></tr>

#### M003-011 — 终端仍列十类且兜底关闭不保留留痕例外

位置：CONTEXT.md:139-149。来源：CX-11；GR-01。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-009, GEN-011, GEN-016, GEN-017。证据类型：1。

当前HTML列十一类，含独立经销商Web门户；CONTEXT十类/受控十端遗漏该类。拉取失败写操作一律关的描述还包含特殊留痕写，HTML明确留痕例外继续记。

候选最小处置：按HTML既有十一类终端同步名录，兜底关闭仅指业务写，保留留痕例外。

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:738：
> <li>开关服务、支付通路、核销与入场等营业必需的能力，不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败时按“兜底关闭”处理，写操作一律拒绝；只有留痕是例外——留痕是一种特殊的写操作，不受兜底关闭影响，开关拉不到期间发生过什么、谁被拒绝了，照样要记下来。前端隐藏只是辅助，后端接口必须独立校验。</li>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:739：
> <li>受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。经销商门户适用与其他终端相同的后台开关、权限、数据范围、访问拒绝留痕和兜底关闭规则，并与商户门户及内部员工端隔离账号、权限和数据。</li>

#### M003-012 — 存档不需审批的绝对表述遗漏批量/整店审批

位置：CONTEXT.md:160-161。来源：CX-12；GR-06。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-005, GEN-016, GEN-017。证据类型：1。

HTML只豁免逐条存档；超过配置条数或整店整类须审批。词表不加限制地声称存档不需审批，正常整店操作反例成立。

候选最小处置：收窄到配置主数据逐条存档，引用已有批量审批规则；不新增审批层。

HTML原文 docs/business-system-map-for-operations-v0.1.html:566：
> <h3>配置数据可以存档、也可以删除（删除要审批）</h3>

HTML原文 docs/business-system-map-for-operations-v0.1.html:569：
> <div class="explain-item"><strong>存档（先下线）</strong>：配错了或暂时不用，点"存档"就从各处的选择列表里隐藏，不再被新业务选用；但历史照样能查、能对账。想恢复点"取消存档"即可。逐条存档属日常操作，不用审批；但一次性存档超过后台配置条数的，或整店、整类存档的，须经审批后执行（业务参数，按综合体可配，须同时配置上下限）。后台另设「已存档内容」页，进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。</div>

#### M003-013 — 目录树有不存在路径及失实脚本数

位置：README.md:38-39,44。来源：CX-13；GR-09、GR-11。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-016。证据类型：4。

Path('database/migrations').exists()=False；当前 tools 顶层mjs为78而README写79。当前HTML不要求保留已删除路径说明，属技术入口事实。

候选最小处置：删除不存在目录项及固定脚本数，不影响命令。

#### M003-014 — 英文镜像仍将V124/386等旧值当当前基线

位置：README.en.md:3,29,78-85,97。来源：CX-14；GR-08。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-008, GEN-016。证据类型：4。

声明追踪中文镜像，但中文当前基线V385/363迁移/720ops；EN Current baseline为V124/386ops/42tests6files，真实Playwright --list=47tests9files，目录图仍V001..V124。虽标日期，仍是首页现行基线而非明确历史记录。

候选最小处置：删除旧当前数字并转引中文自动基线及实跑命令；不再复制易漂移计数。英文开发者文档非产品终端，无需全文件翻译。

#### M003-015 — 每个写动作均经GuardedAction/JDBC的绝对架构断言不成立

位置：README.md:142;README.en.md:101。来源：CX-15。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-002, GEN-005, GEN-016。证据类型：4。

AuthController:24-33登录POST直接AuthService内存会话，未标GuardedAction；SecurityConfig:118将其公开。因此字面上每个写动作的链条不成立；此项不把登录公开认作权限漏洞，仅修文档的过宽实现声明。

候选最小处置：把全称断言收窄为受保护业务动作的典型路径，注明认证入口按自身逻辑，不改控制器。

#### M003-016 — 构建缓存与运行数据忽略规则登记

位置：.gitignore:1-55。来源：CX-16。分类候选：技术性；工程缺陷：否；修复队列：否。冻结依据：GEN-010, GEN-013, GEN-017。证据类型：4。

git check-ignore --no-index -v --stdin 实跑：.env/.env.local/node_modules/嵌套dist/运行附件被忽略；.env.example和audit findings未被忽略。未发现当前文档要求生成的凭据文件漏忽略；不为不存在用法扩充.env.*。所有模式均属构建、机器配置或运行产物，无业务状态或资格发明。

候选最小处置：只登记，不修改。

#### M003-017 — 扫描器输入排除登记

位置：.semgrepignore:1-6。来源：CX-17；GR-14（保留分歧）。分类候选：技术性；工程缺陷：否；修复队列：否。冻结依据：GEN-013, GEN-017。证据类型：4。

声明路径均存在；真实semgrep --dump-ast显示YAML exit2、三个密钥脚本bash exit3，bash -n全部0，属解析器能力差异；JSON及另三个脚本能解析。文档/运维补充检查存在，本项不凭解析成功自行新增必须被该扫描器扫描的判据，不把解析报错当代码安全漏洞。无业务发明。；保留GR-14的原FAIL与原exit0声明，不据此声称扫过文件。本方直接AST有3个bash exit3；补跑同一p/bash命令加JSON返回exit7/HTTP404/scanned=[]，不能证明脚本可被成功扫描。候选否需Grok B2接受，未单方定稿。

候选最小处置：只登记；不更改扫描目录和源码。

#### M003-018 — 其余工程入口和命令登记

位置：AGENTS.md:1-48,53-64,68-73;README.md:1-37,53-98,100-104,106-141,144-148;README.en.md:1-77,87-119。来源：CX-18；Grok §6工程登记。分类候选：技术性；工程缺陷：否；修复队列：否。冻结依据：GEN-001, GEN-002, GEN-013, GEN-016, GEN-017。证据类型：1/4。

所有实际npm命令键存在；泛写verify:*不作缺失命令。技术版本与pom/package一致，明确markdown链接5个全部存在；apps/api引用声明已删除因此缺失符合预期；surefire目录无报告被现有快照口径明确豁免。后端环境门禁Java21/Maven3.9.11实际成功，当前基线检查720/V385/363成功，E2E仅list不宣称执行测试。根README业务概览上位为HTML模块入口/集团预留/第三方未定资料。；README:105的现有报告措辞争议拆到M003-021；工程命令存在不代表已运行全部门禁。

候选最小处置：只登记，不重写正确段落。

#### M003-019 — 其余术语与退租历史建设记录登记

位置：CONTEXT.md:14-19,40-48,53-69,74-76,81,86-87,95-136,151-156;PROGRESS.md:1-33（绿数时态另归022）。来源：CX-19；Grok §6业务细化登记（不含营销费用/PROGRESS绿数）。分类候选：细化；工程缺陷：否；修复队列：否。冻结依据：GEN-001, GEN-008, GEN-011, GEN-014, GEN-016, GEN-017。证据类型：1/4。

上位及可复核证据保留CX-19；营销费用89-93拆到024，内部结算价77-80拆到020，PROGRESS完成及绿数的时态歧义拆到022。其余主体绑定、权限、成长/积分、操作留痕均有HTML上位。PROGRESS所列当前源路径存在，当前COMPLETED钩子调用settleTerminationIfNeeded；不把历史绿数当本次运行证明。

候选最小处置：只登记这些未证缺陷的剩余内容，不修改；具体营销/结算价/PROGRESS时态由020/022/024单独处理。

#### M003-020 — 内部结算价范围被限于兑换SKU

位置：CONTEXT.md:77-81。来源：GR-05；CX-19内部结算段拆分。分类候选：冲突；工程缺陷：是；修复队列：是。冻结依据：GEN-008, GEN-016, GEN-017。证据类型：1。

BEQR:54覆盖各业态商品/服务的项目内部结算价格及实际核销/结算，CONTEXT:79只定义兑换SKU商户补偿。普通非积分券结算也需要该价，狭义定义不成立。

候选最小处置：删除仅兑换SKU/仅商户补偿限定，按HTML现行项目商品/服务结算价范围收窄其余表述，不改业务HTML。

HTML原文 docs/business-enum-quick-reference-v0.1.html:54：
> <tr><td class="k">项目内部结算价</td><td>每个项目分别设“内部结算价管理”页面和独立价格目录，不得跨项目引用。实际提供商品或服务的主体提交，对应域主管公司审批，提交人与审批人分离：酒吧由娱乐平台公司审批，游乐由游乐公司审批，演出传媒由传媒公司审批，中央餐饮供应的酒水或小食由中央餐饮公司内部差人审批；餐饮与零售商户（含独立、联营、租赁各种合作模式）统一由城市项目公司审批，其中中央餐饮供货部分仍由中央餐饮公司审批；业态为“其他”的商户，由其商户档案上指定的主管公司审批。任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。价格变更形成新版本并只对后续生效，核销或结算时锁定当时版本。</td></tr>

#### M003-021 — 报告缺失时仍把保留快照标为现有报告

位置：README.md:99-105（生成块说明）。来源：GR-10；CX-18快照解释分歧。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-011, GEN-016。证据类型：4。

本方确认现有脚注明确允许无surefire目录时保留上次快照，故不接受要求凭空复跑整仓或将862改为0。接受Grok指出取证列“现有surefire报告”仍有时态歧义；当前该目录实际不存在。无业务发明，仅测试取证来源表述。直接引用检查tools/refresh-entry-doc-baseline.mjs:75-96/100-105确认整个baseline:auto块重生成、--check比较完整块，不能仅手改105行；本候选改表外说明且不扩修生成器。

候选最小处置：在README自动基线块前澄清：表内“现有surefire报告”表示生成器保留的最近报告快照，无报告时不代表当前工作区存在报告或本轮执行过测试。保留自动块与862现值，不改生成器、不制造新测试结果；避免脚本重生成覆盖手改行。

#### M003-022 — 单次建设记录的完成/绿数需明确历史时态

位置：PROGRESS.md:1,18,32-33。来源：GR-13；CX-19历史记录解释分歧。分类候选：技术性；工程缺陷：是；修复队列：是。冻结依据：GEN-001, GEN-011, GEN-016。证据类型：4。

原标题指定feat/termination-settlement建设记录，本方原先按历史读；Grok认为根目录“完成/绿”容易被误读为main现状。当前MathTests8个@Test、无surefire，不能用未跑证明历史未跑。接受最小历史时态澄清，不把历史7/32/15当当前数、不要求补跑或伪造新绿。无业务发明，仅工程记录时态。

候选最小处置：在标题或状态处明确这是该分支的历史建设记录、测试数为当时记录；保留既有数字及实现说明，不更新为未执行的当前测试结论。

#### M003-023 — 独立酒吧商户的L4层级标记

位置：CONTEXT.md:35。来源：GR-17；CX-07同一行独立L4片段。分类候选：添加；工程缺陷：否；修复队列：是。冻结依据：GEN-017。证据类型：1/4。

Grok指出三份HTML没有L4，只有独立酒吧商户。PGM415还明确物理项目/运营公司非固定隶属。只将L4标记作为添加项问答；原上下级冲突归006，服务费归007，不因本题保留而免除冲突删除。已查正式后端/网页src及根说明仅CONTEXT命中L4。

候选最小处置：操作员已实际答复“删除（推荐）”，规范答复“删除”；仅删CONTEXT.md第35行“L4 ”三个字符，等待024答复及整批B2双PASS后统一B3。

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:415：
> <h3>物理项目与运营公司：多对多协作，不是固定隶属层级</h3>

HTML原文 docs/platform-governance-map-for-operations-v0.1.html:429：
> <tr><th scope="row">独立酒吧商户</th><td>负责自身人员、设备、经营和履约。设夜班值班经理，为夜间现场的最高负责人，每天一名，由系统根据当日排班自动指定；凡需其批准或知会的事项系统自动抄送该人，其须立即处理；白天没有夜班值班经理的时段，现场最高负责人为项目负责人。设弱电工程师，隶属娱乐平台公司，负责店面弱电设备的检查与维护，其检查安排在下班之后进行、与店内岗位错峰。自行定价、销售并确认单一酒吧门票已核销部分的收入（票款由娱乐平台公司统一收款后结付，到期未核销部分不归本商户），订台低消、二次消费、额外酒水归本商户；非散台服务费由本商户代收，按后台选定的分配方式发放给当班服务提供人，月结定格的应发部分作代收代付处理、不计本商户收入，月结定格的扣留部分计入本商户收入；后续实际发放只结清应付款、不改变收入归属。酒吧商户可提交价格资料、履约事实和异议，但不能自行决定酒吧通票域内分配方式；酒吧商户提出的异议由客户关怀部协调处理：经内部工单流程流转至相关部门处理，处理结果反馈商户并必须形成完整闭环；处理时限为业务参数（按项目可配，出厂默认二十个工作日，须配上下限）；处理期间按原有效数值先行结算，结论生效后按差额补退。</td></tr>

#### M003-024 — 营销费用承担主体及多主体分别锁定预算（分类分歧）

位置：CONTEXT.md:89-93。来源：GR-18；CX-19营销段拆分。分类候选：细化；工程缺陷：否；修复队列：否。冻结依据：GEN-017。证据类型：1/4。

候选提议按BSM1725改为细化；保留Grok原判添加及其完整证据，绝不复制其“HTML无登记赞助主体/分别锁预算”的事实错误。BSM1725逐字支持具体法律主体、登记赞助、多个主体分摊及分别锁预算；本方意见为细化/无缺陷。因现行§4 B2①要求添加项先答后复核，本批将两席分歧及HTML原文完整交操作员，建议保留现有词条，答复后再由Grok复核分类，未单方改写Grok报告。

候选最小处置：操作员实际答复“保留”，按正常审计继续，未证工程缺陷，候选队列否；HTML1725原文交Grok复核分类，不改词条或HTML。

HTML原文 docs/business-system-map-for-operations-v0.1.html:1725：
> <tr><th scope="row">营销费用</th><td>活动发起方与费用承担方可以不同；费用承担方必须锁定到具体法律主体，可以是商户、娱乐平台公司、游乐公司、传媒公司、城市项目公司、中央餐饮公司、全国平台或其他登记赞助主体。同一场活动支持多个主体共同承担，按固定金额或按比例分摊，采用比例分摊时各方比例合计必须为 100%；各承担方的预算分别锁定、分别扣减，任一方预算用尽不得自动占用其他方的预算。</td></tr><!-- param-tier:ignore 100% -->

### 全量差集与只登记项目

CX-01—CX-19分别归001—019；CX-19内营销89—93拆024、内部结算77—80拆020、历史绿数时态拆022，剩余内容仍019。GR-01→011；02→008；03→006；04→009；05→020；06→012；07→003；08→014；09/11→013；10→021；13→022；14→017；15→001/002；17→023；18→024。Grok原报告没有GR-12和GR-16，不补造编号。

Grok §6全部逐条登记保留在下表并映射；表中含“细化”的行按冲突→细化→技术性→添加次序处理，不因原章节叫技术性而误改分类。技术项明确无业务发明；主体/权限/成长/积分等有HTML上位者细化归019，例外具体冲突以003/005—012/020为准。构建忽略归016、扫描忽略归017、环境样例其余变量及根工程命令归018，空PII归002，AGENTS/README命令的缺陷片段独立001/004/013—015/021。PROGRESS源码路径登记019，历史时态022。会员“免费”没有在HTML搜到该逐字短语，登记仅以BEQR65/66自动注册、不另加付款条件为上位，不复用原表错误引号。CONTEXT兜底关闭的留痕例外具体归011。下表保留Grok原始表述作差集证据，不宣称全部事实已获本方接受。

| 项 | 位置 | 无业务发明理由 | 工程缺陷 |
|---|---|---|---|
| 忽略 `node_modules`/`dist`/`backend/target`/`coverage`/`.vite`/Gradle/Kotlin/`*.class`/`*.apk` | `.gitignore:1-21` | 构建产物 | 否 |
| 忽略 `.env` / `.env.local` | `.gitignore:4-5` | 密钥文件不入库 | 否 |
| 忽略 playwright 报告与 `test-results` | `.gitignore:23-26` | 测试产物 | 否 |
| 忽略本机 Claude settings、tsbuildinfo、semgrep 滚动日志、`.loop-engineering/`、根调试脚本 glob | `.gitignore:28-51` | 机器/临时产物 | 否 |
| 忽略运行态 `backend/data/bar-images/`、`evidence-files/` | `.gitignore:53-55` | 运行时生成文件 | 否 |
| 忽略 OpenAPI yaml/json、`docs/_archive/` | `.semgrepignore:2-4` | 非扫描源码；各有专门校验 | 否 |
| `SPRING_PROFILES_ACTIVE`/`COMPLEX_API_*`/`COMPLEX_DATABASE_*`/`COMPLEX_API_VERSION`/`BUILD_TIME` | `.env.example:1-12` | 进程/数据源/构建元数据 | 否 |
| `P0_ADMIN_*` 本地种子（注释禁止生产） | `.env.example:24-28` | local 自动种子；角色名来自 HTML 但本文件不发明规则 | 否 |
| `COMPLEX_KEYVAULT_PII_KEY` 变量名（接线存在） | `.env.example:20-22` | 加密密钥占位，非业务实体 | 否（空串语义并入 GR-15） |
| Issue tracker / triage / domain 指针 | `AGENTS.md:3-15` | 代理操作手册 | 否（目标文件存在） |
| 禁主工作区提交 / worktree / 禁直推 / fetch | `AGENTS.md:21-36` | 多会话工程约束 | 否（钩子与脚本存在） |
| 功能改动同步 PRD | `AGENTS.md:38` | 工程流程 | 否 |
| 三份 HTML 终权、skill 不得自改 | `AGENTS.md:42-48` | 细化 HTML 需求源，非发明 | 否 |
| 展示文字全中文（机读标识除外） | `AGENTS.md:53-54` | 细化运营展示口径 | 否 |
| 不为存量兼容/回填 | `AGENTS.md:56-57` | 工程约束 | 否 |
| 迁移 V 号串行、测试只 export 三 DB 变量、测试库 `complex_test`、`MAVEN_ARGS=-DskipTests` | `AGENTS.md:59-73` | 工程闸门；`complex_test` 与 `.env.example` 的 `complex` 分属测试 vs 本地，不冲突 | 否 |
| CONTEXT 声明 HTML 终权 | `CONTEXT.md:1-10` | 指向需求源 | 否 |
| CONTEXT「平台」顶层治理、非综合体 | `CONTEXT.md:14-19` | 细化 PGM「顶层平台」 | 否 |
| 业态专名：娱乐平台/中央餐饮/游乐/传媒；资产=城市项目公司兼任；酒吧是独立商户不是业态主体 | `CONTEXT.md:28-38`（不含 L4） | 细化 PGM 各主体表 | 否 |
| 商户边界 | `CONTEXT.md:40-45` | 细化 PGM 商户侧 | 否 |
| 会员免费、非付费订阅 | `CONTEXT.md:47-50` 中与 HTML「会员注册免费」一致的部分 | 细化 BSM 会员注册 | 否（「同一自然人」另见 GR-02） |
| 成长值只用于等级、不兑 | `CONTEXT.md:53-57` | 细化「成长值与可用积分是两套独立账」 | 否 |
| 积分全国一余额、不收银抵现 | `CONTEXT.md:59-63` | 细化积分跨项目复用/兑换虚拟券 | 否 |
| 待追回积分非负余额、清偿前禁兑 | `CONTEXT.md:65-69` | 细化 BEQR「有效消费与冻结」 | 否 |
| 四类主体绑定 | `CONTEXT.md:95-100` | 细化 BEQR「四类主体」 | 否 |
| 功能权限点 vs 职级 | `CONTEXT.md:102-107` | 细化 PGM 权限规则 | 否 |
| 临时授权有效期或单次、留痕 | `CONTEXT.md:109-114` | 细化 PGM「临时授权可设有效期或单次使用」 | 否 |
| 高危五类授出前审批、禁自审 | `CONTEXT.md:116-121` | 细化 PGM 高危授权审批 | 否 |
| 外包/审计人员边界 | `CONTEXT.md:123-135` | 细化 PGM 外包与审计 | 否 |
| 终端开关关闭后端拒绝、高层压倒、拉取失败写操作兜底关 | `CONTEXT.md:137-142` | 细化 PGM 终端开关（终端**枚举**另见 GR-01） | 否 |
| 操作留痕 | `CONTEXT.md:151-156` | 细化 PGM 留痕 | 否 |
| 删除只隐藏不抹除、申请人不得自审 | `CONTEXT.md:158-163` 中与 HTML 软删除一致的部分 | 细化 BSM 存档删除（「存档不需审批」另见 GR-06） | 否 |
| README 技术栈表（中文） | `README.md:11-25` | 构建依赖 | 否 |
| README §4 脚本名 | `README.md:53-94` | 入口命令存在于 package.json | 否（未跑通） |
| README 中文基线 V385/720 | `README.md:100-104` | 与目录/OpenAPI JSON 一致 | 否 |
| README E2E「以 playwright --list 为准、不写死」 | `README.md:106` | 避免假快照 | 否 |
| README 单综合体起步、域列举、开关化 mock | `README.md:7-9,144-148` | 细化 PGM「当前一个项目」/外部依赖，非发明 | 否 |
| README 读者路径所链 docs | `README.md:111-127` | 本席 `test -e` 均为 EXISTS | 否 |
| `GuardedAction`/`AdmissionGuard` 类存在 | README 架构段 | 技术分层描述；未跑请求 | 否 |
| PROGRESS 所列源码路径存在 | `PROGRESS.md:16-26` | 工作日志指向已存在文件 | 否（绿数另见 GR-13） |


扫描反证：Codex完整B1中直接semgrep --dump-ast -l bash三文件真实exit3、bash -n六文件exit0。后续补跑semgrep --config p/bash --quiet --error --json ops/keyvault/真实exit7，JSON paths.scanned=[]，errors显示p/bash配置HTTP404。该次既未扫到脚本，也不能用Grok原exit0推出脚本可解析；候选017队列否须由Grok B2独立复核，现仍保留原FAIL分歧。

本段全体仍候选，不改源文件、不增加判据、不修改三份HTML；两个questions的答复首个非空行必须保留/删除。全部实际答复后恢复同一M003 B2，不重开B1。


## M002最终B5-r2双PASS（V0.31当前）

两席完整B5对979行/84判据均PASS，Grok实际退出0；本次无新临时号、无残留、无添加。Codex raw SHA256=8b3d99fbd86976efe319a641f853ce8b316b2e9321c6765498a563d89914233c；Grok raw SHA256=bd36f834f587c15ba2228e0fd54b9b1980a5ce2574cd2cc54607fe56ddf37f00。最终四值全等现锁23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，ledger V0.72已先标M002 PASS，再写模块收口。001—004唯一队列表全否保持；B3=2。以下B1/B2/B4与申请段为保留的历史证据，不代表当前未完成。模块发布按用户已授权的独立PR/CI/合并/推送执行，完成以实际回执为准。

## M002新锁B1整批B2定稿（V0.30，双PASS）

本批两席复核同一候选V0.29 SHA256=a39a97646d124fc42408e0e2e7f66b8b3526b3582e7cd00d664984853dd5ff0b均PASS。Codex b1 raw本批追加V0.06后57026字节/SHA256=34e30e764b766ca28d1b559f94dcf9d30b36b757f62c92b2fc3c60e04b676005；Grok b1 raw本批追加V0.05后77784字节/SHA256=67b5d94b00df907d7e7d28104afc6e74ea843f65ed0d7cf0e9a1c4350f742b7f，真实EXIT_CODE=0，原66357字节保留，无新增独占锁哈希行。本V0.30仅记录双PASS，分类/队列不变。Grok独立确认全部差集与技术性理由，001—004均否，无新ID/未答问题。跳过空队列的B3/B4，进入B5-r2全量复审；以下V0.29候选文字保留提议时态，以本段为定稿，尚未模块PASS。

LOCK-AMEND五步已完成；现锁V0.06，实算、sidecar、ledger和本次Grok B1四值均23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。M002按⑤改PENDING后从B1全量重开，现IN_PROGRESS、B3累计2。两席重新审完整979行/84判据，双方B1 PASS；B1通过不等于B2或模块PASS。

本批输入为两份b1 raw最后独占锁哈希行起至EOF的完整报告：Codex modules/M002-b1-self-raw.md V0.04及同次计数更正V0.05（55361字节，SHA256=ace8fdde4c15613faf621523d86eb4d1c65beee28576e41d484578d27bbd3d19）；Grok modules/M002-b1-grok-raw.md V0.04（66357字节，SHA256=678c4cf6780f2bbeabe133a9913c6126074e7e91ef21fd4d40e6124c8c7b25c1），真实EXIT_CODE=0。此前全部原始前缀保留。

完整差集：两席本次新增临时工程缺陷均为空，无漏并、无新ID；页面壳技术性无业务发明继续归M002-003，仅登记，不制造新的重复ID。M002-001申请20已实施且获B4-r2核实，PGM-066锁引文也已正式同步；M002-004门禁修复已获B4-r2双PASS，真实CLI回归和现行文档门禁实跑通过；001/004队列否。002终端别名为细化、无已证缺陷，003页面壳技术性无缺陷，队列也否。唯一队列表四项分类/队列原值不改。Grok本次B1没有再次提出旧GR-02冲突，不用本方意见覆盖旧席位意见。

本方报告PGM-066“21条确认”计数笔误已按原文命令实算17、在同次self末尾追加V0.05更正，原文范围951—979与全文/锁核对不变，未改Grok报告或增加审计轮次。两席均明确文档结构审计不代替后端业务执行、视觉或法律合规实测；没有凭未执行的其它模块制造待补项。无添加项、无未答HTML问题。下一步仅对本整批候选做B2双席复核并各自追加b1 raw；双PASS后队列若仍为空，跳B3/B4进入B5-r2，不重置或增加B3次数。

## M002 B4-r2双PASS收齐（V0.28历史）

Codex modules/M002-b4-r2-self-raw.md V0.01，SHA256=56486b327ccedf5a178c7fb137562b858155934bb7c64fd2a976daa8aff15d3e；Grok modules/M002-b4-r2-grok-raw.md V0.01，SHA256=efa075777661f0ba18f91e84bec968248464f1090e03554edec314f02786598e，真实EXIT_CODE=0。两席全部diff复审均PASS、无新编号、无越界，完整diff SHA256=bc6ba3135dad818ff0693ca4395df9a531b77abe2d5d25e173bcc1cac8cec6b7；001已修4文件仍正确、004两文件修复有效。按规则将001/004修复队列改否，002/003维持否。B3次数保持2。历史队列段不代表现值；当前唯一表为准。PGM-066引文下一步完整LOCK-AMEND，模块尚未B5/PASS。

## M002 B4-r1整批回退后的B2定稿（V0.27，双PASS）

当前定稿证据：两席复核同一V0.26候选SHA256=567f206b5fe5ce9bacc467c2b3b135eaf52cec785401fe12c523276b3aa3a19c，均PASS。Codex b1 raw追加V0.03后SHA256=61875cf84750eb861244be1d2c8a565897fb874d9c67931976177a1267274958；Grok b1 raw追加V0.03后SHA256=bb15ee8accbbe201a407472be1c9b7367f2060e80e0c1a33d991f4388a07c48b，真实EXIT_CODE=0，前30345字节原报告保留。本V0.27只记双PASS，不改变候选两文件拟修范围。Grok保留B4原PASS，同时接受004为阻断当前模块发布的技术缺陷；不能待M315才修。001/004是，002/003否；以下“候选/未定稿”段保留提议时态，现以本段为准。下一步整批B3-r2；尚未B4双PASS或模块PASS。


当前M002 IN_PROGRESS，B3=1。两席完整B4报告已收齐后才归并：Codex modules/M002-b4-r1-self-raw.md V0.01，SHA256=298ea7f7c6e498b8099dec613a7ef51a663c31582535fa79ce4b182b4f9d72ee，FAIL；Grok modules/M002-b4-r1-grok-raw.md V0.01，SHA256=c1822416e1fec755de46f460a977596b9f61486f91ceea7ef531e26d93d562cb，PASS，真实exit=0。两份均收录未过滤binary diff全文，SHA256=409889c17bfbc24392efbc7ee3cfc38e9bedb963bcb566d89c45c243f44247ac。任一席FAIL则整批FAIL，不将Grok PASS或本方正确性验证替代双PASS。

完整差集：本方CX-01→新永久ID M002-004；Grok没有新增GR，原报告认为引用门禁失败不是四文件diff的缺陷，这一意见保留，没有将其改写成同意004。双方同意001两处HTML与镜像正确、逐行归因合法；未判越界，因而不回滚已获批源修改。B4未双PASS，001当前队列仍是。002/003仍否，004候选是；整批先B2，不先修任何部分。

### M002-004：引用检查把不可改写的历史审计报告当成现行引用

位置：tools/verify-entry-anchors.mjs:38–39、226–249；入口package.json:32及verify:predeploy链:96。依据类型4（现有命令/源码可复现），冻结GEN-013（构建配置与依赖）、GEN-016（文档可验证性）；没有新增判据。实际npm run verify:entry-anchors退出1，错误来源M002-b1-grok-raw.md:213及后来B4 self raw相同历史叙述；注册表、账本、覆盖矩阵及镜像列未失配。原始报告保留审计时的反例和问题，不是现在的业务条目承诺。

依序分类：无相反业务规则，亦无HTML上位业务条目需要细化；这是既有发布校验工具的输入范围错误，归技术性且有工程缺陷，不是添加。无业务发明：仅修正历史证据文件的处理，不创设实体、枚举、状态、权限、数值或业务流程。源码本已有“历史留痕文件一律不改、不检”的EXCLUDE；当前raw命名漏入，该普通首跑已经实际失败，并非极端假说。Grok原PASS只覆盖日期四文件diff；用户已明确要求每模块审计完成后提交/CI/合并/推送，该命令是当前M002交付的强制门禁，不能等后续工具模块再修、谎报全绿或改写审计证据。因此候选在本模块登记必要辅助修复，不改scope.txt、模块切分或任何general定义，也不宣称审完工具所在未来模块。

拟最小修复：只扩展现有EXCLUDE，匹配本次规定的docs/audit-2026-09/modules/M三位编号-b1|b4-rN|b5-rN-self|grok-raw.md精确历史路径；不排除整个audit目录，不排除findings、ledger、模块收口或普通文档，不修改Grok raw，不跳过该门禁、不改已有镜像检查。tools/verify-entry-anchors.mjs增加自身V0.01版本注释；另留一项tools/verify-entry-anchors.test.mjs V0.01可运行回归检查，以真实CLI在临时副本中验证：相同失效引用出现在历史raw→通过，出现在现行findings/ledger/模块收口/普通文档→失败。使用Node标准库，无新依赖/框架/业务配置，不检查私有正则源码。测试失败先落盘并确认，再改现有一条排除规则。

以上仅候选。只有本批B2双PASS才能B3-r2：核实001的已修字节仍正确（不再改HTML或重复递增其版本），执行004两文件最小修复；B4-r2再审全部diff。申请20以实际“可以”解除；没有未答添加项或新HTML申请。PGM-066仍待后续V0.17完整LOCK-AMEND，旧锁与COUNT保持不变。下列较早B2和“尚未批准”等叙述只保留历史，当前状态以本节及唯一M002队列表为准。

## 申请20当前批准记录

操作员实际答复：“可以”。对应上一条完整文字确认及申请20精确拟稿，批准治理HTML第2行删除日期并将V0.07递增到V0.08、第974行仅删除“更新日期：2026-09-01。”；其余文字保留。以下“待批/未获批”均为当时的申请记录，现以本批准为准。M002恢复IN_PROGRESS，B3仍0；本批三个ID及队列值不变，等待双席B2复核后才整批修复。

## M002 B2整批定稿（双PASS，修复队列仅M002-001）

M001已通过PR #410完成提交/推送/CI/合并/160同步，M002继续原B1，不重开。输入为以下本次完整报告，均从独占锁哈希首行起到文件尾：

- Codex：modules/M002-b1-self-raw.md V0.01，SHA256=36229226b9887a48dda4b58f9645c642c11ab33a97c6ce33d3c1f6d620154e84；979行/84判据，B1 PASS，CX-01技术性无缺陷登记。
- Grok：modules/M002-b1-grok-raw.md V0.01，SHA256=68749ec4566dee52e794a62f1cca10c834edaefabd79300cb54ae45b270cb661；979行/84判据，B1 FAIL，GR-01日期、GR-02终端别名及页面壳无缺陷登记。
- 差集：GR-01→M002-001；GR-02→M002-002；Codex CX-01与Grok技术性登记归并M002-003。没有漏项；Grok原始意见完整保留，以下是供双席复核的候选处置，不是Codex单方推翻。

| ID | 当前来源 | 分类 | 工程缺陷 | 修复队列归属 | 候选处置 |
|---|---|---|---|---|---|
| M002-001 | Grok B1 GR-01 | 冲突 | 已修复 | 否 | 申请20已执行；B4-r2两席核实已修4文件仍正确，出队 |
| M002-002 | Grok B1 GR-02 | 细化 | 否，Grok本次B2已接受 | 否 | 保留Grok冲突主张；源文是同一十一类终端的自然语言简称，未证明新增标识/端类/权限；不以猜测未来配置分叉作当前缺陷 |
| M002-003 | Codex B1 CX-01及Grok技术性登记 | 技术性 | 否 | 否 | HTML/CSS/导航页面壳无业务发明，无已证工程缺陷，仅登记 |
| M002-004 | Codex B4-r1 CX-01；Grok本批B2接受技术缺陷入队 | 技术性 | 已修复 | 否 | B4-r2双PASS；历史raw窄排除及真实CLI回归通过，出队 |

### 当前B2双席定稿证据

两席复核同一findings V0.24（SHA256=2d69068b380701932807ad378563b3d6b969d91a69fbe4f4dfa3b40b39b58e7c），均PASS。Codex在b1 self raw追加V0.02，全文件SHA256=708179ec40e1766e72d3111e0a33d2f8e982f022ae00fd8ecda036ec784d5674；Grok在b1 grok raw追加V0.02，全文件SHA256=38f8560586963a9135c07153bf63a7864632313db3c800567d2c3aa8d118252b，真实进程退出0，两份原B1前缀保留。Grok本人撤回当前GR-02冲突判断，接受细化/无已证缺陷/队列否，不是Codex单方覆盖。

本V0.25仅记定稿，不改变三项分类/队列及申请20的两处批准。B3仅执行M002-001：治理HTML第2/974行；相同旧日期在entry-registry.csv与authority-rule-ledger.csv各1行的既有文字镜像随源同步，必要发布门禁的business-decision-ledger.md记录实际批准。三处辅助同步均归同一001，不新增业务规则或finding，不改其它源HTML、终端别名、台账ID/列结构/覆盖矩阵；完整diff交B4逐行双席复核。

### M002-001：日期声明冲突

原文：docs/platform-governance-map-for-operations-v0.1.html:2为“文件版本：V0.07；更新日期：2026-09-02”；:974可见页脚为“更新日期：2026-09-01。用途：……”。两处均称同一文件的更新日期，且未标成不同业务日期；按GEN-016文档版本/状态可复现口径登记。没有用注释发明业务规则，只核对元数据的同名声明。

候选最小修复（尚未授权、尚未执行）：第2行将“<!-- 文件版本：V0.07；更新日期：2026-09-02 -->”替换为“<!-- 文件版本：V0.08 -->”；第974行仅删除“更新日期：2026-09-01。”，保留“用途”及解释权全文。只留文件版本，避免猜测另一个日期；版本递增归因此ID。不改变业务规则或权限。内存验证发现第974行位于PGM-066冻结原文中；若获批准并执行，须按V0.15完整LOCK-AMEND同步该引文，不能直接改锁或自算对齐；最早引用模块为M002，M001不受此ID影响。只有获得操作员针对两处的明确批准，并完成本批B2双PASS后，才能B3执行；当前申请20已向操作员提出，未获批。若Grok对分类或方案有异议，先整批归并，不先修。

### M002-002：终端名称差异的原始主张与候选复核

Grok原始结论：冲突，有工程缺陷；GEN-008、PGM-010/046/047；L373、L718–724与L739/枚举名称不一致，实施/配置会分叉。完整原表见Grok B1 GR-02，未改对方raw。

实读原文：L373列出十一端摘要；L718“客人 App 和小程序”、L720“工作人员平板 App”、L721“员工工作手机和员工私人手机 App”、L722“商户侧 App/Web”；L739明确十一类闭集及“Web 前端（业务人员，客人无网页端）”。同一节L741仍使用“同一个员工 App”描述公司设备/自有手机两种权限，没有把App作为端类标识。PGM-047/L739及枚举“终端十一端”共同明确解释；摘要/列表没有赋予客人网页端或创建额外枚举值。

候选分类按顺序：不是相反业务命题；有明确上位HTML PGM-047十一类闭集，故归细化而非技术性/添加。GEN-008原文要求“前端、后端、数据库、OpenAPI、迁移、DTO 和测试夹具对名称、类型、可空性、枚举及错误语义的约定须一致”；本模块是自然语言说明页，未出现两套机器标识或接口/模式，不能把此条扩成所有摘要与正文逐字同名。PGM-010/046/047冻结原文本身同时含这些自然语言写法，没有另冻“自然语言别名必须删尽”判据。

已验证的事实是存在别名，尚未验证的是别名导致两个端类/配置或相反授权规则。Grok也明确承认“不是多出来的第十二端”。因此候选只登记、队列否。此候选须Grok独立复核同意；若仍反对，保持B2未定稿并按其明确证据处理，Codex不自行覆盖Grok票。

### M002-003：页面壳技术性无缺陷登记

范围：L1–216页面头/CSS、L217–253导航、各节结构、L974–979页脚与闭合标签（日期缺陷独立归001，不由本条抵销）。
无业务发明理由：页面壳仅呈现本份既有说明，不引入实体、枚举、状态、权限、价格、默认值或业务流程。导航31链接仅跳既有18章节及卡片目标；无script/form、无网络或写入入口。GEN-013/015/018结构证据：UTF-8/zh-CN、标签平衡、4表thead、18h2+48h3、31目标有效、18导航顺序匹配；本方故意破坏标签/链接两种内存负例均被捕获，未写文件。Grok另有对比度抽查和outline未关闭证据，不冒充浏览器/读屏器测试。

当前候选无添加项；申请20已提出、等待操作员实际答复。控制迁移到V0.15，旧SHA启动防护在实际Grok调用前退出，未取得任何B2票。整个M002为BLOCKED-ASK、B3=0，任何源码未改；待批准后再做整批B2双席复核。


### 申请20：待操作员逐处批准

申请20（M002-001，技术性文字修改授权）：是否批准修改160:/home/james/complex-management/docs/platform-governance-map-for-operations-v0.1.html的以下两处？
① 第2行，将“<!-- 文件版本：V0.07；更新日期：2026-09-02 -->”替换为“<!-- 文件版本：V0.08 -->”。
② 第974行，仅删除“更新日期：2026-09-01。”，其后的“用途”及完整解释权声明保留。
理由：同一文件出现两个不同的“更新日期”；删除两处日期，只保留递增版本，避免另猜一个日期，不改业务规则。
M002两席979行/84判据的B1全量审计均已完成。本次只申请这两处修改；批准后还须完成整批B2双席复核，才能统一修复。未收到实际答复前全流程保持暂停。

## B2整批定稿记录（双PASS，修复队列为空）

V0.13控制提示词；首轮全量审计；M001/322；B3已执行2次；LOCK-AMEND-COUNT=0；锁V0.05。LOCK-AMEND⑤后本次B1两席全量完成，源文件2340行/335映射判据。尚未模块PASS。

本批输入完整报告：
- Codex：modules/M001-b1-self-raw.md，最后独占锁哈希行起V0.25完整B1；全文件SHA256=c78414da9df601f94816fccc5a896fe544a41f2f0d4108a224a25e7bcc7ce1b2。B1 PASS，0新工程缺陷；CX-01是CSS/结构/导航无业务发明登记，复用M001-004。
- Grok：modules/M001-b1-grok-raw.md，最后独占锁哈希行起V0.08完整B1；全文件SHA256=ac206e8acaad5dc3a857e04df74959ddd0960d1f3750e016a38f78494d66b533。B1 PASS，无新GR编号、无工程缺陷；独立复查现行11项，未以旧结论替代本次审计。
- 两份原字节前缀全部保留；本次B1后实算/sidecar/ledger/Grok四值均为73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439。

控制迁移补记：V0.13逐ID HTML出处与铁律4批准依据已列ledger V0.50，COUNT保持0，B2双PASS的业务队列未变；继续B5-r2。

控制迁移：两份输入B1按V0.11交付，已具备V0.12要求的两席首行哈希；新版本不改变本批业务判据与字节。历史五次锁同步均由铁律4批准HTML触发，按V0.12排除后COUNT=0，完整迁移依据见ledger V0.48。此次旧控制哈希防护在Grok启动前退出，没有Grok失败重跑。

整批差集：Codex登记{CX-01→M001-004}，Grok新增发现集合为空；合并未漏项、无新正式ID。001/002/003/005/006/008已有B4双PASS修复证据，保持出队；004技术性仅登记；007/009/010/011均保留此前双席定稿无缺陷处置。当前修复队列为空，无添加项、无未答HTML申请。14/18/19最新两类投诉稿已按原批准执行，不重开旧稿；15/16/17已执行。B2若双PASS，按控制提示词跳过B3/B4，B3计数保持2，进入B5-r2全量审计。本批已双席B2 PASS定稿：Codex self raw追加V0.26（全文件SHA256=221699cfc7a12ecb07373f8a148911db808bfa20ea6497247d77a41bad6c4a66），Grok raw追加V0.09（全文件SHA256=970463b499ed59985a8bc72f1f0d04f5cf63b569a899ae99e1fdefc034a5e76f），后者真实进程EXIT_CODE=0。两票输入同一V0.17候选，分类/11行队列值未改变；两份原字节前缀保留，B2未新增独占锁哈希行。下一步仅B5-r2；尚未模块PASS。

| ID | 当前来源 | 分类 | 工程缺陷 | 修复队列归属 | 当前处置 |
|---|---|---|---|---|---|
| M001-001 | 上批Grok GR-01 | 冲突 | 是，已修 | 否 | 上批B4-r1双PASS后出队，原证据保留 |
| M001-002 | 上批Grok GR-02 | 冲突 | 是，已修 | 否 | 上批新人一年间隔规则已修，不重复执行 |
| M001-003 | 上批Grok GR-03 | 冲突 | 是，已修 | 否 | 上批合同变更八类已修，不重复执行 |
| M001-004 | 本次Codex V0.25 CX-01及既有登记 | 技术性 | 否 | 否 | CSS/结构/导航无业务发明；具体表头缺陷独立登记008，不以本行抵销 |
| M001-005 | 当前Grok V0.06 GR-01 | 冲突 | 是，已修 | 否 | 投诉统一确认/默认关闭冲突；依最新批准按商户/消费客人两类修复，申请14/18/19 ；B4-r2双PASS后出队 |
| M001-006 | 当前Grok V0.06 GR-02 | 细化 | 是，已修 | 否 | 两处园区容量空答案改引用已存在单项目容量规则，申请15/16 ；B4-r2双PASS后出队 |
| M001-007 | 当前Grok V0.06 GR-03 | 细化 | 否 | 否 | 手环句与相邻三环节通用门槛合取，无明确豁免；原Grok成立意见保留 |
| M001-008 | 当前Grok V0.06 GR-04 | 技术性 | 是，已修 | 否 | 13表无任何表头；补既有问题/答案列语义及匹配样式，申请17；无业务发明 ；B4-r2双PASS后出队 |
| M001-009 | 当前Grok V0.06 GR-05 | 细化 | 否 | 否 | 有BEQR-042/BSM-035上位，按顺序先细化；EXPIRED为内部流水状态描述，已有中文，不是产品界面英文显示证据 |
| M001-010 | 当前Grok V0.06 GR-06 | 细化 | 否 | 否 | 大屏叫号是2026-09-01用户明确批准的原句；有现场显示用途，不扩写/不删 |
| M001-011 | 当前Grok V0.06 GR-07 | 细化 | 否 | 否 | 枚举给包场禁核销、地图给审批；三份共同解释，未冻结每份必须重复完整条款的要求 |

## B3第2次已执行的批准清单（留存证据，不再执行）

本批已修集：M001-005、M001-006、M001-008；当前修复队列为空，其他8个ID未改。14/18/19当前两类投诉稿均逐处获批；15/16/17先前逐处批准有效，全部真实答复在两份raw输入与下方历史中可复核。投诉只按用户最终商户/消费客人两类规则写入，不带入旧主管审批、确认超时或无回应转引稿。
版本递增：枚举V0.06→V0.07、业务地图V0.07→V0.08，均归因M001-005；治理地图不变。文档/HTML内容修复依runbook第5节采用可复现命令验证，不额外生成测试文件。

```json
[
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1384,
    "old": "投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；",
    "new": "投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；",
    "id": "M001-005",
    "approval": "14 两类投诉稿"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1384,
    "old": "其关闭同样须提单方确认。",
    "new": "其关闭按上述消费客人投诉规则执行。",
    "id": "M001-005",
    "approval": "14 两类投诉稿"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 262,
    "old": "投诉类须提单方确认才能关闭",
    "new": "投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭",
    "id": "M001-005",
    "approval": "18 两类投诉稿"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1378,
    "old": "客人（客人端投诉，客人即提单方，关闭须其确认）",
    "new": "客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）",
    "id": "M001-005",
    "approval": "19 两类投诉稿"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 146,
    "old": "<tr><td class=\"k\">园区容量</td><td>园区容量。</td></tr>",
    "new": "<tr><td class=\"k\">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>",
    "id": "M001-006",
    "approval": "15"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1295,
    "old": "<tr><th scope=\"row\">园区容量</th><td>园区容量。</td></tr>",
    "new": "<tr><th scope=\"row\">园区容量</th><td>只按单个游乐项目的实时在玩人数控制容量，具体按本表「容量控制」条执行。</td></tr>",
    "id": "M001-006",
    "approval": "16"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 14,
    "old": "  td { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; }",
    "new": "  td, th { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; } th { text-align: left; }",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 28,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 64,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 99,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 139,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 158,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 186,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 207,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 216,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 225,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 247,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 278,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 289,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 295,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  }
]
```


B4-r2双PASS收据：Codex modules/M001-b4-r2-self-raw.md V0.01 SHA256=1edaff6616ab23b1709dc01d99e9a687dafbc260486e513f96c84417f774e4a2；Grok modules/M001-b4-r2-grok-raw.md V0.01 SHA256=214eedda3e430191bda987e1242d0a622527739602cb2212772a6bdd3f1bc118；真实进程EXIT_CODE=0。两席均完整审30865字节/188行diff并原样写入，18hunk全部归因。当前无待答复申请；全部旧批准、拒绝稿和原FAIL证据保留。模块尚未B5 PASS。

## 历史候选、原始意见及逐处批准（非当前状态）

以下完整保留经双席审核的V0.13及其全部历史；其中“候选/待批”等仅描述当时状态，当前以本文件上方定稿表和修复清单为准。

# 全量审计 Findings 台账
版本：V0.13

## 当前 B2 整批候选（唯一当前表，尚未双 PASS 定稿）

控制提示词V0.11；首轮全量审计；M001/322；B3已执行1次；LOCK-AMEND-COUNT=4；锁V0.04。当前已从BLOCKED-ASK恢复IN_PROGRESS，仅恢复B2，不重跑A阶段/B1。本批六个逐处申请14、15、16、17、18、19全部收到实际批准；14/18/19仅以最新“两类投诉稿”为准，早期拒绝与批准记录保留为历史，不恢复旧稿。无未答HTML申请，无添加项。

输入完整报告：
- Codex：docs/audit-2026-09/modules/M001-b1-self-raw.md，最后一条独占锁哈希行起，包含V0.16完整独审及后续整批互读/真实批准记录。当前B1原票PASS；CX-01映射既有M001-004。
- Grok：docs/audit-2026-09/modules/M001-b1-grok-raw.md，最后一条独占锁哈希行起的V0.06完整B1；当前原票FAIL、GR-01至07共七条，接收整文件SHA256=9d44fdbb305c9edaa497af1185757e8ae1da1cbe1d560d9cc30932cf5a2357b4。
- 所有原始Grok finding原文在其raw以及本文件历史记录中保留，没有改写原票。下表的差异是Codex提请Grok复核的候选处置；不是已生效裁决，也不是以自审推翻Grok。任一异议不被Grok接受，B2仍FAIL，按其整批意见修候选；不进入B3。

| ID | 当前来源 | 候选分类 | 候选工程缺陷 | 候选修复队列归属 | 候选处置 |
|---|---|---|---|---|---|
| M001-001 | 上批Grok GR-01 | 冲突 | 是，已修 | 否 | 上批B4-r1双PASS后出队，原证据保留 |
| M001-002 | 上批Grok GR-02 | 冲突 | 是，已修 | 否 | 上批新人一年间隔规则已修，不重复执行 |
| M001-003 | 上批Grok GR-03 | 冲突 | 是，已修 | 否 | 上批合同变更八类已修，不重复执行 |
| M001-004 | 当前Codex CX-01及上批登记 | 技术性 | 否 | 否 | CSS/结构/导航无业务发明；具体表头缺陷独立登记008，不以本行抵销 |
| M001-005 | 当前Grok V0.06 GR-01 | 冲突 | 是 | 是 | 投诉统一确认/默认关闭冲突；依最新批准按商户/消费客人两类修复，申请14/18/19 |
| M001-006 | 当前Grok V0.06 GR-02 | 细化 | 是 | 是 | 两处园区容量空答案改引用已存在单项目容量规则，申请15/16 |
| M001-007 | 当前Grok V0.06 GR-03 | 细化 | 否（提请Grok复核） | 否 | 手环句与相邻三环节通用门槛合取，无明确豁免；原Grok成立意见保留 |
| M001-008 | 当前Grok V0.06 GR-04 | 技术性 | 是 | 是 | 13表无任何表头；补既有问题/答案列语义及匹配样式，申请17；无业务发明 |
| M001-009 | 当前Grok V0.06 GR-05 | 细化（原Grok技术性，提请复核） | 否（提请Grok复核） | 否 | 有BEQR-042/BSM-035上位，按顺序先细化；EXPIRED为内部流水状态描述，已有中文，不是产品界面英文显示证据 |
| M001-010 | 当前Grok V0.06 GR-06 | 细化 | 否（提请Grok复核） | 否 | 大屏叫号是2026-09-01用户明确批准的原句；有现场显示用途，不扩写/不删 |
| M001-011 | 当前Grok V0.06 GR-07 | 细化 | 否（提请Grok复核） | 否 | 枚举给包场禁核销、地图给审批；三份共同解释，未冻结每份必须重复完整条款的要求 |

当前候选修复集仅M001-005、M001-006、M001-008。全部11个永久ID有且只有一个当前表行，未复用ID。历史同名GR临时号按raw追加版本区分。B2双PASS之前上表不得作为可执行B3队列。

## 提请Grok核实的四项差异

1. M001-007：枚举43三环节逐道通过且无人工放行；地图800同场次重复进出仍须三道、不重复扣次数。手环44/801未明文豁免任一步，两条条件可以同时适用。原GR-03推定手环会绕过门槛，尚无相反命令或真实实现证据。未提新规则，未改HTML。
2. M001-009：枚举83/地图826说“退回流水照建但直接落 EXPIRED（不可用）”，说明系统流水终态，括号已有中文；没有证据表明产品给客人显示英文。GEN-015的界面语言要求不能自动解释成文档不得出现机读标识。按四分类先指BEQR-042/BSM-035上位，故候选细化；保留原GR-05技术性分类全文供复核。
3. M001-010：地图1261“大屏叫号”就是当前权威原句；决策台账2469–2477只用于核实其2026-09-01用户明确批准及不扩写边界。GEN-001要求有用途，不要求每条功能概要都定义状态机；GEN-016是路径/命令/版本/状态/完成声明可复现。不能为一句获批功能概述添加未批准的排队机制，或仅因短就删掉。
4. M001-011：枚举25要求配套地图使用，枚举306/地图2003明确三份共同解释；枚举153已经给包场期间整体停止对外核销，仅该团体客人可入；地图1300只补主管审批和越界复核，没允许对外核销。两者共同构成需求，不存在明文互斥；不能把每份逐句自足作为锁外新要求。

上述均是候选论证，不是最终驳回。Grok须独立判断，每条接受或否决均给证据，不得因Codex建议就跟随。B1原合取仍FAIL，因为005/006/008待修；B2 PASS只代表整批归并可定稿，绝不是模块PASS。

## 本批逐处批准与修复范围

- 申请14：操作员答“批准申请 14 两类投诉稿（推荐）”；地图1384替换投诉关闭主段及后句。最新规则精确见下面替换清单。
- 申请18：操作员答“批准申请 18 两类投诉稿（推荐）”；枚举262同步同一两类正文。
- 申请19：操作员答“批准申请 19 两类投诉稿（推荐）”；地图1378按最新消费客人规则改写，当前批准取代此前对不同文字的拒绝。
- 申请15、16：分别实际答“批准申请 15（推荐）”“批准申请 16（推荐）”；枚举146/地图1295分别引用同表容量规则。
- 申请17：实际答“批准这项技术修改（推荐）”；只在13个表起始行补问题/答案列头，14行扩展已有样式选择器并增加左对齐，197业务项及td.k保留。

两类投诉为用户最新原始决定：商户通常确认解决；按既有规则无法解决，或解决后商户未确认/未点击关闭，由客户关怀部记录信息及关闭缘由后关闭。消费客人投诉由客户关怀部记录解决方案及客人反馈后关闭。早期候选中的主管审核、确认超时、无回应自动/人工关闭、“其他投诉”泛称全部不作为当前修复授权。普通工单既有时效提醒与复杂设备验收条款不变。

修改后的文件自身版本依用户版本铁律分别前进一次：枚举V0.06→V0.07，业务地图V0.07→V0.08；版本行归因M001-005。治理地图不改。不修M001-004/007/009/010/011。

## 已批准的精确替换清单

按目标文件的一基行号定位，在该行只替换唯一匹配的旧片段。此清单在B2双PASS后才执行；当前仅用于复核授权和归因。

```json
[
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1384,
    "old": "投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；",
    "new": "投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；",
    "id": "M001-005",
    "approval": "14 两类投诉稿"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1384,
    "old": "其关闭同样须提单方确认。",
    "new": "其关闭按上述消费客人投诉规则执行。",
    "id": "M001-005",
    "approval": "14 两类投诉稿"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 262,
    "old": "投诉类须提单方确认才能关闭",
    "new": "投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭",
    "id": "M001-005",
    "approval": "18 两类投诉稿"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1378,
    "old": "客人（客人端投诉，客人即提单方，关闭须其确认）",
    "new": "客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）",
    "id": "M001-005",
    "approval": "19 两类投诉稿"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 146,
    "old": "<tr><td class=\"k\">园区容量</td><td>园区容量。</td></tr>",
    "new": "<tr><td class=\"k\">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>",
    "id": "M001-006",
    "approval": "15"
  },
  {
    "path": "docs/business-system-map-for-operations-v0.1.html",
    "line": 1295,
    "old": "<tr><th scope=\"row\">园区容量</th><td>园区容量。</td></tr>",
    "new": "<tr><th scope=\"row\">园区容量</th><td>只按单个游乐项目的实时在玩人数控制容量，具体按本表「容量控制」条执行。</td></tr>",
    "id": "M001-006",
    "approval": "16"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 14,
    "old": "  td { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; }",
    "new": "  td, th { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; } th { text-align: left; }",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 28,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 64,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 99,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 139,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 158,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 186,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 207,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 216,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 225,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 247,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 278,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 289,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  },
  {
    "path": "docs/business-enum-quick-reference-v0.1.html",
    "line": 295,
    "old": "<table>",
    "new": "<table><thead><tr><th scope=\"col\">问题</th><th scope=\"col\">答案</th></tr></thead>",
    "id": "M001-008",
    "approval": "17"
  }
]
```

## 历史归并、原始意见及审批记录（以下不作为当前队列/当前拟写文本）

当前唯一候选表与精确替换清单在上方。以下整段保留V0.12原内容，旧“待批准/拒绝/主管审核”只表示当时状态；本次新两类稿的实际批准已在上方明确列出。

# 全量审计 Findings 台账
版本：V0.12

## 当前批次

V0.11；首轮全量审计；M001/322；B3已执行1次；LOCK-AMEND-COUNT=4。锁V0.04下B1整模块双报告均已收齐：Codex V0.16 PASS，Grok V0.06 FAIL（7项）。四值一致。下列为整批候选，尚未B2双PASS定稿；当前BLOCKED-ASK：15、16、17已批准；操作员明确重定“商户投诉/消费客人投诉”两类关闭规则。18旧稿批准及19旧稿拒绝保留；当前唯一有效投诉提案为文末14/18/19两类稿，尚待逐处批准。旧申请7–13全部实施并通过B4-r1，不再是待实施申请。两个当前B1 raw路径：modules/M001-b1-self-raw.md、modules/M001-b1-grok-raw.md；各取最后一条独占锁哈希行开始的本次完整报告。

## Codex 对当前七项 Grok 意见的核实（不是 B2 定稿）

已按 receiving-code-review 的核实规则逐项复查。B1 原始独审仍为 Codex PASS / Grok FAIL；互读后合取 FAIL，绝不以自审覆盖外审。七条完整意见全部登记，未删任何条目。当前 Grok 报告系追加 V0.06，不得与上批 V0.04 的同名 GR-01/02/03 混淆。

| 正式ID | 当前来源 | 分类 | 工程缺陷（候选） | 修复队列归属（候选） | 内容及核实 |
|---|---|---|---|---|---|
| M001-005 | Grok V0.06 GR-01 | 冲突 | 是 | 是 | 投诉需确认关闭与超时默认关闭；接受删除业务地图1384旧超时例外，申请14 |
| M001-006 | Grok V0.06 GR-02 | 细化 | 是 | 是 | 两处“园区容量。”为空答案；申请15、16仅引用已有单项目容量规则 |
| M001-007 | Grok V0.06 GR-03 | 细化 | 是（保留Grok判定；Codex异议） | 是 | 手环与三环节关系；相邻明确通用规则已适用于再入，未发现手环豁免明文，待Grok B2复核 |
| M001-008 | Grok V0.06 GR-04 | 技术性 | 是 | 是 | 13张表无表头；接受可访问性缺陷；申请17补已有“问题/答案”列语义；无业务发明 |
| M001-009 | Grok V0.06 GR-05 | 技术性（保留原分类，待Grok复核） | 是（保留Grok判定；Codex异议） | 是 | EXPIRED处在描述机器流水终态的句子，已有“不可用”中文；并非产品界面英文展示证据 |
| M001-010 | Grok V0.06 GR-06 | 细化 | 是（保留Grok判定；Codex异议） | 是 | “大屏叫号”是2026-09-01操作员点名批准的精确原句；决策记录还明确不扩写 |
| M001-011 | Grok V0.06 GR-07 | 细化 | 是（保留Grok判定；Codex异议） | 是 | 枚举已定义包场禁对外核销，地图补主管审批；两份共同解释而非各自完整；地图未明文允许对外核销 |

M001-001/002/003 保持上批 B4-r1 双 PASS 后出队的“否”；不复用其ID。M001-004继续只登记为“否”，并吸收当前 Codex V0.16 CX-01 同范围技术登记；当前缺陷 M001-008 独立编号，不能用004笼统“无缺陷”抵销。

当前候选七项暂均保留 Grok 的工程缺陷及“是”，这些值未经 B2 双 PASS 定稿，不是 B3 的新执行许可。上述四项异议只有经 Grok 独立复核同意后才能改变当前值；不会自动出队。客人最新规则对应的14/18/19当前稿逐处批准前全流程暂停，不派 B2 或进入其他模块。

### 四项异议的具体证据

- M001-007：枚举43规定三环节逐道通过、无人工放行；地图800明确同场次重复进出仍须三道但不重复扣次数。44/801手环句未写任何豁免。两条条件可合取，不能仅因概括句短就推定相邻强约束失效；须由 Grok 复核本推断。
- M001-009：枚举83、地图826现行原文都是“退回流水照建但直接落 EXPIRED（不可用）”，对象是机器流水；本席没有证据证明产品向客人显示英文。技术性分类与有上位券包需求的先细化顺序也须复核。保留Grok原意见，不自行更改代码名或业务状态。
- M001-010：地图1261现行原文“大屏叫号”。docs/prd-audit/business-decision-ledger.md:2469–2477 的2026-09-01记录明确“James亲口批准”这四字，并明确不扩写为电子排队号、远程取号、到场保留时间或过号机制。该记录只证明现行HTML原句的确认时间和修改边界，不把未进HTML的内容当新业务需求；GEN-001要求可验证用途，该词已有现场显示叫号用途；GEN-016未要求每条功能概要含详细状态机。删掉或擅自细化会超出用户原批准。
- M001-011：枚举25“配套两份业务地图…使用”，枚举306/地图2003明确三份共同解释。枚举153提供包场禁对外核销，地图1300提供审批和越界限制，二者无相反命令。Grok自己确认“不是明文相反”；单份地图缺少重复句不能直接证明整体需求遗漏，不能把未冻结的“每份HTML必须逐句自足”要求当判据。

### 核实与拟稿验证

远程逐字读取当前1384、146/1295、44/801、83/826、1261、1300及枚举25/306；确认13张表、197个td.k、每行1个问题单元格、没有th。原始B1机械检查已覆盖完整HTML结构和导航。拟稿在内存中预演，源文件未写。
表头按原生th/scope提供列语义。W3C ARIA in HTML 的td规则（https://www.w3.org/TR/html-aria/#el-td）不支持直接在原生table内td上改role=rowheader，故未提出该错误捷径；这是GEN-015的现有技术证据，不新增判据。

## 首次申请14–17及批准记录（15、16、17已批准；14以文末当前修订稿为准；均未实施）

以下行号依据当前枚举 V0.06、业务地图 V0.07。每项仅授权所列替换，其他正文保留；如本批获批并经 B2 双 PASS 后执行 B3，实际被修改文件自身版本末位前进一次。

### 申请14 — M001-005 / 当前 GR-01

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1384。
原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类一律须提单方确认才能关闭；
理由：与枚举262及本行客人投诉关闭须本人确认的规定一致。DEC-R6-13/14 的 2026-08-04 确认记录比本段 2026-06-25 旧来源更晚；仅用决策台账识别确认时间，不把它作为第四业务源。
批准状态：等待操作员逐项答复。

### 申请15 — M001-006 / 当前 GR-02（枚举落点）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：146。
原文：<tr><td class="k">园区容量</td><td>园区容量。</td></tr>
替换为：<tr><td class="k">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>
理由：用紧邻145行已经批准的容量口径替代没有规则的重复标签；保留原锚点，不新设园区容量数值或维度。
批准状态：已批准。操作员实际弹窗答复：“批准申请 15（推荐）”。整批B2双PASS前不实施。

### 申请16 — M001-006 / 当前 GR-02（业务地图落点）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1295。
原文：<tr><th scope="row">园区容量</th><td>园区容量。</td></tr>
替换为：<tr><th scope="row">园区容量</th><td>只按单个游乐项目的实时在玩人数控制容量，具体按本表「容量控制」条执行。</td></tr>
理由：与1294既有口径一致，为此空白条目给出同表引用，不新设规则。
批准状态：已批准。操作员实际弹窗答复：“批准申请 16（推荐）”。整批B2双PASS前不实施。

### 申请17 — M001-008 / 当前 GR-04

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：14；28、64、99、139、158、186、207、216、225、247、278、289、295。
第14行原文：
  td { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; }
第14行替换为：
  td, th { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; } th { text-align: left; }
上述13个表格起始行分别将 <table> 替换为：
<table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
理由：13张表目前没有任何表头。补入“问题／答案”列标题，沿用25行已经定义的两列用途，给辅助阅读工具提供列语义；不改197项业务文字，不改 td.k 锚点，不改CSS现有取值，不新增业务规则。本次不把缺少caption本身当作另一缺陷。
批准状态：已批准。操作员实际弹窗答复：“批准这项技术修改（推荐）”。整批B2双PASS后统一实施。

当前 GR-03、GR-05、GR-06、GR-07 保留全部原始意见与否决权；本席有证据异议，待本批人闸答复后由 Grok 在 B2 独立复核；本次不为这四项提出无必要的 HTML 改动，也不自行关项。

## 当前 Grok V0.06 七条原始 finding（逐字保留）

### GR-01

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:262`；`docs/business-system-map-for-operations-v0.1.html:1384`
- 证据（类型 1，HTML 逐字原文）：
  - 枚举 L262：「投诉类须提单方确认才能关闭」
  - 业务地图 L1384：「投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭……单独统计为“未确认关闭”」
- 四分类：**冲突**（两份最高解释权 HTML 对同一关闭闸的现行正文互相否定：前者关闭以确认为必要条件，后者允许无确认超时关闭）
- 冻结判据：BEQR-172、BSM-079、GEN-008、GEN-016
- 工程缺陷：**是**
- 处置含义：属三份业务 HTML 自身问题。按铁律 4 **只许上报并暂停**，本席不改、不删、不重命名。后续由调度席逐条申请操作员批准。本席不把「后确认覆盖先确认」自行判赢，因为裁决台账不在本模块。

### GR-02

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:146`；`docs/business-system-map-for-operations-v0.1.html:1295`
- 证据（类型 1+4）：两处右列均为「园区容量。」；同表/同章「容量双阈值 / 容量控制」写明只按单个项目实时在玩人数、不再按场次/时段或点位/设备另设容量。结构解析确认 BEQR-089 锚点存在且来源行对齐。
- 四分类：**细化**（上位是游乐容量口径 BEQR-088/BSM-071；本锚点没有给出园区级规则，也没有声明「不设园区容量」）
- 冻结判据：BEQR-089、BSM-071、GEN-001、GEN-016
- 工程缺陷：**是**（无需求承载的空答案；第一次实现就会发明或忽略园区容量）

### GR-03

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:44`；`docs/business-system-map-for-operations-v0.1.html:801`
- 证据（类型 1）：两处均为「有手环则当前场次内允许入场。」对照 BEQR-012 L43 / BSM-034 L800：三道环节须逐道通过；同一场次再入仍须三道、不重复扣次数。
- 四分类：**细化**（上位是入场/核销；手环未定义与检票、年龄核验、再入的关系）
- 冻结判据：BEQR-013、BSM-034、GEN-016
- 工程缺陷：**是**

### GR-04

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:28-304`（13 张 `<table>`，0 个 `<th>` / `<caption>`）
- 证据（类型 4）：`beqr.count('<table')==13` 且 `count('<th')==0`；对照业务地图 `plain-table` 使用 `<th>` 与 `scope="row"`
- 四分类：**技术性**（表头标记；**无业务发明**：不新增实体/枚举/流程，只是把已有左列问题标成表头）
- 冻结判据：GEN-015
- 工程缺陷：**是**（标签/可读性：左列语义是表头却标成数据单元格）
- 无业务发明理由：改的是 HTML 表头语义，不改变任何业务取值。

### GR-05

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:83`；`docs/business-system-map-for-operations-v0.1.html:826`
- 证据（类型 1）：「原积分在退回时点已过期的，退回流水照建但直接落 EXPIRED（不可用）」；同条中文分组已有「已过期」
- 四分类：**技术性**（机读态名出现在业务展示文案；括注已给中文。**无业务发明**：终态仍是「不可用/已过期」，未新造业务状态）
- 冻结判据：GEN-015、BEQR-042、BSM-035
- 工程缺陷：**是**（业务速查/地图对同事展示未声明为「系统枚举码」的英文终态，与全中文展示约定不一致）
- 无业务发明理由：未增加积分状态机取值，只是把机读标识写进展示句。

### GR-06

- 文件:行号：`docs/business-system-map-for-operations-v0.1.html:1261`
- 证据（类型 1）：主要功能列表单独一项为「大屏叫号」，无规则句；同列表前后项均有完整行为说明（出场自动扣减、在玩人数禁止人工调整等）
- 四分类：**细化**（上位 BSM-068/BSM-069 游乐现场能力）
- 冻结判据：BSM-069、GEN-001、GEN-016
- 工程缺陷：**是**

### GR-07

- 文件:行号：`docs/business-enum-quick-reference-v0.1.html:153`；`docs/business-system-map-for-operations-v0.1.html:1300`
- 证据（类型 1）：枚举 L153「包场期间该游乐项目整体停止对外核销，仅该团体客人可入」。业务地图 L1300 团体/包场行无该禁核销句；全文检索「停止对外核销」「仅该团体」在业务地图 0 命中（除本报告外）。
- 四分类：**细化**（不是明文相反，是权威源已写、同模块另一权威文件遗漏；第一次只按地图实现会继续对外核销）
- 冻结判据：BEQR-095、BSM-071、GEN-008
- 工程缺陷：**是**



## 上批记录（历史证据；M001-001至004）

以下出现“待B2”“尚未实施”等均为当时历史，当前值以上方说明及末尾B4-r1记录为准。原意见与批准原句保留。

| ID | 来源 | 分类 | 工程缺陷 | 修复队列归属 | 内容及裁决 |
|---|---|---|---|---|---|
| M001-001 | GR-01 | 冲突 | 是，文档口径/重复总结；原推断分歧保留 | 否 | 申请7删除地图1351末句、申请8将1337“五态”改“状态”；两项已批准，待本批定稿后B3实施；B4-r1双PASS，已修复出队 |
| M001-002 | GR-02 | 冲突 | 是，资格规则口径 | 否 | 申请9已确认不足一年不可、满一年及以上可再享；11至13修改文字已逐处获批；旧长期留存提案撤回；B4-r1双PASS，已修复出队 |
| M001-003 | GR-03 | 冲突 | 是，概要类型不齐；闭集推断分歧保留 | 否 | 申请10治理地图537补齐已有八类，已批准；待本批定稿后B3实施；B4-r1双PASS，已修复出队 |
| M001-004 | Codex本次raw技术性分类登记，无临时ID | 技术性 | 否 | 否 | 两份文档的CSS/静态结构及业务地图页内导航；只登记，无业务发明，无需修复 |

## M001-001处置与保留分歧

原始GR-01全文见后附。本席不赞同“有争议可挂起仲裁”必然取消同段主管出口的推断；接受删除重复旧总结与中性化数量注释，保留Grok否决，不单方关项。
批准7：业务地图1351行删除完整句子“退租状态分待结算、押金抵扣中、欠费追缴中、已结清，有争议可挂起仲裁。”。
批准8：业务地图1337行“退租结算五态”替换为“退租结算状态”。
依据：原始GR-01的BEQR-164、BSM-076、GEN-008/016/018；注释所在BSM-075，仅使用已有判据。

## M001-002处置与操作员最新裁决

操作员最新原文：申请 9   按注册手机号判定需短信验证。同一手机号没买过才算新人。注销后重新注册，如果重新注册时间和注销时间间隔不足一年，不可再享新人资格 。如果重新注册时间和注销时间间隔超过一年。可以再享新人资格，系统自动判断。

操作员边界答复原文：可以再享（满一年及以上）

申请9当前获准文字：按注册手机号判定，需短信验证。同一手机号没买过才算新人。注销后重新注册的，重新注册时间与注销时间间隔不足一年，不可再享新人资格；满一年及以上，可以再享新人资格，由系统自动判断。

上述最新裁决覆盖此前“注销后默认不可再享”的永久排除方向。申请12、13旧拟稿中的“长期保留该手机号不可再享新人资格的判定记录”明确撤回，未曾实施。旧指令与旧提案仅作为ledger/self raw历史记录保留，不再作为本批待批准方案。
满一年边界已由操作员在弹窗明确裁决，按“不足一年”与“满一年及以上”划分，无边界空档；申请11至13随后均已逐处获弹窗批准，详情见当前批次与对应申请，不以边界答复替代逐处许可。
同一GR-02保留原始发现与正式ID；申请11至13继续是该ID的同步源位置，已按一年间隔方向重拟并获操作员逐处批准；保留原始Grok意见，不假称新规则已获Grok B2 PASS。
资格定义同步仍涉及枚举39、68；留存用途同步涉及枚举68和地图862。现有一年注销事件记录可用于一年内判定，但当前仅授权异常预警用途，该用途变更现已由申请12、13逐处获准；不另提长期资格记录。满一年时已允许再享，与既有一年窗口届满删除记录的方向相容；本次提案不延长留存期限。
地图818关于新旧账户不关联的正文保留。枚举38/67的有日期历史注释保留，不当作现行新人资格规则。只使用既有BEQR-009、BEQR-030、BSM-033、BSM-035、BSM-038、GEN-008等判据，没有修改冻结原文或新增判据。

## M001-003处置与保留分歧

原始GR-03全文见后附。本席不认定所有无“等”的列举必为闭集；同意把治理概要列齐另两份已明确的八类。用户已批准申请10，不增加第九类。
批准10：治理地图537行前缀“管理合同全生命周期变更：续租、退租、面积变更、租金调整、经营范围变更和提前终止”替换为“管理合同全生命周期变更：续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）”；其后原文保留。
依据：BEQR-163、BSM-028、BSM-076、GEN-008；PGM-029仅权威对照。本批准仅授权该处源修改，不代表开始M002全量审计。

## M001-004技术性登记

位置：枚举HTML及业务地图的CSS/静态结构；业务地图2008至2029行页内导航脚本。来源：Codex本次raw的“三线审计与分类”与“现场验证与结果”。
无业务发明理由：只控制文档展示、锚点导航和活动栏目，不引入业务实体、状态、价格、权限或交易动作。
证据：原始self raw中的HTMLParser和从真实HTML提取脚本执行的Node vm断言；初始位置、4个滚动位置及119/120/121像素边界通过。GEN-002/015/016/017/018。无缺陷，候选归属否，仍需Grok核对分类，未以技术性免审。

## 本批HTML同步申请（已逐处批准，待B2双PASS后实施）

### 申请11（满一年边界已确认后的修改提案）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：39

原文：新人（按手机号自动判；注销后重新注册默认不穿透历史，可再享新人资格）
拟改：新人（按注册手机号判定，需短信验证；同一手机号没买过才算新人；注销后重新注册的，重新注册时间与注销时间间隔不足一年不可再享新人资格，满一年及以上可以再享，由系统自动判断）

理由：同步申请9已确认的规则：不足一年不可再享，满一年及以上可以再享。
批准状态：已批准。操作员弹窗答复原文：“批准申请 11”。尚未实施，须等本批B2双PASS。

### 申请12（满一年边界已确认后的修改提案）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：68

原文：重新注册默认不穿透历史，可再享新人资格。
拟改：注销后重新注册的，重新注册时间与注销时间间隔不足一年不可再享新人资格，满一年及以上可以再享，由系统自动判断。

原文：为完成该计数，系统仅留存注销事件与该手机号的对应记录至一年统计窗口届满，不得用于其他目的、不与新账户建立任何关联（与《业务系统地图》「账户注销」条的删除例外一致）。
拟改：为完成上述计数及注销后重新注册的新人资格判定，系统仅留存注销事件与该手机号的对应记录至一年统计窗口届满；该记录仅用于上述两项目的、不与新账户建立任何关联（与《业务系统地图》「账户注销」条的删除例外一致）。

理由：同步已确认的一年间隔规则，并给现有一年注销事件记录补入新人资格判定用途；不新增长期留存记录。
批准状态：已批准。操作员弹窗答复原文：“批准申请 12”。尚未实施，须等本批B2双PASS。

### 申请13（满一年边界已确认后的修改提案）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：862

原文：专为执行枚举速查表「会员身份状态」条的『同一手机号一年内注销与注册合计次数』异常预警而留存
拟改：仅为执行枚举速查表「会员身份状态」条的『同一手机号一年内注销与注册合计次数』异常预警及注销后重新注册的新人资格判定而留存

理由：给现有一年注销事件记录补入新人资格判定用途，保留既有一年删除窗口及不关联新旧账户的限制。
批准状态：已批准。操作员弹窗答复原文：“批准申请 13”。尚未实施，须等本批B2双PASS。

同批获批并经B2双PASS后，B3才执行全部修复；届时各被修改HTML自身版本只前进一次：枚举V0.05→V0.06、业务地图V0.06→V0.07、治理地图V0.06→V0.07。当前版本未改。


## 本批B2定稿证据

两席共同审核的候选是V0.04，SHA256=c4e4b8d11a4afe429a2fbba0f2480132170c06045ae136a88cadaa802055fbc5。V0.05仅登记双PASS定稿状态及证据，不改变任何ID、类别、修复文字或队列归属。
- Codex：modules/M001-b1-self-raw.md末尾“ M001 B2整批归并复核 — Codex ”，追加版本V0.15，B2结论PASS。
- Grok：modules/M001-b1-grok-raw.md末尾“ M001 / B2 整批候选台账复核（Grok 席乙） ”，追加版本V0.05，B2结论PASS；真实进程EXIT_CODE=0，元数据适配器交付成功。
- Grok原283行完整字节前缀哈希仍f0d6eca4bcfd65c67fc3e1ce33e633769926731abd6a9616cc591a8265f20397；仅追加本次B2完整报告，没有增加独占锁哈希分隔行。
- 修复队列仍M001-001、M001-002、M001-003；M001-004否。B3须整批修完，不按ID逐个往返。


## B4-r1双PASS与出队

本轮完整diff：before=d78e08ac86b26a71dc016eb1b48c05823468664d，after=de7e9ee39628bf79a65d40f20be8dd2fb6c5e27c，SHA256=33f9c2d96e64d4acb0e46237aba71812af6aff468dba707844e0d5ea9a90d0c4。
Codex：modules/M001-b4-r1-self-raw.md追加V0.02，B4 PASS；Grok：modules/M001-b4-r1-grok-raw.md V0.01，B4 PASS，真实EXIT_CODE=0。Grok报告内29560字节完整diff与git diff输出逐字相同，第一行实算锁哈希正确，未读取本席B4报告后跟随作答。
依V0.11 B4双PASS后出队规则，M001-001/002/003修复队列归属改否；M001-004原本即否。保留三个原始finding及其历史审批/处置，不删除ID，不据此宣称模块最终PASS。
引文定位更正：申请8的地图1337历史注释实际被BSM-074冻结块包含，不是此前说明中的BSM-075；GEN-016及申请8逐处许可始终有效，实际源文修复未超范围。Grok B4附表列六个待同步引文并仅检查BSM-075是否含该注释；本席完整锁引文检索证实还须同步BSM-074，共七个。该事实用于后续既有LOCK-AMEND，不改写任何原始Grok报告。
后续先完成LOCK-AMEND对应原文同步与双席复核，再按其第⑤步对受影响M001从B1重新核验，保留本轮历史与B3=1计数，不重新开展A0范围固化。

## 原始Grok Findings（逐字保留，不改写结论）

### GR-01 — 退租结算状态：同一段落两套闭集

- 临时 ID：GR-01
- 文件:行号：`docs/business-system-map-for-operations-v0.1.html:1351`（同文件注释 L1337；对照 `docs/business-enum-quick-reference-v0.1.html:252`、`docs/platform-governance-map-for-operations-v0.1.html:540`）
- 原文（BSM L1351，前句与末句同 li）：
  - 前句：`有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起，直至诉讼或仲裁有结果后再行处理`
  - 末句：`退租状态分待结算、押金抵扣中、欠费追缴中、已结清，有争议可挂起仲裁。`
  - 注释 L1337：`退租结算五态`
  - 枚举表 L252：`待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」`
- 证据类型：1（HTML 逐字）+ 4（子串检索命令）
- 冻结判据：BEQR-164、BSM-076、GEN-008、GEN-016、GEN-018
- 分类：**冲突**（不能指向上位条目消化：上位枚举已是六态；末句把它收成四态并把「有争议」直接等同仲裁）
- 工程缺陷是否成立：**成立**。同一权威段对同一状态机给出两个不能同时执行的闭集：前句允许主管裁决后继续（不必进诉讼），并单列「有争议，诉讼/仲裁」；末句没有第六态，且「有争议可挂起仲裁」取消了「裁决后继续」出口。注释「五态」与枚举六态亦不符。
- 铁律 4 暂停：是。禁止实际改 HTML。

**申请 1（仅申请，未改）：**

- 文件：`docs/business-system-map-for-operations-v0.1.html`
- 行：1351 该 `<li>` 末句「退租状态分待结算、押金抵扣中、欠费追缴中、已结清，有争议可挂起仲裁。」
- 拟改：删除该末句；或改成与前句及枚举表一致的六态收口，例如「退租结算状态为：待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」。」
- 同行注释 L1337「退租结算五态」拟改为「退租结算六态」
- 为什么：消除同段及与 BEQR-164 的状态闭集互斥
- 批准前不得修改

### GR-02 — 新人资格：终身「没买过」对「注销不穿透」

- 临时 ID：GR-02
- 文件:行号：`docs/business-system-map-for-operations-v0.1.html:781`（对照枚举表 L39、L68；同文件 L818）
- 原文：
  - BSM L781：`新人优惠……同一手机号没买过才算新人，系统自动判。`
  - BEQR L39：`新人（按手机号自动判；注销后重新注册默认不穿透历史，可再享新人资格）`
  - BEQR L68：`「已注销」由客人自行发起……新账户与原账户之间不建立任何关联；重新注册默认不穿透历史，可再享新人资格。`
  - BSM L818：`新账户与原账户不建立任何关联（与枚举速查表「会员身份状态」条一致，DEC-R6-11）`
- 证据类型：1
- 冻结判据：BEQR-009、BEQR-030、BSM-033、BSM-035、GEN-008
- 分类：**冲突**
- 工程缺陷是否成立：**成立**。L781 按手机号终身「没买过」判定；L39/L68/L818 按注销后不穿透历史允许再享新人。同一手机号先购票再注销再注册时，两句命令相反。L818 已声明与枚举表一致，不能把 L781 解释成「仅看当前账户」——「没买过」绑定的是手机号而非账户。
- 铁律 4 暂停：是。

**申请 2（仅申请，未改）：**

- 文件：`docs/business-system-map-for-operations-v0.1.html`
- 行：781
- 拟改：将「同一手机号没买过才算新人」改为与 BEQR-009/BEQR-030 及同文件 L818 一致，例如「按注册手机号判定（需短信验证）；注销后重新注册默认不穿透历史，可再享新人资格，系统自动判。」
- 为什么：消除新人资格互斥
- 批准前不得修改

### GR-03 — 合同变更类型：对照源六类对范围文件八类

- 临时 ID：GR-03
- 文件:行号：对照源 `docs/platform-governance-map-for-operations-v0.1.html:537`；范围文件已一致为八类：`docs/business-enum-quick-reference-v0.1.html:251`、`docs/business-system-map-for-operations-v0.1.html:706`、同文件 L1351
- 原文：
  - PGM L537：`管理合同全生命周期变更：续租、退租、面积变更、租金调整、经营范围变更和提前终止`（无「等」，闭集六类）
  - BEQR L251 / BSM L706 / BSM L1351：八类，另含 `经营范围授权`、`位置变更`
- 证据类型：1
- 冻结判据：BEQR-163、BSM-028、BSM-076、GEN-008（对照源 PGM-029 不在 M001 映射内，仅作权威比对）
- 分类：**冲突**（三份解释权 HTML 之间；M001 两文件彼此不冲突）
- 工程缺陷是否成立：**成立**。治理地图以无「等」的列举给出变更闭集，比枚举表/业务地图少两类。不是「HTML 未提及所以沉默」——它主动给出了较短闭集。
- 铁律 4 暂停：是。缺陷落点在对照源，仍属三份 HTML 自身冲突，本席不改任何 HTML。

**申请 3（仅申请，未改）：**

- 文件：`docs/platform-governance-map-for-operations-v0.1.html`
- 行：537
- 拟改：将变更列举补为与枚举表/业务地图相同的八类，即在「提前终止」之后补入「经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）」
- 为什么：消除三份 HTML 对合同变更闭集的互斥；M001 两文件无需改
- 批准前不得修改


## 当前操作员答复及待裁决（2026-09-05T04:24:33.169442+00:00）

申请15：批准申请 15（推荐）
申请16：批准申请 16（推荐）

申请14原答复：我是可以按照这个提示来，但是有一个问题，如果提单方在问题解决后不主动点击这个。这个问题关闭。怎么办如何解决
处理：这是带问题的有条件表态，未作为最终批准。原删除超时例外提案保留为历史，当前建议补明回访确认后代关，仅拟稿未授权。

申请17原答复：这个完全就不是业务问题啊，为什么在这里提问啊，而且为什么html当中有这个问题啊
处理：用户要求解释技术问题为什么进入业务HTML审批，不等于批准或明确拒绝；不擅自解释为本轮技术豁免、出队或修改提示词。它属于GEN-015界面可访问性，不是新增业务规则；三份HTML文件整体受铁律4保护，故仍需修改授权。

### 申请14修订提案（待确认，仅业务地图1384）

原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
拟替换为：投诉类须取得提单方明确确认后才能关闭。提单方可以自行在线确认，也可以在客户关怀部回访时明确确认问题已解决并同意关闭；通过回访确认的，由客户关怀部保存确认记录后代为关闭；未取得提单方明确确认的不得关闭，继续由客户关怀部跟进；
其他正文保留。理由：保留提单方明确确认门槛，解决已明确认可但没有亲自在线点击的情况；客户关怀部只有保存本人明确确认记录后才代关。未回应者仍继续跟进，不把沉默写成满意或同意；是否希望另设超时审核关闭，须操作员另行明确裁决。

此建议尚未成为业务规则，未修改任何HTML。仍按M001整模块B2整批处理：先解决14/17答复，再Grok复核全部候选（含四项异议），双PASS后才统一B3；不会先修已批准15/16。当前没有新增流程状态、计数器或产物。修订提案在内存中验证原片段唯一命中、替换范围只在1384；未写入业务源码。


## 申请14当前修订稿及同步申请18、19（14、18已批准；19已拒绝）

操作员已选方向：“希望无回应时也允许主管审核关闭，请另拟条文”。这是拟稿指令，不是下面新增具体条文的逐处批准。
申请17已批准，实际弹窗答复：“批准这项技术修改（推荐）”；申请15、16继续已批准。旧14的直接删除稿、回访须本人明确同意后代关稿均不作为待执行稿；以下为当前唯一14候选。

### 申请14（业务地图1384；M001-005）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1384行两处精确替换：
① 原文：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类处理完成后，由客户关怀部通知提单方确认；确认时限自通知提单方之时起算（业务参数，按工单类型可配，须同时配置上下限）。提单方可自行在线确认，也可在回访时明确确认问题已解决并同意关闭；回访确认的，由客户关怀部保存确认记录后关闭。确认时限届满、经回访仍无回应的，须经主管审核处理结果及通知、回访记录，确认问题已解决并批准后，由客户关怀部关闭，单独统计为“未确认关闭”，不得视为提单方确认或满意。提单方明确表示问题未解决或不同意关闭的，不适用上述无回应关闭规则，继续跟进；
② 原文：其关闭同样须提单方确认。
替换为：其关闭同样按上述投诉关闭规则执行。
其余正文保留，尤其复杂类双确认及客户关怀部闭环确认条款不变。理由：落实操作员选择的无回应时主管审核关闭；确认时限继续使用已有按工单类型配置的业务参数，不擅设固定天数。
批准状态：已批准当前修订稿。操作员实际答复：“批准申请 14 修订稿（推荐）”。

### 申请18（枚举262；M001-005同一缺陷同步落点）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
第262行：
原文：投诉类须提单方确认才能关闭
替换为：投诉类由提单方确认后关闭；确认时限届满且经回访仍无回应的，按《业务系统地图》「物业服务」的投诉关闭规则，经主管审核批准后由客户关怀部关闭
其余正文保留。理由：避免枚举仍要求所有投诉必须本人确认而与拟定主管审核关闭规则冲突。
批准状态：已批准。操作员实际答复：“批准申请 18（推荐）”。

### 申请19（业务地图1378；M001-005同一缺陷同步落点）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1378行：
原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（客人端投诉，客人即提单方，关闭按本章投诉关闭规则执行）
其余正文保留。理由：统一客人投诉的关闭规则，消除本章概述的旧绝对门槛。
批准状态：明确拒绝。操作员实际答复：“不批准申请 19”。第1378行不得修改，不把拒绝当成缺省批准。

以上属于同一 M001-005 的业务裁决同步，不新造 finding ID、不增加B3执行次数。内存预演确认四处旧片段各唯一匹配，复杂类原双确认条款保留。治理地图现文只要求客户关怀部确认闭环，没有需同步的提单方绝对确认片段，故未提出治理地图修改。三份HTML均未写入。B2仍BLOCKED-ASK，15/16/17获批不触发先行修复。


## 申请14、18批准及19拒绝的实际答复

UTC 2026-09-05T04:29:19.484919+00:00
申请14：批准申请 14 修订稿（推荐）
申请18：批准申请 18（推荐）
申请19：不批准申请 19

当前批准集合为14当前修订稿、15、16、17、18；19拒绝，不得修改地图1378，不重复申请该处修改。所有批准尚未B3实施。
拒绝19的业务含义尚不明确：原句“客人（客人端投诉，客人即提单方，关闭须其确认）”仍在1384新方案上方；已批准14同时明确客人投诉按无回应时主管审核关闭规则执行。须明确客人是否也适用14，或客人仍必须本人确认而需要收窄14/18的适用范围。本席不把拒绝一个文字修改擅自解释成对任一业务规则的改判，也不凭确认时间线猜选一条。当前仍在同一HTML修改人闸/B2，BLOCKED-ASK等待这一个业务含义答复；没有新增流程闸或状态。


## 客人反馈后由客户关怀部关闭：最新裁决与三处同步拟稿

操作员最新原话：“客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭”。该回复回答了适用范围问题，采用了两选项之外的新明确口径：客人有反馈时，由客户关怀部记录解决方案及反馈后关闭；不要求反馈一定是本人同意关闭。本席不擅自增加该限制。
拟稿同时保留先前已批准14的无回应时主管审核关闭分支；两者按“已有反馈/仍无回应”分开。这个组合将明确展示给操作员核对，不把沉默记成反馈。
原14与18的批准历史保留；最新客人规则改变了它们的具体拟写文字，故下面三份为当前提案，尚待逐处批准。原19拒绝继续有效；新19具体文字也不同，未取得新批准前1378一字不改。15、16、17持续获批。

### 申请14最新稿（地图1384）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
① 原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类处理完成后，由客户关怀部通知提单方；确认时限自通知提单方之时起算（业务参数，按工单类型可配，须同时配置上下限）。客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭。其他投诉的提单方可自行在线确认，也可在回访时明确确认问题已解决并同意关闭；回访确认的，由客户关怀部保存确认记录后关闭。确认时限届满、经回访仍无回应的，须经主管审核处理结果及通知、回访记录，确认问题已解决并批准后，由客户关怀部关闭，单独统计为“未确认关闭”，不得视为提单方确认或满意。已收到提单方反馈的，不适用上述无回应关闭规则；
② 同行原文：其关闭同样须提单方确认。
替换为：其关闭同样按上述投诉关闭规则执行。
其他正文保留。批准状态：等待当前稿逐处批准。

### 申请18最新稿（枚举262）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
原文：投诉类须提单方确认才能关闭
替换为：客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭；其他投诉须提单方确认；确认时限届满且经回访仍无回应的，按《业务系统地图》「物业服务」的投诉关闭规则，经主管审核批准后由客户关怀部关闭
其他正文保留。批准状态：等待当前稿逐处批准。

### 申请19新稿（地图1378；原稿拒绝不变）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭，无回应时按本章投诉关闭规则执行）
其他正文保留。理由：在此处直接显示操作员新定的客户关怀部关闭权限，并指向无回应处置，避免保留“关闭须其确认”旧绝对条件。批准状态：等待新稿逐处批准，不复用旧批准、也不把本次业务裁决当作该行修改许可。

所有修改继续归属M001-005；不创建新findingID。源片段在内存中验证唯一命中，客人原句完整纳入、复杂类双确认未改变，源码未写。B2人闸保持BLOCKED-ASK；本席只做本次业务答复必要的记录及拟稿，不推进外审或修复。


## 当前唯一待审批稿：商户投诉/消费客人投诉两类（替代前述投诉拟稿）

操作员最新申请14答复原文：“这个你看一下html原文。投诉是两种投诉。第一种是商户投诉，第二种是客人投诉商户投诉时需要商户确认投诉解决，如果按规则商户投诉的问题无法解决，或者商户在解决后不点击关闭的，不点击确认投诉解决的。那么客户关怀可以把相关信息，缘由记录这些东西都填进去以后，关闭投诉。那如果是客人投诉就是消费的客人投诉。那么就是客户关怀部这个记录解决方案及客人反馈后。然后关闭”。申请18答复：“批准申请 18 最新稿（推荐）”；申请19答复：“不批准这份稿子”。
最新14明确重定两类投诉关闭条件，优先于此前主管审核/超时确认提案。18的旧稿批准保留为历史，其主管审核分支不得机械写入当前新规则；19仍明确拒绝，源码未改。当前15/16/17已批，下面14/18/19为按用户最新裁决重新拟定的精确同步文字，均待逐处许可。

### 当前原文核实

枚举262把投诉统写为提单方确认；地图1378与1384后句把客人关闭也绑定本人确认；地图1384前段还留有超时默认关闭。地图1322和治理502明确商户投诉/异议与客人投诉同通道、由客户关怀部统一受理；该规定是受理归口，不意味着关闭条件必须相同。治理502关于涉及部门/财务作实体回复、客户关怀部不代作实体判断的既有职责保留。
枚举264/265的商户工单状态链和时效、地图1380回访验收概要、复杂类双闸不新增修改；最新投诉关闭条作为明确的投诉分支适用。没有从实现代码或PRD增补业务规则。

### 申请14当前两类稿（业务地图1384）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
① 第1384行原片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；
② 同行原文：其关闭同样须提单方确认。
替换为：其关闭按上述消费客人投诉规则执行。
其他正文保留。批准状态：等待当前两类稿逐处批准。

### 申请18当前两类稿（枚举262）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
第262行原文：投诉类须提单方确认才能关闭
替换为：投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭
原有后接分号与其他正文保留。批准状态：等待当前两类稿逐处批准。

### 申请19当前两类稿（业务地图1378）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1378行原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）
其他正文保留。本稿只有用户本次明确的消费客人记录方案及反馈后关闭，不带旧的“无回应时”或“主管审核”转引。批准状态：等待当前新稿逐处批准，原稿拒绝继续有效。

本稿不把确认超时、主管审批、未确认关闭统计或“其他投诉”分类加回投诉规则；已有工单响应/处理时限和复杂类验收条款保持原文。三份业务HTML仍未修改，源片段唯一性与最小替换已经内存验证。B2待批，全部问题定稿后才整批B3。


## V0.35 M003首次B2派发前候选精化

两项操作员答复保持不变。024的当前候选分类提议改为细化，依据BSM1725完整明文；仅候选，Grok原判添加及双方完整B1不改，须其B2实际复核接受才能定稿。021直接引用生成器核实整块重生成，因此把拟修位置定在现有README基线块前的解释段，保留auto块、862值与生成器字节；不扩大源修复范围。其余23项分类/24项队列保持，19项是、5项否、未答0。B2两席均须针对本V0.35完整候选表和两份原始B1前缀独立给票。
