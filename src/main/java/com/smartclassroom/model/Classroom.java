package com.smartclassroom.model;

/** A room (lecture hall or lab) with a seating capacity. */
public class Classroom {
    public enum RoomType { LECTURE, LAB }

    private int classroomId;
    private String roomName;
    private int capacity;
    private RoomType roomType = RoomType.LECTURE;

    public int getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(int classroomId) {
        this.classroomId = classroomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getCapacity() {
        return capacity;
    }

    /** Capacity must be positive; the model refuses to hold an impossible value. */
    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    @Override
    public String toString() {
        return roomName + " (cap " + capacity + ")";
    }
}
