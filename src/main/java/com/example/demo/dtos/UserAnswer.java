package com.example.demo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    private Integer id;         // matches question id
    private String type;        // "mcq" | "coding"
    private String answer;      // for MCQ: selected option | for coding: actual code written
}