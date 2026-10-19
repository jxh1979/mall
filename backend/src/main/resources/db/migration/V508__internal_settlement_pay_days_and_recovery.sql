-- 文件版本：V0.01
-- V508：FS-021 负额结算追收与普通内部结算付账日。
-- 依据：BSM L755「平台内部各主体之间的应收应付，一律按总额收付、收支两条线，不得轧差」；BSM L804「每一段冲回都生成反向结算记录，
--   不覆盖原归属、原计算与原结算记录」；BSM L1673「三个付账日为业务参数（全平台统一可配，初始为每月 5 日、15 日、25 日），各笔普通内部
--   结算应付只在配置的付账日中选定……月结定格 → 账单出账 → 异议期届满 → 付账日……系统在保存配置时校验：三个付账日中至少须有一个能满足
--   该顺序」；BSM L1683「每一笔应付具体落在哪一个付账日，由财务人员按其工作负荷统一制定规则，规则一经确定至少一年内不得变更……付款失败
--   待处理……更换收款账户须经收款方所属公司财务负责人审批。重付不得迟于下一个付账日」；BSM L1597 非积分类商户账单异议期（按项目可配，
--   出厂默认 10 个工作日，须同时配置上下限）；ENUM L253 内部应付执行状态；操作员裁决 2026-10-19 第 4 条「单独列追收、按原月重算」。

-- ① 已结付月份后撤销的核销单列追收：当月每家酒吧每支（单店 exclusive_recovery / 分池 pooled_recovery）一条追收明细，
--   按原月结算单拆分的金额与回指留在本表（原月账单、原明细与已付款不回改）。
CREATE TABLE settlement_item_recoveries (
    item_id          VARCHAR(64) NOT NULL REFERENCES settlement_items(id) ON DELETE CASCADE,
    original_bill_id VARCHAR(64) NOT NULL REFERENCES settlement_bills(id),
    amount_cent      BIGINT      NOT NULL,
    PRIMARY KEY (item_id, original_bill_id)
);

-- ② 三个付账日（kind=pay_days，全平台统一）与钱路指派规则（kind=route：该钱路次月出账日 + 指定第几个付账日）。
--   提交与审批分人；按生效起始日版本化；同一钱路新规则生效日不得早于上一已批规则生效日一年后（服务层校验）。
CREATE TABLE internal_pay_day_rules (
    id             VARCHAR(64)  PRIMARY KEY,
    kind           VARCHAR(16)  NOT NULL CHECK (kind IN ('pay_days', 'route')),
    route          VARCHAR(64),
    day_1          INTEGER,
    day_2          INTEGER,
    day_3          INTEGER,
    statement_day  INTEGER,
    pay_slot       INTEGER,
    effective_from DATE         NOT NULL,
    status         VARCHAR(16)  NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    reason         TEXT         NOT NULL,
    submitted_by   VARCHAR(128) NOT NULL,
    submitted_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_by     VARCHAR(128),
    decided_at     TIMESTAMPTZ,
    CONSTRAINT chk_ipdr_pay_days CHECK (kind <> 'pay_days'
        OR (route IS NULL AND day_1 >= 1 AND day_1 < day_2 AND day_2 < day_3 AND day_3 <= 28)),
    CONSTRAINT chk_ipdr_route CHECK (kind <> 'route'
        OR (route IS NOT NULL AND statement_day BETWEEN 1 AND 28 AND pay_slot BETWEEN 1 AND 3)),
    CONSTRAINT chk_ipdr_sod CHECK (decided_by IS NULL OR decided_by <> submitted_by)
);
CREATE INDEX idx_ipdr_effective ON internal_pay_day_rules (kind, route, status, effective_from);
INSERT INTO internal_pay_day_rules (id, kind, day_1, day_2, day_3, effective_from, status, reason, submitted_by, decided_at)
VALUES ('ipdr_initial_pay_days', 'pay_days', 5, 15, 25, DATE '2000-01-01', 'approved',
        '出厂初始值（BSM L1673「初始为每月 5 日、15 日、25 日」）', 'system', now());

-- ③ 非积分商户账单异议期（按项目可配；'__global__' 出厂 10 个工作日；上下限不自拟出厂数值，项目首次配置须同时填写，范式同 V499）。
CREATE TABLE settlement_objection_period_policy (
    project_id   VARCHAR(64) PRIMARY KEY,
    working_days INTEGER     NOT NULL CHECK (working_days >= 0),
    min_days     INTEGER,
    max_days     INTEGER,
    updated_by   VARCHAR(128),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_sopp_bounds CHECK (
        (min_days IS NULL AND max_days IS NULL)
        OR (min_days IS NOT NULL AND max_days IS NOT NULL
            AND min_days >= 0 AND min_days <= working_days AND working_days <= max_days))
);
INSERT INTO settlement_objection_period_policy (project_id, working_days) VALUES ('__global__', 10);

-- ④ 普通内部结算应收应付的付款时点固化（每笔往来一行）：可查看时刻、异议截止、财务指定付账日、按顺序与银行工作日顺延后的到期日。
CREATE TABLE internal_payable_schedules (
    ar_ap_item_id          VARCHAR(128) PRIMARY KEY REFERENCES ar_ap_items(id),
    route                  VARCHAR(64)  NOT NULL,
    project_id             VARCHAR(64),
    counterparty_id        VARCHAR(64)  NOT NULL,
    billing_period_start   DATE         NOT NULL,
    visible_at             TIMESTAMPTZ  NOT NULL,
    objection_deadline     DATE         NOT NULL,
    designated_pay_day     DATE         NOT NULL,
    scheduled_due_date     DATE         NOT NULL,
    pay_days_rule_id       VARCHAR(64)  NOT NULL REFERENCES internal_pay_day_rules(id),
    route_rule_id          VARCHAR(64)  NOT NULL REFERENCES internal_pay_day_rules(id),
    objection_working_days INTEGER      NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ips_counterparty ON internal_payable_schedules (counterparty_id, visible_at);
CREATE INDEX idx_ips_project ON internal_payable_schedules (project_id);

-- ⑤ 内部应付执行：每次付款发起一行（在途 initiated / 到账 succeeded / 明确失败 failed），同一笔同时最多一笔在途。
CREATE TABLE internal_payable_payment_attempts (
    id                  VARCHAR(64)  PRIMARY KEY,
    ar_ap_item_id       VARCHAR(128) NOT NULL REFERENCES ar_ap_items(id),
    attempt_no          INTEGER      NOT NULL,
    amount_cent         BIGINT       NOT NULL CHECK (amount_cent > 0),
    bank_ref            VARCHAR(128),
    account_change_id   VARCHAR(64),
    status              VARCHAR(16)  NOT NULL CHECK (status IN ('initiated', 'succeeded', 'failed')),
    failure_reason      TEXT,
    expected_repay_date DATE,
    initiated_by        VARCHAR(128) NOT NULL,
    initiated_at        TIMESTAMPTZ  NOT NULL,
    resolved_by         VARCHAR(128),
    resolved_at         TIMESTAMPTZ,
    UNIQUE (ar_ap_item_id, attempt_no),
    CONSTRAINT chk_ippa_failed CHECK (status <> 'failed' OR (failure_reason IS NOT NULL AND expected_repay_date IS NOT NULL))
);
CREATE UNIQUE INDEX uq_ippa_in_transit ON internal_payable_payment_attempts (ar_ap_item_id) WHERE status = 'initiated';

-- 更换收款账户：由收款方所属公司财务负责人本人登记即为审批（append-only）。
CREATE TABLE internal_payable_account_changes (
    id                VARCHAR(64)  PRIMARY KEY,
    ar_ap_item_id     VARCHAR(128) NOT NULL REFERENCES ar_ap_items(id),
    failed_attempt_id VARCHAR(64)  NOT NULL REFERENCES internal_payable_payment_attempts(id),
    new_account_ref   VARCHAR(256) NOT NULL,
    reason            TEXT         NOT NULL,
    approved_by       VARCHAR(128) NOT NULL,
    approved_at       TIMESTAMPTZ  NOT NULL
);
ALTER TABLE internal_payable_payment_attempts
    ADD CONSTRAINT fk_ippa_account_change FOREIGN KEY (account_change_id) REFERENCES internal_payable_account_changes(id);

-- 资金不足顺延（顺延三）逐次留痕（append-only）；顺延不另行通知收款方，待付状态对收款方可见。
CREATE TABLE internal_payable_postponements (
    id                VARCHAR(64)  PRIMARY KEY,
    ar_ap_item_id     VARCHAR(128) NOT NULL REFERENCES ar_ap_items(id),
    reason_code       VARCHAR(32)  NOT NULL CHECK (reason_code IN ('funds_insufficient')),
    previous_due_date DATE         NOT NULL,
    due_date          DATE         NOT NULL,
    note              TEXT         NOT NULL,
    recorded_by       VARCHAR(128) NOT NULL,
    recorded_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_ipp_later CHECK (due_date > previous_due_date)
);
CREATE INDEX idx_ipp_item ON internal_payable_postponements (ar_ap_item_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON settlement_item_recoveries TO complex_app;
GRANT SELECT, INSERT, UPDATE ON internal_pay_day_rules, settlement_objection_period_policy, internal_payable_schedules,
    internal_payable_payment_attempts, internal_payable_account_changes, internal_payable_postponements TO complex_app;

-- 收款方所属公司财务负责人审批更换收款账户（商户专区）；平台管理员默认可监督。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('merchant.portal.payee_account.approve', '收款方财务负责人审批更换收款账户', 'merchant_portal', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'merchant.portal.payee_account.approve')
ON CONFLICT (role_id, permission_code) DO NOTHING;
