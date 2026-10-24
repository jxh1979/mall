-- 文件版本：V0.01
-- 第十三批 B 段 ROOT-03（B-account）：客人投诉提交与满意度、学生认证、渠道票与一体机辅助购票按已验证手机号归集。

-- clients-R8 BSM 第 1379 行「工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案
-- 及客人反馈后可以关闭）……」：工单来源闭集加 customer；客人投诉关闭时记录的客人反馈落 customer_feedback。
ALTER TABLE facility_work_orders DROP CONSTRAINT IF EXISTS chk_fwo_source;
ALTER TABLE facility_work_orders ADD CONSTRAINT chk_fwo_source
    CHECK (source IN ('merchant_self', 'platform', 'equipment_alarm', 'inspection', 'customer'));
ALTER TABLE facility_work_orders ADD COLUMN customer_feedback TEXT;

-- BSM 第 869 行「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」：客人投诉附件（真实文件，
-- 经 EvidenceFileStorage 落盘，库里只存相对 key 与服务端判定的类型）。
CREATE TABLE customer_complaint_media (
    id VARCHAR(64) PRIMARY KEY,
    work_order_id VARCHAR(64) NOT NULL REFERENCES facility_work_orders(id),
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    object_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_customer_complaint_media_work_order ON customer_complaint_media (work_order_id);

-- R28 BSM 第 903 行「学生认证必须在本人注册会员后单独申请，认证通过只增加学生资格，不自动创建会员身份」；第 777 行「学生票要票务运营岗
-- 人工审核证件 + 学信网核验通过才可用」；第 890 行「采集证件……前先单独取得同意，并留同意版本、时间和范围」（consent_id 指向该次同意）。
-- 操作员裁决（第十三批第 1 条）「有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」→ valid_until。
-- 注销时证件附件与验证码清空（BSM 第 863 行「证件等个人信息一律删除」），故附件 key 存本表、不进只增不改的证据附件表。
CREATE TABLE customer_student_certifications (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    status VARCHAR(16) NOT NULL,
    consent_id VARCHAR(80) NOT NULL,
    attachment_keys JSONB NOT NULL DEFAULT '[]'::jsonb,
    chsi_verification_code VARCHAR(64),
    valid_until DATE,
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMPTZ,
    reject_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_customer_student_certifications_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT chk_customer_student_certifications_valid_until CHECK (status <> 'approved' OR valid_until IS NOT NULL)
);
CREATE UNIQUE INDEX uq_customer_student_certifications_pending
    ON customer_student_certifications (customer_id) WHERE status = 'pending';
CREATE INDEX idx_customer_student_certifications_status ON customer_student_certifications (status, created_at);

-- 权限点只入字典（中文名），不预绑岗位模板，由既有授权流程授予票务运营岗。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('ticket.student_cert.review', '审核学生认证（票务运营岗，学信网人工核验）', 'ticketing', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- R29 BSM 第 892 行「第三方团购……购买的票按渠道来源在“我的订单/我的票”统一展示」、第 986 行「一体机……客人用手机号短信验证绑定，
-- 在官方 App/小程序查看门票」：购票手机号盲索引（与 customers.phone_hash 同一函数），供尚未注册的手机号注册后把渠道票与一体机票归集到本人。
ALTER TABLE ticket_orders ADD COLUMN buyer_phone_hash CHAR(64);
CREATE INDEX idx_ticket_orders_unlinked_buyer_phone ON ticket_orders (buyer_phone_hash) WHERE customer_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON customer_complaint_media, customer_student_certifications TO complex_app';
    END IF;
END $$;
