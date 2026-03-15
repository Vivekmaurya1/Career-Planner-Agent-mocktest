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
public class MockQuestion {
    private Integer id;
    private String type;                        // "mcq" | "writing"

    // ── MCQ fields ────────────────────────────────────────────────────────────
    private String question;                    // used by both MCQ and writing
    private List<String> options;               // MCQ only — 4 options
    private String correctAnswer;               // MCQ only — included in API response,
    // the frontend is responsible for hiding
    // it during the quiz phase and revealing
    // it only after the user submits

    // ── Writing fields ────────────────────────────────────────────────────────
    private List<String> expectedKeyPoints;     // Writing only — key concepts a correct
    // answer must mention; shown to user
    // after submission as model key points

    // ── Common fields ─────────────────────────────────────────────────────────
    private String difficulty;                  // "easy" | "medium" | "hard"
    private String topic;
    private Integer points;
}