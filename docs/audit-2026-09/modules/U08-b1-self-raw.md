CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U08 未发布材料第 8 段（组织与商户联动）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T22:41:33Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `741da5bf9d75fbbc50468a213e9e70ed75d422c8`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 8 段，5 文件 2191 行（governance/JdbcPlatformRepository.java 1103 / MerchantRecord.java 90 / PlatformController.java 237 / PlatformService.java 757 / dto/MerchantSelfOperatedRequest.java 4 新增），main 主归属 M009；适用判据＝M009 映射行并集 87 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离编译在 `/tmp/u-overlay/backend`（基线 242f4872 `git archive` + 76 未发布文件，`mvn -o compile`）内进行，不触主仓、不写任何 complex_test* 库。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U08-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U07 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U08-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：questions/U04-001.md 操作员 2026-09-09 已答「删除」（自营标注独立布尔）；本段自营插拔为该布尔的后端实现面，按已答处置登记，不再弹窗。只读依赖追流（不审）：副本 `StaffRegistrationService.java`（第 5 段消费方）、`OrganizationMerchantPage.tsx`（第 3 段）、`V408`（第 10 段）、`PlatformServiceTests`/`PlatformControllerTests`（第 12 段）；main `AuditedOperationExecutor.java`、M009 修复后的四个同名文件。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1103 | 61753 | b2552172af0cec506ee1c753e12ea839abbf645d3589c78e5c412c66bf880fec | 全读 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 90 | 6233 | 750a0227662feeb14ad686c6c3c96270644acc559ef5a2c0dc044e9d03953ef8 | 全读 |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 237 | 13611 | 41d1beec574dd21f873f780e9b7bd515bc829563c09f7a83603a8bfd05a9598d | 全读 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 757 | 35843 | e079db05d677f269585b5f0a6c95aa807f86b9ca71f0bb11728e43d4023ab456 | 全读 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java | 4 | 175 | 84a9f41426492cf83d22c7a08d50f18c92bc72b46cef43fe276e504fb6ea7d91 | 全读 |

## 2. 可执行证据原文（/tmp/u08_b1_run.log）

```
START=2026-09-09T20:45:30Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1103 | 61753 | b2552172af0cec506ee1c753e12ea839abbf645d3589c78e5c412c66bf880fec
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 90 | 6233 | 750a0227662feeb14ad686c6c3c96270644acc559ef5a2c0dc044e9d03953ef8
backend/src/main/java/com/complex/platform/governance/PlatformController.java | 237 | 13611 | 41d1beec574dd21f873f780e9b7bd515bc829563c09f7a83603a8bfd05a9598d
backend/src/main/java/com/complex/platform/governance/PlatformService.java | 757 | 35843 | e079db05d677f269585b5f0a6c95aa807f86b9ca71f0bb11728e43d4023ab456
backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java | 4 | 175 | 84a9f41426492cf83d22c7a08d50f18c92bc72b46cef43fe276e504fb6ea7d91
=== manifest entries ===
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java 1103 61753 b2552172af0cec506ee1c753e12ea839abbf645d3589c78e5c412c66bf880fec
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java 90 6233 750a0227662feeb14ad686c6c3c96270644acc559ef5a2c0dc044e9d03953ef8
backend/src/main/java/com/complex/platform/governance/PlatformController.java 237 13611 41d1beec574dd21f873f780e9b7bd515bc829563c09f7a83603a8bfd05a9598d
backend/src/main/java/com/complex/platform/governance/PlatformService.java 757 35843 e079db05d677f269585b5f0a6c95aa807f86b9ca71f0bb11728e43d4023ab456
backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java 4 175 84a9f41426492cf83d22c7a08d50f18c92bc72b46cef43fe276e504fb6ea7d91
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment, numstat) ===
13	10	backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
15	4	backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
10	0	backend/src/main/java/com/complex/platform/governance/PlatformController.java
19	1	backend/src/main/java/com/complex/platform/governance/PlatformService.java
4	0	backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java
=== main HEAD vs baseline (M009 changed these files after the material forked) ===
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java baseline=51f12a0ecd head=7c2522089a unpublished=763382a061 main_changed_since_baseline=yes numstat_baseline_to_head=34/22
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java baseline=afbf6fb5dc head=c2deb1febc unpublished=0f893bd20b main_changed_since_baseline=yes numstat_baseline_to_head=12/1
backend/src/main/java/com/complex/platform/governance/PlatformController.java baseline=93f260fcf9 head=567dc33c2a unpublished=8f3495d8ef main_changed_since_baseline=yes numstat_baseline_to_head=9/0
backend/src/main/java/com/complex/platform/governance/PlatformService.java baseline=0de37ac250 head=fe67a29349 unpublished=36405812e9 main_changed_since_baseline=yes numstat_baseline_to_head=198/61
backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java main_exists=no
=== main commits touching governance since baseline ===
2e812adf audit(160): M001–M010 checkpoint — 十个模块双席闭环的修复与审计台账
=== probes: self-operated plumbing in unpublished copy (to be deleted per questions/U04-001.md) ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:90:           
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:97:           
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:123:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:135:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:150:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:166:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:219:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:396:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:532:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:622:          
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:36:        boolean sel
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:42:    public Merchant
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:43:        return new 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:66:                arc
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:79:                arc
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:88:                arc
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformController.java:86:    @PostMappin
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformController.java:87:    public ApiR
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformController.java:88:            @Re
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformController.java:91:        return 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:178:    public Mercha
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:179:            com.c
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:181:        if (reque
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:183:        return ex
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:184:                "
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:186:                 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:187:                 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:188:                 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/PlatformService.java:655:                 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java:4:pub
=== probe: audit double record — inner recordSuccess inside executeWrite lambda (siblings record none) ===
105:        return executeWrite(
183:        return executeWrite("merchantSelfOperated:"+id,request.idempotencyKey(),request,MerchantRecord.class,
187:                    auditEventService.recordSuccess("merchant.self_operated_changed","merchant.profile.manage","merc
211:        return executeWrite(
239:        return executeWrite(
264:        return executeWrite(
297:        return executeWrite(
365:        return executeWrite(
427:        return executeWrite(
449:        return executeWrite(
519:        return executeWrite(
558:        return executeWrite(
622:        return executeWrite(
660:    private <T> T executeWrite(
671:        return operations.executeWrite(
30:            auditEventService.recordSuccess(
62:            auditEventService.recordSuccess(
89:            auditEventService.recordSuccess(
130:                        auditEventService.recordSuccess(
163:        return auditEventService.recordSuccess(
=== probe: main has no self-operated (HEAD) ===
backend/src/main/java/com/complex/platform/governance/PlatformService.java:2
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:0
backend/src/main/java/com/complex/platform/governance/PlatformController.java:0
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:0
49:    public static final Set<String> MERCHANT_COOPERATION_FORMS =
50:            Set.of("self_operated", "lease", "joint_operation", "managed_operation", "service_provider", "other");
657:        if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
660:                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
=== probe: V408 column in unpublished migrations; main has V410 ===
2:ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE;
1
V410__m010_business_entity_topology_and_role_isolation.sql
=== probe: updateMerchant record construction (19-arg ctor resets archive/delete fields?) baseline vs main ===
186:                    MerchantRecord saved = repository.saveMerchant(merchant.withSelfOperated(request.selfOperated())
635:                    return repository.saveMerchant(new MerchantRecord(
655:                    ).withSelfOperated(repository.findMerchantById(merchantId).map(MerchantRecord::selfOperated).orE
213:                    MerchantRecord saved = repository.saveMerchant(merchant.withArchive(now, actorId(), now));
237:                            merchant.withArchive(null, null, now).withDelete("none", null, null, null, now));
610:                        repository.findActiveBusinessEntityBindingByMerchantId(existing.merchantId())
611:                                .filter(active -> !active.id().equals(existing.id()))
620:                            existing.id(),
621:                            existing.merchantId(),
622:                            existing.operationEntityId(),
623:                            existing.contractEntityId(),
624:                            existing.settlementEntityId(),
626:                            existing.effectiveFrom(),
629:                            existing.createdAt(),
759:                    return repository.saveMerchant(new MerchantRecord(
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:701:                <li>建立商户档案，区分自营、
docs/business-system-map-for-operations-v0.1.html:1433:                <li>新员工注册分两条路径：�
docs/platform-governance-map-for-operations-v0.1.html:495:                <li>区分自营、租赁、联营�
docs/platform-governance-map-for-operations-v0.1.html:578:                <li>员工注册分两条路径：�
=== isolated compile: /tmp/u-overlay/backend mvn -o compile (log /tmp/u_backend_compile.log) ===
START=2026-09-09T18:37:14Z
MVN_EXIT=0
END=2026-09-09T18:37:19Z
1
END=2026-09-09T20:45:30Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 8 段（5 文件 2191 行；`dto/MerchantSelfOperatedRequest` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 numstat：13/10、15/4、10/0、19/1、4/0，合计 +61/−15，全部是 `merchants.is_self_operated` 布尔的插拔：仓储列插拔 7 块、记录字段与兼容构造、控制器 `POST /merchants/{id}/self-operated`、服务 `setMerchantSelfOperated` 与 `updateMerchant` 保留布尔、请求 DTO），main 当前版本仅作分叉对照。
- 第一性原理线：HTML BSM L701/PGM L495/L498「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」、BSM L1433/PGM L578「在商户档案上标注为自营的商户视同公司自有」——「自营」是六类合作方式之一；操作员已于第 4 段按 2.2 裁决（questions/U04-001.md「删除」）：不保留独立于合作方式的自营布尔，自营标注落在既有六类码 `self_operated`。本段全部改动是该布尔的后端实现面。
- 顺从式线：87 项判据逐项（§5）。
- 对抗式线：本段是否还有独立于自营布尔的改动（无）；服务写路径是否重复记审计（是，CX-U08-002）；main 自基线是否已改同名文件（是，M009 四文件，CX-U08-003）；`updateMerchant` 用 19 参构造重建记录是否抹掉存档/删除字段（基线既有，main HEAD 同名路径仍以 19 参重建，归 M009 核查，OBS-2）；请求 DTO 校验；V408 列是否为本段运行前提（是，随删除消失）。
- 只读依赖追流（不审、不计入本段结论）：副本 `StaffRegistrationService.java:66/91`（第 5 段唯一业务消费方）、`OrganizationMerchantPage.tsx:320-329`（第 3 段按钮）、`V408__staff_registration_company_routes.sql:2`（第 10 段列）、`PlatformServiceTests`/`PlatformControllerTests`（第 12 段）；main `evidence/AuditedOperationExecutor.java:113-145`（executeWrite 自记成功/失败事件）。
- 5 文件全文逐行读完（§1）；与基线的逐文件 diff 逐块读完（§2 numstat）。可执行验证：`/tmp/u-overlay/backend` 离线 `mvn -o compile` 退出 0、`MerchantSelfOperatedRequest.class` 生成（§2）；本段 DB 行为（V408 列）未在共享测试库应用，未跑。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- main HEAD 445a49f4 的 `JdbcPlatformRepository`/`MerchantRecord`/`PlatformController`/`PlatformService` 自基线均已被 M009 修复改动（§2 blob 与 numstat：合作形式六类白名单、合同/结算主体同步、创建/更新路径重写等），未发布版基于基线：整文件覆盖会回退 M009 已定稿修复；`dto/MerchantSelfOperatedRequest` main 无对应。
- 按 questions/U04-001.md「删除」处置，本段 +61/−15 应整体不合入；删除后本段相对 main 无净改动，M009 修复面不受影响。

## 5. 87 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 不通过 | 独立自营布尔无 HTML 上位且已答「删除」（CX-U08-001） |
| GEN-002 可执行正确性 | 通过（登记） | 插拔本身可编译、幂等、平台域校验；运行前提 V408 列在第 10 段 |
| GEN-003 测试有效性 | 通过（登记） | 第 12 段 `PlatformServiceTests` 是否覆盖 `setMerchantSelfOperated` 由 U12 登记；随删除失效 |
| GEN-004 信任边界 | 通过 | `selfOperated` 空值 400；事由必填 |
| GEN-005 授权 | 通过 | 控制器 `merchant.profile.manage` + 服务 `requirePlatformScope` |
| GEN-006 数据完整性 | 通过（登记） | `updateMerchant` 从库回读布尔保留（`:655`）；19 参构造重建记录抹掉存档/删除字段为基线既有（OBS-2） |
| GEN-007 并发幂等 | 通过 | `requireIdempotencyKey` + `executeWrite("merchantSelfOperated:"+id)` |
| GEN-008 接口一致性 | 不通过 | 与第 3/4/5/10 段共同构成第二「自营」轴（CX-U08-001）；分叉 main M009（CX-U08-003） |
| GEN-009 错误处理 | 通过 | 中文 ApiException（VALIDATION_FAILED/MERCHANT_NOT_FOUND） |
| GEN-010 安全 | 通过 | 响应经 `maskPhonesForUnauthorized`；无新 PII |
| GEN-011 审计证据 | 不通过 | 同一次变更记两条成功审计事件（CX-U08-002） |
| GEN-012 资源 | 通过 | 单行读写 |
| GEN-013 构建 | 通过 | 隔离 `mvn -o compile` 退出 0（§2） |
| GEN-014 迁移 | 不适用（登记） | 列在第 10 段 V408；随 CX-U08-001 删除 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | 版本行 V0.01；OpenAPI 段（第 14/15 段）含该 POST，随删除同步去掉 |
| GEN-017 分类 | 通过 | CX-U08-001 归「添加（操作员已答删除）」，引用 U04-001，不再问 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| 其余 M009 映射行（BSM/PGM/BEQR 69 项） | 通过／不适用 | 本段仅自营布尔插拔，无商户档案六类合作方式、三主体、业态、存档删除范式等条款的实现面改动；M009 已定稿项不在本段重复登记 |

## 6. 本次 Findings（临时 ID CX-U08-00N；正式 ID 由 B2 分配）

### CX-U08-001 · 添加（操作员已答删除，questions/U04-001.md）· 发布前须修：是

- 位置：`JdbcPlatformRepository.java:90/:97/:123/:135/:150/:166/:219/:396/:532/:622`（`is_self_operated` 列插拔 10 处）、`MerchantRecord.java:36-44`（`selfOperated` 字段、25 参兼容构造、`withSelfOperated`）、`PlatformController.java:86-92`（`POST /merchants/{merchant_id}/self-operated`）、`PlatformService.java:176-191`（`setMerchantSelfOperated`）、`:655`（`updateMerchant` 回读保留布尔）、`dto/MerchantSelfOperatedRequest.java:4`。
- HTML 原句：BSM L701/PGM L495「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；BSM L1433/PGM L578「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」。
- 判据：GEN-001、GEN-008、GEN-017；依据类型：HTML 原句 + 操作员 2.2 答复。
- 事实：本段全部 +61/−15 都是第 4 段 `selfOperated` 布尔（可与六类合作方式分叉的第二「自营」开关）的后端实现面；操作员 2026-09-09 已答「删除」。
- 工程缺陷：是。建议登记：是（引用已答复，不再问）。最小处置（给发布者）：本段五文件的自营插拔整体不合入（删除 `is_self_operated` 列插拔、`MerchantRecord.selfOperated`/`withSelfOperated`/25 参构造、控制器端点、服务方法、DTO），第 5 段改读 `cooperationForm == "self_operated"`（U05 定稿），第 10 段 V408 去掉该列，OpenAPI 段去掉该 POST；不在本轮修复。

### CX-U08-002 · 技术性 · 发布前须修：是

- 位置：`PlatformService.java:183-190`：`executeWrite("merchantSelfOperated:"+id, …, "merchant.self_operated_changed", "merchant.self_operated_change_failed", …)` 的 lambda 内再次调用 `auditEventService.recordSuccess("merchant.self_operated_changed", …)`；main `evidence/AuditedOperationExecutor.java:130`（executeWrite 成功路径已 `recordSuccess(operation.successEvent(), …)`）。同类写路径 `archiveMerchant`/`unarchiveMerchant`/`createBusinessEntity`（`:208-245`、`:104-135`）均不在 lambda 内另记。
- 判据：GEN-011、GEN-008；依据类型：代码事实。
- 业务影响：一次自营标注变更写两条 `merchant.self_operated_changed` 成功事件（一条带 self_operated/reason 明细、一条为执行器通用记录），审计台账计数与追溯口径失真。
- 无业务发明理由：去重是执行器既有范式，不新增规则。
- 工程缺陷：是。建议登记：是。最小处置：随 CX-U08-001 一并删除即消失；若发布者违背 U04-001 保留布尔，则去掉 lambda 内的 `recordSuccess`，事由通过 executeWrite 的请求载荷留痕。不在本轮修复。

### CX-U08-003 · 技术性（分叉，main 已修 M009）· 发布前须修：是

- 位置：本段四个既有文件；§2 blob 对照：基线 ≠ HEAD（M009 修复：`PlatformService` +198/−61、`JdbcPlatformRepository` +34/−22、`MerchantRecord` +12/−1、`PlatformController` +9），未发布版 = 基线 + 自营插拔。
- 判据：GEN-002、GEN-008、GEN-013；依据类型：git 事实。
- 业务影响：若以未发布整文件覆盖 main，M009-001…（六类合作方式白名单 `MERCHANT_COOPERATION_FORMS`、合同/结算主体同步、创建/更新路径重写、存档删除范式修正）全部回退；HTML BSM L701 六类、L1433 视同公司自有等已定稿修复失效。
- 工程缺陷：是。建议登记：是。最小处置：按 CX-U08-001 删除后本段无净改动，不合入；任何后续合并必须以 main 445a49f4 为底做三方合并，禁止整文件/整树覆盖。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`MerchantSelfOperatedRequest` 无 `@Valid`/约束注解，空值校验在服务层（`:180`）；与 `MerchantArchiveRequest` 范式不同。随 CX-U08-001 删除失效。
- OBS-2：`saveMerchant`（`:617-658`，`updateMerchant` 路径）用 19 参构造重建记录，`archivedAt/deleteStatus` 等取默认值——更新档案会抹掉存档/删除状态；基线既有，main HEAD 的同名路径（`:759`）仍以 `new MerchantRecord(` 重建，是否已由 M009 处理归 M009 核查（main 既有），本材料只加 `withSelfOperated` 未加重。
- OBS-3：`updateMerchant` 内 `repository.findMerchantById` 二次查询只为保留布尔，删除后消失。
- OBS-4：自营变更事由只进审计明细，不落商户列（存档走 `setMerchantArchiveReason`）；HTML 对该事由沉默。
- OBS-5：`AuditedOperation`/`executeWrite` 的 `businessId` 分派与既有范式一致；`MERCHANT_BUSINESS_FORMATS` 六业态校验为基线既有。
- 技能：`using-superpowers`、`verification-before-completion`（编译实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。5 文件全文三线审计、87 项判据逐项核对；发现 1 项添加（CX-U08-001，操作员已于 U04-001 答「删除」，本段引用不再问）、2 项技术性须修（CX-U08-002 重复审计、CX-U08-003 分叉 main M009）、5 项观察；无 HTML 修改申请；隔离编译绿；未修源、未提交/推送、未写任何 complex_test* 库。本报告落盘前未读取 `U08-b1-grok-raw.md` 任何正文。
---

# U08 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.58「当前 U08 B2-r1 整批候选」U08-001…006；modules/U08-b1-grok-raw.md V0.01（39529bytes SHA1ea87cac35dc91138db42ee68eaf120eb45152ff0fe021390ccb2135fd8add60）；本文件 B1 段（26725bytes SHAfb1a1e5706598e5907ec3a976deb024be8e760eb7994f4d99ccd4cf6ba9c7bf6）。2026-09-09T22:57:53Z
- 席甲 B2 核实（只读，见 ledger V2.65 U08 节）：四文件 main blob ≠ 基线 ≠ 未发布（席甲 B1 §2 numstat 基线→HEAD 34/22、12/1、9/0、198/61）；Grok 叠 main 编译失败（`parentEntityId` 已删于 M009-012）与其覆盖回退 16 项清单经 findings M009 定稿节对照成立；`AuditedOperationExecutor.java:130` 记成功事件（席甲 B1 §2 探针）；76 文件内无 Platform* 测试（清单检索）。

## 勘误（席甲 B1 段）

- 席甲 B1 CX-U08-003 只写「M009 修复面回退」未逐项列出 16 个已定稿 ID，亦未指出 `parentEntityId` 已删导致叠 main 编译失败；B2 采纳 Grok 清单。席甲 B1 未把「无端点测试」单列（GEN-003 行写「由 U12 登记」），B2 采纳 Grok GR-005 为 U08-005。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 本段 +61/−15 全为自营布尔实现面；操作员已答删除；处置整体不合入 |
| 002 | PASS | 四文件 main 已修 M009 16 项、叠 main 编译失败；以 main 为底且净发布物为空最懒 |
| 003 | PASS | 同根 U04-002；随 001 消失，违背时四件同批 |
| 004 | PASS | lambda 内 recordSuccess + 执行器再记，经 `AuditedOperationExecutor:130` 核实 |
| 005 | PASS | 清单内无 Platform* 测试；U11 用例不断言端点 |
| 006 | PASS | 观察归并未改写双方原意；席甲 OBS-2 以 Grok B2 对 main 源码的裁定为准 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 席甲 OBS-2 改写为：未发布 `saveMerchant`（`:635-655`）以 19 参重建、抹存档/删除字段；main `:757-784` 已是 `requireMerchant` + 25 参保留 archive/delete/createdAt（M009-001/002 已在 HEAD 落地）；该回退面已在 U08-002 清单内，不得写成「main 未修、归 M009 核查」。
2. 006 抽样第 3 条（`requireDate`/`readObjectMap`）读作「并入 002（M009-016，main 已删、覆盖会带回）」，不与 2/4/5/6/12 捆成「main 既有须修否」。
3. 002 回退清单口径：16 行 / 19 个 M009 定稿 ID。
4. 001 六类闭集主锚用 BSM L701 / PGM L498 / BEQR L231；PGM L495 原文「等合作方式」不单独当闭集句。
5. ledger U08 开工节所写「+75/−16」为预注旧数（含第 9 段），本段实算 +61/−15（席甲 B1 §2 numstat）；定稿节勘误，不改开工节原字。

## 结论

席甲 B2-r1 票：**PASS**（候选 U08-001…006 成立；发布前须修「是」5 / 「否」1；添加 1（U04-001 已答删除，不再问）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 59779bytes SHA5389db56ca4a80344cdf9f3c4da1766ea9b5a98d96af6209a813109e244faead，前 39529bytes 原字节不变）。
