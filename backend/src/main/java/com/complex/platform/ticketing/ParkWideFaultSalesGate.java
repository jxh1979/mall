package com.complex.platform.ticketing;

/**
 * CG-08a 全园故障售票闸（ENUM「全园故障与补偿通票」：全园故障状态一经生效，必须立即停止所有纯游乐门票和所有包含游乐权益的
 * 通票的新销售；解除后恢复）。由 amusement 域实现，票务下单前按订单所属综合体查询。
 */
public interface ParkWideFaultSalesGate {

    /** 该综合体是否处于生效中的全园故障状态；projectId 为 null（订单无法定位综合体）时按任一综合体生效即停售（fail-closed）。 */
    boolean salesSuspended(String projectId);
}
