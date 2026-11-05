-- 文件版本：V0.01
-- 第十八批 R 段第 142 条 142-R4：同一钱箱多人当班。
-- 依据：BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」
--   「班结按本班次汇总各支付方式应收」「当班收银员与接班人双方确认签字」；BSM:1489、ENUM:283「多人共同使用同一钱箱时由整个当班组共同补足」。
-- bar_shift_members：加入本钱箱的其他当班收银员（开班人不入此表）；加入时点之后其收款并入本班次应收，交班单签字记 signed_at。
-- bar_shifts.responsible_user_id：有成员的班次短款能定位到具体收银员时记该人；有成员且为空＝当班组共同补足。
-- 研发期无存量（UD-07），不回填。
CREATE TABLE bar_shift_members (
    shift_id VARCHAR(64) NOT NULL REFERENCES bar_shifts(id),
    user_id VARCHAR(64) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    signed_at TIMESTAMPTZ,
    PRIMARY KEY (shift_id, user_id)
);

ALTER TABLE bar_shifts
    ADD COLUMN responsible_user_id VARCHAR(64);
