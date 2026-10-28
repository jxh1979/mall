package com.complex.platform.governance;
// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
import com.complex.platform.support.InMemoryAuditEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
    private InMemoryAuditEventRepository auditEventRepository;
    private VenueBusinessHoursService service;

    private VenueBusinessHoursService service() {
        return service(null);
    }

    private VenueBusinessHoursService service(String projectScope) {
        repository = new RecordingRepository();
        // M017-008：真实 AuditEventService + 内存审计库（与 FloorPlanServiceTests 同构），审计事件可断言
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new VenueBusinessHoursService(repository, new AuditEventService(auditEventRepository), auth(projectScope));
        return service;
    }

    // M015-015：只替换 currentActor，scope 方法走真实现——projectScope 为空＝无 organizationId 的平台管理员（真 currentProjectScopeQuietly 会抛，服务须经 isPlatformAdmin 判全量）
    private static AuthorizationService auth(String projectScope) {
        return actorAuth(projectScope == null
                ? new AccessActor("usr_platform_admin", "platform-admin", "platform_admin", null, null, "active")
                : new AccessActor("usr_" + projectScope, projectScope + "-staff", "staff", projectScope, null, "active"));
    }

    private static AuthorizationService actorAuth(AccessActor actor) {
        return new AuthorizationService(null) {
            @Override
            public AccessActor currentActor() {
                return actor;
            }
            // 18E（第 19 条 19-R2）：服务改按入口权限码可触及的项目判；本类不挂可跨项目角色，只到本人项目（与现行等价）
            @Override
            public ProjectReach projectReach(AccessActor reachActor, String... permissionCodes) {
                return isPlatformAdmin(reachActor) ? super.projectReach(reachActor, permissionCodes)
                        : ProjectReach.ownOnly(requireProjectScope(reachActor));
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

    // M015-015：无 organizationId 的平台管理员列表取全量（修复前真 currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，本例与 service() 各例一起红）
    @Test
    void platformAdminWithoutOrganizationListsAllVenues() {
        VenueBusinessHoursService admin = service();
        admin.save(request(1, "18:00", "23:00"));
        repository.saved.add(new VenueBusinessHoursRecord(
                "vbh_side", "sp_bar_side", 1, "18:00", "23:00", false, null, Instant.EPOCH, Instant.EPOCH));

        assertThat(admin.list()).extracting(VenueBusinessHoursRecord::venueId).containsExactlyInAnyOrder("sp_bar_main", "sp_bar_side");
        assertThat(repository.projectQueries).isEmpty();
    }

    // M015-015：无项目归属的普通员工 fail-closed（红线④：不因缺归属放行全量）
    @Test
    void staffWithoutProjectScopeIsRejected() {
        repository = new RecordingRepository();
        VenueBusinessHoursService orphan = new VenueBusinessHoursService(repository, new AuditEventService(new InMemoryAuditEventRepository()),
                actorAuth(new AccessActor("usr_orphan", "orphan", "staff", null, null, "active")));

        assertThatThrownBy(orphan::list)
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> orphan.save(request(1, "18:00", "23:00")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
        assertThat(repository.saved).isEmpty();
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
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service().save(request(null, "18:00", "23:00")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedTimeAndMissingVenue() {
        assertThatThrownBy(() -> service().save(request(1, "晚上八点", "23:00")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service().save(request(1, "18:00", null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("VALIDATION_FAILED");
                    // M017-006：M015-006 已改的中文整句
                    assertThat(ex.getMessage()).isEqualTo("结束营业时间必须填写，格式 HH:mm");
                });
        assertThatThrownBy(() -> service().save(
                new VenueBusinessHoursRequest("idem-1", "  ", 1, "18:00", "23:00", false, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(ex.getMessage()).isEqualTo("场地不能为空");
                });
        assertThat(repository.saved).isEmpty();
    }

    // M017-008：保存/列表须写审计事件（GEN-011；修复前 mock 从未 verify）
    @Test
    void saveAndListWriteAuditEvents() {
        VenueBusinessHoursService svc = service();
        svc.save(request(3, "18:00", "23:00"));
        svc.list();

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("venue_business_hours.updated", "venue_business_hours.viewed");
    }

    // M017-008：平台管理员（项目 scope 为空）列表取全量，且 effective_from 原样落库
    @Test
    void adminListsAllVenuesAndEffectiveFromIsPersisted() {
        VenueBusinessHoursService svc = service();
        VenueBusinessHoursRecord saved = svc.save(
                new VenueBusinessHoursRequest("idem-1", "sp_bar_main", 4, "18:00", "23:00", false, LocalDate.of(2026, 10, 1)));
        svc.save(new VenueBusinessHoursRequest("idem-2", "sp_bar_side", 4, "18:00", "23:00", false, null));

        assertThat(saved.effectiveFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(svc.list()).extracting(VenueBusinessHoursRecord::venueId).containsExactlyInAnyOrder("sp_bar_main", "sp_bar_side");
        assertThat(repository.projectQueries).isEmpty();
    }

    // M017-007：真实仓库 SQL（spaces / venue_business_hours 表）——不存在场地 empty、存在场地返回项目、按项目过滤；事务回滚
    @Nested
    @SpringBootTest(properties = {
            "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
            "spring.datasource.username=complex",
            "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
    })
    @Transactional
    class RealRepositoryCases {

        @Autowired
        private VenueBusinessHoursRepository realRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void findVenueProjectIdAndListByProjectHitRealSpacesTable() {
            jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) VALUES "
                    + "('sp_vbh_a', 'prj_vbh_1', '吧台 A', 'shop', 'active', now(), now()), "
                    + "('sp_vbh_b', 'prj_vbh_2', '吧台 B', 'shop', 'active', now(), now())");

            assertThat(realRepository.findVenueProjectId("sp_vbh_missing")).isEmpty();
            assertThat(realRepository.findVenueProjectId("sp_vbh_a")).contains("prj_vbh_1");

            Instant now = Instant.parse("2026-06-14T00:00:00Z");
            realRepository.upsert("vbh_real_a", "sp_vbh_a", 1, LocalTime.of(18, 0), LocalTime.of(23, 0), false, null, now);
            realRepository.upsert("vbh_real_b", "sp_vbh_b", 1, LocalTime.of(18, 0), LocalTime.of(23, 0), false, null, now);

            assertThat(realRepository.listByProject("prj_vbh_1"))
                    .extracting(VenueBusinessHoursRecord::venueId)
                    .containsExactly("sp_vbh_a");
        }
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
