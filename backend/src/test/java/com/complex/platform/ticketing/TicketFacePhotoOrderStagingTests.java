package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import com.complex.platform.access.AccessActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V359 客人端下单拍脸照 → 出票转绑（真库 + 真 PiiCipher）。
 *
 * <p>为什么必须有这条链：本仓出票只有后台一条路（ticket.ticket.issue），客人永远调不到。
 * 客人在 H5 上拍的脸照如果没有「订单暂存位」，线上买的游乐票就永远拿不到本人照片，
 * 核销员比对的是空气。以下用例就是这条链的证伪点：
 * <ul>
 *   <li>① 暂存密文≠明文，出票时自动取用并转绑到已发权益（出票请求本身不带照片）；</li>
 *   <li>② 转绑后订单暂存位清空——生物识别数据不留第二份；</li>
 *   <li>③ 暂存张数超过订单票数直接拒（客人端是信任边界，不能任人塞）；</li>
 *   <li>④ 非法 base64 / 超大 payload 直接拒，不进库。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketFacePhotoOrderStagingTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketOrderService orderService;
    @Autowired private TicketFacePhotoService facePhotoService;
    @Autowired private PiiCipher piiCipher;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final byte[] RAW_IMAGE_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    private static final String PHOTO_BASE64 = Base64.getEncoder().encodeToString(RAW_IMAGE_BYTES);

    // ---------- ① 暂存加密 + 出票自动取用转绑；② 转绑后暂存清空 ----------
    @Test
    void stagedOrderPhotoIsEncryptedThenTransferredToEntitlementOnIssue() {
        String orderId = seedPaidOrder();

        facePhotoService.stageForOrder(orderId, List.of(PHOTO_BASE64), 1);

        // 暂存位存的是密文，不是原图、也不是原图 base64 明文。
        byte[] staged = jdbcTemplate.queryForObject(
                "SELECT photo_ciphertext FROM ticket_face_photos WHERE order_id = ? AND slot_index = 0",
                byte[].class, orderId);
        assertThat(staged).isNotNull();
        assertThat(staged).isNotEqualTo(RAW_IMAGE_BYTES);
        assertThat(staged).isNotEqualTo(PHOTO_BASE64.getBytes(StandardCharsets.UTF_8));
        assertThat(new String(staged, StandardCharsets.UTF_8)).startsWith("enc:v1:");
        assertThat(facePhotoService.stagedForOrder(orderId)).containsExactly(PHOTO_BASE64);

        // 出票请求自己不带照片 —— 线上买票的客人不在柜台，运营手上没有他的脸。
        authenticateAs("platform_admin");
        orderService.issueOrder(orderId, new StatusChangeRequest(key(), "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = repository.listIssuedEntitlementsByOrderId(orderId);
        assertThat(issued).hasSize(1);
        byte[] bound = jdbcTemplate.queryForObject(
                "SELECT photo_ciphertext FROM ticket_face_photos WHERE entitlement_id = ?",
                byte[].class, issued.get(0).id());
        assertThat(bound).isNotNull();
        assertThat(piiCipher.decrypt(new String(bound, StandardCharsets.UTF_8))).isEqualTo(PHOTO_BASE64);

        // 转绑完成后暂存位必须空 —— 同一张脸不留两份。
        assertThat(stagedRowCount(orderId)).isZero();
        assertThat(facePhotoService.stagedForOrder(orderId)).isEmpty();
    }

    // ---------- ③ 张数超过订单票数 → 拒（客人端可任意构造 payload） ----------
    @Test
    void stagingMorePhotosThanTicketQuantityIsRejected() {
        String orderId = seedPaidOrder();
        assertThatThrownBy(() -> facePhotoService.stageForOrder(orderId, List.of(PHOTO_BASE64, PHOTO_BASE64), 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_COUNT_EXCEEDED"));
        assertThat(stagedRowCount(orderId)).isZero();
    }

    // ---------- ④ 非法 base64 / 超大 payload → 拒，不进库 ----------
    @Test
    void stagingRejectsNonBase64AndOversizedPayload() {
        String orderId = seedPaidOrder();

        assertThatThrownBy(() -> facePhotoService.stageForOrder(orderId, List.of("这不是 base64!!"), 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_INVALID"));

        String oversized = "A".repeat(4_000_004); // 超过单张 4,000,000 字符上限
        assertThatThrownBy(() -> facePhotoService.stageForOrder(orderId, List.of(oversized), 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_TOO_LARGE"));

        assertThat(stagedRowCount(orderId)).isZero();
    }

    // ================= 夹具 =================

    /** 一张已支付、绑了「要脸照」权益模板的 admin 渠道订单（可直接出票）。 */
    private String seedPaidOrder() {
        Instant now = Instant.now();
        String suffix = key();
        String ticketTypeId = "tt_stage_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "stage-tt", "flat", 5000L, 5000L, 5000L, false, "active",
                1, 60, now, now, null, 0));
        String templateId = "tpl_stage_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                templateId, "amusement-tpl", "amusement", 1, 5000L, "active",
                null, null, null, null, now, now, "virtual", null, "per_use", null, true));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "bind_stage_" + suffix, ticketTypeId, templateId, 1, 1, 5000L, "default", 1, 5000L, now, now));
        String orderId = "ticket_order_stage_" + suffix;
        repository.saveTicketOrder(new TicketOrderRecord(
                orderId, "TOST" + suffix, null, ticketTypeId, null, 1, LocalDate.now(), "admin", null,
                5000L, null, 5000L, 0L, "CNY", "paid", "not_issued", "created", now, now, null));
        return orderId;
    }

    private int stagedRowCount(String orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_face_photos WHERE order_id = ?", Integer.class, orderId);
        return count == null ? 0 : count;
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String accountType) {
        AccessActor actor = new AccessActor("user_" + key(), "u", accountType, "seed-org", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
