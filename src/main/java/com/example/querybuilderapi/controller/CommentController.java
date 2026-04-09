package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Comment;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.CommentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Threaded comment API for any CRM entity.
 *
 * GET    /api/comments/{entityType}/{entityId}        — list top-level comments (with reply lists)
 * POST   /api/comments/{entityType}/{entityId}        — add a comment (or reply if parentId given)
 * PATCH  /api/comments/{commentId}                   — edit own comment body
 * DELETE /api/comments/{commentId}                   — soft-delete own comment (admins can delete any)
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final AuthAccountRepository authAccountRepository;

    public CommentController(CommentRepository commentRepository,
                             AuthAccountRepository authAccountRepository) {
        this.commentRepository    = commentRepository;
        this.authAccountRepository = authAccountRepository;
    }

    public record CommentRequest(
            @NotBlank @Size(max = 10_000) String body,
            UUID parentId   // null = top-level
    ) {}

    // ── Read ──────────────────────────────────────────────────────────────

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> list(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {

        List<Comment> topLevel = commentRepository
                .findByEntityTypeAndEntityIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
                        entityType.toUpperCase(), entityId);

        List<Map<String, Object>> result = topLevel.stream().map(c -> {
            List<Comment> replies = commentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(c.getId());
            return toMap(c, replies.stream().map(r -> toMap(r, List.of())).toList());
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @PostMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @Valid @RequestBody CommentRequest req,
            Authentication auth) {

        AuthAccount author = resolveAuthor(auth.getName());

        Comment comment = new Comment();
        comment.setEntityType(entityType.toUpperCase());
        comment.setEntityId(entityId);
        comment.setAuthor(author);
        comment.setBody(req.body());
        comment.setParentId(req.parentId());

        Comment saved = commentRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved, List.of()));
    }

    @PatchMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> edit(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest req,
            Authentication auth) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        AuthAccount caller = resolveAuthor(auth.getName());
        if (!comment.getAuthor().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only edit your own comments");
        }

        comment.setBody(req.body());
        return ResponseEntity.ok(toMap(commentRepository.save(comment), List.of()));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            Authentication auth) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        AuthAccount caller = resolveAuthor(auth.getName());
        boolean isAdmin = caller.getRole() == AuthAccount.Role.ADMIN;

        if (!isAdmin && !comment.getAuthor().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete your own comments");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private AuthAccount resolveAuthor(String firebaseUid) {
        return authAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private Map<String, Object> toMap(Comment c, List<Map<String, Object>> replies) {
        return Map.of(
                "id",          c.getId(),
                "body",        c.getBody(),
                "parentId",    c.getParentId() != null ? c.getParentId() : "",
                "authorId",    c.getAuthor().getId(),
                "authorName",  c.getAuthor().getDisplayName(),
                "authorAvatar",c.getAuthor().getAvatarUrl() != null ? c.getAuthor().getAvatarUrl() : "",
                "createdAt",   c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
                "replies",     replies
        );
    }
}
