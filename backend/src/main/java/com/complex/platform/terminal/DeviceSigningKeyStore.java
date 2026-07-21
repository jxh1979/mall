package com.complex.platform.terminal;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUD-010 ② kiosk/tablet 设备级签名公钥（{@code terminal_device_signing_keys}）——给设备本身一把密钥，
 * 不借员工 Keystore 密钥。staff_work_phone 走 {@code staff_device_credentials}，不用本表。
 * 登记路径见 {@code TerminalDeviceController} 的 {@code /devices/{id}/signing-key}（ready-to-enforce，
 * 客户端实现签名前不会写入；缺行=未登记，enforce 下该设备走软态）。
 */
@Component
public class DeviceSigningKeyStore {

    private final JdbcTemplate jdbc;

    public DeviceSigningKeyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 设备 active 签名公钥（X.509 base64）；无=空。 */
    public Optional<String> findActivePublicKey(String deviceId) {
        return jdbc.query(
                "SELECT public_key FROM terminal_device_signing_keys WHERE device_id = ? AND status = 'active'",
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty(),
                deviceId);
    }

    /** 登记/轮换设备签名公钥（active）。 */
    public void upsert(String deviceId, String publicKey) {
        jdbc.update(
                "INSERT INTO terminal_device_signing_keys (device_id, public_key, status, updated_at) "
                        + "VALUES (?, ?, 'active', now()) "
                        + "ON CONFLICT (device_id) DO UPDATE SET public_key = EXCLUDED.public_key, "
                        + "status = 'active', updated_at = now()",
                deviceId, publicKey);
    }

    /** 吊销设备签名密钥。 */
    public void revoke(String deviceId) {
        jdbc.update("UPDATE terminal_device_signing_keys SET status = 'revoked', updated_at = now() "
                + "WHERE device_id = ?", deviceId);
    }
}
