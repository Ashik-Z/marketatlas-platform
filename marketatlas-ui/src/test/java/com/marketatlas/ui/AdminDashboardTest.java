package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardTest {

    @Test
    void theAdminOverviewNeverAsksForAWatchlist() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAccounts("Basic x")).thenReturn(List.of());

        DashboardModel.Model model = DashboardModel.load(api, "Basic x", false);

        verify(api, never()).watchlist(any());
        assertTrue(model.watch().isEmpty());
    }
}
