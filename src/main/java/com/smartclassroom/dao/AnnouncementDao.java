package com.smartclassroom.dao;

import com.smartclassroom.model.Announcement;
import com.smartclassroom.model.Role;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for announcements. */
public class AnnouncementDao implements Dao<Announcement> {
    private static final String SELECT_BASE =
            "SELECT a.announcement_id, a.title, a.message, a.audience, a.posted_by, a.created_at, u.full_name AS posted_by_name "
                    + "FROM announcements a JOIN users u ON u.user_id = a.posted_by ";
    private static final String ORDER = " ORDER BY a.created_at DESC, a.announcement_id DESC";

    @Override
    public List<Announcement> findAll() {
        return query(SELECT_BASE + ORDER);
    }

    @Override
    public Optional<Announcement> findById(int id) {
        List<Announcement> list = query(SELECT_BASE + "WHERE a.announcement_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Announcements a given role is allowed to see. Admin sees everything. */
    public List<Announcement> findVisibleTo(Role role) {
        return switch (role) {
            case ADMIN -> findAll();
            case FACULTY -> query(SELECT_BASE + "WHERE a.audience IN ('ALL','FACULTY')" + ORDER);
            case STUDENT -> query(SELECT_BASE + "WHERE a.audience IN ('ALL','STUDENTS')" + ORDER);
        };
    }

    public int insert(Announcement a) {
        String sql = "INSERT INTO announcements (title, message, audience, posted_by) VALUES (?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getMessage());
            ps.setString(3, a.getAudience().name());
            ps.setInt(4, a.getPostedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM announcements WHERE announcement_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Announcement> query(String sql, Object... params) {
        List<Announcement> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Announcement a = new Announcement();
                    a.setAnnouncementId(rs.getInt("announcement_id"));
                    a.setTitle(rs.getString("title"));
                    a.setMessage(rs.getString("message"));
                    a.setAudience(Announcement.Audience.valueOf(rs.getString("audience")));
                    a.setPostedBy(rs.getInt("posted_by"));
                    a.setPostedByName(rs.getString("posted_by_name"));
                    a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    result.add(a);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
