// 文件版本：V0.01
// 我的订台与邀请函（BSM「订卡客人可在 App/小程序查看自己的订卡邀请函二维码（注明酒吧、卡座、可进人数），转发给朋友……作为开卡人，可以在 App/小程序上
// 看到已接受邀请的名单与各自是否已入场，并可把尚未入场的被邀请人移出邀请函、收回其名额」；BSM「邀请函二维码只用于转发与接受，本身不作为入场凭证」）：
// - 本人订台列表（酒吧、卡座、到店与最晚到店时间、定金与订台状态）；定金待支付的在此付定金（同订台页的支付口径）。
// - 邀请函由酒吧员工确认订台后签发（第十三批裁决第 4 条「酒吧员工确认后签发」），未签发的注明待签发；签发后可看名单、移出未入场的被邀请人。
// - 转发：小程序原生分享只带邀请函四项码载荷（口令、防伪标记、卡座、可进人数），不带登录凭证；客人链接域名已配置时另出邀请函二维码。
// - 开卡人本人同样凭动态入场码入场（components/dynamic-code，kind=entry）。
const api = require('../../lib/api')
const config = require('../../config')
const core = require('../../core/index')
const pay = require('../../lib/pay')
const { yuan, timeText, qrImage, TABLE_TYPES } = require('../../lib/format')

const DEPOSIT_TEXT = { not_required: '免定金', pending: '定金待支付', paid: '定金已支付', settled: '定金已结算', timeout_released: '超时已释放' }
const RESERVATION_TEXT = { reserved: '已预订', cancelled: '已取消' }
// ENUM「订卡邀请函状态：未签发、已签发、（成员）已确认接受、（成员）已入场、（成员）已取消」。
const MEMBER_TEXT = { confirmed: '已确认接受', entered: '已入场', revoked: '已取消' }

function rowView(row) {
  return Object.assign({}, row, {
    title: `${row.barName || '酒吧'} · ${row.tableName || '台位'}`,
    statusText: RESERVATION_TEXT[row.status] || core.formatBusinessStatus(row.status),
    timeLine: `${TABLE_TYPES[row.tableType] || '台位'} · ${row.guestCount} 人 · 到店 ${timeText(row.reservedAt)} · 最晚 ${timeText(row.latestArrivalAt)}`,
    depositLine: `${DEPOSIT_TEXT[row.depositStatus] || core.formatBusinessStatus(row.depositStatus)} · 定金 ${yuan(row.depositAmountCent)}`,
    payable: row.depositStatus === 'pending' && row.status !== 'cancelled',
    invitationPending: row.invitationStatus === 'none' && row.status !== 'cancelled',
  })
}

function invitationView(invitation, row) {
  let invitee = 0
  const members = (invitation.members || []).map((m) => Object.assign({}, m, {
    name: m.isHolder ? '开卡人（本人）' : `被邀请人 ${(invitee += 1)}`,
    statusText: MEMBER_TEXT[m.status] || core.formatBusinessStatus(m.status),
    removable: !m.isHolder && m.status === 'confirmed',
  }))
  const code = { token: invitation.invitationToken || '', sig: invitation.invitationSignature || '', table_id: invitation.tableId || '', capacity: String(invitation.capacity || '') }
  const link = core.buildCustomerLink(config.linkHost, 'invitation', code)
  return {
    code,
    title: `${row.barName || '酒吧'} · ${row.tableName || '卡座'} · 可进 ${invitation.capacity || 0} 人`,
    countLine: `已确认 ${invitation.confirmedCount || 0} 人 · 已入场 ${invitation.enteredCount || 0} 人 · 剩余 ${invitation.remaining || 0} 个名额`,
    qr: link ? qrImage(link) : '',
    members,
    holderConfirmed: members.some((m) => m.isHolder && m.status === 'confirmed'),
  }
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, error: '', rows: [], openId: '', invitation: null, invitationError: '', notice: '', busy: false,
    payId: '', payUnavailable: false, unavailableText: pay.PAYMENT_UNAVAILABLE_TEXT, awaitingIntentId: '', payError: '',
  },
  methods: {
    onShow() {
      const state = api.pageState('bar')
      this.setData(state)
      if (state.loggedIn && !state.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    service() {
      return api.service('bar')
    },
    load() {
      this.setData({ loading: true, error: '' })
      return this.service().listMyBarReservations()
        .then((rows) => this.setData({ rows: (rows || []).map(rowView) }))
        .catch((err) => this.setData({ error: err.message || '我的订台加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    openInvitation(e) {
      this.setData({ openId: e.currentTarget.dataset.id, invitation: null, invitationError: '', notice: '' })
      this.loadInvitation()
    },
    loadInvitation() {
      const id = this.data.openId
      const row = this.data.rows.find((r) => r.reservationId === id)
      return this.service().getMyBarInvitation(id)
        .then((invitation) => this.setData({ invitation: invitationView(invitation, row) }))
        .catch((err) => this.setData({ invitationError: err.message || '邀请函加载失败' }))
    },
    async removeMember(e) {
      this.setData({ busy: true })
      try {
        await this.service().removeBarInvitationMember(this.data.openId, e.currentTarget.dataset.id)
        this.setData({ notice: '已移出，名额已释放' })
        await this.loadInvitation()
      } catch (err) {
        this.setData({ notice: err.message || '移出失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    confirmDeposit(id, intentId) {
      return this.service().confirmDepositPayment(id, intentId).then((next) => next.depositStatus === 'paid')
    },
    show(id, result) {
      this.setData({
        busy: false, payId: id, payUnavailable: result.state === 'unavailable',
        awaitingIntentId: result.state === 'awaiting' ? result.intentId : '', payError: result.error || '',
      })
      if (result.state === 'paid') this.load()
    },
    async payDeposit(e) {
      const id = e.currentTarget.dataset.id
      this.setData({ busy: true, payId: id, payUnavailable: false, awaitingIntentId: '', payError: '' })
      try {
        this.show(id, await pay.payAndConfirm(() => this.service().startDepositPayment(id), (intentId) => this.confirmDeposit(id, intentId)))
      } catch (err) {
        this.setData({ busy: false, payError: err.message || '发起支付失败' })
      }
    },
    async recheck() {
      const id = this.data.payId
      this.setData({ busy: true })
      this.show(id, await pay.recheck(this.data.awaitingIntentId, (intentId) => this.confirmDeposit(id, intentId)))
    },
    // 小程序原生分享：只带邀请函码载荷，不带会话令牌；没有打开的邀请函时分享首页。
    onShareAppMessage() {
      const invitation = this.data.invitation
      if (invitation && invitation.code.token) {
        const c = invitation.code
        const enc = encodeURIComponent
        return {
          title: `${invitation.title}，邀请你一起来`,
          path: `/pages/invitation/index?token=${enc(c.token)}&sig=${enc(c.sig)}&table_id=${enc(c.table_id)}&capacity=${enc(c.capacity)}`,
        }
      }
      return { title: '综合体客人端', path: '/pages/home/index' }
    },
  },
})
