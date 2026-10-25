// 文件版本：V0.01
// 选照片、视频（投诉附件、学生证件照片）：wx.chooseMedia 拍摄或从相册选，拿到的是小程序本地临时文件；文件名取临时路径末段（带扩展名，
// 服务端按扩展名判定照片、视频、语音）。语音用 wx.getRecorderManager 录 mp3（页面自己开始、停止）。只交出临时路径，上传时由 lib/api.js 读出。

/** 临时路径 → 上传用的文件（path 供读取，name 作表单文件名）。 */
function fileOf(tempFilePath) {
  return { path: tempFilePath, name: tempFilePath.slice(tempFilePath.lastIndexOf('/') + 1) }
}

/** kind：'image' | 'video'；count：本次最多选几个。取消或失败返回空数组。 */
function chooseMedia(kind, count) {
  return new Promise((resolve) => {
    wx.chooseMedia({
      count,
      mediaType: [kind],
      sourceType: ['album', 'camera'],
      maxDuration: 60,
      success: (res) => resolve((res.tempFiles || []).map((file) => fileOf(file.tempFilePath))),
      fail: () => resolve([]),
    })
  })
}

module.exports = { fileOf, chooseMedia }
