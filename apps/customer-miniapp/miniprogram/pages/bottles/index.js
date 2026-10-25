// 文件版本：V0.01
// 我的存酒与取酒码（BSM「我的存酒：查看本人名下全部存酒、到期日与动态取酒码。存酒只能在存入的那家酒吧取用，不提供跨店取酒或跨店转移库存；客人端「我的存酒」
// 为跨店汇总展示，每条记录标明所属酒吧，取酒须到该酒吧办理。门店退出后仍在保管有效期内的存酒……由所属娱乐平台公司接收，并在指定地点领取」）：
// - 取酒码是按全场统一间隔刷新的动态码（components/dynamic-code，kind=bottle），只画进二维码、不以文字出现。
// - 转存（ENUM「转存须客人本人短信验证码确认」）：先给本人注册手机号发码，填码确认后从当前到期日顺延；次数上限与只能在到期日前办理由服务端判定。
const api = require('../../lib/api')
const { dateText } = require('../../lib/format')

function bottleView(b) {
  return Object.assign({}, b, {
    title: b.barName || '酒吧',
    line: `${b.bottleName} · 剩余 ${b.remainingQuantity} ${b.unit}`,
    expireText: `到期 ${b.expireAt ? dateText(b.expireAt) : '—'}`,
    where: b.handedOverToPlatform ? `门店已退出，请到${b.pickupLocation || '平台公司指定地点'}领取` : `取酒须到${b.barName || '存入的酒吧'}办理`,
  })
}

Component({
  data: { loggedIn: false, paused: false, loading: true, error: '', bottles: [], showingId: '', renewing: null, smsCode: '', sent: false, busy: false, renewError: '', renewResult: '' },
  methods: {
    onShow() {
      const state = api.pageState('saved_bottles')
      this.setData(state)
      if (state.loggedIn && !state.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    service() {
      return api.service('saved_bottles')
    },
    load() {
      this.setData({ loading: true, error: '' })
      return this.service().listMySavedBottles()
        .then((rows) => this.setData({ bottles: (rows || []).map(bottleView) }))
        .catch((err) => this.setData({ error: err.message || '我的存酒加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    toggleCode(e) {
      const id = e.currentTarget.dataset.id
      this.setData({ showingId: this.data.showingId === id ? '' : id })
    },
    openRenew(e) {
      const bottle = this.data.bottles.find((b) => b.id === e.currentTarget.dataset.id)
      this.setData({ renewing: bottle || null, smsCode: '', sent: false, renewError: '', renewResult: '' })
    },
    closeRenew() {
      if (!this.data.busy) this.setData({ renewing: null })
    },
    onSms(e) {
      this.setData({ smsCode: e.detail.value })
    },
    async run(action) {
      this.setData({ busy: true, renewError: '' })
      try {
        await action()
      } catch (err) {
        this.setData({ renewError: err.message || '操作失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    sendCode() {
      this.run(async () => {
        await this.service().requestSavedBottleRenewalCode(this.data.renewing.id)
        this.setData({ sent: true })
      })
    },
    confirmRenew() {
      const smsCode = this.data.smsCode.trim()
      if (!smsCode) {
        this.setData({ renewError: '请填写短信验证码' })
        return
      }
      this.run(async () => {
        const result = await this.service().renewSavedBottle(this.data.renewing.id, smsCode)
        this.setData({ renewResult: `已转存，新到期日 ${dateText(result.newExpireAt)}` })
        await this.load()
      })
    },
  },
})
