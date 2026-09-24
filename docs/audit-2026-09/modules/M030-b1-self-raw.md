CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本文件非席甲所写；使 `d` 键变陈旧的扫描器修复 M029-002 为席甲所写）。2026-09-21T03:19:37Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；本文件与 HEAD 无差异、与 origin/main（d215fde0）有 5 行差异（2026-09-11 两个未合并本地提交，OPEN PR #446，把四条 text 改为中性表述），审计对象为主工作区现状。
- 模块 M030「租户范围与数据隔离（技术不变量）／质量检查记录／第1段」1 文件，18 判据（GEN-001…018）。B3 累计 0。
- 环境前提（D-028）：扫描器实跑在基于 origin/main 的一次性 worktree 里、拷入本文件后进行（已恢复并删除）；未带 --write-baseline。
- 顺序：本席取证脚本先跑完，随后派 Grok，再落本报告；落盘前未读 Grok 正文（modules/M030-b1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| security-scans/tenant-isolation-baseline.json | 22 | 1431 | 7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15 | 全读（L1-22） |

## 2. 可执行证据原文（/home/james/audit-tmp/m030_b1_run.log）

```
START=2026-09-21T03:18:11Z HEAD=e456ba96 origin/main=d215fde0
== 0. file (lines/bytes/sha256) + three states
security-scans/tenant-isolation-baseline.json 22 1431 7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15 | vsHEAD[] vsOM[ 1 file changed, 5 insertions(+), 5 deletions(-)]
14180777 audit M030: known findings stay open scanner reports only
66bc70a4 audit M030: isolation baseline lists open gaps, not exemptions
446 audit M030: isolation baseline open gaps
== 1. JSON shape
top keys ['note', 'generated_at_source', 'known_findings']
known_findings 4 ['F1:UNSCOPED_DIRECT_READ:access_grant_requests', 'F2:UNREGISTERED_TABLE:d', 'F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution', 'F1:UNSCOPED_DIRECT_READ:integration_channel_store_map']
dup keys []
every entry has key+text True
== 2. scanner vs this baseline (disposable worktree at origin/main + this file)
TENANT_GATE_EXIT=0
TENANT_GATE_STRICT_EXIT=1
SECURITY_BASELINE_GATE_EXIT=0
scratch restored clean
scratch removed
[default, full]
[tenant-isolation] 扫描范围：
  - 权威表清单（CSV）：335 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：73 表
    · amusement_incident_actions (scope_source: inspection/incident id)
    · amusement_safety_actions (scope_source: inspection/incident id)
    · analytics_ai_thresholds (scope_source: dimension/project/merchant)
    · analytics_customer_intents (scope_source: dimension/project/merchant)
    · analytics_metric_anomalies (scope_source: dimension/project/merchant)
    · analytics_metric_definitions (scope_source: dimension/project/merchant)
    · …其余 67 表略
[tenant-isolation] 注意：baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d

[tenant-isolation] PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
[strict findings]
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）在 backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java 中有 SELECT 
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）在 backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java 中有 SE
[security-baseline gate tail]
production OpenAPI/Swagger docs access policy verification passed.
Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.
== 3. consumers of this file
./tools/verify-tenant-isolation.mjs ./tools/generate-formal-security-baseline.mjs ./docs/production-evidence-closure-gap-report-v0.1.md ./security-scans/findings-register-v1.md ./security-scans/formal-security-baseline-v1.md 
29:  tenantIsolationBaseline: path.join(root, "security-scans", "tenant-isolation-baseline.json"),
125:const tenantIsolationBaseline = readOptional(paths.tenantIsolationBaseline);
182:  tenantIsolationBaselineReady: tenantIsolationBaseline.trim().length > 0,
security-scans/findings-register-v1.md:27:| P2 | `SEC-P2-TENANT-ISOLATION-PROJECT-READ-001` | 招商/机构 5 表跨项目读残留（棘轮 F1）按 project_id 下沉仓储谓词 | resolved | 已修�
tools/generate-formal-security-baseline.mjs:304:      + "access_grant_requests 保留 baseline（AccessService.canViewGrantRequest 服务层按 actor 隔离更细，静态扫描器不识别服务层�
tools/generate-formal-security-baseline.mjs:309:      + "`PartnerJdbcIntegrationTests`/`LeasingJdbcIntegrationTests` 负例、tenant-isolation baseline 收紧至 1 条",
tools/generate-formal-security-baseline.mjs:310:    nextAction: "若扫描器将来识别服务层闸，可移除最后 1 条 baseline 条目",
== 4. why access_grant_requests no longer reproduces (origin/main source)
727:                  FROM access_grant_requests
747:        List<String> values = jdbcTemplate.query("SELECT authorization_fingerprint FROM access_grant_requests WHERE id = :id",
761:                 FROM access_grant_requests request
766:                        AND (request.project_id IS NULL OR request.project_id = :organization_id)))
== 5. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T03:18:13Z
```

## 3. 三线审计

- 第一性原理线。用途：告诉隔离机审「这几条是已知、暂存的缺口，别因它们阻断；除此之外一律红」。实际行为：4 条里 2 条仍被扫描器复现、起到暂存作用；另 2 条已无对应扫描结果，只剩「将来同一问题回归时静默放行」这一个效果。未经验证的前提：「清单里的每一条现在都还是扫描器报出的缺口」——2 条不成立。
- 顺从式线：18 判据逐项见 §4；22 行逐行读过。
- 对抗式线（grilling）：把 access_grant_requests 的项目范围谓词删掉会怎样→扫描器报 F1，但键在基线里→不红；扫描器哪天又把某个别名 `d` 当表→键在基线里→不红；删掉这两条会不会让现在的门禁变红→不会（探针 2：现只复现 2 条，均在清单内）；`verify:security-baseline` 会不会受影响→它只检查本文件非空（探针 3），删两条后仍绿；text 里的证据指向找不找得到→generated_at_source 在同文件顶层；另两条（integration_channel_store_map、revenue_stream_attribution）是不是真缺口→扫描器确实每次都报，text 如实写「尚未收紧」，保留。

## 4. 18 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| GEN-001 必要性与最小性 | 适用｜文件是隔离机审棘轮的唯一基线，有存在理由；4 条里有 2 条已无对应扫描结果，属多余条目（CX-M030-001）｜不通过 |
| GEN-002 可执行正确性 | 适用｜JSON 可解析、结构为 note／generated_at_source／known_findings[key,text]，键无重复（探针 1）；在 origin/main + 本文件的一次性 worktree 里默认路径 exit 0、--strict exit 1（探针 2）。但扫描器现只复现 2 条（integration_channel_store_map、revenue_stream_attribution），清单里的 `F1:…:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d` 已不再复现，工具每次运行都打印「baseline 中 2 条已不再复现…应从 baseline 与台账移除以收紧闸口」｜不通过 |
| GEN-003 测试有效性 | 适用｜本文件的「可失败检查」就是隔离机审本身：清单外新增 finding 即红（M029 实测）；陈旧键只提示不失败，所以 2 条陈旧项一直留到现在｜登记 |
| GEN-004 信任边界校验 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-005 授权与最小权限 | 适用｜陈旧键的实际后果：两条键对应的问题若将来回归（例如有人删掉 `JdbcAccessRepository` 对 access_grant_requests 的项目范围谓词，或扫描器再次把别名误当表），会被基线静默放行、不作为「新增回归」阻断——棘轮在这两个点上是松的（CX-M030-001）｜不通过 |
| GEN-006 数据完整性 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-007 并发与幂等 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-008 接口与模式一致性 | 适用｜键格式 `F{1,2}:<类型>:<表>` 与 `verify-tenant-isolation.mjs` 生成的 key 一致｜通过 |
| GEN-009 错误处理与恢复 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-010 安全与敏感信息 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-011 可观测与审计证据 | 适用｜text 以「证据：generated_at_source」指向顶层字段「static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv」，可定位；安全台账侧的对应描述另见 OBS-1｜通过 |
| GEN-012 资源与性能边界 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-013 构建配置与依赖 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-014 数据库迁移正确性 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-015 界面可用性与可访问性 | 无本面｜静态 JSON 记录文件，不含该面 |
| GEN-016 文档可验证性 | 适用｜note 写「本清单只登记扫描器已发现、尚未收紧的项…收紧后从本清单移除」；`access_grant_requests` 条 text 写「未见 project_id = : 的 scoped 查询。尚未收紧」——origin/main `JdbcAccessRepository.java:766` 已有 `request.project_id = :organization_id`，扫描器不再报；`d` 条 text 写「扫描器报未登记表名 d。尚未收紧」——M029-002 合并后扫描器不再报。两条 text 与 note 自述的准入条件都不再成立（CX-M030-001）｜不通过 |
| GEN-017 业务来源分类 | 适用｜技术性记录文件；无业务规则｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到扫描器实跑输出（m030_b1_run.log）与源码原文｜通过 |

## 5. 本次 Findings（临时编号）

### CX-M030-001 基线里 2 条已不再复现的键未移除，text 与 note 自述不再成立
- 位置：`security-scans/tenant-isolation-baseline.json:5-12`（`F1:UNSCOPED_DIRECT_READ:access_grant_requests`、`F2:UNREGISTERED_TABLE:d` 两个条目）；`:2` note「本清单只登记扫描器已发现、尚未收紧的项…收紧后从本清单移除」。
- 事实：探针 2——扫描器现只报 2 条，工具每次运行都提示这两个键「已不再复现…应从 baseline 与台账移除以收紧闸口」。原因：origin/main `JdbcAccessRepository.java:766` 已按 `project_id = :organization_id` 过滤；M029-002（PR #470）起扫描器不再把 `IS NOT DISTINCT FROM d.col` 的别名当表。
- 正常触发/影响：每次跑门禁都出现陈旧提示；更要紧的是这两个点上棘轮失效——同一问题回归时因键仍在基线里而被静默放行。
- 四分类：冲突？三 HTML 对该文件沉默，否；细化？是——隔离机审是治理地图隔离要求的下位实现，本文件是其棘轮底座 → **细化**；工程缺陷：成立。判据 GEN-016 / GEN-002 / GEN-005 / GEN-001。依据类型 4。
- 最小拟处置：删去这两个条目（保留另两条与 note、generated_at_source 原文）。不带 --write-baseline 重建（重建会把另两条 text 换回扫描器长文案，超出最小处置）。

## 6. 观察（登记，不立项）

- OBS-1（跨模块）`tools/generate-formal-security-baseline.mjs:304-310` 与其生成的 `security-scans/findings-register-v1.md` 写「access_grant_requests 保留 baseline…tenant-isolation baseline 收紧至 1 条…可移除最后 1 条」，与本文件现有 4 条（修后 2 条）不符；属安全基线生成器与台账所在模块。
- OBS-2 主工作区副本与 origin/main 的差异（OPEN PR #446：四条 text 改中性表述、note 改写）随检查点带入；origin/main 旧 text 里「'd' 是 SQL 自连接别名…误报豁免」的说明随之消失，而该误报已由 M029-002 在扫描器侧根除。
- OBS-3 陈旧键只提示不失败，是 `verify-tenant-isolation.mjs` 的既定行为（M029 已审），不在本模块改。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M030-001，细化、工程缺陷成立）。1 文件全读、18 判据逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## M030 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T03:34:58Z。输入：modules/M030-b1-grok-raw.md V0.01（31502 bytes，SHA256 f105b34a6f4b05e31cb3cb72463a157ea12c3cfe26c578497d22e5f1717f5d84）、modules/M030-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M030 B2-r1 整批候选」段（全文件 SHA256 f80c4f2cec0c731b00ddc78b9bfd8abc98a647ddf8d445101bfc63803d94e67d）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01 与 CX-M030-001 同一事实、同一处置 → 001；两席观察全部登记（Grok 观察 2「--write-baseline 内置旧 note」与观察 6「:747 按 id 的无范围读」席甲 B1 未列，采其登记）；无漏并、无改写。
- 分类细化，上位句采 Grok 所列；处置只删两个对象，其余逐字不动；纯 JSON 不加版本标记。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
