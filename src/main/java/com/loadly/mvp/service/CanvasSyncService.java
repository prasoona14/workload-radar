package com.loadly.mvp.service;

import com.loadly.mvp.model.*;
import com.loadly.mvp.repository.CalendarEventRepo;
import com.loadly.mvp.repository.UserRepo;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.Property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CanvasSyncService {

    @Autowired
    UserRepo userRepo;
    @Autowired
    CalendarEventRepo calendarEventRepo;

    public int syncCanvasEvents(int userId) {
        // 1. Fetch user from database
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getCanvasIcsUrl() == null || user.getCanvasIcsUrl().isBlank()) {
            throw new RuntimeException("User does not have a Canvas ICS URL set");
        }

        int importedCount = 0;

        try {
            URL url = new URL(user.getCanvasIcsUrl());
            InputStream inputStream = url.openStream();

            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(inputStream);

            for (Object component : calendar.getComponents("VEVENT")) {

                VEvent vEvent = (VEvent) component;

                String uid = vEvent.getProperty(Property.UID).map(Property::getValue).orElse(null);
                String title = vEvent.getProperty(Property.SUMMARY).map(Property::getValue).orElse(null);

                DtStart dtStart = vEvent.getProperty(Property.DTSTART).map(p -> (DtStart) p).orElse(null);
                DtEnd dtEnd = vEvent.getProperty(Property.DTEND).map(p -> (DtEnd) p).orElse(null);

                LocalDateTime startTime = convertToLocalDateTime(dtStart);
                LocalDateTime endTime = convertToLocalDateTime(dtEnd);

                Optional<CalendarEvent> existingEvent = calendarEventRepo.findByExternalId(uid);

                if (existingEvent.isPresent()) {

                    // Optional: update fields if needed
                    CalendarEvent event = existingEvent.get();
                    event.setTitle(title);
                    event.setStartTime(startTime);
                    event.setEndTime(endTime);
                    calendarEventRepo.save(event);

                } else {

                    CalendarEvent newEvent = new CalendarEvent();
                    newEvent.setExternalId(uid);
                    newEvent.setTitle(title);
                    newEvent.setStartTime(startTime);
                    newEvent.setEndTime(endTime);

                    // 🔥 Force everything to ASSIGNMENT
                    newEvent.setType(EventType.ASSIGNMENT);

                    // Default effort
                    newEvent.setEffortHours(3);

                    newEvent.setSource(EventSource.CANVAS);

                    newEvent.setUser(user);

                    calendarEventRepo.save(newEvent);
                    importedCount++;
                }
            }

            user.setLastSyncedAt(LocalDateTime.now());
            userRepo.save(user);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sync Canvas events", e);
        }

        return importedCount;
    }

    private LocalDateTime convertToLocalDateTime(DateProperty dateProperty) {

        if (dateProperty == null)
            return null;

        String value = dateProperty.getValue();

        try {
            // Case 1: Date-time format (with timezone)
            if (value.contains("T")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
                return LocalDateTime.parse(value, formatter);
            }

            // Case 2: Date-only format (all-day event)
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            LocalDate date = LocalDate.parse(value, dateFormatter);

            // Convert all-day events to midnight
            return date.atStartOfDay();

        } catch (Exception e) {
            throw new RuntimeException("Invalid date format in ICS: " + value, e);
        }
    }
}