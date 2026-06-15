package com.complex.platform.ticketing;

import java.util.List;

public record IssuedTicketResponse(
        List<IssuedEntitlementResponse> issuedEntitlements
) {
}
