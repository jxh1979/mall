-- C-5（治理 HTML platform-governance-map :429「建立商户档案、运营主体、负责人、第一联系人和第二联系人」）：
-- merchants 补「负责人」owner_person 列。此前仅有 contact_name/contact_phone（第一联系人）+
-- secondary_contact_name/phone（第二联系人），缺独立的「负责人」字段。
-- owner_person 与联系人区分：负责人=商户经营责任主体自然人；联系人=日常对接人。
ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS owner_person VARCHAR(120);
