package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.model.RecordShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordShareRepository extends JpaRepository<RecordShare, Long> {

    @Query("SELECT rs FROM RecordShare rs JOIN FETCH rs.sharedWith WHERE rs.workspace.id = :workspaceId AND rs.resourceType = :resourceType AND rs.resourceId = :resourceId")
    List<RecordShare> findByResource(@Param("workspaceId") Long workspaceId, 
                                     @Param("resourceType") EntityType resourceType, 
                                     @Param("resourceId") UUID resourceId);

    Optional<RecordShare> findByWorkspaceIdAndResourceTypeAndResourceIdAndSharedWithId(
            Long workspaceId, EntityType resourceType, UUID resourceId, Long sharedWithAccountId);

    void deleteByWorkspaceIdAndResourceTypeAndResourceIdAndSharedWithId(
            Long workspaceId, EntityType resourceType, UUID resourceId, Long sharedWithAccountId);
}
