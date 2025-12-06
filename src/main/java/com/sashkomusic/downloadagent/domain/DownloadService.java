package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.exception.MusicDownloadException;
import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.messaging.consumer.dto.DownloadFilesTaskDto;
import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadErrorDto;
import com.sashkomusic.downloadagent.messaging.producer.DownloadErrorProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadService {

    private final Map<DownloadEngine, MusicSourcePort> musicSources;
    private final DownloadErrorProducer errorProducer;
    private final DownloadContext downloadContext;
    private final DownloadMonitorService monitorService;

    public void download(DownloadFilesTaskDto task) {
        try {
            List<String> filenames = task.downloadOption().files().stream()
                    .map(DownloadOption.FileItem::filename)
                    .toList();

            downloadContext.registerBatch(task.chatId(), task.releaseId(), filenames);

            DownloadOption option = task.downloadOption();
            MusicSourcePort client = musicSources.get(option.source());
            log.info("Using {} client for download", option.source());

            String downloadId = client.initiateDownload(option);
            log.info("Download initiated: downloadId={}, source={}, releaseId={}, files={}",
                    downloadId, option.source(), task.releaseId(), filenames.size());

            String downloadPath = client.getDownloadPath(option);
            int expectedFileCount = resolveExpectedFileCount(option);

            String artist = option.technicalMetadata().get("artist");
            String title = option.technicalMetadata().get("title");

            monitorService.startMonitoring(
                    task.chatId(),
                    task.releaseId(),
                    downloadPath,
                    expectedFileCount,
                    option.source(),
                    artist,
                    title
            );

        } catch (MusicDownloadException e) {
            log.error("Download failed for chatId={}: {}", task.chatId(), e.getMessage());
            errorProducer.sendError(DownloadErrorDto.of(task.chatId(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during download for chatId={}: {}", task.chatId(), e.getMessage(), e);
            errorProducer.sendError(DownloadErrorDto.of(task.chatId(), "шось не то, пупупу... " + e.getMessage()));
        }
    }

    private static int resolveExpectedFileCount(DownloadOption option) {
        int expectedFileCount = 1;
        if (!option.files().isEmpty()) {
            expectedFileCount = option.files().size();
        }
        return expectedFileCount;
    }
}