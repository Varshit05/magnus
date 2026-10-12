package tech.vaibhavlachhwani.magnus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TamperedRegionDTO {
    private int x;
    private int y;
    private int width;
    private int height;
    private double confidence;
    private String label;
}
