package com.complex.platform.serviceorder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceOrderRepository {

    ServiceCatalogItemRecord upsertCatalogItem(ServiceCatalogItemRecord item);

    List<ServiceCatalogItemRecord> listCatalog(boolean activeOnly);

    Optional<ServiceCatalogItemRecord> findCatalogItem(String id);

    void upsertPaymentCode(String userId, String qrContent, Instant now);

    Optional<String> findPaymentCode(String userId);

    ServiceOrderRecord saveOrder(ServiceOrderRecord order);

    Optional<ServiceOrderRecord> findOrder(String id);

    List<ServiceOrderRecord> listOrdersForPerformer(String performerUserId);

    List<ServiceOrderRecord> listOrders(LocalDate date, String status);

    /** 履约人当月完成次数与参考价合计(直付台账统计,非营收)。 */
    long[] performerMonthlyStats(String performerUserId, String month);
}
