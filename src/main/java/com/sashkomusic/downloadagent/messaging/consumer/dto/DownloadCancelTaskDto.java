package com.sashkomusic.downloadagent.messaging.consumer.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("download_cancel")
public record DownloadCancelTaskDto(
        long chatId,
        String releaseId
) {
}
