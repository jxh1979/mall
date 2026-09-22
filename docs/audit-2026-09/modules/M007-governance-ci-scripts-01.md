# M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段
版本：V0.01

模块状态：PASS；本计划471模块；B3执行1次。冻结5文件，当前163行；直接修复回归102行。
最终依据：[Codex B5-r1](M007-b5-r1-self-raw.md)、[Grok B5-r1](M007-b5-r1-grok-raw.md)，两席均PASS，无新finding。B5后的实际锁/旁路/ledger/精确Grok锁行四值均为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；LOCK-AMEND-COUNT仍1。先通过校验并写ledger PASS，再建立本收口。
001–004源码问题已修：hook按目标分支判断、安装展开Git路径、提议前拒脏工作树、CI查询错误立即退出且保留原因。005–011仅登记，当前修复队列为空；原记录和两席差异均保留。
验证：当前7回归通过；Codex同测试对before复现4旧缺陷失败；29补充场景通过；四bash语法、CI镜像/15子入口、相关文档门禁通过。79冻结判据完整覆盖。
源码与B3 after树9f63094e21e4ed94a29b29071af5bd07cf99fd6a对应blob相等。三HTML未改。主仓实际.git/hooks/pre-push仍旧安装副本；未运行重新安装、推送、PR创建/合并或部署，不把源码收口当作运行时发布。未宣称全仓verify:predeploy通过；其它模块问题留待其所属模块。


## 现锁完整复审结项
版本：V0.02
状态：PASS（仅冻结源码范围）。当前锁V0.14 SHA b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；COUNT1。上文V0.01旧锁历史保留。
五源163行、79判据，必要直接回归102行完整双审。B1self/GrokV0.03，B2两席V0.04双PASS、findingsV3.44全部11项队列否；001—004历史修复有效、005—011只登记，跳新增B3/B4，历史B3累计1。
B5-r1 selfV0.02 168756bytes SHAf3299584217e0983e4aaefe45acea8d2004d33330ae79fb98651de03aeb6687d；GrokV0.02 65983bytes SHA28220e863cfaa1d3e0929d44cd7bfa84fcce3b19161fa15ee08caa0f3c03348b。先独立封存self，后完整读取外审，真实exit0和四值一致，先ledgerV7.82再本收口。现源未改，回归7项/merge3模式及docs检查通过。
重要事故：Grok自制file-remote夹具误将相对.git/hooks解析到主仓，曾短暂覆盖主仓hook，违反本轮写入边界。已恢复，根独立核验旧内容SHA46aabdf…/997bytes、mode775、历史blob完全相等，indexSHA d9a65108…和HEAD不变。原raw不可改；不得把外审概括成全程只读。仅源码状态通过，不能声称运行副本已更新。后续夹具写入须绝对目录前置断言，优先现有已核回归。
OBS1主仓运行hook旧副本继续保留；OBS2工作区分叉；OBS3全局PGM台账红/入口快照漂移/runtime过期按所属模块输入，未刷绿。不代表全仓或上线通过。无真实GitHub写/发布。继续M008。
