package com.marketatlas.ui;

import com.marketatlas.ui.api.BacktestResult.CurvePoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquityChartTest {

    private static CurvePoint pt(int day, String equity) {
        return new CurvePoint(LocalDate.of(2024, 1, day), new BigDecimal(equity));
    }

    @Test
    void buildsAlignedSeries() {
        String json = EquityChart.json(List.of(pt(2, "10000"), pt(3, "11250.50")), List.of(pt(2, "10000"), pt(3, "11500")));

        assertEquals("{\"dates\":[\"2024-01-02\",\"2024-01-03\"],\"strategy\":[10000,11250.50],\"benchmark\":[10000,11500]}", json);
    }

    @Test
    void missingBenchmarkDateBecomesAGap() {
        String json = EquityChart.json(List.of(pt(2, "10000"), pt(3, "10100")), List.of(pt(3, "10050")));

        assertEquals("{\"dates\":[\"2024-01-02\",\"2024-01-03\"],\"strategy\":[10000,10100],\"benchmark\":[null,10050]}", json);
    }

    @Test
    void emptyCurvesGiveEmptyArrays() {
        assertEquals("{\"dates\":[],\"strategy\":[],\"benchmark\":[]}", EquityChart.json(List.of(), List.of()));
    }
}