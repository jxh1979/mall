-- 文件版本：V0.01
-- V727：第十八批 BS 段第 373 条 373-R1、373-R5（游乐安全巡检按点位顺序走点、逐点扫码）。
-- 依据：BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、不得标记为已完成」；
-- 决策台账 DEC-R3-2026-08-04-33「新增两条扫码强制规则：……巡检必须按点位顺序完成，缺一个点位上一个点位就不能通过」；
-- 第十八批裁决 76（选「逐点扫码」：每个点位生成一张码贴在现场，巡检员用工作手机按顺序扫码后记结果，扫错码或跳点记不上）。
-- 点位清单配在游乐项目上（数组顺序即走点顺序）；走点表只存进行中的一轮，建成巡检或换清单即清空。研发阶段无存量（UD-07），不回填。

ALTER TABLE amusement_projects ADD COLUMN inspection_points JSONB NOT NULL DEFAULT '[]'::jsonb;
COMMENT ON COLUMN amusement_projects.inspection_points IS
    '巡检点位清单（有序，数组顺序即走点顺序；元素 point_name 点位名称、point_code 贴在现场的点位码）；空数组＝未配置点位，巡检照旧填检查项';

CREATE TABLE IF NOT EXISTS amusement_inspection_walk_points (
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects (id) ON DELETE CASCADE,
    seq INTEGER NOT NULL,
    point_name VARCHAR(120) NOT NULL,
    result VARCHAR(8) NOT NULL,
    note TEXT,
    checked_by VARCHAR(128) NOT NULL,
    checked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (project_id, seq),
    CONSTRAINT chk_amusement_inspection_walk_points_seq CHECK (seq >= 1),
    CONSTRAINT chk_amusement_inspection_walk_points_result CHECK (result IN ('pass', 'fail'))
);
COMMENT ON TABLE amusement_inspection_walk_points IS
    '游乐安全巡检进行中的走点（按点位顺序逐点扫码记录；上一个闭店时点之前的行不算本轮；建成巡检或换点位清单即清空）';
COMMENT ON COLUMN amusement_inspection_walk_points.seq IS '点位序号（1 起，对应 amusement_projects.inspection_points 的顺序）';
COMMENT ON COLUMN amusement_inspection_walk_points.result IS '本点位检查结果：pass 通过、fail 不通过';
COMMENT ON COLUMN amusement_inspection_walk_points.checked_by IS '记录人（当前登录账号）';
COMMENT ON COLUMN amusement_inspection_walk_points.checked_at IS '记录时刻（服务端时刻）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_inspection_walk_points TO complex_app';
    END IF;
END $$;
