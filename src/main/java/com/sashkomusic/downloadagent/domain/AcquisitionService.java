package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.messaging.consumer.SearchRequestDto;
import com.sashkomusic.downloadagent.messaging.producer.SearchResultProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcquisitionService {

    private final SearchPort searchPort;
    private final SearchResultProducer searchResultProducer;

    public void search(SearchRequestDto task) {
        String artist = task.artist();
        String title = task.title();

        List<DownloadOption> result = searchPort.search(artist, title);
        searchResultProducer.sendResults(task.chatId(), result);
    }
}

