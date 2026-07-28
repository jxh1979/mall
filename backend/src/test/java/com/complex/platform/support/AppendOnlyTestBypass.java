package com.complex.platform.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 测试清场专用：临时摘掉 append-only 触发器执行一条写语句，随后立即装回（finally 保证）。
 *
 * <p>V344 把 {@code settlement_transactions}（通票分账最高价值账本）纳入 append-only 硬强制后，
 * 集成测试的 {@code @AfterEach} 清场 DELETE 会被数据库触发器拒绝。测试用例必须真提交（依赖
 * AFTER_COMMIT 事件双写），无法退回事务回滚清场，故给测试留这一条<b>显式且刺眼</b>的旁路。
 *
 * <p><b>这不是运行期口子</b>：{@code ALTER TABLE ... DISABLE TRIGGER} 需要表 owner 权限，
 * 生产运行时角色 complex_app 既非 owner（改不动触发器），又被 V344 收回了该表的 UPDATE/DELETE，
 * 两层都堵死。本类仅存在于测试源码树，生产构件不含它。
 */
public final class AppendOnlyTestBypass {

    private AppendOnlyTestBypass() {
    }

    /**
     * 摘掉 {@code settlement_transactions} 的 append-only 触发器执行一条写语句（仅测试清场用）。
     *
     * @return 受影响行数
     */
    public static int update(JdbcTemplate jdbcTemplate, String sql, Object... args) {
        return updateOnTable(jdbcTemplate, "settlement_transactions", "trg_append_only_ud", sql, args);
    }

    /**
     * 摘掉指定表的指定 append-only 触发器执行一条写语句（仅测试清场用）。
     *
     * <p>刻意<b>不</b>与 {@link #update(JdbcTemplate, String, Object...)} 同名重载：同名会让
     * {@code update(jdbc, "DELETE ... IN (?, ?)", a, b)} 这类「SQL + 两个字符串参数」的调用
     * 被静默解析到本方法（把 SQL 当表名、把第一个参数当触发器名），拼出荒谬语句才在运行期炸。
     */
    public static int updateOnTable(JdbcTemplate jdbcTemplate, String table, String trigger,
                                    String sql, Object... args) {
        jdbcTemplate.execute("ALTER TABLE " + table + " DISABLE TRIGGER " + trigger);
        try {
            return jdbcTemplate.update(sql, args);
        } finally {
            jdbcTemplate.execute("ALTER TABLE " + table + " ENABLE TRIGGER " + trigger);
        }
    }
}
