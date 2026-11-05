package com.marketatlas.ui;

import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelcomeViewTest {

    private static final String DOCS = "http://docs.test/swagger-ui.html";

    private static UserSession session(String name, boolean admin) {
        UserSession s = mock(UserSession.class);
        when(s.username()).thenReturn(name);
        when(s.isAdmin()).thenReturn(admin);
        return s;
    }

    @Test
    void aRegularUserGetsTheUserTourWithInAppLinks() {
        WelcomeView view = new WelcomeView(session("carol", false), DOCS);

        assertEquals("Welcome, carol.", view.headingText());
        assertEquals(6, view.stepTitles().size());
        assertTrue(view.stepTitles().contains("Test a strategy"));
        assertTrue(view.stepLinks().containsAll(List.of("market", "backtest", "accounts", "")));
        assertFalse(view.stepLinks().contains(DOCS));
    }

    @Test
    void theAdminGetsTheAdminGuideThatPointsAtTheApiDocs() {
        WelcomeView view = new WelcomeView(session("admin", true), DOCS);

        assertEquals("Welcome, admin.", view.headingText());
        assertEquals("Open the API docs", view.stepTitles().get(0));
        assertTrue(view.stepLinks().contains(DOCS));
        assertFalse(view.stepTitles().contains("Test a strategy"));
    }

    @Test
    void everyStepHasATitleTextAndLink() {
        for (WelcomeGuide.Step s : WelcomeGuide.forUser()) {
            assertFalse(s.title().isBlank());
            assertFalse(s.text().isBlank());
            assertFalse(s.linkLabel().isBlank());
        }
        for (WelcomeGuide.Step s : WelcomeGuide.forAdmin(DOCS)) {
            assertFalse(s.title().isBlank());
            assertFalse(s.text().isBlank());
            assertFalse(s.linkLabel().isBlank());
        }
    }
}
