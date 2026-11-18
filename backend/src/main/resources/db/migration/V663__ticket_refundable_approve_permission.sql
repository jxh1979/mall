-- 文件版本：V0.01
-- V663（第十八批 AW 段第 295 条 295-R1）：票种开启可退审批单列权限码。
-- 依据：BSM:785「「票种规则明确允许退款」这一项在后台出厂默认关闭；要让某一个票种可以退款，必须逐个票种单独开启，每次开启都须经高敏权限审批并留痕，
--   不得整批打开，也不得把原则与例外倒置」；决策台账 DEC-R3B-2026-08-04-31「逐票种开启须高敏权限审批并留痕」。
--   高敏＝high＋requires_approval（同 V624），不预授任何角色模板；最终审批人须经角色持有本码（复核策略 specific_role 把关）。
INSERT INTO permissions(permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('ticket.product.refundable.approve', '票种开启可退审批', 'ticketing', 'high', true, 'active');
