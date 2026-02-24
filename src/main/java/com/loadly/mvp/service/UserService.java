package com.loadly.mvp.service;

import org.springframework.stereotype.Service;

import com.loadly.mvp.model.User;
import com.loadly.mvp.repository.UserRepo;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User getUserById(int id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}