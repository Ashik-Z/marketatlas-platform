package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/** Landing page after login: a tour for regular users, and what only the administrator can do for the admin. */
@Route("welcome")
@PageTitle("Welcome | MarketAtlas")
public class WelcomeView extends VerticalLayout {

    private final H2 heading = new H2();
    private final List<WelcomeGuide.Step> steps;

    public WelcomeView(UserSession session, @Value("${marketatlas.ui.docs-url}") String docsUrl) {
        boolean admin = session.isAdmin();
        steps = admin ? WelcomeGuide.forAdmin(docsUrl) : WelcomeGuide.forUser();
        String name = session.username() == null ? "" : ", " + session.username();
        heading.setText("Welcome" + name + ".");

        Paragraph intro = new Paragraph(admin
                ? "You are signed in as the administrator. Here is what only you can do, and where to do it."
                : "Here is a quick tour of what you can do in MarketAtlas.");

        Div grid = new Div();
        grid.addClassName("ma-guide-grid");
        int n = 1;
        for (WelcomeGuide.Step step : steps) {
            Span num = new Span(String.valueOf(n++));
            num.addClassName("ma-guide-num");
            Anchor link = new Anchor(step.href(), step.linkLabel());
            if (step.external()) {
                link.setTarget("_blank");
                link.getElement().setAttribute("rel", "noopener noreferrer");
            }
            Div card = new Div(num, new H3(step.title()), new Paragraph(step.text()), link);
            card.addClassName("ma-guide-card");
            grid.add(card);
        }

        Paragraph note = new Paragraph("MarketAtlas is a learning and analysis tool. Trading here is simulated and "
                + "nothing on these pages is financial advice.");
        Button overview = new Button("Go to the overview", e -> UI.getCurrent().navigate(""));
        overview.addThemeVariants(ButtonVariant.AURA_PRIMARY);

        addClassName("ma-guide");
        add(heading, intro, grid, note, overview);
    }

    String headingText() {
        return heading.getText();
    }

    List<String> stepTitles() {
        return steps.stream().map(WelcomeGuide.Step::title).toList();
    }

    List<String> stepLinks() {
        return steps.stream().map(WelcomeGuide.Step::href).toList();
    }
}
