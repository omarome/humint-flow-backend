package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user data.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users — returns all users, optionally sorted.
     *
     * @param sortBy  field name to sort by (e.g. "age", "status", "fullName").
     *                "fullName" is mapped to "firstName" because fullName is computed.
     * @param sortDir "asc" or "desc" (default "asc")
     */
    @GetMapping
    public List<User> getAllUsers(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        if (sortBy == null || sortBy.isBlank()) {
            return userService.getAllUsers();
        }

        // fullName is a derived (non-persisted) field — sort by firstName instead
        String column = "fullName".equals(sortBy) ? "firstName" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return userService.getAllUsers(Sort.by(direction, column));
    }

    /**
     * GET /api/users/{id} — returns a single user by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}
