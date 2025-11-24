package com.sashkomusic.downloadagent.messaging.consumer;

public record SearchFilesTaskDto(
        long chatId,
        String releaseId,
        String artist,
        String title) {
}