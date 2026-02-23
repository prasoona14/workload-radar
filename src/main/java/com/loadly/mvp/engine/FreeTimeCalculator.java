package com.loadly.mvp.engine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loadly.mvp.model.CalendarEvent;

@Component
public class FreeTimeCalculator {

    private static final int HOURS_PER_DAY = 13; // 9AM–10PM
    private static final int DAYS_PER_WEEK = 7;

    private static final LocalTime DAY_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DAY_END_TIME = LocalTime.of(22, 0);

    // ----------------------------------------------------
    // 1️⃣ Calculate total free hours (improved precision)
    // ----------------------------------------------------
    public double calculateFreeHours(List<CalendarEvent> events) {

        double totalAvailable = HOURS_PER_DAY * DAYS_PER_WEEK;

        double busyHours = events.stream()
                .mapToDouble(e -> Duration.between(
                        e.getStartTime(),
                        e.getEndTime()).toMinutes() / 60.0)
                .sum();

        return Math.max(0, totalAvailable - busyHours);
    }

    // ----------------------------------------------------
    // 2️⃣ Calculate free windows for entire week
    // ----------------------------------------------------
    public List<FreeWindow> calculateFreeWindows(
            List<CalendarEvent> events,
            LocalDateTime weekStart) {

        List<FreeWindow> freeWindows = new ArrayList<>();

        for (int i = 0; i < DAYS_PER_WEEK; i++) {

            LocalDateTime currentDay = weekStart.plusDays(i);

            LocalDateTime dayStart = currentDay
                    .with(DAY_START_TIME);

            LocalDateTime dayEnd = currentDay
                    .with(DAY_END_TIME);

            DayOfWeek dayOfWeek = currentDay.getDayOfWeek();

            // Filter events for this specific day
            List<CalendarEvent> dayEvents = events.stream()
                    .filter(e -> e.getStartTime().toLocalDate()
                            .equals(currentDay.toLocalDate()))
                    .sorted(Comparator.comparing(CalendarEvent::getStartTime))
                    .toList();

            LocalDateTime pointer = dayStart;

            for (CalendarEvent event : dayEvents) {

                LocalDateTime eventStart = event.getStartTime();
                LocalDateTime eventEnd = event.getEndTime();

                // Clamp event to awake window
                if (eventEnd.isBefore(dayStart) || eventStart.isAfter(dayEnd)) {
                    continue;
                }

                if (eventStart.isBefore(dayStart)) {
                    eventStart = dayStart;
                }

                if (eventEnd.isAfter(dayEnd)) {
                    eventEnd = dayEnd;
                }

                // If gap exists → free window
                if (eventStart.isAfter(pointer)) {

                    freeWindows.add(new FreeWindow(
                            dayOfWeek.name(),
                            pointer.toLocalTime().toString(),
                            eventStart.toLocalTime().toString()));
                }

                // Move pointer forward
                if (eventEnd.isAfter(pointer)) {
                    pointer = eventEnd;
                }
            }

            // After last event → remaining free time
            if (pointer.isBefore(dayEnd)) {
                freeWindows.add(new FreeWindow(
                        dayOfWeek.name(),
                        pointer.toLocalTime().toString(),
                        dayEnd.toLocalTime().toString()));
            }
        }

        return freeWindows;
    }
}