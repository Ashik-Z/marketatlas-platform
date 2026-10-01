package com.marketatlas.market.service;
import com.marketatlas.market.entity.Issuer;

import java.util.Optional;
import java.util.List;



public interface IssuerService {
    Issuer createIssuer(Issuer issuer);
    List<Issuer> findAllIssuers();
    Optional<Issuer> findIssuerById(Long id);
}
