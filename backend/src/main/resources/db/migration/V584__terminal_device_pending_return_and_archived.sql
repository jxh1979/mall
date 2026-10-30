-- 文件版本：V0.01
-- 第十八批 K 段第 96 条 96-R2、96-R3。ENUM:241「设备状态」「待领用、在用、待归还、维修中、挂失、报废、归档」。
-- 第十八批裁决 23「停岗离职转待归还」：员工停岗或离职生效时，名下还在用的工作手机、扫码设备等自动转 pending_return（待归还）；
-- 设备管理员收回实物后在后台办归还回待领用，不给该员工记下班（工作手机这次领用以 employment_ended 结束），收不回的挂失。
-- 第十八批裁决 24「报废后可归档」：设备管理员对已报废的设备办理归档 archived，原因必填；归档后台账里仍可查，不能再恢复或领用。
-- 重建 V107 的 chk_terminal_devices_status 与 V468 的 chk_work_phone_context_close（之后没有迁移改过这两条约束）。

ALTER TABLE terminal_devices DROP CONSTRAINT chk_terminal_devices_status;
ALTER TABLE terminal_devices ADD CONSTRAINT chk_terminal_devices_status CHECK (
    status IN ('bound', 'returned', 'unbound', 'disabled', 'repair', 'lost', 'found', 'scrapped', 'blocked',
               'pending_return', 'archived')
);

ALTER TABLE staff_work_phone_contexts DROP CONSTRAINT chk_work_phone_context_close;
ALTER TABLE staff_work_phone_contexts ADD CONSTRAINT chk_work_phone_context_close CHECK (
    close_reason IS NULL OR close_reason IN ('shift_end', 'fault_swap', 'employment_ended')
);
