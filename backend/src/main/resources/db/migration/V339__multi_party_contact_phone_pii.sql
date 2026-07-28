-- V339 多主体 PII 明文加密（P0-A / ★GAP-07 / SEC-04，v7 综合审计 E 域实证：merchants 59/59 手机号明文入库）。
--
-- 缺陷面：商户、供应商、招商线索、费用通知接收人、施工进场报备五条写入路径的联系电话都没接 PiiCipher，
-- 明文落库。客人手机号（customers.phone / V188）、购票人手机号（ticket_orders.buyer_phone / V247）、
-- 游乐团体联系人（V248）早已加密，本迁移把同一范式补齐到剩下的「多主体」联系电话。
--
-- 本迁移只做三件事，加密动作本身在应用层（密钥在 KeyVault，SQL 拿不到）：
--   ① 加宽列到 VARCHAR(128)——AES-GCM 信封密文 enc:v1:base64(iv|ct+tag) 装不进原来的 20/32/40 位；
--   ② 加 NOT VALID 的 CHECK 约束——存量明文行放行（回填前必然还在），但任何新写入/更新必须是信封密文，
--      明文写入在数据库层直接失败，仓库以外的手写 SQL / 批量导入也绕不过（V249 的 DB 级边界思路）；
--   ③ 种明文查看权限 pii.contact_phone.view（V279 范式）。
-- 存量明文由启动期 MultiPartyContactPhonePiiBackfill 就地加密（V188/V247/V248 同款 ApplicationRunner）。
--
-- 防二次加密：三层。(a) PiiCipher.encrypt 见到 enc:v1: 前缀原样返回，天然幂等；
-- (b) 回填 WHERE 只取 NOT LIKE 'enc:v1:%' 的行，已加密行根本不进结果集；
-- (c) 回填 UPDATE 前先 decrypt 再 encrypt，半途中断重跑也拿得到正确明文。
-- 因此本迁移 + 回填可以任意重跑，不会把密文再加密一层。

-- ① 加宽（幂等：重复执行同类型是空操作）
ALTER TABLE merchants                            ALTER COLUMN contact_phone           TYPE VARCHAR(128);
ALTER TABLE merchants                            ALTER COLUMN secondary_contact_phone TYPE VARCHAR(128);
ALTER TABLE inventory_suppliers                  ALTER COLUMN contact_phone           TYPE VARCHAR(128);
ALTER TABLE leasing_leads                        ALTER COLUMN contact_phone           TYPE VARCHAR(128);
ALTER TABLE merchant_fee_notification_recipient  ALTER COLUMN recipient_phone         TYPE VARCHAR(128);
ALTER TABLE site_entry_reports                   ALTER COLUMN contact_phone           TYPE VARCHAR(128);

-- ② DB 级明文写入边界（NOT VALID：不校验存量行，只卡新写入/更新）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_merchants_contact_phone_pii') THEN
        ALTER TABLE merchants ADD CONSTRAINT chk_merchants_contact_phone_pii
            CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_merchants_secondary_contact_phone_pii') THEN
        ALTER TABLE merchants ADD CONSTRAINT chk_merchants_secondary_contact_phone_pii
            CHECK (secondary_contact_phone IS NULL OR secondary_contact_phone = ''
                   OR secondary_contact_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_suppliers_contact_phone_pii') THEN
        ALTER TABLE inventory_suppliers ADD CONSTRAINT chk_inventory_suppliers_contact_phone_pii
            CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_leasing_leads_contact_phone_pii') THEN
        ALTER TABLE leasing_leads ADD CONSTRAINT chk_leasing_leads_contact_phone_pii
            CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_fee_notification_recipient_phone_pii') THEN
        ALTER TABLE merchant_fee_notification_recipient ADD CONSTRAINT chk_fee_notification_recipient_phone_pii
            CHECK (recipient_phone IS NULL OR recipient_phone = '' OR recipient_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_site_entry_reports_contact_phone_pii') THEN
        ALTER TABLE site_entry_reports ADD CONSTRAINT chk_site_entry_reports_contact_phone_pii
            CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID;
    END IF;
END $$;

-- ③ 明文查看权限种子（V279 范式）＋平台管理员直授（V206 范式）。其它角色按需另行授权。
-- 依据业务 HTML §应急恢复和客人隐私：「个人信息加密存放」「员工日常界面只显示脱敏号码，不见明文」。
-- 无此权限的读端，五处联系电话一律返回掩码（前 3 后 4）；有权限者才见明文。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('pii.contact_phone.view', 'View multi-party contact phone plaintext (merchant/supplier/lead/fee recipient/site entry)',
        'governance', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'pii.contact_phone.view'
ON CONFLICT (role_id, permission_code) DO NOTHING;
