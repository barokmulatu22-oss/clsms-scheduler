package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeUtil Overlap and Parsing Tests")
class TimeUtilTest {

    // Base interval: 09:00 - 10:00
    private final LocalTime start = LocalTime.of(9, 0);
    private final LocalTime end = LocalTime.of(10, 0);

    @Test
    @DisplayName("Exact same interval overlaps (09:00-10:00 vs 09:00-10:00)")
    void testExactSameInterval() {
        assertTrue(TimeUtil.overlaps(start, end, start, end));
    }

    @Test
    @DisplayName("Partial overlap at the start (08:30-09:30 vs 09:00-10:00)")
    void testPartialOverlapAtStart() {
        LocalTime newStart = LocalTime.of(8, 30);
        LocalTime newEnd = LocalTime.of(9, 30);
        assertTrue(TimeUtil.overlaps(newStart, newEnd, start, end));
    }

    @Test
    @DisplayName("Partial overlap at the end (09:30-10:30 vs 09:00-10:00)")
    void testPartialOverlapAtEnd() {
        LocalTime newStart = LocalTime.of(9, 30);
        LocalTime newEnd = LocalTime.of(10, 30);
        assertTrue(TimeUtil.overlaps(newStart, newEnd, start, end));
    }

    @Test
    @DisplayName("New interval inside existing (09:15-09:45 vs 09:00-10:00)")
    void testNewIntervalInsideExisting() {
        LocalTime newStart = LocalTime.of(9, 15);
        LocalTime newEnd = LocalTime.of(9, 45);
        assertTrue(TimeUtil.overlaps(newStart, newEnd, start, end));
    }

    @Test
    @DisplayName("Existing interval inside new interval (08:00-11:00 vs 09:00-10:00)")
    void testExistingInsideNew() {
        LocalTime newStart = LocalTime.of(8, 0);
        LocalTime newEnd = LocalTime.of(11, 0);
        assertTrue(TimeUtil.overlaps(newStart, newEnd, start, end));
    }

    @Test
    @DisplayName("Back-to-back earlier interval does NOT overlap (08:00-09:00 vs 09:00-10:00)")
    void testBackToBackEarlier() {
        LocalTime earlierStart = LocalTime.of(8, 0);
        LocalTime earlierEnd = LocalTime.of(9, 0);
        assertFalse(TimeUtil.overlaps(earlierStart, earlierEnd, start, end));
    }

    @Test
    @DisplayName("Back-to-back later interval does NOT overlap (10:00-11:00 vs 09:00-10:00)")
    void testBackToBackLater() {
        LocalTime laterStart = LocalTime.of(10, 0);
        LocalTime laterEnd = LocalTime.of(11, 0);
        assertFalse(TimeUtil.overlaps(laterStart, laterEnd, start, end));
    }

    @Test
    @DisplayName("Completely separate earlier interval does NOT overlap (07:00-08:00 vs 09:00-10:00)")
    void testCompletelySeparateEarlier() {
        LocalTime earlierStart = LocalTime.of(7, 0);
        LocalTime earlierEnd = LocalTime.of(8, 0);
        assertFalse(TimeUtil.overlaps(earlierStart, earlierEnd, start, end));
    }

    @Test
    @DisplayName("Completely separate later interval does NOT overlap (11:00-12:00 vs 09:00-10:00)")
    void testCompletelySeparateLater() {
        LocalTime laterStart = LocalTime.of(11, 0);
        LocalTime laterEnd = LocalTime.of(12, 0);
        assertFalse(TimeUtil.overlaps(laterStart, laterEnd, start, end));
    }

    @Test
    @DisplayName("One-minute overlap at end (09:00-10:01 vs 10:00-11:00)")
    void testOneMinuteOverlapAtEnd() {
        LocalTime t1Start = LocalTime.of(9, 0);
        LocalTime t1End = LocalTime.of(10, 1);
        LocalTime t2Start = LocalTime.of(10, 0);
        LocalTime t2End = LocalTime.of(11, 0);
        assertTrue(TimeUtil.overlaps(t1Start, t1End, t2Start, t2End));
    }

    @Test
    @DisplayName("One-minute overlap at start (08:00-09:01 vs 09:00-10:00)")
    void testOneMinuteOverlapAtStart() {
        LocalTime t1Start = LocalTime.of(8, 0);
        LocalTime t1End = LocalTime.of(9, 1);
        assertTrue(TimeUtil.overlaps(t1Start, t1End, start, end));
    }

    @Test
    @DisplayName("Rejects null or invalid interval arguments in overlaps()")
    void testOverlapsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(null, end, start, end));
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(start, null, start, end));
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(start, end, null, end));
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(start, end, start, null));

        // Start equal to or after end
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(end, start, start, end));
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(start, end, end, start));
        assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.overlaps(start, start, start, end));
    }

    @Test
    @DisplayName("Parses valid 24-hour time strings correctly")
    void testParseTimeValid() {
        assertEquals(LocalTime.of(9, 30), TimeUtil.parseTime("09:30"));
        assertEquals(LocalTime.of(9, 30), TimeUtil.parseTime("9:30"));
        assertEquals(LocalTime.of(14, 0), TimeUtil.parseTime("14:00"));
        assertEquals(LocalTime.of(0, 0), TimeUtil.parseTime("00:00"));
        assertEquals(LocalTime.of(23, 59), TimeUtil.parseTime("23:59"));
    }

    @Test
    @DisplayName("Rejects invalid time strings with descriptive exception")
    void testParseTimeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime(null));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("  "));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.parseTime("25:00"));
        assertTrue(ex1.getMessage().contains("Invalid time format"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.parseTime("09:60"));
        assertTrue(ex2.getMessage().contains("Invalid time format"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.parseTime("not-a-time"));
        assertTrue(ex3.getMessage().contains("Invalid time format"));
    }

    @Test
    @DisplayName("Parses day of week names and abbreviations case-insensitively")
    void testParseDayValid() {
        assertEquals(DayOfWeek.MONDAY, TimeUtil.parseDay("Monday"));
        assertEquals(DayOfWeek.MONDAY, TimeUtil.parseDay("mon"));
        assertEquals(DayOfWeek.MONDAY, TimeUtil.parseDay("MONDAY"));

        assertEquals(DayOfWeek.TUESDAY, TimeUtil.parseDay("Tuesday"));
        assertEquals(DayOfWeek.TUESDAY, TimeUtil.parseDay("tue"));

        assertEquals(DayOfWeek.WEDNESDAY, TimeUtil.parseDay("Wednesday"));
        assertEquals(DayOfWeek.WEDNESDAY, TimeUtil.parseDay("wed"));

        assertEquals(DayOfWeek.THURSDAY, TimeUtil.parseDay("Thursday"));
        assertEquals(DayOfWeek.THURSDAY, TimeUtil.parseDay("thu"));

        assertEquals(DayOfWeek.FRIDAY, TimeUtil.parseDay("Friday"));
        assertEquals(DayOfWeek.FRIDAY, TimeUtil.parseDay("fri"));

        assertEquals(DayOfWeek.SATURDAY, TimeUtil.parseDay("Saturday"));
        assertEquals(DayOfWeek.SATURDAY, TimeUtil.parseDay("sat"));

        assertEquals(DayOfWeek.SUNDAY, TimeUtil.parseDay("Sunday"));
        assertEquals(DayOfWeek.SUNDAY, TimeUtil.parseDay("sun"));
    }

    @Test
    @DisplayName("Rejects invalid day strings with descriptive exception")
    void testParseDayInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseDay(null));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseDay("   "));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.parseDay("Funday"));
        assertTrue(ex1.getMessage().contains("Invalid day of week"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                TimeUtil.parseDay("123"));
        assertTrue(ex2.getMessage().contains("Invalid day of week"));
    }

    @Test
    @DisplayName("Formats LocalTime to HH:mm string")
    void testFormatTime() {
        assertEquals("09:30", TimeUtil.formatTime(LocalTime.of(9, 30)));
        assertEquals("14:05", TimeUtil.formatTime(LocalTime.of(14, 5)));
        assertEquals("", TimeUtil.formatTime(null));
    }
}
