CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B4-r4 席甲（Claude）修复内容与归因审计

版本：V0.01

修复内容：PASS。逐行归因：PASS。步骤 B4-r4；M013 IN_PROGRESS，B3 累计 4/4（操作员 2026-09-19 批准追加一次，本次已使用）；本次 Grok B4-r4 报告/CLI 正文未读，不是模块 PASS。先完整封存本票再互读。
上行锁哈希为本席在 160 工作区对 docs/audit-2026-09/plan/criteria-lock.md 用 sha256sum 实算（B3 前后各一次，与 sidecar、ledger 顶行、modules/M013-b5-r3-grok-raw.md 末条四值全等）。

## 输入与全部差异
B2 双 PASS 定稿 findings V2.32（全文件 SHA256 3e35c5b5d73b88f6015d5d491e07fd84e907798661d84b320c3b2a780ae76992）：M013-019（冲突）、020/021/022/023（细化、工程缺陷）修复队列「是」；010 续记否；001–018 否。双方 b1 V0.06 B2 PASS：self SHA256 2e924f5fd5d8ddad31936a6767d9406c7e62a2b5b5d541b8c82f2a113d903d29（72465 bytes）、Grok SHA256 560eed6a1f09c5a710df695d3ee5d08d2a6bf5b588437874c2809eef7fa09bd8（156407 bytes，实际 EXIT_CODE=0）。
before 4fe06d65137e6fefa5e2da4061af7260cee291be
after ddff52dad5669cf17a598b75490df0bcfb454429
两次在临时不存在的 GIT_INDEX_FILE 路径下 git add -A 与 write-tree，包含全部 tracked/staged/untracked。真实 .git/index 未动（mtime 仍 2026-09-14 18:02:05 -0700，546244 bytes）。HEAD 前后均 e456ba96c84185c8d39f18f548011daac48a6466。完整无路径过滤 git diff --binary 两树得到 34121 bytes、SHA256 6b97dbc641237b9711116c6e2de3a298db981b1868efeaff3d66b3d6770a95e0，18 个 hunk，+23/-18，仅四文档（git diff --name-only 恰四条）；git diff --check 退出 0。补丁脚本 /home/james/audit-tmp/m013_b3r4_patch.py 在 apply 前 check 输出 CHECK_OK 22 edits（每处原句在对应文件恰好 1 次），apply 输出 APPLIED 22 edits。
diff 原样副本：/home/james/audit-tmp/m013_b3r4.diff（同 SHA256）。

四文件 after（工作区实算 = after 树 blob）：
| 文件 | 行 | SHA256 | after 树 blob = git hash-object |
|---|---:|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 154 | 9cdec650f49335a42a8769aaea0fbfa0881f15b77994171b0d381538edf001b8 | bad5f736e4a4cf2c6d5657753c965b953568e784 |
| docs/group-governance-rebuild-prd-v1.0.md | 117 | 7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52 | d252bdc30d89bc195f80cb8ed6eda835df60de84 |
| docs/platform-governance-acceptance-pack-v0.2.md | 291 | 56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0 | cb89c73ada4d6e36fbcc257aae10609a407ec1ed |
| docs/platform-governance-prd-v0.1.md | 971 | e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c | 1b21047eb64406f523826ccbf1fdc8025f8c6fa0 |
三 HTML 不变：ENUM 4396bb7112f14216…、BSM eebc5b417f8ce307…、PGM a7fe8c015c61564b…（与 B5-r3 所记一致）。四文件均无 CR 字节。

## 全部内容与归因（18 hunk 逐一）
| # | hunk | 变更行 | ID 与判断 |
|---|---|---|---|
| 1 | 绑定 @@ -1,6 +1,6 | L1 标题 V0.11→V0.12；L3 版本 V0.12、最后更新 2026-09-19 | M013-022/023（本文件本批版本行）；+0.01，标题与页首一致，状态文字不变，PASS |
| 2 | 绑定 @@ -36,7 +36,7 | L39 迁移句末「。」→「；V427 另建 `business_entity_projects` 多对多项目关联表，主体表本身仍无 `project_id` 列。」 | M013-022；与 V427 迁移及「主体表无列」事实一致，PASS |
| 3 | 绑定 @@ -46,14 +46,14 | L49 status 行补「；服务只接受 pending_admission / active / suspended / terminated / blacklisted（归档走存档端点）」 | M013-023；与 PlatformService:54–56 SUBJECT_STATUSES 及 requireSubjectStatus 归档提示一致，PASS |
| 3 | 同 hunk | L55 契约缺口句末补「；同一 schema 的 `status` 枚举仍是 draft / active / suspended / archived，与服务可设五值不一致，同属契约缺口（M013-023，归 OpenAPI 契约模块处理）。」 | M013-023；与 openapi-p0.yaml BusinessEntityRequest.status 一致，OpenAPI 本身未改，PASS |
| 3 | 同 hunk | L56 在「字段。」与「证照、资质」之间插入「项目归属由 V427 `business_entity_projects` 多对多关联承载：创建主体时关联当前项目（持授权 U 盘密钥的最高权限可指定已存在项目），列表按当前项目过滤。」 | M013-022；与 PlatformService:121/419–429 creationProject、:92 列表过滤、:363–381 关联方法一致；「持授权 U 盘密钥的最高权限」为 BSM-004 原话，PASS |
| 4 | 绑定 @@ -113,6 +113,7 | §5 表新增一行 `POST /api/v1/platform/business-entities/{entity_id}/projects`「为主体追加项目关联（须平台范围且由持授权 U 盘密钥的最高权限执行）」 | M013-022；与 PlatformController:97 及 requireProjectConfigurationAuthority（requirePlatformScope + isMasterAuthority）一致，PASS |
| 5 | 绑定 @@ -150,4 +151,4 | §8 依据「V410（删父链、角色三值）」后补「、V427（主体项目关联）」 | M013-022；PASS |
| 6 | 集团 @@ -1,4 +1,4 | L1 V1.02→V1.03 | M013-019/020（本文件本批版本行）；PASS |
| 7 | 集团 @@ -20,7 +20,7 | L23 request_type 六值 + 说明「DB CHECK 六类（V392）；申请入口只接受前三类，user_scope_change、position_high_risk_release 由跨范围定岗与岗位高危挂起在服务内生成，role_permission 服务层未开放」 | M013-020；与 V392 CHECK、AccessService:56–57/1580–1585 一致，PASS |
| 8 | 集团 @@ -67,13 +67,13 | L70 末补「；另有服务内生成的 user_scope_change、position_high_risk_release 两类（V392），不是申请入口可填类型。」 | M013-020；PASS |
| 8 | 同 hunk | L76 「本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。」→「本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。」 | M013-019；只删两项排除，保留手工模板应用与 DEC 引用，句子完整，PASS |
| 9 | 集团 @@ -113,3 +113,5 | 末尾空行 + 变更记录（2026-09-19，V1.03，M013-019/020） | M013-019/020；只追加、不改旧记录，PASS |
| 10 | 验收包 @@ -1,4 +1,4 | L1 V0.05→V0.06 | M013-021（本文件本批版本行）；PASS |
| 11 | 验收包 @@ -178,7 +178,7 | G7 说明栏删「，所有对象继续携带 `project_id`」，其余栏不变 | M013-021；删假完成声明、不新写要求句，PASS |
| 12 | 验收包 @@ -198,6 +198,7 | §11 在 2026-09-14 行后插入 2026-09-19 行 | M013-021；接在最近一行之后、不重排历史行序（O-B5-03 只登记），PASS |
| 13 | PRD @@ -1,4 +1,4 | L1 V0.05→V0.06 | M013-019（本文件本批版本行）；PASS |
| 14 | PRD @@ -404,7 +404,7 | §6.10 第 9 条排除句同 L76 改法 | M013-019；PASS |
| 15 | PRD @@ -520,7 +520,7 | §7.8 验收标准删「仅职级变化不作为本规则触发；」 | M013-019；分号链保持完整（「…直至审批通过；敏感权限变更可追溯」），PASS |
| 16 | PRD @@ -541,7 +541,7 | §7.8.2「本规则不扩展到仅职级变化，也不扩展到任意手工模板应用；」→「本规则不扩展到任意手工模板应用；」 | M013-019；PASS |
| 17 | PRD @@ -653,7 +653,7 | §12 第 14B 条「本条不把仅职级变化或任意手工模板应用列为触发条件」→「本条不把任意手工模板应用列为触发条件」 | M013-019；PASS |
| 18 | PRD @@ -968,3 +968,4 | 变更日志末追加 2026-09-19 行 | M013-019；只追加，PASS |
完整 diff 全部 +23/-18 行均归到 019–023，无无 ID 行、无新文件、无 HTML/代码/OpenAPI/迁移/锁/台账改动、无业务句越界改变。18 hunk 与定稿候选 22 处逐字对应（after 文件中 22 处 new 各恰好 1 次；old 仅在 4 处「old 是 new 子串」的追加型改动中命中 1 次，其余 0 次）。

## 被删词残留核查（after 文件）
「仅职级变化」「责任范围变化」「继续携带」「DB CHECK 预留 4 类」「尚无 DB CHECK |」在四文件内仅剩 5 处：集团 PRD L5（2026-07-27 修订说明④历史记录，且方向为「…责任范围变化后立即重算」）、PRD L966（2026-08-10 变更日志历史）、验收包 L201 与 PRD L971（本次新增变更记录行引述被删词）。均为历史/日志记录，按 B2 定稿「历史记录不改」保留；现行规则行零残留。

## 三线
第一性原理：019 删除后四处现行规则与 PGM L658「定完系统自动适配」、L669「职级影响默认权限模板」及 AccessService:1305–1313 改职级重算同向；「任意手工模板应用不触发」仍在，DEC-R8-91 第 6 条免审射程未扩。020/021/022/023 只把已发布迁移（V392、V427）、现行代码入口与契约裂口写成当前事实，不新造规则、不宣称缺口闭合（021 是删假完成声明，022 保留「主体表本身无 project_id 列」）。
顺从式：GEN-016 版本可验证（四文件 +0.01，绑定标题与页首同为 V0.12、日期 2026-09-19 为当前当地日）；GEN-002/014 入口与迁移声称与当前源一致；GEN-017 四分类按定稿；不增判据、不 LOCK-AMEND。
对抗式：未把职级写入正向触发清单；未把 021 改写成新要求句；未在本模块改 OpenAPI 或代码；未借本批改 001–018 已修句（前后树 diff 只含 22 处）；变更记录只追加不改旧行；before/after 全 diff 仅四文档，未掩藏其他改动；真实索引、HEAD、三 HTML 指纹未变。

## 实际验证
SSH 在 160 工作区：sha256sum 四文件 + git rev-parse after:path 与 git hash-object 逐一相等；python 对定稿 22 处 new/old 逐条 count；被删词字面残留定位；L1/L3 版本行提取；CR 字节计数 0；git diff --check 退出 0；git diff --name-only 仅四条；.git/index mtime、HEAD、三 HTML sha256 与 B3 前一致。不是 Java/HTTP/数据库/浏览器测试，未运行这些测试；纯文档修改按文档类豁免新增测试。
