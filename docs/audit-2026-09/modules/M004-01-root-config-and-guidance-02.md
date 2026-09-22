# M004 — 治理门禁与 CI 基线／配置与工程入口／第2段
版本：V0.03

状态：PASS。V0.17、471计划、锁V0.14。已先在ledger V7.70写入PASS。
唯一package-lock.json3321行/220记录/18GEN；源documentVersionV0.02，SHA d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef。本轮无源码改动，B3累计1、方案修复2、失败0不变。

B1 selfV0.08/GrokV0.06均PASS；B2 selfV0.09/GrokV0.07整批PASS，findingsV3.38七项全否。跳新增B3/B4，完整B5-r1 selfV0.04 190568bytes SHA f8ee5cc230aa5dc736ba48d0644340e9d513071c3d3da84e56669838fa27a3d4；GrokV0.03 231170bytes SHA 37122326694e71df96d38e5a2a6306fded049268507a8765d506f9f9cef66a16。根先封存后读全部57260字符新外审（含前导换行），真实exit0、两席PASS。B5后实算/sidecar/ledger/Grok四值均 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，COUNT1。

全图220/582、清单镜像、116engine、217URL/SRI形态与错误payload拒绝均通过，原生ls与dry-run0；dry不是安装或构建测试。audit仍exit1/moderate2，同GHSA涉及Vitest/mocker；当前未启公开插件入口，007按铁律6只登记，不升级。磁盘nanoid3.3.12/postcss8.5.15/undici7.28.0仍旧，锁为3.3.18/8.5.23/7.29.0；不能称修补版已运行。未全量下载tarball或实际安装/业务测试/部署/提交。全局PGM台账/入口快照红保留。

外审历史措辞限制：V0.03把旧V0.01/V0.02合说成65254915/V0.01/audit绿，不能采信该历史概括。实际旧V0.02已明确d3676e/V0.02/audit红；当前同字节复用依据是新B1V0.06和实算，现结论不依赖该旧票概括。原文不改，无源码finding。

## 历史 V0.02 原文（不代表现锁）

# M004 — 01-root-config-and-guidance-02
版本：V0.02

当前471计划状态：PASS。2026-09-08按160-audit-prompt V0.17在160正本main重新完成B1、B2、B5，未继承旧322同号通过。B5双PASS及四值相等后先写ledger V1.66的M004 PASS，随后才写本收口。

范围：package-lock.json全3321行/114258字节/220条、GEN001—018。源documentVersion V0.02，SHA d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef；HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。本次源零改动；历史B3累计1、方案修复2、交付失败0保持。

B1当前两席完整三线PASS。B2同一候选V0.93 SHA2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78经两席PASS后定稿findings V0.94，SHAc7fcaaee7d1efc9a3137c2c380defecf2487c56572a42e49a9f80585dc14f1b5；001—007均技术性、当前修复队列否。001—003旧缺陷/已修史保留。007是同一Vitest公告涉及两包，真实audit exit1/moderate2；当前jsdom/react配置无公开mock插件入口，按铁律6仅登记。未宣称公告误报或npm audit全绿。

本次跳B3/B4，完整B5-r1仍重新执行。先Grok实际exit0，根独立完整保存后才读外席。两席B5 PASS，无新增finding/添加/HTML问题/未答闸。
- modules/M004-b1-self-raw.md 当前V0.07，SHA d6f1742dd5cb128728802b072a7c875c4d5e65b2b2c1cc786eb018feb719824a。
- modules/M004-b1-grok-raw.md 当前V0.05，SHA c3cf6898805d515b7bfcb49f106d996e056b0b78ed8e51220fa1edec3012a3b1。
- modules/M004-b5-r1-self-raw.md 当前V0.03，SHA e83796801539f549ac86afcbc236014f70b45104da8a4c4a58aaa267759e46c8；本次独立完整段以旧89089字节后的新增内容及保存时全SHA bc8c0d5beb4b67b5d86900bdd8c7521e94d4afe0f1d8cc2a91310c00eb1ff2ba定位。新增段误沿用历史V0.02，已追加V0.03更正登记，原独立内容与旧前缀不覆盖。
- modules/M004-b5-r1-grok-raw.md 当前V0.02，SHA 951be0608835cd8ed9df191bee4646cb5aeafcc97dd07b96ec1514116f97ffd1；本次追加L430—935全506行已读，旧95358字节保留。

B5后四值actual/sidecar/ledger/Grok当前最后独占字段均b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，LOCK-AMEND-COUNT=1，锁V0.11未动。统计展示限制（Grok563/582、三元数据行SRI标签、yaml闭合行）均在self接收段标明，不硬称两席每个数字一致，不改写外席原文。

验证：本次原生220条/582边、必需边无invalid、无extraneous/unreachable；根/web镜像一致；116engine相容；28平台约束均optional；217个完整SRI格式和错payload拒绝；ci离线dry-run0，真实未安装；缺react/错版本反例有效。外席另做原生ci副本负例并明确loadVirtual/dry-run的证明边界。当前源与三HTML hash不变。
未做：真实npm安装、全前后端/数据库/设备/业务UI验证或部署；未下载全部tarball；未用npm10.9.8实跑。全局BSM镜像门禁仍红，不代表全仓/发布已通过。没有提交、推送、发布动作。下一模块M005按当前计划串行开工。

以下完整保留V0.01旧322计划历史收口。其旧版本/旧源SHA/旧公告结果/发布描述均为历史，不覆盖本次结论或授权。


# M004 — 01-root-config-and-guidance-02
版本：V0.01

状态：PASS。首轮全仓审计M004/322；控制V0.17、锁V0.06。B5-r1双席完整报告与四值相等成立后，先写ledger PASS，再写本文件。

## 范围与结果

唯一源package-lock.json，OVERSIZE-SINGLE-FILE，当前3321行/114242字节/220记录/18GEN，源SHA256 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc。初始3320行，修复增加自身documentVersion V0.01一行；应用版本0.1.0和lockfileVersion 3不变。B3累计1，运行失败重跑0；六ID均出队，新增0、未答0。

修复nanoid 3.3.12→3.3.18、postcss 8.5.15→8.5.23、undici 7.28.0→7.29.0，对应官方resolved/integrity及PostCSS内nanoid范围同步。三个均为现有dev依赖的同主版补丁，未证明生产利用路径。其余217记录不变，三份业务HTML与35个既有scratch文件不变。

## 审计链证据

B1与全部B2：M004-b1-self-raw.md V0.04 SHA789448acf5ea1f33bab8ad340c94b873b97b0ee2873a8e6f521bd65686a62950；M004-b1-grok-raw.md V0.03 SHAabbb09df87fd38e272738558f170171a8f30077be8755570c6390597613b7d21。原B1本方FAIL/外席PASS差集归并六ID；B2原版本字段分歧保留，用户版本铁律和实际npm序列化核实后候选V0.44 SHA209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2双PASS，才整批B3。

B3：before tree d4445654c7afca5232ec15f2fdd7ae3e39951756 → after tree 42e9241c8bc8bf71565166e20885a005b741e50e。全部路径binary diff 2259字节/5hunk/1文件，SHA59384beb2fb6844714b818d17319992f11012b7737988e574b11dbbe8ab56563；5hunk分别归001/001/002/002/003。两树各3514路径，只有源锁blob不同。

B4：M004-b4-r1-self-raw.md原独立12083字节SHA02e6d6ffd88c6f9b515ef2537d1192e0b40f8588d1cd1af2c98c4b16496a50ef，追加接收后全文件SHAe8462d5b258fdf83619c829007635c570fbf468c673b4d07dbf44c8372c8af0d。Grok首20152字节报告意外见self正文，不采信该票；新进程独立复审在同名raw追加完整报告，追加段SHA512af0048edaeade1c6bf27de3f2bb7f2c51407d010b3f662432a3f4e6b4412e、全文件SHA8d8ac42fcdab0a45b509026766ca72ff558815d50d581a5bd82517569b1fc80e。有效双PASS且越界双PASS。

B5：M004-b5-r1-self-raw.md原独立V0.01 SHAaec13af9f4be9998af84839dc7411e1dd2d058fd2d98da909d886863c2f3d43e；收到外席后仅追加事实校正，V0.02全SHA babfcc9aa382b4d329e0dc5cafc16f6d874da5c0d5545c786c297b160510ed0e。M004-b5-r1-grok-raw.md原95358字节SHA31f7bd9586520ad2ab89fd4b3bbc5a95fa596d081b68a2a343ce60af5ced264f，真实exit0、完整429行已读。整模块两席PASS，无新问题。外席安装原子性和客人端标签两处说法不采信，原文保留；当前源无这些业务/事务声明，详见本方接收段。

B5后实际、sidecar、ledger、Grok四值均23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。

## 验证与边界

两席全量检查220记录、根/web镜像、链接、普通/optional/peer依赖、平台、引擎、嵌套和217来源记录；216去重官方registry元数据均一致。Arborist全图582边/220可达节点/0孤儿/0非法边；删除react的负面对照被检测。npm ls全量及npm ci dry-run通过，npm audit全量与omit-dev均0。

本方B4对同一源SHA真实执行官方npm10.9.8隔离npm ci --ignore-scripts成功并保持锁SHA；三实际安装版本正确。B5明确沿用该相同源安装证据，未把dry-run称为真实安装。两席修补包实际库级验证覆盖nanoid负值/零、PostCSS外部绝对source map、Undici两种private边界，正常路径亦通过。Grok B5只核3个修补tarball字节、其余元数据，未声称全部tarball重下载。主node_modules未重装。

npm audit只代表当时数据库；根engines宽于部分上游包要求记为跨模块观察，未篡改锁的上游事实。本模块结论不代表全业务测试、数据库、设备或生产部署通过。

审计源HEAD=9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b。本文件写入时尚未提交发布；随后按既有授权复制已审字节到发布分支，经propose-change.sh真实predeploy、推PR、精确提交CI绿、merge-pr.sh合并及ff同步160闭环后，方进入M005。实际发布结果以PR/CI与ledger回执为准。
