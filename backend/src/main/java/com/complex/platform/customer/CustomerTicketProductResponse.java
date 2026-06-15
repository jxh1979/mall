package com.complex.platform.customer;

public record CustomerTicketProductResponse(
        String ticketTypeId,
        String ticketName,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        String currency,
        String status
) {
}
