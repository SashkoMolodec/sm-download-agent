package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;

import java.util.List;

public record SearchResultDto(
        long chatId,
        List<DownloadOption> results) {
}
