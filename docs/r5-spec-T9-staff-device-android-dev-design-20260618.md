# T9 员工设备凭证 Android 端 可编码开发规格 V0.13（2026-06-18）

> **任务**：R5 backlog **T9**，W1，端侧。**依赖 T1（后端员工三表与端点已就绪）**。
> **裁决依据**：裁决⑨——员工三表 + Android 端凭证（注册/Keystore 指纹/Root 检测/换机解绑重绑）本期 P0；MDM 阻断按第三方原则预留 + 开关，自有侧字段二轮裁决**暂缓**。
> **最初基准**：`apps/android-terminal/src/main/kotlin/com/complex/terminal/` 有 `TerminalApiClient`/`TerminalModels`/`TerminalWorkflow`/`InMemoryLocalStore` 核心模块 + `android-app/.../MainActivity.kt`（仅健康检查 Activity，无注册/指纹/Root/换机）。
> **最初迁移范围**：复用 T1 的 `staff_profiles`/`staff_device_credentials`/`employee_notices`(V066) 与其端点；本任务为 Kotlin/Android 端实现。

---

## 1. 端侧能力（私人手机 App，仅注册/通知/状态，不进现场业务）
1. **注册**：公司员工在私人手机 App（终端类型 `staff_private_phone`，安装时由 Keystore 生成设备密钥）本人填写资料并选择公司，调用 `POST /api/v1/staff/self-registrations`，请求携带该手机的设备公钥与指纹并随申请绑定本人；工作手机应用不提供自助注册入口。商户代员工提交调用 `/staff/registrations`。合作方式为「自营」的商户视同公司自有员工。已登录后本人通知使用 `GET /api/v1/staff/notices`。
2. **设备凭证生成与绑定**：
   - 用 Android **Keystore** 生成设备密钥对（优先 StrongBox/TEE，`setIsStrongBoxBacked`，回退 TEE）；私钥不可导出。
   - 生成**不可逆设备指纹哈希**（设备稳定标识 + Keystore 公钥摘要的单向哈希，**不读手机本机 NFC 唯一标识**，D8）。
   - `POST /api/v1/staff/device-credentials` 提交 `device_public_key` + `device_fingerprint_hash` → 后端校验本人归属，首次直接 `active`；换机或异常后重新绑定为 `pending_review`。
3. **完整性检测**：启动/关键操作前检测 Root/调试/异常环境（Play Integrity API 或等效 + 本地 root 检测）；异常 → `POST /staff/device-credentials/{id}/report-integrity(status=root_detected|debug_detected|abnormal)` → 后端自动 revoke + 通知。
4. **换机**：旧机解绑（或新机绑定触发 supersedes 链）→ 新机 `POST /staff/device-credentials`（pending_review，supersedes=旧 active）→ 后台审批后新 active、旧 superseded。App 引导"先解绑旧再绑新"。
5. **通知/同意**：拉取 `employee_notices`，合规同意项 `POST /staff/notices/{id}/acknowledge`。

## 2. 实现（复用既有模块）
- `TerminalApiClient`：新增 staff 注册/凭证/通知端点调用方法。
- `TerminalModels`：新增 StaffRegistration/DeviceCredential/Notice DTO（与后端 OpenAPI 对齐）。
- 复用 `DeviceKeystore` 生成公钥、指纹和请求签名；不新增重复的密钥管理抽象。
- `MainActivity` 扩为注册/绑定/状态界面（替换纯健康检查）。
- 请求鉴权：凭证绑定后，端侧用 Keystore 私钥对请求挑战签名，后端用 `device_public_key` 验签（与后端 `report-integrity`/敏感操作配套；本期最小实现可先做绑定+状态）。

### M010 工作手机领取与签验

公司工作手机沿用 `terminal_devices.staff_id` 对应员工，重新登记保持该对应关系。设备管理员在本机登记设备编号、公钥；编号保存在本机配置，员工后续登录直接使用，不要求每次重复绑定。公司工作手机公钥存入 `terminal_device_signing_keys`，与私人手机凭证分开。

两个现有客户端共用 `DeviceRequestHeaders`：设备编号、毫秒时间戳、随机请求号、方法、路径和正文摘要由本机私钥签名。后台同时核对设备状态和员工归属。只声明终端类型不足以打卡。

打卡页使用实际扫码或 NFC 回调，不提供手工录入打卡。应用运行且员工已登录时，系统充电断开事件会提交领取记录；三种领取方式按服务器最早已接收记录去重。后台拒绝私人手机、未验证设备和其他员工的工作手机。实机 NFC、Keystore、充电设施触发及应用进程退出后的硬件上报尚未验证，不能以 JVM 测试或安装包构建代替实机验收。柜机扫码和开锁的厂商接口接入同样须等待真实资料。

主管手机排班提交后显示待审和审批编号，批准前不显示为已发布。

## 3. MDM（预留 + 开关，本期暂缓自有侧）
- 不落 MDM 自有侧字段（二轮裁决）；仅保留 MDM 纳管结果消费接口占位 + 后台开关（`integration.mdm.*`，T2 门控）。受管工作手机的白名单/网站拦截由外部商业 MDM + Android Enterprise/AMAPI 负责。

## 4. 验收（GWT）
- Given 新安装 App，When 注册并绑定，Then 生成 Keystore 密钥对 + 指纹哈希，本人首次绑定后端凭证直接 active；不另设人工审批。
- Given Root 设备，When 启动检测，Then 上报 root_detected，后端凭证 revoked + 发通知，App 进失效复核态。
- Given 换机，When 新机绑定，Then 走 pending_review + supersedes，审批后旧 superseded、新 active（唯一 active 成立）。
- Given 私人手机，When 尝试现场业务操作，Then App 不提供入口（仅注册/通知/状态）。

## 5. 测试 / 依赖
- 端侧单测：DTO 序列化（沿用 `TerminalRequestSerializationTest` 模式）、指纹哈希稳定性、Root 检测分支、换机流程状态。
- 依赖：**T1**（后端三表 + 端点）；Play Integrity（或等效）；不依赖 MDM 落地。
- 注意：真机 Keystore/StrongBox 与 Play Integrity 需设备/集成环境验证，CI 用 fake/mock。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T9 端侧规格（Keystore 设备凭证+指纹哈希+Root 检测+换机 supersedes+仅注册通知状态；MDM 预留暂缓） | R5 backlog T9；裁决⑨+二轮；端侧现状取自 apps/android-terminal |
