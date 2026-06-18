package com.complex.platform.common.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Shared JDBC timestamp conversions. Centralizes the {@code Instant} <-> SQL
 * {@code timestamptz} mapping that was previously copy-pasted verbatim across
 * every JDBC repository and parameter source.
 */
public final class JdbcTimestamps {

    private JdbcTimestamps() {
    }

    /** Converts an {@code Instant} to a UTC {@link OffsetDateTime} for binding to a timestamptz column. */
    public static OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    /** Reads a nullable timestamptz column as an {@code Instant}. */
    public static Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
