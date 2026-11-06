// 文件版本：V0.02
import { describe, expect, it } from "vitest";
import { apiErrorFromResponse, formatApiErrorDetail } from "./apiError";

function msgFor(status: number, body: Record<string, unknown>, method = "POST"): string {
  return apiErrorFromResponse("/api/v1/x", status, JSON.stringify(body), method).message;
}

describe("apiError translate — 字段级报错", () => {
  it("PARAM_INVALID 单字段 required 精确到字段中文", () => {
    const m = msgFor(400, { code: "PARAM_INVALID", message: "merchant_name is required" });
    expect(m).toContain("商户名称");
    expect(m).toContain("未填写");
    expect(m).not.toContain("提交内容有误，请检查");
  });

  it("字段 invalid → 格式不正确", () => {
    const m = msgFor(400, { code: "PARAM_INVALID", message: "commission_mode is invalid" });
    expect(m).toContain("分成方式");
    expect(m).toContain("格式不正确");
  });

  it("VALIDATION_FAILED 多字段逐段翻译", () => {
    const m = msgFor(400, {
      code: "PARAM_INVALID",
      detail_code: "VALIDATION_FAILED",
      message: "merchant_id: must not be null; name: must not be blank",
    });
    expect(m).toContain("商户");
    expect(m).toContain("名称");
    expect(m).toContain("未填写");
  });

  it("字典缺失字段降级显示原文，不再丢信息", () => {
    const m = msgFor(400, { code: "PARAM_INVALID", message: "zzz_unknown_field is required" });
    expect(m).toContain("zzz_unknown_field");
    expect(m).toContain("未填写");
  });

  it("idempotency_key 类机读键映射为用户话术", () => {
    const m = msgFor(400, { code: "PARAM_INVALID", message: "idempotency_key is required" });
    expect(m).not.toContain("idempotency");
    expect(m).toContain("重复提交");
  });

  it("CONTRACT_SCOPE_VIOLATION（合同经营范围越界）透传中文业务话术", () => {
    // 酒吧 POS/后台下单越界：后端 409 + 中文消息 → err.message 直接是业务语言（62 页 err.message 渲染点自动受益）。
    const err = apiErrorFromResponse("/api/v1/bar/orders", 409, JSON.stringify({
      code: "STATE_CONFLICT",
      detail_code: "CONTRACT_SCOPE_VIOLATION",
      message: "超出合同经营范围：品类「烈酒」不在本商户合同约定的经营范围内，禁止销售",
    }), "POST");
    expect(err.detailCode).toBe("CONTRACT_SCOPE_VIOLATION");
    expect(err.message).toContain("超出合同经营范围");
    expect(err.message).toContain("烈酒");
    expect(err.message).not.toMatch(/[A-Za-z_]{4,}/);
  });

  it("409 STATE_CONFLICT → 操作冲突（区别于读取失败）", () => {
    const m = msgFor(409, { code: "STATE_CONFLICT", message: "已被占用" });
    expect(m).toContain("操作冲突");
    expect(m).toContain("已被占用");
    expect(m).not.toContain("读取");
  });

  it("IDEMPOTENCY_CONFLICT → 请勿重复提交类", () => {
    const m = msgFor(409, {
      code: "IDEMPOTENCY_CONFLICT",
      detail_code: "IDEMPOTENCY_CONFLICT",
      message: "idempotency_key was already used with a different request",
    });
    expect(m).toContain("重复");
    expect(m).not.toContain("idempotency");
  });

  it("500 保持服务不可用（技术详情走 detail）", () => {
    expect(msgFor(500, { code: "INTERNAL_ERROR", message: "boom" })).toContain("服务暂时不可用");
  });

  it("INVALID_CREDENTIALS 保持不变", () => {
    const m = msgFor(401, { code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "bad" });
    expect(m).toBe("账号或密码不正确");
  });

  it("GET 读取失败与提交错误区分：404 读取语义", () => {
    expect(msgFor(404, { code: "NOT_FOUND", message: "x" }, "GET")).toContain("不存在");
  });
});

// 第 193 条 193-R2（PGM:287「可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符」）：
// 技术详情折叠块只留状态码；接口路径、request_id、原始报文不上屏。
describe("formatApiErrorDetail — 技术详情只留状态码", () => {
  it("界面文本只有状态码，不含接口路径、request_id、原始报文", () => {
    const raw = JSON.stringify({ code: "STATE_CONFLICT", message: "已被占用", request_id: "req-1" });
    expect(formatApiErrorDetail(apiErrorFromResponse("/api/v1/bar/orders", 409, raw, "POST"))).toBe("状态码 409");
    expect(formatApiErrorDetail(apiErrorFromResponse("/api/v1/x", 502, "<html>Bad Gateway</html>", "GET"))).toBe("状态码 502");
  });

  it("非 ApiError 不给技术详情", () => {
    expect(formatApiErrorDetail(new Error("x"))).toBeNull();
  });
});
