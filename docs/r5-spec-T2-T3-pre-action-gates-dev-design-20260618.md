# T2 + T3 动作前置闸口链 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T2（能力开关 7 维 + 后端拒绝拦截器）** 与 **T3（合同准入校验共享组件 + supervisor_review 接线）**。两者是同一条"业务动作前置闸口链"，合并设计以共用拦截框架。
> **裁决依据**：裁决⑥（终端开关后端拒绝铁律 P0、scope 7 维）；裁决⑧（准入统一走 supervisor_review，不建 admission_audits/contract_admission_rules，业务动作内嵌准入校验 P0）。
> **现状基准**（已核代码）：`terminal_capability_switches`(V007，`scope_type` 为无 CHECK 自由列，唯一键含 4 列) 仅 kiosk 一处调用 `findCapabilitySwitch`，解析只产 global/device；`SupervisorReviewService.evaluate/approve/listPolicies` 已在，但**策略硬编码 `List.of`**、业务域零调用 evaluate；`AuthorizationService` 已有 `requirePermission/currentActorLevelRank/levelRankOf/currentMerchantScope`；现仅有 `BearerAuthenticationFilter`，**无 HandlerInterceptor/Aspect**。
> **迁移取号**：T1 占 V066 则本规格取 **V067**（连续，实际以合并顺序为准）；测试用 `MigrationVersions.latest()`。

---

## 0. 统一动作前置闸口链（T2/T3 共用框架）

业务接口在执行前依次过三道闸（任一不过即拒绝并留痕）：

```
请求 → [认证 Filter(已有)] → 闸口链：
  ① 终端开关闸（T2）：该端/模块 capability 是否开启 → 否则 403 TERMINAL_DISABLED + terminal.access_rejected
  ② 合同准入闸（T3）：该业务动作是否被合同/基础定义阻断 → 阻断则 409 ADMISSION_BLOCKED + 建复核
  ③ 权限/职级闸（已有）：AuthorizationService.requirePermission + DENY 优先
→ 业务逻辑
```

**实现方式**：注解驱动 + Spring AOP 切面（不改各 Controller 主体）。
```java
@Retention(RUNTIME) @Target(METHOD)
public @interface GuardedAction {
    String capability() default "";       // T2：能力开关 capability_code，空=不校验开关
    String admissionAction() default "";  // T3：准入 action_type，空=不校验准入
    String permission() default "";       // 既有：权限点，空=不校验权限
    String objectType() default "";       // T3：准入对象类型
}

@Aspect @Component @Order(10)
class ActionGateAspect {
  @Before("@annotation(g)")
  void gate(JoinPoint jp, GuardedAction g) {
    if (!g.capability().isEmpty())     terminalSwitchGuard.check(g.capability());      // ①
    if (!g.admissionAction().isEmpty()) admissionGuard.check(g.admissionAction(), g.objectType(), resolveObjectId(jp)); // ②
    if (!g.permission().isEmpty())     authorizationService.requirePermission(g.permission());                          // ③
  }
}
```
切面 `@Order` 早于业务事务；`resolveObjectId` 从入参解析（约定 `@AdmissionObjectId` 标参或 request.getObjectId()）。

---

## 一、T2：能力开关 7 维 + 后端拒绝拦截器

### 1.1 数据模型（不加列，扩 scope_type 枚举 + 加 CHECK）
```sql
-- V067（节选）：约束既有列取值，不改表结构
ALTER TABLE terminal_capability_switches
  ADD CONSTRAINT chk_tcs_scope_type CHECK (scope_type IN
    ('global','project','merchant','business_system','module','device','version'));
ALTER TABLE terminal_capability_switches
  ADD CONSTRAINT chk_tcs_terminal_type CHECK (terminal_type IN
    ('customer_app','customer_miniapp','web_admin','mobile_admin','staff_tablet',
     'staff_work_phone','staff_personal_app','merchant_web','merchant_app','kiosk','*'));
```
（`terminal_type='*'` 表示跨端能力；既有 UNIQUE(terminal_type,capability_code,scope_type,scope_id) 保留。）

### 1.2 求值口径（取最严，default-open，fail-closed）

> **⚠️ 2026-07-22 R-10 改口径（用户裁决）**：本节原写"**最具体者胜**"，实现照此落地后出现
> "global=关 + device=开 → 放行"，即**低层打开压过高层关闭**，安全方向反了（v7 审计定 P0）。
> 现改为**取最严**。下文为现行口径，旧的"最具体者胜"仅在"没有任何层关闭"时残留生效。

对一次请求按 (terminal_type, capability_code) 取候选开关行，层级从粗到细：
`global > project > merchant > business_system > module > version/device > 灰度`
（代码里的 `orderedScopes` 仍按"最细在前"排列：`device > version > module > business_system > merchant > project > global`，只是遍历方向不同。）

- **任一层显式 `enabled=false` → 最终关闭**（高层关闭压倒低层打开）；返回其中**最高层**的关闭行，作为 `terminal.access_rejected` 的 `scope_type/scope_id` 审计口径。
- 命中层**全部为开** → 返回**最具体**的那条开启行（保留调用方 `map(enabled).orElse(default)` 语义）。
- 无任何行 → **默认开启**（开关为 opt-out 关停语义）。
- **同层内**：`terminal_type` 专端行优先于跨端通配 `'*'` 行（通配/专端是另一坐标轴、不是层级，**不**纳入取最严；否则关掉 `'*'` 后就无法为单端单独放开）。
- **灰度层（V342）**是独立的最细一档，串在七层之后求值，只能收窄不能放宽：无 active 试点=弃权；有 active 试点时白名单内不关、**白名单外一律关**（`reason=canary_not_whitelisted`，`scope_type=canary`）。
- 开关服务/DB 读取异常 → **fail-closed 拒绝**（§5.6 兜底），写 `terminal.access_rejected(reason=switch_fetch_failed)`；灰度读取异常同理（`reason=canary_fetch_failed`）。
- scope_id 取值：project=project_id、merchant=merchant_id、device=device_id、module/business_system=模块码、version=客户端版本号（均来自鉴权上下文/请求头）。
- 查询实现：由"逐层最多 7 次查询、命中即返回"改为**一次查询取回本请求各层的显式行**再在内存里定序判定（看全部层也不会更慢）。

### 1.3 拦截器/Guard
`TerminalSwitchGuard.check(capabilityCode)`：从 `SecurityContext` 取 terminal_type + scope 上下文 → `repository.resolveEffectiveSwitch(...)`（新增方法，按优先级查询）→ 关闭则 `throw ApiException.forbidden("TERMINAL_DISABLED", ...)` 并发事件。
- 复用既有 `findCapabilitySwitch`，新增 `resolveEffectiveSwitch` 按 1.2 取最严返回生效行；灰度层由 `TerminalCanaryRepository.evaluate(...)` 在其后串联（R-10，V342）。
- 被拒留痕：`audit_events` 写 `terminal.access_rejected`，payload 含 terminal_type/capability/scope/reason（修正现状——不再只在 kiosk 凭证失败时触发）。

### 1.4 埋点（业务接口加注解）
finance/bar/ticketing/amusement/inventory 关键写接口加 `@GuardedAction(capability="<域.能力>")`，例：
`@GuardedAction(capability="finance.daily_close.lock", permission="finance.daily_close.manage")`。
能力码建议与权限点同前缀、便于配置。

### 1.5 事件 / 验收
- 事件：`terminal.access_rejected`（业务接口被拒，含 reason: disabled / switch_fetch_failed）。
- 验收（GWT）：Given 某端某模块开关 enabled=false，When 调其业务接口，Then 后端返回 TERMINAL_DISABLED 且写 terminal.access_rejected；Given 开关读取失败，Then fail-closed 拒绝；Given 无开关行，Then 放行（默认开启）；Given device 级关、global 级开，Then device 最具体胜=拒绝。

---

## 二、T3：合同准入校验共享组件（统一走 supervisor_review）

### 2.1 数据模型（裁决：不建 admission_audits/contract_admission_rules；外部化硬编码策略）
复用 `supervisor_review_decisions`（V050/V057）承载阻断/审批/放行留痕；新增**策略表**替换 `SupervisorReviewService` 里硬编码的 `List.of`：
```sql
-- V067（节选）
CREATE TABLE IF NOT EXISTS supervisor_review_policies (
    policy_code   VARCHAR(100) PRIMARY KEY,
    action_type   VARCHAR(100) NOT NULL,     -- 业务动作（与 @GuardedAction.admissionAction 对应）
    object_type   VARCHAR(100) NOT NULL,
    trigger_mode  VARCHAR(32)  NOT NULL,      -- 触发判定：always / over_contract_scope / over_space / expired / qualification_abnormal / permission_insufficient
    outcome       VARCHAR(16)  NOT NULL,      -- 三态：allow / warn / block
    approval_level VARCHAR(32) NOT NULL,      -- one_level_up / two_levels_up / top_authority / specific_role
    required_level INT NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_srp_trigger CHECK (trigger_mode IN ('always','over_contract_scope','over_space','expired','qualification_abnormal','permission_insufficient')),
    CONSTRAINT chk_srp_outcome CHECK (outcome IN ('allow','warn','block')),
    CONSTRAINT chk_srp_level CHECK (approval_level IN ('one_level_up','two_levels_up','top_authority','specific_role'))
);
```
> **三态收口**：补 `outcome` 的 `warn` 态，修正现状"仅阻断/放行两态、预警缺失"。`warn` = 放行但留痕告警；`block` = 阻断进审批；`allow` = 直接放行。

### 2.2 准入共享组件
`AdmissionGuard.check(actionType, objectType, objectId)`：
1. 按 (actionType, objectType) 查 `supervisor_review_policies`（enabled）→ 无策略=allow 放行。
2. 按 `trigger_mode` 判定是否命中（超合同范围/超空间/过期/资质异常/权限不足；判定数据来自 contracts/spaces/资质字段）。
3. `outcome=allow` → 放行；`warn` → 放行 + 写 supervisor_review_decisions(blocked=false, review_status=warned)；`block` → 调既有 `SupervisorReviewService.evaluate(...)` 建 pending 复核（blocked=true，按 approval_level + initiatorRank 算 required_approver_rank）→ `throw ApiException("ADMISSION_BLOCKED", reviewId)`。
4. 升级放行：沿用既有 `SupervisorReviewService.approve(reviewId, ...)`（已实现 reviewer_level/rank 校验）；放行后业务方重试动作。

> 准入策略从表读取（替换 `List.of`）；`SupervisorReviewService.listPolicies()` 改为读 `supervisor_review_policies`。

### 2.3 埋点（裁决：票务/酒吧/游乐/结算业务动作前内嵌）
在四域关键动作方法加 `@GuardedAction(admissionAction="<动作>", objectType="<对象>")`，例：
- 票务发布/核销特殊放行：`admissionAction="ticketing.special_release"`
- 酒吧免单/调账：`admissionAction="bar.bill_waiver"`
- 游乐恢复营业：`admissionAction="amusement.reopen"`
- 结算补录：`admissionAction="finance.locked_backfill"`

### 2.4 事件名收口（修 REQ-42/68 死名）
统一发 `admission.blocked` / `admission.released` / `admission.warned` / `admission.escalated`（替代代码零命中的 `contract.blocked`/`contract.released`）。**矩阵 REQ-42/68/69 的事件名与 `admission_audits.approval_level` 落点应同步改向 supervisor_review + 本组事件名**（claims.json REQ-42 项更新）。

### 2.5 验收（GWT）
- Given 某动作命中 block 策略，When 业务调用，Then 被拒(ADMISSION_BLOCKED)+建 pending 复核+发 admission.blocked。
- Given pending 复核，When 足够职级 approve，Then 放行+发 admission.released，业务可重试成功。
- Given warn 策略，Then 放行但写 warned 留痕+发 admission.warned。
- Given 策略表为空/无匹配，Then 放行（不误伤）。
- Given 发起人职级未知(rank=0)，Then 回退 required_level（主管级），与既有 evaluate 行为一致。

---

## 三、权限点 / 隔离 / 迁移同步
- 新增权限点（纳入 T13 预置）：`terminal.switch.manage`、`admission.policy.manage`、`admission.review`（升级审批）。
- `supervisor_review_policies`、开关查询经治理层管理；准入/开关 Guard 自身不做商户隔离（平台级规则），但写入的 decision/audit 带 scope 以供下钻。
- **claims.json 同步**：本任务不新增"声称已实现但未落"的表；落地后更新 REQ-74/75（terminal.access_rejected 业务场景 present）、REQ-42（admission 事件名/ supervisor_review_policies present）。`terminal_capability_switches` 加 CHECK、新增 `supervisor_review_policies` 入 claims（present）。

## 四、测试计划
- 迁移：`MigrationVersions.latest()` 动态断言；CHECK 拒绝非法 scope_type/terminal_type/outcome。
- T2：层级求值（**R-10 后：任一层关闭即关闭**，含灰度层的 8 层 × 开/关 = 256 组合矩阵，见 `TerminalSwitchStrictestAndCanaryMatrixTests`）、默认开启、fail-closed、被拒发事件；5 域接口埋点回归。
- T3：block→pending→approve→放行全链、warn 留痕、空策略放行、rank=0 回退；事件名 admission.* 命中。
- 闸口链顺序：开关→准入→权限三道任一失败即拒，且顺序正确（开关在最前避免泄露准入信息）。
- `verify:matrix-vs-migration` / `verify:p0-contract` / `verify:openapi` 绿；后端测试全绿。

## 五、依赖与风险
- 依赖：`supervisor_review_*`(V050/V057)、`AuthorizationService`、`audit_events`、`terminal_capability_switches`(V007)。
- 风险：①五域接口埋点工作量大、需逐域回归（与 T10/T11 实为同批埋点，建议合并执行）；②切面 `@Order` 与既有事务/鉴权 Filter 的次序需验证；③能力码/动作码命名需与权限点统一前缀，避免配置混乱。
- **T2/T3 埋点 = backlog T10（准入埋点）/T11（开关埋点）的落地动作**，本规格把框架与策略表先建好，埋点随各域 P0 推进。

## 六、变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T2+T3 动作前置闸口链规格（统一 @GuardedAction 切面；T2 开关 7 维优先级+fail-closed+拒绝留痕；T3 supervisor_review_policies 外部化+三态+admission.* 事件收口） | R5 backlog T2/T3；裁决⑥⑧；表/服务现状取自 V007/V050/V057 与 SupervisorReviewService/AuthorizationService |
