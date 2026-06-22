# 2026-06-15 Security Audit Fix Report

## Scope

Fixed the four validated findings from `report.md` and two same-family adjacent plaintext credential sinks found during fix review:

1. Customer phone login accepted any nonblank `sms_code`.
2. Customer session `customer_token` was stored and queried in plaintext.
3. Webhook HMAC canonical string did not bind the event payload.
4. Ticket QR `credential_token` was stored and queried in plaintext.
5. Dynamic QR verification ledger payload still stored the raw `cred_` payload after successful verification.
6. Terminal sync task payload could persist raw dynamic QR `cred_` values in `terminal_sync_tasks.payload`.

## Code Changes

- Added customer OTP verifier boundary with local fixed-code implementation and production fail-closed implementation.
- Changed customer sessions to store and query SHA-256 token hashes only.
- Changed webhook signature canonical input to include deterministic `payload_hash`.
- Changed ticket QR credentials to store and query SHA-256 credential hashes only.
- Changed dynamic QR verification ledger payload to store `sha256:<hash>` instead of raw `cred_` payload.
- Changed terminal sync task payload persistence to redact dynamic QR values at both service and JDBC repository boundaries.
- Added Flyway V038 migration to clear or revoke unsafe plaintext customer session and ticket QR credential legacy rows and create hash indexes.
- Added Flyway V039 migration to scrub legacy dynamic QR values from terminal sync task JSON payloads.

## Regression Coverage

- `CustomerServiceTests#phoneLoginRejectsInvalidSmsCodeWithoutCreatingSession`
- `CustomerJdbcIntegrationTests#persistCustomerSessionConsentAndPoints`
- `IntegrationServiceTests#webhookPersistsEventAndDuplicateIdempotencyReturnsOriginalResult`
- `IntegrationServiceTests#webhookRejectsSignatureWhenPayloadChanges`
- `TicketingJdbcIntegrationTests#persistTicketOrderCredentialAndRefundSliceTables`
- `TicketingServiceTests#dynamicCredentialVerificationStoresHashedPayloadOnly`
- `TicketingServiceTests#offlineSyncTaskStoresHashedDynamicCredentialPayloadOnly`
- `TicketingJdbcIntegrationTests#persistTicketingIssueVerificationAndOfflineSyncTables`

## RED Evidence

- Invalid SMS code test initially failed because no exception was thrown.
- Payload-bound webhook signature test initially failed because the service expected the old canonical string without `payload_hash`.
- Customer JDBC test initially failed because `customer_sessions.customer_token` contained `cust_jdbc`.
- Ticketing JDBC test initially failed because `ticket_credentials.credential_token` contained `cred_jdbc`.
- Dynamic QR verification ledger test initially failed because `ticket_verifications.credential_payload` contained a raw `cred_` payload.
- Terminal sync task payload test initially failed because `terminal_sync_tasks.payload.credentialPayload` contained a raw `cred_` payload.
- A Flyway checksum failure was intentionally treated as a quality gate: V038 was restored and terminal sync payload redaction was moved to V039.

## GREEN / Verification Evidence

- `cd backend && mvn -q -Dtest=TicketingServiceTests#offlineSyncTaskStoresHashedDynamicCredentialPayloadOnly test` passed after the service-layer fix.
- `cd backend && mvn -q -Dtest=TicketingJdbcIntegrationTests#persistTicketingIssueVerificationAndOfflineSyncTables test` passed after V039 and repository-layer redaction.
- `npm run test:backend` passed: 547 tests, 0 failures, 0 errors, 0 skipped.
- `npm run verify:p0-contract` passed.
- `npm run docs:security-baseline && npm run verify:security-baseline` passed.
- `~/.local/bin/semgrep --config=p/secrets --config=p/owasp-top-ten --error --no-git-ignore ...` passed on 10 changed security-sensitive files with 0 findings.
- `npm run build` passed; Vite emitted only the existing large-chunk warning.
- `git diff --check` passed.
- Database spot check on 160 returned: `customer_token_plain=0`, `ticket_credential_plain=0`, `terminal_payload_cred=0`, `flyway_version=039`.

## Residual Risk

- V038 revokes legacy customer sessions whose plaintext tokens cannot be safely backfilled; affected customers must log in again.
- V038 clears ticket QR plaintext credentials; legacy rows with non-SHA-256 hashes are revoked and QR codes must be refreshed.
- V039 redacts legacy terminal sync dynamic QR payloads to `sha256:legacy-redacted`; this preserves the audit record while intentionally discarding unrecoverable secret material.
- Production customer OTP remains intentionally fail-closed until the selected SMS or identity provider adapter is configured.
