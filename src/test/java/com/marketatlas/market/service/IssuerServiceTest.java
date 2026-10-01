package com.marketatlas.market.service;

import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.repository.IssuerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuerServiceTest {

    @Mock
    private IssuerRepository issuerRepository;

    private IssuerServiceImpl issuerService;

    @BeforeEach
    void setUp() {
        issuerService = new IssuerServiceImpl(issuerRepository);
    }

    @Test
    void shouldCreateIssuer() {

        Issuer issuer = new Issuer(
            "Apple",
            "United States"
        );

        when(issuerRepository.save(issuer))
            .thenReturn(issuer);

        Issuer savedIssuer =
            issuerService.createIssuer(issuer);

        assertEquals(issuer, savedIssuer);

        verify(issuerRepository)
            .save(issuer);
    }

    @Test
    void shouldFindIssuers() {

        Issuer apple = new Issuer(
            "Apple",
            "United States"
        );

        Issuer tesla = new Issuer(
            "Tesla",
            "United States"
        );

        List<Issuer> issuers =
            List.of(apple, tesla);

        when(issuerRepository.findAll())
            .thenReturn(issuers);

        List<Issuer> result =
            issuerService.findAllIssuers();

        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Tesla", result.get(1).getName());

        verify(issuerRepository)
            .findAll();
    }

    @Test
    void shouldFindIssuerById() {

        Issuer apple = new Issuer(
            "Apple",
            "United States"
        );

        Long id = 1L;

        when(issuerRepository.findById(id))
            .thenReturn(Optional.of(apple));

        Optional<Issuer> result =
            issuerService.findIssuerById(id);

        assertTrue(result.isPresent());
        assertEquals(apple, result.get());

        verify(issuerRepository)
            .findById(id);
    }
}