package com.marketatlas.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("MarketAtlas")
public class HomeView extends VerticalLayout {

    public HomeView() {
        add(new H1("MarketAtlas"), new Paragraph("Market data, inflation-adjusted prices, paper trading and backtesting."));
    }
}