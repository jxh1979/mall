# M001 — 业务需求正本／需求HTML／第1段
版本：V0.04

## 当前收口：471模块计划 / 160-audit-prompt V0.17

状态：PASS。2026-09-08，B5-r4两席完整报告均已落盘并完整读取，B5后四值校验全等；已先将ledger V1.58的M001记PASS，现更新本收口文件。以下旧V0.03内容作为历史原样保留，不继承其旧锁或322模块结论。

本轮按操作员“授权仅为 A、D 再执行 1 次修复”，实际仅执行一次B3（累计3→4），只改BSM的L1380概括句与L2版本号；两树完整diff只1文件、2增2删。A现为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”；D由V0.12递增为V0.13，日期仍2026-09-08。
操作员随后明确：“b c两项原文不冲突。就是我的原意。”B（L1020客人来源）和C（L1068打折原因）按该原意保留。两席已撤销冲突/工程缺陷判断，014/015现为细化、无缺陷、无需修复；不是容忍已确认缺陷，也不是改完了。Grok曾新提的红线概述、票名/有效期、补录短表、合作模式两行及越权程序疑点，经其独立完整原文复核均撤销，旧票保留历史。

| 当前文件 | 版本 | 行数 | SHA256 |
|---|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | V0.07 | 308 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | V0.13 | 2032 | a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa |

两席覆盖两份HTML共2340行、335映射判据（197BEQR+120BSM+18GEN）、第一性原理/顺从式/对抗式三线。317摘录按来源行逐字一致，结构与27导航原脚本检查、金额正反例通过。未运行产品后端交易、浏览器或读屏器验收；结论仅覆盖该文档模块，未把76未发布文件作为代码PASS证据。

| 阶段 | 当前有效证据 |
|---|---|
| B3-r4 | before 4263c8c1338031a38d977741ffb111a0752b7092；after 7d6c4f7544de2957faec09e04aa2a262ba4aad86；未过滤binary diff 2013字节，SHA256 57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46 |
| B4-r4 | self/grok均V0.01、双PASS，完整diff各自留存；self SHA70245a3b39f5de88e4d59c9887e4ab1a86f437894dfb3bd2b94d1121ca2e997c；grok SHAb4766eddfc1ea4e8789c4769d33e722abb1031c2a0e5d168304d7f88103e0c9e |
| LOCK-AMEND | BSM079获批出处同步；双PASS、反向旧hash与401定义/383摘录/471映射实核，完整五步已完成。锁V0.11，COUNT保持1。Grok完整IN-RECEIPT见ledger与self回执 |
| 新锁B1与B2 | self V0.36/Grok V0.17当前B1双PASS；self V0.38/Grok V0.18 B2双PASS；findings V0.86定稿16行、队列空，跳过新的B3/B4 |
| B5-r4 | modules/M001-b5-r4-self-raw.md V0.01，SHAb98a83bd01295decdbeb20021e80773e072da15e03603353050ad9b522e917dc；modules/M001-b5-r4-grok-raw.md V0.01，SHA1334b1e26cd7d4c99ee942534ad6e44eab5ae5c99676cd020fd1ea992c68bbad；双PASS，Grok真实exit0 |

B5后四值实核：
- 实算：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
- sidecar：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
- ledger：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
- Grok B5：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

当前main HEAD=242f487233d028173cb9198eecd079f5d877405c，未提交、推送、部署。B3累计4、额外额度用尽；计划修复2、历史调用失败6保持。471模块当前1个PASS、470个PENDING，本轮不启动M002；不是全仓审计完成。全部实际Grok命令及退出状态见M001-b1-self-raw.md末尾本轮回执，所有历史原票保留。

## 历史V0.03收口与发布记录（旧322模块计划，非当前基准）


状态：PASS。首轮全仓审计，M001/322；控制提示词V0.13；锁V0.05。
B5-r2两席报告全部落盘并完整读取后，四值校验通过，先将ledger标记PASS，再写本文件。

## 范围与结果
| 文件 | 当前版本 | 行数 | SHA256 |
|---|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | V0.07 | 308 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | V0.08 | 2032 | 3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea |

两席全量2340行/335判据（18GEN+197BEQR+120BSM），三线完成；B5均PASS、无新工程缺陷或添加项。M001当前11项登记均不在修复队列，无未答questions或HTML申请。已批准14/18/19最终两类投诉稿、15/16容量引用、17表头均已执行；旧稿不再执行。
B3累计2次；V0.13 LOCK-AMEND-COUNT=0，历史5次获批HTML引文同步及18个ID依据保留于ledger，未增加第6次。

## 可复核证据
| 阶段 | 证据路径/结果 |
|---|---|
| 当前锁B1 | modules/M001-b1-self-raw.md V0.25完整报告、modules/M001-b1-grok-raw.md V0.08完整报告，双PASS |
| B2定稿 | 上述两份b1 raw末尾分别追加V0.26/V0.09双PASS；同一findings V0.17候选；后续V0.19仅控制迁移，11项队列不变 |
| B3-r2 | before tree fa2e80597255da1277291d5ba2e8146f9334bd53；after tree ff5573a74f5ed84f6bc84d73ad85af7a63a4d961；未过滤完整binary diff SHA256=041e46f19586f8ed63cdf0cfd89895b3afcd5e79e1586567a029d8bb217e908e |
| B4-r2 | modules/M001-b4-r2-self-raw.md SHA256=1edaff6616ab23b1709dc01d99e9a687dafbc260486e513f96c84417f774e4a2；modules/M001-b4-r2-grok-raw.md SHA256=214eedda3e430191bda987e1242d0a622527739602cb2212772a6bdd3f1bc118；双PASS |
| B5-r2 | modules/M001-b5-r2-self-raw.md V0.01 SHA256=edf52ea0729337f1c99c72bcce612a0fd79c275961a32ab0f6b9a3dee7f69b36；modules/M001-b5-r2-grok-raw.md V0.01 SHA256=7e2aba70086c8e2b68dce26f3ec2584b15fefb8e83a6def6a9c3089c76a4364f；双PASS |

B5后四值（已原样打印操作员回执）：
- 实算：73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439
- sidecar：73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439
- ledger：73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439
- Grok本次报告：73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439

复现命令与完整输出证据见两席B5 raw：317锚点精确对照、26张表结构、45内部链接及27真实导航、实际JS三种位置及scroll回调、当前10档排名比例合计100%、负尾差算例均通过。没有运行浏览器/读屏器/后端生产交易，本结论仅覆盖权威HTML模块；后续实现仍按其各模块审计。
源HEAD仍2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2，真实Git索引未改变。审计收口时尚未提交、推送或发布。操作员随后重申每模块提交合并推送：现补分支提交、PR门禁和合并，发布完成后再推进M002。

交付范围为本模块已批准的三份HTML同步修复与M001审计证据；治理地图的两处已批准同步修改已在M001修复/锁同步证据中留痕，不等同于M002全量审计已完成。发布工作区只搬运已审字节，不新增业务修改。

发布门禁补录：业务裁决台账V0.17承接全部已批准事项；入口注册表15行与规则账本15行机械镜像同步，ID/名称/行锚/列结构/覆盖矩阵不变；verify:html-ledger-sync与verify:entry-anchors已通过。三份HTML仍与B5原始哈希一致。其余predeploy门禁与PR合并结果继续以实际执行回执为准。
