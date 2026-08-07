# 招商获客业务域 PRD（V1.0·据需求重建）

> 2026-07-04 逻辑一致性修订：对齐 `merchant-space-contract-model-v0.1.md` 当前实现模型（四类主体绑定落点=`business_entity_bindings`，非 merchants 三列）+ 明确「后续可拆」的可拆边界与不强一致口径；最终业务解释仍以三份业务 HTML 为准。
>
> 生成：2026-06-25。依据：M1 需求采集（`complex-m1-requirements-20260625.md` §1）+ 三份业务 HTML §10（线索/意向/品牌库枚举）+ PGM #merchant「招商前链」。本文是推倒重建（feat/real-estate-rebuild-from-requirements）招商域的产品基线，向业务 HTML 对齐；冲突以业务 HTML 为最终解释权。
> 重建动因：旧骨架 V164（leasing_leads/offers）系统性简化——线索 5 态≠HTML 7 态、意向 5 阶段被压进报价状态机、品牌库/跟进表全缺。本文据用户需求重建，纠正之。

## 0. 定位与边界
- 招商 = 地产履约链最前端：线索 → 意向 → 签约 → 自动转正式商户档案 → 喂入合同环。
- 数据隔离 = 综合体（项目）级；所有招商对象带 `project_id`。
- 不做：第三方招商代理对接（来源不含代理）；线索自动分配（本期主管手动派）。

## 1. 业务对象与状态枚举（对齐 HTML §10，重建须逐字落 DB CHECK）
| 对象 | 枚举 | 取值 |
|---|---|---|
| 招商线索 leasing_lead | 线索状态（7 态） | 待跟进 / 跟进中 / 已转意向 / 洽谈中 / 已签约 / 已流失 / 已搁置 |
| 招商意向 leasing_intent | 意向阶段（5 阶段，**独立流转**，非报价状态机） | 初步接触 / 需求确认 / 方案报价 / 商务谈判 / 待签约 |
| 品牌/客户库 brand | 品牌库类型（4 类） | 意向品牌 / 储备品牌 / 合作品牌 / 黑名单品牌 |
| 跟进记录 follow_up | —（流水） | 跟进时间/方式/内容/下次计划 |

## 2. 线索管理
- **来源（4 类，录入即打来源标签）**：主动拓展（招商员外出）/ 官网·小程序在线留资 / 老商户·合作方转介绍 / 展会·行业活动。
- **分配**：招商主管手动指派给招商员（不自动分、不抢单、不共享池）。线索带 `owner_user_id`（负责人）。
- **状态流转**：待跟进→跟进中→已转意向→洽谈中→已签约；旁支 已流失 / 已搁置（可从多数态进入，留原因）。
- 「已转意向」时生成 leasing_intent（进入 §3）。

## 3. 意向管理（5 阶段独立流转 + 关键阶段卡控）
- 5 阶段顺序推进：初步接触 → 需求确认 → 方案报价 → 商务谈判 → 待签约。
- **关键阶段卡控**：进入「方案报价」「商务谈判」需**主管确认**才可推进；其余阶段招商员自由推进。卡控走现有主管复核机制（留审批人+时间+原因）。
- 「待签约」→ 签约：触发 §6 转商户。
- 意向记录意向业态、意向铺位/空间、意向面积、报价租金条款（喂入转商户与合同）。
- **意向业态词表（V348 + V384）**：与商户业态同一套六值——酒吧 bar / 餐饮 catering / 零售 retail / 游乐 amusement / 传媒 media / 其他 other（`leasing_leads.intended_business_type` 加 CHECK `chk_leasing_leads_business_format`，线索阶段可留空）。签约转商户时该值直接落成 `merchants.merchant_type`，留空则落「其他」，由招商在商户档案改正——词表不一致会让签约在商户业态 CHECK 上被拒。

## 4. 品牌/客户库（4 类 + 去重 + 黑名单拦截）
- 4 类：意向品牌 / 储备品牌 / 合作品牌 / 黑名单品牌。
- **用途（全 4 项）**：招商目标清单（主动找）/ 线索去重（建线索时按品牌查重、提示已有）/ 调铺·汰换候选池 / 业态规划参考。
- **黑名单**：招商主管可拉黑品牌；**拉黑后，同品牌新建线索/意向被拦截**（阻断 + 提示，需更高权限放行才可继续）。

## 5. 跟进
- 跟进记录：每次跟进记 时间 / 方式（电话/到访/微信…）/ 内容 / 下次跟进计划时间。
- **提醒**：线索 **超过约定天数未跟进**（默认 3 天，后台「提醒与留存天数配置」ADM-ANA-011 可改，键 `leasing.lead_follow_up`，V219），系统提醒负责人。
  - **落点（2026-07-28 用户拍板"接进工作台"）**：招商人员一进「招商工作台」ADM-LEAS-001 就先看见页首提醒条「有 N 条线索超过约定天数没跟进」，
    并有「该跟进」KPI 卡；点任一处直接聚焦到那几条线索（`?kpi=leads&subset=due`）。到期判定在后端 `GET /api/v1/leasing/leads/follow-up-due`，
    前端不复刻天数口径——后台改天数，这张单子立刻跟着变。
  - **意向侧未跟进提醒（2026-08-04 口径更正·实现待对齐，登记缺口）**：后端 `GET /api/v1/leasing/leads/follow-up-due` **只判线索**，意向侧无到期判定、工作台也无意向提醒。原「暂未接、需要时再单开」的口径**作废**——**意向与线索同属获客链、同受「超过约定天数未跟进」约束，属必须补齐的缺口**：`follow-up-due` 须补 **intent 分支**（按 `leasing_follow_ups.ref_type='intent'` 的最近跟进时间与同一套后台可配天数判定，天数口径与线索共用、前端不复刻），并在工作台提醒条与「该跟进」KPI 卡中与线索**分列可辨**。**本轮只登记口径与缺口，不改代码、不改 OpenAPI。**

## 6. 线索→正式商户档案（签约转换）
- 触发：意向「待签约」→ 签约成功。
- **自动带入字段（全部）**：联系人 + 联系方式；意向业态 + 面积 + 位置；报价租金条款；跟进历史记录。
- **四类主体（当前实现模型见 `merchant-space-contract-model-v0.1.md`）**：四类=经营对象（商户自身）/ 运营主体 / 合同主体 / 结算主体；三个主体统一落在 `business_entities`（`entity_role` 区分，无独立「项目公司/SPV」表）。
  - **当前绑定载体 = `business_entity_bindings`（`binding_type=four_party`）**，每商户至多一条 `active` 绑定（`uq_business_entity_bindings_active_merchant`；重复激活报 `409 ACTIVE_BINDING_EXISTS`）。转商户初期三主体可都指向同一 `business_entity`。
  - **可拆边界**：「后续可拆」= 新建一条 active 绑定替换旧绑定，指向不同 运营/合同/结算 主体；**不是**在 `merchants` 的 `*_entity_id` 快照列上改写。`merchants.business_entity_id/contract_entity_id/settlement_entity_id` 仅为档案快照。
  - 合同签署时另在 `contracts.contract_subject_id/operation_subject_id/settlement_subject_id` 固化该合同主体快照，与商户当前 active 绑定**可不一致**（无外键强绑），勿假定二者恒等。
- 转换后线索状态→已签约，生成正式 merchant 档案，喂入合同环（合同 draft 引用该商户 + 报价条款）。
- **实现保证（AUD-002/003 修复，2026-07-21）**：签约转换 = 一次原子业务事务——`signIntent` 公有入口 `@Transactional`，自动建商户 → 即建 `business_entity_bindings`（`binding_type` 四主体、`active`，稳定幂等键按意向派生）→ 合同 draft → 线索置已签约 → 审计留痕；**任一步失败整体回滚，无孤儿商户/绑定/合同**，线索不误置已签约。幂等重签命中同一商户与同一 active 绑定、不重复建。绑定创建仅在「无既有 `merchant_id`、签约自动建档」路径触发（带既有商户签约，其主体绑定由治理域主体绑定页管理，不在此重建）。
- **合同起草原子性（AUD-002）**：`ContractService.createDraft` 亦已 `@Transactional`——字段/空间校验前置于落库（校验失败零脏写）、空间存在性+租期重叠早检在保存前完成、幂等命中(重放)也必过业务校验后才返回既有草稿；失败草稿整体回滚，重试走完整校验、无半成品可复用。


## 6A. 签约后到期 / 变更 / 退出衔接（P2，2026-07-28）

> 闭合 BSM-0267 `expiry_exit`。招商 PRD 负责「收到签约」；租约履约/退租细节以 real-estate / 合同履约 PRD 为准，本节定义**必须闭环的交接**。

1. **意向/线索不会自动变成无限期占位**：签约转换成功后，线索/意向关闭为「已签约」类终态（名称以枚举为准），不得长期停在可编辑获客态。  
2. **合同到期前**：系统须能按可配提前期产生跟进/预警任务（到期提醒），避免静默过期。  
3. **到期路径（三选一，须留痕）**：  
   - **续约/变更**：走合同变更或新签流程，保留原合同与商户档案关联；  
   - **退租结算**：触发/转交退租与费用结清流程（租金、物业、能源、押金等以合同履约域为准）。**押金治理与退租结算细则见 `real-estate-full-chain-domain-prd-v0.1.md` §2「退租结算与押金退还」「押金治理」**（多级多部门审批全过后系统按结算清单自动落账、押金退还以算账完成为准且不早于等待期届满、押金不得日常抵扣欠费等；**2026-08-06 新口径：押金金额 ＝ 收取月数 × 签约时月租金（只乘月租）、收取月数下限三个月、清退硬线按「押金减当期欠费不足以覆盖 30 天当前月均应缴全费用」判、欠缴月数按「欠费总额 ÷ 当前月均应缴费用」折算**——H-23／矛盾7／矛盾9），本 PRD 不重复正文，避免两处口径漂移；  
   - **逾期未决**：到期后未续未退则进入「待处置」并限制新的超范围承诺（具体准入限制对齐合同/空间域）。  
4. **退出完成**：退租结算完成后，招商侧商户合作状态更新为退出/结束类状态，黑名单规则仍可拦截再入。  
5. **权限/证据**：到期处置、续约、退租发起均需授权角色；关键节点写审计事件与关联合同 ID。


## 7. 权限点（最小颗粒，复用现有权限体系）
- `leasing.lead.manage`（线索增改/分配，medium）
- `leasing.intent.manage`（意向推进，medium；关键阶段推进需主管确认）
- `leasing.brand.manage`（品牌库维护，medium）
- `leasing.brand.blacklist`（拉黑/解黑品牌，high·需审批——属高危授权范围「提权/敏感」之外的业务高危，走主管复核）
- `leasing.convert`（签约转商户，high·需审批）
- 主管确认/审批复用现有 supervisor_review 机制。

## 8. 数据模型（迁移 V161，对齐 §1 枚举落 CHECK）
- `leasing_leads`(id, source CHECK 4 值, contact_name, contact_phone, intended_business_type, intended_space_id, intended_area, owner_user_id, status CHECK 7 值, lost_reason, project_id, last_follow_up_at, created/updated_at)

> **联系人手机号 PII 加密（P0-A / ★SEC-04，迁移 V339，2026-07-27）**：`leasing_leads.contact_phone` 落库为 AES-GCM 信封密文（`enc:v1:`），仓库层写时加密、读时解密，业务层拿到的仍是明文；数据库 `CHECK` 约束（`NOT VALID`）卡死任何明文新写入，仓库以外的手写 SQL / 批量导入绕不过；存量明文由启动期 `MultiPartyContactPhonePiiBackfill` 幂等回填就地加密（三层防二次加密）。读端按权限脱敏：持 `pii.contact_phone.view`（高危，默认仅平台管理员）才见明文，其余见掩码（前 3 后 4）。本域无按手机号的后端检索路径，加密不影响任何既有查询，也未建盲索引。统一口径见 `compliance-and-personal-information-protection-requirements-v0.1.md` §6.1。

- `leasing_intents`(id, lead_id FK, stage CHECK 5 值, intended_space_id, intended_area, rent_terms JSONB, last_stage_change_at, project_id, created/updated_at)
- `leasing_brands`(id, brand_name UNIQUE-ish, brand_type CHECK 4 值, blacklist_reason, project_id, created/updated_at)
- `leasing_follow_ups`(id, ref_type CHECK[lead,intent], ref_id, method, content, next_plan_at, operator_user_id, created_at)
- 权限种子 + role_permissions（仿现有）。

## 9. 接口（OpenAPI，重建 /leasing/*）
- 线索：`GET/POST /leasing/leads`、`POST /leads/{id}/assign`、`POST /leads/{id}/status`、`POST /leads/{id}/to-intent`
- 意向：`GET/POST /leasing/intents`、`POST /intents/{id}/advance`（推进阶段，关键阶段触发主管确认）、`POST /intents/{id}/sign`（签约→转商户）
- 品牌库：`GET/POST /leasing/brands`、`POST /brands/{id}/blacklist`
- 跟进：`GET/POST /leasing/follow-ups`
- 所有写操作幂等 + project scope + 审计留痕。

## 10. UI / 验收（full-admin 矩阵补 ADM-LEAS-001..004）
- ADM-LEAS-001 线索看板（来源/状态筛选、分配、转意向）
- ADM-LEAS-002 意向推进（5 阶段流转、关键阶段确认）
- ADM-LEAS-003 品牌库（4 类、去重提示、黑名单拉黑）
- ADM-LEAS-004 签约转商户（字段映射确认、四类主体绑定）

> **页面 ID 口径（2026-08-04 统一）**：本域页面 ID 一律用 **`ADM-LEAS-` 前缀**（与 `docs/full-admin-ui-acceptance-matrix-v0.1.md` 的 UI-ADM-LEAS-001..004 一致），§5 原「ADM-LEASE-001」已改为 **ADM-LEAS-001**。**（待同步）** `docs/full-admin-page-permission-field-matrix-v0.1.csv` 现为 **ADM-LEASE-001「招商工作台」（一页合并覆盖线索/意向/品牌/签约四项能力）+ ADM-LEASE-002「租赁状态看板」**——**前缀与编号含义两处都与本 PRD/UI 矩阵不一致**（LEAS-002 在本 PRD 指「意向推进」，LEASE-002 在 CSV 指「租赁状态看板」）；**本轮不改 CSV**，登记为权限矩阵 CSV 侧待同步项（含「四页拆分 vs 一页工作台」的编号归并口径）。

## 11. 验收用例（G/W/T 摘要）
- 建线索同品牌已黑名单 → 拦截提示。
- 意向推进到「方案报价」无主管确认 → 阻断。
- 线索**超过后台配置的未跟进天数**无跟进 → 负责人收到提醒（业务参数，**按项目可配**，**出厂默认三天**，键 `leasing.lead_follow_up`／ADM-ANA-011；验收须同时覆盖「改配置后判定随之改变」，不得把三天写死进用例）。
- 意向签约 → 自动生成商户档案带全字段 + 四类主体指向同一主体。
- 跨综合体：A 项目招商员看不到 B 项目线索。

## 变更日志

| 日期 | 类型 | 章节 | 内容 |
|---|---|---|---|
| 2026-07-28 | 增 | §6A | 到期/变更/退出衔接（P2 BSM-0267） |
| 2026-08-04 | 增 | §6A | 退租结算处补押金治理与退租结算细则的交叉引用，指向 real-estate PRD §2，避免两处重复正文（CL-24） |
| 2026-08-04 | 改 | §5／§10／§11 | 页面 ID 统一为 `ADM-LEAS-` 前缀（§5 ADM-LEASE-001 → ADM-LEAS-001）并登记权限矩阵 CSV 侧待同步；§11 验收「3 天」改为「按后台配置的未跟进天数（按项目可配，出厂默认三天）」；§5 意向到期提醒由「暂未接、需要时再单开」改为「实现待对齐（登记缺口）：`follow-up-due` 须补 intent 分支」；文末变更日志补「## 变更日志」标题与表头（F2-13…F2-16） |
| 2026-08-06 | 改 | §6A | 押金交叉引用补 2026-08-06 新口径要点（押金＝收取月数×签约时月租、下限三个月、清退硬线按 30 天当前月均应缴全费用判、欠缴月数按金额折算），正文仍以 real-estate PRD §2 为准 |
