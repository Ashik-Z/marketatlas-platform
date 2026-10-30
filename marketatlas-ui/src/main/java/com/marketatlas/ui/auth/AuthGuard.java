package com.marketatlas.ui.auth;

import com.marketatlas.ui.HomeView;
import com.marketatlas.ui.LoginView;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Sends anonymous users to the login page and logged-in users away from public pages. */
@Component
public class AuthGuard implements VaadinServiceInitListener {

    private final ObjectProvider<UserSession> sessions;

    public AuthGuard(ObjectProvider<UserSession> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI().addBeforeEnterListener(this::beforeEnter));
    }

    private void beforeEnter(BeforeEnterEvent event) {
        UserSession session = sessions.getObject();
        boolean isPublic = PublicView.class.isAssignableFrom(event.getNavigationTarget());
        if (!isPublic && !session.isLoggedIn()) {
            event.forwardTo(LoginView.class);
        } else if (isPublic && session.isLoggedIn()) {
            event.forwardTo(HomeView.class);
        }
    }
}