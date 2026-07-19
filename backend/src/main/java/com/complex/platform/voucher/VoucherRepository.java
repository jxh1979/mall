package com.complex.platform.voucher;

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.Optional;

/** 统一券台账仓储。merchantScope 参数语义：非空＝商户 scope 账号，只见（自家商户∪平台通用）券。 */
public interface VoucherRepository {

    Optional<VoucherDefinitionRecord> findDefinitionById(String id);

    /** 兑换目录项→券定义：目录显式挂接（voucher_definition_id）优先，否则按 vdef_cat_&lt;目录id&gt; 找派生定义。 */
    Optional<VoucherDefinitionRecord> findDefinitionForCatalogItem(String catalogItemId);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝已存在。 */
    boolean insertDefinition(VoucherDefinitionRecord definition);

    PageResponse<VoucherDefinitionRecord> listDefinitions(
            String merchantScope, String status, String voucherKind, int page, int pageSize);

    Optional<VoucherInstanceRecord> findInstanceById(String id);

    Optional<VoucherInstanceRecord> findInstanceByCode(String code);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝已存在。 */
    boolean insertInstance(VoucherInstanceRecord instance);

    /**
     * CAS 状态迁移：status=expectFrom 时置为 toStatus（redeemed/expired 落 redeemed_at/
     * revoked 落 revoked_at+reason），返回受影响行数（0＝并发已被别人迁走）。
     */
    int updateInstanceStatus(String id, String expectFrom, String toStatus, Instant at, String revokeReason);

    PageResponse<VoucherInstanceRecord> listInstances(
            String merchantScope, String customerId, String definitionId, String status, String sourceType,
            int page, int pageSize);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝该实例已有核销记录（实例级唯一索引双花闸）。 */
    boolean insertRedemption(VoucherRedemptionEntryRecord entry);

    PageResponse<VoucherRedemptionEntryRecord> listRedemptions(
            String merchantId, String voucherInstanceId, int page, int pageSize);

    /** "全部券"聚合（读层并入）：统一台账实例 ∪ V282 通票第三方合作券权益实例。 */
    PageResponse<VoucherOverviewRecord> overview(String merchantScope, String customerId, int page, int pageSize);
}
