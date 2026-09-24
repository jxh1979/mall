CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T03:47:07Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M030 B3-r1 修复 diff：before tree `9c214f11e1a52ddc01a267443fba97f59c0540a9` → after tree `6a1ead3603bbba4a1d941aa57c49756f90c5384b`；冻结 diff `/home/james/audit-tmp/m030_b3_r1.diff`（1440 bytes，SHA256 d8e4449d1af84ef21ed31c87af6cb00cfdeeabb2fdcad29f873acee0ad736d83），全文见 §2。定稿 ID：M030-001。
- 顺序：Grok 已先派（modules/M030-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

- 1 文件 1 个 hunk：删除 8 行、新增 0 行。删除的 8 行恰为 `F1:UNSCOPED_DIRECT_READ:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d` 两个 JSON 对象（各 4 行：`{`、key、text、`},`），全部归 M030-001；无新增行；note、generated_at_source 与另两条 F1 逐字未动；其他文件 0。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/security-scans/tenant-isolation-baseline.json b/security-scans/tenant-isolation-baseline.json
index f75a0263..7bc98032 100644
--- a/security-scans/tenant-isolation-baseline.json
+++ b/security-scans/tenant-isolation-baseline.json
@@ -2,14 +2,6 @@
   "note": "verify:tenant-isolation 已知缺口 baseline（棘轮底座）。闸口对此清单之外的新增 finding 硬失败。本清单只登记扫描器已发现、尚未收紧的项，并指向 generated_at_source。不得把缺口写成已修复或豁免。收紧后从本清单移除。重建：node tools/verify-tenant-isolation.mjs --write-baseline。",
   "generated_at_source": "static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv",
   "known_findings": [
-    {
-      "key": "F1:UNSCOPED_DIRECT_READ:access_grant_requests",
-      "text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 access_grant_requests 有 SELECT 读，未见 project_id = : 的 scoped 查询。尚未收紧。证据：generated_at_source。"
-    },
-    {
-      "key": "F2:UNREGISTERED_TABLE:d",
-      "text": "[F2 UNREGISTERED_TABLE] 扫描器报未登记表名 d。尚未收紧。证据：generated_at_source。"
-    },
     {
       "key": "F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution",
       "text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 revenue_stream_attribution 存在无项目范围的读。尚未收紧。证据：generated_at_source。"
```

## 3. 内容审计

- 改后 JSON 可解析，`known_findings` 剩 2 条且与扫描器现行 finding 逐键相同；补丁脚本在落盘前断言了「note／generated_at_source／其余两条对象与原文逐字相同」。
- 一次性 worktree（origin/main + 改后文件）：默认路径 exit 0 且不再出现陈旧提示；--strict 仍只红两条活 F1；`verify:security-baseline` 与全链 `verify:predeploy` exit 0（执行日志）。
- 未跑 --write-baseline；未改扫描器、仓储、安全台账。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m030_b3r1_run.log）

```
START=2026-09-21T03:46:58Z HEAD=e456ba96 origin/main=d215fde0
patchcheck OK; would remove 350 chars
BEFORE_TREE=9c214f11e1a52ddc01a267443fba97f59c0540a9
== RED: current baseline
RED_DEFAULT_EXIT=0
RED_STALE=2
[tenant-isolation] 注意：baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d

== apply in main worktree
wrote security-scans/tenant-isolation-baseline.json 65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2 lines 14
json ok; keys: ['F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution', 'F1:UNSCOPED_DIRECT_READ:integration_channel_store_map']
== GREEN: patched baseline
GREEN_DEFAULT_EXIT=0
GREEN_STRICT_EXIT=1
GREEN_STALE=0
  - 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
strict findings: 2
PREDEPLOY_EXIT=0
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
scratch restored clean
scratch removed
AFTER_TREE=6a1ead3603bbba4a1d941aa57c49756f90c5384b
files changed: security-scans/tenant-isolation-baseline.json 
DIFF bytes=1440 sha256=d8e4449d1af84ef21ed31c87af6cb00cfdeeabb2fdcad29f873acee0ad736d83
 security-scans/tenant-isolation-baseline.json | 8 --------
 1 file changed, 8 deletions(-)
security-scans/tenant-isolation-baseline.json 14 995 65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T03:47:03Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
