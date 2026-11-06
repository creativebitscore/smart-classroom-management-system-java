package com.smartclassroom.dao;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the classrooms table. */
public class ClassroomDao implements Dao<Classroom> {

    @Override
    public List<Classroom> findAll() {
        List<Classroom> result = new ArrayList<>();
        String sql = "SELECT classroom_id, room_name, capacity, room_type FROM classrooms ORDER BY room_name";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    @Override
    public Optional<Classroom> findById(int id) {
        String sql = "SELECT classroom_id, room_name, capacity, room_type FROM classrooms WHERE classroom_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public int insert(Classroom room) {
        String sql = "INSERT INTO classrooms (room_name, capacity, room_type) VALUES (?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomName());
            ps.setInt(2, room.getCapacity());
            ps.setString(3, room.getRoomType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public void update(Classroom room) {
        String sql = "UPDATE classrooms SET room_name = ?, capacity = ?, room_type = ? WHERE classroom_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, room.getRoomName());
            ps.setInt(2, room.getCapacity());
            ps.setString(3, room.getRoomType().name());
            ps.setInt(4, room.getClassroomId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM classrooms WHERE classroom_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private static Classroom map(ResultSet rs) throws SQLException {
        Classroom room = new Classroom();
        room.setClassroomId(rs.getInt("classroom_id"));
        room.setRoomName(rs.getString("room_name"));
        room.setCapacity(rs.getInt("capacity"));
        room.setRoomType(Classroom.RoomType.valueOf(rs.getString("room_type")));
        return room;
    }
}
