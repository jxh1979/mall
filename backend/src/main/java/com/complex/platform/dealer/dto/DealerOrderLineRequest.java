package com.complex.platform.dealer.dto;

/** inquiryId 非空＝按已报价询价单订购（BSM:1220「报价审批通过后才能订购」），否则按合同价格目录。 */
public record DealerOrderLineRequest(String itemId, String spec, Integer quantity, String inquiryId) {
}
