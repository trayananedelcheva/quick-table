# 🔧 СПЕШНА ПОПРАВКА: JWT Token Provider

## ⚠️ ПРОБЛЕМ

Създаденият `JwtTokenProvider.java` използва **ОСТАРЯЛО API** което НЕ работи с JJWT 0.12.3!

Грешката която виждаш е:
```
cannot find symbol: method parserBuilder()
location: class io.jsonwebtoken.Jwts
```

## ✅ РЕШЕНИЕ

**Отвори този файл:**
```
user-service/src/main/java/com/quicktable/userservice/security/JwtTokenProvider.java
```

**ИЗТРИЙ ЦЕЛИЯ код и копирай ТОЧНО ТОВА:**

---

```java
package com.quicktable.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

## 📝 Какво се промени:

| Остаряло (не работи) | Ново (работи с 0.12.3) |
|---------------------|------------------------|
| `import java.security.Key;` | `import javax.crypto.SecretKey;` |
| `.setClaims()` `.setSubject()` | `.claims()` `.subject()` |
| `.signWith(key, SignatureAlgorithm.HS256)` | `.signWith(key)` |
| `.parserBuilder()` | `.parser()` |
| `.setSigningKey()` | `.verifyWith()` |
| `.parseClaimsJws()` | `.parseSignedClaims()` |
| `.getBody()` | `.getPayload()` |
| `private Key getSigningKey()` | `private SecretKey getSigningKey()` |

## 🏃 След поправката

В PowerShell изпълни:
```powershell
cd c:\develop\university\quick-table
mvn clean install
```

Трябва да видиш **BUILD SUCCESS**! 🎉
