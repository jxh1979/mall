package com.complex.platform.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 铺位平面图与租赁状态看板数据源：每个铺位的租赁状态（六态）+ 命中的着色；第十八批 CE 段 403-R1 另带所属项目、空间类型、面积
 * （看板按项目、按面积算出租率）与「已签待交」标注（口径见 {@link SpaceRentalDerivationSql#SIGNED_PENDING_LATERAL}，只是标注、不改六态）。
 */
public record SpaceRentalStatusRecord(
        String spaceId,
        String spaceName,
        String rentalStatus,    // vacant(空置) / negotiating(洽谈中) / let(已租) / expiring(即将到期) / moved_out(已退租) / unavailable(不可租)
        String color,           // 命中着色分类色（仅 expiring 非空）
        Integer daysToExpiry,   // 距到期天数（已租/即将到期非空）
        String projectId,
        String spaceType,
        BigDecimal areaSqm,     // spaces.area_square_meter
        String signedPendingContractId,         // 已签待交关联合同（没有为空）
        LocalDate signedPendingEffectiveDate    // 已签待交交付依据：该合同生效日（BSM:1347「交付日（即起租日）」）
) {
}
