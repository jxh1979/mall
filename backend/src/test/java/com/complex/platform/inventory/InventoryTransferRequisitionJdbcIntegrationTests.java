package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-011 门店要货（供货调拨）真库、真接口回归，请求体一律按前端真实形状（蛇形键）。
 * BSM L1212「门店要货依次经过门店提交、供货主体审核、仓库拣配、出库、配送、门店签收和差异处理；每次转换记录操作者和时间。
 * 签收分别记录实收、合格接收、拒收和短少数量；合格接收部分转移货权、计入门店库存，并按已锁定内部结算价形成货款。
 * 未出库数量可经审批取消」；BSM L1213 / ENUM L169 九态；台账 §25「调拨买断或正式供货销售在接收方验收后转移所有权，并同步生成应收应付」；
 * 操作员裁决 2026-10-19 第 2 条「本批做，不同意退回草稿：供货主体不同意时退回草稿，门店改后可重新提交，不想要了再自行取消」。
 * 第十八批第 18 条（BSM:1201「平台级供货主体（如中央餐饮）向商户送货（调拨）」「「调」是买断，成交时所有权转移给接收方」；ENUM:162「调拨」）：
 * 门店要货签收与借调买断两侧都记调拨（transfer）流水——买断出让方交货扣出让方库位，接收方收货入本商户物品。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryTransferRequisitionJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/transfer-orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SupervisorReviewService reviews;

    @Autowired
    private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fs011_prj_" + suffix;
    private final String merchant = "fs011_mch_" + suffix;
    private final String otherMerchant = "fs011_mch2_" + suffix;
    private final String supplier = "fs011_sup_" + suffix;
    private final String item = "fs011_item_" + suffix;
    private final String warehouse = "fs011_wh_" + suffix;
    private final String location = "fs011_loc_" + suffix;
    private final String sourceItem = "fs011_srcitem_" + suffix;
    private final String sourceLocation = "fs011_srcloc_" + suffix;
    private RequestPostProcessor admin;
    private RequestPostProcessor store;
    private RequestPostProcessor otherStore;

    @BeforeEach
    void seed() {
        String entity = "fs011_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','门店要货回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", m, m, entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "fs011_ct_" + m, m, entity, entity);
        }
        jdbc.update("INSERT INTO supply_entities(id,name,entity_type,level,status) VALUES(?,'中央酒水供货','liquor','platform','active')", supplier);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'要货测试酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','门店仓','active',?)", warehouse, project, merchant);
        jdbc.update("INSERT INTO warehouse_locations(id,warehouse_id) VALUES(?,?)", location, warehouse);
        String role = "fs011_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'门店要货回归','project','active',now())", role, role);
        jdbc.update("""
                INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(
                'inventory.transfer_order.manage','inventory.transfer_order.view','governance.supervisor_review.view','governance.supervisor_review.approve',
                'finance.ar_ap.manage','evidence.attachment.create')
                """, role);
        admin = account("admin", "platform_admin", "org", null, role);
        store = account("store", "staff", project, merchant, role);
        account("boss", "staff", project, merchant, role);
        otherStore = account("other", "staff", project, otherMerchant, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("store"), user("boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R5：草稿→待供货主体审核→待拣配→待出库→配送中→待签收→已完成；未经审核与拣配不得出库，出库后不能再取消；每次转换一行操作者与时间。
    @Test
    void supplyRequisitionWalksTheNineStatesAndRecordsOperatorAndTimeForEveryTransition() throws Exception {
        String id = createSupply(2, 1000L, List.of());
        call(admin, id, "ship", null).andExpect(status().isConflict());
        step(store, id, "submit", "{}", "pending_supplier_review");
        step(admin, id, "supplier-review", "{\"decision\":\"approved\",\"reason\":\"库存充足\"}", "pending_picking");
        call(admin, id, "ship", null).andExpect(status().isConflict());
        step(admin, id, "pick", "{}", "pending_outbound");
        call(admin, id, "ship", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("in_delivery"));
        call(store, id, "cancel", "{\"operator_note\":\"不要了\"}").andExpect(status().isConflict());
        step(admin, id, "deliver", "{}", "pending_receipt");
        step(store, id, "receive", receiptBody(line(id, 2, 2, 0, List.of())), "completed");

        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT from_status,to_status,operator_id,operated_at FROM inventory_transfer_order_events WHERE transfer_order_id=? ORDER BY operated_at,id", id);
        assertThat(events).extracting(e -> e.get("to_status")).containsExactly(
                "draft", "pending_supplier_review", "pending_picking", "pending_outbound", "in_delivery", "pending_receipt", "completed");
        assertThat(events).extracting(e -> e.get("from_status")).containsExactly(
                null, "draft", "pending_supplier_review", "pending_picking", "pending_outbound", "in_delivery", "pending_receipt");
        assertThat(events).extracting(e -> e.get("operator_id")).containsExactly(
                user("store"), user("store"), user("admin"), user("admin"), user("admin"), user("admin"), user("store"));
        assertThat(events).allSatisfy(e -> assertThat(e.get("operated_at")).isNotNull());
    }

    // 裁决第 2 条：供货主体不同意退回草稿；门店改后重新提交。门店账号不能代供货主体审核。
    @Test
    void supplierRejectionReturnsTheDraftForTheStoreToEditAndResubmit() throws Exception {
        String id = createSupply(5, 1000L, List.of());
        step(store, id, "submit", "{}", "pending_supplier_review");
        call(store, id, "supplier-review", "{\"decision\":\"approved\",\"reason\":\"自己批\"}").andExpect(status().isForbidden());
        step(admin, id, "supplier-review", "{\"decision\":\"rejected\",\"reason\":\"库存不足，请改为 3 瓶\"}", "draft");
        assertThat(jdbc.queryForMap("SELECT operator_id,note FROM inventory_transfer_order_events WHERE transfer_order_id=? AND from_status='pending_supplier_review'", id))
                .containsEntry("operator_id", user("admin")).containsEntry("note", "库存不足，请改为 3 瓶");

        call(store, id, "lines", "{\"lines\":[{\"item_id\":\"" + item + "\",\"quantity\":3,\"unit_price_cent\":1000,\"trace_codes\":[]}]}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT quantity FROM inventory_transfer_lines WHERE transfer_order_id=?", Integer.class, id)).containsExactly(3);
        step(store, id, "submit", "{}", "pending_supplier_review");
    }

    // 未出库取消须审批（批准才取消、驳回照常流转）；草稿门店可自行取消。审批中不得继续流转。
    @Test
    void cancelAfterSubmissionNeedsApprovalWhileDraftIsCancelledDirectly() throws Exception {
        String draft = createSupply(1, 1000L, List.of());
        call(store, draft, "cancel", "{\"operator_note\":\"不想要了\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        String id = createSupply(1, 1000L, List.of());
        step(store, id, "submit", "{}", "pending_supplier_review");
        call(store, id, "cancel", "{\"operator_note\":\"门店活动取消\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_supplier_review"));
        String review = jdbc.queryForObject("SELECT cancel_review_id FROM inventory_transfer_orders WHERE id=?", String.class, id);
        assertThat(review).isNotNull();
        call(admin, id, "supplier-review", "{\"decision\":\"approved\",\"reason\":\"照常备货\"}").andExpect(status().isConflict());
        decide(review, "approved");
        assertThat(orderStatus(id)).isEqualTo("cancelled");
        assertThat(jdbc.queryForMap("SELECT from_status,operator_id FROM inventory_transfer_order_events WHERE transfer_order_id=? AND to_status='cancelled'", id))
                .containsEntry("from_status", "pending_supplier_review").containsEntry("operator_id", user("boss"));

        String kept = createSupply(1, 1000L, List.of());
        step(store, kept, "submit", "{}", "pending_supplier_review");
        call(store, kept, "cancel", "{\"operator_note\":\"想取消\"}").andExpect(status().isOk());
        decide(jdbc.queryForObject("SELECT cancel_review_id FROM inventory_transfer_orders WHERE id=?", String.class, kept), "rejected");
        step(admin, kept, "supplier-review", "{\"decision\":\"approved\",\"reason\":\"照常备货\"}", "pending_picking");
    }

    // R1/R2：发货不挂账；签收 20 发、18 到、16 合格、2 拒收 → 短少 2、货款 16×500、门店库存 +16、单据差异处理中。
    @Test
    void qualifiedReceiptPostsPayableForAcceptedQuantityOnlyAndAddsItToStoreStock() throws Exception {
        String id = createSupply(20, 500L, List.of());
        walkToPendingReceipt(id);
        assertThat(count("SELECT count(*) FROM ar_ap_item_sources WHERE source_type='inventory_transfer' AND source_id=?", id)).isZero();

        step(store, id, "receive", receiptBody(line(id, 18, 16, 2, List.of())), "discrepancy");

        Map<String, Object> receipt = jdbc.queryForMap("SELECT * FROM inventory_transfer_receipts WHERE transfer_order_id=?", id);
        assertThat(receipt).containsEntry("arrived_quantity", 18).containsEntry("accepted_quantity", 16)
                .containsEntry("rejected_quantity", 2).containsEntry("short_quantity", 2).containsEntry("received_by", user("store"));
        Map<String, Object> payable = jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", receipt.get("payable_item_id"));
        assertThat(payable).containsEntry("direction", "payable").containsEntry("amount_cent", 8000L)
                .containsEntry("counterparty_id", supplier).containsEntry("merchant_id", merchant).containsEntry("project_id", project)
                .containsEntry("source_type", "inventory_transfer");
        assertThat(jdbc.queryForMap("SELECT quantity,merchant_id,project_id,warehouse_id FROM inventory_stock_balances WHERE item_id=? AND location_id=?", item, location))
                .containsEntry("quantity", 16).containsEntry("merchant_id", merchant).containsEntry("project_id", project).containsEntry("warehouse_id", warehouse);
        assertThat(jdbc.queryForList("SELECT movement_type,quantity,unit_cost_cent FROM inventory_stock_movements WHERE business_type='inventory_transfer' AND business_id=?", id))
                .singleElement().satisfies(m -> assertThat(m).containsEntry("movement_type", "transfer").containsEntry("quantity", 16).containsEntry("unit_cost_cent", 500L));
        assertThat(receipt.get("movement_id")).isNotNull();
    }

    // R1：合格接收部分的溯源码转到门店，拒收的仍归供货主体；逐件扫码数与合格数不符拒收。
    @Test
    void acceptedTraceCodesMoveToTheStoreWhileRejectedOnesStayWithTheSupplier() throws Exception {
        List<String> codes = List.of("FS011A" + suffix, "FS011B" + suffix, "FS011C" + suffix);
        for (String code : codes) {
            jdbc.update("INSERT INTO inventory_trace_codes(id,trace_code,item_id,merchant_id,owner_type,owner_id) VALUES(?,?,?,?,'supply_entity',?)",
                    "tc_" + code, code, item, merchant, supplier);
        }
        String id = createSupply(3, 1000L, codes);
        walkToPendingReceipt(id);
        call(store, id, "receive", receiptBody(line(id, 3, 2, 1, List.of(codes.get(0))))).andExpect(status().isBadRequest());

        step(store, id, "receive", receiptBody(line(id, 3, 2, 1, codes.subList(0, 2))), "discrepancy");
        assertThat(owner(codes.get(0))).isEqualTo(merchant);
        assertThat(owner(codes.get(1))).isEqualTo(merchant);
        assertThat(owner(codes.get(2))).isEqualTo(supplier);
    }

    @Test
    void receiptByAnotherMerchantIsForbidden() throws Exception {
        String id = createSupply(2, 1000L, List.of());
        walkToPendingReceipt(id);
        call(otherStore, id, "receive", receiptBody(line(id, 2, 2, 0, List.of()))).andExpect(status().isForbidden());
        assertThat(count("SELECT count(*) FROM inventory_transfer_receipts WHERE transfer_order_id=?", id)).isZero();
        assertThat(orderStatus(id)).isEqualTo("pending_receipt");
    }

    // 借调买断同为内部送货：发货不挂账，接收方收货后按货款形成应付。第十八批第 18 条：出让方交货扣出让方出货库位并写一条调拨（transfer）出，
    // 接收方收货把货入本商户物品、本门店库位并写一条调拨（transfer）入，单价按建单锁定内部结算价。
    @Test
    void lendingPurchasePostsPayableOnReceiptNotOnShip() throws Exception {
        String id = lentPurchase();
        call(otherStore, id, "ship", null).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE source_type='inventory_transfer' AND source_id=?", id)).isZero();
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, sourceItem, sourceLocation))
                .isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT source_location_id,target_location_id,quantity,merchant_id FROM inventory_stock_movements WHERE business_type='inventory_transfer' AND business_id=? AND movement_type='transfer'", id))
                .singleElement().satisfies(m -> assertThat(m).containsEntry("source_location_id", sourceLocation).containsEntry("target_location_id", null)
                        .containsEntry("quantity", 4).containsEntry("merchant_id", otherMerchant));

        step(store, id, "receive", purchaseReceipt(id, item, warehouse), "received");
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type='inventory_transfer' AND source_id=?", Long.class, id))
                .isEqualTo(1000L);
        assertThat(jdbc.queryForMap("SELECT quantity,merchant_id,project_id,warehouse_id FROM inventory_stock_balances WHERE item_id=? AND location_id=?", item, location))
                .containsEntry("quantity", 4).containsEntry("merchant_id", merchant).containsEntry("project_id", project).containsEntry("warehouse_id", warehouse);
        assertThat(jdbc.queryForList("SELECT item_id,source_location_id,quantity,unit_cost_cent,merchant_id FROM inventory_stock_movements WHERE business_type='inventory_transfer' AND business_id=? AND movement_type='transfer' AND target_location_id=?", id, location))
                .singleElement().satisfies(m -> assertThat(m).containsEntry("item_id", item).containsEntry("source_location_id", null)
                        .containsEntry("quantity", 4).containsEntry("unit_cost_cent", 250L).containsEntry("merchant_id", merchant));
    }

    // 第十八批第 18 条（BSM:1201「「调」是买断，成交时所有权转移给接收方」「收货时扫码核对防调包」）：买断收货写接收方库存，须接收方本商户账号办理、
    // 入本商户物品与本门店在用仓库——选出让方物品 400、他商户仓库 400、出让方账号 403，均不入库、不挂账、单仍已发货。
    @Test
    void lendingPurchaseReceiptMustStockTheReceiversOwnItemInItsOwnWarehouse() throws Exception {
        String otherWarehouse = "fs011_wh2_" + suffix;
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','出让方仓','active',?)", otherWarehouse, project, otherMerchant);
        String id = lentPurchase();
        call(otherStore, id, "ship", null).andExpect(status().isOk());

        call(store, id, "receive", purchaseReceipt(id, sourceItem, warehouse)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        call(store, id, "receive", purchaseReceipt(id, item, otherWarehouse)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_RECEIVE_WAREHOUSE_INVALID"));
        call(otherStore, id, "receive", purchaseReceipt(id, item, warehouse)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("STORE_RECEIPT_ONLY"));

        assertThat(orderStatus(id)).isEqualTo("shipped");
        assertThat(count("SELECT count(*) FROM inventory_stock_balances WHERE location_id=?", location)).isZero();
        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE source_type='inventory_transfer' AND source_id=?", id)).isZero();
    }

    /**
     * 第十八批 M 段第 119 条 119-R1（BSM:1936「……库房、库位……都要先在基础管理中定义清楚。」）：门店要货签收与借调买断收货只收收货仓库下已建档的库位，
     * 未建档 400 TRANSFER_RECEIVE_WAREHOUSE_INVALID，单据不动、不入库。
     */
    @Test
    void receiptOnlyAcceptsLocationRegisteredUnderTheReceivingWarehouse() throws Exception {
        String unregistered = "fs011_loc_new_" + suffix;
        String id = createSupply(2, 1000L, List.of());
        walkToPendingReceipt(id);
        call(store, id, "receive", receiptBody(line(id, 2, 2, 0, List.of())).replace(location, unregistered))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("TRANSFER_RECEIVE_WAREHOUSE_INVALID"));
        assertThat(orderStatus(id)).isEqualTo("pending_receipt");
        assertThat(count("SELECT count(*) FROM inventory_stock_balances WHERE location_id=?", unregistered)).isZero();

        String lent = lentPurchase();
        call(otherStore, lent, "ship", null).andExpect(status().isOk());
        call(store, lent, "receive", purchaseReceipt(lent, item, warehouse).replace(location, unregistered))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("TRANSFER_RECEIVE_WAREHOUSE_INVALID"));
        assertThat(orderStatus(lent)).isEqualTo("shipped");
        assertThat(count("SELECT count(*) FROM inventory_stock_balances WHERE location_id=?", unregistered)).isZero();
    }

    /**
     * 第十八批 M 段第 112 条 112-R4（BSM:1201「「调」是买断，成交时所有权转移给接收方」「买断时溯源码归属从原主体变更到接收主体，收货时扫码核对防调包」；
     * BSM:1197「出库须同时核验订单和物品码」）：买断收货后随货码的归属商户与登记物料随转为接收方及其所选物品——接收方库管按码列出本店含该物品的
     * 待出品订单并建交接链，出让方账号再查该码 403。
     */
    @Test
    void lendingPurchaseReceiptMovesTraceCodeMerchantAndItemToTheReceiver() throws Exception {
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions"
                + " WHERE permission_code IN('inventory.trace_code.register','inventory.handover.scan')", "fs011_role_" + suffix);
        String code = "TRC-B1-" + suffix;
        String id = lentPurchase(1, List.of(code));
        mockMvc.perform(post("/api/v1/inventory/trace-codes").with(otherStore).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key() + "\",\"item_id\":\"" + sourceItem + "\",\"trace_code\":\"" + code + "\"}"))
                .andExpect(status().isOk());
        call(otherStore, id, "ship", null).andExpect(status().isOk());
        step(store, id, "receive", purchaseReceipt(id, item, warehouse, List.of(code)), "received");
        assertThat(jdbc.queryForMap("SELECT owner_id,merchant_id,item_id FROM inventory_trace_codes WHERE trace_code=?", code))
                .containsEntry("owner_id", merchant).containsEntry("merchant_id", merchant).containsEntry("item_id", item);

        // 接收方卖这瓶酒：本店一张含该物品按瓶出品、待出品明细的酒吧订单（夹具同 TraceHandoverOutboundOrderJdbcTests）。
        String order = "fs011_bo_" + suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'K1', '卡座 K1', 'card_seat', 'available', now(), now())", "fs011_tbl_" + suffix, merchant, merchant);
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, inventory_item_id, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, '要货测试酒水 整瓶', 'liquor', 88800, ?, 'active', now(), now())",
                "fs011_bp_" + suffix, merchant, merchant, item);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, merchant_id, table_id, order_source, status, total_amount_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'staff', 'submitted', 88800, now(), now())", order, merchant, merchant, "fs011_tbl_" + suffix);
        jdbc.update("INSERT INTO bar_order_items (id, order_id, product_id, product_name, quantity, unit_price_cent, amount_cent, delivery_status,"
                + " created_at, updated_at) VALUES (?, ?, ?, '要货测试酒水 整瓶', 1, 88800, 88800, 'pending', now(), now())",
                "fs011_boi_" + suffix, order, "fs011_bp_" + suffix);
        String outbound = "/api/v1/inventory/trace-codes/" + code + "/outbound-orders";
        mockMvc.perform(get(outbound).with(store)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].order_id").value(order));
        mockMvc.perform(post("/api/v1/inventory/trace-handovers").with(store).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key() + "\",\"trace_code\":\"" + code + "\",\"order_id\":\"" + order
                                + "\",\"device_id\":\"fs011_dev_" + suffix + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(outbound).with(otherStore)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    // 签收转移货权、计入门店库存并形成门店应付供货主体的货款（BSM L1212「门店签收」）：供货侧非商户账号不得代门店签收。
    @Test
    void supplySideAccountCannotSignTheStoresReceipt() throws Exception {
        RequestPostProcessor supply = account("supply", "staff", project, null, "fs011_role_" + suffix);
        String id = createSupply(2, 1000L, List.of());
        walkToPendingReceipt(id);

        call(supply, id, "receive", receiptBody(line(id, 2, 2, 0, List.of()))).andExpect(status().isForbidden());
        assertThat(orderStatus(id)).isEqualTo("pending_receipt");
    }

    // 差异复核由财务办理（BSM L1212「财务复核金额后办理补发、退回、货款调整或责任损失」；L1673 数据默认按项目隔离）：
    //   门店本店同事（商户账号，即便持复核权限）与他项目财务都不能复核，本项目财务可以。
    @Test
    void sameStoreColleagueAndOtherProjectFinanceCannotReviewDisposition() throws Exception {
        String role = "fs011_role_" + suffix;
        RequestPostProcessor colleague = account("colleague", "staff", project, merchant, role);
        String otherProject = "fs011_prj2_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','他项目','active',now(),now())", otherProject);
        RequestPostProcessor otherFinance = account("fin2", "staff", otherProject, null, role);
        String id = createSupply(4, 500L, List.of());
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 4, 2, 2, List.of())), "discrepancy");
        String adjust = dispose(id, "payment_adjustment", null, null, 100L);

        call(colleague, id, "dispositions/" + adjust + "/review", "{\"decision\":\"approved\",\"reason\":\"同店同事批\"}")
                .andExpect(status().isForbidden());
        call(otherFinance, id, "dispositions/" + adjust + "/review", "{\"decision\":\"approved\",\"reason\":\"他项目财务批\"}")
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM inventory_transfer_dispositions WHERE id=?", String.class, adjust))
                .isEqualTo("pending_review");
        review(id, adjust, "approved");
    }

    // R3：已出库不能直接取消；拒收 2 经退回验收、短少 2 经复核损失（供货主体担责不形成应付），财务复核（登记人不得自审）后
    // 全部数量有结果才完成；同一数量只能有一个结果，登记超出未处理数量拒绝。
    @Test
    void discrepancyCompletesOnlyAfterEveryUnitHasAReviewedOutcome() throws Exception {
        String id = createSupply(20, 500L, List.of());
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 18, 16, 2, List.of())), "discrepancy");
        call(store, id, "cancel", "{\"operator_note\":\"不要了\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS"));

        String returned = dispose(id, "return_accepted", 2, null, null);
        String lost = dispose(id, "loss", 2, "supplier", null);
        call(store, id, "dispositions", dispositionBody(id, "return_accepted", 1, null, null)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_DISPOSITION_EXCEEDS_DISCREPANCY"));
        call(store, id, "dispositions/" + returned + "/review", "{\"decision\":\"approved\",\"reason\":\"自己批\"}")
                .andExpect(status().isForbidden());
        review(id, returned, "approved");
        assertThat(orderStatus(id)).isEqualTo("discrepancy");
        review(id, lost, "approved");

        assertThat(orderStatus(id)).isEqualTo("completed");
        assertThat(count("SELECT count(*) FROM ar_ap_item_sources WHERE source_type='inventory_transfer' AND source_id=?", id)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT amount_cent,reviewed_by,payable_item_id FROM inventory_transfer_dispositions WHERE id=?", lost))
                .containsEntry("amount_cent", 1000L).containsEntry("reviewed_by", user("admin")).containsEntry("payable_item_id", null);
        assertThat(jdbc.queryForMap("SELECT from_status,operator_id FROM inventory_transfer_order_events WHERE transfer_order_id=? AND to_status='completed'", id))
                .containsEntry("from_status", "discrepancy").containsEntry("operator_id", user("admin"));
    }

    // R3：收货方担责的损失按该行锁定单价追加一笔门店应付供货主体的应付。
    @Test
    void receiverResponsibleLossPostsPayableAtTheLockedPrice() throws Exception {
        String id = createSupply(10, 500L, List.of());
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 8, 8, 0, List.of())), "discrepancy");
        String lost = dispose(id, "loss", 2, "receiver", null);
        review(id, lost, "approved");

        String payable = jdbc.queryForObject("SELECT payable_item_id FROM inventory_transfer_dispositions WHERE id=?", String.class, lost);
        assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", payable)).containsEntry("direction", "payable")
                .containsEntry("amount_cent", 1000L).containsEntry("unpaid_amount_cent", 1000L).containsEntry("counterparty_id", supplier)
                .containsEntry("merchant_id", merchant).containsEntry("project_id", project).containsEntry("source_type", "inventory_transfer");
        assertThat(orderStatus(id)).isEqualTo("completed");
    }

    // R3：货款调整按 V344 守恒冲正——原应付行不动，追加严格取反的冲正行与重述行；复核前不动账。
    @Test
    void paymentAdjustmentAppendsReversalAndRestatementKeepingTheOriginal() throws Exception {
        String id = createSupply(10, 500L, List.of());
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 10, 9, 1, List.of())), "discrepancy");
        String original = jdbc.queryForObject("SELECT DISTINCT payable_item_id FROM inventory_transfer_receipts WHERE transfer_order_id=?", String.class, id);
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", original);

        String adjust = dispose(id, "payment_adjustment", null, null, 500L);
        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE origin_item_id=?", original)).isZero();
        review(id, adjust, "approved");

        assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", original)).isEqualTo(before);
        List<Map<String, Object>> derived = jdbc.queryForList(
                "SELECT id,amount_cent,paid_amount_cent,unpaid_amount_cent,status FROM ar_ap_items WHERE origin_item_id=? ORDER BY amount_cent", original);
        assertThat(derived).extracting(row -> row.get("amount_cent")).containsExactly(-4500L, 4000L);
        assertThat(derived.get(0)).containsEntry("status", "reversed").containsEntry("unpaid_amount_cent", -4500L);
        assertThat(derived.get(1)).containsEntry("status", "pending_payment").containsEntry("unpaid_amount_cent", 4000L);
        mockMvc.perform(get(BASE + "/" + id + "/dispositions").with(store)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable_item_id").value(derived.get(1).get("id")))
                .andExpect(jsonPath("$.data.lines[0].unresolved_quantity").value(1));
        assertThat(orderStatus(id)).isEqualTo("discrepancy");

        review(id, dispose(id, "return_accepted", 1, null, null), "approved");
        assertThat(orderStatus(id)).isEqualTo("completed");
    }

    // R3：实际退回验收的件溯源码仍归供货主体，不形成任何款项。
    @Test
    void returnAcceptedKeepsTraceCodesWithTheSupplierAndPostsNoMoney() throws Exception {
        List<String> codes = List.of("FS011R" + suffix, "FS011S" + suffix, "FS011T" + suffix);
        for (String code : codes) {
            jdbc.update("INSERT INTO inventory_trace_codes(id,trace_code,item_id,merchant_id,owner_type,owner_id) VALUES(?,?,?,?,'supply_entity',?)",
                    "tc_" + code, code, item, merchant, supplier);
        }
        String id = createSupply(3, 1000L, codes);
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 3, 2, 1, codes.subList(0, 2))), "discrepancy");
        long items = count("SELECT count(*) FROM ar_ap_items WHERE counterparty_id=? AND merchant_id=?", supplier, merchant);

        review(id, dispose(id, "return_accepted", 1, null, null), "approved");

        assertThat(owner(codes.get(2))).isEqualTo(supplier);
        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE counterparty_id=? AND merchant_id=?", supplier, merchant)).isEqualTo(items);
        assertThat(orderStatus(id)).isEqualTo("completed");
    }

    // R3：补发经复核后另开一张关联要货单，从待拣配起按原锁定单价出库、配送、签收；补发不替代原短少件的结果，
    // 原单仍须复核损失后才完成；配送保管方担责的损失只登记责任与金额，不形成应付。
    @Test
    void reshipOpensALinkedRequisitionAtTheLockedPrice() throws Exception {
        String id = createSupply(5, 700L, List.of());
        walkToPendingReceipt(id);
        step(store, id, "receive", receiptBody(line(id, 3, 3, 0, List.of())), "discrepancy");
        String reship = dispose(id, "reship", 2, null, null);
        review(id, reship, "approved");

        String replacement = jdbc.queryForObject("SELECT reship_order_id FROM inventory_transfer_dispositions WHERE id=?", String.class, reship);
        assertThat(jdbc.queryForMap("SELECT transfer_type,from_owner_id,to_owner_id,status FROM inventory_transfer_orders WHERE id=?", replacement))
                .containsEntry("transfer_type", "supply").containsEntry("from_owner_id", supplier).containsEntry("to_owner_id", merchant)
                .containsEntry("status", "pending_picking");
        assertThat(jdbc.queryForMap("SELECT item_id,quantity,unit_price_cent FROM inventory_transfer_lines WHERE transfer_order_id=?", replacement))
                .containsEntry("item_id", item).containsEntry("quantity", 2).containsEntry("unit_price_cent", 700L);
        assertThat(orderStatus(id)).isEqualTo("discrepancy");

        review(id, dispose(id, "loss", 2, "carrier", null), "approved");
        assertThat(orderStatus(id)).isEqualTo("completed");
        assertThat(count("SELECT count(*) FROM ar_ap_item_sources WHERE source_type='inventory_transfer' AND source_id=?", id)).isEqualTo(1);

        step(admin, replacement, "pick", "{}", "pending_outbound");
        call(admin, replacement, "ship", null).andExpect(status().isOk());
        step(admin, replacement, "deliver", "{}", "pending_receipt");
        step(store, replacement, "receive", receiptBody(line(replacement, 2, 2, 0, List.of())), "completed");
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM ar_ap_items WHERE id IN (SELECT payable_item_id FROM inventory_transfer_receipts WHERE transfer_order_id=?)",
                Long.class, replacement)).isEqualTo(1400L);
    }

    // ── 夹具 ──

    private String createSupply(int quantity, long unitPriceCent, List<String> codes) throws Exception {
        String body = """
                {"idempotency_key":"%s","transfer_type":"supply","from_owner_type":"supply_entity","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s","to_location_id":"%s",
                 "lines":[{"item_id":"%s","quantity":%d,"unit_price_cent":%d,"trace_codes":%s}]}
                """.formatted(key(), supplier, merchant, location, item, quantity, unitPriceCent, json(codes));
        return JsonPath.read(mockMvc.perform(post(BASE).with(store).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 第十八批第 18 条夹具：出让方（otherMerchant）出货库位有 10 件自有物品，按蛇形键建一张买断单（4 件、单价 250 分）。 */
    private String lentPurchase() throws Exception {
        return lentPurchase(4, List.of());
    }

    /** 第十八批 M 段 112-R4：同上，指定件数与随货溯源码。 */
    private String lentPurchase(int quantity, List<String> codes) throws Exception {
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'出让方酒水','liquor','瓶',0,'active',?,now(),now())", sourceItem, otherMerchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,updated_at) VALUES(?,?,?,10,0,10,?,?,now())",
                "fs011_srcbal_" + suffix, sourceItem, sourceLocation, otherMerchant, project);
        return JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s","transfer_type":"lending","lending_mode":"purchase","from_owner_type":"merchant",
                         "from_owner_id":"%s","to_owner_type":"merchant","to_owner_id":"%s","from_location_id":"%s",
                         "lines":[{"item_id":"%s","quantity":%d,"unit_price_cent":250,"trace_codes":%s}]}
                        """.formatted(key(), otherMerchant, merchant, sourceLocation, sourceItem, quantity, json(codes))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("created"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 买断收货（蛇形键）：收货仓库、库位与逐行本商户物品。 */
    private String purchaseReceipt(String orderId, String itemId, String warehouseId) {
        return purchaseReceipt(orderId, itemId, warehouseId, List.of());
    }

    private String purchaseReceipt(String orderId, String itemId, String warehouseId, List<String> codes) {
        return "{\"warehouse_id\":\"" + warehouseId + "\",\"location_id\":\"" + location + "\",\"scanned_trace_codes\":" + json(codes) + ","
                + "\"lines\":[{\"line_id\":\"" + orderId + "_l0\",\"item_id\":\"" + itemId + "\"}]}";
    }

    private void walkToPendingReceipt(String id) throws Exception {
        step(store, id, "submit", "{}", "pending_supplier_review");
        step(admin, id, "supplier-review", "{\"decision\":\"approved\",\"reason\":\"库存充足\"}", "pending_picking");
        step(admin, id, "pick", "{}", "pending_outbound");
        call(admin, id, "ship", null).andExpect(status().isOk());
        step(admin, id, "deliver", "{}", "pending_receipt");
    }

    private void step(RequestPostProcessor who, String id, String action, String body, String expectedStatus) throws Exception {
        call(who, id, action, body).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(expectedStatus));
    }

    /** body 为 null＝无请求体（发货）；否则补上幂等键后按蛇形键原样提交。 */
    private ResultActions call(RequestPostProcessor who, String id, String action, String body) throws Exception {
        var request = post(BASE + "/" + id + "/" + action).with(who);
        if (body != null) {
            String withKey = "{\"idempotency_key\":\"" + key() + "\"" + (body.equals("{}") ? "}" : "," + body.substring(1));
            request = request.contentType(MediaType.APPLICATION_JSON).content(withKey);
        }
        return mockMvc.perform(request);
    }

    private String receiptBody(String line) {
        return "{\"warehouse_id\":\"" + warehouse + "\",\"location_id\":\"" + location + "\",\"lines\":[" + line + "]}";
    }

    private String line(String orderId, int arrived, int accepted, int rejected, List<String> acceptedCodes) {
        return "{\"line_id\":\"" + orderId + "_l0\",\"arrived_quantity\":" + arrived + ",\"accepted_quantity\":" + accepted
                + ",\"rejected_quantity\":" + rejected + ",\"accepted_trace_codes\":" + json(acceptedCodes) + "}";
    }

    /** 门店按交接证据登记差异处理（蛇形键），返回登记 id。 */
    private String dispose(String id, String type, Integer quantity, String responsible, Long amountCent) throws Exception {
        return JsonPath.read(call(store, id, "dispositions", dispositionBody(id, type, quantity, responsible, amountCent))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String dispositionBody(String id, String type, Integer quantity, String responsible, Long amountCent) {
        return "{\"disposition_type\":\"" + type + "\""
                + (quantity == null ? "" : ",\"line_id\":\"" + id + "_l0\",\"quantity\":" + quantity)
                + (responsible == null ? "" : ",\"responsible_party\":\"" + responsible + "\"")
                + (amountCent == null ? "" : ",\"amount_cent\":" + amountCent)
                + ",\"reason\":\"按交接单核对\",\"evidence_attachment_ids\":[\"" + upload(id) + "\"]}";
    }

    /** 财务复核（与登记人不同）。 */
    private void review(String id, String disposition, String decision) throws Exception {
        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"" + decision + "\",\"reason\":\"财务复核金额\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(decision));
    }

    /** 门店上传交接证据（绑定本要货单）。 */
    private String upload(String orderId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user("store"), user("store"), "staff", project, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", "handover.png", "image/png",
                    Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                    "inventory_transfer_order", orderId, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void decide(String review, String decision) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user("boss"), user("boss"), "staff", project, merchant, "active"), "test", List.of()));
        reviews.approve(review, new SupervisorReviewApprovalRequest(key(), "核对门店取消原因", List.of(), null, decision, "门店负责人裁决"));
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organizationId);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "fs011_" + tag + "_" + suffix;
    }

    private String orderStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM inventory_transfer_orders WHERE id=?", String.class, id);
    }

    private String owner(String code) {
        return jdbc.queryForObject("SELECT owner_id FROM inventory_trace_codes WHERE trace_code=?", String.class, code);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private static String json(List<String> values) {
        return values.isEmpty() ? "[]" : "[\"" + String.join("\",\"", values) + "\"]";
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
