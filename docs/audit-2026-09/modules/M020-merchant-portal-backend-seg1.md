# M020 商户档案、商户类型与商户侧门户／后台实现／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。4 文件（242 行）、22 判据；B1 双 FAIL（Grok GR-01…07、席甲 CX-M020-001…005）→ B2-r1 双 PASS 定稿 M020-001…007（001–006 细化入队、007 XM 否；operator_note 只登记；无添加项）→ B3-r1（席甲，TDD：MerchantPortalService/MerchantPortalController V0.01 + M022 MerchantPortalServiceTests V0.01 跨单元变更）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 19d1e510dc1e48c0888b8d23ed428fced145e8d117e4e9ed9d88b12d5f8c3b65；席甲 V0.01 SHA256 0821c84773b70f79772b3ab38c987196009e4524613b638cf53761ec4213840c；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 商户 scope fail-closed（删平台全量死分支/platform-all，类注释与真实现一致，测试改无商户账号被拒）；002 商户留言保留裁决人/裁决时间、合并既有理由；003 合并后长度 >500（V162 列宽）→ 400 中文；004 幂等经 IdempotencyService，控制器改 ApiRequests.requireIdempotencyKey；005 三入口失败落 OpenAPI 声明的 *_failed 审计；006 不可异议报错去英文状态。

跨模块观察（cross_module_observations）：M392 OpenAPI 三条描述（挂起为有争议/裁决流闭环、平台管理员全量、reason maxLength 2000、dispute 仅 200 响应、operator_note 留痕声称）；M021 V200 L10 注释；M023 商户专区对账页无异议入口且 e2e 仍找表单；M022 JDBC 测试类注释「平台管理员全量」过时；evidence 模块 AuditEventService 终端硬编码 web；合同域 ContractSettlementRecord 注释三态；finance 模块 FinanceService.listArApItems 以 currentMerchantScope().orElse(null)「平台口径全量」为前提；字典实态仅 role_platform_admin 持三码（商户角色运行时授出）。
