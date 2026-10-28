package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.bar.BarVoidReasonCodeRecord;
import com.complex.platform.bar.JdbcBarVoidReasonCodeRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.settlement.JdbcHighValueThresholdRepository;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * V350/V351 验收（走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚）：
 * ① 全平台唯一阈值表 {@code high_value_thresholds} 出厂种子 = 5000 元，删光后返回 null（判定侧据此一律拦下）；
 * ①b 判定基准 = 该物品最近一次采购入库流水的实际到货价（不用滚动均价），大于或等于阈值即高价值（BSM「高价值物品」）；
 * ② 退酒原因码字典出厂八条、只认生效码、下线必填理由、下线后不可再选；
 * ③ 证据附件真上传后存在性/类型校验通过，假 id 被拒。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class HighValueUnificationJdbcIntegrationTests {

    @Autowired
    private JdbcHighValueThresholdRepository thresholdRepository;

    @Autowired
    private JdbcBarVoidReasonCodeRepository reasonCodeRepository;

    @Autowired
    private HighValueDecisionService highValueDecisionService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EvidenceEventService evidenceEventService;

    @Autowired
    private EvidenceAttachmentValidator evidenceAttachmentValidator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    // ========== ① 阈值：全平台一个固定金额，缺配置不静默放行 ==========

    @Test
    void factorySeedIsFiveThousandYuan() {
        assertThat(thresholdRepository.findHighValueThresholdCent()).isEqualTo(500_000L);
    }

    @Test
    void thresholdMissingReturnsNullSoDecisionFallsBackToHighValue() {
        jdbcTemplate.update("DELETE FROM high_value_thresholds");
        assertThat(thresholdRepository.findHighValueThresholdCent()).isNull();
        // 判定侧收到 null ⇒ 一律高价值拦下（业务HTML：阈值未配置不得静默放行）。
        HighValueDecisionService.HighValueDecision decision =
                highValueDecisionService.decide("item_does_not_exist", null);
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_THRESHOLD_UNSET);
    }

    @Test
    void unknownPurchaseCostIsHighValueEvenWithThresholdConfigured() {
        // 阈值有配置，但物品没有进货价基准 ⇒ 仍按高价值拦下。
        HighValueDecisionService.HighValueDecision decision =
                highValueDecisionService.decide("item_never_costed", null);
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
    }

    // ========== ①b 判定基准：最近一次实际到货价，大于或等于阈值即高价值 ==========
    // BSM「高价值物品」：以该物品最近一次实际到货价（不使用滚动变化的当前平均进货价）与后台配置的固定金额阈值比较，
    // 该到货价大于或等于阈值即为高价值、低于阈值即为低价值。到货价 = PurchaseRequestService.receive 写入的
    // movement_type=inbound / business_type=purchase_request 流水的 unit_cost_cent（真实列，非均价）。

    @Test
    void arrivalPriceEqualToThresholdIsHighValue() {
        String itemId = seedItem();
        Instant t0 = Instant.now().minusSeconds(60);
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "eq", 1, 500_000L, t0), null);

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(500_000L);
        assertThat(decision.thresholdCent()).isEqualTo(500_000L);
    }

    @Test
    void latestArrivalPriceDecidesEvenWhenMovingAverageIsBelowThreshold() {
        String itemId = seedItem();
        Instant t0 = Instant.now().minusSeconds(60);
        // 先 10 件 @4000 元，再 1 件 @6000 元：滚动均价 (40000+6000)/11 ≈ 4181 元 < 阈值，最近一次到货价 6000 元 ≥ 阈值。
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "old", 10, 400_000L, t0), null);
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "new", 1, 600_000L, t0.plusSeconds(1)), null);

        // 追到实际写入的列：均价确实低于阈值、最近一笔采购入库行的 unit_cost_cent 确实是 6000 元。
        Long avg = jdbcTemplate.queryForObject(
                "SELECT avg_unit_cost_cent FROM inventory_item_cost_state WHERE item_id = ?", Long.class, itemId);
        assertThat(avg).isLessThan(500_000L);
        Long latestRowCost = jdbcTemplate.queryForObject("""
                SELECT unit_cost_cent FROM inventory_stock_movements
                 WHERE item_id = ? AND movement_type = 'inbound' AND business_type = 'purchase_request'
                 ORDER BY created_at DESC LIMIT 1
                """, Long.class, itemId);
        assertThat(latestRowCost).isEqualTo(600_000L);

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(600_000L);
    }

    @Test
    void latestArrivalPriceBelowThresholdIsLowValueEvenWhenMovingAverageIsAbove() {
        String itemId = seedItem();
        Instant t0 = Instant.now().minusSeconds(60);
        // 先 1 件 @8000 元，再 1 件 @4000 元：均价 6000 元 ≥ 阈值，但最近一次到货价 4000 元 < 阈值 ⇒ 低价值。
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "old", 1, 800_000L, t0), null);
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "new", 1, 400_000L, t0.plusSeconds(1)), null);

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isFalse();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_BELOW_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(400_000L);
    }

    @Test
    void nonPurchaseInboundDoesNotCountAsArrival() {
        String itemId = seedItem();
        Instant t0 = Instant.now().minusSeconds(60);
        // 采购到货 1 件 @4000 元；其后一笔非采购来源入库（通用 stock-in，business_type=manual）@8000 元把均价推到 6000 元。
        // 非采购入库不是「到货」，最近一次实际到货价仍是 4000 元 ⇒ 低价值。
        inventoryRepository.saveStockMovement(purchaseInbound(itemId, "pr", 1, 400_000L, t0), null);
        inventoryRepository.saveStockMovement(inbound(itemId, "manual_in", 1, 800_000L, "manual", t0.plusSeconds(1)), null);
        Long avg = jdbcTemplate.queryForObject(
                "SELECT avg_unit_cost_cent FROM inventory_item_cost_state WHERE item_id = ?", Long.class, itemId);
        assertThat(avg).isGreaterThanOrEqualTo(500_000L);

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isFalse();
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(400_000L);
    }

    @Test
    void movingAverageAloneWithoutPurchaseInboundIsCostUnknown() {
        String itemId = seedItem();
        // 只有非采购来源入库建立了成本基准（均价 6000 元），从未有采购入库流水 ⇒ 到货价未知 ⇒ 按高价值拦下，不拿均价顶替。
        inventoryRepository.saveStockMovement(
                inbound(itemId, "manual_only", 1, 600_000L, "manual", Instant.now().minusSeconds(60)), null);
        Boolean hasBasis = jdbcTemplate.queryForObject(
                "SELECT has_cost_basis FROM inventory_item_cost_state WHERE item_id = ?", Boolean.class, itemId);
        assertThat(hasBasis).isTrue();

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
        assertThat(decision.purchaseUnitCostCent()).isNull();
    }

    @Test
    void purchaseInboundWithoutRecordedPriceIsCostUnknown() {
        String itemId = seedItem();
        // 采购入库确认时未录单价（unit_cost_cent 为空）⇒ 最近一次到货价未知 ⇒ 高价值。
        inventoryRepository.saveStockMovement(
                purchaseInbound(itemId, "nocost", 1, null, Instant.now().minusSeconds(60)), null);
        Long rowCost = jdbcTemplate.queryForObject(
                "SELECT unit_cost_cent FROM inventory_stock_movements WHERE item_id = ?", Long.class, itemId);
        assertThat(rowCost).isNull();

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
    }

    @Test
    void manualMarkStillWinsOverLowArrivalPrice() {
        String itemId = seedItem();
        inventoryRepository.saveStockMovement(
                purchaseInbound(itemId, "cheap", 1, 1_000L, Instant.now().minusSeconds(60)), null);
        jdbcTemplate.update("UPDATE inventory_items SET manual_high_value = TRUE WHERE id = ?", itemId);

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);
    }

    @Test
    void thresholdUnsetStillWinsOverLowArrivalPrice() {
        String itemId = seedItem();
        inventoryRepository.saveStockMovement(
                purchaseInbound(itemId, "cheap", 1, 1_000L, Instant.now().minusSeconds(60)), null);
        jdbcTemplate.update("DELETE FROM high_value_thresholds");

        HighValueDecisionService.HighValueDecision decision = highValueDecisionService.decide(itemId, "m_hv_it");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_THRESHOLD_UNSET);
    }

    private String seedItem() {
        String itemId = "item_hv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, min_quantity, status,
                                             created_at, updated_at, merchant_id)
                VALUES (?, '高价值判定测试物料', 'beverage', '瓶', 0, 'active', now(), now(), 'm_hv_it')
                """, itemId);
        return itemId;
    }

    /** 采购入库流水：与 PurchaseRequestService.receive → createPurchaseInbound 写入的行同形。 */
    private StockMovementRecord purchaseInbound(String itemId, String seed, int qty, Long unitCostCent, Instant at) {
        return inbound(itemId, "pr_" + seed, qty, unitCostCent, "purchase_request", at);
    }

    private StockMovementRecord inbound(String itemId, String seed, int qty, Long unitCostCent,
            String businessType, Instant at) {
        return new StockMovementRecord("mv_hv_" + seed + "_" + itemId, "inbound", itemId, null, "loc_hv_it", qty,
                unitCostCent, null, businessType, businessType.equals("purchase_request") ? "pr_" + itemId : null,
                null, null, List.of(), "completed", at, "m_hv_it");
    }

    // ========== ② 退酒原因码字典 ==========

    @Test
    void factorySeedsTheEightApprovedReasonCodes() {
        List<BarVoidReasonCodeRecord> codes = reasonCodeRepository.listReasonCodes(true);
        assertThat(codes).extracting(BarVoidReasonCodeRecord::code)
                .containsExactly("quality_defect", "wrong_item", "customer_change", "service_delay",
                        "damaged_in_service", "system_error", "goodwill", "other");
        // 上屏一律中文（「所有终端零英文」铁律）：标签不得是英文码本身。
        assertThat(codes).extracting(BarVoidReasonCodeRecord::labelZh)
                .containsExactly("酒品质量问题", "上错酒 / 出错单", "客人改单", "出品超时",
                        "服务中损坏", "系统/录单错误", "客诉安抚", "其他（必须同时填写说明）");
    }

    @Test
    void onlyDictionaryCodesAreActive() {
        assertThat(reasonCodeRepository.isActiveReasonCode("quality_defect")).isTrue();
        assertThat(reasonCodeRepository.isActiveReasonCode("asdf")).isFalse();
    }

    @Test
    void saveAddsNewCodeAndArchiveRequiresReasonThenDeactivates() {
        reasonCodeRepository.saveReasonCode("spill_accident", "洒漏事故", 90, "admin");
        assertThat(reasonCodeRepository.isActiveReasonCode("spill_accident")).isTrue();

        assertThatThrownBy(() -> reasonCodeRepository.archiveReasonCode("spill_accident", "  ", "admin"))
                .isInstanceOf(ApiException.class);

        BarVoidReasonCodeRecord archived =
                reasonCodeRepository.archiveReasonCode("spill_accident", "并入服务中损坏", "admin");
        assertThat(archived.status()).isEqualTo("inactive");
        assertThat(archived.archiveReason()).isEqualTo("并入服务中损坏");
        assertThat(reasonCodeRepository.isActiveReasonCode("spill_accident")).isFalse();
        // 软删不物理删：历史退酒单仍引用得到。
        assertThat(reasonCodeRepository.listReasonCodes(false))
                .extracting(BarVoidReasonCodeRecord::code).contains("spill_accident");
    }

    // ========== ③ 双证据闭环：先能传，才校验 ==========

    @Test
    void uploadedAttachmentPassesExistenceAndKindCheckWhileFakeIdIsRejected() {
        var photo = evidenceEventService.uploadAttachment(
                new MockMultipartFile("file", "evidence.jpg", "image/jpeg", "fake-bytes".getBytes()),
                "bar_order_item_void", null, "photo");
        var video = evidenceEventService.uploadAttachment(
                new MockMultipartFile("file", "evidence.mp4", "video/mp4", "fake-bytes".getBytes()),
                "bar_order_item_void", null, "video");

        // content_type 由服务端按扩展名判定，不信客户端自报。
        assertThat(photo.contentType()).isEqualTo("image/jpeg");
        assertThat(video.contentType()).isEqualTo("video/mp4");
        assertThat(photo.status()).isEqualTo("uploaded");

        evidenceAttachmentValidator.requirePhotos(List.of(photo.id()), "photo_attachment_ids");
        evidenceAttachmentValidator.requireVideos(List.of(video.id()), "video_attachment_ids");

        assertThatThrownBy(() -> evidenceAttachmentValidator.requireExisting(
                List.of("att_totally_made_up"), "evidence_attachment_ids"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EVIDENCE_ATTACHMENT_NOT_FOUND"));
        assertThatThrownBy(() -> evidenceAttachmentValidator.requirePhotos(
                List.of(video.id()), "photo_attachment_ids"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EVIDENCE_ATTACHMENT_KIND_MISMATCH"));
    }

    @Test
    void uploadRejectsUnsupportedFileType() {
        assertThatThrownBy(() -> evidenceEventService.uploadAttachment(
                new MockMultipartFile("file", "payload.exe", "application/octet-stream", "x".getBytes()),
                "bar_order_item_void", null, null))
                .isInstanceOf(ApiException.class);
    }
}
