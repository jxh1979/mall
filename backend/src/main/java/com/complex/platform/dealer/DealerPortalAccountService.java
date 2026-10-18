package com.complex.platform.dealer;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessCredentialProvisioner;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerPortalAccountRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-11d 经销商门户账号（内部后台开通）与停用撤权。
 * 账号与内部员工端、商户门户隔离：account_type = dealer、只挂 role_dealer_portal（不含任何 dealer.* 内部权限）、归属 dealer_portal_accounts。
 * 停用生效时立即终止全部门户登录会话（ENUM:181 / BSM:1246），已完成订单与审计记录保留。
 */
@Service
public class DealerPortalAccountService {

    static final String MANAGE = "dealer.portal_account.manage";
    static final String ACCOUNT_TYPE = "dealer";
    static final String ROLE = "role_dealer_portal";

    private final JdbcDealerPortalRepository portalAccounts;
    private final JdbcDealerRepository dealers;
    private final AccessRepository access;
    private final AccessCredentialProvisioner credentials;
    private final SessionIssuer sessions;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public DealerPortalAccountService(JdbcDealerPortalRepository portalAccounts, JdbcDealerRepository dealers, AccessRepository access,
            ObjectProvider<AccessCredentialProvisioner> credentials, SessionIssuer sessions, AuthorizationService authorization,
            IdempotencyService idempotency, AuditEventService audit) {
        this.portalAccounts = portalAccounts;
        this.dealers = dealers;
        this.access = access;
        this.credentials = credentials.getIfAvailable(AccessCredentialProvisioner::noop);
        this.sessions = sessions;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    public PageResponse<DealerPortalAccountRecord> listPortalAccounts(String dealerId) {
        authorization.requirePermission(MANAGE);
        requireDealer(dealerId);
        List<DealerPortalAccountRecord> items = portalAccounts.listByDealer(dealerId);
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    /** 未四审的账号仅预建为禁用；准入批准后才启用。 */
    @Transactional
    public DealerPortalAccountRecord createPortalAccount(String dealerId, DealerPortalAccountRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            requireDealer(dealerId);
            DealerRecord dealer = dealers.lockDealer(dealerId).orElseThrow();
            if (DealerService.DEACTIVATED.equals(dealer.status())) {
                throw ApiException.conflict("DEALER_DEACTIVATED", "经销商已停用，不恢复旧账号也不开新门户账号");
            }
            String username = requireText(request == null ? null : request.username(), "username").trim();
            String password = requireText(request.password(), "password");
            if (portalAccounts.usernameExists(username)) {
                throw ApiException.conflict("USERNAME_EXISTS", "登录名已被使用");
            }
            return idempotency.execute("dealerPortalAccount:" + dealerId, request.idempotencyKey(), Map.of("username", username, "dealer", dealerId), () -> {
                Instant now = Instant.now();
                String userId = "usr_dlr_" + UUID.randomUUID();
                String status = DealerService.APPROVED.equals(dealer.status()) && dealer.portalEnabled() ? "active" : "disabled";
                access.createUser(new AccessUser(userId, username, ACCOUNT_TYPE, dealer.projectId(), null, status, now));
                credentials.provisionCredential(userId, password, status, now);
                access.assignUserRole(userId, ROLE);
                DealerPortalAccountRecord account = new DealerPortalAccountRecord(userId, dealerId, username, status, actorId(), now, null);
                portalAccounts.saveAccount(account);
                audit.recordSuccess("dealer.portal_account.created", MANAGE, "dealer_portal_account", userId, null, request.idempotencyKey(),
                        Map.of("dealer_id", dealerId, "username", username, "contact_name", request.contactName() == null ? "" : request.contactName()));
                return account;
            }, DealerPortalAccountRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.portal_account.create_failed", MANAGE, "dealer_portal_account", dealerId, null, request == null ? null : request.idempotencyKey(),
                    e instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw e;
        }
    }

    void activateApprovedDealer(String dealerId) {
        DealerRecord dealer = dealers.findDealer(dealerId).orElseThrow();
        if (!DealerService.APPROVED.equals(dealer.status()) || !dealer.portalEnabled()) {
            throw ApiException.conflict("DEALER_PORTAL_NOT_ENABLED", "四项准入审核通过后才能启用账号");
        }
        Instant now = Instant.now();
        for (DealerPortalAccountRecord account : portalAccounts.listByDealer(dealerId)) {
            if ("disabled".equals(account.status())) credentials.changeStatus(account.userId(), "active", now);
        }
        portalAccounts.activateByDealer(dealerId);
    }

    /** 停用撤权（由 DealerService.deactivateDealer 在同一事务内调用）：门户账号停用 + 全部会话立即作废 + 留痕。 */
    void revokeDealerPortal(String dealerId, String reason) {
        Instant now = Instant.now();
        List<DealerPortalAccountRecord> accounts = portalAccounts.listByDealer(dealerId);
        int sessionsRevoked = 0;
        for (DealerPortalAccountRecord account : accounts) {
            if (!"revoked".equals(account.status())) credentials.changeStatus(account.userId(), "disabled", now);
            sessionsRevoked += sessions.revokeUserSessions(account.userId());
        }
        int accountsRevoked = portalAccounts.revokeByDealer(dealerId, now);
        audit.recordSuccess("dealer.portal.revoked", DealerService.MANAGE, "dealer", dealerId, null, null,
                Map.of("accounts_revoked", accountsRevoked, "sessions_revoked", sessionsRevoked, "reason", reason == null ? "" : reason));
    }

    private DealerRecord requireDealer(String id) {
        DealerRecord dealer = dealers.findDealer(requireText(id, "dealer_id")).orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        String scope = authorization.currentProjectScopeQuietly().orElse(null);
        if (scope != null && dealer.projectId() != null && !scope.equals(dealer.projectId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权访问其他项目的经销商");
        }
        return dealer;
    }

    /** CG-11d：经销商门户账号（account_type = dealer）按所属经销商隔离数据，不按项目范围过滤（归属由 DealerPortalService 硬校验）。 */
    static String projectScopeOf(AuthorizationService authorization) {
        AccessActor actor = authorization.currentActor();
        if (actor != null && DealerPortalAccountService.ACCOUNT_TYPE.equals(actor.accountType())) {
            return null;
        }
        return authorization.currentProjectScopeQuietly().orElse(null);
    }

    private String actorId() {
        AccessActor actor = authorization.currentActor();
        return actor == null ? null : actor.id();
    }
}
