package com.complex.platform.support;

import com.complex.platform.access.AccessActor;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test utility for injecting security principals into SecurityContextHolder.
 * Use in @BeforeEach / @AfterEach with clear() for clean teardown.
 * Compatible with any service test that uses AuthorizationService.currentMerchantScope()
 * or AuthorizationService.isPlatformAdmin() without a full Spring context.
 */
public final class TestSecurityActors {

    private TestSecurityActors() {
    }

    /** Sets a platform_admin actor — passes isPlatformAdmin checks, scope returns Optional.empty(). */
    public static void asPlatformAdmin() {
        AccessActor actor = new AccessActor(
                "usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    /** Platform admin with an explicit user id — for two-admin segregation-of-duties (initiator vs approver) tests. */
    public static void asPlatformAdmin(String userId) {
        AccessActor actor = new AccessActor(
                userId, userId + "_user", "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    /** Sets a merchant actor — currentMerchantScope() returns Optional.of(merchantId). */
    public static void asMerchantUser(String merchantId) {
        AccessActor actor = new AccessActor(
                "usr_" + merchantId, "merchant_user", "staff", "org_1", merchantId, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    /** Sets an actor with the given supervisor level encoded in accountType. */
    public static void asSupervisorLevel(int level) {
        AccessActor actor = new AccessActor(
                "usr_supervisor_l" + level, "supervisor_l" + level + "_user",
                "supervisor_l" + level, "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    /**
     * Sets a supervisor actor with an explicit user id at the given level — use when a test needs
     * two distinct supervisors at the same level (e.g. segregation of duties: initiator vs reviewer).
     */
    public static void asSupervisor(String userId, int level) {
        AccessActor actor = new AccessActor(
                userId, userId + "_user", "supervisor_l" + level, "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    /** Clears the security context. Call from @AfterEach to avoid cross-test contamination. */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
