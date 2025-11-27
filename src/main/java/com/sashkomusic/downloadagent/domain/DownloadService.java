package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.exception.MusicDownloadException;
import com.sashkomusic.downloadagent.messaging.consumer.dto.DownloadFilesTaskDto;
import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadErrorDto;
import com.sashkomusic.downloadagent.messaging.producer.DownloadErrorProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadService {

    private final MusicSourcePort musicSourcePort;
    private final DownloadErrorProducer errorProducer;
    private final DownloadContext downloadContext;

    public void download(DownloadFilesTaskDto task) {
        try {
            // Register all files with chatId so webhook can find them later
            task.downloadOption().files().forEach(file ->
                    downloadContext.registerDownload(file.filename(), task.chatId())
            );

            String downloadId = musicSourcePort.initiateDownload(task.downloadOption());
            log.info("Download initiated: downloadId={}, files={}", downloadId, task.downloadOption().files().size());

        } catch (MusicDownloadException e) {
            log.error("Download failed for chatId={}: {}", task.chatId(), e.getMessage());
            errorProducer.sendError(DownloadErrorDto.of(task.chatId(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during download for chatId={}: {}", task.chatId(), e.getMessage(), e);
            errorProducer.sendError(DownloadErrorDto.of(task.chatId(), "шось не то, пупупу... " + e.getMessage()));
        }
    }
}