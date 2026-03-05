package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "John", "Doe", 28,
                "john.doe@example.com", "Active", "Johnny", true);
    }

    @Test
    @DisplayName("getAllUsers returns list of users")
    void getAllUsers_returnsUsers() {
        User user2 = new User(2L, "Jane", "Smith", 32,
                "jane.smith@example.com", "Active", null, false);
        when(userRepository.findAll()).thenReturn(List.of(sampleUser, user2));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllUsers returns empty list when no users exist")
    void getAllUsers_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserById returns user when found")
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserById returns null when not found")
    void getUserById_notFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        User result = userService.getUserById(999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("seedUsers seeds data when database is empty")
    void seedUsers_seedsWhenEmpty() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        userService.seedUsers().run();

        verify(userRepository, times(1)).saveAll(argThat(list ->
                ((List<?>) list).size() == 10
        ));
    }

    @Test
    @DisplayName("seedUsers does not seed when database already has data")
    void seedUsers_skipsWhenNotEmpty() throws Exception {
        when(userRepository.count()).thenReturn(10L);

        userService.seedUsers().run();

        verify(userRepository, never()).saveAll(anyList());
    }
}
