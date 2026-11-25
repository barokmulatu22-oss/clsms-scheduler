package util;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Utility class for time calculations, overlap detection, and parsing.
 * Contains the single authoritative interval overlap check for the entire application.
 */
public final class TimeUtil {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter TIME_PARSER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, java.time.format.SignStyle.NEVER)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    private TimeUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Checks if two time intervals overlap on the same day.
     * Overlap formula: newStart < existingEnd AND newEnd > existingStart.
     * Back-to-back bookings (e.g., 09:00-10:00 and 10:00-11:00) do NOT overlap.
     *
     * @param newStart      Proposed booking start time
     * @param newEnd        Proposed booking end time
     * @param existingStart Existing booking start time
     * @param existingEnd   Existing booking end time
     * @return true if the intervals intersect, false if separate or back-to-back
     * @throws IllegalArgumentException if any argument is null or if start is not before end
     */
    public static boolean overlaps(LocalTime newStart, LocalTime newEnd,
                                    LocalTime existingStart, LocalTime existingEnd) {
        if (newStart == null || newEnd == null || existingStart == null || existingEnd == null) {
            throw new IllegalArgumentException("Time interval arguments cannot be null.");
        }
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("New end time (" + newEnd + ") must be after start time (" + newStart + ").");
        }
        if (!existingEnd.isAfter(existingStart)) {
            throw new IllegalArgumentException("Existing end time (" + existingEnd + ") must be after start time (" + existingStart + ").");
        }

        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }

    /**
     * Parses a 24-hour time string (such as "09:30" or "9:30") into a LocalTime.
     *
     * @param timeString Time string in 24-hour format
     * @return Parsed LocalTime
     * @throws IllegalArgumentException with a clear error message if input is invalid or null
     */
    public static LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.isBlank()) {
            throw new IllegalArgumentException("Time string cannot be null or blank.");
        }
        try {
            return LocalTime.parse(timeString.trim(), TIME_PARSER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid time format: '%s'. Expected 24-hour format HH:mm (e.g. '09:30', '14:00').", timeString),
                    e
            );
        }
    }

    /**
     * Parses a day of the week string (case-insensitive, e.g. "mon", "Monday") into a DayOfWeek.
     *
     * @param dayString Day of week name or abbreviation
     * @return Corresponding java.time.DayOfWeek
     * @throws IllegalArgumentException with a clear error message if input is invalid or null
     */
    public static DayOfWeek parseDay(String dayString) {
        if (dayString == null || dayString.isBlank()) {
            throw new IllegalArgumentException("Day string cannot be null or blank.");
        }
        String clean = dayString.trim().toUpperCase();
        return switch (clean) {
            case "MON", "MONDAY", "1" -> DayOfWeek.MONDAY;
            case "TUE", "TUES", "TUESDAY", "2" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY", "3" -> DayOfWeek.WEDNESDAY;
            case "THU", "THUR", "THURS", "THURSDAY", "4" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY", "5" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY", "6" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY", "7" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException(
                    String.format("Invalid day of week: '%s'. Expected Monday through Sunday (e.g. 'Monday', 'Mon').", dayString)
            );
        };
    }

    /**
     * Formats a LocalTime object as an "HH:mm" 24-hour string.
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.format(TIME_FORMATTER);
    }
}
