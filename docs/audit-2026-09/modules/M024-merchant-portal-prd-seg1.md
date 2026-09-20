# M024 商户档案、商户类型与商户侧门户／文档与接口定义／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。1 个文档 docs/merchant-portal-zone-prd-v0.1.md、19 判据；B1 双 FAIL（Grok GR-01…05、席甲 CX-M024-001…003）→ B2-r1 双 PASS 定稿 M024-001…005（均细化）→ B3-r1（席甲：修前检查 10 处问题 → 修后 0 处 + 条目锚点门禁 PASS）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 fa725d0c4e8b50a7920b5d41c871de77404760ce12c1f3dc8b726668186b1392；席甲 V0.01 SHA256 4f02568358aa3de62df53273377892f21798abe85a8dc68ce008a95d89cbf587；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要（文档 V0.13→V0.14）：001 §8 商户门户两个列表接口改写为现行事实（无商户范围一律 403、按商户取全量后切片、total 为本商户真实总数，删「平台全量取真实 total」）；002 两处 HTML 行号锚点改为条目名（BSM「给商户」、BSM「商户管理·主要功能」、BSM「商户专区自助」）；003 §11 依据每个名字带代号且可唯一定位；004 §8 表两行「权限」列只留路由门控码、页内权限写入「当前能力」列；005 页面暗纹与敏感页防护标为当前未实现并列入 §9。

跨模块观察（cross_module_observations）：条目锚点门禁 `tools/verify-entry-anchors.mjs` 两处盲区（检查② 只检紧邻代号的第一个名字；检查③ 行号只认「实测 LNNN」句式）；`docs/full-admin-ui-acceptance-matrix-v0.1.md` UI-ADM-MER-007 备注「明文显示手机号」与现行掩码不一致；专区工单页 `resolved` 显示「已解决」与枚举「已处理待关闭」未对齐；合同/结算前端超 500 条丢行；专区工单页不展示 `sla_breached`；V166 文件头注释称 V022 为死表（注释不是依据）；`MerchantBillingService.listBills` 不可达的 findAllBills 分支。
