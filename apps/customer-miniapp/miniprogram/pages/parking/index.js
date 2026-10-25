// 文件版本：V0.01
// 停车缴费（BSM「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场……不做车牌绑定无感支付」；BSM「无牌车在入口扫码领取临时码、
// 出口凭码缴费」；BSM「连接客人端，客人在官方App/小程序缴费并查看消费抵免」）：出口码链接（/c/parking?project_id=&code=）经客人链接入口打开本页，
// 按车牌或临时码查费、付款、看离场宽限截止；显示本日消费抵免，抵免把应缴抵到 0 时由客人确认使用抵免离场（记 0 元已缴）。
// 付款只拉起后端签发的小程序微信支付参数（lib/pay.js），拉起参数为空即「支付暂不可用，未扣款」；回来一律查单，渠道确认收款才显示已缴。
// 从首页入口进入（不带项目）时按综合体列表定停车场所在综合体：只有一个就直接用，多个由客人选，客人端已暂停服务的综合体不列；
// 链接带的综合体被关只提示暂停服务（PGM「关闭后菜单不可见或不可点」）。请求带所用综合体（X-Project-Id，后端按综合体拒绝）。
const api = require('../../lib/api')
const pay = require('../../lib/pay')
const { yuan, timeText } = require('../../lib/format')

const CREDIT_LABELS = { available: '可用', used: '已使用', revoked: '已撤销（消费退款后未达门槛）', not_reached: '未达门槛' }

function quoteView(quote) {
  const gross = quote.grossFeeCent != null ? quote.grossFeeCent : quote.feeCent
  return Object.assign({}, quote, {
    vehicle: quote.plateNo || `临时码 ${quote.tempCode || '—'}`,
    enteredText: timeText(quote.enteredAt),
    grossText: yuan(gross),
    // 只在实际抵掉小时数大于 0 时显示抵免一行；金额＝停车费−max(抵后应收, 已缴)（与 App 同口径）。
    offsetText: (quote.creditOffsetHours || 0) > 0 ? `消费抵免（${quote.creditMinutes || 0} 分钟）−${yuan(gross - Math.max(quote.feeCent, quote.paidCent))}` : '',
    paidText: yuan(quote.paidCent),
    dueText: yuan(quote.dueCent),
    payLabel: quote.dueCent > 0 ? '去缴费' : '使用抵免离场',
    payable: quote.dueCent > 0 || Boolean(quote.creditId),
  })
}

Component({
  data: {
    loggedIn: false, paused: false, projectId: '', projects: [], projectNames: [], projectIndex: -1, code: '', credit: '', quote: null,
    payment: null, paidText: '', busy: false, error: '', unavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT, awaiting: false,
  },
  methods: {
    onLoad(query) {
      this.setData({ projectId: (query && query.project_id) || '', code: (query && query.code) || '' })
    },
    onShow() {
      const loggedIn = api.hasSession()
      const paused = Boolean(this.data.projectId) && !api.projectOpen(this.data.projectId)
      this.setData({ loggedIn, paused })
      if (!loggedIn || paused) return
      if (!this.data.projectId && !this.data.projects.length) {
        api.service('parking').listProjects()
          .then((rows) => {
            const projects = (rows || []).filter((p) => api.projectOpen(p.projectId))
            this.setData({ projects, projectNames: projects.map((p) => p.projectName) })
            if (projects.length === 1) this.useProject(projects[0].projectId)
          })
          .catch((err) => this.setData({ error: err.message || '综合体加载失败' }))
        return
      }
      this.loadCredit()
    },
    onProject(e) {
      const index = Number(e.detail.value)
      this.setData({ projectIndex: index })
      this.useProject(this.data.projects[index].projectId)
    },
    useProject(projectId) {
      this.setData({ projectId, credit: '', quote: null, payment: null, paidText: '' })
      this.loadCredit()
    },
    parking() {
      return api.service('parking', this.data.projectId)
    },
    loadCredit() {
      if (!this.data.projectId) return
      this.parking().getParkingCredit(this.data.projectId).then((credit) => this.setData({
        credit: `今日消费抵免：${CREDIT_LABELS[credit.status] || credit.status}（酒吧与商户有效消费已累计 ${yuan(credit.eligibleAmountCent)}，满 ${yuan(credit.thresholdCent)} 可抵 ${credit.creditMinutes} 分钟停车，每日一次）`,
      })).catch(() => this.setData({ credit: '' }))
    },
    toLogin() {
      api.toLogin()
    },
    onCode(e) {
      this.setData({ code: e.detail.value })
    },
    async run(action) {
      this.setData({ busy: true, error: '' })
      try {
        await action()
      } catch (err) {
        this.setData({ error: err.message || '操作失败，请稍后再试' })
      } finally {
        this.setData({ busy: false })
      }
    },
    lookup() {
      const code = this.data.code.trim()
      if (!code) return
      this.run(async () => {
        this.setData({ payment: null, paidText: '', unavailable: false, awaiting: false })
        this.setData({ quote: quoteView(await this.parking().findParkingSession(this.data.projectId, code)) })
      })
    },
    showPayment(payment) {
      this.setData({
        payment,
        awaiting: payment.status === 'pending',
        paidText: payment.status === 'paid' ? `已缴费 ${yuan(payment.amountCent)}，请在 ${timeText(payment.graceUntil)} 前离场；超过离场宽限时限须补缴。` : '',
      })
    },
    pay() {
      this.run(async () => {
        const service = this.parking()
        const payment = await service.createParkingPayment(this.data.quote.parkingSessionId)
        this.showPayment(payment)
        if (payment.status !== 'pending') return
        const result = await pay.launchPayment({
          paymentIntentId: payment.id, businessOrderId: payment.parkingSessionId || '', amountCent: payment.amountCent, channel: null,
          gatewayStatus: null, paymentUrl: payment.paymentUrl || null, launch: payment.launch || null,
        })
        if (result === 'unavailable') {
          this.setData({ unavailable: true, awaiting: false })
          return
        }
        this.showPayment(await this.parking().confirmParkingPayment(payment.id))
      })
    },
    confirm() {
      this.run(async () => this.showPayment(await this.parking().confirmParkingPayment(this.data.payment.id)))
    },
  },
})
