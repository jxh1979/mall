package com.complex.platform.ticketing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.TicketTypeSaleSettingsRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AW 段 299-R1 票种售卖设置（ENUM:30「票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、
 * 暂停售卖（临时停止售卖）、已结束（售卖周期结束）、归档（历史策略）。」；R14 裁决 20「不设独立状态机，复用票种 4 态（草稿/上架/暂停/归档）；
 * 「待启用」「已结束」按生效时间派生、不存储」）：暂停售卖、开售与停售时间一个动作整体替换；草稿票与已上架票都能设，不重跑发布闸，
 * 不走审批，只拦新下单（售卖闸见 TicketOrderService，按 {@link TicketTypeRecord#saleStatus(Instant)}）。
 */
@Service
public class TicketTypeSaleService {

    private final TicketingRepository repository;
    private final JdbcTicketTypeSaleRepository saleRepository;
    private final TicketingOperationExecutor operations;

    public TicketTypeSaleService(TicketingRepository repository, JdbcTicketTypeSaleRepository saleRepository,
            TicketingOperationExecutor operations) {
        this.repository = repository;
        this.saleRepository = saleRepository;
        this.operations = operations;
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord updateSaleSettings(String ticketTypeId, TicketTypeSaleSettingsRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        Map<String, Object> trail = new LinkedHashMap<>();
        return operations.executeWrite(
                "updateTicketTypeSaleSettings:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.UPDATE_TICKET_TYPE_SALE_SETTINGS,
                () -> apply(id, request, trail),
                trail);
    }

    private TicketTypeRecord apply(String id, TicketTypeSaleSettingsRequest request, Map<String, Object> trail) {
        TicketTypeRecord current = repository.findTicketTypeById(id)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        if (current.archivedAt() != null || !"none".equals(current.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE", "已存档或待删除的票种不可改售卖设置");
        }
        if (request == null || request.salePaused() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "sale_paused is required");
        }
        Instant startsAt = request.saleStartsAt();
        Instant endsAt = request.saleEndsAt();
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "停售时间须晚于开售时间");
        }
        Instant now = Instant.now();
        Instant pausedAt = !request.salePaused() ? null : current.salePausedAt() != null ? current.salePausedAt() : now;
        saleRepository.updateSaleSettings(id, startsAt, endsAt, pausedAt, now);
        // 留痕记改前改后；时间为空（不限）不记该键（留痕元数据不收空值）。
        trail.put("sale_paused_before", current.salePausedAt() != null);
        trail.put("sale_paused", pausedAt != null);
        putTime(trail, "sale_starts_at_before", current.saleStartsAt());
        putTime(trail, "sale_starts_at", startsAt);
        putTime(trail, "sale_ends_at_before", current.saleEndsAt());
        putTime(trail, "sale_ends_at", endsAt);
        String reason = trimToNull(request.reason());
        if (reason != null) {
            trail.put("reason", reason);
        }
        return repository.findTicketTypeById(id).orElseThrow();
    }

    private static void putTime(Map<String, Object> trail, String key, Instant instant) {
        if (instant != null) {
            trail.put(key, instant.toString());
        }
    }
}
