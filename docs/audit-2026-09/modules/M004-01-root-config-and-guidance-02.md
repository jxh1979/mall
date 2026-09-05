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
