package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.SavedView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SavedView entity.
 */
@Repository
public interface SavedViewRepository extends JpaRepository<SavedView, Long> {
}
