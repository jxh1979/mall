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
import java.util.List;
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
        assertThat(thresholdRepository.findHighValueThresholdCent(null, null)).isEqualTo(500_000L);
    }

    @Test
    void thresholdMissingReturnsNullSoDecisionFallsBackToHighValue() {
        jdbcTemplate.update("DELETE FROM high_value_thresholds");
        assertThat(thresholdRepository.findHighValueThresholdCent(null, null)).isNull();
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
