package com.complex.platform.bar;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 图形化点单 POS 目录服务（V256/V257 阶段1）：分类 CRUD、套餐 CRUD、给 SKU 设分类/图片、按分类列 SKU。
 * 目录属后台配置（非移动资金的 POS 交易），走轻量 CRUD，不套 BarOperationExecutor 的幂等/凭证机制。
 * 租户隔离：写入时 merchant_id 取当前 scope；读取按 scope 过滤；跨商户改分类/套餐/商品统一 checkOwnership 兜底。
 */
@Service
public class BarPosCatalogService {

    private final JdbcBarPosCatalogRepository repository;
    private final AuthorizationService authorizationService;

    public BarPosCatalogService(JdbcBarPosCatalogRepository repository,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    // ---------- 分类 ----------

    public List<BarProductCategoryRecord> listCategories(String barId) {
        return repository.listCategories(authorizationService.currentMerchantScope(), trimToNull(barId));
    }

    @Transactional
    public BarProductCategoryRecord saveCategory(BarProductCategoryRequest request) {
        BarProductCategoryRequest r = require(request);
        Optional<String> scope = authorizationService.currentMerchantScope();
        Instant now = Instant.now();
        if (trimToNull(r.id()) == null) {
            String barId = requireText(r.barId(), "bar_id");
            return repository.saveCategory(new BarProductCategoryRecord(
                    "barcat_" + slug(),
                    barId,
                    scope.orElse(null),
                    requireText(r.categoryName(), "category_name"),
                    r.sortOrder() == null ? 0 : r.sortOrder(),
                    statusOrDefault(r.status()),
                    now, now));
        }
        BarProductCategoryRecord existing = repository.findCategory(r.id().trim())
                .orElseThrow(() -> ApiException.notFound("BAR_CATEGORY_NOT_FOUND", "category not found"));
        checkOwnership(scope, existing.merchantId());
        return repository.saveCategory(new BarProductCategoryRecord(
                existing.id(), existing.barId(), existing.merchantId(),
                r.categoryName() == null ? existing.categoryName() : requireText(r.categoryName(), "category_name"),
                r.sortOrder() == null ? existing.sortOrder() : r.sortOrder(),
                r.status() == null ? existing.status() : statusOrDefault(r.status()),
                existing.createdAt(), now));
    }

    // ---------- 商品设分类/图片 + 按分类列 ----------

    @Transactional
    public BarProductRecord assignCategory(String productId, BarProductCategoryAssignRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductRecord product = repository.findProduct(requireText(productId, "product_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "product not found"));
        checkOwnership(scope, product.merchantId());
        String categoryId = request == null ? null : trimToNull(request.categoryId());
        if (categoryId != null) {
            BarProductCategoryRecord category = repository.findCategory(categoryId)
                    .orElseThrow(() -> ApiException.notFound("BAR_CATEGORY_NOT_FOUND", "category not found"));
            checkOwnership(scope, category.merchantId());
            if (!category.barId().equals(product.barId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "category is not in this bar");
            }
        }
        repository.updateProductCategory(product.id(), categoryId, Instant.now());
        return repository.findProduct(product.id()).orElseThrow();
    }

    @Transactional
    public BarProductRecord setProductImage(String productId, String imageUrl) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductRecord product = repository.findProduct(requireText(productId, "product_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "product not found"));
        checkOwnership(scope, product.merchantId());
        repository.updateProductImage(product.id(), trimToNull(imageUrl), Instant.now());
        return repository.findProduct(product.id()).orElseThrow();
    }

    public List<BarProductRecord> listProductsByCategory(String barId, String categoryId) {
        return repository.listProductsByCategory(authorizationService.currentMerchantScope(),
                requireText(barId, "bar_id"), trimToNull(categoryId));
    }

    // ---------- 套餐 ----------

    public List<BarComboRecord> listCombos(String barId) {
        return repository.listCombos(authorizationService.currentMerchantScope(), trimToNull(barId));
    }

    @Transactional
    public BarComboRecord saveCombo(BarComboRequest request) {
        BarComboRequest r = require(request);
        Optional<String> scope = authorizationService.currentMerchantScope();
        Instant now = Instant.now();
        Long internalPrice = r.internalSettlementPriceCent();
        if (internalPrice != null && internalPrice < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "internal_settlement_price_cent must be >= 0");
        }
        String comboId;
        String barId;
        String merchantId;
        Instant createdAt;
        if (trimToNull(r.id()) == null) {
            comboId = "barcombo_" + slug();
            barId = requireText(r.barId(), "bar_id");
            merchantId = scope.orElse(null);
            createdAt = now;
        } else {
            BarComboRecord existing = repository.findCombo(r.id().trim())
                    .orElseThrow(() -> ApiException.notFound("BAR_COMBO_NOT_FOUND", "combo not found"));
            checkOwnership(scope, existing.merchantId());
            comboId = existing.id();
            barId = existing.barId();
            merchantId = existing.merchantId();
            createdAt = existing.createdAt();
        }
        List<BarComboRequest.Item> reqItems = r.items() == null ? List.of() : r.items();
        if (reqItems.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "combo must contain at least one product");
        }
        List<BarComboItemRecord> items = reqItems.stream().map(it -> {
            String productId = requireText(it.productId(), "items[].productId");
            BarProductRecord product = repository.findProduct(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "combo product not found: " + productId));
            checkOwnership(scope, product.merchantId());
            if (!product.barId().equals(barId)) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "combo product is not in this bar: " + productId);
            }
            int qty = it.quantity() == null ? 0 : it.quantity();
            if (qty <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "combo item quantity must be > 0");
            }
            return new BarComboItemRecord("barcomboitem_" + slug(), comboId, productId, product.productName(), qty);
        }).toList();
        return repository.saveCombo(new BarComboRecord(
                comboId, barId, merchantId,
                requireText(r.comboName(), "combo_name"),
                nonNegative(r.priceCent(), "price_cent"),
                internalPrice, trimToNull(r.imageUrl()),
                statusOrDefault(r.status()), createdAt, now, items));
    }

    // ---------- helpers ----------

    private void checkOwnership(Optional<String> scope, String resourceMerchantId) {
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("BAR_MERCHANT_SCOPE", "resource belongs to another merchant");
        }
    }

    private static <T> T require(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private static String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private static long nonNegative(Long value, String field) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be a non-negative number");
        }
        return value;
    }

    private static String statusOrDefault(String status) {
        String trimmed = trimToNull(status);
        return trimmed == null ? "active" : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String slug() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
