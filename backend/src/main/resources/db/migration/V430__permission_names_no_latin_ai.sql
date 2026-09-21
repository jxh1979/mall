-- V430：M035 审计（2026-09-21）001。
-- 治理地图：展示给用户的一切文字「必须全部为中文，不得出现任何英文」，可以不用中文的只有四样（单据号、设备编号、二维码内容、
-- 系统内部标识符），没有第五样。权限名由权限列表页、模板页直接上屏，仍有名称含拉丁字母「AI」；此前的展示名测试把「AI」当豁免放过。
-- 历史迁移不回改，向前修：把权限名里的「AI」写成同义的「人工智能」。permission_code 是机读标识，不变。

UPDATE permissions
   SET permission_name = replace(permission_name, 'AI', '人工智能')
 WHERE permission_name ~ 'AI';
