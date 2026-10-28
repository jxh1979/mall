package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.complex.platform.access.AccessActor;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第 75 条试算与短期经营激励用例的公共造数（调用方 @Transactional 回滚，不清理）。
 *
 * <p>本项目配置：通路费 wechat 30bps、alipay 60bps（最高 60）；销售渠道费第三方通用 1000bps、meituan 每张票固定 1000 分；
 * 套票折扣项目承担（platform_absorb）。单人单次通票 {@link #singlePass}：酒吧入场 6000、游乐入场 4000（票面原价），
 * 日历价工作日 10000、周末 12000，会员价 9000，已发布动态规则 −20%、最低 7500；综合服务费 10%（月封顶 1 分，试算不套）；
 * 赠送权益按票 300。最低实付价 7500 → 酒吧份额 4500＋项目承担折扣 1500＝6000；通路费按 60bps：45×4500÷7500＝27；
 * 销售渠道费取 meituan 固定额 1000×4500÷7500＝600（大于比例 450）；赠送 300；服务费 600 → 最低份池 4473。
 */
final class PoolTrialFixture {

    static final long SINGLE_PASS_LOWEST_POOL = 4473L;

    private final JdbcTemplate jdbc;
    private final String suffix;
    final String project;
    private int seq;

    PoolTrialFixture(JdbcTemplate jdbc, String suffix) {
        this.jdbc = jdbc;
        this.suffix = suffix;
        this.project = "ptr_prj_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','试算项目','active',now(),now())",
                project);
        channelFee("route", "wechat", 30, null);
        channelFee("route", "alipay", 60, null);
        channelFee("sales", null, 1000, null);
        channelFee("sales", "meituan", 0, 1000L);
        jdbc.update("""
                INSERT INTO ticket_combo_discount_configs (id, project_id, discount_bearer, split_platform_bps, status, is_draft)
                VALUES (?, ?, 'platform_absorb', 10000, 'active', FALSE)""", "tcdc_ptr_" + suffix, project);
    }

    String bar(String label) {
        return merchant(label, "bar");
    }

    String merchant(String label, String type) {
        String id = "ptr_m_" + label + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,?,?,?,'active',now(),now())", id, id, type, project);
        return id;
    }

    /** 单人单次通票（见类注释），最低份池 4473。 */
    String singlePass(String bar) {
        String type = ticketType(10000L, 12000L);
        bind(type, "entry", bar, 6000L, 1);
        bind(type, "amusement_entry", merchant("park" + seq, "amusement"), 4000L, 1);
        jdbc.update("""
                INSERT INTO ticket_price_rules (id, ticket_type_id, variant_type, price_mode, weekday_price_cent, status, created_at, updated_at)
                VALUES (?, ?, 'member', 'fixed_price', 9000, 'active', now(), now())""", "tpr_ptr_" + type, type);
        jdbc.update("""
                INSERT INTO ticket_dynamic_pricing_rules (id, ticket_type_id, rule_name, priority, effective_from, effective_to,
                    adjustment_type, adjustment_rate, min_price_cent, status, created_at, updated_at)
                VALUES (?, ?, '淡季下调', 0, ?, ?, 'percent_delta', -20, 7500, 'published', now(), now())""",
                "tdp_ptr_" + type, type, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                java.sql.Date.valueOf(LocalDate.now().plusDays(60)));
        serviceFeeAndGift(type, 300L);
        return type;
    }

    /**
     * 两人单次通票：酒吧 a、b 入场各 3000、游乐 4000（均每份 2 人），日历价 10000，无价格规则；赠送权益按票 301。
     * 酒吧份额 6000、通路费 36、销售渠道费 600、赠送 301、服务费 600 → 单张池 4463，两份按最大余数均分 2232／2231，最不利一份 2231。
     */
    String twoPersonPass(String barA, String barB) {
        String type = ticketType(10000L, 10000L);
        bind(type, "entry", barA, 3000L, 2);
        bind(type, "entry", barB, 3000L, 2);
        bind(type, "amusement_entry", merchant("park" + seq, "amusement"), 4000L, 2);
        serviceFeeAndGift(type, 301L);
        return type;
    }

    /** 平台账号（不属任何商户）带给定权限码，按后台网页请求形状（X-Terminal-Type: web）。 */
    RequestPostProcessor actor(String user, String... permissions) {
        String role = "ptr_role_" + user;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'试算','project','active',now())",
                role, role);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin','org','active',now())",
                user, user);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "platform_admin", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String ticketType(long weekday, long weekend) {
        String type = "ptr_tt_" + (++seq) + "_" + suffix;
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, weekday_price_cent,
                    weekend_price_cent, created_at, updated_at)
                VALUES (?, ?, 'fixed_price', FALSE, 'published', ?, ?, now(), now())""", type, "试算通票" + seq, weekday, weekend);
        return type;
    }

    private void bind(String type, String entitlementType, String merchant, long componentPrice, int people) {
        String template = "ptr_et_" + (++seq) + "_" + suffix;
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    merchant_id, created_at, updated_at) VALUES (?, ?, ?, 1, 0, 'active', ?, now(), now())""",
                template, entitlementType, entitlementType, merchant);
        jdbc.update("""
                INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times, num_people,
                    value_amount_cent, component_price_cent, created_at, updated_at)
                VALUES (?, ?, ?, 1, 1, ?, 0, ?, now(), now())""", "ptr_b_" + template, type, template, people, componentPrice);
    }

    private void serviceFeeAndGift(String type, long gift) {
        jdbc.update("""
                INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id, fee_bps,
                    monthly_cap_amount_cent, status, is_draft)
                VALUES (?, ?, NULL, ?, 1000, 1, 'active', FALSE)""", "csfc_ptr_" + type, project, type);
        jdbc.update("""
                INSERT INTO pass_gift_cost_configs (id, project_id, ticket_type_id, prededuct_mode, ticket_gift_amount_cent,
                    monthly_cap_amount_cent, status, is_draft, updated_by)
                VALUES (?, ?, ?, 'by_ticket_amount', ?, NULL, 'active', FALSE, 'tester')""", "pgc_ptr_" + type, project, type, gift);
    }

    private void channelFee(String layer, String channel, int bps, Long fixed) {
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft, fee_layer)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE, ?)""", "cfc_ptr_" + (++seq) + "_" + suffix, project, channel, bps, fixed, layer);
    }
}
