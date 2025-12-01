package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.config.SlskdPathConfig;
import com.sashkomusic.downloadagent.domain.model.DownloadBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DownloadContext {

    private final SlskdPathConfig pathConfig;

    // directoryPath -> DownloadBatch
    private final ConcurrentHashMap<String, DownloadBatch> batches = new ConcurrentHashMap<>();

    public void registerBatch(long chatId, String releaseId, List<String> filenames) {
        if (filenames.isEmpty()) {
            log.warn("Attempted to register empty batch for chatId={}, releaseId={}", chatId, releaseId);
            return;
        }

        String remoteDirectoryPath = extractDirectory(filenames.getFirst());

        DownloadBatch batch = new DownloadBatch(chatId, releaseId, remoteDirectoryPath, filenames);
        batches.put(remoteDirectoryPath, batch);

        log.info("Registered download batch: releaseId={}, directory={}, files={}",
                releaseId, remoteDirectoryPath, filenames.size());
    }

    public DownloadBatch markFileCompleted(String remoteFilename, String localFilename) {
        String directory = extractDirectory(remoteFilename);
        DownloadBatch batch = batches.get(directory);

        if (batch == null) {
            log.warn("No batch found for file: {} (directory: {})", remoteFilename, directory);
            return null;
        }

        batch.markFileCompleted(remoteFilename);

        String transformedLocalPath = pathConfig.transformToLocalPath(localFilename);
        log.debug("Path transformation: {} -> {}", localFilename, transformedLocalPath);
        batch.addLocalFilename(transformedLocalPath);

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