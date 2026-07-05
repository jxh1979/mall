-- 游乐核销 阶段2（2026-07-05 用户拍板）：出场核销 + 主管释放 + 并发容量硬化。
--
-- 背景：自由进出型（并发容量 max_concurrent / amusement_capacity_snapshot.occupied_count）人随时进出，
-- 阶段1 只做「入场 +N」，容量只增不减。本迁移给核销记录加方向/释放时刻/入场配对列，
-- 支撑「出场 -N 释放」并防重复出场；并加部分唯一约束防并发双提交超扣。
--
-- 场次/批次型（amusement_sessions.used_capacity）满等下场、无需释放，出场核销对其幂等跳过（不减）。

ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS direction VARCHAR(8) NOT NULL DEFAULT 'entry';
ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS released_at TIMESTAMPTZ;
-- 出场记录关联其入场记录（direction='exit' 时非空，指向被释放的 direction='entry' 记录）。
ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS entry_verification_id VARCHAR(64);
-- 释放该入场用到的权益实例（出场按此定位入场并释放并发容量；入场记录落自身权益 id）。
ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS entitlement_id VARCHAR(128);
-- 入场落核销点位（device_id 已有列）：出场释放并发容量须按入场同一 (point, device) 维度键 -N，故入场须留痕点位。
ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS location_point_id VARCHAR(128);

ALTER TABLE amusement_verifications
    DROP CONSTRAINT IF EXISTS chk_amusement_verifications_direction;
ALTER TABLE amusement_verifications
    ADD CONSTRAINT chk_amusement_verifications_direction CHECK (direction IN ('entry', 'exit'));

-- 【并发硬化】同一权益实例在同一场次至多一条【未释放的成功入场】：并发双提交（不同幂等键）时
-- 第二条 INSERT 撞唯一约束回滚 → claim 前后靠唯一约束兜底防超扣。出场释放后 released_at 非空，
-- 允许合法二次入场（自由进出：出场后可再入，各扣一次权益）。
CREATE UNIQUE INDEX IF NOT EXISTS uk_amusement_verifications_active_entry
    ON amusement_verifications (entitlement_id, session_id)
    WHERE direction = 'entry'
      AND verification_result = 'success'
      AND entitlement_id IS NOT NULL
      AND released_at IS NULL;

-- 幂等：一条入场记录至多被出场释放一次（同一 entry_verification_id 只允许一条 exit 记录）。
CREATE UNIQUE INDEX IF NOT EXISTS uk_amusement_verifications_exit_of_entry
    ON amusement_verifications (entry_verification_id)
    WHERE direction = 'exit'
      AND entry_verification_id IS NOT NULL;

-- 阶段2 主管释放（异常清场）新增 override 类型 release：放宽 V014 类型白名单。
ALTER TABLE amusement_manual_overrides
    DROP CONSTRAINT IF EXISTS chk_amusement_manual_overrides_type;
ALTER TABLE amusement_manual_overrides
    ADD CONSTRAINT chk_amusement_manual_overrides_type
        CHECK (override_type IN ('allow', 'deny', 'pause', 'resume', 'release'));
