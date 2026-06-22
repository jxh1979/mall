# 第一轮 待后续供应商资料补齐清单 R1（2026-06-18）

> 定位：把"真实供应商/外部资料未到位"的事项汇成单一可执行清单，明确**当前允许做什么 / 禁止做什么 / 解锁需要什么资料 / 责任人**。供应商未确定前一律保留接口边界 + mock + 字段映射占位。
> 整合源：`external-dependency-gates-v0.1.md`、`production-blocker-register-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`、`integration-capability-switches-v0.1.md`、`production-readiness-evidence/**` owner 包。
> 机器标记：R1_PENDING_SUPPLIER_BACKFILL_READY、MOCK_NOT_PRODUCTION_EVIDENCE。

## 1. 供应商/外部依赖资料清单（按能力）

| # | 事项 | 状态 | 允许做 | 禁止做 | 解锁所需资料 | 责任人 |
|---|---|---|---|---|---|---|
| S1 | 聚合支付（aggregate_payment）| switch_off / pending_info | 抽象接口、mock 支付、对账口径 | 真实收款/退款/生产对账 | 商户号、密钥引用、回调验签、退款权限、对账文件 | 财务/IT/采购 |
| S2 | 团购/渠道售票（美团/点评/抖音）| switch_off | 渠道订单回流 mock、后台补录 | 真实券码核销/订单回流/退款同步 | 渠道 API/SDK、券码核销、对账口径、测试账号 | 运营/采购 |
| S3 | 企业微信（wecom）| switch_off / pending_info | 基础同步占位、规则质检、失败告警设计 | AI 自动处罚/自动绩效 | 应用配置、通讯录/会话存档权限、回调、同步证据 | IT/运营/法务 |
| S4 | 酒吧人脸/年龄核验（bar_face_age）| switch_off / pending_info | mock、接口占位、失败阻断逻辑 | 生产仅靠 mock 或口头确认标记通过 | 供应商 SDK/API、合规材料、测试账号、错误码 | 采购/法务/酒吧 |
| S5 | MDM/Android Enterprise（managed_work_devices）| switch_off | 字段映射占位、设备能力字典 | 未纳管设备作正式工作手机/平板生产准入 | MDM 选型、enroll、策略下发、远程锁定/擦除测试、设备绑定证据 | IT |
| S6 | 对象存储/证据桶（object_storage）| switch_off | 字段映射占位、外部证据编号/手工 link | 平台内文件上传/签名 URL 能力 | 桶、Access Key 引用、上传下载、签名 URL、备份轮换证据 | IT/运维 |
| S7 | 开放平台/第三方应用接入（open_platform）| switch_off | 内部 adapter/mock | 真实 app_key/secret、动态 endpoint、入站 webhook | 开放应用、scopes、签名密钥、nonce、防重放、验签、轮换证据 | IT/安全 |
| S8 | 餐饮 POS / 零售 POS | switch_off | 后台手工登记 | 真实点单/收银/券核销/退款/回流/对账 | POS API/SDK、字段映射、Webhook/轮询、对账文件 | 采购/运营 |
| S9 | 机器人平台（robot_platform）| blocked | 字段映射、mock、签名样例、只读分析 | 下发真实任务/调度/移动/远程控制 | API/SDK、签名、回调、错误码、沙箱、额度、验收证据 | 采购/IT |
| S10 | 视频监控（video_platform）| blocked | mock 链接、证据字段、权限设计 | 真实播放/片段截取/自动证据绑定 | 播放鉴权、录像索引、保留期限、调阅审计协议 | 采购/IT/安全 |
| S11 | 设备厂商/NFC/闸机（gate_nfc_scan_device）| blocked | 设备能力字典、mock 状态、接口占位 | 开闸/锁定/放行/离线高风险核销 | 硬件型号、SDK、签名、网络条件、失败回退 | 采购/IT |
| S12 | **短信/通知（sms_notification）** | **switch_off / 占位已建未并入主矩阵** | mock OTP（loopback）、催缴/告警降级 manual | 生产用固定 mock 验证码；静默丢弃催缴/告警 | 供应商 mobile/templateId/params/signName 映射、回执回调、错误码 | IT/运营 |
| S13 | 独立 Python AI 服务（T3-D2）| blocked | Java 规则、160 本地 loopback LLM 只读建议 | 替换生产路径、AI 直接影响业务决策 | 模型方案、样本集、SLA、部署监控、安全审查、数据合规 | 技术负责人/算法/安全 |

## 2. 生产前置（非供应商，但属上线阻断输入）

| item_id | 类别 | 状态 | 责任人 | 下一步 |
|---|---|---|---|---|
| PRD-NAS-AUTOMOUNT-001 | NAS | pending_info | IT/运维 | 回填重启后 findmnt + 读写删除测试 + 截图 |
| PRD-NAS-SCHEDULE-001 | NAS | pending_info | IT/运维 | 回填调度配置 + 最近一次 scheduled 备份 + .sha256 |
| PRD-NAS-ALERT-001 | NAS | pending_info | IT/运维/安全 | 回填容量/失败告警 + 通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | NAS | pending_info | IT/运维 | 回填恢复演练报告 + 临时库清理 + 验证表 |
| PRD-CRED-001 / -OPS / -DB / -JWT / -NAS-001 | 凭证 | pending_info | IT/安全/财务 | 密钥引用、轮换周期、责任人、应急回滚；真实密码写入受控保管位置（不入文档/聊天/仓库）|
| PRD-160-001 | ops160 | pending_info | IT/运维 | 用户级覆盖迁移到系统配置 + systemd + 告警 |
| PRD-HTTPS-001 | 网络 | not_started | IT | 公开访问启动后配置 |
| SEC-P0-OPS-RECOVERY-001 | 安全 | open | IT/安全/运维 | 开机自动挂载/定时备份/告警/周期演练 + 凭证台账 |
| SEC-P2-CSRF-API-001 | 安全 | open_for_prod_review | 后端/前端/安全 | 认证方案定稿时按 Bearer/SameSite/双提交复核 |

## 3. 本轮新增登记项（R1）
- **ISO-GAP-01（P0，安全/工程）**：已关闭（2026-06-21）。`SEC-P0-TENANT-ISOLATION-001` 已由安全基线生成器写入 `security-scans/findings-register-v1.md` 与 `formal-security-baseline-v1.md`，`verify:tenant-isolation` 已通过。责任人后续维护 scope CSV / Repository scoped 通道 / 闸口。
- **R1-FINDING-A（P1，文档治理）**：已关闭（2026-06-21）。数据隔离规格已新增 §3A 自动迁移隔离基线，并提供 `docs:data-isolation-spec` / `verify:data-isolation-spec`，后续由技术负责人维护脚本闸口而非手抄迁移计数。
- **S12 落地（P0，登录链路）**：`sms_notification` 开关并入主矩阵 + `-latest.json` + supplier-mock 样例。责任人：IT。

## 4. 解锁与回填纪律
1. 责任人补齐证据后，先更新生产前置台账或 findings register，再 `npm run docs:production-blockers`、`verify:production-blockers`、`verify:delivery-gates`。
2. **不得**手工删除阻断项；必须从来源台账或 finding 关闭。
3. **不得**把 mock、口头确认、截图占位、未纳管设备当作生产证据。
4. 供应商资料到位 → 先建专项计划 + 字段映射 → 再补 OpenAPI/权限/事件/验签/失败回退/测试 → 最后改开关与准入门。
