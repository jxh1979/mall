// 文件版本：V0.01
// 动态二维码（BSM「凡是出示出来给人验证的二维码……都不是一张固定的图……有效期（刷新间隔）由后台统一设置、以 5 秒为单位可调、最长 60 秒；
// 截图、转发出去的旧码过了有效期就作废」；BSM「二维码原始值不存在手机本地，只在线核验」）：显示时长、到点换码、取码失败隐藏都用
// core 的 createDynamicCode（只认服务端给的到期信息）；所在页面切到后台（含小程序进后台）即隐藏、停计时，回到前台先取新码再显示；
// 码值只在内存里画成图，不写存储、不以文字出现、不开长按识别与保存。
// kind 决定取哪种码：门票验票码、动态取酒码（存酒编号）、本人动态入场码（订台编号）、券动态核销码（券编号）。
const api = require('../../lib/api')
const core = require('../../core/index')
const { qrImage } = require('../../lib/format')

const KINDS = {
  ticket: { module: 'ticketing', fetch: (service, id) => service.getTicketQr(id), value: (code) => code.credentialToken },
  bottle: { module: 'saved_bottles', fetch: (service, id) => service.getSavedBottlePickupCode(id), value: (code) => code.code },
  entry: { module: 'bar', fetch: (service, id) => service.getBarInvitationEntryCode(id), value: (code) => code.code },
  voucher: { module: 'vouchers', fetch: (service, id) => service.getVoucherRedeemCode(id), value: (code) => code.code },
}

// 计时交给运行环境的定时器（不依赖 globalThis）。
const timers = {
  setTimeout: (handler, ms) => setTimeout(handler, ms),
  clearTimeout: (handle) => clearTimeout(handle),
  setInterval: (handler, ms) => setInterval(handler, ms),
  clearInterval: (handle) => clearInterval(handle),
}

Component({
  options: { addGlobalClass: true },
  properties: {
    kind: { type: String, value: 'ticket' },
    targetId: { type: String, value: '' },
    title: { type: String, value: '二维码' },
  },
  data: { status: 'loading', src: '', secondsLeft: 0, message: '' },
  lifetimes: {
    attached() {
      const kind = KINDS[this.data.kind]
      let shown = null
      this.controller = core.createDynamicCode(
        () => kind.fetch(api.service(kind.module), this.data.targetId),
        (state) => {
          if (state.status === 'shown') {
            const patch = { status: 'shown', secondsLeft: state.secondsLeft, message: '' }
            if (state.code !== shown) {
              shown = state.code
              patch.src = qrImage(kind.value(state.code))
            }
            this.setData(patch)
            return
          }
          shown = null
          this.setData({ status: state.status === 'loading' ? 'loading' : state.reason, src: '', message: state.message || '' })
        },
        timers,
      )
      this.controller.start()
    },
    detached() {
      this.controller.stop()
    },
  },
  pageLifetimes: {
    hide() {
      this.controller.pause()
    },
    show() {
      this.controller.start()
    },
  },
  methods: {
    retry() {
      this.controller.start()
    },
  },
})
