// 文件版本：V0.01
// 我的：登录状态、本人功能入口（积分与签到、储值与会员、券包、存酒、投诉与反馈、学生认证、账户与条款、注销账号；所属模块被终端开关关闭即不显示，
// PGM「关闭后菜单不可见或不可点」）与退出登录（先带令牌撤销服务端会话，再清本端令牌）。
const api = require('../../lib/api')

const ENTRIES = [
  { key: 'points', title: '积分与签到', module: 'points' },
  { key: 'wallet', title: '储值与会员', module: 'wallet' },
  { key: 'vouchers', title: '我的券包', module: 'vouchers' },
  { key: 'bottles', title: '我的存酒', module: 'saved_bottles' },
  { key: 'complaints', title: '投诉与反馈', module: 'complaints' },
  { key: 'student', title: '学生认证', module: 'account' },
  { key: 'account', title: '账户与条款', module: 'account' },
  { key: 'deregister', title: '注销账号', module: 'account' },
]

Component({
  data: { loggedIn: false, maskedPhone: '', entries: [] },
  methods: {
    onShow() {
      this.setData({
        loggedIn: api.hasSession(),
        maskedPhone: api.maskedPhone() || '',
        entries: ENTRIES.filter((entry) => !api.moduleHidden(entry.module)),
      })
    },
    openEntry(e) {
      wx.navigateTo({ url: `/pages/${e.currentTarget.dataset.key}/index` })
    },
    toLogin() {
      api.toLogin()
    },
    async logout() {
      await api.logout()
      wx.reLaunch({ url: '/pages/home/index' })
    },
  },
})
