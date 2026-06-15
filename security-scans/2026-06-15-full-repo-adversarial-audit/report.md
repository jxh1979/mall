# Security Review: complex-management

## Scope

- Target: `/home/james/complex-management` on `192.168.88.160`.
- Scan mode: single-agent repository-wide adversarial and security audit using Codex Security phase model. This is not labeled as formal Deep Security Scan fanout because multi-agent delegation was not explicitly authorized.
- In-scope code: `backend`, `apps/web`, `apps/android-terminal`, `apps/api`, `packages`, `tools`, and `database` tracked source/configuration files.
- Runtime and verification artifacts: `security-scans/2026-06-15-full-repo-adversarial-audit/artifacts/`.
- Generated context: threat model was generated during this review and written to `context/threat_model.md`.
- Explicit limitation: archived `apps/api` has no `package-lock.json`, so npm dependency audit cannot produce evidence for that legacy prototype; start/build scripts are still blocked and root lock does not include legacy packages.

### Scan Summary

| Field | Value |
|---|---|
| Reportable findings | 4 |
| Severity mix | high: 2, medium: 2 |
| Confidence mix | high: 4 |
| Coverage | 584 tracked code/config files inventoried; Semgrep scanned 511 tracked files with OWASP Top 10 and secrets rules |
| Validation mode | Static source review, security grep, Semgrep, npm audit, backend tests, web build, Android build, OpenAPI/ops/security gate checks |
| Passing verification | `verify:p0-contract`, `test:backend` 543 tests, `build`, `verify:ops160-recovery`, `verify:android-env`, `android:p0:build`, `git diff --check`, `npm audit --omit=dev` |
| Failed or partial verification | `verify:security-baseline` failed due stale formal baseline operation count; legacy `apps/api` npm audit unavailable due missing lockfile |
| Semgrep | 1 warning: Android exported launcher activity; reviewed and rejected as reportable finding |

Artifact paths:
- Markdown report: `security-scans/2026-06-15-full-repo-adversarial-audit/report.md`
- HTML report: `security-scans/2026-06-15-full-repo-adversarial-audit/report.html`
- Threat model: `security-scans/2026-06-15-full-repo-adversarial-audit/context/threat_model.md`
- Candidate validation: `security-scans/2026-06-15-full-repo-adversarial-audit/validation/candidate_validation.md`
- Attack paths: `security-scans/2026-06-15-full-repo-adversarial-audit/attack-path/attack_paths.md`
- Reviewed surfaces: `security-scans/2026-06-15-full-repo-adversarial-audit/coverage/reviewed_surfaces.md`

## Threat Model

### Assets
- Staff/admin bearer sessions, customer session tokens, ticket QR credentials, device credentials, open platform HMAC secrets, payment/finance records, audit/evidence records, and PostgreSQL data.
- Public customer API, public open-platform API, authenticated staff API, Android terminal, web management UI, 160 operation scripts, NAS recovery scripts, and legacy prototype directory.

### Trust Boundaries
- Internet or intranet caller to `/api/v1/customer/**` and `/api/open/**`.
- Staff/operator bearer token to `/api/v1/**` management APIs.
- Third-party provider webhook callers using HMAC signatures and configured endpoint secrets.
- Android terminal/device caller using device credentials and staff sessions.
- Database/operator boundary, including backup and restore artifacts.

### Primary Attacker Goals
- Impersonate a customer to view or use tickets, points, bar reservations, or QR credentials.
- Modify trusted third-party webhook event content after or around signature generation.
- Reuse leaked bearer-like credentials from database or logs.
- Bypass staff permission checks or exploit legacy prototype/deployment drift.

### Security Invariants Used For This Review
- Production staff sessions should store only hashes of bearer tokens; this is already implemented for `auth_sessions`.
- Customer and ticket bearer-like credentials should follow the same hash-at-rest rule unless there is a documented exception.
- Public endpoints must either be truly public read-only data or prove caller identity with a cryptographic/session control.
- Third-party webhook signatures must bind the security decision to the exact body that is persisted and trusted.
- Legacy TypeScript/Fastify/SQLite prototype code must remain blocked and excluded from production launch paths.

## Findings

| # | Title | Severity | Confidence |
|---|---|---|---|
| 1 | [Customer phone login accepts any nonblank SMS code and mints a session](#1-customer-phone-login-accepts-any-nonblank-sms-code-and-mints-a-session) | high | high |
| 2 | [Customer bearer tokens are stored and queried in plaintext](#2-customer-bearer-tokens-are-stored-and-queried-in-plaintext) | high | high |
| 3 | [Webhook HMAC signature does not bind the request payload](#3-webhook-hmac-signature-does-not-bind-the-request-payload) | medium | high |
| 4 | [Ticket QR credentials are stored and queried in plaintext](#4-ticket-qr-credentials-are-stored-and-queried-in-plaintext) | medium | high |

### Confidence Scale

| Label | Meaning |
|---|---|
| high | Direct source, configuration, or runtime evidence supports the finding, with no material unresolved reachability or exploitability blocker. |
| medium | Source evidence supports a plausible issue, but runtime behavior, deployment configuration, role reachability, type constraints, or exploit reliability still need proof. |
| low | Weak or incomplete evidence; included only when follow-up candidates are useful in the final report. |

### [1] Customer phone login accepts any nonblank SMS code and mints a session

| Field | Value |
|---|---|
| Severity | high |
| Confidence | high |
| Confidence rationale | Source and route evidence show the endpoint is anonymous and the service only checks `sms_code` is nonblank before issuing a session. |
| Category | Authentication bypass |
| CWE | CWE-287 Improper Authentication |
| Affected lines | `backend/src/main/java/com/complex/platform/config/SecurityConfig.java:63-67`, `backend/src/main/java/com/complex/platform/customer/CustomerController.java:32-34`, `backend/src/main/java/com/complex/platform/customer/CustomerService.java:57-90` |

#### Summary
The customer phone login endpoint is under `/api/v1/customer/**`, which is configured as public. The controller forwards `POST /auth/phone-login` directly to `CustomerService.phoneLogin`. The service requires `phone` and a nonblank `sms_code`, then creates or reactivates the customer and mints a `cust_...` session token. No OTP verification service, challenge table, third-party SMS validation, rate limiter, or one-time code consumption appears in the customer package.

#### Validation
- Reviewed `SecurityConfig.publicApiPaths`: `/api/v1/customer/**` is anonymous.
- Reviewed `CustomerController.phoneLogin`: no authentication guard before service call.
- Reviewed `CustomerService.phoneLogin`: line 61 only validates nonblank `sms_code`; lines 82-90 issue a session.
- Grep for `sms`, `otp`, `CustomerPhoneLogin`, `customer_token`, `token_hash`, and `hash` found no OTP verification path in customer implementation.
- Existing tests use static `000000` SMS codes, which reinforces that no live validation exists.

#### Dataflow
Anonymous HTTP caller -> `/api/v1/customer/auth/phone-login` -> `CustomerController.phoneLogin` -> `CustomerService.phoneLogin` -> `repository.saveCustomer` -> `repository.saveSession` -> returned `customer_token` -> subsequent customer APIs trust that token.

#### Reachability
Any caller able to reach the API can submit a target phone number and any nonblank SMS code. After receiving a customer token, the caller can hit customer-scoped APIs for orders, ticket QR credentials, points, redemptions, and bar reservations, constrained only by the data/actions currently available to that customer.

#### Severity
Severity is high because the vulnerable boundary is anonymous and the result is a valid customer session for an attacker-selected phone number. Impact includes account takeover for customer-facing functions and possible misuse of value-bearing tickets, points, or reservations. Severity would be lower only if this endpoint were provably blocked from all production profiles or fronted by an external gateway that performs OTP verification before the request reaches Spring Boot.

#### Remediation
- Do not enable this endpoint in production until SMS/OTP verification is real.
- Add an OTP challenge model: issue code, store hash with TTL and attempt counter, verify once, then consume.
- Integrate the selected SMS provider behind an interface so local/test can stub it while prod fails closed if provider config is missing.
- Add rate limiting by phone/IP/device and audit failed attempts.
- Add negative tests: wrong code, expired code, reused code, too many attempts, missing provider config in prod.

### [2] Customer bearer tokens are stored and queried in plaintext

| Field | Value |
|---|---|
| Severity | high |
| Confidence | high |
| Confidence rationale | Repository and migration evidence show raw `cust_...` tokens are inserted, indexed, selected, and compared as `customer_token`. |
| Category | Sensitive credential stored in plaintext |
| CWE | CWE-256 Plaintext Storage of a Password; CWE-522 Insufficiently Protected Credentials |
| Affected lines | `backend/src/main/java/com/complex/platform/customer/CustomerService.java:82-86`, `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:68-86`, `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:330-338`, `backend/src/main/resources/db/migration/V009__p0_ticketing_customer_orders_credentials.sql:133-144` |

#### Summary
Customer session tokens are bearer credentials. The service returns a raw `cust_...` token, and the repository stores and queries that same raw value in `customer_sessions.customer_token`. This is weaker than the production staff/open/device token patterns already present elsewhere in the repo, where token hashes are stored and raw tokens are returned only once.

#### Validation
- `CustomerService` mints a raw `cust_...` token and passes it to `saveSession`.
- `JdbcCustomerRepository.saveSession` inserts `customer_token` directly.
- `findSessionByToken` queries `WHERE customer_token = :customer_token`.
- Migration `V009` creates and indexes `customer_token` directly.
- No customer token hash column or hash lookup was found in customer implementation.

#### Dataflow
`CustomerService.phoneLogin` -> raw token generated -> `JdbcCustomerRepository.saveSession` -> PostgreSQL `customer_sessions.customer_token` -> later `findSessionByToken` compares attacker-supplied raw token to database raw token.

#### Reachability
A database read path through SQL console, backup, debug export, or compromised operator account yields replayable active customer tokens. The attacker does not need to crack a hash or pass OTP once a non-expired token is obtained.

#### Severity
Severity is high because it turns a database confidentiality incident into immediate customer account access. The impact is broad across customer endpoints that trust `X-Customer-Token`. Severity would be reduced if all customer session tokens were short-lived, hashed at rest, bound to device context, and revocable without exposing raw values in query results.

#### Remediation
- Replace `customer_token` with `customer_token_hash` for lookup and storage; return raw token only in the login response.
- Use at least SHA-256 over high-entropy random tokens; consider an application pepper stored outside the database.
- Add token rotation/revocation and migration that invalidates existing plaintext sessions.
- Add tests mirroring `ProductionAuthJdbcIntegrationTests`: database value is 64 hex chars and not equal to returned token.

### [3] Webhook HMAC signature does not bind the request payload

| Field | Value |
|---|---|
| Severity | medium |
| Confidence | high |
| Confidence rationale | Direct source evidence shows signature verification omits body or body hash while the unbound payload is persisted as trusted webhook content. |
| Category | Insufficient message integrity protection |
| CWE | CWE-345 Insufficient Verification of Data Authenticity |
| Affected lines | `backend/src/main/java/com/complex/platform/integration/OpenPlatformController.java:46-56`, `backend/src/main/java/com/complex/platform/integration/IntegrationService.java:165-205`, `backend/src/main/java/com/complex/platform/integration/IntegrationService.java:967-990`, `backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java:145-170` |

#### Summary
Webhook verification signs `endpoint_key`, `provider`, `event`, and `idempotency_key`, but it does not include the raw request body or a body hash. The exact `payload` map is then persisted in `open_api_events.payload_json` with `signature_valid=true`. That means the security decision is not cryptographically bound to the content the system later trusts.

#### Validation
- `receiveWebhook` verifies signature at line 185 before persisting `payload` at line 200.
- `webhookCanonical` only contains endpoint, event, idempotency, and provider.
- Existing tests compute `webhookSignature(...)` without payload and do not include a tampered-payload negative test.
- OpenAPI examples mention `raw_payload_hash`, but the runtime verification path does not enforce one.

#### Dataflow
Public webhook caller -> `OpenPlatformController.receiveWebhook` -> header signature and JSON payload -> `IntegrationService.verifySignature` over canonical metadata only -> `repository.saveOpenEvent(... payload ...)` -> downstream reconciliation/order/voucher workflows consume trusted event data.

#### Reachability
The endpoint is intentionally public under `/api/open/**`. A malicious provider, compromised provider-side component, or intermediary with access to a valid signature for the same metadata can alter the body before first acceptance under that idempotency key. TLS reduces network tampering but does not solve provider-side or gateway/body-rewrite integrity gaps.

#### Severity
Severity is medium because the attacker must be in or near the provider/webhook delivery path or obtain a valid metadata signature. The impact is integrity loss for third-party events, potentially affecting reconciliation, payment/order sync, voucher verification, or compensation tasks. Severity would rise if production providers drive financial settlement directly from this payload without independent reconciliation.

#### Remediation
- Include `raw_body_sha256` in the signed canonical string, or sign the raw body bytes directly.
- Add `X-Webhook-Timestamp` and `X-Webhook-Nonce` with replay protection separate from idempotency.
- Verify the signature before JSON parsing when possible, using raw request bytes.
- Store the computed payload hash and signature version with the event.
- Add tests where the same signature with a changed payload is rejected.

### [4] Ticket QR credentials are stored and queried in plaintext

| Field | Value |
|---|---|
| Severity | medium |
| Confidence | high |
| Confidence rationale | The ticket credential table stores both `credential_token` and `credential_hash`, but runtime lookup and index use raw `credential_token`. |
| Category | Sensitive credential stored in plaintext |
| CWE | CWE-522 Insufficiently Protected Credentials |
| Affected lines | `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java:485-515`, `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java:1174-1180`, `backend/src/main/resources/db/migration/V009__p0_ticketing_customer_orders_credentials.sql:84-104` |

#### Summary
Ticket QR credentials are value-bearing bearer tokens. The schema already contains `credential_hash`, but the repository persists and queries `credential_token` directly, and the index is on the raw token. A database read can therefore expose active QR payloads without needing to reverse a hash.

#### Validation
- `ticket_credentials` contains `credential_token` and `credential_hash`.
- `saveTicketCredential` inserts and updates raw `credential_token`.
- `findTicketCredentialByToken` queries raw token and returns it.
- `TicketingService.credentialByPayload` accepts `cred_...` payloads and resolves them through raw token lookup.

#### Dataflow
Ticket issue/credential refresh -> raw `cred_...` token saved to DB -> QR scan submits token as credential payload -> `TicketingService.credentialByPayload` -> `JdbcTicketingRepository.findTicketCredentialByToken` -> entitlement verification.

#### Reachability
An attacker with database or backup read access can replay non-expired active ticket QR credentials. This is less severe than customer login bypass because normal status, expiry, and entitlement usage controls still apply, but it affects directly value-bearing tickets.

#### Severity
Severity is medium because exploitation requires database/backup read access and active credentials, but successful replay can consume ticket entitlements. Severity would rise if QR credentials have long TTLs, are reusable, or are accepted offline without rapid reconciliation.

#### Remediation
- Drop raw token lookup from runtime: store and index only `credential_hash` for active credentials.
- Return raw QR token only at issuance/refresh time; never persist it after hashing.
- Migrate existing credentials by expiring/reissuing or backfilling hash then nulling raw token if compatibility allows.
- Add tests that DB token columns are not equal to returned QR token and that lookup uses hash.

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
|---|---|---|---|
| Customer public API | Anonymous auth, session issuance, token storage | Reported | OTP bypass and plaintext token persistence became findings 1 and 2. |
| Ticketing QR credentials | Bearer credential storage and verification | Reported | Raw QR credential token is stored and queried despite hash column. |
| Open platform webhook | HMAC signature, replay, body integrity | Reported | Signature omits raw body or body hash. |
| Staff production auth | Password hashing, failed login lock, token hash storage | No issue found | BCrypt, lockout, token hash lookup, refresh rotation reviewed. |
| SecurityConfig | Public paths, docs, actuator exposure | No issue found | Docs public only outside prod; actuator limited to health/info. |
| AuthorizationService | Permission checking and platform admin bridge | No issue found | Permission existence check, deny precedence, single-use consumption reviewed; platform_admin bridge is a design superuser. |
| JDBC repositories | SQL injection | Rejected | Named parameters dominate; only dynamic table names are hardcoded internal constants. |
| Evidence attachments | Upload intent and object key binding | Needs follow-up | Current implementation is a stub URL/object key flow; future object storage integration needs signed URL, content length, checksum, and object-key ownership enforcement. |
| Floor plan import | Parser abuse and resource exhaustion | No issue found | 1,000,000 character payload cap, allowed formats, point range validation, and JSON parse failure handling reviewed. |
| Android terminal | Exported component, HTTPS, local storage | Rejected | Exported launcher has no privileged intent handling; API client requires HTTPS outside loopback/explicit test override. |
| Web management UI | DOM XSS, token storage, API normalization | No issue found | No dangerous DOM APIs found; token held in module memory, not localStorage/sessionStorage. |
| Legacy `apps/api` | Old TypeScript/Fastify/SQLite prototype | Needs follow-up | Scripts block execution and root lock excludes dependencies, but no package-lock exists for dependency audit evidence. |
| 160 ops/NAS/credential ledgers | Backup, recovery, credential rotation | No issue found | `verify:ops160-recovery` passed. |
| Formal security baseline docs | Release gate drift | Needs follow-up | `verify:security-baseline` failed because generated baseline counts lag OpenAPI 190 operations. |

## Open Questions And Follow Up

- Fix and rerun formal security baseline generation so operation counts match current OpenAPI 190 operations, then rerun `npm run verify:security-baseline`.
- Decide whether archived `apps/api` should be physically removed or kept with a minimal lockfile solely for dependency audit evidence; production scripts already block it.
- When object storage is wired, run a focused review on evidence upload completion: signed URL scope, object key ownership, checksum enforcement, size limits, and malware scanning.
- After webhook payload signing is fixed, re-review provider onboarding docs and OpenAPI examples so `raw_payload_hash` or raw-body signing is mandatory and testable.
