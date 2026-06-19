package com.complex.platform.ticketing;

import com.complex.platform.amusement.AmusementEntitlementGateway;
import com.complex.platform.amusement.AmusementEntitlementView;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 票务域对游乐核销端口 {@link AmusementEntitlementGateway} 的实现（ticketing → amusement 单向依赖，防循环）。
 *
 * <p>复用票务既有 {@link TicketingRepository} 的凭证/权益查询与原子消费/回补能力，
 * 不改动票务既有核销逻辑（{@link TicketVerificationService}）。游乐核销据此识别票/权益实例并消费游乐权益（设计 §6.4/§12.1）。
 */
@Component
public class TicketingAmusementEntitlementGateway implements AmusementEntitlementGateway {

    private final TicketingRepository repository;

    public TicketingAmusementEntitlementGateway(TicketingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AmusementEntitlementView> findEntitlementByCredential(String credentialPayload) {
        if (credentialPayload == null || credentialPayload.isBlank()) {
            return Optional.empty();
        }
        String payload = credentialPayload.trim();
        // 动态二维码 token（cred_ 前缀）：先查凭证、校验有效再取权益；其余按权益码直查。
        if (payload.startsWith("cred_")) {
            Optional<TicketCredentialRecord> credential = repository.findTicketCredentialByToken(payload);
            if (credential.isEmpty()) {
                return Optional.empty();
            }
            TicketCredentialRecord cred = credential.get();
            if (!"active".equals(cred.status()) || cred.expiresAt().isBefore(Instant.now())) {
                // 凭证过期/失效：等同凭证不可用，交由调用方落 ticket_not_found（不抛出，避免 500）。
                return Optional.empty();
            }
            return repository.findIssuedEntitlementById(cred.entitlementId()).map(this::toView);
        }
        return repository.findIssuedEntitlementByCode(payload).map(this::toView);
    }

    @Override
    public boolean consumeEntitlement(String entitlementId) {
        return repository.atomicUseEntitlement(requireText(entitlementId), Instant.now()).isPresent();
    }

    @Override
    public void reverseEntitlement(String entitlementId) {
        // 补偿回滚：权益已扣但容量扣减失败时回补一次（used_times-1，下限 0）。
        repository.atomicReverseEntitlement(requireText(entitlementId), Instant.now());
    }

    private AmusementEntitlementView toView(IssuedEntitlementRecord entitlement) {
        return new AmusementEntitlementView(
                entitlement.id(),
                entitlement.entitlementType(),
                entitlement.status(),
                entitlement.validFrom(),
                entitlement.validTo(),
                entitlement.remainingTimes()
        );
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "entitlement_id is required");
        }
        return value.trim();
    }
}
