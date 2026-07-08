// ID 展示统一助手(对抗审计 C:消除「有的截断有的显全串」不一致)。
// 长 id 只显尾段(足以辨识),完整值放 title 悬停可见;配 .admin-id-cell 等宽。
export function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}
