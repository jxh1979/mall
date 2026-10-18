package com.complex.platform.inventory;

// 文件版本：V0.01

import com.complex.platform.inventory.dto.*;
import org.springframework.stereotype.Service;
import com.complex.platform.gate.GuardedAction;

/** 所有旧采购入口转入同一数量、合同、审批与资金工作流。 */
@Service
public class PurchaseRequestService {
    private final PurchaseWorkflowService workflow;
    public PurchaseRequestService(PurchaseWorkflowService workflow){this.workflow=workflow;}
    public PurchaseRequestRecord approve(String id,PurchaseRequestApproveRequest r){return workflow.submitPurchase(id,r);}
    @GuardedAction(capability="inventory.purchase_request.manage",admissionAction="inventory.purchase_order",objectType="inventory_purchase_request")
    public PurchaseRequestRecord order(String id,PurchaseRequestTransitionRequest r){return workflow.transition(id,r,"ordered");}
    @GuardedAction(capability="inventory.purchase_request.manage",admissionAction="inventory.purchase_arrive",objectType="inventory_purchase_request")
    public PurchaseRequestRecord arrive(String id,PurchaseRequestTransitionRequest r){return workflow.transition(id,r,"arrived");}
    public PurchaseRequestRecord receive(String id,PurchaseRequestReceiveRequest r){return workflow.receive(id,r);}
}
