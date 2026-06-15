# 2026-06-15 T3-B 供应商 SDK/mock 套件实施计划

机器标记：T3B_SUPPLIER_MOCK_PLAN_READY
机器标记：allowed_scope=sdk_mock_examples_only
机器标记：no_real_vendor_control
机器标记：no_real_secret

## 目标

在供应商真实 SDK/API 未到位前，补齐 T3-B 最小可用联调材料：开放平台签名工具、Webhook 签名示例、provider adapter mock 测试样例和机器校验脚本。该切片不接入真实机器人、视频、设备或支付供应商，不写真实密钥，不产生生产控制动作。

## 修改文件

- 新增 `tools/verify-t3b-supplier-mock-kit.mjs`。
- 新增 `tools/t3b-open-platform-signature-helper.mjs`。
- 新增 `docs/t3b-supplier-sdk-mock-kit-v0.1.md`。
- 新增 `docs/supplier-mock/*.example.json` 三份样例。
- 修改 `package.json` 增加 `verify:t3b-supplier-mock-kit`。
- 修改 `docs/project-progress-dashboard.md` 和 `docs/00-current-effective-document-entry-v0.1.md`，补 T3-B 入口。

## 数据结构/接口变化

无数据库、OpenAPI、Controller、Service、Repository 变化。全部复用现有 `/api/open/*` 和 `/api/v1/integration/provider-adapters/{id}/test-mock`。

## 测试点

1. RED：缺套件文档、样例、签名工具或入口引用时，`verify:t3b-supplier-mock-kit` 必须失败。
2. GREEN：补齐签名工具和样例后，`verify:t3b-supplier-mock-kit` 必须通过。
3. 签名工具自测：用固定 mock key 计算 auth canonical 的 HMAC-SHA256，与 Node crypto 期望值一致。
4. 安全：Semgrep 0 findings；套件不出现真实密钥、生产 token、真实供应商控制语义。
5. 回归：`verify:t3-coding-package`、`verify:t3a-readonly-analytics`、`verify:p0-contract`、`test:backend`、`build`、`verify:security-baseline` 必须通过。

## 验收标准

- 供应商可按文档生成开放平台 auth token 签名和 Webhook 签名。
- 样例路径必须来自当前 OpenAPI，不新增非正式路径。
- 样例只使用 mock provider 和占位签名，不包含真实密钥。
- T3-B 不标记任何真实供应商生产就绪。

## 风险和回滚

风险：样例与后端 canonical 逻辑漂移。回滚方式是撤回 T3-B 套件文件和 package 验证入口，不影响后端生产路径。
