-- V387：M006 审计（2026-09-06）G-08：上屏展示名中文化（PGM-1.18「界面全中文（所有终端零英文）」）。
-- roles.role_name / permission_templates.template_name 由治理页、账号页、模板页直接显示；5 条权限名含 MDM / SKU 缩写
-- （「AI」为 HTML 正文自身用词，保留）。id / permission_code 是机读标识不变；历史迁移不回改，向前修一次到位。

UPDATE roles SET role_name = '平台管理员' WHERE id = 'role_platform_admin' AND role_name = 'platform_admin';
UPDATE roles SET role_name = '运营经理' WHERE id = 'role_ops_manager' AND role_name = 'OPS_MANAGER';
UPDATE roles SET role_name = '设施经理' WHERE id = 'role_facility_manager' AND role_name = 'FACILITY_MANAGER';
UPDATE roles SET role_name = '内部审计' WHERE id = 'role_auditor_internal' AND role_name = 'AUDITOR_INTERNAL';
UPDATE roles SET role_name = '外部审计' WHERE id = 'role_auditor_external' AND role_name = 'AUDITOR_EXTERNAL';
UPDATE roles SET role_name = '外包设施' WHERE id = 'role_outsourced_facility' AND role_name = 'OUTSOURCED_FACILITY';
UPDATE roles SET role_name = '外包安保保洁' WHERE id = 'role_outsourced_security_cleaning' AND role_name = 'OUTSOURCED_SECURITY_CLEANING';
UPDATE roles SET role_name = '外包施工' WHERE id = 'role_outsourced_construction' AND role_name = 'OUTSOURCED_CONSTRUCTION';
UPDATE roles SET role_name = '外包机构管理员' WHERE id = 'role_outsourced_vendor_admin' AND role_name = 'OUTSOURCED_VENDOR_ADMIN';
UPDATE roles SET role_name = '员工自助' WHERE id = 'role_staff_self' AND role_name = 'STAFF_SELF';

UPDATE permission_templates SET template_name = '外包设施模板' WHERE id = 'tpl_outsourced_facility' AND template_name = 'OUTSOURCED_FACILITY';
UPDATE permission_templates SET template_name = '外包安保保洁模板' WHERE id = 'tpl_outsourced_security_cleaning' AND template_name = 'OUTSOURCED_SECURITY_CLEANING';
UPDATE permission_templates SET template_name = '外包施工模板' WHERE id = 'tpl_outsourced_construction' AND template_name = 'OUTSOURCED_CONSTRUCTION';
UPDATE permission_templates SET template_name = '内部审计模板' WHERE id = 'tpl_auditor_internal' AND template_name = 'AUDITOR_INTERNAL';
UPDATE permission_templates SET template_name = '外部审计模板' WHERE id = 'tpl_auditor_external' AND template_name = 'AUDITOR_EXTERNAL';

UPDATE permissions SET permission_name = '维护酒吧套餐目录（组合商品、售价与内部结算价，属敏感成本数据）'
 WHERE permission_code = 'bar.combo.manage';
UPDATE permissions SET permission_name = '注册/同步受管控的移动设备'
 WHERE permission_code = 'mdm.device.manage';
UPDATE permissions SET permission_name = '管理受管控移动设备的应用/站点白名单'
 WHERE permission_code = 'mdm.policy.manage';
UPDATE permissions SET permission_name = '处理/处置受管控移动设备的策略违规'
 WHERE permission_code = 'mdm.violation.handle';
UPDATE permissions SET permission_name = '查看受管控移动设备的策略违规'
 WHERE permission_code = 'mdm.violation.view';
