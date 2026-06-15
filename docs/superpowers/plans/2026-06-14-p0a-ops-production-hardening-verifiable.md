# P0-A Ops Production Hardening Verifiable Plan

## Goal

Close the remaining code-owned part of P0-A by making 160 production operations hardening verifiable without writing system-level configuration from Codex.

Confirmed decisions:

- Do not let Codex write `/etc/fstab`, `/etc/systemd/system`, root-owned secret files, or other system-level production configuration on `192.168.88.160`.
- Do not expand P0 business scope. Ticketing, bar, inventory, amusement, finance, governance, terminal, and integration P0 business behavior stays frozen unless a regression is found.
- Focus on P0-A production operations hardening and production prerequisite verification.

## Scope

P0-A no-system-write boundary: Codex may produce scripts, templates, validators, Runbooks, and evidence placeholders, but Codex must not install or edit system-level production configuration on 160.

In scope:

- User-level verification scripts, dry-run checks, templates, Runbooks, and evidence files.
- NAS mount readiness checks for `/mnt/complex-nas-backup`.
- Scheduled backup plan verification without installing the schedule.
- Backup failure and capacity alert verification without configuring a real alert channel secret.
- Production credential ledger completeness checks using `credential_ref` only.
- Verification commands that run on `192.168.88.160`.

Out of scope:

- Writing systemd units or timers into system paths.
- Editing `/etc/fstab`.
- Writing real passwords, tokens, API secrets, JWT secrets, object-storage keys, or SMB passwords into repository files.
- Real MDM enroll, real payment merchant onboarding, real WeCom app credentials, real face-age supplier SDK/API integration.
- P1 bar features such as table transfer, table merge, renewal, split bill, merge bill, service fee, formula costing, and advanced commission.

## User Story Map

MVP / P0-A:

- As IT, I can run a user-level preflight check on 160 and see whether NAS mount, write permission, backup target, retention, capacity threshold, and required environment references are ready.
- As operations, I can run a scheduled-backup dry-run verifier and get explicit PASS/FAIL output before IT installs any timer or cron.
- As security, I can verify that production credentials are tracked by `credential_ref`, expiry reminder fields, rotation steps, rollback steps, and evidence links, without exposing secrets.
- As the project owner, I can run one P0-A verification command and see exactly which production prerequisite remains blocking and why.

Second phase / P1:

- IT installs real systemd timers or cron entries from the generated templates.
- Alert channel secrets are configured outside the repository and verified through a real notification test.
- Monthly restore drill evidence is appended to the evidence folder and reviewed in release readiness.

Future:

- Automated credential rotation integration.
- Offsite cloud backup or dual NAS replication.
- Centralized observability and alert routing.

## Files To Modify

- `package.json`
  - Add a P0-A verification command if needed, such as `verify:p0a-ops-hardening`.
  - Keep existing `verify:ops160-recovery` intact.

- `tools/verify-160-backup-restore-scripts.mjs`
  - Extend checks for mount preflight, dry-run backup scheduling template, capacity threshold markers, and alert markers.

- `tools/verify-production-credential-rotation-ledger.mjs`
  - Extend checks for real-evidence placeholders, reminder fields, and no plaintext secret patterns.

- `tools/verify-ops160-execution-policy.mjs`
  - Enforce the confirmed rule that Codex-delivered artifacts must not instruct direct system-level writes as an automated step.

- `docs/server-160-runbook-v0.2.md`
  - Add a section that separates "Codex-verifiable user-level artifacts" from "IT-applied system-level steps".

- `docs/nas-backup-and-restore-runbook-v0.1.md`
  - Add NAS auto-mount handoff template, scheduled backup dry-run acceptance, capacity alert acceptance, failure alert acceptance, and periodic restore drill cadence.

- `docs/production-readiness-implementation-ledger-template-v0.1.md`
  - Add fields or rows for automatic mount evidence, scheduled backup evidence, capacity alert evidence, failed-backup alert evidence, and periodic restore drill evidence.

- `docs/production-credential-rotation-runbook-v0.1.md`
  - Tighten acceptance criteria for credential issuance, expiry reminders, rotation evidence, emergency disable evidence, and no-secret storage.

- `security-scans/findings-register-v1.md`
  - Update only if verification scope changes or evidence wording needs to reflect the no-system-write decision.

## Files To Add

- `ops/160/complex-nas-preflight.sh`
  - User-level script that checks mount path existence, CIFS mount source, read/write/delete permission, free capacity, and retention configuration.

- `ops/160/complex-backup-schedule.example`
  - Non-installing schedule template for IT to convert into systemd timer or cron.

- `ops/160/complex-backup-alert.example`
  - Non-secret alert configuration template describing WeCom webhook/email placeholders and expected failure behavior.

- `tools/verify-p0a-ops-hardening.mjs`
  - Aggregates P0-A checks that are specific to this hardening package.

- `docs/production-readiness-evidence/p0a-ops-hardening-latest.json`
  - Machine-readable evidence summary generated manually or by script after 160 verification.

## Data And Interface Changes

- No business database schema changes.
- No OpenAPI business contract changes.
- If production readiness evidence shape needs to be machine-readable, use a JSON evidence file under `docs/production-readiness-evidence/`.
- Existing production-readiness API remains unchanged unless tests reveal it cannot represent the new evidence fields.

## TDD Plan

1. Add failing verifier tests/checks first:
   - `tools/verify-p0a-ops-hardening.mjs` should fail while required scripts/templates/evidence markers are missing.
   - `verify-ops160-execution-policy` should fail if Runbooks tell Codex to directly install systemd/fstab.
   - `verify-160-backup-restore-scripts` should fail if NAS preflight or schedule/alert markers are missing.

2. Run the failing checks on 160:
   - `npm run verify:p0a-ops-hardening`
   - Expected first result: failure on missing artifacts.

3. Implement the minimum artifacts:
   - Add scripts/templates/Runbook sections.
   - Do not touch system paths.
   - Do not add secrets.

4. Re-run focused checks:
   - `npm run verify:p0a-ops-hardening`
   - `npm run verify:ops160-recovery`
   - `npm run verify:security-baseline`

5. Re-run contract guard:
   - `npm run verify:p0-contract`

6. If code or security-sensitive logic changes:
   - Run backend tests on 160.
   - Run security review and differential review.

## Acceptance Criteria

- Given Codex does not have permission to write system-level config, when P0-A hardening artifacts are generated, then no script attempts to install systemd timers, edit `/etc/fstab`, or write root-owned secret files automatically.
- Given NAS is mounted at `/mnt/complex-nas-backup`, when `complex-nas-preflight.sh` runs, then it verifies mount source, write/read/delete, capacity, and configured backup target without leaking credentials.
- Given a backup schedule is not installed by Codex, when IT reads the schedule template, then it has enough command, environment, cadence, and rollback information to install it manually.
- Given alert secrets are not committed, when alert templates are checked, then they contain only secret references or placeholders and define failure/capacity alert behavior.
- Given production credentials are still managed by manual ledger, when verification runs, then every credential class has `credential_ref`, owner, reminder days, rotation evidence placeholder, verification step, rollback step, and emergency disable step.
- Given a business P0 module is not touched, when P0-A verification is complete, then `verify:p0-contract` remains green.

## Risks

- System-level installation cannot be fully proven without IT applying the templates. Mitigation: split Codex verification from IT-applied evidence and keep blocking scope explicit.
- Alert channels require real secrets. Mitigation: verify templates and secret references only; real notification test remains IT evidence.
- NAS capacity can change. Mitigation: preflight reads live filesystem capacity when executed on 160.
- Over-expanding into P1. Mitigation: verification fails only on P0-A operations artifacts, not on P1 business features.

## Rollback

- Revert added P0-A scripts/templates and verifier script.
- Keep existing `verify:ops160-recovery`, `verify:security-baseline`, and `verify:p0-contract` untouched where possible.
- If package scripts are changed, restore previous `package.json` script values.
- No database rollback is expected because this plan does not change schema.

## Verification Commands

Run on `192.168.88.160` from `/home/james/complex-management`:

```bash
npm run verify:p0a-ops-hardening
npm run verify:ops160-recovery
npm run verify:security-baseline
npm run verify:p0-contract
```

If backend code is touched:

```bash
npm run test:backend
```

Full final gate when time allows:

```bash
npm run verify:p0
```
