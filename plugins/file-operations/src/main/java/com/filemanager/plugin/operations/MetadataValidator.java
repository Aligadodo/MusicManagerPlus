package com.filemanager.plugin.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataValidator {
    
    public enum ValidationResult {
        VALID,
        INVALID,
        MISSING_REQUIRED,
        INVALID_FORMAT,
        WARNING
    }
    
    public static class ValidationIssue {
        private String field;
        private String message;
        private ValidationResult result;
        
        public ValidationIssue(String field, String message, ValidationResult result) {
            this.field = field;
            this.message = message;
            this.result = result;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ValidationResult getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            return "[" + result + "] " + field + ": " + message;
        }
    }
    
    public static class ValidationReport {
        private File file;
        private boolean valid;
        private List<ValidationIssue> issues;
        private Map<String, Object> metadata;
        
        public ValidationReport(File file) {
            this.file = file;
            this.valid = true;
            this.issues = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        public File getFile() {
            return file;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<ValidationIssue> getIssues() {
            return issues;
        }
        
        public void addIssue(ValidationIssue issue) {
            this.issues.add(issue);
            if (issue.getResult() == ValidationResult.INVALID || 
                issue.getResult() == ValidationResult.MISSING_REQUIRED ||
                issue.getResult() == ValidationResult.INVALID_FORMAT) {
                this.valid = false;
            }
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        public boolean hasErrors() {
            return issues.stream()
                .anyMatch(issue -> issue.getResult() == ValidationResult.INVALID || 
                                   issue.getResult() == ValidationResult.MISSING_REQUIRED ||
                                   issue.getResult() == ValidationResult.INVALID_FORMAT);
        }
        
        public boolean hasWarnings() {
            return issues.stream()
                .anyMatch(issue -> issue.getResult() == ValidationResult.WARNING);
        }
        
        public String getSummary() {
            return "File: " + file.getName() + 
                   ", Valid: " + valid + 
                   ", Issues: " + issues.size();
        }
    }
    
    private boolean validateRequiredFields;
    private boolean validateFormat;
    private boolean validateConsistency;
    private boolean validateEncoding;
    
    public MetadataValidator() {
        this.validateRequiredFields = true;
        this.validateFormat = true;
        this.validateConsistency = true;
        this.validateEncoding = false;
    }
    
    public MetadataValidator(boolean validateRequiredFields, boolean validateFormat, 
                            boolean validateConsistency, boolean validateEncoding) {
        this.validateRequiredFields = validateRequiredFields;
        this.validateFormat = validateFormat;
        this.validateConsistency = validateConsistency;
        this.validateEncoding = validateEncoding;
    }
    
    public ValidationReport validate(File file, Map<String, Object> metadata) {
        ValidationReport report = new ValidationReport(file);
        report.setMetadata(metadata);
        
        if (validateRequiredFields) {
            validateRequiredFields(report, metadata);
        }
        
        if (validateFormat) {
            validateFormat(report, metadata);
        }
        
        if (validateConsistency) {
            validateConsistency(report, metadata);
        }
        
        if (validateEncoding) {
            validateEncoding(report, metadata);
        }
        
        return report;
    }
    
    public ValidationReport validate(String filePath, Map<String, Object> metadata) {
        return validate(new File(filePath), metadata);
    }
    
    private void validateRequiredFields(ValidationReport report, Map<String, Object> metadata) {
        String[] requiredFields = {"title", "artist", "album"};
        
        for (String field : requiredFields) {
            Object value = metadata.get(field);
            
            if (value == null || value.toString().trim().isEmpty()) {
                report.addIssue(new ValidationIssue(
                    field, 
                    "Required field is missing or empty", 
                    ValidationResult.MISSING_REQUIRED
                ));
            }
        }
    }
    
    private void validateFormat(ValidationReport report, Map<String, Object> metadata) {
        String[] numericFields = {"track_number", "disc_number", "year"};
        
        for (String field : numericFields) {
            Object value = metadata.get(field);
            
            if (value != null && !value.toString().trim().isEmpty()) {
                try {
                    Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    report.addIssue(new ValidationIssue(
                        field, 
                        "Invalid numeric format: " + value, 
                        ValidationResult.INVALID_FORMAT
                    ));
                }
            }
        }
        
        Object duration = metadata.get("duration");
        if (duration != null && !duration.toString().trim().isEmpty()) {
            try {
                double dur = Double.parseDouble(duration.toString());
                if (dur <= 0) {
                    report.addIssue(new ValidationIssue(
                        "duration", 
                        "Duration must be positive: " + duration, 
                        ValidationResult.INVALID_FORMAT
                    ));
                }
            } catch (NumberFormatException e) {
                report.addIssue(new ValidationIssue(
                    "duration", 
                    "Invalid duration format: " + duration, 
                    ValidationResult.INVALID_FORMAT
                ));
            }
        }
    }
    
    private void validateConsistency(ValidationReport report, Map<String, Object> metadata) {
        Object trackNumber = metadata.get("track_number");
        Object totalTracks = metadata.get("total_tracks");
        
        if (trackNumber != null && totalTracks != null) {
            try {
                int track = Integer.parseInt(trackNumber.toString());
                int total = Integer.parseInt(totalTracks.toString());
                
                if (track > total) {
                    report.addIssue(new ValidationIssue(
                        "track_number", 
                        "Track number (" + track + ") exceeds total tracks (" + total + ")", 
                        ValidationResult.WARNING
                    ));
                }
            } catch (NumberFormatException e) {
            }
        }
        
        Object discNumber = metadata.get("disc_number");
        Object totalDiscs = metadata.get("total_discs");
        
        if (discNumber != null && totalDiscs != null) {
            try {
                int disc = Integer.parseInt(discNumber.toString());
                int total = Integer.parseInt(totalDiscs.toString());
                
                if (disc > total) {
                    report.addIssue(new ValidationIssue(
                        "disc_number", 
                        "Disc number (" + disc + ") exceeds total discs (" + total + ")", 
                        ValidationResult.WARNING
                    ));
                }
            } catch (NumberFormatException e) {
            }
        }
        
        Object year = metadata.get("year");
        if (year != null && !year.toString().trim().isEmpty()) {
            try {
                int yr = Integer.parseInt(year.toString());
                if (yr < 1900 || yr > 2100) {
                    report.addIssue(new ValidationIssue(
                        "year", 
                        "Year (" + yr + ") is outside reasonable range (1900-2100)", 
                        ValidationResult.WARNING
                    ));
                }
            } catch (NumberFormatException e) {
            }
        }
    }
    
    private void validateEncoding(ValidationReport report, Map<String, Object> metadata) {
        String[] textFields = {"title", "artist", "album", "genre", "comment"};
        
        for (String field : textFields) {
            Object value = metadata.get(field);
            
            if (value != null && !value.toString().trim().isEmpty()) {
                String text = value.toString();
                
                if (!isValidUTF8(text)) {
                    report.addIssue(new ValidationIssue(
                        field, 
                        "Invalid UTF-8 encoding detected", 
                        ValidationResult.WARNING
                    ));
                }
            }
        }
    }
    
    private boolean isValidUTF8(String text) {
        try {
            byte[] bytes = text.getBytes("UTF-8");
            String reconstructed = new String(bytes, "UTF-8");
            return text.equals(reconstructed);
        } catch (Exception e) {
            return false;
        }
    }
    
    public List<ValidationReport> validateBatch(List<File> files, List<Map<String, Object>> metadataList) {
        List<ValidationReport> reports = new ArrayList<>();
        
        for (int i = 0; i < files.size() && i < metadataList.size(); i++) {
            ValidationReport report = validate(files.get(i), metadataList.get(i));
            reports.add(report);
        }
        
        return reports;
    }
    
    public boolean isValidateRequiredFields() {
        return validateRequiredFields;
    }
    
    public void setValidateRequiredFields(boolean validateRequiredFields) {
        this.validateRequiredFields = validateRequiredFields;
    }
    
    public boolean isValidateFormat() {
        return validateFormat;
    }
    
    public void setValidateFormat(boolean validateFormat) {
        this.validateFormat = validateFormat;
    }
    
    public boolean isValidateConsistency() {
        return validateConsistency;
    }
    
    public void setValidateConsistency(boolean validateConsistency) {
        this.validateConsistency = validateConsistency;
    }
    
    public boolean isValidateEncoding() {
        return validateEncoding;
    }
    
    public void setValidateEncoding(boolean validateEncoding) {
        this.validateEncoding = validateEncoding;
    }
}
