# 开发文档完整性、业务逻辑可用性与验收可执行性对抗式审计 V0.1

> ⚠️ 已被 `development-documentation-audit-v0.2.md`（2026-06-18）取代。本文 operation 数（198）、迁移引用（V041）等已对当前代码态（207 ops / V052 / 673 测试）滞后；仅作历史参考。

> 审计日期：2026-06-16
> 审计范围：160 机器 `/home/james/complex-management` 当前工作区中的 PRD、SRS、开发设计、接口文档、OpenAPI、数据库/迁移设计、业务流程、用户故事、验收标准、Runbook、README、任务清单、生产前置证据与审计报告。
> 审计方法：documentation-specialist + PM Critic 反向质疑 + acceptance criteria + edge cases + user story map + gstack spec / eng review / ceo review。目标不是证明文档“写过”，而是判断产品、开发、测试、运营、供应商和交付负责人是否能据此执行。

## 1. 总体结论

| 维度 | 评分/结论 | 证据 |
|---|---:|---|
| 文档完整性 | 88/100 | 已有当前入口、统一基线、需求追溯矩阵、OpenAPI、operation 验收矩阵、P0/P1/P2/T3 编码与验收审计、Runbook、生产前置证据模板；全量文档地图已生成 `development-documentation-audit-map-v0.1.csv`，覆盖 297 份开发相关文档。 |
| 业务逻辑可运行性 | 85/100 | P0/P1/P2/T3 主流程、权限、事件、状态、接口、终端和验收大体闭环；票务动态定价旧口径已修正为“草稿只模拟、发布规则命中正式订单并留价格轨迹”。 |
| 验收可执行性 | 90/100 | `npm run verify:delivery-gates` 已通过，覆盖 OpenAPI 契约、示例、operation matrix、closure appendix、Web/API 路由、正式安全基线、生产前置证据模板、外部依赖闸口等。 |
| 是否可继续开发 | 可以 | 自有系统、内网联调、产品/测试验收可以继续推进。 |
| 是否可公开生产上线 | 不可以 | 生产账号/密钥/第三方真实供应商/生产证据仍需按 blocker register 和 owner action packs 回填；文档已经定义阻断口径，但外部证据未全部到位。 |

## 2. 文档地图

全量文档地图文件：`docs/development-documentation-audit-map-v0.1.csv`。

| 文档桶 | 数量 | 当前状态判断 |
|---|---:|---|
| archive | 45 | 只作历史追溯，不作为开发入口。 |
| delivery-contract | 52 | 当前交付、验收、OpenAPI、operation 矩阵和 closure 的核心依据。 |
| design-spec | 22 | 支撑模块级开发设计，局部需继续拆 full Web 后台 SRS。 |
| index-governance | 33 | 入口、基线、追溯、治理、口径统一。 |
| machine-contract | 2 | OpenAPI YAML/JSON，当前机器契约核心。 |
| ops-readiness | 16 | MDM、NAS、凭证轮换、160 运维、上线前置。 |
| plans-reviews | 52 | 编码计划、审计、差异与交付判断，多数为阶段性证据。 |
| prd | 14 | 产品总纲、模块 PRD、业务系统边界。 |
| production-evidence | 61 | 生产前置证据模板、回填清单、责任人动作包。 |

主引用链：

`00-current-effective-document-entry-v0.1.md` -> `unified-standards-baseline-v0.1.md` / `business-decision-log-v0.1.md` -> PRD/开发设计 -> `openapi/openapi-p0.*` + operation matrix + closure appendix -> 用户故事/任务/API/权限/事件映射 -> 验收与边界用例 -> Runbook / blocker register / delivery gates。

## 3. 业务拆解

| 模块 | 角色 | 核心对象 | 主状态/动作 | 关键风险 |
|---|---|---|---|---|
| 平台治理/主体绑定 | 总控、平台运营、财务、法务、商户管理员 | 经营主体、运营主体、合同主体、结算主体、合同、附件 | 建档、绑定、审批、归档、变更留痕 | 主体错绑会导致合同、财务、权限和报表错账。 |
| 票务/权益/动态定价 | 票务运营、渠道、检票员、游客 | 票种、日历价、多人/新人/学生/渠道价、订单、核销、权益 | 配价、售票、支付、核销、退改、动态规则发布 | 旧文档曾残留“动态定价不影响正式订单”的表述，已修正。 |
| 酒吧/卡座/订单/存酒 | 酒吧经理、服务员、销售人员、客户、核销员 | 卡座、订台、入场名额、订单、低消、酒水、存酒、提成 | 订台、入场、点单、支付、核销、存取、转卡/合并/续卡 | 年龄人脸、卡座入场范围、低消和通票边界必须强约束。 |
| 游乐/安全 | 游乐管理员、巡检员、主管、游客 | 设施、巡检项、整改、事故、免责、预约 | 开放前巡检、阻断、整改、复检、恢复 | 安全闭环不能只写告知，必须有阻断和恢复权限。 |
| 库存/供应链 | 仓管、采购、财务、门店 | SKU、库存、批次、出入库、盘点、供应商 | 入库、扣减、盘点、调整、对账 | P0 是否强制库存扣减已收口；高价值酒水保持在线核销。 |
| 财务/应收应付 | 财务、运营、商户、平台 | AR/AP 台账、结算单、对账差异、账龄 | 应收、应付、结算、冲销、核对 | P0 必须完整台账，但生产支付/税务对接仍需凭证和口径。 |
| 第三方接入/开放平台 | 平台技术、供应商、运营 | Adapter、Webhook、轮询、签名、字段映射、补偿任务 | 双向接入、回调、校验、对账、异常补偿 | 真实 SDK/API 未到位前只能保留接口边界与 mock，不得伪实现。 |
| 终端/Web/Android/一体机 | 员工、平板、一体机、Web 管理员、游客 | 终端身份、设备状态、登录会话、页面/路由 | 登录、操作、离线摘要、扫码/核销、售票 | Web 当前是轻量管理入口，不等于完整后台管理系统。 |
| 生产运维/证据 | IT、运维、安全、财务、供应商 | NAS、凭证、JWT/第三方密钥、备份、恢复演练、告警 | 台账、到期提醒、轮换、备份、恢复、审计 | 文档模板齐全，但真实生产证据仍需回填。 |

## 4. 开发文档审计矩阵

| 文档/章节 | 业务模块 | 需求点 | 当前描述 | 依赖对象 | 关联接口/页面/数据表 | 验收标准 | 缺口 | 风险 |
|---|---|---|---|---|---|---|---|---|
| `00-current-effective-document-entry-v0.1.md` | 全局 | 当前有效入口 | 已定义第一阅读入口、废止内容、角色阅读包、生产前置 | 全部文档 | 全部模块 | 能从入口定位有效口径 | 已补入本审计、全量地图和规格草案入口 | P1 |
| `unified-standards-baseline-v0.1.md` | 全局 | 单一权威口径 D1-D13 | 金额分、终端枚举、权限模板、Java/Spring/PostgreSQL/Flyway、CNY、销售提成等已明确 | 需求追溯、OpenAPI、实现 | 全部接口/表 | 冲突以 D1-D13 为准 | 需持续禁止旧文档反向打开已收口事项 | P2 |
| `requirement-traceability-matrix-v0.1.md` | 全局 | REQ-01~REQ-92 追溯 | 已覆盖绝大多数用户原始需求与后续决策 | 原始需求、业务决策 | 全部模块 | 需求可追到文档 | 代码实现覆盖需继续由代码验收矩阵维护 | P2 |
| `openapi/openapi-p0.yaml/json` | 接口契约 | 机器可读 API | 198 operations，权限/认证上下文基本闭合 | 后端、Web、Android、供应商 mock | `/api/**` | `verify:p0-contract` 通过 | 3 个 open/platform endpoint 无 terminalAllowed，按开放端点可接受 | P2 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | 接口/测试 | operation 级验收 | 已绑定权限、终端、事件、示例和任务 | OpenAPI | 全部 API | matrix 与 OpenAPI 同步 | 后续新增接口必须先改 OpenAPI 再生成矩阵 | P1 |
| `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | P0 主流程 | 端到端验收 | 覆盖票务、酒吧、库存、财务、第三方、游乐 | 用户故事、OpenAPI | 核心页面/接口 | Given/When/Then 可执行 | 需与真实 UI 自动化持续绑定 | P2 |
| `p0-user-story-map-and-sprint-slices-v0.1.md` | 用户故事 | MVP/P1/P2 分层 | 已拆角色、任务、切片和验收 | PRD、任务清单 | 全部模块 | 开发可按 slice 推进 | 未来版本切片需避免进入当前 P0/P1 scope | P2 |
| `ui-automation-acceptance-report-v0.1.md` | Web/终端 | UI 自动化验收 | 已覆盖登录、权限、核心页面、窄屏、真实后端抽样 | Playwright、Web 路由 | Web 管理端 | 有真实断言和截图/视觉检查 | 完整后台管理系统仍需独立 SRS 和页面矩阵 | P1 |
| `p2-ticket-dynamic-pricing-*` | 票务 | 动态定价 | 发布规则影响正式订单并留价格轨迹；草稿仅模拟 | TicketingService、pricing 子服务、V041 | 票务 API/Web | 草稿/发布/权限/价格轨迹测试 | 旧冲突表述已修正 | 已修 |
| `bar-*` 相关文档 | 酒吧 | 卡座、低消、入场、提成、存酒 | 已明确通票与卡座低消无关；卡座允许规定人数入所在酒吧；销售卡座 15%/非销售 10% | Bar 服务、权限、订单 | 酒吧 API/Web | 入场、订单、结算、提成、留痕 | 年龄强制人脸依赖第三方采购，需供应商资料到位 | P1 |
| `amusement-p0-safety-inspection-spec-v0.1.md` | 游乐 | 安全闭环 | 开放前巡检、阻断、整改复检、主管恢复 | 设施、巡检、事故 | 游乐 API/终端 | 未过巡检不得开放 | 电子免责/事故工单后续 P1/P2 扩展 | P2 |
| `third-party-supplier-integration-kit-v0.1.md` | 第三方 | 双向开放与接入 | Adapter、Webhook+轮询校验、字段映射、补偿策略已定义 | 供应商 SDK/API | `/api/integration/**`、开放接口 | mock 可验，真实联调需资料 | 真实 SDK/API 未到位前不可进入生产级实现 | P1 |
| `production-blocker-register-v0.1.md` | 运维/上线 | 生产阻断 | 已列生产账号、密钥、对象存储、数据库、MDM、NAS、供应商证据 | IT/采购/财务/供应商 | Runbook/证据包 | blocker 关闭后复跑交付闸口 | 证据仍需责任人回填 | P1 |
| `mdm-*` / `production-credential-*` / `nas-*` | 生产前置 | 设备管控、凭证轮换、备份恢复 | 已按低管理成本方案和 NAS 方案形成 Runbook | IT、NAS、SMB、credential_ref | 运维脚本/证据模板 | 证据模板和闸口通过 | 真实恢复演练、凭证轮换截图/日志待回填 | P1 |
| `_archive/**` | 历史 | 归档 | 已标历史，不作为当前入口 | 无 | 无 | 不参与开发 | 需避免被外部智能体误读为当前事实 | P3 |

## 5. 对抗式发现

### P0

当前未发现会导致开发方向立即错误的开放 P0 文档缺陷。原因是：旧 TypeScript/SQLite 路线已被入口和闸口阻断；正式技术栈、OpenAPI、权限、终端、验收和生产阻断均有明确入口；动态定价旧冲突已在本次审计中修正。

### P1

| 编号 | 问题 | 证据 | 后果 | 修复建议 | 状态 |
|---|---|---|---|---|---|
| DOC-P1-01 | 动态定价旧口径残留 | `p2-current-diff-adversarial-review-v0.1.md`、入口依赖说明、P2 计划曾写“只模拟/不影响订单” | 开发会误以为发布规则不进入正式订单 | 已统一为“草稿仅模拟；发布规则命中正式订单并记录价格轨迹” | 已修复 |
| DOC-P1-02 | 完整 Web 后台管理端仍未拆成独立 SRS | `ui-automation-acceptance-report-v0.1.md` 明确当前 Web 是轻量入口 | 若直接要求“完整后台”，开发会缺页面、权限、字段、空/错态标准 | 已新增 `full-admin-srs-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`full-admin-ui-acceptance-matrix-v0.1.md`，下一步按矩阵拆开发批次 | 已修复 |
| DOC-P1-03 | “编码完成”与“可上线”容易被混读 | gstack/P2 完成审计与 production blocker register 并存 | 管理层可能误判可以上线 | 入口必须继续强调：可开发/联调，不可生产上线 | 已收口，需持续维护 |
| DOC-P1-04 | 真实供应商联调资料未到位 | external dependency gates、third-party kit | 伪实现机器人/视频/设备/支付/人脸会导致后续返工和安全风险 | 保持 adapter/mock/字段映射模板，等 SDK/API 后补齐 | 待外部资料 |

### P2

| 编号 | 问题 | 风险 | 修复建议 |
|---|---|---|---|
| DOC-P2-01 | 文档规模过大，297 份中只有 109 份被入口直接引用 | 外部接手者仍可能迷路 | 保留文档地图 CSV，入口引用 S0/S1/S2/S3 分层，后续把阶段审计合并成 overview。 |
| DOC-P2-02 | 生产证据模板齐全但真实证据未完全回填 | 验收时可能把模板当证据 | blocker register 和 evidence intake 必须区分 template / submitted / verified。 |
| DOC-P2-03 | P2/T3 计划、完成审计和 gstack 报告数量多 | 后续维护成本高 | 下一版交付包可合并为“版本完成审计总览 + 原始证据链接”。 |
| DOC-P2-04 | OpenAPI 变更纪律依赖脚本闸口 | 人工绕过会造成文档/代码漂移 | 继续保留 `docs:openapi-closure`、`verify:p0-contract`、`verify:delivery-gates`。 |

### P3

| 编号 | 问题 | 修复建议 |
|---|---|---|
| DOC-P3-01 | 少数文档存在旧阶段计数、历史日期或格式噪音 | 保留历史计数但在入口和 manifest 中注明当前审计以 CSV 和当前入口为准。 |
| DOC-P3-02 | 少数表格缺空行或章节衔接不够干净 | 后续文档整理时统一 markdown lint，不影响开发。 |

## 6. 需要补写、废弃或合并的文档

### 需要补写/正式化

| 文档 | 原因 | 优先级 |
|---|---|---|
| 完整后台管理端 SRS / 页面矩阵 / UI 自动化矩阵 | 已补写为 `full-admin-srs-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`full-admin-ui-acceptance-matrix-v0.1.md`。剩余工作是按矩阵开发和补 UI 自动化。 | P1 |
| 真实第三方供应商接入规格 | 机器人、视频、设备、人脸、支付等需等 SDK/API、签名、回调、限流、错误码、对账口径到位。 | P1 |
| 生产证据回填包 | 凭证轮换、NAS 恢复演练、MDM 纳管、供应商联调、生产告警等需真实证据。 | P1 |

### 可废弃或只保留归档

| 文档/范围 | 处理 |
|---|---|
| `_archive/**` | 只作历史证据，不作为当前开发依据。 |
| 旧 TypeScript / Fastify / Prisma / SQLite 原型相关 | 已由入口和 verifier 阻断，不再作为正式路线。 |
| 早期“待拍板清单” | 已被 business decision log、unified baseline、final owner checklist 吸收。 |
| 多轮阶段性审计报告 | 暂保留证据，下一版可合并为一份版本审计总览。 |

## 7. 是否可以进入开发/联调/验收/上线

| 阶段 | 结论 | 条件 |
|---|---|---|
| 自有系统继续开发 | 可以 | 以当前入口、OpenAPI、operation matrix、用户故事和验收文档为准。 |
| 内网联调 | 可以 | 继续跑 `verify:delivery-gates`，真实外部依赖只接 mock 或沙箱。 |
| 产品/测试验收 | 可以 | 按 P0/P1/P2/T3 验收矩阵和 UI 自动化报告执行。 |
| 完整后台管理端开发 | 条件可以 | 已完成 SRS、页面/权限/字段矩阵、UI 自动化矩阵；下一步按 MVP 页面批次进入开发。 |
| 真实供应商联调 | 条件可以 | 供应商 SDK/API/签名/回调/错误码/对账口径到位后再进入。 |
| 公开生产上线 | 不可以 | 生产 blocker 与真实证据未全部关闭。 |

## 8. 下一步修订顺序

1. 将本审计、全量文档地图 CSV、可开发规格草案挂入当前入口和交付清单。
2. 若下一步做完整后台，按 `full-admin-srs-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`full-admin-ui-acceptance-matrix-v0.1.md` 拆首批后台页面开发和 Playwright 补测。
3. 若下一步做真实第三方联调，按供应商一个一个建 adapter contract、字段映射、Webhook/轮询、对账和补偿测试。
4. 生产上线前，按 blocker register 回填真实凭证、NAS 恢复、MDM、告警、供应商和安全证据。
5. 每次 OpenAPI 变化后固定执行：改 OpenAPI -> 生成 operation matrix/closure -> 跑 `verify:p0-contract` -> 跑 `verify:delivery-gates`。

## 9. 本次已执行修复

- 修正 `docs/p2-current-diff-adversarial-review-v0.1.md` 中动态定价旧口径。
- 修正 `docs/00-current-effective-document-entry-v0.1.md` 中动态定价完成审计的依赖说明。
- 修正 `docs/superpowers/plans/2026-06-15-p2-baseline-protection-and-web-entry.md` 中“P2-C 只模拟”的旧描述。
- 生成 `docs/development-documentation-audit-map-v0.1.csv`。
- 新增本审计报告与可开发规格草案。

## 10. 验证证据

- `npm run verify:delivery-gates` 已通过。
- OpenAPI 契约验证通过：198 operations，operation matrix 与 closure appendix 同步。
- OpenAPI examples 验证通过：9 个 request body、3 个 response。
- Client route 与 method-specific calls 覆盖验证通过。
- Formal security baseline、production readiness evidence、production blocker register、production owner actions、production evidence submission kit、evidence intake 等交付闸口通过。
