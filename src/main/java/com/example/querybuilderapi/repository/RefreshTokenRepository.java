package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByAuthAccount(AuthAccount authAccount);
}
