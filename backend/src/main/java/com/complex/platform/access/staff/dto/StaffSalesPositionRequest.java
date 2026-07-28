package com.complex.platform.access.staff.dto;

/**
 * V349 员工「销售岗」标识维护请求。
 *
 * <p>salesPosition=是否销售岗(true=按销售人员费率计提卡座提成,false=按非销售人员费率);
 * reason=改动事由(必填)——这一勾直接决定提成按哪档费率发钱,对账追溯时必须能看到是谁为什么改的。
 */
public record StaffSalesPositionRequest(Boolean salesPosition, String reason) {
}
