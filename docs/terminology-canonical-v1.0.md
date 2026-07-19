# 组织实体与结算主体 术语权威表 v1.0

> 结算重构 批 0 产出。**权威口径来源 = 现行迁移与代码(origin/main),非任何单份 PRD。** 用于统一全部 PRD/设计文档对"四层实体 / 六类业态主体 / 结算术语"的称谓,消解"综合体/集团/项目/组织"混用。
> 关联:`merchant-space-contract-model-v0.1.md`(数据模型权威 ER)、`settlement-rebuild-prd-v1.0.md`(结算口径权威)。

## 一、四层组织实体(定死)

| 层 | 名词 | 定义 | DB 落点 |
|---|---|---|---|
| L1 | **平台** | 最大管理实体,可辖多城市多项目 | `business_entities`(entity_role=platform) |
| L2 | **项目公司** | 某城市的项目经营体(如长沙 NEXT PARK) | `business_entities`(entity_role=project_company) |
| L3 | **管理公司 / 业态主体(SPV)** | 项目公司与商户之间的分账主体 | `business_entities`(entity_role=management_company / spv …) |
| L4 | **商户** | 具体经营单位 | `merchants` 表 |

## 二、DB 落点铁律(勿臆造独立表)

- **无 `organizations` / `org_type` 表**;四层主体统一用 **`business_entities` 一表多角色**(`entity_role` 文本,无 CHECK)承载。
- **层级父链(V318)** = `business_entities.parent_entity_id`:L3 管理公司 → L2 项目公司,L2 → L1 平台,L1=NULL。「一个项目公司下多个管理公司主体」= 多行 `parent_entity_id` 指向同一项目公司。
- **`projects` 表 ≠ 项目公司** —— 它是**业态经营单元 / 场馆**(`venue_type` = bar / amusement / ticketing);`project_id` = 综合体维度。两者是两回事,勿混。
- **四主体绑定** = `business_entity_bindings`(binding_type=four_party;每商户至多一条 `active`,重复激活报 409 `ACTIVE_BINDING_EXISTS`);"后续可拆" = 换一条新 active 绑定,不是改 `merchants` 快照列。
- `merchants` / `contracts` 的 `*_entity_id` / `*_subject_id` = 档案 / 签约时快照,与当前绑定可不一致。
- 商户 ↔ 铺位 ↔ 合同 = `merchants`(1)-<`contracts`(N,续约链)-<`contract_spaces`>- `spaces`(多对多);`spaces.status` 仅 `active` / `unavailable`(6 态租赁着色是看板派生视图,非 DB status);无转租(换租户=先退租再招商)。

## 三、六类业态主体(管理公司层)专名 + 收入归属 + 税务属性

依据结算重构 PRD §4 / 财务底稿 `03_收入流向图`:

| 专名(L3) | 收哪些收入 | 税务属性 |
|---|---|---|
| **娱乐运营平台** | 通票(平台留存 20% + SPV 分账 80% 余额) | 平台运营服务费 + 文化事业建设费 |
| **酒吧 SPV(×N,可配)** | SPV 分账池 + 非散台服务费(仅包厢/卡座 10%) | 娱乐服务 |
| **中央餐饮公司** | 订台低消 / 二次消费 / 额外酒水(开关关→归对应酒吧) | 餐饮服务 |
| **游乐公司** | 游乐票 | 游乐服务 |
| **传媒公司** | 演出票 | 文化服务 |
| **资产公司** | 商业租金 | 不动产租赁 |

**收入归属落库(V319)**:六业态收入流 → L3 管理公司主体的映射落 `revenue_stream_attribution`(收入流枚举 `all_pass_ticket` / `table_dining` / `bar_service_fee` / `amusement_ticket` / `performance_ticket` / `venue_rent` → `management_entity_id`),后台可配,解析优先级 **项目特例 > 平台默认 > 未配置(null)**。通票分账已接:`settlement_bills.attributed_entity_id` 由 `settleMonth` 按 (项目, `all_pass_ticket`) 解析标注(仅记归属,不改金额);其余 5 流经同一 `RevenueStreamAttributionRepository.resolveManagementEntity` 解析,随各自结算路径上线时接入。

## 四、混用词纠正规则

| 旧词 | 纠正 |
|---|---|
| "综合体" | 指单个经营体 → **项目公司**;多项目汇总 → **平台 / 集团视图** |
| "集团" | **平台 / 集团视图**(多项目汇总,当前无独立实体) |
| "项目" | 按语境区分:**项目公司**(经营体)vs **`projects` 表**(业态场馆);文中明确指出 |
| "组织 / organization" | 权限侧保留 `organization_id`(仅角色/用户权限用);业务主体一律 `business_entities` |

## 五、结算核心术语(结算重构 PRD 权威)

- **按核销结算 / 未核销归项目**:通票权益在核销时才结算给商户/SPV;未核销部分归项目公司(平台留存)。
- **平台留存 20% = 综合服务费**:所有门票(含通票)都过一道,费率可配。
- **通票赠酒(两段口径)**:①先留赠酒成本(每访问点位默认 3 元,补偿酒吧酒成本,可配);②剩余按各酒吧协议"每进一客给几元"分账(`by_venue_per_guest`,`spv_per_guest_rate` 按门店可配)。核销才结、未核销归项目。
- **中央餐饮开关**(`central_catering_enabled`,默认开):开→酒水销售归中央餐饮 + 按销售额返场地费给酒吧 SPV(可配);关→酒水直接归该酒吧。
- **可配置铁律**:一切业务策略(比例/费率/单价/阈值/限额)后台可配、禁硬编码;成本 = 真实费用归集,不用"收入×成本率"估算。
