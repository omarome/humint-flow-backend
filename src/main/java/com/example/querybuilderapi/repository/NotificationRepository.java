package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications ordered by timestamp descending (newest first).
     * @return Ordered list of notifications.
     */
    List<Notification> findAllByOrderByTimestampDesc();
}
