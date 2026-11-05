package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route("market")
@PageTitle("Markets | MarketAtlas")
public class MarketView extends VerticalLayout {

    private final ApiClient api;
    private final String authorization;
    private final Grid<AssetSummary> grid = new Grid<>(AssetSummary.class, false);
    private final Paragraph status = new Paragraph();
    private final Set<Long> watched = new HashSet<>();

    public MarketView(ApiClient api, UserSession session) {
        this.api = api;
        this.authorization = session.isLoggedIn() && !session.isAdmin() ? session.authHeader() : null;

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
            showStatus(assets.isEmpty() ? "No assets are registered yet." : "");
        } catch (ApiException e) {
            showStatus(e.getMessage());
        }

        if (authorization != null) {
            addWatchColumn();
        }

        add(new H2("Markets"), status, grid);
    }

    private void addWatchColumn() {
        try {
            api.watchlist(authorization).forEach(entry -> watched.add(entry.assetId()));
        } catch (ApiException e) {
            showStatus(e.getMessage());
            return;
        }
        grid.addComponentColumn(asset -> {
            boolean on = watched.contains(asset.id());
            Button star = new Button(on ? "\u2605" : "\u2606", e -> toggle(asset));
            star.addThemeVariants(ButtonVariant.AURA_TERTIARY);
            star.setAriaLabel(on ? "Remove " + asset.name() + " from watchlist" : "Watch " + asset.name());
            return star;
        }).setHeader("Watch").setFlexGrow(0);
    }

    void toggle(AssetSummary asset) {
        try {
            if (watched.contains(asset.id())) {
                api.unwatch(authorization, asset.id());
                watched.remove(asset.id());
            } else {
                api.watch(authorization, asset.id());
                watched.add(asset.id());
            }
            showStatus("");
            grid.getDataProvider().refreshItem(asset);
        } catch (ApiException e) {
            showStatus(e.getMessage());
        }
    }

    private void showStatus(String text) {
        status.setText(text);
        status.setVisible(!text.isEmpty());
    }

    Grid<AssetSummary> grid() {
        return grid;
    }

    Set<Long> watchedIds() {
        return Set.copyOf(watched);
    }

    String statusText() {
        return status.getText();
    }
}


