// 文件版本：V0.01
// 积分、签到与兑换（BSM「查看会员积分摘要和会员等级；积分明细、礼物列表、兑换发起和兑换记录」）：积分摘要决定页面级错误，其余各块读不到各自降级为空并
// 局部提示、不拖垮积分余额（与 App 同）。积分明细按来源取中文名（后端说明文字是内部口径、可能含英文，不直接展示——PGM「界面全中文」）；
// 门票积分与成长值、商户消费积分只展示结果；有待追回积分时暂停兑换（新增积分先抵扣待追回部分，结清前暂停兑换）。
const api = require('../../lib/api')
const core = require('../../core/index')
const { timeText } = require('../../lib/format')

const REWARD_STATUS = { pending: '待确认', confirmed: '已确认', cancelled: '已取消', reversed: '已冲回' }
const LEDGER_LABELS = {
  check_in: '签到', points_redemption: '积分兑换', points_voucher_refund: '兑换退回', ticket_use: '门票奖励', ticket_points_recovery: '抵扣待追回',
  merchant_reward_refund: '消费退款冲回', expire: '积分过期', adjust: '积分调整', earn: '获得积分', deduct: '扣减积分', confirm: '确认积分',
}

function ledgerLine(entry) {
  const label = LEDGER_LABELS[entry.sourceType] || LEDGER_LABELS[entry.changeType] || (entry.pointsDelta >= 0 ? '积分增加' : '积分减少')
  const delta = entry.pointsDelta > 0 ? `+${entry.pointsDelta}` : `${entry.pointsDelta}`
  return { id: entry.id, text: `${label} ${delta} · 余额 ${entry.balanceAfter}${entry.status === 'pending' ? ' · 待确认' : ''}`, time: timeText(entry.createdAt) }
}

function rewardStatus(reward) {
  const status = REWARD_STATUS[reward.status] || core.formatBusinessStatus(reward.status)
  return reward.status === 'pending' && reward.availableAt ? `${status} · 等待期至 ${timeText(reward.availableAt)}，退款处理期间不发放` : status
}

/** 一块副资源：读不到降级为空并记下局部提示。 */
function soft(promise, notices, key, text, empty) {
  return promise.catch(() => {
    notices[key] = text
    return empty
  })
}

Component({
  data: {
    loggedIn: false, paused: false, loading: true, error: '', summary: null, ledger: [], ticketRewards: [], pendingRecovery: 0, rewardsLoaded: false,
    merchantRewards: [], rules: [], checkIns: [], items: [], redemptions: [], notices: {}, busy: false,
  },
  methods: {
    onShow() {
      const state = api.pageState('points')
      this.setData(state)
      if (state.loggedIn && !state.paused) this.load()
    },
    toLogin() {
      api.toLogin()
    },
    service() {
      return api.service('points')
    },
    load() {
      const s = this.service()
      const notices = {}
      this.setData({ error: '' })
      return Promise.all([
        s.getPoints(),
        soft(s.listPointsLedger(), notices, 'ledger', '积分明细暂时加载失败，请稍后重试', []),
        soft(s.listTicketRewards(), notices, 'ticket', '门票奖励及待追回积分暂时加载失败，请稍后重试', null),
        soft(s.listMerchantRewards(), notices, 'merchant', '消费积分与成长值暂时加载失败，请稍后重试', []),
        soft(s.listTicketRewardRules(), notices, 'rules', '门票奖励公告暂时加载失败，请稍后重试', []),
        soft(s.listCheckIns(), notices, 'checkIns', '签到记录暂时加载失败，不影响积分与兑换', []),
        soft(s.listRedemptionItems(), notices, 'items', '兑换目录暂时加载失败，可稍后重试', []),
        soft(s.listRedemptions(), notices, 'redemptions', '兑换记录暂时加载失败，可稍后重试', []),
      ]).then(([summary, ledger, rewards, merchant, rules, checkIns, items, redemptions]) => {
        const balance = summary ? summary.balance : 0
        const recovery = rewards ? rewards.pendingRecovery || 0 : 0
        this.setData({
          notices,
          summary: summary ? Object.assign({}, summary, { levelText: summary.level ? core.formatBusinessStatus(summary.level) : '普通' }) : null,
          ledger: (ledger || []).map(ledgerLine),
          rewardsLoaded: Boolean(rewards),
          pendingRecovery: recovery,
          ticketRewards: ((rewards && rewards.rewards) || []).map((r) => ({
            id: r.id, text: `第 ${r.slotIndex} 人份 · 第 ${r.useIndex} 次 · ${r.points} 积分 · ${r.growth} 成长值`, status: rewardStatus(r),
          })),
          merchantRewards: (merchant || []).map((r) => ({
            id: r.id, text: `${r.merchantName} · 单号 ${r.sourceNo} · ${r.points} 积分 · ${r.growth} 成长值`, status: rewardStatus(r),
          })),
          rules: (rules || []).map((r) => ({
            id: r.id,
            text: `${r.projectName} · ${r.ticketName} · ${r.kind === 'points' ? '每人次积分' : '每人次成长值'} ${r.fixedValue}`,
            time: `${r.version} · 公告于 ${timeText(r.publishedAt)} · 生效于 ${timeText(r.effectiveAt)}`,
          })),
          checkIns: (checkIns || []).map((c) => ({ id: c.id, text: `${c.checkInDate} · 连续 ${c.streak} 天 · +${c.awardedPoints} 积分` })),
          // 兑换要读到门票奖励（才知道有没有待追回积分）；售罄、下架、积分不足、待追回未结清都不可兑。
          items: (items || []).map((item) => {
            const soldOut = item.stockQuantity <= 0 || item.status !== 'active'
            const affordable = balance >= item.pointsCost
            return Object.assign({}, item, {
              typeText: core.formatBusinessStatus(item.itemType),
              label: soldOut ? '已兑罄' : affordable ? '兑换' : '积分不足',
              redeemable: !soldOut && affordable && Boolean(rewards) && recovery === 0,
            })
          }),
          redemptions: (redemptions || []).map((r) => ({
            id: r.id, text: `${r.itemName} ×${r.quantity} · 扣 ${r.totalPoints} 积分 · ${core.formatBusinessStatus(r.status)}`,
          })),
        })
      }).catch((err) => this.setData({ error: err.message || '积分加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    async checkIn() {
      this.setData({ busy: true })
      try {
        const record = await this.service().checkIn()
        wx.showToast({ title: `签到成功，连续 ${record.streak} 天，获 ${record.awardedPoints} 积分`, icon: 'none' })
        await this.load()
      } catch (err) {
        wx.showToast({ title: err.message || '签到失败', icon: 'none' })
      } finally {
        this.setData({ busy: false })
      }
    },
    async redeem(e) {
      const item = this.data.items.find((i) => i.id === e.currentTarget.dataset.id)
      if (!item || !item.redeemable) return
      this.setData({ busy: true })
      try {
        const record = await this.service().redeemPoints(item.id, 1)
        wx.showToast({ title: `兑换成功：${record.itemName} ×${record.quantity}，扣 ${record.totalPoints} 积分`, icon: 'none' })
        await this.load()
      } catch (err) {
        wx.showToast({ title: err.message || '兑换失败', icon: 'none' })
      } finally {
        this.setData({ busy: false })
      }
    },
  },
})
