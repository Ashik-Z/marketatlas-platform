package com.marketatlas.ui;

import com.marketatlas.ui.api.AccountSummary;
import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Route("accounts")
@PageTitle("Accounts | MarketAtlas")
public class AccountsView extends VerticalLayout {

    record NewAccount(String name, String currency, BigDecimal startingBalance) {
    }

    private final ApiClient api;
    private final UserSession session;
    private final Grid<AccountSummary> grid = new Grid<>(AccountSummary.class, false);
    private final TextField name = new TextField("Account name");
    private final TextField currency = new TextField("Currency");
    private final BigDecimalField balance = new BigDecimalField("Starting balance");
    private final Paragraph status = new Paragraph();

    public AccountsView(ApiClient api, UserSession session) {
        this.api = api;
        this.session = session;
        currency.setValue("USD");
        currency.setMaxLength(3);
        balance.setValue(new BigDecimal("10000"));
        grid.addColumn(AccountSummary::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(AccountSummary::currency).setHeader("Currency");
        grid.addColumn(a -> money(a.cashBalance())).setHeader("Cash balance");
        grid.addColumn(a -> money(a.startingBalance())).setHeader("Starting balance");
        grid.setAllRowsVisible(true);
        grid.addItemClickListener(e -> getUI().ifPresent(ui -> ui.navigate(AccountView.class, e.getItem().id())));
        Button create = new Button("Create account", e -> create());
        HorizontalLayout form = new HorizontalLayout(name, currency, balance, create);
        form.setDefaultVerticalComponentAlignment(Alignment.END);
        form.setWrap(true);
        status.setVisible(false);
        add(new H2("Trading accounts"), form, status, grid);
        reload();
    }

    void reload() {
        try {
            List<AccountSummary> accounts = api.listAccounts(session.authHeader());
            grid.setItems(accounts);
            if (accounts.isEmpty()) {
                say("You have no accounts yet. Create one above.");
            }
        } catch (ApiException e) {
            say(BacktestView.message(e));
        }
    }

    void create() {
        NewAccount input;
        try {
            input = validate(name.getValue(), currency.getValue(), balance.getValue());
        } catch (IllegalArgumentException e) {
            say(e.getMessage());
            return;
        }
        try {
            AccountSummary created = api.createAccount(session.authHeader(), input.name(), input.currency(),
                    input.startingBalance());
            name.clear();
            status.setVisible(false);
            reload();
            getUI().ifPresent(ui -> ui.navigate(AccountView.class, created.id()));
        } catch (ApiException e) {
            say(BacktestView.message(e));
        }
    }

    static NewAccount validate(String name, String currency, BigDecimal balance) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new IllegalArgumentException("Enter an account name.");
        }
        if (n.length() > 100) {
            throw new IllegalArgumentException("The name can be at most 100 characters.");
        }
        String c = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!c.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter code such as USD.");
        }
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("Starting balance must be zero or more.");
        }
        BigDecimal b = balance.stripTrailingZeros();
        if (b.scale() > 6) {
            throw new IllegalArgumentException("Starting balance can have at most 6 decimal places.");
        }
        if (b.precision() - b.scale() > 14) {
            throw new IllegalArgumentException("Starting balance is too large.");
        }
        return new NewAccount(n, c, balance);
    }

    private void say(String message) {
        status.setText(message);
        status.setVisible(true);
    }

    private static String money(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    TextField nameField() {
        return name;
    }

    String statusText() {
        return status.getText();
    }

    Grid<AccountSummary> grid() {
        return grid;
    }
}