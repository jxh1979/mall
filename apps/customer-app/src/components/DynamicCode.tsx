// 文件版本：V0.01
import { Alert, Button, Card, Typography } from "antd";
import { QRCodeSVG } from "qrcode.react";
import { useEffect, useRef, useState } from "react";

import { createDynamicCode, type DynamicCodeController, type DynamicCodeState, type DynamicCodeWindow } from "@complex/customer-core";

import { platform } from "../platform";

const { Paragraph, Text } = Typography;

type DynamicCodeProps<T extends DynamicCodeWindow> = {
  title: string;
  fetchCode: () => Promise<T>;
  /** 二维码内容（只画进码图，不以文字出现在页面上）。 */
  valueOf: (code: T) => string;
  testId?: string;
};

// 门票验票码、动态取酒码、被邀请人入场码、券核销码共用（BSM「凡是出示出来给人验证的二维码……都不是一张固定的图」）：
// 按服务端到期时刻显示，到点即隐藏并取新码；取码失败、切到后台即隐藏；回前台先取新码再显示。
// 码值只在内存里：不写本地存储、不提供复制。
export function DynamicCode<T extends DynamicCodeWindow>({ title, fetchCode, valueOf, testId = "dynamic-code" }: DynamicCodeProps<T>): React.ReactElement {
  const [state, setState] = useState<DynamicCodeState<T>>({ status: "loading" });
  const fetchRef = useRef(fetchCode);
  fetchRef.current = fetchCode;
  const controllerRef = useRef<DynamicCodeController | null>(null);

  useEffect(() => {
    const controller = createDynamicCode<T>(() => fetchRef.current(), setState);
    controllerRef.current = controller;
    controller.start();
    const offPause = platform.onPause(() => controller.pause());
    const offResume = platform.onResume(() => controller.start());
    return () => {
      offPause();
      offResume();
      controller.stop();
    };
  }, []);

  return (
    <Card title={title} data-testid={testId}>
      {state.status === "shown" ? (
        <>
          <div data-testid="qr-code" style={{ display: "flex", justifyContent: "center", padding: 12 }}>
            <QRCodeSVG value={valueOf(state.code)} size={220} level="M" marginSize={4} title={title} role="img" />
          </div>
          <Paragraph type="secondary" data-testid="qr-countdown">
            {state.secondsLeft} 秒后失效，到点自动换新码；截图无效，现场须在线核验。
          </Paragraph>
        </>
      ) : state.status === "loading" ? (
        <Text type="secondary">正在获取二维码……</Text>
      ) : state.reason === "failed" ? (
        <>
          <Alert type="warning" showIcon data-testid="qr-hidden" message="二维码已隐藏，请联网后重试" description={state.message} />
          <Button style={{ marginTop: 12 }} onClick={() => controllerRef.current?.start()} data-testid="qr-manual-refresh">
            重新获取
          </Button>
        </>
      ) : (
        <Text type="secondary" data-testid="qr-hidden">二维码已隐藏，回到本页后自动换新码。</Text>
      )}
    </Card>
  );
}
