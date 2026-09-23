# M038 功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。16 个前端文件（访问治理工作台、员工账号、权限模板三个页面及其测试，Can／PermissionGate 两个权限闸组件及测试）、106 判据；B1 双 FAIL（Grok GR-M038-001…011／席甲 CX-M038-001…005）→ B2-r1 双 PASS 定稿 M038-001…010 → B3-r1（RED 14 红 → GREEN 57 绿）→ B4-r1 双 PASS → B5-r1 Grok 否决（GR-M038-012）→ 旧锁 B2-r2 双 PASS → LOCK-AMEND（锁 V0.11→V0.14，定义变更的 BSM-114/BSM-120/PGM-043/PGM-059 在本模块映射内）后按 §5 闸 2 ⑤ 改回 PENDING、现锁 B1 双 FAIL → B2-r3 双 PASS 定稿 M038-011、M038-012 → B3-r2（RED 3 红 → GREEN 60 绿）→ B4-r2 双 PASS → B5-r2 双 PASS：Grok V0.01 SHA256 8cfe6ee8f1c89450ccf06626057698c9e764bc89641c353ac9707a79b6c38c7d；席甲 V0.01 SHA256 7fbe9015b893e959bd623a3c3a51a5f73dcfa7d2f9c07a814e9e937090cfd40f；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458（现锁 V0.14）。B3 累计 2。

修复摘要：001 授权申请状态（已生效）与三种内部类型、账号开通方式、模板类型 role、默认审批意见此前以英文上屏或留痕，现页内按后端闭集给全中文。002 字段名、占位、aria-label、校验句去掉 id／ID。003 未映射的岗位码显示为「未登记岗位（岗位码）」。004 恢复按钮由「恢复为停用模板」改为「取消存档」／「恢复」（业务地图原句；M031-001 后恢复即启用）。005 治理页写动作统一经 runWrite：失败报后端中文消息、提交中置忙防双击。006 权限模板编辑的时间按本地时区回填，原样保存不再把生效/截止时间前移 8 小时。007 智能总结表表头去掉重复列。008 四处职级改为 30 档下拉。009 状态下拉与表格同一套用词。010 补权限闸组件渲染测试与发起授权申请的引导提交测试。011 模板页复制/应用共用提交中态，连点不发第二笔。012 新建外部协作账号时账号已落库、套用权限模板被拒：账号照常入表并提示「账号已创建；套用权限模板失败：<后端消息>」，不再报整笔失败。未改公共字典、客户端与后端。

跨模块观察（cross_module_observations）：
- XM-M038-A（→各自模块）：origin/main 全量前端单测 3 条失败——TraceLinkDrilldown.test.tsx 1 条、TraceLinkP2c.test.tsx 2 条，与本模块无关。
- 公共字典 customer/i18n/statusLabels.ts 缺 applied、role_permission、user_scope_change、position_high_risk_release、manual_admin、legacy 等键（本模块以页内映射挡住上屏；字典属别的模块）。
- 其余登记：岗位编码、所属组织、目标员工、权限模板等处手填内部编号；`hasPermissions` 的 wildcard 分支在后端恒给 false 后不会被走到；员工账号页标题说明提到「企业微信同步结果」「禁用」而页内无此内容；速查表「权限状态」五态后端无改状态接口；发起授权申请的引导提交在 M080 的 AccessGovernanceGrantRequest.guidedcreate.test.tsx 里另有一份测试；审批按钮所用的公共 RowActions 无禁用槽，提交中不能视觉置忙（runWrite 的 ref 仍挡第二笔）。
