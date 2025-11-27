package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadCompleteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadCompleteProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendComplete(DownloadCompleteDto complete) {
        log.info("Sending download complete: {} - {} MB", complete.filename(), complete.sizeMB());
        kafkaTemplate.send("download-complete", complete);
    }
}