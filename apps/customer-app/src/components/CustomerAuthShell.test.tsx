// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ApiError, type CustomerService } from "@complex/customer-core";

import { CustomerAuthShell } from "./CustomerAuthShell";

// BSM「登录可支持手机号短信验证码和微信；创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款」；
// 操作员裁决第十三批第 3 条「满 18 周岁（推荐）」。新手机号登录不自动建号（服务端 409 CUSTOMER_REGISTRATION_REQUIRED），转注册。
const TERMS = { memberTerms: { version: "mt-v1", content: "会员条款正文" }, privacyPolicy: { version: "pp-v1", content: "隐私条款正文" } };
const LOGIN = { customerId: "c-1", maskedPhone: "138****8888", customerToken: "tok", expiresAt: "2026-12-31T00:00:00Z" };

function apiError(status: number, detailCode: string, message: string): ApiError {
  return new ApiError(message, { detail: { path: "/api/v1/customer/auth", status, requestId: null, raw: "" }, code: "STATE_CONFLICT", detailCode });
}

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    requestLoginSmsCode: vi.fn().mockResolvedValue({ phoneMasked: "138****8888", requestedAt: "2026-10-24T00:00:00Z" }),
    phoneLogin: vi.fn().mockResolvedValue(LOGIN),
    getTerms: vi.fn().mockResolvedValue(TERMS),
    register: vi.fn().mockResolvedValue(LOGIN),
    ...overrides,
  };
}

function renderShell(service: ReturnType<typeof makeService>, onAuthenticated = vi.fn()) {
  render(<CustomerAuthShell service={service as unknown as CustomerService} onAuthenticated={onAuthenticated} />);
  return onAuthenticated;
}

function fillPhoneAndCode() {
  fireEvent.change(screen.getByTestId("login-phone"), { target: { value: "13800138888" } });
  fireEvent.change(screen.getByTestId("login-sms"), { target: { value: "123456" } });
}

async function goToRegister(service: ReturnType<typeof makeService>) {
  await waitFor(() => expect(screen.queryByTestId("login-phone")).not.toBeNull());
  fillPhoneAndCode();
  fireEvent.click(screen.getByTestId("login-submit"));
  await waitFor(() => expect(screen.queryByTestId("customer-register-view")).not.toBeNull());
  await waitFor(() => expect(service.getTerms).toHaveBeenCalled());
  await waitFor(() => expect(screen.queryByTestId("register-submit")).not.toBeDisabled());
}

function pickBirthDate(value: string) {
  const input = screen.getByLabelText("生日");
  fireEvent.mouseDown(input);
  fireEvent.change(input, { target: { value } });
  fireEvent.keyDown(input, { key: "Enter", code: "Enter", keyCode: 13 });
}

describe("CustomerAuthShell 登录与注册", () => {
  it("requestsSmsCodeThenLogsIn：先取短信验证码再登录", async () => {
    const service = makeService();
    const onAuthenticated = renderShell(service);
    await waitFor(() => expect(screen.queryByTestId("login-sms-request")).not.toBeNull());
    fireEvent.change(screen.getByTestId("login-phone"), { target: { value: "13800138888" } });
    fireEvent.click(screen.getByTestId("login-sms-request"));
    await waitFor(() => expect(service.requestLoginSmsCode).toHaveBeenCalledWith("13800138888"));
    await waitFor(() => expect(screen.queryByTestId("login-sms-sent")?.textContent ?? "").toContain("138****8888"));

    fireEvent.change(screen.getByTestId("login-sms"), { target: { value: "123456" } });
    fireEvent.click(screen.getByTestId("login-submit"));
    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith(LOGIN));
    expect(service.phoneLogin).toHaveBeenCalledWith({ phone: "13800138888", smsCode: "123456" });
  });

  it("smsProviderUnavailableIsNotFakeSuccess：短信服务未配置时如实报错，不显示已发送", async () => {
    const service = makeService({
      requestLoginSmsCode: vi.fn().mockRejectedValue(apiError(409, "CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "操作冲突：短信服务未配置")),
    });
    renderShell(service);
    await waitFor(() => expect(screen.queryByTestId("login-sms-request")).not.toBeNull());
    fireEvent.change(screen.getByTestId("login-phone"), { target: { value: "13800138888" } });
    fireEvent.click(screen.getByTestId("login-sms-request"));
    await waitFor(() => expect(screen.queryByTestId("customer-auth-error")?.textContent ?? "").toContain("短信服务未配置"));
    expect(screen.queryByTestId("login-sms-sent")).toBeNull();
  });

  it("loginUnknownPhoneGoesToRegister：未注册手机号登录转注册并展示两份条款，不建会话", async () => {
    const service = makeService({
      phoneLogin: vi.fn().mockRejectedValue(apiError(409, "CUSTOMER_REGISTRATION_REQUIRED", "操作冲突：该手机号尚未注册，请完成注册")),
    });
    const onAuthenticated = renderShell(service);
    await goToRegister(service);
    await waitFor(() => expect(screen.queryByTestId("customer-register-view")?.textContent ?? "").toContain("会员条款正文"));
    expect(screen.getByTestId("customer-register-view").textContent).toContain("隐私条款正文");
    expect(onAuthenticated).not.toHaveBeenCalled();
  });

  it("registerRequiresBirthDateAndTermsConsent：生日与两份条款同意缺一不可，齐全才注册并回传当前条款版本", async () => {
    const service = makeService({
      phoneLogin: vi.fn().mockRejectedValue(apiError(409, "CUSTOMER_REGISTRATION_REQUIRED", "操作冲突：该手机号尚未注册，请完成注册")),
    });
    const onAuthenticated = renderShell(service);
    await goToRegister(service);

    fireEvent.click(screen.getByTestId("register-submit"));
    await waitFor(() => expect(screen.queryByText("请选择生日")).not.toBeNull());
    expect(service.register).not.toHaveBeenCalled();

    pickBirthDate("2000-01-01");
    fireEvent.click(screen.getByRole("checkbox", { name: /会员条款/ }));
    fireEvent.click(screen.getByTestId("register-submit"));
    await waitFor(() => expect(screen.queryByText("请阅读并同意隐私条款")).not.toBeNull());
    expect(service.register).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("checkbox", { name: /隐私条款/ }));
    fireEvent.click(screen.getByTestId("register-submit"));
    await waitFor(() => expect(service.register).toHaveBeenCalledWith({
      phone: "13800138888", smsCode: "123456", birthDate: "2000-01-01", memberTermsVersion: "mt-v1", privacyTermsVersion: "pp-v1",
    }));
    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith(LOGIN));
  });

  it("underAgeRegistrationFailsWithoutSession：未满 18 周岁注册失败，如实提示且不建会话", async () => {
    const service = makeService({
      phoneLogin: vi.fn().mockRejectedValue(apiError(409, "CUSTOMER_REGISTRATION_REQUIRED", "操作冲突：该手机号尚未注册，请完成注册")),
      register: vi.fn().mockRejectedValue(apiError(409, "CUSTOMER_UNDER_AGE", "操作冲突：未满 18 周岁不能注册会员")),
    });
    const onAuthenticated = renderShell(service);
    await goToRegister(service);
    pickBirthDate("2010-01-01");
    fireEvent.click(screen.getByRole("checkbox", { name: /会员条款/ }));
    fireEvent.click(screen.getByRole("checkbox", { name: /隐私条款/ }));
    fireEvent.click(screen.getByTestId("register-submit"));
    await waitFor(() => expect(screen.queryByTestId("customer-auth-error")?.textContent ?? "").toContain("未满 18 周岁不能注册会员"));
    expect(onAuthenticated).not.toHaveBeenCalled();
  });
});
