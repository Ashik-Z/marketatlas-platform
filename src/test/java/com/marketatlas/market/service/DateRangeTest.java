package com.marketatlas.market.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    private static LocalDate d(int day) {
        return LocalDate.of(2026, 10, day);
    }

    private static DateRange r(int from, int to) {
        return new DateRange(d(from), d(to));
    }

    @Test
    void shouldReturnWholeRangeWhenNothingIsCovered() {
        assertEquals(List.of(r(1, 10)), DateRange.missing(r(1, 10), List.of()));
    }

    @Test
    void shouldReturnNothingWhenFullyCovered() {
        assertTrue(DateRange.missing(r(3, 8), List.of(r(1, 10))).isEmpty());
        assertTrue(DateRange.missing(r(1, 10), List.of(r(1, 10))).isEmpty());
    }

    @Test
    void shouldReturnBothSidesWhenMiddleIsCovered() {
        assertEquals(
                List.of(r(1, 3), r(8, 10)),
                DateRange.missing(r(1, 10), List.of(r(4, 7))));
    }

    @Test
    void shouldReturnOnlyTheSuffixWhenPrefixIsCovered() {
        assertEquals(List.of(r(6, 10)), DateRange.missing(r(1, 10), List.of(r(1, 5))));
    }

    @Test
    void shouldReturnOnlyThePrefixWhenSuffixIsCovered() {
        assertEquals(List.of(r(1, 5)), DateRange.missing(r(1, 10), List.of(r(6, 10))));
    }

    @Test
    void shouldTreatTouchingRangesAsContinuousCoverage() {
        assertTrue(DateRange.missing(r(1, 10), List.of(r(1, 5), r(6, 10))).isEmpty());
    }

    @Test
    void shouldReportSingleDayGapBetweenRanges() {
        assertEquals(
                List.of(r(6, 6)),
                DateRange.missing(r(1, 10), List.of(r(1, 5), r(7, 10))));
    }

    @Test
    void shouldHandleUnsortedAndOverlappingCoverage() {
        assertEquals(
                List.of(r(1, 1), r(9, 10)),
                DateRange.missing(r(1, 10), List.of(r(5, 8), r(2, 6), r(3, 4))));
    }

    @Test
    void shouldIgnoreCoverageOutsideTheRequestedRange() {
        assertEquals(
                List.of(r(10, 15)),
                DateRange.missing(r(10, 15), List.of(r(1, 5), r(20, 25))));
    }

    @Test
    void shouldHandleCoverageExtendingBeyondBothEnds() {
        assertTrue(DateRange.missing(r(5, 6), List.of(r(1, 3), r(4, 20))).isEmpty());
    }

    @Test
    void shouldHandleSingleDayRequests() {
        assertEquals(List.of(r(5, 5)), DateRange.missing(r(5, 5), List.of(r(1, 4), r(6, 9))));
        assertTrue(DateRange.missing(r(5, 5), List.of(r(5, 5))).isEmpty());
    }

    @Test
    void shouldNotModifyTheCoveredListPassedIn() {
        List<DateRange> covered = List.of(r(5, 8), r(1, 2));
        DateRange.missing(r(1, 10), covered);
        assertEquals(List.of(r(5, 8), r(1, 2)), covered);
    }

    @Test
    void shouldRejectInvertedRange() {
        assertThrows(IllegalArgumentException.class, () -> r(5, 4));
    }
}