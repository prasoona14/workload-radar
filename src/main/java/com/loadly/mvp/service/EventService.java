package com.loadly.mvp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loadly.mvp.engine.WeeklyMetrics;
import com.loadly.mvp.engine.WorkloadEngine;
import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.repository.CalendarEventRepo;

@Service
public class EventService {

    @Autowired
    CalendarEventRepo calendarEventRepo;

    @Autowired
    WorkloadEngine workloadEngine;

    public CalendarEvent createEvent(CalendarEvent event) {
        return calendarEventRepo.save(event);
    }

    public void deleteEvent(int eventId) {
        calendarEventRepo.deleteById(eventId);
    }

    public List<CalendarEvent> getEventsForUser(int userId) {
        return calendarEventRepo.findByUserId(userId);
    }

    public List<CalendarEvent> getEventsForWeek(int userId, LocalDateTime weekStart, LocalDateTime weekEnd) {
        return calendarEventRepo.findByUserIdAndStartTimeBetween(userId, weekStart, weekEnd);
    }

    public WeeklyMetrics getWeeklyAnalysis(int userId, LocalDateTime weekStart, LocalDateTime weekEnd) {
        List<CalendarEvent> weeklyEvents = getEventsForWeek(userId, weekStart, weekEnd);
        return workloadEngine.computeWeeklyMetrics(weeklyEvents);
    }

}
