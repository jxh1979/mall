-- V374 通票分账瀑布重建 S8·第 1 刀:商户级固定规则显式化(rule_type 二选一)。
--
-- 【业务依据】(最终解释权=业务 HTML,逐字)business-system-map-for-operations §「通票收入分账全流程」:
--   「可分配池按两段分配:第一段,约定为『按每个客人的固定单价』或『按固定金额』的酒吧,
--     先按约定的绝对金额拿走;第二段,池额减去第一段后的余额,由其余按『排名档位』约定的
--     酒吧按档位比例分尽——拿了第一段的酒吧不再参与第二段。」
--
-- 【为什么必须加 rule_type(业务人员角度)】一家酒吧到底按「每客单价」还是「固定额」拿第一段,
--   旧模型靠项目级 ticket_distribution_configs.method_type 三选一统一定——两段化后同一个池里
--   两种固定规则并存,项目级那个消歧来源没了;一行同时配着 per_guest_cent 和 fixed_amount_cent
--   时(开发库实测存在),运营在配置页看不出这家酒吧月底会按哪个数拿钱。显式二选一后,
--   配置页每一行都能直读:「这家 = 每客 5 元」或「这家 = 固定 100 元」,没有第二种解释。
--
-- 【配套语义收窄】ticket_distribution_configs.method_type 收窄为「第二段的分配方式」,
--   现实中仅 by_rank_tier 有意义;by_venue_per_guest / by_fixed_amount 由本表商户级规则取代
--   (配置页已下线这两个选项,写入口拒绝)。CHECK 枚举不收(历史行留读),引擎侧 fail-loud。
--
-- 【存量】开发环境数据最后全删,不为存量做兼容/回填设计;下方 UPDATE 仅为让 NOT NULL + CHECK
--   能在带着 1 行测试数据的开发库上落下去(该行 method_type=by_rank_tier 从未被引擎消费,消歧无损)。

ALTER TABLE ticket_distribution_merchant_rates
    ADD COLUMN IF NOT EXISTS rule_type VARCHAR(16);

-- 现存行显式化:配了 per_guest_cent 的按每客单价;仅配 fixed_amount_cent 的按固定额。
UPDATE ticket_distribution_merchant_rates
   SET rule_type = CASE WHEN per_guest_cent IS NOT NULL THEN 'per_guest' ELSE 'fixed' END
 WHERE rule_type IS NULL;
-- 二选一:另一列清空(同行双值正是本迁移要消灭的歧义)。
UPDATE ticket_distribution_merchant_rates SET fixed_amount_cent = NULL
 WHERE rule_type = 'per_guest' AND fixed_amount_cent IS NOT NULL;
UPDATE ticket_distribution_merchant_rates SET per_guest_cent = NULL
 WHERE rule_type = 'fixed' AND per_guest_cent IS NOT NULL;

ALTER TABLE ticket_distribution_merchant_rates ALTER COLUMN rule_type SET NOT NULL;

-- 二选一 CHECK:rule_type 与两金额列强绑定,配了 A 则 B 必空(打错字/两头都填在落库时就被拦)。
ALTER TABLE ticket_distribution_merchant_rates DROP CONSTRAINT IF EXISTS chk_tdmr_rule;
ALTER TABLE ticket_distribution_merchant_rates ADD CONSTRAINT chk_tdmr_rule CHECK (
       (rule_type = 'per_guest' AND per_guest_cent    IS NOT NULL AND fixed_amount_cent IS NULL)
    OR (rule_type = 'fixed'     AND fixed_amount_cent IS NOT NULL AND per_guest_cent    IS NULL));

COMMENT ON COLUMN ticket_distribution_merchant_rates.rule_type IS
    'S8 两段分配第一段的固定规则类型(二选一):per_guest=按每客单价(per_guest_cent×当月客数)/fixed=按固定额(fixed_amount_cent)。有 active 生效行=该酒吧走第一段(拿了第一段不再参与第二段);无行=走第二段排名档位';

COMMENT ON COLUMN ticket_distribution_configs.method_type IS
    'S8 语义收窄:仅指【第二段】(池额减去第一段后的余额)的分配方式,现实中只有 by_rank_tier(排名档位归一化分尽)有意义;by_venue_per_guest/by_fixed_amount 已由商户级固定规则(ticket_distribution_merchant_rates.rule_type)取代,写入口不再接受';

-- 唯一索引复核(方案 V358 行提到「重建」):现行 uq_tdmr = (scope, merchant_id, is_draft) WHERE status='active'
-- (V329)——键不含规则维度,一商户一行,恰好就是两段消歧需要的「一家酒吧只有一种约定」,无需重建。
