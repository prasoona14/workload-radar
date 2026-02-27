package com.loadly.mvp.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loadly.mvp.service.CanvasSyncService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sync")
public class SyncController {

    private final CanvasSyncService canvasSyncService;

    @PostMapping("/canvas")
    public String syncCanvas(@RequestParam int userId) {
        int count = canvasSyncService.syncCanvasEvents(userId);
        return count + " Canvas events synced successfully";
    }
}
