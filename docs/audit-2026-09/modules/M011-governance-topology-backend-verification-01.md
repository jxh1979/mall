# M011 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 3 文件（PlatformControllerTests / PlatformJdbcIntegrationTests / PlatformServiceTests；计划冻结 1342 行，当前含 M009/M010 修复期与本模块 B3 新增断言）；判据 87 项（与 M009 相同集合）。B3 整批修复累计 1 次。M011-001–005 已修复；006（M009 跨模块观察：PlatformController.createSpaceType 无 requireIdempotencyKey，供全局收口）、007 仅登记；当前 7 项修复队列全否。

B5-r1 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M011-b5-r1-self-raw.md)：V0.01，13184 bytes，SHA256 a5f39791ec90b6cb1dcf5b1f432e089e44c510daf0a07b557b35c58a300dc58b。
- [Grok完整报告](M011-b5-r1-grok-raw.md)：V0.01，43154 bytes，SHA256 9fe74824c4566d8e7c7b6d9cb9275eb41a960abb951711a3c67eafe9b7285a20。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.34 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（只改本模块 3 个测试文件，被测 M009 源码未改）：内存 fake `deleteMerchant` 由物理 remove 改为软删标记，审批删除用例断言改为「默认列表不含、已存档列表含且 delete_status=approved」，与 JDBC 及 HTML BSM-004「不是真从数据库里抹掉…可查、可审计、可恢复」同口径（001，冲突）；JDBC/Controller 夹具的退役码 fixed_rent_plus_commission / revenue_share / draft 改为服务层白名单值 lease / joint_operation / active（002）；补取消存档、驳回删除、存档/删除理由落仓储的服务用例与一条存档 HTTP 用例（003）；补商户账号打治理写路径的服务层作用域负例与其余五个治理读端的控制器作用域负例（004）；FiveValues 改 SixValues、两条绑定用例补缺幂等键→400 断言（005）。

正本三 HTML/锁/权限种子/迁移/被测生产代码未修改，未提交/合并/推送/部署。
