// 文件版本：V0.01
import { Alert, Card, Space, Spin, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService, CustomerTerms } from "@complex/customer-core";

const { Paragraph, Text } = Typography;

// 账户与条款（ENUM「会员注册……同意当前会员条款和隐私条款后，系统自动注册成功」）：显示登录手机号（掩码），可随时查看当前生效的会员条款与隐私条款
// 及版本。证件采集等敏感信息的单独同意在对应页面使用前取得（BSM「采集证件……前先单独取得同意」，见学生认证）。
export function AccountPage({ service, maskedPhone }: { service: CustomerService; maskedPhone: string }): React.ReactElement {
  const [terms, setTerms] = useState<CustomerTerms | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    service.getTerms().then(setTerms).catch((err) => setError(err instanceof Error ? err.message : "条款加载失败"));
  }, [service]);

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      <Card size="small" title="账户">
        <Text data-testid="account-phone">登录手机号：{maskedPhone}</Text>
      </Card>
      <Card size="small" title="会员条款与隐私条款" data-testid="account-terms">
        {error ? <Alert type="error" showIcon message={error} /> : !terms ? <Spin /> : (
          <Space direction="vertical" style={{ width: "100%" }}>
            <details>
              <summary data-testid="account-terms-member">会员条款（版本 {terms.memberTerms.version}）</summary>
              <Paragraph style={{ whiteSpace: "pre-wrap", marginTop: 8 }}>{terms.memberTerms.content}</Paragraph>
            </details>
            <details>
              <summary data-testid="account-terms-privacy">隐私条款（版本 {terms.privacyPolicy.version}）</summary>
              <Paragraph style={{ whiteSpace: "pre-wrap", marginTop: 8 }}>{terms.privacyPolicy.content}</Paragraph>
            </details>
          </Space>
        )}
      </Card>
    </Space>
  );
}
