## 文件｜`full-admin-mvp-requirements-code-matrix-v0.1.csv`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`
- main：`docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`
- backup SHA-256：`873625892809cb27c4619a4f5da23b480b9919fa1caa64336257307d778eeea0`
- main SHA-256：`ec71c699d908dec36b1b1aa3a229268e3166d835fabea3f17fbcde77acbc7fc2`
- 大小：3959 → 3967 bytes

- 对齐单元数：备份 10 / main 10

- 差异块数：1
- 相似度（序列比）：0.9000

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - "ADM-BAR-001","卡座与订台","卡座入场人数上限、转卡/合并/续卡审批提示、有效登记入场","/admin/bar/table-sessions","GET /api/v1/bar/tables; GET /api/v1/bar/reservations; POST /api/v1/bar/reservations/{id}/check-in-guest","apps/web/src/admin/pages/BarTableSessionsPage.tsx","tests/e2e/full-admin-mvp.spec.ts::ADM-MVP-04","已完整实现","7 人超限拦截，4 人登记 checked_in","P0"

**main 现行：**

  + "ADM-BAR-001","卡座与订台","卡座入场人数上限、转卡/合并审批提示（无续卡）、有效登记入场","/admin/bar/table-sessions","GET /api/v1/bar/tables; GET /api/v1/bar/reservations; POST /api/v1/bar/reservations/{id}/check-in-guest","apps/web/src/admin/pages/BarTableSessionsPage.tsx","tests/e2e/full-admin-mvp.spec.ts::ADM-MVP-04","已完整实现","7 人超限拦截，4 人登记 checked_in","P0"

**本文件小计：** replace 块 1；仅备份条数 0；仅 main 条数 0；差异块 1

---

