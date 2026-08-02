## 文件｜`dividend-engine-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/dividend-engine-prd-v0.1.md`
- main：`docs/dividend-engine-prd-v0.1.md`
- backup SHA-256：`4bedc43faf3f2eaec36e35db80911dd00263283021d0f88c74f58772191cd4f8`
- main SHA-256：`dc3367024ceff0e1d3b97455071da56476e96f4f5431ea791e784470c57e312f`
- 大小：24607 → 25526 bytes

- 对齐单元数：备份 172 / main 180

- 差异块数：2
- 相似度（序列比）：0.9773

### 差异项 1｜仅 main 有（main 新增）｜共 7 条

  + ## 4A. 跨周期维度补充（P2，2026-07-28）
  + 闭合 BSM-0411 `cross_period`（与 §4 期间作业一致，明示验收）：
  + 1. **正式周期 vs 预览**：默认季度正式 + 月度预览；切换为月度正式时，预览规则按方案配置，不得两套正式单重叠同一官方期间。
  + 2. **跨季/跨月重算**：已 `approved/paid` 的正式单不因后期费用调整自动重算；需冲销/作废后按权限重开期间作业。
  + 3. **关账锁**：正式非 preview 作业锁定费用侧期间后，跨周期补录费用不得无声进入已锁定分红基数；须走解锁/冲销策略（见既有 period-lock）。
  + 4. **比例/税改配置**：改方案只影响新期间；历史单按单内快照。
  + 5. **权限/证据**：跨期冲销、重算、解锁须最高权限或配置权限 + 全链路审计。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §4A | 跨周期维度明示（P2 BSM-0411） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 8；差异块 2

---

