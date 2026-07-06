-- 权限字典 P3 术语打磨(2026-07-06):承接 V251 中文化,微调 8 条 permission_name 使术语更顺、语域统一、高危可辨。
-- permission_code 机读标识不动;仅改展示名。幂等:纯 UPDATE 按 code 定位,重复执行结果一致。
-- 基线 origin/main=edb6696(V251/V252 之后)。

-- 1) 动宾更顺(退款…账单 → 为…账单退款);保留结算语义括注
UPDATE permissions SET permission_name = '为已结算酒吧账单退款(结算后扣减日净营收)' WHERE permission_code = 'bar.bill.refund';

-- 2) 与核销语域统一(冲正偏财务 → 撤销)
UPDATE permissions SET permission_name = '撤销票务核验' WHERE permission_code = 'ticket.verify.reverse';

-- 3) 团体/私享为两类预约,不合并并列(团体私享 → 团体和私享)
UPDATE permissions SET permission_name = '审批游乐团体和私享预约锁容' WHERE permission_code = 'amusement.group_booking.approve';
UPDATE permissions SET permission_name = '管理游乐团体和私享预约' WHERE permission_code = 'amusement.group_booking.manage';
UPDATE permissions SET permission_name = '查看游乐团体和私享预约' WHERE permission_code = 'amusement.group_booking.view';

-- 4) 避免与"覆盖写入"混淆(覆盖 → 强制改判)
UPDATE permissions SET permission_name = '强制改判游乐核验结果' WHERE permission_code = 'amusement.verification.override';

-- 5) critical 高危权限名末尾加提示
UPDATE permissions SET permission_name = '审批或驳回主管复核决定(高危)' WHERE permission_code = 'governance.supervisor_review.approve';

-- 6) critical 敏感权限(看原始未脱敏消息)末尾加提示
UPDATE permissions SET permission_name = '查看企微原始消息(敏感)' WHERE permission_code = 'wecom.view_raw';
