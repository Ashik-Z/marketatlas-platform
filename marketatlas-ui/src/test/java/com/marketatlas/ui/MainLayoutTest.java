package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainLayoutTest {

    @Test
    void sidebarListsTheFourMainPagesInOrder() {
        UserSession session = mock(UserSession.class);
        when(session.username()).thenReturn("alice");

        MainLayout layout = new MainLayout(session);

        assertEquals(List.of("Overview", "Markets", "Backtest lab", "Accounts"), layout.navLabels());
    }
}
