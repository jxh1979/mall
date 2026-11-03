package com.complex.platform.bar;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图形化点单 POS 目录接口（V256/V257 阶段1）：分类/套餐 CRUD、给 SKU 设分类/图片、按分类列 SKU、图片上传/回读。
 * 权限：查看类 bar.category.view / bar.combo.view（medium）；写类 bar.category.manage（medium）、
 *   bar.combo.manage（high，含内部结算价）；设商品分类/图片属商品管理 bar.product.manage（high）。
 * V262 存档/删除：archive/unarchive/delete-request 用对应 manage 权限；delete-approve 用 *.approve（critical，职责分离）。
 */
@RestController
@RequestMapping("/api/v1/bar/catalog")
public class BarPosCatalogController {

    private final BarPosCatalogService service;
    private final BarPosImageStorage imageStorage;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;
    private final AuditEventService audit;

    // 第十八批 N 段第 124 条 124-R5（PGM L966「已拍板：所有有过程的业务动作都必须留痕……」、L738「访问被拒绝也要留痕……」）：写口任一步失败
    // 经 AuditedOperationExecutor.recordingFailure 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；服务层原本不写成功事件的，
    // 服务返回后在这里落声明的 x-audit-event-success。
    public BarPosCatalogController(BarPosCatalogService service, BarPosImageStorage imageStorage,
            AuthorizationService authorizationService, AuditedOperationExecutor operations, AuditEventService audit) {
        this.service = service;
        this.imageStorage = imageStorage;
        this.authorizationService = authorizationService;
        this.operations = operations;
        this.audit = audit;
    }

    // ---------- 分类 ----------

    @GetMapping("/categories")
    public ApiResponse<List<BarProductCategoryRecord>> listCategories(
            @RequestParam(value = "bar_id", required = false) String barId,
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("bar.category.view");
        return ApiResponse.ok(service.listCategories(barId, includeArchived));
    }

    @PostMapping("/categories")
    public ApiResponse<BarProductCategoryRecord> saveCategory(
            @RequestBody(required = false) BarProductCategoryRequest request) {
        return ApiResponse.ok(operations.recordingFailure("bar.category.saved_failed", "bar.category.manage", "bar_product_category",
                request == null || request.id() == null ? "new" : request.id(), null, () -> {
                    authorizationService.requirePermission("bar.category.manage");
                    BarProductCategoryRecord saved = service.saveCategory(request);
                    audit.recordSuccess("bar.category.saved", "bar.category.manage", "bar_product_category", saved.id(), null, null, Map.of());
                    return saved;
                }));
    }

    @PostMapping("/categories/{id}/archive")
    public ApiResponse<BarProductCategoryRecord> archiveCategory(@PathVariable("id") String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) BarCatalogArchiveRequest request) {
        authorizationService.requirePermission("bar.category.manage");
        return ApiResponse.ok(service.archiveCategory(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/categories/{id}/unarchive")
    public ApiResponse<BarProductCategoryRecord> unarchiveCategory(@PathVariable("id") String id) {
        authorizationService.requirePermission("bar.category.manage");
        return ApiResponse.ok(service.archiveCategory(id, false, null));
    }

    @PostMapping("/categories/{id}/delete-request")
    public ApiResponse<BarProductCategoryRecord> requestDeleteCategory(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteRequest request) {
        authorizationService.requirePermission("bar.category.manage");
        return ApiResponse.ok(service.requestDeleteCategory(id, request));
    }

    @PostMapping("/categories/{id}/delete-approve")
    public ApiResponse<BarCatalogDeleteDecision> approveDeleteCategory(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("bar.category.approve");
        return ApiResponse.ok(service.approveDeleteCategory(id, request));
    }

    // ---------- 给商品设分类/图片、按分类列 SKU、商品存档/删除 ----------

    @PostMapping("/products/{product_id}/category")
    public ApiResponse<BarProductRecord> assignProductCategory(
            @PathVariable("product_id") String productId,
            @RequestBody(required = false) BarProductCategoryAssignRequest request) {
        return ApiResponse.ok(operations.recordingFailure("bar.product.category_assigned_failed", "bar.product.manage", "bar_product",
                productId, null, () -> {
                    authorizationService.requirePermission("bar.product.manage");
                    BarProductRecord saved = service.assignCategory(productId, request);
                    audit.recordSuccess("bar.product.category_assigned", "bar.product.manage", "bar_product", productId, null, null,
                            Map.of("category_id", request == null || request.categoryId() == null ? "" : request.categoryId()));
                    return saved;
                }));
    }

    @GetMapping("/products")
    public ApiResponse<List<BarProductRecord>> listProductsByCategory(
            @RequestParam("bar_id") String barId,
            @RequestParam(value = "category_id", required = false) String categoryId) {
        authorizationService.requirePermission("bar.product.view");
        java.util.List<BarProductRecord> products = service.listProductsByCategory(barId, categoryId);
        if (!authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")) {
            products = products.stream().map(BarProductRecord::withoutInternalPrice).toList();
        }
        return ApiResponse.ok(products);
    }

    @PostMapping("/products/{id}/archive")
    public ApiResponse<BarProductRecord> archiveProduct(@PathVariable("id") String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) BarCatalogArchiveRequest request) {
        authorizationService.requirePermission("bar.product.manage");
        return ApiResponse.ok(service.archiveProduct(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/products/{id}/unarchive")
    public ApiResponse<BarProductRecord> unarchiveProduct(@PathVariable("id") String id) {
        authorizationService.requirePermission("bar.product.manage");
        return ApiResponse.ok(service.archiveProduct(id, false, null));
    }

    @PostMapping("/products/{id}/delete-request")
    public ApiResponse<BarProductRecord> requestDeleteProduct(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteRequest request) {
        authorizationService.requirePermission("bar.product.manage");
        return ApiResponse.ok(service.requestDeleteProduct(id, request));
    }

    @PostMapping("/products/{id}/delete-approve")
    public ApiResponse<BarCatalogDeleteDecision> approveDeleteProduct(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("bar.product.approve");
        return ApiResponse.ok(service.approveDeleteProduct(id, request));
    }

    // ---------- 套餐 ----------

    @GetMapping("/combos")
    public ApiResponse<List<BarComboRecord>> listCombos(
            @RequestParam(value = "bar_id", required = false) String barId,
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("bar.combo.view");
        java.util.List<BarComboRecord> combos = service.listCombos(barId, includeArchived);
        if (!authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")) {
            combos = combos.stream().map(BarComboRecord::withoutInternalPrice).toList();
        }
        return ApiResponse.ok(combos);
    }

    @PostMapping("/combos")
    public ApiResponse<BarComboRecord> saveCombo(
            @RequestBody(required = false) BarComboRequest request) {
        return ApiResponse.ok(operations.recordingFailure("bar.combo.saved_failed", "bar.combo.manage", "bar_combo",
                request == null || request.id() == null ? "new" : request.id(), null, () -> {
                    authorizationService.requirePermission("bar.combo.manage");
                    BarComboRecord saved = service.saveCombo(request);
                    audit.recordSuccess("bar.combo.saved", "bar.combo.manage", "bar_combo", saved.id(), null, null, Map.of());
                    return saved;
                }));
    }

    @PostMapping("/combos/{id}/archive")
    public ApiResponse<BarComboRecord> archiveCombo(@PathVariable("id") String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) BarCatalogArchiveRequest request) {
        authorizationService.requirePermission("bar.combo.manage");
        return ApiResponse.ok(service.archiveCombo(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/combos/{id}/unarchive")
    public ApiResponse<BarComboRecord> unarchiveCombo(@PathVariable("id") String id) {
        authorizationService.requirePermission("bar.combo.manage");
        return ApiResponse.ok(service.archiveCombo(id, false, null));
    }

    @PostMapping("/combos/{id}/delete-request")
    public ApiResponse<BarComboRecord> requestDeleteCombo(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteRequest request) {
        authorizationService.requirePermission("bar.combo.manage");
        return ApiResponse.ok(service.requestDeleteCombo(id, request));
    }

    @PostMapping("/combos/{id}/delete-approve")
    public ApiResponse<BarCatalogDeleteDecision> approveDeleteCombo(
            @PathVariable("id") String id, @RequestBody(required = false) BarCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("bar.combo.approve");
        return ApiResponse.ok(service.approveDeleteCombo(id, request));
    }

    // ---------- 图片上传 / 回读（本地存储，同 image_url 先例） ----------

    /** 上传商品/套餐图片，返回可写入 image_url 的相对路径。product_id 非空时同时写回该商品图片。 */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "product_id", required = false) String productId) {
        return ApiResponse.ok(operations.recordingFailure("bar.catalog.image_uploaded_failed", "bar.product.manage", "bar_catalog_image",
                "new", null, () -> {
                    authorizationService.requirePermission("bar.product.manage");
                    String imageUrl = imageStorage.store(file);
                    if (productId != null && !productId.isBlank()) {
                        service.setProductImage(productId, imageUrl);
                    }
                    audit.recordSuccess("bar.catalog.image_uploaded", "bar.product.manage", "bar_catalog_image", imageUrl, null, null,
                            Map.of("product_id", productId == null ? "" : productId));
                    return Map.of("imageUrl", imageUrl);
                }));
    }

    /** 回读图片（鉴权走既有 API 会话，不开公共静态目录）。 */
    @GetMapping("/images/{name}")
    public ResponseEntity<Resource> readImage(@PathVariable("name") String name) {
        authorizationService.requirePermission("bar.product.view");
        Path path = imageStorage.resolveForRead(name);
        MediaType mediaType = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(path));
    }
}
