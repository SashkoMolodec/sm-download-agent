package com.sashkomusic.downloadagent.infrastracture.client.slskd;

import com.sashkomusic.downloadagent.domain.SearchPort;
import com.sashkomusic.downloadagent.domain.model.DownloadOption;
import com.sashkomusic.downloadagent.exception.SearchNotCompleteException;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.dto.SlskdSearchEventResponse;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.dto.SlskdSearchEntryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SlskdClient implements SearchPort {

    private final RestClient client;
    private final String apiKey;
    private final RetryTemplate retryTemplate;

    public SlskdClient(RestClient.Builder builder,
                       @Value("${slskd.api-key:}") String apiKey) {
        this.client = builder.baseUrl("http://localhost:5030").build();
        this.apiKey = apiKey;
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(20)
                .fixedBackoff(3000)
                .retryOn(SearchNotCompleteException.class)
                .build();
    }

    @Override
    public List<DownloadOption> search(String artist, String release) {
        var query = artist + " " + release;

        var searchId = initiateSearchRequest(query);
        List<SlskdSearchEntryResponse> response = retryTemplate.execute(
                context -> pollSearchResultsUntilComplete(searchId), context -> {
                    log.warn("Search polling timeout after max attempts, returning empty. SearchId: {}, Query: '{}'", searchId, query);
                    return List.of();
                });

        return toDomain(response);
    }

    private List<DownloadOption> toDomain(List<SlskdSearchEntryResponse> response) {
        return response.stream()
                .filter(r -> r.files() != null && !r.files().isEmpty())
                .filter(r -> r.lockedFileCount() == 0)
                .filter(SlskdSearchEntryResponse::canDownload)
                .map(SlskdClient::mapOption)
                .toList();
    }

    private UUID initiateSearchRequest(String query) {
        Map<String, Object> searchRequest = Map.of(
                "searchText", query,
                "searchTimeout", 15000,
                "responseLimit", 100,
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

        log.info("Search initiated successfully: '{}' (ID: {})", query, response.getId());
        return response.getId();
    }

    private List<SlskdSearchEntryResponse> pollSearchResultsUntilComplete(UUID searchId) {
        log.info("Polling search status: searchId={}", searchId);

        SlskdSearchEventResponse status = client.get()
                .uri("/api/v0/searches/{id}", searchId)
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(SlskdSearchEventResponse.class);

        log.info("Status: searchId={}, state={}, isComplete={}, fileCount={}",
                searchId, status.getState(), status.getIsComplete(), status.getFileCount());

        if (Boolean.FALSE.equals(status.getIsComplete())) {
            throw new SearchNotCompleteException("Search still in progress");
        }

        return client.get()
                .uri("/api/v0/searches/{id}/responses", searchId)
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private static DownloadOption mapOption(SlskdSearchEntryResponse r) {
        var fileItems = r.files().stream().map(SlskdClient::mapFileItem).collect(Collectors.toList());

        return new DownloadOption(
                UUID.randomUUID().toString(),
                "soulseek",
                r.username(),
                r.getTotalSizeMB(),
                fileItems,
                new HashMap<>()
        );
    }

    private static DownloadOption.FileItem mapFileItem(SlskdSearchEntryResponse.SoulseekFile f) {
        return new DownloadOption.FileItem(
                f.filename(),
                f.getSizeMB(),
                f.bitRate(),                // null, якщо це FLAC/WAV
                f.bitDepth(),               // null, якщо це MP3
                f.sampleRate(),             // null, якщо це MP3
                f.length() != null ? f.length() : 0 // Тривалість у секундах (null check)
        );
    }
}
