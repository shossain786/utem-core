package com.utem.utem_core.service;

import com.utem.utem_core.entity.User;
import com.utem.utem_core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInitService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${utem.admin.username:admin}")
    private String adminUsername;

    @Value("${utem.admin.password:admin}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!securityEnabled) return;
        if (userRepository.count() > 0) return;

        User admin = User.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(User.Role.SUPER_ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        log.info("Created initial SUPER_ADMIN user '{}'", adminUsername);
    }
}
