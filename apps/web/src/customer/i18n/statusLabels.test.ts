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

describe("formatBusinessStatus KPI前置批全局补充", () => {
  it("商户类型/合作形式翻中文", () => {
    expect(formatBusinessStatus("tenant")).toBe("租赁商户");
    expect(formatBusinessStatus("retail")).toBe("零售");
    expect(formatBusinessStatus("catering")).toBe("餐饮");
    expect(formatBusinessStatus("entertainment")).toBe("娱乐");
    expect(formatBusinessStatus("service")).toBe("服务");
    expect(formatBusinessStatus("fixed_rent")).toBe("固定租金");
    expect(formatBusinessStatus("revenue_share")).toBe("收入分成");
    expect(formatBusinessStatus("fixed_rent_plus_commission")).toBe("固定租金+抽成");
  });

  it("业务域/统计口径/权益类型翻中文", () => {
    expect(formatBusinessStatus("ticket")).toBe("票务");
    expect(formatBusinessStatus("bar")).toBe("酒吧");
    expect(formatBusinessStatus("amusement")).toBe("游乐");
    expect(formatBusinessStatus("finance")).toBe("财务");
    expect(formatBusinessStatus("all")).toBe("全部");
    expect(formatBusinessStatus("platform")).toBe("平台");
    expect(formatBusinessStatus("project")).toBe("项目");
    expect(formatBusinessStatus("entry")).toBe("入园");
    expect(formatBusinessStatus("gift_drink")).toBe("赠饮");
  });

  it("游乐 KPI 标签用词翻中文", () => {
    expect(formatBusinessStatus("bookings")).toBe("预约");
    expect(formatBusinessStatus("sessions")).toBe("场次");
    expect(formatBusinessStatus("capacity")).toBe("容量");
    expect(formatBusinessStatus("locked")).toBe("已锁定");
    expect(formatBusinessStatus("not_issued")).toBe("未出票");
  });

  it("工单/巡检/能耗/对账流转翻中文", () => {
    expect(formatBusinessStatus("pending_review")).toBe("待复核");
    expect(formatBusinessStatus("pending_acceptance")).toBe("待验收");
    expect(formatBusinessStatus("overtime")).toBe("已超时");
    expect(formatBusinessStatus("rectifying")).toBe("整改中");
    expect(formatBusinessStatus("rechecked")).toBe("已复查");
    expect(formatBusinessStatus("recorded")).toBe("已记录");
    expect(formatBusinessStatus("reviewed")).toBe("已复核");
    expect(formatBusinessStatus("submitted")).toBe("已提交");
    expect(formatBusinessStatus("overdue")).toBe("已逾期");
    expect(formatBusinessStatus("processed")).toBe("已处理");
    expect(formatBusinessStatus("matched")).toBe("已匹配");
    expect(formatBusinessStatus("ignored")).toBe("已忽略");
  });

  it("集成适配器/退款规则/支付渠道翻中文", () => {
    expect(formatBusinessStatus("payment")).toBe("支付");
    expect(formatBusinessStatus("ticket_channel")).toBe("票务渠道");
    expect(formatBusinessStatus("wecom")).toBe("企业微信");
    expect(formatBusinessStatus("not_ready")).toBe("未就绪");
    expect(formatBusinessStatus("expired_unused")).toBe("到期未用");
    expect(formatBusinessStatus("unused")).toBe("未使用");
    expect(formatBusinessStatus("cash")).toBe("现金");
    expect(formatBusinessStatus("pos")).toBe("POS刷卡");
  });

  it("多义词不进全局表：保持既有值或原样兜底", () => {
    // 既有键含义不被覆盖
    expect(formatBusinessStatus("reserved")).toBe("已预订");
    expect(formatBusinessStatus("suspended")).toBe("已封禁");
    expect(formatBusinessStatus("merchant")).toBe("商户员工");
    expect(formatBusinessStatus("created")).toBe("已创建");
    // 多义词未入表，原样返回（页级各自处理）
    expect(formatBusinessStatus("accepted")).toBe("accepted");
    expect(formatBusinessStatus("manual")).toBe("manual");
    // 未知值兜底回归
    expect(formatBusinessStatus("totally_unknown_code")).toBe("totally_unknown_code");
    expect(formatBusinessStatus("")).toBe("—");
  });
});
