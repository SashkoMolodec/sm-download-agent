package com.sashkomusic.downloadagent.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DownloadContext {

    private final ConcurrentHashMap<String, Long> filenameToChatId = new ConcurrentHashMap<>();

    public void registerDownload(String filename, long chatId) {
        log.debug("Registering download: filename={}, chatId={}", filename, chatId);
        filenameToChatId.put(filename, chatId);
    }

    public Long getChatIdForFile(String filename) {
        return filenameToChatId.get(filename);
    }

    public void clearDownload(String filename) {
        filenameToChatId.remove(filename);
    }
}