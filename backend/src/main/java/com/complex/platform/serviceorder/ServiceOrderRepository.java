package com.complex.platform.serviceorder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceOrderRepository {

    ServiceCatalogItemRecord upsertCatalogItem(ServiceCatalogItemRecord item);

    List<ServiceCatalogItemRecord> listCatalog(boolean activeOnly, boolean includeArchived);

    void archiveCatalogItem(String id, boolean archive, String reason, String actorId);

    void requestDeleteCatalogItem(String id, String reason, String actorId);

    void approveDeleteCatalogItem(String id, boolean approved);

    Optional<ServiceCatalogItemRecord> findCatalogItem(String id);

    void upsertPaymentCode(String userId, String qrContent, Instant now);

    Optional<String> findPaymentCode(String userId);

    ServiceOrderRecord saveOrder(ServiceOrderRecord order);

    Optional<ServiceOrderRecord> findOrder(String id);

    /** 并发守卫(2026-07-05 P1):paid→completed 原子状态转移。受影响行数=1 即为唯一进入凭据;
     * 落 mgmt_fee/dancer_share/completed_at/updated_at。返回受影响行数(>1 不可能,主键唯一)。 */
    int markCompletedIfPaid(String id, long mgmtFeeCent, long dancerShareCent, Instant completedAt, Instant now);

    /** 收款对账(2026-07-05 P1):mark-paid 落一条公司应收/已收流水到复用的 ar_ap_items 应收台账
     * (source=service_order:orderId 唯一,幂等;paid=amount 即公司账户实收)。返回是否新写入。 */
    boolean insertReceivableForOrder(String orderId, long amountCent, Instant now);

    List<ServiceOrderRecord> listOrdersForPerformer(String performerUserId);

    /** P0 租户隔离(V214):projectScope=null 见全量;非空仅见 project_id=该值的行。 */
    List<ServiceOrderRecord> listOrders(LocalDate date, String status, String projectScope);

    void upsertPayConfig(String qrContent, String updatedBy, Instant now);

    String companyPayQr();

    /** 履约人当月完成次数与分账应得合计(公司代收,月结进薪资)。 */
    long[] performerMonthlyStats(String performerUserId, String month);
}
