package com.sashkomusic.downloadagent.messaging.consumer.dto;

import com.sashkomusic.downloadagent.domain.model.DownloadEngine;

public record SearchFilesTaskDto(
        long chatId,
        String releaseId,
        String artist,
        String title,
        DownloadEngine source) {
}