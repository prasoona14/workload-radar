package com.loadly.mvp.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyBlock {
    private String day; // "MONDAY"
    private String startTime; // "18:00"
    private double durationHours; // 1.5
    private String description; // "HW2 - finish Q1-Q3"
}
