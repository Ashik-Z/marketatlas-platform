package com.marketatlas.market.service;

import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.repository.IssuerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IssuerServiceImpl implements IssuerService {

    private final IssuerRepository issuerRepository;
    
    public IssuerServiceImpl(IssuerRepository issuerRepository) {
        this.issuerRepository = issuerRepository;
    }

    @Override
    public Issuer createIssuer(Issuer issuer) {
        return issuerRepository.save(issuer);
    }

    @Override
    public List<Issuer> findAllIssuers() {
        return issuerRepository.findAll();
    }

    @Override
    public Optional<Issuer> findIssuerById(Long id) {
        return issuerRepository.findById(id);
    }
}