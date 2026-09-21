CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本文件现行改动为席甲 B3 所写，以 Grok 票为准）。2026-09-21T03:56:40Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `6a1ead3603bbba4a1d941aa57c49756f90c5384b`。
- 模块 M030 1 文件（§1 全读），18 判据；B3 累计 1。环境前提同 B1（D-028）。
- 顺序：本席取证脚本先跑完，随后派 Grok，再落本报告；落盘前未读 Grok 正文（modules/M030-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；未带 --write-baseline。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| security-scans/tenant-isolation-baseline.json | 14 | 995 | 65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2 | 全读（L1-14） |

## 2. 可执行证据原文（/home/james/audit-tmp/m030_b5r1_run.log；B1 同一取证脚本修后重跑）

```
START=2026-09-21T03:56:35Z HEAD=e456ba96 origin/main=d215fde0
== 0. file (lines/bytes/sha256) + three states
security-scans/tenant-isolation-baseline.json 14 995 65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2 | vsHEAD[ 1 file changed, 8 deletions(-)] vsOM[ 1 file changed, 3 insertions(+), 11 deletions(-)]
14180777 audit M030: known findings stay open scanner reports only
66bc70a4 audit M030: isolation baseline lists open gaps, not exemptions
446 audit M030: isolation baseline open gaps
== 1. JSON shape
top keys ['note', 'generated_at_source', 'known_findings']
known_findings 2 ['F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution', 'F1:UNSCOPED_DIRECT_READ:integration_channel_store_map']
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
END=2026-09-21T03:56:37Z
```

## 3. 三线审计

- 第一性原理线：用途同 B1。修后清单 2 条＝扫描器现行 finding 2 条，逐键相同；默认路径不再打印陈旧提示；两个已收紧的点重新受棘轮保护（同键回归会作为新增 finding 硬失败）。
- 顺从式线：18 判据逐项——B1 不通过的 GEN-001/002/005/016 由 001 闭合；GEN-003（登记）/008/011/017/018 同 B1；其余无本面。
- 对抗式线（grilling）：清单里有没有现在不复现的键→无；有没有现行 finding 不在清单里→无（默认路径新增回归 0）；两条 text「存在无…范围的读。尚未收紧」是否属实→--strict 每次都报这两张表；note 每句与现状是否一致→「只登记扫描器已发现、尚未收紧的项」现成立；`verify:security-baseline` 与全链 predeploy→绿（B3 执行日志）。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001 现态

已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B2-r1 定稿的登记项（安全台账与其生成器里「保留 baseline／收紧至 1 条」的过期描述；--write-baseline 内置旧 note；generated_at_source 不是扫描定位符；:747 按 id 的无范围读属扫描器 B3 盲区；OPEN PR #446 内容随检查点带入）。

## 7. 结论

席甲 B5-r1 票：**PASS**。1 文件全读、18 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
