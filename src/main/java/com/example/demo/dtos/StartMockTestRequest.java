package com.example.demo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartMockTestRequest {
    private String role;
    private String experienceLevel;       // ← matches frontend field name exactly
    private String currentLevel;          // "Beginner" | "Intermediate" | "Advanced"
    private String goal;
    private String assessmentType;        // "quiz" | "writing" | "mixed"
    private String difficultyPreference;  // "easy" | "medium" | "hard"
    private String focusTopics;
}