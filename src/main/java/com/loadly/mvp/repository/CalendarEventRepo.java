package com.loadly.mvp.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loadly.mvp.model.CalendarEvent;

@Repository
public interface CalendarEventRepo extends JpaRepository<CalendarEvent, Integer> {

    List<CalendarEvent> findByUserIdAndStartTimeBetween(int userId, LocalDateTime weekStart, LocalDateTime weekEnd);

    List<CalendarEvent> findByUserId(int userId);

}
