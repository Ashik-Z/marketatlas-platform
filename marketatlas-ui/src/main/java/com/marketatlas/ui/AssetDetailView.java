package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.RealPrices;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Route("market/asset")
@PageTitle("Asset | MarketAtlas")
public class AssetDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ApiClient api;
    private final H2 title = new H2("Asset");
    private final DatePicker start = new DatePicker("From");
    private final DatePicker end = new DatePicker("To");
    private final Paragraph status = new Paragraph();
    private final Paragraph summary = new Paragraph();
    private final Paragraph note = new Paragraph();
    private final PriceChart chart = new PriceChart();
    private Long assetId;

    public AssetDetailView(ApiClient api) {
        this.api = api;
        end.setValue(LocalDate.now());
        start.setValue(end.getValue().minusYears(2));
        Button show = new Button("Show", e -> load());
        chart.setVisible(false);
        status.setVisible(false);
        add(title, new HorizontalLayout(start, end, show), status, summary, note, chart);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        open(parameter);
    }

    void open(Long id) {
        this.assetId = id;
        title.setText("Asset #" + id);
        load();
    }

    void load() {
        if (start.getValue() == null || end.getValue() == null) {
            fail("Choose both dates.");
            return;
        }
        if (start.getValue().isAfter(end.getValue())) {
            fail("The start date must be on or before the end date.");
            return;
        }
        try {
            RealPrices data = api.realPrices(assetId, start.getValue(), end.getValue(), null);
            title.setText(data.assetName() + " (" + data.currency() + ")");
            summary.setText("Nominal return " + pct(data.nominalReturnPct()) + ", real return "
                    + pct(data.realReturnPct()) + " (in " + data.baseMonth().getYear() + "-"
                    + String.format("%02d", data.baseMonth().getMonthValue()) + " dollars).");
            boolean estimated = data.points().stream().anyMatch(p -> p.cpiEstimated());
            note.setText((estimated ? "Some months use the latest published CPI because newer data is not out yet. " : "")
                    + data.attribution());
            status.setVisible(false);
            chart.setVisible(true);
            chart.show(data);
        } catch (ApiException e) {
            fail(e.getMessage());
        }
    }

    private void fail(String message) {
        status.setText(message);
        status.setVisible(true);
        summary.setText("");
        note.setText("");
        chart.setVisible(false);
    }

    private static String pct(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    DatePicker startPicker() {
        return start;
    }

    DatePicker endPicker() {
        return end;
    }

    String titleText() {
        return title.getText();
    }

    String summaryText() {
        return summary.getText();
    }

    String statusText() {
        return status.getText();
    }

    boolean chartVisible() {
        return chart.isVisible();
    }
}