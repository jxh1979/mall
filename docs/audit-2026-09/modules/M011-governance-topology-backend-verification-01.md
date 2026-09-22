# M011 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
版本：V0.03

2026-09-14 Codex补充复查PASS；Grok NOT-CALLED。本次按操作员“无问题自己审，有问题恢复原完整流程”执行，不是新双PASS；历史Claude Fable 5.1/Grok双PASS保留。B3累计1，本次修复0。

当前三测试文件2632行全文和87判据三线核对完成，未发现新缺陷。重点核验软删fake保留行、真实SQL与锁、未来绑定不提前改三主体、项目行/总数隔离、密钥身份与自审限制、归属补录不改其它列、手机号密文/脱敏。Controller14、JDBC23、Service69共106条在本会话M009已实跑通过；当前三文件逐SHA与该执行树一致，读取当前XML全部零失败/错误/跳过。未重复跑、未把纯mock通过当作真实库证明。

[本次完整报告](M011-b5-r1-self-raw.md) V0.02：43801bytes，SHA256 b168aed0e1c7c2f69c41fa4ee04ea0448050a9ba7b86e8b0be35c027f16bd4ab；旧13184byte前缀完整保留。旧Grok V0.01 SHA256 9fe74824c4566d8e7c7b6d9cb9275eb41a960abb951711a3c67eafe9b7285a20保持。历史7项全否（5已修2登记）不改；没有新问题/人闸。

锁实算/sidecar/ledger=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，LOCK-AMEND-COUNT1；HEAD e456ba96c84185c8d39f18f548011daac48a6466。三HTML/源码/测试/锁未改、未发布；先核验并写ledger V3.75保留PASS，后写本报告。

---
以下历史V0.01原文原样保留，旧席位/计数代表当时：

# M011 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 3 文件（PlatformControllerTests / PlatformJdbcIntegrationTests / PlatformServiceTests；计划冻结 1342 行，当前含 M009/M010 修复期与本模块 B3 新增断言）；判据 87 项（与 M009 相同集合）。B3 整批修复累计 1 次。M011-001–005 已修复；006（M009 跨模块观察：PlatformController.createSpaceType 无 requireIdempotencyKey，供全局收口）、007 仅登记；当前 7 项修复队列全否。

B5-r1 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M011-b5-r1-self-raw.md)：V0.01，13184 bytes，SHA256 a5f39791ec90b6cb1dcf5b1f432e089e44c510daf0a07b557b35c58a300dc58b。
- [Grok完整报告](M011-b5-r1-grok-raw.md)：V0.01，43154 bytes，SHA256 9fe74824c4566d8e7c7b6d9cb9275eb41a960abb951711a3c67eafe9b7285a20。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.34 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（只改本模块 3 个测试文件，被测 M009 源码未改）：内存 fake `deleteMerchant` 由物理 remove 改为软删标记，审批删除用例断言改为「默认列表不含、已存档列表含且 delete_status=approved」，与 JDBC 及 HTML BSM-004「不是真从数据库里抹掉…可查、可审计、可恢复」同口径（001，冲突）；JDBC/Controller 夹具的退役码 fixed_rent_plus_commission / revenue_share / draft 改为服务层白名单值 lease / joint_operation / active（002）；补取消存档、驳回删除、存档/删除理由落仓储的服务用例与一条存档 HTTP 用例（003）；补商户账号打治理写路径的服务层作用域负例与其余五个治理读端的控制器作用域负例（004）；FiveValues 改 SixValues、两条绑定用例补缺幂等键→400 断言（005）。

正本三 HTML/锁/权限种子/迁移/被测生产代码未修改，未提交/合并/推送/部署。


## 2026-09-22 当前锁完整双席重审
当前结论PASS（先登记ledger）。三份测试当前2776行、87冻结判据已由Codex/Grok各自完整B1及B5；B2 7既有ID和12观察完整归并、当前修复队列全否。B3累计1保留，本次修复0，跳过新B3/B4。
- 锁SHA：b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；LOCK-AMEND-COUNT=1，B1/B5后四值相等。
- B5席甲：[V0.03完整报告](M011-b5-r1-self-raw.md)，SHA b4127b2337304691bd377a3f95dce69f33173bd5bcf3cdbec11c030aa4cb3f3d。
- B5席乙：[V0.02完整报告](M011-b5-r1-grok-raw.md)，SHA 6a95b55c1fe6b9846214dd83aa61339e713db9371ede116eeb03926128958b44，真实exit0。
- 本轮新跑mvn三测试113通过（Controller19/Jdbc24/Service70，失败/错误/跳过均0）；B5两席只读核XML及相同源码，不重复计数。
- B1/B2 raw均追加现锁V0.04/V0.05，历史原文保留；003/005以旧B2-r2收窄为准，空archiveReason fake不作漏修，不追加按verb重复测试。旧文“存档/删除理由落仓储”不代表fake存archiveReason；当前证明deleteReason。
- 12观察保留，无添加项、无待答问题、无本轮HTML修改。当前HTML允许获有效权限/真实密钥最高权限在获准商户档案查看两联系人号码，测试遵循例外。
- 此结论仅本模块审计，不表示全仓发布门禁通过或已提交合并推送。操作员已授权发布，需完成已批准HTML的决策台账同步及发布内容/依赖检查。下一模块M012。
