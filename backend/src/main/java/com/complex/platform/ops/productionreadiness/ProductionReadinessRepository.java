package com.complex.platform.ops.productionreadiness;

import java.util.List;
import java.util.Optional;

public interface ProductionReadinessRepository {

    List<ProductionReadinessItem> findAllItems();

    List<ProductionReadinessItem> findItemsByCategory(String category);

    Optional<ProductionReadinessItem> findItemById(String id);

    void saveDefaultItems(List<ProductionReadinessItem> defaultItems);

    ProductionReadinessItem saveItem(ProductionReadinessItem item);

    ProductionReadinessCheck saveCheck(ProductionReadinessCheck check, List<ProductionReadinessItem> blockers);
}
