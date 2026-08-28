package dev.voidpulsar.lc_claim_economy.service;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static dev.voidpulsar.lc_claim_economy.service.WarDeclarationWindow.*;
import static org.junit.jupiter.api.Assertions.*;

class WarDeclarationWindowTest {

    // 2024-01-01 was a Monday, so this week is Mon..Sun 2024-01-01..2024-01-07.
    private static ZonedDateTime at(DayOfWeek day, int hour, int minute) {
        int offsetDays = day.getValue() - 1;
        return ZonedDateTime.of(2024, 1, 1 + offsetDays, hour, minute, 0, 0, ZoneOffset.UTC);
    }

    // --- isOpen: standard (non-wrapping) window, Friday 22:00 - Sunday 22:00 ---

    @Test
    void isOpen_atExactStart_isOpen() {
        assertTrue(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.FRIDAY, 22, 0)));
    }

    @Test
    void isOpen_justBeforeStart_isClosed() {
        assertFalse(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.FRIDAY, 21, 59)));
    }

    @Test
    void isOpen_middleOfWindow_isOpen() {
        assertTrue(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.SATURDAY, 12, 0)));
    }

    @Test
    void isOpen_justBeforeEnd_isOpen() {
        assertTrue(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.SUNDAY, 21, 59)));
    }

    @Test
    void isOpen_atExactEnd_isClosed() {
        assertFalse(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.SUNDAY, 22, 0)));
    }

    @Test
    void isOpen_outsideWindow_isClosed() {
        assertFalse(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.MONDAY, 12, 0)));
        assertFalse(isOpen(DayOfWeek.FRIDAY, 22, DayOfWeek.SUNDAY, 22, at(DayOfWeek.WEDNESDAY, 0, 0)));
    }

    // --- isOpen: wrapping window, Sunday 22:00 - Friday 22:00 (crosses the week boundary) ---

    @Test
    void isOpen_wrappingWindow_openMidweek() {
        assertTrue(isOpen(DayOfWeek.SUNDAY, 22, DayOfWeek.FRIDAY, 22, at(DayOfWeek.WEDNESDAY, 12, 0)));
    }

    @Test
    void isOpen_wrappingWindow_openJustAfterWeekBoundary() {
        assertTrue(isOpen(DayOfWeek.SUNDAY, 22, DayOfWeek.FRIDAY, 22, at(DayOfWeek.MONDAY, 0, 0)));
    }

    @Test
    void isOpen_wrappingWindow_closedOnWeekend() {
        assertFalse(isOpen(DayOfWeek.SUNDAY, 22, DayOfWeek.FRIDAY, 22, at(DayOfWeek.SATURDAY, 12, 0)));
    }

    @Test
    void isOpen_wrappingWindow_atExactStart_isOpen() {
        assertTrue(isOpen(DayOfWeek.SUNDAY, 22, DayOfWeek.FRIDAY, 22, at(DayOfWeek.SUNDAY, 22, 0)));
    }

    @Test
    void isOpen_wrappingWindow_atExactEnd_isClosed() {
        assertFalse(isOpen(DayOfWeek.SUNDAY, 22, DayOfWeek.FRIDAY, 22, at(DayOfWeek.FRIDAY, 22, 0)));
    }

    // --- isOpen: degenerate start == end treated as always open ---

    @Test
    void isOpen_startEqualsEnd_alwaysOpen() {
        assertTrue(isOpen(DayOfWeek.MONDAY, 5, DayOfWeek.MONDAY, 5, at(DayOfWeek.THURSDAY, 3, 0)));
    }

    // --- parseDay ---

    @Test
    void parseDay_validName_caseInsensitive() {
        assertEquals(DayOfWeek.FRIDAY, parseDay("friday", DayOfWeek.MONDAY));
        assertEquals(DayOfWeek.SUNDAY, parseDay("SUNDAY", DayOfWeek.MONDAY));
    }

    @Test
    void parseDay_invalidOrNull_usesFallback() {
        assertEquals(DayOfWeek.MONDAY, parseDay("notaday", DayOfWeek.MONDAY));
        assertEquals(DayOfWeek.MONDAY, parseDay(null, DayOfWeek.MONDAY));
    }
}
