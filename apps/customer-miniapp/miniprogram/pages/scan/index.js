// 文件版本：V0.01
// 扫码点单与本桌账单（BSM「扫码点单只适用于散台，卡座不开放扫码点单。散台也须服务员先开台才能扫码点单，开台后客人扫描桌台二维码即可自助点单，下单时
// 即时付款、付款成功订单才成立，不挂账……付款成功后所点酒水计入该桌位当前账单，账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜」；
// ENUM「非会员可以浏览、点单和付款」；ENUM「沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏」）：
// 微信扫散台固定桌台码（或经客人链接入口带 table_id 进来），只认本综合体的桌台码链接（core parseCustomerLink 白名单），当前开台会话由服务端按桌台找，
// 不手填桌台或会话编号；不登录也可点单付款。下单即在线付款（只拉起服务端签发的小程序微信支付参数），回来一律查单，渠道确认收款才成单；
// 本桌当前账单只列该桌本次开台已成单的酒水。
const api = require('../../lib/api')
const config = require('../../config')
const core = require('../../core/index')
const pay = require('../../lib/pay')
const { yuan, param } = require('../../lib/format')

function scan() {
  return new Promise((resolve) => {
    wx.scanCode({ onlyFromCamera: true, scanType: ['qrCode'], success: (res) => resolve(res.result || ''), fail: () => resolve(null) })
  })
}

Component({
  data: {
    paused: false, tableId: '', menu: null, lines: [], totalText: '', bill: [], billTotal: '', error: '', busy: false, paid: false,
    unavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT, awaitingIntentId: '', payError: '',
  },
  methods: {
    onLoad(query) {
      this.setData({ tableId: param(query, 'table_id') })
    },
    onShow() {
      const { paused } = api.pageState('bar')
      this.setData({ paused })
      if (!paused && this.data.tableId && !this.data.menu) this.load()
    },
    service() {
      return api.service('bar')
    },
    load() {
      const tableId = this.data.tableId
      this.setData({ menu: null, lines: [], totalText: '', error: '' })
      this.service().listBarScanMenu(tableId)
        .then((rows) => this.setData({ menu: (rows || []).map((item) => Object.assign({}, item, { priceText: yuan(item.priceCent), count: 0 })) }))
        .catch((err) => this.setData({ error: err.message || '菜单加载失败' }))
      this.loadBill()
    },
    loadBill() {
      return this.service().getBarTableCurrentBill(this.data.tableId).then((items) => {
        const bill = (items || []).map((item) => Object.assign({}, item, { line: `${item.productName} ${yuan(item.unitPriceCent)} × ${item.quantity}` }))
        this.setData({ bill, billTotal: yuan(bill.reduce((sum, item) => sum + item.amountCent, 0)) })
      }).catch(() => this.setData({ bill: [] }))
    },
    async scanTable() {
      this.setData({ error: '' })
      const raw = await scan()
      if (raw === null) {
        this.setData({ error: '未扫到二维码，请对准桌上的点单二维码重试' })
        return
      }
      const link = core.parseCustomerLink(raw, config.linkHost)
      if (!link || link.route !== 'table') {
        this.setData({ error: '无法识别的二维码' })
        return
      }
      this.setData({ tableId: link.params.table_id, paid: false })
      this.load()
    },
    /** 各商品点的份数记在菜单行上（count）；沽清的不能加。 */
    setMenu(menu) {
      const lines = menu.filter((m) => m.count > 0).map((m) => ({ productId: m.productId, quantity: m.count, priceCent: m.priceCent }))
      this.setData({ menu, lines, totalText: yuan(lines.reduce((sum, line) => sum + line.priceCent * line.quantity, 0)) })
    },
    change(productId, delta) {
      const item = (this.data.menu || []).find((m) => m.productId === productId)
      if (!item || (delta > 0 && item.soldOut)) return
      this.setMenu(this.data.menu.map((m) => (m.productId === productId ? Object.assign({}, m, { count: Math.max(0, m.count + delta) }) : m)))
    },
    add(e) {
      this.change(e.currentTarget.dataset.id, 1)
    },
    minus(e) {
      this.change(e.currentTarget.dataset.id, -1)
    },
    confirmOrder() {
      return this.service().confirmBarScanOrder(this.data.tableId, this.scanOrderId).then((order) => order.status === 'paid')
    },
    show(result) {
      this.setData({
        busy: false, unavailable: result.state === 'unavailable', awaitingIntentId: result.state === 'awaiting' ? result.intentId : '',
        payError: result.error || '', paid: result.state === 'paid',
      })
      if (result.state === 'paid') {
        this.setMenu(this.data.menu.map((m) => Object.assign({}, m, { count: 0 })))
        this.loadBill()
      }
    },
    async pay() {
      const tableId = this.data.tableId
      const items = this.data.lines.map((line) => ({ productId: line.productId, quantity: line.quantity }))
      this.setData({ busy: true, paid: false, unavailable: false, awaitingIntentId: '', payError: '' })
      try {
        this.show(await pay.payAndConfirm(async () => {
          const order = await this.service().placeBarScanOrder(tableId, items)
          this.scanOrderId = order.scanOrderId
          return {
            paymentIntentId: order.paymentIntentId, businessOrderId: order.scanOrderId, amountCent: order.amountCent, channel: order.channel || null,
            gatewayStatus: order.gatewayStatus || null, paymentUrl: order.paymentUrl || null, launch: order.launch || null,
          }
        }, () => this.confirmOrder()))
      } catch (err) {
        this.setData({ busy: false, payError: err.message || '下单失败' })
      }
    },
    async recheck() {
      this.setData({ busy: true })
      this.show(await pay.recheck(this.data.awaitingIntentId, () => this.confirmOrder()))
    },
  },
})
