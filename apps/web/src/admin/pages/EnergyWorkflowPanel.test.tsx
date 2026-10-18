// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EnergyWorkflowPanel } from "./EnergyWorkflowPanel";
import type { AdminPageProps } from "../types";

function setup() {
  const post=vi.fn().mockResolvedValue({id:"saved"}),postForm=vi.fn().mockResolvedValue({id:"photo-1"}),get=vi.fn().mockResolvedValue([]);
  const api={post,postForm,get} as unknown as AdminPageProps["api"];
  render(<EnergyWorkflowPanel api={api} effectivePermissions={{status:"ready",value:{grants:[],denies:[],wildcard:true}}} setActionResult={vi.fn()} setGlobalError={vi.fn()} meters={[{id:"meter-1",name:"商户独表",projectId:"project-1"}]} readings={[{id:"reading-1",meterId:"meter-1",status:"abnormal"}]} period="2026-06" reload={vi.fn().mockResolvedValue(undefined)}/>);
  fireEvent.change(screen.getByLabelText("能源处理表计"),{target:{value:"meter-1"}});
  return {post,postForm,get};
}
async function upload(){
  fireEvent.change(screen.getByLabelText("能源真实凭据"),{target:{files:[new File(["actual photo"],"表底.png",{type:"image/png"})]}});
  await waitFor(()=>expect(screen.getByText("表底.png")).toBeTruthy());
}
describe("能源计量和实际费用完整入口",()=>{
  it("缺真实凭据时不提交换表，上传后只绑定所选原表",async()=>{
    const {post,postForm}=setup();
    fireEvent.click(screen.getByText("登记真实换表"));
    await waitFor(()=>expect(screen.getByRole("alert").textContent).toContain("上传"));
    expect(post).not.toHaveBeenCalled();
    await upload();
    const form=postForm.mock.calls[0]![1] as FormData;
    expect(form.get("business_type")).toBe("energy_meter");
    expect(form.get("business_id")).toBe("meter-1");
    fireEvent.change(screen.getByLabelText("能源交接时点"),{target:{value:"2026-06-16T10:00"}});
    fireEvent.change(screen.getByLabelText("能源启用初值"),{target:{value:"100"}});
    fireEvent.change(screen.getByLabelText("能源旧表终值"),{target:{value:"5000"}});
    fireEvent.change(screen.getByLabelText("能源新表名称"),{target:{value:"新电表"}});
    fireEvent.change(screen.getByLabelText("能源处理说明"),{target:{value:"现场交接完成"}});
    fireEvent.click(screen.getByText("登记真实换表"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/meters/meter-1/replacement",expect.objectContaining({oldFinalValue:5000,newInitialValue:100,newMeterName:"新电表",attachmentIds:["photo-1"]})));
  });
  it("永久停用只提交真实终值时点与照片，不创建替代表",async()=>{
    const {post}=setup();
    fireEvent.click(screen.getByText("按实际终值永久停用本表"));
    await waitFor(()=>expect(screen.getByRole("alert").textContent).toContain("上传"));
    expect(post).not.toHaveBeenCalled();
    await upload();
    fireEvent.change(screen.getByLabelText("能源交接时点"),{target:{value:"2026-06-30T10:00"}});
    fireEvent.change(screen.getByLabelText("能源旧表终值"),{target:{value:"150"}});
    fireEvent.change(screen.getByLabelText("能源处理说明"),{target:{value:"现场永久停用"}});
    fireEvent.click(screen.getByText("按实际终值永久停用本表"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/meters/meter-1/retirement",expect.objectContaining({period:"2026-06",finalValue:150,retiredAt:new Date("2026-06-30T10:00").toISOString(),note:"现场永久停用",attachmentIds:["photo-1"]})));
    expect(post.mock.calls[0]![1]).not.toHaveProperty("newMeterName");
  });
  it("异常必须提交核表证据和主管确认，页面不能代填核表人",async()=>{
    const {post}=setup();await upload();
    fireEvent.click(screen.getByRole("tab",{name:"异常核表"}));
    fireEvent.change(screen.getByLabelText("能源待核异常"),{target:{value:"reading-1"}});
    fireEvent.change(screen.getByLabelText("能源核实终值"),{target:{value:"150"}});
    fireEvent.change(screen.getByLabelText("能源核实起点"),{target:{value:"100"}});
    fireEvent.change(screen.getByLabelText("能源确认主管"),{target:{value:"supervisor-1"}});
    fireEvent.change(screen.getByLabelText("能源处理说明"),{target:{value:"原录入值有误，已现场复抄"}});
    fireEvent.click(screen.getByText("以本人身份提交核表并申请主管确认"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/readings/reading-1/verification",expect.objectContaining({verifiedValue:150,verifiedOpeningValue:100,method:"reread",finalApproverId:"supervisor-1",attachmentIds:["photo-1"]})));
    expect(post.mock.calls[0]![1]).not.toHaveProperty("verifiedBy");
    expect(post.mock.calls[0]![1]).not.toHaveProperty("reviewedBy");
  });
  it("实际费用按明确金额和供能到期日提交，不填单价和公摊率",async()=>{
    const {post}=setup();await upload();
    fireEvent.click(screen.getByRole("tab",{name:"独表实际费用"}));
    fireEvent.change(screen.getByLabelText("能源实际费用合同"),{target:{value:"contract-1"}});
    fireEvent.change(screen.getByLabelText("能源供能账单号"),{target:{value:"供电账单001"}});
    fireEvent.change(screen.getByLabelText("能源实际费用金额"),{target:{value:"124.05"}});
    fireEvent.change(screen.getByLabelText("能源最后付款日"),{target:{value:"2026-07-08"}});
    fireEvent.click(screen.getByText("确认凭据与独表用量一致，登记实际费用"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/actual-charges",expect.objectContaining({amountCent:12405,dueDate:"2026-07-08",period:"2026-06",meterId:"meter-1",contractId:"contract-1"})));
    expect(post.mock.calls[0]![1]).not.toHaveProperty("unitPrice");
  });
  it("主管从超期待办定位原读数，再指派真实复抄人",async()=>{
    const {get,post}=setup();
    get.mockResolvedValue([{readingId:"reading-1",meterId:"meter-1",period:"2026-06",deadlineAt:"2026-07-07T00:00:00Z"}]);
    fireEvent.click(screen.getByText("查看指派给我的超期异常"));
    fireEvent.click(await screen.findByText("选择此异常办理复抄"));
    fireEvent.click(screen.getByRole("tab",{name:"异常核表"}));
    expect((screen.getByLabelText("能源待核异常") as HTMLSelectElement).value).toBe("reading-1");
    fireEvent.change(screen.getByLabelText("能源指派复抄人"),{target:{value:"second-person"}});
    fireEvent.change(screen.getByLabelText("能源处理说明"),{target:{value:"超期安排现场复抄"}});
    fireEvent.click(screen.getByText("主管指派复抄"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/readings/reading-1/assignment",expect.objectContaining({verifierId:"second-person",reason:"超期安排现场复抄"})));
  });
  it("配置未填上下限不得提交，默认七天不冒充已经批准",async()=>{
    const {post}=setup();
    fireEvent.click(screen.getByRole("tab",{name:"核实期限"}));
    expect((screen.getByLabelText("能源核实天数") as HTMLInputElement).value).toBe("7");
    fireEvent.click(screen.getByText("提交期限配置审批"));
    await waitFor(()=>expect(screen.getByRole("alert").textContent).toContain("上下限"));
    expect(post).not.toHaveBeenCalled();
  });
});
