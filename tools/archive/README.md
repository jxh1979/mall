# tools/archive

一次性历史脚本归档（4.7）。此处脚本为早期开发/重建过程中的一次性工具，**已无任何引用**
（package.json/CI/docs 零引用），从仓库根目录移入此处以保持根目录整洁，保留供历史追溯。

- `inject-*-openapi.mjs`：早期各域 OpenAPI 片段注入脚本（现 OpenAPI 由 docs/openapi 与生成器维护）。
- `shot-*.mjs`：早期截图/UI 重建辅助脚本。

如需复用，直接从此目录运行；不再纳入常规构建/校验链。
