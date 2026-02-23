package com.loadly.mvp.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMetrics {
    private double workloadScore;
    private double freeHours;
    private String busiestDay;
    private String pressureLevel;

}
