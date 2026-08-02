## 文件｜`requirement-traceability-matrix-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/requirement-traceability-matrix-v0.1.md`
- main：`docs/requirement-traceability-matrix-v0.1.md`
- backup SHA-256：`82e66bd83df349cecd9bf424d01d6f28ce71dc5501799f844a456d2d36cbc1a9`
- main SHA-256：`a99e8855bd1716681c2e15f30ffe7d331afa227e33dd21b1c179638bb462e7e7`
- 大小：100879 → 99515 bytes

- 对齐单元数：备份 269 / main 269

- 差异块数：6
- 相似度（序列比）：0.9777

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > - **判定口径不变**：实现以 `backend/src/main/resources/db/migration/` + 代码为准；本横幅上修项已与 `matrix-migration-claims.json`（已置 present）一致。下方行内 R5 旧描述若与本横幅冲突，以本 R7 横幅为准。

**main 现行：**

  + > - **实现证据判定不变**：仅判断“当前是否已实现”时，以 `backend/src/main/resources/db/migration/` + 代码为证据；业务规则仍以三份业务 HTML 为最终解释权，代码不能反向覆盖。下方行内 R5 旧实现描述若与本 R7 横幅冲突，以本 R7 横幅为准。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > - **REQ-35/42/68 留痕与准入权威事件（2026-07-03 用户裁决：文档对齐代码，非漂移）**：准入闸事件代码有意收口到 admission 域——发 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`（区分放行/驳回/告警/升级，较原 `contract.blocked`/`contract.released` 更细，`SupervisorReviewService` R5 T3 收口有据）；订单销售归属以字段 `service_responsibility_records.sales_staff_id`（V059）记录、可查询，属「记录型」留痕而非「事件型」，不发 `order.acquired`/`order.sales_attributed` 事件。→ **权威口径以代码实际为准，原「事件名漂移」结论作废。**

**main 现行：**

  + > - **REQ-35/42/68 留痕与准入实现事件名（2026-07-03 实现映射）**：业务 HTML 只要求阻断、放行、告警、升级和责任链可追溯，不规定技术事件码。当前实现使用 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`；订单销售归属以 `service_responsibility_records.sales_staff_id` 记录。上述代码和字段仅是实现映射，不反向解释业务规则。

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应；口径冲突一律以 `unified-standards-baseline-v0.1.md`（统一口径基准 D1–D13）与 `business-decision-log-v0.1.md` §32（2026-06-07 A1/A5/A6/A9 拍板）为准。

**main 现行：**

  + > **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应。原始需求、统一口径基准、决策日志和本矩阵均为追溯材料；业务冲突一律以三份业务 HTML 为最终解释权。

### 差异项 4｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`** + 4.1 准入事件 | 已覆盖（准入闸 V068 已落地）·事件名已对齐（有意 admission.* 收口） | R7 上修（2026-06-19，核验迁移/代码）：准入卡控框架已落（V068）——`AdmissionGuard` 经 `ActionGateAspect` 在带 `@GuardedAction(admissionAction=...)` 的业务方法执行前读 `supervisor_review_policies` 判定准入（三态 allow/warn/block），业务动作内嵌校验、卡控可触发并发 `admission.blocked/released/warned/escalated`；统一走 supervisor_review，admission_audits/contract_admission_rules 不建。**已对齐（2026-07-03 文档对齐代码）**：准入闸事件有意收口为 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`（区分放行/驳回/告警/升级，较原 `contract.blocked`/`contract.released` 更细，收口有据），权威口径改以代码 admission.* 为准；原「事件名漂移待对齐」结论作废。 |

**main 现行：**

  + | REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`** + 4.1 准入事件 | 已覆盖（准入闸 V068 已落地）·实现事件名使用 admission.* | 业务规则以 HTML 为准：准入阻断、升级、放行和结果必须留痕。当前实现以 `admission.blocked/released/warned/escalated` 承接；事件码只说明实现映射，不是业务事实源。 |

### 差异项 5｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-112 | **高危授权审批：requires_approval 激活、四类高危、SupervisorReview 三档审批、临时权限到期和智能总结** | group-governance-rebuild PRD §3；治理 HTML「临时权限/高危授权审批」；迁移 `V170__access_grant_requests.sql`；UI `ADM-ACC-003` / `UI-ADM-ACC-003`；E2E `AC-AUTH-06` | 已覆盖（V170 已落地，审批自动拦截开关默认关；P0 矩阵已补 UI/验收编号） | 新表 `access_grant_requests`；CHECK：`request_type`=`user_role/user_position/temporary_permission/role_permission`（服务当前支持前三类），`status`=`pending/applied/rejected`。端点：`POST/GET /api/v1/access/grant-requests`、`POST /grant-requests/{id}/review`、`GET /grant-requests/summary`、`POST /temporary-grants`、`POST /temporary-grants/{id}/revoke`。权限 `access.grant_request.view/apply/review`；配置 `access.grant_approval.enabled=false` 默认关；风险等级映射 `critical→permission_grant_l3/top_authority`、`high→l2/two_levels_up`、其他→`l1/one_level_up`；临时权限到期失效由 `AC-AUTH-06` 验收。 |

**main 现行：**

  + | REQ-112 | **高危授权审批：四类高危授权先审批后生效、主管三档审批、临时权限到期和智能总结** | group-governance-rebuild PRD §3；治理 HTML「临时权限/高危授权审批」；UI `ADM-ACC-003` / `UI-ADM-ACC-003`；E2E `AC-AUTH-06` | 业务规则已覆盖；当前实现如允许关闭四类高危审批，则为实现待对齐 | 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务四类授权必须先审批才生效，配置只能新增高危项或提高审批等级，不得关闭这四类强制闸门；申请人不得审批自己的授权申请。临时权限到期或单次使用后立即失效，授予、收回、到期、消耗和失效均留痕。 |

### 差异项 6｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | unified-standards-baseline **D5**；operation-traceability PRD §5.2/§6.3；迁移 `V068` | 已覆盖 | 10 端权威表 terminal_type；govfam-3 跨文档枚举对齐以 D5 为准。R7 补（2026-06-19）：scope 7 维已落（V068 `terminal_capability_switches.scope_type` CHECK：global/project/merchant/business_system/module/device/version），`TerminalSwitchGuard` 按 device>version>module>business_system>merchant>project>global 解析，不再收窄 global/device。 |

**main 现行：**

  + | REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | 治理 HTML「终端开关」；operation-traceability PRD §5.2/§6.3；统一口径基准 D5 为下游编码映射 | 已覆盖 | 10 端业务全集以治理 HTML 为最终解释权；D5 和 `terminal_type` 仅承接编码映射。商户侧 Web 门户与商户侧 App 是两个独立终端，不得合并计数。 |

**本文件小计：** replace 块 6；仅备份条数 0；仅 main 条数 0；差异块 6

---

