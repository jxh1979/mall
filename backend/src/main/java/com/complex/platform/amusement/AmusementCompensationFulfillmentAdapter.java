package com.complex.platform.amusement;

import com.complex.platform.ticketing.CompensationFulfillmentPort;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** CG-08b 票务核销成功 → 把补偿履约锁定到补偿记录（只依赖本域仓储，避免初始化环）。 */
@Service
public class AmusementCompensationFulfillmentAdapter implements CompensationFulfillmentPort {

    private final JdbcAmusementFaultCompensationRepository repository;

    public AmusementCompensationFulfillmentAdapter(JdbcAmusementFaultCompensationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordFulfillment(String compensationItemId, String verificationId, Instant fulfilledAt, Long settlementPriceCent) {
        repository.markItemFulfilled(compensationItemId, verificationId, fulfilledAt, settlementPriceCent);
    }
}
