package com.smartclassroom.dao;

import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Key/value application settings (for example the attendance threshold). */
public class SettingsDao {
    public String get(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public void set(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
