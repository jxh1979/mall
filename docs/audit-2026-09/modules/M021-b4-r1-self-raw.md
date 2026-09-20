CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T17:47:48Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M021 B3-r1 整批修复 diff：before tree `0ea996b1a680dc068d0486da7f22e68fc68353c6` → after tree `a5e4fadfe03ff042c00bd240719b76d9ab7341d6`；冻结 diff `/home/james/audit-tmp/m021_b3_r1.diff`（4412 bytes，SHA256 85adf53df99703a9b7fad430374c693ceb4133c9c57df17f80490ab0ff2a8ce9），全文见 §2。定稿 ID：M021-001（findings V2.66，队列「是」）；M021-002 只登记不动。
- 顺序：Grok 已先派（modules/M021-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| V428__m021_settlement_format_share_media.sql | `@@ -0,0 +1,13 @@` | 13 | 001 |
| SettlementFormatShareS11Tests.java | `@@ -1,4 +1,5 @@` | 1 | 001 |
| SettlementFormatShareS11Tests.java | `@@ -362,6 +363,37 @@ class SettlementFor` | 31 | 001 |

- 2 文件 3 个 hunk 全部归 M021-001（新迁移整文件；测试类的版本标记行与新增 media 用例）；五个已应用迁移与 V366 零改写；未改结算 Java/OpenAPI/三份 HTML/锁；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql b/backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql
new file mode 100644
index 00000000..5920e7d5
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql
@@ -0,0 +1,13 @@
+-- 文件版本：V0.01
+-- M021-001：V384 把商户业态词表放开到六值（加 media=演出传媒）时，只同步了 merchants 与 leasing_leads 两处 CHECK，
+--   漏了同词表的第三处 settlement_bill_format_shares.chk_sbfs_format（V366 建表时为 V348 五值）。
+--   月结的业态取自 merchants.merchant_type（JdbcSettlementBillRepository.aggregateFormatRevenue），
+--   media 业态商户的权益进联合通票月结时，份额留痕行写不进去，整张结算单生成失败。
+-- 业务依据（最终解释权=业务 HTML，逐字）：
+--   business-enum-quick-reference「商户业态」：「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。」
+--   business-system-map-for-operations「收入归属」：「演出票 → 传媒公司」。
+-- 已应用的 V366/V384 不改写；本迁移只把这一处 CHECK 放开到同一六值闭集，不改任何金额列与既有行。
+ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;
+ALTER TABLE settlement_bill_format_shares
+    ADD CONSTRAINT chk_sbfs_format
+    CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
diff --git a/backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java b/backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java
index 19bd0998..f4559e5f 100644
--- a/backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java
+++ b/backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.settlement;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -362,6 +363,37 @@ class SettlementFormatShareS11Tests {
         assertThat(again.get("catering").get("net_revenue_cent")).isEqualTo(10000L);
     }
 
+    // ---------------------------------------------------------------- ⑤ 传媒业态份额（M021-001 / V428）
+
+    /**
+     * M021-001：演出传媒（media）是 V384 起合法的第六种业态（业务 HTML「商户业态」：酒吧、餐饮、零售、游乐、
+     * 演出传媒（系统枚举码 media）、其他；「演出票 → 传媒公司」）。它的权益进联合通票月结时，业态份额留痕行
+     * 必须写得进去——V366 建表时的五值封口由 V428 放开到同一六值闭集——且份额归传媒公司这个主体
+     * （V384 全局种子：performance_ticket 流 → be_media），不进酒吧池、不参与酒吧排名。
+     */
+    @Test
+    void mediaShareIsRecordedAndAttributedToMediaEntity() {
+        String pid = newProject();
+        setRankTier(pid, 1, 9, 5000);
+        seedMerchant("m_fmt_bar_md", "bar");
+        seedMerchant("m_fmt_media", "media");
+        seedVerified(pid, "m_fmt_bar_md", 2, 2, 50000L);
+        seedVerified(pid, "m_fmt_media", 3, 3, 20000L);
+
+        String billId = settlementService.settleMonth(pid, MONTH);
+        Map<String, Object> b = bill(pid);
+
+        Map<String, Map<String, Object>> shares = formatShares(billId);
+        assertThat(shares.keySet()).containsExactlyInAnyOrder("bar", "media");
+        assertThat(shares.get("media").get("net_revenue_cent")).isEqualTo(20000L);
+        assertThat(shares.get("media").get("beneficiary_kind")).as("传媒份额归传媒公司这个主体").isEqualTo("entity");
+        assertThat(shares.get("media").get("beneficiary_entity_id"))
+                .as("V384 全局种子:performance_ticket 流 → 传媒公司").isEqualTo("be_media");
+        assertThat(b.get("ticket_revenue_cent")).isEqualTo(70000L);
+        assertThat(b.get("available_pool_cent")).as("传媒份额不进酒吧池").isEqualTo(50000L);
+        assertThat(itemAmounts(pid)).as("分池明细只有酒吧").containsOnlyKeys("m_fmt_bar_md");
+    }
+
     // ---------------------------------------------------------------- 夹具
 
     private String newProject() {
```

## 3. 内容审计（对照定稿最小处置与现行实态）

- 迁移：`DROP CONSTRAINT IF EXISTS chk_sbfs_format` 后 `ADD CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar','catering','retail','amusement','media','other'))`——与 `chk_merchants_business_format`、`chk_leasing_leads_business_format`、枚举 L195 六值逐值一致；`business_format VARCHAR(16)` 容得下 media；不用 NOT VALID（月结留痕小表）；Flyway 在 PostgreSQL 单事务执行，DROP/ADD 之间无无约束窗口；文件名/版本号合规（V428，见台账编号说明）；头注引 HTML 两句逐字、说明不改写 V366/V384。
- 测试：真实库 `settleMonth`，bar 50000 + media 20000 → 份额两行（bar/media），media 行净额 20000、beneficiary_kind=entity、beneficiary_entity_id=be_media（V384 全局种子 performance_ticket → be_media），通票收入 70000、可分配池 50000（传媒不进酒吧池），分池明细只有酒吧；清场沿用既有 @AfterEach（按 projectId/merchantId）。RED：无 V428 时该例因 chk_sbfs_format 违例失败、其余 7 例通过；GREEN：8 例全过。
- 修后实态：flyway 428 success；同词表 CHECK 三处均六值、无 media 的计数 0；测试无残留行。
- 证据摘要（/home/james/audit-tmp/m021_b3r1_run.log）：
```
pre: flyway top=427; chk_sbfs_format=b ing il musmn o 
RED_MVN_EXIT=1
com.complex.platform.settlement.SettlementFormatShareS11Tests.txt: Tests run: 8, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 4.589 s <<< FAILURE! -- in com.complex.platform.settlement.SettlementFormatShareS11Tests
]; ERROR: new row for relation "settlement_bill_format_shares" violates check constraint "chk_sbfs_format"
red: flyway top=427; leftover rows=0
GREEN_MVN_EXIT=0
com.complex.platform.settlement.SettlementFormatShareS11Tests.txt: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.650 s -- in com.complex.platform.settlement.SettlementFormatShareS11Tests
TEST-com.complex.platform.settlement.SettlementFormatShareS11Tests.xml: tests="8" failures="0" errors="0"
428|m021 settlement format share media|t|1696678370
settlement_bill_format_shares|chk_sbfs_format|CHECK (((business_format)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
same-vocabulary CHECKs without media: 0
test leftovers: merchants=0 shares(media)=0
untouched applied migrations: 0 changed
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m021_b3r1_run.log）

```
START=2026-09-20T17:46:59Z HEAD=e456ba96 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
check OK 26f9b8476b5cfd4ee1139b547f8158d07da3981ed47954518cecf99d7d4be591
pre: flyway top=427; chk_sbfs_format=b ing il musmn o 
BEFORE_TREE=0ea996b1a680dc068d0486da7f22e68fc68353c6
== RED: media case against five-value CHECK (no V428 yet)
check OK 26f9b8476b5cfd4ee1139b547f8158d07da3981ed47954518cecf99d7d4be591
wrote tests 0649e4dedac9e93442625f530a2612479695e8ab29ca5014f25578f5d752e7fa
RED_MVN_EXIT=1
com.complex.platform.settlement.SettlementFormatShareS11Tests.txt: Tests run: 8, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 4.589 s <<< FAILURE! -- in com.complex.platform.settlement.SettlementFormatShareS11Tests
[ERROR] com.complex.platform.settlement.SettlementFormatShareS11Tests.mediaShareIsRecordedAndAttributedToMediaEntity -- Time elapsed: 0.046 s <<< ERROR!
]; ERROR: new row for relation "settlement_bill_format_shares" violates check constraint "chk_sbfs_format"
	at com.complex.platform.settlement.SettlementFormatShareS11Tests.mediaShareIsRecordedAndAttributedToMediaEntity(SettlementFormatShareS11Tests.java:383)
red: flyway top=427; leftover rows=0
== GREEN: add forward migration V428, rerun (Flyway migrates complex_test to 428)
wrote V428__m021_settlement_format_share_media.sql 239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589
GREEN_MVN_EXIT=0
com.complex.platform.settlement.SettlementFormatShareS11Tests.txt: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.650 s -- in com.complex.platform.settlement.SettlementFormatShareS11Tests
TEST-com.complex.platform.settlement.SettlementFormatShareS11Tests.xml: tests="8" failures="0" errors="0"
== post-fix actual state (SELECT only)
428|m021 settlement format share media|t|1696678370
merchants|chk_merchants_business_format|CHECK (((merchant_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
leasing_leads|chk_leasing_leads_business_format|CHECK (((intended_business_type IS NULL) OR ((intended_business_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[]))))
settlement_bill_format_shares|chk_sbfs_format|CHECK (((business_format)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
same-vocabulary CHECKs without media: 0
test leftovers: merchants=0 shares(media)=0
untouched applied migrations: 0 changed
AFTER_TREE=a5e4fadfe03ff042c00bd240719b76d9ab7341d6
DIFF bytes=4412 sha256=85adf53df99703a9b7fad430374c693ceb4133c9c57df17f80490ab0ff2a8ce9
 .../V428__m021_settlement_format_share_media.sql   | 13 +++++++++
 .../settlement/SettlementFormatShareS11Tests.java  | 32 ++++++++++++++++++++++
 2 files changed, 45 insertions(+)
hunks=3 files=2
backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java 495 0649e4dedac9e93442625f530a2612479695e8ab29ca5014f25578f5d752e7fa
backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql 13 239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T17:47:18Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
