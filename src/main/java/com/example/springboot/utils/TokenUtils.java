package com.example.springboot.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.springboot.entity.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Date;

public class TokenUtils {
    public static String generateToken(String userId, String passwordHash) {
        return JWT.create().withAudience(userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + 7200000))
                .sign(Algorithm.HMAC256(passwordHash));
    }
    public static User getCurrentUser() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : (User) attrs.getRequest().getAttribute("currentUser");
    }
}
