-- 文件版本：V0.01
-- 永久停用以实际终段读数为依据，照片和原因沿用读数证据链。
CREATE TABLE energy_meter_retirements(
    meter_id VARCHAR(64) PRIMARY KEY REFERENCES energy_meters(id),
    terminal_reading_id VARCHAR(64) NOT NULL UNIQUE REFERENCES energy_meter_readings(id),
    retired_at TIMESTAMPTZ NOT NULL
);
CREATE TRIGGER trg_energy_retirement_immutable BEFORE UPDATE OR DELETE ON energy_meter_retirements
    FOR EACH ROW EXECUTE FUNCTION energy_preserve_original_fact();
