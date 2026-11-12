-- 文件版本：V0.01
-- V618（第十八批 AH 段第 215 条 215-R2）：通票最低赠酒杯数按票种可配，值与上下限同时配置。
-- 依据：ENUM:34「一张酒吧通票或跨域组合通票覆盖的每一家酒吧，赠酒杯数不得低于后台配置的最低赠酒杯数（业务参数，按票种可配，出厂默认 1 杯，
--   须同时配置上下限）……低于该下限的票种不得发布上架」；BSM:1032 同句。
-- 只存后台配过的票种；没有行的按出厂 1 杯（上下限 HTML 未给数值，同 V537 先例出厂只定值，见 TicketCatalogService）。CHECK 同 V617
-- approval_deadline_settings。

CREATE TABLE ticket_type_gift_floors (
    ticket_type_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_types(id),
    min_cups INTEGER NOT NULL,
    min_cups_lower INTEGER NOT NULL,
    min_cups_upper INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ttgf_min_cups CHECK (1 <= min_cups_lower AND min_cups_lower <= min_cups AND min_cups <= min_cups_upper)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON ticket_type_gift_floors TO complex_app';
    END IF;
END $$;
