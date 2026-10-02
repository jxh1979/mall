-- M001-F20（全量审计 M001 第 4 轮；操作员 2026-09-25 裁决「合并为同一种」）：权限模板只有两种——基础模板（角色级，base）与临时模板（temporary）。
-- 存量取值按下述判定并入，之后后端只接收 base / temporary（AGENTS.md 第 8 条：不为存量数据做兼容；本迁移是裁决明定的种类合并）。
-- ① role：V178 / V179 / V180 种子模板（tpl_outsourced_facility、tpl_outsourced_security_cleaning、tpl_outsourced_construction、
--    tpl_auditor_internal、tpl_auditor_external）的类型；此前前端新建与编辑下拉也提供「角色模板」、后端不校验类型，运行期同样可能写出 role 行
--    （可能带有效期或单次）。裁决：基础模板就是角色级模板，role 行一律并入 base，不看有效期与单次。
-- ② copy：迁移与种子里没有这一取值，只可能是此前前端复制按钮运行时写入的复制件。治理地图第 656 行「临时授权可设有效期或单次使用」，
--    临时模板又必须有到期时间或单次使用（AccessService.requireBoundedTemporaryTemplate）：有到期时间（valid_to）或单次使用的并入 temporary，
--    其余（含只有生效时间的）并入 base。
-- ③ 其他不在闭集内的取值（如大小写变体）不处理；这类模板此后修改或按原类型复制会 400。
UPDATE permission_templates SET template_type = 'base' WHERE template_type = 'role';

UPDATE permission_templates
   SET template_type = CASE WHEN valid_to IS NOT NULL OR single_use THEN 'temporary' ELSE 'base' END
 WHERE template_type = 'copy';
