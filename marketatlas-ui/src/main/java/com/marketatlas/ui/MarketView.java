package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("market")
@PageTitle("Markets | MarketAtlas")
public class MarketView extends VerticalLayout {

    private final Grid<AssetSummary> grid = new Grid<>(AssetSummary.class, false);
    private final Paragraph status = new Paragraph();

    public MarketView(ApiClient api) {
        grid.addColumn(AssetSummary::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(AssetSummary::assetClass).setHeader("Class");
        grid.addColumn(AssetSummary::symbol).setHeader("Symbol");
        grid.addColumn(AssetSummary::currency).setHeader("Currency");
        grid.addColumn(AssetSummary::unit).setHeader("Unit");
        grid.setAllRowsVisible(true);
        grid.addItemClickListener(e -> getUI().ifPresent(ui -> ui.navigate(AssetDetailView.class, e.getItem().id())));

        try {
            List<AssetSummary> assets = api.listAssets();
            grid.setItems(assets);
            status.setText(assets.isEmpty() ? "No assets are registered yet." : "");
        } catch (ApiException e) {
            status.setText(e.getMessage());
        }
        status.setVisible(!status.getText().isEmpty());

        add(new H2("Markets"), status, grid);
    }

    Grid<AssetSummary> grid() {
        return grid;
    }

    String statusText() {
        return status.getText();
    }
}