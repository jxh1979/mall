import { describe, it, expect } from "vitest";
import { formatBusinessStatus } from "./statusLabels";

describe("formatBusinessStatus 新增枚举域中文化", () => {
  it("工单类型/优先级/巡检类型翻中文", () => {
    expect(formatBusinessStatus("repair")).toBe("报修");
    expect(formatBusinessStatus("rectification")).toBe("整改");
    expect(formatBusinessStatus("critical")).toBe("紧急");
    expect(formatBusinessStatus("security")).toBe("安防");
    expect(formatBusinessStatus("environment")).toBe("环境");
  });

  it("风险预警/主管复核/售票渠道翻中文", () => {
    expect(formatBusinessStatus("staff_behavior")).toBe("员工行为");
    expect(formatBusinessStatus("suspicious")).toBe("可疑");
    expect(formatBusinessStatus("contract_violation")).toBe("合同违规");
    expect(formatBusinessStatus("web")).toBe("网页端");
    expect(formatBusinessStatus("miniapp")).toBe("小程序");
    expect(formatBusinessStatus("channel")).toBe("第三方渠道");
  });

  it("授权申请类型/审计结果/巡检异常模式翻中文", () => {
    expect(formatBusinessStatus("temporary_permission")).toBe("临时权限");
    expect(formatBusinessStatus("success")).toBe("成功");
    expect(formatBusinessStatus("failure")).toBe("失败");
    expect(formatBusinessStatus("to_work_order")).toBe("转工单");
    expect(formatBusinessStatus("abnormal")).toBe("异常");
  });

  it("未知码原样兜底，空值给占位符", () => {
    expect(formatBusinessStatus("some_unmapped_code")).toBe("some_unmapped_code");
    expect(formatBusinessStatus(null)).toBe("—");
    expect(formatBusinessStatus(undefined)).toBe("—");
  });
});
