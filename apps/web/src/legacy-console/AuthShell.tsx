import { BankOutlined, LockOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, Layout } from "antd";
import type { FormInstance } from "antd";

const { Content } = Layout;

type AuthShellProps = {
  error: string | null;
  form: FormInstance;
  loading: boolean;
  onLogin: (values: Record<string, string>) => Promise<void>;
};

export function AuthShell({ error, form, loading, onLogin }: AuthShellProps) {
  return (
    <Layout className="app-shell auth-shell" data-testid="auth-shell">
      <Content className="auth-content">
        <div className="auth-panel" data-testid="login-view">
          <div className="brand auth-brand">
            <BankOutlined />
            <span>综合体管理系统</span>
          </div>
          <Alert
            className="status-alert"
            data-testid="legacy-retired-banner"
            type="warning"
            showIcon
            message="该控制台已退役"
            description="已迁移至管理后台，写动作请在管理后台/现场端执行。"
            action={(
              <Button size="small" type="primary" data-testid="legacy-go-admin" onClick={() => window.location.assign("/admin")}>
                前往管理后台
              </Button>
            )}
          />
          {error ? (
            <Alert
              className="status-alert"
              data-testid="global-error"
              type="error"
              message="登录失败"
              description={error}
            />
          ) : null}
          <Form form={form} layout="vertical" onFinish={onLogin} data-testid="login-form">
            <Form.Item name="username" label="账号" rules={[{ required: true }]}>
              <Input data-testid="login-username" prefix={<LockOutlined />} autoComplete="username" />
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{ required: true }]}>
              <Input.Password data-testid="login-password" autoComplete="current-password" />
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
