package com.sashkomusic.downloadagent.messaging.consumer;

import com.sashkomusic.downloadagent.domain.AcquisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchTaskListener {

    private final AcquisitionService acquisitionService;

    @KafkaListener(topics = "files-search-tasks")
    public void handleSearchTask(SearchRequestDto task) {
        acquisitionService.search(task);
    }
}
