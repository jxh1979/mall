# Semgrep 安装与扫描报告

## 1. 执行环境

| 项目 | 结果 |
|---|---|
| 执行机器 | 192.168.88.160 / `y9000p-ai` |
| 执行用户 | `james` |
| 安装方式 | `pipx install semgrep` |
| Semgrep 版本 | `1.166.0` |
| Semgrep 路径 | `/home/james/.local/bin/semgrep` |
| 用户级 shim | `/home/james/bin/semgrep` |
| 引擎 | OSS only，未检测到 Semgrep Pro |
| 遥测 | 所有扫描命令均使用 `--metrics=off` |

## 2. 扫描范围

扫描输出目录：

`/home/james/complex-management/security-scans/static_analysis_semgrep_2`

扫描范围：

- `backend`
- `apps/web`
- `packages/shared`
- `tools`
- `ops`

排除范围：

- `apps/api` 历史 TypeScript/Fastify/SQLite 预研原型
- `node_modules`
- `backend/target`
- `security-scans`

检测到的主要技术：

| 类型 | 数量 |
|---|---:|
| Java | 337 |
| SQL migration | 20 |
| TypeScript | 27 |
| TSX | 2 |
| JavaScript | 27 |
| JSX | 1 |
| YAML | 6 |
| JSON | 26 |
| Dockerfile | 2 |

## 3. 规则集

模式：Important only。

Semgrep 1.166 的严重级别枚举为 `INFO`、`WARNING`、`ERROR`，因此本次用 `WARNING`、`ERROR` 作为预过滤，之后再按 `category=security`、`confidence=MEDIUM/HIGH`、`impact=MEDIUM/HIGH` 做后过滤。

官方规则集：

- `p/security-audit`
- `p/secrets`
- `p/owasp-top-ten`
- `p/cwe-top-25`
- `p/java`
- `p/findsecbugs`
- `p/typescript`
- `p/javascript`
- `p/react`
- `p/nodejs`
- `p/dockerfile`

第三方规则集：

- Trail of Bits
- elttam
- kondukto
- federicodotta
- Atlassian Labs
- Apiiro

说明：`p/spring` 和 `p/yaml` 在当前 Semgrep Registry 返回 404，未纳入执行；Spring 相关覆盖由 `p/java`、`p/findsecbugs`、`p/security-audit` 承担。

## 4. 扫描结果摘要

| 规则集 | 原始结果 | Important 结果 | 错误 |
|---|---:|---:|---:|
| `baseline-cwe` | 0 | 0 | 0 |
| `baseline-owasp` | 2 | 0 | 2 |
| `baseline-secrets` | 0 | 0 | 0 |
| `baseline-security-audit` | 0 | 0 | 0 |
| `java-findsecbugs` | 11 | 11 | 0 |
| `java-java` | 0 | 0 | 0 |
| `js-javascript` | 0 | 0 | 0 |
| `js-nodejs` | 0 | 0 | 0 |
| `js-react` | 0 | 0 | 0 |
| `ops-dockerfile` | 0 | 0 | 0 |
| `thirdparty-apiiro` | 0 | 0 | 7 |
| `thirdparty-atlassian` | 0 | 0 | 19 |
| `thirdparty-dotta` | 0 | 0 | 0 |
| `thirdparty-elttam` | 0 | 0 | 2 |
| `thirdparty-kondukto` | 0 | 0 | 0 |
| `thirdparty-trailofbits` | 0 | 0 | 2 |
| `ts-typescript` | 0 | 0 | 0 |

合并 SARIF：

- 文件：`/home/james/complex-management/security-scans/static_analysis_semgrep_2/results/results.sarif`
- SARIF runs：17
- SARIF 原始 findings：13

## 5. 发现项复核

结论：未发现需要立即修复的高置信 Semgrep 漏洞。

| 规则 | 文件 | 结论 |
|---|---|---|
| `gitlab.find_sec_bugs.HARD_CODE_KEY-*` | `backend/src/main/java/com/complex/platform/amusement/AmusementService.java` | 误报。规则将业务状态字符串 `"pass"` 识别为硬编码 key。 |
| `gitlab.find_sec_bugs.HARD_CODE_KEY-*` | `backend/src/main/java/com/complex/platform/bar/BarService.java` | 误报。规则将年龄验证结果字符串 `"passed"` 识别为硬编码 key。 |
| `gitlab.find_sec_bugs.HARD_CODE_KEY-*` | `backend/src/main/java/com/complex/platform/integration/OpenPlatformSecretResolver.java` | 误报。命中的是配置前缀常量 `complex.open-platform.secret-refs.`，不是密钥值。 |
| `generic.nginx.security.request-host-used.request-host-used` | `ops/160/complex-runtime-config-check.sh` | 误报。命中的是从 JDBC URL 解析出的 `host` 变量，不是 Nginx `$host` 或 HTTP Host 请求头。 |

## 6. 扫描限制

- 当前为 Semgrep OSS，未启用跨文件 Pro taint 分析。
- 部分第三方规则库在 Semgrep 1.166 下存在规则解析或内部匹配错误，原始错误已保留在 `raw/*.json` 和 `raw/*.stderr`。
- `baseline-owasp` 对部分 Bash 脚本有解析错误，但未影响 Java/Spring Boot 主代码扫描。

## 7. 建议

- 本轮 P1 酒吧账单调整切片无需因 Semgrep 结果追加修复。
- 建议后续把 Semgrep 加入 160 的固定验证脚本，但默认采用 Important-only，避免大量低价值噪音。
- 如果后续要做生产安全基线，可单独做一次 Java/Spring 的 Pro 或 CodeQL 扫描，补足跨文件数据流分析。
