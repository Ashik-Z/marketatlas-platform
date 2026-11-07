package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.UserSummary;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;

/** Administrator-only, read-only list of registered users. */
@Route("users")
@PageTitle("Users | MarketAtlas")
public class UsersView extends VerticalLayout {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final Grid<UserSummary> grid = new Grid<>();
    private final Paragraph status = new Paragraph();
    private List<UserSummary> shown = List.of();

    public UsersView(ApiClient api, UserSession session) {
        add(new H2("Registered users"));
        if (!session.isAdmin()) {
            status.setText("Only the administrator can see this page.");
            add(status);
            return;
        }
        grid.addColumn(UserSummary::username).setHeader("Username").setAutoWidth(true);
        grid.addColumn(UserSummary::role).setHeader("Role");
        grid.addColumn(u -> u.createdAt() == null ? "" : WHEN.format(u.createdAt())).setHeader("Registered");
        grid.setAllRowsVisible(true);
        try {
            shown = api.listUsers(session.authHeader());
            grid.setItems(shown);
            status.setText(shown.isEmpty()
                    ? "No users have signed up yet."
                    : shown.size() + " registered user(s). The administrator account is not listed.");
        } catch (ApiException e) {
            status.setText(BacktestView.message(e));
            grid.setVisible(false);
        }
        add(status, grid);
    }

    String statusText() {
        return status.getText();
    }

    List<String> shownUsernames() {
        return shown.stream().map(UserSummary::username).toList();
    }
}
