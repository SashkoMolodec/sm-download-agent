package com.sashkomusic.downloadagent.messaging.consumer;

import com.sashkomusic.downloadagent.domain.DownloadService;
import com.sashkomusic.downloadagent.messaging.consumer.dto.DownloadCancelTaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadCancelListener {

    private final DownloadService downloadService;

    @KafkaListener(topics = "download-cancel-tasks", groupId = "download-agent-group")
    public void handleCancelTask(DownloadCancelTaskDto dto) {
        log.info("Received cancel download task: chatId={}, releaseId={}",
                dto.chatId(), dto.releaseId());
        downloadService.cancelDownload(dto.chatId(), dto.releaseId());
    }
}
