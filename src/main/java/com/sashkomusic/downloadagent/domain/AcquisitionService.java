package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.messaging.consumer.dto.SearchFilesTaskDto;
import com.sashkomusic.downloadagent.messaging.producer.SearchResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcquisitionService {

    private final Map<DownloadEngine, MusicSourcePort> musicSources;
    private final SearchResultProducer searchResultProducer;

    public void search(SearchFilesTaskDto task) {
        String artist = task.artist();
        String title = task.title();

        log.info("Starting music search: artist='{}', title='{}', source={}", artist, title, task.source());

        if (task.source() != null) {
            MusicSourcePort source = musicSources.get(task.source());
            log.info("Searching only in {} (user requested)", task.source());

            List<DownloadOption> results = source.search(artist, title);
            searchResultProducer.sendResults(task.chatId(), task.releaseId(), results);
            return;
        }

        MusicSourcePort qobuzSource = musicSources.get(DownloadEngine.QOBUZ);
        List<DownloadOption> qobuzResults = qobuzSource.search(artist, title);

        if (!qobuzResults.isEmpty()) {
            log.info("Found {} options on Qobuz, skipping Soulseek", qobuzResults.size());
            searchResultProducer.sendResults(task.chatId(), task.releaseId(), qobuzResults);
            return;
        }

        log.info("No Qobuz results, falling back to Soulseek");
        MusicSourcePort slskdSource = musicSources.get(DownloadEngine.SOULSEEK);
        List<DownloadOption> slskdResults = slskdSource.search(artist, title);
        searchResultProducer.sendResults(task.chatId(), task.releaseId(), slskdResults);
    }
}

