# 阶段 5：地产、招商、合同、楼宇与供应链审计

状态：**阶段 5 文档审计完成**（覆盖初评 + 强制对抗；**未自动批改业务 PRD 正文**；**无代码变更**）
日期：2026-07-28
基线：`6a79450b`
分支：`audit/prd-phase4-5-docs-closeout`

## 范围
- 本阶段新增规则：**132** 条
- 领域：realestate / leasing / building / supply 及关键词命中的 cross-domain/finance 等
- 权威：三份业务 HTML；MAIN 域 PRD 承接

### 领域分布
| domain | count |
|---|---:|
| building | 40 |
| supply | 36 |
| realestate | 30 |
| leasing | 24 |
| merchant | 2 |

## 覆盖统计（本阶段新增）
| status | count |
|---|---:|
| FULL | 131 |
| PARTIAL | 1 |

| severity | count |
|---|---:|
| P2 | 1 |

coverage 总行数（累计）：**733** / ledger **844**  
仍未覆盖规则：**111**（主要留给 phase6/7：{'amusement': 32, 'thirdparty': 35, 'terminal': 11, 'service': 4, 'merchant': 3, 'data': 26}）

## 对象生命周期审计（摘要）

| 对象 | 创建 | 生效/占用 | 变更 | 中止/终止 | 历史追溯 | 结论 |
|---|---|---|---|---|---|---|
| 项目/楼栋/楼层/区块/铺位/公区 | 空间主数据 | 占用/出租状态 | 拆合变更 | 停用 | 需可追溯 | 见 ADV-RE-001/009 |
| 招商线索/商机/商户 | 招商 PRD | 落位准入 | 变更 | 退出 | 过程留痕 | 见 ADV-RE-002 |
| 合同/证照 | 合同模型+地产 | 生效校验 | 变更版本 | 到期终止 | 扫描件/版本 | ADV-RE-002/003/004 |
| 应收应付 | 财务/结算 | 出账 | 调整 | 核销关闭 | 主体绑定 | ADV-RE-005 |
| 工单 | 楼宇运营 | 派单 | 升级 | 验收关闭 | 责任人 | ADV-RE-006 |
| 采购/收货/库存/调拨/盘点 | 供应链 | 在途/在库 | 调整 | 关闭 | 批次流水 | ADV-RE-007/008 |

## 强制对抗场景
| ID | 场景 | 严重度 | 结果 | 缺口 | 建议 |
|---|---|---|---|---|---|
| ADV-RE-001 | 空间重复出租、拆分或合并后合同仍指向旧空间 | P0 | COVERED |  |  |
| ADV-RE-002 | 合同未签/已过期或证照失效后仍准入 | P0 | COVERED |  |  |
| ADV-RE-003 | 纸质合同与系统扫描件责任边界不清 | P1 | COVERED |  |  |
| ADV-RE-004 | 租金调整、减免、退租和跨期计费无审批或无追溯 | P0 | COVERED |  |  |
| ADV-RE-005 | 应收应付主体与合同主体不一致 | P0 | COVERED |  |  |
| ADV-RE-006 | 工单超时、升级、验收和满意度责任人缺失 | P1 | COVERED |  |  |
| ADV-RE-007 | 采购部分收货、退货、调拨在途和盘盈盘亏导致库存不守恒 | P0 | COVERED |  |  |
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-22，本文不再重复记载。
| ADV-RE-009 | 跨项目或跨商户错误共享空间、库存或费用 | P0 | COVERED |  |  |

## 方法与局限
- 检索启发式 + 维度检查 + 对抗抬升
- `FULL` ≠ 实现已上线
- **docs-only**：不修改业务 PRD 正文、不改代码；OPEN 项登记建议

## 非 FULL 样例
| rule_id | domain | status | sev | finding | summary |
|---|---|---|---|---|---|
| BSM-0267 | leasing | PARTIAL | P2 | RE-BSM-0267-DIM | 部分承接，缺少: expiry_exit |

## 待后续文档迭代（不阻塞本阶段完成标签）


## 权威 HTML 哈希（未改）
| file | sha256 |
|---|---|
| business-system-map-for-operations-v0.1.html | `0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e` |
| platform-governance-map-for-operations-v0.1.html | `a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b` |
| business-enum-quick-reference-v0.1.html | `3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648` |

## 产物
- `html-prd-coverage.csv`（追加 phase5）
- `phase5-adversarial-findings.csv`
- `phase5-issue-rows.csv`
- `phases/05-realestate-leasing-building-supply.md`（本文件）

## 阶段 5 完成声明
- 文档审计步骤（覆盖+生命周期摘要+对抗）已完成
- 无代码变更；无未授权业务立法
