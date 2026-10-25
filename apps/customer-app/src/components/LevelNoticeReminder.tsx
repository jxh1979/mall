// 文件版本：V0.01
import { List, Modal } from "antd";
import dayjs from "dayjs";
import { useEffect, useState } from "react";

import type { CustomerService, LevelNotice } from "@complex/customer-core";

import { platform } from "../platform";

// 会员等级通知（OBS-13-01；BSM L820「保护期结束前……向会员发出降级预警，告知按发出预警当日的成长值测算将降至哪一等级」「降级预警只是提醒，
// 不是降级决定」「向其发出『已保级』通知」「在降级生效后立即补发通知」）：登录后（含恢复会话）与回到前台拉取本人未读的等级通知并弹出，
// 点「知道了」回写已读、之后不再弹；回写失败的下次回前台再弹。BSM L1596 客人短信清单不含等级通知，只在 App／小程序站内弹出；
// 应用关闭时的系统推送需推送凭证，未接入。
function day(iso: string | null): string {
  return iso ? dayjs(iso).format("YYYY年M月D日") : "";
}

function text(n: LevelNotice): string {
  if (n.noticeType === "downgrade_warning") {
    return `按今天的成长值测算，保护期结束（${day(n.cycleEnd)}）后将降至${n.targetLevelName}。这只是提醒，最终等级以保护期结束当日重新定级为准`;
  }
  if (n.noticeType === "level_kept") return `成长值已重新达到${n.currentLevelName}门槛，保护期结束不降级`;
  return `您的会员等级已于${day(n.createdAt)}调整为${n.targetLevelName}`;
}

export function LevelNoticeReminder({ service }: { service: CustomerService }): React.ReactElement | null {
  const [pending, setPending] = useState<LevelNotice[]>([]);

  useEffect(() => {
    const load = () => {
      service
        .listLevelNotices()
        .then((rows) => setPending(Array.isArray(rows) ? rows : []))
        .catch(() => undefined); // 拉取失败不打断客人当前操作，下次回前台再拉。
    };
    load();
    return platform.onResume(load);
  }, [service]);

  if (pending.length === 0) return null;
  const dismiss = () => {
    for (const row of pending) void service.markLevelNoticeRead(row.id).catch(() => undefined);
    setPending([]);
  };
  return (
    <Modal open title="会员等级通知" onOk={dismiss} onCancel={dismiss} okText="知道了" cancelButtonProps={{ style: { display: "none" } }}>
      <div data-testid="level-notice-reminder">
        <List size="small" dataSource={pending} renderItem={(row) => <List.Item key={row.id}>{text(row)}</List.Item>} />
      </div>
    </Modal>
  );
}
