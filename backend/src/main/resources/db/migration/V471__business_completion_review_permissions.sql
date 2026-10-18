-- 文件版本：V0.01
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status) VALUES
 ('dealer.delivery_resolution.approve','经销交付差异主管审批','dealer','high',true,'active'),
 ('inventory.quarantine.approve','隔离库存处置主管审批','inventory','high',true,'active'),
 ('ticket.reward.rule.approve','门票奖励规则主管审批','ticketing','high',true,'active'),
 ('ticket.reward.national.approve','全国积分结算中心参数审批','ticketing','high',true,'active'),
 ('ticket.reward.sales_contract.approve','门票销售合同主管审批','ticketing','high',true,'active');
