package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.inventory.dto.SupplyContractCreateRequest;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AF 段第 198 条 198-R5（库存与经销与商户计费）：契约删去 PurchaseRequestCreate.operator_note（建单从不读；采购工作台
 * PurchaseWorkbenchPage 建单删「申请依据」框并停发）后，采购人员按工作台建单的新形状——supplier_id、supply_contract_id、
 * items[{item_id, quantity}]、expected_arrival_date、idempotency_key——走真实 HTTP、真实授权链建采购申请。
 * 入口未改，本类只锁现行为，不先红。供货合同的审批链由 PurchaseInboundPayableJdbcIntegrationTests 覆盖，本类只把造数用的合同直接置为已批准。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PurchaseRequestCreateWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private SupplyContractService contracts;
    @Autowired private PurchaseWorkflowService workflow;
    @Autowired private EvidenceEventService evidence;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_b52pr_" + s;
    private final String entity = "ent_b52pr_" + s;
    private final String merchant = "mch_b52pr_" + s;
    private final String item = "item_b52pr_" + s;
    private final String supplier = "sup_b52pr_" + s;
    private final String requestorId = "usr_b52pr_" + s;
    private RequestPostProcessor requestor;
    private String contract;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','采购形状项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'采购形状主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'采购形状商户','retail',?,?,'active',now(),now())", merchant, entity, project);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'采购形状耗材','consumable','包',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO inventory_suppliers(id,supplier_name,supplier_type,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'采购形状供应商','general','active',?,now(),now())", supplier, merchant);
        jdbc.update("INSERT INTO supplier_qualifications(id,supplier_id,qualification_type,valid_from,valid_to,status,merchant_id)"
                + " VALUES(?,?,'business_license',current_date-1,current_date+365,'active',?)", "qual_b52pr_" + s, supplier, merchant);
        requestor = new StaffRequestFixture(jdbc).staff(requestorId, "staff", project, merchant,
                "inventory.purchase_request.manage", "inventory.supply_contract.manage", "evidence.attachment.create");
        // 造数：商户自采合同＋执行条款（按收货逐笔、账期 30 天、按实收单价），再直接置为已批准。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(requestorId, requestorId, "staff", project, merchant, "active"), "test", List.of()));
        LocalDate today = LocalDate.now(LocalZone.ID);
        contract = contracts.createSupplyContract(new SupplyContractCreateRequest("b52pr-contract-" + s, supplier, project, merchant,
                "merchant_self_purchase", "本商品", "见合同执行价", null, today.minusDays(1), today.plusYears(1), "采购形状合同",
                List.of(item))).id();
        String proof = evidence.uploadAttachment(new MockMultipartFile("file", "contract.png", "image/png",
                Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                "supply_contract", contract, "other").id();
        workflow.terms(contract, new PurchaseWorkflowRequests.Terms("b52pr-terms-" + s, new PurchaseContractTerms(entity, "credit_term",
                "accepted_actual_unit_price", Map.of(), "each_receipt", 30, null, today, null, null, null), List.of(proof)));
        jdbc.update("UPDATE supply_contracts SET approval_status = 'approved' WHERE id = ?", contract);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 工作台建单新形状（不带 operator_note）：200，采购申请落库为 created，申请人取登录主体、归属合同所在项目。
    @Test
    void createPurchaseRequest_webShapeWithoutOperatorNote_ok() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplier_id", supplier);
        body.put("supply_contract_id", contract);
        body.put("items", List.of(Map.of("item_id", item, "quantity", 30)));
        body.put("expected_arrival_date", LocalDate.now(LocalZone.ID).plusDays(3).toString());
        body.put("idempotency_key", "b52pr-create-" + s);
        String id = JsonPath.read(mockMvc.perform(post("/api/v1/inventory/purchase-requests").with(requestor)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        assertThat(jdbc.queryForMap("SELECT status, requested_by, project_id, supply_contract_id FROM inventory_purchase_requests WHERE id = ?", id))
                .containsEntry("status", "created")
                .containsEntry("requested_by", requestorId)
                .containsEntry("project_id", project)
                .containsEntry("supply_contract_id", contract);
    }
}
