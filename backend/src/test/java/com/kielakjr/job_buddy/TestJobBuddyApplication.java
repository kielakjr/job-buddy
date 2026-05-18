package com.kielakjr.job_buddy;

import org.springframework.boot.SpringApplication;

public class TestJobBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.from(JobBuddyApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
