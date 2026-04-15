package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.model.Organization;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * GET /api/audit/{entityType}/{id}
 *
 * Returns the full revision history for a CRM entity as stored by Hibernate Envers.
 * Supports: CONTACT, ORGANIZATION, OPPORTUNITY.
 *
 * Response: list of revision objects, each containing:
 *   - revisionNumber  (int)
 *   - revisionDate    (ISO-8601 timestamp)
 *   - revisionType    (ADD | MOD | DEL)
 *   - entity          (snapshot of the entity at that revision)
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final EntityManager entityManager;

    public AuditLogController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/{entityType}/{id}")
    @PreAuthorize("@perms.can('AUDIT_READ')")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @PathVariable String entityType,
            @PathVariable UUID id) {

        Class<?> clazz = resolveClass(entityType);
        if (clazz == null) {
            return ResponseEntity.badRequest().build();
        }

        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> revisions = reader.createQuery()
                .forRevisionsOfEntity(clazz, false, true)
                .add(AuditEntity.id().eq(id))
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : revisions) {
            Object entity       = row[0];
            Object revisionInfo = row[1];
            Object revisionType = row[2];

            // Extract revision metadata via reflection to stay loosely coupled
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("revisionType", revisionType != null ? revisionType.toString() : null);

            try {
                var revClass = revisionInfo.getClass();
                entry.put("revisionNumber", revClass.getMethod("getId").invoke(revisionInfo));
                Object timestamp = revClass.getMethod("getRevisionDate").invoke(revisionInfo);
                if (timestamp instanceof Date d) {
                    entry.put("revisionDate", d.toInstant().toString());
                }
            } catch (Exception e) {
                entry.put("revisionNumber", null);
                entry.put("revisionDate", null);
            }

            entry.put("entity", entity);
            result.add(entry);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Returns revision numbers for a given entity (lightweight — just IDs + dates).
     * Useful for building a compact timeline before fetching full snapshots.
     */
    @GetMapping("/{entityType}/{id}/summary")
    @PreAuthorize("@perms.can('AUDIT_READ')")
    public ResponseEntity<List<Map<String, Object>>> getRevisionSummary(
            @PathVariable String entityType,
            @PathVariable UUID id) {

        Class<?> clazz = resolveClass(entityType);
        if (clazz == null) return ResponseEntity.badRequest().build();

        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Number> revNums = reader.getRevisions(clazz, id);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Number rev : revNums) {
            Date revDate = reader.getRevisionDate(rev);
            result.add(Map.of(
                    "revisionNumber", rev,
                    "revisionDate",   revDate != null ? revDate.toInstant().toString() : null
            ));
        }
        return ResponseEntity.ok(result);
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private Class<?> resolveClass(String entityType) {
        return switch (entityType.toUpperCase()) {
            case "CONTACT"      -> Contact.class;
            case "ORGANIZATION" -> Organization.class;
            case "OPPORTUNITY"  -> Opportunity.class;
            default -> null;
        };
    }
}
