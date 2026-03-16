package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.SavedView;
import com.example.querybuilderapi.repository.SavedViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for managing saved views.
 */
@Service
public class SavedViewService {

    private final SavedViewRepository savedViewRepository;

    // Matches the frontend DANGEROUS_CHARS logic
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[;'\"\\\\`]");
    
    // Matches the frontend SQL_PATTERNS logic
    private static final Pattern[] SQL_PATTERNS = {
        Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|EXECUTE)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(UNION\\s+(ALL\\s+)?SELECT)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(OR|AND)\\s+[\\d'\"].*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("--"),
        Pattern.compile("/\\*")
    };

    public SavedViewService(SavedViewRepository savedViewRepository) {
        this.savedViewRepository = savedViewRepository;
    }

    /**
     * Saves a new view with validation.
     */
    public SavedView saveView(String name, String queryJson) {
        validateInput(name, "View name");
        
        // Basic check to ensure the query is not empty or missing rules
        if (!StringUtils.hasText(queryJson) || queryJson.contains("\"rules\":[]")) {
            throw new IllegalArgumentException("At least one filter must be selected");
        }
        
        SavedView view = SavedView.builder()
                .name(name.trim())
                .queryJson(queryJson)
                .build();
        
        return savedViewRepository.save(view);
    }

    /**
     * Returns all saved views.
     */
    public List<SavedView> getAllSavedViews() {
        return savedViewRepository.findAll();
    }

    /**
     * Deletes a saved view by its ID.
     */
    public void deleteView(Long id) {
        if (!savedViewRepository.existsById(id)) {
            throw new IllegalArgumentException("Saved view not found with ID: " + id);
        }
        savedViewRepository.deleteById(id);
    }

    /**
     * Validation logic to match frontend's advanced filter validation system.
     */
    private void validateInput(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }

        if (value.length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot be more than 100 characters");
        }

        if (DANGEROUS_CHARS.matcher(value).find()) {
            throw new IllegalArgumentException("Special characters are not allowed in " + fieldName);
        }

        for (Pattern pattern : SQL_PATTERNS) {
            if (pattern.matcher(value).find()) {
                throw new IllegalArgumentException(fieldName + " contains disallowed patterns");
            }
        }
    }
}
