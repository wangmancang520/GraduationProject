package com.example.springboot.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
@Profile("!demo")
public class BootstrapAdminConfig {
    @Bean
    public CommandLineRunner bootstrapAdmin(UserService users,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (email.isEmpty()) return;
            if (password.length() < 12) throw new IllegalArgumentException("初始化管理员密码至少需要12个字符");
            if (users.count(new QueryWrapper<User>().eq("u_login_name", email)) > 0) return;
            User user = new User(); user.setULoginName(email); user.setUEmail(email);
            user.setUName("系统管理员"); user.setUNickname("系统管理员");
            user.setUType(4); user.setUPassword(password); users.saveUser(user);
        };
    }
}
