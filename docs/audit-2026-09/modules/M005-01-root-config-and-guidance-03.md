# M005 治理门禁与 CI 基线／配置与工程入口／第3段
版本：V0.02

结论：PASS。以当前471模块计划为准，本文件替代旧计划同编号收口；旧raw全部保留，不继承旧范围PASS。正本/home/james/complex-management，main，HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。

| 当前范围 | 行数 | SHA256 |
|---|---|---|
| tsconfig.base.json | 13 | 13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db |
| .github/CODEOWNERS | 12 | b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d |
| .github/workflows/predeploy-gates.yml V0.02 | 27 | 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d |

全部52行、GEN-001—018，第一性原理/顺从式/对抗式均已执行。技术性012/013只登记；014将CI职责误述修正，生效YAML及ops/ci镜像各只改L6说明和L1自身版本，B4双PASS后出队。镜像为直接依赖同步，不代表提前审完M007。

- B2：本方b1 raw V0.09、Grok b1 raw V0.07对候选findings V0.97双PASS，V0.98定稿。
- B3累计2；本次两树f6390b5328ba446786d04408d3780ef68c269425→7abf713d9c735c93b3c27ff5af5d6e9ff4c6f5d0，完整diff保存在两份[M005-b4-r2-self-raw.md](M005-b4-r2-self-raw.md)/[M005-b4-r2-grok-raw.md](M005-b4-r2-grok-raw.md)，两席范围/归因均PASS。
- B5：[本方完整报告](M005-b5-r2-self-raw.md) V0.01，30126bytes，SHAb922fb74fdc36f994c603e22def06541d510f42a48a776fa116ccb9efee6a14c；[Grok完整报告](M005-b5-r2-grok-raw.md) V0.01，16689bytes，SHA0bd729eebbd18bb266a9137f1170e367af347a2e023c93f047682004ef1b8271；双PASS。
- B5后实算/sidecar/ledger/Grok四值全为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；COUNT1。核对后ledger V1.73先记PASS，再写本收口。
- 当前无待修/添加/未答项；方案修复累计2、Grok交付失败0。三份业务HTML与TS/CODEOWNERS未改。

验证：真实TS353输入配置与正负诊断、9条CODEOWNERS规则及反例、YAML/镜像、hook三probe0/1/0、现有verify:delivery-gates-docs exit0；根另查当前GitHub owner/token/action/保护API。Grok未查API的限制保留，不冒称双方都查过。报告中“16闸”计数已实算纠正为15，三个入口列表逐项相同，未改其原raw。

当前verify:predeploy因既有BSM/业务决策台账不同步在第一闸exit1，后续按所属模块处理；本PASS仅M005，不是全仓或发布通过。原始无效/失败报告保留为历史；本次使用新独立B1、B2、B4、B5链收口。下一模块M006按当前plan/modules.md继续。


# M005 当前锁复审收口
版本：V0.03
UTC 2026-09-22T10:57:45.892261+00:00
当前状态：PASS（仅M005）。本次先ledger V7.74登记PASS，后追加本收口。当前main HEAD e456ba96c84185c8d39f18f548011daac48a6466；本地origin/main指针ebb1eb99c610df330c07d24278798c2d3611299a，未fetch；不继承前段旧基线/旧PASS。
三源全52行、18GEN全三线：tsconfig.base.json 13行SHA13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db；CODEOWNERS12行SHAb8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d；workflow27行V0.02 SHA4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d。
B1 selfV0.10/GrokV0.08完整独立双PASS；B2 selfV0.11/GrokV0.09整批双PASS，findings V3.40定稿。当前012/013/014无缺陷队列否，旧001—011历史原范围保留。跳新增B3/B4，历史B3累计2不变，不重修014。
B5-r2 self V0.02 41970bytes SHAbc0bf104b19f7f29ebd5acd55c222721f401192434347a970b6da854e70abcfa；Grok V0.02 54683bytes SHA03b72441817f491314cb7558ccee500e32df1c56d27f7a82ae76ea6213ca44e1，完整报告26237字符全部读完，真实exit0。旧raw前缀逐字保留。
四值实算/sidecar/ledger/Grok均b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，COUNT1。
真实验证：原生TS354输入与可红诊断、9owner规则及保护限制、YAML固定SHA/事件/基线/镜像、hook0/1/0、docs checker exit0、门禁拒绝错误基线及PGM不同步。未全业务构建/安装/触发CI/部署，不把旧origin CI绿当工作区绿。
全局PGM台账同步红M426/工具M008、入口快照M435/M436保留。基线文案origin/main@88c7b465为merge-base祖先显示，ref本身ebb1eb99，两者已两席澄清。未改三业务HTML/源/锁。M006下一模块按计划继续；M009获批单次消费修复尚未实施。
