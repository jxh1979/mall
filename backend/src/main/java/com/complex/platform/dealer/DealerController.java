package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerDeactivateRequest;
import com.complex.platform.dealer.dto.DealerIdempotentRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerQuoteDecisionRequest;
import com.complex.platform.dealer.dto.DealerQuoteRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CG-11a 经销商准入、合同、价格目录、询价与库存外显（内部后台；经销商门户见 CG-11d）。 */
@RestController
@RequestMapping("/api/v1/dealers")
public class DealerController {

    private final DealerService service;

    public DealerController(DealerService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<DealerRecord>> listDealers() {
        return ApiResponse.ok(service.listDealers());
    }

    @PostMapping
    public ApiResponse<DealerRecord> createDealer(@RequestBody DealerCreateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDealer(request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<DealerRecord> submitDealer(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitDealer(id, request.idempotencyKey()));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<DealerRecord> reviewDealer(@PathVariable String id, @RequestBody DealerReviewRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewDealer(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<DealerRecord> deactivateDealer(@PathVariable String id, @RequestBody DealerDeactivateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.deactivateDealer(id, request));
    }

    @GetMapping("/{id}/contracts")
    public ApiResponse<PageResponse<DealerContractRecord>> listContracts(@PathVariable String id) {
        return ApiResponse.ok(service.listContracts(id));
    }

    @PostMapping("/{id}/contracts")
    public ApiResponse<DealerContractRecord> createContract(@PathVariable String id, @RequestBody DealerContractRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createContract(id, request));
    }

    @GetMapping("/contracts/{id}/price-catalogs")
    public ApiResponse<PageResponse<DealerPriceCatalogRecord>> listCatalogs(@PathVariable String id) {
        return ApiResponse.ok(service.listCatalogs(id));
    }

    @PostMapping("/contracts/{id}/price-catalogs")
    public ApiResponse<DealerPriceCatalogRecord> createCatalogDraft(@PathVariable String id, @RequestBody DealerCatalogDraftRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createCatalogDraft(id, request));
    }

    @GetMapping("/contracts/{id}/external-stock")
    public ApiResponse<PageResponse<DealerExternalStockRecord>> externalStock(@PathVariable String id) {
        return ApiResponse.ok(service.externalStock(id));
    }

    @GetMapping("/price-catalogs/{id}/lines")
    public ApiResponse<PageResponse<DealerPriceCatalogLineRecord>> listCatalogLines(@PathVariable String id) {
        return ApiResponse.ok(service.listCatalogLines(id));
    }

    @PostMapping("/price-catalogs/{id}/submit")
    public ApiResponse<DealerPriceCatalogRecord> submitCatalog(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitCatalog(id, request.idempotencyKey()));
    }

    @PostMapping("/price-catalogs/{id}/decision")
    public ApiResponse<DealerPriceCatalogRecord> decideCatalog(@PathVariable String id, @RequestBody DealerCatalogDecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideCatalog(id, request));
    }

    @GetMapping("/{id}/inquiries")
    public ApiResponse<PageResponse<DealerInquiryRecord>> listInquiries(@PathVariable String id) {
        return ApiResponse.ok(service.listInquiries(id));
    }

    @PostMapping("/{id}/inquiries")
    public ApiResponse<DealerInquiryRecord> createInquiry(@PathVariable String id, @RequestBody DealerInquiryRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInquiry(id, request));
    }

    @PostMapping("/inquiries/{id}/quote")
    public ApiResponse<DealerInquiryRecord> quoteInquiry(@PathVariable String id, @RequestBody DealerQuoteRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.quoteInquiry(id, request));
    }

    @PostMapping("/inquiries/{id}/decision")
    public ApiResponse<DealerInquiryRecord> decideQuote(@PathVariable String id, @RequestBody DealerQuoteDecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideQuote(id, request));
    }

    @PostMapping("/inquiries/{id}/expire")
    public ApiResponse<DealerInquiryRecord> expireInquiry(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.expireInquiry(id, request.idempotencyKey()));
    }
}
