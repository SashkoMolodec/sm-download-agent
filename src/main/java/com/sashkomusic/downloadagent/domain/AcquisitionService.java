package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.messaging.consumer.dto.SearchFilesTaskDto;
import com.sashkomusic.downloadagent.messaging.producer.SearchResultProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcquisitionService {

    private final MusicSourcePort musicSourcePort;
    private final SearchResultProducer searchResultProducer;

    public void search(SearchFilesTaskDto task) {
        String artist = task.artist();
        String title = task.title();

        List<DownloadOption> result = musicSourcePort.search(artist, title);
        searchResultProducer.sendResults(task.chatId(), task.releaseId(), result);
    }
}

