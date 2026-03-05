package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User service backed by a PostgreSQL database via Spring Data JPA.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns all users.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Returns a user by id, or null if not found.
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Seeds the database with sample data if empty.
     */
    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.saveAll(List.of(
                    new User(null, "John", "Doe", 28, "john.doe@example.com", "Active", "Johnny", true),
                    new User(null, "Jane", "Smith", 32, "jane.smith@example.com", "Active", null, false),
                    new User(null, "Bob", "Johnson", 45, "bob.johnson@example.com", "Inactive", "Bobby", false),
                    new User(null, "Alice", "Williams", 29, "alice.williams@example.com", "Active", null, true),
                    new User(null, "Charlie", "Brown", 35, "charlie.brown@example.com", "Pending", "Chuck", true),
                    new User(null, "Diana", "Davis", 27, "diana.davis@example.com", "Active", null, false),
                    new User(null, "Edward", "Miller", 41, "edward.miller@example.com", "Inactive", "Ed", false),
                    new User(null, "Fiona", "Wilson", 33, "fiona.wilson@example.com", "Active", null, true),
                    new User(null, "George", "Moore", 38, "george.moore@example.com", "Pending", "Geo", false),
                    new User(null, "Helen", "Taylor", 26, "helen.taylor@example.com", "Active", null, true)
                ));
                System.out.println("✅ Seeded 10 users into the database.");
            }
        };
    }
}
