package com.sashkomusic.downloadagent.domain.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class DownloadBatch {
    private final long chatId;
    private final String releaseId;
    private final String directoryPath;
    private final List<String> allFiles;
    private final Set<String> remainingFiles;

    public DownloadBatch(long chatId, String releaseId, String directoryPath, List<String> files) {
        this.chatId = chatId;
        this.releaseId = releaseId;
        this.directoryPath = directoryPath;
        this.allFiles = List.copyOf(files);
        this.remainingFiles = new HashSet<>(files);
        log.debug("Created download batch: releaseId={}, directory={}, totalFiles={}",
                releaseId, directoryPath, files.size());
    }

    public void markFileCompleted(String filename) {
        boolean removed = remainingFiles.remove(filename);
        if (removed) {
            log.debug("File completed: {} ({}/{} files remaining)",
                    filename, remainingFiles.size(), allFiles.size());
        }
    }

    public boolean isComplete() {
        return remainingFiles.isEmpty();
    }

    public int getTotalFiles() {
        return allFiles.size();
    }

    public int getRemainingCount() {
        return remainingFiles.size();
    }
}