// 文件版本：V0.01
import { LockOutlined, MobileOutlined, ShopOutlined } from "@ant-design/icons";
import { Alert, Button, Checkbox, DatePicker, Form, Input, Layout, Space, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useState } from "react";

import { ApiError, formatApiErrorDetail, type CustomerLoginResponse, type CustomerService, type CustomerTerms } from "@complex/customer-core";

const { Content } = Layout;
const { Paragraph, Text } = Typography;

type CustomerAuthShellProps = {
  service: Pick<CustomerService, "requestLoginSmsCode" | "phoneLogin" | "getTerms" | "register">;
  /** 外壳给的提示（如「登录已过期，请重新登录」）。 */
  notice?: string | null;
  /** 正在凭本端安全会话恢复登录。 */
  restoring?: boolean;
  onAuthenticated: (result: CustomerLoginResponse) => Promise<void> | void;
};

type Values = { phone: string; smsCode: string; birthDate?: Dayjs; agreeMember?: boolean; agreePrivacy?: boolean };

const mustAgree = (message: string) => ({
  validator: (_: unknown, checked?: boolean) => (checked ? Promise.resolve() : Promise.reject(new Error(message))),
});

// 客人登录与注册（BSM「登录可支持手机号短信验证码和微信；创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，
// 并同意当前会员条款和隐私条款。条件全部满足即自动创建有效会员，否则注册失败且不创建身份」；第十三批裁决第 3 条「满 18 周岁」）：
// 先取短信验证码再登录；未注册的手机号登录时服务端不建号（CUSTOMER_REGISTRATION_REQUIRED，验证码不被消费），转注册：填生日、阅读并逐份同意
// 当前两份条款，回传条款版本。年龄与条款版本由服务端判定，不满足即注册失败、不建会话。微信登录外部资料未到，未做。
export function CustomerAuthShell({ service, notice = null, restoring = false, onAuthenticated }: CustomerAuthShellProps): React.ReactElement {
  const [form] = Form.useForm<Values>();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [terms, setTerms] = useState<CustomerTerms | null>(null);
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState<string | null>(null);
  const [error, setError] = useState<{ message: string; detail: string | null } | null>(null);

  const fail = (err: unknown, fallback: string) =>
    setError({ message: err instanceof Error ? err.message : fallback, detail: formatApiErrorDetail(err) });

  const toRegister = async () => {
    setMode("register");
    if (terms) return;
    try {
      setTerms(await service.getTerms());
    } catch (err) {
      fail(err, "条款加载失败");
    }
  };

  const requestCode = async () => {
    try {
      await form.validateFields(["phone"]);
    } catch {
      return;
    }
    setBusy(true);
    setError(null);
    setSent(null);
    try {
      const receipt = await service.requestLoginSmsCode(form.getFieldValue("phone").trim());
      setSent(`验证码已发送至 ${receipt.phoneMasked}，5 分钟内有效`);
    } catch (err) {
      fail(err, "验证码发送失败");
    } finally {
      setBusy(false);
    }
  };

  const submit = async (values: Values) => {
    setBusy(true);
    setError(null);
    const phone = values.phone.trim();
    const smsCode = values.smsCode.trim();
    try {
      if (mode === "login") {
        await onAuthenticated(await service.phoneLogin({ phone, smsCode }));
      } else if (terms && values.birthDate) {
        await onAuthenticated(await service.register({
          phone,
          smsCode,
          birthDate: values.birthDate.format("YYYY-MM-DD"),
          memberTermsVersion: terms.memberTerms.version,
          privacyTermsVersion: terms.privacyPolicy.version,
        }));
      }
    } catch (err) {
      if (mode === "login" && err instanceof ApiError && err.detailCode === "CUSTOMER_REGISTRATION_REQUIRED") await toRegister();
      else fail(err, mode === "login" ? "登录失败" : "注册失败");
    } finally {
      setBusy(false);
    }
  };

  const registering = mode === "register";
  return (
    <Layout className="app-shell auth-shell" data-testid="customer-auth-shell">
      <Content className="auth-content">
        <div className="auth-panel" data-testid="customer-login-view">
          <h1 className="brand auth-brand" style={{ margin: 0 }}>
            <ShopOutlined />
            <span>{registering ? "注册会员" : "综合体客人端"}</span>
          </h1>
          {notice && !error ? <Alert className="status-alert" type="warning" showIcon message={notice} /> : null}
          {error ? (
            <Alert
              className="status-alert"
              type="error"
              data-testid="customer-auth-error"
              message={registering ? "注册失败" : "登录失败"}
              description={
                <>
                  <div>{error.message}</div>
                  {error.detail ? (
                    <details data-testid="customer-login-error-detail">
                      <summary>详情</summary>
                      <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-all", margin: 0 }}>{error.detail}</pre>
                    </details>
                  ) : null}
                </>
              }
            />
          ) : null}
          {registering ? (
            <Alert type="info" showIcon message="该手机号尚未注册：填写生日、阅读并同意会员条款与隐私条款后即可注册。须年满 18 周岁。" />
          ) : null}
          <Form form={form} layout="vertical" onFinish={submit} data-testid="customer-login-form">
            <Form.Item name="phone" label="手机号" rules={[{ required: true, whitespace: true, message: "请输入手机号" }]}>
              <Input data-testid="login-phone" prefix={<MobileOutlined />} type="tel" autoComplete="tel" />
            </Form.Item>
            <Form.Item label="短信验证码" required>
              <Space.Compact style={{ width: "100%" }}>
                <Form.Item name="smsCode" noStyle rules={[{ required: true, whitespace: true, message: "请输入验证码" }]}>
                  <Input data-testid="login-sms" prefix={<LockOutlined />} inputMode="numeric" autoComplete="one-time-code" aria-label="短信验证码" />
                </Form.Item>
                <Button onClick={() => void requestCode()} disabled={busy} data-testid="login-sms-request">获取验证码</Button>
              </Space.Compact>
            </Form.Item>
            {sent ? <Paragraph type="secondary" data-testid="login-sms-sent">{sent}</Paragraph> : null}
            {registering ? (
              <div data-testid="customer-register-view">
                <Form.Item name="birthDate" label="生日" rules={[{ required: true, message: "请选择生日" }]}>
                  <DatePicker style={{ width: "100%" }} format="YYYY-MM-DD" disabledDate={(day) => day.isAfter(dayjs(), "day")} />
                </Form.Item>
                {terms ? (
                  <>
                    <details>
                      <summary>会员条款</summary>
                      <Paragraph style={{ whiteSpace: "pre-wrap" }}>{terms.memberTerms.content}</Paragraph>
                    </details>
                    <details>
                      <summary>隐私条款</summary>
                      <Paragraph style={{ whiteSpace: "pre-wrap" }}>{terms.privacyPolicy.content}</Paragraph>
                    </details>
                  </>
                ) : (
                  <Text type="secondary">正在加载会员条款与隐私条款……</Text>
                )}
                <Form.Item name="agreeMember" valuePropName="checked" rules={[mustAgree("请阅读并同意会员条款")]}>
                  <Checkbox>我已阅读并同意会员条款</Checkbox>
                </Form.Item>
                <Form.Item name="agreePrivacy" valuePropName="checked" rules={[mustAgree("请阅读并同意隐私条款")]}>
                  <Checkbox>我已阅读并同意隐私条款</Checkbox>
                </Form.Item>
              </div>
            ) : null}
            <Button
              type="primary"
              htmlType="submit"
              loading={busy || restoring}
              disabled={registering && !terms}
              block
              data-testid={registering ? "register-submit" : "login-submit"}
            >
              {registering ? "注册并登录" : "登录"}
            </Button>
            <Button type="link" block onClick={() => (registering ? setMode("login") : void toRegister())} data-testid="auth-mode-switch">
              {registering ? "已有账号，返回登录" : "新用户注册"}
            </Button>
          </Form>
        </div>
      </Content>
    </Layout>
  );
}
