# M019 空间点位、楼层平面图与营业时段／文档与接口定义／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。4 文档（683 行）、58 判据；B1 双 FAIL（Grok GR-M019-001…009、席甲 CX-M019-001/002）→ B2-r1 双 PASS 定稿 M019-001…009（003/007 技术性，其余细化；无添加项）→ B3-r1（席甲，仅文档：p1/p2 契约与完成审计 V0.1→V0.2、开发计划加审计说明）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 df7fb83b95d08847d2dda366f217788c1cc707675b414000f321964428bfdc9c；席甲 V0.01 SHA256 0b9860f042c8bcaaa0ce56856203bd001c0a89e97e9d25967028fb12bb191fb0；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：P1 契约删幽灵字段（evidence_attachment_ids/operator_user_id）、z_index 选填、错误矩阵按现行实现（他图 shape_id 400、未知 id 新建、补 PROJECT_NOT_FOUND/CROSS_*）、引用改现行迁移与真实标题；P2 契约/完成审计/计划按 M015-016 分批发布、M015-001 派生 id、解析器现行规则（闭合 polyline、非八种整单拒绝、source_format 必填）、完成数字标快照并指向 verify:predeploy、补高危二次确认句（M015-011 同批）、八值 CHECK 指向 V423。

跨模块观察：M015-010/011 前端/契约字段待后端同批；OpenAPI 事件名点号、plan_name 长度、saveShape additionalProperties 属 OpenAPI 模块；PR #442 文档部分随本模块检查点入 origin/main。
