package com.marketatlas.trading.service;

import com.jayway.jsonpath.JsonPath;
import com.marketatlas.common.exception.OrderRejectedException;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.trading.model.Account;
import com.marketatlas.trading.model.TradeSide;
import com.marketatlas.trading.repository.AccountRepository;
import com.marketatlas.trading.repository.TradeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Not @Transactional on purpose: every thread needs its own real, committed transaction.
 * Cleans up after itself because the database is shared with other test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class OrderConcurrencyTest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:test-only-password".getBytes(StandardCharsets.UTF_8));

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderService orderService;
    @Autowired private AccountService accountService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private MarketDataRepository marketDataRepository;
    @Autowired private Clock clock;

    private Long assetId;

    @BeforeEach
    void setUp() throws Exception {
        String body = mockMvc.perform(post("/api/assets")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gold\", \"assetClass\": \"COMMODITY\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assetId = ((Number) JsonPath.read(body, "$.id")).longValue();
        Asset asset = assetRepository.findById(assetId).orElseThrow();
        marketDataRepository.save(
                new MarketData(2000.0, 2000.0, 2000.0, 2000.0, 1000.0, asset, LocalDate.now(clock)));
    }

    @AfterEach
    void cleanUp() {
        tradeRepository.deleteAll();
        accountRepository.deleteAll();
        marketDataRepository.deleteAll();
        assetRepository.deleteById(assetId);
    }

    private <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean tryOrder(Long accountId, String key, TradeSide side) {
        try {
            orderService.placeMarketOrder(accountId, assetId, side, BigDecimal.ONE, key);
            return true;
        } catch (OrderRejectedException rejected) {
            return false;
        }
    }

    private void assertCash(String expected, Long accountId) {
        assertEquals(0, new BigDecimal(expected)
                .compareTo(accountService.get(accountId).getCashBalance()));
    }

    @Test
    void concurrentBuysNeverOverspendTheAccount() throws Exception {
        Account account = accountService.create("Race", "USD", new BigDecimal("10000"));
        Long id = account.getId();

        // each buy costs 2000 + 1 commission, so exactly 4 of 8 fit into 10000
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String key = "race-" + i;
            tasks.add(() -> tryOrder(id, key, TradeSide.BUY));
        }
        long filled = runConcurrently(tasks).stream().filter(ok -> ok).count();

        assertEquals(4, filled);
        assertEquals(4, tradeRepository.count());
        assertCash("1996", id);
    }

    @Test
    void concurrentSellsNeverSellMoreThanHeld() throws Exception {
        Account account = accountService.create("Race", "USD", new BigDecimal("10000"));
        Long id = account.getId();
        orderService.placeMarketOrder(id, assetId, TradeSide.BUY, new BigDecimal("3"), "seed");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String key = "sell-" + i;
            tasks.add(() -> tryOrder(id, key, TradeSide.SELL));
        }
        long filled = runConcurrently(tasks).stream().filter(ok -> ok).count();

        assertEquals(3, filled);
        assertEquals(0, BigDecimal.ZERO.compareTo(tradeRepository.netQuantity(id, assetId)));
        // 10000 - 6001 for the seed buy, then 3 sales of 1999 each
        assertCash("9996", id);
    }

    @Test
    void concurrentRetriesWithOneKeyTradeOnlyOnce() throws Exception {
        Account account = accountService.create("Race", "USD", new BigDecimal("10000"));
        Long id = account.getId();

        List<Callable<OrderService.OrderResult>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> orderService.placeMarketOrder(
                    id, assetId, TradeSide.BUY, BigDecimal.ONE, "same-key"));
        }
        List<OrderService.OrderResult> results = runConcurrently(tasks);

        assertEquals(1, results.stream().filter(r -> !r.replayed()).count());
        assertEquals(1, results.stream().map(r -> r.trade().getId()).distinct().count());
        assertEquals(1, tradeRepository.count());
        assertCash("7999", id);
    }
}