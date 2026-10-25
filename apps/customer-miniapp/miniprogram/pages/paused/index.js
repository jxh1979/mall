// 文件版本：V0.01
// 本端暂停服务（PGM「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）：小程序总开关、本版本或开关拉取失败时由 app.js 转到本页；
// 回到前台再拉一次，恢复即回首页。
const api = require('../../lib/api')

Component({
  methods: {
    onShow() {
      api.loadSwitches().then((state) => {
        if (!state.paused) wx.switchTab({ url: '/pages/home/index' })
      })
    },
  },
})
