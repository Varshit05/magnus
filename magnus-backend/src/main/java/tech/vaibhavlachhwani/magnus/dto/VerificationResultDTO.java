package tech.vaibhavlachhwani.magnus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResultDTO {
    private boolean authentic;
    private String status;
    private UUID documentId;
    private String message;

    // ML & Forensic Analysis Fields
    private Boolean mlAnalyzed;
    private Double tamperScore;
    private String tamperVerdict;
    private String heatmapBase64;
    private String elaImageBase64;
    private List<TamperedRegionDTO> tamperedRegions;
    private Map<String, Object> metrics;
}
