package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.api.TradePage;
import com.marketatlas.ui.api.TradeRecord;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Route("account")
@PageTitle("Account | MarketAtlas")
public class AccountView extends VerticalLayout implements HasUrlParameter<Long> {

    static final int PAGE_SIZE = 25;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    record Row(String label, String value) {
    }

    record OrderInput(Long assetId, String side, BigDecimal quantity) {
    }

    private final ApiClient api;
    private final UserSession session;
    private final H2 title = new H2("Account");
    private final Paragraph status = new Paragraph();
    private final Paragraph warning = new Paragraph();
    private final Grid<Row> summary = new Grid<>(Row.class, false);
    private final Grid<Portfolio.Position> positions = new Grid<>(Portfolio.Position.class, false);
    private final ComboBox<AssetSummary> asset = new ComboBox<>("Asset");
    private final Select<String> side = new Select<>();
    private final BigDecimalField quantity = new BigDecimalField("Quantity");
    private final Paragraph orderStatus = new Paragraph();
    private final Grid<TradeRecord> trades = new Grid<>(TradeRecord.class, false);
    private final Paragraph tradesStatus = new Paragraph();
    private final Span pageLabel = new Span();
    private final Button newer = new Button("Newer");
    private final Button older = new Button("Older");
    private final VerticalLayout content = new VerticalLayout();

    private List<AssetSummary> allAssets = List.of();
    private Map<Long, String> assetNames = Map.of();
    private String itemsCurrency;
    private String pendingKey;
    private int tradePage;
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

        try {
            List<AssetSummary> loaded = api.listAssets();
            allAssets = loaded == null ? List.of() : loaded;
        } catch (ApiException e) {
            orderStatus.setText(BacktestView.message(e));
        }
        assetNames = allAssets.stream().collect(Collectors.toMap(AssetSummary::id, AssetSummary::name, (a, b) -> a));
        asset.setItemLabelGenerator(a -> a.name() + " (" + a.symbol() + ")");
        side.setLabel("Side");
        side.setItems("BUY", "SELL");
        side.setItemLabelGenerator(s -> "BUY".equals(s) ? "Buy" : "Sell");
        side.setValue("BUY");
        asset.addValueChangeListener(e -> pendingKey = null);
        side.addValueChangeListener(e -> pendingKey = null);
        quantity.addValueChangeListener(e -> pendingKey = null);
        Button place = new Button("Place market order", e -> placeOrder());
        HorizontalLayout orderForm = new HorizontalLayout(asset, side, quantity, place);
        orderForm.setDefaultVerticalComponentAlignment(Alignment.END);
        orderForm.setWrap(true);

        trades.addColumn(t -> t.executedAt() == null ? "" : TIME.format(t.executedAt())).setHeader("Time").setAutoWidth(true);
        trades.addColumn(TradeRecord::side).setHeader("Side");
        trades.addColumn(t -> assetNames.getOrDefault(t.assetId(), "asset #" + t.assetId())).setHeader("Asset");
        trades.addColumn(t -> qty(t.quantity())).setHeader("Quantity");
        trades.addColumn(t -> money(t.price())).setHeader("Price");
        trades.addColumn(t -> money(t.commission())).setHeader("Commission");
        trades.setAllRowsVisible(true);
        newer.addClickListener(e -> {
            tradePage = Math.max(0, tradePage - 1);
            loadTrades();
        });
        older.addClickListener(e -> {
            tradePage++;
            loadTrades();
        });
        newer.setEnabled(false);
        older.setEnabled(false);

        content.setPadding(false);
        content.setVisible(false);
        content.add(summary, warning, new H3("Positions"), positions,
                new H3("Place a market order"), orderForm, orderStatus,
                new H3("Trade history"), tradesStatus, trades, new HorizontalLayout(newer, pageLabel, older));
        status.setVisible(false);
        orderStatus.setVisible(false);
        tradesStatus.setVisible(false);
        add(new Anchor("accounts", "Back to accounts"), title, status, content);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        open(parameter);
    }

    void open(Long id) {
        this.accountId = id;
        this.tradePage = 0;
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
            return;
        }
        loadTrades();
    }

    private void show(Portfolio p) {
        title.setText("Account #" + p.accountId() + " (" + p.currency() + ")");
        summary.setItems(summaryRows(p));
        warning.setText(p.valuationComplete() ? ""
                : "Some positions have no price yet, so the total value leaves them out.");
        warning.setVisible(!p.valuationComplete());
        positions.setItems(p.positions() == null ? List.of() : p.positions());
        if (!p.currency().equals(itemsCurrency)) {
            itemsCurrency = p.currency();
            asset.setItems(allAssets.stream().filter(a -> p.currency().equals(a.currency())).toList());
        }
        status.setVisible(false);
        content.setVisible(true);
    }

    void loadTrades() {
        try {
            TradePage page = api.trades(session.authHeader(), accountId, tradePage, PAGE_SIZE);
            tradesStatus.setVisible(false);
            if (page == null || page.content() == null) {
                trades.setItems(List.of());
                pageLabel.setText("");
                newer.setEnabled(false);
                older.setEnabled(false);
                return;
            }
            trades.setItems(page.content());
            pageLabel.setText("Page " + (page.page() + 1) + " of " + Math.max(1, page.totalPages())
                    + " (" + page.totalElements() + " trades)");
            newer.setEnabled(page.page() > 0);
            older.setEnabled(page.page() + 1 < page.totalPages());
        } catch (ApiException e) {
            tradesStatus.setText(BacktestView.message(e));
            tradesStatus.setVisible(true);
            trades.setItems(List.of());
        }
    }

    void placeOrder() {
        OrderInput input;
        try {
            input = validateOrder(asset.getValue(), side.getValue(), quantity.getValue());
        } catch (IllegalArgumentException e) {
            say(e.getMessage());
            return;
        }
        if (pendingKey == null) {
            pendingKey = UUID.randomUUID().toString();
        }
        try {
            TradeRecord t = api.placeOrder(session.authHeader(), accountId, pendingKey,
                    input.assetId(), input.side(), input.quantity());
            pendingKey = null;
            say(fillText(t));
            quantity.clear();
            tradePage = 0;
            load();
        } catch (ApiException e) {
            if (e.kind() != ApiException.Kind.UNAVAILABLE) {
                pendingKey = null;
            }
            say(BacktestView.message(e));
        }
    }

    private String fillText(TradeRecord t) {
        String name = assetNames.getOrDefault(t.assetId(), "asset #" + t.assetId());
        return "Filled: " + t.side() + " " + qty(t.quantity()) + " " + name + " at " + money(t.price())
                + " (commission " + money(t.commission()) + ").";
    }

    private void say(String message) {
        orderStatus.setText(message);
        orderStatus.setVisible(true);
    }

    static OrderInput validateOrder(AssetSummary asset, String side, BigDecimal quantity) {
        if (asset == null) {
            throw new IllegalArgumentException("Choose an asset.");
        }
        if (side == null) {
            throw new IllegalArgumentException("Choose buy or sell.");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        BigDecimal q = quantity.stripTrailingZeros();
        if (q.scale() > 6) {
            throw new IllegalArgumentException("Quantity can have at most 6 decimal places.");
        }
        if (q.precision() - q.scale() > 14) {
            throw new IllegalArgumentException("Quantity is too large.");
        }
        return new OrderInput(asset.id(), side, quantity);
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

    ComboBox<AssetSummary> assetBox() {
        return asset;
    }

    Select<String> sideSelect() {
        return side;
    }

    BigDecimalField quantityField() {
        return quantity;
    }

    String orderStatusText() {
        return orderStatus.getText();
    }

    Grid<TradeRecord> tradesGrid() {
        return trades;
    }

    String tradesStatusText() {
        return tradesStatus.getText();
    }

    String pageText() {
        return pageLabel.getText();
    }
}