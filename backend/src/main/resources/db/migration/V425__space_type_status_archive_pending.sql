-- M018：空间种类状态在「启用 / 存档 / 待删除审批 / 已删除」四值。
-- V423 只允许 active/disabled；存档与删除审批写入 archived/pending_delete 会被库约束拒绝。
-- disabled 保留：审批通过后的软删除，以及历史「停用」行（本轮起停用即存档）。
ALTER TABLE space_type_dictionary DROP CONSTRAINT IF EXISTS chk_space_type_dictionary_status;
ALTER TABLE space_type_dictionary ADD CONSTRAINT chk_space_type_dictionary_status
    CHECK (status IN ('active', 'disabled', 'archived', 'pending_delete'));
