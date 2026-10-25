// 文件版本：V0.01
// 学生认证（BSM「学生认证必须在本人注册会员后单独申请，认证通过只增加学生资格，不自动创建会员身份」；BSM「学生票要票务运营岗人工审核证件 + 学信网核验通过
// 才可用」；BSM「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」；第十三批裁决第 1 条「有效到客人提供的学信网
// 《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」）：先单独同意证件采集（记同意编号与版本，时间由服务端记），才可选证件照片（1～9 张）并填
// 学信网在线验证码提交；待审核期间不能再申请，被驳回的可重新申请。同意的范围原文与版本与官方 App 同一份。
const api = require('../../lib/api')
const media = require('../../lib/media')

const CONSENT_VERSION = '学生证件采集同意 V0.01'
const CONSENT_SCOPE = '为申请学生资格，同意本平台采集本人学生证件照片与学信网《学籍在线验证报告》在线验证码，仅供票务运营岗人工审核学生资格使用；注销账号时一并删除。'
const MAX_PHOTOS = 9

function statusView(cert) {
  if (!cert) return null
  const line = cert.status === 'approved' ? `${cert.statusLabel}：学生资格有效至 ${cert.validUntil || '—'}`
    : cert.status === 'rejected' ? `${cert.statusLabel}：${cert.rejectReason || '未注明原因'}` : cert.statusLabel
  return { line, tone: cert.status === 'approved' ? 'success' : cert.status === 'rejected' ? 'warn' : 'info', canApply: cert.status === 'rejected' }
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, current: null, canApply: false, consentId: '', photos: [], chsiCode: '', busy: false, error: '',
    consentScope: CONSENT_SCOPE, consentVersion: CONSENT_VERSION, maxPhotos: MAX_PHOTOS,
  },
  methods: {
    onShow() {
      const state = api.pageState('account')
      this.setData(state)
      if (!state.loggedIn || state.paused) return
      api.service('account').getStudentCertification()
        .then((cert) => this.showCurrent(cert))
        .catch((err) => this.setData({ error: err.message || '学生认证加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    toLogin() {
      api.toLogin()
    },
    showCurrent(cert) {
      const current = statusView(cert)
      this.setData({ current, canApply: !current || current.canApply })
    },
    async run(action) {
      this.setData({ busy: true, error: '' })
      try {
        await action()
      } catch (err) {
        this.setData({ error: err.message || '操作失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    agree() {
      this.run(async () => {
        const consent = await api.service('account').recordConsent({ consentType: 'student_certificate', accepted: true, version: CONSENT_VERSION })
        this.setData({ consentId: consent.id })
      })
    },
    async addPhoto() {
      const room = MAX_PHOTOS - this.data.photos.length
      if (!this.data.consentId || room <= 0) return
      const files = await media.chooseMedia('image', room)
      this.setData({ photos: this.data.photos.concat(files).slice(0, MAX_PHOTOS) })
    },
    onChsi(e) {
      this.setData({ chsiCode: e.detail.value })
    },
    submit() {
      const chsiVerificationCode = this.data.chsiCode.trim()
      if (!this.data.photos.length || !chsiVerificationCode) {
        this.setData({ error: '请添加学生证件照片并填写学信网在线验证码' })
        return
      }
      this.run(async () => {
        const cert = await api.service('account').applyStudentCertification({ consentId: this.data.consentId, chsiVerificationCode, files: this.data.photos })
        this.setData({ consentId: '', photos: [], chsiCode: '' })
        this.showCurrent(cert)
      })
    },
  },
})
