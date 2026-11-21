package com.complex.platform.governance;
// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.dto.SpaceUpdateRequest;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BL 段第 388 条 388-R1（PGM:469「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」；
 * PGM:470「记录空间面积、用途、状态、负责人和所属商户。」）：修改空间。可改名称、类型、上级、楼层、面积、用途、负责人、所属商户与状态
 * （改为不可用即停用）；所属项目与平面图点位照原值。入口口径同 PlatformService.createSpace：平台范围＋governance.space.manage
 * 可触及空间所属项目（范围闸在幂等之前）；改了的类型须为启用值、改了的所属商户须未存档（BSM:569「不再被新业务选用」只管新选，沿用原值不核）。
 */
@Service
public class SpaceMaintenanceService {

    private static final String CODE = "governance.space.manage";

    private final PlatformService platform;
    private final PlatformRepository repository;
    private final AuthorizationService authorizationService;

    public SpaceMaintenanceService(PlatformService platform, PlatformRepository repository,
            AuthorizationService authorizationService) {
        this.platform = platform;
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public SpaceRecord updateSpace(String spaceId, SpaceUpdateRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(spaceId, "space_id");
        return platform.executeWrite(
                "updateSpace:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SpaceRecord.class,
                "space.updated",
                "space.update_failed",
                CODE,
                "space",
                () -> {
                    SpaceRecord existing = existing(id);
                    if (!platform.platformWide() && !platform.reachOf(CODE).covers(existing.projectId())) {
                        throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
                    }
                },
                () -> {
                    SpaceRecord existing = existing(id);
                    String spaceName = requireText(request == null ? null : request.spaceName(), "space_name");
                    String spaceType = requireText(request.spaceType(), "space_type");
                    if (!spaceType.equals(existing.spaceType()) && !repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "空间种类必须是空间种类表中的启用值");
                    }
                    String merchantId = trimToNull(request.merchantId());
                    if (merchantId != null && !merchantId.equals(existing.merchantId())) {
                        platform.requireActiveMerchant(merchantId, CODE);
                    }
                    String parentSpaceId = trimToNull(request.parentSpaceId());
                    if (parentSpaceId != null) {
                        SpaceRecord parent = repository.findSpaceById(parentSpaceId)
                                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在"));
                        if (!Objects.equals(existing.projectId(), parent.projectId())) {
                            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "上级空间须属于同一项目");
                        }
                    }
                    platform.requireNonNegative(request.areaSquareMeter(), "空间面积");
                    return repository.saveSpace(new SpaceRecord(
                            existing.id(),
                            existing.projectId(),
                            merchantId,
                            parentSpaceId,
                            spaceName,
                            spaceType,
                            trimToNull(request.floorNo()),
                            request.areaSquareMeter(),
                            existing.polygonPoints(),
                            platform.requireSpaceStatus(request.status()),
                            existing.createdAt(),
                            Instant.now(),
                            trimToNull(request.purpose()),
                            trimToNull(request.ownerUserId())
                    ));
                }
        );
    }

    private SpaceRecord existing(String id) {
        return repository.findSpaceById(id).orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "空间不存在"));
    }
}
