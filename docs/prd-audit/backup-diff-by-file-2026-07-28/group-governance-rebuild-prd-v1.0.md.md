## 文件｜`group-governance-rebuild-prd-v1.0.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/group-governance-rebuild-prd-v1.0.md`
- main：`docs/group-governance-rebuild-prd-v1.0.md`
- backup SHA-256：`e32f98b33b2ae20e8040eb548085a3a157a68143dc907aae75336de8f7fcf4aa`
- main SHA-256：`aade1ae6a91d7b5a1f3ad0489746ec8cd94dc1193a1f8886b64ec82c4f51751d`
- 大小：13179 → 12816 bytes

- 对齐单元数：备份 88 / main 90

- 差异块数：6
- 相似度（序列比）：0.9213

### 差异项 1｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - > 2026-07-04 逻辑一致性修订：①澄清角色分级——平台/集团级角色可跨项目公司下钻（requirePlatformScope 放行），项目公司管理员限本项目，商户用户 fail-closed 403，消除「集团下钻」与「项目公司级隔离」的表述冲突；②高危权限审批开关 `access.grant_approval.enabled` 据代码校正为**默认开**（原文「默认关」与代码不符）；③57 个高危权限码的 risk_level/requires_approval 改「后台可配、V170 预置不锁死」口径；④对标红点阈值据代码（V218）校正为「后台可配，默认 0.20；analytics_ai_thresholds」，非固定值。
  - > 生成：2026-06-27。依据：M1 需求采集 §7 + `platform-governance-map-for-operations-v0.1.html`「综合体和集团 / 权限规则」+ V169-V170 迁移 + `com.complex.platform.access` 本轮新增角色装配/授权审批 + `analytics/group-overview` 代码。本文向业务 HTML §十及治理 HTML 对齐；冲突以业务 HTML 为最终解释权，技术字段以真实 DB CHECK 和 Controller 为准。

**main 现行：**

  + > 2026-07-27 PRD 审计修订：①平台/集团级角色可按职位模板跨项目公司下钻，项目公司管理员限本项目，商户用户 fail-closed；②四类高危授权审批按治理 HTML 改为不可关闭的强制闸门，配置只能加严；③补授权申请人与审批人职责分离；④补岗位、职级、组织、商户或责任范围变化后立即重算并收回旧范围派生权限。当前实现若不满足这些规则，记为实现待对齐，不反向改写 PRD。
  + > 生成：2026-06-27。业务规则以三份业务 HTML 为最终解释权；迁移、代码和接口只作为当前实现证据，不得反向改写业务规则。本文承接治理 HTML「综合体和集团 / 权限规则」及枚举 HTML §九，M1 需求、V169-V170 和当前代码仅用于说明追溯与实现状态。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 高危权限审批开关字段（后台可配，逐权限码可改）；V170 预置把一批高危权限置为 `true`，数值可后台调整 |

**main 现行：**

  + | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 实现字段；四类 HTML 强制高危权限必须为 `true`，后台只能新增高危项或提高审批等级，不能关闭强制项 |

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - - 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；如果开启授权审批开关，含 `requires_approval=true` 权限的角色装配会被 `GRANT_REQUIRES_APPROVAL` 拦截，必须走 §3 申请。

**main 现行：**

  + - 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；角色装配只要包含四类强制高危权限或其他 `requires_approval=true` 权限，就必须被审批闸拦截并走 §3 申请，不受可关闭总开关影响。

### 差异项 4｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - - 审批总开关：`access.grant_approval.enabled` **默认开（`:true`，以代码 `AccessService` 默认值为准）**，后台可配。开启时含 `requires_approval=true` 权限的直接授权会被 `GRANT_REQUIRES_APPROVAL` 阻断，高危权限必须先走授权申请（§3 流程）；若后台关闭，直接授权仍按提权闸和跨组织闸执行，但不再因 `requires_approval=true` 自动阻断。（注：历史文档曾记「默认关」，与当前代码不符，2026-07-04 据代码校正。）
  - - V170 预置高危权限审批字段：按迁移 `high_risk` 清单（四类，预置 57 个权限码）对资金/核销/退款/调账类、提权或跨商户跨项目公司类、敏感数据类、高危业务操作类权限批量设置 `requires_approval=true`。**该清单为 P0 预置基线、非锁死：`permissions.risk_level` 与 `requires_approval` 均为后台可配字段，可逐权限码增删改（配置表 `permissions`）；V170 只给初值，不硬编码为不可变策略。**清单初值以 V170 迁移为准，不从业务说明另造权限数量。

**main 现行：**

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-16，本文不再重复记载。
  + - 高危权限配置：迁移清单只是当前实现初值。后台可以新增高危权限、提高风险等级或提高审批层级，但不得把 HTML 明确的四类强制项降级为无需审批，也不得移出强制范围。

### 差异项 5｜仅 main 有（main 新增）｜共 1 条

  + - 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。

### 差异项 6｜仅 main 有（main 新增）｜共 1 条

  + - 岗位、职级、所属组织、所属商户或责任范围变化时，立即按新归属重新计算有效权限；旧岗位和旧范围派生权限同步失效，员工单独授权若超出新范围则暂停并重新审批，不允许只提示人工收回。

**本文件小计：** replace 块 4；仅备份条数 0；仅 main 条数 2；差异块 6

---

