CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B4-r2 修复 diff 审计 · 席甲报告
版本：V0.01

## 0. 输入与方法

- 席甲：Claude Opus 5.5（作者自审票；B4-r1 本席 PASS 被 Grok 归因票推翻）。2026-09-24T08:28:14Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14，LOCK-AMEND-COUNT 1）。定稿同 B4-r1（findings V4.08 所载 XM-M034-AB-001…004）。
- r2 增量 diff /home/james/audit-tmp/xm034_b3r2.diff（1714bytes SHA256 716e7d09e06722474b15f5881ea682c58bff9163629e9c60a5e93d947bfba9ef）；累计 diff /home/james/audit-tmp/xm034_b3cum.diff（126694bytes SHA256 21e4a442ddf404285e2b3a289a36acb6daae6b454549256afce3eccc65bb8797）。落盘前未读 Grok 本次正文。

## 1. r2 增量全文

```
diff --git a/docs/production-readiness-evidence/security-baseline-latest.json b/docs/production-readiness-evidence/security-baseline-latest.json
index 0ce003fc..908330ad 100644
--- a/docs/production-readiness-evidence/security-baseline-latest.json
+++ b/docs/production-readiness-evidence/security-baseline-latest.json
@@ -1,7 +1,7 @@
 {
   "status": "passed",
   "evidence_type": "formal_security_baseline_v1",
-  "verified_at": "2026-09-24T08:04:55Z",
+  "verified_at": "2026-09-10T05:00:53Z",
   "verified_on_host": "192.168.88.160",
   "verification_command": "npm run verify:security-baseline",
   "baseline_path": "security-scans/formal-security-baseline-v1.md",
@@ -9,6 +9,6 @@
   "openapi_operation_count": 730,
   "security_baseline_version": "V1.03",
   "plaintext_secret_present": false,
-  "notes": "XM-M034-AB（操作员 D-033「删除」）：票务人工放行写入口已在代码中去掉（契约本无该路径），游乐人工改判 createAmusementManualOverride / listAmusementManualOverrides 两项接口下线，OpenAPI 由 732 项降为 730 项；由 main 生成器重生基线与发现登记两件，verify:security-baseline --strict 绿。生产前置阻断仍以原台账为准。",
-  "version": "V0.04"
+  "notes": "U-FIX B3（U13-001/U13-010/U14-001）：OpenAPI 定稿后由 main 生成器重生三件并同步 732 项接口（删除 setMerchantSelfOperated；新增 issueStaffLockerScanToken / recordStaffLockerScan（柜机单端）/ coverStaffShift / listStaffShiftCovers）；U13-010 已修（生成器 reminder_days 判据读取面扩到 canonical 规则文档），verify:security-baseline --strict 绿。生产前置阻断仍以原台账为准。",
+  "version": "V0.03"
 }
```

## 2. 逐块归因

| 文件 | 块 | 块头 | ± | 归因 ID | 首条改动行（截断） |
|---|---:|---|---|---|---|
| security-baseline-latest.json | 1 | `@@ -1,7 +1,7` | +1/−1 | XM-M034-AB-004 | "verified_at": "2026-09-24T08:04:55Z", |
| security-baseline-latest.json | 2 | `@@ -9,6 +9,6` | +2/−2 | XM-M034-AB-004 | "notes": "XM-M034-AB（操作员 D-033「删除」）：票务人工放行写入口已在代码中去掉（契约本无该路径），游乐人工改判 c |

共 2 块，+3/−3，逐块归因齐全。

两块都是把 verified_at、notes、version 恢复为 origin/main 原值；相对 origin/main，该文件只剩 `"openapi_operation_count": 730` 一行不同（门禁 verify-production-readiness-evidence 第 119 行比较的字段），归 004。越界 0。

## 3. 证据

```
累计 diff 已应用（sha256 21e4a442ddf404285e2b3a289a36acb6daae6b454549256afce3eccc65bb8797）
30 个路径与主工作区一致：yes
node_modules 软链正确
verify:predeploy=0
36
PRODUCTION_READINESS_EVIDENCE_PASS
PRODUCTION_BLOCKER_REGISTER_PASS
PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
scratch removed
```

累计 diff 相对 B4-r1 已判内容 PASS 的 30 个路径，只有该证据文件由「四处改动」收窄为「一处」；其余 29 个路径逐字节未变（累计 diff 与 B3-r1 冻结 diff 在这 29 个路径上相同）。全链 verify:predeploy 退出码 0。

## 4. 自审纠正

B3-r1 把「门禁要求的产物一并带入」读成可以顺手改写整份证据说明（时间戳、说明段、版本），超出门禁实际比较的字段。今后对无生成器的门禁输入文件，只改门禁比较的字段。

## 5. 结论

- 归因票：**PASS**。内容票：**PASS**。席甲 B4-r2 票：**PASS**。
