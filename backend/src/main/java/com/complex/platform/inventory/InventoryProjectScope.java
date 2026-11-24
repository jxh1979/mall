package com.complex.platform.inventory;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 第十八批 BK 段第 384 条 384-R1：库存盘点单各动作（列表、详情、建单、登记差异、复核、结束盘点）、差异过账（384-R2）与差异照片视频（384-R3）共用的范围闸。
 *
 * <p>依据：PGM:664「单店层面的财务工作——日结确认与锁定、商户费用账单出账、差异认定、异常单据处置、报损物品的财务确认——由该酒吧对应的财务 BP 负责」；
 * BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；PGM:659「被分配了可跨项目角色的员工（含经岗位得到），
 * 在所分配的权限内可以查看、处理该角色可查看项目的数据。」；PGM:948「库房、库存报损和盘点差异，都要看商户归属、合同范围和复核权限。」；
 * PGM:744「后端接口必须独立校验」；第十八批裁决 71（最高权限看全部）。
 *
 * <p>口径：商户账号（含派驻期内取派驻酒吧）只办本商户库房的，否则 403 CROSS_MERCHANT_FORBIDDEN；不挂商户的账号（财务 BP、平台库管）按入口权限码
 * 可触及库房所在综合体办理（{@link AuthorizationService#projectReach}，多个码取交集），否则 403 CROSS_PROJECT_FORBIDDEN；平台管理员与最高权限不限；
 * 没有项目归属的账号由 projectReach 抛 403 PROJECT_SCOPE_REQUIRED。盘点单的商户取库房的商户（平台侧账号给酒吧库建的单酒吧自己看得到）。
 */
@Component
public class InventoryProjectScope {

    private final AuthorizationService authorizationService;
    private final InventoryRepository repository;

    public InventoryProjectScope(AuthorizationService authorizationService, InventoryRepository repository) {
        this.authorizationService = authorizationService;
        this.repository = repository;
    }

    /** 建盘点单：库房存在（否则 404 WAREHOUSE_NOT_FOUND）且在范围内，返回库房。 */
    public WarehouseRecord requireStocktakeWarehouse(String warehouseId, String... permissionCodes) {
        WarehouseRecord warehouse = repository.findWarehouseById(requireText(warehouseId, "warehouse_id"))
                .orElseThrow(() -> ApiException.notFound("WAREHOUSE_NOT_FOUND", "库房不存在"));
        require(warehouse.merchantId(), warehouse::projectId, permissionCodes);
        return warehouse;
    }

    /** 按编号取盘点单（否则 404 STOCKTAKE_NOT_FOUND）并过范围闸；不挂商户的账号按盘点库房所在综合体判。 */
    public StocktakeRecord requireStocktake(String stocktakeId, String... permissionCodes) {
        StocktakeRecord stocktake = repository.findStocktakeById(requireText(stocktakeId, "stocktake_id"))
                .orElseThrow(() -> ApiException.notFound("STOCKTAKE_NOT_FOUND", "盘点不存在或已被删除"));
        require(stocktake.merchantId(), () -> repository.findWarehouseProjectId(stocktake.warehouseId()).orElse(null),
                permissionCodes);
        return stocktake;
    }

    /** 盘点单列表：商户账号本商户的；平台管理员与最高权限全部；其余按该码可触及的综合体。 */
    public List<StocktakeRecord> visibleStocktakes(String permissionCode) {
        Optional<String> merchant = authorizationService.currentMerchantIdQuietly();
        if (merchant.isPresent()) {
            return repository.listStocktakesByMerchant(merchant.get());
        }
        AuthorizationService.ProjectReach reach = authorizationService.projectReach(authorizationService.currentActor(), permissionCode);
        if (reach.platformAdmin()) {
            return repository.listStocktakes();
        }
        // ponytail: 全量取盘点单与库房后在内存里按项目过滤；量大再改成 SQL 按项目集合过滤。
        Map<String, String> projectOf = new HashMap<>();
        for (WarehouseRecord warehouse : repository.listWarehouses()) {
            projectOf.put(warehouse.id(), warehouse.projectId());
        }
        return repository.listStocktakes().stream()
                .filter(stocktake -> reach.covers(projectOf.get(stocktake.warehouseId())))
                .toList();
    }

    /** 384-R2：盘点单所在综合体（盘点库房的项目），差异过账的台账行带它；取不到返回空。 */
    public String stocktakeProject(String stocktakeId) {
        return repository.findStocktakeById(stocktakeId)
                .flatMap(stocktake -> repository.findWarehouseProjectId(stocktake.warehouseId()))
                .orElse(null);
    }

    private void require(String merchantId, Supplier<String> project, String... permissionCodes) {
        Optional<String> merchant = authorizationService.currentMerchantIdQuietly();
        if (merchant.isPresent()) {
            if (!merchant.get().equals(merchantId)) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该盘点不属于当前商户");
            }
            return;
        }
        if (!authorizationService.projectReach(authorizationService.currentActor(), permissionCodes).covers(project.get())) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权办理其他项目库房的盘点");
        }
    }
}
