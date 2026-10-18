package com.complex.platform.dealer.dto;

/** 准入审核：decision approve / reject；四项资料逐项勾核（BSM:1217），全部通过才可 approve。 */
public record DealerReviewRequest(String idempotencyKey, String decision, Boolean companyProfileReviewed, Boolean contractReviewed,
        Boolean invoiceReviewed, Boolean bankReviewed, String note) {
}
