-- BI S2:应收账龄/欠费分析的查询谓词索引(direction+due_date,只索引未结清行)。
CREATE INDEX IF NOT EXISTS idx_ar_ap_items_receivable_open_due
    ON ar_ap_items (direction, due_date)
 WHERE status <> 'closed' AND unpaid_amount_cent > 0;
