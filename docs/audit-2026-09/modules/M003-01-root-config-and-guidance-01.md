# M003 — 治理门禁与 CI 基线／配置与工程入口／第1段
版本：V0.03

状态：PASS。控制V0.17，471计划，锁V0.14。ledger V7.66已先登记本模块PASS。仅当前M001—M003完成新锁复审，不代表全仓通过。

## 当前范围与证据
.gitignore58行 + package.json147行=205行，18GEN全部三线复审。两源SHA256分别 a10b953aa5d53b1f476c250c8ccd4e0ff3751f122ceac7a240f8e4643d125836 / 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f。新增既有Graft缓存行单列032，不扩大016旧对象；027只登记重叠，无故障不强删。findings V3.36七ID016、027—032全为无缺陷/队列否，旧裁决与原票保留。无新增B3/B4，累计B3=2、方案修复2、失败重跑0。

B1 self V0.10 / Grok V0.07均PASS；B2 self V0.11 / Grok V0.08整批PASS。B5-r2 self V0.04完整116159bytes SHA256 19ea4291c38154ed77c7924030b63dd4b4790b4c30b50b5cf626ec5cce9bde0c；Grok V0.03完整103531bytes SHA256 d6fff34948a42a72e70e526917f7636ba06cbcefd2d2e527b9f41caf651e2f06。self先封存再读Grok完整新报告，真实exit0，均PASS。B5后实算/sidecar/ledger/Grok四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；LOCK-AMEND-COUNT=1。

## 验证与限制
127脚本语法、75Node入口语法、引用/工作区/锁镜像、ignore正负例及tracked-ignore=0均核验。后端环境check、Vite help、TypeScript dry只验证入口和环境，不是业务测试/编译已通过；root复用B1同字节运行证据明确见B5。Grok独立验证环境、入口失败传播、ignore及HTML门禁。
html-ledger-sync/predeploy仍因获批PGM680/871与M426台账未同批同步而红，工具M008。entry-anchors现绿。入口快照DRIFT归README M435、入口文档M436、工具M008；AGENTS Graft措辞观察归M435。不刷绿、不在本模块修上述材料，不重开已收口HTML。未跑全后端/全UI/Android/数据库/全交付链，未提交推送或部署。本轮没有修改源码或HTML。

## 历史 V0.02 原文（旧结论不代表现锁）

# M003 — 治理门禁与 CI 基线／配置与工程入口／第1段
版本：V0.02

状态：PASS。当前471模块计划；控制V0.17、冻结锁V0.11。已先写ledger V1.63的M003 PASS，再写本收口。完成3/471模块；不代表全仓或发布门禁通过。

## 当前范围与结果

仅.gitignore（55行）与package.json（147行），共202行；GEN-001—GEN-018三线完整复审。当前B2定稿ID为M003-016、027—031，全为无缺陷/修复队列否；B5两席无新增finding、添加项或HTML自身问题。
本轮没有源码修改，跳过B3/B4；历史B3累计2、方案修复2、交付失败0保持。旧322模块同号证据只作历史。

| 文件 | SHA256 |
|---|---|
| .gitignore | 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b |
| package.json | 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f |

## 两席证据

- 本次B1：Codex V0.04最初FAIL（冗余ignore），Grok V0.04 PASS。原始分歧保留；冗余不影响正常运行，不构成必须修复的工程缺陷，未删除规则。
- 首批B2 findings V0.90：根PASS/Grok FAIL；收窄027处置后V0.91双PASS，V0.92仅定稿。B1 raw保留各批完整归并意见。
- B5-r2 self V0.03（当前完整报告自L309起），82593字节，SHA256 327ab67716aa3a17351657cb4008328776a8a367c0a853e52b189428fb854586。
- B5-r2 Grok V0.02（当前报告L460—893），64554字节，SHA256 965163dbbb2e5d3969e78a038663fded87287eca59d30e21afb20bd303ab782a；Grok真实exit0。根完整self落盘后才读取全部434行。
- B5两席均PASS。技术性/细化措辞有部分差别，均无业务发明、缺陷或修复项；不宣称分类逐项一致，不覆盖已双PASS的B2定稿。

B5后四值分别：
实算：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
sidecar：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
ledger：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
当前Grok最后独占行：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
四值全等，LOCK-AMEND-COUNT=1。完整命令与输入、结果见M003-b1-self-raw.md收口回执和两份B5 raw。

## 验证与限制

根当前检查覆盖127条脚本语法、75个Node目标、工作区引用、依赖清单镜像、21个ignore样例与入口正负例；两个环境检查成功。UI仅列出32测试/6文件，真实后端配置仅列18测试/4文件；typecheck使用dry，仅验证构建图，不是类型检查或业务用例通过。
verify:predeploy实际exit1，第一处html-ledger-sync失败；entry-anchors独立实跑exit1，6处BSM引用镜像错误（4处A回访概括、2处已批准页脚日期），归M425/M427。本模块未修这些文件，不能称全局发布已绿。
没有全后端/数据库迁移/全UI/Android构建/部署验证，没有提交推送。三份需求HTML与锁未改，用户B/C原意确认继续有效。
远程操作已使用直接SSH；没有把审计委派给Claude Agent，未修改全局MCP配置。

## 历史 V0.01（旧322计划，完整原文保留）

以下旧范围、旧hash、旧通过/发布安排不代表当前471计划状态。

# M003 — 01-root-config-and-guidance-01
版本：V0.01

状态：PASS。首轮全仓审计M003/322；控制V0.17、锁V0.06。B5-r2两席完整报告及四值相等成立后，先写ledger PASS，再写本文件。

## 范围与结果

当前8文件621行，18GEN三线全量重审；26ID均无待修，新发现0，未答0。B3累计2，Grok运行失败重跑0。最初模块625行，修复后621行；范围8路径不变。

| 文件 | 当前行数 | SHA256 |
|---|---:|---|
| .env.example | 22 | e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049 |
| .gitignore | 55 | 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b |
| .semgrepignore | 6 | 45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba |
| AGENTS.md | 74 | ea470f256c8e9f1e23e9b346d1b3828f2e4e1edd84e9ea83d7564725759bfcf0 |
| CONTEXT.md | 163 | d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7 |
| PROGRESS.md | 36 | 91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d |
| README.en.md | 116 | dd83a2943d401ec25bfa2043ef4762662c78d1526763eb3faf2cc20008d092db |
| README.md | 149 | 28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5 |

六个文件有修订：移除死环境变量/空PII示例，纠正文档与三HTML冲突，修复路径、快照时态、JDBC事实及客人Web连线。两ignore、三HTML保持。023按实际答复删除L4词，024按实际答复保留营销词；019高危层级原发现经具体上位核实撤回。

## 可复核证据

- B1及两批B2：modules/M003-b1-self-raw.md V0.03 SHA d141759f52582e517019922de2e4a11865b34aa6a28ee322fd273870d4b48d01；modules/M003-b1-grok-raw.md V0.03 SHA b595810df2273dc8a57fd639e6a9de130f130966459d46848b656a568c076bea。两批B2各自双PASS，原B1和分歧完整保留。
- B3-r1：a339075ff27c89319739c4ca4e9fb802b31f376c → 18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7；全diff 19006字节/22hunk/6文件，SHA c5d7661649fbabd3b003394242e2a4069d36251e994a9b5bc5b43255a4c92a55。
- B3-r2：6318ad55b33dc901d19038d1d643bbb483c72f7a → fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110；全diff 4996字节/9hunk/3文件，SHA 7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e。
- B4-r2：modules/M003-b4-r2-self-raw.md SHA 8c7e35e117ddfb390d7dd5ea5a936e60cd6410b1bc7a38336a3bad62aa888a30；modules/M003-b4-r2-grok-raw.md SHA 23019239a89237433bee6bf94b94f6e9554be945fc80c9b40e86777f39180a75；双PASS。
- B5-r2：modules/M003-b5-r2-self-raw.md V0.02 SHA 4fdd804f891c5bc412064b98027cb27224c3e39ec50708485088ff3ac1d0059d；modules/M003-b5-r2-grok-raw.md V0.01 SHA 0fe035344280ae6e9450b1fe0f5bc7fd91a2861da156927ac407c4350c351818；双PASS，Grok真实exit0。self原独立49514字节先落盘，V0.02为外审收到后事实补核，原文不覆盖。

B5后实算、sidecar、ledger及Grok本次报告四值均为 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；COUNT0。B5-r1双FAIL及完整归并保留在原raw/findings，未把中间PASS当全模块通过。

## 验证与边界

实际成功：bash -n、entry-doc-baseline --check、verify:entry-anchors、verify:html-ledger-sync、git diff --check、文档相对链接/脚本键/UTF8校验。当前Spring+KeyVault源码真实编译执行证明旧空值拒绝、未设置变量时local可生成32字节AES键、prod拒绝默认值；此执行属于B4-r1，B5只引用同字节证据。收到B5外审后另真实运行Spring StandardEnvironment，以环境变量取到local，临时帮助目录已删除。

Grok verify:backend-env因宿主JAVA_HOME不存在失败，未算PASS；未改持久环境。Playwright --list仅列47测试9文件，不是用例通过。未宣称全后端、数据库迁移、全E2E、生产部署、设备联调已验证。结论只覆盖本模块当前根配置和文档。

审计源HEAD=df580198172b2c497ef3cb7e25d2e8e5dc058920。此文件写入时尚未提交发布；后续按既有授权将已审字节复制到独立发布分支，跑predeploy、推PR、等CI并经merge-pr.sh合并、ff同步160，实际结果以PR/CI与操作员回执为准。发布闭环后方进M004。
