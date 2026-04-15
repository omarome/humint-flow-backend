package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.RoleAudit;
import com.example.querybuilderapi.repository.RoleAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/audit/roles")
public class RoleAuditController {

    private final RoleAuditRepository roleAuditRepository;

    public RoleAuditController(RoleAuditRepository roleAuditRepository) {
        this.roleAuditRepository = roleAuditRepository;
    }

    @GetMapping
    @PreAuthorize("@perms.can('AUDIT_READ')")
    public ResponseEntity<List<Map<String, Object>>> getRoleAuditLogs(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RoleAudit> audits = roleAuditRepository.findByWorkspaceIdWithAccounts(workspaceId, pageable);

        List<Map<String, Object>> result = audits.stream().map(audit -> Map.<String, Object>of(
                "id", audit.getId(),
                "actorId", audit.getActor() != null ? audit.getActor().getId() : null,
                "actorName", audit.getActor() != null ? audit.getActor().getDisplayName() : "System",
                "targetAccountId", audit.getTargetAccount().getId(),
                "targetAccountName", audit.getTargetAccount().getDisplayName(),
                "targetAccountEmail", audit.getTargetAccount().getEmail(),
                "oldRole", audit.getOldRole() != null ? audit.getOldRole().name() : null,
                "newRole", audit.getNewRole() != null ? audit.getNewRole().name() : null,
                "reason", audit.getReason() != null ? audit.getReason() : "",
                "createdAt", audit.getCreatedAt().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
