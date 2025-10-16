package org.ww2.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.ww2.entity.User;
import org.ww2.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

        createTestUsers();
    }

    private void createTestUsers() {

        if (userRepository.findByUsername("customer").isEmpty()) {
            User customer = new User();
            customer.setUsername("customer");
            customer.setPassword("password");
            customer.setEmail("customer@example.com");
            customer.setRole(User.UserRole.CUSTOMER);
            customer.setIsOnline(false);
            userRepository.save(customer);
            log.info("Created test customer user");
        }

        if (userRepository.findByUsername("support").isEmpty()) {
            User support = new User();
            support.setUsername("support");
            support.setPassword("password");
            support.setEmail("support@example.com");
            support.setRole(User.UserRole.SUPPORT);
            support.setIsOnline(false);
            userRepository.save(support);
            log.info("Created test support user");
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("password");
            admin.setEmail("admin@example.com");
            admin.setRole(User.UserRole.ADMIN);
            admin.setIsOnline(false);
            userRepository.save(admin);
            log.info("Created test admin user");
        }
    }
}
