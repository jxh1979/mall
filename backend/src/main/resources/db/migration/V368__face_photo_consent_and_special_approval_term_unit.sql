-- V368 两件用户 2026-07-28 裁决：① 客人端人脸照同意落库留痕；② 超池特批期限放开到「天」。
--
-- ========== ① 人脸照同意留痕（用户裁决「要，落到同意台账」）==========
-- V359 让客人在 H5 上拍脸照下单，拍前弹告知（用途/保存方式/留存天数/不拍的后果），勾同意才开摄像头——
--   但那个「同意」只在浏览器里勾一下，库里一个字都没有。将来被问「这位客人同意过吗、同意的是哪一版告知」，
--   拿不出任何证据。人脸是生物识别信息，同意必须留痕。
--
-- 落法：不新建第二套同意台账，用既有 customer_consents（V009）——同一个客人的隐私/营销同意都在那张表，
--   人脸照同意也进同一张表（consent_type='ticket_face_photo'，version 记当时那一版告知）。
--   本迁移只把「照片」和「那条同意」挂上钩：ticket_face_photos.consent_id。
--
-- 为什么加 CHECK 而不是只在代码里判：客人端下单是唯一「客人自己的脸」进系统的入口，
--   有照没同意 = 直接的合规事故。写在库上，任何代码路径（含将来新写的）都绕不过去。
--   不加外键：同意行是永久留痕（不删），照片行到期由清理作业硬删；外键会把「客人注销硬删」卡在照片行上。

-- 订单暂存位（客人自己拍的）已存在的行没有同意 id，直接清掉：暂存照本就是临时数据、
--   出票即删/到期即删，开发库不做回填。清完再上约束。
DELETE FROM ticket_face_photos WHERE order_id IS NOT NULL;

ALTER TABLE ticket_face_photos
    ADD COLUMN IF NOT EXISTS consent_id VARCHAR(64);

COMMENT ON COLUMN ticket_face_photos.consent_id IS
    '这张脸照对应的同意留痕 id(customer_consents.id);客人端下单暂存位必填(chk_tfp_consent_present),柜台现场拍的权益位为空';

-- 客人端暂存位（order_id 非空）必须挂同意；柜台现场拍的权益位（entitlement_id 非空、order_id 空）不受影响。
ALTER TABLE ticket_face_photos DROP CONSTRAINT IF EXISTS chk_tfp_consent_present;
ALTER TABLE ticket_face_photos
    ADD CONSTRAINT chk_tfp_consent_present
    CHECK (order_id IS NULL OR consent_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_tfp_consent ON ticket_face_photos (consent_id);

-- ========== ② 超池特批期限：放开「月」这个下限（用户 2026-07-28 纠正）==========
-- 用户原话：「不是指校验大于等于一个月，也可以三天」。V357 把期限做成了「月」为单位 + CHECK ≥1 个月，
--   运营想配一场三天的短期激励根本配不出来。
--
-- 改法：期限拆成「数值 + 单位」两列，单位 ∈ {day, month}。运营在同一个输入框里填 3，旁边选「天」或「月」——
--   配三天和配三个月是同一个动作，不用心算 90。
-- 为什么不直接改成「天」一个单位：业务 HTML 逐字口径是「期限可配，默认不超过 3 个月」。
--   改成天就得把默认写成 90 天，① 与 HTML 措辞对不上，② 3 个月 ≠ 90 天（1月31日+3月=4月30日，+90天=5月1日），
--   等于顺手改了一条已生效的到期口径。留「月」这个单位，默认值一字不动。
-- 上限：仍然没有（HTML 的「3 个月」是默认值不是上限，用户上一轮已确认）。

ALTER TABLE ticket_distribution_configs
    RENAME COLUMN special_approval_term_months TO special_approval_term_value;

ALTER TABLE ticket_distribution_configs
    ADD COLUMN IF NOT EXISTS special_approval_term_unit VARCHAR(8) NOT NULL DEFAULT 'month';

COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_value IS
    '超池特批有效期数值,配合 special_approval_term_unit;特批当日 + N 天/月 = 到期日;出厂默认 3(单位 month)';
COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_unit IS
    '超池特批有效期单位:day=天(短期激励可配三天)/month=月;出厂默认 month(业务口径「期限可配,默认不超过 3 个月」)';

-- 下限只保留「至少 1 个单位」（1 天 / 1 个月），不再有「至少 1 个月」这条错约束。
ALTER TABLE ticket_distribution_configs DROP CONSTRAINT IF EXISTS chk_tdc_sa_term;
ALTER TABLE ticket_distribution_configs
    ADD CONSTRAINT chk_tdc_sa_term CHECK (special_approval_term_value >= 1);

ALTER TABLE ticket_distribution_configs DROP CONSTRAINT IF EXISTS chk_tdc_sa_term_unit;
ALTER TABLE ticket_distribution_configs
    ADD CONSTRAINT chk_tdc_sa_term_unit CHECK (special_approval_term_unit IN ('day', 'month'));
