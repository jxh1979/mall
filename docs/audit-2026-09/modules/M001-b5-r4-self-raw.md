CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M001 B5-r4 — Codex 修复后整模块全量确认
版本：V0.01
日期：2026-09-08 UTC
控制：160-audit-prompt.md V0.17；modules V0.04的471模块计划，runbook V0.02，criteria-lock V0.11。
正本：160:/home/james/complex-management，main HEAD=242f487233d028173cb9198eecd079f5d877405c。
本阶段在最新B2双PASS定稿后执行，当前队列为空，跳过新的B3/B4，直接B5-r4。B3累计4；本次A/D额外1次已用尽，无第五次修复授权。本次未改HTML、源码或锁。

## 范围、证据方法与独立性

本次两份权威HTML全部范围：BEQR308行、BSM2032行，合计2340；全部335映射判据：BEQR197+BSM120+GEN18。BEQR SHA256=4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66；BSM SHA256=a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa。PGM只作已有交叉上下文，不开M002。76未发布文件专项不作本模块产品实现PASS证据。
本会话已完整分17段读取两份源文，随后全部文件字节证明未读变化仅已批准A/D两行，该两处及相关完整业务段已重新阅读。新基准B1后两份文件字节再无变化。本次沿用这些已完整读过的相同原文字节，重新逐335判据作全模块三线语义核对，并实跑当前摘录/结构/链接/原JS与金额正反例，具体输出如下；不是只审两行diff，不冒称本回合重新逐页Read2340行或执行业务服务端测试。
先派Grok B5到指定新raw，再由根完成本报告；本报告完整落盘前不读取本次Grok B5正文或CLI JSON。本报告不引用另一席本次意见为依据，无第三席。两席旧阶段PASS不替代本次B5审查结论。
操作员最新业务解释逐字为“b c两项原文不冲突。就是我的原意。”本次两席派发均明确该输入；B/C是预期业务规则、无需修复，不是容忍已确认缺陷。A/D修复已完成，先前目录/页脚授权继续有效；不能把HEAD累计diff当本次未批改动。当前16个永久ID处置已B2双PASS，以下四项只复核004/014/015，无新增finding、无添加项或新的HTML修改申请。不扩大可配范围、不另写业务例外。

## 第一性原理线
1. 为什么存在：BEQR提供业务枚举与边界，BSM提供流程、责任与入口关系，两份各有独有上位内容；重述有定位用途，不是可删的整文件重复。全局分类原则及两个具体分类句均是现行权威原文，需要能够同时解释下游实现。
2. 哪些声称已实际验证：本模块是需求文档，实际跑HTML全结构/链接、27目录目标的原始JS、1—10家比例/并列/尾差、99%变异拒绝等类型4检查；业务支付、工资、第三方、设备等实现未运行，不冒称通过。
3. 哪个前提已经得到用户验证：本席先前将L521与L1020/1068理解成分类配置冲突；操作员最新明确B/C原文不冲突、就是本意。审计者不能用自己的不同理解推翻业务所有者的明确说明。按当前原文和此项解释确认B/C均为预期业务，撤销错误冲突判断；不进一步猜测按项目可配具体允许哪些后台操作，不改全局原则，不新增例外文字。

## 顺从式线与全部GEN判据
317条HTML冻结摘录全部匹配当前文件和来源位置；同时完成语义审查。BSM001/052/054按操作员最新B/C业务含义澄清核对，不再判冲突；其他条目按已读原文、交叉语义及当前实际检查重新对照，未确认缺陷。
| 判据 | 本模块证据与结论 |
|---|---|
| GEN-001 | 两份权威文件用途不同；纯展示 CSS/导航为正常阅读服务，无业务发明；不因重述规则就删业务正本。 |
| GEN-002 | BSM 原脚本在真实 27 个导航目标的 DOM 等价输入上执行；初始和每目标激活均断言通过。文档中业务行为不冒充已执行代码。 |
| GEN-003 | 机械断言实跑；将比例 100% 改为 99% 的内存反例确实失败，非永绿断言。 |
| GEN-004 | 本模块静态文本无用户写入口；动态脚本选择器取本文件静态 href，全部解析到实际 ID；业务校验规则按各锚点核对。 |
| GEN-005 | 逐读默认拒绝、十一端、职责与敏感信息范围；具体明定的密钥/人工查验例外不误判为普通权限放行。未执行服务端授权测试。 |
| GEN-006 | 核对钱路守恒、来源分栏、租赁排他、券与支付共同生效、挂起取消与付款互斥；结构解析无标签失配。 |
| GEN-007 | 核对转赠三动作先成功者、多人/多次票单位、出场重复不重复扣、幂等逐笔密钥例外及并拆台整体成功；未声称产品并发测试通过。 |
| GEN-008 | 两份中的枚举与作用域逐段对照；流程概述并非完整清单，具体例外与独立子流程按其范围解释。 |
| GEN-009 | 接口未接入口灰显、未出款状态、第三方差异、能力缺失等明确列出；缺配置不视为开启，未发现静默伪成功文案。 |
| GEN-010 | 本模块无可执行外部输入/凭证；人脸、注销、查询、导出与证据查看边界按明文审读；不将本文当法规论证。 |
| GEN-011 | 对照逐笔来源/时点/责任人与不可覆盖记录；安检、人工验龄的特定溯源终点与全局原则按明定例外一并核对。 |
| GEN-012 | 导航每次遍历固定 27 个章节，非开放输入放大；只读静态页面无后台资源持有。 |
| GEN-013 | 文件以 UTF-8 原生打开，不依赖额外网络库；导航用原生 JS。未用“本次无 diff”门禁冒充全量内容审计。 |
| GEN-014 | 不适用执行检查：本模块无 SQL/迁移；迁移正确性留所辖模块。 |
| GEN-015 | HTML 语言与编码声明、标题层次、表格表头、原生链接可键盘访问；导航有效、标签闭合；未执行真实浏览器截图或读屏测试，不夸大覆盖。 |
| GEN-016 | 文档版本V0.07/V0.13、日期和导航正确，317摘录与来源行一致；B/C原文已由操作员明确为预期且不冲突，见CX-03/04。未确认缺陷。 |
| GEN-017 | 原文按冲突→细化→技术性→添加判断：CX-03/04依最新业务澄清为细化、无缺陷；CX-01/02为无业务发明的技术性登记；无添加项。 |
| GEN-018 | 仅使用实际 HTML 原文与本报告所附命令输出；不是以旧 PASS、Grok 意见或代码注释作判据。 |


### 全部 HTML 判据覆盖表

源文件简称：BEQR=docs/business-enum-quick-reference-v0.1.html；BSM=docs/business-system-map-for-operations-v0.1.html。
本表每行的“已核对”指顺从式全文和对抗式作用域对照均完成，结合下方反例与排除说明。冻结摘录逐字匹配的命令覆盖所有 317 行。

| 判据 ID | 源行 | 锚点 | 结果 |
|---|---|---|---|
| BEQR-001 | BEQR:29 | 票种 | 已核对，未确认缺陷 |
| BEQR-002 | BEQR:30 | 票务策略状态 | 已核对，未确认缺陷 |
| BEQR-003 | BEQR:32 | 定价、售票与收款主体 | 已核对，未确认缺陷 |
| BEQR-004 | BEQR:33 | 年龄限制 | 已核对，未确认缺陷 |
| BEQR-005 | BEQR:34 | 通票赠酒下限 | 已核对，未确认缺陷 |
| BEQR-006 | BEQR:35 | 通票类型 | 已核对，未确认缺陷 |
| BEQR-007 | BEQR:36 | 权益类型 | 已核对，未确认缺陷 |
| BEQR-008 | BEQR:37 | 餐饮/零售券两种用法 | 已核对，未确认缺陷 |
| BEQR-009 | BEQR:39 | 资格判定 | 已核对，未确认缺陷 |
| BEQR-010 | BEQR:40 | 订单状态 | 已核对，未确认缺陷 |
| BEQR-011 | BEQR:41 | 票/权益状态 | 已核对，未确认缺陷 |
| BEQR-012 | BEQR:43 | 核销记录状态 | 已核对，未确认缺陷 |
| BEQR-013 | BEQR:44 | 手环入场 | 已核对，未确认缺陷 |
| BEQR-014 | BEQR:45 | 退款单状态 | 已核对，未确认缺陷 |
| BEQR-015 | BEQR:46 | 有效期类型 | 已核对，未确认缺陷 |
| BEQR-016 | BEQR:47 | 售票与退款原则 | 已核对，未确认缺陷 |
| BEQR-017 | BEQR:48 | 到期未核销款 | 已核对，未确认缺陷 |
| BEQR-018 | BEQR:49 | 域归属对照 | 已核对，未确认缺陷 |
| BEQR-019 | BEQR:51 | 跨域通票分配 | 已核对，未确认缺陷 |
| BEQR-020 | BEQR:52 | 酒吧通票可分配池 | 已核对，未确认缺陷 |
| BEQR-021 | BEQR:53 | 收费与免费权益结算 | 已核对，未确认缺陷 |
| BEQR-022 | BEQR:54 | 项目内部结算价 | 已核对，未确认缺陷 |
| BEQR-023 | BEQR:55 | 核销顺序 | 已核对，未确认缺陷 |
| BEQR-024 | BEQR:56 | 游乐核销归属 | 已核对，未确认缺陷 |
| BEQR-025 | BEQR:57 | 门票综合服务费 | 已核对，未确认缺陷 |
| BEQR-026 | BEQR:58 | 动态价与会员价 | 已核对，未确认缺陷 |
| BEQR-027 | BEQR:60 | 人脸验龄数据生命周期 | 已核对，未确认缺陷 |
| BEQR-028 | BEQR:65 | 会员唯一识别 | 已核对，未确认缺陷 |
| BEQR-029 | BEQR:66 | 会员注册 | 已核对，未确认缺陷 |
| BEQR-030 | BEQR:68 | 会员身份状态 | 已核对，未确认缺陷 |
| BEQR-031 | BEQR:70 | 等级与成长值 | 已核对，未确认缺陷 |
| BEQR-032 | BEQR:71 | 等级权益对照 | 已核对，未确认缺陷 |
| BEQR-033 | BEQR:72 | 积分与成长值来源 | 已核对，未确认缺陷 |
| BEQR-034 | BEQR:73 | 票务人次计算 | 已核对，未确认缺陷 |
| BEQR-035 | BEQR:75 | 有效消费与冻结 | 已核对，未确认缺陷 |
| BEQR-036 | BEQR:77 | 积分明细类型 | 已核对，未确认缺陷 |
| BEQR-037 | BEQR:78 | 积分兑换定价 | 已核对，未确认缺陷 |
| BEQR-038 | BEQR:79 | 商品规格 | 已核对，未确认缺陷 |
| BEQR-039 | BEQR:80 | 项目零售参考价 | 已核对，未确认缺陷 |
| BEQR-040 | BEQR:81 | 发券与核销结算 | 已核对，未确认缺陷 |
| BEQR-041 | BEQR:82 | 重复兑换与转赠 | 已核对，未确认缺陷 |
| BEQR-042 | BEQR:83 | 券包状态 | 已核对，未确认缺陷 |
| BEQR-043 | BEQR:84 | 积分结算账期 | 已核对，未确认缺陷 |
| BEQR-044 | BEQR:85 | 会员系统服务费 | 已核对，未确认缺陷 |
| BEQR-045 | BEQR:86 | 储值卡类型与收款主体 | 已核对，未确认缺陷 |
| BEQR-046 | BEQR:87 | 会员忠诚度扩展 | 已核对，未确认缺陷 |
| BEQR-047 | BEQR:89 | 会员注册与开卡 | 已核对，未确认缺陷 |
| BEQR-048 | BEQR:90 | 学生认证 | 已核对，未确认缺陷 |
| BEQR-049 | BEQR:91 | 会员积分活动状态 | 已核对，未确认缺陷 |
| BEQR-050 | BEQR:92 | 积分活动预算守恒 | 已核对，未确认缺陷 |
| BEQR-051 | BEQR:93 | 充值积分 | 已核对，未确认缺陷 |
| BEQR-052 | BEQR:94 | 会员营销边界 | 已核对，未确认缺陷 |
| BEQR-053 | BEQR:95 | 发券对象 | 已核对，未确认缺陷 |
| BEQR-054 | BEQR:100 | 桌台/卡座/吧台位状态 | 已核对，未确认缺陷 |
| BEQR-055 | BEQR:101 | 账单状态 | 已核对，未确认缺陷 |
| BEQR-056 | BEQR:103 | 订台定金状态 | 已核对，未确认缺陷 |
| BEQR-057 | BEQR:105 | 订卡邀请函状态 | 已核对，未确认缺陷 |
| BEQR-058 | BEQR:106 | 点酒方式 | 已核对，未确认缺陷 |
| BEQR-059 | BEQR:107 | 支付方式 | 已核对，未确认缺陷 |
| BEQR-060 | BEQR:108 | 销售提成 | 已核对，未确认缺陷 |
| BEQR-061 | BEQR:109 | 服务员提成 | 已核对，未确认缺陷 |
| BEQR-062 | BEQR:110 | 退菜退酒 | 已核对，未确认缺陷 |
| BEQR-063 | BEQR:111 | 高危动作（需审批/双证据） | 已核对，未确认缺陷 |
| BEQR-064 | BEQR:112 | 出品票据状态 | 已核对，未确认缺陷 |
| BEQR-065 | BEQR:114 | 账单操作 | 已核对，未确认缺陷 |
| BEQR-066 | BEQR:115 | 收银交班状态 | 已核对，未确认缺陷 |
| BEQR-067 | BEQR:117 | 最低消费 | 已核对，未确认缺陷 |
| BEQR-068 | BEQR:118 | 服务费 | 已核对，未确认缺陷 |
| BEQR-069 | BEQR:119 | 小费 | 已核对，未确认缺陷 |
| BEQR-070 | BEQR:120 | 客人存酒有效期与转存 | 已核对，未确认缺陷 |
| BEQR-071 | BEQR:122 | 存酒瓶数 | 已核对，未确认缺陷 |
| BEQR-072 | BEQR:123 | 套餐赠券适用范围 | 已核对，未确认缺陷 |
| BEQR-073 | BEQR:124 | 非低消商品 | 已核对，未确认缺陷 |
| BEQR-074 | BEQR:125 | 隐私桌台权限 | 已核对，未确认缺陷 |
| BEQR-075 | BEQR:126 | 桌台会话与菜单 | 已核对，未确认缺陷 |
| BEQR-076 | BEQR:127 | 桌台计时产品 | 已核对，未确认缺陷 |
| BEQR-077 | BEQR:128 | 门店支付方式 | 已核对，未确认缺陷 |
| BEQR-078 | BEQR:129 | 扫码点单身份 | 已核对，未确认缺陷 |
| BEQR-079 | BEQR:130 | 优惠券自动选择 | 已核对，未确认缺陷 |
| BEQR-080 | BEQR:131 | 优惠券付款预留 | 已核对，未确认缺陷 |
| BEQR-081 | BEQR:132 | 正式挂起订单状态 | 已核对，未确认缺陷 |
| BEQR-082 | BEQR:133 | 正式挂起订单规则 | 已核对，未确认缺陷 |
| BEQR-083 | BEQR:134 | 挂单取消与付款互斥 | 已核对，未确认缺陷 |
| BEQR-084 | BEQR:135 | 挂单坏账损耗 | 已核对，未确认缺陷 |
| BEQR-085 | BEQR:140 | 项目开放状态 | 已核对，未确认缺陷 |
| BEQR-086 | BEQR:142 | 场次状态 | 已核对，未确认缺陷 |
| BEQR-087 | BEQR:144 | 游乐「当天／每天」口径 | 已核对，未确认缺陷 |
| BEQR-088 | BEQR:145 | 容量双阈值 | 已核对，未确认缺陷 |
| BEQR-089 | BEQR:146 | 园区容量 | 已核对，未确认缺陷 |
| BEQR-090 | BEQR:147 | 设备关键/辅助分级 | 已核对，未确认缺陷 |
| BEQR-091 | BEQR:148 | 全园故障与补偿通票 | 已核对，未确认缺陷 |
| BEQR-092 | BEQR:149 | 安全严重度 | 已核对，未确认缺陷 |
| BEQR-093 | BEQR:150 | 巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-094 | BEQR:151 | 越界类型 | 已核对，未确认缺陷 |
| BEQR-095 | BEQR:153 | 团体/包场 | 已核对，未确认缺陷 |
| BEQR-096 | BEQR:154 | 游乐增强阶段 | 已核对，未确认缺陷 |
| BEQR-097 | BEQR:159 | 供应合同模式 | 已核对，未确认缺陷 |
| BEQR-098 | BEQR:160 | 库房分类 | 已核对，未确认缺陷 |
| BEQR-099 | BEQR:161 | 物品大类 | 已核对，未确认缺陷 |
| BEQR-100 | BEQR:162 | 库存流水类型 | 已核对，未确认缺陷 |
| BEQR-101 | BEQR:164 | 高价值判定 | 已核对，未确认缺陷 |
| BEQR-102 | BEQR:166 | 跨主体借调 | 已核对，未确认缺陷 |
| BEQR-103 | BEQR:168 | 智能采购建议 | 已核对，未确认缺陷 |
| BEQR-104 | BEQR:169 | 门店要货履约状态 | 已核对，未确认缺陷 |
| BEQR-105 | BEQR:170 | 经销商状态 | 已核对，未确认缺陷 |
| BEQR-106 | BEQR:171 | 经销询价状态 | 已核对，未确认缺陷 |
| BEQR-107 | BEQR:172 | 经销价格目录状态 | 已核对，未确认缺陷 |
| BEQR-108 | BEQR:173 | 经销订单状态 | 已核对，未确认缺陷 |
| BEQR-109 | BEQR:174 | 经销退货单状态 | 已核对，未确认缺陷 |
| BEQR-110 | BEQR:175 | 经销库存外显 | 已核对，未确认缺陷 |
| BEQR-111 | BEQR:176 | 经销支付方式 | 已核对，未确认缺陷 |
| BEQR-112 | BEQR:177 | 经销库存预留 | 已核对，未确认缺陷 |
| BEQR-113 | BEQR:178 | 经销职责分离 | 已核对，未确认缺陷 |
| BEQR-114 | BEQR:179 | 经销取消、退货与开票 | 已核对，未确认缺陷 |
| BEQR-115 | BEQR:180 | 经销价格与税务快照 | 已核对，未确认缺陷 |
| BEQR-116 | BEQR:181 | 经销商停用撤权 | 已核对，未确认缺陷 |
| BEQR-117 | BEQR:182 | 经销资金归属 | 已核对，未确认缺陷 |
| BEQR-118 | BEQR:187 | 运营营业日 | 已核对，未确认缺陷 |
| BEQR-119 | BEQR:188 | 对账口径 | 已核对，未确认缺陷 |
| BEQR-120 | BEQR:189 | 对账差异四类 | 已核对，未确认缺陷 |
| BEQR-121 | BEQR:190 | 日结状态 | 已核对，未确认缺陷 |
| BEQR-122 | BEQR:191 | 差异处理状态 | 已核对，未确认缺陷 |
| BEQR-123 | BEQR:192 | 结算状态 | 已核对，未确认缺陷 |
| BEQR-124 | BEQR:193 | 综合通票定价 | 已核对，未确认缺陷 |
| BEQR-125 | BEQR:195 | 商户业态 | 已核对，未确认缺陷 |
| BEQR-126 | BEQR:196 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-127 | BEQR:197 | 退款资金流向 | 已核对，未确认缺陷 |
| BEQR-128 | BEQR:198 | 物业费 | 已核对，未确认缺陷 |
| BEQR-129 | BEQR:199 | 能源费与损耗费 | 已核对，未确认缺陷 |
| BEQR-130 | BEQR:201 | 税费配置 | 已核对，未确认缺陷 |
| BEQR-131 | BEQR:202 | 三方对账框架 | 已核对，未确认缺陷 |
| BEQR-132 | BEQR:203 | 开票状态 | 已核对，未确认缺陷 |
| BEQR-133 | BEQR:208 | 接入的外部系统 | 已核对，未确认缺陷 |
| BEQR-134 | BEQR:209 | 客户标签 | 已核对，未确认缺陷 |
| BEQR-135 | BEQR:210 | 统一原则 | 已核对，未确认缺陷 |
| BEQR-136 | BEQR:211 | 接入状态 | 已核对，未确认缺陷 |
| BEQR-137 | BEQR:212 | 回流结果状态 | 已核对，未确认缺陷 |
| BEQR-138 | BEQR:217 | 风险四级 | 已核对，未确认缺陷 |
| BEQR-139 | BEQR:218 | 事件状态 | 已核对，未确认缺陷 |
| BEQR-140 | BEQR:219 | 主管复核强制项 | 已核对，未确认缺陷 |
| BEQR-141 | BEQR:220 | 经营异常处置状态 | 已核对，未确认缺陷 |
| BEQR-142 | BEQR:221 | 异常状态的统一出口 | 已核对，未确认缺陷 |
| BEQR-143 | BEQR:226 | 四类主体 | 已核对，未确认缺陷 |
| BEQR-144 | BEQR:227 | 经营主体关系 | 已核对，未确认缺陷 |
| BEQR-145 | BEQR:228 | 城市项目公司 | 已核对，未确认缺陷 |
| BEQR-146 | BEQR:229 | 运营公司职责 | 已核对，未确认缺陷 |
| BEQR-147 | BEQR:230 | 主要收入归属 | 已核对，未确认缺陷 |
| BEQR-148 | BEQR:231 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-149 | BEQR:232 | 职级 | 已核对，未确认缺陷 |
| BEQR-150 | BEQR:233 | 终端十一端 | 已核对，未确认缺陷 |
| BEQR-151 | BEQR:234 | 最终权限计算 | 已核对，未确认缺陷 |
| BEQR-152 | BEQR:235 | 项目与敏感数据权限 | 已核对，未确认缺陷 |
| BEQR-153 | BEQR:236 | 终端开关优先级 | 已核对，未确认缺陷 |
| BEQR-154 | BEQR:237 | 综合体项目状态 | 已核对，未确认缺陷 |
| BEQR-155 | BEQR:238 | 商户/运营主体状态 | 已核对，未确认缺陷 |
| BEQR-156 | BEQR:239 | 合同状态 | 已核对，未确认缺陷 |
| BEQR-157 | BEQR:240 | 员工状态 | 已核对，未确认缺陷 |
| BEQR-158 | BEQR:241 | 设备状态 | 已核对，未确认缺陷 |
| BEQR-159 | BEQR:242 | 权限状态 | 已核对，未确认缺陷 |
| BEQR-160 | BEQR:248 | 招商线索状态 | 已核对，未确认缺陷 |
| BEQR-161 | BEQR:249 | 招商意向阶段 | 已核对，未确认缺陷 |
| BEQR-162 | BEQR:250 | 品牌/客户库类型 | 已核对，未确认缺陷 |
| BEQR-163 | BEQR:251 | 合同变更类型 | 已核对，未确认缺陷 |
| BEQR-164 | BEQR:252 | 退租结算状态 | 已核对，未确认缺陷 |
| BEQR-165 | BEQR:253 | 内部应付执行状态 | 已核对，未确认缺陷 |
| BEQR-166 | BEQR:255 | 合同审批链 | 已核对，未确认缺陷 |
| BEQR-167 | BEQR:256 | 租金/账单类型 | 已核对，未确认缺陷 |
| BEQR-168 | BEQR:257 | 营业额抽成模式 | 已核对，未确认缺陷 |
| BEQR-169 | BEQR:258 | 公区能源费 | 已核对，未确认缺陷 |
| BEQR-170 | BEQR:259 | 铺位租赁状态 | 已核对，未确认缺陷 |
| BEQR-171 | BEQR:260 | 物业工单类型 | 已核对，未确认缺陷 |
| BEQR-172 | BEQR:262 | 物业工单状态 | 已核对，未确认缺陷 |
| BEQR-173 | BEQR:264 | 商户服务工单状态 | 已核对，未确认缺陷 |
| BEQR-174 | BEQR:265 | 工单时效 | 已核对，未确认缺陷 |
| BEQR-175 | BEQR:266 | 楼宇机电设备状态 | 已核对，未确认缺陷 |
| BEQR-176 | BEQR:267 | 设备维保类型 | 已核对，未确认缺陷 |
| BEQR-177 | BEQR:268 | 能耗表计类型 | 已核对，未确认缺陷 |
| BEQR-178 | BEQR:269 | 抄表状态 | 已核对，未确认缺陷 |
| BEQR-179 | BEQR:270 | 能耗归集对象 | 已核对，未确认缺陷 |
| BEQR-180 | BEQR:271 | 楼宇巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-181 | BEQR:273 | 楼宇巡检类型 | 已核对，未确认缺陷 |
| BEQR-182 | BEQR:279 | 考勤日结果 | 已核对，未确认缺陷 |
| BEQR-183 | BEQR:280 | 打卡方式 | 已核对，未确认缺陷 |
| BEQR-184 | BEQR:281 | 请假类型 | 已核对，未确认缺陷 |
| BEQR-185 | BEQR:282 | 请假单状态 | 已核对，未确认缺陷 |
| BEQR-186 | BEQR:283 | 薪资单构成 | 已核对，未确认缺陷 |
| BEQR-187 | BEQR:285 | 员工提成发放 | 已核对，未确认缺陷 |
| BEQR-188 | BEQR:290 | 服务订单状态 | 已核对，未确认缺陷 |
| BEQR-189 | BEQR:291 | 管理费方式 | 已核对，未确认缺陷 |
| BEQR-190 | BEQR:296 | 分红基数 | 已核对，未确认缺陷 |
| BEQR-191 | BEQR:297 | 分红比例 | 已核对，未确认缺陷 |
| BEQR-192 | BEQR:298 | 分红税务 | 已核对，未确认缺陷 |
| BEQR-193 | BEQR:299 | 分红周期 | 已核对，未确认缺陷 |
| BEQR-194 | BEQR:300 | 分红单状态 | 已核对，未确认缺陷 |
| BEQR-195 | BEQR:301 | 可选扣除项 | 已核对，未确认缺陷 |
| BEQR-196 | BEQR:302 | 发放保护线留存 | 已核对，未确认缺陷 |
| BEQR-197 | BEQR:303 | 期间关账 | 已核对，未确认缺陷 |
| BSM-001 | BSM:505 | 整体说明：这套系统到底管什么 | 已核对；B/C业务含义经操作员确认，不冲突 |
| BSM-002 | BSM:526 | 一笔业务怎么走 | 已核对，未确认缺陷 |
| BSM-003 | BSM:563 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 已核对，未确认缺陷 |
| BSM-004 | BSM:566 | 配置数据可以存档、也可以删除（删除要审批） | 已核对，未确认缺陷 |
| BSM-005 | BSM:581 | 后台的"新建/录入"改成分步引导 | 已核对，未确认缺陷 |
| BSM-006 | BSM:584 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 已核对，未确认缺陷 |
| BSM-007 | BSM:587 | 平台管理员拥有全部日常功能权限 | 已核对，未确认缺陷 |
| BSM-008 | BSM:592 | 子系统入口 | 已核对，未确认缺陷 |
| BSM-009 | BSM:596 | 基础管理 | 已核对，未确认缺陷 |
| BSM-010 | BSM:601 | 票务权益 | 已核对，未确认缺陷 |
| BSM-011 | BSM:606 | 客人端 | 已核对，未确认缺陷 |
| BSM-012 | BSM:611 | 酒吧平板/工作手机 | 已核对，未确认缺陷 |
| BSM-013 | BSM:616 | 触摸屏一体机 | 已核对，未确认缺陷 |
| BSM-014 | BSM:621 | 酒吧管理 | 已核对，未确认缺陷 |
| BSM-015 | BSM:626 | 供应链库存 | 已核对，未确认缺陷 |
| BSM-016 | BSM:631 | 游乐管理 | 已核对，未确认缺陷 |
| BSM-017 | BSM:637 | 商户管理 | 已核对，未确认缺陷 |
| BSM-018 | BSM:643 | 招商租赁 | 已核对，未确认缺陷 |
| BSM-019 | BSM:650 | 物业服务 | 已核对，未确认缺陷 |
| BSM-020 | BSM:656 | 楼宇运营 | 已核对，未确认缺陷 |
| BSM-021 | BSM:661 | 第三方接入 | 已核对，未确认缺陷 |
| BSM-022 | BSM:666 | 留痕与异常 | 已核对，未确认缺陷 |
| BSM-023 | BSM:671 | 财务对账 | 已核对，未确认缺陷 |
| BSM-024 | BSM:676 | 持股分红 | 已核对，未确认缺陷 |
| BSM-025 | BSM:681 | 经营看板 | 已核对，未确认缺陷 |
| BSM-026 | BSM:686 | 各类入口 | 已核对，未确认缺陷 |
| BSM-027 | BSM:694 | 基础管理：先把综合体的盘子管清楚 | 已核对，未确认缺陷 |
| BSM-028 | BSM:698 | 主要功能 | 已核对，未确认缺陷 |
| BSM-029 | BSM:714 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-030 | BSM:727 | 票务权益：管卖什么票、给什么权益、怎么核销 | 已核对，未确认缺陷 |
| BSM-031 | BSM:731 | 主要功能 | 已核对，未确认缺陷 |
| BSM-032 | BSM:760 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-033 | BSM:772 | 票种、通票与权益类型（系统支持哪些种类） | 已核对，未确认缺陷 |
| BSM-034 | BSM:796 | 核销、退款与有效期规则 | 已核对，未确认缺陷 |
| BSM-035 | BSM:813 | 会员与积分规则 | 已核对，未确认缺陷 |
| BSM-036 | BSM:840 | 人脸验龄数据使用与删除 | 已核对，未确认缺陷 |
| BSM-037 | BSM:850 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 已核对，未确认缺陷 |
| BSM-038 | BSM:854 | 主要功能 | 已核对，未确认缺陷 |
| BSM-039 | BSM:873 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-040 | BSM:883 | 登录与关键规则 | 已核对，未确认缺陷 |
| BSM-041 | BSM:898 | 会员注册、认证与收费边界 | 已核对，未确认缺陷 |
| BSM-042 | BSM:907 | 会员积分活动与发券 | 已核对，未确认缺陷 |
| BSM-043 | BSM:916 | 会员营销明确边界 | 已核对，未确认缺陷 |
| BSM-044 | BSM:929 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 已核对，未确认缺陷 |
| BSM-045 | BSM:933 | 主要功能 | 已核对，未确认缺陷 |
| BSM-046 | BSM:953 | 平板图形点单（POS） | 已核对，未确认缺陷 |
| BSM-047 | BSM:963 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-048 | BSM:978 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 已核对，未确认缺陷 |
| BSM-049 | BSM:982 | 主要功能 | 已核对，未确认缺陷 |
| BSM-050 | BSM:996 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-051 | BSM:1010 | 酒吧管理：从入场、开台到结账关台 | 已核对，未确认缺陷 |
| BSM-052 | BSM:1014 | 主要功能 | 已核对；来源分类按项目可配为操作员原意 |
| BSM-053 | BSM:1041 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-054 | BSM:1053 | 桌台/账单状态与经营规则 | 已核对；打折原因分类按项目可配为操作员原意 |
| BSM-055 | BSM:1088 | 桌台、菜单与优惠券 | 已核对，未确认缺陷 |
| BSM-056 | BSM:1101 | 正式未付款订单挂起 | 已核对，未确认缺陷 |
| BSM-057 | BSM:1120 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 已核对，未确认缺陷 |
| BSM-058 | BSM:1124 | 怎么点、怎么收钱 | 已核对，未确认缺陷 |
| BSM-059 | BSM:1135 | 钱怎么分、怎么退 | 已核对，未确认缺陷 |
| BSM-060 | BSM:1151 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 已核对，未确认缺陷 |
| BSM-061 | BSM:1155 | 主要功能 | 已核对，未确认缺陷 |
| BSM-062 | BSM:1174 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-063 | BSM:1186 | 库存类型与规则 | 已核对，未确认缺陷 |
| BSM-064 | BSM:1208 | 采购建议与门店要货 | 已核对，未确认缺陷 |
| BSM-065 | BSM:1216 | 外部经销商准入、价格与库存 | 已核对，未确认缺陷 |
| BSM-066 | BSM:1226 | 经销订单、付款与履约 | 已核对，未确认缺陷 |
| BSM-067 | BSM:1238 | 经销退货、退款、红冲与资金归属 | 已核对，未确认缺陷 |
| BSM-068 | BSM:1253 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 已核对，未确认缺陷 |
| BSM-069 | BSM:1257 | 主要功能 | 已核对，未确认缺陷 |
| BSM-070 | BSM:1276 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-071 | BSM:1287 | 游乐类型与规则 | 已核对，未确认缺陷 |
| BSM-072 | BSM:1308 | 商户管理：商户在授权范围内完整使用自己的系统 | 已核对，未确认缺陷 |
| BSM-073 | BSM:1313 | 主要功能 | 已核对，未确认缺陷 |
| BSM-074 | BSM:1326 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-075 | BSM:1339 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 已核对，未确认缺陷 |
| BSM-076 | BSM:1343 | 主要功能 | 已核对，未确认缺陷 |
| BSM-077 | BSM:1357 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-078 | BSM:1370 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 已核对，未确认缺陷 |
| BSM-079 | BSM:1374 | 主要功能 | 已核对：A概括句已按批准与原分支对齐 |
| BSM-080 | BSM:1388 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-081 | BSM:1401 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已核对，未确认缺陷 |
| BSM-082 | BSM:1405 | 主要功能 | 已核对，未确认缺陷 |
| BSM-083 | BSM:1414 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-084 | BSM:1427 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 已核对，未确认缺陷 |
| BSM-085 | BSM:1431 | 日常工作 | 已核对，未确认缺陷 |
| BSM-086 | BSM:1441 | 排班、考勤与请假 | 已核对，未确认缺陷 |
| BSM-087 | BSM:1455 | 我的薪资 | 已核对，未确认缺陷 |
| BSM-088 | BSM:1467 | 员工提成、活动执行与人员服务边界 | 已核对，未确认缺陷 |
| BSM-089 | BSM:1479 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 已核对，未确认缺陷 |
| BSM-090 | BSM:1545 | 机器人配送与扫码交接 | 已核对，未确认缺陷 |
| BSM-091 | BSM:1554 | 视频监控（留点位/时间） | 已核对，未确认缺陷 |
| BSM-092 | BSM:1563 | 重要事项短信通知（2026-06-29 新增） | 已核对，未确认缺陷 |
| BSM-093 | BSM:1574 | 留痕与异常：重要动作留得住，出了问题追得回 | 已核对，未确认缺陷 |
| BSM-094 | BSM:1578 | 必须关注的动作 | 已核对，未确认缺陷 |
| BSM-095 | BSM:1596 | 怎样减少人工填写 | 已核对，未确认缺陷 |
| BSM-096 | BSM:1607 | 处理结果 | 已核对，未确认缺陷 |
| BSM-097 | BSM:1619 | 风险等级与风控规则 | 已核对，未确认缺陷 |
| BSM-098 | BSM:1636 | 财务对账：把经营事实整理成财务能确认的口径 | 已核对，未确认缺陷 |
| BSM-099 | BSM:1639 | 内部结算价目录（按商品规格逐项建目录） | 已核对，未确认缺陷 |
| BSM-100 | BSM:1641 | 结算配置中心（后台总控页） | 已核对，未确认缺陷 |
| BSM-101 | BSM:1643 | 项目零售参考价 | 已核对，未确认缺陷 |
| BSM-102 | BSM:1648 | 主要功能 | 已核对，未确认缺陷 |
| BSM-103 | BSM:1666 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-104 | BSM:1678 | 收入归属：哪类收入归哪个业态主体 | 已核对，未确认缺陷 |
| BSM-105 | BSM:1697 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 已核对，未确认缺陷 |
| BSM-106 | BSM:1709 | 对账与结算类型规则 | 已核对，未确认缺陷 |
| BSM-107 | BSM:1737 | 持股分红：按持股把经营成果定期分给主理人 | 已核对，未确认缺陷 |
| BSM-108 | BSM:1741 | 主要功能 | 已核对，未确认缺陷 |
| BSM-109 | BSM:1760 | 谁会用 | 已核对，未确认缺陷 |
| BSM-110 | BSM:1768 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-111 | BSM:1781 | 经营看板：让管理层和运营看得懂、能追问 | 已核对，未确认缺陷 |
| BSM-112 | BSM:1785 | 主要指标 | 已核对，未确认缺陷 |
| BSM-113 | BSM:1813 | 谁会看 | 已核对，未确认缺陷 |
| BSM-114 | BSM:1827 | 各类入口：不同人只看自己该看的、只做自己该做的 | 已核对，未确认缺陷 |
| BSM-115 | BSM:1897 | 相互关系：为什么这些子系统必须连在一起 | 已核对，未确认缺陷 |
| BSM-116 | BSM:1928 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 已核对，未确认缺陷 |
| BSM-117 | BSM:1932 | 正式上线时的安全加固 | 已核对，未确认缺陷 |
| BSM-118 | BSM:1940 | 能自动识别环境的安装包 + 内附教程 | 已核对，未确认缺陷 |
| BSM-119 | BSM:1947 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-120 | BSM:1957 | 已确认规则和实施前输入项 | 已核对，未确认缺陷 |


## 对抗式线：正常路径反例与排除

| 对照组 | 反例/边界 | 本席结论与依据 |
|---|---|---|
| 全局分类与酒吧局部配置 | 全局不做后台可配 vs 客人来源/打折原因枚举按项目可配 | 操作员最新明确B/C原文不冲突、就是本意；按已确认业务含义判为成立。不改原文，不新增可配范围或例外。BSM:521/1020/1068及最新答复，见CX-03/04。 |
| 票务/年龄 | 设备不可用、误拒、多人、多次、场次终结、已用票再赠、补偿延长 | 年龄设备人工抽查/一次凭据是明定例外，非免检票；补偿是场次终结例外；普通退款与安全例外分开。BEQR:33/47/73、BSM:746/800/803/808/1074。 |
| 退款单位 | 人份退款与“同订单不存在部分核销并部分退款” | 人次处理规定核算单位，不单独授予退款资格；两份均明写普通退款限制，不能靠人份定义推导出例外。未确认矛盾，不发明放宽。BEQR:47/73、BSM:746/803。 |
| 分配/尾差 | 1—10 家比例、并列第4/5、仅第一段、第二段全未核销、缺家数行 | 十行均100%，并列17.5%；未核销减除/未分配池/尾差三栏独立；缺行补配对暂挂票是明确特例。命令实算在下。 |
| 零元/定价/费用 | 免费权益出资、内部价时点、销售通路二层、单票/通票服务费上限 | 逐项锁出资与内部价的特殊规则不能被一般“核销时固化”覆盖；各费只扣一次，单票/通票上限分开。BEQR票务财务锚点、BSM:748—755/1037—1042/1640。 |
| 积分/会员/储值 | 付款与核销不同日、未用票、退款积分过期、注销一年、券重回可用 | 积分/成长值双账，票务/商户消费两种计算；商户取消退券与退积分继承原到期日明确；注销事件保留是窄例外。BEQR会员段，BSM:815—849/864。 |
| 台位/酒吧 | A+B→T→并C→拆、撤销时原台另占、低消已达后优惠、赠送/提成 | 原台历史不进入新T运算；任一后续变化阻断旧快照撤销；低消折前与提成折后为不同基数；服务费应发部分与扣留部分分开。BSM:1063—1081。 |
| 终端/员工 | 私人手机概述含考勤与工作机领机打卡、设备故障延误、跨月班次 | 概述不替代领机三事件的具体入口；设备故障延误为严格迟到规则的明定例外；班次结束归属为营业日规则的明定例外。BSM:930/1443—1452/1458。 |
| 供应链/游乐 | 可消费借物、补录、恰等于高价值阈值、关键设备可替代、重复出场 | 可消费物品实际买断；补录与原始记录并列；高价值触发是“高于”而不是自行改成“大于等于”，未确认能以缺少重复等值句新增业务。关键安全依赖依实际流程、辅助故障需工程留痕，容量不人工改。BEQR:164，BSM:1202—1206/1253—1305。 |
| 租赁/物业 | 零欠费/仅罚款、面积变更、押金等待与三个付账日、投诉关闭 | 清退前提明确；合同变更子合同与一般不追补分别适用；退租押金不受三个付账日为明定例外；投诉类型分支不同，不套全类验收链。BSM:1340—1385。 |
| 财务/分红 | 跨月退款、在途/失败、税额、服务费扣留、借款前置 | 不改已关期间；在途不重付、失败另路跟进；税额依专业规则不套普通舍入；借款前置不属于三个可选开关。BEQR财务/分红段，BSM:1642—1790。 |
| 治理/交付 | 普通审批绕红线、密钥逐笔例外、测试弱密码、只读分析、第三方占位 | 三硬红线不可绕，其他密钥权限限逐笔；生产换强密码/安装与运行权限不同；只读分析不发控制指令；无接口不得假造数据。BSM:703/1805—1820/1930—1955。 |

上表行区间是定位辅助，精确判据原文和源行以逐 ID 表为准。各组概述与具体条目的边界按已明定限定解释；B/C按操作员最新澄清解释；不为理论组合添加状态/闸/规则。



## 本席复核登记（四项，均无工程缺陷）
### CX-01 — BEQR 静态呈现
- 文件：docs/business-enum-quick-reference-v0.1.html:1—26。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/015/017/018。
- 无业务发明理由：doctype、UTF-8、CSS、表格语义只承载同页权威文本的阅读，不创造收费、状态、角色或审批。结构解析与源码全文读取均完成。
- 处置：沿用已B2双PASS的M001-004，只登记，不自行删除正本任何部分。

### CX-02 — BSM 静态呈现与目录导航
- 文件：docs/business-system-map-for-operations-v0.1.html:1—520、2008—2032。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/012/015/017/018。
- 无业务发明理由：布局与 setActive 只高亮当前目录，不修改业务数据；真实27个href均对应源ID，原脚本执行首屏/每目标通过。
- 处置：沿用已B2双PASS的M001-004，只登记。



### CX-03 — B：客人来源分类按项目可配，经操作员确认不冲突
- 文件：docs/business-system-map-for-operations-v0.1.html:521、1020。
- 当前分类：细化；工程缺陷：否；建议修复队列：否。依据类型1+4，另有操作员本次明确业务解释；冻结判据BSM-001、BSM-052、GEN-016、GEN-018。
- L521原文：“业务分类定义也不在此列：红线有哪几类、退款有哪几种情形、会员有哪几个等级、终端有哪几类、合作模式有哪几种等「有哪几种」的清单，属于业务口径本身，不做后台可配；增加或删减一类必须先经批准写入三份 HTML，不得在后台自行增删。”
- L1020原文：“开台时系统自动判定并记录客人来源：经第三方团购券核销或第三方预订到店的，按回流渠道自动记（美团点评、抖音等）；经平台自有渠道订台或购票到店的，自动记平台自有渠道；由有销售归属的订台开台的，自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客——此时现场可人工补选营销带客并选定销售人员，不补选即维持自来客。来源枚举项为业务参数，按项目可配。”
- 完整段还把服务员/销售人员分开、提成引用来源、事后改派由店长结清前复核；这些未改，不构成另项finding。
- 核对：上述两处原文及冻结BSM001/052摘录均保持。操作员已明确B原文不冲突、就是本意；此前将具体来源规则误判成冲突的结论撤销。未以数值上下限缺失作判断，也未跑代码或推定后台应支持哪些增删动作。
- 操作员原话：“b c两项原文不冲突。就是我的原意。”据此作为符合预期的细化登记；无需修复，原文保留。旧疑点保留审计历史，不继续标为当前冲突，不改写为已修，不开添加项questions。

### CX-04 — C：打折原因分类按项目可配，经操作员确认不冲突
- 文件：docs/business-system-map-for-operations-v0.1.html:521、1068。
- 当前分类：细化；工程缺陷：否；建议修复队列：否。依据类型1+4，另有操作员本次明确业务解释；冻结判据BSM-001、BSM-054、GEN-016、GEN-018。
- 全局原文同CX03。L1068连续原文：“最高折扣额度可按岗位分档配置（业务参数，按项目可配，出厂默认各授权岗位一致为八折，须同时配置上下限）。现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）；月度管理报告按原因分类归集。”
- 数值折扣额度可配和“原因有哪几种”是不同对象。本席只核原因清单与L521全局原则；不将数值额度算作冲突、不为枚举发明数值上下限。全文中整单打折、不得手工改价、可与券/赠送合并、承担方及报告归集均按原文明定范围审读，不据偏好加业务。
- 核对：当前行与BSM054摘录保留五类原因及“枚举项业务参数，按项目可配”，BSM001原文亦保持。操作员已明确C原文不冲突、就是本意；当前按此确认规则成立，不额外限制、扩张配置边界。
- 操作员最新原话同CX03：当前作为符合预期的细化登记，无需修复。撤销本席先前的冲突判断，原文保留；依据是新的明确业务澄清，而非仅凭先前拒绝修复。

## A/D当前核对（不是本次finding）
L1380现为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”；L1384及BEQR262保留保洁完成即结、两类投诉和复杂报修的已确认分支。概括与分支对齐，不再要求全类统一验收。L2现V0.13/2026-09-08，版本仅进一次，页脚和目录保持。BSM079新锁摘录与此句逐字一致；A/D本批2行未出现回归。旧005详细投诉分支保持。B4与LOCK-AMEND双PASS只证明各自阶段，不替代本次整模块判定。

## 技能、可复现检查及实际输出
沿用本会话已实际读取的using-superpowers、grilling/grill-me、codebase-design、ponytail-review、receiving-code-review、verification-before-completion、systematic-debugging/diagnosing-bugs，按提示词收窄；无子代理、无新增判据/门槛。HTML文档按类型1/4，不强制新增测试文件，不冒称浏览器或读屏验收。batch-grill-me/ubiquitous-language为此前实际预检缺口，不冒充调用。

### 全量摘录、结构和原导航脚本
~~~bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'((?:BEQR|BSM)-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 piece=raw[1].splitlines();start=int(src[2])-1
 assert source.splitlines()[start:start+len(piece)]==piece,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==317,len(coverage)
class AuditHTML(HTMLParser):
 def __init__(self):
  super().__init__();self.ids=[];self.links=[];self.navlinks=[];self.stack=[];self.nav=False;self.errors=[];self.tags=Counter();self.lang=None;self.charset=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs);self.tags[tag]+=1
  if 'id' in a:self.ids.append(a['id'])
  if tag=='html':self.lang=a.get('lang')
  if tag=='meta' and 'charset' in a:self.charset=a['charset']
  if tag=='nav' or 'nav' in a.get('class','').split():self.nav=True
  if tag=='a' and 'href' in a:
   self.links.append(a['href'])
   if self.nav:self.navlinks.append(a['href'])
  if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag=='nav':self.nav=False
  if self.stack and self.stack[-1]==tag:self.stack.pop()
  else:self.errors.append([self.getpos()[0],tag,self.stack[-3:]])
checks=[]
for path in paths:
 b=(root/path).read_bytes();s=b.decode('utf-8',errors='strict');p=AuditHTML();p.feed(s)
 assert not b.startswith(b'\xef\xbb\xbf') and '\ufffd' not in s,path
 duplicate=[x for x,n in Counter(p.ids).items() if n>1]
 broken=[x for x in p.links if x.startswith('#') and x[1:] not in p.ids]
 missing=[x for x in p.links if x and not x.startswith(('#','http:','https:','mailto:','javascript:')) and not (root/path).parent.joinpath(x.split('#')[0]).exists()]
 assert not (duplicate or broken or missing or p.errors or p.stack),(path,duplicate,broken,missing,p.errors,p.stack)
 assert p.lang and p.charset.lower()=='utf-8'
 checks.append({'path':path,'sha256':hashlib.sha256(b).hexdigest(),'wc_l':b.count(b'\n'),'actual_lines':len(s.splitlines()),'ids':len(p.ids),'navlinks':len(p.navlinks),'broken_links':len(broken)+len(missing),'stack_errors':len(p.errors),'utf8':'PASS'})
 if 'business-system-map' in path:
  script=re.search(r'<script>([\s\S]*?)</script>',s)[1]
  refs=[x for x in p.navlinks if x.startswith('#')]
  js="const vm=require('node:vm'), assert=require('node:assert/strict'); const input=JSON.parse(process.argv[1]); let top=[],active=[]; const nodes=input.refs.map((id,i)=>({id:id.slice(1),getBoundingClientRect:()=>({top:top[i]})})); const links=input.refs.map((id,i)=>({getAttribute:()=>id,classList:{toggle:(_,v)=>active[i]=v}})); const context={document:{querySelectorAll:()=>links,querySelector:id=>nodes.find(n=>'#'+n.id===id)},window:{addEventListener:(event,fn,opts)=>{assert.equal(event,'scroll');assert.equal(opts.passive,true)}}};top=nodes.map((_,i)=>1000+i*100);vm.createContext(context);vm.runInContext(input.script,context);assert.deepEqual(active,links.map((_,i)=>i===0));for(let target=0;target<nodes.length;target++){top=nodes.map((_,i)=>(i-target)*200+120);vm.runInContext('setActive()',context);assert.deepEqual(active,links.map((_,i)=>i===target));}console.log(JSON.stringify({nav_targets:nodes.length,initial_and_every_section:'PASS',actual_script:true}));"
  run=subprocess.run(['node','-e',js,json.dumps({'script':script,'refs':refs})],capture_output=True,text=True)
  assert run.returncode==0,run.stderr
  checks.append({'navigation_execution':json.loads(run.stdout)})
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
mapping=next(x for x in lock.splitlines() if x.startswith('M001 → ['))
ids=[x.strip() for x in mapping.split('[',1)[1].rsplit(']',1)[0].split(',')]
assert set(ids)=={f'BEQR-{i:03d}' for i in range(1,198)}|{f'BSM-{i:03d}' for i in range(1,121)}|{f'GEN-{i:03d}' for i in range(1,19)} and len(ids)==335
print('M001_MAPPING_EXACT=335; SOURCE_EXCERPTS_AT_DECLARED_LINES=317')
PY
~~~
实际输出：
~~~text
{"lock_sha256": "b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
COVERAGE_COUNT=317
M001_MAPPING_EXACT=335; SOURCE_EXCERPTS_AT_DECLARED_LINES=317
~~~

### 金额与反例
~~~bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
import re,json,hashlib
root=Path('/home/james/complex-management')
s=(root/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
row=next(x for x in s.splitlines() if '<td class="k">酒吧通票可分配池</td>' in x)
table=row.split('参与家数档位表（出厂默认')[1].split('参与家数档位表不设家数上限')[0]
def parse_table(t):
 rows={}
 for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?:；|。)',t):
  slots={}
  for start,end,value in re.findall(r'第 (\d+)(?:[–-](\d+))? 名\s*(?:各\s*)?(\d+(?:\.\d+)?)%',body):
   for rank in range(int(start),int(end or start)+1):
    assert rank not in slots
    slots[rank]=Decimal(value)
  assert set(slots)==set(range(1,int(n)+1)),(n,slots)
  assert sum(slots.values())==100,(n,slots)
  rows[int(n)]=slots
 assert len(rows)==10
 return rows
rows=parse_table(table)
assert (rows[5][4]+rows[5][5])/2==Decimal('17.5')
rejected=False
try:parse_table(table.replace('100%','99%',1))
except AssertionError:rejected=True
assert rejected
# 只证明 HTML 明文的金额/场次算例可同时成立，不代表执行了业务实现。
round_cent=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
tie=[round_cent(Decimal('.10')*Decimal(1)/3)]*3
tail=Decimal('.10')-sum(tie)
assert sum(tie)+tail==Decimal('.10') and tail==Decimal('.01')
assert 8+5==13 # A4+B6 转 T8 后再并 C5
budget_total,available,reserved,issued,reclaimed=100,60,20,30,10
assert budget_total==available+reserved+issued-reclaimed
assert round_cent(Decimal(5000+1000+1000+1000)*Decimal('.2'))==Decimal('1600.00')
print(json.dumps({'rank_rows':{n:list(map(str,v.values())) for n,v in rows.items()},'rank_sums':'all 100','tie_4_5':'17.5','rounding_three_shares':list(map(str,tie)),'separate_tail':str(tail),'mutated_99_percent_rejected':rejected,'merge_example':'T8+C5=13','activity_budget':'100=60+20+30-10','scope':'HTML arithmetic only, not business implementation'},ensure_ascii=False))
raw=root/'docs/audit-2026-09/modules/M001-b1-self-raw.md'
if raw.exists():
 text=raw.read_text(encoding='utf-8');print('SELF_RAW_METADATA='+json.dumps({'size':raw.stat().st_size,'report_versions':re.findall(r'(?m)^.*版本[：:]\s*(V\d+\.\d+).*$',text)[-5:],'complete_report_blocks':len(re.findall(r'(?m)^CRITERIA-LOCK-SHA256:',text))},ensure_ascii=False))
print('HEAD='+__import__('subprocess').check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip())
PY
~~~
实际输出（SELF_RAW_METADATA为当时追加前元数据，不作语义判据）：
~~~text
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 459038, "report_versions": ["V0.13", "V0.35", "V0.36", "V0.37", "V0.38"], "complete_report_blocks": 11}
HEAD=242f487233d028173cb9198eecd079f5d877405c
~~~

### 全335映射与原完整阅读字节锚定
~~~json
{"mapping_ids": 335, "all_335_exact": true, "source_byte_recheck": [{"file": "docs/business-enum-quick-reference-v0.1.html", "changed_since_previous_complete_read": [], "all_other_lines_byte_identical": true, "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66"}, {"file": "docs/business-system-map-for-operations-v0.1.html", "changed_since_previous_complete_read": [2, 1380], "all_other_lines_byte_identical": true, "sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa"}]}
~~~
还已实跑按行取出L521/L1020/L1068并与对应BSM001/052/054原文相匹配；A新句与1384分支共存，实际确认无源字漂移。CX03/04另依据操作员最新业务澄清确认，不用机械检查代替语义判断。没有运行默认只看diff的脚本冒充全文审计，没有Java/SQL/产品业务测试声明。


### B5当前原文字节与收口前提实核

~~~json
{"all_three_expected_hashes_match": true, "current_semantic_source_anchors": {"A": true, "B": true, "C": true, "redline": true, "manual_record_review": true}, "B2_final_current_rows": 16, "queue": [], "M001_questions": [], "HEAD_origin": ["242f487233d028173cb9198eecd079f5d877405c", "242f487233d028173cb9198eecd079f5d877405c"], "no_fifth_B3": true}
~~~
本次实读锁及HTML SHA与B1完整阅读的字节一致；A/B/C和红线、补录原句均保持；B2已定稿16行队列空，当前无M001 questions。
本席检查命令更正记录：首个机械检查命令的JS替换字符串把Python正则末尾$与单引号解释成替换后缀，导致解析失败、未执行。改用回调替换与split解析后只重跑该检查；其余两个已完成检查保留。不是HTML修改、不计B3或Grok失败。

## 本席B5结论

PASS。两份HTML与全部335映射已全模块三线复核；结构、317源摘录、45内部链接/27导航原脚本、金额及反例检查有实际证据，无新业务冲突/工程缺陷/添加项。CX01/02属已有004技术性无缺陷；CX03/04属已有014/015已明确业务原意、细化无缺陷。A/D及既有详细分支保持，B/C原文保持。
这是本席B5票，不先写模块PASS。必须等本次Grok完整B5落盘，根报告完整保存后再读，双PASS且B5后实算/sidecar/ledger/Grok四值全等才允许按提示词先更新ledger、再写模块收口文件。本次Grok报告在本报告落盘前未读；不存在根票推翻外审FAIL。
无第五次B3，无新HTML/判据修改、无提交推送部署；M002及其余模块尚未在此阶段启动。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M001 B5-r4 整模块最终复核 — Codex
版本：V0.02
范围：2文件2340行；317 HTML判据+18 GEN=335。B3累计4，状态IN_PROGRESS。先启动Grok，本席封存前未读取其B5-r4 V0.02 raw/JSON。本轮B5自算两文件SHA仍与V0.40逐段完整阅读及V0.42复核字节相同，复用相同原文取证；在B2 V0.43/V0.22双PASS、findings V3.32空队列后独立重新评估全335判据与三线，不将B1/B2或旧B5 PASS当成本次结论。技能沿用已实际读取的grilling追问结构、verification-before-completion和receiving-code-review；不调用第三席。文档模块不声称已测试业务后端。

## 文件覆盖与第一性原理
- docs/business-enum-quick-reference-v0.1.html：1—308全读；SHA256 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66。
- docs/business-system-map-for-operations-v0.1.html：1—2032全读；SHA256 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66。
枚举是197个明确问题答案，地图提供流程、责任、入口和跨域关系；删除任何一份会丢失其专有说明。静态CSS和导航只负责可读性，无业务发明，沿用M001-004技术性登记。页面不是产品实现完成声明；不能因为要求中有功能就声称已有后台。地图1—455样式结构、456—726总览治理、727—1009票务会员员工入口、1010—1252酒吧服务供应链、1253—1478游乐商户物业员工、1479—1738第三方留痕财务、1739—2032分红看板终端关系上线已确认规则及导航脚本全部覆盖。枚举1—308全部13表含巨型通票池与账单操作原句完整阅读。

## 通用判据逐项
| ID | 实核与判断 |
|---|---|
| GEN-001 | 两文件提供业务范围与枚举互补；合作模式两处一简表一模板解释，用途不同，不以同名删除。 |
| GEN-002 | HTMLParser解析成功；27目录目标存在；本轮用node:vm执行现HTML原始脚本，初始与全部27滚动目标唯一高亮断言通过。未运行真实浏览器，不声称视觉或后台验收。 |
| GEN-003 | 现有entry-anchors测试实际1/1 PASS；文档类不强制Maven。 |
| GEN-004 | 静态HTML无用户输入执行；导航href均本地静态片段；不评未纳入业务控制器。 |
| GEN-005 | 商户本域、项目隔离、导出单独授权与PGM联系人特例对象不冲突；不因平台管理员日常概述扩最高权限。 |
| GEN-006 | 退款成功才计退额、补录复核才生效、历史不回改条款相容；未声称数据库事务已验。 |
| GEN-007 | 入场多次同场不重复扣；流水只追加冲正；静态阅读无写竞争。 |
| GEN-008 | 状态目录与流程适用对象对齐，退款部分态为订单聚合不推翻单张禁部分退款；例外与一般规则合取。 |
| GEN-009 | 对账差异、付款在途/失败、缺档位暂挂均有已定义出口，不引入新状态。 |
| GEN-010 | 受控工作设备和敏感取证范围明确；静态文件无真实客人记录/凭据；手机号新特例不扩客人员工。 |
| GEN-011 | 订单、服务归属、移交和人工补录证据明确；安检已批准设备侧例外保留。 |
| GEN-012 | 导航仅27节一次scroll扫描，无外部请求/持久化资源；合理静态规模。 |
| GEN-013 | 内联CSS/JS，静态浏览器原生API，无额外依赖；未编译非本模块。 |
| GEN-014 | 本模块无迁移或DB执行；条款不证明模式已实现。 |
| GEN-015 | 中文、UTF8、13表13thead/26th；地图13表13thead，导航链接可达，无重复id。 |
| GEN-016 | V0.07/V0.14和页脚日期真实存在；entry-anchors现跑PASS，旧红不是当前结果。 |
| GEN-017 | 按冲突→细化→技术性→添加；B/C为操作员已确认原意，不转为容忍缺陷。 |
| GEN-018 | 本票证据限实际HTML原文及本次命令；未实跑的浏览器/服务端不宣称通过。 |

## 顺从式：全317条对照
以下来源坐标来自现锁V0.14，逐条绑定本席已读的相同字节现文。本轮317/317摘录全文均包含于现HTML，BSM120日期已按获批09-10完成LOCK-AMEND双PASS及④⑤；三条考勤匹配，工作手机1445保持打卡。
| ID | 标题 | HTML证据 | 结果 |
|---|---|---|---|
| BEQR-001 | 票种 | docs/business-enum-quick-reference-v0.1.html:29 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-002 | 票务策略状态 | docs/business-enum-quick-reference-v0.1.html:30 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-003 | 定价、售票与收款主体 | docs/business-enum-quick-reference-v0.1.html:32 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-004 | 年龄限制 | docs/business-enum-quick-reference-v0.1.html:33 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-005 | 通票赠酒下限 | docs/business-enum-quick-reference-v0.1.html:34 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-006 | 通票类型 | docs/business-enum-quick-reference-v0.1.html:35 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-007 | 权益类型 | docs/business-enum-quick-reference-v0.1.html:36 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-008 | 餐饮/零售券两种用法 | docs/business-enum-quick-reference-v0.1.html:37 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-009 | 资格判定 | docs/business-enum-quick-reference-v0.1.html:39 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-010 | 订单状态 | docs/business-enum-quick-reference-v0.1.html:40 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-011 | 票/权益状态 | docs/business-enum-quick-reference-v0.1.html:41 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-012 | 核销记录状态 | docs/business-enum-quick-reference-v0.1.html:43 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-013 | 手环入场 | docs/business-enum-quick-reference-v0.1.html:44 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-014 | 退款单状态 | docs/business-enum-quick-reference-v0.1.html:45 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-015 | 有效期类型 | docs/business-enum-quick-reference-v0.1.html:46 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-016 | 售票与退款原则 | docs/business-enum-quick-reference-v0.1.html:47 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-017 | 到期未核销款 | docs/business-enum-quick-reference-v0.1.html:48 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-018 | 域归属对照 | docs/business-enum-quick-reference-v0.1.html:49 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-019 | 跨域通票分配 | docs/business-enum-quick-reference-v0.1.html:51 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-020 | 酒吧通票可分配池 | docs/business-enum-quick-reference-v0.1.html:52 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-021 | 收费与免费权益结算 | docs/business-enum-quick-reference-v0.1.html:53 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-022 | 项目内部结算价 | docs/business-enum-quick-reference-v0.1.html:54 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-023 | 核销顺序 | docs/business-enum-quick-reference-v0.1.html:55 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-024 | 游乐核销归属 | docs/business-enum-quick-reference-v0.1.html:56 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-025 | 门票综合服务费 | docs/business-enum-quick-reference-v0.1.html:57 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-026 | 动态价与会员价 | docs/business-enum-quick-reference-v0.1.html:58 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-027 | 人脸验龄数据生命周期 | docs/business-enum-quick-reference-v0.1.html:60 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-028 | 会员唯一识别 | docs/business-enum-quick-reference-v0.1.html:65 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-029 | 会员注册 | docs/business-enum-quick-reference-v0.1.html:66 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-030 | 会员身份状态 | docs/business-enum-quick-reference-v0.1.html:68 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-031 | 等级与成长值 | docs/business-enum-quick-reference-v0.1.html:70 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-032 | 等级权益对照 | docs/business-enum-quick-reference-v0.1.html:71 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-033 | 积分与成长值来源 | docs/business-enum-quick-reference-v0.1.html:72 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-034 | 票务人次计算 | docs/business-enum-quick-reference-v0.1.html:73 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-035 | 有效消费与冻结 | docs/business-enum-quick-reference-v0.1.html:75 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-036 | 积分明细类型 | docs/business-enum-quick-reference-v0.1.html:77 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-037 | 积分兑换定价 | docs/business-enum-quick-reference-v0.1.html:78 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-038 | 商品规格 | docs/business-enum-quick-reference-v0.1.html:79 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-039 | 项目零售参考价 | docs/business-enum-quick-reference-v0.1.html:80 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-040 | 发券与核销结算 | docs/business-enum-quick-reference-v0.1.html:81 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-041 | 重复兑换与转赠 | docs/business-enum-quick-reference-v0.1.html:82 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-042 | 券包状态 | docs/business-enum-quick-reference-v0.1.html:83 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-043 | 积分结算账期 | docs/business-enum-quick-reference-v0.1.html:84 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-044 | 会员系统服务费 | docs/business-enum-quick-reference-v0.1.html:85 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-045 | 储值卡类型与收款主体 | docs/business-enum-quick-reference-v0.1.html:86 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-046 | 会员忠诚度扩展 | docs/business-enum-quick-reference-v0.1.html:87 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-047 | 会员注册与开卡 | docs/business-enum-quick-reference-v0.1.html:89 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-048 | 学生认证 | docs/business-enum-quick-reference-v0.1.html:90 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-049 | 会员积分活动状态 | docs/business-enum-quick-reference-v0.1.html:91 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-050 | 积分活动预算守恒 | docs/business-enum-quick-reference-v0.1.html:92 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-051 | 充值积分 | docs/business-enum-quick-reference-v0.1.html:93 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-052 | 会员营销边界 | docs/business-enum-quick-reference-v0.1.html:94 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-053 | 发券对象 | docs/business-enum-quick-reference-v0.1.html:95 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-054 | 桌台/卡座/吧台位状态 | docs/business-enum-quick-reference-v0.1.html:100 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-055 | 账单状态 | docs/business-enum-quick-reference-v0.1.html:101 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-056 | 订台定金状态 | docs/business-enum-quick-reference-v0.1.html:103 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-057 | 订卡邀请函状态 | docs/business-enum-quick-reference-v0.1.html:105 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-058 | 点酒方式 | docs/business-enum-quick-reference-v0.1.html:106 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-059 | 支付方式 | docs/business-enum-quick-reference-v0.1.html:107 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-060 | 销售提成 | docs/business-enum-quick-reference-v0.1.html:108 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-061 | 服务员提成 | docs/business-enum-quick-reference-v0.1.html:109 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-062 | 退菜退酒 | docs/business-enum-quick-reference-v0.1.html:110 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-063 | 高危动作（需审批/双证据） | docs/business-enum-quick-reference-v0.1.html:111 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-064 | 出品票据状态 | docs/business-enum-quick-reference-v0.1.html:112 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-065 | 账单操作 | docs/business-enum-quick-reference-v0.1.html:114 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-066 | 收银交班状态 | docs/business-enum-quick-reference-v0.1.html:115 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-067 | 最低消费 | docs/business-enum-quick-reference-v0.1.html:117 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-068 | 服务费 | docs/business-enum-quick-reference-v0.1.html:118 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-069 | 小费 | docs/business-enum-quick-reference-v0.1.html:119 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-070 | 客人存酒有效期与转存 | docs/business-enum-quick-reference-v0.1.html:120 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-071 | 存酒瓶数 | docs/business-enum-quick-reference-v0.1.html:122 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-072 | 套餐赠券适用范围 | docs/business-enum-quick-reference-v0.1.html:123 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-073 | 非低消商品 | docs/business-enum-quick-reference-v0.1.html:124 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-074 | 隐私桌台权限 | docs/business-enum-quick-reference-v0.1.html:125 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-075 | 桌台会话与菜单 | docs/business-enum-quick-reference-v0.1.html:126 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-076 | 桌台计时产品 | docs/business-enum-quick-reference-v0.1.html:127 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-077 | 门店支付方式 | docs/business-enum-quick-reference-v0.1.html:128 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-078 | 扫码点单身份 | docs/business-enum-quick-reference-v0.1.html:129 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-079 | 优惠券自动选择 | docs/business-enum-quick-reference-v0.1.html:130 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-080 | 优惠券付款预留 | docs/business-enum-quick-reference-v0.1.html:131 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-081 | 正式挂起订单状态 | docs/business-enum-quick-reference-v0.1.html:132 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-082 | 正式挂起订单规则 | docs/business-enum-quick-reference-v0.1.html:133 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-083 | 挂单取消与付款互斥 | docs/business-enum-quick-reference-v0.1.html:134 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-084 | 挂单坏账损耗 | docs/business-enum-quick-reference-v0.1.html:135 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-085 | 项目开放状态 | docs/business-enum-quick-reference-v0.1.html:140 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-086 | 场次状态 | docs/business-enum-quick-reference-v0.1.html:142 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-087 | 游乐「当天／每天」口径 | docs/business-enum-quick-reference-v0.1.html:144 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-088 | 容量双阈值 | docs/business-enum-quick-reference-v0.1.html:145 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-089 | 园区容量 | docs/business-enum-quick-reference-v0.1.html:146 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-090 | 设备关键/辅助分级 | docs/business-enum-quick-reference-v0.1.html:147 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-091 | 全园故障与补偿通票 | docs/business-enum-quick-reference-v0.1.html:148 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-092 | 安全严重度 | docs/business-enum-quick-reference-v0.1.html:149 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-093 | 巡检任务状态 | docs/business-enum-quick-reference-v0.1.html:150 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-094 | 越界类型 | docs/business-enum-quick-reference-v0.1.html:151 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-095 | 团体/包场 | docs/business-enum-quick-reference-v0.1.html:153 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-096 | 游乐增强阶段 | docs/business-enum-quick-reference-v0.1.html:154 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-097 | 供应合同模式 | docs/business-enum-quick-reference-v0.1.html:159 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-098 | 库房分类 | docs/business-enum-quick-reference-v0.1.html:160 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-099 | 物品大类 | docs/business-enum-quick-reference-v0.1.html:161 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-100 | 库存流水类型 | docs/business-enum-quick-reference-v0.1.html:162 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-101 | 高价值判定 | docs/business-enum-quick-reference-v0.1.html:164 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-102 | 跨主体借调 | docs/business-enum-quick-reference-v0.1.html:166 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-103 | 智能采购建议 | docs/business-enum-quick-reference-v0.1.html:168 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-104 | 门店要货履约状态 | docs/business-enum-quick-reference-v0.1.html:169 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-105 | 经销商状态 | docs/business-enum-quick-reference-v0.1.html:170 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-106 | 经销询价状态 | docs/business-enum-quick-reference-v0.1.html:171 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-107 | 经销价格目录状态 | docs/business-enum-quick-reference-v0.1.html:172 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-108 | 经销订单状态 | docs/business-enum-quick-reference-v0.1.html:173 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-109 | 经销退货单状态 | docs/business-enum-quick-reference-v0.1.html:174 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-110 | 经销库存外显 | docs/business-enum-quick-reference-v0.1.html:175 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-111 | 经销支付方式 | docs/business-enum-quick-reference-v0.1.html:176 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-112 | 经销库存预留 | docs/business-enum-quick-reference-v0.1.html:177 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-113 | 经销职责分离 | docs/business-enum-quick-reference-v0.1.html:178 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-114 | 经销取消、退货与开票 | docs/business-enum-quick-reference-v0.1.html:179 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-115 | 经销价格与税务快照 | docs/business-enum-quick-reference-v0.1.html:180 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-116 | 经销商停用撤权 | docs/business-enum-quick-reference-v0.1.html:181 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-117 | 经销资金归属 | docs/business-enum-quick-reference-v0.1.html:182 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-118 | 运营营业日 | docs/business-enum-quick-reference-v0.1.html:187 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-119 | 对账口径 | docs/business-enum-quick-reference-v0.1.html:188 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-120 | 对账差异四类 | docs/business-enum-quick-reference-v0.1.html:189 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-121 | 日结状态 | docs/business-enum-quick-reference-v0.1.html:190 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-122 | 差异处理状态 | docs/business-enum-quick-reference-v0.1.html:191 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-123 | 结算状态 | docs/business-enum-quick-reference-v0.1.html:192 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-124 | 综合通票定价 | docs/business-enum-quick-reference-v0.1.html:193 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-125 | 商户业态 | docs/business-enum-quick-reference-v0.1.html:195 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-126 | 合作模式 | docs/business-enum-quick-reference-v0.1.html:196 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-127 | 退款资金流向 | docs/business-enum-quick-reference-v0.1.html:197 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-128 | 物业费 | docs/business-enum-quick-reference-v0.1.html:198 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-129 | 能源费与损耗费 | docs/business-enum-quick-reference-v0.1.html:199 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-130 | 税费配置 | docs/business-enum-quick-reference-v0.1.html:201 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-131 | 三方对账框架 | docs/business-enum-quick-reference-v0.1.html:202 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-132 | 开票状态 | docs/business-enum-quick-reference-v0.1.html:203 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-133 | 接入的外部系统 | docs/business-enum-quick-reference-v0.1.html:208 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-134 | 客户标签 | docs/business-enum-quick-reference-v0.1.html:209 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-135 | 统一原则 | docs/business-enum-quick-reference-v0.1.html:210 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-136 | 接入状态 | docs/business-enum-quick-reference-v0.1.html:211 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-137 | 回流结果状态 | docs/business-enum-quick-reference-v0.1.html:212 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-138 | 风险四级 | docs/business-enum-quick-reference-v0.1.html:217 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-139 | 事件状态 | docs/business-enum-quick-reference-v0.1.html:218 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-140 | 主管复核强制项 | docs/business-enum-quick-reference-v0.1.html:219 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-141 | 经营异常处置状态 | docs/business-enum-quick-reference-v0.1.html:220 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-142 | 异常状态的统一出口 | docs/business-enum-quick-reference-v0.1.html:221 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-143 | 四类主体 | docs/business-enum-quick-reference-v0.1.html:226 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-144 | 经营主体关系 | docs/business-enum-quick-reference-v0.1.html:227 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-145 | 城市项目公司 | docs/business-enum-quick-reference-v0.1.html:228 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-146 | 运营公司职责 | docs/business-enum-quick-reference-v0.1.html:229 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-147 | 主要收入归属 | docs/business-enum-quick-reference-v0.1.html:230 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-148 | 合作模式 | docs/business-enum-quick-reference-v0.1.html:231 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-149 | 职级 | docs/business-enum-quick-reference-v0.1.html:232 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-150 | 终端十一端 | docs/business-enum-quick-reference-v0.1.html:233 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-151 | 最终权限计算 | docs/business-enum-quick-reference-v0.1.html:234 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-152 | 项目与敏感数据权限 | docs/business-enum-quick-reference-v0.1.html:235 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-153 | 终端开关优先级 | docs/business-enum-quick-reference-v0.1.html:236 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-154 | 综合体项目状态 | docs/business-enum-quick-reference-v0.1.html:237 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-155 | 商户/运营主体状态 | docs/business-enum-quick-reference-v0.1.html:238 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-156 | 合同状态 | docs/business-enum-quick-reference-v0.1.html:239 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-157 | 员工状态 | docs/business-enum-quick-reference-v0.1.html:240 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-158 | 设备状态 | docs/business-enum-quick-reference-v0.1.html:241 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-159 | 权限状态 | docs/business-enum-quick-reference-v0.1.html:242 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-160 | 招商线索状态 | docs/business-enum-quick-reference-v0.1.html:248 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-161 | 招商意向阶段 | docs/business-enum-quick-reference-v0.1.html:249 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-162 | 品牌/客户库类型 | docs/business-enum-quick-reference-v0.1.html:250 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-163 | 合同变更类型 | docs/business-enum-quick-reference-v0.1.html:251 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-164 | 退租结算状态 | docs/business-enum-quick-reference-v0.1.html:252 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-165 | 内部应付执行状态 | docs/business-enum-quick-reference-v0.1.html:253 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-166 | 合同审批链 | docs/business-enum-quick-reference-v0.1.html:255 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-167 | 租金/账单类型 | docs/business-enum-quick-reference-v0.1.html:256 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-168 | 营业额抽成模式 | docs/business-enum-quick-reference-v0.1.html:257 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-169 | 公区能源费 | docs/business-enum-quick-reference-v0.1.html:258 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-170 | 铺位租赁状态 | docs/business-enum-quick-reference-v0.1.html:259 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-171 | 物业工单类型 | docs/business-enum-quick-reference-v0.1.html:260 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-172 | 物业工单状态 | docs/business-enum-quick-reference-v0.1.html:262 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-173 | 商户服务工单状态 | docs/business-enum-quick-reference-v0.1.html:264 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-174 | 工单时效 | docs/business-enum-quick-reference-v0.1.html:265 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-175 | 楼宇机电设备状态 | docs/business-enum-quick-reference-v0.1.html:266 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-176 | 设备维保类型 | docs/business-enum-quick-reference-v0.1.html:267 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-177 | 能耗表计类型 | docs/business-enum-quick-reference-v0.1.html:268 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-178 | 抄表状态 | docs/business-enum-quick-reference-v0.1.html:269 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-179 | 能耗归集对象 | docs/business-enum-quick-reference-v0.1.html:270 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-180 | 楼宇巡检任务状态 | docs/business-enum-quick-reference-v0.1.html:271 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-181 | 楼宇巡检类型 | docs/business-enum-quick-reference-v0.1.html:273 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-182 | 考勤日结果 | docs/business-enum-quick-reference-v0.1.html:279 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-183 | 打卡方式 | docs/business-enum-quick-reference-v0.1.html:280 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-184 | 请假类型 | docs/business-enum-quick-reference-v0.1.html:281 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-185 | 请假单状态 | docs/business-enum-quick-reference-v0.1.html:282 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-186 | 薪资单构成 | docs/business-enum-quick-reference-v0.1.html:283 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-187 | 员工提成发放 | docs/business-enum-quick-reference-v0.1.html:285 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-188 | 服务订单状态 | docs/business-enum-quick-reference-v0.1.html:290 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-189 | 管理费方式 | docs/business-enum-quick-reference-v0.1.html:291 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-190 | 分红基数 | docs/business-enum-quick-reference-v0.1.html:296 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-191 | 分红比例 | docs/business-enum-quick-reference-v0.1.html:297 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-192 | 分红税务 | docs/business-enum-quick-reference-v0.1.html:298 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-193 | 分红周期 | docs/business-enum-quick-reference-v0.1.html:299 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-194 | 分红单状态 | docs/business-enum-quick-reference-v0.1.html:300 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-195 | 可选扣除项 | docs/business-enum-quick-reference-v0.1.html:301 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-196 | 发放保护线留存 | docs/business-enum-quick-reference-v0.1.html:302 | 现文原段一致；与全篇流程合取无冲突 |
| BEQR-197 | 期间关账 | docs/business-enum-quick-reference-v0.1.html:303 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-001 | 整体说明：这套系统到底管什么 | docs/business-system-map-for-operations-v0.1.html:505 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-002 | 一笔业务怎么走 | docs/business-system-map-for-operations-v0.1.html:526 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-003 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | docs/business-system-map-for-operations-v0.1.html:563 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-004 | 配置数据可以存档、也可以删除（删除要审批） | docs/business-system-map-for-operations-v0.1.html:566 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-005 | 后台的"新建/录入"改成分步引导 | docs/business-system-map-for-operations-v0.1.html:581 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-006 | 验证用的二维码一律动态刷新（间隔可在后台设定） | docs/business-system-map-for-operations-v0.1.html:584 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-007 | 平台管理员拥有全部日常功能权限 | docs/business-system-map-for-operations-v0.1.html:587 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-008 | 子系统入口 | docs/business-system-map-for-operations-v0.1.html:592 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-009 | 基础管理 | docs/business-system-map-for-operations-v0.1.html:596 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-010 | 票务权益 | docs/business-system-map-for-operations-v0.1.html:601 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-011 | 客人端 | docs/business-system-map-for-operations-v0.1.html:606 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-012 | 酒吧平板/工作手机 | docs/business-system-map-for-operations-v0.1.html:611 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-013 | 触摸屏一体机 | docs/business-system-map-for-operations-v0.1.html:616 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-014 | 酒吧管理 | docs/business-system-map-for-operations-v0.1.html:621 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-015 | 供应链库存 | docs/business-system-map-for-operations-v0.1.html:626 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-016 | 游乐管理 | docs/business-system-map-for-operations-v0.1.html:631 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-017 | 商户管理 | docs/business-system-map-for-operations-v0.1.html:637 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-018 | 招商租赁 | docs/business-system-map-for-operations-v0.1.html:643 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-019 | 物业服务 | docs/business-system-map-for-operations-v0.1.html:650 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-020 | 楼宇运营 | docs/business-system-map-for-operations-v0.1.html:656 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-021 | 第三方接入 | docs/business-system-map-for-operations-v0.1.html:661 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-022 | 留痕与异常 | docs/business-system-map-for-operations-v0.1.html:666 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-023 | 财务对账 | docs/business-system-map-for-operations-v0.1.html:671 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-024 | 持股分红 | docs/business-system-map-for-operations-v0.1.html:676 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-025 | 经营看板 | docs/business-system-map-for-operations-v0.1.html:681 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-026 | 各类入口 | docs/business-system-map-for-operations-v0.1.html:686 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-027 | 基础管理：先把综合体的盘子管清楚 | docs/business-system-map-for-operations-v0.1.html:694 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-028 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:698 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-029 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:714 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-030 | 票务权益：管卖什么票、给什么权益、怎么核销 | docs/business-system-map-for-operations-v0.1.html:727 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-031 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:731 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-032 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:760 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-033 | 票种、通票与权益类型（系统支持哪些种类） | docs/business-system-map-for-operations-v0.1.html:772 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-034 | 核销、退款与有效期规则 | docs/business-system-map-for-operations-v0.1.html:796 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-035 | 会员与积分规则 | docs/business-system-map-for-operations-v0.1.html:813 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-036 | 人脸验龄数据使用与删除 | docs/business-system-map-for-operations-v0.1.html:840 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-037 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | docs/business-system-map-for-operations-v0.1.html:850 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-038 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:854 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-039 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:873 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-040 | 登录与关键规则 | docs/business-system-map-for-operations-v0.1.html:883 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-041 | 会员注册、认证与收费边界 | docs/business-system-map-for-operations-v0.1.html:898 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-042 | 会员积分活动与发券 | docs/business-system-map-for-operations-v0.1.html:907 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-043 | 会员营销明确边界 | docs/business-system-map-for-operations-v0.1.html:916 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-044 | 酒吧平板/工作手机：平板点酒，工作手机核销 | docs/business-system-map-for-operations-v0.1.html:929 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-045 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:933 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-046 | 平板图形点单（POS） | docs/business-system-map-for-operations-v0.1.html:953 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-047 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:963 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-048 | 触摸屏一体机：权益领取、现场服务和辅助售票 | docs/business-system-map-for-operations-v0.1.html:978 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-049 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:982 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-050 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:996 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-051 | 酒吧管理：从入场、开台到结账关台 | docs/business-system-map-for-operations-v0.1.html:1010 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-052 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1014 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-053 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1041 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-054 | 桌台/账单状态与经营规则 | docs/business-system-map-for-operations-v0.1.html:1053 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-055 | 桌台、菜单与优惠券 | docs/business-system-map-for-operations-v0.1.html:1088 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-056 | 正式未付款订单挂起 | docs/business-system-map-for-operations-v0.1.html:1101 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-057 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | docs/business-system-map-for-operations-v0.1.html:1120 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-058 | 怎么点、怎么收钱 | docs/business-system-map-for-operations-v0.1.html:1124 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-059 | 钱怎么分、怎么退 | docs/business-system-map-for-operations-v0.1.html:1135 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-060 | 供应链库存：管供应商、采购、库房、消耗和存酒 | docs/business-system-map-for-operations-v0.1.html:1151 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-061 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1155 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-062 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1174 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-063 | 库存类型与规则 | docs/business-system-map-for-operations-v0.1.html:1186 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-064 | 采购建议与门店要货 | docs/business-system-map-for-operations-v0.1.html:1208 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-065 | 外部经销商准入、价格与库存 | docs/business-system-map-for-operations-v0.1.html:1216 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-066 | 经销订单、付款与履约 | docs/business-system-map-for-operations-v0.1.html:1226 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-067 | 经销退货、退款、红冲与资金归属 | docs/business-system-map-for-operations-v0.1.html:1238 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-068 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | docs/business-system-map-for-operations-v0.1.html:1253 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-069 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1257 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-070 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1276 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-071 | 游乐类型与规则 | docs/business-system-map-for-operations-v0.1.html:1287 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统 | docs/business-system-map-for-operations-v0.1.html:1308 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-073 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1313 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-074 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1326 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-075 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | docs/business-system-map-for-operations-v0.1.html:1339 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-076 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1343 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-077 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1357 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-078 | 物业服务：商户和现场的报修、投诉、整改都进工单 | docs/business-system-map-for-operations-v0.1.html:1370 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-079 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1374 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-080 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1388 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-081 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | docs/business-system-map-for-operations-v0.1.html:1401 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-082 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1405 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-083 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1414 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-084 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | docs/business-system-map-for-operations-v0.1.html:1427 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-085 | 日常工作 | docs/business-system-map-for-operations-v0.1.html:1431 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-086 | 排班、考勤与请假 | docs/business-system-map-for-operations-v0.1.html:1441 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-087 | 我的薪资 | docs/business-system-map-for-operations-v0.1.html:1455 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-088 | 员工提成、活动执行与人员服务边界 | docs/business-system-map-for-operations-v0.1.html:1467 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-089 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | docs/business-system-map-for-operations-v0.1.html:1479 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-090 | 机器人配送与扫码交接 | docs/business-system-map-for-operations-v0.1.html:1545 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-091 | 视频监控（留点位/时间） | docs/business-system-map-for-operations-v0.1.html:1554 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-092 | 重要事项短信通知（2026-06-29 新增） | docs/business-system-map-for-operations-v0.1.html:1563 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-093 | 留痕与异常：重要动作留得住，出了问题追得回 | docs/business-system-map-for-operations-v0.1.html:1574 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-094 | 必须关注的动作 | docs/business-system-map-for-operations-v0.1.html:1578 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-095 | 怎样减少人工填写 | docs/business-system-map-for-operations-v0.1.html:1596 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-096 | 处理结果 | docs/business-system-map-for-operations-v0.1.html:1607 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-097 | 风险等级与风控规则 | docs/business-system-map-for-operations-v0.1.html:1619 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-098 | 财务对账：把经营事实整理成财务能确认的口径 | docs/business-system-map-for-operations-v0.1.html:1636 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-099 | 内部结算价目录（按商品规格逐项建目录） | docs/business-system-map-for-operations-v0.1.html:1639 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-100 | 结算配置中心（后台总控页） | docs/business-system-map-for-operations-v0.1.html:1641 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-101 | 项目零售参考价 | docs/business-system-map-for-operations-v0.1.html:1643 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-102 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1648 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-103 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1666 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-104 | 收入归属：哪类收入归哪个业态主体 | docs/business-system-map-for-operations-v0.1.html:1678 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-105 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | docs/business-system-map-for-operations-v0.1.html:1697 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-106 | 对账与结算类型规则 | docs/business-system-map-for-operations-v0.1.html:1709 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-107 | 持股分红：按持股把经营成果定期分给主理人 | docs/business-system-map-for-operations-v0.1.html:1737 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-108 | 主要功能 | docs/business-system-map-for-operations-v0.1.html:1741 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-109 | 谁会用 | docs/business-system-map-for-operations-v0.1.html:1760 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-110 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1768 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-111 | 经营看板：让管理层和运营看得懂、能追问 | docs/business-system-map-for-operations-v0.1.html:1781 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-112 | 主要指标 | docs/business-system-map-for-operations-v0.1.html:1785 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-113 | 谁会看 | docs/business-system-map-for-operations-v0.1.html:1813 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-114 | 各类入口：不同人只看自己该看的、只做自己该做的 | docs/business-system-map-for-operations-v0.1.html:1827 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-115 | 相互关系：为什么这些子系统必须连在一起 | docs/business-system-map-for-operations-v0.1.html:1897 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-116 | 系统交付与上线：正式上线要做的安全加固和安装交付 | docs/business-system-map-for-operations-v0.1.html:1928 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-117 | 正式上线时的安全加固 | docs/business-system-map-for-operations-v0.1.html:1932 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-118 | 能自动识别环境的安装包 + 内附教程 | docs/business-system-map-for-operations-v0.1.html:1940 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-119 | 和谁有关 | docs/business-system-map-for-operations-v0.1.html:1947 | 现文原段一致；与全篇流程合取无冲突 |
| BSM-120 | 已确认规则和实施前输入项 | docs/business-system-map-for-operations-v0.1.html:1957 | 现文原段一致；日期副本已同步 |

## 对抗式具体反例
1. 用手环试图跳年龄核验：枚举43通用三道与44同场条件合取，地图800明确重复进出仍三道；无明文豁免。不虚构手环绕过。
2. 第二段无人核销试改余额：枚举52明确有参与者仍逐家减除，只有从无第二段参与者才余额；并列17.5%，默认1至10家行合计100%，未混第一段和第二段。
3. 用明文商户权限看客人：现PGM871仅第一/第二商户联系人例外，BSM客人检索脱敏仍成立；导出打印另授权且不继承页面例外。
4. 以无通知不算异议期：BSM1566按商户专区可见次日起，完整自然日不可用才扣除，通知成败不影响；与积分独立账期不混。
5. 退款批准与到账混淆：枚举退款成功才计退额，资金在途保持待付不重付，失败有重付期限；不把批准等于完成。
6. BSM521种类须写HTML与1020客源、1068折扣理由：B/C操作员确认原意，按具体条款可配；未重新提出变更。
7. 全部物业工单都强制双确认：1380已按分类型回访验收关闭，投诉按商户/消费客人不同分支，保洁不被一般句覆盖。
8. 私人手机标题“考勤”强推现场打卡：930/1865明确查看考勤，1445明确工作手机打卡；标题是域名不是授权例外。
9. 跨月借业务日回改历史：考勤班次结束例外与经营开台营业日区别明示；结算固化版本、冲正而非改历史。
10. 单票无部分退款和订单“部分退款”不是同一粒度，不能以枚举目录取消已确认业务；股权建议80/20不覆盖工商实际。
11. 实物积分券发券预占、领取实扣与退回EXPIRED不可用相容；EXPIRED描述流水内部状态且已有中文，不证明客人界面英文。
12. 主理人店内经营数据可见与跨项目隔离同存，内部结算价仍需专权；不能推定财务审批含自审或受益人豁免。

## 真实命令与结果（明确复用边界）
本轮B5新跑哈希、317摘录与实际内联导航脚本的node:vm断言；结构/映射/UTF8复用同字节本轮B1核验。下列entry-anchors与其测试、完整结构计数是同一会话V0.40实际结果，本轮文件字节未变，复用而未重跑；不声称新跑。
- Python3 HTMLParser：两文件strict UTF8通过、UFFFD=0；duplicates=[]；badlinks=[]。枚举13thead/26th；地图27section/27h2/93h3，45a均未发现失效片段。
- 本轮新跑Python冻结定义正则逐条提取来源和html fence，对现文substring：317/317一致，M001映射335且无缺号；两文件哈希与V0.40全文阅读时相同。
- npm run verify:entry-anchors：exit0，1228注册表/1235账本/1244矩阵/1565镜像/440Markdown，PASS。
- node --test tools/verify-entry-anchors.test.mjs：exit0，tests1 pass1 fail0；历史raw可放行，现行失效引用仍拒绝。
- 没有运行全仓predeploy、浏览器或后端测试，不据此声称它们绿。没改HTML/业务代码/DB。

## B5新增实际行为验证
读取BSM唯一<script>实际源码，以node:vm及node:assert/strict在只读模拟DOM执行，注册真实scroll回调。断言初始只有首项高亮，逐一移动27节位置后每次恰有对应一项高亮，监听passive=true。exit0，输出：PASS actual BSM inline script: initial active and 27 scroll targets; one active each; passive listener。不声称等同完整浏览器视觉验收，未写测试文件或改HTML。

## 历史16项及当前发现全集
001/002/003/005/006/008/012/013/016已修现场仍保留，分别核租赁状态/注销一年/合同八类交叉/两类投诉/容量/表头/目录日期版本/物业分类分支；004技术性无缺陷；007/009/010/011细化无缺陷；014/015按B/C确认原意无需修。历史编号和误判证据不删。
既有技术性登记M001-004（B1 CX-01）：两文件静态样式结构导航，技术性、无业务发明、工程缺陷否，依据GEN001/002/015、实际HTMLParser与源码；沿用M001-004，不新编号。
本席B5当前新增发现空，无工程缺陷或添加项；findings V3.32已B2双PASS的16项队列为空，没有需再回B2的新问题。上轮Grok GR01与本席已查出的日期差已通过完整LOCK-AMEND消除，所有旧FAIL原票保留。本轮没有旧日期残留，不给它新产品ID。
本席B5结论：PASS。仍非单席模块PASS；必须等Grok本次完整B5票及B5后四值一致，才能先更新ledger为PASS再更新收口文件。本次B3新增0、累计4。
