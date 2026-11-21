-- 文件版本：V0.01
-- V731：第十八批 BR 段第 274 条 274-R1（分红收入域加「游乐域内结算」）。
-- 依据：ENUM:297「参股方、主理人和其他股东按每家采用参股经营的经营主体工商登记的真实持股比例分配；酒吧的参股方为娱乐平台公司，引进参股的游乐项目的参股方
--   为引进该项目的域主管公司」；BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径）……」；BSM:753「游乐公司再按已批准的内部结算价或域内
--   规则，与实际提供服务的游乐商户结算。」；决策台账 DEC-R3-2026-08-04-17。
-- 分红计算单明细行类型闭集（现行 V660）加 revenue_amusement（游乐域内结算收入）；合并时以主干现行闭集为底取并集。研发阶段无存量（UD-07）。
ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund', 'revenue_amusement',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other', 'expense_ticket_service_fee', 'tax_period',
     'net_profit', 'loss_carryforward', 'legal_cap', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout',
     'shareholder_gross', 'shareholder_tax_withheld', 'shareholder_payout', 'rounding_residual', 'loan_repayment'));
