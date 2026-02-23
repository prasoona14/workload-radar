package com.loadly.mvp.engine;

import java.time.Duration;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.model.EventType;

@Component
public class WorkloadEngine {

    @Autowired
    private FreeTimeCalculator freeTimeCalculator;

    public WeeklyMetrics computeWeeklyMetrics(List<CalendarEvent> events) {

        double totalEffort = 0;
        double totalBusyHours = 0;

        Map<DayOfWeek, Double> dayLoadMap = new HashMap<>();

        for (CalendarEvent event : events) {

            double duration = Duration.between(
                    event.getStartTime(),
                    event.getEndTime()).toHours();

            totalBusyHours += duration;

            // Track busiest day
            DayOfWeek day = event.getStartTime().getDayOfWeek();
            dayLoadMap.put(day,
                    dayLoadMap.getOrDefault(day, 0.0) + duration);

            // Count assignment effort
            if (event.getType() == EventType.ASSIGNMENT) {
                totalEffort += event.getEffortHours();
            }
        }

        // Simple workload formula
        double workloadScore = totalBusyHours + totalEffort;

        // Find busiest day
        String busiestDay = dayLoadMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().name())
                .orElse("NONE");

        // Calculate free hours
        double freeHours = freeTimeCalculator.calculateFreeHours(events);

        // Pressure classification
        String pressureLevel;

        if (workloadScore < 20) {
            pressureLevel = "LOW";
        } else if (workloadScore < 40) {
            pressureLevel = "MEDIUM";
        } else {
            pressureLevel = "HIGH";
        }

        return new WeeklyMetrics(workloadScore, freeHours, busiestDay, pressureLevel);
    }
}
