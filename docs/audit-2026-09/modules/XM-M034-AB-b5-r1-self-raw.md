CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票；本模块 B4-r1 本席 PASS 被 Grok 归因否决，已在 B3-r2 收窄）。2026-09-24T08:42:42Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。范围文件的主工作区现状＝origin/main daf6adfc＋累计 diff /home/james/audit-tmp/xm034_b3cum.diff（30 个路径）。
- 范围：开工段 33 个文件的现状（4 个已删除文件核确已删除且无残留调用）＋定稿新增 4 项＋门禁生成物与证据文件；判据 17 个模块映射并集 181 项。B3 累计 2。
- 顺序：本席取证先跑完，随后落本报告；落盘前未读 Grok 正文。未改任何已跟踪文件；一次性 worktree 与一次性空库已删。

## 1. 文件覆盖（30 个改动路径，现状）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/amusement/AmusementController.java | 538 | 29086 | c109d1cc28ed0b6c42fe79ec408ee1f0472dc8ccd289d4da1122c40ac3ebcec7 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java | 322 | 17774 | d875cddd69d5944c0a80a1204ee9cfdba83a5afa654501c4ebc1bf921582afa3 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/amusement/AmusementService.java | 1878 | 97945 | cbe5672f39070eddcdcbb33bf56f7e7f33d79e656b6a13d08cdf5338f487c441 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java | 2001 | 107781 | 7ae729ad7d96864668c4a3eee3b5081cbed4244841dbb2ade6dbc7c5e7ecb047 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRowMapper.java | 331 | 13823 | f88dcfe5490de1a0d457ddc82e388c611356b8a2eba7c3a4971709723cd58073 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java | 1219 | 77523 | 800390c2b250d6e2e0049364d268f15821304ad67d1e6b681f79989253509361 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/ticketing/TicketingController.java | 530 | 29622 | abed1465575736ab51b8080d31e2751610d8d263908b6837f7591135946e7882 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/ticketing/TicketingService.java | 971 | 50647 | d12513b954b4611441cd22a4bb01306b5173c8f0d163aea12db35edfdfaf9bb7 | 改动处及上下文全读 |
| backend/src/main/resources/db/migration/V433__retire_amusement_verification_override.sql | 16 | 1258 | 02dd443c64d450bf127f3aaad6a6a18c293dbfa093ab16b9a66dd2975f3531e1 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java | 463 | 30590 | 745b176b1d6883f2c11fe3ce7826d601d6735208bcc1baf937d2dcf6e52519a5 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java | 523 | 28110 | 33767c27217643df3f86f6f41c61a7ffa71d43318b6832dd79a870411f17d1e6 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java | 3344 | 176430 | 569aa65499d368ae1346d29e885d67f50d8c515b867654c6f9dc5d8363c3f889 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/ticketing/AmusementExitReleaseJdbcIntegrationTests.java | 317 | 18112 | bb98b627f2bc7a635d7d4a9e513da45c3b69190c82b36ca07ac2e44bcdc2062b | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java | 651 | 31796 | 730681392bbec0ac452aeb46ece6d830de9a17cebf491d2dcc8425cc3d9243b5 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java | 4829 | 277404 | 67d83aadc0ca57764f648c3db78a977ec92264eab1ab2e7a509ecccfe3bc2de1 | 改动处及上下文全读 |
| docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 374 | 42310 | 89b6f5d4bc708b75b30abcb86f27f4d459de00a5da1fdd4f39d08adebea3dabc | 改动处及上下文全读 |
| docs/openapi/openapi-p0.json | 51282 | 1697769 | d6a2085004caaa10b33f5057c7fa973da19b1064a30579562a3f7df64bbb9e2a | 改动处及上下文全读 |
| docs/openapi/openapi-p0.yaml | 37303 | 1322583 | 5ea4ef81f146cb0e89bb552769636a59def7de31d186ad459cfa67774951f3c3 | 改动处及上下文全读 |
| docs/p0-api-contract-pack-v0.1.md | 539 | 37574 | da5b293bb4357a80dcfbe7a133ed76ed1154bb3985cc4a4ad2516d354f65e4e6 | 改动处及上下文全读 |
| docs/p0-openapi-contract-closure-appendix-v0.1.md | 2610 | 565032 | b0aeb41bbd9e991ee5a57dfcb6e95119a832da11ffb926eefe963da16acb4375 | 改动处及上下文全读 |
| docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 812 | 223391 | 6ce0fefbd0dddc71729426d45f985d6fced92ada50b842117c9d1301910c3c92 | 改动处及上下文全读 |
| docs/p0-operation-permission-matrix-v0.1.md | 301 | 40990 | 9dade6fdaabe23f6b2ad1ce8a8196b6baee66b14122d989d80ba454a4dc2cefd | 改动处及上下文全读 |
| docs/production-readiness-evidence/security-baseline-latest.json | 14 | 943 | 87283eec5afe99c0b2bdb6c87f155a1cca3562022c4052a1d8a1ecf8fa4fac7e | 改动处及上下文全读 |
| docs/requirements-code-implementation-matrix-v0.1.csv | 98 | 185262 | 7c424f70841a7c77bbf7285c88392b03165395f6c8507c06f09b8883820762da | 改动处及上下文全读 |
| security-scans/findings-register-v1.md | 53 | 7803 | 45581ed2457be671ccc5c634aa153e9063210244a5a05554b0b5f27a4ba79f28 | 改动处及上下文全读 |
| security-scans/formal-security-baseline-v1.md | 162 | 15318 | a91b7b1e3c2f1f0c76ad884566fa4c2243369053b6f99c182268313e343fb0de | 改动处及上下文全读 |
| tests/e2e/support/mock-api.ts | 1045 | 92472 | 4d500f3ea95aa069dbb8067883ce391d9e21658388d819f69413e67728dbec3a | 改动处及上下文全读 |

已删除文件（现状不存在）：AmusementManualOverrideRecord.java、AmusementManualOverrideRequest.java、ticketing/dto/VerificationManualOverrideRequest.java。开工段其余未改文件（V014、V067、V099、V170、V234、V251、V254、V270、V429、JdbcAdmissionFactRepository 等）按 origin/main 读，确认未改。

## 2. 可执行证据（/home/james/audit-tmp/xm034_b5_run.log）

```
START=2026-09-24T08:40:19Z HEAD=e456ba96 origin/main=daf6adfc
scratch at daf6adfc
冻结 diff 已应用
与主工作区逐文件一致：yes（30 个文件）
scratch db xm034b5_test created
MVN_EXIT=0
suites=263 tests=3529 red=0
全部红（预期 0）：
scratch db dropped
verify:predeploy=0
PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
scratch dirty after cleanup: 0
scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T08:41:15Z
```

残留检索（主工作区现状）：

```
git grep（主工作区，排除审计文档与历史目录）manual-overrides / ManualOverride / manualOverride( / amusement.verification.override / clearProjectConcurrentCapacity：
backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql:185:    ('amusement.verification.override', 'Override amuse
backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql:213:        'amusement.verification.override',
backend/src/main/resources/db/migration/V170__access_grant_requests.sql:180:        ('amusement.verification.override', 'high'),
backend/src/main/resources/db/migration/V251__permission_names_i18n_zh.sql:67:UPDATE permissions SET permission_name = '覆盖游乐核验结果' WHER
backend/src/main/resources/db/migration/V254__permission_names_p3_polish.sql:17:UPDATE permissions SET permission_name = '强制改判游乐核验结�
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:55:                "amusement.verification.override",
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:479:                "SELECT status FROM permissions WHERE perm
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:482:                SELECT (SELECT count(*) FROM role_permissi
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:483:                     + (SELECT count(*) FROM permission_te
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:484:                     + (SELECT count(*) FROM user_permissi
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java:486:                         WHERE permission_code = 'amusemen
security-scans/semgrep-20260623-180915-manual-request/semgrep.json:1:{"version":"1.166.0","results":[{"check_id":"javascript.lang.security.audit.detec
security-scans/semgrep-20260703-delivery-gates-clean/semgrep.json:1:{"version":"1.166.0","results":[],"errors":[],"paths":{"scanned":[".env.example","
security-scans/semgrep-latest-summary.json:274:    "backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java",
security-scans/semgrep-latest-summary.json:275:    "backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java",
```

## 3. 三线审计

- 第一性原理：系统不应再提供「人工放行」（枚举速查第 43 行）与「人工清零在玩人数」（BSM 第 1263 行），操作员 D-033 另把无依据的 deny/pause/resume 记录一并删除。实际：票务写入口链、游乐改判两端点与服务、仓储、行映射、记录类型全部删除；两个权限码 retired 且四处授予为 0；契约、附录、安全基线、隔离规格、证据、文档同步；历史数据与已应用迁移保留；出场自动扣减保留。未验证前提：生产库里的历史改判行数量（不查生产，处置不改历史行）。
- 顺从式：181 项逐项见 §4。
- 对抗式（grilling）：问：删掉接口后前端会不会 404？答：apps 与客户端无调用，客户端覆盖校验通过；e2e 模拟行已删。问：最高权限能否绕过停用码？答：不能，knownPermission 先判字典 active。问：会不会把合法的出场扣减一起删了？答：releaseEntryConcurrentCapacity 与出场路径逐处未动，出场测试三条保留且绿。问：历史记录还能查吗？答：只能在库里查，不再有接口；这是 D-033「删除」的直接后果，已写明。问：V099 的准入策略行会不会造成放行？答：该动作已无调用方，策略行惰性，不产生放行。问：新迁移会不会与主工作区未发布迁移撞号？答：主工作区与各远端分支均无 V433。问：有无新引入问题？答：未见。
- 无新缺陷、无添加项、无 HTML 问题。

## 4. 181 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-001 票种 | business-enum-quick-reference-v0.1.html:29 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「票种」所述对象｜通过 |
| BEQR-002 票务策略状态 | business-enum-quick-reference-v0.1.html:30 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「票务策略状态」所述对象｜通过 |
| BEQR-003 定价、售票与收款主体 | business-enum-quick-reference-v0.1.html:32 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「定价、售票与收款主体」所述对象｜通过 |
| BEQR-004 年龄限制 | business-enum-quick-reference-v0.1.html:33 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「年龄限制」所述对象｜通过 |
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-006 通票类型 | business-enum-quick-reference-v0.1.html:35 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「通票类型」所述对象｜通过 |
| BEQR-007 权益类型 | business-enum-quick-reference-v0.1.html:36 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「权益类型」所述对象｜通过 |
| BEQR-008 餐饮/零售券两种用法 | business-enum-quick-reference-v0.1.html:37 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「餐饮/零售券两种用法」所述对象｜通过 |
| BEQR-009 资格判定 | business-enum-quick-reference-v0.1.html:39 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「资格判定」所述对象｜通过 |
| BEQR-010 订单状态 | business-enum-quick-reference-v0.1.html:40 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「订单状态」所述对象｜通过 |
| BEQR-011 票/权益状态 | business-enum-quick-reference-v0.1.html:41 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「票/权益状态」所述对象｜通过 |
| BEQR-012 核销记录状态 | business-enum-quick-reference-v0.1.html:43 | 适用｜「系统不提供人工放行……任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」：票务写入口链与游乐 allow 均已删除，权限码停用｜通过 |
| BEQR-013 手环入场 | business-enum-quick-reference-v0.1.html:44 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「手环入场」所述对象｜通过 |
| BEQR-014 退款单状态 | business-enum-quick-reference-v0.1.html:45 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「退款单状态」所述对象｜通过 |
| BEQR-015 有效期类型 | business-enum-quick-reference-v0.1.html:46 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「有效期类型」所述对象｜通过 |
| BEQR-016 售票与退款原则 | business-enum-quick-reference-v0.1.html:47 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「售票与退款原则」所述对象｜通过 |
| BEQR-017 到期未核销款 | business-enum-quick-reference-v0.1.html:48 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「到期未核销款」所述对象｜通过 |
| BEQR-018 域归属对照 | business-enum-quick-reference-v0.1.html:49 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「域归属对照」所述对象｜通过 |
| BEQR-019 跨域通票分配 | business-enum-quick-reference-v0.1.html:51 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「跨域通票分配」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-021 收费与免费权益结算 | business-enum-quick-reference-v0.1.html:53 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「收费与免费权益结算」所述对象｜通过 |
| BEQR-022 项目内部结算价 | business-enum-quick-reference-v0.1.html:54 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「项目内部结算价」所述对象｜通过 |
| BEQR-023 核销顺序 | business-enum-quick-reference-v0.1.html:55 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「核销顺序」所述对象｜通过 |
| BEQR-024 游乐核销归属 | business-enum-quick-reference-v0.1.html:56 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「游乐核销归属」所述对象｜通过 |
| BEQR-025 门票综合服务费 | business-enum-quick-reference-v0.1.html:57 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「门票综合服务费」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-027 人脸验龄数据生命周期 | business-enum-quick-reference-v0.1.html:60 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「人脸验龄数据生命周期」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「等级权益对照」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「票务人次计算」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作：人工放行与改判不再作为可审批放行的动作存在｜通过 |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-085 项目开放状态 | business-enum-quick-reference-v0.1.html:140 | 适用｜项目开放状态由项目状态与检修确认路径承担；删除的 pause/resume 只是无作用记录，不影响开放状态｜通过 |
| BEQR-086 场次状态 | business-enum-quick-reference-v0.1.html:142 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「场次状态」所述对象｜通过 |
| BEQR-087 游乐「当天／每天」口径 | business-enum-quick-reference-v0.1.html:144 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「游乐「当天／每天」口径」所述对象｜通过 |
| BEQR-088 容量双阈值 | business-enum-quick-reference-v0.1.html:145 | 适用｜容量按实时在玩人数，计数不再可被人工清零｜通过 |
| BEQR-089 园区容量 | business-enum-quick-reference-v0.1.html:146 | 适用｜同 BEQR-088｜通过 |
| BEQR-090 设备关键/辅助分级 | business-enum-quick-reference-v0.1.html:147 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「设备关键/辅助分级」所述对象｜通过 |
| BEQR-091 全园故障与补偿通票 | business-enum-quick-reference-v0.1.html:148 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「全园故障与补偿通票」所述对象｜通过 |
| BEQR-092 安全严重度 | business-enum-quick-reference-v0.1.html:149 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「安全严重度」所述对象｜通过 |
| BEQR-093 巡检任务状态 | business-enum-quick-reference-v0.1.html:150 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「巡检任务状态」所述对象｜通过 |
| BEQR-094 越界类型 | business-enum-quick-reference-v0.1.html:151 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「越界类型」所述对象｜通过 |
| BEQR-095 团体/包场 | business-enum-quick-reference-v0.1.html:153 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「团体/包场」所述对象｜通过 |
| BEQR-096 游乐增强阶段 | business-enum-quick-reference-v0.1.html:154 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「游乐增强阶段」所述对象｜通过 |
| BEQR-097 供应合同模式 | business-enum-quick-reference-v0.1.html:159 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「供应合同模式」所述对象｜通过 |
| BEQR-124 综合通票定价 | business-enum-quick-reference-v0.1.html:193 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「综合通票定价」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「经营主体关系」所述对象｜通过 |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「职级」所述对象｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 适用｜终端能力 amusement.verification.override 随入口下线不再被引用｜通过 |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 适用｜最终权限计算：retired 码不进入任何主体的有效权限｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「项目与敏感数据权限」所述对象｜通过 |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 无本面｜开关优先级未改｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 适用｜权限状态：两码为 retired，字典行保留｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「合同变更类型」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「合同审批链」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「管理费方式」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 无本面｜删除审批流程未触及（本批删的是入口代码，不是业务数据删除）｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「平台管理员拥有全部日常功能权限」所述对象｜通过 |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「基础管理」所述对象｜通过 |
| BSM-010 票务权益 | business-system-map-for-operations-v0.1.html:601 | 适用｜票务权益核销只保留成功、拒绝、失败、冲正等路径，无人工放行｜通过 |
| BSM-016 游乐管理 | business-system-map-for-operations-v0.1.html:631 | 适用｜游乐管理：无人工改判入口｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-030 票务权益：管卖什么票、给什么权益、怎么核销 | business-system-map-for-operations-v0.1.html:727 | 适用｜同 BSM-010｜通过 |
| BSM-031 主要功能 | business-system-map-for-operations-v0.1.html:731 | 适用｜同 BSM-010｜通过 |
| BSM-032 和谁有关 | business-system-map-for-operations-v0.1.html:760 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-033 票种、通票与权益类型（系统支持哪些种类） | business-system-map-for-operations-v0.1.html:772 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「票种、通票与权益类型（系统支持哪些种类）」所述对象｜通过 |
| BSM-034 核销、退款与有效期规则 | business-system-map-for-operations-v0.1.html:796 | 适用｜核销、退款与有效期规则：核销冲正路径保留，人工放行已删｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「会员与积分规则」所述对象｜通过 |
| BSM-036 人脸验龄数据使用与删除 | business-system-map-for-operations-v0.1.html:840 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「人脸验龄数据使用与删除」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-048 触摸屏一体机：权益领取、现场服务和辅助售票 | business-system-map-for-operations-v0.1.html:978 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「触摸屏一体机：权益领取、现场服务和辅助售票」所述对象｜通过 |
| BSM-049 主要功能 | business-system-map-for-operations-v0.1.html:982 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| BSM-050 和谁有关 | business-system-map-for-operations-v0.1.html:996 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-068 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | business-system-map-for-operations-v0.1.html:1253 | 适用｜第 1262–1263 行：出场自动扣减保留（releaseEntryConcurrentCapacity 四处未动），在玩人数不再可人工清零｜通过 |
| BSM-069 主要功能 | business-system-map-for-operations-v0.1.html:1257 | 适用｜同 BSM-068｜通过 |
| BSM-070 和谁有关 | business-system-map-for-operations-v0.1.html:1276 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-071 游乐类型与规则 | business-system-map-for-operations-v0.1.html:1287 | 无本面｜游乐类型与规则未改｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-075 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | business-system-map-for-operations-v0.1.html:1339 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「招商租赁与合同履约：从找商户到签约、履约、变更、退租」所述对象｜通过 |
| BSM-076 主要功能 | business-system-map-for-operations-v0.1.html:1343 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| BSM-077 和谁有关 | business-system-map-for-operations-v0.1.html:1357 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」所述对象｜通过 |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜各入口：去掉两个不应存在的入口，其余入口权限未变｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 适用｜已确认规则：人工放行废止口径落到代码｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜只下线已废止与操作员裁决删除的能力；历史表、历史行、已应用迁移与 CHECK 保留｜通过 |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main＋累计 diff 后端全量 263 套 3529 测 0 红；全链 verify:predeploy 0（§2）｜通过 |
| GEN-003 测试有效性 | 通用 | 适用｜删去的断言只守护已废止能力；新增停用回归在旧迁移下红（B3 RED）｜通过 |
| GEN-004 信任边界校验 | 通用 | 无本面｜未新增输入面，入口只减不增｜通过 |
| GEN-005 授权与最小权限 | 通用 | 适用｜amusement.verification.override 与 verification.manual_override 均 retired 且四处授予为 0，knownPermission 对含最高权限在内的所有主体拒绝｜通过 |
| GEN-006 数据完整性 | 通用 | 适用｜在玩人数只剩入场核销与出场自动扣减两条写路径（release 清零已删）｜通过 |
| GEN-007 并发与幂等 | 通用 | 无本面｜不涉并发与幂等变更｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜两份 OpenAPI 深相等、附录与安全基线由生成器重建、需求矩阵与契约包同步、e2e 模拟去掉该路径；客户端路由覆盖校验通过｜通过 |
| GEN-009 错误处理与恢复 | 通用 | 无本面｜无错误处理变更｜通过 |
| GEN-010 安全与敏感信息 | 通用 | 无本面｜不涉敏感信息｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜历史 manual_overridden 核销记录与 amusement_manual_overrides 行保留、不删不改；结算排除子句保留｜通过 |
| GEN-012 资源与性能边界 | 通用 | 无本面｜无性能面变化｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 无本面｜无依赖变化｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 适用｜V433 只含对 permissions/role_permissions/permission_template_items/user_permission_overrides/temporary_permission_grants 的停用与清授予，与 V429 同形；不改已应用迁移；数据隔离规格戳 V433 同步｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜前端无调用｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜权限矩阵四行、契约包一行改为已下线；证据 JSON 只改门禁比较的接口数｜通过 |
| GEN-017 业务来源分类 | 通用 | 适用｜001–003 冲突、004 添加经操作员 D-033 删除｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码、全量与校验输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」所述对象｜通过 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「治理顺序：一件业务能不能做，要先经过这些判断」所述对象｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「商户和主体」所述对象｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工和手机」所述对象｜通过 |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 适用｜权限和职级：两码停用｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 无本面｜终端开关逻辑未改｜通过 |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「设备管理」所述对象｜通过 |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜留痕复核：历史改判与人工放行记录仍可在库中追溯｜通过 |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「应急与隐私」所述对象｜通过 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「业务边界」所述对象｜通过 |
| PGM-028 合同和准入：违反规则先卡住，再按层级放行 | platform-governance-map-for-operations-v0.1.html:518 | 适用｜准入先卡后放：V099 的 amusement.manual_override 策略行已无调用方、惰性保留，不形成放行｜通过 |
| PGM-029 主要功能 | platform-governance-map-for-operations-v0.1.html:522 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| PGM-030 业务意义 | platform-governance-map-for-operations-v0.1.html:544 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「业务意义」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 适用｜单一功能授权：停用码不再可授｜通过 |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 适用｜权限规则：停用码四处授予清零｜通过 |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「职级规则」所述对象｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「外包人员与审计人员（2026-06-29 新增）」所述对象｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「客人与员工的个人信息保护」所述对象｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（33 个文件现状与定稿新增项）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 适用｜已拍板规则（R10 人工放行废止）落到代码｜通过 |

共 181 项；逐项给出适用与结论的 45 项，其余 136 项无本面。

## 5. 已定稿 001…004 现态

全部已修（B3-r1＋B3-r2），B4-r2 双 PASS；修复队列「是」＝0。

## 6. 本次 Findings

无新增。残留检索只余已应用迁移里的历史行（不可改）、本批新增的停用回归、security-scans 下历史 semgrep 扫描快照（semgrep-latest-summary.json 等列出当时扫描过的已删文件路径，属历史证据，不改；V433 为未跟踪新文件，git grep 不列）。登记不变：O-01/O-02 项目与场次状态常量与枚举速查不同形；O-04 主工作区 V429 与 origin/main 不同；JdbcAdmissionFactRepository:135 过时注释；设计文档已写废止不改；权限中文名不改。

## 7. 结论

席甲 B5-r1 票：**PASS**。
