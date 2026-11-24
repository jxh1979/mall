package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 第十八批 BP 段 359-R3（第十八批裁决 75「按当天已售张数：规则里填『到访当天已售满多少张起』，不要求票种设限量；系统新增按到访日统计该票种已售张数」）：
 * 某到访日某票种已售张数，与票种限量的已售数同一口径——下单即占、取消回补、退一张放一张（TicketOrderService、TicketRefundService 的扣减与回补），
 * 即未取消订单的张数减其中已退的票。定价在本单落库前取数，不含本单。
 */
@FunctionalInterface
public interface TicketDaySoldPort {

    long soldOn(String ticketTypeId, LocalDate visitDate);

    @Repository
    class Jdbc implements TicketDaySoldPort {

        private final JdbcTemplate jdbc;

        public Jdbc(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public long soldOn(String ticketTypeId, LocalDate visitDate) {
            Long sold = jdbc.queryForObject("""
                    SELECT COALESCE(SUM(o.quantity), 0)
                           - (SELECT count(*) FROM ticket_instances i JOIN ticket_orders r ON r.id = i.order_id
                               WHERE r.ticket_type_id = ? AND r.visit_date = ? AND r.status <> 'cancelled' AND i.status = 'refunded')
                      FROM ticket_orders o
                     WHERE o.ticket_type_id = ? AND o.visit_date = ? AND o.status <> 'cancelled'
                    """, Long.class, ticketTypeId, visitDate, ticketTypeId, visitDate);
            return sold == null ? 0L : sold;
        }
    }
}
