# APPENDIX A2 收口（2026-07-28）

用户选择 **A2**：直接修订附录正文。

## 已改
1. `p1-bar-table-session-api-contract-v0.1.md` — 废止续卡/renew
2. full-admin SRS / UI 验收 / development-ready-spec — 去掉续卡现行能力
3. full-admin 矩阵 CSV 三份 — 去掉 renew 权限/入口/事件
4. `member-loyalty-and-stored-value-development-design-v0.1.md` — MAIN §7 / DEC-SV-* 覆盖 + 实现缺口

## 未做
- 未改代码/OpenAPI 生成/控制器
- 若代码仍暴露 renew，属实现债，不在本 PR

## 权威
转台/并台：bar PRD §7.3A；储值：member-loyalty PRD §7；续卡：DEC-SEAT-NO-RENEW-001
