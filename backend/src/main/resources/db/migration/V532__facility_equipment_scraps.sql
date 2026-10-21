-- 文件版本：V0.01
-- HP-F03（BSM「楼宇机电设备」L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理，记录批准人、处置方式及实际处置凭证；
-- 残值或损失由财务记录并关联原设备。标记报废不等于实物处置或款项完成」；ENUM L266；PGM L821；批准 13｜Q19
-- 「报废状态、实际处置、款项分别记，标报废不等于都完成」）：
-- 报废单分别记三项事实——审批（status/approved_by/approved_at，批准后设备才置 retired）、实物处置（disposal_*，须凭证）、
-- 款项（residual_*/loss_*/finance_*，残值应收与损失内部核算关联原报废单与设备，收款走应收应付既有登记收付款）。
-- 借来的设备按库存借用流程（FS-019）办理，不进本表。

CREATE TABLE facility_equipment_scraps (
    id VARCHAR(64) PRIMARY KEY,
    equipment_id VARCHAR(64) NOT NULL REFERENCES facility_equipment(id),
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    reason TEXT NOT NULL,
    disposal_method TEXT NOT NULL,
    review_id VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'pending_approval',
    requested_by VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    disposal_completed_by VARCHAR(64),
    disposal_completed_at TIMESTAMPTZ,
    disposal_evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    disposal_note TEXT,
    residual_amount_cent BIGINT,
    residual_counterparty_id VARCHAR(64),
    residual_ar_ap_item_id VARCHAR(64),
    loss_amount_cent BIGINT,
    loss_ar_ap_item_id VARCHAR(64),
    finance_recorded_by VARCHAR(64),
    finance_recorded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fes_status CHECK (status IN ('pending_approval','approved','rejected')),
    CONSTRAINT chk_fes_residual CHECK (residual_amount_cent IS NULL OR residual_amount_cent >= 0),
    CONSTRAINT chk_fes_loss CHECK (loss_amount_cent IS NULL OR loss_amount_cent >= 0)
);
-- 一台设备同时只能有一张在办或已批准的报废单。
CREATE UNIQUE INDEX uq_facility_equipment_scraps_open ON facility_equipment_scraps (equipment_id)
    WHERE status IN ('pending_approval','approved');
CREATE INDEX idx_facility_equipment_scraps_review ON facility_equipment_scraps (review_id);
