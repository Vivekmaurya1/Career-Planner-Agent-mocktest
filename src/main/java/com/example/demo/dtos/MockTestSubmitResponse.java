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
public class MockTestSubmitResponse {
    private String role;
    private String experience;
    private String statedLevel;
    private String detectedLevel;           // BEGINNER | INTERMEDIATE | ADVANCED
    private String overallScore;            // e.g. "78/100"
    private String mcqScore;               // e.g. "42/60"
    private String writingScore;            // e.g. "28/40"  (was codingScore)
    private String technicalDepthScore;    // e.g. "7/10"
    private String problemSolvingScore;    // e.g. "8/10"
    private List<String> strengths;
    private List<String> weakAreas;
    private List<String> mustLearnNext;
    private List<String> recommendedTopics;
    private String interviewReadiness;     // "Not Ready" | "Partially Ready" | "Ready"
    private String confidenceLevel;        // "Low" | "Medium" | "High"
    private String finalVerdict;
    private String roadmapId;              // populated if roadmap was pre-generated
}