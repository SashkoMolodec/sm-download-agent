package com.sashkomusic.downloadagent.messaging.consumer.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;

@JsonTypeName("download_request")
public record DownloadFilesTaskDto(
        long chatId,
        String releaseId,
        DownloadOption downloadOption
) {
}