package com.marketatlas.ui;

import com.marketatlas.ui.api.BacktestResult.CurvePoint;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Strategy vs buy-and-hold equity curves, drawn with Apache ECharts. */
@JsModule("./equity-chart.js")
public class EquityChart extends Div {

    public EquityChart() {
        setWidthFull();
        setHeight("420px");
    }

    public void show(List<CurvePoint> strategy, List<CurvePoint> benchmark) {
        getElement().executeJs("window.marketatlasEquityChart($0, $1)", getElement(), json(strategy, benchmark));
    }

    /** Uses the strategy's dates; a benchmark value missing for a date becomes a null gap. */
    static String json(List<CurvePoint> strategy, List<CurvePoint> benchmark) {
        Map<LocalDate, BigDecimal> bench = new HashMap<>();
        for (CurvePoint p : benchmark) {
            bench.put(p.date(), p.equity());
        }
        StringBuilder dates = new StringBuilder("[");
        StringBuilder strat = new StringBuilder("[");
        StringBuilder ben = new StringBuilder("[");
        for (int i = 0; i < strategy.size(); i++) {
            CurvePoint p = strategy.get(i);
            String sep = i == 0 ? "" : ",";
            dates.append(sep).append('"').append(p.date()).append('"');
            strat.append(sep).append(number(p.equity()));
            ben.append(sep).append(number(bench.get(p.date())));
        }
        return "{\"dates\":" + dates + "],\"strategy\":" + strat + "],\"benchmark\":" + ben + "]}";
    }

    private static String number(BigDecimal v) {
        return v == null ? "null" : v.toPlainString();
    }
}