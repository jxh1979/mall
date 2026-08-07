# R11 shard-07a - implementation-gaps.csv 草案

来源: `docs/prd-audit/rounds/r8/findings.csv` @`1c5c0c7d`,筛选 `status=open` 且属代码类。命中 46 行。

字段逐行照抄自 findings.csv,未改写、未归纳、未补判断。
`implementation_evidence` = `file` + `line` 拼接;`prd_evidence` = `html_evidence`;`affected_component` 由 `file` 路径机械推出;`target_round` 一律 `code-round`;`canonical_findings_id` = finding_id 自身。

```csv
finding_id,prd_evidence,implementation_evidence,affected_component,severity,status,target_round,canonical_findings_id
GAP-01,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,important,open,code-round,GAP-01
GAP-02,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-02
GAP-03,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-03
GAP-04,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,important,open,code-round,GAP-04
GAP-05,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-05
GAP-06,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,important,open,code-round,GAP-06
GAP-07,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-07
GAP-08,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-08
GAP-09,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,important,open,code-round,GAP-09
GAP-10,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-10
GAP-11,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-11
GAP-12,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-12
GAP-13,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-13
GAP-14,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-14
GAP-15,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-15
GAP-16,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-16
GAP-17,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-17
GAP-18,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-18
GAP-19,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-19
GAP-20,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-20
GAP-21,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-21
GAP-22,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-22
GAP-24,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-24
GAP-26,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-26
GAP-27,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-27
GAP-28,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-28
GAP-29,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-29
GAP-30,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-30
GAP-31,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-31
GAP-32,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-32
GAP-33,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-33
GAP-34,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-34
GAP-37,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-37
GAP-38,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-38
GAP-39,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-39
GAP-40,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-40
GAP-41,见 §7.4A 状态依据列,docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A,docs,info,open,code-round,GAP-41
R10-O-01,ENUM「租金/账单类型」条（含能源费/电损费/水损费）,docs/requirement-traceability-matrix-v0.1.md:REQ-110,docs,important,open,code-round,R10-O-01
R10-O-02,-,tools/generate-partial-requirement-mock-coverage.mjs:-,tools,advisory,open,code-round,R10-O-02
R10-O-03,BSM「第三方接入」相关条,docs/third-party-integration-development-design.md:接入异常处置,docs,important,open,code-round,R10-O-03
FINAL-01,BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条：月结定格→账单出账→异议期届满→付账日，后一环不得早于前一环,backend/src/main/java/com/complex/platform/settlement/SettlementPostingService.java:237,settlement,important,open,code-round,FINAL-01
FINAL-02,BSM「域内通票不按经营域比例拆分」条：票务订单产生的积分发行成本，同样在域间拆分之前从整单扣除且只扣一次,backend（通票分账瀑布 S1~S11）:-,backend,important,open,code-round,FINAL-02
FINAL-03,BSM「各域取得份额后，由对应域主管公司决定采用内部结算价」条与酒吧入场可分配池排名口径,backend（酒吧入场可分配池排名档位）:-,backend,important,open,code-round,FINAL-03
FINAL-04,BSM「营业额＝该商户当期在本综合体内含税实收成交总额，减去当期已成功的退款金额，减去由该商户以外的出资方承担的赠送与补贴」条,backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java:1071,finance,important,open,code-round,FINAL-04
FINAL-06,-,tools/verify-entry-anchors.mjs:检查⑤,tools,important,open,code-round,FINAL-06
FINAL-10,"门禁检查③的正则为 `/实测s**{0,2}Ld+/g`，只覆盖「实测 L123」「实测 **L123**」写法；收官批 T3 清掉的是另一种写法「业务:1340」「治理:270、778」「业务:562-565」（文件代号中文简称 ＋ 冒号 ＋ 行号），该正则对其 0 命中。",tools/verify-entry-anchors.mjs:检查③ LINE_ANCHOR,tools,advisory,open,code-round,FINAL-10
```

## 缺 file / line 的 finding_id

### line 列为空或 `-`(implementation_evidence 只能填到文件级)

- `R10-O-02` — file=`tools/generate-partial-requirement-mock-coverage.mjs`,line=`-`
- `FINAL-02` — file=`backend（通票分账瀑布 S1~S11）`,line=`-`
- `FINAL-03` — file=`backend（酒吧入场可分配池排名档位）`,line=`-`

### file 列不是可解析路径(写的是自然语言描述,非文件名)

- `FINAL-02` — file=`backend（通票分账瀑布 S1~S11）`
- `FINAL-03` — file=`backend（酒吧入场可分配池排名档位）`

### 全部 46 行的 file 列均非空

findings.csv 中无 `file` 列为空的代码类 open 行。
