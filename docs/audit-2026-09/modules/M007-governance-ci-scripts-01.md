# M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段
版本：V0.01

模块状态：PASS；本计划471模块；B3执行1次。冻结5文件，当前163行；直接修复回归102行。
最终依据：[Codex B5-r1](M007-b5-r1-self-raw.md)、[Grok B5-r1](M007-b5-r1-grok-raw.md)，两席均PASS，无新finding。B5后的实际锁/旁路/ledger/精确Grok锁行四值均为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；LOCK-AMEND-COUNT仍1。先通过校验并写ledger PASS，再建立本收口。
001–004源码问题已修：hook按目标分支判断、安装展开Git路径、提议前拒脏工作树、CI查询错误立即退出且保留原因。005–011仅登记，当前修复队列为空；原记录和两席差异均保留。
验证：当前7回归通过；Codex同测试对before复现4旧缺陷失败；29补充场景通过；四bash语法、CI镜像/15子入口、相关文档门禁通过。79冻结判据完整覆盖。
源码与B3 after树9f63094e21e4ed94a29b29071af5bd07cf99fd6a对应blob相等。三HTML未改。主仓实际.git/hooks/pre-push仍旧安装副本；未运行重新安装、推送、PR创建/合并或部署，不把源码收口当作运行时发布。未宣称全仓verify:predeploy通过；其它模块问题留待其所属模块。
