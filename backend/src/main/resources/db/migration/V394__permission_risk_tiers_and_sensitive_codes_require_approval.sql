-- V394：M006 失效重审第 3 周期（2026-09-07）I8-01 / F8-01 / I8-02。
-- I8-01（BSM-1.3.4「拥有系统里全部日常功能权限…定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况」）：
--   V126/V127/V144 等把 11 条只读/日常作业码种成 risk_level='normal'，既不在平台管理员日常桥（low/medium），
--   也不在授权政策的四个审批档位（low/medium/high/critical）；按种子意图归入 low。
--   并以约束禁止再写入四档之外的值：BSM-1.3.4「还没定类的一律先当高危功能对待」由运行时兜底，种子阶段直接拒绝，不再产生"半定类"的码。
-- F8-01（同条）：access.grant_request.apply（申请高危授权）被种成 high，平台管理员日常桥剔除且无其他角色持有，
--   生产字典下无人能发起授权申请；申请本身不是授出（审批才是控制点），归入 low；
--   PGM-1.10.1「员工可申请临时权限，按层级对应主管审批」：员工自助角色（注册审批通过即分配，V322）补持申请权限。
-- I8-02（PGM-1.10.1「以下五类权限授出前须先审批才生效——资金/核销/退款/调账/分红发放类…敏感经营数据的查看/导出/修改…」；
--   BSM-1.3.4「敏感数据查看与导出（…企业微信会话原文、客人与员工个人信息）」）：落在五类内、仍为 requires_approval=FALSE 的高危码补 TRUE。
--   其余 11 条 high/无审批码（删除、最终业务审批、预警处置、调拨、供货主体、指标异常、邀请、客户账户生命周期）不在五类命名范围内，登记不改。
UPDATE permissions SET risk_level = 'low' WHERE risk_level = 'normal';
UPDATE permissions SET risk_level = 'low' WHERE permission_code = 'access.grant_request.apply';
INSERT INTO role_permissions (role_id, permission_code) VALUES ('role_staff_self', 'access.grant_request.apply') ON CONFLICT DO NOTHING;
ALTER TABLE permissions ADD CONSTRAINT chk_permissions_risk_level CHECK (risk_level IN ('low', 'medium', 'high', 'critical'));
UPDATE permissions SET requires_approval = TRUE
 WHERE permission_code IN ('analytics.group.view', 'customer.voucher.manage', 'finance.merchant_expense.manage',
                           'pii.contact_phone.view', 'wecom.sync');
