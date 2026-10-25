// 文件版本：V0.02
// 小程序入口：启动凭本地令牌恢复会话；启动与每次回到前台拉终端开关（本端或本版本被关、只有一个综合体且它被关、拉取失败 → 本端暂停服务页）；
// 已登录时拉本人即将到期的未核销票并弹出到期提醒、拉本人未读的会员等级通知并弹出（lib/reminders.js；本次运行中刚登录的由登录页拉）。
// 小程序关闭时的推送需订阅消息模板（外部资料），未接入。
const api = require('./lib/api')
const { remindExpiring, remindLevelNotices } = require('./lib/reminders')

App({
  onLaunch() {
    api.restoreSession()
  },
  onShow() {
    api.loadSwitches().then((state) => {
      if (state.paused) wx.reLaunch({ url: '/pages/paused/index' })
    })
    remindExpiring()
    remindLevelNotices()
  },
})
