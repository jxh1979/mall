// 文件版本：V0.01
package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import java.time.LocalDate;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AN 段第 258 条（PGM:531「管理证照、资质、保险和合同关键日期的到期预警与到期阻断……」）：合规提醒页扫描、补录合同关键日期、
 * 补录证照三处按页面实发的请求体落库。扫描基准日留空按当天、操作人取登录人（BSM:940）；合同关键日期的商户由合同带出。
 * 真库、真实授权链，请求体按前端蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ComplianceManualEntryJdbcIntegrationTests {

    private static final String BASE = "/api/v1/compliance";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final LocalDate today = LocalDate.now(LocalZone.ID);
    private String project;
    private String merchant;
    private StaffRequestFixture staffs;
    private RequestPostProcessor platform;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
        merchant = merchant("a");
        staffs = new StaffRequestFixture(jdbc);
        platform = staffs.staff("cme_p_" + suffix, "platform_admin", project, null, "compliance.reminder.scan",
                "compliance.contract_key_date.manage", "compliance.expiry_item.manage");
    }

    /** 258-R1①③ 页面基准日留空、不带操作人：按当天生成提醒，扫描留痕的操作人是登录账号。 */
    @Test
    void scanWithoutDateOrOperatorUsesTodayAndRecordsLoggedInOperator() throws Exception {
        String keyDate = keyDate(contract(merchant), today.plusDays(10));
        String key = key();
        send(platform, "/reminders/scan", "{\"idempotency_key\":\"" + key + "\",\"default_lookahead_days\":30}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reminder_type || '@' || reminder_at FROM compliance_reminders WHERE source_id = ?",
                String.class, keyDate)).isEqualTo("upcoming@" + today);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM audit_events WHERE event_name = 'compliance.reminder.scan'"
                + " AND idempotency_key = ?", String.class, key)).isEqualTo("cme_p_" + suffix);
    }

    /** 258-R1② 选了基准日仍按所给日期生成。 */
    @Test
    void scanWithDateUsesGivenDate() throws Exception {
        String keyDate = keyDate(contract(merchant), today.plusDays(10));
        send(platform, "/reminders/scan", "{\"idempotency_key\":\"" + key() + "\",\"scan_date\":\"" + today.plusDays(11)
                + "\",\"default_lookahead_days\":30}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reminder_type || '@' || reminder_at FROM compliance_reminders WHERE source_id = ?",
                String.class, keyDate)).isEqualTo("expired@" + today.plusDays(11));
    }

    /** 258-R2① 页面请求体不带商户：商户由合同带出，责任人照请求体入库。 */
    @Test
    void contractKeyDateTakesMerchantFromContract() throws Exception {
        String contract = contract(merchant);
        createKeyDate(platform, contract, "renewal_notice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(merchant));
        assertThat(jdbc.queryForObject("SELECT merchant_id || '|' || owner_user_id || '|' || date_type FROM contract_key_dates"
                + " WHERE contract_id = ?", String.class, contract))
                .isEqualTo(merchant + "|cme_p_" + suffix + "|renewal_notice");
    }

    /** 258-R2②③④ 合同编号填错 400「关联合同不存在」；商户账号用别家商户的合同 403；旧页面的日期类型 renewal 仍 400（防回流）。 */
    @Test
    void contractKeyDateRejections() throws Exception {
        createKeyDate(platform, "ct_missing_" + suffix, "renewal_notice")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("关联合同不存在")));
        RequestPostProcessor merchantB = staffs.staff("cme_m_" + suffix, "staff", project, merchant("b"),
                "compliance.contract_key_date.manage");
        createKeyDate(merchantB, contract(merchant), "renewal_notice")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        createKeyDate(platform, contract(merchant), "renewal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /** 258-R3 页面补录证照的新请求体（主体即所选商户、责任人为登录人）能落库。 */
    @Test
    void expiryItemFromPageShapePersistsMerchantAsSubject() throws Exception {
        send(platform, "/expiry-items", "{\"idempotency_key\":\"" + key() + "\",\"subject_type\":\"merchant\",\"subject_id\":\""
                + merchant + "\",\"merchant_id\":\"" + merchant + "\",\"item_type\":\"business_license\",\"item_name\":\"营业执照\","
                + "\"document_no\":\"LIC-9\",\"valid_to\":\"" + today.plusYears(1) + "\",\"source_type\":\"manual\","
                + "\"human_confirmed\":true,\"owner_user_id\":\"cme_p_" + suffix + "\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT subject_id || '|' || owner_user_id FROM compliance_expiry_items WHERE merchant_id = ?",
                String.class, merchant)).isEqualTo(merchant + "|cme_p_" + suffix);
    }

    private ResultActions createKeyDate(RequestPostProcessor who, String contract, String dateType) throws Exception {
        return send(who, "/contract-key-dates", "{\"idempotency_key\":\"" + key() + "\",\"contract_id\":\"" + contract
                + "\",\"title\":\"续约通知\",\"date_type\":\"" + dateType + "\",\"due_date\":\"" + today.plusDays(60)
                + "\",\"source_type\":\"manual\",\"human_confirmed\":true,\"owner_user_id\":\"cme_p_" + suffix + "\"}");
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(BASE + path).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String merchant(String tag) {
        String id = "cme_mer_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,?,'bar',?,'active',now(),now())", id, "合规回归商户" + tag, project);
        return id;
    }

    private String contract(String merchantId) {
        String id = "cme_ct_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2026-01-01', DATE '2028-12-31', 'effective', ?, now(), now())""",
                id, merchantId, merchantId, merchantId, project);
        return id;
    }

    private String keyDate(String contract, LocalDate due) {
        String id = "ckd_cme_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbc.update("""
                INSERT INTO contract_key_dates (id, contract_id, merchant_id, date_type, title, due_date, reminder_days, status,
                    owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at)
                VALUES (?, ?, ?, 'renewal_notice', '续约通知', ?, '[]'::jsonb, 'active', 'owner', 'manual', TRUE, '[]'::jsonb, now(), now())
                """, id, contract, merchant, due);
        return id;
    }

    private String key() {
        return "cme-" + UUID.randomUUID();
    }
}
