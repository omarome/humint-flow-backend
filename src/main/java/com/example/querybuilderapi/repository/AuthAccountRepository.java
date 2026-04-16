package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long>, JpaSpecificationExecutor<AuthAccount> {

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findByOauthProviderAndOauthId(
            AuthAccount.OAuthProvider oauthProvider, String oauthId);

    Optional<AuthAccount> findByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    List<AuthAccount> findAllByIsActiveTrue();

    Optional<AuthAccount> findByFcmToken(String fcmToken);

    @Modifying
    @Query("UPDATE AuthAccount a SET a.manager = null WHERE a.manager.id = :accountId")
    void nullManagerByAccountId(@Param("accountId") Long accountId);
}
