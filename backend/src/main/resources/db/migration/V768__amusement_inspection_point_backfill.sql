-- 文件版本：V0.01
-- V768：第十八批 CF 段第 404 条 404-R1（游乐巡检点位缺扫码人工补录接入通用补录主链）。
-- 依据：BSM:1300「巡检必须按点位顺序完成……确需补录的一律须附证据（照片／录音／录像等）并经主管复核后方生效」；
-- 决策台账 DEC-R6-09「扫码缺失可事后人工补录，但一律须附证据（照片／录音／录像等）并经主管复核后方生效；补录记录与原始扫码记录并列展示、互不覆盖」；
-- BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」；第十八批裁决 76（题面写明 DEC-R6-09 扫码缺失可补录也扩到游乐巡检行）。
-- ① 补录对象类型加 amusement_inspection_point（目标＝游乐项目，部位＝点位序号）；② 主管复核通过后落的走点行记补录单编号（扫码记的行为空）。

ALTER TABLE manual_backfills DROP CONSTRAINT chk_manual_backfills_target_type;
ALTER TABLE manual_backfills ADD CONSTRAINT chk_manual_backfills_target_type
    CHECK (target_type IN ('trace_handover_step', 'bar_session_sales', 'bar_session_service', 'amusement_inspection_point'));
COMMENT ON COLUMN manual_backfills.target_part IS '补录的部位（交接扫码补录为交接段名，游乐巡检点位补录为点位序号，其余为空）';

ALTER TABLE amusement_inspection_walk_points ADD COLUMN backfill_id VARCHAR(64);
COMMENT ON COLUMN amusement_inspection_walk_points.backfill_id IS '人工补录单编号；为空表示扫码记录';
