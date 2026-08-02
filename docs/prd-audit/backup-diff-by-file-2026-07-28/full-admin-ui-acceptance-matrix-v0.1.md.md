## 文件｜`full-admin-ui-acceptance-matrix-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-ui-acceptance-matrix-v0.1.md`
- main：`docs/full-admin-ui-acceptance-matrix-v0.1.md`
- backup SHA-256：`3f5cd004e7fd4d27e9c491c70b35dd95f71e1549de0de30ae08f2ac2736daa93`
- main SHA-256：`42e4ad0a01d8155ffe5ce61fe48371495b23ad64865efb120cc664a8508e0621`
- 大小：21324 → 21443 bytes

- 对齐单元数：备份 90 / main 91

- 差异块数：3
- 相似度（序列比）：0.9724

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | UI-ADM-BAR-001 | 卡座与订台 | 酒吧主管登录 | 预订卡座、转卡、合并、续卡、释放 | 人数限制、跨酒吧限制、未结账限制正确；审计链完整 | table sessions | 覆盖卡座权益不抵扣低消、跨酒吧拒绝 |

**main 现行：**

  + | UI-ADM-BAR-001 | 卡座与订台 | 酒吧主管登录 | 预订卡座、转卡、合并、释放（**无续卡**） | 人数限制、跨酒吧限制、未结账限制正确；审计链完整 | table sessions | 覆盖卡座权益不抵扣低消、跨酒吧拒绝 |

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 已建立完整后台首批截图基线：总控驾驶舱桌面、生产 blocker 与证据桌面、酒吧订单/出品/账单移动端、动态定价移动端。后续建议继续补权限模板复杂权限树、票务策略价格矩阵、卡座订台转卡/合并/续卡弹窗、应收应付台账、供应商适配器密钥脱敏与回调失败详情。

**main 现行：**

  + 已建立完整后台首批截图基线：总控驾驶舱桌面、生产 blocker 与证据桌面、酒吧订单/出品/账单移动端、动态定价移动端。后续建议继续补权限模板复杂权限树、票务策略价格矩阵、卡座订台转卡/合并弹窗（无续卡）、应收应付台账、供应商适配器密钥脱敏与回调失败详情。

### 差异项 3｜仅 main 有（main 新增）｜共 1 条

  + > A2 2026-07-28：移除续卡现行能力表述，对齐 DEC-SEAT-NO-RENEW-001 / bar PRD §7.3A。

**本文件小计：** replace 块 2；仅备份条数 0；仅 main 条数 1；差异块 3

---

