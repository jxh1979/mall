// 文件版本：V0.01
// 登录与注册（BSM「登录可支持手机号短信验证码和微信；创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款
// 和隐私条款。条件全部满足即自动创建有效会员，否则注册失败且不创建身份」；第十三批裁决第 3 条「满 18 周岁（推荐）」）：
// 先取短信验证码再登录；未注册的手机号登录时服务端不建号（409 CUSTOMER_REGISTRATION_REQUIRED，验证码不被消费），转注册：填生日、
// 阅读并逐份同意当前两份条款，同一验证码提交注册。年龄与条款版本由服务端判定，不满足即注册失败、不建会话。
// 登录成功回到来的页面（没有则回首页）并拉一次到期提醒（本次运行中刚登录的客人不必等下次回前台）；令牌只交 lib/api.js 保存，不进页面路径。
// 微信登录外部资料未到，未做。
const api = require('../../lib/api')
const { todayText } = require('../../lib/format')
const { remindExpiring } = require('../../lib/reminders')

Component({
  data: {
    mode: 'login', phone: '', smsCode: '', birthDate: '', today: '', agreeMember: false, agreePrivacy: false,
    terms: null, busy: false, sent: '', error: '', notice: '',
  },
  methods: {
    onLoad(query) {
      this.setData({ today: todayText(), notice: query && query.expired ? '登录已过期，请重新登录' : '' })
    },
    onPhone(e) {
      this.setData({ phone: e.detail.value })
    },
    onSms(e) {
      this.setData({ smsCode: e.detail.value })
    },
    onBirth(e) {
      this.setData({ birthDate: e.detail.value })
    },
    onAgree(e) {
      const values = e.detail.value || []
      this.setData({ agreeMember: values.includes('member'), agreePrivacy: values.includes('privacy') })
    },
    async requestCode() {
      const phone = this.data.phone.trim()
      if (!phone) {
        this.setData({ error: '请输入手机号' })
        return
      }
      this.setData({ busy: true, error: '', sent: '' })
      try {
        const receipt = await api.service(null).requestLoginSmsCode(phone)
        this.setData({ sent: `验证码已发送至 ${receipt.phoneMasked}，5 分钟内有效` })
      } catch (err) {
        this.setData({ error: err.message || '验证码发送失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    async toRegister() {
      this.setData({ mode: 'register', error: '' })
      if (this.data.terms) return
      try {
        this.setData({ terms: await api.service(null).getTerms() })
      } catch (err) {
        this.setData({ error: err.message || '条款加载失败' })
      }
    },
    switchMode() {
      if (this.data.mode === 'register') this.setData({ mode: 'login', error: '' })
      else this.toRegister()
    },
    missing() {
      const { mode, phone, smsCode, birthDate, agreeMember, agreePrivacy, terms } = this.data
      if (!phone.trim()) return '请输入手机号'
      if (!smsCode.trim()) return '请输入验证码'
      if (mode !== 'register') return ''
      if (!terms) return '会员条款与隐私条款尚未加载，请稍后再试'
      if (!birthDate) return '请选择生日'
      if (!agreeMember) return '请阅读并同意会员条款'
      if (!agreePrivacy) return '请阅读并同意隐私条款'
      return ''
    },
    async submit() {
      const missing = this.missing()
      if (missing) {
        this.setData({ error: missing })
        return
      }
      const { mode, terms, birthDate } = this.data
      const phone = this.data.phone.trim()
      const smsCode = this.data.smsCode.trim()
      this.setData({ busy: true, error: '' })
      try {
        const result = mode === 'login'
          ? await api.service(null).phoneLogin({ phone, smsCode })
          : await api.service(null).register({
            phone, smsCode, birthDate, memberTermsVersion: terms.memberTerms.version, privacyTermsVersion: terms.privacyPolicy.version,
          })
        api.saveSession(result)
        remindExpiring()
        wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/home/index' }) })
      } catch (err) {
        if (mode === 'login' && err && err.detailCode === 'CUSTOMER_REGISTRATION_REQUIRED') await this.toRegister()
        else this.setData({ error: err.message || (mode === 'login' ? '登录失败' : '注册失败') })
      } finally {
        this.setData({ busy: false })
      }
    },
  },
})
