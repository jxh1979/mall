-- V347 专名统一:「娱乐运营平台」→「娱乐平台公司」(2026-07-28 业务 HTML f19ea732 / terminology-canonical §三)。
--   V319 已在生产/测试库应用,其文件内容不可回改(改动破坏 Flyway 校验和致启动失败),
--   故用后续迁移覆盖同一列注释文本。V319 文件里的 `--` 行注释属冻结历史,不落库、不影响任何对象。
--   纯注释文本变更:零结构、零数据、零行为。
COMMENT ON COLUMN settlement_bills.attributed_entity_id IS
    '通票收入归属的 L3 管理公司/业态主体(娱乐平台公司);解析自 revenue_stream_attribution(all_pass_ticket)';
