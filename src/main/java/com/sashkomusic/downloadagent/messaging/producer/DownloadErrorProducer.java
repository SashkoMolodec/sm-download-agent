package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadErrorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadErrorProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendError(DownloadErrorDto error) {
        log.info("Sending download error for chatId={}: {}", error.chatId(), error.errorMessage());
        kafkaTemplate.send("download-errors", error);
    }
}