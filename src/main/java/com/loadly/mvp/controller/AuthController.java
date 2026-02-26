package com.loadly.mvp.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.loadly.mvp.model.User;
import com.loadly.mvp.repository.UserRepo;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // show register page
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // handle registration form submission
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {

        // chekc if email exists
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email already in use");
            return "register";
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save user to database
        userRepo.save(user);

        return "redirect:/login";
    }

    // Show login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

}
