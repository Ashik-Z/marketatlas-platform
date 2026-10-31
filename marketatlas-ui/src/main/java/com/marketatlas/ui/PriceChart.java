package com.marketatlas.ui;

import com.marketatlas.ui.api.RealPrices;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/** Line chart of nominal vs inflation-adjusted closes, drawn with Apache ECharts. */
@NpmPackage(value = "echarts", version = "5.6.0")
@JsModule("./price-chart.js")
public class PriceChart extends Div {

    public PriceChart() {
        setWidthFull();
        setHeight("420px");
    }

    public void show(RealPrices data) {
        String realLabel = "Real (" + data.baseMonth().getYear() + "-"
                + String.format("%02d", data.baseMonth().getMonthValue()) + " dollars)";
        getElement().executeJs("window.marketatlasPriceChart($0, $1, $2)", getElement(), json(data), realLabel);
    }

    /** Builds the chart payload from ISO dates and plain numbers only, so nothing needs escaping. */
    static String json(RealPrices data) {
        List<RealPrices.Point> pts = data.points();
        StringBuilder sb = new StringBuilder("{\"dates\":[");
        for (int i = 0; i < pts.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('"').append(pts.get(i).date()).append('"');
        }
        sb.append("],\"nominal\":").append(numbers(pts, RealPrices.Point::nominalClose));
        sb.append(",\"real\":").append(numbers(pts, RealPrices.Point::realClose)).append('}');
        return sb.toString();
    }

    private static String numbers(List<RealPrices.Point> pts, Function<RealPrices.Point, BigDecimal> f) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pts.size(); i++) {
            BigDecimal v = f.apply(pts.get(i));
            sb.append(i == 0 ? "" : ",").append(v == null ? "null" : v.toPlainString());
        }
        return sb.append(']').toString();
    }
}