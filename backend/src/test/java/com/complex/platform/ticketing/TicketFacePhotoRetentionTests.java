package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.NotificationWindowConfig;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * V353 人脸照留存天数后台可配（用户 2026-07-28 拍板："改成后台可配，默认 30 天"）。
 *
 * <p>证伪点：purge_after 必须 = 票 valid_to + 后台配置的天数。若有人把
 * {@code windowConfig.days(...)} 改回硬编码 30，第一个用例（配 45 天）立刻红。
 * 第二个用例锁住"缺配置兜底 30 = 零行为改变"。
 */
class TicketFacePhotoRetentionTests {

    private static final Instant VALID_TO = Instant.parse("2026-08-01T00:00:00Z");

    private LocalDate capturePurgeAfter(NotificationWindowConfig windowConfig) {
        TicketFacePhotoRepository repository = mock(TicketFacePhotoRepository.class);
        PiiCipher piiCipher = mock(PiiCipher.class);
        when(piiCipher.encrypt(anyString())).thenReturn("enc:payload");
        TicketFacePhotoService service = new TicketFacePhotoService(
                repository, piiCipher, mock(AuthorizationService.class), mock(AuditEventService.class), windowConfig);

        service.capture("ent_1", "Zm9v", "image/jpeg", Instant.parse("2026-07-01T00:00:00Z"), VALID_TO);

        ArgumentCaptor<LocalDate> purgeAfter = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).save(anyString(), eq("ent_1"), any(), eq("image/jpeg"), any(), purgeAfter.capture());
        return purgeAfter.getValue();
    }

    @Test
    void purgeAfterFollowsBackofficeConfiguredRetentionDays() {
        NotificationWindowConfig windowConfig = mock(NotificationWindowConfig.class);
        when(windowConfig.days(eq("ticketing.face_photo_retention"), eq(30))).thenReturn(45);

        assertThat(capturePurgeAfter(windowConfig)).isEqualTo(LocalDate.of(2026, 9, 15)); // 2026-08-01 + 45 天
    }

    @Test
    void purgeAfterFallsBackToThirtyDaysWhenUnconfigured() {
        // 无库/未配置：NotificationWindowConfig 空构造一律回落调用方默认 → 与 V353 前行为一致。
        assertThat(capturePurgeAfter(new NotificationWindowConfig())).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    /**
     * V368 告知版本号跟着留存天数走：客人拍照前被告知的「留多久」是后台可配的，
     * 今天告知 30 天、明天运营改成 60 天，这两批客人同意的不是同一件事——版本号必须能区分，
     * 否则同意台账里只剩一个「同意过」的空壳，答不出「同意的是哪一版告知」。
     */
    @Test
    void noticeVersionCarriesTheRetentionDaysActuallyToldToTheCustomer() {
        NotificationWindowConfig windowConfig = mock(NotificationWindowConfig.class);
        when(windowConfig.days(eq("ticketing.face_photo_retention"), eq(30))).thenReturn(60);
        TicketFacePhotoService service = new TicketFacePhotoService(
                mock(TicketFacePhotoRepository.class), mock(PiiCipher.class),
                mock(AuthorizationService.class), mock(AuditEventService.class), windowConfig);

        assertThat(service.noticeVersion()).isEqualTo("face_photo_notice_v1@retention_60d");

        TicketFacePhotoService unconfigured = new TicketFacePhotoService(
                mock(TicketFacePhotoRepository.class), mock(PiiCipher.class),
                mock(AuthorizationService.class), mock(AuditEventService.class), new NotificationWindowConfig());
        assertThat(unconfigured.noticeVersion()).isEqualTo("face_photo_notice_v1@retention_30d");
    }
}
