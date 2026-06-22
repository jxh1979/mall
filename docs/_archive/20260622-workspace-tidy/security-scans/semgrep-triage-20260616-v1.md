# Semgrep 扫描分诊报告 2026-06-16

> 目标：把 Semgrep 工具发现转成可执行安全审计结论；本报告不替代正式安全基线，只作为本次静态扫描证据和后续修复入口。

## 扫描摘要

- 初始扫描目录：`/home/james/complex-management`
- 初始结果目录：`security-scans/semgrep-20260616-153210`
- 引擎：Semgrep OSS
- 模式：run-all
- 初始去重发现：13
- 初始严重度：`ERROR 2`，`WARNING 11`
- SARIF：`security-scans/semgrep-20260616-153210/results/results.sarif`
- 修复后定向复扫：`p/findsecbugs` + `*.kt`，`URLCONNECTION_SSRF` 命中数为 0

## 分诊矩阵

| id | severity | triage | file:line | rule | 判断 | next_action |
|---|---|---|---|---|---|---|
| `SG-20260616-001` | P2/ERROR | `resolved_hardened` | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt:48` | `gitlab.find_sec_bugs_kotlin.URLCONNECTION_SSRF_FD-1` | 已补 `buildHealthEndpoint()` 和 `validateApiUrl()`：API Base URL 必须是 HTTPS，除本地 smoke 外 host 必须落在 `COMPLEX_ANDROID_API_ALLOWED_HOSTS` 白名单中；`openConnection()` 前完成校验。 | 已通过 `verify:android-env`、`android:p0:build` 和 Kotlin FindSecBugs 定向复扫。 |
| `SG-20260616-002` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:343` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值，不是密钥材料；属于 FindSecBugs 硬编码密钥规则的字符串误报。 | 不改业务状态枚举；必要时集中增加 Semgrep 豁免说明。 |
| `SG-20260616-003` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:768` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `pass`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-004` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:769` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `not_required`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-005` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:807` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `pass`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-006` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:808` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `not_required`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-007` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:827` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `pass`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-008` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:884` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值，未发现真实密钥字面量。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-009` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java:1012` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为业务状态值 `pass`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-010` | P3/ERROR | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java:518` | `gitlab.find_sec_bugs.IMPROPER_UNICODE-1` | 代码使用 `toLowerCase(Locale.ROOT)`，Semgrep 仍命中 Improper Unicode；当前证据更像规则泛化误报。 | 保留人工复核记录；若后续继续噪声，可在最小范围添加 `nosemgrep` 和原因。 |
| `SG-20260616-011` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/bar/BarAgeVerificationService.java:32` | `gitlab.find_sec_bugs.HARD_CODE_KEY-4` | 命中位置为年龄核验结果状态 `passed`/`accepted`，不是密钥材料。 | 不改业务状态枚举；保留误报记录。 |
| `SG-20260616-012` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/integration/OpenPlatformSecretResolver.java:16` | `gitlab.find_sec_bugs.HARD_CODE_KEY-2` | 命中的是配置前缀常量 `complex.open-platform.secret-refs`，不是实际密钥值；真实密钥通过 env/file/config 引用解析，不在源码中硬编码。 | 保留，不改业务；后续可为常量添加安全注释或规则豁免。 |
| `SG-20260616-013` | P3/WARNING | `false_positive_candidate` | `backend/src/main/java/com/complex/platform/integration/OpenPlatformSecretResolver.java:16` | `gitlab.find_sec_bugs.HARD_CODE_KEY-3` | 与 SG-20260616-012 同源，命中配置前缀常量，不是实际密钥值。 | 保留，不改业务；后续可为常量添加安全注释或规则豁免。 |

## 本次结论

1. 初始扫描没有发现已确认的明文生产密钥泄漏。
2. Android 端 API Base URL 已做白名单加固，避免生产构建变量误配到非授权地址。
3. 其余硬编码密钥类命中主要是业务状态字符串误报，暂不修改业务枚举，避免制造兼容风险。
4. 若后续把 Semgrep 纳入发布闸口，应建立最小豁免文件，避免同类误报反复阻塞。