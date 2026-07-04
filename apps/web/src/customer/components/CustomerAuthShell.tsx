import { LockOutlined, MobileOutlined, ShopOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, Layout } from "antd";

const { Content } = Layout;

type CustomerAuthShellProps = {
  error: string | null;
  /** 技术详情（接口 path/status/request_id/原文），默认折叠，供报障。 */
  errorDetail?: string | null;
  loading: boolean;
  onLogin: (values: { phone: string; smsCode: string }) => Promise<void>;
};

// 客人端登录壳：手机号 + 短信验证码换取客人 Token（POST /customer/auth/phone-login）
export function CustomerAuthShell({ error, errorDetail, loading, onLogin }: CustomerAuthShellProps): React.ReactElement {
  const [form] = Form.useForm();
  return (
    <Layout className="app-shell auth-shell" data-testid="customer-auth-shell">
      <Content className="auth-content">
        <div className="auth-panel" data-testid="customer-login-view">
          <h1 className="brand auth-brand" style={{ margin: 0 }}>
            <ShopOutlined />
            <span>综合体客人端</span>
          </h1>
          {error ? (
            <Alert
              className="status-alert"
              type="error"
              message="登录失败"
              description={
                <>
                  <div>{error}</div>
                  {errorDetail ? (
                    <details data-testid="customer-login-error-detail">
                      <summary>详情</summary>
                      <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-all", margin: 0 }}>{errorDetail}</pre>
                    </details>
                  ) : null}
                </>
              }
            />
          ) : null}
          <Form form={form} layout="vertical" onFinish={onLogin} data-testid="customer-login-form">
            <Form.Item name="phone" label="手机号" rules={[{ required: true, message: "请输入手机号" }]}>
              <Input data-testid="login-phone" prefix={<MobileOutlined />} type="tel" autoComplete="tel" />
            </Form.Item>
            <Form.Item name="smsCode" label="短信验证码" rules={[{ required: true, message: "请输入验证码" }]}>
              <Input data-testid="login-sms" prefix={<LockOutlined />} inputMode="numeric" autoComplete="one-time-code" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block data-testid="login-submit">
              登录
            </Button>
          </Form>
        </div>
      </Content>
    </Layout>
  );
}
