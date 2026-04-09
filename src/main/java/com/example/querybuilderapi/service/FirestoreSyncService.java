package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.Opportunity;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors a lightweight subset of Opportunity data to Cloud Firestore
 * so the Kanban board can subscribe to real-time updates via onSnapshot.
 *
 * Only the fields the Kanban board needs are synced — the full record
 * stays in PostgreSQL (source of truth).
 *
 * Sync direction: PostgreSQL → Firestore (this service).
 * The reverse direction (Firestore → PostgreSQL) is handled by a
 * Cloud Function (P5-T6) that calls the Spring Boot API on drag-drop.
 */
@Service
public class FirestoreSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirestoreSyncService.class);
    private static final String COLLECTION = "opportunities";

    /**
     * Upserts the Kanban-relevant fields for an opportunity in Firestore.
     * Called after every PostgreSQL create/update.
     */
    public void syncOpportunity(Opportunity opp) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            Map<String, Object> data = new HashMap<>();
            data.put("id", opp.getId().toString());
            data.put("name", opp.getName());
            data.put("stage", opp.getStage());
            data.put("amount", opp.getAmount());
            data.put("ownerId", opp.getAssignedTo() != null ? opp.getAssignedTo().getId() : null);
            data.put("ownerName", opp.getAssignedTo() != null ? opp.getAssignedTo().getEmail() : null);
            data.put("updatedAt", opp.getUpdatedAt() != null ? opp.getUpdatedAt().toString() : null);

            db.collection(COLLECTION)
              .document(opp.getId().toString())
              .set(data);

            log.debug("Firestore synced opportunity: {}", opp.getId());
        } catch (Exception e) {
            // Firestore sync failure must never break the main PostgreSQL transaction
            log.error("Failed to sync opportunity {} to Firestore", opp.getId(), e);
        }
    }

    /**
     * Removes an opportunity document from Firestore on soft-delete.
     */
    public void deleteOpportunity(String opportunityId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection(COLLECTION).document(opportunityId).delete();
            log.debug("Firestore deleted opportunity: {}", opportunityId);
        } catch (Exception e) {
            log.error("Failed to delete opportunity {} from Firestore", opportunityId, e);
        }
    }
}
