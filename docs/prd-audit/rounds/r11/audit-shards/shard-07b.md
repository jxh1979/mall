# R11 shard-07b — 6 项 P0 代码位置复核

基线 `1c5c0c7d`。方法:`git show 1c5c0c7d:<path>` 读取,逐项实测行号与代码原文。
`evidence_type` 定义:`IMPLEMENTATION_ACTIVE` = 有 route/wiring/迁移种子证明当前可达;
`IMPLEMENTATION_INERT` = 只有代码,未证明可达。

---

## P0-1 `accounting_period` 取 dueDate(= 过账日 +30 天)所在月

**判定:成立(转述的行号完全准确)**

实测 `backend/src/main/java/com/complex/platform/settlement/SettlementPostingService.java:169`

```java
List<JdbcSettlementBillRepository.PostableItem> items = repository.findPostableItems(billId);
LocalDate dueDate = LocalDate.now().plusDays(30);
```

实测同文件 `:219-237`(`createPayable`,ArApItemRequest 最后两个实参):

```java
    String createPayable(String settlementItemId, String merchantId, long amountCent, LocalDate dueDate,
                         String sourceType, String idempotencyPrefix, String billProjectId) {
        ...
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                ...
                dueDate,
                YearMonth.from(dueDate).toString()
        ), explicitProjectId);
```

`accounting_period` 与 `due_date` 同源,二者都由 `LocalDate.now()` 决定,**与结算单自身的业务账期无任何关联**。

**账期数据是拿得到的、但没被用**:`JdbcSettlementBillRepository` 结算单头就存了 `period_start` /
`period_end`(`:559-571`、`:755-774`,唯一键 `ON CONFLICT (COALESCE(project_id,'__DEFAULT__'), period_start, period_end)`),
`findBillId(projectId, periodStart, periodEnd)` `:804`。即真实账期在同一张表上,`createPayable` 只是没接。

**后果(用实数跑一遍)**:7 月账期的结算单,若在 8 月 5 日过账 → dueDate=9/4 → `accounting_period` 记 **`2026-09`**;
同一张单若 8 月 25 日过账 → dueDate=9/24 → 仍 `2026-09`;若 7 月 3 日过账 → `2026-08`。
即**同一账期的同一笔钱,记账期随「谁哪天点了过账」漂移**,且恒定后移 1~2 个月。

`evidence_type`: **IMPLEMENTATION_ACTIVE**
理由:`createPayable` 由同文件 `:184/:188/:195` 三处 `postXxx` 主流程直接调用(非 dead code);
且记忆在案的过账开关 `SETTLEMENT_MONTHLY_POSTING_ENABLED=true` 已 go-live、2026-08-01 已真落过一次账
(该点为 runtime 环境事实,本 shard 未在 1c5c0c7d 代码内二次取证,仅作可达性佐证)。

**权威口径(相反)**
`docs/business-system-map-for-operations-v0.1.html:1501`:
> 「**通票分账**、单店门票结付、游乐域内结算、餐饮与零售的券结算,一律沿用与积分结算相同的账期:**自然月计费、次月出账**;付款落在三个付账日上。」
> 「每一笔应付落在哪一个付账日仍由财务人员按本文件付账日归属规则指定」
> 「必须满足固定的先后顺序:月结定格 → 账单出账 → 异议期届满 → 付账日,后一环不得早于前一环。」

`docs/business-enum-quick-reference-v0.1.html:207`(营业额抽成,同一账期原则):
> 「按开台或下单时点所属的营业日归属账期;该账期发生商品退款的,在退款发生的当期生成冲正,**不回改历史月份**」

代码三处违背:①账期不是自然月计费而是 now+30 派生;②付账日不是财务人员从 5/15/25 三档指定而是硬编码 +30 天
(`LocalDate.now().plusDays(30)` 落在哪天全凭过账动作当天,几乎必然不是 5/15/25);
③「月结定格→出账→异议期→付账日」的顺序校验在这条路径上根本不存在。

**severity 建议:Blocking / P0**
风险:总账按错误月份归集,财务月度报表与结算单账期对不上;跨年时(12 月账期过账 → `2026-01`)错到跨会计年度;
且该字段一旦落库进 ar_ap_item 即为记账事实,事后纠正需冲正而非改写。

---

## P0-2 企微留存策略会真删/真匿名 + 迁移种子默认 180 天 anonymize enabled=true

**判定:成立,但须拆成两半分别定级 —— 「种子」这一半比转述的更强,「会真删」这一半比转述的弱。**

### (a) 种子确是**迁移种子**(不是「某个库里碰巧有这行」)

实测 `backend/src/main/resources/db/migration/V087__wecom_compliance.sql:70-73`(转述的 `:72` 落在该 INSERT 块内):

```sql
-- 默认留存策略：全局 180 天匿名化。
INSERT INTO wecom_retention_policies (id, scope_type, scope_id, retention_days, expiry_action, enabled)
VALUES ('wrp_global_default', 'global', '*', 180, 'anonymize', true)
ON CONFLICT (scope_type, scope_id) DO NOTHING;
```

**这是 Flyway 迁移脚本里的 INSERT** → 任何跑完迁移的环境(dev/test/prod)**必然存在该行**,且 `enabled=true`、
`expiry_action='anonymize'`、`retention_days=180`。全仓仅 `V087` 与 `V353` 提到 `wecom_retention_policies`,
而 `V353__ticket_face_photo_retention_window.sql` 只在**注释**里引用它作上界对齐(`:14`),**没有任何后续迁移改写或删除该种子行**。
`evidence_type`(种子): **IMPLEMENTATION_ACTIVE**

代码侧同源硬编码兜底,`WecomConversationService.java:56` `static final int DEFAULT_RETENTION_DAYS = 180;`,
`:725-728` `defaultPolicy(...)` 在查不到策略时同样返回 `180 / "anonymize" / true`——即使有人手删种子行,兜底仍是 180 天匿名化。

更关键的一条(转述里没有):`WecomConversationService.java:218` 在**每次同步落库时**就给会话盖了到期戳:

```java
LocalDate retentionUntil = businessDate.plusDays(retentionDays);
```

即到期时点已经写进数据,不是运行时才算。

### (b) 「会真删/真匿名」——动作代码属实,但**当前不可达**

实测 `WecomConversationService.java:433-457`(转述行号准确):

```java
    public WecomRetentionRunResult applyRetention(LocalDate today) {
        ...
            switch (action) {
                case "delete" -> {
                    repository.deleteMessagesByConversation(conversation.id());
                    repository.updateConversationArchiveStatus(conversation.id(), "deleted");
```

匿名化确为**不可逆**,`JdbcWecomComplianceRepository.java:223-232`:

```java
                UPDATE wecom_messages
                   SET content_raw_ref = NULL,
                       content_redacted = '[ANONYMIZED]',
                       anonymized = true,
                       ai_qc_result = NULL
```

`content_raw_ref` 置 NULL + 脱敏文替换为占位符 → 原文引用丢失,不可恢复。

**但 `applyRetention` 全仓只有一个调用点**(`git grep applyRetention` 命中 4 处,除 javadoc 与定义外仅 Job):
`WecomRetentionJob.java:44-51`,实测:

```java
    @ConditionalOnProperty(name = "compliance.wecom-retention.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${compliance.wecom-retention.scheduled.fixed-delay-ms:86400000}")
    public void scheduledApply() {
        if (!scheduledEnabled) { return; }
```

且 `backend/src/main/resources/application-prod.yml:26` 明确写着:
> `# 企微会话留存(compliance.wecom-retention)按用户选择保持默认关。`

**无任何 Controller/REST 端点调用 `applyRetention`。**
`evidence_type`(销毁执行): **IMPLEMENTATION_INERT** —— 双重关闭(`@ConditionalOnProperty` 不注册 + 布尔早返回),
prod 显式保持关。转述所称「会真删/真匿名」在当前配置下**不会发生**,这一半应降级。

### (c) 与口径相反的**真问题**在配置面,不在销毁面

`docs/business-system-map-for-operations-v0.1.html:899`:
> 「企业微信,员工工作聊天每天自动上传到后台,**永久保存、不自动到期删除**;确需清理的,按留痕清理的同一规则由**最高权限本人插入授权 U 盘密钥后手动执行**。」

同口径另见 `business-system-map-for-operations-v0.1.html:1450`、`:1838`,
`platform-governance-map-for-operations-v0.1.html:570`、`:868`(「永久存储(不自动到期删除…)」)。

代码与之相反的三点:
1. 种子把「不自动到期删除」写成了 **180 天到期 anonymize、enabled=true**,方向完全相反;
2. `WecomConversationService.java:720` 校验 `retentionDays` 必须在 **1..3650** 之间——
   ```java
   if (request.retentionDays() != null && (request.retentionDays() < 1 || request.retentionDays() > 3650)) {
       throw ApiException.badRequest("VALIDATION_FAILED", "留存天数必须在 1 到 3650 之间");
   ```
   **「永久」在这套配置里根本无法表达**,最长只能配 10 年,连人工绕开都做不到;
3. HTML 要求的「最高权限本人 + 授权 U 盘密钥手动执行」清理通道,在本模块**不存在**;
   现有唯一清理路径是定时批处理(虽关着),授权形态与口径不符。

**severity 建议:Blocking / P0(降级说明见下)**
- 「种子 + 到期戳 + 无法表达永久」= Blocking:数据已按 180 天盖戳,一旦有人把 `scheduled.enabled` 打开
  (一个配置项之隔),历史会话立即批量匿名化且不可逆;而这是合规取证材料。
- 「当前已经在删」= **不成立,应降级为 INERT**:prod 显式关闭且无手动端点。
风险描述:开关是「一行配置从关到开」的距离,而其后果不可逆、且直接违反三份 HTML 四处明写的「永久保存」。

---

## P0-3 人工放行两条链活着,而 ENUM 明写「系统不提供」

**判定:成立。且实测发现的是**三条**链,不是两条。**

### 权威口径(全仓唯一一处「人工放行」原文)

`docs/business-enum-quick-reference-v0.1.html:41`(「核销记录状态」词条句末):
> 「**系统不提供人工放行**:核销要么成功、要么拒绝或失败,**任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**。」

同词条另有约束:「共六态」= 成功、拒绝、失败、已冲正、待复核、异常。**`manual_overridden` 不在六态之内。**

### 链 1:票务核销人工放行(端点 + 权限种子 + DB CHECK 三件齐)

`backend/src/main/java/com/complex/platform/ticketing/TicketingController.java:483-490`(转述 `:486` 准确):

```java
    // C1/REQ-05：人工放行（主管）。主管对核销做人工放行决策，留痕 status=manual_overridden。
    @PostMapping("/ticketing/verifications/manual-override")
    public ApiResponse<VerificationResponse> manualOverrideVerification(
            @RequestBody @jakarta.validation.Valid VerificationManualOverrideRequest request
    ) {
        authorizationService.requirePermission("verification.manual_override");
```

`backend/src/main/resources/db/migration/V270__verification_manual_override_permission.sql:8-17`(转述 `:10` 落在此 INSERT 内):

```sql
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('verification.manual_override', '人工放行票务核销', 'ticketing', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
...
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'verification.manual_override')
```

`backend/src/main/resources/db/migration/V067__ticket_multiperson_reverse.sql:4-6`(转述 `:5` 准确):

```sql
-- status：现为自由 VARCHAR(32)，本期补 CHECK 收口，真实全集 = success/failed/rejected/reversed/manual_overridden。
--   注意：manual_overridden 由 TicketVerificationService.manualOverride 实际写入，failed/rejected 为权威状态词表保留，
--   漏 manual_overridden 会破坏现有人工放行流。
```

执行体 `backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java:211-229`:

```java
    VerificationRecord verification = repository.saveVerification(new VerificationRecord(
            ...
            "manual_overridden",
            reason == null || reason.isBlank() ? "manual override" : reason.trim(),
```

**该方法不跑任何核销规则校验**(只做 `requireRegisteredDevice` + `enforceMerchantScope` 归属守卫),
直接落一条 `manual_overridden` 记录——这正是口径禁止的「以人工方式让一笔不满足规则的核销通过」。

`evidence_type`: **IMPLEMENTATION_ACTIVE**
四重证据齐:HTTP route + 迁移种子建权限 + **迁移种子把该权限授给 `role_platform_admin`** + DB CHECK 把
`manual_overridden` 明确纳入合法状态全集。任一环境跑完迁移,平台管理员即刻可调。

### 链 2:治理督导复核内置 `manual_release` 策略(前后端俱全)

`backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:36`(转述准确):

```java
    private final List<SupervisorReviewPolicyRecord> policies = List.of(
            new SupervisorReviewPolicyRecord("manual_release_l2", "manual_release", 0, 2, true, List.of("operator_note", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
```

注意第 8 个字段值为 `"active"` —— 该策略是**启用态**,不是占位。

前端 `apps/web/src/admin/pages/SupervisorReviewPage.tsx:255`(转述准确),操作类型下拉里明摆着:

```tsx
<option value="contract_violation">合同违规</option><option value="base_definition_violation">基础定义违规</option><option value="billing_discount">账单折扣</option><option value="manual_release">人工放行</option>
```

后端三条 route 齐备(`SupervisorReviewController.java:27/33/40`):
`GET /api/v1/platform/supervisor-review-policies`、`POST /supervisor-review/evaluate`、`POST /supervisor-review/{review_id}/approve`。

`evidence_type`: **IMPLEMENTATION_ACTIVE**(route + UI 入口 + status=active 策略)
**限定**:本链只产出审批决策记录,未见它回写 `ticket_verifications`;即它是「人工放行的审批壳」,
不是执行体。但口径禁的是「**任何岗位、任何权限都不能**以人工方式让…通过」,一个面向运营的、
名字就叫「人工放行」的审批工位摆在后台,与「系统不提供」直接冲突——这是**业务面**的矛盾,不需要它回写才算数。

### 链 3(转述未列,实测发现):游乐人工放行

`backend/src/main/java/com/complex/platform/amusement/AmusementController.java:39/45`:
`GET /manual-overrides` + `POST /manual-overrides`;
`AmusementService.java:120` `@GuardedAction(capability = "amusement.verification.override", admissionAction = "amusement.manual_override", ...)`;
`backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql:123/133`
建表 `amusement_manual_overrides`,`CHECK (override_type IN ('allow', 'deny', 'pause', 'resume'))`——
`'allow'` 即放行。capability 名 `amusement.verification.override` 直指**核销覆盖**。
`evidence_type`: **IMPLEMENTATION_ACTIVE**(route + 迁移建表 + GuardedAction wiring)。
未逐行追它是否绕过规则,标为需 shard-07c/后续单列复核。

**severity 建议:Blocking / P0**
风险:①口径写死「不提供」而系统提供了三条;②`manual_overridden` 是六态之外的第七态,
所有按六态写的下游统计/对账/BI 分桶会漏计或落到 default 分支;
③票务链无规则校验直写通过记录,免票入场、超期票入场、年龄闸绕过均可由一个高权限账号完成,
而这恰是 HTML 反复强调不可绕的年龄合规红线相邻面。

---

## P0-4 分池归一化 + 缺行权重 0

**判定:成立,且比转述更严重 —— 归一化只是表征,根因是「档位表少了一个维度」。**

实测 `backend/src/main/java/com/complex/platform/settlement/TicketDistributionSettlementService.java`
(转述的 393/445/452 三处对应实测 388-398 javadoc / 445-451 权重装配 / 460 prorate):

`:445-451` 权重装配:

```java
        List<JdbcSettlementBillRepository.RankTier> tiers = repository.findRankTiers(projectId);
        int[] weights = new int[tierSet.size()];
        long weightSum = 0L;
        for (int i = 0; i < tierSet.size(); i++) {
            weights[i] = shareBpsForRank(tiers, i + 1);
            weightSum += weights[i];
        }
```

`:460` 归一化分配:

```java
                long amount = MoneyRounding.prorate(p2, weights[i], weightSum);
```

`:581-588` 缺行返 0:

```java
    private int shareBpsForRank(List<JdbcSettlementBillRepository.RankTier> tiers, int rank) {
        for (var t : tiers) {
            if (rank >= t.rankFrom() && rank <= t.rankTo()) {
                return t.shareBps();
            }
        }
        return 0;
    }
```

代码自己的 javadoc `:391-393` 把这条设计写成了明文:

```java
     * <b>第二段(归一化分尽)</b>:其余酒吧按排名档位权重对余额 P2 一次归一化分光
     * ({@code b_j = round_half_up(P2 × w_j / Σw)},只舍入一次),档位只是相对权重——
     * 权重和不足 10000 也分尽(和恰为 10000 时与两段化前 bpsOf 逐分一致)。
```

`evidence_type`: **IMPLEMENTATION_ACTIVE**
(位于 `calculate` 主链,同 service 落 `settlement_bills`/明细;分账过账管道 memory 在案已 go-live。)

### 权威口径(逐字相反)

`docs/business-enum-quick-reference-v0.1.html:49`(「酒吧通票可分配池」):
> 「**档位为动态百分比,不是相对权重、不做归一化**:一张票的第二段余额,由该票覆盖的酒吧中参加第二段的那几家,
> 按其在第二段参与者内部的排名分配——已从第一段取得份额的酒吧不占第二段名次,其后名次依次前移;
> **这几家的各档合计恒为 100%**。**按本张票的第二段参与家数查「参与家数档位表」(原称「覆盖家数档位表」,同一张表两个叫法)取对应一行**,
> 行内各名次的百分比即为该名次酒吧应分得的比例(出现并列名次的,按下述并列均分规则换算后适用)。」
> 「当期核销入场人数相同的酒吧为**并列名次**:并列各家共同占用其本应占据的连续名次,
> 各家一律取这几个名次的百分比之和的**算术平均值**,作为本张票的适用比例」

代码与之三点相反:

1. **归一化本身被 HTML 逐字禁止**(「不是相对权重、不做归一化」),代码 `prorate(p2, w, Σw)` 就是归一化;
2. **档位表缺「参与家数」维度**。HTML 要求二维查表:先按本张票第二段参与家数取**一行**,行内按名次取百分比。
   实测 `JdbcSettlementBillRepository.java:1189` `public record RankTier(int rankFrom, int rankTo, int shareBps) {}`
   —— **只有名次区间 + 比例,没有参与家数**;`findRankTiers(projectId)` 也只按 projectId 取。
   即「参与家数档位表」这张表**在代码里根本不存在**,现有实现是一维名次表。
   这是缺行返 0 的**根因**:一维表配到第 N 名,第 N+1 名必然脱档 → 权重 0 → 该酒吧分文不得,
   而 HTML 口径下「各档合计恒为 100%」意味着**不存在脱档的参与者**;
3. **并列名次未实现**。`:446` 注释「tierSet 保持 stats 的 verified_count DESC 序 → 顺位即第二段内部排名(1-based)」,
   `:450` 直接用 `i + 1` 当名次 —— 核销人数相同的两家会拿到不同名次、不同档位比例,
   HTML 要求的「取这几个名次百分比之和的算术平均值」完全没有对应代码。

### 归一化掩盖了缺行(为什么两个 bug 缠在一起)

归一化恰好把「缺行 → Σw < 100%」的账面症状抹平:少一家的权重 0 会被 `Σw` 一起变小抵消,
剩余各家按比例被**放大**分尽 P2,总额守恒、对账看不出异常,但**每一家分到的钱都不是 HTML 规定的那个百分比**。
若按 HTML 不归一化,则「Σ档位 ≠ 100%」会立刻暴露成显式差额,反而可被 S7 差额规则接住。
即当前实现把一个**可发现的配置错误**转成了**不可发现的金额错误**。

**severity 建议:Blocking / P0**
风险:真实钱路。每张通票第二段的每一家酒吧分账金额都可能偏离权威口径,且账面守恒、对账无异常信号;
并列名次场景下同人数两家酒吧拿到不同金额,商户按 HTML 承诺的「计算规则对该酒吧主理人完全可见」自查即可发现差异,
构成对外可举证的口径违约。缺「参与家数」维度属**数据模型级缺失**,不是改一行能修的。

---

## P0-5 终端开关 opt-out(无显式行=默认开启),与口径「缺行不得视为开启」相反

**判定:核心结论成立。但转述给的第二个位置 `V068:46` **不成立**,是张冠李戴——须改引。**

### (a) Java 侧:成立,`TerminalSwitchGuard.java:20` 一字不差

实测 `backend/src/main/java/com/complex/platform/gate/TerminalSwitchGuard.java:20`(类 javadoc 内):

```java
     * <b>高层级关闭压倒低层级打开</b>——任一层关闭即最终关闭，低层"打开"不再把高层"关闭"翻回来。
     * 无任何显式行=默认开启（opt-out 语义）；读取异常=fail-closed 拒绝。
```

实现兑现了这句注释,实测 `:83-102` `check(String capabilityCode)`:

```java
        Optional<TerminalCapabilitySwitchRecord> effective;
        try {
            effective = repository.resolveEffectiveSwitch(terminalType, capabilityCode, scopes);
        } catch (RuntimeException exception) {
            reject(terminalType, capabilityCode, "switch_fetch_failed", null, null);
            return; // unreachable，reject 必抛
        }
        // 任一层显式关闭 → 拒绝（effective 已是"最高层关闭行"）。
        if (effective.isPresent() && !effective.get().enabled()) {
```

`Optional.empty()`(七层 scope 一行都没配)→ 两个 if 都不进 → 直接落到灰度层,
而灰度层 `:110-124` 注释明写「无 active 试点则**弃权**」→ 整条链放行。**缺行 = 放行,证实。**

同一模式在 `:62-76` `checkTerminalEnabled()` 端总开关上重复一次:
`if (master.isPresent() && !master.get().enabled())` —— 连**端总开关**缺行也是默认开。

`evidence_type`: **IMPLEMENTATION_ACTIVE**
`@Component`,由 `com.complex.platform.gate` 准入闸链路调用(与 `@GuardedAction` 同包 wiring),是全部终端能力的必经闸。

### (b) `V068:46 DEFAULT true` —— **不成立,反证如下**

实测 `backend/src/main/resources/db/migration/V068__action_gate_switches_admission_policies.sql:38-46`:

```sql
CREATE TABLE IF NOT EXISTS supervisor_review_policies (
    ...
    enabled        BOOLEAN      NOT NULL DEFAULT true,
```

该 `DEFAULT true` 属于 **`supervisor_review_policies`(督导复核策略表)**,与终端开关无关。
V068 全文只有这一处 `enabled ` 列定义(`grep -n 'CREATE TABLE\|enabled '` 仅命中 `:38` 与 `:46`)。

终端开关表的真实 DDL 在 `backend/src/main/resources/db/migration/V010__p0_foundation_residual_platform_terminal_evidence.sql:64-75`:

```sql
CREATE TABLE IF NOT EXISTS terminal_capability_switches (
    ...
    enabled BOOLEAN NOT NULL,
```

**没有 DEFAULT 子句**。opt-out 语义**完全由 Java 层的 `Optional.isPresent()` 判定产生**,不是数据库默认值。
这个区别有实操意义:①修复点只有一处 Java(把 `isPresent() && !enabled` 改成 `isEmpty() || !enabled`),
不需要改迁移;②不能用「改 DB 默认值」来修——缺行场景下压根没有行,DEFAULT 永远不会被求值。

### 权威口径(逐字相反,且已预先堵死本实现的辩解口)

`docs/platform-governance-map-for-operations-v0.1.html:672`:
> 「开关默认取值:任何能力在后台没有显式开关配置行时**一律默认关闭(opt-in)**,要启用必须有人显式打开;
> **开关缺行不得视为开启,也不得由代码或部署脚本代为「视同打开」**。
> 这与拉取失败时的「兜底关闭」是两件事——兜底关闭针对读不到配置的运行时故障,本条针对配置里压根没有这一行。」

`docs/business-system-map-for-operations-v0.1.html:1848`(已拍板框):
> 「任何能力在后台没有显式开关配置行时一律默认关闭(opt-in),要启用必须有人显式打开;开关缺行不得视为开启。」

代码注释里的「读取异常=fail-closed 拒绝」正是 HTML 末句点名的**另一件事**,不能拿来抵这一条。
换言之 HTML 作者写这条时已经预见到了「我们有 fail-closed 啊」这个辩解并明确排除。

**severity 建议:Blocking / P0**
风险:任何**新上线的 capability**在没人配开关行之前默认全端可用——包括新增的高危能力;
`terminal_capability_switches` 是空表时,`terminal.enabled` 端总开关也形同虚设,「关闭后该端不可用」的承诺不成立。
修复是一处 Java 条件反转,但**反转后所有既有 capability 会立刻全部拒绝**,必须同步补齐显式开启种子迁移,
属「改一行、配一片」型,不可只改代码就发。

---

## P0-6 抽成基数负额静默钳零

**判定:成立(行号 `:1071` 精确)。但「静默」需限定,且真正的口径冲突比钳零更大。**

实测 `backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java:1045-1071`
(`aggregateMerchantReflowTurnover`):

```java
        Long netTurnover = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(
                         CASE sync_type
                             WHEN 'order'  THEN amount_cent
                             WHEN 'refund' THEN -amount_cent
                             ELSE 0
                         END), 0)
                  FROM integration_order_syncs
                 WHERE merchant_id = :merchant_id AND status = 'synced' ...
        return netTurnover == null ? 0L : Math.max(0L, netTurnover);
```

**「静默」限定**:代码**有**注释交代(`:1036-1037`):

```java
        //   ② 扣退款 —— 实收 = SUM(order.amount_cent) − SUM(refund.amount_cent)，同商户同窗口相抵；
        //      退款超出订单时钳到 0（负营业额无业务意义，不作为抽成基数）；
```

即设计时是**有意**钳的,不是疏忽。但**运行时确为静默**:钳零不写 `audit_events`、不置任何标志、
不返回被丢弃的差额,上层 `FinanceService.java:1585` / `:1764` 拿到的就是一个普通的 `long`,
无从分辨「本月真的没营业额」与「本月退款超出订单 X 元」。对账面上两者完全一样。

`evidence_type`: **IMPLEMENTATION_ACTIVE**
两个生产调用点 `FinanceService.java:1585`(按 period 试算)与 `:1764`(出账,`rent<=0` 抛 `TURNOVER_RENT_ZERO`),
其后接 `TurnoverRentMath.tieredRent(...)` / `clampRent(...)` 直接算租金,是真实钱路。

### 权威口径(相反)

`docs/business-enum-quick-reference-v0.1.html:207`(「营业额抽成模式」),
同句另见 `docs/business-system-map-for-operations-v0.1.html:1218`:
> 「营业额＝该商户当期在本综合体内含税实收成交总额,减去当期已成功的退款金额…
> **该账期发生商品退款的,在退款发生的当期生成冲正,不回改历史月份;
> 冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回,
> 不按冲正发生当期的档位与费率重算,原账期已定格的账单与档位不因冲正而重算;
> 多笔退款逐笔分别冲正,不合并折算。**」

代码与之两处相反,**第二处比钳零更严重**:

1. **钳零 = 丢弃冲正**。HTML 要求退款「生成冲正」,冲正天然可为负(向商户退还已收租金)。
   `Math.max(0L, ...)` 把超额退款月的负基数抹成 0 → 该退给商户的抽成**永远不会退**,
   且因「不回改历史月份」,这笔差额也不会在后续月份被找补——**钱直接消失在平台一侧**。
2. **合并折算 + 按当期档位重算**,而 HTML 逐字禁止两者。
   SQL 是 `SUM(order) - SUM(refund)` 一把汇总(= 「合并折算」),
   汇总结果再整体过 `tieredRent(turnover, tiers, ...)`(= 按**冲正发生当期**的档位与费率算),
   而 HTML 要求「多笔退款**逐笔分别**冲正」且「按该笔款项在其**原账期**实际计费时所适用的档位与费率**原额冲回**」。
   现有数据结构里连「该退款对应的原订单在哪个账期、当时适用哪一档」都没有记录,
   `integration_order_syncs` 的 refund 行只有 `amount_cent` 与 `created_at`,
   **无 refund→原 order 的关联列**,因此逐笔原档冲回目前**不具备实现基础**。

### 用实数跑一遍(超额累进放大效应)

设某商户阶梯 0–10 万 5%、10 万以上 8%。
7 月订单 30 万 → 已按 5%×10万 + 8%×20万 = 2.1 万计租并出账定格。
8 月发生 25 万退款、新订单 5 万 → 净 −20 万。
- HTML 口径:8 月生成冲正 −(8%×20万) = **−1.6 万**(按原账期 8% 档原额冲回),商户应收回 1.6 万。
- 代码口径:`Math.max(0L, -200000_00)` → 0 → 8 月抽成租金 0,冲正额 **0**。
差额 1.6 万元由平台留存,且无任何审计痕迹。若退款只有 15 万(净 −10 万),差额仍是 8000 元量级。

**severity 建议:Blocking / P0**
风险:真实资金差错且方向恒定利于平台、不利于商户;错额随退款规模与阶梯档位放大;
无审计痕迹 → 商户与财务均无法从系统内发现,只能靠外部对账;
「逐笔原档冲回」缺数据基础,属**数据模型级欠账**,修复不是去掉一个 `Math.max` 就完事——
直接去掉钳零会让负基数进 `tieredRent`,其对负数的行为未在本 shard 核实,需连带复核 `TurnoverRentMath`。

---

## 汇总

| # | 结论 | evidence_type | 备注 |
|---|------|---------------|------|
| P0-1 | **成立**,行号精确 | ACTIVE | 账期取 now+30 派生,无 |
| P0-2 | **部分成立** | 种子 ACTIVE / 销毁执行 **INERT** | 「会真删」需降级:prod 显式关、无手动端点 |
| P0-3 | **成立**,且是**三条**链非两条 | ACTIVE ×3 | 新增游乐链 `amusement_manual_overrides` |
| P0-4 | **成立**,且更严重 | ACTIVE | 根因是「参与家数档位表」维度缺失 + 并列名次未实现 |
| P0-5 | **核心成立**,但 `V068:46` **不成立** | ACTIVE | 引证须改为 `V010:64-70`(无 DEFAULT);语义在 Java 层 |
| P0-6 | **成立**,行号精确 | ACTIVE | 「静默」限定为运行时无留痕(代码有注释);更大问题是合并折算+按当期档位 |

**六项中:5 项完全成立(P0-1/3/4/5-核心/6),1 项部分成立需降级(P0-2 的销毁执行面 → INERT);
另有 2 处引证必须订正:P0-5 的 `V068:46` 张冠李戴(应为 `V010:64-70`),P0-3 遗漏第三条链(游乐)。**
