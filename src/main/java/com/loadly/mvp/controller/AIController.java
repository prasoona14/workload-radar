package com.loadly.mvp.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.loadly.mvp.ai.AIPlannerService;
import com.loadly.mvp.ai.AIWeeklyPlan;
import com.loadly.mvp.ai.AIWeeklyPlanRequest;
import com.loadly.mvp.model.User;
import com.loadly.mvp.service.UserService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    AIPlannerService aiPlannerService;

    @Autowired
    private UserService userService;

    @PostMapping("/weeklyplan")
    public AIWeeklyPlan weeklyPlan(@RequestBody AIWeeklyPlanRequest request) {
        LocalDateTime start = LocalDateTime.parse(request.getWeekStart());
        LocalDateTime end = LocalDateTime.parse(request.getWeekEnd());
        User user = userService.getUserById(request.getUserId());
        return aiPlannerService.generatWeeklyPlan(user, start, end);
    }

}
