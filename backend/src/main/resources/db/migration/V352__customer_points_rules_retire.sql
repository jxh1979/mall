-- V352 停用旧积分规则表 customer_points_rules 与旧页 ADM-CUS-003「积分规则配置」(用户 2026-07-28 拍板:
--   "停用旧表和旧页,只留配置中心")。
--
-- 【为什么停】这一页是个陷阱:除"规则名称"外,改任何字段都不影响任何计算。逐字段核过——
--   ① earn_ratio(获取系数)      → 引擎真正取的是 points_accrual_rate_configs.amount_cent_per_point(V340,结算配置中心)
--   ② level_threshold(等级阈值)  → 引擎真正取的是 customer_member_levels(升/保级门槛 + 积分倍率)
--   ③ 签到积分                   → 引擎真正取的是 customer_check_in_tiers(连签天数档位)
--   ④ validity_type/expire_mode  → 生日/有效期口径走 customer_birthday_rules 与积分台账口径
--   全库 grep:customer_points_rules 除自身 CRUD(Controller/Service/Repository)与其测试外,零读取方。
--   即运营在这页改完保存成功、审计也写了,但一分钱积分都不会变——比没有这页更危险。
--
-- 【怎么停】按删除铁律(删除=存库 + 前台不显 + 可恢复 + 分类 + 填理由 + 最高权限批准),范式对齐
--   V255/V260/V271/V341,字段名/类型一字不改:
--   存库不物删:不 DROP TABLE、不 DELETE 行;既有行原样留库,随时可查可恢复。
--   前台不显  :同 PR 下线 ADM-CUS-003 路由/菜单/页面组件与四个 CRUD 端点,后台再也进不去。
--   可恢复    :恢复 = 清空本迁移写入的存档三列 + 还原前端路由,数据一行没动。
--   分类      :archived_at 非空 = 已存档(软下线),与"已删除墓碑"两轴分开,列表可分别筛出。
--   填理由    :archive_reason 写明停用依据(下方 UPDATE 里逐条写死,不留空)。
--   最高权限批准:停用本身由用户 2026-07-28 拍板;权限点 customer.points_rule.* 收回到只剩
--                role_platform_admin 也一并撤销(下方 ④),即无人再持有该页权限。
--
-- 幂等可重跑(IF NOT EXISTS / WHERE archived_at IS NULL)。向后兼容,可独立回滚。

-- ① 存档轴三列(照 V341 口径:archived_at 非空=已存档)。
ALTER TABLE customer_points_rules
    ADD COLUMN IF NOT EXISTS archived_at    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by    VARCHAR(64),
    ADD COLUMN IF NOT EXISTS archive_reason TEXT;

-- ② 全表存档 + 填理由(存量行一行不删,只打存档标记)。
UPDATE customer_points_rules
   SET archived_at    = now(),
       archived_by    = 'role_platform_admin',
       archive_reason = '2026-07-28 用户拍板停用:本表四个配置维度全被活表接管'
                     || '(获取系数→points_accrual_rate_configs / 等级阈值→customer_member_levels /'
                     || ' 签到→customer_check_in_tiers / 生日与有效期→customer_birthday_rules),'
                     || '本表零读取方,后台页 ADM-CUS-003 改任何字段都不影响任何计算。积分口径统一走结算配置中心。'
 WHERE archived_at IS NULL;

-- ③ 存档标记留库,读路径已随 ④/前端一并下线;此处不再建生效行偏索引(无生效行可读)。

-- ④ 撤销 ADM-CUS-003 的两个权限点:先解绑角色(任何人都拿不到),再把权限本体置 inactive
--    (权限行留库不物删,与存库口径一致;将来若恢复该页,改回 active + 重新授权即可)。
DELETE FROM role_permissions WHERE permission_code IN ('customer.points_rule.view', 'customer.points_rule.manage');

UPDATE permissions
   SET status = 'inactive',
       permission_name = permission_name || '（2026-07-28 停用:页面与端点已下线）'
 WHERE permission_code IN ('customer.points_rule.view', 'customer.points_rule.manage')
   AND status <> 'inactive';
