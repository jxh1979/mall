// 文件版本：V0.01
// 官方购票（BSM「通过官方 App/小程序购票、预定、确认订单、查看支付结果」；BSM「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、
// 有效期、年龄限制与可使用范围」）：选票种 → 看我的价格与门票守则 → 下单 → 组合支付（储值＋在线补足，components/order-payment）→ 付清才显示支付成功。
// 价格由系统按本人资格自动取最低（BSM「多种资格价不叠加，选择最终价格最低的一项」），客人不选资格、不填金额；单店票带所属酒吧下单，
// 酒吧专属入口（客人链接 /c/bar?bar_id=）只拉该酒吧单店票并显示酒吧名；购票不拍人脸（BSM「购票和转赠均不要求录入人脸照」）。
// PGM「可以按全局、综合体……开关」：下单请求带所选票种所属的综合体（X-Project-Id），后端按综合体的终端开关拒绝。
const api = require('../../lib/api')
const { yuan } = require('../../lib/format')

function rulesOf(p) {
  return {
    myPrice: p.myPriceCent != null ? `我的价格 ${yuan(p.myPriceCent)}${p.myPriceBasis ? `（${p.myPriceBasis}）` : ''}，系统已按您的资格自动取最低价` : '',
    refund: p.refundable ? '按票种规则可申请退款，开始核销后不可退' : '售出后不退款',
    validity: p.validityDays != null ? `${p.validityDays} 天` : '以出票后票面为准',
    age: p.ageLimitText || '—',
    scope: p.usageScope || '—',
    entitlements: (p.entitlements || []).join('、'),
  }
}

Component({
  data: {
    loggedIn: false, lockedBarId: '', lockedBarName: '', products: [], labels: [], index: -1, rules: null,
    quantity: '1', visitDate: '', contactPhone: '', order: null, error: '', submitting: false,
  },
  methods: {
    onLoad(query) {
      this.setData({ lockedBarId: (query && query.bar_id) || '' })
    },
    onShow() {
      const loggedIn = api.hasSession()
      this.setData({ loggedIn })
      if (loggedIn && !this.data.products.length) this.load()
    },
    load() {
      const barId = this.data.lockedBarId
      api.service('ticketing').listTicketProducts(barId || undefined).then((rows) => {
        // 通票在前，单店票随后并带酒吧名（同名不同店可分）。
        const products = [...(rows || []).filter((p) => !p.barId), ...(rows || []).filter((p) => p.barId)]
        const locked = barId ? products.find((p) => p.barId === barId) : null
        this.setData({
          products,
          labels: products.map((p) => (p.barId ? `${p.ticketName} · ${p.barName || p.barId}` : `${p.ticketName}（综合体通票）`)),
          lockedBarName: barId ? (locked && locked.barName) || barId : '',
        })
      }).catch((err) => this.setData({ error: err.message || '票种加载失败' }))
    },
    toLogin() {
      api.toLogin()
    },
    onPick(e) {
      const index = Number(e.detail.value)
      const product = this.data.products[index]
      this.setData({ index, rules: product ? rulesOf(product) : null })
    },
    onQuantity(e) {
      this.setData({ quantity: e.detail.value })
    },
    onDate(e) {
      this.setData({ visitDate: e.detail.value })
    },
    onPhone(e) {
      this.setData({ contactPhone: e.detail.value })
    },
    async submit() {
      const product = this.data.products[this.data.index]
      const quantity = Number(this.data.quantity)
      if (!product) {
        this.setData({ error: '请选择票种' })
        return
      }
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 20) {
        this.setData({ error: '数量须为 1～20 张' })
        return
      }
      this.setData({ submitting: true, error: '', order: null })
      try {
        const order = await api.service('ticketing', product.projectId).createTicketOrder({
          ticketTypeId: product.ticketTypeId,
          quantity,
          visitDate: this.data.visitDate || undefined,
          contactPhone: this.data.contactPhone.trim() || undefined,
          // 单店票带其所属酒吧（服务端据票种校验）；通票不带。
          barId: product.barId || undefined,
        })
        this.setData({ order })
      } catch (err) {
        this.setData({ error: err.message || '下单失败' })
      } finally {
        this.setData({ submitting: false })
      }
    },
    onOrderChange(e) {
      this.setData({ order: e.detail.order })
    },
  },
})
