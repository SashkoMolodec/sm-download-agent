package com.sashkomusic.downloadagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry(proxyTargetClass = true)
public class SmDownloadAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmDownloadAgentApplication.class, args);
    }

}
