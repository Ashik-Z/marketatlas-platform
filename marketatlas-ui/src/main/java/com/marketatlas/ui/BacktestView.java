package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.BacktestParams;
import com.marketatlas.ui.api.BacktestResult;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route("backtest")
@PageTitle("Backtest lab | MarketAtlas")
public class BacktestView extends VerticalLayout {

    static final String BUY_AND_HOLD = "BUY_AND_HOLD";
    static final String SMA = "SMA_CROSSOVER";

    record Row(String label, String strategy, String benchmark) {
    }

    private final ApiClient api;
    private final UserSession session;
    private final ComboBox<AssetSummary> asset = new ComboBox<>("Asset");
    private final Select<String> strategy = new Select<>();
    private final IntegerField shortWindow = new IntegerField("Short window");
    private final IntegerField longWindow = new IntegerField("Long window");
    private final DatePicker start = new DatePicker("From");
    private final DatePicker end = new DatePicker("To");
    private final BigDecimalField cash = new BigDecimalField("Initial cash");
    private final IntegerField slippage = new IntegerField("Slippage (bps)");
    private final Paragraph status = new Paragraph();
    private final Paragraph headline = new Paragraph();
    private final Paragraph note = new Paragraph();
    private final Grid<Row> metrics = new Grid<>(Row.class, false);
    private final Grid<BacktestResult.Trade> trades = new Grid<>(BacktestResult.Trade.class, false);
    private final EquityChart chart = new EquityChart();
    private final VerticalLayout result = new VerticalLayout();

    public BacktestView(ApiClient api, UserSession session) {
        this.api = api;
        this.session = session;

        strategy.setLabel("Strategy");
        strategy.setItems(SMA, BUY_AND_HOLD);
        strategy.setItemLabelGenerator(s -> SMA.equals(s) ? "SMA crossover" : "Buy and hold");
        strategy.setValue(SMA);
        shortWindow.setValue(20);
        longWindow.setValue(50);
        end.setValue(LocalDate.now());
        start.setValue(end.getValue().minusYears(2));
        cash.setValue(new BigDecimal("10000"));
        slippage.setValue(5);
        asset.setItemLabelGenerator(a -> a.name() + " (" + a.symbol() + ")");
        HorizontalLayout windows = new HorizontalLayout(shortWindow, longWindow);
        strategy.addValueChangeListener(e -> windows.setVisible(SMA.equals(e.getValue())));

        metrics.addColumn(Row::label).setHeader("").setAutoWidth(true);
        metrics.addColumn(Row::strategy).setHeader("Strategy");
        metrics.addColumn(Row::benchmark).setHeader("Buy and hold");
        metrics.setAllRowsVisible(true);
        trades.addColumn(BacktestResult.Trade::date).setHeader("Date");
        trades.addColumn(BacktestResult.Trade::side).setHeader("Side");
        trades.addColumn(t -> num(t.quantity())).setHeader("Quantity");
        trades.addColumn(t -> money(t.price())).setHeader("Price");
        trades.addColumn(t -> money(t.commission())).setHeader("Commission");
        trades.setAllRowsVisible(true);

        try {
            asset.setItems(api.listAssets());
        } catch (ApiException e) {
            status.setText(message(e));
        }
        status.setVisible(!status.getText().isEmpty());
        result.setPadding(false);
        result.setVisible(false);
        result.add(headline, metrics, note, chart, new H3("Trades"), trades);

        Button run = new Button("Run backtest", e -> run());
        HorizontalLayout form = new HorizontalLayout(asset, strategy, windows, start, end, cash, slippage, run);
        form.setWrap(true);
        form.setDefaultVerticalComponentAlignment(Alignment.END);
        add(new H2("Backtest lab"), form, status, result);
    }

    void run() {
        BacktestParams params;
        try {
            params = buildParams(asset.getValue(), start.getValue(), end.getValue(), strategy.getValue(),
                    shortWindow.getValue(), longWindow.getValue(), cash.getValue(), slippage.getValue());
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
            return;
        }
        try {
            show(api.backtest(asset.getValue().id(), session.authHeader(), params));
        } catch (ApiException e) {
            fail(message(e));
        }
    }

    private void show(BacktestResult r) {
        status.setVisible(false);
        headline.setText(r.assetName() + ": " + r.barsUsed() + " daily bars, " + r.firstBarDate() + " to "
                + r.lastBarDate() + (r.skippedBars() > 0 ? " (" + r.skippedBars() + " unusable bars skipped)" : "") + ".");
        metrics.setItems(rows(r));
        note.setText(r.realTerms() == null
                ? (r.realTermsNote() == null ? "" : r.realTermsNote())
                : "Inflation over the period: " + pct(r.realTerms().inflationPct())
                        + (r.realTerms().cpiEstimated() ? " (latest CPI month estimated)." : "."));
        trades.setItems(r.trades() == null ? List.of() : r.trades());
        result.setVisible(true);
        if (r.equityCurve() != null && !r.equityCurve().isEmpty()) {
            chart.setVisible(true);
            chart.show(r.equityCurve(), r.benchmarkEquityCurve() == null ? List.of() : r.benchmarkEquityCurve());
        } else {
            chart.setVisible(false);
        }
    }

    private void fail(String message) {
        status.setText(message);
        status.setVisible(true);
        result.setVisible(false);
    }

    static BacktestParams buildParams(AssetSummary asset, LocalDate start, LocalDate end, String strategy,
                                      Integer shortW, Integer longW, BigDecimal cash, Integer slippage) {
        if (asset == null) {
            throw new IllegalArgumentException("Choose an asset.");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Choose a strategy.");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Choose both dates.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("The start date must be on or before the end date.");
        }
        if (cash == null || cash.signum() <= 0) {
            throw new IllegalArgumentException("Initial cash must be greater than zero.");
        }
        int slip = slippage == null ? 0 : slippage;
        if (slip < 0 || slip > 1000) {
            throw new IllegalArgumentException("Slippage must be between 0 and 1000 bps.");
        }
        boolean sma = SMA.equals(strategy);
        if (sma) {
            if (shortW == null || longW == null) {
                throw new IllegalArgumentException("Enter both moving-average windows.");
            }
            if (shortW < 1 || longW > 400 || shortW >= longW) {
                throw new IllegalArgumentException("The short window must be smaller than the long window (at most 400).");
            }
        }
        return new BacktestParams(start, end, strategy, sma ? shortW : null, sma ? longW : null, cash, null, slip, true);
    }

    static List<Row> rows(BacktestResult r) {
        BacktestResult.Metrics m = r.metrics();
        BacktestResult.Metrics b = r.benchmark();
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Total return", pct(m.totalReturnPct()), pct(b.totalReturnPct())));
        rows.add(new Row("Annualized return", pct(m.annualizedReturnPct()), pct(b.annualizedReturnPct())));
        rows.add(new Row("Max drawdown", pct(m.maxDrawdownPct()), pct(b.maxDrawdownPct())));
        rows.add(new Row("Volatility (annualized)", pct(m.annualizedVolatilityPct()), pct(b.annualizedVolatilityPct())));
        rows.add(new Row("Time in market", pct(m.exposurePct()), pct(b.exposurePct())));
        rows.add(new Row("Trades", String.valueOf(m.trades()), String.valueOf(b.trades())));
        rows.add(new Row("Commission paid", money(m.commissionPaid()), money(b.commissionPaid())));
        BacktestResult.RealTerms t = r.realTerms();
        if (t != null) {
            rows.add(new Row("Real total return (" + t.baseMonth() + " dollars)",
                    pct(t.realTotalReturnPct()), pct(t.benchmarkRealTotalReturnPct())));
            rows.add(new Row("Real final equity", money(t.realFinalEquity()), money(t.benchmarkRealFinalEquity())));
        }
        return rows;
    }

    static String message(ApiException e) {
        Long wait = e.retryAfterSeconds();
        return e.getMessage() + (e.kind() == ApiException.Kind.RATE_LIMITED && wait != null
                ? " Try again in " + wait + " seconds." : "");
    }

    private static String pct(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String money(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String num(BigDecimal v) {
        return v == null ? "n/a" : v.stripTrailingZeros().toPlainString();
    }

    ComboBox<AssetSummary> assetBox() {
        return asset;
    }

    String statusText() {
        return status.getText();
    }

    boolean resultVisible() {
        return result.isVisible();
    }

    Grid<Row> metricsGrid() {
        return metrics;
    }
}