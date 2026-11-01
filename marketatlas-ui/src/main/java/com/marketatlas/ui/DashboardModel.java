package com.marketatlas.ui;

import com.marketatlas.ui.api.AccountSummary;
import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.api.TradePage;
import com.marketatlas.ui.api.TradeRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything the landing page shows: one P&L card per account and the newest trades across them. */
final class DashboardModel {

    static final int MAX_ACCOUNTS = 6;
    static final int MAX_TRADES = 8;

    record AccountCard(Long accountId, String name, String currency, BigDecimal totalValue, BigDecimal profitLoss,
                       BigDecimal returnPct, BigDecimal realizedPnl, BigDecimal unrealizedPnl,
                       boolean valuationComplete, boolean available) {
    }

    record RecentTrade(Instant executedAt, String accountName, String side, String assetName,
                       BigDecimal quantity, BigDecimal price, String currency) {
    }

    record Model(List<AccountCard> cards, List<RecentTrade> trades, int totalAccounts) {
    }

    private DashboardModel() {
    }

    /** Throws ApiException only when the account list itself cannot be loaded. */
    static Model load(ApiClient api, String authorization) {
        List<AccountSummary> all = api.listAccounts(authorization);
        List<AccountSummary> shown = all.size() > MAX_ACCOUNTS ? all.subList(0, MAX_ACCOUNTS) : all;
        Map<Long, String> assetNames = assetNames(api);
        List<AccountCard> cards = new ArrayList<>();
        List<RecentTrade> trades = new ArrayList<>();
        for (AccountSummary account : shown) {
            cards.add(card(account, portfolioOrNull(api, authorization, account.id())));
            trades.addAll(recentTrades(api, authorization, account, assetNames));
        }
        trades.sort(Comparator.comparing(RecentTrade::executedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        List<RecentTrade> newest = trades.size() > MAX_TRADES ? trades.subList(0, MAX_TRADES) : trades;
        return new Model(List.copyOf(cards), List.copyOf(newest), all.size());
    }

    static AccountCard card(AccountSummary account, Portfolio p) {
        if (p == null) {
            return new AccountCard(account.id(), account.name(), account.currency(), null, null, null, null, null,
                    false, false);
        }
        BigDecimal returnPct = null;
        if (p.startingBalance() != null && p.startingBalance().signum() > 0 && p.profitLoss() != null) {
            returnPct = p.profitLoss().multiply(BigDecimal.valueOf(100))
                    .divide(p.startingBalance(), 2, RoundingMode.HALF_UP);
        }
        return new AccountCard(account.id(), account.name(), p.currency(), p.totalValue(), p.profitLoss(), returnPct,
                p.realizedPnl(), p.unrealizedPnl(), p.valuationComplete(), true);
    }

    private static Portfolio portfolioOrNull(ApiClient api, String authorization, Long accountId) {
        try {
            return api.portfolio(authorization, accountId);
        } catch (ApiException e) {
            return null;
        }
    }

    private static List<RecentTrade> recentTrades(ApiClient api, String authorization, AccountSummary account,
                                                  Map<Long, String> assetNames) {
        try {
            TradePage page = api.trades(authorization, account.id(), 0, MAX_TRADES);
            if (page == null || page.content() == null) {
                return List.of();
            }
            List<RecentTrade> rows = new ArrayList<>();
            for (TradeRecord t : page.content()) {
                rows.add(new RecentTrade(t.executedAt(), account.name(), t.side(),
                        assetNames.getOrDefault(t.assetId(), "asset #" + t.assetId()),
                        t.quantity(), t.price(), account.currency()));
            }
            return rows;
        } catch (ApiException e) {
            return List.of();
        }
    }

    private static Map<Long, String> assetNames(ApiClient api) {
        Map<Long, String> names = new HashMap<>();
        try {
            for (AssetSummary a : api.listAssets()) {
                names.put(a.id(), a.name());
            }
        } catch (ApiException e) {
            // names fall back to "asset #id"
        }
        return names;
    }
}