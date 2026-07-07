package com.complex.platform.ops;

import java.util.Arrays;
import java.util.Optional;

/**
 * 集中清理工具支持的清理范围（Tier-D）。每个范围绑定一张【已有存档能力】的配置类实体表，
 * 声明其 id 列、时间列（archived_at）与"活引用列名"（照 V259/V260/V262 元数据驱动引用计数口径）。
 *
 * 安全边界（诚实）：只清理 archived_at 非空（已存档=软下线可恢复）且超过 N 天的行；绝不删活数据。
 * 删除前逐行再校验"仍已存档 + delete_status<>'approved'（不与逐条审批删除态打架）+ 无活引用"。
 *
 * referenceColumn=null 表示该实体无外部活引用（如套餐 bar_combos：明细子表 ON DELETE CASCADE、无订单引用，
 * 照 BarPosCatalogService.approveDeleteCombo 结论）。
 */
public enum DataCleanupScope {
    ARCHIVED_ENTITLEMENT_TEMPLATES("archived_entitlement_templates", "entitlement_templates", "template_id"),
    ARCHIVED_MERCHANTS("archived_merchants", "merchants", "merchant_id"),
    ARCHIVED_TICKET_TYPES("archived_ticket_types", "ticket_types", "ticket_type_id"),
    ARCHIVED_TICKET_PRICE_RULES("archived_ticket_price_rules", "ticket_price_rules", "price_rule_id"),
    ARCHIVED_BAR_PRODUCTS("archived_bar_products", "bar_products", "product_id"),
    ARCHIVED_BAR_PRODUCT_CATEGORIES("archived_bar_product_categories", "bar_product_categories", "category_id"),
    ARCHIVED_BAR_COMBOS("archived_bar_combos", "bar_combos", null);

    private final String code;
    private final String tableName;
    /** 别的基表用来引用本实体行的列名（元数据驱动逐表 COUNT），null=无外部活引用。 */
    private final String referenceColumn;

    DataCleanupScope(String code, String tableName, String referenceColumn) {
        this.code = code;
        this.tableName = tableName;
        this.referenceColumn = referenceColumn;
    }

    public String code() {
        return code;
    }

    public String tableName() {
        return tableName;
    }

    public Optional<String> referenceColumn() {
        return Optional.ofNullable(referenceColumn);
    }

    public static Optional<DataCleanupScope> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String trimmed = code.trim();
        return Arrays.stream(values()).filter(s -> s.code.equals(trimmed)).findFirst();
    }
}
