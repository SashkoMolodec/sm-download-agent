package com.sashkomusic.downloadagent.infrastracture.client.applemusic;

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
public class AppleMusicClient implements MusicSourcePort {

    private final ITunesSearchClient searchClient;
    private final DownloadMonitorService monitorService;

    @Value("${applemusic.gamdl.path}")
    private String gamdlPath;

    @Value("${applemusic.gamdl.cookies}")
    private String cookiesPath;

    @Value("${applemusic.gamdl.output}")
    private String outputPath;

    @Override
    public boolean autoDownloadEnabled() {
        return true;
    }

    @Override
    public List<DownloadOption> search(String artist, String release) {
        log.info("Searching Apple Music: artist='{}', release='{}'", artist, release);

        try {
            var searchResults = searchClient.search(artist, release);

            if (searchResults.isEmpty()) {
                log.info("No Apple Music results found for: {} - {}", artist, release);
                return List.of();
            }

            log.info("Found {} Apple Music results", searchResults.size());

            return searchResults.stream()
                    .map(this::toDownloadOption)
                    .toList();

        } catch (Exception e) {
            log.error("Error searching Apple Music: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public String initiateDownload(DownloadOption option) {
        String url = option.technicalMetadata().get("url");

        if (url == null) {
            throw new MusicDownloadException("Missing Apple Music URL in metadata");
        }

        log.info("Initiating Apple Music download: url={}", url);

        try {
            executeGamdlDownload(url);
            log.info("Apple Music download completed successfully");

            String batchId = option.technicalMetadata().get("albumId");
            return batchId != null ? batchId : option.id();

        } catch (Exception e) {
            log.error("Error downloading from Apple Music: {}", e.getMessage(), e);
            throw new MusicDownloadException("не вийшло скачати з Apple Music: " + e.getMessage(), e);
        }
    }

    private void executeGamdlDownload(String url) {
        try {
            log.info("Starting Apple Music download with gamdl: url={}", url);

            if (!Files.exists(Path.of(gamdlPath))) {
                log.error("gamdl executable not found: {}", gamdlPath);
                throw new MusicDownloadException("gamdl executable not found: " + gamdlPath);
            }

            if (!Files.exists(Path.of(cookiesPath))) {
                log.error("Cookies file not found: {}", cookiesPath);
                throw new MusicDownloadException("Cookies file not found: " + cookiesPath);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    gamdlPath,
                    url,
                    "--cookies-path", cookiesPath,
                    "--output-path", outputPath,
                    "--language", "uk",
                    "--song-codec", "aac-legacy"
            );

            processBuilder.redirectErrorStream(true);

            log.info("Executing command: {}", String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[gamdl] {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Apple Music download failed with exit code {}", exitCode);
                throw new MusicDownloadException("Apple Music download failed with exit code: " + exitCode);
            }

            log.info("gamdl completed successfully");

        } catch (MusicDownloadException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing gamdl: {}", e.getMessage(), e);
            throw new MusicDownloadException("Error downloading from Apple Music: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDownloadPath(DownloadOption option) {
        return outputPath;
    }

    @Override
    public void handleDownloadCompletion(long chatId, String releaseId, DownloadOption option, String downloadPath) {
        log.info("Handling Apple Music download completion: chatId={}, releaseId={}", chatId, releaseId);

        String artist = option.technicalMetadata().get("artist");
        String albumName = option.technicalMetadata().get("albumName");
        int trackCount = Integer.parseInt(option.technicalMetadata().getOrDefault("trackCount", "1"));

        log.info("Starting monitoring for Apple Music album: artist='{}', album='{}', expectedTracks={}",
                artist, albumName, trackCount);

        monitorService.startMonitoring(chatId, releaseId, downloadPath, trackCount, artist, albumName);
    }

    private DownloadOption toDownloadOption(ITunesSearchClient.AppleMusicSearchResult result) {
        String optionId = "applemusic-" + result.id();
        String displayName = result.displayName();

        int totalSizeMB = 0;
        List<DownloadOption.FileItem> files = List.of();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", result.url());
        metadata.put("albumId", result.id());
        metadata.put("artist", result.artistName());
        metadata.put("albumName", result.albumName());
        metadata.put("trackCount", String.valueOf(result.trackCount()));

        return new DownloadOption(
                optionId,
                DownloadEngine.APPLE_MUSIC,
                displayName,
                totalSizeMB,
                files,
                metadata
        );
    }
}
