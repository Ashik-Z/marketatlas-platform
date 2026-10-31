package com.marketatlas.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;

/** Page-level setup: the Aura theme plus MarketAtlas's own tokens (palette, chart colors). */
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("marketatlas.css")
public class UiAppShell implements AppShellConfigurator {
}