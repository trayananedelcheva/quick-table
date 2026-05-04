package com.quicktable.reservationservicev2.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class TestJwtUtil {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public static String generateToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        "userId", userId,
                        "role", role,
                        "firstName", "Test",
                        "lastName", "User"
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
