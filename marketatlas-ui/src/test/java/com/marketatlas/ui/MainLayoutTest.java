package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainLayoutTest {

    @Test
    void sidebarListsTheMainPagesInOrder() {
        UserSession session = mock(UserSession.class);
        when(session.username()).thenReturn("alice");

        MainLayout layout = new MainLayout(session);

        assertEquals(List.of("Overview", "Markets", "Backtest lab", "Accounts", "Guide"), layout.navLabels());
    }

    @Test
    void theAdminSeesAnExtraUsersPageBeforeTheGuide() {
        UserSession session = mock(UserSession.class);
        when(session.username()).thenReturn("admin");
        when(session.isAdmin()).thenReturn(true);

        assertEquals(List.of("Overview", "Markets", "Backtest lab", "Accounts", "Users", "Guide"),
                new MainLayout(session).navLabels());
    }}


