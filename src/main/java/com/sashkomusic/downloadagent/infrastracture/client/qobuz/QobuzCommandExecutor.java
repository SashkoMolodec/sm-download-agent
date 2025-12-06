package com.sashkomusic.downloadagent.infrastracture.client.qobuz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class QobuzCommandExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public CommandResult execute(String... command) {
        return execute(DEFAULT_TIMEOUT_SECONDS, command);
    }

    public CommandResult execute(int timeoutSeconds, String... command) {
        log.info("Executing command: {}", String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stderr into stdout

            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("qobuz-dl output: {}", line);
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.error("Command timed out after {} seconds", timeoutSeconds);
                return new CommandResult(false, "", "Command timed out");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            if (exitCode == 0) {
                log.info("Command completed successfully");
                return new CommandResult(true, outputStr, null);
            } else {
                log.error("Command failed with exit code {}: {}", exitCode, outputStr);
                return new CommandResult(false, outputStr, "Exit code: " + exitCode);
            }

        } catch (IOException e) {
            log.error("Failed to execute command: {}", e.getMessage(), e);
            return new CommandResult(false, "", "IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Command execution interrupted: {}", e.getMessage());
            return new CommandResult(false, "", "Interrupted: " + e.getMessage());
        }
    }

    public record CommandResult(
            boolean success,
            String output,
            String error
    ) {
    }
}
