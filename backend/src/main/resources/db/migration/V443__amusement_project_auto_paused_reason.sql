-- V443 硬安全容量自动停运后自动恢复开放（CG-02）。
--
-- 【业务口径】(business-system-map「容量分两道」/「容量控制」条、business-enum-quick-reference「容量双阈值」条)
--   「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后由系统自动恢复开放」
--   「安全容量（硬上限）任何情况都阻断并自动停运，人数回落到硬上限以下后由系统自动恢复开放」
--
-- 【为什么要加列】项目 open_status=paused 有三种来源：硬超容自动停运（本条要自动恢复）、高危安全事故自动停运
--   （不得被自动恢复）、人工暂停（不动）。表上只有 open_status 一列区分不了来源，故记下「系统自动停运的原因」：
--   hard_capacity=硬超容自动停运；safety_incident=高危事故自动停运；NULL=非系统自动停运（含人工）。
--   任何路径把 open_status 改离 paused 时一并清空；硬超容停运期间发生高危事故则升级为 safety_incident。
--   不回填存量（铁律 8）。
ALTER TABLE amusement_projects ADD COLUMN IF NOT EXISTS auto_paused_reason VARCHAR(32);

ALTER TABLE amusement_projects DROP CONSTRAINT IF EXISTS chk_amusement_projects_auto_paused_reason;
ALTER TABLE amusement_projects ADD CONSTRAINT chk_amusement_projects_auto_paused_reason
    CHECK (auto_paused_reason IS NULL OR auto_paused_reason IN ('hard_capacity', 'safety_incident'));

COMMENT ON COLUMN amusement_projects.auto_paused_reason IS
    '系统自动停运原因：hard_capacity=硬超容（人数回落到硬上限以下由系统自动恢复开放）；safety_incident=高危事故（不自动恢复）；NULL=非系统自动停运。';
