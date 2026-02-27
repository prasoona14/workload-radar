package com.loadly.mvp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.model.User;

@Repository
public interface CalendarEventRepo extends JpaRepository<CalendarEvent, Integer> {

    List<CalendarEvent> findByUserAndStartTimeBetween(User user, LocalDateTime weekStart, LocalDateTime weekEnd);

    List<CalendarEvent> findByUser(User user);

    Optional<CalendarEvent> findByExternalId(String externalId);

}
