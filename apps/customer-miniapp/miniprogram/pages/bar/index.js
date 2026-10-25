// 文件版本：V0.01
// 酒吧（标签页）：订台、我的订台、接受邀请、扫码点单入口（BSM「官方 App 和小程序都可以订台」；BSM「订卡客人可在 App/小程序查看自己的订卡邀请函」；
// BSM「支持客人扫码点单」）。不登录也可进扫码点单（ENUM「非会员可以浏览、点单和付款」）；酒吧模块被终端开关关闭即只提示暂停服务、入口不显示。
const api = require('../../lib/api')

const ENTRIES = [
  { key: 'reserve', title: '订台', url: '/pages/reserve/index' },
  { key: 'reservations', title: '我的订台', url: '/pages/reservations/index' },
  { key: 'invitation', title: '接受邀请', url: '/pages/invitation/index' },
  { key: 'scan', title: '扫码点单', url: '/pages/scan/index' },
]

Component({
  data: { paused: false, entries: ENTRIES },
  methods: {
    onShow() {
      this.setData({ paused: api.moduleHidden('bar') })
    },
    openEntry(e) {
      wx.navigateTo({ url: e.currentTarget.dataset.url })
    },
  },
})
