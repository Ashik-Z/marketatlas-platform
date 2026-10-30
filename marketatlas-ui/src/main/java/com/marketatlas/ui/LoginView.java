package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.auth.PublicView;
import com.marketatlas.ui.auth.UserSession;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Log in | MarketAtlas")
public class LoginView extends VerticalLayout implements PublicView {

    public LoginView(ApiClient api, UserSession session) {
        TextField username = new TextField("Username");
        username.setAutofocus(true);
        username.setWidth("20em");
        PasswordField password = new PasswordField("Password");
        password.setWidth("20em");

        Span error = new Span();
        error.getElement().setAttribute("role", "alert");
        error.getStyle().set("color", "var(--lumo-error-text-color)");
        error.setVisible(false);

        Button login = new Button("Log in");
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        login.addClickShortcut(Key.ENTER);
        login.addClickListener(e -> {
            error.setVisible(false);
            String name = username.getValue().trim();
            String pass = password.getValue();
            if (name.isEmpty() || pass.isEmpty()) {
                show(error, "Enter your username and password.");
                return;
            }
            try {
                api.checkLogin(name, pass);
                session.login(name, pass);
                UI.getCurrent().navigate(HomeView.class);
            } catch (ApiException ex) {
                show(error, messageFor(ex));
            } finally {
                password.clear();
            }
        });

        setAlignItems(Alignment.CENTER);
        add(new H1("MarketAtlas"), username, password, error, login,
                new Span("No account yet?"), new Anchor("signup", "Create an account"));
    }

    static String messageFor(ApiException ex) {
        if (ex.kind() == ApiException.Kind.RATE_LIMITED && ex.retryAfterSeconds() != null) {
            return "Too many attempts. Try again in " + ex.retryAfterSeconds() + " seconds.";
        }
        return ex.getMessage();
    }

    private static void show(Span error, String text) {
        error.setText(text);
        error.setVisible(true);
    }
}