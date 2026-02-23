package com.loadly.mvp.ai;

import lombok.Data;

@Data
public class AIWeeklyPlanRequest {
    private int userId;
    private String weekStart; // ISO string: "2026-02-23T00:00:00"
    private String weekEnd; // ISO string: "2026-03-01T23:59:59"
}
