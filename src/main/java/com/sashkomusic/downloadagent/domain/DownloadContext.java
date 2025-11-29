package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DownloadContext {

    // directoryPath -> DownloadBatch
    private final ConcurrentHashMap<String, DownloadBatch> batches = new ConcurrentHashMap<>();

    public void registerBatch(long chatId, String releaseId, List<String> filenames) {
        if (filenames.isEmpty()) {
            log.warn("Attempted to register empty batch for chatId={}, releaseId={}", chatId, releaseId);
            return;
        }

        String directoryPath = extractDirectory(filenames.getFirst());

        DownloadBatch batch = new DownloadBatch(chatId, releaseId, directoryPath, filenames);
        batches.put(directoryPath, batch);

        log.info("Registered download batch: releaseId={}, directory={}, files={}",
                releaseId, directoryPath, filenames.size());
    }

    public DownloadBatch markFileCompleted(String remoteFilename) {
        String directory = extractDirectory(remoteFilename);
        DownloadBatch batch = batches.get(directory);

        if (batch == null) {
            log.warn("No batch found for file: {} (directory: {})", remoteFilename, directory);
            return null;
        }

        batch.markFileCompleted(remoteFilename);

        if (batch.isComplete()) {
            batches.remove(directory);
            log.info("Download batch completed: releaseId={}, directory={}",
                    batch.getReleaseId(), directory);
        }

        return batch;
    }

    private String extractDirectory(String path) {
        if (path == null) {
            return "";
        }
        int lastSlash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }
        return "";
    }
}