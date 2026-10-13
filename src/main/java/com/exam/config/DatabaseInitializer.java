package com.exam.config;

import com.exam.entity.Role;
import com.exam.entity.User;
import com.exam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String emailToUse = StringUtils.hasText(adminEmail) ? adminEmail.trim().toLowerCase() : "admin@college.edu";
        String passwordToUse = StringUtils.hasText(adminPassword) ? adminPassword : "AdminSecurePass123!";
        String nameToUse = StringUtils.hasText(adminName) ? adminName.trim() : "System Administrator";

        User existingAdmin = userRepository.findByEmail(emailToUse).orElse(null);
        if (existingAdmin == null) {
            User admin = new User(
                    emailToUse,
                    passwordEncoder.encode(passwordToUse),
                    nameToUse,
                    Role.ADMIN
            );
            userRepository.save(admin);
            logger.info("Default administrator account verified and active for: {}", emailToUse);
        } else {
            // Keep password in sync with configured environment credentials
            existingAdmin.setPassword(passwordEncoder.encode(passwordToUse));
            existingAdmin.setRole(Role.ADMIN);
            userRepository.save(existingAdmin);
            logger.info("Administrator credentials synced successfully for: {}", emailToUse);
        }
    }
}
