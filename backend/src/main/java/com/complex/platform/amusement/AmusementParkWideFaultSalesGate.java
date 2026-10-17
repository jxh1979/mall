package com.complex.platform.amusement;

import com.complex.platform.ticketing.ParkWideFaultSalesGate;
import org.springframework.stereotype.Service;

/** CG-08a 票务下单前查询全园故障状态（只读，不依赖 ticketing 其他 bean，避免初始化环）。 */
@Service
public class AmusementParkWideFaultSalesGate implements ParkWideFaultSalesGate {

    private final JdbcAmusementParkWideFaultRepository repository;

    public AmusementParkWideFaultSalesGate(JdbcAmusementParkWideFaultRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean salesSuspended(String projectId) {
        return projectId == null ? repository.existsAnyActive() : repository.existsActive(projectId);
    }
}
