package com.complex.platform.parking;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** HP-P01 停车场真库回归共用：项目、角色账号、本项目停车参数、停车系统开放平台端点与签名（与 OpenPlatformCrypto 同算法）。 */
final class ParkingFixture {

    /** @SpringBootTest 属性里登记的端点密钥（config: 前缀经 OpenPlatformSecretResolver 取属性）。 */
    static final String SECRET_PROPERTY = "parking.test.open-secret=pk-test-secret";
    private static final String SECRET = "pk-test-secret";
    private static final ObjectMapper SORTED = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private static final String MAPPING = """
            {"session_ref":"session_ref","plate_no":"plate_no","temp_code":"temp_code","vehicle_kind":"vehicle_kind",
             "entered_at":"entered_at","exited_at":"exited_at","fee_cent":"fee_cent","paid_cent":"paid_cent",
             "payment_ref":"payment_ref","amount_cent":"amount_cent","channel":"channel","paid_at":"paid_at",
             "platform_payment_id":"platform_payment_id","reported_at":"reported_at","total_spaces":"total_spaces",
             "occupied_total":"occupied_total","occupied_monthly":"occupied_monthly","occupied_staff":"occupied_staff","kind":"kind"}""";

    final JdbcTemplate jdbc;
    final String suffix = UUID.randomUUID().toString().substring(0, 8);
    final String project;
    final String endpoint;
    final String provider;
    final String appKey;

    ParkingFixture(JdbcTemplate jdbc, String tag) {
        this.jdbc = jdbc;
        this.project = tag + "_prj_" + suffix;
        this.endpoint = tag + "_ep_" + suffix;
        this.provider = tag + "_pv_" + suffix;
        this.appKey = tag + "_app_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','停车回归项目','active',now(),now())",
                project);
    }

    String role(String tag, String... permissions) {
        String role = "pk_role_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'停车回归','project','active',now())",
                role, role);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        return role;
    }

    /** 本项目员工（merchant 为空）或商户员工账号。 */
    RequestPostProcessor account(String tag, String role, String merchantId) {
        return account(tag, role, merchantId, project);
    }

    RequestPostProcessor account(String tag, String role, String merchantId, String organization) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, merchantId == null ? "staff" : "merchant_staff", organization, merchantId);
        if (role != null) {
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        }
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organization);
        return as(userId, merchantId == null ? "staff" : "merchant_staff", organization, merchantId);
    }

    static RequestPostProcessor as(String userId, String accountType, String organization, String merchantId) {
        AccessActor actor = new AccessActor(userId, userId, accountType, organization, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    String user(String tag) {
        return "pk_" + tag + "_" + suffix;
    }

    /** 本项目停车参数：免费 30 分、5 元/时、每日封顶 30 元、离场宽限 15 分、抵免门槛 200 元、抵免 120 分、员工优惠 50%。 */
    void policy() {
        jdbc.update("""
                INSERT INTO parking_policies (project_id, free_minutes, free_minutes_min, free_minutes_max, hourly_rate_cent,
                    hourly_rate_cent_min, hourly_rate_cent_max, daily_cap_cent, daily_cap_cent_min, daily_cap_cent_max,
                    exit_grace_minutes, exit_grace_minutes_min, exit_grace_minutes_max, credit_threshold_cent,
                    credit_threshold_cent_min, credit_threshold_cent_max, credit_minutes, credit_minutes_min, credit_minutes_max,
                    staff_discount_bp, staff_discount_bp_min, staff_discount_bp_max)
                VALUES (?,30,0,120,500,0,2000,3000,0,10000,15,0,60,20000,0,100000,120,0,480,5000,0,10000)""", project);
    }

    /** 停车系统开放平台端点（绑定本项目）、停车适配器（标准字段同名映射）与读取用应用凭证。 */
    void vendor(boolean bound) {
        String secretRef = "config:" + SECRET_PROPERTY.substring(0, SECRET_PROPERTY.indexOf('='));
        jdbc.update("""
                INSERT INTO open_endpoint_configs (id, endpoint_key, provider_code, secret_ref, status, webhook_enabled, created_at, updated_at)
                VALUES (?,?,?,?,'active',TRUE,now(),now())""", "oec_" + endpoint, endpoint, provider, secretRef);
        jdbc.update("""
                INSERT INTO integration_provider_adapters (id, provider_code, adapter_type, mode, environment, status, webhook_enabled,
                    field_mapping_json, created_at, updated_at)
                VALUES (?,?,'parking','mock','mock','mock_ready',TRUE,?::jsonb,now(),now())""", "ipa_" + endpoint, provider, MAPPING);
        jdbc.update("""
                INSERT INTO open_app_credentials (id, app_key, secret_ref, status, allowed_endpoint_keys, created_at, updated_at)
                VALUES (?,?,?,'active',?::jsonb,now(),now())""", "oac_" + endpoint, appKey, secretRef, "[\"" + endpoint + "\"]");
        if (bound) {
            jdbc.update("INSERT INTO parking_vendor_bindings (endpoint_key, project_id) VALUES (?,?)", endpoint, project);
        }
    }

    /** 停车系统回写（签名同开放平台回调）。 */
    MockHttpServletRequestBuilder webhook(String event, String idempotencyKey, Map<String, Object> payload) throws Exception {
        String body = SORTED.writeValueAsString(new TreeMap<>(payload));
        long ts = System.currentTimeMillis();
        String nonce = "n_" + UUID.randomUUID();
        String canonical = "endpoint_key=" + endpoint + "\nevent=" + event + "\nidempotency_key=" + idempotencyKey + "\nnonce=" + nonce
                + "\npayload_hash=" + sha256(body) + "\nprovider=" + provider + "\ntimestamp=" + ts;
        return post("/api/open/d/" + endpoint + "/webhooks/" + provider + "/" + event)
                .header("X-Webhook-Signature", hmac(canonical)).header("X-Open-Timestamp", String.valueOf(ts))
                .header("X-Open-Nonce", nonce).header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** 停车系统按开放平台签名读取。 */
    MockHttpServletRequestBuilder openGet(String path) {
        return openGet(appKey, path);
    }

    MockHttpServletRequestBuilder openGet(String appKey, String path) {
        long ts = System.currentTimeMillis();
        String nonce = "n_" + UUID.randomUUID();
        return get("/api/open/d/" + endpoint + path).header("X-Open-App-Key", appKey).header("X-Open-Timestamp", String.valueOf(ts))
                .header("X-Open-Nonce", nonce).header("X-Open-Signature", hmac("app_key=" + appKey + "\nnonce=" + nonce + "\ntimestamp=" + ts));
    }

    String session(String ref, String plate, String enteredAt) {
        String id = "ps_" + ref;
        jdbc.update("INSERT INTO parking_sessions (id, project_id, vendor_session_ref, plate_no, entered_at) VALUES (?,?,?,?,?::timestamptz)",
                id, project, ref, plate, enteredAt);
        return id;
    }

    // ── HP-P01 第二组：消费抵免造数 ──────────────────────────────

    /** 客人与客人端登录令牌：{customerId, token}。 */
    String[] customer(com.complex.platform.customer.CustomerRepository customers, String tag) {
        java.time.Instant now = java.time.Instant.now();
        String id = "pk_cus_" + tag + "_" + suffix;
        String phone = "139" + String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000));
        customers.saveCustomer(new com.complex.platform.customer.CustomerRecord(id, phone, "139****" + phone.substring(7), "active", null,
                null, now, now));
        String token = "pk_tok_" + tag + "_" + suffix;
        customers.saveSession(new com.complex.platform.customer.CustomerSessionRecord("pk_sess_" + tag + "_" + suffix, id, token, "miniapp",
                now, now.plusSeconds(3600), "active"));
        return new String[] {id, token};
    }

    /** 本项目唯一有效的城市项目公司（停车费收款主体）。 */
    String company() {
        String company = "pk_cpc_" + suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'城市项目公司','project_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        return company;
    }

    /** 本项目商户及其合同主体：{merchantId, entityId}。 */
    String[] merchant(String tag, String type) {
        String entity = "pk_be_" + tag + "_" + suffix;
        String merchant = "pk_m_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,?,'platform','active',now(),now())",
                entity, "合同主体" + tag);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,contract_entity_id,created_at,updated_at) VALUES(?,?,?,'active',?,?,now(),now())",
                merchant, "商户" + tag, type, project, entity);
        return new String[] {merchant, entity};
    }

    /** 已结清的酒吧账单：下单客人 orderCustomer（可空），下单时点 orderedAt；member 非空时另记收银识别的会员。 */
    String barBill(String merchant, String orderCustomer, String member, long amountCent, java.time.Instant orderedAt) {
        String k = UUID.randomUUID().toString().substring(0, 8);
        String table = "pk_tbl_" + k, order = "pk_ord_" + k, bill = "pk_bill_" + k;
        java.sql.Timestamp at = java.sql.Timestamp.from(orderedAt);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,status,created_at,updated_at) VALUES(?,?,?,?,'停车回归桌','card_seat','active',now(),now())",
                table, merchant, merchant, "T" + k);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,customer_id,order_source,status,total_amount_cent,created_at,updated_at,paid_at) VALUES(?,?,?,?,?,'staff','completed',?,?,?,?)",
                order, merchant, merchant, table, orderCustomer, amountCent, at, at, at);
        jdbc.update("""
                INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,payable_amount_cent,paid_amount_cent,
                    payment_status,settlement_status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,'paid','settled',?,?)""",
                bill, "PK-" + k, order, merchant, merchant, table, amountCent, amountCent, amountCent, at, at);
        if (member != null) {
            jdbc.update("INSERT INTO merchant_consumption_members(source_type,source_id,customer_id,identified_at) VALUES('bar_bill',?,?,now())",
                    bill, member);
        }
        return bill;
    }

    /** 已完成、收款时识别会员的专项服务单。 */
    String serviceOrder(String merchant, String member, long amountCent, java.time.Instant orderedAt) {
        String id = "pk_svc_" + UUID.randomUUID().toString().substring(0, 8);
        java.sql.Timestamp at = java.sql.Timestamp.from(orderedAt);
        jdbc.update("""
                INSERT INTO service_orders(id,catalog_item_id,item_name,price_cent,bar_id,performer_user_id,status,created_at,updated_at,completed_at,paid_at,project_id)
                VALUES(?,'pk_cat','停车回归服务',?,?,'pk_performer','completed',?,?,?,?,?)""", id, amountCent, merchant, at, at, at, at, project);
        jdbc.update("INSERT INTO merchant_consumption_members(source_type,source_id,customer_id,identified_at) VALUES('service_order',?,?,now())",
                id, member);
        return id;
    }

    /** 城市项目公司的停车抵免每小时内部结算价（目录生效条目，提供方为经营主体）。 */
    void hourPrice(String entity, long priceCent) {
        jdbc.update("""
                INSERT INTO internal_price_catalog(id,item_code,provider_entity_id,category,item_name,price_cent,status,is_draft,updated_by)
                VALUES(?,?,?,'parking_credit','停车抵免（每小时）',?,'active',FALSE,'pk_fixture')""",
                "ipc_pk_" + suffix, "sku_pk_" + suffix, entity, priceCent);
    }

    private static String hmac(String canonical) throws IllegalStateException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
