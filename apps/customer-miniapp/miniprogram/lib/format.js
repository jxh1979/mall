// 文件版本：V0.01
// 页面展示用的格式（PGM「界面全中文」：金额用元、日期一律中文格式）与二维码图（纯 JS 编码成图片数据，只在内存里，不写存储）。
const qrcode = require('../vendor/qrcode')

/** 分 → ¥元；没有金额显示「—」。 */
function yuan(cent) {
  return cent == null ? '—' : `¥${(cent / 100).toFixed(2)}`
}

function dateText(iso) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '—' : `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function timeText(iso) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${dateText(iso)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function todayText() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 日期选择器（YYYY-MM-DD）与时间选择器（HH:mm）拼成本机时区的时刻再转 ISO；任一未选返回空串。 */
function localIso(date, time) {
  if (!date || !time) return ''
  const [y, m, d] = date.split('-').map(Number)
  const [hh, mm] = time.split(':').map(Number)
  return new Date(y, m - 1, d, hh, mm).toISOString()
}

/** 页面路径参数（分享卡片、客人链接入口带进来的值经过 encodeURIComponent，onLoad 拿到的是原样）；解不开的按没有。 */
function param(query, key) {
  try {
    return decodeURIComponent((query && query[key]) || '')
  } catch (err) {
    return ''
  }
}

/** 台位类型（后端 bar_tables.table_type）。 */
const TABLE_TYPES = { card_seat: '卡座', vip: '包厢', bar_counter: '吧台位', loose_table: '散台' }

/** 二维码图片数据（image 组件直接显示；不开长按识别与保存）。 */
function qrImage(text) {
  const qr = qrcode(0, 'M')
  qr.addData(text)
  qr.make()
  return qr.createDataURL(6, 4)
}

module.exports = { yuan, dateText, timeText, todayText, localIso, param, TABLE_TYPES, qrImage }
