// 文件版本：V0.01
// 本人待付订单的组合支付（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」；BSM「通过官方 App/小程序购票、预定、
// 确认订单、查看支付结果」）：储值卡先扣（卡内余额不足待付时只扣卡内余额，不让客人填扣款金额），余下在线补足；在线付款金额由服务端定，
// 只拉起后端签发的小程序微信支付参数（lib/pay.js），未启用或未配置显示「支付暂不可用，未扣款」；拉起后一律向服务端查单，
// 付清与否以服务端订单视图为准，只有付清才由上层显示支付成功。每次变化以 change 事件把服务端订单视图交给上层。
const api = require('../../lib/api')
const pay = require('../../lib/pay')
const { yuan } = require('../../lib/format')

/** 待付＝订单视图的 remaining_cent；下单接口只回订单原字段时按订单金额减已付。 */
function remainingOf(order) {
  return order.remainingCent != null ? order.remainingCent : Math.max(0, order.amountCent - (order.paidAmountCent || 0))
}

function cardApplies(card, order) {
  return card.status === 'active' && card.balanceCent > 0
    && (card.projectId == null || card.projectId === order.projectId)
    && (card.merchantId == null || card.merchantId === order.barId)
}

Component({
  options: { addGlobalClass: true },
  properties: {
    order: { type: Object, value: null, observer() { this.refresh() } },
  },
  data: {
    summary: '', onlineLabel: '在线支付', busy: false, unavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT,
    awaitingIntentId: '', error: '',
    svOpen: false, cards: [], cardLabels: [], cardIndex: -1, svAmountText: '', phone: '', smsCode: '', svBusy: false, svError: '',
  },
  lifetimes: {
    attached() {
      this.refresh()
    },
  },
  methods: {
    refresh() {
      const order = this.data.order
      if (!order) return
      const remaining = remainingOf(order)
      this.setData({
        summary: `应付 ${yuan(order.amountCent)} · 已付 ${yuan(order.amountCent - remaining)} · 还需支付 ${yuan(remaining)}（全部付清才算完成）`,
        onlineLabel: order.amountCent > remaining ? '在线支付补足' : '在线支付',
      })
      this.updateAmount()
    },
    async confirm(paymentIntentId) {
      this.setData({ busy: true, error: '' })
      try {
        const next = await api.service('ticketing').confirmTicketPayment(this.data.order.id, paymentIntentId)
        const paid = next.payStatus === 'paid'
        this.setData({ awaitingIntentId: paid ? '' : paymentIntentId, error: paid ? '' : '尚未确认收款，请完成支付后再查' })
        this.triggerEvent('change', { order: next })
      } catch (err) {
        this.setData({ awaitingIntentId: paymentIntentId, error: err.message || '支付结果确认失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    async payOnline() {
      this.setData({ busy: true, error: '', unavailable: false, awaitingIntentId: '' })
      try {
        const view = await api.service('ticketing').startTicketPayment(this.data.order.id)
        if ((await pay.launchPayment(view)) === 'unavailable') {
          this.setData({ busy: false, unavailable: true })
          return
        }
        await this.confirm(view.paymentIntentId)
      } catch (err) {
        this.setData({ busy: false, error: err.message || '发起支付失败' })
      }
    },
    recheck() {
      this.confirm(this.data.awaitingIntentId)
    },
    async openStoredValue() {
      this.setData({ svOpen: true, cards: [], cardLabels: [], cardIndex: -1, svAmountText: '', phone: '', smsCode: '', svError: '', svBusy: true })
      try {
        const order = this.data.order
        const cards = ((await api.service('ticketing').getStoredValueAccounts()) || []).filter((card) => cardApplies(card, order))
        this.setData({
          cards,
          cardLabels: cards.map((card) => `${card.merchantId ? `商户卡（${card.merchantId}）` : '综合体通用卡'} · 余额 ${yuan(card.balanceCent)}`),
          cardIndex: cards.length ? 0 : -1,
          svError: cards.length ? '' : '没有适用于该订单的储值卡',
        })
        this.updateAmount()
      } catch (err) {
        this.setData({ svError: err.message || '储值卡加载失败' })
      } finally {
        this.setData({ svBusy: false })
      }
    },
    /** 本次从卡里扣多少：卡内余额不足待付时只扣卡内余额，其余在线补足；够付则付清待付。 */
    storedValueAmount() {
      const card = this.data.cards[this.data.cardIndex]
      return card ? Math.min(card.balanceCent, remainingOf(this.data.order)) : 0
    },
    updateAmount() {
      if (!this.data.svOpen || this.data.cardIndex < 0) return
      const amount = this.storedValueAmount()
      const rest = remainingOf(this.data.order) - amount
      this.setData({ svAmountText: `本次从储值卡扣 ${yuan(amount)}${rest > 0 ? `，余下 ${yuan(rest)} 请在线补足` : ''}` })
    },
    onCard(e) {
      this.setData({ cardIndex: Number(e.detail.value) })
      this.updateAmount()
    },
    onPhone(e) {
      this.setData({ phone: e.detail.value })
    },
    onCode(e) {
      this.setData({ smsCode: e.detail.value })
    },
    closeStoredValue() {
      if (!this.data.svBusy) this.setData({ svOpen: false })
    },
    async requestCode() {
      const phone = this.data.phone.trim()
      if (!phone) {
        this.setData({ svError: '请填写本人注册手机号' })
        return
      }
      this.setData({ svBusy: true, svError: '' })
      try {
        await api.service('ticketing').requestStoredValueCode(phone)
        wx.showToast({ title: '验证码已发送', icon: 'none' })
      } catch (err) {
        this.setData({ svError: err.message || '验证码发送失败' })
      } finally {
        this.setData({ svBusy: false })
      }
    },
    async payStoredValue() {
      const card = this.data.cards[this.data.cardIndex]
      const phone = this.data.phone.trim()
      const smsCode = this.data.smsCode.trim()
      if (!card || !phone || !smsCode) {
        this.setData({ svError: '请选择储值卡并填写手机号与短信验证码' })
        return
      }
      const amount = this.storedValueAmount()
      this.setData({ svBusy: true, svError: '' })
      try {
        const next = await api.service('ticketing').payTicketOrderWithStoredValue(this.data.order.id, Object.assign(
          { accountId: card.id, phone, smsCode },
          amount < remainingOf(this.data.order) ? { amountCent: amount } : {},
        ))
        this.setData({ svOpen: false })
        this.triggerEvent('change', { order: next })
      } catch (err) {
        this.setData({ svError: err.message || '余额支付失败' })
      } finally {
        this.setData({ svBusy: false })
      }
    },
  },
})
