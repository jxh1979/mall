package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.complex.platform.access.AccessActor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * TK-29 传媒逐场演出合同 / 履约 / 结算三组真库回归的共用夹具：项目、酒吧与传媒商户、带角色授权的员工主体（按商户范围）、
 * 前端 adminClient.mapKeys 蛇形键的逐场合同请求体。
 */
final class MediaPerformanceTestFixtures {

    static final String BASE = "/api/v1/settlement/media-shows";
    static final String[] ALL_PERMISSIONS = {
            "media.performance.view", "media.performance.contract.manage", "media.performance.fulfillment.record",
            "media.performance.fulfillment.confirm", "media.performance.dispute.resolve", "media.performance.settle"};

    private MediaPerformanceTestFixtures() {
    }

    static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    static void seedProject(JdbcTemplate jdbc, String projectId) {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','演出回归项目','active',now(),now())",
                projectId);
    }

    static void seedMerchant(JdbcTemplate jdbc, String merchantId, String format, String projectId) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,?,?,?,'active',now(),now())",
                merchantId, merchantId, format, projectId);
    }

    static void seedTicketType(JdbcTemplate jdbc, String ticketTypeId, String barMerchantId) {
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,bar_id,refundable,status,created_at,updated_at) VALUES(?,'演出日酒吧门票','fixed',?,TRUE,'active',now(),now())",
                ticketTypeId, barMerchantId);
    }

    /** 员工主体：account_type=staff、organization_id=项目、merchant_id=所属商户（平台员工传 null），角色显式授予指定权限码。 */
    static RequestPostProcessor staff(JdbcTemplate jdbc, String userId, String projectId, String merchantId, String... permissions) {
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'演出回归','project','active',now())", role, role);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, projectId, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 前端真实请求形状（adminClient.mapKeys 蛇形键）：到酒吧演出，三种情形全填。 */
    static Map<String, Object> barShowBody(String projectId, String bar, String media, String ticketTypeId, String showDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "ms-" + suffix());
        body.put("project_id", projectId);
        body.put("bar_merchant_id", bar);
        body.put("media_merchant_id", media);
        body.put("show_kind", "bar_show");
        body.put("show_date", showDate);
        body.put("planned_start_at", showDate + "T13:00:00Z");
        body.put("planned_end_at", showDate + "T16:00:00Z");
        body.put("contract_ref", "合同-" + showDate);
        body.put("ticket_share_mode", "ratio");
        body.put("ticket_share_bps", 5000);
        body.put("ticket_surcharge_per_ticket_cent", 2000);
        body.put("ticket_type_id", ticketTypeId);
        body.put("fixed_fee_cent", 50000);
        body.put("drink_commission_bps", 1000);
        body.put("drink_base_template", "whole_store_session");
        return body;
    }

    static Map<String, Object> selfHostedBody(String projectId, String media, String showDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "ms-" + suffix());
        body.put("project_id", projectId);
        body.put("media_merchant_id", media);
        body.put("show_kind", "self_hosted");
        body.put("show_date", showDate);
        body.put("contract_ref", "自办-" + showDate);
        body.put("ticket_share_mode", "none");
        return body;
    }

    static Map<String, Object> fulfillmentBody(String showDate, String uncompleted) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mf-" + suffix());
        body.put("actual_start_at", showDate + "T13:10:00Z");
        body.put("actual_end_at", showDate + "T15:50:00Z");
        body.put("completed_content", "乐队 3 组按约演出");
        body.put("uncompleted_content", uncompleted);
        body.put("basis", "现场视频与签到表");
        return body;
    }
}
