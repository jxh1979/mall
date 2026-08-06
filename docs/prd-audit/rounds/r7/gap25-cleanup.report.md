# GAP-25 域外清理执行报告（企微会话留存旧口径）

- **轮次**：R7 收尾专项（任务 B）
- **日期**：2026-08-05
- **分支**：`docs/prd-audit-r7-20260805`；**基线 HEAD**：`37953d4d`
- **现行口径（唯一有效）**：L-19 / `DEC-CFG-2026-08-03-10`，PGM「企业微信工作会话永久保存」条 —— **企微会话原文永久保存、不自动到期删除**，不存在按期自动归档/销毁/匿名化或按策略批量到期删除的路径；确需清理**仅限最高权限本人插入授权 U 盘密钥后手动执行**（逐条填写理由、先试运行预览、清理动作本身另行留痕）。
- **作废对象**：180 天默认留存、可配置留存期限、到期归档/销毁/匿名化、离职/争议后降级销毁匿名化处置。
- **本任务范围**：**仅域外文档**（r5-spec / backlog / dev-design / compliance / 分层规格 / 索引 / 矩阵等）。三份业务 HTML、决策台账、`prd-audit-fix-change-log-20260804.md`、审计范围内 23 份 PRD **一律不动**（唯一例外：按任务要求更新 `prd-resync-change-log-20260805.md` §5.1 GAP-25 行状态）。
- **处置原则**：**原文一律保留、不删除任何文字与文件**，只加显著作废标注。

## 1. 定位方法与命中总量

```
grep -rn '180 天|180天|留存期|销毁|匿名化|到期归档|自动删除' docs/ --include='*.md'
  交叉过滤：本行 或 前后 2 行 或 所属最近标题 含 '企微|企业微信|wecom|WeCom'
```

- 关键词初筛：**51 份** `.md` 命中。
- 企微交叉过滤后：**115 处 / 43 份**（含台账、审计产物、归档区与范围内 PRD）。
- 判定结果：**A/B 类已处置 54 处 / 23 份**（**A 类可执行规格/待办/契约 20 处**、**B 类描述性口径句 34 处**）；**C 类只登记不改 26 组**（见 §3）。

## 2. A/B 类处置全量清单（54 处 / 23 份，均为域外文档）

> 行号为 **HEAD=`37953d4d` 基线原始行号**；标注插入会使同文件后续行下移，故「现行行号」以 §4 回读为准。
> 类型 **A ＝ 可执行规格/待办/契约**（不作废即可能被实施、造成真实数据丢失）；**B ＝ 描述性口径句**。

| # | 文件 | 基线行 | 类型 | 标注形态 | 落点说明 | 处置前原文（摘录） |
|---|---|---|---|---|---|---|
| 1 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L1 | **A** | 块（文档级） | H1 之后插入文档级作废标注，覆盖全文留存期/到期/销毁/匿名化规格 | `# T8 企微平台侧合规能力 可编码开发规格 V0.1（2026-06-18）` |
| 2 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L4 | **A** | 行内 | 裁决依据行「默认留存 180 天」 | `> **裁决依据**：裁决⑥ + 二轮——平台侧合规能力（可配置留存/到期归档销毁匿名化/权限脱敏/查看写访问审计）本期 P0，**默认留存 180 天**；企微**同步接口按第三方原则预留 + 后台开关**（依赖 T2 开关闸口）。` |
| 3 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L12 | **A** | 块内注（SQL） | SQL 注释「留存策略（可配置，默认 180 天）」下方插入 SQL 作废注释 | `-- 留存策略（可配置，默认 180 天）` |
| 4 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L65 | **A** | 行内 | §2「到期处理（@Scheduled）」定时销毁逻辑 | `- **到期处理（@Scheduled）**：扫 `retention_until < today` 且 active 的会话/消息，按 `policy.expiry_action` 执行 archive/delete/anonymize（anonymize=清原文引用 + content_reda …（原文续）` |
| 5 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L77 | **A** | 行内 | §4 GWT：默认 180 天改 365 | `- Given 留存策略默认 180 天，When 配置改 365，Then 新会话按新期限计算 retention_until。` |
| 6 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L78 | **A** | 行内 | §4 GWT：定时任务按 expiry_action 归档/销毁/匿名化（GAP-25 登记最高危处） | `- Given 会话超 retention_until，When 定时任务运行，Then 按 expiry_action 归档/销毁/匿名化。` |
| 7 | `r5-spec-T8-wecom-compliance-dev-design-20260618.md` | L84 | **A** | 行内 | §5 测试项「留存到期三动作」 | `- 测试：留存到期三动作、脱敏/原文分级 + 审计落库、开关门控、retention_until 计算；`MigrationVersions.latest()`。` |
| 8 | `r5-development-implementation-backlog-20260618.md` | L90 | **A** | 块（节级） | #### T8 标题之后插入任务项级作废标注 | `#### T8 企微平台侧合规能力 — 裁决⑥` |
| 9 | `r5-development-implementation-backlog-20260618.md` | L91 | **A** | 行内 | T8 迁移项（GAP-25 登记高危处） | `- **迁移**：会话留存表 + 留存策略（可配置期限）、到期归档/销毁/匿名化、脱敏链、查看访问审计表。` |
| 10 | `r5-development-implementation-backlog-20260618.md` | L93 | **A** | 行内 | T8 验收项（GAP-25 登记高危处） | `- **验收**：留存策略可配；到期触发归档/销毁/匿名化；查看原文产生审计记录。` |
| 11 | `r5-development-implementation-backlog-20260618.md` | L94 | **A** | 行内 | T8 未决项「留存期限参数」 | `- **依赖**：T2（开关）。**未决**：留存期限参数、企微授权配置 owner。` |
| 12 | `r5-development-implementation-backlog-20260618.md` | L95 | **A** | 行内 | T8 可编码规格引用（retention_policies 默认180天 + 到期定时） | `- **可编码规格（已出）**：`docs/r5-spec-T8-wecom-compliance-dev-design-20260618.md`（wecom_conversations/messages/retention_policies(默认180天)/access_audits+到期归档销毁 …（原文续）` |
| 13 | `r5-development-implementation-backlog-20260618.md` | L169 | **A** | 表内 | §四 二轮裁决表「企微留存期限参数＝后台可配，默认 180 天」（GAP-25 登记高危处） | `\| 企微留存期限参数 \| T8 \| **后台可配，默认 180 天**；到期归档/销毁/匿名化 \| 企微授权配置 owner \|` |
| 14 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L4 | **B** | 行内 | 文首审计块「留存期限默认 180 天、后台可配」与候选验收口径 | `> 以业务说明地图（企微会话治理）为基准核验真实代码：本要求 §4 的**企微会话可配置留存、到期归档/销毁/匿名化、不脱敏原文最小授权+审批、查看写访问审计**在后端**全部零落地**（无留存表/上传接口/到期触发/脱敏链/访问审计，仅 V015 一行 mock 种子）。这是 PIPL/劳动法监控 …（原文续）` |
| 15 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L70 | **B** | 块（章级） | ## 4 企业微信会话处理合规（C3）章级作废标注 | `## 4. 企业微信会话处理合规（C3，关联报告 A7）` |
| 16 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L81 | **B** | 行内 | §4.1 第 5 条 离职/争议后降级/销毁/匿名化处置策略 | `5. 离职/合同终止后、争议关闭后的会话**处置策略**（降级/销毁/匿名化）。` |
| 17 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L137 | **B** | 行内（范围例外） | §6.1 C5 分类留存表（含企微会话类别） | `- 建立**个人信息分类留存表**：对手机号、证件、支付信息、消费记录、企微会话、视频证据分别设定法定/业务留存期限。` |
| 18 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L138 | **B** | 行内（范围例外） | §6.1「到期自动删除或匿名化」 | `- 到期**自动删除或匿名化**，与软删除/归档机制对齐。` |
| 19 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L146 | **B** | 表内（范围例外） | §6 数据类别表「企微会话 → 归档/销毁/匿名化」行 | `\| 企微会话 \| 见第 4 节可配置留存 \| 归档/销毁/匿名化 \|` |
| 20 | `compliance-and-personal-information-protection-requirements-v0.1.md` | L317 | **B** | 表内（范围例外） | C5 合规映射表行（分类留存表 + 销毁作业） | `\| C5 数据留存销毁 \| data-risk-ai / foundation \| 分类留存表 + 销毁作业 \| P0策略/P1作业 \|` |
| 21 | `data-risk-ai-development-design.md` | L675 | **B** | 块（节级） | ### 11.5 企业微信会话留存合规 节级作废标注 | `### 11.5 企业微信会话留存合规` |
| 22 | `data-risk-ai-development-design.md` | L684 | **B** | 行内 | §11.5 第 4 条 离职/争议后处置策略（降级/销毁/匿名化） | `4. **范围与到期处置**：定义离职/合同终止后、争议关闭后的会话处置策略（降级/销毁/匿名化），与 system-breakdown 事件归档周期一致，二者取严。` |
| 23 | `data-risk-ai-development-design.md` | L692 | **A** | 表内 | §11.5 留存策略字段表 retention_days 行 | `\| retention_days \| int nullable \| 留存天数；为空表示按 `permanent_with_basis`（长期保存且有合法性论证） \|` |
| 24 | `data-risk-ai-development-design.md` | L693 | **A** | 表内 | §11.5 留存策略字段表 disposal_action 行 | `\| disposal_action \| varchar \| 到期处置：archive / delete / anonymize \|` |
| 25 | `data-risk-ai-development-design.md` | L698 | **B** | 行内 | §11.5 末注「编码以本节可配置留存字段为准」（旧口径直接编码指令） | `> 注：本节与 §11.2"永久保存"口径的关系——"永久保存"应理解为"业务存续期内按策略保存且可配置到期处置"，而非无限期硬保存；编码以本节可配置留存字段为准。§11.2 原文已就地改为可配置留存（A7/C3 收口），两处口径已统一。` |
| 26 | `data-risk-ai-development-design.md` | L700 | **A** | 块（节级·范围例外） | §11.6 C5 销毁作业：wecom_session 不得纳入到期扫描/删除/匿名化与撤回同意级联 | `### 11.6 个人信息留存与销毁作业（C5）` |
| 27 | `terminal-display-layer-spec-v0.2.md` | L886 | **B** | 行内 | §5 已确认 #11 | `11. 企业微信工作会话**留存期限可配置（非永久）**，到期归档/销毁/匿名化；普通展示按权限脱敏；不脱敏原文仅限最小授权 + 审批的取证/质检场景，查看必须写入审计。（依基准 A7/合规 C3 修订：原"永久保存"改为可配置留存，与第三方接入层验收清单口径统一）` |
| 28 | `data-analysis-layer-spec-v0.2.md` | L161 | **B** | 行内 | §8 业务规则条 | `- 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看原文必须写入审计。<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"与本层 page …（原文续）` |
| 29 | `data-analysis-layer-spec-v0.2.md` | L184 | **B** | 行内 | §10 已确认规则第 8 条 | `8. 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏。<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"为收口前固化规则，与 A7/C3 可 …（原文续）` |
| 30 | `data-analysis-layer-spec-v0.2.md` | L389 | **B** | 表内 | 上线前置门槛行（会话留存期限须按 C3 落地后方可上线） | `\| 企业微信质检合规 \| 会话留存期限与查看授权须按 compliance §4（C3）落地后方可上线 \| C3 \|` |
| 31 | `data-analysis-layer-spec-v0.2.md` | L950 | **B** | 表内 | 验收清单行（企业微信会话留存合规） | `\| 企业微信会话留存合规 \| 留存期限与查看授权须按 compliance §4（C3）落地后上线 \| C3 \|` |
| 32 | `third-party-access-layer-spec-v0.2.md` | L769 | **B** | 行内 | §已确认第 4 条 | `4. 企业微信工作会话**留存期限可配置（非永久）**，到期归档/销毁/匿名化；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看强制写审计（合规文档 §4 C3、报告 A7；finding third-6）。` |
| 33 | `product-requirements-v0.1.md` | L539 | **B** | 行内 | 需求条 26 | `26. 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（见 compliance §4 C3、data-risk §11.5）；会话原文按权限查看，不脱敏原文仅限最小授权 + 审批的取证/质检场景，查看必须写入访问审计。<!-- 修订说明（2026-06-08 三轮收口）：原始需求 …（原文续）` |
| 34 | `business-decision-log-v0.1.md` | L168 | **B** | 行内 | §27 企微决策条 | `- 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）。保留"按权限查看 + 写审计"。` |
| 35 | `business-systems-breakdown.md` | L757 | **B** | 行内 | 会话原文留存期限可配置条 | `- 会话原文留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看必须写入审计<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"为 MVP 转述，已按 A7/ …（原文续）` |
| 36 | `business-functional-topology-v0.1.md` | L421 | **B** | 表内 | §6 业务规则影响表行 | `\| 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看写审计<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"为 §6 影响表正文断言，与 …（原文续）` |
| 37 | `product-documentation-index-v0.1.md` | L224 | **B** | 表内 | 业务规则表行（并行任务在本文件前部插入 1 行，实际落点为现行 L225，已按内容校正） | `\| 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看写审计<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"与 A7/C3 可配置留存相 …（原文续）` |
| 38 | `system-capability-layers-product-index-v0.1.md` | L198 | **B** | 表内 | 业务规则表行 | `\| 企业微信工作会话留存期限可配置（非永久），到期归档/销毁/匿名化（按 A7/合规C3、data-risk §11.5）；不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看写审计<!-- 修订说明（2026-06-08 三轮收口）：原"永久保存"与 A7/C3 可配置留存相 …（原文续）` |
| 39 | `p0-action-event-evidence-matrix-v0.1.md` | L113 | **B** | 表内 | 证据留存表「工作会话/质检材料 → 按配置留存，到期归档/销毁/匿名化」行 | `\| 工作会话/质检材料 \| 按配置留存，到期归档/销毁/匿名化 \| 质检、法务、授权主管 \|` |
| 40 | `third-party-integration-development-design.md` | L9 | **B** | 行内 | 文首 R5 审计标注（把到期归档销毁匿名化触发列为待落地能力） | `> - **企微会话留存/脱敏/审计零落地**：后端无会话上传接口/留存表/到期归档销毁匿名化触发/脱敏专链/查看访问审计，仅 V015 一行 mock 种子。属 PIPL 合规暴露（另见 `compliance-...-requirements-v0.1.md` R5）。` |
| 41 | `third-party-integration-development-design.md` | L1051 | **A** | 表内 | wecom_messages.content_text 字段规格（retention_days/disposal_action 落库口径） | `\| content_text \| text \| 文本内容；企业微信工作会话不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看受权限控制并强制写审计。**留存期限可配置（per 报告 A7），非无限期"永久"**：按合规文档 §4 C3 设 `retention_policy_ …（原文续）` |
| 42 | `third-party-integration-development-design.md` | L1429 | **B** | 表内 | 口径表「企业微信会话留存期限与处置」行 | `\| 企业微信会话留存期限与处置 \| **口径（2026-06-07）：可配置 `retention_days`（非永久），到期归档/销毁/匿名化，离职/争议关闭后按处置策略降级；合法性基础与告知见合规文档 §4 C3、报告 A7（finding third-6）** \|` |
| 43 | `final-open-decisions-and-owner-checklist-20260610.md` | L25 | **B** | 表内（范围例外） | L-002 待决项（含企微会话留存天数配置） | `\| L-002 \| 个人信息留存期限 \| 分类留存，最短必要；到期归档/销毁/匿名化 \| 实施阶段配置手机号、证件、支付、消费、企微会话、视频证据、合同附件留存天数 \| 影响合规和自动销毁任务 \|` |
| 44 | `requirement-traceability-matrix-v0.1.md` | L57 | **B** | 行内 | REQ-54 横幅第 6 条 | `> 6. **企微会话（REQ-54）**：平台侧合规能力（可配置留存/到期归档销毁匿名化/权限脱敏/查看写审计）本期 P0；企微同步接口按第三方原则预留 + 开关。` |
| 45 | `requirement-traceability-matrix-v0.1.md` | L63 | **B** | 行内 | 二轮裁决横幅「企微留存＝默认 180 天可配」 | `> **二轮裁决（2026-06-18，原未决项收口）**：订台定金=后台可配固定额+比例；供应链高价值复核=库存域内自建；扫码交接=员工扫码主动+机器人上报被动；企微留存=默认 180 天可配；空间=扁平 spaces 保留+全对象 project_id 隔离（四级模型作废）；权限点=全业务域最小 …（原文续）` |
| 46 | `requirement-traceability-matrix-v0.1.md` | L186 | **A** | 表内 | REQ-54 行：V087 wecom_conversations「可配置留存默认 180 天 + 到期归档/销毁/匿名化」已落库描述 | `\| REQ-54 \| **企微每日上传聊天、服务分析、违规检查；原始提出永久保存/不脱敏，现行裁决为可配置留存/分级查看** \| business-decision-log §27；data-risk-ai dev-design §11.2/§11.5；compliance-and-personal …（原文续）` |
| 47 | `p0-implementation-task-list.md` | L472 | **B** | 表内 | D-010 企业微信统一同步实施责任行 | `\| D-010 \| 企业微信统一同步实施责任 \| 已确认按最低管理成本方案执行：会话留存期限可配置；管理员一次性完成企业微信后台授权和密钥配置，系统自动做账号映射、同步、失败告警和基础规则质检；AI 深度质检进入 P2 \|` |
| 48 | `system-breakdown.md` | L223 | **B** | 表内 | 数据脱敏行内 HTML 注释（会话原文留存非永久） | `\| 数据脱敏 \| 手机号、支付信息、证件信息脱敏；企业微信会话不脱敏原文仅限最小授权 + 审批的取证/质检场景，普通展示按权限脱敏，查看写审计<!-- 修订说明（2026-06-08 三轮收口）：会话原文留存非永久——留存期限可配置，到期归档/销毁/匿名化（按 A7/合规 C3、data-risk  …（原文续）` |
| 49 | `original-requirements-capture-v0.1.md` | L142 | **B** | 行内 | 原始需求留痕条括注「现行设计已裁决为可配置留存期限，非永久」 | `- 企业微信工作会话永久保存（原始口述需求留痕；现行设计已裁决为可配置留存期限，非永久）。` |
| 50 | `original-requirements-capture-v0.1.md` | L145 | **B** | 行内 | 2026-06-08 复核注（陈述现行执行口径为可配置留存） | `> 2026-06-08 复核注：以上三条为原始口述需求留痕。现行执行口径已按合规要求改为“留存期限可配置（非永久），到期归档/销毁/匿名化；普通展示按权限脱敏；不脱敏原文仅限最小授权 + 审批的取证/质检场景，且必须写访问审计”。` |
| 51 | `foundation-database-design.md` | L1428 | **B** | 行内 | §6B.4 交叉引用（到期销毁/匿名化定时作业、作业留痕表） | `> 交叉引用（2026-06-08 收口）：本表与 `data-risk-ai-development-design §11.6.1` **为同一张表**（已统一表名 `pii_retention_policies`，避免重名冲突）；其**到期销毁/匿名化定时作业、撤回同意级联、作业留痕表 `dat …（原文续）` |
| 52 | `foundation-database-design.md` | L1452 | **B** | 行内 | §6B.4「企微会话留存以 data_category=wecom_session 为准，与 §11.5 可配置留存一致」 | `> 与企微会话留存口径对齐：企微会话留存以本表 `data_category=wecom_session` 行为准，与 data-risk-ai §11.5「可配置留存」一致；撤回同意（6B.2 withdrawn_at）触发对应类别记录的级联软删/匿名化。` |
| 53 | `p0-openapi-contract-closure-appendix-v0.1.md` | L2457 | **A** | 表内（端点） | upsertWecomRetentionPolicy 端点行「配置企微留存策略（默认180天）」 | `\| P0-待归类 \| `upsertWecomRetentionPolicy` \| `POST` \| `/api/v1/compliance/wecom/retention-policies` \| 配置企微留存策略（默认180天） \| `wecom.retention.manage` \| `weco …（原文续）` |
| 54 | `p0-openapi-operation-acceptance-matrix-v0.1.md` | L319 | **A** | 表内（端点） | upsertWecomRetentionPolicy 验收矩阵行「默认180天」 | `\| compliance \| `POST` \| `/api/v1/compliance/wecom/retention-policies` \| `upsertWecomRetentionPolicy` \| 配置企微留存策略（默认180天） \| `object` \| `wecom.retention. …（原文续）` |

**处置后统一标注文本**（三种变体，均为追加、不删原文）：

- 行内 / 表内：`**【已作废（2026-08-03 L-19 裁决 \`DEC-CFG-2026-08-03-10\`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**`
- 块（文档级/章节级）：引用块起首为 `> **⚠ 【已作废（2026-08-03 L-19 裁决 \`DEC-CFG-2026-08-03-10\`）】**`，点名本文档/本节全部留存期与到期销毁规格不得实施，并声明「以下原文一律保留，仅作历史留痕，不构成可实施规格」。
- 范围例外变体：用于 C5 通用分类留存/销毁作业处，只把 **企微会话 / `wecom_session`** 类别排除出自动到期处置，明确「其他数据类别不受本标注影响」。

## 3. C 类：已定位但按判定不改（只登记）

| 文件 | 命中处 | 性质 | 判定与理由 |
|---|---|---|---|
| `prd-audit-fix-change-log-20260804.md` | L63/L64/L66/L73/L74/L154/L155/L1986/L2015/L2058–L2098/L2214（14 处命中） | 台账（GAP-25 唯一登记处） | 不改。本文件纪律为「只追加、不改既有行」；GAP-25 登记与阻断要求原样保留 |
| `prd-resync-change-log-20260805.md` | L862–L864/L1304/L1455 | R7 主台账 | 仅按本任务要求更新 §5.1 GAP-25 行状态；其余行（含 DIFF-B-037 记录）不改 |
| `prd-sync-change-log-20260804.md` | 4 处命中 | 历史变更台账 | 不改（历史记录） |
| `prd-audit/business-decision-ledger.md` | 1 处命中 | 决策台账（事实源之一） | 不改（本任务不动台账既有行） |
| `prd-audit/rounds/r7/history-index.md` | 2 处 | R7 审计产物 | 不改 |
| `prd-audit/rounds/r7/diff-matrix-b.md` | 2 处 | R7 审计产物 | 不改 |
| `prd-audit/rounds/r7/changelog-t6b.fragment.md` | 3 处 | R7 变更片段（逐字节合并源） | 不改 |
| `evidence-risk-control-product-prd-v0.1.md` | L265/L267/L268/L270/L284/L402/L412 | 审计范围内 PRD（R7 已修） | 不改。已为现行口径（永久保存 + U 盘密钥手动清理），DIFF-B-037 已闭环 |
| `third-party-business-access-product-prd-v0.1.md` | L205/L208/L287/L289 | 审计范围内 PRD（R7 已修） | 不改。已为现行口径 |
| `platform-governance-prd-v0.1.md` | L708 | 审计范围内 PRD（已修） | 不改。已为现行口径 |
| `terminal-page-product-prd-v0.1.md` | L352 | 审计范围内 PRD（已修） | 不改。已为现行口径 |
| `business-functional-topology-v0.1.md` | L455 | 变更日志行 | 不改（历史记录，记载 2026-06-08 三轮收口的旧处置） |
| `data-analysis-layer-spec-v0.2.md` | L207 | 变更日志行 | 不改（历史记录） |
| `system-capability-layers-product-index-v0.1.md` | L243 | 变更日志行 | 不改（历史记录） |
| `terminal-display-layer-spec-v0.2.md` | L909 | 变更日志行 | 不改（历史记录） |
| `data-risk-ai-development-design.md` | L1200/L1202/L1204/L1209（现行 L1204/L1206/L1208/L1213） | 变更日志行 | 不改（历史记录） |
| `data-risk-ai-development-design.md` | L702/L708/L710（现行 L706/L712/L714） | §11.6 修订说明块 | 不改（历史成因说明）；实施约束已由 §11.6 范围例外标注覆盖 |
| `foundation-database-design.md` | L1426 | §6B.4 修订说明 | 不改（历史成因说明，泛 C5 口径） |
| `compliance-and-personal-information-protection-requirements-v0.1.md` | L79（现行 L81） | §4.1 第 3 条 | 不改。已有 2026-08-03 用户裁决作废标注（前轮已处置），不重复标注 |
| `data-risk-ai-development-design.md` | L683（现行 L685） | §11.5 第 3 条 | 不改。已有 2026-08-03 用户裁决作废标注（前轮已处置），不重复标注 |
| `compliance-and-personal-information-protection-requirements-v0.1.md` | L6 | 文档定位说明 | 不改。「数据留存期限」为泛列举的立法缺口清单，非企微留存口径 |
| `business-decision-log-v0.1.md` | L861 | 人脸照留存天数决策 | 不改。非企微会话（生物识别信息留存），不属 GAP-25 |
| `operation-traceability-and-terminal-switch-prd-v0.1.md` | L74/L378/L379 | C5 通用个人信息事件字典 | 不改。为通用 personal_data.purged/anonymized 事件，非企微专属；企微例外已由 data-risk-ai §11.6 标注覆盖 |
| `p0-action-event-evidence-matrix-v0.1.md` | L115 | 合同附件法定留存期行 | 不改。非企微 |
| `platform-governance-product-spec-v0.2.md` | L1855 | 员工监控告知流程图节点 | 不改。「告知目的/范围/留存期限/权利」为 PIPL 告知内容项，非企微留存规格 |
| `_archive/20260610-retention-audit/**（10 份、13 处）` | historical-audits 2 份、merged-layer-spec-sources 6 份、merged-source-docs 2 份 | 已归档历史文档 | 不改。归档区为历史留痕，且已在 `_archive` 之下，不构成现行规格 |

## 4. 高危处前后原文回读（`sed -n` 实测）

### GAP-25 登记最高危：定时任务销毁会话原文

- **文件**：`docs/r5-spec-T8-wecom-compliance-dev-design-20260618.md`
- **基线行**：L78（HEAD=`37953d4d`）→ **现行行**：L81

处置前：

```text
- Given 会话超 retention_until，When 定时任务运行，Then 按 expiry_action 归档/销毁/匿名化。
```

处置后：

```text
- Given 会话超 retention_until，When 定时任务运行，Then 按 expiry_action 归档/销毁/匿名化。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
```

### GAP-25 登记高危：T8 迁移项

- **文件**：`docs/r5-development-implementation-backlog-20260618.md`
- **基线行**：L91（HEAD=`37953d4d`）→ **现行行**：L94

处置前：

```text
- **迁移**：会话留存表 + 留存策略（可配置期限）、到期归档/销毁/匿名化、脱敏链、查看访问审计表。
```

处置后：

```text
- **迁移**：会话留存表 + 留存策略（可配置期限）、到期归档/销毁/匿名化、脱敏链、查看访问审计表。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
```

### GAP-25 登记高危：T8 验收项

- **文件**：`docs/r5-development-implementation-backlog-20260618.md`
- **基线行**：L93（HEAD=`37953d4d`）→ **现行行**：L96

处置前：

```text
- **验收**：留存策略可配；到期触发归档/销毁/匿名化；查看原文产生审计记录。
```

处置后：

```text
- **验收**：留存策略可配；到期触发归档/销毁/匿名化；查看原文产生审计记录。　**【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话原文永久保存、不自动到期删除；本处销毁／留存期／到期处置规格不得实施；确需清理仅限最高权限本人插授权 U 盘密钥手动执行（逐条填写理由、先试运行预览、清理动作本身另行留痕）。原文保留仅作历史留痕。**
```

### GAP-25 登记高危：默认 180 天参数裁决行

- **文件**：`docs/r5-development-implementation-backlog-20260618.md`
- **基线行**：L169（HEAD=`37953d4d`）→ **现行行**：L172

处置前：

```text
| 企微留存期限参数 | T8 | **后台可配，默认 180 天**；到期归档/销毁/匿名化 | 企微授权配置 owner |
```

处置后：

```text
| 企微留存期限参数 | T8 | **后台可配，默认 180 天**；到期归档/销毁/匿名化 | 企微授权配置 owner  **【已作废（2026-08-03 L-19 裁决 `DEC-CFG-2026-08-03-10`）】企微会话永久保存、不自动到期删除；本行销毁／留存期规格不得实施；确需清理仅限最高权限 U 盘密钥手动路径** |
```

## 5. 验证记录

| 检查项 | 命令 / 方法 | 结果 |
|---|---|---|
| 落点归属 | `git diff --numstat` | 本任务 23 份文件全部为「+N/-N 行内追加」型改动；无删除行、无文件新增/删除（报告文件除外） |
| 标注落点正确性 | 逐条校验：每条行内/表内标注所在行必须同时含留存关键词 | **54/54 通过，0 处错位**（含 1 处初次错位已修正，见 §6） |
| GAP-25 登记 22 处覆盖 | 按条目内容（非行号）定位后检查标注 | 20 处新加标注；2 处（`compliance` §4.1#3、`data-risk-ai` §11.5#3）已有 2026-08-03 裁决作废标注，不重复标注 |
| 高危处回读 | `sed -n` 前后对照 | 见 §4，4 处全部命中 |
| 残留复扫 | 交叉过滤脚本重跑，排除作废标注行与节级块覆盖行 | 剩余 22 行全部落入 C 类（变更日志 / 修订说明 / 非企微 / 已有标注），**无未处置的在用旧口径规格** |
| 行尾符 | 逐文件对比 HEAD 版本与工作区的 CRLF 行数（`grep -c` 计 `\r` 结尾行） | 2 份 CRLF 文件（`business-decision-log-v0.1.md`、`terminal-display-layer-spec-v0.2.md`）曾被误转 LF，**已还原 CRLF**，numstat 由 908/908、910/910 回到 1/1 |
| 禁止项 | `git status --porcelain` | 未删除任何文件；未改三份 HTML、决策台账、`prd-audit-fix-change-log-20260804.md`；未执行 `git commit` / `git push` |

## 6. 执行过程中的偏差与修正

1. **CRLF 误转**：首次批量写入把 2 份 CRLF 文件整体转为 LF（全文件 diff）。已按原行尾符还原，复核 numstat 回到 1/1。
2. **并行任务导致行号漂移 1 处**：`product-documentation-index-v0.1.md` 在本任务读取行号与写入之间被并行任务在 L107 区插入 1 行（`service-order-product-prd` 索引行），使目标行由 L224 变为 L225，标注一度落到上一行（「合同或基础定义违规」行）。已**撤回误标注并按内容重新定位**到正确的企微行（现行 L225）；该文件现存的 `+ service-order` 行属并行任务改动，非本任务产出。

## 7. 未闭合风险（须另立专项，本任务不处理）

1. **实现侧已落库的旧口径（最高优先）**：`requirement-traceability-matrix-v0.1.md` REQ-54 行记载 **V087 已建 `wecom_conversations`（可配置留存默认 180 天 + 到期归档/销毁/匿名化）与 `wecom_retention_policies`（项目/商户/全局三级）**；`p0-openapi-*` 两份记载端点 `POST /api/v1/compliance/wecom/retention-policies`（`upsertWecomRetentionPolicy`）。**本任务只作废文档规格，未核查也未改动代码、迁移与 OpenAPI**。若这些表/端点与到期作业在代码中真实存在，则**旧口径已在实现侧生效，存在真实的会话原文丢失风险**，须单独立项核查（表/列/定时任务/端点是否存在、是否有调度）。
2. **`data_retention_purge_job`（C5）与企微的交集**：本任务已在 `data-risk-ai` §11.6 加范围例外标注，但**实现侧该作业是否已把 `wecom_session` 纳入扫描范围未核查**。
3. **两份文档对「长期保存合法性论证」状态表述不一致**：`compliance` §4.1#3 称论证已于 2026-08-04 补齐（DEC-R6-23，未了义务关闭）；`data-risk-ai` §11.5#3 仍称「论证尚未完成，列为未了义务」。属既有标注冲突，本任务未改写（超范围），建议登记。
4. **`_archive` 归档区 13 处旧口径未加标注**：判定为历史留痕；若后续有人从归档区取材，仍可能误引。
