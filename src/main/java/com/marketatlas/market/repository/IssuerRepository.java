package com.marketatlas.market.repository;

import com.marketatlas.market.entity.Issuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssuerRepository extends JpaRepository<Issuer, Long> {
    
}
