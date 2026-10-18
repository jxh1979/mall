package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 「高价值物品」判定——<b>全平台唯一入口</b>（V350）。
 *
 * <p>业务口径（business-system-map-for-operations §物品档案「高价值物品」现句）：
 * <blockquote>全平台统一判定：以该物品<b>最近一次实际到货价</b>（不使用滚动变化的当前平均进货价）与后台配置的
 * <b>固定金额阈值</b>比较……该到货价<b>大于或等于</b>阈值即为高价值、低于阈值即为低价值；或被<b>手工标记</b>为高价值
 * （限库房主管及以上岗位）。退酒、报损、盘点差异、存酒作废等需主管复核或先审后行的场景一律以此判定；
 * <b>阈值未配置时一律按高价值处理、拦下走审批，不得静默放行。</b></blockquote>
 *
 * <p>四个调用点全部收口到 {@link #decide}：
 * <ul>
 *   <li>退酒（已出品）—— {@code BarOrderService.voidOrderItem}，高价值 ⇒ 先审后行 + 双证据；</li>
 *   <li>报损 / 盘点差异 —— {@code InventoryStockMovementService.createStockAdjustment}，高价值 ⇒ 建复核单不扣减；</li>
 *   <li>存酒作废 —— {@code InventoryService.cancelSavedBottle}，高价值 ⇒ 建复核单 + 置 disputed_frozen。</li>
 * </ul>
 *
 * <p><b>三条 fail-closed 分支（都不静默放行）</b>：
 * <ol>
 *   <li>阈值未配置（端口未注入 / 配置行被删）⇒ 高价值；</li>
 *   <li>物品手工标记 {@code manual_high_value=TRUE} ⇒ 高价值（不看到货价）；</li>
 *   <li><b>到货价未知</b>（物品未关联 / 从无采购入库流水 / 最近一次采购入库未录单价）⇒ 高价值。理由与①同源：
 *       无法证明它低价值就不放行，否则「不录到货价」立刻变成绕过主管复核的后门。下次采购入库录入单价后自动恢复正常分流。</li>
 * </ol>
 *
 * <p>到货价取该物品<b>最近一次采购入库流水</b>的 {@code inventory_stock_movements.unit_cost_cent}
 * （{@code movement_type=inbound} 且 {@code business_type=purchase_request}，由 {@code PurchaseRequestService.receive}
 * 写入；调拨 / 盘盈 / 退货等非采购来源的入库不算到货）。<b>不用</b> {@code inventory_item_cost_state.avg_unit_cost_cent}
 * ——那是 V296 移动加权均价，HTML 明文「不使用滚动变化的当前平均进货价」；均价只用于成本核算。
 */
// ponytail: 一个 @Service + 一个只读端口，无策略接口/无规则引擎；判定就三个分支，多一层抽象都是负债。
@Service
public class HighValueDecisionService {

    /** 判定结论。{@code basis} 为机读依据码，进审计元数据用，不上屏（上屏文案在前端映射中文）。 */
    public record HighValueDecision(
            boolean highValue,
            Long thresholdCent,
            Long purchaseUnitCostCent,
            String basis
    ) {
    }

    /** 阈值未配置 —— 拦下走审批。 */
    public static final String BASIS_THRESHOLD_UNSET = "threshold_unset";
    /** 物品档案手工标记为高价值。 */
    public static final String BASIS_MANUAL_MARK = "manual_mark";
    /** 到货价未知（未关联物品 / 无采购入库流水 / 最近一次采购入库未录单价）—— 拦下走审批。 */
    public static final String BASIS_COST_UNKNOWN = "cost_unknown";
    /** 最近一次实际到货价大于或等于阈值 —— 高价值（依据码沿用旧字符串，不改）。 */
    public static final String BASIS_ABOVE_THRESHOLD = "above_threshold";
    /** 最近一次实际到货价低于阈值 —— 低价值。 */
    public static final String BASIS_BELOW_THRESHOLD = "below_threshold";

    private final InventoryRepository repository;
    private final AuthorizationService authorizationService;

    /**
     * 阈值配置端口（实现在结算配置中心侧）。字段注入免改构造点；生产恒注入。
     * 未注入 ⇒ 视同「阈值未配置」⇒ 一律高价值（fail-closed），绝不回落硬编码 5000 元。
     */
    @Autowired(required = false)
    HighValueThresholdPort thresholdPort;

    public HighValueDecisionService(InventoryRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    /**
     * 判定某物品是否高价值。
     *
     * @param inventoryItemId 物品（库存物料）id；null / 查不到 = 到货价未知 ⇒ 高价值
     * @param merchantId      物品归属商户（阈值作用域用，权威值由调用方给，不取请求体）
     */
    public HighValueDecision decide(String inventoryItemId, String merchantId) {
        Long threshold = thresholdPort == null ? null : thresholdPort.findHighValueThresholdCent(
                authorizationService.currentProjectScopeQuietly().orElse(null), merchantId);
        if (threshold == null) {
            return new HighValueDecision(true, null, purchaseUnitCostCent(inventoryItemId).orElse(null),
                    BASIS_THRESHOLD_UNSET);
        }
        if (inventoryItemId != null
                && repository.findInventoryItemById(inventoryItemId)
                        .map(InventoryItemRecord::manualHighValue).orElse(false)) {
            return new HighValueDecision(true, threshold,
                    purchaseUnitCostCent(inventoryItemId).orElse(null), BASIS_MANUAL_MARK);
        }
        Optional<Long> cost = purchaseUnitCostCent(inventoryItemId);
        if (cost.isEmpty()) {
            return new HighValueDecision(true, threshold, null, BASIS_COST_UNKNOWN);
        }
        // BSM「高价值物品」：该到货价大于或等于阈值即为高价值、低于阈值即为低价值。
        boolean high = cost.get() >= threshold;
        return new HighValueDecision(high, threshold, cost.get(),
                high ? BASIS_ABOVE_THRESHOLD : BASIS_BELOW_THRESHOLD);
    }

    /**
     * 最近一次实际到货价（分/单位）：该物品最近一次采购入库流水的 {@code unit_cost_cent}；
     * 无采购入库流水或该笔未录单价即视为未知。不读移动加权均价。
     */
    private Optional<Long> purchaseUnitCostCent(String inventoryItemId) {
        if (inventoryItemId == null || inventoryItemId.isBlank()) {
            return Optional.empty();
        }
        return repository.findLatestPurchaseArrivalUnitCostCent(inventoryItemId);
    }
}
