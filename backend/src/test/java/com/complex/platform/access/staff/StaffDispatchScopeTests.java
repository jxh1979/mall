package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.complex.platform.access.*;
import com.complex.platform.access.staff.dto.StaffDispatchCreateRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StaffDispatchScopeTests {
    final JdbcStaffRepository repo = mock(JdbcStaffRepository.class);
    final AccessRepository access = mock(AccessRepository.class);
    final AuthorizationService auth = mock(AuthorizationService.class);
    final PlatformRepository platform = mock(PlatformRepository.class);
    StaffService service;
    AccessActor actor = new AccessActor("manager", "manager", "staff", "company", null, "active");

    @BeforeEach
    void setup() {
        var idem = mock(IdempotencyService.class);
        when(idem.execute(any(), any(), any(), any(), any())).thenAnswer(i -> ((Supplier<?>)i.getArgument(3)).get());
        service = new StaffService(repo, access, idem, mock(AuditEventService.class), auth, AccessCredentialProvisioner.noop());
        when(auth.currentActor()).thenAnswer(i -> actor);
        when(auth.requireProjectScope(any())).thenReturn("company");
        var registrations = mock(StaffRegistrationService.class);
        when(registrations.actorCompany(any())).thenReturn("company");
        ReflectionTestUtils.setField(service, "registrations", registrations);
        ReflectionTestUtils.setField(service, "platformRepository", platform);
        var employee = new AccessUser("employee", "employee", "staff", "company", null, "active", Instant.EPOCH);
        when(access.findUserById("employee")).thenReturn(Optional.of(employee));
        when(auth.canReachUser(any(AccessActor.class), eq(employee), eq("staff.profile.review"))).thenReturn(true);
        when(auth.canReachProject(any(), eq("projectA"), eq("staff.profile.review"))).thenReturn(true);
        var profile = new StaffProfileRecord("profile", "company", null, "employee", "员工", null, null, null,
                "approved", null, null, null, "active", Instant.EPOCH, Instant.EPOCH).withCompanyRoute("company", "company_self");
        when(repo.findProfileByUserId("employee")).thenReturn(Optional.of(profile));
        when(repo.findProfileById("profile")).thenReturn(Optional.of(profile));
        when(repo.saveDispatch(any())).thenAnswer(i -> i.getArgument(0));
        var merchant = mock(MerchantRecord.class);
        when(merchant.id()).thenReturn("bar");
        when(merchant.merchantType()).thenReturn("bar");
        when(merchant.projectId()).thenReturn("projectA");
        when(merchant.status()).thenReturn("active");
        when(platform.findMerchantById("bar")).thenReturn(Optional.of(merchant));
    }

    @Test void merchantAccountCannotRegisterDispatchEvenWhenGrantedThePermission() {
        actor = new AccessActor("manager", "manager", "merchant_admin", "company", "bar", "active");
        var profile = new StaffProfileRecord("profile", "company", "bar", "employee", "员工", null, null, null,
                "approved", null, null, null, "active", Instant.EPOCH, Instant.EPOCH).withCompanyRoute("company", "company_self");
        when(repo.findProfileByUserId("employee")).thenReturn(Optional.of(profile));
        when(repo.findProfileById("profile")).thenReturn(Optional.of(profile));
        assertThatThrownBy(this::create).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("STAFF_DISPATCH_PLATFORM_REQUIRED"));
        verify(repo, never()).saveDispatch(any());
    }

    @Test void targetMerchantMustExistAndBeInTheAuthorizedProject() {
        when(auth.canReachProject(any(), eq("projectA"), eq("staff.profile.review"))).thenReturn(false);
        assertThatThrownBy(this::create).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        verify(repo, never()).saveDispatch(any());
    }

    @Test void employeeMustBeWithinTheAuthorizedUserScope() {
        when(auth.canReachUser(any(AccessActor.class), any(AccessUser.class), eq("staff.profile.review"))).thenReturn(false);
        assertThatThrownBy(this::create).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
    }

    @Test void acceptedDispatchLocksTheDestinationProjectRatherThanEmployeeCompany() {
        assertThat(create().projectId()).isEqualTo("projectA");
    }

    @Test void listCannotLeakAnotherProjectEvenWhenCompanyScopedQueriesWouldBeUnfiltered() {
        var reach = AuthorizationService.ProjectReach.ownOnly("projectA");
        when(auth.projectReach(actor, "staff.profile.view")).thenReturn(reach);
        var first = new StaffDispatchRecord("one", "employee", "company", "bar", "projectA",
                java.time.LocalDate.parse("2099-06-01"), null, "active", null, "manager", Instant.EPOCH, null, null);
        var other = new StaffDispatchRecord("two", "employee", "company", "bar2", "projectB",
                first.startDate(), null, "active", null, "manager", Instant.EPOCH, null, null);
        when(repo.listDispatches(null, null, "projectA")).thenReturn(java.util.List.of(first, other));
        when(auth.canReachUser(eq(reach), any(AccessUser.class))).thenReturn(true);
        assertThat(service.listDispatches(null, null).items()).extracting(StaffDispatchRecord::id).containsExactly("one");
    }

    @Test void endingDispatchAlsoChecksPlatformSideBeforeReadingTheRecord() {
        actor = new AccessActor("manager", "manager", "merchant_admin", "company", "bar", "active");
        assertThatThrownBy(() -> service.endDispatch("record",
                new com.complex.platform.access.staff.dto.StaffDispatchEndRequest("end", "2099-06-02", "结束")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STAFF_DISPATCH_PLATFORM_REQUIRED"));
        verify(repo, never()).findDispatchById(anyString());
    }

    private StaffDispatchRecord create() {
        return service.createDispatch(new StaffDispatchCreateRequest("dispatch-scope", "employee", "bar", "2099-06-01", "派驻"));
    }
}
