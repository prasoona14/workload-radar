package com.loadly.mvp.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loadly.mvp.engine.WeeklyMetrics;
import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.model.User;
import com.loadly.mvp.service.EventService;
import com.loadly.mvp.service.UserService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    EventService eventService;

    @Autowired
    private UserService userService;

    // Create Event
    @PostMapping
    public CalendarEvent createEvent(@RequestBody CalendarEvent event, @RequestParam int userId) {
        User user = userService.getUserById(userId);
        return eventService.createEvent(event, user);
    }

    // DeleteEvent
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable int id) {
        eventService.deleteEvent(id);
    }

    // Get All Events for User
    @GetMapping("/user/{userId}")
    public List<CalendarEvent> getEventsForUser(@PathVariable int userId) {
        User user = userService.getUserById(userId);
        return eventService.getEventsForUser(user);
    }

    // Get Events for Week
    @GetMapping("/week")
    public List<CalendarEvent> getEventsForWeek(
            @RequestParam int userId,
            @RequestParam String weekStart,
            @RequestParam String weekEnd) {
        User user = userService.getUserById(userId);
        return eventService.getEventsForWeek(user, LocalDateTime.parse(weekStart), LocalDateTime.parse(weekEnd));
    }

    @GetMapping("/user/{userId}/analysis/week")
    public WeeklyMetrics getWeeklyAnalysis(
            @PathVariable int userId,
            @RequestParam String weekStart,
            @RequestParam String weekEnd) {

        User user = userService.getUserById(userId);
        return eventService.getWeeklyAnalysis(user, LocalDateTime.parse(weekStart), LocalDateTime.parse(weekEnd));
    }

}
