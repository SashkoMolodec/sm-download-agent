package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadBatchCompleteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadBatchCompleteProducer {

    private final KafkaTemplate<String, DownloadBatchCompleteDto> kafkaTemplate;

    public void sendBatchComplete(DownloadBatchCompleteDto dto) {
        log.info("Sending download batch complete: releaseId={}, chatId={}, files={}",
                dto.releaseId(), dto.chatId(), dto.totalFiles());
        kafkaTemplate.send("download-batch-complete", dto);
    }
}