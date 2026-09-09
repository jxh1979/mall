# M008 治理门禁与 CI 基线／检查及生成工具／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的17文件，当前1605行；判据GEN-001–018、PGM-036。B3整批修复累计2次。M008-001–004、006–007已修复，005/008–016仅登记；当前16项修复队列全否。

B5-r2按Grok真实退出→Codex独立全量审计并落盘→读取Grok顺序完成，两席全模块三线PASS：
- [Codex完整报告](M008-b5-r2-self-raw.md)：V0.01，30565bytes，SHA256 1f7c74f79eddf7eadaee556c982e243ec27b7cd083f51bd3bd644c5bfcc75951。
- [Grok完整报告](M008-b5-r2-grok-raw.md)：V0.01，35604bytes，SHA256 65f7ef12326060db9a1dbc67a3407568344cf87f753ac8aa8accc7ed74d428bf。
- B5后actual/sidecar/ledger/精确本次Grok raw四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写ledger V1.99为PASS，再创建本文件；LOCK-AMEND-COUNT仍1。

双方fresh77回归PASS、6份语法检查0。真实入口126/160在隔离副本正确回填；两个npm入口非法文本被拒绝；删除台账与非法ref被拒绝；三HTML全量扫描无新增未标层级数值。GR-M008-B5-R2-OBS-01并011，测试裸assert失败提前退出但exit1真实，非新缺陷。

实际anchors六处资料失配、ledger未同步、entry/README快照漂移仍报红，属已登记008/009和后续资料模块；本收口仅证明本模块工具/测试/历史证据审计通过，不宣称全链或部署通过。正本三HTML/README/entry/基线未修改，未提交/合并/推送/部署。
