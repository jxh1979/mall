// 文件版本：V0.01
// 未核销票到期提醒（BSM「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认到期前二十四小时，
// 须同时配置上下限）」：提前量由服务端按项目参数判定，客户端不设关闭此类提醒的开关）：已登录时拉本人即将到期的未核销票并弹出。
// 回到前台（app.js）与本次运行中刚登录成功（登录页）都拉一次，与 App 同口径；点「知道了」的票本次运行内不再弹（只记在内存）。
const api = require('./api')
const { timeText } = require('./format')

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

module.exports = { remindExpiring }
