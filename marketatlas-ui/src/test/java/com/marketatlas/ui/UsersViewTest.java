package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.UserSummary;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersViewTest {

    private static UserSession session(boolean admin) {
        UserSession s = mock(UserSession.class);
        when(s.isAdmin()).thenReturn(admin);
        when(s.authHeader()).thenReturn("Basic x");
        return s;
    }

    @Test
    void theAdminSeesTheRegisteredUsers() {
        ApiClient api = mock(ApiClient.class);
        when(api.listUsers("Basic x")).thenReturn(List.of(
                new UserSummary(2L, "bob", "USER", Instant.parse("2026-11-02T09:13:00Z")),
                new UserSummary(1L, "alice", "USER", null)));

        UsersView view = new UsersView(api, session(true));

        assertEquals(List.of("bob", "alice"), view.shownUsernames());
        assertTrue(view.statusText().startsWith("2 registered"));
    }

    @Test
    void aRegularUserIsTurnedAwayWithoutAnyRequest() {
        ApiClient api = mock(ApiClient.class);

        UsersView view = new UsersView(api, session(false));

        assertEquals("Only the administrator can see this page.", view.statusText());
        verify(api, never()).listUsers(any());
    }

    @Test
    void anApiFailureIsShownInsteadOfAnEmptyList() {
        ApiClient api = mock(ApiClient.class);
        when(api.listUsers("Basic x"))
                .thenThrow(new ApiException(ApiException.Kind.UNAVAILABLE, "down", null));

        UsersView view = new UsersView(api, session(true));

        assertFalse(view.statusText().isBlank());
        assertTrue(view.shownUsernames().isEmpty());
    }
}
