package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User service — sources data from auth_accounts so the Employee Directory
 * shows the same real users as Team Management.
 */
@Service
public class UserService {

    private final AuthAccountRepository authAccountRepository;

    public UserService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    public List<User> getAllUsers() {
        return authAccountRepository.findAll()
                .stream()
                .map(this::toUser)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers(Sort sort) {
        // Sort is applied in-memory because User is a derived DTO, not a JPA entity
        List<User> users = getAllUsers();
        if (sort == null || sort.isUnsorted()) return users;

        Sort.Order order = sort.iterator().next();
        Comparator<User> comparator = switch (order.getProperty()) {
            case "status"     -> Comparator.comparing(u -> u.getStatus() != null ? u.getStatus() : "");
            case "department" -> Comparator.comparing(u -> u.getDepartment() != null ? u.getDepartment() : "");
            case "position"   -> Comparator.comparing(u -> u.getPosition() != null ? u.getPosition() : "");
            default           -> Comparator.comparing(u -> u.getFullName() != null ? u.getFullName() : "");
        };
        if (order.isDescending()) comparator = comparator.reversed();
        users.sort(comparator);
        return users;
    }

    public User getUserById(Long id) {
        return authAccountRepository.findById(id).map(this::toUser).orElse(null);
    }

    private User toUser(AuthAccount account) {
        // Split displayName into first/last for the fullName getter in User
        String display = account.getDisplayName() != null ? account.getDisplayName().trim() : "";
        int space = display.lastIndexOf(' ');
        String firstName = space > 0 ? display.substring(0, space) : display;
        String lastName  = space > 0 ? display.substring(space + 1) : "";

        String status = Boolean.TRUE.equals(account.getIsActive()) ? "Active" : "Inactive";

        return new User(
                account.getId(),
                firstName,
                lastName,
                account.getEmail(),
                status,
                null,              // isOnline — not tracked in auth_accounts
                account.getJobTitle(),
                account.getDepartment()
        );
    }
}
