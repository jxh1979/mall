# Security Audit Fixes Implementation Plan

> For agentic workers: this plan records the TDD implementation tasks completed for the 2026-06-15 adversarial security audit fix pass.

**Goal:** Fix the validated security findings and same-family adjacent plaintext credential sinks without expanding unrelated business scope.

**Architecture:** Java 21, Spring Boot 4, PostgreSQL, Flyway, Maven, JUnit 5, AssertJ, Semgrep.

---

### Task 1: Customer OTP Fail-Closed Login

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/customer/CustomerServiceTests.java`
- Create: `backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java`
- Create: `backend/src/main/java/com/complex/platform/customer/FixedCustomerOtpVerifier.java`
- Create: `backend/src/main/java/com/complex/platform/customer/FailClosedCustomerOtpVerifier.java`
- Modify: `backend/src/main/java/com/complex/platform/customer/CustomerService.java`

- [x] Write a failing test proving an invalid SMS code does not create a customer session.
- [x] Add `CustomerOtpVerifier`; local/dev/test accepts only the configured fixed code, prod fails closed until a real provider is wired.

### Task 2: Customer Session Token Hash-Only Storage

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/customer/CustomerJdbcIntegrationTests.java`
- Modify: `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java`
- Create: `backend/src/main/resources/db/migration/V038__security_credential_hashing_and_webhook_payload_hash.sql`
- Modify: JDBC migration version assertions.

- [x] Write a failing JDBC assertion that `customer_sessions.customer_token` is null and lookup works through `customer_token_hash`.
- [x] Add Flyway migration and repository hashing so new sessions store only SHA-256 token hashes and lookups hash caller-provided tokens.

### Task 3: Webhook Signature Payload Binding

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java`
- Modify: `backend/src/main/java/com/complex/platform/integration/IntegrationService.java`

- [x] Write a failing test proving a signature created for payload A cannot be reused with payload B.
- [x] Add deterministic payload hashing to the webhook canonical string and include the hash in stored result metadata.

### Task 4: Ticket QR Credential Hash-Only Storage

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`
- Modify: `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- Modify: `backend/src/main/resources/db/migration/V038__security_credential_hashing_and_webhook_payload_hash.sql`

- [x] Write a failing JDBC assertion that `ticket_credentials.credential_token` is null, `credential_hash` equals SHA-256(raw token), and lookup still works by caller payload.
- [x] Clear raw QR credential storage and query by hash only.

### Task 5: Dynamic QR Verification Ledger Payload Redaction

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- Modify: `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`

- [x] Write a failing test proving successful dynamic QR verification does not store raw `cred_` in `ticket_verifications.credential_payload`.
- [x] Store `sha256:<hash>` for dynamic QR verification payloads while preserving ordinary entitlement-code audit values.

### Task 6: Terminal Sync Payload Redaction

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- Modify: `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`
- Modify: `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`
- Modify: `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- Create: `backend/src/main/resources/db/migration/V039__security_terminal_sync_payload_redaction.sql`

- [x] Write a failing test proving terminal offline sync task payload does not store raw dynamic QR `cred_` values.
- [x] Redact dynamic QR values at service boundary and JDBC persistence boundary.
- [x] Add V039 migration to scrub legacy terminal sync payload values without changing already-applied V038 checksum.

### Verification

- [x] Targeted failing tests reproduced before fixes.
- [x] Targeted tests passed after fixes.
- [x] `npm run test:backend` passed with 547 tests.
- [x] `npm run verify:p0-contract` passed.
- [x] `npm run docs:security-baseline && npm run verify:security-baseline` passed.
- [x] Semgrep secrets and OWASP scan passed with 0 findings on changed security-sensitive files.
- [x] `npm run build` passed.
- [x] `git diff --check` passed.
- [x] Database spot check on 160 showed no plaintext customer tokens, no plaintext ticket credentials, no `cred_` in terminal sync payloads, and Flyway version 039.
