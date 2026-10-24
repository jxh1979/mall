-- 文件版本：V0.01
-- 第十三批 B 段复核问题修复（服务端）。

-- ① ENUM 第 103 行「订台定金状态未支付、免收（员工代订）、已支付、达标已原路退回、已退还、已没收、已超时释放」；BSM 第 1062 行「超过最晚到店时间
--    未到店的，系统自动释放该台位——已付定金的按爽约规则没收定金，未付定金的直接作废」：超时释放时未付定金记 timeout_released（已超时释放）。
ALTER TABLE bar_reservations DROP CONSTRAINT IF EXISTS chk_bar_reservations_deposit_status;
ALTER TABLE bar_reservations ADD CONSTRAINT chk_bar_reservations_deposit_status
    CHECK (deposit_status IN ('not_required', 'pending', 'paid', 'settled', 'timeout_released'));

-- ② BSM 第 1025 行「下单时即时付款、付款成功订单才成立」：扫码点单已收款却成不了单（该桌已结束接待、所点酒水已沽清或下架、扫码点单已关、
--    下单后改价）时原路待退，记下原因码（退款中时向客人说明原因）。
ALTER TABLE bar_scan_order_payments ADD COLUMN IF NOT EXISTS reject_code VARCHAR(64);

-- ③ BSM 第 1749 行「混合支付的退款按收款相反顺序逐笔原路退回……储值部分回冲仍按先赠送金后本金的既有顺序」：批准时分到在线笔、待财务原路打款的
--    各笔（[{channel, amount_cent}]）；储值笔当场回原卡，不在其中。财务确认拨付按此打款，不按整笔金额。
ALTER TABLE ticket_refunds ADD COLUMN IF NOT EXISTS payout_items JSONB;

-- ④ PGM 第 876 行「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」：学信网在线验证码改存 PiiCipher 密文（信封串长于原 64 位）。
ALTER TABLE customer_student_certifications ALTER COLUMN chsi_verification_code TYPE TEXT;

-- ⑤ 第十三批裁决第 5 条「上级审批后发布：运营编辑后走审批，批准才对客人展示」：提交发布时先存待审原文，审批人审批前能读到这次要发布的内容；
--    批准后同一内容再提交才写入 customer_home_contents（客人端只读该表），写入后删除待审原文。
CREATE TABLE IF NOT EXISTS customer_home_content_drafts (
    project_id VARCHAR(64) PRIMARY KEY REFERENCES projects(id),
    intro TEXT NOT NULL,
    announcement TEXT NOT NULL,
    submitted_by VARCHAR(128) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON customer_home_content_drafts TO complex_app';
    END IF;
END $$;
