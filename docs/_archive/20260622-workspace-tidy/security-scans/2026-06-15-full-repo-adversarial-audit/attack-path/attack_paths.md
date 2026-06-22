# Attack Path Analysis

## AP-01 Customer account takeover by phone number
1. Attacker sends `POST /api/v1/customer/auth/phone-login` with a target phone and any nonblank `sms_code`.
2. SecurityConfig permits `/api/v1/customer/**` anonymously.
3. `CustomerService.phoneLogin` creates or reactivates that customer and issues a `cust_...` token without verifying OTP ownership.
4. Attacker uses the token to call customer order, ticket QR, points, redemption, and bar reservation APIs that trust `X-Customer-Token`.
5. Impact: customer data exposure and value-bearing action abuse, depending on available ticket/points/reservation state.

## AP-02 Database read to customer session replay
1. Attacker or insider obtains read access to PostgreSQL, backup, debug export, or SQL console output.
2. `customer_sessions.customer_token` contains raw bearer tokens.
3. Attacker replays a non-expired token in `X-Customer-Token` or `Authorization: Bearer ...`.
4. Impact: customer account access without needing the phone/SMS flow.

## AP-03 Signed webhook body integrity gap
1. Provider or intermediary produces/forwards a valid `X-Webhook-Signature` for endpoint/provider/event/idempotency.
2. Payload body is modified before the first accepted request with the same idempotency key.
3. Service verifies the signature against canonical fields only and persists the modified `payload_json` with `signature_valid=true`.
4. Impact: third-party reconciliation/order/voucher event content can be trusted without body integrity proof.

## AP-04 Database read to ticket QR replay
1. Attacker obtains read access to `ticket_credentials` from database or backup.
2. Raw `credential_token` values can be queried directly because the table and repository use plaintext lookup.
3. Attacker presents an active `cred_...` payload to verification flow.
4. Impact: active ticket/entitlement misuse until expiry/status controls stop it.
