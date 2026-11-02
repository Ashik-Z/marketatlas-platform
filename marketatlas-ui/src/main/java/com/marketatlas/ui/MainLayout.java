package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinSession;

import java.util.List;
import java.util.Set;

/** Shell for every page except log-in and sign-up: side navigation plus the signed-in user and Log out. */
@Layout
public class MainLayout extends AppLayout {

    private final SideNav nav = new SideNav();

    public MainLayout(UserSession session) {
        Span brand = new Span("MarketAtlas");
        brand.addClassName("ma-brand");

        SideNavItem overview = new SideNavItem("Overview", "", VaadinIcon.DASHBOARD.create());
        SideNavItem markets = new SideNavItem("Markets", "market", VaadinIcon.CHART.create());
        markets.setMatchNested(true);
        SideNavItem backtest = new SideNavItem("Backtest lab", "backtest", VaadinIcon.FLASK.create());
        SideNavItem accounts = new SideNavItem("Accounts", "accounts", VaadinIcon.WALLET.create());
        accounts.setMatchNested(true);
        accounts.setPathAliases(Set.of("account"));
        nav.addItem(overview, markets, backtest, accounts);

        Span user = new Span("Signed in as " + session.username());
        Button logout = new Button("Log out", e -> {
            session.logout();
            UI.getCurrent().getPage().setLocation("login");
            VaadinSession.getCurrent().getSession().invalidate();
        });
        Div footer = new Div(user, logout);
        footer.addClassName("ma-drawer-footer");

        addToNavbar(new DrawerToggle());
        addToDrawer(brand, nav, footer);
    }

    List<String> navLabels() {
        return nav.getItems().stream().map(SideNavItem::getLabel).toList();
    }
}
