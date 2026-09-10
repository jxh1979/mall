CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U03 未发布材料第 3 段（员工网页和组织网页）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T19:35:10Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `eba327c3c880ba59674e100936233d3bc2872ffa`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 3 段，8 文件 1844 行（OrganizationMerchantPage.tsx 480 / StaffPayrollLeavePage.tsx 331 / StaffProfilesPage.test.tsx 112 / StaffProfilesPage.tsx 212 / StaffShiftAttendancePage.batchshift.test.tsx 134 / .guidedcreate.test.tsx 107 / .settings.test.tsx 61 新增 / StaffShiftAttendancePage.tsx 407），main 主归属 M012、M057、M081、M290、M293；适用判据＝这些模块映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单 `unpublished-03acdb98-input.json` 逐文件核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离测试在 `/tmp/u-overlay`（发布基线 242f4872 `git archive` + 76 个未发布文件覆盖，76/76 哈希核对；node_modules 以符号链接只读复用）内进行，不触主仓。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U03-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4；M009-003 与 M012-006/007/008 改动了本段的 OrganizationMerchantPage.tsx，均已在 main）；U01/U02 已定稿 REVIEWED-FAIL（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U03-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：未发布副本内 `apps/web/src/admin/types.ts`（第 4 段）、`backend/.../governance/PlatformController.java`、`PlatformService.java`、`dto/MerchantSelfOperatedRequest.java`（第 8 段）、`backend/.../access/staff/StaffController.java`、`StaffService.java`、`StaffProfileRecord.java`、`StaffRegistrationService.java`（第 5/6 段）；main 当前同名文件（含已合并的 M009/M012 修复版）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 480 | 29799 | 54e39b868275943cd76987e8a6d93de88f13b3c93695593a861a0893d9b96503 | 全读 |
| apps/web/src/admin/pages/StaffPayrollLeavePage.tsx | 331 | 21079 | e0b6c7fc87b505f9835877224582a251330911064d145b7567adbb591e3d95ea | 全读 |
| apps/web/src/admin/pages/StaffProfilesPage.test.tsx | 112 | 5780 | f4e3a1fb971deece7874b14e168a26b0e3c5fd64994e123017d3b132ac50614e | 全读 |
| apps/web/src/admin/pages/StaffProfilesPage.tsx | 212 | 12098 | 4eab40e08aab79009a2a49f4392b13ca15acadccc2b4c3a2132006be619494d6 | 全读 |
| apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx | 134 | 7110 | 32abe8911e674954a0edb810283196e10175339023c4868de396618e7607d0db | 全读 |
| apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx | 107 | 5525 | f2b007727b00d34b11260df996976a3e5cd5cc16a7b00734434dfa575f8fece2 | 全读 |
| apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx | 61 | 3752 | 2068734c7a8760f49d16e69d8d7d466d4ebcb6eca743a96e9e68d623107e331a | 全读 |
| apps/web/src/admin/pages/StaffShiftAttendancePage.tsx | 407 | 26866 | 1629b00be3595d1373a8a25dc4e2d6ed504347b0c4c194b7e229e8d12dd6bed2 | 全读 |

## 2. 可执行证据原文（/tmp/u03_b1_run.log）

```
START=2026-09-09T19:35:09Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 480 | 29799 | 54e39b868275943cd76987e8a6d93de88f13b3c93695593a861a0893d9b96503
apps/web/src/admin/pages/StaffPayrollLeavePage.tsx | 331 | 21079 | e0b6c7fc87b505f9835877224582a251330911064d145b7567adbb591e3d95ea
apps/web/src/admin/pages/StaffProfilesPage.test.tsx | 112 | 5780 | f4e3a1fb971deece7874b14e168a26b0e3c5fd64994e123017d3b132ac50614e
apps/web/src/admin/pages/StaffProfilesPage.tsx | 212 | 12098 | 4eab40e08aab79009a2a49f4392b13ca15acadccc2b4c3a2132006be619494d6
apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx | 134 | 7110 | 32abe8911e674954a0edb810283196e10175339023c4868de396618e7607d0db
apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx | 107 | 5525 | f2b007727b00d34b11260df996976a3e5cd5cc16a7b00734434dfa575f8fece2
apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx | 61 | 3752 | 2068734c7a8760f49d16e69d8d7d466d4ebcb6eca743a96e9e68d623107e331a
apps/web/src/admin/pages/StaffShiftAttendancePage.tsx | 407 | 26866 | 1629b00be3595d1373a8a25dc4e2d6ed504347b0c4c194b7e229e8d12dd6bed2
=== manifest entries ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx 480 29799 54e39b868275943cd76987e8a6d93de88f13b3c93695593a861a0893d9b96503
apps/web/src/admin/pages/StaffPayrollLeavePage.tsx 331 21079 e0b6c7fc87b505f9835877224582a251330911064d145b7567adbb591e3d95ea
apps/web/src/admin/pages/StaffProfilesPage.test.tsx 112 5780 f4e3a1fb971deece7874b14e168a26b0e3c5fd64994e123017d3b132ac50614e
apps/web/src/admin/pages/StaffProfilesPage.tsx 212 12098 4eab40e08aab79009a2a49f4392b13ca15acadccc2b4c3a2132006be619494d6
apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx 134 7110 32abe8911e674954a0edb810283196e10175339023c4868de396618e7607d0db
apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx 107 5525 f2b007727b00d34b11260df996976a3e5cd5cc16a7b00734434dfa575f8fece2
apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx 61 3752 2068734c7a8760f49d16e69d8d7d466d4ebcb6eca743a96e9e68d623107e331a
apps/web/src/admin/pages/StaffShiftAttendancePage.tsx 407 26866 1629b00be3595d1373a8a25dc4e2d6ed504347b0c4c194b7e229e8d12dd6bed2
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment) ===
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 12 ++++
 apps/web/src/admin/pages/StaffPayrollLeavePage.tsx | 40 ++++++++++-
 .../web/src/admin/pages/StaffProfilesPage.test.tsx | 35 +++++++++
 apps/web/src/admin/pages/StaffProfilesPage.tsx     | 84 ++++++++++++++++++----
 .../StaffShiftAttendancePage.batchshift.test.tsx   |  7 +-
 .../StaffShiftAttendancePage.guidedcreate.test.tsx |  5 +-
 .../StaffShiftAttendancePage.settings.test.tsx     | 61 ++++++++++++++++
 .../src/admin/pages/StaffShiftAttendancePage.tsx   | 75 ++++++++++++++-----
 8 files changed, 285 insertions(+), 34 deletions(-)
=== main current vs unpublished copy (divergence) + main_vs_baseline blob ids ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx main_sha=95328007702f02c6c80f9c846eea72f80a94a5813f6b97349e8759964a4c1d95 unpublished_sha=54e39b868275943cd76987e8a6d93de88f13b3c93695593a861a0893d9b96503 diff_lines=130 baseline=432714076c head=7d5b3ee9c5 worktree=7d5b3ee9c5 main_unchanged_since_baseline=no worktree_dirty=no
apps/web/src/admin/pages/StaffPayrollLeavePage.tsx main_sha=4ff15043d47fc7f9a2c96255f5fd44ee1c72cf08a95dcfbbc7adf14a2396b7f6 unpublished_sha=e0b6c7fc87b505f9835877224582a251330911064d145b7567adbb591e3d95ea diff_lines=40 baseline=0ba58e7be0 head=0ba58e7be0 worktree=0ba58e7be0 main_unchanged_since_baseline=yes worktree_dirty=no
apps/web/src/admin/pages/StaffProfilesPage.test.tsx main_sha=826cf951905cc4cb9d4a09790da93c1383af78464ef677d4c99d590e8717ff0a unpublished_sha=f4e3a1fb971deece7874b14e168a26b0e3c5fd64994e123017d3b132ac50614e diff_lines=35 baseline=77e0954a0e head=77e0954a0e worktree=77e0954a0e main_unchanged_since_baseline=yes worktree_dirty=no
apps/web/src/admin/pages/StaffProfilesPage.tsx main_sha=873ae391a6085f5ab3f8f277c69c9cb7958fc05f1c0a6bcbab6c21f0fcbb68c9 unpublished_sha=4eab40e08aab79009a2a49f4392b13ca15acadccc2b4c3a2132006be619494d6 diff_lines=84 baseline=d8dedc7648 head=d8dedc7648 worktree=d8dedc7648 main_unchanged_since_baseline=yes worktree_dirty=no
apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx main_sha=c08fb0a2cc7161444717ad8137c6fcb51a32393a009f71d14e4b1e9c33fb7c39 unpublished_sha=32abe8911e674954a0edb810283196e10175339023c4868de396618e7607d0db diff_lines=7 baseline=691029a54d head=691029a54d worktree=691029a54d main_unchanged_since_baseline=yes worktree_dirty=no
apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx main_sha=8c40f1e96d798f437ab1f0902376e30dea42aaf3767a17e09e6d662b17f10359 unpublished_sha=f2b007727b00d34b11260df996976a3e5cd5cc16a7b00734434dfa575f8fece2 diff_lines=5 baseline=ab5aa0af46 head=ab5aa0af46 worktree=ab5aa0af46 main_unchanged_since_baseline=yes worktree_dirty=no
apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx main_exists=no
apps/web/src/admin/pages/StaffShiftAttendancePage.tsx main_sha=eab2aea0feca263eb6bdc947af61d5c8c8a388a3ba8238cbaeb42686efa2db4e unpublished_sha=1629b00be3595d1373a8a25dc4e2d6ed504347b0c4c194b7e229e8d12dd6bed2 diff_lines=75 baseline=4954b599a3 head=4954b599a3 worktree=4954b599a3 main_unchanged_since_baseline=yes worktree_dirty=no
=== main versions of the same files (文件版本 header) ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx main:V0.02 unpublished:V0.01
apps/web/src/admin/pages/StaffPayrollLeavePage.tsx main:none unpublished:V0.01
apps/web/src/admin/pages/StaffProfilesPage.test.tsx main:none unpublished:V0.02
apps/web/src/admin/pages/StaffProfilesPage.tsx main:V0.01 unpublished:V0.04
apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx main:none unpublished:V0.01
apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx main:none unpublished:V0.01
apps/web/src/admin/pages/StaffShiftAttendancePage.tsx main:none unpublished:V0.01
=== probes: self-operated request body vs backend DTO/controller (unpublished copy) ===
268:        { label: "自营商户", value: row.selfOperated ? "是（员工本人注册，经所属公司审批）" : "否（商户代提交，经域公司审批）" },
320:    if (!row.archivedAt && row.deleteStatus !== "pending") actions.push({ key: "self-operated", label: row.selfOperated ? "取消自营标记" : "标记为自营",
321:      permissions: ["merchant.profile.manage"], testId: `merchant-self-operated-${row.id}`, onClick: async () => {
325:          await api.post("/api/v1/platform/merchants/" + encodeURIComponent(row.id) + "/self-operated", { selfOperated: !row.selfOperated, reason: reason.trim() });
326:          setActionResult({ 结果: row.selfOperated ? "已取消自营标记" : "已标记为自营" });
5:86:    @PostMapping("/merchants/{merchant_id}/self-operated")
6-90:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
7-99:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
8-108:        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
package com.complex.platform.governance.dto;

// 文件版本：V0.01
public record MerchantSelfOperatedRequest(String idempotencyKey, Boolean selfOperated, String reason) {}
=== probes: cooperation forms in unpublished page vs main page ===
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:33:const COOPERATION_FORMS = ["fixed_rent_plus_commission", "revenue_share", "fixed_rent"];
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:465:              <label>合作形式<select data-testid="organization-merchant-cooperation"
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:34:// M009-003：合作方式六类（BEQR-148/BSM-028/PGM-026：自营/租赁/联营/代运营/服务商接入/其他）；代码与后端 Platfo
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:35:const COOPERATION_FORMS: ReadonlyArray<{ code: string; label: string }> = [
=== probes: employment states / phone column / master literal / include_archived ===
26:const EMPLOYMENT = { pending_entry: "待入职", employed: "在职", trial: "试岗", suspended: "停岗", departed: "离职" };
47:  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";
51:      const items = await api.get<StaffProfileRow[]>("/api/v1/staff/profiles?include_archived=true");
57:  async function changeArchive(row: StaffProfileRow, action: "archive" | "unarchive" | "delete-request" | "delete-approve") {
58:    if ((action === "archive" || action === "delete-request") && !reason.trim()) {
66:        action === "delete-approve" ? { decision: "approved", reason: reason.trim() } : { reason: reason.trim() });
68:        : action === "delete-request" && !isMaster ? "删除申请已提交，等待最高权限审批" : "员工档案已删除", 员工档案: row.id });
166:                  <td>{row.phone ?? "—"}</td>
185:                      data-testid={`staff-profile-delete-request-${row.id}`} disabled={submittingId === row.id}
186:                      onClick={() => void changeArchive(row, "delete-request")}>{isMaster ? "直接删除" : "申请删除"}</PermissionButton> : null}
188:                      data-testid={`staff-profile-delete-approve-${row.id}`} disabled={submittingId === row.id}
189:                      onClick={() => void changeArchive(row, "delete-approve")}>批准删除</PermissionButton> : <span>待最高权限审批</span> : null}
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:61:        return ApiResponse
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:70:        return ApiResponse
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:78:                .map(Staff
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:86:        return ApiResponse
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:91:        return ApiResponse
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffController.java:98:        return ApiResponse
=== probes: shift revision (createdBy/future/history/note) + backend requireReason ===
147:        shiftId: editingShift,
260:                  {row.createdBy === user.id && new Date(row.startAt ?? "").getTime() > Date.now() ? <button type="button" onClick={(event) => {
268:                      const entries = await api.get<Array<{ reviewId: string; before: ShiftRow | null; after: ShiftRow; appliedAt: string | null }>>("/api/v1/staff/shifts/" + encodeURIComponent(ro
313:            validate: () => (userId.trim() && start && end ? null : "请填全 员工/开始/结束时间"),
324:            description: "点位选填；修改排班必须填写原因，批准后生效。",
637:            throw ApiException.conflict("SHIFT_STARTED", "班次已经开始，不能改写排班；临时顶班须记录顶班安排");
641:            throw ApiException.forbidden("SHIFT_AUTHOR_REQUIRED", "排班修改须由原排班负责人办理，员工应先向负责人申请");
643:        if (previous != null) requireReason(request.note());
=== probes: penalty cap / visible days / leave text / method labels / English action keys ===
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:20:  description: "薪资四块:固定工资(员工档案)+提成(业务自动归集)+奖金/�
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:106:      setActionResult({ action: "payroll_base_saved", userId: baseUser.trim() });
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:128:    setActionResult({ action: "payroll_adjustment_added", adjType });
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:134:    setActionResult({ action: "payroll_month_closed", month, statements: result?.statements
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:140:    setActionResult({ action: "leave_reviewed", id, decision });
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:192:            const current = await api.get<{ value: number; minValue: number; maxValue: numb
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:210:              const review = await api.post<{ reviewId: string }>("/api/v1/staff/settings/p
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffPayrollLeavePage.tsx:212:                reason: capReason.trim(), parallelLines: Object.entries(capReviewers).map((
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffShiftAttendancePage.tsx:23:const METHOD_LABELS: Record<string, string> = { nfc: "NFC", qr: "扫码", manual: "历史
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffShiftAttendancePage.tsx:106:        { label: "方式", value: METHOD_LABELS[row.method ?? ""] ?? row.method ?? "—"
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffShiftAttendancePage.tsx:220:          const value = await api.get<{ value: number; minValue: number | null; maxValue
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffShiftAttendancePage.tsx:233:            await api.post("/api/v1/staff/settings/shift-visible-days", { value: Number(
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/StaffShiftAttendancePage.tsx:289:                  <td>{METHOD_LABELS[row.method ?? ""] ?? row.method ?? "—"}</td>
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:1433:                <li>新员工注册分两条路径：商户员工由商户代该员�
docs/business-system-map-for-operations-v0.1.html:1443:                <li>主管排班：排到几点几分，可指定当天工作点位�
docs/business-system-map-for-operations-v0.1.html:1444:                <li>员工看自己未来若干天的排班，可见天数由后台�
docs/business-system-map-for-operations-v0.1.html:1458:                <li>每位员工都有一个结构化的薪资配置：基础薪资�
docs/platform-governance-map-for-operations-v0.1.html:578:                <li>员工注册分两条路径：公司自有员工由本人在�
docs/platform-governance-map-for-operations-v0.1.html:871:                <li>员工日常界面只显示脱敏号码，不见明文；界�
docs/business-enum-quick-reference-v0.1.html:240:    <tr><td class="k">员工状态</td><td>待入职、在职、试岗、停岗、离职</t
docs/business-enum-quick-reference-v0.1.html:283:    <tr><td class="k">薪资单构成</td><td>每位员工都有一个结构化的薪资配
=== isolated vitest in /tmp/u-overlay (baseline + 76 unpublished files): segment test files + main's OrganizationMerchantPage.test against unpublished page ===
10: [32m✓[39m src/admin/pages/StaffProfilesPage.test.tsx [2m([22m[2m7 tests[22m[2m)[22m[32m 102[2mms[22m[39m
11: [32m✓[39m src/admin/pages/StaffShiftAttendancePage.settings.test.tsx [2m([22m[2m2 tests[22m[2m)[22m[32m 99[2mms[22m[39m
16: [32m✓[39m src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx [2m([22m[2m3 tests[22m[2m)[22m[33m 320[2mms[22m[39m
18: [32m✓[39m src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx [2m([22m[2m4 tests[22m[2m)[22m[33m 375[2mms[22m[39m
20:[2m Test Files [22m [1m[32m4 passed[39m[22m[90m (4)[39m
21:[2m      Tests [22m [1m[32m16 passed[39m[22m[90m (16)[39m
41:     [32m✓[39m 点“正常商户”数字 → URL 带 ?kpi=merchants&status=active，页面进入聚焦视图[32m 202[2mms[22m[39m
42:     [32m✓[39m 聚焦视图点「返回全部」→ 参数清除，恢复完整页面（三表+表单+KPI 卡都在）[32m 50[2mms[22m[39m
43:     [32m✓[39m 带参刷新/直链进入 → 直接呈现聚焦视图（参数从 URL 重建）[32m 15[2mms[22m[39m
44:     [32m✓[39m 浏览器后退（popstate）→ 退出聚焦视图回完整页面[32m 44[2mms[22m[39m
45:     [32m✓[39m 三表按后端真实字段渲染出真实值（字段错位时单元格是“-”，本用例必红）[32m 25[2mms[22m[39m
46:     [32m✓[39m KPI 子标签渲染为中文，不含英文[32m 18[2mms[22m[39m
47:[31m     [31m×[31m 引导式弹窗内商户类型/合作形式下拉显示中文，不含英文枚举[39m[32m 67[2mms[22m[39m
48:     [32m✓[39m 引导式创建流：分步校验拦截 → 确认页汇总 → 创建提交并关闭弹窗[32m 83[2mms[22m[39m
49:     [32m✓[39m 点经营主体行 → 抽屉含主体名/信用代码/角色中文[32m 22[2mms[22m[39m
50:     [32m✓[39m 点商户行 → 抽屉含商户名/联系人/合作形式中文[32m 24[2mms[22m[39m
51:     [32m✓[39m 点项目行 → 抽屉含项目名/编码；抽屉零英文枚举[32m 20[2mms[22m[39m
52:     [32m✓[39m Esc 关闭抽屉 + 点遮罩关闭[32m 33[2mms[22m[39m
54:[31m⎯⎯⎯⎯⎯⎯⎯[39m[1m[41m Failed Tests 1 [49m[22m[31m⎯⎯⎯⎯⎯⎯⎯[39m
56:[41m[1m FAIL [22m[49m src/admin/pages/OrganizationMerchantPage.mainhead.test.tsx[2m > [22mOrganizationMerchantPage KPI 聚焦视图（带参重进本页）[2m > [22m引导式弹窗内商户类型/合作形式下拉显示中文，不含英文枚举
73:[2m Test Files [22m [1m[31m1 failed[39m[22m[90m (1)[39m
74:[2m      Tests [22m [1m[31m1 failed[39m[22m[2m | [22m[1m[32m11 passed[39m[22m[90m (12)[39m
END=2026-09-09T19:35:10Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 3 段（8 文件 1844 行；`StaffShiftAttendancePage.settings.test.tsx` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 diffstat，共 +287/−34 含第 4 段 types.ts），main 当前版本（含已合并的 M009/M012 修复）仅作分叉对照。
- 第一性原理线：从 HTML 反推四张后台页应当承载什么——BEQR L240 员工状态五值、PGM L871 界面脱敏、BSM L1443 排班审批/修改追加留痕/开始锁定、BSM L1444 可见天数按经营主体可配且须同时配置上下限、BSM L1450 事假按工时比例扣薪零为下限、BSM L1458 各类扣除合计上限 20% 全平台可配且须人力/财务/法务三审加顶层平台最终确认、PGM L578/BSM L1433 商户档案「标注为自营」；逐一对照页面行为、请求体与副本后端契约。
- 顺从式线：21 项判据逐项（§5）。
- 对抗式线：新增写路径的请求体与副本后端 DTO/控制器是否对得上（自营标记、员工状态、存档/删除、可见天数、扣除上限、排班修改）；发布时是否覆盖 main 已修项（M009-003 六类合作方式、M012-006/007/008）；页面自述文案与实际校验是否一致；主权限硬编码；测试是否锁真实契约；隔离 vitest 是否真跑。
- 只读依赖追流（不审、不计入本段结论）：副本内 `types.ts`（第 4 段）、`PlatformController.java`/`PlatformService.java`/`dto/MerchantSelfOperatedRequest.java`（第 8 段）、`StaffController.java`/`StaffService.java`/`StaffProfileRecord.java`/`StaffRegistrationService.java`（第 5/6 段）；main 当前 `OrganizationMerchantPage.tsx`（M009-003 于 64b9cc0c，M012 修复经 PR #429 合并）与 `OrganizationMerchantPage.test.tsx`。
- 8 文件全文逐行读完（§1）；与 main 的逐文件 diff 逐块读完（§2 diffstat）。可执行验证：在 `/tmp/u-overlay`（`git archive 242f4872` + 76 个未发布文件覆盖，76/76 哈希核对，node_modules 符号链接只读复用，不触主仓）内跑本段 4 个测试文件，并把 main HEAD 的 `OrganizationMerchantPage.test.tsx` 放到副本页上跑一次作分叉证据（§2）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- `types.ts`（第 4 段）、`StaffProfilesPage.tsx`、`StaffShiftAttendancePage.tsx`、`StaffPayrollLeavePage.tsx` 及三份既有测试：main 自基线以来未改（§2 blob 对照 baseline=head）——快进合并，无冲突。
- `OrganizationMerchantPage.tsx`：未发布版 = 基线 + 自营标记 12 行（V0.01）；main 已含 M009-003（六类合作方式、`cooperationLabel`）与 M012-006/007/008（PR #429）（去预填/去隐藏字段、手机号非空、写路径 catch）。未发布版仍是三类旧合作方式、预填「二期测试商户/13800000000/1F-A/org-1/contract-subject-1」并夹带假证据附件号（`:68–73`、`:159–178`）。按副本直接发布会回退 main 已修项：main HEAD 测试在副本页上 1 失败/11 通过（失败用例＝六类合作方式中文，§2）；M012 的测试（空默认值/手机号非空/存档 catch，已在 main）亦会失败。→ CX-U03-002。

## 5. 21 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 五项新增能力均有 HTML 上位（员工状态五值 L240、存档/删除范式 BSM-003、可见天数 L1444、扣除上限 L1458、自营标注 L578/L1433、排班审批与修改历史 L1443） |
| GEN-002 可执行正确性 | 不通过 | 自营标记请求体缺 `idempotencyKey`，副本控制器 `requireIdempotencyKey` 必拒（CX-U03-001）；副本页发布会回退 M009/M012 修复并使 main 测试失败（CX-U03-002） |
| GEN-003 测试有效性 | 不通过（部分） | 新增用例锁真实端点与请求体（员工状态/存档/删除申请/可见天数/扣除上限/排班审批单）；但自营标记无任何用例，正是缺幂等键处（CX-U03-001）；隔离 vitest 本段 4 文件 16/16 绿（§2） |
| GEN-004 信任边界 | 通过 | 写路径均带事由或审批单；三条审批线最终审批人由后端校验（第 6 段） |
| GEN-005 授权 | 通过（登记） | 按钮级权限门（staff.profile.review/submit/approve、merchant.profile.manage、staff.payroll.manage）；`isMaster` 以字面 id/用户名硬编码（OBS-1，服务端 `isMasterAuthority` 兜底） |
| GEN-006 数据完整性 | 通过 | 存档先于删除、删除申请须事由、修改历史只读追加展示 |
| GEN-007 并发幂等 | 不通过 | 自营标记未传幂等键（CX-U03-001）；其余新增写路径由后端幂等服务承接 |
| GEN-008 接口一致性 | 不通过 | `{selfOperated, reason}` ↔ `MerchantSelfOperatedRequest(idempotencyKey, selfOperated, reason)`（CX-U03-001）；其余：员工状态五值与后端闭集一致、`include_archived` 与控制器一致、扣除上限单位（万分比）两端一致、排班 `shiftId`/`reviewId` 与后端一致 |
| GEN-009 错误处理 | 通过 | 新增写路径均 try/catch 显错（自营标记、存档、状态、上限、天数、修改历史） |
| GEN-010 安全 | 通过 | 手机号列由服务端 `maskedPersonalInfo()` 脱敏（§2 探针），页面不解密 |
| GEN-011 审计证据 | 通过 | 事由必填落后端审计（副本 `setEmploymentStatus`/`setMerchantSelfOperated` 记 reason） |
| GEN-012 资源 | 通过 | 设置类接口按需点击加载 |
| GEN-013 构建 | 通过（本段） | 隔离 vitest 4 文件 16/16 绿（§2）；tsc 见 U04 §2 |
| GEN-014 迁移 | 不适用 | 无迁移 |
| GEN-015 界面 | 通过（登记） | 新增文案全中文、结果键中文；薪酬页既有 `action: "payroll_base_saved"` 等英文结果键为 main 既有（OBS-2，归 M293） |
| GEN-016 文档 | 通过（登记） | 页描述与 HTML 口径一致；「首次配置须填写上下限」与校验「每次都须填」略有出入（OBS-3）；排班修改「必须填写原因」文案与前端校验不一致（OBS-4） |
| GEN-017 分类 | 通过 | 四分类见 §6；扣除上限三审并非添加（HTML L1458 原句） |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| BSM-001 整体说明 | 通过 | 商户档案「自营」标注可见，谁在经营可追溯 |
| BSM-003 通用能力 | 通过 | 存档/恢复/申请删除/批准删除齐全，操作有引导，改动须事由 |
| PGM-001 整体定位 | 通过（登记） | 新增文案零英文；既有英文结果键 OBS-2 |

## 6. 本次 Findings（临时 ID CX-U03-00N；正式 ID 由 B2 分配）

### CX-U03-001 · 技术性 · 发布前须修：是

- 位置：`OrganizationMerchantPage.tsx:320–329`（`api.post(".../self-operated", { selfOperated: !row.selfOperated, reason })`）。
- 事实：副本 `PlatformController.java:86–91` `requireIdempotencyKey(request.idempotencyKey())`；`MerchantSelfOperatedRequest(idempotencyKey, selfOperated, reason)`；`PlatformService.setMerchantSelfOperated` 以 `request.idempotencyKey()` 走 `executeWrite`。请求体无 `idempotencyKey` → 每次点击「标记为自营/取消自营标记」都被 400 拒绝，功能不可用；本段无该动作的测试。
- HTML：PGM L578/BSM L1433「在商户档案上标注为自营的商户视同公司自有」——标注能力本身有上位，缺的是执行正确性。
- 判据：GEN-002、GEN-007、GEN-008、GEN-003；依据类型：代码事实（HTML 无幂等键细节）。
- 最小反例：副本页点「标记为自营」→ 副本后端 `IDEMPOTENCY_KEY_REQUIRED`（或同类 400）。
- 无业务发明理由：补一个幂等键与同页其它写路径一致（`merchant-archive-…-${Date.now()}` 模式）。
- 工程缺陷：是。建议登记：是。最小处置：请求体加 `idempotencyKey`，并补一条用例锁请求体键。不在本轮修复。

### CX-U03-002 · 技术性（与 main 已修项冲突）· 发布前须修：是

- 位置：`OrganizationMerchantPage.tsx` 整文件（未发布版 V0.01：`:33` 三类旧合作方式；`:68–73` 预填「二期测试商户/运营联系人/13800000000/fixed_rent_plus_commission」；`:159–178` 创建体夹带 `operatorNote`「完整后台组织与商户档案验收创建」、`evidenceAttachmentIds: ["att-merchant-profile-create"]`、`businessEntityId: entities[0]?.id ?? "org-1"`、`contractEntityId: "contract-subject-1"`、`settlementEntityId: "settlement-subject-1"`、`mallBlockCode: "1F-A"`、面积/租金常量、`status: "active"`；`:153/:453` 手机号 11 位正则）。
- 事实：main 已含 M009-003 六类合作方式（64b9cc0c）与 M012 修复（006 去预填与隐藏字段、007 手机号非空、008 写路径 catch，操作员已答 006/007「删除」，PR #429），改的是同一文件同一区域。未发布版基于旧基线，按副本发布＝回退这些已定稿修复。证据：main HEAD 的 `OrganizationMerchantPage.test.tsx` 在副本页上 1 失败/11 通过（失败＝「合作形式下拉显示中文，不含英文枚举」）（§2）。
- 判据：GEN-002、GEN-003、GEN-006、GEN-017（M012-006 为操作员已裁决的删除项，副本把假值带回即违背裁决）；依据类型：main 已定稿项（findings M009-003、M012-006/007/008）。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：以 main 当前版为底做三方合并，只保留本段新增的自营标记 12 行（抽屉字段 + 行操作，并按 CX-U03-001 加幂等键）；不得整文件覆盖。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`StaffProfilesPage.tsx:47` 以字面 `usr_master_authority`/`master-authority` 判最高权限，仅决定按钮文案与提示；服务端 `isMasterAuthority` 兜底（副本 `StaffService` L387–392/L400–402）。建议随发布改读后端能力标志。
- OBS-2：薪酬页既有 `setActionResult({ action: "payroll_base_saved" … })` 等英文结果键（`StaffPayrollLeavePage.tsx:106/128/134/140`）为 main 既有，PGM-001 归 M293。
- OBS-3：可见天数 legend「首次配置须填写上下限」，校验对每次提交都要求三项齐全（`:231`）——比 HTML L1444「须同时配置上下限」更严，无冲突；文案可改。
- OBS-4：排班修改弹窗文案「修改排班必须填写原因，批准后生效」（`:324`），前端 `validate` 只校验员工/起止（`:313`），原因由后端 `requireReason(request.note())`（副本 `StaffService` L642）拦截并回显——功能正确、体验滞后。
- OBS-5：`shiftId` 为空时后端按（员工，开始时刻）匹配既有班次进入修改路径（副本 L633–635），前端「新增排班」若撞上同员工同时刻会被要求原因/作者校验；口径在后端，登记。
- OBS-6：扣除上限三名最终审批人以自由文本「员工账号编号」录入（`StaffPayrollLeavePage.tsx:202–204`），无选择器；后端校验（第 6 段）。
- OBS-7：`selfOperated` 与合作方式「自营」（六类之一）并存两种表示；副本 `StaffRegistrationService` L66/L91 只按 `merchant.selfOperated()` 路由注册——两者是否须联动无 HTML 规定，登记不发明。
- 技能：`using-superpowers`、`verification-before-completion`（vitest 实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。8 文件全文三线审计、21 项判据逐项核对；发现 2 项技术性须修（CX-U03-001 自营标记缺幂等键、CX-U03-002 发布回退 main 已修项）、7 项观察；无添加项（扣除上限三审为 HTML L1458 原句）；无 HTML 修改申请；隔离 vitest 本段 16/16 绿；未修源、未提交/推送。本报告落盘前未读取 `U03-b1-grok-raw.md` 任何正文。
---

# U03 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.48「当前 U03 B2-r1 整批候选」U03-001…015；modules/U03-b1-grok-raw.md V0.01（49874bytes SHA28dcd8efd729c7839e4cfd33e6c551df1645ffbcb08a3643d43727c6f8ebb1b5）；本文件 B1 段（35075bytes SHAba66667d5b5783337fd5cd10c0c6bb22e64d3c70d7687a6acfc9a7a3f2655650）。2026-09-09T19:53:32Z
- 席甲 B2 核实（只读，见 ledger V2.50 U03 节）：以 445a49f4 的 OrganizationMerchantPage.test.tsx 在 /tmp/u-overlay 副本页重跑 → 6 失败/9 通过（/tmp/u03_vitest_head445a.log）；GR-008/010/011/012/014 五处均在基线同位置存在且 diff 未触及（main 既有）；GR-009/013 为本材料新增行；main types.ts 无 selfOperated、main 无 /self-operated 路由。

## 勘误（席甲 B1 段）

- 本文件 §2/§4 所记「main 当前测试对副本页 1 失败/11 通过」是对 bd445728（PR #429 合并前）版本测试跑的；对 445a49f4 版本测试重跑为 6 失败/9 通过（15），与 Grok OBS-03 一致。定稿以 6/15 为准；B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 副本 PlatformController.java:86-91 requireIdempotencyKey 与请求体 :320-329 无幂等键均经全读核实；GR-007 测试缺口并入同 ID 不改原意；处置沿用同页既有幂等键模式，不发明业务 |
| 002 | PASS | 三码 vs 六类经 HTML BSM L701/PGM L495/L498 与 main COOPERATION_FORMS 核实；「冲突（main 已修 M009-003）」分类成立；自营布尔与合作方式「自营」关系只留观察，不替发布者定业务 |
| 003 | PASS | 与 M012-006 同文件同代码；操作员已答「删除」并已在 main 执行；不重复弹窗合规（同一内容不二问） |
| 004 | PASS | 与 M012-007 同文件同代码；同上 |
| 005 | PASS | main 已含 reportWriteFailure + 用例（M012-008）；以 main 为底即可 |
| 006 | PASS | 6/15 失败中 2 例即 merchant-archive testid 不在 DOM；技术性成立；处置两选一（保持 testid 可直查或同步改测试）最懒 |
| 007 | PASS | tsc TS2339×4 与路由缺失均有实跑/grep 证据；同批 U04/U08/U10 或先移除动作 |
| 008 | PASS | :51 固定 include_archived=true 为新增行；后端默认 false；处置默认 false + 勾选带参最小 |
| 009 | PASS | :260-264/:313/:324 新增代码；文案自述必填而不校验；处置补 validate 一行 + 一条可失败用例 |
| 010 | PASS | UTC slice 基线既有，diff 未触及 → main 既有归 M290/M293，须修否 |
| 011 | PASS | split(",") 基线既有 → M290 |
| 012 | PASS | user_id/space_id 标签基线既有 → M290/M293 |
| 013 | PASS | 无 TraceLink 基线既有；细化归 M057/M290/M293 |
| 014 | PASS | 奖惩/月结无 catch 基线既有 → M293 |
| 015 | PASS | 双方观察归并未改写原意；含 OBS-03 计数勘误与 OBS-07 两旋钮 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 001 处置补一句「POST 不断言把合作方式改成六类之一」（GR-007 原句，防与 OBS-07 混，不另开 ID）。
2. 002 处置后半（自营布尔与合作方式「自营」关系）保持 OBS，不读成冲突本体强制产品二选一。
3. 012 薪酬页 `:260` 与 `:253/:266` 同一条，不另开 ID。
4. 候选「共同处置」中 U08 只服务 002/006/007 的自营标注；人事七文件同批条件为 U05/U06 员工后端，不把 U08 写进人事七文件同批。
5. Grok B1 GR-008 未写「pre-existing」字样；B2 按函数体三方全等归 main 既有，与 U01 口径一致（候选 010 事实栏据此读作「B2 依三方全等判 main 既有」）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U03-001…015 成立；发布前须修「是」9 / 「否」6；添加 2 为操作员已答「删除」之同一内容，不再问；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 71357bytes SHA9dc5f9a13251836c63a1f8bbfb88dea9ba91f02c11735cb7c11188ddd54936af，前 49874bytes 原字节不变）。
