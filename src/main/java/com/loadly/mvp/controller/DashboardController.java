package com.loadly.mvp.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.loadly.mvp.ai.AIPlannerService;
import com.loadly.mvp.model.User;
import com.loadly.mvp.repository.UserRepo;
import com.loadly.mvp.service.CanvasSyncService;
import com.loadly.mvp.service.EventService;

@Controller
public class DashboardController {

        private final EventService eventService;
        private final AIPlannerService aiPlannerService;
        private final UserRepo userRepo;
        private final CanvasSyncService canvasSyncService;

        public DashboardController(EventService eventService,
                        AIPlannerService aiPlannerService, UserRepo userRepo, CanvasSyncService canvasSyncService) {
                this.eventService = eventService;
                this.aiPlannerService = aiPlannerService;
                this.userRepo = userRepo;
                this.canvasSyncService = canvasSyncService;
        }

        @GetMapping("/")
        public String root() {
                return "redirect:/login";
        }

        @GetMapping("/home")
        public String home() {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth.getName();

                User user = userRepo.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (user.getCanvasIcsUrl() == null || user.getCanvasIcsUrl().isBlank()) {
                        return "redirect:/setup";
                }

                return "redirect:/dashboard";
        }

        @GetMapping("/dashboard")
        public String dashboard(
                        @RequestParam(required = false) String date,
                        Model model) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth.getName();
                User user = userRepo.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Logged-in user not found in DB"));

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
                model.addAttribute("user", user);

                model.addAttribute("events",
                                eventService.getEventsForWeek(user, weekStart, weekEnd));

                model.addAttribute("metrics",
                                eventService.getWeeklyAnalysis(user, weekStart, weekEnd));

                model.addAttribute("aiPlan",
                                aiPlannerService.generatWeeklyPlan(user, weekStart, weekEnd));

                return "dashboard";
        }

        @GetMapping("/setup")
        public String setupPage() {
                return "setup";
        }

        @PostMapping("/setup")
        public String saveSetup(@RequestParam String canvasIcsUrl, Model model) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth.getName();

                User user = userRepo.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                user.setCanvasIcsUrl(canvasIcsUrl);
                userRepo.save(user);

                try {
                        canvasSyncService.syncCanvasEvents(user.getId());
                } catch (Exception e) {

                        e.printStackTrace(); // ✅ ADD THIS LINE HERE (prints real error in terminal)
                        model.addAttribute("errorMessage", e.getMessage());
                        return "setup";
                }

                return "redirect:/dashboard";
        }
}
