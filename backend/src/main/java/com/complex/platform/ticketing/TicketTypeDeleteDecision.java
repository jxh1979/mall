package com.complex.platform.ticketing;

/** 票种删除审批结果。approved 时已物理删除（deleted=true）；rejected 回 none 未删除。 */
public record TicketTypeDeleteDecision(String ticketTypeId, String decision, boolean deleted) {
}
