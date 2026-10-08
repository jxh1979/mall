// 文件版本：V0.01
import { Alert } from "antd";
import React from "react";

import { onReplayedNotice } from "./operationKey";

/** M725-F03：重复提交返回第一次的结果时，在页面顶部提示已完成；下一次提交开始即撤下。 */
export function ReplayedNotice(): React.ReactElement | null {
  const [notice, setNotice] = React.useState<string | null>(null);
  React.useEffect(() => onReplayedNotice(setNotice), []);
  return notice ? (
    <Alert type="success" showIcon role="status" message={notice} data-testid="replayed-notice" style={{ marginBottom: 12 }} />
  ) : null;
}
