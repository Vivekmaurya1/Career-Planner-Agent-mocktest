package com.example.demo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMockTestRequest {
    private String testId;
    private String role;
    private String experience;
    private String statedLevel;
    private String goal;
    private List<MockQuestion> questions;   // original questions sent back for evaluation context
    private List<UserAnswer> answers;       // user's submitted answers / code
}