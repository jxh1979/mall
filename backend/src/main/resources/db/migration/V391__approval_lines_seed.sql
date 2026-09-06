-- M087-A-03 / F-M087-03（PGM-1.10.1「税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」；
-- PGM-1.7.1「需要平行线审批的（如法务、财务），另行指定该条线的最终审批人……每个部门或业务线须预先设定其平行审批的最低起点层级」）：
-- 三条平行审批线的出厂设置。起点人与最低起点层级由平台管理员在组织层级维护入口按实际组织填写（出厂最低起点 1，起点人为空 =
-- 该线只有起草人指定的最终审批人一个节点，仍须先审批）。

INSERT INTO approval_lines (line_code, line_name, min_start_rank, entry_user_id, status)
VALUES
    ('finance_review', '财务审核线', 1, NULL, 'active'),
    ('tax_review',     '税务审核线', 1, NULL, 'active'),
    ('legal_review',   '法务审核线', 1, NULL, 'active')
ON CONFLICT (line_code) DO NOTHING;
