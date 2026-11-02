package com.marketatlas.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/** Shared split layout for the public log-in and sign-up pages: brand panel on the left, form on the right. */
final class AuthPage {

    private static final String MARK = """
            <svg class="ma-mark" viewBox="0 0 240 120" role="img"
                 aria-label="The nominal price line rises faster than the inflation-adjusted line">
              <polyline class="ma-mark-nominal" points="0,100 40,85 80,70 120,52 160,38 200,22 240,8"/>
              <polyline class="ma-mark-real" points="0,100 40,92 80,84 120,78 160,70 200,66 240,58"/>
            </svg>""";

    private AuthPage() {
    }

    static Div wrap(String heading, String subheading, Component... parts) {
        Span name = new Span("MarketAtlas");
        name.addClassName("ma-auth-name");
        Paragraph tagline = new Paragraph("Every price twice: as quoted, and adjusted for inflation.");
        tagline.addClassName("ma-auth-tagline");
        Span nominal = new Span("Nominal");
        nominal.addClassName("ma-legend-nominal");
        Span real = new Span("Real");
        real.addClassName("ma-legend-real");
        Div legend = new Div(nominal, real);
        legend.addClassName("ma-legend");
        Div brand = new Div(name, tagline, new Html(MARK), legend);
        brand.addClassName("ma-auth-brand");

        Paragraph sub = new Paragraph(subheading);
        sub.addClassName("ma-muted");
        Div box = new Div(new H1(heading), sub);
        box.addClassName("ma-auth-box");
        box.add(parts);
        Div form = new Div(box);
        form.addClassName("ma-auth-form");

        Div root = new Div(brand, form);
        root.addClassName("ma-auth");
        return root;
    }

    static Div alt(String text, Component link) {
        Div alt = new Div(new Span(text + " "), link);
        alt.addClassName("ma-auth-alt");
        return alt;
    }
}
