import { LockOutlined, MobileOutlined, ShopOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, Layout } from "antd";

const { Content } = Layout;

type CustomerAuthShellProps = {
  error: string | null;
  loading: boolean;
  onLogin: (values: { phone: string; smsCode: string }) => Promise<void>;
};

// 客人端登录壳：手机号 + 短信验证码换取客人 Token（POST /customer/auth/phone-login）
export function CustomerAuthShell({ error, loading, onLogin }: CustomerAuthShellProps): React.ReactElement {
  const [form] = Form.useForm();
  return (
    <Layout className="app-shell auth-shell" data-testid="customer-auth-shell">
      <Content className="auth-content">
        <div className="auth-panel" data-testid="customer-login-view">
          <div className="brand auth-brand">
            <ShopOutlined />
            <span>综合体客人端</span>
          </div>
          {error ? <Alert className="status-alert" type="error" message="登录失败" description={error} /> : null}
          <Form form={form} layout="vertical" onFinish={onLogin} data-testid="customer-login-form">
            <Form.Item name="phone" label="手机号" rules={[{ required: true, message: "请输入手机号" }]}>
              <Input data-testid="login-phone" prefix={<MobileOutlined />} autoComplete="tel" />
            </Form.Item>
            <Form.Item name="smsCode" label="短信验证码" rules={[{ required: true, message: "请输入验证码" }]}>
              <Input data-testid="login-sms" prefix={<LockOutlined />} />
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
