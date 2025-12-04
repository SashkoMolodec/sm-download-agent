package com.sashkomusic.downloadagent.infrastracture.client.slskd;

import com.sashkomusic.downloadagent.domain.MusicSourcePort;
import com.sashkomusic.downloadagent.domain.exception.MusicDownloadException;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.dto.SlskdDownloadResponse;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.dto.SlskdSearchEntryResponse;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.dto.SlskdSearchEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class SlskdClient implements MusicSourcePort {

    private static final long POLL_TIMEOUT_MS = 20_000;
    private static final long POLL_INTERVAL_MS = 3_000;
    private static final long STABILIZATION_DELAY_MS = 10_000;

    private final RestClient client;
    private final String apiKey;

    public SlskdClient(RestClient.Builder builder,
                       @Value("${slskd.api-key:}") String apiKey) {
        this.client = builder.baseUrl("http://localhost:5030").build();
        this.apiKey = apiKey;
    }

    @Override
    @Retryable(
            retryFor = NoSearchResultsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public List<DownloadOption> search(String artist, String release) {
        var query = artist + " " + release;
        log.info("🔄 Soulseek search attempt for: {}", query);

        var searchId = initiateSearchRequest(query);
        int fileCount = waitForSearchToComplete(searchId);
        waitToStabilize(fileCount);

        List<DownloadOption> results = getSearchResults(searchId);

        if (results.isEmpty()) {
            log.warn("❌ No results found for query: {}, throwing exception to trigger retry", query);
            throw new NoSearchResultsException("No results found for: " + query);
        }

        log.info("✅ Found {} results", results.size());
        return results;
    }

    @Recover
    public List<DownloadOption> recoverFromEmptySearch(NoSearchResultsException e, String artist, String release) {
        log.error("🛑 RECOVERY: No results found after 3 attempts for: {} - {}", artist, release);
        return List.of();
    }

    private UUID initiateSearchRequest(String query) {
        Map<String, Object> searchRequest = Map.of(
                "searchText", query,
                "searchTimeout", (int) POLL_TIMEOUT_MS,
                "responseLimit", 70,
                "filterResponses", true,
                "minimumResponseFileCount", 1,
                "minimumPeerUploadSpeed", 0
        );

        SlskdSearchEventResponse response = client.post()
                .uri("/api/v0/searches")
                .header("X-API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(searchRequest)
                .retrieve()
                .body(SlskdSearchEventResponse.class);

        if (response == null || response.getId() == null) {
            throw new RuntimeException("Failed to initiate search for query: " + query);
        }

        log.info("Search initiated: '{}' (ID: {})", query, response.getId());
        return response.getId();
    }

    private int waitForSearchToComplete(UUID searchId) {
        long endTime = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        SlskdSearchEventResponse lastStatus = null;

        while (System.currentTimeMillis() < endTime) {
            try {
                var status = getSearchStatus(searchId);
                lastStatus = status;
                log.info("Status: searchId={}, state={}, isComplete={}, fileCount={}",
                        searchId, status.getState(), status.getIsComplete(), status.getFileCount());

                if (Boolean.TRUE.equals(status.getIsComplete())) {
                    return status.getFileCount() != null ? status.getFileCount() : 0;
                }

                Thread.sleep(POLL_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Search interrupted", e);
            } catch (Exception e) {
                log.warn("Error polling status, retrying in next tick: {}", e.getMessage());
            }
        }
        log.warn("Search polling timed out locally for ID: {}. Returning accumulated results.", searchId);
        return lastStatus != null && lastStatus.getFileCount() != null ? lastStatus.getFileCount() : 0;
    }

    private static void waitToStabilize(int fileCount) {
        if (fileCount > 0) {
            try {
                log.debug("Waiting {}ms for results to stabilize...", STABILIZATION_DELAY_MS);
                Thread.sleep(STABILIZATION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            log.info("No files found, skipping stabilization wait");
        }
    }

    private SlskdSearchEventResponse getSearchStatus(UUID searchId) {
        return client.get()
                .uri("/api/v0/searches/{id}", searchId)
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(SlskdSearchEventResponse.class);
    }

    private List<DownloadOption> getSearchResults(UUID searchId) {
        List<SlskdSearchEntryResponse> responses = client.get()
                .uri("/api/v0/searches/{id}/responses", searchId.toString())
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return toDomain(responses);
    }

    private List<DownloadOption> toDomain(List<SlskdSearchEntryResponse> response) {
        if (response == null) return List.of();

        return response.stream()
                .filter(r -> r.files() != null && !r.files().isEmpty())
                .filter(r -> r.lockedFileCount() == 0)
                .filter(SlskdSearchEntryResponse::canDownload)
                .flatMap(this::splitByAlbumFolder)
                .toList();
    }

    private Stream<DownloadOption> splitByAlbumFolder(SlskdSearchEntryResponse response) {
        Map<String, List<SlskdSearchEntryResponse.SoulseekFile>> groupedByFolder = response.files().stream()
                .filter(f -> f.size() > 0)
                .filter(SlskdSearchEntryResponse.SoulseekFile::isAudioFile)
                .collect(Collectors.groupingBy(f -> extractAlbumFolder(f.filename())));

        return groupedByFolder.entrySet().stream()
                .map(entry -> {
                    String albumFolder = entry.getKey();
                    List<SlskdSearchEntryResponse.SoulseekFile> filesInFolder = entry.getValue();

                    double totalSizeMB = filesInFolder.stream()
                            .mapToLong(SlskdSearchEntryResponse.SoulseekFile::size)
                            .sum() / (1024.0 * 1024.0);

                    return mapOption(response, albumFolder, filesInFolder, totalSizeMB);
                });
    }

    private static DownloadOption mapOption(
            SlskdSearchEntryResponse response,
            String albumFolder,
            List<SlskdSearchEntryResponse.SoulseekFile> files,
            double totalSizeMB) {

        var fileItems = files.stream()
                .map(SlskdClient::mapFileItem)
                .collect(Collectors.toList());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("username", response.username());
        metadata.put("albumFolder", albumFolder);

        return new DownloadOption(
                UUID.randomUUID().toString(),
                "soulseek",
                response.username() + " - " + albumFolder,
                (int) totalSizeMB,
                fileItems,
                metadata
        );
    }

    private static DownloadOption.FileItem mapFileItem(SlskdSearchEntryResponse.SoulseekFile f) {
        return new DownloadOption.FileItem(
                f.filename(),
                f.size(),
                f.bitRate(),
                f.bitDepth(),
                f.sampleRate(),
                f.length() != null ? f.length() : 0
        );
    }

    private static String extractAlbumFolder(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "Unknown Album";
        }

        String normalized = filePath.replace("/", "\\");
        int lastSlash = normalized.lastIndexOf('\\');

        if (lastSlash > 0) {
            return normalized.substring(0, lastSlash);
        }

        return "Unknown Album";
    }

    @Override
    @Retryable(
            retryFor = MusicDownloadException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public String initiateDownload(DownloadOption option) {
        String username = option.technicalMetadata().get("username");

        if (username == null) {
            log.error("Missing required metadata: username is null");
            throw new MusicDownloadException("трохи даних бракує для запиту шоби скачати музло");
        }

        log.info("Attempting download from user={}, files={}", username, option.files().size());

        List<Map<String, Object>> files = option.files().stream()
                .map(f -> Map.<String, Object>of(
                        "filename", f.filename(),
                        "size", f.size()
                ))
                .toList();

        log.info("Initiating download from user={}, files count={}", username, files.size());
        files.forEach(f -> log.debug("  - {}", f.get("filename")));

        try {
            var response = client.post()
                    .uri("/api/v0/transfers/downloads/{username}", username)
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(files)
                    .retrieve()
                    .body(SlskdDownloadResponse.class);

            if (response == null || response.enqueued() == null || response.enqueued().isEmpty()) {
                log.warn("No files were enqueued for download from username={}", username);
                throw new MusicDownloadException("ніц не виходе скачати...");
            }

            log.info("Download initiated for username={}, enqueued {} files",
                    username, response.enqueued().size());

            String batchId = response.enqueued().getFirst().id();
            log.info("Batch ID: {}", batchId);

            return batchId;
        } catch (MusicDownloadException e) {
            log.warn("Download attempt failed for username={}, will retry: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to initiate download for username={}: {}", username, e.getMessage());
            throw new MusicDownloadException("не вийшло розпочати скачування: " + e.getMessage(), e);
        }
    }

    @Recover
    public String recoverFromDownloadFailure(MusicDownloadException e, DownloadOption option) {
        String username = option.technicalMetadata().get("username");
        log.error("Failed to download from username={} after 5 attempts: {}", username, e.getMessage());
        throw new MusicDownloadException("не вийшло скачати після 5 спроб: " + e.getMessage(), e);
    }
}