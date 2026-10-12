package com.ansar.secureapi.security;

import com.ansar.secureapi.domain.AppUser;
import com.ansar.secureapi.domain.Role;
import com.ansar.secureapi.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Creates an ADMIN only if APP_ADMIN_PASSWORD is supplied. No default admin credentials ship with the app. */
@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner seedAdmin(UserRepository users, PasswordEncoder enc,
                                @Value("${app.admin.username:admin}") String username,
                                @Value("${app.admin.password:}") String password) {
        return args -> {
            if (!password.isBlank() && !users.existsByUsername(username)) {
                users.save(new AppUser(username, enc.encode(password), Role.ADMIN));
            }
        };
    }
}
