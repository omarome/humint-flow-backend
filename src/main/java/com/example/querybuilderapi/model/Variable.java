package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Variable entity describing the metadata of each user field.
 * Mapped to the "variables" table in PostgreSQL.
 */
@Entity
@Table(name = "variables")
public class Variable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "Label is required")
    private String label;

    @Column(name = "field_offset", nullable = false)
    @Min(value = 0, message = "Offset must be non-negative")
    private Integer offset;

    @Column(nullable = false)
    @NotBlank(message = "Type is required")
    private String type;

    public Variable() {
    }

    public Variable(Long id, String name, String label, Integer offset, String type) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.offset = offset;
        this.type = type;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
