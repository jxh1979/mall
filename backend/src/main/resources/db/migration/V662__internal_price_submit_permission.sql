-- 文件版本：V0.01
-- V662（第十八批 AW 段第 291 条 291-R2）：内部结算价提交单列权限码，由实际提供方持有（商户账号只报本商户、经营主体人员只报本公司）。
-- 依据：ENUM:54「实际提供商品或服务的主体提交，对应域主管公司审批，提交人与审批人分离」；BSM:1034「酒吧商户只能提交价格资料」；
--   PGM:660「查看、导出和修改分别授权」。高敏、授出前审批，不预授任何角色模板。
INSERT INTO permissions(permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('settlement.internal_price.submit', '内部结算价提交', 'settlement', 'high', true, 'active');
