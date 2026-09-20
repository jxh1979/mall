# M022 商户档案、商户类型与商户侧门户／后台验证／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。2 个测试文件、22 判据；B1 双 FAIL（Grok GR-01…06、席甲 CX-M022-001/002）→ B2-r1 双 PASS 定稿 M022-001（冲突）…007 → B3-r1（席甲，只改测试；红用变异给出：6 条把被测实现临时改坏、新断言全部变红、还原后全绿）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 39550083e7991a91bc5a234985205f3a8a871aff38a463bfea27ca7330438fb5；席甲 V0.01 SHA256 be6c5006611cdff31f717981a312ebb04e2bd996491a26518e7c5e4ce871bfd8；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 删除守护无生产调用方路径、且把「商户专区平台管理员全量」当现行事实的 JDBC 用例并改写类注释；002 补「我的合同只见本商户」真实库隔离用例；003 合并后超列宽；004 异议入口无 scope；005 结算列表成功审计与 total；006 异议权限码；007 空理由出网文案为中文（锁 FieldLabels.translate，不改 M020 服务文件）。生产代码零改动。

跨模块观察（cross_module_observations）：合同域 `ContractRepository.listAllSettlementsPaged`（含 Jdbc 覆写）现无生产调用方；M020 `requireMerchantScope` 的 orElseThrow 为防御性冗余；本包无控制器测试；DISPUTABLE 的 deposit_offsetting/arrears_collecting 两态无成功用例（HTML 未点名）。
