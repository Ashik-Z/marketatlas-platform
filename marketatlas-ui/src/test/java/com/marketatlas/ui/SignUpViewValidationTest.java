package com.marketatlas.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SignUpViewValidationTest {

    private static final String GOOD = "a-long-test-password";

    @Test
    void emptyUsernameIsRejected() {
        assertEquals("Choose a username.", SignUpView.validate("", GOOD, GOOD));
    }

    @Test
    void passwordUnderTwelveCharactersIsRejected() {
        assertEquals("The password must be at least 12 characters.", SignUpView.validate("alice", "short-pass", "short-pass"));
    }

    @Test
    void passwordOverSeventyTwoBytesIsRejectedEvenWhenItIsFewCharacters() {
        String multiByte = "\u00e9".repeat(40); // 40 characters, 80 bytes in UTF-8
        assertEquals("The password is too long (at most 72 bytes).", SignUpView.validate("alice", multiByte, multiByte));
    }

    @Test
    void mismatchedConfirmationIsRejected() {
        assertEquals("The two passwords do not match.", SignUpView.validate("alice", GOOD, GOOD + "x"));
    }

    @Test
    void validInputPasses() {
        assertNull(SignUpView.validate("alice", GOOD, GOOD));
    }
}