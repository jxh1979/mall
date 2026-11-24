package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
 * 第十八批 BK 段第 384 条 384-R1～R3：不挂商户的平台侧账号（酒吧财务 BP、平台库房库管）办理库存盘点、差异过账与差异照片视频。
 * PGM:664「单店层面的财务工作——日结确认与锁定、商户费用账单出账、差异认定、异常单据处置、报损物品的财务确认——由该酒吧对应的财务 BP 负责」；
 * PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」；
 * BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；BSM:1687「核对采购入库、库存消耗、报损、盘点差异和高价值酒水异常。」；
 * BSM:1183「高价值酒水报损、盘点差异、存酒作废必须拍照和视频留证，并主管复核。」；PGM:948「库房、库存报损和盘点差异，都要看商户归属、合同范围和复核权限。」。
 *
 * <p>口径：商户账号只办本商户库房的盘点单；不挂商户的账号按入口权限码可触及库房所在综合体办理；最高权限与平台管理员不限；盘点单商户取库房的商户；
 * 差异过账落的台账行带盘点库房所在综合体；差异证据带盘点单编号上传、按盘点码与盘点单范围回读。修复前不挂商户的账号各处 403 MERCHANT_SCOPE_REQUIRED，
 * 商户账号给别家库房建单放行。真库、真接口、真实授权链、蛇形键；夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StocktakeProjectScopeJdbcIntegrationTests {

    private static final String VIEW = "inventory.stocktake.view";
    private static final String CREATE = "inventory.stocktake.create";
    private static final String RECORD = "inventory.stocktake.variance_record";
    private static final String REVIEW = "inventory.stocktake.variance_review";
    private static final String POST = "finance.inventory_variance.post";
    private static final String UPLOAD = "evidence.attachment.create";
    private static final String READ_FILE = "evidence.timeline.view";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private InventoryRepository inventoryRepository;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r384pa_" + s;
    private final String projectB = "r384pb_" + s;
    /** A 综合体里的两家酒吧：甲、乙。 */
    private final String merchantA = "r384ma_" + s;
    private final String merchantB = "r384mb_" + s;
    private final String barItem = "r384ib_" + s;
    private final String platformItem = "r384ip_" + s;
    /** A 综合体甲的吧台仓、A 综合体不挂商户的总库、B 综合体的总库。 */
    private final String barWarehouse = "r384wb_" + s;
    private final String platformWarehouse = "r384wp_" + s;
    private final String otherWarehouse = "r384wo_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','盘点范围回归综合体甲','active',now(),now())", projectA);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','盘点范围回归综合体乙','active',now(),now())", projectB);
        String entity = "r384en_" + s;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, projectA);
        for (String id : List.of(merchantA, merchantB)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", id, id, entity, projectA);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "ct_" + id, id, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'盘点范围吧台酒水','liquor','瓶',0,'active',?,now(),now())", barItem, merchantA);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'盘点范围总库酒水','liquor','瓶',0,'active',NULL,now(),now())", platformItem);
        warehouse(barWarehouse, projectA, "bar", merchantA);
        warehouse(platformWarehouse, projectA, "main", null);
        warehouse(otherWarehouse, projectB, "main", null);
        balance(barItem, barWarehouse, merchantA, projectA);
        balance(platformItem, platformWarehouse, null, projectA);
        balance(platformItem, otherWarehouse, null, projectB);
        // 吧台酒水低价值（到货价 10 元 < 出厂阈值 5000 元）；总库酒水高价值（到货价 6000 元）：差异不为 0 须照片＋视频。
        arrival(barItem, barWarehouse, 1_000L, merchantA, projectA);
        arrival(platformItem, platformWarehouse, 600_000L, null, projectA);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 384-R1：财务 BP（A 综合体员工、不挂商户）看得到 A 综合体各库房的盘点单与差异，看不到 B 综合体的。 */
    @Test
    void financeBpWithoutMerchantSeesStocktakesOfReachableProjectOnly() throws Exception {
        String bar = stocktake(barWarehouse, merchantA);
        String platform = stocktake(platformWarehouse, null);
        String other = stocktake(otherWarehouse, null);
        variance(bar, barItem, "pending", -2000L);
        RequestPostProcessor bp = account("bp", projectA, null, VIEW);

        List<String> ids = JsonPath.read(get(bp, "/api/v1/inventory/stocktakes").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(ids).contains(bar, platform).doesNotContain(other);
        get(bp, "/api/v1/inventory/stocktakes/" + bar).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stocktake.id").value(bar))
                .andExpect(jsonPath("$.data.variances.length()").value(1));
        get(bp, "/api/v1/inventory/stocktakes/" + other).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    /**
     * 384-R1、384-R3：平台库管（A 综合体员工、不挂商户）给总库建单（不挂商户）、带盘点单编号上传照片视频（附件不挂商户）、登记高价值差异，
     * 另一名不挂商户的复核人复核后纠账，结束盘点；B 综合体的账号拿同一请求编号重放登记 403；库管给 B 综合体库房建单 403。
     */
    @Test
    void platformKeeperCountsPlatformWarehouseWithEvidenceButNotAnotherProjects() throws Exception {
        RequestPostProcessor keeper = account("kp", projectA, null, CREATE, RECORD, UPLOAD);
        RequestPostProcessor reviewer = account("rv", projectA, null, REVIEW);
        RequestPostProcessor outsider = account("os", projectB, null, CREATE, RECORD, REVIEW);

        String stocktake = id(create(keeper, platformWarehouse).andExpect(status().isOk()));
        assertThat(merchantOf(stocktake)).isNull();

        String photo = id(upload(keeper, stocktake, "现场照片.jpg", "image/jpeg", "photo").andExpect(status().isOk()));
        String video = id(upload(keeper, stocktake, "现场视频.mp4", "video/mp4", "video").andExpect(status().isOk()));
        assertThat(jdbc.queryForList("SELECT merchant_id FROM evidence_attachments WHERE id IN (?,?)", String.class, photo, video))
                .hasSize(2).containsOnlyNulls();

        String body = "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + platformItem
                + "\",\"counted_qty\":8,\"reason\":\"shortage\",\"photo_attachment_ids\":[\"" + photo + "\"],\"video_attachment_ids\":[\"" + video + "\"]}";
        String variance = id(call(keeper, "/api/v1/inventory/stocktakes/" + stocktake + "/variances", body).andExpect(status().isOk()));
        // 范围闸在幂等之前：别的综合体的账号拿已成功的请求编号重放，照样 403。
        call(outsider, "/api/v1/inventory/stocktakes/" + stocktake + "/variances", body).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        // 复核的范围闸（PGM:744「后端接口必须独立校验」；PGM:948「盘点差异，都要看商户归属、合同范围和复核权限」）：
        // B 综合体的不挂商户账号持复核码，驳回 A 综合体总库盘点单上的待复核差异 403，差异仍待复核。
        call(outsider, "/api/v1/inventory/stocktakes/" + stocktake + "/variances/" + variance + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"rejected\"}").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(reviewStatusOf(variance)).isEqualTo("pending");

        call(reviewer, "/api/v1/inventory/stocktakes/" + stocktake + "/variances/" + variance + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"approved\"}").andExpect(status().isOk());
        assertThat(available(platformItem, platformWarehouse)).isEqualTo(8);
        call(keeper, "/api/v1/inventory/stocktakes/" + stocktake + "/close", "{\"idempotency_key\":\"" + UUID.randomUUID() + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        create(keeper, otherWarehouse).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stocktake WHERE warehouse_id=?", Long.class, otherWarehouse)).isZero();
    }

    /**
     * 384-R1：盘点单商户取库房的商户——平台侧账号给甲的吧台仓建的单挂甲、甲看得到；乙的商户账号给甲的吧台仓建单 403（修复前放行）；
     * 库房不存在 404；最高权限给甲的吧台仓建单也挂甲（修复前 403）。
     */
    @Test
    void stocktakeTakesWarehouseMerchantAndMerchantAccountsStayOnOwnWarehouses() throws Exception {
        RequestPostProcessor keeper = account("kp", projectA, null, CREATE);
        RequestPostProcessor barManager = account("bm", projectA, merchantA, VIEW, CREATE);
        RequestPostProcessor otherBar = account("ob", projectA, merchantB, CREATE, REVIEW);

        String byKeeper = id(create(keeper, barWarehouse).andExpect(status().isOk()));
        assertThat(merchantOf(byKeeper)).isEqualTo(merchantA);
        List<String> seenByBar = JsonPath.read(get(barManager, "/api/v1/inventory/stocktakes").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(seenByBar).contains(byKeeper);

        create(otherBar, barWarehouse).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        // 复核的范围闸：乙的商户账号持复核码，驳回甲盘点单上的待复核差异 403，差异仍待复核。
        String pending = variance(byKeeper, barItem, "pending", -2000L);
        call(otherBar, "/api/v1/inventory/stocktakes/" + byKeeper + "/variances/" + pending + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"rejected\"}").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(reviewStatusOf(pending)).isEqualTo("pending");
        create(barManager, "r384wx_" + s).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("WAREHOUSE_NOT_FOUND"));

        String byMaster = id(create(master(), barWarehouse).andExpect(status().isOk()));
        assertThat(merchantOf(byMaster)).isEqualTo(merchantA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stocktake WHERE warehouse_id=?", Long.class, barWarehouse)).isEqualTo(2L);
    }

    /**
     * 384-R2：财务 BP 过账 A 综合体甲吧台仓与总库的差异，台账行带 A 综合体、商户取盘点单商户，自己在往来台账里看得到；
     * B 综合体的差异 403、不落台账不回填不落幂等记录；商户账号照旧只过本店的。
     */
    @Test
    void financeBpPostsReachableVariancesAndLedgerRowCarriesWarehouseProject() throws Exception {
        String bar = stocktake(barWarehouse, merchantA);
        String platform = stocktake(platformWarehouse, null);
        String other = stocktake(otherWarehouse, null);
        String barVariance = variance(bar, barItem, "approved", -2000L);
        String platformVariance = variance(platform, platformItem, "approved", -3000L);
        String otherVariance = variance(other, platformItem, "approved", -4000L);
        RequestPostProcessor bp = account("bp", projectA, null, VIEW, POST, "finance.ar_ap.view");

        String barRow = id(postVariance(bp, barVariance, merchantA, UUID.randomUUID().toString()).andExpect(status().isOk()));
        assertThat(jdbc.queryForMap("SELECT merchant_id, project_id FROM ar_ap_items WHERE id=?", barRow))
                .containsEntry("merchant_id", merchantA).containsEntry("project_id", projectA);
        assertThat(postedOf(barVariance)).isEqualTo(barRow);
        List<String> listed = JsonPath.read(get(bp, "/api/v1/finance/ar-ap-items?size=200").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(listed).contains(barRow);

        String rejectedKey = UUID.randomUUID().toString();
        postVariance(bp, otherVariance, "r384cp_" + s, rejectedKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(postedOf(otherVariance)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id=?", Long.class, otherVariance)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key LIKE ?", Long.class,
                "%" + rejectedKey + "%")).isZero();

        String platformRow = id(postVariance(bp, platformVariance, "r384cp_" + s, UUID.randomUUID().toString()).andExpect(status().isOk()));
        assertThat(jdbc.queryForMap("SELECT merchant_id, project_id FROM ar_ap_items WHERE id=?", platformRow))
                .containsEntry("merchant_id", null).containsEntry("project_id", projectA);

        // 商户账号照旧：甲过本店差异，乙过甲的 403。
        String ownVariance = variance(bar, barItem, "approved", -1000L);
        String foreignVariance = variance(bar, barItem, "approved", -500L);
        postVariance(account("mf", projectA, merchantA, POST), ownVariance, merchantA, UUID.randomUUID().toString())
                .andExpect(status().isOk());
        postVariance(account("of", projectA, merchantB, POST), foreignVariance, merchantB, UUID.randomUUID().toString())
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /**
     * 384-R3：差异照片视频按盘点码与盘点单范围回读——财务 BP 看得到甲上传到甲盘点单上的、看不到 B 综合体盘点单上的；不持盘点查看码的 403；
     * 乙的商户账号看不到甲的；甲的主管看得到平台侧账号传到甲盘点单上的（附件不挂商户）。
     */
    @Test
    void varianceEvidenceIsReadByStocktakeScopeNotByAttachmentMerchant() throws Exception {
        String bar = stocktake(barWarehouse, merchantA);
        String other = stocktake(otherWarehouse, null);
        String byBar = id(upload(account("mk", projectA, merchantA, RECORD, UPLOAD), bar, "吧台照片.jpg", "image/jpeg", "photo")
                .andExpect(status().isOk()));
        String byPlatform = id(upload(account("pk", projectA, null, RECORD, UPLOAD), bar, "总部照片.jpg", "image/jpeg", "photo")
                .andExpect(status().isOk()));
        String onOther = id(upload(account("ok", projectB, null, RECORD, UPLOAD), other, "外项目照片.jpg", "image/jpeg", "photo")
                .andExpect(status().isOk()));

        RequestPostProcessor bp = account("bp", projectA, null, READ_FILE, VIEW);
        file(bp, byBar).andExpect(status().isOk());
        file(bp, onOther).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        file(account("tv", projectA, null, READ_FILE), byBar).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        file(account("ob", projectA, merchantB, READ_FILE, VIEW), byBar).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        file(account("sv", projectA, merchantA, READ_FILE, VIEW), byPlatform).andExpect(status().isOk());
    }

    // ---------- 夹具 ----------

    private void warehouse(String id, String project, String type, String merchant) {
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,?,'盘点范围回归库房','active',?)",
                id, project, type, merchant);
    }

    private void balance(String item, String warehouse, String merchant, String project) {
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())",
                "r384bal_" + warehouse, item, warehouse, merchant, project, warehouse);
    }

    /** 采购到货流水（与采购收货写入的行同形）：高价值判定看最近一次到货价，差异金额估算看当前平均进货价。 */
    private void arrival(String item, String warehouse, long unitCostCent, String merchant, String project) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("r384mv_" + warehouse, "inbound", item, null, warehouse, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    private String stocktake(String warehouse, String merchant) {
        String id = "r384st_" + UUID.randomUUID().toString().substring(0, 8) + "_" + s;
        jdbc.update("INSERT INTO stocktake(id,stocktake_no,warehouse_id,merchant_id,business_date,status,created_at,updated_at) VALUES(?,?,?,?,current_date,'counting',now(),now())",
                id, "ST" + id, warehouse, merchant);
        return id;
    }

    private String variance(String stocktake, String item, String reviewStatus, long amountCent) {
        String id = "r384va_" + UUID.randomUUID().toString().substring(0, 8) + "_" + s;
        jdbc.update("INSERT INTO inventory_variance(id,stocktake_id,item_id,book_qty,counted_qty,variance_qty,variance_amount_cent,reason,review_status,recorded_by) VALUES(?,?,?,10,8,-2,?,'shortage',?,'r384_recorder')",
                id, stocktake, item, amountCent, reviewStatus);
        return id;
    }

    private String merchantOf(String stocktake) {
        return jdbc.queryForObject("SELECT merchant_id FROM stocktake WHERE id=?", String.class, stocktake);
    }

    private String postedOf(String variance) {
        return jdbc.queryForObject("SELECT posted_ar_ap_item_id FROM inventory_variance WHERE id=?", String.class, variance);
    }

    private String reviewStatusOf(String variance) {
        return jdbc.queryForObject("SELECT review_status FROM inventory_variance WHERE id=?", String.class, variance);
    }

    private int available(String item, String warehouse) {
        Integer value = jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                Integer.class, item, warehouse);
        return value == null ? -1 : value;
    }

    // ---------- 请求 ----------

    private ResultActions create(RequestPostProcessor who, String warehouse) throws Exception {
        return call(who, "/api/v1/inventory/stocktakes",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"warehouse_id\":\"" + warehouse + "\",\"remark\":\"月末盘点\"}");
    }

    private ResultActions postVariance(RequestPostProcessor who, String variance, String counterparty, String key) throws Exception {
        return call(who, "/api/v1/finance/inventory-variances/post",
                "{\"idempotency_key\":\"" + key + "\",\"inventory_variance_id\":\"" + variance + "\",\"counterparty_id\":\"" + counterparty + "\"}");
    }

    /** 盘点页上传差异证据的真实形状：multipart，业务类型 inventory_variance，业务编号为所选盘点单。 */
    private ResultActions upload(RequestPostProcessor who, String stocktake, String name, String contentType, String role) throws Exception {
        return mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                .file(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes(StandardCharsets.UTF_8)))
                .param("business_type", "inventory_variance").param("business_id", stocktake).param("attachment_role", role)
                .with(who));
    }

    private ResultActions file(RequestPostProcessor who, String attachment) throws Exception {
        return get(who, "/api/v1/evidence/attachments/" + attachment + "/file");
    }

    private ResultActions get(RequestPostProcessor who, String path) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(who));
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String id(ResultActions result) throws Exception {
        JsonNode root = json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("id").asText();
    }

    /** 后台账号（真库建角色、权限码、岗位职级）；merchantId 为空＝不挂商户的综合体员工（财务 BP、平台库管）。 */
    private RequestPostProcessor account(String tag, String project, String merchantId, String... permissions) {
        String userId = "r384u_" + tag + "_" + s;
        String role = "r384r_" + tag + "_" + s;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'盘点范围回归','project','active',now())",
                role, "盘点范围回归角色" + tag + s);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        return web(new AccessActor(userId, userId, "staff", project, merchantId, "active"));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        return web(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"));
    }

    private RequestPostProcessor web(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
