// 文件版本：V0.01
// 接受订卡邀请（BSM「朋友在 App/小程序确认接受后占一个名额，该名额自动关联到接受人本人的动态入场码，到店出示本人的动态入场码入场」；BSM「邀请函二维码
// 只用于转发与接受，本身不作为入场凭证（邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受）」）：开卡人分享的卡片、客人链接入口或扫邀请函
// 二维码带四项码载荷进来，先预览（不占名额），点「确认接受」才占名额；接受后出示本人动态入场码（components/dynamic-code，kind=entry）。
// 扫码只认本综合体的邀请函链接（core parseCustomerLink 白名单）。须登录（名额关联到接受人本人）。
const api = require('../../lib/api')
const config = require('../../config')
const core = require('../../core/index')
const { timeText, param } = require('../../lib/format')

/** 码载荷 → 预览与接受要原样回传核验的四项（参数格式已由 parseCustomerLink 或分享路径带入，篡改的由服务端拒绝）。 */
function codeOf(params) {
  if (!params || !params.token) return null
  return { invitationToken: params.token, invitationSignature: params.sig || '', tableId: params.table_id || '', capacity: Number(params.capacity) }
}

function scan() {
  return new Promise((resolve) => {
    wx.scanCode({ onlyFromCamera: true, scanType: ['qrCode'], success: (res) => resolve(res.result || ''), fail: () => resolve(null) })
  })
}

Component({
  data: { loggedIn: false, paused: false, preview: null, member: null, reservationId: '', busy: false, error: '' },
  methods: {
    onLoad(query) {
      this.code = codeOf({ token: param(query, 'token'), sig: param(query, 'sig'), table_id: param(query, 'table_id'), capacity: param(query, 'capacity') })
    },
    onShow() {
      const state = api.pageState('bar')
      this.setData(state)
      if (state.loggedIn && !state.paused && this.code && !this.data.preview && !this.data.member) this.preview()
    },
    toLogin() {
      api.toLogin()
    },
    async preview() {
      this.setData({ busy: true, error: '', preview: null })
      try {
        const preview = await api.service('bar').previewBarInvitation(this.code)
        this.setData({
          preview: Object.assign({}, preview, {
            title: `${preview.barName || '酒吧'} · ${preview.tableName || '卡座'} · 可进 ${preview.capacity} 人`,
            line: `预订时间 ${timeText(preview.reservedAt)} · 剩余 ${preview.remaining} 个名额`,
          }),
        })
      } catch (err) {
        this.setData({ error: err.message || '邀请函无效' })
      } finally {
        this.setData({ busy: false })
      }
    },
    async scanInvitation() {
      this.setData({ error: '' })
      const raw = await scan()
      if (raw === null) {
        this.setData({ error: '未扫到二维码，可打开朋友分享的邀请' })
        return
      }
      const link = core.parseCustomerLink(raw, config.linkHost)
      if (!link || link.route !== 'invitation') {
        this.setData({ error: '无法识别的二维码' })
        return
      }
      this.code = codeOf(link.params)
      this.preview()
    },
    async accept() {
      this.setData({ busy: true, error: '' })
      try {
        const member = await api.service('bar').confirmBarInvitation(this.code)
        this.setData({ member, reservationId: member.reservationId || this.data.preview.reservationId })
      } catch (err) {
        this.setData({ error: err.message || '接受邀请失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
  },
})
