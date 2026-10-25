// 文件版本：V0.01
// 账户注销（BSM「账户注销：客人可以自行申请注销账户。发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；放弃的存酒
// 按存酒到期规则转为所在酒吧的自有库存资产，放弃的储值卡余额归发卡主体……需要退还本金的注销事项按本文件「储值」条自动生成待退事项、通知发卡主体执行」）：
// 先预览，逐卡列本金与赠送金、逐条列名下存酒（所属酒吧、到期日）；名下有存酒的须明确勾选放弃存酒才可注销。2026-10-25 操作员更正「即使注销，门票是
// 不可以退的，除非碰上特殊安全情况」（取代第十三批第 8、12 条）：名下未使用的票注销时作废、不退款，不因此拦注销，本页不设退票入口。
// 注销成功即撤销会话、清本端令牌回首页。
const api = require('../../lib/api')
const { yuan, dateText } = require('../../lib/format')

function previewView(p) {
  const cards = (p.storedValueCards || []).map((c) => Object.assign({}, c, {
    text: `${c.cardScope === 'merchant' ? `商户卡（${c.merchantId || '—'}）` : '综合体通用卡'}：本金 ${yuan(c.principalCent)} · 赠送金 ${yuan(c.bonusCent)}`,
  }))
  return {
    cards,
    hasBalance: cards.some((c) => c.principalCent + c.bonusCent > 0),
    hasPrincipal: cards.some((c) => c.principalCent > 0),
    bottles: (p.savedBottles || []).map((b) => Object.assign({}, b, {
      text: `${b.barName || '酒吧'} · ${b.bottleName} ${b.remainingQuantity} ${b.unit} · 到期 ${b.expireAt ? dateText(b.expireAt) : '—'}`,
    })),
  }
}

Component({
  data: { loggedIn: false, paused: false, loading: true, error: '', preview: null, abandonBottles: false, waive: false, busy: false, notice: '' },
  methods: {
    onShow() {
      const state = api.pageState('account')
      this.setData(state)
      if (state.loggedIn && !state.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    load() {
      this.setData({ error: '' })
      return api.service('account').getDeregisterPreview()
        .then((p) => this.setData({ preview: previewView(p) }))
        .catch((err) => this.setData({ error: err.message || '注销预览加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    onAbandon(e) {
      this.setData({ abandonBottles: (e.detail.value || []).includes('abandon') })
    },
    onWaive(e) {
      this.setData({ waive: (e.detail.value || []).includes('waive') })
    },
    async submit() {
      const { preview, abandonBottles, waive } = this.data
      if (preview.bottles.length && !abandonBottles) {
        this.setData({ notice: '名下有存酒，须勾选放弃名下存酒后才能注销' })
        return
      }
      this.setData({ busy: true, notice: '' })
      try {
        await api.service('account').deregister({ waiveBalance: waive, abandonSavedBottles: abandonBottles })
        await api.logout()
        wx.reLaunch({ url: '/pages/home/index', complete: () => wx.showToast({ title: '账号已注销', icon: 'none' }) })
      } catch (err) {
        this.setData({ notice: err.message || '注销失败' })
        this.load()
      } finally {
        this.setData({ busy: false })
      }
    },
  },
})
