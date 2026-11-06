package com.complex.platform.settlement;

// 文件版本：V0.02

import static com.complex.platform.settlement.MediaPerformanceTestFixtures.BASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-29-R3 三种结算真库回归（BSM L1145「该场适用的门票加价分配、固定演出费和酒水提成共同引用该场已确认的实际履约事实，分别按该场已锁定合同的
 * 适用条款结算」「不默认全额、半价或零额结算」「③……计提基数不含通票赠饮和酒吧门票赠饮的酒水」「传媒自办场次……不因本条确认新增酒吧应付」
 * 「已形成结算需要调整时，关联原记录……不覆盖原结果」；BSM L1144 ①收款主体；操作员裁决 2026-10-20「逐场填每张加价×已核销张数」）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MediaPerformanceSettlementJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SHOW_DATE = "2014-03-17";
    private static final String SOURCE = "media_performance_settlement";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MediaPerformanceSettlementService settlementService;

    private final String suffix = MediaPerformanceTestFixtures.suffix();
    private final String project = "mps_prj_" + suffix;
    private final String bar = "mps_bar_" + suffix;
    private final String media = "mps_media_" + suffix;
    private final String ticketType = "mps_tt_" + suffix;
    private final String template = "mps_et_" + suffix;
    private final String table = "mps_tbl_" + suffix;
    private final String product = "mps_prd_" + suffix;
    private int seq;
    private RequestPostProcessor manager;
    private RequestPostProcessor mediaClerk;
    private RequestPostProcessor barOnSite;
    private RequestPostProcessor financeBp;
    /** 159-R3 新用例的演出日：北京时间今天减 3 天（已闭店，可标最终）。 */
    private final LocalDate recentDay = LocalDate.now(LocalZone.ID).minusDays(3);

    @BeforeEach
    void seed() {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, bar, "bar", project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, media, "media", project);
        MediaPerformanceTestFixtures.seedTicketType(jdbc, ticketType, bar);
        jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at) VALUES(?,'演出日入场','bar_entry',1,0,'active',now(),now())", template);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,status,created_at,updated_at) VALUES(?,?,?,'T1','T1','booth','available',now(),now())", table, bar, bar);
        jdbc.update("INSERT INTO bar_products(id,bar_id,product_name,product_type,price_cent,status,created_at,updated_at) VALUES(?,?,'威士忌','spirit',1000,'active',now(),now())", product, bar);
        manager = MediaPerformanceTestFixtures.staff(jdbc, "mps_mgr_" + suffix, project, null,
                "media.performance.contract.manage", "media.performance.settle", "media.performance.view", "media.performance.dispute.resolve");
        mediaClerk = MediaPerformanceTestFixtures.staff(jdbc, "mps_clerk_" + suffix, project, media,
                "media.performance.fulfillment.record");
        barOnSite = MediaPerformanceTestFixtures.staff(jdbc, "mps_site_" + suffix, project, bar,
                "media.performance.fulfillment.confirm");
        // 159-R3：项目与酒吧回填为 30 天前建档（随测试事务回滚），近几天的演出日要查最终标记；既有用例的 2014 演出日仍在建档之前、不查。
        jdbc.update("UPDATE projects SET created_at = now() - interval '30 days' WHERE id = ?", project);
        jdbc.update("UPDATE merchants SET created_at = now() - interval '30 days' WHERE id = ?", bar);
        financeBp = MediaPerformanceTestFixtures.staff(jdbc, "mps_bp_" + suffix, project, null, "finance.daily_close.lock");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 未取得确认：三项各写一行 pending(unconfirmed)，不出任何应收应付（不默认全额/半价/零额）。
    @Test
    void unconfirmed_allFeesPending_noArAp() throws Exception {
        String showId = lockedShow(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        settle(showId);
        List<Map<String, Object>> rows = settlements(showId);
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(r -> r.get("fee_kind")).containsExactlyInAnyOrder("ticket_share", "fixed_fee", "drink_commission");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("status")).isEqualTo("pending");
            assertThat(r.get("pending_reason")).isEqualTo("unconfirmed");
            assertThat(r.get("ar_ap_item_id")).isNull();
        });
        assertThat(arApItems(showId)).isEmpty();
    }

    // ②固定演出费：已确认 → 酒吧→传媒公司应付，金额=合同固定费，往来对象=传媒公司主体、merchant=酒吧、项目=该场项目。
    @Test
    void fixedFee_confirmed_postsPayableBarToMedia() throws Exception {
        Map<String, Object> body = fixedFeeOnly();
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        settle(showId);
        List<Map<String, Object>> rows = settlements(showId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("fee_kind")).isEqualTo("fixed_fee");
        assertThat(rows.get(0).get("status")).isEqualTo("posted");
        assertThat(num(rows.get(0), "amount_cent")).isEqualTo(50000L);
        List<Map<String, Object>> items = arApItems(showId);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("direction")).isEqualTo("payable");
        assertThat(items.get(0).get("counterparty_id")).isEqualTo("be_media");
        assertThat(items.get(0).get("merchant_id")).isEqualTo(bar);
        assertThat(items.get(0).get("project_id")).isEqualTo(project);
        assertThat(num(items.get(0), "amount_cent")).isEqualTo(50000L);
    }

    // ③酒水提成基数：当场全店已出品酒水，剔除赠饮单(order_type=gift)、酒吧门票赠饮(bar_gift_redemptions 关联单)与作废项。
    @Test
    void drinkCommission_excludesGiftOrders_andVoidedItems() throws Exception {
        Map<String, Object> body = drinkOnly("whole_store_session");
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        Instant at = Instant.parse(SHOW_DATE + "T10:00:00Z");
        order("standard", at, 10000L, "delivered");
        order("gift", at, 3000L, "delivered");
        String giftRedeemed = order("standard", at, 5000L, "delivered");
        jdbc.update("INSERT INTO bar_gift_redemptions(id,entitlement_id,verification_id,bar_order_id,bar_id,merchant_id,status,created_at,updated_at) VALUES(?,?,?,?,?,?,'delivered',now(),now())",
                "bgr_" + suffix, "ent_" + suffix, "vf_" + suffix, giftRedeemed, bar, bar);
        order("standard", at, 2000L, "voided");
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(row.get("fee_kind")).isEqualTo("drink_commission");
        assertThat(num(row, "base_amount_cent")).as("基数只含普通单已出品未作废酒水").isEqualTo(10000L);
        assertThat(num(row, "amount_cent")).isEqualTo(1000L);
        assertThat(arApItems(showId)).extracting(r -> num(r, "amount_cent")).containsExactly(1000L);
    }

    // ③按演出时段模板：只取已确认实际演出时段内的酒水。
    @Test
    void drinkCommission_showWindowTemplate_onlyOrdersInsideActualWindow() throws Exception {
        String showId = lockedShow(drinkOnly("show_window"));
        recordAndConfirm(showId, null); // 实际时段 13:10Z–15:50Z
        order("standard", Instant.parse(SHOW_DATE + "T14:00:00Z"), 10000L, "delivered");
        order("standard", Instant.parse(SHOW_DATE + "T10:00:00Z"), 7000L, "delivered");
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(num(row, "base_amount_cent")).isEqualTo(10000L);
        assertThat(num(row, "amount_cent")).isEqualTo(1000L);
    }

    // ①门票加价分成：基数＝该票种当场已核销入场张数×每张加价；未核销的不分传媒；出账为娱乐平台公司→传媒公司（不挂酒吧）。
    @Test
    void ticketShare_usesVerifiedCountTimesSurcharge_notUnverified() throws Exception {
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE);
        body.remove("fixed_fee_cent");
        body.remove("drink_commission_bps");
        body.remove("drink_base_template");
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        verifiedTicket(Instant.parse(SHOW_DATE + "T12:30:00Z"), true);
        verifiedTicket(Instant.parse(SHOW_DATE + "T12:40:00Z"), true);
        verifiedTicket(Instant.parse(SHOW_DATE + "T12:50:00Z"), true);
        verifiedTicket(Instant.parse(SHOW_DATE + "T12:55:00Z"), false);
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(row.get("fee_kind")).isEqualTo("ticket_share");
        assertThat(num(row, "base_amount_cent")).as("3 张已核销 × 2000").isEqualTo(6000L);
        assertThat(num(row, "amount_cent")).as("按合同 50%").isEqualTo(3000L);
        List<Map<String, Object>> items = arApItems(showId);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("counterparty_id")).isEqualTo("be_media");
        assertThat(items.get(0).get("merchant_id")).as("娱乐平台公司付，不挂酒吧").isNull();
        assertThat(row.get("payer_entity_id")).isEqualTo("be_ent_platform");
    }

    // ①按金额分成：直接按约定额出账，不按已核销加价基数封顶（操作员裁决 2026-10-20 第二轮第 4 条）。
    @Test
    void ticketShare_amountMode_postsAgreedAmountWithoutCap() throws Exception {
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE);
        body.remove("fixed_fee_cent");
        body.remove("drink_commission_bps");
        body.remove("drink_base_template");
        body.remove("ticket_share_bps");
        body.put("ticket_share_mode", "fixed");
        body.put("ticket_share_amount_cent", 10000);
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        verifiedTicket(Instant.parse(SHOW_DATE + "T12:30:00Z"), true);
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(num(row, "base_amount_cent")).as("1 张已核销 × 2000").isEqualTo(2000L);
        assertThat(num(row, "amount_cent")).as("按约定额出账，不封顶到基数").isEqualTo(10000L);
        assertThat(arApItems(showId)).extracting(r -> num(r, "amount_cent")).containsExactly(10000L);
    }

    // 「当场」＝演出日所在酒吧营业日（bar 域切日，ENUM L187）：次日凌晨（切日前）的核销/点单归本场；演出日凌晨（切日前）的归前一营业日。
    @Test
    void sameSession_usesBarBusinessDay_acrossMidnight() throws Exception {
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE);
        body.remove("fixed_fee_cent");
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        Instant evening = Instant.parse(SHOW_DATE + "T12:30:00Z");          // 演出日 20:30（北京）
        Instant afterMidnight = Instant.parse(SHOW_DATE + "T17:30:00Z");    // 次日 01:30（北京），切日前 → 本营业日
        Instant showDateDawn = Instant.parse("2014-03-16T19:00:00Z");       // 演出日 03:00（北京），切日前 → 前一营业日
        verifiedTicket(evening, true);
        verifiedTicket(afterMidnight, true);
        verifiedTicket(afterMidnight.plusSeconds(60), true);
        verifiedTicket(showDateDawn, true);
        order("standard", evening, 10000L, "delivered");
        order("standard", afterMidnight, 4000L, "delivered");
        order("standard", showDateDawn, 2000L, "delivered");
        settle(showId);
        Map<String, Map<String, Object>> byKind = new java.util.HashMap<>();
        settlements(showId).forEach(r -> byKind.put((String) r.get("fee_kind"), r));
        assertThat(num(byKind.get("ticket_share"), "base_amount_cent")).as("3 张（含次日凌晨 2 张、不含演出日凌晨 1 张）× 2000")
                .isEqualTo(6000L);
        assertThat(num(byKind.get("drink_commission"), "base_amount_cent")).as("10000 + 次日凌晨 4000，不含演出日凌晨 2000")
                .isEqualTo(14000L);
    }

    // 自办场次：不生成任何酒吧应付、不生成结算行。
    @Test
    void selfHosted_neverCreatesBarPayable() throws Exception {
        String showId = lockedShow(MediaPerformanceTestFixtures.selfHostedBody(project, media, SHOW_DATE));
        settle(showId);
        assertThat(settlements(showId)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE merchant_id = ? OR counterparty_id = ?", Integer.class, bar, bar)).isZero();
    }

    // 部分完成（未完成内容非空）且合同无部分履约条款 → 固定费 pending(contract_silent)，不按半价、不出账。
    @Test
    void partialCompletion_contractSilent_pending() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        recordAndConfirm(showId, "第三组乐队未到场");
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(row.get("status")).isEqualTo("pending");
        assertThat(row.get("pending_reason")).isEqualTo("contract_silent");
        assertThat(arApItems(showId)).isEmpty();
    }

    // 部分完成经争议处理流程（提出争议 → 主管裁决留痕）明确后再办理：固定费按锁定合同出账，裁决额不同由调整行处理。
    @Test
    void partialCompletion_afterDisputeRuling_postsFixedFee() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment").with(mediaClerk).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(MediaPerformanceTestFixtures.fulfillmentBody(SHOW_DATE, "第三组乐队未到场"))))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/dispute").with(barOnSite).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dp-" + showId + "\",\"dispute_reason\":\"只演出两组\"}"))
                .andExpect(status().isOk());
        settle(showId);
        assertThat(settlements(showId).get(0).get("pending_reason")).as("争议中保持待处理").isEqualTo("disputed");
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/dispute/resolve").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"rs-" + showId + "\",\"resolution_note\":\"按合同固定费结算\"}"))
                .andExpect(status().isOk());
        settle(showId);
        Map<String, Object> row = settlements(showId).get(0);
        assertThat(row.get("status")).isEqualTo("posted");
        assertThat(num(row, "amount_cent")).isEqualTo(50000L);
        assertThat(arApItems(showId)).hasSize(1);
    }

    // 调整：原行保留（金额不改、状态 adjusted），新行回指原行；原应付按 V344 红冲取反、新应付另挂。
    @Test
    void adjust_keepsOriginalRow_linksNew_andReversesArAp() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        recordAndConfirm(showId, null);
        settle(showId);
        Map<String, Object> original = settlements(showId).get(0);
        String originalId = (String) original.get("id");
        String originalItem = (String) original.get("ar_ap_item_id");
        mockMvc.perform(post(BASE + "/" + showId + "/settlements/" + originalId + "/adjust").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adj-" + suffix + "\",\"amount_cent\":30000,\"adjust_reason\":\"裁决按两组结算\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjusts_settlement_id").value(originalId))
                .andExpect(jsonPath("$.data.amount_cent").value(30000));
        Map<String, Object> originalAfter = jdbc.queryForMap("SELECT status, amount_cent, ar_ap_item_id FROM media_performance_settlements WHERE id = ?", originalId);
        assertThat(originalAfter.get("status")).isEqualTo("adjusted");
        assertThat(num(originalAfter, "amount_cent")).as("原行金额不覆盖").isEqualTo(50000L);
        assertThat(originalAfter.get("ar_ap_item_id")).isEqualTo(originalItem);
        List<Map<String, Object>> live = settlements(showId).stream().filter(r -> !"adjusted".equals(r.get("status"))).toList();
        assertThat(live).hasSize(1);
        assertThat(live.get(0).get("adjusts_settlement_id")).isEqualTo(originalId);
        assertThat(num(live.get(0), "amount_cent")).isEqualTo(30000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE origin_item_id = ?", Long.class, originalItem))
                .as("原应付红冲取反").isEqualTo(-50000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id = ?", Long.class, (String) live.get(0).get("ar_ap_item_id")))
                .isEqualTo(30000L);
    }

    // 重跑结算幂等：同一场同一费项只挂一笔应付。
    @Test
    void settleShow_idempotentReplay_singleArAp() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        recordAndConfirm(showId, null);
        settle(showId);
        settle(showId);
        assertThat(settlements(showId)).hasSize(1);
        assertThat(arApItems(showId)).hasSize(1);
    }

    // 复核 authz B1（PGM L659 项目 / 商户范围）：他项目持结算权限码的员工结算 / 调整本场 → 403，不写结算行、不动原行。
    @Test
    void settleAndAdjust_crossProjectStaff_forbidden() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        recordAndConfirm(showId, null);
        settle(showId);
        String originalId = (String) settlements(showId).get(0).get("id");
        String otherProject = "mps_prj2_" + suffix;
        MediaPerformanceTestFixtures.seedProject(jdbc, otherProject);
        RequestPostProcessor outsider = MediaPerformanceTestFixtures.staff(jdbc, "mps_out_" + suffix, otherProject, null,
                "media.performance.settle");
        mockMvc.perform(post(BASE + "/" + showId + "/settle").with(outsider).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"st-out-" + suffix + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + showId + "/settlements/" + originalId + "/adjust").with(outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adj-out-" + suffix + "\",\"amount_cent\":1,\"adjust_reason\":\"越权\"}"))
                .andExpect(status().isForbidden());
        assertThat(settlements(showId)).hasSize(1);
        assertThat(settlements(showId).get(0).get("status")).isEqualTo("posted");
    }

    // 复核 money m5 / authz I1（PGM L303 幂等）：调整同一请求编号同一正文重放返回第一次的结果，不再 409、不多挂调整行。
    @Test
    void adjust_sameKeyReplay_returnsFirstResult() throws Exception {
        String showId = lockedShow(fixedFeeOnly());
        recordAndConfirm(showId, null);
        settle(showId);
        String originalId = (String) settlements(showId).get(0).get("id");
        String body = "{\"idempotency_key\":\"adj-rp-" + suffix + "\",\"amount_cent\":30000,\"adjust_reason\":\"裁决按两组结算\"}";
        String first = mockMvc.perform(post(BASE + "/" + showId + "/settlements/" + originalId + "/adjust").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post(BASE + "/" + showId + "/settlements/" + originalId + "/adjust").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JSON.readTree(second).path("data").path("id").asText())
                .isEqualTo(JSON.readTree(first).path("data").path("id").asText());
        assertThat(settlements(showId)).hasSize(2);
    }

    // 复核 money I5（BSM L1145「已形成结算需要调整时，关联原记录……不覆盖原结果」）：该场已有出账结算后不得新建合同版本，引导走调整。
    @Test
    void createVersion_afterPostedSettlement_rejected() throws Exception {
        Map<String, Object> body = fixedFeeOnly();
        String showId = lockedShow(body);
        recordAndConfirm(showId, null);
        settle(showId);
        body.put("idempotency_key", "ver-" + suffix);
        body.put("fixed_fee_cent", 80000);
        mockMvc.perform(post(BASE + "/" + showId + "/versions").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_SETTLED_USE_ADJUST"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE show_group_id = (SELECT show_group_id FROM media_performance_shows WHERE id = ?)",
                Integer.class, showId)).isEqualTo(1);
    }

    // 第十八批 V 段第 159 条 159-R3（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」）：酒水提成要演出日营业日该酒吧的
    // 订单与核销、退款与冲正、出品与物品交接已标记为最终；出品未标 → 409、一行不写、不挂应付；标齐后同一接口换幂等键结算出账。
    @Test
    void drinkCommission_waitsUntilShowDayDataFinal() throws Exception {
        String date = recentDay.toString();
        String showId = lockedShow(drinkOnly("whole_store_session", date));
        recordAndConfirm(showId, null, date);
        order("standard", Instant.parse(date + "T13:00:00Z"), 10000L, "delivered"); // 演出日 21:00（北京）
        String closeId = dailyCloseMarked(recentDay, "orders_verifications", "refunds_reversals", "attendance", "anomaly_resolutions");
        settleCall(showId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SETTLEMENT_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(containsString("当日出品与物品交接记录")));
        assertThat(settlements(showId)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = ? AND merchant_id = ?", Integer.class,
                SOURCE, bar)).isZero();

        finalMark(closeId, "production_handover");
        settle(showId);
        List<Map<String, Object>> rows = settlements(showId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("fee_kind")).isEqualTo("drink_commission");
        assertThat(rows.get(0).get("status")).isEqualTo("posted");
        assertThat(num(rows.get(0), "base_amount_cent")).isEqualTo(10000L);
        assertThat(num(rows.get(0), "amount_cent")).isEqualTo(1000L);
        assertThat(arApItems(showId)).hasSize(1);
    }

    // 159-R3：只有固定演出费的场次不判最终标记（BSM:1145「②签订固定费用的，由该酒吧向传媒公司支付合同约定的固定费用」）——演出日当天没有任何日结也照结。
    @Test
    void fixedFeeOnly_settlesWithoutFinalMarks() throws Exception {
        String date = recentDay.minusDays(1).toString();
        String showId = lockedShow(fixedFeeOnly(date));
        recordAndConfirm(showId, null, date);
        settle(showId);
        List<Map<String, Object>> rows = settlements(showId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("fee_kind")).isEqualTo("fixed_fee");
        assertThat(rows.get(0).get("status")).isEqualTo("posted");
    }

    // 159-R3：事实未确认的场次照旧写待处理行（待处理路径不取数据，不判最终标记）。
    @Test
    void unconfirmedFacts_stayPendingWithoutFinalMarks() throws Exception {
        String showId = lockedShow(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType,
                recentDay.minusDays(2).toString()));
        settle(showId);
        List<Map<String, Object>> rows = settlements(showId);
        assertThat(rows).hasSize(3);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("status")).isEqualTo("pending");
            assertThat(r.get("pending_reason")).isEqualTo("unconfirmed");
        });
    }

    // 159-R3：门票加价分配取已核销未冲正的入场，要退款与冲正已标记为最终（三项全填的场次因酒水提成同样要这一类，证明不了，故用单项场次）。
    @Test
    void ticketShare_needsRefundsReversalsFinal() throws Exception {
        String date = recentDay.toString();
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, date);
        body.remove("fixed_fee_cent");
        body.remove("drink_commission_bps");
        body.remove("drink_base_template");
        String showId = lockedShow(body);
        recordAndConfirm(showId, null, date);
        dailyCloseMarked(recentDay, "orders_verifications", "production_handover", "attendance", "anomaly_resolutions");
        settleCall(showId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SETTLEMENT_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(containsString("当日退款与冲正记录")));
        assertThat(settlements(showId)).isEmpty();
    }

    // 159-R3：月结补跑逐场 try/catch——出品未标的酒水提成场次跳过、不写结算行，同月只有固定费的场次照结（不断言返回值：共享库里同月别的场次也会被取到）。
    @Test
    void monthlyBackfill_skipsNonFinalShow_settlesFixedFeeShow() throws Exception {
        String date = recentDay.toString();
        String drinkShow = lockedShow(drinkOnly("whole_store_session", date));
        recordAndConfirm(drinkShow, null, date);
        order("standard", Instant.parse(date + "T13:00:00Z"), 10000L, "delivered");
        dailyCloseMarked(recentDay, "orders_verifications", "refunds_reversals", "attendance", "anomaly_resolutions");
        String fixedShow = lockedShow(fixedFeeOnly(date));
        recordAndConfirm(fixedShow, null, date);
        TestSecurityActors.asPlatformAdmin();
        settlementService.settleConfirmedForMonth(YearMonth.from(recentDay));
        assertThat(settlements(drinkShow)).isEmpty();
        List<Map<String, Object>> rows = settlements(fixedShow);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("fee_kind")).isEqualTo("fixed_fee");
        assertThat(rows.get(0).get("status")).isEqualTo("posted");
    }

    // ---------------------------------------------------------------- 夹具

    private Map<String, Object> fixedFeeOnly() {
        return fixedFeeOnly(SHOW_DATE);
    }

    private Map<String, Object> fixedFeeOnly(String showDate) {
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, showDate);
        body.put("ticket_share_mode", "none");
        body.remove("ticket_share_bps");
        body.remove("ticket_surcharge_per_ticket_cent");
        body.remove("ticket_type_id");
        body.remove("drink_commission_bps");
        body.remove("drink_base_template");
        return body;
    }

    private Map<String, Object> drinkOnly(String template) {
        return drinkOnly(template, SHOW_DATE);
    }

    private Map<String, Object> drinkOnly(String template, String showDate) {
        Map<String, Object> body = fixedFeeOnly(showDate);
        body.remove("fixed_fee_cent");
        body.put("drink_commission_bps", 1000);
        body.put("drink_base_template", template);
        return body;
    }

    private String lockedShow(Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String showId = JSON.readTree(response).path("data").path("id").asText();
        mockMvc.perform(post(BASE + "/" + showId + "/lock").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"lock-" + showId + "\"}"))
                .andExpect(status().isOk());
        return showId;
    }

    private void recordAndConfirm(String showId, String uncompleted) throws Exception {
        recordAndConfirm(showId, uncompleted, SHOW_DATE);
    }

    private void recordAndConfirm(String showId, String uncompleted, String showDate) throws Exception {
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment").with(mediaClerk).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(MediaPerformanceTestFixtures.fulfillmentBody(showDate, uncompleted))))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/confirm").with(barOnSite).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"cf-" + showId + "\"}"))
                .andExpect(status().isOk());
    }

    private void settle(String showId) throws Exception {
        settleCall(showId).andExpect(status().isOk());
    }

    private ResultActions settleCall(String showId) throws Exception {
        return mockMvc.perform(post(BASE + "/" + showId + "/settle").with(manager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"st-" + showId + "-" + (++seq) + "\"}"));
    }

    /** 159-R3：平台侧财务 BP 经真实接口（蛇形键）生成该酒吧该营业日的日结，再逐类标记为最终；返回日结编号。 */
    private String dailyCloseMarked(LocalDate day, String... categories) throws Exception {
        String response = mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dc-" + suffix + "-" + (++seq) + "\",\"business_date\":\"" + day
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String closeId = JSON.readTree(response).path("data").path("id").asText();
        for (String category : categories) {
            finalMark(closeId, category);
        }
        return closeId;
    }

    private void finalMark(String closeId, String category) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + closeId + "/final-marks").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"fm-" + suffix + "-" + (++seq) + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }

    private String order(String orderType, Instant at, long amountCent, String deliveryStatus) {
        String orderId = "mps_ord_" + suffix + "_" + (++seq);
        Timestamp ts = Timestamp.from(at);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,table_id,order_source,status,total_amount_cent,order_type,created_at,updated_at) VALUES(?,?,?,'pos','delivered',?,?,?,?)",
                orderId, bar, table, amountCent, orderType, ts, ts);
        jdbc.update("INSERT INTO bar_order_items(id,order_id,product_id,product_name,quantity,unit_price_cent,amount_cent,delivery_status,created_at,updated_at) VALUES(?,?,?,'威士忌',1,?,?,?,?,?)",
                orderId + "_i", orderId, product, amountCent, amountCent, deliveryStatus, ts, ts);
        return orderId;
    }

    private void verifiedTicket(Instant at, boolean verified) {
        String id = "mps_tk_" + suffix + "_" + (++seq);
        Timestamp ts = Timestamp.from(at);
        jdbc.update("INSERT INTO ticket_orders(id,order_no,ticket_type_id,quantity,sales_channel,amount_cent,pay_status,issue_status,status,created_at,updated_at,project_id,bar_id,paid_at) VALUES(?,?,?,1,'miniapp',12000,'paid','issued','created',?,?,?,?,?)",
                "ord_" + id, "ord_" + id, ticketType, ts, ts, project, bar, ts);
        jdbc.update("INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,status,created_at,updated_at,bar_id) VALUES(?,?,?,?,1,'active',?,?,?)",
                id, id, "ord_" + id, ticketType, ts, ts, bar);
        jdbc.update("INSERT INTO issued_entitlements(id,ticket_type_id,order_id,entitlement_template_id,entitlement_code,entitlement_type,total_times,used_times,status,valid_from,valid_to,created_at,updated_at,parent_ticket_id,bar_id) VALUES(?,?,?,?,?,'bar_entry',1,?,'active',?,?,?,?,?,?)",
                id + "_e", ticketType, "ord_" + id, template, "code_" + id, verified ? 1 : 0, ts, ts, ts, ts, id, bar);
        if (verified) {
            // ticket_verifications.device_id 有 FK → terminal_devices(id)（V159）：先种一台合法设备（照 SettlementStateMachine 测试范式）。
            jdbc.update("INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at) VALUES('dev_mps','dev-dev_mps','DC-dev_mps','kiosk','venue','bound',?,?) ON CONFLICT (id) DO NOTHING", ts, ts);
            jdbc.update("INSERT INTO ticket_verifications(id,verification_no,entitlement_id,entitlement_code,credential_type,credential_payload,device_id,point_id,status,created_at) VALUES(?,?,?,?,'qr','p','dev_mps','pt','success',?)",
                    id + "_v", "VN-" + id, id + "_e", "code_" + id, ts);
        }
    }

    private List<Map<String, Object>> settlements(String showId) {
        return jdbc.queryForList("SELECT * FROM media_performance_settlements WHERE show_id = ? ORDER BY created_at, id", showId);
    }

    private List<Map<String, Object>> arApItems(String showId) {
        return jdbc.queryForList("""
                SELECT a.* FROM ar_ap_items a
                 WHERE a.source_type = ? AND a.origin_item_id IS NULL
                   AND a.source_id IN (SELECT id FROM media_performance_settlements WHERE show_id = ?)
                 ORDER BY a.created_at, a.id
                """, SOURCE, showId);
    }

    private static long num(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
