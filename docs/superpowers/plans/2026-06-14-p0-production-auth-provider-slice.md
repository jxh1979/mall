# P0 production auth provider slice

## Goal

Close the code-owned part of `SEC-P0-AUTH-PROD-001` by adding a formal production authentication provider that is separate from the local/dev/test smoke auth flow.

## Scope

- Keep the existing `AuthService`, `AuthController`, and `BearerAuthenticationFilter` for `local/dev/test`.
- Add a production-profile JDBC auth provider for staff login, session refresh, logout, token revocation, failed-login lockout, and audit events.
- Store password hashes with BCrypt and store only SHA-256 hashes of high-entropy access/refresh tokens.
- Keep third-party SSO, enterprise identity, device MDM enrollment, payment merchant config, face/age supplier config, WeCom config, NAS drill, and credential rotation as production readiness tasks because they require external operational inputs.

## Files to modify

- `backend/src/main/java/com/complex/platform/config/SecurityConfig.java`
  - Register the production bearer filter when the `prod` profile is active.
- `tools/generate-formal-security-baseline.mjs`
  - Mark the production auth finding as resolved when the production provider, migration, and tests exist.
- `tools/verify-formal-security-baseline.mjs`
  - Accept resolved production auth after the provider lands, while keeping MDM/NAS/credential blockers open.

## Files to add

- `backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java`
- `backend/src/main/java/com/complex/platform/auth/ProductionAuthController.java`
- `backend/src/main/java/com/complex/platform/auth/ProductionBearerAuthenticationFilter.java`
- `backend/src/main/resources/db/migration/V018__p0_production_auth_provider.sql`
- `backend/src/test/java/com/complex/platform/auth/ProductionAuthJdbcIntegrationTests.java`

## Data changes

- Add `auth_credentials`:
  - `user_id`, `password_hash`, `password_algo`, `failed_attempt_count`, `locked_until`, `last_login_at`, `password_changed_at`, `status`, timestamps.
- Add `auth_sessions`:
  - `id`, `user_id`, hashed access/refresh token columns, device/terminal, expiry, revocation, status, timestamps.
- Reuse `access_users`, `roles`, and `user_roles` for staff identity and permission bridging.

## Interface changes

- No OpenAPI path change.
- Production profile implements the existing `/api/v1/auth/login`, `/api/v1/auth/refresh`, and `/api/v1/auth/logout` response shape.
- Seed admin is optional and property-driven:
  - `complex.auth.production.seed-admin.username`
  - `complex.auth.production.seed-admin.password`
  - No default production password is introduced.

## TDD steps

1. Add failing `ProductionAuthJdbcIntegrationTests` for prod-profile login, hashed token storage, authenticated request, refresh rotation, refresh reuse rejection, logout revocation, and failed-login lockout.
2. Add Flyway migration.
3. Add production service/controller/filter.
4. Wire production filter into `SecurityConfig`.
5. Update security baseline generator/verifier.
6. Run targeted backend tests.
7. Run security baseline verification and P0 verification on 160.

## Acceptance criteria

- With `prod` profile and seed admin properties, login returns access token, refresh token, session id, Bearer token type, and user object.
- Raw access/refresh tokens are never stored in `auth_sessions`.
- Refresh rotates both access and refresh tokens and rejects a reused refresh token.
- Logout revokes the session and subsequent use of the old access token is denied.
- Repeated invalid login attempts lock the account for the configured window.
- Dev/test smoke auth remains unchanged and existing auth tests still pass.
- `SEC-P0-AUTH-PROD-001` no longer remains open in the generated findings register.

## Risks and rollback

- Risk: profile collision could register both smoke and production auth endpoints. Mitigation: production classes are `prod` only; smoke classes remain `local/dev/test`.
- Risk: migration could conflict with existing auth naming. Mitigation: use `auth_credentials` and `auth_sessions`, reuse `access_users`.
- Risk: production seed admin could become a permanent secret path. Mitigation: seed only when explicit properties are provided, never introduce a default secret.
- Rollback: remove the five added auth files, remove `V018__p0_production_auth_provider.sql` before it is applied to a shared DB, revert `SecurityConfig` and security baseline scripts, then rerun auth tests.
