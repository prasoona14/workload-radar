package com.loadly.mvp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loadly.mvp.engine.WeeklyMetrics;
import com.loadly.mvp.engine.WorkloadEngine;
import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.model.User;
import com.loadly.mvp.repository.CalendarEventRepo;

@Service
public class EventService {

    @Autowired
    CalendarEventRepo calendarEventRepo;

    @Autowired
    WorkloadEngine workloadEngine;

    public CalendarEvent createEvent(CalendarEvent event, User user) {
        event.setUser(user);
        return calendarEventRepo.save(event);
    }

    public void deleteEvent(int eventId) {
        calendarEventRepo.deleteById(eventId);
    }

    public List<CalendarEvent> getEventsForUser(User user) {
        return calendarEventRepo.findByUser(user);
    }

    public List<CalendarEvent> getEventsForWeek(User user, LocalDateTime weekStart, LocalDateTime weekEnd) {
        return calendarEventRepo.findByUserAndStartTimeBetween(user, weekStart, weekEnd);
    }

    public WeeklyMetrics getWeeklyAnalysis(User user, LocalDateTime weekStart, LocalDateTime weekEnd) {
        List<CalendarEvent> weeklyEvents = getEventsForWeek(user, weekStart, weekEnd);
        return workloadEngine.computeWeeklyMetrics(weeklyEvents);
    }

}
