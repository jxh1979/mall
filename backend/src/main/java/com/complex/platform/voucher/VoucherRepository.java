package com.complex.platform.voucher;

// 文件版本：V0.01

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 统一券台账仓储。merchantScope 参数语义：非空＝商户 scope 账号，只见（自家商户∪平台通用）券。 */
public interface VoucherRepository {

    Optional<VoucherDefinitionRecord> findDefinitionById(String id);

    /** 兑换目录项→券定义：目录显式挂接（voucher_definition_id）优先，否则按 vdef_cat_&lt;目录id&gt; 找派生定义。 */
    Optional<VoucherDefinitionRecord> findDefinitionForCatalogItem(String catalogItemId);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝已存在。 */
    boolean insertDefinition(VoucherDefinitionRecord definition);

    // ---------- 批10/E 内部结算 SKU 目录(V379/V380) ----------

    /** 目录条目(生效或草案)的绑定商户:建券设引用时校验「条目商家 = 结算商户」。空 = 无此条目。default 空兼容测试替身。 */
    default Optional<String> findCatalogItemMerchant(String itemCode) {
        return Optional.empty();
    }

    /** 目录生效价(is_draft=FALSE):建券快照来源。空 = 条目仅草案/已归档。default 空兼容测试替身。 */
    default Optional<Long> findActiveCatalogPriceCent(String itemCode) {
        return Optional.empty();
    }

    /** 券定义挂目录引用(定向 UPDATE;internal_price_cent 快照由 insertDefinition 落、目录转正传播维护)。 */
    default void setDefinitionCatalogRef(String definitionId, String itemCode) {
    }

    PageResponse<VoucherDefinitionRecord> listDefinitions(
            String merchantScope, String status, String voucherKind, int page, int pageSize);

    Optional<VoucherInstanceRecord> findInstanceById(String id);

    /**
     * 操作员裁决第十三批第 9 条「和门票一致不冻结……先成功的动作为准」：锁住该券实例行（SELECT … FOR UPDATE），转赠发起、领取与核销在同一行锁上
     * 串行，锁后再读即见先提交者的结果。default 空操作（内存测试替身）。
     */
    default void lockInstance(String id) {
    }

    Optional<VoucherInstanceRecord> findInstanceByCode(String code);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝已存在。 */
    boolean insertInstance(VoucherInstanceRecord instance);

    /**
     * CAS 状态迁移：status=expectFrom 时置为 toStatus（redeemed/expired 落 redeemed_at/
     * revoked 落 revoked_at+reason），返回受影响行数（0＝并发已被别人迁走）。
     */
    int updateInstanceStatus(String id, String expectFrom, String toStatus, Instant at, String revokeReason);

    /** V313 转赠领取：把券实例归属改为受赠人（守卫 status='issued'），返回受影响行数（0＝已非 issued）。 */
    int updateInstanceCustomer(String id, String newCustomerId, Instant at);

    /** V312 过期释放扫描：issued + 已预占（stock_reserved）+ 已过期（expires_at&lt;=now）的实例，最多 limit 条。 */
    List<VoucherInstanceRecord> listExpiredReservedIssuedInstances(Instant now, int limit);

    PageResponse<VoucherInstanceRecord> listInstances(
            String merchantScope, String customerId, String definitionId, String status, String sourceType,
            int page, int pageSize);

    /** 幂等插入（ON CONFLICT DO NOTHING）；false＝该实例已有核销记录（实例级唯一索引双花闸）。 */
    boolean insertRedemption(VoucherRedemptionEntryRecord entry);

    PageResponse<VoucherRedemptionEntryRecord> listRedemptions(
            String merchantId, String voucherInstanceId, int page, int pageSize);

    /** "全部券"聚合（读层并入）：统一台账实例 ∪ V282 通票第三方合作券权益实例。 */
    PageResponse<VoucherOverviewRecord> overview(String merchantScope, String customerId, int page, int pageSize);

    /** ROOT-03-R32 券动态核销码（V541 voucher_redeem_codes）：未过期的 active 码所属券与签发对象。 */
    record ActiveRedeemCode(String voucherInstanceId, String customerId) {
    }

    default void insertRedeemCode(String id, String voucherInstanceId, String customerId, String codeHash, Instant issuedAt,
            Instant expiresAt, int refreshIntervalSeconds) {
        throw new UnsupportedOperationException("insertRedeemCode");
    }

    /** 撤销该券全部 active 码（取新码、转赠领取时）。 */
    default int revokeActiveRedeemCodes(String voucherInstanceId) {
        return 0;
    }

    default Optional<ActiveRedeemCode> findActiveRedeemCode(String codeHash, Instant now) {
        return Optional.empty();
    }

    /** 核销成功同事务把该码置 consumed。 */
    default void consumeRedeemCode(String codeHash, String redemptionId) {
    }
}
