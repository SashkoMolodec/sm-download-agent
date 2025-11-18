package com.sashkomusic.downloadagent.messaging.producer;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;
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

    private final KafkaTemplate<String, SearchResultDto> kafkaTemplate;

    public void sendResults(long chatId, List<DownloadOption> results) {
        log.info("Sending {} results back to chat {}", results.size(), chatId);

        SearchResultDto dto = new SearchResultDto(chatId, results);

        kafkaTemplate.send(RESULT_TOPIC, dto);
    }
}