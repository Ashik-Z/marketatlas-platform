package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSessionRoleTest {

    @Test
    void rememberedRoleIsClearedOnLogoutAndOnPlainLogin() {
        UserSession session = new UserSession();

        session.login("admin", "pw", true);
        assertTrue(session.isAdmin());

        session.logout();
        assertFalse(session.isAdmin());

        session.login("admin", "pw", true);
        session.login("carol", "pw");
        assertFalse(session.isAdmin());

        session.login("carol", "pw", false);
        assertFalse(session.isAdmin());
    }
}
