package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.UserRepository;
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
}
