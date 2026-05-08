package Premaify.com.example.web.config;

import Premaify.com.example.web.model.UserAccount;
import Premaify.com.example.web.model.UserRole;
import Premaify.com.example.web.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SuperAdminSeeder {
    @Bean
    CommandLineRunner createDefaultSuperAdmin(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${premaify.super-admin.email}") String superAdminEmail,
            @Value("${premaify.super-admin.password}") String superAdminPassword
    ) {
        return args -> {
            if (userAccountRepository.existsByEmailIgnoreCase(superAdminEmail)) {
                return;
            }

            UserAccount superAdmin = new UserAccount();
            superAdmin.setEmail(superAdminEmail.toLowerCase());
            superAdmin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
            superAdmin.setRole(UserRole.SUPER_ADMIN);
            superAdmin.setEnabled(true);
            userAccountRepository.save(superAdmin);
        };
    }
}
