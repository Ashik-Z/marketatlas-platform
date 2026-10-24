package com.marketatlas.market.validation;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssetValidatorTest {

    private final Validator validator;

    AssetValidatorTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void equityWithIssuerShouldBeValid() {
        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );
        Asset asset = new Asset(
                "Apple",
                AssetClass.EQUITY,
                apple
        );

        Set<ConstraintViolation<Asset>> violations = validator.validate(asset);

        assertTrue(violations.isEmpty());
    }

    @Test
    void equityWithoutIssuerShouldBeInvalid() {
        Asset asset = new Asset(
                "Apple",
                AssetClass.EQUITY,
                null
        );

        Set<ConstraintViolation<Asset>> violations = validator.validate(asset);

        assertFalse(violations.isEmpty());
    }

    @Test
    void commodityWithoutIssuerShouldBeValid() {
        Asset asset = new Asset(
                "Gold",
                AssetClass.COMMODITY,
                null
        );

        Set<ConstraintViolation<Asset>> violations = validator.validate(asset);

        assertTrue(violations.isEmpty());
    }

    @Test
    void commodityWithIssuerShouldBeInvalid() {
        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset asset = new Asset(
                "Gold",
                AssetClass.COMMODITY,
                apple
        );

        Set<ConstraintViolation<Asset>> violations = validator.validate(asset);

        assertFalse(violations.isEmpty());
    }
}