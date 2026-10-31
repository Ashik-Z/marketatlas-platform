package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Route("account")
@PageTitle("Account | MarketAtlas")
public class AccountView extends VerticalLayout implements HasUrlParameter<Long> {

    record Row(String label, String value) {
    }

    private final ApiClient api;
    private final UserSession session;
    private final H2 title = new H2("Account");
    private final Paragraph status = new Paragraph();
    private final Paragraph warning = new Paragraph();
    private final Grid<Row> summary = new Grid<>(Row.class, false);
    private final Grid<Portfolio.Position> positions = new Grid<>(Portfolio.Position.class, false);
    private final VerticalLayout content = new VerticalLayout();
    private Long accountId;

    public AccountView(ApiClient api, UserSession session) {
        this.api = api;
        this.session = session;
        summary.addColumn(Row::label).setHeader("Summary").setAutoWidth(true);
        summary.addColumn(Row::value).setHeader("Amount");
        summary.setAllRowsVisible(true);
        positions.addColumn(Portfolio.Position::assetName).setHeader("Asset").setAutoWidth(true);
        positions.addColumn(p -> qty(p.quantity())).setHeader("Quantity");
        positions.addColumn(p -> money(p.averageCost())).setHeader("Average cost");
        positions.addColumn(AccountView::priceText).setHeader("Latest price");
        positions.addColumn(p -> money(p.marketValue())).setHeader("Market value");
        positions.addColumn(p -> money(p.unrealizedPnl())).setHeader("Unrealized P&L");
        positions.setAllRowsVisible(true);
        content.setPadding(false);
        content.setVisible(false);
        content.add(summary, warning, new H3("Positions"), positions);
        status.setVisible(false);
        add(new Anchor("accounts", "Back to accounts"), title, status, content);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        open(parameter);
    }

    void open(Long id) {
        this.accountId = id;
        title.setText("Account #" + id);
        load();
    }

    void load() {
        try {
            show(api.portfolio(session.authHeader(), accountId));
        } catch (ApiException e) {
            status.setText(BacktestView.message(e));
            status.setVisible(true);
            content.setVisible(false);
        }
    }

    private void show(Portfolio p) {
        title.setText("Account #" + p.accountId() + " (" + p.currency() + ")");
        summary.setItems(summaryRows(p));
        warning.setText(p.valuationComplete() ? ""
                : "Some positions have no price yet, so the total value leaves them out.");
        warning.setVisible(!p.valuationComplete());
        positions.setItems(p.positions() == null ? List.of() : p.positions());
        status.setVisible(false);
        content.setVisible(true);
    }

    static List<Row> summaryRows(Portfolio p) {
        return List.of(
                new Row("Cash balance", money(p.cashBalance())),
                new Row("Positions value", money(p.positionsValue())),
                new Row("Total value", money(p.totalValue())),
                new Row("Profit / loss (total)", money(p.profitLoss())),
                new Row("Realized P&L", money(p.realizedPnl())),
                new Row("Unrealized P&L", money(p.unrealizedPnl())));
    }

    static String priceText(Portfolio.Position p) {
        if (p.latestPrice() == null) {
            return "no price";
        }
        return money(p.latestPrice()) + (p.stale() ? " (stale, " + p.priceDate() + ")" : "");
    }

    private static String money(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String qty(BigDecimal v) {
        return v == null ? "n/a" : v.stripTrailingZeros().toPlainString();
    }

    String statusText() {
        return status.getText();
    }

    boolean contentVisible() {
        return content.isVisible();
    }

    Grid<Row> summaryGrid() {
        return summary;
    }

    Grid<Portfolio.Position> positionsGrid() {
        return positions;
    }
}