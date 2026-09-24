# XM-D029 1aa4c9c2 带出的未审后台改动按 D-032 审后保留（合成模块，D-032）
版本：V0.01

2026-09-23 收口（操作员 2026-09-23「解决这些」；D-032 两问答复原样：「不约束平台管理员」「立合成模块审计后保留（推荐）」）。来源：Codex 线 M009/M011 发布提交 1aa4c9c2 把主工作区全部后台生产 Java 一并发布，带出 27 个未经审计的代码文件改动（D-029 所列红线④工作流等）。范围：这 27 个文件在 1aa4c9c2 的改动（冻结 /home/james/audit-tmp/xmd029_scope.diff，128489 bytes SHA256 bd228b3491a84d9405a85a08c0282844a419ae86762af77f2b5bb9e92bcde7bf）；判据取锁内 M015、M029、M032、M036、M037、M040、M042、M044、M046、M053、M155、M249、M251 映射并集 229 项，只读引用。

流程：B1 双 FAIL（Grok GR-01…04／席甲 CX-01…10，席甲 CX-05 判断有误、漏 GR-02/GR-04，均以 Grok 为准）→ B2-r1 双 PASS 定稿 XM-D029-001…008 → B2-r2 双 PASS（003 改丁案）→ B3-r1（14 文件 336 增 100 删，冻结 /home/james/audit-tmp/xmd029_b3r1.diff SHA256 1fa24e3a4dcd8e8dc2cc700d4c7801f1c18a92382e8b29e925c5e6cc19ed8eb4；origin/main 一次性 worktree＋一次性空库：RED 11 条新增或改动测试新红 → GREEN 3532 测 23 红，全为 XM-M036-A 既有红）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 32cd4c7d1b0e61562f0dbee83c2e091c0e6d105cf6397b304603e780fda18067；席甲 V0.01 SHA256 5eda6bff1e71c6a47075106b49c831a31c6e4efb34a4afcf0e66f644b2d6b40e；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458。B3 累计 1。Grok 票均为 grok-4.7/xhigh。

修复摘要（依 findings 定稿处置）：
- 001 平台管理员恢复全量视角：AuthorizationService 三个范围方法对平台管理员返回空范围；JdbcAccessRepository 空范围恢复全量；审批待办与单据可见对平台管理员放开；PlatformService 平台管理员走不带项目参数的列表重载，单对象只校验存在，新建须显式项目；普通岗位与商户仍按本项目、本商户隔离。
- 002 最高权限待审列表不再整表，按编号逐笔打开跨项目单据并留痕（governance.supervisor_review.viewed_cross_scope）。
- 003 直接受益人强制登记只保留授权类事项（丁案）；免单等动作不再因缺受益人被拒，「受益人不得审批、不得被转发」的比较对任何动作保留。
- 004 单次临时授权整组消耗时逐码写使用记录。
- 005 只有分红强制升到最高权限档，资金类授出按政策档位走审批链。
- 006 网页会话刷新不再强制设备号；绑定了设备的会话缺号或错号仍拒；主会话不可续期保留。
- 007 WecomRiskHitsEndpointReproTests 按既有方式注入测试公钥；生产缺公钥拒启动保留。
- 008 恢复 M037-002 的「不落写入」校验。

失效范围：PlatformService.java（M009）与 PlatformServiceTests.java（M011）被改，两模块由 PASS 改为 PENDING；其余 12 个文件所属模块均已 PENDING。

跨模块观察（cross_module_observations）：
- XM-D029-003 丁案（→酒吧 M193、财务 M332、票务 M133 与身份模块）：退款收款人、往来方等客人或外部受益人与员工账号之间没有身份关联，「收款人不得审批、不得被转发」对这类受益人无从执行；须先建身份关联再补。
- XM-M036-A：origin/main 后端全量的既有 23 条红（旧 20 条、FieldLabelsTests 1 条、StaffProfileArchiveDeleteJdbcIntegrationTests 2 条），以及 JdbcPlatformRepository.saveSpaceTypeDeleteRequest 以类型号加毫秒时间戳作主键、同一毫秒两次申请撞键（PlatformJdbcIntegrationTests.completingSpaceTypeRequestKeepsPriorRequestHistory 时红时绿）。
- 其余只登记：OBS-1 换公钥作废主会话；OBS-2 动作名子串匹配；OBS-3 普通员工同项目同商户过滤；OBS-4 储值夹具；M010 回归用例演员。
