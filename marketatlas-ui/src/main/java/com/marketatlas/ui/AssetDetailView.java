package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.RealPrices;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.marketatlas.ui.api.DcaResult;
import java.util.ArrayList;
import java.util.List;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
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
    private final String authorization;
    private final Button watchButton = new Button();
    private boolean watching;
    private final BigDecimalField dcaAmount = new BigDecimalField("Amount per month");
    private final Paragraph dcaStatus = new Paragraph();
    private final Paragraph dcaNote = new Paragraph();
    private final Div dcaCards = new Div();
    private final Div dcaPanel = new Div();
    private List<String> dcaShown = List.of();

    public AssetDetailView(ApiClient api, UserSession session) {
        this.api = api;
        this.authorization = session.isLoggedIn() ? session.authHeader() : null;
        end.setValue(LocalDate.now());
        start.setValue(end.getValue().minusYears(2));
        Button show = new Button("Show");
        show.setDisableOnClick(true);
        show.addClickListener(e -> {
            try {
                load();
            } finally {
                show.setEnabled(true);
            }
        });
        chart.setVisible(false);
        status.setVisible(false);
        watchButton.addThemeVariants(ButtonVariant.AURA_TERTIARY);
        watchButton.addClickListener(e -> toggleWatch());
        watchButton.setVisible(false);
        HorizontalLayout heading = new HorizontalLayout(title, watchButton);
        heading.setAlignItems(Alignment.CENTER);
        dcaAmount.setValue(new BigDecimal("100"));
        Button dcaRun = new Button("Calculate");
        dcaRun.addThemeVariants(ButtonVariant.AURA_PRIMARY);
        dcaRun.setDisableOnClick(true);
        dcaRun.addClickListener(e -> {
            try {
                calculateDca();
            } finally {
                dcaRun.setEnabled(true);
            }
        });
        dcaStatus.setVisible(false);
        dcaCards.addClassName("ma-stat-strip");
        dcaPanel.addClassName("ma-lab-card");
        HorizontalLayout dcaForm = new HorizontalLayout(dcaAmount, dcaRun);
        dcaForm.setDefaultVerticalComponentAlignment(Alignment.END);
        dcaPanel.add(new H3("If you had bought every month"),
                new Paragraph("Buy a fixed amount on the first trading day of each month between the dates above, "
                        + "then see what it is worth today, before and after inflation."),
                dcaForm, dcaStatus, dcaCards, dcaNote);
        dcaPanel.setVisible(false);
        add(heading, new HorizontalLayout(start, end, show), status, summary, note, chart, dcaPanel);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        open(parameter);
    }

    void open(Long id) {
        this.assetId = id;
        title.setText("Asset #" + id);
        lookUpName(id);
        refreshWatchState();
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
            dcaPanel.setVisible(true);
            clearDca();
        } catch (ApiException e) {
            fail(friendly(e.getMessage()));
        }
    }

    private void lookUpName(Long id) {
        try {
            api.listAssets().stream().filter(a -> id.equals(a.id())).findFirst()
                    .ifPresent(a -> title.setText(a.name()));
        } catch (ApiException e) {
            // keep the generic "Asset #id" title
        }
    }

    private void refreshWatchState() {
        watchButton.setVisible(authorization != null);
        if (authorization == null) {
            return;
        }
        try {
            watching = api.watchlist(authorization).stream().anyMatch(e -> e.assetId().equals(assetId));
            showWatchLabel();
        } catch (ApiException e) {
            watchButton.setVisible(false);
        }
    }

    void toggleWatch() {
        try {
            if (watching) {
                api.unwatch(authorization, assetId);
                watching = false;
            } else {
                api.watch(authorization, assetId);
                watching = true;
            }
            showWatchLabel();
        } catch (ApiException e) {
            status.setText(e.getMessage());
            status.setVisible(true);
        }
    }

    private void showWatchLabel() {
        watchButton.setText(watching ? "\u2605 Watching" : "\u2606 Watch");
    }

    String watchLabel() {
        return watchButton.getText();
    }

    boolean watchVisible() {
        return watchButton.isVisible();
    }

    void calculateDca() {
        BigDecimal amount = dcaAmount.getValue();
        if (amount == null || amount.signum() <= 0) {
            dcaFail("Enter an amount greater than zero.");
            return;
        }
        if (start.getValue() == null || end.getValue() == null || start.getValue().isAfter(end.getValue())) {
            dcaFail("Choose valid dates above.");
            return;
        }
        try {
            showDca(api.dca(assetId, start.getValue(), end.getValue(), amount));
        } catch (ApiException e) {
            dcaFail(friendly(e.getMessage()));
        }
    }

    private void showDca(DcaResult r) {
        clearDca();
        String base = monthText(r.baseMonth());
        List<String> shown = new ArrayList<>();
        addDcaCard(shown, "Invested", money(r.investedNominal()),
                "In " + base + " dollars: " + money(r.investedReal()), false);
        addDcaCard(shown, "Worth now", money(r.valueNominal()),
                "In " + base + " dollars: " + money(r.valueReal()), false);
        addDcaCard(shown, "Nominal gain", money(r.nominalGain()), pct(r.nominalGainPct()), true);
        addDcaCard(shown, "Real gain", money(r.realGain()), pct(r.realGainPct()), true);
        dcaShown = List.copyOf(shown);
        boolean estimated = r.purchases().stream().anyMatch(DcaResult.Purchase::cpiEstimated);
        dcaNote.setText(r.purchaseCount() + " purchases of " + money(r.amountPerPurchase()) + " " + r.currency()
                + ", from " + r.purchases().get(0).date() + " to " + r.valuationDate() + ". Real figures are in "
                + base + " dollars." + (estimated ? " Some months use the latest published CPI. " : " ")
                + (r.attribution() == null ? "" : r.attribution()));
    }

    private void addDcaCard(List<String> shown, String label, String value, String sub, boolean tone) {
        Span l = new Span(label);
        l.addClassName("ma-stat-label");
        Span v = new Span(value);
        v.addClassName("ma-stat-value");
        if (tone) {
            String cls = AccountView.toneClass(value);
            if (cls != null) {
                v.addClassName(cls);
            }
        }
        Span s = new Span(sub);
        s.addClassName("ma-stat-sub");
        Div card = new Div(l, v, s);
        card.addClassName("ma-stat");
        dcaCards.add(card);
        shown.add(label + ": " + value);
    }

    private void dcaFail(String message) {
        clearDca();
        dcaStatus.setText(message);
        dcaStatus.setVisible(true);
    }

    private void clearDca() {
        dcaCards.removeAll();
        dcaShown = List.of();
        dcaNote.setText("");
        dcaStatus.setVisible(false);
    }

    private static String monthText(LocalDate d) {
        return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
    }

    private static String money(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    BigDecimalField dcaAmountField() {
        return dcaAmount;
    }

    List<String> dcaSummary() {
        return dcaShown;
    }

    String dcaStatusText() {
        return dcaStatus.getText();
    }

    boolean dcaPanelVisible() {
        return dcaPanel.isVisible();
    }
    static String friendly(String message) {
        if (message != null && message.contains("CPIAUCNS")) {
            return "Inflation data (CPI) has not been loaded yet, so real prices are unavailable. "
                    + "An administrator needs to refresh the CPI series.";
        }
        return message;
    }

    private void fail(String message) {
        status.setText(message);
        status.setVisible(true);
        summary.setText("");
        note.setText("");
        dcaPanel.setVisible(false);
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