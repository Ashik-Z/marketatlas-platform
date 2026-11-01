package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.VaadinSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Route("")
@PageTitle("Overview | MarketAtlas")
public class HomeView extends VerticalLayout {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final Paragraph status = new Paragraph();
    private final Paragraph accountsNote = new Paragraph();
    private final Paragraph tradesNote = new Paragraph();
    private final Div cards = new Div();
    private final Grid<DashboardModel.RecentTrade> trades = new Grid<>(DashboardModel.RecentTrade.class, false);
    private final VerticalLayout content = new VerticalLayout();

    public HomeView(ApiClient api, UserSession session) {
        Button logout = new Button("Log out", e -> {
            session.logout();
            UI ui = UI.getCurrent();
            ui.getPage().setLocation("login");
            VaadinSession.getCurrent().getSession().invalidate();
        });

        cards.addClassName("ma-card-grid");
        trades.addColumn(t -> t.executedAt() == null ? "" : TIME.format(t.executedAt())).setHeader("Time").setAutoWidth(true);
        trades.addColumn(DashboardModel.RecentTrade::accountName).setHeader("Account");
        trades.addColumn(DashboardModel.RecentTrade::side).setHeader("Side");
        trades.addColumn(DashboardModel.RecentTrade::assetName).setHeader("Asset");
        trades.addColumn(t -> qty(t.quantity())).setHeader("Quantity");
        trades.addColumn(t -> money(t.price()) + " " + t.currency()).setHeader("Price");
        trades.setAllRowsVisible(true);

        content.setPadding(false);
        content.add(new H2("Your accounts"), accountsNote, cards, new H2("Latest trades"), tradesNote, trades);
        status.setVisible(false);

        HorizontalLayout links = new HorizontalLayout(
                link("Browse markets", "market"),
                link("Backtest lab", "backtest"),
                link("Accounts", "accounts"));
        add(new H1("Overview"), new Paragraph("Signed in as " + session.username() + "."), links, status, content, logout);

        load(api, session.authHeader());
    }

    private void load(ApiClient api, String authorization) {
        try {
            show(DashboardModel.load(api, authorization));
        } catch (ApiException e) {
            status.setText(BacktestView.message(e));
            status.setVisible(true);
            content.setVisible(false);
        }
    }

    private void show(DashboardModel.Model m) {
        cards.removeAll();
        m.cards().forEach(c -> cards.add(cardComponent(c)));
        if (m.cards().isEmpty()) {
            accountsNote.setText("You have no trading accounts yet. Create one under Accounts.");
        } else if (m.totalAccounts() > m.cards().size()) {
            accountsNote.setText("Showing the first " + m.cards().size() + " of " + m.totalAccounts() + " accounts.");
        } else {
            accountsNote.setText("");
        }
        accountsNote.setVisible(!accountsNote.getText().isEmpty());
        trades.setItems(m.trades());
        trades.setVisible(!m.trades().isEmpty());
        tradesNote.setText(m.trades().isEmpty() && !m.cards().isEmpty()
                ? "No trades yet. Place a market order from an account page." : "");
        tradesNote.setVisible(!tradesNote.getText().isEmpty());
        status.setVisible(false);
        content.setVisible(true);
    }

    private static Div cardComponent(DashboardModel.AccountCard c) {
        Div card = new Div();
        card.addClassName("ma-card");
        Span name = new Span(c.name());
        name.addClassName("ma-card-title");
        Span currency = new Span(c.currency() == null ? "" : c.currency());
        currency.addClassName("ma-muted");
        card.add(new HorizontalLayout(name, currency));
        if (!c.available()) {
            card.add(new Paragraph("Figures are unavailable right now."));
        } else {
            Div value = new Div(money(c.totalValue()));
            value.addClassName("ma-card-value");
            Div pnl = new Div(arrow(c.profitLoss()) + signed(c.profitLoss())
                    + (c.returnPct() == null ? "" : " (" + signed(c.returnPct()) + "%)"));
            pnl.addClassNames("ma-card-pnl", pnlClass(c.profitLoss()));
            Div split = new Div("Realized " + signed(c.realizedPnl()) + ", unrealized " + signed(c.unrealizedPnl()));
            split.addClassName("ma-muted");
            card.add(value, pnl, split);
            if (!c.valuationComplete()) {
                Div note = new Div("Some positions have no price, so the value leaves them out.");
                note.addClassName("ma-muted");
                card.add(note);
            }
        }
        card.add(link("Open account", "account/" + c.accountId()));
        return card;
    }

    private static Anchor link(String text, String href) {
        Anchor a = new Anchor(href, text);
        a.getElement().setAttribute("router-link", true);
        return a;
    }

    static String signed(BigDecimal v) {
        if (v == null) {
            return "n/a";
        }
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP);
        return (r.signum() > 0 ? "+" : "") + r.toPlainString();
    }

    static String arrow(BigDecimal v) {
        if (v == null || v.setScale(2, RoundingMode.HALF_UP).signum() == 0) {
            return "";
        }
        return v.signum() > 0 ? "\u25B2 " : "\u25BC ";
    }

    static String pnlClass(BigDecimal v) {
        if (v == null || v.setScale(2, RoundingMode.HALF_UP).signum() == 0) {
            return "ma-muted";
        }
        return v.signum() > 0 ? "ma-gain" : "ma-loss";
    }

    private static String money(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String qty(BigDecimal v) {
        return v == null ? "n/a" : v.stripTrailingZeros().toPlainString();
    }

    long cardCount() {
        return cards.getChildren().count();
    }

    Grid<DashboardModel.RecentTrade> tradesGrid() {
        return trades;
    }

    String statusText() {
        return status.getText();
    }

    String accountsNoteText() {
        return accountsNote.getText();
    }

    boolean contentVisible() {
        return content.isVisible();
    }
}