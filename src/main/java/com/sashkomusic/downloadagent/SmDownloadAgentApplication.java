package com.sashkomusic.downloadagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry(proxyTargetClass = true)
@EnableScheduling
public class SmDownloadAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmDownloadAgentApplication.class, args);
    }

}
