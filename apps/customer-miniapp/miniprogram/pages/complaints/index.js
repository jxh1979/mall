// 文件版本：V0.01
// 投诉与满意度（BSM「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」；BSM「满意度在回访/验收时由提单方评分」）：
// - 提交：选综合体（多个时）、标题、文字留言，照片与视频用 wx.chooseMedia（拍摄或相册），语音用录音管理器录 mp3；附件合计最多 9 个，类型由服务端按扩展名判定。
//   文字与全部附件随同一个请求编号一次提交（OpenAPI multipart，lib/api.js 组表单），结果未明再提交沿用原编号、不重复建单。
// - 我的投诉：只列本人提交的；写了处理结论、尚未评过的才可评 1～5 分，同单只评一次。
// - 客人端已暂停服务的综合体不列（PGM「关闭后菜单不可见或不可点」）；提交带所选综合体（X-Project-Id，后端按综合体拒绝）。
const api = require('../../lib/api')
const media = require('../../lib/media')
const { timeText } = require('../../lib/format')

const MAX_FILES = 9
const SCORES = [1, 2, 3, 4, 5]

function complaintView(c) {
  return Object.assign({}, c, {
    meta: `编号 ${c.code} · 提交于 ${timeText(c.submittedAt)} · 附件 ${c.mediaCount} 个`,
    rated: c.satisfaction != null,
    rateable: c.satisfaction == null && Boolean(c.resolution),
  })
}

Component({
  data: {
    loggedIn: false, paused: false, projects: [], projectNames: [], projectIndex: 0, title: '', description: '', files: [], recording: false,
    busy: false, error: '', submitted: '', mine: [], mineError: '', scores: SCORES, rating: false, maxFiles: MAX_FILES,
  },
  methods: {
    onLoad() {
      // 录音管理器全局唯一：只登记一次结束回调，录完即加入附件。
      this.recorder = wx.getRecorderManager()
      this.recorder.onStop((res) => {
        this.setData({ recording: false })
        if (res && res.tempFilePath) this.addFiles([media.fileOf(res.tempFilePath)])
      })
      // 未授权麦克风等录音失败：退出录音状态（否则提交按钮一直不可点）并提示。
      this.recorder.onError(() => this.setData({ recording: false, error: '录音失败，请允许使用麦克风后重试' }))
    },
    onShow() {
      const state = api.pageState('complaints')
      this.setData(state)
      if (!state.loggedIn || state.paused) return
      if (!this.data.projects.length) {
        api.service('complaints').listProjects()
          .then((rows) => {
            const projects = (rows || []).filter((p) => api.projectOpen(p.projectId))
            this.setData({ projects, projectNames: projects.map((p) => p.projectName) })
          })
          .catch((err) => this.setData({ error: err.message || '综合体加载失败' }))
      }
      this.loadMine()
    },
    toLogin() {
      api.toLogin()
    },
    loadMine() {
      return api.service('complaints').listMyComplaints()
        .then((rows) => this.setData({ mine: (rows || []).map(complaintView), mineError: '' }))
        .catch((err) => this.setData({ mineError: err.message || '我的投诉加载失败' }))
    },
    onProject(e) {
      this.setData({ projectIndex: Number(e.detail.value) })
    },
    onTitle(e) {
      this.setData({ title: e.detail.value })
    },
    onDescription(e) {
      this.setData({ description: e.detail.value })
    },
    addFiles(files) {
      this.setData({ files: this.data.files.concat(files).slice(0, MAX_FILES) })
    },
    async pick(kind) {
      const room = MAX_FILES - this.data.files.length
      if (room <= 0) return
      this.addFiles(await media.chooseMedia(kind, room))
    },
    addImage() {
      this.pick('image')
    },
    addVideo() {
      this.pick('video')
    },
    startRecord() {
      if (this.data.files.length >= MAX_FILES) return
      // 时长取小程序录音上限 10 分钟（默认只有 1 分钟，会替客人截断）。
      this.recorder.start({ format: 'mp3', duration: 600000 })
      this.setData({ recording: true })
    },
    stopRecord() {
      this.recorder.stop()
    },
    removeFile(e) {
      const index = Number(e.currentTarget.dataset.index)
      this.setData({ files: this.data.files.filter((_, i) => i !== index) })
    },
    async submit() {
      const project = this.data.projects[this.data.projectIndex]
      const title = this.data.title.trim()
      if (!project || !title) {
        this.setData({ error: !project ? '综合体尚未加载，请稍后再试' : '请填写投诉标题' })
        return
      }
      this.setData({ busy: true, error: '', submitted: '' })
      try {
        const result = await api.service('complaints', project.projectId).submitComplaint({
          projectId: project.projectId, title, description: this.data.description.trim() || undefined, files: this.data.files,
        })
        this.setData({ submitted: `投诉已提交，编号 ${result.code}`, title: '', description: '', files: [] })
        this.loadMine()
      } catch (err) {
        this.setData({ error: err.message || '投诉提交失败' })
      } finally {
        this.setData({ busy: false })
      }
    },
    async rate(e) {
      const { id, score } = e.currentTarget.dataset
      this.setData({ rating: true })
      try {
        await api.service('complaints').rateComplaint(id, Number(score))
        await this.loadMine()
      } catch (err) {
        this.setData({ mineError: err.message || '评价失败' })
      } finally {
        this.setData({ rating: false })
      }
    },
  },
})
