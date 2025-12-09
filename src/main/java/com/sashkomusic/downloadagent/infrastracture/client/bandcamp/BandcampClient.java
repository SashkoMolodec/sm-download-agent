package com.sashkomusic.downloadagent.infrastracture.client.bandcamp;

import com.sashkomusic.downloadagent.domain.DownloadMonitorService;
import com.sashkomusic.downloadagent.domain.MusicSourcePort;
import com.sashkomusic.downloadagent.domain.exception.MusicDownloadException;
import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BandcampClient implements MusicSourcePort {

    private final BandcampSearchClient searchClient;
    private final DownloadMonitorService monitorService;

    @Value("${bandcamp.cli-path}")
    private String bandcampDlPath;

    @Value("${bandcamp.download-path}")
    private String outputPath;

    @Override
    public boolean autoDownloadEnabled() {
        return true;
    }

    @Override
    public List<DownloadOption> search(String artist, String release) {
        log.info("Searching Bandcamp: artist='{}', release='{}'", artist, release);

        try {
            var searchResults = searchClient.search(artist, release);

            if (searchResults.isEmpty()) {
                log.info("No Bandcamp results found for: {} - {}", artist, release);
                return List.of();
            }

            log.info("Found {} Bandcamp results", searchResults.size());

            return searchResults.stream()
                    .map(this::toDownloadOption)
                    .toList();

        } catch (Exception e) {
            log.error("Error searching Bandcamp: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public String initiateDownload(DownloadOption option) {
        String url = option.technicalMetadata().get("url");

        if (url == null) {
            throw new MusicDownloadException("Missing Bandcamp URL in metadata");
        }

        log.info("Initiating Bandcamp download: url={}", url);

        try {
            executeBandcampDl(url);
            log.info("Bandcamp download completed successfully");

            return option.id();

        } catch (Exception e) {
            log.error("Error downloading from Bandcamp: {}", e.getMessage(), e);
            throw new MusicDownloadException("не вийшло скачати з Bandcamp: " + e.getMessage(), e);
        }
    }

    private void executeBandcampDl(String url) {
        try {
            log.info("Starting Bandcamp download with bandcamp-dl: url={}", url);

            if (!Files.exists(Path.of(bandcampDlPath))) {
                log.error("bandcamp-dl executable not found: {}", bandcampDlPath);
                throw new MusicDownloadException("bandcamp-dl executable not found: " + bandcampDlPath);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    bandcampDlPath,
                    url,
                    "--base-dir", outputPath,
                    "--template", "%{artist}/%{album}/%{track} - %{title}"
            );

            processBuilder.redirectErrorStream(true);

            log.info("Executing command: {}", String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[bandcamp-dl] {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Bandcamp download failed with exit code {}", exitCode);
                throw new MusicDownloadException("Bandcamp download failed with exit code: " + exitCode);
            }

            log.info("bandcamp-dl completed successfully");

        } catch (MusicDownloadException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing bandcamp-dl: {}", e.getMessage(), e);
            throw new MusicDownloadException("Error downloading from Bandcamp: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDownloadPath(DownloadOption option) {
        return outputPath;
    }

    @Override
    public void handleDownloadCompletion(long chatId, String releaseId, DownloadOption option, String downloadPath) {
        log.info("Handling Bandcamp download completion: chatId={}, releaseId={}", chatId, releaseId);

        String artist = option.technicalMetadata().get("artist");
        String title = option.technicalMetadata().get("title");
        // Bandcamp doesn't provide trackCount in search, use 1 as default
        int expectedFiles = 1;

        log.info("Starting monitoring for Bandcamp release: artist='{}', title='{}', expectedFiles={}",
                artist, title, expectedFiles);

        monitorService.startMonitoring(chatId, releaseId, downloadPath, expectedFiles, artist, title);
    }

    private DownloadOption toDownloadOption(BandcampSearchResult result) {
        String optionId = "bandcamp-" + UUID.randomUUID();
        String displayName = result.artist() + " - " + result.title() + " [" + result.type() + "]";

        int totalSizeMB = 0; // Unknown until download
        List<DownloadOption.FileItem> files = List.of(); // Not available from search

        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", result.url());
        metadata.put("artist", result.artist());
        metadata.put("title", result.title());
        metadata.put("type", result.type());

        return new DownloadOption(
                optionId,
                DownloadEngine.BANDCAMP,
                displayName,
                totalSizeMB,
                files,
                metadata
        );
    }
}
