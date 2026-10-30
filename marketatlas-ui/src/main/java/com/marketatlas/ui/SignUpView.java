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

import java.nio.charset.StandardCharsets;

@Route("signup")
@PageTitle("Sign up | MarketAtlas")
public class SignUpView extends VerticalLayout implements PublicView {

    private static final int MIN_PASSWORD_CHARS = 12;
    private static final int MAX_PASSWORD_BYTES = 72;

    public SignUpView(ApiClient api, UserSession session) {
        TextField username = new TextField("Username");
        username.setAutofocus(true);
        username.setWidth("20em");
        PasswordField password = new PasswordField("Password (12 to 72 characters)");
        password.setWidth("20em");
        PasswordField confirm = new PasswordField("Confirm password");
        confirm.setWidth("20em");

        Span error = new Span();
        error.getElement().setAttribute("role", "alert");
        error.getStyle().set("color", "var(--lumo-error-text-color)");
        error.setVisible(false);

        Button signUp = new Button("Create account");
        signUp.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        signUp.addClickShortcut(Key.ENTER);
        signUp.addClickListener(e -> {
            error.setVisible(false);
            String name = username.getValue().trim();
            String pass = password.getValue();
            String problem = validate(name, pass, confirm.getValue());
            if (problem != null) {
                show(error, problem);
                return;
            }
            try {
                api.signUp(name, pass);
                session.login(name, pass);
                UI.getCurrent().navigate(HomeView.class);
            } catch (ApiException ex) {
                show(error, LoginView.messageFor(ex));
            } finally {
                password.clear();
                confirm.clear();
            }
        });

        setAlignItems(Alignment.CENTER);
        add(new H1("Create your account"), username, password, confirm, error, signUp,
                new Anchor("login", "Back to log in"));
    }

    static String validate(String username, String password, String confirmation) {
        if (username.isEmpty()) {
            return "Choose a username.";
        }
        if (password.length() < MIN_PASSWORD_CHARS) {
            return "The password must be at least " + MIN_PASSWORD_CHARS + " characters.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "The password is too long (at most " + MAX_PASSWORD_BYTES + " bytes).";
        }
        if (!password.equals(confirmation)) {
            return "The two passwords do not match.";
        }
        return null;
    }

    private static void show(Span error, String text) {
        error.setText(text);
        error.setVisible(true);
    }
}