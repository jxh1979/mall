package com.complex.platform.governance;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantRecord(
        String id,
        String merchantName,
        String merchantType,
        String businessEntityId,
        String contractEntityId,
        String settlementEntityId,
        String contactName,
        String contactPhone,
        String ownerPerson,
        String mallBlockCode,
        BigDecimal areaSquareMeter,
        BigDecimal leasedAreaSquareMeter,
        String secondaryContactName,
        String secondaryContactPhone,
        BigDecimal rentPerSquareMeter,
        String cooperationForm,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // V260 数据存档与审批后删除范式(照 V255 权益模板试点)。存档=软标记可恢复;删除=硬删走审批。
        // archivedAt 非空=已存档;deleteStatus none|pending|approved。放末位便于既有构造点追加。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {
    // ponytail: V260 前的 19 参构造点(既有代码/测试)不带存档/删除字段,默认未存档 + delete_status=none。
    //           避免为 6 个新字段改所有既有构造点;日后语义要变则删本构造让编译逼出所有点。
    public MerchantRecord(
            String id, String merchantName, String merchantType, String businessEntityId, String contractEntityId,
            String settlementEntityId, String contactName, String contactPhone, String ownerPerson, String mallBlockCode,
            BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter, String secondaryContactName,
            String secondaryContactPhone, BigDecimal rentPerSquareMeter, String cooperationForm, String status,
            Instant createdAt, Instant updatedAt) {
        this(id, merchantName, merchantType, businessEntityId, contractEntityId, settlementEntityId, contactName,
                contactPhone, ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter, secondaryContactName,
                secondaryContactPhone, rentPerSquareMeter, cooperationForm, status, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    // --- V260 存档/删除范式：不可变 record 的字段更新辅助（避免 25 参构造散落业务代码）。 ---
    public MerchantRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
                settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }

    public MerchantRecord withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
            String deleteReason, Instant updatedAt) {
        return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
                settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }
}
