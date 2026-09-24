# M021 商户档案、商户类型与商户侧门户／数据库迁移／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。5 迁移（103 行）、18 判据；B1 双 FAIL（Grok GR-M021-B1-01/02、席甲 CX-M021-001）→ B2-r1 双 PASS 定稿 M021-001（冲突/是）、M021-002（技术性/否，只登记）→ B3-r1（席甲，TDD：新增前向迁移 V428__m021_settlement_format_share_media.sql V0.01 + SettlementFormatShareS11Tests 加 media 一例，跨单元变更）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 bc15b34b3abd7ddf22a28151954c5faab290797ba82b7240d7cae33185b51bc6；席甲 V0.01 SHA256 426dfdfdfb62b78d00c8aa7320fd7e740ffe886c56e695bc98a5460ec9a6e292；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 V384 把商户业态放开到六值（加 media）时漏同步 settlement_bill_format_shares.chk_sbfs_format（V366 五值），media 业态商户权益进通票月结会违反 CHECK、整单生成失败；V428 把该 CHECK 放开到同一六值闭集，真实库回归证明 media 份额写入并归 be_media、不进酒吧池。已应用的 V147/V199/V200/V348/V384/V366 零改写。

登记：002 V200 L10 注释「平台管理员默认可监督商户自助层」过时（授予本身是业务地图 L587-588「平台管理员拥有全部日常功能权限」的明文要求，注释不可改写；M020-007 的 V200 部分就此闭合）。

跨模块观察（cross_module_observations）：V147 注释行号漂移；V348/V366 历史注释五值；出厂无商户角色（补角色=添加，不问不建）；`LeasingService.java:285` 注释「同五值词表」；枚举速查「其他须指定主管公司」规则不在本模块；环境事实——M009 遗留暂存 V427 未入 origin/main，`validate-on-migrate: true`、未开 out-of-order、生产库 complex flyway=385，V427 须先于或随同 V428 并入后再部署；主工作区门禁产物既有失步（spec 写最新迁移 V423），与本模块无关。
