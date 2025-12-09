package com.sashkomusic.downloadagent.messaging.producer.dto;

import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;

import java.util.List;

public record SearchFilesResultDto(
        long chatId,
        String releaseId,
        DownloadEngine source,
        List<DownloadOption> results,
        boolean autoDownload) {
}
