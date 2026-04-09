package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("save and findById returns the saved user")
    void saveAndFindById() {
        User user = new User(null, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("findAll returns all saved users")
    void findAll() {
        userRepository.save(new User(null, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales"));
        userRepository.save(new User(null, "Jane", "Smith",
                "jane.smith@example.com", "Active", false, "Manager", "Sales"));

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("findById returns empty when user does not exist")
    void findById_notFound() {
        Optional<User> found = userRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("deleteById removes the user")
    void deleteById() {
        User saved = userRepository.save(new User(null, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales"));

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("count returns correct number of users")
    void count() {
        assertThat(userRepository.count()).isZero();

        userRepository.save(new User(null, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales"));
        userRepository.save(new User(null, "Jane", "Smith",
                "jane.smith@example.com", "Active", false, "Manager", "Sales"));

        assertThat(userRepository.count()).isEqualTo(2);
    }
}
