# XM-M034-AB 票务人工放行写入口与游乐人工改判下线（合成模块，D-031、D-033）
版本：V0.01

2026-09-24T09:51:06Z 收口（操作员 2026-09-23「解决这些」；ledger D-031 按 D-027 先例立项，XM-M034-A 与 XM-M034-B 同对枚举速查第 43 行合并审；操作员 D-033 对添加项先答「保留」、后在聊天改答「删除」，以最新输入为准）。范围：开工段 33 个文件（全仓对 manual-override、manualOverride、manual_override、verification.override 的引用文件）＋定稿新增契约包、需求实现矩阵、e2e 模拟、新迁移 V433 与门禁生成物；判据取锁内 M039、M114、M132–M135、M230、M232、M233、M237、M245、M246、M248、M389–M392 映射并集 181 项，只读引用。

流程：B1 双 FAIL（Grok GR-01…04／席甲 CX-01…04）→ 添加项弹窗与 D-033 → B2-r1 按「删除」重写后双 PASS 定稿 001…004（按「保留」写的候选与派出的 Grok 复核作废终止）→ B3-r1（30 个文件）→ B4-r1 Grok 归因否决（生产就绪证据 JSON 多改 verified_at、notes、version 三行）→ B3-r2 收窄为只改接口数 → B4-r2 双 PASS → B5-r1 Grok 否决（GR-01：需求实现矩阵仍指向已删类文件）→ B2-r2 定稿 005 → B3-r3 → B4-r3 双 PASS → B5-r2 双 PASS：Grok V0.01 SHA256 2fc0a6fa9e148666e69e0396d66598e251851277c7c1316ab4332171cb9f6be1；席甲 V0.01 SHA256 20e23de2d2b1837133e35afc15db3fc6cb3d30fd92ac8ddad80762a977064667；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458。B3 累计 3。Grok 票均为 grok-4.7/xhigh。累计 diff /home/james/audit-tmp/xm034_b3cum_r3.diff（SHA256 a8554489151f00ffeb3b518a46c0fc3a06ce88436e530a5db6dafe012da34aa0，30 个路径）。

修复摘要：
- 001 票务：删 POST /api/v1/ticketing/verifications/manual-override、TicketingService.manualOverrideVerification、TicketVerificationService.manualOverride 与 DTO（枚举速查第 43 行「系统不提供人工放行」；权限码已由 V429 停用）。V067 的 manual_overridden 状态值与历史行、结算排除保留。
- 002–004 游乐：删 GET/POST /api/v1/amusement/manual-overrides、服务与仓储方法、行映射、两个记录类型；allow（人工放行）、release（清零在玩人数，BSM 第 1263 行）与无 HTML 依据的 deny/pause/resume（D-033 删除）一并下线；V433 照 V429 停用 amusement.verification.override 并清四处授予。amusement_manual_overrides 表与历史行、V014/V234 类型约束、V099 准入策略行保留；出场自动扣减 releaseEntryConcurrentCapacity 保留。
- 契约与生成物：两份 OpenAPI 删路径与组件（732→730）；契约附录两份、安全基线与发现登记、数据隔离规格（V432→V433）由生成器在 origin/main＋补丁上重建；生产就绪证据只改接口数；权限矩阵四行、契约包一行、需求实现矩阵（接口引用 8 处与已删类路径 4 处）、e2e 模拟一行同步。
- 后端全量 263 套 3529 测 0 红，全链 verify:predeploy 通过。

跨模块观察（cross_module_observations）：
- →M230（游乐）：docs/scope-wide-coding-rule-canonical-V0.01.md:259 把「扫码出场核销」列为已废止设计，并把 AmusementService.exitVerification 登记为须下线实现；本批两席定出场扣减保留，须在 M230 串行审计时对照 BSM 第 1262 行「出口设置闸机或摄像头感应器……系统自动」判定扫码出场是否合规。
- →M230：项目状态常量 PROJECT_STATUSES、场次状态常量 SESSION_STATUSES 与枚举速查 BEQR-085/086 不完全同形（未追完）；权限码中文名「强制改判游乐核验结果」随停用不改名。
- →M390：docs/p0-task-api-permission-event-acceptance-map-v0.1.md:50 的 P0 游乐任务权限清单仍列已停用的 amusement.verification.override（历史验收映射，不影响门禁）。
- →M460：JdbcAdmissionFactRepository:135 注释提及 amusement_manual_override，已成过时注释。
- 历史证据不改：security-scans 下 semgrep 扫描快照与 docs/production-evidence-closure-gap-report-v0.1.md:45 列出的已删文件路径；设计文档里已写「已废止」的旧句。
- 经验：无生成器的门禁证据文件只改门禁比较的字段；删除文件的修复收尾前全仓 grep 被删文件名（含文档索引与矩阵）。
