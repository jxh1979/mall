// 文件版本：V0.01
// 首页（BSM「查看综合体介绍、营业时间、活动公告、票务入口」）：不登录也可看。综合体介绍与活动公告只展示经审批发布的
// （第十三批裁决第 5 条「上级审批后发布」：运营编辑后走审批，批准才对客人展示——服务端未发布即为空，本页不展示）；各场地营业时间
// （闭店不晚于开门＝跨夜，写「次日」）；可售票种；购票、停车缴费入口（模块被终端开关关闭即不显示）。
// 每个综合体开放中的游乐项目与尚未结束的场次（BSM「连接游乐系统，展示可预定项目和场次」）：只展示，不在此预约；读不到不显示。
const api = require('../../lib/api')
const core = require('../../core/index')
const { yuan, timeText } = require('../../lib/format')

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const ENTRIES = [
  { key: 'buy', title: '购票', module: 'ticketing', url: '/pages/buy/index' },
  { key: 'parking', title: '停车缴费', module: 'parking', url: '/pages/parking/index' },
]

function hoursByVenue(rows) {
  const venues = new Map()
  for (const row of rows || []) {
    const venue = venues.get(row.venueId) || { venueName: row.venueName, days: [] }
    const overnight = row.closeTime <= row.openTime
    venue.days.push(`${WEEKDAYS[row.dayOfWeek] || ''} ${row.openTime}–${overnight ? '次日 ' : ''}${row.closeTime}`)
    venues.set(row.venueId, venue)
  }
  return [...venues.values()].map((venue) => ({ venueName: venue.venueName, text: venue.days.join('；') }))
}

function amusementView(item) {
  return {
    id: item.amusementProjectId,
    name: item.projectName,
    sessions: (item.sessions || []).map((s) => ({
      id: s.sessionId,
      text: `${timeText(s.startTime)}–${timeText(s.endTime).slice(-5)} · ${core.formatBusinessStatus(s.status)} · 余 ${Math.max(0, s.capacity - s.usedCapacity)} / ${s.capacity}`,
    })),
  }
}

Component({
  data: { projects: [], products: [], projectError: '', productError: '', loading: true, entries: [] },
  methods: {
    onLoad() {
      const service = api.service(null)
      service.listProjects()
        .then((rows) => {
          this.setData({ projects: (rows || []).map((p) => Object.assign({}, p, { venues: hoursByVenue(p.businessHours), amusements: [] })) })
          for (const project of rows || []) {
            service.listAmusementProjects(project.projectId)
              .then((items) => this.setData({
                projects: this.data.projects.map((p) => (p.projectId === project.projectId
                  ? Object.assign({}, p, { amusements: (items || []).map(amusementView) }) : p)),
              }))
              .catch(() => undefined)
          }
        })
        .catch((err) => this.setData({ projectError: err.message || '综合体信息加载失败' }))
      service.listTicketProducts()
        .then((rows) => this.setData({
          products: (rows || []).map((p) => ({
            id: p.ticketTypeId,
            name: p.ticketName,
            statusText: core.formatBusinessStatus(p.status),
            priceText: `平日 ${yuan(p.weekdayPriceCent)} · 周末 ${yuan(p.weekendPriceCent)} · 节假日 ${yuan(p.holidayPriceCent)}`,
          })),
        }))
        .catch((err) => this.setData({ productError: err.message || '票种加载失败' }))
        .then(() => this.setData({ loading: false }))
    },
    onShow() {
      this.setData({ entries: ENTRIES.filter((entry) => !api.moduleHidden(entry.module)) })
    },
    open(e) {
      wx.navigateTo({ url: e.currentTarget.dataset.url })
    },
  },
})
