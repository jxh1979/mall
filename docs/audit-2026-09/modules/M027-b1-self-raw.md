CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M027 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本模块文件非席甲所写）。2026-09-21T00:15:50Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；13 个文件与 HEAD、origin/main（6ce763ff）均无差异。
- 模块 M027「租户范围与数据隔离（技术不变量）／数据库迁移／第1段」13 个迁移，18 判据（GEN-001…018，无 HTML 条目）。B3 累计 0。
- 前提：13 个迁移均已应用，Flyway 校验和覆盖全文（含注释），不可改写；缺陷只能经新的前向迁移修复或登记。
- 顺序：Grok 已先派（modules/M027-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V044__finance_security_isolation.sql | 14 | 751 | 7547d6f7de4f7af6b73cab72fbaa37691116b757c47ac7464f0e3ac4b79dcf2d | 全读 |
| backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql | 31 | 1043 | 9d5b55cc5a0b4cf3abe2c21dd53fb24be21a433a4d260ed6d71834f5629b9b67 | 全读 |
| backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql | 62 | 2911 | 00178d960b8a656689036ad53fc2706f97ebd02806b583eddfbe0cd46f82c5e8 | 全读 |
| backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql | 14 | 1360 | 8369056d19d23b12fbc3c7bc1ff8b4171f804e73028b229076e9149417addadb | 全读 |
| backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql | 7 | 414 | a7e6dc25e5643397567611dfdfaa1b11903b612d2553c0b3e70a9a65964b2541 | 全读 |
| backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql | 14 | 632 | 80b453285ae44ff4ec8224f6c64e035ee5402297149d9b9d5a1f2db6dc0777e7 | 全读 |
| backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql | 5 | 207 | 7c319c79cb53ea49677940ee785737fbe5a0ea099f66212207518445bbc66063 | 全读 |
| backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql | 10 | 765 | c0e869114b15fb40dcddf7546e88fc821c9adc39cea94c3292c379495bb46bf0 | 全读 |
| backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql | 12 | 1012 | 04a7eb6af7876948092865cce076d81d950baba5271cafb80638687b858111bf | 全读 |
| backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql | 87 | 4127 | 463857a23c9cbc18a7390a45e1e4cec2ad3578d35a47f41b12003f759620dfa0 | 全读 |
| backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql | 38 | 2224 | 760f8373c5c2410bd7a50ff99a106eedcd869b0fd4161352bd50dd6fe702f9d0 | 全读 |
| backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql | 41 | 1974 | 178d132ab4238d2d341ea480c9b8e40017cbf30d3f8373fb78f995309234b662 | 全读 |
| backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql | 40 | 2635 | 70f74a458cf24475a0b78a8e360495b1a9468488226cd13ffc1592bf7344cebe | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m027_b1_run.log）

```
START=2026-09-21T00:12:37Z HEAD=e456ba96 origin/main=6ce763ff
== 0. files (lines/bytes/sha256) + vs HEAD / origin/main
backend/src/main/resources/db/migration/V044__finance_security_isolation.sql 14 751 7547d6f7de4f7af6b73cab72fbaa37691116b757c47ac7464f0e3ac4b79dcf2d
backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql 31 1043 9d5b55cc5a0b4cf3abe2c21dd53fb24be21a433a4d260ed6d71834f5629b9b67
backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql 62 2911 00178d960b8a656689036ad53fc2706f97ebd02806b583eddfbe0cd46f82c5e8
backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql 14 1360 8369056d19d23b12fbc3c7bc1ff8b4171f804e73028b229076e9149417addadb
backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql 7 414 a7e6dc25e5643397567611dfdfaa1b11903b612d2553c0b3e70a9a65964b2541
backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql 14 632 80b453285ae44ff4ec8224f6c64e035ee5402297149d9b9d5a1f2db6dc0777e7
backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql 5 207 7c319c79cb53ea49677940ee785737fbe5a0ea099f66212207518445bbc66063
backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql 10 765 c0e869114b15fb40dcddf7546e88fc821c9adc39cea94c3292c379495bb46bf0
backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql 12 1012 04a7eb6af7876948092865cce076d81d950baba5271cafb80638687b858111bf
backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql 87 4127 463857a23c9cbc18a7390a45e1e4cec2ad3578d35a47f41b12003f759620dfa0
backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql 38 2224 760f8373c5c2410bd7a50ff99a106eedcd869b0fd4161352bd50dd6fe702f9d0
backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql 41 1974 178d132ab4238d2d341ea480c9b8e40017cbf30d3f8373fb78f995309234b662
backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql 40 2635 70f74a458cf24475a0b78a8e360495b1a9468488226cd13ffc1592bf7344cebe
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
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:23:        // AUD-008 综合体隔离：只命中「本综合体行 OR 全局(NULL)行」，精确综合体优
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:25:        // 为 unknown、仅 `project_id IS NULL` 命中 → 只回退全局，绝不跨到其它综合体�
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:31:                   AND (project_id = :pid OR project_id IS NULL)
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:32:                 ORDER BY (project_id IS NULL)
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java:46:                   AND (project_id = :pid OR project_id IS NULL)
== 8. V214 claim: later writes derive project_id in repository SQL
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:35:            "(SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :user_id"
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:88:                INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_by, updated_at, project_id)
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:103:                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_b
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:117:                INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note,
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:132:                "SELECT project_id FROM staff_profiles WHERE user_id = :user_id ORDER BY created_at DESC LIMIT 1",
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:233:                  LEFT JOIN (SELECT DISTINCT ON (user_id) user_id, project_id, merchant_id, company_id FROM staff_p
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:237:                 -- 绝不 INSERT/覆盖其他项目的薪资单。无档案员工(sp.project_id 为 NULL)只在平
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:653:                    INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,note,cre
== 9. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T00:12:39Z
```

补充只读核对：
- 探针 6「refund authority upsert conflict targets」段为空是 grep 窗口只取了 INSERT 后 3 行所致；实查 `JdbcRefundAuthorizationRepository.java:61` `ON CONFLICT (subject_type, subject_id, refund_domain, COALESCE(project_id, '__global__'))`，与 V174 表达式索引逐字匹配。
- `inventory_stock_adjustments` 在 complex_test 0 行（`select adjustment_type, count(*) … group by 1` 无输出）；V012 原始定义 `adjustment_type VARCHAR(64) NOT NULL` 无 CHECK。
- V243 回填用到的 source_type 字面量在现行主代码中均有出处（contract 11、contract_rent 2、property_fee 10、settlement 2、cam 3 处）；complex_test 的 ar_ap_items 只有 stored_value_topup 28 行，回填对测试库无对象。回填对生产存量数据当时的实际命中情况，两席按只读约束无法验证，属一次性历史动作。
- `JdbcInventoryRepository.java:565-569` 读谓词原文：`WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id) AND (:project_id IS NULL OR project_id = :project_id) AND (:item_id IS NULL OR item_id = :item_id)`。

## 3. 三线审计

- 第一性原理线。用途：给各业务表补「商户／项目」隔离列与配套索引、唯一键，并回填存量。实际行为：探针 1-4 证实全部对象现存。未经验证的前提：①隔离列真由服务端身份写入（抽核三域属实）；②唯一键列不可空（属实）；③现行 upsert 的冲突目标与 V174 的表达式索引匹配（属实）；④NOT VALID 约束之后有人 VALIDATE（不属实，见 OBS-1）。
- 顺从式线：18 判据逐项见 §4；13 文件 375 行逐行读过。
- 对抗式线（grilling）：同一逻辑日结用不同幂等键重复提交→`uq_daily_close_scope_date` 拒绝（三列均 NOT NULL）；同一到期提醒被多次扫描→`uq_compliance_reminder_dedup` 拒绝；两个综合体配同一主体/域的退款额度→V174 后互不覆盖，全局行只有一个确定槽；项目 A 员工读库存余额/流水/批次→只见 project_id = A 的行，NULL 历史行不可见；写入一个闭集外的 adjustment_type→约束拒绝（NOT VALID 对新写入同样生效）、服务层先行 400；存量里若有闭集外的旧值→不会被发现（OBS-1）；V049 无 IF NOT EXISTS→Flyway 每库只跑一次，不构成问题；有没有列建了却没人写→抽核的域均有写入路径。

## 4. 18 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| GEN-001 必要性与最小性 | 适用｜13 个迁移各自只做「加隔离列＋部分索引／唯一键／回填」；V065 单独补 V049 漏建的 4 个索引，属必要接续；无多余对象｜通过 |
| GEN-002 可执行正确性 | 适用｜13 个版本在 complex_test 的 flyway_schema_history 全部 success=t（探针 1）；所建的隔离列、部分索引、3 个约束、2 个表达式唯一索引现态齐全（探针 2-4；索引检查只打印缺失项，无缺失）｜通过 |
| GEN-003 测试有效性 | 适用（间接）｜迁移本身无单测；由真库测试类在迁移后的库上运行间接守住（库即 complex_test，flyway 428）；回填语句在空测试库上不产生行，属一次性历史动作｜通过（间接） |
| GEN-004 信任边界校验 | 适用｜各迁移头注称隔离列「由调用方安全令牌写入、从不取自请求参数」：抽核三域——`ArApItemRequest` 无商户字段、`FinanceService` 用 `authorizationService.currentMerchantScope()`；`AuditEventService:88-124` 取 `actor.merchantId()`；`InventoryService` 用 `currentMerchantScope()`。属实｜通过 |
| GEN-005 授权与最小权限 | 适用｜读路径谓词 `(:project_id IS NULL OR project_id = :project_id)`（`JdbcInventoryRepository:567/603/636`、`JdbcFinanceRepository:169`）：调用方无项目范围＝全量；有项目范围＝只见本项目行，project_id 为 NULL 的历史行对项目级调用方不可见（fail-closed）。V237/V244 注释「NULL=不限项目…走全量读通道」按此读法与代码一致。退款授权 `(project_id = :pid OR project_id IS NULL)` 只回退全局行，不跨综合体｜通过 |
| GEN-006 数据完整性 | 适用｜唯一键列均 NOT NULL（finance_daily_closes 三列、compliance_reminders 四列，探针 3），去重声称成立；V174 用 `COALESCE(project_id, '__global__')` 表达式索引堵住「多个 NULL 互不冲突」；V214 回填口径（每 user 取最新 staff_profiles.project_id）与现行仓储写入口径（`JdbcStaffRepository:35/132`）一致；V237/V243/V244 回填只填 NULL 行、推不出留 NULL｜通过 |
| GEN-007 并发与幂等 | 适用｜V044/V051 唯一约束为「不同幂等键打同一逻辑对象」兜底，与头注一致；V174 upsert 的 `ON CONFLICT (…, COALESCE(project_id, '__global__'))`（`JdbcRefundAuthorizationRepository:61`）与表达式唯一索引逐字匹配｜通过 |
| GEN-008 接口与模式一致性 | 适用｜列类型与代码一致；merchant_id 宽度 V047 为 VARCHAR(100)、V051 为 VARCHAR(80)，其余 64（merchants.id 为 64）——更宽不致截断，登记 OBS-2；`chk_stock_adjustment_type` 闭集与 `InventoryStockMovementService.ALLOWED_ADJUSTMENT_TYPES` 五值一致｜通过 |
| GEN-009 错误处理与恢复 | 无本面｜迁移无运行期错误处理分支 |
| GEN-010 安全与敏感信息 | 适用｜隔离列与索引即本模块的安全面；无明文敏感数据写入｜通过 |
| GEN-011 可观测与审计证据 | 无本面｜迁移不产生审计事件 |
| GEN-012 资源与性能边界 | 适用｜商户/项目列均建部分索引（WHERE … IS NOT NULL）；V214 为 8 张表中的 7 张建项目索引，staff_base_salaries（按 user_id 主键、每人一行）未建，登记 OBS-4｜通过 |
| GEN-013 构建配置与依赖 | 无本面｜无构建配置 |
| GEN-014 数据库迁移正确性 | 适用｜版本号单调、均已应用、与 HEAD/origin/main 三态一致（不可改写）；无破坏性动作（V174 DROP CONSTRAINT 后立即建等价更严的表达式唯一索引）；`chk_stock_adjustment_type` 以 NOT VALID 建立、至今 convalidated=f，头注说「回填迁移完成后再 VALIDATE」而后续无 VALIDATE——约束对 V046 之后的全部写入已生效，模式与代码一致，未完成的只是对存量行的一次性校验，登记 OBS-1｜通过（登记 OBS-1） |
| GEN-015 界面可用性与可访问性 | 无本面｜无界面 |
| GEN-016 文档可验证性 | 适用｜注释中的可复现声称逐条核过（GEN-004/005/006/007 各行）；V237/V244 头注里的「基线 origin/main=…、当前最高迁移 V235」是落笔时的历史记录，已应用迁移不可改，登记 OBS-5｜通过 |
| GEN-017 业务来源分类 | 适用｜全部为技术性（隔离列/索引/约束）；V174 头注引「用户裁决：每个综合体各管各的退款审批规则」为业务来源，非本模块发明｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到只读命令输出（m027_b1_run.log 与补充 grep/SELECT）与源码原文｜通过 |

## 5. 本次 Findings

无。

## 6. 观察（登记，不立项）

- OBS-1 `chk_stock_adjustment_type`（V046:56-62）以 NOT VALID 建立，头注称「回填迁移完成后再 VALIDATE CONSTRAINT」，其后的全部迁移里没有任何 VALIDATE（探针 5），库内 convalidated=f。对 V046 之后的写入约束已生效、与服务层闭集一致；未完成的只是对存量行的一次性校验。补一条 `VALIDATE CONSTRAINT` 的前向迁移在生产存量含闭集外旧值时会让部署失败，而生产数据两席都不能查——是否补、何时补属发布决策，登记并报操作员。
- OBS-2 merchant_id 宽度不一（V047 为 100、V051 为 80、其余 64；merchants.id 为 64）：更宽不截断，不立项。
- OBS-3 V049 的 ADD COLUMN 无 IF NOT EXISTS、且当时未建索引（V065 已补）。
- OBS-4 V214 为 8 张表加列、只给 7 张建项目索引；staff_base_salaries 每人一行、按 user_id 主键访问，不立项。
- OBS-5 V237/V244 头注记录的「基线提交号／当时最高迁移号」为历史信息，已应用迁移不可改。
- OBS-6 V243 三条回填对生产存量的实际命中无法在只读约束下验证；推不出的行留 NULL，对项目级调用方不可见、对平台口径可见（头注已说明）。

## 7. 结论

席甲 B1 票：**PASS**（无工程缺陷；OBS-1…6 登记，其中 OBS-1 报操作员）。13 文件全读、18 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。按 V0.17：无发现也须经 B2 登记定稿、跳过 B3/B4、B5 全量确认后才收口。


## M027 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T00:31:15Z。输入：modules/M027-b1-grok-raw.md V0.01（36658 bytes，SHA256 ce202dc090ccc503d2adbaf829b1cba19fedf5f1a4f83a9f666640e2fc0d4353）、modules/M027-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M027 B2-r1 整批候选」段（全文件 SHA256 80c1021382d871a6841c6ce7ad377df475f541d165d1e34f045919975102ed14）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：两席 B1 均 0 finding；席甲 OBS-1…6 与 Grok 观察 1…9 归并登记为 11 条，无漏并、无改写。Grok 观察 4（upsertBaseSalary 不更新 project_id）与观察 8（调用方 `.orElse(null)`）席甲 B1 未见，属别的模块，采其登记。
- 如实说明：上方 §2 证据块里有若干「�」替换字符——取证脚本用按字节截断的 `cut -c` 切断了多字节字符，落盘脚本以容错方式读入所致；不影响所引事实（被截的是长行尾部）。
- 第 1 条（NOT VALID 从未 VALIDATE）：席甲维持登记、不入队——约束对新写入已生效且与服务层闭集一致；补 VALIDATE 的前向迁移有因生产存量而部署失败的风险，生产数据两席不能查，属发布决策，报操作员。Grok 若主张入队并给出不会因存量使部署失败的最小处置，以其意见为准再议。
- 修复队列为空 → 按 V0.17 跳过 B3、B4，直接 B5；B3 次数保持 0。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
