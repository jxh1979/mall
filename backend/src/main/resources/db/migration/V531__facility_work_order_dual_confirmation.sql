-- 文件版本：V0.01
-- HP-F02（BSM「物业服务」运营细则 L1385「复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭。
-- 自动生成的大型维修工单，由该设备或点位的在岗责任人承接提单确认；责任人不在岗或本人负责此次维修时，由物业主管指定其他人员并留痕。
-- 仍须同时完成主管／物业验收和提单责任人确认，才能关闭」；ENUM L262「复杂类（大型设备报修等）须主管验收＋提单方确认双闸」；
-- DEC-R3B-2026-08-04-25、DEC-R6-13、批准 12｜Q17；第十二批操作员裁决 2026-10-21 第 12 条「关联设备的报修才两道」）：
-- 关联了设备的报修单走两道，主管／物业验收（verified_at/verifier_id）与提单方确认（reporter_confirmed_at/by）分别留存，
-- 两项齐全才关闭；没关联设备的普通报修照旧按 facility_work_order_policy.acceptance_by 配置验收，策略不改。
-- confirmer_id＝自动大型维修工单的提单确认承接人。

ALTER TABLE facility_work_orders
    ADD COLUMN confirmer_id VARCHAR(64),
    ADD COLUMN reporter_confirmed_at TIMESTAMPTZ,
    ADD COLUMN reporter_confirmed_by VARCHAR(64);
