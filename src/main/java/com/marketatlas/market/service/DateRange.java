package com.marketatlas.market.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** An inclusive range of calendar dates. */
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Range bounds must not be null.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Range start must be before or equal to its end.");
        }
    }

    /**
     * Returns the parts of {@code requested} that are not inside any of the
     * {@code covered} ranges, in chronological order. Covered ranges may
     * overlap, touch, be unsorted, or extend beyond the requested range.
     */
    public static List<DateRange> missing(DateRange requested, List<DateRange> covered) {
        List<DateRange> sorted = new ArrayList<>(covered);
        sorted.sort(Comparator.comparing(DateRange::start));

        List<DateRange> gaps = new ArrayList<>();
        LocalDate cursor = requested.start();

        for (DateRange range : sorted) {
            if (range.end().isBefore(cursor)) {
                continue;
            }
            if (range.start().isAfter(requested.end())) {
                break;
            }
            if (range.start().isAfter(cursor)) {
                gaps.add(new DateRange(cursor, range.start().minusDays(1)));
            }
            cursor = range.end().plusDays(1);
            if (cursor.isAfter(requested.end())) {
                return gaps;
            }
        }

        if (!cursor.isAfter(requested.end())) {
            gaps.add(new DateRange(cursor, requested.end()));
        }
        return gaps;
    }
}