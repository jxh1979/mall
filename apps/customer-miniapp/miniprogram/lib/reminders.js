// 文件版本：V0.02
// 未核销票到期提醒（BSM「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认到期前二十四小时，
// 须同时配置上下限）」：提前量由服务端按项目参数判定，客户端不设关闭此类提醒的开关）：已登录时拉本人即将到期的未核销票并弹出。
// 回到前台（app.js）与本次运行中刚登录成功（登录页）都拉一次，与 App 同口径；点「知道了」的票本次运行内不再弹（只记在内存）。
const api = require('./api')
const { dateText, timeText } = require('./format')

const seen = new Set()

function remindExpiring() {
  if (!api.hasSession()) return
  api.service('ticketing').listTicketExpiryReminders().then((rows) => {
    const fresh = (rows || []).filter((row) => !seen.has(row.ticketId))
    if (fresh.length === 0) return
    fresh.forEach((row) => seen.add(row.ticketId))
    wx.showModal({
      title: '门票即将到期',
      content: fresh.map((row) => `${row.ticketName || '门票'} 将于 ${timeText(row.validTo)} 到期，请在到期前使用`).join('\n'),
      showCancel: false,
      confirmText: '知道了',
    })
  }).catch(() => undefined) // 提醒拉取失败不打断客人当前操作，下次回前台再拉。
}

// 会员等级通知（OBS-13-01；BSM L820「保护期结束前……向会员发出降级预警，告知按发出预警当日的成长值测算将降至哪一等级」「降级预警只是提醒，
// 不是降级决定」「向其发出『已保级』通知」「降级生效后立即补发通知」；BSM L1596 客人短信清单不含等级通知，只在站内弹出）：
// 已登录时拉本人未读的等级通知并弹出，点「知道了」回写已读、之后不再弹；回写失败的下次回前台再弹。与 App 同口径。
function levelNoticeText(n) {
  if (n.noticeType === 'downgrade_warning') {
    return `按今天的成长值测算，保护期结束（${dateText(n.cycleEnd)}）后将降至${n.targetLevelName}。这只是提醒，最终等级以保护期结束当日重新定级为准`
  }
  if (n.noticeType === 'level_kept') return `成长值已重新达到${n.currentLevelName}门槛，保护期结束不降级`
  return `您的会员等级已于${dateText(n.createdAt)}调整为${n.targetLevelName}`
}

function remindLevelNotices() {
  if (!api.hasSession()) return
  const service = api.service('points')
  service.listLevelNotices().then((rows) => {
    if (!Array.isArray(rows) || rows.length === 0) return
    wx.showModal({
      title: '会员等级通知',
      content: rows.map(levelNoticeText).join('\n'),
      showCancel: false,
      confirmText: '知道了',
      success: (res) => {
        if (res.confirm) rows.forEach((row) => service.markLevelNoticeRead(row.id).catch(() => undefined))
      },
    })
  }).catch(() => undefined) // 拉取失败不打断客人当前操作，下次回前台再拉。
}

module.exports = { remindExpiring, remindLevelNotices }
