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
