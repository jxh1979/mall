package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 第十八批 AF 段第 198 条 198-R5（存酒）：契约删去 SavedBottleRequest.operator_note、SavedBottleTransferRequest.operator_note（代码不读；
 * 后台存酒页 StoredWinePage 转让停发 operator_note）后，门店员工按新形状走真实 HTTP、真实授权链：先按契约删后形状登记存酒，
 * 再按存酒页转让的真实请求形状（idempotency_key、evidence_attachment_ids、to_name_or_nickname、to_phone、verification_code）转让。
 * 入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleTransferWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());
    private final String merchant = "bar_b52sw_" + s;
    private final String item = "item_b52sw_" + s;
    private final String staffId = "usr_b52sw_" + s;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        String project = "prj_b52sw_" + s;
        String entity = "ent_b52sw_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','存酒形状综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'存酒形状主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'存酒形状酒吧','bar',?,?,'active',now(),now())", merchant, entity, project);
        // 转让的准入闸 inventory.saved_bottle_claim 按合同经营范围判（V099 over_contract_scope）：给本酒吧一份含 inventory 的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,"
                + "allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\"]'::jsonb,now(),now())",
                "ct_b52sw_" + s, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'存酒形状威士忌','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        staff = new StaffRequestFixture(jdbc).staff(staffId, "staff", project, merchant, "bar.saved_bottle.manage");
    }

    // 登记（不带 operator_note）与转让（存酒页形状，不带 operator_note）：都 200，持有人换成受让人，转让记录落操作人与整瓶数量。
    @Test
    void transferSavedBottle_webShapeWithoutOperatorNote_ok() throws Exception {
        String digits = s.substring(s.length() - 8);
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("idempotency_key", "b52sw-create-" + s);
        create.put("bar_id", merchant);
        create.put("items", List.of(Map.of("item_id", item, "quantity", 2, "bottle_name", "存酒形状威士忌", "unit", "bottle")));
        create.put("expire_at", Instant.now().plusSeconds(30L * 86400).toString());
        create.put("bottle_label_value", "LBL-B52SW");
        create.put("customer_name_or_nickname", "出让人");
        create.put("customer_phone", "155" + digits);
        create.put("registration_photo_refs", List.of("reg_photo_b52sw"));
        String bottleId = JsonPath.read(call("/api/v1/inventory/saved-bottles", create).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        Map<String, Object> transfer = new LinkedHashMap<>();
        transfer.put("idempotency_key", "admin-saved-bottle-transfer-" + bottleId + "-156" + digits);
        transfer.put("evidence_attachment_ids", List.of());
        transfer.put("to_name_or_nickname", "受让人");
        transfer.put("to_phone", "156" + digits);
        transfer.put("verification_code", "000000");
        call("/api/v1/inventory/saved-bottles/" + bottleId + "/transfer", transfer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved_bottle_id").value(bottleId))
                .andExpect(jsonPath("$.data.status").value("transferred"));

        assertThat(jdbc.queryForObject("SELECT customer_name_or_nickname FROM saved_bottles WHERE id = ?", String.class, bottleId))
                .isEqualTo("受让人");
        assertThat(jdbc.queryForMap("SELECT to_name_or_nickname, operator_id, transfer_quantity"
                        + " FROM saved_bottle_transfers WHERE saved_bottle_id = ?", bottleId))
                .containsEntry("to_name_or_nickname", "受让人")
                .containsEntry("operator_id", staffId)
                .containsEntry("transfer_quantity", 2);
    }

    private ResultActions call(String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(staff).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
