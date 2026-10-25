// 文件版本：V0.01
// 账户与条款（ENUM「会员注册……同意当前会员条款和隐私条款后，系统自动注册成功」）：显示登录手机号（掩码），可随时查看当前生效的会员条款与隐私条款及版本。
// 证件采集等敏感信息的单独同意在对应页面使用前取得（BSM「采集证件……前先单独取得同意」，见学生认证）。
const api = require('../../lib/api')

Component({
  data: { loggedIn: false, paused: false, maskedPhone: '', terms: null, error: '' },
  methods: {
    onShow() {
      const state = api.pageState('account')
      this.setData(Object.assign(state, { maskedPhone: api.maskedPhone() || '' }))
      if (!state.loggedIn || state.paused || this.data.terms) return
      api.service('account').getTerms()
        .then((terms) => this.setData({ terms }))
        .catch((err) => this.setData({ error: err.message || '条款加载失败' }))
    },
    toLogin() {
      api.toLogin()
    },
  },
})
