package com.smartclassroom.dao;

import com.smartclassroom.model.Admin;
import com.smartclassroom.model.Role;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the users table (login data shared by admins, faculty and students). */
public class UserDao {

    /** What is needed to check a login attempt. */
    public record Credentials(int userId, String passwordHash, Role role, boolean active) {
    }

    /** A user whose password has not been set yet (demo data). */
    public record PendingAccount(int userId, String username, Role role) {
    }

    public Optional<Credentials> findCredentials(String username) {
        String sql = "SELECT user_id, password_hash, role, is_active FROM users WHERE username = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Credentials(rs.getInt("user_id"), rs.getString("password_hash"),
                            Role.valueOf(rs.getString("role")), rs.getBoolean("is_active")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public Optional<Admin> findAdmin(int userId) {
        String sql = "SELECT user_id, username, full_name, email FROM users WHERE user_id = ? AND role = 'ADMIN'";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin();
                    admin.setUserId(rs.getInt("user_id"));
                    admin.setUsername(rs.getString("username"));
                    admin.setFullName(rs.getString("full_name"));
                    admin.setEmail(rs.getString("email"));
                    return Optional.of(admin);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Inserts a user inside an existing transaction and returns the new user_id. */
    public int insert(Connection c, String username, String passwordHash, Role role, String fullName, String email)
            throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Updates name and email inside an existing transaction. */
    public void updateProfile(Connection c, int userId, String fullName, String email) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ? WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void updatePasswordHash(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Accounts created by the SQL script with the placeholder password 'PENDING'. */
    public List<PendingAccount> findAccountsNeedingPassword() {
        String sql = "SELECT user_id, username, role FROM users WHERE password_hash = 'PENDING'";
        List<PendingAccount> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new PendingAccount(rs.getInt("user_id"), rs.getString("username"),
                        Role.valueOf(rs.getString("role"))));
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
