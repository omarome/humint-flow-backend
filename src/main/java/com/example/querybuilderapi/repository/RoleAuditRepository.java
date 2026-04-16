package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.RoleAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAuditRepository extends JpaRepository<RoleAudit, Long> {

    // LEFT JOIN on actor: system-generated role changes (e.g. DataInitializer seed) have a null actor.
    // An inner JOIN FETCH would silently drop those rows from results.
    @Query("SELECT ra FROM RoleAudit ra " +
           "LEFT JOIN FETCH ra.actor a " +
           "JOIN FETCH ra.targetAccount target " +
           "WHERE ra.workspace.id = :workspaceId")
    Page<RoleAudit> findByWorkspaceIdWithAccounts(@Param("workspaceId") Long workspaceId, Pageable pageable);

    void deleteByTargetAccountId(Long accountId);

    @Modifying
    @Query("UPDATE RoleAudit r SET r.actor = null WHERE r.actor.id = :accountId")
    void nullActorByAccountId(@Param("accountId") Long accountId);
}
