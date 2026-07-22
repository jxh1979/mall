package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketCredentialService {

    private final TicketVerificationService verificationService;
    private final AuditEventService auditEventService;

    public TicketCredentialService(TicketVerificationService verificationService, AuditEventService auditEventService) {
        this.verificationService = verificationService;
        this.auditEventService = auditEventService;
    }

    // 复核修正(AUD-012):取码链必须在一个事务内(共享锁读 holder→插凭证同事务提交),@Transactional 须 public 才被代理。
    @Transactional
    public TicketCredentialResponse getTicketCredential(String ticketId) {
        try {
            TicketCredentialResponse response = verificationService.refreshCredential(ticketId, null);
            auditEventService.recordSuccess("ticket_credential.refreshed", "ticket.credential.view", "ticket_credential", response.id(), null, null, Map.of("ticket_id", response.ticketId()));
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("ticket_credential.refresh_failed", "ticket.credential.view", "ticket_credential", ticketId, null, null, codeOf(exception));
            throw exception;
        }
    }

    @Transactional
    public TicketCredentialResponse getCustomerTicketCredential(String ticketId, String customerId) {
        String resolvedCustomerId = requireText(customerId, "customer_id");
        try {
            TicketCredentialResponse response = verificationService.refreshCredential(ticketId, resolvedCustomerId);
            auditEventService.recordSuccess("ticket_credential.refreshed", "not_applicable", "ticket_credential", response.id(), null, null, Map.of("ticket_id", response.ticketId(), "customer_id", resolvedCustomerId));
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("ticket_credential.refresh_failed", "not_applicable", "ticket_credential", ticketId, null, null, codeOf(exception));
            throw exception;
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
