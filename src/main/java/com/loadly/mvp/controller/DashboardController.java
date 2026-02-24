package com.loadly.mvp.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.loadly.mvp.ai.AIPlannerService;
import com.loadly.mvp.model.User;
import com.loadly.mvp.repository.UserRepo;
import com.loadly.mvp.service.EventService;

@Controller
public class DashboardController {

        private final EventService eventService;
        private final AIPlannerService aiPlannerService;
        private final UserRepo userRepo;

        public DashboardController(EventService eventService,
                        AIPlannerService aiPlannerService, UserRepo userRepo) {
                this.eventService = eventService;
                this.aiPlannerService = aiPlannerService;
                this.userRepo = userRepo;
        }

        @GetMapping("/")
        public String dashboard(
                        @RequestParam(required = false) String date,
                        Model model) {

                User user = userRepo.findById(1)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                LocalDate selectedDate;

                if (date != null) {
                        selectedDate = LocalDate.parse(date);
                } else {
                        selectedDate = LocalDate.now();
                }

                LocalDateTime weekStart = selectedDate
                                .with(DayOfWeek.MONDAY)
                                .atStartOfDay();

                LocalDateTime weekEnd = weekStart
                                .plusDays(6)
                                .withHour(23)
                                .withMinute(59);

                model.addAttribute("selectedDate", selectedDate);

                model.addAttribute("events",
                                eventService.getEventsForWeek(user, weekStart, weekEnd));

                model.addAttribute("metrics",
                                eventService.getWeeklyAnalysis(user, weekStart, weekEnd));

                model.addAttribute("aiPlan",
                                aiPlannerService.generatWeeklyPlan(user, weekStart, weekEnd));

                return "dashboard";
        }
}
