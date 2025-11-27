package com.sashkomusic.downloadagent.messaging.consumer;

import com.sashkomusic.downloadagent.domain.DownloadService;
import com.sashkomusic.downloadagent.messaging.consumer.dto.DownloadFilesTaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadTaskListener {

    private final DownloadService downloadService;

    @KafkaListener(topics = "files-download-tasks", groupId = "download-agent-group")
    public void handleDownloadTask(DownloadFilesTaskDto dto) {
        log.info("Received download task: chatId={}, releaseId={}", dto.chatId(), dto.releaseId());
        downloadService.download(dto);
    }
}