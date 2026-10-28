package com.marketatlas.trading.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.trading.dto.TradePageResponse;
import com.marketatlas.trading.dto.TradeResponse;
import com.marketatlas.trading.repository.AccountRepository;
import com.marketatlas.trading.repository.TradeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeHistoryService {

    // Fixed on the server: callers cannot choose a sort field.
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("executedAt"), Sort.Order.desc("id"));

    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;

    public TradeHistoryService(AccountRepository accountRepository, TradeRepository tradeRepository) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional(readOnly = true)
    public TradePageResponse list(Long accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
        Page<com.marketatlas.trading.model.Trade> result =
                tradeRepository.findByAccountId(accountId, PageRequest.of(page, size, NEWEST_FIRST));
        return new TradePageResponse(
                result.getContent().stream().map(TradeResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}