# M033 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第3段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。13 个请求记录类（access/dto/）、52 判据；B1 Grok FAIL（GR-01…03）／席甲 PASS 经核源撤回 → B2-r1 双 PASS 定稿 M033-001、002、003（均细化、工程缺陷「是」、修复队列「否」）→ 队列为空，跳过 B3/B4 → B5-r1 双 PASS：Grok V0.01 SHA256 2a65e7c6403f8d7fc9132f72928668dad50077da575167a2ffb3058fc3d0eddf；席甲 V0.01 SHA256 913bdce03aa0de03a5b3476c1597a2582d0cca79bdcc97c83e085485531b5028；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 0。本模块无代码改动。

定稿三条（有效修复不在本模块 13 个文件内）：
- 001：`AccessUserRequest.phone` 契约称「仅保存不可逆 phone_hash」，实现按已闭环 M006-025 的处置拒收（`@JsonIgnore`，回归测试靠该组件钉住）——说谎的是契约。
- 002：套用模板 `mode` 契约枚举 replace/merge/preview，后端只认 `single_use`、管理端发 `single_use`/`append`——按契约传 preview 会真的套用。
- 003：5 个 DTO 的 `evidenceAttachmentIds` 后端不读；这是全项目写请求信封（135 个 DTO／96 个 schema），删这 5 处不改变任何可观察行为且要动已闭环 M031。治理地图 L656、L899 点名授权类动作须留痕；若要让授权申请等动作存证据附件，属新增接线（M031），须操作员定。

跨模块观察（cross_module_observations）：
- XM-M033-A（→M392）：契约 `AccessUserRequest.phone` 的描述改为本接口不接收手机号，或去掉该字段。
- XM-M033-B（→M392）：契约套用模板 `mode` 枚举改为实际取值 `single_use`/`append`。
- XM-M033-C（→M392，兼 →M031）：契约缺 `final_approver_id`；`UserPermissionOverrideRequest` 无 schema（同 XM-M031-C）；对不落证据的端点仍宣称 `evidence_attachment_ids`。
- XM-M033-D（→M392，B5 新登记）：契约 `AccessUserRequest.account_type` 枚举缺 `outsourced_vendor`，而建号接口实际只放行该类型（其余一律 STAFF_REGISTRATION_REQUIRED）。
- 其余登记：`PermissionTemplateApplyRequest.templateId`、`PermissionTemplateRequest.sourceTemplateId` 后端不读；两个向后兼容构造器只剩测试在用；记录类 toString 含口令但无日志点；DTO 无 Bean Validation、控制器 `@Valid` 空转。
