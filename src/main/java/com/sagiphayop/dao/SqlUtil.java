package com.sagiphayop.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Small helpers shared by the DAOs. */
final class SqlUtil {
    private SqlUtil() { }

    static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    static String blankToNull(String s) {
        return has(s) ? s.trim() : null;
    }

    /** Null-safe string read: SQL NULL becomes "". */
    static String text(ResultSet rs, String column) throws SQLException {
        String v = rs.getString(column);
        return v == null ? "" : v;
    }

    /** Nullable integer read. */
    static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /** Reads the generated key of an INSERT that was prepared with RETURN_GENERATED_KEYS, or -1. */
    static int generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            return keys.next() ? keys.getInt(1) : -1;
        }
    }
}
