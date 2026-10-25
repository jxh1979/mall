// 文件版本：V0.01
// 小程序在线付款（购票组合支付、停车缴费、订台定金、扫码点单、储值充值共用；BSM「通过官方 App/小程序购票、预定、确认订单、查看支付结果」）：按 core 的 launchDecision 判定——
// 只有后端签发的小程序微信支付参数（kind=miniapp_wechat）才拉起 wx.requestPayment，参数原样传入、不改不拼；
// 渠道未启用或未配置、没有拉起参数、或本端拉不起的方式（H5 跳转、App 支付）一律「支付暂不可用，未扣款」（小程序不用 web-view 打开网页）。
// 拉起后不论微信回调成功、失败还是取消，都由调用方向服务端查单，渠道确认收款才算付成，不据回调显示成功。
const core = require('../core/index')

/** 返回 'paid'（渠道已收款，去查单）、'launched'（已拉起微信支付并回到本页，去查单）或 'unavailable'。 */
function launchPayment(view) {
  const decision = core.launchDecision(view, 'miniapp', [])
  if (decision.action === 'paid') return Promise.resolve('paid')
  if (decision.action !== 'wx_pay') return Promise.resolve('unavailable')
  return new Promise((resolve) => {
    wx.requestPayment(Object.assign({}, decision.params, { complete: () => resolve('launched') }))
  })
}

/**
 * 查单一次：confirm(付款编号) 返回 true＝渠道已确认收款。返回 { state: 'paid' | 'awaiting', intentId, error }；
 * 未确认或查单失败都留着付款编号，客人付完点「我已完成支付」再查。
 */
async function recheck(intentId, confirm) {
  try {
    if (await confirm(intentId)) return { state: 'paid', intentId, error: '' }
    return { state: 'awaiting', intentId, error: '尚未确认收款，请完成支付后再查' }
  } catch (err) {
    return { state: 'awaiting', intentId, error: err.message || '支付结果确认失败' }
  }
}

/**
 * 订台定金、扫码点单、储值充值共用：start() 向服务端发起付款（金额由服务端定）并返回拉起视图，按本端拉起后一律 recheck 查单。
 * 返回 recheck 的结果，或 { state: 'unavailable' }（支付暂不可用，未扣款）；发起失败照常抛错由页面提示。
 */
async function payAndConfirm(start, confirm) {
  const view = await start()
  if ((await launchPayment(view)) === 'unavailable') return { state: 'unavailable', intentId: '', error: '' }
  return recheck(view.paymentIntentId, confirm)
}

module.exports = { launchPayment, payAndConfirm, recheck, PAYMENT_UNAVAILABLE_TEXT: core.PAYMENT_UNAVAILABLE_TEXT }
