package com.sashkomusic.downloadagent.api.webhook;

import com.sashkomusic.downloadagent.api.webhook.dto.SlskdDownloadCompleteWebhook;
import com.sashkomusic.downloadagent.domain.DownloadContext;
import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadCompleteDto;
import com.sashkomusic.downloadagent.messaging.producer.dto.DownloadErrorDto;
import com.sashkomusic.downloadagent.messaging.producer.DownloadCompleteProducer;
import com.sashkomusic.downloadagent.messaging.producer.DownloadErrorProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/slskd")
@Slf4j
@RequiredArgsConstructor
public class SlskdWebhookController {

    private final DownloadCompleteProducer downloadCompleteProducer;
    private final DownloadErrorProducer errorProducer;
    private final DownloadContext downloadContext;

    @PostMapping("/download-complete")
    public ResponseEntity<Void> handleDownloadComplete(@RequestBody SlskdDownloadCompleteWebhook webhook) {
        log.info("Received download complete webhook: type={}, remoteFilename={}",
                webhook.type(), webhook.remoteFilename());
        log.info("Webhook details: size={} bytes, state={}, localFilename={}",
                webhook.transfer() != null ? webhook.transfer().size() : 0,
                webhook.transfer() != null ? webhook.transfer().state() : "null",
                webhook.localFilename());

        if (webhook.transfer() == null) {
            log.error("Webhook transfer is null for file: {}", webhook.remoteFilename());
            return ResponseEntity.ok().build();
        }

        Long chatId = downloadContext.getChatIdForFile(webhook.remoteFilename());
        if (chatId == null) {
            log.warn("No chatId found for file: {}", webhook.remoteFilename());
            return ResponseEntity.ok().build();
        }

        try {
            var dto = DownloadCompleteDto.of(chatId, webhook.remoteFilename(), webhook.transfer().size());
            downloadCompleteProducer.sendComplete(dto);
            downloadContext.clearDownload(webhook.remoteFilename());
            
        } catch (Exception e) {
            log.error("Failed to process download complete webhook for chatId={}, file={}: {}",
                    chatId, webhook.remoteFilename(), e.getMessage(), e);
            errorProducer.sendError(DownloadErrorDto.of(
                    chatId,
                    "шось не ладні при обробці завершення скачування... " + extractFileName(webhook.remoteFilename())
            ));
        }

        return ResponseEntity.ok().build();
    }

    private String extractFileName(String path) {
        if (path == null) {
            return "";
        }
        int lastSlash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }
}