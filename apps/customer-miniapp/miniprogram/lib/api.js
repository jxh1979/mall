// 文件版本：V0.01
// 小程序的请求、会话与终端开关：全工程只有本文件发网络请求、读写本地存储、读本地临时文件（上传附件用；test/structure.test.js 断言）。
// - 请求经三端共用的客人业务模块（core，构建时由 packages/customer-core 编译而来），本端只把 wx.request 包成它要的请求通道；
//   每个请求带 X-Terminal-Type: miniapp 与 X-Client-Version，有会话带 X-Customer-Token，带当前请求所属客人模块 X-Module-Code
//   与当前综合体 X-Project-Id（页面所选或链接带的综合体；都没有时只有一个综合体就用它）
//   （PGM「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」：后端终端开关按这几层判关）。
// - 令牌只放内存与本小程序本地存储的 customer_token 一项，只经请求头发出，不进页面路径、分享内容与日志；
//   任何请求 401 即清掉令牌回登录页，403 权限不足不登出；登出即删；恢复会话时断网或服务端异常不删。动态码等其余内容一律不写存储。
// - 终端开关（PGM「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）：读本端已关闭的开关行，本端总开关、本版本被关或拉取失败
//   → 本端暂停服务；某模块被关 → 只隐藏该模块入口；只有一个综合体且它被关 → 本端暂停服务；选综合体的页面不列被关的综合体，
//   链接带的综合体被关该页只提示暂停服务。
const core = require('../core/index')
const config = require('../config')

const TOKEN_KEY = 'customer_token'
let token
let maskedPhone = null
let currentModule = null
let currentProject = null
// 只有一个综合体时的那个综合体（多个时为空，以页面所属综合体为准）；读到一次即不再读。
let onlyProject = null
let projectsLoaded = false
// 本端已关闭的开关行（null＝拉取失败）。
let switchRows = []
let switches = { paused: false, hiddenModules: new Set() }

function getToken() {
  if (token === undefined) token = wx.getStorageSync(TOKEN_KEY) || null
  return token
}

/** 文字按 UTF-8 编成字节（小程序运行环境不一定有 TextEncoder）。 */
function utf8(text) {
  const binary = unescape(encodeURIComponent(text))
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i)
  return bytes
}

/**
 * 带文件的提交（投诉附件、学生证件照片，OpenAPI 为 multipart/form-data）：字段与全部文件组成一个表单，随同一个请求编号一次发出。
 * 不用 wx.uploadFile：它一次只能传一个文件。文件从小程序本地临时路径读出，只用于本次上传、不另存。
 */
function multipart(form) {
  const boundary = `----complex${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
  const fs = wx.getFileSystemManager()
  const parts = []
  for (const key of Object.keys(form.fields)) {
    parts.push(utf8(`--${boundary}\r\nContent-Disposition: form-data; name="${key}"\r\n\r\n${form.fields[key]}\r\n`))
  }
  for (const item of form.files) {
    parts.push(utf8(`--${boundary}\r\nContent-Disposition: form-data; name="${item.field}"; filename="${item.fileName}"\r\n`
      + 'Content-Type: application/octet-stream\r\n\r\n'))
    parts.push(new Uint8Array(fs.readFileSync(item.file.path)))
    parts.push(utf8('\r\n'))
  }
  parts.push(utf8(`--${boundary}--\r\n`))
  const body = new Uint8Array(parts.reduce((sum, part) => sum + part.length, 0))
  let offset = 0
  for (const part of parts) {
    body.set(part, offset)
    offset += part.length
  }
  return { contentType: `multipart/form-data; boundary=${boundary}`, body: body.buffer }
}

function transport(request) {
  let header = request.headers
  let data = request.body
  if (request.form) {
    const form = multipart(request.form)
    header = Object.assign({}, header, { 'content-type': form.contentType })
    data = form.body
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: request.url,
      method: request.method,
      header,
      data,
      dataType: 'text',
      responseType: 'text',
      success: (res) => resolve({ status: res.statusCode, text: typeof res.data === 'string' ? res.data : JSON.stringify(res.data) }),
      // 网络中断、超时：结果未明（core 留着请求编号，再次提交沿用，不会重复扣款）。
      fail: () => reject(new Error('网络连接失败，请检查网络后重试')),
    })
  })
}

function createService(withSession) {
  return new core.CustomerService(new core.CustomerApi({
    baseUrl: config.apiBaseUrl,
    terminalType: 'miniapp',
    clientVersion: config.clientVersion,
    transport,
    getToken: withSession ? getToken : () => null,
    getContext: withSession ? () => ({ moduleCode: currentModule, projectId: currentProject }) : () => ({}),
  }))
}

const sessionService = createService(true)
// 拉终端开关不带令牌与模块（某模块被关时仍要能拉到开关、只隐藏那个模块）。
const switchService = createService(false)

function clearSession() {
  token = null
  maskedPhone = null
  wx.removeStorageSync(TOKEN_KEY)
}

core.setCustomerSessionExpiredHandler(() => {
  clearSession()
  wx.reLaunch({ url: '/pages/login/index?expired=1' })
})

module.exports = {
  /** 取客人服务并记下这次请求所属的客人模块（首页、登录等不属任何模块的传 null）与综合体（不传＝只有一个综合体时的那个）。 */
  service(moduleCode, projectId) {
    currentModule = moduleCode || null
    currentProject = projectId || onlyProject
    return sessionService
  },
  hasSession: () => Boolean(getToken()),
  maskedPhone: () => maskedPhone,
  saveSession(result) {
    token = result.customerToken
    maskedPhone = result.maskedPhone
    wx.setStorageSync(TOKEN_KEY, token)
    core.resetCustomerSessionExpiredNotice()
  },
  clearSession,
  /** 启动时凭本地令牌恢复会话；服务端判令牌已失效（401）即清掉，断网、超时或服务端异常不算失效、令牌留着。 */
  async restoreSession() {
    if (!getToken()) return null
    let info
    try {
      info = await sessionService.restoreSession()
    } catch (err) {
      return null
    }
    if (info) maskedPhone = info.maskedPhone
    else clearSession()
    return info
  },
  /** 登出：先带令牌撤销服务端会话（失败不阻断），再清本端令牌。 */
  async logout() {
    await sessionService.logout()
    clearSession()
  },
  async loadSwitches() {
    if (!projectsLoaded) {
      try {
        const projects = (await switchService.listProjects()) || []
        onlyProject = projects.length === 1 ? projects[0].projectId : null
        projectsLoaded = true
      } catch (err) {
        // 读不到综合体列表：下次回前台再读。
      }
    }
    try {
      switchRows = (await switchService.listEffectiveTerminalSwitches()) || []
    } catch (err) {
      switchRows = null
    }
    switches = core.customerSwitchState(switchRows, { version: config.clientVersion, projectId: onlyProject })
    return switches
  },
  moduleHidden: (moduleCode) => switches.hiddenModules.has(moduleCode),
  /** 该综合体的客人端是否开着。 */
  projectOpen: (projectId) => !switchRows || !core.customerSwitchState(switchRows, { version: config.clientVersion, projectId }).paused,
  /** 页面进入时的状态：是否已登录、所属模块是否被终端开关关闭（被关的页面只提示暂停服务、不发业务请求）。 */
  pageState: (moduleCode) => ({ loggedIn: Boolean(getToken()), paused: switches.hiddenModules.has(moduleCode) }),
  /** 未登录时去登录页（登录成功返回原页面，原页面 onShow 重新加载）。 */
  toLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },
}
