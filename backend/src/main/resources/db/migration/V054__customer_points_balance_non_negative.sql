-- Defense-in-depth backstop: the points account balance must never go negative. The redemption path
-- already guards this with SELECT ... FOR UPDATE inside a transaction, but the table itself lacked the
-- CHECK that customer_points_ledger.balance_after (V025) already enforces.
ALTER TABLE customer_points_accounts
    ADD CONSTRAINT chk_customer_points_accounts_balance_non_negative CHECK (balance >= 0);
