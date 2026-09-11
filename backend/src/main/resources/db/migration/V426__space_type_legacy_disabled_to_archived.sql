-- M018：历史停用行改为存档。此后 disabled 只表示删除审批通过后的软删除。
UPDATE space_type_dictionary SET status = 'archived' WHERE status = 'disabled';
