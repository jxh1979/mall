UPDATE open_app_credentials
   SET secret_ref = 'config:complex.open-platform.secrets.p0-open-app',
       updated_at = now()
 WHERE app_key = 'p0-open-app'
   AND secret_ref = 'manual-ledger:p0-open-app';

UPDATE open_endpoint_configs
   SET secret_ref = 'config:complex.open-platform.secrets.p0-webhook',
       updated_at = now()
 WHERE endpoint_key = 'p0-default'
   AND secret_ref = 'manual-ledger:p0-webhook';
