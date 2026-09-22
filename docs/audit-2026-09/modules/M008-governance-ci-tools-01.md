# M008 治理门禁与 CI 基线／检查及生成工具／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的17文件，当前1605行；判据GEN-001–018、PGM-036。B3整批修复累计2次。M008-001–004、006–007已修复，005/008–016仅登记；当前16项修复队列全否。

B5-r2按Grok真实退出→Codex独立全量审计并落盘→读取Grok顺序完成，两席全模块三线PASS：
- [Codex完整报告](M008-b5-r2-self-raw.md)：V0.01，30565bytes，SHA256 1f7c74f79eddf7eadaee556c982e243ec27b7cd083f51bd3bd644c5bfcc75951。
- [Grok完整报告](M008-b5-r2-grok-raw.md)：V0.01，35604bytes，SHA256 65f7ef12326060db9a1dbc67a3407568344cf87f753ac8aa8accc7ed74d428bf。
- B5后actual/sidecar/ledger/精确本次Grok raw四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写ledger V1.99为PASS，再创建本文件；LOCK-AMEND-COUNT仍1。

双方fresh77回归PASS、6份语法检查0。真实入口126/160在隔离副本正确回填；两个npm入口非法文本被拒绝；删除台账与非法ref被拒绝；三HTML全量扫描无新增未标层级数值。GR-M008-B5-R2-OBS-01并011，测试裸assert失败提前退出但exit1真实，非新缺陷。

实际anchors六处资料失配、ledger未同步、entry/README快照漂移仍报红，属已登记008/009和后续资料模块；本收口仅证明本模块工具/测试/历史证据审计通过，不宣称全链或部署通过。正本三HTML/README/entry/基线未修改，未提交/合并/推送/部署。


# M008现锁重新收口
版本：V0.02
结论：PASS。现锁b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，COUNT1。17文件1605行/19判据，B1/B2/B5完整双席；B3累计2，当前空队列跳新增B3/B4。findingsV3.46当前16项全技术性/无缺陷/队列否；历史原票保留，005重复常量不删。
证据：M008-b1-self/grok-raw本次V0.05 B1及V0.06 B2；M008-b5-r2-self/grok-raw本次V0.02。selfSHA54c458d7dcdb0618d7f4a91fc58b4a7f99a38e677d025c2f02d392bae8227da2；GrokSHAa7ebf8dcd1bb95dcfbff2b2295276dbd2f6df89b6b186349ed7b5188329d8df7，真实exit0，四值全等后先ledgerV7.87 PASS再本文件。
验证：本轮77项回归通过；delivery/anchors/param现绿；三HTML隔离全扫描0；真实两快照句隔离回填后检查0。主仓ledger同步红归M426、快照漂移红归M435/M436；runtime指纹/56天红仍M409/M415输入。均未刷绿，非全链/部署通过。
本轮源码/三HTML无新编辑，HEAD e456ba96…、index d9a65108…、hook46aabdf…保持；外审本模块未写主hook。下一个M009按20FILES/88判据完整B1重开，再走已批准修复，不直接带旧PASS。
