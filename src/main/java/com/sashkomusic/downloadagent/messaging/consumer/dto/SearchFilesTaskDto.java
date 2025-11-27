package com.sashkomusic.downloadagent.messaging.consumer.dto;

public record SearchFilesTaskDto(
        long chatId,
        String releaseId,
        String artist,
        String title) {
}