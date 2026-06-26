# 招商获客业务域 PRD（V1.0·据需求重建）

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

## 4. 品牌/客户库（4 类 + 去重 + 黑名单拦截）
- 4 类：意向品牌 / 储备品牌 / 合作品牌 / 黑名单品牌。
- **用途（全 4 项）**：招商目标清单（主动找）/ 线索去重（建线索时按品牌查重、提示已有）/ 调铺·汰换候选池 / 业态规划参考。
- **黑名单**：招商主管可拉黑品牌；**拉黑后，同品牌新建线索/意向被拦截**（阻断 + 提示，需更高权限放行才可继续）。

## 5. 跟进
- 跟进记录：每次跟进记 时间 / 方式（电话/到访/微信…）/ 内容 / 下次跟进计划时间。
- **提醒**：线索或意向 **3 天未跟进**，系统自动提醒负责人（进负责人待办）。

## 6. 线索→正式商户档案（签约转换）
- 触发：意向「待签约」→ 签约成功。
- **自动带入字段（全部）**：联系人 + 联系方式；意向业态 + 面积 + 位置；报价租金条款；跟进历史记录。
- **四类主体**：默认同一公司——转商户时录 1 个主体，经营对象/运营主体/合同主体/结算主体四类**都指向它**；**后续可拆**成不同公司（复用现有 business_entity_bindings 唯一绑定）。
- 转换后线索状态→已签约，生成正式 merchant 档案，喂入合同环（合同 draft 引用该商户 + 报价条款）。

## 7. 权限点（最小颗粒，复用现有权限体系）
- `leasing.lead.manage`（线索增改/分配，medium）
- `leasing.intent.manage`（意向推进，medium；关键阶段推进需主管确认）
- `leasing.brand.manage`（品牌库维护，medium）
- `leasing.brand.blacklist`（拉黑/解黑品牌，high·需审批——属高危授权范围「提权/敏感」之外的业务高危，走主管复核）
- `leasing.convert`（签约转商户，high·需审批）
- 主管确认/审批复用现有 supervisor_review 机制。

## 8. 数据模型（迁移 V161，对齐 §1 枚举落 CHECK）
- `leasing_leads`(id, source CHECK 4 值, contact_name, contact_phone, intended_business_type, intended_space_id, intended_area, owner_user_id, status CHECK 7 值, lost_reason, project_id, last_follow_up_at, created/updated_at)
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

## 11. 验收用例（G/W/T 摘要）
- 建线索同品牌已黑名单 → 拦截提示。
- 意向推进到「方案报价」无主管确认 → 阻断。
- 线索 3 天无跟进 → 负责人收到提醒。
- 意向签约 → 自动生成商户档案带全字段 + 四类主体指向同一主体。
- 跨综合体：A 项目招商员看不到 B 项目线索。
