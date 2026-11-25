package service;

import model.Room;
import model.RoomType;
import model.Schedule;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that builds timetable queries and summary reports from stored data.
 * Pure service logic: returns structured collections and data objects rather than printing to console.
 */
public class ReportManager {

    private final ScheduleManager scheduleManager;
    private final RoomManager roomManager;
    private final CourseManager courseManager;
    private final InstructorManager instructorManager;

    public ReportManager(ScheduleManager scheduleManager, RoomManager roomManager,
                         CourseManager courseManager, InstructorManager instructorManager) {
        this.scheduleManager = scheduleManager;
        this.roomManager = roomManager;
        this.courseManager = courseManager;
        this.instructorManager = instructorManager;
    }

    /**
     * Returns all scheduled bookings for a given day in chronological start-time order.
     *
     * @param day Day of week
     * @return Chronologically sorted list of schedules
     */
    public List<Schedule> getDailyTimetable(DayOfWeek day) {
        if (day == null || scheduleManager == null) {
            return Collections.emptyList();
        }

        List<Schedule> daily = new ArrayList<>();
        for (Schedule s : scheduleManager.getAllSchedules()) {
            if (s.getDay() == day) {
                daily.add(s);
            }
        }
        daily.sort(Comparator.comparing(Schedule::getStartTime));
        return Collections.unmodifiableList(daily);
    }

    /**
     * Returns all bookings for a specific room grouped by DayOfWeek.
     * Each day's bookings are sorted chronologically by start time.
     *
     * @param roomId Target room ID (case-insensitive)
     * @return Map of DayOfWeek to sorted list of schedules
     */
    public Map<DayOfWeek, List<Schedule>> getRoomTimetable(String roomId) {
        Map<DayOfWeek, List<Schedule>> timetable = new EnumMap<>(DayOfWeek.class);
        if (roomId == null || roomId.isBlank() || scheduleManager == null) {
            return Collections.emptyMap();
        }

        String targetId = roomId.trim().toUpperCase();
        for (Schedule s : scheduleManager.getAllSchedules()) {
            if (s.getRoom().getRoomId().equalsIgnoreCase(targetId)) {
                timetable.computeIfAbsent(s.getDay(), k -> new ArrayList<>()).add(s);
            }
        }

        // Sort schedules within each day
        for (List<Schedule> dayList : timetable.values()) {
            dayList.sort(Comparator.comparing(Schedule::getStartTime));
        }

        return Collections.unmodifiableMap(timetable);
    }

    /**
     * Returns all bookings for a specific instructor grouped by DayOfWeek.
     * Each day's bookings are sorted chronologically by start time.
     *
     * @param instructorId Target instructor ID (case-insensitive)
     * @return Map of DayOfWeek to sorted list of schedules
     */
    public Map<DayOfWeek, List<Schedule>> getInstructorTimetable(String instructorId) {
        Map<DayOfWeek, List<Schedule>> timetable = new EnumMap<>(DayOfWeek.class);
        if (instructorId == null || instructorId.isBlank() || scheduleManager == null) {
            return Collections.emptyMap();
        }

        String targetId = instructorId.trim().toUpperCase();
        for (Schedule s : scheduleManager.getAllSchedules()) {
            if (s.getInstructor().getInstructorId().equalsIgnoreCase(targetId)) {
                timetable.computeIfAbsent(s.getDay(), k -> new ArrayList<>()).add(s);
            }
        }

        // Sort schedules within each day
        for (List<Schedule> dayList : timetable.values()) {
            dayList.sort(Comparator.comparing(Schedule::getStartTime));
        }

        return Collections.unmodifiableMap(timetable);
    }

    /**
     * Returns an aggregate summary of system entities and active bookings.
     */
    public Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        List<Room> rooms = (roomManager != null) ? roomManager.getAllRooms() : Collections.emptyList();
        long classrooms = rooms.stream().filter(r -> r.getType() == RoomType.CLASSROOM).count();
        long laboratories = rooms.stream().filter(r -> r.getType() == RoomType.LABORATORY).count();
        long activeRooms = rooms.stream().filter(Room::isActive).count();

        int totalCourses = (courseManager != null) ? courseManager.getAllCourses().size() : 0;
        int totalInstructors = (instructorManager != null) ? instructorManager.getAllInstructors().size() : 0;
        int totalSchedules = (scheduleManager != null) ? scheduleManager.getAllSchedules().size() : 0;

        summary.put("totalRooms", rooms.size());
        summary.put("activeRooms", activeRooms);
        summary.put("totalClassrooms", classrooms);
        summary.put("totalLaboratories", laboratories);
        summary.put("totalCourses", totalCourses);
        summary.put("totalInstructors", totalInstructors);
        summary.put("totalSchedules", totalSchedules);

        return Collections.unmodifiableMap(summary);
    }
}
