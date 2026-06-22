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
