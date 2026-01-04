package com.sashkomusic.downloadagent.infrastracture.client.bandcamp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class BandcampCommandExecutor {

    public CompletableFuture<Process> executeAsync(String... command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing command: {}", String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();

                logOutput(process);

                return process;
            } catch (IOException e) {
                throw new RuntimeException("Failed to start bandcamp-dl process", e);
            }
        });
    }

    private static void logOutput(Process process) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[bandcamp-dl] {}", line);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    log.error("Bandcamp download failed with exit code {}", exitCode);
                } else {
                    log.info("bandcamp-dl completed successfully");
                }
            } catch (Exception e) {
                log.error("Error reading bandcamp-dl output: {}", e.getMessage(), e);
            }
        });
    }
}
