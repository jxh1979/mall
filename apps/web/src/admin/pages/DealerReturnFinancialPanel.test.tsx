// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DealerInvoiceActions, DealerReturnFinancialPanel } from "./DealerReturnFinancialPanel";
import type { AdminApiClient, AdminPageProps } from "../types";

function fixture(viewOnly = false) {
  const api = {
    get: vi.fn().mockResolvedValue({goodsAmountCent:1000,goodsOffsetCent:0,goodsRefundCent:1000,goodsRefundedCent:0,firstLegAdjustmentCent:0,firstLegOffsetCent:0,firstLegRefundCent:0,firstLegRefundedCent:0,refunds:[]}),
    post: vi.fn().mockResolvedValue({}), postForm: vi.fn().mockResolvedValue({id:"actual_file"}),
  };
  const props:AdminPageProps={api:api as unknown as AdminApiClient,user:{id:"finance",username:"财务",accountType:"staff"},setActionResult:vi.fn(),setGlobalError:vi.fn(),
    effectivePermissions:{status:"ready",value:{grants:viewOnly?["dealer.return.view"]:[],denies:[],wildcard:!viewOnly}}};
  return {api,props};
}

describe("退货真实款项与发票操作",()=>{
  it("运费不猜金额且必须先上传凭证，提交后仍显示实际未退",async()=>{
    const {api,props}=fixture();render(<DealerReturnFinancialPanel {...props} returnId="ret" status="completed" bearer="pending_confirmation" onChanged={vi.fn()} />);
    fireEvent.click(await screen.findByTestId("freight-confirm"));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("freight-amount"),{target:{value:"12.34"}});
    fireEvent.click(screen.getByTestId("freight-first-leg"));
    fireEvent.change(screen.getByTestId("freight-note"),{target:{value:"双方确认本次首程退还12.34元"}});
    fireEvent.click(screen.getByTestId("freight-confirm"));expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("freight-evidence"),{target:{files:[new File(["actual"],"agreement.png",{type:"image/png"})]}});
    await screen.findByText("agreement.png");fireEvent.click(screen.getByTestId("freight-confirm"));
    await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/returns/ret/freight-confirmation",expect.objectContaining({firstLegAdjustmentCent:1234,evidenceAttachmentIds:["actual_file"]})));
    expect(screen.getByTestId("first-leg-refunded").textContent).toBe("¥0.00");
  });
  it("驳回的双方确认方案可重新改承担方和首程退费，不当作锁定合同",async()=>{
    const {api,props}=fixture();
    render(<DealerReturnFinancialPanel {...props} returnId="ret" status="completed" bearer="sales_entity" refundable
      contract={{returnFreightBearer:"pending_confirmation",firstLegFreightRefundable:null}} onChanged={vi.fn()} />);
    await screen.findByTestId("freight-confirm");
    expect((screen.getByTestId("freight-bearer") as HTMLSelectElement).disabled).toBe(false);
    expect((screen.getByTestId("freight-first-leg") as HTMLInputElement).disabled).toBe(false);
    fireEvent.change(screen.getByTestId("freight-bearer"),{target:{value:"dealer"}});
    fireEvent.click(screen.getByTestId("freight-first-leg"));
    fireEvent.change(screen.getByTestId("freight-amount"),{target:{value:"0"}});
    fireEvent.change(screen.getByTestId("freight-note"),{target:{value:"驳回后双方重新确认首程不退"}});
    fireEvent.change(screen.getByTestId("freight-evidence"),{target:{files:[new File(["actual"],"重新确认.png",{type:"image/png"})]}});
    await screen.findByText("重新确认.png");fireEvent.click(screen.getByTestId("freight-confirm"));
    await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/returns/ret/freight-confirmation",
      expect.objectContaining({returnFreightBearer:"dealer",firstLegFreightRefundable:false,firstLegAdjustmentCent:0,evidenceAttachmentIds:["actual_file"]})));
  });
  it("原合同已约定的首程规则仍锁定，质量退回承担方固定销售主体",async()=>{
    const {props}=fixture();
    render(<DealerReturnFinancialPanel {...props} returnId="ret" status="completed" reasonType="quality_issue"
      bearer="pending_confirmation" contract={{returnFreightBearer:"dealer",firstLegFreightRefundable:false}} onChanged={vi.fn()} />);
    await screen.findByTestId("freight-confirm");
    const bearer=screen.getByTestId("freight-bearer") as HTMLSelectElement;
    const refundable=screen.getByTestId("freight-first-leg") as HTMLInputElement;
    expect(bearer.disabled).toBe(true);expect(bearer.value).toBe("sales_entity");
    expect(refundable.disabled).toBe(true);expect(refundable.checked).toBe(false);
  });
  it("红冲必须有真实编号和文件，默认处理中不伪报成功",async()=>{
    const {api,props}=fixture();render(<DealerInvoiceActions {...props} invoice={{id:"inv",status:"issued",requiresReversal:true}} onChanged={vi.fn()} />);
    fireEvent.change(screen.getByTestId("invoice-reference-inv"),{target:{value:"RED-EXTERNAL"}});
    fireEvent.click(screen.getByTestId("invoice-issue-inv"));expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("invoice-evidence-inv"),{target:{files:[new File(["actual"],"red.png",{type:"image/png"})]}});
    await screen.findByText("red.png");fireEvent.click(screen.getByTestId("invoice-issue-inv"));
    await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/invoices/inv/reversal-result",expect.objectContaining({result:"processing",referenceNo:"RED-EXTERNAL",evidenceAttachmentIds:["actual_file"]})));
    expect(api.post).not.toHaveBeenCalledWith(expect.stringContaining("/issue"),expect.anything());
  });
  it("只读人员可看资金事实但不能上传、审批或登记退款",async()=>{
    const {api,props}=fixture(true);render(<DealerReturnFinancialPanel {...props} returnId="ret" status="completed" refundApproved bearer="pending_confirmation" onChanged={vi.fn()} />);
    const confirm=await screen.findByTestId("freight-confirm");expect((confirm as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("return-refund-prepare") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("freight-evidence") as HTMLInputElement).disabled).toBe(true);
    fireEvent.click(confirm);fireEvent.click(screen.getByTestId("return-refund-prepare"));expect(api.post).not.toHaveBeenCalled();
    expect(screen.getByTestId("first-leg-refunded").textContent).toBe("¥0.00");
  });
});
