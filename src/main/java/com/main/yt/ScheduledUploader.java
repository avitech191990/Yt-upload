package com.main.yt;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledUploader {
    private final YtUploadApplication app;

    public ScheduledUploader(YtUploadApplication app) {
        this.app = app;
    }

    //@Scheduled(fixedRate = 120000) // every 2 minutes
    public void runUploadTask() {
        try {
            app.runUploader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
