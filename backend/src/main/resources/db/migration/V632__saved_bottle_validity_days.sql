-- 文件版本：V0.01
-- V632：第十八批 AP 段第 254 条 254-R2（存酒有效期按项目可配，登记时由服务端固化到期日）。
-- 依据：BSM:1072「……客户存酒有效期（业务参数，按项目可配，出厂默认 30 天，须同时配置上下限），登记时即固化本次适用的有效期与到期日，
-- 后续改配置不追溯已登记的存酒……」；决策台账 §26「客户存酒默认有效期为 30 天」。
-- 与转存参数同表（V513 saved_bottle_renewal_policy）：validity_days 按项目（project:<id>），缺行回落出厂行 global；上下限留空、首次配置须同时填写。
ALTER TABLE saved_bottle_renewal_policy DROP CONSTRAINT chk_saved_bottle_renewal_policy_key;
ALTER TABLE saved_bottle_renewal_policy ADD CONSTRAINT chk_saved_bottle_renewal_policy_key CHECK (
    (policy_key = 'max_renewals' AND numeric_value >= 0 AND (scope_id = 'global' OR scope_id LIKE 'project:%'))
    OR (policy_key = 'extension_days' AND numeric_value >= 1 AND (scope_id = 'global' OR scope_id LIKE 'bar:%'))
    OR (policy_key = 'validity_days' AND numeric_value >= 1 AND (scope_id = 'global' OR scope_id LIKE 'project:%')));
INSERT INTO saved_bottle_renewal_policy (policy_key, scope_id, numeric_value)
VALUES ('validity_days', 'global', 30)
ON CONFLICT (policy_key, scope_id) DO NOTHING;
UPDATE permissions SET permission_name = '配置存酒有效期、转存次数上限与顺延期限'
WHERE permission_code = 'bar.saved_bottle.renewal_policy.manage';
