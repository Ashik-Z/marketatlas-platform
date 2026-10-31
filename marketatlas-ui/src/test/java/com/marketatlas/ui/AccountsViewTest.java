package com.marketatlas.ui;

import com.marketatlas.ui.api.AccountSummary;
import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountsViewTest {

    private static final AccountSummary MAIN = new AccountSummary(3L, "Main", "USD",
            new BigDecimal("10000"), new BigDecimal("9000"));

    private static UserSession session() {
        UserSession s = mock(UserSession.class);
        when(s.authHeader()).thenReturn("Basic abc");
        return s;
    }

    @Test
    void validateNormalizesNameAndCurrency() {
        AccountsView.NewAccount a = AccountsView.validate("  Main ", "usd", new BigDecimal("10000"));

        assertEquals("Main", a.name());
        assertEquals("USD", a.currency());
    }

    @Test
    void validateRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("  ", "USD", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("x".repeat(101), "USD", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("Main", "US", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("Main", "U1D", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("Main", "USD", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("Main", "USD", new BigDecimal("1.0000001")));
        assertThrows(IllegalArgumentException.class, () -> AccountsView.validate("Main", "USD", new BigDecimal("100000000000000")));
        assertEquals(BigDecimal.ZERO, AccountsView.validate("Main", "USD", BigDecimal.ZERO).startingBalance());
    }

    @Test
    void listsTheAccounts() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic abc")).thenReturn(List.of(MAIN));

        AccountsView view = new AccountsView(api, session());

        assertEquals(1, view.grid().getListDataView().getItemCount());
        assertEquals("", view.statusText());
    }

    @Test
    void emptyListInvitesTheUserToCreateOne() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic abc")).thenReturn(List.of());

        AccountsView view = new AccountsView(api, session());

        assertEquals("You have no accounts yet. Create one above.", view.statusText());
    }

    @Test
    void createCallsTheApiAndReloadsTheList() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic abc")).thenReturn(List.of(MAIN));
        when(api.createAccount(eq("Basic abc"), eq("Main"), eq("USD"), eq(new BigDecimal("10000")))).thenReturn(MAIN);
        AccountsView view = new AccountsView(api, session());
        view.nameField().setValue("Main");

        view.create();

        verify(api).createAccount("Basic abc", "Main", "USD", new BigDecimal("10000"));
        verify(api, times(2)).listAccounts("Basic abc");
    }

    @Test
    void invalidInputNeverCallsTheApi() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic abc")).thenReturn(List.of(MAIN));
        AccountsView view = new AccountsView(api, session());

        view.create();

        assertEquals("Enter an account name.", view.statusText());
        verify(api, never()).createAccount(any(), any(), any(), any());
    }

    @Test
    void apiErrorOnCreateShowsTheMessage() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic abc")).thenReturn(List.of(MAIN));
        when(api.createAccount(any(), any(), any(), any())).thenThrow(
                new ApiException(ApiException.Kind.INVALID_INPUT, "name: must not be blank", null));
        AccountsView view = new AccountsView(api, session());
        view.nameField().setValue("Main");

        view.create();

        assertEquals("name: must not be blank", view.statusText());
    }
}