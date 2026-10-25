// 文件版本：V0.01
// 票与订单（BSM「查看我的订单、我的票、我的权益和使用记录；「我的票」显示补偿标注及对应人份或次份的实际补偿有效期」；
// BSM「第三方团购（美团/大众点评/抖音）购买的票按渠道来源在“我的订单/我的票”统一展示……渠道未接入前相关入口置灰、不展示伪造数据」）：
// - 我的订单：来源、支付与出票状态；未付清的订单在此继续组合支付（components/order-payment）；渠道未接入的入口置灰并注明。
// - 我的票＝本人当前持有的票（含转赠领取来的），按父票聚合，每人份出示动态码（components/dynamic-code）、看使用记录；补偿行单列标注与补偿有效期；
//   转赠待领取不冻结、照常出码（BSM「发起转赠后不冻结该权益」）。
// - 2026-10-25 操作员更正「即使注销，门票是不可以退的，除非碰上特殊安全情况」：客人端不设退票入口，安全例外与票种规则允许的退款由员工在后台办理。
const api = require('../../lib/api')
const core = require('../../core/index')
const { yuan, dateText, timeText } = require('../../lib/format')

function remainingOf(order) {
  return order.remainingCent != null ? order.remainingCent : Math.max(0, order.amountCent - (order.paidAmountCent || 0))
}

function groupTickets(tickets) {
  const groups = new Map()
  for (const t of tickets) {
    const key = t.parentTicketId || t.id
    groups.set(key, (groups.get(key) || []).concat(t))
  }
  return [...groups.entries()].map(([key, all]) => {
    // 完整补发的补偿行按人份单列，不并入原票「剩余 x/y 次」。
    const members = all.filter((m) => m.compensationKind !== 'full_reissue')
    const compensations = new Map()
    for (const c of all.filter((m) => m.compensationKind === 'full_reissue')) {
      const k = `${c.slotIndex || 1}|${c.validTo || ''}`
      compensations.set(k, (compensations.get(k) || []).concat(c))
    }
    const head = members[0] || all[0]
    const remaining = members.reduce((sum, m) => sum + m.remainingTimes, 0)
    const total = members.reduce((sum, m) => sum + m.totalTimes, 0)
    const button = (m, label) => ({ id: m.id, label, active: m.status === 'active', name: m.ticketName || '门票' })
    return {
      key,
      name: head.ticketName || '票',
      ticketNo: head.ticketNo || '',
      statusText: core.formatBusinessStatus(head.status),
      active: head.status === 'active',
      numPeople: head.numPeople,
      transferring: members.some((m) => m.transferState === 'transferring'),
      remainText: `剩余 ${remaining}/${total} 次${head.validTo ? ` · 有效期至 ${dateText(head.validTo)}` : ''}`,
      members: members.map((m, i) => button(m, members.length > 1 ? `第${m.slotIndex || i + 1}人 出示二维码` : '出示二维码')),
      compensations: [...compensations.entries()].map(([k, rows]) => ({
        key: k,
        label: `补偿（第${rows[0].slotIndex || 1}人）${rows[0].validTo ? ` · 补偿有效期至 ${dateText(rows[0].validTo)}` : ''}`,
        members: rows.map((c) => button(c, '出示二维码')),
      })),
    }
  })
}

function orderView(order) {
  return Object.assign({}, order, {
    channelText: core.formatBusinessStatus(order.salesChannel),
    payText: core.formatBusinessStatus(order.payStatus),
    issueText: core.formatBusinessStatus(order.issueStatus),
    summary: `${order.quantity} 张 · ${yuan(order.amountCent || 0)} · 游玩日 ${order.visitDate || '—'}`,
    payable: order.payStatus === 'unpaid' && order.status === 'created' && remainingOf(order) > 0,
  })
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, error: '', ticketsError: '', orders: [], shownOrders: [], channels: [], channelFilter: '',
    groups: [], showTransfer: true, qrTicketId: '', usages: null,
  },
  methods: {
    onShow() {
      const loggedIn = api.hasSession()
      this.setData({ loggedIn, paused: api.moduleHidden('ticketing'), showTransfer: !api.moduleHidden('transfer') })
      if (loggedIn && !this.data.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    load() {
      const service = api.service('ticketing')
      this.setData({ loading: true, error: '' })
      service.listOrders()
        .then((rows) => this.setOrders((rows || []).map(orderView)))
        .catch((err) => this.setData({ error: err.message || '订单加载失败' }))
        .then(() => this.setData({ loading: false }))
      // 渠道接入状态只用于显示入口：读不到渠道不列。
      service.listTicketChannels().then((rows) => this.setData({ channels: rows || [] })).catch(() => undefined)
      this.loadTickets()
    },
    loadTickets() {
      const service = api.service('ticketing')
      this.setData({ ticketsError: '' })
      return service.listMyTickets()
        .then((tickets) => {
          this.tickets = tickets || []
          this.setData({ groups: groupTickets(this.tickets) })
        })
        .catch((err) => this.setData({ ticketsError: err.message || '我的票加载失败' }))
    },
    setOrders(orders) {
      const filter = this.data.channelFilter
      this.setData({ orders, shownOrders: filter ? orders.filter((o) => o.salesChannel === filter) : orders })
    },
    pickChannel(e) {
      const channel = e.currentTarget.dataset.channel
      this.setData({ channelFilter: this.data.channelFilter === channel ? '' : channel })
      this.setOrders(this.data.orders)
    },
    onOrderChange(e) {
      const next = e.detail.order
      this.setOrders(this.data.orders.map((o) => (o.id === next.id ? orderView(Object.assign({}, o, next)) : o)))
      if (next.payStatus === 'paid') this.loadTickets()
    },
    openQr(e) {
      this.setData({ qrTicketId: e.currentTarget.dataset.id })
    },
    closeQr() {
      this.setData({ qrTicketId: '' })
    },
    async openUsages(e) {
      const { id, name } = e.currentTarget.dataset
      this.setData({ usages: { name, rows: null, error: '' } })
      try {
        const rows = (await api.service('ticketing').listTicketUsages(id)) || []
        this.setData({
          usages: {
            name,
            error: '',
            rows: rows.map((row) => ({ label: row.resultLabel, success: row.result === 'success', time: timeText(row.verifiedAt), merchant: row.merchantName || '' })),
          },
        })
      } catch (err) {
        this.setData({ usages: { name, rows: [], error: err.message || '使用记录加载失败' } })
      }
    },
    closeUsages() {
      this.setData({ usages: null })
    },
    openPage(e) {
      wx.navigateTo({ url: e.currentTarget.dataset.url })
    },
  },
})
