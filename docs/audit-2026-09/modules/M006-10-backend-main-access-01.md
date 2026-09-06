# M006 — 后台权限域（access-01）审计收口
版本：V0.01

审计结论：PASS。M006/322，14个模块文件3289行（切分时2988，B3后增长）、52条冻结判据（GEN-001—018 + 7 BEQR + 14 BSM + 13 PGM）全部完成三线双审。B3执行5次（第1–3次席甲Codex；第4次操作员例外、第5次操作员批准，均由Claude Fable 5.1执行），方案复核已修2次沿用，运行失败重跑0。控制V0.17；源工作区160:/home/james/complex-management/main。先完成B5后四值全等并写ledger PASS，再创建本文件。

## 修复及证据

M006-001—036全部定稿修复并经B4双PASS出队：001—033（前三轮，Codex）覆盖DENY优先与高危权限、跨组织/跨商户隔离、模板编辑提权、审批快照指纹、受益人/申请人自审、岗位派生角色、员工注册审批路径、手机号摘要、岗位模板软删除审批、伪分页、跨组织幂等串用、账号类型报错中文等；034（第4轮）四处用户可见报错去英文枚举/缩写/回显（另按操作员指令同步StaffService两处）；035（第5轮）平台管理员有效权限计算对权限字典逐项重复查询（N+1）改为单次计算内只读一次；036（第5轮）无组织归属主体复制模板由NullPointerException改为403 PROJECT_SCOPE_REQUIRED。登记不入队：单参listGrantRequests、私有slug()、EffectivePermissions.wildcard恒false、注释漂移、StaffService.java:363同类英文回显（属M008）。无添加项待答（027已答保留）、无HTML申请、无LOCK-AMEND。

B5-r5证据：席甲101项回归0 FAIL（M006Security 36 / AccessService 35 / Authorization 21 / Idempotency 9）+ 探针（字典200条时有效集计算listPermissions 2次、requirePermission 1次、staff 0次；orphan copy→PROJECT_SCOPE_REQUIRED；18入口无组织主体扫描0原始异常；6条英文字段键经FieldLabels.translate全中文）。席乙111 PASS / 0 FAIL / 0 SKIP（含M006JdbcRegressionTests 12项，complex_test库TEMP表）+ 独立探针（effectiveCalls=2、orphan copy/update/apply/assignRole/assignStaffLevel/assignPosition均PROJECT_SCOPE_REQUIRED、sameOrg.copyOther CROSS_ORG_FORBIDDEN）。两席均未跑完整mvn test、前端、真实支付/短信厂商、并发压测；未跑不等于通过。

| 环节 | 原始证据 |
|---|---|
| B1全量/各批B2 | M006-b1-self-raw.md（含Codex各批B2与席甲第4批B2追加）；M006-b1-grok-raw.md（V0.01–V0.08） |
| B4-r1/r2/r3（Codex） | M006-b4-r{1,2,3}-self-raw.md；M006-b4-r{1,2,3}-grok-raw.md；均双PASS |
| B5-r2/r3（Codex） | M006-b5-r{2,3}-self-raw.md / -grok-raw.md；r3双FAIL→034 |
| B4-r4（Claude） | M006-b4-r4-self-raw.md；M006-b4-r4-grok-raw.md；双PASS |
| B5-r4（Claude） | M006-b5-r4-self-raw.md（FAIL，035/036）；M006-b5-r4-grok-raw.md（PASS）；合取FAIL→B2定稿 |
| B4-r5（Claude） | M006-b4-r5-self-raw.md；M006-b4-r5-grok-raw.md；双PASS |
| B5-r5（Claude） | M006-b5-r5-self-raw.md；M006-b5-r5-grok-raw.md；两席全模块PASS |

五轮B3树对（完整未过滤binary diff及逐块ID归因在各轮两席B4 raw）：
| 轮 | before | after | diff字节/SHA256 |
|---|---|---|---|
| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc |
| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 |
| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 |
| 4 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 54999203b0587456b8d2f473b5ee0f5e3d67908b | 179689 / c390d6d126c6bd46abddd9635cca596c49d0da3b98f6e89701c03743ba009d38（含审计产物8路径） |
| 5 | 4a10ba268b3408173639bc79dba3e52b935164a7 | 7e3d9e970fd7b01735a78089cbeb4921e122cf80 | 297255 / bac943157098c1e9a11c65fc895f33950f6eb688cb5b2062252e20d71afa0102（含.scratch临时6路径，业务3文件） |

Grok B5-r5真实exit0（16:31:47 UTC接收），31051字节/406行/SHA da7fa5257660d431d8dce08fe1c16363a804f27d0955e2caf89d9bcbf3b37d1f；席甲读取前独立落盘13014字节/SHA d0cb18fa8681439003597d779ddd2702031c3a2982a5314ec3c1fb43ca2a3f4e，追加接收说明不改原前缀、不作新盲审票。

## 四值与源快照

actual=sidecar=ledger=当前B5 Grok最后哈希：
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
LOCK-AMEND-COUNT: 0

14文件SHA256（B5-r5席甲raw §1表；合计3289行）：AccessService 8d0b2ca064302aa3837577f415d452285292ab112db9078c1a5a9b7e1c7db14e；AuthorizationService 6ad9fa0fc3b37b27f137aaf82fc63adcbb5719fec9048e72f1232399d920cbb8；AccessController 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c；AccessRepository c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680；其余10文件见该表。三份业务HTML与HEAD逐字相同；35个既有scratch文件保持。

## 后续执行

按操作员已授权的逐模块发布顺序，基于本次已审字节创建release worktree，提交、走propose-change.sh实际predeploy后推送/PR、走merge-pr.sh等待对应HEAD的CI通过后合并，再ff同步160。发布以实际PR/CI/merge回执证明，未完成前不开始M007。禁止借本收口文件声称发布已经完成。

收口时间：2026-09-06T16:33:39Z
