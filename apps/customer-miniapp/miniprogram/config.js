// 文件版本：V0.01
// 小程序运行配置（唯一来源）。下列为开发值：接口与客人链接主机都指 160（自签证书，只能在开发者工具里关掉合法域名校验调试）。
// 正式接口域名、客人链接域名属外部资料，到位后改这里；上传前 npm run check-release 拒绝开发值（http、私网或本机地址、游客 AppID、
// 本端版本与 package.json 不一致）。H5 支付不在小程序里打开（不用 web-view），支付只认后端签发的小程序微信支付参数。
module.exports = {
  apiBaseUrl: 'https://192.168.88.160',
  // 客人链接主机：分享、扫码与「扫普通链接二维码打开小程序」共用的 https://<主机>/c/… 链接。
  linkHost: '192.168.88.160',
  // 本端版本：随每个请求带 X-Client-Version（终端开关可按版本关闭），须与 package.json 的 version 一致。
  clientVersion: '0.1.0',
}
