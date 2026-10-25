// 文件版本：V0.01
// 储值与会员（BSM「我的余额：查看本人储值卡余额，含本金与赠送金分列。我的余额同时提供逐笔记录：每笔充值（时间、金额、本金与赠送金拆分、支付通路）、
// 每笔扣款（商户、扣减的本金与赠送金各多少）、每笔退款与回冲（含回冲顺序结果）；通用卡与商户级卡分列展示」）：
// - 商户储值卡退出公告（ENUM「可用到哪天、能否退、退多少比例、怎么退、找谁退」；第十三批裁决第 2 条「商户自己发，不卡」：公告由商户在商户门户自行发布，
//   本页只展示公示中的公告，本人持卡的注明）。
// - 充值前先读所选卡的扣款顺序、赠送金性质与退卡口径并展示（ENUM「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」），读不到不许充值；
//   小程序只能拉起微信支付（服务端签发的小程序参数，lib/pay.js），回来由服务端向渠道确认收款后入账才显示到账。
const api = require('../../lib/api')
const core = require('../../core/index')
const pay = require('../../lib/pay')
const { yuan, timeText } = require('../../lib/format')

const TXN_TYPES = { topup: '充值', consume: '消费', refund: '退款', adjust: '调整', consume_reversal: '退款回冲' }
const PAGE_SIZE = 20

const split = (principalCent, bonusCent) => `本金 ${yuan(principalCent || 0)} · 赠送金 ${yuan(bonusCent || 0)}`
const scopeOf = (card) => ({ projectId: card.projectId || undefined, merchantId: card.merchantId || undefined })

function cardView(card) {
  return Object.assign({}, card, {
    label: card.merchantId ? `商户卡（${card.merchantId}）` : '综合体通用卡',
    fundText: card.merchantId ? (card.fundOwnership === 'platform_collect' ? '平台代收' : '商户预收') : '',
    statusText: core.formatBusinessStatus(card.status),
    balanceText: `余额 ${yuan(card.balanceCent)}`,
    splitText: split(card.principalCent, card.bonusCent),
  })
}

function noticeView(n) {
  return Object.assign({}, n, {
    lines: [
      `可用至 ${n.useUntil}`,
      n.refundable ? `可退，退回比例 ${n.refundRatioBps / 100}%` : '不可退',
      `怎么退：${n.refundMethod || '见商户店面公告'}`,
      `找谁退：${n.refundContact || '见商户店面公告'}`,
      `公示至 ${n.publishUntil}`,
    ],
  })
}

function txnView(t) {
  const parts = [split(t.principalCent, t.bonusCent)]
  if (t.txnType === 'topup' && t.paymentChannel) parts.push(`支付通路：${core.formatBusinessStatus(t.paymentChannel)}`)
  if (t.txnType === 'consume') parts.push(`扣款商户：${t.merchantName || '综合体'}`)
  if (t.reason) parts.push(t.reason)
  return {
    id: t.id,
    title: `${TXN_TYPES[t.txnType] || '余额变动'} · ${yuan(t.amountCent)}`,
    detail: parts.join(' · '),
    after: `余额 ${yuan(t.balanceAfterCent)} · ${timeText(t.createdAt)}`,
  }
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, error: '', cards: [], selectedId: '', notices: [], rules: null, rulesError: false,
    amountText: '', busy: false, unavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT, awaitingIntentId: '', payError: '', topupDone: false,
    transactions: [], historyPage: 1, hasNext: false, historyError: '',
  },
  methods: {
    onShow() {
      const state = api.pageState('wallet')
      this.setData(state)
      if (state.loggedIn && !state.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    service() {
      return api.service('wallet')
    },
    load() {
      const s = this.service()
      this.setData({ loading: true, error: '' })
      s.listMerchantCardExitNotices().then((rows) => this.setData({ notices: (rows || []).map(noticeView) })).catch(() => this.setData({ notices: [] }))
      return s.getStoredValueAccounts().then((rows) => {
        const cards = (rows || []).map(cardView)
        const keep = cards.some((c) => c.id === this.data.selectedId)
        this.setData({ cards, selectedId: keep ? this.data.selectedId : (cards[0] ? cards[0].id : '') })
        this.afterSelect()
      }).catch((err) => this.setData({ error: err.message || '钱包加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    selected() {
      return this.data.cards.find((c) => c.id === this.data.selectedId)
    },
    pick(e) {
      this.setData({ selectedId: e.currentTarget.dataset.id, historyPage: 1, topupDone: false })
      this.afterSelect()
    },
    // 选卡即读该卡的充值规则（商户卡按发卡商户、通用卡按综合体）与逐笔记录。
    afterSelect() {
      const card = this.selected()
      this.setData({ rules: null, rulesError: false, transactions: [], historyError: '' })
      if (!card) return
      this.service().getStoredValueTopupRules(scopeOf(card))
        .then((rules) => this.setData({ rules }))
        .catch(() => this.setData({ rulesError: true }))
      this.loadHistory()
    },
    loadHistory() {
      return this.service().getStoredValueTransactions(this.data.selectedId, this.data.historyPage)
        .then((rows) => this.setData({ transactions: (rows || []).map(txnView), hasNext: (rows || []).length >= PAGE_SIZE }))
        .catch((err) => this.setData({ historyError: err.message || '交易记录加载失败' }))
    },
    prevPage() {
      if (this.data.historyPage <= 1) return
      this.setData({ historyPage: this.data.historyPage - 1 })
      this.loadHistory()
    },
    nextPage() {
      if (!this.data.hasNext) return
      this.setData({ historyPage: this.data.historyPage + 1 })
      this.loadHistory()
    },
    onAmount(e) {
      this.setData({ amountText: e.detail.value })
    },
    settle(intentId) {
      return this.service().settleStoredValueTopup(Object.assign({ paymentIntentId: intentId }, scopeOf(this.selected()))).then(() => true)
    },
    show(result) {
      this.setData({
        busy: false, unavailable: result.state === 'unavailable', awaitingIntentId: result.state === 'awaiting' ? result.intentId : '',
        payError: result.error || '', topupDone: result.state === 'paid',
      })
      if (result.state === 'paid') this.load()
    },
    async topup() {
      const card = this.selected()
      const amountCent = Math.round(Number(this.data.amountText) * 100)
      if (!card || !this.data.rules) {
        this.setData({ payError: '读到本卡充值规则后才能充值' })
        return
      }
      if (!Number.isFinite(amountCent) || amountCent <= 0) {
        this.setData({ payError: '请填写充值金额' })
        return
      }
      this.setData({ busy: true, unavailable: false, awaitingIntentId: '', payError: '', topupDone: false })
      try {
        this.show(await pay.payAndConfirm(async () => {
          const intent = await this.service().topupStoredValue(Object.assign(scopeOf(card), { amountCent, channel: 'wechat' }))
          return {
            paymentIntentId: String(intent.id || ''), businessOrderId: card.id, amountCent, channel: intent.channel || null,
            gatewayStatus: intent.gatewayStatus || null, paymentUrl: intent.paymentUrl || null, launch: intent.launch || null,
          }
        }, (intentId) => this.settle(intentId)))
      } catch (err) {
        this.setData({ busy: false, payError: err.message || '发起充值失败' })
      }
    },
    async recheck() {
      this.setData({ busy: true })
      this.show(await pay.recheck(this.data.awaitingIntentId, (intentId) => this.settle(intentId)))
    },
  },
})
