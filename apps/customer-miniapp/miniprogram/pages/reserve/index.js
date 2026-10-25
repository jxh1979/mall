// 文件版本：V0.01
// 订台（BSM「官方 App 和小程序都可以订台，订台需要支付定金；订台页面须写明该卡座适用的最低消费金额与服务费比例」；BSM「客人自助订台必须支付定金，
// 定金直接进该卡座所属的酒吧商户……每一笔订台都必须填写最晚到店时间；超过最晚到店时间未到店的，系统自动释放该台位」）：
// 选台位（写明最低消费、服务费比例、可进人数与应付定金；已被预留的、本酒吧未配定金规则的不可选）→ 填到店时间、最晚到店时间与人数 → 下单 →
// 付定金（只拉起服务端签发的小程序微信支付参数，拉起后一律查单，渠道确认收款才算付成、才为客人预留台位）。
// 邀请函由酒吧员工确认订台后签发（第十三批裁决第 4 条「酒吧员工确认后签发」），在「我的订台」查看与转发。
// 订台表单写明定金条款（BSM「订卡时在订卡规则中写明」；BSM「定金不抵扣消费、不计入最低消费累计……按既有规则没收」），与 App 同一句。
// PGM「可以按全局、综合体……开关」：订台请求带所选台位所属的综合体（X-Project-Id），后端按综合体的终端开关拒绝。
const api = require('../../lib/api')
const pay = require('../../lib/pay')
const { yuan, localIso, TABLE_TYPES } = require('../../lib/format')

/** 不可自助订的原因：已被有效预留占着；应付定金为 0＝本酒吧未配定金规则、不开放自助订台（服务端 409 BAR_SELF_RESERVATION_UNAVAILABLE）。 */
function reasonOf(option) {
  if (option.reserved) return '已预留，暂不可订'
  if (!option.depositAmountCent) return '该酒吧暂未开放自助订台'
  return ''
}

function optionView(option) {
  const fee = typeof option.serviceFeeRateBps === 'number' ? `服务费 ${option.serviceFeeRateBps / 100}%` : '服务费比例未设置'
  return Object.assign({}, option, {
    typeText: TABLE_TYPES[option.tableType] || '台位',
    ruleText: `最低消费 ${yuan(option.minConsumptionCent)} · ${fee} · 可进 ${option.capacity} 人`,
    depositText: `定金 ${yuan(option.depositAmountCent || 0)}`,
    reason: reasonOf(option),
  })
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, loadError: '', options: [], picked: null,
    reserveDate: '', reserveTime: '', latestDate: '', latestTime: '', guests: '2', contactPhone: '', submitting: false, error: '',
    reservation: null, depositText: '', busy: false, unavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT,
    awaitingIntentId: '', payError: '', paid: false,
  },
  methods: {
    onShow() {
      const state = api.pageState('bar')
      this.setData(state)
      if (state.loggedIn && !state.paused && !this.loaded) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    load() {
      this.loaded = true
      api.service('bar').listBarReservationOptions()
        .then((rows) => this.setData({ options: (rows || []).map(optionView) }))
        .catch((err) => this.setData({ loadError: err.message || '可订台位加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    pick(e) {
      const option = this.data.options.find((o) => o.tableId === e.currentTarget.dataset.id)
      if (!option || option.reason) return
      this.setData({ picked: option, error: '' })
    },
    onReserveDate(e) {
      this.setData({ reserveDate: e.detail.value })
    },
    onReserveTime(e) {
      this.setData({ reserveTime: e.detail.value })
    },
    onLatestDate(e) {
      this.setData({ latestDate: e.detail.value })
    },
    onLatestTime(e) {
      this.setData({ latestTime: e.detail.value })
    },
    onGuests(e) {
      this.setData({ guests: e.detail.value })
    },
    onPhone(e) {
      this.setData({ contactPhone: e.detail.value })
    },
    async submit() {
      const { picked, reserveDate, reserveTime, latestDate, latestTime } = this.data
      const guests = Number(this.data.guests)
      const reservationTime = localIso(reserveDate, reserveTime)
      const latestArrivalAt = localIso(latestDate, latestTime)
      const missing = !reservationTime ? '请选择到店日期与时间'
        : !latestArrivalAt ? '请选择最晚到店时间（超过最晚到店时间未到店的，台位自动释放）'
          : !Number.isInteger(guests) || guests < 1 ? '请填写到店人数' : ''
      if (missing) {
        this.setData({ error: missing })
        return
      }
      this.setData({ submitting: true, error: '' })
      try {
        const reservation = await api.service('bar', picked.projectId).createBarReservation({
          barId: picked.barId, tableId: picked.tableId, tableType: picked.tableType, reservationTime, latestArrivalAt, guestCount: guests,
          contactPhone: this.data.contactPhone.trim() || undefined,
        })
        this.setData({ reservation, depositText: yuan(reservation.depositAmountCent) })
      } catch (err) {
        this.setData({ error: err.message || '订台失败' })
      } finally {
        this.setData({ submitting: false })
      }
    },
    confirmDeposit(intentId) {
      const id = this.data.reservation.id
      return api.service('bar').confirmDepositPayment(id, intentId).then((next) => next.depositStatus === 'paid')
    },
    show(result) {
      this.setData({
        busy: false, unavailable: result.state === 'unavailable', paid: result.state === 'paid',
        awaitingIntentId: result.state === 'awaiting' ? result.intentId : '', payError: result.error || '',
      })
    },
    async payDeposit() {
      this.setData({ busy: true, unavailable: false, awaitingIntentId: '', payError: '' })
      try {
        const id = this.data.reservation.id
        this.show(await pay.payAndConfirm(() => api.service('bar').startDepositPayment(id), (intentId) => this.confirmDeposit(intentId)))
      } catch (err) {
        this.setData({ busy: false, payError: err.message || '发起支付失败' })
      }
    },
    async recheck() {
      this.setData({ busy: true })
      this.show(await pay.recheck(this.data.awaitingIntentId, (intentId) => this.confirmDeposit(intentId)))
    },
  },
})
