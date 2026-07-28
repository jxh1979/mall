package com.complex.platform.terminal;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTerminalDeviceRepository implements TerminalDeviceRepository {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcTerminalDeviceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public TerminalDeviceRecord save(TerminalDeviceRecord device) {
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (
                    id, device_id, device_code, terminal_type, binding_scope, bound_space_id,
                    mdm_device_ref, kiosk_mode, merchant_id, venue_id, area_id, location_point_id,
                    staff_id, status, bound_at, returned_at, unbound_at, return_reason,
                    unbind_reason, created_at, updated_at
                )
                VALUES (
                    :id, :device_id, :device_code, :terminal_type, :binding_scope, :bound_space_id,
                    :mdm_device_ref, :kiosk_mode, :merchant_id, :venue_id, :area_id, :location_point_id,
                    :staff_id, :status, :bound_at, :returned_at, :unbound_at, :return_reason,
                    :unbind_reason, :created_at, :updated_at
                )
                ON CONFLICT (device_id) DO UPDATE SET
                    device_code = EXCLUDED.device_code,
                    terminal_type = EXCLUDED.terminal_type,
                    binding_scope = EXCLUDED.binding_scope,
                    bound_space_id = EXCLUDED.bound_space_id,
                    mdm_device_ref = EXCLUDED.mdm_device_ref,
                    kiosk_mode = EXCLUDED.kiosk_mode,
                    merchant_id = EXCLUDED.merchant_id,
                    venue_id = EXCLUDED.venue_id,
                    area_id = EXCLUDED.area_id,
                    location_point_id = EXCLUDED.location_point_id,
                    staff_id = EXCLUDED.staff_id,
                    status = EXCLUDED.status,
                    bound_at = EXCLUDED.bound_at,
                    returned_at = EXCLUDED.returned_at,
                    unbound_at = EXCLUDED.unbound_at,
                    return_reason = EXCLUDED.return_reason,
                    unbind_reason = EXCLUDED.unbind_reason,
                    updated_at = EXCLUDED.updated_at
                """, deviceParameters(device));
        return device;
    }

    @Override
    public int compareAndSetStatus(String id, String expectedFrom, TerminalDeviceRecord updated) {
        // P2 并发缺口①修复：条件 UPDATE，仅当当前 status = :expected_from 时整行更新。
        // WHERE id=:id AND status=:expected_from 提供乐观并发保护：
        // 并发流转（如 found 与 scrap 同时）只有一个 UPDATE 命中前态，另一个受影响 0 行被判冲突，
        // 杜绝 ON CONFLICT DO UPDATE 的盲写丢更新；终态（scrapped/blocked）因前态不再匹配天然不可逆。
        MapSqlParameterSource params = deviceParameters(updated)
                .addValue("expected_from", expectedFrom);
        return jdbcTemplate.update("""
                UPDATE terminal_devices SET
                    device_code = :device_code,
                    terminal_type = :terminal_type,
                    binding_scope = :binding_scope,
                    bound_space_id = :bound_space_id,
                    mdm_device_ref = :mdm_device_ref,
                    kiosk_mode = :kiosk_mode,
                    merchant_id = :merchant_id,
                    venue_id = :venue_id,
                    area_id = :area_id,
                    location_point_id = :location_point_id,
                    staff_id = :staff_id,
                    status = :status,
                    bound_at = :bound_at,
                    returned_at = :returned_at,
                    unbound_at = :unbound_at,
                    return_reason = :return_reason,
                    unbind_reason = :unbind_reason,
                    updated_at = :updated_at
                 WHERE id = :id
                   AND status = :expected_from
                """, params);
    }

    @Override
    public Optional<TerminalDeviceRecord> findByDeviceCode(String deviceCode) {
        return jdbcTemplate.query("""
                SELECT id, device_id, device_code, terminal_type, binding_scope, bound_space_id,
                       mdm_device_ref, kiosk_mode, merchant_id, venue_id, area_id, location_point_id,
                       staff_id, status, bound_at, returned_at, unbound_at, return_reason,
                       unbind_reason, created_at, updated_at
                  FROM terminal_devices
                 WHERE device_code = :device_code
                """, Map.of("device_code", deviceCode), this::mapDevice).stream().findFirst();
    }

    @Override
    public Optional<TerminalDeviceRecord> findById(String id) {
        return jdbcTemplate.query("""
                SELECT id, device_id, device_code, terminal_type, binding_scope, bound_space_id,
                       mdm_device_ref, kiosk_mode, merchant_id, venue_id, area_id, location_point_id,
                       staff_id, status, bound_at, returned_at, unbound_at, return_reason,
                       unbind_reason, created_at, updated_at
                  FROM terminal_devices
                 WHERE id = :id
                """, Map.of("id", id), this::mapDevice).stream().findFirst();
    }

    @Override
    public Optional<TerminalDeviceRecord> findByDeviceId(String deviceId) {
        return jdbcTemplate.query("""
                SELECT id, device_id, device_code, terminal_type, binding_scope, bound_space_id,
                       mdm_device_ref, kiosk_mode, merchant_id, venue_id, area_id, location_point_id,
                       staff_id, status, bound_at, returned_at, unbound_at, return_reason,
                       unbind_reason, created_at, updated_at
                  FROM terminal_devices
                 WHERE device_id = :device_id
                """, Map.of("device_id", deviceId), this::mapDevice).stream().findFirst();
    }

    @Override
    public List<TerminalDeviceRecord> listDevicesByVenue(String venueId) {
        // 商户面 scoped 读通道：按 terminal_devices.venue_id = token scope 过滤（AC-ISO-1，§4）。
        return jdbcTemplate.query("""
                SELECT id, device_id, device_code, terminal_type, binding_scope, bound_space_id,
                       mdm_device_ref, kiosk_mode, merchant_id, venue_id, area_id, location_point_id,
                       staff_id, status, bound_at, returned_at, unbound_at, return_reason,
                       unbind_reason, created_at, updated_at
                  FROM terminal_devices
                 WHERE venue_id = :venue_id
                 ORDER BY created_at DESC, id
                """, Map.of("venue_id", venueId), this::mapDevice);
    }

    @Override
    public Optional<DeviceCredentialRecord> findDeviceCredentialByDeviceId(String deviceId) {
        return jdbcTemplate.query("""
                SELECT device_id, token_sha256, status, created_at, updated_at
                  FROM terminal_device_credentials
                 WHERE device_id = :device_id
                """, Map.of("device_id", deviceId), this::mapDeviceCredential).stream().findFirst();
    }

    @Override
    public DeviceCredentialRecord saveDeviceCredential(DeviceCredentialRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_device_credentials (
                    device_id, token_sha256, status, created_at, updated_at
                )
                VALUES (
                    :device_id, :token_sha256, :status, :created_at, :updated_at
                )
                ON CONFLICT (device_id) DO UPDATE SET
                    token_sha256 = EXCLUDED.token_sha256,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, deviceCredentialParameters(record));
        return record;
    }

    @Override
    public TerminalDeviceStatusEventRecord saveStatusEvent(TerminalDeviceStatusEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO terminal_device_status_events (
                    id, device_id, device_code, event_type, status, reason, staff_id, created_at
                )
                VALUES (
                    :id, :device_id, :device_code, :event_type, :status, :reason, :staff_id, :created_at
                )
                """, statusEventParameters(event));
        return event;
    }

    @Override
    public List<TerminalCapabilitySwitchRecord> listCapabilitySwitches() {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM terminal_capability_switches
                 ORDER BY updated_at DESC, id
                 LIMIT 1000
                """, this::mapCapabilitySwitch);
    }

    @Override
    public Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitchById(String id) {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM terminal_capability_switches
                 WHERE id = :id
                """, Map.of("id", id), this::mapCapabilitySwitch).stream().findFirst();
    }

    @Override
    public Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitch(
            String terminalType,
            String capabilityCode,
            String scopeId
    ) {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM terminal_capability_switches
                 WHERE terminal_type = :terminal_type
                   AND capability_code = :capability_code
                   AND scope_id = :scope_id
                 ORDER BY updated_at DESC, id
                 LIMIT 1
                """, Map.of(
                "terminal_type", terminalType,
                "capability_code", capabilityCode,
                "scope_id", scopeId
        ), this::mapCapabilitySwitch).stream().findFirst();
    }

    @Override
    public Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
            String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
        // R-10（2026-07-22 用户裁决）取最严：任一层显式关闭即最终关闭，低层"打开"不再压过高层"关闭"。
        // 旧实现按 device→…→global 顺序命中第一条显式行即返回，导致 global=关 + device=开 时返回 device 行
        // 而放行（安全方向反了，P0）。新实现一次取回全部命中层，按"高层优先"给出关闭行；
        // 全部为开时仍返回最具体的那条开启行（保留既有 map(enabled).orElse(default) 调用方语义）。
        // terminal_type 同时匹配请求端与跨端通配 '*'（同一层内仍是"专端行优先于通配行"，
        // 通配/专端属另一坐标轴，不是「层级」，故不纳入取最严——见 PRD §5.7 口径说明）。
        Map<String, TerminalCapabilitySwitchRecord> hits =
                loadSwitchRowsByScope(terminalType, capabilityCode, orderedScopes);
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        // orderedScopes 由细到粗（device…global）；反向遍历 = 由粗到细，先命中的关闭行即"最高层关闭"，
        // 用它做审计口径（告诉运营"是哪一层把它关了"最有价值的是最高那层）。
        List<String> coarseToFine = new java.util.ArrayList<>(orderedScopes.keySet());
        java.util.Collections.reverse(coarseToFine);
        for (String scopeType : coarseToFine) {
            TerminalCapabilitySwitchRecord hit = hits.get(scopeType);
            if (hit != null && !hit.enabled()) {
                return Optional.of(hit);
            }
        }
        // 无任何关闭层 → 返回最具体的显式开启行。
        for (String scopeType : orderedScopes.keySet()) {
            TerminalCapabilitySwitchRecord hit = hits.get(scopeType);
            if (hit != null) {
                return Optional.of(hit);
            }
        }
        return Optional.empty();
    }

    /**
     * 一次查询取回该 (terminal_type, capability_code) 在本次请求各 scope 维度上的显式开关行。
     * 返回 scope_type → 该层胜出行（同层内 专端行 &gt; 通配 '*' 行，再按 updated_at DESC, id）。
     * 相比旧实现的逐层 N 次查询，本方法是 1 次查询，取最严需看全部层也不会更慢。
     */
    private Map<String, TerminalCapabilitySwitchRecord> loadSwitchRowsByScope(
            String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
        StringBuilder scopePredicate = new StringBuilder();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("terminal_type", terminalType == null ? "" : terminalType)
                .addValue("capability_code", capabilityCode);
        int index = 0;
        for (Map.Entry<String, String> scope : orderedScopes.entrySet()) {
            String scopeId = scope.getValue();
            if (scopeId == null || scopeId.isBlank()) {
                continue;
            }
            if (index > 0) {
                scopePredicate.append(" OR ");
            }
            scopePredicate.append("(scope_type = :scope_type_").append(index)
                    .append(" AND scope_id = :scope_id_").append(index).append(')');
            parameters.addValue("scope_type_" + index, scope.getKey());
            parameters.addValue("scope_id_" + index, scopeId);
            index++;
        }
        if (index == 0) {
            return Map.of();
        }
        List<TerminalCapabilitySwitchRecord> rows = jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM terminal_capability_switches
                 WHERE terminal_type IN (:terminal_type, '*')
                   AND capability_code = :capability_code
                   AND (%s)
                 ORDER BY CASE WHEN terminal_type = '*' THEN 1 ELSE 0 END, updated_at DESC, id
                """.formatted(scopePredicate), parameters, this::mapCapabilitySwitch);
        // ORDER BY 已把同层内的胜出行排在前面，故 putIfAbsent 保留第一条。
        Map<String, TerminalCapabilitySwitchRecord> byScope = new java.util.LinkedHashMap<>();
        for (TerminalCapabilitySwitchRecord row : rows) {
            byScope.putIfAbsent(row.scopeType(), row);
        }
        return byScope;
    }

    @Override
    public TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled,
                    reason, created_at, updated_at
                )
                VALUES (
                    :id, :terminal_type, :capability_code, :scope_type, :scope_id, :enabled,
                    :reason, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    terminal_type = EXCLUDED.terminal_type,
                    capability_code = EXCLUDED.capability_code,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = EXCLUDED.updated_at
                """, capabilitySwitchParameters(record));
        return record;
    }

    @Override
    public Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId) {
        return jdbcTemplate.query("""
                SELECT device_id, kiosk_mode, ticketing_enabled, bound_scope,
                       config::text AS config, updated_at
                  FROM terminal_kiosk_configs
                 WHERE device_id = :device_id
                """, Map.of("device_id", deviceId), this::mapKioskConfig).stream().findFirst();
    }

    @Override
    public TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_kiosk_configs (
                    device_id, kiosk_mode, ticketing_enabled, bound_scope, config, updated_at
                )
                VALUES (
                    :device_id, :kiosk_mode, :ticketing_enabled, :bound_scope, CAST(:config AS jsonb), :updated_at
                )
                ON CONFLICT (device_id) DO UPDATE SET
                    kiosk_mode = EXCLUDED.kiosk_mode,
                    ticketing_enabled = EXCLUDED.ticketing_enabled,
                    bound_scope = EXCLUDED.bound_scope,
                    config = EXCLUDED.config,
                    updated_at = EXCLUDED.updated_at
                """, kioskConfigParameters(record));
        return record;
    }

    @Override
    public KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_kiosk_heartbeats (
                    device_id, status, app_version, capabilities, last_error_code,
                    kiosk_mode, last_heartbeat_at
                )
                VALUES (
                    :device_id, :status, :app_version, CAST(:capabilities AS jsonb), :last_error_code,
                    :kiosk_mode, :last_heartbeat_at
                )
                ON CONFLICT (device_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    app_version = EXCLUDED.app_version,
                    capabilities = EXCLUDED.capabilities,
                    last_error_code = EXCLUDED.last_error_code,
                    kiosk_mode = EXCLUDED.kiosk_mode,
                    last_heartbeat_at = EXCLUDED.last_heartbeat_at
                """, kioskHeartbeatParameters(record));
        return record;
    }

    private MapSqlParameterSource deviceParameters(TerminalDeviceRecord device) {
        return new MapSqlParameterSource()
                .addValue("id", device.id())
                .addValue("device_id", device.deviceId())
                .addValue("device_code", device.deviceCode())
                .addValue("terminal_type", device.terminalType())
                .addValue("binding_scope", device.bindingScope())
                .addValue("bound_space_id", device.boundSpaceId())
                .addValue("mdm_device_ref", device.mdmDeviceRef())
                .addValue("kiosk_mode", device.kioskMode())
                .addValue("merchant_id", device.merchantId())
                .addValue("venue_id", device.venueId())
                .addValue("area_id", device.areaId())
                .addValue("location_point_id", device.locationPointId())
                .addValue("staff_id", device.staffId())
                .addValue("status", device.status())
                .addValue("bound_at", timestampWithTimeZone(device.boundAt()))
                .addValue("returned_at", timestampWithTimeZone(device.returnedAt()))
                .addValue("unbound_at", timestampWithTimeZone(device.unboundAt()))
                .addValue("return_reason", device.returnReason())
                .addValue("unbind_reason", device.unbindReason())
                .addValue("created_at", timestampWithTimeZone(device.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(device.updatedAt()));
    }

    private MapSqlParameterSource statusEventParameters(TerminalDeviceStatusEventRecord event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("device_id", event.deviceId())
                .addValue("device_code", event.deviceCode())
                .addValue("event_type", event.eventType())
                .addValue("status", event.status())
                .addValue("reason", event.reason())
                .addValue("staff_id", event.staffId())
                .addValue("created_at", timestampWithTimeZone(event.createdAt()));
    }

    private MapSqlParameterSource deviceCredentialParameters(DeviceCredentialRecord record) {
        return new MapSqlParameterSource()
                .addValue("device_id", record.deviceId())
                .addValue("token_sha256", record.tokenSha256())
                .addValue("status", record.status())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource capabilitySwitchParameters(TerminalCapabilitySwitchRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("terminal_type", record.terminalType())
                .addValue("capability_code", record.capabilityCode())
                .addValue("scope_type", record.scopeType())
                .addValue("scope_id", record.scopeId())
                .addValue("enabled", record.enabled())
                .addValue("reason", record.reason())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource kioskConfigParameters(TerminalKioskConfigRecord record) {
        return new MapSqlParameterSource()
                .addValue("device_id", record.deviceId())
                .addValue("kiosk_mode", record.kioskMode())
                .addValue("ticketing_enabled", record.ticketingEnabled())
                .addValue("bound_scope", record.boundScope())
                .addValue("config", writeJson(record.config() == null ? Map.of() : record.config()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource kioskHeartbeatParameters(KioskHeartbeatRecord record) {
        return new MapSqlParameterSource()
                .addValue("device_id", record.deviceId())
                .addValue("status", record.status())
                .addValue("app_version", record.appVersion())
                .addValue("capabilities", writeJson(record.capabilities() == null ? List.of() : record.capabilities()))
                .addValue("last_error_code", record.lastErrorCode())
                .addValue("kiosk_mode", record.kioskMode())
                .addValue("last_heartbeat_at", timestampWithTimeZone(record.lastHeartbeatAt()));
    }

    private TerminalDeviceRecord mapDevice(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalDeviceRecord(
                resultSet.getString("id"),
                resultSet.getString("device_id"),
                resultSet.getString("device_code"),
                resultSet.getString("terminal_type"),
                resultSet.getString("binding_scope"),
                resultSet.getString("bound_space_id"),
                resultSet.getString("mdm_device_ref"),
                resultSet.getString("kiosk_mode"),
                resultSet.getString("merchant_id"),
                resultSet.getString("venue_id"),
                resultSet.getString("area_id"),
                resultSet.getString("location_point_id"),
                resultSet.getString("staff_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "bound_at"),
                nullableInstant(resultSet, "returned_at"),
                nullableInstant(resultSet, "unbound_at"),
                resultSet.getString("return_reason"),
                resultSet.getString("unbind_reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private DeviceCredentialRecord mapDeviceCredential(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DeviceCredentialRecord(
                resultSet.getString("device_id"),
                resultSet.getString("token_sha256"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TerminalCapabilitySwitchRecord mapCapabilitySwitch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalCapabilitySwitchRecord(
                resultSet.getString("id"),
                resultSet.getString("terminal_type"),
                resultSet.getString("capability_code"),
                resultSet.getString("scope_type"),
                resultSet.getString("scope_id"),
                resultSet.getBoolean("enabled"),
                resultSet.getString("reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TerminalKioskConfigRecord mapKioskConfig(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalKioskConfigRecord(
                resultSet.getString("device_id"),
                resultSet.getString("kiosk_mode"),
                resultSet.getBoolean("ticketing_enabled"),
                resultSet.getString("bound_scope"),
                readObjectMap(resultSet.getString("config")),
                nullableInstant(resultSet, "updated_at")
        );
    }



    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("terminal JSON value is not serializable", exception);
        }
    }

    private Map<String, Object> readObjectMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("terminal config JSON value is not readable", exception);
        }
    }
}
