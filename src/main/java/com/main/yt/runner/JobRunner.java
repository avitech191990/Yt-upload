package com.main.yt.runner;
import com.main.yt.job.DriveVideoProcessingJob;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class JobRunner implements CommandLineRunner {

    private final DriveVideoProcessingJob job;

    public JobRunner(DriveVideoProcessingJob job) {
        this.job = job;
    }

    @Override
    public void run(String... args) throws Exception {
        job.run();
        System.exit(0); // REQUIRED for GitHub Actions
    }
}

