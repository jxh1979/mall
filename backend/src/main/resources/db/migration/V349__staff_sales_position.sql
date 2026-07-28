-- V349 卡座销售提成改按员工岗位判定(G 批;业务 HTML 真源 2026-07-28 定稿)。
--
-- 【业务口径(真源逐字)】「按员工岗位判定:设有「销售岗」标识的员工按销售人员费率计提,其余按非销售人员费率计提;
--   费率后台可配(出厂默认销售 15%、非销售 10%)。员工是否为销售岗在员工档案里维护,不按单次是否指派销售人员临时判断。」
--
-- 【改前方向是反的】BarBillingService.calculateCommission 原按「账单上挂没挂 sales_staff_id」判档:
--   挂了人 → sales(15%)、没挂人 → non_sales(10%)。而 bar PRD §5b 写的是「非销售人员售卖【或被归属为】卡座销售按 10%」——
--   「被归属」的人该按其【岗位】判,原实现把「有归属」直接当成「是销售岗」,方向恰好相反:
--   非销售岗员工只要被挂上销售归属就按 15% 计提,反而多发提成。本批改为按员工档案的销售岗标识判。
--
-- 【为什么是员工档案新列,不复用职位体系 user_position_assignments.position_code】
--   ① 真源写明「在员工档案里维护」,员工档案 = staff_profiles;
--   ② position_code 是自由文本、无词表、无后台维护入口,且该表 PK=user_id 并强制绑 staff_level_id + 角色模板——
--      把资金口径挂成 RBAC 角色装配的副作用,人事改一次销售岗要动角色装配,改错了对账时谁都看不出来;
--   ③ 解析键取 staff_profiles.user_id:提成落 finance 应付以 sales_staff_id 为收款人、薪酬各表(考勤/薪资/提成台账)
--      一律按 user_id 归集,bar_bills.sales_staff_id 与 access_users.id 同名值空间,反查员工档案天然走 user_id。
--   费率仍读 V340 的 bar_commission_rate_configs(项目公司 × 商户 × 角色档),本批不建第二套费率配置。
--
-- 【默认 FALSE】新员工默认【不是】销售岗,须由人事在「员工档案」页显式勾选。
--   资金口径不能就高不就低:默认销售岗 = 全员按 15% 计提、笔笔多发。

ALTER TABLE staff_profiles
    ADD COLUMN IF NOT EXISTS is_sales_position BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN staff_profiles.is_sales_position
    IS '销售岗标识:TRUE=卡座提成按销售人员费率计提,FALSE=按非销售人员费率(出厂默认)';

-- 开单落档时按 bar_bills.sales_staff_id 反查员工档案(user_id);既有索引只有 (project_id, merchant_id)。
CREATE INDEX IF NOT EXISTS idx_staff_profiles_user
    ON staff_profiles (user_id) WHERE user_id IS NOT NULL;
