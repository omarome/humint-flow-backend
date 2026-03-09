package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findByOauthProviderAndOauthId(
            AuthAccount.OAuthProvider oauthProvider, String oauthId);

    boolean existsByEmail(String email);
}
