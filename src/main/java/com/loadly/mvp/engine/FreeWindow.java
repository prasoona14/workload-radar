package com.loadly.mvp.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FreeWindow {
    private String day; // MONDAY
    private String startTime; // 09:00
    private String endTime; // 11:00
}