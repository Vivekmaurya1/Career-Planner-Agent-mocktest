package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockTestResponse {
    private String testId;
    private Integer timeMinutes;        // total time allowed
    private Integer totalPoints;
    private AssessmentInfo assessmentInfo;
    private List<MockQuestion> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssessmentInfo {
        private String role;
        private String experience;
        private String level;
        private String goal;
    }
}