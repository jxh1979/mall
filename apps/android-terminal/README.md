# Android 终端 P0

文件版本：V0.01

工作手机考勤：领取前选择本班并校准时间；NFC、扫码和离充电事件先以固定编号持久保存，重试不改原事件。实际时刻须匹配服务器时间锚、同次启动和领用班次；无法核实的事件上传后显示待复核，不用登录或上传时间替代。领取页可查看服务端事件及重试未传队列。本班结束由获授权人员确认工作和收尾完成、实际收到手机后办理归还；故障换机由有效夜班值班经理办理，旧机归还、新机领用沿原班，不能重复记上班或提前结束原班。

验证：共享 Kotlin 核心含持久领取队列重启、网络失败重试、重复确认和启动标识测试；APK 编译仅证明工程可构建，真实 NFC/电源断开、设备管理与离线归还现场链仍需真机验收。

本工程先实现 Android 终端可复用的 Kotlin 业务核心和 160 联调入口。

P0 已覆盖：

- 员工登录
- 设备绑定和点位绑定
- 在线扫码预览与确认核销
- 离线权益包加载
- 离线核销本地队列
- 人工补录本地队列
- 待同步任务回传
- 有待同步任务时归还拦截
- 终端操作事件批量上报

当前构建方式：

```bash
npm run verify:android-env
npm run android:p0:build
npm run android:p0:smoke
npm run android:p0:apk
```

说明：`android:p0:build` 与 `android:p0:smoke` 验证 Kotlin 终端核心；`android:p0:smoke` 会自动打包并启动一个临时 P0 后端，默认使用 `http://127.0.0.1:18081` 和测试管理员 `admin/admin-pass`，烟测结束后自动停止，不依赖人工预先启动后端，也不得使用真实生产管理员密码做 smoke；`android:p0:apk` 验证 APK 壳工程，并要求 Android SDK 可访问。根脚本会优先使用 Gradle Wrapper；若项目目录没有 Wrapper，则使用系统 Gradle。160 或本机未完成 Java 21、Gradle/Wrapper、Android SDK 准备前，不得把 Android P0 构建、smoke 或 APK 验收标记为完成。

APK 壳工程健康检查默认连接 `https://192.168.88.160/api/v1/system/health`。如需覆盖地址，使用 `COMPLEX_ANDROID_API_BASE_URL`；同时必须把 host 写入 `COMPLEX_ANDROID_API_ALLOWED_HOSTS`，默认允许 `192.168.88.160,localhost,127.0.0.1,::1`。非本地 smoke 地址必须使用 HTTPS，防止构建变量误配到非授权目标。

如需复用外部已启动的测试后端，可显式指定：

```bash
ANDROID_P0_SMOKE_USE_EXISTING_BACKEND=1 P0_API_BASE_URL=http://127.0.0.1:8080 P0_ADMIN_USERNAME=<seeded-admin> P0_ADMIN_PASSWORD=<seeded-password> npm run android:p0:smoke
```

160 已通过完整 `complex-verify-p0.sh`。当前后端用于 Android smoke 的业务接口是 `local/dev/test` profile 下的内存态 P0 smoke 切片，只用于自动化验收和端侧流程保活；正式业务实现仍需迁移到 PostgreSQL + Flyway + Service/Repository + 权限/事件/审计。
工作手机领取事件按真实设备领用上下文隔离，换人后不沿用旧时间锚。待传队列逐项同步并保留失败原件；缺原领用编号的事件不自动套用当前持有人。主管可在原验签工作手机的“本机待传事件与主管代补报”页面，查询指定原员工的真实领用历史并明确选择，补充依据后只提交待核事项；仍须经办主管真实上一级核实关联与时间。客户端不改写原事件，也不把代补报当成已出勤。

实际捕获不把持久缓存的员工/领用编号当作当前绑定：外部归还改绑后先拔线再登录也保存为原关联待核。已有缓存锚仅是线索；服务端证明同设备、同次领用、同启动、实际时刻及班次有效时仍自动核时，含同次领用的离线补传；无锚或证明不成立的原件走现主管补证。退出清锚和领用缓存，保留待传原件。
