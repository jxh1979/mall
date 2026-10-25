// 文件版本：V0.01
// 我的券包（BSM「我的券包：客人可查看自己名下全部虚拟券，按「可用 / 已核销（已使用） / 已过期 / 已失效」分组；已失效详情显示原因以及是否退回积分。
// 核销后的券不能再次使用；可用券只有在券种允许时才显示转赠入口」）：
// - 可用的统一台账券可「使用」：出示按全场统一间隔刷新的动态核销码（components/dynamic-code，kind=voucher；BSM「凡是出示出来给人验证的二维码……都不是一张
//   固定的图」），码值只画进二维码、不以文字出现；核销在员工侧。
// - 券种允许的可「转赠」：填手机号或微信分享（第十三批裁决第 9 条「和门票一致不冻结（推荐）」：发起后原持有人仍可自用或改转他人，先成功的为准，其余自动作废）；
//   分享卡片只带领取码（/pages/vouchers/index?claim_code=），不带登录凭证；客人链接域名已配置时另出领取二维码（/c/voucher-claim）。
// - 领取：分享卡片、客人链接入口或粘贴领取码进来只预览，点「确认领取」才领；扫码只认券领取链接。
const api = require('../../lib/api')
const config = require('../../config')
const core = require('../../core/index')
const { yuan, dateText, qrImage, param } = require('../../lib/format')

const GROUP_OF = { issued: 'available', active: 'available', open: 'available', redeemed: 'redeemed', used: 'redeemed', consumed: 'redeemed', claimed: 'redeemed', expired: 'expired' }
const GROUPS = [
  { key: 'available', title: '可用' },
  { key: 'redeemed', title: '已核销（已使用）' },
  { key: 'expired', title: '已过期' },
  { key: 'invalid', title: '已失效' },
]
const STATUS_TEXT = { issued: '可用', active: '可用', open: '可用', redeemed: '已核销', used: '已核销', consumed: '已核销', claimed: '已核销', expired: '已过期', revoked: '已作废', voided: '已失效', inactive: '未生效' }
const KIND_SCOPE = { merchant_discount: '商户折扣券 · 结账时抵扣', in_store_goods: '店内实物券 · 到核销点领取', external_partner: '外部合作伙伴券 · 由合作方核销' }
const METHOD_TEXT = { in_store_staff: '到店由店员核销', onsite_code: '出示动态券码到核销点核销', external_reflow: '由合作方按其系统核销（以合作方为准）' }

function voucherView(v) {
  const status = (v.status || '').toLowerCase()
  const group = GROUP_OF[status] || 'invalid'
  const lines = [KIND_SCOPE[v.voucherKind] || '券']
  if (v.faceValueCent > 0) lines.push(v.voucherKind === 'merchant_discount' ? `抵扣 ${yuan(v.faceValueCent)}` : `面额 ${yuan(v.faceValueCent)}`)
  if (v.expiresAt) lines.push(`有效期至 ${dateText(v.expiresAt)}`)
  if (group === 'available' && METHOD_TEXT[v.redeemMethod]) lines.push(`核销方式：${METHOD_TEXT[v.redeemMethod]}`)
  if (group === 'invalid') {
    lines.push(`失效原因：${v.invalidReason || '未注明'}`)
    if (v.pointsRefunded != null) lines.push(v.pointsRefunded > 0 ? `已退回 ${v.pointsRefunded} 积分` : '未退回积分')
  }
  // 统一台账券且可用才可自助使用；转赠另须券种允许（服务端 transferable）。
  const usable = group === 'available' && v.source === 'unified'
  return Object.assign({}, v, {
    group, lines, usable, transferable: usable && v.transferable === true,
    statusText: STATUS_TEXT[status] || core.formatBusinessStatus(v.status),
    remainText: v.remainingTimes > 1 ? `剩余 ${v.remainingTimes} 次` : '',
  })
}

function scan() {
  return new Promise((resolve) => {
    wx.scanCode({ onlyFromCamera: true, scanType: ['qrCode'], success: (res) => resolve(res.result || ''), fail: () => resolve(null) })
  })
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, error: '', groups: [], incoming: [], outgoing: [],
    useVoucher: null, transferVoucher: null, channel: 'phone', receiverPhone: '', busy: false, transferResult: null, transferQr: '',
    claimCode: '', preview: null, scanNotice: '',
  },
  methods: {
    onLoad(query) {
      const claimCode = param(query, 'claim_code')
      this.setData({ claimCode })
      this.pendingPreview = Boolean(claimCode)
    },
    onShow() {
      const state = api.pageState('vouchers')
      this.setData(state)
      if (!state.loggedIn || state.paused) return
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
      return api.service('vouchers')
    },
    toast(title) {
      wx.showToast({ title, icon: 'none' })
    },
    load() {
      const s = this.service()
      this.setData({ loading: true, error: '' })
      return Promise.all([
        s.listMyVouchers(),
        s.listIncomingVoucherTransfers().catch(() => []),
        s.listOutgoingVoucherTransfers().catch(() => []),
      ]).then(([list, incoming, outgoing]) => {
        const vouchers = (list || []).map(voucherView)
        this.setData({
          groups: GROUPS.map((g) => {
            const items = vouchers.filter((v) => v.group === g.key)
            return { key: g.key, title: `${g.title}（${items.length}）`, items }
          }),
          incoming: (incoming || []).filter((t) => t.status === 'pending'),
          outgoing: (outgoing || []).filter((t) => t.status === 'pending').map((t) => Object.assign({}, t, {
            target: t.channel === 'phone' ? `转赠给 ${t.receiverPhoneMasked || '指定手机号'}` : `领取码：${t.claimCode || '—'}，等待领取`,
          })),
        })
      }).catch((err) => this.setData({ error: err.message || '券包加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    find(id) {
      for (const g of this.data.groups) {
        const v = g.items.find((item) => item.id === id)
        if (v) return v
      }
      return null
    },
    openUse(e) {
      const v = this.find(e.currentTarget.dataset.id)
      if (v && v.usable) this.setData({ useVoucher: v })
    },
    closeUse() {
      this.setData({ useVoucher: null })
    },
    openTransfer(e) {
      const v = this.find(e.currentTarget.dataset.id)
      if (v && v.transferable) this.setData({ transferVoucher: v, channel: 'phone', receiverPhone: '', transferResult: null, transferQr: '' })
    },
    closeTransfer() {
      if (!this.data.busy) this.setData({ transferVoucher: null })
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
    async submitTransfer() {
      const { transferVoucher, channel } = this.data
      const receiverPhone = this.data.receiverPhone.trim()
      if (channel === 'phone' && !receiverPhone) {
        this.toast('请填写接收人手机号')
        return
      }
      this.setData({ busy: true })
      try {
        const result = await this.service().initiateVoucherTransfer({
          voucherInstanceId: transferVoucher.id, channel, receiverPhone: channel === 'phone' ? receiverPhone : undefined,
        })
        const link = result.channel === 'link' && result.claimCode
          ? core.buildCustomerLink(config.linkHost, 'voucher-claim', { claim_code: result.claimCode })
          : null
        this.setData({ transferResult: result, transferQr: link ? qrImage(link) : '' })
        this.load()
      } catch (err) {
        this.toast(err.message || '转赠发起失败')
      } finally {
        this.setData({ busy: false })
      }
    },
    async revoke(e) {
      this.setData({ busy: true })
      try {
        await this.service().revokeVoucherTransfer(e.currentTarget.dataset.id)
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
      if (!claimCode) {
        this.toast('请输入领取码')
        return
      }
      this.setData({ busy: true, preview: null })
      try {
        this.setData({ preview: await this.service().previewVoucherTransfer(claimCode) })
      } catch (err) {
        this.toast(err.message || '领取码无效或已被领取')
      } finally {
        this.setData({ busy: false })
      }
    },
    // 扫对方出示的领取二维码：只认券领取链接，扫到即预览、不领取。
    async scanClaim() {
      const raw = await scan()
      if (raw === null) {
        this.setData({ scanNotice: '未扫到二维码，可打开对方分享的领取入口或粘贴领取码' })
        return
      }
      const link = core.parseCustomerLink(raw, config.linkHost)
      const code = link && link.route === 'voucher-claim' ? link.params.claim_code : ''
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
        await this.service().claimVoucherTransfer(transferId, claimCode)
        this.toast('领取成功，券已进入你的券包')
        this.setData({ claimCode: '', preview: null })
        this.load()
      } catch (err) {
        this.toast(err.message || '领取失败')
      } finally {
        this.setData({ busy: false })
      }
    },
    claimPreview() {
      this.claim(this.data.preview.id, this.data.claimCode.trim())
    },
    claimIncoming(e) {
      this.claim(e.currentTarget.dataset.id)
    },
    // 小程序原生分享：只带领取码，不带会话令牌；没有可分享的领取码时分享首页。
    onShareAppMessage() {
      const result = this.data.transferResult
      if (result && result.channel === 'link' && result.claimCode) {
        return { title: `送你一张${result.voucherName || '券'}`, path: `/pages/vouchers/index?claim_code=${encodeURIComponent(result.claimCode)}` }
      }
      return { title: '综合体客人端', path: '/pages/home/index' }
    },
  },
})
