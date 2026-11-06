# 闭环当前态快照 V0.1

> 本文由 `tools/generate-closed-loop-current-state.mjs` 从需求矩阵、代码行数、安全扫描、生产证据提交状态和 NAS 当前漂移证据生成。它给出联调、业务验收、生产上线就绪三项判断（见第 1 节），不替代真实测试命令输出。

机器识别标记：

- CLOSED_LOOP_CURRENT_STATE_SNAPSHOT_READY
- SNAPSHOT_FROM_REQUIREMENTS_CODE_MATRIX
- SNAPSHOT_FROM_SEMGREP_LATEST_SUMMARY
- SNAPSHOT_FROM_PRODUCTION_EVIDENCE_STATUS
- SNAPSHOT_FROM_NAS_CURRENT_STATE_DRIFT
- SNAPSHOT_FROM_NAS_DIAGNOSTIC_REPORT
- SNAPSHOT_FROM_RUNTIME_ACCEPTANCE_EVIDENCE
- SNAPSHOT_FROM_PARTIAL_REQUIREMENT_MOCK_COVERAGE
- SNAPSHOT_FROM_PRODUCTION_OWNER_ACTION_MANIFEST_LINKS
- OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS
- NO_PRODUCTION_READY_CLAIM_WITH_OPEN_MANIFESTS

## 1. Go / No-Go

| 判断项 | 当前值 |
|---|---|
| 可以进入联调 | 否 |
| 可以进入业务验收 | 否 |
| 可以宣称生产上线就绪 | 否 |

## 2. 需求矩阵当前态

| 矩阵 | 总数 | 完整/已实现 | 缺口 |
|---|---:|---:|---:|
| `docs/requirements-code-implementation-matrix-v0.1.csv` | 97 | 88 | 9 |
| `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv` | 9 | 9 | 0 |
| `docs/full-admin-remaining-requirements-code-matrix-v0.1.csv` | 22 | 22 | 0 |

## 3. 架构瘦身行数快照

| 文件 | 当前行数 |
|---|---:|
| `apps/web/src/main.tsx` | 50 |
| `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java` | 1200 |
| `backend/src/main/java/com/complex/platform/bar/BarService.java` | 1136 |
| `backend/src/main/java/com/complex/platform/bar/BarOrderService.java` | 1848 |
| `backend/src/main/java/com/complex/platform/bar/BarBillingService.java` | 1056 |
| `backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java` | 828 |

## 4. 安全与生产证据闸口

| 闸口 | 当前态 |
|---|---|
| Semgrep | results=0，errors=0，paths_scanned=1984，scan_dir=`security-scans/semgrep-20260703-delivery-gates-clean` |
| 运行时验收证据 | backend=6509 tests / failures=0 / errors=0，web_unit=1635，ui=47，real_backend_ui=3，ready=false |
| 生产证据提交 | expected=15，awaiting=15，valid=0，invalid=0 |
| Production blocker register | readiness=6, credential=4, findings=2, blocking_total=12, complete=false |
| P0 生产证据 | expected=13，awaiting=13，valid=0，invalid=0 |
| NAS 当前状态 | live_status=failed，can_close_sec_p0_ops_recovery=false |
| NAS 只读诊断 | ping=reachable，tcp445=reachable，cifs_mount=inactive，preflight=failed |
| NAS 诊断根因 | NAS host and SMB port are reachable, mount.cifs exists, but the expected CIFS mount is inactive and the current user has no passwordless sudo; IT must execute the controlled mount handoff |
| 外部证据 | partial=9，complete=false |
| Owner action manifest links | blockers=12, actions=15, related_submissions=15, items_without_manifest=0, complete=true |

## 5. 不可宣称生产上线就绪的原因

- 15 production evidence submissions awaiting, including 13 P0
- NAS current state is failed; SEC-P0-OPS-RECOVERY-001 cannot close
- 12 production blocker register items remain open (6 readiness, 4 credential, 2 security findings)
- 9 external evidence or supplier-backed requirements remain partial


## 6. 下一步可执行阻断明细

这一节只汇总“现在该找谁、补什么”，不把外部证据伪装成已完成。完整清单以 `production-evidence-backfill-execution-ledger-latest.json`、`production-blocker-register-latest.json` 和 `external-evidence-requirement-traceability-latest.json` 为准。

### 6.1 P0 生产证据待回填（前 12 项）

待回填总数：13。

| item_id | owner | category | manifest_path |
| --- | --- | --- | --- |
| PRD-160-001 | IT/运维/安全 | ops160 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json |
| PRD-CRED-001 | IT/运维/安全 | credential | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json |
| PRD-CRED-DB-001 | IT/运维/安全 | credential | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json |
| PRD-CRED-JWT-001 | IT/运维/安全 | credential | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |
| PRD-CRED-NAS-001 | IT/运维/安全 | credential | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json |
| PRD-CRED-OPS-001 | IT/运维/安全 | credential | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json |
| PRD-NAS-ALERT-001 | IT/运维/安全 | nas | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json |
| PRD-NAS-AUTOMOUNT-001 | IT/运维/安全 | nas | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json |
| PRD-NAS-DRILL-CADENCE-001 | IT/运维/安全 | nas | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json |
| PRD-NAS-SCHEDULE-001 | IT/运维/安全 | nas | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json |
| SEC-P0-OPS-RECOVERY-001 | IT/运维/安全 | ops160 | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json |
| PRD-CRED-JWT-001 | 技术实现负责人 | credential | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |

### 6.2 生产前置台账阻断项

| item_id | owner | status | blocking_scope | next_action |
| --- | --- | --- | --- | --- |
| PRD-NAS-AUTOMOUNT-001 | IT/运维 | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | IT 安装后回填重启后 findmnt、读写删除测试和截图 |
| PRD-NAS-SCHEDULE-001 | IT/运维 | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 |
| PRD-NAS-ALERT-001 | IT/运维/安全 | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | 回填容量告警、失败告警和通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | IT/运维 | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | 回填最近一次恢复演练报告、临时库清理和验证表 |
| PRD-CRED-001 | IT/安全/财务 | pending_info | 阻塞长期预生产和生产 | 建立密钥引用、轮换周期、责任人和应急回滚 |
| PRD-160-001 | IT/运维 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | 迁移用户级覆盖到系统配置，接入 systemd 和告警 |

### 6.3 生产凭证轮换阻断项

| credential_item_id | owner | status | blocking_scope | next_action |
| --- | --- | --- | --- | --- |
| PRD-CRED-OPS-001 | IT/运维/安全 | pending_info | 阻塞长期预生产和生产 | 建立账号负责人、授权范围、到期日和停用步骤 |
| PRD-CRED-DB-001 | IT/运维/安全 | pending_info | 阻塞生产数据库接入 | 既成事实（2026-07-06 prod 切换后）：运行时 app 角色 complex_app 已轮换 55 位强密码且被 REVOKE 最小权限；owner/Flyway 超级角色 complex 密码仍为 dev 默认 complex_dev_pwd，运行时以 COMPLEX_DATABASE_CREDENTIAL_SEPARATION_ENFORCED=false 关闭凭证守卫掩盖此状态。待 IT 为 owner ALTER ROLE 生成强密码、同步 owner env 并重启，随后移除 enforced=false 让守卫 fail-closed 生效；补到期日和维护窗口 |
| PRD-CRED-JWT-001 | IT/安全/技术负责人 | pending_info | 阻塞生产登录和开放接口签发 | 设计签名版本、双验窗口、旧 token 失效计划 |
| PRD-CRED-NAS-001 | IT/运维/安全 | pending_info | 阻塞生产可恢复结论 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 |

### 6.4 安全 findings 阻断项

| finding_id | severity | owner | status | blocking_scope | next_action |
| --- | --- | --- | --- | --- | --- |
| SEC-P0-OPS-RECOVERY-001 | P0 | IT/安全/运维 | open | 阻塞公开生产安全准入；不阻塞开发联调 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 |
| SEC-P2-CSRF-API-001 | P2 | 后端/前端/安全 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 |

### 6.5 NAS 当前态下一步

- root_cause：NAS host and SMB port are reachable, mount.cifs exists, but the expected CIFS mount is inactive and the current user has no passwordless sudo; IT must execute the controlled mount handoff
- next_action：IT must use an approved sudo/root maintenance window to run the controlled mount handoff, then regenerate NAS drift and diagnostic reports

## 7. 仍需外部证据或供应商资料的需求

| 需求编号 | 模块 | 状态 | 缺口 |
|---|---|---|---|
| # 总体标注(2026-06-22) | 全部模块 | 以文档/迁移/代码为准(总体标注) | 见各行md文档/迁移 |
| REQ-17 | 酒吧 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-21 | 酒吧 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-23b | 酒吧 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-25 | 酒吧 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-32 | 酒吧 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-40 | 第三方 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-41 | 第三方 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
| REQ-44 | 第三方 | 部分实现 | 真实外部资料/生产证据可能未全部到位；当前以 mock、adapter、Runbook、blocker register 或沙箱验收，不得伪装生产闭环。 |
