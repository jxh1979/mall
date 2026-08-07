> 本报告为只读调查产物，代码轮开工输入。

# 代码轮欠账·非钱路现状调查（2026-08-07，基线 origin/main=7da3aaa1）

> 只读代理产出，主会话代存。findings open 66 条中 16 条归钱路组，本报告覆盖 50 条 + 2 项调查中新发现。量级分布：S 27 / M 17 / L 6。

## ⚠ 两项新发现（不在 findings 任何一行）

### 1. 企微"到期销毁"是完整可用的数据丢失路径（违反"永久保存"裁决，不可逆）
- WecomRetentionJob（@Scheduled 日跑）→ WecomConversationService.applyRetention:433-455 真删/真匿名；入库时 retention_until 时钟一直在写
- **prod 库实查：wrp_global_default | 180 天 | anonymize | enabled=true 此刻就在生产库**
- 后台 UI 活着（WecomComplianceReviewPage:89-91 可保存留存策略），权限已授 role_platform_admin
- 当前暴露低：@ConditionalOnProperty 默认不注册、env/yml 均未开、wecom_conversations 仅 1 行 retention_until NULL
- **风险**：三个默认关的开关任何一个被后人打开即真跑，无任何门禁挡（正是 R8-B-02"无机械门禁"那条腿）
- 建议（S~M）：删 Job/改抛异常 + 迁移 enabled=false + UI/端点下线 + verify:wecom-no-auto-purge 门禁挂 predeploy

### 2. "人工放行"是活功能两条链（HTML 明写"系统不提供"）
- 票务链：POST /ticketing/verifications/manual-override 端点活、V270 权限已授 platform_admin、status='manual_overridden' CHECK 在
- 治理链：SupervisorReviewService manual_release_l2 策略、后台下拉框有"人工放行"选项、openapi 枚举含 manual_release
- 业务人员在后台看得见且点得动一个 HTML 说不存在的动作。量级 M（端点+权限+CHECK+前端+openapi 全链下线）

## 已实测坐实的代码缺口（节选，全 50 条见分批表）
- REQ-110 能源费：**代码实现的正是已作废的公区四模式分摊**（V162 CHECK 四值、EnergyService 按面积/用量摊公区费），prod 已真出过 1 笔 energy_fee 账、by_area 合同 3 份；商户表计代收路径不存在；电损/水损 source_type 零命中。量级 M
- ignored 下线：4 条 CHECK+Java 两个 Set+前端 5 处；**prod 存量 ignored=0 行，零回填**；inventory_risk_alerts.ignored 是否同删须裁；回流表词表整体与 spec 六态不一致（与 GAP-02 合并为"状态机统一"M 项）
- GAP-26 终端开关 opt-out→opt-in 反转：高危，必须与全量 seed 同批否则全平台静默停用
- GAP-19 手机号全局无条件 UNIQUE（V188）：动 PII hash 约束，单独评审
- GAP-40 风控基准硬编码 0.20 兜底；GAP-30 低消保护 refund 前置零命中；GAP-33/34/12 三条 L 级零承载
- preview_passed/兑换维 manual_override：DB 与代码本就不存在，纯文档幽灵，S

## 三项 closed-loop 基线红（实跑复现 EXIT=1）
1. final-closed-loop-audit：文档硬写行数漂移（TicketingService 905→实际961 等 5 处）——**根因是文档断言活代码行数，建议删断言而非追数字**
2. closed-loop-current-state / 3. production-closure-gap-reports：重跑生成器即绿
修好也守不住除非挂 CI（delivery-gates 整链 CI 从不跑）

## R8-B-02 现状
三道 HTML 门禁已在 predeploy；弱点：ledger-sync 只看台账文件被 touch（改一个空格即过）、不管账本/coverage 两张 csv；企微那条腿=无 wecom 门禁（见新发现1）。加严门禁须用户拍板。

## 代码轮建议分批（与钱路组互补）
- **批 0 待裁**（不写代码）：GAP-21 清账/R10-O-02 越界追认终审/U-03+REGX-15 归口/inventory ignored 是否同删/第五批草案两条
- **批 1 零风险**（8 条 S 一个 PR）：GAP-20/27/22/39/31、伪 ID、ignored S 部分、保证金 10 处标签+场次容量 2 处报错文案
- **批 2 最高优先**（HTML 说没有的东西业务人员点得动）：人工放行两条链全下线、企微销毁路径下线+门禁、三基线红修复挂 CI、R8-U-02 扩检⑤覆盖
- **批 3 单域改造**（12 条 M 互不阻塞）：能源费、状态机统一、库存冲正、订台定金、低消、风控兜底等
- **批 4 高危/新建模**（10 条）：GAP-26 开关反转、GAP-19 唯一约束、工单七态、三条 L、45 条 PARTIAL 人工核验
- **文档批**（13 条 S 照抄补写，可并行）
