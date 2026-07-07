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
 * 目录属后台配置（非移动资金的 POS 交易），可逆编辑（create/archive/unarchive）走轻量 CRUD 不套幂等；
 * 但【删除生命周期 request/approve】走 BarOperationExecutor 记审计留痕（物理删除不可逆，须可追溯谁申请/为何/谁批准）。
 * 租户隔离：写入时 merchant_id 取当前 scope；读取按 scope 过滤；跨商户改分类/套餐/商品统一 checkOwnership 兜底。
 *
 * V262 数据存档与审批后删除范式（照 V255 通票权益模板先例）：分类/套餐/商品均可存档（软下线可恢复）、
 * 走审批后硬删（须先存档、禁申请人自审、无活引用方可删）。
 */
@Service
public class BarPosCatalogService {

    private final JdbcBarPosCatalogRepository repository;
    private final AuthorizationService authorizationService;
    private final BarOperationExecutor operations;

    public BarPosCatalogService(JdbcBarPosCatalogRepository repository,
            AuthorizationService authorizationService,
            BarOperationExecutor operations) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    // ---------- 分类 ----------

    public List<BarProductCategoryRecord> listCategories(String barId, boolean includeArchived) {
        return repository.listCategories(authorizationService.currentMerchantScope(), trimToNull(barId), includeArchived);
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
        // 已存档分类不可编辑（存档=软下线，先取消存档再改）。
        if (existing.archivedAt() != null) {
            throw ApiException.conflict("BAR_CATEGORY_ARCHIVED", "category is archived; unarchive before editing");
        }
        return repository.saveCategory(new BarProductCategoryRecord(
                existing.id(), existing.barId(), existing.merchantId(),
                r.categoryName() == null ? existing.categoryName() : requireText(r.categoryName(), "category_name"),
                r.sortOrder() == null ? existing.sortOrder() : r.sortOrder(),
                r.status() == null ? existing.status() : statusOrDefault(r.status()),
                existing.createdAt(), now,
                existing.archivedAt(), existing.archivedBy(), existing.deleteStatus(),
                existing.deleteRequestedBy(), existing.deleteRequestedAt(), existing.deleteReason()));
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
            // 复核教训①：不可把商品分到已存档/待删分类（否则违背存档=软下线，且分类被引用后永远删不掉）。
            if (category.archivedAt() != null || !"none".equals(category.deleteStatus())) {
                throw ApiException.conflict("BAR_CATEGORY_NOT_ASSIGNABLE",
                        "cannot assign product to an archived or delete-pending category");
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

    public List<BarComboRecord> listCombos(String barId, boolean includeArchived) {
        return repository.listCombos(authorizationService.currentMerchantScope(), trimToNull(barId), includeArchived);
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
        BarComboRecord existing = null;
        if (trimToNull(r.id()) == null) {
            comboId = "barcombo_" + slug();
            barId = requireText(r.barId(), "bar_id");
            merchantId = scope.orElse(null);
            createdAt = now;
        } else {
            existing = repository.findCombo(r.id().trim())
                    .orElseThrow(() -> ApiException.notFound("BAR_COMBO_NOT_FOUND", "combo not found"));
            checkOwnership(scope, existing.merchantId());
            // 已存档套餐不可编辑（存档=软下线，先取消存档再改）。
            if (existing.archivedAt() != null) {
                throw ApiException.conflict("BAR_COMBO_ARCHIVED", "combo is archived; unarchive before editing");
            }
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
            // 复核教训①：套餐不可含已存档/待删商品（否则商品被引用后永远删不掉，且卖出已下线 SKU）。
            if (product.archivedAt() != null || !"none".equals(product.deleteStatus())) {
                throw ApiException.conflict("BAR_PRODUCT_NOT_ASSIGNABLE",
                        "cannot include an archived or delete-pending product in a combo: " + productId);
            }
            int qty = it.quantity() == null ? 0 : it.quantity();
            if (qty <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "combo item quantity must be > 0");
            }
            return new BarComboItemRecord("barcomboitem_" + slug(), comboId, productId, product.productName(), qty);
        }).toList();
        // 保留既有存档/删除态（编辑不改软下线轴，走专用 archive/delete 端点）。
        BarComboRecord toSave = new BarComboRecord(
                comboId, barId, merchantId,
                requireText(r.comboName(), "combo_name"),
                nonNegative(r.priceCent(), "price_cent"),
                internalPrice, trimToNull(r.imageUrl()),
                statusOrDefault(r.status()), createdAt, now, items,
                existing == null ? null : existing.archivedAt(),
                existing == null ? null : existing.archivedBy(),
                existing == null ? "none" : existing.deleteStatus(),
                existing == null ? null : existing.deleteRequestedBy(),
                existing == null ? null : existing.deleteRequestedAt(),
                existing == null ? null : existing.deleteReason());
        return repository.saveCombo(toSave);
    }

    // ================= V262 数据存档与审批后删除范式 =================

    // ---------- 分类：存档/删除 ----------

    @Transactional
    public BarProductCategoryRecord archiveCategory(String id, boolean archive) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductCategoryRecord c = requireCategory(id);
        checkOwnership(scope, c.merchantId());
        Instant now = Instant.now();
        if (archive) {
            return repository.saveCategory(c.withArchive(now, actorId(), now));
        }
        // 取消存档：清空存档标记，并把 pending 删除申请回退 none（复核教训②）。
        return repository.saveCategory(c.withArchive(null, null, now).withDelete("none", null, null, null, now));
    }

    @Transactional
    public BarProductCategoryRecord requestDeleteCategory(String id, BarCatalogDeleteRequest request) {
        return operations.auditedWrite(BarWriteOperation.REQUEST_DELETE_BAR_CATEGORY, () -> doRequestDeleteCategory(id, request));
    }

    private BarProductCategoryRecord doRequestDeleteCategory(String id, BarCatalogDeleteRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductCategoryRecord c = requireCategory(id);
        checkOwnership(scope, c.merchantId());
        if (c.archivedAt() == null) {
            throw ApiException.badRequest("BAR_CATEGORY_NOT_ARCHIVED", "category must be archived before requesting deletion");
        }
        if ("pending".equals(c.deleteStatus())) {
            throw ApiException.conflict("BAR_CATEGORY_DELETE_ALREADY_PENDING", "a delete request is already pending");
        }
        Instant now = Instant.now();
        String reason = request == null ? null : trimToNull(request.reason());
        return repository.saveCategory(c.withDelete("pending", actorId(), now, reason, now));
    }

    @Transactional
    public BarCatalogDeleteDecision approveDeleteCategory(String id, BarCatalogDeleteApprovalRequest request) {
        return operations.auditedWrite(BarWriteOperation.APPROVE_DELETE_BAR_CATEGORY, () -> doApproveDeleteCategory(id, request));
    }

    private BarCatalogDeleteDecision doApproveDeleteCategory(String id, BarCatalogDeleteApprovalRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductCategoryRecord c = requireCategory(id);
        checkOwnership(scope, c.merchantId());
        requirePending(c.deleteStatus(), "BAR_CATEGORY_DELETE_NOT_PENDING");
        String decision = decisionOf(request);
        forbidSelfApprove(c.deleteRequestedBy());
        if ("rejected".equals(decision)) {
            repository.saveCategory(c.withDelete("none", null, null, null, Instant.now()));
            return new BarCatalogDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countCategoryReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("BAR_CATEGORY_IN_USE", "category is still referenced by " + refs + " products");
        }
        repository.deleteCategory(id);
        return new BarCatalogDeleteDecision(id, "approved", true);
    }

    // ---------- 商品：存档/删除 ----------

    @Transactional
    public BarProductRecord archiveProduct(String id, boolean archive) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductRecord product = requireProduct(id);
        checkOwnership(scope, product.merchantId());
        Instant now = Instant.now();
        BarProductRecord updated = archive
                ? product.withArchive(now, actorId(), now)
                : product.withArchive(null, null, now).withDelete("none", null, null, null, now);
        repository.updateProductArchiveDelete(updated);
        return repository.findProduct(id).orElseThrow();
    }

    @Transactional
    public BarProductRecord requestDeleteProduct(String id, BarCatalogDeleteRequest request) {
        return operations.auditedWrite(BarWriteOperation.REQUEST_DELETE_BAR_PRODUCT, () -> doRequestDeleteProduct(id, request));
    }

    private BarProductRecord doRequestDeleteProduct(String id, BarCatalogDeleteRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductRecord product = requireProduct(id);
        checkOwnership(scope, product.merchantId());
        if (product.archivedAt() == null) {
            throw ApiException.badRequest("BAR_PRODUCT_NOT_ARCHIVED", "product must be archived before requesting deletion");
        }
        if ("pending".equals(product.deleteStatus())) {
            throw ApiException.conflict("BAR_PRODUCT_DELETE_ALREADY_PENDING", "a delete request is already pending");
        }
        Instant now = Instant.now();
        String reason = request == null ? null : trimToNull(request.reason());
        repository.updateProductArchiveDelete(product.withDelete("pending", actorId(), now, reason, now));
        return repository.findProduct(id).orElseThrow();
    }

    @Transactional
    public BarCatalogDeleteDecision approveDeleteProduct(String id, BarCatalogDeleteApprovalRequest request) {
        return operations.auditedWrite(BarWriteOperation.APPROVE_DELETE_BAR_PRODUCT, () -> doApproveDeleteProduct(id, request));
    }

    private BarCatalogDeleteDecision doApproveDeleteProduct(String id, BarCatalogDeleteApprovalRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarProductRecord product = requireProduct(id);
        checkOwnership(scope, product.merchantId());
        requirePending(product.deleteStatus(), "BAR_PRODUCT_DELETE_NOT_PENDING");
        String decision = decisionOf(request);
        forbidSelfApprove(product.deleteRequestedBy());
        if ("rejected".equals(decision)) {
            repository.updateProductArchiveDelete(product.withDelete("none", null, null, null, Instant.now()));
            return new BarCatalogDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countProductReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("BAR_PRODUCT_IN_USE",
                    "product is still referenced by " + refs + " order items or combo items");
        }
        repository.deleteProduct(id);
        return new BarCatalogDeleteDecision(id, "approved", true);
    }

    // ---------- 套餐：存档/删除 ----------

    @Transactional
    public BarComboRecord archiveCombo(String id, boolean archive) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarComboRecord combo = requireCombo(id);
        checkOwnership(scope, combo.merchantId());
        Instant now = Instant.now();
        if (archive) {
            return repository.saveCombo(combo.withArchive(now, actorId(), now));
        }
        return repository.saveCombo(combo.withArchive(null, null, now).withDelete("none", null, null, null, now));
    }

    @Transactional
    public BarComboRecord requestDeleteCombo(String id, BarCatalogDeleteRequest request) {
        return operations.auditedWrite(BarWriteOperation.REQUEST_DELETE_BAR_COMBO, () -> doRequestDeleteCombo(id, request));
    }

    private BarComboRecord doRequestDeleteCombo(String id, BarCatalogDeleteRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarComboRecord combo = requireCombo(id);
        checkOwnership(scope, combo.merchantId());
        if (combo.archivedAt() == null) {
            throw ApiException.badRequest("BAR_COMBO_NOT_ARCHIVED", "combo must be archived before requesting deletion");
        }
        if ("pending".equals(combo.deleteStatus())) {
            throw ApiException.conflict("BAR_COMBO_DELETE_ALREADY_PENDING", "a delete request is already pending");
        }
        Instant now = Instant.now();
        String reason = request == null ? null : trimToNull(request.reason());
        return repository.saveCombo(combo.withDelete("pending", actorId(), now, reason, now));
    }

    @Transactional
    public BarCatalogDeleteDecision approveDeleteCombo(String id, BarCatalogDeleteApprovalRequest request) {
        return operations.auditedWrite(BarWriteOperation.APPROVE_DELETE_BAR_COMBO, () -> doApproveDeleteCombo(id, request));
    }

    private BarCatalogDeleteDecision doApproveDeleteCombo(String id, BarCatalogDeleteApprovalRequest request) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        BarComboRecord combo = requireCombo(id);
        checkOwnership(scope, combo.merchantId());
        requirePending(combo.deleteStatus(), "BAR_COMBO_DELETE_NOT_PENDING");
        String decision = decisionOf(request);
        forbidSelfApprove(combo.deleteRequestedBy());
        if ("rejected".equals(decision)) {
            repository.saveCombo(combo.withDelete("none", null, null, null, Instant.now()));
            return new BarCatalogDeleteDecision(id, "rejected", false);
        }
        // 套餐无外部活引用：bar_combo_items 是套餐自身明细（ON DELETE CASCADE 子表），无订单表引用套餐，
        // 故 countReferences 恒 0，可直接删，明细随 CASCADE 清除（诚实按 grep 结果：套餐不被外部引用）。
        repository.deleteCombo(id);
        return new BarCatalogDeleteDecision(id, "approved", true);
    }

    // ---------- helpers ----------

    private BarProductCategoryRecord requireCategory(String id) {
        return repository.findCategory(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("BAR_CATEGORY_NOT_FOUND", "category not found"));
    }

    private BarProductRecord requireProduct(String id) {
        return repository.findProduct(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "product not found"));
    }

    private BarComboRecord requireCombo(String id) {
        return repository.findCombo(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("BAR_COMBO_NOT_FOUND", "combo not found"));
    }

    private static void requirePending(String deleteStatus, String code) {
        if (!"pending".equals(deleteStatus)) {
            throw ApiException.badRequest(code, "no pending delete request");
        }
    }

    private static String decisionOf(BarCatalogDeleteApprovalRequest request) {
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(java.util.Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        return decision;
    }

    /** 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权上下文/测试）时跳过该闸。 */
    private void forbidSelfApprove(String requestedBy) {
        String actor = actorId();
        if (actor != null && actor.equals(requestedBy)) {
            throw ApiException.forbidden("BAR_CATALOG_SELF_APPROVE_FORBIDDEN",
                    "the initiator of a delete request cannot approve it");
        }
    }

    /** 当前操作者 id（存档/删除留痕 + 自审校验）；无鉴权上下文（测试/内部调用）回退 null。 */
    private String actorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

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
