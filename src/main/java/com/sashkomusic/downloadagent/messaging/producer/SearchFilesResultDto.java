package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;

import java.util.List;

public record SearchFilesResultDto(
        long chatId,
        String releaseId,
        List<DownloadOption> results) {
}
