-- 文件版本：V0.01
-- V740：第十八批 BY 段第 387 条 387-R2（已存档内容页每个分类都显示存档理由）。
-- 依据：第十八批裁决 81「每个分类都显示存档理由；空间类型补存存档时间、存档人、理由，岗位角色模板补存理由」；
-- BSM:569「后台另设「已存档内容」页，进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。」；BSM:577「存档及删除都需要填写理由」。
-- 空间类型存档时写三列、取消存档清空；岗位角色模板的存档时间、存档人 V386 已有，只补存档理由（存档时写、恢复清空）。
-- 研发阶段无存量（UD-07），不回填。
ALTER TABLE space_type_dictionary
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN archived_by VARCHAR(64),
    ADD COLUMN archive_reason TEXT;

ALTER TABLE position_role_templates
    ADD COLUMN archive_reason TEXT;
