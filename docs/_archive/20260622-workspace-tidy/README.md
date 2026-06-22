# 工作区整理归档 · 2026-06-22

本批次为只读评估后、经用户逐项确认的归档（**移动而非删除**），全部可用 `git revert <提交>` 整批撤销，或 `git mv` 单文件回退。

- 源分支：`feat/business-html-audit-and-coding-closure`
- 整理前 HEAD：`a3117b7`

## codex-p0-notes/
仓库根原有 25 个 `.codex-*.md` —— Codex 逐 slice 的 P0 修复工作笔记。P0 已全部完成，作为历史过程记录归档至此，不再散落仓库根。

## security-scans/
原 `security-scans/` 下的历史 semgrep 滚动扫描产物整体下沉：dated 扫描目录/JSON/日志、`static_analysis_semgrep_*`、`2026-06-15-full-repo-adversarial-audit/`、`nogit_20260605-000000/`（旧栈快照）、`latest-*` 滚动指针等。

依据：`security-scans/LEGACY-SECURITY-SCANS.md` 已声明这些基于旧 TS/Fastify/Prisma/SQLite 预研原型，仅作历史追溯，**非当前 Java21+Spring 正式安全基线**。

`security-scans/` 根保留的当前有效件：`formal-security-baseline-v1.md`、`findings-register-v1.md`、`threat_model.md`、`LEGACY-SECURITY-SCANS.md`。

## 未纳入本批次
- `semgrep-current/`：当期滚动产物，已 gitignore，保持原位。
- docs/ 过程件：本轮未发现「被同名新版取代」的确证项，未移动。
