package com.complex.platform.inventory;

// 文件版本：V0.01

import com.complex.platform.inventory.dto.*;
import com.complex.platform.common.api.PageResponse;
import org.springframework.stereotype.Service;

@Service
public class SupplyContractService {
    private final PurchaseWorkflowService workflow;
    public SupplyContractService(PurchaseWorkflowService workflow){this.workflow=workflow;}
    public PageResponse<SupplyContractRecord> listSupplyContracts(){return workflow.contracts();}
    public SupplyContractRecord createSupplyContract(SupplyContractCreateRequest r){return workflow.createContract(r);}
    public SupplyContractRecord approveSupplyContract(String id,SupplyContractApproveRequest r){return workflow.submitContract(id,r);}
    public SupplyContractRecord requirePurchasable(String id,String item){return workflow.requirePurchasable(id,item);}
}
