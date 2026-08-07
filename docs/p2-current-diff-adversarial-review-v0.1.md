# P2 当前差异对抗式审计报告 V0.1

> **口径修订（2026-08-06 R10）：「场次容量」已废止**——BSM「容量控制」条 明确「**只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量**」（DEC-R5-2026-08-04-04；R8 用户裁决 D-02）。本文原「场次容量」一律改称「**项目容量（实时在玩人数）**」；双阈值不变（运营容量软上限告警放行、安全容量硬上限阻断并自动停运）。仍按场次/时段或点位/设备维度罗列的页面、接口、组件与字段只作历史结构参考，容量口径一律以本条为准，相关字段待代码轮下线，期间不得据以新增受控维度。

## 1. 审计范围

本报告审计 160 服务器 `/home/james/complex-management` 中，相对提交 `0efe4c2 chore: establish protected baseline before p2 coding` 的当前未提交差异。

审计目标不是润色文档，而是判断当前 P2 差异是否足够进入保护性基线，并为下一步 P2 Web 管理入口补齐提供干净起点。

## 2. 差异分组

| 分组 | 文件范围 | 审计结论 |
|---|---|---|
| P2-B 游乐团体/包场 | `backend/src/main/java/com/complex/platform/amusement/*GroupBooking*`、`AmusementService`、`JdbcAmusementRepository`、`V035__p2_amusement_group_private_bookings.sql`、相关测试和契约文档 | 可进入保护性基线。容量锁定使用数据库条件更新和约束，控制了超卖风险 |
| P2-C 票务动态定价 | `backend/src/main/java/com/complex/platform/ticketing/*DynamicPricing*`、`TicketingService`、`JdbcTicketingRepository`、`V036__p2_ticket_dynamic_pricing_rules.sql`、相关测试和契约文档 | 可进入保护性基线。当前已升级为规则容器、模拟和已发布规则影响正式订单实收价格的闭环；草稿规则不影响订单 |
| P2-D 经营指标快照 | `backend/src/main/java/com/complex/platform/analytics/*`、`V037__p2_business_metric_snapshots.sql`、相关测试和契约文档 | 可进入保护性基线。当前是快照读取和种子数据，不涉及实时财务结算改写 |
| P2-E/F 边界文档 | 第三方深度集成边界、独立 Python AI 服务边界文档 | 可进入保护性基线。边界明确为接口/资料/后续实现占位，未伪装为真实接入完成 |
| 当前有效入口 | `docs/00-current-effective-document-entry-v0.1.md` | 本次补齐 P2-B/C/D/E/F 和 P2 全量审计入口，避免 P2 完成材料无法被后续开发找到 |

## 3. 对抗式发现

### F1. 当前 P2 后端差异较大，必须先固化基线再进入 Web 编码

**风险等级：P0**

当前差异包含 Java 业务代码、Flyway 迁移、权限种子、Controller 测试、Service 测试、JDBC 集成测试和多份 P2 文档。如果直接继续写 Web 入口，后续缺陷定位会混在一个大 diff 里，回滚边界不清。

**收口动作：**先完成本报告、测试、安全扫描和保护性提交，再进入 P2 Web 管理入口。

### F2. P2-B 容量锁定必须以数据库条件更新为最终防线

**风险等级：P1**

团体/包场原会占用场次容量（原称「场次容量」，已废止·见文首口径）；现行：包场期间该项目整体停止对外核销，团体按项目容量（实时在玩人数）判定。如果只在 Service 层判断剩余容量，并发审批时可能超卖。当前实现使用 `used_capacity + locked_capacity + reserved_capacity <= capacity` 的数据库更新条件，并在表约束中限制 `used_capacity + locked_capacity <= capacity`，方向正确。

**剩余注意：**Web 端只能展示和发起审批，不能在前端自行判断“必定可锁定”；最终结果必须以后端审批返回为准。

### F3. P2-C 动态定价生效边界必须写清楚

**风险等级：P1**

动态定价已经从“规则容器/模拟”升级为“已发布且匹配的规则会影响正式售票订单实收价格”。当前正确口径是：草稿规则只用于模拟，不影响订单；发布规则命中正式订单时，订单必须记录基础金额、动态调整金额、最终金额和命中规则。

**剩余注意：**后续不得再引用“动态定价不影响正式订单”作为验收口径；若要改变优先级、叠加算法、渠道例外或自动化调价策略，必须另开规则治理和验收任务。
### F4. P2-D 经营指标快照不能被误解为财务主账

**风险等级：P1**

经营指标快照用于分析和趋势展示，不能替代 P0/P1 财务应收应付、日结、对账主口径。当前实现为独立快照表和只读接口，未反向写财务主账。

**剩余注意：**Web 页面文案必须写成“经营指标快照/分析”，不得写成“财务结算结果”。

### F5. P2-E/F 仍然不能进入真实编码

**风险等级：P1**

第三方深度集成和独立 Python AI 服务都需要供应商 SDK/API、签名、回调、字段映射、模型、部署和 SLA 资料。当前只允许边界文档和占位接口语言，不应编写真实适配器或真实 AI 服务。

**收口动作：**下一步候选 B 不包含供应商真实接入和 Python AI 服务实现。

### F6. 当前缺口从后端能力转为 Web 可操作入口

**风险等级：P1**

后端 P2-B/C/D 已具备接口和测试，但 Web 端尚未形成对应管理入口。若不补 Web，运营、测试和产品验收仍会依赖接口调用，不能证明业务角色可执行。

**推荐动作：**候选 B 优先做 P2 Web 管理入口 MVP：能查看、能发起受控操作、能看到状态和审计线索；高级图表、复杂筛选和供应商真联调后置。

## 4. 安全审计结论

| 维度 | 结论 |
|---|---|
| 权限 | 新增接口均有权限点，P2-B 管理/审批、P2-C 管理/发布/模拟均区分权限 |
| 幂等 | 新增写操作和模拟操作均要求 `idempotency_key`，符合当前系统审计口径 |
| 留痕 | 新增 Service 写操作通过既有审计事件路径记录成功/失败事件 |
| SQL 注入 | 当前新增 SQL 使用命名参数和受控枚举，未发现高置信 SQL 注入路径 |
| PII | 团体/包场列表包含联系人电话，当前受 `amusement.group_booking.view` 权限保护；后续 Web 端应避免无权限展示或导出 |
| Semgrep | `p/owasp-top-ten` + `p/secrets` 扫描 0 findings |

## 5. 新鲜验证证据

以下命令均在 160 服务器 `/home/james/complex-management` 执行。

| 命令 | 结果 |
|---|---|
| `git diff --check` | 通过，无空白/补丁格式错误 |
| `npm run test:backend` | 通过，Maven 测试 `543` 个，失败 `0`，错误 `0`，跳过 `0` |
| `npm run verify:p0-contract` | 通过，OpenAPI、示例、operation 矩阵、追溯附录、客户端路径、客户端 OpenAPI 覆盖和 payload 策略均通过 |
| `~/.local/bin/semgrep scan --config p/owasp-top-ten --config p/secrets --metrics=off --json --output security-scans/latest-semgrep-output.json backend apps/web packages tools` | 通过，扫描 `464` 个 git 跟踪文件，0 findings |
| `npm run typecheck` | 通过，Web TypeScript 检查通过 |
| `npm run build` | 通过，Vite 构建成功；存在大 chunk 警告，不阻断候选 A，候选 B 可顺手观察但不做无关拆包 |

## 6. 保护性基线建议

1. 将当前 P2-B/C/D/E/F 后端、迁移、测试、文档和本审计报告作为一个保护性提交。
2. 提交后立即进入候选 B：P2 Web 管理入口补齐。
3. 候选 B 不改变 P2-B/C/D 后端业务语义；若发现接口缺口，先补测试再补后端。
4. P3/T3 只在候选 B 完成并验证后评估，不在当前基线内扩写。

## 7. 审计结论

当前 P2 差异可以进入保护性基线。没有发现需要阻断提交的 P0/P1 安全漏洞或业务方向错误；但必须先提交保护点，再进行 Web 入口编码。
