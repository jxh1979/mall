// 文件版本：V0.01
// 门票转赠（BSM「权益转赠允许通过微信、二维码、手机号等形式转赠，但必须按完整权益单位转赠……微信链接或二维码允许多人打开，由最先点击
// “确认领取”的人取得权益……仅打开转赠页面不算领取」「发起转赠后不冻结该权益：在接收人点击“确认领取”之前，原持有人仍然可以自己使用这份权益，
// 也可以再次转赠给其他人……以先处理成功的那个动作为准，其余动作自动失效并提示」「转出时，系统须同时向转出人与受赠人明示这一场次限制」）：
// - 送出：按完整人份；发起后票照常可用、可改转他人；手机号送出由服务端通知对方；链接送出用小程序原生分享，分享卡片只带领取码
//   （/pages/transfer/index?claim_code=），不带任何会话令牌；客人链接域名已配置时另出领取二维码（https://<客人链接主机>/c/ticket-claim?claim_code=）。
// - 领取：分享卡片或领取链接带码进来只预览，点「确认领取」才领；场次已结束的「确认领取」不可点；扫码只认门票领取链接。
const api = require('../../lib/api')
const config = require('../../config')
const core = require('../../core/index')
const { qrImage } = require('../../lib/format')

const SESSION_ENDED = '该权益所在场次已结束、权益已终结'

/** 多人票同票已有人入场：本人份只能在该场次内入场。 */
function sessionLimitText(limit) {
  if (!limit) return ''
  if (limit.sessionEnded) return SESSION_ENDED
  if (!limit.sessionRestricted) return ''
  return `同票已有人入场：这一人份只能在 ${limit.sessionLabel || '该场次'} 场次内入场，该场次结束后权益即告终结`
}

function slotsOf(tickets) {
  const map = new Map()
  for (const t of tickets) {
    if (!t.parentTicketId) continue
    const key = `${t.parentTicketId}-${t.slotIndex || 1}`
    if (!map.has(key)) {
      map.set(key, { key, parentTicketId: t.parentTicketId, slotIndex: t.slotIndex || 1, ticketName: t.ticketName || '门票',
        pending: t.transferState === 'transferring' })
    }
  }
  return [...map.values()]
}

function scan() {
  return new Promise((resolve) => {
    wx.scanCode({ onlyFromCamera: true, scanType: ['qrCode'], success: (res) => resolve(res.result || ''), fail: () => resolve(null) })
  })
}

Component({
  data: {
    loggedIn: false, loading: true, error: '', slots: [], outgoing: [], incoming: [], outgoingNotice: '', incomingNotice: '',
    channel: 'phone', receiverPhone: '', busy: false, created: null, createdLimit: '', claimQr: '',
    claimCode: '', preview: null, previewLimit: '', scanNotice: '',
  },
  methods: {
    onLoad(query) {
      this.setData({ claimCode: (query && query.claim_code) || '' })
      this.pendingPreview = Boolean(query && query.claim_code)
    },
    onShow() {
      const loggedIn = api.hasSession()
      this.setData({ loggedIn })
      if (!loggedIn) return
      this.load()
      // 领取入口带码进来：自动预览，不领取。
      if (this.pendingPreview) {
        this.pendingPreview = false
        this.preview(this.data.claimCode)
      }
    },
    toLogin() {
      api.toLogin()
    },
    service() {
      return api.service('transfer')
    },
    load() {
      const service = this.service()
      this.setData({ loading: true, error: '', outgoingNotice: '', incomingNotice: '' })
      // 我的票决定页面级错误；转出、转入各自降级为空并局部提示。
      Promise.all([
        service.listMyTickets(),
        service.listOutgoingTicketTransfers().catch(() => {
          this.setData({ outgoingNotice: '我发起的转赠暂时加载失败，不影响其它内容' })
          return []
        }),
        service.listIncomingTicketTransfers().catch(() => {
          this.setData({ incomingNotice: '待我领取暂时加载失败，不影响其它内容' })
          return []
        }),
      ]).then(([tickets, outgoing, incoming]) => {
        this.setData({
          slots: slotsOf(tickets || []),
          outgoing: (outgoing || []).map((t) => Object.assign({}, t, {
            statusText: core.formatBusinessStatus(t.status),
            target: t.channel === 'link' && t.claimCode ? `领取码：${t.claimCode}` : t.receiverPhoneMasked ? `送给 ${t.receiverPhoneMasked}` : '微信分享',
            limit: sessionLimitText(t),
          })),
          incoming: (incoming || []).map((t) => Object.assign({}, t, { limit: sessionLimitText(t) || '有人把这张票转赠给了你的手机号' })),
        })
      }).catch((err) => this.setData({ error: err.message || '转赠信息加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    pickPhone() {
      this.setData({ channel: 'phone' })
    },
    pickLink() {
      this.setData({ channel: 'link' })
    },
    onReceiverPhone(e) {
      this.setData({ receiverPhone: e.detail.value })
    },
    toast(title) {
      wx.showToast({ title, icon: 'none' })
    },
    async initiate(e) {
      const slot = this.data.slots.find((s) => s.key === e.currentTarget.dataset.key)
      const { channel } = this.data
      const receiverPhone = this.data.receiverPhone.trim()
      if (!slot) return
      if (channel === 'phone' && !receiverPhone) {
        this.toast('请填写受赠人手机号')
        return
      }
      this.setData({ busy: true })
      try {
        const created = await this.service().initiateTicketTransfer({
          parentTicketId: slot.parentTicketId, slotIndex: slot.slotIndex, channel, receiverPhone: channel === 'phone' ? receiverPhone : undefined,
        })
        const link = created.channel === 'link' && created.claimCode
          ? core.buildCustomerLink(config.linkHost, 'ticket-claim', { claim_code: created.claimCode })
          : null
        this.setData({ created, createdLimit: sessionLimitText(created), claimQr: link ? qrImage(link) : '', receiverPhone: '' })
        this.load()
      } catch (err) {
        this.toast(err.message || '发起转赠失败')
      } finally {
        this.setData({ busy: false })
      }
    },
    async revoke(e) {
      this.setData({ busy: true })
      try {
        await this.service().revokeTicketTransfer(e.currentTarget.dataset.id)
        this.toast('已撤回转赠')
        this.load()
      } catch (err) {
        this.toast(err.message || '撤回失败')
      } finally {
        this.setData({ busy: false })
      }
    },
    onClaimCode(e) {
      this.setData({ claimCode: e.detail.value })
    },
    previewInput() {
      this.preview(this.data.claimCode)
    },
    async preview(code) {
      const claimCode = (code || '').trim()
      if (!claimCode) return
      this.setData({ busy: true })
      try {
        const preview = await this.service().previewTicketTransfer(claimCode)
        this.setData({ preview, previewLimit: sessionLimitText(preview) })
      } catch (err) {
        this.setData({ preview: null, previewLimit: '' })
        this.toast(err.message || '领取码无效或已失效')
      } finally {
        this.setData({ busy: false })
      }
    },
    // 扫对方出示的领取二维码：只认门票领取链接，扫到即预览、不领取。
    async scanClaim() {
      const raw = await scan()
      if (raw === null) {
        this.setData({ scanNotice: '未扫到二维码，可打开对方分享的领取入口或粘贴领取码' })
        return
      }
      const link = core.parseCustomerLink(raw, config.linkHost)
      const code = link && link.route === 'ticket-claim' ? link.params.claim_code : ''
      if (!code) {
        this.setData({ scanNotice: '无法识别的二维码' })
        return
      }
      this.setData({ scanNotice: '', claimCode: code })
      this.preview(code)
    },
    // 先成功的动作为准：原持有人已使用或已改转他人时，服务端拒绝领取并给出原因。
    async claim(transferId, claimCode) {
      this.setData({ busy: true })
      try {
        await this.service().claimTicketTransfer(transferId, claimCode)
        this.toast('领取成功，票已到你的“我的票”')
        this.setData({ claimCode: '', preview: null, previewLimit: '' })
        this.load()
      } catch (err) {
        this.toast(err.message || '领取失败')
      } finally {
        this.setData({ busy: false })
      }
    },
    claimPreview() {
      this.claim(this.data.preview.transferId, this.data.claimCode.trim())
    },
    claimIncoming(e) {
      this.claim(e.currentTarget.dataset.id)
    },
    // 小程序原生分享：只带领取码，不带会话令牌；没有可分享的领取码时分享首页。
    onShareAppMessage() {
      const created = this.data.created
      if (created && created.channel === 'link' && created.claimCode) {
        return { title: `送你一张${created.ticketName || '门票'}`, path: `/pages/transfer/index?claim_code=${encodeURIComponent(created.claimCode)}` }
      }
      return { title: '综合体客人端', path: '/pages/home/index' }
    },
  },
})
