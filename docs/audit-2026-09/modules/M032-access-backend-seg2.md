# M032 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。25 个文件、52 判据；B1 双 FAIL（Grok GR-01…03／席甲 CX-M032-001…004）→ B2-r1 双 PASS 定稿 M032-001（细化）、002（技术性），只在主工作区未发布版成立的 4 项登记为 W-1…W-4 → B3-r1（席甲：发布口径一次性 worktree 里 BASE 全绿 → RED 12 条 → GREEN 六类全绿 13/25/54/21/12/39）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 bbace2c818a3cbd61a6191a09f9382bbb3372d14273956c44193c2076816d698；席甲 V0.01 SHA256 f36aabbc318477d806613147d9ba23da862a1df3a5930d3f057f92c5ebac7270；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 库内授权政策行自 V389 起多一个键 `deadlineWorkingDays`，记录类不认得它，`new ObjectMapper()` 遇未知字段即抛，凡经 `grantPolicy()` 的路径（授权政策读取、授权申请列表与统计、含须审批权限的授权申请、岗位模板含高危码时的定岗与范围变更）在跑过 V389 的库上全部 409；保存还会把该键整份覆盖掉。现记录类增可空第 5 组件、为空不输出，保存改为 jsonb 合并写；补真库回归。002 `EffectivePermissions` 类注释改为现状（wildcard 恒 false）。

发布口径（ledger D-029）：主工作区 JdbcAccessRepository.java 与 AuthorizationService.java 带 2026-09-13 起未发布的跨模块工作流改动（提交 52074492、e9b88faf、49aaaa94 加已暂存改动），其中 AuthorizationService 依赖未发布的 `SessionIssuer.MASTER_ACCOUNT_TYPE`，不能随本模块单独发布。本模块检查点带 origin/main 版＋本模块补丁（JdbcAccessRepository.java 用发布变体）。

登记到该未发布工作流（发布前须处理）：
- W-1（＝XM-M031-A）：Jdbc 三个带参列表对空范围改为拒绝，而已发布的接口契约与 AccessService 调用点仍是「null＝平台管理员全量」，平台管理员打开权限模板/角色/岗位映射列表即 403。取哪边取决于「七类红线第四条是否约束平台管理员」的业务口径，须操作员定；无论取哪边，发布前必须消掉这条 403。
- W-2：`currentProjectScopeQuietly()` 现在会抛，多个服务的 `.orElse(null)` 调用面按「不抛」写成。
- W-3：单次临时授权改按 `grant_group_id` 整组消耗后，`M006JdbcRegressionTests` 临时表夹具缺该列，主工作区该类 1 条报错。
- W-4：主工作区版不能在 origin/main 上编译。
- 另：`currentMerchantScope` 对无商户的管理员与最高权限一律抛、「逐笔开放」未实现；`pageGrantRequests`/`listGrantRequests` 对 null 仍全量，与 W-1 的收紧不一致。

其余登记：`withLifecycle` 与生命周期 SQL 恒写 inactive（M031-001 已在调用侧解决）；`findTemporaryGrant` 对非 UUID id 抛 IllegalArgumentException；origin/main 版 `currentSupervisorLevel` 不封顶（消费方另有最高权限闸）；`listUserIdsAssignedToPosition` Jdbc 未覆盖；`updateGrantPolicy` 返回体不含客户端没带的可选键。
