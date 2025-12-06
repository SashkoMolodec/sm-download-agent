package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.messaging.producer.DownloadBatchCompleteProducer;
import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadBatchCompleteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadMonitorService {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "flac", "mp3", "wav", "m4a", "aac", "alac", "aiff", "ogg", "wma", "ape"
    );

    private final DownloadBatchCompleteProducer batchCompleteProducer;
    private final Map<String, DownloadMonitorTask> activeTasks = new ConcurrentHashMap<>();

    public void startMonitoring(long chatId, String releaseId, String downloadPath,
                                 int expectedFileCount, DownloadEngine source, String artist, String title) {
        String taskId = releaseId + ":" + chatId;

        DownloadMonitorTask task = new DownloadMonitorTask(
                chatId, releaseId, downloadPath, expectedFileCount, source, artist, title
        );

        activeTasks.put(taskId, task);
        log.info("Started monitoring download: taskId={}, path={}, expectedFiles={}, source={}, artist={}, title={}",
                 taskId, downloadPath, expectedFileCount, source, artist, title);
    }

    @Scheduled(fixedDelay = 3000) // Every 3 seconds
    public void checkDownloads() {
        if (activeTasks.isEmpty()) {
            return;
        }

        log.info("Checking {} active downloads", activeTasks.size());

        activeTasks.entrySet().removeIf(entry -> {
            String taskId = entry.getKey();
            DownloadMonitorTask task = entry.getValue();

            try {
                Path basePath = Path.of(task.downloadPath());

                if (!Files.exists(basePath)) {
                    log.info("Base directory not yet created: {}", basePath);
                    return false; // Keep monitoring
                }

                // For Qobuz, find folder matching artist and title (qobuz-dl creates: "Artist - Album (Year) [Quality]")
                Path downloadDir = basePath;
                if (task.source() == DownloadEngine.QOBUZ) {
                    String artist = task.artist();
                    String title = task.title();

                    if (artist == null || title == null) {
                        log.warn("Missing artist or title metadata for Qobuz download");
                        return false;
                    }

                    try (Stream<Path> subdirs = Files.list(basePath)) {
                        var albumFolder = subdirs
                                .filter(Files::isDirectory)
                                .filter(p -> {
                                    String folderName = p.getFileName().toString().toLowerCase();
                                    return folderName.contains(artist.toLowerCase()) &&
                                           folderName.contains(title.toLowerCase());
                                })
                                .findFirst();

                        if (albumFolder.isEmpty()) {
                            log.info("Album folder not yet created matching artist='{}' and title='{}' in: {}",
                                    artist, title, basePath);
                            return false; // Keep monitoring
                        }

                        downloadDir = albumFolder.get();
                        log.info("Found Qobuz album folder: {}", downloadDir);
                    }
                }

                List<String> audioFiles = findAudioFiles(downloadDir);
                int currentCount = audioFiles.size();

                log.info("taskId={}, checking: {} / {} audio files, source={}",
                         taskId, currentCount, task.expectedFileCount(), task.source());

                boolean isComplete = false;

                if (task.source() == DownloadEngine.QOBUZ) {
                    // For Qobuz: check if files are stable (not changing for 6 seconds)
                    if (task.isStable(currentCount)) {
                        log.info("Download complete (Qobuz stable): taskId={}, files={}", taskId, currentCount);
                        isComplete = true;
                    }
                } else {
                    // For Soulseek: check exact file count
                    if (currentCount >= task.expectedFileCount()) {
                        log.info("Download complete (Soulseek count match): taskId={}, files={}", taskId, currentCount);
                        isComplete = true;
                    }
                }

                if (isComplete) {
                    batchCompleteProducer.sendBatchComplete(
                            DownloadBatchCompleteDto.of(
                                    task.chatId(),
                                    task.releaseId(),
                                    downloadDir.toString(),
                                    audioFiles
                            )
                    );

                    return true; // Remove from active tasks
                }

            } catch (Exception e) {
                log.error("Error checking download taskId={}: {}", taskId, e.getMessage(), e);
            }

            return false; // Keep monitoring
        });
    }

    private List<String> findAudioFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> isAudioFile(path.getFileName().toString()))
                    .map(Path::toString)
                    .toList();
        }
    }

    private boolean isAudioFile(String filename) {
        String lower = filename.toLowerCase();
        return AUDIO_EXTENSIONS.stream().anyMatch(ext -> lower.endsWith("." + ext));
    }

    public static class DownloadMonitorTask {
        private final long chatId;
        private final String releaseId;
        private final String downloadPath;
        private final int expectedFileCount;
        private final DownloadEngine source;
        private final String artist;
        private final String title;
        private int lastFileCount = -1;
        private int stableChecks = 0;

        public DownloadMonitorTask(long chatId, String releaseId, String downloadPath,
                                    int expectedFileCount, DownloadEngine source, String artist, String title) {
            this.chatId = chatId;
            this.releaseId = releaseId;
            this.downloadPath = downloadPath;
            this.expectedFileCount = expectedFileCount;
            this.source = source;
            this.artist = artist;
            this.title = title;
        }

        public long chatId() { return chatId; }
        public String releaseId() { return releaseId; }
        public String downloadPath() { return downloadPath; }
        public int expectedFileCount() { return expectedFileCount; }
        public DownloadEngine source() { return source; }
        public String artist() { return artist; }
        public String title() { return title; }

        public boolean isStable(int currentCount) {
            if (currentCount == lastFileCount && currentCount > 0) {
                stableChecks++;
            } else {
                stableChecks = 0;
            }
            lastFileCount = currentCount;
            return stableChecks >= 2; // Stable for 2 checks (6 seconds)
        }
    }
}
