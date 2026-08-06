# R8 变更台账片段 · 实施代理 C2（账本重建 + 待裁决落票 + HTML 建议草案）

- 日期：**2026-08-05**
- 范围：`docs/prd-audit/` 目录内的两份 CSV 与本轮报告；`docs/prd-audit-fix-change-log-20260804.md`（**仅追加**）；本地 scratch 与 r8 工作区文件
- **三份业务 HTML 一字未动**（sha256 前 8 位复测 `085918bd` / `5081f029` / `a4c02dab`，与本轮开始时一致）
- **未执行** `git add` / `git commit` / `git push` / 任何 git 写操作
- **未修改** `docs/` 根目录下 prd-audit 目录以外的任何 `.md`，唯一例外是 `prd-audit-fix-change-log-20260804.md`，且**只在文件末尾追加**（已用 `after.startswith(before)` 断言原文零改动）

---

## 一、authority-rule-ledger.csv（844 行 → 901 行）

| 序 | 改动 | 条数 | 说明 |
|---|---|---|---|
| 1 | `anchor_or_line` 重锚 | 768 行写入新行号 | EXACT 665 ＋ FUZZY 103；其中 760 行行号实际发生位移 |
| 2 | `anchor_or_line` 标 ORPHANED | 76 行 | 写为 `ORPHANED(旧记 NNN)`，保留旧值，不臆造新行号 |
| 3 | 追加 ENUM 未登记条目 | +45 行 | BEQ-0123 ～ BEQ-0167 |
| 4 | 追加 BSM 新规则 | +11 行 | BSM-0531 ～ BSM-0541 |
| 5 | 追加 PGM 新规则 | +1 行 | PGM-0193（PGM L478 游乐容量） |

- 新行 `decision_status` 一律 `AUTHORITATIVE`；`source_text` 逐字取自当前 HTML 对应行。
- 新行的 11 个要素列一律 `NOT_EXTRACTED_R8`（不复用 `NOT_STATED`，避免把「没做抽取」伪装成「HTML 没写」）。
- **未改写任何既有行的 `source_text`**。

## 二、html-prd-coverage.csv（853 行 → 910 行）

| 序 | 改动 | 条数 | 说明 |
|---|---|---|---|
| 1 | FULL → PARTIAL ＋ finding | 2 行 | BEQ-0004（R8-F01;R8-F02）、BEQ-0066（R8-F06） |
| 2 | NOT_APPLICABLE → FULL | 2 行 | BSM-0530、PGM-0192，`prd_path` 指向 service-order PRD L3 |
| 3 | `prd_path` 改指 service-order PRD | 11 行 | BSM-0190～0199（去掉 5 处指向决策日志的错配）、BEQ-0113/0114 |
| 4 | 追加新映射行 | +57 行 | `phase=r8-rebuild`；FULL 12 / PARTIAL 45 |
| 5 | 新行带 finding | 2 行 | BSM-0540（R8-F03）、BSM-0541（R8-F04;R8-F05） |

状态分布：FULL 842→854、PARTIAL 9→56、NOT_APPLICABLE 2→0。

> **偏差**：R8-F03/F04/F05 在旧账本里**没有可改的既有行**（BSM L748、L1498 从未被登记），因此改为「先补登记规则行、再挂 finding」。R8-F01 与 R8-F02 落在同一条规则（ENUM L34），合并为一行。

## 三、新建文件

| 路径 | 内容 |
|---|---|
| `docs/prd-audit/rounds/r8/ledger-rebuild-report.md` | 重建报告（统计 / ORPHANED 76 条清单 / 新增行清单 / 方法 / 未做事项） |
| `docs/prd-audit/rounds/r8/reanchor-manual-review.csv` | 待人工核对清单 179 行（FUZZY 103 ＋ ORPHANED 76，含旧文→新文对照） |
| `docs/prd-audit/rounds/r8/reanchor-text-drift.csv` | 正文漂移清单 183 行（GAP-35 的量化范围） |
| `docs/prd-audit/rounds/r8/changelog-imp2.md` | 本文件 |

## 四、追加（不改既有行）

`docs/prd-audit-fix-change-log-20260804.md`：文件末尾追加 **§7.4A GAP 状态总表（2026-08-05 R8 补建）**，含 41 条 GAP 一行一状态 ＋ 三条勘误注。文件行数 2219 → 2281。

关键结论：
- 41 条中只有 **GAP-25 / GAP-36** 两条可标「已闭合(文档层)」，**没有任何一条可标「已闭合(全)」**（本地快照无代码仓）；
- **勘误①**：§7.4 抬头 L1987 写「合计 34 项」、L2041 自证句写「GAP × 25」，实测 41 行——三个数互不相等，「行数与声明一致」这句自证本身不成立；
- **勘误②**：GAP-07 与三份 PRD 的 ★GAP-07（PII 加密，已完成）撞号，R8 已由并行代理把三份 PRD 改为 ★SEC-04，`compliance-and-personal-information-protection-requirements-v0.1.md` L166 仍为旧写法待复核。

## 五、本地文件（不进 PR，不在 docs/ 内）

| 路径 | 内容 |
|---|---|
| `D:\AI\ccwork\.scratch\prd-audit-r8\issues\01..10-*.md` | B 类待裁决 10 张票 |
| `D:\AI\ccwork\prd-audit-r8\HTML修改建议草案.md` | C 类 HTML 修改建议 8 条（逐句草案，等批准，绝不实施） |
| `D:\AI\ccwork\prd-audit-r8\scripts\*.py` | 5 个可复算脚本 |
| `D:\AI\ccwork\prd-audit-r8\scripts\out\*` | 中间结果与改前备份 |

## 六、实际执行的命令

```
sha256sum business-system-map-for-operations-v0.1.html platform-governance-map-for-operations-v0.1.html business-enum-quick-reference-v0.1.html
grep -c 'td class="k"' business-enum-quick-reference-v0.1.html      # 150
wc -l *.html                                                        # 1875 / 889 / 255
python reanchor.py                                                  # 试算
python enum_gap.py                                                  # entries 150 matched 105 unregistered 45
python unregistered_lines.py                                        # 161 条无落点内容行
python find_prd.py out/targets.json                                 # 55 条候选承接
python rebuild_ledger.py                                            # 试算（不落盘）
python rebuild_ledger.py --write                                    # 落盘
sed -n 'Np' ...                                                     # 逐行实测 HTML 原文（新增规则、H-01~H-08 草案取证）
```

## 七、遗留

1. 259 行账本正文与当前 HTML 不一致（183 漂移 ＋ 76 ORPHANED），**本轮未改写正文**，GAP-35 仍开放。
2. 重锚后仍有 **161 行** HTML 内容无账本落点（`scripts/out/unregistered-lines.txt`），本轮只补了任务书点名的部分。
3. coverage 中 9 行 `*-MPS-000N` 伪 rule_id 在账本中无对应，**改前即存在**，本轮未动。
4. 45 条新 PARTIAL 行只经机检、**未逐句核验**。
5. B 类 10 张票、C 类 8 条草案**全部等用户裁决**，本轮零实施。
