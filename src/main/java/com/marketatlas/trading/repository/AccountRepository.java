package com.marketatlas.trading.repository;

import com.marketatlas.trading.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}