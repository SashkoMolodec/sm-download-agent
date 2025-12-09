package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.messaging.producer.dto.SearchFilesResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchResultProducer {

    public static final String RESULT_TOPIC = "file-search-results";

    private final KafkaTemplate<String, SearchFilesResultDto> kafkaTemplate;

    public void sendResults(long chatId, String releaseId, DownloadEngine source, List<DownloadOption> results, boolean autoDownload) {
        log.info("Sending {} results from {} back to chat {} (autoDownload={})", results.size(), source, chatId, autoDownload);

        SearchFilesResultDto dto = new SearchFilesResultDto(chatId, releaseId, source, results, autoDownload);

        kafkaTemplate.send(RESULT_TOPIC, dto);
    }
}