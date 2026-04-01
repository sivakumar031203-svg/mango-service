package com.siva.mango_bt.Config;

import com.siva.mango_bt.Entity.AdminUser;
import com.siva.mango_bt.Repos.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin@mango2024}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!adminUserRepository.existsByUsername(adminUsername)) {
            AdminUser admin = AdminUser.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role("ADMIN")
                    .build();
            adminUserRepository.save(admin);
            System.out.println("✅ Admin user created: " + adminUsername);
        }
    }
}
