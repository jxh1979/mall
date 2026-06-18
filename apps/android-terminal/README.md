# Android 终端 P0

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