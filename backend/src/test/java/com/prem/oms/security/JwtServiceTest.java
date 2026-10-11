package com.prem.oms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";

    @Test
    void generatedTokenIsValidAndCarriesEmail() {
        JwtService svc = new JwtService(SECRET, 60_000);
        String token = svc.generateToken("a@b.com", "ADMIN");
        assertTrue(svc.isValid(token));
        assertEquals("a@b.com", svc.extractEmail(token));
    }

    @Test
    void tamperedOrExpiredTokensAreRejected() {
        JwtService svc = new JwtService(SECRET, 60_000);
        String token = svc.generateToken("a@b.com", "CUSTOMER");
        assertFalse(svc.isValid(token + "x"));
        assertFalse(svc.isValid("not-a-jwt"));

        JwtService expired = new JwtService(SECRET, -1_000);
        assertFalse(expired.isValid(expired.generateToken("a@b.com", "CUSTOMER")));
    }
}
