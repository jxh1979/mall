-- Terminal sync task payloads are audit metadata; scrub legacy dynamic QR values if they were stored before this hardening.
UPDATE terminal_sync_tasks
   SET payload = jsonb_set(payload, '{credentialPayload}', to_jsonb('sha256:legacy-redacted'::text), false)
 WHERE payload->>'credentialPayload' LIKE 'cred_%';

UPDATE terminal_sync_tasks
   SET payload = jsonb_set(payload, '{credential_payload}', to_jsonb('sha256:legacy-redacted'::text), false)
 WHERE payload->>'credential_payload' LIKE 'cred_%';

UPDATE terminal_sync_tasks
   SET payload = jsonb_set(payload, '{entitlementCode}', to_jsonb('sha256:legacy-redacted'::text), false)
 WHERE payload->>'entitlementCode' LIKE 'cred_%';

UPDATE terminal_sync_tasks
   SET payload = jsonb_set(payload, '{entitlement_code}', to_jsonb('sha256:legacy-redacted'::text), false)
 WHERE payload->>'entitlement_code' LIKE 'cred_%';
