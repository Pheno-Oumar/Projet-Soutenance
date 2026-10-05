package com.kadi_aon.mon_salon.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 900000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtils.generateAccessToken(1L, "test@example.com", "ADMIN_SYSTEME");
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("test@example.com", jwtUtils.getEmailFromToken(token));
        assertEquals(1L, jwtUtils.getCompteIdFromToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.jwt.token"));
    }
}
