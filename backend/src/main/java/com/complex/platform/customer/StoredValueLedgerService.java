package com.complex.platform.customer;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BD 段第 364 条 364-R1（BSM:833「运营端提供储值台账，可按客人、商户、期间查询逐笔明细与余额构成。」「商户的员工只能管理本商户发行的储值卡。」；
 * ENUM:86「每笔充值拆为本金与赠送金两段分别记账」；PGM:659「所有业务数据默认按项目隔离」「在所分配的权限内可以查看、处理该角色可查看项目的数据」
 * 「商户账号持有这类角色也不能跨项目」）：储值台账查询——按客人（完整手机号）、商户（发卡或扣款）、期间（北京时间日期，含首尾）查逐笔明细，
 * 每笔给本金、赠送金变动及变动后的本金、赠送金与余额。范围：平台管理员与最高权限不限；商户账号只到本商户发的卡（卡带项目的只到本人项目）；
 * 其余按 customer.stored_value.view 可触及的项目（本人项目与可跨项目角色勾选的项目），不带项目的商户卡按发卡商户的经营项目判。
 * 手机号走请求体；查询不扣单次授权、不记审计事件，查了谁由外壳敏感页登记（PGM:878，同退卡查卡）。
 */
@Service
public class StoredValueLedgerService {

    static final String VIEW = "customer.stored_value.view";

    /** 查询条件，全部可空。 */
    public record QueryRequest(String phone, String merchantId, LocalDate dateFrom, LocalDate dateTo, Integer page, Integer size) {
    }

    /** 台账一行：一笔储值流水。cardType＝general（综合体通用卡）或 merchant（商户卡）；consumeMerchant＝消费与回冲行的扣款商户。 */
    public record LedgerRow(String txnId, Instant createdAt, String txnType, long amountCent, long principalCent, long bonusCent,
            long balanceAfterCent, long principalAfterCent, long bonusAfterCent, String accountId, String cardType, String projectId,
            String issuerMerchantId, String issuerMerchantName, String customerId, String maskedPhone, String paymentChannel,
            String consumeMerchantId, String consumeMerchantName, String originalTxnId, String reason) {
    }

    private final AuthorizationService authorizationService;
    private final CustomerRepository repository;
    private final NamedParameterJdbcTemplate jdbc;

    public StoredValueLedgerService(AuthorizationService authorizationService, CustomerRepository repository,
            NamedParameterJdbcTemplate jdbc) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.jdbc = jdbc;
    }

    public PageResponse<LedgerRow> query(QueryRequest request) {
        authorizationService.requirePermission(VIEW);
        QueryRequest req = request == null ? new QueryRequest(null, null, null, null, null, null) : request;
        int page = req.page() == null ? 1 : req.page();
        int size = req.size() == null ? 20 : req.size();
        if (page < 1 || size < 1 || size > 100) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分页参数无效");
        }
        PageResponse<LedgerRow> empty = new PageResponse<>(List.of(), page, size, 0);
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> accountWhere = new ArrayList<>();
        String phone = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();
        if (phone != null) {
            CustomerRecord customer = repository.findCustomerByPhone(phone).orElse(null);
            if (customer == null) {
                return empty;
            }
            accountWhere.add("a.customer_id = :customer");
            params.addValue("customer", customer.id());
        }
        AuthorizationService.ProjectReach reach = authorizationService.projectReach(authorizationService.currentActor(), VIEW);
        if (!reach.platformAdmin()) {
            String merchant = authorizationService.currentMerchantIdQuietly().orElse(null);
            if (merchant != null) {
                accountWhere.add("a.merchant_id = :own_merchant AND (a.project_id IS NULL OR a.project_id = :own_project)");
                params.addValue("own_merchant", merchant).addValue("own_project", reach.ownProject());
            } else {
                Set<String> projects = reach.organizationIds();
                if (projects.isEmpty()) {
                    return empty;
                }
                accountWhere.add("(a.project_id IN (:projects) OR (a.project_id IS NULL AND a.merchant_id IN ("
                        + "SELECT id FROM merchants WHERE project_id IN (:projects)"
                        + " UNION SELECT merchant_id FROM merchant_projects WHERE project_id IN (:projects))))");
                params.addValue("projects", projects);
            }
        }
        List<String> rowWhere = new ArrayList<>();
        if (req.dateFrom() != null) {
            rowWhere.add("tx.created_at >= :from");
            params.addValue("from", Timestamp.from(req.dateFrom().atStartOfDay(LocalZone.ID).toInstant()));
        }
        if (req.dateTo() != null) {
            rowWhere.add("tx.created_at < :to");
            params.addValue("to", Timestamp.from(req.dateTo().plusDays(1).atStartOfDay(LocalZone.ID).toInstant()));
        }
        String merchantFilter = req.merchantId() == null || req.merchantId().isBlank() ? null : req.merchantId().trim();
        if (merchantFilter != null) {
            rowWhere.add("(acc.merchant_id = :merchant OR cm.id = :merchant)");
            params.addValue("merchant", merchantFilter);
        }
        params.addValue("size", size).addValue("offset", (long) (page - 1) * size);
        AtomicLong total = new AtomicLong();
        // 变动后本金、赠送金先按卡的全部流水累计，再按期间与商户过滤，余额才对。
        // 修复项 F07：退卡、消费回冲、账户关闭与随后的赠送金作废（adjust）同一事务同一时刻写入，流水号随机；
        // 同一时刻的作废行固定排在后面（外层倒序同向），不靠流水号定先后。
        List<LedgerRow> rows = jdbc.query("""
                WITH acc AS (
                    SELECT a.id, a.project_id, a.merchant_id FROM customer_stored_value_accounts a %s
                ), tx AS (
                    SELECT t.*,
                           SUM(t.principal_cent) OVER w AS principal_after_cent,
                           SUM(t.bonus_cent) OVER w AS bonus_after_cent
                      FROM customer_stored_value_transactions t JOIN acc ON acc.id = t.account_id
                    WINDOW w AS (PARTITION BY t.account_id ORDER BY t.created_at, (t.txn_type = 'adjust'), t.id ROWS UNBOUNDED PRECEDING)
                )
                SELECT tx.id, tx.created_at, tx.txn_type, tx.amount_cent, tx.principal_cent, tx.bonus_cent, tx.balance_after_cent,
                       tx.principal_after_cent, tx.bonus_after_cent, tx.account_id,
                       CASE WHEN acc.merchant_id IS NULL THEN 'general' ELSE 'merchant' END AS card_type,
                       acc.project_id, acc.merchant_id AS issuer_merchant_id, im.merchant_name AS issuer_merchant_name,
                       tx.customer_id, c.masked_phone,
                       CASE WHEN tx.txn_type = 'topup' THEN COALESCE(b.payment_channel, fpi.channel) END AS payment_channel,
                       cm.id AS consume_merchant_id, cm.merchant_name AS consume_merchant_name, tx.original_txn_id, tx.reason,
                       COUNT(*) OVER () AS total
                  FROM tx
                  JOIN acc ON acc.id = tx.account_id
                  LEFT JOIN customers c ON c.id = tx.customer_id
                  LEFT JOIN merchants im ON im.id = acc.merchant_id
                  LEFT JOIN stored_value_topup_batches b ON b.id = tx.id
                  LEFT JOIN finance_payment_intents fpi ON fpi.id = tx.payment_intent_id
                  LEFT JOIN customer_stored_value_transactions ot ON tx.txn_type IN ('consume', 'consume_reversal')
                        AND ot.id = COALESCE(tx.original_txn_id, tx.id)
                  LEFT JOIN ticket_payment_intents tpi ON tpi.id = ot.id
                  LEFT JOIN ticket_orders o ON o.id = tpi.business_order_id
                  LEFT JOIN bar_bill_settlements bs ON acc.merchant_id IS NULL AND left(ot.idempotency_key, 6) = 'barsv_'
                        AND bs.id = 'settle_' || substr(ot.idempotency_key, 7)
                  LEFT JOIN merchants cm ON ot.id IS NOT NULL AND cm.id = COALESCE(acc.merchant_id, o.bar_id, bs.merchant_id)
                 %s
                 ORDER BY tx.created_at DESC, (tx.txn_type = 'adjust') DESC, tx.id DESC
                 LIMIT :size OFFSET :offset
                """.formatted(where(accountWhere), where(rowWhere)), params, (rs, n) -> {
                    total.set(rs.getLong("total"));
                    return new LedgerRow(rs.getString("id"), rs.getTimestamp("created_at").toInstant(), rs.getString("txn_type"),
                            rs.getLong("amount_cent"), rs.getLong("principal_cent"), rs.getLong("bonus_cent"), rs.getLong("balance_after_cent"),
                            rs.getLong("principal_after_cent"), rs.getLong("bonus_after_cent"), rs.getString("account_id"),
                            rs.getString("card_type"), rs.getString("project_id"), rs.getString("issuer_merchant_id"),
                            rs.getString("issuer_merchant_name"), rs.getString("customer_id"), rs.getString("masked_phone"),
                            rs.getString("payment_channel"), rs.getString("consume_merchant_id"), rs.getString("consume_merchant_name"),
                            rs.getString("original_txn_id"), rs.getString("reason"));
                });
        return new PageResponse<>(rows, page, size, total.get());
    }

    private static String where(List<String> conditions) {
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }
}
