-- V350 「高价值」判定全平台归一（业务HTML 2026-07-28 定稿口径）。
--
-- 【业务口径】(business-system-map §物品档案「高价值物品」/ business-enum-quick-reference「高价值判定」)
--   全平台一套判定：以该物品的**进货价格**与后台配置的**一个固定金额阈值**比较——
--   进货价高于阈值即高价值、低于即低价值；或该物品被**手工标记**为高价值。
--   退酒、报损、盘点差异、存酒作废等需主管复核或先审后行的场景一律以此判定。
--   **阈值未配置时一律按高价值处理、拦下走审批，不得静默放行。**
--
-- 【改造前是三套阈值 + 两处静默放行】（本迁移前实读）
--   ① 退酒     bar_void_approval_configs.high_value_threshold_cent 比「账单明细金额」——口径错（用售价不是进货价）；
--   ② 报损/盘点 inventory_items.high_value_threshold_cents 比「数量×申报单价」——**物料没录阈值就直接放行**（静默）；
--   ③ 存酒作废 saved_bottles.is_high_value 取「登记时前端传的布尔」——**前端不传就永远低价值**（静默）。
--   本迁移把 ① 的配置表升为全平台唯一阈值表，删掉 ② 的按物料阈值，把 ③ 的手工标记落到物品档案。
--
-- 【进货价取哪儿】inventory_item_cost_state.avg_unit_cost_cent（V296 移动加权成本台账，
--   has_cost_basis=TRUE 才算数）。进货价未知 = 无法证明它低价值 ⇒ 同样按高价值拦下（fail-closed），
--   否则「不录进货价」就成了绕过复核的后门，与阈值未配置同理。判定入口见 HighValueDecisionService。

-- ========== 1) 阈值表升格为全平台唯一：bar_void_approval_configs → high_value_thresholds ==========
-- V345 建的表结构（项目公司 × 商户 × is_draft 草案位 + 软删）完全够用，只是名字被酒吧域占了名。
-- 现在它同时管退酒/报损/盘点差异/存酒作废，名字必须跟着口径走——否则运营在配置中心看到
-- 「酒吧退酒审批阈值」，不会知道改一次同时改了报损和存酒作废的拦截线。
--
-- 用「删旧建新」而不是 ALTER ... RENAME TO：仓内三处 schema 生成器（域归属审计 / 矩阵对账 /
-- 租户隔离基线）都只解析 CREATE TABLE 与 DROP TABLE，不认 RENAME，用 RENAME 它们会继续
-- 把旧表名当权威。开发库无存量包袱（本表只有出厂种子 + 少量运营特例行），删旧建新最不容易漂。
DROP TABLE IF EXISTS bar_void_approval_configs;

CREATE TABLE IF NOT EXISTS high_value_thresholds (
    id                        VARCHAR(48) PRIMARY KEY,
    project_id                VARCHAR(64),          -- NULL=全局默认；非空=某项目公司特例
    merchant_id               VARCHAR(64),          -- NULL=该项目下所有商户通用；非空=某商户特例
    high_value_threshold_cent BIGINT      NOT NULL, -- 物品「进货价」高于本值即高价值（分）；出厂 500000=5000 元
    effective_from            DATE,
    effective_to              DATE,
    status                    VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft                  BOOLEAN     NOT NULL DEFAULT FALSE,
    archive_reason            TEXT,                 -- 软删铁律：下线留理由，不物理删
    updated_by                VARCHAR(64),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_hvt_threshold CHECK (high_value_threshold_cent >= 0),
    CONSTRAINT chk_hvt_status    CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_high_value_thresholds
    ON high_value_thresholds (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        is_draft
    ) WHERE status = 'active';

-- 出厂种子 = 5000 元（500000 分），全局默认作用域、生效态；写入面把它列为「禁软删的出厂默认行」。
INSERT INTO high_value_thresholds (id, project_id, merchant_id, high_value_threshold_cent, is_draft)
VALUES ('hvt_default', NULL, NULL, 500000, FALSE)
ON CONFLICT (id) DO NOTHING;

-- 新表一律授权受限运行时（V296/V298/V338 教训：漏 GRANT → 运行时静默 permission denied）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON high_value_thresholds TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;

COMMENT ON TABLE high_value_thresholds IS
    '全平台「高价值物品」判定阈值（分）：物品进货价高于本值即高价值。退酒/报损/盘点差异/存酒作废共用。缺配置一律按高价值拦下。';

-- ========== 2) 物品档案：删「按物料阈值」，加「手工标记高价值」 ==========
-- 按物料各自设阈值 = 三套阈值里的一套，与「一个固定金额阈值」直接冲突，删掉（开发库无存量包袱）。
-- 手工标记按业务口径落在**物品档案**（不是每瓶存酒各标一次）：标了就恒为高价值，不看进货价。
ALTER TABLE inventory_items DROP CONSTRAINT IF EXISTS chk_inventory_items_high_value_threshold;
ALTER TABLE inventory_items DROP COLUMN IF EXISTS high_value_threshold_cents;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS manual_high_value BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN inventory_items.manual_high_value IS
    '手工标记为高价值：TRUE 则不论进货价一律按高价值走复核/先审后行（业务HTML「或被手工标记为高价值」）。';

-- saved_bottles.is_high_value / high_value_threshold_cents 保留，但语义改为
-- **登记当刻的判定快照**（由服务端按统一入口算出后写入，不再由前端传布尔）：
-- 作废时以统一入口**重新判定**为准（阈值/进货价会随运营变动，快照只用于事后举证）。
COMMENT ON COLUMN saved_bottles.is_high_value IS
    '登记当刻按统一高价值口径判定的结果快照（服务端计算，非前端传入）；作废时以实时重判为准。';
COMMENT ON COLUMN saved_bottles.high_value_threshold_cents IS
    '登记当刻生效的高价值阈值快照（分）；NULL=当时阈值未配置（按高价值处理）。';
