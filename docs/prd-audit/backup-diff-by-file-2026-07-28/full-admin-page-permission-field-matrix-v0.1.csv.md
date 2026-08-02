## 文件｜`full-admin-page-permission-field-matrix-v0.1.csv`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-page-permission-field-matrix-v0.1.csv`
- main：`docs/full-admin-page-permission-field-matrix-v0.1.csv`
- backup SHA-256：`b2f4f175f769b86b84b79eca3765006ced4bb788539164791bdee7e37d2941e4`
- main SHA-256：`0bc3dfd3c02c659ee601d00a1bc2b87e130e89a2ae6c6c7fc561a9a64cb92b31`
- 大小：67746 → 67705 bytes

- 对齐单元数：备份 85 / main 85

- 差异块数：1
- 相似度（序列比）：0.9882

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - ADM-BAR-001,酒吧,卡座与订台,/admin/bar/table-sessions,MVP,SUPER_ADMIN;BAR_MANAGER;SALES,bar.table_session.view;bar.table.reserve;bar.table.transfer;bar.table.merge;bar.table.renew,GET/POST /api/v1/bar/tables;GET/POST /api/v1/bar/reservations;POST /api/v1/bar/reservations/{id}/check-in-guest;GET /api/v1/bar/orders,预订;转卡;合并;续卡;释放,bar_id;table_id;session_id;guest_limit;min_spend;sales_owner,reserved->seated->settled->closed/cancelled,免票人数超限拒绝;跨酒吧拒绝;未结账不可直接转卡,BAR_TABLE_RESERVED;BAR_TABLE_TRANSFERRED;BAR_TABLE_MERGED;BAR_TABLE_RENEWED,UI-ADM-BAR-001,已实现：完整后台首批 MVP 已接入正式 /api/v1 路径，详见 full-admin-mvp-requirements-code-matrix-v0.1.csv

**main 现行：**

  + ADM-BAR-001,酒吧,卡座与订台,/admin/bar/table-sessions,MVP,SUPER_ADMIN;BAR_MANAGER;SALES,bar.table_session.view;bar.table.reserve;bar.table.transfer;bar.table.merge,GET/POST /api/v1/bar/tables;GET/POST /api/v1/bar/reservations;POST /api/v1/bar/reservations/{id}/check-in-guest;GET /api/v1/bar/orders,预订;转卡;合并;释放,bar_id;table_id;session_id;guest_limit;min_spend;sales_owner,reserved->seated->settled->closed/cancelled,免票人数超限拒绝;跨酒吧拒绝;未结账不可直接转卡,BAR_TABLE_RESERVED;BAR_TABLE_TRANSFERRED;BAR_TABLE_MERGED,UI-ADM-BAR-001,已实现：完整后台首批 MVP 已接入正式 /api/v1 路径，详见 full-admin-mvp-requirements-code-matrix-v0.1.csv

**本文件小计：** replace 块 1；仅备份条数 0；仅 main 条数 0；差异块 1

---

