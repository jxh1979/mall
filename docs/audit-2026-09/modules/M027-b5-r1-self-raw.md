CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M027 B5-r1 全模块确认审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本模块文件非席甲所写，本轮无任何改动）。2026-09-21T00:39:18Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 模块 M027 13 个迁移（§1 全读），18 判据；B2-r1 双 PASS 定稿修复队列为空，按 V0.17 跳过 B3/B4 直接 B5；B3 累计 0。
- 顺序：Grok 已先派（modules/M027-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。
- 本轮不复用 B1 票：13 个文件重新逐行读过，取证脚本去掉按字节截断后整段重跑（§2 无替换字符），并补了两处 B1 没做的核对（§2b）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席本轮独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V044__finance_security_isolation.sql | 14 | 751 | 7547d6f7de4f7af6b73cab72fbaa37691116b757c47ac7464f0e3ac4b79dcf2d | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql | 31 | 1043 | 9d5b55cc5a0b4cf3abe2c21dd53fb24be21a433a4d260ed6d71834f5629b9b67 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql | 62 | 2911 | 00178d960b8a656689036ad53fc2706f97ebd02806b583eddfbe0cd46f82c5e8 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql | 14 | 1360 | 8369056d19d23b12fbc3c7bc1ff8b4171f804e73028b229076e9149417addadb | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql | 7 | 414 | a7e6dc25e5643397567611dfdfaa1b11903b612d2553c0b3e70a9a65964b2541 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql | 14 | 632 | 80b453285ae44ff4ec8224f6c64e035ee5402297149d9b9d5a1f2db6dc0777e7 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql | 5 | 207 | 7c319c79cb53ea49677940ee785737fbe5a0ea099f66212207518445bbc66063 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql | 10 | 765 | c0e869114b15fb40dcddf7546e88fc821c9adc39cea94c3292c379495bb46bf0 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql | 12 | 1012 | 04a7eb6af7876948092865cce076d81d950baba5271cafb80638687b858111bf | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql | 87 | 4127 | 463857a23c9cbc18a7390a45e1e4cec2ad3578d35a47f41b12003f759620dfa0 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql | 38 | 2224 | 760f8373c5c2410bd7a50ff99a106eedcd869b0fd4161352bd50dd6fe702f9d0 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql | 41 | 1974 | 178d132ab4238d2d341ea480c9b8e40017cbf30d3f8373fb78f995309234b662 | 全读；与 B1 时逐字节一致 |
| backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql | 40 | 2635 | 70f74a458cf24475a0b78a8e360495b1a9468488226cd13ffc1592bf7344cebe | 全读；与 B1 时逐字节一致 |

## 2. 可执行证据原文（/home/james/audit-tmp/m027_b5r1_run.log；B1 同一取证脚本去掉截断后重跑）

```
START=2026-09-21T00:39:15Z HEAD=e456ba96 origin/main=6ce763ff
== 0. files (lines/bytes/sha256) + vs HEAD / origin/main
backend/src/main/resources/db/migration/V044__finance_security_isolation.sql 14 751 7547d6f7de4f7af6b73cab72fbaa37691116b757c47ac7464f0e3ac4b79dcf2d  backend/src/main/resources/db/migration/V044__finance_security_isolation.sql
backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql 31 1043 9d5b55cc5a0b4cf3abe2c21dd53fb24be21a433a4d260ed6d71834f5629b9b67  backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql
backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql 62 2911 00178d960b8a656689036ad53fc2706f97ebd02806b583eddfbe0cd46f82c5e8  backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql
backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql 14 1360 8369056d19d23b12fbc3c7bc1ff8b4171f804e73028b229076e9149417addadb  backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql
backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql 7 414 a7e6dc25e5643397567611dfdfaa1b11903b612d2553c0b3e70a9a65964b2541  backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql
backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql 14 632 80b453285ae44ff4ec8224f6c64e035ee5402297149d9b9d5a1f2db6dc0777e7  backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql
backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql 5 207 7c319c79cb53ea49677940ee785737fbe5a0ea099f66212207518445bbc66063  backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql
backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql 10 765 c0e869114b15fb40dcddf7546e88fc821c9adc39cea94c3292c379495bb46bf0  backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql
backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql 12 1012 04a7eb6af7876948092865cce076d81d950baba5271cafb80638687b858111bf  backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql
backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql 87 4127 463857a23c9cbc18a7390a45e1e4cec2ad3578d35a47f41b12003f759620dfa0  backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql
backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql 38 2224 760f8373c5c2410bd7a50ff99a106eedcd869b0fd4161352bd50dd6fe702f9d0  backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql
backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql 41 1974 178d132ab4238d2d341ea480c9b8e40017cbf30d3f8373fb78f995309234b662  backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql
backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql 40 2635 70f74a458cf24475a0b78a8e360495b1a9468488226cd13ffc1592bf7344cebe  backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql
diff vs HEAD:  ; vs origin/main: 
== 1. flyway history: applied + checksum state in complex_test
044|t|-182843814 045|t|-1679173742 046|t|-1422152229 047|t|1403914415 049|t|-1574552726 051|t|-1008319877 052|t|-41470600 065|t|-108722951 174|t|1712723501 214|t|-905068566 237|t|333115499 243|t|-48591936 244|t|-25656073 
flyway max version: 428
== 2. isolation columns now (table.column type nullable)
amusement_incidents.merchant_id character varying(64) null=YES
amusement_projects.merchant_id character varying(64) null=YES
amusement_safety_notices.merchant_id character varying(64) null=YES
amusement_safety_templates.merchant_id character varying(64) null=YES
ar_ap_items.merchant_id character varying(64) null=YES
ar_ap_items.project_id character varying(64) null=YES
audit_events.merchant_id character varying(64) null=YES
bar_bills.merchant_id character varying(100) null=YES
bar_orders.merchant_id character varying(100) null=YES
bar_production_tickets.merchant_id character varying(100) null=YES
bar_products.merchant_id character varying(100) null=YES
bar_reservations.merchant_id character varying(100) null=YES
bar_tables.merchant_id character varying(100) null=YES
compliance_reminders.merchant_id character varying(80) null=YES
evidence_attachments.merchant_id character varying(64) null=YES
evidence_events.merchant_id character varying(64) null=YES
evidence_reviews.merchant_id character varying(64) null=YES
inventory_high_value_reviews.merchant_id character varying(64) null=YES
inventory_items.merchant_id character varying(64) null=YES
inventory_purchase_requests.merchant_id character varying(64) null=YES
inventory_risk_alerts.merchant_id character varying(64) null=YES
inventory_stock_adjustments.merchant_id character varying(64) null=YES
inventory_stock_balances.merchant_id character varying(64) null=YES
inventory_stock_balances.project_id character varying(64) null=YES
inventory_stock_batches.merchant_id character varying(64) null=YES
inventory_stock_batches.project_id character varying(64) null=YES
inventory_stock_movements.merchant_id character varying(64) null=YES
inventory_stock_movements.project_id character varying(64) null=YES
inventory_supplier_reconciliations.merchant_id character varying(64) null=YES
inventory_suppliers.merchant_id character varying(64) null=YES
mall_floor_plans.merchant_id character varying(64) null=YES
refund_authority_limits.project_id character varying(64) null=YES
refund_auto_rules.project_id character varying(64) null=YES
saved_bottles.merchant_id character varying(64) null=YES
service_orders.project_id character varying(64) null=YES
staff_attendance_records.project_id character varying(64) null=YES
staff_base_salaries.project_id character varying(64) null=YES
staff_commission_ledger.project_id character varying(64) null=YES
staff_leave_requests.project_id character varying(64) null=YES
staff_payroll_adjustments.project_id character varying(64) null=YES
staff_payroll_statements.project_id character varying(64) null=YES
staff_shift_schedules.project_id character varying(64) null=YES
merchants.id / projects.id width: merchants.id=64 projects.id=64 
== 3. constraints created here: present? validated? nullable key columns?
uq_daily_close_scope_date|u|t|UNIQUE (business_date, scope_type, scope_id)
chk_stock_adjustment_type|c|f|CHECK (((adjustment_type)::text = ANY ((ARRAY['gain'::character varying, 'loss'::character varying, 'damage'::character varying, 'waste'::character varying, 'count_correction'::character varying])::text[]))) NOT VALID
uq_compliance_reminder_dedup|u|t|UNIQUE (source_type, source_id, reminder_type, due_date)
uq_refund_authority_project :: CREATE UNIQUE INDEX uq_refund_authority_project ON public.refund_authority_limits USING btree (subject_type, subject_id, refund_domain, COALESCE(project_id, '__global__'::character varying))
uq_refund_auto_rule_project :: CREATE UNIQUE INDEX uq_refund_auto_rule_project ON public.refund_auto_rules USING btree (refund_domain, rule_type, COALESCE(project_id, '__global__'::character varying))
nullable key columns: finance_daily_closes.business_date null=NO compliance_reminders.due_date null=NO finance_daily_closes.scope_id null=NO compliance_reminders.source_id null=NO compliance_reminders.reminder_type null=NO compliance_reminders.source_type null=NO finance_daily_closes.scope_type null=NO 
== 4. indexes created here: present?
index check done (only MISSING lines printed)
staff_base_salaries project index: 0
== 5. follow-ups in later migrations
VALIDATE chk_stock_adjustment_type: 
later migrations touching chk_stock_adjustment_type / adjustment_type: V012__p0_inventory_saved_bottle_trace.sql V019__p1_bar_table_session_adjustments.sql V020__p1_bar_bill_adjustments.sql V036__p2_ticket_dynamic_pricing_rules.sql V046__inventory_security_fixes.sql V079__bar_bill_waiver_refund.sql V124__bar_void_reject_reversal_adjustment.sql 
later migrations touching uq_refund_authority*/uq_refund_auto_rule*: V156__refund_tiered_authorization.sql V174__refund_authority_project_isolation.sql 
later migrations touching uq_daily_close_scope_date: V044__finance_security_isolation.sql 
later migrations touching uq_compliance_reminder_dedup: V051__compliance_security_fixes.sql 
== 6. code vs constraints
-- adjustment types accepted by service:
backend/src/main/java/com/complex/platform/inventory/InventoryService.java:986:                                        "count_correction",
backend/src/main/java/com/complex/platform/inventory/InventoryService.java:1017:                                                "count_correction",
backend/src/main/java/com/complex/platform/inventory/InventoryStockMovementService.java:23:    private static final Set<String> ALLOWED_ADJUSTMENT_TYPES =
backend/src/main/java/com/complex/platform/inventory/InventoryStockMovementService.java:24:            Set.of("gain", "loss", "damage", "waste", "count_correction");
backend/src/main/java/com/complex/platform/inventory/InventoryStockMovementService.java:38:            "count_correction", InventoryHighValueReviewService.SUBJECT_STOCK_VARIANCE);
backend/src/main/java/com/complex/platform/inventory/InventoryStockMovementService.java:85:                    if (!ALLOWED_ADJUSTMENT_TYPES.contains(adjustmentType)) {
-- refund authority upsert conflict targets:
-- daily close scope_id null writes:
305:                    id, business_date, scope_type, scope_id, force_close, status, locked_at, created_at
308:                    :id, :business_date, :scope_type, :scope_id, :force_close, :status, :locked_at, :created_at
322:    public boolean dailyCloseExists(LocalDate businessDate, String scopeType, String scopeId) {
558:                SELECT id, business_date, scope_type, scope_id, force_close, status, locked_at, created_at
1406:                .addValue("scope_id", dailyClose.scopeId())
== 7. NULL-means-unrestricted read predicates (V237/V243/V244 comments)
567:                   AND (:project_id IS NULL OR project_id = :project_id)
603:                   AND (:project_id IS NULL OR project_id = :project_id)
636:                   AND (:project_id IS NULL OR project_id = :project_id)
backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java:169:                   AND (:project_id IS NULL OR project_id = :project_id)
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:23:        // AUD-008 综合体隔离：只命中「本综合体行 OR 全局(NULL)行」，精确综合体优先（ORDER BY (project_id IS NULL)：
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:25:        // 为 unknown、仅 `project_id IS NULL` 命中 → 只回退全局，绝不跨到其它综合体。
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:31:                   AND (project_id = :pid OR project_id IS NULL)
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:32:                 ORDER BY (project_id IS NULL)
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:46:                   AND (project_id = :pid OR project_id IS NULL)
== 8. V214 claim: later writes derive project_id in repository SQL
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:35:            "(SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :user_id"
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:88:                INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_by, updated_at, project_id)
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:103:                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_by, created_at, project_id)
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:117:                INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at, project_id)
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:132:                "SELECT project_id FROM staff_profiles WHERE user_id = :user_id ORDER BY created_at DESC LIMIT 1",
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:233:                  LEFT JOIN (SELECT DISTINCT ON (user_id) user_id, project_id, merchant_id, company_id FROM staff_profiles
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:237:                 -- 绝不 INSERT/覆盖其他项目的薪资单。无档案员工(sp.project_id 为 NULL)只在平台口径纳入。
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:653:                    INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,note,created_by,created_at,updated_at,project_id)
== 9. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md 
END=2026-09-21T00:39:17Z
```

说明：探针 6「refund authority upsert conflict targets」段为空系 grep 窗口只取 INSERT 后 3 行所致，实查见 B1 §2（`JdbcRefundAuthorizationRepository.java:61`）。

## 2b. 本轮补充核对（只读）

- Grok B1 观察 4（`upsertBaseSalary` 的 ON CONFLICT 不更新 project_id）：`JdbcStaffRepository.java:88` 起的 INSERT 在冲突时只更新薪资与更新人，project_id 取首次写入时的推导值；V214 回填与首次写入口径一致，后续员工换项目时该列不随档案变化——属 access/staff 仓储所在模块的运行期口径，不是本模块迁移的缺陷，维持登记。
- V174 DROP 的两个旧约束名在库内已不存在、两个表达式唯一索引存在（§2 探针 3）；V156 建表时的旧唯一键已被完全取代，无并存。

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。四条前提复核结论不变：隔离列由服务端身份写入；唯一键列均 NOT NULL；现行 upsert 冲突目标与 V174 表达式索引逐字匹配；NOT VALID 约束至今未 VALIDATE（已登记并报操作员）。
- 顺从式线：18 判据逐项结论同 B1 §4——GEN-001/002/003（间接）/004/005/006/007/008/010/012/014/016/017/018 通过，GEN-009/011/013/015 无本面；本轮重读与重跑未见变化。
- 对抗式线（grilling）：同 B1 §3 八问逐一重走，结论不变；另问「13 个文件自 B1 以来有没有被动过」→ §1 SHA 与 B1 self raw 逐个相同；「库有没有被迁到新版本导致对象变化」→ flyway 最高版本仍 428，对象现态同 B1。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿现态

本模块无 ID；修复队列为空；11 条观察只登记（findings 顶部 B2-r1 定稿段）。

## 5. 本次 Findings

无。

## 6. 观察（登记）

- 同 B2-r1 定稿的 11 条；其中第 1 条（`chk_stock_adjustment_type` 以 NOT VALID 建立后从未 VALIDATE）报操作员。

## 7. 结论

席甲 B5-r1 票：**PASS**。13 文件全读、18 判据逐项、取证脚本整段重跑；未改任何文件；落盘前未读 Grok 本次正文。
