package com.filemanager.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThemeDTO {
    private String id;
    private String name;
    private String description;
    private String type; // default | custom
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> config;
}