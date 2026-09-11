package com.complex.platform.governance;
// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * D1 场地营业时间配置的入参闸（V226 建表后一直无界面，本页是唯一配置入口）。
 *
 * <p>证伪点全是「配错了会让整个场地当天进不了人」的那几种写法：
 * 星期几越界、时刻不是 HH:mm、开歇同刻（零长营业窗口，页面上看着像配好了、实际谁都进不来）。
 * 跨零点（歇业早于开业）必须放行——酒吧营业到次日凌晨是常态，误拦等于把夜场全封了。
 */
class VenueBusinessHoursServiceTests {

    /** 只记录落库参数的假仓库（父类 jdbcTemplate 传 null：本用例一条 SQL 都不该发出去）。 */
    private static final class RecordingRepository extends VenueBusinessHoursRepository {
        private final List<VenueBusinessHoursRecord> saved = new ArrayList<>();

        RecordingRepository() {
            super(null);
        }

        private final List<String> projectQueries = new ArrayList<>();

        @Override
        Optional<String> findVenueProjectId(String venueId) {
            return Optional.ofNullable(Map.of("sp_bar_main", "prj_1", "sp_bar_side", "prj_2").get(venueId));
        }

        @Override
        List<VenueBusinessHoursRecord> listAll() {
            return new ArrayList<>(saved);
        }

        @Override
        List<VenueBusinessHoursRecord> listByProject(String projectId) {
            projectQueries.add(projectId);
            return saved.stream().filter(r -> projectId.equals(findVenueProjectId(r.venueId()).orElse(null))).toList();
        }

        @Override
        VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                        LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
            VenueBusinessHoursRecord record = new VenueBusinessHoursRecord(
                    id, venueId, dayOfWeek, openTime.toString(), closeTime.toString(), disabled,
                    effectiveFrom, now, now);
            saved.add(record);
            return record;
        }
    }

    private RecordingRepository repository;
    private VenueBusinessHoursService service;

    private VenueBusinessHoursService service() {
        return service(null);
    }

    private VenueBusinessHoursService service(String projectScope) {
        repository = new RecordingRepository();
        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class), auth(projectScope));
        return service;
    }

    private static AuthorizationService auth(String projectScope) {
        return new AuthorizationService(null) {
            @Override
            public Optional<String> currentProjectScopeQuietly() {
                return Optional.ofNullable(projectScope);
            }
        };
    }

    // M015-009：场地必须是现存空间（spaces.id），且项目员工只能读写本项目场地（BEQR-152）；修复前此三例红
    @Test
    void rejectsUnknownVenue() {
        assertThatThrownBy(() -> service().save(
                new VenueBusinessHoursRequest("idem-1", "sp_missing", 1, "18:00", "23:00", false, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VENUE_NOT_FOUND"));
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void rejectsVenueOutsideProjectScope() {
        assertThatThrownBy(() -> service("prj_2").save(request(1, "18:00", "23:00")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void listFiltersByProjectScope() {
        VenueBusinessHoursService scoped = service("prj_1");
        scoped.save(request(1, "18:00", "23:00"));
        repository.saved.add(new VenueBusinessHoursRecord(
                "vbh_side", "sp_bar_side", 1, "18:00", "23:00", false, null, Instant.EPOCH, Instant.EPOCH));

        assertThat(scoped.list()).extracting(VenueBusinessHoursRecord::venueId).containsOnly("sp_bar_main");
        assertThat(repository.projectQueries).containsExactly("prj_1");
    }

    private VenueBusinessHoursRequest request(Integer dayOfWeek, String open, String close) {
        return new VenueBusinessHoursRequest("idem-1", "sp_bar_main", dayOfWeek, open, close, false, null);
    }

    @Test
    void savesNormalBusinessWindow() {
        VenueBusinessHoursRecord saved = service().save(request(5, "18:00", "23:30"));
        assertThat(saved.venueId()).isEqualTo("sp_bar_main");
        assertThat(saved.dayOfWeek()).isEqualTo(5);
        assertThat(saved.openTime()).isEqualTo("18:00");
        assertThat(saved.closeTime()).isEqualTo("23:30");
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void allowsWindowCrossingMidnight() {
        // 20:00 开、次日 02:00 歇 —— 夜场常态，拦掉就等于夜里没人能进场。
        VenueBusinessHoursRecord saved = service().save(request(6, "20:00", "02:00"));
        assertThat(saved.openTime()).isEqualTo("20:00");
        assertThat(saved.closeTime()).isEqualTo("02:00");
    }

    @Test
    void rejectsIdenticalOpenAndCloseTime() {
        // 零长营业窗口：表里有行、页面上看着配好了，实际这一天谁都核销不了。宁可当场拒。
        assertThatThrownBy(() -> service().save(request(1, "20:00", "20:00")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void rejectsDayOfWeekOutOfRange() {
        assertThatThrownBy(() -> service().save(request(7, "18:00", "23:00")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service().save(request(null, "18:00", "23:00")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsMalformedTimeAndMissingVenue() {
        assertThatThrownBy(() -> service().save(request(1, "晚上八点", "23:00")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service().save(request(1, "18:00", null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service().save(
                new VenueBusinessHoursRequest("idem-1", "  ", 1, "18:00", "23:00", false, null)))
                .isInstanceOf(ApiException.class);
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void savesDisabledDayWithoutDeletingConfiguration() {
        // 「本日不营业」= 停用，配置行保留可随时恢复（不物理删配置）。
        VenueBusinessHoursRecord saved = service().save(
                new VenueBusinessHoursRequest("idem-1", "sp_bar_main", 2, "18:00", "23:00", true, null));
        assertThat(saved.disabled()).isTrue();
        assertThat(repository.saved).hasSize(1);
    }
}
