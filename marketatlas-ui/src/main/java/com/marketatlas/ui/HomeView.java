package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("")
@PageTitle("MarketAtlas")
public class HomeView extends VerticalLayout {

    public HomeView(UserSession session) {
        Button logout = new Button("Log out", e -> {
            session.logout();
            UI ui = UI.getCurrent();
            ui.getPage().setLocation("login");
            VaadinSession.getCurrent().getSession().invalidate();
        });
        add(new H1("MarketAtlas"),
                new Paragraph("Signed in as " + session.username() + "."),
                new Paragraph("Market data, inflation-adjusted prices, paper trading and backtesting."),
                new RouterLink("Browse markets", MarketView.class),
                logout);
    }
}