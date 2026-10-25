// 文件版本：V0.01
// 客人链接入口：微信「扫普通链接二维码打开小程序」把扫到的链接放在 q 参数里交给本页（规则须在小程序管理后台按正式客人链接域名配置，属外部资料）。
// 只认 core parseCustomerLink 的白名单路由与参数（配置的客人链接主机、https、/c/ 前缀，参数逐个校验），跳到对应页面；其余一律提示无法识别、不跳转。
// 白名单：停车出口码、酒吧专属购票入口、散台桌台码（扫码点单）、门票领取、券领取、订卡邀请函（四项码载荷）。
const config = require('../../config')
const core = require('../../core/index')

const enc = encodeURIComponent
const ROUTES = {
  parking: (p) => `/pages/parking/index?project_id=${enc(p.project_id)}${p.code ? `&code=${enc(p.code)}` : ''}`,
  bar: (p) => `/pages/buy/index?bar_id=${enc(p.bar_id)}`,
  table: (p) => `/pages/scan/index?table_id=${enc(p.table_id)}`,
  'ticket-claim': (p) => `/pages/transfer/index?claim_code=${enc(p.claim_code)}`,
  'voucher-claim': (p) => `/pages/vouchers/index?claim_code=${enc(p.claim_code)}`,
  invitation: (p) => `/pages/invitation/index?token=${enc(p.token)}&sig=${enc(p.sig)}&table_id=${enc(p.table_id)}&capacity=${enc(p.capacity)}`,
}

Component({
  data: { unknown: false },
  methods: {
    onLoad(query) {
      const raw = query && query.q ? decodeURIComponent(query.q) : ''
      const link = core.parseCustomerLink(raw, config.linkHost)
      const target = link && ROUTES[link.route]
      if (target) wx.redirectTo({ url: target(link.params) })
      else this.setData({ unknown: true })
    },
    toHome() {
      wx.switchTab({ url: '/pages/home/index' })
    },
  },
})
